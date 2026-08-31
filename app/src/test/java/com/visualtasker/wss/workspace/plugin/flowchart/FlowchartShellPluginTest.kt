package com.visualtasker.wss.workspace.plugin.flowchart

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
import de.visualtasker.flowchart.serialization.FlowGraphJsonCodec
import de.visualtasker.flowchart.testsupport.FlowchartFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowchartShellPluginTest {
    @Test
    fun opensFlowGraphThroughShellContract() {
        val host = RecordingShellPluginHostAdapter()
        val plugin = FlowchartShellPlugin()

        val session = plugin.createEditorSession(sampleInput(), host) as FlowchartShellEditorSession

        assertEquals(FlowchartShellPlugin.PLUGIN_ID, plugin.pluginId.value)
        assertEquals(setOf(FlowchartShellPlugin.FLOW_GRAPH_JSON), plugin.supportedFormatIds)
        assertEquals(ShellDirtyState.CLEAN, session.dirtyState)
        assertEquals(ShellEditorCloseState.CAN_CLOSE, session.closeState)
        assertFalse(session.isDisposed())
        assertTrue(host.recordedDiagnostics().isNotEmpty())
        assertEquals("ATTACHED", host.recordedRuntimeStates().last().second.status)
    }

    @Test
    fun viewChangesBecomeDraftExportsAndCanBeAcknowledged() {
        val host = RecordingShellPluginHostAdapter()
        val session = FlowchartShellPlugin().createEditorSession(sampleInput(), host) as FlowchartShellEditorSession
        val view = requireNotNull(session.controller.snapshot().view)

        session.onViewDocumentChanged(view)

        assertEquals(ShellDirtyState.DIRTY, session.dirtyState)
        val output = session.requestSave()
        assertEquals(FlowchartShellPlugin.FLOW_VIEW_JSON, output.formatId)
        assertEquals(ShellEditorOutputDisposition.DRAFT_EXPORT, output.disposition)
        assertEquals(1, host.recordedSaveRequests().size)

        assertEquals(ShellSaveAcknowledgmentResult.APPLIED, session.acknowledgeSave(output))
        assertEquals(ShellDirtyState.CLEAN, session.dirtyState)
    }

    @Test
    fun defaultShellRegistryOpensFlowchartThroughCoordinator() {
        val registry = defaultWorkspaceShellPluginRegistry()
        val coordinator = WorkspaceShellPluginHostCoordinator(
            pluginLookup = registry::findEditorPlugin
        )

        val bound = coordinator.openEditor("Flowchart", sampleInput())

        assertTrue(bound.session is FlowchartShellEditorSession)
        bound.close()
        assertTrue((bound.session as FlowchartShellEditorSession).isDisposed())
    }

    private fun sampleInput(): ShellEditorInput =
        ShellEditorInput(
            sessionId = ShellPluginSessionId("flowchart-session-1"),
            documentId = ShellDocumentId("workflow-1-flowchart"),
            formatId = FlowchartShellPlugin.FLOW_GRAPH_JSON,
            revision = "rev-1",
            content = FlowGraphJsonCodec().encodeCanonical(FlowchartFixtures.linear())
        )
}
