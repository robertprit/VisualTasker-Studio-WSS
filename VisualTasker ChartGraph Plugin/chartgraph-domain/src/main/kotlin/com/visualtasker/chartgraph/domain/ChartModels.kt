package com.visualtasker.chartgraph.domain

enum class ChartKind {
    LINE,
    BAR,
    PIE,
    DONUT,
    CANDLE,
    HISTOGRAM,
    BOX_PLOT,
    SCATTER,
    HEATMAP,
    VENN_LINEAR,
    VENN_STACKED,
    VENN_RADIAL,
    VENN_GROUP,
    AREA,
    BUBBLE,
    MOSAIC,
    GAUGE,
    GANTT,
    RADAR,
    WATERFALL,
    FUNNEL,
    PARETO,
    PICTOGRAPH,
    DIAGRAM,
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

data class BoxPlotPoint(
    val label: String,
    val minimum: Double,
    val lowerQuartile: Double,
    val median: Double,
    val upperQuartile: Double,
    val maximum: Double,
    val outliers: List<Double> = emptyList(),
)

data class HeatmapCell(
    val column: Int,
    val row: Int,
    val value: Double,
    val label: String? = null,
)

data class BubblePoint(
    val x: Double,
    val y: Double,
    val magnitude: Double,
    val label: String? = null,
    val colorArgb: Long? = null,
)

data class VennSet(
    val id: String,
    val label: String,
    val value: Double,
    val colorArgb: Long,
)

data class VennOverlap(
    val setIds: Set<String>,
    val value: Double,
    val label: String? = null,
)

data class MosaicCell(
    val group: String,
    val category: String,
    val value: Double,
    val colorArgb: Long,
)

data class GanttTask(
    val id: String,
    val label: String,
    val start: Double,
    val end: Double,
    val progress: Double = 0.0,
    val colorArgb: Long,
)

data class RadarAxis(
    val label: String,
    val value: Double,
    val maximum: Double = 1.0,
)

data class RadarSeries(
    val id: String,
    val label: String,
    val colorArgb: Long,
    val axes: List<RadarAxis>,
)

data class DiagramNode(
    val id: String,
    val label: String,
    val level: Int = 0,
    val colorArgb: Long = 0xFF5BE7C4,
)

data class DiagramEdge(
    val fromId: String,
    val toId: String,
    val label: String? = null,
)

data class ChartDocument(
    val id: String,
    val title: String,
    val kind: ChartKind,
    val series: List<ChartSeries> = emptyList(),
    val candles: List<CandlePoint> = emptyList(),
    val boxPlots: List<BoxPlotPoint> = emptyList(),
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val histogramBinCount: Int = 10,
    val bubbles: List<BubblePoint> = emptyList(),
    val vennSets: List<VennSet> = emptyList(),
    val vennOverlaps: List<VennOverlap> = emptyList(),
    val mosaicCells: List<MosaicCell> = emptyList(),
    val gaugeValue: Double? = null,
    val gaugeMinimum: Double = 0.0,
    val gaugeMaximum: Double = 100.0,
    val ganttTasks: List<GanttTask> = emptyList(),
    val radarSeries: List<RadarSeries> = emptyList(),
    val diagramNodes: List<DiagramNode> = emptyList(),
    val diagramEdges: List<DiagramEdge> = emptyList(),
    val xAxisLabel: String? = null,
    val yAxisLabel: String? = null,
    val showLegend: Boolean = true,
)
