package com.visualtasker.wss.recording.persistence

import androidx.room.withTransaction
import com.visualtasker.wss.recording.A11ySnapshot
import com.visualtasker.wss.recording.CaptureFrame
import com.visualtasker.wss.recording.PersistedRecordingSession
import com.visualtasker.wss.recording.RawRecordingEvent
import com.visualtasker.wss.recording.RecordingScene
import com.visualtasker.wss.recording.RecordingSession
import com.visualtasker.wss.recording.RecordingSessionStatus
import com.visualtasker.wss.recording.ScreenshotAsset
import com.visualtasker.wss.recording.TapInteraction

interface RecorderSessionStore {
    suspend fun createSession(session: RecordingSession)
    suspend fun persistInitialState(
        session: RecordingSession,
        scene: RecordingScene,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
    )
    suspend fun persistTap(rawEvent: RawRecordingEvent, interaction: TapInteraction)
    suspend fun persistUnchangedCapture(
        session: RecordingSession,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
        interaction: TapInteraction,
    )
    suspend fun persistChangedCapture(
        session: RecordingSession,
        closedScene: RecordingScene,
        newScene: RecordingScene,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
        interaction: TapInteraction,
    )
    suspend fun persistFailedInteraction(session: RecordingSession, interaction: TapInteraction)
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

    override suspend fun persistTap(rawEvent: RawRecordingEvent, interaction: TapInteraction) =
        database.withTransaction {
            dao.insertRawEvent(rawEvent.toEntity())
            dao.insertInteraction(interaction.toEntity())
        }

    override suspend fun persistUnchangedCapture(
        session: RecordingSession,
        asset: ScreenshotAsset,
        frame: CaptureFrame,
        snapshot: A11ySnapshot,
        interaction: TapInteraction,
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
        interaction: TapInteraction,
    ) = database.withTransaction {
        dao.insertAsset(asset.toEntity())
        dao.updateScene(closedScene.toEntity())
        dao.insertScene(newScene.toEntity())
        dao.insertFrame(frame.toEntity())
        dao.insertSnapshot(snapshot.toEntity())
        dao.updateInteraction(interaction.toEntity())
        dao.updateSession(session.toEntity())
    }

    override suspend fun persistFailedInteraction(session: RecordingSession, interaction: TapInteraction) =
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
        )
    }

    override suspend fun listSessions(): List<RecordingSession> = dao.sessions().map { it.toDomain() }

    override suspend fun maxRawSequence(sessionId: String): Long = dao.maxRawSequence(sessionId)

    override suspend fun recoverInterrupted(failure: String): List<String> = database.withTransaction {
        dao.interruptedSessions().map { entity ->
            dao.updateSession(
                entity.copy(
                    status = RecordingSessionStatus.PARTIAL.name,
                    failure = failure,
                )
            )
            entity.sessionId
        }
    }
}
