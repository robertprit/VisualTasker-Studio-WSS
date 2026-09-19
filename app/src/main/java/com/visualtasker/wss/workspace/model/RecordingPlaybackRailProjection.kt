package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.recording.RecordingPlaybackDiagnosticSeverity
import com.visualtasker.wss.recording.RecordingPlaybackDocument
import com.visualtasker.wss.recording.RecordingPlaybackEntry
import com.visualtasker.wss.recording.RecordingPlaybackSessionSummary
import com.visualtasker.wss.recording.RecordingPlaybackTransitionStatus
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
    val tap = entry.interaction.tap
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
        } ?: "Tap ${tap.xPx}, ${tap.yPx}",
        actionType = "tap",
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
        point = WorldviewPoint(
            x = tap.xPx.toFloat(),
            y = tap.yPx.toFloat(),
            coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
        ),
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
            put("recording.beforeSceneId", tap.beforeSceneId)
            tap.afterSceneId?.let { put("recording.afterSceneId", it) }
            tap.targetA11yNodeId?.let { put("recording.targetA11yNodeId", it) }
            before?.primaryFrame?.frame?.assetReference?.let { put("recording.beforeAsset", it) }
            after?.primaryFrame?.frame?.assetReference?.let { put("recording.afterAsset", it) }
        },
    )
}

fun String.recordingPlaybackSessionIdOrNull(): String? =
    takeIf { it.startsWith(RECORDING_PLAYBACK_SESSION_PREFIX) }
        ?.removePrefix(RECORDING_PLAYBACK_SESSION_PREFIX)
        ?.takeIf(String::isNotBlank)
