package com.visualtasker.wss.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class PanelType {
    EDITOR,
    KEYBOARD,
    LIST_TEST,
    BROWSER,
    BLOCKEDITOR,
    FLOWCHART,
    EMSCRIPT,
    LOG_CONSOLE,
}

data class PanelState(
    val id: String,
    val position: Offset,
    val width: Int,
    val height: Int,
    val accentColor: Color,
    val title: String,
    val panelType: PanelType = PanelType.EDITOR,
    val zIndex: Int = 0,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false
)
