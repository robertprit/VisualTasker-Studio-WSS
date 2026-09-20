package com.visualtasker.ime.ime.toolbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarPageCatalogTest {
    @Test
    fun exposesFourPagesInRequestedOrder() {
        val pages = ToolbarPageCatalog.pages()

        assertEquals(listOf("Werkzeuge", "Modi", "F-Tasten", "VisualTasker"), pages.map { it.name })
        assertEquals(
            listOf(
                ToolbarAction.UNDO,
                ToolbarAction.REDO,
                ToolbarAction.CUT,
                ToolbarAction.COPY,
                ToolbarAction.PASTE,
                ToolbarAction.CLIPBOARD_HISTORY
            ),
            pages[0].items.map { it.action }
        )
    }

    @Test
    fun functionPageContainsEveryFunctionKeyExactlyOnce() {
        val actions = ToolbarPageCatalog.pages()[2].items.map { it.action }

        assertEquals(12, actions.size)
        assertEquals((1..12).map { ToolbarAction.valueOf("F$it") }, actions)
        assertEquals(actions.size, actions.distinct().size)
    }

    @Test
    fun visualTaskerPageContainsRuntimeAndAssignableActions() {
        val actions = ToolbarPageCatalog.pages()[3].items.map { it.action }.toSet()

        assertTrue(ToolbarAction.WORKFLOW_START in actions)
        assertTrue(ToolbarAction.WORKFLOW_STOP in actions)
        assertTrue(ToolbarAction.RECORDER_TOGGLE in actions)
        assertTrue(ToolbarAction.WATCHDOG_TOGGLE in actions)
        assertTrue(ToolbarAction.VT_SLOT_1 in actions)
        assertTrue(ToolbarAction.VT_SLOT_2 in actions)
    }

    @Test
    fun visualTaskerSlotLabelsCanBeConfigured() {
        val items = ToolbarPageCatalog.pages(listOf("Login", "Watch"))[3].items

        assertEquals("Login", items.first { it.action == ToolbarAction.VT_SLOT_1 }.label)
        assertEquals("Watch", items.first { it.action == ToolbarAction.VT_SLOT_2 }.label)
    }
}
