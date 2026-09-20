package com.visualtasker.ime.ime.toolbar

enum class ToolbarAction {
    UNDO,
    REDO,
    CUT,
    COPY,
    PASTE,
    CLIPBOARD_HISTORY,
    QWERTZ,
    SPECIAL_1,
    SPECIAL_2,
    COLOR_PICKER,
    CALCULATOR,
    HANDWRITING,
    ESCAPE,
    TAB,
    HOME,
    END,
    WORKFLOW_START,
    WORKFLOW_STOP,
    RECORDER_TOGGLE,
    WATCHDOG_TOGGLE,
    VT_SLOT_1,
    VT_SLOT_2,
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    F7,
    F8,
    F9,
    F10,
    F11,
    F12
}

data class ToolbarItem(
    val action: ToolbarAction,
    val label: String,
    val contentDescription: String,
    val androidIcon: Int? = null
)

data class ToolbarPage(
    val name: String,
    val items: List<ToolbarItem>
)

object ToolbarPageCatalog {
    fun pages(vtSlotLabels: List<String> = listOf("VT1", "VT2")): List<ToolbarPage> = listOf(
        ToolbarPage(
            name = "Werkzeuge",
            items = listOf(
                ToolbarItem(ToolbarAction.UNDO, "Undo", "Rückgängig", android.R.drawable.ic_menu_revert),
                ToolbarItem(ToolbarAction.REDO, "Redo", "Wiederholen", android.R.drawable.ic_menu_rotate),
                ToolbarItem(ToolbarAction.CUT, "Cut", "Ausschneiden", android.R.drawable.ic_menu_crop),
                ToolbarItem(ToolbarAction.COPY, "Copy", "Kopieren", android.R.drawable.ic_menu_save),
                ToolbarItem(ToolbarAction.PASTE, "Paste", "Einfügen", android.R.drawable.ic_menu_set_as),
                ToolbarItem(ToolbarAction.CLIPBOARD_HISTORY, "Clip", "Zwischenablage-Verlauf", android.R.drawable.ic_menu_agenda)
            )
        ),
        ToolbarPage(
            name = "Modi",
            items = listOf(
                ToolbarItem(ToolbarAction.QWERTZ, "ABC", "QWERTZ"),
                ToolbarItem(ToolbarAction.SPECIAL_1, "#1", "Sondertasten 1"),
                ToolbarItem(ToolbarAction.SPECIAL_2, "#2", "Sondertasten 2"),
                ToolbarItem(ToolbarAction.COLOR_PICKER, "Color", "ColourPicker", android.R.drawable.ic_menu_gallery),
                ToolbarItem(ToolbarAction.CALCULATOR, "123", "Taschenrechner"),
                ToolbarItem(ToolbarAction.HANDWRITING, "Pen", "Handschrift", android.R.drawable.ic_menu_edit)
            )
        ),
        ToolbarPage(
            name = "F-Tasten",
            items = (1..12).map { number ->
                ToolbarItem(
                    action = ToolbarAction.valueOf("F$number"),
                    label = "F$number",
                    contentDescription = "Funktionstaste F$number"
                )
            }
        ),
        ToolbarPage(
            name = "VisualTasker",
            items = listOf(
                ToolbarItem(ToolbarAction.ESCAPE, "Esc", "Escape"),
                ToolbarItem(ToolbarAction.TAB, "Tab", "Tabulator"),
                ToolbarItem(ToolbarAction.HOME, "Home", "Pos1"),
                ToolbarItem(ToolbarAction.END, "End", "Ende"),
                ToolbarItem(ToolbarAction.WORKFLOW_START, "Run", "VisualTasker Workflow starten", android.R.drawable.ic_media_play),
                ToolbarItem(ToolbarAction.WORKFLOW_STOP, "Stop", "VisualTasker Workflow stoppen", android.R.drawable.ic_media_pause),
                ToolbarItem(ToolbarAction.RECORDER_TOGGLE, "Rec", "VisualTasker Aufnahme starten oder stoppen", android.R.drawable.presence_video_online),
                ToolbarItem(ToolbarAction.WATCHDOG_TOGGLE, "Watch", "VisualTasker Watchdog pausieren oder fortsetzen", android.R.drawable.ic_menu_view),
                ToolbarItem(ToolbarAction.VT_SLOT_1, vtSlotLabels.getOrElse(0) { "VT1" }, "VisualTasker frei belegbare Taste 1"),
                ToolbarItem(ToolbarAction.VT_SLOT_2, vtSlotLabels.getOrElse(1) { "VT2" }, "VisualTasker frei belegbare Taste 2")
            )
        )
    )
}
