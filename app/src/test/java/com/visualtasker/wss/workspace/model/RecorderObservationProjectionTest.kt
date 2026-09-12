package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class RecorderObservationProjectionTest {
    @Test
    fun parsesRecordingBoundsAndPointIntoStructuredStepFields() {
        val file = File.createTempFile("vt-recording", ".jsonl").apply {
            writeText(
                """
                {"index":"0","timestampMs":"1000","elapsedMs":"120","source":"overlay","kind":"click","label":"Click Login","bounds":"10,20,110,70","x":"60","y":"45","activity":"LoginActivity"}
                """.trimIndent() + "\n"
            )
            deleteOnExit()
        }

        val step = RecordingEventStore.recordingStepsFor(file.absolutePath).single()

        assertEquals("click", step.actionType)
        assertEquals("LoginActivity", step.activityName)
        assertEquals(CoordinateSpaceKind.Screen, step.bounds?.coordinateSpace?.kind)
        assertEquals(10f, step.bounds?.left)
        assertEquals(70f, step.bounds?.bottom)
        assertEquals(CoordinateSpaceKind.Screen, step.point?.coordinateSpace?.kind)
        assertEquals(60f, step.point?.x)
        assertEquals("10,20,110,70", step.properties["bounds"])
    }

    @Test
    fun projectsRecorderStepToAccessibilityObservation() {
        val step = RecorderStepUi(
            id = "record-login-0",
            label = "Click Login",
            actionType = "click",
            status = StepStatus.Recorded,
            timestampMs = 120L,
            durationMs = 30L,
            activityName = "LoginActivity",
            bounds = WorldviewRect(
                10f,
                20f,
                110f,
                70f,
                coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
            ),
            point = WorldviewPoint(
                60f,
                45f,
                coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
            ),
        )

        val observation = listOf(step).toRecorderWorldObservations().single()

        assertEquals("observation:record:record-login-0", observation.id)
        assertEquals(ObservationProvider.Accessibility, observation.provider)
        assertEquals(ObservationKind.Touch, observation.kind)
        assertEquals(120L, observation.observedAtEpochMs)
        assertNotNull(observation.bounds)
        assertEquals("true", observation.properties["clickable"])
        assertEquals("LoginActivity", observation.properties["activity"])
    }

    @Test
    fun skipsRecorderStepsWithoutVisualPosition() {
        val steps = listOf(
            RecorderStepUi(
                id = "record-1",
                label = "Activity changed",
                actionType = "activity.change",
                status = StepStatus.Recorded,
            ),
        )

        assertEquals(emptyList<WorldObservation>(), steps.toRecorderWorldObservations())
    }
}
