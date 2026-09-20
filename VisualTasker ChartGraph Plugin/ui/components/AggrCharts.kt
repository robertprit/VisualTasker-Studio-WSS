package com.visualtasker.chartgraph.demo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.visualtasker.chartgraph.compose.ChartGraph
import com.visualtasker.chartgraph.demo.data.CandleDataPoint
import com.visualtasker.chartgraph.demo.data.ChartDataset
import com.visualtasker.chartgraph.demo.data.ChartType
import com.visualtasker.chartgraph.domain.BubblePoint
import com.visualtasker.chartgraph.domain.CandlePoint
import com.visualtasker.chartgraph.domain.ChartDocument
import com.visualtasker.chartgraph.domain.ChartKind
import com.visualtasker.chartgraph.domain.ChartPoint
import com.visualtasker.chartgraph.domain.ChartSeries
import com.visualtasker.chartgraph.domain.ChartStatistics
import com.visualtasker.chartgraph.domain.DiagramEdge
import com.visualtasker.chartgraph.domain.DiagramNode
import com.visualtasker.chartgraph.domain.GanttTask
import com.visualtasker.chartgraph.domain.HeatmapCell
import com.visualtasker.chartgraph.domain.MosaicCell
import com.visualtasker.chartgraph.domain.RadarAxis
import com.visualtasker.chartgraph.domain.RadarSeries
import com.visualtasker.chartgraph.domain.VennOverlap
import com.visualtasker.chartgraph.domain.VennSet

@Composable
fun AggrChart(
    type: ChartType,
    datasets: List<ChartDataset>,
    candleData: List<CandleDataPoint> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val document = remember(type, datasets, candleData) {
        val series = datasets.map { dataset ->
            ChartSeries(
                id = dataset.id,
                label = dataset.label,
                colorArgb = dataset.color,
                points = dataset.points.map { ChartPoint(it.x.toDouble(), it.y.toDouble()) },
            )
        }
        val flattened = datasets.flatMap { it.points }
        val diagramNodes = flattened.take(12).mapIndexed { index, point ->
            DiagramNode(
                id = "node-$index",
                label = "${point.y.toInt()}",
                level = index / 4,
                colorArgb = datasets.getOrNull(index % datasets.size.coerceAtLeast(1))?.color ?: 0xFF5BE7C4,
            )
        }
        val vennSets = List(4) { index ->
            val dataset = datasets.getOrNull(index % datasets.size.coerceAtLeast(1))
            VennSet(
                id = "set-$index",
                label = dataset?.label?.let { "$it ${index + 1}" } ?: "Set ${index + 1}",
                value = kotlin.math.abs(dataset?.points?.getOrNull(index)?.y?.toDouble() ?: (index + 1.0)).coerceAtLeast(1.0),
                colorArgb = listOf(0xFFE57373, 0xFFAED581, 0xFF9575CD, 0xFF4DD0E1)[index],
            )
        }
        val commonOverlap = vennSets.minOf(VennSet::value) * 0.45
        ChartDocument(
            id = "chartgraph-demo-${type.name.lowercase()}",
            title = type.label,
            kind = type.toChartKind(),
            series = series,
            candles = candleData.map { CandlePoint(it.x.toDouble(), it.open.toDouble(), it.high.toDouble(), it.low.toDouble(), it.close.toDouble()) },
            boxPlots = series.flatMap { chart ->
                chart.points.chunked(6).take(5).mapIndexedNotNull { index, chunk ->
                    ChartStatistics.boxPlot("${chart.label} ${index + 1}", chunk.map(ChartPoint::y))
                }
            },
            heatmapCells = datasets.flatMapIndexed { row, dataset ->
                dataset.points.take(16).mapIndexed { column, point -> HeatmapCell(column, row, point.y.toDouble()) }
            },
            bubbles = flattened.take(24).mapIndexed { index, point ->
                BubblePoint(index.toDouble(), point.y.toDouble(), kotlin.math.abs(point.y.toDouble()), colorArgb = datasets.getOrNull(index % datasets.size.coerceAtLeast(1))?.color)
            },
            vennSets = vennSets,
            vennOverlaps = listOf(
                VennOverlap(vennSets.mapTo(linkedSetOf(), VennSet::id), commonOverlap, "Gemeinsam"),
            ),
            mosaicCells = datasets.flatMap { dataset ->
                dataset.points.take(6).mapIndexed { index, point -> MosaicCell(dataset.label, "$index", kotlin.math.abs(point.y.toDouble()), dataset.color) }
            },
            gaugeValue = flattened.lastOrNull()?.y?.toDouble(),
            gaugeMinimum = flattened.minOfOrNull { it.y }?.toDouble() ?: 0.0,
            gaugeMaximum = flattened.maxOfOrNull { it.y }?.toDouble() ?: 100.0,
            ganttTasks = flattened.take(8).mapIndexed { index, point ->
                GanttTask(
                    id = "task-$index",
                    label = "Task ${index + 1}",
                    start = index.toDouble(),
                    end = index + 1.0 + kotlin.math.abs(point.y % 3),
                    progress = (point.y / 100f).toDouble().coerceIn(0.0, 1.0),
                    colorArgb = datasets.getOrNull(index % datasets.size.coerceAtLeast(1))?.color ?: 0xFF5BE7C4,
                )
            },
            radarSeries = series.take(3).mapIndexed { seriesIndex, chart ->
                RadarSeries(
                    id = chart.id,
                    label = chart.label,
                    colorArgb = chart.colorArgb,
                    axes = chart.points.take(7).mapIndexed { index, point -> RadarAxis("A${index + 1}", kotlin.math.abs(point.y), 100.0) },
                )
            },
            diagramNodes = diagramNodes,
            diagramEdges = diagramNodes.zipWithNext { first, second -> DiagramEdge(first.id, second.id) },
            xAxisLabel = "Zeit / Kategorie",
            yAxisLabel = "Wert",
        )
    }
    ChartGraph(document = document, modifier = modifier)
}

val ChartType.label: String
    get() = when (this) {
        ChartType.LINE -> "Kurve"
        ChartType.COLUMN -> "Balken"
        ChartType.PIE -> "Torte"
        ChartType.DONUT -> "Donut"
        ChartType.CANDLE -> "Kerzen"
        ChartType.HISTOGRAM -> "Histogramm"
        ChartType.BOX_PLOT -> "Boxplot"
        ChartType.SCATTER -> "Streuung"
        ChartType.HEATMAP -> "Heatmap"
        ChartType.VENN_LINEAR -> "Venn linear"
        ChartType.VENN_STACKED -> "Venn gestapelt"
        ChartType.VENN_RADIAL -> "Venn radial"
        ChartType.VENN_GROUP -> "Venn Gruppe"
        ChartType.AREA -> "Fläche"
        ChartType.BUBBLE -> "Blasen"
        ChartType.MOSAIC -> "Mosaik"
        ChartType.GAUGE -> "Gauge"
        ChartType.GANTT -> "Gantt"
        ChartType.RADAR -> "Radar"
        ChartType.WATERFALL -> "Wasserfall"
        ChartType.FUNNEL -> "Trichter"
        ChartType.PARETO -> "Pareto"
        ChartType.PICTOGRAPH -> "Piktogramm"
        ChartType.DIAGRAM -> "Mindmap / Flowchart"
    }

private fun ChartType.toChartKind(): ChartKind = when (this) {
    ChartType.LINE -> ChartKind.LINE
    ChartType.COLUMN -> ChartKind.BAR
    else -> ChartKind.valueOf(name)
}
