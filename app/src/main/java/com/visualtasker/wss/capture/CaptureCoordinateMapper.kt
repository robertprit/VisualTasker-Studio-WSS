package com.visualtasker.wss.capture

import com.visualtasker.wss.window.WindowBounds
import com.visualtasker.wss.window.WindowCoordinateSpace
import com.visualtasker.wss.window.WindowPoint

class CaptureCoordinateMapper(
    private val metadata: CaptureMetadata,
) {
    private val captureBounds = metadata.captureBoundsDisplayPx

    fun windowLocalToDisplay(point: WindowPoint, windowOriginDisplayPx: WindowPoint): WindowPoint =
        metadata.actualDisplayContext.localToDisplay(point, windowOriginDisplayPx)

    fun displayToWindowLocal(point: WindowPoint, windowOriginDisplayPx: WindowPoint): WindowPoint =
        metadata.actualDisplayContext.displayToLocal(point, windowOriginDisplayPx)

    fun displayToScreenshot(point: WindowPoint): WindowPoint {
        require(point.space == WindowCoordinateSpace.DISPLAY_PX)
        val localX = point.x - captureBounds.left
        val localY = point.y - captureBounds.top
        val (rotatedX, rotatedY, rotatedWidth, rotatedHeight) = when (metadata.rotation) {
            90 -> listOf(captureBounds.height - localY, localX, captureBounds.height, captureBounds.width)
            180 -> listOf(captureBounds.width - localX, captureBounds.height - localY, captureBounds.width, captureBounds.height)
            270 -> listOf(localY, captureBounds.width - localX, captureBounds.height, captureBounds.width)
            else -> listOf(localX, localY, captureBounds.width, captureBounds.height)
        }
        return WindowPoint(
            x = rotatedX * metadata.widthPx / rotatedWidth,
            y = rotatedY * metadata.heightPx / rotatedHeight,
            space = WindowCoordinateSpace.SCREENSHOT_PX,
        )
    }

    fun screenshotToDisplay(point: WindowPoint): WindowPoint {
        require(point.space == WindowCoordinateSpace.SCREENSHOT_PX)
        val rotatedWidth = if (metadata.rotation in setOf(90, 270)) captureBounds.height else captureBounds.width
        val rotatedHeight = if (metadata.rotation in setOf(90, 270)) captureBounds.width else captureBounds.height
        val x = point.x * rotatedWidth / metadata.widthPx
        val y = point.y * rotatedHeight / metadata.heightPx
        val (localX, localY) = when (metadata.rotation) {
            90 -> y to (captureBounds.height - x)
            180 -> (captureBounds.width - x) to (captureBounds.height - y)
            270 -> (captureBounds.width - y) to x
            else -> x to y
        }
        return WindowPoint(
            x = captureBounds.left + localX,
            y = captureBounds.top + localY,
            space = WindowCoordinateSpace.DISPLAY_PX,
        )
    }

    fun screenshotToRegionLocal(point: WindowPoint, regionScreenshotPx: WindowBounds): WindowPoint {
        require(point.space == WindowCoordinateSpace.SCREENSHOT_PX)
        require(regionScreenshotPx.space == WindowCoordinateSpace.SCREENSHOT_PX)
        return WindowPoint(
            point.x - regionScreenshotPx.left,
            point.y - regionScreenshotPx.top,
            WindowCoordinateSpace.LOCAL_WINDOW_PX,
        )
    }
}
