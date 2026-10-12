package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectOperation
import de.visualtasker.workflow.core.ProjectOperationExecutor
import de.visualtasker.workflow.core.ProjectOperationId
import de.visualtasker.workflow.core.ProjectOperationResult
import de.visualtasker.workflow.core.ProjectReference
import de.visualtasker.workflow.core.ProjectReferenceId
import de.visualtasker.workflow.core.ProjectReferenceKind
import de.visualtasker.workflow.core.ProjectReferenceSource
import de.visualtasker.workflow.core.ProjectReferenceTarget
import de.visualtasker.workflow.core.ProjectResource
import de.visualtasker.workflow.core.ProjectResourceScope
import de.visualtasker.workflow.core.ProjectResourceType
import de.visualtasker.workflow.core.ProjectTransaction
import de.visualtasker.workflow.core.ProjectVariable
import de.visualtasker.workflow.core.ResourceId
import de.visualtasker.workflow.core.SemanticEntityId
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.SemanticRelationKind
import de.visualtasker.workflow.core.SemanticRelationRoleKind
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.WorkflowRootId

const val PROJECT_INVENTORY_VERSION_KEY = "visualtasker.inventory.version"
const val PROJECT_INVENTORY_VERSION = "1"

private const val RESOURCE_PAYLOAD_KEY = "visualtasker.workspaceResource.payload"
private const val RESOURCE_OWNER_KEY = "visualtasker.workspaceResource.owner"
private const val MANAGED_REFERENCE_OWNER_KEY = "visualtasker.reference.owner"
private const val MANAGED_REFERENCE_OWNER = "m8-2-inventory"
private const val INLINE_LOCATION_PREFIX = "inline:workspace-resource:"

enum class ProjectInventoryDiagnosticCode {
    ConflictingVariableDefinition,
    ExternalVariableProviderMissing,
    ExplicitResourceReferenceMissing,
    ProjectTransactionRejected,
}

data class ProjectInventoryDiagnostic(
    val code: ProjectInventoryDiagnosticCode,
    val message: String,
    val rootId: WorkflowRootId? = null,
    val entityId: SemanticEntityId? = null,
    val resourceId: String? = null,
    val variableId: String? = null,
)

data class ProjectInventoryIntegrationResult(
    val project: ProjectDocument,
    val diagnostics: List<ProjectInventoryDiagnostic>,
    val changed: Boolean,
)

object ProjectResourceBridge {
    fun toProjectResource(
        resource: WorkspaceResource,
        previous: ProjectResource? = null,
    ): ProjectResource {
        val scope = resource.metadata["projectScope"]
            ?.let { value -> enumValues<ProjectResourceScope>().firstOrNull { it.name.equals(value, ignoreCase = true) } }
            ?: previous?.scope
            ?: ProjectResourceScope.ProjectLocal
        val location = resource.uri?.takeIf(::isExternalLocation)
            ?: previous?.location?.takeUnless { it.startsWith(INLINE_LOCATION_PREFIX) }
            ?: "$INLINE_LOCATION_PREFIX${resource.id}"
        val bridgeMetadata = mapOf(
            RESOURCE_PAYLOAD_KEY to WorkspaceResourceCodec.encode(WorkspaceResourceBundle(resources = listOf(resource))),
            RESOURCE_OWNER_KEY to resource.pluginOwner,
        )
        return ProjectResource(
            id = ResourceId(resource.id),
            name = resource.label,
            type = resource.toProjectResourceType(),
            scope = scope,
            location = location,
            contentHash = resource.metadata["contentHash"] ?: previous?.contentHash,
            metadata = previous?.metadata.orEmpty() + bridgeMetadata,
        )
    }

