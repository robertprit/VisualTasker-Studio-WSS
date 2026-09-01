package com.visualtasker.wss.emscript.runtime

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.registry.BlockTypes

class RuntimeCapabilityGate {
    fun inspect(document: WorkspaceDocument): RuntimeCapabilityReport {
        val commands = document.blocks.values
            .mapNotNull { block -> commandForBlockType(block.type) }
            .distinctBy { it.command }
            .sortedBy { it.command }
        val capabilities = commands.map { command ->
            when (command.command) {
                "log", "let", "set", "wait" -> RuntimeCapability(
                    command = command.command,
                    status = RuntimeCapabilityStatus.DRY_RUN_READY,
                    details = "Dry-Run verfügbar; Real-Run benötigt den Runtime-Scheduler.",
                )
                "click" -> RuntimeCapability(
                    command = command.command,
                    status = RuntimeCapabilityStatus.BLOCKED,
                    details = "Real-Run benötigt Accessibility/Shizuku-Ausführungsadapter und Capability-Freigabe.",
                )
                "beep", "vibrate" -> RuntimeCapability(
                    command = command.command,
                    status = RuntimeCapabilityStatus.BLOCKED,
                    details = "Dry-Run verfügbar; Real-Run benötigt Feedback-Bridge und Nutzereinstellung.",
                )
                else -> RuntimeCapability(
                    command = command.command,
                    status = RuntimeCapabilityStatus.BLOCKED,
                    details = "Kein Runtime-Adapter registriert.",
                )
            }
        }
        return RuntimeCapabilityReport(capabilities)
    }

    private fun commandForBlockType(type: String): RuntimeCommand? = when (type) {
        BlockTypes.ACTION_CLICK_TEXT -> RuntimeCommand("click")
        BlockTypes.ACTION_WAIT -> RuntimeCommand("wait")
        BlockTypes.DEBUG_LOG -> RuntimeCommand("log")
        BlockTypes.FEEDBACK_BEEP -> RuntimeCommand("beep")
        BlockTypes.FEEDBACK_VIBRATE -> RuntimeCommand("vibrate")
        BlockTypes.VARIABLE_SET -> RuntimeCommand("set")
        else -> null
    }
}

data class RuntimeCapabilityReport(
    val capabilities: List<RuntimeCapability>,
) {
    val realRunAllowed: Boolean
        get() = capabilities.isNotEmpty() && capabilities.all { it.status == RuntimeCapabilityStatus.REAL_RUN_READY }

    val summary: String
        get() {
            if (capabilities.isEmpty()) return "Keine ausführbaren Runtime-Kommandos im Workspace."
            val blocked = capabilities.count { it.status == RuntimeCapabilityStatus.BLOCKED }
            val dry = capabilities.count { it.status == RuntimeCapabilityStatus.DRY_RUN_READY }
            val real = capabilities.count { it.status == RuntimeCapabilityStatus.REAL_RUN_READY }
            return "Runtime Gates: $real real-ready, $dry dry-run-ready, $blocked blockiert."
        }
}

data class RuntimeCapability(
    val command: String,
    val status: RuntimeCapabilityStatus,
    val details: String,
)

enum class RuntimeCapabilityStatus {
    REAL_RUN_READY,
    DRY_RUN_READY,
    BLOCKED,
}

private data class RuntimeCommand(val command: String)
