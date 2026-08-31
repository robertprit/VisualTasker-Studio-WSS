package com.visualtasker.wss.workspace.bridge

import com.visualtasker.wss.workspace.model.PanelAction
import com.visualtasker.wss.workspace.model.PanelActionSink

sealed interface WorkflowDocumentAction {
    data class UpdateSelection(val stepId: String) : WorkflowDocumentAction
    data class ReorderSequence(val from: Int, val to: Int) : WorkflowDocumentAction
    data class RemoveStep(val stepId: String) : WorkflowDocumentAction
    data class RemoveNode(val nodeId: String) : WorkflowDocumentAction
}

interface WorkflowDocumentActionSink {
    fun onWorkflowDocumentAction(action: WorkflowDocumentAction)
}

enum class DeleteMappingMode {
    RemoveStep,
    RemoveNode
}

class PanelActionWorkflowAdapter(
    private val workflowSink: WorkflowDocumentActionSink,
    private val deleteMappingMode: DeleteMappingMode = DeleteMappingMode.RemoveStep,
    private val downstream: PanelActionSink? = null
) : PanelActionSink {
    override fun onPanelAction(action: PanelAction) {
        when (action) {
            is PanelAction.SelectStep ->
                workflowSink.onWorkflowDocumentAction(WorkflowDocumentAction.UpdateSelection(action.stepId))
            is PanelAction.ReorderStep ->
                workflowSink.onWorkflowDocumentAction(WorkflowDocumentAction.ReorderSequence(action.from, action.to))
            is PanelAction.DeleteStep -> when (deleteMappingMode) {
                DeleteMappingMode.RemoveStep ->
                    workflowSink.onWorkflowDocumentAction(WorkflowDocumentAction.RemoveStep(action.stepId))
                DeleteMappingMode.RemoveNode ->
                    workflowSink.onWorkflowDocumentAction(WorkflowDocumentAction.RemoveNode(action.stepId))
            }
            else -> Unit
        }
        downstream?.onPanelAction(action)
    }
}
