package com.visualtasker.wss.recording

/** Distinct lifecycle domains. None of these values implies a conversion to workflow intent. */
enum class RecordingDomainKind {
    RECORDING,
    RECORD,
    REPLAY,
    DRY_RUN,
    WET_RUN,
    WATCHDOG,
}

enum class RecordingEvidenceKind {
    INPUT,
    WINDOW,
    ACCESSIBILITY,
    VISUAL,
}

enum class RecordingResourceKind {
    RAW_EVENT,
    SCENE,
    A11Y_SNAPSHOT,
    CAPTURE_FRAME,
    SCREENSHOT_ASSET,
}

data class RecordingResourceRef(
    val kind: RecordingResourceKind,
    val resourceId: String,
) {
    init {
        require(resourceId.isNotBlank()) { "Recording resource id must not be blank." }
    }
}

data class RecordingEvidenceRef(
    val evidenceId: String,
    val sessionId: String,
    val stepId: String,
    val sequence: Long,
    val occurredAtEpochMs: Long,
    val occurredAtElapsedRealtimeNanos: Long,
    val kind: RecordingEvidenceKind,
    val resources: List<RecordingResourceRef>,
    val properties: Map<String, String> = emptyMap(),
) {
    init {
        require(evidenceId.isNotBlank()) { "Evidence id must not be blank." }
        require(sessionId.isNotBlank()) { "Evidence session id must not be blank." }
        require(stepId.isNotBlank()) { "Evidence step id must not be blank." }
        require(sequence >= 0L) { "Evidence sequence must not be negative." }
    }
}

data class RecordingReplayBookmark(
    val recordId: String,
    val entryId: String?,
    val entryIndex: Int,
    val phase: RecordingPlaybackPhase,
    val positionMs: Long,
    val speed: Float,
    val selectedA11yNodeId: String?,
    val wasPlaying: Boolean,
) {
    init {
        require(recordId.isNotBlank()) { "Replay record id must not be blank." }
        require(entryIndex >= 0) { "Replay entry index must not be negative." }
        require(positionMs >= 0L) { "Replay position must not be negative." }
    }
}

object RecordingCanonicalOrder {
    val scenes: Comparator<RecordingScene> =
        compareBy<RecordingScene> { it.sequence }
            .thenBy { it.openedAtElapsedRealtimeNanos }
            .thenBy { it.sceneId }

    val frames: Comparator<CaptureFrame> =
        compareBy<CaptureFrame> { it.capturedAtElapsedRealtimeNanos }
            .thenBy { it.frameId }

    val snapshots: Comparator<A11ySnapshot> =
        compareBy<A11ySnapshot> { it.capturedAtEpochMs }
            .thenBy { it.snapshotId }

    val events: Comparator<RawRecordingEvent> =
        compareBy<RawRecordingEvent> { it.sequence }
            .thenBy { it.occurredAtElapsedRealtimeNanos }
            .thenBy { it.rawEventId }

    val interactions: Comparator<RecordingInteraction> =
        compareBy<RecordingInteraction> { it.sequence }
            .thenBy { it.occurredAtElapsedRealtimeNanos }
            .thenBy { it.interactionId }
}

fun PersistedRecordingSession.canonicalOrder(): PersistedRecordingSession = copy(
    scenes = scenes.sortedWith(RecordingCanonicalOrder.scenes),
    frames = frames.sortedWith(RecordingCanonicalOrder.frames),
    assets = assets.sortedBy(ScreenshotAsset::assetHash),
    snapshots = snapshots.sortedWith(RecordingCanonicalOrder.snapshots),
    rawEvents = rawEvents.sortedWith(RecordingCanonicalOrder.events),
    interactions = interactions.sortedWith(RecordingCanonicalOrder.interactions),
)
