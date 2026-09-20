package com.visualtasker.wss.recording

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.visualtasker.wss.recording.persistence.RecorderDatabase
import com.visualtasker.wss.recording.persistence.RoomRecorderSessionStore
import com.visualtasker.wss.recording.persistence.ScreenshotAssetStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class R11RecoveryFixtureSeederTest {
    @Test
    fun seedInterruptedSessionWithReplayBookmark() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = RoomRecorderSessionStore(RecorderDatabase.get(context))
        val assetStore = ScreenshotAssetStore(File(context.filesDir, "recorder/assets"))
        val suffix = System.currentTimeMillis().toString()
        val sessionId = "r11-interrupted-$suffix"
        val sceneId = "scene-r11-$suffix"
        val frameId = "frame-r11-$suffix"
        val snapshotId = "a11y-r11-$suffix"
        val interactionId = "interaction-r11-$suffix"
        val startedAt = System.currentTimeMillis()
        val startedElapsed = android.os.SystemClock.elapsedRealtimeNanos()
        val storedAsset = assetStore.putPng(Base64.decode(ONE_PIXEL_PNG, Base64.DEFAULT)).asset
        val preparing = RecordingSession(
            sessionId = sessionId,
            status = RecordingSessionStatus.PREPARING,
            startedAtEpochMs = startedAt,
            startedAtElapsedRealtimeNanos = startedElapsed,
            createdBy = "r11-ui-fixture",
            recordingPolicySnapshot = RecordingPolicySnapshot(),
        )
        val scene = RecordingScene(
            sceneId = sceneId,
            sessionId = sessionId,
            sequence = 1L,
            openedAtEpochMs = startedAt,
            openedAtElapsedRealtimeNanos = startedElapsed,
            primaryFrameId = frameId,
            windowContext = RecordingWindowContext(
                packageName = context.packageName,
                activityName = "R11 Recovery",
                windowId = "window-r11",
                title = "Interrupted recording",
            ),
            status = RecordingSceneStatus.OPEN,
        )
        val frame = CaptureFrame(
            frameId = frameId,
            sessionId = sessionId,
            sceneId = sceneId,
            capturedAtEpochMs = startedAt,
            capturedAtElapsedRealtimeNanos = startedElapsed,
            assetHash = storedAsset.assetHash,
            assetReference = storedAsset.assetReference,
            widthPx = 1,
            heightPx = 1,
            rotation = 0,
            densityDpi = 480,
            captureStatus = RecordingCaptureStatus.CAPTURED,
        )
        val snapshot = A11ySnapshot(
            snapshotId = snapshotId,
            sessionId = sessionId,
            sceneId = sceneId,
            frameId = frameId,
            capturedAtEpochMs = startedAt,
            rootNode = A11yNodeSnapshot(
                stableSnapshotNodeId = "node-r11-$suffix",
                parentNodeId = null,
                childOrder = 0,
                className = "android.widget.FrameLayout",
                viewIdResourceName = null,
                text = "R11 interrupted fixture",
                contentDescription = "Recovery fixture",
                left = 0,
                top = 0,
                right = 1,
                bottom = 1,
                clickable = false,
                longClickable = false,
                scrollable = false,
                editable = false,
                enabled = true,
                selected = false,
                checked = false,
                visibleToUser = true,
                actions = emptyList(),
            ),
            windowId = "window-r11",
            packageName = context.packageName,
            status = RecordingCaptureStatus.CAPTURED,
        )
        val recording = preparing.copy(
            status = RecordingSessionStatus.RECORDING,
            initialSceneId = sceneId,
            latestSceneId = sceneId,
        )
        val rawEvent = RawRecordingEvent(
            rawEventId = "raw-r11-$suffix",
            sessionId = sessionId,
            sequence = 1L,
            occurredAtEpochMs = startedAt + 200L,
            occurredAtElapsedRealtimeNanos = startedElapsed + 200_000_000L,
            kind = "tap",
            payload = mapOf("fixture" to "R11"),
        )
        val interaction = RecordingInteraction(
            interactionId = interactionId,
            sessionId = sessionId,
            sequence = 1L,
            occurredAtEpochMs = rawEvent.occurredAtEpochMs,
            occurredAtElapsedRealtimeNanos = rawEvent.occurredAtElapsedRealtimeNanos,
            source = RecordingInteractionSource.RECORDER,
            rawEventIds = listOf(rawEvent.rawEventId),
            beforeSceneId = sceneId,
            afterSceneId = sceneId,
            status = RecordingInteractionStatus.UNCHANGED,
            payload = RecordingInteractionPayload.Tap(RecordingPoint(1, 1)),
        )

        store.createSession(preparing)
        store.persistInitialState(recording, scene, storedAsset, frame, snapshot)
        store.persistCanonicalImport(listOf(rawEvent), listOf(interaction))
        assertEquals(listOf(sessionId), store.recoverInterrupted("PROCESS_INTERRUPTED"))

        val bookmark = RecordingReplayBookmark(
            recordId = sessionId,
            entryId = "entry:$interactionId",
            entryIndex = 0,
            phase = RecordingPlaybackPhase.AFTER,
            positionMs = 650L,
            speed = 2f,
            selectedA11yNodeId = snapshot.rootNode.stableSnapshotNodeId,
            wasPlaying = false,
        )
        SharedPreferencesRecordingReplayBookmarkStore(context).save(bookmark)

        assertEquals(RecordingSessionStatus.INTERRUPTED, store.load(sessionId)?.session?.status)
        assertNotNull(SharedPreferencesRecordingReplayBookmarkStore(context).load(sessionId))
    }

    private companion object {
        const val ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    }
}
