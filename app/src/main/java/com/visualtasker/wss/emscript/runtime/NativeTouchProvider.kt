package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.accessibility.AccessibilityGesturePoint
import com.visualtasker.wss.accessibility.AccessibilityGestureResult
import com.visualtasker.wss.accessibility.AccessibilityGestureSubmission
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

enum class TouchGestureSubmission {
    Accepted,
    Busy,
    Rejected,
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

    fun containsPixel(point: TouchGesturePoint): Boolean =
        point.x >= left && point.x < right && point.y >= top && point.y < bottom
}

data class TouchGesturePoint(
    val x: Float,
    val y: Float,
)

data class TouchGesturePath(
    val points: List<TouchGesturePoint>,
    val durationMs: Long,
)

interface TouchGestureGateway {
    fun availability(): TouchAvailability

    fun dispatchGesture(
        path: TouchGesturePath,
        onResult: (TouchGestureResult) -> Unit,
    ): TouchGestureSubmission
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

    override fun dispatchGesture(
        path: TouchGesturePath,
        onResult: (TouchGestureResult) -> Unit,
    ): TouchGestureSubmission {
        val service = serviceProvider() ?: return TouchGestureSubmission.Rejected
        return when (
            service.dispatchGesturePath(
                points = path.points.map { AccessibilityGesturePoint(it.x, it.y) },
                durationMs = path.durationMs,
            ) { result ->
                onResult(
                    when (result) {
                        AccessibilityGestureResult.Completed -> TouchGestureResult.Completed
                        AccessibilityGestureResult.Cancelled -> TouchGestureResult.Cancelled
                    },
                )
            }
        ) {
            AccessibilityGestureSubmission.Accepted -> TouchGestureSubmission.Accepted
            AccessibilityGestureSubmission.Busy -> TouchGestureSubmission.Busy
            AccessibilityGestureSubmission.Rejected -> TouchGestureSubmission.Rejected
        }
    }
}

