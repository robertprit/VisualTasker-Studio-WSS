package com.visualtasker.wss.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioLogStoreTest {
    @Test
    fun append_addsEntry() {
        val store = StudioLogStore(maxEntries = 10)
        store.append(level = StudioLogLevel.INFO, source = "BLOCKEDITOR", message = "Ready")
        val entries = store.allEntries()
        assertEquals(1, entries.size)
        assertEquals("BLOCKEDITOR", entries.first().source)
    }

    @Test
    fun maxSize_evictsOldestEntries() {
        val store = StudioLogStore(maxEntries = 2)
        store.append(StudioLogLevel.INFO, "A", "one")
        store.append(StudioLogLevel.INFO, "B", "two")
        store.append(StudioLogLevel.INFO, "C", "three")
        val entries = store.allEntries()
        assertEquals(2, entries.size)
        assertEquals("two", entries.first().message)
        assertEquals("three", entries.last().message)
    }

    @Test
    fun filter_byLevelSourceAndQuery() {
        val store = StudioLogStore(maxEntries = 10)
        store.append(StudioLogLevel.INFO, "BLOCKEDITOR", "Dokument valide")
        store.append(StudioLogLevel.ERROR, "EMSCRIPT", "Projektion fehlgeschlagen", details = "line 3 col 11")
        val filtered = store.visibleEntries(
            StudioLogFilters(
                levels = setOf(StudioLogLevel.ERROR),
                sources = setOf("EMSCRIPT"),
                query = "line 3",
            ),
        )
        assertEquals(1, filtered.size)
        assertEquals("EMSCRIPT", filtered.first().source)
    }

    @Test
    fun pause_buffersAndResumeFlushes() {
        val store = StudioLogStore(maxEntries = 10)
        store.setEmissionPaused(true)
        store.append(StudioLogLevel.INFO, "FLOWCHART", "RUNNING")
        assertTrue(store.allEntries().isEmpty())
        store.setEmissionPaused(false)
        assertEquals(1, store.allEntries().size)
    }

    @Test
    fun clearVisible_removesOnlyMatchingEntries() {
        val store = StudioLogStore(maxEntries = 10)
        store.append(StudioLogLevel.INFO, "BLOCKEDITOR", "ok")
        store.append(StudioLogLevel.ERROR, "EMSCRIPT", "bad")
        store.clearVisible(
            StudioLogFilters(
                levels = setOf(StudioLogLevel.ERROR),
                sources = setOf("EMSCRIPT"),
            ),
        )
        val entries = store.allEntries()
        assertEquals(1, entries.size)
        assertEquals("BLOCKEDITOR", entries.first().source)
    }

    @Test
    fun repeatedIdenticalEntries_areGrouped() {
        val store = StudioLogStore(maxEntries = 10)
        store.append(StudioLogLevel.DEBUG, "FLOWCHART", "Graph and view attached")
        store.append(StudioLogLevel.DEBUG, "FLOWCHART", "Graph and view attached")
        val entries = store.allEntries()
        assertEquals(1, entries.size)
        assertEquals(2, entries.first().repeatCount)
    }

    @Test
    fun differentDiagnostics_areNotMerged() {
        val store = StudioLogStore(maxEntries = 10)
        store.append(
            StudioLogLevel.WARNING,
            "FLOWCHART",
            "Unbekannter Blocktyp",
            details = "vision.ocr at block-a",
            groupKey = "diag:block-a",
        )
        store.append(
            StudioLogLevel.WARNING,
            "FLOWCHART",
            "Unbekannter Blocktyp",
            details = "vision.ocr at block-b",
            groupKey = "diag:block-b",
        )
        assertEquals(2, store.allEntries().size)
    }
}
