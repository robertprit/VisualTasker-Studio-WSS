package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.DefinitionId
import de.visualtasker.workflow.core.ProjectDefinitionKind
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectHistoryController
import de.visualtasker.workflow.core.ProjectOperation
import de.visualtasker.workflow.core.ProjectOperationExecutor
import de.visualtasker.workflow.core.ProjectOperationResult
import de.visualtasker.workflow.core.ProjectReferenceTarget
import de.visualtasker.workflow.core.ProjectResource
import de.visualtasker.workflow.core.ProjectResourceType
import de.visualtasker.workflow.core.ProjectValidator
import de.visualtasker.workflow.core.ResourceId
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.WorkflowRootId

enum class ProjectExplorerSectionKind {
    Workflows,
    Functions,
    Subworkflows,
    LocalAndScriptVariables,
    ProjectVariables,
    AppSharedVariables,
    ExternalVariables,
    ImagesAndTemplates,
    PointsAndRegions,
    Paths,
    Shapes,
    OtherResources,
    Diagnostics,
}

enum class ProjectExplorerItemKind { Workflow, Definition, Variable, Resource, Diagnostic }

data class ProjectExplorerItem(
    val key: String,
    val kind: ProjectExplorerItemKind,
    val stableId: String,
    val label: String,
    val detail: String,
    val active: Boolean = false,
    val missing: Boolean = false,
    val whereUsedCount: Int = 0,
    val enabled: Boolean = true,
)

data class ProjectExplorerSection(
    val kind: ProjectExplorerSectionKind,
    val label: String,
    val items: List<ProjectExplorerItem>,
)

data class ProjectExplorerProjection(
    val projectId: String,
    val projectName: String,
    val projectRevision: Long,
    val activeRootId: WorkflowRootId?,
    val sections: List<ProjectExplorerSection>,
) {
    val itemCount: Int get() = sections.sumOf { it.items.size }
    fun item(key: String?): ProjectExplorerItem? = sections.asSequence().flatMap { it.items.asSequence() }.firstOrNull { it.key == key }
}

data class ProjectExplorerUiState(
    val selectedItemKey: String? = null,
    val expandedSections: Set<ProjectExplorerSectionKind> = DEFAULT_EXPANDED_SECTIONS,
    val searchQuery: String = "",
) {
    companion object {
        val DEFAULT_EXPANDED_SECTIONS = setOf(
            ProjectExplorerSectionKind.Workflows,
            ProjectExplorerSectionKind.Functions,
            ProjectExplorerSectionKind.Subworkflows,
            ProjectExplorerSectionKind.ProjectVariables,
            ProjectExplorerSectionKind.ImagesAndTemplates,
            ProjectExplorerSectionKind.Diagnostics,
        )
    }
}

