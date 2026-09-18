package com.visualtasker.wss.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveMarkerGestureBuilderTest {
    @Test
    fun regionKeepsStartAndLatestCorner() {
        val builder = LiveMarkerGestureBuilder(LiveMarkerMode.Region)
        builder.start(10f, 20f)
        builder.move(30f, 40f)
        val points = builder.finish(50f, 60f)

        assertEquals(listOf(LiveMarkerPoint(10f, 20f), LiveMarkerPoint(50f, 60f)), points)
    }

    @Test
    fun splineKeepsIntermediatePoints() {
        val builder = LiveMarkerGestureBuilder(LiveMarkerMode.Spline)
        builder.start(1f, 2f)
        builder.move(3f, 4f)
        val points = builder.finish(5f, 6f)

        assertEquals(3, points.size)
        assertEquals(LiveMarkerPoint(3f, 4f), points[1])
    }
}
