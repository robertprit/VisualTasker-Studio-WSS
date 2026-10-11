package com.visualtasker.wss.emscript.runtime

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.pow

data class PointerPathPoint(
    val x: Double,
    val y: Double,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "PointerPath coordinates must be finite." }
    }
}

sealed interface PointerPathSegment {
    val from: PointerPathPoint
    val to: PointerPathPoint

    data class Line(
        override val from: PointerPathPoint,
        override val to: PointerPathPoint,
    ) : PointerPathSegment

    data class QuadraticBezier(
        override val from: PointerPathPoint,
        val control: PointerPathPoint,
        override val to: PointerPathPoint,
    ) : PointerPathSegment

    data class CubicBezier(
        override val from: PointerPathPoint,
        val control1: PointerPathPoint,
        val control2: PointerPathPoint,
        override val to: PointerPathPoint,
    ) : PointerPathSegment
}

data class PointerPathGeometry(
    val startPoint: PointerPathPoint,
    val segments: List<PointerPathSegment>,
) {
    init {
        require(segments.isNotEmpty()) { "PointerPath requires at least one segment." }
    }

    val allPoints: List<PointerPathPoint>
        get() = buildList {
            add(startPoint)
            segments.forEach { segment ->
                when (segment) {
                    is PointerPathSegment.Line -> Unit
                    is PointerPathSegment.QuadraticBezier -> add(segment.control)
                    is PointerPathSegment.CubicBezier -> {
                        add(segment.control1)
                        add(segment.control2)
                    }
                }
                add(segment.to)
            }
        }
}

data class PointerPathTiming(
    val durationMs: Long? = null,
    val segmentDurationsMs: List<Long>? = null,
    val speedPixelsPerSecond: Double? = null,
    val startHoldMs: Long = 0L,
    val endHoldMs: Long = 0L,
    val delayMs: Long = 0L,
    val repeat: Int = 1,
) {
    init {
        require(listOf(durationMs, segmentDurationsMs, speedPixelsPerSecond).count { it != null } == 1) {
            "PointerPath timing requires exactly one of duration, segmentDurations or speed."
        }
    }
}

data class PointerPath(
    val pointerId: Int,
    val geometry: PointerPathGeometry,
    val timing: PointerPathTiming,
    val pointerIdExplicit: Boolean = false,
)

data class MultiPointerGesture(
    val pointers: List<PointerPath>,
) {
    init {
        require(pointers.size >= 2) { "MultiPath requires at least two PointerPath values." }
        require(pointers.map(PointerPath::pointerId).distinct().size == pointers.size) {
            "MultiPath pointer IDs must be unique."
        }
    }
}

data class ResolvedPointerPathSegment(
    val geometry: PointerPathSegment,
    val durationMs: Long,
)

data class ResolvedPointerPath(
    val pointerId: Int,
    val startPoint: PointerPathPoint,
    val segments: List<ResolvedPointerPathSegment>,
    val startHoldMs: Long,
    val endHoldMs: Long,
    val delayMs: Long,
    val repeat: Int,
) {
    val movementDurationMs: Long = segments.sumOf(ResolvedPointerPathSegment::durationMs)
    val totalDispatchDurationMs: Long = delayMs + startHoldMs + movementDurationMs + endHoldMs
}

data class ResolvedMultiPointerGesture(
    val pointers: List<ResolvedPointerPath>,
) {
    val plannedTotalDurationMs: Long = pointers.maxOf(ResolvedPointerPath::totalDispatchDurationMs)
}

sealed interface PointerPathParseResult {
    data class Success(
        val pointerPath: PointerPath,
        val legacySyntax: Boolean,
    ) : PointerPathParseResult

    data class Failure(
        val code: String,
        val message: String,
    ) : PointerPathParseResult
}

sealed interface PointerPathResolutionResult {
    data class Success(val pointerPath: ResolvedPointerPath) : PointerPathResolutionResult
    data class Failure(val code: String, val message: String) : PointerPathResolutionResult
}

sealed interface MultiPointerResolutionResult {
    data class Success(val gesture: ResolvedMultiPointerGesture) : MultiPointerResolutionResult
    data class Failure(val code: String, val message: String) : MultiPointerResolutionResult
}

object MultiPointerGestureLowerer {
    fun resolve(gesture: MultiPointerGesture): MultiPointerResolutionResult {
        val resolved = mutableListOf<ResolvedPointerPath>()
        gesture.pointers.forEachIndexed { index, pointer ->
            if (pointer.timing.repeat != 1) {
                return failure(
                    "TOUCH_MULTIPATH_REPEAT_AMBIGUOUS",
                    "MultiPath pointer $index must use repeat=1; repeat the complete gesture instead.",
                )
            }
            when (val result = PointerPathLowerer.resolve(pointer)) {
                is PointerPathResolutionResult.Failure -> return failure(
                    result.code,
                    "MultiPath pointer $index (${pointer.pointerId}): ${result.message}",
                )
                is PointerPathResolutionResult.Success -> resolved += result.pointerPath
            }
        }
        return MultiPointerResolutionResult.Success(ResolvedMultiPointerGesture(resolved))
    }

    private fun failure(code: String, message: String) = MultiPointerResolutionResult.Failure(code, message)
}

