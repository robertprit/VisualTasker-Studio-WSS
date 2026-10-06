package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityGate
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityStatus
import de.visualtasker.workflow.semantics.VisualTaskerCommandCatalog

enum class WorkspacePreflightSeverity {
    INFO,
    WARNING,
    ERROR,
}

data class WorkspacePreflightDiagnostic(
    val code: String,
    val message: String,
    val severity: WorkspacePreflightSeverity,
    val sourceId: String? = null,
)

data class WorkspaceReleasePreflightReport(
    val diagnostics: List<WorkspacePreflightDiagnostic>,
    val catalogRuntimeCount: Int,
    val catalogLiveCount: Int,
    val catalogPlannedCount: Int,
    val catalogAdapterCount: Int,
) {
    val canRelease: Boolean
        get() = diagnostics.none { it.severity == WorkspacePreflightSeverity.ERROR }

    val errorCount: Int
        get() = diagnostics.count { it.severity == WorkspacePreflightSeverity.ERROR }

    val warningCount: Int
        get() = diagnostics.count { it.severity == WorkspacePreflightSeverity.WARNING }
}

object WorkspaceReleasePreflight {
    fun inspect(
        state: WorkspaceWorkflowState,
        runtimeGate: RuntimeCapabilityGate = RuntimeCapabilityGate(),
    ): WorkspaceReleasePreflightReport {
        val diagnostics = mutableListOf<WorkspacePreflightDiagnostic>()
        val sync = WorkspaceSyncGuard().inspect(state.serializedJson)
        if (!sync.isValid) {
            diagnostics += WorkspacePreflightDiagnostic(
                code = "WORKSPACE_SYNC_INVALID",
                message = sync.messages.joinToString(" | "),
                severity = WorkspacePreflightSeverity.ERROR,
            )
        }

        VisualTaskerCommandCatalog.validate().forEach { issue ->
            diagnostics += WorkspacePreflightDiagnostic(
                code = "COMMAND_CATALOG_${issue.code.name}",
                message = issue.message,
                severity = WorkspacePreflightSeverity.ERROR,
                sourceId = issue.entryId,
            )
        }

        val runtimeDescriptors = VisualTaskerCommandCatalog.capabilityDescriptors()
            .filter { it.dryRunBehavior != "none" }
        runtimeDescriptors
            .filter { it.requiredAdapter == null }
            .forEach { descriptor ->
                diagnostics += WorkspacePreflightDiagnostic(
                    code = "COMMAND_RUNTIME_CONTRACT_MISSING",
                    message = "${descriptor.canonicalName}: Runtime-Vertrag ohne Capability-Adapter.",
                    severity = WorkspacePreflightSeverity.ERROR,
                    sourceId = descriptor.id,
                )
            }
        runtimeDescriptors
            .filterNot { it.liveImplemented }
            .forEach { descriptor ->
                diagnostics += WorkspacePreflightDiagnostic(
                    code = "COMMAND_LIVE_NOT_IMPLEMENTED",
                    message = "${descriptor.canonicalName}: DryRun vorhanden, Live-Ausfuehrung noch nicht implementiert (${descriptor.diagnosticCode}).",
                    severity = WorkspacePreflightSeverity.WARNING,
                    sourceId = descriptor.id,
                )
            }

        runtimeDescriptors
            .filter { it.pluginOwner != "visualtasker.core" }
            .groupBy { it.pluginOwner }
            .forEach { (pluginOwner, descriptors) ->
                diagnostics += WorkspacePreflightDiagnostic(
                    code = "COMMAND_PLUGIN_ADAPTER_DECLARED",
                    message = "$pluginOwner: ${descriptors.size} katalogisierte Runtime-Kommandos adaptergebunden.",
                    severity = WorkspacePreflightSeverity.INFO,
                    sourceId = pluginOwner,
                )
            }

        EmaVisualAssetIntegrity.inspect(state.resources).forEach { issue ->
            diagnostics += WorkspacePreflightDiagnostic(
                code = issue.code,
                message = issue.message,
                severity = when (issue.severity) {
                    EmaVisualAssetIssueSeverity.WARNING -> WorkspacePreflightSeverity.WARNING
                    EmaVisualAssetIssueSeverity.ERROR -> WorkspacePreflightSeverity.ERROR
                },
                sourceId = issue.resourceId,
            )
        }

        runtimeGate.inspect(state.document).capabilities
            .filter { it.status == RuntimeCapabilityStatus.BLOCKED }
            .forEach { capability ->
                diagnostics += WorkspacePreflightDiagnostic(
                    code = capability.diagnosticCode ?: "CAPABILITY_BLOCKED",
                    message = "${capability.command}: ${capability.details}",
                    severity = WorkspacePreflightSeverity.WARNING,
                    sourceId = capability.command,
                )
            }

        if (diagnostics.none { it.severity == WorkspacePreflightSeverity.ERROR }) {
            diagnostics += WorkspacePreflightDiagnostic(
                code = "RELEASE_PREFLIGHT_READY",
                message = "Workspace-Dokument, Command-Katalog und Ressourcen sind strukturell releasefaehig.",
                severity = WorkspacePreflightSeverity.INFO,
            )
        }
        return WorkspaceReleasePreflightReport(
            diagnostics = diagnostics.distinctBy { listOf(it.code, it.sourceId, it.message) },
            catalogRuntimeCount = runtimeDescriptors.size,
            catalogLiveCount = runtimeDescriptors.count { it.liveImplemented },
            catalogPlannedCount = runtimeDescriptors.count { !it.liveImplemented },
            catalogAdapterCount = runtimeDescriptors.count { it.pluginOwner != "visualtasker.core" },
        )
    }
}
