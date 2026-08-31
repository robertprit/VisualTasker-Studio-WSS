package com.visualtasker.wss.workspace.plugin.blockeditor

import com.visualtasker.wss.workspace.plugin.ShellDirtyState
import com.visualtasker.wss.workspace.plugin.ShellDocumentId
import com.visualtasker.wss.workspace.plugin.ShellEditorCloseState
import com.visualtasker.wss.workspace.plugin.ShellEditorInput
import com.visualtasker.wss.workspace.plugin.ShellEditorOutput
import com.visualtasker.wss.workspace.plugin.ShellEditorOutputDisposition
import com.visualtasker.wss.workspace.plugin.ShellEditorPlugin
import com.visualtasker.wss.workspace.plugin.ShellEditorSession
import com.visualtasker.wss.workspace.plugin.ShellPanelId
import com.visualtasker.wss.workspace.plugin.ShellPluginHostServices
import com.visualtasker.wss.workspace.plugin.ShellPluginId
import com.visualtasker.wss.workspace.plugin.ShellPluginRuntimeState
import com.visualtasker.wss.workspace.plugin.ShellPluginSessionId
import com.visualtasker.wss.workspace.plugin.ShellSaveAcknowledgmentResult
import com.visualtasker.wss.workspace.plugin.ShellSaveRequest
import com.visualtasker.wss.workspace.plugin.ShellValidationResult
import de.visualtasker.blockeditor.compose.host.BlockEditorController
import de.visualtasker.blockeditor.compose.host.BlockEditorHostCallbacks
import de.visualtasker.blockeditor.compose.host.BlockEditorRuntimeState
import de.visualtasker.blockeditor.compose.host.BlockEditorRuntimeStatus
import de.visualtasker.blockeditor.serialization.BlockEditorDocumentFormats
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.blockeditor.validation.ValidationError
import de.visualtasker.blockeditor.validation.Validator

class BlockEditorShellPlugin : ShellEditorPlugin {
    override val pluginId: ShellPluginId = ShellPluginId(PLUGIN_ID)
    override val panelId: ShellPanelId = ShellPanelId(PANEL_ID)
    override val supportedFormatIds: Set<String> = setOf(BlockEditorDocumentFormats.WORKSPACE_JSON)

    override fun createEditorSession(
        input: ShellEditorInput,
        hostServices: ShellPluginHostServices
    ): ShellEditorSession = BlockEditorShellEditorSession(input, hostServices)

    companion object {
        const val PLUGIN_ID = "blockeditor"
        const val PANEL_ID = "blockeditor-panel"
    }
}

class BlockEditorShellEditorSession(
    input: ShellEditorInput,
    private val hostServices: ShellPluginHostServices
) : ShellEditorSession {
    override val sessionId: ShellPluginSessionId = input.sessionId
    override var documentId: ShellDocumentId = input.documentId
        private set
    override var dirtyState: ShellDirtyState = ShellDirtyState.CLEAN
        private set
    override val closeState: ShellEditorCloseState
        get() = if (dirtyState == ShellDirtyState.CLEAN) {
            ShellEditorCloseState.CAN_CLOSE
        } else {
            ShellEditorCloseState.UNSAVED_CHANGES
        }

    lateinit var controller: BlockEditorController
        private set

    private var formatId: String = input.formatId
    private var persistedContent: String = ""
    private var active: Boolean = false
    private var disposed: Boolean = false

    init {
        open(input)
    }

    override fun open(input: ShellEditorInput) {
        check(!disposed) { "Blockeditor session is already disposed." }
        require(input.sessionId == sessionId) {
            "Blockeditor session cannot be reopened with another session id."
        }
        require(input.formatId == BlockEditorDocumentFormats.WORKSPACE_JSON) {
            "Blockeditor supports only ${BlockEditorDocumentFormats.WORKSPACE_JSON}."
        }
        if (::controller.isInitialized) {
            controller.close()
        }
        documentId = input.documentId
        formatId = input.formatId
        val document = WorkspaceSerializer.deserialize(input.content)
        persistedContent = WorkspaceSerializer.serialize(document)
        dirtyState = ShellDirtyState.CLEAN
        hostServices.reportDirtyState(sessionId, dirtyState)
        controller = BlockEditorController(
            initialDocument = document,
            callbacks = callbacks()
        )
    }

    override fun requestSave(): ShellEditorOutput {
        hostServices.requestSave(ShellSaveRequest(sessionId, documentId))
        return ShellEditorOutput(
            sessionId = sessionId,
            documentId = documentId,
            formatId = formatId,
            content = currentSerializedDocument(),
            disposition = ShellEditorOutputDisposition.DOCUMENT_SAVE
        )
    }

    override fun acknowledgeSave(persistedOutput: ShellEditorOutput): ShellSaveAcknowledgmentResult {
        if (
            persistedOutput.sessionId != sessionId ||
            persistedOutput.documentId != documentId ||
            persistedOutput.formatId != formatId ||
            persistedOutput.content != currentSerializedDocument()
        ) {
            return ShellSaveAcknowledgmentResult.STALE
        }
        persistedContent = persistedOutput.content
        updateDirtyState(ShellDirtyState.CLEAN)
        return ShellSaveAcknowledgmentResult.APPLIED
    }

    override fun validate(): ShellValidationResult {
        val errors = Validator.validate(controller.document, controller.registry).errors
        val result = ShellValidationResult(errors.map(ValidationError::message))
        hostServices.reportDiagnostics(sessionId, result)
        return result
    }

    override fun onActivated() {
        active = true
    }

    override fun onDeactivated() {
        active = false
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        active = false
        controller.close()
    }

    fun isActive(): Boolean = active

    fun isDisposed(): Boolean = disposed

    private fun callbacks(): BlockEditorHostCallbacks =
        object : BlockEditorHostCallbacks {
            override fun onWorkspaceDocumentChanged(serializedJson: String) {
                val normalized = normalize(serializedJson)
                val nextDirtyState = if (normalized == persistedContent) {
                    ShellDirtyState.CLEAN
                } else {
                    ShellDirtyState.DIRTY
                }
                updateDirtyState(nextDirtyState)
            }

            override fun onEmscriptDraftChanged(emscript: String) {
                hostServices.publishOutput(
                    ShellEditorOutput(
                        sessionId = sessionId,
                        documentId = documentId,
                        formatId = BlockEditorDocumentFormats.EMSCRIPT,
                        content = emscript,
                        disposition = ShellEditorOutputDisposition.DRAFT_EXPORT
                    )
                )
            }

            override fun onValidationErrors(errors: List<ValidationError>) {
                hostServices.reportDiagnostics(
                    sessionId,
                    ShellValidationResult(errors.map(ValidationError::message))
                )
            }

            override fun onEmscriptGenerationFailed(message: String) {
                hostServices.reportDiagnostics(sessionId, ShellValidationResult(listOf(message)))
            }

            override fun onRuntimeStateChanged(state: BlockEditorRuntimeState) {
                hostServices.reportRuntimeState(
                    sessionId,
                    ShellPluginRuntimeState(
                        status = state.status.name,
                        blocked = state.status == BlockEditorRuntimeStatus.BLOCKED
                    )
                )
            }
        }

    private fun currentSerializedDocument(): String =
        WorkspaceSerializer.serialize(controller.document)

    private fun normalize(raw: String): String =
        WorkspaceSerializer.serialize(WorkspaceSerializer.deserialize(raw))

    private fun updateDirtyState(next: ShellDirtyState) {
        if (dirtyState == next) return
        dirtyState = next
        hostServices.reportDirtyState(sessionId, dirtyState)
    }
}
