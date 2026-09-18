package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.runtime.ExecutionMode
import com.visualtasker.wss.emscript.runtime.ExecutionOperationId
import com.visualtasker.wss.emscript.runtime.ExecutionOperationStatus
import com.visualtasker.wss.emscript.runtime.ExecutionRunId
import com.visualtasker.wss.emscript.runtime.ExecutionSourceKind
import com.visualtasker.wss.emscript.runtime.ExecutionSourceRef
import com.visualtasker.wss.emscript.runtime.ExecutionTrace
import com.visualtasker.wss.emscript.runtime.OperationResult

fun List<RecorderStepUi>.toRecordingExecutionTrace(
    runId: String,
    sourceSessionId: String,
    startedAtEpochMs: Long = firstOrNull()?.timestampMs ?: 0L,
    completedAtEpochMs: Long? = maxOfOrNull { (it.timestampMs ?: 0L) + (it.durationMs ?: 0L) },
): ExecutionTrace {
    val stableRunId = ExecutionRunId(runId)
    return ExecutionTrace(
        runId = stableRunId,
        mode = ExecutionMode.Replay,
        sourceSessionId = sourceSessionId,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        completed = true,
        operations = mapIndexed { index, step ->
            step.toRecordingOperation(
                runId = stableRunId,
                index = index + 1,
            )
        },
    )
}

fun List<RecorderStepUi>.toRecordingRailTraceSteps(
    runId: String,
    sourceSessionId: String,
): List<RecorderStepUi> {
    val trace = toRecordingExecutionTrace(
        runId = runId,
        sourceSessionId = sourceSessionId,
    )
    return zip(trace.operations).map { (step, operation) ->
        step.copy(
            status = operation.status.toStepStatus(),
            detail = operation.message,
            properties = step.properties + buildMap {
                put("runId", trace.runId.value)
                put("operationId", operation.id.value)
                put("mode", trace.mode.name)
                put("sourceSessionId", trace.sourceSessionId)
                put("operationStatus", operation.status.name)
                put("sourceKind", ExecutionSourceKind.Record.name)
                put("sourceId", step.id)
                put("correlationId", step.id)
                if (operation.evidenceRefs.isNotEmpty()) {
                    put("evidenceRefs", operation.evidenceRefs.joinToString(separator = "|"))
                }
            },
        )
    }
}

private fun RecorderStepUi.toRecordingOperation(
    runId: ExecutionRunId,
    index: Int,
): OperationResult =
    OperationResult(
        id = ExecutionOperationId("${runId.value}:op:$index"),
        runId = runId,
        index = index,
        kind = actionType,
        status = status.toExecutionOperationStatus(),
        message = detail?.takeIf { it.isNotBlank() } ?: label,
        command = actionType,
        sourceRef = ExecutionSourceRef(
            kind = ExecutionSourceKind.Record,
            id = id,
        ),
        correlationId = id,
        evidenceRefs = evidenceReferences(),
    )

private fun RecorderStepUi.evidenceReferences(): List<String> =
    buildList {
        listOf(
            "screenshot",
            "screenshotPath",
            "image",
            "imagePath",
            "file",
            "path",
        ).forEach { key ->
            properties[key]
                ?.takeIf { it.isNotBlank() }
                ?.let(::add)
        }
        bounds?.let { add("bounds:${it.left},${it.top},${it.right},${it.bottom}") }
        point?.let { add("point:${it.x},${it.y}") }
    }.distinct()

private fun StepStatus.toExecutionOperationStatus(): ExecutionOperationStatus =
    when (this) {
        StepStatus.Recorded -> ExecutionOperationStatus.Succeeded
        StepStatus.Edited -> ExecutionOperationStatus.Warning
        StepStatus.Invalid -> ExecutionOperationStatus.Failed
        StepStatus.Executed -> ExecutionOperationStatus.Succeeded
    }

private fun ExecutionOperationStatus.toStepStatus(): StepStatus =
    when (this) {
        ExecutionOperationStatus.Pending,
        ExecutionOperationStatus.Running -> StepStatus.Edited
        ExecutionOperationStatus.Succeeded -> StepStatus.Recorded
        ExecutionOperationStatus.Warning -> StepStatus.Edited
        ExecutionOperationStatus.Failed,
        ExecutionOperationStatus.Skipped,
        ExecutionOperationStatus.Cancelled -> StepStatus.Invalid
    }
