package com.visualtasker.ime.ime.input

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import kotlin.math.max
import kotlin.math.min

data class SelectionRange(
    val start: Int,
    val end: Int
) {
    val isCollapsed: Boolean get() = start == end
}

class InputConnectionFacade(
    private val connectionProvider: () -> InputConnection?,
    private val editorInfoProvider: () -> EditorInfo?
) {
    fun insertText(text: String): Boolean {
        val ic = connectionProvider() ?: return false
        return ic.commitText(text, 1)
    }

    fun replaceSelection(text: String): Boolean {
        val ic = connectionProvider() ?: return false
        return ic.commitText(text, 1)
    }

    fun replaceSelectionAndSelect(text: String, relativeSelectionStart: Int, relativeSelectionEnd: Int): Boolean {
        val ic = connectionProvider() ?: return false
        val before = getSelectionRange() ?: return ic.commitText(text, 1)
        val changed = ic.commitText(text, 1)
        if (!changed) return false
        val normalizedStart = min(relativeSelectionStart, relativeSelectionEnd)
        val normalizedEnd = max(relativeSelectionStart, relativeSelectionEnd)
        val absoluteStart = before.start + normalizedStart
        val absoluteEnd = before.start + normalizedEnd
        return ic.setSelection(absoluteStart, absoluteEnd)
    }

    fun shiftTabSelection(): Boolean {
        val selected = selectedText()
        if (selected.isBlank()) return false
        val transformed = selected
            .lines()
            .joinToString("\n") { line ->
                when {
                    line.startsWith("\t") -> line.drop(1)
                    line.startsWith("    ") -> line.drop(4)
                    line.startsWith("  ") -> line.drop(2)
                    else -> line
                }
            }
        return replaceSelection(transformed)
    }

    fun selectedText(): String {
        val ic = connectionProvider() ?: return ""
        return ic.getSelectedText(0)?.toString().orEmpty()
    }

    fun setSelection(start: Int, end: Int): Boolean {
        val ic = connectionProvider() ?: return false
        return ic.setSelection(start, end)
    }

    fun getTextBeforeCursor(maxChars: Int = 200): String {
        val ic = connectionProvider() ?: return ""
        return ic.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()
    }

    fun getTextAfterCursor(maxChars: Int = 200): String {
        val ic = connectionProvider() ?: return ""
        return ic.getTextAfterCursor(maxChars, 0)?.toString().orEmpty()
    }

    fun deleteSelectionOrCharacterBeforeCursor(): Boolean {
        val range = getSelectionRange() ?: return false
        val ic = connectionProvider() ?: return false
        if (!range.isCollapsed) {
            return ic.commitText("", 1)
        }
        return ic.deleteSurroundingText(1, 0)
    }

    fun deleteWordBeforeCursor(): Boolean {
        val before = getTextBeforeCursor(256)
        if (before.isEmpty()) return false
        val toDelete = before.takeLastWhile { !it.isWhitespace() }.length
            .takeIf { it > 0 } ?: before.takeLastWhile { it.isWhitespace() }.length
        if (toDelete <= 0) return false
        val ic = connectionProvider() ?: return false
        return ic.deleteSurroundingText(toDelete, 0)
    }

    fun deleteWordAfterCursor(): Boolean {
        val after = getTextAfterCursor(256)
        if (after.isEmpty()) return false
        val toDelete = after.takeWhile { !it.isWhitespace() }.length
            .takeIf { it > 0 } ?: after.takeWhile { it.isWhitespace() }.length
        if (toDelete <= 0) return false
        val ic = connectionProvider() ?: return false
        return ic.deleteSurroundingText(0, toDelete)
    }

    fun moveCursorByWord(direction: Int): Boolean {
        val range = getSelectionRange() ?: return false
        val ic = connectionProvider() ?: return false
        val before = getTextBeforeCursor(256)
        val after = getTextAfterCursor(256)
        return if (direction < 0) {
            val move = wordJumpLeft(before)
            ic.setSelection(max(0, range.start - move), max(0, range.start - move))
        } else {
            val move = wordJumpRight(after)
            ic.setSelection(range.end + move, range.end + move)
        }
    }

    fun moveCursorToLineBoundary(toStart: Boolean): Boolean {
        val range = getSelectionRange() ?: return false
        val ic = connectionProvider() ?: return false
        val before = getTextBeforeCursor(512)
        val after = getTextAfterCursor(512)
        return if (toStart) {
            val idx = before.lastIndexOf('\n')
            val target = if (idx >= 0) range.start - (before.length - idx - 1) else range.start - before.length
            ic.setSelection(max(0, target), max(0, target))
        } else {
            val idx = after.indexOf('\n')
            val target = if (idx >= 0) range.end + idx else range.end + after.length
            ic.setSelection(target, target)
        }
    }

    fun performImeActionOrEnter(): Boolean {
        val ic = connectionProvider() ?: return false
        val info = editorInfoProvider()
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_UNSPECIFIED
        return if (action != EditorInfo.IME_ACTION_UNSPECIFIED && action != EditorInfo.IME_ACTION_NONE) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
    }

    fun performImeAction(action: Int): Boolean {
        val ic = connectionProvider() ?: return false
        return ic.performEditorAction(action)
    }

    fun getSelectionRange(): SelectionRange? {
        val ic = connectionProvider() ?: return null
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0) ?: return null
        val start = extracted.selectionStart
        val end = extracted.selectionEnd
        if (start < 0 || end < 0) return null
        return SelectionRange(start, end)
    }

    private fun wordJumpLeft(textBeforeCursor: String): Int {
        if (textBeforeCursor.isEmpty()) return 0
        val trimmed = textBeforeCursor.trimEnd()
        if (trimmed.isEmpty()) return textBeforeCursor.length
        val wsSuffix = textBeforeCursor.length - trimmed.length
        val word = trimmed.takeLastWhile { !it.isWhitespace() }.length
        return wsSuffix + word
    }

    private fun wordJumpRight(textAfterCursor: String): Int {
        if (textAfterCursor.isEmpty()) return 0
        val wsPrefix = textAfterCursor.takeWhile { it.isWhitespace() }.length
        if (wsPrefix > 0) return wsPrefix
        return textAfterCursor.takeWhile { !it.isWhitespace() }.length
    }
}
