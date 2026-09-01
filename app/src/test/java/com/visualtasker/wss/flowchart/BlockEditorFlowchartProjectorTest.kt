package com.visualtasker.wss.flowchart

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.domain.VariableDefinition
import de.visualtasker.blockeditor.domain.VariableScope
import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.blockeditor.registry.asFactory
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockEditorFlowchartProjectorTest {
    @Test
    fun registryDefinitions_exposeCompareAndVariableReporterFieldContracts() {
        val compare = DefaultBlockRegistry.getDefinition(BlockTypes.LOGIC_COMPARE)!!
        assertEquals("logic.compare", compare.id)
        assertEquals(listOf("operator"), compare.fields.map { it.key })
        assertEquals(setOf("LEFT", "RIGHT"), compare.valueInputs.map { it.name }.toSet())

        val variable = VariableDefinition("score", "Score", "Number", VariableScope.Global)
        val reporter = VariableReporterFactory.create(variable)
        assertEquals("variable.reporter.score", reporter.id)
        assertTrue(reporter.fields.map { it.key }.contains("variable"))
    }

    @Test
    fun projectsReferenceGraph_withoutUnsupportedNodes_andWithDataAndConditionEdges() {
        val workspace = buildReferenceWorkspace()
        val result = BlockEditorFlowchartProjector.project(workspace)

        assertEquals(FlowchartProjectionStatus.RUNNING, result.status)
        assertTrue(result.graph.diagnostics.isEmpty())
        assertFalse(result.graph.nodes.any { it.kind.standard == FlowNodeKind.UNKNOWN_SOURCE })
        assertTrue(result.graph.nodes.any { it.label == "IF" })
        assertTrue(result.graph.nodes.any { it.label == "ADD" })
        assertTrue(result.graph.nodes.any { it.label == "COMPARE >=" })
        assertTrue(result.graph.nodes.any { it.label == "v1" })
        assertTrue(result.graph.nodes.any { it.label == "v2" })
        assertTrue(result.graph.nodes.any { it.label == "v3" })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.DATA_FLOW && it.label == "Input1" })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.DATA_FLOW && it.label == "Input2" })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.CONDITION && it.label == "CONDITION" })
    }

    @Test
    fun saveLoadRoundtrip_keepsFlowchartSemanticsStable() {
        val workspace = buildReferenceWorkspace()
        val saved = WorkspaceSerializer.serialize(workspace)
        val loaded = WorkspaceSerializer.deserialize(saved)
        val first = BlockEditorFlowchartProjector.project(workspace).graph
        val second = BlockEditorFlowchartProjector.project(loaded).graph

        assertEquals(first.nodes.map { it.id to it.label }, second.nodes.map { it.id to it.label })
        assertEquals(
            first.edges.map { "${it.sourceNodeId.value}|${it.targetNodeId.value}|${it.kind}|${it.label.orEmpty()}" }.sorted(),
            second.edges.map { "${it.sourceNodeId.value}|${it.targetNodeId.value}|${it.kind}|${it.label.orEmpty()}" }.sorted(),
        )
    }

    @Test
    fun changingOperatorAndVariableLabel_updatesProjection_butKeepsVariableIdentity() {
        var workspace = buildReferenceWorkspace()
        val compareId = workspace.blocks.entries.first { it.value.type == BlockTypes.LOGIC_COMPARE }.key
        val variableOneId = workspace.blocks.entries.first {
            it.value.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX) && it.value.fields["variable"] == FieldValue.Text("v1")
        }.key
        val factory = buildRegistry().asFactory()

        workspace = WorkspaceReducer.reduce(
            workspace,
            WorkspaceAction.UpdateField(compareId, "operator", FieldValue.Text("LESS_OR_EQUAL")),
            factory,
        )
        workspace = WorkspaceReducer.reduce(
            workspace,
            WorkspaceAction.UpdateField(variableOneId, "variableLabel", FieldValue.Text("temperature")),
            factory,
        )
        val projected = BlockEditorFlowchartProjector.project(workspace).graph
        val variableNode = projected.nodes.first { it.id.value == "block:${variableOneId.value}" }

        assertEquals("COMPARE <=", projected.nodes.first { it.id.value == "block:${compareId.value}" }.label)
        assertEquals("temperature", variableNode.label)
        assertEquals("v1", variableNode.properties["variableId"]?.let { (it as de.visualtasker.flowchart.domain.FlowSemanticValue.StringValue).value })
    }

    @Test
    fun detachAndReattachValueEdge_updatesProjectionWithoutOrphans() {
        val factoryRegistry = buildRegistry()
        val factory = factoryRegistry.asFactory()
        var workspace = buildReferenceWorkspace(factoryRegistry)
        val addId = workspace.blocks.entries.first { it.value.type == BlockTypes.LOGIC_OPERATE && it.value.fields["operator"] == FieldValue.Text("ADD") }.key
        val v1Id = workspace.blocks.entries.first {
            it.value.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX) && it.value.fields["variable"] == FieldValue.Text("v1")
        }.key
        val addInput1 = workspace.blocks[addId]!!.valueInputs.first { it.name == "Input1" }.connection.id
        val v1Output = workspace.blocks[v1Id]!!.output!!.id

        workspace = WorkspaceReducer.reduce(workspace, WorkspaceAction.Disconnect(v1Output), factory)
        val detached = BlockEditorFlowchartProjector.project(workspace).graph
        assertFalse(detached.edges.any { it.kind == FlowEdgeKind.DATA_FLOW && it.label == "Input1" && it.sourceNodeId.value == "block:${v1Id.value}" })

        workspace = WorkspaceReducer.reduce(workspace, WorkspaceAction.Connect(v1Output, addInput1), factory)
        val reattached = BlockEditorFlowchartProjector.project(workspace).graph
        assertTrue(reattached.edges.any { it.kind == FlowEdgeKind.DATA_FLOW && it.label == "Input1" && it.sourceNodeId.value == "block:${v1Id.value}" })
        assertFalse(reattached.edges.groupBy { it.id.value }.values.any { it.size > 1 })
    }

    @Test
    fun literalReporterNodes_areProjectedWithSemanticValues() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            LET sum = 5 + 3
            IF sum >= 7
            END IF
            """.trimIndent(),
            workspaceId = "literal-flow",
        )
        assertTrue(imported.isSuccess)
        val graph = BlockEditorFlowchartProjector.project(imported.document!!).graph

        val numberNodes = graph.nodes.filter { it.properties["blockType"] == FlowSemanticValue.StringValue(BlockTypes.LITERAL_NUMBER) }
        assertTrue(numberNodes.isNotEmpty())
        assertTrue(numberNodes.any { it.label.startsWith("NUM ") })
    }

    @Test
    fun integrationTestScript_projectsSupportedCommandsWithoutDiagnostics() {
        val imported = EmscriptWorkspaceImporter()
            .import(EditorDefaults.integrationTestScript, workspaceId = "flowchart-integration")
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val result = BlockEditorFlowchartProjector.project(imported.document!!)
        val labels = result.graph.nodes.map { it.label }

        assertEquals(FlowchartProjectionStatus.RUNNING, result.status)
        assertTrue(result.graph.diagnostics.joinToString { it.message }, result.graph.diagnostics.isEmpty())
        assertTrue(labels.any { it.startsWith("REPEAT 10x") })
        assertTrue(labels.any { it == "IF" })
        assertTrue(labels.any { it.startsWith("WAIT ") })
        assertTrue(labels.any { it.startsWith("CLICK ") })
        assertTrue(labels.any { it.startsWith("LOG ") })
        assertTrue(labels.any { it.startsWith("BEEP ") })
        assertTrue(labels.any { it.startsWith("VIBRATE ") })
        assertTrue(labels.any { it.startsWith("COMPARE ") })
        assertTrue(labels.any { it in setOf("ADD", "SUB", "MUL", "DIV", "MOD") })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.TRUE_BRANCH })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.ELSE_IF_BRANCH })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.FALSE_BRANCH })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.LOOP_BODY })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.LOOP_EXIT })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.CONDITION })
        assertTrue(result.graph.edges.any { it.kind == FlowEdgeKind.DATA_FLOW })
    }

    private fun buildReferenceWorkspace(registry: CompositeBlockRegistry = buildRegistry()): WorkspaceDocument {
        val factory = registry.asFactory()
        var doc = WorkspaceBootstrap.empty()
        listOf(
            VariableDefinition("v1", "v1", "Number", VariableScope.Global),
            VariableDefinition("v2", "v2", "Number", VariableScope.Global),
            VariableDefinition("v3", "v3", "Number", VariableScope.Global),
        ).forEach { variable ->
            doc = WorkspaceReducer.reduce(doc, WorkspaceAction.CreateVariable(variable), factory)
        }

        fun instantiate(type: String, x: Float, y: Float): BlockId {
            doc = WorkspaceReducer.reduce(doc, WorkspaceAction.InstantiateBlock(type, x, y), factory)
            return doc.rootBlocks.last()
        }

        val startId = instantiate(BlockTypes.EVENT_START, 40f, 40f)
        val ifId = instantiate(BlockTypes.CONTROL_IF, 120f, 100f)
        val addId = instantiate(BlockTypes.LOGIC_OPERATE, 240f, 160f)
        val compareId = instantiate(BlockTypes.LOGIC_COMPARE, 360f, 160f)
        val v1Id = instantiate(VariableReporterFactory.reporterId("v1"), 120f, 260f)
        val v2Id = instantiate(VariableReporterFactory.reporterId("v2"), 220f, 260f)
        val v3Id = instantiate(VariableReporterFactory.reporterId("v3"), 320f, 260f)

        fun connect(source: BlockId, sourceConn: String, target: BlockId, targetConn: String) {
            val sourceId = connectionId(doc, source, sourceConn)
            val targetId = connectionId(doc, target, targetConn)
            doc = WorkspaceReducer.reduce(doc, WorkspaceAction.Connect(sourceId, targetId), factory)
        }

        doc = WorkspaceReducer.reduce(doc, WorkspaceAction.UpdateField(addId, "operator", FieldValue.Text("ADD")), factory)
        doc = WorkspaceReducer.reduce(doc, WorkspaceAction.UpdateField(compareId, "operator", FieldValue.Text("GREATER_OR_EQUAL")), factory)

        connect(startId, "next", ifId, "previous")
        connect(v1Id, "output", addId, "Input1")
        connect(v2Id, "output", addId, "Input2")
        connect(addId, "output", compareId, "LEFT")
        connect(v3Id, "output", compareId, "RIGHT")
        connect(compareId, "output", ifId, "CONDITION")
        return doc
    }

    private fun buildRegistry(): CompositeBlockRegistry {
        val registry = CompositeBlockRegistry(DefaultBlockRegistry)
        listOf(
            VariableDefinition("v1", "v1", "Number", VariableScope.Global),
            VariableDefinition("v2", "v2", "Number", VariableScope.Global),
            VariableDefinition("v3", "v3", "Number", VariableScope.Global),
        ).forEach { registry.register(VariableReporterFactory.create(it)) }
        return registry
    }

    private fun connectionId(
        document: WorkspaceDocument,
        blockId: BlockId,
        key: String,
    ): de.visualtasker.blockeditor.domain.ConnectionId {
        val block = document.blocks.getValue(blockId)
        return when (key) {
            "previous" -> block.previous!!.id
            "next" -> block.next!!.id
            "output" -> block.output!!.id
            else -> block.valueInputs.firstOrNull { it.name == key }?.connection?.id
                ?: block.statementInputs.first { it.name == key }.connection.id
        }
    }
}
