package com.visualtasker.wss.emscript.runtime

import de.visualtasker.emscript.contract.CapabilityId
import de.visualtasker.emscript.contract.ProviderId
import java.util.Locale

enum class ProviderRegistrationState {
    REGISTERED,
    NOT_REGISTERED,
}

enum class ProviderInstallationState {
    INSTALLED,
    NOT_INSTALLED,
    NOT_APPLICABLE,
    UNKNOWN,
}

enum class ProviderConnectionState {
    CONNECTED,
    DISCONNECTED,
    NOT_APPLICABLE,
    UNKNOWN,
}

enum class ProviderPermissionState {
    GRANTED,
    DENIED,
    NOT_REQUIRED,
    UNKNOWN,
}

enum class ProviderCapabilityState {
    AVAILABLE,
    UNAVAILABLE,
    UNSUPPORTED,
    UNKNOWN,
}

enum class ProviderExecutionState {
    EXECUTABLE,
    NOT_EXECUTABLE,
    UNKNOWN,
}

data class ProviderAvailability(
    val registration: ProviderRegistrationState = ProviderRegistrationState.REGISTERED,
    val installation: ProviderInstallationState = ProviderInstallationState.NOT_APPLICABLE,
    val connection: ProviderConnectionState = ProviderConnectionState.NOT_APPLICABLE,
    val permission: ProviderPermissionState = ProviderPermissionState.NOT_REQUIRED,
    val capability: ProviderCapabilityState = ProviderCapabilityState.AVAILABLE,
    val execution: ProviderExecutionState = ProviderExecutionState.EXECUTABLE,
    val diagnosticCode: String? = null,
    val message: String? = null,
) {
    val isExecutable: Boolean
        get() = registration == ProviderRegistrationState.REGISTERED &&
            installation !in setOf(ProviderInstallationState.NOT_INSTALLED, ProviderInstallationState.UNKNOWN) &&
            connection !in setOf(ProviderConnectionState.DISCONNECTED, ProviderConnectionState.UNKNOWN) &&
            permission !in setOf(ProviderPermissionState.DENIED, ProviderPermissionState.UNKNOWN) &&
            capability == ProviderCapabilityState.AVAILABLE &&
            execution == ProviderExecutionState.EXECUTABLE

    companion object {
        fun available(): ProviderAvailability = ProviderAvailability()
    }
}

data class ProviderRequirements(
    val permissions: Set<String> = emptySet(),
    val requiresInstallation: Boolean = false,
    val requiresConnection: Boolean = false,
    val notes: String? = null,
)

enum class ProviderValueType {
    VOID,
    BOOLEAN,
    NUMBER,
    STRING,
    LIST,
    POINT,
    PATH,
    POINTER_PATH,
    MULTI_PATH,
    ANY,
}

enum class ProviderSideEffect {
    NONE,
    DELAY,
    INPUT,
    EXTERNAL_ACTION,
    PROCESS,
}

data class CapabilityDescriptor(
    val id: CapabilityId,
    val runtimeCapability: ExecutionCapability,
    val inputTypes: List<ProviderValueType> = emptyList(),
    val resultType: ProviderValueType = ProviderValueType.VOID,
    val sideEffect: ProviderSideEffect = ProviderSideEffect.NONE,
)

data class ProviderDescriptor(
    val id: ProviderId,
    val displayName: String,
    val host: String,
    val capabilities: Set<CapabilityDescriptor>,
    val requirements: ProviderRequirements = ProviderRequirements(),
    val priority: Int = 0,
    val optional: Boolean = false,
)

data class ProviderDiscovery(
    val descriptor: ProviderDescriptor,
    val compatible: Boolean,
    val availability: ProviderAvailability,
)

enum class ProviderFallbackMode {
    DISABLED,
    SAFE_PRE_EXECUTION_ONLY,
}

