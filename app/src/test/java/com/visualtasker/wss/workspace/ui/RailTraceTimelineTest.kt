package com.visualtasker.wss.workspace.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RailTraceTimelineTest {
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
}
