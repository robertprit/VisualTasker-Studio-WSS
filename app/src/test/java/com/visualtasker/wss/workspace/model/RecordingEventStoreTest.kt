package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.runtime.ExecutionMode
import com.visualtasker.wss.emscript.runtime.ExecutionOperationStatus
import com.visualtasker.wss.emscript.runtime.ExecutionSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.visualtasker.wss.recording.RecordingInteractionPayload
import com.visualtasker.wss.recording.RecordingInteractionType

class RecordingEventStoreTest {
    @Test
    fun canonicalImportKeepsStableJsonlProvenance() {
        val file = kotlin.io.path.createTempFile(prefix = "canonical-recording", suffix = ".jsonl").toFile()
        file.writeText(
            """
            {"index":"0","timestampMs":"1000","elapsedMs":"0","source":"floatingOverlay","kind":"recording.started","label":"Start"}
            {"index":"1","timestampMs":"1100","elapsedMs":"100","source":"floatingOverlay","kind":"click","label":"Login","x":"120","y":"240","resourceId":"login"}
            {"index":"2","timestampMs":"1200","elapsedMs":"200","source":"floatingOverlay","kind":"activity.change","label":"Home","package":"com.example","activity":"HomeActivity"}
            """.trimIndent(),
        )

        val imported = RecordingEventStore.canonicalImport(file.absolutePath, "session-import")

        assertEquals(3, imported.rawEvents.size)
        assertEquals(2, imported.interactions.size)
        assertEquals(listOf(RecordingInteractionType.TAP, RecordingInteractionType.WINDOW), imported.interactions.map { it.type })
        assertEquals("jsonl:${file.nameWithoutExtension}:1", imported.interactions.first().rawEventIds.single())
        assertEquals(1L, imported.interactions.first().sequence)
        assertEquals(1_100L, imported.interactions.first().occurredAtEpochMs)
        assertEquals(100_000_000L, imported.interactions.first().occurredAtElapsedRealtimeNanos)
        val tap = imported.interactions.first().payload as RecordingInteractionPayload.Tap
        assertEquals("login", tap.targetReference)
    }

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
    fun keepsWindowEvidenceMetadataWhenMappingRecordingToSteps() {
        val file = kotlin.io.path.createTempFile(prefix = "recording-window", suffix = ".jsonl").toFile()
        file.writeText(
            """
            {"index":"0","timestampMs":"1000","elapsedMs":"0","source":"floatingOverlay","kind":"recording.started","label":"Aufnahme gestartet","file":"overlay.jsonl"}
            {"index":"1","timestampMs":"1100","elapsedMs":"100","source":"floatingOverlay","kind":"activity.change","label":"Activity: LoginActivity","package":"com.example","activity":"LoginActivity","recording.evidence":"window.baseline","observationPolicy":"RECORD","window.count":"1","window.activity":"LoginActivity"}
            {"index":"2","timestampMs":"1300","elapsedMs":"300","source":"floatingOverlay","kind":"activity.change","label":"Activity: HomeActivity","package":"com.example","activity":"HomeActivity","recording.evidence":"window.transition","observationPolicy":"RECORD","window.transitionCount":"1","window.transitionKinds":"ACTIVITY_CHANGED","window.previousActivity":"LoginActivity","window.currentActivity":"HomeActivity","window.confidence":"1.0"}
            """.trimIndent()
        )

        val steps = RecordingEventStore.run { file.toRecorderSteps() }

        assertEquals(2, steps.size)
        assertEquals("window.baseline", steps[0].properties["recording.evidence"])
        assertEquals("LoginActivity", steps[0].properties["window.activity"])
        assertEquals("window.transition", steps[1].properties["recording.evidence"])
        assertEquals("ACTIVITY_CHANGED", steps[1].properties["window.transitionKinds"])
        assertEquals("LoginActivity", steps[1].properties["window.previousActivity"])
        assertEquals("HomeActivity", steps[1].properties["window.currentActivity"])
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

    @Test
    fun readsCanonicalSessionLinkFromRawEvidenceHeader() {
        val file = kotlin.io.path.createTempFile(prefix = "recording-linked", suffix = ".jsonl").toFile()
        file.writeText(
            """{"index":"0","timestampMs":"1000","elapsedMs":"0","source":"floatingOverlay","kind":"recording.started","label":"Aufnahme gestartet","canonicalSessionId":"session-42"}"""
        )

        assertEquals("session-42", RecordingEventStore.run { file.linkedCanonicalSessionId() })
    }

    @Test
    fun mapsRecorderStepsToStableRecordingTrace() {
        val file = kotlin.io.path.createTempFile(prefix = "overlay-20260915", suffix = ".jsonl").toFile()
        file.writeText(
            """
            {"index":"0","timestampMs":"1000","elapsedMs":"0","source":"floatingOverlay","kind":"recording.started","label":"Aufnahme gestartet"}
            {"index":"1","timestampMs":"1200","elapsedMs":"200","source":"floatingOverlay","kind":"activity.change","label":"Activity: Demo","activity":"DemoActivity"}
            {"index":"2","timestampMs":"1400","elapsedMs":"400","source":"floatingOverlay","kind":"click","label":"Click Play","x":"120","y":"240","bounds":"100,200,180,280","screenshotPath":"/tmp/demo.png"}
            """.trimIndent()
        )
        val steps = RecordingEventStore.run { file.toRecorderSteps() }

        val trace = steps.toRecordingExecutionTrace(
            runId = "record:demo",
            sourceSessionId = file.absolutePath,
        )

        assertEquals("record:demo", trace.runId.value)
        assertEquals(ExecutionMode.Replay, trace.mode)
        assertEquals(file.absolutePath, trace.sourceSessionId)
        assertEquals(2, trace.operations.size)
        assertEquals(ExecutionOperationStatus.Succeeded, trace.operations.last().status)
        assertEquals(ExecutionSourceKind.Record, trace.operations.last().sourceRef?.kind)
        assertEquals(steps.last().id, trace.operations.last().sourceRef?.id)
        assertTrue(trace.operations.last().evidenceRefs.contains("/tmp/demo.png"))
        assertTrue(trace.operations.last().evidenceRefs.any { it.startsWith("bounds:") })
        assertTrue(trace.operations.last().evidenceRefs.any { it.startsWith("point:") })
    }

    @Test
    fun recordingRailTraceStepsKeepVisualStepFieldsAndAddTraceMetadata() {
        val file = kotlin.io.path.createTempFile(prefix = "overlay-20260915", suffix = ".jsonl").toFile()
        file.writeText(
            """
            {"index":"0","timestampMs":"1000","elapsedMs":"0","source":"floatingOverlay","kind":"recording.started","label":"Aufnahme gestartet"}
            {"index":"1","timestampMs":"1300","elapsedMs":"300","source":"floatingOverlay","kind":"click","label":"Click OK","x":"12","y":"34","bounds":"10,30,20,40","screenshotPath":"/tmp/click.png"}
            """.trimIndent()
        )
        val steps = RecordingEventStore.run { file.toRecorderSteps() }

        val railSteps = steps.toRecordingRailTraceSteps(
            runId = "record:rail",
            sourceSessionId = "session:rail",
        )

        assertEquals(steps.single().id, railSteps.single().id)
        assertEquals(steps.single().bounds, railSteps.single().bounds)
        assertEquals(steps.single().point, railSteps.single().point)
        assertEquals("record:rail", railSteps.single().properties["runId"])
        assertEquals("Replay", railSteps.single().properties["mode"])
        assertEquals("Record", railSteps.single().properties["sourceKind"])
        assertEquals(steps.single().id, railSteps.single().properties["sourceId"])
        assertEquals("/tmp/click.png", railSteps.single().properties["screenshotPath"])
        assertTrue(railSteps.single().properties["evidenceRefs"].orEmpty().contains("/tmp/click.png"))
    }

    @Test
    fun mapsTaskerAndPluginEventsToWatchDogTraceOnly() {
        val file = kotlin.io.path.createTempFile(prefix = "external-tasker-feedback", suffix = ".jsonl").toFile()
        file.writeText(
            """
            {"index":"0","timestampMs":"1000","elapsedMs":"0","source":"tasker","kind":"tasker.feedback","label":"Tasker: done","taskName":"VT_TEST","message":"done","status":"done"}
            {"index":"1","timestampMs":"1200","elapsedMs":"200","source":"floatingOverlay","kind":"click","label":"Click OK","x":"12","y":"34"}
            {"index":"2","timestampMs":"1400","elapsedMs":"400","source":"tasker-plugin","kind":"tasker.plugin.record_event.error","label":"Tasker slot_1/error: Demo","command":"record_event","eventName":"Demo","runId":"run-tasker","status":"error","eventSlot":"slot_1"}
            """.trimIndent()
        )
        val steps = RecordingEventStore.run { file.toRecorderSteps() }
        val watchDogSteps = steps.watchDogSteps()

        val trace = watchDogSteps.toWatchDogExecutionTrace(
            runId = "watchdog:test",
            sourceSessionId = file.absolutePath,
        )

        assertEquals(2, watchDogSteps.size)
        assertEquals(ExecutionMode.WatchDog, trace.mode)
        assertEquals(2, trace.operations.size)
        assertEquals(ExecutionSourceKind.Provider, trace.operations.first().sourceRef?.kind)
        assertEquals("tasker", trace.operations.first().pluginOwner)
        assertEquals(ExecutionOperationStatus.Succeeded, trace.operations.first().status)
        assertEquals(ExecutionOperationStatus.Failed, trace.operations.last().status)
        assertTrue(watchDogSteps.none { it.actionType == "click" })
    }

    @Test
    fun watchdogRailTraceStepsKeepIdsAndExposeProviderMetadata() {
        val steps = listOf(
            RecorderStepUi(
                id = "record-tasker-1",
                label = "Tasker done",
                actionType = "tasker.feedback",
                status = StepStatus.Recorded,
                timestampMs = 100,
                properties = mapOf("status" to "done", "command" to "record_event"),
            ),
        )

        val railSteps = steps.toWatchDogRailTraceSteps(
            runId = "watchdog:rail",
            sourceSessionId = "session:watchdog",
        )

        assertEquals("record-tasker-1", railSteps.single().id)
        assertEquals(StepStatus.Recorded, railSteps.single().status)
        assertEquals("watchdog:rail", railSteps.single().properties["runId"])
        assertEquals("WatchDog", railSteps.single().properties["mode"])
        assertEquals("Provider", railSteps.single().properties["sourceKind"])
        assertEquals("tasker", railSteps.single().properties["pluginOwner"])
        assertEquals("record_event", railSteps.single().properties["command"])
    }

    @Test
    fun watchdogIncludesSystemActivityButtonScreenAndAppEvents() {
        val steps = listOf(
            RecorderStepUi(
                id = "record-activity",
                label = "Activity: LoginActivity",
                actionType = "activity.change",
                status = StepStatus.Recorded,
                properties = mapOf("category" to "activity"),
            ),
            RecorderStepUi(
                id = "record-button",
                label = "Button Login",
                actionType = "button.click",
                status = StepStatus.Recorded,
                properties = mapOf("role" to "button"),
            ),
            RecorderStepUi(
                id = "record-lock",
                label = "Bildschirm gesperrt",
                actionType = "screen.lock",
                status = StepStatus.Recorded,
                properties = mapOf("category" to "screen"),
            ),
            RecorderStepUi(
                id = "record-start",
                label = "App gestartet",
                actionType = "app.start",
                status = StepStatus.Recorded,
                properties = mapOf("category" to "app"),
            ),
            RecorderStepUi(
                id = "record-scroll",
                label = "Scroll list",
                actionType = "scroll",
                status = StepStatus.Recorded,
            ),
        )

        val watchDogSteps = steps.watchDogSteps()
        val trace = watchDogSteps.toWatchDogExecutionTrace(
            runId = "watchdog:system",
            sourceSessionId = "session:system",
        )

        assertEquals(
            listOf("activity.change", "button.click", "screen.lock", "app.start"),
            watchDogSteps.map { it.actionType },
        )
        assertEquals(4, trace.operations.size)
        assertTrue(trace.operations.all { it.pluginOwner == "watchdog" })
    }
}
