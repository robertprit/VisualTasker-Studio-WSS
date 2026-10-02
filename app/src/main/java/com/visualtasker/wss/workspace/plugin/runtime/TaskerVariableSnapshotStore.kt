package com.visualtasker.wss.workspace.plugin.runtime

import android.content.Context

internal data class TaskerVariableQueryResult(
    val value: String? = null,
    val present: Boolean = false,
    val failure: Exception? = null,
) {
    init {
        require(failure == null || !present)
        require(present || value == null)
    }
}

/** Latest variables explicitly delivered by the Tasker plugin receiver. */
internal object TaskerVariableSnapshotStore {
    private const val STORE = "tasker-variable-snapshot"

    fun update(context: Context, variables: Map<String, String>) {
        if (variables.isEmpty()) return
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit()
            .apply {
                variables.forEach { (name, value) -> putString(name.normalizedTaskerVariableName(), value) }
            }
            .apply()
    }

    fun query(context: Context, name: String): TaskerVariableQueryResult = try {
        val normalized = name.normalizedTaskerVariableName()
        require(normalized.length > 1) { "Tasker variable name is empty" }
        val preferences = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        if (!preferences.contains(normalized)) {
            TaskerVariableQueryResult()
        } else {
            TaskerVariableQueryResult(value = preferences.getString(normalized, "").orEmpty(), present = true)
        }
    } catch (error: Exception) {
        TaskerVariableQueryResult(failure = error)
    }
}

internal fun String.normalizedTaskerVariableName(): String {
    val clean = trim().trim('"')
    return if (clean.startsWith("%")) clean else "%$clean"
}
