package com.visualtasker.wss.emscript.runtime

import de.visualtasker.blockeditor.domain.BlockNode
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.asString
import de.visualtasker.blockeditor.registry.BlockTypes

class WorkspaceBasicRuntime(
    private val dryRunRuntime: WorkspaceDryRunRuntime = WorkspaceDryRunRuntime(),
    private val gate: RuntimeCapabilityGate = RuntimeCapabilityGate(),
    private val environment: WorkspaceBasicRuntimeEnvironment,
) {
    suspend fun run(document: WorkspaceDocument): EmscriptDryRunResult {
        val report = gate.inspect(document)
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
            else -> null
        }
    }

    private fun BlockNode.fieldNumber(key: String): Double =
        when (val value = fields[key]) {
            is FieldValue.Number -> value.value
            is FieldValue.Text -> value.value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    private fun BlockNode.fieldLongList(key: String): List<Long> =
        (fields[key]?.asString().orEmpty())
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .ifEmpty {
                val fallback = fieldNumber(key).toLong()
                if (type == BlockTypes.FEEDBACK_VIBRATE && fallback <= 0L) listOf(80L) else listOf(fallback)
            }
}

data class WorkspaceBasicRuntimeEnvironment(
    val delayMs: suspend (Long) -> Unit,
    val playBeep: (frequencyHz: Int, durationMs: Int, volumePercent: Int) -> Unit,
    val vibrate: (patternMs: List<Long>) -> Unit,
    val log: (String) -> Unit,
)
