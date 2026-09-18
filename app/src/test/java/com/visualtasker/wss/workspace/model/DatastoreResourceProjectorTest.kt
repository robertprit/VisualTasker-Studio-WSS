package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatastoreResourceProjectorTest {
    @Test
    fun recordingSessionsAndRuntimeValuesBecomeDatasetResources() {
        val session = RecordingSessionUi(
            path = "/tmp/recording.jsonl",
            fileName = "recording.jsonl",
            label = "Session 1",
            lastModifiedMs = 42L,
            stepCount = 7,
            durationMs = 900L,
        )

        val projected = DatastoreResourceProjector.project(
            base = WorkspaceResourceBundle(),
            recordingSessions = listOf(session),
            runtimeValues = mapOf("counter" to "3"),
        )

        assertEquals(2, projected.byKind(WorkspaceResourceKind.Dataset).size)
        assertTrue(projected.resources.any { it.pluginOwner == "visualtasker.recorder" && it.metadata["stepCount"] == "7" })
        assertTrue(projected.resources.any { it.pluginOwner == "visualtasker.runtime" && it.metadata["value"] == "3" })
    }

    @Test
    fun repeatedProjectionReplacesGeneratedResourcesWithoutDuplicates() {
        val once = DatastoreResourceProjector.project(
            base = WorkspaceResourceBundle(),
            recordingSessions = emptyList(),
            runtimeValues = mapOf("state" to "old"),
        )
        val twice = DatastoreResourceProjector.project(
            base = once,
            recordingSessions = emptyList(),
            runtimeValues = mapOf("state" to "new"),
        )

        assertEquals(1, twice.resources.size)
        assertEquals("new", twice.resources.single().metadata["value"])
    }
}