object PointerPathLowerer {
    fun resolve(pointerPath: PointerPath): PointerPathResolutionResult {
        val geometry = pointerPath.geometry
        if (pointerPath.pointerId < 0) {
            return failure("TOUCH_POINTER_ID_INVALID", "PointerPath pointerId must not be negative.")
        }
        if (geometry.startPoint.x < 0.0 || geometry.startPoint.y < 0.0 ||
            geometry.allPoints.any { it.x < 0.0 || it.y < 0.0 }
        ) {
            return failure("TOUCH_COORDINATES_INVALID", "PointerPath coordinates must not be negative.")
        }
        var previous = geometry.startPoint
        val lengths = mutableListOf<Double>()
        geometry.segments.forEachIndexed { index, segment ->
            if (!segment.from.samePosition(previous)) {
                return failure(
                    "TOUCH_PATH_DISCONTINUOUS",
                    "Segment $index starts at ${segment.from} instead of previous endpoint $previous.",
                )
            }
            val length = segment.approximateLength()
            if (!length.isFinite() || length <= 0.0) {
                return failure("TOUCH_PATH_ZERO_LENGTH", "Segment $index must have positive geometric length.")
            }
            lengths += length
            previous = segment.to
        }
        val timing = pointerPath.timing
        if (timing.durationMs != null && timing.durationMs <= 0L) {
            return failure("TOUCH_TIMING_INVALID", "PointerPath duration must be positive.")
        }
        if (timing.segmentDurationsMs != null &&
            (timing.segmentDurationsMs.size != geometry.segments.size || timing.segmentDurationsMs.any { it <= 0L })
        ) {
            return failure(
                "TOUCH_SEGMENT_TIMING_INVALID",
                "PointerPath segmentDurations must contain one positive duration per segment.",
            )
        }
        if (timing.speedPixelsPerSecond != null &&
            (!timing.speedPixelsPerSecond.isFinite() || timing.speedPixelsPerSecond <= 0.0)
        ) {
            return failure("TOUCH_SPEED_INVALID", "PointerPath speed must be a positive finite pixel/second value.")
        }
        if (timing.startHoldMs < 0L || timing.endHoldMs < 0L || timing.delayMs < 0L) {
            return failure("TOUCH_TIMING_INVALID", "PointerPath hold and delay values must not be negative.")
        }
        if (timing.repeat < 1) {
            return failure("TOUCH_REPEAT_INVALID", "PointerPath repeat must be a positive whole number.")
        }

        val durations = when {
            timing.segmentDurationsMs != null -> timing.segmentDurationsMs
            timing.durationMs != null -> distributeDuration(timing.durationMs, lengths)
                ?: return failure(
                    "TOUCH_TIMING_INVALID",
                    "PointerPath duration ${timing.durationMs} ms is shorter than its ${lengths.size} segments.",
                )
            else -> lengths.map { length ->
                ceil(length / requireNotNull(timing.speedPixelsPerSecond) * 1_000.0)
                    .toLong()
                    .coerceAtLeast(1L)
            }
        }
        return PointerPathResolutionResult.Success(
            ResolvedPointerPath(
                pointerId = pointerPath.pointerId,
                startPoint = geometry.startPoint,
                segments = geometry.segments.zip(durations) { segment, duration ->
                    ResolvedPointerPathSegment(segment, duration)
                },
                startHoldMs = timing.startHoldMs,
                endHoldMs = timing.endHoldMs,
                delayMs = timing.delayMs,
                repeat = timing.repeat,
            ),
        )
    }

    private fun distributeDuration(totalDurationMs: Long, lengths: List<Double>): List<Long>? {
        if (totalDurationMs < lengths.size) return null
        val totalLength = lengths.sum()
        var remainingDuration = totalDurationMs
        var remainingLength = totalLength
        return lengths.mapIndexed { index, length ->
            if (index == lengths.lastIndex) {
                remainingDuration
            } else {
                val remainingSegments = lengths.lastIndex - index
                val share = (remainingDuration * (length / remainingLength)).toLong()
                    .coerceIn(1L, remainingDuration - remainingSegments)
                remainingDuration -= share
                remainingLength -= length
                share
            }
        }
    }

    private fun PointerPathSegment.approximateLength(): Double = when (this) {
        is PointerPathSegment.Line -> from.distanceTo(to)
        is PointerPathSegment.QuadraticBezier -> sampledLength(CURVE_SAMPLES) { t ->
            val inverse = 1.0 - t
            PointerPathPoint(
                x = inverse.pow(2) * from.x + 2.0 * inverse * t * control.x + t.pow(2) * to.x,
                y = inverse.pow(2) * from.y + 2.0 * inverse * t * control.y + t.pow(2) * to.y,
            )
        }
        is PointerPathSegment.CubicBezier -> sampledLength(CURVE_SAMPLES) { t ->
            val inverse = 1.0 - t
            PointerPathPoint(
                x = inverse.pow(3) * from.x + 3.0 * inverse.pow(2) * t * control1.x +
                    3.0 * inverse * t.pow(2) * control2.x + t.pow(3) * to.x,
                y = inverse.pow(3) * from.y + 3.0 * inverse.pow(2) * t * control1.y +
                    3.0 * inverse * t.pow(2) * control2.y + t.pow(3) * to.y,
            )
        }
    }

    private inline fun PointerPathSegment.sampledLength(
        samples: Int,
        pointAt: (Double) -> PointerPathPoint,
    ): Double {
        var previous = from
        var length = 0.0
        for (index in 1..samples) {
            val current = pointAt(index.toDouble() / samples)
            length += previous.distanceTo(current)
            previous = current
        }
        return length
    }

    private fun PointerPathPoint.distanceTo(other: PointerPathPoint): Double = hypot(other.x - x, other.y - y)

    private fun PointerPathPoint.samePosition(other: PointerPathPoint): Boolean =
        kotlin.math.abs(x - other.x) <= POSITION_EPSILON && kotlin.math.abs(y - other.y) <= POSITION_EPSILON

    private fun failure(code: String, message: String) = PointerPathResolutionResult.Failure(code, message)

    private const val CURVE_SAMPLES = 32
    private const val POSITION_EPSILON = 0.0001
}

