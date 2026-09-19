package com.visualtasker.wss.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordingPlaybackReviewResolutionTest {
    @Test
    fun preservedA11yCorrectionWinsOverOriginalTargetDuringStatusChange() {
        val correctedTarget = a11yTarget("corrected-node")

        val selected = resolveSelectedTargetNodeId(
            correctedProposal = null,
            preservedCorrection = StepProposalSnapshot(CandidateAction.TAP, correctedTarget, "Korrigiert"),
            requestedTargetNodeId = "original-node",
            existingTargetNodeId = "original-node",
        )

        assertEquals("corrected-node", selected)
    }

    @Test
    fun preservedCoordinateCorrectionClearsOriginalTargetDuringStatusChange() {
        val selected = resolveSelectedTargetNodeId(
            correctedProposal = null,
            preservedCorrection = StepProposalSnapshot(
                CandidateAction.TAP,
                CandidateTarget.Coordinate(30, 50),
                "Koordinate",
            ),
            requestedTargetNodeId = "original-node",
            existingTargetNodeId = "original-node",
        )

        assertNull(selected)
    }

    @Test
    fun uncorrectedReviewUsesRequestedTarget() {
        val selected = resolveSelectedTargetNodeId(
            correctedProposal = null,
            preservedCorrection = null,
            requestedTargetNodeId = "original-node",
            existingTargetNodeId = null,
        )

        assertEquals("original-node", selected)
    }

    private fun a11yTarget(nodeId: String) = CandidateTarget.A11y(
        nodeId = nodeId,
        resourceId = null,
        text = null,
        contentDescription = null,
        className = "android.view.View",
        left = 0,
        top = 0,
        right = 100,
        bottom = 100,
        clickable = true,
        visible = true,
        enabled = true,
        packageName = "com.example",
        windowId = "window-1",
        xPx = 50,
        yPx = 50,
    )
}
