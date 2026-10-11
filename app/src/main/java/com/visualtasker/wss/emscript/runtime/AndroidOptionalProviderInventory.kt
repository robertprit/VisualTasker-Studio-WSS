package com.visualtasker.wss.emscript.runtime

import android.content.Context
import com.visualtasker.wss.workspace.plugin.runtime.CUSTOM_CHROME_TAB_PLUGIN_OWNER
import com.visualtasker.wss.workspace.plugin.runtime.CustomChromeTabRegistration
import com.visualtasker.wss.workspace.plugin.runtime.CustomChromeTabStatus
import com.visualtasker.wss.workspace.plugin.runtime.SHIZUKU_PERMISSION
import com.visualtasker.wss.workspace.plugin.runtime.SHIZUKU_PLUGIN_OWNER
import com.visualtasker.wss.workspace.plugin.runtime.ShizukuRegistration
import com.visualtasker.wss.workspace.plugin.runtime.ShizukuRegistrationStatus
import com.visualtasker.wss.workspace.plugin.runtime.TASKER_PLUGIN_OWNER
import com.visualtasker.wss.workspace.plugin.runtime.TaskerRegistration
import com.visualtasker.wss.workspace.plugin.runtime.TaskerRegistrationStatus
import com.visualtasker.wss.workspace.plugin.runtime.TERMUX_PLUGIN_OWNER
import com.visualtasker.wss.workspace.plugin.runtime.TERMUX_RUN_COMMAND_PERMISSION
import com.visualtasker.wss.workspace.plugin.runtime.TermuxRegistration
import com.visualtasker.wss.workspace.plugin.runtime.TermuxRegistrationStatus
import de.visualtasker.emscript.contract.ProviderId
import de.visualtasker.workflow.semantics.CommandCapability

data class OptionalProviderInventoryEntry(
    val descriptor: ProviderDescriptor,
    val availability: ProviderAvailability,
    val supportedCommandFamilies: Set<String>,
    val executionStatus: String,
    val gap: String? = null,
)

data class OptionalProviderStatusSnapshot(
    val tasker: TaskerRegistrationStatus,
    val shizuku: ShizukuRegistrationStatus,
    val termux: TermuxRegistrationStatus,
    val customTabs: CustomChromeTabStatus,
)

object AndroidOptionalProviderInventory {
    fun inspect(context: Context): List<OptionalProviderInventoryEntry> = fromSnapshot(
        OptionalProviderStatusSnapshot(
            tasker = TaskerRegistration.inspect(context),
            shizuku = ShizukuRegistration.inspect(context),
            termux = TermuxRegistration.inspect(context),
            customTabs = CustomChromeTabRegistration.inspect(context),
        ),
    )

    fun fromSnapshot(snapshot: OptionalProviderStatusSnapshot): List<OptionalProviderInventoryEntry> = listOf(
        snapshot.tasker.toInventoryEntry(),
        snapshot.shizuku.toInventoryEntry(),
        snapshot.termux.toInventoryEntry(),
        snapshot.customTabs.toInventoryEntry(),
    ).sortedBy { it.descriptor.id.value }
}

private fun TaskerRegistrationStatus.toInventoryEntry(): OptionalProviderInventoryEntry =
    OptionalProviderInventoryEntry(
        descriptor = optionalDescriptor(
            id = TASKER_PLUGIN_OWNER,
            displayName = "Tasker",
            host = "Tasker broadcast host",
            capability = CommandCapability.TASKER,
            permissions = setOf(TaskerRegistration.PERMISSION_RUN_TASKS),
            sideEffect = ProviderSideEffect.EXTERNAL_ACTION,
        ),
        availability = ProviderAvailability(
            installation = if (installed) ProviderInstallationState.INSTALLED else ProviderInstallationState.NOT_INSTALLED,
            connection = if (receiverAvailable && taskerEnabled != false && externalAccessAllowed != false) {
                ProviderConnectionState.CONNECTED
            } else {
                ProviderConnectionState.DISCONNECTED
            },
            permission = if (runTaskPermissionGranted) ProviderPermissionState.GRANTED else ProviderPermissionState.DENIED,
            capability = if (installed && receiverAvailable) ProviderCapabilityState.AVAILABLE else ProviderCapabilityState.UNAVAILABLE,
            execution = if (available) ProviderExecutionState.EXECUTABLE else ProviderExecutionState.NOT_EXECUTABLE,
            diagnosticCode = if (available) null else "TASKER_UNAVAILABLE",
            message = summary,
        ),
        supportedCommandFamilies = setOf("tasker.run", "tasker.event", "tasker.variable", "tasker.profile"),
        executionStatus = "PARTIAL",
        gap = "Existing Tasker host is discoverable; callback/event completion is not yet an ExecutionProvider result contract.",
    )

