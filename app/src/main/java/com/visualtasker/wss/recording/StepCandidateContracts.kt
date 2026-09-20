package com.visualtasker.wss.recording

const val STEP_CANDIDATE_SCHEMA_VERSION = 1

enum class CandidateAction { TAP }
enum class StepReviewStatus {
    UNREVIEWED,
    CONFIRMED,
    CORRECTED,
    REJECTED,
    DEFERRED,
    STALE,
    UNSUPPORTED,
    PROPOSED,
    NEEDS_REVIEW,
}
enum class ConfidenceLevel { HIGH, MEDIUM, LOW, UNKNOWN }
enum class ConfidenceReason {
    EXPLICIT_RECORDED_TARGET,
    SINGLE_CONTAINING_NODE,
    MULTIPLE_TARGETS,
    TARGET_NOT_CLICKABLE,
    TARGET_NOT_VISIBLE,
    TARGET_NOT_ENABLED,
    TAP_OUTSIDE_TARGET,
    COORDINATE_ONLY,
    MISSING_A11Y_SNAPSHOT,
    MISSING_BEFORE_SCENE,
    MISSING_AFTER_SCENE,
    CORRUPT_SOURCE_REFERENCE,
}

enum class StepCandidateDiagnosticSeverity { INFO, WARNING, ERROR }

data class StepCandidateDiagnostic(
    val code: String,
    val message: String,
    val severity: StepCandidateDiagnosticSeverity,
    val referenceId: String? = null,
)

data class CandidateConfidence(
    val level: ConfidenceLevel,
    val reasons: List<ConfidenceReason>,
)

data class SceneReference(
    val sceneId: String,
    val sequence: Long,
    val packageName: String?,
    val activityName: String?,
    val windowId: String?,
    val frameId: String?,
    val a11ySnapshotId: String?,
)

sealed interface CandidateTarget {
    val xPx: Int
    val yPx: Int

    data class A11y(
        val nodeId: String,
        val resourceId: String?,
        val text: String?,
        val contentDescription: String?,
        val className: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val clickable: Boolean,
        val visible: Boolean,
        val enabled: Boolean,
        val packageName: String?,
        val windowId: String?,
        override val xPx: Int,
        override val yPx: Int,
        val alternativeNodeIds: List<String> = emptyList(),
    ) : CandidateTarget

    data class Coordinate(
        override val xPx: Int,
        override val yPx: Int,
    ) : CandidateTarget
}

enum class ObservedOutcomeKind { UNCHANGED, SCENE_TRANSITION, MISSING_AFTER, FAILED, PARTIAL }

data class ObservedOutcome(
    val kind: ObservedOutcomeKind,
    val beforeSceneId: String?,
    val afterSceneId: String?,
    val windowChanged: Boolean,
)

data class CandidateEvidence(
    val beforeFrameId: String?,
    val beforeAssetReference: String?,
    val beforeA11ySnapshotId: String?,
    val afterFrameId: String?,
    val afterAssetReference: String?,
    val afterA11ySnapshotId: String?,
    val tapXpx: Int,
    val tapYpx: Int,
    val targetBounds: List<Int>?,
    val evidenceRefs: List<String> = emptyList(),
)

data class StepCandidate(
    val candidateId: String,
    val sessionId: String,
    val ordinal: Int,
    val sourceEntryId: String,
    val sourceInteractionId: String,
    val action: CandidateAction,
    val target: CandidateTarget,
    val displayLabel: String,
    val beforeSceneRef: SceneReference?,
    val observedOutcome: ObservedOutcome,
    val evidence: CandidateEvidence,
    val confidence: CandidateConfidence,
    val reviewStatus: StepReviewStatus,
    val diagnostics: List<StepCandidateDiagnostic>,
)

data class StepCandidateDocument(
    val documentId: String,
    val schemaVersion: Int = STEP_CANDIDATE_SCHEMA_VERSION,
    val sessionId: String,
    val sourceRecordVersion: Int,
    val generatedAtEpochMs: Long,
    val candidates: List<StepCandidate>,
    val diagnostics: List<StepCandidateDiagnostic>,
)

data class StepProposalSnapshot(
    val action: CandidateAction,
    val target: CandidateTarget,
    val displayLabel: String,
)

data class StepReviewDecision(
    val decisionId: String,
    val candidateId: String,
    val sourceSessionId: String,
    val sourceRecordVersion: Int,
    val status: StepReviewStatus,
    val originalProposal: StepProposalSnapshot,
    val correctedProposal: StepProposalSnapshot?,
    val selectedTargetNodeId: String?,
    val reasonCode: String?,
    val note: String?,
    val decidedAtEpochMs: Long,
)

data class ReviewedStep(
    val stepId: String,
    val ordinal: Int,
    val sourceCandidateId: String,
    val reviewStatus: StepReviewStatus,
    val action: CandidateAction,
    val finalTarget: CandidateTarget,
    val displayLabel: String,
    val beforeSceneRef: SceneReference?,
    val observedOutcome: ObservedOutcome,
    val evidence: CandidateEvidence,
    val reviewDecisionRef: String,
)

data class ReviewDiagnostic(
    val code: String,
    val message: String,
    val candidateId: String? = null,
)

data class ReviewedStepDocument(
    val sessionId: String,
    val sourceCandidateDocumentId: String,
    val steps: List<ReviewedStep>,
    val unresolvedCount: Int,
    val rejectedCount: Int,
    val diagnostics: List<ReviewDiagnostic>,
)

fun StepCandidate.proposalSnapshot(): StepProposalSnapshot =
    StepProposalSnapshot(action, target, displayLabel)
