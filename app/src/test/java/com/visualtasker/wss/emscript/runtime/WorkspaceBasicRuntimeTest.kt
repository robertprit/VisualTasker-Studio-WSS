package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun basicRuntimeMarksFailedLiveSideEffectsAsWarnings() = runBlocking {
        val imported = EmscriptWorkspaceImporter().import(
            """
            click("Missing")
            clickPoint(12, 24, 1)
            screenshot("missing.png")
            """.trimIndent(),
            workspaceId = "workspace-basic-runtime-live-warning",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val runtime = WorkspaceBasicRuntime(
            capabilityGate = { RuntimeCapabilityGate.withAccessibilityAdapter() },
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = {},
                playBeep = { _, _, _ -> },
                vibrate = {},
                log = {},
                clickText = { false },
                clickPoint = { _, _ -> false },
                screenshot = { false },
            ),
        )

        val result = runtime.run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        val events = (result as EmscriptDryRunResult.Success).events
        assertTrue(events.any {
            it.kind == "live" &&
                it.severity == EmscriptDryRunEventSeverity.WARNING &&
                it.message.contains("click(\"Missing\") fehlgeschlagen")
        })
        assertTrue(events.any {
            it.kind == "live" &&
                it.severity == EmscriptDryRunEventSeverity.WARNING &&
                it.message.contains("clickPoint(12,24,1) fehlgeschlagen")
        })
        assertTrue(events.any {
            it.kind == "live" &&
                it.severity == EmscriptDryRunEventSeverity.WARNING &&
                it.message.contains("screenshot(missing.png) fehlgeschlagen")
        })
        assertTrue(events.any {
            it.kind == "done" &&
                it.severity == EmscriptDryRunEventSeverity.WARNING &&
                it.message.contains("Warnungen")
        })
        val summary = result.traceSummary()
        assertTrue(summary.completed)
        assertEquals(4, summary.warningCount)
        assertEquals(0, summary.errorCount)
    }

    @Test
    fun basicRuntimeCanStopOnFailedLiveSideEffects() = runBlocking {
        val imported = EmscriptWorkspaceImporter().import(
            """
            click("Missing")
            log("after")
            """.trimIndent(),
            workspaceId = "workspace-basic-runtime-stop-on-warning",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val calls = mutableListOf<String>()
        val runtime = WorkspaceBasicRuntime(
            capabilityGate = { RuntimeCapabilityGate.withAccessibilityAdapter() },
            config = WorkspaceBasicRuntimeConfig(stopOnLiveWarning = true),
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = {},
                playBeep = { _, _, _ -> },
                vibrate = {},
                log = { calls += it },
                clickText = { false },
            ),
        )

        val result = runtime.run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Failure)
        assertTrue((result as EmscriptDryRunResult.Failure).message.contains("fehlgeschlagen"))
        assertEquals(emptyList<String>(), calls)
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

    @Test
    fun basicRuntimeExecutesDatastoreMarkerAndTemplateCommands() = runBlocking {
        val imported = EmscriptWorkspaceImporter().import(
            """
            datastorePut("score", "42")
            datastoreGet("score")
            markerSave("button", region(10, 20, 30, 40), "region", 0.90)
            markerLoad("button")
            templateDefine("buttonTpl", region(10, 20, 30, 40), "grayscale")
            templateCompare("buttonTpl", region(10, 20, 30, 40), "grayscale")
            findTemplate("buttonTpl.png", 0.8, 1000, 1, region(10, 20, 30, 40))
            markerDelete("button")
            """.trimIndent(),
            workspaceId = "workspace-basic-runtime-datastore-marker",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val calls = mutableListOf<String>()
        val datastore = mutableMapOf<String, String>()
        val markers = mutableMapOf<String, RuntimeAutomationRegion>()
        val templates = mutableMapOf<String, RuntimeAutomationRegion>()
        val runtime = WorkspaceBasicRuntime(
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = {},
                playBeep = { _, _, _ -> },
                vibrate = {},
                log = { calls += "log:$it" },
                markerSave = { name, region, mode, threshold ->
                    calls += "markerSave:$name:${region.width}x${region.height}:$mode:$threshold"
                    markers[name] = region
                    true
                },
                markerLoad = { name ->
                    calls += "markerLoad:$name"
                    markers[name]
                },
                markerDelete = { name ->
                    calls += "markerDelete:$name"
                    markers.remove(name) != null
                },
                templateDefine = { name, region, processing ->
                    calls += "templateDefine:$name:${region.x},${region.y}:$processing"
                    templates[name] = region
                    true
                },
                templateCompare = { name, region, processing ->
                    calls += "templateCompare:$name:${region.width}x${region.height}:$processing"
                    if (templates[name] == region) 0.97f else null
                },
                findTemplate = { name, threshold, timeoutMs, region ->
                    calls += "findTemplate:$name:$threshold:$timeoutMs:${region?.width}x${region?.height}"
                    templates[name.substringBeforeLast('.')]
                        ?.let { RuntimeTemplateMatch(name, it, 0.96f) }
                },
                datastorePut = { key, value ->
                    calls += "datastorePut:$key:$value"
                    datastore[key] = value
                },
                datastoreGet = { key ->
                    calls += "datastoreGet:$key"
                    datastore[key]
                },
            ),
        )

        val result = runtime.run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        assertEquals("42", datastore["score"])
        assertFalse(markers.containsKey("button"))
        assertEquals(RuntimeAutomationRegion(10, 20, 30, 40), templates["buttonTpl"])
        assertTrue(calls.contains("datastorePut:score:42"))
        assertTrue(calls.contains("datastoreGet:score"))
        assertTrue(calls.any { it.startsWith("markerSave:button:30x40") })
        assertTrue(calls.contains("markerLoad:button"))
        assertTrue(calls.any { it.startsWith("templateDefine:buttonTpl:10,20") })
        assertTrue(calls.any { it.startsWith("templateCompare:buttonTpl:30x40") })
        assertTrue(calls.joinToString(), calls.any { it.startsWith("findTemplate:buttonTpl.png:0.8:1000:30x40") })
        assertTrue(calls.contains("markerDelete:button"))
    }
}
