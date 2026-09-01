package com.visualtasker.wss.emscript.runtime

import de.visualtasker.blockeditor.domain.ArithmeticOperator
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.domain.BlockNode
import de.visualtasker.blockeditor.domain.CompareOperator
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.domain.NormalizedOperator
import de.visualtasker.blockeditor.domain.OperatorNormalization
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceGraph
import de.visualtasker.blockeditor.domain.asString
import de.visualtasker.blockeditor.registry.BlockTypes
import com.visualtasker.wss.emscript.parser.EmscriptBinaryOp
import com.visualtasker.wss.emscript.parser.EmscriptIrExpression
import com.visualtasker.wss.emscript.parser.EmscriptIrStatement
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice

class WorkspaceDryRunRuntime(
    private val config: EmscriptDryRunConfig = EmscriptDryRunConfig(),
) {
    fun run(document: WorkspaceDocument): EmscriptDryRunResult =
        WorkspaceInterpreter(document, config).run()
}

private class WorkspaceInterpreter(
    private val document: WorkspaceDocument,
    private val config: EmscriptDryRunConfig,
) {
    private val variables = linkedMapOf<String, EmscriptValue>()
    private val events = mutableListOf<EmscriptDryRunEvent>()
    private val parser = EmscriptParserSlice()
    private var steps = 0

    fun run(): EmscriptDryRunResult =
        runCatching {
            val startRoots = WorkspaceGraph.topLevelRoots(document).filter { id ->
                document.blocks[id]?.type == BlockTypes.EVENT_START
            }
            startRoots.forEach { start ->
                emitBlock(start, "start", "Script Start")
                WorkspaceGraph.nextChain(document, start)?.let { next ->
                    emitEdge(start, next, "SEQUENCE")
                    executeChain(next)
                }
            }
            emit("done", "Workspace Dry-Run abgeschlossen: ${events.size} Events, ${variables.size} Variablen.")
            EmscriptDryRunResult.Success(events.toList(), variables.toMap())
        }.getOrElse { error ->
            EmscriptDryRunResult.Failure(
                message = error.message ?: "Workspace Dry-Run fehlgeschlagen.",
                events = events.toList(),
            )
        }

    private fun executeChain(headId: BlockId) {
        var current: BlockId? = headId
        while (current != null) {
            execute(current)
            val previous = current
            current = WorkspaceGraph.nextChain(document, current)
            if (current != null) {
                emitEdge(previous, current, edgeKindFromNext(previous))
            }
        }
    }

    private fun execute(blockId: BlockId) {
        guardStep()
        val block = document.blocks[blockId] ?: error("Block ${blockId.value} fehlt.")
        when (block.type) {
            BlockTypes.ACTION_CLICK_TEXT -> emitBlock(blockId, "click", "würde Text \"${block.fieldText("text")}\" anklicken")
            BlockTypes.ACTION_WAIT -> emitBlock(blockId, "wait", "würde ${block.fieldNumber("ms").toLong().coerceAtLeast(0L)} ms warten")
            BlockTypes.DEBUG_LOG -> emitBlock(blockId, "log", block.fieldText("message"))
            BlockTypes.FEEDBACK_BEEP -> {
                val hz = block.fieldNumber("frequency").toInt().coerceIn(20, 20_000)
                val duration = block.fieldNumber("durationMs").toInt().coerceIn(10, 10_000)
                val volume = block.fieldNumber("volume").toInt().coerceIn(0, 100)
                emitBlock(blockId, "beep", "würde Beep ${hz}Hz/${duration}ms/${volume}% abspielen")
            }
            BlockTypes.FEEDBACK_VIBRATE -> {
                val pattern = block.fieldLongList("pattern").joinToString(",")
                emitBlock(blockId, "vibrate", "würde Vibrationsmuster $pattern ms starten")
            }
            BlockTypes.VARIABLE_SET -> {
                val variable = block.fieldText("variable").ifBlank { "variable" }
                val value = evaluateInlineText(block.fieldText("value"))
                variables[variable] = value
                emitBlock(blockId, block.fieldText("assignmentKind").ifBlank { "set" }.lowercase(), "$variable = ${value.renderDryRun()}")
            }
            BlockTypes.CONTROL_REPEAT -> {
                val count = block.fieldNumber("times").toLong().coerceAtLeast(0L)
                if (count > config.maxLoopIterations) {
                    error("LOOP $count überschreitet Limit ${config.maxLoopIterations}.")
                }
                repeat(count.toInt()) { index ->
                    emitBlock(blockId, "loop", "Iteration ${index + 1}/$count")
                    executeStatementSlot(blockId, BlockTypes.SLOT_DO, "LOOP_BODY")
                }
            }
            BlockTypes.CONTROL_WHILE -> {
                var iterations = 0
                while (evaluateValueInput(block, "CONDITION").asBooleanDryRun("while")) {
                    iterations += 1
                    if (iterations > config.maxLoopIterations) {
                        error("WHILE nach ${config.maxLoopIterations} Iterationen abgebrochen.")
                    }
                    emitBlock(blockId, "while", "Iteration $iterations")
                    executeStatementSlot(blockId, BlockTypes.SLOT_BODY, "LOOP_BODY")
                }
            }
            BlockTypes.CONTROL_IF,
            BlockTypes.CONTROL_IF_ELSE,
            BlockTypes.CONTROL_IF_ELSEIF_ELSE -> executeIf(blockId, block)
            else -> emitBlock(blockId, "unsupported", "Unsupported block type: ${block.type}")
        }
    }

    private fun executeIf(blockId: BlockId, block: BlockNode) {
        val condition = evaluateValueInput(block, "CONDITION").asBooleanDryRun("if")
        if (condition) {
            emitBlock(blockId, "if", "THEN")
            executeStatementSlot(blockId, BlockTypes.SLOT_THEN, "TRUE_BRANCH")
            return
        }
        val elseIfCondition = if (block.valueInputs.any { it.name == "ELIF_CONDITION" }) {
            evaluateValueInput(block, "ELIF_CONDITION").asBooleanDryRun("elseif")
        } else {
            false
        }
        if (elseIfCondition) {
            emitBlock(blockId, "elseif", "ELSEIF")
            executeStatementSlot(blockId, BlockTypes.SLOT_ELIF, "ELSE_IF_BRANCH")
            return
        }
        emitBlock(blockId, "else", "ELSE")
        executeStatementSlot(blockId, BlockTypes.SLOT_ELSE, "FALSE_BRANCH")
    }

    private fun executeStatementSlot(parentId: BlockId, slotName: String, edgeKind: String) {
        val head = WorkspaceGraph.statementStackHead(document, parentId, slotName) ?: return
        emitEdge(parentId, head, edgeKind)
        executeChain(head)
    }

    private fun evaluateValueInput(block: BlockNode, inputName: String): EmscriptValue {
        val input = block.valueInputs.firstOrNull { it.name == inputName } ?: return EmscriptValue.BooleanValue(false)
        val connected = input.connection.connectedTo ?: return EmscriptValue.BooleanValue(false)
        val (valueBlockId, _) = WorkspaceGraph.findConnection(document, connected) ?: return EmscriptValue.BooleanValue(false)
        emitEdge(valueBlockId, block.id, edgeKindFromValueInput(inputName))
        return evaluateExpression(valueBlockId)
    }

    private fun evaluateExpression(blockId: BlockId): EmscriptValue {
        guardStep()
        val block = document.blocks[blockId] ?: return EmscriptValue.NullValue
        return when (block.type) {
            BlockTypes.LOGIC_BOOLEAN,
            BlockTypes.LITERAL_BOOLEAN -> block.fieldBool("value").also {
                emitBlock(blockId, "reporter", it.toString())
            }.let(EmscriptValue::BooleanValue)
            BlockTypes.LITERAL_NUMBER -> block.fieldNumber("value").also {
                emitBlock(blockId, "reporter", it.renderNumber())
            }.let(EmscriptValue::NumberValue)
            BlockTypes.LITERAL_STRING -> block.fieldText("value").also {
                emitBlock(blockId, "reporter", it)
            }.let(EmscriptValue::StringValue)
            BlockTypes.VARIABLE_GET,
            BlockTypes.VARIABLE_VALUE,
            BlockTypes.VARIABLES_GET -> variableValue(blockId, block.fieldText("variable"))
            BlockTypes.LOGIC_COMPARE -> evaluateCompare(blockId, block)
            BlockTypes.LOGIC_OPERATE -> evaluateOperate(blockId, block)
            else -> if (block.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX)) {
                variableValue(blockId, block.fieldText("variable"))
            } else {
                emitBlock(blockId, "reporter", "unsupported ${block.type}")
                EmscriptValue.NullValue
            }
        }
    }

    private fun variableValue(blockId: BlockId, variable: String): EmscriptValue {
        val value = variables[variable] ?: EmscriptValue.NullValue
        emitBlock(blockId, "variable", "$variable = ${value.renderDryRun()}")
        return value
    }

    private fun evaluateCompare(blockId: BlockId, block: BlockNode): EmscriptValue {
        val left = evaluateValueInput(block, "LEFT")
        val right = evaluateValueInput(block, "RIGHT")
        val op = when (val normalized = OperatorNormalization.normalize(block.fieldText("operator"))) {
            is NormalizedOperator.Compare -> normalized.value
            else -> error("Unsupported compare operator: ${block.fieldText("operator")}")
        }
        val result = when (op) {
            CompareOperator.EQUAL -> left.renderDryRun() == right.renderDryRun()
            CompareOperator.NOT_EQUAL -> left.renderDryRun() != right.renderDryRun()
            CompareOperator.LESS -> left.asDoubleDryRun("<") < right.asDoubleDryRun("<")
            CompareOperator.LESS_OR_EQUAL -> left.asDoubleDryRun("<=") <= right.asDoubleDryRun("<=")
            CompareOperator.GREATER -> left.asDoubleDryRun(">") > right.asDoubleDryRun(">")
            CompareOperator.GREATER_OR_EQUAL -> left.asDoubleDryRun(">=") >= right.asDoubleDryRun(">=")
        }
        emitBlock(blockId, "compare", "${left.renderDryRun()} ${op.symbol} ${right.renderDryRun()} = $result")
        return EmscriptValue.BooleanValue(result)
    }

    private fun evaluateOperate(blockId: BlockId, block: BlockNode): EmscriptValue {
        val left = evaluateValueInput(block, "Input1")
        val right = evaluateValueInput(block, "Input2")
        val op = when (val normalized = OperatorNormalization.normalize(block.fieldText("operator"))) {
            is NormalizedOperator.Arithmetic -> normalized.value
            else -> error("Unsupported operate operator: ${block.fieldText("operator")}")
        }
        val result = when (op) {
            ArithmeticOperator.ADD -> left.asDoubleDryRun("+") + right.asDoubleDryRun("+")
            ArithmeticOperator.SUB -> left.asDoubleDryRun("-") - right.asDoubleDryRun("-")
            ArithmeticOperator.MUL -> left.asDoubleDryRun("*") * right.asDoubleDryRun("*")
            ArithmeticOperator.DIV -> left.asDoubleDryRun("/") / right.asDoubleDryRun("/")
            ArithmeticOperator.MOD -> left.asDoubleDryRun("%") % right.asDoubleDryRun("%")
        }
        emitBlock(blockId, "operate", "${left.renderDryRun()} ${op.symbol} ${right.renderDryRun()} = ${result.renderNumber()}")
        return EmscriptValue.NumberValue(result)
    }

    private fun evaluateInlineText(value: String): EmscriptValue {
        val trimmed = value.trim()
        parseInlineExpression(trimmed)?.let { return evaluateParsedExpression(it) }
        return when {
            trimmed.equals("true", ignoreCase = true) -> EmscriptValue.BooleanValue(true)
            trimmed.equals("false", ignoreCase = true) -> EmscriptValue.BooleanValue(false)
            trimmed.toDoubleOrNull() != null -> EmscriptValue.NumberValue(trimmed.toDouble())
            trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2 ->
                EmscriptValue.StringValue(trimmed.substring(1, trimmed.length - 1))
            trimmed in variables -> variables.getValue(trimmed)
            else -> EmscriptValue.StringValue(trimmed)
        }
    }

    private fun parseInlineExpression(value: String): EmscriptIrExpression? {
        if (value.isBlank()) return null
        val parsed = parser.parse("SET __trace = $value")
        val statement = parsed.ir?.statements?.singleOrNull() as? EmscriptIrStatement.Set
        return statement?.value
    }

    private fun evaluateParsedExpression(expression: EmscriptIrExpression): EmscriptValue =
        when (expression) {
            is EmscriptIrExpression.VariableRef -> variables[expression.name] ?: EmscriptValue.NullValue
            is EmscriptIrExpression.NumberLiteral -> EmscriptValue.NumberValue(expression.value)
            is EmscriptIrExpression.StringLiteral -> EmscriptValue.StringValue(expression.value)
            is EmscriptIrExpression.BooleanLiteral -> EmscriptValue.BooleanValue(expression.value)
            is EmscriptIrExpression.Binary -> {
                val left = evaluateParsedExpression(expression.left)
                val right = evaluateParsedExpression(expression.right)
                when (expression.op) {
                    EmscriptBinaryOp.ADD -> EmscriptValue.NumberValue(left.asDoubleDryRun("+") + right.asDoubleDryRun("+"))
                    EmscriptBinaryOp.SUB -> EmscriptValue.NumberValue(left.asDoubleDryRun("-") - right.asDoubleDryRun("-"))
                    EmscriptBinaryOp.MUL -> EmscriptValue.NumberValue(left.asDoubleDryRun("*") * right.asDoubleDryRun("*"))
                    EmscriptBinaryOp.DIV -> EmscriptValue.NumberValue(left.asDoubleDryRun("/") / right.asDoubleDryRun("/"))
                    EmscriptBinaryOp.MOD -> EmscriptValue.NumberValue(left.asDoubleDryRun("%") % right.asDoubleDryRun("%"))
                    EmscriptBinaryOp.EQ -> EmscriptValue.BooleanValue(left.renderDryRun() == right.renderDryRun())
                    EmscriptBinaryOp.NEQ -> EmscriptValue.BooleanValue(left.renderDryRun() != right.renderDryRun())
                    EmscriptBinaryOp.LT -> EmscriptValue.BooleanValue(left.asDoubleDryRun("<") < right.asDoubleDryRun("<"))
                    EmscriptBinaryOp.LTE -> EmscriptValue.BooleanValue(left.asDoubleDryRun("<=") <= right.asDoubleDryRun("<="))
                    EmscriptBinaryOp.GT -> EmscriptValue.BooleanValue(left.asDoubleDryRun(">") > right.asDoubleDryRun(">"))
                    EmscriptBinaryOp.GTE -> EmscriptValue.BooleanValue(left.asDoubleDryRun(">=") >= right.asDoubleDryRun(">="))
                }
            }
        }

    private fun edgeKindFromNext(sourceId: BlockId): String =
        when (document.blocks[sourceId]?.type) {
            BlockTypes.CONTROL_REPEAT,
            BlockTypes.CONTROL_WHILE -> "LOOP_EXIT"
            else -> "SEQUENCE"
        }

    private fun edgeKindFromValueInput(inputName: String): String =
        if (inputName.endsWith("CONDITION")) "CONDITION" else "DATA_FLOW"

    private fun guardStep() {
        steps += 1
        if (steps > config.maxSteps) {
            error("Dry-Run nach ${config.maxSteps} Schritten abgebrochen.")
        }
    }

    private fun emitBlock(blockId: BlockId, kind: String, message: String) {
        events += EmscriptDryRunEvent(
            index = events.size + 1,
            kind = kind,
            message = message,
            blockId = blockId.value,
        )
    }

    private fun emitEdge(source: BlockId, target: BlockId, kind: String) {
        events += EmscriptDryRunEvent(
            index = events.size + 1,
            kind = "edge",
            message = "$kind ${source.value} -> ${target.value}",
            edgeSourceBlockId = source.value,
            edgeTargetBlockId = target.value,
            edgeKind = kind,
        )
    }

    private fun emit(kind: String, message: String) {
        events += EmscriptDryRunEvent(events.size + 1, kind, message)
    }

    private fun BlockNode.fieldText(key: String): String =
        fields[key]?.asString().orEmpty()

    private fun BlockNode.fieldNumber(key: String): Double =
        when (val value = fields[key]) {
            is FieldValue.Number -> value.value
            is FieldValue.Text -> value.value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    private fun BlockNode.fieldLongList(key: String): List<Long> =
        fieldText(key)
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .ifEmpty { listOf(fieldNumber(key).toLong()) }

    private fun BlockNode.fieldBool(key: String): Boolean =
        when (val value = fields[key]) {
            is FieldValue.Bool -> value.value
            is FieldValue.Text -> value.value.equals("true", ignoreCase = true)
            else -> false
        }
}

private fun EmscriptValue.asDoubleDryRun(context: String): Double =
    when (this) {
        is EmscriptValue.NumberValue -> value
        is EmscriptValue.BooleanValue -> if (value) 1.0 else 0.0
        is EmscriptValue.StringValue -> value.toDoubleOrNull() ?: error("$context erwartet Zahl, erhalten: \"$value\"")
        EmscriptValue.NullValue -> 0.0
    }

private fun EmscriptValue.asBooleanDryRun(context: String): Boolean =
    when (this) {
        is EmscriptValue.BooleanValue -> value
        is EmscriptValue.NumberValue -> value != 0.0
        is EmscriptValue.StringValue -> value.isNotEmpty()
        EmscriptValue.NullValue -> false
    }

private fun EmscriptValue.renderDryRun(): String =
    when (this) {
        is EmscriptValue.NumberValue -> value.renderNumber()
        is EmscriptValue.StringValue -> value
        is EmscriptValue.BooleanValue -> value.toString()
        EmscriptValue.NullValue -> "null"
    }

private fun Double.renderNumber(): String =
    if (isFinite() && this % 1.0 == 0.0) toLong().toString() else toString()
