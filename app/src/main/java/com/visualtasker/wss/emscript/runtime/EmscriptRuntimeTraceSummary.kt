package com.visualtasker.wss.emscript.runtime

data class EmscriptRuntimeTraceSummary(
    val completed: Boolean,
    val eventCount: Int,
    val warningCount: Int,
    val errorCount: Int,
    val lastCommand: String?,
    val message: String,
) {
    val hasWarnings: Boolean = warningCount > 0
    val hasErrors: Boolean = errorCount > 0
}

fun EmscriptDryRunResult.traceSummary(): EmscriptRuntimeTraceSummary =
    toExecutionTrace(
        runId = ExecutionRunId("summary"),
        mode = ExecutionMode.DryRun,
        sourceSessionId = "summary",
    ).let { trace ->
        when (this) {
            is EmscriptDryRunResult.Success -> {
                EmscriptRuntimeTraceSummary(
                    completed = true,
                    eventCount = trace.eventCount,
                    warningCount = trace.warningCount,
                    errorCount = trace.errorCount,
                    lastCommand = trace.operations.lastOrNull { it.command != null }?.command,
                    message = if (trace.warningCount > 0 || trace.errorCount > 0) {
                        "Runtime abgeschlossen: ${trace.eventCount} Events, ${trace.warningCount} Warnungen, ${trace.errorCount} Fehler."
                    } else {
                        "Runtime abgeschlossen: ${trace.eventCount} Events."
                    },
                )
            }

            is EmscriptDryRunResult.Failure -> {
                EmscriptRuntimeTraceSummary(
                    completed = false,
                    eventCount = trace.eventCount,
                    warningCount = trace.warningCount,
                    errorCount = trace.errorCount.coerceAtLeast(1),
                    lastCommand = trace.operations.lastOrNull { it.command != null }?.command,
                    message = message,
                )
            }
        }
    }
