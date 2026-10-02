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

class ShizukuAvailabilityConvergenceTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun `catalog exposes a generic zero argument Bool reporter with legacy alias`() {
        val entry = VisualTaskerCommandCatalog.findById(COMMAND_ID)!!
        val definition = DefaultBlockRegistry.getDefinition(entry.block!!.blockType)!!

        assertEquals(COMMAND_ID, entry.canonicalName)
        assertEquals(listOf(LEGACY_ALIAS), entry.acceptedAliases)
        assertTrue(entry.arguments.isEmpty())
        assertEquals(CommandCatalogKind.REPORTER, entry.kind)
        assertEquals("Bool", entry.returnType)
        assertTrue(definition.isReporter)
        assertEquals("Bool", definition.outputType)
        assertFalse(definition.hasPrevious)
        assertFalse(definition.hasNext)
    }

    @Test
    fun `LET SET IF legacy import and canonical roundtrip remain lossless`() {
        val source = """
            LET available:Bool = $LEGACY_ALIAS()
            SET available = $COMMAND_ID()
            LET optionalAvailable:Bool? = $COMMAND_ID()
            IF $COMMAND_ID()
              log("available")
            ELSE
              log("unavailable")
            END IF
        """.trimIndent()
        val preview = guard.preview(source) as EmscriptApplyGuardResult.Success
        val registry = CompositeBlockRegistry().apply {
            preview.importedDocument.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val ir = IrGenerator(registry).generate(preview.importedDocument)
        val generated = EmscriptGenerator(IrGenerator(registry)).generate(preview.importedDocument)

        assertTrue(ir.statements.toString().contains(COMMAND_ID))
        assertTrue(generated.contains("$COMMAND_ID()"))
        assertFalse(generated.contains("$LEGACY_ALIAS()"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)
    }

    @Test
    fun `Bool nullable succeeds while String and Number targets are rejected`() {
        assertTrue(guard.preview("LET available:Bool = $COMMAND_ID()") is EmscriptApplyGuardResult.Success)
        assertTrue(guard.preview("LET available:Bool? = $COMMAND_ID()") is EmscriptApplyGuardResult.Success)
        assertTrue(guard.preview("LET available:String = $COMMAND_ID()") is EmscriptApplyGuardResult.Failure)
        assertTrue(guard.preview("LET available:Number = $COMMAND_ID()") is EmscriptApplyGuardResult.Failure)
    }

    @Test
    fun `successful true and false values take normal IF branches`() = runBlocking {
        listOf(true, false).forEach { available ->
            val logs = mutableListOf<String>()
            val result = runtime(
                RuntimeAdapterResult.success(EmscriptValue.BooleanValue(available)),
                logs,
            ).run(runtimeDocument())

            assertTrue(result is EmscriptDryRunResult.Success)
            result as EmscriptDryRunResult.Success
            assertEquals(EmscriptValue.BooleanValue(available), result.variables["available"])
            assertTrue(logs.contains(if (available) "branch:true" else "branch:false"))
        }
    }

    @Test
    fun `adapter and inspection failures remain failures rather than false`() = runBlocking {
        listOf(
            RuntimeQueryDiagnosticCodes.SHIZUKU_ADAPTER_UNAVAILABLE,
            RuntimeQueryDiagnosticCodes.SHIZUKU_INSTALLATION_CHECK_FAILED,
            RuntimeQueryDiagnosticCodes.SHIZUKU_PERMISSION_CHECK_FAILED,
            RuntimeQueryDiagnosticCodes.SHIZUKU_BINDER_CHECK_FAILED,
        ).forEach { code ->
            val result = runtime(RuntimeAdapterResult.failure(code, "test failure")).run(runtimeDocument())

            assertTrue("$code -> $result", result is EmscriptDryRunResult.Failure)
            result as EmscriptDryRunResult.Failure
            assertTrue(result.events.any { it.kind == "runtime_failure" && it.diagnosticCode == code })
        }
    }

    @Test
    fun `audit removes only Shizuku availability in 3U`() {
        assertEquals(setOf(COMMAND_ID), QueryReturnContractAudit.MIGRATED_M1B_3U)
        assertEquals(5, QueryReturnContractAudit.ALL.size)
        assertTrue(QueryReturnContractAudit.ALL.none { it.stableId == COMMAND_ID })
        assertTrue(QueryReturnContractAudit.MIGRATED_M1B_3W.contains("shizuku.getUid"))
    }

    private fun runtimeDocument() = (guard.preview(
        """
        LET available:Bool = $COMMAND_ID()
        SET available = $COMMAND_ID()
        IF $COMMAND_ID()
          log("branch:true")
        ELSE
          log("branch:false")
        END IF
        """.trimIndent(),
    ) as EmscriptApplyGuardResult.Success).importedDocument

    private fun runtime(
        result: RuntimeAdapterResult,
        logs: MutableList<String> = mutableListOf(),
    ) = WorkspaceBasicRuntime(
        capabilityGate = {
            RuntimeCapabilityGate.withDeviceAdapters(
                accessibilityAvailable = false,
                customChromeTabAvailable = false,
                shizukuAvailable = true,
                termuxAvailable = false,
                taskerAvailable = false,
                usbAdbBridgeAvailable = false,
            )
        },
        environment = WorkspaceBasicRuntimeEnvironment(
            delayMs = {},
            playBeep = { _, _, _ -> },
            vibrate = {},
            log = { logs += it },
            shizukuAvailable = { result },
        ),
    )

    private companion object {
        const val COMMAND_ID = "shizuku.isAvailable"
        const val LEGACY_ALIAS = "Shizuku.isAvailable"
    }
}
