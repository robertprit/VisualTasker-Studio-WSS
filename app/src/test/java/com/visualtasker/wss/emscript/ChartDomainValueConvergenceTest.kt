package com.visualtasker.wss.emscript

import com.visualtasker.chartgraph.domain.BoxPlotPoint
import com.visualtasker.chartgraph.domain.BubblePoint
import com.visualtasker.chartgraph.domain.CandlePoint
import com.visualtasker.chartgraph.domain.ChartDocument
import com.visualtasker.chartgraph.domain.ChartKind
import com.visualtasker.chartgraph.domain.ChartPoint
import com.visualtasker.chartgraph.domain.ChartSeries
import com.visualtasker.chartgraph.domain.DiagramEdge
import com.visualtasker.chartgraph.domain.DiagramNode
import com.visualtasker.chartgraph.domain.GanttTask
import com.visualtasker.chartgraph.domain.HeatmapCell
import com.visualtasker.chartgraph.domain.MosaicCell
import com.visualtasker.chartgraph.domain.RadarAxis
import com.visualtasker.chartgraph.domain.RadarSeries
import com.visualtasker.chartgraph.domain.VennOverlap
import com.visualtasker.chartgraph.domain.VennSet
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.runtime.ChartDocumentRepository
import com.visualtasker.wss.emscript.runtime.ChartQueryRuntime
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeAdapterResult
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityGate
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntimeEnvironment
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrExpression
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.ir.IrStatement
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CommandCapability
import de.visualtasker.blockeditor.registry.CommandCatalogKind
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ChartDomainValueConvergenceTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun `chart document maps losslessly to an immutable domain snapshot`() {
        val mutablePoints = mutableListOf(ChartPoint(1.0, 2.0), ChartPoint(3.0, 4.0))
        val mutableSeries = mutableListOf(ChartSeries("series-1", "Series", 0xFF00FF00, mutablePoints))
        val mutableOutliers = mutableListOf(0.5, 9.5)
        val mutableSetIds = linkedSetOf("set-a", "set-b")
        val document = completeDocument(
            series = mutableSeries,
            boxPlots = listOf(BoxPlotPoint("Box", 1.0, 2.0, 3.0, 4.0, 5.0, mutableOutliers)),
            vennOverlaps = listOf(VennOverlap(mutableSetIds, 2.0, "Overlap")),
        )

        val result = ChartQueryRuntime.lookup("chart-1", ChartDocumentRepository { document })
        assertTrue(result.success)
        val snapshot = result.value as EmscriptValue.ChartSnapshotValue

        assertEquals("chart-1", snapshot.id)
        assertEquals("Complete chart", snapshot.title)
        assertEquals(ChartKind.LINE.name, snapshot.kind)
        assertEquals(mutableSeries.map { it.id }, snapshot.series.map { it.id })
        assertEquals(document.candles.size, snapshot.candles.size)
        assertEquals(document.boxPlots.size, snapshot.boxPlots.size)
        assertEquals(document.heatmapCells.size, snapshot.heatmapCells.size)
        assertEquals(document.bubbles.size, snapshot.bubbles.size)
        assertEquals(document.vennSets.size, snapshot.vennSets.size)
        assertEquals(document.vennOverlaps.size, snapshot.vennOverlaps.size)
        assertEquals(document.mosaicCells.size, snapshot.mosaicCells.size)
        assertEquals(document.ganttTasks.size, snapshot.ganttTasks.size)
        assertEquals(document.radarSeries.size, snapshot.radarSeries.size)
        assertEquals(document.diagramNodes.size, snapshot.diagramNodes.size)
        assertEquals(document.diagramEdges.size, snapshot.diagramEdges.size)
        assertEquals("Time", snapshot.options.xAxisLabel)
        assertEquals("Value", snapshot.options.yAxisLabel)
        assertFalse(snapshot.options.showLegend)

        mutablePoints.clear()
        mutableSeries.clear()
        mutableOutliers.clear()
        mutableSetIds.clear()
        assertEquals(2, snapshot.series.single().points.size)
        assertEquals(listOf(0.5, 9.5), snapshot.boxPlots.single().outliers)
        assertEquals(setOf("set-a", "set-b"), snapshot.vennOverlaps.single().setIds)
        assertTrue(runCatching {
            @Suppress("UNCHECKED_CAST")
            (snapshot.series as MutableList).clear()
        }.isFailure)
    }

    @Test
    fun `exists and get share value absent and failure semantics`() = runBlocking {
        val present = runtime(ChartDocumentRepository { id -> completeDocument().takeIf { it.id == id } })
            .run(documentFor(EXISTS_AND_GET))
        assertTrue(present.toString(), present is EmscriptDryRunResult.Success)
        present as EmscriptDryRunResult.Success
        assertEquals(EmscriptValue.BooleanValue(true), present.variables["exists"])
        assertTrue(present.variables["snapshot"] is EmscriptValue.ChartSnapshotValue)

        val absent = runtime(ChartDocumentRepository { null }).run(documentFor(EXISTS_AND_GET))
        assertTrue(absent is EmscriptDryRunResult.Success)
        absent as EmscriptDryRunResult.Success
        assertEquals(EmscriptValue.BooleanValue(false), absent.variables["exists"])
        assertTrue(absent.variables["snapshot"] === EmscriptValue.NullValue)

        val brokenRepository = ChartDocumentRepository { throw IOException("storage unavailable") }
        listOf(
            "LET exists:Bool = chart.exists(\"chart-1\")",
            "LET snapshot:ChartSnapshot? = chart.get(\"chart-1\")",
        ).forEach { source ->
            val failure = runtime(brokenRepository).run(documentFor(source))
            assertTrue(failure is EmscriptDryRunResult.Failure)
            failure as EmscriptDryRunResult.Failure
            assertTrue(failure.events.any { it.diagnosticCode == RuntimeQueryDiagnosticCodes.CHART_LOAD_FAILED })
        }
    }

    @Test
    fun `repository absence failure decode and invalid adapter values remain distinct`() {
        val absent = ChartQueryRuntime.lookup("missing", ChartDocumentRepository { null })
        assertTrue(absent.success)
        assertTrue(absent.value === EmscriptValue.NullValue)

        val unavailable = ChartQueryRuntime.lookup("chart-1", null)
        assertFalse(unavailable.success)
        assertEquals(RuntimeQueryDiagnosticCodes.CHART_REPOSITORY_UNAVAILABLE, unavailable.diagnosticCode)

        val queryFailure = ChartQueryRuntime.lookup("chart-1", ChartDocumentRepository { error("query failed") })
        assertFalse(queryFailure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.CHART_QUERY_FAILED, queryFailure.diagnosticCode)

        val decodeFailure = ChartQueryRuntime.lookup(
            "chart-1",
            ChartDocumentRepository { completeDocument().copy(id = "wrong-id") },
        )
        assertFalse(decodeFailure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.CHART_DECODE_FAILED, decodeFailure.diagnosticCode)
    }

    @Test
    fun `source workspace IR serializer and aliases preserve chart reporters`() {
        val source = """
            LET exists:Bool = Chart.exists("chart-1")
            LET snapshot:ChartSnapshot? = Chart.get("chart-1")
            SET exists = chart.exists("chart-2")
            SET snapshot = chart.get("chart-2")
            IF exists
                log("chart exists")
            END IF
        """.trimIndent()
        val preview = guard.preview(source)
        assertTrue(preview.toString(), preview is EmscriptApplyGuardResult.Success)
        preview as EmscriptApplyGuardResult.Success
        val document = preview.importedDocument
        val commands = document.blocks.values.filter {
            it.type.startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX) &&
                it.metadata[VisualTaskerCommandCatalog.METADATA_COMMAND_ID] in COMMAND_IDS
        }
        assertEquals(4, commands.size)
        assertTrue(commands.all { it.output?.provides == RETURN_TYPES.getValue(it.metadata.getValue(VisualTaskerCommandCatalog.METADATA_COMMAND_ID)) })

        val registry = CompositeBlockRegistry().apply {
            document.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val ir = IrGenerator(registry).generate(document)
        val calls = ir.statements.filterIsInstance<IrStatement.SetVariable>()
            .mapNotNull { it.expression as? IrExpression.CommandCall }
        assertEquals(4, calls.size)
        assertTrue(calls.all { it.returnType == RETURN_TYPES.getValue(it.commandId) })

        val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)
        assertTrue(generated, generated.contains("chart.exists("))
        assertTrue(generated, generated.contains("chart.get("))
        assertFalse(generated, generated.contains("Chart.exists"))
        assertFalse(generated, generated.contains("Chart.get"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)

        val decoded = WorkspaceSerializer.deserialize(WorkspaceSerializer.serialize(document))
        assertEquals(document.blocks.keys, decoded.blocks.keys)
    }

    @Test
    fun `chart reporters enforce type compatibility and reject legacy property access at runtime`() = runBlocking {
        listOf(
            "LET value:String = chart.exists(\"chart-1\")",
            "LET value:Number = chart.exists(\"chart-1\")",
            "LET value:ChartSnapshot = chart.exists(\"chart-1\")",
            "LET value:Bool = chart.get(\"chart-1\")",
            "LET value:String = chart.get(\"chart-1\")",
            "LET value:Number = chart.get(\"chart-1\")",
        ).forEach { source ->
            assertTrue("$source must fail", guard.preview(source) is EmscriptApplyGuardResult.Failure)
        }

        val result = runtime(ChartDocumentRepository { completeDocument() })
            .run(documentFor("LET snapshot:ChartSnapshot? = chart.get(\"chart-1\", \"title\")"))
        assertTrue(result is EmscriptDryRunResult.Failure)
        result as EmscriptDryRunResult.Failure
        assertTrue(result.events.any { it.diagnosticCode == RuntimeQueryDiagnosticCodes.CHART_RESULT_INVALID })
    }

    @Test
    fun `catalog exposes final static chart return contracts`() {
        val exists = VisualTaskerCommandCatalog.findById("chart.exists")!!
        val get = VisualTaskerCommandCatalog.findById("chart.get")!!
        assertEquals(CommandCatalogKind.REPORTER, exists.kind)
        assertEquals("Bool", exists.returnType)
        assertEquals(CommandCatalogKind.REPORTER, get.kind)
        assertEquals("ChartSnapshot?", get.returnType)
        assertEquals(listOf("Chart.exists"), exists.acceptedAliases)
        assertEquals(listOf("Chart.get"), get.acceptedAliases)
    }

    private fun runtime(repository: ChartDocumentRepository?) = WorkspaceBasicRuntime(
        capabilityGate = {
            RuntimeCapabilityGate(RuntimeCapabilityGate.BasicRealRunCapabilities + CommandCapability.CHARTS)
        },
        environment = WorkspaceBasicRuntimeEnvironment(
            delayMs = {},
            playBeep = { _, _, _ -> },
            vibrate = {},
            log = {},
            chartLookup = { id -> ChartQueryRuntime.lookup(id, repository) },
        ),
    )

    private fun documentFor(source: String) =
        (guard.preview(source) as EmscriptApplyGuardResult.Success).importedDocument

    private fun completeDocument(
        series: List<ChartSeries> = listOf(
            ChartSeries("series-1", "Series", 0xFF00FF00, listOf(ChartPoint(1.0, 2.0))),
        ),
        boxPlots: List<BoxPlotPoint> = listOf(BoxPlotPoint("Box", 1.0, 2.0, 3.0, 4.0, 5.0)),
        vennOverlaps: List<VennOverlap> = listOf(VennOverlap(setOf("set-a", "set-b"), 2.0)),
    ) = ChartDocument(
        id = "chart-1",
        title = "Complete chart",
        kind = ChartKind.LINE,
        series = series,
        candles = listOf(CandlePoint(1.0, 2.0, 5.0, 1.0, 4.0)),
        boxPlots = boxPlots,
        heatmapCells = listOf(HeatmapCell(1, 2, 3.0, "Cell")),
        histogramBinCount = 12,
        bubbles = listOf(BubblePoint(1.0, 2.0, 3.0, "Bubble", 0xFFFF0000)),
        vennSets = listOf(
            VennSet("set-a", "A", 4.0, 0xFFFF0000),
            VennSet("set-b", "B", 5.0, 0xFF00FF00),
        ),
        vennOverlaps = vennOverlaps,
        mosaicCells = listOf(MosaicCell("Group", "Category", 6.0, 0xFF0000FF)),
        gaugeValue = 42.0,
        gaugeMinimum = 0.0,
        gaugeMaximum = 100.0,
        ganttTasks = listOf(GanttTask("task-1", "Task", 1.0, 5.0, 0.5, 0xFFFFFFFF)),
        radarSeries = listOf(
            RadarSeries("radar-1", "Radar", 0xFFFFFFFF, listOf(RadarAxis("Axis", 0.5, 1.0))),
        ),
        diagramNodes = listOf(DiagramNode("node-1", "Node", 1, 0xFFFFFFFF)),
        diagramEdges = listOf(DiagramEdge("node-1", "node-2", "Edge")),
        xAxisLabel = "Time",
        yAxisLabel = "Value",
        showLegend = false,
    )

    private companion object {
        const val EXISTS_AND_GET = """
            LET exists:Bool = chart.exists("chart-1")
            LET snapshot:ChartSnapshot? = chart.get("chart-1")
        """
        val COMMAND_IDS = setOf("chart.exists", "chart.get")
        val RETURN_TYPES = mapOf("chart.exists" to "Bool", "chart.get" to "ChartSnapshot?")
    }
}
