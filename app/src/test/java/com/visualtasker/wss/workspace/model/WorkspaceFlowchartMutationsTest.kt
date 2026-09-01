package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.asFactory
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
}
