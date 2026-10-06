package com.visualtasker.wss.emscript.runtime

import de.visualtasker.workflow.semantics.CommandCapability
import de.visualtasker.workflow.semantics.CommandCapabilityDescriptor

internal enum class CommandToolboxAvailability {
    LOCAL,
    ADAPTER,
    PLANNED,
    NONE,
}

internal data class CommandToolboxCapability(
    val availability: CommandToolboxAvailability,
    val shortLabel: String,
    val description: String,
)

internal fun CommandCapabilityDescriptor.toToolboxCapability(): CommandToolboxCapability {
    val adapter = requiredAdapter
        ?: return CommandToolboxCapability(
            availability = CommandToolboxAvailability.NONE,
            shortLabel = "",
            description = "Kein Runtime-Kommando",
        )
    if (!liveImplemented) {
        return CommandToolboxCapability(
            availability = CommandToolboxAvailability.PLANNED,
            shortLabel = "PLAN",
            description = "$canonicalName ist im Live-Run noch nicht implementiert ($diagnosticCode).",
        )
    }
    if (adapter in RuntimeCapabilityGate.BasicRealRunCapabilities) {
        return CommandToolboxCapability(
            availability = CommandToolboxAvailability.LOCAL,
            shortLabel = "LIVE",
            description = "$canonicalName ist lokal live ausführbar.",
        )
    }
    return CommandToolboxCapability(
        availability = CommandToolboxAvailability.ADAPTER,
        shortLabel = adapter.toolboxLabel(),
        description = "$canonicalName benötigt den Adapter ${adapter.name}.",
    )
}

private fun CommandCapability.toolboxLabel(): String = when (this) {
    CommandCapability.SCREEN_CAPTURE -> "SCREEN"
    CommandCapability.CUSTOM_TAB -> "TAB"
    else -> name.take(7)
}
