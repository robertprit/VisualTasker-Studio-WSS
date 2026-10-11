package com.visualtasker.wss.emscript.runtime

import de.visualtasker.workflow.core.WorkspaceDocument

enum class ExecutionState {
    CREATED,
    VALIDATING,
    READY,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REJECTED,
}

data class ExecutionRequest(
    val id: String,
    val workflowId: String,
    val document: WorkspaceDocument,
    val entryPointEntityId: String? = null,
    val initialVariables: Map<String, ExecutionValue> = emptyMap(),
    val providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
)

data class ExecutionPlan(
    val requestId: String,
    val workflowId: String,
    val sourceRevision: Long,
    val entryPointEntityId: String,
    val steps: List<ExecutionStep>,
    val requiredCapabilities: Set<ExecutionCapability>,
)

data class ExecutionContext(
    val requestId: String,
    val workflowId: String,
    val variables: MutableMap<String, ExecutionValue>,
    val cancellationToken: CancellationToken,
    val providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
)

sealed interface ExecutionStep {
    val entityId: String

    data class Assign(
        override val entityId: String,
        val variableId: String,
        val declaration: Boolean,
        val expression: ExecutionExpression,
    ) : ExecutionStep

    data class Invoke(
        override val entityId: String,
        val commandId: String,
        val capability: ExecutionCapability,
        val arguments: List<ExecutionExpression>,
    ) : ExecutionStep

    data class Branch(
        override val entityId: String,
        val alternatives: List<ExecutionBranchAlternative>,
        val elseSteps: List<ExecutionStep>,
    ) : ExecutionStep

    data class Repeat(
        override val entityId: String,
        val count: ExecutionExpression,
        val body: List<ExecutionStep>,
    ) : ExecutionStep

    data class While(
        override val entityId: String,
        val condition: ExecutionExpression,
        val body: List<ExecutionStep>,
    ) : ExecutionStep
}

data class ExecutionBranchAlternative(
    val relationId: String,
    val condition: ExecutionExpression,
    val steps: List<ExecutionStep>,
)

sealed interface ExecutionExpression {
    val entityId: String

    data class Literal(
        override val entityId: String,
        val value: ExecutionValue,
    ) : ExecutionExpression

    data class Variable(
        override val entityId: String,
        val variableId: String,
    ) : ExecutionExpression

    data class Binary(
        override val entityId: String,
        val operator: String,
        val left: ExecutionExpression,
        val right: ExecutionExpression,
    ) : ExecutionExpression

    data class ListExpression(
        override val entityId: String,
        val values: List<ExecutionExpression>,
    ) : ExecutionExpression

    data class ValueConstructor(
        override val entityId: String,
        val commandId: String,
        val arguments: List<ExecutionExpression>,
        val argumentNames: List<String?> = emptyList(),
    ) : ExecutionExpression

    data class ProviderCall(
        override val entityId: String,
        val commandId: String,
        val capability: ExecutionCapability,
        val arguments: List<ExecutionExpression>,
    ) : ExecutionExpression
}

sealed interface ExecutionValue {
    data class NumberValue(val value: Double) : ExecutionValue
    data class StringValue(val value: String) : ExecutionValue
    data class BooleanValue(val value: Boolean) : ExecutionValue
    data class ListValue(val values: List<ExecutionValue>) : ExecutionValue
    data class PointValue(val value: PointerPathPoint) : ExecutionValue
    data class PathSegmentValue(val value: PointerPathSegment) : ExecutionValue
    data class PathValue(val value: PointerPathGeometry) : ExecutionValue
    data class PointerPathValue(val value: PointerPath) : ExecutionValue
    data class MultiPathValue(val value: MultiPointerGesture) : ExecutionValue
    data object NullValue : ExecutionValue
    data object VoidValue : ExecutionValue
}

@JvmInline
value class ExecutionCapability(val value: String)

data class ExecutionDiagnostic(
    val code: String,
    val message: String,
    val entityId: String? = null,
    val pathTrace: PointerPathTraceInfo? = null,
    val targetTrace: TargetResolutionTraceInfo? = null,
    val providerTrace: ProviderResolutionTraceInfo? = null,
)

data class TargetResolutionTraceInfo(
    val queryType: String,
    val candidateCount: Int,
    val resolutionStatus: String,
    val action: String? = null,
    val fallbackUsed: Boolean = false,
    val displayId: Int,
    val observationStatus: String? = null,
)

data class PointerPathTraceInfo(
    val segmentCount: Int,
    val segmentTypes: List<String>,
    val plannedTotalDurationMs: Long?,
    val status: String,
    val invalidSegmentIndex: Int? = null,
    val cancellationPosition: String? = null,
    val pointerCount: Int = 1,
    val pointerIds: List<Int> = listOf(0),
    val pointerStartOffsetsMs: List<Long> = emptyList(),
    val pointerEndOffsetsMs: List<Long> = emptyList(),
    val pointerSegmentCounts: List<Int> = emptyList(),
    val gestureKind: String = "SINGLE_TOUCH",
)

