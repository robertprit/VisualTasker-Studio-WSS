package com.visualtasker.chartgraph.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.visualtasker.chartgraph.domain.BoxPlotPoint
import com.visualtasker.chartgraph.domain.ChartDocument
import com.visualtasker.chartgraph.domain.ChartKind
import com.visualtasker.chartgraph.domain.ChartStatistics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun ChartGraph(
    document: ChartDocument,
    modifier: Modifier = Modifier,
) {
    if (!document.hasRenderableData()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Keine Chart-Daten", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var touchPosition by remember(document.id, document.kind) { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(document) {
                        detectTapGestures { position -> touchPosition = position }
                    },
            ) {
                if (document.kind.hasCartesianAxes()) drawAxes()
                when (document.kind) {
                    ChartKind.CANDLE -> drawCandles(document)
                    ChartKind.PIE, ChartKind.DONUT -> drawPie(document)
                    ChartKind.LINE, ChartKind.BAR -> drawCartesian(document)
                    ChartKind.HISTOGRAM -> drawHistogram(document)
                    ChartKind.BOX_PLOT -> drawBoxPlots(document)
                    ChartKind.SCATTER -> drawScatter(document)
                    ChartKind.HEATMAP -> drawHeatmap(document)
                    ChartKind.AREA -> drawArea(document)
                    ChartKind.BUBBLE -> drawBubbles(document)
                    ChartKind.VENN_LINEAR,
                    ChartKind.VENN_STACKED,
                    ChartKind.VENN_RADIAL,
                    ChartKind.VENN_GROUP -> drawVenn(document)
                    ChartKind.MOSAIC -> drawMosaic(document)
                    ChartKind.GAUGE -> drawGauge(document)
                    ChartKind.GANTT -> drawGantt(document)
                    ChartKind.RADAR -> drawRadar(document)
                    ChartKind.WATERFALL -> drawWaterfall(document)
                    ChartKind.FUNNEL -> drawFunnel(document)
                    ChartKind.PARETO -> drawPareto(document)
                    ChartKind.PICTOGRAPH -> drawPictograph(document)
                    ChartKind.DIAGRAM -> drawDiagram(document)
                }
            }
            touchPosition?.let { position ->
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.94f),
                    shape = MaterialTheme.shapes.small,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = document.tooltipText(position, canvasSize),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            document.xAxisLabel?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            document.yAxisLabel?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (document.showLegend) ChartLegend(document)
    }
}

@Composable
private fun ChartLegend(document: ChartDocument) {
    val entries = when {
        document.series.isNotEmpty() -> document.series.map { it.label to Color(it.colorArgb) }
        document.vennSets.isNotEmpty() -> document.vennSets.map { it.label to Color(it.colorArgb) }
        document.ganttTasks.isNotEmpty() -> document.ganttTasks.map { it.label to Color(it.colorArgb) }
        else -> emptyList()
    }.take(6)
    if (entries.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        entries.forEach { (label, color) ->
            Surface(modifier = Modifier.size(8.dp), color = color, shape = CircleShape) {}
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
        }
    }
}

private fun ChartDocument.tooltipText(position: Offset, size: IntSize): String {
    val xFraction = if (size.width > 0) (position.x / size.width).coerceIn(0f, 1f) else 0f
    val yFraction = if (size.height > 0) (1f - position.y / size.height).coerceIn(0f, 1f) else 0f
    val points = series.flatMap { series -> series.points.map { series.label to it } }
    if (points.isNotEmpty() && kind.hasCartesianAxes()) {
        val minX = points.minOf { it.second.x }
        val maxX = points.maxOf { it.second.x }
        val targetX = minX + (maxX - minX) * xFraction
        val nearest = points.minByOrNull { (_, point) -> kotlin.math.abs(point.x - targetX) }
        if (nearest != null) return "${nearest.first}: x=${"%.2f".format(nearest.second.x)}, y=${"%.2f".format(nearest.second.y)}"
    }
    return "${kind.name.lowercase().replace('_', ' ')}  ${"%.0f".format(xFraction * 100)}% / ${"%.0f".format(yFraction * 100)}%"
}

