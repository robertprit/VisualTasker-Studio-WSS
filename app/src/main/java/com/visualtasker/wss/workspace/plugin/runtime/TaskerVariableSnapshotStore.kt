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

internal data class TaskerVariableSnapshot(
    val name: String,
    val value: String,
) {
    init {
        require(name.isNotBlank())
    }
}

internal data class TaskerVariablesQueryResult(
    val variables: List<TaskerVariableSnapshot> = emptyList(),
    val failure: Exception? = null,
) {
    init {
        require(failure == null || variables.isEmpty())
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

    fun queryAll(context: Context, pattern: String?): TaskerVariablesQueryResult = try {
        queryTaskerVariables(
            snapshot = context.getSharedPreferences(STORE, Context.MODE_PRIVATE).all,
            pattern = pattern,
        )
    } catch (error: Exception) {
        TaskerVariablesQueryResult(failure = error)
    }
}

internal fun queryTaskerVariables(
    snapshot: Map<String, *>,
    pattern: String?,
): TaskerVariablesQueryResult = try {
    val variables = snapshot.entries
        .map { (name, rawValue) ->
            val value = rawValue as? String
                ?: error("Tasker variable '$name' is not a String value")
            TaskerVariableSnapshot(name.normalizedTaskerVariableName(), value)
        }
        .filter { it.name.matchesTaskerVariablePattern(pattern) }
        .sortedBy(TaskerVariableSnapshot::name)
    TaskerVariablesQueryResult(variables = variables)
} catch (error: Exception) {
    TaskerVariablesQueryResult(failure = error)
}

private fun String.matchesTaskerVariablePattern(pattern: String?): Boolean {
    val raw = pattern?.trim()?.trim('"').orEmpty()
    if (raw.isEmpty() || raw == "*") return true
    val normalized = if ('*' in raw || '?' in raw || raw.startsWith('%')) {
        raw
    } else {
        raw.normalizedTaskerVariableName()
    }
    val expression = buildString {
        append('^')
        normalized.forEach { character ->
            when (character) {
                '*' -> append(".*")
                '?' -> append('.')
                else -> append(Regex.escape(character.toString()))
            }
        }
        append('$')
    }
    return Regex(expression).matches(this)
}

internal fun String.normalizedTaskerVariableName(): String {
    val clean = trim().trim('"')
    return if (clean.startsWith("%")) clean else "%$clean"
}
