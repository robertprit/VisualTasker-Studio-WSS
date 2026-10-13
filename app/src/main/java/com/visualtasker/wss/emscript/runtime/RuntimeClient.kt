package com.visualtasker.wss.emscript.runtime

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import de.visualtasker.workflow.core.WorkspaceDocument
import kotlinx.coroutines.flow.StateFlow

class RuntimeClient(
    context: Context,
    private val host: AndroidRuntimeHost = RuntimeProcessHost.get(context),
) {
    private val appContext = context.applicationContext

    val session: StateFlow<ExecutionSessionSnapshot?> = host.currentSession

    fun start(
        document: WorkspaceDocument,
        workflowId: String,
        workspaceRevision: Long,
        entryPointEntityId: String? = null,
        providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
        projectId: String? = null,
        workflowRootId: String? = null,
        projectRevision: Long? = null,
        variableBindings: Map<String, VariableBinding> = emptyMap(),
    ): Boolean {
        val sessionId = host.reserve(
            document = document,
            workflowId = workflowId,
            workspaceRevision = workspaceRevision,
            entryPointEntityId = entryPointEntityId,
            providerPolicies = providerPolicies,
            projectId = projectId,
            workflowRootId = workflowRootId,
            projectRevision = projectRevision,
            variableBindings = variableBindings,
        ) ?: return false
        val intent = HeadlessRuntimeService.startIntent(appContext, sessionId)
        return runCatching {
            ContextCompat.startForegroundService(appContext, intent)
            true
        }.getOrElse {
            host.stop(sessionId, ExecutionCancellationReason.START_FAILURE)
            false
        }
    }

    fun stop(reason: ExecutionCancellationReason = ExecutionCancellationReason.USER_STOP): Boolean {
        val sessionId = session.value?.sessionId ?: return false
        val intent = HeadlessRuntimeService.stopIntent(appContext, sessionId, reason)
        return runCatching {
            appContext.startService(intent)
            true
        }.getOrElse {
            host.stop(sessionId, reason)
        }
    }

    fun session(sessionId: ExecutionSessionId): ExecutionSessionSnapshot? = host.session(sessionId)
}
