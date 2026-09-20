package com.visualtasker.wss.workspace.plugin.flowchart

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowchartFocusPolicyTest {
    @Test
    fun visibleFocusDoesNotMoveViewport() {
        assertFalse(shouldRevealFlowchartFocus(400.0, 300.0, 800, 600, interactionActive = false))
    }

    @Test
    fun offscreenFocusIsRevealedWhenIdle() {
        assertTrue(shouldRevealFlowchartFocus(900.0, 300.0, 800, 600, interactionActive = false))
    }

    @Test
    fun dragOrConnectionSuppressesViewportJump() {
        assertFalse(shouldRevealFlowchartFocus(900.0, 300.0, 800, 600, interactionActive = true))
    }
}
