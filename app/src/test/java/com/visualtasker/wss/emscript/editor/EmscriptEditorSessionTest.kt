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

    @Test
    fun replacingManualContentForAnotherRootDoesNotMarkItDirty() {
        val session = EmscriptEditorSession.create("OUTPUT first")
            .updateManualContent("OUTPUT changed")
            .replaceManualContent("OUTPUT second")

        assertEquals("OUTPUT second", session.activeTab.content)
        assertFalse(session.activeTab.dirty)
    }

    @Test
    fun rootSwitchClearsTextEditorDocumentState() {
        val state = EmscriptEditorUiState().apply {
            selectionStarts[EmscriptEditorSession.MANUAL_TAB_ID] = 4
            selectionEnds[EmscriptEditorSession.MANUAL_TAB_ID] = 8
            foldedKeysByTab[EmscriptEditorSession.MANUAL_TAB_ID] = setOf("if:1")
            pushUndo(EmscriptEditorSession.MANUAL_TAB_ID, "OUTPUT old")
            pushRedo(EmscriptEditorSession.MANUAL_TAB_ID, "OUTPUT newer")
        }

        state.resetDocumentState(EmscriptEditorSession.MANUAL_TAB_ID)

        assertFalse(EmscriptEditorSession.MANUAL_TAB_ID in state.selectionStarts)
        assertFalse(EmscriptEditorSession.MANUAL_TAB_ID in state.selectionEnds)
        assertFalse(EmscriptEditorSession.MANUAL_TAB_ID in state.foldedKeysByTab)
        assertFalse(EmscriptEditorSession.MANUAL_TAB_ID in state.undoStacks)
        assertFalse(EmscriptEditorSession.MANUAL_TAB_ID in state.redoStacks)
    }
}
