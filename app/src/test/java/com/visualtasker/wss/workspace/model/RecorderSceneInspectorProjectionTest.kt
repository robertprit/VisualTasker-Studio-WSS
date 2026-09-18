package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecorderSceneInspectorProjectionTest {
    @Test
    fun projectsSceneEvidenceGestureAndMatchingObservations() {
        val step = RecorderStepUi(
            id = "record-7",
            label = "Swipe Login",
            actionType = "swipe",
            status = StepStatus.Recorded,
            timestampMs = 1_200L,
            durationMs = 350L,
            activityName = "LoginActivity",
            detail = "user gesture",
            properties = mapOf(
                "startX" to "10",
                "startY" to "20",
                "endX" to "80",
                "endY" to "120",
                "recording.evidence" to "touch",
                "screenshotPath" to "/tmp/login.png",
            ),
        )
        val matching = WorldObservation(
            id = "observation:record:record-7",
            provider = ObservationProvider.Runtime,
            kind = ObservationKind.Touch,
            properties = mapOf("activity" to "LoginActivity"),
        )
        val unrelated = WorldObservation(
            id = "observation:other",
            provider = ObservationProvider.Runtime,
            kind = ObservationKind.RuntimeEvent,
            properties = mapOf("activity" to "OtherActivity"),
        )

        val projection = RecorderSceneInspectorProjector.project(step, listOf(unrelated, matching))

        assertEquals("LoginActivity", projection.sceneLabel)
        assertEquals(listOf(matching), projection.observations)
        assertEquals(RecorderCanvasGestureKind.Swipe, projection.canvasProjection.gesture?.kind)
        assertTrue(projection.evidence.any { it.first == "recording.evidence" && it.second == "touch" })
        assertTrue(projection.evidence.any { it.first == "screenshotPath" })
    }
}
