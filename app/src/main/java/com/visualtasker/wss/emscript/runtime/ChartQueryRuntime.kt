package com.visualtasker.wss.emscript.runtime

import com.visualtasker.chartgraph.domain.ChartDocument
import java.io.IOException
import java.util.Collections

data class ChartPointSnapshot(val x: Double, val y: Double)

data class ChartSeriesSnapshot(
    val id: String,
    val label: String,
    val colorArgb: Long,
    val points: List<ChartPointSnapshot>,
)

data class ChartCandleSnapshot(
    val x: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
)

data class ChartBoxPlotSnapshot(
    val label: String,
    val minimum: Double,
    val lowerQuartile: Double,
    val median: Double,
    val upperQuartile: Double,
    val maximum: Double,
    val outliers: List<Double>,
)

data class ChartHeatmapCellSnapshot(
    val column: Int,
    val row: Int,
    val value: Double,
    val label: String?,
)

data class ChartBubbleSnapshot(
    val x: Double,
    val y: Double,
    val magnitude: Double,
    val label: String?,
    val colorArgb: Long?,
)

data class ChartVennSetSnapshot(
    val id: String,
    val label: String,
    val value: Double,
    val colorArgb: Long,
)

data class ChartVennOverlapSnapshot(
    val setIds: Set<String>,
    val value: Double,
    val label: String?,
)

data class ChartMosaicCellSnapshot(
    val group: String,
    val category: String,
    val value: Double,
    val colorArgb: Long,
)

data class ChartGanttTaskSnapshot(
    val id: String,
    val label: String,
    val start: Double,
    val end: Double,
    val progress: Double,
    val colorArgb: Long,
)

data class ChartRadarAxisSnapshot(
    val label: String,
    val value: Double,
    val maximum: Double,
)

data class ChartRadarSeriesSnapshot(
    val id: String,
    val label: String,
    val colorArgb: Long,
    val axes: List<ChartRadarAxisSnapshot>,
)

data class ChartDiagramNodeSnapshot(
    val id: String,
    val label: String,
    val level: Int,
    val colorArgb: Long,
)

data class ChartDiagramEdgeSnapshot(
    val fromId: String,
    val toId: String,
    val label: String?,
)

data class ChartSnapshotOptions(
    val xAxisLabel: String? = null,
    val yAxisLabel: String? = null,
    val showLegend: Boolean = true,
)

