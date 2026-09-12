package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RailTraceModelsTest {
    @Test
    fun surfaceModesMapToExplicitRailFamilies() {
        assertEquals(RailSurfaceMode.Run, RailMode.Step.toSurfaceMode())
        assertEquals(RailSurfaceMode.Run, RailMode.Program.toSurfaceMode())
        assertEquals(RailSurfaceMode.Records, RailMode.Replay.toSurfaceMode())
        assertEquals(RailSurfaceMode.Records, RailMode.Curate.toSurfaceMode())
        assertEquals(RailSurfaceMode.WatchDog, RailMode.Live.toSurfaceMode())

        assertEquals(RailMode.Step, RailSurfaceMode.Run.defaultRailMode())
        assertEquals(RailMode.Replay, RailSurfaceMode.Records.defaultRailMode())
        assertEquals(RailMode.Live, RailSurfaceMode.WatchDog.defaultRailMode())
    }

    @Test
    fun stepModeKeepsExecutionControlSeparateFromWorkflowMutation() {
        val contract = RailModeContract.forMode(RailMode.Step)

        assertEquals(setOf(RailPrimarySource.WorkflowGraph, RailPrimarySource.SandboxExecution), contract.primarySources)
        assertEquals(RailMutationPolicy.ControlledExecution, contract.mutationPolicy)
        assertTrue(contract.canControlExecution)
        assertFalse(contract.canMutateWorkflow)
        assertFalse(contract.canMutateDataset)
    }

    @Test
    fun replayModeIsReadonlyRecordProjection() {
        val contract = RailModeContract.forMode(RailMode.Replay)

        assertEquals(setOf(RailPrimarySource.Record), contract.primarySources)
        assertEquals(RailMutationPolicy.ReadOnly, contract.mutationPolicy)
        assertFalse(contract.canControlExecution)
        assertFalse(contract.canMutateWorkflow)
        assertFalse(contract.canMutateDataset)
    }

    @Test
    fun mapsRecorderStepsToTemporalRailProjection() {
        val steps = listOf(
            RecorderStepUi(
                id = "step-1",
                label = "Click Login",
                actionType = "click",
                status = StepStatus.Recorded,
                timestampMs = 200L,
                durationMs = 120L,
                activityName = "LoginActivity",
                detail = "x=12 | y=34",
            ),
            RecorderStepUi(
                id = "step-2",
                label = "DryRun wait",
                actionType = "wait",
                status = StepStatus.Executed,
                timestampMs = 500L,
                durationMs = 20L,
                activityName = "LoginActivity",
            ),
        )

        val projection = steps.toRailProjection(mode = RailMode.Step, scaleMode = RailScaleMode.Temporal)

        assertEquals(RailMode.Step, projection.mode)
        assertEquals(RailScaleMode.Temporal, projection.scaleMode)
        assertEquals(listOf("Workflow", "Runtime", "Events"), projection.tracks.map { it.label })
        assertEquals(RailTrackKind.Workflow, projection.tracks[0].kind)
        assertEquals(RailTrackKind.Runtime, projection.tracks[1].kind)
        assertEquals(RailTrackKind.Events, projection.tracks[2].kind)
        assertEquals(2, projection.tracks[0].items.size)
        assertEquals(1, projection.tracks[1].items.size)
        assertEquals(1, projection.tracks[2].items.size)
        assertEquals(RailItemStatus.Recorded, projection.tracks[2].items.single().status)
        assertEquals(RailItemStatus.Success, projection.tracks[1].items.single().status)
        assertEquals(580L, projection.tracks[1].items.single().endMs)
        assertEquals(RailSurfaceMode.Run, projection.surfaceMode)
        assertEquals("Run", projection.title)
    }

    @Test
    fun stepModeAddsDataTrackForVariableAndFileOperations() {
        val projection = listOf(
            RecorderStepUi(
                id = "step-1",
                label = "Variable score setzen",
                actionType = "set",
                status = StepStatus.Executed,
                timestampMs = 0L,
            ),
            RecorderStepUi(
                id = "step-2",
                label = "Click OK",
                actionType = "click",
                status = StepStatus.Recorded,
                timestampMs = 200L,
            ),
        ).toRailProjection(mode = RailMode.Step)

        assertEquals(listOf("Workflow", "Runtime", "Events", "Variables"), projection.tracks.map { it.label })
        assertEquals(listOf("rail-step-1"), projection.tracks.single { it.kind == RailTrackKind.Data }.items.map { it.id })
    }
}
