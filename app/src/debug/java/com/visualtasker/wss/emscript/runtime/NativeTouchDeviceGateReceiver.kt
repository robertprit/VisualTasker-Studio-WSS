package com.visualtasker.wss.emscript.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.visualtasker.wss.MainActivity
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.serialization.WorkflowSerializer

/** Debug-only device gate that avoids force-stopping the app's AccessibilityService. */
class NativeTouchDeviceGateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PREPARE -> prepare(context)
            ACTION_RESTORE -> restore(context)
            else -> return
        }
    }

    private fun prepare(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val backup = context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
        if (!backup.getBoolean(BACKUP_ACTIVE_KEY, false)) {
            backup.edit()
                .snapshot(preferences, WORKSPACE_KEY)
                .snapshot(preferences, WORKSPACE_VERSION_KEY)
                .snapshot(preferences, TEST_TARGET_KEY)
                .snapshot(preferences, STEPPER_STATE_KEY)
                .putBoolean(BACKUP_ACTIVE_KEY, true)
                .commit()
        }

        val metrics = context.resources.displayMetrics
        val script = buildString {
            appendLine("wait(500)")
            appendLine("clickPoint(${metrics.widthPixels / 2}, ${metrics.heightPixels / 2})")
            append("wait(500)")
        }
        val imported = EmscriptWorkspaceImporter().import(script, workspaceId = "m5-2-device-shell")
        check(imported.isSuccess) { imported.issues.joinToString { it.message } }
        val workspaceJson = WorkflowSerializer.serialize(requireNotNull(imported.document))

        preferences.edit()
            .putString(WORKSPACE_KEY, workspaceJson)
            .putInt(WORKSPACE_VERSION_KEY, Int.MAX_VALUE)
            .putBoolean(TEST_TARGET_KEY, true)
            .remove(STEPPER_STATE_KEY)
            .commit()
        launchWorkspace(context)
        resultCode = RESULT_PREPARED
        resultData = "M5-2 device gate prepared at ${metrics.widthPixels / 2},${metrics.heightPixels / 2}"
    }

    private fun restore(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val backup = context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
        if (backup.getBoolean(BACKUP_ACTIVE_KEY, false)) {
            preferences.edit()
                .restore(backup, WORKSPACE_KEY)
                .restore(backup, WORKSPACE_VERSION_KEY)
                .restore(backup, TEST_TARGET_KEY)
                .restore(backup, STEPPER_STATE_KEY)
                .commit()
            backup.edit().clear().commit()
        }
        launchWorkspace(context)
        resultCode = RESULT_RESTORED
        resultData = "M5-2 device gate restored"
    }

    private fun launchWorkspace(context: Context) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            },
        )
    }

    private fun android.content.SharedPreferences.Editor.snapshot(
        source: android.content.SharedPreferences,
        key: String,
    ): android.content.SharedPreferences.Editor {
        putBoolean("$key.exists", source.contains(key))
        return when (val value = source.all[key]) {
            is Boolean -> putBoolean("$key.boolean", value).putString("$key.type", "boolean")
            is Int -> putInt("$key.int", value).putString("$key.type", "int")
            is String -> putString("$key.string", value).putString("$key.type", "string")
            else -> putString("$key.type", "missing")
        }
    }

    private fun android.content.SharedPreferences.Editor.restore(
        backup: android.content.SharedPreferences,
        key: String,
    ): android.content.SharedPreferences.Editor {
        if (!backup.getBoolean("$key.exists", false)) return remove(key)
        return when (backup.getString("$key.type", "missing")) {
            "boolean" -> putBoolean(key, backup.getBoolean("$key.boolean", false))
            "int" -> putInt(key, backup.getInt("$key.int", 0))
            "string" -> putString(key, backup.getString("$key.string", null))
            else -> remove(key)
        }
    }

    companion object {
        private const val ACTION_PREPARE = "com.visualtasker.wss.debug.M5_2_PREPARE"
        private const val ACTION_RESTORE = "com.visualtasker.wss.debug.M5_2_RESTORE"
        private const val PREFS_NAME = "panel_ui_options"
        private const val BACKUP_PREFS_NAME = "m5_2_device_gate_backup"
        private const val BACKUP_ACTIVE_KEY = "active"
        private const val WORKSPACE_KEY = "blockeditor_workspace_json"
        private const val WORKSPACE_VERSION_KEY = "blockeditor_test_workspace_version"
        private const val TEST_TARGET_KEY = "m5_2_touch_test_target_visible"
        private const val STEPPER_STATE_KEY = "workspace_stepper_state"
        private const val RESULT_PREPARED = 52
        private const val RESULT_RESTORED = 53
    }
}