sealed interface PointerPathConstructionResult {
    data class Success(val value: ExecutionValue) : PointerPathConstructionResult
    data class Failure(
        val code: String,
        val message: String,
        val invalidSegmentIndex: Int? = null,
    ) : PointerPathConstructionResult
}

object PointerPathRuntimeConstructors {
    fun evaluate(
        commandId: String,
        arguments: List<ExecutionValue>,
        argumentNames: List<String?> = emptyList(),
    ): PointerPathConstructionResult = try {
        val value = when (commandId) {
            "geometry.point" -> {
                val bound = bind(arguments, argumentNames, listOf("x", "y"))
                ExecutionValue.PointValue(PointerPathPoint(bound.number("x"), bound.number("y")))
            }
            "geometry.lineSegment" -> {
                val bound = bind(arguments, argumentNames, listOf("from", "to"))
                ExecutionValue.PathSegmentValue(PointerPathSegment.Line(bound.point("from"), bound.point("to")))
            }
            "geometry.quadraticBezierSegment" -> {
                val bound = bind(arguments, argumentNames, listOf("from", "control", "to"))
                ExecutionValue.PathSegmentValue(
                    PointerPathSegment.QuadraticBezier(
                        bound.point("from"),
                        bound.point("control"),
                        bound.point("to"),
                    ),
                )
            }
            "geometry.cubicBezierSegment" -> {
                val bound = bind(arguments, argumentNames, listOf("from", "control1", "control2", "to"))
                ExecutionValue.PathSegmentValue(
                    PointerPathSegment.CubicBezier(
                        bound.point("from"),
                        bound.point("control1"),
                        bound.point("control2"),
                        bound.point("to"),
                    ),
                )
            }
            "geometry.path" -> ExecutionValue.PathValue(path(arguments))
            "gesture.pointerPath" -> pointerPath(arguments, argumentNames)
            "gesture.multiPath" -> multiPath(arguments)
            else -> return failure("PATH_CONSTRUCTOR_UNSUPPORTED", "Unsupported path constructor '$commandId'.")
        }
        PointerPathConstructionResult.Success(value)
    } catch (error: PathBindingException) {
        failure(error.code, error.message ?: error.code, error.invalidSegmentIndex)
    } catch (error: IllegalArgumentException) {
        failure("PATH_VALUE_INVALID", error.message ?: "Invalid path value.")
    }

    private fun path(arguments: List<ExecutionValue>): PointerPathGeometry {
        val values = arguments.singleOrNull()
            ?.let { (it as? ExecutionValue.ListValue)?.values }
            ?: arguments
        if (values.all { it is ExecutionValue.PointValue }) {
            val points = values.map { (it as ExecutionValue.PointValue).value }
            if (points.size < 2) throw PathBindingException("PATH_POINTS_REQUIRED", "Path requires at least two Point values.")
            return PointerPathGeometry(
                startPoint = points.first(),
                segments = points.zipWithNext { from, to -> PointerPathSegment.Line(from, to) },
            )
        }
        if (values.all { it is ExecutionValue.PathSegmentValue }) {
            val segments = values.map { (it as ExecutionValue.PathSegmentValue).value }
            if (segments.isEmpty()) throw PathBindingException("PATH_SEGMENTS_REQUIRED", "Path requires at least one segment.")
            return PointerPathGeometry(segments.first().from, segments)
        }
        throw PathBindingException(
            "PATH_ELEMENT_TYPE_INVALID",
            "Path values must contain only Point values or only PathSegment values.",
        )
    }

    private fun pointerPath(
        arguments: List<ExecutionValue>,
        argumentNames: List<String?>,
    ): ExecutionValue.PointerPathValue {
        val bound = bind(
            arguments,
            argumentNames,
            listOf("path", "duration", "segmentDurations", "speed", "startHold", "endHold", "delay", "repeat", "pointerId"),
        )
        val geometry = (bound.required("path") as? ExecutionValue.PathValue)?.value
            ?: throw PathBindingException("PATH_REQUIRED", "pointerPath.path must be a Path value.")
        val duration = bound.optionalWholeLong("duration", positive = true)
        val segmentDurations = bound["segmentDurations"]?.let { value ->
            val values = (value as? ExecutionValue.ListValue)?.values
                ?: throw PathBindingException("PATH_TIMING_TYPE_INVALID", "segmentDurations must be a List<Number>.")
            if (values.isEmpty()) throw PathBindingException("PATH_TIMING_INVALID", "segmentDurations must not be empty.")
            values.mapIndexed { index, item -> item.wholeLong("segmentDurations[$index]", positive = true) }
        }
        val speed = bound.optionalNumber("speed")?.also {
            if (it <= 0.0) throw PathBindingException("TOUCH_SPEED_INVALID", "speed must be positive.")
        }
        if (listOf(duration, segmentDurations, speed).count { it != null } != 1) {
            throw PathBindingException(
                "TOUCH_TIMING_REQUIRED",
                "pointerPath requires exactly one of duration, segmentDurations or speed.",
            )
        }
        return ExecutionValue.PointerPathValue(
            PointerPath(
                pointerId = bound.optionalWholeLong("pointerId", positive = false)?.toIntChecked("pointerId") ?: 0,
                geometry = geometry,
                timing = PointerPathTiming(
                    durationMs = duration,
                    segmentDurationsMs = segmentDurations,
                    speedPixelsPerSecond = speed,
                    startHoldMs = bound.optionalWholeLong("startHold", positive = false) ?: 0L,
                    endHoldMs = bound.optionalWholeLong("endHold", positive = false) ?: 0L,
                    delayMs = bound.optionalWholeLong("delay", positive = false) ?: 0L,
                    repeat = bound.optionalWholeLong("repeat", positive = true)?.toIntChecked("repeat") ?: 1,
                ),
                pointerIdExplicit = "pointerId" in bound,
            ),
        )
    }

