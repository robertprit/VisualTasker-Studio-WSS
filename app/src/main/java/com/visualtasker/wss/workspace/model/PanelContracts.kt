package com.visualtasker.wss.workspace.model

import androidx.compose.ui.graphics.Color

data class PanelState(
    val id: String,
    val type: PanelType,
    val title: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val zIndex: Int,
    val minimized: Boolean,
    val locked: Boolean = false,
    val accentColor: Color = Color(0xFF6C5CE7),
    val isMaximized: Boolean = false
)

enum class PanelType {
    RecorderSteps,
    BlockEditor,
    Flowchart,
    Screenshot,
    Marker,
    Vision,
    Datastore,
    Emscript,
    RuntimeLog,
    TextEditor,
    LogConsole,
    DebugInfo,
    M3Director
}

interface PanelActionSink {
    fun onPanelAction(action: PanelAction)
}

sealed interface PanelAction {
    data class SelectStep(val stepId: String) : PanelAction
    data class ReorderStep(val from: Int, val to: Int) : PanelAction
    data class DeleteStep(val stepId: String) : PanelAction
    data class OpenPanel(val type: PanelType) : PanelAction
    data class ClosePanel(val panelId: String) : PanelAction
}
