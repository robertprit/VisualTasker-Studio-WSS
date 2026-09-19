package com.visualtasker.wss.recording

import kotlin.math.max
import kotlin.math.min

data class PlaybackPoint(val x: Float, val y: Float)
data class PlaybackRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class RecordingPlaybackViewportTransform(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val rotation: Int,
) {
    private val normalizedRotation = ((rotation % 360) + 360) % 360
    private val rotatedWidth = if (normalizedRotation == 90 || normalizedRotation == 270) sourceHeight.toFloat() else sourceWidth.toFloat()
    private val rotatedHeight = if (normalizedRotation == 90 || normalizedRotation == 270) sourceWidth.toFloat() else sourceHeight.toFloat()
    val scale: Float = min(viewportWidth / rotatedWidth.coerceAtLeast(1f), viewportHeight / rotatedHeight.coerceAtLeast(1f))
    val offsetX: Float = (viewportWidth - rotatedWidth * scale) / 2f
    val offsetY: Float = (viewportHeight - rotatedHeight * scale) / 2f

    fun mapPoint(x: Float, y: Float): PlaybackPoint {
        val rotated = rotate(x, y)
        return PlaybackPoint(offsetX + rotated.x * scale, offsetY + rotated.y * scale)
    }

    fun mapRect(left: Float, top: Float, right: Float, bottom: Float): PlaybackRect {
        val points = listOf(
            mapPoint(left, top), mapPoint(right, top), mapPoint(right, bottom), mapPoint(left, bottom),
        )
        return PlaybackRect(
            left = points.minOf(PlaybackPoint::x),
            top = points.minOf(PlaybackPoint::y),
            right = points.maxOf(PlaybackPoint::x),
            bottom = points.maxOf(PlaybackPoint::y),
        )
    }

    fun unmapPoint(viewportX: Float, viewportY: Float): PlaybackPoint? {
        if (scale <= 0f) return null
        val x = (viewportX - offsetX) / scale
        val y = (viewportY - offsetY) / scale
        if (x !in 0f..rotatedWidth || y !in 0f..rotatedHeight) return null
        return when (normalizedRotation) {
            90 -> PlaybackPoint(y, sourceHeight - x)
            180 -> PlaybackPoint(sourceWidth - x, sourceHeight - y)
            270 -> PlaybackPoint(sourceWidth - y, x)
            else -> PlaybackPoint(x, y)
        }
    }

    private fun rotate(x: Float, y: Float): PlaybackPoint = when (normalizedRotation) {
        90 -> PlaybackPoint(sourceHeight - y, x)
        180 -> PlaybackPoint(sourceWidth - x, sourceHeight - y)
        270 -> PlaybackPoint(y, sourceWidth - x)
        else -> PlaybackPoint(x, y)
    }
}

fun A11yNodeSnapshot.deepestNodeAt(x: Float, y: Float): A11yNodeSnapshot? {
    if (x < left || x > right || y < top || y > bottom) return null
    return children.asReversed().firstNotNullOfOrNull { child -> child.deepestNodeAt(x, y) } ?: this
}

fun A11yNodeSnapshot.findPlaybackNode(id: String): A11yNodeSnapshot? =
    if (stableSnapshotNodeId == id) this else children.firstNotNullOfOrNull { it.findPlaybackNode(id) }
