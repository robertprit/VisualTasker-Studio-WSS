package com.visualtasker.wss.workspace.plugin.runtime

import com.visualtasker.wss.workspace.vt2vt.Vt2VtUsbAdbBridgeStatus

enum class PluginReadinessState {
    READY,
    BLOCKED,
    MISSING,
}

data class PluginReadinessEntry(
    val id: String,
    val label: String,
    val state: PluginReadinessState,
    val summary: String,
) {
    val ready: Boolean get() = state == PluginReadinessState.READY
}

data class PluginRuntimeReadinessReport(
    val entries: List<PluginReadinessEntry>,
) {
    val readyCount: Int get() = entries.count { it.state == PluginReadinessState.READY }
    val blockedCount: Int get() = entries.count { it.state == PluginReadinessState.BLOCKED }
    val missingCount: Int get() = entries.count { it.state == PluginReadinessState.MISSING }

    fun entry(id: String): PluginReadinessEntry =
        entries.first { it.id == id }
}

object PluginRuntimeReadiness {
    const val CUSTOM_TABS = "custom_tabs"
    const val SHIZUKU = "shizuku"
    const val TERMUX = "termux"
    const val TASKER = "tasker"
    const val VT2VT_USB = "vt2vt_usb"
    const val VISION_PROVIDERS = "vision_providers"

    fun inspect(
        customTabs: CustomChromeTabStatus,
        shizuku: ShizukuRegistrationStatus,
        termux: TermuxRegistrationStatus,
        tasker: TaskerRegistrationStatus,
        usb: Vt2VtUsbAdbBridgeStatus,
    ): PluginRuntimeReadinessReport = PluginRuntimeReadinessReport(
        entries = listOf(
            PluginReadinessEntry(
                id = CUSTOM_TABS,
                label = "CustomChromeTab",
                state = if (customTabs.supported) PluginReadinessState.READY else PluginReadinessState.MISSING,
                summary = customTabs.summary,
            ),
            PluginReadinessEntry(
                id = SHIZUKU,
                label = "Shizuku",
                state = when {
                    shizuku.available -> PluginReadinessState.READY
                    shizuku.installed -> PluginReadinessState.BLOCKED
                    else -> PluginReadinessState.MISSING
                },
                summary = shizuku.summary,
            ),
            PluginReadinessEntry(
                id = TERMUX,
                label = "Termux",
                state = when {
                    termux.canRunCommands -> PluginReadinessState.READY
                    termux.installed -> PluginReadinessState.BLOCKED
                    else -> PluginReadinessState.MISSING
                },
                summary = termux.summary,
            ),
            PluginReadinessEntry(
                id = TASKER,
                label = "Tasker",
                state = when {
                    tasker.available -> PluginReadinessState.READY
                    tasker.installed -> PluginReadinessState.BLOCKED
                    else -> PluginReadinessState.MISSING
                },
                summary = tasker.summary,
            ),
            PluginReadinessEntry(
                id = VT2VT_USB,
                label = "scrcpy/VT2VT USB",
                state = when {
                    usb.bridgeReady -> PluginReadinessState.READY
                    usb.usbConnected -> PluginReadinessState.BLOCKED
                    else -> PluginReadinessState.MISSING
                },
                summary = usb.summary,
            ),
            PluginReadinessEntry(
                id = VISION_PROVIDERS,
                label = "Vision Provider",
                state = PluginReadinessState.BLOCKED,
                summary = "Crop/Compare lokal; OCR, OCV und YOLO noch ohne registrierten Live-Provider",
            ),
        ),
    )
}
