package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeAdapterResult
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityGate
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromeTabSupportedConvergenceTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun `typed adapter result separates execution outcome value void null and failure`() {
        val positive = RuntimeAdapterResult.success(EmscriptValue.BooleanValue(true), "supported")
        val negative = RuntimeAdapterResult.success(EmscriptValue.BooleanValue(false), "unsupported")
        val void = RuntimeAdapterResult.successWithoutValue("opened")
        val nullable = RuntimeAdapterResult.success(EmscriptValue.NullValue, "absent")
        val failure = RuntimeAdapterResult.failure("TEST_FAILURE", "failed")

        assertTrue(positive.success)
        assertEquals(EmscriptValue.BooleanValue(true), positive.value)
        assertTrue(negative.success)
        assertEquals(EmscriptValue.BooleanValue(false), negative.value)
        assertTrue(void.success)
        assertNull(void.value)
        assertTrue(nullable.success)
        assertEquals(EmscriptValue.NullValue, nullable.value)
        assertFalse(failure.success)
        assertNull(failure.value)
        assertEquals("TEST_FAILURE", failure.diagnosticCode)
        assertEquals("unsupported", negative.message)
    }

    @Test
    fun `catalog workspace IR and roundtrip expose one generic Bool reporter`() {
        val entry = VisualTaskerCommandCatalog.findById("chromeTab.isSupported")!!
        val definition = DefaultBlockRegistry.getDefinition(entry.block!!.blockType)!!

        assertEquals("chromeTab.isSupported", entry.canonicalName)
        assertEquals(listOf("ChromeTab.isSupported"), entry.acceptedAliases)
        assertEquals(CommandCatalogKind.REPORTER, entry.kind)
        assertEquals("Bool", entry.returnType)
        assertTrue(entry.arguments.isEmpty())
        assertTrue(definition.isReporter)
        assertEquals("Bool", definition.outputType)
        assertFalse(definition.hasPrevious)
        assertFalse(definition.hasNext)

        val source = """
            LET supported:Bool = chromeTab.isSupported()
            SET supported = ChromeTab.isSupported()
            LET optionalSupported:Bool? = chromeTab.isSupported()
            IF chromeTab.isSupported()
              log("supported")
            ELSE
              log("unsupported")
            END IF
        """.trimIndent()
        val preview = guard.preview(source) as EmscriptApplyGuardResult.Success
        val document = preview.importedDocument
        val registry = CompositeBlockRegistry().apply {
            document.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val ir = IrGenerator(registry).generate(document)
        val calls = mutableListOf<IrExpression.CommandCall>()
        ir.statements.forEach { statement ->
            when (statement) {
                is IrStatement.SetVariable -> (statement.expression as? IrExpression.CommandCall)?.let(calls::add)
                is IrStatement.If -> (statement.condition as? IrExpression.CommandCall)?.let(calls::add)
                else -> Unit
            }
        }
        assertTrue(calls.isNotEmpty())
        assertTrue(calls.all { it.commandId == "chromeTab.isSupported" })
        assertTrue(calls.all { it.returnType == "Bool" && it.arguments.isEmpty() })

        val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)
        assertTrue(generated, generated.contains("chromeTab.isSupported()"))
        assertFalse(generated, generated.contains("ChromeTab.isSupported()"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)
    }

    @Test
    fun `Bool assignments accept Bool nullable and reject String and Number`() {
        assertTrue(guard.preview("LET supported:Bool = chromeTab.isSupported()") is EmscriptApplyGuardResult.Success)
        assertTrue(guard.preview("LET supported:Bool? = chromeTab.isSupported()") is EmscriptApplyGuardResult.Success)
        assertTrue(WorkspaceValueTypeSystem.isCompatible("Bool", setOf("Bool?")))
        listOf("String", "Number").forEach { target ->
            assertTrue(
                target,
                guard.preview("LET supported:$target = chromeTab.isSupported()") is EmscriptApplyGuardResult.Failure,
            )
        }
    }

    @Test
    fun `runtime transports true and false as successful Bool values and branches normally`() = runBlocking {
        listOf(true, false).forEach { supported ->
            val logs = mutableListOf<String>()
            val result = runtime(
                adapterResult = RuntimeAdapterResult.success(
                    EmscriptValue.BooleanValue(supported),
                    "human message deliberately not parsed",
                ),
                logs = logs,
            ).run(runtimeDocument())

            assertTrue("$supported -> $result", result is EmscriptDryRunResult.Success)
            result as EmscriptDryRunResult.Success
            assertEquals(EmscriptValue.BooleanValue(supported), result.variables["supported"])
            assertTrue(logs.contains(if (supported) "branch:true" else "branch:false"))
        }
    }

    @Test
    fun `adapter and resolution failures remain structured failures in IF`() = runBlocking {
        listOf(
            RuntimeQueryDiagnosticCodes.CHROME_TAB_ADAPTER_UNAVAILABLE,
            RuntimeQueryDiagnosticCodes.CHROME_TAB_RESOLUTION_FAILED,
        ).forEach { code ->
            val result = runtime(
                adapterResult = RuntimeAdapterResult.failure(code, "test failure"),
            ).run(runtimeDocument())

            assertTrue("$code -> $result", result is EmscriptDryRunResult.Failure)
            result as EmscriptDryRunResult.Failure
            assertTrue(result.events.any { it.kind == "runtime_failure" && it.diagnosticCode == code })
        }
    }

    @Test
    fun `old void adapter callers remain source and binary compatible`() = runBlocking {
        val imported = guard.preview("chromeTab.open(\"https://example.com\")") as EmscriptApplyGuardResult.Success
        val calls = mutableListOf<String>()
        val runtime = WorkspaceBasicRuntime(
            capabilityGate = ::deviceAdapterGate,
            environment = environment(
                logs = calls,
                chromeTabCommand = { command, _ ->
                    calls += command
                    RuntimeAdapterResult(true, "opened", warning = false)
                },
            ),
        )

        val result = runtime.run(imported.importedDocument)

        assertTrue(result is EmscriptDryRunResult.Success)
        assertTrue(calls.any { it == "chrometab.open" })
    }

    @Test
    fun `chrome tab support remains outside D query return after later migrations`() {
        assertEquals(setOf("chromeTab.isSupported"), QueryReturnContractAudit.MIGRATED_M1B_3R)
        assertEquals(5, QueryReturnContractAudit.ALL.size)
        assertTrue(QueryReturnContractAudit.ALL.none { it.stableId == "chromeTab.isSupported" })
        assertTrue(QueryReturnContractAudit.MIGRATED_M1B_3T.containsAll(
            listOf("tasker.isInstalled", "shizuku.isInstalled", "termux.isInstalled"),
        ))
    }

    private fun runtimeDocument() = (guard.preview(
        """
        LET supported:Bool = chromeTab.isSupported()
        SET supported = chromeTab.isSupported()
        IF chromeTab.isSupported()
          log("branch:true")
        ELSE
          log("branch:false")
        END IF
        """.trimIndent(),
    ) as EmscriptApplyGuardResult.Success).importedDocument

    private fun runtime(
        adapterResult: RuntimeAdapterResult,
        logs: MutableList<String> = mutableListOf(),
    ) = WorkspaceBasicRuntime(
        capabilityGate = ::deviceAdapterGate,
        environment = environment(
            logs = logs,
            chromeTabCommand = { _, _ -> adapterResult },
        ),
    )

    private fun environment(
        logs: MutableList<String>,
        chromeTabCommand: (String, List<String>) -> RuntimeAdapterResult,
    ) = WorkspaceBasicRuntimeEnvironment(
        delayMs = {},
        playBeep = { _, _, _ -> },
        vibrate = {},
        log = { logs += it },
        chromeTabCommand = chromeTabCommand,
    )

    private fun deviceAdapterGate() = RuntimeCapabilityGate.withDeviceAdapters(
        accessibilityAvailable = false,
        customChromeTabAvailable = true,
        shizukuAvailable = false,
        termuxAvailable = false,
        taskerAvailable = false,
        usbAdbBridgeAvailable = false,
    )
}
