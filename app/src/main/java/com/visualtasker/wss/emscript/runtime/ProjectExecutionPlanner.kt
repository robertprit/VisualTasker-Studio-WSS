package com.visualtasker.wss.emscript.runtime

import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.WorkflowRootId

/** Selects exactly one project root, then delegates to the existing workflow planner. */
class ProjectExecutionPlanner(
    private val workflowPlanner: WorkflowExecutionPlanner = WorkflowExecutionPlanner(),
) {
    fun plan(
        project: ProjectDocument,
        rootId: WorkflowRootId,
        requestId: String,
        entryPointEntityId: String? = null,
        initialVariables: Map<String, ExecutionValue> = emptyMap(),
        providerPolicies: Map<ExecutionCapability, ProviderSelectionPolicy> = emptyMap(),
    ): ExecutionPlanResult {
        val root = project.roots[rootId] ?: return ExecutionPlanResult.Rejected(
            listOf(
                ExecutionDiagnostic(
                    code = "PROJECT_ROOT_NOT_FOUND",
                    message = "Project ${project.id.value} has no workflow root ${rootId.value}.",
                ),
            ),
        )
        return workflowPlanner.plan(
            ExecutionRequest(
                id = requestId,
                workflowId = rootId.value,
                document = root.workspace,
                entryPointEntityId = entryPointEntityId,
                initialVariables = initialVariables,
                providerPolicies = providerPolicies,
                projectId = project.id.value,
                workflowRootId = rootId.value,
                projectRevision = project.revision.value,
                variableBindings = ProjectVariableBindings.forExecution(project, rootId),
            ),
        )
    }
}
