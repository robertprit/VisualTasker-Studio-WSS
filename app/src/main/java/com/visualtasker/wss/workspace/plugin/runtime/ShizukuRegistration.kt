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

internal enum class ShizukuAvailabilityState {
    NOT_INSTALLED,
    PERMISSION_NOT_GRANTED,
    BINDER_NOT_ALIVE,
    AVAILABLE,
}

internal enum class ShizukuAvailabilityFailureStage {
    INSTALLATION,
    PERMISSION,
    BINDER,
    UID,
}

internal data class ShizukuAvailabilityInspection(
    val installation: PackageInstallationInspection,
    val state: ShizukuAvailabilityState? = null,
    val permissionGranted: Boolean? = null,
    val binderAlive: Boolean? = null,
    val uid: Int? = null,
    val uidFailure: Exception? = null,
    val failureStage: ShizukuAvailabilityFailureStage? = null,
    val failure: Exception? = null,
) {
    init {
        require((state != null) xor (failure != null))
        require((failureStage == null) == (failure == null))
    }

    val available: Boolean get() = state == ShizukuAvailabilityState.AVAILABLE
}

internal fun inspectShizukuAvailability(
    installation: PackageInstallationInspection,
    binderProbe: () -> Boolean,
    permissionProbe: (binderAlive: Boolean) -> Boolean,
    uidProbe: () -> Int? = { null },
): ShizukuAvailabilityInspection {
    installation.failure?.let { error ->
        return ShizukuAvailabilityInspection(
            installation = installation,
            failureStage = ShizukuAvailabilityFailureStage.INSTALLATION,
            failure = error,
        )
    }
    if (!installation.installed) {
        return ShizukuAvailabilityInspection(
            installation = installation,
            state = ShizukuAvailabilityState.NOT_INSTALLED,
            permissionGranted = false,
            binderAlive = false,
        )
    }

    val binderAlive = try {
        binderProbe()
    } catch (error: Exception) {
        return ShizukuAvailabilityInspection(
            installation = installation,
            failureStage = ShizukuAvailabilityFailureStage.BINDER,
            failure = error,
        )
    }
    val permissionGranted = try {
        permissionProbe(binderAlive)
    } catch (error: Exception) {
        return ShizukuAvailabilityInspection(
            installation = installation,
            binderAlive = binderAlive,
            failureStage = ShizukuAvailabilityFailureStage.PERMISSION,
            failure = error,
        )
    }
    val state = when {
        !permissionGranted -> ShizukuAvailabilityState.PERMISSION_NOT_GRANTED
        !binderAlive -> ShizukuAvailabilityState.BINDER_NOT_ALIVE
        else -> ShizukuAvailabilityState.AVAILABLE
    }
    val uidResult = if (state == ShizukuAvailabilityState.AVAILABLE) {
        try {
            uidProbe() to null
        } catch (error: Exception) {
            null to error
        }
    } else {
        null to null
    }
    return ShizukuAvailabilityInspection(
        installation = installation,
        state = state,
        permissionGranted = permissionGranted,
        binderAlive = binderAlive,
        uid = uidResult.first,
        uidFailure = uidResult.second,
    )
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

    internal fun inspectInstallation(context: Context): PackageInstallationInspection =
        context.packageManager.inspectInstalledPackages(listOf(SHIZUKU_PACKAGE))

    internal fun inspectAvailability(context: Context): ShizukuAvailabilityInspection {
        val packageManager = context.packageManager
        return inspectShizukuAvailability(
            installation = inspectInstallation(context),
            binderProbe = { Shizuku.pingBinder() },
            permissionProbe = { binderAlive ->
                if (binderAlive) {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } else {
                    packageManager.checkPermission(SHIZUKU_PERMISSION, context.packageName) ==
                        PackageManager.PERMISSION_GRANTED
                }
            },
            uidProbe = { Shizuku.getUid() },
        )
    }

    fun inspect(context: Context): ShizukuRegistrationStatus {
        val packageManager = context.packageManager
        val availability = inspectAvailability(context)
        val launchable = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null
        return ShizukuRegistrationStatus(
            installed = availability.installation.installed,
            permissionGranted = availability.permissionGranted == true,
            launchable = launchable,
            binderAlive = availability.binderAlive == true,
            uid = availability.uid,
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