enum class ExecutionTraceEventKind {
    EXECUTION_STARTED,
    EXECUTION_FINISHED,
    STATE_CHANGED,
    STEP_STARTED,
    STEP_FINISHED,
    BRANCH_SELECTED,
    PROVIDER_STARTED,
    PROVIDER_FINISHED,
    DIAGNOSTIC,
    CANCELLED,
}

data class ExecutionTraceEvent(
    val index: Int,
    val kind: ExecutionTraceEventKind,
    val state: ExecutionState,
    val entityId: String? = null,
    val relationId: String? = null,
    val capability: ExecutionCapability? = null,
    val providerId: String? = null,
    val value: ExecutionValue? = null,
    val diagnostic: ExecutionDiagnostic? = null,
    val pathTrace: PointerPathTraceInfo? = null,
    val targetTrace: TargetResolutionTraceInfo? = null,
    val providerTrace: ProviderResolutionTraceInfo? = null,
)

data class ExecutionResult(
    val requestId: String,
    val state: ExecutionState,
    val value: ExecutionValue,
    val variables: Map<String, ExecutionValue>,
    val diagnostics: List<ExecutionDiagnostic>,
    val trace: List<ExecutionTraceEvent>,
)

interface CancellationToken {
    val isCancellationRequested: Boolean
}

class MutableCancellationToken : CancellationToken {
    @Volatile
    override var isCancellationRequested: Boolean = false
        private set

    fun cancel() {
        isCancellationRequested = true
    }
}

data class ProviderInvocation(
    val requestId: String,
    val workflowId: String,
    val entityId: String,
    val commandId: String,
    val capability: ExecutionCapability,
    val arguments: List<ExecutionValue>,
)

sealed interface ProviderResult {
    data class Success(
        val value: ExecutionValue = ExecutionValue.VoidValue,
        val targetTrace: TargetResolutionTraceInfo? = null,
    ) : ProviderResult
    data class Failure(
        val code: String,
        val message: String,
        val targetTrace: TargetResolutionTraceInfo? = null,
        val failureClass: ProviderFailureClass = ProviderFailureClass.EXECUTION_FAILED,
        val effectStatus: ProviderEffectStatus = ProviderEffectStatus.UNKNOWN,
    ) : ProviderResult
    data class Cancelled(
        val message: String = "Provider invocation cancelled.",
        val targetTrace: TargetResolutionTraceInfo? = null,
    ) : ProviderResult
}

interface ExecutionProvider {
    val id: String
    val capabilities: Set<ExecutionCapability>
    val descriptor: ProviderDescriptor
        get() = defaultDescriptor()

    fun supports(request: CapabilityRequest): Boolean = request.invocation.capability in capabilities

    fun availability(
        request: CapabilityRequest,
        context: ExecutionContext,
    ): ProviderAvailability = ProviderAvailability.available()

    fun invoke(
        invocation: ProviderInvocation,
        context: ExecutionContext,
    ): ProviderResult
}

