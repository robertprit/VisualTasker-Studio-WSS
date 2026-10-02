package com.visualtasker.wss.workspace.plugin.runtime

import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeAdapterResult
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes
import de.visualtasker.emscript.contract.ProviderTypes

internal fun TaskerEnabledInspection.toEnabledAdapterResult(): RuntimeAdapterResult {
    failure?.let { error ->
        return RuntimeAdapterResult.failure(
            diagnosticCode = if (installed == null) {
                RuntimeQueryDiagnosticCodes.TASKER_INSTALLATION_CHECK_FAILED
            } else {
                RuntimeQueryDiagnosticCodes.TASKER_ENABLED_CHECK_FAILED
            },
            message = "tasker.isEnabled check failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    val value = enabled ?: return RuntimeAdapterResult.failure(
        diagnosticCode = RuntimeQueryDiagnosticCodes.TASKER_ENABLED_CHECK_FAILED,
        message = "tasker.isEnabled returned no Bool value.",
    )
    return RuntimeAdapterResult.success(
        value = EmscriptValue.BooleanValue(value),
        message = "tasker.isEnabled = $value",
    )
}

internal fun TaskerVariableQueryResult.toVariableAdapterResult(): RuntimeAdapterResult {
    failure?.let { error ->
        return RuntimeAdapterResult.failure(
            diagnosticCode = RuntimeQueryDiagnosticCodes.TASKER_VARIABLE_QUERY_FAILED,
            message = "tasker.getVariable failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    return RuntimeAdapterResult.success(
        value = if (present) EmscriptValue.StringValue(value.orEmpty()) else EmscriptValue.NullValue,
        message = if (present) "tasker.getVariable -> VALUE" else "tasker.getVariable -> ABSENT",
    )
}

internal fun taskerVariableAdapterResult(
    installation: PackageInstallationInspection,
    query: () -> TaskerVariableQueryResult,
): RuntimeAdapterResult {
    installation.failure?.let { error ->
        return RuntimeAdapterResult.failure(
            diagnosticCode = RuntimeQueryDiagnosticCodes.TASKER_INSTALLATION_CHECK_FAILED,
            message = "tasker.getVariable installation check failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    if (!installation.installed) {
        return RuntimeAdapterResult.failure(
            diagnosticCode = RuntimeQueryDiagnosticCodes.TASKER_NOT_INSTALLED,
            message = "tasker.getVariable requires an installed Tasker provider.",
        )
    }
    return query().toVariableAdapterResult()
}

internal fun TaskerVariablesQueryResult.toVariablesAdapterResult(): RuntimeAdapterResult {
    failure?.let { error ->
        return RuntimeAdapterResult.failure(
            diagnosticCode = RuntimeQueryDiagnosticCodes.TASKER_VARIABLE_QUERY_FAILED,
            message = "tasker.getVariables failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    return try {
        val values = variables.map { variable ->
            EmscriptValue.TaskerVariableValue(name = variable.name, value = variable.value)
        }
        RuntimeAdapterResult.success(
            value = EmscriptValue.ListValue(ProviderTypes.TASKER_VARIABLE.ref, values),
            message = "tasker.getVariables -> ${values.size} VALUE(s)",
        )
    } catch (error: Exception) {
        RuntimeAdapterResult.failure(
            diagnosticCode = RuntimeQueryDiagnosticCodes.TASKER_RESULT_INVALID,
            message = "tasker.getVariables returned an invalid result: ${error.message ?: error::class.java.simpleName}",
        )
    }
}

internal fun taskerVariablesAdapterResult(
    installation: PackageInstallationInspection,
    query: () -> TaskerVariablesQueryResult,
): RuntimeAdapterResult {
    installation.failure?.let { error ->
        return RuntimeAdapterResult.failure(
            diagnosticCode = RuntimeQueryDiagnosticCodes.TASKER_INSTALLATION_CHECK_FAILED,
            message = "tasker.getVariables installation check failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    if (!installation.installed) {
        return RuntimeAdapterResult.failure(
            diagnosticCode = RuntimeQueryDiagnosticCodes.TASKER_NOT_INSTALLED,
            message = "tasker.getVariables requires an installed Tasker provider.",
        )
    }
    return query().toVariablesAdapterResult()
}

internal fun ShizukuAvailabilityInspection.toUidAdapterResult(): RuntimeAdapterResult {
    failure?.let { error ->
        val code = when (failureStage) {
            ShizukuAvailabilityFailureStage.INSTALLATION -> RuntimeQueryDiagnosticCodes.SHIZUKU_INSTALLATION_CHECK_FAILED
            ShizukuAvailabilityFailureStage.PERMISSION -> RuntimeQueryDiagnosticCodes.SHIZUKU_PERMISSION_CHECK_FAILED
            ShizukuAvailabilityFailureStage.BINDER -> RuntimeQueryDiagnosticCodes.SHIZUKU_BINDER_CHECK_FAILED
            ShizukuAvailabilityFailureStage.UID, null -> RuntimeQueryDiagnosticCodes.SHIZUKU_UID_QUERY_FAILED
        }
        return RuntimeAdapterResult.failure(code, "shizuku.getUid failed: ${error.message ?: error::class.java.simpleName}")
    }
    uidFailure?.let { error ->
        return RuntimeAdapterResult.failure(
            RuntimeQueryDiagnosticCodes.SHIZUKU_UID_QUERY_FAILED,
            "shizuku.getUid failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    val uidValue = uid
    if (uidValue != null && uidValue < 0) {
        return RuntimeAdapterResult.failure(
            RuntimeQueryDiagnosticCodes.SHIZUKU_UID_QUERY_FAILED,
            "shizuku.getUid returned invalid UID $uidValue.",
        )
    }
    return RuntimeAdapterResult.success(
        value = uidValue?.let { EmscriptValue.NumberValue(it.toDouble()) } ?: EmscriptValue.NullValue,
        message = uidValue?.let { "shizuku.getUid = $it" } ?: "shizuku.getUid -> ABSENT",
    )
}

internal fun TermuxStatusInspection.toGetAdapterResult(key: String): RuntimeAdapterResult {
    failure?.let { error ->
        return RuntimeAdapterResult.failure(
            RuntimeQueryDiagnosticCodes.TERMUX_STATUS_CHECK_FAILED,
            "termux.get failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    val current = status ?: return RuntimeAdapterResult.failure(
        RuntimeQueryDiagnosticCodes.TERMUX_STATUS_CHECK_FAILED,
        "termux.get returned no status.",
    )
    val normalized = key.trim().trim('"').lowercase()
    val value = when (normalized) {
        "installed" -> current.installed.toString()
        "apiinstalled" -> current.apiInstalled.toString()
        "canruncommands" -> current.canRunCommands.toString()
        "permission" -> if (current.runCommandPermissionGranted) "granted" else "missing"
        "summary", "" -> current.summary
        else -> null
    }
    return RuntimeAdapterResult.success(
        value = value?.let(EmscriptValue::StringValue) ?: EmscriptValue.NullValue,
        message = value?.let { "termux.get($normalized) = $it" } ?: "termux.get($normalized) -> ABSENT",
    )
}
