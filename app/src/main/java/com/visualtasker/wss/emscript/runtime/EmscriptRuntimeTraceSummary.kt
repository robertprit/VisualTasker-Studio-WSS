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
    when (this) {
        is EmscriptDryRunResult.Success -> {
            val warnings = events.count { it.severity == EmscriptDryRunEventSeverity.WARNING }
            val errors = events.count { it.severity == EmscriptDryRunEventSeverity.ERROR }
            EmscriptRuntimeTraceSummary(
                completed = true,
                eventCount = events.size,
                warningCount = warnings,
                errorCount = errors,
                lastCommand = events.lastOrNull { it.command != null }?.command,
                message = if (warnings > 0 || errors > 0) {
                    "Runtime abgeschlossen: ${events.size} Events, $warnings Warnungen, $errors Fehler."
                } else {
                    "Runtime abgeschlossen: ${events.size} Events."
                },
            )
        }
        is EmscriptDryRunResult.Failure -> {
            val warnings = events.count { it.severity == EmscriptDryRunEventSeverity.WARNING }
            val errors = events.count { it.severity == EmscriptDryRunEventSeverity.ERROR }.coerceAtLeast(1)
            EmscriptRuntimeTraceSummary(
                completed = false,
                eventCount = events.size,
                warningCount = warnings,
                errorCount = errors,
                lastCommand = events.lastOrNull { it.command != null }?.command,
                message = message,
            )
        }
    }
