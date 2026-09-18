package com.visualtasker.wss.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityWindowInfo
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.visualtasker.wss.workspace.model.CoordinateSpace
import com.visualtasker.wss.workspace.model.CoordinateSpaceKind
import com.visualtasker.wss.workspace.model.RecordingEventStore
import com.visualtasker.wss.workspace.model.WindowContext
import com.visualtasker.wss.workspace.model.WindowSnapshot
import com.visualtasker.wss.workspace.model.WorldviewRect
import java.io.File
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VisualTaskerAccessibilityService : AccessibilityService() {
    @Volatile
    private var lastInspectorSnapshot: AccessibilityInspectorSnapshot? = null

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            lastInspectorSnapshot = accessibilityEvent.source
                ?.toInspectorSnapshot(
                    activityName = currentWindowContext(accessibilityEvent)?.activityName,
                    timestampMs = accessibilityEvent.eventTime,
                )
                ?: lastInspectorSnapshot
            RecordingEventStore.recordAccessibilityEvent(
                event = accessibilityEvent,
                windowContext = currentWindowContext(accessibilityEvent),
            )
        }
    }

    override fun onInterrupt() = Unit

    fun currentWindowContext(event: AccessibilityEvent? = null): WindowContext? {
        val now = System.currentTimeMillis()
        val snapshots = windows.orEmpty()
            .mapNotNull { window -> window.toSnapshot(now) }
            .distinctBy { it.id }
        val rootPackage = rootInActiveWindow?.packageName?.toString()?.takeIf { it.isNotBlank() }
        val eventPackage = event?.packageName?.toString()?.takeIf { it.isNotBlank() }
        val eventActivity = event?.trustedActivityName()
        if (snapshots.isEmpty() && eventPackage == null && eventActivity == null) return null
        return WindowContext(
            packageName = eventPackage ?: rootPackage ?: snapshots.firstOrNull()?.packageName,
            activityName = eventActivity,
            windows = snapshots.ifEmpty {
                listOf(
                    WindowSnapshot(
                        id = "android-window-${eventPackage.orEmpty()}-${eventActivity.orEmpty()}".ifBlank { "android-window-unknown" },
                        packageName = eventPackage ?: rootPackage,
                        activityName = eventActivity,
                        timestampMs = now,
                    ),
                )
            },
            timestampMs = now,
        )
    }

    internal fun currentInspectorSnapshot(): AccessibilityInspectorSnapshot? {
        val context = currentWindowContext()
        val root = rootInActiveWindow
        val focusedNode = root?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return (focusedNode ?: root)
            ?.toInspectorSnapshot(
                activityName = context?.activityName,
                timestampMs = System.currentTimeMillis(),
            )
            ?: lastInspectorSnapshot
    }

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

internal data class AccessibilityInspectorSnapshot(
    val packageName: String?,
    val activityName: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val clickable: Boolean,
    val focusable: Boolean,
    val focused: Boolean,
    val enabled: Boolean,
    val visible: Boolean,
    val timestampMs: Long,
)

internal fun AccessibilityInspectorSnapshot.toInspectorText(): String = buildString {
    appendLine("Accessibility: aktiv")
    append("App: ").append(packageName ?: "-")
    activityName?.let { append("\nActivity: ").append(it) }
    append("\nElement: ").append(className?.substringAfterLast('.') ?: "-")
    text?.takeIf(String::isNotBlank)?.let { append("\nText: ").append(it) }
    contentDescription?.takeIf(String::isNotBlank)?.let { append("\nBeschreibung: ").append(it) }
    viewId?.takeIf(String::isNotBlank)?.let { append("\nID: ").append(it) }
    append("\nBounds: [").append(left).append(',').append(top)
        .append(" - ").append(right).append(',').append(bottom).append(']')
    append("\nStatus: ")
    append(
        listOfNotNull(
            "clickbar".takeIf { clickable },
            "fokussierbar".takeIf { focusable },
            "fokussiert".takeIf { focused },
            "aktiv".takeIf { enabled },
            "sichtbar".takeIf { visible },
        ).ifEmpty { listOf("passiv") }.joinToString(" · ")
    )
}

private fun AccessibilityNodeInfo.toInspectorSnapshot(
    activityName: String?,
    timestampMs: Long,
): AccessibilityInspectorSnapshot {
    val bounds = Rect().also(::getBoundsInScreen)
    return AccessibilityInspectorSnapshot(
        packageName = packageName?.toString()?.takeIf(String::isNotBlank),
        activityName = activityName,
        className = className?.toString()?.takeIf(String::isNotBlank),
        text = text?.toString()?.takeIf(String::isNotBlank),
        contentDescription = contentDescription?.toString()?.takeIf(String::isNotBlank),
        viewId = viewIdResourceName?.takeIf(String::isNotBlank),
        left = bounds.left,
        top = bounds.top,
        right = bounds.right,
        bottom = bounds.bottom,
        clickable = isClickable,
        focusable = isFocusable,
        focused = isFocused || isAccessibilityFocused,
        enabled = isEnabled,
        visible = isVisibleToUser,
        timestampMs = timestampMs,
    )
}

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

private fun AccessibilityEvent.trustedActivityName(): String? {
    if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return null
    val eventPackageName = packageName?.toString().orEmpty()
    return className
        ?.toString()
        ?.takeIf { candidate ->
            candidate.isNotBlank() &&
                !candidate.startsWith("android.") &&
                (
                    candidate.endsWith("Activity") ||
                        eventPackageName.isNotBlank() && candidate.startsWith(eventPackageName)
                    )
        }
}

private fun AccessibilityWindowInfo.toSnapshot(timestampMs: Long): WindowSnapshot? {
    val bounds = Rect().also(::getBoundsInScreen).toWorldviewRect()
    val rootNode = root
    val packageName = rootNode?.packageName?.toString()?.takeIf { it.isNotBlank() }
    val titleText = title?.toString()?.takeIf { it.isNotBlank() }
    return WindowSnapshot(
        id = "android-window-$id",
        packageName = packageName,
        activityName = null,
        title = titleText,
        bounds = bounds,
        focused = isFocused,
        active = isActive,
        timestampMs = timestampMs,
        properties = mapOf(
            "androidWindowId" to id.toString(),
            "windowType" to type.toString(),
            "windowLayer" to layer.toString(),
        ),
    )
}

private fun Rect.toWorldviewRect(): WorldviewRect? =
    runCatching {
        WorldviewRect(
            left = left.toFloat(),
            top = top.toFloat(),
            right = right.toFloat(),
            bottom = bottom.toFloat(),
            coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
        )
    }.getOrNull()
