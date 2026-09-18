package com.visualtasker.wss.emscript.runtime

data class ExecutionRunId(
    val value: String,
)

data class ExecutionOperationId(
    val value: String,
)

enum class ExecutionMode {
    DryRun,
    BasicRun,
    LiveRun,
    Replay,
    WatchDog,
}

enum class ExecutionOperationStatus {
    Pending,
    Running,
    Succeeded,
    Warning,
    Failed,
    Skipped,
    Cancelled,
}

enum class ExecutionSourceKind {
    Workflow,
    Block,
    Edge,
    Text,
    Runtime,
    Provider,
    Record,
}

data class ExecutionSourceRef(
    val kind: ExecutionSourceKind,
    val id: String,
    val edgeTargetId: String? = null,
    val edgeKind: String? = null,
)

data class OperationResult(
    val id: ExecutionOperationId,
    val runId: ExecutionRunId,
    val index: Int,
    val kind: String,
    val status: ExecutionOperationStatus,
    val message: String,
    val command: String? = null,
    val capability: String? = null,
    val pluginOwner: String? = null,
    val diagnosticCode: String? = null,
    val sourceLine: Int? = null,
    val sourceRef: ExecutionSourceRef? = null,
    val causedBy: ExecutionOperationId? = null,
    val correlationId: String? = null,
    val evidenceRefs: List<String> = emptyList(),
)

data class ExecutionTrace(
    val runId: ExecutionRunId,
    val mode: ExecutionMode,
    val sourceSessionId: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val completed: Boolean,
    val operations: List<OperationResult>,
    val variables: Map<String, EmscriptValue> = emptyMap(),
    val failureMessage: String? = null,
) {
    val warningCount: Int = operations.count { it.status == ExecutionOperationStatus.Warning }
    val errorCount: Int = operations.count { it.status == ExecutionOperationStatus.Failed }
    val eventCount: Int = operations.size
}

fun EmscriptDryRunResult.toExecutionTrace(
    runId: ExecutionRunId,
    mode: ExecutionMode,
    sourceSessionId: String,
    startedAtEpochMs: Long = System.currentTimeMillis(),
    completedAtEpochMs: Long? = startedAtEpochMs,
): ExecutionTrace =
    when (this) {
        is EmscriptDryRunResult.Success -> ExecutionTrace(
            runId = runId,
            mode = mode,
            sourceSessionId = sourceSessionId,
            startedAtEpochMs = startedAtEpochMs,
            completedAtEpochMs = completedAtEpochMs,
            completed = true,
            operations = events.toOperationResults(runId),
            variables = variables,
        )

        is EmscriptDryRunResult.Failure -> ExecutionTrace(
            runId = runId,
            mode = mode,
            sourceSessionId = sourceSessionId,
            startedAtEpochMs = startedAtEpochMs,
            completedAtEpochMs = completedAtEpochMs,
            completed = false,
            operations = events.toOperationResults(runId),
            failureMessage = message,
        )
    }

private fun List<EmscriptDryRunEvent>.toOperationResults(runId: ExecutionRunId): List<OperationResult> =
    map { event ->
        OperationResult(
            id = ExecutionOperationId("${runId.value}:op:${event.index}"),
            runId = runId,
            index = event.index,
            kind = event.kind,
            status = event.toOperationStatus(),
            message = event.message,
            command = event.command,
            capability = event.capability,
            pluginOwner = event.pluginOwner,
            diagnosticCode = event.diagnosticCode,
            sourceLine = event.sourceLine,
            sourceRef = event.toSourceRef(),
            correlationId = event.blockId
                ?: event.edgeSourceBlockId?.let { source -> "$source->${event.edgeTargetBlockId.orEmpty()}" },
        )
    }

private fun EmscriptDryRunEvent.toOperationStatus(): ExecutionOperationStatus =
    when {
        kind == "edge" -> ExecutionOperationStatus.Succeeded
        severity == EmscriptDryRunEventSeverity.ERROR -> ExecutionOperationStatus.Failed
        severity == EmscriptDryRunEventSeverity.WARNING -> ExecutionOperationStatus.Warning
        kind == "unsupported" -> ExecutionOperationStatus.Skipped
        else -> ExecutionOperationStatus.Succeeded
    }

private fun EmscriptDryRunEvent.toSourceRef(): ExecutionSourceRef? =
    when {
        blockId != null -> ExecutionSourceRef(
            kind = ExecutionSourceKind.Block,
            id = blockId,
        )

        edgeSourceBlockId != null -> ExecutionSourceRef(
            kind = ExecutionSourceKind.Edge,
            id = edgeSourceBlockId,
            edgeTargetId = edgeTargetBlockId,
            edgeKind = edgeKind,
        )

        else -> null
    }
