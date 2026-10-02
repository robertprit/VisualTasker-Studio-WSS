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

class InstalledProviderBoolConvergenceTest {
    private val guard = EmscriptApplyGuard()
    private val commands = listOf(
        ProviderCase(
            id = "tasker.isInstalled",
            alias = "Tasker.isInstalled",
            unavailable = RuntimeQueryDiagnosticCodes.TASKER_ADAPTER_UNAVAILABLE,
            checkFailed = RuntimeQueryDiagnosticCodes.TASKER_INSTALLATION_CHECK_FAILED,
        ),
        ProviderCase(
            id = "shizuku.isInstalled",
            alias = "Shizuku.isInstalled",
            unavailable = RuntimeQueryDiagnosticCodes.SHIZUKU_ADAPTER_UNAVAILABLE,
            checkFailed = RuntimeQueryDiagnosticCodes.SHIZUKU_INSTALLATION_CHECK_FAILED,
        ),
        ProviderCase(
            id = "termux.isInstalled",
            alias = "Termux.isInstalled",
            unavailable = RuntimeQueryDiagnosticCodes.TERMUX_ADAPTER_UNAVAILABLE,
            checkFailed = RuntimeQueryDiagnosticCodes.TERMUX_INSTALLATION_CHECK_FAILED,
        ),
    )

    @Test
    fun `catalog exposes three generic non-null Bool reporters`() {
        commands.forEach { provider ->
            val entry = VisualTaskerCommandCatalog.findById(provider.id)!!
            val definition = DefaultBlockRegistry.getDefinition(entry.block!!.blockType)!!

            assertEquals(provider.id, entry.canonicalName)
            assertEquals(listOf(provider.alias), entry.acceptedAliases)
            assertTrue(entry.arguments.isEmpty())
            assertEquals(CommandCatalogKind.REPORTER, entry.kind)
            assertEquals("Bool", entry.returnType)
            assertTrue(definition.isReporter)
            assertEquals("Bool", definition.outputType)
            assertFalse(definition.hasPrevious)
            assertFalse(definition.hasNext)
        }
    }

    @Test
    fun `LET SET IF and roundtrip preserve all stable command IDs`() {
        commands.forEach { provider ->
            val source = """
                LET installed:Bool = ${provider.alias}()
                SET installed = ${provider.id}()
                LET optionalInstalled:Bool? = ${provider.id}()
                IF ${provider.id}()
                  log("installed")
                ELSE
                  log("missing")
                END IF
            """.trimIndent()
            val preview = guard.preview(source) as EmscriptApplyGuardResult.Success
            val document = preview.importedDocument
            val registry = CompositeBlockRegistry().apply {
                document.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
            }
            val ir = IrGenerator(registry).generate(document)
            val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)

            assertTrue(ir.statements.toString(), ir.statements.toString().contains(provider.id))
            assertTrue(generated, generated.contains("${provider.id}()"))
            assertFalse(generated, generated.contains("${provider.alias}()"))
            assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)
        }
    }

    @Test
    fun `Bool nullable succeeds and String Number targets are rejected`() {
        commands.forEach { provider ->
            assertTrue(guard.preview("LET installed:Bool = ${provider.id}()") is EmscriptApplyGuardResult.Success)
            assertTrue(guard.preview("LET installed:Bool? = ${provider.id}()") is EmscriptApplyGuardResult.Success)
            listOf("String", "Number").forEach { target ->
                assertTrue(
                    "$target <- ${provider.id}",
                    guard.preview("LET installed:$target = ${provider.id}()") is EmscriptApplyGuardResult.Failure,
                )
            }
        }
    }

    @Test
    fun `true and false remain successful values and select normal IF branches`() = runBlocking {
        commands.forEach { provider ->
            listOf(true, false).forEach { installed ->
                val logs = mutableListOf<String>()
                val result = runtime(
                    provider = provider,
                    adapterResult = RuntimeAdapterResult.success(EmscriptValue.BooleanValue(installed)),
                    logs = logs,
                ).run(runtimeDocument(provider.id))

                assertTrue("${provider.id}:$installed -> $result", result is EmscriptDryRunResult.Success)
                result as EmscriptDryRunResult.Success
                assertEquals(EmscriptValue.BooleanValue(installed), result.variables["installed"])
                assertTrue(logs.contains(if (installed) "branch:true" else "branch:false"))
            }
        }
    }

    @Test
    fun `adapter absence and PackageManager failure use structured diagnostics`() = runBlocking {
        commands.forEach { provider ->
            listOf(provider.unavailable, provider.checkFailed).forEach { diagnostic ->
                val result = runtime(
                    provider = provider,
                    adapterResult = RuntimeAdapterResult.failure(diagnostic, "test failure"),
                ).run(runtimeDocument(provider.id))

                assertTrue("${provider.id}:$diagnostic -> $result", result is EmscriptDryRunResult.Failure)
                result as EmscriptDryRunResult.Failure
                assertTrue(result.events.any { it.kind == "runtime_failure" && it.diagnosticCode == diagnostic })
            }
        }
    }

    @Test
    fun `audit removes exactly the three installed queries and keeps Shizuku availability`() {
        assertEquals(commands.mapTo(linkedSetOf()) { it.id }, QueryReturnContractAudit.MIGRATED_M1B_3T)
        assertEquals(6, QueryReturnContractAudit.ALL.size)
        assertTrue(commands.none { provider -> QueryReturnContractAudit.ALL.any { it.stableId == provider.id } })
        assertEquals(setOf("shizuku.isAvailable"), QueryReturnContractAudit.MIGRATED_M1B_3U)
    }

    private fun runtimeDocument(commandId: String) = (guard.preview(
        """
        LET installed:Bool = $commandId()
        SET installed = $commandId()
        IF $commandId()
          log("branch:true")
        ELSE
          log("branch:false")
        END IF
        """.trimIndent(),
    ) as EmscriptApplyGuardResult.Success).importedDocument

    private fun runtime(
        provider: ProviderCase,
        adapterResult: RuntimeAdapterResult,
        logs: MutableList<String> = mutableListOf(),
    ) = WorkspaceBasicRuntime(
        capabilityGate = {
            RuntimeCapabilityGate.withDeviceAdapters(
                accessibilityAvailable = false,
                customChromeTabAvailable = false,
                shizukuAvailable = true,
                termuxAvailable = true,
                taskerAvailable = true,
                usbAdbBridgeAvailable = false,
            )
        },
        environment = WorkspaceBasicRuntimeEnvironment(
            delayMs = {},
            playBeep = { _, _, _ -> },
            vibrate = {},
            log = { logs += it },
            taskerInstalled = { if (provider.id.startsWith("tasker.")) adapterResult else unexpected(provider.id) },
            shizukuInstalled = { if (provider.id.startsWith("shizuku.")) adapterResult else unexpected(provider.id) },
            termuxInstalled = { if (provider.id.startsWith("termux.")) adapterResult else unexpected(provider.id) },
        ),
    )

    private fun unexpected(expected: String): RuntimeAdapterResult =
        RuntimeAdapterResult.failure("UNEXPECTED_PROVIDER", "Unexpected callback for $expected")

    private data class ProviderCase(
        val id: String,
        val alias: String,
        val unavailable: String,
        val checkFailed: String,
    )
}
