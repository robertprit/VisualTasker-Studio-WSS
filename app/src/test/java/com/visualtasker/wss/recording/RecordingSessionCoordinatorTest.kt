package com.visualtasker.wss.recording

import com.visualtasker.wss.recording.ScreenshotAssetStoreTest.Companion.PNG_A
import com.visualtasker.wss.recording.ScreenshotAssetStoreTest.Companion.PNG_B
import com.visualtasker.wss.recording.persistence.RecorderSessionStore
import com.visualtasker.wss.recording.persistence.ScreenshotAssetStore
import java.nio.file.Files
import java.util.ArrayDeque
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingSessionCoordinatorTest {
    @Test
    fun startPersistsCompleteInitialScene() = runBlocking {
        val fixture = fixture(capture(PNG_A))
        val result = fixture.coordinator.start("test") as RecordingOperationResult.Success
        val record = fixture.store.load(result.value.sessionId)!!

        assertEquals(RecordingSessionStatus.RECORDING, record.session.status)
        assertEquals(1, record.scenes.size)
        assertEquals(1, record.frames.size)
        assertEquals(1, record.assets.size)
        assertEquals(1, record.snapshots.size)
        assertEquals(record.scenes.single().sceneId, record.session.initialSceneId)
        assertEquals(record.frames.single().frameId, record.scenes.single().primaryFrameId)
        assertEquals(record.frames.single().assetHash, record.assets.single().assetHash)
    }

    @Test
    fun identicalAfterCaptureReusesAssetAndScene() = runBlocking {
        val fixture = fixture(capture(PNG_A), capture(PNG_A))
        val session = (fixture.coordinator.start("test") as RecordingOperationResult.Success).value
        val tap = fixture.coordinator.recordTap(10, 20) as RecordingOperationResult.Success
        val record = fixture.store.load(session.sessionId)!!

        assertEquals(RecordingInteractionStatus.UNCHANGED, tap.value.status)
        assertEquals(tap.value.beforeSceneId, tap.value.afterSceneId)
        assertEquals(1, record.scenes.size)
        assertEquals(2, record.frames.size)
        assertEquals(1, record.assets.size)
        assertEquals(1, fixture.assetRoot.walkTopDown().count { it.isFile })
    }

    @Test
    fun changedAfterCaptureCreatesSceneAndCausalLink() = runBlocking {
        val fixture = fixture(capture(PNG_A), capture(PNG_B))
        val session = (fixture.coordinator.start("test") as RecordingOperationResult.Success).value
        val tap = fixture.coordinator.recordTap(10, 20) as RecordingOperationResult.Success
        val record = fixture.store.load(session.sessionId)!!

        assertEquals(RecordingInteractionStatus.CHANGED, tap.value.status)
        assertNotEquals(tap.value.beforeSceneId, tap.value.afterSceneId)
        assertEquals(2, record.scenes.size)
        assertEquals(2, record.frames.size)
        assertEquals(2, record.assets.size)
        assertEquals(RecordingSceneStatus.CLOSED, record.scenes.first().status)
        assertEquals(RecordingSceneStatus.OPEN, record.scenes.last().status)
    }

    @Test
    fun recoveryMarksInterruptedSessionPartialWithoutDroppingData() = runBlocking {
        val fixture = fixture(capture(PNG_A))
        val session = (fixture.coordinator.start("test") as RecordingOperationResult.Success).value
        val recovered = fixture.coordinator.recoverInterruptedSessions() as RecordingOperationResult.Success
        val record = fixture.store.load(session.sessionId)!!

        assertEquals(listOf(session.sessionId), recovered.value)
        assertEquals(RecordingSessionStatus.PARTIAL, record.session.status)
        assertEquals("PROCESS_INTERRUPTED", record.session.failure)
        assertEquals(1, record.scenes.size)
        assertEquals(1, record.frames.size)
    }

    @Test
    fun metadataFailureDoesNotReportSuccessOrLeaveAsset() = runBlocking {
        val fixture = fixture(capture(PNG_A))
        fixture.store.failInitialPersist = true
        val result = fixture.coordinator.start("test")

        assertTrue(result is RecordingOperationResult.Failure)
        assertEquals(0, fixture.assetRoot.walkTopDown().count { it.isFile })
        assertEquals(RecordingSessionStatus.FAILED, fixture.store.sessions.values.single().status)
        assertNull(fixture.store.sessions.values.single().initialSceneId)
    }

    @Test
    fun epochAndMonotonicTimeRemainSeparateAndOrdered() = runBlocking {
        val clock = FakeClock(epoch = 1_700_000_000_000L, elapsed = 9_000L)
        val fixture = fixture(capture(PNG_A), capture(PNG_A), clock = clock)
        val session = (fixture.coordinator.start("test") as RecordingOperationResult.Success).value
        clock.epoch = 1_700_000_000_250L
        clock.elapsed = 9_250L
        fixture.coordinator.recordTap(1, 2)
        val record = fixture.store.load(session.sessionId)!!

        assertEquals(1_700_000_000_000L, record.session.startedAtEpochMs)
        assertEquals(9_000L, record.session.startedAtElapsedRealtimeNanos)
        assertEquals(1_700_000_000_250L, record.rawEvents.single().occurredAtEpochMs)
        assertEquals(9_250L, record.rawEvents.single().occurredAtElapsedRealtimeNanos)
        assertNotEquals(record.rawEvents.single().occurredAtEpochMs, record.rawEvents.single().occurredAtElapsedRealtimeNanos)
    }

    @Test
    fun stopFinalizesSession() = runBlocking {
        val fixture = fixture(capture(PNG_A))
        val started = (fixture.coordinator.start("test") as RecordingOperationResult.Success).value
        val stopped = fixture.coordinator.stop() as RecordingOperationResult.Success
        assertEquals(started.sessionId, stopped.value.sessionId)
        assertEquals(RecordingSessionStatus.COMPLETED, stopped.value.status)
        assertNotNull(stopped.value.stoppedAtEpochMs)
        assertEquals(
            RecordingSceneStatus.CLOSED,
            fixture.store.load(started.sessionId)!!.scenes.single().status,
        )
    }

    private fun fixture(
        vararg captures: RecorderCapture,
        clock: FakeClock = FakeClock(),
    ): Fixture {
        val root = Files.createTempDirectory("recorder-coordinator").toFile()
        val store = FakeRecorderSessionStore()
        var id = 0
        return Fixture(
            coordinator = RecordingSessionCoordinator(
                store = store,
                assetStore = ScreenshotAssetStore(root) { clock.epochMillis() },
                captureSource = QueueCaptureSource(ArrayDeque(captures.toList())),
                clock = clock,
                idGenerator = RecorderIdGenerator { prefix -> "$prefix-${++id}" },
            ),
            store = store,
            assetRoot = root,
        )
    }

    private fun capture(bytes: ByteArray, activity: String = "MainActivity") = RecorderCapture(
        pngBytes = bytes,
        widthPx = 1080,
        heightPx = 2340,
        rotation = 0,
        densityDpi = 480,
        windowContext = RecordingWindowContext("com.example", activity, "window-1"),
        a11yRoot = A11yNodeSnapshot(
            stableSnapshotNodeId = "node-0",
            parentNodeId = null,
            childOrder = 0,
            className = "android.widget.FrameLayout",
            viewIdResourceName = null,
            text = null,
            contentDescription = null,
            left = 0, top = 0, right = 1080, bottom = 2340,
            clickable = false, longClickable = false, scrollable = false, editable = false,
            enabled = true, selected = false, checked = false, visibleToUser = true,
            actions = emptyList(),
        ),
    )
}

