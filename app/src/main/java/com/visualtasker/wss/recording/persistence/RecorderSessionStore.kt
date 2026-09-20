package com.visualtasker.wss.recording.persistence

import androidx.room.withTransaction
import com.visualtasker.wss.recording.A11ySnapshot
import com.visualtasker.wss.recording.CaptureFrame
import com.visualtasker.wss.recording.PersistedRecordingSession
import com.visualtasker.wss.recording.RawRecordingEvent
import com.visualtasker.wss.recording.RecordingScene
import com.visualtasker.wss.recording.RecordingInteraction
import com.visualtasker.wss.recording.RecordingSession
import com.visualtasker.wss.recording.RecordingSessionStatus
import com.visualtasker.wss.recording.ScreenshotAsset
import com.visualtasker.wss.recording.canonicalOrder

interface RecorderSessionStore {
    suspend fun createSession(session: RecordingSession)
    suspend fun persistInitialState(
        session: RecordingSession,
        scene: RecordingScene,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
    )
    suspend fun persistInteraction(rawEvents: List<RawRecordingEvent>, interaction: RecordingInteraction)
    suspend fun persistCanonicalImport(rawEvents: List<RawRecordingEvent>, interactions: List<RecordingInteraction>) {
        val eventsById = rawEvents.associateBy(RawRecordingEvent::rawEventId)
        interactions.forEach { interaction ->
            val sources = interaction.rawEventIds.map { rawId ->
                requireNotNull(eventsById[rawId]) { "Interaction ${interaction.interactionId} references missing raw event $rawId." }
            }
            persistInteraction(sources, interaction)
        }
    }
    suspend fun persistUnchangedCapture(
        session: RecordingSession,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
        interaction: RecordingInteraction,
    )
    suspend fun persistChangedCapture(
        session: RecordingSession,
        closedScene: RecordingScene,
        newScene: RecordingScene,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
        interaction: RecordingInteraction,
    )
    suspend fun persistFailedInteraction(session: RecordingSession, interaction: RecordingInteraction)
    suspend fun updateSession(session: RecordingSession)
    suspend fun finalizeSession(session: RecordingSession, closedScene: RecordingScene)
    suspend fun load(sessionId: String): PersistedRecordingSession?
    suspend fun listSessions(): List<RecordingSession>
    suspend fun maxRawSequence(sessionId: String): Long
    suspend fun recoverInterrupted(failure: String): List<String>
}

class RoomRecorderSessionStore(
    private val database: RecorderDatabase,
) : RecorderSessionStore {
    private val dao get() = database.recorderDao()

    override suspend fun createSession(session: RecordingSession) = dao.insertSession(session.toEntity())

    override suspend fun persistInitialState(
        session: RecordingSession,
        scene: RecordingScene,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
    ) = database.withTransaction {
        dao.insertAsset(asset.toEntity())
        dao.insertScene(scene.toEntity())
        dao.insertFrame(frame.toEntity())
        dao.insertSnapshot(snapshot.toEntity())
        dao.updateSession(session.toEntity())
    }

    override suspend fun persistInteraction(rawEvents: List<RawRecordingEvent>, interaction: RecordingInteraction) =
        database.withTransaction {
            validateImport(listOf(interaction), rawEvents)
            rawEvents.forEach { dao.insertRawEvent(it.toEntity()) }
            dao.insertInteraction(interaction.toEntity())
        }

    override suspend fun persistCanonicalImport(
        rawEvents: List<RawRecordingEvent>,
        interactions: List<RecordingInteraction>,
    ) = database.withTransaction {
        validateImport(interactions, rawEvents)
        rawEvents.forEach { dao.insertRawEvent(it.toEntity()) }
        interactions.forEach { dao.insertInteraction(it.toEntity()) }
    }

    override suspend fun persistUnchangedCapture(
        session: RecordingSession,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
        interaction: RecordingInteraction,
    ) = database.withTransaction {
        dao.insertAsset(asset.toEntity())
        dao.insertFrame(frame.toEntity())
        dao.insertSnapshot(snapshot.toEntity())
        dao.updateInteraction(interaction.toEntity())
        dao.updateSession(session.toEntity())
    }

    override suspend fun persistChangedCapture(
        session: RecordingSession,
        closedScene: RecordingScene,
        newScene: RecordingScene,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
        interaction: RecordingInteraction,
    ) = database.withTransaction {
        dao.insertAsset(asset.toEntity())
        dao.updateScene(closedScene.toEntity())
        dao.insertScene(newScene.toEntity())
        dao.insertFrame(frame.toEntity())
        dao.insertSnapshot(snapshot.toEntity())
        dao.updateInteraction(interaction.toEntity())
        dao.updateSession(session.toEntity())
    }

    override suspend fun persistFailedInteraction(session: RecordingSession, interaction: RecordingInteraction) =
        database.withTransaction {
            dao.updateInteraction(interaction.toEntity())
            dao.updateSession(session.toEntity())
        }

    override suspend fun updateSession(session: RecordingSession) = dao.updateSession(session.toEntity())

    override suspend fun finalizeSession(session: RecordingSession, closedScene: RecordingScene) =
        database.withTransaction {
            dao.updateScene(closedScene.toEntity())
            dao.updateSession(session.toEntity())
        }

    override suspend fun load(sessionId: String): PersistedRecordingSession? {
        val session = dao.session(sessionId)?.toDomain() ?: return null
        return PersistedRecordingSession(
            session = session,
            scenes = dao.scenes(sessionId).map { it.toDomain() },
            frames = dao.frames(sessionId).map { it.toDomain() },
            assets = dao.assets(sessionId).map { it.toDomain() },
            snapshots = dao.snapshots(sessionId).map { it.toDomain() },
            rawEvents = dao.rawEvents(sessionId).map { it.toDomain() },
            interactions = dao.interactions(sessionId).map { it.toDomain() },
        ).canonicalOrder()
    }

    override suspend fun listSessions(): List<RecordingSession> = dao.sessions().map { it.toDomain() }

    override suspend fun maxRawSequence(sessionId: String): Long = dao.maxRawSequence(sessionId)

    override suspend fun recoverInterrupted(failure: String): List<String> = database.withTransaction {
        dao.interruptedSessions().map { entity ->
            dao.updateSession(
                entity.copy(
                    status = RecordingSessionStatus.INTERRUPTED.name,
                    failure = failure,
                )
            )
            entity.sessionId
        }
    }
}

private fun validateImport(interactions: List<RecordingInteraction>, rawEvents: List<RawRecordingEvent>) {
    val eventIds = rawEvents.mapTo(mutableSetOf(), RawRecordingEvent::rawEventId)
    val sessionIds = (rawEvents.map(RawRecordingEvent::sessionId) + interactions.map(RecordingInteraction::sessionId)).toSet()
    require(sessionIds.size <= 1) { "Canonical import must belong to exactly one recording session." }
    interactions.forEach { interaction ->
        require(interaction.rawEventIds.all(eventIds::contains)) {
            "Interaction ${interaction.interactionId} has incomplete raw provenance."
        }
    }
}