    private fun multiPath(arguments: List<ExecutionValue>): ExecutionValue.MultiPathValue {
        val values = arguments.singleOrNull()
            ?.let { (it as? ExecutionValue.ListValue)?.values }
            ?: arguments
        if (values.size < 2 || values.any { it !is ExecutionValue.PointerPathValue }) {
            throw PathBindingException(
                "TOUCH_MULTIPATH_VALUES_REQUIRED",
                "multiPath requires at least two PointerPath values.",
            )
        }
        val pointers = values.map { (it as ExecutionValue.PointerPathValue).value }
        val explicitIds = pointers.filter(PointerPath::pointerIdExplicit).map(PointerPath::pointerId)
        if (explicitIds.distinct().size != explicitIds.size) {
            throw PathBindingException("TOUCH_POINTER_ID_DUPLICATE", "Explicit MultiPath pointer IDs must be unique.")
        }
        val usedIds = explicitIds.toMutableSet()
        var candidate = 0
        val normalized = pointers.map { pointer ->
            if (pointer.pointerIdExplicit) {
                pointer
            } else {
                while (candidate in usedIds) candidate += 1
                pointer.copy(pointerId = candidate.also(usedIds::add))
            }
        }
        return ExecutionValue.MultiPathValue(MultiPointerGesture(normalized))
    }

    private fun bind(
        arguments: List<ExecutionValue>,
        names: List<String?>,
        expectedNames: List<String>,
    ): Map<String, ExecutionValue> {
        val result = linkedMapOf<String, ExecutionValue>()
        var positionalIndex = 0
        arguments.forEachIndexed { index, value ->
            val suppliedName = names.getOrNull(index)
            val canonicalName = if (suppliedName == null) {
                expectedNames.getOrNull(positionalIndex++)
                    ?: throw PathBindingException("PATH_ARGUMENT_COUNT_INVALID", "Too many positional path arguments.")
            } else {
                expectedNames.firstOrNull { it.equals(suppliedName, ignoreCase = true) }
                    ?: throw PathBindingException("PATH_ARGUMENT_UNKNOWN", "Unknown path argument '$suppliedName'.")
            }
            if (result.put(canonicalName, value) != null) {
                throw PathBindingException("PATH_ARGUMENT_DUPLICATE", "Path argument '$canonicalName' is duplicated.")
            }
        }
        return result
    }

    private fun Map<String, ExecutionValue>.required(name: String): ExecutionValue =
        get(name) ?: throw PathBindingException("PATH_ARGUMENT_REQUIRED", "Path argument '$name' is required.")

    private fun Map<String, ExecutionValue>.number(name: String): Double =
        (required(name) as? ExecutionValue.NumberValue)?.value
            ?.takeIf(Double::isFinite)
            ?: throw PathBindingException("PATH_NUMBER_REQUIRED", "Path argument '$name' must be a finite Number.")

    private fun Map<String, ExecutionValue>.point(name: String): PointerPathPoint =
        (required(name) as? ExecutionValue.PointValue)?.value
            ?: throw PathBindingException("PATH_POINT_REQUIRED", "Path argument '$name' must be a Point.")

    private fun Map<String, ExecutionValue>.optionalNumber(name: String): Double? =
        get(name)?.let { value ->
            (value as? ExecutionValue.NumberValue)?.value?.takeIf(Double::isFinite)
                ?: throw PathBindingException("PATH_NUMBER_REQUIRED", "Path argument '$name' must be a finite Number.")
        }

    private fun Map<String, ExecutionValue>.optionalWholeLong(name: String, positive: Boolean): Long? =
        get(name)?.wholeLong(name, positive)

    private fun ExecutionValue.wholeLong(name: String, positive: Boolean): Long {
        val number = (this as? ExecutionValue.NumberValue)?.value
            ?.takeIf(Double::isFinite)
            ?: throw PathBindingException("PATH_NUMBER_REQUIRED", "Path argument '$name' must be a finite Number.")
        val minimum = if (positive) 1.0 else 0.0
        if (number < minimum || number > Long.MAX_VALUE.toDouble() || number % 1.0 != 0.0) {
            throw PathBindingException(
                "PATH_WHOLE_NUMBER_REQUIRED",
                "Path argument '$name' must be ${if (positive) "a positive" else "a non-negative"} whole Number.",
            )
        }
        return number.toLong()
    }

    private fun Long.toIntChecked(name: String): Int {
        if (this > Int.MAX_VALUE) throw PathBindingException("PATH_NUMBER_RANGE", "$name exceeds the supported range.")
        return toInt()
    }

    private fun failure(code: String, message: String, invalidSegmentIndex: Int? = null) =
        PointerPathConstructionResult.Failure(code, message, invalidSegmentIndex)

    private class PathBindingException(
        val code: String,
        message: String,
        val invalidSegmentIndex: Int? = null,
    ) : IllegalArgumentException(message)
}

fun PointerPath.traceInfo(
    status: String,
    invalidSegmentIndex: Int? = null,
    cancellationPosition: String? = null,
): PointerPathTraceInfo {
    val resolved = PointerPathLowerer.resolve(this) as? PointerPathResolutionResult.Success
    return PointerPathTraceInfo(
        segmentCount = geometry.segments.size,
        segmentTypes = geometry.segments.map { segment ->
            when (segment) {
                is PointerPathSegment.Line -> "LINE"
                is PointerPathSegment.QuadraticBezier -> "QUADRATIC_BEZIER"
                is PointerPathSegment.CubicBezier -> "CUBIC_BEZIER"
            }
        },
        plannedTotalDurationMs = resolved?.pointerPath?.let { path -> path.totalDispatchDurationMs * path.repeat },
        status = status,
        invalidSegmentIndex = invalidSegmentIndex,
        cancellationPosition = cancellationPosition,
        pointerCount = 1,
        pointerIds = listOf(pointerId),
        pointerStartOffsetsMs = listOf(timing.delayMs),
        pointerEndOffsetsMs = listOfNotNull(resolved?.pointerPath?.totalDispatchDurationMs),
        pointerSegmentCounts = listOf(geometry.segments.size),
        gestureKind = "SINGLE_TOUCH",
    )
}

