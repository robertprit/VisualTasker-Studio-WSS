package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspacePoint
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.asFactory
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowViewDocument
import kotlin.math.abs

const val WORKFLOW_SOURCE_FLOWCHART_PREFIX = "flowchart:"
private const val FLOW_BLOCK_NODE_PREFIX = "block:"

fun addFlowchartNodeToWorkspace(
    document: WorkspaceDocument,
    definitionId: String,
): WorkspaceDocument {
    val x = 96f
    val y = 120f + (document.rootBlocks.size * 32f)
    return WorkspaceReducer.reduce(
        document,
        WorkspaceAction.InstantiateBlock(definitionId, x, y),
        DefaultBlockRegistry.asFactory(),
    )
}

fun deleteFlowchartNodeFromWorkspace(
    document: WorkspaceDocument,
    nodeId: FlowNodeId,
): WorkspaceDocument {
    val blockId = nodeId.toWorkspaceBlockId() ?: return document
    if (blockId !in document.blocks) return document
    return WorkspaceReducer.reduce(document, WorkspaceAction.DeleteBlock(blockId))
}

fun syncRootPositionsFromFlowchartView(
    document: WorkspaceDocument,
    viewDocument: FlowViewDocument,
): WorkspaceDocument {
    var updated = document
    viewDocument.nodeViews.forEach { nodeView ->
        val blockId = nodeView.nodeId.toWorkspaceBlockId() ?: return@forEach
        if (blockId !in updated.rootBlocks || blockId !in updated.blocks) return@forEach
        val current = updated.rootPositions[blockId]
        val nextX = nodeView.position.x.toFloat()
        val nextY = nodeView.position.y.toFloat()
        if (current != null && current.closeTo(nextX, nextY)) return@forEach
        updated = WorkspaceReducer.reduce(
            updated,
            WorkspaceAction.MoveRoot(blockId, nextX, nextY),
        )
    }
    return updated
}

fun FlowNodeId.toWorkspaceBlockId(): de.visualtasker.blockeditor.domain.BlockId? {
    val value = value.removePrefix(FLOW_BLOCK_NODE_PREFIX)
    if (value == this.value || value.isBlank()) return null
    return de.visualtasker.blockeditor.domain.BlockId(value)
}

private fun WorkspacePoint.closeTo(x: Float, y: Float): Boolean =
    abs(this.x - x) < 0.5f && abs(this.y - y) < 0.5f
