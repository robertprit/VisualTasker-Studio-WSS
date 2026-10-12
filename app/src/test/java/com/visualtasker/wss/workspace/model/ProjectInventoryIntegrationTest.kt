package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.BlockNode
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.LegacyWorkspaceCanonicalizer
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectManifest
import de.visualtasker.workflow.core.ProjectOperation
import de.visualtasker.workflow.core.ProjectOperationDiagnosticCode
import de.visualtasker.workflow.core.ProjectOperationExecutor
import de.visualtasker.workflow.core.ProjectOperationId
import de.visualtasker.workflow.core.ProjectOperationResult
import de.visualtasker.workflow.core.ProjectReferenceTarget
import de.visualtasker.workflow.core.ProjectVariable
import de.visualtasker.workflow.core.ProjectWorkflowRoot
import de.visualtasker.workflow.core.ResourceId
import de.visualtasker.workflow.core.SemanticRelation
import de.visualtasker.workflow.core.SemanticRelationId
import de.visualtasker.workflow.core.SemanticRelationKind
import de.visualtasker.workflow.core.SemanticRelationRole
import de.visualtasker.workflow.core.SemanticRelationRoleKind
import de.visualtasker.workflow.core.VariableDefinition
import de.visualtasker.workflow.core.VariableRegistry
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.WorkflowRootId
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.serialization.ProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectInventoryIntegrationTest {
    @Test
    fun workspaceResourcePayloadRoundtripsWithoutLosingMarkerMetadata() {
        val resource = markerResource("marker:login")

        val projectResource = ProjectResourceBridge.toProjectResource(resource)
        val restored = ProjectResourceBridge.toWorkspaceResource(projectResource)

        assertEquals(resource, restored)
        assertEquals("inline:workspace-resource:${resource.id}", projectResource.location)
        assertTrue(ProjectResourceBridge.isInline(projectResource))
    }

    @Test
    fun legacyInventoryIsImportedExactlyOnceAndSurvivesSaveReload() {
        val original = project(workspace())
        val legacy = WorkspaceResourceBundle(resources = listOf(markerResource("marker:first")))

        val imported = ProjectInventoryIntegrator.integrate(original, legacy)
        val second = ProjectInventoryIntegrator.integrate(
            imported.project,
            WorkspaceResourceBundle(resources = listOf(markerResource("marker:late"))),
        )
        val restored = ProjectSerializer.deserialize(ProjectSerializer.serialize(imported.project))

        assertTrue(imported.changed)
        assertEquals("1", imported.project.metadata[PROJECT_INVENTORY_VERSION_KEY])
        assertNotNull(imported.project.resources[ResourceId("marker:first")])
        assertFalse(second.changed)
        assertFalse(ResourceId("marker:late") in second.project.resources)
        assertEquals(imported.project, restored)
    }

    @Test
    fun sharedScopesMoveIntoProjectWhileRuntimeScopesRemainRootOwned() {
        val workspace = workspace(
            variables = listOf(
                variable("local", VariableScope.Local),
                variable("script", VariableScope.Script),
                variable("project", VariableScope.Project),
                variable("shared", VariableScope.AppShared),
                variable("external", VariableScope.External),
            ),
        )

        val result = ProjectInventoryIntegrator.integrate(project(workspace))

        assertEquals(setOf("project", "shared"), result.project.variables.keys)
        assertEquals(setOf("local", "script", "project", "shared", "external"), result.project.activeRoot()!!.workspace.variables.variables.keys)
        assertTrue(result.diagnostics.any {
            it.code == ProjectInventoryDiagnosticCode.ExternalVariableProviderMissing && it.variableId == "external"
        })
        assertFalse("external" in result.project.variables)
    }

    @Test
    fun boundExternalVariableIsPreservedAndExposedWithoutInventingProviderState() {
        val external = variable("external", VariableScope.External)
        val original = project(workspace(variables = listOf(external))).copy(
            variables = mapOf(
                external.id to ProjectVariable(external, providerBinding = "tasker.variable"),
            ),
        )

        val result = ProjectInventoryIntegrator.integrate(original)
        val item = ProjectExplorerProjector.project(result.project).item("variable:external")

        assertEquals("tasker.variable", result.project.variables.getValue("external").providerBinding)
        assertTrue(item?.detail.orEmpty().contains("tasker.variable"))
        assertTrue(item?.detail.orEmpty().contains("Provider"))
    }

    @Test
    fun explicitResourceAndVariableReferencesProduceStableWhereUsedEntries() {
        val workspace = workspace(
            fields = mapOf("templateId" to FieldValue.Text("template:login")),
            variables = listOf(variable("project", VariableScope.Project)),
            addVariableReference = true,
        )
        val legacy = WorkspaceResourceBundle(resources = listOf(templateResource("template:login")))

        val first = ProjectInventoryIntegrator.integrate(project(workspace), legacy)
        val second = ProjectInventoryIntegrator.integrate(first.project)
        val resourceReferences = first.project.references.values.filter {
            (it.target as? ProjectReferenceTarget.Resource)?.resourceId == ResourceId("template:login")
        }
        val variableReferences = first.project.references.values.filter {
            (it.target as? ProjectReferenceTarget.Variable)?.variableId == "project"
        }

        assertEquals(1, resourceReferences.size)
        assertEquals(1, variableReferences.size)
        assertFalse(second.changed)
        assertEquals(first.project.references.keys, second.project.references.keys)
        assertEquals(1, ProjectExplorerProjector.project(first.project).item("resource:template:login")?.whereUsedCount)
    }

    @Test
    fun missingExplicitResourceBecomesDiagnosticWithoutInvalidProjectReference() {
        val workspace = workspace(fields = mapOf("templateId" to FieldValue.Text("template:missing")))

        val result = ProjectInventoryIntegrator.integrate(project(workspace))

        assertTrue(result.diagnostics.any {
            it.code == ProjectInventoryDiagnosticCode.ExplicitResourceReferenceMissing && it.resourceId == "template:missing"
        })
        assertTrue(result.project.references.isEmpty())
    }

    @Test
    fun referencedImportedResourceCannotBeDeletedAndRollbackIsExact() {
        val workspace = workspace(fields = mapOf("templateId" to FieldValue.Text("template:login")))
        val integrated = ProjectInventoryIntegrator.integrate(
            project(workspace),
            WorkspaceResourceBundle(resources = listOf(templateResource("template:login"))),
        ).project

        val result = ProjectOperationExecutor.execute(
            integrated,
            ProjectOperation.RemoveResource(op("remove"), ResourceId("template:login")),
        ) as ProjectOperationResult.Failure

        assertEquals(ProjectOperationDiagnosticCode.ReferencedItem, result.diagnostic.code)
        assertSame(integrated, result.project)
    }

    private fun project(workspace: WorkspaceDocument): ProjectDocument = ProjectDocument(
        id = ProjectId("project:inventory"),
        manifest = ProjectManifest("Inventory", ROOT),
        roots = mapOf(ROOT to ProjectWorkflowRoot(ROOT, "Main", workspace)),
    )

    private fun workspace(
        fields: Map<String, FieldValue> = emptyMap(),
        variables: List<VariableDefinition> = emptyList(),
        addVariableReference: Boolean = false,
    ): WorkspaceDocument {
        val blockId = BlockId("start")
        val legacy = WorkspaceDocument(
            id = "workspace:inventory",
            blocks = mapOf(blockId to BlockNode(blockId, "test", fields = fields)),
            rootBlocks = listOf(blockId),
            variables = VariableRegistry(variables.associateBy { it.id }),
        )
        val canonical = LegacyWorkspaceCanonicalizer.canonicalize(legacy).document
        val withReference = if (addVariableReference) {
            val statement = canonical.entities.single { it.legacySourceId == blockId.value }.ref
            val variable = canonical.entities.single { it.ref.id.value == "variable:project" }.ref
            canonical.copy(
                relations = canonical.relations + SemanticRelation(
                    id = SemanticRelationId("reference:start:project"),
                    kind = SemanticRelationKind.Reference,
                    source = statement,
                    target = variable,
                    role = SemanticRelationRole(SemanticRelationRoleKind.Variable),
                ),
            )
        } else {
            canonical
        }
        return legacy.copy(canonical = withReference)
    }

    private fun variable(id: String, scope: VariableScope) =
        VariableDefinition(id, id.replaceFirstChar(Char::uppercase), "String", scope)

    private fun markerResource(id: String) = WorkspaceResource(
        id = id,
        kind = WorkspaceResourceKind.Marker,
        label = "Login point",
        pluginOwner = "visualtasker.canvas",
        markerMode = WorkspaceMarkerMode.Point,
        point = WorkspacePointBounds(0.4f, 0.6f),
        referenceWidthPx = 1080,
        referenceHeightPx = 2340,
        metadata = mapOf("source" to "canvas", "opaque" to "kept"),
    )

    private fun templateResource(id: String) = WorkspaceResource(
        id = id,
        kind = WorkspaceResourceKind.Template,
        label = "Login template",
        pluginOwner = "visualtasker.canvas",
        region = WorkspaceRegionBounds(0.1f, 0.2f, 0.5f, 0.6f),
        referenceWidthPx = 1080,
        referenceHeightPx = 2340,
    )

    private fun op(value: String) = ProjectOperationId("inventory:$value")

    private companion object {
        val ROOT = WorkflowRootId("root:inventory")
    }
}
