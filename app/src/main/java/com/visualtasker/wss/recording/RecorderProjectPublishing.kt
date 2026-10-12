package com.visualtasker.wss.recording

import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectOperation
import de.visualtasker.workflow.core.ProjectOperationExecutor
import de.visualtasker.workflow.core.ProjectOperationId
import de.visualtasker.workflow.core.ProjectOperationResult
import de.visualtasker.workflow.core.ProjectTransaction
import de.visualtasker.workflow.core.WorkflowRootId

data class RecorderProjectPublishPreview(
    val rootId: WorkflowRootId,
    val rootName: String,
    val projectRevision: Long,
    val workspacePreview: RecorderPublishPreview,
    val diagnostics: List<RecorderPublishDiagnostic>,
) {
    val canPublish: Boolean get() = workspacePreview.canPublish && diagnostics.none { it.severity == RecorderPublishDiagnosticSeverity.ERROR }
}

sealed interface RecorderProjectPublishResult {
    data class Success(
        val project: ProjectDocument,
        val rootId: WorkflowRootId,
        val publishedEntityIds: List<de.visualtasker.workflow.core.SemanticEntityId>,
    ) : RecorderProjectPublishResult

    data class AlreadyPublished(val rootId: WorkflowRootId) : RecorderProjectPublishResult
    data class Failure(val diagnostics: List<RecorderPublishDiagnostic>) : RecorderProjectPublishResult
}

class RecorderProjectWorkflowPublisher(
    private val workspacePublisher: RecorderWorkflowPublisher = RecorderWorkflowPublisher(),
) {
    fun preview(
        request: RecorderPublishRequest,
        candidates: StepCandidateDocument,
        reviewed: ReviewedStepDocument,
        project: ProjectDocument,
    ): RecorderProjectPublishPreview {
        val diagnostics = validateProjectTarget(request, project).toMutableList()
        val rootId = request.targetWorkflowRootId?.let(::WorkflowRootId)
        val root = rootId?.let(project.roots::get)
        val workspacePreview = if (root != null && diagnostics.none { it.severity == RecorderPublishDiagnosticSeverity.ERROR }) {
            workspacePublisher.preview(request, candidates, reviewed, root.workspace)
        } else {
            RecorderPublishPreview(request, "invalid-project-target", null, diagnostics)
        }
        return RecorderProjectPublishPreview(
            rootId = rootId ?: WorkflowRootId("invalid"),
            rootName = root?.name.orEmpty(),
            projectRevision = project.revision.value,
            workspacePreview = workspacePreview,
            diagnostics = (diagnostics + workspacePreview.diagnostics).distinctBy { it.code to it.candidateId },
        )
    }

    fun apply(
        request: RecorderPublishRequest,
        preview: RecorderProjectPublishPreview,
        project: ProjectDocument,
    ): RecorderProjectPublishResult {
        val validation = validateProjectTarget(request, project)
        if (validation.any { it.severity == RecorderPublishDiagnosticSeverity.ERROR }) return RecorderProjectPublishResult.Failure(validation)
        val root = project.roots[preview.rootId] ?: return RecorderProjectPublishResult.Failure(listOf(error("PUBLISH_PROJECT_ROOT_NOT_FOUND", "Der Ziel-Root ist nicht vorhanden.")))
        return when (val workspaceResult = workspacePublisher.apply(request, preview.workspacePreview, root.workspace)) {
            is RecorderPublishResult.Failure -> RecorderProjectPublishResult.Failure(workspaceResult.diagnostics)
            is RecorderPublishResult.AlreadyPublished -> RecorderProjectPublishResult.AlreadyPublished(preview.rootId)
            is RecorderPublishResult.Success -> {
                val transactionId = ProjectOperationId("recorder-project:${workspaceResult.transactionId}")
                val projectResult = ProjectOperationExecutor.execute(
                    project,
                    ProjectTransaction(
                        transactionId = transactionId,
                        expectedRevision = project.revision,
                        operations = listOf(
                            ProjectOperation.ReplaceRootWorkspace(
                                operationId = transactionId,
                                rootId = preview.rootId,
                                expectedWorkspaceRevision = root.workspace.version,
                                workspace = workspaceResult.document,
                            ),
                        ),
                    ),
                )
                when (projectResult) {
                    is ProjectOperationResult.Success -> RecorderProjectPublishResult.Success(projectResult.project, preview.rootId, workspaceResult.publishedEntityIds)
                    is ProjectOperationResult.Failure -> RecorderProjectPublishResult.Failure(listOf(error("PUBLISH_PROJECT_TRANSACTION_FAILED", projectResult.diagnostic.message)))
                }
            }
        }
    }

    private fun validateProjectTarget(request: RecorderPublishRequest, project: ProjectDocument): List<RecorderPublishDiagnostic> = buildList {
        if (request.targetProjectId != project.id.value) add(error("PUBLISH_PROJECT_MISMATCH", "Das Zielprojekt stimmt nicht ueberein."))
        if (request.targetProjectRevision != project.revision.value) add(error("PUBLISH_PROJECT_REVISION_CONFLICT", "Die erwartete Projektrevision ist veraltet."))
        val rootId = request.targetWorkflowRootId?.let(::WorkflowRootId)
        val root = rootId?.let(project.roots::get)
        if (root == null) add(error("PUBLISH_PROJECT_ROOT_NOT_FOUND", "Der Ziel-Root ist nicht vorhanden."))
        else if (request.targetWorkspaceId != root.workspace.id) add(error("PUBLISH_ROOT_WORKSPACE_MISMATCH", "Der Ziel-Workspace gehoert nicht zum angegebenen Root."))
    }

    private fun error(code: String, message: String) = RecorderPublishDiagnostic(code, message, RecorderPublishDiagnosticSeverity.ERROR)
}
