package com.visualtasker.wss.emscript.parser

import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.domain.VariableDefinition
import de.visualtasker.blockeditor.domain.VariableScope
import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.registry.asFactory

data class EmscriptImportResult(
    val ir: EmscriptIrScript?,
    val document: WorkspaceDocument?,
    val issues: List<EmscriptParseIssue>,
) {
    val isSuccess: Boolean
        get() = ir != null && document != null && issues.isEmpty()
}

class EmscriptWorkspaceImporter(
    private val parser: EmscriptParserSlice = EmscriptParserSlice(),
) {
    fun import(script: String, workspaceId: String = "emscript-import"): EmscriptImportResult {
        val parsed = parser.parse(script)
        val ir = parsed.ir ?: return EmscriptImportResult(
            ir = null,
            document = null,
            issues = parsed.issues,
        )
        return runCatching {
            val assembler = WorkspaceAssembler(workspaceId)
            val document = assembler.build(ir, EmscriptEditorFacetScanner.scan(script))
            EmscriptImportResult(ir = ir, document = document, issues = emptyList())
        }.getOrElse { error ->
            EmscriptImportResult(
                ir = ir,
                document = null,
                issues = listOf(
                    EmscriptParseIssue(
                        line = 1,
                        column = 1,
                        message = error.message ?: "Workspace-Import fehlgeschlagen.",
                    ),
                ),
            )
        }
    }
}

private class WorkspaceAssembler(workspaceId: String) {
    private val registry = CompositeBlockRegistry()
    private var document = WorkspaceDocument(id = workspaceId)

    fun build(ir: EmscriptIrScript, facets: List<EmscriptGroupFacet> = emptyList()): WorkspaceDocument {
        val startBlock = instantiate(BlockTypes.EVENT_START)
        if (facets.isNotEmpty()) {
            annotateGroupFacets(startBlock, facets)
        }
        val topLevelHead = appendStatementChain(ir.statements)
        if (topLevelHead != null) {
            connectNext(startBlock, topLevelHead)
        }
        return document
    }

    private fun annotateGroupFacets(blockId: BlockId, facets: List<EmscriptGroupFacet>) {
        val block = document.blocks[blockId] ?: return
        val metadata = buildMap {
            putAll(block.metadata)
            put("emscript.groupFacet.count", facets.size.toString())
            facets.forEachIndexed { index, facet ->
                val prefix = "emscript.groupFacet.$index"
                put("$prefix.id", facet.id)
                put("$prefix.label", facet.label)
                put("$prefix.kind", facet.kind)
                put("$prefix.startLine", facet.startLine.toString())
                facet.endLine?.let { put("$prefix.endLine", it.toString()) }
            }
        }
        document = document.copy(blocks = document.blocks + (blockId to block.copy(metadata = metadata)))
    }

    private fun appendStatementChain(statements: List<EmscriptIrStatement>): BlockId? {
        var head: BlockId? = null
        var tail: BlockId? = null
        statements.forEach { statement ->
            val statementId = emitStatement(statement)
            if (head == null) {
                head = statementId
            }
            if (tail != null) {
                connectNext(tail!!, statementId)
            }
            tail = statementId
        }
        return head
    }

