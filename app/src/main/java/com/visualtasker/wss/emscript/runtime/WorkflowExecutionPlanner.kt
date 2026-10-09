package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptBinaryOp
import com.visualtasker.wss.emscript.parser.EmscriptIrExpression
import com.visualtasker.wss.emscript.parser.EmscriptIrStatement
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.BlockNode
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.NormalizedOperator
import de.visualtasker.workflow.core.OperatorNormalization
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.core.WorkspaceGraph
import de.visualtasker.workflow.core.asString
import de.visualtasker.workflow.core.canonicalSymbol
import de.visualtasker.workflow.semantics.CommandArgumentType
import de.visualtasker.workflow.semantics.CommandCatalogEntry
import de.visualtasker.workflow.semantics.VisualTaskerCommandCatalog
import de.visualtasker.workflow.semantics.WorkflowElementTypes as BlockTypes
import de.visualtasker.workflow.semantics.maximumArgumentCount
import de.visualtasker.workflow.semantics.validation.Validator
import de.visualtasker.workflow.semantics.workspaceInputNameAt

class WorkflowExecutionPlanner(
    private val parser: EmscriptParserSlice = EmscriptParserSlice(),
) {
    fun plan(request: ExecutionRequest): ExecutionPlanResult {
        val validation = Validator.validate(request.document)
        if (!validation.isValid) {
            return ExecutionPlanResult.Rejected(
                validation.errors.map { error ->
                    ExecutionDiagnostic(
                        code = "WORKFLOW_INVALID",
                        message = error.message,
                    )
                },
            )
        }

        val roots = WorkspaceGraph.topLevelRoots(request.document)
            .filter { request.document.blocks[it]?.type == BlockTypes.EVENT_START }
        val entry = when {
            request.entryPointEntityId != null -> roots.firstOrNull {
                it.value == request.entryPointEntityId
            }
            roots.size == 1 -> roots.single()
            roots.isEmpty() -> null
            else -> return ExecutionPlanResult.Rejected(
                listOf(
                    ExecutionDiagnostic(
                        code = "AMBIGUOUS_ENTRY_POINT",
                        message = "Workflow has ${roots.size} entry points; select one explicitly.",
                    ),
                ),
            )
        } ?: return ExecutionPlanResult.Rejected(
            listOf(
                ExecutionDiagnostic(
                    code = "ENTRY_POINT_NOT_FOUND",
                    message = "No matching event.start entry point exists.",
                    entityId = request.entryPointEntityId,
                ),
            ),
        )

        return runCatching {
            val steps = WorkspaceGraph.nextChain(request.document, entry)
                ?.let { compileChain(request.document, it) }
                .orEmpty()
            ExecutionPlanResult.Ready(
                ExecutionPlan(
                    requestId = request.id,
                    workflowId = request.workflowId,
                    sourceRevision = request.document.version,
                    entryPointEntityId = entry.value,
                    steps = steps,
                    requiredCapabilities = collectCapabilities(steps),
                ),
            )
        }.getOrElse { error ->
            ExecutionPlanResult.Rejected(
                listOf(
                    ExecutionDiagnostic(
                        code = "UNSUPPORTED_WORKFLOW_CONSTRUCT",
                        message = error.message ?: "Workflow cannot be compiled for M5-0.",
                    ),
                ),
            )
        }
    }

    private fun compileChain(document: WorkspaceDocument, head: BlockId): List<ExecutionStep> = buildList {
        val visited = linkedSetOf<BlockId>()
        var current: BlockId? = head
        while (current != null) {
            check(visited.add(current)) { "Cycle reached while compiling ${current.value}." }
            add(compileStep(document, document.blocks.getValue(current)))
            current = WorkspaceGraph.nextChain(document, current)
        }
    }

    private fun compileStep(document: WorkspaceDocument, block: BlockNode): ExecutionStep = when (block.type) {
        BlockTypes.VARIABLE_SET -> ExecutionStep.Assign(
            entityId = block.id.value,
            variableId = block.variableReferenceId(),
            declaration = block.fieldText("assignmentKind").equals("LET", ignoreCase = true),
            expression = expressionInput(document, block, "value")
                ?: expressionInput(document, block, "VALUE")
                ?: parseInlineExpression(block.id, block.fieldText("value")),
        )

        BlockTypes.CONTROL_IF,
        BlockTypes.CONTROL_IF_ELSE,
        BlockTypes.CONTROL_IF_ELSEIF_ELSE,
        -> ExecutionStep.Branch(
            entityId = block.id.value,
            alternatives = buildList {
                add(
                    ExecutionBranchAlternative(
                        relationId = "branch:${block.id.value}:${BlockTypes.SLOT_THEN}",
                        condition = requireExpressionInput(document, block, "CONDITION"),
                        steps = compileSlot(document, block.id, BlockTypes.SLOT_THEN),
                    ),
                )
                block.elseIfSlots().forEach { (conditionSlot, statementSlot) ->
                    add(
                        ExecutionBranchAlternative(
                            relationId = "branch:${block.id.value}:$statementSlot",
                            condition = requireExpressionInput(document, block, conditionSlot),
                            steps = compileSlot(document, block.id, statementSlot),
                        ),
                    )
                }
            },
            elseSteps = compileSlot(document, block.id, BlockTypes.SLOT_ELSE),
        )

        BlockTypes.CONTROL_REPEAT -> ExecutionStep.Repeat(
            entityId = block.id.value,
            count = ExecutionExpression.Literal(block.id.value, ExecutionValue.NumberValue(block.fieldNumber("times"))),
            body = compileSlot(document, block.id, BlockTypes.SLOT_DO),
        )

        BlockTypes.CONTROL_WHILE -> ExecutionStep.While(
            entityId = block.id.value,
            condition = requireExpressionInput(document, block, "CONDITION"),
            body = compileSlot(document, block.id, BlockTypes.SLOT_BODY),
        )

        else -> compileInvocation(document, block)
    }

    private fun compileInvocation(document: WorkspaceDocument, block: BlockNode): ExecutionStep.Invoke {
        val entry = VisualTaskerCommandCatalog.findByBlockType(block.type)
            ?: error("Unsupported executable block '${block.type}' at ${block.id.value}.")
        return ExecutionStep.Invoke(
            entityId = block.id.value,
            commandId = entry.id,
            capability = entry.executionCapability(),
            arguments = compileArguments(document, block, entry),
        )
    }

    private fun compileArguments(
        document: WorkspaceDocument,
        block: BlockNode,
        entry: CommandCatalogEntry,
    ): List<ExecutionExpression> {
        val maximum = entry.maximumArgumentCount() ?: block.valueInputs.size
        return buildList {
            entry.arguments.take(maximum).forEachIndexed { index, argument ->
                if (argument.type == CommandArgumentType.STATEMENT_BODY) return@forEachIndexed
                val inputName = entry.workspaceInputNameAt(index) ?: argument.name
                val expression = expressionInput(document, block, inputName)
                    ?: block.fields[inputName]?.let { field -> literal(block.id.value, field) }
                    ?: block.fields[argument.name]?.let { field -> literal(block.id.value, field) }
                    ?: specialFieldArgument(block, argument.name)
                    ?: argument.defaultValue?.let { default -> defaultExpression(block.id.value, argument.type, default) }
                if (expression != null) add(expression)
            }
        }
    }

    private fun specialFieldArgument(block: BlockNode, argumentName: String): ExecutionExpression? {
        val key = when (block.type) {
            BlockTypes.ACTION_CLICK_TEXT -> if (argumentName.equals("text", true)) "text" else null
            BlockTypes.ACTION_WAIT -> if (argumentName in setOf("ms", "duration", "durationMs")) "ms" else null
            BlockTypes.DEBUG_LOG -> if (argumentName in setOf("message", "value")) "message" else null
            BlockTypes.FEEDBACK_BEEP -> argumentName.takeIf { it in setOf("frequency", "durationMs", "volume") }
            BlockTypes.FEEDBACK_VIBRATE -> if (argumentName == "pattern") "pattern" else null
            else -> null
        } ?: return null
        return block.fields[key]?.let { literal(block.id.value, it) }
    }

    private fun defaultExpression(
        entityId: String,
        type: CommandArgumentType,
        value: String,
    ): ExecutionExpression = when (type) {
        CommandArgumentType.NUMBER,
        CommandArgumentType.DURATION_MS,
        CommandArgumentType.FREQUENCY_HZ,
        CommandArgumentType.PERCENT,
        -> ExecutionExpression.Literal(entityId, ExecutionValue.NumberValue(value.toDouble()))
        CommandArgumentType.BOOLEAN -> ExecutionExpression.Literal(entityId, ExecutionValue.BooleanValue(value.toBooleanStrict()))
        else -> ExecutionExpression.Literal(entityId, ExecutionValue.StringValue(value))
    }

    private fun compileSlot(document: WorkspaceDocument, parent: BlockId, slot: String): List<ExecutionStep> =
        WorkspaceGraph.statementStackHead(document, parent, slot)?.let { compileChain(document, it) }.orEmpty()

    private fun expressionInput(
        document: WorkspaceDocument,
        block: BlockNode,
        inputName: String,
    ): ExecutionExpression? {
        val connected = block.valueInputs
            .firstOrNull { it.name.equals(inputName, ignoreCase = true) }
            ?.connection
            ?.connectedTo
            ?: return null
        val childId = WorkspaceGraph.findConnection(document, connected)?.first ?: return null
        return compileExpression(document, document.blocks.getValue(childId))
    }

    private fun requireExpressionInput(document: WorkspaceDocument, block: BlockNode, inputName: String): ExecutionExpression =
        requireNotNull(expressionInput(document, block, inputName)) {
            "Missing expression '$inputName' at ${block.id.value}."
        }

    private fun compileExpression(document: WorkspaceDocument, block: BlockNode): ExecutionExpression = when (block.type) {
        BlockTypes.LOGIC_BOOLEAN,
        BlockTypes.LITERAL_BOOLEAN,
        -> ExecutionExpression.Literal(block.id.value, ExecutionValue.BooleanValue(block.fieldBool("value")))
        BlockTypes.LITERAL_NUMBER -> ExecutionExpression.Literal(block.id.value, ExecutionValue.NumberValue(block.fieldNumber("value")))
        BlockTypes.LITERAL_STRING -> ExecutionExpression.Literal(block.id.value, ExecutionValue.StringValue(block.fieldText("value")))
        BlockTypes.VARIABLE_GET,
        BlockTypes.VARIABLE_VALUE,
        BlockTypes.VARIABLES_GET,
        -> ExecutionExpression.Variable(block.id.value, block.variableReferenceId())
        BlockTypes.LOGIC_COMPARE,
        BlockTypes.LOGIC_OPERATE,
        BlockTypes.LOGIC_AND,
        BlockTypes.LOGIC_OR,
        -> {
            val (leftName, rightName, fallback) = when (block.type) {
                BlockTypes.LOGIC_COMPARE -> Triple("LEFT", "RIGHT", "==")
                BlockTypes.LOGIC_OPERATE -> Triple("Input1", "Input2", "+")
                BlockTypes.LOGIC_AND -> Triple("A", "B", "&&")
                else -> Triple("A", "B", "||")
            }
            val operator = if (block.type == BlockTypes.LOGIC_AND) "&&" else if (block.type == BlockTypes.LOGIC_OR) "||" else {
                when (val normalized = OperatorNormalization.normalize(block.fieldText("operator"))) {
                    is NormalizedOperator.Compare -> normalized.canonicalSymbol
                    is NormalizedOperator.Arithmetic -> normalized.canonicalSymbol
                    null -> fallback
                }
            }
            ExecutionExpression.Binary(
                entityId = block.id.value,
                operator = operator,
                left = requireExpressionInput(document, block, leftName),
                right = requireExpressionInput(document, block, rightName),
            )
        }
        else -> when {
            block.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX) ->
                ExecutionExpression.Variable(block.id.value, block.variableReferenceId())
            VisualTaskerCommandCatalog.findByBlockType(block.type) != null -> {
                val entry = requireNotNull(VisualTaskerCommandCatalog.findByBlockType(block.type))
                check(!entry.returnType.isNullOrBlank() && !entry.returnType.equals("Void", true)) {
                    "Void command '${entry.id}' cannot be used as an expression."
                }
                ExecutionExpression.ProviderCall(
                    entityId = block.id.value,
                    commandId = entry.id,
                    capability = entry.executionCapability(),
                    arguments = compileArguments(document, block, entry),
                )
            }
            else -> error("Unsupported expression block '${block.type}' at ${block.id.value}.")
        }
    }

    private fun parseInlineExpression(entityId: BlockId, source: String): ExecutionExpression {
        val parsed = parser.parse("SET __runtime = $source")
        val statement = parsed.ir?.statements?.singleOrNull() as? EmscriptIrStatement.Set
            ?: return ExecutionExpression.Literal(entityId.value, ExecutionValue.StringValue(source))
        return compileParsedExpression(entityId.value, statement.value)
    }

    private fun compileParsedExpression(entityId: String, expression: EmscriptIrExpression): ExecutionExpression = when (expression) {
        is EmscriptIrExpression.VariableRef -> ExecutionExpression.Variable(entityId, expression.name)
        is EmscriptIrExpression.NumberLiteral -> ExecutionExpression.Literal(entityId, ExecutionValue.NumberValue(expression.value))
        is EmscriptIrExpression.StringLiteral -> ExecutionExpression.Literal(entityId, ExecutionValue.StringValue(expression.value))
        is EmscriptIrExpression.BooleanLiteral -> ExecutionExpression.Literal(entityId, ExecutionValue.BooleanValue(expression.value))
        is EmscriptIrExpression.Binary -> ExecutionExpression.Binary(
            entityId = entityId,
            operator = expression.op.symbol(),
            left = compileParsedExpression(entityId, expression.left),
            right = compileParsedExpression(entityId, expression.right),
        )
        is EmscriptIrExpression.FunctionCall -> {
            val entry = VisualTaskerCommandCatalog.findByCanonicalName(expression.name)
                ?: VisualTaskerCommandCatalog.findByAcceptedName(expression.name)
                ?: error("Unknown expression command '${expression.name}'.")
            check(!entry.returnType.isNullOrBlank() && !entry.returnType.equals("Void", true)) {
                "Void command '${entry.id}' cannot be used as an expression."
            }
            ExecutionExpression.ProviderCall(
                entityId = entityId,
                commandId = entry.id,
                capability = entry.executionCapability(),
                arguments = expression.arguments.map { compileParsedExpression(entityId, it) },
            )
        }
    }

    private fun literal(entityId: String, field: FieldValue): ExecutionExpression.Literal = ExecutionExpression.Literal(
        entityId = entityId,
        value = when (field) {
            is FieldValue.Bool -> ExecutionValue.BooleanValue(field.value)
            is FieldValue.Number -> ExecutionValue.NumberValue(field.value)
            is FieldValue.Text -> ExecutionValue.StringValue(field.value)
        },
    )

    private fun collectCapabilities(steps: List<ExecutionStep>): Set<ExecutionCapability> = buildSet {
        fun expression(value: ExecutionExpression) {
            when (value) {
                is ExecutionExpression.Binary -> {
                    expression(value.left)
                    expression(value.right)
                }
                is ExecutionExpression.ProviderCall -> {
                    add(value.capability)
                    value.arguments.forEach(::expression)
                }
                is ExecutionExpression.Literal,
                is ExecutionExpression.Variable,
                -> Unit
            }
        }
        fun visit(step: ExecutionStep) {
            when (step) {
                is ExecutionStep.Assign -> expression(step.expression)
                is ExecutionStep.Invoke -> {
                    add(step.capability)
                    step.arguments.forEach(::expression)
                }
                is ExecutionStep.Branch -> {
                    step.alternatives.forEach { alternative ->
                        expression(alternative.condition)
                        alternative.steps.forEach(::visit)
                    }
                    step.elseSteps.forEach(::visit)
                }
                is ExecutionStep.Repeat -> {
                    expression(step.count)
                    step.body.forEach(::visit)
                }
                is ExecutionStep.While -> {
                    expression(step.condition)
                    step.body.forEach(::visit)
                }
            }
        }
        steps.forEach(::visit)
    }

    private fun BlockNode.elseIfSlots(): List<Pair<String, String>> = valueInputs.mapNotNull { input ->
        when (input.name) {
            "ELIF_CONDITION" -> input.name to BlockTypes.SLOT_ELIF
            else -> input.name.removePrefix("ELIF_CONDITION_")
                .takeIf { it != input.name && it.toIntOrNull() != null }
                ?.let { input.name to "ELIF_$it" }
        }
    }.sortedBy { (condition, _) ->
        if (condition == "ELIF_CONDITION") 0 else condition.removePrefix("ELIF_CONDITION_").toIntOrNull() ?: Int.MAX_VALUE
    }

    private fun BlockNode.variableReferenceId(): String =
        fields["variableId"]?.asString()?.takeIf(String::isNotBlank)
            ?: type.removePrefix(BlockTypes.VARIABLE_REPORTER_PREFIX).takeIf { it != type && it.isNotBlank() }
            ?: fieldText("variable")

    private fun BlockNode.fieldText(key: String): String = fields[key]?.asString().orEmpty()

    private fun BlockNode.fieldNumber(key: String): Double = when (val value = fields[key]) {
        is FieldValue.Number -> value.value
        is FieldValue.Text -> value.value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    private fun BlockNode.fieldBool(key: String): Boolean = when (val value = fields[key]) {
        is FieldValue.Bool -> value.value
        is FieldValue.Text -> value.value.equals("true", ignoreCase = true)
        else -> false
    }

    private fun CommandCatalogEntry.executionCapability(): ExecutionCapability =
        ExecutionCapability(runtime?.liveCapabilityGate?.name ?: "command:$id")

    private fun EmscriptBinaryOp.symbol(): String = when (this) {
        EmscriptBinaryOp.OR -> "||"
        EmscriptBinaryOp.AND -> "&&"
        EmscriptBinaryOp.ADD -> "+"
        EmscriptBinaryOp.SUB -> "-"
        EmscriptBinaryOp.MUL -> "*"
        EmscriptBinaryOp.DIV -> "/"
        EmscriptBinaryOp.MOD -> "%"
        EmscriptBinaryOp.EQ -> "=="
        EmscriptBinaryOp.NEQ -> "!="
        EmscriptBinaryOp.LT -> "<"
        EmscriptBinaryOp.LTE -> "<="
        EmscriptBinaryOp.GT -> ">"
        EmscriptBinaryOp.GTE -> ">="
    }
}
