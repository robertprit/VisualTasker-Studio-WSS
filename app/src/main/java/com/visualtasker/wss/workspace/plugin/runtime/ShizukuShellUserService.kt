package com.visualtasker.wss.workspace.plugin.runtime

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ShizukuShellUserService : IShizukuShellService.Stub {
    @Suppress("unused")
    constructor()

    @Suppress("unused", "UNUSED_PARAMETER")
    constructor(context: Context)

    override fun execute(commandLine: String, timeoutMs: Long): String {
        if (commandLine.isBlank()) {
            return encodeShellResult(null, "", "Leerer Shizuku-Befehl", timedOut = false)
        }
        return runCatching {
            val process = ProcessBuilder("/system/bin/sh", "-c", commandLine)
                .redirectErrorStream(false)
                .start()
            var output = ""
            var error = ""
            val stdoutReader = thread(start = true, name = "wss-shizuku-stdout") {
                output = process.inputStream.bufferedReader().readText()
            }
            val stderrReader = thread(start = true, name = "wss-shizuku-stderr") {
                error = process.errorStream.bufferedReader().readText()
            }
            val timeout = timeoutMs.coerceIn(100L, 60_000L)
            val finished = process.waitFor(timeout, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                encodeShellResult(null, "", "Timeout nach ${timeout}ms", timedOut = true)
            } else {
                stdoutReader.join(500L)
                stderrReader.join(500L)
                encodeShellResult(
                    exitCode = process.exitValue(),
                    output = output,
                    error = error,
                    timedOut = false,
                )
            }
        }.getOrElse { error ->
            encodeShellResult(null, "", error.message ?: error::class.java.simpleName, timedOut = false)
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}

private fun encodeShellResult(
    exitCode: Int?,
    output: String,
    error: String,
    timedOut: Boolean,
): String = listOf(
    exitCode?.toString().orEmpty(),
    if (timedOut) "1" else "0",
    output.base64(),
    error.base64(),
).joinToString("\t")

private fun String.base64(): String =
    android.util.Base64.encodeToString(toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
