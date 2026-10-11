package com.visualtasker.wss.capture

import com.visualtasker.wss.emscript.runtime.ProviderAvailability
import com.visualtasker.wss.emscript.runtime.ProviderCapabilityState
import com.visualtasker.wss.emscript.runtime.ProviderConnectionState
import com.visualtasker.wss.emscript.runtime.ProviderInstallationState
import com.visualtasker.wss.emscript.runtime.ProviderPermissionState
import com.visualtasker.wss.emscript.runtime.ProviderRegistrationState
import com.visualtasker.wss.emscript.runtime.ProviderSelectionPolicy
import com.visualtasker.wss.window.DisplayContext
import com.visualtasker.wss.window.DisplayId
import com.visualtasker.wss.window.WindowBounds
import com.visualtasker.wss.window.WindowCoordinateSpace
import de.visualtasker.emscript.contract.ProviderId

@JvmInline
value class CaptureFrameId(val value: String) {
    init {
        require(value.isNotBlank()) { "CaptureFrameId must not be blank" }
    }
}

enum class CaptureCapability {
    SCREENSHOT,
    DISPLAY_SELECTION,
    CANCELLATION,
    SHARED_FRAME,
}

enum class CaptureSource {
    ACCESSIBILITY,
    MEDIA_PROJECTION,
    STORED_ASSET,
    TEST,
}

enum class CaptureValidity {
    VALID,
    INVALID,
    RELEASED,
}

data class CaptureDataReference(
    val frameId: CaptureFrameId,
    val mimeType: String,
    val byteCount: Long,
)

data class CaptureMetadata(
    val requestedDisplayId: DisplayId,
    val actualDisplayContext: DisplayContext,
    val capturedAtEpochMs: Long,
    val capturedAtElapsedRealtimeNanos: Long,
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Int,
    val densityDpi: Int,
    val source: CaptureSource,
    val coordinateSpace: WindowCoordinateSpace = WindowCoordinateSpace.SCREENSHOT_PX,
    val captureBoundsDisplayPx: WindowBounds = actualDisplayContext.availableBounds,
) {
    init {
        require(widthPx > 0 && heightPx > 0) { "Capture dimensions must be positive" }
        require(rotation in setOf(0, 90, 180, 270)) { "Capture rotation must be normalized" }
        require(densityDpi > 0) { "Capture density must be positive" }
        require(captureBoundsDisplayPx.space == WindowCoordinateSpace.DISPLAY_PX) {
            "Capture bounds must use DISPLAY_PX"
        }
    }
}

data class CaptureFrame(
    val id: CaptureFrameId,
    val metadata: CaptureMetadata,
    val dataReference: CaptureDataReference,
    val validity: CaptureValidity = CaptureValidity.VALID,
)

fun interface CaptureCancellation {
    fun isCancellationRequested(): Boolean

    companion object {
        val None = CaptureCancellation { false }
    }
}

data class CaptureRequest(
    val requestId: String,
    val displayContext: DisplayContext,
    val cropDisplayPx: WindowBounds? = null,
    val timeoutMillis: Long = 5_000L,
    val reuseFrameId: CaptureFrameId? = null,
    val providerPolicy: ProviderSelectionPolicy = ProviderSelectionPolicy(),
    val cancellation: CaptureCancellation = CaptureCancellation.None,
) {
    init {
        require(requestId.isNotBlank()) { "Capture request id must not be blank" }
        require(timeoutMillis > 0L) { "Capture timeout must be positive" }
        require(cropDisplayPx == null || cropDisplayPx.space == WindowCoordinateSpace.DISPLAY_PX) {
            "Capture crop must use DISPLAY_PX"
        }
    }
}

enum class CaptureFailureKind {
    PROVIDER_UNAVAILABLE,
    CAPABILITY_UNSUPPORTED,
    PERMISSION_MISSING,
    DISPLAY_UNSUPPORTED,
    DISPLAY_MISMATCH,
    CANCELLED,
    TIMEOUT,
    PROVIDER_LOST,
    CAPTURE_FAILED,
    INVALID_FRAME,
    RESOURCE_LIMIT,
}

data class CaptureFailure(
    val kind: CaptureFailureKind,
    val code: String,
    val message: String,
    val providerId: ProviderId? = null,
    val requestedDisplayId: DisplayId? = null,
    val actualDisplayId: DisplayId? = null,
)

sealed interface CaptureResult {
    data class Success(
        val lease: CaptureFrameLease,
        val providerId: ProviderId,
        val reused: Boolean,
    ) : CaptureResult

    data class Failure(val failure: CaptureFailure) : CaptureResult
}

data class CaptureProviderDescriptor(
    val id: ProviderId,
    val displayName: String,
    val capabilities: Set<CaptureCapability>,
    val priority: Int = 0,
)

data class CapturedFramePayload(
    val bytes: ByteArray,
    val metadata: CaptureMetadata,
    val mimeType: String = "image/png",
)

sealed interface ProviderCaptureResult {
    data class Success(val payload: CapturedFramePayload) : ProviderCaptureResult
    data class Failure(val failure: CaptureFailure) : ProviderCaptureResult
}

interface CaptureFrameProvider {
    val descriptor: CaptureProviderDescriptor
    fun availability(request: CaptureRequest): ProviderAvailability
    suspend fun capture(request: CaptureRequest): ProviderCaptureResult
}

internal fun ProviderAvailability.toCaptureFailure(
    providerId: ProviderId,
    displayId: DisplayId,
): CaptureFailure = when {
    registration != ProviderRegistrationState.REGISTERED ||
        installation in setOf(ProviderInstallationState.NOT_INSTALLED, ProviderInstallationState.UNKNOWN) ||
        connection in setOf(ProviderConnectionState.DISCONNECTED, ProviderConnectionState.UNKNOWN) -> CaptureFailure(
            CaptureFailureKind.PROVIDER_UNAVAILABLE,
            diagnosticCode ?: "CAPTURE_PROVIDER_UNAVAILABLE",
            message ?: "Capture provider '${providerId.value}' is unavailable.",
            providerId,
            displayId,
        )

    permission == ProviderPermissionState.DENIED -> CaptureFailure(
        CaptureFailureKind.PERMISSION_MISSING,
        diagnosticCode ?: "CAPTURE_PERMISSION_MISSING",
        message ?: "Capture permission is missing.",
        providerId,
        displayId,
    )

    capability != ProviderCapabilityState.AVAILABLE -> CaptureFailure(
        CaptureFailureKind.DISPLAY_UNSUPPORTED,
        diagnosticCode ?: "CAPTURE_CAPABILITY_UNAVAILABLE",
        message ?: "Capture capability is unavailable for display ${displayId.value}.",
        providerId,
        displayId,
    )

    else -> CaptureFailure(
        CaptureFailureKind.PROVIDER_UNAVAILABLE,
        diagnosticCode ?: "CAPTURE_PROVIDER_NOT_EXECUTABLE",
        message ?: "Capture provider '${providerId.value}' is not executable.",
        providerId,
        displayId,
    )
}
