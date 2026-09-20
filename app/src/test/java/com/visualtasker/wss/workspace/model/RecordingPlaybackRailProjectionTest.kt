package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingPlaybackRailProjectionTest {
    @Test
    fun mergeKeepsEvidenceButSuppressesDuplicateRawClick() {
        val canonical = RecorderStepUi(
            id = "entry-1",
            label = "Login",
            actionType = "tap",
            status = StepStatus.Executed,
            timestampMs = 1_000,
        )
        val duplicateClick = RecorderStepUi(
            id = "raw-1",
            label = "Click Login",
            actionType = "button.click",
            status = StepStatus.Recorded,
            timestampMs = 1_120,
        )
        val activity = RecorderStepUi(
            id = "raw-2",
            label = "Activity: Home",
            actionType = "activity.change",
            status = StepStatus.Recorded,
            timestampMs = 1_300,
        )

        val merged = mergeCanonicalRecordingSteps(
            canonicalSteps = listOf(canonical),
            evidenceSteps = listOf(duplicateClick, activity),
        )

        assertEquals(listOf("entry-1", "raw-2"), merged.map(RecorderStepUi::id))
        assertEquals("record", merged.first().properties["recording.layer"])
        assertEquals("evidence", merged.last().properties["recording.layer"])
        assertTrue(merged.none { it.id == "raw-1" })
    }
}
