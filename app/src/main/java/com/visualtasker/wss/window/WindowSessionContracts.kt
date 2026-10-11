package com.visualtasker.wss.window

import kotlin.math.roundToInt

@JvmInline
value class WindowSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "WindowSessionId must not be blank" }
    }
}

@JvmInline
value class WindowHostId(val value: String) {
    init {
        require(value.isNotBlank()) { "WindowHostId must not be blank" }
    }
}

@JvmInline
value class WindowSurfaceId(val value: String) {
    init {
        require(value.isNotBlank()) { "WindowSurfaceId must not be blank" }
    }
}

@JvmInline
value class DisplayId(val value: Int)

enum class WindowHostType {
    STUDIO,
    OVERLAY,
    DISPLAY,
}

enum class WindowVisibility {
    VISIBLE,
    HIDDEN,
}

enum class WindowFocusState {
    INACTIVE,
    ACTIVE,
    KEYBOARD,
    TOUCH,
}

enum class WindowInteractionMode {
    PASS_THROUGH,
    INTERACTIVE,
    DRAGGING,
    RESIZING,
    MODAL,
}

enum class WindowDockPosition {
    FLOATING,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
}

enum class WindowCoordinateSpace {
    COMPOSE_DP,
    LOCAL_WINDOW_PX,
    DISPLAY_PX,
    SCREENSHOT_PX,
}

data class WindowPoint(
    val x: Float,
    val y: Float,
    val space: WindowCoordinateSpace,
)

data class WindowInsets(
    val leftPx: Int = 0,
    val topPx: Int = 0,
    val rightPx: Int = 0,
    val bottomPx: Int = 0,
)

