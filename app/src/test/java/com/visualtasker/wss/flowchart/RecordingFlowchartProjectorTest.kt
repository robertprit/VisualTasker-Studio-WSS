package com.visualtasker.wss.flowchart

import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.StepStatus
import de.visualtasker.flowchart.domain.FlowExecutionKind
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowTerminatorRole
import de.visualtasker.flowchart.domain.executionKind
import de.visualtasker.flowchart.domain.terminatorRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFlowchartProjectorTest {
    @Test
    fun projectsSequentialRecordingWithLifecycleTerminators() {
        val steps = listOf(
            RecorderStepUi("record-1", "Activity", "activity.change", StepStatus.Recorded, timestampMs = 0),
            RecorderStepUi("record-2", "Click", "click", StepStatus.Executed, timestampMs = 40),
        )

        val graph = RecordingFlowchartProjector.project(steps)

        assertEquals(FlowExecutionKind.RECORDING, graph.executionKind())
        assertEquals(4, graph.nodes.size)
        assertEquals(3, graph.edges.size)
        assertEquals(FlowTerminatorRole.START, graph.nodes.first().terminatorRole())
        assertEquals(FlowTerminatorRole.END, graph.nodes.last().terminatorRole())
        assertEquals(listOf("record-1", "record-2"), graph.nodes.drop(1).dropLast(1).map { it.id.value })
        assertEquals(FlowNodeKind.ANNOTATION, graph.nodes[1].kind.standard)
        assertEquals(FlowNodeKind.ACTION, graph.nodes[2].kind.standard)
    }

    @Test
    fun emptyRecordingStillProducesExecutableBoundary() {
        val graph = RecordingFlowchartProjector.project(emptyList())

        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
        assertEquals(graph.nodes.first().id, graph.edges.single().sourceNodeId)
        assertEquals(graph.nodes.last().id, graph.edges.single().targetNodeId)
        assertTrue(graph.sourceHash.isNotBlank())
    }
}
