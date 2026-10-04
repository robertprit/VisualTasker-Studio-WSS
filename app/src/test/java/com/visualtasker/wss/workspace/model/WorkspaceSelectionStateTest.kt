package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.BlockId
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowNodeId
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunRuntime
import com.visualtasker.wss.emscript.runtime.ExecutionMode
import com.visualtasker.wss.emscript.runtime.toRailTraceSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun railTraceBlockStepFocusesBlockAndFlowNode() {
        val step = RecorderStepUi(
            id = "dry-run-8",
            label = "Log",
            actionType = "log",
            status = StepStatus.Executed,
            properties = mapOf(
                "sourceKind" to "Block",
                "sourceId" to "log-1",
                "sourceLine" to "8",
            ),
        )

        val state = WorkspaceSelectionState()
            .selectRailTraceTarget(step.toRailTraceFocusTarget())

        assertEquals("dry-run-8", state.railStepId)
        assertEquals(BlockId("log-1"), state.blockId)
        assertEquals(FlowNodeId("block:log-1"), state.flowNodeId)
        assertNull(state.flowEdgeId)
        assertEquals(8, state.sourceLine)
    }

    @Test
    fun railTraceEdgeStepPreservesEdgeEndpointMetadata() {
        val target = RecorderStepUi(
            id = "dry-run-9",
            label = "Edge",
            actionType = "edge",
            status = StepStatus.Executed,
            properties = mapOf(
                "sourceKind" to "Edge",
                "sourceId" to "if-1",
                "edgeTargetId" to "log-1",
                "edgeKind" to "TRUE_BRANCH",
            ),
        ).toRailTraceFocusTarget()

        assertEquals(BlockId("if-1"), target.edgeSourceBlockId)
        assertEquals(BlockId("log-1"), target.edgeTargetBlockId)
        assertEquals("TRUE_BRANCH", target.edgeKind)
    }

    @Test
    fun railTraceExplicitEdgeIdSelectsEdgeAndRailStep() {
        val step = RecorderStepUi(
            id = "dry-run-10",
            label = "Edge",
            actionType = "edge",
            status = StepStatus.Executed,
            properties = mapOf(
                "sourceKind" to "Edge",
                "flowEdgeId" to "edge:block:if-1|block:log-1|TRUE_BRANCH|then",
            ),
        )

        val state = WorkspaceSelectionState()
            .selectRailTraceTarget(step.toRailTraceFocusTarget())

        assertEquals("dry-run-10", state.railStepId)
        assertEquals(FlowEdgeId("edge:block:if-1|block:log-1|TRUE_BRANCH|then"), state.flowEdgeId)
        assertNull(state.blockId)
        assertNull(state.flowNodeId)
    }

    @Test
    fun textDryRunRailTraceStepCarriesSourceLineToSelectionState() {
        val result = EmscriptDryRunRuntime().run(
            """
            let value = 1
            log("source")
            """.trimIndent(),
        )
        assertTrue(result is EmscriptDryRunResult.Success)

        val step = (result as EmscriptDryRunResult.Success)
            .toRailTraceSteps(runId = "text-source", mode = ExecutionMode.DryRun)
            .first { it.actionType == "log" }
        val state = WorkspaceSelectionState()
            .selectRailTraceTarget(step.toRailTraceFocusTarget())

        assertEquals(step.id, state.railStepId)
        assertEquals(2, state.sourceLine)
        assertNull(state.blockId)
        assertNull(state.flowNodeId)
        assertNull(state.flowEdgeId)
    }
}
