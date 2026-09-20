package com.visualtasker.wss.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RecordingReplayRecoveryTest {
    @Test
    fun bookmarkCodecPreservesRecoveryPositionSpeedAndSelectedStep() {
        val source = bookmark()

        val restored = RecordingReplayBookmarkCodec.decode(RecordingReplayBookmarkCodec.encode(source))

        assertEquals(source, restored)
        assertEquals("entry-7", restored?.entryId)
        assertEquals(2f, restored?.speed)
        assertEquals(1_250L, restored?.positionMs)
    }

    @Test
    fun restartDoesNotImplicitlyDeletePersistedBookmark() {
        val store = InMemoryBookmarkStore().apply { save(bookmark()) }
        val before = store.load("record-interrupted")
        val controller = RecordingPlaybackController().apply {
            load(interruptedDocument(), before)
            restart()
        }

        val after = store.load("record-interrupted")

        assertNotNull(before)
        assertEquals(before, after)
        assertNotNull(controller.bookmark())
    }

    @Test
    fun bookmarkIsRemovedOnlyByExplicitDelete() {
        val store = InMemoryBookmarkStore().apply { save(bookmark()) }

        store.clear("record-interrupted")

        assertNull(store.load("record-interrupted"))
    }

    private fun bookmark() = RecordingReplayBookmark(
        recordId = "record-interrupted",
        entryId = "entry-7",
        entryIndex = 7,
        phase = RecordingPlaybackPhase.AFTER,
        positionMs = 1_250L,
        speed = 2f,
        selectedA11yNodeId = "node-login",
        wasPlaying = true,
    )

    private fun interruptedDocument() = RecordingPlaybackDocument(
        sessionId = "record-interrupted",
        schemaVersion = RECORDING_SCHEMA_VERSION,
        sessionStatus = RecordingSessionStatus.INTERRUPTED,
        startedAtEpochMs = 1_000L,
        stoppedAtEpochMs = null,
        durationMs = 0L,
        initialScene = null,
        scenes = emptyList(),
        entries = emptyList(),
        diagnostics = emptyList(),
    )
}

private class InMemoryBookmarkStore : RecordingReplayBookmarkStore {
    private val values = mutableMapOf<String, RecordingReplayBookmark>()
    override fun load(recordId: String): RecordingReplayBookmark? = values[recordId]
    override fun save(bookmark: RecordingReplayBookmark) { values[bookmark.recordId] = bookmark }
    override fun clear(recordId: String) { values.remove(recordId) }
}
