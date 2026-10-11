package com.visualtasker.wss.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityWindowInfo
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.visualtasker.wss.workspace.model.CoordinateSpace
import com.visualtasker.wss.workspace.model.CoordinateSpaceKind
import com.visualtasker.wss.workspace.model.RecordingEventStore
import com.visualtasker.wss.workspace.model.WindowContext
import com.visualtasker.wss.workspace.model.WindowSnapshot
import com.visualtasker.wss.workspace.model.WorldviewRect
import com.visualtasker.wss.recording.A11yNodeSnapshot
import com.visualtasker.wss.recording.RecordingSessionRuntime
import com.visualtasker.wss.targeting.AccessibilityTargetResolver
import com.visualtasker.wss.targeting.AccessibilityTreeSnapshot
import com.visualtasker.wss.targeting.ElementBoundsPx
import com.visualtasker.wss.targeting.ElementQuery
import com.visualtasker.wss.targeting.ElementRef
import com.visualtasker.wss.targeting.ObservedAccessibilityElement
import com.visualtasker.wss.targeting.TargetActionSubmission
import com.visualtasker.wss.targeting.TargetResolutionPolicy
import com.visualtasker.wss.targeting.TargetResolutionStatus
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VisualTaskerAccessibilityService : AccessibilityService() {
    @Volatile
    private var lastInspectorSnapshot: AccessibilityInspectorSnapshot? = null
    private val activeGestureId = AtomicLong(NO_ACTIVE_GESTURE)
    private val activeGestureCancellation = AtomicReference<AtomicBoolean?>(null)
    private val gestureIdSequence = AtomicLong(NO_ACTIVE_GESTURE)

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
            if (accessibilityEvent.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                val bounds = accessibilityEvent.source?.let { Rect().also(it::getBoundsInScreen) }
                if (bounds != null && !bounds.isEmpty) {
                    RecordingSessionRuntime.recordTap(bounds.centerX(), bounds.centerY())
                }
            }
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

    fun currentRecordingA11yTree(): A11yNodeSnapshot? =
        rootInActiveWindow?.toRecordingNode(
            nodeId = "node-0",
            parentNodeId = null,
            childOrder = 0,
        )

    suspend fun clickText(text: String): Boolean {
        val snapshot = captureTargetTree(DEFAULT_TARGET_DISPLAY_ID) ?: return false
        val resolution = AccessibilityTargetResolver.resolve(
            snapshot = snapshot,
            query = ElementQuery(
                text = text,
                displayId = DEFAULT_TARGET_DISPLAY_ID,
                matchTextOrDescription = true,
            ),
            policy = TargetResolutionPolicy(displayId = DEFAULT_TARGET_DISPLAY_ID),
        )
        val target = resolution.match?.actionTarget ?: return false
        return resolution.status == TargetResolutionStatus.FOUND &&
            performTargetClick(target) == TargetActionSubmission.ACCEPTED
    }

    fun captureTargetTree(displayId: Int): AccessibilityTreeSnapshot? {
        if (displayId != DEFAULT_TARGET_DISPLAY_ID) return null
        val root = rootInActiveWindow ?: return null
        val now = System.currentTimeMillis()
        val windowId = root.windowId.toString()
        return AccessibilityTreeSnapshot(
            snapshotId = "a11y-$windowId-$now",
            capturedAtEpochMs = now,
            displayId = DEFAULT_TARGET_DISPLAY_ID,
            windowId = windowId,
            packageName = root.packageName?.toString(),
            root = root.toObservedTargetNode(
                nodeId = "node-0",
                parentNodeId = null,
                childOrder = 0,
                displayId = DEFAULT_TARGET_DISPLAY_ID,
                windowId = windowId,
            ),
        )
    }

    fun performTargetClick(target: ElementRef): TargetActionSubmission {
        if (target.displayId != DEFAULT_TARGET_DISPLAY_ID) return TargetActionSubmission.REJECTED
        val root = rootInActiveWindow ?: return TargetActionSubmission.STALE
        val current = root.findBySnapshotPath(target.snapshotNodeId) ?: return TargetActionSubmission.STALE
        val bounds = Rect().also(current::getBoundsInScreen)
        val stillMatches = current.packageName?.toString() == target.packageName &&
            current.className?.toString().orEmpty() == target.className &&
            current.viewIdResourceName == target.viewId &&
            ElementBoundsPx(bounds.left, bounds.top, bounds.right, bounds.bottom) == target.bounds
        if (!stillMatches) return TargetActionSubmission.STALE
        return if (current.isEnabled && current.isVisibleToUser &&
            current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        ) {
            TargetActionSubmission.ACCEPTED
        } else {
            TargetActionSubmission.REJECTED
        }
    }

    suspend fun clickPoint(x: Int, y: Int): Boolean =
        dispatchPathGesture(
            path = Path().apply { moveTo(x.toFloat(), y.toFloat()) },
            durationMs = 1L,
        )

    fun gestureDisplayBounds(): Rect {
        val metrics = resources.displayMetrics
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    fun canPerformGestures(): Boolean =
        serviceInfo?.capabilities
            ?.and(AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) != 0

    fun maxGestureDurationMs(): Long = GestureDescription.getMaxGestureDuration()

    fun maxGesturePointerCount(): Int = GestureDescription.getMaxStrokeCount()

    fun cancelActiveGestureSequence() {
        activeGestureCancellation.get()?.set(true)
    }

    fun dispatchClickGesture(
        x: Float,
        y: Float,
        onResult: (AccessibilityGestureResult) -> Unit,
    ): Boolean = dispatchGesturePath(
        points = listOf(AccessibilityGesturePoint(x, y)),
        durationMs = 1L,
        onResult = onResult,
    ) == AccessibilityGestureSubmission.Accepted

    fun dispatchGesturePath(
        points: List<AccessibilityGesturePoint>,
        durationMs: Long,
        onResult: (AccessibilityGestureResult) -> Unit,
    ): AccessibilityGestureSubmission {
        if (points.isEmpty() || points.any { !it.x.isFinite() || !it.y.isFinite() }) {
            return AccessibilityGestureSubmission.Rejected
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        }
        return dispatchPathGesture(path, durationMs.coerceAtLeast(1L), onResult)
    }

    fun dispatchPointerPath(
        pointerPath: AccessibilityPointerPath,
        onResult: (AccessibilityGestureResult) -> Unit,
    ): AccessibilityGestureSubmission {
        if (pointerPath.pointerId < 0 || pointerPath.segments.isEmpty()) {
            return AccessibilityGestureSubmission.Rejected
        }
        val phases = runCatching {
            buildList {
                if (pointerPath.startHoldMs > 0L) {
                    addAll(
                        stationaryPhases(
                            point = pointerPath.startPoint,
                            durationMs = pointerPath.startHoldMs,
                            initialStartTimeMs = pointerPath.delayMs,
                        ),
                    )
                }
                pointerPath.segments.forEachIndexed { index, segment ->
                    addAll(
                        segment.toCancellablePhases(
                            initialStartTimeMs = if (index == 0 && pointerPath.startHoldMs == 0L) {
                                pointerPath.delayMs
                            } else {
                                0L
                            },
                        ),
                    )
                }
                if (pointerPath.endHoldMs > 0L) {
                    addAll(
                        stationaryPhases(
                            point = pointerPath.segments.last().to,
                            durationMs = pointerPath.endHoldMs,
                            initialStartTimeMs = 0L,
                        ),
                    )
                }
            }
        }.getOrElse { return AccessibilityGestureSubmission.Rejected }
        if (phases.any { it.durationMs <= 0L || it.startTimeMs < 0L }) {
            return AccessibilityGestureSubmission.Rejected
        }
        return dispatchGestureSequence(phases, onResult)
    }

    fun dispatchMultiPointerGesture(
        pointerPaths: List<AccessibilityPointerPath>,
        onResult: (AccessibilityGestureResult) -> Unit,
    ): AccessibilityGestureSubmission {
        if (pointerPaths.size < 2 || pointerPaths.size > maxGesturePointerCount() ||
            pointerPaths.map(AccessibilityPointerPath::pointerId).distinct().size != pointerPaths.size ||
            pointerPaths.any { it.pointerId < 0 || it.segments.isEmpty() }
        ) {
            return AccessibilityGestureSubmission.Rejected
        }
        val phases = runCatching { buildMultiPointerPhases(pointerPaths) }
            .getOrElse { return AccessibilityGestureSubmission.Rejected }
        return dispatchGestureSequence(phases, onResult)
    }

    internal fun inspectMultiPointerPhasePlan(
        pointerPaths: List<AccessibilityPointerPath>,
    ): List<AccessibilityGesturePhaseSnapshot> = buildMultiPointerPhases(pointerPaths).map { phase ->
        AccessibilityGesturePhaseSnapshot(
            pointerIds = phase.strokes.map(AccessibilityGestureStrokePhase::pointerId),
            strokeStartTimesMs = phase.strokes.map(AccessibilityGestureStrokePhase::startTimeMs),
            strokeDurationsMs = phase.strokes.map(AccessibilityGestureStrokePhase::durationMs),
            startTimeMs = phase.startTimeMs,
            durationMs = phase.durationMs,
        )
    }

    suspend fun swipe(points: List<RuntimePoint>, durationMs: Long): Boolean {
        if (points.size < 2) return false
        val path = Path().apply {
            moveTo(points.first().x.toFloat(), points.first().y.toFloat())
            points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
        }
        return dispatchPathGesture(path, durationMs.coerceAtLeast(1L))
    }

    suspend fun takeScreenshotTo(file: File, displayId: Int = Display.DEFAULT_DISPLAY): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return suspendCancellableCoroutine { continuation ->
            takeScreenshot(
                displayId,
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
            val submission = dispatchPathGesture(path, durationMs) { result ->
                if (continuation.isActive) {
                    continuation.resume(result == AccessibilityGestureResult.Completed)
                }
            }
            if (submission != AccessibilityGestureSubmission.Accepted && continuation.isActive) {
                continuation.resume(false)
            }
        }

    private fun dispatchPathGesture(
        path: Path,
        durationMs: Long,
        onResult: (AccessibilityGestureResult) -> Unit,
    ): AccessibilityGestureSubmission = dispatchGestureSequence(
        phases = listOf(AccessibilityGesturePhase(path, 0L, durationMs)),
        onResult = onResult,
    )

    private fun dispatchGestureSequence(
        phases: List<AccessibilityGesturePhase>,
        onResult: (AccessibilityGestureResult) -> Unit,
    ): AccessibilityGestureSubmission {
        if (phases.isEmpty()) return AccessibilityGestureSubmission.Rejected
        val gestureId = gestureIdSequence.incrementAndGet()
        if (!activeGestureId.compareAndSet(NO_ACTIVE_GESTURE, gestureId)) {
            return AccessibilityGestureSubmission.Busy
        }
        val cancellation = AtomicBoolean(false)
        activeGestureCancellation.set(cancellation)

        fun finish(result: AccessibilityGestureResult) {
            if (activeGestureId.compareAndSet(gestureId, NO_ACTIVE_GESTURE)) {
                activeGestureCancellation.compareAndSet(cancellation, null)
                onResult(result)
            }
        }

        fun releaseCancelledStrokes(
            strokes: Map<Int, GestureDescription.StrokeDescription>,
            phase: AccessibilityGesturePhase,
        ) {
            val releases = runCatching {
                phase.strokes.mapNotNull { strokePhase ->
                    val endPoint = strokePhase.endPoint ?: return@mapNotNull null
                    strokes[strokePhase.pointerId]?.continueStroke(
                        stationaryPath(endPoint),
                        0L,
                        CANCELLATION_RELEASE_DURATION_MS,
                        false,
                    )
                }
            }.getOrNull()
            if (releases.isNullOrEmpty()) {
                finish(AccessibilityGestureResult.Cancelled)
                return
            }
            val releaseGesture = GestureDescription.Builder().apply {
                releases.forEach(::addStroke)
            }.build()
            val accepted = dispatchGesture(
                releaseGesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        finish(AccessibilityGestureResult.Cancelled)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        finish(AccessibilityGestureResult.Cancelled)
                    }
                },
                null,
            )
            if (!accepted) finish(AccessibilityGestureResult.Cancelled)
        }

        lateinit var dispatchPhase: (Int, Map<Int, GestureDescription.StrokeDescription>) -> Boolean
        dispatchPhase = { index, previousStrokes ->
            val phase = phases[index]
            val hasNext = index < phases.lastIndex
            val nextPhase = phases.getOrNull(index + 1)
            val continuingPointerIds = nextPhase
                ?.takeIf { it.startTimeMs == 0L }
                ?.strokes
                ?.filter { it.startTimeMs == 0L }
                ?.mapTo(mutableSetOf(), AccessibilityGestureStrokePhase::pointerId)
                .orEmpty()
            val strokes = phase.strokes.associate { strokePhase ->
                val willContinue = strokePhase.pointerId in continuingPointerIds
                val stroke = previousStrokes[strokePhase.pointerId]?.continueStroke(
                    strokePhase.path,
                    strokePhase.startTimeMs,
                    strokePhase.durationMs,
                    willContinue,
                ) ?: GestureDescription.StrokeDescription(
                    strokePhase.path,
                    phase.startTimeMs + strokePhase.startTimeMs,
                    strokePhase.durationMs,
                    willContinue,
                )
                strokePhase.pointerId to stroke
            }
            val gesture = GestureDescription.Builder().apply {
                strokes.values.forEach(::addStroke)
            }.build()
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        when {
                            cancellation.get() && continuingPointerIds.isNotEmpty() -> releaseCancelledStrokes(
                                strokes.filterKeys(continuingPointerIds::contains),
                                phase.copy(strokes = phase.strokes.filter { it.pointerId in continuingPointerIds }),
                            )
                            cancellation.get() -> finish(AccessibilityGestureResult.Cancelled)
                            hasNext -> if (!dispatchPhase(index + 1, strokes)) {
                                finish(AccessibilityGestureResult.Rejected)
                            }
                            else -> finish(AccessibilityGestureResult.Completed)
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        finish(AccessibilityGestureResult.Cancelled)
                    }
                },
                null,
            )
        }

        val accepted = runCatching { dispatchPhase(0, emptyMap()) }.getOrDefault(false)
        if (!accepted) {
            finish(AccessibilityGestureResult.Rejected)
            return AccessibilityGestureSubmission.Rejected
        }
        val expectedDurationMs = phases.sumOf { it.startTimeMs + it.durationMs }
        Handler(Looper.getMainLooper()).postDelayed(
            { finish(AccessibilityGestureResult.Cancelled) },
            expectedDurationMs.coerceAtLeast(1L) + GESTURE_CALLBACK_GRACE_MS,
        )
        return AccessibilityGestureSubmission.Accepted
    }

    private fun stationaryPath(point: AccessibilityGesturePoint): Path =
        Path().apply { moveTo(point.x, point.y) }

    private fun buildMultiPointerPhases(
        pointerPaths: List<AccessibilityPointerPath>,
    ): List<AccessibilityGesturePhase> {
        val sections = pointerPaths.flatMap { it.toTimedSections() }
        val timelineEndMs = sections.maxOf(AccessibilityTimedPathSection::endMs)
        val sectionsByPointer = sections.groupBy(AccessibilityTimedPathSection::pointerId)
        val pointerStartTimesMs = sectionsByPointer.mapValues { (_, pointerSections) ->
            pointerSections.minOf(AccessibilityTimedPathSection::startMs)
        }
        val firstPointerStartMs = pointerStartTimesMs.values.min()
        val latestPointerStartMs = pointerStartTimesMs.values.max()
        val delayedPointerSections = sectionsByPointer
            .filterKeys { pointerId -> pointerStartTimesMs.getValue(pointerId) > firstPointerStartMs }
            .values
        delayedPointerSections.forEach { pointerSections ->
            require(pointerSections.size == 1) {
                "A delayed pointer cannot cross its own timing-section boundary on Android."
            }
        }
        val firstSectionEndAfterAllPointersStart = sections
            .asSequence()
            .map(AccessibilityTimedPathSection::endMs)
            .filter { it > latestPointerStartMs }
            .minOrNull()
            ?: timelineEndMs
        val initialPhaseEndMs = delayedPointerSections
            .takeIf { it.isNotEmpty() }
            ?.maxOf { pointerSections -> pointerSections.single().endMs }
            ?: minOf(
                timelineEndMs,
                latestPointerStartMs + MAX_CANCELLABLE_PHASE_DURATION_MS,
                firstSectionEndAfterAllPointersStart,
            )
        val boundaries = sortedSetOf(0L, initialPhaseEndMs, timelineEndMs)
        var cursor = initialPhaseEndMs
        while (cursor < timelineEndMs) {
            cursor = minOf(timelineEndMs, cursor + MAX_CANCELLABLE_PHASE_DURATION_MS)
            boundaries += cursor
        }
        sections.forEach { section ->
            if (section.endMs >= initialPhaseEndMs) boundaries += section.endMs
        }
        var previousDispatchedEndMs = 0L
        return boundaries.zipWithNext().mapNotNull { (startMs, endMs) ->
            if (endMs <= startMs) return@mapNotNull null
            val strokes = sections
                .mapNotNull { section ->
                    val overlapStartMs = maxOf(startMs, section.startMs)
                    val overlapEndMs = minOf(endMs, section.endMs)
                    if (overlapEndMs <= overlapStartMs) {
                        null
                    } else {
                        section.slice(overlapStartMs, overlapEndMs, startMs)
                    }
                }
                .sortedBy(AccessibilityGestureStrokePhase::pointerId)
            if (strokes.isEmpty()) return@mapNotNull null
            require(strokes.map(AccessibilityGestureStrokePhase::pointerId).distinct().size == strokes.size) {
                "A delayed pointer cannot begin after another pointer crossed a timing-section boundary."
            }
            AccessibilityGesturePhase(
                strokes = strokes,
                startTimeMs = (startMs - previousDispatchedEndMs).coerceAtLeast(0L),
                durationMs = endMs - startMs,
            ).also { previousDispatchedEndMs = endMs }
        }
    }

    private fun AccessibilityPointerPath.toTimedSections(): List<AccessibilityTimedPathSection> {
        var cursor = delayMs
        return buildList {
            if (startHoldMs > 0L) {
                add(AccessibilityTimedPathSection(pointerId, cursor, cursor + startHoldMs, stationaryPath(startPoint), startPoint, startPoint))
                cursor += startHoldMs
            }
            segments.forEach { segment ->
                add(
                    AccessibilityTimedPathSection(
                        pointerId = pointerId,
                        startMs = cursor,
                        endMs = cursor + segment.durationMs,
                        path = segment.toAndroidPath(),
                        startPoint = segment.from,
                        endPoint = segment.to,
                    ),
                )
                cursor += segment.durationMs
            }
            if (endHoldMs > 0L) {
                val endPoint = segments.last().to
                add(AccessibilityTimedPathSection(pointerId, cursor, cursor + endHoldMs, stationaryPath(endPoint), endPoint, endPoint))
            }
        }
    }

    private fun stationaryPhases(
        point: AccessibilityGesturePoint,
        durationMs: Long,
        initialStartTimeMs: Long,
    ): List<AccessibilityGesturePhase> = cancellablePhaseDurations(durationMs).mapIndexed { index, phaseDurationMs ->
        AccessibilityGesturePhase(
            path = stationaryPath(point),
            startTimeMs = if (index == 0) initialStartTimeMs else 0L,
            durationMs = phaseDurationMs,
            endPoint = point,
        )
    }

    private fun AccessibilityGestureSegment.toAndroidPath(): Path = Path().apply {
        moveTo(from.x, from.y)
        when (this@toAndroidPath) {
            is AccessibilityGestureSegment.Line -> lineTo(to.x, to.y)
            is AccessibilityGestureSegment.QuadraticBezier -> quadTo(control.x, control.y, to.x, to.y)
            is AccessibilityGestureSegment.CubicBezier -> cubicTo(
                control1.x,
                control1.y,
                control2.x,
                control2.y,
                to.x,
                to.y,
            )
        }
    }

    private fun AccessibilityGestureSegment.toCancellablePhases(
        initialStartTimeMs: Long,
    ): List<AccessibilityGesturePhase> {
        val sourcePath = toAndroidPath()
        val measure = PathMeasure(sourcePath, false)
        require(measure.length > 0f) { "Gesture segment must have positive path length." }
        val phaseDurations = cancellablePhaseDurations(durationMs)
        var elapsedMs = 0L
        return phaseDurations.mapIndexed { index, phaseDurationMs ->
            val startDistance = measure.length * (elapsedMs.toDouble() / durationMs).toFloat()
            elapsedMs += phaseDurationMs
            val endDistance = measure.length * (elapsedMs.toDouble() / durationMs).toFloat()
            val phasePath = Path()
            require(measure.getSegment(startDistance, endDistance, phasePath, true)) {
                "Android could not extract a PointerPath phase."
            }
            val endPosition = FloatArray(2)
            require(measure.getPosTan(endDistance.coerceAtMost(measure.length), endPosition, null)) {
                "Android could not resolve a PointerPath phase endpoint."
            }
            AccessibilityGesturePhase(
                path = phasePath,
                startTimeMs = if (index == 0) initialStartTimeMs else 0L,
                durationMs = phaseDurationMs,
                endPoint = AccessibilityGesturePoint(endPosition[0], endPosition[1]),
            )
        }
    }

    private fun cancellablePhaseDurations(durationMs: Long): List<Long> {
        require(durationMs > 0L) { "Gesture phase duration must be positive." }
        val phaseCount = ((durationMs + MAX_CANCELLABLE_PHASE_DURATION_MS - 1L) /
            MAX_CANCELLABLE_PHASE_DURATION_MS).toInt().coerceAtLeast(1)
        val baseDuration = durationMs / phaseCount
        val remainder = durationMs % phaseCount
        return List(phaseCount) { index -> baseDuration + if (index < remainder) 1L else 0L }
    }

    companion object {
        const val DEFAULT_TARGET_DISPLAY_ID = Display.DEFAULT_DISPLAY
        private const val NO_ACTIVE_GESTURE = 0L
        private const val GESTURE_CALLBACK_GRACE_MS = 5_000L
        private const val MAX_CANCELLABLE_PHASE_DURATION_MS = 200L
        private const val CANCELLATION_RELEASE_DURATION_MS = 1L

        @Volatile
        private var instance: VisualTaskerAccessibilityService? = null

        fun current(): VisualTaskerAccessibilityService? = instance

        fun isConnected(): Boolean = instance != null
    }
}

