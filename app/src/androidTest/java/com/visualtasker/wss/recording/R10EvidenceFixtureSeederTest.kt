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
class R10EvidenceFixtureSeederTest {
    @Test
    fun seedCanonicalEvidenceInspectorFixture() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = RoomRecorderSessionStore(RecorderDatabase.get(context))
        val assetStore = ScreenshotAssetStore(File(context.filesDir, "recorder/assets"))
        val suffix = System.currentTimeMillis().toString()
        val sessionId = "r10-evidence-$suffix"
        val sceneId = "scene-$suffix"
        val frameId = "frame-$suffix"
        val snapshotId = "a11y-$suffix"
        val startedAt = System.currentTimeMillis()
        val startedElapsed = android.os.SystemClock.elapsedRealtimeNanos()
        val storedAsset = assetStore.putPng(Base64.decode(ONE_PIXEL_PNG, Base64.DEFAULT)).asset
        val preparing = RecordingSession(
            sessionId = sessionId,
            status = RecordingSessionStatus.PREPARING,
            startedAtEpochMs = startedAt,
            startedAtElapsedRealtimeNanos = startedElapsed,
            createdBy = "r10-ui-fixture",
            recordingPolicySnapshot = RecordingPolicySnapshot(),
        )
        val openScene = RecordingScene(
            sceneId = sceneId,
            sessionId = sessionId,
            sequence = 1L,
            openedAtEpochMs = startedAt,
            openedAtElapsedRealtimeNanos = startedElapsed,
            primaryFrameId = frameId,
            windowContext = RecordingWindowContext(
                packageName = context.packageName,
                activityName = "R10 Evidence Inspector",
                windowId = "window-r10",
                title = "RailTrace Evidence",
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
                stableSnapshotNodeId = "root-$suffix",
                parentNodeId = null,
                childOrder = 0,
                className = "android.widget.FrameLayout",
                viewIdResourceName = null,
                text = "R10 fixture",
                contentDescription = "Evidence inspector fixture",
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
            windowId = "window-r10",
            packageName = context.packageName,
            status = RecordingCaptureStatus.CAPTURED,
        )
        val recording = preparing.copy(
            status = RecordingSessionStatus.RECORDING,
            initialSceneId = sceneId,
            latestSceneId = sceneId,
        )

        store.createSession(preparing)
        store.persistInitialState(recording, openScene, storedAsset, frame, snapshot)

        val validRaw = rawEvent(sessionId, suffix, 1L, startedAt, startedElapsed)
        val brokenRaw = rawEvent(sessionId, suffix, 2L, startedAt + 200L, startedElapsed + 200_000_000L)
        val validInteraction = RecordingInteraction(
            interactionId = "interaction-valid-$suffix",
            sessionId = sessionId,
            sequence = 1L,
            occurredAtEpochMs = validRaw.occurredAtEpochMs,
            occurredAtElapsedRealtimeNanos = validRaw.occurredAtElapsedRealtimeNanos,
            source = RecordingInteractionSource.RECORDER,
            rawEventIds = listOf(validRaw.rawEventId),
            beforeSceneId = sceneId,
            afterSceneId = sceneId,
            status = RecordingInteractionStatus.UNCHANGED,
            payload = RecordingInteractionPayload.Tap(RecordingPoint(1, 1)),
        )
        val brokenInteraction = RecordingInteraction(
            interactionId = "interaction-broken-$suffix",
            sessionId = sessionId,
            sequence = 2L,
            occurredAtEpochMs = brokenRaw.occurredAtEpochMs,
            occurredAtElapsedRealtimeNanos = brokenRaw.occurredAtElapsedRealtimeNanos,
            source = RecordingInteractionSource.IMPORT,
            rawEventIds = listOf(brokenRaw.rawEventId),
            evidenceRefs = listOf("evidence-missing-$suffix"),
            beforeSceneId = sceneId,
            afterSceneId = sceneId,
            status = RecordingInteractionStatus.UNCHANGED,
            payload = RecordingInteractionPayload.Screenshot(
                resource = RecordingResourceRef(
                    RecordingResourceKind.SCREENSHOT_ASSET,
                    "asset-missing-$suffix",
                ),
                widthPx = 1,
                heightPx = 1,
            ),
        )
        store.persistCanonicalImport(
            rawEvents = listOf(validRaw, brokenRaw),
            interactions = listOf(validInteraction, brokenInteraction),
        )
        store.finalizeSession(
            recording.copy(
                status = RecordingSessionStatus.COMPLETED,
                stoppedAtEpochMs = startedAt + 400L,
            ),
            openScene.copy(
                status = RecordingSceneStatus.CLOSED,
                closedAtEpochMs = startedAt + 400L,
            ),
        )

        val persisted = store.load(sessionId)
        assertNotNull(persisted)
        assertEquals(RecordingSessionStatus.COMPLETED, persisted?.session?.status)
        assertEquals(2, persisted?.interactions?.size)
    }

    private fun rawEvent(
        sessionId: String,
        suffix: String,
        sequence: Long,
        epochMs: Long,
        elapsedNanos: Long,
    ) = RawRecordingEvent(
        rawEventId = "raw-$sequence-$suffix",
        sessionId = sessionId,
        sequence = sequence,
        occurredAtEpochMs = epochMs,
        occurredAtElapsedRealtimeNanos = elapsedNanos,
        kind = if (sequence == 1L) "tap" else "screenshot",
        payload = mapOf("fixture" to "R10", "sequence" to sequence.toString()),
    )

    private companion object {
        const val ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    }
}
