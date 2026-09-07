package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingEventStoreTest {
    @Test
    fun mapsJsonlRecordingToStepperSteps() {
        val file = kotlin.io.path.createTempFile(prefix = "recording", suffix = ".jsonl").toFile()
        file.writeText(
            """
            {"index":"0","timestampMs":"1000","elapsedMs":"0","source":"floatingOverlay","kind":"recording.started","label":"Aufnahme gestartet","file":"overlay.jsonl"}
            {"index":"1","timestampMs":"1100","elapsedMs":"100","source":"floatingOverlay","kind":"activity.change","label":"Activity: LoginActivity","package":"com.example","activity":"LoginActivity"}
            {"index":"2","timestampMs":"1200","elapsedMs":"200","source":"floatingOverlay","kind":"click","label":"Click Login","x":"120","y":"240","text":"Login"}
            """.trimIndent()
        )

        val steps = RecordingEventStore.run { file.toRecorderSteps() }

        assertEquals(2, steps.size)
        assertEquals("Activity: LoginActivity", steps[0].label)
        assertEquals("activity.change", steps[0].actionType)
        assertEquals("LoginActivity", steps[0].activityName)
        assertEquals("Click Login", steps[1].label)
        assertEquals("click", steps[1].actionType)
        assertEquals("LoginActivity", steps[1].activityName)
        assertEquals(StepStatus.Recorded, steps[1].status)
    }

    @Test
    fun mapsRecordingFileToSessionMetadata() {
        val file = kotlin.io.path.createTempFile(prefix = "overlay-20260907", suffix = ".jsonl").toFile()
        file.writeText(
            """
            {"index":"0","timestampMs":"1000","elapsedMs":"0","source":"floatingOverlay","kind":"recording.started","label":"Aufnahme gestartet"}
            {"index":"1","timestampMs":"1500","elapsedMs":"500","source":"floatingOverlay","kind":"activity.change","label":"Activity: A","activity":"A"}
            {"index":"2","timestampMs":"2000","elapsedMs":"1000","source":"floatingOverlay","kind":"click","label":"Click OK","durationMs":"120"}
            """.trimIndent()
        )

        val steps = RecordingEventStore.run { file.toRecorderSteps() }

        assertEquals(2, steps.size)
        assertEquals(1120L, steps.maxOf { (it.timestampMs ?: 0L) + (it.durationMs ?: 0L) })
    }
}
