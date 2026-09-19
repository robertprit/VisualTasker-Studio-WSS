package com.visualtasker.wss.recording

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.visualtasker.wss.recording.persistence.RecorderDatabase
import com.visualtasker.wss.recording.persistence.RoomRecorderSessionStore
import com.visualtasker.wss.recording.persistence.RoomStepReviewStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecorderRoomPersistenceTest {
    private lateinit var context: Context
    private lateinit var databaseFile: File
    private var database: RecorderDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseFile = context.getDatabasePath("recorder-r1-test.db")
        context.deleteDatabase(databaseFile.name)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseFile.name)
    }

    @Test
    fun sessionSurvivesCloseAndReloadWithStableReferences() = runBlocking {
        var db = openDatabase()
        var store = RoomRecorderSessionStore(db)
        val fixture = initialFixture()
        store.createSession(fixture.preparing)
        store.persistInitialState(fixture.recording, fixture.scene, fixture.asset, fixture.frame, fixture.snapshot)
        db.close()
        database = null

        db = openDatabase()
        store = RoomRecorderSessionStore(db)
        val loaded = store.load(fixture.recording.sessionId)

        assertNotNull(loaded)
        assertEquals(fixture.recording.sessionId, loaded!!.session.sessionId)
        assertEquals(fixture.scene.sceneId, loaded.scenes.single().sceneId)
        assertEquals(fixture.frame.frameId, loaded.frames.single().frameId)
        assertEquals(fixture.asset.assetHash, loaded.assets.single().assetHash)
        assertEquals(fixture.snapshot.snapshotId, loaded.snapshots.single().snapshotId)
        assertEquals(RecordingSessionStatus.RECORDING, loaded.session.status)
    }

    @Test
    fun recoveryMarksOpenSessionPartialAndPreservesGraph() = runBlocking {
        val db = openDatabase()
        val store = RoomRecorderSessionStore(db)
        val fixture = initialFixture()
        store.createSession(fixture.preparing)
        store.persistInitialState(fixture.recording, fixture.scene, fixture.asset, fixture.frame, fixture.snapshot)

        assertEquals(listOf(fixture.recording.sessionId), store.recoverInterrupted("PROCESS_INTERRUPTED"))
        val loaded = store.load(fixture.recording.sessionId)!!
        assertEquals(RecordingSessionStatus.PARTIAL, loaded.session.status)
        assertEquals("PROCESS_INTERRUPTED", loaded.session.failure)
        assertEquals(1, loaded.scenes.size)
        assertEquals(1, loaded.frames.size)
        assertEquals(1, loaded.snapshots.size)
    }

    @Test
    fun reviewDecisionPersistsSeparatelyAndKeepsOriginalProposal() = runBlocking {
        var db = openDatabase()
        var recorderStore = RoomRecorderSessionStore(db)
        var reviewStore = RoomStepReviewStore(db)
        val fixture = initialFixture()
        recorderStore.createSession(fixture.preparing)
        recorderStore.persistInitialState(fixture.recording, fixture.scene, fixture.asset, fixture.frame, fixture.snapshot)
        val originalTarget = CandidateTarget.A11y(
            nodeId = "node-0", resourceId = null, text = null, contentDescription = null,
            className = "FrameLayout", left = 0, top = 0, right = 100, bottom = 200,
            clickable = false, visible = true, enabled = true,
            packageName = "com.example", windowId = "window-1", xPx = 30, yPx = 50,
        )
        val correctedTarget = CandidateTarget.Coordinate(30, 50)
        val decision = StepReviewDecision(
            decisionId = "review:candidate-1",
            candidateId = "candidate-1",
            sourceSessionId = fixture.recording.sessionId,
            sourceRecordVersion = fixture.recording.schemaVersion,
            status = StepReviewStatus.CORRECTED,
            originalProposal = StepProposalSnapshot(CandidateAction.TAP, originalTarget, "FrameLayout"),
            correctedProposal = StepProposalSnapshot(CandidateAction.TAP, correctedTarget, "Koordinate"),
            selectedTargetNodeId = null,
            reasonCode = "COORDINATE_FALLBACK_SELECTED",
            note = "Test",
            decidedAtEpochMs = 2_000L,
        )
        val recordBefore = recorderStore.load(fixture.recording.sessionId)
        reviewStore.save(decision)
        db.close()
        database = null

        db = openDatabase()
        recorderStore = RoomRecorderSessionStore(db)
        reviewStore = RoomStepReviewStore(db)
        val loaded = reviewStore.loadForSession(fixture.recording.sessionId).single()

        assertEquals(StepReviewStatus.CORRECTED, loaded.status)
        assertEquals(decision.originalProposal, loaded.originalProposal)
        assertEquals(decision.correctedProposal, loaded.correctedProposal)
        assertNotEquals(loaded.originalProposal, loaded.correctedProposal)
        assertEquals(recordBefore, recorderStore.load(fixture.recording.sessionId))
    }

    @Test
    fun reviewForUnknownSessionIsRejected() = runBlocking {
        val store = RoomStepReviewStore(openDatabase())
        val result = runCatching {
            store.save(
                StepReviewDecision(
                    decisionId = "review:missing",
                    candidateId = "candidate-missing",
                    sourceSessionId = "missing-session",
                    sourceRecordVersion = 1,
                    status = StepReviewStatus.REJECTED,
                    originalProposal = StepProposalSnapshot(
                        CandidateAction.TAP,
                        CandidateTarget.Coordinate(1, 1),
                        "Tap",
                    ),
                    correctedProposal = null,
                    selectedTargetNodeId = null,
                    reasonCode = null,
                    note = null,
                    decidedAtEpochMs = 2_000L,
                )
            )
        }

        assertTrue(result.isFailure)
    }

    private fun openDatabase(): RecorderDatabase = Room.databaseBuilder(
        context,
        RecorderDatabase::class.java,
        databaseFile.name,
    ).build().also { database = it }

    private fun initialFixture(): InitialFixture {
        val preparing = RecordingSession(
            sessionId = "session-1",
            status = RecordingSessionStatus.PREPARING,
            startedAtEpochMs = 1000,
            startedAtElapsedRealtimeNanos = 10,
            createdBy = "test",
            recordingPolicySnapshot = RecordingPolicySnapshot(),
        )
        val scene = RecordingScene(
            sceneId = "scene-1",
            sessionId = preparing.sessionId,
            sequence = 1,
            openedAtEpochMs = 1000,
            openedAtElapsedRealtimeNanos = 10,
            primaryFrameId = "frame-1",
            windowContext = RecordingWindowContext("com.example", "MainActivity", "window-1"),
            status = RecordingSceneStatus.OPEN,
        )
        val asset = ScreenshotAsset("hash-a", "screenshots/ha/hash-a.png", 9, 1000)
        val frame = CaptureFrame(
            frameId = "frame-1", sessionId = preparing.sessionId, sceneId = scene.sceneId,
            capturedAtEpochMs = 1000, capturedAtElapsedRealtimeNanos = 10,
            assetHash = asset.assetHash, assetReference = asset.assetReference,
            widthPx = 100, heightPx = 200, rotation = 0, densityDpi = 480,
            captureStatus = RecordingCaptureStatus.CAPTURED,
        )
        val snapshot = A11ySnapshot(
            snapshotId = "a11y-1", sessionId = preparing.sessionId, sceneId = scene.sceneId,
            frameId = frame.frameId, capturedAtEpochMs = 1000,
            rootNode = A11yNodeSnapshot(
                stableSnapshotNodeId = "node-0", parentNodeId = null, childOrder = 0,
                className = "FrameLayout", viewIdResourceName = null, text = null, contentDescription = null,
                left = 0, top = 0, right = 100, bottom = 200,
                clickable = false, longClickable = false, scrollable = false, editable = false,
                enabled = true, selected = false, checked = false, visibleToUser = true,
                actions = emptyList(),
            ),
            windowId = "window-1", packageName = "com.example", status = RecordingCaptureStatus.CAPTURED,
        )
        return InitialFixture(
            preparing = preparing,
            recording = preparing.copy(
                status = RecordingSessionStatus.RECORDING,
                initialSceneId = scene.sceneId,
                latestSceneId = scene.sceneId,
            ),
            scene = scene,
            asset = asset,
            frame = frame,
            snapshot = snapshot,
        )
    }
}

private data class InitialFixture(
    val preparing: RecordingSession,
    val recording: RecordingSession,
    val scene: RecordingScene,
    val asset: ScreenshotAsset,
    val frame: CaptureFrame,
    val snapshot: A11ySnapshot,
)
