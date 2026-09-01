package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.registry.BlockTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
