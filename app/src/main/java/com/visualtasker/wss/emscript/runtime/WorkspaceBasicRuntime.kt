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
                    executeEvent(document, event)?.let { message ->
                        liveEvents += EmscriptDryRunEvent(
                            index = dryRun.events.size + liveEvents.size + 1,
                            kind = "live",
                            message = message,
                            blockId = event.blockId,
                            command = event.command,
                            capability = event.capability,
                            pluginOwner = event.pluginOwner,
                        )
                    }
                }
                EmscriptDryRunResult.Success(
                    events = dryRun.events + liveEvents + EmscriptDryRunEvent(
                        index = dryRun.events.size + liveEvents.size + 1,
                        kind = "done",
                        message = "Workspace Basic-Run abgeschlossen: ${liveEvents.size} Live-Effekte.",
                    ),
                    variables = dryRun.variables,
                )
            }
        }
    }

    private suspend fun executeEvent(document: WorkspaceDocument, event: EmscriptDryRunEvent): String? {
        val block = event.blockId?.let { document.blocks[de.visualtasker.blockeditor.domain.BlockId(it)] }
        val command = event.command.orEmpty().lowercase()
        return when (event.kind) {
            "wait" -> {
                val ms = block?.fieldNumber("ms")?.toLong()?.coerceAtLeast(0L) ?: 0L
                environment.delayMs(ms)
                "wait($ms) ausgeführt"
            }
            "log" -> {
                environment.log(event.message)
                "log ausgeführt: ${event.message}"
            }
            "click" -> {
                val text = block?.fieldText("text").orEmpty()
                if (environment.clickText(text)) "click(\"$text\") ausgeführt" else "click(\"$text\") fehlgeschlagen"
            }
            "beep" -> {
                val hz = block?.fieldNumber("frequency")?.toInt()?.coerceIn(20, 20_000) ?: 1_000
                val durationMs = block?.fieldNumber("durationMs")?.toInt()?.coerceIn(10, 10_000) ?: 200
                val volume = block?.fieldNumber("volume")?.toInt()?.coerceIn(0, 100) ?: 100
                environment.playBeep(hz, durationMs, volume)
                "beep($hz,$durationMs,$volume) ausgeführt"
            }
            "vibrate" -> {
                val pattern = block?.fieldLongList("pattern") ?: listOf(80L)
                environment.vibrate(pattern)
                "vibrate(${pattern.joinToString(",")}) ausgeführt"
            }
            "let",
            "set" -> "Variable ${event.message} gesetzt"
            "command",
            "capability" -> executeCommandEvent(block, command)
            else -> null
        }
    }

    private suspend fun executeCommandEvent(block: BlockNode?, command: String): String? =
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
                if (success) "clickPoint($x,$y,$repeat) ausgeführt" else "clickPoint($x,$y,$repeat) fehlgeschlagen"
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
                if (success) "swipe(${points.size} Punkte,$repeat) ausgeführt" else "swipe(${points.size} Punkte,$repeat) fehlgeschlagen"
            }
            "clipboard.get" -> {
                val value = environment.clipboardGet()
                environment.log("Clipboard.get -> ${value.length} Zeichen")
                "Clipboard.get ausgeführt: ${value.length} Zeichen"
            }
            "clipboard.set" -> {
                val text = block.stringArgument(fieldName = "text")
                environment.clipboardSet(text)
                "Clipboard.set ausgeführt: ${text.length} Zeichen"
            }
            "cache.clear" -> {
                val removed = environment.cacheClear()
                "Cache.clear ausgeführt: $removed Einträge entfernt"
            }
            "sys.info" -> {
                val info = environment.systemInfo()
                environment.log(info)
                "Sys.info ausgeführt"
            }
            "env.get" -> {
                val name = block.stringArgument(fieldName = "name")
                val value = environment.envGet(name)
                environment.log("Env.get($name) -> $value")
                "Env.get($name) ausgeführt"
            }
            "file.readtext" -> {
                val path = block.stringArgument(fieldName = "path")
                val text = environment.fileReadText(path)
                environment.log("File.readText($path) -> ${text?.length ?: 0} Zeichen")
                if (text != null) "File.readText($path) ausgeführt" else "File.readText($path) fehlgeschlagen"
            }
            "file.writetext" -> {
                val path = block.stringArgument(fieldName = "path")
                val text = block.stringArgument(index = 1, fieldName = "text")
                if (environment.fileWriteText(path, text)) {
                    "File.writeText($path) ausgeführt: ${text.length} Zeichen"
                } else {
                    "File.writeText($path) fehlgeschlagen"
                }
            }
            "screenshot" -> {
                val path = block.stringArgument(fieldName = "path").ifBlank { "screenshots/latest.png" }
                if (environment.screenshot(path)) {
                    "screenshot($path) ausgeführt"
                } else {
                    "screenshot($path) fehlgeschlagen"
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

    private fun BlockNode?.stringArgument(index: Int = 0, fieldName: String): String {
        if (this == null) return ""
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
)

data class RuntimeAutomationPoint(
    val x: Int,
    val y: Int,
)
