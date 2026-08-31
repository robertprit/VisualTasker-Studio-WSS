package com.visualtasker.wss.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelTypeRestoreTest {
    @Test
    fun restorePanelType_supportsLogConsole() {
        assertEquals(PanelType.LOG_CONSOLE, restorePanelType("LOG_CONSOLE"))
    }

    @Test
    fun restorePanelType_fallsBackToEditorForUnknownValues() {
        assertEquals(PanelType.EDITOR, restorePanelType("NOT_A_REAL_PANEL"))
    }
}
