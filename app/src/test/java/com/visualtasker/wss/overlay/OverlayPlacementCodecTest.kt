package com.visualtasker.wss.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayPlacementCodecTest {
    @Test
    fun roundtripPreservesPlacementAndVisibility() {
        val placement = OverlayPlacement(x = 12, y = 34, width = 560, height = 240, visible = true)

        assertEquals(placement, OverlayPlacementCodec.decode(OverlayPlacementCodec.encode(placement)))
    }

    @Test
    fun malformedStateIsIgnored() {
        assertNull(OverlayPlacementCodec.decode("12,broken,560"))
    }
}
