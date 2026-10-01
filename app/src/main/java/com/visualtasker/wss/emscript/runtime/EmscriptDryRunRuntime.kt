package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptBinaryOp
import com.visualtasker.wss.emscript.parser.EmscriptIrExpression
import com.visualtasker.wss.emscript.parser.EmscriptIrScript
import com.visualtasker.wss.emscript.parser.EmscriptIrStatement
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import de.visualtasker.blockeditor.registry.CommandCapability
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import de.visualtasker.blockeditor.registry.toCapabilityDescriptor
import de.visualtasker.emscript.contract.CoreTypes
import de.visualtasker.emscript.contract.LanguageTypeCompatibility
import de.visualtasker.emscript.contract.LanguageTypeRef

data class EmscriptDryRunConfig(
    val maxSteps: Int = 2_000,
    val maxLoopIterations: Int = 500,
)

enum class EmscriptDryRunEventSeverity {
    INFO,
    WARNING,
    ERROR,
}

data class EmscriptDryRunEvent(
    val index: Int,
    val kind: String,
    val message: String,
    val blockId: String? = null,
    val edgeSourceBlockId: String? = null,
    val edgeTargetBlockId: String? = null,
    val edgeKind: String? = null,
    val severity: EmscriptDryRunEventSeverity = EmscriptDryRunEventSeverity.INFO,
    val command: String? = null,
    val capability: String? = null,
    val pluginOwner: String? = null,
    val diagnosticCode: String? = null,
    val sourceLine: Int? = null,
    val numericArguments: List<Long> = emptyList(),
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
    private val parser: EmscriptParserSlice = EmscriptParserSlice(includeSourceSpans = true),
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
    private val variableTypes = linkedMapOf<String, LanguageTypeRef>()
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
            is EmscriptIrStatement.CommandCall -> {
                if (statement.command.equals("vibrate", ignoreCase = true)) {
                    val pattern = statement.expressionArguments.mapIndexed { index, expression ->
                        evaluate(expression).asLong("vibrate Parameter ${index + 1}")
                    }
                    emit(
                        kind = "vibrate",
                        message = "würde Vibrationsmuster ${pattern.joinToString(",")} ms starten",
                        sourceLine = statement.source?.startLine,
                        numericArguments = pattern,
                    )
                } else {
                    emitCommand(statement.command, statement.arguments, statement.source?.startLine)
                }
            }
            is EmscriptIrStatement.Let -> {
                val value = evaluate(statement.value)
                val declaredType = statement.declaredType ?: value.runtimeType()
                EmscriptRuntimeTypeSafety.requireMatches(
                    value = value,
                    expectedType = LanguageTypeCompatibility.sourceName(declaredType),
                    context = "LET ${statement.variable}",
                )
                variables[statement.variable] = value
                variableTypes[statement.variable] = declaredType
                emit("let", "${statement.variable} = ${value.render()}", sourceLine = statement.source?.startLine)
            }
            is EmscriptIrStatement.Set -> {
                val value = evaluate(statement.value)
                variableTypes[statement.variable]?.let { expected ->
                    EmscriptRuntimeTypeSafety.requireMatches(
                        value = value,
                        expectedType = LanguageTypeCompatibility.sourceName(expected),
                        context = "SET ${statement.variable}",
                    )
                }
                variables[statement.variable] = value
                emit("set", "${statement.variable} = ${value.render()}", sourceLine = statement.source?.startLine)
            }
            is EmscriptIrStatement.Wait -> {
                val ms = evaluate(statement.milliseconds).asLong("wait")
                emit("wait", "würde ${ms.coerceAtLeast(0L)} ms warten", sourceLine = statement.source?.startLine)
            }
            is EmscriptIrStatement.ClickText -> {
                emit("click", "würde Text \"${statement.text}\" anklicken", sourceLine = statement.source?.startLine)
            }
            is EmscriptIrStatement.Output -> {
                emit("log", evaluate(statement.value).render(), sourceLine = statement.source?.startLine)
            }
            is EmscriptIrStatement.Beep -> {
                val hz = statement.frequency ?: 1_000
                val duration = statement.durationMs ?: 200
                val volume = statement.volume ?: 100
                emit("beep", "würde Beep ${hz}Hz/${duration}ms/${volume}% abspielen", sourceLine = statement.source?.startLine)
            }
            is EmscriptIrStatement.Vibrate -> {
                emit("vibrate", "würde Vibrationsmuster ${statement.pattern.joinToString(",")} ms starten", sourceLine = statement.source?.startLine)
            }
            is EmscriptIrStatement.Loop -> {
                val count = evaluate(statement.times).asLong("loop").coerceAtLeast(0L)
                repeatLoop(count, statement.body, statement.source?.startLine)
            }
            is EmscriptIrStatement.While -> {
                var iterations = 0
                requireBooleanExpression(statement.condition, "WHILE")
                while (evaluate(statement.condition).asBoolean("while")) {
                    iterations += 1
                    if (iterations > config.maxLoopIterations) {
                        error("WHILE nach ${config.maxLoopIterations} Iterationen abgebrochen.")
                    }
                    emit("while", "Iteration $iterations", sourceLine = statement.source?.startLine)
                    execute(statement.body)
                }
            }
            is EmscriptIrStatement.If -> {
                requireBooleanExpression(statement.condition, "IF")
                when {
                    evaluate(statement.condition).asBoolean("if") -> {
                        emit("if", "THEN", sourceLine = statement.source?.startLine)
                        execute(statement.thenBranch)
                    }
                    else -> {
                        val elseIf = statement.elseIfBranches.firstOrNull {
                            requireBooleanExpression(it.condition, "ELSEIF")
                            evaluate(it.condition).asBoolean("elseif")
                        }
                        if (elseIf != null) {
                            emit("elseif", "ELSEIF", sourceLine = elseIf.source?.startLine ?: statement.source?.startLine)
                            execute(elseIf.body)
                        } else {
                            emit("else", "ELSE", sourceLine = statement.source?.startLine)
                            execute(statement.elseBranch)
                        }
                    }
                }
            }
        }
    }

    private fun repeatLoop(count: Long, body: List<EmscriptIrStatement>, sourceLine: Int?) {
        if (count > config.maxLoopIterations) {
            error("LOOP $count überschreitet Limit ${config.maxLoopIterations}.")
        }
        repeat(count.toInt()) { index ->
            emit("loop", "Iteration ${index + 1}/$count", sourceLine = sourceLine)
            execute(body)
        }
    }

    private fun evaluate(expression: EmscriptIrExpression): EmscriptValue =
        when (expression) {
            is EmscriptIrExpression.VariableRef -> variables[expression.name] ?: EmscriptValue.NullValue
            is EmscriptIrExpression.NumberLiteral -> EmscriptValue.NumberValue(expression.value)
            is EmscriptIrExpression.StringLiteral -> EmscriptValue.StringValue(expression.value)
            is EmscriptIrExpression.BooleanLiteral -> EmscriptValue.BooleanValue(expression.value)
            is EmscriptIrExpression.FunctionCall -> evaluateFunctionCall(expression)
            is EmscriptIrExpression.Binary -> evaluateBinary(expression)
        }

    private fun evaluateFunctionCall(expression: EmscriptIrExpression.FunctionCall): EmscriptValue {
        if (expression.name.equals("region", ignoreCase = true) || expression.name.equals("bbox", ignoreCase = true)) {
            val values = expression.arguments.map { evaluate(it).asDouble("region") }
            require(values.size == 4) { "region erwartet vier Number-Argumente" }
            return EmscriptValue.StringValue(
                values.joinToString(prefix = "region(", postfix = ")") { value ->
                    EmscriptValue.NumberValue(value).render()
                },
            )
        }
        val arguments = expression.arguments.joinToString(",") { evaluate(it).render() }
        val entry = VisualTaskerCommandCatalog.findByCanonicalName(expression.name)
            ?: VisualTaskerCommandCatalog.findByAcceptedName(expression.name)
        val gate = entry?.runtime?.liveCapabilityGate
        val message = when {
            entry == null -> "Reporter ${expression.name}($arguments) ist nicht im Katalog."
            gate.isRuntimeBlocked() -> "Reporter live blockiert: ${entry.canonicalName}($arguments) [${gate?.name ?: "UNKNOWN"}]"
            else -> "würde Reporter ${entry.canonicalName}($arguments) auswerten"
        }
        events += EmscriptDryRunEvent(
            index = events.size + 1,
            kind = "reporter",
            message = message,
            severity = if (gate.isRuntimeBlocked()) EmscriptDryRunEventSeverity.WARNING else EmscriptDryRunEventSeverity.INFO,
            command = entry?.canonicalName ?: expression.name,
            capability = gate?.name,
            pluginOwner = entry?.pluginOwner,
        )
        val returnType = entry?.returnType
        if (returnType == null || returnType.equals("Void", ignoreCase = true)) {
            error("${expression.name} ist kein wertliefernder Ausdruck")
        }
        return EmscriptRuntimeTypeSafety.defaultValue(returnType)
    }

    private fun requireBooleanExpression(expression: EmscriptIrExpression, context: String) {
        val type = when (expression) {
            is EmscriptIrExpression.VariableRef -> variableTypes[expression.name]
            is EmscriptIrExpression.BooleanLiteral,
            is EmscriptIrExpression.Binary,
            -> CoreTypes.BOOL.ref
            is EmscriptIrExpression.FunctionCall -> VisualTaskerCommandCatalog
                .findByAcceptedName(expression.name)
                ?.returnType
                ?.let(LanguageTypeCompatibility::fromWorkspaceName)
            else -> null
        }
        require(type == null || LanguageTypeCompatibility.isAssignable(type, CoreTypes.BOOL.ref)) {
            "${EmscriptRuntimeTypeSafety.NULLABLE_VALUE_IN_NONNULL_CONTEXT}: $context erwartet Bool"
        }
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

    private fun emit(
        kind: String,
        message: String,
        sourceLine: Int? = null,
        numericArguments: List<Long> = emptyList(),
    ) {
        events += EmscriptDryRunEvent(
            index = events.size + 1,
            kind = kind,
            message = message,
            sourceLine = sourceLine,
            numericArguments = numericArguments,
        )
    }

    private fun emitCommand(command: String, arguments: String, sourceLine: Int?) {
        val entry = VisualTaskerCommandCatalog.findByCanonicalName(command)
            ?: VisualTaskerCommandCatalog.findByAcceptedName(command)
        val gate = entry?.runtime?.liveCapabilityGate
        val descriptor = entry?.toCapabilityDescriptor()
        val basicReady = entry?.isBasicRuntimeReady() == true
        val adapterGated = !basicReady && entry?.runtime?.dryRunBehavior == "adapter-gated"
        val pluginOwner = entry?.pluginOwner
        val severity = if (!basicReady && (adapterGated || gate.isRuntimeBlocked())) {
            EmscriptDryRunEventSeverity.WARNING
        } else {
            EmscriptDryRunEventSeverity.INFO
        }
        val kind = if (severity == EmscriptDryRunEventSeverity.WARNING) "capability" else "command"
        val detail = when {
            entry == null -> "bekannt im Parser, aber kein Katalogeintrag für $command"
            adapterGated -> "Adapter noch nicht live: ${entry.canonicalName}(${arguments}) [${gate?.name ?: "UNKNOWN"} via ${entry.pluginOwner}]"
            gate.isRuntimeBlocked() -> "Live-Capability noch blockiert: ${entry.canonicalName}(${arguments}) [${gate?.name ?: "UNKNOWN"}]"
            else -> "würde ${entry.canonicalName}(${arguments}) ausführen"
        }
        events += EmscriptDryRunEvent(
            index = events.size + 1,
            kind = kind,
            message = detail,
            severity = severity,
            command = entry?.canonicalName ?: command,
            capability = gate?.name,
            pluginOwner = pluginOwner,
            sourceLine = sourceLine,
            diagnosticCode = when {
                entry == null -> "CAPABILITY_CATALOG_MISSING"
                severity == EmscriptDryRunEventSeverity.WARNING -> descriptor?.diagnosticCode ?: "CAPABILITY_BLOCKED"
                else -> null
            },
        )
    }
}

