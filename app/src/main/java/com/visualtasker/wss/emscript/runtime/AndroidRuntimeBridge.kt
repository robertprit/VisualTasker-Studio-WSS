package com.visualtasker.wss.emscript.runtime

import de.visualtasker.workflow.core.WorkspaceDocument
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

data class AndroidRuntimeSnapshot(
    val state: ExecutionState = ExecutionState.READY,
    val requestId: String? = null,
    val workflowId: String? = null,
    val activeEntityId: String? = null,
    val trace: List<ExecutionTraceEvent> = emptyList(),
    val result: ExecutionResult? = null,
    val stateHistory: List<ExecutionState> = listOf(ExecutionState.READY),
) {
    val isRunning: Boolean
        get() = state == ExecutionState.RUNNING

    val hasActiveRequest: Boolean
        get() = requestId != null && result == null && state !in setOf(
            ExecutionState.COMPLETED,
            ExecutionState.FAILED,
            ExecutionState.CANCELLED,
            ExecutionState.REJECTED,
        )
}

class AndroidRuntimeBridge(
    private val scope: CoroutineScope,
    private val runner: DeterministicExecutionRunner = DeterministicExecutionRunner(
        providers = ExecutionProviderRegistry(
            listOf(
                AndroidWaitExecutionProvider(),
                NativeTouchProvider(),
            ),
        ),
    ),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
) : AutoCloseable {
    private val lock = Any()
    private val mutableState = MutableStateFlow(AndroidRuntimeSnapshot())
    private var activeToken: MutableCancellationToken? = null
    private var activeJob: Job? = null
    private var closed = false

    val state: StateFlow<AndroidRuntimeSnapshot> = mutableState.asStateFlow()

    fun start(
        document: WorkspaceDocument,
        workflowId: String,
        entryPointEntityId: String? = null,
    ): Boolean = synchronized(lock) {
        if (closed || activeToken != null) return false
        val requestId = requestIdFactory()
        val token = MutableCancellationToken()
        activeToken = token
        mutableState.value = AndroidRuntimeSnapshot(
            state = ExecutionState.READY,
            requestId = requestId,
            workflowId = workflowId,
            stateHistory = appendState(mutableState.value.stateHistory, ExecutionState.READY),
        )
        activeJob = scope.launch {
            transition(requestId, ExecutionState.RUNNING)
            val result = withContext(dispatcher) {
                runner.execute(
                    request = ExecutionRequest(
                        id = requestId,
                        workflowId = workflowId,
                        document = document,
                        entryPointEntityId = entryPointEntityId,
                    ),
                    cancellationToken = token,
                    traceObserver = { event -> publishTrace(requestId, event) },
                )
            }
            synchronized(lock) {
                if (mutableState.value.requestId == requestId) {
                    mutableState.value = mutableState.value.copy(
                        state = result.state,
                        activeEntityId = result.trace.lastOrNull { it.entityId != null }?.entityId,
                        trace = result.trace,
                        result = result,
                        stateHistory = appendState(mutableState.value.stateHistory, result.state),
                    )
                }
                if (activeToken === token) {
                    activeToken = null
                    activeJob = null
                }
            }
        }
        true
    }

    fun stop(): Boolean = synchronized(lock) {
        val token = activeToken ?: return false
        token.cancel()
        true
    }

    override fun close() {
        synchronized(lock) {
            closed = true
            activeToken?.cancel()
        }
    }

    private fun transition(requestId: String, next: ExecutionState) {
        synchronized(lock) {
            if (mutableState.value.requestId != requestId) return
            mutableState.value = mutableState.value.copy(
                state = next,
                stateHistory = appendState(mutableState.value.stateHistory, next),
            )
        }
    }

    private fun publishTrace(requestId: String, event: ExecutionTraceEvent) {
        synchronized(lock) {
            if (mutableState.value.requestId != requestId) return
            mutableState.value = mutableState.value.copy(
                state = event.state.takeIf { it != ExecutionState.CREATED } ?: mutableState.value.state,
                activeEntityId = event.entityId ?: mutableState.value.activeEntityId,
                trace = mutableState.value.trace + event,
                stateHistory = appendState(mutableState.value.stateHistory, event.state),
            )
        }
    }
}

private fun appendState(history: List<ExecutionState>, state: ExecutionState): List<ExecutionState> =
    if (history.lastOrNull() == state) history else history + state
