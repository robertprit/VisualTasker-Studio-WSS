package com.visualtasker.chartgraph.demo.data

data class CanvasPoint(val x: Float, val y: Float)

data class PieSegmentGeometry(
    val id: String,
    val startAngle: Float,
    val sweepAngle: Float,
    val value: Float
)

object ChartGeometry {
    fun mapLinePoints(
        points: List<DataPoint>,
        width: Float,
        height: Float,
        minY: Float,
        maxY: Float
    ): List<CanvasPoint> {
        if (points.isEmpty()) return emptyList()
        val valueRange = (maxY - minY).takeIf { it > 0f } ?: 1f
        val xDenominator = (points.size - 1).coerceAtLeast(1).toFloat()
        return points.mapIndexed { index, point ->
            val x = (index / xDenominator) * width
            val normalizedY = ((point.y - minY) / valueRange).coerceIn(0f, 1f)
            val y = height - normalizedY * height
            CanvasPoint(x = x, y = y)
        }
    }

    fun pieSegments(
        datasets: List<ChartDataset>,
        startAngle: Float = -90f
    ): List<PieSegmentGeometry> {
        val values = datasets.map { kotlin.math.abs(it.points.lastOrNull()?.y ?: 0f) }
        val total = values.sum()
        if (total <= 0f) return emptyList()

        var current = startAngle
        return datasets.mapIndexed { index, dataset ->
            val sweep = (values[index] / total) * 360f
            PieSegmentGeometry(
                id = dataset.id,
                startAngle = current,
                sweepAngle = sweep,
                value = values[index]
            ).also {
                current += sweep
            }
        }
    }
}
