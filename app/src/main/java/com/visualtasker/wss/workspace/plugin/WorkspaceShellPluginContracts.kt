package com.visualtasker.wss.workspace.plugin

/**
 * Shell-side mirror of the Studio plugin surface.
 *
 * This package intentionally contains no Compose, Android persistence, Workflow
 * mutation, or Runtime execution. The Workspace Shell may frame panels; plugin
 * sessions own editor-local draft state.
 */
enum class WorkspaceShellHostKind {
    WORKSPACE_SHELL
}

enum class WorkflowViewSurface {
    EMSCRIPT,
    BLOCK_EDITOR,
    FLOWCHART,
    STEP_EDITOR
}

enum class ShellDirtyState {
    CLEAN,
    DIRTY
}

enum class ShellEditorCloseState {
    CAN_CLOSE,
    UNSAVED_CHANGES
}

enum class ShellEditorOutputDisposition {
    DOCUMENT_SAVE,
    DRAFT_EXPORT
}

enum class ShellSaveAcknowledgmentResult {
    APPLIED,
    STALE
}

enum class ShellToolbarActionPlacement {
    PANEL_RAIL,
    PANEL_HEADER,
    COMMAND_PALETTE
}

data class ShellPluginSessionId(val value: String) {
    init {
        require(value.isNotBlank() && value == value.trim()) {
            "ShellPluginSessionId must be nonblank and trimmed."
        }
    }
}

data class ShellDocumentId(val value: String) {
    init {
        require(value.isNotBlank() && value == value.trim()) {
            "ShellDocumentId must be nonblank and trimmed."
        }
    }
}

data class ShellPluginId(val value: String) {
    init {
        require(value.isNotBlank() && value == value.trim()) {
            "ShellPluginId must be nonblank and trimmed."
        }
    }
}

data class ShellPanelId(val value: String) {
    init {
        require(value.isNotBlank() && value == value.trim()) {
            "ShellPanelId must be nonblank and trimmed."
        }
    }
}

data class ShellEditorInput(
    val sessionId: ShellPluginSessionId,
    val documentId: ShellDocumentId,
    val formatId: String,
    val revision: String?,
    val content: String
) {
    init {
        require(formatId.isNotBlank() && formatId == formatId.trim()) {
            "ShellEditorInput formatId must be nonblank and trimmed."
        }
    }
}

data class ShellEditorOutput(
    val sessionId: ShellPluginSessionId,
    val documentId: ShellDocumentId,
    val formatId: String,
    val content: String,
    val disposition: ShellEditorOutputDisposition
) {
    init {
        require(formatId.isNotBlank() && formatId == formatId.trim()) {
            "ShellEditorOutput formatId must be nonblank and trimmed."
        }
    }
}

data class ShellSaveRequest(
    val sessionId: ShellPluginSessionId,
    val documentId: ShellDocumentId
)

data class ShellValidationResult(
    val messages: List<String>
) {
    val isValid: Boolean = messages.isEmpty()
}

data class ShellPluginRuntimeState(
    val status: String,
    val blocked: Boolean = false
) {
    init {
        require(status.isNotBlank() && status == status.trim()) {
            "ShellPluginRuntimeState status must be nonblank and trimmed."
        }
    }
}

data class ShellToolbarActionId(val value: String) {
    init {
        require(value.isNotBlank() && value == value.trim()) {
            "ShellToolbarActionId must be nonblank and trimmed."
        }
    }
}

data class ShellToolbarAction(
    val id: ShellToolbarActionId,
    val label: String,
    val iconName: String,
    val placement: ShellToolbarActionPlacement = ShellToolbarActionPlacement.PANEL_RAIL,
    val enabled: Boolean = true
) {
    init {
        require(label.isNotBlank() && label == label.trim()) {
            "ShellToolbarAction label must be nonblank and trimmed."
        }
        require(iconName.isNotBlank() && iconName == iconName.trim()) {
            "ShellToolbarAction iconName must be nonblank and trimmed."
        }
    }
}

data class ShellToolbarActionRequest(
    val sessionId: ShellPluginSessionId,
    val actionId: ShellToolbarActionId
)

data class ShellPanelStatus(
    val title: String,
    val dirtyState: ShellDirtyState = ShellDirtyState.CLEAN,
    val validation: ShellValidationResult = ShellValidationResult(emptyList()),
    val runtimeState: ShellPluginRuntimeState? = null
) {
    init {
        require(title.isNotBlank() && title == title.trim()) {
            "ShellPanelStatus title must be nonblank and trimmed."
        }
    }
}