    fun toWorkspaceResource(resource: ProjectResource): WorkspaceResource? {
        resource.metadata[RESOURCE_PAYLOAD_KEY]?.let { payload ->
            val decoded = WorkspaceResourceCodec.decode(payload) as? WorkspaceResourceDecodeResult.Decoded
            decoded?.bundle?.resources?.singleOrNull()?.let { return it }
        }
        val id = resource.id.value.lowercase()
            .replace(Regex("[^a-z0-9._:-]+"), "-")
            .trim('-')
            .ifBlank { "resource" }
        return runCatching {
            WorkspaceResource(
                id = id,
                kind = resource.type.toWorkspaceResourceKind(),
                label = resource.name.trim().ifBlank { id },
                pluginOwner = resource.metadata[RESOURCE_OWNER_KEY] ?: "visualtasker.project",
                uri = resource.location.takeUnless { it.startsWith(INLINE_LOCATION_PREFIX) },
                metadata = mapOf(
                    "projectResourceId" to resource.id.value,
                    "projectScope" to resource.scope.name,
                ),
            )
        }.getOrNull()
    }

    fun toWorkspaceBundle(project: ProjectDocument): WorkspaceResourceBundle = WorkspaceResourceBundle(
        revision = project.revision.value,
        resources = project.resources.values
            .sortedBy { it.id.value }
            .mapNotNull(::toWorkspaceResource),
    )

    fun isInline(resource: ProjectResource): Boolean = resource.location.startsWith(INLINE_LOCATION_PREFIX)

    private fun isExternalLocation(value: String): Boolean =
        value.startsWith("/") ||
            value.startsWith("file:") ||
            value.startsWith("content:") ||
            value.startsWith("http://") ||
            value.startsWith("https://")
}

object ProjectInventoryIntegrator {
    fun integrate(
        project: ProjectDocument,
        legacyResources: WorkspaceResourceBundle = WorkspaceResourceBundle(),
    ): ProjectInventoryIntegrationResult {
        val diagnostics = mutableListOf<ProjectInventoryDiagnostic>()
        val importLegacyResources = project.metadata[PROJECT_INVENTORY_VERSION_KEY] != PROJECT_INVENTORY_VERSION
        val desiredResources = if (importLegacyResources) {
            legacyResources.resources.associate { resource ->
                val id = ResourceId(resource.id)
                id to ProjectResourceBridge.toProjectResource(resource, project.resources[id])
            }
        } else {
            emptyMap()
        }
        val virtualResources = project.resources + desiredResources
        val desiredVariables = collectSharedVariables(project, diagnostics)
        val virtualVariables = project.variables + desiredVariables
        val virtualProject = project.copy(resources = virtualResources, variables = virtualVariables)
        val desiredReferences = collectManagedReferences(virtualProject, diagnostics)
        val managedExisting = project.references.filterValues { it.metadata[MANAGED_REFERENCE_OWNER_KEY] == MANAGED_REFERENCE_OWNER }

        val operations = buildList {
            desiredResources.values.sortedBy { it.id.value }.forEach { resource ->
                if (project.resources[resource.id] != resource) {
                    add(ProjectOperation.PutResource(operationId("resource", resource.id.value), resource))
                }
            }
            desiredVariables.values.sortedBy { it.definition.id }.forEach { variable ->
                if (project.variables[variable.definition.id] != variable) {
                    add(ProjectOperation.PutVariable(operationId("variable", variable.definition.id), variable))
                }
            }
            managedExisting.toSortedMap(compareBy { it.value }).forEach { (id, existing) ->
                val desired = desiredReferences[id]
                if (desired != existing) add(ProjectOperation.UnbindReference(operationId("unbind", id.value), id))
            }
            desiredReferences.toSortedMap(compareBy { it.value }).forEach { (id, reference) ->
                if (project.references[id] != reference) add(ProjectOperation.BindReference(operationId("bind", id.value), reference))
            }
            if (importLegacyResources) {
                add(
                    ProjectOperation.PutMetadata(
                        operationId("metadata", PROJECT_INVENTORY_VERSION_KEY),
                        mapOf(PROJECT_INVENTORY_VERSION_KEY to PROJECT_INVENTORY_VERSION),
                    ),
                )
            }
        }
        if (operations.isEmpty()) return ProjectInventoryIntegrationResult(project, diagnostics, changed = false)

        return when (
            val result = ProjectOperationExecutor.execute(
                project,
                ProjectTransaction(
                    transactionId = operationId("transaction", "${project.id.value}:${project.revision.value}"),
                    operations = operations,
                    expectedRevision = project.revision,
                ),
            )
        ) {
            is ProjectOperationResult.Success -> ProjectInventoryIntegrationResult(result.project, diagnostics, changed = true)
            is ProjectOperationResult.Failure -> ProjectInventoryIntegrationResult(
                project,
                diagnostics + ProjectInventoryDiagnostic(
                    ProjectInventoryDiagnosticCode.ProjectTransactionRejected,
                    result.diagnostic.message,
                ),
                changed = false,
            )
        }
    }

