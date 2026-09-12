package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCapabilityGateTest {
    @Test
    fun inspectReportsDryRunReadyAndBlockedCommands() {
        val imported = EmscriptWorkspaceImporter().import(EditorDefaults.integrationTestScript)
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val report = RuntimeCapabilityGate().inspect(imported.document!!)

        assertFalse(report.realRunAllowed)
        assertTrue(report.capabilities.any { it.command == "wait" && it.status == RuntimeCapabilityStatus.REAL_RUN_READY })
        assertTrue(report.capabilities.any { it.command == "click" && it.status == RuntimeCapabilityStatus.BLOCKED })
        assertTrue(report.capabilities.any { it.command == "beep" && it.status == RuntimeCapabilityStatus.REAL_RUN_READY })
        assertTrue(report.capabilities.any { it.command == "vibrate" && it.status == RuntimeCapabilityStatus.REAL_RUN_READY })
        assertTrue(report.summary.contains("Runtime Gates"))
    }

    @Test
    fun inspectAllowsBasicRealRunWithoutAdapterCommands() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            LET count = 1
            SET count = count + 1
            wait(10)
            log("ok")
            beep(440, 30, 25)
            vibrate(10,20)
            Clipboard.set("ready")
            Clipboard.get()
            Cache.clear()
            Sys.info()
            Env.get("SDK_INT")
            File.writeText("state.txt", "ok")
            File.readText("state.txt")
            """.trimIndent(),
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val report = RuntimeCapabilityGate().inspect(imported.document!!)

        assertTrue(report.realRunAllowed)
        val commands = report.capabilities.map { it.command }.toSet()
        assertTrue(commands.containsAll(setOf("beep", "log", "set", "vibrate", "wait", "Clipboard.set", "Clipboard.get", "Cache.clear", "Sys.info", "Env.get", "File.writeText", "File.readText")))
        assertTrue(report.capabilities.all { it.status == RuntimeCapabilityStatus.REAL_RUN_READY })
    }

    @Test
    fun runtimeGateUsesCatalogCanonicalNames() {
        val imported = EmscriptWorkspaceImporter().import(EditorDefaults.integrationTestScript)
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val report = RuntimeCapabilityGate().inspect(imported.document!!)
        val runtimeNames = VisualTaskerCommandCatalog.acceptedNamesForRuntime().map { it.lowercase() }.toSet()

        assertTrue(report.capabilities.isNotEmpty())
        assertEquals(
            emptySet<String>(),
            report.capabilities.map { it.command }.filterNot { it.lowercase() in runtimeNames }.toSet(),
        )
    }

    @Test
    fun accessibilityAdapterEnablesOnlyImplementedA11yCommands() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            click("Start")
            clickPoint(120, 240, 1)
            swipe([120, 640, 120, 220], 1)
            screenshot("screen.png")
            touch(["down", 120, 240, "up"])
            """.trimIndent(),
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val report = RuntimeCapabilityGate.withAccessibilityAdapter().inspect(imported.document!!)
        val states = report.capabilities.associate { it.command to it.status }

        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["click"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["clickPoint"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["swipe"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["screenshot"])
        assertEquals(RuntimeCapabilityStatus.BLOCKED, states["touch"])
    }

    @Test
    fun deviceAdaptersEnableShizukuAndScrcpyCommandsWhenAvailable() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            ChromeTab.isSupported()
            ChromeTab.open("https://example.com")
            Shizuku.isAvailable()
            Shizuku.systemService("package")
            Shizuku.call("package", "1", ["s16", "com.visualtasker.wss"])
            Shizuku.shell("cmd package list packages")
            Termux.canRunCommands()
            Termux.shell("echo ok")
            Tasker.isInstalled()
            Tasker.runTask("VT_TEST", ["alpha", "beta"])
            Tasker.lastResult("run-1")
            Tasker.error("run-1")
            Tasker.getVariable("%vt")
            Scrcpy.hostAvailable()
            Scrcpy.connect("")
            Scrcpy.touch("", "tap", point(10, 20))
            """.trimIndent(),
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val report = RuntimeCapabilityGate.withDeviceAdapters(
            accessibilityAvailable = false,
            customChromeTabAvailable = true,
            shizukuAvailable = true,
            termuxAvailable = true,
            taskerAvailable = true,
            usbAdbBridgeAvailable = true,
        ).inspect(imported.document!!)
        val states = report.capabilities.associate { it.command to it.status }

        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["ChromeTab.isSupported"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["ChromeTab.open"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Shizuku.isAvailable"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Shizuku.systemService"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Shizuku.call"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Shizuku.shell"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Termux.canRunCommands"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Termux.shell"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Tasker.isInstalled"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Tasker.runTask"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Tasker.lastResult"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Tasker.error"])
        assertEquals(RuntimeCapabilityStatus.BLOCKED, states["Tasker.getVariable"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Scrcpy.hostAvailable"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Scrcpy.connect"])
        assertEquals(RuntimeCapabilityStatus.REAL_RUN_READY, states["Scrcpy.touch"])
    }
}
