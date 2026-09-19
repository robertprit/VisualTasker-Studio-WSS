package com.visualtasker.wss.recording

import android.content.Context
import com.visualtasker.wss.recording.persistence.RecorderDatabase
import com.visualtasker.wss.recording.persistence.RoomRecorderSessionStore
import com.visualtasker.wss.recording.persistence.RoomStepReviewStore
import com.visualtasker.wss.recording.persistence.ScreenshotAssetStore
import com.visualtasker.wss.recording.persistence.StepReviewStore
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RecordingPlaybackRuntimeState(
    val sessions: List<RecordingPlaybackSessionSummary> = emptyList(),
    val playback: RecordingPlaybackState = RecordingPlaybackState(),
    val candidateDocument: StepCandidateDocument? = null,
    val reviewDecisions: List<StepReviewDecision> = emptyList(),
    val reviewedDocument: ReviewedStepDocument? = null,
    val reviewError: String? = null,
)

object RecordingPlaybackRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repositoryRef = AtomicReference<RecordingPlaybackRepository?>(null)
    private val reviewStoreRef = AtomicReference<StepReviewStore?>(null)
    private val controller = RecordingPlaybackController()
    private val _state = MutableStateFlow(RecordingPlaybackRuntimeState())
    val state: StateFlow<RecordingPlaybackRuntimeState> = _state.asStateFlow()
    private var playbackJob: Job? = null

    init {
        scope.launch {
            controller.state.collect { playback -> _state.value = _state.value.copy(playback = playback) }
        }
    }

    fun initialize(context: Context) {
        repository(context)
        refresh(context, loadLatestWhenIdle = true)
    }

    fun refresh(context: Context, loadLatestWhenIdle: Boolean = false) {
        scope.launch {
            val repository = repository(context)
            val sessions = repository.listSessions()
            _state.value = _state.value.copy(sessions = sessions)
            if (loadLatestWhenIdle && controller.state.value.document == null) {
                sessions.firstOrNull()?.let { loadSession(context, it.sessionId) }
            }
        }
    }

    fun loadSession(context: Context, sessionId: String) {
        controller.loading()
        _state.value = _state.value.copy(
            candidateDocument = null,
            reviewDecisions = emptyList(),
            reviewedDocument = null,
            reviewError = null,
        )
        scope.launch {
            runCatching { repository(context).load(sessionId) }
                .onSuccess { document ->
                    if (document == null) controller.fail("RecordingSession $sessionId wurde nicht gefunden.")
                    else {
                        val candidates = StepCandidateAssembler.assemble(document)
                        val decisions = reviewStore(context).loadForSession(sessionId)
                        controller.load(document)
                        _state.value = _state.value.copy(
                            candidateDocument = candidates,
                            reviewDecisions = decisions,
                            reviewedDocument = ReviewedStepProjector.project(candidates, decisions),
                            reviewError = null,
                        )
                    }
                }
                .onFailure { controller.fail(it.message ?: "RecordingSession konnte nicht geladen werden.") }
        }
    }

    fun saveReview(
        context: Context,
        candidateId: String,
        status: StepReviewStatus,
        correctedProposal: StepProposalSnapshot? = null,
        selectedTargetNodeId: String? = null,
        reasonCode: String? = null,
        note: String? = null,
        decidedAtEpochMs: Long = System.currentTimeMillis(),
    ) {
        val candidates = _state.value.candidateDocument ?: return
        val candidate = candidates.candidates.firstOrNull { it.candidateId == candidateId } ?: return
        val existing = _state.value.reviewDecisions.firstOrNull { it.candidateId == candidateId }
        val preservedCorrection = correctedProposal ?: existing?.correctedProposal
        val effectiveStatus = if (
            status == StepReviewStatus.CONFIRMED && preservedCorrection != null
        ) StepReviewStatus.CORRECTED else status
        val effectiveSelectedTargetNodeId = resolveSelectedTargetNodeId(
            correctedProposal = correctedProposal,
            preservedCorrection = preservedCorrection,
            requestedTargetNodeId = selectedTargetNodeId,
            existingTargetNodeId = existing?.selectedTargetNodeId,
        )
        val decision = StepReviewDecision(
            decisionId = existing?.decisionId ?: "review:$candidateId",
            candidateId = candidateId,
            sourceSessionId = candidates.sessionId,
            sourceRecordVersion = candidates.sourceRecordVersion,
            status = effectiveStatus,
            originalProposal = existing?.originalProposal ?: candidate.proposalSnapshot(),
            correctedProposal = preservedCorrection,
            selectedTargetNodeId = effectiveSelectedTargetNodeId,
            reasonCode = reasonCode,
            note = note,
            decidedAtEpochMs = decidedAtEpochMs,
        )
        scope.launch {
            runCatching { reviewStore(context).save(decision) }
                .onSuccess {
                    val decisions = reviewStore(context).loadForSession(candidates.sessionId)
                    _state.value = _state.value.copy(
                        reviewDecisions = decisions,
                        reviewedDocument = ReviewedStepProjector.project(candidates, decisions),
                        reviewError = null,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(reviewError = error.message ?: "Review konnte nicht gespeichert werden.")
                }
        }
    }

    fun setReviewStatus(
        context: Context,
        candidateId: String,
        status: StepReviewStatus,
        displayLabel: String? = null,
        reasonCode: String? = null,
        note: String? = null,
    ) {
        val candidate = _state.value.candidateDocument?.candidates?.firstOrNull { it.candidateId == candidateId } ?: return
        val corrected = displayLabel
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it != candidate.displayLabel }
            ?.let { candidate.proposalSnapshot().copy(displayLabel = it) }
        saveReview(
            context = context,
            candidateId = candidateId,
            status = if (corrected != null && status == StepReviewStatus.CONFIRMED) StepReviewStatus.CORRECTED else status,
            correctedProposal = corrected,
            selectedTargetNodeId = (corrected?.target ?: candidate.target as? CandidateTarget.A11y)?.let { (it as? CandidateTarget.A11y)?.nodeId },
            reasonCode = reasonCode,
            note = note,
        )
    }

    fun correctTarget(
        context: Context,
        candidateId: String,
        targetNodeId: String,
        displayLabel: String? = null,
    ) {
        val runtime = _state.value
        val candidate = runtime.candidateDocument?.candidates?.firstOrNull { it.candidateId == candidateId } ?: return
        val entry = runtime.playback.document?.entries?.firstOrNull { it.entryId == candidate.sourceEntryId } ?: return
        val node = entry.beforeScene?.a11ySnapshot?.rootNode?.findPlaybackNode(targetNodeId) ?: return
        val target = CandidateTarget.A11y(
            nodeId = node.stableSnapshotNodeId,
            resourceId = node.viewIdResourceName,
            text = node.text,
            contentDescription = node.contentDescription,
            className = node.className,
            left = node.left,
            top = node.top,
            right = node.right,
            bottom = node.bottom,
            clickable = node.clickable,
            visible = node.visibleToUser,
            enabled = node.enabled,
            packageName = entry.beforeScene?.scene?.windowContext?.packageName,
            windowId = entry.beforeScene?.scene?.windowContext?.windowId,
            xPx = candidate.evidence.tapXpx,
            yPx = candidate.evidence.tapYpx,
        )
        val label = displayLabel?.trim()?.takeIf(String::isNotEmpty)
            ?: node.text?.takeIf(String::isNotBlank)
            ?: node.contentDescription?.takeIf(String::isNotBlank)
            ?: node.viewIdResourceName?.substringAfterLast('/')
            ?: node.className.substringAfterLast('.')
        saveReview(
            context = context,
            candidateId = candidateId,
            status = StepReviewStatus.CORRECTED,
            correctedProposal = StepProposalSnapshot(CandidateAction.TAP, target, label),
            selectedTargetNodeId = targetNodeId,
            reasonCode = "TARGET_CORRECTED",
        )
    }

    fun useCoordinateTarget(
        context: Context,
        candidateId: String,
        displayLabel: String? = null,
    ) {
        val candidate = _state.value.candidateDocument?.candidates?.firstOrNull { it.candidateId == candidateId } ?: return
        val target = CandidateTarget.Coordinate(candidate.evidence.tapXpx, candidate.evidence.tapYpx)
        saveReview(
            context = context,
            candidateId = candidateId,
            status = StepReviewStatus.CORRECTED,
            correctedProposal = StepProposalSnapshot(
                CandidateAction.TAP,
                target,
                displayLabel?.trim()?.takeIf(String::isNotEmpty) ?: "Tap ${target.xPx}, ${target.yPx}",
            ),
            selectedTargetNodeId = null,
            reasonCode = "COORDINATE_FALLBACK_SELECTED",
        )
    }

    fun nextUnreviewed() {
        val runtime = _state.value
        val candidates = runtime.candidateDocument?.candidates.orEmpty()
        if (candidates.isEmpty()) return
        val decisions = runtime.reviewDecisions.associateBy(StepReviewDecision::candidateId)
        fun isOpen(candidate: StepCandidate): Boolean {
            val decision = decisions[candidate.candidateId] ?: return true
            return decision.sourceRecordVersion != runtime.candidateDocument?.sourceRecordVersion ||
                decision.status !in setOf(StepReviewStatus.CONFIRMED, StepReviewStatus.CORRECTED, StepReviewStatus.REJECTED)
        }
        val current = runtime.playback.selectedEntryIndex.coerceIn(candidates.indices)
        val next = ((current + 1)..candidates.lastIndex).firstOrNull { isOpen(candidates[it]) }
            ?: (0..current).firstOrNull { isOpen(candidates[it]) }
            ?: return
        seekToEntry(next)
    }

    fun showBeforePhase() {
        seekToEntry(_state.value.playback.selectedEntryIndex)
    }

    fun play() {
        controller.play()
        if (playbackJob?.isActive == true) return
        playbackJob = scope.launch {
            while (isActive && controller.state.value.status == RecordingPlaybackStatus.PLAYING) {
                delay(50L)
                controller.advanceBy(50L)
            }
        }
    }

    fun pause() { controller.pause(); playbackJob?.cancel(); playbackJob = null }
    fun next() { pause(); controller.next() }
    fun previous() { pause(); controller.previous() }
    fun restart() { pause(); controller.restart() }
    fun seekToEntry(index: Int) { pause(); controller.seekToEntry(index) }
    fun setPlaybackSpeed(speed: Float) = controller.setPlaybackSpeed(speed)
    fun selectA11yNode(nodeId: String?) = controller.selectA11yNode(nodeId)
    fun close() { pause(); controller.close() }

    private fun repository(context: Context): RecordingPlaybackRepository =
        repositoryRef.get() ?: synchronized(this) {
            repositoryRef.get() ?: RecordingPlaybackRepository(
                store = RoomRecorderSessionStore(RecorderDatabase.get(context.applicationContext)),
                assetStore = ScreenshotAssetStore(File(context.filesDir, "recorder/assets")),
            ).also(repositoryRef::set)
        }

    private fun reviewStore(context: Context): StepReviewStore =
        reviewStoreRef.get() ?: synchronized(this) {
            reviewStoreRef.get() ?: RoomStepReviewStore(
                RecorderDatabase.get(context.applicationContext),
            ).also(reviewStoreRef::set)
        }
}

internal fun resolveSelectedTargetNodeId(
    correctedProposal: StepProposalSnapshot?,
    preservedCorrection: StepProposalSnapshot?,
    requestedTargetNodeId: String?,
    existingTargetNodeId: String?,
): String? {
    val effectiveTarget = correctedProposal?.target ?: preservedCorrection?.target
    return when (effectiveTarget) {
        is CandidateTarget.Coordinate -> null
        is CandidateTarget.A11y -> effectiveTarget.nodeId
        null -> requestedTargetNodeId ?: existingTargetNodeId
    }
}
