package com.visualtasker.wss.emscript.runtime

import android.content.Context
import com.visualtasker.wss.workspace.plugin.runtime.TaskerRegistration
import com.visualtasker.wss.workspace.plugin.runtime.TaskerVariableSnapshotStore

class TaskerExternalVariableProvider(private val context: Context) : VariableProvider {
    override val id: String = TASKER_VARIABLE_PROVIDER_ID

    override fun read(binding: VariableBinding, cancellationToken: CancellationToken): VariableReadResult {
        if (cancellationToken.isCancellationRequested) return failed(binding, VariableFailureCode.CANCELLED, "Tasker variable read was cancelled.")
        val availability = TaskerRegistration.inspect(context)
        if (!availability.available) {
            return failed(binding, VariableFailureCode.PROVIDER_UNAVAILABLE, "Tasker variable provider unavailable: ${availability.summary}")
        }
        val result = TaskerVariableSnapshotStore.query(context, binding.variableLabel)
        result.failure?.let { return failed(binding, VariableFailureCode.READ_UNAVAILABLE, "Tasker variable read failed: ${it.message}") }
        return if (result.present) {
            convertSnapshot(binding, result.value)
        } else {
            VariableReadResult.Missing
        }
    }

    override fun write(
        binding: VariableBinding,
        value: ExecutionValue,
        expectedRevision: Long?,
        cancellationToken: CancellationToken,
    ): VariableWriteResult = VariableWriteResult.Failed(
        VariableFailure(
            if (cancellationToken.isCancellationRequested) VariableFailureCode.CANCELLED else VariableFailureCode.WRITE_UNAVAILABLE,
            if (cancellationToken.isCancellationRequested) {
                "Tasker variable write was cancelled."
            } else {
                "Tasker variable writes have no confirmed callback contract and remain PARTIAL."
            },
            binding.variableId,
            binding.scope,
            id,
        ),
    )

    private fun failed(binding: VariableBinding, code: VariableFailureCode, message: String) = VariableReadResult.Failed(
        VariableFailure(code, message, binding.variableId, binding.scope, id),
    )

    private fun convertSnapshot(binding: VariableBinding, rawValue: String?): VariableReadResult {
        if (rawValue == null) {
            return if (binding.declaredType.endsWith("?") || binding.declaredType.equals("Any", true)) {
                VariableReadResult.Found(VariableStoredValue(ExecutionValue.NullValue, 0L))
            } else {
                failed(binding, VariableFailureCode.TYPE_MISMATCH, "Tasker returned null for non-null ${binding.declaredType} variable.")
            }
        }
        val value = when (binding.declaredType.removeSuffix("?").lowercase()) {
            "", "any", "string" -> ExecutionValue.StringValue(rawValue)
            "number" -> rawValue.toDoubleOrNull()?.let(ExecutionValue::NumberValue)
                ?: return failed(binding, VariableFailureCode.TYPE_MISMATCH, "Tasker value cannot be converted to Number.")
            "bool", "boolean" -> when (rawValue.lowercase()) {
                "true" -> ExecutionValue.BooleanValue(true)
                "false" -> ExecutionValue.BooleanValue(false)
                else -> return failed(binding, VariableFailureCode.TYPE_MISMATCH, "Tasker value cannot be converted to Bool.")
            }
            else -> return failed(
                binding,
                VariableFailureCode.READ_UNAVAILABLE,
                "Tasker snapshot conversion for ${binding.declaredType} is not implemented.",
            )
        }
        return VariableReadResult.Found(VariableStoredValue(value, 0L))
    }
}

fun androidVariableRuntimeFactory(context: Context): VariableRuntimeSessionFactory {
    val store = AtomicFileVariableValueStore(context.applicationContext.filesDir.resolve("runtime-variable-values"))
    return DefaultVariableRuntimeSessionFactory(
        VariableProviderRegistry(
            listOf(
                ProjectVariableProvider(store),
                AppSharedVariableProvider(store),
                TaskerExternalVariableProvider(context.applicationContext),
            ),
        ),
    )
}
