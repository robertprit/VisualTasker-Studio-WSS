package com.visualtasker.wss.visual.policy

import com.visualtasker.wss.visual.descriptor.BadgeRole
import com.visualtasker.wss.visual.descriptor.Emphasis
import com.visualtasker.wss.visual.descriptor.MotionRole
import com.visualtasker.wss.visual.descriptor.OpacityRole
import com.visualtasker.wss.visual.descriptor.OutlineRole
import com.visualtasker.wss.visual.descriptor.ShapeRole
import com.visualtasker.wss.visual.descriptor.SurfaceRole
import com.visualtasker.wss.visual.descriptor.VisualBadge
import com.visualtasker.wss.visual.descriptor.VisualDescriptor
import com.visualtasker.wss.visual.semantics.ProjectionKind
import com.visualtasker.wss.visual.semantics.VisualActivity
import com.visualtasker.wss.visual.semantics.VisualAuthority
import com.visualtasker.wss.visual.semantics.VisualAvailability
import com.visualtasker.wss.visual.semantics.VisualCertainty
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualFocus
import com.visualtasker.wss.visual.semantics.VisualFreshness
import com.visualtasker.wss.visual.semantics.VisualRole
import com.visualtasker.wss.visual.semantics.VisualSemanticState
import com.visualtasker.wss.visual.semantics.VisualValidation

interface VisualPolicyResolver {
    fun resolve(state: VisualSemanticState, context: VisualContext): VisualDescriptor
}

object DefaultVisualPolicyResolver : VisualPolicyResolver {
    override fun resolve(state: VisualSemanticState, context: VisualContext): VisualDescriptor {
        val badges = buildList {
            when (state.authority) {
                VisualAuthority.AiProposed -> add(VisualBadge(BadgeRole.AiProposal))
                VisualAuthority.Unresolved -> add(VisualBadge(BadgeRole.Blocked))
                VisualAuthority.Canonical,
                VisualAuthority.Derived,
                VisualAuthority.HumanConfirmed -> Unit
            }
            when (state.certainty) {
                VisualCertainty.Ambiguous -> add(VisualBadge(BadgeRole.Ambiguous))
                VisualCertainty.Conflicting -> add(VisualBadge(BadgeRole.Warning, "conflict"))
                VisualCertainty.Known,
                VisualCertainty.Uncertain -> Unit
            }
            when (state.validation) {
                VisualValidation.Warning -> add(VisualBadge(BadgeRole.Warning))
                VisualValidation.Invalid -> add(VisualBadge(BadgeRole.Invalid))
                VisualValidation.Valid -> Unit
            }
            when (state.activity) {
                VisualActivity.Running,
                VisualActivity.Waiting -> add(VisualBadge(BadgeRole.Running))
                VisualActivity.Failed -> add(VisualBadge(BadgeRole.Invalid))
                VisualActivity.Skipped,
                VisualActivity.Cancelled -> add(VisualBadge(BadgeRole.Blocked))
                VisualActivity.Idle,
                VisualActivity.Queued,
                VisualActivity.Succeeded -> Unit
            }
            if (state.freshness == VisualFreshness.Stale) add(VisualBadge(BadgeRole.Stale))
            if (state.availability != VisualAvailability.Enabled) add(VisualBadge(BadgeRole.Blocked))
        }.distinctBy { it.role to it.label }

        return VisualDescriptor(
            shapeRole = shapeFor(state.role, context.projection),
            surfaceRole = surfaceFor(state.role),
            outlineRole = outlineFor(state),
            motionRole = motionFor(state.activity),
            opacityRole = opacityFor(state),
            emphasis = emphasisFor(state),
            badges = badges,
        )
    }

