package com.visualtasker.wss.workspace.ui

import com.visualtasker.wss.workspace.model.RailSurfaceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
