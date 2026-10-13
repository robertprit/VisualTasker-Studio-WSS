package com.visualtasker.wss.emscript.runtime

import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.VariableDefinition
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.WorkflowRootId

enum class VariableStoragePolicy {
    VOLATILE,
    SESSION,
    PROJECT,
    APP_SHARED,
    EXTERNAL,
}

data class VariableBinding(
    val variableId: String,
    val variableLabel: String,
    val declaredType: String,
    val scope: VariableScope,
    val storagePolicy: VariableStoragePolicy,
    val projectId: String? = null,
    val workflowRootId: String? = null,
    val providerId: String? = null,
    val defaultValue: String? = null,
)

enum class VariableOperation { DECLARE, RESOLVE, READ, WRITE }

enum class VariableFailureCode {
    MISSING_VARIABLE,
    AMBIGUOUS_LABEL,
    SCOPE_CONTEXT_MISSING,
    PROVIDER_UNAVAILABLE,
    READ_UNAVAILABLE,
    WRITE_UNAVAILABLE,
    TYPE_MISMATCH,
    REVISION_CONFLICT,
    CANCELLED,
    STORAGE_FAILURE,
    ALREADY_DECLARED,
}

data class VariableFailure(
    val code: VariableFailureCode,
    val message: String,
    val variableId: String? = null,
    val scope: VariableScope? = null,
    val providerId: String? = null,
)

data class VariableTraceInfo(
    val variableId: String,
    val scope: String,
    val providerId: String,
    val operation: String,
    val status: String,
    val failureCode: String? = null,
)

sealed interface VariableResolution {
    data class Resolved(val binding: VariableBinding) : VariableResolution
    data class Failed(val failure: VariableFailure) : VariableResolution
}

data class VariableStoredValue(
    val value: ExecutionValue,
    val revision: Long,
)

sealed interface VariableReadResult {
    data class Found(val stored: VariableStoredValue) : VariableReadResult
    data object Missing : VariableReadResult
    data class Failed(val failure: VariableFailure) : VariableReadResult
}

sealed interface VariableWriteResult {
    data class Written(val stored: VariableStoredValue) : VariableWriteResult
    data class Failed(val failure: VariableFailure) : VariableWriteResult
}

interface VariableProvider {
    val id: String
    fun read(binding: VariableBinding, cancellationToken: CancellationToken): VariableReadResult
    fun write(
        binding: VariableBinding,
        value: ExecutionValue,
        expectedRevision: Long?,
        cancellationToken: CancellationToken,
    ): VariableWriteResult
}

class VariableProviderRegistry(providers: List<VariableProvider>) {
    private val providersById = providers.associateBy(VariableProvider::id).also { indexed ->
        require(indexed.size == providers.size) { "Duplicate variable provider IDs." }
    }

    fun provider(id: String): VariableProvider? = providersById[id]
}

class VariableScopeResolver(bindings: Collection<VariableBinding>) {
    private val byId = bindings.associateBy(VariableBinding::variableId).also { indexed ->
        require(indexed.size == bindings.size) { "Duplicate variable IDs in runtime bindings." }
    }
    private val byLabel = bindings.groupBy { it.variableLabel }

    fun resolve(reference: String): VariableResolution {
        byId[reference]?.let { return VariableResolution.Resolved(it) }
        return when (val candidates = byLabel[reference].orEmpty()) {
            emptyList<VariableBinding>() -> VariableResolution.Failed(
                VariableFailure(
                    VariableFailureCode.MISSING_VARIABLE,
                    "Variable '$reference' is not declared.",
                    variableId = reference,
                ),
            )
            else -> if (candidates.size == 1) {
                VariableResolution.Resolved(candidates.single())
            } else {
                VariableResolution.Failed(
                    VariableFailure(
                        VariableFailureCode.AMBIGUOUS_LABEL,
                        "Variable label '$reference' resolves to ${candidates.size} identities.",
                        variableId = reference,
                    ),
                )
            }
        }
    }
}

