package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.workflow.semantics.VisualTaskerCommandCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeepCommandMigrationTest {
    @Test
    fun `canonical beep preserves command identity values and source roundtrip`() {
        val imported = EmscriptWorkspaceImporter().import("beep(880, 150, 75)", workspaceId = "beep-v1")

        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val document = imported.document!!
        val beep = document.blocks.values.single { it.type == BlockTypes.FEEDBACK_BEEP }
        assertEquals(FieldValue.Number(880.0), beep.fields.getValue("frequency"))
        assertEquals(FieldValue.Number(150.0), beep.fields.getValue("durationMs"))
        assertEquals(FieldValue.Number(75.0), beep.fields.getValue("volume"))
        assertEquals("feedback.beep", VisualTaskerCommandCatalog.findByBlockType(beep.type)?.id)

        val generated = EmscriptGenerator(IrGenerator()).generate(document, scriptName = "beep-v1")
        assertTrue(generated.contains("beep(880, 150, 75);"))
    }

    @Test
    fun `beep defaults remain canonical and typed in the workspace`() {
        val imported = EmscriptWorkspaceImporter().import("beep()", workspaceId = "beep-defaults")

        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val document = imported.document!!
        val beep = document.blocks.values.single { it.type == BlockTypes.FEEDBACK_BEEP }
        assertEquals(FieldValue.Number(1000.0), beep.fields.getValue("frequency"))
        assertEquals(FieldValue.Number(200.0), beep.fields.getValue("durationMs"))
        assertEquals(FieldValue.Number(100.0), beep.fields.getValue("volume"))

        val generated = EmscriptGenerator(IrGenerator()).generate(document, scriptName = "beep-defaults")
        assertTrue(generated.contains("beep();"))
    }

    @Test
    fun `accepted legacy beep spelling serializes canonically`() {
        val imported = EmscriptWorkspaceImporter().import("BEEP 660 60 45", workspaceId = "beep-legacy")

        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val generated = EmscriptGenerator(IrGenerator()).generate(imported.document!!, scriptName = "beep-legacy")
        assertTrue(generated.contains("beep(660, 60, 45);"))
    }
}
