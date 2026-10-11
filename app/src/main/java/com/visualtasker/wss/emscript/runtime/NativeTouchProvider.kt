package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.accessibility.AccessibilityGesturePoint
import com.visualtasker.wss.accessibility.AccessibilityGestureResult
import com.visualtasker.wss.accessibility.AccessibilityGestureSegment
import com.visualtasker.wss.accessibility.AccessibilityGestureSubmission
import com.visualtasker.wss.accessibility.AccessibilityPointerPath
import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
import de.visualtasker.workflow.semantics.CommandCapability
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

enum class TouchGestureResult {
    Completed,
    Cancelled,
    Rejected,
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
    val maxGestureDurationMs: Long = DEFAULT_ANDROID_MAX_GESTURE_DURATION_MS,
    val maxPointerCount: Int = DEFAULT_ANDROID_MAX_POINTER_COUNT,
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
    val pointerId: Int = 0,
    val segments: List<TouchGestureSegment> = emptyList(),
    val startDelayMs: Long = 0L,
    val startHoldMs: Long = 0L,
    val endHoldMs: Long = 0L,
)

sealed interface TouchGestureSegment {
    val from: TouchGesturePoint
    val to: TouchGesturePoint
    val durationMs: Long

    data class Line(
        override val from: TouchGesturePoint,
        override val to: TouchGesturePoint,
        override val durationMs: Long,
    ) : TouchGestureSegment

    data class QuadraticBezier(
        override val from: TouchGesturePoint,
        val control: TouchGesturePoint,
        override val to: TouchGesturePoint,
        override val durationMs: Long,
    ) : TouchGestureSegment

    data class CubicBezier(
        override val from: TouchGesturePoint,
        val control1: TouchGesturePoint,
        val control2: TouchGesturePoint,
        override val to: TouchGesturePoint,
        override val durationMs: Long,
    ) : TouchGestureSegment
}

interface TouchGestureGateway {
    fun availability(): TouchAvailability

    fun dispatchGesture(
        path: TouchGesturePath,
        onResult: (TouchGestureResult) -> Unit,
    ): TouchGestureSubmission

    fun dispatchGesture(
        paths: List<TouchGesturePath>,
        onResult: (TouchGestureResult) -> Unit,
    ): TouchGestureSubmission = if (paths.size == 1) {
        dispatchGesture(paths.single(), onResult)
    } else {
        TouchGestureSubmission.Rejected
    }

