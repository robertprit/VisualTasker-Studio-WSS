package com.visualtasker.wss.capture

import com.visualtasker.wss.window.DisplayId
import com.visualtasker.wss.window.WindowBounds
import com.visualtasker.wss.window.WindowCoordinateSpace

@JvmInline
value class ObservationId(val value: String) {
    init {
        require(value.isNotBlank()) { "ObservationId must not be blank" }
    }
}

enum class ObservationProducer {
    ACCESSIBILITY,
    OCR,
    OPENCV,
    YOLO,
    RECORDER,
    RUNTIME,
    INSPECTOR,
}

enum class ObservationResultStatus {
    OBSERVED,
    ABSENT,
    PARTIAL,
    FAILED,
}

sealed interface ObservationSourceReference {
    data class Frame(val frameId: CaptureFrameId) : ObservationSourceReference
    data class AccessibilitySnapshot(val snapshotId: String, val associatedFrameId: CaptureFrameId?) : ObservationSourceReference
}

data class Observation(
    val id: ObservationId,
    val source: ObservationSourceReference,
    val displayId: DisplayId,
    val observedAtEpochMs: Long,
    val coordinateSpace: WindowCoordinateSpace,
    val producer: ObservationProducer,
    val status: ObservationResultStatus,
    val confidence: Float? = null,
    val bounds: WindowBounds? = null,
    val properties: Map<String, String> = emptyMap(),
) {
    init {
        require(observedAtEpochMs >= 0L) { "Observation timestamp must not be negative" }
        require(confidence == null || confidence in 0f..1f) { "Observation confidence must be in 0..1" }
        require(bounds == null || bounds.space == coordinateSpace) { "Observation bounds must match coordinate space" }
    }
}

data class ElementMatch(
    val observationId: ObservationId,
    val stableElementId: String,
    val label: String?,
    val bounds: WindowBounds,
    val confidence: Float?,
)

data class AccessibilitySnapshotAssociation(
    val snapshotId: String,
    val frameId: CaptureFrameId,
    val snapshotTimestampEpochMs: Long,
    val frameTimestampEpochMs: Long,
) {
    val temporalOffsetMillis: Long get() = snapshotTimestampEpochMs - frameTimestampEpochMs
    val atomicallyCaptured: Boolean = false
}
