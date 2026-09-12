package com.visualtasker.wss.workspace.plugin.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

const val TERMUX_PACKAGE = "com.termux"
const val TERMUX_API_PACKAGE = "com.termux.api"
const val TERMUX_RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
const val TERMUX_PLUGIN_OWNER = "visualtasker.termux"
const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
const val TERMUX_HOME = "/data/data/com.termux/files/home"

private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
private const val EXTRA_STDIN = "com.termux.RUN_COMMAND_STDIN"
private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_LABEL"
private const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_DESCRIPTION"

data class TermuxRegistrationStatus(
    val installed: Boolean,
    val apiInstalled: Boolean,
    val runCommandPermissionGranted: Boolean,
    val launchable: Boolean,
) {
    val canRunCommands: Boolean get() = installed && runCommandPermissionGranted
    val summary: String
        get() = when {
            canRunCommands -> "Termux RUN_COMMAND bereit"
            installed -> "Termux installiert, RUN_COMMAND Permission offen"
            else -> "Termux nicht installiert"
        }
}

data class TermuxCommandRequest(
    val path: String,
    val arguments: List<String> = emptyList(),
    val stdin: String = "",
    val workdir: String = TERMUX_HOME,
    val background: Boolean = false,
    val label: String = "VisualTasker WSS",
    val description: String = "EMScript Termux command",
)

data class TermuxCommandResult(
    val accepted: Boolean,
    val message: String,
) {
    val success: Boolean get() = accepted
}

object TermuxRegistration {
    fun inspect(context: Context): TermuxRegistrationStatus {
        val packageManager = context.packageManager
        val installed = packageManager.isPackageInstalled(TERMUX_PACKAGE)
        val apiInstalled = packageManager.isPackageInstalled(TERMUX_API_PACKAGE)
        val permissionGranted =
            packageManager.checkPermission(TERMUX_RUN_COMMAND_PERMISSION, context.packageName) ==
                PackageManager.PERMISSION_GRANTED
        val launchable = packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE) != null
        return TermuxRegistrationStatus(
            installed = installed,
            apiInstalled = apiInstalled,
            runCommandPermissionGranted = permissionGranted,
            launchable = launchable,
        )
    }

    fun buildShellRequest(commandLine: String, background: Boolean = false): TermuxCommandRequest =
        TermuxCommandRequest(
            path = "/system/bin/sh",
            arguments = listOf("-lc", commandLine),
            background = background,
            label = "VisualTasker Shell",
            description = commandLine.take(220),
        )

    fun buildRunRequest(path: String, args: List<String>, background: Boolean = false): TermuxCommandRequest =
        TermuxCommandRequest(
            path = path.trim().ifBlank { "$TERMUX_PREFIX/bin/bash" },
            arguments = args,
            background = background,
            label = "VisualTasker Termux.run",
            description = path.trim().ifBlank { "bash" },
        )

    fun buildApiRequest(command: String, args: List<String> = emptyList()): TermuxCommandRequest =
        buildRunRequest(
            path = "$TERMUX_PREFIX/bin/termux-${command.trim().removePrefix("termux-")}",
            args = args,
            background = true,
        )

    fun runCommand(context: Context, request: TermuxCommandRequest): TermuxCommandResult {
        val status = inspect(context)
        if (!status.installed) return TermuxCommandResult(false, "Termux ist nicht installiert.")
        if (!status.runCommandPermissionGranted) {
            return TermuxCommandResult(
                false,
                "Termux RUN_COMMAND Permission fehlt. Zusätzlich in Termux allow-external-apps=true setzen.",
            )
        }
        if (request.path.isBlank()) return TermuxCommandResult(false, "Leerer Termux-Befehlspfad.")

        val intent = Intent(ACTION_RUN_COMMAND)
            .setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
            .putExtra(EXTRA_COMMAND_PATH, request.path)
            .putExtra(EXTRA_ARGUMENTS, request.arguments.toTypedArray())
            .putExtra(EXTRA_WORKDIR, request.workdir)
            .putExtra(EXTRA_BACKGROUND, request.background)
            .putExtra(EXTRA_SESSION_ACTION, "0")
            .putExtra(EXTRA_COMMAND_LABEL, request.label)
            .putExtra(EXTRA_COMMAND_DESCRIPTION, request.description)
        if (request.stdin.isNotBlank()) {
            intent.putExtra(EXTRA_STDIN, request.stdin)
        }

        return runCatching {
            context.startService(intent)
            TermuxCommandResult(
                accepted = true,
                message = "Termux Befehl übergeben: ${request.path} ${request.arguments.joinToString(" ")}".trim(),
            )
        }.getOrElse { error ->
            TermuxCommandResult(false, "Termux RUN_COMMAND fehlgeschlagen: ${error.message.orEmpty()}")
        }
    }

    fun settingsIntent(status: TermuxRegistrationStatus): Intent =
        if (status.installed) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$TERMUX_PACKAGE"))
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
}

private fun PackageManager.isPackageInstalled(packageName: String): Boolean =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, 0)
        }
    }.isSuccess
