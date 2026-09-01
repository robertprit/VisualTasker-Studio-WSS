package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptBinaryOp
import com.visualtasker.wss.emscript.parser.EmscriptIrExpression
import com.visualtasker.wss.emscript.parser.EmscriptIrScript
import com.visualtasker.wss.emscript.parser.EmscriptIrStatement
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice

data class EmscriptDryRunConfig(
    val maxSteps: Int = 2_000,
    val maxLoopIterations: Int = 500,
)

data class EmscriptDryRunEvent(
    val index: Int,
    val kind: String,
    val message: String,
    val blockId: String? = null,
    val edgeSourceBlockId: String? = null,
    val edgeTargetBlockId: String? = null,
    val edgeKind: String? = null,
)

sealed interface EmscriptDryRunResult {
    data class Success(
        val events: List<EmscriptDryRunEvent>,
        val variables: Map<String, EmscriptValue>,
    ) : EmscriptDryRunResult

    data class Failure(
        val message: String,
        val events: List<EmscriptDryRunEvent> = emptyList(),
    ) : EmscriptDryRunResult
}

sealed interface EmscriptValue {
    data class NumberValue(val value: Double) : EmscriptValue
    data class StringValue(val value: String) : EmscriptValue
    data class BooleanValue(val value: Boolean) : EmscriptValue
    data object NullValue : EmscriptValue
}

class EmscriptDryRunRuntime(
    private val parser: EmscriptParserSlice = EmscriptParserSlice(),
    private val config: EmscriptDryRunConfig = EmscriptDryRunConfig(),
) {
    fun run(script: String): EmscriptDryRunResult {
        val parsed = parser.parse(script)
        val ir = parsed.ir ?: return EmscriptDryRunResult.Failure(
            message = parsed.issues.joinToString(separator = "\n") { issue ->
                "${issue.line}:${issue.column} ${issue.message}"
            }.ifBlank { "Parse fehlgeschlagen." },
        )
        return Interpreter(config).run(ir)
    }
}

