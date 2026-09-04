package com.visualtasker.wss.flowchart

import com.visualtasker.wss.emscript.runtime.EmscriptDryRunEvent
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import de.visualtasker.blockeditor.ir.IrGraph
import de.visualtasker.blockeditor.ir.IrGraphEdgeId
import de.visualtasker.blockeditor.ir.IrGraphEdgeKind
import de.visualtasker.blockeditor.ir.IrGraphNodeId
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState

data class IrGraphRuntimeSnapshot(
    val sequence: Long,
    val activeNodeId: IrGraphNodeId?,
    val nodeStates: Map<IrGraphNodeId, FlowRuntimeNodeState>,
    val traversedEdgeIds: List<IrGraphEdgeId>,
    val events: List<IrGraphRuntimeEvent>,
)

data class IrGraphRuntimeEvent(
    val index: Int,
    val kind: String,
    val message: String,
    val nodeId: IrGraphNodeId? = null,
    val edgeId: IrGraphEdgeId? = null,
    val edgeKind: IrGraphEdgeKind? = null,
    val severity: String = "INFO",
    val command: String? = null,
    val capability: String? = null,
    val pluginOwner: String? = null,
)

object EmscriptDryRunIrGraphRuntimeMapper {
    fun map(
        irGraph: IrGraph,
        result: EmscriptDryRunResult,
        sequence: Long,
        maxEventIndex: Int? = null,
    ): IrGraphRuntimeSnapshot {
        val sourceEvents = when (result) {
            is EmscriptDryRunResult.Success -> result.events
            is EmscriptDryRunResult.Failure -> result.events
        }.let { events ->
            maxEventIndex?.let { limit -> events.filter { it.index <= limit } } ?: events
        }
        val mappedEvents = sourceEvents.map { event -> event.toIrRuntimeEvent(irGraph) }
        val tracedNodeIds = mappedEvents.mapNotNull { it.nodeId }.toSet()
        val traversedEdges = mappedEvents.mapNotNull { it.edgeId }.distinct()
        val activeNodeId = mappedEvents.asReversed().firstNotNullOfOrNull { it.nodeId }
        val hasWorkspaceTrace = tracedNodeIds.isNotEmpty() || traversedEdges.isNotEmpty()
        val nodeStates = when (result) {
            is EmscriptDryRunResult.Success -> if (hasWorkspaceTrace) {
                irGraph.nodes.associate { node ->
                    node.id to if (node.id in tracedNodeIds) FlowRuntimeNodeState.SUCCEEDED else FlowRuntimeNodeState.SKIPPED
                }
            } else {
                emptyMap()
            }
            is EmscriptDryRunResult.Failure -> {
                val failedNodeId = activeNodeId
                if (hasWorkspaceTrace) {
                    irGraph.nodes.associate { node ->
                        node.id to when {
                            node.id == failedNodeId -> FlowRuntimeNodeState.FAILED
                            node.id in tracedNodeIds -> FlowRuntimeNodeState.SUCCEEDED
                            else -> FlowRuntimeNodeState.SKIPPED
                        }
                    }
                } else {
                    irGraph.entryNodeIds.firstOrNull()
                        ?.let { mapOf(it to FlowRuntimeNodeState.FAILED) }
                        .orEmpty()
                }
            }
        }
        return IrGraphRuntimeSnapshot(
            sequence = sequence,
            activeNodeId = activeNodeId,
            nodeStates = nodeStates,
            traversedEdgeIds = traversedEdges,
            events = mappedEvents,
        )
    }

    private fun EmscriptDryRunEvent.toIrRuntimeEvent(irGraph: IrGraph): IrGraphRuntimeEvent {
        val nodeId = blockId
            ?.let { IrGraphNodeId("block:$it") }
            ?.takeIf { candidate -> irGraph.nodes.any { it.id == candidate } }
        val edgeKind = edgeKind?.let { runCatching { IrGraphEdgeKind.valueOf(it) }.getOrNull() }
        val edgeId = edgeSourceBlockId?.let { sourceBlockId ->
            edgeTargetBlockId?.let { targetBlockId ->
                val source = IrGraphNodeId("block:$sourceBlockId")
                val target = IrGraphNodeId("block:$targetBlockId")
                irGraph.edges.firstOrNull { edge ->
                    edge.sourceNodeId == source &&
                        edge.targetNodeId == target &&
                        (edgeKind == null || edge.kind == edgeKind)
                }?.id
            }
        }
        return IrGraphRuntimeEvent(
            index = index,
            kind = kind,
            message = message,
            nodeId = nodeId,
            edgeId = edgeId,
            edgeKind = edgeKind,
            severity = severity.name,
            command = command,
            capability = capability,
            pluginOwner = pluginOwner,
        )
    }
}
