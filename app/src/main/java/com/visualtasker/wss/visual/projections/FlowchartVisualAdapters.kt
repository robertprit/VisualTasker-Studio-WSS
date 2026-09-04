package com.visualtasker.wss.visual.projections

import com.visualtasker.wss.visual.semantics.VisualActivity
import com.visualtasker.wss.visual.semantics.VisualAuthority
import com.visualtasker.wss.visual.semantics.VisualAvailability
import com.visualtasker.wss.visual.semantics.VisualCertainty
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualFocus
import com.visualtasker.wss.visual.semantics.VisualRole
import com.visualtasker.wss.visual.semantics.VisualSemanticAdapter
import com.visualtasker.wss.visual.semantics.VisualSemanticState
import com.visualtasker.wss.visual.semantics.VisualValidation
import de.visualtasker.flowchart.domain.FlowDiagnosticSeverity
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowGraphEdge
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot

data class FlowchartNodeVisualSubject(
    val node: FlowGraphNode,
    val graph: FlowGraphDocument,
    val runtimeSnapshot: FlowRuntimeSnapshot? = null,
    val selectedNodeId: FlowNodeId? = null,
)

object FlowchartNodeVisualAdapter : VisualSemanticAdapter<FlowchartNodeVisualSubject> {
    override fun map(value: FlowchartNodeVisualSubject, context: VisualContext): VisualSemanticState {
        val diagnostics = value.node.diagnosticIds.mapNotNull { diagnosticId ->
            value.graph.diagnostics.firstOrNull { it.id == diagnosticId }
        }
        val runtimeState = value.runtimeSnapshot?.nodeStates?.get(value.node.id)
        return VisualSemanticState(
            role = value.node.kind.standard.toVisualRole(),
            authority = if (value.node.kind.standard == FlowNodeKind.SYNTHETIC) {
                VisualAuthority.Derived
            } else {
                VisualAuthority.Canonical
            },
            certainty = if (value.node.kind.standard == FlowNodeKind.UNKNOWN_SOURCE) {
                VisualCertainty.Uncertain
            } else {
                VisualCertainty.Known
            },
            activity = runtimeState.toVisualActivity(),
            validation = diagnostics.toVisualValidation(),
            focus = if (value.selectedNodeId == value.node.id) VisualFocus.Selected else VisualFocus.None,
            availability = if (value.node.kind.standard == FlowNodeKind.UNKNOWN_SOURCE) {
                VisualAvailability.Unavailable
            } else {
                VisualAvailability.Enabled
            },
        )
    }
}

object FlowchartEdgeVisualAdapter : VisualSemanticAdapter<FlowGraphEdge> {
    override fun map(value: FlowGraphEdge, context: VisualContext): VisualSemanticState =
        VisualSemanticState(
            role = VisualRole.WorkflowConnector,
            authority = VisualAuthority.Canonical,
        )
}

private fun FlowNodeKind?.toVisualRole(): VisualRole =
    when (this) {
        FlowNodeKind.ENTRY,
        FlowNodeKind.EXIT,
        FlowNodeKind.FUNCTION_START,
        FlowNodeKind.FUNCTION_END -> VisualRole.WorkflowEvent
        FlowNodeKind.ACTION,
        FlowNodeKind.INPUT,
        FlowNodeKind.OUTPUT,
        FlowNodeKind.ASSIGNMENT,
        FlowNodeKind.PROPERTY_ACCESS,
        FlowNodeKind.FUNCTION_CALL -> VisualRole.WorkflowAction
        FlowNodeKind.DECISION,
        FlowNodeKind.ELSE_IF,
        FlowNodeKind.ELSE,
        FlowNodeKind.TRY_START,
        FlowNodeKind.CATCH,
        FlowNodeKind.TRY_END -> VisualRole.WorkflowCondition
        FlowNodeKind.LOOP_START,
        FlowNodeKind.LOOP_END -> VisualRole.WorkflowLoop
        FlowNodeKind.ANNOTATION,
        FlowNodeKind.SYNTHETIC -> VisualRole.Group
        FlowNodeKind.UNKNOWN_SOURCE,
        null -> VisualRole.Unknown
    }

private fun FlowRuntimeNodeState?.toVisualActivity(): VisualActivity =
    when (this) {
        FlowRuntimeNodeState.QUEUED -> VisualActivity.Queued
        FlowRuntimeNodeState.RUNNING -> VisualActivity.Running
        FlowRuntimeNodeState.WAITING -> VisualActivity.Waiting
        FlowRuntimeNodeState.SUCCEEDED -> VisualActivity.Succeeded
        FlowRuntimeNodeState.FAILED -> VisualActivity.Failed
        FlowRuntimeNodeState.SKIPPED -> VisualActivity.Skipped
        FlowRuntimeNodeState.CANCELLED -> VisualActivity.Cancelled
        FlowRuntimeNodeState.NOT_STARTED,
        null -> VisualActivity.Idle
    }

private fun List<de.visualtasker.flowchart.domain.FlowGraphDiagnostic>.toVisualValidation(): VisualValidation =
    when {
        any { it.severity == FlowDiagnosticSeverity.ERROR } -> VisualValidation.Invalid
        any { it.severity == FlowDiagnosticSeverity.WARNING } -> VisualValidation.Warning
        else -> VisualValidation.Valid
    }