class NativeGestureProvider(
    private val gateway: TouchGestureGateway = AccessibilityTouchGestureGateway(),
    private val callbackTimeoutMs: Long = 10_000L,
    private val cancellationPollMs: Long = 10L,
    private val androidCancellationCorrelationMs: Long = 500L,
    private val repeatDispatchIntervalMs: Long = 100L,
) : ExecutionProvider {
    override val id: String = PROVIDER_ID
    override val capabilities: Set<ExecutionCapability> = setOf(A11Y_CAPABILITY)

    private val sequenceInFlight = AtomicBoolean(false)

    override fun invoke(
        invocation: ProviderInvocation,
        context: ExecutionContext,
    ): ProviderResult {
        val request = invocation.toGestureRequest() ?: return failure(
            "TOUCH_COMMAND_UNSUPPORTED",
            "Native gesture provider does not support '${invocation.commandId}'.",
        )
        if (context.cancellationToken.isCancellationRequested) {
            return ProviderResult.Cancelled("Gesture cancelled before dispatch.")
        }
        val validated = request.validate() ?: return request.validationFailure()
        val initialAvailability = gateway.safeAvailability()
            ?: return failure("TOUCH_AVAILABILITY_FAILED", "Accessibility gesture availability could not be read.")
        availabilityFailure(initialAvailability)?.let { return it }
        val initialBounds = requireNotNull(initialAvailability.displayBounds)
        validated.points.firstOrNull { !initialBounds.containsPixel(it) }?.let { point ->
            return failure(
                "TOUCH_COORDINATES_OUT_OF_BOUNDS",
                "Pixel (${point.x}, ${point.y}) is outside display bounds $initialBounds.",
            )
        }
        if (!sequenceInFlight.compareAndSet(false, true)) {
            return failure("TOUCH_GESTURE_BUSY", "Another native gesture sequence is still active.")
        }

        return try {
            repeat(validated.repeat) { iteration ->
                if (context.cancellationToken.isCancellationRequested) {
                    return ProviderResult.Cancelled("Gesture sequence cancelled before dispatch ${iteration + 1}.")
                }
                when (val result = awaitGesture(invocation, context, validated.path, initialBounds)) {
                    is ProviderResult.Success -> Unit
                    else -> return result
                }
                if (iteration < validated.repeat - 1) {
                    awaitNextRepeat(context)?.let { return it }
                }
            }
            ProviderResult.Success()
        } finally {
            sequenceInFlight.set(false)
        }
    }

    private fun awaitGesture(
        invocation: ProviderInvocation,
        context: ExecutionContext,
        path: TouchGesturePath,
        initialBounds: TouchDisplayBounds,
    ): ProviderResult {
        val latch = CountDownLatch(1)
        val completion = AtomicReference<TouchGestureResult?>(null)
        val callbackOpen = AtomicBoolean(true)
        val submission = try {
            gateway.dispatchGesture(path) { result ->
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
        when (submission) {
            TouchGestureSubmission.Accepted -> Unit
            TouchGestureSubmission.Busy -> {
                callbackOpen.set(false)
                return failure("TOUCH_GESTURE_BUSY", "The AccessibilityService is still executing another gesture.")
            }
            TouchGestureSubmission.Rejected -> {
                callbackOpen.set(false)
                return failure("TOUCH_DISPATCH_REJECTED", "Android rejected the gesture before execution.")
            }
        }

        val deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(callbackTimeoutMs.coerceAtLeast(1L))
        while (true) {
            if (context.cancellationToken.isCancellationRequested) {
                callbackOpen.set(false)
                return ProviderResult.Cancelled(
                    "Gesture cancellation requested after dispatch; physical delivery may already have occurred.",
                )
            }
            when (completion.get()) {
                TouchGestureResult.Completed -> {
                    val completedAvailability = gateway.safeAvailability()
                        ?: return failure(
                            "TOUCH_AVAILABILITY_FAILED",
                            "Accessibility gesture availability could not be read after dispatch.",
                        )
                    if (!completedAvailability.connected) {
                        return failure("ACCESSIBILITY_SERVICE_LOST", "AccessibilityService disconnected during gesture dispatch.")
                    }
                    if (completedAvailability.displayBounds != initialBounds) {
                        return failure("TOUCH_DISPLAY_CHANGED", "Physical display bounds changed during gesture dispatch.")
                    }
                    return ProviderResult.Success()
                }
                TouchGestureResult.Cancelled -> return correlateAndroidCancellation(context)
                null -> Unit
            }
            val availability = gateway.safeAvailability()
                ?: return failure("TOUCH_AVAILABILITY_FAILED", "Accessibility gesture availability could not be read during dispatch.")
            if (!availability.connected) {
                callbackOpen.set(false)
                return failure("ACCESSIBILITY_SERVICE_LOST", "AccessibilityService disconnected during gesture dispatch.")
            }
            if (availability.displayBounds != initialBounds) {
                callbackOpen.set(false)
                return failure("TOUCH_DISPLAY_CHANGED", "Physical display bounds changed during gesture dispatch.")
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
                return ProviderResult.Cancelled("Gesture provider worker was interrupted while awaiting the Android callback.")
            }
        }
    }

    private fun awaitNextRepeat(context: ExecutionContext): ProviderResult.Cancelled? {
        if (context.cancellationToken.isCancellationRequested) {
            return ProviderResult.Cancelled("Gesture sequence cancelled between repeated dispatches.")
        }
        return try {
            Thread.sleep(repeatDispatchIntervalMs.coerceAtLeast(1L))
            if (context.cancellationToken.isCancellationRequested) {
                ProviderResult.Cancelled("Gesture sequence cancelled between repeated dispatches.")
            } else {
                null
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            ProviderResult.Cancelled("Gesture provider worker was interrupted between repeated dispatches.")
        }
    }

    private fun correlateAndroidCancellation(context: ExecutionContext): ProviderResult {
        val deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(
            androidCancellationCorrelationMs.coerceAtLeast(0L),
        )
        while (true) {
            if (context.cancellationToken.isCancellationRequested) {
                return ProviderResult.Cancelled(
                    "Runtime cancellation coincided with Android gesture cancellation; physical delivery may be partial.",
                )
            }
            val remainingNs = deadlineNs - System.nanoTime()
            if (remainingNs <= 0L) break
            val waitMs = minOf(
                cancellationPollMs.coerceAtLeast(1L),
                TimeUnit.NANOSECONDS.toMillis(remainingNs).coerceAtLeast(1L),
            )
            try {
                Thread.sleep(waitMs)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return ProviderResult.Cancelled("Gesture provider worker was interrupted after Android cancellation.")
            }
        }
        return ProviderResult.Cancelled(
            "Android cancelled the dispatched gesture; physical delivery may be partial.",
        )
    }

    private fun ProviderInvocation.toGestureRequest(): GestureRequest? = when (commandId) {
        "input.clickPoint", "clickPoint" -> GestureRequest.Click(arguments)
        "action.swipe", "swipe" -> GestureRequest.Swipe(arguments)
        else -> null
    }

    private fun GestureRequest.validate(): ValidatedGesture? = when (this) {
        is GestureRequest.Click -> {
            if (arguments.size !in 2..3) return null
            val point = arguments.take(2).toClickPointOrNull() ?: return null
            val repeat = arguments.getOrNull(2)?.positiveIntegerOrNull()
                ?: if (arguments.size == 2) 1 else return null
            if (repeat != 1) return null
            ValidatedGesture(
                points = listOf(point),
                repeat = 1,
                path = TouchGesturePath(listOf(point), CLICK_DURATION_MS),
            )
        }
        is GestureRequest.Swipe -> {
            if (arguments.size !in 1..2) return null
            val points = arguments.firstOrNull()?.toTouchPoints().orEmpty()
            if (points.size < 2) return null
            val repeat = arguments.getOrNull(1)?.positiveIntegerOrNull()
                ?: if (arguments.size == 1) 1 else return null
            ValidatedGesture(
                points = points,
                repeat = repeat,
                path = TouchGesturePath(points, LEGACY_SWIPE_DURATION_MS),
            )
        }
    }

    private fun GestureRequest.validationFailure(): ProviderResult.Failure = when (this) {
        is GestureRequest.Click -> {
            val repeat = arguments.getOrNull(2)?.positiveIntegerOrNull()
            if (repeat != null && repeat != 1) {
                failure("TOUCH_REPEAT_UNSUPPORTED", "Coordinate click supports exactly one click per invocation.")
            } else {
                failure("TOUCH_COORDINATES_REQUIRED", "clickPoint(x, y) requires two finite numeric pixel coordinates.")
            }
        }
        is GestureRequest.Swipe -> {
            val repeat = arguments.getOrNull(1)
            if (repeat != null && repeat.positiveIntegerOrNull() == null) {
                failure("TOUCH_REPEAT_INVALID", "swipe repeat must be a positive whole number.")
            } else {
                failure(
                    "TOUCH_PATH_INVALID",
                    "swipe requires an ordered list containing at least two finite pixel coordinate pairs.",
                )
            }
        }
    }

    private fun availabilityFailure(availability: TouchAvailability): ProviderResult.Failure? = when {
        !availability.connected -> failure(
            "ACCESSIBILITY_SERVICE_UNAVAILABLE",
            "VisualTasker AccessibilityService is not connected. Enable it in Android accessibility settings.",
        )
        !availability.canPerformGestures -> failure(
            "ACCESSIBILITY_GESTURES_UNAVAILABLE",
            "The connected AccessibilityService cannot perform gestures.",
        )
        availability.displayBounds?.isValid != true -> failure(
            "TOUCH_DISPLAY_UNAVAILABLE",
            "No valid physical display bounds are available.",
        )
        else -> null
    }

    private fun TouchGestureGateway.safeAvailability(): TouchAvailability? = runCatching(::availability).getOrNull()

    private fun List<ExecutionValue>.toClickPointOrNull(): TouchGesturePoint? {
        if (size != 2) return null
        val x = (get(0) as? ExecutionValue.NumberValue)?.value ?: return null
        val y = (get(1) as? ExecutionValue.NumberValue)?.value ?: return null
        return listOf(x, y).toPointPairs().singleOrNull()
    }

    private fun ExecutionValue.toTouchPoints(): List<TouchGesturePoint> {
        val numbers = when (this) {
            is ExecutionValue.ListValue -> {
                values.mapNotNull { (it as? ExecutionValue.NumberValue)?.value }
                    .takeIf { it.size == values.size }
            }
            is ExecutionValue.StringValue -> rawPointListRegex.matchEntire(value.trim())
                ?.groupValues
                ?.get(1)
                ?.split(',')
                ?.map { it.trim() }
                ?.map(String::toDoubleOrNull)
                ?.takeIf { values -> values.all { it != null } }
                ?.filterNotNull()
            else -> null
        } ?: return emptyList()
        return numbers.toPointPairs()
    }

    private fun List<Double>.toPointPairs(): List<TouchGesturePoint> {
        if (size < 2 || size % 2 != 0 || any { !it.isFinite() || it !in -Float.MAX_VALUE.toDouble()..Float.MAX_VALUE.toDouble() }) {
            return emptyList()
        }
        return chunked(2).map { pair -> TouchGesturePoint(pair[0].toFloat(), pair[1].toFloat()) }
    }

    private fun ExecutionValue.positiveIntegerOrNull(): Int? {
        val number = when (this) {
            is ExecutionValue.NumberValue -> value
            is ExecutionValue.StringValue -> value.trim().toDoubleOrNull()
            else -> null
        } ?: return null
        if (!number.isFinite() || number < 1.0 || number > Int.MAX_VALUE || number % 1.0 != 0.0) return null
        return number.toInt()
    }

    private fun failure(code: String, message: String): ProviderResult.Failure =
        ProviderResult.Failure(code, message)

    private sealed interface GestureRequest {
        val arguments: List<ExecutionValue>

        data class Click(override val arguments: List<ExecutionValue>) : GestureRequest
        data class Swipe(override val arguments: List<ExecutionValue>) : GestureRequest
    }

    private data class ValidatedGesture(
        val points: List<TouchGesturePoint>,
        val repeat: Int,
        val path: TouchGesturePath,
    )

    companion object {
        const val PROVIDER_ID = "android.native.touch"
        const val CLICK_DURATION_MS = 1L
        const val LEGACY_SWIPE_DURATION_MS = 250L
        val A11Y_CAPABILITY = ExecutionCapability(CommandCapability.A11Y.name)
        val SUPPORTED_COMMAND_IDS = setOf("input.clickPoint", "clickPoint", "action.swipe", "swipe")
        private val rawPointListRegex = Regex("^\\[\\s*(.*?)\\s*]$")
    }
}

typealias NativeTouchProvider = NativeGestureProvider
