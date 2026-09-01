package com.visualtasker.wss.flowchart

import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import de.visualtasker.flowchart.domain.FlowDiagnosticId
import de.visualtasker.flowchart.domain.FlowDiagnosticSeverity
import de.visualtasker.flowchart.domain.FlowGraphExtension
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowRuntimeDiagnostic
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowRunId
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowSourceSessionId

object EmscriptDryRunFlowRuntimeMapper {
    fun map(
        graph: FlowGraphDocument,
        result: EmscriptDryRunResult,
        sequence: Long,
        capturedAtEpochMs: Long = System.currentTimeMillis(),
        maxEventIndex: Int? = null,
    ): FlowRuntimeSnapshot {
        val executableNodes = graph.nodes
            .map { it.id }
        val events = when (result) {
            is EmscriptDryRunResult.Success -> result.events
            is EmscriptDryRunResult.Failure -> result.events
        }.let { sourceEvents ->
            maxEventIndex?.let { limit -> sourceEvents.filter { it.index <= limit } } ?: sourceEvents
        }
        val tracedNodeIds = events
            .mapNotNull { it.blockId?.let { blockId -> FlowNodeId("block:$blockId") } }
            .filter { traced -> graph.nodes.any { it.id == traced } }
            .toSet()
        val traversedEdges = events.mapNotNull { event ->
            val source = event.edgeSourceBlockId?.let { FlowNodeId("block:$it") } ?: return@mapNotNull null
            val target = event.edgeTargetBlockId?.let { FlowNodeId("block:$it") } ?: return@mapNotNull null
            graph.edges.firstOrNull { edge ->
                edge.sourceNodeId == source &&
                    edge.targetNodeId == target &&
                    (event.edgeKind == null || edge.kind.name == event.edgeKind)
            }?.id
        }.distinct()
        val activeNodeId = events
            .asReversed()
            .firstNotNullOfOrNull { event ->
                event.blockId?.let { blockId -> FlowNodeId("block:$blockId") }
            }
            ?.takeIf { candidate -> graph.nodes.any { it.id == candidate } }
        val hasWorkspaceTrace = tracedNodeIds.isNotEmpty() || traversedEdges.isNotEmpty()
        val nodeStates = when (result) {
            is EmscriptDryRunResult.Success -> if (hasWorkspaceTrace) {
                executableNodes.associateWith { nodeId ->
                    if (nodeId in tracedNodeIds) FlowRuntimeNodeState.SUCCEEDED else FlowRuntimeNodeState.SKIPPED
                }
            } else {
                emptyMap()
            }
            is EmscriptDryRunResult.Failure -> {
                val lastTraced = tracedNodeIds.lastOrNull()
                if (hasWorkspaceTrace) {
                    executableNodes.associateWith { nodeId ->
                        when {
                            nodeId == lastTraced -> FlowRuntimeNodeState.FAILED
                            nodeId in tracedNodeIds -> FlowRuntimeNodeState.SUCCEEDED
                            else -> FlowRuntimeNodeState.SKIPPED
                        }
                    }
                } else {
                    graph.entryNodeId?.let { mapOf(it to FlowRuntimeNodeState.FAILED) }.orEmpty()
                }
            }
        }
        val diagnostics = when (result) {
            is EmscriptDryRunResult.Success -> emptyList()
            is EmscriptDryRunResult.Failure -> listOf(
                FlowRuntimeDiagnostic(
                    id = FlowDiagnosticId("dry-run-failure:$sequence"),
                    severity = FlowDiagnosticSeverity.ERROR,
                    code = "EMSCRIPT_DRY_RUN_FAILED",
                    message = result.message,
                    nodeId = graph.entryNodeId,
                )
            )
        }
        return FlowRuntimeSnapshot(
            runId = FlowRunId("emscript-dry-run"),
            sourceSessionId = FlowSourceSessionId("workspace-emscript"),
            documentId = graph.documentId,
            documentRevision = graph.documentRevision,
            sequence = sequence,
            capturedAtEpochMs = capturedAtEpochMs,
            activeNodeId = activeNodeId,
            nodeStates = nodeStates,
            traversedEdgeIds = traversedEdges,
            diagnostics = diagnostics,
            extensions = runtimeExtensions(events, result),
        )
    }

    private fun runtimeExtensions(
        events: List<com.visualtasker.wss.emscript.runtime.EmscriptDryRunEvent>,
        result: EmscriptDryRunResult,
    ): List<FlowGraphExtension> = buildList {
        add(runtimeEventExtension(events))
        if (result is EmscriptDryRunResult.Success) {
            add(runtimeVariablesExtension(result.variables))
        }
    }

    private fun runtimeEventExtension(events: List<com.visualtasker.wss.emscript.runtime.EmscriptDryRunEvent>): FlowGraphExtension {
        val values = events.map { event ->
            FlowSemanticValue.ObjectValue(
                buildMap {
                    put("index", FlowSemanticValue.NumberValue(event.index.toString()))
                    put("kind", FlowSemanticValue.StringValue(event.kind))
                    put("message", FlowSemanticValue.StringValue(event.message))
                    event.blockId?.let { put("nodeId", FlowSemanticValue.StringValue("block:$it")) }
                    event.edgeSourceBlockId?.let { put("edgeSourceNodeId", FlowSemanticValue.StringValue("block:$it")) }
                    event.edgeTargetBlockId?.let { put("edgeTargetNodeId", FlowSemanticValue.StringValue("block:$it")) }
                    event.edgeKind?.let { put("edgeKind", FlowSemanticValue.StringValue(it)) }
                }
            )
        }
        return FlowGraphExtension(
            key = "visualtasker.runtime-events",
            value = FlowSemanticValue.ListValue(values),
        )
    }

    private fun runtimeVariablesExtension(variables: Map<String, EmscriptValue>): FlowGraphExtension =
        FlowGraphExtension(
            key = "visualtasker.runtime-variables",
            value = FlowSemanticValue.ObjectValue(
                variables.mapValues { (_, value) -> FlowSemanticValue.StringValue(value.renderRuntimeValue()) }
            ),
        )

    private fun EmscriptValue.renderRuntimeValue(): String = when (this) {
        is EmscriptValue.NumberValue -> if (value.isFinite() && value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is EmscriptValue.StringValue -> value
        is EmscriptValue.BooleanValue -> value.toString()
        EmscriptValue.NullValue -> "null"
    }
}
