package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.parser.EmscriptIrStatement
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.WorkspaceDryRunRuntime
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockRegistry
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.workflow.semantics.validation.TypeMismatch
import de.visualtasker.workflow.semantics.validation.Validator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrateSignatureConvergenceTest {
    private val parser = EmscriptParserSlice()
    private val guard = EmscriptApplyGuard()

    @Test
    fun parserAcceptsOneThroughFourPatternExpressionsWithoutFixedMaximum() {
        listOf(
            "vibrate(200)" to 1,
            "vibrate(100, 200)" to 2,
            "vibrate(100, 200, 100)" to 3,
            "vibrate(100, 200, 100, 400)" to 4,
            "vibrate(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)" to 17,
        ).forEach { (source, expectedCount) ->
            val result = parser.parse(source)

            assertTrue(result.issues.toString(), result.isSuccess)
            val call = result.ir!!.statements.single() as EmscriptIrStatement.CommandCall
            assertEquals("vibrate", call.command)
            assertEquals(expectedCount, call.expressionArguments.size)
        }
    }

    @Test
    fun oneAndFourValuePatternsRoundtripSemantically() {
        listOf(
            "vibrate(200)" to 1,
            "vibrate(100, 200, 100, 400)" to 4,
        ).forEach { (source, expectedCount) ->
            val success = guard.preview(source) as EmscriptApplyGuardResult.Success
            val generated = EmscriptGenerator(IrGenerator(success.importedDocument.registryWithVariables()))
                .generate(success.importedDocument)
            val reparsed = parser.parse(generated)

            assertTrue(generated, reparsed.isSuccess)
            val call = reparsed.ir!!.statements.single() as EmscriptIrStatement.CommandCall
            assertEquals(expectedCount, call.expressionArguments.size)
        }
    }

    @Test
    fun emptyStringAndBooleanPatternsAreRejectedWithoutImplicitConversion() {
        val empty = parser.parse("vibrate()")
        assertFalse(empty.isSuccess)

        listOf("vibrate(\"200\")", "vibrate(true)").forEach { source ->
            val result = guard.preview(source)

            assertTrue("$source -> $result", result is EmscriptApplyGuardResult.Failure)
            result as EmscriptApplyGuardResult.Failure
            assertEquals(com.visualtasker.wss.emscript.apply.EmscriptApplyGuardStage.PRE_VALIDATE, result.stage)
            assertEquals("EMSCRIPT_ARGUMENT_TYPE_MISMATCH", result.diagnosticCode)
        }
    }

    @Test
    fun literalsVariablesAndArithmeticRoundtripLosslessly() {
        val source = """
            LET pause = 100
            LET duration = 200
            vibrate(pause, duration, 100 + 100, 400)
        """.trimIndent()
        val success = guard.preview(source) as EmscriptApplyGuardResult.Success
        val document = success.importedDocument
        val generated = EmscriptGenerator(IrGenerator(document.registryWithVariables())).generate(document)
        val vibrate = document.blocks.values.single { it.type == BlockTypes.FEEDBACK_VIBRATE }

        assertEquals(listOf("patternMs", "patternMs_2", "patternMs_3", "patternMs_4"), vibrate.valueInputs.map { it.name })
        assertTrue(generated, generated.contains("vibrate(pause, duration, (100 + 100), 400);"))

        val reparsed = parser.parse(generated)
        assertTrue(reparsed.issues.toString(), reparsed.isSuccess)
        val call = reparsed.ir!!.statements.last() as EmscriptIrStatement.CommandCall
        assertEquals(4, call.expressionArguments.size)
    }

    @Test
    fun anyVariableIsNotImplicitlyAssignableToPatternNumber() {
        val success = guard.preview("LET duration = 200\nvibrate(duration)") as EmscriptApplyGuardResult.Success
        val original = success.importedDocument
        val anyDocument = original.copy(
            variables = original.variables.copy(
                variables = original.variables.variables +
                    ("duration" to original.variables.variables.getValue("duration").copy(type = "Any")),
            ),
        )

        val validation = Validator.validate(anyDocument, anyDocument.registryWithVariables())
        val mismatch = validation.errors.filterIsInstance<TypeMismatch>().single()

        assertEquals("patternMs", mismatch.inputName)
        assertEquals(setOf("Number"), mismatch.expected)
        assertEquals("Any", mismatch.actual)
    }

    @Test
    fun dryRunEvaluatesVariableAndArithmeticPatternArgumentsInOrder() {
        val source = """
            LET pause = 100
            LET duration = 200
            vibrate(pause, duration, 100 + 100, 400)
        """.trimIndent()
        val success = guard.preview(source) as EmscriptApplyGuardResult.Success
        val result = WorkspaceDryRunRuntime().run(success.importedDocument) as EmscriptDryRunResult.Success
        val vibrate = result.events.single { it.kind == "vibrate" }

        assertEquals(listOf(100L, 200L, 200L, 400L), vibrate.numericArguments)
    }

    @Test
    fun zeroAndNegativeValuesRemainExplicitUntilAndroidDispatchSanitization() {
        val success = guard.preview("vibrate(100, 0, -20, 200)") as EmscriptApplyGuardResult.Success
        val document = success.importedDocument
        val generated = EmscriptGenerator(IrGenerator(document.registryWithVariables())).generate(document)
        val result = WorkspaceDryRunRuntime().run(document) as EmscriptDryRunResult.Success

        assertTrue(generated, generated.contains("vibrate(100, 0, (0 - 20), 200);"))
        assertTrue(parser.parse(generated).issues.toString(), parser.parse(generated).isSuccess)
        assertEquals(listOf(100L, 0L, -20L, 200L), result.events.single { it.kind == "vibrate" }.numericArguments)
    }

    @Test
    fun explicitLegacyFieldValueStillSerializesAsSinglePatternValue() {
        val success = guard.preview("vibrate(80)") as EmscriptApplyGuardResult.Success
        val original = success.importedDocument
        val vibrate = original.blocks.values.single { it.type == BlockTypes.FEEDBACK_VIBRATE }
        val legacy = original.copy(
            blocks = original.blocks + (
                vibrate.id to vibrate.copy(
                    fields = vibrate.fields + ("pattern" to FieldValue.Text("80")),
                    valueInputs = emptyList(),
                )
            ),
        )

        val generated = EmscriptGenerator(IrGenerator(legacy.registryWithVariables())).generate(legacy)

        assertTrue(generated, generated.contains("vibrate(80);"))
    }

    private fun de.visualtasker.workflow.core.WorkspaceDocument.registryWithVariables(): BlockRegistry =
        CompositeBlockRegistry().apply {
            variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
}
