package com.visualtasker.wss.recording

enum class RecordingIntegritySeverity { INFO, WARNING, ERROR }

data class RecordingStepEvidenceRef(
    val stepId: String,
    val interactionId: String,
    val evidenceIds: List<String>,
)

data class RecordingIntegritySnapshot(
    val steps: List<RecordingStepEvidenceRef>,
    val evidence: List<RecordingEvidenceRef>,
)

sealed interface RecordingIntegrityIssue {
    val severity: RecordingIntegritySeverity
    val recordingId: String
    val code: String
    val referenceId: String

    data class MissingRawEvent(
        override val recordingId: String,
        val interactionId: String,
        val rawEventId: String,
    ) : RecordingIntegrityIssue {
        override val severity = RecordingIntegritySeverity.ERROR
        override val code = "MISSING_RAW_EVENT"
        override val referenceId = rawEventId
    }

    data class MissingEvidence(
        override val recordingId: String,
        val interactionId: String,
        val evidenceId: String,
    ) : RecordingIntegrityIssue {
        override val severity = RecordingIntegritySeverity.ERROR
        override val code = "MISSING_EVIDENCE"
        override val referenceId = evidenceId
    }

    data class MissingResource(
        override val recordingId: String,
        val evidenceId: String,
        val resource: RecordingResourceRef,
    ) : RecordingIntegrityIssue {
        override val severity = RecordingIntegritySeverity.ERROR
        override val code = "MISSING_RESOURCE"
        override val referenceId = resource.resourceId
    }

    data class BrokenStepEvidenceRef(
        override val recordingId: String,
        val stepId: String,
        val evidenceId: String,
    ) : RecordingIntegrityIssue {
        override val severity = RecordingIntegritySeverity.ERROR
        override val code = "BROKEN_STEP_EVIDENCE_REF"
        override val referenceId = evidenceId
    }

    data class BrokenStepInteractionRef(
        override val recordingId: String,
        val stepId: String,
        val interactionId: String,
    ) : RecordingIntegrityIssue {
        override val severity = RecordingIntegritySeverity.ERROR
        override val code = "BROKEN_STEP_INTERACTION_REF"
        override val referenceId = interactionId
    }

    data class OrphanEvidence(
        override val recordingId: String,
        val evidenceId: String,
    ) : RecordingIntegrityIssue {
        override val severity = RecordingIntegritySeverity.WARNING
        override val code = "ORPHAN_EVIDENCE"
        override val referenceId = evidenceId
    }

    data class OrphanInteraction(
        override val recordingId: String,
        val interactionId: String,
    ) : RecordingIntegrityIssue {
        override val severity = RecordingIntegritySeverity.WARNING
        override val code = "ORPHAN_INTERACTION"
        override val referenceId = interactionId
    }
}

data class RecordingEvidenceProvenance(
    val evidence: RecordingEvidenceRef,
    val resources: List<RecordingResourceRef>,
)

data class RecordingStepProvenance(
    val stepId: String,
    val interactionId: String,
    val rawEventIds: List<String>,
    val evidence: List<RecordingEvidenceProvenance>,
)

data class RecordingIntegrityStepReport(
    val stepId: String,
    val interactionId: String,
    val issues: List<RecordingIntegrityIssue>,
    val provenance: RecordingStepProvenance,
) {
    val isValid: Boolean
        get() = issues.none { it.severity == RecordingIntegritySeverity.ERROR }
}

data class RecordingIntegrityReport(
    val recordingId: String,
    val issues: List<RecordingIntegrityIssue>,
    val steps: List<RecordingIntegrityStepReport>,
) {
    val isValid: Boolean
        get() = issues.none { it.severity == RecordingIntegritySeverity.ERROR }

    fun forStep(stepId: String): RecordingIntegrityStepReport? = steps.firstOrNull { it.stepId == stepId }
}

