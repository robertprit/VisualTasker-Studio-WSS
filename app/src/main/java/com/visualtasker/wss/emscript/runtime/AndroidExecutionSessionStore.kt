package com.visualtasker.wss.emscript.runtime

import android.content.Context
import org.json.JSONObject

class AndroidExecutionSessionStore(context: Context) : ExecutionSessionCheckpointStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): ExecutionSessionCheckpoint? {
        val raw = preferences.getString(KEY_CHECKPOINT, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            ExecutionSessionCheckpoint(
                sessionId = json.getString("sessionId"),
                workflowId = json.getString("workflowId"),
                workspaceRevision = json.getLong("workspaceRevision"),
                state = ExecutionSessionState.valueOf(json.getString("state")),
                startedAtEpochMs = json.optLongOrNull("startedAtEpochMs"),
                endedAtEpochMs = json.optLongOrNull("endedAtEpochMs"),
                traceCount = json.optInt("traceCount", 0),
                lastEntityId = json.optStringOrNull("lastEntityId"),
                lastProviderId = json.optStringOrNull("lastProviderId"),
                cancellationReason = json.optStringOrNull("cancellationReason")
                    ?.let(ExecutionCancellationReason::valueOf),
                projectId = json.optStringOrNull("projectId"),
                workflowRootId = json.optStringOrNull("workflowRootId"),
                projectRevision = json.optLongOrNull("projectRevision"),
            )
        }.getOrNull()
    }

    override fun write(checkpoint: ExecutionSessionCheckpoint) {
        val json = JSONObject()
            .put("schema", CHECKPOINT_SCHEMA)
            .put("sessionId", checkpoint.sessionId)
            .put("workflowId", checkpoint.workflowId)
            .put("workspaceRevision", checkpoint.workspaceRevision)
            .put("state", checkpoint.state.name)
            .put("traceCount", checkpoint.traceCount)
        checkpoint.startedAtEpochMs?.let { json.put("startedAtEpochMs", it) }
        checkpoint.endedAtEpochMs?.let { json.put("endedAtEpochMs", it) }
        checkpoint.lastEntityId?.let { json.put("lastEntityId", it) }
        checkpoint.lastProviderId?.let { json.put("lastProviderId", it) }
        checkpoint.cancellationReason?.let { json.put("cancellationReason", it.name) }
        checkpoint.projectId?.let { json.put("projectId", it) }
        checkpoint.workflowRootId?.let { json.put("workflowRootId", it) }
        checkpoint.projectRevision?.let { json.put("projectRevision", it) }
        preferences.edit().putString(KEY_CHECKPOINT, json.toString()).apply()
    }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key).takeIf(String::isNotBlank) else null

    private companion object {
        const val PREFS_NAME = "execution_session_checkpoint"
        const val KEY_CHECKPOINT = "latest"
        const val CHECKPOINT_SCHEMA = 2
    }
}
