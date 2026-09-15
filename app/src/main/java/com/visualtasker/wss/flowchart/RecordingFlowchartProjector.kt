package com.visualtasker.wss.flowchart

import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.StepStatus
import de.visualtasker.flowchart.domain.FlowDocumentId
import de.visualtasker.flowchart.domain.FlowDocumentRevision
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowExecutionKind
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowGraphEdge
import de.visualtasker.flowchart.domain.FlowGraphExtension
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowLifecycleSemantics
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticKind
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowTerminatorRole

object RecordingFlowchartProjector {
    const val STEP_ID_PROPERTY = "recordingStepId"

    fun project(steps: List<RecorderStepUi>): FlowGraphDocument {
        val signature = steps.joinToString(separator = "|") { step ->
            "${step.id}:${step.actionType}:${step.timestampMs}:${step.durationMs}:${step.status}"
        }.hashCode().toUInt().toString(16)
        val startId = FlowNodeId("recording:start")
        val endId = FlowNodeId("recording:end")
        val stepNodes = steps.map { step ->
            FlowGraphNode(
                id = FlowNodeId(step.id),
                kind = FlowSemanticKind(step.flowNodeKind()),
                label = step.label,
                properties = buildMap {
                    put("blockType", FlowSemanticValue.StringValue("recording.${step.actionType}"))
                    put(STEP_ID_PROPERTY, FlowSemanticValue.StringValue(step.id))
                    put("recordingStatus", FlowSemanticValue.StringValue(step.status.name))
                    step.timestampMs?.let { put("timestampMs", FlowSemanticValue.NumberValue(it.toString())) }
                    step.durationMs?.let { put("durationMs", FlowSemanticValue.NumberValue(it.toString())) }
                    step.activityName?.let { put("activityName", FlowSemanticValue.StringValue(it)) }
                    step.detail?.let { put("detail", FlowSemanticValue.StringValue(it)) }
                    step.properties.forEach { (key, value) ->
                        put("recording.$key", FlowSemanticValue.StringValue(value))
                    }
                },
            )
        }
        val start = FlowGraphNode(
            id = startId,
            kind = FlowSemanticKind(FlowNodeKind.ENTRY),
            label = "Recording Start",
            properties = FlowLifecycleSemantics.nodeProperties(FlowExecutionKind.RECORDING, FlowTerminatorRole.START),
        )
        val end = FlowGraphNode(
            id = endId,
            kind = FlowSemanticKind(FlowNodeKind.EXIT),
            label = "Recording End",
            properties = FlowLifecycleSemantics.nodeProperties(FlowExecutionKind.RECORDING, FlowTerminatorRole.END),
        )
        val chain = listOf(startId) + stepNodes.map { it.id } + endId
        val edges = chain.zipWithNext().mapIndexed { index, (source, target) ->
            FlowGraphEdge(
                id = FlowEdgeId("recording:sequence:$index"),
                sourceNodeId = source,
                targetNodeId = target,
                kind = FlowEdgeKind.SEQUENCE,
            )
        }
        return FlowGraphDocument(
            documentId = FlowDocumentId("recording:$signature"),
            documentRevision = FlowDocumentRevision("recording-$signature"),
            producerId = "recording-flowchart-projector",
            producerVersion = "1",
            sourceRevision = "recording-$signature",
            sourceHash = signature,
            entryNodeId = startId,
            nodes = listOf(start) + stepNodes + end,
            edges = edges,
            extensions = listOf(
                FlowLifecycleSemantics.graphExtension(FlowExecutionKind.RECORDING),
                FlowGraphExtension("visualtasker.projection-source", FlowSemanticValue.StringValue("recording")),
            ),
        )
    }

    private fun RecorderStepUi.flowNodeKind(): FlowNodeKind {
        val type = actionType.lowercase()
        return when {
            status == StepStatus.Invalid -> FlowNodeKind.UNKNOWN_SOURCE
            type.contains("activity") || type.contains("window") || type.contains("scene") -> FlowNodeKind.ANNOTATION
            type.contains("input") || type.contains("text") -> FlowNodeKind.INPUT
            type.contains("result") || type.contains("output") -> FlowNodeKind.OUTPUT
            type.contains("condition") || type.contains("assert") || type.contains("match") -> FlowNodeKind.DECISION
            else -> FlowNodeKind.ACTION
        }
    }
}
