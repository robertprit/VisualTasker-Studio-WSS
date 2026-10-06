package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardStage
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntimeEnvironment
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrExpression
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.workflow.semantics.ir.IrStatement
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.workflow.semantics.VisualTaskerCommandCatalog
import de.visualtasker.workflow.semantics.WorkspaceValueTypeSystem
import de.visualtasker.workflow.serialization.WorkflowSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowRiskQueryReturnConvergenceTest {
    private val guard = EmscriptApplyGuard()
    private val source = """
        LET clipboardValue = Clipboard.get()
        LET systemValue = Sys.info()
        LET envValue = Env.get("PACKAGE_NAME")
        SET clipboardValue = Env.get("DEVICE_MODEL")
        log(Clipboard.get())
    """.trimIndent()

    @Test
    fun `three approved queries are String reporters with generic snap compatibility`() {
        val ids = setOf("clipboard.get", "system.info", "system.env")

        ids.forEach { id ->
            val entry = VisualTaskerCommandCatalog.findById(id)!!
            val definition = de.visualtasker.blockeditor.registry.DefaultBlockRegistry
                .getDefinition(entry.block!!.blockType)!!
            assertEquals("String", entry.returnType)
            assertEquals("String", definition.outputType)
            assertTrue(definition.isReporter)
            assertTrue(WorkspaceValueTypeSystem.isCompatible("String", setOf("String")))
            assertTrue(WorkspaceValueTypeSystem.isCompatible("String", setOf("Any")))
            assertFalse(WorkspaceValueTypeSystem.isCompatible("String", setOf("Number")))
            assertFalse(WorkspaceValueTypeSystem.isCompatible("String", setOf("Boolean")))
        }
    }

    @Test
    fun `LET SET and nested query expressions survive source workspace IR and serialization`() {
        val first = guard.preview(source)
        assertTrue(first.toString(), first is EmscriptApplyGuardResult.Success)
        first as EmscriptApplyGuardResult.Success
        val document = first.importedDocument
        val queryBlocks = document.blocks.values.filter {
            it.type.startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX) &&
                it.metadata[VisualTaskerCommandCatalog.METADATA_COMMAND_ID] in
                setOf("clipboard.get", "system.info", "system.env")
        }
        assertEquals(5, queryBlocks.size)
        assertEquals(
            setOf("clipboard.get", "system.info", "system.env"),
            queryBlocks.mapNotNull { it.metadata[VisualTaskerCommandCatalog.METADATA_COMMAND_ID] }.toSet(),
        )

        val registry = CompositeBlockRegistry().apply {
            document.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val ir = IrGenerator(registry).generate(document)
        val queryExpressions = buildList {
            ir.statements.filterIsInstance<IrStatement.SetVariable>().mapNotNullTo(this) { it.expression as? IrExpression.CommandCall }
            ir.statements.filterIsInstance<IrStatement.Log>().mapNotNullTo(this) { it.value as? IrExpression.CommandCall }
        }
        assertEquals(5, queryExpressions.size)
        assertTrue(queryExpressions.all { it.returnType == "String" })
        assertEquals(
            setOf("clipboard.get", "system.info", "system.env"),
            queryExpressions.map { it.commandId }.toSet(),
        )

        val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)
        assertTrue(generated.contains("let clipboardValue = Clipboard.get();"))
        assertTrue(generated.contains("set clipboardValue = Env.get(\"DEVICE_MODEL\");"))
        assertTrue(generated.contains("log(Clipboard.get());"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)

        val decoded = WorkflowSerializer.deserialize(WorkflowSerializer.serialize(document))
        assertEquals(document.blocks.keys, decoded.blocks.keys)
        assertEquals(
            queryBlocks.associate { it.id to it.metadata[VisualTaskerCommandCatalog.METADATA_COMMAND_ID] },
            decoded.blocks.values
                .filter { it.id in queryBlocks.map { block -> block.id } }
                .associate { it.id to it.metadata[VisualTaskerCommandCatalog.METADATA_COMMAND_ID] },
        )
    }

    @Test
    fun `query assignment type mismatches and Void expressions are rejected`() {
        listOf(
            "LET numberValue = 1\nSET numberValue = Clipboard.get()",
            "LET boolValue = true\nSET boolValue = Sys.info()",
        ).forEach { invalid ->
            val result = guard.preview(invalid)
            assertTrue("$invalid -> $result", result is EmscriptApplyGuardResult.Failure)
            result as EmscriptApplyGuardResult.Failure
            assertEquals(EmscriptApplyGuardStage.PRE_VALIDATE, result.stage)
            assertEquals("EMSCRIPT_ASSIGNMENT_TYPE_MISMATCH", result.diagnosticCode)
        }

        val parsed = EmscriptParserSlice().parse("LET value = wait(1)")
        assertFalse(parsed.isSuccess)
        assertTrue(parsed.issues.any { it.message.contains("kein Ausdruck") })
    }

    @Test
    fun `basic runtime transports existing non null query values into expressions`() = runBlocking {
        val imported = EmscriptWorkspaceImporter().import(source, workspaceId = "query-runtime")
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val logs = mutableListOf<String>()
        val runtime = WorkspaceBasicRuntime(
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = {},
                playBeep = { _, _, _ -> },
                vibrate = {},
                log = logs::add,
                clipboardGet = { "clipboard-value" },
                systemInfo = { "system-info" },
                envGet = { name -> "env:$name" },
            ),
        )

        val result = runtime.run(imported.document!!)

        assertTrue(result.toString(), result is EmscriptDryRunResult.Success)
        result as EmscriptDryRunResult.Success
        assertEquals(EmscriptValue.StringValue("env:DEVICE_MODEL"), result.variables["clipboardValue"])
        assertEquals(EmscriptValue.StringValue("system-info"), result.variables["systemValue"])
        assertEquals(EmscriptValue.StringValue("env:PACKAGE_NAME"), result.variables["envValue"])
        assertTrue(logs.contains("clipboard-value"))
    }
}
