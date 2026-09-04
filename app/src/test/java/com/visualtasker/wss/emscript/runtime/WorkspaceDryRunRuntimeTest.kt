package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceDryRunRuntimeTest {
    @Test
    fun dryRunEmitsBlockAndEdgeTraceFromWorkspaceDocument() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            LET value = 1
            IF value > 0
              log("then")
            ELSE
              log("else")
            END IF
            """.trimIndent(),
            workspaceId = "workspace-dry-run",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val result = WorkspaceDryRunRuntime().run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        val events = (result as EmscriptDryRunResult.Success).events
        assertTrue(events.any { it.blockId != null && it.kind == "if" && it.message == "THEN" })
        assertTrue(events.any { it.edgeSourceBlockId != null && it.edgeTargetBlockId != null && it.edgeKind == "TRUE_BRANCH" })
        assertTrue(events.any { it.blockId != null && it.kind == "log" && it.message == "then" })
        assertTrue(events.none { it.kind == "log" && it.message == "else" })
    }

    @Test
    fun dryRunEvaluatesInlineSetExpressionsForBranchDecisions() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            LET value = 0
            SET value = value + 1
            IF value == 1
              log("updated")
            ELSE
              log("stale")
            END IF
            """.trimIndent(),
            workspaceId = "workspace-dry-run-expression",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val result = WorkspaceDryRunRuntime().run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        val success = result as EmscriptDryRunResult.Success
        assertTrue(success.events.any { it.kind == "set" && it.message == "value = 1" })
        assertTrue(success.events.any { it.kind == "log" && it.message == "updated" })
        assertTrue(success.events.none { it.kind == "log" && it.message == "stale" })
    }

    @Test
    fun dryRunExecutesElseifBranchScenario() {
        val imported = EmscriptWorkspaceImporter().import(
            EditorDefaults.elseifBranchTestScript,
            workspaceId = "workspace-dry-run-elseif",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val result = WorkspaceDryRunRuntime().run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        val success = result as EmscriptDryRunResult.Success
        assertTrue(success.events.any { it.kind == "elseif" && it.message == "ELSEIF" })
        assertTrue(success.events.any { it.kind == "vibrate" })
        assertTrue(success.events.any { it.kind == "log" && it.message == "high" })
        assertTrue(success.events.none { it.kind == "log" && it.message == "low" })
        assertTrue(success.events.none { it.kind == "log" && it.message == "middle" })
    }

    @Test
    fun dryRunExecutesNestedFallbackBeepAndClickScenario() {
        val imported = EmscriptWorkspaceImporter().import(
            EditorDefaults.fallbackBranchTestScript,
            workspaceId = "workspace-dry-run-fallback",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val result = WorkspaceDryRunRuntime().run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        val success = result as EmscriptDryRunResult.Success
        assertTrue(success.events.any { it.kind == "else" && it.message == "ELSE" })
        assertTrue(success.events.any { it.kind == "set" && it.message == "nestedScore = 5" })
        assertTrue(success.events.any { it.kind == "beep" && it.message.contains("440Hz/60ms/35%") })
        assertTrue(success.events.any { it.kind == "click" && it.message.contains("\"fallback\"") })
        assertTrue(success.events.none { it.kind == "log" && it.message == "nested-then" })
    }

    @Test
    fun dryRunMarksWorkspaceCatalogAdaptersAsCapabilityWarnings() {
        val imported = EmscriptWorkspaceImporter().import(
            """
            findTemplate("button.png", 0.8, 1000)
            Termux.shell("echo ok")
            """.trimIndent(),
            workspaceId = "workspace-dry-run-capabilities",
        )
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val result = WorkspaceDryRunRuntime().run(imported.document!!)

        assertTrue(result is EmscriptDryRunResult.Success)
        val events = (result as EmscriptDryRunResult.Success).events
        assertTrue(events.any {
            it.blockId != null &&
                it.kind == "capability" &&
                it.severity == EmscriptDryRunEventSeverity.WARNING &&
                it.command == "findTemplate" &&
                it.capability == "VISION"
        })
        assertTrue(events.any {
            it.blockId != null &&
                it.kind == "capability" &&
                it.command == "Termux.shell" &&
                it.pluginOwner == "visualtasker.termux"
        })
    }
}
