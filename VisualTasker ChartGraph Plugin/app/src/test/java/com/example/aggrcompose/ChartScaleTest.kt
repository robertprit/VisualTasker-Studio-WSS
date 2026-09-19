package com.visualtasker.chartgraph.demo

import com.visualtasker.chartgraph.demo.data.LinearScale
import com.visualtasker.chartgraph.demo.data.TimeScale
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartScaleTest {

    @Test
    fun linearScale_mapsMinMaxAndMidpoint() {
        val scale = LinearScale(
            domainMin = 0.0,
            domainMax = 100.0,
            rangeMin = 0f,
            rangeMax = 200f
        )

        assertEquals(0f, scale.toPixel(0.0), 0.0001f)
        assertEquals(200f, scale.toPixel(100.0), 0.0001f)
        assertEquals(100f, scale.toPixel(50.0), 0.0001f)
    }

    @Test
    fun timeScale_mapsBoundariesAndMidpoint() {
        val start = 1_000L
        val end = 5_000L
        val scale = TimeScale(
            domainStart = start,
            domainEnd = end,
            rangeMin = 10f,
            rangeMax = 110f
        )

        assertEquals(10f, scale.toPixel(start), 0.0001f)
        assertEquals(110f, scale.toPixel(end), 0.0001f)
        assertEquals(60f, scale.toPixel(3_000L), 0.0001f)
    }
}
