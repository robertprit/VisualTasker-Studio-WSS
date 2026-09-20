package com.visualtasker.wss.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingInteractionCanonicalizerTest {
    @Test
    fun canonicalizesAllSupportedPayloadsWithStableRawProvenance() {
        val events = listOf(
            raw(5, "screenshot", mapOf("assetHash" to "asset-1", "widthPx" to "100")),
            raw(1, "click", mapOf("x" to "10", "y" to "20", "resourceId" to "login")),
            raw(3, "text.change", mapOf("text" to "secret", "password" to "true")),
            raw(2, "swipe", mapOf("startX" to "1", "startY" to "2", "endX" to "30", "endY" to "40", "durationMs" to "250", "path" to "1,2;15,20;30,40")),
            raw(4, "activity.change", mapOf("package" to "com.example", "activity" to "HomeActivity")),
        )

        val interactions = RecordingInteractionCanonicalizer.canonicalize(events)

        assertEquals(
            listOf(
                RecordingInteractionType.TAP,
                RecordingInteractionType.SWIPE,
                RecordingInteractionType.TEXT,
                RecordingInteractionType.WINDOW,
                RecordingInteractionType.SCREENSHOT,
            ),
            interactions.map(RecordingInteraction::type),
        )
        interactions.forEachIndexed { index, interaction ->
            assertEquals(listOf("raw-${index + 1}"), interaction.rawEventIds)
            assertEquals("interaction:raw-${index + 1}", interaction.interactionId)
        }
        val text = interactions[2].payload as RecordingInteractionPayload.Text
        assertTrue(text.redacted)
        assertNull(text.value)
        assertEquals(64, text.valueHash?.length)
        val swipe = interactions[1].payload as RecordingInteractionPayload.Swipe
        assertEquals(3, swipe.path.size)
    }

    @Test
    fun ignoresUnsupportedOrIncompleteRawEventsWithoutInventingMeaning() {
        val events = listOf(
            raw(1, "gesture.start", emptyMap()),
            raw(2, "click", mapOf("x" to "10")),
            raw(3, "recording.started", emptyMap()),
        )

        assertTrue(RecordingInteractionCanonicalizer.canonicalize(events).isEmpty())
    }

    @Test
    fun rejectsClearTextInsideRedactedPayload() {
        val result = runCatching { RecordingInteractionPayload.Text("secret", redacted = true) }
        assertFalse(result.isSuccess)
    }

    private fun raw(sequence: Long, kind: String, payload: Map<String, String>) = RawRecordingEvent(
        rawEventId = "raw-$sequence",
        sessionId = "session-1",
        sequence = sequence,
        occurredAtEpochMs = 1_000L + sequence,
        occurredAtElapsedRealtimeNanos = sequence * 1_000_000L,
        kind = kind,
        payload = payload + ("source" to "floatingOverlay"),
    )
}
