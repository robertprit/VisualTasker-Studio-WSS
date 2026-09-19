package com.visualtasker.wss.recording.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecorderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(entity: RecordingSessionEntity)

    @Update
    suspend fun updateSession(entity: RecordingSessionEntity)

    @Query("SELECT * FROM recording_sessions WHERE sessionId = :sessionId")
    suspend fun session(sessionId: String): RecordingSessionEntity?

    @Query("SELECT * FROM recording_sessions ORDER BY startedAtEpochMs DESC")
    suspend fun sessions(): List<RecordingSessionEntity>

    @Query("SELECT * FROM recording_sessions WHERE status IN ('PREPARING', 'RECORDING', 'STOPPING')")
    suspend fun interruptedSessions(): List<RecordingSessionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScene(entity: RecordingSceneEntity)

    @Update
    suspend fun updateScene(entity: RecordingSceneEntity)

    @Query("SELECT * FROM recording_scenes WHERE sessionId = :sessionId ORDER BY sequence")
    suspend fun scenes(sessionId: String): List<RecordingSceneEntity>

    @Query("SELECT * FROM recording_scenes WHERE sceneId = :sceneId")
    suspend fun scene(sceneId: String): RecordingSceneEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAsset(entity: ScreenshotAssetEntity): Long

    @Query("SELECT * FROM recording_screenshot_assets WHERE assetHash IN (SELECT assetHash FROM recording_capture_frames WHERE sessionId = :sessionId) ORDER BY assetHash")
    suspend fun assets(sessionId: String): List<ScreenshotAssetEntity>

    @Query("SELECT * FROM recording_screenshot_assets WHERE assetHash = :hash")
    suspend fun asset(hash: String): ScreenshotAssetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFrame(entity: CaptureFrameEntity)

    @Query("SELECT * FROM recording_capture_frames WHERE sessionId = :sessionId ORDER BY capturedAtElapsedRealtimeNanos")
    suspend fun frames(sessionId: String): List<CaptureFrameEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(entity: A11ySnapshotEntity)

    @Query("SELECT * FROM recording_a11y_snapshots WHERE sessionId = :sessionId ORDER BY capturedAtEpochMs")
    suspend fun snapshots(sessionId: String): List<A11ySnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRawEvent(entity: RawRecordingEventEntity)

    @Query("SELECT * FROM recording_raw_events WHERE sessionId = :sessionId ORDER BY sequence")
    suspend fun rawEvents(sessionId: String): List<RawRecordingEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInteraction(entity: TapInteractionEntity)

    @Update
    suspend fun updateInteraction(entity: TapInteractionEntity)

    @Query("SELECT * FROM recording_tap_interactions WHERE sessionId = :sessionId ORDER BY sequence")
    suspend fun interactions(sessionId: String): List<TapInteractionEntity>

    @Query("SELECT COALESCE(MAX(sequence), 0) FROM recording_raw_events WHERE sessionId = :sessionId")
    suspend fun maxRawSequence(sessionId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReviewDecision(entity: StepReviewDecisionEntity)

    @Query("SELECT * FROM recording_step_review_decisions WHERE sourceSessionId = :sessionId ORDER BY decidedAtEpochMs, candidateId")
    suspend fun reviewDecisions(sessionId: String): List<StepReviewDecisionEntity>

    @Query("SELECT * FROM recording_step_review_decisions WHERE candidateId = :candidateId")
    suspend fun reviewDecision(candidateId: String): StepReviewDecisionEntity?
}
