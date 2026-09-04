package com.visualtasker.wss.visual.policy

import com.visualtasker.wss.visual.descriptor.BadgeRole
import com.visualtasker.wss.visual.descriptor.Emphasis
import com.visualtasker.wss.visual.descriptor.MotionRole
import com.visualtasker.wss.visual.descriptor.OutlineRole
import com.visualtasker.wss.visual.descriptor.ShapeRole
import com.visualtasker.wss.visual.descriptor.SurfaceRole
import com.visualtasker.wss.visual.semantics.ProjectionKind
import com.visualtasker.wss.visual.semantics.VisualActivity
import com.visualtasker.wss.visual.semantics.VisualAuthority
import com.visualtasker.wss.visual.semantics.VisualCertainty
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualFocus
import com.visualtasker.wss.visual.semantics.VisualRole
import com.visualtasker.wss.visual.semantics.VisualSemanticState
import com.visualtasker.wss.visual.semantics.VisualValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualPolicyResolverTest {
    @Test
    fun resolvesWorkflowConditionShapeByProjectionWithoutChangingMeaning() {
        val state = VisualSemanticState(role = VisualRole.WorkflowCondition)

        val flowDescriptor = DefaultVisualPolicyResolver.resolve(
            state,
            VisualContext(projection = ProjectionKind.Flowchart),
        )
        val inspectorDescriptor = DefaultVisualPolicyResolver.resolve(
            state,
            VisualContext(projection = ProjectionKind.Inspector),
        )

        assertEquals(ShapeRole.Diamond, flowDescriptor.shapeRole)
        assertEquals(ShapeRole.Row, inspectorDescriptor.shapeRole)
        assertEquals(SurfaceRole.Condition, flowDescriptor.surfaceRole)
        assertEquals(SurfaceRole.Condition, inspectorDescriptor.surfaceRole)
    }

    @Test
    fun prioritizesInvalidOverRuntimeAndSelectionSignals() {
        val state = VisualSemanticState(
            role = VisualRole.WorkflowAction,
            activity = VisualActivity.Running,
            validation = VisualValidation.Invalid,
            focus = VisualFocus.Selected,
        )

        val descriptor = DefaultVisualPolicyResolver.resolve(
            state,
            VisualContext(projection = ProjectionKind.BlockEditor),
        )

        assertEquals(OutlineRole.Invalid, descriptor.outlineRole)
        assertEquals(MotionRole.RuntimeFlow, descriptor.motionRole)
        assertEquals(Emphasis.Critical, descriptor.emphasis)
        assertTrue(descriptor.badges.any { it.role == BadgeRole.Running })
        assertTrue(descriptor.badges.any { it.role == BadgeRole.Invalid })
    }

    @Test
    fun keepsAiProposalAndAmbiguityAsDiscreteBadges() {
        val state = VisualSemanticState(
            role = VisualRole.WorldEntity,
            authority = VisualAuthority.AiProposed,
            certainty = VisualCertainty.Ambiguous,
            validation = VisualValidation.Warning,
        )

        val descriptor = DefaultVisualPolicyResolver.resolve(
            state,
            VisualContext(projection = ProjectionKind.SceneCanvas),
        )

        assertEquals(OutlineRole.Warning, descriptor.outlineRole)
        assertTrue(descriptor.badges.any { it.role == BadgeRole.AiProposal })
        assertTrue(descriptor.badges.any { it.role == BadgeRole.Ambiguous })
        assertTrue(descriptor.badges.any { it.role == BadgeRole.Warning })
    }
}