class VariableRuntimeSession internal constructor(
    private val resolver: VariableScopeResolver,
    private val providers: VariableProviderRegistry,
    initialValues: Map<String, ExecutionValue>,
    private val cancellationToken: CancellationToken,
) {
    private val localValues = linkedMapOf<String, VariableStoredValue>()
    private val scriptValues = initialValues.mapValuesTo(linkedMapOf()) { VariableStoredValue(it.value, 0L) }
    private val declaredInSession = mutableSetOf<String>()

    fun read(reference: String): VariableRuntimeResult {
        val binding = resolve(reference) ?: return lastFailure(reference)
        val result = when (binding.storagePolicy) {
            VariableStoragePolicy.VOLATILE -> localValues[binding.variableId]?.let(VariableReadResult::Found) ?: VariableReadResult.Missing
            VariableStoragePolicy.SESSION -> scriptValues[binding.variableId]?.let(VariableReadResult::Found) ?: VariableReadResult.Missing
            else -> provider(binding)?.read(binding, cancellationToken)
                ?: unavailable(binding, VariableFailureCode.PROVIDER_UNAVAILABLE, "No variable provider '${binding.providerId}' is registered.")
        }
        return when (result) {
            is VariableReadResult.Found -> validateValue(binding, result.stored.value)?.let { failure ->
                VariableRuntimeResult.Failed(failure, trace(binding, VariableOperation.READ, "FAILED", failure.code))
            } ?: VariableRuntimeResult.Success(
                binding,
                result.stored.value,
                trace(binding, VariableOperation.READ, "FOUND"),
            )
            VariableReadResult.Missing -> VariableRuntimeResult.Missing(
                binding,
                trace(binding, VariableOperation.READ, "MISSING", VariableFailureCode.MISSING_VARIABLE),
            )
            is VariableReadResult.Failed -> VariableRuntimeResult.Failed(result.failure, trace(binding, VariableOperation.READ, "FAILED", result.failure.code))
        }
    }

    fun write(reference: String, value: ExecutionValue, declaration: Boolean): VariableRuntimeResult {
        val binding = resolve(reference) ?: return lastFailure(reference)
        if (cancellationToken.isCancellationRequested) {
            val failure = failure(binding, VariableFailureCode.CANCELLED, "Variable write was cancelled.")
            return VariableRuntimeResult.Failed(failure, trace(binding, VariableOperation.WRITE, "CANCELLED", failure.code))
        }
        validateValue(binding, value)?.let { failure ->
            return VariableRuntimeResult.Failed(failure, trace(binding, VariableOperation.WRITE, "FAILED", failure.code))
        }
        val current = readCurrent(binding)
        if (current is VariableReadResult.Failed) {
            return VariableRuntimeResult.Failed(current.failure, trace(binding, VariableOperation.WRITE, "FAILED", current.failure.code))
        }
        if (declaration && !declaredInSession.add(binding.variableId)) {
            val failure = failure(binding, VariableFailureCode.ALREADY_DECLARED, "Variable '${binding.variableId}' is already declared in this scope.")
            return VariableRuntimeResult.Failed(failure, trace(binding, VariableOperation.DECLARE, "FAILED", failure.code))
        }
        if (declaration && current is VariableReadResult.Found) {
            return VariableRuntimeResult.Success(
                binding,
                current.stored.value,
                trace(binding, VariableOperation.DECLARE, "PRESERVED"),
            )
        }
        if (!declaration && current === VariableReadResult.Missing) {
            val failure = failure(binding, VariableFailureCode.MISSING_VARIABLE, "SET requires an existing variable '${binding.variableId}'.")
            return VariableRuntimeResult.Failed(failure, trace(binding, VariableOperation.WRITE, "FAILED", failure.code))
        }
        val expectedRevision = (current as? VariableReadResult.Found)?.stored?.revision
        val written = when (binding.storagePolicy) {
            VariableStoragePolicy.VOLATILE -> writeMemory(localValues, binding.variableId, value, expectedRevision)
            VariableStoragePolicy.SESSION -> writeMemory(scriptValues, binding.variableId, value, expectedRevision)
            else -> provider(binding)?.write(binding, value, expectedRevision, cancellationToken)
                ?: VariableWriteResult.Failed(failure(binding, VariableFailureCode.PROVIDER_UNAVAILABLE, "No variable provider '${binding.providerId}' is registered."))
        }
        return when (written) {
            is VariableWriteResult.Written -> VariableRuntimeResult.Success(
                binding,
                written.stored.value,
                trace(binding, if (declaration) VariableOperation.DECLARE else VariableOperation.WRITE, "WRITTEN"),
            )
            is VariableWriteResult.Failed -> VariableRuntimeResult.Failed(
                written.failure,
                trace(binding, VariableOperation.WRITE, "FAILED", written.failure.code),
            )
        }
    }

    private var resolutionFailure: VariableFailure? = null

    private fun resolve(reference: String): VariableBinding? = when (val result = resolver.resolve(reference)) {
        is VariableResolution.Resolved -> result.binding
        is VariableResolution.Failed -> null.also { resolutionFailure = result.failure }
    }

    private fun lastFailure(reference: String): VariableRuntimeResult.Failed {
        val failure = resolutionFailure ?: VariableFailure(VariableFailureCode.MISSING_VARIABLE, "Variable '$reference' is not declared.", reference)
        resolutionFailure = null
        return VariableRuntimeResult.Failed(
            failure,
            VariableTraceInfo(reference, "UNRESOLVED", "none", VariableOperation.RESOLVE.name, "FAILED", failure.code.name),
        )
    }

    private fun readCurrent(binding: VariableBinding): VariableReadResult = when (binding.storagePolicy) {
        VariableStoragePolicy.VOLATILE -> localValues[binding.variableId]?.let(VariableReadResult::Found) ?: VariableReadResult.Missing
        VariableStoragePolicy.SESSION -> scriptValues[binding.variableId]?.let(VariableReadResult::Found) ?: VariableReadResult.Missing
        else -> provider(binding)?.read(binding, cancellationToken)
            ?: unavailable(binding, VariableFailureCode.PROVIDER_UNAVAILABLE, "No variable provider '${binding.providerId}' is registered.")
    }

    private fun provider(binding: VariableBinding): VariableProvider? = binding.providerId?.let(providers::provider)

    private fun writeMemory(
        target: MutableMap<String, VariableStoredValue>,
        variableId: String,
        value: ExecutionValue,
        expectedRevision: Long?,
    ): VariableWriteResult {
        val current = target[variableId]
        if (current?.revision != expectedRevision) {
            return VariableWriteResult.Failed(
                VariableFailure(VariableFailureCode.REVISION_CONFLICT, "Variable '$variableId' changed concurrently.", variableId),
            )
        }
        return VariableWriteResult.Written(VariableStoredValue(value, (current?.revision ?: -1L) + 1L).also { target[variableId] = it })
    }

    private fun validateValue(binding: VariableBinding, value: ExecutionValue): VariableFailure? {
        if (value === ExecutionValue.VoidValue) {
            return failure(binding, VariableFailureCode.TYPE_MISMATCH, "Void cannot be stored as a variable value.")
        }
        if (value === ExecutionValue.NullValue) {
            return if (binding.declaredType.endsWith("?") || binding.declaredType.equals("Any", true)) null
            else failure(binding, VariableFailureCode.TYPE_MISMATCH, "Null is not valid for ${binding.declaredType}.")
        }
        val base = binding.declaredType.removeSuffix("?").lowercase()
        val valid = base in setOf("", "any") || when (value) {
            is ExecutionValue.BooleanValue -> base in setOf("bool", "boolean")
            is ExecutionValue.NumberValue -> base == "number"
            is ExecutionValue.StringValue -> base == "string"
            is ExecutionValue.ListValue -> base.startsWith("list") || base == "array"
            is ExecutionValue.PointValue -> base == "point"
            is ExecutionValue.PathSegmentValue -> base == "pathsegment"
            is ExecutionValue.PathValue -> base == "path"
            is ExecutionValue.PointerPathValue -> base == "pointerpath"
            is ExecutionValue.MultiPathValue -> base in setOf("multipath", "multiswipe")
            ExecutionValue.NullValue, ExecutionValue.VoidValue -> false
        }
        return if (valid) null else failure(binding, VariableFailureCode.TYPE_MISMATCH, "${value::class.simpleName} is not compatible with ${binding.declaredType}.")
    }

    private fun failure(binding: VariableBinding, code: VariableFailureCode, message: String) =
        VariableFailure(code, message, binding.variableId, binding.scope, binding.providerId)

    private fun unavailable(binding: VariableBinding, code: VariableFailureCode, message: String) =
        VariableReadResult.Failed(failure(binding, code, message))

    private fun trace(binding: VariableBinding, operation: VariableOperation, status: String, failure: VariableFailureCode? = null) =
        VariableTraceInfo(binding.variableId, binding.scope.name, binding.providerId ?: "session", operation.name, status, failure?.name)
}

