package com.visualtasker.wss.recording

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
import com.visualtasker.wss.capture.CaptureFrameProcessService
import com.visualtasker.wss.capture.CaptureRequest
import com.visualtasker.wss.capture.CaptureResult
import com.visualtasker.wss.window.AndroidDisplayContextFactory

class AndroidRecorderCaptureSource(
    private val context: Context,
    private val serviceProvider: () -> VisualTaskerAccessibilityService? = VisualTaskerAccessibilityService::current,
) : RecorderCaptureSource {
    override suspend fun capture(): RecorderCapture {
        val service = checkNotNull(serviceProvider()) { "Accessibility service is not connected." }
        val window = checkNotNull(service.currentWindowContext()) { "Window context is unavailable." }
        val root = checkNotNull(service.currentRecordingA11yTree()) { "Accessibility root is unavailable." }
        val display = checkNotNull(context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)) {
            "Default display is unavailable."
        }
        val displayContext = AndroidDisplayContextFactory.from(context.createDisplayContext(display))
        val result = CaptureFrameProcessService.get(context).capture(
            CaptureRequest(
                requestId = "recorder-${System.nanoTime()}",
                displayContext = displayContext,
            )
        )
        val success = result as? CaptureResult.Success
            ?: error((result as CaptureResult.Failure).failure.message)
        success.lease.use { lease ->
            val metadata = lease.frame.metadata
            val insets = metadata.actualDisplayContext.insets
            val bounds = metadata.captureBoundsDisplayPx
            val primaryWindow = window.windows.firstOrNull { it.active } ?: window.windows.firstOrNull()
            return RecorderCapture(
                pngBytes = lease.readBytes(),
                widthPx = metadata.widthPx,
                heightPx = metadata.heightPx,
                rotation = metadata.rotation,
                densityDpi = metadata.densityDpi,
                windowContext = RecordingWindowContext(
                    packageName = window.packageName,
                    activityName = window.activityName,
                    windowId = primaryWindow?.id,
                    title = primaryWindow?.title,
                ),
                a11yRoot = root,
                sourceCaptureFrameId = lease.frame.id.value,
                displayId = metadata.actualDisplayContext.displayId.value,
                captureSource = metadata.source.name,
                insetLeftPx = insets.leftPx,
                insetTopPx = insets.topPx,
                insetRightPx = insets.rightPx,
                insetBottomPx = insets.bottomPx,
                captureLeftPx = bounds.left,
                captureTopPx = bounds.top,
                captureWidthPx = bounds.width,
                captureHeightPx = bounds.height,
            )
        }
    }
}