interface ShellPanelSession {
    val sessionId: ShellPluginSessionId

    fun onActivated()
    fun onDeactivated()
    fun dispose()

    fun toolbarActions(): List<ShellToolbarAction> = emptyList()
    fun performToolbarAction(request: ShellToolbarActionRequest): Boolean = false
    fun status(): ShellPanelStatus = ShellPanelStatus(title = sessionId.value)
}

interface ShellEditorSession : ShellPanelSession {
    val documentId: ShellDocumentId
    val dirtyState: ShellDirtyState
    val closeState: ShellEditorCloseState

    fun open(input: ShellEditorInput)
    fun requestSave(): ShellEditorOutput
    fun acknowledgeSave(persistedOutput: ShellEditorOutput): ShellSaveAcknowledgmentResult
    fun validate(): ShellValidationResult
}

interface ShellEditorPlugin {
    val pluginId: ShellPluginId
    val panelId: ShellPanelId
    val supportedFormatIds: Set<String>

    fun createEditorSession(
        input: ShellEditorInput,
        hostServices: ShellPluginHostServices
    ): ShellEditorSession
}

interface ShellPluginHostServices {
    fun reportDirtyState(sessionId: ShellPluginSessionId, dirtyState: ShellDirtyState)
    fun requestSave(request: ShellSaveRequest)
    fun publishOutput(output: ShellEditorOutput)
    fun reportDiagnostics(sessionId: ShellPluginSessionId, result: ShellValidationResult)

    fun reportToolbarActions(
        sessionId: ShellPluginSessionId,
        actions: List<ShellToolbarAction>
    ) {
        // Hosts that do not expose plugin toolbars ignore the event.
    }

    fun reportRuntimeState(
        sessionId: ShellPluginSessionId,
        state: ShellPluginRuntimeState
    ) {
        // Hosts that do not display runtime status ignore the event.
    }
}

class RecordingShellPluginHostAdapter(
    val hostKind: WorkspaceShellHostKind = WorkspaceShellHostKind.WORKSPACE_SHELL
) : ShellPluginHostServices {
    private val dirtyStates = mutableListOf<Pair<ShellPluginSessionId, ShellDirtyState>>()
    private val saveRequests = mutableListOf<ShellSaveRequest>()
    private val outputs = mutableListOf<ShellEditorOutput>()
    private val diagnostics = mutableListOf<Pair<ShellPluginSessionId, ShellValidationResult>>()
    private val runtimeStates = mutableListOf<Pair<ShellPluginSessionId, ShellPluginRuntimeState>>()
    private val toolbarActions = mutableListOf<Pair<ShellPluginSessionId, List<ShellToolbarAction>>>()

    override fun reportDirtyState(sessionId: ShellPluginSessionId, dirtyState: ShellDirtyState) {
        dirtyStates += sessionId to dirtyState
    }

    override fun requestSave(request: ShellSaveRequest) {
        saveRequests += request
    }

    override fun publishOutput(output: ShellEditorOutput) {
        outputs += output
    }

    override fun reportDiagnostics(sessionId: ShellPluginSessionId, result: ShellValidationResult) {
        diagnostics += sessionId to result
    }

    override fun reportToolbarActions(
        sessionId: ShellPluginSessionId,
        actions: List<ShellToolbarAction>
    ) {
        toolbarActions += sessionId to actions
    }

    override fun reportRuntimeState(sessionId: ShellPluginSessionId, state: ShellPluginRuntimeState) {
        runtimeStates += sessionId to state
    }

    fun recordedDirtyStates(): List<Pair<ShellPluginSessionId, ShellDirtyState>> = dirtyStates.toList()
    fun recordedSaveRequests(): List<ShellSaveRequest> = saveRequests.toList()
    fun recordedOutputs(): List<ShellEditorOutput> = outputs.toList()
    fun recordedDiagnostics(): List<Pair<ShellPluginSessionId, ShellValidationResult>> = diagnostics.toList()
    fun recordedRuntimeStates(): List<Pair<ShellPluginSessionId, ShellPluginRuntimeState>> = runtimeStates.toList()
    fun recordedToolbarActions(): List<Pair<ShellPluginSessionId, List<ShellToolbarAction>>> = toolbarActions.toList()
}
