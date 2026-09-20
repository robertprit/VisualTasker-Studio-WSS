package com.visualtasker.wss.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingDomainContractsTest {
    @Test
    fun canonicalOrderIsStableAcrossTenLoadsWithTimestampTies() {
        val original = record(
            interactions = listOf(
                interaction("tap-d", sequence = 4, elapsed = 175),
                interaction("tap-c", sequence = 3, elapsed = 120),
                interaction("tap-a", sequence = 1, elapsed = 100),
                interaction("tap-b", sequence = 2, elapsed = 120),
            ),
        )

        val orders = List(10) {
            original.copy(interactions = original.interactions.shuffled()).canonicalOrder()
                .interactions.map(RecordingInteraction::interactionId)
        }

        assertTrue(orders.all { it == listOf("tap-a", "tap-b", "tap-c", "tap-d") })
    }

    @Test
    fun bookmarkPersistenceIsThrottledButSemanticChangesAreImmediate() {
        val base = bookmark(position = 100, entryId = "entry:1")

        assertTrue(shouldPersistReplayBookmark(null, base))
        assertFalse(shouldPersistReplayBookmark(base, base.copy(positionMs = 200)))
        assertTrue(shouldPersistReplayBookmark(base, base.copy(positionMs = 600)))
        assertTrue(shouldPersistReplayBookmark(base, base.copy(entryId = "entry:2")))
        assertTrue(shouldPersistReplayBookmark(base, base.copy(wasPlaying = true)))
    }

    private fun bookmark(position: Long, entryId: String) = RecordingReplayBookmark(
        recordId = "session-1",
        entryId = entryId,
        entryIndex = 0,
        phase = RecordingPlaybackPhase.BEFORE,
        positionMs = position,
        speed = 1f,
        selectedA11yNodeId = null,
        wasPlaying = false,
    )

    private fun record(interactions: List<RecordingInteraction>) = PersistedRecordingSession(
        session = RecordingSession(
            sessionId = "session-1",
            status = RecordingSessionStatus.COMPLETED,
            startedAtEpochMs = 0,
            startedAtElapsedRealtimeNanos = 0,
            stoppedAtEpochMs = 200,
            createdBy = "test",
            recordingPolicySnapshot = RecordingPolicySnapshot(),
        ),
        scenes = emptyList(),
        frames = emptyList(),
        assets = emptyList(),
        snapshots = emptyList(),
        rawEvents = emptyList(),
        interactions = interactions,
    )

    private fun interaction(id: String, sequence: Long, elapsed: Long) = recordingTapInteraction(
        interactionId = id,
        rawEventId = "raw-$id",
        sessionId = "session-1",
        sequence = sequence,
        occurredAtEpochMs = elapsed,
        occurredAtElapsedRealtimeNanos = elapsed,
        xPx = 0,
        yPx = 0,
        beforeSceneId = "scene-1",
        status = RecordingInteractionStatus.UNCHANGED,
    )
}
