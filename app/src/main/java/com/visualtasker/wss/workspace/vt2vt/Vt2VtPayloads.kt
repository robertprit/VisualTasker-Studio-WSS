package com.visualtasker.wss.workspace.vt2vt

import com.visualtasker.wss.logging.StudioLogEntry
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot

fun workspacePayload(workflowState: WorkspaceWorkflowState): Map<String, String> =
    mapOf(
        "revision" to workflowState.revision.toString(),
        "mutationSource" to workflowState.mutationSource,
        "blocks" to workflowState.document.blocks.size.toString(),
        "flowNodes" to workflowState.flowchartProjection.graph.nodes.size.toString(),
        "flowEdges" to workflowState.flowchartProjection.graph.edges.size.toString()
    )

fun selectionPayload(
    focusedNodeId: FlowNodeId?,
    focusedEdgeId: FlowEdgeId?
): Map<String, String> =
    mapOf(
        "node" to (focusedNodeId?.value ?: "-"),
        "edge" to (focusedEdgeId?.value ?: "-")
    )

fun runtimePayload(
    workflowState: WorkspaceWorkflowState,
    snapshot: FlowRuntimeSnapshot?,
    activeRuntimeStepIndex: Int?,
    focusedNodeId: FlowNodeId?,
    focusedEdgeId: FlowEdgeId?
): Map<String, String> =
    workspacePayload(workflowState) + selectionPayload(focusedNodeId, focusedEdgeId) + mapOf(
        "runId" to (snapshot?.runId?.value ?: "-"),
        "sourceSessionId" to (snapshot?.sourceSessionId?.value ?: "-"),
        "runtimeSequence" to (snapshot?.sequence?.toString() ?: "-"),
        "activeStep" to (activeRuntimeStepIndex?.toString() ?: "-"),
        "activeNode" to (snapshot?.activeNodeId?.value ?: focusedNodeId?.value ?: "-"),
        "traversedEdges" to (snapshot?.traversedEdgeIds?.size?.toString() ?: "0"),
        "diagnostics" to (snapshot?.diagnostics?.size?.toString() ?: "0"),
        "succeeded" to snapshot.countState(FlowRuntimeNodeState.SUCCEEDED).toString(),
        "failed" to snapshot.countState(FlowRuntimeNodeState.FAILED).toString(),
        "skipped" to snapshot.countState(FlowRuntimeNodeState.SKIPPED).toString()
    )

fun logPayload(entry: StudioLogEntry?): Map<String, String> =
    if (entry == null) {
        mapOf("status" to "no-log-entry")
    } else {
        mapOf(
            "id" to entry.id,
            "timestamp" to entry.timestamp.toString(),
            "level" to entry.level.name,
            "source" to entry.source,
            "message" to entry.message,
            "details" to entry.details.orEmpty(),
            "documentRevision" to (entry.documentRevision?.toString() ?: "-"),
            "repeatCount" to entry.repeatCount.toString()
        )
    }

private fun FlowRuntimeSnapshot?.countState(state: FlowRuntimeNodeState): Int =
    this?.nodeStates?.values?.count { it == state } ?: 0
