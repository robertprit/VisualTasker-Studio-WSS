package com.visualtasker.wss.recording.persistence

import com.visualtasker.wss.recording.RecordingInteraction
import com.visualtasker.wss.recording.RecordingInteractionPayload
import com.visualtasker.wss.recording.RecordingInteractionSource
import com.visualtasker.wss.recording.RecordingInteractionStatus
import com.visualtasker.wss.recording.RecordingPoint
import com.visualtasker.wss.recording.RecordingResourceKind
import com.visualtasker.wss.recording.RecordingResourceRef
import com.visualtasker.wss.recording.RecordingWindowChange
import org.junit.Assert.assertEquals
import org.junit.Test

class RecorderEntityMapperTest {
    @Test
    fun allCanonicalPayloadsRoundTripThroughPersistenceJson() {
        val payloads = listOf(
            RecordingInteractionPayload.Tap(RecordingPoint(1, 2), 40, 3, "primary", "target"),
            RecordingInteractionPayload.Swipe(RecordingPoint(1, 2), RecordingPoint(30, 40), 250, listOf(RecordingPoint(10, 20)), 2),
            RecordingInteractionPayload.Text("hello", false, targetReference = "input", inputMethod = "ime"),
            RecordingInteractionPayload.Text(null, true, valueHash = "hash", targetReference = "password"),
            RecordingInteractionPayload.Window(RecordingWindowChange.ACTIVITY_CHANGED, "com.example", "HomeActivity", "window-1", "Home"),
            RecordingInteractionPayload.Screenshot(RecordingResourceRef(RecordingResourceKind.SCREENSHOT_ASSET, "asset-1"), 100, 200),
        )

        payloads.forEachIndexed { index, payload ->
            val source = RecordingInteraction(
                interactionId = "interaction-$index",
                sessionId = "session-1",
                sequence = index.toLong(),
                occurredAtEpochMs = 1_000L + index,
                occurredAtElapsedRealtimeNanos = index * 1_000_000L,
                source = RecordingInteractionSource.IMPORT,
                rawEventIds = listOf("raw-$index"),
                evidenceRefs = listOf("evidence-$index"),
                beforeSceneId = "before",
                afterSceneId = "after",
                status = RecordingInteractionStatus.CHANGED,
                payload = payload,
            )

            assertEquals(source, source.toEntity().toDomain())
        }
    }
}