data class ProviderSelectionPolicy(
    val explicitProviderId: ProviderId? = null,
    val excludedProviderIds: Set<ProviderId> = emptySet(),
    val preferredProviderIds: List<ProviderId> = emptyList(),
    val fallbackMode: ProviderFallbackMode = ProviderFallbackMode.DISABLED,
)

data class CapabilityRequest(
    val invocation: ProviderInvocation,
    val policy: ProviderSelectionPolicy = ProviderSelectionPolicy(),
)

enum class ProviderSelectionReason {
    EXPLICIT,
    PREFERRED,
    PRIORITY,
    STABLE_ID_TIE_BREAK,
}

enum class ProviderFailureClass {
    NOT_REGISTERED,
    NOT_INSTALLED,
    NOT_CONNECTED,
    PERMISSION_DENIED,
    CAPABILITY_UNAVAILABLE,
    INVALID_REQUEST,
    REJECTED,
    EXECUTION_FAILED,
    PROVIDER_LOST,
    TIMEOUT,
    CANCELLED,
    UNKNOWN_RESULT,
}

enum class ProviderEffectStatus {
    NOT_EXECUTED,
    EXECUTED,
    MAY_HAVE_EXECUTED,
    UNKNOWN,
}

data class ProviderFailure(
    val code: String,
    val message: String,
    val failureClass: ProviderFailureClass,
    val effectStatus: ProviderEffectStatus = ProviderEffectStatus.UNKNOWN,
    val providerId: ProviderId? = null,
) {
    val permitsSafeFallback: Boolean
        get() = effectStatus == ProviderEffectStatus.NOT_EXECUTED &&
            failureClass in setOf(
                ProviderFailureClass.NOT_INSTALLED,
                ProviderFailureClass.NOT_CONNECTED,
                ProviderFailureClass.PERMISSION_DENIED,
                ProviderFailureClass.CAPABILITY_UNAVAILABLE,
                ProviderFailureClass.REJECTED,
                ProviderFailureClass.PROVIDER_LOST,
            )
}

enum class ProviderFallbackDecision {
    NOT_REQUESTED,
    AVAILABLE,
    USED,
    BLOCKED_POLICY,
    BLOCKED_SIDE_EFFECT,
    NO_COMPATIBLE_PROVIDER,
}

data class ProviderResolutionTraceInfo(
    val capabilityId: String,
    val providerId: String? = null,
    val selectionReason: String,
    val registered: Boolean,
    val installed: String,
    val connected: String,
    val permissionState: String,
    val capabilityState: String,
    val executionStatus: String,
    val failureClass: String? = null,
    val fallbackDecision: String = ProviderFallbackDecision.NOT_REQUESTED.name,
    val cancelled: Boolean = false,
    val timedOut: Boolean = false,
    val resultStatus: String? = null,
)

sealed interface ProviderResolution {
    data class Selected(
        val provider: ExecutionProvider,
        val fallbackProviders: List<ExecutionProvider>,
        val reason: ProviderSelectionReason,
        val availability: ProviderAvailability,
    ) : ProviderResolution

    data class Rejected(
        val failure: ProviderFailure,
        val availability: ProviderAvailability? = null,
    ) : ProviderResolution
}

internal fun ExecutionCapability.toCapabilityId(): CapabilityId {
    val normalized = when {
        value.startsWith("capability.") -> value
        value.startsWith("command:") -> "capability.command.${value.removePrefix("command:").stableIdSegment()}"
        else -> "capability.${value.lowercase(Locale.ROOT).stableIdSegment()}"
    }
    return CapabilityId(normalized)
}

private fun String.stableIdSegment(): String =
    replace(Regex("[^A-Za-z0-9._-]+"), ".")
        .trim('.')
        .ifBlank { "unknown" }

