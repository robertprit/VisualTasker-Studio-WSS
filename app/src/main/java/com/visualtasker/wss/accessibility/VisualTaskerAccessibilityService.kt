package com.visualtasker.wss.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VisualTaskerAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    suspend fun clickText(text: String): Boolean {
        val target = rootInActiveWindow?.findFirstTextMatch(text) ?: return false
        val bounds = Rect()
        target.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false
        return clickPoint(bounds.centerX(), bounds.centerY())
    }

    suspend fun clickPoint(x: Int, y: Int): Boolean =
        dispatchPathGesture(
            path = Path().apply { moveTo(x.toFloat(), y.toFloat()) },
            durationMs = 1L,
        )

    suspend fun swipe(points: List<RuntimePoint>, durationMs: Long): Boolean {
        if (points.size < 2) return false
        val path = Path().apply {
            moveTo(points.first().x.toFloat(), points.first().y.toFloat())
            points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
        }
        return dispatchPathGesture(path, durationMs.coerceAtLeast(1L))
    }

    suspend fun takeScreenshotTo(file: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return suspendCancellableCoroutine { continuation ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                            ?.copy(Bitmap.Config.ARGB_8888, false)
                        screenshot.hardwareBuffer.close()
                        val success = bitmap?.let {
                            file.parentFile?.mkdirs()
                            runCatching {
                                file.outputStream().use { output ->
                                    it.compress(Bitmap.CompressFormat.PNG, 100, output)
                                }
                            }.getOrDefault(false)
                        } ?: false
                        bitmap?.recycle()
                        if (continuation.isActive) continuation.resume(success)
                    }

                    override fun onFailure(errorCode: Int) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
        }
    }

    private suspend fun dispatchPathGesture(path: Path, durationMs: Long): Boolean =
        suspendCancellableCoroutine { continuation ->
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
                .build()
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
                null,
            )
        }

    companion object {
        @Volatile
        private var instance: VisualTaskerAccessibilityService? = null

        fun current(): VisualTaskerAccessibilityService? = instance

        fun isConnected(): Boolean = instance != null
    }
}

data class RuntimePoint(
    val x: Int,
    val y: Int,
)

private fun AccessibilityNodeInfo.findFirstTextMatch(query: String): AccessibilityNodeInfo? {
    val needle = query.trim()
    if (needle.isEmpty()) return null
    val ownText = listOfNotNull(text, contentDescription)
        .map(CharSequence::toString)
        .any { it.contains(needle, ignoreCase = true) }
    if (ownText) return this
    for (index in 0 until childCount) {
        val match = getChild(index)?.findFirstTextMatch(needle)
        if (match != null) return match
    }
    return null
}
