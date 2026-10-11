package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.emscript.contract.ProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderPolicyTest {
    @Test
    fun uniqueAvailableProviderIsSelectedAndDiscovered() {
        val provider = fake("provider.native", priority = 10)
        val registry = ExecutionProviderRegistry(listOf(provider))

        val resolution = registry.resolve(request(), context()) as ProviderResolution.Selected
        val discovery = registry.discover(request(), context())

        assertEquals(provider, resolution.provider)
        assertEquals(ProviderSelectionReason.PRIORITY, resolution.reason)
        assertTrue(discovery.single().compatible)
        assertTrue(discovery.single().availability.isExecutable)
    }

    @Test
    fun explicitAvailableProviderWinsOverPriority() {
        val high = fake("provider.high", priority = 100)
        val explicit = fake("provider.explicit", priority = 1)

        val resolution = ExecutionProviderRegistry(listOf(high, explicit)).resolve(
            request(policy = ProviderSelectionPolicy(explicitProviderId = explicit.descriptor.id)),
            context(),
        ) as ProviderResolution.Selected

        assertEquals(explicit, resolution.provider)
        assertEquals(ProviderSelectionReason.EXPLICIT, resolution.reason)
    }

    @Test
    fun explicitUnavailableProviderFailsWithoutSubstitution() {
        val unavailable = fake("provider.explicit", availability = disconnected())
        val alternative = fake("provider.alternative", priority = 100)

        val resolution = ExecutionProviderRegistry(listOf(unavailable, alternative)).resolve(
            request(policy = ProviderSelectionPolicy(explicitProviderId = unavailable.descriptor.id)),
            context(),
        ) as ProviderResolution.Rejected

        assertEquals(ProviderFailureClass.NOT_CONNECTED, resolution.failure.failureClass)
        assertEquals(unavailable.descriptor.id, resolution.failure.providerId)
    }

    @Test
    fun automaticSelectionUsesPriorityThenStableProviderId() {
        val low = fake("provider.low", priority = 1)
        val zeta = fake("provider.zeta", priority = 10)
        val alpha = fake("provider.alpha", priority = 10)

        val resolution = ExecutionProviderRegistry(listOf(low, zeta, alpha)).resolve(request(), context()) as ProviderResolution.Selected

        assertEquals(alpha, resolution.provider)
        assertEquals(ProviderSelectionReason.STABLE_ID_TIE_BREAK, resolution.reason)
    }

    @Test
    fun preferredProviderAndExclusionAreAppliedDeterministically() {
        val alpha = fake("provider.alpha", priority = 100)
        val beta = fake("provider.beta", priority = 1)
        val policy = ProviderSelectionPolicy(
            preferredProviderIds = listOf(beta.descriptor.id),
            excludedProviderIds = setOf(alpha.descriptor.id),
        )

        val resolution = ExecutionProviderRegistry(listOf(alpha, beta)).resolve(request(policy = policy), context()) as ProviderResolution.Selected

        assertEquals(beta, resolution.provider)
        assertEquals(ProviderSelectionReason.PREFERRED, resolution.reason)
    }

    @Test
    fun permissionInstallationAndConnectionFailuresRemainDistinct() {
        val permission = resolveFailure(ProviderAvailability(permission = ProviderPermissionState.DENIED, execution = ProviderExecutionState.NOT_EXECUTABLE))
        val installation = resolveFailure(ProviderAvailability(installation = ProviderInstallationState.NOT_INSTALLED, execution = ProviderExecutionState.NOT_EXECUTABLE))
        val connection = resolveFailure(disconnected())

        assertEquals(ProviderFailureClass.PERMISSION_DENIED, permission.failureClass)
        assertEquals(ProviderFailureClass.NOT_INSTALLED, installation.failureClass)
        assertEquals(ProviderFailureClass.NOT_CONNECTED, connection.failureClass)
    }

    @Test
    fun connectedProviderWithoutCapabilityIsReportedUnsupported() {
        val provider = fake("provider.other", supportedCommands = setOf("other"))
        val registry = ExecutionProviderRegistry(listOf(provider))

        val discovery = registry.discover(request(), context()).single()
        val resolution = registry.resolve(request(), context()) as ProviderResolution.Rejected

        assertFalse(discovery.compatible)
        assertEquals(ProviderCapabilityState.UNSUPPORTED, discovery.availability.capability)
        assertEquals(ProviderFailureClass.NOT_REGISTERED, resolution.failure.failureClass)
    }

    @Test
    fun safePreExecutionFailureUsesCompatibleFallbackAndTracesDecision() {
        val first = fake(
            "provider.first",
            priority = 100,
            result = ProviderResult.Failure(
                "PROVIDER_LOST",
                "lost before execution",
                failureClass = ProviderFailureClass.PROVIDER_LOST,
                effectStatus = ProviderEffectStatus.NOT_EXECUTED,
            ),
        )
        val fallback = fake("provider.fallback", priority = 10, result = ProviderResult.Success(ExecutionValue.StringValue("ok")))

        val result = runLog(
            providers = listOf(first, fallback),
            policy = ProviderSelectionPolicy(fallbackMode = ProviderFallbackMode.SAFE_PRE_EXECUTION_ONLY),
        )

        assertEquals(ExecutionState.COMPLETED, result.state)
        assertEquals(1, first.invocationCount)
        assertEquals(1, fallback.invocationCount)
        assertTrue(result.trace.any { it.providerTrace?.fallbackDecision == ProviderFallbackDecision.USED.name })
        assertTrue(result.trace.any { it.providerTrace?.resultStatus == "FAILED_BEFORE_EXECUTION" })
    }

    @Test
    fun fallbackIsForbiddenByPolicy() {
        val first = fake(
            "provider.first",
            priority = 100,
            result = ProviderResult.Failure(
                "REJECTED",
                "not accepted",
                failureClass = ProviderFailureClass.REJECTED,
                effectStatus = ProviderEffectStatus.NOT_EXECUTED,
            ),
        )
        val fallback = fake("provider.fallback")

        val result = runLog(listOf(first, fallback), ProviderSelectionPolicy())

        assertEquals(ExecutionState.FAILED, result.state)
        assertEquals(0, fallback.invocationCount)
        assertEquals(ProviderFallbackDecision.BLOCKED_POLICY.name, result.diagnostics.single().providerTrace?.fallbackDecision)
    }

    @Test
    fun timeoutAndUnknownSideEffectNeverRetryNonIdempotentAction() {
        listOf(
            ProviderFailureClass.TIMEOUT to ProviderEffectStatus.MAY_HAVE_EXECUTED,
            ProviderFailureClass.UNKNOWN_RESULT to ProviderEffectStatus.UNKNOWN,
            ProviderFailureClass.PROVIDER_LOST to ProviderEffectStatus.EXECUTED,
        ).forEach { (failureClass, effectStatus) ->
            val first = fake(
                "provider.first.$failureClass",
                priority = 100,
                result = ProviderResult.Failure(
                    failureClass.name,
                    "unsafe outcome",
                    failureClass = failureClass,
                    effectStatus = effectStatus,
                ),
            )
            val fallback = fake("provider.fallback.$failureClass")

            val result = runLog(
                listOf(first, fallback),
                ProviderSelectionPolicy(fallbackMode = ProviderFallbackMode.SAFE_PRE_EXECUTION_ONLY),
            )

            assertEquals(ExecutionState.FAILED, result.state)
            assertEquals(0, fallback.invocationCount)
            assertEquals(ProviderFallbackDecision.BLOCKED_SIDE_EFFECT.name, result.diagnostics.single().providerTrace?.fallbackDecision)
        }
    }

    @Test
    fun incompatibleFallbackIsNotOffered() {
        val first = fake("provider.first", priority = 100, result = preExecutionFailure())
        val incompatible = fake("provider.incompatible", supportedCommands = setOf("other"))

        val result = runLog(
            listOf(first, incompatible),
            ProviderSelectionPolicy(fallbackMode = ProviderFallbackMode.SAFE_PRE_EXECUTION_ONLY),
        )

        assertEquals(ExecutionState.FAILED, result.state)
        assertEquals(0, incompatible.invocationCount)
        assertEquals(ProviderFallbackDecision.NO_COMPATIBLE_PROVIDER.name, result.diagnostics.single().providerTrace?.fallbackDecision)
    }

    @Test
    fun cancellationAndTypedResultAreProjectedIntoTrace() {
        val cancelled = fake("provider.cancelled", result = ProviderResult.Cancelled("cancelled"))
        val cancelledResult = runLog(listOf(cancelled), ProviderSelectionPolicy())
        assertEquals(ExecutionState.CANCELLED, cancelledResult.state)
        assertTrue(cancelledResult.trace.any { it.providerTrace?.cancelled == true })

        val typed = fake("provider.typed", result = ProviderResult.Success(ExecutionValue.BooleanValue(false)))
        val typedResult = runLog(listOf(typed), ProviderSelectionPolicy())
        assertTrue(typedResult.trace.any { it.value == ExecutionValue.BooleanValue(false) })
    }

    @Test
    fun duplicateProviderIdsAreRejected() {
        val failure = runCatching {
            ExecutionProviderRegistry(listOf(fake("provider.same"), fake("provider.same")))
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun resolveFailure(availability: ProviderAvailability): ProviderFailure {
        val provider = fake("provider.failure", availability = availability)
        return (ExecutionProviderRegistry(listOf(provider)).resolve(request(), context()) as ProviderResolution.Rejected).failure
    }

    private fun runLog(
        providers: List<FakePolicyProvider>,
        policy: ProviderSelectionPolicy,
    ): ExecutionResult {
        val document = EmscriptWorkspaceImporter().import("log(\"policy\")", "m5-8-policy").let {
            require(it.isSuccess) { it.issues.joinToString { issue -> issue.message } }
            requireNotNull(it.document)
        }
        val baseRequest = ExecutionRequest("request:m5-8", document.id, document)
        val plan = WorkflowExecutionPlanner().plan(baseRequest) as ExecutionPlanResult.Ready
        val capability = plan.plan.requiredCapabilities.single()
        providers.forEach { it.runtimeCapability = capability }
        return DeterministicExecutionRunner( providers = ExecutionProviderRegistry(providers)).execute(
            baseRequest.copy(providerPolicies = mapOf(capability to policy)),
        )
    }

    private fun request(policy: ProviderSelectionPolicy = ProviderSelectionPolicy()): CapabilityRequest = CapabilityRequest(
        invocation = ProviderInvocation(
            requestId = "request",
            workflowId = "workflow",
            entityId = "entity",
            commandId = "test.command",
            capability = TEST_CAPABILITY,
            arguments = emptyList(),
        ),
        policy = policy,
    )

    private fun context() = ExecutionContext(
        requestId = "request",
        workflowId = "workflow",
        variables = mutableMapOf(),
        cancellationToken = MutableCancellationToken(),
    )

    private fun fake(
        id: String,
        priority: Int = 0,
        availability: ProviderAvailability = ProviderAvailability.available(),
        supportedCommands: Set<String> = setOf("test.command", "debug.log"),
        result: ProviderResult = ProviderResult.Success(),
    ) = FakePolicyProvider(id, priority, availability, supportedCommands, result)

    private fun disconnected() = ProviderAvailability(
        connection = ProviderConnectionState.DISCONNECTED,
        execution = ProviderExecutionState.NOT_EXECUTABLE,
    )

    private fun preExecutionFailure() = ProviderResult.Failure(
        "REJECTED",
        "rejected before execution",
        failureClass = ProviderFailureClass.REJECTED,
        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
    )

    private class FakePolicyProvider(
        private val providerId: String,
        private val priority: Int,
        private val providerAvailability: ProviderAvailability,
        private val supportedCommands: Set<String>,
        private val result: ProviderResult,
    ) : ExecutionProvider {
        var runtimeCapability: ExecutionCapability = TEST_CAPABILITY
        var invocationCount: Int = 0
            private set
        override val id: String get() = providerId
        override val capabilities: Set<ExecutionCapability> get() = setOf(runtimeCapability)
        override val descriptor: ProviderDescriptor
            get() = ProviderDescriptor(
                id = ProviderId(providerId.lowercase()),
                displayName = providerId,
                host = "test",
                capabilities = setOf(
                    CapabilityDescriptor(
                        id = runtimeCapability.toCapabilityId(),
                        runtimeCapability = runtimeCapability,
                        resultType = ProviderValueType.ANY,
                        sideEffect = ProviderSideEffect.EXTERNAL_ACTION,
                    ),
                ),
                priority = priority,
            )

        override fun supports(request: CapabilityRequest): Boolean =
            request.invocation.capability == runtimeCapability && request.invocation.commandId in supportedCommands

        override fun availability(request: CapabilityRequest, context: ExecutionContext): ProviderAvailability = providerAvailability

        override fun invoke(invocation: ProviderInvocation, context: ExecutionContext): ProviderResult {
            invocationCount += 1
            return result
        }
    }

    private companion object {
        val TEST_CAPABILITY = ExecutionCapability("TEST")
    }
}