    private fun emitStatement(statement: EmscriptIrStatement): BlockId {
        return when (statement) {
            is EmscriptIrStatement.Let -> {
                ensureVariable(
                    variableId = statement.variable,
                    defaultValue = expressionToInlineText(statement.value),
                    declaredType = inferExpressionType(statement.value),
                )
                emitSetVariableBlock(statement.variable, statement.value, assignmentKind = "LET")
            }
            is EmscriptIrStatement.Set -> {
                ensureVariable(statement.variable, defaultValue = null)
                emitSetVariableBlock(statement.variable, statement.value, assignmentKind = "SET")
            }
            is EmscriptIrStatement.CommandCall -> {
                val entry = de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog.findByCanonicalName(statement.command)
                    ?: de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog.findByAcceptedName(statement.command)
                    ?: error("Kommando '${statement.command}' ist nicht im Katalog.")
                val blockType = entry.block?.blockType ?: error("Kommando '${statement.command}' hat keinen Block-Typ.")
                val block = instantiate(blockType)
                setTextField(block, "command", entry.canonicalName)
                setTextField(block, "args", statement.arguments)
                block
            }
            is EmscriptIrStatement.Wait -> {
                val block = instantiate(BlockTypes.ACTION_WAIT)
                setNumberField(block, "ms", expressionToNumber(statement.milliseconds, fallback = 500.0))
                block
            }
            is EmscriptIrStatement.ClickText -> {
                val block = instantiate(BlockTypes.ACTION_CLICK_TEXT)
                setTextField(block, "text", statement.text)
                block
            }
            is EmscriptIrStatement.Output -> {
                val block = instantiate(BlockTypes.DEBUG_LOG)
                setTextField(block, "message", expressionToOutputText(statement.value))
                block
            }
            is EmscriptIrStatement.Beep -> {
                val block = instantiate(BlockTypes.FEEDBACK_BEEP)
                setNumberField(block, "frequency", statement.frequency?.toDouble() ?: 1000.0)
                setNumberField(block, "durationMs", statement.durationMs?.toDouble() ?: 200.0)
                setNumberField(block, "volume", statement.volume?.coerceIn(0, 100)?.toDouble() ?: 100.0)
                block
            }
            is EmscriptIrStatement.Vibrate -> {
                val block = instantiate(BlockTypes.FEEDBACK_VIBRATE)
                setTextField(block, "pattern", statement.pattern.joinToString())
                block
            }
            is EmscriptIrStatement.Loop -> {
                val repeatBlock = instantiate(BlockTypes.CONTROL_REPEAT)
                setNumberField(repeatBlock, "times", expressionToRepeatCount(statement.times))
                val bodyHead = appendStatementChain(statement.body)
                if (bodyHead != null) {
                    connectStatementInput(parent = repeatBlock, slotName = BlockTypes.SLOT_DO, child = bodyHead)
                }
                repeatBlock
            }
            is EmscriptIrStatement.While -> {
                val whileBlock = instantiate(BlockTypes.CONTROL_WHILE)
                val conditionBlock = emitExpression(statement.condition)
                connectValueInput(parent = whileBlock, inputName = "CONDITION", child = conditionBlock)
                val bodyHead = appendStatementChain(statement.body)
                if (bodyHead != null) {
                    connectStatementInput(parent = whileBlock, slotName = BlockTypes.SLOT_BODY, child = bodyHead)
                }
                whileBlock
            }
            is EmscriptIrStatement.If -> {
                val ifType = when {
                    statement.elseIfBranches.isNotEmpty() -> BlockTypes.CONTROL_IF_ELSEIF_ELSE
                    statement.elseBranch.isNotEmpty() -> BlockTypes.CONTROL_IF_ELSE
                    else -> BlockTypes.CONTROL_IF
                }
                val ifBlock = instantiate(ifType)
                val conditionBlock = emitExpression(statement.condition)
                connectValueInput(parent = ifBlock, inputName = "CONDITION", child = conditionBlock)
                val thenHead = appendStatementChain(statement.thenBranch)
                if (thenHead != null) {
                    connectStatementInput(parent = ifBlock, slotName = BlockTypes.SLOT_THEN, child = thenHead)
                }
                val elseIf = statement.elseIfBranches.firstOrNull()
                if (elseIf != null) {
                    val elseIfConditionBlock = emitExpression(elseIf.condition)
                    connectValueInput(parent = ifBlock, inputName = "ELIF_CONDITION", child = elseIfConditionBlock)
                    val elseIfHead = appendStatementChain(elseIf.body)
                    if (elseIfHead != null) {
                        connectStatementInput(parent = ifBlock, slotName = BlockTypes.SLOT_ELIF, child = elseIfHead)
                    }
                }
                val elseHead = appendStatementChain(statement.elseBranch)
                if (elseHead != null) {
                    connectStatementInput(parent = ifBlock, slotName = BlockTypes.SLOT_ELSE, child = elseHead)
                }
                ifBlock
            }
        }
    }

    private fun emitSetVariableBlock(
        variableId: String,
        value: EmscriptIrExpression,
        assignmentKind: String,
    ): BlockId {
        val block = instantiate(BlockTypes.VARIABLE_SET)
        setTextField(block, "assignmentKind", assignmentKind)
        setTextField(block, "variable", variableId)
        setTextField(block, "value", expressionToInlineText(value))
        return block
    }

    private fun emitExpression(expression: EmscriptIrExpression): BlockId {
        return when (expression) {
            is EmscriptIrExpression.VariableRef -> {
                ensureVariable(expression.name, defaultValue = null)
                emitVariableReporter(expression.name)
            }
            is EmscriptIrExpression.BooleanLiteral -> emitLiteralBoolean(expression.value)
            is EmscriptIrExpression.NumberLiteral -> emitLiteralNumber(expression.value)
            is EmscriptIrExpression.StringLiteral -> emitLiteralString(expression.value)
            is EmscriptIrExpression.Binary -> emitBinaryExpression(expression)
        }
    }