fun MultiPointerGesture.traceInfo(
    status: String,
    cancellationPosition: String? = null,
): PointerPathTraceInfo {
    val resolved = MultiPointerGestureLowerer.resolve(this) as? MultiPointerResolutionResult.Success
    val resolvedPointers = resolved?.gesture?.pointers.orEmpty()
    return PointerPathTraceInfo(
        segmentCount = pointers.sumOf { it.geometry.segments.size },
        segmentTypes = pointers.flatMap { pointer ->
            pointer.geometry.segments.map { segment ->
                when (segment) {
                    is PointerPathSegment.Line -> "LINE"
                    is PointerPathSegment.QuadraticBezier -> "QUADRATIC_BEZIER"
                    is PointerPathSegment.CubicBezier -> "CUBIC_BEZIER"
                }
            }
        },
        plannedTotalDurationMs = resolved?.gesture?.plannedTotalDurationMs,
        status = status,
        cancellationPosition = cancellationPosition,
        pointerCount = pointers.size,
        pointerIds = pointers.map(PointerPath::pointerId),
        pointerStartOffsetsMs = pointers.map { it.timing.delayMs },
        pointerEndOffsetsMs = resolvedPointers.map(ResolvedPointerPath::totalDispatchDurationMs),
        pointerSegmentCounts = pointers.map { it.geometry.segments.size },
        gestureKind = "MULTI_TOUCH",
    )
}

fun List<ExecutionValue>.gestureTraceInfoOrNull(
    commandId: String,
    status: String,
    invalidSegmentIndex: Int? = null,
    cancellationPosition: String? = null,
): PointerPathTraceInfo? = when {
    commandId in setOf("action.multiSwipe", "multiSwipe") ->
        (firstOrNull() as? ExecutionValue.MultiPathValue)?.value?.traceInfo(status, cancellationPosition)
    else -> gesturePointerPathOrNull(commandId)?.traceInfo(status, invalidSegmentIndex, cancellationPosition)
}

fun List<ExecutionValue>.gesturePointerPathOrNull(commandId: String): PointerPath? {
    if (commandId !in setOf("action.swipe", "swipe")) return null
    return when (val first = firstOrNull()) {
        is ExecutionValue.PointerPathValue -> first.value
        is ExecutionValue.PathValue -> {
            val repeat = (getOrNull(1) as? ExecutionValue.NumberValue)?.value
                ?.takeIf { it.isFinite() && it >= 1.0 && it % 1.0 == 0.0 && it <= Int.MAX_VALUE.toDouble() }
                ?.toInt()
                ?: 1
            PointerPath(
                pointerId = 0,
                geometry = first.value,
                timing = PointerPathTiming(durationMs = PointerPathSourceCodec.DEFAULT_DURATION_MS, repeat = repeat),
            )
        }
        else -> null
    }
}

object PointerPathSourceCodec {
    fun parseSwipe(arguments: String): PointerPathParseResult = try {
        val parsed = GestureSourceParser(arguments).parseTopLevelArguments()
        decodeSwipe(parsed)
    } catch (error: GestureSourceException) {
        PointerPathParseResult.Failure(error.code, error.message ?: error.code)
    } catch (error: IllegalArgumentException) {
        PointerPathParseResult.Failure("TOUCH_PATH_INVALID", error.message ?: "Invalid PointerPath.")
    }

    private fun decodeSwipe(arguments: List<SourceArgument>): PointerPathParseResult {
        if (arguments.isEmpty()) return failure("TOUCH_PATH_INVALID", "swipe requires geometry.")
        val first = arguments.first()
        if (first.name != null) return failure("TOUCH_PATH_INVALID", "swipe geometry must be the first positional argument.")

        if (first.value is SourceValue.ArrayValue && first.value.values.all { it is SourceValue.NumberValue }) {
            return decodeLegacy(arguments)
        }

        val pointerPathCall = (first.value as? SourceValue.CallValue)
            ?.takeIf { it.name.equals("pointerPath", ignoreCase = true) }
        val decoded = if (pointerPathCall != null) {
            decodePointerPath(pointerPathCall)
        } else {
            val (geometry, consumed) = decodeTopLevelGeometry(arguments)
            val timing = decodeTiming(arguments.drop(consumed), defaultDurationMs = DEFAULT_DURATION_MS)
            PointerPath(
                pointerId = timing.pointerId,
                geometry = geometry,
                timing = timing.value,
                pointerIdExplicit = timing.pointerIdExplicit,
            )
        }
        return PointerPathParseResult.Success(decoded, legacySyntax = false)
    }

    private fun decodeLegacy(arguments: List<SourceArgument>): PointerPathParseResult {
        if (arguments.size !in 1..2 || arguments.any { it.name != null }) {
            return failure("TOUCH_PATH_INVALID", "Legacy swipe accepts a numeric point array and optional repeat.")
        }
        val values = (arguments.first().value as SourceValue.ArrayValue).values.map { value ->
            (value as SourceValue.NumberValue).value
        }
        if (values.size < 4 || values.size % 2 != 0) {
            return failure("TOUCH_PATH_INVALID", "Legacy swipe requires at least two complete point pairs.")
        }
        val points = values.chunked(2).map { PointerPathPoint(it[0], it[1]) }
        val repeat = arguments.getOrNull(1)?.value?.positiveWholeNumber("repeat")?.toInt() ?: 1
        return PointerPathParseResult.Success(
            pointerPath = PointerPath(
                pointerId = 0,
                geometry = points.toPolyline(),
                timing = PointerPathTiming(durationMs = DEFAULT_DURATION_MS, repeat = repeat),
            ),
            legacySyntax = true,
        )
    }

