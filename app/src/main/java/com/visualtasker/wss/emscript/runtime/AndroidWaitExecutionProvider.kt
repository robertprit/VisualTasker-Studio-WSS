package com.visualtasker.wss.emscript.runtime

import de.visualtasker.workflow.semantics.CommandCapability
import de.visualtasker.emscript.contract.EmscriptV1CommandIds
import de.visualtasker.emscript.contract.ProviderId
import kotlin.math.ceil

class AndroidWaitExecutionProvider(
    private val sleep: (Long) -> Unit = Thread::sleep,
    private val cancellationPollMs: Long = 20L,
) : ExecutionProvider {
    override val id: String = PROVIDER_ID
    override val capabilities: Set<ExecutionCapability> = setOf(TIMING_CAPABILITY)
    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        id = ProviderId(PROVIDER_ID),
        displayName = "Android native timing",
        host = "Android runtime",
        capabilities = setOf(
            CapabilityDescriptor(
                id = TIMING_CAPABILITY.toCapabilityId(),
                runtimeCapability = TIMING_CAPABILITY,
                inputTypes = listOf(ProviderValueType.NUMBER),
                resultType = ProviderValueType.VOID,
                sideEffect = ProviderSideEffect.DELAY,
            ),
        ),
        priority = 100,
    )

    override fun supports(request: CapabilityRequest): Boolean =
        request.invocation.capability == TIMING_CAPABILITY &&
            request.invocation.commandId in setOf("wait", EmscriptV1CommandIds.ACTION_WAIT.value)

    override fun invoke(
        invocation: ProviderInvocation,
        context: ExecutionContext,
    ): ProviderResult {
        if (invocation.commandId !in setOf("wait", EmscriptV1CommandIds.ACTION_WAIT.value)) {
            return ProviderResult.Failure(
                code = "TIMING_COMMAND_UNSUPPORTED",
                message = "Android timing provider does not support '${invocation.commandId}'.",
            )
        }
        val duration = (invocation.arguments.singleOrNull() as? ExecutionValue.NumberValue)?.value
            ?: return ProviderResult.Failure(
                code = "WAIT_DURATION_REQUIRED",
                message = "wait(duration) requires exactly one numeric duration in milliseconds.",
            )
        if (!duration.isFinite() || duration < 0.0 || duration > Long.MAX_VALUE.toDouble()) {
            return ProviderResult.Failure(
                code = "WAIT_DURATION_INVALID",
                message = "wait(duration) requires a finite, non-negative millisecond duration.",
            )
        }

        var remainingMs = ceil(duration).toLong()
        while (remainingMs > 0L) {
            if (context.cancellationToken.isCancellationRequested) {
                return ProviderResult.Cancelled("wait($remainingMs ms remaining) cancelled.")
            }
            val sliceMs = minOf(remainingMs, cancellationPollMs.coerceAtLeast(1L))
            try {
                sleep(sliceMs)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                return ProviderResult.Cancelled("wait interrupted.")
            }
            remainingMs -= sliceMs
        }
        return if (context.cancellationToken.isCancellationRequested) {
            ProviderResult.Cancelled("wait cancelled.")
        } else {
            ProviderResult.Success()
        }
    }

    companion object {
        const val PROVIDER_ID = "android.native.timing"
        val TIMING_CAPABILITY = ExecutionCapability(CommandCapability.TIMING.name)
    }
}