object RecordingIntegrityValidator {
    fun validate(
        record: PersistedRecordingSession,
        snapshot: RecordingIntegritySnapshot,
    ): RecordingIntegrityReport {
        val recordingId = record.session.sessionId
        val rawEventIds = record.rawEvents.mapTo(mutableSetOf(), RawRecordingEvent::rawEventId)
        val evidenceById = snapshot.evidence.associateBy(RecordingEvidenceRef::evidenceId)
        val interactionById = record.interactions.associateBy(RecordingInteraction::interactionId)
        val referencedEvidence = snapshot.steps.flatMapTo(mutableSetOf(), RecordingStepEvidenceRef::evidenceIds)
        val referencedInteractions = snapshot.steps.mapTo(mutableSetOf(), RecordingStepEvidenceRef::interactionId)
        val issues = mutableListOf<RecordingIntegrityIssue>()

        record.interactions.forEach { interaction ->
            interaction.rawEventIds
                .filterNot(rawEventIds::contains)
                .forEach { rawEventId ->
                    issues += RecordingIntegrityIssue.MissingRawEvent(recordingId, interaction.interactionId, rawEventId)
                }
            interaction.evidenceRefs
                .filterNot(evidenceById::containsKey)
                .forEach { evidenceId ->
                    issues += RecordingIntegrityIssue.MissingEvidence(recordingId, interaction.interactionId, evidenceId)
                }
            if (interaction.interactionId !in referencedInteractions) {
                issues += RecordingIntegrityIssue.OrphanInteraction(recordingId, interaction.interactionId)
            }
        }

        snapshot.steps.forEach { step ->
            if (step.interactionId !in interactionById) {
                issues += RecordingIntegrityIssue.BrokenStepInteractionRef(recordingId, step.stepId, step.interactionId)
            }
            step.evidenceIds.filterNot(evidenceById::containsKey).forEach { evidenceId ->
                issues += RecordingIntegrityIssue.BrokenStepEvidenceRef(recordingId, step.stepId, evidenceId)
            }
            step.evidenceIds.mapNotNull(evidenceById::get)
                .filter { it.stepId != step.stepId }
                .forEach { evidence ->
                    issues += RecordingIntegrityIssue.BrokenStepEvidenceRef(recordingId, step.stepId, evidence.evidenceId)
                }
        }

        snapshot.evidence.forEach { evidence ->
            if (evidence.evidenceId !in referencedEvidence) {
                issues += RecordingIntegrityIssue.OrphanEvidence(recordingId, evidence.evidenceId)
            }
            evidence.resources.filterNot { record.contains(it) }.forEach { resource ->
                issues += RecordingIntegrityIssue.MissingResource(recordingId, evidence.evidenceId, resource)
            }
        }

        val sortedIssues = issues.distinct().sortedWith(
            compareByDescending<RecordingIntegrityIssue> { it.severity }
                .thenBy(RecordingIntegrityIssue::code)
                .thenBy(RecordingIntegrityIssue::referenceId),
        )
        val stepReports = snapshot.steps.map { step ->
            val interaction = interactionById[step.interactionId]
            val stepEvidence = step.evidenceIds.mapNotNull(evidenceById::get)
            val relatedEvidenceIds = step.evidenceIds.toSet()
            val stepIssues = sortedIssues.filter { issue ->
                when (issue) {
                    is RecordingIntegrityIssue.MissingRawEvent -> issue.interactionId == step.interactionId
                    is RecordingIntegrityIssue.MissingEvidence -> issue.interactionId == step.interactionId
                    is RecordingIntegrityIssue.MissingResource -> issue.evidenceId in relatedEvidenceIds
                    is RecordingIntegrityIssue.BrokenStepEvidenceRef -> issue.stepId == step.stepId
                    is RecordingIntegrityIssue.BrokenStepInteractionRef -> issue.stepId == step.stepId
                    is RecordingIntegrityIssue.OrphanEvidence -> false
                    is RecordingIntegrityIssue.OrphanInteraction -> issue.interactionId == step.interactionId
                }
            }
            RecordingIntegrityStepReport(
                stepId = step.stepId,
                interactionId = step.interactionId,
                issues = stepIssues,
                provenance = RecordingStepProvenance(
                    stepId = step.stepId,
                    interactionId = step.interactionId,
                    rawEventIds = interaction?.rawEventIds.orEmpty(),
                    evidence = stepEvidence.map { evidence ->
                        RecordingEvidenceProvenance(evidence, evidence.resources)
                    },
                ),
            )
        }
        return RecordingIntegrityReport(recordingId, sortedIssues, stepReports)
    }
}

fun RecordingPlaybackDocument.toIntegritySnapshot(): RecordingIntegritySnapshot = RecordingIntegritySnapshot(
    steps = entries.map { entry ->
        RecordingStepEvidenceRef(
            stepId = entry.entryId,
            interactionId = entry.interaction.interaction.interactionId,
            evidenceIds = entry.evidence.map(RecordingEvidenceRef::evidenceId),
        )
    },
    evidence = entries.flatMap(RecordingPlaybackEntry::evidence).distinctBy(RecordingEvidenceRef::evidenceId),
)

private fun PersistedRecordingSession.contains(resource: RecordingResourceRef): Boolean = when (resource.kind) {
    RecordingResourceKind.RAW_EVENT -> rawEvents.any { it.rawEventId == resource.resourceId }
    RecordingResourceKind.SCENE -> scenes.any { it.sceneId == resource.resourceId }
    RecordingResourceKind.A11Y_SNAPSHOT -> snapshots.any { it.snapshotId == resource.resourceId }
    RecordingResourceKind.CAPTURE_FRAME -> frames.any { it.frameId == resource.resourceId }
    RecordingResourceKind.SCREENSHOT_ASSET -> assets.any { it.assetHash == resource.resourceId }
}