object ProjectExplorerProjector {
    fun project(
        project: ProjectDocument,
        resourceExists: (ProjectResource) -> Boolean = { it.location.isNotBlank() },
    ): ProjectExplorerProjection {
        val referencesByTarget = project.references.values.groupingBy { it.target.targetId }.eachCount()
        val sections = buildList {
            add(
                section(ProjectExplorerSectionKind.Workflows, "Workflows") {
                    project.roots.values.sortedBy { it.name.lowercase() }.map { root ->
                        ProjectExplorerItem(
                            key = "workflow:${root.id.value}",
                            kind = ProjectExplorerItemKind.Workflow,
                            stableId = root.id.value,
                            label = root.name,
                            detail = "${root.workspace.blocks.size} blocks · ${root.workspace.id}",
                            active = project.manifest.activeRootId == root.id,
                            whereUsedCount = project.references.values.count { it.source.ownerId == root.id.value },
                        )
                    }
                },
            )
            addDefinitionSection(project, ProjectDefinitionKind.Function, ProjectExplorerSectionKind.Functions, "Functions")
            addDefinitionSection(project, ProjectDefinitionKind.Subworkflow, ProjectExplorerSectionKind.Subworkflows, "Subworkflows")
            addVariableSection(project, setOf(VariableScope.Local, VariableScope.Script), ProjectExplorerSectionKind.LocalAndScriptVariables, "Local / Script")
            addVariableSection(project, setOf(VariableScope.Global, VariableScope.Project), ProjectExplorerSectionKind.ProjectVariables, "Project")
            addVariableSection(project, setOf(VariableScope.AppShared), ProjectExplorerSectionKind.AppSharedVariables, "App Shared")
            addVariableSection(project, setOf(VariableScope.External), ProjectExplorerSectionKind.ExternalVariables, "External")
            addResourceSection(project, setOf(ProjectResourceType.Image, ProjectResourceType.Template), ProjectExplorerSectionKind.ImagesAndTemplates, "Images / Templates", resourceExists, referencesByTarget)
            addResourceSection(project, setOf(ProjectResourceType.Point, ProjectResourceType.Region), ProjectExplorerSectionKind.PointsAndRegions, "Points / Regions", resourceExists, referencesByTarget)
            addResourceSection(project, setOf(ProjectResourceType.Path), ProjectExplorerSectionKind.Paths, "Paths", resourceExists, referencesByTarget)
            addResourceSection(project, setOf(ProjectResourceType.Shape), ProjectExplorerSectionKind.Shapes, "Shapes", resourceExists, referencesByTarget)
            addResourceSection(project, setOf(ProjectResourceType.Capture, ProjectResourceType.Observation, ProjectResourceType.Provider), ProjectExplorerSectionKind.OtherResources, "Other Assets", resourceExists, referencesByTarget)
            val diagnostics = ProjectValidator.validate(project).mapIndexed { index, issue ->
                ProjectExplorerItem(
                    key = "diagnostic:${issue.code}:$index",
                    kind = ProjectExplorerItemKind.Diagnostic,
                    stableId = issue.referenceId?.value ?: issue.rootId?.value ?: issue.code.name,
                    label = issue.code.name,
                    detail = issue.message,
                    missing = true,
                    enabled = false,
                )
            }
            add(ProjectExplorerSection(ProjectExplorerSectionKind.Diagnostics, "Diagnostics", diagnostics))
        }
        return ProjectExplorerProjection(
            projectId = project.id.value,
            projectName = project.manifest.name,
            projectRevision = project.revision.value,
            activeRootId = project.manifest.activeRootId,
            sections = sections,
        )
    }

    private fun MutableList<ProjectExplorerSection>.addDefinitionSection(
        project: ProjectDocument,
        definitionKind: ProjectDefinitionKind,
        sectionKind: ProjectExplorerSectionKind,
        label: String,
    ) {
        add(
            section(sectionKind, label) {
                project.definitions.values.filter { it.kind == definitionKind }.sortedBy { it.name.lowercase() }.map { definition ->
                    ProjectExplorerItem(
                        key = "definition:${definition.id.value}",
                        kind = ProjectExplorerItemKind.Definition,
                        stableId = definition.id.value,
                        label = definition.name,
                        detail = definition.kind.name,
                        whereUsedCount = project.whereUsed(definition.id).size,
                    )
                }
            },
        )
    }

    private fun MutableList<ProjectExplorerSection>.addVariableSection(
        project: ProjectDocument,
        scopes: Set<VariableScope>,
        sectionKind: ProjectExplorerSectionKind,
        label: String,
    ) {
        add(
            section(sectionKind, label) {
                project.variables.values.filter { it.definition.scope in scopes }.sortedBy { it.definition.name.lowercase() }.map { variable ->
                    ProjectExplorerItem(
                        key = "variable:${variable.definition.id}",
                        kind = ProjectExplorerItemKind.Variable,
                        stableId = variable.definition.id,
                        label = variable.definition.name,
                        detail = buildString {
                            append(variable.definition.type).append(" · ").append(variable.definition.scope.name)
                            variable.providerBinding?.let { append(" · ").append(it) }
                        },
                        whereUsedCount = project.references.values.count {
                            (it.target as? ProjectReferenceTarget.Variable)?.variableId == variable.definition.id
                        },
                    )
                }
            },
        )
    }

    private fun MutableList<ProjectExplorerSection>.addResourceSection(
        project: ProjectDocument,
        types: Set<ProjectResourceType>,
        sectionKind: ProjectExplorerSectionKind,
        label: String,
        resourceExists: (ProjectResource) -> Boolean,
        referencesByTarget: Map<String, Int>,
    ) {
        add(
            section(sectionKind, label) {
                project.resources.values.filter { it.type in types }.sortedBy { it.name.lowercase() }.map { resource ->
                    ProjectExplorerItem(
                        key = "resource:${resource.id.value}",
                        kind = ProjectExplorerItemKind.Resource,
                        stableId = resource.id.value,
                        label = resource.name,
                        detail = "${resource.type.name} · ${resource.scope.name} · ${resource.location}",
                        missing = !resourceExists(resource),
                        whereUsedCount = referencesByTarget[resource.id.value] ?: 0,
                    )
                }
            },
        )
    }

