package com.visualtasker.wss.workspace.plugin.runtime

import android.content.Context
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeAdapterResult
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes
import java.util.UUID

internal data class ScrcpySessionSnapshot(
    val sessionId: String,
    val serial: String,
    val state: String,
    val transport: String,
    val host: String,
    val port: Int,
) {
    val running: Boolean get() = state.equals("running", ignoreCase = true)
}

internal data class ScrcpySessionInspection(
    val session: ScrcpySessionSnapshot? = null,
    val failure: Exception? = null,
) {
    init {
        require(failure == null || session == null)
    }
}

internal object ScrcpySessionStore {
    private const val STORE = "scrcpy-session-runtime"

    fun markRunning(
        context: Context,
        serial: String,
        transport: String,
        host: String,
        port: Int,
    ): ScrcpySessionSnapshot {
        val snapshot = ScrcpySessionSnapshot(
            sessionId = "scrcpy-${UUID.randomUUID()}",
            serial = serial.trim().trim('"'),
            state = "running",
            transport = transport,
            host = host,
            port = port,
        )
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putString("sessionId", snapshot.sessionId)
            .putString("serial", snapshot.serial)
            .putString("state", snapshot.state)
            .putString("transport", snapshot.transport)
            .putString("host", snapshot.host)
            .putInt("port", snapshot.port)
            .apply()
        return snapshot
    }

    fun clear(context: Context) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun inspect(context: Context, serial: String? = null): ScrcpySessionInspection = try {
        val values = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val sessionId = values.getString("sessionId", null)
            ?: return ScrcpySessionInspection()
        val snapshot = ScrcpySessionSnapshot(
            sessionId = sessionId,
            serial = values.getString("serial", "").orEmpty(),
            state = values.getString("state", "stopped").orEmpty(),
            transport = values.getString("transport", "unknown").orEmpty(),
            host = values.getString("host", "").orEmpty(),
            port = values.getInt("port", 0),
        )
        val requestedSerial = serial?.trim()?.trim('"').orEmpty()
        ScrcpySessionInspection(
            session = snapshot.takeIf { requestedSerial.isBlank() || it.serial == requestedSerial },
        )
    } catch (error: Exception) {
        ScrcpySessionInspection(failure = error)
    }
}

internal fun ScrcpySessionInspection.toIsRunningAdapterResult(): RuntimeAdapterResult {
    failure?.let { error ->
        return RuntimeAdapterResult.failure(
            RuntimeQueryDiagnosticCodes.SCRCPY_SESSION_CHECK_FAILED,
            "scrcpy.isRunning failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    val running = session?.running == true
    return RuntimeAdapterResult.success(
        EmscriptValue.BooleanValue(running),
        "scrcpy.isRunning = $running",
    )
}

internal fun ScrcpySessionInspection.toGetAdapterResult(key: String): RuntimeAdapterResult {
    failure?.let { error ->
        return RuntimeAdapterResult.failure(
            RuntimeQueryDiagnosticCodes.SCRCPY_SESSION_CHECK_FAILED,
            "scrcpy.get failed: ${error.message ?: error::class.java.simpleName}",
        )
    }
    val normalized = key.trim().trim('"').lowercase()
    val supported = setOf("sessionid", "serial", "state", "transport", "host", "port")
    if (normalized !in supported) {
        return RuntimeAdapterResult.failure(
            RuntimeQueryDiagnosticCodes.SCRCPY_UNKNOWN_KEY,
            "scrcpy.get received unknown key '$normalized'.",
        )
    }
    val value = session?.let { current ->
        when (normalized) {
            "sessionid" -> current.sessionId
            "serial" -> current.serial.takeIf(String::isNotBlank)
            "state" -> current.state
            "transport" -> current.transport.takeIf(String::isNotBlank)
            "host" -> current.host.takeIf(String::isNotBlank)
            "port" -> current.port.takeIf { it > 0 }?.toString()
            else -> null
        }
    }
    return RuntimeAdapterResult.success(
        value?.let(EmscriptValue::StringValue) ?: EmscriptValue.NullValue,
        value?.let { "scrcpy.get($normalized) = $it" } ?: "scrcpy.get($normalized) -> ABSENT",
    )
}
