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
    data object NullValue : ExecutionValue
    data object VoidValue : ExecutionValue
}

@JvmInline
value class ExecutionCapability(val value: String)

data class ExecutionDiagnostic(
    val code: String,
    val message: String,
    val entityId: String? = null,
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
    data class Success(val value: ExecutionValue = ExecutionValue.VoidValue) : ProviderResult
    data class Failure(val code: String, val message: String) : ProviderResult
    data class Cancelled(val message: String = "Provider invocation cancelled.") : ProviderResult
}

interface ExecutionProvider {
    val id: String
    val capabilities: Set<ExecutionCapability>

    fun invoke(
        invocation: ProviderInvocation,
        context: ExecutionContext,
    ): ProviderResult
}

class ExecutionProviderRegistry(
    providers: List<ExecutionProvider>,
) {
    private val providers = providers.toList()

    fun resolve(capability: ExecutionCapability): ExecutionProvider? =
        providers.firstOrNull { capability in it.capabilities }
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
