package com.visualtasker.wss.capture

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.SystemClock
import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
import com.visualtasker.wss.emscript.runtime.ProviderAvailability
import com.visualtasker.wss.emscript.runtime.ProviderCapabilityState
import com.visualtasker.wss.emscript.runtime.ProviderConnectionState
import com.visualtasker.wss.emscript.runtime.ProviderExecutionState
import com.visualtasker.wss.emscript.runtime.ProviderPermissionState
import com.visualtasker.wss.window.AndroidDisplayContextFactory
import com.visualtasker.wss.window.WindowCoordinateSpace
import de.visualtasker.emscript.contract.ProviderId
import java.io.File

class AccessibilityCaptureFrameProvider(
    context: Context,
    private val serviceProvider: () -> VisualTaskerAccessibilityService? = VisualTaskerAccessibilityService::current,
) : CaptureFrameProvider {
    private val applicationContext = context.applicationContext
    private val displayManager = applicationContext.getSystemService(DisplayManager::class.java)

    override val descriptor = CaptureProviderDescriptor(
        id = ProviderId("android.accessibility.capture"),
        displayName = "Android Accessibility Screenshot",
        capabilities = setOf(
            CaptureCapability.SCREENSHOT,
            CaptureCapability.DISPLAY_SELECTION,
            CaptureCapability.CANCELLATION,
            CaptureCapability.SHARED_FRAME,
        ),
        priority = 100,
    )

    override fun availability(request: CaptureRequest): ProviderAvailability = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> ProviderAvailability(
            capability = ProviderCapabilityState.UNSUPPORTED,
            execution = ProviderExecutionState.NOT_EXECUTABLE,
            diagnosticCode = "ACCESSIBILITY_SCREENSHOT_REQUIRES_API_30",
            message = "Accessibility screenshots require Android 11 or newer.",
        )

        displayManager?.getDisplay(request.displayContext.displayId.value) == null -> ProviderAvailability(
            capability = ProviderCapabilityState.UNAVAILABLE,
            execution = ProviderExecutionState.NOT_EXECUTABLE,
            diagnosticCode = "CAPTURE_DISPLAY_UNAVAILABLE",
            message = "Requested display ${request.displayContext.displayId.value} is unavailable.",
        )

        serviceProvider() == null -> ProviderAvailability(
            connection = ProviderConnectionState.DISCONNECTED,
            permission = ProviderPermissionState.DENIED,
            execution = ProviderExecutionState.NOT_EXECUTABLE,
            diagnosticCode = "ACCESSIBILITY_SERVICE_DISCONNECTED",
            message = "Accessibility service is not connected.",
        )

        else -> ProviderAvailability.available()
    }

    override suspend fun capture(request: CaptureRequest): ProviderCaptureResult {
        val availability = availability(request)
        if (!availability.isExecutable) {
            return ProviderCaptureResult.Failure(
                availability.toCaptureFailure(descriptor.id, request.displayContext.displayId)
            )
        }
        if (request.cropDisplayPx != null) {
            return ProviderCaptureResult.Failure(
                CaptureFailure(
                    CaptureFailureKind.CAPABILITY_UNSUPPORTED,
                    "ACCESSIBILITY_CAPTURE_CROP_UNSUPPORTED",
                    "Accessibility capture provides a full display frame; crop must be derived by a consumer.",
                    descriptor.id,
                    request.displayContext.displayId,
                )
            )
        }
        if (request.cancellation.isCancellationRequested()) return cancelled(request)
        val service = serviceProvider() ?: return ProviderCaptureResult.Failure(
            CaptureFailure(
                CaptureFailureKind.PROVIDER_LOST,
                "ACCESSIBILITY_SERVICE_LOST",
                "Accessibility service disconnected before capture.",
                descriptor.id,
                request.displayContext.displayId,
            )
        )
        val target = File(applicationContext.cacheDir, "capture/frame-${request.requestId}-${System.nanoTime()}.png")
        return try {
            val captured = service.takeScreenshotTo(target, request.displayContext.displayId.value)
            if (!captured || !target.isFile) {
                return ProviderCaptureResult.Failure(
                    CaptureFailure(
                        CaptureFailureKind.CAPTURE_FAILED,
                        "ACCESSIBILITY_CAPTURE_FAILED",
                        "Accessibility screenshot failed.",
                        descriptor.id,
                        request.displayContext.displayId,
                    )
                )
            }
            if (request.cancellation.isCancellationRequested()) return cancelled(request)
            val bytes = target.readBytes()
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return ProviderCaptureResult.Failure(
                    CaptureFailure(
                        CaptureFailureKind.INVALID_FRAME,
                        "ACCESSIBILITY_FRAME_INVALID",
                        "Accessibility screenshot dimensions are invalid.",
                        descriptor.id,
                        request.displayContext.displayId,
                    )
                )
            }
            val actualDisplay = displayManager?.getDisplay(request.displayContext.displayId.value)
                ?: return ProviderCaptureResult.Failure(
                    CaptureFailure(
                        CaptureFailureKind.PROVIDER_LOST,
                        "CAPTURE_DISPLAY_LOST",
                        "Requested display disappeared during capture.",
                        descriptor.id,
                        request.displayContext.displayId,
                    )
                )
            val actualContext = AndroidDisplayContextFactory.from(applicationContext.createDisplayContext(actualDisplay))
            ProviderCaptureResult.Success(
                CapturedFramePayload(
                    bytes = bytes,
                    metadata = CaptureMetadata(
                        requestedDisplayId = request.displayContext.displayId,
                        actualDisplayContext = actualContext,
                        capturedAtEpochMs = System.currentTimeMillis(),
                        capturedAtElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(),
                        widthPx = options.outWidth,
                        heightPx = options.outHeight,
                        rotation = actualContext.rotation,
                        densityDpi = actualContext.density.times(160f).toInt().coerceAtLeast(1),
                        source = CaptureSource.ACCESSIBILITY,
                        coordinateSpace = WindowCoordinateSpace.SCREENSHOT_PX,
                        captureBoundsDisplayPx = actualContext.availableBounds,
                    ),
                )
            )
        } finally {
            target.delete()
        }
    }

    private fun cancelled(request: CaptureRequest) = ProviderCaptureResult.Failure(
        CaptureFailure(
            CaptureFailureKind.CANCELLED,
            "ACCESSIBILITY_CAPTURE_CANCELLED",
            "Accessibility capture was cancelled.",
            descriptor.id,
            request.displayContext.displayId,
        )
    )
}

object CaptureFrameProcessService {
    @Volatile
    private var instance: CaptureFrameService? = null

    fun get(context: Context): CaptureFrameService = instance ?: synchronized(this) {
        instance ?: CaptureFrameService(
            providers = listOf(AccessibilityCaptureFrameProvider(context.applicationContext)),
        ).also { instance = it }
    }
}

suspend fun CaptureFrameService.captureToFile(request: CaptureRequest, target: File): CaptureResult {
    val result = capture(request)
    if (result is CaptureResult.Success) {
        try {
            target.parentFile?.mkdirs()
            target.writeBytes(result.lease.readBytes())
        } finally {
            result.lease.close()
        }
    }
    return result
}