    private fun collectSharedVariables(
        project: ProjectDocument,
        diagnostics: MutableList<ProjectInventoryDiagnostic>,
    ): Map<String, ProjectVariable> {
        val declarations = project.roots.values
            .flatMap { root -> root.workspace.variables.variables.values.map { root.id to it } }
            .filter { (_, definition) -> definition.scope in SHARED_VARIABLE_SCOPES }
            .groupBy { (_, definition) -> definition.id }
        return buildMap {
            declarations.toSortedMap().forEach { (variableId, entries) ->
                val definitions = entries.map { it.second }.distinct()
                if (definitions.size != 1) {
                    diagnostics += ProjectInventoryDiagnostic(
                        ProjectInventoryDiagnosticCode.ConflictingVariableDefinition,
                        "Variable $variableId has conflicting declarations in ${entries.joinToString { it.first.value }}.",
                        variableId = variableId,
                    )
                    return@forEach
                }
                val definition = definitions.single()
                val previous = project.variables[variableId]
                if (definition.scope == VariableScope.External && previous?.providerBinding.isNullOrBlank()) {
                    diagnostics += ProjectInventoryDiagnostic(
                        ProjectInventoryDiagnosticCode.ExternalVariableProviderMissing,
                        "External variable $variableId is declared but has no provider binding.",
                        rootId = entries.first().first,
                        variableId = variableId,
                    )
                    return@forEach
                }
                put(
                    variableId,
                    ProjectVariable(
                        definition = definition,
                        providerBinding = previous?.providerBinding,
                        metadata = previous?.metadata.orEmpty() + mapOf("declaration.owner" to "workflow-root"),
                    ),
                )
            }
        }
    }

    private fun collectManagedReferences(
        project: ProjectDocument,
        diagnostics: MutableList<ProjectInventoryDiagnostic>,
    ): Map<ProjectReferenceId, ProjectReference> = buildMap {
        project.roots.values.sortedBy { it.id.value }.forEach { root ->
            val canonical = root.workspace.canonical ?: return@forEach
            canonical.relations
                .asSequence()
                .filter { relation ->
                    relation.kind == SemanticRelationKind.Reference &&
                        relation.role.kind == SemanticRelationRoleKind.Variable
                }
                .sortedBy { it.id.value }
                .forEach { relation ->
                    val variableId = relation.target.id.value.removePrefix("variable:")
                    if (variableId !in project.variables) return@forEach
                    val id = ProjectReferenceId("m8:variable:${root.id.value}:${relation.source.id.value}:$variableId")
                    put(
                        id,
                        ProjectReference(
                            id = id,
                            source = ProjectReferenceSource.Root(root.id, relation.source.id),
                            target = ProjectReferenceTarget.Variable(variableId),
                            kind = ProjectReferenceKind.Variable,
                            metadata = mapOf(MANAGED_REFERENCE_OWNER_KEY to MANAGED_REFERENCE_OWNER),
                        ),
                    )
                }

            val entitiesByLegacyId = canonical.entities
                .filter { it.ref.kind in setOf(SemanticEntityKind.Statement, SemanticEntityKind.Expression) }
                .mapNotNull { entity -> entity.legacySourceId?.let { it to entity.ref.id } }
                .toMap()
            root.workspace.blocks.values.sortedBy { it.id.value }.forEach { block ->
                val sourceEntityId = entitiesByLegacyId[block.id.value] ?: return@forEach
                block.fields.toSortedMap().forEach fieldLoop@{ (fieldName, fieldValue) ->
                    if (fieldName.lowercase() !in RESOURCE_REFERENCE_FIELDS) return@fieldLoop
                    val resourceId = (fieldValue as? FieldValue.Text)?.value?.trim().orEmpty()
                    if (resourceId.isBlank()) return@fieldLoop
                    val resource = project.resources[ResourceId(resourceId)]
                    if (resource == null) {
                        if (RESOURCE_ID_PREFIXES.any(resourceId::startsWith)) {
                            diagnostics += ProjectInventoryDiagnostic(
                                ProjectInventoryDiagnosticCode.ExplicitResourceReferenceMissing,
                                "${root.name} references missing resource $resourceId in $fieldName.",
                                rootId = root.id,
                                entityId = sourceEntityId,
                                resourceId = resourceId,
                            )
                        }
                        return@fieldLoop
                    }
                    val id = ProjectReferenceId("m8:resource:${root.id.value}:${sourceEntityId.value}:$fieldName:${resource.id.value}")
                    put(
                        id,
                        ProjectReference(
                            id = id,
                            source = ProjectReferenceSource.Root(root.id, sourceEntityId),
                            target = ProjectReferenceTarget.Resource(resource.id),
                            kind = ProjectReferenceKind.Resource,
                            metadata = mapOf(MANAGED_REFERENCE_OWNER_KEY to MANAGED_REFERENCE_OWNER),
                        ),
                    )
                }
            }
        }
    }

