package com.visualtasker.wss.workspace.plugin.runtime

import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeAdapterResult
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes

internal fun ShizukuAvailabilityInspection.toAvailabilityAdapterResult(): RuntimeAdapterResult {
    failure?.let { error ->
        val diagnosticCode = when (failureStage) {
            ShizukuAvailabilityFailureStage.INSTALLATION ->
                RuntimeQueryDiagnosticCodes.SHIZUKU_INSTALLATION_CHECK_FAILED
            ShizukuAvailabilityFailureStage.PERMISSION ->
                RuntimeQueryDiagnosticCodes.SHIZUKU_PERMISSION_CHECK_FAILED
            ShizukuAvailabilityFailureStage.BINDER ->
                RuntimeQueryDiagnosticCodes.SHIZUKU_BINDER_CHECK_FAILED
            null -> RuntimeQueryDiagnosticCodes.SHIZUKU_BINDER_CHECK_FAILED
        }
        return RuntimeAdapterResult.failure(
            diagnosticCode = diagnosticCode,
            message = "shizuku.isAvailable check failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    return RuntimeAdapterResult.success(
        value = EmscriptValue.BooleanValue(available),
        message = "shizuku.isAvailable = $available (${state?.name ?: "UNKNOWN"})",
    )
}
