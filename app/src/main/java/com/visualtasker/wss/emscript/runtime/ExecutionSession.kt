package com.visualtasker.wss.emscript.runtime

import de.visualtasker.workflow.core.WorkspaceDocument

@JvmInline
value class ExecutionSessionId(val value: String)

enum class ExecutionSessionState {
    CREATED,
    STARTING,
    RUNNING,
    STOPPING,
    COMPLETED,
    FAILED,
    CANCELLED,
    INTERRUPTED,
}

enum class ExecutionCancellationReason {
    USER_STOP,
    NOTIFICATION_STOP,
    SERVICE_STOP,
    PROVIDER_CANCELLATION,
    PROCESS_LOSS,
    START_FAILURE,
}

data class ExecutionSessionRequest(
    val sessionId: ExecutionSessionId,
    val workflowId: String,
    val workspaceRevision: Long,
    val document: WorkspaceDocument,
    val entryPointEntityId: String? = null,
    val initialVariables: Map<String, ExecutionValue> = emptyMap(),
    val providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
)

data class ExecutionSessionSnapshot(
    val sessionId: ExecutionSessionId,
    val workflowId: String,
    val workspaceRevision: Long,
    val state: ExecutionSessionState,
    val executionPlan: ExecutionPlan? = null,
    val providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
    val trace: List<ExecutionTraceEvent> = emptyList(),
    val startedAtEpochMs: Long? = null,
    val endedAtEpochMs: Long? = null,
    val result: ExecutionResult? = null,
    val cancellationReason: ExecutionCancellationReason? = null,
    val recoveredAfterProcessLoss: Boolean = false,
) {
    val isActive: Boolean
        get() = state in setOf(
            ExecutionSessionState.CREATED,
            ExecutionSessionState.STARTING,
            ExecutionSessionState.RUNNING,
            ExecutionSessionState.STOPPING,
        )

    val activeEntityId: String?
        get() = trace.lastOrNull { it.entityId != null }?.entityId
}

data class ExecutionSessionCheckpoint(
    val sessionId: String,
    val workflowId: String,
    val workspaceRevision: Long,
    val state: ExecutionSessionState,
    val startedAtEpochMs: Long?,
    val endedAtEpochMs: Long?,
    val traceCount: Int,
    val lastEntityId: String?,
    val lastProviderId: String?,
    val cancellationReason: ExecutionCancellationReason?,
)

interface ExecutionSessionCheckpointStore {
    fun read(): ExecutionSessionCheckpoint?
    fun write(checkpoint: ExecutionSessionCheckpoint)
}

internal fun ExecutionSessionSnapshot.toCheckpoint(): ExecutionSessionCheckpoint = ExecutionSessionCheckpoint(
    sessionId = sessionId.value,
    workflowId = workflowId,
    workspaceRevision = workspaceRevision,
    state = state,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    traceCount = trace.size,
    lastEntityId = activeEntityId,
    lastProviderId = trace.lastOrNull { it.providerId != null }?.providerId,
    cancellationReason = cancellationReason,
)

internal fun ExecutionSessionState.requireTransitionTo(next: ExecutionSessionState) {
    require(next in SESSION_TRANSITIONS.getValue(this)) {
        "Invalid execution session transition: $this -> $next"
    }
}

private val SESSION_TRANSITIONS = mapOf(
    ExecutionSessionState.CREATED to setOf(
        ExecutionSessionState.STARTING,
        ExecutionSessionState.CANCELLED,
        ExecutionSessionState.INTERRUPTED,
    ),
    ExecutionSessionState.STARTING to setOf(
        ExecutionSessionState.RUNNING,
        ExecutionSessionState.STOPPING,
        ExecutionSessionState.FAILED,
        ExecutionSessionState.CANCELLED,
        ExecutionSessionState.INTERRUPTED,
    ),
    ExecutionSessionState.RUNNING to setOf(
        ExecutionSessionState.STOPPING,
        ExecutionSessionState.COMPLETED,
        ExecutionSessionState.FAILED,
        ExecutionSessionState.CANCELLED,
        ExecutionSessionState.INTERRUPTED,
    ),
    ExecutionSessionState.STOPPING to setOf(
        ExecutionSessionState.CANCELLED,
        ExecutionSessionState.FAILED,
        ExecutionSessionState.INTERRUPTED,
    ),
    ExecutionSessionState.COMPLETED to emptySet(),
    ExecutionSessionState.FAILED to emptySet(),
    ExecutionSessionState.CANCELLED to emptySet(),
    ExecutionSessionState.INTERRUPTED to emptySet(),
)
