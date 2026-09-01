package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.asFactory
import com.visualtasker.wss.flowchart.BlockEditorFlowchartProjector
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowDocumentId
import de.visualtasker.flowchart.domain.FlowDocumentRevision
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeView
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowSurfaceId
import de.visualtasker.flowchart.domain.FlowViewDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceFlowchartMutationsTest {
    @Test
    fun `add flowchart node instantiates workspace block`() {
        val document = WorkspaceDocument(id = "flowchart-add-test")

        val updated = addFlowchartNodeToWorkspace(document, BlockTypes.ACTION_WAIT)

        assertNotEquals(document, updated)
        assertEquals(1, updated.blocks.size)
        assertEquals(1, updated.rootBlocks.size)
        assertTrue(updated.blocks.values.any { it.type == BlockTypes.ACTION_WAIT })
    }

    @Test
    fun `unknown flowchart node definition leaves document unchanged`() {
        val document = WorkspaceDocument(id = "flowchart-add-test")

        val updated = addFlowchartNodeToWorkspace(document, "missing.flowchart.node")

        assertEquals(document, updated)
    }

    @Test
    fun `delete flowchart node removes projected workspace block`() {
        val document = addFlowchartNodeToWorkspace(
            WorkspaceDocument(id = "flowchart-delete-test"),
            BlockTypes.ACTION_WAIT,
        )
        val blockId = document.blocks.keys.single()

        val updated = deleteFlowchartNodeFromWorkspace(document, FlowNodeId("block:${blockId.value}"))

        assertNotEquals(document, updated)
        assertFalse(blockId in updated.blocks)
        assertTrue(updated.rootBlocks.isEmpty())
    }

    @Test
    fun `delete non workspace flowchart node leaves document unchanged`() {
        val document = addFlowchartNodeToWorkspace(
            WorkspaceDocument(id = "flowchart-delete-test"),
            BlockTypes.ACTION_WAIT,
        )

        val updated = deleteFlowchartNodeFromWorkspace(document, FlowNodeId("runtime:external"))

        assertEquals(document, updated)
    }

    @Test
    fun `disconnect flowchart sequence edge removes workspace next connection`() {
        val document = twoConnectedStatementBlocks()
        val graph = BlockEditorFlowchartProjector.project(document).graph
        val edge = graph.edges.single { it.kind == FlowEdgeKind.SEQUENCE }

        val updated = disconnectFlowchartEdgeFromWorkspace(document, graph, edge.id)

        assertNotEquals(document, updated)
        assertTrue(updated.blocks.values.all { block ->
            block.next?.connectedTo == null && block.previous?.connectedTo == null
        })
        assertEquals(2, updated.rootBlocks.size)
    }

    @Test
    fun `disconnect flowchart branch edge removes workspace statement slot connection`() {
        var document = instantiate(WorkspaceDocument(id = "flowchart-branch-disconnect-test"), BlockTypes.CONTROL_REPEAT, 10f, 20f)
        val repeatId = document.blocks.keys.single()
        document = instantiate(document, BlockTypes.ACTION_WAIT, 40f, 80f)
        val waitId = document.blocks.keys.single { it != repeatId }
        val repeatDo = document.blocks.getValue(repeatId).statementInputs.single { it.name == BlockTypes.SLOT_DO }.connection.id
        val waitPrevious = document.blocks.getValue(waitId).previous!!.id
        document = WorkspaceReducer.reduce(document, WorkspaceAction.Connect(repeatDo, waitPrevious))
        val graph = BlockEditorFlowchartProjector.project(document).graph
        val edge = graph.edges.single { it.kind == FlowEdgeKind.LOOP_BODY }

        val updated = disconnectFlowchartEdgeFromWorkspace(document, graph, edge.id)

        assertNotEquals(document, updated)
        assertEquals(null, updated.blocks.getValue(repeatId).statementInputs.single { it.name == BlockTypes.SLOT_DO }.connection.connectedTo)
        assertEquals(null, updated.blocks.getValue(waitId).previous?.connectedTo)
        assertTrue(waitId in updated.rootBlocks)
    }

    @Test
    fun `disconnect flowchart data edge removes workspace value connection`() {
        var document = instantiate(WorkspaceDocument(id = "flowchart-data-disconnect-test"), BlockTypes.LOGIC_COMPARE, 10f, 20f)
        val compareId = document.blocks.keys.single()
        document = instantiate(document, BlockTypes.LITERAL_NUMBER, 40f, 80f)
        val numberId = document.blocks.keys.single { it != compareId }
        val numberOutput = document.blocks.getValue(numberId).output!!.id
        val compareLeft = document.blocks.getValue(compareId).valueInputs.single { it.name == "LEFT" }.connection.id
        document = WorkspaceReducer.reduce(document, WorkspaceAction.Connect(numberOutput, compareLeft))
        val graph = BlockEditorFlowchartProjector.project(document).graph
        val edge = graph.edges.single { it.kind == FlowEdgeKind.DATA_FLOW }

        val updated = disconnectFlowchartEdgeFromWorkspace(document, graph, edge.id)

        assertNotEquals(document, updated)
        assertEquals(null, updated.blocks.getValue(compareId).valueInputs.single { it.name == "LEFT" }.connection.connectedTo)
        assertEquals(null, updated.blocks.getValue(numberId).output?.connectedTo)
        assertTrue(numberId in updated.rootBlocks)
    }

    @Test
    fun `connect flowchart sequence edge creates workspace next connection`() {
        var document = instantiate(WorkspaceDocument(id = "flowchart-sequence-connect-test"), BlockTypes.ACTION_WAIT, 10f, 20f)
        val firstId = document.blocks.keys.single()
        document = instantiate(document, BlockTypes.ACTION_CLICK_TEXT, 40f, 80f)
        val secondId = document.blocks.keys.single { it != firstId }

        val updated = connectFlowchartNodesInWorkspace(
            document = document,
            sourceNodeId = FlowNodeId("block:${firstId.value}"),
            targetNodeId = FlowNodeId("block:${secondId.value}"),
            kind = FlowEdgeKind.SEQUENCE,
        )

        assertNotEquals(document, updated)
        assertEquals(
            updated.blocks.getValue(secondId).previous!!.id,
            updated.blocks.getValue(firstId).next!!.connectedTo,
        )
        assertFalse(secondId in updated.rootBlocks)
    }

    @Test
    fun `connect flowchart branch edge creates workspace statement slot connection`() {
        var document = instantiate(WorkspaceDocument(id = "flowchart-branch-connect-test"), BlockTypes.CONTROL_REPEAT, 10f, 20f)
        val repeatId = document.blocks.keys.single()
        document = instantiate(document, BlockTypes.ACTION_WAIT, 40f, 80f)
        val waitId = document.blocks.keys.single { it != repeatId }

        val updated = connectFlowchartNodesInWorkspace(
            document = document,
            sourceNodeId = FlowNodeId("block:${repeatId.value}"),
            targetNodeId = FlowNodeId("block:${waitId.value}"),
            kind = FlowEdgeKind.LOOP_BODY,
        )

        assertNotEquals(document, updated)
        assertEquals(
            updated.blocks.getValue(waitId).previous!!.id,
            updated.blocks.getValue(repeatId).statementInputs.single { it.name == BlockTypes.SLOT_DO }.connection.connectedTo,
        )
        assertFalse(waitId in updated.rootBlocks)
    }

    @Test
    fun `connect flowchart condition edge creates workspace value connection`() {
        var document = instantiate(WorkspaceDocument(id = "flowchart-condition-connect-test"), BlockTypes.LITERAL_BOOLEAN, 10f, 20f)
        val booleanId = document.blocks.keys.single()
        document = instantiate(document, BlockTypes.CONTROL_IF, 40f, 80f)
        val ifId = document.blocks.keys.single { it != booleanId }

        val updated = connectFlowchartNodesInWorkspace(
            document = document,
            sourceNodeId = FlowNodeId("block:${booleanId.value}"),
            targetNodeId = FlowNodeId("block:${ifId.value}"),
            kind = FlowEdgeKind.CONDITION,
        )

        assertNotEquals(document, updated)
        assertEquals(
            updated.blocks.getValue(ifId).valueInputs.single { it.name == "CONDITION" }.connection.id,
            updated.blocks.getValue(booleanId).output!!.connectedTo,
        )
        assertFalse(booleanId in updated.rootBlocks)
    }

    @Test
    fun `connect incompatible flowchart data edge leaves workspace unchanged`() {
        var document = instantiate(WorkspaceDocument(id = "flowchart-incompatible-connect-test"), BlockTypes.ACTION_WAIT, 10f, 20f)
        val waitId = document.blocks.keys.single()
        document = instantiate(document, BlockTypes.CONTROL_IF, 40f, 80f)
        val ifId = document.blocks.keys.single { it != waitId }

        val updated = connectFlowchartNodesInWorkspace(
            document = document,
            sourceNodeId = FlowNodeId("block:${waitId.value}"),
            targetNodeId = FlowNodeId("block:${ifId.value}"),
            kind = FlowEdgeKind.CONDITION,
        )

        assertEquals(document, updated)
    }

    @Test
    fun `sync root positions from flowchart view updates workspace roots`() {
        val document = addFlowchartNodeToWorkspace(
            WorkspaceDocument(id = "flowchart-move-test"),
            BlockTypes.ACTION_WAIT,
        )
        val blockId = document.blocks.keys.single()

        val updated = syncRootPositionsFromFlowchartView(
            document,
            flowView("block:${blockId.value}", x = 240.0, y = 320.0),
        )

        assertNotEquals(document, updated)
        assertEquals(240f, updated.rootPositions.getValue(blockId).x)
        assertEquals(320f, updated.rootPositions.getValue(blockId).y)
    }

    @Test
    fun `sync root positions ignores nested workspace blocks`() {
        var document = WorkspaceDocument(id = "flowchart-nested-move-test")
        document = WorkspaceReducer.reduce(
            document,
            WorkspaceAction.InstantiateBlock(BlockTypes.CONTROL_REPEAT, 10f, 20f),
            DefaultBlockRegistry.asFactory(),
        )
        val repeatId = document.blocks.keys.single()
        document = WorkspaceReducer.reduce(
            document,
            WorkspaceAction.InstantiateBlock(BlockTypes.ACTION_WAIT, 40f, 80f),
            DefaultBlockRegistry.asFactory(),
        )
        val waitId = document.blocks.keys.single { it != repeatId }
        val repeatDo = document.blocks.getValue(repeatId).statementInputs.single { it.name == BlockTypes.SLOT_DO }.connection.id
        val waitPrevious = document.blocks.getValue(waitId).previous!!.id
        document = WorkspaceReducer.reduce(document, WorkspaceAction.Connect(repeatDo, waitPrevious))

        val updated = syncRootPositionsFromFlowchartView(
            document,
            flowView("block:${waitId.value}", x = 300.0, y = 360.0),
        )

        assertEquals(document, updated)
    }

    private fun flowView(
        nodeId: String,
        x: Double,
        y: Double,
    ): FlowViewDocument = FlowViewDocument(
        documentId = FlowDocumentId("test-flow"),
        compatibleDocumentRevision = FlowDocumentRevision("1"),
        surfaceId = FlowSurfaceId("test-surface"),
        nodeViews = listOf(
            FlowNodeView(
                nodeId = FlowNodeId(nodeId),
                position = FlowPoint(x, y),
            )
        ),
    )

    private fun twoConnectedStatementBlocks(): WorkspaceDocument {
        var document = instantiate(WorkspaceDocument(id = "flowchart-sequence-disconnect-test"), BlockTypes.ACTION_WAIT, 10f, 20f)
        val firstId = document.blocks.keys.single()
        document = instantiate(document, BlockTypes.ACTION_WAIT, 40f, 80f)
        val secondId = document.blocks.keys.single { it != firstId }
        return WorkspaceReducer.reduce(
            document,
            WorkspaceAction.Connect(
                document.blocks.getValue(firstId).next!!.id,
                document.blocks.getValue(secondId).previous!!.id,
            ),
        )
    }

    private fun instantiate(
        document: WorkspaceDocument,
        type: String,
        x: Float,
        y: Float,
    ): WorkspaceDocument = WorkspaceReducer.reduce(
        document,
        WorkspaceAction.InstantiateBlock(type, x, y),
        DefaultBlockRegistry.asFactory(),
    )
}
