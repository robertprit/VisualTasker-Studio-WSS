package com.visualtasker.wss.workspace.plugin.flowchart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowchartViewportScrollbarTest {
    @Test
    fun smallTrackNeverCreatesAnEmptyThumbRange() {
        val thumb = scrollbarThumbLength(
            trackLength = 12f,
            visibleLength = 100f,
            contentLength = 800f,
            minimumThumbLength = 80f,
        )

        assertEquals(12f, thumb)
    }

    @Test
    fun regularTrackKeepsMinimumThumbVisible() {
        val thumb = scrollbarThumbLength(
            trackLength = 300f,
            visibleLength = 50f,
            contentLength = 1_000f,
            minimumThumbLength = 28f,
        )

        assertTrue(thumb in 28f..300f)
    }
}
