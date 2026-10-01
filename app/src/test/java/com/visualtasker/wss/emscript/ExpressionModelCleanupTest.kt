package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrExpression
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.ir.IrStatement
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionModelCleanupTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun variableReferenceRoundtripUsesDynamicReporterAndPreservesStableIdentity() {
        val success = guard.preview("LET stableId = 42\nlog(stableId)") as EmscriptApplyGuardResult.Success
        val original = success.importedDocument
        val renamedVariable = original.variables.variables.getValue("stableId").copy(name = "Visible label")
        val renamed = original.copy(
            variables = original.variables.copy(variables = original.variables.variables + ("stableId" to renamedVariable)),
            blocks = original.blocks.mapValues { (_, block) ->
                if (block.type.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX)) {
                    block.copy(fields = block.fields + ("variableLabel" to FieldValue.Text("Visible label")))
                } else block
            },
        )
        val registry = CompositeBlockRegistry().apply {
            renamed.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val ir = IrGenerator(registry).generate(renamed)
        val log = ir.statements.last() as IrStatement.Log
        val source = EmscriptGenerator(IrGenerator(registry)).generate(renamed)

        assertEquals(IrExpression.GetVariable("stableId"), log.value)
        assertTrue(renamed.blocks.values.any { it.type == "variable.reporter.stableId" })
        assertFalse(renamed.blocks.values.any { it.type == BlockTypes.VARIABLE_GET })
        assertTrue(source.contains("log(stableId);"))
        assertFalse(source.contains("variable.get"))
        assertFalse(source.contains("Visible label"))
    }

    @Test
    fun boolAndStringIdentityRemainDistinctAcrossRoundtrip() {
        val success = guard.preview("log(true)\nlog(false)\nlog(\"true\")\nlog(\"false\")") as EmscriptApplyGuardResult.Success
        val registry = CompositeBlockRegistry()
        val statements = IrGenerator(registry).generate(success.importedDocument).statements.map { it as IrStatement.Log }
        val source = EmscriptGenerator(IrGenerator(registry)).generate(success.importedDocument)

        assertEquals(IrExpression.LiteralBoolean(true), statements[0].value)
        assertEquals(IrExpression.LiteralBoolean(false), statements[1].value)
        assertEquals(IrExpression.LiteralString("true"), statements[2].value)
        assertEquals(IrExpression.LiteralString("false"), statements[3].value)
        assertTrue(success.importedDocument.blocks.values.filter { it.type == BlockTypes.LITERAL_BOOLEAN }.size == 2)
        assertTrue(source.contains("log(true);"))
        assertTrue(source.contains("log(false);"))
        assertTrue(source.contains("log(\"true\");"))
        assertFalse(source.contains("boolean("))
    }

    @Test
    fun commandShapedExpressionAliasesAreNotAcceptedAsSourceFunctions() {
        listOf("log(boolean(true))", "log(get(x))", "variable.get(x)").forEach { source ->
            assertTrue("$source must fail", guard.preview(source) is EmscriptApplyGuardResult.Failure)
        }
    }
}
