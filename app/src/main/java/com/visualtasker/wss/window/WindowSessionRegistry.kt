package com.visualtasker.wss.window

interface WindowSessionPersistence {
    fun load(): List<WindowSession>
    fun save(sessions: List<WindowSession>)
}

interface WindowHost {
    val id: WindowHostId
    val type: WindowHostType
    fun supports(displayId: DisplayId): Boolean
    fun onAttach(session: WindowSession): Boolean = true
    fun onDetach(session: WindowSession): Boolean = true
}

class WindowSessionRegistry(
    private val persistence: WindowSessionPersistence? = null,
) {
    private val sessions = linkedMapOf<WindowSessionId, WindowSession>()
    private val hosts = linkedMapOf<WindowHostId, WindowHost>()
    private val diagnostics = mutableListOf<WindowDiagnostic>()

    init {
        persistence?.load()?.forEach { restored ->
            sessions[restored.id] = restored.copy(
                attachedHostId = null,
                focusState = WindowFocusState.INACTIVE,
                interactionMode = restored.interactionMode
                    .takeUnless { it == WindowInteractionMode.DRAGGING || it == WindowInteractionMode.RESIZING }
                    ?: WindowInteractionMode.INTERACTIVE,
            )
        }
    }

    @Synchronized
    fun registerHost(host: WindowHost) {
        val previous = hosts.put(host.id, host)
        if (previous === host) return
        sessions.values
            .filter { it.attachedHostId == host.id }
            .forEach { session -> host.onAttach(session) }
    }

    @Synchronized
    fun unregisterHost(hostId: WindowHostId) {
        val host = hosts.remove(hostId) ?: return
        sessions.values
            .filter { it.attachedHostId == hostId }
            .forEach { session -> host.onDetach(session) }
    }

    @Synchronized
    fun unregisterHost(host: WindowHost) {
        if (hosts[host.id] !== host) return
        unregisterHost(host.id)
    }

    @Synchronized
    fun create(session: WindowSession): WindowOperationResult {
        val existing = sessions[session.id]
        if (existing != null) {
            return if (existing == session) {
                WindowOperationResult.Success(existing, changed = false)
            } else {
                failure(
                    WindowDiagnosticCode.DUPLICATE_SESSION,
                    "WindowSession ${session.id.value} already exists with a different contract",
                    session.id,
                )
            }
        }
        if (session.panelId.isBlank() || session.placement.bounds.width <= 0f || session.placement.bounds.height <= 0f) {
            return failure(WindowDiagnosticCode.INVALID_BOUNDS, "Panel identity and positive bounds are required", session.id)
        }
        sessions[session.id] = session.copy(attachedHostId = null)
        persist()
        return WindowOperationResult.Success(sessions.getValue(session.id), changed = true)
    }

    @Synchronized
    fun attach(sessionId: WindowSessionId, hostId: WindowHostId): WindowOperationResult = mutate(sessionId) { current ->
        val host = hosts[hostId]
            ?: return@mutate failure(WindowDiagnosticCode.HOST_NOT_FOUND, "Window host ${hostId.value} is not registered", sessionId, hostId)
        if (host.type != current.hostType) {
            return@mutate failure(WindowDiagnosticCode.HOST_TYPE_MISMATCH, "Host type ${host.type} cannot attach ${current.hostType}", sessionId, hostId)
        }
        if (!host.supports(current.displayContext.displayId)) {
            return@mutate failure(WindowDiagnosticCode.DISPLAY_UNAVAILABLE, "Display ${current.displayContext.displayId.value} is unavailable", sessionId, hostId)
        }
        if (current.attachedHostId == hostId) return@mutate unchanged(current)
        if (current.attachedHostId != null) {
            return@mutate failure(
                WindowDiagnosticCode.ALREADY_ATTACHED_TO_OTHER_HOST,
                "Session is attached to ${current.attachedHostId.value}",
                sessionId,
                hostId,
            )
        }
        if (!host.onAttach(current)) {
            return@mutate failure(WindowDiagnosticCode.HOST_ATTACH_FAILED, "Host rejected attach", sessionId, hostId)
        }
        changed(current.copy(attachedHostId = hostId))
    }

    @Synchronized
    fun detach(sessionId: WindowSessionId): WindowOperationResult = mutate(sessionId) { current ->
        val hostId = current.attachedHostId ?: return@mutate unchanged(current)
        val host = hosts[hostId]
        if (host != null && !host.onDetach(current)) {
            return@mutate failure(WindowDiagnosticCode.HOST_DETACH_FAILED, "Host rejected detach", sessionId, hostId)
        }
        changed(current.copy(attachedHostId = null, focusState = WindowFocusState.INACTIVE))
    }

    @Synchronized
    fun show(sessionId: WindowSessionId): WindowOperationResult = update(sessionId) { it.copy(visibility = WindowVisibility.VISIBLE) }

    @Synchronized
    fun hide(sessionId: WindowSessionId): WindowOperationResult = update(sessionId) {
        it.copy(visibility = WindowVisibility.HIDDEN, focusState = WindowFocusState.INACTIVE)
    }

    @Synchronized
    fun move(sessionId: WindowSessionId, left: Float, top: Float): WindowOperationResult = update(sessionId) { current ->
        val moved = current.placement.bounds.copy(left = left, top = top)
        current.copy(placement = current.placement.copy(bounds = clampIfDisplayPixels(current, moved)))
    }

    @Synchronized
    fun resize(sessionId: WindowSessionId, width: Float, height: Float): WindowOperationResult {
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) {
            return failure(WindowDiagnosticCode.INVALID_BOUNDS, "Window size must be finite and positive", sessionId)
        }
        return update(sessionId) { current ->
            val resized = current.placement.bounds.copy(width = width, height = height)
            current.copy(placement = current.placement.copy(bounds = clampIfDisplayPixels(current, resized)))
        }
    }

    @Synchronized
    fun bringToFront(sessionId: WindowSessionId): WindowOperationResult = mutate(sessionId) { current ->
        val top = sessions.values.maxOfOrNull { it.placement.zIndex } ?: 0
        if (current.placement.zIndex == top) return@mutate unchanged(current)
        changed(current.copy(placement = current.placement.copy(zIndex = top + 1)))
    }

    @Synchronized
    fun setZIndex(sessionId: WindowSessionId, zIndex: Int): WindowOperationResult =
        update(sessionId) { it.copy(placement = it.placement.copy(zIndex = zIndex)) }

    @Synchronized
    fun dock(sessionId: WindowSessionId, docking: WindowDocking): WindowOperationResult {
        if (docking.position == WindowDockPosition.FLOATING) {
            return failure(WindowDiagnosticCode.INVALID_DOCKING, "Use undock for FLOATING", sessionId)
        }
        return update(sessionId) { it.copy(placement = it.placement.copy(docking = docking)) }
    }

    @Synchronized
    fun undock(sessionId: WindowSessionId): WindowOperationResult = update(sessionId) {
        it.copy(placement = it.placement.copy(docking = WindowDocking()))
    }

    @Synchronized
    fun focus(sessionId: WindowSessionId, focusState: WindowFocusState): WindowOperationResult = mutate(sessionId) { current ->
        var changedAny = false
        sessions.replaceAll { id, session ->
            if (id != sessionId && session.focusState != WindowFocusState.INACTIVE) {
                changedAny = true
                session.copy(focusState = WindowFocusState.INACTIVE)
            } else {
                session
            }
        }
        val updated = current.copy(focusState = focusState)
        if (updated != current) changedAny = true
        if (changedAny) changed(updated) else unchanged(current)
    }

    @Synchronized
    fun setInteractionMode(sessionId: WindowSessionId, mode: WindowInteractionMode): WindowOperationResult =
        update(sessionId) { it.copy(interactionMode = mode) }

    @Synchronized
    fun updateDisplayContext(sessionId: WindowSessionId, context: DisplayContext): WindowOperationResult = update(sessionId) { current ->
        current.copy(
            displayContext = context,
            placement = current.placement.copy(bounds = clampIfDisplayPixels(current.copy(displayContext = context), current.placement.bounds)),
        )
    }

    @Synchronized
    fun close(sessionId: WindowSessionId): WindowOperationResult {
        val current = sessions[sessionId]
            ?: return failure(WindowDiagnosticCode.SESSION_NOT_FOUND, "WindowSession ${sessionId.value} does not exist", sessionId)
        val host = current.attachedHostId?.let(hosts::get)
        if (host != null && !host.onDetach(current)) {
            return failure(WindowDiagnosticCode.HOST_DETACH_FAILED, "Host rejected close detach", sessionId, host.id)
        }
        sessions.remove(sessionId)
        persist()
        return WindowOperationResult.Success(current.copy(attachedHostId = null), changed = true)
    }

    @Synchronized
    fun restore(session: WindowSession): WindowOperationResult {
        val restored = session.copy(attachedHostId = null, focusState = WindowFocusState.INACTIVE)
        val changed = sessions[session.id] != restored
        sessions[session.id] = restored
        if (changed) persist()
        return WindowOperationResult.Success(restored, changed)
    }

    @Synchronized
    fun session(sessionId: WindowSessionId): WindowSession? = sessions[sessionId]

    @Synchronized
    fun find(hostType: WindowHostType, panelId: String): WindowSession? =
        sessions.values.firstOrNull { it.hostType == hostType && it.panelId == panelId }

    @Synchronized
    fun snapshot(): List<WindowSession> = sessions.values.sortedWith(
        compareBy<WindowSession> { it.placement.zIndex }.thenBy { it.id.value }
    )

    @Synchronized
    fun diagnostics(): List<WindowDiagnostic> = diagnostics.toList()

    @Synchronized
    fun report(diagnostic: WindowDiagnostic) {
        diagnostics += diagnostic
    }

    private fun clampIfDisplayPixels(session: WindowSession, bounds: WindowBounds): WindowBounds =
        if (bounds.space == WindowCoordinateSpace.DISPLAY_PX) session.displayContext.clamp(bounds) else bounds

    private inline fun update(
        sessionId: WindowSessionId,
        transform: (WindowSession) -> WindowSession,
    ): WindowOperationResult = mutate(sessionId) { current ->
        val updated = transform(current)
        if (updated == current) unchanged(current) else changed(updated)
    }

    private inline fun mutate(
        sessionId: WindowSessionId,
        block: (WindowSession) -> WindowOperationResult,
    ): WindowOperationResult {
        val current = sessions[sessionId]
            ?: return failure(WindowDiagnosticCode.SESSION_NOT_FOUND, "WindowSession ${sessionId.value} does not exist", sessionId)
        val result = block(current)
        if (result is WindowOperationResult.Success && result.changed) {
            sessions[sessionId] = result.session
            persist()
        }
        return result
    }

    private fun changed(session: WindowSession) = WindowOperationResult.Success(session, changed = true)
    private fun unchanged(session: WindowSession) = WindowOperationResult.Success(session, changed = false)

    private fun failure(
        code: WindowDiagnosticCode,
        message: String,
        sessionId: WindowSessionId? = null,
        hostId: WindowHostId? = null,
    ): WindowOperationResult.Failure {
        val diagnostic = WindowDiagnostic(code, message, sessionId, hostId)
        diagnostics += diagnostic
        return WindowOperationResult.Failure(diagnostic)
    }

    private fun persist() {
        persistence?.save(snapshot())
    }
}
