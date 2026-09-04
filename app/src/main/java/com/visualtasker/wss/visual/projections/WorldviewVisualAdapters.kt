package com.visualtasker.wss.visual.projections

import com.visualtasker.wss.visual.semantics.VisualAuthority
import com.visualtasker.wss.visual.semantics.VisualAvailability
import com.visualtasker.wss.visual.semantics.VisualCertainty
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualFreshness
import com.visualtasker.wss.visual.semantics.VisualRole
import com.visualtasker.wss.visual.semantics.VisualSemanticAdapter
import com.visualtasker.wss.visual.semantics.VisualSemanticState
import com.visualtasker.wss.visual.semantics.VisualValidation
import com.visualtasker.wss.workspace.model.AmbiguityResolutionState
import com.visualtasker.wss.workspace.model.KnowledgeState
import com.visualtasker.wss.workspace.model.ObservationProvider
import com.visualtasker.wss.workspace.model.WorldAmbiguity
import com.visualtasker.wss.workspace.model.WorldEntity
import com.visualtasker.wss.workspace.model.WorldObservation
import com.visualtasker.wss.workspace.model.WorkspaceResource

object WorldEntityVisualAdapter : VisualSemanticAdapter<WorldEntity> {
    override fun map(value: WorldEntity, context: VisualContext): VisualSemanticState =
        VisualSemanticState(
            role = VisualRole.WorldEntity,
            certainty = when (value.state) {
                KnowledgeState.Known -> VisualCertainty.Known
                KnowledgeState.Conflicting -> VisualCertainty.Conflicting
                KnowledgeState.Unknown,
                KnowledgeState.NotObserved -> VisualCertainty.Uncertain
                KnowledgeState.NotAvailable,
                KnowledgeState.NotApplicable -> VisualCertainty.Ambiguous
            },
            validation = if (value.state == KnowledgeState.Conflicting) VisualValidation.Warning else VisualValidation.Valid,
            availability = when (value.state) {
                KnowledgeState.NotAvailable,
                KnowledgeState.NotApplicable -> VisualAvailability.Unavailable
                else -> VisualAvailability.Enabled
            },
            freshness = if (value.state == KnowledgeState.NotObserved) VisualFreshness.Stale else VisualFreshness.Current,
        )
}

object ObservationVisualAdapter : VisualSemanticAdapter<WorldObservation> {
    override fun map(value: WorldObservation, context: VisualContext): VisualSemanticState =
        VisualSemanticState(
            role = VisualRole.Observation,
            authority = if (value.provider == ObservationProvider.User) {
                VisualAuthority.HumanConfirmed
            } else {
                VisualAuthority.Derived
            },
            certainty = when {
                value.confidence >= 0.85f -> VisualCertainty.Known
                value.confidence >= 0.5f -> VisualCertainty.Uncertain
                else -> VisualCertainty.Ambiguous
            },
            validation = if (value.confidence < 0.5f) VisualValidation.Warning else VisualValidation.Valid,
        )
}

object ResourceVisualAdapter : VisualSemanticAdapter<WorkspaceResource> {
    override fun map(value: WorkspaceResource, context: VisualContext): VisualSemanticState =
        VisualSemanticState(
            role = VisualRole.Resource,
            authority = if (value.locked) VisualAuthority.HumanConfirmed else VisualAuthority.Canonical,
            availability = if (value.hidden) VisualAvailability.Disabled else VisualAvailability.Enabled,
        )
}

object AmbiguityVisualAdapter : VisualSemanticAdapter<WorldAmbiguity> {
    override fun map(value: WorldAmbiguity, context: VisualContext): VisualSemanticState =
        VisualSemanticState(
            role = VisualRole.Ambiguity,
            authority = when (value.resolutionState) {
                AmbiguityResolutionState.Open -> VisualAuthority.Unresolved
                AmbiguityResolutionState.Proposed -> VisualAuthority.AiProposed
                AmbiguityResolutionState.Resolved -> VisualAuthority.HumanConfirmed
                AmbiguityResolutionState.Rejected -> VisualAuthority.Unresolved
            },
            certainty = if (value.resolutionState == AmbiguityResolutionState.Resolved) {
                VisualCertainty.Known
            } else {
                VisualCertainty.Ambiguous
            },
            validation = if (value.resolutionState == AmbiguityResolutionState.Rejected) {
                VisualValidation.Invalid
            } else {
                VisualValidation.Warning
            },
        )
}
