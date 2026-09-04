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

    @Test
    fun basicRuntimeDispatchesImplementedAccessibilityCommandsWhenAdapterIsEnabled() = runBlocking {
        val imported = EmscriptWorkspaceImporter().import(
            """
            click("Start")
            clickPoint(12, 24, 2)
            swipe([10, 20, 30, 40], 1)
            screenshot("screen.png")
            """.trimIndent(),
            workspaceId = "workspace-basic-runtime-a11y",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val calls = mutableListOf<String>()
        val runtime = WorkspaceBasicRuntime(
            capabilityGate = { RuntimeCapabilityGate.withAccessibilityAdapter() },
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = { calls += "wait:$it" },
                playBeep = { hz, duration, volume -> calls += "beep:$hz:$duration:$volume" },
                vibrate = { pattern -> calls += "vibrate:${pattern.joinToString(",")}" },
                log = { calls += "log:$it" },
                clickText = { calls += "clickText:$it"; true },
                clickPoint = { x, y -> calls += "clickPoint:$x:$y"; true },
                swipe = { points, duration ->
                    calls += "swipe:${points.joinToString("|") { "${it.x},${it.y}" }}:$duration"
                    true
                },
                screenshot = { path -> calls += "screenshot:$path"; true },
            ),
        )

        val result = runtime.run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        assertEquals(
            listOf(
                "clickText:Start",
                "clickPoint:12:24",
                "clickPoint:12:24",
                "swipe:10,20|30,40:250",
                "screenshot:screen.png",
            ),
            calls,
        )
    }

    @Test
    fun basicRuntimeExecutesCoreSystemCommands() = runBlocking {
        val imported = EmscriptWorkspaceImporter().import(
            """
            Clipboard.set("alpha")
            Clipboard.get()
            Cache.clear()
            Sys.info()
            Env.get("SDK_INT")
            File.writeText("state.txt", "ok")
            File.readText("state.txt")
            """.trimIndent(),
            workspaceId = "workspace-basic-runtime-core",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val calls = mutableListOf<String>()
        val files = mutableMapOf<String, String>()
        var clipboard = ""
        val runtime = WorkspaceBasicRuntime(
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = { calls += "wait:$it" },
                playBeep = { hz, duration, volume -> calls += "beep:$hz:$duration:$volume" },
                vibrate = { pattern -> calls += "vibrate:${pattern.joinToString(",")}" },
                log = { calls += "log:$it" },
                clipboardGet = { clipboard },
                clipboardSet = { clipboard = it; calls += "clipboardSet:$it" },
                cacheClear = { calls += "cacheClear"; 3 },
                systemInfo = { "system-info" },
                envGet = { name -> "env:$name" },
                fileWriteText = { path, text -> files[path] = text; calls += "fileWrite:$path:$text"; true },
                fileReadText = { path -> files[path].also { calls += "fileRead:$path:${it.orEmpty()}" } },
            ),
        )

        val result = runtime.run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        assertEquals("alpha", clipboard)
        assertEquals("ok", files["state.txt"])
        assertTrue(calls.contains("clipboardSet:alpha"))
        assertTrue(calls.contains("cacheClear"))
        assertTrue(calls.contains("log:system-info"))
        assertTrue(calls.contains("log:Env.get(SDK_INT) -> env:SDK_INT"))
        assertTrue(calls.contains("fileWrite:state.txt:ok"))
        assertTrue(calls.contains("fileRead:state.txt:ok"))
    }
}
