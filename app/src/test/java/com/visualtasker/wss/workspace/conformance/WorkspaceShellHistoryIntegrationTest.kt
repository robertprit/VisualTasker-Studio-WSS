package com.visualtasker.wss.workspace.conformance

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.editor.EmscriptEditorSession
import com.visualtasker.wss.emscript.editor.EmscriptEditorUiState
import com.visualtasker.wss.workspace.model.FlowchartWorkspaceMutation
import com.visualtasker.wss.workspace.model.WORKFLOW_SOURCE_EMSCRIPT_APPLY
import com.visualtasker.wss.workspace.model.WORKFLOW_SOURCE_FLOWCHART_PREFIX
import com.visualtasker.wss.workspace.model.WorkspaceSelectionResolver
import com.visualtasker.wss.workspace.model.WorkspaceSelectionState
import com.visualtasker.wss.workspace.model.WorkspaceShellHistoryController
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import com.visualtasker.wss.workspace.model.applyFlowchartWorkspaceMutation
import com.visualtasker.wss.workspace.model.toFlowNodeId
import de.visualtasker.blockeditor.compose.host.BlockEditorController
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.WorkspaceAction
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.core.WorkspaceReducer
import de.visualtasker.workflow.serialization.WorkflowSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceShellHistoryIntegrationTest {
    @Test
    fun textEditorDraftChangeUndoRedoUsesSourceApplyAndKeepsIdentityAnchorsAndMetadata() {
        val imported = importDocument("wait(100)", "shell-text")
        val anchoredSource = ThreeEditorConformanceHarness.project(imported).generatedSource
        val anchored = applySource(imported, anchoredSource, "shell-text")
        val wait = command(anchored, BlockTypes.ACTION_WAIT)
        val decorated = anchored.copy(
            blocks = anchored.blocks + (
                wait.id to wait.copy(metadata = wait.metadata + ("unknown.owner" to "m4-3"))
            ),
        )
        val shell = ShellHarness(decorated)
        val editorHistory = EmscriptEditorUiState()
        val tabId = EmscriptEditorSession.MANUAL_TAB_ID
        val before = ThreeEditorConformanceHarness.snapshot(shell.state.document)
        val beforeSource = ThreeEditorConformanceHarness.project(shell.state.document).generatedSource
        val changedSource = beforeSource.replaceFirst(Regex("wait\\([^\\n)]*\\)"), "wait(250)")

        editorHistory.pushUndo(tabId, beforeSource)
        assertTrue(shell.applySource(changedSource, "$WORKFLOW_SOURCE_EMSCRIPT_APPLY:texteditor"))
        val changed = ThreeEditorConformanceHarness.snapshot(shell.state.document)
        val changedVersion = shell.state.document.version

        val undoSource = requireNotNull(editorHistory.popUndo(tabId))
        editorHistory.pushRedo(tabId, changedSource)
        assertTrue(shell.applySource(undoSource, "$WORKFLOW_SOURCE_EMSCRIPT_APPLY:texteditor:undo"))
        assertEquals(changedVersion + 1, shell.state.document.version)
        assertEquals(before.semanticStructure, ThreeEditorConformanceHarness.snapshot(shell.state.document).semanticStructure)
        assertEquals(before.identityStructure, ThreeEditorConformanceHarness.snapshot(shell.state.document).identityStructure)
        assertEquals(before.metadataStructure, ThreeEditorConformanceHarness.snapshot(shell.state.document).metadataStructure)

        val redoSource = requireNotNull(editorHistory.popRedo(tabId))
        editorHistory.pushUndo(tabId, undoSource)
        assertTrue(shell.applySource(redoSource, "$WORKFLOW_SOURCE_EMSCRIPT_APPLY:texteditor:redo"))
        assertEquals(changed.semanticStructure, ThreeEditorConformanceHarness.snapshot(shell.state.document).semanticStructure)
        assertEquals(changed.identityStructure, ThreeEditorConformanceHarness.snapshot(shell.state.document).identityStructure)
        assertEquals("m4-3", shell.state.document.blocks.getValue(wait.id).metadata["unknown.owner"])
        assertEquals(sourceDirectives(beforeSource), sourceDirectives(ThreeEditorConformanceHarness.project(shell.state.document).generatedSource))
    }

    @Test
    fun flowMutationUndoRedoRestoresExactCanonicalSnapshots() {
        val shell = ShellHarness(importDocument("wait(100)", "shell-flow"))
        val wait = command(shell.state.document, BlockTypes.ACTION_WAIT)
        val before = ThreeEditorConformanceHarness.snapshot(shell.state.document)
        val result = applyFlowchartWorkspaceMutation(
            shell.state.document,
            FlowchartWorkspaceMutation.UpdateNodeField(wait.id.toFlowNodeId(), "ms", "500"),
        )

        assertTrue(result.applied)
        assertTrue(shell.publish(result.document, "${WORKFLOW_SOURCE_FLOWCHART_PREFIX}wait:field"))
        val changed = ThreeEditorConformanceHarness.snapshot(shell.state.document)
        assertEquals(before.document.version + 1, changed.document.version)
        assertTrue(shell.undo())
        assertEquals(before, ThreeEditorConformanceHarness.snapshot(shell.state.document))
        assertTrue(shell.redo())
        assertEquals(changed, ThreeEditorConformanceHarness.snapshot(shell.state.document))
    }

    @Test
    fun blockMutationSurvivesFlowProjectionAndShellUndo() {
        val shell = ShellHarness(importDocument("wait(100)\nbeep()", "shell-block-flow"))
        val wait = command(shell.state.document, BlockTypes.ACTION_WAIT)
        val before = ThreeEditorConformanceHarness.snapshot(shell.state.document)
        val changed = withBlockEditor(shell.state.document) { controller ->
            controller.onAction(WorkspaceAction.UpdateField(wait.id, "ms", FieldValue.Number(300.0)))
            controller.document
        }

        assertTrue(shell.publish(changed, "blockeditor:panel-1"))
        val projected = ThreeEditorConformanceHarness.project(shell.state.document)
        assertTrue("block:${wait.id.value}" in projected.flowNodeIds)
        assertFalse(shell.recordEditorSwitch("floweditor"))
        assertTrue(shell.undo())
        assertEquals(before, ThreeEditorConformanceHarness.snapshot(shell.state.document))
    }

    @Test
    fun flowMutationSurvivesTextProjectionAndShellRedo() {
        val shell = ShellHarness(importDocument("wait(100)", "shell-flow-text"))
        val wait = command(shell.state.document, BlockTypes.ACTION_WAIT)
        val result = applyFlowchartWorkspaceMutation(
            shell.state.document,
            FlowchartWorkspaceMutation.UpdateNodeField(wait.id.toFlowNodeId(), "ms", "700"),
        )
        assertTrue(shell.publish(result.document, "${WORKFLOW_SOURCE_FLOWCHART_PREFIX}wait:field"))
        val changed = ThreeEditorConformanceHarness.snapshot(shell.state.document)
        assertTrue(shell.undo())

        val textProjection = ThreeEditorConformanceHarness.project(shell.state.document).generatedSource
        assertTrue(textProjection.contains("wait(100"))
        assertFalse(shell.recordEditorSwitch("texteditor"))
        assertTrue(shell.redo())
        assertEquals(changed, ThreeEditorConformanceHarness.snapshot(shell.state.document))
    }

    @Test
    fun deletingSelectedElementClearsSelectionAndUndoRestoresIdentityAndFocusTarget() {
        val shell = ShellHarness(importDocument("wait(100)\nbeep()", "shell-delete"))
        val wait = command(shell.state.document, BlockTypes.ACTION_WAIT)
        shell.selection = shell.selection.selectBlock(wait.id)
        val before = ThreeEditorConformanceHarness.snapshot(shell.state.document)
        val result = applyFlowchartWorkspaceMutation(
            shell.state.document,
            FlowchartWorkspaceMutation.DeleteNode(wait.id.toFlowNodeId()),
        )

        assertTrue(shell.publish(result.document, "${WORKFLOW_SOURCE_FLOWCHART_PREFIX}wait:delete"))
        assertNull(shell.selection.blockId)
        assertNull(shell.selection.flowNodeId)
        assertTrue(shell.undo())
        assertEquals(before, ThreeEditorConformanceHarness.snapshot(shell.state.document))
        assertEquals(wait.id, shell.selection.blockId)
        assertEquals(wait.id.toFlowNodeId(), shell.selection.flowNodeId)
    }

    @Test
    fun invalidSelectionAfterEditorSwitchFallsBackWithoutInventingIdentity() {
        val shell = ShellHarness(importDocument("wait(100)", "shell-invalid-selection"))
        shell.selection = WorkspaceSelectionState().selectBlock(BlockId("missing"))
        val revision = shell.state.document.version

        shell.selection = WorkspaceSelectionResolver.reconcile(shell.state.document, shell.selection)
        assertFalse(shell.recordEditorSwitch("floweditor"))

        assertNull(shell.selection.blockId)
        assertNull(shell.selection.flowNodeId)
        assertEquals(revision, shell.state.document.version)
        assertEquals(0, shell.history.undoDepth)
    }

    @Test
    fun multipleTransactionsUndoAndRedoInStrictReverseOrder() {
        val shell = ShellHarness(importDocument("wait(100)", "shell-order"))
        val wait = command(shell.state.document, BlockTypes.ACTION_WAIT)
        val snapshots = mutableListOf(ThreeEditorConformanceHarness.snapshot(shell.state.document))
        listOf(200.0, 300.0, 400.0).forEachIndexed { index, value ->
            val changed = withBlockEditor(shell.state.document) { controller ->
                controller.onAction(WorkspaceAction.UpdateField(wait.id, "ms", FieldValue.Number(value)))
                controller.document
            }
            assertTrue(shell.publish(changed, "blockeditor:transaction-$index"))
            snapshots += ThreeEditorConformanceHarness.snapshot(shell.state.document)
        }

        assertEquals(3, shell.history.undoDepth)
        for (index in 2 downTo 0) {
            assertTrue(shell.undo())
            assertEquals(snapshots[index], ThreeEditorConformanceHarness.snapshot(shell.state.document))
        }
        for (index in 1..3) {
            assertTrue(shell.redo())
            assertEquals(snapshots[index], ThreeEditorConformanceHarness.snapshot(shell.state.document))
        }
    }

    @Test
    fun rejectedMutationCreatesNoHistoryEntry() {
        val shell = ShellHarness(importDocument("wait(100)", "shell-rejected"))
        val wait = command(shell.state.document, BlockTypes.ACTION_WAIT)
        val result = applyFlowchartWorkspaceMutation(
            shell.state.document,
            FlowchartWorkspaceMutation.UpdateNodeField(wait.id.toFlowNodeId(), "missing-field", "500"),
        )

        assertFalse(result.applied)
        assertFalse(shell.publish(result.document, "${WORKFLOW_SOURCE_FLOWCHART_PREFIX}invalid"))
        assertEquals(0, shell.history.undoDepth)
        assertFalse(shell.undo())
    }

    @Test
    fun visualMoveChangesProjectionWithoutCanonicalRevisionOrSemanticHistory() {
        val shell = ShellHarness(importDocument("wait(100)", "shell-visual"))
        assertTrue(shell.applySource("wait(250)", "$WORKFLOW_SOURCE_EMSCRIPT_APPLY:visual-baseline"))
        val semanticChange = ThreeEditorConformanceHarness.snapshot(shell.state.document)
        assertTrue(shell.undo())
        assertEquals(1, shell.history.redoDepth)
        val rootId = shell.state.document.rootBlocks.single()
        val beforeCanonical = shell.state.canonicalDocument
        val moved = WorkspaceReducer.reduce(
            shell.state.document,
            WorkspaceAction.MoveRoot(rootId, 900f, 700f),
        )

        assertFalse(moved == shell.state.document)
        assertEquals(shell.state.document.version, moved.version)
        assertEquals(beforeCanonical, requireNotNull(moved.canonical))
        assertFalse(shell.publish(moved, "blockeditor:panel-1"))
        assertEquals(0, shell.history.undoDepth)
        assertEquals(1, shell.history.redoDepth)
        assertTrue(shell.redo())
        assertEquals(semanticChange, ThreeEditorConformanceHarness.snapshot(shell.state.document))
    }

    @Test
    fun saveReloadKeepsInMemoryHistoryButProcessRestartDoesNotPersistIt() {
        val shell = ShellHarness(importDocument("wait(100)", "shell-save-reload"))
        assertTrue(shell.applySource("wait(250)", "$WORKFLOW_SOURCE_EMSCRIPT_APPLY:save"))
        assertEquals(1, shell.history.undoDepth)

        shell.reloadCurrentDocument()
        assertEquals(1, shell.history.undoDepth)
        assertTrue(shell.undo())

        val restartedHistory = WorkspaceShellHistoryController()
        assertEquals(0, restartedHistory.undoDepth)
        assertEquals(0, restartedHistory.redoDepth)
    }

    private class ShellHarness(initialDocument: WorkspaceDocument) {
        val history = WorkspaceShellHistoryController()
        var state = WorkspaceWorkflowState.fromDocument(initialDocument, "shell:test")
        var selection = WorkspaceSelectionState()

        fun applySource(source: String, mutationSource: String): Boolean {
            val result = EmscriptApplyGuard().preview(
                source,
                workspaceId = state.document.id,
                previousDocument = state.document,
            ) as? EmscriptApplyGuardResult.Success ?: return false
            return publish(result.importedDocument, mutationSource)
        }

        fun publish(document: WorkspaceDocument, source: String): Boolean {
            val next = WorkspaceWorkflowState.fromDocument(document, source)
            val recorded = history.recordAcceptedTransition(state, next, selection, source)
            state = next
            selection = WorkspaceSelectionResolver.reconcile(state.document, selection)
            return recorded
        }

        fun undo(): Boolean {
            val entry = history.undo(state, selection) ?: return false
            state = WorkspaceWorkflowState.fromSerialized(entry.serializedJson, "workspace:undo")
            selection = WorkspaceSelectionResolver.reconcile(state.document, entry.selection)
            return true
        }

        fun redo(): Boolean {
            val entry = history.redo(state, selection) ?: return false
            state = WorkspaceWorkflowState.fromSerialized(entry.serializedJson, "workspace:redo")
            selection = WorkspaceSelectionResolver.reconcile(state.document, entry.selection)
            return true
        }

        fun recordEditorSwitch(editor: String): Boolean = history.recordAcceptedTransition(
            current = state,
            next = WorkspaceWorkflowState.fromDocument(state.document, "editor-switch:$editor"),
            selection = selection,
            source = "editor-switch:$editor",
        )

        fun reloadCurrentDocument() {
            state = WorkspaceWorkflowState.fromSerialized(
                WorkflowSerializer.serialize(state.document),
                "workspace:reload",
            )
            selection = WorkspaceSelectionResolver.reconcile(state.document, selection)
        }
    }

    private fun importDocument(source: String, workspaceId: String): WorkspaceDocument =
        (ThreeEditorConformanceHarness.importSource(source, workspaceId) as EmscriptApplyGuardResult.Success)
            .importedDocument

    private fun applySource(document: WorkspaceDocument, source: String, workspaceId: String): WorkspaceDocument =
        (ThreeEditorConformanceHarness.applySource(source, workspaceId, document) as EmscriptApplyGuardResult.Success)
            .importedDocument

    private fun command(document: WorkspaceDocument, type: String) =
        document.blocks.values.single { it.type == type }

    private inline fun <T> withBlockEditor(document: WorkspaceDocument, block: (BlockEditorController) -> T): T {
        val controller = BlockEditorController(initialDocument = document, debounceMillis = 0L)
        return try {
            block(controller)
        } finally {
            controller.close()
        }
    }

    private fun sourceDirectives(source: String): List<String> = source.lineSequence()
        .map(String::trim)
        .filter { it.startsWith("@source.") }
        .toList()
}
