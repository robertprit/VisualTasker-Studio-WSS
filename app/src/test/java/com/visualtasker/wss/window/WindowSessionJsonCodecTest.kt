package com.visualtasker.wss.window

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WindowSessionJsonCodecTest {
    @Test
    fun persistenceRoundtripKeepsLayoutDisplayDockAndVisibility() {
        val original = WindowSession(
            id = WindowSessionId("window-1"),
            panelId = "flow-panel",
            hostType = WindowHostType.OVERLAY,
            displayContext = DisplayContext(DisplayId(3), 1440, 3120, 4f, 90, WindowInsets(1, 2, 3, 4)),
            placement = WindowPlacement(
                bounds = WindowBounds(11f, 22f, 333f, 444f, WindowCoordinateSpace.DISPLAY_PX),
                docking = WindowDocking(WindowDockPosition.LEFT, WindowSessionId("window-2")),
                zIndex = 7,
            ),
            visibility = WindowVisibility.HIDDEN,
            interactionMode = WindowInteractionMode.PASS_THROUGH,
        )

        val restored = WindowSessionJsonCodec.decode(WindowSessionJsonCodec.encode(listOf(original))).single()

        assertEquals(original.id, restored.id)
        assertEquals(original.panelId, restored.panelId)
        assertEquals(original.hostType, restored.hostType)
        assertEquals(original.displayContext, restored.displayContext)
        assertEquals(original.placement, restored.placement)
        assertEquals(original.visibility, restored.visibility)
        assertEquals(original.interactionMode, restored.interactionMode)
        assertNull(restored.attachedHostId)
    }

    @Test
    fun transientResizeModeIsPersistedAsInteractive() {
        val session = WindowSession(
            id = WindowSessionId("window-1"),
            panelId = "panel",
            hostType = WindowHostType.STUDIO,
            displayContext = DisplayContext(DisplayId(0), 100, 200, 1f, 0),
            placement = WindowPlacement(WindowBounds(0f, 0f, 50f, 50f, WindowCoordinateSpace.DISPLAY_PX)),
            interactionMode = WindowInteractionMode.RESIZING,
        )
        val restored = WindowSessionJsonCodec.decode(WindowSessionJsonCodec.encode(listOf(session))).single()
        assertEquals(WindowInteractionMode.INTERACTIVE, restored.interactionMode)
    }

    @Test
    fun malformedPersistenceIsIgnored() {
        assertEquals(emptyList<WindowSession>(), WindowSessionJsonCodec.decode("not-json"))
    }
}