    fun cancelActiveGesture() = Unit
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
            maxGestureDurationMs = service?.maxGestureDurationMs() ?: DEFAULT_ANDROID_MAX_GESTURE_DURATION_MS,
            maxPointerCount = service?.maxGesturePointerCount() ?: DEFAULT_ANDROID_MAX_POINTER_COUNT,
        )
    }

    override fun dispatchGesture(
        path: TouchGesturePath,
        onResult: (TouchGestureResult) -> Unit,
    ): TouchGestureSubmission {
        val service = serviceProvider() ?: return TouchGestureSubmission.Rejected
        val submission = if (path.segments.isEmpty()) {
            service.dispatchGesturePath(
                points = path.points.map { point -> point.toAccessibilityPoint() },
                durationMs = path.durationMs,
            ) { result ->
                onResult(
                    when (result) {
                        AccessibilityGestureResult.Completed -> TouchGestureResult.Completed
                        AccessibilityGestureResult.Cancelled -> TouchGestureResult.Cancelled
                        AccessibilityGestureResult.Rejected -> TouchGestureResult.Rejected
                    },
                )
            }
        } else {
            service.dispatchPointerPath(
                pointerPath = AccessibilityPointerPath(
                    pointerId = path.pointerId,
                    startPoint = path.segments.first().from.toAccessibilityPoint(),
                    segments = path.segments.map { segment -> segment.toAccessibilitySegment() },
                    startHoldMs = path.startHoldMs,
                    endHoldMs = path.endHoldMs,
                    delayMs = path.startDelayMs,
                ),
            ) { result ->
                onResult(
                    when (result) {
                        AccessibilityGestureResult.Completed -> TouchGestureResult.Completed
                        AccessibilityGestureResult.Cancelled -> TouchGestureResult.Cancelled
                        AccessibilityGestureResult.Rejected -> TouchGestureResult.Rejected
                    },
                )
            }
        }
        return when (submission) {
            AccessibilityGestureSubmission.Accepted -> TouchGestureSubmission.Accepted
            AccessibilityGestureSubmission.Busy -> TouchGestureSubmission.Busy
            AccessibilityGestureSubmission.Rejected -> TouchGestureSubmission.Rejected
        }
    }

    override fun dispatchGesture(
        paths: List<TouchGesturePath>,
        onResult: (TouchGestureResult) -> Unit,
    ): TouchGestureSubmission {
        if (paths.size == 1) return dispatchGesture(paths.single(), onResult)
        val service = serviceProvider() ?: return TouchGestureSubmission.Rejected
        val submission = service.dispatchMultiPointerGesture(
            paths.map { path ->
                AccessibilityPointerPath(
                    pointerId = path.pointerId,
                    startPoint = path.segments.first().from.toAccessibilityPoint(),
                    segments = path.segments.map { segment -> segment.toAccessibilitySegment() },
                    startHoldMs = path.startHoldMs,
                    endHoldMs = path.endHoldMs,
                    delayMs = path.startDelayMs,
                )
            },
        ) { result ->
            onResult(
                when (result) {
                    AccessibilityGestureResult.Completed -> TouchGestureResult.Completed
                    AccessibilityGestureResult.Cancelled -> TouchGestureResult.Cancelled
                    AccessibilityGestureResult.Rejected -> TouchGestureResult.Rejected
                },
            )
        }
        return when (submission) {
            AccessibilityGestureSubmission.Accepted -> TouchGestureSubmission.Accepted
            AccessibilityGestureSubmission.Busy -> TouchGestureSubmission.Busy
            AccessibilityGestureSubmission.Rejected -> TouchGestureSubmission.Rejected
        }
    }

    override fun cancelActiveGesture() {
        serviceProvider()?.cancelActiveGestureSequence()
    }

    private fun TouchGesturePoint.toAccessibilityPoint(): AccessibilityGesturePoint =
        AccessibilityGesturePoint(x, y)

    private fun TouchGestureSegment.toAccessibilitySegment(): AccessibilityGestureSegment = when (this) {
        is TouchGestureSegment.Line -> AccessibilityGestureSegment.Line(
            from.toAccessibilityPoint(),
            to.toAccessibilityPoint(),
            durationMs,
        )
        is TouchGestureSegment.QuadraticBezier -> AccessibilityGestureSegment.QuadraticBezier(
            from.toAccessibilityPoint(),
            control.toAccessibilityPoint(),
            to.toAccessibilityPoint(),
            durationMs,
        )
        is TouchGestureSegment.CubicBezier -> AccessibilityGestureSegment.CubicBezier(
            from.toAccessibilityPoint(),
            control1.toAccessibilityPoint(),
            control2.toAccessibilityPoint(),
            to.toAccessibilityPoint(),
            durationMs,
        )
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
    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        id = de.visualtasker.emscript.contract.ProviderId(PROVIDER_ID),
        displayName = "Android native touch",
        host = "AccessibilityService",
        capabilities = setOf(
            CapabilityDescriptor(
                id = A11Y_CAPABILITY.toCapabilityId(),
                runtimeCapability = A11Y_CAPABILITY,
                inputTypes = listOf(ProviderValueType.ANY),
                resultType = ProviderValueType.VOID,
                sideEffect = ProviderSideEffect.INPUT,
            ),
        ),
        requirements = ProviderRequirements(
            permissions = setOf("android.permission.BIND_ACCESSIBILITY_SERVICE"),
            requiresConnection = true,
        ),
        priority = 90,
    )

    private val sequenceInFlight = AtomicBoolean(false)

    override fun supports(request: CapabilityRequest): Boolean =
        request.invocation.capability == A11Y_CAPABILITY && request.invocation.commandId in SUPPORTED_COMMAND_IDS

    override fun availability(
        request: CapabilityRequest,
        context: ExecutionContext,
    ): ProviderAvailability {
        val availability = gateway.safeAvailability() ?: return ProviderAvailability(
            connection = ProviderConnectionState.UNKNOWN,
            permission = ProviderPermissionState.UNKNOWN,
            execution = ProviderExecutionState.UNKNOWN,
            diagnosticCode = "TOUCH_AVAILABILITY_FAILED",
            message = "Accessibility gesture availability could not be read.",
        )
        return when {
            !availability.connected -> ProviderAvailability(
                connection = ProviderConnectionState.DISCONNECTED,
                permission = ProviderPermissionState.UNKNOWN,
                execution = ProviderExecutionState.NOT_EXECUTABLE,
                diagnosticCode = "ACCESSIBILITY_SERVICE_UNAVAILABLE",
                message = "VisualTasker AccessibilityService is not connected.",
            )
            !availability.canPerformGestures -> ProviderAvailability(
                connection = ProviderConnectionState.CONNECTED,
                permission = ProviderPermissionState.GRANTED,
                capability = ProviderCapabilityState.UNAVAILABLE,
                execution = ProviderExecutionState.NOT_EXECUTABLE,
                diagnosticCode = "ACCESSIBILITY_GESTURES_UNAVAILABLE",
                message = "The connected AccessibilityService cannot perform gestures.",
            )
            availability.displayBounds?.isValid != true -> ProviderAvailability(
                connection = ProviderConnectionState.CONNECTED,
                permission = ProviderPermissionState.GRANTED,
                capability = ProviderCapabilityState.UNAVAILABLE,
                execution = ProviderExecutionState.NOT_EXECUTABLE,
                diagnosticCode = "TOUCH_DISPLAY_UNAVAILABLE",
                message = "No valid physical display bounds are available.",
            )
            else -> ProviderAvailability(
                connection = ProviderConnectionState.CONNECTED,
                permission = ProviderPermissionState.GRANTED,
            )
        }
    }

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
        val validated = when (val validation = request.validate()) {
            is GestureValidation.Success -> validation.gesture
            is GestureValidation.Failure -> return failure(validation.code, validation.message)
        }
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
        if (validated.paths.size > initialAvailability.maxPointerCount) {
            return failure(
                "TOUCH_POINTER_COUNT_ANDROID_LIMIT",
                "Gesture uses ${validated.paths.size} pointers; Android supports ${initialAvailability.maxPointerCount}.",
            )
        }
        validated.paths.firstNotNullOfOrNull { path ->
            path.androidDurationViolation(initialAvailability.maxGestureDurationMs)
        }?.let { message ->
            return failure("TOUCH_DURATION_ANDROID_LIMIT", message)
        }
        if (!sequenceInFlight.compareAndSet(false, true)) {
            return failure("TOUCH_GESTURE_BUSY", "Another native gesture sequence is still active.")
        }

        return try {
            repeat(validated.repeat) { iteration ->
                if (context.cancellationToken.isCancellationRequested) {
                    return ProviderResult.Cancelled("Gesture sequence cancelled before dispatch ${iteration + 1}.")
                }
                when (val result = awaitGesture(invocation, context, validated.paths, initialBounds)) {
                    is ProviderResult.Success -> Unit
                    else -> return result
                }
                if (iteration < validated.repeat - 1 && validated.interRepeatDelayMs > 0L) {
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
        paths: List<TouchGesturePath>,
        initialBounds: TouchDisplayBounds,
    ): ProviderResult {
        val latch = CountDownLatch(1)
        val completion = AtomicReference<TouchGestureResult?>(null)
        val callbackOpen = AtomicBoolean(true)
        val submission = try {
            gateway.dispatchGesture(paths) { result ->
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

        val effectiveTimeoutMs = maxOf(
            callbackTimeoutMs.coerceAtLeast(1L),
            paths.maxOf { it.expectedTimelineDurationMs() } + CALLBACK_TIMEOUT_GRACE_MS,
        )
        val deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(effectiveTimeoutMs)
        while (true) {
            if (context.cancellationToken.isCancellationRequested) {
                callbackOpen.set(false)
                gateway.cancelActiveGesture()
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
                TouchGestureResult.Rejected -> return failure(
                    "TOUCH_DISPATCH_REJECTED",
                    "Android rejected a continued PointerPath segment.",
                )
                null -> Unit
            }
            val availability = gateway.safeAvailability()
                ?: return failure("TOUCH_AVAILABILITY_FAILED", "Accessibility gesture availability could not be read during dispatch.")
            if (!availability.connected) {
                callbackOpen.set(false)
                gateway.cancelActiveGesture()
                return failure("ACCESSIBILITY_SERVICE_LOST", "AccessibilityService disconnected during gesture dispatch.")
            }
            if (availability.displayBounds != initialBounds) {
                callbackOpen.set(false)
                gateway.cancelActiveGesture()
                return failure("TOUCH_DISPLAY_CHANGED", "Physical display bounds changed during gesture dispatch.")
            }
            val remainingNs = deadlineNs - System.nanoTime()
            if (remainingNs <= 0L) {
                callbackOpen.set(false)
                gateway.cancelActiveGesture()
                return failure("TOUCH_GESTURE_TIMEOUT", "Gesture callback did not complete within $effectiveTimeoutMs ms.")
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
                gateway.cancelActiveGesture()
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
        "action.multiSwipe", "multiSwipe" -> GestureRequest.MultiSwipe(arguments)
        else -> null
    }

    private fun GestureRequest.validate(): GestureValidation = when (this) {
        is GestureRequest.Click -> {
            if (arguments.size !in 2..3) return validationFailure(
                "TOUCH_COORDINATES_REQUIRED",
                "clickPoint(x, y) requires two finite numeric pixel coordinates.",
            )
            val point = arguments.take(2).toClickPointOrNull() ?: return validationFailure(
                "TOUCH_COORDINATES_REQUIRED",
                "clickPoint(x, y) requires two finite numeric pixel coordinates.",
            )
            val repeat = arguments.getOrNull(2)?.positiveIntegerOrNull()
                ?: if (arguments.size == 2) 1 else return validationFailure(
                    "TOUCH_REPEAT_INVALID",
                    "clickPoint repeat must be a positive whole number.",
                )
            if (repeat != 1) return validationFailure(
                "TOUCH_REPEAT_UNSUPPORTED",
                "Coordinate click supports exactly one click per invocation.",
            )
            GestureValidation.Success(
                ValidatedGesture(
                    points = listOf(point),
                    repeat = 1,
                    paths = listOf(TouchGesturePath(listOf(point), CLICK_DURATION_MS)),
                    interRepeatDelayMs = 0L,
                ),
            )
        }
        is GestureRequest.Swipe -> {
            val typed = when (val first = arguments.firstOrNull()) {
                is ExecutionValue.PointerPathValue -> {
                    if (arguments.size != 1) return validationFailure(
                        "TOUCH_REPEAT_DUPLICATE",
                        "PointerPath already owns repeat timing; swipe must not add another repeat value.",
                    )
                    first.value
                }
                is ExecutionValue.PathValue -> {
                    if (arguments.size !in 1..2) return validationFailure(
                        "TOUCH_PATH_INVALID",
                        "swipe(Path, repeat?) accepts one Path and an optional repeat value.",
                    )
                    val repeat = arguments.getOrNull(1)?.positiveIntegerOrNull()
                        ?: if (arguments.size == 1) 1 else return validationFailure(
                            "TOUCH_REPEAT_INVALID",
                            "swipe Path repeat must be a positive whole number.",
                        )
                    PointerPath(
                        pointerId = 0,
                        geometry = first.value,
                        timing = PointerPathTiming(
                            durationMs = PointerPathSourceCodec.DEFAULT_DURATION_MS,
                            repeat = repeat,
                        ),
                    )
                }
                else -> null
            }
            if (typed != null) {
                return when (val resolved = PointerPathLowerer.resolve(typed)) {
                    is PointerPathResolutionResult.Failure -> validationFailure(resolved.code, resolved.message)
                    is PointerPathResolutionResult.Success -> GestureValidation.Success(resolved.pointerPath.toValidatedGesture())
                }
            }
            if (arguments.size !in 1..2) return validationFailure(
                "TOUCH_PATH_INVALID",
                "swipe requires an ordered list containing at least two finite pixel coordinate pairs.",
            )
            val points = arguments.firstOrNull()?.toTouchPoints().orEmpty()
            if (points.size < 2) return validationFailure(
                "TOUCH_PATH_INVALID",
                "swipe requires an ordered list containing at least two finite pixel coordinate pairs.",
            )
            val repeat = arguments.getOrNull(1)?.positiveIntegerOrNull()
                ?: if (arguments.size == 1) 1 else return validationFailure(
                    "TOUCH_REPEAT_INVALID",
                    "swipe repeat must be a positive whole number.",
                )
            GestureValidation.Success(
                ValidatedGesture(
                    points = points,
                    repeat = repeat,
                    paths = listOf(TouchGesturePath(points, LEGACY_SWIPE_DURATION_MS)),
                    interRepeatDelayMs = repeatDispatchIntervalMs,
                ),
            )
        }
        is GestureRequest.MultiSwipe -> {
            val gesture = (arguments.singleOrNull() as? ExecutionValue.MultiPathValue)?.value
                ?: return validationFailure(
                    "TOUCH_MULTIPATH_REQUIRED",
                    "multiSwipe requires exactly one MultiPath value.",
                )
            when (val resolved = MultiPointerGestureLowerer.resolve(gesture)) {
                is MultiPointerResolutionResult.Failure -> validationFailure(resolved.code, resolved.message)
                is MultiPointerResolutionResult.Success -> GestureValidation.Success(
                    ValidatedGesture(
                        points = resolved.gesture.pointers.flatMap { it.allTouchPoints() },
                        repeat = 1,
                        paths = resolved.gesture.pointers.map { it.toTouchGesturePath() },
                        interRepeatDelayMs = 0L,
                    ),
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
        data class MultiSwipe(override val arguments: List<ExecutionValue>) : GestureRequest
    }

    private data class ValidatedGesture(
        val points: List<TouchGesturePoint>,
        val repeat: Int,
        val paths: List<TouchGesturePath>,
        val interRepeatDelayMs: Long,
    )

    private sealed interface GestureValidation {
        data class Success(val gesture: ValidatedGesture) : GestureValidation
        data class Failure(val code: String, val message: String) : GestureValidation
    }

    private fun validationFailure(code: String, message: String): GestureValidation.Failure =
        GestureValidation.Failure(code, message)

    private fun ResolvedPointerPath.toValidatedGesture(): ValidatedGesture {
        val touchSegments = segments.map { segment ->
            when (val geometry = segment.geometry) {
                is PointerPathSegment.Line -> TouchGestureSegment.Line(
                    geometry.from.toTouchPoint(),
                    geometry.to.toTouchPoint(),
                    segment.durationMs,
                )
                is PointerPathSegment.QuadraticBezier -> TouchGestureSegment.QuadraticBezier(
                    geometry.from.toTouchPoint(),
                    geometry.control.toTouchPoint(),
                    geometry.to.toTouchPoint(),
                    segment.durationMs,
                )
                is PointerPathSegment.CubicBezier -> TouchGestureSegment.CubicBezier(
                    geometry.from.toTouchPoint(),
                    geometry.control1.toTouchPoint(),
                    geometry.control2.toTouchPoint(),
                    geometry.to.toTouchPoint(),
                    segment.durationMs,
                )
            }
        }
        val coordinates = buildList {
            add(startPoint.toTouchPoint())
            touchSegments.forEach { segment ->
                when (segment) {
                    is TouchGestureSegment.Line -> Unit
                    is TouchGestureSegment.QuadraticBezier -> add(segment.control)
                    is TouchGestureSegment.CubicBezier -> {
                        add(segment.control1)
                        add(segment.control2)
                    }
                }
                add(segment.to)
            }
        }
        return ValidatedGesture(
            points = coordinates,
            repeat = repeat,
            paths = listOf(TouchGesturePath(
                points = coordinates,
                durationMs = movementDurationMs,
                pointerId = pointerId,
                segments = touchSegments,
                startDelayMs = delayMs,
                startHoldMs = startHoldMs,
                endHoldMs = endHoldMs,
            )),
            interRepeatDelayMs = 0L,
        )
    }

    private fun ResolvedPointerPath.toTouchGesturePath(): TouchGesturePath =
        toValidatedGesture().paths.single()

    private fun ResolvedPointerPath.allTouchPoints(): List<TouchGesturePoint> =
        toValidatedGesture().points

    private fun PointerPathPoint.toTouchPoint(): TouchGesturePoint = TouchGesturePoint(x.toFloat(), y.toFloat())

    private fun TouchGesturePath.androidDurationViolation(maxDurationMs: Long): String? {
        val phaseDurations = if (segments.isEmpty()) {
            listOf(durationMs)
        } else {
            buildList {
                if (startHoldMs > 0L) add(startDelayMs + startHoldMs)
                segments.forEachIndexed { index, segment ->
                    add(segment.durationMs + if (index == 0 && startHoldMs == 0L) startDelayMs else 0L)
                }
                if (endHoldMs > 0L) add(endHoldMs)
            }
        }
        val invalid = phaseDurations.firstOrNull { it <= 0L || it > maxDurationMs }
        return invalid?.let {
            "PointerPath phase duration $it ms exceeds Android's per-gesture limit of $maxDurationMs ms."
        }
    }

    private fun TouchGesturePath.expectedTimelineDurationMs(): Long {
        val phaseDurations = if (segments.isEmpty()) {
            listOf(durationMs)
        } else {
            buildList {
                add(startDelayMs)
                add(startHoldMs)
                addAll(segments.map(TouchGestureSegment::durationMs))
                add(endHoldMs)
            }
        }
        return phaseDurations.fold(0L) { total, duration ->
            if (Long.MAX_VALUE - total < duration) Long.MAX_VALUE else total + duration
        }
    }

    companion object {
        const val PROVIDER_ID = "android.native.touch"
        const val CLICK_DURATION_MS = 1L
        const val LEGACY_SWIPE_DURATION_MS = 250L
        private const val CALLBACK_TIMEOUT_GRACE_MS = 5_000L
        val A11Y_CAPABILITY = ExecutionCapability(CommandCapability.A11Y.name)
        val SUPPORTED_COMMAND_IDS = setOf(
            "input.clickPoint",
            "clickPoint",
            "action.swipe",
            "swipe",
            "action.multiSwipe",
            "multiSwipe",
        )
        private val rawPointListRegex = Regex("^\\[\\s*(.*?)\\s*]$")
    }
}

private const val DEFAULT_ANDROID_MAX_GESTURE_DURATION_MS = 60_000L
private const val DEFAULT_ANDROID_MAX_POINTER_COUNT = 10

typealias NativeTouchProvider = NativeGestureProvider
