package com.visualtasker.wss.emscript.runtime

fun AndroidRuntimeSnapshot.toExecutionTrace(
    startedAtEpochMs: Long = 0L,
): ExecutionTrace? {
    val currentRequestId = requestId ?: return null
    val operations = trace.map { event ->
        OperationResult(
            id = ExecutionOperationId("$currentRequestId:op:${event.index}"),
            runId = ExecutionRunId(currentRequestId),
            index = event.index,
            kind = event.kind.name.lowercase(),
            status = event.toOperationStatus(),
            message = event.toOperationMessage(),
            command = event.kind.takeIf {
                it == ExecutionTraceEventKind.PROVIDER_STARTED || it == ExecutionTraceEventKind.PROVIDER_FINISHED
            }?.let { event.capability?.value },
            capability = event.capability?.value,
            pluginOwner = event.providerId,
            diagnosticCode = event.diagnostic?.code,
            sourceRef = event.entityId?.let { entityId ->
                ExecutionSourceRef(kind = ExecutionSourceKind.Block, id = entityId)
            },
            correlationId = event.entityId ?: event.relationId,
        )
    }
    val finalResult = result
    return ExecutionTrace(
        runId = ExecutionRunId(currentRequestId),
        mode = ExecutionMode.LiveRun,
        sourceSessionId = workflowId ?: "workspace-runtime",
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = if (state.isTerminal()) System.currentTimeMillis() else null,
        completed = state == ExecutionState.COMPLETED,
        operations = operations,
        variables = finalResult?.variables.orEmpty().mapValues { (_, value) -> value.toEmscriptValue() },
        failureMessage = finalResult?.diagnostics?.joinToString("\n") { it.message }
            ?.takeIf(String::isNotBlank),
    )
}

private fun ExecutionTraceEvent.toOperationStatus(): ExecutionOperationStatus = when {
    kind == ExecutionTraceEventKind.CANCELLED -> ExecutionOperationStatus.Cancelled
    kind == ExecutionTraceEventKind.DIAGNOSTIC -> ExecutionOperationStatus.Failed
    state == ExecutionState.FAILED || state == ExecutionState.REJECTED -> ExecutionOperationStatus.Failed
    kind == ExecutionTraceEventKind.STEP_STARTED || kind == ExecutionTraceEventKind.PROVIDER_STARTED ->
        ExecutionOperationStatus.Running
    else -> ExecutionOperationStatus.Succeeded
}

private fun ExecutionTraceEvent.toOperationMessage(): String = when (kind) {
    ExecutionTraceEventKind.STATE_CHANGED -> "Runtime state: $state"
    ExecutionTraceEventKind.EXECUTION_STARTED -> "Workflow execution started"
    ExecutionTraceEventKind.EXECUTION_FINISHED -> "Workflow execution finished: $state"
    ExecutionTraceEventKind.STEP_STARTED -> "Step started: ${entityId.orEmpty()}"
    ExecutionTraceEventKind.STEP_FINISHED -> "Step finished: ${entityId.orEmpty()}"
    ExecutionTraceEventKind.BRANCH_SELECTED -> "Branch selected: ${relationId.orEmpty()}"
    ExecutionTraceEventKind.PROVIDER_STARTED -> "Provider started: ${providerId.orEmpty()}"
    ExecutionTraceEventKind.PROVIDER_FINISHED -> "Provider finished: ${providerId.orEmpty()}"
    ExecutionTraceEventKind.DIAGNOSTIC -> diagnostic?.message ?: "Runtime diagnostic"
    ExecutionTraceEventKind.CANCELLED -> "Workflow execution cancelled"
}

private fun ExecutionValue.toEmscriptValue(): EmscriptValue = when (this) {
    is ExecutionValue.NumberValue -> EmscriptValue.NumberValue(value)
    is ExecutionValue.StringValue -> EmscriptValue.StringValue(value)
    is ExecutionValue.BooleanValue -> EmscriptValue.BooleanValue(value)
    is ExecutionValue.ListValue -> EmscriptValue.NullValue
    ExecutionValue.NullValue,
    ExecutionValue.VoidValue,
    -> EmscriptValue.NullValue
}

private fun ExecutionState.isTerminal(): Boolean = this in setOf(
    ExecutionState.COMPLETED,
    ExecutionState.FAILED,
    ExecutionState.CANCELLED,
    ExecutionState.REJECTED,
)
