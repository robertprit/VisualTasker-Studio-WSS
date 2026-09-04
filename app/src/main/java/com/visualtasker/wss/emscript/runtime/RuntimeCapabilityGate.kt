package com.visualtasker.wss.emscript.runtime

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.registry.CommandCapability
import de.visualtasker.blockeditor.registry.CommandCatalogEntry
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog

class RuntimeCapabilityGate {
    fun inspect(document: WorkspaceDocument): RuntimeCapabilityReport {
        val commands = document.blocks.values
            .mapNotNull { block -> VisualTaskerCommandCatalog.findByBlockType(block.type) }
            .distinctBy { it.id }
            .sortedBy { it.canonicalName }
        val capabilities = commands.map { command -> command.runtimeCapability() }
        return RuntimeCapabilityReport(capabilities)
    }

    private fun CommandCatalogEntry.runtimeCapability(): RuntimeCapability {
        val command = canonicalName
        val gate = runtime?.liveCapabilityGate
        return when (gate) {
            CommandCapability.CORE,
            CommandCapability.TIMING,
            CommandCapability.FEEDBACK,
            CommandCapability.DEBUG,
            -> when (command) {
                "log", "let", "set", "wait", "beep", "vibrate" -> RuntimeCapability(
                    command = command,
                    status = RuntimeCapabilityStatus.DRY_RUN_READY,
                    details = "Dry-Run verfügbar; Real-Run benötigt den Runtime-Scheduler.",
                )
                else -> RuntimeCapability(
                    command = command,
                    status = RuntimeCapabilityStatus.DRY_RUN_READY,
                    details = "Dry-Run verfügbar; Real-Run Adapter noch nicht finalisiert.",
                )
            }
            CommandCapability.A11Y -> RuntimeCapability(
                command = command,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = "Real-Run benötigt Accessibility/Shizuku-Ausführungsadapter und Capability-Freigabe.",
            )
            null -> RuntimeCapability(
                command = command,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = "Kein Runtime-Adapter registriert.",
            )
            else -> RuntimeCapability(
                command = command,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = "Capability ${gate.name} ist im Live-Runtime-Gate noch blockiert.",
            )
        }
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
