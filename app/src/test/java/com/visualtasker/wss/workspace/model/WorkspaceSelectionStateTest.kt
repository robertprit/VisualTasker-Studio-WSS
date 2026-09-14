package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowNodeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceSelectionStateTest {
    @Test
    fun blockSelectionProjectsToFlowNodeAndClearsEdge() {
        val state = WorkspaceSelectionState(flowEdgeId = FlowEdgeId("edge-1"))
            .selectBlock(BlockId("wait-1"))

        assertEquals(BlockId("wait-1"), state.blockId)
        assertEquals(FlowNodeId("block:wait-1"), state.flowNodeId)
        assertNull(state.flowEdgeId)
        assertEquals("blockeditor", state.source)
    }

    @Test
    fun flowNodeSelectionRestoresBlockIdForBlockNodes() {
        val state = WorkspaceSelectionState()
            .selectFlowNode(FlowNodeId("block:click-1"))

        assertEquals(BlockId("click-1"), state.blockId)
        assertEquals(FlowNodeId("block:click-1"), state.flowNodeId)
        assertNull(state.flowEdgeId)
    }

    @Test
    fun nonBlockFlowNodeDoesNotInventBlockId() {
        val state = WorkspaceSelectionState(blockId = BlockId("old"))
            .selectFlowNode(FlowNodeId("facet:region-1"))

        assertNull(state.blockId)
        assertEquals(FlowNodeId("facet:region-1"), state.flowNodeId)
    }

    @Test
    fun edgeSelectionClearsVisualNodeSelectionButKeepsRailStep() {
        val state = WorkspaceSelectionState()
            .selectBlock(BlockId("if-1"))
            .selectRailStep("dry-run-7")
            .selectFlowEdge(FlowEdgeId("edge-7"))

        assertNull(state.blockId)
        assertNull(state.flowNodeId)
        assertEquals(FlowEdgeId("edge-7"), state.flowEdgeId)
        assertEquals("dry-run-7", state.railStepId)
    }

    @Test
    fun railStepSelectionCanCarryTextSourceLine() {
        val state = WorkspaceSelectionState()
            .selectBlock(BlockId("log-1"), sourceLine = 12)
            .selectRailStep("dry-run-12", sourceLine = 12)

        assertEquals(BlockId("log-1"), state.blockId)
        assertEquals(FlowNodeId("block:log-1"), state.flowNodeId)
        assertEquals("dry-run-12", state.railStepId)
        assertEquals(12, state.sourceLine)
    }
}
