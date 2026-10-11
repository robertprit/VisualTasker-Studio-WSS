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
            ACTION_PREPARE_LINEAR -> preparePointerPath(context, PointerPathGateMode.Linear)
            ACTION_PREPARE_QUADRATIC -> preparePointerPath(context, PointerPathGateMode.Quadratic)
            ACTION_PREPARE_CUBIC -> preparePointerPath(context, PointerPathGateMode.Cubic)
            ACTION_PREPARE_SLOW -> preparePointerPath(context, PointerPathGateMode.Slow)
            ACTION_PREPARE_FAST -> preparePointerPath(context, PointerPathGateMode.Fast)
            ACTION_PREPARE_POINTER_CANCELLATION -> preparePointerPath(context, PointerPathGateMode.Cancellation)
            ACTION_PREPARE_VARIABLE_PATH -> preparePointerPath(context, PointerPathGateMode.Variable)
            ACTION_PREPARE_PINCH_IN -> prepareMultiPointer(context, MultiPointerGateMode.PinchIn)
            ACTION_PREPARE_PINCH_OUT -> prepareMultiPointer(context, MultiPointerGateMode.PinchOut)
            ACTION_PREPARE_PARALLEL -> prepareMultiPointer(context, MultiPointerGateMode.Parallel)
            ACTION_PREPARE_OPPOSING -> prepareMultiPointer(context, MultiPointerGateMode.Opposing)
            ACTION_PREPARE_DELAYED -> prepareMultiPointer(context, MultiPointerGateMode.Delayed)
            ACTION_PREPARE_MULTI_CANCELLATION -> prepareMultiPointer(context, MultiPointerGateMode.Cancellation)
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

    private fun preparePointerPath(context: Context, mode: PointerPathGateMode) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureBackup(context, preferences)
        val metrics = context.resources.displayMetrics
        val left = metrics.widthPixels * 35 / 100
        val centerX = metrics.widthPixels / 2
        val right = metrics.widthPixels * 65 / 100
        val centerY = metrics.heightPixels / 2
        val verticalOffset = metrics.heightPixels / 22
        val path = when (mode) {
            PointerPathGateMode.Linear,
            PointerPathGateMode.Slow,
            PointerPathGateMode.Fast,
            PointerPathGateMode.Variable,
            -> "path(point($left,$centerY),point($right,$centerY))"
            PointerPathGateMode.Quadratic ->
                "path([quadraticBezierSegment(point($left,$centerY)," +
                    "point($centerX,${centerY - verticalOffset}),point($right,$centerY))])"
            PointerPathGateMode.Cubic,
            PointerPathGateMode.Cancellation,
            ->
                "path([cubicBezierSegment(point($left,$centerY)," +
                    "point(${left + (centerX - left) / 2},${centerY - verticalOffset})," +
                    "point(${centerX + (right - centerX) / 2},${centerY + verticalOffset})," +
                    "point($right,$centerY))])"
        }
        val timing = when (mode) {
            PointerPathGateMode.Linear -> "duration=900,startHold=80,endHold=80"
            PointerPathGateMode.Quadratic -> "duration=1200,startHold=80,endHold=80"
            PointerPathGateMode.Cubic -> "duration=1400,startHold=80,endHold=80"
            PointerPathGateMode.Slow -> "speed=140"
            PointerPathGateMode.Fast -> "speed=700"
            PointerPathGateMode.Cancellation -> "duration=5000,repeat=5"
            PointerPathGateMode.Variable -> "duration=duration,startHold=80,endHold=80"
        }
        val script = buildString {
            appendLine("wait(500)")
            if (mode == PointerPathGateMode.Variable) {
                appendLine("LET duration = 900")
                appendLine("LET route = pointerPath($path,$timing)")
                appendLine("swipe(route)")
            } else {
                appendLine("swipe(pointerPath($path,$timing))")
            }
            append("wait(500)")
        }
        val imported = EmscriptWorkspaceImporter().import(
            script,
            workspaceId = "${if (mode == PointerPathGateMode.Variable) "m5-5" else "m5-4"}" +
                "-device-shell-${mode.name.lowercase()}",
        )
        check(imported.isSuccess) { imported.issues.joinToString { it.message } }

        preferences.edit()
            .putString(WORKSPACE_KEY, WorkflowSerializer.serialize(requireNotNull(imported.document)))
            .putInt(WORKSPACE_VERSION_KEY, Int.MAX_VALUE)
            .putBoolean(TEST_TARGET_KEY, false)
            .putString(
                GESTURE_TEST_MODE_KEY,
                "${if (mode == PointerPathGateMode.Variable) "m5-5" else "m5-4"}" +
                    "-${mode.name.lowercase()}:${System.nanoTime()}",
            )
            .remove(STEPPER_STATE_KEY)
            .commit()
        launchWorkspace(context)
        resultCode = RESULT_PREPARED
        resultData = "${if (mode == PointerPathGateMode.Variable) "M5-5" else "M5-4"} " +
            "${mode.name.lowercase()} gate prepared: $path $timing"
    }

    private fun prepareMultiPointer(context: Context, mode: MultiPointerGateMode) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureBackup(context, preferences)
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2
        val centerY = metrics.heightPixels / 2
        val wideLeft = centerX - metrics.widthPixels / 6
        val wideRight = centerX + metrics.widthPixels / 6
        val nearLeft = centerX - metrics.widthPixels / 28
        val nearRight = centerX + metrics.widthPixels / 28
        val upperY = centerY - metrics.heightPixels / 24
        val lowerY = centerY + metrics.heightPixels / 24
        val durationMs = if (mode == MultiPointerGateMode.Cancellation) 5_000L else 1_000L
        val first = when (mode) {
            MultiPointerGateMode.PinchIn -> pathSource(wideLeft, centerY, nearLeft, centerY)
            MultiPointerGateMode.PinchOut -> pathSource(nearLeft, centerY, wideLeft, centerY)
            MultiPointerGateMode.Parallel,
            MultiPointerGateMode.Delayed,
            MultiPointerGateMode.Cancellation,
            -> pathSource(wideLeft, upperY, wideRight, upperY)
            MultiPointerGateMode.Opposing -> pathSource(wideLeft, upperY, wideRight, upperY)
        }
        val second = when (mode) {
            MultiPointerGateMode.PinchIn -> pathSource(wideRight, centerY, nearRight, centerY)
            MultiPointerGateMode.PinchOut -> pathSource(nearRight, centerY, wideRight, centerY)
            MultiPointerGateMode.Parallel,
            MultiPointerGateMode.Cancellation,
            -> pathSource(wideLeft, lowerY, wideRight, lowerY)
            MultiPointerGateMode.Opposing,
            MultiPointerGateMode.Delayed,
            -> pathSource(wideRight, lowerY, wideLeft, lowerY)
        }
        val secondDelayMs = if (mode == MultiPointerGateMode.Delayed) 250L else 0L
        val secondDurationMs = if (mode == MultiPointerGateMode.Delayed) 600L else durationMs
        // Android cannot add a new pointer after a continued stroke. Keep the delayed gate's
        // pre-activation interval free of section boundaries; holds remain covered by the
        // simultaneous device gates and by the planner tests.
        val startHoldMs = if (mode == MultiPointerGateMode.Delayed) 0L else 80L
        val endHoldMs = if (mode == MultiPointerGateMode.Delayed) 0L else 80L
        val script = buildString {
            appendLine(
                    "LET finger0 = pointerPath($first,pointerId=0,duration=$durationMs," +
                    "startHold=$startHoldMs,endHold=$endHoldMs)",
            )
            appendLine(
                    "LET finger1 = pointerPath($second,pointerId=1,duration=$secondDurationMs," +
                    "startHold=$startHoldMs,endHold=$endHoldMs,delay=$secondDelayMs)",
            )
            appendLine("LET gesture = multiPath([finger0,finger1])")
            append("multiSwipe(gesture)")
        }
        val imported = EmscriptWorkspaceImporter().import(
            script,
            workspaceId = "m5-6-device-shell-${mode.name.lowercase()}",
        )
        check(imported.isSuccess) { imported.issues.joinToString { it.message } }

        preferences.edit()
            .putString(WORKSPACE_KEY, WorkflowSerializer.serialize(requireNotNull(imported.document)))
            .putInt(WORKSPACE_VERSION_KEY, Int.MAX_VALUE)
            .putBoolean(TEST_TARGET_KEY, false)
            .putString(GESTURE_TEST_MODE_KEY, "m5-6-${mode.name.lowercase()}:${System.nanoTime()}")
            .remove(STEPPER_STATE_KEY)
            .commit()
        launchWorkspace(context)
        resultCode = RESULT_PREPARED
        resultData = "M5-6 ${mode.name.lowercase()} gate prepared: duration=$durationMs delay=$secondDelayMs"
    }

    private fun pathSource(fromX: Int, fromY: Int, toX: Int, toY: Int): String =
        "path(point($fromX,$fromY),point($toX,$toY))"

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
        private const val ACTION_PREPARE_LINEAR = "com.visualtasker.wss.debug.M5_4_PREPARE_LINEAR"
        private const val ACTION_PREPARE_QUADRATIC = "com.visualtasker.wss.debug.M5_4_PREPARE_QUADRATIC"
        private const val ACTION_PREPARE_CUBIC = "com.visualtasker.wss.debug.M5_4_PREPARE_CUBIC"
        private const val ACTION_PREPARE_SLOW = "com.visualtasker.wss.debug.M5_4_PREPARE_SLOW"
        private const val ACTION_PREPARE_FAST = "com.visualtasker.wss.debug.M5_4_PREPARE_FAST"
        private const val ACTION_PREPARE_POINTER_CANCELLATION =
            "com.visualtasker.wss.debug.M5_4_PREPARE_CANCELLATION"
        private const val ACTION_PREPARE_VARIABLE_PATH =
            "com.visualtasker.wss.debug.M5_5_PREPARE_VARIABLE_PATH"
        private const val ACTION_PREPARE_PINCH_IN = "com.visualtasker.wss.debug.M5_6_PREPARE_PINCH_IN"
        private const val ACTION_PREPARE_PINCH_OUT = "com.visualtasker.wss.debug.M5_6_PREPARE_PINCH_OUT"
        private const val ACTION_PREPARE_PARALLEL = "com.visualtasker.wss.debug.M5_6_PREPARE_PARALLEL"
        private const val ACTION_PREPARE_OPPOSING = "com.visualtasker.wss.debug.M5_6_PREPARE_OPPOSING"
        private const val ACTION_PREPARE_DELAYED = "com.visualtasker.wss.debug.M5_6_PREPARE_DELAYED"
        private const val ACTION_PREPARE_MULTI_CANCELLATION =
            "com.visualtasker.wss.debug.M5_6_PREPARE_CANCELLATION"
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

    private enum class PointerPathGateMode {
        Linear,
        Quadratic,
        Cubic,
        Slow,
        Fast,
        Cancellation,
        Variable,
    }

    private enum class MultiPointerGateMode {
        PinchIn,
        PinchOut,
        Parallel,
        Opposing,
        Delayed,
        Cancellation,
    }
}
