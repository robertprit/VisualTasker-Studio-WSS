package com.visualtasker.wss.workspace.plugin.blockeditor

import com.visualtasker.wss.workspace.plugin.RecordingShellPluginHostAdapter
import com.visualtasker.wss.workspace.plugin.ShellDirtyState
import com.visualtasker.wss.workspace.plugin.ShellDocumentId
import com.visualtasker.wss.workspace.plugin.ShellEditorCloseState
import com.visualtasker.wss.workspace.plugin.ShellEditorInput
import com.visualtasker.wss.workspace.plugin.ShellEditorOutputDisposition
import com.visualtasker.wss.workspace.plugin.ShellPluginSessionId
import com.visualtasker.wss.workspace.plugin.ShellSaveAcknowledgmentResult
import com.visualtasker.wss.workspace.plugin.WorkspaceShellPluginHostCoordinator
import com.visualtasker.wss.workspace.plugin.defaultWorkspaceShellPluginRegistry
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.blockeditor.serialization.BlockEditorDocumentFormats
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockEditorShellPluginTest {
    @Test
    fun opensNativeWorkspaceDocumentThroughShellContract() {
        val host = RecordingShellPluginHostAdapter()
        val plugin = BlockEditorShellPlugin()

        val session = plugin.createEditorSession(sampleInput(), host) as BlockEditorShellEditorSession

        assertEquals(BlockEditorShellPlugin.PLUGIN_ID, plugin.pluginId.value)
        assertEquals(setOf(BlockEditorDocumentFormats.WORKSPACE_JSON), plugin.supportedFormatIds)
        assertEquals(ShellDirtyState.CLEAN, session.dirtyState)
        assertEquals(ShellEditorCloseState.CAN_CLOSE, session.closeState)
        assertFalse(session.isDisposed())
        assertEquals(listOf(sampleInput().sessionId to ShellDirtyState.CLEAN), host.recordedDirtyStates())
        assertTrue(host.recordedDiagnostics().isNotEmpty())
    }

    @Test
    fun documentMutationsBecomeDirtyAndSaveAcknowledgmentCleansSession() {
        val host = RecordingShellPluginHostAdapter()
        val session = BlockEditorShellPlugin().createEditorSession(
            sampleInput(content = WorkspaceSerializer.serialize(WorkspaceBootstrap.empty())),
            host
        ) as BlockEditorShellEditorSession

        session.controller.clearWorkspace()

        assertEquals(ShellDirtyState.DIRTY, session.dirtyState)
        assertEquals(ShellEditorCloseState.UNSAVED_CHANGES, session.closeState)
        val output = session.requestSave()
        assertEquals(ShellEditorOutputDisposition.DOCUMENT_SAVE, output.disposition)
        assertEquals(BlockEditorDocumentFormats.WORKSPACE_JSON, output.formatId)
        assertEquals(1, host.recordedSaveRequests().size)

        assertEquals(ShellSaveAcknowledgmentResult.APPLIED, session.acknowledgeSave(output))
        assertEquals(ShellDirtyState.CLEAN, session.dirtyState)
        assertEquals(ShellEditorCloseState.CAN_CLOSE, session.closeState)
    }

    @Test
    fun staleSaveAcknowledgmentDoesNotCleanDirtySession() {
        val session = BlockEditorShellPlugin().createEditorSession(
            sampleInput(content = WorkspaceSerializer.serialize(WorkspaceBootstrap.empty())),
            RecordingShellPluginHostAdapter()
        ) as BlockEditorShellEditorSession

        session.controller.clearWorkspace()
        val staleOutput = session.requestSave().copy(content = "{}")

        assertEquals(ShellSaveAcknowledgmentResult.STALE, session.acknowledgeSave(staleOutput))
        assertEquals(ShellDirtyState.DIRTY, session.dirtyState)
    }

    @Test
    fun publishesEmscriptAsDraftExportNotDocumentSave() {
        val host = RecordingShellPluginHostAdapter()

        BlockEditorShellPlugin().createEditorSession(sampleInput(), host)

        val draft = host.recordedOutputs().firstOrNull {
            it.formatId == BlockEditorDocumentFormats.EMSCRIPT
        }
        assertEquals(ShellEditorOutputDisposition.DRAFT_EXPORT, draft?.disposition)
    }

    @Test
    fun validatesAgainstBlockeditorRegistryAndReportsDiagnostics() {
        val host = RecordingShellPluginHostAdapter()
        val session = BlockEditorShellPlugin().createEditorSession(sampleInput(), host)

        val result = session.validate()

        assertTrue(result.isValid)
        assertEquals(result, host.recordedDiagnostics().last().second)
    }

    @Test
    fun defaultShellRegistryOpensBlockeditorThroughCoordinator() {
        val registry = defaultWorkspaceShellPluginRegistry()
        val coordinator = WorkspaceShellPluginHostCoordinator(
            pluginLookup = registry::findEditorPlugin
        )

        val bound = coordinator.openEditor("BLOCKEDITOR", sampleInput())

        assertTrue(bound.session is BlockEditorShellEditorSession)
        bound.close()
        assertTrue((bound.session as BlockEditorShellEditorSession).isDisposed())
    }

    private fun sampleInput(
        content: String = WorkspaceSerializer.serialize(WorkspaceBootstrap.starter())
    ): ShellEditorInput =
        ShellEditorInput(
            sessionId = ShellPluginSessionId("session-1"),
            documentId = ShellDocumentId("workflow-1"),
            formatId = BlockEditorDocumentFormats.WORKSPACE_JSON,
            revision = "rev-1",
            content = content
        )
}
