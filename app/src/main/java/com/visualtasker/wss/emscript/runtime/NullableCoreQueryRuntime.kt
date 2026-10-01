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
    const val CHROME_TAB_ADAPTER_UNAVAILABLE = "CHROME_TAB_ADAPTER_UNAVAILABLE"
    const val CHROME_TAB_RESOLUTION_FAILED = "CHROME_TAB_RESOLUTION_FAILED"
    const val TASKER_ADAPTER_UNAVAILABLE = "TASKER_ADAPTER_UNAVAILABLE"
    const val TASKER_INSTALLATION_CHECK_FAILED = "TASKER_INSTALLATION_CHECK_FAILED"
    const val SHIZUKU_ADAPTER_UNAVAILABLE = "SHIZUKU_ADAPTER_UNAVAILABLE"
    const val SHIZUKU_INSTALLATION_CHECK_FAILED = "SHIZUKU_INSTALLATION_CHECK_FAILED"
    const val SHIZUKU_PERMISSION_CHECK_FAILED = "SHIZUKU_PERMISSION_CHECK_FAILED"
    const val SHIZUKU_BINDER_CHECK_FAILED = "SHIZUKU_BINDER_CHECK_FAILED"
    const val TERMUX_ADAPTER_UNAVAILABLE = "TERMUX_ADAPTER_UNAVAILABLE"
    const val TERMUX_INSTALLATION_CHECK_FAILED = "TERMUX_INSTALLATION_CHECK_FAILED"
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
