package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSyncGuardTest {
    @Test
    fun inspectAcceptsRoundtrippableWorkspace() {
        val imported = EmscriptWorkspaceImporter().import(EditorDefaults.integrationTestScript)
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val report = WorkspaceSyncGuard().inspect(WorkspaceSerializer.serialize(imported.document!!))

        assertTrue(report.messages.joinToString(), report.isValid)
        assertTrue(report.messages.any { it.contains("EMScript-Projektion OK") })
        assertTrue(report.messages.any { it.contains("Flowchart-Projektion OK") })
    }

    @Test
    fun inspectRejectsMalformedWorkspaceJson() {
        val report = WorkspaceSyncGuard().inspect("{not-json")

        assertFalse(report.isValid)
        assertTrue(report.messages.any { it.contains("fehlerhaft") })
    }
}