data class WindowBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val space: WindowCoordinateSpace,
) {
    init {
        require(left.isFinite() && top.isFinite()) { "Window position must be finite" }
        require(width.isFinite() && height.isFinite()) { "Window size must be finite" }
        require(width >= 0f && height >= 0f) { "Window size must not be negative" }
    }

    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

data class WindowDocking(
    val position: WindowDockPosition = WindowDockPosition.FLOATING,
    val targetSessionId: WindowSessionId? = null,
)

data class WindowPlacement(
    val bounds: WindowBounds,
    val docking: WindowDocking = WindowDocking(),
    val zIndex: Int = 0,
)

data class WindowSurface(
    val id: WindowSurfaceId,
    val panelId: String,
    val bounds: WindowBounds,
)

data class DisplayContext(
    val displayId: DisplayId,
    val physicalWidthPx: Int,
    val physicalHeightPx: Int,
    val density: Float,
    val rotation: Int,
    val insets: WindowInsets = WindowInsets(),
) {
    init {
        require(physicalWidthPx > 0 && physicalHeightPx > 0) { "Display size must be positive" }
        require(density > 0f && density.isFinite()) { "Display density must be positive" }
        require(rotation in setOf(0, 90, 180, 270)) { "Rotation must be 0, 90, 180 or 270" }
    }

    val availableBounds: WindowBounds
        get() = WindowBounds(
            left = insets.leftPx.toFloat(),
            top = insets.topPx.toFloat(),
            width = (physicalWidthPx - insets.leftPx - insets.rightPx).coerceAtLeast(0).toFloat(),
            height = (physicalHeightPx - insets.topPx - insets.bottomPx).coerceAtLeast(0).toFloat(),
            space = WindowCoordinateSpace.DISPLAY_PX,
        )

    fun composeDpToLocalPx(point: WindowPoint): WindowPoint {
        require(point.space == WindowCoordinateSpace.COMPOSE_DP)
        return WindowPoint(point.x * density, point.y * density, WindowCoordinateSpace.LOCAL_WINDOW_PX)
    }

    fun localPxToComposeDp(point: WindowPoint): WindowPoint {
        require(point.space == WindowCoordinateSpace.LOCAL_WINDOW_PX)
        return WindowPoint(point.x / density, point.y / density, WindowCoordinateSpace.COMPOSE_DP)
    }

    fun localToDisplay(point: WindowPoint, windowOrigin: WindowPoint): WindowPoint {
        require(point.space == WindowCoordinateSpace.LOCAL_WINDOW_PX)
        require(windowOrigin.space == WindowCoordinateSpace.DISPLAY_PX)
        return WindowPoint(
            x = windowOrigin.x + point.x,
            y = windowOrigin.y + point.y,
            space = WindowCoordinateSpace.DISPLAY_PX,
        )
    }

    fun displayToLocal(point: WindowPoint, windowOrigin: WindowPoint): WindowPoint {
        require(point.space == WindowCoordinateSpace.DISPLAY_PX)
        require(windowOrigin.space == WindowCoordinateSpace.DISPLAY_PX)
        return WindowPoint(
            x = point.x - windowOrigin.x,
            y = point.y - windowOrigin.y,
            space = WindowCoordinateSpace.LOCAL_WINDOW_PX,
        )
    }

    fun displayToScreenshot(point: WindowPoint, screenshotWidthPx: Int, screenshotHeightPx: Int): WindowPoint {
        require(point.space == WindowCoordinateSpace.DISPLAY_PX)
        require(screenshotWidthPx > 0 && screenshotHeightPx > 0)
        return WindowPoint(
            x = point.x * screenshotWidthPx / physicalWidthPx,
            y = point.y * screenshotHeightPx / physicalHeightPx,
            space = WindowCoordinateSpace.SCREENSHOT_PX,
        )
    }

    fun clamp(bounds: WindowBounds): WindowBounds {
        require(bounds.space == WindowCoordinateSpace.DISPLAY_PX)
        val available = availableBounds
        val width = bounds.width.coerceAtMost(available.width)
        val height = bounds.height.coerceAtMost(available.height)
        return bounds.copy(
            left = bounds.left.coerceIn(available.left, (available.right - width).coerceAtLeast(available.left)),
            top = bounds.top.coerceIn(available.top, (available.bottom - height).coerceAtLeast(available.top)),
            width = width,
            height = height,
        )
    }

    fun roundedDisplayBounds(bounds: WindowBounds): WindowBounds {
        require(bounds.space == WindowCoordinateSpace.DISPLAY_PX)
        return bounds.copy(
            left = bounds.left.roundToInt().toFloat(),
            top = bounds.top.roundToInt().toFloat(),
            width = bounds.width.roundToInt().toFloat(),
            height = bounds.height.roundToInt().toFloat(),
        )
    }
}

data class WindowSession(
    val id: WindowSessionId,
    val panelId: String,
    val hostType: WindowHostType,
    val displayContext: DisplayContext,
    val placement: WindowPlacement,
    val visibility: WindowVisibility = WindowVisibility.VISIBLE,
    val focusState: WindowFocusState = WindowFocusState.INACTIVE,
    val interactionMode: WindowInteractionMode = WindowInteractionMode.INTERACTIVE,
    val attachedHostId: WindowHostId? = null,
    val surface: WindowSurface? = null,
)

enum class WindowDiagnosticCode {
    SESSION_NOT_FOUND,
    DUPLICATE_SESSION,
    HOST_NOT_FOUND,
    HOST_TYPE_MISMATCH,
    ALREADY_ATTACHED_TO_OTHER_HOST,
    DISPLAY_UNAVAILABLE,
    INVALID_BOUNDS,
    INVALID_DOCKING,
    HOST_ATTACH_FAILED,
    HOST_DETACH_FAILED,
    OVERLAY_PERMISSION_MISSING,
    WINDOW_MANAGER_FAILURE,
}

data class WindowDiagnostic(
    val code: WindowDiagnosticCode,
    val message: String,
    val sessionId: WindowSessionId? = null,
    val hostId: WindowHostId? = null,
)

sealed interface WindowOperationResult {
    data class Success(val session: WindowSession, val changed: Boolean) : WindowOperationResult
    data class Failure(val diagnostic: WindowDiagnostic) : WindowOperationResult
}
