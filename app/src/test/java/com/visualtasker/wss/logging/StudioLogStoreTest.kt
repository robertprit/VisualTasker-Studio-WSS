package com.visualtasker.wss.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudioLogStoreTest {
    @Test
    fun sourceTargetSurvivesLogStorage() {
        val store = StudioLogStore()
        val target = StudioLogSourceTarget(
            railStepId = "dry-run-4",
            railSurface = StudioLogRailSurface.RUN,
            sourceLine = 7,
            blockId = "block-4",
            flowNodeId = "block:block-4",
        )

        store.append(
            level = StudioLogLevel.DEBUG,
            source = "RAILTRACE",
            message = "Step fokussiert",
            sourceTarget = target,
        )

        assertEquals(target, store.allEntries().single().sourceTarget)
    }

    @Test
    fun emptySourceTargetIsNotStored() {
        val store = StudioLogStore()

        store.append(
            level = StudioLogLevel.INFO,
            source = "RUNTIME",
            message = "Ohne Quelle",
            sourceTarget = StudioLogSourceTarget(),
        )

        assertNull(store.allEntries().single().sourceTarget)
    }

    @Test
    fun preferredSurfaceAloneCreatesNavigableTarget() {
        val store = StudioLogStore()
        val target = StudioLogSourceTarget(preferredSurface = StudioLogSourceSurface.PLUGIN_SETTINGS)

        store.append(
            level = StudioLogLevel.ERROR,
            source = "RUNTIME",
            message = "Adapter fehlt",
            sourceTarget = target,
        )

        assertEquals(target, store.allEntries().single().sourceTarget)
    }
}