internal fun ExecutionProvider.defaultDescriptor(): ProviderDescriptor = ProviderDescriptor(
    id = ProviderId(id),
    displayName = id,
    host = "runtime",
    capabilities = capabilities.mapTo(linkedSetOf()) { capability ->
        CapabilityDescriptor(
            id = capability.toCapabilityId(),
            runtimeCapability = capability,
        )
    },
)

internal fun ProviderAvailability.toFailure(providerId: ProviderId): ProviderFailure = when {
    registration != ProviderRegistrationState.REGISTERED -> ProviderFailure(
        code = diagnosticCode ?: "PROVIDER_NOT_REGISTERED",
        message = message ?: "Provider '${providerId.value}' is not registered.",
        failureClass = ProviderFailureClass.NOT_REGISTERED,
        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
        providerId = providerId,
    )
    installation == ProviderInstallationState.NOT_INSTALLED -> ProviderFailure(
        code = diagnosticCode ?: "PROVIDER_NOT_INSTALLED",
        message = message ?: "Provider '${providerId.value}' is not installed.",
        failureClass = ProviderFailureClass.NOT_INSTALLED,
        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
        providerId = providerId,
    )
    connection == ProviderConnectionState.DISCONNECTED -> ProviderFailure(
        code = diagnosticCode ?: "PROVIDER_NOT_CONNECTED",
        message = message ?: "Provider '${providerId.value}' is not connected.",
        failureClass = ProviderFailureClass.NOT_CONNECTED,
        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
        providerId = providerId,
    )
    permission == ProviderPermissionState.DENIED -> ProviderFailure(
        code = diagnosticCode ?: "PROVIDER_PERMISSION_DENIED",
        message = message ?: "Provider '${providerId.value}' lacks required permission.",
        failureClass = ProviderFailureClass.PERMISSION_DENIED,
        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
        providerId = providerId,
    )
    capability != ProviderCapabilityState.AVAILABLE -> ProviderFailure(
        code = diagnosticCode ?: "PROVIDER_CAPABILITY_UNAVAILABLE",
        message = message ?: "Provider '${providerId.value}' does not expose the requested capability.",
        failureClass = ProviderFailureClass.CAPABILITY_UNAVAILABLE,
        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
        providerId = providerId,
    )
    else -> ProviderFailure(
        code = diagnosticCode ?: "PROVIDER_NOT_EXECUTABLE",
        message = message ?: "Provider '${providerId.value}' is not executable.",
        failureClass = ProviderFailureClass.REJECTED,
        effectStatus = ProviderEffectStatus.NOT_EXECUTED,
        providerId = providerId,
    )
}

internal fun providerTrace(
    capability: ExecutionCapability,
    provider: ExecutionProvider?,
    reason: String,
    availability: ProviderAvailability?,
    failure: ProviderFailure? = null,
    fallback: ProviderFallbackDecision = ProviderFallbackDecision.NOT_REQUESTED,
    resultStatus: String? = null,
): ProviderResolutionTraceInfo = ProviderResolutionTraceInfo(
    capabilityId = capability.toCapabilityId().value,
    providerId = provider?.descriptor?.id?.value ?: failure?.providerId?.value,
    selectionReason = reason,
    registered = availability?.registration == ProviderRegistrationState.REGISTERED,
    installed = availability?.installation?.name ?: ProviderInstallationState.UNKNOWN.name,
    connected = availability?.connection?.name ?: ProviderConnectionState.UNKNOWN.name,
    permissionState = availability?.permission?.name ?: ProviderPermissionState.UNKNOWN.name,
    capabilityState = availability?.capability?.name ?: ProviderCapabilityState.UNKNOWN.name,
    executionStatus = availability?.execution?.name ?: ProviderExecutionState.UNKNOWN.name,
    failureClass = failure?.failureClass?.name,
    fallbackDecision = fallback.name,
    cancelled = failure?.failureClass == ProviderFailureClass.CANCELLED,
    timedOut = failure?.failureClass == ProviderFailureClass.TIMEOUT,
    resultStatus = resultStatus,
)
