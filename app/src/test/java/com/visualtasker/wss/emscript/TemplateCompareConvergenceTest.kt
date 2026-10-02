package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptRuntimeDiagnosticException
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntimeEnvironment
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrExpression
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.ir.IrStatement
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CommandCatalogKind
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.QueryReturnContractAudit
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import de.visualtasker.blockeditor.registry.WorkspaceValueTypeSystem
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateCompareConvergenceTest {
    private val guard = EmscriptApplyGuard()
    private val explicitSource = """
        LET score:Number = templateCompare("button", region(10, 20, 30, 40), "mask")
        SET score = templateCompare("button", region(10, 20, 30, 40), "grayscale")
        LET accepted:Bool = templateCompare("button", region(10, 20, 30, 40)) >= 0.8
        LET optionalScore:Number? = templateCompare("button", region(10, 20, 30, 40))
    """.trimIndent()

    @Test
    fun `catalog workspace and connector expose one non null Number reporter`() {
        val entry = VisualTaskerCommandCatalog.findById("vision.templateCompare")!!
        val definition = DefaultBlockRegistry.getDefinition(entry.block!!.blockType)!!

        assertEquals(CommandCatalogKind.REPORTER, entry.kind)
        assertEquals("Number", entry.returnType)
        assertEquals(listOf("name", "region", "processing"), entry.arguments.map { it.name })
        assertEquals("grayscale", entry.arguments.last().defaultValue)
        assertEquals("Number", definition.outputType)
        assertTrue(definition.isReporter)
        assertFalse(definition.hasPrevious)
        assertFalse(definition.hasNext)
        assertEquals(setOf("String"), definition.valueInputs.single { it.name == "name" }.accepts)
        assertEquals(setOf("Region"), definition.valueInputs.single { it.name == "region" }.accepts)
        assertEquals(setOf("String"), definition.valueInputs.single { it.name == "processing" }.accepts)
        assertTrue(WorkspaceValueTypeSystem.isCompatible("Number", setOf("Number")))
        assertTrue(WorkspaceValueTypeSystem.isCompatible("Number", setOf("Number?")))
        assertTrue(WorkspaceValueTypeSystem.isCompatible("Number", setOf("Any")))
        assertFalse(WorkspaceValueTypeSystem.isCompatible("Number", setOf("String")))
        assertFalse(WorkspaceValueTypeSystem.isCompatible("Number", setOf("Boolean")))
    }

    @Test
    fun `source workspace IR serializer and identity roundtrip preserve template compare`() {
        val preview = guard.preview(explicitSource) as EmscriptApplyGuardResult.Success
        val document = preview.importedDocument
        val reporterType = BlockTypes.EMSCRIPT_COMMAND_PREFIX + "vision.templateCompare"
        val reporters = document.blocks.values.filter { it.type == reporterType }
        assertEquals(4, reporters.size)
        assertTrue(reporters.all { it.output?.provides == "Number" })

        val registry = CompositeBlockRegistry().apply {
            document.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val ir = IrGenerator(registry).generate(document)
        val calls = buildList {
            ir.statements.filterIsInstance<IrStatement.SetVariable>().forEach { statement ->
                when (val expression = statement.expression) {
                    is IrExpression.CommandCall -> add(expression)
                    is IrExpression.Compare -> (expression.left as? IrExpression.CommandCall)?.let(::add)
                    else -> Unit
                }
            }
        }
        assertEquals(4, calls.size)
        assertTrue(calls.all { it.commandId == "vision.templateCompare" && it.returnType == "Number" })
        assertTrue(calls.all { it.arguments[1] is IrExpression.CommandCall })
        assertTrue(calls.all { (it.arguments[1] as IrExpression.CommandCall).returnType == "Region" })

        val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)
        assertTrue(generated, generated.contains("templateCompare(\"button\", region(10, 20, 30, 40), \"mask\")"))
        assertTrue(generated, generated.contains("templateCompare(\"button\", region(10, 20, 30, 40))"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)

        val decoded = WorkspaceSerializer.deserialize(WorkspaceSerializer.serialize(document))
        assertEquals(document.blocks.keys, decoded.blocks.keys)
        assertEquals(
            reporters.map { it.id }.toSet(),
            decoded.blocks.values.filter { it.type == reporterType }.map { it.id }.toSet(),
        )
    }

    @Test
    fun `typed assignments and numeric composition accept only compatible targets`() {
        assertTrue(guard.preview(explicitSource) is EmscriptApplyGuardResult.Success)
        listOf(
            "LET score:String = templateCompare(\"button\", region(1, 2, 3, 4))",
            "LET match:Bool = templateCompare(\"button\", region(1, 2, 3, 4))",
        ).forEach { source ->
            assertTrue("$source must fail", guard.preview(source) is EmscriptApplyGuardResult.Failure)
        }
    }

    @Test
    fun `runtime transports low and boundary scores as Number values`() = runBlocking {
        listOf(0f, 0.1f, 0.49f, 1f).forEach { score ->
            val result = runtime(score = score).run(singleScoreDocument())
            assertTrue("$score -> $result", result is EmscriptDryRunResult.Success)
            result as EmscriptDryRunResult.Success
            assertEquals(EmscriptValue.NumberValue(score.toDouble()), result.variables["score"])
            assertTrue(result.events.any { it.message.contains("templateCompare =") })
        }
    }

    @Test
    fun `runtime maps every non value state to a structured failure and never NullValue`() = runBlocking {
        listOf(
            RuntimeQueryDiagnosticCodes.TEMPLATE_NOT_FOUND,
            RuntimeQueryDiagnosticCodes.TEMPLATE_IMAGE_UNAVAILABLE,
            RuntimeQueryDiagnosticCodes.TEMPLATE_REGION_UNAVAILABLE,
            RuntimeQueryDiagnosticCodes.TEMPLATE_COMPARE_FAILED,
        ).forEach { code ->
            val result = failingRuntime(code).run(singleScoreDocument())
            assertTrue("$code -> $result", result is EmscriptDryRunResult.Failure)
            result as EmscriptDryRunResult.Failure
            assertTrue(result.events.any { it.kind == "runtime_failure" && it.diagnosticCode == code })
        }

        val invalidRegion = (guard.preview(
            "LET score:Number = templateCompare(\"button\", region(1, 2, 0, 4))",
        ) as EmscriptApplyGuardResult.Success).importedDocument
        val result = runtime(score = 0.5f).run(invalidRegion)
        assertTrue(result is EmscriptDryRunResult.Failure)
        assertTrue((result as EmscriptDryRunResult.Failure).events.any {
            it.diagnosticCode == RuntimeQueryDiagnosticCodes.TEMPLATE_REGION_UNAVAILABLE
        })
    }

    @Test
    fun `invalid adapter scores fail instead of clamping or returning a sentinel`() = runBlocking {
        listOf(Float.NaN, -0.01f, 1.01f).forEach { score ->
            val result = runtime(score = score).run(singleScoreDocument())
            assertTrue("$score -> $result", result is EmscriptDryRunResult.Failure)
            assertTrue((result as EmscriptDryRunResult.Failure).events.any {
                it.diagnosticCode == RuntimeQueryDiagnosticCodes.TEMPLATE_COMPARE_FAILED
            })
        }
    }

    @Test
    fun `query audit removes only template compare from D query return`() {
        assertEquals(setOf("vision.templateCompare"), QueryReturnContractAudit.MIGRATED_M1B_3P)
        assertEquals(6, QueryReturnContractAudit.ALL.size)
        assertTrue(QueryReturnContractAudit.ALL.none { it.stableId == "vision.templateCompare" })
        assertTrue(QueryReturnContractAudit.ALL.any { it.stableId == "action.findTemplate" })
    }

    private fun singleScoreDocument() = (guard.preview(
        "LET score:Number = templateCompare(\"button\", region(10, 20, 30, 40))",
    ) as EmscriptApplyGuardResult.Success).importedDocument

    private fun runtime(score: Float) = WorkspaceBasicRuntime(
        environment = WorkspaceBasicRuntimeEnvironment(
            delayMs = {},
            playBeep = { _, _, _ -> },
            vibrate = {},
            log = {},
            templateCompare = { _, _, _ -> score },
        ),
    )

    private fun failingRuntime(code: String) = WorkspaceBasicRuntime(
        environment = WorkspaceBasicRuntimeEnvironment(
            delayMs = {},
            playBeep = { _, _, _ -> },
            vibrate = {},
            log = {},
            templateCompare = { _, _, _ -> throw EmscriptRuntimeDiagnosticException(code, "test") },
        ),
    )
}
