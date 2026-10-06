package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.workflow.semantics.VisualTaskerCommandCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitCommandMigrationTest {
    @Test
    fun `canonical wait preserves command identity value and source roundtrip`() {
        val imported = EmscriptWorkspaceImporter().import("wait(275)", workspaceId = "wait-v1")

        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val document = imported.document!!
        val wait = document.blocks.values.single { it.type == BlockTypes.ACTION_WAIT }
        assertEquals(FieldValue.Number(275.0), wait.fields.getValue("ms"))
        assertEquals(
            "action.wait",
            VisualTaskerCommandCatalog.findByBlockType(wait.type)?.id,
        )

        val generated = EmscriptGenerator(IrGenerator()).generate(document, scriptName = "wait-v1")
        assertTrue(generated.contains("wait(275);"))
    }

    @Test
    fun `accepted legacy wait spelling serializes canonically`() {
        val imported = EmscriptWorkspaceImporter().import("WAIT 325", workspaceId = "wait-legacy")

        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val generated = EmscriptGenerator(IrGenerator()).generate(imported.document!!, scriptName = "wait-legacy")
        assertTrue(generated.contains("wait(325);"))
    }
}