private class Interpreter(
    private val config: EmscriptDryRunConfig,
) {
    private val variables = linkedMapOf<String, EmscriptValue>()
    private val events = mutableListOf<EmscriptDryRunEvent>()
    private var steps = 0

    fun run(script: EmscriptIrScript): EmscriptDryRunResult =
        runCatching {
            execute(script.statements)
            emit("done", "Dry-Run abgeschlossen: ${events.size} Events, ${variables.size} Variablen.")
            EmscriptDryRunResult.Success(events.toList(), variables.toMap())
        }.getOrElse { error ->
            EmscriptDryRunResult.Failure(
                message = error.message ?: "Dry-Run fehlgeschlagen.",
                events = events.toList(),
            )
        }

    private fun execute(statements: List<EmscriptIrStatement>) {
        statements.forEach(::execute)
    }

    private fun execute(statement: EmscriptIrStatement) {
        guardStep()
        when (statement) {
            is EmscriptIrStatement.Let -> {
                val value = evaluate(statement.value)
                variables[statement.variable] = value
                emit("let", "${statement.variable} = ${value.render()}")
            }
            is EmscriptIrStatement.Set -> {
                val value = evaluate(statement.value)
                variables[statement.variable] = value
                emit("set", "${statement.variable} = ${value.render()}")
            }
            is EmscriptIrStatement.Wait -> {
                val ms = evaluate(statement.milliseconds).asLong("wait")
                emit("wait", "würde ${ms.coerceAtLeast(0L)} ms warten")
            }
            is EmscriptIrStatement.ClickText -> {
                emit("click", "würde Text \"${statement.text}\" anklicken")
            }
            is EmscriptIrStatement.Output -> {
                emit("log", evaluate(statement.value).render())
            }
            is EmscriptIrStatement.Beep -> {
                val hz = statement.frequency ?: 1_000
                val duration = statement.durationMs ?: 200
                val volume = statement.volume ?: 100
                emit("beep", "würde Beep ${hz}Hz/${duration}ms/${volume}% abspielen")
            }
            is EmscriptIrStatement.Vibrate -> {
                emit("vibrate", "würde Vibrationsmuster ${statement.pattern.joinToString(",")} ms starten")
            }
            is EmscriptIrStatement.Loop -> {
                val count = evaluate(statement.times).asLong("loop").coerceAtLeast(0L)
                repeatLoop(count, statement.body)
            }
            is EmscriptIrStatement.While -> {
                var iterations = 0
                while (evaluate(statement.condition).asBoolean("while")) {
                    iterations += 1
                    if (iterations > config.maxLoopIterations) {
                        error("WHILE nach ${config.maxLoopIterations} Iterationen abgebrochen.")
                    }
                    emit("while", "Iteration $iterations")
                    execute(statement.body)
                }
            }
            is EmscriptIrStatement.If -> {
                when {
                    evaluate(statement.condition).asBoolean("if") -> {
                        emit("if", "THEN")
                        execute(statement.thenBranch)
                    }
                    else -> {
                        val elseIf = statement.elseIfBranches.firstOrNull {
                            evaluate(it.condition).asBoolean("elseif")
                        }
                        if (elseIf != null) {
                            emit("elseif", "ELSEIF")
                            execute(elseIf.body)
                        } else {
                            emit("else", "ELSE")
                            execute(statement.elseBranch)
                        }
                    }
                }
            }
        }
    }

    private fun repeatLoop(count: Long, body: List<EmscriptIrStatement>) {
        if (count > config.maxLoopIterations) {
            error("LOOP $count überschreitet Limit ${config.maxLoopIterations}.")
        }
        repeat(count.toInt()) { index ->
            emit("loop", "Iteration ${index + 1}/$count")
            execute(body)
        }
    }

    private fun evaluate(expression: EmscriptIrExpression): EmscriptValue =
        when (expression) {
            is EmscriptIrExpression.VariableRef -> variables[expression.name] ?: EmscriptValue.NullValue
            is EmscriptIrExpression.NumberLiteral -> EmscriptValue.NumberValue(expression.value)
            is EmscriptIrExpression.StringLiteral -> EmscriptValue.StringValue(expression.value)
            is EmscriptIrExpression.BooleanLiteral -> EmscriptValue.BooleanValue(expression.value)
            is EmscriptIrExpression.Binary -> evaluateBinary(expression)
        }

    private fun evaluateBinary(expression: EmscriptIrExpression.Binary): EmscriptValue {
        val left = evaluate(expression.left)
        val right = evaluate(expression.right)
        return when (expression.op) {
            EmscriptBinaryOp.OR -> EmscriptValue.BooleanValue(left.asBoolean("||") || right.asBoolean("||"))
            EmscriptBinaryOp.AND -> EmscriptValue.BooleanValue(left.asBoolean("&&") && right.asBoolean("&&"))
            EmscriptBinaryOp.ADD -> EmscriptValue.NumberValue(left.asDouble("+") + right.asDouble("+"))
            EmscriptBinaryOp.SUB -> EmscriptValue.NumberValue(left.asDouble("-") - right.asDouble("-"))
            EmscriptBinaryOp.MUL -> EmscriptValue.NumberValue(left.asDouble("*") * right.asDouble("*"))
            EmscriptBinaryOp.DIV -> EmscriptValue.NumberValue(left.asDouble("/") / right.asDouble("/"))
            EmscriptBinaryOp.MOD -> EmscriptValue.NumberValue(left.asDouble("%") % right.asDouble("%"))
            EmscriptBinaryOp.EQ -> EmscriptValue.BooleanValue(left.render() == right.render())
            EmscriptBinaryOp.NEQ -> EmscriptValue.BooleanValue(left.render() != right.render())
            EmscriptBinaryOp.LT -> EmscriptValue.BooleanValue(left.asDouble("<") < right.asDouble("<"))
            EmscriptBinaryOp.LTE -> EmscriptValue.BooleanValue(left.asDouble("<=") <= right.asDouble("<="))
            EmscriptBinaryOp.GT -> EmscriptValue.BooleanValue(left.asDouble(">") > right.asDouble(">"))
            EmscriptBinaryOp.GTE -> EmscriptValue.BooleanValue(left.asDouble(">=") >= right.asDouble(">="))
        }
    }

    private fun guardStep() {
        steps += 1
        if (steps > config.maxSteps) {
            error("Dry-Run nach ${config.maxSteps} Schritten abgebrochen.")
        }
    }

    private fun emit(kind: String, message: String) {
        events += EmscriptDryRunEvent(events.size + 1, kind, message)
    }
}

private fun EmscriptValue.asDouble(context: String): Double =
    when (this) {
        is EmscriptValue.NumberValue -> value
        is EmscriptValue.BooleanValue -> if (value) 1.0 else 0.0
        is EmscriptValue.StringValue -> value.toDoubleOrNull() ?: error("$context erwartet Zahl, erhalten: \"$value\"")
        EmscriptValue.NullValue -> 0.0
    }

private fun EmscriptValue.asLong(context: String): Long =
    asDouble(context).toLong()

private fun EmscriptValue.asBoolean(context: String): Boolean =
    when (this) {
        is EmscriptValue.BooleanValue -> value
        is EmscriptValue.NumberValue -> value != 0.0
        is EmscriptValue.StringValue -> value.isNotEmpty()
        EmscriptValue.NullValue -> false
    }

private fun EmscriptValue.render(): String =
    when (this) {
        is EmscriptValue.NumberValue -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is EmscriptValue.StringValue -> value
        is EmscriptValue.BooleanValue -> value.toString()
        EmscriptValue.NullValue -> "null"
    }
