package com.visualtasker.chartgraph.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.visualtasker.chartgraph.domain.ChartDocument
import com.visualtasker.chartgraph.domain.ChartKind
import kotlin.math.max

@Composable
fun ChartGraph(
    document: ChartDocument,
    modifier: Modifier = Modifier,
) {
    if (document.series.all { it.points.isEmpty() } && document.candles.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Keine Chart-Daten", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Canvas(modifier = modifier.fillMaxSize().padding(12.dp)) {
        when (document.kind) {
            ChartKind.CANDLE -> drawCandles(document)
            ChartKind.PIE, ChartKind.DONUT -> drawPie(document)
            ChartKind.LINE, ChartKind.BAR -> drawCartesian(document)
        }
    }
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