    private fun decodePointerPath(call: SourceValue.CallValue): PointerPath {
        val positional = call.arguments.filter { it.name == null }
        if (positional.size != 1) {
            throw GestureSourceException("TOUCH_PATH_INVALID", "pointerPath requires exactly one Path value.")
        }
        val geometry = decodeGeometry(positional.single().value)
        val timing = decodeTiming(call.arguments.filter { it.name != null }, defaultDurationMs = null)
        return PointerPath(
            pointerId = timing.pointerId,
            geometry = geometry,
            timing = timing.value,
            pointerIdExplicit = timing.pointerIdExplicit,
        )
    }

    private fun decodeTopLevelGeometry(arguments: List<SourceArgument>): Pair<PointerPathGeometry, Int> {
        val positional = arguments.takeWhile { it.name == null }
        if (positional.isEmpty()) throw GestureSourceException("TOUCH_PATH_INVALID", "swipe requires positional geometry.")
        val firstCall = positional.first().value as? SourceValue.CallValue
        if (firstCall?.name.equals("point", ignoreCase = true)) {
            val points = positional.takeWhile { argument ->
                (argument.value as? SourceValue.CallValue)?.name.equals("point", ignoreCase = true)
            }.map { decodePoint(it.value) }
            if (points.size < 2) {
                throw GestureSourceException("TOUCH_PATH_INVALID", "swipe requires at least two Point values.")
            }
            return points.toPolyline() to points.size
        }
        return decodeGeometry(positional.first().value) to 1
    }

    private fun decodeGeometry(value: SourceValue): PointerPathGeometry {
        val call = value as? SourceValue.CallValue
            ?: throw GestureSourceException("TOUCH_PATH_INVALID", "Expected path(...) or pointerPath(...).")
        if (!call.name.equals("path", ignoreCase = true)) {
            throw GestureSourceException(
                "TOUCH_PATH_CONSTRUCTOR_UNSUPPORTED",
                "Unsupported gesture constructor '${call.name}'. Use path(...) with V1 segment constructors.",
            )
        }
        if (call.arguments.any { it.name != null }) {
            throw GestureSourceException("TOUCH_PATH_INVALID", "Path geometry does not accept timing or named arguments.")
        }
        val values = if (call.arguments.size == 1 && call.arguments.single().value is SourceValue.ArrayValue) {
            (call.arguments.single().value as SourceValue.ArrayValue).values
        } else {
            call.arguments.map(SourceArgument::value)
        }
        if (values.size < 2 && values.firstOrNull() is SourceValue.CallValue &&
            !(values.first() as SourceValue.CallValue).name.endsWith("Segment", ignoreCase = true)
        ) {
            throw GestureSourceException("TOUCH_PATH_INVALID", "path(points...) requires at least two points.")
        }
        if (values.all { (it as? SourceValue.CallValue)?.name.equals("point", ignoreCase = true) }) {
            return values.map(::decodePoint).toPolyline()
        }
        val segments = values.map(::decodeSegment)
        if (segments.isEmpty()) throw GestureSourceException("TOUCH_PATH_INVALID", "path requires at least one segment.")
        return PointerPathGeometry(segments.first().from, segments)
    }

    private fun decodeSegment(value: SourceValue): PointerPathSegment {
        val call = value as? SourceValue.CallValue
            ?: throw GestureSourceException("TOUCH_PATH_INVALID", "Path arrays may contain only segment constructors.")
        if (call.arguments.any { it.name != null }) {
            throw GestureSourceException("TOUCH_PATH_INVALID", "Path segment constructors use positional geometry only.")
        }
        val values = call.arguments.map(SourceArgument::value)
        return when (call.name.lowercase()) {
            "linesegment" -> {
                requireArity(call, 2)
                PointerPathSegment.Line(decodePoint(values[0]), decodePoint(values[1]))
            }
            "quadraticbeziersegment" -> {
                requireArity(call, 3)
                PointerPathSegment.QuadraticBezier(
                    decodePoint(values[0]),
                    decodePoint(values[1]),
                    decodePoint(values[2]),
                )
            }
            "cubicbeziersegment" -> {
                requireArity(call, 4)
                PointerPathSegment.CubicBezier(
                    decodePoint(values[0]),
                    decodePoint(values[1]),
                    decodePoint(values[2]),
                    decodePoint(values[3]),
                )
            }
            else -> throw GestureSourceException(
                "TOUCH_PATH_CONSTRUCTOR_UNSUPPORTED",
                "Unsupported Path segment '${call.name}'.",
            )
        }
    }

    private fun decodePoint(value: SourceValue): PointerPathPoint {
        val call = value as? SourceValue.CallValue
            ?: throw GestureSourceException("TOUCH_POINT_INVALID", "Expected point(x, y).")
        if (!call.name.equals("point", ignoreCase = true) || call.arguments.size != 2 ||
            call.arguments.any { it.name != null }
        ) {
            throw GestureSourceException("TOUCH_POINT_INVALID", "Point requires exactly two positional numbers.")
        }
        return PointerPathPoint(call.arguments[0].value.number("point.x"), call.arguments[1].value.number("point.y"))
    }