    private fun section(
        kind: ProjectExplorerSectionKind,
        label: String,
        items: () -> List<ProjectExplorerItem>,
    ) = ProjectExplorerSection(kind, label, items())
}

data class ProjectExplorerMutationContext(
    val activeExecutionRootIds: Set<WorkflowRootId> = emptySet(),
    val recorderTargetRootId: WorkflowRootId? = null,
)

enum class ProjectExplorerDiagnosticCode {
    ActiveExecution,
    RecorderTarget,
    InvalidName,
    OperationRejected,
    NothingToUndo,
    NothingToRedo,
}

data class ProjectExplorerDiagnostic(val code: ProjectExplorerDiagnosticCode, val message: String)

sealed interface ProjectExplorerMutationResult {
    data class Success(val project: ProjectDocument) : ProjectExplorerMutationResult
    data class Rejected(val project: ProjectDocument, val diagnostic: ProjectExplorerDiagnostic) : ProjectExplorerMutationResult
}

class ProjectExplorerController(
    private val history: ProjectHistoryController = ProjectHistoryController(),
) {
    val canUndo: Boolean get() = history.undoDepth > 0
    val canRedo: Boolean get() = history.redoDepth > 0

    fun recordAcceptedTransition(before: ProjectDocument, after: ProjectDocument): Boolean =
        history.recordAcceptedTransition(before, after)

    fun execute(
        project: ProjectDocument,
        operation: ProjectOperation,
        context: ProjectExplorerMutationContext = ProjectExplorerMutationContext(),
    ): ProjectExplorerMutationResult {
        guard(operation, context)?.let { return ProjectExplorerMutationResult.Rejected(project, it) }
        val result = ProjectOperationExecutor.execute(project, operation)
        return when (result) {
            is ProjectOperationResult.Success -> {
                history.recordAcceptedTransition(project, result.project)
                ProjectExplorerMutationResult.Success(result.project)
            }
            is ProjectOperationResult.Failure -> ProjectExplorerMutationResult.Rejected(
                project,
                ProjectExplorerDiagnostic(ProjectExplorerDiagnosticCode.OperationRejected, result.diagnostic.message),
            )
        }
    }

    fun undo(project: ProjectDocument): ProjectExplorerMutationResult = history.undo(project)
        ?.let { ProjectExplorerMutationResult.Success(it.project) }
        ?: ProjectExplorerMutationResult.Rejected(project, ProjectExplorerDiagnostic(ProjectExplorerDiagnosticCode.NothingToUndo, "No project change to undo."))

    fun redo(project: ProjectDocument): ProjectExplorerMutationResult = history.redo(project)
        ?.let { ProjectExplorerMutationResult.Success(it.project) }
        ?: ProjectExplorerMutationResult.Rejected(project, ProjectExplorerDiagnostic(ProjectExplorerDiagnosticCode.NothingToRedo, "No project change to redo."))

    private fun guard(
        operation: ProjectOperation,
        context: ProjectExplorerMutationContext,
    ): ProjectExplorerDiagnostic? = when (operation) {
        is ProjectOperation.DeleteRoot -> when (operation.rootId) {
            in context.activeExecutionRootIds -> ProjectExplorerDiagnostic(ProjectExplorerDiagnosticCode.ActiveExecution, "Workflow is used by an active execution session.")
            context.recorderTargetRootId -> ProjectExplorerDiagnostic(ProjectExplorerDiagnosticCode.RecorderTarget, "Workflow is the active recorder publish target.")
            else -> null
        }
        is ProjectOperation.RenameRoot -> if (operation.name.isBlank()) {
            ProjectExplorerDiagnostic(ProjectExplorerDiagnosticCode.InvalidName, "Workflow name must not be blank.")
        } else {
            null
        }
        else -> null
    }
}

fun ProjectExplorerItem.definitionIdOrNull(): DefinitionId? =
    stableId.takeIf { kind == ProjectExplorerItemKind.Definition }?.let(::DefinitionId)

fun ProjectExplorerItem.resourceIdOrNull(): ResourceId? =
    stableId.takeIf { kind == ProjectExplorerItemKind.Resource }?.let(::ResourceId)

fun ProjectExplorerItem.rootIdOrNull(): WorkflowRootId? =
    stableId.takeIf { kind == ProjectExplorerItemKind.Workflow }?.let(::WorkflowRootId)
