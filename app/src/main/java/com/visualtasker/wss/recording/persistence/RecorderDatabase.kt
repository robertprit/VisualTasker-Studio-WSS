package com.visualtasker.wss.recording.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecordingSessionEntity::class,
        RecordingSceneEntity::class,
        ScreenshotAssetEntity::class,
        CaptureFrameEntity::class,
        A11ySnapshotEntity::class,
        RawRecordingEventEntity::class,
        RecordingInteractionEntity::class,
        StepReviewDecisionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class RecorderDatabase : RoomDatabase() {
    abstract fun recorderDao(): RecorderDao

    companion object {
        @Volatile private var instance: RecorderDatabase? = null

        fun get(context: Context): RecorderDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RecorderDatabase::class.java,
                "visualtasker-recording.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recording_step_review_decisions` (
                        `decisionId` TEXT NOT NULL,
                        `candidateId` TEXT NOT NULL,
                        `sourceSessionId` TEXT NOT NULL,
                        `sourceRecordVersion` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `originalProposalJson` TEXT NOT NULL,
                        `correctedProposalJson` TEXT,
                        `selectedTargetNodeId` TEXT,
                        `reasonCode` TEXT,
                        `note` TEXT,
                        `decidedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`decisionId`),
                        FOREIGN KEY(`sourceSessionId`) REFERENCES `recording_sessions`(`sessionId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recording_step_review_decisions_sourceSessionId` ON `recording_step_review_decisions` (`sourceSessionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recording_step_review_decisions_candidateId` ON `recording_step_review_decisions` (`candidateId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recording_interactions` (
                        `interactionId` TEXT NOT NULL,
                        `sessionId` TEXT NOT NULL,
                        `sequence` INTEGER NOT NULL,
                        `occurredAtEpochMs` INTEGER NOT NULL,
                        `occurredAtElapsedRealtimeNanos` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `rawEventIdsJson` TEXT NOT NULL,
                        `evidenceRefsJson` TEXT NOT NULL,
                        `beforeSceneId` TEXT,
                        `afterSceneId` TEXT,
                        `status` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        PRIMARY KEY(`interactionId`),
                        FOREIGN KEY(`sessionId`) REFERENCES `recording_sessions`(`sessionId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recording_interactions_sessionId` ON `recording_interactions` (`sessionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recording_interactions_sessionId_sequence` ON `recording_interactions` (`sessionId`, `sequence`)")
                db.execSQL(
                    """
                    INSERT INTO `recording_interactions` (
                        `interactionId`, `sessionId`, `sequence`, `occurredAtEpochMs`,
                        `occurredAtElapsedRealtimeNanos`, `type`, `source`, `rawEventIdsJson`,
                        `evidenceRefsJson`, `beforeSceneId`, `afterSceneId`, `status`, `payloadJson`
                    )
                    SELECT
                        `interactionId`, `sessionId`, `sequence`, `occurredAtEpochMs`,
                        `occurredAtElapsedRealtimeNanos`, 'TAP', 'RECORDER',
                        '[' || char(34) || replace(`rawEventId`, char(34), '\\' || char(34)) || char(34) || ']',
                        '[]', `beforeSceneId`, `afterSceneId`, `status`,
                        '{"xPx":' || `xPx` || ',"yPx":' || `yPx` ||
                        ',"durationMs":null,"pointerId":null,"button":null,"targetReference":' ||
                        CASE WHEN `targetA11yNodeId` IS NULL THEN 'null'
                             ELSE char(34) || replace(`targetA11yNodeId`, char(34), '\\' || char(34)) || char(34)
                        END || '}'
                    FROM `recording_tap_interactions`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `recording_tap_interactions`")
                db.execSQL("UPDATE `recording_sessions` SET `schemaVersion` = 2")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recording_sessions` ADD COLUMN `resumedFromSessionId` TEXT")
                db.execSQL("UPDATE `recording_sessions` SET `schemaVersion` = 3")
            }
        }
    }
}
