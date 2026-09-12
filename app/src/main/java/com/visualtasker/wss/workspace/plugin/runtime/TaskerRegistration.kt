package com.visualtasker.wss.workspace.plugin.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.UUID

const val TASKER_PACKAGE = "net.dinglisch.android.tasker"
const val TASKER_MARKET_PACKAGE = "net.dinglisch.android.taskerm"
const val TASKER_PLUGIN_OWNER = "visualtasker.tasker"

data class TaskerRegistrationStatus(
    val installed: Boolean,
    val packageName: String?,
    val launchable: Boolean,
    val runTaskPermissionGranted: Boolean,
    val taskerEnabled: Boolean?,
    val externalAccessAllowed: Boolean?,
    val receiverAvailable: Boolean,
) {
    val available: Boolean
        get() = installed && runTaskPermissionGranted && taskerEnabled != false && externalAccessAllowed != false && receiverAvailable
    val summary: String
        get() = when {
            !installed -> "Tasker nicht installiert"
            !runTaskPermissionGranted -> "Tasker Run-Task Permission fehlt"
            taskerEnabled == false -> "Tasker ist deaktiviert"
            externalAccessAllowed == false -> "Tasker External Access ist deaktiviert"
            !receiverAvailable -> "Tasker Broadcast-Receiver nicht verfügbar"
            available -> "Tasker bereit (${packageName ?: TASKER_MARKET_PACKAGE})"
            else -> "Tasker installiert; Status nicht vollständig prüfbar"
        }
}

object TaskerRegistration {
    private const val ACTION_TASK = "$TASKER_PACKAGE.ACTION_TASK"
    private const val ACTION_OPEN_PREFS = "$TASKER_PACKAGE.ACTION_OPEN_PREFS"
    private const val EXTRA_OPEN_PREFS_TAB_NO = "tno"
    private const val MISC_PREFS_TAB_NO = 3
    private const val EXTRA_TASK_NAME = "task_name"
    private const val EXTRA_VAR_NAMES_LIST = "varNames"
    private const val EXTRA_VAR_VALUES_LIST = "varValues"
    private const val EXTRA_INTENT_VERSION_NUMBER = "version_number"
    private const val INTENT_VERSION_NUMBER = "1.1"
    private const val TASK_ID_SCHEME = "id"
    private const val TASKER_PREFS_URI = "content://$TASKER_PACKAGE/prefs"
    private const val PROVIDER_COL_NAME_EXTERNAL_ACCESS = "ext_access"
    private const val PROVIDER_COL_NAME_ENABLED = "enabled"
    const val PERMISSION_RUN_TASKS = "$TASKER_PACKAGE.PERMISSION_RUN_TASKS"

    fun inspect(context: Context): TaskerRegistrationStatus {
        val packageManager = context.packageManager
        val installedPackage = listOf(TASKER_PACKAGE, TASKER_MARKET_PACKAGE).firstOrNull { packageName ->
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }
            }.isSuccess
        }
        val launchable = installedPackage?.let { packageManager.getLaunchIntentForPackage(it) != null } ?: false
        val permissionGranted = context.checkPermission(
            PERMISSION_RUN_TASKS,
            Process.myPid(),
            Process.myUid(),
        ) == PackageManager.PERMISSION_GRANTED
        val probeIntent = buildTaskIntent(taskName = "", parameters = emptyList(), variables = emptyMap())
        val receiverAvailable = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryBroadcastReceivers(
                    probeIntent,
                    PackageManager.ResolveInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryBroadcastReceivers(probeIntent, 0)
            }
        }.getOrDefault(emptyList()).isNotEmpty()
        return TaskerRegistrationStatus(
            installed = installedPackage != null,
            packageName = installedPackage,
            launchable = launchable,
            runTaskPermissionGranted = permissionGranted,
            taskerEnabled = taskerPrefSet(context, PROVIDER_COL_NAME_ENABLED),
            externalAccessAllowed = taskerPrefSet(context, PROVIDER_COL_NAME_EXTERNAL_ACCESS),
            receiverAvailable = receiverAvailable,
        )
    }

    fun runTask(
        context: Context,
        taskName: String,
        parameters: List<String> = emptyList(),
        variables: Map<String, String> = emptyMap(),
    ): TaskerRunResult {
        val status = inspect(context)
        if (!status.installed) return TaskerRunResult(false, status.summary)
        if (!status.runTaskPermissionGranted) return TaskerRunResult(false, status.summary)
        if (status.taskerEnabled == false || status.externalAccessAllowed == false || !status.receiverAvailable) {
            return TaskerRunResult(false, status.summary)
        }
        val normalizedName = taskName.trim().trim('"')
        if (normalizedName.isBlank()) {
            return TaskerRunResult(false, "Tasker Taskname fehlt")
        }
        val intent = buildTaskIntent(
            taskName = normalizedName,
            parameters = parameters,
            variables = variables,
        )
        return runCatching {
            context.sendBroadcast(intent)
            TaskerRunResult(
                success = true,
                message = "Tasker.runTask \"$normalizedName\" gesendet (${parameters.size} Parameter, ${variables.size} Variablen)",
            )
        }.getOrElse { error ->
            TaskerRunResult(false, "Tasker.runTask fehlgeschlagen: ${error.message ?: error::class.java.simpleName}")
        }
    }

    fun settingsIntent(status: TaskerRegistrationStatus): Intent =
        if (status.installed) {
            Intent(ACTION_OPEN_PREFS)
                .putExtra(EXTRA_OPEN_PREFS_TAB_NO, MISC_PREFS_TAB_NO)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }

    private fun buildTaskIntent(
        taskName: String,
        parameters: List<String>,
        variables: Map<String, String>,
    ): Intent {
        val names = ArrayList<String>()
        val values = ArrayList<String>()
        parameters.forEachIndexed { index, value ->
            names += "%par${index + 1}"
            values += value
        }
        variables.forEach { (name, value) ->
            names += name.ensureTaskerLocalVariableName()
            values += value
        }
        return Intent(ACTION_TASK)
            .setData(Uri.parse("$TASK_ID_SCHEME:${UUID.randomUUID()}"))
            .putExtra(EXTRA_INTENT_VERSION_NUMBER, INTENT_VERSION_NUMBER)
            .putExtra(EXTRA_TASK_NAME, taskName)
            .apply {
                if (names.isNotEmpty()) {
                    putStringArrayListExtra(EXTRA_VAR_NAMES_LIST, names)
                    putStringArrayListExtra(EXTRA_VAR_VALUES_LIST, values)
                }
            }
    }

    private fun taskerPrefSet(context: Context, column: String): Boolean? =
        runCatching {
            context.contentResolver.query(
                Uri.parse(TASKER_PREFS_URI),
                arrayOf(column),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0).equals("true", ignoreCase = true)
                } else {
                    null
                }
            }
        }.getOrNull()

    private fun String.ensureTaskerLocalVariableName(): String {
        val clean = trim().trim('"')
        return if (clean.startsWith("%")) clean else "%$clean"
    }
}

data class TaskerRunResult(
    val success: Boolean,
    val message: String,
)
