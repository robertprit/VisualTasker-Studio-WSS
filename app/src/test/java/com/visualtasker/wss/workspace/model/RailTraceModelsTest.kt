package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.emscript.runtime.ExecutionMode
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.WorkspaceDryRunRuntime
import com.visualtasker.wss.emscript.runtime.dryRunEventIndexOrNull
import com.visualtasker.wss.emscript.runtime.toRailTraceSteps
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
    fun surfaceModesDeclareStableSourceKinds() {
        assertEquals(RailSourceKind.WorkflowRun, RailSurfaceMode.Run.toSourceKind())
        assertEquals(RailSourceKind.Recording, RailSurfaceMode.Records.toSourceKind())
        assertEquals(RailSourceKind.WatchDog, RailSurfaceMode.WatchDog.toSourceKind())
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
        assertEquals(RailSourceKind.WorkflowRun, projection.sourceKind)
        assertEquals("Workflow Run", projection.title)
        assertEquals("Dry/Wet Run aus dem gemeinsamen Workflow-Graph", projection.description)
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

    @Test
    fun liveModeSplitsWatchDogSystemAndProviderTracks() {
        val projection = listOf(
            RecorderStepUi(
                id = "activity",
                label = "Activity: Login",
                actionType = "activity.change",
                status = StepStatus.Recorded,
                properties = mapOf("category" to "activity"),
            ),
            RecorderStepUi(
                id = "tasker",
                label = "Tasker done",
                actionType = "tasker.feedback",
                status = StepStatus.Recorded,
                properties = mapOf("pluginOwner" to "tasker"),
            ),
            RecorderStepUi(
                id = "runtime",
                label = "WatchDog heartbeat",
                actionType = "heartbeat",
                status = StepStatus.Edited,
            ),
        ).toRailProjection(mode = RailMode.Live)

        assertEquals(RailSurfaceMode.WatchDog, projection.surfaceMode)
        assertEquals(listOf("System", "Provider", "Runtime"), projection.tracks.map { it.label })
        assertEquals(listOf("rail-activity"), projection.tracks.single { it.id == "watchdog-system" }.items.map { it.id })
        assertEquals(listOf("rail-tasker"), projection.tracks.single { it.id == "watchdog-provider" }.items.map { it.id })
        assertEquals(listOf("rail-runtime"), projection.tracks.single { it.id == "live-runtime" }.items.map { it.id })
    }

    @Test
    fun mapsExecutionTraceToRailTraceStepsWithOperationMetadata() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            LET score = 1
            log("rail")
            """.trimIndent(),
            workspaceId = "rail-trace-runtime",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val result = WorkspaceDryRunRuntime().run(imported.document!!)
        assertTrue(result is EmscriptDryRunResult.Success)

        val steps = result.toRailTraceSteps(
            runId = "run:rail",
            mode = ExecutionMode.DryRun,
            sourceSessionId = "session:rail",
            startedAtEpochMs = 12,
        )

        assertTrue(steps.isNotEmpty())
        assertEquals("dry-run-1", steps.first().id)
        assertEquals("run:rail", steps.first().properties["runId"])
        assertEquals("run:rail:op:1", steps.first().properties["operationId"])
        assertEquals("DryRun", steps.first().properties["mode"])
        assertEquals("session:rail", steps.first().properties["sourceSessionId"])
        assertEquals("Succeeded", steps.first().properties["operationStatus"])
        assertTrue(steps.any {
            it.actionType == "log" &&
                it.properties["command"] == "log" &&
                it.properties["sourceKind"] == "Block"
        })
        assertEquals(2, "dry-run-2".dryRunEventIndexOrNull())
    }
}
