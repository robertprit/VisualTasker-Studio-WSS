package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.DefinitionId
import de.visualtasker.workflow.core.ProjectDefinition
import de.visualtasker.workflow.core.ProjectDefinitionKind
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectManifest
import de.visualtasker.workflow.core.ProjectOperation
import de.visualtasker.workflow.core.ProjectOperationId
import de.visualtasker.workflow.core.ProjectOperationExecutor
import de.visualtasker.workflow.core.ProjectOperationResult
import de.visualtasker.workflow.core.ProjectReference
import de.visualtasker.workflow.core.ProjectReferenceId
import de.visualtasker.workflow.core.ProjectReferenceKind
import de.visualtasker.workflow.core.ProjectReferenceSource
import de.visualtasker.workflow.core.ProjectReferenceTarget
import de.visualtasker.workflow.core.ProjectResource
import de.visualtasker.workflow.core.ProjectResourceScope
import de.visualtasker.workflow.core.ProjectResourceType
import de.visualtasker.workflow.core.ProjectRevision
import de.visualtasker.workflow.core.ProjectTransaction
import de.visualtasker.workflow.core.ProjectVariable
import de.visualtasker.workflow.core.ProjectWorkflowRoot
import de.visualtasker.workflow.core.ResourceId
import de.visualtasker.workflow.core.VariableDefinition
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.WorkflowRootId
import de.visualtasker.workflow.core.WorkspaceDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectExplorerTest {
    @Test
    fun projectionUsesProjectDocumentAsItsOnlySource() {
        val projection = ProjectExplorerProjector.project(project())

        assertEquals("project:explorer", projection.projectId)
        assertEquals(2, projection.sections.first { it.kind == ProjectExplorerSectionKind.Workflows }.items.size)
        assertEquals(project().roots.size + project().definitions.size + project().variables.size + project().resources.size, projection.itemCount)
    }

    @Test
    fun activeRootIsExplicitAndStable() {
        val item = ProjectExplorerProjector.project(project()).item("workflow:${ROOT_A.value}")

        assertTrue(requireNotNull(item).active)
        assertEquals(ROOT_A.value, item.stableId)
    }

    @Test
    fun definitionsAreGroupedAndWhereUsedIsVisible() {
        val projection = ProjectExplorerProjector.project(project())
        val function = projection.item("definition:${FUNCTION.value}")
        val subworkflow = projection.item("definition:${SUBWORKFLOW.value}")

        assertEquals(1, requireNotNull(function).whereUsedCount)
        assertNotNull(subworkflow)
        assertTrue(projection.sections.first { it.kind == ProjectExplorerSectionKind.Functions }.items.contains(function))
        assertTrue(projection.sections.first { it.kind == ProjectExplorerSectionKind.Subworkflows }.items.contains(subworkflow))
    }

    @Test
    fun variablesAreGroupedByScopeWithoutClaimingRuntimeSupport() {
        val projection = ProjectExplorerProjector.project(project())

        assertEquals(setOf("local", "script"), projection.section(ProjectExplorerSectionKind.LocalAndScriptVariables).map { it.stableId }.toSet())
        assertEquals(setOf("project"), projection.section(ProjectExplorerSectionKind.ProjectVariables).map { it.stableId }.toSet())
        assertEquals(setOf("shared"), projection.section(ProjectExplorerSectionKind.AppSharedVariables).map { it.stableId }.toSet())
        assertTrue(projection.item("variable:external")?.detail.orEmpty().contains("External"))
        assertTrue(projection.item("variable:external")?.detail.orEmpty().contains("tasker.variable"))
    }

    @Test
    fun resourcesAreGroupedByTypeAndExposeWhereUsed() {
        val projection = ProjectExplorerProjector.project(project())
        val image = projection.item("resource:${IMAGE.value}")

        assertEquals(1, requireNotNull(image).whereUsedCount)
        assertTrue(projection.sections.first { it.kind == ProjectExplorerSectionKind.ImagesAndTemplates }.items.contains(image))
        assertEquals(1, projection.section(ProjectExplorerSectionKind.Paths).size)
        assertEquals(1, projection.section(ProjectExplorerSectionKind.Shapes).size)
    }

    @Test
    fun missingResourceIsMarkedWithoutRemovingIt() {
        val projection = ProjectExplorerProjector.project(project()) { it.id != IMAGE }

        assertTrue(requireNotNull(projection.item("resource:${IMAGE.value}")).missing)
        assertEquals(IMAGE.value, projection.item("resource:${IMAGE.value}")?.stableId)
    }

    @Test
    fun invalidReferencesBecomeDiagnosticsInsteadOfCrashing() {
        val invalid = project().copy(
            references = project().references + (
                ProjectReferenceId("reference:bad") to ProjectReference(
                    ProjectReferenceId("reference:bad"),
                    ProjectReferenceSource.Root(ROOT_A),
                    ProjectReferenceTarget.Resource(ResourceId("resource:missing")),
                    ProjectReferenceKind.Resource,
                )
            ),
        )

        val diagnostics = ProjectExplorerProjector.project(invalid).section(ProjectExplorerSectionKind.Diagnostics)

        assertTrue(diagnostics.any { it.missing && it.detail.contains("missing") })
    }

    @Test
    fun createRenameOpenAndDeleteRootUseProjectOperations() {
        val controller = ProjectExplorerController()
        val createdRoot = ProjectWorkflowRoot(ROOT_C, "Third", WorkspaceDocument("workspace-c"))
        val created = success(controller.execute(project(), ProjectOperation.CreateRoot(op("create"), createdRoot)))
        val renamed = success(controller.execute(created, ProjectOperation.RenameRoot(op("rename"), ROOT_C, "Renamed")))
        val opened = success(controller.execute(renamed, ProjectOperation.SetActiveRoot(op("open"), ROOT_C)))
        val deleted = success(controller.execute(opened, ProjectOperation.DeleteRoot(op("delete"), ROOT_C)))

        assertEquals("Renamed", opened.roots.getValue(ROOT_C).name)
        assertEquals(ROOT_C, opened.manifest.activeRootId)
        assertFalse(ROOT_C in deleted.roots)
        assertEquals(ROOT_A, deleted.manifest.activeRootId)
    }

    @Test
    fun blankRootRenameIsRejectedBeforeMutation() {
        val original = project()
        val result = rejected(ProjectExplorerController().execute(original, ProjectOperation.RenameRoot(op("rename"), ROOT_A, "  ")))

        assertEquals(ProjectExplorerDiagnosticCode.InvalidName, result.diagnostic.code)
        assertSame(original, result.project)
    }

    @Test
    fun activeExecutionGuardsRootDeletion() {
        val original = project()
        val result = rejected(
            ProjectExplorerController().execute(
                original,
                ProjectOperation.DeleteRoot(op("delete"), ROOT_A),
                ProjectExplorerMutationContext(activeExecutionRootIds = setOf(ROOT_A)),
            ),
        )

        assertEquals(ProjectExplorerDiagnosticCode.ActiveExecution, result.diagnostic.code)
        assertEquals(original, result.project)
    }

    @Test
    fun recorderTargetGuardsRootDeletion() {
        val result = rejected(
            ProjectExplorerController().execute(
                project(),
                ProjectOperation.DeleteRoot(op("delete"), ROOT_B),
                ProjectExplorerMutationContext(recorderTargetRootId = ROOT_B),
            ),
        )

        assertEquals(ProjectExplorerDiagnosticCode.RecorderTarget, result.diagnostic.code)
    }

    @Test
    fun referencedDefinitionDeletionIsDiagnosed() {
        val result = rejected(ProjectExplorerController().execute(project(), ProjectOperation.DeleteDefinition(op("delete"), FUNCTION)))

        assertEquals(ProjectExplorerDiagnosticCode.OperationRejected, result.diagnostic.code)
        assertTrue(result.diagnostic.message.contains("referenced"))
    }

    @Test
    fun rootOwningReferencesCannotBeDeleted() {
        val result = rejected(ProjectExplorerController().execute(project(), ProjectOperation.DeleteRoot(op("delete-root"), ROOT_A)))

        assertEquals(ProjectExplorerDiagnosticCode.OperationRejected, result.diagnostic.code)
        assertTrue(result.diagnostic.message.contains("references"))
    }

    @Test
    fun referencedResourceDeletionIsDiagnosed() {
        val result = rejected(ProjectExplorerController().execute(project(), ProjectOperation.RemoveResource(op("delete"), IMAGE)))

        assertEquals(ProjectExplorerDiagnosticCode.OperationRejected, result.diagnostic.code)
        assertTrue(result.diagnostic.message.contains("referenced"))
    }

    @Test
    fun acceptedMutationAdvancesProjectRevisionOnce() {
        val original = project()
        val staleOperation = ProjectOperation.RenameRoot(op("rename"), ROOT_A, "Changed")
        val first = success(ProjectExplorerController().execute(original, staleOperation))

        assertEquals(original.revision.value + 1, first.revision.value)
    }

    @Test
    fun staleRevisionFailsAtomicallyWithoutEnteringExplorerHistory() {
        val original = project()
        val result = ProjectOperationExecutor.execute(
            original,
            ProjectTransaction(
                transactionId = op("stale"),
                operations = listOf(ProjectOperation.RenameRoot(op("stale-rename"), ROOT_A, "Changed")),
                expectedRevision = ProjectRevision(original.revision.value + 4),
            ),
        )

        assertTrue(result is ProjectOperationResult.Failure)
        assertEquals(original, (result as ProjectOperationResult.Failure).project)
        assertEquals("Main", original.roots.getValue(ROOT_A).name)
    }

    @Test
    fun rootNavigationNeverMutatesSiblingWorkspaces() {
        val original = project()
        val sibling = original.roots.getValue(ROOT_B).workspace
        val switched = success(ProjectExplorerController().execute(original, ProjectOperation.SetActiveRoot(op("switch-isolation"), ROOT_A)))
        val renamed = success(ProjectExplorerController().execute(switched, ProjectOperation.RenameRoot(op("rename-isolation"), ROOT_A, "Primary")))

        assertEquals(sibling, renamed.roots.getValue(ROOT_B).workspace)
        assertSame(original.roots.getValue(ROOT_A).workspace, renamed.roots.getValue(ROOT_A).workspace)
    }

    @Test
    fun undoAndRedoRestoreProjectAndActiveRoot() {
        val controller = ProjectExplorerController()
        val original = project()
        val switched = success(controller.execute(original, ProjectOperation.SetActiveRoot(op("switch"), ROOT_B)))
        val undone = success(controller.undo(switched))
        val redone = success(controller.redo(undone))

        assertEquals(ROOT_A, undone.manifest.activeRootId)
        assertEquals(switched, redone)
        assertTrue(controller.canUndo)
    }

    @Test
    fun emptyHistoryReturnsTypedDiagnostic() {
        val result = rejected(ProjectExplorerController().undo(project()))

        assertEquals(ProjectExplorerDiagnosticCode.NothingToUndo, result.diagnostic.code)
    }

    @Test
    fun projectActivationClearsUndoAndRedoHistory() {
        val controller = ProjectExplorerController()
        val original = project()
        val changed = success(controller.execute(original, ProjectOperation.RenameRoot(op("rename-before-import"), ROOT_A, "Changed")))
        assertTrue(controller.canUndo)

        controller.clearHistory()

        assertFalse(controller.canUndo)
        assertFalse(controller.canRedo)
        assertEquals(ProjectExplorerDiagnosticCode.NothingToUndo, rejected(controller.undo(changed)).diagnostic.code)
    }

    @Test
    fun uiStateDefaultsExposeRealCategoriesAndNoFakeItems() {
        val state = ProjectExplorerUiState()

        assertTrue(ProjectExplorerSectionKind.Workflows in state.expandedSections)
        assertTrue(ProjectExplorerSectionKind.Diagnostics in state.expandedSections)
        assertEquals(null, state.selectedItemKey)
    }

    private fun ProjectExplorerProjection.section(kind: ProjectExplorerSectionKind) = sections.first { it.kind == kind }.items

    private fun success(result: ProjectExplorerMutationResult) = (result as ProjectExplorerMutationResult.Success).project
    private fun rejected(result: ProjectExplorerMutationResult) = result as ProjectExplorerMutationResult.Rejected
    private fun op(value: String) = ProjectOperationId("explorer:$value")

    private fun project(): ProjectDocument {
        val function = ProjectDefinition(FUNCTION, "Login", ProjectDefinitionKind.Function)
        val subworkflow = ProjectDefinition(SUBWORKFLOW, "Recover", ProjectDefinitionKind.Subworkflow)
        val resources = listOf(
            ProjectResource(IMAGE, "Login screen", ProjectResourceType.Image, ProjectResourceScope.ProjectLocal, "images/login.png"),
            ProjectResource(ResourceId("resource:path"), "Gesture", ProjectResourceType.Path, ProjectResourceScope.ProjectLocal, "paths/gesture.json"),
            ProjectResource(ResourceId("resource:shape"), "Node", ProjectResourceType.Shape, ProjectResourceScope.Embedded, "shapes/node.ema"),
        ).associateBy { it.id }
        val definitions = listOf(function, subworkflow).associateBy { it.id }
        val variables = listOf(
            variable("local", VariableScope.Local),
            variable("script", VariableScope.Script),
            variable("project", VariableScope.Project),
            variable("shared", VariableScope.AppShared),
            variable("external", VariableScope.External, "tasker.variable"),
        ).associateBy { it.definition.id }
        val references = listOf(
            ProjectReference(ProjectReferenceId("reference:function"), ProjectReferenceSource.Root(ROOT_A), ProjectReferenceTarget.Definition(FUNCTION), ProjectReferenceKind.Call),
            ProjectReference(ProjectReferenceId("reference:image"), ProjectReferenceSource.Root(ROOT_A), ProjectReferenceTarget.Resource(IMAGE), ProjectReferenceKind.Resource),
        ).associateBy { it.id }
        return ProjectDocument(
            id = ProjectId("project:explorer"),
            manifest = ProjectManifest("Explorer", ROOT_A),
            roots = mapOf(
                ROOT_A to ProjectWorkflowRoot(ROOT_A, "Main", WorkspaceDocument("workspace-a")),
                ROOT_B to ProjectWorkflowRoot(ROOT_B, "Secondary", WorkspaceDocument("workspace-b")),
            ),
            definitions = definitions,
            variables = variables,
            resources = resources,
            references = references,
        )
    }

    private fun variable(id: String, scope: VariableScope, provider: String? = null) =
        ProjectVariable(VariableDefinition(id, id.replaceFirstChar(Char::uppercase), "String", scope), provider)

    private companion object {
        val ROOT_A = WorkflowRootId("root:a")
        val ROOT_B = WorkflowRootId("root:b")
        val ROOT_C = WorkflowRootId("root:c")
        val FUNCTION = DefinitionId("definition:login")
        val SUBWORKFLOW = DefinitionId("definition:recover")
        val IMAGE = ResourceId("resource:image")
    }
}
