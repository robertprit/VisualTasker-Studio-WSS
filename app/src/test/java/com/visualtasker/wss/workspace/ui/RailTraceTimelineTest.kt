package com.visualtasker.wss.workspace.ui

import com.visualtasker.wss.workspace.model.RailSurfaceMode
import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.RecordingSessionUi
import com.visualtasker.wss.workspace.model.StepStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RailTraceTimelineTest {

    @Test
    fun `runtime focus applies only to run rail`() {
        assertEquals(2, activeRuntimeRailIndex(RailSurfaceMode.Run, activeIndex = 2, stepCount = 4))
        assertNull(activeRuntimeRailIndex(RailSurfaceMode.WatchDog, activeIndex = 2, stepCount = 4))
        assertNull(activeRuntimeRailIndex(RailSurfaceMode.Records, activeIndex = 2, stepCount = 4))
    }

    @Test
    fun `runtime focus rejects stale index`() {
        assertNull(activeRuntimeRailIndex(RailSurfaceMode.Run, activeIndex = 4, stepCount = 4))
        assertNull(activeRuntimeRailIndex(RailSurfaceMode.Run, activeIndex = null, stepCount = 4))
    }

    @Test
    fun nearestTimelineIndexFindsClosestStepAcrossLongTimelines() {
        val startTimesMs = (0 until 1_000).map { index -> index * 1_000L }

        assertEquals(0, nearestTimelineIndexByStartTimes(startTimesMs, 0L))
        assertEquals(10, nearestTimelineIndexByStartTimes(startTimesMs, 10_499L))
        assertEquals(11, nearestTimelineIndexByStartTimes(startTimesMs, 10_501L))
        assertEquals(999, nearestTimelineIndexByStartTimes(startTimesMs, Long.MAX_VALUE))
    }

    @Test
    fun nearestTimelineIndexHandlesEmptyTimeline() {
        assertEquals(0, nearestTimelineIndexByStartTimes(emptyList(), 500L))
    }

    @Test
    fun timelineWidthIsBoundedForLongSavedRails() {
        assertEquals(360f, railTimelineWidthDp(stepCount = 0, zoom = 1f))
        assertEquals(6_000f, railTimelineWidthDp(stepCount = 10_000, zoom = 4f))
    }

    @Test
    fun canonicalPlaybackUsesEntryIdentityInsteadOfMixedRailIndex() {
        val evidence = RecorderStepUi(
            id = "raw-before",
            label = "Evidence",
            actionType = "activity.change",
            status = StepStatus.Recorded,
        )
        val canonical = RecorderStepUi(
            id = "entry-first",
            label = "Tap",
            actionType = "tap",
            status = StepStatus.Recorded,
            properties = mapOf("recording.entryId" to "entry-first"),
        )

        assertNull(canonicalEntryIndexForRailStep(evidence, listOf("entry-first")))
        assertEquals(0, canonicalEntryIndexForRailStep(canonical, listOf("entry-first", "entry-second")))
    }

    @Test
    fun `canonical recording playback is restricted to records rail`() {
        val canonical = RecorderStepUi(
            id = "entry-1",
            label = "Tap",
            actionType = "tap",
            status = StepStatus.Recorded,
            properties = mapOf("recording.entryId" to "entry-1"),
        )
        val evidence = RecorderStepUi(
            id = "evidence-1",
            label = "Activity",
            actionType = "activity.change",
            status = StepStatus.Recorded,
        )

        assertTrue(usesCanonicalRecordingPlayback(RailSurfaceMode.Records, "room:recording-1", listOf(canonical)))
        assertFalse(usesCanonicalRecordingPlayback(RailSurfaceMode.Records, "room:recording-1", listOf(canonical, evidence)))
        assertFalse(usesCanonicalRecordingPlayback(RailSurfaceMode.Run, "room:recording-1", listOf(canonical)))
        assertFalse(usesCanonicalRecordingPlayback(RailSurfaceMode.WatchDog, "room:recording-1", listOf(canonical)))
        assertFalse(usesCanonicalRecordingPlayback(RailSurfaceMode.Records, null, listOf(canonical)))
    }

    @Test
    fun `canonical session resolves its linked complete evidence stream`() {
        val evidence = RecordingSessionUi(
            path = "/records/overlay-1.jsonl",
            fileName = "overlay-1.jsonl",
            label = "overlay-1",
            lastModifiedMs = 1L,
            stepCount = 173,
            durationMs = 95_000L,
            linkedCanonicalSessionId = "recording-1",
        )

        assertEquals(
            evidence.path,
            resolveRecordingEvidencePath("room:recording-1", listOf(evidence)),
        )
        assertEquals(
            "/records/legacy.jsonl",
            resolveRecordingEvidencePath("/records/legacy.jsonl", listOf(evidence)),
        )
    }
}