    private fun decodeTiming(arguments: List<SourceArgument>, defaultDurationMs: Long?): DecodedTiming {
        if (arguments.any { it.name == null }) {
            throw GestureSourceException("TOUCH_TIMING_INVALID", "Typed PointerPath timing arguments must be named.")
        }
        val unique = linkedMapOf<String, SourceValue>()
        arguments.forEach { argument ->
            val canonical = when (argument.name!!.lowercase()) {
                "duration", "durationms" -> "duration"
                "segmentdurations", "segmentdurationsms" -> "segmentDurations"
                "speed", "speedpixelspersecond" -> "speed"
                "starthold", "startholdms" -> "startHold"
                "endhold", "endholdms" -> "endHold"
                "delay", "delayms" -> "delay"
                "repeat" -> "repeat"
                "pointerid" -> "pointerId"
                else -> throw GestureSourceException(
                    "TOUCH_TIMING_ARGUMENT_UNKNOWN",
                    "Unknown PointerPath timing argument '${argument.name}'.",
                )
            }
            if (unique.put(canonical, argument.value) != null) {
                throw GestureSourceException("TOUCH_TIMING_DUPLICATE", "PointerPath argument '$canonical' is duplicated.")
            }
        }
        val explicitDuration = unique["duration"]?.positiveWholeNumber("duration")
        val segmentDurations = unique["segmentDurations"]?.positiveWholeNumberList("segmentDurations")
        val speed = unique["speed"]?.number("speed")
        if (listOf(explicitDuration, segmentDurations, speed).count { it != null } > 1) {
            throw GestureSourceException(
                "TOUCH_TIMING_CONFLICT",
                "PointerPath duration, segmentDurations and speed are mutually exclusive.",
            )
        }
        val duration = explicitDuration ?: defaultDurationMs.takeIf {
            segmentDurations == null && speed == null
        }
        if (duration == null && segmentDurations == null && speed == null) {
            throw GestureSourceException(
                "TOUCH_TIMING_REQUIRED",
                "PointerPath requires duration, segmentDurations or speed.",
            )
        }
        if (speed != null && speed <= 0.0) {
            throw GestureSourceException("TOUCH_SPEED_INVALID", "PointerPath speed must be positive.")
        }
        val pointerId = unique["pointerId"]?.nonNegativeWholeNumber("pointerId")?.toInt() ?: 0
        return DecodedTiming(
            pointerId = pointerId,
            pointerIdExplicit = "pointerId" in unique,
            value = PointerPathTiming(
                durationMs = duration,
                segmentDurationsMs = segmentDurations,
                speedPixelsPerSecond = speed,
                startHoldMs = unique["startHold"]?.nonNegativeWholeNumber("startHold") ?: 0L,
                endHoldMs = unique["endHold"]?.nonNegativeWholeNumber("endHold") ?: 0L,
                delayMs = unique["delay"]?.nonNegativeWholeNumber("delay") ?: 0L,
                repeat = unique["repeat"]?.positiveWholeNumber("repeat")?.toInt() ?: 1,
            ),
        )
    }

    private fun List<PointerPathPoint>.toPolyline(): PointerPathGeometry {
        if (size < 2) throw GestureSourceException("TOUCH_PATH_INVALID", "A path requires at least two points.")
        return PointerPathGeometry(
            startPoint = first(),
            segments = zipWithNext { from, to -> PointerPathSegment.Line(from, to) },
        )
    }

    private fun requireArity(call: SourceValue.CallValue, expected: Int) {
        if (call.arguments.size != expected) {
            throw GestureSourceException(
                "TOUCH_PATH_INVALID",
                "${call.name} requires $expected point arguments.",
            )
        }
    }

    private fun SourceValue.number(label: String): Double {
        val number = (this as? SourceValue.NumberValue)?.value
            ?: throw GestureSourceException("TOUCH_TIMING_INVALID", "$label must be numeric.")
        if (!number.isFinite()) throw GestureSourceException("TOUCH_TIMING_INVALID", "$label must be finite.")
        return number
    }

    private fun SourceValue.positiveWholeNumber(label: String): Long {
        val number = number(label)
        if (number < 1.0 || number > Long.MAX_VALUE.toDouble() || number % 1.0 != 0.0) {
            throw GestureSourceException("TOUCH_TIMING_INVALID", "$label must be a positive whole number.")
        }
        return number.toLong()
    }

    private fun SourceValue.nonNegativeWholeNumber(label: String): Long {
        val number = number(label)
        if (number < 0.0 || number > Long.MAX_VALUE.toDouble() || number % 1.0 != 0.0) {
            throw GestureSourceException("TOUCH_TIMING_INVALID", "$label must be a non-negative whole number.")
        }
        return number.toLong()
    }

    private fun SourceValue.positiveWholeNumberList(label: String): List<Long> {
        val values = (this as? SourceValue.ArrayValue)?.values
            ?: throw GestureSourceException("TOUCH_TIMING_INVALID", "$label must be a numeric list.")
        if (values.isEmpty()) {
            throw GestureSourceException("TOUCH_TIMING_INVALID", "$label must not be empty.")
        }
        return values.mapIndexed { index, value -> value.positiveWholeNumber("$label[$index]") }
    }

    private fun failure(code: String, message: String) = PointerPathParseResult.Failure(code, message)

    private data class DecodedTiming(
        val pointerId: Int,
        val pointerIdExplicit: Boolean,
        val value: PointerPathTiming,
    )

    const val DEFAULT_DURATION_MS = 250L
}

private sealed interface SourceValue {
    data class NumberValue(val value: Double) : SourceValue
    data class ArrayValue(val values: List<SourceValue>) : SourceValue
    data class SymbolValue(val name: String) : SourceValue
    data class CallValue(val name: String, val arguments: List<SourceArgument>) : SourceValue
}

private data class SourceArgument(val name: String?, val value: SourceValue)

private class GestureSourceParser(source: String) {
    private val tokens = GestureSourceLexer(source).lex()
    private var index = 0

    fun parseTopLevelArguments(): List<SourceArgument> {
        if (check(SourceTokenKind.EOF)) return emptyList()
        val values = parseArguments(SourceTokenKind.EOF)
        consume(SourceTokenKind.EOF, "Unexpected content after swipe arguments.")
        return values
    }