internal data class ChartSnapshotState(
    val id: String,
    val title: String,
    val kind: String,
    val series: List<ChartSeriesSnapshot>,
    val candles: List<ChartCandleSnapshot>,
    val boxPlots: List<ChartBoxPlotSnapshot>,
    val heatmapCells: List<ChartHeatmapCellSnapshot>,
    val histogramBinCount: Int,
    val bubbles: List<ChartBubbleSnapshot>,
    val vennSets: List<ChartVennSetSnapshot>,
    val vennOverlaps: List<ChartVennOverlapSnapshot>,
    val mosaicCells: List<ChartMosaicCellSnapshot>,
    val gaugeValue: Double?,
    val gaugeMinimum: Double,
    val gaugeMaximum: Double,
    val ganttTasks: List<ChartGanttTaskSnapshot>,
    val radarSeries: List<ChartRadarSeriesSnapshot>,
    val diagramNodes: List<ChartDiagramNodeSnapshot>,
    val diagramEdges: List<ChartDiagramEdgeSnapshot>,
    val options: ChartSnapshotOptions,
) {
    companion object {
        fun create(
            id: String,
            title: String,
            kind: String,
            series: List<ChartSeriesSnapshot>,
            candles: List<ChartCandleSnapshot>,
            boxPlots: List<ChartBoxPlotSnapshot>,
            heatmapCells: List<ChartHeatmapCellSnapshot>,
            histogramBinCount: Int,
            bubbles: List<ChartBubbleSnapshot>,
            vennSets: List<ChartVennSetSnapshot>,
            vennOverlaps: List<ChartVennOverlapSnapshot>,
            mosaicCells: List<ChartMosaicCellSnapshot>,
            gaugeValue: Double?,
            gaugeMinimum: Double,
            gaugeMaximum: Double,
            ganttTasks: List<ChartGanttTaskSnapshot>,
            radarSeries: List<ChartRadarSeriesSnapshot>,
            diagramNodes: List<ChartDiagramNodeSnapshot>,
            diagramEdges: List<ChartDiagramEdgeSnapshot>,
            options: ChartSnapshotOptions,
        ): ChartSnapshotState {
            require(id.isNotBlank()) { "Chart snapshot id must not be blank." }
            require(title.isNotBlank()) { "Chart snapshot title must not be blank." }
            require(kind.isNotBlank()) { "Chart snapshot kind must not be blank." }
            require(histogramBinCount > 0) { "Chart histogram bin count must be positive." }
            requireFinite(gaugeMinimum, "gaugeMinimum")
            requireFinite(gaugeMaximum, "gaugeMaximum")
            gaugeValue?.let { requireFinite(it, "gaugeValue") }
            require(gaugeMinimum <= gaugeMaximum) { "Chart gauge range is invalid." }
            return ChartSnapshotState(
                id = id,
                title = title,
                kind = kind,
                series = immutableList(series.map { item ->
                    item.copy(points = immutableList(item.points.map { point -> point.copy() }))
                }),
                candles = immutableList(candles.map { it.copy() }),
                boxPlots = immutableList(boxPlots.map { item ->
                    item.copy(outliers = immutableList(item.outliers.toList()))
                }),
                heatmapCells = immutableList(heatmapCells.map { it.copy() }),
                histogramBinCount = histogramBinCount,
                bubbles = immutableList(bubbles.map { it.copy() }),
                vennSets = immutableList(vennSets.map { it.copy() }),
                vennOverlaps = immutableList(vennOverlaps.map { item ->
                    item.copy(setIds = Collections.unmodifiableSet(LinkedHashSet(item.setIds)))
                }),
                mosaicCells = immutableList(mosaicCells.map { it.copy() }),
                gaugeValue = gaugeValue,
                gaugeMinimum = gaugeMinimum,
                gaugeMaximum = gaugeMaximum,
                ganttTasks = immutableList(ganttTasks.map { it.copy() }),
                radarSeries = immutableList(radarSeries.map { item ->
                    item.copy(axes = immutableList(item.axes.map { axis -> axis.copy() }))
                }),
                diagramNodes = immutableList(diagramNodes.map { it.copy() }),
                diagramEdges = immutableList(diagramEdges.map { it.copy() }),
                options = options.copy(),
            ).also(ChartSnapshotState::validateNumbers)
        }

        private fun <T> immutableList(values: Collection<T>): List<T> =
            Collections.unmodifiableList(ArrayList(values))
    }

    private fun validateNumbers() {
        series.flatMap(ChartSeriesSnapshot::points).forEach { point ->
            requireFinite(point.x, "series.x")
            requireFinite(point.y, "series.y")
        }
        candles.forEach { item ->
            listOf(item.x, item.open, item.high, item.low, item.close).forEach { requireFinite(it, "candle") }
        }
        boxPlots.forEach { item ->
            listOf(item.minimum, item.lowerQuartile, item.median, item.upperQuartile, item.maximum)
                .plus(item.outliers)
                .forEach { requireFinite(it, "boxPlot") }
        }
        heatmapCells.forEach { requireFinite(it.value, "heatmap") }
        bubbles.forEach { item ->
            listOf(item.x, item.y, item.magnitude).forEach { requireFinite(it, "bubble") }
        }
        vennSets.forEach { requireFinite(it.value, "vennSet") }
        vennOverlaps.forEach { requireFinite(it.value, "vennOverlap") }
        mosaicCells.forEach { requireFinite(it.value, "mosaic") }
        ganttTasks.forEach { item ->
            listOf(item.start, item.end, item.progress).forEach { requireFinite(it, "gantt") }
        }
        radarSeries.flatMap(ChartRadarSeriesSnapshot::axes).forEach { item ->
            requireFinite(item.value, "radar.value")
            requireFinite(item.maximum, "radar.maximum")
        }
    }
}

fun interface ChartDocumentRepository {
    fun findById(id: String): ChartDocument?
}