sealed interface VariableRuntimeResult {
    val trace: VariableTraceInfo

    data class Success(
        val binding: VariableBinding,
        val value: ExecutionValue,
        override val trace: VariableTraceInfo,
    ) : VariableRuntimeResult

    data class Missing(
        val binding: VariableBinding,
        override val trace: VariableTraceInfo,
    ) : VariableRuntimeResult

    data class Failed(
        val failure: VariableFailure,
        override val trace: VariableTraceInfo,
    ) : VariableRuntimeResult
}

fun interface VariableRuntimeSessionFactory {
    fun open(request: ExecutionRequest, cancellationToken: CancellationToken): VariableRuntimeSession
}

class DefaultVariableRuntimeSessionFactory(
    private val providers: VariableProviderRegistry,
) : VariableRuntimeSessionFactory {
    override fun open(request: ExecutionRequest, cancellationToken: CancellationToken): VariableRuntimeSession {
        val bindings = if (request.variableBindings.isNotEmpty()) {
            request.variableBindings.values
        } else {
            request.document.variables.variables.values.map { definition ->
                definition.toRuntimeBinding(request.projectId, request.workflowRootId, providerBinding = null)
            }
        }
        return VariableRuntimeSession(
            resolver = VariableScopeResolver(bindings),
            providers = providers,
            initialValues = request.initialVariables,
            cancellationToken = cancellationToken,
        )
    }
}

