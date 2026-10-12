package com.visualtasker.wss.emscript.runtime

import android.content.Context
import de.visualtasker.workflow.core.WorkspaceDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

class AndroidRuntimeHost(
    private val sessions: ExecutionSessionManager,
) {
    val currentSession: StateFlow<ExecutionSessionSnapshot?> = sessions.currentSession

    fun reserve(
        document: WorkspaceDocument,
        workflowId: String,
        workspaceRevision: Long,
        entryPointEntityId: String? = null,
        providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
        projectId: String? = null,
        workflowRootId: String? = null,
        projectRevision: Long? = null,
    ): ExecutionSessionId? = sessions.reserve(
        workflowId = workflowId,
        workspaceRevision = workspaceRevision,
        document = document,
        entryPointEntityId = entryPointEntityId,
        providerPolicies = providerPolicies,
        projectId = projectId,
        workflowRootId = workflowRootId,
        projectRevision = projectRevision,
    )

    fun startReserved(sessionId: ExecutionSessionId): Boolean = sessions.startReserved(sessionId)

    fun stop(sessionId: ExecutionSessionId, reason: ExecutionCancellationReason): Boolean =
        sessions.stop(sessionId, reason)

    fun stopCurrent(reason: ExecutionCancellationReason): Boolean = sessions.stopCurrent(reason)

    fun session(sessionId: ExecutionSessionId): ExecutionSessionSnapshot? = sessions.session(sessionId)
}

object RuntimeProcessHost {
    @Volatile
    private var instance: AndroidRuntimeHost? = null

    fun initialize(context: Context): AndroidRuntimeHost = instance ?: synchronized(this) {
        instance ?: create(context.applicationContext).also { instance = it }
    }

    fun get(context: Context): AndroidRuntimeHost = initialize(context)

    private fun create(context: Context): AndroidRuntimeHost {
        val planner = WorkflowExecutionPlanner()
        val providers = ExecutionProviderRegistry(
            listOf(
                AndroidWaitExecutionProvider(),
                NativeAccessibilityProvider(),
            ),
        )
        val runner = DeterministicExecutionRunner(planner = planner, providers = providers)
        return AndroidRuntimeHost(
            ExecutionSessionManager(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
                runner = runner,
                planner = planner,
                checkpointStore = AndroidExecutionSessionStore(context),
            ),
        )
    }
}
