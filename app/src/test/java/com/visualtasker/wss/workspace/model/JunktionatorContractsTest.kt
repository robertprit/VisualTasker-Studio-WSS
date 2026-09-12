package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JunktionatorContractsTest {
    @Test
    fun clickRecordingStepSeedsEditableAutomationTargets() {
        val plan = JunctionatorSeed.fromRailTraceStep(
            RecorderStepUi(
                id = "record-1",
                label = "Click Play",
                actionType = "click",
                status = StepStatus.Recorded,
                timestampMs = 1200L,
                durationMs = 80L,
                activityName = "RewardedAdActivity",
                detail = "x=540 | y=1440",
            )
        )

        val candidate = plan.primaryCandidate!!

        assertEquals("junction:record-1", plan.id)
        assertEquals(JunctionConfidence.Medium, candidate.confidence)
        assertTrue(JunctionOutputTarget.Marker in candidate.outputTargets)
        assertTrue(JunctionOutputTarget.Block in candidate.outputTargets)
        assertTrue(JunctionOutputTarget.FlowNode in candidate.outputTargets)
        assertTrue(JunctionOutputTarget.Emscript in candidate.outputTargets)
        assertEquals("click", candidate.parameters["actionType"])
        assertEquals("RewardedAdActivity", plan.evidence.single().activityName)
    }

    @Test
    fun executedRuntimeStepBecomesVerifiedEvidence() {
        val plan = JunctionatorSeed.fromRailTraceStep(
            RecorderStepUi(
                id = "dry-3",
                label = "Template verified",
                actionType = "template.match",
                status = StepStatus.Executed,
            )
        )

        assertEquals(JunctionConfidence.Verified, plan.primaryCandidate?.confidence)
        assertEquals(setOf(JunctionOutputTarget.Dataset), plan.primaryCandidate?.outputTargets)
    }
}
