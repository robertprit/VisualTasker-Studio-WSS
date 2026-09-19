package com.visualtasker.chartgraph.demo

import com.visualtasker.chartgraph.demo.data.ChartDataset
import com.visualtasker.chartgraph.demo.data.ChartGeometry
import com.visualtasker.chartgraph.demo.data.DataPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartGeometryTest {

    @Test
    fun lineGeometry_mapsKnownPointsToCanvasCoordinates() {
        val points = listOf(
            DataPoint(x = 0L, y = 0f),
            DataPoint(x = 1L, y = 50f),
            DataPoint(x = 2L, y = 100f)
        )

        val mapped = ChartGeometry.mapLinePoints(
            points = points,
            width = 200f,
            height = 100f,
            minY = 0f,
            maxY = 100f
        )

        assertEquals(3, mapped.size)
        assertEquals(0f, mapped[0].x, 0.0001f)
        assertEquals(100f, mapped[0].y, 0.0001f)
        assertEquals(100f, mapped[1].x, 0.0001f)
        assertEquals(50f, mapped[1].y, 0.0001f)
        assertEquals(200f, mapped[2].x, 0.0001f)
        assertEquals(0f, mapped[2].y, 0.0001f)
    }

    @Test
    fun pieGeometry_distributesAnglesByValue() {
        val datasets = listOf(
            ChartDataset("a", "A", 0xFFFFFFFF, listOf(DataPoint(0L, 1f))),
            ChartDataset("b", "B", 0xFFFFFFFF, listOf(DataPoint(0L, 3f)))
        )

        val segments = ChartGeometry.pieSegments(datasets)

        assertEquals(2, segments.size)
        assertEquals(-90f, segments[0].startAngle, 0.0001f)
        assertEquals(90f, segments[0].sweepAngle, 0.0001f)
        assertEquals(0f, segments[1].startAngle, 0.0001f)
        assertEquals(270f, segments[1].sweepAngle, 0.0001f)
        assertTrue(segments.sumOf { it.sweepAngle.toDouble() } in 359.99..360.01)
    }
}
