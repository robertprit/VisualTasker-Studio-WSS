package com.visualtasker.wss.emscript.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class EmscriptEditorCommand {
    Undo,
    Redo,
    Cut,
    Copy,
    Paste,
    ApplyDraft,
    TextDecrease,
    TextIncrease,
    Search
}

data class EmscriptEditorCommandEvent(
    val sequence: Long,
    val command: EmscriptEditorCommand
)

class EmscriptEditorUiState {
    var fontSizeSp by mutableStateOf(12f)
    var pendingCommand by mutableStateOf<EmscriptEditorCommandEvent?>(null)
        private set
    private var commandSequence = 0L
    val selectionStarts = mutableStateMapOf<String, Int>()
    val selectionEnds = mutableStateMapOf<String, Int>()
    val foldedKeysByTab = mutableStateMapOf<String, Set<String>>()
    val undoStacks = mutableStateMapOf<String, List<String>>()
    val redoStacks = mutableStateMapOf<String, List<String>>()

    fun dispatch(command: EmscriptEditorCommand) {
        commandSequence += 1
        pendingCommand = EmscriptEditorCommandEvent(commandSequence, command)
    }

    fun pushUndo(tabId: String, text: String, maxDepth: Int = 100) {
        undoStacks[tabId] = (undoStacks[tabId].orEmpty() + text).takeLast(maxDepth)
        redoStacks[tabId] = emptyList()
    }

    fun popUndo(tabId: String): String? {
        val stack = undoStacks[tabId].orEmpty()
        val previous = stack.lastOrNull() ?: return null
        undoStacks[tabId] = stack.dropLast(1)
        return previous
    }

    fun pushRedo(tabId: String, text: String, maxDepth: Int = 100) {
        redoStacks[tabId] = (redoStacks[tabId].orEmpty() + text).takeLast(maxDepth)
    }

    fun popRedo(tabId: String): String? {
        val stack = redoStacks[tabId].orEmpty()
        val next = stack.lastOrNull() ?: return null
        redoStacks[tabId] = stack.dropLast(1)
        return next
    }
}