enum class AccessibilityGestureResult {
    Completed,
    Cancelled,
    Rejected,
}

enum class AccessibilityGestureSubmission {
    Accepted,
    Busy,
    Rejected,
}

data class AccessibilityGesturePoint(
    val x: Float,
    val y: Float,
)

sealed interface AccessibilityGestureSegment {
    val from: AccessibilityGesturePoint
    val to: AccessibilityGesturePoint
    val durationMs: Long

    data class Line(
        override val from: AccessibilityGesturePoint,
        override val to: AccessibilityGesturePoint,
        override val durationMs: Long,
    ) : AccessibilityGestureSegment

    data class QuadraticBezier(
        override val from: AccessibilityGesturePoint,
        val control: AccessibilityGesturePoint,
        override val to: AccessibilityGesturePoint,
        override val durationMs: Long,
    ) : AccessibilityGestureSegment

    data class CubicBezier(
        override val from: AccessibilityGesturePoint,
        val control1: AccessibilityGesturePoint,
        val control2: AccessibilityGesturePoint,
        override val to: AccessibilityGesturePoint,
        override val durationMs: Long,
    ) : AccessibilityGestureSegment
}

data class AccessibilityPointerPath(
    val pointerId: Int,
    val startPoint: AccessibilityGesturePoint,
    val segments: List<AccessibilityGestureSegment>,
    val startHoldMs: Long,
    val endHoldMs: Long,
    val delayMs: Long,
)