    private fun emitBinaryExpression(expression: EmscriptIrExpression.Binary): BlockId {
        val blockType = when (expression.op) {
            EmscriptBinaryOp.OR,
            EmscriptBinaryOp.AND -> when (expression.op) {
                EmscriptBinaryOp.OR -> BlockTypes.LOGIC_OR
                else -> BlockTypes.LOGIC_AND
            }
            EmscriptBinaryOp.ADD,
            EmscriptBinaryOp.SUB,
            EmscriptBinaryOp.MUL,
            EmscriptBinaryOp.DIV,
            EmscriptBinaryOp.MOD -> BlockTypes.LOGIC_OPERATE
            EmscriptBinaryOp.EQ,
            EmscriptBinaryOp.NEQ,
            EmscriptBinaryOp.LT,
            EmscriptBinaryOp.LTE,
            EmscriptBinaryOp.GT,
            EmscriptBinaryOp.GTE -> BlockTypes.LOGIC_COMPARE
        }
        val block = instantiate(blockType)
        setTextField(block, "operator", operatorFieldValue(expression.op))
        val left = emitExpression(expression.left)
        val right = emitExpression(expression.right)
        when (blockType) {
            BlockTypes.LOGIC_COMPARE -> {
                connectValueInput(parent = block, inputName = "LEFT", child = left)
                connectValueInput(parent = block, inputName = "RIGHT", child = right)
            }
            BlockTypes.LOGIC_AND,
            BlockTypes.LOGIC_OR -> {
                connectValueInput(parent = block, inputName = "A", child = left)
                connectValueInput(parent = block, inputName = "B", child = right)
            }
            else -> {
                connectValueInput(parent = block, inputName = "Input1", child = left)
                connectValueInput(parent = block, inputName = "Input2", child = right)
            }
        }
        return block
    }

    private fun emitLiteralNumber(value: Double): BlockId {
        val block = instantiate(BlockTypes.LITERAL_NUMBER)
        apply(
            WorkspaceAction.UpdateField(
                blockId = block,
                key = "value",
                value = FieldValue.Number(value),
            ),
        )
        return block
    }

    private fun emitLiteralString(value: String): BlockId {
        val block = instantiate(BlockTypes.LITERAL_STRING)
        setTextField(block, "value", value)
        return block
    }

    private fun emitLiteralBoolean(value: Boolean): BlockId {
        val block = instantiate(BlockTypes.LITERAL_BOOLEAN)
        apply(
            WorkspaceAction.UpdateField(
                blockId = block,
                key = "value",
                value = FieldValue.Bool(value),
            ),
        )
        return block
    }

    private fun emitVariableReporter(variableId: String): BlockId {
        val reporterType = VariableReporterFactory.reporterId(variableId)
        val block = instantiate(reporterType)
        val variableLabel = document.variables.variables[variableId]?.name ?: variableId
        setTextField(block, "variableId", variableId)
        setTextField(block, "variableLabel", variableLabel)
        return block
    }

    private fun ensureVariable(
        variableId: String,
        defaultValue: String?,
        label: String = variableId,
        declaredType: String? = null,
    ) {
        if (variableId in document.variables.variables) return
        val definition = VariableDefinition(
            id = variableId,
            name = label,
            type = declaredType ?: inferType(defaultValue),
            scope = VariableScope.Global,
            defaultValue = defaultValue,
        )
        apply(WorkspaceAction.CreateVariable(definition))
        registry.register(VariableReporterFactory.create(definition))
    }

    private fun inferType(defaultValue: String?): String {
        if (defaultValue == null) return "Any"
        if (defaultValue.toDoubleOrNull() != null) return "Number"
        if (defaultValue.equals("true", ignoreCase = true) || defaultValue.equals("false", ignoreCase = true)) {
            return "Boolean"
        }
        return "Text"
    }

    private fun inferExpressionType(expression: EmscriptIrExpression): String {
        return when (expression) {
            is EmscriptIrExpression.NumberLiteral -> "Number"
            is EmscriptIrExpression.BooleanLiteral -> "Boolean"
            is EmscriptIrExpression.StringLiteral -> "Text"
            is EmscriptIrExpression.VariableRef -> "Any"
            is EmscriptIrExpression.Binary -> when (expression.op) {
                EmscriptBinaryOp.OR,
                EmscriptBinaryOp.AND,
                -> "Boolean"
                EmscriptBinaryOp.ADD,
                EmscriptBinaryOp.SUB,
                EmscriptBinaryOp.MUL,
                EmscriptBinaryOp.DIV,
                EmscriptBinaryOp.MOD,
                -> "Number"
                EmscriptBinaryOp.EQ,
                EmscriptBinaryOp.NEQ,
                EmscriptBinaryOp.LT,
                EmscriptBinaryOp.LTE,
                EmscriptBinaryOp.GT,
                EmscriptBinaryOp.GTE,
                -> "Boolean"
            }
        }
    }

