package com.visualtasker.chartgraph.demo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.visualtasker.chartgraph.demo.data.CandleDataPoint
import com.visualtasker.chartgraph.demo.data.ChartGeometry
import com.visualtasker.chartgraph.demo.data.ChartDataset
import com.visualtasker.chartgraph.demo.data.ChartType

@Composable
fun AggrChart(
    type: ChartType,
    datasets: List<ChartDataset>,
    candleData: List<CandleDataPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (type != ChartType.CANDLE && datasets.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("Keine Daten", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    if (type == ChartType.CANDLE && candleData.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("Keine Kerzendaten", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    Canvas(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (type == ChartType.CANDLE) {
            val maxCandles = candleData.size.coerceAtLeast(2)
            val highs = candleData.map { it.high }
            val lows = candleData.map { it.low }
            val minY = lows.minOrNull() ?: 0f
            val maxY = highs.maxOrNull() ?: 1f
            val range = (maxY - minY).takeIf { it > 0f } ?: 1f

            fun yFor(v: Float): Float = size.height - (((v - minY) / range).coerceIn(0f, 1f) * size.height)

            val slotWidth = size.width / maxCandles
            val bodyWidth = slotWidth * 0.55f

            candleData.forEachIndexed { index, candle ->
                val centerX = (index + 0.5f) * slotWidth
                val openY = yFor(candle.open)
                val closeY = yFor(candle.close)
                val highY = yFor(candle.high)
                val lowY = yFor(candle.low)
                val isBullish = candle.close >= candle.open
                val color = if (isBullish) Color(0xFF00FF9D) else Color(0xFFFF007F)

                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(centerX, highY),
                    end = androidx.compose.ui.geometry.Offset(centerX, lowY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                val top = minOf(openY, closeY)
                val bottom = maxOf(openY, closeY)
                val bodyHeight = (bottom - top).coerceAtLeast(2f)

                drawRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = androidx.compose.ui.geometry.Offset(centerX - bodyWidth / 2f, top),
                    size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight)
                )
            }

            return@Canvas
        }

        val chartWidth = size.width
        val chartHeight = size.height
        val maxPointCount = datasets.maxOf { it.points.size }.coerceAtLeast(2)
        val allValues = datasets.flatMap { dataset -> dataset.points.map { it.y } }
        val minY = allValues.minOrNull() ?: 0f
        val maxY = allValues.maxOrNull() ?: 100f
        val valueRange = (maxY - minY).takeIf { it > 0f } ?: 1f

        fun normalizedY(value: Float): Float {
            return ((value - minY) / valueRange).coerceIn(0f, 1f)
        }

        if (type == ChartType.PIE || type == ChartType.DONUT) {
            val segments = ChartGeometry.pieSegments(datasets)
            if (segments.isEmpty()) return@Canvas
            val diameter = minOf(chartWidth, chartHeight) * 0.8f
            val topLeft = androidx.compose.ui.geometry.Offset(
                x = (chartWidth - diameter) / 2f,
                y = (chartHeight - diameter) / 2f
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            val donutStroke = Stroke(width = diameter * 0.28f)

            segments.forEachIndexed { index, segment ->
                drawArc(
                    color = Color(datasets[index].color).copy(alpha = 0.9f),
                    startAngle = segment.startAngle,
                    sweepAngle = segment.sweepAngle,
                    useCenter = type == ChartType.PIE,
                    topLeft = topLeft,
                    size = arcSize,
                    style = if (type == ChartType.DONUT) donutStroke else androidx.compose.ui.graphics.drawscope.Fill
                )
            }
            return@Canvas
        }

        datasets.forEachIndexed { datasetIndex, dataset ->
            if (dataset.points.isEmpty()) return@forEachIndexed
            val color = Color(dataset.color)
            val points = dataset.points

            when (type) {
                ChartType.LINE -> {
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val x1 = ((i - 1).toFloat() / (maxPointCount - 1)) * chartWidth
                        val x2 = (i.toFloat() / (maxPointCount - 1)) * chartWidth
                        val y1 = chartHeight - normalizedY(prev.y) * chartHeight
                        val y2 = chartHeight - normalizedY(curr.y) * chartHeight
                        drawLine(
                            color = color.copy(alpha = 0.9f),
                            start = androidx.compose.ui.geometry.Offset(x1, y1),
                            end = androidx.compose.ui.geometry.Offset(x2, y2),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                ChartType.COLUMN -> {
                    val barWidth = chartWidth / (maxPointCount * (datasets.size + 1f))
                    points.forEachIndexed { i, point ->
                        val barHeight = normalizedY(point.y) * chartHeight
                        val left = i * barWidth * (datasets.size + 1f) + datasetIndex * barWidth
                        drawRect(
                            color = color.copy(alpha = 0.75f),
                            topLeft = androidx.compose.ui.geometry.Offset(left, chartHeight - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                        )
                    }
                }
                ChartType.PIE -> Unit
                ChartType.DONUT -> Unit
                ChartType.CANDLE -> Unit
            }
        }
    }
}