package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
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
        assertTrue(report.capabilities.any { it.command == "wait" && it.status == RuntimeCapabilityStatus.DRY_RUN_READY })
        assertTrue(report.capabilities.any { it.command == "click" && it.status == RuntimeCapabilityStatus.BLOCKED })
        assertTrue(report.capabilities.any { it.command == "beep" && it.status == RuntimeCapabilityStatus.BLOCKED })
        assertTrue(report.summary.contains("Runtime Gates"))
    }
}
