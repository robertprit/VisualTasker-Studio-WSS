package com.visualtasker.wss.recording

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
import java.io.File

class AndroidRecorderCaptureSource(
    private val context: Context,
    private val serviceProvider: () -> VisualTaskerAccessibilityService? = VisualTaskerAccessibilityService::current,
) : RecorderCaptureSource {
    override suspend fun capture(): RecorderCapture {
        val service = checkNotNull(serviceProvider()) { "Accessibility service is not connected." }
        val window = checkNotNull(service.currentWindowContext()) { "Window context is unavailable." }
        val root = checkNotNull(service.currentRecordingA11yTree()) { "Accessibility root is unavailable." }
        val temporary = File(context.cacheDir, "recorder-capture-${System.nanoTime()}.png")
        try {
            check(service.takeScreenshotTo(temporary) && temporary.isFile) { "Screenshot capture failed." }
            val bytes = temporary.readBytes()
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            check(options.outWidth > 0 && options.outHeight > 0) { "Screenshot dimensions are invalid." }
            val primaryWindow = window.windows.firstOrNull { it.active } ?: window.windows.firstOrNull()
            return RecorderCapture(
                pngBytes = bytes,
                widthPx = options.outWidth,
                heightPx = options.outHeight,
                rotation = context.getSystemService(DisplayManager::class.java)
                    ?.getDisplay(Display.DEFAULT_DISPLAY)
                    ?.rotation
                    ?: Surface.ROTATION_0,
                densityDpi = context.resources.displayMetrics.densityDpi,
                windowContext = RecordingWindowContext(
                    packageName = window.packageName,
                    activityName = window.activityName,
                    windowId = primaryWindow?.id,
                    title = primaryWindow?.title,
                ),
                a11yRoot = root,
            )
        } finally {
            temporary.delete()
        }
    }
}
