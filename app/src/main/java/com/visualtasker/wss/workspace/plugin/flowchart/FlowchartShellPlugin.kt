package com.visualtasker.wss.workspace.plugin.flowchart

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
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowSurfaceId
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.interaction.FlowchartController
import de.visualtasker.flowchart.interaction.FlowchartStatus
import de.visualtasker.flowchart.interaction.FlowchartStatusCode
import de.visualtasker.flowchart.serialization.FlowDecodeResult
import de.visualtasker.flowchart.serialization.FlowGraphJsonCodec
import de.visualtasker.flowchart.serialization.FlowViewJsonCodec
import de.visualtasker.flowchart.validation.FlowGraphValidator

class FlowchartShellPlugin : ShellEditorPlugin {
    override val pluginId: ShellPluginId = ShellPluginId(PLUGIN_ID)
    override val panelId: ShellPanelId = ShellPanelId(PANEL_ID)
    override val supportedFormatIds: Set<String> = setOf(FLOW_GRAPH_JSON)

    override fun createEditorSession(
        input: ShellEditorInput,
        hostServices: ShellPluginHostServices
    ): ShellEditorSession = FlowchartShellEditorSession(input, hostServices)

    companion object {
        const val PLUGIN_ID = "flowchart"
        const val PANEL_ID = "flowchart-panel"
        const val FLOW_GRAPH_JSON = "application/vnd.visualtasker.flowchart.graph+json"
        const val FLOW_VIEW_JSON = "application/vnd.visualtasker.flowchart.view+json"
    }
}

class FlowchartShellEditorSession(
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

    val controller: FlowchartController = FlowchartController(FlowSurfaceId(input.sessionId.value))
    lateinit var graphDocument: FlowGraphDocument
        private set
    var viewDocument: FlowViewDocument? = null
        private set

    private var persistedViewContent: String? = null
    private var active: Boolean = false
    private var disposed: Boolean = false

    init {
        open(input)
    }

    override fun open(input: ShellEditorInput) {
        check(!disposed) { "Flowchart session is already disposed." }
        require(input.sessionId == sessionId) {
            "Flowchart session cannot be reopened with another session id."
        }
        require(input.formatId == FlowchartShellPlugin.FLOW_GRAPH_JSON) {
            "Flowchart supports only ${FlowchartShellPlugin.FLOW_GRAPH_JSON}."
        }
        documentId = input.documentId
        graphDocument = decodeGraph(input.content)
        viewDocument = null
        persistedViewContent = null
        dirtyState = ShellDirtyState.CLEAN
        hostServices.reportDirtyState(sessionId, dirtyState)
        reportValidation()
        reportStatus(controller.attachGraph(graphDocument))
    }

    fun replaceGraphContent(content: String) {
        check(!disposed) { "Flowchart session is already disposed." }
        val nextGraph = decodeGraph(content)
        if (::graphDocument.isInitialized && graphDocument == nextGraph) return
        graphDocument = nextGraph
        dirtyState = ShellDirtyState.CLEAN
        hostServices.reportDirtyState(sessionId, dirtyState)
        reportValidation()
        reportStatus(controller.attachGraph(graphDocument, viewDocument))
    }

    fun onViewDocumentChanged(view: FlowViewDocument) {
        viewDocument = view
        val nextDirty = if (persistedViewContent == encodeView(view)) {
            ShellDirtyState.CLEAN
        } else {
            ShellDirtyState.DIRTY
        }
        updateDirtyState(nextDirty)
    }

    fun onStatusMessage(status: FlowchartStatus) {
        reportStatus(status)
    }

    override fun requestSave(): ShellEditorOutput {
        hostServices.requestSave(ShellSaveRequest(sessionId, documentId))
        return ShellEditorOutput(
            sessionId = sessionId,
            documentId = documentId,
            formatId = FlowchartShellPlugin.FLOW_VIEW_JSON,
            content = currentViewContent(),
            disposition = ShellEditorOutputDisposition.DRAFT_EXPORT
        )
    }

    override fun acknowledgeSave(persistedOutput: ShellEditorOutput): ShellSaveAcknowledgmentResult {
        if (
            persistedOutput.sessionId != sessionId ||
            persistedOutput.documentId != documentId ||
            persistedOutput.formatId != FlowchartShellPlugin.FLOW_VIEW_JSON ||
            persistedOutput.content != currentViewContent()
        ) {
            return ShellSaveAcknowledgmentResult.STALE
        }
        persistedViewContent = persistedOutput.content
        updateDirtyState(ShellDirtyState.CLEAN)
        return ShellSaveAcknowledgmentResult.APPLIED
    }

    override fun validate(): ShellValidationResult =
        reportValidation()

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

    private fun reportValidation(): ShellValidationResult {
        val result = ShellValidationResult(
            FlowGraphValidator.validate(graphDocument).diagnostics.map { it.message }
        )
        hostServices.reportDiagnostics(sessionId, result)
        return result
    }

    private fun reportStatus(status: FlowchartStatus) {
        hostServices.reportRuntimeState(
            sessionId,
            ShellPluginRuntimeState(
                status = status.code.name,
                blocked = status.code in setOf(
                    FlowchartStatusCode.INVALID_GRAPH,
                    FlowchartStatusCode.RUNTIME_REJECTED,
                    FlowchartStatusCode.CLOSED
                )
            )
        )
        if (status.diagnostics.isNotEmpty()) {
            hostServices.reportDiagnostics(
                sessionId,
                ShellValidationResult(status.diagnostics.map { it.message })
            )
        }
    }

    private fun updateDirtyState(next: ShellDirtyState) {
        if (dirtyState == next) return
        dirtyState = next
        hostServices.reportDirtyState(sessionId, dirtyState)
    }

    private fun currentViewContent(): String {
        val view = viewDocument ?: controller.snapshot().view
        return view?.let(::encodeView).orEmpty()
    }

    private fun encodeView(view: FlowViewDocument): String =
        FlowViewJsonCodec(graphDocument).encodeCanonical(view)

    private fun decodeGraph(raw: String): FlowGraphDocument =
        when (val decoded = FlowGraphJsonCodec().decode(raw)) {
            is FlowDecodeResult.Success -> decoded.value
            is FlowDecodeResult.Malformed -> error("Malformed flowchart graph: ${decoded.message}")
            is FlowDecodeResult.UnsupportedSchema -> error("Unsupported flowchart graph schema: ${decoded.version}")
        }
}