class ExecutionProviderRegistry(
    providers: List<ExecutionProvider>,
) {
    private val providers = providers.toList().also { entries ->
        val duplicateIds = entries.groupBy { it.descriptor.id }.filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) {
            "Duplicate execution provider IDs: ${duplicateIds.joinToString { it.value }}"
        }
    }

    fun descriptors(): List<ProviderDescriptor> = providers
        .map(ExecutionProvider::descriptor)
        .sortedBy { it.id.value }

    fun discover(
        request: CapabilityRequest,
        context: ExecutionContext,
    ): List<ProviderDiscovery> = providers
        .sortedBy { it.descriptor.id.value }
        .map { provider ->
            val compatible = provider.supports(request)
            ProviderDiscovery(
                descriptor = provider.descriptor,
                compatible = compatible,
                availability = if (compatible) {
                    provider.availability(request, context)
                } else {
                    ProviderAvailability(
                        capability = ProviderCapabilityState.UNSUPPORTED,
                        execution = ProviderExecutionState.NOT_EXECUTABLE,
                        diagnosticCode = "PROVIDER_CAPABILITY_UNSUPPORTED",
                        message = "Provider '${provider.descriptor.id.value}' does not support '${request.invocation.commandId}'.",
                    )
                },
            )
        }

    fun resolve(capability: ExecutionCapability): ExecutionProvider? =
        providers
            .filter { capability in it.capabilities }
            .sortedWith(compareByDescending<ExecutionProvider> { it.descriptor.priority }.thenBy { it.descriptor.id.value })
            .firstOrNull()

    fun resolve(
        request: CapabilityRequest,
        context: ExecutionContext,
    ): ProviderResolution {
        val capability = request.invocation.capability
        val compatible = providers.filter { provider ->
            provider.descriptor.id !in request.policy.excludedProviderIds && provider.supports(request)
        }
        request.policy.explicitProviderId?.let { explicitId ->
            val provider = providers.firstOrNull { it.descriptor.id == explicitId }
                ?: return ProviderResolution.Rejected(
                    ProviderFailure(
                        code = "PROVIDER_EXPLICIT_NOT_REGISTERED",
                        message = "Explicit provider '${explicitId.value}' is not registered.",
                        failureClass = ProviderFailureClass.NOT_REGISTERED,
                        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
                        providerId = explicitId,
                    ),
                )
            if (explicitId in request.policy.excludedProviderIds || !provider.supports(request)) {
                return ProviderResolution.Rejected(
                    ProviderFailure(
                        code = "PROVIDER_EXPLICIT_INCOMPATIBLE",
                        message = "Explicit provider '${explicitId.value}' is excluded or incompatible with '${capability.value}'.",
                        failureClass = ProviderFailureClass.CAPABILITY_UNAVAILABLE,
                        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
                        providerId = explicitId,
                    ),
                )
            }
            val availability = provider.availability(request, context)
            return if (availability.isExecutable) {
                ProviderResolution.Selected(provider, emptyList(), ProviderSelectionReason.EXPLICIT, availability)
            } else {
                ProviderResolution.Rejected(availability.toFailure(explicitId), availability)
            }
        }
        if (compatible.isEmpty()) {
            return ProviderResolution.Rejected(
                ProviderFailure(
                    code = "CAPABILITY_PROVIDER_MISSING",
                    message = "No provider supplies capability '${capability.value}' for '${request.invocation.commandId}'.",
                    failureClass = ProviderFailureClass.NOT_REGISTERED,
                    effectStatus = ProviderEffectStatus.NOT_EXECUTED,
                ),
            )
        }
        val preferredOrder = request.policy.preferredProviderIds.withIndex().associate { (index, id) -> id to index }
        val ordered = compatible.sortedWith(
            compareBy<ExecutionProvider> { preferredOrder[it.descriptor.id] ?: Int.MAX_VALUE }
                .thenByDescending { it.descriptor.priority }
                .thenBy { it.descriptor.id.value },
        )
        val inspected = ordered.map { provider -> provider to provider.availability(request, context) }
        val selectedIndex = inspected.indexOfFirst { (_, availability) -> availability.isExecutable }
        if (selectedIndex < 0) {
            val (provider, availability) = inspected.first()
            return ProviderResolution.Rejected(availability.toFailure(provider.descriptor.id), availability)
        }
        val (selected, availability) = inspected[selectedIndex]
        val reason = when {
            selected.descriptor.id in preferredOrder -> ProviderSelectionReason.PREFERRED
            compatible.size > 1 && compatible.count { it.descriptor.priority == selected.descriptor.priority } > 1 ->
                ProviderSelectionReason.STABLE_ID_TIE_BREAK
            else -> ProviderSelectionReason.PRIORITY
        }
        val fallbacks = if (request.policy.fallbackMode == ProviderFallbackMode.SAFE_PRE_EXECUTION_ONLY) {
            inspected.drop(selectedIndex + 1).filter { it.second.isExecutable }.map { it.first }
        } else {
            emptyList()
        }
        return ProviderResolution.Selected(selected, fallbacks, reason, availability)
    }
}

sealed interface ExecutionPlanResult {
    data class Ready(val plan: ExecutionPlan) : ExecutionPlanResult
    data class Rejected(val diagnostics: List<ExecutionDiagnostic>) : ExecutionPlanResult
}

internal class ExecutionLifecycle {
    var state: ExecutionState = ExecutionState.CREATED
        private set

    fun transitionTo(next: ExecutionState) {
        require(next in allowedTransitions.getValue(state)) {
            "Invalid execution state transition: $state -> $next"
        }
        state = next
    }

    private companion object {
        val allowedTransitions = mapOf(
            ExecutionState.CREATED to setOf(ExecutionState.VALIDATING),
            ExecutionState.VALIDATING to setOf(ExecutionState.READY, ExecutionState.REJECTED),
            ExecutionState.READY to setOf(ExecutionState.RUNNING, ExecutionState.CANCELLED),
            ExecutionState.RUNNING to setOf(
                ExecutionState.COMPLETED,
                ExecutionState.FAILED,
                ExecutionState.CANCELLED,
            ),
            ExecutionState.COMPLETED to emptySet(),
            ExecutionState.FAILED to emptySet(),
            ExecutionState.CANCELLED to emptySet(),
            ExecutionState.REJECTED to emptySet(),
        )
    }
}
