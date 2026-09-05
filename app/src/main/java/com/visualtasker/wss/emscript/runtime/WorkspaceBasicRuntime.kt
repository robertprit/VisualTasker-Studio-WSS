package com.visualtasker.wss.emscript.runtime

import de.visualtasker.blockeditor.domain.BlockNode
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.asString
import de.visualtasker.blockeditor.registry.BlockTypes
import kotlin.math.roundToInt

class WorkspaceBasicRuntime(
    private val dryRunRuntime: WorkspaceDryRunRuntime = WorkspaceDryRunRuntime(),
    private val capabilityGate: () -> RuntimeCapabilityGate = { RuntimeCapabilityGate() },
    private val config: WorkspaceBasicRuntimeConfig = WorkspaceBasicRuntimeConfig(),
    private val environment: WorkspaceBasicRuntimeEnvironment,
) {
    suspend fun run(document: WorkspaceDocument): EmscriptDryRunResult {
        val report = capabilityGate().inspect(document)
        if (!report.realRunAllowed) {
            return EmscriptDryRunResult.Failure(
                message = report.summary,
                events = report.capabilities
                    .filter { it.status != RuntimeCapabilityStatus.REAL_RUN_READY }
                    .mapIndexed { index, capability ->
                        EmscriptDryRunEvent(
                            index = index + 1,
                            kind = "capability",
                            message = "${capability.command}: ${capability.details}",
                            severity = if (capability.status == RuntimeCapabilityStatus.BLOCKED) {
                                EmscriptDryRunEventSeverity.ERROR
                            } else {
                                EmscriptDryRunEventSeverity.WARNING
                            },
                            command = capability.command,
                        )
                    },
            )
        }

        return when (val dryRun = dryRunRuntime.run(document)) {
            is EmscriptDryRunResult.Failure -> dryRun
            is EmscriptDryRunResult.Success -> {
                val liveEvents = mutableListOf<EmscriptDryRunEvent>()
                dryRun.events.forEach { event ->
                    executeEvent(document, event)?.let { outcome ->
                        liveEvents += EmscriptDryRunEvent(
                            index = dryRun.events.size + liveEvents.size + 1,
                            kind = "live",
                            message = outcome.message,
                            blockId = event.blockId,
                            severity = outcome.severity,
                            command = event.command,
                            capability = event.capability,
                            pluginOwner = event.pluginOwner,
                        )
                        if (config.stopOnLiveWarning && outcome.severity == EmscriptDryRunEventSeverity.WARNING) {
                            return EmscriptDryRunResult.Failure(
                                message = outcome.message,
                                events = dryRun.events + liveEvents,
                            )
                        }
                    }
                }
                val warningCount = liveEvents.count { it.severity == EmscriptDryRunEventSeverity.WARNING }
                EmscriptDryRunResult.Success(
                    events = dryRun.events + liveEvents + EmscriptDryRunEvent(
                        index = dryRun.events.size + liveEvents.size + 1,
                        kind = "done",
                        message = if (warningCount > 0) {
                            "Workspace Basic-Run abgeschlossen: ${liveEvents.size} Live-Effekte, $warningCount Warnungen."
                        } else {
                            "Workspace Basic-Run abgeschlossen: ${liveEvents.size} Live-Effekte."
                        },
                        severity = if (warningCount > 0) {
                            EmscriptDryRunEventSeverity.WARNING
                        } else {
                            EmscriptDryRunEventSeverity.INFO
                        },
                    ),
                    variables = dryRun.variables,
                )
            }
        }
    }

    private suspend fun executeEvent(document: WorkspaceDocument, event: EmscriptDryRunEvent): LiveExecutionOutcome? {
        val block = event.blockId?.let { document.blocks[de.visualtasker.blockeditor.domain.BlockId(it)] }
        val command = event.command.orEmpty().lowercase()
        return when (event.kind) {
            "wait" -> {
                val ms = block?.fieldNumber("ms")?.toLong()?.coerceAtLeast(0L) ?: 0L
                environment.delayMs(ms)
                LiveExecutionOutcome("wait($ms) ausgeführt")
            }
            "log" -> {
                environment.log(event.message)
                LiveExecutionOutcome("log ausgeführt: ${event.message}")
            }
            "click" -> {
                val text = block?.fieldText("text").orEmpty()
                if (environment.clickText(text)) {
                    LiveExecutionOutcome("click(\"$text\") ausgeführt")
                } else {
                    LiveExecutionOutcome("click(\"$text\") fehlgeschlagen", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "beep" -> {
                val hz = block?.fieldNumber("frequency")?.toInt()?.coerceIn(20, 20_000) ?: 1_000
                val durationMs = block?.fieldNumber("durationMs")?.toInt()?.coerceIn(10, 10_000) ?: 200
                val volume = block?.fieldNumber("volume")?.toInt()?.coerceIn(0, 100) ?: 100
                environment.playBeep(hz, durationMs, volume)
                LiveExecutionOutcome("beep($hz,$durationMs,$volume) ausgeführt")
            }
            "vibrate" -> {
                val pattern = block?.fieldLongList("pattern") ?: listOf(80L)
                environment.vibrate(pattern)
                LiveExecutionOutcome("vibrate(${pattern.joinToString(",")}) ausgeführt")
            }
            "let",
            "set" -> LiveExecutionOutcome("Variable ${event.message} gesetzt")
            "command",
            "capability" -> executeCommandEvent(block, command)
            else -> null
        }
    }

    private suspend fun executeCommandEvent(block: BlockNode?, command: String): LiveExecutionOutcome? =
        when (command) {
            "clickpoint" -> {
                val args = block?.numberArguments().orEmpty()
                val x = block?.fieldNumberOrNull("x")?.roundToInt() ?: args.getOrNull(0)?.roundToInt() ?: 0
                val y = block?.fieldNumberOrNull("y")?.roundToInt() ?: args.getOrNull(1)?.roundToInt() ?: 0
                val repeat = (
                    block?.fieldNumberOrNull("repeat")?.roundToInt()
                        ?: args.getOrNull(2)?.roundToInt()
                        ?: 1
                    ).coerceAtLeast(1)
                var success = false
                repeat(repeat) {
                    success = environment.clickPoint(x, y) || success
                }
                if (success) {
                    LiveExecutionOutcome("clickPoint($x,$y,$repeat) ausgeführt")
                } else {
                    LiveExecutionOutcome("clickPoint($x,$y,$repeat) fehlgeschlagen", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "swipe" -> {
                val args = block?.numberArguments().orEmpty()
                val points = block?.fieldPointList("points").orEmpty().ifEmpty {
                    val pointNumbers = if (args.size % 2 == 1) args.dropLast(1) else args
                    pointNumbers.toPointList()
                }
                val repeat = (
                    block?.fieldNumberOrNull("repeat")?.roundToInt()
                        ?: (if (args.size % 2 == 1) args.lastOrNull()?.roundToInt() else null)
                        ?: 1
                    ).coerceAtLeast(1)
                var success = false
                repeat(repeat) {
                    success = environment.swipe(points, 250L) || success
                }
                if (success) {
                    LiveExecutionOutcome("swipe(${points.size} Punkte,$repeat) ausgeführt")
                } else {
                    LiveExecutionOutcome("swipe(${points.size} Punkte,$repeat) fehlgeschlagen", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "clipboard.get" -> {
                val value = environment.clipboardGet()
                environment.log("Clipboard.get -> ${value.length} Zeichen")
                LiveExecutionOutcome("Clipboard.get ausgeführt: ${value.length} Zeichen")
            }
            "clipboard.set" -> {
                val text = block.stringArgument(fieldName = "text")
                environment.clipboardSet(text)
                LiveExecutionOutcome("Clipboard.set ausgeführt: ${text.length} Zeichen")
            }
            "cache.clear" -> {
                val removed = environment.cacheClear()
                LiveExecutionOutcome("Cache.clear ausgeführt: $removed Einträge entfernt")
            }
            "sys.info" -> {
                val info = environment.systemInfo()
                environment.log(info)
                LiveExecutionOutcome("Sys.info ausgeführt")
            }
            "env.get" -> {
                val name = block.stringArgument(fieldName = "name")
                val value = environment.envGet(name)
                environment.log("Env.get($name) -> $value")
                LiveExecutionOutcome("Env.get($name) ausgeführt")
            }
            "file.readtext" -> {
                val path = block.stringArgument(fieldName = "path")
                val text = environment.fileReadText(path)
                environment.log("File.readText($path) -> ${text?.length ?: 0} Zeichen")
                if (text != null) {
                    LiveExecutionOutcome("File.readText($path) ausgeführt")
                } else {
                    LiveExecutionOutcome("File.readText($path) fehlgeschlagen", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "file.writetext" -> {
                val path = block.stringArgument(fieldName = "path")
                val text = block.stringArgument(index = 1, fieldName = "text")
                if (environment.fileWriteText(path, text)) {
                    LiveExecutionOutcome("File.writeText($path) ausgeführt: ${text.length} Zeichen")
                } else {
                    LiveExecutionOutcome("File.writeText($path) fehlgeschlagen", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "screenshot" -> {
                val path = block.stringArgument(fieldName = "path").ifBlank { "screenshots/latest.png" }
                if (environment.screenshot(path)) {
                    LiveExecutionOutcome("screenshot($path) ausgeführt")
                } else {
                    LiveExecutionOutcome("screenshot($path) fehlgeschlagen", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "findtemplate" -> {
                val name = block.stringArgument(fieldName = "imagePath").ifBlank { "template" }
                val threshold = (
                    block?.numberArgument(index = 1, fieldName = "threshold")
                        ?: 0.82
                    ).toFloat().coerceIn(0f, 1f)
                val timeoutMs = (
                    block?.numberArgument(index = 2, fieldName = "timeoutMs")
                        ?: 3000.0
                    ).toLong().coerceAtLeast(0L)
                val region = block?.regionArgumentOrNull("searchRegion")
                val match = environment.findTemplate(name, threshold, timeoutMs, region)
                if (match != null && match.score >= threshold) {
                    LiveExecutionOutcome("findTemplate($name) = ${"%.1f".format(match.score * 100f)}%")
                } else {
                    LiveExecutionOutcome("findTemplate($name) nicht gefunden", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "markersave" -> {
                val name = block.stringArgument(fieldName = "name").ifBlank { "marker" }
                val region = block?.regionArgument(fieldName = "region") ?: RuntimeAutomationRegion(0, 0, 1, 1)
                val mode = block.stringArgument(index = 2, fieldName = "mode").ifBlank { "region" }
                val threshold = block?.numberArgument(index = 3, fieldName = "threshold")?.toFloat()
                    ?: 0.85f
                if (environment.markerSave(name, region, mode, threshold.coerceIn(0f, 1f))) {
                    LiveExecutionOutcome("markerSave($name) ausgeführt")
                } else {
                    LiveExecutionOutcome("markerSave($name) fehlgeschlagen", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "markerload" -> {
                val name = block.stringArgument(fieldName = "name").ifBlank { "marker" }
                val loaded = environment.markerLoad(name)
                if (loaded != null) {
                    LiveExecutionOutcome("markerLoad($name) ausgeführt: ${loaded.width}x${loaded.height}")
                } else {
                    LiveExecutionOutcome("markerLoad($name) nicht gefunden", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "markerdelete" -> {
                val name = block.stringArgument(fieldName = "name").ifBlank { "marker" }
                if (environment.markerDelete(name)) {
                    LiveExecutionOutcome("markerDelete($name) ausgeführt")
                } else {
                    LiveExecutionOutcome("markerDelete($name) nicht gefunden", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "templatedefine" -> {
                val name = block.stringArgument(fieldName = "name").ifBlank { "template" }
                val region = block?.regionArgument(fieldName = "region") ?: RuntimeAutomationRegion(0, 0, 1, 1)
                val processing = block.stringArgument(index = 2, fieldName = "processing").ifBlank { "grayscale" }
                if (environment.templateDefine(name, region, processing)) {
                    LiveExecutionOutcome("templateDefine($name) ausgeführt")
                } else {
                    LiveExecutionOutcome("templateDefine($name) fehlgeschlagen", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "templatecompare" -> {
                val name = block.stringArgument(fieldName = "name").ifBlank { "template" }
                val region = block?.regionArgument(fieldName = "region") ?: RuntimeAutomationRegion(0, 0, 1, 1)
                val processing = block.stringArgument(index = 2, fieldName = "processing").ifBlank { "grayscale" }
                val score = environment.templateCompare(name, region, processing)
                if (score != null) {
                    LiveExecutionOutcome("templateCompare($name) = ${"%.1f".format(score * 100f)}%")
                } else {
                    LiveExecutionOutcome("templateCompare($name) nicht möglich", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            "datastoreput" -> {
                val key = block.stringArgument(fieldName = "key").ifBlank { "key" }
                val value = block.stringArgument(index = 1, fieldName = "value").ifBlank { block?.fieldText("value").orEmpty() }
                environment.datastorePut(key, value)
                LiveExecutionOutcome("datastorePut($key) ausgeführt")
            }
            "datastoreget" -> {
                val key = block.stringArgument(fieldName = "key").ifBlank { "key" }
                val value = environment.datastoreGet(key)
                environment.log("datastoreGet($key) -> ${value.orEmpty()}")
                if (value != null) {
                    LiveExecutionOutcome("datastoreGet($key) ausgeführt")
                } else {
                    LiveExecutionOutcome("datastoreGet($key) leer", EmscriptDryRunEventSeverity.WARNING)
                }
            }
            else -> null
        }

    private fun BlockNode.fieldNumber(key: String): Double =
        fieldNumberOrNull(key) ?: 0.0

    private fun BlockNode.fieldNumberOrNull(key: String): Double? =
        when (val value = fields[key]) {
            is FieldValue.Number -> value.value
            is FieldValue.Text -> value.value.toDoubleOrNull()
            else -> null
        }

    private fun BlockNode.fieldText(key: String): String =
        fields[key]?.asString().orEmpty()

    private fun BlockNode.fieldLongList(key: String): List<Long> =
        (fields[key]?.asString().orEmpty())
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .ifEmpty {
                val fallback = fieldNumber(key).toLong()
                if (type == BlockTypes.FEEDBACK_VIBRATE && fallback <= 0L) listOf(80L) else listOf(fallback)
            }

    private fun BlockNode.fieldPointList(key: String): List<RuntimeAutomationPoint> {
        val numbers = Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(fields[key]?.asString().orEmpty())
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()
        return numbers.toPointList()
    }

    private fun BlockNode.numberArguments(): List<Double> =
        Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(fieldText("args"))
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()

    private fun BlockNode.numberArgument(index: Int, fieldName: String): Double? =
        rawArgument(index)
            ?.toDoubleOrNull()
            ?: fieldNumberOrNull(fieldName)

    private fun BlockNode.regionArgument(fieldName: String): RuntimeAutomationRegion {
        return regionArgumentOrNull(fieldName) ?: RuntimeAutomationRegion(0, 0, 1, 1)
    }

    private fun BlockNode.regionArgumentOrNull(fieldName: String): RuntimeAutomationRegion? {
        val direct = fieldText(fieldName)
        val argsRegion = Regex("""(?i)(region|bbox)\([^)]*\)""")
            .findAll(fieldText("args"))
            .lastOrNull()
            ?.value
        val source = argsRegion ?: direct
        if (source.isBlank()) return null
        val numbers = Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(source)
            .mapNotNull { it.value.toDoubleOrNull()?.roundToInt() }
            .toList()
        if (numbers.size < 4) return null
        return RuntimeAutomationRegion(
            x = numbers.getOrNull(0) ?: 0,
            y = numbers.getOrNull(1) ?: 0,
            width = (numbers.getOrNull(2) ?: 1).coerceAtLeast(1),
            height = (numbers.getOrNull(3) ?: 1).coerceAtLeast(1),
        )
    }

    private fun BlockNode.rawArgument(index: Int): String? =
        rawArguments().getOrNull(index)?.trim()?.trim('"')?.takeIf { it.isNotBlank() }

    private fun BlockNode.rawArguments(): List<String> {
        val source = fieldText("args")
        if (source.isBlank()) return emptyList()
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var parenDepth = 0
        var bracketDepth = 0
        var braceDepth = 0
        var inString = false
        var escaped = false
        source.forEach { char ->
            if (escaped) {
                current.append(char)
                escaped = false
                return@forEach
            }
            if (char == '\\' && inString) {
                current.append(char)
                escaped = true
                return@forEach
            }
            if (char == '"') {
                inString = !inString
                current.append(char)
                return@forEach
            }
            if (!inString) {
                when (char) {
                    '(' -> parenDepth += 1
                    ')' -> parenDepth -= 1
                    '[' -> bracketDepth += 1
                    ']' -> bracketDepth -= 1
                    '{' -> braceDepth += 1
                    '}' -> braceDepth -= 1
                    ',' -> if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                        args += current.toString().trim()
                        current.clear()
                        return@forEach
                    }
                }
            }
            current.append(char)
        }
        args += current.toString().trim()
        return args
    }

    private fun BlockNode?.stringArgument(index: Int = 0, fieldName: String): String {
        if (this == null) return ""
        rawArgument(index)?.let { return it }
        val direct = fieldText(fieldName)
        if (direct.isNotBlank()) return direct.trim('"')
        val args = fieldText("args")
        val quoted = Regex("\"((?:\\\\.|[^\"])*)\"")
            .findAll(args)
            .map { it.groupValues[1].replace("\\\"", "\"") }
            .toList()
        if (index in quoted.indices) return quoted[index]
        return args.split(',')
            .map { it.trim().trim('"') }
            .getOrNull(index)
            .orEmpty()
    }

    private fun List<Double>.toPointList(): List<RuntimeAutomationPoint> =
        chunked(2).mapNotNull { pair ->
            if (pair.size == 2) RuntimeAutomationPoint(pair[0].roundToInt(), pair[1].roundToInt()) else null
        }
}

data class WorkspaceBasicRuntimeConfig(
    val stopOnLiveWarning: Boolean = false,
)

private data class LiveExecutionOutcome(
    val message: String,
    val severity: EmscriptDryRunEventSeverity = EmscriptDryRunEventSeverity.INFO,
)

data class WorkspaceBasicRuntimeEnvironment(
    val delayMs: suspend (Long) -> Unit,
    val playBeep: (frequencyHz: Int, durationMs: Int, volumePercent: Int) -> Unit,
    val vibrate: (patternMs: List<Long>) -> Unit,
    val log: (String) -> Unit,
    val clickText: suspend (String) -> Boolean = { false },
    val clickPoint: suspend (x: Int, y: Int) -> Boolean = { _, _ -> false },
    val swipe: suspend (points: List<RuntimeAutomationPoint>, durationMs: Long) -> Boolean = { _, _ -> false },
    val clipboardGet: () -> String = { "" },
    val clipboardSet: (String) -> Unit = {},
    val cacheClear: () -> Int = { 0 },
    val systemInfo: () -> String = { "" },
    val envGet: (String) -> String = { "" },
    val fileReadText: (String) -> String? = { null },
    val fileWriteText: (path: String, text: String) -> Boolean = { _, _ -> false },
    val screenshot: suspend (path: String) -> Boolean = { false },
    val findTemplate: (name: String, threshold: Float, timeoutMs: Long, searchRegion: RuntimeAutomationRegion?) -> RuntimeTemplateMatch? = { _, _, _, _ -> null },
    val markerSave: (name: String, region: RuntimeAutomationRegion, mode: String, threshold: Float) -> Boolean = { _, _, _, _ -> false },
    val markerLoad: (name: String) -> RuntimeAutomationRegion? = { null },
    val markerDelete: (name: String) -> Boolean = { false },
    val templateDefine: (name: String, region: RuntimeAutomationRegion, processing: String) -> Boolean = { _, _, _ -> false },
    val templateCompare: (name: String, region: RuntimeAutomationRegion, processing: String) -> Float? = { _, _, _ -> null },
    val datastorePut: (key: String, value: String) -> Unit = { _, _ -> },
    val datastoreGet: (key: String) -> String? = { null },
)

data class RuntimeAutomationPoint(
    val x: Int,
    val y: Int,
)

data class RuntimeAutomationRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

data class RuntimeTemplateMatch(
    val name: String,
    val region: RuntimeAutomationRegion,
    val score: Float,
)
