package com.visualtasker.chartgraph.demo.data

interface ChartScale<D> {
    fun toPixel(value: D): Float
}

data class LinearScale(
    val domainMin: Double,
    val domainMax: Double,
    val rangeMin: Float,
    val rangeMax: Float,
    val clamp: Boolean = true
) : ChartScale<Double> {
    override fun toPixel(value: Double): Float {
        val domainSpan = (domainMax - domainMin).takeIf { it != 0.0 } ?: 1.0
        val raw = ((value - domainMin) / domainSpan).toFloat()
        val normalized = if (clamp) raw.coerceIn(0f, 1f) else raw
        return rangeMin + normalized * (rangeMax - rangeMin)
    }
}

data class TimeScale(
    val domainStart: Long,
    val domainEnd: Long,
    val rangeMin: Float,
    val rangeMax: Float,
    val clamp: Boolean = true
) : ChartScale<Long> {
    override fun toPixel(value: Long): Float {
        val base = LinearScale(
            domainMin = domainStart.toDouble(),
            domainMax = domainEnd.toDouble(),
            rangeMin = rangeMin,
            rangeMax = rangeMax,
            clamp = clamp
        )
        return base.toPixel(value.toDouble())
    }
}

data class CategoryScale(
    val categories: List<String>,
    val rangeMin: Float,
    val rangeMax: Float
) : ChartScale<String> {
    override fun toPixel(value: String): Float {
        if (categories.isEmpty()) return rangeMin
        val index = categories.indexOf(value).coerceAtLeast(0)
        val last = (categories.size - 1).coerceAtLeast(1)
        val ratio = index.toFloat() / last.toFloat()
        return rangeMin + ratio * (rangeMax - rangeMin)
    }
}
