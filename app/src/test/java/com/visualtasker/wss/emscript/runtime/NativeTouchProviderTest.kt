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

        val result = provider(gateway).invoke(invocation(120.0, 240.0), context())

        assertTrue(result is ProviderResult.Success)
        assertEquals(120f to 240f, gateway.lastCoordinates)
    }

    @Test
    fun invalidCoordinatesAreRejectedBeforeDispatch() {
        val gateway = FakeTouchGateway()

        val result = provider(gateway).invoke(
            invocation(arguments = listOf(ExecutionValue.StringValue("x"), ExecutionValue.NumberValue(Double.NaN))),
            context(),
        )

        assertFailure(result, "TOUCH_COORDINATES_REQUIRED")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun coordinatesOutsidePhysicalDisplayAreRejected() {
        val gateway = FakeTouchGateway(bounds = TouchDisplayBounds(0, 0, 1080, 2400))

        val result = provider(gateway).invoke(invocation(1080.0, 2400.0), context())

        assertFailure(result, "TOUCH_COORDINATES_OUT_OF_BOUNDS")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun invalidPhysicalDisplayBoundsAreRejected() {
        val gateway = FakeTouchGateway(bounds = TouchDisplayBounds(0, 0, 0, 2400))

        val result = provider(gateway).invoke(invocation(), context())

        assertFailure(result, "TOUCH_DISPLAY_UNAVAILABLE")
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun repeatCountOtherThanOneIsRejected() {
        val gateway = FakeTouchGateway()
        val repeated = invocation(
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
        val gateway = FakeTouchGateway(connected = false)

        val result = provider(gateway).invoke(invocation(), context())

        assertFailure(result, "ACCESSIBILITY_SERVICE_UNAVAILABLE")
    }

    @Test
    fun unavailableGestureCapabilityReturnsDiagnostic() {
        val gateway = FakeTouchGateway(canPerformGestures = false)

        val result = provider(gateway).invoke(invocation(), context())

        assertFailure(result, "ACCESSIBILITY_GESTURES_UNAVAILABLE")
    }

    @Test
    fun dispatchRejectionReturnsTypedFailure() {
        val gateway = FakeTouchGateway(dispatchAccepted = false)

        val result = provider(gateway).invoke(invocation(), context())

        assertFailure(result, "TOUCH_DISPATCH_REJECTED")
    }

    @Test
    fun successfulCallbackCompletesExactlyOnce() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Completed)

        val result = provider(gateway).invoke(invocation(), context())

        assertTrue(result is ProviderResult.Success)
        assertEquals(1, gateway.dispatchCount)
    }

    @Test
    fun androidCancelledCallbackReturnsTypedFailure() {
        val gateway = FakeTouchGateway(autoResult = TouchGestureResult.Cancelled)

        val result = provider(gateway).invoke(invocation(), context())

        assertFailure(result, "TOUCH_GESTURE_CANCELLED_BY_ANDROID")
    }

    @Test
    fun cancellationBeforeDispatchDoesNotSubmitGesture() {
        val gateway = FakeTouchGateway()
        val token = MutableCancellationToken().also(MutableCancellationToken::cancel)

        val result = provider(gateway).invoke(invocation(), context(token))

        assertTrue(result is ProviderResult.Cancelled)
        assertFalse(gateway.wasDispatched)
    }

    @Test
    fun cancellationDuringGestureReturnsCancelled() {
        val gateway = FakeTouchGateway()
        val token = MutableCancellationToken()
        val result = AtomicReference<ProviderResult>()
        val thread = Thread { result.set(provider(gateway).invoke(invocation(), context(token))) }.apply { start() }
        assertTrue(gateway.awaitDispatch())

        token.cancel()
        thread.join(1_000)

        assertTrue(result.get() is ProviderResult.Cancelled)
        assertFalse(thread.isAlive)
    }

    @Test
    fun callbackAfterCancellationCannotPublishSuccess() {
        val gateway = FakeTouchGateway()
        val token = MutableCancellationToken()
        val result = AtomicReference<ProviderResult>()
        val thread = Thread { result.set(provider(gateway).invoke(invocation(), context(token))) }.apply { start() }
        assertTrue(gateway.awaitDispatch())

        token.cancel()
        thread.join(1_000)
        gateway.complete(TouchGestureResult.Completed)

        assertTrue(result.get() is ProviderResult.Cancelled)
    }

    @Test
    fun parallelGestureIsRejectedWhileFirstGestureIsActive() {
        val gateway = FakeTouchGateway()
        val provider = provider(gateway)
        val firstToken = MutableCancellationToken()
        val firstResult = AtomicReference<ProviderResult>()
        val firstThread = Thread {
            firstResult.set(provider.invoke(invocation(), context(firstToken)))
        }.apply { start() }
        assertTrue(gateway.awaitDispatch())

        val secondResult = provider.invoke(invocation(), context())
        firstToken.cancel()
        firstThread.join(1_000)

        assertFailure(secondResult, "TOUCH_GESTURE_BUSY")
        assertTrue(firstResult.get() is ProviderResult.Cancelled)
    }

    @Test
    fun interruptedWorkerReturnsCancellationWithoutWaitingForTimeout() {
        val gateway = FakeTouchGateway()
        val result = AtomicReference<ProviderResult>()
        val thread = Thread {
            result.set(provider(gateway).invoke(invocation(), context()))
        }.apply { start() }
        assertTrue(gateway.awaitDispatch())

        thread.interrupt()
        thread.join(1_000)

        assertTrue(result.get() is ProviderResult.Cancelled)
        assertFalse(thread.isAlive)
    }

    @Test
    fun duplicateCallbackCannotOverrideFirstResult() {
        val gateway = FakeTouchGateway(callbackSequence = listOf(TouchGestureResult.Completed, TouchGestureResult.Cancelled))

        val result = provider(gateway).invoke(invocation(), context())

        assertTrue(result is ProviderResult.Success)
    }

    @Test
    fun gatewayExceptionBecomesRuntimeDiagnostic() {
        val gateway = FakeTouchGateway(dispatchError = IllegalStateException("service died"))

        val result = provider(gateway).invoke(invocation(), context())

        assertFailure(result, "TOUCH_DISPATCH_FAILED")
    }

    @Test
    fun waitClickWaitExecutesInOrderWithStableEntityIds() {
        val fixture = runtimeFixture()

        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        val completed = fixture.awaitTerminal()
        val startedEntities = completed.trace
            .filter { it.kind == ExecutionTraceEventKind.STEP_STARTED }
            .mapNotNull { it.entityId }
        val expectedEntities = fixture.document.statementIdsInOrder()

        assertEquals(ExecutionState.COMPLETED, completed.state)
        assertEquals(expectedEntities, startedEntities)
        assertEquals(1, fixture.gateway.dispatchCount)
        fixture.close()
    }

    @Test
    fun nativeTouchRunDoesNotMutateWorkspace() {
        val fixture = runtimeFixture()
        val before = WorkflowSerializer.serialize(fixture.document)

        fixture.bridge.start(fixture.document, fixture.document.id)
        fixture.awaitTerminal()

        assertEquals(before, WorkflowSerializer.serialize(fixture.document))
        fixture.close()
    }

    @Test
    fun completedTouchWorkflowCanStartAgain() {
        val fixture = runtimeFixture()

        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        assertEquals(ExecutionState.COMPLETED, fixture.awaitTerminal().state)
        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        assertEquals(ExecutionState.COMPLETED, fixture.awaitTerminal().state)
        assertEquals(2, fixture.gateway.dispatchCount)
        fixture.close()
    }

    private fun runtimeFixture(): RuntimeFixture {
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
        val document = import("wait(1)\nclickPoint(120, 240)\nwait(1)")
        return RuntimeFixture(scope, document, gateway, AndroidRuntimeBridge(scope, runner))
    }

    private fun import(script: String) = EmscriptWorkspaceImporter().import(
        script = script,
        workspaceId = "m5-2-native-touch-test",
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

    private fun provider(gateway: TouchGestureGateway) = NativeTouchProvider(
        gateway = gateway,
        callbackTimeoutMs = 500,
        cancellationPollMs = 2,
    )

    private fun invocation(x: Double = 120.0, y: Double = 240.0): ProviderInvocation = invocation(
        arguments = listOf(
            ExecutionValue.NumberValue(x),
            ExecutionValue.NumberValue(y),
            ExecutionValue.NumberValue(1.0),
        ),
    )

    private fun invocation(arguments: List<ExecutionValue>) = ProviderInvocation(
        requestId = "request",
        workflowId = "workflow",
        entityId = "click-block",
        commandId = "input.clickPoint",
        capability = NativeTouchProvider.A11Y_CAPABILITY,
        arguments = arguments,
    )

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
    private val connected: Boolean = true,
    private val canPerformGestures: Boolean = true,
    private val bounds: TouchDisplayBounds? = TouchDisplayBounds(0, 0, 1080, 2400),
    private val dispatchAccepted: Boolean = true,
    private val autoResult: TouchGestureResult? = null,
    private val callbackSequence: List<TouchGestureResult> = emptyList(),
    private val dispatchError: Throwable? = null,
) : TouchGestureGateway {
    private val dispatched = CountDownLatch(1)
    private var callback: ((TouchGestureResult) -> Unit)? = null
    var dispatchCount: Int = 0
        private set
    var lastCoordinates: Pair<Float, Float>? = null
        private set
    val wasDispatched: Boolean
        get() = dispatchCount > 0

    override fun availability() = TouchAvailability(connected, canPerformGestures, bounds)

    override fun dispatchClick(x: Float, y: Float, onResult: (TouchGestureResult) -> Unit): Boolean {
        dispatchError?.let { throw it }
        dispatchCount += 1
        lastCoordinates = x to y
        callback = onResult
        dispatched.countDown()
        if (!dispatchAccepted) return false
        callbackSequence.forEach(onResult)
        autoResult?.let(onResult)
        return true
    }

    fun awaitDispatch(): Boolean = dispatched.await(1, TimeUnit.SECONDS)

    fun complete(result: TouchGestureResult) {
        callback?.invoke(result)
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