internal object ChartQueryRuntime {
    fun lookup(
        id: String,
        repository: ChartDocumentRepository?,
    ): RuntimeAdapterResult {
        if (repository == null) {
            return RuntimeAdapterResult.failure(
                RuntimeQueryDiagnosticCodes.CHART_REPOSITORY_UNAVAILABLE,
                "Das Chart-Repository ist nicht verfuegbar.",
            )
        }
        if (id.isBlank()) {
            return RuntimeAdapterResult.failure(
                RuntimeQueryDiagnosticCodes.CHART_QUERY_FAILED,
                "Die Chart-ID darf nicht leer sein.",
            )
        }
        val document = try {
            repository.findById(id)
        } catch (error: IOException) {
            return RuntimeAdapterResult.failure(
                RuntimeQueryDiagnosticCodes.CHART_LOAD_FAILED,
                error.message ?: "Die Chart-Ressource konnte nicht geladen werden.",
            )
        } catch (error: Exception) {
            return RuntimeAdapterResult.failure(
                RuntimeQueryDiagnosticCodes.CHART_QUERY_FAILED,
                error.message ?: "Die Chart-Abfrage ist fehlgeschlagen.",
            )
        } ?: return RuntimeAdapterResult.success(EmscriptValue.NullValue, "chart.lookup -> ABSENT")

        return runCatching { document.toSnapshotValue(expectedId = id) }.fold(
            onSuccess = { value -> RuntimeAdapterResult.success(value, "chart.lookup -> VALUE(id=${value.id})") },
            onFailure = { error ->
                RuntimeAdapterResult.failure(
                    RuntimeQueryDiagnosticCodes.CHART_DECODE_FAILED,
                    error.message ?: "Die Chart-Ressource ist ungueltig.",
                )
            },
        )
    }
}

internal fun ChartDocument.toSnapshotValue(expectedId: String = id): EmscriptValue.ChartSnapshotValue {
    require(id == expectedId) { "Chart repository returned id '$id' for '$expectedId'." }
    return EmscriptValue.ChartSnapshotValue.create(
        id = id,
        title = title,
        kind = kind.name,
        series = series.map { item ->
            ChartSeriesSnapshot(item.id, item.label, item.colorArgb, item.points.map { ChartPointSnapshot(it.x, it.y) })
        },
        candles = candles.map { ChartCandleSnapshot(it.x, it.open, it.high, it.low, it.close) },
        boxPlots = boxPlots.map {
            ChartBoxPlotSnapshot(it.label, it.minimum, it.lowerQuartile, it.median, it.upperQuartile, it.maximum, it.outliers)
        },
        heatmapCells = heatmapCells.map { ChartHeatmapCellSnapshot(it.column, it.row, it.value, it.label) },
        histogramBinCount = histogramBinCount,
        bubbles = bubbles.map { ChartBubbleSnapshot(it.x, it.y, it.magnitude, it.label, it.colorArgb) },
        vennSets = vennSets.map { ChartVennSetSnapshot(it.id, it.label, it.value, it.colorArgb) },
        vennOverlaps = vennOverlaps.map { ChartVennOverlapSnapshot(it.setIds, it.value, it.label) },
        mosaicCells = mosaicCells.map { ChartMosaicCellSnapshot(it.group, it.category, it.value, it.colorArgb) },
        gaugeValue = gaugeValue,
        gaugeMinimum = gaugeMinimum,
        gaugeMaximum = gaugeMaximum,
        ganttTasks = ganttTasks.map { ChartGanttTaskSnapshot(it.id, it.label, it.start, it.end, it.progress, it.colorArgb) },
        radarSeries = radarSeries.map { item ->
            ChartRadarSeriesSnapshot(
                item.id,
                item.label,
                item.colorArgb,
                item.axes.map { ChartRadarAxisSnapshot(it.label, it.value, it.maximum) },
            )
        },
        diagramNodes = diagramNodes.map { ChartDiagramNodeSnapshot(it.id, it.label, it.level, it.colorArgb) },
        diagramEdges = diagramEdges.map { ChartDiagramEdgeSnapshot(it.fromId, it.toId, it.label) },
        options = ChartSnapshotOptions(xAxisLabel, yAxisLabel, showLegend),
    )
}

private fun requireFinite(value: Double, field: String) {
    require(value.isFinite()) { "Chart snapshot field '$field' must be finite." }
}
