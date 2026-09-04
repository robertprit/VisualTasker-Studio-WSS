package com.visualtasker.wss.visual.debug

import de.visualtasker.flowchart.domain.FlowDocumentId
import de.visualtasker.flowchart.domain.FlowDocumentRevision
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowRunId
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowSemanticKind
import de.visualtasker.flowchart.domain.FlowSourceSessionId
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualSemanticsReporterTest {
    @Test
    fun summarizesFlowchartVisualSemanticsForDebugPanel() {
        val graph = FlowGraphDocument(
            documentId = FlowDocumentId("flow:debug"),
            documentRevision = FlowDocumentRevision("1"),
            producerId = "test",
            producerVersion = "1",
            sourceRevision = "1",
            sourceHash = "hash",
            nodes = listOf(
                FlowGraphNode(
                    id = FlowNodeId("block:start"),
                    kind = FlowSemanticKind(standard = FlowNodeKind.ENTRY),
                    label = "Start",
                ),
                FlowGraphNode(
                    id = FlowNodeId("block:if"),
                    kind = FlowSemanticKind(standard = FlowNodeKind.DECISION),
                    label = "if",
                ),
            ),
        )
        val runtime = FlowRuntimeSnapshot(
            runId = FlowRunId("run:debug"),
            sourceSessionId = FlowSourceSessionId("session:debug"),
            documentId = graph.documentId,
            documentRevision = graph.documentRevision,
            sequence = 1,
            capturedAtEpochMs = 1,
            activeNodeId = FlowNodeId("block:if"),
            nodeStates = mapOf(FlowNodeId("block:if") to FlowRuntimeNodeState.RUNNING),
        )

        val lines = VisualSemanticsReporter.summarizeFlowchart(graph, runtime)

        assertTrue(lines.any { it.startsWith("VAL Flowchart: 2 Nodes") })
        assertTrue(lines.any { it.contains("WorkflowCondition") || it.contains("WORKFLOW") })
        assertTrue(lines.any { it.contains("Running") || it.contains("RUNNING") })
    }
}
