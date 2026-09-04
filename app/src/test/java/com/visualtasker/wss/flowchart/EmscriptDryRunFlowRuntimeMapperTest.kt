package com.visualtasker.wss.flowchart

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceDryRunRuntime
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.ir.IrGraph
import de.visualtasker.blockeditor.ir.IrGraphGenerator
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.validation.FlowRuntimeSnapshotValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmscriptDryRunFlowRuntimeMapperTest {
    @Test
    fun leavesTextOnlyDryRunWithoutFlowchartNodeStates() {
        val imported = EmscriptWorkspaceImporter()
            .import(EditorDefaults.integrationTestScript, workspaceId = "runtime-flowchart")
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val (irGraph, graph) = project(imported.document!!)
        val dryRun = EmscriptDryRunRuntime().run(EditorDefaults.integrationTestScript)
        assertTrue(dryRun is EmscriptDryRunResult.Success)

        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = irGraph,
            graph = graph,
            result = dryRun,
            sequence = 7,
            capturedAtEpochMs = 42,
        )

        assertEquals(graph.documentId, snapshot.documentId)
        assertEquals(graph.documentRevision, snapshot.documentRevision)
        assertEquals(7, snapshot.sequence)
        assertTrue(snapshot.nodeStates.isEmpty())
        assertTrue(snapshot.diagnostics.isEmpty())
        assertTrue(FlowRuntimeSnapshotValidator.validate(graph, snapshot).isValid)
    }

    @Test
    fun mapsWorkspaceTraceToPartialStepperSnapshot() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            LET value = 1
            log("after-let")
            """.trimIndent(),
            workspaceId = "runtime-flowchart-stepper",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val (irGraph, graph) = project(imported.document!!)
        val dryRun = WorkspaceDryRunRuntime().run(imported.document!!)
        assertTrue(dryRun is EmscriptDryRunResult.Success)

        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = irGraph,
            graph = graph,
            result = dryRun,
            sequence = 4,
            capturedAtEpochMs = 42,
            maxEventIndex = 1,
        )
        val startNode = requireNotNull(graph.entryNodeId)
        val logNode = graph.nodes.first { it.label == "LOG \"after-let\"" }

        assertEquals(startNode, snapshot.activeNodeId)
        assertEquals(FlowRuntimeNodeState.SUCCEEDED, snapshot.nodeStates[startNode])
        assertEquals(FlowRuntimeNodeState.SKIPPED, snapshot.nodeStates[logNode.id])
        assertTrue(FlowRuntimeSnapshotValidator.validate(graph, snapshot).isValid)
    }

    @Test
    fun mapsFailedDryRunToRuntimeDiagnostic() {
        val imported = EmscriptWorkspaceImporter()
            .import(EditorDefaults.integrationTestScript, workspaceId = "runtime-flowchart-failure")
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val (irGraph, graph) = project(imported.document!!)
        val dryRun = EmscriptDryRunRuntime().run("WHILE true\nEND WHILE")
        assertTrue(dryRun is EmscriptDryRunResult.Failure)

        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = irGraph,
            graph = graph,
            result = dryRun,
            sequence = 1,
            capturedAtEpochMs = 42,
        )

        assertTrue(snapshot.diagnostics.any { it.code == "EMSCRIPT_DRY_RUN_FAILED" })
        assertEquals(FlowRuntimeNodeState.FAILED, snapshot.nodeStates[graph.entryNodeId])
        assertTrue(FlowRuntimeSnapshotValidator.validate(graph, snapshot).isValid)
    }

    @Test
    fun mapsWorkspaceTraceToExecutedAndSkippedFlowNodes() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            LET value = 0
            IF value > 0
              log("then")
            ELSE
              log("else")
            END IF
            """.trimIndent(),
            workspaceId = "runtime-flowchart-branches",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val (irGraph, graph) = project(imported.document!!)
        val dryRun = WorkspaceDryRunRuntime().run(imported.document!!)
        assertTrue(dryRun is EmscriptDryRunResult.Success)

        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = irGraph,
            graph = graph,
            result = dryRun,
            sequence = 2,
            capturedAtEpochMs = 42,
        )
        val thenNode = graph.nodes.first { it.label == "LOG \"then\"" }
        val elseNode = graph.nodes.first { it.label == "LOG \"else\"" }

        assertEquals(FlowRuntimeNodeState.SKIPPED, snapshot.nodeStates[thenNode.id])
        assertEquals(FlowRuntimeNodeState.SUCCEEDED, snapshot.nodeStates[elseNode.id])
        assertTrue(snapshot.extensions.any { it.key == "visualtasker.runtime-events" })
        assertTrue(snapshot.runtimeVariables().containsKey("value"))
        assertTrue(snapshot.traversedEdgeIds.any { edgeId ->
            graph.edges.any { it.id == edgeId && it.kind.name == "FALSE_BRANCH" }
        })
        assertTrue(snapshot.traversedEdgeIds.any { edgeId ->
            graph.edges.any { it.id == edgeId && it.kind.name == "CONDITION" }
        })
        assertTrue(snapshot.traversedEdgeIds.any { edgeId ->
            graph.edges.any { it.id == edgeId && it.kind.name == "DATA_FLOW" }
        })
        assertTrue(FlowRuntimeSnapshotValidator.validate(graph, snapshot).isValid)
    }

    @Test
    fun mapsNestedFallbackTraceToConnectedBeepAndClickNodes() {
        val imported = EmscriptWorkspaceImporter().import(
            EditorDefaults.fallbackBranchTestScript,
            workspaceId = "runtime-flowchart-fallback",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val (irGraph, graph) = project(imported.document!!)
        val dryRun = WorkspaceDryRunRuntime().run(imported.document!!)
        assertTrue(dryRun is EmscriptDryRunResult.Success)

        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = irGraph,
            graph = graph,
            result = dryRun,
            sequence = 3,
            capturedAtEpochMs = 42,
        )
        val beepNode = graph.nodes.first { it.label == "BEEP 440Hz 60ms 35%" }
        val clickNode = graph.nodes.first { it.label == "CLICK \"fallback\"" }
        val nestedThenNode = graph.nodes.first { it.label == "LOG \"nested-then\"" }

        assertEquals(FlowRuntimeNodeState.SUCCEEDED, snapshot.nodeStates[beepNode.id])
        assertEquals(FlowRuntimeNodeState.SUCCEEDED, snapshot.nodeStates[clickNode.id])
        assertEquals(FlowRuntimeNodeState.SKIPPED, snapshot.nodeStates[nestedThenNode.id])
        assertTrue(snapshot.traversedEdgeIds.any { edgeId ->
            graph.edges.any {
                it.id == edgeId &&
                    it.sourceNodeId == beepNode.id &&
                    it.targetNodeId == clickNode.id
            }
        })
        assertTrue(FlowRuntimeSnapshotValidator.validate(graph, snapshot).isValid)
    }

    @Test
    fun mapsCapabilityWarningsToRuntimeDiagnostics() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            findTemplate("button.png", 0.8, 1000)
            Termux.shell("echo ok")
            """.trimIndent(),
            workspaceId = "runtime-flowchart-capabilities",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val (irGraph, graph) = project(imported.document!!)
        val dryRun = WorkspaceDryRunRuntime().run(imported.document!!)
        assertTrue(dryRun is EmscriptDryRunResult.Success)

        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = irGraph,
            graph = graph,
            result = dryRun,
            sequence = 8,
            capturedAtEpochMs = 42,
        )

        assertTrue(snapshot.diagnostics.any {
            it.severity.name == "WARNING" &&
                it.code == "CAPABILITY_VISION" &&
                it.message.contains("findTemplate")
        })
        assertTrue(snapshot.diagnostics.any {
            it.severity.name == "WARNING" &&
                it.code == "CAPABILITY_TERMUX" &&
                it.message.contains("Termux.shell")
        })
        val runtimeEvents = snapshot.runtimeEvents()
        assertTrue(runtimeEvents.any {
            it["severity"] == "WARNING" &&
                it["command"] == "findTemplate" &&
                it["capability"] == "VISION" &&
                it["pluginOwner"] == "visualtasker.core"
        })
        assertTrue(runtimeEvents.any {
            it["severity"] == "WARNING" &&
                it["command"] == "Termux.shell" &&
                it["capability"] == "TERMUX" &&
                it["pluginOwner"] == "visualtasker.termux"
        })
        assertTrue(FlowRuntimeSnapshotValidator.validate(graph, snapshot).isValid)
    }

    private fun project(document: WorkspaceDocument): Pair<IrGraph, de.visualtasker.flowchart.domain.FlowGraphDocument> {
        val irGraph = IrGraphGenerator().generate(document)
        return irGraph to IrGraphFlowchartProjector.project(irGraph).graph
    }

    private fun de.visualtasker.flowchart.domain.FlowRuntimeSnapshot.runtimeVariables(): Map<String, String> =
        extensions
            .firstOrNull { it.key == "visualtasker.runtime-variables" }
            ?.value
            ?.let { it as? FlowSemanticValue.ObjectValue }
            ?.values
            .orEmpty()
            .mapNotNull { (key, value) ->
                val rendered = (value as? FlowSemanticValue.StringValue)?.value ?: return@mapNotNull null
                key to rendered
            }
            .toMap()

    private fun de.visualtasker.flowchart.domain.FlowRuntimeSnapshot.runtimeEvents(): List<Map<String, String>> =
        extensions
            .firstOrNull { it.key == "visualtasker.runtime-events" }
            ?.value
            ?.let { it as? FlowSemanticValue.ListValue }
            ?.values
            .orEmpty()
            .mapNotNull { it as? FlowSemanticValue.ObjectValue }
            .map { event ->
                event.values.mapNotNull { (key, value) ->
                    val rendered = (value as? FlowSemanticValue.StringValue)?.value ?: return@mapNotNull null
                    key to rendered
                }.toMap()
            }
}
