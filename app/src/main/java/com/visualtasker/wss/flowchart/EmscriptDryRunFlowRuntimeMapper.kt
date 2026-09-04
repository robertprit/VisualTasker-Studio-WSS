package com.visualtasker.wss.flowchart

import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunEventSeverity
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import de.visualtasker.blockeditor.ir.IrGraph
import de.visualtasker.flowchart.domain.FlowDiagnosticId
import de.visualtasker.flowchart.domain.FlowDiagnosticSeverity
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowGraphExtension
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowRuntimeDiagnostic
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowRunId
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowSourceSessionId

object EmscriptDryRunFlowRuntimeMapper {
    fun map(
        irGraph: IrGraph,
        graph: FlowGraphDocument,
        result: EmscriptDryRunResult,
        sequence: Long,
        capturedAtEpochMs: Long = System.currentTimeMillis(),
        maxEventIndex: Int? = null,
    ): FlowRuntimeSnapshot {
        val irRuntime = EmscriptDryRunIrGraphRuntimeMapper.map(
            irGraph = irGraph,
            result = result,
            sequence = sequence,
            maxEventIndex = maxEventIndex,
        )
        val nodeStates = irRuntime.nodeStates
            .mapKeys { (nodeId, _) -> FlowNodeId(nodeId.value) }
            .filterKeys { nodeId -> graph.nodes.any { it.id == nodeId } }
        val traversedEdges = irRuntime.traversedEdgeIds
            .map { FlowEdgeId(it.value) }
            .filter { edgeId -> graph.edges.any { it.id == edgeId } }
        val activeNodeId = irRuntime.activeNodeId
            ?.let { FlowNodeId(it.value) }
            ?.takeIf { candidate -> graph.nodes.any { it.id == candidate } }
        val diagnostics = buildList {
            irRuntime.events
                .filter { it.severity != EmscriptDryRunEventSeverity.INFO.name }
                .forEach { event ->
                    add(
                        FlowRuntimeDiagnostic(
                            id = FlowDiagnosticId("dry-run-event:${sequence}:${event.index}"),
                            severity = when (event.severity) {
                                EmscriptDryRunEventSeverity.ERROR.name -> FlowDiagnosticSeverity.ERROR
                                else -> FlowDiagnosticSeverity.WARNING
                            },
                            code = event.capability?.let { "CAPABILITY_$it" } ?: "EMSCRIPT_RUNTIME_NOTICE",
                            message = event.message,
                            nodeId = event.nodeId?.let { FlowNodeId(it.value) },
                            edgeId = event.edgeId?.let { FlowEdgeId(it.value) },
                        )
                    )
                }
            if (result is EmscriptDryRunResult.Failure) add(
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
            extensions = runtimeExtensions(irRuntime.events, result),
        )
    }

    private fun runtimeExtensions(
        events: List<IrGraphRuntimeEvent>,
        result: EmscriptDryRunResult,
    ): List<FlowGraphExtension> = buildList {
        add(runtimeEventExtension(events))
        if (result is EmscriptDryRunResult.Success) {
            add(runtimeVariablesExtension(result.variables))
        }
    }

    private fun runtimeEventExtension(events: List<IrGraphRuntimeEvent>): FlowGraphExtension {
        val values = events.map { event ->
            FlowSemanticValue.ObjectValue(
                buildMap {
                    put("index", FlowSemanticValue.NumberValue(event.index.toString()))
                    put("kind", FlowSemanticValue.StringValue(event.kind))
                    put("message", FlowSemanticValue.StringValue(event.message))
                    event.nodeId?.let { put("irNodeId", FlowSemanticValue.StringValue(it.value)) }
                    event.nodeId?.let { put("nodeId", FlowSemanticValue.StringValue(it.value)) }
                    event.edgeId?.let { put("irEdgeId", FlowSemanticValue.StringValue(it.value)) }
                    event.edgeKind?.let { put("edgeKind", FlowSemanticValue.StringValue(it.name)) }
                    put("severity", FlowSemanticValue.StringValue(event.severity))
                    event.command?.let { put("command", FlowSemanticValue.StringValue(it)) }
                    event.capability?.let { put("capability", FlowSemanticValue.StringValue(it)) }
                    event.pluginOwner?.let { put("pluginOwner", FlowSemanticValue.StringValue(it)) }
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
