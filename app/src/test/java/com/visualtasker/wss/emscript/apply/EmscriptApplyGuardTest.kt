package com.visualtasker.wss.emscript.apply

import com.visualtasker.wss.emscript.editor.EditorDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmscriptApplyGuardTest {
    @Test
    fun previewAcceptsValidDraftAndProducesSerializedWorkspace() {
        val result = EmscriptApplyGuard().preview("LET foo = 1")

        assertTrue(result is EmscriptApplyGuardResult.Success)
        result as EmscriptApplyGuardResult.Success
        assertTrue(result.blockCount >= 2)
        assertTrue(result.serializedWorkspaceJson.contains("foo"))
        assertTrue(result.summary.contains("Draft -> Parse -> Import -> Validate: OK"))
    }

    @Test
    fun previewReturnsFailureForInvalidDraft() {
        val result = EmscriptApplyGuard().preview("THIS IS NOT EMSCRIPT")

        assertTrue(result is EmscriptApplyGuardResult.Failure)
        result as EmscriptApplyGuardResult.Failure
        assertEquals(EmscriptApplyGuardStage.PARSE_IMPORT, result.stage)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun previewAcceptsIntegrationTestDraft() {
        val result = EmscriptApplyGuard().preview(EditorDefaults.integrationTestScript)

        assertTrue(result is EmscriptApplyGuardResult.Success)
        result as EmscriptApplyGuardResult.Success
        assertTrue(result.blockCount > 20)
        assertTrue(result.summary.contains("Roundtrip-Script-Länge"))
    }
}
