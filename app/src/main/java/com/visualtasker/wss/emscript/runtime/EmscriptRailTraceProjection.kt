package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.StepStatus

fun EmscriptDryRunResult.toRailTraceSteps(
    runId: String,
    mode: ExecutionMode,
    sourceSessionId: String = "workspace-emscript",
    startedAtEpochMs: Long = 0L,
): List<RecorderStepUi> {
    val trace = toExecutionTrace(
        runId = ExecutionRunId(runId),
        mode = mode,
        sourceSessionId = sourceSessionId,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = startedAtEpochMs,
    )
    return trace.toRailTraceSteps(idPrefix = "dry-run")
}

fun ExecutionTrace.toRailTraceSteps(
    idPrefix: String = mode.defaultRailTraceStepPrefix(),
): List<RecorderStepUi> =
    operations.map { operation ->
        RecorderStepUi(
            id = operation.toRailTraceStepId(idPrefix),
            label = "#${operation.index} ${operation.command ?: operation.kind}",
            actionType = operation.kind,
            status = operation.status.toStepStatus(),
            timestampMs = operation.index * 180L,
            durationMs = 140L,
            activityName = operation.status.toRuntimeLaneLabel(this.mode),
            detail = operation.message,
            properties = buildMap {
                put("runId", this@toRailTraceSteps.runId.value)
                put("operationId", operation.id.value)
                put("mode", this@toRailTraceSteps.mode.name)
                put("sourceSessionId", this@toRailTraceSteps.sourceSessionId)
                put("operationStatus", operation.status.name)
                operation.command?.let { put("command", it) }
                operation.capability?.let { put("capability", it) }
                operation.pluginOwner?.let { put("pluginOwner", it) }
                operation.diagnosticCode?.let { put("diagnosticCode", it) }
                operation.correlationId?.let { put("correlationId", it) }
                operation.sourceLine?.let { put("sourceLine", it.toString()) }
                operation.sourceRef?.let { source ->
                    put("sourceKind", source.kind.name)
                    put("sourceId", source.id)
                    source.edgeTargetId?.let { put("edgeTargetId", it) }
                    source.edgeKind?.let { put("edgeKind", it) }
                }
            },
        )
    }

fun String.dryRunEventIndexOrNull(): Int? =
    takeIf { startsWith("dry-run-") }
        ?.removePrefix("dry-run-")
        ?.substringAfterLast(":op:")
        ?.toIntOrNull()

private fun OperationResult.toRailTraceStepId(idPrefix: String): String =
    sourceRef
        ?.takeIf { it.kind == ExecutionSourceKind.Record }
        ?.id
        ?.takeIf { it.startsWith("$idPrefix-") || idPrefix.isBlank() }
        ?: "$idPrefix-${sourceRef?.takeIf { it.kind == ExecutionSourceKind.Record }?.id ?: index}"

private fun ExecutionMode.defaultRailTraceStepPrefix(): String =
    when (this) {
        ExecutionMode.DryRun,
        ExecutionMode.BasicRun,
        ExecutionMode.LiveRun -> "dry-run"
        ExecutionMode.Replay -> "record"
        ExecutionMode.WatchDog -> "watchdog"
    }

private fun ExecutionOperationStatus.toStepStatus(): StepStatus =
    when (this) {
        ExecutionOperationStatus.Pending,
        ExecutionOperationStatus.Running -> StepStatus.Edited
        ExecutionOperationStatus.Succeeded -> StepStatus.Executed
        ExecutionOperationStatus.Warning -> StepStatus.Edited
        ExecutionOperationStatus.Failed -> StepStatus.Invalid
        ExecutionOperationStatus.Skipped -> StepStatus.Invalid
        ExecutionOperationStatus.Cancelled -> StepStatus.Invalid
    }

private fun ExecutionOperationStatus.toRuntimeLaneLabel(mode: ExecutionMode): String =
    when (this) {
        ExecutionOperationStatus.Failed -> "Runtime Fehler"
        ExecutionOperationStatus.Warning -> "Runtime Hinweise"
        else -> when (mode) {
            ExecutionMode.DryRun -> "DryRun Runtime"
            ExecutionMode.BasicRun -> "BasicRun Runtime"
            ExecutionMode.LiveRun -> "Live Runtime"
            ExecutionMode.Replay -> "Replay Runtime"
            ExecutionMode.WatchDog -> "WatchDog Runtime"
        }
    }