private data class Fixture(
    val coordinator: RecordingSessionCoordinator,
    val store: FakeRecorderSessionStore,
    val assetRoot: java.io.File,
)

private class QueueCaptureSource(private val captures: ArrayDeque<RecorderCapture>) : RecorderCaptureSource {
    override suspend fun capture(): RecorderCapture = captures.removeFirst()
}

private class FakeClock(var epoch: Long = 1000L, var elapsed: Long = 100L) : RecorderClock {
    override fun epochMillis(): Long = epoch
    override fun elapsedRealtimeNanos(): Long = elapsed
}

private class FakeRecorderSessionStore : RecorderSessionStore {
    val sessions = linkedMapOf<String, RecordingSession>()
    private val scenes = linkedMapOf<String, MutableList<RecordingScene>>()
    private val frames = linkedMapOf<String, MutableList<CaptureFrame>>()
    private val assets = linkedMapOf<String, ScreenshotAsset>()
    private val snapshots = linkedMapOf<String, MutableList<A11ySnapshot>>()
    private val rawEvents = linkedMapOf<String, MutableList<RawRecordingEvent>>()
    private val interactions = linkedMapOf<String, MutableList<TapInteraction>>()
    var failInitialPersist = false

    override suspend fun createSession(session: RecordingSession) { sessions[session.sessionId] = session }
    override suspend fun persistInitialState(session: RecordingSession, scene: RecordingScene, asset: ScreenshotAsset, frame: CaptureFrame, snapshot: A11ySnapshot) {
        if (failInitialPersist) error("metadata failure")
        sessions[session.sessionId] = session
        scenes.getOrPut(session.sessionId, ::mutableListOf).add(scene)
        frames.getOrPut(session.sessionId, ::mutableListOf).add(frame)
        assets[asset.assetHash] = asset
        snapshots.getOrPut(session.sessionId, ::mutableListOf).add(snapshot)
    }
    override suspend fun persistTap(rawEvent: RawRecordingEvent, interaction: TapInteraction) {
        rawEvents.getOrPut(rawEvent.sessionId, ::mutableListOf).add(rawEvent)
        interactions.getOrPut(interaction.sessionId, ::mutableListOf).add(interaction)
    }
    override suspend fun persistUnchangedCapture(session: RecordingSession, asset: ScreenshotAsset, frame: CaptureFrame, snapshot: A11ySnapshot, interaction: TapInteraction) {
        sessions[session.sessionId] = session
        assets[asset.assetHash] = asset
        frames.getOrPut(session.sessionId, ::mutableListOf).add(frame)
        snapshots.getOrPut(session.sessionId, ::mutableListOf).add(snapshot)
        replaceInteraction(interaction)
    }
    override suspend fun persistChangedCapture(session: RecordingSession, closedScene: RecordingScene, newScene: RecordingScene, asset: ScreenshotAsset, frame: CaptureFrame, snapshot: A11ySnapshot, interaction: TapInteraction) {
        sessions[session.sessionId] = session
        scenes.getValue(session.sessionId).replaceAll { if (it.sceneId == closedScene.sceneId) closedScene else it }
        scenes.getValue(session.sessionId).add(newScene)
        assets[asset.assetHash] = asset
        frames.getOrPut(session.sessionId, ::mutableListOf).add(frame)
        snapshots.getOrPut(session.sessionId, ::mutableListOf).add(snapshot)
        replaceInteraction(interaction)
    }
    override suspend fun persistFailedInteraction(session: RecordingSession, interaction: TapInteraction) {
        sessions[session.sessionId] = session
        replaceInteraction(interaction)
    }
    override suspend fun updateSession(session: RecordingSession) { sessions[session.sessionId] = session }
    override suspend fun finalizeSession(session: RecordingSession, closedScene: RecordingScene) {
        sessions[session.sessionId] = session
        scenes.getValue(session.sessionId).replaceAll { if (it.sceneId == closedScene.sceneId) closedScene else it }
    }
    override suspend fun load(sessionId: String): PersistedRecordingSession? = sessions[sessionId]?.let { session ->
        PersistedRecordingSession(
            session, scenes[sessionId].orEmpty(), frames[sessionId].orEmpty(),
            frames[sessionId].orEmpty().mapNotNull { assets[it.assetHash] }.distinctBy { it.assetHash },
            snapshots[sessionId].orEmpty(), rawEvents[sessionId].orEmpty(), interactions[sessionId].orEmpty(),
        )
    }
    override suspend fun listSessions(): List<RecordingSession> = sessions.values.sortedByDescending { it.startedAtEpochMs }
    override suspend fun maxRawSequence(sessionId: String): Long = rawEvents[sessionId].orEmpty().maxOfOrNull { it.sequence } ?: 0
    override suspend fun recoverInterrupted(failure: String): List<String> = sessions.values
        .filter { it.status in setOf(RecordingSessionStatus.PREPARING, RecordingSessionStatus.RECORDING, RecordingSessionStatus.STOPPING) }
        .map { session ->
            sessions[session.sessionId] = session.copy(status = RecordingSessionStatus.PARTIAL, failure = failure)
            session.sessionId
        }
    private fun replaceInteraction(interaction: TapInteraction) {
        interactions.getValue(interaction.sessionId).replaceAll { if (it.interactionId == interaction.interactionId) interaction else it }
    }
}