    private fun expressionToRepeatCount(expression: EmscriptIrExpression): Double =
        expressionToNumber(expression, fallback = 0.0)

    private fun expressionToNumber(expression: EmscriptIrExpression, fallback: Double): Double =
        when (expression) {
            is EmscriptIrExpression.NumberLiteral -> expression.value
            else -> expressionToInlineText(expression).toDoubleOrNull() ?: fallback
        }

    private fun expressionToOutputText(expression: EmscriptIrExpression): String =
        when (expression) {
            is EmscriptIrExpression.StringLiteral -> expression.value
            else -> expressionToInlineText(expression)
        }

    private fun operatorFieldValue(op: EmscriptBinaryOp): String {
        return when (op) {
            EmscriptBinaryOp.OR -> "OR"
            EmscriptBinaryOp.AND -> "AND"
            EmscriptBinaryOp.ADD -> "ADD"
            EmscriptBinaryOp.SUB -> "SUB"
            EmscriptBinaryOp.MUL -> "MUL"
            EmscriptBinaryOp.DIV -> "DIV"
            EmscriptBinaryOp.MOD -> "MOD"
            EmscriptBinaryOp.EQ -> "EQUAL"
            EmscriptBinaryOp.NEQ -> "NOT_EQUAL"
            EmscriptBinaryOp.LT -> "LESS"
            EmscriptBinaryOp.LTE -> "LESS_OR_EQUAL"
            EmscriptBinaryOp.GT -> "GREATER"
            EmscriptBinaryOp.GTE -> "GREATER_OR_EQUAL"
        }
    }

    private fun expressionToInlineText(expression: EmscriptIrExpression): String {
        return when (expression) {
            is EmscriptIrExpression.VariableRef -> expression.name
            is EmscriptIrExpression.NumberLiteral -> expression.raw
            is EmscriptIrExpression.StringLiteral -> "\"${expression.value}\""
            is EmscriptIrExpression.BooleanLiteral -> expression.value.toString()
            is EmscriptIrExpression.Binary -> {
                "(${expressionToInlineText(expression.left)} ${inlineOperator(expression.op)} ${expressionToInlineText(expression.right)})"
            }
        }
    }

    private fun inlineOperator(op: EmscriptBinaryOp): String {
        return when (op) {
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

    private fun connectNext(sourceBlock: BlockId, targetBlock: BlockId) {
        val source = document.blocks[sourceBlock]?.next?.id
            ?: error("NEXT-Verbindung fehlt bei ${sourceBlock.value}")
        val target = document.blocks[targetBlock]?.previous?.id
            ?: error("PREVIOUS-Verbindung fehlt bei ${targetBlock.value}")
        apply(WorkspaceAction.Connect(source = source, target = target))
    }

    private fun connectStatementInput(parent: BlockId, slotName: String, child: BlockId) {
        val source = document.blocks[parent]
            ?.statementInputs
            ?.firstOrNull { it.name == slotName }
            ?.connection
            ?.id
            ?: error("Statement-Slot $slotName fehlt bei ${parent.value}")
        val target = document.blocks[child]?.previous?.id
            ?: error("PREVIOUS-Verbindung fehlt bei ${child.value}")
        apply(WorkspaceAction.Connect(source = source, target = target))
    }

    private fun connectValueInput(parent: BlockId, inputName: String, child: BlockId) {
        val target = document.blocks[parent]
            ?.valueInputs
            ?.firstOrNull { it.name == inputName }
            ?.connection
            ?.id
            ?: error("Value-Input $inputName fehlt bei ${parent.value}")
        val source = document.blocks[child]?.output?.id
            ?: error("OUTPUT-Verbindung fehlt bei ${child.value}")
        apply(WorkspaceAction.Connect(source = source, target = target))
    }

    private fun setTextField(blockId: BlockId, key: String, value: String) {
        apply(
            WorkspaceAction.UpdateField(
                blockId = blockId,
                key = key,
                value = FieldValue.Text(value),
            ),
        )
    }

    private fun setNumberField(blockId: BlockId, key: String, value: Double) {
        apply(
            WorkspaceAction.UpdateField(
                blockId = blockId,
                key = key,
                value = FieldValue.Number(value),
            ),
        )
    }

    private fun instantiate(definitionId: String): BlockId {
        val before = document.blocks.keys
        apply(WorkspaceAction.InstantiateBlock(definitionId = definitionId, x = 64f, y = 64f))
        val created = document.blocks.keys - before
        return created.firstOrNull() ?: error("Block $definitionId konnte nicht instanziert werden.")
    }

    private fun apply(action: WorkspaceAction) {
        document = WorkspaceReducer.reduce(document, action, registry.asFactory())
    }
}
