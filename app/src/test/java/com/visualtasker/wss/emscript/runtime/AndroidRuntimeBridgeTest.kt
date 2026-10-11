package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.serialization.WorkflowSerializer
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidRuntimeBridgeTest {
    @Test
    fun nativeWaitRunsThroughBridgeWithoutBlockingCaller() {
        val fixture = fixture("wait(40)")

        val startedAt = System.nanoTime()
        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        val startCallMs = (System.nanoTime() - startedAt) / 1_000_000
        val completed = fixture.awaitTerminal()

        assertTrue("start must not block for wait duration", startCallMs < 30)
        assertEquals(ExecutionState.COMPLETED, completed.state)
        assertTrue(completed.stateHistory.containsAll(listOf(ExecutionState.READY, ExecutionState.RUNNING, ExecutionState.COMPLETED)))
        fixture.close()
    }

    @Test
    fun cancellationInterruptsActiveWaitAndPublishesCancelledState() {
        val fixture = fixture("wait(2000)")
        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        fixture.awaitState(ExecutionState.RUNNING)

        assertTrue(fixture.bridge.stop())
        val cancelled = fixture.awaitTerminal()

        assertEquals(ExecutionState.CANCELLED, cancelled.state)
        assertTrue(cancelled.trace.any { it.kind == ExecutionTraceEventKind.CANCELLED })
        fixture.close()
    }

    @Test
    fun duplicateStartIsRejectedAndRestartAfterCompletionSucceeds() {
        val fixture = fixture("wait(120)")
        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        assertFalse(fixture.bridge.start(fixture.document, fixture.document.id))
        fixture.awaitTerminal()

        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        assertEquals(ExecutionState.COMPLETED, fixture.awaitTerminal().state)
        fixture.close()
    }

    @Test
    fun waitRejectsMalformedAndNegativeDurations() {
        val provider = AndroidWaitExecutionProvider(sleep = {})
        val context = executionContext()

        val missing = provider.invoke(invocation(emptyList()), context)
        val negative = provider.invoke(
            invocation(listOf(ExecutionValue.NumberValue(-1.0))),
            context,
        )

        assertEquals("WAIT_DURATION_REQUIRED", (missing as ProviderResult.Failure).code)
        assertEquals("WAIT_DURATION_INVALID", (negative as ProviderResult.Failure).code)
    }

    @Test
    fun missingCapabilityRemainsTypedFailure() {
        val document = import("wait(1)")
        val result = DeterministicExecutionRunner(
            providers = ExecutionProviderRegistry(emptyList()),
        ).execute(ExecutionRequest("missing", document.id, document))

        assertEquals(ExecutionState.FAILED, result.state)
        assertEquals("CAPABILITY_PROVIDER_MISSING", result.diagnostics.single().code)
    }

    @Test
    fun traceOrderAndCanonicalEntityIdsSurviveAndroidBridge() {
        val fixture = fixture("wait(1)")
        val expectedWaitId = fixture.document.blocks.values.single { it.type.endsWith("wait") }.id.value

        fixture.bridge.start(fixture.document, fixture.document.id)
        val completed = fixture.awaitTerminal()

        assertEquals(completed.trace.indices.map { it + 1 }, completed.trace.map { it.index })
        assertTrue(completed.trace.any { it.kind == ExecutionTraceEventKind.STEP_STARTED && it.entityId == expectedWaitId })
        assertTrue(completed.trace.any { it.kind == ExecutionTraceEventKind.PROVIDER_FINISHED && it.providerId == AndroidWaitExecutionProvider.PROVIDER_ID })
        fixture.close()
    }

    @Test
    fun runtimeDoesNotMutateCanonicalWorkspace() {
        val fixture = fixture("wait(1)")
        val before = WorkflowSerializer.serialize(fixture.document)

        fixture.bridge.start(fixture.document, fixture.document.id)
        fixture.awaitTerminal()

        assertEquals(before, WorkflowSerializer.serialize(fixture.document))
        fixture.close()
    }

    @Test
    fun closeRequestsLifecycleCleanupAndPreventsNewRuns() {
        val fixture = fixture("wait(2000)")
        fixture.bridge.start(fixture.document, fixture.document.id)
        fixture.awaitState(ExecutionState.RUNNING)

        fixture.bridge.close()
        assertEquals(ExecutionState.CANCELLED, fixture.awaitTerminal().state)
        assertFalse(fixture.bridge.start(fixture.document, fixture.document.id))
        fixture.scope.cancel()
    }

    @Test
    fun completedTraceProjectsIntoExistingRailTraceContract() {
        val fixture = fixture("wait(1)")
        fixture.bridge.start(fixture.document, fixture.document.id)
        val completed = fixture.awaitTerminal()

        val trace = completed.toExecutionTrace()
        val railSteps = trace?.toRailTraceSteps().orEmpty()

        assertNotNull(trace)
        assertEquals(ExecutionMode.LiveRun, trace?.mode)
        assertTrue(trace.orEmptyOperations().any { it.sourceRef?.id != null })
        assertTrue(
            railSteps.any { step ->
                step.properties["providerId"] == AndroidWaitExecutionProvider.PROVIDER_ID &&
                    step.properties["providerSelectionReason"] == ProviderSelectionReason.PRIORITY.name &&
                    step.properties["providerResultStatus"] == "SUCCEEDED"
            },
        )
        fixture.close()
    }

    private fun fixture(script: String): Fixture {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return Fixture(
            scope = scope,
            document = import(script),
            bridge = AndroidRuntimeBridge(scope = scope),
        )
    }

    private fun import(script: String) = EmscriptWorkspaceImporter().import(
        script = script,
        workspaceId = "m5-1-android-runtime-test",
    ).let { result ->
        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        requireNotNull(result.document)
    }

    private fun invocation(arguments: List<ExecutionValue>) = ProviderInvocation(
        requestId = "request",
        workflowId = "workflow",
        entityId = "wait-block",
        commandId = "wait",
        capability = AndroidWaitExecutionProvider.TIMING_CAPABILITY,
        arguments = arguments,
    )

    private fun executionContext() = ExecutionContext(
        requestId = "request",
        workflowId = "workflow",
        variables = mutableMapOf(),
        cancellationToken = MutableCancellationToken(),
    )

    private data class Fixture(
        val scope: CoroutineScope,
        val document: de.visualtasker.workflow.core.WorkspaceDocument,
        val bridge: AndroidRuntimeBridge,
    ) {
        fun awaitState(expected: ExecutionState): AndroidRuntimeSnapshot = await { it.state == expected }

        fun awaitTerminal(): AndroidRuntimeSnapshot = await {
            it.state in setOf(
                ExecutionState.COMPLETED,
                ExecutionState.FAILED,
                ExecutionState.CANCELLED,
                ExecutionState.REJECTED,
            ) && it.result != null
        }

        fun close() {
            bridge.close()
            scope.cancel()
        }

        private fun await(predicate: (AndroidRuntimeSnapshot) -> Boolean): AndroidRuntimeSnapshot {
            repeat(500) {
                bridge.state.value.let { snapshot -> if (predicate(snapshot)) return snapshot }
                Thread.sleep(10)
            }
            error("Runtime state timed out at ${bridge.state.value.state}")
        }
    }
}

private fun ExecutionTrace?.orEmptyOperations(): List<OperationResult> = this?.operations.orEmpty()
