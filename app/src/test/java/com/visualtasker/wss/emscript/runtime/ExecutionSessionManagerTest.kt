package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.emscript.contract.ProviderId
import de.visualtasker.workflow.serialization.WorkflowSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionSessionManagerTest {
    @Test
    fun startCompletesWithStableSessionPlanRevisionAndResult() {
        val fixture = fixture("wait(1)")
        val before = WorkflowSerializer.serialize(fixture.document)

        val sessionId = requireNotNull(fixture.manager.start("workflow", 42L, fixture.document))
        val completed = fixture.awaitTerminal()

        assertEquals(sessionId, completed.sessionId)
        assertEquals(42L, completed.workspaceRevision)
        assertEquals(fixture.document.version, completed.executionPlan?.sourceRevision)
        assertEquals(ExecutionSessionState.COMPLETED, completed.state)
        assertEquals(ExecutionState.COMPLETED, completed.result?.state)
        assertEquals(before, WorkflowSerializer.serialize(fixture.document))
        fixture.close()
    }

    @Test
    fun detachAndReattachObserveSameSessionWithoutRestartOrDuplicateTrace() {
        val fixture = fixture("wait(100)")
        val sessionId = requireNotNull(fixture.manager.start("workflow", 7L, fixture.document))
        fixture.awaitState(ExecutionSessionState.RUNNING)

        val detachedSnapshot = fixture.manager.currentSession.value
        val reattachedSnapshot = fixture.manager.session(sessionId)
        val completed = fixture.awaitTerminal()

        assertSame(detachedSnapshot, reattachedSnapshot)
        assertEquals(sessionId, completed.sessionId)
        assertEquals(completed.trace.map { it.index }.distinct(), completed.trace.map { it.index })
        fixture.close()
    }

    @Test
    fun duplicateStartIsRejectedButNewSessionStartsAfterTerminal() {
        val fixture = fixture("wait(100)")
        val first = requireNotNull(fixture.manager.start("workflow", 1L, fixture.document))

        assertNull(fixture.manager.start("workflow", 1L, fixture.document))
        fixture.awaitTerminal()
        val second = requireNotNull(fixture.manager.start("workflow", 1L, fixture.document))

        assertNotEquals(first, second)
        assertEquals(ExecutionSessionState.COMPLETED, fixture.awaitTerminal().state)
        fixture.close()
    }

    @Test
    fun studioStopCancelsWaitAndRetainsExplicitReason() {
        val fixture = fixture("wait(5000)")
        val sessionId = requireNotNull(fixture.manager.start("workflow", 1L, fixture.document))
        fixture.awaitState(ExecutionSessionState.RUNNING)

        assertTrue(fixture.manager.stop(sessionId, ExecutionCancellationReason.USER_STOP))
        val cancelled = fixture.awaitTerminal()

        assertEquals(ExecutionSessionState.CANCELLED, cancelled.state)
        assertEquals(ExecutionCancellationReason.USER_STOP, cancelled.cancellationReason)
        assertEquals(ExecutionState.CANCELLED, cancelled.result?.state)
        fixture.close()
    }

    @Test
    fun notificationStopAndReservedStopAreDeterministic() {
        val fixture = fixture("wait(5000)")
        val reserved = requireNotNull(fixture.manager.reserve("workflow", 1L, fixture.document))

        assertTrue(fixture.manager.stop(reserved, ExecutionCancellationReason.NOTIFICATION_STOP))
        assertEquals(ExecutionSessionState.CANCELLED, fixture.manager.session(reserved)?.state)
        assertEquals(ExecutionCancellationReason.NOTIFICATION_STOP, fixture.manager.session(reserved)?.cancellationReason)
        assertFalse(fixture.manager.startReserved(reserved))
        fixture.close()
    }

    @Test
    fun staleSessionCannotStopActiveSession() {
        val fixture = fixture("wait(5000)")
        val active = requireNotNull(fixture.manager.start("workflow", 1L, fixture.document))
        fixture.awaitState(ExecutionSessionState.RUNNING)

        assertFalse(fixture.manager.stop(ExecutionSessionId("missing"), ExecutionCancellationReason.USER_STOP))
        assertTrue(fixture.manager.stop(active, ExecutionCancellationReason.USER_STOP))
        fixture.awaitTerminal()
        fixture.close()
    }

    @Test
    fun providerPolicySurvivesSessionAndAppearsInTrace() {
        val fixture = fixture("wait(1)")
        val policy = ProviderSelectionPolicy(
            explicitProviderId = ProviderId(AndroidWaitExecutionProvider.PROVIDER_ID),
        )
        requireNotNull(
            fixture.manager.start(
                workflowId = "workflow",
                workspaceRevision = 3L,
                document = fixture.document,
                providerPolicies = mapOf(AndroidWaitExecutionProvider.TIMING_CAPABILITY to policy),
            ),
        )

        val completed = fixture.awaitTerminal()

        assertEquals(policy, completed.providerPolicies[AndroidWaitExecutionProvider.TIMING_CAPABILITY])
        assertTrue(completed.trace.any { it.providerTrace?.selectionReason == ProviderSelectionReason.EXPLICIT.name })
        fixture.close()
    }

    @Test
    fun headlessSessionProjectsProviderProvenanceIntoRailTrace() {
        val fixture = fixture("wait(1)")
        val sessionId = requireNotNull(fixture.manager.start("workflow", 4L, fixture.document))
        val completed = fixture.awaitTerminal()

        val trace = completed.toExecutionTrace()
        val railSteps = trace?.toRailTraceSteps().orEmpty()

        assertNotNull(trace)
        assertEquals(sessionId.value, trace?.runId?.value)
        assertEquals(completed.startedAtEpochMs, trace?.startedAtEpochMs)
        assertEquals(completed.endedAtEpochMs, trace?.completedAtEpochMs)
        assertTrue(
            railSteps.any { step ->
                step.properties["providerId"] == AndroidWaitExecutionProvider.PROVIDER_ID &&
                    step.properties["providerSelectionReason"] == ProviderSelectionReason.PRIORITY.name &&
                    step.properties["providerResultStatus"] == "SUCCEEDED"
            },
        )
        fixture.close()
    }

    @Test
    fun editorChangesDoNotRecompileFrozenSessionInput() {
        val fixture = fixture("wait(80)")
        val originalVersion = fixture.document.version
        val sessionId = requireNotNull(fixture.manager.start("workflow", 11L, fixture.document))
        fixture.awaitState(ExecutionSessionState.RUNNING)
        val independentlyEdited = import("wait(1)\nwait(1)")

        assertNotEquals(originalVersion, independentlyEdited.version)
        val completed = fixture.awaitTerminal()
        assertEquals(sessionId, completed.sessionId)
        assertEquals(originalVersion, completed.executionPlan?.sourceRevision)
        assertEquals(11L, completed.workspaceRevision)
        fixture.close()
    }

    @Test
    fun processLossRecoversInterruptedWithoutReplay() {
        val checkpointStore = MemoryCheckpointStore(
            ExecutionSessionCheckpoint(
                sessionId = "lost-session",
                workflowId = "workflow",
                workspaceRevision = 9L,
                state = ExecutionSessionState.RUNNING,
                startedAtEpochMs = 100L,
                endedAtEpochMs = null,
                traceCount = 4,
                lastEntityId = "wait-block",
                lastProviderId = AndroidWaitExecutionProvider.PROVIDER_ID,
                cancellationReason = null,
            ),
        )
        var providerCalls = 0
        val provider = AndroidWaitExecutionProvider(sleep = { providerCalls += 1 })
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = manager(scope, provider, checkpointStore)

        val recovered = requireNotNull(manager.currentSession.value)

        assertEquals(ExecutionSessionState.INTERRUPTED, recovered.state)
        assertEquals(ExecutionCancellationReason.PROCESS_LOSS, recovered.cancellationReason)
        assertTrue(recovered.recoveredAfterProcessLoss)
        assertEquals("RUNTIME_PROCESS_LOSS", recovered.result?.diagnostics?.single()?.code)
        assertEquals(0, providerCalls)
        scope.cancel()
    }

    @Test
    fun terminalCheckpointDoesNotCreateRecoveredActiveSession() {
        val store = MemoryCheckpointStore(
            ExecutionSessionCheckpoint(
                sessionId = "done",
                workflowId = "workflow",
                workspaceRevision = 1L,
                state = ExecutionSessionState.COMPLETED,
                startedAtEpochMs = 1L,
                endedAtEpochMs = 2L,
                traceCount = 3,
                lastEntityId = null,
                lastProviderId = null,
                cancellationReason = null,
            ),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = manager(scope, AndroidWaitExecutionProvider(sleep = {}), store)

        assertNull(manager.currentSession.value)
        scope.cancel()
    }

    @Test
    fun invalidWorkflowFailsBeforeRuntimeAndPublishesTypedResult() {
        val fixture = fixture("wait(1)")
        val invalid = fixture.document.copy(blocks = emptyMap())
        requireNotNull(fixture.manager.start("workflow", 1L, invalid))

        val failed = fixture.awaitTerminal()

        assertEquals(ExecutionSessionState.FAILED, failed.state)
        assertEquals(ExecutionState.REJECTED, failed.result?.state)
        assertTrue(failed.result?.diagnostics?.isNotEmpty() == true)
        fixture.close()
    }

    @Test
    fun terminalSessionStateCannotTransitionBackToRunning() {
        val failure = runCatching {
            ExecutionSessionState.COMPLETED.requireTransitionTo(ExecutionSessionState.RUNNING)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun headlessSessionRetainsProjectAndRootBinding() {
        val fixture = fixture("wait(1)")
        val sessionId = requireNotNull(
            fixture.manager.start(
                workflowId = "root:main",
                workspaceRevision = fixture.document.version,
                document = fixture.document,
                projectId = "project:demo",
                workflowRootId = "root:main",
                projectRevision = 12L,
            ),
        )
        val completed = fixture.awaitTerminal()

        assertEquals(sessionId, completed.sessionId)
        assertEquals("project:demo", completed.projectId)
        assertEquals("root:main", completed.workflowRootId)
        assertEquals(12L, completed.projectRevision)
        assertEquals("project:demo", completed.executionPlan?.projectId)
        assertEquals("root:main", completed.executionPlan?.workflowRootId)
        fixture.close()
    }

    private fun fixture(script: String): Fixture {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val provider = AndroidWaitExecutionProvider()
        return Fixture(scope, import(script), manager(scope, provider, MemoryCheckpointStore()))
    }

    private fun manager(
        scope: CoroutineScope,
        provider: ExecutionProvider,
        store: ExecutionSessionCheckpointStore,
    ): ExecutionSessionManager {
        val planner = WorkflowExecutionPlanner()
        return ExecutionSessionManager(
            scope = scope,
            runner = DeterministicExecutionRunner(planner, ExecutionProviderRegistry(listOf(provider))),
            planner = planner,
            checkpointStore = store,
        )
    }

    private fun import(script: String) = EmscriptWorkspaceImporter().import(script, "m5-h-session").let {
        assertTrue(it.issues.joinToString { issue -> issue.message }, it.isSuccess)
        requireNotNull(it.document)
    }

    private data class Fixture(
        val scope: CoroutineScope,
        val document: de.visualtasker.workflow.core.WorkspaceDocument,
        val manager: ExecutionSessionManager,
    ) {
        fun awaitState(state: ExecutionSessionState): ExecutionSessionSnapshot = await { it.state == state }

        fun awaitTerminal(): ExecutionSessionSnapshot = await { !it.isActive && it.result != null }

        fun close() {
            manager.currentSession.value?.takeIf(ExecutionSessionSnapshot::isActive)?.let {
                manager.stop(it.sessionId, ExecutionCancellationReason.USER_STOP)
                awaitTerminal()
            }
            scope.cancel()
        }

        private fun await(predicate: (ExecutionSessionSnapshot) -> Boolean): ExecutionSessionSnapshot {
            repeat(800) {
                manager.currentSession.value?.let { snapshot -> if (predicate(snapshot)) return snapshot }
                Thread.sleep(10)
            }
            error("Session timed out at ${manager.currentSession.value}")
        }
    }

    private class MemoryCheckpointStore(
        var checkpoint: ExecutionSessionCheckpoint? = null,
    ) : ExecutionSessionCheckpointStore {
        override fun read(): ExecutionSessionCheckpoint? = checkpoint

        override fun write(checkpoint: ExecutionSessionCheckpoint) {
            this.checkpoint = checkpoint
        }
    }
}
