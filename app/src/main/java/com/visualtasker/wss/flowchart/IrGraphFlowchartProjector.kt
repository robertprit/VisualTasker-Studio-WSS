package com.visualtasker.wss.flowchart

import de.visualtasker.blockeditor.ir.IrGraph
import de.visualtasker.blockeditor.ir.IrGraphEdgeKind
import de.visualtasker.blockeditor.ir.IrGraphNodeKind
import de.visualtasker.flowchart.domain.FlowDiagnosticId
import de.visualtasker.flowchart.domain.FlowDiagnosticSeverity
import de.visualtasker.flowchart.domain.FlowDocumentId
import de.visualtasker.flowchart.domain.FlowDocumentRevision
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowGraphDiagnostic
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowGraphExtension
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowGraphSourceReference
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticKind
import de.visualtasker.flowchart.domain.FlowSemanticValue

object IrGraphFlowchartProjector {
    fun project(graph: IrGraph): FlowchartProjectionResult {
        val diagnostics = graph.diagnostics.mapIndexed { index, diagnostic ->
            FlowGraphDiagnostic(
                id = FlowDiagnosticId("ir:${index}:${diagnostic.code}"),
                severity = FlowDiagnosticSeverity.WARNING,
                code = diagnostic.code,
                message = diagnostic.message,
                nodeId = diagnostic.source.blockId?.let { FlowNodeId("block:$it") },
                sourceReference = sourceReference(graph, diagnostic.source.blockId, diagnostic.source.slotName),
            )
        }
        val flowGraph = FlowGraphDocument(
            documentId = FlowDocumentId("flow:${graph.id}"),
            documentRevision = FlowDocumentRevision(graph.sourceRevision),
            producerId = "irgraph-flowchart-projector",
            producerVersion = "1",
            sourceRevision = graph.sourceRevision,
            sourceHash = "${graph.id}:${graph.sourceRevision}:${graph.nodes.size}:${graph.edges.size}",
            entryNodeId = graph.entryNodeIds.firstOrNull()?.let { FlowNodeId(it.value) },
            nodes = graph.nodes.map { node ->
                FlowGraphNode(
                    id = FlowNodeId(node.id.value),
                    kind = FlowSemanticKind(kindFor(node.kind)),
                    label = node.label,
                    sourceReference = sourceReference(graph, node.source.blockId, node.source.slotName),
                    properties = node.properties.mapValues { (_, value) -> FlowSemanticValue.StringValue(value) } +
                        ("irScope" to FlowSemanticValue.ListValue(node.scopePath.map(FlowSemanticValue::StringValue))),
                    extensions = listOf(
                        FlowGraphExtension("visualtasker.ir-node-kind", FlowSemanticValue.StringValue(node.kind.name)),
                    ),
                )
            },
            edges = graph.edges.map { edge ->
                de.visualtasker.flowchart.domain.FlowGraphEdge(
                    id = FlowEdgeId(edge.id.value),
                    sourceNodeId = FlowNodeId(edge.sourceNodeId.value),
                    targetNodeId = FlowNodeId(edge.targetNodeId.value),
                    kind = kindFor(edge.kind),
                    label = edge.label,
                    sourceReference = sourceReference(graph, edge.source.blockId, edge.source.slotName),
                    extensions = listOf(
                        FlowGraphExtension("visualtasker.ir-edge-kind", FlowSemanticValue.StringValue(edge.kind.name)),
                    ),
                )
            },
            diagnostics = diagnostics,
            extensions = listOf(
                FlowGraphExtension("visualtasker.projection-source", FlowSemanticValue.StringValue("ir-graph")),
            ),
        )
        return FlowchartProjectionResult(
            graph = flowGraph,
            status = if (diagnostics.any { it.severity == FlowDiagnosticSeverity.ERROR }) {
                FlowchartProjectionStatus.DEGRADED
            } else {
                FlowchartProjectionStatus.RUNNING
            },
        )
    }

    private fun kindFor(kind: IrGraphNodeKind): FlowNodeKind = when (kind) {
        IrGraphNodeKind.SCRIPT_ENTRY -> FlowNodeKind.ENTRY
        IrGraphNodeKind.ACTION -> FlowNodeKind.ACTION
        IrGraphNodeKind.ASSIGNMENT -> FlowNodeKind.ASSIGNMENT
        IrGraphNodeKind.DECISION -> FlowNodeKind.DECISION
        IrGraphNodeKind.LOOP -> FlowNodeKind.LOOP_START
        IrGraphNodeKind.VALUE -> FlowNodeKind.INPUT
        IrGraphNodeKind.UNKNOWN -> FlowNodeKind.UNKNOWN_SOURCE
    }

    private fun kindFor(kind: IrGraphEdgeKind): FlowEdgeKind = when (kind) {
        IrGraphEdgeKind.SEQUENCE -> FlowEdgeKind.SEQUENCE
        IrGraphEdgeKind.TRUE_BRANCH -> FlowEdgeKind.TRUE_BRANCH
        IrGraphEdgeKind.FALSE_BRANCH -> FlowEdgeKind.FALSE_BRANCH
        IrGraphEdgeKind.ELSE_IF_BRANCH -> FlowEdgeKind.ELSE_IF_BRANCH
        IrGraphEdgeKind.LOOP_BODY -> FlowEdgeKind.LOOP_BODY
        IrGraphEdgeKind.LOOP_EXIT -> FlowEdgeKind.LOOP_EXIT
        IrGraphEdgeKind.CONDITION -> FlowEdgeKind.CONDITION
        IrGraphEdgeKind.DATA_FLOW -> FlowEdgeKind.DATA_FLOW
    }

    private fun sourceReference(
        graph: IrGraph,
        blockId: String?,
        slotName: String?,
    ): FlowGraphSourceReference = FlowGraphSourceReference(
        producerDocumentId = graph.id,
        sourceRevision = graph.sourceRevision,
        sourceKind = "ir-graph",
        canonicalText = listOfNotNull(blockId?.let { "block:$it" }, slotName?.let { "slot:$it" }).joinToString(" "),
    )
}
