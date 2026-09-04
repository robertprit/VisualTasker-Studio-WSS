package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceBasicRuntimeTest {
    @Test
    fun basicRuntimeExecutesLocalSideEffectsFromWorkspaceOrder() = runBlocking {
        val imported = EmscriptWorkspaceImporter().import(
            """
            LET count = 0
            LOOP 2
              SET count = count + 1
              IF count == 1
                log("first")
                beep(440, 30, 25)
              ELSE
                vibrate(10,20)
              END IF
              wait(5)
            END LOOP
            """.trimIndent(),
            workspaceId = "workspace-basic-runtime",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val calls = mutableListOf<String>()
        val runtime = WorkspaceBasicRuntime(
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = { calls += "wait:$it" },
                playBeep = { hz, duration, volume -> calls += "beep:$hz:$duration:$volume" },
                vibrate = { pattern -> calls += "vibrate:${pattern.joinToString(",")}" },
                log = { calls += "log:$it" },
            ),
        )

        val result = runtime.run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        val success = result as EmscriptDryRunResult.Success
        assertEquals(EmscriptValue.NumberValue(2.0), success.variables["count"])
        assertEquals(
            listOf(
                "log:first",
                "beep:440:30:25",
                "wait:5",
                "vibrate:10,20",
                "wait:5",
            ),
            calls,
        )
        assertTrue(success.events.any { it.kind == "live" && it.message.contains("beep") })
        assertTrue(success.events.any { it.kind == "done" && it.message.contains("Basic-Run") })
    }

    @Test
    fun basicRuntimeBlocksAdapterCommandsBeforeSideEffects() = runBlocking {
        val imported = EmscriptWorkspaceImporter().import(
            """
            log("before")
            click("OK")
            """.trimIndent(),
            workspaceId = "workspace-basic-runtime-blocked",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val calls = mutableListOf<String>()
        val runtime = WorkspaceBasicRuntime(
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = { calls += "wait:$it" },
                playBeep = { hz, duration, volume -> calls += "beep:$hz:$duration:$volume" },
                vibrate = { pattern -> calls += "vibrate:${pattern.joinToString(",")}" },
                log = { calls += "log:$it" },
            ),
        )

        val result = runtime.run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Failure)
        assertTrue((result as EmscriptDryRunResult.Failure).message.contains("blockiert"))
        assertEquals(emptyList<String>(), calls)
    }
}
