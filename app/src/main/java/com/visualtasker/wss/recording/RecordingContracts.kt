package com.visualtasker.wss.recording

const val RECORDING_SCHEMA_VERSION = 1

enum class RecordingSessionStatus { PREPARING, RECORDING, STOPPING, COMPLETED, PARTIAL, FAILED, PAUSED }
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

data class TapInteraction(
    val interactionId: String,
    val rawEventId: String,
    val sessionId: String,
    val sequence: Long,
    val occurredAtEpochMs: Long,
    val occurredAtElapsedRealtimeNanos: Long,
    val xPx: Int,
    val yPx: Int,
    val beforeSceneId: String,
    val afterSceneId: String? = null,
    val targetA11yNodeId: String? = null,
    val status: RecordingInteractionStatus,
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
    val interactions: List<TapInteraction>,
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
