package com.visualtasker.wss.emscript.runtime

import java.io.File
import java.io.IOException
import java.nio.file.AccessDeniedException

internal object RuntimeQueryDiagnosticCodes {
    const val FILE_INVALID_PATH = "FILE_INVALID_PATH"
    const val FILE_READ_PERMISSION_DENIED = "FILE_READ_PERMISSION_DENIED"
    const val FILE_READ_FAILED = "FILE_READ_FAILED"
    const val DATASTORE_LOAD_FAILED = "DATASTORE_LOAD_FAILED"
    const val TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND"
    const val TEMPLATE_IMAGE_UNAVAILABLE = "TEMPLATE_IMAGE_UNAVAILABLE"
    const val TEMPLATE_REGION_UNAVAILABLE = "TEMPLATE_REGION_UNAVAILABLE"
    const val TEMPLATE_COMPARE_FAILED = "TEMPLATE_COMPARE_FAILED"
    const val VISION_ADAPTER_UNAVAILABLE = "VISION_ADAPTER_UNAVAILABLE"
    const val TEMPLATE_RESOURCE_UNAVAILABLE = "TEMPLATE_RESOURCE_UNAVAILABLE"
    const val VISION_CAPTURE_FAILED = "VISION_CAPTURE_FAILED"
    const val TEMPLATE_MATCH_FAILED = "TEMPLATE_MATCH_FAILED"
    const val OCR_FAILED = "OCR_FAILED"
    const val VISION_RESULT_INVALID = "VISION_RESULT_INVALID"
    const val MARKER_REPOSITORY_UNAVAILABLE = "MARKER_REPOSITORY_UNAVAILABLE"
    const val MARKER_DECODE_FAILED = "MARKER_DECODE_FAILED"
    const val CHART_REPOSITORY_UNAVAILABLE = "CHART_REPOSITORY_UNAVAILABLE"
    const val CHART_QUERY_FAILED = "CHART_QUERY_FAILED"
    const val CHART_LOAD_FAILED = "CHART_LOAD_FAILED"
    const val CHART_DECODE_FAILED = "CHART_DECODE_FAILED"
    const val CHART_RESULT_INVALID = "CHART_RESULT_INVALID"
    const val CHROME_TAB_ADAPTER_UNAVAILABLE = "CHROME_TAB_ADAPTER_UNAVAILABLE"
    const val CHROME_TAB_RESOLUTION_FAILED = "CHROME_TAB_RESOLUTION_FAILED"
    const val TASKER_ADAPTER_UNAVAILABLE = "TASKER_ADAPTER_UNAVAILABLE"
    const val TASKER_INSTALLATION_CHECK_FAILED = "TASKER_INSTALLATION_CHECK_FAILED"
    const val TASKER_ENABLED_CHECK_FAILED = "TASKER_ENABLED_CHECK_FAILED"
    const val TASKER_NOT_INSTALLED = "TASKER_NOT_INSTALLED"
    const val TASKER_VARIABLE_QUERY_FAILED = "TASKER_VARIABLE_QUERY_FAILED"
    const val TASKER_RESULT_INVALID = "TASKER_RESULT_INVALID"
    const val SHIZUKU_ADAPTER_UNAVAILABLE = "SHIZUKU_ADAPTER_UNAVAILABLE"
    const val SHIZUKU_INSTALLATION_CHECK_FAILED = "SHIZUKU_INSTALLATION_CHECK_FAILED"
    const val SHIZUKU_PERMISSION_CHECK_FAILED = "SHIZUKU_PERMISSION_CHECK_FAILED"
    const val SHIZUKU_BINDER_CHECK_FAILED = "SHIZUKU_BINDER_CHECK_FAILED"
    const val SHIZUKU_UID_QUERY_FAILED = "SHIZUKU_UID_QUERY_FAILED"
    const val TERMUX_ADAPTER_UNAVAILABLE = "TERMUX_ADAPTER_UNAVAILABLE"
    const val TERMUX_INSTALLATION_CHECK_FAILED = "TERMUX_INSTALLATION_CHECK_FAILED"
    const val TERMUX_STATUS_CHECK_FAILED = "TERMUX_STATUS_CHECK_FAILED"
    const val SCRCPY_ADAPTER_UNAVAILABLE = "SCRCPY_ADAPTER_UNAVAILABLE"
    const val SCRCPY_SESSION_CHECK_FAILED = "SCRCPY_SESSION_CHECK_FAILED"
    const val SCRCPY_UNKNOWN_KEY = "SCRCPY_UNKNOWN_KEY"
    const val SCRCPY_RESULT_INVALID = "SCRCPY_RESULT_INVALID"
}

internal class EmscriptRuntimeDiagnosticException(
    val diagnosticCode: String,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException("$diagnosticCode: $message", cause)

internal object FileReadTextQueryRuntime {
    fun read(
        path: String,
        resolve: (String) -> File?,
        readText: (File) -> String = File::readText,
    ): String? {
        val file = try {
            resolve(path)
        } catch (error: Exception) {
            throw invalidPath(error)
        } ?: throw invalidPath()

        if (!file.exists() || !file.isFile) return null

        return try {
            readText(file)
        } catch (error: AccessDeniedException) {
            throw EmscriptRuntimeDiagnosticException(
                RuntimeQueryDiagnosticCodes.FILE_READ_PERMISSION_DENIED,
                "Keine Leseberechtigung fuer die Datei.",
                error,
            )
        } catch (error: SecurityException) {
            throw EmscriptRuntimeDiagnosticException(
                RuntimeQueryDiagnosticCodes.FILE_READ_PERMISSION_DENIED,
                "Keine Leseberechtigung fuer die Datei.",
                error,
            )
        } catch (error: IOException) {
            throw EmscriptRuntimeDiagnosticException(
                RuntimeQueryDiagnosticCodes.FILE_READ_FAILED,
                "Die Datei konnte nicht gelesen werden.",
                error,
            )
        }
    }

    private fun invalidPath(cause: Throwable? = null) = EmscriptRuntimeDiagnosticException(
        RuntimeQueryDiagnosticCodes.FILE_INVALID_PATH,
        "Der Dateipfad ist ungueltig.",
        cause,
    )
}

internal data class RuntimeDatastoreLoadResult(
    val values: Map<String, String>,
    val failure: Throwable? = null,
)

internal object DatastoreGetQueryRuntime {
    fun get(
        values: Map<String, String>,
        loadFailure: Throwable?,
        key: String,
    ): String? {
        if (loadFailure != null) {
            throw EmscriptRuntimeDiagnosticException(
                RuntimeQueryDiagnosticCodes.DATASTORE_LOAD_FAILED,
                "Der Datastore konnte nicht geladen werden.",
                loadFailure,
            )
        }
        return values[key]
    }
}
