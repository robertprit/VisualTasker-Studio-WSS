package com.visualtasker.wss.recording.persistence

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recording_sessions")
data class RecordingSessionEntity(
    @PrimaryKey val sessionId: String,
    val schemaVersion: Int,
    val status: String,
    val startedAtEpochMs: Long,
    val startedAtElapsedRealtimeNanos: Long,
    val stoppedAtEpochMs: Long?,
    val createdBy: String,
    val recordingPolicyJson: String,
    val initialSceneId: String?,
    val latestSceneId: String?,
    val failure: String?,
    val resumedFromSessionId: String?,
)

@Entity(
    tableName = "recording_scenes",
    foreignKeys = [ForeignKey(
        entity = RecordingSessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index(value = ["sessionId", "sequence"], unique = true)],
)
data class RecordingSceneEntity(
    @PrimaryKey val sceneId: String,
    val sessionId: String,
    val sequence: Long,
    val openedAtEpochMs: Long,
    val openedAtElapsedRealtimeNanos: Long,
    val closedAtEpochMs: Long?,
    val primaryFrameId: String,
    val windowContextJson: String,
    val status: String,
)

@Entity(tableName = "recording_screenshot_assets")
data class ScreenshotAssetEntity(
    @PrimaryKey val assetHash: String,
    val assetReference: String,
    val byteCount: Long,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "recording_capture_frames",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecordingSceneEntity::class,
            parentColumns = ["sceneId"],
            childColumns = ["sceneId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ScreenshotAssetEntity::class,
            parentColumns = ["assetHash"],
            childColumns = ["assetHash"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sessionId"), Index("sceneId"), Index("assetHash")],
)
data class CaptureFrameEntity(
    @PrimaryKey val frameId: String,
    val sessionId: String,
    val sceneId: String,
    val capturedAtEpochMs: Long,
    val capturedAtElapsedRealtimeNanos: Long,
    val assetHash: String,
    val assetReference: String,
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Int,
    val densityDpi: Int,
    val captureStatus: String,
    val coordinateSpace: String,
)

@Entity(
    tableName = "recording_a11y_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecordingSceneEntity::class,
            parentColumns = ["sceneId"],
            childColumns = ["sceneId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CaptureFrameEntity::class,
            parentColumns = ["frameId"],
            childColumns = ["frameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("sceneId"), Index("frameId")],
)
data class A11ySnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val sessionId: String,
    val sceneId: String,
    val frameId: String,
    val capturedAtEpochMs: Long,
    val rootNodeJson: String,
    val windowId: String?,
    val packageName: String?,
    val status: String,
)

@Entity(
    tableName = "recording_raw_events",
    foreignKeys = [ForeignKey(
        entity = RecordingSessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index(value = ["sessionId", "sequence"], unique = true)],
)
data class RawRecordingEventEntity(
    @PrimaryKey val rawEventId: String,
    val sessionId: String,
    val sequence: Long,
    val occurredAtEpochMs: Long,
    val occurredAtElapsedRealtimeNanos: Long,
    val kind: String,
    val payloadJson: String,
)

@Entity(
    tableName = "recording_interactions",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index(value = ["sessionId", "sequence"], unique = true)],
)
data class RecordingInteractionEntity(
    @PrimaryKey val interactionId: String,
    val sessionId: String,
    val sequence: Long,
    val occurredAtEpochMs: Long,
    val occurredAtElapsedRealtimeNanos: Long,
    val type: String,
    val source: String,
    val rawEventIdsJson: String,
    val evidenceRefsJson: String,
    val beforeSceneId: String?,
    val afterSceneId: String?,
    val status: String,
    val payloadJson: String,
)

@Entity(
    tableName = "recording_step_review_decisions",
    foreignKeys = [ForeignKey(
        entity = RecordingSessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sourceSessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sourceSessionId"), Index(value = ["candidateId"], unique = true)],
)
data class StepReviewDecisionEntity(
    @PrimaryKey val decisionId: String,
    val candidateId: String,
    val sourceSessionId: String,
    val sourceRecordVersion: Int,
    val status: String,
    val originalProposalJson: String,
    val correctedProposalJson: String?,
    val selectedTargetNodeId: String?,
    val reasonCode: String?,
    val note: String?,
    val decidedAtEpochMs: Long,
)
