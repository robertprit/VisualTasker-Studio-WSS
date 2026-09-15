package com.visualtasker.wss.emscript.runtime

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.registry.CommandCapability
import de.visualtasker.blockeditor.registry.CommandCapabilityDescriptor
import de.visualtasker.blockeditor.registry.CommandCatalogEntry
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import de.visualtasker.blockeditor.registry.toCapabilityDescriptor

class RuntimeCapabilityGate(
    private val realRunCapabilities: Set<CommandCapability> = BasicRealRunCapabilities,
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
        val descriptor = toCapabilityDescriptor()
        val gate = descriptor.requiredAdapter
        if (descriptor.isLiveReady(realRunCapabilities)) {
            return RuntimeCapability(
                command = canonicalName,
                status = RuntimeCapabilityStatus.REAL_RUN_READY,
                details = "Live-Adapter lokal verfügbar.",
            )
        }
        if (descriptor.dryRunBehavior == "adapter-gated") {
            return RuntimeCapability(
                command = canonicalName,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = if (!descriptor.liveImplemented) {
                    "Real-Run für ${canonicalName} ist noch nicht implementiert."
                } else {
                    "Real-Run benötigt den Adapter ${pluginOwner}."
                },
            )
        }
        return when (gate) {
            CommandCapability.CORE,
            CommandCapability.TIMING,
            CommandCapability.FEEDBACK,
            CommandCapability.DEBUG,
            -> if (gate in realRunCapabilities) RuntimeCapability(
                command = canonicalName,
                status = RuntimeCapabilityStatus.REAL_RUN_READY,
                details = "Basic-Run lokal ausführbar.",
            ) else RuntimeCapability(
                command = canonicalName,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = "Capability ${gate.name} ist im Live-Runtime-Gate noch blockiert.",
            )
            CommandCapability.A11Y -> RuntimeCapability(
                command = canonicalName,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = "Real-Run benötigt Accessibility/Shizuku-Ausführungsadapter und Capability-Freigabe.",
            )
            null -> RuntimeCapability(
                command = canonicalName,
                status = RuntimeCapabilityStatus.BLOCKED,
                details = "Kein Runtime-Adapter registriert.",
            )
            else -> RuntimeCapability(
                command = canonicalName,
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
            *VisualTaskerCommandCatalog.runtimeCapabilityDescriptors()
                .filter { it.requiredAdapter in BasicRealRunCapabilities && it.liveImplemented }
                .flatMap { it.acceptedNames }
                .map { it.lowercase() }
                .toTypedArray(),
        )

        fun withAccessibilityAdapter(): RuntimeCapabilityGate =
            RuntimeCapabilityGate(
                realRunCapabilities = BasicRealRunCapabilities + CommandCapability.A11Y + CommandCapability.SCREEN_CAPTURE,
            )

        fun withDeviceAdapters(
            accessibilityAvailable: Boolean,
            customChromeTabAvailable: Boolean,
            shizukuAvailable: Boolean,
            termuxAvailable: Boolean,
            taskerAvailable: Boolean,
            usbAdbBridgeAvailable: Boolean,
        ): RuntimeCapabilityGate {
            var capabilities = BasicRealRunCapabilities
            if (accessibilityAvailable) {
                capabilities += CommandCapability.A11Y
                capabilities += CommandCapability.SCREEN_CAPTURE
            }
            if (customChromeTabAvailable) {
                capabilities += CommandCapability.CUSTOM_TAB
            }
            if (shizukuAvailable) {
                capabilities += CommandCapability.SHIZUKU
            }
            if (termuxAvailable) {
                capabilities += CommandCapability.TERMUX
            }
            if (taskerAvailable) {
                capabilities += CommandCapability.TASKER
            }
            if (usbAdbBridgeAvailable) {
                capabilities += CommandCapability.SCRCPY
            }
            return RuntimeCapabilityGate(
                realRunCapabilities = capabilities,
            )
        }
    }
}

internal fun CommandCatalogEntry.isBasicRuntimeReady(
    realRunCapabilities: Set<CommandCapability> = RuntimeCapabilityGate.BasicRealRunCapabilities,
): Boolean =
    toCapabilityDescriptor().isLiveReady(realRunCapabilities)

private fun CommandCapabilityDescriptor.isLiveReady(
    realRunCapabilities: Set<CommandCapability>,
): Boolean =
    liveImplemented &&
        requiredAdapter != null &&
        requiredAdapter in realRunCapabilities

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
