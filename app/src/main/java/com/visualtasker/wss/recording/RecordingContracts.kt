package com.visualtasker.wss.recording

const val RECORDING_SCHEMA_VERSION = 3

enum class RecordingSessionStatus { PREPARING, RECORDING, STOPPING, COMPLETED, PARTIAL, INTERRUPTED, FAILED, PAUSED }
enum class RecordingSceneStatus { OPEN, CLOSED }
enum class RecordingCaptureStatus { CAPTURED, REUSED, FAILED, DISCARDED }
enum class RecordingInteractionStatus { CAPTURING_AFTER, UNCHANGED, CHANGED, CAPTURE_FAILED }

data class RecordingPolicySnapshot(
    val captureScreenshot: Boolean = true,
    val captureAccessibility: Boolean = true,
    val captureText: Boolean = false,
)

data class RecordingSession(
    val sessionId: String,
    val schemaVersion: Int = RECORDING_SCHEMA_VERSION,
    val status: RecordingSessionStatus,
    val startedAtEpochMs: Long,
    val startedAtElapsedRealtimeNanos: Long,
    val stoppedAtEpochMs: Long? = null,
    val createdBy: String,
    val recordingPolicySnapshot: RecordingPolicySnapshot,
    val initialSceneId: String? = null,
    val latestSceneId: String? = null,
    val failure: String? = null,
    val resumedFromSessionId: String? = null,
)

data class RecordingWindowContext(
    val packageName: String?,
    val activityName: String?,
    val windowId: String?,
    val title: String? = null,
)

data class RecordingScene(
    val sceneId: String,
    val sessionId: String,
    val sequence: Long,
    val openedAtEpochMs: Long,
    val openedAtElapsedRealtimeNanos: Long,
    val closedAtEpochMs: Long? = null,
    val primaryFrameId: String,
    val windowContext: RecordingWindowContext,
    val status: RecordingSceneStatus,
)

data class ScreenshotAsset(
    val assetHash: String,
    val assetReference: String,
    val byteCount: Long,
    val createdAtEpochMs: Long,
)

data class CaptureFrame(
    val frameId: String,
    val sessionId: String,
    val sceneId: String,
    val capturedAtEpochMs: Long,
    val capturedAtElapsedRealtimeNanos: Long,
    val assetHash: String,
    val assetReference: String,
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Int,
    val densityDpi: Int,
    val captureStatus: RecordingCaptureStatus,
    val coordinateSpace: String = "screen_px",
)

data class A11yNodeSnapshot(
    val stableSnapshotNodeId: String,
    val parentNodeId: String?,
    val childOrder: Int,
    val className: String,
    val viewIdResourceName: String?,
    val text: String?,
    val contentDescription: String?,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val clickable: Boolean,
    val longClickable: Boolean,
    val scrollable: Boolean,
    val editable: Boolean,
    val enabled: Boolean,
    val selected: Boolean,
    val checked: Boolean,
    val visibleToUser: Boolean,
    val actions: List<String>,
    val children: List<A11yNodeSnapshot> = emptyList(),
)

data class A11ySnapshot(
    val snapshotId: String,
    val sessionId: String,
    val sceneId: String,
    val frameId: String,
    val capturedAtEpochMs: Long,
    val rootNode: A11yNodeSnapshot,
    val windowId: String?,
    val packageName: String?,
    val status: RecordingCaptureStatus,
)

data class RawRecordingEvent(
    val rawEventId: String,
    val sessionId: String,
    val sequence: Long,
    val occurredAtEpochMs: Long,
    val occurredAtElapsedRealtimeNanos: Long,
    val kind: String,
    val payload: Map<String, String>,
)

enum class RecordingInteractionType { TAP, SWIPE, TEXT, WINDOW, SCREENSHOT }
enum class RecordingInteractionSource { RECORDER, ACCESSIBILITY, OVERLAY, IMPORT, SYSTEM }
enum class RecordingWindowChange { APPEARED, DISAPPEARED, ACTIVITY_CHANGED, FOCUS_CHANGED, FOREGROUND_CHANGED }

data class RecordingPoint(
    val xPx: Int,
    val yPx: Int,
)

sealed interface RecordingInteractionPayload {
    data class Tap(
        val position: RecordingPoint,
        val durationMs: Long? = null,
        val pointerId: Int? = null,
        val button: String? = null,
        val targetReference: String? = null,
    ) : RecordingInteractionPayload

    data class Swipe(
        val start: RecordingPoint,
        val end: RecordingPoint,
        val durationMs: Long,
        val path: List<RecordingPoint> = emptyList(),
        val pointerId: Int? = null,
    ) : RecordingInteractionPayload

