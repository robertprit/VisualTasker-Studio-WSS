package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.serialization.WorkflowSerializer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeTouchProviderTest {
    @Test
    fun successfulNativeClickReturnsSuccess() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Completed)

        val result = provider(gateway).invoke(clickInvocation(120.0, 240.0), context())

        assertTrue(result is ProviderResult.Success)
        assertEquals(
            TouchGesturePath(listOf(TouchGesturePoint(120f, 240f)), NativeGestureProvider.CLICK_DURATION_MS),
            gateway.paths.single(),
        )
    }

    @Test
    fun nativeSwipePreservesTwoPointOrderAndLegacyDuration() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Completed)

        val result = provider(gateway).invoke(swipeInvocation(120.0, 640.0, 120.0, 220.0), context())

        assertTrue(result is ProviderResult.Success)
        assertEquals(
            TouchGesturePath(
                points = listOf(TouchGesturePoint(120f, 640f), TouchGesturePoint(120f, 220f)),
                durationMs = NativeGestureProvider.LEGACY_SWIPE_DURATION_MS,
            ),
            gateway.paths.single(),
        )
    }

    @Test
    fun nativePolylinePathPreservesEveryWaypointInOrder() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Completed)

        val result = provider(gateway).invoke(
            swipeInvocation(120.0, 640.0, 320.0, 420.0, 520.0, 640.0),
            context(),
        )

        assertTrue(result is ProviderResult.Success)
        assertEquals(
            listOf(
                TouchGesturePoint(120f, 640f),
                TouchGesturePoint(320f, 420f),
                TouchGesturePoint(520f, 640f),
            ),
            gateway.paths.single().points,
        )
    }

    @Test
    fun bracketedCatalogPayloadIsAcceptedWithoutGeometryNormalization() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Completed)
        val invocation = swipeInvocation(
            arguments = listOf(
                ExecutionValue.StringValue("[10, 20, 30, 40, 50, 60]"),
                ExecutionValue.StringValue("1"),
            ),
        )

        val result = provider(gateway).invoke(invocation, context())

        assertTrue(result is ProviderResult.Success)
        assertEquals(TouchGesturePoint(50f, 60f), gateway.paths.single().points.last())
    }

    @Test
    fun swipeRepeatDispatchesSequentialGestures() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Completed)

        val result = provider(gateway).invoke(
            swipeInvocation(
                arguments = listOf(pointList(10.0, 20.0, 30.0, 40.0), ExecutionValue.NumberValue(3.0)),
            ),
            context(),
        )

        assertTrue(result is ProviderResult.Success)
        assertEquals(3, gateway.dispatchCount)
        assertEquals(3, gateway.paths.size)
    }

    @Test
    fun swipeRepeatCanBeCancelledBetweenDispatches() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Completed)
        val token = MutableCancellationToken()
        val result = AtomicReference<ProviderResult>()
        val invocation = swipeInvocation(
            arguments = listOf(pointList(10.0, 20.0, 30.0, 40.0), ExecutionValue.NumberValue(100.0)),
        )
        val thread = Thread { result.set(provider(gateway).invoke(invocation, context(token))) }.apply { start() }
        assertTrue(gateway.awaitDispatch())

        token.cancel()
        thread.join(1_000)

        assertTrue(result.get() is ProviderResult.Cancelled)
        assertTrue(gateway.dispatchCount < 100)
        assertFalse(thread.isAlive)
    }

    @Test
    fun invalidCoordinatesAreRejectedBeforeDispatch() {
        val gateway = FakeTouchGateway()

        val result = provider(gateway).invoke(
            clickInvocation(arguments = listOf(ExecutionValue.StringValue("x"), ExecutionValue.NumberValue(Double.NaN))),
            context(),
        )

        assertFailure(result, "TOUCH_COORDINATES_REQUIRED")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun pathRequiresAtLeastTwoCompleteFinitePoints() {
        val gateway = FakeTouchGateway()
        val invalid = listOf(
            pointList(10.0, 20.0),
            pointList(10.0, 20.0, 30.0),
            pointList(10.0, 20.0, Double.NaN, 40.0),
        )

        invalid.forEach { points ->
            assertFailure(provider(gateway).invoke(swipeInvocation(listOf(points)), context()), "TOUCH_PATH_INVALID")
        }
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun unsupportedTypedPathTextIsDiagnosedInsteadOfFlattened() {
        val gateway = FakeTouchGateway()

        val result = provider(gateway).invoke(
            swipeInvocation(listOf(ExecutionValue.StringValue("path(point(10,20),point(30,40))"))),
            context(),
        )

        assertFailure(result, "TOUCH_PATH_INVALID")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun invalidSwipeRepeatIsRejected() {
        val gateway = FakeTouchGateway()

        val result = provider(gateway).invoke(
            swipeInvocation(listOf(pointList(10.0, 20.0, 30.0, 40.0), ExecutionValue.NumberValue(1.5))),
            context(),
        )

        assertFailure(result, "TOUCH_REPEAT_INVALID")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun coordinatesOutsidePhysicalDisplayAreRejected() {
        val gateway = FakeTouchGateway(bounds = TouchDisplayBounds(0, 0, 1080, 2400))

        val result = provider(gateway).invoke(swipeInvocation(20.0, 20.0, 1080.0, 2400.0), context())

        assertFailure(result, "TOUCH_COORDINATES_OUT_OF_BOUNDS")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun invalidPhysicalDisplayBoundsAreRejected() {
        val gateway = FakeTouchGateway(bounds = TouchDisplayBounds(0, 0, 0, 2400))

        val result = provider(gateway).invoke(clickInvocation(), context())

        assertFailure(result, "TOUCH_DISPLAY_UNAVAILABLE")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun repeatCountOtherThanOneRemainsUnsupportedForClick() {
        val gateway = FakeTouchGateway()
        val repeated = clickInvocation(
            arguments = listOf(
                ExecutionValue.NumberValue(120.0),
                ExecutionValue.NumberValue(240.0),
                ExecutionValue.NumberValue(2.0),
            ),
        )

        val result = provider(gateway).invoke(repeated, context())

        assertFailure(result, "TOUCH_REPEAT_UNSUPPORTED")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun disconnectedAccessibilityServiceReturnsActionableDiagnostic() {
        val result = provider(FakeTouchGateway(connected = false)).invoke(clickInvocation(), context())

        assertFailure(result, "ACCESSIBILITY_SERVICE_UNAVAILABLE")
    }

    @Test
    fun unavailableGestureCapabilityReturnsDiagnostic() {
        val result = provider(FakeTouchGateway(canPerformGestures = false)).invoke(clickInvocation(), context())

        assertFailure(result, "ACCESSIBILITY_GESTURES_UNAVAILABLE")
    }

    @Test
    fun dispatchRejectionAndServiceBusyRemainDistinct() {
        val rejected = provider(FakeTouchGateway(submission = TouchGestureSubmission.Rejected))
            .invoke(clickInvocation(), context())
        val busy = provider(FakeTouchGateway(submission = TouchGestureSubmission.Busy))
            .invoke(clickInvocation(), context())

        assertFailure(rejected, "TOUCH_DISPATCH_REJECTED")
        assertFailure(busy, "TOUCH_GESTURE_BUSY")
    }

    @Test
    fun androidCancelledCallbackReturnsProviderCancellation() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Cancelled)

        val result = provider(gateway).invoke(clickInvocation(), context())

        assertTrue(result is ProviderResult.Cancelled)
        assertTrue((result as ProviderResult.Cancelled).message.contains("Android cancelled"))
    }

    @Test
    fun androidCancelledCallbackCorrelatesWithNearRuntimeCancellation() {
        val gateway = FakeTouchGateway()
        val token = MutableCancellationToken()
        val result = AtomicReference<ProviderResult>()
        val thread = Thread { result.set(provider(gateway).invoke(swipeInvocation(), context(token))) }.apply { start() }
        assertTrue(gateway.awaitDispatch())

        gateway.complete(TouchGestureResult.Cancelled)
        Thread.sleep(10)
        token.cancel()
        thread.join(1_000)

        assertTrue(result.get() is ProviderResult.Cancelled)
        assertFalse(thread.isAlive)
    }

    @Test
    fun cancellationBeforeDispatchDoesNotSubmitGesture() {
        val gateway = FakeTouchGateway()
        val token = MutableCancellationToken().also(MutableCancellationToken::cancel)

        val result = provider(gateway).invoke(clickInvocation(), context(token))

        assertTrue(result is ProviderResult.Cancelled)
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun cancellationDuringGestureReturnsCancelledAndIgnoresLateCallback() {
        val gateway = FakeTouchGateway()
        val token = MutableCancellationToken()
        val result = AtomicReference<ProviderResult>()
        val thread = Thread { result.set(provider(gateway).invoke(swipeInvocation(), context(token))) }.apply { start() }
        assertTrue(gateway.awaitDispatch())

        token.cancel()
        thread.join(1_000)
        gateway.complete(TouchGestureResult.Completed)

        assertTrue(result.get() is ProviderResult.Cancelled)
        assertFalse(thread.isAlive)
    }

    @Test
    fun parallelClickAndSwipeAreRejectedByOneSharedProviderGate() {
        val gateway = FakeTouchGateway()
        val provider = provider(gateway)
        val firstToken = MutableCancellationToken()
        val firstResult = AtomicReference<ProviderResult>()
        val firstThread = Thread {
            firstResult.set(provider.invoke(swipeInvocation(), context(firstToken)))
        }.apply { start() }
        assertTrue(gateway.awaitDispatch())

        val secondResult = provider.invoke(clickInvocation(), context())
        firstToken.cancel()
        firstThread.join(1_000)

        assertFailure(secondResult, "TOUCH_GESTURE_BUSY")
        assertTrue(firstResult.get() is ProviderResult.Cancelled)
    }

    @Test
    fun duplicateCallbackCannotOverrideFirstResult() {
        val gateway = FakeTouchGateway(
            callbackSequence = listOf(TouchGestureResult.Completed, TouchGestureResult.Cancelled),
        )

        val result = provider(gateway).invoke(clickInvocation(), context())

        assertTrue(result is ProviderResult.Success)
    }

    @Test
    fun missingCallbackTimesOutAndReleasesProviderForNextRun() {
        val gateway = FakeTouchGateway(
            submissionSequence = listOf(TouchGestureSubmission.Accepted, TouchGestureSubmission.Accepted),
            autoResultSequence = listOf(null, TouchGestureResult.Completed),
        )
        val provider = provider(gateway, callbackTimeoutMs = 20)

        val timedOut = provider.invoke(clickInvocation(), context())
        val retry = provider.invoke(clickInvocation(), context())

        assertFailure(timedOut, "TOUCH_GESTURE_TIMEOUT")
        assertTrue(retry is ProviderResult.Success)
    }

    @Test
    fun serviceLossDuringDispatchProducesDedicatedDiagnostic() {
        val gateway = FakeTouchGateway(
            autoResult = TouchGestureResult.Completed,
            availabilityProvider = { call ->
                TouchAvailability(
                    connected = call == 1,
                    canPerformGestures = true,
                    displayBounds = TouchDisplayBounds(0, 0, 1080, 2400),
                )
            },
        )

        val result = provider(gateway).invoke(swipeInvocation(), context())

        assertFailure(result, "ACCESSIBILITY_SERVICE_LOST")
    }

    @Test
    fun displayChangeDuringDispatchProducesDedicatedDiagnostic() {
        val gateway = FakeTouchGateway(
            autoResult = TouchGestureResult.Completed,
            availabilityProvider = { call ->
                TouchAvailability(
                    connected = true,
                    canPerformGestures = true,
                    displayBounds = if (call == 1) {
                        TouchDisplayBounds(0, 0, 1080, 2400)
                    } else {
                        TouchDisplayBounds(0, 0, 2400, 1080)
                    },
                )
            },
        )

        val result = provider(gateway).invoke(swipeInvocation(), context())

        assertFailure(result, "TOUCH_DISPLAY_CHANGED")
    }

    @Test
    fun gatewayExceptionBecomesRuntimeDiagnosticAndNextGestureCanRun() {
        val gateway = FakeTouchGateway(
            dispatchErrorSequence = listOf(IllegalStateException("service died"), null),
            autoResult = TouchGestureResult.Completed,
        )
        val provider = provider(gateway)

        val failed = provider.invoke(clickInvocation(), context())
        val retry = provider.invoke(clickInvocation(), context())

        assertFailure(failed, "TOUCH_DISPATCH_FAILED")
        assertTrue(retry is ProviderResult.Success)
    }

    @Test
    fun importedSwipeRunsThroughPlannerProviderAndTraceWithStableEntityIds() {
        val fixture = runtimeFixture("wait(1)\nswipe([120, 640, 120, 220], 1)\nwait(1)")
        val before = WorkflowSerializer.serialize(fixture.document)

        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        val completed = fixture.awaitTerminal()
        val expectedEntities = fixture.document.statementIdsInOrder()
        val startedEntities = completed.trace
            .filter { it.kind == ExecutionTraceEventKind.STEP_STARTED }
            .mapNotNull { it.entityId }

        assertEquals(ExecutionState.COMPLETED, completed.state)
        assertEquals(expectedEntities, startedEntities)
        assertEquals(
            NativeGestureProvider.PROVIDER_ID,
            completed.trace.single {
                it.kind == ExecutionTraceEventKind.PROVIDER_STARTED &&
                    it.providerId == NativeGestureProvider.PROVIDER_ID
            }.providerId,
        )
        assertEquals(before, WorkflowSerializer.serialize(fixture.document))
        assertEquals(2, fixture.gateway.paths.single().points.size)
        fixture.close()
    }

    @Test
    fun importedPolylinePathCompletesAndCanRunAgain() {
        val fixture = runtimeFixture("swipe([120, 640, 320, 420, 520, 640], 1)")

        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        assertEquals(ExecutionState.COMPLETED, fixture.awaitTerminal().state)
        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        assertEquals(ExecutionState.COMPLETED, fixture.awaitTerminal().state)

        assertEquals(2, fixture.gateway.dispatchCount)
        assertTrue(fixture.gateway.paths.all { it.points.size == 3 })
        fixture.close()
    }

    private fun runtimeFixture(script: String): RuntimeFixture {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Completed)
        val runner = DeterministicExecutionRunner(
            providers = ExecutionProviderRegistry(
                listOf(
                    AndroidWaitExecutionProvider(sleep = {}),
                    provider(gateway),
                ),
            ),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val document = import(script)
        return RuntimeFixture(scope, document, gateway, AndroidRuntimeBridge(scope, runner))
    }

    private fun import(script: String) = EmscriptWorkspaceImporter().import(
        script = script,
        workspaceId = "m5-3-native-gesture-test",
    ).let { result ->
        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        requireNotNull(result.document)
    }

    private fun de.visualtasker.workflow.core.WorkspaceDocument.statementIdsInOrder(): List<String> {
        val start = blocks.values.single { it.type == de.visualtasker.workflow.semantics.WorkflowElementTypes.EVENT_START }
        return de.visualtasker.workflow.core.WorkspaceGraph.nextChain(this, start.id)
            ?.let { head -> de.visualtasker.workflow.core.WorkspaceGraph.chainFrom(this, head) }
            .orEmpty()
            .map { it.value }
    }

    private fun provider(
        gateway: TouchGestureGateway,
        callbackTimeoutMs: Long = 500,
    ) = NativeGestureProvider(
        gateway = gateway,
        callbackTimeoutMs = callbackTimeoutMs,
        cancellationPollMs = 2,
    )

    private fun clickInvocation(x: Double = 120.0, y: Double = 240.0): ProviderInvocation = clickInvocation(
        arguments = listOf(
            ExecutionValue.NumberValue(x),
            ExecutionValue.NumberValue(y),
            ExecutionValue.NumberValue(1.0),
        ),
    )

    private fun clickInvocation(arguments: List<ExecutionValue>) = ProviderInvocation(
        requestId = "request",
        workflowId = "workflow",
        entityId = "click-block",
        commandId = "input.clickPoint",
        capability = NativeGestureProvider.A11Y_CAPABILITY,
        arguments = arguments,
    )

    private fun swipeInvocation(vararg coordinates: Double): ProviderInvocation {
        val resolved = coordinates.takeIf { it.isNotEmpty() }
            ?: doubleArrayOf(120.0, 640.0, 120.0, 220.0)
        return swipeInvocation(
            arguments = listOf(pointList(*resolved), ExecutionValue.NumberValue(1.0)),
        )
    }

    private fun swipeInvocation(arguments: List<ExecutionValue>) = ProviderInvocation(
        requestId = "request",
        workflowId = "workflow",
        entityId = "swipe-block",
        commandId = "action.swipe",
        capability = NativeGestureProvider.A11Y_CAPABILITY,
        arguments = arguments,
    )

    private fun pointList(vararg coordinates: Double): ExecutionValue.ListValue =
        ExecutionValue.ListValue(coordinates.map(ExecutionValue::NumberValue))

    private fun context(token: MutableCancellationToken = MutableCancellationToken()) = ExecutionContext(
        requestId = "request",
        workflowId = "workflow",
        variables = mutableMapOf(),
        cancellationToken = token,
    )

    private fun assertFailure(result: ProviderResult, code: String) {
        assertTrue("Expected failure $code, got $result", result is ProviderResult.Failure)
        assertEquals(code, (result as ProviderResult.Failure).code)
    }
}

private class FakeTouchGateway(
    connected: Boolean = true,
    canPerformGestures: Boolean = true,
    bounds: TouchDisplayBounds? = TouchDisplayBounds(0, 0, 1080, 2400),
    private val submission: TouchGestureSubmission = TouchGestureSubmission.Accepted,
    private val submissionSequence: List<TouchGestureSubmission> = emptyList(),
    private val autoResult: TouchGestureResult? = null,
    private val autoResultSequence: List<TouchGestureResult?> = emptyList(),
    private val callbackSequence: List<TouchGestureResult> = emptyList(),
    private val dispatchErrorSequence: List<Throwable?> = emptyList(),
    private val availabilityProvider: ((Int) -> TouchAvailability)? = null,
) : TouchGestureGateway {
    private val defaultAvailability = TouchAvailability(connected, canPerformGestures, bounds)
    private val dispatched = CountDownLatch(1)
    private val callbacks = mutableListOf<(TouchGestureResult) -> Unit>()
    private var availabilityCount = 0
    private var dispatchAttempts = 0
    val paths = mutableListOf<TouchGesturePath>()
    val dispatchCount: Int
        get() = paths.size
    val wasDispatched: Boolean
        get() = dispatchCount > 0

    override fun availability(): TouchAvailability {
        availabilityCount += 1
        return availabilityProvider?.invoke(availabilityCount) ?: defaultAvailability
    }

    override fun dispatchGesture(
        path: TouchGesturePath,
        onResult: (TouchGestureResult) -> Unit,
    ): TouchGestureSubmission {
        val index = dispatchAttempts++
        dispatchErrorSequence.getOrNull(index)?.let { throw it }
        paths += path
        callbacks += onResult
        dispatched.countDown()
        val selectedSubmission = submissionSequence.getOrNull(index) ?: submission
        if (selectedSubmission != TouchGestureSubmission.Accepted) return selectedSubmission
        callbackSequence.forEach(onResult)
        (autoResultSequence.getOrNull(index) ?: autoResult)?.let(onResult)
        return selectedSubmission
    }

    fun awaitDispatch(): Boolean = dispatched.await(1, TimeUnit.SECONDS)

    fun complete(result: TouchGestureResult, index: Int = callbacks.lastIndex) {
        callbacks.getOrNull(index)?.invoke(result)
    }
}

private data class RuntimeFixture(
    val scope: CoroutineScope,
    val document: de.visualtasker.workflow.core.WorkspaceDocument,
    val gateway: FakeTouchGateway,
    val bridge: AndroidRuntimeBridge,
) {
    fun awaitTerminal(): AndroidRuntimeSnapshot {
        repeat(500) {
            bridge.state.value.let { snapshot ->
                if (snapshot.state in setOf(
                        ExecutionState.COMPLETED,
                        ExecutionState.FAILED,
                        ExecutionState.CANCELLED,
                        ExecutionState.REJECTED,
                    )
                ) {
                    return snapshot
                }
            }
            Thread.sleep(10)
        }
        error("Runtime state timed out at ${bridge.state.value.state}")
    }

    fun close() {
        bridge.close()
        scope.cancel()
    }
}
