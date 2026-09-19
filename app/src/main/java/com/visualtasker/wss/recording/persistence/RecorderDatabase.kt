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
        TapInteractionEntity::class,
        StepReviewDecisionEntity::class,
    ],
    version = 2,
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
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
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
    }
}
