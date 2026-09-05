package com.visualtasker.wss.emscript.runtime

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.registry.CommandCapability
import de.visualtasker.blockeditor.registry.CommandCatalogEntry
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog

class RuntimeCapabilityGate(
    private val realRunCapabilities: Set<CommandCapability> = BasicRealRunCapabilities,
    private val realRunCommandNames: Set<String> = BasicRealRunCommandNames,
) {
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
        if (command.lowercase() in realRunCommandNames && gate in realRunCapabilities) {
            return RuntimeCapability(
                command = command,
                status = RuntimeCapabilityStatus.REAL_RUN_READY,
                details = "Live-Adapter lokal verfügbar.",
            )
        }
        if (runtime?.dryRunBehavior == "adapter-gated") {
            return RuntimeCapability(
                command = command,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = "Real-Run benötigt den Adapter ${pluginOwner}.",
            )
        }
        return when (gate) {
            CommandCapability.CORE,
            CommandCapability.TIMING,
            CommandCapability.FEEDBACK,
            CommandCapability.DEBUG,
            -> if (gate in realRunCapabilities) RuntimeCapability(
                command = command,
                status = RuntimeCapabilityStatus.REAL_RUN_READY,
                details = "Basic-Run lokal ausführbar.",
            ) else RuntimeCapability(
                command = command,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = "Capability ${gate.name} ist im Live-Runtime-Gate noch blockiert.",
            )
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

    companion object {
        val BasicRealRunCapabilities: Set<CommandCapability> = setOf(
            CommandCapability.CORE,
            CommandCapability.TIMING,
            CommandCapability.FEEDBACK,
            CommandCapability.DEBUG,
            CommandCapability.VISION,
        )

        val BasicRealRunCommandNames: Set<String> = setOf(
            "onstart",
            "wait",
            "beep",
            "vibrate",
            "log",
            "file.readtext",
            "file.writetext",
            "clipboard.get",
            "clipboard.set",
            "cache.clear",
            "sys.info",
            "env.get",
            "let",
            "set",
            "get",
            "repeat",
            "while",
            "if",
            "boolean",
            "and",
            "or",
            "operate",
            "compare",
            "number",
            "string",
            "findtemplate",
            "markersave",
            "markerload",
            "markerdelete",
            "templatedefine",
            "templatecompare",
        )

        fun withAccessibilityAdapter(): RuntimeCapabilityGate =
            RuntimeCapabilityGate(
                realRunCapabilities = BasicRealRunCapabilities + CommandCapability.A11Y + CommandCapability.SCREEN_CAPTURE,
                realRunCommandNames = BasicRealRunCommandNames + setOf(
                    "click",
                    "clickpoint",
                    "swipe",
                    "screenshot",
                ),
            )
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
