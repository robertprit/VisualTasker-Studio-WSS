package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceReleasePreflightTest {
    @Test
    fun acceptsStructurallyValidWorkspaceEvenWithAdapterWarnings() {
        val imported = EmscriptWorkspaceImporter().import(EditorDefaults.integrationTestScript)
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val state = WorkspaceWorkflowState.fromDocument(imported.document!!, "test")

        val report = WorkspaceReleasePreflight.inspect(state)

        assertTrue(report.diagnostics.joinToString(), report.canRelease)
        assertTrue(report.diagnostics.any { it.code == "RELEASE_PREFLIGHT_READY" })
        assertTrue(report.catalogRuntimeCount > 0)
        assertTrue(report.catalogLiveCount > 0)
        assertTrue(report.catalogPlannedCount > 0)
        assertTrue(report.catalogAdapterCount > 0)
        assertTrue(
            report.diagnostics.any {
                it.code == "COMMAND_LIVE_NOT_IMPLEMENTED" && it.sourceId == "input.touch"
            },
        )
        assertFalse(report.diagnostics.any { it.code == "COMMAND_RUNTIME_CONTRACT_MISSING" })
    }

    @Test
    fun blocksReleaseWhenVisualAssetDependencyIsMissing() {
        val imported = EmscriptWorkspaceImporter().import(EditorDefaults.integrationTestScript)
        val missingAsset = WorkspaceResource(
            id = "visual-asset:missing",
            kind = WorkspaceResourceKind.VisualAsset,
            label = "Missing Asset",
            uri = File("/definitely/missing/asset.ema").absolutePath,
            mimeType = EMA_MIME_TYPE,
        )
        val state = WorkspaceWorkflowState.fromDocument(
            document = imported.document!!,
            mutationSource = "test",
            resources = WorkspaceResourceBundle(resources = listOf(missingAsset)),
        )

        val report = WorkspaceReleasePreflight.inspect(state)

        assertFalse(report.canRelease)
        assertTrue(report.diagnostics.any { it.code == "EMA_FILE_MISSING" })
    }
}
