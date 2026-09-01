package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceGraph
import de.visualtasker.blockeditor.domain.WorkspacePoint
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.asFactory
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowGraphDocument
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

fun disconnectFlowchartEdgeFromWorkspace(
    document: WorkspaceDocument,
    graph: FlowGraphDocument,
    edgeId: FlowEdgeId,
): WorkspaceDocument {
    val edge = graph.edges.firstOrNull { it.id == edgeId } ?: return document
    val sourceBlockId = edge.sourceNodeId.toWorkspaceBlockId() ?: return document
    val targetBlockId = edge.targetNodeId.toWorkspaceBlockId() ?: return document
    val sourceBlock = document.blocks[sourceBlockId] ?: return document
    val targetBlock = document.blocks[targetBlockId] ?: return document

    val connectionId = when (edge.kind) {
        FlowEdgeKind.SEQUENCE,
        FlowEdgeKind.LOOP_EXIT -> sourceBlock.next?.id
        FlowEdgeKind.TRUE_BRANCH,
        FlowEdgeKind.FALSE_BRANCH,
        FlowEdgeKind.ELSE_IF_BRANCH,
        FlowEdgeKind.LOOP_BODY -> sourceBlock.statementInputs
            .firstOrNull { input ->
                input.name == edge.label ||
                    input.connection.connectedTo == targetBlock.previous?.id
            }
            ?.connection
            ?.id
        FlowEdgeKind.DATA_FLOW,
        FlowEdgeKind.CONDITION -> targetBlock.valueInputs
            .firstOrNull { input ->
                input.name == edge.label ||
                    input.connection.connectedTo == sourceBlock.output?.id
            }
            ?.connection
            ?.id
        else -> null
    } ?: return document

    val (_, connection) = WorkspaceGraph.findConnection(document, connectionId) ?: return document
    if (connection.connectedTo == null) return document
    val disconnected = WorkspaceReducer.reduce(document, WorkspaceAction.Disconnect(connectionId))
    val promotedRoots = when (edge.kind) {
        FlowEdgeKind.SEQUENCE,
        FlowEdgeKind.LOOP_EXIT,
        FlowEdgeKind.TRUE_BRANCH,
        FlowEdgeKind.FALSE_BRANCH,
        FlowEdgeKind.ELSE_IF_BRANCH,
        FlowEdgeKind.LOOP_BODY -> listOf(targetBlockId)
        FlowEdgeKind.DATA_FLOW,
        FlowEdgeKind.CONDITION -> listOf(sourceBlockId)
        else -> emptyList()
    }.filter { it in disconnected.blocks }
    if (promotedRoots.isEmpty()) return disconnected
    return disconnected.copy(
        rootBlocks = WorkspaceGraph.pruneRootBlocks(
            disconnected,
            disconnected.rootBlocks + promotedRoots,
        ),
    )
}

fun connectFlowchartNodesInWorkspace(
    document: WorkspaceDocument,
    sourceNodeId: FlowNodeId,
    targetNodeId: FlowNodeId,
    kind: FlowEdgeKind = FlowEdgeKind.SEQUENCE,
    label: String? = null,
): WorkspaceDocument {
    val sourceBlockId = sourceNodeId.toWorkspaceBlockId() ?: return document
    val targetBlockId = targetNodeId.toWorkspaceBlockId() ?: return document
    val sourceBlock = document.blocks[sourceBlockId] ?: return document
    val targetBlock = document.blocks[targetBlockId] ?: return document
    if (sourceBlockId == targetBlockId) return document

    val action = when (kind) {
        FlowEdgeKind.SEQUENCE,
        FlowEdgeKind.LOOP_EXIT -> {
            val source = sourceBlock.next?.id ?: return document
            val target = targetBlock.previous?.id ?: return document
            WorkspaceAction.Connect(source, target)
        }
        FlowEdgeKind.TRUE_BRANCH,
        FlowEdgeKind.FALSE_BRANCH,
        FlowEdgeKind.ELSE_IF_BRANCH,
        FlowEdgeKind.LOOP_BODY -> {
            val slotName = label ?: defaultStatementSlotName(sourceBlock.type, kind) ?: return document
            val source = sourceBlock.statementInputs.firstOrNull { it.name == slotName }?.connection?.id
                ?: return document
            val target = targetBlock.previous?.id ?: return document
            WorkspaceAction.Connect(source, target)
        }
        FlowEdgeKind.DATA_FLOW,
        FlowEdgeKind.CONDITION -> {
            val inputName = label ?: defaultValueInputName(targetBlock.type, kind) ?: return document
            val source = sourceBlock.output?.id ?: return document
            val target = targetBlock.valueInputs.firstOrNull { it.name == inputName }?.connection?.id
                ?: return document
            WorkspaceAction.Connect(source, target)
        }
        else -> return document
    }

    return WorkspaceReducer.reduce(document, action)
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

private fun defaultStatementSlotName(sourceBlockType: String, kind: FlowEdgeKind): String? =
    when (kind) {
        FlowEdgeKind.TRUE_BRANCH -> BlockTypes.SLOT_THEN
        FlowEdgeKind.FALSE_BRANCH -> BlockTypes.SLOT_ELSE
        FlowEdgeKind.ELSE_IF_BRANCH -> BlockTypes.SLOT_ELIF
        FlowEdgeKind.LOOP_BODY -> when (sourceBlockType) {
            BlockTypes.CONTROL_REPEAT -> BlockTypes.SLOT_DO
            BlockTypes.CONTROL_WHILE -> BlockTypes.SLOT_BODY
            else -> null
        }
        else -> null
    }

private fun defaultValueInputName(targetBlockType: String, kind: FlowEdgeKind): String? =
    when (kind) {
        FlowEdgeKind.CONDITION -> "CONDITION"
        FlowEdgeKind.DATA_FLOW -> when (targetBlockType) {
            BlockTypes.LOGIC_COMPARE -> "LEFT"
            BlockTypes.LOGIC_AND,
            BlockTypes.LOGIC_OR -> "A"
            BlockTypes.LOGIC_OPERATE -> "Input1"
            else -> null
        }
        else -> null
    }

private fun WorkspacePoint.closeTo(x: Float, y: Float): Boolean =
    abs(this.x - x) < 0.5f && abs(this.y - y) < 0.5f
