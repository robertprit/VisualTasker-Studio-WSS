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
                        FlowGraphExtension("visualtasker.ir-source", sourceExtension(node.source)),
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
                        FlowGraphExtension("visualtasker.ir-source", sourceExtension(edge.source)),
                    ),
                )
            },
            diagnostics = diagnostics,
            extensions = listOf(
                FlowGraphExtension("visualtasker.projection-source", FlowSemanticValue.StringValue("ir-graph")),
                FlowGraphExtension("visualtasker.ir-scopes", scopesExtension(graph)),
                FlowGraphExtension("visualtasker.ir-branches", branchesExtension(graph)),
                FlowGraphExtension("visualtasker.ir-facets", facetsExtension(graph)),
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

    private fun scopesExtension(graph: IrGraph): FlowSemanticValue =
        FlowSemanticValue.ListValue(
            graph.scopes.map { scope ->
                FlowSemanticValue.ObjectValue(
                    buildMap {
                        put("id", FlowSemanticValue.StringValue(scope.id))
                        put("kind", FlowSemanticValue.StringValue(scope.kind.name))
                        scope.parentId?.let { put("parentId", FlowSemanticValue.StringValue(it)) }
                        put("label", FlowSemanticValue.StringValue(scope.label))
                        put("source", sourceExtension(scope.source))
                    }
                )
            }
        )

    private fun branchesExtension(graph: IrGraph): FlowSemanticValue =
        FlowSemanticValue.ListValue(
            graph.branches.map { branch ->
                FlowSemanticValue.ObjectValue(
                    buildMap {
                        put("id", FlowSemanticValue.StringValue(branch.id))
                        put("ownerNodeId", FlowSemanticValue.StringValue(branch.ownerNodeId.value))
                        put("role", FlowSemanticValue.StringValue(branch.role.name))
                        put("index", FlowSemanticValue.NumberValue(branch.index.toString()))
                        put("slotName", FlowSemanticValue.StringValue(branch.slotName))
                        put("scopeId", FlowSemanticValue.StringValue(branch.scopeId))
                        branch.conditionNodeId?.let { put("conditionNodeId", FlowSemanticValue.StringValue(it.value)) }
                        branch.bodyEntryNodeId?.let { put("bodyEntryNodeId", FlowSemanticValue.StringValue(it.value)) }
                        put("source", sourceExtension(branch.source))
                    }
                )
            }
        )

    private fun facetsExtension(graph: IrGraph): FlowSemanticValue =
        FlowSemanticValue.ListValue(
            graph.facets.map { facet ->
                FlowSemanticValue.ObjectValue(
                    buildMap {
                        put("id", FlowSemanticValue.StringValue(facet.id))
                        put("kind", FlowSemanticValue.StringValue(facet.kind.name))
                        put("label", FlowSemanticValue.StringValue(facet.label))
                        facet.scopeId?.let { put("scopeId", FlowSemanticValue.StringValue(it)) }
                        facet.ownerNodeId?.let { put("ownerNodeId", FlowSemanticValue.StringValue(it.value)) }
                        put("nodeIds", FlowSemanticValue.ListValue(facet.nodeIds.map { FlowSemanticValue.StringValue(it.value) }))
                        put(
                            "properties",
                            FlowSemanticValue.ObjectValue(facet.properties.mapValues { (_, value) -> FlowSemanticValue.StringValue(value) })
                        )
                        put("source", sourceExtension(facet.source))
                    }
                )
            }
        )

    private fun sourceExtension(source: de.visualtasker.blockeditor.ir.IrGraphSourceRef): FlowSemanticValue =
        FlowSemanticValue.ObjectValue(
            buildMap {
                put("workspaceId", FlowSemanticValue.StringValue(source.workspaceId))
                put("workspaceVersion", FlowSemanticValue.NumberValue(source.workspaceVersion.toString()))
                source.blockId?.let { put("blockId", FlowSemanticValue.StringValue(it)) }
                source.slotName?.let { put("slotName", FlowSemanticValue.StringValue(it)) }
                source.sourceLine?.let { put("sourceLine", FlowSemanticValue.NumberValue(it.toString())) }
                source.sourceColumn?.let { put("sourceColumn", FlowSemanticValue.NumberValue(it.toString())) }
                source.branch?.let { branch ->
                    put(
                        "branch",
                        FlowSemanticValue.ObjectValue(
                            mapOf(
                                "id" to FlowSemanticValue.StringValue(branch.id),
                                "ownerBlockId" to FlowSemanticValue.StringValue(branch.ownerBlockId),
                                "role" to FlowSemanticValue.StringValue(branch.role.name),
                                "index" to FlowSemanticValue.NumberValue(branch.index.toString()),
                                "slotName" to FlowSemanticValue.StringValue(branch.slotName),
                            )
                        )
                    )
                }
            }
        )
}
