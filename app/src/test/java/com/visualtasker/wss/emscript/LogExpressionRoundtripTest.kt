package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.WorkspaceDryRunRuntime
import de.visualtasker.blockeditor.domain.WorkspaceGraph
import de.visualtasker.blockeditor.domain.asString
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrExpression
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.ir.IrStatement
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.SemanticPropertyCategory
import de.visualtasker.blockeditor.registry.BlockNodePresentationContract
import de.visualtasker.blockeditor.serialization.WorkspaceDecodeResult
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogExpressionRoundtripTest {
    private val importer = EmscriptWorkspaceImporter()
    private val irGenerator = IrGenerator()
    private val generator = EmscriptGenerator(irGenerator)

    @Test
    fun `log scalar literal identity survives workspace serialization and source generation`() {
        val cases = listOf(
            "log(\"42\")" to IrExpression.LiteralString("42"),
            "log(42)" to IrExpression.LiteralNumber(42.0),
            "log(\"true\")" to IrExpression.LiteralString("true"),
            "log(true)" to IrExpression.LiteralBoolean(true),
            "log(\"3.14\")" to IrExpression.LiteralString("3.14"),
            "log(3.14)" to IrExpression.LiteralNumber(3.14),
        )

        cases.forEach { (source, expectedExpression) ->
            val document = importAndSerialize(source)
            val log = irGenerator.generate(document).statements.single() as IrStatement.Log

            assertEquals(source, expectedExpression, log.value)
            assertEquals("$source;", generator.generate(document))
        }
    }

    @Test
    fun `log variable keeps variable identity separate from label`() {
        val document = importAndSerialize("LET score = 42\nlog(score)")
        val logBlock = document.blocks.values.single { it.type == BlockTypes.DEBUG_LOG }
        val reporterId = logBlock.valueInputs.single { it.name == "value" }.connection.connectedTo
            ?.let { WorkspaceGraph.findConnection(document, it)?.first }
            ?: error("log value reporter missing")
        val reporter = document.blocks.getValue(reporterId)
        val log = irGenerator.generate(document).statements.last() as IrStatement.Log

        assertEquals("score", reporter.fields.getValue("variableId").asString())
        assertEquals("score", reporter.fields.getValue("variableLabel").asString())
        assertEquals(IrExpression.GetVariable("score"), log.value)
        assertTrue(generator.generate(document).contains("log(score);"))
        val runtime = WorkspaceDryRunRuntime().run(document) as EmscriptDryRunResult.Success
        assertTrue(runtime.events.any { it.kind == "log" && it.message == "42" })
    }

    @Test
    fun `log arithmetic keeps stable operator reporter and evaluates after transport`() {
        val document = importAndSerialize("log(1 + 2)")
        val log = irGenerator.generate(document).statements.single() as IrStatement.Log
        val operation = log.value as IrExpression.Operate
        val result = WorkspaceDryRunRuntime().run(document) as EmscriptDryRunResult.Success

        assertEquals("add", operation.operator)
        assertEquals(IrExpression.LiteralNumber(1.0), operation.a)
        assertEquals(IrExpression.LiteralNumber(2.0), operation.b)
        assertEquals("log((1 + 2));", generator.generate(document))
        assertTrue(result.events.any { it.kind == "log" && it.message == "3" })
    }

    @Test
    fun `nested log arithmetic keeps expression tree`() {
        val document = importAndSerialize("log((1 + 2) * 3)")
        val outer = (irGenerator.generate(document).statements.single() as IrStatement.Log).value as IrExpression.Operate

        assertEquals("multiply", outer.operator)
        assertEquals("add", (outer.a as IrExpression.Operate).operator)
        assertEquals(IrExpression.LiteralNumber(3.0), outer.b)
    }

    @Test
    fun `workspace serialization preserves log input connection and semantic property identity`() {
        val original = importer.import("log(42)", workspaceId = "stable-log")
            .also { assertTrue(it.issues.joinToString { issue -> issue.message }, it.isSuccess) }
            .document!!
        val originalLog = original.blocks.values.single { it.type == BlockTypes.DEBUG_LOG }
        val originalInput = originalLog.valueInputs.single { it.name == "value" }.connection
        val decoded = (WorkspaceSerializer.decode(WorkspaceSerializer.serialize(original)) as WorkspaceDecodeResult.Decoded).document
        val decodedLog = decoded.blocks.getValue(originalLog.id)
        val decodedInput = decodedLog.valueInputs.single { it.name == "value" }.connection

        assertEquals(originalInput.id, decodedInput.id)
        assertEquals(originalInput.connectedTo, decodedInput.connectedTo)
        assertEquals(
            BlockNodePresentationContract.valueInputPropertyId(originalLog.id.value, "value"),
            BlockNodePresentationContract.valueInputPropertyId(decodedLog.id.value, "value"),
        )
    }

    @Test
    fun `log definition exposes one Any semantic input and no message config field`() {
        val definition = DefaultBlockRegistry.getDefinition(BlockTypes.DEBUG_LOG)!!
        val properties = BlockNodePresentationContract.semanticPropertiesForDefinition("log-1", definition)

        assertEquals(setOf("Any"), definition.valueInputs.single { it.name == "value" }.accepts)
        assertFalse(definition.fields.any { it.key == "message" })
        assertEquals(
            SemanticPropertyCategory.INPUT,
            properties.single { it.key == "value" }.category,
        )
    }

    private fun importAndSerialize(source: String) = importer.import(source, workspaceId = "log-roundtrip")
        .also { assertTrue(it.issues.joinToString { issue -> issue.message }, it.isSuccess) }
        .document!!
        .let(WorkspaceSerializer::serialize)
        .let { WorkspaceSerializer.decode(it) as WorkspaceDecodeResult.Decoded }
        .document
}
