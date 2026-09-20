package com.visualtasker.chartgraph.domain

import kotlin.math.ceil
import kotlin.math.floor

data class HistogramBin(
    val start: Double,
    val end: Double,
    val count: Int,
)

object ChartStatistics {
    fun histogram(values: List<Double>, requestedBinCount: Int): List<HistogramBin> {
        val finiteValues = values.filter(Double::isFinite)
        if (finiteValues.isEmpty()) return emptyList()

        val binCount = requestedBinCount.coerceIn(1, 100)
        val minimum = finiteValues.min()
        val maximum = finiteValues.max()
        if (minimum == maximum) {
            return listOf(HistogramBin(minimum, maximum, finiteValues.size))
        }

        val width = (maximum - minimum) / binCount
        val counts = IntArray(binCount)
        finiteValues.forEach { value ->
            val index = floor((value - minimum) / width).toInt().coerceIn(0, binCount - 1)
            counts[index] += 1
        }
        return counts.mapIndexed { index, count ->
            HistogramBin(
                start = minimum + index * width,
                end = if (index == binCount - 1) maximum else minimum + (index + 1) * width,
                count = count,
            )
        }
    }

    fun boxPlot(label: String, values: List<Double>): BoxPlotPoint? {
        val sorted = values.filter(Double::isFinite).sorted()
        if (sorted.isEmpty()) return null

        val lowerQuartile = percentile(sorted, 0.25)
        val median = percentile(sorted, 0.5)
        val upperQuartile = percentile(sorted, 0.75)
        val interquartileRange = upperQuartile - lowerQuartile
        val lowerFence = lowerQuartile - 1.5 * interquartileRange
        val upperFence = upperQuartile + 1.5 * interquartileRange
        val inliers = sorted.filter { it in lowerFence..upperFence }

        return BoxPlotPoint(
            label = label,
            minimum = inliers.firstOrNull() ?: sorted.first(),
            lowerQuartile = lowerQuartile,
            median = median,
            upperQuartile = upperQuartile,
            maximum = inliers.lastOrNull() ?: sorted.last(),
            outliers = sorted.filterNot { it in lowerFence..upperFence },
        )
    }

    private fun percentile(sorted: List<Double>, percentile: Double): Double {
        if (sorted.size == 1) return sorted.first()
        val rank = percentile.coerceIn(0.0, 1.0) * (sorted.lastIndex)
        val lower = floor(rank).toInt()
        val upper = ceil(rank).toInt()
        if (lower == upper) return sorted[lower]
        val fraction = rank - lower
        return sorted[lower] + (sorted[upper] - sorted[lower]) * fraction
    }
}