object ProjectVariableBindings {
    fun forExecution(project: ProjectDocument, rootId: WorkflowRootId): Map<String, VariableBinding> {
        val root = requireNotNull(project.roots[rootId]) { "Project root ${rootId.value} is missing." }
        val projectDefinitions = project.variables.mapValues { it.value.definition }
        return (root.workspace.variables.variables + projectDefinitions).mapValues { (id, definition) ->
            val projectVariable = project.variables[id]
            definition.toRuntimeBinding(
                projectId = project.id.value,
                workflowRootId = rootId.value,
                providerBinding = projectVariable?.providerBinding,
            )
        }
    }
}

private fun VariableDefinition.toRuntimeBinding(
    projectId: String?,
    workflowRootId: String?,
    providerBinding: String?,
): VariableBinding {
    val normalizedScope = if (scope == VariableScope.Global) VariableScope.Project else scope
    val policy = when (normalizedScope) {
        VariableScope.Local -> VariableStoragePolicy.VOLATILE
        VariableScope.Script -> VariableStoragePolicy.SESSION
        VariableScope.Project -> VariableStoragePolicy.PROJECT
        VariableScope.AppShared -> VariableStoragePolicy.APP_SHARED
        VariableScope.External -> VariableStoragePolicy.EXTERNAL
        VariableScope.Global -> error("Global scope was not normalized.")
    }
    val providerId = when (policy) {
        VariableStoragePolicy.PROJECT -> PROJECT_VARIABLE_PROVIDER_ID
        VariableStoragePolicy.APP_SHARED -> APP_SHARED_VARIABLE_PROVIDER_ID
        VariableStoragePolicy.EXTERNAL -> providerBinding
        else -> null
    }
    return VariableBinding(id, name, type, normalizedScope, policy, projectId, workflowRootId, providerId, defaultValue)
}

const val PROJECT_VARIABLE_PROVIDER_ID = "visualtasker.variable.project"
const val APP_SHARED_VARIABLE_PROVIDER_ID = "visualtasker.variable.app-shared"
const val TASKER_VARIABLE_PROVIDER_ID = "tasker.variable"
