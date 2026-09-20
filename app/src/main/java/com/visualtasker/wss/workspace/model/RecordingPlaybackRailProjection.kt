package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.recording.RecordingPlaybackDiagnosticSeverity
import com.visualtasker.wss.recording.RecordingPlaybackDocument
import com.visualtasker.wss.recording.RecordingPlaybackEntry
import com.visualtasker.wss.recording.RecordingPlaybackSessionSummary
import com.visualtasker.wss.recording.RecordingPlaybackTransitionStatus
import com.visualtasker.wss.recording.RecordingInteractionPayload
import com.visualtasker.wss.recording.StepCandidateDocument
import com.visualtasker.wss.recording.StepReviewDecision
import com.visualtasker.wss.recording.StepReviewStatus

const val RECORDING_PLAYBACK_SESSION_PREFIX = "room:"

fun RecordingPlaybackSessionSummary.toRecordingSessionUi(): RecordingSessionUi = RecordingSessionUi(
    path = RECORDING_PLAYBACK_SESSION_PREFIX + sessionId,
    fileName = sessionId,
    label = "${status.name.lowercase().replaceFirstChar(Char::uppercase)} ${sessionId.takeLast(8)}",
    lastModifiedMs = stoppedAtEpochMs ?: startedAtEpochMs,
    stepCount = interactionCount,
    durationMs = ((stoppedAtEpochMs ?: startedAtEpochMs) - startedAtEpochMs).coerceAtLeast(0L),
)

fun RecordingPlaybackDocument.toRecorderSteps(
    candidateDocument: StepCandidateDocument? = null,
    reviewDecisions: List<StepReviewDecision> = emptyList(),
): List<RecorderStepUi> = entries.map { entry ->
    val interaction = entry.interaction.interaction
    val tap = interaction.payload as? RecordingInteractionPayload.Tap
    val target = entry.interaction.targetNode
    val before = entry.beforeScene
    val after = entry.afterScene
    val candidate = candidateDocument?.candidates?.firstOrNull { it.sourceEntryId == entry.entryId }
    val decision = candidate?.let { selected -> reviewDecisions.firstOrNull { it.candidateId == selected.candidateId } }
    val reviewStatus = when {
        decision == null -> candidate?.reviewStatus
        decision.sourceRecordVersion != candidateDocument.sourceRecordVersion -> StepReviewStatus.STALE
        else -> decision.status
    }
    val hasError = entry.diagnostics.any { it.severity == RecordingPlaybackDiagnosticSeverity.ERROR }
    val timestamp = (entry.occurredAtEpochMs - startedAtEpochMs).coerceAtLeast(0L)
    val afterOffset = after?.scene?.openedAtEpochMs?.minus(startedAtEpochMs)
    RecorderStepUi(
        id = entry.entryId,
        label = target?.let { node ->
            node.text?.takeIf(String::isNotBlank)
                ?: node.contentDescription?.takeIf(String::isNotBlank)
                ?: node.className.substringAfterLast('.')
        } ?: interaction.displayLabel(),
        actionType = interaction.type.name.lowercase(),
        status = when {
            reviewStatus == StepReviewStatus.REJECTED || reviewStatus == StepReviewStatus.STALE -> StepStatus.Invalid
            reviewStatus == StepReviewStatus.CONFIRMED -> StepStatus.Executed
            reviewStatus == StepReviewStatus.CORRECTED -> StepStatus.Edited
            hasError || entry.transitionStatus == RecordingPlaybackTransitionStatus.FAILED -> StepStatus.Invalid
            entry.transitionStatus == RecordingPlaybackTransitionStatus.CHANGED -> StepStatus.Executed
            entry.transitionStatus == RecordingPlaybackTransitionStatus.PARTIAL -> StepStatus.Edited
            else -> StepStatus.Recorded
        },
        timestampMs = timestamp,
        durationMs = afterOffset?.minus(timestamp)?.coerceAtLeast(0L) ?: 0L,
        activityName = before?.scene?.windowContext?.activityName ?: after?.scene?.windowContext?.activityName,
        detail = listOfNotNull(entry.transitionStatus.name, reviewStatus?.name).joinToString(" | "),
        bounds = target?.takeIf { it.left < it.right && it.top < it.bottom }?.let { node ->
            WorldviewRect(
                left = node.left.toFloat(),
                top = node.top.toFloat(),
                right = node.right.toFloat(),
                bottom = node.bottom.toFloat(),
                coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
            )
        },
        point = tap?.position?.let { point ->
            WorldviewPoint(point.xPx.toFloat(), point.yPx.toFloat(), CoordinateSpace(CoordinateSpaceKind.Screen))
        },
        integrityReport = integrityReport?.forStep(entry.entryId),
        properties = buildMap {
            put("recording.sessionId", sessionId)
            put("recording.entryId", entry.entryId)
            put("recording.sequence", entry.sequence.toString())
            put("recording.transition", entry.transitionStatus.name)
            candidate?.let {
                put("review.candidateId", it.candidateId)
                put("review.confidence", it.confidence.level.name)
                put("review.confidenceReasons", it.confidence.reasons.joinToString(",") { reason -> reason.name })
            }
            reviewStatus?.let { put("review.status", it.name) }
            interaction.beforeSceneId?.let { put("recording.beforeSceneId", it) }
            interaction.afterSceneId?.let { put("recording.afterSceneId", it) }
            tap?.targetReference?.let { put("recording.targetA11yNodeId", it) }
            put("recording.rawEventIds", interaction.rawEventIds.joinToString(","))
            before?.primaryFrame?.frame?.assetReference?.let { put("recording.beforeAsset", it) }
            after?.primaryFrame?.frame?.assetReference?.let { put("recording.afterAsset", it) }
        },
    )
}

