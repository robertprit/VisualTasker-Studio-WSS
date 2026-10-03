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
import de.visualtasker.emscript.contract.DomainTypes
import de.visualtasker.emscript.contract.LanguageTypeCompatibility
import de.visualtasker.emscript.contract.LanguageTypeRef
import de.visualtasker.emscript.contract.ProviderTypes
import java.util.Collections

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

enum class EmscriptCoordinateSpace {
    PIXEL,
    NORMALIZED,
}

data class EmscriptPointValue(
    val x: Double,
    val y: Double,
    val coordinateSpace: EmscriptCoordinateSpace,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "Point coordinates must be finite." }
        require(x >= 0.0 && y >= 0.0) { "Point coordinates must not be negative." }
        if (coordinateSpace == EmscriptCoordinateSpace.NORMALIZED) {
            require(x <= 1.0 && y <= 1.0) { "Normalized point coordinates must stay in 0..1." }
        }
    }
}

data class EmscriptRegionValue(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val coordinateSpace: EmscriptCoordinateSpace,
) {
    init {
        require(x.isFinite() && y.isFinite() && width.isFinite() && height.isFinite()) {
            "Region coordinates must be finite."
        }
        require(x >= 0.0 && y >= 0.0 && width > 0.0 && height > 0.0) {
            "Region coordinates must describe a non-empty positive area."
        }
        if (coordinateSpace == EmscriptCoordinateSpace.NORMALIZED) {
            require(x + width <= 1.0 && y + height <= 1.0) {
                "Normalized regions must stay in 0..1."
            }
        }
    }
}

data class EmscriptPathValue(
    val start: EmscriptPointValue,
    val control: EmscriptPointValue,
    val end: EmscriptPointValue,
) {
    init {
        require(start.coordinateSpace == control.coordinateSpace && control.coordinateSpace == end.coordinateSpace) {
            "Path points must use one coordinate space."
        }
    }
}

sealed interface EmscriptValue {
    data class NumberValue(val value: Double) : EmscriptValue
    data class StringValue(val value: String) : EmscriptValue
    data class BooleanValue(val value: Boolean) : EmscriptValue
    data class TaskerVariableValue(val name: String, val value: String) : EmscriptValue {
        init {
            require(name.isNotBlank()) { "Tasker variable name must not be blank." }
        }
    }
    data class ImageMatchValue(
        val templateId: String,
        val label: String,
        val region: EmscriptRegionValue,
        val score: Double,
    ) : EmscriptValue {
        init {
            require(templateId.isNotBlank()) { "ImageMatch templateId must not be blank." }
            require(label.isNotBlank()) { "ImageMatch label must not be blank." }
            require(score.isFinite() && score in 0.0..1.0) { "ImageMatch score must stay in 0..1." }
        }
    }
    data class TextMatchValue(
        val text: String,
        val region: EmscriptRegionValue,
        val confidence: Double,
        val source: String,
    ) : EmscriptValue {
        init {
            require(text.isNotBlank()) { "TextMatch text must not be blank." }
            require(source.isNotBlank()) { "TextMatch source must not be blank." }
            require(confidence.isFinite() && confidence in 0.0..1.0) {
                "TextMatch confidence must stay in 0..1."
            }
        }
    }
    data class MarkerValue(
        val markerId: String,
        val label: String,
        val region: EmscriptRegionValue,
        val path: EmscriptPathValue?,
        val mode: String,
        val assetId: String?,
        val threshold: Double,
    ) : EmscriptValue {
        init {
            require(markerId.isNotBlank()) { "Marker id must not be blank." }
            require(label.isNotBlank()) { "Marker label must not be blank." }
            require(mode.isNotBlank()) { "Marker mode must not be blank." }
            require(assetId == null || assetId.isNotBlank()) { "Marker assetId must be null or nonblank." }
            require(threshold.isFinite() && threshold in 0.0..1.0) { "Marker threshold must stay in 0..1." }
        }
    }
    class ChartSnapshotValue private constructor(
        private val state: ChartSnapshotState,
    ) : EmscriptValue {
        val id: String get() = state.id
        val title: String get() = state.title
        val kind: String get() = state.kind
        val series: List<ChartSeriesSnapshot> get() = state.series
        val candles: List<ChartCandleSnapshot> get() = state.candles
        val boxPlots: List<ChartBoxPlotSnapshot> get() = state.boxPlots
        val heatmapCells: List<ChartHeatmapCellSnapshot> get() = state.heatmapCells
        val histogramBinCount: Int get() = state.histogramBinCount
        val bubbles: List<ChartBubbleSnapshot> get() = state.bubbles
        val vennSets: List<ChartVennSetSnapshot> get() = state.vennSets
        val vennOverlaps: List<ChartVennOverlapSnapshot> get() = state.vennOverlaps
        val mosaicCells: List<ChartMosaicCellSnapshot> get() = state.mosaicCells
        val gaugeValue: Double? get() = state.gaugeValue
        val gaugeMinimum: Double get() = state.gaugeMinimum
        val gaugeMaximum: Double get() = state.gaugeMaximum
        val ganttTasks: List<ChartGanttTaskSnapshot> get() = state.ganttTasks
        val radarSeries: List<ChartRadarSeriesSnapshot> get() = state.radarSeries
        val diagramNodes: List<ChartDiagramNodeSnapshot> get() = state.diagramNodes
        val diagramEdges: List<ChartDiagramEdgeSnapshot> get() = state.diagramEdges
        val options: ChartSnapshotOptions get() = state.options

        override fun equals(other: Any?): Boolean =
            other is ChartSnapshotValue && state == other.state

        override fun hashCode(): Int = state.hashCode()

        override fun toString(): String = "ChartSnapshotValue(id=$id, title=$title, kind=$kind)"

        companion object {
            fun create(
                id: String,
                title: String,
                kind: String,
                series: List<ChartSeriesSnapshot> = emptyList(),
                candles: List<ChartCandleSnapshot> = emptyList(),
                boxPlots: List<ChartBoxPlotSnapshot> = emptyList(),
                heatmapCells: List<ChartHeatmapCellSnapshot> = emptyList(),
                histogramBinCount: Int = 10,
                bubbles: List<ChartBubbleSnapshot> = emptyList(),
                vennSets: List<ChartVennSetSnapshot> = emptyList(),
                vennOverlaps: List<ChartVennOverlapSnapshot> = emptyList(),
                mosaicCells: List<ChartMosaicCellSnapshot> = emptyList(),
                gaugeValue: Double? = null,
                gaugeMinimum: Double = 0.0,
                gaugeMaximum: Double = 100.0,
                ganttTasks: List<ChartGanttTaskSnapshot> = emptyList(),
                radarSeries: List<ChartRadarSeriesSnapshot> = emptyList(),
                diagramNodes: List<ChartDiagramNodeSnapshot> = emptyList(),
                diagramEdges: List<ChartDiagramEdgeSnapshot> = emptyList(),
                options: ChartSnapshotOptions = ChartSnapshotOptions(),
            ): ChartSnapshotValue = ChartSnapshotValue(
                ChartSnapshotState.create(
                    id = id,
                    title = title,
                    kind = kind,
                    series = series,
                    candles = candles,
                    boxPlots = boxPlots,
                    heatmapCells = heatmapCells,
                    histogramBinCount = histogramBinCount,
                    bubbles = bubbles,
                    vennSets = vennSets,
                    vennOverlaps = vennOverlaps,
                    mosaicCells = mosaicCells,
                    gaugeValue = gaugeValue,
                    gaugeMinimum = gaugeMinimum,
                    gaugeMaximum = gaugeMaximum,
                    ganttTasks = ganttTasks,
                    radarSeries = radarSeries,
                    diagramNodes = diagramNodes,
                    diagramEdges = diagramEdges,
                    options = options,
                ),
            )
        }
    }
    class ListValue(
        val elementType: LanguageTypeRef,
        values: List<EmscriptValue>,
    ) : EmscriptValue {
        val values: List<EmscriptValue> = Collections.unmodifiableList(ArrayList(values))

        init {
            val expectedType = LanguageTypeCompatibility.sourceName(elementType)
            require(this.values.all { EmscriptRuntimeTypeSafety.matches(it, expectedType) }) {
                "ListValue contains an element incompatible with $expectedType."
            }
        }

        override fun equals(other: Any?): Boolean =
            other is ListValue && elementType == other.elementType && values == other.values

        override fun hashCode(): Int = 31 * elementType.hashCode() + values.hashCode()

        override fun toString(): String = "ListValue(elementType=$elementType, values=$values)"
    }
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
        is EmscriptValue.ListValue,
        is EmscriptValue.ImageMatchValue,
        is EmscriptValue.TextMatchValue,
        is EmscriptValue.MarkerValue,
        is EmscriptValue.ChartSnapshotValue,
        is EmscriptValue.TaskerVariableValue,
        -> error("$context erwartet Number")
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
        is EmscriptValue.ListValue,
        is EmscriptValue.ImageMatchValue,
        is EmscriptValue.TextMatchValue,
        is EmscriptValue.MarkerValue,
        is EmscriptValue.ChartSnapshotValue,
        is EmscriptValue.TaskerVariableValue,
        -> error("$context erwartet Bool")
        EmscriptValue.NullValue -> error(
            "${EmscriptRuntimeTypeSafety.NULLABLE_VALUE_IN_NONNULL_CONTEXT}: $context erwartet Bool",
        )
    }