    private fun operationId(kind: String, value: String): ProjectOperationId =
        ProjectOperationId("m8-2:$kind:${value.hashCode().toUInt().toString(16)}")

    private val SHARED_VARIABLE_SCOPES = setOf(
        VariableScope.Global,
        VariableScope.Project,
        VariableScope.AppShared,
        VariableScope.External,
    )
    private val RESOURCE_REFERENCE_FIELDS = setOf(
        "resourceid",
        "templateid",
        "markerid",
        "assetid",
        "imageid",
        "regionid",
        "pointid",
        "pathid",
        "shapeid",
    )
    private val RESOURCE_ID_PREFIXES = setOf(
        "resource:",
        "template:",
        "marker:",
        "screenshot:",
        "region:",
        "point:",
        "path:",
        "shape:",
        "visual-asset:",
    )
}

private fun WorkspaceResource.toProjectResourceType(): ProjectResourceType = when (kind) {
    WorkspaceResourceKind.Template -> ProjectResourceType.Template
    WorkspaceResourceKind.Region -> ProjectResourceType.Region
    WorkspaceResourceKind.Screenshot -> ProjectResourceType.Capture
    WorkspaceResourceKind.VisualAsset -> ProjectResourceType.Shape
    WorkspaceResourceKind.Dataset -> ProjectResourceType.Observation
    WorkspaceResourceKind.Marker -> when (markerMode) {
        WorkspaceMarkerMode.Point -> ProjectResourceType.Point
        WorkspaceMarkerMode.Swipe,
        WorkspaceMarkerMode.Path,
        -> ProjectResourceType.Path
        WorkspaceMarkerMode.Region -> ProjectResourceType.Region
        WorkspaceMarkerMode.Node -> ProjectResourceType.Shape
        null -> ProjectResourceType.Observation
    }
    WorkspaceResourceKind.Overlay -> ProjectResourceType.Observation
    WorkspaceResourceKind.Unknown -> ProjectResourceType.Provider
}

private fun ProjectResourceType.toWorkspaceResourceKind(): WorkspaceResourceKind = when (this) {
    ProjectResourceType.Image,
    ProjectResourceType.Capture,
    -> WorkspaceResourceKind.Screenshot
    ProjectResourceType.Template -> WorkspaceResourceKind.Template
    ProjectResourceType.Region -> WorkspaceResourceKind.Region
    ProjectResourceType.Point,
    ProjectResourceType.Path,
    -> WorkspaceResourceKind.Marker
    ProjectResourceType.Shape -> WorkspaceResourceKind.VisualAsset
    ProjectResourceType.Observation -> WorkspaceResourceKind.Dataset
    ProjectResourceType.Provider -> WorkspaceResourceKind.Unknown
}