    data class Text(
        val value: String?,
        val redacted: Boolean,
        val valueHash: String? = null,
        val targetReference: String? = null,
        val inputMethod: String? = null,
    ) : RecordingInteractionPayload {
        init {
            require(!redacted || value == null) { "Redacted text must not retain its clear value." }
        }
    }

    data class Window(
        val change: RecordingWindowChange,
        val packageName: String?,
        val activityName: String?,
        val windowId: String?,
        val title: String? = null,
    ) : RecordingInteractionPayload

    data class Screenshot(
        val resource: RecordingResourceRef,
        val widthPx: Int? = null,
        val heightPx: Int? = null,
    ) : RecordingInteractionPayload {
        init {
            require(resource.kind == RecordingResourceKind.SCREENSHOT_ASSET) {
                "Screenshot interactions must reference a screenshot asset."
            }
        }
    }
}

data class RecordingInteraction(
    val interactionId: String,
    val sessionId: String,
    val sequence: Long,
    val occurredAtEpochMs: Long,
    val occurredAtElapsedRealtimeNanos: Long,
    val source: RecordingInteractionSource,
    val rawEventIds: List<String>,
    val evidenceRefs: List<String> = emptyList(),
    val beforeSceneId: String? = null,
    val afterSceneId: String? = null,
    val status: RecordingInteractionStatus,
    val payload: RecordingInteractionPayload,
) {
    val type: RecordingInteractionType
        get() = when (payload) {
            is RecordingInteractionPayload.Tap -> RecordingInteractionType.TAP
            is RecordingInteractionPayload.Swipe -> RecordingInteractionType.SWIPE
            is RecordingInteractionPayload.Text -> RecordingInteractionType.TEXT
            is RecordingInteractionPayload.Window -> RecordingInteractionType.WINDOW
            is RecordingInteractionPayload.Screenshot -> RecordingInteractionType.SCREENSHOT
        }

    init {
        require(interactionId.isNotBlank()) { "Interaction id must not be blank." }
        require(sessionId.isNotBlank()) { "Interaction session id must not be blank." }
        require(sequence >= 0L) { "Interaction sequence must not be negative." }
        require(rawEventIds.isNotEmpty()) { "Canonical interactions require raw provenance." }
        require(rawEventIds.none(String::isBlank)) { "Raw event ids must not be blank." }
    }
}

fun recordingTapInteraction(
    interactionId: String,
    rawEventId: String,
    sessionId: String,
    sequence: Long,
    occurredAtEpochMs: Long,
    occurredAtElapsedRealtimeNanos: Long,
    xPx: Int,
    yPx: Int,
    beforeSceneId: String,
    afterSceneId: String? = null,
    targetA11yNodeId: String? = null,
    status: RecordingInteractionStatus,
): RecordingInteraction = RecordingInteraction(
    interactionId = interactionId,
    sessionId = sessionId,
    sequence = sequence,
    occurredAtEpochMs = occurredAtEpochMs,
    occurredAtElapsedRealtimeNanos = occurredAtElapsedRealtimeNanos,
    source = RecordingInteractionSource.RECORDER,
    rawEventIds = listOf(rawEventId),
    beforeSceneId = beforeSceneId,
    afterSceneId = afterSceneId,
    status = status,
    payload = RecordingInteractionPayload.Tap(
        position = RecordingPoint(xPx, yPx),
        targetReference = targetA11yNodeId,
    ),
)

data class RecorderCapture(
    val pngBytes: ByteArray,
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Int,
    val densityDpi: Int,
    val windowContext: RecordingWindowContext,
    val a11yRoot: A11yNodeSnapshot,
)

data class PersistedRecordingSession(
    val session: RecordingSession,
    val scenes: List<RecordingScene>,
    val frames: List<CaptureFrame>,
    val assets: List<ScreenshotAsset>,
    val snapshots: List<A11ySnapshot>,
    val rawEvents: List<RawRecordingEvent>,
    val interactions: List<RecordingInteraction>,
)

sealed interface RecordingOperationResult<out T> {
    data class Success<T>(val value: T) : RecordingOperationResult<T>
    data class Failure(val code: String, val message: String, val cause: Throwable? = null) : RecordingOperationResult<Nothing>
}

fun interface RecorderCaptureSource {
    suspend fun capture(): RecorderCapture
}

interface RecorderClock {
    fun epochMillis(): Long
    fun elapsedRealtimeNanos(): Long
}

fun interface RecorderIdGenerator {
    fun nextId(prefix: String): String
}