internal data class AccessibilityGesturePhaseSnapshot(
    val pointerIds: List<Int>,
    val strokeStartTimesMs: List<Long>,
    val strokeDurationsMs: List<Long>,
    val startTimeMs: Long,
    val durationMs: Long,
)

private data class AccessibilityTimedPathSection(
    val pointerId: Int,
    val startMs: Long,
    val endMs: Long,
    val path: Path,
    val startPoint: AccessibilityGesturePoint,
    val endPoint: AccessibilityGesturePoint,
) {
    init {
        require(endMs > startMs) { "Timed pointer section must have positive duration." }
    }

    fun slice(
        absoluteStartMs: Long,
        absoluteEndMs: Long,
        phaseStartMs: Long,
    ): AccessibilityGestureStrokePhase {
        require(absoluteStartMs >= startMs && absoluteEndMs <= endMs && absoluteEndMs > absoluteStartMs)
        val strokeStartTimeMs = absoluteStartMs - phaseStartMs
        val strokeDurationMs = absoluteEndMs - absoluteStartMs
        if (startPoint == endPoint) {
            return AccessibilityGestureStrokePhase(
                pointerId = pointerId,
                path = Path().apply { moveTo(startPoint.x, startPoint.y) },
                endPoint = endPoint,
                startTimeMs = strokeStartTimeMs,
                durationMs = strokeDurationMs,
            )
        }
        val measure = PathMeasure(path, false)
        require(measure.length > 0f) { "Timed pointer section must have positive path length." }
        val durationMs = endMs - startMs
        val startFraction = (absoluteStartMs - startMs).toDouble() / durationMs.toDouble()
        val endFraction = (absoluteEndMs - startMs).toDouble() / durationMs.toDouble()
        val startDistance = measure.length * startFraction.toFloat()
        val endDistance = measure.length * endFraction.toFloat()
        val slicedPath = Path()
        require(measure.getSegment(startDistance, endDistance, slicedPath, true)) {
            "Android could not extract a synchronized pointer phase."
        }
        val endPosition = FloatArray(2)
        require(measure.getPosTan(endDistance.coerceAtMost(measure.length), endPosition, null)) {
            "Android could not resolve a synchronized pointer endpoint."
        }
        return AccessibilityGestureStrokePhase(
            pointerId = pointerId,
            path = slicedPath,
            endPoint = AccessibilityGesturePoint(endPosition[0], endPosition[1]),
            startTimeMs = strokeStartTimeMs,
            durationMs = strokeDurationMs,
        )
    }
}

