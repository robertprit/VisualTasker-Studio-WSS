package com.visualtasker.wss.recording

object ReviewedStepProjector {
    fun project(
        candidates: StepCandidateDocument,
        decisions: List<StepReviewDecision>,
    ): ReviewedStepDocument {
        val decisionsByCandidate = decisions.associateBy(StepReviewDecision::candidateId)
        val diagnostics = mutableListOf<ReviewDiagnostic>()
        var unresolved = 0
        var rejected = 0
        val reviewed = candidates.candidates.mapNotNull { candidate ->
            val decision = decisionsByCandidate[candidate.candidateId]
            if (decision == null) {
                unresolved += 1
                return@mapNotNull null
            }
            if (decision.sourceSessionId != candidates.sessionId || decision.sourceRecordVersion != candidates.sourceRecordVersion) {
                unresolved += 1
                diagnostics += ReviewDiagnostic(
                    "STALE_REVIEW",
                    "Reviewentscheidung passt nicht mehr zur Record-Version.",
                    candidate.candidateId,
                )
                return@mapNotNull null
            }
            when (decision.status) {
                StepReviewStatus.REJECTED -> {
                    rejected += 1
                    null
                }
                StepReviewStatus.CONFIRMED, StepReviewStatus.CORRECTED -> {
                    val proposal = decision.correctedProposal ?: decision.originalProposal
                    ReviewedStep(
                        stepId = "reviewed:${candidate.candidateId}",
                        ordinal = candidate.ordinal,
                        sourceCandidateId = candidate.candidateId,
                        reviewStatus = decision.status,
                        action = proposal.action,
                        finalTarget = proposal.target,
                        displayLabel = proposal.displayLabel,
                        beforeSceneRef = candidate.beforeSceneRef,
                        observedOutcome = candidate.observedOutcome,
                        evidence = candidate.evidence,
                        reviewDecisionRef = decision.decisionId,
                    )
                }
                else -> {
                    unresolved += 1
                    null
                }
            }
        }
        decisions.filter { decision -> candidates.candidates.none { it.candidateId == decision.candidateId } }.forEach { stale ->
            diagnostics += ReviewDiagnostic("ORPHANED_REVIEW", "Reviewentscheidung hat keinen passenden Kandidaten.", stale.candidateId)
        }
        return ReviewedStepDocument(
            sessionId = candidates.sessionId,
            sourceCandidateDocumentId = candidates.documentId,
            steps = reviewed,
            unresolvedCount = unresolved,
            rejectedCount = rejected,
            diagnostics = diagnostics,
        )
    }
}
