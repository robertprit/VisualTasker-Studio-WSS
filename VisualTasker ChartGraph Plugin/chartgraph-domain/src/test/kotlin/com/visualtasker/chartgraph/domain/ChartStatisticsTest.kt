package com.visualtasker.chartgraph.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartStatisticsTest {
    @Test
    fun histogram_assignsMaximumToLastBin() {
        val bins = ChartStatistics.histogram(listOf(0.0, 1.0, 2.0, 3.0, 4.0), 2)

        assertEquals(listOf(2, 3), bins.map(HistogramBin::count))
        assertEquals(4.0, bins.last().end)
    }

    @Test
    fun boxPlot_separatesOutliersUsingInterquartileRange() {
        val plot = ChartStatistics.boxPlot("runtime", listOf(1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 30.0))

        requireNotNull(plot)
        assertEquals(1.0, plot.minimum)
        assertEquals(4.0, plot.maximum)
        assertEquals(listOf(30.0), plot.outliers)
    }
}