private data class AccessibilityGestureStrokePhase(
    val pointerId: Int,
    val path: Path,
    val endPoint: AccessibilityGesturePoint? = null,
    val startTimeMs: Long = 0L,
    val durationMs: Long,
)

private data class AccessibilityGesturePhase(
    val strokes: List<AccessibilityGestureStrokePhase>,
    val startTimeMs: Long,
    val durationMs: Long,
) {
    constructor(
        path: Path,
        startTimeMs: Long,
        durationMs: Long,
        endPoint: AccessibilityGesturePoint? = null,
    ) : this(
        strokes = listOf(
            AccessibilityGestureStrokePhase(
                pointerId = 0,
                path = path,
                endPoint = endPoint,
                durationMs = durationMs,
            ),
        ),
        startTimeMs = startTimeMs,
        durationMs = durationMs,
    )
}

private fun AccessibilityNodeInfo.toRecordingNode(
    nodeId: String,
    parentNodeId: String?,
    childOrder: Int,
): A11yNodeSnapshot {
    val bounds = Rect().also(::getBoundsInScreen)
    val children = buildList {
        for (index in 0 until childCount) {
            getChild(index)?.let { child ->
                add(child.toRecordingNode("$nodeId.$index", nodeId, index))
            }
        }
    }
    return A11yNodeSnapshot(
        stableSnapshotNodeId = nodeId,
        parentNodeId = parentNodeId,
        childOrder = childOrder,
        className = className?.toString().orEmpty(),
        viewIdResourceName = viewIdResourceName,
        text = text?.toString(),
        contentDescription = contentDescription?.toString(),
        left = bounds.left,
        top = bounds.top,
        right = bounds.right,
        bottom = bounds.bottom,
        clickable = isClickable,
        longClickable = isLongClickable,
        scrollable = isScrollable,
        editable = isEditable,
        enabled = isEnabled,
        selected = isSelected,
        checked = isChecked,
        visibleToUser = isVisibleToUser,
        actions = actionList.map { action -> action.label?.toString() ?: action.id.toString() },
        children = children,
    )
}

