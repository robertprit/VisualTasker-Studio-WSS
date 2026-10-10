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
            ACTION_PREPARE -> prepareClick(context)
            ACTION_PREPARE_SWIPE -> prepareGesture(context, GestureGateMode.Swipe)
            ACTION_PREPARE_PATH -> prepareGesture(context, GestureGateMode.Path)
            ACTION_PREPARE_CANCELLATION -> prepareGesture(context, GestureGateMode.Cancellation)
            ACTION_CANCEL -> requestCancellation(context)
            ACTION_RESTORE -> restore(context)
            else -> return
        }
    }

    private fun prepareClick(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureBackup(context, preferences)

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
            .remove(GESTURE_TEST_MODE_KEY)
            .remove(STEPPER_STATE_KEY)
            .commit()
        launchWorkspace(context)
        resultCode = RESULT_PREPARED
        resultData = "M5-2 device gate prepared at ${metrics.widthPixels / 2},${metrics.heightPixels / 2}"
    }

    private fun prepareGesture(context: Context, mode: GestureGateMode) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureBackup(context, preferences)
        val metrics = context.resources.displayMetrics
        val left = metrics.widthPixels * 35 / 100
        val centerX = metrics.widthPixels / 2
        val right = metrics.widthPixels * 65 / 100
        val centerY = metrics.heightPixels / 2
        val verticalOffset = metrics.heightPixels / 24
        val points = when (mode) {
            GestureGateMode.Swipe,
            GestureGateMode.Cancellation,
            -> listOf(left, centerY, right, centerY)
            GestureGateMode.Path -> listOf(
                left,
                centerY - verticalOffset,
                centerX,
                centerY + verticalOffset,
                right,
                centerY - verticalOffset,
            )
        }
        val repeat = if (mode == GestureGateMode.Cancellation) CANCELLATION_SEQUENCE_LENGTH else 1
        val script = buildString {
            appendLine("wait(500)")
            appendLine("swipe([${points.joinToString(", ")}], $repeat)")
            append("wait(500)")
        }
        val imported = EmscriptWorkspaceImporter().import(script, workspaceId = "m5-3-device-shell-${mode.name.lowercase()}")
        check(imported.isSuccess) { imported.issues.joinToString { it.message } }
        val workspaceJson = WorkflowSerializer.serialize(requireNotNull(imported.document))

        preferences.edit()
            .putString(WORKSPACE_KEY, workspaceJson)
            .putInt(WORKSPACE_VERSION_KEY, Int.MAX_VALUE)
            .putBoolean(TEST_TARGET_KEY, false)
            .putString(GESTURE_TEST_MODE_KEY, "${mode.name.lowercase()}:${System.nanoTime()}")
            .remove(STEPPER_STATE_KEY)
            .commit()
        launchWorkspace(context)
        resultCode = RESULT_PREPARED
        resultData = "M5-3 ${mode.name.lowercase()} gate prepared: ${points.joinToString()} repeat=$repeat"
    }

    private fun ensureBackup(
        context: Context,
        preferences: android.content.SharedPreferences,
    ) {
        val backup = context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
        if (backup.getBoolean(BACKUP_ACTIVE_KEY, false)) return
        backup.edit()
            .snapshot(preferences, WORKSPACE_KEY)
            .snapshot(preferences, WORKSPACE_VERSION_KEY)
            .snapshot(preferences, TEST_TARGET_KEY)
            .snapshot(preferences, GESTURE_TEST_MODE_KEY)
            .snapshot(preferences, STEPPER_STATE_KEY)
            .putBoolean(BACKUP_ACTIVE_KEY, true)
            .commit()
    }

    private fun restore(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val backup = context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
        if (backup.getBoolean(BACKUP_ACTIVE_KEY, false)) {
            preferences.edit()
                .restore(backup, WORKSPACE_KEY)
                .restore(backup, WORKSPACE_VERSION_KEY)
                .restore(backup, TEST_TARGET_KEY)
                .restore(backup, GESTURE_TEST_MODE_KEY)
                .restore(backup, STEPPER_STATE_KEY)
                .commit()
            backup.edit().clear().commit()
        }
        launchWorkspace(context)
        resultCode = RESULT_RESTORED
        resultData = "M5-2 device gate restored"
    }

    private fun requestCancellation(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(CANCEL_REQUEST_KEY, System.nanoTime())
            .commit()
        resultCode = RESULT_CANCEL_REQUESTED
        resultData = "M5-3 runtime cancellation requested"
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
        private const val ACTION_PREPARE_SWIPE = "com.visualtasker.wss.debug.M5_3_PREPARE_SWIPE"
        private const val ACTION_PREPARE_PATH = "com.visualtasker.wss.debug.M5_3_PREPARE_PATH"
        private const val ACTION_PREPARE_CANCELLATION = "com.visualtasker.wss.debug.M5_3_PREPARE_CANCELLATION"
        private const val ACTION_CANCEL = "com.visualtasker.wss.debug.M5_3_CANCEL"
        private const val ACTION_RESTORE = "com.visualtasker.wss.debug.M5_2_RESTORE"
        private const val PREFS_NAME = "panel_ui_options"
        private const val BACKUP_PREFS_NAME = "m5_2_device_gate_backup"
        private const val BACKUP_ACTIVE_KEY = "active"
        private const val WORKSPACE_KEY = "blockeditor_workspace_json"
        private const val WORKSPACE_VERSION_KEY = "blockeditor_test_workspace_version"
        private const val TEST_TARGET_KEY = "m5_2_touch_test_target_visible"
        private const val GESTURE_TEST_MODE_KEY = "m5_3_gesture_test_mode"
        private const val CANCEL_REQUEST_KEY = "m5_3_cancel_request"
        private const val STEPPER_STATE_KEY = "workspace_stepper_state"
        private const val CANCELLATION_SEQUENCE_LENGTH = 20
        private const val RESULT_PREPARED = 52
        private const val RESULT_RESTORED = 53
        private const val RESULT_CANCEL_REQUESTED = 54
    }

    private enum class GestureGateMode {
        Swipe,
        Path,
        Cancellation,
    }
}
