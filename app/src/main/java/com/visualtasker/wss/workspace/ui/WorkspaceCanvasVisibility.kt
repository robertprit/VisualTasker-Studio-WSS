package com.visualtasker.wss.workspace.ui

import com.visualtasker.wss.workspace.model.PanelType
import com.visualtasker.wss.workspace.model.PanelState

internal fun workspaceSurfaceOwnerPanelType(panels: List<PanelState>): PanelType? =
    panels
        .asSequence()
        .filterNot { it.minimized }
        .maxByOrNull { it.zIndex }
        ?.type

internal fun shouldShowWorkspaceCanvas(
    focusedPanelType: PanelType?,
    hasSelectedRailTraceStep: Boolean,
    hasScreenshotAssets: Boolean,
): Boolean = when (focusedPanelType) {
    PanelType.Marker,
    PanelType.Screenshot,
    -> true

    PanelType.RecorderSteps -> hasSelectedRailTraceStep && hasScreenshotAssets
    else -> false
}
