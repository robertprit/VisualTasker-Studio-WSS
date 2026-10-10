package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.accessibility.AccessibilityGestureResult
import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
import de.visualtasker.workflow.semantics.CommandCapability
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

enum class TouchGestureResult {
    Completed,
    Cancelled,
}

data class TouchAvailability(
    val connected: Boolean,
    val canPerformGestures: Boolean,
    val displayBounds: TouchDisplayBounds?,
)

data class TouchDisplayBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val isValid: Boolean
        get() = right > left && bottom > top

    fun containsPixel(x: Float, y: Float): Boolean =
        x >= left && x < right && y >= top && y < bottom
}

interface TouchGestureGateway {
    fun availability(): TouchAvailability

    fun dispatchClick(
        x: Float,
        y: Float,
        onResult: (TouchGestureResult) -> Unit,
    ): Boolean
}

class AccessibilityTouchGestureGateway(
    private val serviceProvider: () -> VisualTaskerAccessibilityService? = VisualTaskerAccessibilityService::current,
) : TouchGestureGateway {
    override fun availability(): TouchAvailability {
        val service = serviceProvider()
        return TouchAvailability(
            connected = service != null,
            canPerformGestures = service?.canPerformGestures() == true,
            displayBounds = service?.gestureDisplayBounds()?.let { bounds ->
                TouchDisplayBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
            },
        )
    }

    override fun dispatchClick(
        x: Float,
        y: Float,
        onResult: (TouchGestureResult) -> Unit,
    ): Boolean {
        val service = serviceProvider() ?: return false
        return service.dispatchClickGesture(x, y) { result ->
            onResult(
                when (result) {
                    AccessibilityGestureResult.Completed -> TouchGestureResult.Completed
                    AccessibilityGestureResult.Cancelled -> TouchGestureResult.Cancelled
                },
            )
        }
    }
}

