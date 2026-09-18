package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.runtime.ExecutionMode
import com.visualtasker.wss.emscript.runtime.ExecutionOperationId
import com.visualtasker.wss.emscript.runtime.ExecutionOperationStatus
import com.visualtasker.wss.emscript.runtime.ExecutionRunId
import com.visualtasker.wss.emscript.runtime.ExecutionSourceKind
import com.visualtasker.wss.emscript.runtime.ExecutionSourceRef
import com.visualtasker.wss.emscript.runtime.ExecutionTrace
import com.visualtasker.wss.emscript.runtime.OperationResult

fun List<RecorderStepUi>.watchDogSteps(): List<RecorderStepUi> =
    filter(RecorderStepUi::isWatchDogRelevant)

fun List<RecorderStepUi>.toWatchDogExecutionTrace(
    runId: String,
    sourceSessionId: String,
    startedAtEpochMs: Long = firstOrNull()?.timestampMs ?: 0L,
    completedAtEpochMs: Long? = maxOfOrNull { (it.timestampMs ?: 0L) + (it.durationMs ?: 0L) },
): ExecutionTrace {
    val stableRunId = ExecutionRunId(runId)
    return ExecutionTrace(
        runId = stableRunId,
        mode = ExecutionMode.WatchDog,
        sourceSessionId = sourceSessionId,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        completed = none { it.properties["status"].equals("running", ignoreCase = true) },
        operations = mapIndexed { index, step ->
            step.toWatchDogOperation(
                runId = stableRunId,
                index = index + 1,
            )
        },
    )
}

fun List<RecorderStepUi>.toWatchDogRailTraceSteps(
    runId: String,
    sourceSessionId: String,
): List<RecorderStepUi> {
    val trace = toWatchDogExecutionTrace(runId = runId, sourceSessionId = sourceSessionId)
    return zip(trace.operations).map { (step, operation) ->
        step.copy(
            status = operation.status.toStepStatus(),
            detail = operation.message,
            activityName = step.activityName ?: "WatchDog",
            properties = step.properties + buildMap {
                put("runId", trace.runId.value)
                put("operationId", operation.id.value)
                put("mode", trace.mode.name)
                put("sourceSessionId", trace.sourceSessionId)
                put("operationStatus", operation.status.name)
                put("sourceKind", ExecutionSourceKind.Provider.name)
                put("sourceId", step.id)
                put("correlationId", step.id)
                operation.pluginOwner?.let { put("pluginOwner", it) }
            },
        )
    }
}

private fun RecorderStepUi.toWatchDogOperation(
    runId: ExecutionRunId,
    index: Int,
): OperationResult =
    OperationResult(
        id = ExecutionOperationId("${runId.value}:op:$index"),
        runId = runId,
        index = index,
        kind = actionType,
        status = watchDogStatus(),
        message = detail?.takeIf { it.isNotBlank() } ?: label,
        command = properties["command"] ?: actionType,
        pluginOwner = pluginOwner(),
        sourceRef = ExecutionSourceRef(
            kind = ExecutionSourceKind.Provider,
            id = id,
        ),
        correlationId = id,
    )

private fun RecorderStepUi.watchDogStatus(): ExecutionOperationStatus =
    when (properties["status"]?.lowercase()) {
        "running" -> ExecutionOperationStatus.Running
        "done", "success", "succeeded" -> ExecutionOperationStatus.Succeeded
        "error", "failed", "invalid" -> ExecutionOperationStatus.Failed
        "cancelled", "canceled" -> ExecutionOperationStatus.Cancelled
        else -> when (status) {
            StepStatus.Recorded -> ExecutionOperationStatus.Succeeded
            StepStatus.Edited -> ExecutionOperationStatus.Warning
            StepStatus.Invalid -> ExecutionOperationStatus.Failed
            StepStatus.Executed -> ExecutionOperationStatus.Succeeded
        }
    }

private fun RecorderStepUi.pluginOwner(): String? =
    when {
        actionType.startsWith("tasker", ignoreCase = true) -> "tasker"
        actionType.startsWith("watchdog", ignoreCase = true) -> "watchdog"
        actionType.contains("vt2vt", ignoreCase = true) -> "vt2vt"
        actionType in WATCHDOG_SYSTEM_EVENT_TYPES -> "watchdog"
        properties["category"] in setOf("activity", "app", "screen", "button") -> "watchdog"
        properties["role"].equals("button", ignoreCase = true) -> "watchdog"
        else -> properties["pluginOwner"] ?: properties["source"]
    }

private fun RecorderStepUi.isWatchDogRelevant(): Boolean {
    if (actionType in WATCHDOG_SYSTEM_EVENT_TYPES) return true
    if (properties["category"] in WATCHDOG_SYSTEM_CATEGORIES) return true
    if (properties["role"].equals("button", ignoreCase = true)) return true
    val haystack = buildString {
        append(actionType)
        append(' ')
        append(activityName.orEmpty())
        append(' ')
        append(label)
        append(' ')
        append(detail.orEmpty())
        properties.values.forEach {
            append(' ')
            append(it)
        }
    }.lowercase()
    return WATCHDOG_TEXT_TOKENS.any { token -> token in haystack }
}

private val WATCHDOG_SYSTEM_EVENT_TYPES = setOf(
    "activity.change",
    "window.baseline",
    "window.transition",
    "app.foreground",
    "app.start",
    "screen.lock",
    "screen.on",
    "screen.unlock",
    "button.click",
)

private val WATCHDOG_SYSTEM_CATEGORIES = setOf("activity", "app", "screen", "button")

private val WATCHDOG_TEXT_TOKENS = listOf("watchdog", "tasker", "plugin", "provider", "vt2vt")

private fun ExecutionOperationStatus.toStepStatus(): StepStatus =
    when (this) {
        ExecutionOperationStatus.Pending,
        ExecutionOperationStatus.Running,
        ExecutionOperationStatus.Warning -> StepStatus.Edited
        ExecutionOperationStatus.Succeeded -> StepStatus.Recorded
        ExecutionOperationStatus.Failed,
        ExecutionOperationStatus.Skipped,
        ExecutionOperationStatus.Cancelled -> StepStatus.Invalid
    }
