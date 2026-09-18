package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceResourceReducerTest {
    @Test
    fun batchUpsertIsAtomicAndPrefersNewestDuplicate() {
        val existing = resource(id = "marker:a", label = "Alt", updatedAt = 10L)
        val bundle = WorkspaceResourceBundle(revision = 4L, resources = listOf(existing))

        val updated = WorkspaceResourceReducer.upsertAll(
            bundle = bundle,
            resources = listOf(
                resource(id = "marker:a", label = "Aelterer Import", updatedAt = 5L),
                resource(id = "marker:a", label = "Neu", updatedAt = 20L),
                resource(id = "marker:b", label = "B", updatedAt = 15L),
            ),
        )

        assertEquals(5L, updated.revision)
        assertEquals(2, updated.resources.size)
        assertEquals("Neu", updated.find("marker:a")?.label)
    }

    @Test
    fun emptyBatchKeepsBundleAndRevision() {
        val bundle = WorkspaceResourceBundle(revision = 8L)

        assertEquals(bundle, WorkspaceResourceReducer.upsertAll(bundle, emptyList()))
    }

    private fun resource(id: String, label: String, updatedAt: Long): WorkspaceResource =
        WorkspaceResource(
            id = id,
            kind = WorkspaceResourceKind.Marker,
            label = label,
            createdAtEpochMs = updatedAt,
            updatedAtEpochMs = updatedAt,
        )
}