private fun com.visualtasker.wss.recording.RecordingInteraction.displayLabel(): String = when (val value = payload) {
    is RecordingInteractionPayload.Tap -> "Tap ${value.position.xPx}, ${value.position.yPx}"
    is RecordingInteractionPayload.Swipe -> "Swipe ${value.start.xPx},${value.start.yPx} -> ${value.end.xPx},${value.end.yPx}"
    is RecordingInteractionPayload.Text -> if (value.redacted) "Text [redacted]" else "Text ${value.value.orEmpty()}"
    is RecordingInteractionPayload.Window -> "Window ${value.change.name}"
    is RecordingInteractionPayload.Screenshot -> "Screenshot ${value.resource.resourceId}"
}

fun mergeCanonicalRecordingSteps(
    canonicalSteps: List<RecorderStepUi>,
    evidenceSteps: List<RecorderStepUi>,
    duplicateWindowMs: Long = 600L,
): List<RecorderStepUi> {
    val canonicalInteractionTimes = canonicalSteps
        .filter { it.actionType == "tap" }
        .mapNotNull(RecorderStepUi::timestampMs)
    val filteredEvidence = evidenceSteps.filterNot { evidence ->
        evidence.actionType in setOf("click", "button.click") &&
            evidence.timestampMs?.let { timestamp ->
                canonicalInteractionTimes.any { canonical -> kotlin.math.abs(canonical - timestamp) <= duplicateWindowMs }
            } == true
    }
    return (canonicalSteps.map { step ->
        step.copy(properties = step.properties + ("recording.layer" to "record"))
    } + filteredEvidence.map { step ->
        step.copy(properties = step.properties + ("recording.layer" to "evidence"))
    }).sortedWith(compareBy<RecorderStepUi> { it.timestampMs ?: Long.MAX_VALUE }.thenBy(RecorderStepUi::id))
}

fun String.recordingPlaybackSessionIdOrNull(): String? =
    takeIf { it.startsWith(RECORDING_PLAYBACK_SESSION_PREFIX) }
        ?.removePrefix(RECORDING_PLAYBACK_SESSION_PREFIX)
        ?.takeIf(String::isNotBlank)
