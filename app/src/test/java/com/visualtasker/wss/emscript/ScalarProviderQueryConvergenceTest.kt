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
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.registry.CommandCatalogKind
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.QueryReturnContractAudit
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScalarProviderQueryConvergenceTest {
    private val guard = EmscriptApplyGuard()
    private val cases = listOf(
        QueryCase("tasker.isEnabled", "Tasker.isEnabled", "Bool", ""),
        QueryCase("tasker.getVariable", "Tasker.getVariable", "String?", "\"%Name\""),
        QueryCase("shizuku.getUid", "Shizuku.getUid", "Number?", ""),
        QueryCase("termux.get", "Termux.get", "String?", "\"summary\""),
        QueryCase("scrcpy.isRunning", "Scrcpy.isRunning", "Bool", ""),
        QueryCase("scrcpy.get", "Scrcpy.get", "String?", "\"state\""),
    )

    @Test
    fun `catalog exposes six generic scalar reporters with stable IDs and aliases`() {
        cases.forEach { query ->
            val entry = VisualTaskerCommandCatalog.findById(query.id)!!
            val block = DefaultBlockRegistry.getDefinition(entry.block!!.blockType)!!
            assertEquals(query.id, entry.canonicalName)
            assertEquals(listOf(query.alias), entry.acceptedAliases)
            assertEquals(query.returnType, entry.returnType)
            assertEquals(CommandCatalogKind.REPORTER, entry.kind)
            assertTrue(block.isReporter)
            assertEquals(query.returnType, block.outputType)
            assertFalse(block.hasPrevious)
            assertFalse(block.hasNext)
        }
        assertEquals(cases.mapTo(linkedSetOf()) { it.id }, QueryReturnContractAudit.MIGRATED_M1B_3W)
        assertTrue(QueryReturnContractAudit.ALL.isEmpty())
    }

    @Test
    fun `LET SET IR and source roundtrip preserve canonical calls`() {
        cases.forEach { query ->
            val source = """
                LET value:${query.returnType} = ${query.alias}(${query.args})
                SET value = ${query.id}(${query.args})
            """.trimIndent()
            val preview = guard.preview(source) as EmscriptApplyGuardResult.Success
            val registry = CompositeBlockRegistry().apply {
                preview.importedDocument.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
            }
            val ir = IrGenerator(registry).generate(preview.importedDocument)
            val generated = EmscriptGenerator(IrGenerator(registry)).generate(preview.importedDocument)
            assertTrue(ir.statements.toString(), ir.statements.toString().contains(query.id))
            assertTrue(generated, generated.contains("${query.id}(${query.args})"))
            assertFalse(generated, generated.contains("${query.alias}(${query.args})"))
            assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)
        }
    }

    @Test
    fun `type system accepts exact and nullable Bool targets and rejects incompatible targets`() {
        cases.forEach { query ->
            assertTrue(guard.preview("LET value:${query.returnType} = ${query.id}(${query.args})") is EmscriptApplyGuardResult.Success)
            if (query.returnType == "Bool") {
                assertTrue(guard.preview("LET value:Bool? = ${query.id}(${query.args})") is EmscriptApplyGuardResult.Success)
            }
            val incompatible = when (query.returnType) {
                "Bool" -> listOf("String", "Number")
                "Number?" -> listOf("String", "Bool")
                else -> listOf("Number", "Bool")
            }
            incompatible.forEach { target ->
                assertTrue(
                    "${query.id}: $target",
                    guard.preview("LET value:$target = ${query.id}(${query.args})") is EmscriptApplyGuardResult.Failure,
                )
            }
        }
    }

    @Test
    fun `typed runtime preserves true false empty zero and absent values`() = runBlocking {
        val values = listOf(
            Triple(cases[0], EmscriptValue.BooleanValue(false), "false"),
            Triple(cases[1], EmscriptValue.StringValue(""), ""),
            Triple(cases[2], EmscriptValue.NumberValue(0.0), "0"),
            Triple(cases[3], EmscriptValue.StringValue("unavailable"), "unavailable"),
            Triple(cases[4], EmscriptValue.BooleanValue(false), "false"),
            Triple(cases[5], EmscriptValue.NullValue, "null"),
        )
        values.forEach { (query, value, _) ->
            val result = runtime(query.id, RuntimeAdapterResult.success(value)).run(document(query))
            assertTrue("${query.id}: $result", result is EmscriptDryRunResult.Success)
            result as EmscriptDryRunResult.Success
            assertEquals(value, result.variables["value"])
        }
    }

    @Test
    fun `Bool queries remain usable directly as conditions`() = runBlocking {
        listOf(cases[0], cases[4]).forEach { query ->
            listOf(true, false).forEach { state ->
                val logs = mutableListOf<String>()
                val source = """
                    IF ${query.id}(${query.args})
                      log("true")
                    ELSE
                      log("false")
                    END IF
                """.trimIndent()
                val document = (guard.preview(source) as EmscriptApplyGuardResult.Success).importedDocument
                val result = runtime(query.id, RuntimeAdapterResult.success(EmscriptValue.BooleanValue(state)), logs).run(document)
                assertTrue(result is EmscriptDryRunResult.Success)
                assertTrue(logs.contains(state.toString()))
            }
        }
    }

    @Test
    fun `adapter and query failures remain structured failures`() = runBlocking {
        val failures = mapOf(
            "tasker.isEnabled" to RuntimeQueryDiagnosticCodes.TASKER_ENABLED_CHECK_FAILED,
            "tasker.getVariable" to RuntimeQueryDiagnosticCodes.TASKER_VARIABLE_QUERY_FAILED,
            "shizuku.getUid" to RuntimeQueryDiagnosticCodes.SHIZUKU_UID_QUERY_FAILED,
            "termux.get" to RuntimeQueryDiagnosticCodes.TERMUX_STATUS_CHECK_FAILED,
            "scrcpy.isRunning" to RuntimeQueryDiagnosticCodes.SCRCPY_SESSION_CHECK_FAILED,
            "scrcpy.get" to RuntimeQueryDiagnosticCodes.SCRCPY_UNKNOWN_KEY,
        )
        cases.forEach { query ->
            val code = failures.getValue(query.id)
            val result = runtime(query.id, RuntimeAdapterResult.failure(code, "test failure")).run(document(query))
            assertTrue("${query.id}: $result", result is EmscriptDryRunResult.Failure)
            result as EmscriptDryRunResult.Failure
            assertTrue(result.events.any { it.kind == "runtime_failure" && it.diagnosticCode == code })
        }
    }

    @Test
    fun `missing provider adapters fail instead of returning fallback values`() = runBlocking {
        cases.forEach { query ->
            val result = WorkspaceBasicRuntime(
                capabilityGate = ::allProvidersReady,
                environment = baseEnvironment(),
            ).run(document(query))
            assertTrue("${query.id}: $result", result is EmscriptDryRunResult.Failure)
        }
    }

    private fun document(query: QueryCase) = (guard.preview(
        "LET value:${query.returnType} = ${query.id}(${query.args})",
    ) as EmscriptApplyGuardResult.Success).importedDocument

    private fun runtime(
        commandId: String,
        result: RuntimeAdapterResult,
        logs: MutableList<String> = mutableListOf(),
    ): WorkspaceBasicRuntime {
        val base = baseEnvironment(logs)
        val environment = when (commandId) {
            "tasker.isEnabled" -> base.copy(taskerIsEnabled = { result })
            "tasker.getVariable" -> base.copy(taskerGetVariable = { result })
            "shizuku.getUid" -> base.copy(shizukuGetUid = { result })
            "termux.get" -> base.copy(termuxGet = { result })
            "scrcpy.isRunning" -> base.copy(scrcpyIsRunning = { result })
            "scrcpy.get" -> base.copy(scrcpyGet = { result })
            else -> error("Unexpected command $commandId")
        }
        return WorkspaceBasicRuntime(capabilityGate = ::allProvidersReady, environment = environment)
    }

    private fun baseEnvironment(logs: MutableList<String> = mutableListOf()) = WorkspaceBasicRuntimeEnvironment(
        delayMs = {},
        playBeep = { _, _, _ -> },
        vibrate = {},
        log = { logs += it },
    )

    private fun allProvidersReady() = RuntimeCapabilityGate.withDeviceAdapters(
        accessibilityAvailable = false,
        customChromeTabAvailable = false,
        shizukuAvailable = true,
        termuxAvailable = true,
        taskerAvailable = true,
        usbAdbBridgeAvailable = true,
    )

    private data class QueryCase(
        val id: String,
        val alias: String,
        val returnType: String,
        val args: String,
    )
}
