package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectOperation
import de.visualtasker.workflow.core.ProjectOperationExecutor
import de.visualtasker.workflow.core.ProjectOperationId
import de.visualtasker.workflow.core.ProjectOperationResult
import de.visualtasker.workflow.core.WorkflowRootId

data class ProjectEditorRootState(
    val project: ProjectDocument,
    val rootId: WorkflowRootId,
    val workflow: WorkspaceWorkflowState,
    val selection: WorkspaceSelectionState,
)

sealed interface ProjectRootOpenResult {
    data class Opened(val state: ProjectEditorRootState) : ProjectRootOpenResult
    data class MissingRoot(val rootId: WorkflowRootId) : ProjectRootOpenResult
}

/** Owns active-root projection state without introducing another editor document. */
class ProjectEditorSession(initialProject: ProjectDocument) {
    private val selections = linkedMapOf<WorkflowRootId, WorkspaceSelectionState>()
    var project: ProjectDocument = initialProject
        private set

    var current: ProjectEditorRootState? = initialProject.manifest.activeRootId
        ?.let { openInitial(it) }
        private set

    fun openRoot(
        rootId: WorkflowRootId,
        outgoingSelection: WorkspaceSelectionState = current?.selection ?: WorkspaceSelectionState(),
    ): ProjectRootOpenResult {
        current?.let { selections[it.rootId] = outgoingSelection }
        val root = project.roots[rootId] ?: return ProjectRootOpenResult.MissingRoot(rootId)
        val operation = ProjectOperation.SetActiveRoot(
            operationId = ProjectOperationId("project:open:${project.revision.value}:${rootId.value}"),
            rootId = rootId,
        )
        val result = ProjectOperationExecutor.execute(project, operation)
        if (result !is ProjectOperationResult.Success) return ProjectRootOpenResult.MissingRoot(rootId)
        project = result.project
        return ProjectRootOpenResult.Opened(
            ProjectEditorRootState(
                project = project,
                rootId = rootId,
                workflow = WorkspaceWorkflowState.fromDocument(root.workspace, "project:root:${rootId.value}"),
                selection = selections[rootId] ?: WorkspaceSelectionState(source = "project:root:${rootId.value}"),
            ).also { current = it },
        )
    }

    fun updateSelection(selection: WorkspaceSelectionState) {
        val state = current ?: return
        selections[state.rootId] = selection
        current = state.copy(selection = selection)
    }

    private fun openInitial(rootId: WorkflowRootId): ProjectEditorRootState? {
        val root = project.roots[rootId] ?: return null
        return ProjectEditorRootState(
            project,
            rootId,
            WorkspaceWorkflowState.fromDocument(root.workspace, "project:root:${rootId.value}"),
            WorkspaceSelectionState(source = "project:root:${rootId.value}"),
        )
    }
}