internal fun CommandCapability?.isRuntimeBlocked(): Boolean =
    this != null &&
        this !in setOf(
            CommandCapability.CORE,
            CommandCapability.TIMING,
            CommandCapability.FEEDBACK,
            CommandCapability.A11Y,
            CommandCapability.SCREEN_CAPTURE,
            CommandCapability.DEBUG,
        )

private fun EmscriptValue.asDouble(context: String): Double =
    when (this) {
        is EmscriptValue.NumberValue -> value
        is EmscriptValue.BooleanValue -> if (value) 1.0 else 0.0
        is EmscriptValue.StringValue -> value.toDoubleOrNull() ?: error("$context erwartet Zahl, erhalten: \"$value\"")
        EmscriptValue.NullValue -> error(
            "${EmscriptRuntimeTypeSafety.NULLABLE_VALUE_IN_NONNULL_CONTEXT}: $context erwartet Number",
        )
    }

private fun EmscriptValue.asLong(context: String): Long =
    asDouble(context).toLong()

private fun EmscriptValue.asBoolean(context: String): Boolean =
    when (this) {
        is EmscriptValue.BooleanValue -> value
        is EmscriptValue.NumberValue -> value != 0.0
        is EmscriptValue.StringValue -> value.isNotEmpty()
        EmscriptValue.NullValue -> error(
            "${EmscriptRuntimeTypeSafety.NULLABLE_VALUE_IN_NONNULL_CONTEXT}: $context erwartet Bool",
        )
    }

private fun EmscriptValue.render(): String =
    when (this) {
        is EmscriptValue.NumberValue -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is EmscriptValue.StringValue -> value
        is EmscriptValue.BooleanValue -> value.toString()
        EmscriptValue.NullValue -> "null"
    }

private fun EmscriptValue.runtimeType(): LanguageTypeRef = when (this) {
    is EmscriptValue.StringValue -> CoreTypes.STRING.ref
    is EmscriptValue.NumberValue -> CoreTypes.NUMBER.ref
    is EmscriptValue.BooleanValue -> CoreTypes.BOOL.ref
    EmscriptValue.NullValue -> error(
        "NULL_LITERAL_DECISION_REQUIRED: absent besitzt ohne erwarteten Typ keinen ableitbaren Basistyp",
    )
}