private fun EmscriptValue.render(): String =
    when (this) {
        is EmscriptValue.NumberValue -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is EmscriptValue.StringValue -> value
        is EmscriptValue.BooleanValue -> value.toString()
        is EmscriptValue.TaskerVariableValue -> "${name}=$value"
        is EmscriptValue.ListValue -> values.joinToString(prefix = "[", postfix = "]") { it.render() }
        is EmscriptValue.ImageMatchValue -> "ImageMatch($templateId,$score)"
        is EmscriptValue.TextMatchValue -> "TextMatch($text,$confidence)"
        is EmscriptValue.MarkerValue -> "Marker($markerId)"
        is EmscriptValue.ChartSnapshotValue -> "ChartSnapshot($id,$kind)"
        EmscriptValue.NullValue -> "null"
    }

private fun EmscriptValue.runtimeType(): LanguageTypeRef = when (this) {
    is EmscriptValue.StringValue -> CoreTypes.STRING.ref
    is EmscriptValue.NumberValue -> CoreTypes.NUMBER.ref
    is EmscriptValue.BooleanValue -> CoreTypes.BOOL.ref
    is EmscriptValue.TaskerVariableValue -> ProviderTypes.TASKER_VARIABLE.ref
    is EmscriptValue.ListValue -> LanguageTypeRef.ListOf(elementType)
    is EmscriptValue.ImageMatchValue -> DomainTypes.IMAGE_MATCH.ref
    is EmscriptValue.TextMatchValue -> DomainTypes.TEXT_MATCH.ref
    is EmscriptValue.MarkerValue -> DomainTypes.MARKER.ref
    is EmscriptValue.ChartSnapshotValue -> DomainTypes.CHART_SNAPSHOT.ref
    EmscriptValue.NullValue -> error(
        "NULL_LITERAL_DECISION_REQUIRED: absent besitzt ohne erwarteten Typ keinen ableitbaren Basistyp",
    )
}
