package com.visualtasker.chartgraph.demo.data

typealias ChartId = String

sealed interface ChartSpec {
    val id: ChartId
    val title: String?
}

data class ChartPoint(
    val x: Double,
    val y: Double
)

data class LineSeries(
    val id: String,
    val label: String,
    val points: List<ChartPoint>
)

data class BarEntry(
    val id: String,
    val label: String,
    val value: Double
)

data class PieSlice(
    val id: String,
    val label: String,
    val value: Double
)

data class OhlcPoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double? = null
)

data class LineChartSpec(
    override val id: ChartId,
    override val title: String?,
    val series: List<LineSeries>
) : ChartSpec

data class BarChartSpec(
    override val id: ChartId,
    override val title: String?,
    val entries: List<BarEntry>
) : ChartSpec

data class PieChartSpec(
    override val id: ChartId,
    override val title: String?,
    val slices: List<PieSlice>,
    val donut: Boolean = false
) : ChartSpec

data class CandlestickChartSpec(
    override val id: ChartId,
    override val title: String?,
    val points: List<OhlcPoint>
) : ChartSpec
