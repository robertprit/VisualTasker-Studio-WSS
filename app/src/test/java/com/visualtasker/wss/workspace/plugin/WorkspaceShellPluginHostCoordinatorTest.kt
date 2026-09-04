package com.visualtasker.wss.workspace.plugin

import com.visualtasker.wss.data.PanelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceShellPluginHostCoordinatorTest {
    @Test
    fun catalogMapsExistingShellPanelTypesToSharedPluginIds() {
        val mainBinding = WorkspaceShellPluginCatalog.bindingForMainPanelType(PanelType.BLOCKEDITOR)
        val workspaceBinding = WorkspaceShellPluginCatalog.bindingForWorkspacePanelType(
            com.visualtasker.wss.workspace.model.PanelType.BlockEditor
        )

        assertNotNull(mainBinding)
        assertEquals(mainBinding, workspaceBinding)
        assertEquals(ShellPluginId("blockeditor"), mainBinding?.pluginId)
        assertEquals("NATIVE_BLOCK_EDITOR", mainBinding?.studioPanelTypeName)
        assertEquals(WorkflowViewSurface.BLOCK_EDITOR, mainBinding?.viewSurface)
    }

    @Test
    fun flowchartAndEmscriptMapToStablePluginIds() {
        assertEquals(
            ShellPluginId("flowchart"),
            WorkspaceShellPluginCatalog.bindingForShellPanelType("FLOWCHART")?.pluginId
        )
        assertEquals(
            ShellPluginId("emscript"),
            WorkspaceShellPluginCatalog.bindingForShellPanelType("Emscript")?.pluginId
        )
    }

    @Test
    fun opensRegisteredPluginThroughShellCoordinator() {
        val plugin = FakeShellEditorPlugin(ShellPluginId("blockeditor"))
        val host = RecordingShellPluginHostAdapter()
        val coordinator = WorkspaceShellPluginHostCoordinator(
            hostServices = host,
            pluginLookup = { id -> if (id == plugin.pluginId) plugin else null }
        )

        val bound = coordinator.openEditor("BLOCKEDITOR", sampleInput())

        assertEquals(ShellPluginId("blockeditor"), bound.binding.pluginId)
        assertEquals(WorkflowViewSurface.BLOCK_EDITOR, bound.binding.viewSurface)
        val session = bound.session as FakeShellEditorSession
        assertTrue(session.isActive)
        assertEquals(ShellSaveAcknowledgmentResult.APPLIED, bound.session.acknowledgeSave(bound.session.requestSave()))
        assertSame(host, bound.hostServices)

        bound.close()
        assertFalse(session.isActive)
        assertTrue(session.isDisposed)
    }

    @Test
    fun rejectsUnknownPanelTypesAndMissingPlugins() {
        val coordinator = WorkspaceShellPluginHostCoordinator()

        try {
            coordinator.openEditor("KEYBOARD", sampleInput())
            throw AssertionError("Expected unknown panel type failure")
        } catch (error: IllegalStateException) {
            assertTrue(error.message!!.contains("KEYBOARD"))
        }

        try {
            coordinator.openEditor("FLOWCHART", sampleInput())
            throw AssertionError("Expected missing plugin failure")
        } catch (error: IllegalStateException) {
            assertTrue(error.message!!.contains("flowchart"))
        }
    }

    @Test
    fun rejectsCrossEditorSourcesAndUnsupportedFormats() {
        val plugin = FakeShellEditorPlugin(ShellPluginId("blockeditor"))
        val coordinator = WorkspaceShellPluginHostCoordinator(
            pluginLookup = { id -> if (id == plugin.pluginId) plugin else null }
        )

        try {
            coordinator.openEditor("BLOCKEDITOR", sampleInput(), workflowSourceId = "blockeditor-workspace")
            throw AssertionError("Expected workflow source failure")
        } catch (_: IllegalArgumentException) {
        }

        try {
            coordinator.openEditor("BLOCKEDITOR", sampleInput(formatId = "text/plain"))
            throw AssertionError("Expected unsupported format failure")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("text/plain"))
        }
    }

    @Test
    fun hostAdapterRecordsPluginEventsWithoutWorkflowWrites() {
        val host = RecordingShellPluginHostAdapter()
        val sessionId = ShellPluginSessionId("session-1")
        val documentId = ShellDocumentId("workflow-1")

        host.reportDirtyState(sessionId, ShellDirtyState.DIRTY)
        host.requestSave(ShellSaveRequest(sessionId, documentId))
        host.publishOutput(
            ShellEditorOutput(
                sessionId = sessionId,
                documentId = documentId,
                formatId = "application/vnd.visualtasker.blockeditor+json",
                content = "{}",
                disposition = ShellEditorOutputDisposition.DRAFT_EXPORT
            )
        )
        host.reportDiagnostics(sessionId, ShellValidationResult(emptyList()))
        host.reportRuntimeState(
            sessionId,
            ShellPluginRuntimeState(
                status = "RUNNING_WITH_GUARDS",
                phase = ShellRuntimePhase.RUNNING_LIVE,
                eventCount = 12,
                warningCount = 1,
                activeCommand = "click",
                requiredCapabilities = listOf(
                    ShellCapabilityRequirement(
                        capabilityId = "a11y",
                        label = "Accessibility",
                        status = ShellCapabilityStatus.AVAILABLE,
                        pluginOwner = "visualtasker.core",
                    )
                ),
            ),
        )
        val toolbarAction = ShellToolbarAction(
            id = ShellToolbarActionId("save"),
            label = "Save",
            iconName = "Save"
        )
        host.reportToolbarActions(sessionId, listOf(toolbarAction))

        assertEquals(listOf(sessionId to ShellDirtyState.DIRTY), host.recordedDirtyStates())
        assertEquals(1, host.recordedSaveRequests().size)
        assertEquals(ShellEditorOutputDisposition.DRAFT_EXPORT, host.recordedOutputs().single().disposition)
        assertTrue(host.recordedDiagnostics().single().second.isValid)
        val runtimeState = host.recordedRuntimeStates().single().second
        assertEquals("RUNNING_WITH_GUARDS", runtimeState.status)
        assertEquals(ShellRuntimePhase.RUNNING_LIVE, runtimeState.phase)
        assertEquals(12, runtimeState.eventCount)
        assertEquals(1, runtimeState.warningCount)
        assertEquals("click", runtimeState.activeCommand)
        assertEquals("a11y", runtimeState.requiredCapabilities.single().capabilityId)
        assertEquals(listOf(sessionId to listOf(toolbarAction)), host.recordedToolbarActions())
    }

    @Test
    fun panelSessionsExposeStableToolbarAndStatusDefaults() {
        val session = FakeShellEditorSession(ShellPluginSessionId("session-1"))

        assertTrue(session.toolbarActions().isEmpty())
        assertFalse(
            session.performToolbarAction(
                ShellToolbarActionRequest(
                    sessionId = session.sessionId,
                    actionId = ShellToolbarActionId("unknown")
                )
            )
        )
        assertEquals("session-1", session.status().title)
    }

    @Test
    fun runtimeStateKeepsBackwardsCompatibleDefaults() {
        val idle = ShellPluginRuntimeState("IDLE")
        val blocked = ShellPluginRuntimeState("BLOCKED", blocked = true)

        assertEquals(ShellRuntimePhase.IDLE, idle.phase)
        assertEquals(ShellRuntimeSeverity.INFO, idle.severity)
        assertEquals(ShellRuntimePhase.BLOCKED, blocked.phase)
        assertEquals(ShellRuntimeSeverity.ERROR, blocked.severity)
    }

    private fun sampleInput(
        formatId: String = "application/vnd.visualtasker.blockeditor+json",
        content: String = "{}"
    ): ShellEditorInput =
        ShellEditorInput(
            sessionId = ShellPluginSessionId("session-1"),
            documentId = ShellDocumentId("workflow-1"),
            formatId = formatId,
            revision = "rev-1",
            content = content
        )

    private class FakeShellEditorPlugin(
        override val pluginId: ShellPluginId
    ) : ShellEditorPlugin {
        override val panelId: ShellPanelId = ShellPanelId("${pluginId.value}-panel")
        override val supportedFormatIds: Set<String> = setOf("application/vnd.visualtasker.blockeditor+json")

        override fun createEditorSession(
            input: ShellEditorInput,
            hostServices: ShellPluginHostServices
        ): FakeShellEditorSession =
            FakeShellEditorSession(input.sessionId).also { it.open(input) }
    }

    private class FakeShellEditorSession(
        override val sessionId: ShellPluginSessionId
    ) : ShellEditorSession {
        override var documentId: ShellDocumentId = ShellDocumentId("unopened")
            private set
        override var dirtyState: ShellDirtyState = ShellDirtyState.CLEAN
            private set
        override val closeState: ShellEditorCloseState
            get() = if (dirtyState == ShellDirtyState.CLEAN) {
                ShellEditorCloseState.CAN_CLOSE
            } else {
                ShellEditorCloseState.UNSAVED_CHANGES
            }
        var isActive: Boolean = false
            private set
        var isDisposed: Boolean = false
            private set
        private var input: ShellEditorInput? = null

        override fun open(input: ShellEditorInput) {
            this.input = input
            documentId = input.documentId
            dirtyState = ShellDirtyState.CLEAN
        }

        override fun requestSave(): ShellEditorOutput {
            val opened = requireNotNull(input)
            return ShellEditorOutput(
                sessionId = sessionId,
                documentId = documentId,
                formatId = opened.formatId,
                content = opened.content,
                disposition = ShellEditorOutputDisposition.DOCUMENT_SAVE
            )
        }

        override fun acknowledgeSave(persistedOutput: ShellEditorOutput): ShellSaveAcknowledgmentResult =
            if (persistedOutput.content == input?.content) {
                ShellSaveAcknowledgmentResult.APPLIED
            } else {
                ShellSaveAcknowledgmentResult.STALE
            }

        override fun validate(): ShellValidationResult = ShellValidationResult(emptyList())

        override fun onActivated() {
            isActive = true
        }

        override fun onDeactivated() {
            isActive = false
        }

        override fun dispose() {
            isDisposed = true
        }
    }
}
