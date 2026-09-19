package com.visualtasker.wss.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RecordingPlaybackStatus { IDLE, LOADING, READY, PLAYING, PAUSED, COMPLETED, ERROR }
enum class RecordingPlaybackPhase { BEFORE, AFTER }

data class RecordingPlaybackState(
    val status: RecordingPlaybackStatus = RecordingPlaybackStatus.IDLE,
    val document: RecordingPlaybackDocument? = null,
    val selectedEntryIndex: Int = 0,
    val phase: RecordingPlaybackPhase = RecordingPlaybackPhase.BEFORE,
    val selectedA11yNodeId: String? = null,
    val speed: Float = 1f,
    val phaseElapsedMs: Long = 0L,
    val error: String? = null,
) {
    val selectedEntry: RecordingPlaybackEntry? get() = document?.entries?.getOrNull(selectedEntryIndex)
    val selectedScene: RecordingPlaybackScene?
        get() = when (phase) {
            RecordingPlaybackPhase.BEFORE -> selectedEntry?.beforeScene ?: document?.initialScene
            RecordingPlaybackPhase.AFTER -> selectedEntry?.afterScene ?: selectedEntry?.beforeScene ?: document?.initialScene
        }
}

class RecordingPlaybackController(
    private val previewPolicy: RecordingPlaybackPreviewPolicy = RecordingPlaybackPreviewPolicy(),
) {
    private val _state = MutableStateFlow(RecordingPlaybackState())
    val state: StateFlow<RecordingPlaybackState> = _state.asStateFlow()

    fun loading() {
        _state.value = RecordingPlaybackState(status = RecordingPlaybackStatus.LOADING)
    }

    fun load(document: RecordingPlaybackDocument) {
        _state.value = RecordingPlaybackState(
            status = RecordingPlaybackStatus.READY,
            document = document,
            selectedEntryIndex = 0,
            phase = RecordingPlaybackPhase.BEFORE,
            selectedA11yNodeId = document.entries.firstOrNull()?.interaction?.tap?.targetA11yNodeId,
        )
    }

    fun fail(message: String) {
        _state.value = RecordingPlaybackState(status = RecordingPlaybackStatus.ERROR, error = message)
    }

    fun play() {
        val current = _state.value
        if (current.document?.entries.isNullOrEmpty()) return
        _state.value = current.copy(
            status = if (current.status == RecordingPlaybackStatus.COMPLETED) RecordingPlaybackStatus.PLAYING else RecordingPlaybackStatus.PLAYING,
            selectedEntryIndex = if (current.status == RecordingPlaybackStatus.COMPLETED) 0 else current.selectedEntryIndex,
            phase = if (current.status == RecordingPlaybackStatus.COMPLETED) RecordingPlaybackPhase.BEFORE else current.phase,
            phaseElapsedMs = if (current.status == RecordingPlaybackStatus.COMPLETED) 0L else current.phaseElapsedMs,
        ).withTargetSelection()
    }

    fun pause() {
        if (_state.value.status == RecordingPlaybackStatus.PLAYING) {
            _state.value = _state.value.copy(status = RecordingPlaybackStatus.PAUSED)
        }
    }

    fun next() {
        advancePosition(manual = true)
    }

    fun previous() {
        val current = _state.value
        val document = current.document ?: return
        val next = when {
            current.phase == RecordingPlaybackPhase.AFTER -> current.copy(phase = RecordingPlaybackPhase.BEFORE)
            current.selectedEntryIndex > 0 -> current.copy(
                selectedEntryIndex = current.selectedEntryIndex - 1,
                phase = RecordingPlaybackPhase.AFTER,
            )
            else -> current.copy(phase = RecordingPlaybackPhase.BEFORE)
        }
        _state.value = next.copy(
            status = RecordingPlaybackStatus.PAUSED,
            phaseElapsedMs = 0L,
            selectedEntryIndex = next.selectedEntryIndex.coerceIn(0, (document.entries.size - 1).coerceAtLeast(0)),
        ).withTargetSelection()
    }

    fun seekToEntry(index: Int) {
        val current = _state.value
        val entries = current.document?.entries.orEmpty()
        if (entries.isEmpty()) return
        _state.value = current.copy(
            status = RecordingPlaybackStatus.PAUSED,
            selectedEntryIndex = index.coerceIn(entries.indices),
            phase = RecordingPlaybackPhase.BEFORE,
            phaseElapsedMs = 0L,
        ).withTargetSelection()
    }

    fun restart() {
        val current = _state.value
        if (current.document == null) return
        _state.value = current.copy(
            status = RecordingPlaybackStatus.READY,
            selectedEntryIndex = 0,
            phase = RecordingPlaybackPhase.BEFORE,
            phaseElapsedMs = 0L,
        ).withTargetSelection()
    }

    fun setPlaybackSpeed(speed: Float) {
        require(speed in SupportedPlaybackSpeeds) { "Unsupported playback speed: $speed" }
        _state.value = _state.value.copy(speed = speed)
    }

    fun selectA11yNode(nodeId: String?) {
        _state.value = _state.value.copy(selectedA11yNodeId = nodeId)
    }

    fun advanceBy(realElapsedMs: Long) {
        val current = _state.value
        if (current.status != RecordingPlaybackStatus.PLAYING || realElapsedMs <= 0L) return
        val scaled = (realElapsedMs * current.speed).toLong().coerceAtLeast(1L)
        val nextElapsed = current.phaseElapsedMs + scaled
        val threshold = previewPolicy.delayFor(current)
        if (nextElapsed >= threshold) {
            advancePosition(manual = false, carryMs = nextElapsed - threshold)
        } else {
            _state.value = current.copy(phaseElapsedMs = nextElapsed)
        }
    }

    fun close() {
        _state.value = RecordingPlaybackState()
    }

    private fun advancePosition(manual: Boolean, carryMs: Long = 0L) {
        val current = _state.value
        val entries = current.document?.entries.orEmpty()
        if (entries.isEmpty()) return
        val hasAfterPhase = current.selectedEntry?.afterScene != null
        val next = when {
            current.phase == RecordingPlaybackPhase.BEFORE && hasAfterPhase -> current.copy(phase = RecordingPlaybackPhase.AFTER)
            current.selectedEntryIndex < entries.lastIndex -> current.copy(
                selectedEntryIndex = current.selectedEntryIndex + 1,
                phase = RecordingPlaybackPhase.BEFORE,
            )
            else -> current.copy(status = RecordingPlaybackStatus.COMPLETED, phase = RecordingPlaybackPhase.AFTER)
        }
        _state.value = next.copy(
            status = when {
                next.status == RecordingPlaybackStatus.COMPLETED -> RecordingPlaybackStatus.COMPLETED
                manual -> RecordingPlaybackStatus.PAUSED
                else -> RecordingPlaybackStatus.PLAYING
            },
            phaseElapsedMs = if (next.status == RecordingPlaybackStatus.COMPLETED) 0L else carryMs,
        ).withTargetSelection()
    }

    private fun RecordingPlaybackState.withTargetSelection(): RecordingPlaybackState {
        val targetId = selectedEntry?.interaction?.tap?.targetA11yNodeId
        val targetExistsInScene = targetId != null && selectedScene?.a11ySnapshot?.rootNode?.containsNode(targetId) == true
        return copy(selectedA11yNodeId = targetId.takeIf { targetExistsInScene })
    }

    companion object {
        val SupportedPlaybackSpeeds = setOf(0.5f, 1f, 2f)
    }
}

