package com.visualtasker.wss.visual.projections

import com.visualtasker.wss.visual.semantics.ProjectionKind
import com.visualtasker.wss.visual.semantics.VisualActivity
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualFocus
import com.visualtasker.wss.visual.semantics.VisualRole
import com.visualtasker.wss.visual.semantics.VisualValidation
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.asFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockEditorVisualAdaptersTest {
    private val factory = CompositeBlockRegistry().asFactory()

    @Test
    fun mapsBlockKindsToSharedVisualRoles() {
        val start = factory.create(BlockTypes.EVENT_START, BlockId("start"))!!
        val action = factory.create(BlockTypes.ACTION_WAIT, BlockId("wait"))!!
        val condition = factory.create(BlockTypes.CONTROL_IF, BlockId("if"))!!
        val value = factory.create(BlockTypes.LOGIC_COMPARE, BlockId("compare"))!!

        assertEquals(VisualRole.WorkflowEvent, start.visualRole())
        assertEquals(VisualRole.WorkflowAction, action.visualRole())
        assertEquals(VisualRole.WorkflowCondition, condition.visualRole())
        assertEquals(VisualRole.WorkflowValue, value.visualRole())
    }

    @Test
    fun keepsFocusValidationAndRuntimeAsSeparateAxes() {
        val block = factory.create(BlockTypes.ACTION_WAIT, BlockId("wait"))!!

        val state = BlockEditorBlockVisualAdapter.map(
            BlockEditorVisualSubject(
                block = block,
                selectedBlockId = block.id,
                warningBlockIds = setOf(block.id),
                runningBlockIds = setOf(block.id),
            ),
            VisualContext(projection = ProjectionKind.BlockEditor),
        )

        assertEquals(VisualRole.WorkflowAction, state.role)
        assertEquals(VisualFocus.Selected, state.focus)
        assertEquals(VisualValidation.Warning, state.validation)
        assertEquals(VisualActivity.Running, state.activity)
    }

    private fun de.visualtasker.blockeditor.domain.BlockNode.visualRole(): VisualRole =
        BlockEditorBlockVisualAdapter.map(
            BlockEditorVisualSubject(this),
            VisualContext(projection = ProjectionKind.BlockEditor),
        ).role
}
