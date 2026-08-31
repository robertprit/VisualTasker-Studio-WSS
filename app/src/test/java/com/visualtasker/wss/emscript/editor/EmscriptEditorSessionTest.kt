package com.visualtasker.wss.emscript.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmscriptEditorSessionTest {
    @Test
    fun sessionStartsWithManualTabActive() {
        val session = EmscriptEditorSession.create(manualContent = "OUTPUT hi")
        assertEquals(EmscriptEditorSession.MANUAL_TAB_ID, session.activeTabId)
        assertEquals(EmscriptEditorTabKind.MANUAL, session.activeTab.kind)
        assertFalse(session.activeTab.readOnly)
        assertTrue(session.canExecuteActiveTab())
    }

    @Test
    fun generatedProjectionDoesNotOverwriteManualTab() {
        val session = EmscriptEditorSession.create("OUTPUT manual")
            .updateGeneratedFromBlocks("OUTPUT generated")
        val manual = session.tabs.single { it.kind == EmscriptEditorTabKind.MANUAL }
        assertEquals("OUTPUT manual", manual.content)
    }

    @Test
    fun copyGeneratedToManualSelectsManualTab() {
        val session = EmscriptEditorSession.create("OUTPUT manual")
            .updateGeneratedFromBlocks("OUTPUT generated")
            .copyGeneratedToManual()
        assertEquals(EmscriptEditorSession.MANUAL_TAB_ID, session.activeTabId)
        assertEquals("OUTPUT generated", session.activeTab.content)
        assertFalse(session.activeTab.readOnly)
    }
}
