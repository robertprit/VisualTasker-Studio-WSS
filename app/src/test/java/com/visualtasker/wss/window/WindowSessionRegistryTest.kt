package com.visualtasker.wss.window

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowSessionRegistryTest {
    private val display = DisplayContext(DisplayId(0), 1080, 2400, 3f, 0, WindowInsets(0, 90, 0, 120))

    @Test
    fun createStoresWindowSession() {
        val registry = WindowSessionRegistry()
        assertChanged(registry.create(session("one")))
        assertEquals("panel-one", registry.session(WindowSessionId("session-one"))?.panelId)
    }

    @Test
    fun identicalCreateIsIdempotent() {
        val registry = WindowSessionRegistry()
        val session = session("one")
        registry.create(session)
        assertUnchanged(registry.create(session))
    }

    @Test
    fun conflictingCreateIsDiagnosed() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        val result = registry.create(session("one").copy(panelId = "different"))
        assertFailure(result, WindowDiagnosticCode.DUPLICATE_SESSION)
    }

    @Test
    fun attachAndDetachUseRegisteredHost() {
        val registry = WindowSessionRegistry()
        val host = FakeHost()
        registry.registerHost(host)
        registry.create(session("one"))
        assertChanged(registry.attach(WindowSessionId("session-one"), host.id))
        assertEquals(host.id, registry.session(WindowSessionId("session-one"))?.attachedHostId)
        assertChanged(registry.detach(WindowSessionId("session-one")))
        assertNull(registry.session(WindowSessionId("session-one"))?.attachedHostId)
    }

    @Test
    fun duplicateAttachIsIdempotent() {
        val registry = attachedRegistry()
        assertUnchanged(registry.attach(WindowSessionId("session-one"), WindowHostId("studio")))
    }

    @Test
    fun attachToSecondHostRequiresDetach() {
        val registry = attachedRegistry()
        registry.registerHost(FakeHost(id = WindowHostId("other")))
        assertFailure(
            registry.attach(WindowSessionId("session-one"), WindowHostId("other")),
            WindowDiagnosticCode.ALREADY_ATTACHED_TO_OTHER_HOST,
        )
    }

    @Test
    fun detachIsIdempotent() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        assertUnchanged(registry.detach(WindowSessionId("session-one")))
    }

    @Test
    fun showAndHidePreserveIdentity() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        assertChanged(registry.hide(WindowSessionId("session-one")))
        assertEquals(WindowVisibility.HIDDEN, registry.session(WindowSessionId("session-one"))?.visibility)
        assertChanged(registry.show(WindowSessionId("session-one")))
        assertEquals(WindowSessionId("session-one"), registry.snapshot().single().id)
    }

    @Test
    fun moveClampsDisplayPixelBoundsToInsets() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        registry.move(WindowSessionId("session-one"), -50f, 2390f)
        val bounds = registry.snapshot().single().placement.bounds
        assertEquals(0f, bounds.left)
        assertEquals(1880f, bounds.top)
    }

    @Test
    fun resizePreservesPositiveDisplayBounds() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        registry.resize(WindowSessionId("session-one"), 500f, 700f)
        assertEquals(500f, registry.snapshot().single().placement.bounds.width)
        assertEquals(700f, registry.snapshot().single().placement.bounds.height)
    }

    @Test
    fun invalidResizeIsDiagnosed() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        assertFailure(registry.resize(WindowSessionId("session-one"), 0f, 20f), WindowDiagnosticCode.INVALID_BOUNDS)
    }

    @Test
    fun bringToFrontCreatesDeterministicZOrder() {
        val registry = WindowSessionRegistry()
        registry.create(session("one", z = 1))
        registry.create(session("two", z = 2))
        registry.bringToFront(WindowSessionId("session-one"))
        assertEquals(listOf("session-two", "session-one"), registry.snapshot().map { it.id.value })
        assertUnchanged(registry.bringToFront(WindowSessionId("session-one")))
    }

    @Test
    fun dockAndUndockAreLossless() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        registry.dock(WindowSessionId("session-one"), WindowDocking(WindowDockPosition.RIGHT))
        assertEquals(WindowDockPosition.RIGHT, registry.snapshot().single().placement.docking.position)
        registry.undock(WindowSessionId("session-one"))
        assertEquals(WindowDockPosition.FLOATING, registry.snapshot().single().placement.docking.position)
    }

    @Test
    fun floatingDockRequestIsDiagnosed() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        assertFailure(
            registry.dock(WindowSessionId("session-one"), WindowDocking(WindowDockPosition.FLOATING)),
            WindowDiagnosticCode.INVALID_DOCKING,
        )
    }

    @Test
    fun focusIsExclusiveAcrossPanels() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        registry.create(session("two"))
        registry.focus(WindowSessionId("session-one"), WindowFocusState.KEYBOARD)
        registry.focus(WindowSessionId("session-two"), WindowFocusState.TOUCH)
        assertEquals(WindowFocusState.INACTIVE, registry.session(WindowSessionId("session-one"))?.focusState)
        assertEquals(WindowFocusState.TOUCH, registry.session(WindowSessionId("session-two"))?.focusState)
    }

    @Test
    fun touchPassThroughIsExplicit() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        registry.setInteractionMode(WindowSessionId("session-one"), WindowInteractionMode.PASS_THROUGH)
        assertEquals(WindowInteractionMode.PASS_THROUGH, registry.snapshot().single().interactionMode)
    }

    @Test
    fun keyboardFocusIsClearedWhenHidden() {
        val registry = WindowSessionRegistry()
        registry.create(session("one"))
        registry.focus(WindowSessionId("session-one"), WindowFocusState.KEYBOARD)
        registry.hide(WindowSessionId("session-one"))
        assertEquals(WindowFocusState.INACTIVE, registry.snapshot().single().focusState)
    }

    @Test
    fun persistenceReloadDropsAndroidAttachmentAndTransientGestureMode() {
        val persistence = MemoryPersistence()
        val first = WindowSessionRegistry(persistence)
        first.registerHost(FakeHost())
        first.create(session("one"))
        first.attach(WindowSessionId("session-one"), WindowHostId("studio"))
        first.setInteractionMode(WindowSessionId("session-one"), WindowInteractionMode.DRAGGING)

        val restored = WindowSessionRegistry(persistence).snapshot().single()
        assertNull(restored.attachedHostId)
        assertEquals(WindowInteractionMode.INTERACTIVE, restored.interactionMode)
        assertEquals(DisplayId(0), restored.displayContext.displayId)
    }

    @Test
    fun unavailableDisplayDoesNotFallback() {
        val registry = WindowSessionRegistry()
        registry.registerHost(FakeHost(displays = setOf(DisplayId(0))))
        registry.create(session("one").copy(displayContext = display.copy(displayId = DisplayId(4))))
        assertFailure(
            registry.attach(WindowSessionId("session-one"), WindowHostId("studio")),
            WindowDiagnosticCode.DISPLAY_UNAVAILABLE,
        )
        assertEquals(DisplayId(4), registry.snapshot().single().displayContext.displayId)
    }

    @Test
    fun hostTypeMismatchIsDiagnosed() {
        val registry = WindowSessionRegistry()
        registry.registerHost(FakeHost(type = WindowHostType.OVERLAY))
        registry.create(session("one"))
        assertFailure(
            registry.attach(WindowSessionId("session-one"), WindowHostId("studio")),
            WindowDiagnosticCode.HOST_TYPE_MISMATCH,
        )
    }

    @Test
    fun hostAttachFailureLeavesSessionDetached() {
        val registry = WindowSessionRegistry()
        registry.registerHost(FakeHost(attachResult = false))
        registry.create(session("one"))
        assertFailure(
            registry.attach(WindowSessionId("session-one"), WindowHostId("studio")),
            WindowDiagnosticCode.HOST_ATTACH_FAILED,
        )
        assertNull(registry.snapshot().single().attachedHostId)
    }

    @Test
    fun hostDetachFailureLeavesSessionAttached() {
        val registry = WindowSessionRegistry()
        val host = FakeHost(detachResult = false)
        registry.registerHost(host)
        registry.create(session("one"))
        registry.attach(WindowSessionId("session-one"), host.id)
        assertFailure(registry.detach(WindowSessionId("session-one")), WindowDiagnosticCode.HOST_DETACH_FAILED)
        assertEquals(host.id, registry.snapshot().single().attachedHostId)
    }

    @Test
    fun activityRecreationReattachesWithoutDuplicateSession() {
        val registry = WindowSessionRegistry()
        val oldHost = FakeHost()
        registry.registerHost(oldHost)
        registry.create(session("one"))
        registry.attach(WindowSessionId("session-one"), oldHost.id)
        val replacement = FakeHost()
        registry.registerHost(replacement)
        registry.unregisterHost(oldHost)
        assertEquals(1, registry.snapshot().size)
        assertEquals(1, replacement.attachCount)
        assertEquals(WindowHostId("studio"), registry.snapshot().single().attachedHostId)
    }

    @Test
    fun closeCleansRegistryAndPersistence() {
        val persistence = MemoryPersistence()
        val registry = WindowSessionRegistry(persistence)
        registry.create(session("one"))
        registry.close(WindowSessionId("session-one"))
        assertTrue(registry.snapshot().isEmpty())
        assertTrue(persistence.saved.isEmpty())
    }

    @Test
    fun multiplePanelsHaveStableDeterministicOrdering() {
        val registry = WindowSessionRegistry()
        registry.create(session("b", z = 1))
        registry.create(session("a", z = 1))
        registry.create(session("c", z = 0))
        assertEquals(listOf("session-c", "session-a", "session-b"), registry.snapshot().map { it.id.value })
    }

    @Test
    fun runtimeAndWorkflowStateRemainOutsideWindowOperations() {
        val registry = WindowSessionRegistry()
        val workflowRevision = 42L
        val runtimeSessionId = "runtime-7"
        registry.create(session("one"))
        registry.move(WindowSessionId("session-one"), 200f, 300f)
        registry.hide(WindowSessionId("session-one"))
        registry.close(WindowSessionId("session-one"))
        assertEquals(42L, workflowRevision)
        assertEquals("runtime-7", runtimeSessionId)
    }

    @Test
    fun composeDpAndLocalPixelConversionUseDensity() {
        val px = display.composeDpToLocalPx(WindowPoint(10f, 20f, WindowCoordinateSpace.COMPOSE_DP))
        assertEquals(WindowPoint(30f, 60f, WindowCoordinateSpace.LOCAL_WINDOW_PX), px)
        assertEquals(WindowPoint(10f, 20f, WindowCoordinateSpace.COMPOSE_DP), display.localPxToComposeDp(px))
    }

    @Test
    fun localAndDisplayCoordinatesRoundtripWithWindowOrigin() {
        val local = WindowPoint(15f, 25f, WindowCoordinateSpace.LOCAL_WINDOW_PX)
        val origin = WindowPoint(100f, 200f, WindowCoordinateSpace.DISPLAY_PX)
        val absolute = display.localToDisplay(local, origin)
        assertEquals(WindowPoint(115f, 225f, WindowCoordinateSpace.DISPLAY_PX), absolute)
        assertEquals(local, display.displayToLocal(absolute, origin))
    }

    @Test
    fun displayAndScreenshotCoordinatesAreExplicitlyScaled() {
        val screenshot = display.displayToScreenshot(
            WindowPoint(540f, 1200f, WindowCoordinateSpace.DISPLAY_PX),
            screenshotWidthPx = 360,
            screenshotHeightPx = 800,
        )
        assertEquals(WindowPoint(180f, 400f, WindowCoordinateSpace.SCREENSHOT_PX), screenshot)
    }

    @Test
    fun rotationIsPartOfDisplayIdentityContract() {
        val rotated = display.copy(physicalWidthPx = 2400, physicalHeightPx = 1080, rotation = 90)
        assertEquals(90, rotated.rotation)
        assertNotEquals(display.physicalWidthPx, rotated.physicalWidthPx)
    }

    @Test
    fun insetsReduceAvailableDisplayBounds() {
        val available = display.availableBounds
        assertEquals(90f, available.top)
        assertEquals(2190f, available.height)
    }

    @Test
    fun studioHostUsesSessionIdentitySeparateFromPanelIdentity() {
        val registry = WindowSessionRegistry()
        val host = StudioWindowHost(registry, display)
        host.reconcile(
            listOf(
                StudioPanelWindowState(
                    panelId = "panel-one",
                    bounds = WindowBounds(0f, 100f, 600f, 800f, WindowCoordinateSpace.LOCAL_WINDOW_PX),
                    zIndex = 1,
                    visible = true,
                    focused = true,
                )
            )
        )
        val session = registry.snapshot().single()
        assertNotEquals(session.panelId, session.id.value)
        assertEquals(host.id, session.attachedHostId)
    }

    @Test
    fun overlayServiceRestartReattachesExistingLogicalSession() {
        val registry = WindowSessionRegistry()
        val first = OverlayWindowHost(registry, display)
        val id = first.attached("toolbar", WindowBounds(10f, 100f, 200f, 80f, WindowCoordinateSpace.DISPLAY_PX), false)
        first.detached("toolbar", hidden = false)
        first.dispose()
        val second = OverlayWindowHost(registry, display)
        val restoredId = second.attached("toolbar", WindowBounds(20f, 110f, 200f, 80f, WindowCoordinateSpace.DISPLAY_PX), false)
        assertEquals(id, restoredId)
        assertEquals(1, registry.snapshot().size)
        assertEquals(second.id, registry.snapshot().single().attachedHostId)
    }

    private fun attachedRegistry(): WindowSessionRegistry = WindowSessionRegistry().also { registry ->
        registry.registerHost(FakeHost())
        registry.create(session("one"))
        registry.attach(WindowSessionId("session-one"), WindowHostId("studio"))
    }

    private fun session(name: String, z: Int = 0): WindowSession = WindowSession(
        id = WindowSessionId("session-$name"),
        panelId = "panel-$name",
        hostType = WindowHostType.STUDIO,
        displayContext = display,
        placement = WindowPlacement(
            WindowBounds(100f, 100f, 400f, 400f, WindowCoordinateSpace.DISPLAY_PX),
            zIndex = z,
        ),
    )

    private fun assertChanged(result: WindowOperationResult) {
        assertTrue(result is WindowOperationResult.Success && result.changed)
    }

    private fun assertUnchanged(result: WindowOperationResult) {
        assertTrue(result is WindowOperationResult.Success && !result.changed)
    }

    private fun assertFailure(result: WindowOperationResult, code: WindowDiagnosticCode) {
        assertTrue(result is WindowOperationResult.Failure)
        assertEquals(code, (result as WindowOperationResult.Failure).diagnostic.code)
    }

    private class MemoryPersistence(initial: List<WindowSession> = emptyList()) : WindowSessionPersistence {
        var saved: List<WindowSession> = initial
        override fun load(): List<WindowSession> = saved
        override fun save(sessions: List<WindowSession>) {
            saved = sessions
        }
    }

    private class FakeHost(
        override val id: WindowHostId = WindowHostId("studio"),
        override val type: WindowHostType = WindowHostType.STUDIO,
        private val displays: Set<DisplayId> = setOf(DisplayId(0)),
        private val attachResult: Boolean = true,
        private val detachResult: Boolean = true,
    ) : WindowHost {
        var attachCount = 0
        var detachCount = 0

        override fun supports(displayId: DisplayId): Boolean = displayId in displays

        override fun onAttach(session: WindowSession): Boolean {
            attachCount += 1
            return attachResult
        }

        override fun onDetach(session: WindowSession): Boolean {
            detachCount += 1
            return detachResult
        }
    }
}
