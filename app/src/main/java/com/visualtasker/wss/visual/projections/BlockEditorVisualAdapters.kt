package com.visualtasker.wss.visual.projections

import com.visualtasker.wss.visual.semantics.VisualActivity
import com.visualtasker.wss.visual.semantics.VisualAvailability
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualFocus
import com.visualtasker.wss.visual.semantics.VisualRole
import com.visualtasker.wss.visual.semantics.VisualSemanticAdapter
import com.visualtasker.wss.visual.semantics.VisualSemanticState
import com.visualtasker.wss.visual.semantics.VisualValidation
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.domain.BlockNode
import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CommandCatalogKind
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog

data class BlockEditorVisualSubject(
    val block: BlockNode,
    val selectedBlockId: BlockId? = null,
    val focusedBlockId: BlockId? = null,
    val invalidBlockIds: Set<BlockId> = emptySet(),
    val warningBlockIds: Set<BlockId> = emptySet(),
    val runningBlockIds: Set<BlockId> = emptySet(),
)

object BlockEditorBlockVisualAdapter : VisualSemanticAdapter<BlockEditorVisualSubject> {
    override fun map(value: BlockEditorVisualSubject, context: VisualContext): VisualSemanticState =
        VisualSemanticState(
            role = value.block.toVisualRole(),
            activity = if (value.block.id in value.runningBlockIds) VisualActivity.Running else VisualActivity.Idle,
            validation = when (value.block.id) {
                in value.invalidBlockIds -> VisualValidation.Invalid
                in value.warningBlockIds -> VisualValidation.Warning
                else -> VisualValidation.Valid
            },
            focus = when (value.block.id) {
                value.selectedBlockId -> VisualFocus.Selected
                value.focusedBlockId -> VisualFocus.Focused
                else -> VisualFocus.None
            },
            availability = if (value.block.metadata["disabled"] == "true") {
                VisualAvailability.Disabled
            } else {
                VisualAvailability.Enabled
            },
        )

    private fun BlockNode.toVisualRole(): VisualRole {
        val entry = VisualTaskerCommandCatalog.findByBlockType(type)
        return when {
            type == BlockTypes.EVENT_START -> VisualRole.WorkflowEvent
            type in setOf(
                BlockTypes.CONTROL_REPEAT,
                BlockTypes.CONTROL_WHILE,
            ) -> VisualRole.WorkflowLoop
            type in setOf(
                BlockTypes.CONTROL_IF,
                BlockTypes.CONTROL_IF_ELSE,
                BlockTypes.CONTROL_IF_ELSEIF_ELSE,
            ) -> VisualRole.WorkflowCondition
            output != null -> VisualRole.WorkflowValue
            entry?.kind == CommandCatalogKind.EVENT -> VisualRole.WorkflowEvent
            entry?.kind == CommandCatalogKind.CONTROL -> VisualRole.WorkflowCondition
            entry?.kind == CommandCatalogKind.REPORTER ||
                entry?.kind == CommandCatalogKind.OPERATOR -> VisualRole.WorkflowValue
            entry?.kind == CommandCatalogKind.VARIABLE && output != null -> VisualRole.WorkflowValue
            entry?.kind == CommandCatalogKind.VARIABLE -> VisualRole.WorkflowAction
            entry?.category == BlockCategories.DEBUG -> VisualRole.RuntimeEvent
            entry?.kind == CommandCatalogKind.STATEMENT -> VisualRole.WorkflowAction
            else -> VisualRole.Unknown
        }
    }
}
