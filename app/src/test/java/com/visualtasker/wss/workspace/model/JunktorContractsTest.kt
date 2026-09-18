package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JunktorContractsTest {
    @Test
    fun clickRecordingStepSeedsEditableAutomationTargets() {
        val plan = JunktorSeed.fromRailTraceStep(
            RecorderStepUi(
                id = "record-1",
                label = "Click Play",
                actionType = "click",
                status = StepStatus.Recorded,
                timestampMs = 1200L,
                durationMs = 80L,
                activityName = "RewardedAdActivity",
                detail = "x=540 | y=1440",
                point = WorldviewPoint(
                    540f,
                    1440f,
                    CoordinateSpace(CoordinateSpaceKind.Screen),
                ),
            )
        )

        val marker = plan.candidates.single { it.outputTargets == setOf(JunctionOutputTarget.Marker) }
        val block = plan.candidates.single { it.outputTargets == setOf(JunctionOutputTarget.Block) }
        val flowNode = plan.candidates.single { it.outputTargets == setOf(JunctionOutputTarget.FlowNode) }
        val emscript = plan.candidates.single { it.outputTargets == setOf(JunctionOutputTarget.Emscript) }

        assertEquals("junction:record-1", plan.id)
        assertEquals(JunctionConfidence.Medium, marker.confidence)
        assertEquals("markerSave(\"Click Play\", point(540, 1440))", marker.parameters["emscript"])
        assertEquals("clickPoint(540, 1440, 1)", block.parameters["emscript"])
        assertEquals(block.parameters["emscript"], flowNode.parameters["emscript"])
        assertEquals(block.parameters["emscript"], emscript.parameters["emscript"])
        assertEquals("click", block.parameters["actionType"])
        assertEquals("RewardedAdActivity", plan.evidence.single().activityName)
    }

    @Test
    fun executedRuntimeStepBecomesVerifiedEvidence() {
        val plan = JunktorSeed.fromRailTraceStep(
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

    @Test
    fun swipeStepCreatesMarkerBlockNodeAndScriptSuggestions() {
        val plan = JunktorSeed.fromRailTraceStep(
            RecorderStepUi(
                id = "record-swipe",
                label = "Swipe list",
                actionType = "swipe",
                status = StepStatus.Recorded,
                bounds = WorldviewRect(
                    100f,
                    200f,
                    300f,
                    800f,
                    CoordinateSpace(CoordinateSpaceKind.Screen),
                ),
            )
        )

        assertEquals(4, plan.candidates.size)
        assertEquals(
            "swipe([200, 800, 200, 200], 1)",
            plan.candidates.single { it.outputTargets == setOf(JunctionOutputTarget.Block) }.parameters["emscript"],
        )
        assertEquals(
            "markerSave(\"Swipe list\", region(100, 200, 200, 600))",
            plan.candidates.single { it.outputTargets == setOf(JunctionOutputTarget.Marker) }.parameters["emscript"],
        )
    }
}
