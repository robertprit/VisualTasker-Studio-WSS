package com.visualtasker.wss.window

import java.util.UUID

data class StudioPanelWindowState(
    val panelId: String,
    val bounds: WindowBounds,
    val zIndex: Int,
    val visible: Boolean,
    val focused: Boolean,
)

class StudioWindowHost(
    private val registry: WindowSessionRegistry,
    private var displayContext: DisplayContext,
) : WindowHost {
    override val id: WindowHostId = WindowHostId("studio-window-host")
    override val type: WindowHostType = WindowHostType.STUDIO

    init {
        registry.registerHost(this)
    }

    override fun supports(displayId: DisplayId): Boolean = displayContext.displayId == displayId

    fun reconcile(panels: List<StudioPanelWindowState>) {
        val activePanelIds = panels.mapTo(mutableSetOf()) { it.panelId }
        panels.sortedWith(compareBy<StudioPanelWindowState> { it.zIndex }.thenBy { it.panelId }).forEach { panel ->
            val existing = registry.find(type, panel.panelId)
            val session = existing ?: WindowSession(
                id = WindowSessionId(UUID.randomUUID().toString()),
                panelId = panel.panelId,
                hostType = type,
                displayContext = displayContext,
                placement = WindowPlacement(panel.bounds, zIndex = panel.zIndex),
                visibility = if (panel.visible) WindowVisibility.VISIBLE else WindowVisibility.HIDDEN,
                focusState = if (panel.focused) WindowFocusState.ACTIVE else WindowFocusState.INACTIVE,
                interactionMode = WindowInteractionMode.INTERACTIVE,
                surface = WindowSurface(
                    id = WindowSurfaceId("studio-surface:${panel.panelId}"),
                    panelId = panel.panelId,
                    bounds = panel.bounds.copy(left = 0f, top = 0f, space = WindowCoordinateSpace.LOCAL_WINDOW_PX),
                ),
            ).also { registry.create(it) }

            if (session.displayContext.displayId != displayContext.displayId) {
                registry.report(
                    WindowDiagnostic(
                        WindowDiagnosticCode.DISPLAY_UNAVAILABLE,
                        "Studio panel ${panel.panelId} belongs to unavailable display ${session.displayContext.displayId.value}",
                        session.id,
                        id,
                    )
                )
                return@forEach
            }
            registry.attach(session.id, id)
            registry.move(session.id, panel.bounds.left, panel.bounds.top)
            registry.resize(session.id, panel.bounds.width, panel.bounds.height)
            registry.setZIndex(session.id, panel.zIndex)
            if (panel.visible) registry.show(session.id) else registry.hide(session.id)
            if (panel.focused) registry.focus(session.id, WindowFocusState.ACTIVE)
        }

        registry.snapshot()
            .filter { it.hostType == type && it.panelId !in activePanelIds }
            .forEach { registry.close(it.id) }
    }

    fun sessionId(panelId: String): WindowSessionId? = registry.find(type, panelId)?.id

    fun move(panelId: String, leftPx: Float, topPx: Float) {
        sessionId(panelId)?.let { registry.move(it, leftPx, topPx) }
    }

    fun resize(panelId: String, widthPx: Float, heightPx: Float) {
        sessionId(panelId)?.let { registry.resize(it, widthPx, heightPx) }
    }

    fun bringToFront(panelId: String) {
        sessionId(panelId)?.let { registry.bringToFront(it) }
    }

    fun focus(panelId: String, focusState: WindowFocusState) {
        sessionId(panelId)?.let { registry.focus(it, focusState) }
    }

    fun setInteraction(panelId: String, mode: WindowInteractionMode) {
        sessionId(panelId)?.let { registry.setInteractionMode(it, mode) }
    }

    fun close(panelId: String) {
        sessionId(panelId)?.let { registry.close(it) }
    }

    fun updateDisplayContext(context: DisplayContext) {
        displayContext = context
        registry.snapshot()
            .filter { it.hostType == type && it.displayContext.displayId == context.displayId }
            .forEach { registry.updateDisplayContext(it.id, context) }
    }

    fun dispose() {
        registry.unregisterHost(this)
    }
}

class OverlayWindowHost(
    private val registry: WindowSessionRegistry,
    private var displayContext: DisplayContext,
) : WindowHost {
    override val id: WindowHostId = WindowHostId("android-overlay-window-host")
    override val type: WindowHostType = WindowHostType.OVERLAY

    init {
        registry.registerHost(this)
    }

    override fun supports(displayId: DisplayId): Boolean = displayContext.displayId == displayId

    fun attached(
        panelId: String,
        bounds: WindowBounds,
        passThrough: Boolean,
    ): WindowSessionId {
        val existing = registry.find(type, panelId)
        val session = existing ?: WindowSession(
            id = WindowSessionId(UUID.randomUUID().toString()),
            panelId = panelId,
            hostType = type,
            displayContext = displayContext,
            placement = WindowPlacement(bounds, zIndex = nextZ()),
            visibility = WindowVisibility.VISIBLE,
            interactionMode = if (passThrough) WindowInteractionMode.PASS_THROUGH else WindowInteractionMode.INTERACTIVE,
            surface = WindowSurface(
                id = WindowSurfaceId("overlay-surface:$panelId"),
                panelId = panelId,
                bounds = bounds.copy(left = 0f, top = 0f, space = WindowCoordinateSpace.LOCAL_WINDOW_PX),
            ),
        ).also { registry.create(it) }

        if (session.displayContext.displayId != displayContext.displayId) {
            registry.report(
                WindowDiagnostic(
                    WindowDiagnosticCode.DISPLAY_UNAVAILABLE,
                    "Overlay $panelId belongs to unavailable display ${session.displayContext.displayId.value}",
                    session.id,
                    id,
                )
            )
            return session.id
        }
        registry.attach(session.id, id)
        registry.move(session.id, bounds.left, bounds.top)
        registry.resize(session.id, bounds.width, bounds.height)
        registry.show(session.id)
        registry.setInteractionMode(
            session.id,
            if (passThrough) WindowInteractionMode.PASS_THROUGH else WindowInteractionMode.INTERACTIVE,
        )
        registry.bringToFront(session.id)
        return session.id
    }

    fun beginDrag(panelId: String) = updateInteraction(panelId, WindowInteractionMode.DRAGGING)
    fun beginResize(panelId: String) = updateInteraction(panelId, WindowInteractionMode.RESIZING)
    fun endInteraction(panelId: String) = updateInteraction(panelId, WindowInteractionMode.INTERACTIVE)

    fun move(panelId: String, leftPx: Float, topPx: Float) {
        registry.find(type, panelId)?.id?.let { registry.move(it, leftPx, topPx) }
    }

    fun resize(panelId: String, widthPx: Float, heightPx: Float) {
        registry.find(type, panelId)?.id?.let { registry.resize(it, widthPx, heightPx) }
    }

    fun detached(panelId: String, hidden: Boolean) {
        val session = registry.find(type, panelId) ?: return
        if (hidden) registry.hide(session.id)
        registry.detach(session.id)
    }

    fun dispose() {
        registry.unregisterHost(this)
    }

    private fun updateInteraction(panelId: String, mode: WindowInteractionMode) {
        registry.find(type, panelId)?.id?.let { registry.setInteractionMode(it, mode) }
    }

    private fun nextZ(): Int = (registry.snapshot().maxOfOrNull { it.placement.zIndex } ?: 0) + 1
}
