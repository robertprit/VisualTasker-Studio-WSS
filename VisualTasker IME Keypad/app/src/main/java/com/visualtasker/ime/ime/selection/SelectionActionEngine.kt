package com.visualtasker.ime.ime.selection

import com.visualtasker.ime.ime.context.KeyboardProfile

data class SelectionAction(
    val id: String,
    val label: String,
    val replacement: String
)

object SelectionActionEngine {
    private val twoPointRegex = Regex("""^\s*(-?\d+)\s*,\s*(-?\d+)\s*$""")
    private val swipeRegex = Regex("""^\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*$""")

    fun actionsForSelection(selection: String, profile: KeyboardProfile): List<SelectionAction> {
        if (selection.isBlank()) return emptyList()
        val actions = mutableListOf<SelectionAction>()
        val trimmed = selection.trim()

        twoPointRegex.matchEntire(trimmed)?.let { match ->
            val (x, y) = match.destructured
            actions += SelectionAction(
                id = "emscript_click_xy",
                label = "Als click(x, y)",
                replacement = "click($x, $y)"
            )
        }

        swipeRegex.matchEntire(trimmed)?.let { match ->
            val (x1, y1, x2, y2) = match.destructured
            actions += SelectionAction(
                id = "emscript_swipe_coords",
                label = "Als swipe(x1,y1,x2,y2)",
                replacement = "swipe($x1, $y1, $x2, $y2, 250)"
            )
        }

        if (profile == KeyboardProfile.EMSCRIPT || profile == KeyboardProfile.VISIONTASKER || profile == KeyboardProfile.DEVELOPER) {
            val escaped = trimmed.replace("\"", "\\\"")
            actions += SelectionAction(
                id = "emscript_click_text",
                label = "Als clickText(\"...\")",
                replacement = "clickText(\"$escaped\")"
            )
        }
        return actions
    }
}
