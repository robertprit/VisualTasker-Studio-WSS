package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.workflow.semantics.VisualTaskerCommandCatalog
import de.visualtasker.emscript.contract.EmscriptV1OperatorIds
import de.visualtasker.emscript.contract.EmscriptV1Operators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventStartCommandMigrationTest {
    @Test
    fun `implicit start remains singular and omitted across source roundtrip`() {
        val first = EmscriptWorkspaceImporter().import("wait(1)", workspaceId = "event-start-v1")

        assertTrue(first.issues.joinToString { it.message }, first.isSuccess)
        val firstDocument = first.document!!
        val starts = firstDocument.blocks.values.filter { it.type == BlockTypes.EVENT_START }
        assertEquals(1, starts.size)
        assertEquals("event.start", VisualTaskerCommandCatalog.findByBlockType(starts.single().type)?.id)

        val generated = EmscriptGenerator(IrGenerator()).generate(firstDocument, scriptName = "event-start-v1")
        assertTrue(generated.contains("wait(1);"))
        assertFalse(generated.contains("onStart", ignoreCase = true))

        val second = EmscriptWorkspaceImporter().import(generated, workspaceId = "event-start-v1-roundtrip")
        assertTrue(second.issues.joinToString { it.message }, second.isSuccess)
        assertEquals(1, second.document!!.blocks.values.count { it.type == BlockTypes.EVENT_START })
    }

    @Test
    fun `operator literal and variable candidates retain their expression models`() {
        val source = """
            LET score = 2.5
            IF (score > 1.25) && (true || false) {
                wait(1)
            }
        """.trimIndent()
        val imported = EmscriptWorkspaceImporter().import(source, workspaceId = "expression-models")

        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val blocks = imported.document!!.blocks.values
        assertEquals(1, blocks.count { it.type == BlockTypes.EVENT_START })
        assertTrue(blocks.any { it.type == BlockTypes.LOGIC_AND })
        assertTrue(blocks.any { it.type == BlockTypes.LOGIC_OR })
        assertEquals("&&", EmscriptV1Operators.requireDefinition(EmscriptV1OperatorIds.AND).symbol)
        assertEquals("||", EmscriptV1Operators.requireDefinition(EmscriptV1OperatorIds.OR).symbol)
        val decimalValues = blocks
            .filter { it.type == BlockTypes.LITERAL_NUMBER }
            .mapNotNull { (it.fields["value"] as? FieldValue.Number)?.value }
        assertTrue(decimalValues.contains(1.25))
        val variableReporter = blocks.single { it.type.startsWith("${BlockTypes.VARIABLE_REPORTER}.") }
        assertEquals(FieldValue.Text("score"), variableReporter.fields["variableId"])
        assertEquals(FieldValue.Text("score"), variableReporter.fields["variableLabel"])
        assertFalse(blocks.any { VisualTaskerCommandCatalog.findByBlockType(it.type)?.id == "variable.get" })
    }
}