private fun ChartKind.hasCartesianAxes(): Boolean = this in setOf(
    ChartKind.LINE,
    ChartKind.BAR,
    ChartKind.CANDLE,
    ChartKind.HISTOGRAM,
    ChartKind.BOX_PLOT,
    ChartKind.SCATTER,
    ChartKind.AREA,
    ChartKind.BUBBLE,
    ChartKind.GANTT,
    ChartKind.WATERFALL,
    ChartKind.PARETO,
)

private fun ChartDocument.hasRenderableData(): Boolean = when (kind) {
    ChartKind.CANDLE -> candles.isNotEmpty()
    ChartKind.BOX_PLOT -> boxPlots.isNotEmpty() || series.any { it.points.isNotEmpty() }
    ChartKind.HEATMAP -> heatmapCells.isNotEmpty()
    ChartKind.BUBBLE -> bubbles.isNotEmpty()
    ChartKind.VENN_LINEAR,
    ChartKind.VENN_STACKED,
    ChartKind.VENN_RADIAL,
    ChartKind.VENN_GROUP -> vennSets.isNotEmpty()
    ChartKind.MOSAIC -> mosaicCells.isNotEmpty()
    ChartKind.GAUGE -> gaugeValue != null
    ChartKind.GANTT -> ganttTasks.isNotEmpty()
    ChartKind.RADAR -> radarSeries.any { it.axes.size >= 3 }
    ChartKind.DIAGRAM -> diagramNodes.isNotEmpty()
    else -> series.any { it.points.isNotEmpty() }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxes() {
    val axisColor = Color.White.copy(alpha = 0.22f)
    repeat(5) { index ->
        val y = size.height * index / 4f
        drawLine(axisColor.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), 1f)
    }
    drawLine(axisColor, Offset(0f, 0f), Offset(0f, size.height), 2f)
    drawLine(axisColor, Offset(0f, size.height), Offset(size.width, size.height), 2f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCartesian(document: ChartDocument) {
    val values = document.series.flatMap { series -> series.points.map { it.y } }
    val minY = values.minOrNull() ?: 0.0
    val maxY = values.maxOrNull() ?: 1.0
    val range = max(0.0001, maxY - minY)
    val maxPoints = max(2, document.series.maxOfOrNull { it.points.size } ?: 2)

    document.series.forEachIndexed { seriesIndex, series ->
        val color = Color(series.colorArgb)
        when (document.kind) {
            ChartKind.LINE -> series.points.zipWithNext().forEachIndexed { index, pair ->
                val start = pair.first
                val end = pair.second
                drawLine(
                    color = color,
                    start = Offset(index.toFloat() / (maxPoints - 1) * size.width, size.height - ((start.y - minY) / range).toFloat() * size.height),
                    end = Offset((index + 1).toFloat() / (maxPoints - 1) * size.width, size.height - ((end.y - minY) / range).toFloat() * size.height),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                )
            }
            ChartKind.BAR -> {
                val slot = size.width / maxPoints
                val barWidth = slot / max(1, document.series.size) * 0.72f
                series.points.forEachIndexed { index, point ->
                    val height = ((point.y - minY) / range).toFloat().coerceIn(0f, 1f) * size.height
                    drawRect(
                        color = color.copy(alpha = 0.85f),
                        topLeft = Offset(index * slot + seriesIndex * barWidth, size.height - height),
                        size = Size(barWidth, height),
                    )
                }
            }
            else -> Unit
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScatter(document: ChartDocument) {
    val points = document.series.flatMap { it.points }
    val minX = points.minOfOrNull { it.x } ?: return
    val maxX = points.maxOfOrNull { it.x } ?: return
    val minY = points.minOfOrNull { it.y } ?: return
    val maxY = points.maxOfOrNull { it.y } ?: return
    val rangeX = max(0.0001, maxX - minX)
    val rangeY = max(0.0001, maxY - minY)
    val radius = (minOf(size.width, size.height) * 0.018f).coerceIn(5f, 13f)

    document.series.forEach { series ->
        val color = Color(series.colorArgb)
        series.points.forEach { point ->
            val x = ((point.x - minX) / rangeX).toFloat() * size.width
            val y = size.height - ((point.y - minY) / rangeY).toFloat() * size.height
            drawCircle(color.copy(alpha = 0.28f), radius * 1.65f, Offset(x, y))
            drawCircle(color, radius, Offset(x, y))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHistogram(document: ChartDocument) {
    val values = document.series.flatMap { series -> series.points.map { it.y } }
    val bins = ChartStatistics.histogram(values, document.histogramBinCount)
    val maximumCount = bins.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: return
    val slotWidth = size.width / bins.size
    val color = document.series.firstOrNull()?.let { Color(it.colorArgb) } ?: Color(0xFF5BE7C4)

    bins.forEachIndexed { index, bin ->
        val height = bin.count.toFloat() / maximumCount * size.height
        val gap = (slotWidth * 0.08f).coerceAtMost(6f)
        drawRect(
            color = color.copy(alpha = 0.84f),
            topLeft = Offset(index * slotWidth + gap / 2f, size.height - height),
            size = Size((slotWidth - gap).coerceAtLeast(1f), height),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBoxPlots(document: ChartDocument) {
    val plots = document.boxPlots.ifEmpty {
        document.series.mapNotNull { series ->
            ChartStatistics.boxPlot(series.label, series.points.map { it.y })
        }
    }
    val values = plots.flatMap { plot ->
        listOf(plot.minimum, plot.maximum) + plot.outliers
    }
    val minimum = values.minOrNull() ?: return
    val maximum = values.maxOrNull() ?: return
    val range = max(0.0001, maximum - minimum)
    val slotWidth = size.width / plots.size
    fun y(value: Double) = size.height - ((value - minimum) / range).toFloat() * size.height

    plots.forEachIndexed { index, plot ->
        drawBoxPlot(
            plot = plot,
            centerX = (index + 0.5f) * slotWidth,
            boxWidth = slotWidth * 0.5f,
            color = document.series.getOrNull(index)?.let { Color(it.colorArgb) } ?: Color(0xFF8F7CFF),
            y = ::y,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBoxPlot(
    plot: BoxPlotPoint,
    centerX: Float,
    boxWidth: Float,
    color: Color,
    y: (Double) -> Float,
) {
    val capWidth = boxWidth * 0.58f
    drawLine(color, Offset(centerX, y(plot.maximum)), Offset(centerX, y(plot.minimum)), 3f)
    drawLine(color, Offset(centerX - capWidth / 2f, y(plot.maximum)), Offset(centerX + capWidth / 2f, y(plot.maximum)), 3f)
    drawLine(color, Offset(centerX - capWidth / 2f, y(plot.minimum)), Offset(centerX + capWidth / 2f, y(plot.minimum)), 3f)
    val boxTop = y(plot.upperQuartile)
    val boxBottom = y(plot.lowerQuartile)
    drawRect(
        color = color.copy(alpha = 0.28f),
        topLeft = Offset(centerX - boxWidth / 2f, boxTop),
        size = Size(boxWidth, (boxBottom - boxTop).coerceAtLeast(2f)),
    )
    drawRect(
        color = color,
        topLeft = Offset(centerX - boxWidth / 2f, boxTop),
        size = Size(boxWidth, (boxBottom - boxTop).coerceAtLeast(2f)),
        style = Stroke(width = 3f),
    )
    drawLine(color, Offset(centerX - boxWidth / 2f, y(plot.median)), Offset(centerX + boxWidth / 2f, y(plot.median)), 4f)
    plot.outliers.forEach { value ->
        drawCircle(color, radius = 5f, center = Offset(centerX, y(value)), style = Stroke(width = 2f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeatmap(document: ChartDocument) {
    val columns = (document.heatmapCells.maxOfOrNull { it.column } ?: return) + 1
    val rows = (document.heatmapCells.maxOfOrNull { it.row } ?: return) + 1
    val minimum = document.heatmapCells.minOf { it.value }
    val maximum = document.heatmapCells.maxOf { it.value }
    val range = max(0.0001, maximum - minimum)
    val cellWidth = size.width / columns
    val cellHeight = size.height / rows
    val lowColor = Color(0xFF251C4C)
    val highColor = Color(0xFFFFC857)

    document.heatmapCells.forEach { cell ->
        val intensity = ((cell.value - minimum) / range).toFloat().coerceIn(0f, 1f)
        val gap = minOf(cellWidth, cellHeight) * 0.06f
        drawRect(
            color = lerp(lowColor, highColor, intensity),
            topLeft = Offset(cell.column * cellWidth + gap / 2f, cell.row * cellHeight + gap / 2f),
            size = Size((cellWidth - gap).coerceAtLeast(1f), (cellHeight - gap).coerceAtLeast(1f)),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPie(document: ChartDocument) {
    val slices = document.series.flatMap { it.points.map { point -> it.colorArgb to point.y.coerceAtLeast(0.0) } }
    val total = slices.sumOf { it.second }.takeIf { it > 0.0 } ?: return
    val diameter = minOf(size.width, size.height) * 0.8f
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    var start = -90f
    slices.forEach { (color, value) ->
        val sweep = (value / total * 360.0).toFloat()
        drawArc(
            color = Color(color),
            startAngle = start,
            sweepAngle = sweep,
            useCenter = document.kind == ChartKind.PIE,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = if (document.kind == ChartKind.DONUT) Stroke(diameter * 0.22f) else androidx.compose.ui.graphics.drawscope.Fill,
        )
        start += sweep
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCandles(document: ChartDocument) {
    val low = document.candles.minOfOrNull { it.low } ?: return
    val high = document.candles.maxOfOrNull { it.high } ?: return
    val range = max(0.0001, high - low)
    val slot = size.width / max(1, document.candles.size)
    fun y(value: Double) = size.height - ((value - low) / range).toFloat() * size.height
    document.candles.forEachIndexed { index, candle ->
        val x = (index + 0.5f) * slot
        val color = if (candle.close >= candle.open) Color(0xFF39D98A) else Color(0xFFFF6B7A)
        drawLine(color, Offset(x, y(candle.high)), Offset(x, y(candle.low)), 3f)
        val top = minOf(y(candle.open), y(candle.close))
        val bottom = maxOf(y(candle.open), y(candle.close))
        drawRect(color, Offset(x - slot * 0.28f, top), Size(slot * 0.56f, (bottom - top).coerceAtLeast(3f)))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArea(document: ChartDocument) {
    val values = document.series.flatMap { it.points.map { point -> point.y } }
    val minimum = minOf(0.0, values.minOrNull() ?: return)
    val maximum = values.maxOrNull() ?: return
    val range = max(0.0001, maximum - minimum)
    document.series.forEach { series ->
        if (series.points.isEmpty()) return@forEach
        val maxX = series.points.maxOf { it.x }
        val minX = series.points.minOf { it.x }
        val rangeX = max(0.0001, maxX - minX)
        val path = Path()
        path.moveTo(0f, size.height)
        series.points.forEach { point ->
            path.lineTo(
                ((point.x - minX) / rangeX).toFloat() * size.width,
                size.height - ((point.y - minimum) / range).toFloat() * size.height,
            )
        }
        path.lineTo(size.width, size.height)
        path.close()
        val color = Color(series.colorArgb)
        drawPath(path, color.copy(alpha = 0.3f))
        drawPath(path, color, style = Stroke(width = 3f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBubbles(document: ChartDocument) {
    val minX = document.bubbles.minOfOrNull { it.x } ?: return
    val maxX = document.bubbles.maxOfOrNull { it.x } ?: return
    val minY = document.bubbles.minOfOrNull { it.y } ?: return
    val maxY = document.bubbles.maxOfOrNull { it.y } ?: return
    val maxMagnitude = document.bubbles.maxOfOrNull { it.magnitude }?.coerceAtLeast(0.0001) ?: return
    val rangeX = max(0.0001, maxX - minX)
    val rangeY = max(0.0001, maxY - minY)
    document.bubbles.forEachIndexed { index, bubble ->
        val radius = (8f + (bubble.magnitude / maxMagnitude).toFloat() * minOf(size.width, size.height) * 0.1f)
        val center = Offset(
            ((bubble.x - minX) / rangeX).toFloat() * size.width,
            size.height - ((bubble.y - minY) / rangeY).toFloat() * size.height,
        )
        val color = bubble.colorArgb?.let(::Color)
            ?: document.series.getOrNull(index % document.series.size.coerceAtLeast(1))?.let { Color(it.colorArgb) }
            ?: Color(0xFF5BE7C4)
        drawCircle(color.copy(alpha = 0.3f), radius * 1.12f, center)
        drawCircle(color.copy(alpha = 0.78f), radius, center)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVenn(document: ChartDocument) {
    val sets = document.vennSets.take(5)
    if (sets.isEmpty()) return
    when (document.kind) {
        ChartKind.VENN_LINEAR -> {
            val radius = minOf(size.height * 0.3f, size.width / (sets.size * 1.35f + 0.65f))
            val step = radius * 1.35f
            val startX = (size.width - (step * (sets.size - 1) + radius * 2f)) / 2f + radius
            sets.forEachIndexed { index, set -> drawVennCircle(set.label, Color(set.colorArgb), Offset(startX + index * step, size.height / 2f), radius) }
        }
        ChartKind.VENN_STACKED -> {
            val maximumRadius = minOf(size.width, size.height) * 0.43f
            val bottom = size.height * 0.93f
            sets.forEachIndexed { index, set ->
                val radius = maximumRadius * (1f - index * 0.16f).coerceAtLeast(0.3f)
                drawVennCircle(set.label, Color(set.colorArgb), Offset(size.width / 2f, bottom - radius), radius)
            }
        }
        ChartKind.VENN_RADIAL -> {
            val center = Offset(size.width / 2f, size.height / 2f)
            val centralRadius = minOf(size.width, size.height) * 0.25f
            val satelliteRadius = centralRadius * 0.48f
            val orbit = centralRadius + satelliteRadius * 0.62f
            sets.firstOrNull()?.let { drawVennCircle(it.label, Color(it.colorArgb), center, centralRadius) }
            sets.drop(1).forEachIndexed { index, set ->
                val angle = (-PI / 2 + index * 2 * PI / (sets.size - 1).coerceAtLeast(1)).toFloat()
                drawVennCircle(set.label, Color(set.colorArgb), center + Offset(cos(angle), sin(angle)) * orbit, satelliteRadius)
            }
        }
        ChartKind.VENN_GROUP -> {
            val diameter = minOf(size.width, size.height)
            val commonCenter = Offset(size.width / 2f, size.height / 2f)
            val visibleIds = sets.mapTo(linkedSetOf()) { it.id }
            val commonOverlap = document.vennOverlaps
                .filter { it.setIds.containsAll(visibleIds) }
                .maxByOrNull { it.setIds.size }
                ?: document.vennOverlaps.maxByOrNull { it.setIds.size }
            val maximumSetValue = sets.maxOf { kotlin.math.abs(it.value) }.coerceAtLeast(0.0001)
            val maximumOrbit = diameter * 0.105f

            sets.forEachIndexed { index, set ->
                val setValue = kotlin.math.abs(set.value).coerceAtLeast(0.0001)
                val commonShare = ((commonOverlap?.value?.let { kotlin.math.abs(it) } ?: 0.0) / setValue)
                    .toFloat()
                    .coerceIn(0f, 1f)
                val angle = (-PI / 2 + index * 2 * PI / sets.size).toFloat()
                val direction = Offset(cos(angle), sin(angle))
                val distance = maximumOrbit * (1f - commonShare)
                val center = commonCenter + direction * distance
                val valueScale = (setValue / maximumSetValue).toFloat().coerceIn(0f, 1f)
                val radius = diameter * (0.235f + valueScale * 0.045f)
                drawVennCircle("", Color(set.colorArgb), center, radius)
                drawCanvasLabel(
                    set.label.take(16),
                    center + direction * radius * 0.52f + Offset(-radius * 0.2f, 5f),
                    Color.White,
                )
            }
            commonOverlap?.let { overlap ->
                drawCanvasLabel(
                    overlap.label ?: overlap.value.toInt().toString(),
                    commonCenter + Offset(-diameter * 0.065f, 5f),
                    Color.White,
                )
            }
        }
        else -> Unit
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVennCircle(
    label: String,
    color: Color,
    center: Offset,
    radius: Float,
) {
    drawCircle(color.copy(alpha = 0.42f), radius, center)
    drawCircle(color.copy(alpha = 0.9f), radius, center, style = Stroke(width = 3f))
    if (label.isNotBlank()) {
        drawCanvasLabel(label.take(16), center + Offset(-radius * 0.42f, 5f), Color.White)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMosaic(document: ChartDocument) {
    val groups = document.mosaicCells.groupBy { it.group }
    val grandTotal = document.mosaicCells.sumOf { it.value.coerceAtLeast(0.0) }.coerceAtLeast(0.0001)
    var left = 0f
    groups.values.forEach { cells ->
        val groupTotal = cells.sumOf { it.value.coerceAtLeast(0.0) }
        val width = (groupTotal / grandTotal).toFloat() * size.width
        var top = 0f
        cells.forEach { cell ->
            val height = if (groupTotal > 0.0) (cell.value / groupTotal).toFloat() * size.height else 0f
            drawRect(
                color = Color(cell.colorArgb).copy(alpha = 0.82f),
                topLeft = Offset(left + 2f, top + 2f),
                size = Size((width - 4f).coerceAtLeast(1f), (height - 4f).coerceAtLeast(1f)),
            )
            top += height
        }
        left += width
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGauge(document: ChartDocument) {
    val range = max(0.0001, document.gaugeMaximum - document.gaugeMinimum)
    val fraction = (((document.gaugeValue ?: return) - document.gaugeMinimum) / range).toFloat().coerceIn(0f, 1f)
    val diameter = minOf(size.width, size.height * 1.7f) * 0.78f
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val stroke = diameter * 0.11f
    drawArc(Color.White.copy(alpha = 0.15f), 150f, 240f, false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
    drawArc(Color(0xFF5BE7C4), 150f, 240f * fraction, false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
    val center = Offset(size.width / 2f, topLeft.y + diameter / 2f)
    val angle = (150f + 240f * fraction) / 180f * PI.toFloat()
    val needle = Offset(center.x + cos(angle) * diameter * 0.34f, center.y + sin(angle) * diameter * 0.34f)
    drawLine(Color.White, center, needle, 5f, StrokeCap.Round)
    drawCircle(Color.White, 8f, center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGantt(document: ChartDocument) {
    val minimum = document.ganttTasks.minOfOrNull { it.start } ?: return
    val maximum = document.ganttTasks.maxOfOrNull { it.end } ?: return
    val range = max(0.0001, maximum - minimum)
    val rowHeight = size.height / document.ganttTasks.size
    document.ganttTasks.forEachIndexed { index, task ->
        val left = ((task.start - minimum) / range).toFloat() * size.width
        val width = ((task.end - task.start) / range).toFloat() * size.width
        val top = index * rowHeight + rowHeight * 0.18f
        val height = rowHeight * 0.64f
        drawRoundRect(Color(task.colorArgb).copy(alpha = 0.35f), Offset(left, top), Size(width, height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f))
        drawRoundRect(Color(task.colorArgb), Offset(left, top), Size(width * task.progress.toFloat().coerceIn(0f, 1f), height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f))
        drawCanvasLabel(task.label, Offset(4f, top + height * 0.65f), Color.White)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadar(document: ChartDocument) {
    val radarSeries = document.radarSeries.take(3)
    val axes = radarSeries.firstOrNull()?.axes ?: return
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.4f
    repeat(4) { ring ->
        val ringPath = Path()
        axes.forEachIndexed { index, _ ->
            val angle = (-PI / 2 + index * 2 * PI / axes.size).toFloat()
            val point = center + Offset(cos(angle), sin(angle)) * radius * (ring + 1).toFloat() / 4f
            if (index == 0) ringPath.moveTo(point.x, point.y) else ringPath.lineTo(point.x, point.y)
        }
        ringPath.close()
        drawPath(ringPath, Color.White.copy(alpha = 0.15f), style = Stroke(1.5f))
    }
    axes.forEachIndexed { index, _ ->
        val angle = (-PI / 2 + index * 2 * PI / axes.size).toFloat()
        val outer = center + Offset(cos(angle), sin(angle)) * radius
        drawLine(Color.White.copy(alpha = 0.2f), center, outer, 1.5f)
    }
    radarSeries.forEach { series ->
        val valuePath = Path()
        axes.forEachIndexed { index, referenceAxis ->
            val axis = series.axes.getOrNull(index) ?: referenceAxis.copy(value = 0.0)
            val angle = (-PI / 2 + index * 2 * PI / axes.size).toFloat()
            val fraction = (axis.value / axis.maximum.coerceAtLeast(0.0001)).toFloat().coerceIn(0f, 1f)
            val point = center + Offset(cos(angle), sin(angle)) * radius * fraction
            if (index == 0) valuePath.moveTo(point.x, point.y) else valuePath.lineTo(point.x, point.y)
        }
        valuePath.close()
        val color = Color(series.colorArgb)
        drawPath(valuePath, color.copy(alpha = 0.16f))
        drawPath(valuePath, color, style = Stroke(3f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaterfall(document: ChartDocument) {
    val points = document.series.firstOrNull()?.points ?: return
    val cumulative = mutableListOf(0.0)
    points.forEach { cumulative += cumulative.last() + it.y }
    val minimum = minOf(0.0, cumulative.min())
    val maximum = maxOf(0.0, cumulative.max())
    val range = max(0.0001, maximum - minimum)
    val slot = size.width / points.size.coerceAtLeast(1)
    fun y(value: Double) = size.height - ((value - minimum) / range).toFloat() * size.height
    points.forEachIndexed { index, point ->
        val from = cumulative[index]
        val to = cumulative[index + 1]
        val top = minOf(y(from), y(to))
        val bottom = maxOf(y(from), y(to))
        val color = if (point.y >= 0) Color(0xFF39D98A) else Color(0xFFFF6B7A)
        drawRect(color.copy(alpha = 0.85f), Offset(index * slot + slot * 0.12f, top), Size(slot * 0.76f, (bottom - top).coerceAtLeast(3f)))
        if (index < points.lastIndex) drawLine(Color.White.copy(alpha = 0.25f), Offset((index + 0.88f) * slot, y(to)), Offset((index + 1.12f) * slot, y(to)), 2f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFunnel(document: ChartDocument) {
    val points = document.series.firstOrNull()?.points?.filter { it.y > 0.0 } ?: return
    val maximum = points.maxOfOrNull { it.y } ?: return
    val rowHeight = size.height / points.size.coerceAtLeast(1)
    points.forEachIndexed { index, point ->
        val width = (point.y / maximum).toFloat() * size.width * 0.92f
        val nextWidth = points.getOrNull(index + 1)?.let { (it.y / maximum).toFloat() * size.width * 0.92f } ?: width * 0.72f
        val left = (size.width - width) / 2f
        val nextLeft = (size.width - nextWidth) / 2f
        val top = index * rowHeight
        val path = Path().apply {
            moveTo(left, top + 2f)
            lineTo(left + width, top + 2f)
            lineTo(nextLeft + nextWidth, top + rowHeight - 2f)
            lineTo(nextLeft, top + rowHeight - 2f)
            close()
        }
        val color = document.series.getOrNull(index % document.series.size.coerceAtLeast(1))?.let { Color(it.colorArgb) } ?: Color(0xFF5BE7C4)
        drawPath(path, color.copy(alpha = 0.82f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPareto(document: ChartDocument) {
    val points = document.series.firstOrNull()?.points?.sortedByDescending { it.y } ?: return
    val total = points.sumOf { it.y.coerceAtLeast(0.0) }.coerceAtLeast(0.0001)
    val maximum = points.maxOfOrNull { it.y }?.coerceAtLeast(0.0001) ?: return
    val slot = size.width / points.size.coerceAtLeast(1)
    var cumulative = 0.0
    var previous: Offset? = null
    points.forEachIndexed { index, point ->
        val height = (point.y / maximum).toFloat() * size.height
        drawRect(Color(0xFF5BE7C4).copy(alpha = 0.78f), Offset(index * slot + 2f, size.height - height), Size((slot - 4f).coerceAtLeast(1f), height))
        cumulative += point.y.coerceAtLeast(0.0)
        val current = Offset((index + 0.5f) * slot, size.height - (cumulative / total).toFloat() * size.height)
        previous?.let { drawLine(Color(0xFFFFC857), it, current, 4f, StrokeCap.Round) }
        drawCircle(Color(0xFFFFC857), 5f, current)
        previous = current
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPictograph(document: ChartDocument) {
    val points = document.series.firstOrNull()?.points ?: return
    val unit = (points.maxOfOrNull { it.y } ?: 1.0).coerceAtLeast(1.0) / 10.0
    val rowHeight = size.height / points.size.coerceAtLeast(1)
    points.forEachIndexed { row, point ->
        val count = (point.y / unit).toInt().coerceIn(0, 20)
        val spacing = size.width / 20f
        repeat(count) { index ->
            val center = Offset((index + 0.5f) * spacing, (row + 0.5f) * rowHeight)
            drawCircle(Color(document.series.first().colorArgb), minOf(spacing, rowHeight) * 0.28f, center)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiagram(document: ChartDocument) {
    val levels = document.diagramNodes.groupBy { it.level }.toSortedMap()
    val positions = mutableMapOf<String, Offset>()
    levels.entries.forEachIndexed { levelIndex, (_, nodes) ->
        nodes.forEachIndexed { nodeIndex, node ->
            val x = (nodeIndex + 0.5f) / nodes.size * size.width
            val y = (levelIndex + 0.5f) / levels.size.coerceAtLeast(1) * size.height
            positions[node.id] = Offset(x, y)
        }
    }
    document.diagramEdges.forEach { edge ->
        val start = positions[edge.fromId] ?: return@forEach
        val end = positions[edge.toId] ?: return@forEach
        val middle = Offset(end.x, start.y)
        drawLine(Color.White.copy(alpha = 0.45f), start, middle, 3f)
        drawLine(Color.White.copy(alpha = 0.45f), middle, end, 3f)
    }
    document.diagramNodes.forEach { node ->
        val center = positions[node.id] ?: return@forEach
        val nodeSize = Size(88f, 44f)
        drawRoundRect(Color(node.colorArgb).copy(alpha = 0.85f), center - Offset(nodeSize.width / 2f, nodeSize.height / 2f), nodeSize, androidx.compose.ui.geometry.CornerRadius(12f))
        drawCanvasLabel(node.label.take(14), center + Offset(-nodeSize.width * 0.38f, 5f), Color.Black)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCanvasLabel(text: String, offset: Offset, color: Color) {
    drawContext.canvas.nativeCanvas.drawText(
        text,
        offset.x,
        offset.y,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textSize = 24f
        },
    )
}
