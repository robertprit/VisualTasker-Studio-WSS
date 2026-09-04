package com.visualtasker.wss.flowchart

import de.visualtasker.blockeditor.ir.IrGraph
import de.visualtasker.blockeditor.ir.IrGraphEdgeKind
import de.visualtasker.blockeditor.ir.IrGraphFacet
import de.visualtasker.blockeditor.ir.IrGraphFacetKind
import de.visualtasker.blockeditor.ir.IrGraphNodeKind
import de.visualtasker.blockeditor.ir.IrGraphSourceRef
import de.visualtasker.blockeditor.ir.validateIntegrity
import de.visualtasker.blockeditor.ir.validateSemantics
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
        val irDiagnostics = graph.diagnostics + graph.validateIntegrity() + graph.validateSemantics()
        val diagnostics = irDiagnostics.mapIndexed { index, diagnostic ->
            FlowGraphDiagnostic(
                id = FlowDiagnosticId("ir:${index}:${diagnostic.code}"),
                severity = if (diagnostic.code.startsWith("UNKNOWN_") || diagnostic.code.startsWith("DUPLICATE_")) {
                    FlowDiagnosticSeverity.ERROR
                } else {
                    FlowDiagnosticSeverity.WARNING
                },
                code = diagnostic.code,
                message = diagnostic.message,
                nodeId = diagnostic.source.blockId?.let { FlowNodeId("block:$it") },
                sourceReference = sourceReference(graph, diagnostic.source.blockId, diagnostic.source.slotName),
            )
        }
        val projectedNodes = graph.nodes.map { node ->
            FlowGraphNode(
                id = FlowNodeId(node.id.value),
                kind = FlowSemanticKind(kindFor(node.kind)),
                label = node.label,
                sourceReference = sourceReference(graph, node.source),
                properties = node.properties.mapValues { (key, value) -> nodeProperty(key, value) } +
                    ("irScope" to FlowSemanticValue.ListValue(node.scopePath.map(FlowSemanticValue::StringValue))),
                extensions = listOf(
                    FlowGraphExtension("visualtasker.ir-node-kind", FlowSemanticValue.StringValue(node.kind.name)),
                    FlowGraphExtension("visualtasker.ir-source", sourceExtension(node.source)),
                ),
            )
        }
        val projectedEdges = graph.edges.map { edge ->
            de.visualtasker.flowchart.domain.FlowGraphEdge(
                id = FlowEdgeId(edge.id.value),
                sourceNodeId = FlowNodeId(edge.sourceNodeId.value),
                targetNodeId = FlowNodeId(edge.targetNodeId.value),
                kind = kindFor(edge.kind),
                label = edge.label,
                sourceReference = sourceReference(graph, edge.source),
                extensions = listOf(
                    FlowGraphExtension("visualtasker.ir-edge-kind", FlowSemanticValue.StringValue(edge.kind.name)),
                    FlowGraphExtension("visualtasker.ir-source", sourceExtension(edge.source)),
                ),
            )
        }
        val joinNodes = joinNodes(graph)
        val joinEdges = joinEdges(graph)
        val flowGraph = FlowGraphDocument(
            documentId = FlowDocumentId("flow:${graph.id}"),
            documentRevision = FlowDocumentRevision(graph.sourceRevision),
            producerId = "irgraph-flowchart-projector",
            producerVersion = "1",
            sourceRevision = graph.sourceRevision,
            sourceHash = "${graph.id}:${graph.sourceRevision}:${projectedNodes.size + joinNodes.size}:${projectedEdges.size + joinEdges.size}:${graph.facets.size}",
            entryNodeId = graph.entryNodeIds.firstOrNull()?.let { FlowNodeId(it.value) },
            nodes = projectedNodes + graph.facets.map { facetNode(graph, it) } + joinNodes,
            edges = projectedEdges + joinEdges,
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

    private fun kindFor(kind: IrGraphFacetKind): FlowNodeKind = when (kind) {
        IrGraphFacetKind.BRANCH_REGION,
        IrGraphFacetKind.COMMENT_MARKER,
        -> FlowNodeKind.ANNOTATION
        IrGraphFacetKind.FUNCTION_REGION -> FlowNodeKind.FUNCTION_START
        IrGraphFacetKind.COLLAPSE_GROUP,
        IrGraphFacetKind.VARIABLE_BULK,
        -> FlowNodeKind.SYNTHETIC
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

    private fun joinNodes(graph: IrGraph): List<FlowGraphNode> =
        graph.branches
            .groupBy { it.ownerNodeId }
            .filterValues { it.size >= 2 }
            .map { (ownerNodeId, branches) ->
                val source = branches.first().source
                FlowGraphNode(
                    id = FlowNodeId("join:${ownerNodeId.value}"),
                    kind = FlowSemanticKind(FlowNodeKind.SYNTHETIC),
                    label = "JOIN",
                    sourceReference = sourceReference(graph, source),
                    properties = mapOf(
                        "syntheticJoin" to FlowSemanticValue.BooleanValue(true),
                        "visualFacet" to FlowSemanticValue.BooleanValue(true),
                        "ownerNodeId" to FlowSemanticValue.StringValue(ownerNodeId.value),
                        "branchIds" to FlowSemanticValue.ListValue(branches.map { FlowSemanticValue.StringValue(it.id) }),
                        "inputPorts" to FlowSemanticValue.ListValue(
                            branches.map {
                                FlowSemanticValue.ObjectValue(
                                    mapOf(
                                        "name" to FlowSemanticValue.StringValue(it.role.name),
                                        "label" to FlowSemanticValue.StringValue(it.role.name),
                                        "kind" to FlowSemanticValue.StringValue(FlowEdgeKind.SEQUENCE.name),
                                    )
                                )
                            }
                        ),
                        "outputPorts" to FlowSemanticValue.ListValue(
                            listOf(
                                FlowSemanticValue.ObjectValue(
                                    mapOf(
                                        "name" to FlowSemanticValue.StringValue("next"),
                                        "label" to FlowSemanticValue.StringValue("next"),
                                        "kind" to FlowSemanticValue.StringValue(FlowEdgeKind.SEQUENCE.name),
                                    )
                                )
                            )
                        ),
                    ),
                    extensions = listOf(
                        FlowGraphExtension("visualtasker.ir-join", FlowSemanticValue.StringValue(ownerNodeId.value)),
                        FlowGraphExtension("visualtasker.ir-source", sourceExtension(source)),
                    ),
                )
            }

    private fun joinEdges(graph: IrGraph): List<de.visualtasker.flowchart.domain.FlowGraphEdge> =
        graph.branches
            .groupBy { it.ownerNodeId }
            .filterValues { it.size >= 2 }
            .flatMap { (ownerNodeId, branches) ->
                val nodeIds = graph.nodes.map { it.id }.toSet()
                val outgoingBySource = graph.edges.groupBy { it.sourceNodeId }
                branches.mapNotNull { branch ->
                    val terminal = branchTerminalNodeId(graph, branch.scopeId, branch.bodyEntryNodeId, outgoingBySource, nodeIds)
                        ?: return@mapNotNull null
                    de.visualtasker.flowchart.domain.FlowGraphEdge(
                        id = FlowEdgeId("join:${ownerNodeId.value}:${branch.id}"),
                        sourceNodeId = FlowNodeId(terminal.value),
                        targetNodeId = FlowNodeId("join:${ownerNodeId.value}"),
                        kind = FlowEdgeKind.SEQUENCE,
                        label = branch.role.name,
                        sourceReference = sourceReference(graph, branch.source),
                        extensions = listOf(
                            FlowGraphExtension("visualtasker.ir-join-edge", FlowSemanticValue.StringValue(branch.id)),
                            FlowGraphExtension("visualtasker.ir-source", sourceExtension(branch.source)),
                        ),
                    )
                }
            }

    private fun branchTerminalNodeId(
        graph: IrGraph,
        scopeId: String,
        bodyEntryNodeId: de.visualtasker.blockeditor.ir.IrGraphNodeId?,
        outgoingBySource: Map<de.visualtasker.blockeditor.ir.IrGraphNodeId, List<de.visualtasker.blockeditor.ir.IrGraphEdge>>,
        nodeIds: Set<de.visualtasker.blockeditor.ir.IrGraphNodeId>,
    ): de.visualtasker.blockeditor.ir.IrGraphNodeId? {
        val scoped = graph.nodes
            .filter { scopeId in it.scopePath }
            .map { it.id }
            .toSet()
        if (scoped.isEmpty()) return bodyEntryNodeId?.takeIf { it in nodeIds }
        return scoped
            .filter { nodeId ->
                outgoingBySource[nodeId].orEmpty().none { edge ->
                    edge.kind == IrGraphEdgeKind.SEQUENCE && edge.targetNodeId in scoped
                }
            }
            .sortedBy { it.value }
            .lastOrNull()
            ?: bodyEntryNodeId?.takeIf { it in nodeIds }
    }

    private fun facetNode(graph: IrGraph, facet: IrGraphFacet): FlowGraphNode =
        FlowGraphNode(
            id = FlowNodeId(facet.id),
            kind = FlowSemanticKind(kindFor(facet.kind)),
            label = facet.label,
            sourceReference = sourceReference(graph, facet.source),
            properties = buildMap {
                put("visualFacet", FlowSemanticValue.BooleanValue(true))
                put("facetId", FlowSemanticValue.StringValue(facet.id))
                put("facetKind", FlowSemanticValue.StringValue(facet.kind.name))
                put("nodeIds", FlowSemanticValue.ListValue(facet.nodeIds.map { FlowSemanticValue.StringValue(it.value) }))
                facet.scopeId?.let { put("scopeId", FlowSemanticValue.StringValue(it)) }
                facet.ownerNodeId?.let { put("ownerNodeId", FlowSemanticValue.StringValue(it.value)) }
                put("flowFacet", FlowSemanticValue.BooleanValue(true))
                put("flowFacetRole", FlowSemanticValue.StringValue(flowFacetRole(facet.kind)))
                put("flowFacetNodeCount", FlowSemanticValue.NumberValue(facet.nodeIds.size.toString()))
                facet.properties.forEach { (key, value) -> put(key, nodeProperty(key, value)) }
            },
            extensions = listOf(
                FlowGraphExtension("visualtasker.ir-facet-kind", FlowSemanticValue.StringValue(facet.kind.name)),
                FlowGraphExtension("visualtasker.ir-facet-role", FlowSemanticValue.StringValue(flowFacetRole(facet.kind))),
                FlowGraphExtension("visualtasker.ir-facet-nodes", FlowSemanticValue.ListValue(facet.nodeIds.map { FlowSemanticValue.StringValue(it.value) })),
                FlowGraphExtension("visualtasker.ir-source", sourceExtension(facet.source)),
            ),
        )

    private fun flowFacetRole(kind: IrGraphFacetKind): String = when (kind) {
        IrGraphFacetKind.BRANCH_REGION -> "region.branch"
        IrGraphFacetKind.COLLAPSE_GROUP -> "collapse.group"
        IrGraphFacetKind.COMMENT_MARKER -> "marker.comment"
        IrGraphFacetKind.VARIABLE_BULK -> "bulk.variables"
        IrGraphFacetKind.FUNCTION_REGION -> "region.function"
    }

    private fun sourceReference(
        graph: IrGraph,
        blockId: String?,
        slotName: String?,
    ): FlowGraphSourceReference = sourceReference(
        graph,
        IrGraphSourceRef(
            workspaceId = graph.id,
            workspaceVersion = graph.sourceRevision.toLongOrNull() ?: 0L,
            blockId = blockId,
            slotName = slotName,
        ),
    )

    private fun sourceReference(
        graph: IrGraph,
        source: IrGraphSourceRef,
    ): FlowGraphSourceReference = FlowGraphSourceReference(
        producerDocumentId = graph.id,
        sourceRevision = graph.sourceRevision,
        span = source.sourceLine?.let { line ->
            de.visualtasker.flowchart.domain.FlowSourceSpan(
                startOffset = 0,
                endOffsetExclusive = 0,
                startLine = line,
                startColumn = source.sourceColumn ?: 1,
                endLine = line,
                endColumnExclusive = source.sourceColumn ?: 1,
            )
        },
        sourceKind = "ir-graph",
        canonicalText = listOfNotNull(
            source.blockId?.let { "block:$it" },
            source.slotName?.let { "slot:$it" },
            source.branch?.let { "branch:${it.id}" },
        ).joinToString(" "),
        extensions = listOf(FlowGraphExtension("visualtasker.ir-source", sourceExtension(source))),
    )

    private fun nodeProperty(key: String, value: String): FlowSemanticValue =
        when {
            key == "inputPorts" || key == "outputPorts" -> portListValue(value)
            key in setOf("literalBoolean", "collapsed") -> value.toBooleanStrictOrNull()
                ?.let(FlowSemanticValue::BooleanValue)
                ?: FlowSemanticValue.StringValue(value)
            key in setOf(
                "branchCount",
                "literalNumber",
                "waitMs",
                "frequency",
                "durationMs",
                "volume",
                "sourceLine",
                "sourceColumn",
            ) -> value.toBigDecimalOrNull()
                ?.let { FlowSemanticValue.NumberValue(value) }
                ?: FlowSemanticValue.StringValue(value)
            else -> FlowSemanticValue.StringValue(value)
        }

    private fun portListValue(encoded: String): FlowSemanticValue =
        FlowSemanticValue.ListValue(
            encoded
                .split("|")
                .filter { it.isNotBlank() }
                .mapNotNull(::portValue),
        )

    private fun portValue(encoded: String): FlowSemanticValue? {
        val parts = encoded.split("~")
        if (parts.size != 3) return null
        return FlowSemanticValue.ObjectValue(
            mapOf(
                "name" to FlowSemanticValue.StringValue(parts[0].decodePortPart()),
                "label" to FlowSemanticValue.StringValue(parts[1].decodePortPart()),
                "kind" to FlowSemanticValue.StringValue(kindFor(IrGraphEdgeKind.valueOf(parts[2])).name),
            )
        )
    }

    private fun String.decodePortPart(): String =
        replace("%7C", "|").replace("%7E", "~")

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
