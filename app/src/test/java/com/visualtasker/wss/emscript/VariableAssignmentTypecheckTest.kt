package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardStage
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.validation.Validator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VariableAssignmentTypecheckTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun compatibleAssignmentsSurviveTextWorkspaceIrRoundtrip() {
        val scripts = listOf(
            "LET x = 1\nSET x = 2",
            "LET x = 1\nSET x = 1 + 2",
            "LET s = \"a\"\nSET s = \"b\"",
            "LET b = true\nSET b = false",
            "LET x = 1\nLET y = 2\nSET x = y",
            "LET x = 1.5\nSET x = 2.75",
        )

        scripts.forEach { source ->
            val first = guard.preview(source)
            assertTrue("$source -> $first", first is EmscriptApplyGuardResult.Success)
            first as EmscriptApplyGuardResult.Success
            val registry = first.importedDocument.registryWithVariables()
            val generated = EmscriptGenerator(IrGenerator(registry)).generate(first.importedDocument)
            val second = guard.preview(generated)
            assertTrue("$generated -> $second", second is EmscriptApplyGuardResult.Success)
        }
    }

    @Test
    fun incompatibleAssignmentsProduceStableStructuredDiagnostic() {
        val scripts = listOf(
            "LET x = 1\nSET x = \"1\"" to ("Number" to "Text"),
            "LET x = 1\nSET x = true" to ("Number" to "Boolean"),
            "LET b = true\nSET b = 1" to ("Boolean" to "Number"),
        )

        scripts.forEach { (source, types) ->
            val result = guard.preview(source)
            assertTrue("$source -> $result", result is EmscriptApplyGuardResult.Failure)
            result as EmscriptApplyGuardResult.Failure
            assertEquals(EmscriptApplyGuardStage.PRE_VALIDATE, result.stage)
            assertEquals("EMSCRIPT_ASSIGNMENT_TYPE_MISMATCH", result.diagnosticCode)
            assertTrue(result.message.contains(types.first))
            assertTrue(result.message.contains(types.second))
        }
    }

    @Test
    fun variableLabelRenameDoesNotChangeIdBasedAssignmentType() {
        val success = guard.preview("LET stableId = 1\nSET stableId = 2") as EmscriptApplyGuardResult.Success
        val original = success.importedDocument
        val renamedVariable = original.variables.variables.getValue("stableId").copy(name = "Anzeige Name")
        val renamedBlocks = original.blocks.mapValues { (_, block) ->
            if (block.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX)) {
                block.copy(fields = block.fields + ("variableLabel" to FieldValue.Text("Anzeige Name")))
            } else {
                block
            }
        }
        val renamed = original.copy(
            blocks = renamedBlocks,
            variables = original.variables.copy(
                variables = original.variables.variables + ("stableId" to renamedVariable),
            ),
        )

        val validation = Validator.validate(renamed, renamed.registryWithVariables())

        assertTrue(validation.errors.toString(), validation.isValid)
        val setBlocks = renamed.blocks.values.filter { it.type == BlockTypes.VARIABLE_SET }
        assertFalse(setBlocks.isEmpty())
        assertTrue(setBlocks.all { it.fields["variableId"] == FieldValue.Text("stableId") })
    }

    @Test
    fun setUsesExpressionInputAndDoesNotCreateSourceLevelVariableGet() {
        val success = guard.preview("LET x = 1\nLET y = 2\nSET x = y + 1") as EmscriptApplyGuardResult.Success
        val document = success.importedDocument
        val set = document.blocks.values.last { it.type == BlockTypes.VARIABLE_SET }

        assertTrue(set.valueInputs.single { it.name.equals("VALUE", ignoreCase = true) }.connection.connectedTo != null)
        assertFalse(document.blocks.values.any { it.type == BlockTypes.VARIABLE_GET })
    }

    private fun de.visualtasker.blockeditor.domain.WorkspaceDocument.registryWithVariables() =
        CompositeBlockRegistry().apply {
            variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
}
