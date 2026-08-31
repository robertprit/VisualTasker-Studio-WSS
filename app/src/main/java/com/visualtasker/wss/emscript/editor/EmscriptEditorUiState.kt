package com.visualtasker.wss.emscript.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class EmscriptEditorUiState {
    var fontSizeSp by mutableStateOf(12f)
    val selectionStarts = mutableStateMapOf<String, Int>()
    val selectionEnds = mutableStateMapOf<String, Int>()
    val foldedKeysByTab = mutableStateMapOf<String, Set<String>>()
}
