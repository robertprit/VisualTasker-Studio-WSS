package com.visualtasker.wss.workspace.plugin.runtime

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Base64
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
const val SHIZUKU_PERMISSION = "moe.shizuku.manager.permission.API_V23"
const val SHIZUKU_PLUGIN_OWNER = "visualtasker.shizuku"

data class ShizukuRegistrationStatus(
    val installed: Boolean,
    val permissionGranted: Boolean,
    val launchable: Boolean,
    val binderAlive: Boolean = false,
    val uid: Int? = null,
) {
    val registered: Boolean get() = installed
    val available: Boolean get() = installed && permissionGranted && binderAlive
    val legitimized: Boolean get() = installed && permissionGranted
    val summary: String
        get() = when {
            available -> "Shizuku aktiv als UID ${uid ?: "?"}"
            permissionGranted -> "Shizuku erlaubt, Dienst nicht aktiv"
            installed -> "Shizuku installiert, Permission offen"
            else -> "Shizuku nicht installiert"
        }
}

data class ShizukuShellResult(
    val exitCode: Int?,
    val output: String,
    val error: String,
    val timedOut: Boolean = false,
) {
    val success: Boolean get() = exitCode == 0 && !timedOut
    val summary: String
        get() = when {
            timedOut -> "Shizuku shell timeout"
            success -> output.ifBlank { "Shizuku shell ok" }.lineSequence().firstOrNull().orEmpty()
            else -> error.ifBlank { output }.ifBlank { "Shizuku shell exit=$exitCode" }.lineSequence().firstOrNull().orEmpty()
        }
}

object ShizukuRegistration {
    private const val REQUEST_CODE = 7401

    fun inspect(context: Context): ShizukuRegistrationStatus {
        val packageManager = context.packageManager
        val installed = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(SHIZUKU_PACKAGE, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            }
        }.isSuccess
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val permissionGranted = if (binderAlive) {
            runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
                .getOrDefault(false)
        } else {
            packageManager.checkPermission(SHIZUKU_PERMISSION, context.packageName) ==
                PackageManager.PERMISSION_GRANTED
        }
        val uid = if (binderAlive && permissionGranted) runCatching { Shizuku.getUid() }.getOrNull() else null
        val launchable = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null
        return ShizukuRegistrationStatus(
            installed = installed,
            permissionGranted = permissionGranted,
            launchable = launchable,
            binderAlive = binderAlive,
            uid = uid,
        )
    }

    fun requestPermissionIfPossible(): Boolean =
        runCatching {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE)
                true
            } else {
                false
            }
        }.getOrDefault(false)

    suspend fun runShell(commandLine: String, timeoutMs: Long = 10_000): ShizukuShellResult =
        if (commandLine.isBlank()) {
            ShizukuShellResult(null, "", "Leerer Shizuku-Befehl")
        } else {
            runShell(context = null, commandLine = commandLine, timeoutMs = timeoutMs)
        }

    suspend fun runShell(context: Context?, commandLine: String, timeoutMs: Long = 10_000): ShizukuShellResult {
        if (commandLine.isBlank()) return ShizukuShellResult(null, "", "Leerer Shizuku-Befehl")
        val appContext = context?.applicationContext
            ?: return ShizukuShellResult(null, "", "Shizuku Shell benötigt App-Kontext")
        val status = inspect(appContext)
        if (!status.installed) return ShizukuShellResult(null, "", status.summary)
        if (!status.permissionGranted) return ShizukuShellResult(null, "", status.summary)
        if (!status.binderAlive) return ShizukuShellResult(null, "", status.summary)
        val args = Shizuku.UserServiceArgs(ComponentName(appContext, ShizukuShellUserService::class.java))
            .tag("visualtasker-wss-shell")
            .version(1)
            .debuggable(true)
            .processNameSuffix("shizuku_shell")
        return withTimeoutOrNull(timeoutMs.coerceAtLeast(100L) + 5_000L) {
            suspendCancellableCoroutine { continuation ->
                lateinit var connection: ServiceConnection
                fun unbindQuietly() {
                    runCatching { Shizuku.unbindUserService(args, connection, false) }
                }
                connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        Thread {
                            val result = runCatching {
                                val shell = IShizukuShellService.Stub.asInterface(service)
                                decodeShellResult(shell.execute(commandLine, timeoutMs))
                            }.getOrElse { error ->
                                ShizukuShellResult(
                                    exitCode = null,
                                    output = "",
                                    error = "Shizuku shell fehlgeschlagen: ${error.message ?: error::class.java.simpleName}",
                                )
                            }
                            unbindQuietly()
                            if (continuation.isActive) continuation.resume(result)
                        }.start()
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        if (continuation.isActive) {
                            continuation.resume(ShizukuShellResult(null, "", "Shizuku UserService getrennt"))
                        }
                    }
                }
                continuation.invokeOnCancellation { unbindQuietly() }
                runCatching {
                    Shizuku.bindUserService(args, connection)
                }.onFailure { error ->
                    if (continuation.isActive) {
                        continuation.resume(
                            ShizukuShellResult(
                                exitCode = null,
                                output = "",
                                error = "Shizuku UserService bind fehlgeschlagen: ${error.message ?: error::class.java.simpleName}",
                            ),
                        )
                    }
                }
            }
        } ?: ShizukuShellResult(null, "", "Shizuku UserService timeout", timedOut = true)
    }

    fun settingsIntent(status: ShizukuRegistrationStatus): Intent =
        if (status.installed) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$SHIZUKU_PACKAGE"))
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }

    private fun decodeShellResult(encoded: String): ShizukuShellResult {
        val parts = encoded.split('\t')
        if (parts.size < 4) return ShizukuShellResult(null, "", "Ungültige Shizuku Shell-Antwort")
        return ShizukuShellResult(
            exitCode = parts[0].toIntOrNull(),
            output = parts[2].decodeBase64(),
            error = parts[3].decodeBase64(),
            timedOut = parts[1] == "1",
        )
    }

    private fun String.decodeBase64(): String =
        runCatching { String(Base64.decode(this, Base64.NO_WRAP), Charsets.UTF_8) }.getOrDefault("")
}