private fun AccessibilityNodeInfo.toObservedTargetNode(
    nodeId: String,
    parentNodeId: String?,
    childOrder: Int,
    displayId: Int,
    windowId: String?,
): ObservedAccessibilityElement {
    val bounds = Rect().also(::getBoundsInScreen)
    return ObservedAccessibilityElement(
        ref = ElementRef(
            snapshotNodeId = nodeId,
            packageName = packageName?.toString(),
            className = className?.toString().orEmpty(),
            viewId = viewIdResourceName,
            bounds = ElementBoundsPx(bounds.left, bounds.top, bounds.right, bounds.bottom),
            windowId = windowId,
            displayId = displayId,
        ),
        parentNodeId = parentNodeId,
        childOrder = childOrder,
        text = text?.toString(),
        contentDescription = contentDescription?.toString(),
        clickable = isClickable,
        enabled = isEnabled,
        visible = isVisibleToUser,
        children = buildList {
            for (index in 0 until childCount) {
                getChild(index)?.let { child ->
                    add(
                        child.toObservedTargetNode(
                            nodeId = "$nodeId.$index",
                            parentNodeId = nodeId,
                            childOrder = index,
                            displayId = displayId,
                            windowId = windowId,
                        ),
                    )
                }
            }
        },
    )
}

private fun AccessibilityNodeInfo.findBySnapshotPath(snapshotNodeId: String): AccessibilityNodeInfo? {
    if (snapshotNodeId == "node-0") return this
    if (!snapshotNodeId.startsWith("node-0.")) return null
    var current: AccessibilityNodeInfo = this
    snapshotNodeId.removePrefix("node-0.").split('.').forEach { segment ->
        val childIndex = segment.toIntOrNull() ?: return null
        current = current.getChild(childIndex) ?: return null
    }
    return current
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
