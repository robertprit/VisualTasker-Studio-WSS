package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardStage
import de.visualtasker.workflow.core.WorkspaceGraph
import de.visualtasker.workflow.core.asString
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrExpression
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.workflow.semantics.ir.IrStatement
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.workflow.semantics.validation.TypeMismatch
import de.visualtasker.workflow.semantics.validation.Validator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatastorePutStringContractTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun stringLiteralsAndVariableReferencesRoundtripLosslessly() {
        val scripts = listOf(
            "datastorePut(\"key\", \"value\")",
            "LET key = \"key\"\ndatastorePut(key, \"value\")",
            "LET value = \"value\"\ndatastorePut(\"key\", value)",
            "LET key = \"key\"\nLET value = \"value\"\ndatastorePut(key, value)",
        )

        scripts.forEach { source ->
            val success = guard.preview(source)
            assertTrue("$source -> $success", success is EmscriptApplyGuardResult.Success)
            success as EmscriptApplyGuardResult.Success
            val document = success.importedDocument
            val registry = document.registryWithVariables()
            val command = IrGenerator(registry).generate(document).statements.last() as IrStatement.CommandCall
            val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)
            val second = guard.preview(generated)

            assertEquals(2, command.expressionArguments.size)
            assertTrue("$generated -> $second", second is EmscriptApplyGuardResult.Success)
        }
    }

    @Test
    fun datastoreVariableReferencesKeepStableIdsAndLabels() {
        val success = guard.preview(
            "LET key = \"key\"\nLET value = \"value\"\ndatastorePut(key, value)",
        ) as EmscriptApplyGuardResult.Success
        val document = success.importedDocument
        val commandBlock = document.blocks.values.single {
            it.type.startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX) && it.fields["command"]?.asString() == "datastorePut"
        }
        val connectedReporters = commandBlock.valueInputs.associate { input ->
            val reporterId = input.connection.connectedTo
                ?.let { WorkspaceGraph.findConnection(document, it)?.first }
                ?: error("${input.name} reporter missing")
            input.name to document.blocks.getValue(reporterId)
        }
        val command = IrGenerator(document.registryWithVariables()).generate(document).statements.last() as IrStatement.CommandCall

        assertEquals("key", connectedReporters.getValue("key").fields.getValue("variableId").asString())
        assertEquals("key", connectedReporters.getValue("key").fields.getValue("variableLabel").asString())
        assertEquals("value", connectedReporters.getValue("value").fields.getValue("variableId").asString())
        assertEquals("value", connectedReporters.getValue("value").fields.getValue("variableLabel").asString())
        assertEquals(listOf(IrExpression.GetVariable("key"), IrExpression.GetVariable("value")), command.expressionArguments)
    }

    @Test
    fun numberAndBooleanArgumentsFailBeforeApplyWithGenericDiagnostic() {
        val cases = listOf(
            "datastorePut(\"key\", 42)" to ("value" to "Number"),
            "datastorePut(\"key\", 3.14)" to ("value" to "Number"),
            "datastorePut(\"key\", true)" to ("value" to "Boolean"),
            "datastorePut(42, \"value\")" to ("key" to "Number"),
            "datastorePut(true, \"value\")" to ("key" to "Boolean"),
        )

        cases.forEach { (source, expected) ->
            val result = guard.preview(source)
            assertTrue("$source -> $result", result is EmscriptApplyGuardResult.Failure)
            result as EmscriptApplyGuardResult.Failure
            assertEquals(EmscriptApplyGuardStage.PRE_VALIDATE, result.stage)
            assertEquals("$source -> $result", "EMSCRIPT_ARGUMENT_TYPE_MISMATCH", result.diagnosticCode)
            assertTrue(result.message.contains("system.datastorePut"))
            assertTrue(result.message.contains(expected.first))
            assertTrue(result.message.contains("Text"))
            assertTrue(result.message.contains(expected.second))
        }
    }

    @Test
    fun anyVariableIsNotImplicitlyAssignableToStringInput() {
        val success = guard.preview("LET value = \"value\"\ndatastorePut(\"key\", value)") as EmscriptApplyGuardResult.Success
        val original = success.importedDocument
        val anyDocument = original.copy(
            variables = original.variables.copy(
                variables = original.variables.variables + ("value" to original.variables.variables.getValue("value").copy(type = "Any")),
            ),
        )
        val validation = Validator.validate(anyDocument, anyDocument.registryWithVariables())
        val mismatch = validation.errors.filterIsInstance<TypeMismatch>().single()

        assertEquals("value", mismatch.inputName)
        assertEquals(setOf("Text"), mismatch.expected)
        assertEquals("Any", mismatch.actual)
        assertEquals("EMSCRIPT_ARGUMENT_TYPE_MISMATCH", mismatch.code)
    }

    private fun de.visualtasker.workflow.core.WorkspaceDocument.registryWithVariables() =
        CompositeBlockRegistry().apply {
            variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
}
