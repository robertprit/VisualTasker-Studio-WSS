package com.visualtasker.chartgraph.domain

enum class ChartKind {
    LINE,
    BAR,
    PIE,
    DONUT,
    CANDLE
}

data class ChartPoint(
    val x: Double,
    val y: Double,
)

data class ChartSeries(
    val id: String,
    val label: String,
    val colorArgb: Long,
    val points: List<ChartPoint>,
)

data class CandlePoint(
    val x: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
)

data class ChartDocument(
    val id: String,
    val title: String,
    val kind: ChartKind,
    val series: List<ChartSeries> = emptyList(),
    val candles: List<CandlePoint> = emptyList(),
)
