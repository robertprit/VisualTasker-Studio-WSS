package com.visualtasker.wss.workspace.plugin.runtime

import android.content.Context
import java.io.File

object TaskerPluginSessionStore {
    private const val SESSION_PATH = "emscript-runtime/tasker/tasker-plugin-sessions.jsonl"

    fun record(context: Context, action: TaskerPluginAction): TaskerPluginSessionSnapshot =
        record(File(context.filesDir, SESSION_PATH), action)

    fun record(file: File, action: TaskerPluginAction): TaskerPluginSessionSnapshot {
        file.parentFile?.mkdirs()
        val now = System.currentTimeMillis()
        val previous = latestByRunId(file)[action.runId]
        val ordinal = (previous?.ordinal ?: 0) + 1
        val snapshot = TaskerPluginSessionSnapshot(
            runId = action.runId,
            eventSlot = action.eventSlot,
            status = action.status,
            command = action.command,
            eventName = action.eventName,
            message = action.message,
            ordinal = ordinal,
            timestampMs = now,
        )
        file.appendText(snapshot.toJsonLine() + "\n")
        return snapshot
    }

    fun latestByRunId(file: File): Map<String, TaskerPluginSessionSnapshot> {
        if (!file.isFile) return emptyMap()
        return file.readLines()
            .mapNotNull(::parseSnapshot)
            .associateBy { it.runId }
    }

    fun latestByRunId(context: Context): Map<String, TaskerPluginSessionSnapshot> =
        latestByRunId(File(context.filesDir, SESSION_PATH))

    fun clear(context: Context) {
        File(context.filesDir, SESSION_PATH).delete()
    }

    fun lastResult(context: Context, runId: String? = null): TaskerPluginSessionSnapshot? =
        lastResult(File(context.filesDir, SESSION_PATH), runId)

    fun lastResult(file: File, runId: String? = null): TaskerPluginSessionSnapshot? {
        val snapshots = readSnapshots(file)
        return if (runId.isNullOrBlank()) {
            snapshots.lastOrNull()
        } else {
            snapshots.lastOrNull { it.runId == runId }
        }
    }

    fun lastError(context: Context, runId: String? = null): TaskerPluginSessionSnapshot? =
        lastError(File(context.filesDir, SESSION_PATH), runId)

    fun lastError(file: File, runId: String? = null): TaskerPluginSessionSnapshot? =
        readSnapshots(file).lastOrNull { snapshot ->
            snapshot.status.equals("error", ignoreCase = true) &&
                (runId.isNullOrBlank() || snapshot.runId == runId)
        }

    private fun parseSnapshot(line: String): TaskerPluginSessionSnapshot? {
        val fields = parseFlatJsonObject(line)
        return TaskerPluginSessionSnapshot(
            runId = fields["runId"]?.takeIf { it.isNotBlank() } ?: return null,
            eventSlot = fields["eventSlot"].orEmpty().ifBlank { "slot_1" },
            status = fields["status"].orEmpty().ifBlank { "received" },
            command = fields["command"].orEmpty().ifBlank { TaskerPluginContract.COMMAND_RECORD_EVENT },
            eventName = fields["eventName"].orEmpty().ifBlank { "Tasker Event" },
            message = fields["message"].orEmpty(),
            ordinal = fields["ordinal"]?.toIntOrNull() ?: 1,
            timestampMs = fields["timestampMs"]?.toLongOrNull() ?: 0L,
        )
    }

    private fun readSnapshots(file: File): List<TaskerPluginSessionSnapshot> {
        if (!file.isFile) return emptyList()
        return file.readLines().mapNotNull(::parseSnapshot)
    }

    private fun parseFlatJsonObject(raw: String): Map<String, String> {
        val text = raw.trim()
        if (!text.startsWith("{") || !text.endsWith("}")) return emptyMap()
        val result = linkedMapOf<String, String>()
        var index = 1
        while (index < text.lastIndex) {
            while (index < text.lastIndex && (text[index].isWhitespace() || text[index] == ',')) index++
            if (index >= text.lastIndex || text[index] != '"') break
            val keyEnd = findJsonStringEnd(text, index + 1)
            if (keyEnd < 0) break
            val key = unescapeJsonString(text.substring(index + 1, keyEnd))
            index = keyEnd + 1
            while (index < text.lastIndex && (text[index].isWhitespace() || text[index] == ':')) index++
            val value = if (index < text.lastIndex && text[index] == '"') {
                val valueEnd = findJsonStringEnd(text, index + 1)
                if (valueEnd < 0) break
                val parsed = unescapeJsonString(text.substring(index + 1, valueEnd))
                index = valueEnd + 1
                parsed
            } else {
                val valueStart = index
                while (index < text.lastIndex && text[index] != ',') index++
                text.substring(valueStart, index).trim()
            }
            result[key] = value
        }
        return result
    }

    private fun findJsonStringEnd(text: String, start: Int): Int {
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == '"') {
                return index
            }
        }
        return -1
    }

    private fun unescapeJsonString(value: String): String =
        buildString(value.length) {
            var escaped = false
            value.forEach { char ->
                if (escaped) {
                    append(
                        when (char) {
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            '\\' -> '\\'
                            '"' -> '"'
                            else -> char
                        },
                    )
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else {
                    append(char)
                }
            }
            if (escaped) append('\\')
        }
}

data class TaskerPluginSessionSnapshot(
    val runId: String,
    val eventSlot: String,
    val status: String,
    val command: String,
    val eventName: String,
    val message: String,
    val ordinal: Int,
    val timestampMs: Long,
) {
    fun toJsonLine(): String {
        val values = linkedMapOf(
            "timestampMs" to timestampMs.toString(),
            "runId" to runId,
            "eventSlot" to eventSlot,
            "status" to status,
            "command" to command,
            "eventName" to eventName,
            "message" to message,
            "ordinal" to ordinal.toString(),
        )
        return values.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"${key.jsonEscape()}\":\"${value.jsonEscape()}\""
        }
    }
}

private fun String.jsonEscape(): String =
    buildString(length) {
        this@jsonEscape.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
