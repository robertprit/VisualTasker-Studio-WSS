package com.visualtasker.wss.workspace.plugin.runtime

import android.content.Context
import android.content.SharedPreferences

data class TaskerPluginSettings(
    val recordToRailTrace: Boolean = true,
    val showToasts: Boolean = true,
    val autoOpenWorkspace: Boolean = true,
) {
    fun save(context: Context) {
        save(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }

    fun save(prefs: SharedPreferences) {
        prefs.edit()
            .putBoolean(KEY_RECORD_TO_RAILTRACE, recordToRailTrace)
            .putBoolean(KEY_SHOW_TOASTS, showToasts)
            .putBoolean(KEY_AUTO_OPEN_WORKSPACE, autoOpenWorkspace)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "tasker_plugin_settings"
        const val KEY_RECORD_TO_RAILTRACE = "record_to_railtrace"
        const val KEY_SHOW_TOASTS = "show_toasts"
        const val KEY_AUTO_OPEN_WORKSPACE = "auto_open_workspace"

        fun load(context: Context): TaskerPluginSettings =
            load(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

        fun load(prefs: SharedPreferences): TaskerPluginSettings =
            TaskerPluginSettings(
                recordToRailTrace = prefs.getBoolean(KEY_RECORD_TO_RAILTRACE, true),
                showToasts = prefs.getBoolean(KEY_SHOW_TOASTS, true),
                autoOpenWorkspace = prefs.getBoolean(KEY_AUTO_OPEN_WORKSPACE, true),
            )
    }
}
