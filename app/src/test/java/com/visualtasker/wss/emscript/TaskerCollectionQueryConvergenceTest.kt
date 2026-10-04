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
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.workflow.serialization.WorkflowSerializer
import de.visualtasker.emscript.contract.ProviderTypes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskerCollectionQueryConvergenceTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun `ListValue is typed ordered immutable and distinct from absence`() {
        val mutableValues = mutableListOf<EmscriptValue>(
            EmscriptValue.TaskerVariableValue("%B", "2"),
            EmscriptValue.TaskerVariableValue("%A", ""),
        )
        val list = EmscriptValue.ListValue(ProviderTypes.TASKER_VARIABLE.ref, mutableValues)
        mutableValues.clear()

        assertEquals(listOf("%B", "%A"), list.values.map { (it as EmscriptValue.TaskerVariableValue).name })
        assertEquals(listOf("2", ""), list.values.map { (it as EmscriptValue.TaskerVariableValue).value })
        assertNotEquals(EmscriptValue.NullValue, list)
        assertNotNull(RuntimeAdapterResult.success(list).value)
        assertEquals(list, RuntimeAdapterResult.success(list).value)
        assertTrue(EmscriptValue.ListValue(ProviderTypes.TASKER_VARIABLE.ref, emptyList()).values.isEmpty())
        assertTrue(runCatching {
            @Suppress("UNCHECKED_CAST")
            (list.values as MutableList<EmscriptValue>).clear()
        }.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `explicit List TaskerVariable declarations parse and roundtrip canonically`() {
        val source = """
            LET vars:List<TaskerVariable> = Tasker.getVariables("%VT_*")
            SET vars = tasker.getVariables()
        """.trimIndent()
        val preview = guard.preview(source)
        assertTrue(preview.toString(), preview is EmscriptApplyGuardResult.Success)
        preview as EmscriptApplyGuardResult.Success
        assertEquals("List<TaskerVariable>", preview.importedDocument.variables.variables.getValue("vars").type)

        val registry = CompositeBlockRegistry().apply {
            preview.importedDocument.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val ir = IrGenerator(registry).generate(preview.importedDocument)
        val calls = ir.statements.filterIsInstance<IrStatement.SetVariable>()
            .mapNotNull { it.expression as? IrExpression.CommandCall }
        assertEquals(2, calls.size)
        assertTrue(calls.all { it.commandId == "tasker.getVariables" })
        assertTrue(calls.all { it.returnType == "List<TaskerVariable>" })

        val generated = EmscriptGenerator(IrGenerator(registry)).generate(preview.importedDocument)
        assertTrue(generated, generated.contains("let vars:List<TaskerVariable> = tasker.getVariables(\"%VT_*\");"))
        assertTrue(generated, generated.contains("set vars = tasker.getVariables();"))
        assertFalse(generated, generated.contains("Tasker.getVariables"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)

        val decoded = WorkflowSerializer.deserialize(WorkflowSerializer.serialize(preview.importedDocument))
        assertEquals("List<TaskerVariable>", decoded.variables.variables.getValue("vars").type)
    }

    @Test
    fun `Tasker collection query rejects scalar declarations`() {
        listOf("String", "Number", "Bool").forEach { target ->
            val result = guard.preview("LET vars:$target = tasker.getVariables()")
            assertTrue("$target -> $result", result is EmscriptApplyGuardResult.Failure)
        }
    }

    @Test
    fun `runtime transports empty one and many Tasker variables losslessly`() = runBlocking {
        val cases = listOf(
            emptyList(),
            listOf(EmscriptValue.TaskerVariableValue("%A", "")),
            listOf(
                EmscriptValue.TaskerVariableValue("%A", "1"),
                EmscriptValue.TaskerVariableValue("%B", "2"),
            ),
        )
        cases.forEach { values ->
            val expected = EmscriptValue.ListValue(ProviderTypes.TASKER_VARIABLE.ref, values)
            val result = runtime(RuntimeAdapterResult.success(expected)).run(document())
            assertTrue(result.toString(), result is EmscriptDryRunResult.Success)
            result as EmscriptDryRunResult.Success
            assertEquals(expected, result.variables["vars"])
        }
    }

    @Test
    fun `runtime keeps provider failures missing values and invalid list types distinct`() = runBlocking {
        val failure = runtime(
            RuntimeAdapterResult.failure(RuntimeQueryDiagnosticCodes.TASKER_VARIABLE_QUERY_FAILED, "snapshot"),
        ).run(document())
        assertTrue(failure is EmscriptDryRunResult.Failure)
        failure as EmscriptDryRunResult.Failure
        assertTrue(failure.events.any { it.diagnosticCode == RuntimeQueryDiagnosticCodes.TASKER_VARIABLE_QUERY_FAILED })

        val missingValue = runtime(RuntimeAdapterResult.successWithoutValue("missing")).run(document())
        assertTrue(missingValue is EmscriptDryRunResult.Failure)
        missingValue as EmscriptDryRunResult.Failure
        assertTrue(missingValue.events.any { it.diagnosticCode == RuntimeQueryDiagnosticCodes.TASKER_RESULT_INVALID })

        val wrongList = EmscriptValue.ListValue(
            de.visualtasker.emscript.contract.CoreTypes.STRING.ref,
            listOf(EmscriptValue.StringValue("%A=1")),
        )
        val invalid = runtime(RuntimeAdapterResult.success(wrongList)).run(document())
        assertTrue(invalid is EmscriptDryRunResult.Failure)
        invalid as EmscriptDryRunResult.Failure
        assertTrue(invalid.events.any { it.diagnosticCode == RuntimeQueryDiagnosticCodes.TASKER_RESULT_INVALID })
    }

    @Test
    fun `missing Tasker adapter fails instead of producing empty collection`() = runBlocking {
        val result = WorkspaceBasicRuntime(
            capabilityGate = ::allProvidersReady,
            environment = baseEnvironment(),
        ).run(document())

        assertTrue(result is EmscriptDryRunResult.Failure)
        result as EmscriptDryRunResult.Failure
        assertTrue(result.events.any { it.diagnosticCode == RuntimeQueryDiagnosticCodes.TASKER_ADAPTER_UNAVAILABLE })
    }

    private fun document() = (guard.preview(
        "LET vars:List<TaskerVariable> = tasker.getVariables()",
    ) as EmscriptApplyGuardResult.Success).importedDocument

    private fun runtime(result: RuntimeAdapterResult) = WorkspaceBasicRuntime(
        capabilityGate = ::allProvidersReady,
        environment = baseEnvironment().copy(taskerGetVariables = { result }),
    )

    private fun baseEnvironment() = WorkspaceBasicRuntimeEnvironment(
        delayMs = {},
        playBeep = { _, _, _ -> },
        vibrate = {},
        log = {},
    )

    private fun allProvidersReady() = RuntimeCapabilityGate.withDeviceAdapters(
        accessibilityAvailable = false,
        customChromeTabAvailable = false,
        shizukuAvailable = false,
        termuxAvailable = false,
        taskerAvailable = true,
        usbAdbBridgeAvailable = false,
    )
}
