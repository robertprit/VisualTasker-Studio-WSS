package com.visualtasker.wss.capture

import com.visualtasker.wss.emscript.runtime.CaptureTraceInfo
import com.visualtasker.wss.recording.A11ySnapshot
import com.visualtasker.wss.recording.CaptureFrame as RecordingCaptureFrame
import com.visualtasker.wss.window.DisplayId
import com.visualtasker.wss.window.WindowBounds
import com.visualtasker.wss.window.WindowCoordinateSpace

fun CaptureFrame.toTraceInfo(): CaptureTraceInfo = CaptureTraceInfo(
    frameId = id.value,
    displayId = metadata.actualDisplayContext.displayId.value,
    source = metadata.source.name,
    coordinateSpace = metadata.coordinateSpace.name,
    capturedAtEpochMs = metadata.capturedAtEpochMs,
    widthPx = metadata.widthPx,
    heightPx = metadata.heightPx,
)

fun RecordingCaptureFrame.toObservationSourceReference(): ObservationSourceReference.Frame? =
    sourceCaptureFrameId?.let { ObservationSourceReference.Frame(CaptureFrameId(it)) }

fun A11ySnapshot.toCaptureAssociation(frame: RecordingCaptureFrame): AccessibilitySnapshotAssociation? {
    val captureFrameId = frame.sourceCaptureFrameId ?: return null
    if (frame.frameId != frameId) return null
    return AccessibilitySnapshotAssociation(
        snapshotId = snapshotId,
        frameId = CaptureFrameId(captureFrameId),
        snapshotTimestampEpochMs = capturedAtEpochMs,
        frameTimestampEpochMs = frame.capturedAtEpochMs,
    )
}

fun RecordingCaptureFrame.toFrameObservation(
    observationId: String = "observation:frame:$frameId",
): Observation? {
    val source = toObservationSourceReference() ?: return null
    val resolvedDisplayId = displayId ?: return null
    return Observation(
        id = ObservationId(observationId),
        source = source,
        displayId = DisplayId(resolvedDisplayId),
        observedAtEpochMs = capturedAtEpochMs,
        coordinateSpace = WindowCoordinateSpace.SCREENSHOT_PX,
        producer = ObservationProducer.RECORDER,
        status = when (captureStatus.name) {
            "FAILED", "DISCARDED" -> ObservationResultStatus.FAILED
            else -> ObservationResultStatus.OBSERVED
        },
        bounds = WindowBounds(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), WindowCoordinateSpace.SCREENSHOT_PX),
        properties = mapOf(
            "recordingFrameId" to frameId,
            "assetReference" to assetReference,
            "captureSource" to captureSource.orEmpty(),
        ).filterValues(String::isNotBlank),
    )
}