private fun A11yNodeSnapshot.containsNode(nodeId: String): Boolean =
    stableSnapshotNodeId == nodeId || children.any { it.containsNode(nodeId) }

data class RecordingPlaybackPreviewPolicy(
    val minimumPhaseMs: Long = 250L,
    val maximumPhaseMs: Long = 2_000L,
    val fallbackPhaseMs: Long = 600L,
) {
    fun delayFor(state: RecordingPlaybackState): Long {
        val entry = state.selectedEntry ?: return fallbackPhaseMs
        val original = when (state.phase) {
            RecordingPlaybackPhase.BEFORE -> entry.afterScene?.scene?.openedAtElapsedRealtimeNanos
                ?.minus(entry.occurredAtElapsedRealtimeNanos)
                ?.div(1_000_000L)
            RecordingPlaybackPhase.AFTER -> state.document?.entries
                ?.getOrNull(state.selectedEntryIndex + 1)
                ?.occurredAtElapsedRealtimeNanos
                ?.minus(entry.afterScene?.scene?.openedAtElapsedRealtimeNanos ?: entry.occurredAtElapsedRealtimeNanos)
                ?.div(1_000_000L)
        }
        return (original ?: fallbackPhaseMs).coerceIn(minimumPhaseMs, maximumPhaseMs)
    }
}
