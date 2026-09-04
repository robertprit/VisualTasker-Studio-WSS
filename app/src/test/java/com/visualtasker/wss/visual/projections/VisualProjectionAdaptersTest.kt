package com.visualtasker.wss.visual.projections

import com.visualtasker.wss.visual.semantics.ProjectionKind
import com.visualtasker.wss.visual.semantics.VisualActivity
import com.visualtasker.wss.visual.semantics.VisualAuthority
import com.visualtasker.wss.visual.semantics.VisualCertainty
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualFocus
import com.visualtasker.wss.visual.semantics.VisualRole
import com.visualtasker.wss.visual.semantics.VisualValidation
import com.visualtasker.wss.workspace.model.AmbiguityResolutionState
import com.visualtasker.wss.workspace.model.AmbiguityType
import com.visualtasker.wss.workspace.model.KnowledgeState
import com.visualtasker.wss.workspace.model.WorldAmbiguity
import com.visualtasker.wss.workspace.model.WorldEntity
import com.visualtasker.wss.workspace.model.WorldEntityKind
import de.visualtasker.flowchart.domain.FlowDiagnosticId
import de.visualtasker.flowchart.domain.FlowDiagnosticSeverity
import de.visualtasker.flowchart.domain.FlowDocumentId
import de.visualtasker.flowchart.domain.FlowDocumentRevision
import de.visualtasker.flowchart.domain.FlowGraphDiagnostic
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowRunId
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowSemanticKind
import de.visualtasker.flowchart.domain.FlowSourceSessionId
import org.junit.Assert.assertEquals
import org.junit.Test

class VisualProjectionAdaptersTest {
    @Test
    fun mapsFlowchartRuntimeAndDiagnosticsToOrthogonalSemanticAxes() {
        val nodeId = FlowNodeId("block:if")
        val diagnosticId = FlowDiagnosticId("diag:if")
        val node = FlowGraphNode(
            id = nodeId,
            kind = FlowSemanticKind(standard = FlowNodeKind.DECISION),
            label = "if",
            diagnosticIds = listOf(diagnosticId),
        )
        val graph = FlowGraphDocument(
            documentId = FlowDocumentId("flow:test"),
            documentRevision = FlowDocumentRevision("1"),
            producerId = "test",
            producerVersion = "1",
            sourceRevision = "1",
            sourceHash = "hash",
            nodes = listOf(node),
            diagnostics = listOf(
                FlowGraphDiagnostic(
                    id = diagnosticId,
                    severity = FlowDiagnosticSeverity.WARNING,
                    code = "TEST",
                    message = "warning",
                    nodeId = nodeId,
                ),
            ),
        )
        val runtime = FlowRuntimeSnapshot(
            runId = FlowRunId("run:test"),
            sourceSessionId = FlowSourceSessionId("session:test"),
            documentId = graph.documentId,
            documentRevision = graph.documentRevision,
            sequence = 1,
            capturedAtEpochMs = 1,
            activeNodeId = nodeId,
            nodeStates = mapOf(nodeId to FlowRuntimeNodeState.RUNNING),
        )

        val state = FlowchartNodeVisualAdapter.map(
            FlowchartNodeVisualSubject(
                node = node,
                graph = graph,
                runtimeSnapshot = runtime,
                selectedNodeId = nodeId,
            ),
            VisualContext(projection = ProjectionKind.Flowchart),
        )

        assertEquals(VisualRole.WorkflowCondition, state.role)
        assertEquals(VisualActivity.Running, state.activity)
        assertEquals(VisualValidation.Warning, state.validation)
        assertEquals(VisualFocus.Selected, state.focus)
    }

    @Test
    fun mapsWorldviewConflictAndAmbiguityWithoutVisualStyleLeakage() {
        val entityState = WorldEntityVisualAdapter.map(
            WorldEntity(
                id = "entity:login",
                kind = WorldEntityKind.UiElement,
                label = "Login",
                state = KnowledgeState.Conflicting,
            ),
            VisualContext(projection = ProjectionKind.Inspector),
        )
        val ambiguityState = AmbiguityVisualAdapter.map(
            WorldAmbiguity(
                id = "ambiguity:login",
                type = AmbiguityType.ProviderConflict,
                subjectRefs = setOf("entity:login"),
                confidence = 0.6f,
                impact = "Provider disagree.",
                resolutionState = AmbiguityResolutionState.Proposed,
            ),
            VisualContext(projection = ProjectionKind.SceneCanvas),
        )

        assertEquals(VisualRole.WorldEntity, entityState.role)
        assertEquals(VisualCertainty.Conflicting, entityState.certainty)
        assertEquals(VisualValidation.Warning, entityState.validation)
        assertEquals(VisualRole.Ambiguity, ambiguityState.role)
        assertEquals(VisualAuthority.AiProposed, ambiguityState.authority)
        assertEquals(VisualCertainty.Ambiguous, ambiguityState.certainty)
    }
}