    private fun shapeFor(role: VisualRole, projection: ProjectionKind): ShapeRole =
        when (projection) {
            ProjectionKind.Inspector,
            ProjectionKind.LogConsole,
            ProjectionKind.DebugInfo,
            ProjectionKind.RecordList,
            ProjectionKind.DataBrowser -> ShapeRole.Row
            else -> when (role) {
                VisualRole.WorkflowEvent -> ShapeRole.Capsule
                VisualRole.WorkflowAction -> ShapeRole.RoundedRect
                VisualRole.WorkflowCondition,
                VisualRole.Ambiguity -> ShapeRole.Diamond
                VisualRole.WorkflowLoop -> ShapeRole.Hexagon
                VisualRole.WorkflowValue -> ShapeRole.NotchedValue
                VisualRole.Group -> ShapeRole.GroupRegion
                VisualRole.WorkflowConnector -> ShapeRole.Connector
                VisualRole.WorldEntity,
                VisualRole.Observation,
                VisualRole.Resource,
                VisualRole.RuntimeEvent,
                VisualRole.RecordStep,
                VisualRole.Unknown -> ShapeRole.RoundedRect
            }
        }

    private fun surfaceFor(role: VisualRole): SurfaceRole =
        when (role) {
            VisualRole.WorkflowEvent -> SurfaceRole.Event
            VisualRole.WorkflowAction -> SurfaceRole.Action
            VisualRole.WorkflowCondition -> SurfaceRole.Condition
            VisualRole.WorkflowLoop -> SurfaceRole.Loop
            VisualRole.WorkflowValue -> SurfaceRole.Value
            VisualRole.WorldEntity -> SurfaceRole.WorldEntity
            VisualRole.Observation -> SurfaceRole.Observation
            VisualRole.Resource -> SurfaceRole.Resource
            VisualRole.Ambiguity -> SurfaceRole.Ambiguity
            VisualRole.RuntimeEvent -> SurfaceRole.Runtime
            VisualRole.Group -> SurfaceRole.Group
            VisualRole.WorkflowConnector,
            VisualRole.RecordStep,
            VisualRole.Unknown -> SurfaceRole.Neutral
        }

    private fun outlineFor(state: VisualSemanticState): OutlineRole =
        when {
            state.validation == VisualValidation.Invalid -> OutlineRole.Invalid
            state.validation == VisualValidation.Warning -> OutlineRole.Warning
            state.activity in setOf(VisualActivity.Running, VisualActivity.Waiting) -> OutlineRole.RuntimeActive
            state.focus == VisualFocus.Selected -> OutlineRole.Selected
            state.focus == VisualFocus.Focused || state.focus == VisualFocus.Editing -> OutlineRole.Focused
            state.authority == VisualAuthority.AiProposed -> OutlineRole.Proposal
            else -> OutlineRole.None
        }

    private fun motionFor(activity: VisualActivity): MotionRole =
        when (activity) {
            VisualActivity.Running -> MotionRole.RuntimeFlow
            VisualActivity.Waiting,
            VisualActivity.Queued -> MotionRole.RuntimePulse
            VisualActivity.Failed -> MotionRole.AttentionPulse
            VisualActivity.Idle,
            VisualActivity.Succeeded,
            VisualActivity.Skipped,
            VisualActivity.Cancelled -> MotionRole.None
        }

    private fun opacityFor(state: VisualSemanticState): OpacityRole =
        when {
            state.availability == VisualAvailability.Disabled -> OpacityRole.Disabled
            state.availability == VisualAvailability.Unavailable -> OpacityRole.Muted
            state.freshness == VisualFreshness.Historical -> OpacityRole.Historical
            state.freshness == VisualFreshness.Stale -> OpacityRole.Muted
            else -> OpacityRole.Normal
        }

    private fun emphasisFor(state: VisualSemanticState): Emphasis =
        when {
            state.validation == VisualValidation.Invalid || state.activity == VisualActivity.Failed -> Emphasis.Critical
            state.activity in setOf(VisualActivity.Running, VisualActivity.Waiting) -> Emphasis.High
            state.focus != VisualFocus.None -> Emphasis.High
            state.validation == VisualValidation.Warning || state.certainty != VisualCertainty.Known -> Emphasis.Normal
            state.availability != VisualAvailability.Enabled || state.freshness != VisualFreshness.Current -> Emphasis.Low
            else -> Emphasis.Normal
        }
}
