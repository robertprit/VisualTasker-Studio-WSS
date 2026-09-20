package com.visualtasker.wss.recording.persistence

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecorderDatabaseMigrationTest {
    private val databaseName = "recorder-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RecorderDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration2To3PreservesTapIdentityOrderingAndProvenance() {
        helper.createDatabase(databaseName, 2).apply {
            execSQL(
                "INSERT INTO recording_sessions VALUES ('session-1',1,'COMPLETED',1000,1000000000,2000,'test','{}','scene-1','scene-1',NULL)",
            )
            execSQL(
                "INSERT INTO recording_raw_events VALUES ('raw-1','session-1',7,1500,1500000000,'tap','{\"xPx\":\"12\",\"yPx\":\"34\"}')",
            )
            execSQL(
                "INSERT INTO recording_tap_interactions VALUES ('tap-1','raw-1','session-1',7,1500,1500000000,12,34,'scene-1','scene-1','target-1','UNCHANGED')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(databaseName, 3, true, RecorderDatabase.MIGRATION_2_3)
        migrated.query(
            "SELECT interactionId, sequence, type, rawEventIdsJson, payloadJson FROM recording_interactions",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("tap-1", cursor.getString(0))
            assertEquals(7L, cursor.getLong(1))
            assertEquals("TAP", cursor.getString(2))
            assertEquals("[\"raw-1\"]", cursor.getString(3))
            assertEquals(true, cursor.getString(4).contains("\"targetReference\":\"target-1\""))
        }
        migrated.query("SELECT schemaVersion FROM recording_sessions WHERE sessionId='session-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
        migrated.query("SELECT name FROM sqlite_master WHERE type='table' AND name='recording_tap_interactions'").use { cursor ->
            assertEquals(0, cursor.count)
        }
    }

    @Test
    fun migration3To4AddsContinuationReferenceWithoutChangingStatus() {
        helper.createDatabase(databaseName, 3).apply {
            execSQL(
                "INSERT INTO recording_sessions VALUES ('session-interrupted',2,'INTERRUPTED',1000,1000000000,NULL,'test','{}','scene-1','scene-1','PROCESS_INTERRUPTED')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(databaseName, 4, true, RecorderDatabase.MIGRATION_3_4)
        migrated.query(
            "SELECT status, schemaVersion, resumedFromSessionId FROM recording_sessions WHERE sessionId='session-interrupted'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("INTERRUPTED", cursor.getString(0))
            assertEquals(3, cursor.getInt(1))
            assertEquals(true, cursor.isNull(2))
        }
    }
}
