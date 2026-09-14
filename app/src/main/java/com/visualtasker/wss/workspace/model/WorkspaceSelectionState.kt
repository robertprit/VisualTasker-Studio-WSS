package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowNodeId

data class WorkspaceSelectionState(
    val blockId: BlockId? = null,
    val flowNodeId: FlowNodeId? = null,
    val flowEdgeId: FlowEdgeId? = null,
    val railStepId: String? = null,
    val sourceLine: Int? = null,
    val source: String = "initial",
) {
    fun selectBlock(
        blockId: BlockId,
        sourceLine: Int? = this.sourceLine,
        source: String = "blockeditor",
    ): WorkspaceSelectionState =
        copy(
            blockId = blockId,
            flowNodeId = blockId.toFlowNodeId(),
            flowEdgeId = null,
            sourceLine = sourceLine,
            source = source,
        )

    fun selectFlowNode(
        nodeId: FlowNodeId,
        sourceLine: Int? = this.sourceLine,
        source: String = "flowchart",
    ): WorkspaceSelectionState =
        copy(
            blockId = nodeId.toBlockIdOrNull(),
            flowNodeId = nodeId,
            flowEdgeId = null,
            sourceLine = sourceLine,
            source = source,
        )

    fun selectFlowEdge(edgeId: FlowEdgeId, source: String = "flowchart"): WorkspaceSelectionState =
        copy(
            blockId = null,
            flowNodeId = null,
            flowEdgeId = edgeId,
            sourceLine = null,
            source = source,
        )

    fun selectRailStep(
        stepId: String,
        sourceLine: Int? = this.sourceLine,
        source: String = "railtrace",
    ): WorkspaceSelectionState =
        copy(
            railStepId = stepId.takeIf { it.isNotBlank() },
            sourceLine = sourceLine,
            source = source,
        )

    fun clearVisualSelection(source: String = "workspace"): WorkspaceSelectionState =
        copy(
            blockId = null,
            flowNodeId = null,
            flowEdgeId = null,
            sourceLine = null,
            source = source,
        )
}

fun BlockId.toFlowNodeId(): FlowNodeId = FlowNodeId("block:$value")

fun FlowNodeId.toBlockIdOrNull(): BlockId? {
    val blockId = value.removePrefix("block:").takeIf { it != value && it.isNotBlank() } ?: return null
    return BlockId(blockId)
}
