package com.visualtasker.chartgraph.demo.data

enum class ChartType {
    LINE, COLUMN, PIE, DONUT, CANDLE, HISTOGRAM, BOX_PLOT, SCATTER, HEATMAP,
    VENN_LINEAR, VENN_STACKED, VENN_RADIAL, VENN_GROUP, AREA, BUBBLE, MOSAIC,
    GAUGE, GANTT, RADAR, WATERFALL, FUNNEL, PARETO, PICTOGRAPH, DIAGRAM
}

enum class MarketInterval(val millis: Long, val label: String) {
    SEC_5(5_000L, "5s"),
    SEC_15(15_000L, "15s"),
    SEC_30(30_000L, "30s"),
    MIN_1(60_000L, "1m"),
    MIN_5(5 * 60_000L, "5m"),
    MIN_15(15 * 60_000L, "15m"),
    HOUR_1(60 * 60_000L, "1h"),
    HOUR_4(4 * 60 * 60_000L, "4h"),
    DAY_1(24 * 60 * 60_000L, "1d"),
    WEEK_1(7 * 24 * 60 * 60_000L, "1w"),
    MONTH_1(30L * 24 * 60 * 60_000L, "1M")
}

enum class MarketRangePreset(val label: String, val durationMillis: Long?) {
    HOUR_1("1H", 60 * 60_000L),
    DAY_1("1D", 24 * 60 * 60_000L),
    WEEK_1("1W", 7 * 24 * 60 * 60_000L),
    MONTH_1("1M", 30L * 24 * 60 * 60_000L),
    MONTH_3("3M", 90L * 24 * 60 * 60_000L),
    YEAR_1("1Y", 365L * 24 * 60 * 60_000L),
    MAX("MAX", null),
    CUSTOM("CUSTOM", null)
}

data class MarketTimeRange(
    val startMillis: Long,
    val endMillis: Long
)

enum class MarketDataMode {
    SYNTHETIC,
    LIVE
}

data class DataPoint(
    val x: Long, // Zeitstempel oder Kategorie
    val y: Float // Wert
)

data class ChartDataset(
    val id: String,
    val label: String,
    val color: Long, // ARGB Color
    val points: List<DataPoint> = emptyList()
)

data class CandleDataPoint(
    val x: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float
)
