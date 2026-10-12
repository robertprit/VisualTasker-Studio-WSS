package com.visualtasker.wss.emscript.runtime

import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExecutionSessionManager(
    private val scope: CoroutineScope,
    private val runner: DeterministicExecutionRunner,
    private val planner: WorkflowExecutionPlanner = WorkflowExecutionPlanner(),
    private val checkpointStore: ExecutionSessionCheckpointStore? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val sessionIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val lock = Any()
    private val mutableCurrentSession = MutableStateFlow<ExecutionSessionSnapshot?>(null)
    private val sessions = linkedMapOf<ExecutionSessionId, ExecutionSessionSnapshot>()
    private var reservedRequest: ExecutionSessionRequest? = null
    private var activeToken: MutableCancellationToken? = null
    private var activeJob: Job? = null

    val currentSession: StateFlow<ExecutionSessionSnapshot?> = mutableCurrentSession.asStateFlow()

    init {
        checkpointStore?.read()?.takeIf { it.state.isCheckpointActive() }?.let(::recoverInterrupted)
    }

    fun reserve(
        workflowId: String,
        workspaceRevision: Long,
        document: de.visualtasker.workflow.core.WorkspaceDocument,
        entryPointEntityId: String? = null,
        initialVariables: Map<String, ExecutionValue> = emptyMap(),
        providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
        projectId: String? = null,
        workflowRootId: String? = null,
        projectRevision: Long? = null,
    ): ExecutionSessionId? = synchronized(lock) {
        if (mutableCurrentSession.value?.isActive == true || reservedRequest != null) return null
        val sessionId = ExecutionSessionId(sessionIdFactory())
        val request = ExecutionSessionRequest(
            sessionId = sessionId,
            workflowId = workflowId,
            workspaceRevision = workspaceRevision,
            document = document,
            entryPointEntityId = entryPointEntityId,
            initialVariables = initialVariables,
            providerPolicies = providerPolicies,
            projectId = projectId,
            workflowRootId = workflowRootId,
            projectRevision = projectRevision,
        )
        reservedRequest = request
        publish(
            ExecutionSessionSnapshot(
                sessionId = sessionId,
                workflowId = workflowId,
                workspaceRevision = workspaceRevision,
                state = ExecutionSessionState.CREATED,
                providerPolicies = providerPolicies,
                projectId = projectId,
                workflowRootId = workflowRootId,
                projectRevision = projectRevision,
            ),
        )
        sessionId
    }

    fun startReserved(sessionId: ExecutionSessionId): Boolean = synchronized(lock) {
        val request = reservedRequest?.takeIf { it.sessionId == sessionId } ?: return false
        if (activeJob != null || activeToken != null) return false
        reservedRequest = null
        val token = MutableCancellationToken()
        activeToken = token
        transition(sessionId, ExecutionSessionState.STARTING) {
            copy(startedAtEpochMs = clock())
        }
        activeJob = scope.launch {
            execute(request, token)
        }
        true
    }

    fun start(
        workflowId: String,
        workspaceRevision: Long,
        document: de.visualtasker.workflow.core.WorkspaceDocument,
        entryPointEntityId: String? = null,
        initialVariables: Map<String, ExecutionValue> = emptyMap(),
        providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
        projectId: String? = null,
        workflowRootId: String? = null,
        projectRevision: Long? = null,
    ): ExecutionSessionId? {
        val sessionId = reserve(
            workflowId,
            workspaceRevision,
            document,
            entryPointEntityId,
            initialVariables,
            providerPolicies,
            projectId,
            workflowRootId,
            projectRevision,
        ) ?: return null
        return sessionId.takeIf(::startReserved)
    }

    fun stop(
        sessionId: ExecutionSessionId,
        reason: ExecutionCancellationReason,
    ): Boolean = synchronized(lock) {
        val snapshot = sessions[sessionId] ?: return false
        if (!snapshot.isActive) return false
        if (snapshot.state == ExecutionSessionState.CREATED) {
            reservedRequest = null
            transition(sessionId, ExecutionSessionState.CANCELLED) {
                copy(
                    endedAtEpochMs = clock(),
                    cancellationReason = reason,
                    result = cancelledResult(sessionId.value),
                )
            }
            return true
        }
        if (snapshot.state != ExecutionSessionState.STOPPING) {
            transition(sessionId, ExecutionSessionState.STOPPING) {
                copy(cancellationReason = reason)
            }
        }
        activeToken?.cancel()
        true
    }

    fun stopCurrent(reason: ExecutionCancellationReason): Boolean =
        mutableCurrentSession.value?.sessionId?.let { stop(it, reason) } ?: false

    fun session(sessionId: ExecutionSessionId): ExecutionSessionSnapshot? = synchronized(lock) {
        sessions[sessionId]
    }

    private suspend fun execute(request: ExecutionSessionRequest, token: MutableCancellationToken) {
        val runtimeRequest = ExecutionRequest(
            id = request.sessionId.value,
            workflowId = request.workflowId,
            document = request.document,
            entryPointEntityId = request.entryPointEntityId,
            initialVariables = request.initialVariables,
            providerPolicies = request.providerPolicies,
            projectId = request.projectId,
            workflowRootId = request.workflowRootId,
            projectRevision = request.projectRevision,
        )
        val plan = when (val planned = planner.plan(runtimeRequest)) {
            is ExecutionPlanResult.Ready -> planned.plan
            is ExecutionPlanResult.Rejected -> {
                finishPlanningFailure(request, planned.diagnostics)
                return
            }
        }
        synchronized(lock) {
            val current = sessions[request.sessionId] ?: return
            if (current.state == ExecutionSessionState.STOPPING || token.isCancellationRequested) {
                finishCancelled(request.sessionId, token)
                return
            }
            transition(request.sessionId, ExecutionSessionState.RUNNING) {
                copy(executionPlan = plan)
            }
        }
        val result = withContext(dispatcher) {
            runner.execute(
                request = runtimeRequest,
                cancellationToken = token,
                traceObserver = { event -> appendTrace(request.sessionId, event) },
            )
        }
        synchronized(lock) {
            val current = sessions[request.sessionId] ?: return@synchronized
            val terminal = when (result.state) {
                ExecutionState.COMPLETED -> ExecutionSessionState.COMPLETED
                ExecutionState.CANCELLED -> ExecutionSessionState.CANCELLED
                ExecutionState.FAILED,
                ExecutionState.REJECTED,
                -> ExecutionSessionState.FAILED
                else -> ExecutionSessionState.FAILED
            }
            current.state.requireTransitionTo(terminal)
            publish(
                current.copy(
                    state = terminal,
                    trace = mergeTrace(current.trace, result.trace),
                    endedAtEpochMs = clock(),
                    result = result,
                    cancellationReason = current.cancellationReason
                        ?: if (terminal == ExecutionSessionState.CANCELLED) {
                            ExecutionCancellationReason.PROVIDER_CANCELLATION
                        } else {
                            null
                        },
                ),
            )
            activeToken = null
            activeJob = null
        }
    }

    private fun finishPlanningFailure(
        request: ExecutionSessionRequest,
        diagnostics: List<ExecutionDiagnostic>,
    ) = synchronized(lock) {
        val current = sessions[request.sessionId] ?: return@synchronized
        val result = ExecutionResult(
            requestId = request.sessionId.value,
            state = ExecutionState.REJECTED,
            value = ExecutionValue.NullValue,
            variables = emptyMap(),
            diagnostics = diagnostics,
            trace = diagnostics.mapIndexed { index, diagnostic ->
                ExecutionTraceEvent(
                    index = index + 1,
                    kind = ExecutionTraceEventKind.DIAGNOSTIC,
                    state = ExecutionState.REJECTED,
                    entityId = diagnostic.entityId,
                    diagnostic = diagnostic,
                )
            },
        )
        current.state.requireTransitionTo(ExecutionSessionState.FAILED)
        publish(current.copy(state = ExecutionSessionState.FAILED, endedAtEpochMs = clock(), result = result, trace = result.trace))
        activeToken = null
        activeJob = null
    }

    private fun finishCancelled(sessionId: ExecutionSessionId, token: MutableCancellationToken) {
        val current = sessions[sessionId] ?: return
        if (current.state != ExecutionSessionState.STOPPING) return
        token.cancel()
        current.state.requireTransitionTo(ExecutionSessionState.CANCELLED)
        publish(
            current.copy(
                state = ExecutionSessionState.CANCELLED,
                endedAtEpochMs = clock(),
                result = cancelledResult(sessionId.value),
            ),
        )
        activeToken = null
        activeJob = null
    }

    private fun appendTrace(sessionId: ExecutionSessionId, event: ExecutionTraceEvent) = synchronized(lock) {
        val current = sessions[sessionId] ?: return@synchronized
        publish(current.copy(trace = mergeTrace(current.trace, listOf(event))))
    }

    private fun transition(
        sessionId: ExecutionSessionId,
        next: ExecutionSessionState,
        transform: ExecutionSessionSnapshot.() -> ExecutionSessionSnapshot = { this },
    ) {
        val current = sessions[sessionId] ?: error("Unknown session '${sessionId.value}'.")
        current.state.requireTransitionTo(next)
        publish(current.transform().copy(state = next))
    }

    private fun publish(snapshot: ExecutionSessionSnapshot) {
        sessions[snapshot.sessionId] = snapshot
        while (sessions.size > MAX_RETAINED_SESSIONS) sessions.remove(sessions.keys.first())
        mutableCurrentSession.value = snapshot
        checkpointStore?.write(snapshot.toCheckpoint())
    }

    private fun recoverInterrupted(checkpoint: ExecutionSessionCheckpoint) {
        val sessionId = ExecutionSessionId(checkpoint.sessionId)
        val diagnostic = ExecutionDiagnostic(
            code = "RUNTIME_PROCESS_LOSS",
            message = "Session was interrupted by process loss and was not replayed.",
            entityId = checkpoint.lastEntityId,
        )
        val trace = listOf(
            ExecutionTraceEvent(
                index = checkpoint.traceCount + 1,
                kind = ExecutionTraceEventKind.DIAGNOSTIC,
                state = ExecutionState.FAILED,
                entityId = checkpoint.lastEntityId,
                providerId = checkpoint.lastProviderId,
                diagnostic = diagnostic,
            ),
        )
        val result = ExecutionResult(
            requestId = checkpoint.sessionId,
            state = ExecutionState.FAILED,
            value = ExecutionValue.NullValue,
            variables = emptyMap(),
            diagnostics = listOf(diagnostic),
            trace = trace,
        )
        publish(
            ExecutionSessionSnapshot(
                sessionId = sessionId,
                workflowId = checkpoint.workflowId,
                workspaceRevision = checkpoint.workspaceRevision,
                state = ExecutionSessionState.INTERRUPTED,
                trace = trace,
                startedAtEpochMs = checkpoint.startedAtEpochMs,
                endedAtEpochMs = clock(),
                result = result,
                cancellationReason = ExecutionCancellationReason.PROCESS_LOSS,
                recoveredAfterProcessLoss = true,
                projectId = checkpoint.projectId,
                workflowRootId = checkpoint.workflowRootId,
                projectRevision = checkpoint.projectRevision,
            ),
        )
    }

    private fun cancelledResult(requestId: String) = ExecutionResult(
        requestId = requestId,
        state = ExecutionState.CANCELLED,
        value = ExecutionValue.NullValue,
        variables = emptyMap(),
        diagnostics = emptyList(),
        trace = emptyList(),
    )

    private fun mergeTrace(
        current: List<ExecutionTraceEvent>,
        incoming: List<ExecutionTraceEvent>,
    ): List<ExecutionTraceEvent> = (current + incoming)
        .associateBy(ExecutionTraceEvent::index)
        .toSortedMap()
        .values
        .toList()

    private fun ExecutionSessionState.isCheckpointActive(): Boolean = this in setOf(
        ExecutionSessionState.CREATED,
        ExecutionSessionState.STARTING,
        ExecutionSessionState.RUNNING,
        ExecutionSessionState.STOPPING,
    )

    private companion object {
        const val MAX_RETAINED_SESSIONS = 8
    }
}