private fun ShizukuRegistrationStatus.toInventoryEntry(): OptionalProviderInventoryEntry =
    OptionalProviderInventoryEntry(
        descriptor = optionalDescriptor(
            id = SHIZUKU_PLUGIN_OWNER,
            displayName = "Shizuku",
            host = "Shizuku binder",
            capability = CommandCapability.SHIZUKU,
            permissions = setOf(SHIZUKU_PERMISSION),
            sideEffect = ProviderSideEffect.PROCESS,
        ),
        availability = ProviderAvailability(
            installation = if (installed) ProviderInstallationState.INSTALLED else ProviderInstallationState.NOT_INSTALLED,
            connection = if (binderAlive) ProviderConnectionState.CONNECTED else ProviderConnectionState.DISCONNECTED,
            permission = if (permissionGranted) ProviderPermissionState.GRANTED else ProviderPermissionState.DENIED,
            capability = if (installed) ProviderCapabilityState.AVAILABLE else ProviderCapabilityState.UNAVAILABLE,
            execution = if (available) ProviderExecutionState.EXECUTABLE else ProviderExecutionState.NOT_EXECUTABLE,
            diagnosticCode = if (available) null else "SHIZUKU_UNAVAILABLE",
            message = summary,
        ),
        supportedCommandFamilies = setOf("shizuku.status", "shizuku.userService", "shizuku.shell"),
        executionStatus = "PARTIAL",
        gap = "The suspend host has no bounded ExecutionProvider timeout/result adapter yet; permission is never escalated automatically.",
    )

private fun TermuxRegistrationStatus.toInventoryEntry(): OptionalProviderInventoryEntry =
    OptionalProviderInventoryEntry(
        descriptor = optionalDescriptor(
            id = TERMUX_PLUGIN_OWNER,
            displayName = "Termux",
            host = "Termux RunCommandService",
            capability = CommandCapability.TERMUX,
            permissions = setOf(TERMUX_RUN_COMMAND_PERMISSION),
            sideEffect = ProviderSideEffect.PROCESS,
        ),
        availability = ProviderAvailability(
            installation = if (installed) ProviderInstallationState.INSTALLED else ProviderInstallationState.NOT_INSTALLED,
            connection = if (installed) ProviderConnectionState.CONNECTED else ProviderConnectionState.DISCONNECTED,
            permission = if (runCommandPermissionGranted) ProviderPermissionState.GRANTED else ProviderPermissionState.DENIED,
            capability = if (installed) ProviderCapabilityState.AVAILABLE else ProviderCapabilityState.UNAVAILABLE,
            execution = if (canRunCommands) ProviderExecutionState.EXECUTABLE else ProviderExecutionState.NOT_EXECUTABLE,
            diagnosticCode = if (canRunCommands) null else "TERMUX_UNAVAILABLE",
            message = summary,
        ),
        supportedCommandFamilies = setOf("termux.run", "termux.api", "termux.status"),
        executionStatus = "PARTIAL",
        gap = "Command acceptance exists, but completion, cancellation and stdout are not typed runtime results; raw shell text is not auto-bound.",
    )

private fun CustomChromeTabStatus.toInventoryEntry(): OptionalProviderInventoryEntry =
    OptionalProviderInventoryEntry(
        descriptor = optionalDescriptor(
            id = CUSTOM_CHROME_TAB_PLUGIN_OWNER,
            displayName = "Custom Tabs",
            host = "Android browser service",
            capability = CommandCapability.CUSTOM_TAB,
            sideEffect = ProviderSideEffect.EXTERNAL_ACTION,
        ),
        availability = ProviderAvailability(
            installation = if (supported) ProviderInstallationState.INSTALLED else ProviderInstallationState.NOT_INSTALLED,
            connection = if (supported) ProviderConnectionState.CONNECTED else ProviderConnectionState.DISCONNECTED,
            capability = if (supported) ProviderCapabilityState.AVAILABLE else ProviderCapabilityState.UNAVAILABLE,
            execution = if (supported) ProviderExecutionState.EXECUTABLE else ProviderExecutionState.NOT_EXECUTABLE,
            diagnosticCode = if (supported) null else "CUSTOM_TABS_UNAVAILABLE",
            message = summary,
        ),
        supportedCommandFamilies = setOf("chromeTab.open", "chromeTab.status"),
        executionStatus = "PARTIAL",
        gap = "Launch is available; navigation callbacks and asynchronous completion are not synchronous action results.",
    )

private fun optionalDescriptor(
    id: String,
    displayName: String,
    host: String,
    capability: CommandCapability,
    permissions: Set<String> = emptySet(),
    sideEffect: ProviderSideEffect,
): ProviderDescriptor {
    val runtimeCapability = ExecutionCapability(capability.name)
    return ProviderDescriptor(
        id = ProviderId(id),
        displayName = displayName,
        host = host,
        capabilities = setOf(
            CapabilityDescriptor(
                id = runtimeCapability.toCapabilityId(),
                runtimeCapability = runtimeCapability,
                inputTypes = listOf(ProviderValueType.ANY),
                resultType = ProviderValueType.ANY,
                sideEffect = sideEffect,
            ),
        ),
        requirements = ProviderRequirements(
            permissions = permissions,
            requiresInstallation = true,
            requiresConnection = true,
        ),
        optional = true,
    )
}