class NativeTouchProvider(
    private val gateway: TouchGestureGateway = AccessibilityTouchGestureGateway(),
    private val callbackTimeoutMs: Long = 10_000L,
    private val cancellationPollMs: Long = 10L,
) : ExecutionProvider {
    override val id: String = PROVIDER_ID
    override val capabilities: Set<ExecutionCapability> = setOf(A11Y_CAPABILITY)

    private val gestureInFlight = AtomicBoolean(false)

    override fun invoke(
        invocation: ProviderInvocation,
        context: ExecutionContext,
    ): ProviderResult {
        if (invocation.commandId !in SUPPORTED_COMMAND_IDS) {
            return failure("TOUCH_COMMAND_UNSUPPORTED", "Native touch provider does not support '${invocation.commandId}'.")
        }
        if (context.cancellationToken.isCancellationRequested) {
            return ProviderResult.Cancelled("Touch cancelled before dispatch.")
        }
        val coordinates = invocation.validatedCoordinates() ?: return failure(
            "TOUCH_COORDINATES_REQUIRED",
            "clickPoint(x, y) requires two finite numeric pixel coordinates.",
        )
        val repeat = invocation.arguments.getOrNull(2)
        if (repeat != null && (repeat as? ExecutionValue.NumberValue)?.value != 1.0) {
            return failure("TOUCH_REPEAT_UNSUPPORTED", "M5-2 supports exactly one click per invocation.")
        }

        val availability = gateway.availability()
        if (!availability.connected) {
            return failure(
                "ACCESSIBILITY_SERVICE_UNAVAILABLE",
                "VisualTasker AccessibilityService is not connected. Enable it in Android accessibility settings.",
            )
        }
        if (!availability.canPerformGestures) {
            return failure(
                "ACCESSIBILITY_GESTURES_UNAVAILABLE",
                "The connected AccessibilityService cannot perform gestures.",
            )
        }
        val bounds = availability.displayBounds
        if (bounds == null || !bounds.isValid) {
            return failure("TOUCH_DISPLAY_UNAVAILABLE", "No valid physical display bounds are available.")
        }
        if (!bounds.containsPixel(coordinates.first, coordinates.second)) {
            return failure(
                "TOUCH_COORDINATES_OUT_OF_BOUNDS",
                "Pixel (${coordinates.first}, ${coordinates.second}) is outside display bounds $bounds.",
            )
        }
        if (!gestureInFlight.compareAndSet(false, true)) {
            return failure("TOUCH_GESTURE_BUSY", "Another native touch gesture is still active.")
        }

        return try {
            awaitGesture(invocation, context, coordinates.first, coordinates.second)
        } finally {
            gestureInFlight.set(false)
        }
    }

    private fun awaitGesture(
        invocation: ProviderInvocation,
        context: ExecutionContext,
        x: Float,
        y: Float,
    ): ProviderResult {
        val latch = CountDownLatch(1)
        val completion = AtomicReference<TouchGestureResult?>(null)
        val callbackOpen = AtomicBoolean(true)
        val accepted = try {
            gateway.dispatchClick(x, y) { result ->
                if (callbackOpen.compareAndSet(true, false)) {
                    completion.set(result)
                    latch.countDown()
                }
            }
        } catch (error: Throwable) {
            callbackOpen.set(false)
            return failure(
                "TOUCH_DISPATCH_FAILED",
                "Gesture dispatch failed for ${invocation.entityId}: ${error.message ?: error::class.java.simpleName}",
            )
        }
        if (!accepted) {
            callbackOpen.set(false)
            return failure("TOUCH_DISPATCH_REJECTED", "Android rejected the click gesture before execution.")
        }

        val deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(callbackTimeoutMs.coerceAtLeast(1L))
        while (true) {
            if (context.cancellationToken.isCancellationRequested) {
                callbackOpen.set(false)
                return ProviderResult.Cancelled(
                    "Touch cancellation requested after dispatch; physical delivery may already have occurred.",
                )
            }
            when (completion.get()) {
                TouchGestureResult.Completed -> return ProviderResult.Success()
                TouchGestureResult.Cancelled -> return failure(
                    "TOUCH_GESTURE_CANCELLED_BY_ANDROID",
                    "Android cancelled the dispatched click gesture.",
                )
                null -> Unit
            }
            val remainingNs = deadlineNs - System.nanoTime()
            if (remainingNs <= 0L) {
                callbackOpen.set(false)
                return failure("TOUCH_GESTURE_TIMEOUT", "Gesture callback did not complete within $callbackTimeoutMs ms.")
            }
            val waitMs = minOf(
                cancellationPollMs.coerceAtLeast(1L),
                TimeUnit.NANOSECONDS.toMillis(remainingNs).coerceAtLeast(1L),
            )
            try {
                latch.await(waitMs, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                callbackOpen.set(false)
                return ProviderResult.Cancelled("Touch provider worker was interrupted while awaiting the gesture callback.")
            }
        }
    }

    private fun ProviderInvocation.validatedCoordinates(): Pair<Float, Float>? {
        if (arguments.size !in 2..3) return null
        val x = (arguments[0] as? ExecutionValue.NumberValue)?.value ?: return null
        val y = (arguments[1] as? ExecutionValue.NumberValue)?.value ?: return null
        if (!x.isFinite() || !y.isFinite() || x !in -Float.MAX_VALUE.toDouble()..Float.MAX_VALUE.toDouble() || y !in -Float.MAX_VALUE.toDouble()..Float.MAX_VALUE.toDouble()) {
            return null
        }
        return x.toFloat() to y.toFloat()
    }

    private fun failure(code: String, message: String): ProviderResult.Failure =
        ProviderResult.Failure(code, message)

    companion object {
        const val PROVIDER_ID = "android.native.touch"
        val A11Y_CAPABILITY = ExecutionCapability(CommandCapability.A11Y.name)
        val SUPPORTED_COMMAND_IDS = setOf("input.clickPoint", "clickPoint")
    }
}
