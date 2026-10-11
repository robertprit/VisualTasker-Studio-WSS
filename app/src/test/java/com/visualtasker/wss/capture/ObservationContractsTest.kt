package com.visualtasker.wss.capture

import com.visualtasker.wss.recording.A11yNodeSnapshot
import com.visualtasker.wss.recording.A11ySnapshot
import com.visualtasker.wss.recording.CaptureFrame as RecordingCaptureFrame
import com.visualtasker.wss.recording.RecordingCaptureStatus
import com.visualtasker.wss.window.DisplayContext
import com.visualtasker.wss.window.DisplayId
import com.visualtasker.wss.window.WindowCoordinateSpace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ObservationContractsTest {
    @Test
    fun ocrAndOpenCvObservationsCanReferenceSameFrameWithoutOwningImageBytes() {
        val frameId = CaptureFrameId("frame-shared")
        val ocr = observation(frameId, ObservationProducer.OCR, "ocr")
        val openCv = observation(frameId, ObservationProducer.OPENCV, "opencv")

        assertEquals(ObservationSourceReference.Frame(frameId), ocr.source)
        assertEquals(ocr.source, openCv.source)
        assertFalse(Observation::class.java.declaredFields.any { it.type == ByteArray::class.java })
    }

    @Test
    fun recorderFrameProjectsToObservationAndTraceProvenance() {
        val recording = recordingFrame()
        val observation = recording.toFrameObservation()
        assertNotNull(observation)
        assertEquals(DisplayId(7), observation?.displayId)

        val frame = captureFrame()
        val trace = frame.toTraceInfo()
        assertEquals("frame-shared", trace.frameId)
        assertEquals(7, trace.displayId)
        assertEquals("TEST", trace.source)
    }

    @Test
    fun accessibilityAssociationIsTemporalAndNeverClaimsAtomicCapture() {
        val frame = recordingFrame()
        val snapshot = A11ySnapshot(
            snapshotId = "snapshot-1",
            sessionId = "session-1",
            sceneId = "scene-1",
            frameId = frame.frameId,
            capturedAtEpochMs = 1_010,
            rootNode = root(),
            windowId = "window-1",
            packageName = "example",
            status = RecordingCaptureStatus.CAPTURED,
        )
        val association = snapshot.toCaptureAssociation(frame)
        assertNotNull(association)
        assertEquals(10L, association?.temporalOffsetMillis)
        assertFalse(requireNotNull(association).atomicallyCaptured)
    }

    @Test
    fun legacyRecorderFrameWithoutCaptureReferenceRemainsReadable() {
        val legacy = recordingFrame().copy(sourceCaptureFrameId = null, displayId = null, captureSource = null)
        assertNull(legacy.toFrameObservation())
        assertNull(legacy.toObservationSourceReference())
    }

    private fun observation(frameId: CaptureFrameId, producer: ObservationProducer, suffix: String) = Observation(
        id = ObservationId("observation-$suffix"),
        source = ObservationSourceReference.Frame(frameId),
        displayId = DisplayId(7),
        observedAtEpochMs = 1_000,
        coordinateSpace = WindowCoordinateSpace.SCREENSHOT_PX,
        producer = producer,
        status = ObservationResultStatus.OBSERVED,
        confidence = 0.9f,
    )

    private fun recordingFrame() = RecordingCaptureFrame(
        frameId = "recording-frame-1",
        sessionId = "session-1",
        sceneId = "scene-1",
        capturedAtEpochMs = 1_000,
        capturedAtElapsedRealtimeNanos = 2_000,
        assetHash = "hash",
        assetReference = "asset.png",
        widthPx = 100,
        heightPx = 200,
        rotation = 0,
        densityDpi = 320,
        captureStatus = RecordingCaptureStatus.CAPTURED,
        sourceCaptureFrameId = "frame-shared",
        displayId = 7,
        captureSource = "ACCESSIBILITY",
    )

    private fun captureFrame(): CaptureFrame {
        val id = CaptureFrameId("frame-shared")
        return CaptureFrame(
            id,
            CaptureMetadata(
                DisplayId(7),
                DisplayContext(DisplayId(7), 100, 200, 2f, 0),
                1_000,
                2_000,
                100,
                200,
                0,
                320,
                CaptureSource.TEST,
            ),
            CaptureDataReference(id, "image/png", 4),
        )
    }

    private fun root() = A11yNodeSnapshot(
        "root", null, 0, "FrameLayout", null, null, null,
        0, 0, 100, 200, false, false, false, false,
        true, false, false, true, emptyList(),
    )
}