    private fun parseArguments(terminator: SourceTokenKind): List<SourceArgument> {
        val arguments = mutableListOf<SourceArgument>()
        if (check(terminator)) return arguments
        do {
            val name = if (check(SourceTokenKind.IDENTIFIER) && peekNext().kind == SourceTokenKind.EQUALS) {
                advance().lexeme.also { advance() }
            } else {
                null
            }
            arguments += SourceArgument(name, parseValue())
        } while (match(SourceTokenKind.COMMA))
        return arguments
    }

    private fun parseValue(): SourceValue = when {
        match(SourceTokenKind.NUMBER) -> SourceValue.NumberValue(
            previous().lexeme.toDoubleOrNull()
                ?: throw GestureSourceException("TOUCH_SOURCE_SYNTAX", "Invalid number '${previous().lexeme}'."),
        )
        match(SourceTokenKind.IDENTIFIER) -> {
            val name = previous().lexeme
            if (match(SourceTokenKind.LEFT_PAREN)) {
                val arguments = parseArguments(SourceTokenKind.RIGHT_PAREN)
                consume(SourceTokenKind.RIGHT_PAREN, "Missing ')' after $name arguments.")
                SourceValue.CallValue(name, arguments)
            } else {
                SourceValue.SymbolValue(name)
            }
        }
        match(SourceTokenKind.LEFT_BRACKET) -> {
            val values = parseArguments(SourceTokenKind.RIGHT_BRACKET)
            if (values.any { it.name != null }) {
                throw GestureSourceException("TOUCH_SOURCE_SYNTAX", "Arrays cannot contain named values.")
            }
            consume(SourceTokenKind.RIGHT_BRACKET, "Missing ']' after array.")
            SourceValue.ArrayValue(values.map(SourceArgument::value))
        }
        else -> throw GestureSourceException(
            "TOUCH_SOURCE_SYNTAX",
            "Expected PointerPath value near '${peek().lexeme}'.",
        )
    }

    private fun match(kind: SourceTokenKind): Boolean {
        if (!check(kind)) return false
        advance()
        return true
    }

    private fun consume(kind: SourceTokenKind, message: String): SourceToken {
        if (check(kind)) return advance()
        throw GestureSourceException("TOUCH_SOURCE_SYNTAX", message)
    }

    private fun check(kind: SourceTokenKind): Boolean = peek().kind == kind
    private fun advance(): SourceToken = tokens[index++].also { }
    private fun previous(): SourceToken = tokens[index - 1]
    private fun peek(): SourceToken = tokens[index]
    private fun peekNext(): SourceToken = tokens.getOrElse(index + 1) { tokens.last() }
}

private class GestureSourceLexer(private val source: String) {
    fun lex(): List<SourceToken> = buildList {
        var index = 0
        while (index < source.length) {
            val character = source[index]
            when {
                character.isWhitespace() -> index += 1
                character == '(' -> add(SourceToken(SourceTokenKind.LEFT_PAREN, "(")).also { index += 1 }
                character == ')' -> add(SourceToken(SourceTokenKind.RIGHT_PAREN, ")")).also { index += 1 }
                character == '[' -> add(SourceToken(SourceTokenKind.LEFT_BRACKET, "[")).also { index += 1 }
                character == ']' -> add(SourceToken(SourceTokenKind.RIGHT_BRACKET, "]")).also { index += 1 }
                character == ',' -> add(SourceToken(SourceTokenKind.COMMA, ",")).also { index += 1 }
                character == '=' -> add(SourceToken(SourceTokenKind.EQUALS, "=")).also { index += 1 }
                character == '-' || character == '+' || character.isDigit() || character == '.' -> {
                    val start = index
                    if (character == '-' || character == '+') index += 1
                    var hasDigits = false
                    while (index < source.length && source[index].isDigit()) {
                        hasDigits = true
                        index += 1
                    }
                    if (index < source.length && source[index] == '.') {
                        index += 1
                        while (index < source.length && source[index].isDigit()) {
                            hasDigits = true
                            index += 1
                        }
                    }
                    if (!hasDigits) {
                        throw GestureSourceException("TOUCH_SOURCE_SYNTAX", "Invalid numeric token near '$character'.")
                    }
                    if (index < source.length && source[index].lowercaseChar() == 'e') {
                        index += 1
                        if (index < source.length && source[index] in "+-") index += 1
                        val exponentStart = index
                        while (index < source.length && source[index].isDigit()) index += 1
                        if (index == exponentStart) {
                            throw GestureSourceException("TOUCH_SOURCE_SYNTAX", "Invalid numeric exponent.")
                        }
                    }
                    add(SourceToken(SourceTokenKind.NUMBER, source.substring(start, index)))
                }
                character == '_' || character == '§' || character == '%' || character.isLetter() -> {
                    val start = index++
                    while (index < source.length &&
                        (source[index] == '_' || source[index] == '§' || source[index] == '%' ||
                            source[index] == '.' || source[index].isLetterOrDigit())
                    ) {
                        index += 1
                    }
                    add(SourceToken(SourceTokenKind.IDENTIFIER, source.substring(start, index)))
                }
                else -> throw GestureSourceException(
                    "TOUCH_SOURCE_SYNTAX",
                    "Unsupported token '$character' in PointerPath source.",
                )
            }
        }
        add(SourceToken(SourceTokenKind.EOF, ""))
    }
}

private data class SourceToken(val kind: SourceTokenKind, val lexeme: String)

private enum class SourceTokenKind {
    IDENTIFIER,
    NUMBER,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    COMMA,
    EQUALS,
    EOF,
}

private class GestureSourceException(
    val code: String,
    message: String,
) : IllegalArgumentException(message)
