package com.visualtasker.wss.flowchart

import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.domain.ConnectionId
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.ir.IrGraphGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.blockeditor.registry.asFactory
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrGraphFlowchartProjectorTest {
    @Test
    fun `ir projection exposes editor ports field properties scopes branches and source mapping`() {
        val workspace = buildWorkspace()
        val irGraph = IrGraphGenerator().generate(workspace)
        val result = IrGraphFlowchartProjector.project(irGraph)
        val graph = result.graph
        val ifId = workspace.blocks.entries.first { it.value.type == BlockTypes.CONTROL_IF_ELSE }.key
        val waitId = workspace.blocks.entries.first { it.value.type == BlockTypes.ACTION_WAIT }.key
        val boolId = workspace.blocks.entries.first { it.value.type == BlockTypes.LOGIC_BOOLEAN }.key
        val ifNode = graph.nodes.first { it.id.value == "block:${ifId.value}" }
        val waitNode = graph.nodes.first { it.id.value == "block:${waitId.value}" }
        val boolNode = graph.nodes.first { it.id.value == "block:${boolId.value}" }

        assertEquals(FlowchartProjectionStatus.RUNNING, result.status)
        assertEquals("ir-graph", graph.extensions.stringExtension("visualtasker.projection-source"))
        assertTrue("previous" in portNames(ifNode.properties.getValue("inputPorts")))
        assertTrue("CONDITION" in portNames(ifNode.properties.getValue("inputPorts")))
        assertTrue(BlockTypes.SLOT_THEN in portNames(ifNode.properties.getValue("outputPorts")))
        assertTrue(BlockTypes.SLOT_ELSE in portNames(ifNode.properties.getValue("outputPorts")))
        assertTrue("output" in portNames(boolNode.properties.getValue("outputPorts")))
        assertEquals("250.0", (waitNode.properties.getValue("waitMs") as FlowSemanticValue.NumberValue).canonicalValue)
        assertNotNull(ifNode.sourceReference)
        assertTrue(ifNode.sourceReference!!.canonicalText.orEmpty().contains("block:${ifId.value}"))
        assertTrue(graph.edges.any { it.kind == FlowEdgeKind.CONDITION && it.label == "CONDITION" })
        assertTrue(graph.edges.any { it.kind == FlowEdgeKind.TRUE_BRANCH && it.label == BlockTypes.SLOT_THEN })
        assertTrue(graph.extensions.listExtension("visualtasker.ir-scopes").isNotEmpty())
        assertTrue(graph.extensions.listExtension("visualtasker.ir-branches").isNotEmpty())
        assertTrue(graph.extensions.listExtension("visualtasker.ir-facets").isNotEmpty())
        assertTrue(graph.nodes.any { node ->
            node.kind.standard == FlowNodeKind.ANNOTATION &&
                node.properties["visualFacet"] == FlowSemanticValue.BooleanValue(true) &&
                node.properties["facetKind"] == FlowSemanticValue.StringValue("BRANCH_REGION")
        })
        val joinNode = graph.nodes.firstOrNull { it.properties["syntheticJoin"] == FlowSemanticValue.BooleanValue(true) }
        assertNotNull(joinNode)
        assertEquals("block:${ifId.value}", (joinNode!!.properties["ownerNodeId"] as FlowSemanticValue.StringValue).value)
        assertTrue(graph.edges.any { it.targetNodeId == joinNode.id && it.id.value.startsWith("join:block:${ifId.value}:") })
    }

    @Test
    fun `ir facets are projected as visual flowchart nodes`() {
        val factory = DefaultBlockRegistry.asFactory()
        var doc = WorkspaceBootstrap.empty()

        fun instantiate(type: String): BlockId {
            val before = doc.blocks.keys
            doc = WorkspaceReducer.reduce(doc, WorkspaceAction.InstantiateBlock(type, 0f, 0f), factory)
            return (doc.blocks.keys - before).single()
        }

        val firstVariable = instantiate(BlockTypes.VARIABLE_SET)
        instantiate(BlockTypes.VARIABLE_SET)
        doc = WorkspaceReducer.reduce(doc, WorkspaceAction.Collapse(firstVariable), factory)

        val graph = IrGraphFlowchartProjector.project(IrGraphGenerator().generate(doc)).graph
        val facetNodes = graph.nodes.filter { it.properties["visualFacet"] == FlowSemanticValue.BooleanValue(true) }

        assertTrue(facetNodes.any { node ->
            node.kind.standard == FlowNodeKind.SYNTHETIC &&
                node.properties["facetKind"] == FlowSemanticValue.StringValue("COLLAPSE_GROUP") &&
                node.properties["collapsed"] == FlowSemanticValue.BooleanValue(true) &&
                node.properties["flowFacet"] == FlowSemanticValue.BooleanValue(true) &&
                node.properties["flowFacetRole"] == FlowSemanticValue.StringValue("collapse.group") &&
                node.properties["flowFacetNodeCount"] == FlowSemanticValue.NumberValue("1")
        })
        assertTrue(facetNodes.any { node ->
            node.kind.standard == FlowNodeKind.SYNTHETIC &&
                node.properties["facetKind"] == FlowSemanticValue.StringValue("VARIABLE_BULK") &&
                node.properties["flowFacetRole"] == FlowSemanticValue.StringValue("bulk.variables")
        })
        assertTrue(facetNodes.all { node ->
            node.extensions.any { it.key == "visualtasker.ir-facet-nodes" }
        })
    }

    private fun buildWorkspace(): WorkspaceDocument {
        val factory = DefaultBlockRegistry.asFactory()
        var doc = WorkspaceBootstrap.empty()

        fun instantiate(type: String): BlockId {
            val before = doc.blocks.keys
            doc = WorkspaceReducer.reduce(doc, WorkspaceAction.InstantiateBlock(type, 0f, 0f), factory)
            return (doc.blocks.keys - before).single()
        }

        val startId = instantiate(BlockTypes.EVENT_START)
        val ifId = instantiate(BlockTypes.CONTROL_IF_ELSE)
        val boolId = instantiate(BlockTypes.LOGIC_BOOLEAN)
        val waitId = instantiate(BlockTypes.ACTION_WAIT)
        val logId = instantiate(BlockTypes.DEBUG_LOG)

        doc = WorkspaceReducer.reduce(doc, WorkspaceAction.UpdateField(waitId, "ms", FieldValue.Number(250.0)), factory)
        connect(doc, startId, "next", ifId, "previous").also { doc = it }
        connect(doc, boolId, "output", ifId, "CONDITION").also { doc = it }
        connect(doc, ifId, BlockTypes.SLOT_THEN, waitId, "previous").also { doc = it }
        connect(doc, ifId, BlockTypes.SLOT_ELSE, logId, "previous").also { doc = it }
        return doc
    }

    private fun connect(
        document: WorkspaceDocument,
        source: BlockId,
        sourceConn: String,
        target: BlockId,
        targetConn: String,
    ): WorkspaceDocument = WorkspaceReducer.reduce(
        document,
        WorkspaceAction.Connect(
            connectionId(document, source, sourceConn),
            connectionId(document, target, targetConn),
        ),
        DefaultBlockRegistry.asFactory(),
    )

    private fun connectionId(
        document: WorkspaceDocument,
        blockId: BlockId,
        key: String,
    ): ConnectionId {
        val block = document.blocks.getValue(blockId)
        return when (key) {
            "previous" -> block.previous!!.id
            "next" -> block.next!!.id
            "output" -> block.output!!.id
            else -> block.valueInputs.firstOrNull { it.name == key }?.connection?.id
                ?: block.statementInputs.first { it.name == key }.connection.id
        }
    }

    private fun portNames(value: FlowSemanticValue): Set<String> {
        val list = value as FlowSemanticValue.ListValue
        return list.values.mapNotNull { item ->
            ((item as? FlowSemanticValue.ObjectValue)?.values?.get("name") as? FlowSemanticValue.StringValue)?.value
        }.toSet()
    }

    private fun List<de.visualtasker.flowchart.domain.FlowGraphExtension>.stringExtension(key: String): String? =
        firstOrNull { it.key == key }
            ?.value
            ?.let { it as? FlowSemanticValue.StringValue }
            ?.value

    private fun List<de.visualtasker.flowchart.domain.FlowGraphExtension>.listExtension(key: String): List<FlowSemanticValue> =
        firstOrNull { it.key == key }
            ?.value
            ?.let { it as? FlowSemanticValue.ListValue }
            ?.values
            .orEmpty()
}
