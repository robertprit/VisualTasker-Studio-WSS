package com.visualtasker.wss.workspace.model

const val WORLDVIEW_SCHEMA_VERSION = 1

enum class WorldEntityKind {
    UiElement,
    VisualObject,
    Region,
    Screen,
    Window,
    Resource,
    Unknown,
}

enum class ObservationProvider {
    User,
    Accessibility,
    Ocr,
    OpenCv,
    Yolo,
    Dom,
    Runtime,
    Import,
    Unknown,
}

enum class ObservationKind {
    Touch,
    Bounds,
    Text,
    Role,
    TemplateMatch,
    ObjectDetection,
    DomElement,
    ResourceImport,
    RuntimeEvent,
    Unknown,
}

enum class WorldRelationKind {
    ParentOf,
    Contains,
    AppearsIn,
    ObservedAs,
    UsesResource,
    UsedByWorkflow,
    NextStep,
    ResolvesTo,
    SimilarTo,
}

enum class KnowledgeState {
    Known,
    Unknown,
    NotObserved,
    NotAvailable,
    NotApplicable,
    Conflicting,
}

enum class AmbiguityType {
    IdentityAmbiguity,
    ActionIntentAmbiguity,
    GeneralizationAmbiguity,
    SceneBoundaryAmbiguity,
    TimingAmbiguity,
    ProviderConflict,
    ExpectationDivergence,
    ContextAmbiguity,
    SelectionAmbiguity,
    CorrectionAmbiguity,
}

enum class AmbiguityResolutionState {
    Open,
    Proposed,
    Resolved,
    Rejected,
}

enum class RecordedEventKind {
    UserInput,
    ProviderScan,
    RuntimeAction,
    SceneTransition,
    Correction,
    Unknown,
}

enum class InterpretationState {
    Observed,
    Interpreted,
    IntendedProposal,
    AcceptedIntent,
}

enum class CoordinateSpaceKind {
    Screen,
    Window,
    Viewport,
    DomLocal,
    Image,
    Normalized,
}

data class CoordinateSpace(
    val kind: CoordinateSpaceKind,
    val label: String = kind.name,
) {
    init {
        require(label.isNotBlank() && label == label.trim()) {
            "Coordinate space label must be nonblank and trimmed."
        }
    }
}

data class WorldviewRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val coordinateSpace: CoordinateSpace = CoordinateSpace(CoordinateSpaceKind.Normalized),
) {
    init {
        require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()) {
            "Worldview bounds must be finite."
        }
        require(left < right && top < bottom) {
            "Worldview bounds must describe a non-empty area."
        }
        if (coordinateSpace.kind == CoordinateSpaceKind.Normalized) {
            require(left in 0f..1f && right in 0f..1f && top in 0f..1f && bottom in 0f..1f) {
                "Normalized worldview bounds must stay in 0..1."
            }
        }
    }
}

data class WorldviewPoint(
    val x: Float,
    val y: Float,
    val coordinateSpace: CoordinateSpace = CoordinateSpace(CoordinateSpaceKind.Normalized),
) {
    init {
        require(x.isFinite() && y.isFinite()) {
            "Worldview point must be finite."
        }
        if (coordinateSpace.kind == CoordinateSpaceKind.Normalized) {
            require(x in 0f..1f && y in 0f..1f) {
                "Normalized worldview point must stay in 0..1."
            }
        }
    }
}

data class WorldScene(
    val id: String,
    val label: String,
    val startedAtEpochMs: Long = 0L,
    val endedAtEpochMs: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        requireWorldId(id, "Scene id")
        requireTrimmed(label, "Scene label")
        require(endedAtEpochMs == null || endedAtEpochMs >= startedAtEpochMs) {
            "Scene end must not be before start."
        }
        requireMetadata(metadata)
    }
}

data class WorldEntity(
    val id: String,
    val kind: WorldEntityKind,
    val label: String,
    val sceneId: String? = null,
    val conceptId: String? = null,
    val state: KnowledgeState = KnowledgeState.Known,
    val observationIds: Set<String> = emptySet(),
    val resourceIds: Set<String> = emptySet(),
    val properties: Map<String, String> = emptyMap(),
) {
    init {
        requireWorldId(id, "Entity id")
        requireTrimmed(label, "Entity label")
        sceneId?.let { requireWorldId(it, "Entity scene id") }
        conceptId?.let { requireWorldId(it, "Entity concept id") }
        observationIds.forEach { requireWorldId(it, "Entity observation id") }
        resourceIds.forEach { requireWorldId(it, "Entity resource id") }
        requireMetadata(properties)
    }
}

data class WorldObservation(
    val id: String,
    val provider: ObservationProvider,
    val kind: ObservationKind,
    val sceneId: String? = null,
    val entityId: String? = null,
    val confidence: Float = 1f,
    val observedAtEpochMs: Long = 0L,
    val point: WorldviewPoint? = null,
    val bounds: WorldviewRect? = null,
    val properties: Map<String, String> = emptyMap(),
) {
    init {
        requireWorldId(id, "Observation id")
        sceneId?.let { requireWorldId(it, "Observation scene id") }
        entityId?.let { requireWorldId(it, "Observation entity id") }
        require(confidence in 0f..1f) {
            "Observation confidence must stay in 0..1."
        }
        requireMetadata(properties)
    }
}

data class WorldRelation(
    val id: String,
    val kind: WorldRelationKind,
    val fromId: String,
    val toId: String,
    val confidence: Float = 1f,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        requireWorldId(id, "Relation id")
        requireWorldId(fromId, "Relation from id")
        requireWorldId(toId, "Relation to id")
        require(confidence in 0f..1f) {
            "Relation confidence must stay in 0..1."
        }
        requireMetadata(metadata)
    }
}

data class WorldEvent(
    val id: String,
    val kind: RecordedEventKind,
    val observedAtEpochMs: Long = 0L,
    val sceneId: String? = null,
    val observationIds: Set<String> = emptySet(),
    val entityIds: Set<String> = emptySet(),
    val properties: Map<String, String> = emptyMap(),
) {
    init {
        requireWorldId(id, "Event id")
        sceneId?.let { requireWorldId(it, "Event scene id") }
        observationIds.forEach { requireWorldId(it, "Event observation id") }
        entityIds.forEach { requireWorldId(it, "Event entity id") }
        requireMetadata(properties)
    }
}

data class WorldStep(
    val id: String,
    val label: String,
    val sceneId: String? = null,
    val eventIds: Set<String> = emptySet(),
    val observationIds: Set<String> = emptySet(),
    val entityIds: Set<String> = emptySet(),
    val interpretation: InterpretationState = InterpretationState.Observed,
    val proposedIntent: String? = null,
) {
    init {
        requireWorldId(id, "Step id")
        requireTrimmed(label, "Step label")
        sceneId?.let { requireWorldId(it, "Step scene id") }
        eventIds.forEach { requireWorldId(it, "Step event id") }
        observationIds.forEach { requireWorldId(it, "Step observation id") }
        entityIds.forEach { requireWorldId(it, "Step entity id") }
        require(proposedIntent == null || proposedIntent.isNotBlank()) {
            "Proposed intent must be nonblank when present."
        }
    }
}

data class WorldRecord(
    val id: String,
    val label: String,
    val sceneIds: Set<String> = emptySet(),
    val stepIds: Set<String> = emptySet(),
    val eventIds: Set<String> = emptySet(),
    val startedAtEpochMs: Long = 0L,
    val endedAtEpochMs: Long? = null,
) {
    init {
        requireWorldId(id, "Record id")
        requireTrimmed(label, "Record label")
        sceneIds.forEach { requireWorldId(it, "Record scene id") }
        stepIds.forEach { requireWorldId(it, "Record step id") }
        eventIds.forEach { requireWorldId(it, "Record event id") }
        require(endedAtEpochMs == null || endedAtEpochMs >= startedAtEpochMs) {
            "Record end must not be before start."
        }
    }
}

data class WorldAmbiguity(
    val id: String,
    val type: AmbiguityType,
    val subjectRefs: Set<String>,
    val sceneId: String? = null,
    val recordId: String? = null,
    val workflowRef: String? = null,
    val evidenceObservationIds: Set<String> = emptySet(),
    val candidateRefs: Set<String> = emptySet(),
    val confidence: Float = 1f,
    val impact: String,
    val resolutionState: AmbiguityResolutionState = AmbiguityResolutionState.Open,
    val humanResolution: String? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        requireWorldId(id, "Ambiguity id")
        require(subjectRefs.isNotEmpty()) {
            "Ambiguity must reference at least one subject."
        }
        subjectRefs.forEach { requireWorldId(it, "Ambiguity subject ref") }
        sceneId?.let { requireWorldId(it, "Ambiguity scene id") }
        recordId?.let { requireWorldId(it, "Ambiguity record id") }
        evidenceObservationIds.forEach { requireWorldId(it, "Ambiguity evidence observation id") }
        candidateRefs.forEach { requireWorldId(it, "Ambiguity candidate ref") }
        require(confidence in 0f..1f) {
            "Ambiguity confidence must stay in 0..1."
        }
        requireTrimmed(impact, "Ambiguity impact")
        require(humanResolution == null || humanResolution.isNotBlank()) {
            "Human resolution must be nonblank when present."
        }
        requireMetadata(metadata)
    }
}

data class WorldviewDocument(
    val schemaVersion: Int = WORLDVIEW_SCHEMA_VERSION,
    val revision: Long = 0L,
    val scenes: List<WorldScene> = emptyList(),
    val entities: List<WorldEntity> = emptyList(),
    val observations: List<WorldObservation> = emptyList(),
    val relations: List<WorldRelation> = emptyList(),
    val events: List<WorldEvent> = emptyList(),
    val steps: List<WorldStep> = emptyList(),
    val records: List<WorldRecord> = emptyList(),
    val ambiguities: List<WorldAmbiguity> = emptyList(),
    val resources: WorkspaceResourceBundle = WorkspaceResourceBundle(),
) {
    init {
        require(schemaVersion == WORLDVIEW_SCHEMA_VERSION) {
            "Unsupported worldview schema version $schemaVersion."
        }
        requireUnique(scenes.map { it.id }, "Scene")
        requireUnique(entities.map { it.id }, "Entity")
        requireUnique(observations.map { it.id }, "Observation")
        requireUnique(relations.map { it.id }, "Relation")
        requireUnique(events.map { it.id }, "Event")
        requireUnique(steps.map { it.id }, "Step")
        requireUnique(records.map { it.id }, "Record")
        requireUnique(ambiguities.map { it.id }, "Ambiguity")
    }

    fun findScene(id: String): WorldScene? =
        scenes.firstOrNull { it.id == id }

    fun findEntity(id: String): WorldEntity? =
        entities.firstOrNull { it.id == id }

    fun observationsForEntity(entityId: String): List<WorldObservation> =
        observations.filter { it.entityId == entityId || it.id in findEntity(entityId)?.observationIds.orEmpty() }

    fun resourcesForEntity(entityId: String): List<WorkspaceResource> =
        findEntity(entityId)
            ?.resourceIds
            .orEmpty()
            .mapNotNull(resources::find)

    fun ambiguitiesForSubject(subjectRef: String): List<WorldAmbiguity> =
        ambiguities.filter { subjectRef in it.subjectRefs }

    companion object {
        fun fromResources(resources: WorkspaceResourceBundle): WorldviewDocument =
            WorkspaceResourcesWorldviewProjector.project(resources)
    }
}

object WorldviewReducer {
    fun upsertScene(document: WorldviewDocument, scene: WorldScene): WorldviewDocument =
        document.copy(revision = document.revision + 1, scenes = upsertSorted(document.scenes, scene, WorldScene::id, WorldScene::label))

    fun upsertEntity(document: WorldviewDocument, entity: WorldEntity): WorldviewDocument =
        document.copy(revision = document.revision + 1, entities = upsertSorted(document.entities, entity, WorldEntity::id, WorldEntity::label))

    fun upsertObservation(document: WorldviewDocument, observation: WorldObservation): WorldviewDocument =
        document.copy(
            revision = document.revision + 1,
            observations = upsertSorted(document.observations, observation, WorldObservation::id) { it.id },
        )

    fun upsertAmbiguity(document: WorldviewDocument, ambiguity: WorldAmbiguity): WorldviewDocument =
        document.copy(
            revision = document.revision + 1,
            ambiguities = upsertSorted(document.ambiguities, ambiguity, WorldAmbiguity::id) { it.id },
        )
}

object WorkspaceResourcesWorldviewProjector {
    fun project(resources: WorkspaceResourceBundle): WorldviewDocument {
        val scene = if (resources.resources.isEmpty()) {
            null
        } else {
            WorldScene(
                id = "scene:resources",
                label = "Imported Resources",
                metadata = mapOf("source" to "workspace-resources"),
            )
        }
        return WorldviewDocument(
            revision = resources.revision,
            scenes = listOfNotNull(scene),
            entities = resources.resources.map { it.toWorldEntity(scene?.id) },
            observations = resources.resources.map { it.toResourceObservation(scene?.id) },
            relations = resources.resources.map { resource ->
                WorldRelation(
                    id = "relation:${resource.id}:observed-as-resource",
                    kind = WorldRelationKind.ObservedAs,
                    fromId = "observation:${resource.id}:import",
                    toId = "entity:${resource.id}",
                    metadata = mapOf("resourceId" to resource.id),
                )
            },
            resources = resources,
        )
    }
}

private fun WorkspaceResource.toWorldEntity(sceneId: String?): WorldEntity =
    WorldEntity(
        id = "entity:$id",
        kind = when (kind) {
            WorkspaceResourceKind.Marker -> WorldEntityKind.UiElement
            WorkspaceResourceKind.Region -> WorldEntityKind.Region
            WorkspaceResourceKind.Template -> WorldEntityKind.VisualObject
            WorkspaceResourceKind.Screenshot -> WorldEntityKind.Screen
            WorkspaceResourceKind.Dataset,
            WorkspaceResourceKind.Overlay -> WorldEntityKind.Resource
            WorkspaceResourceKind.Unknown -> WorldEntityKind.Unknown
        },
        label = label,
        sceneId = sceneId,
        observationIds = setOf("observation:$id:import"),
        resourceIds = setOf(id),
        properties = mapOfNonNullValues(
            "resourceKind" to kind.name,
            "pluginOwner" to pluginOwner,
            "markerMode" to markerMode?.name,
            "packageName" to packageName,
            "activityClass" to activityClass,
        ),
    )

private fun WorkspaceResource.toResourceObservation(sceneId: String?): WorldObservation =
    WorldObservation(
        id = "observation:$id:import",
        provider = ObservationProvider.Import,
        kind = ObservationKind.ResourceImport,
        sceneId = sceneId,
        entityId = "entity:$id",
        observedAtEpochMs = updatedAtEpochMs,
        point = point?.let { WorldviewPoint(it.x, it.y) },
        bounds = region?.let { WorldviewRect(it.left, it.top, it.right, it.bottom) },
        properties = mapOfNonNullValues(
            "resourceKind" to kind.name,
            "uri" to uri,
            "mimeType" to mimeType,
            "hidden" to hidden.toString(),
            "locked" to locked.toString(),
        ) + metadata,
    )

private fun <T> upsertSorted(
    list: List<T>,
    item: T,
    idOf: (T) -> String,
    labelOf: (T) -> String,
): List<T> {
    val nextItems = if (list.any { idOf(it) == idOf(item) }) {
        list.map { existing -> if (idOf(existing) == idOf(item)) item else existing }
    } else {
        list + item
    }
    return nextItems.sortedWith(compareBy(labelOf, idOf))
}

private fun requireWorldId(id: String, label: String) {
    require(id.matches(WORLD_ID_PATTERN)) {
        "$label must use lowercase letters, numbers, dot, dash, underscore, or colon."
    }
}

private fun requireTrimmed(value: String, label: String) {
    require(value.isNotBlank() && value == value.trim()) {
        "$label must be nonblank and trimmed."
    }
}

private fun requireMetadata(metadata: Map<String, String>) {
    require(metadata.keys.all { it.isNotBlank() && it == it.trim() }) {
        "Metadata keys must be nonblank and trimmed."
    }
}

private fun requireUnique(ids: List<String>, label: String) {
    require(ids.toSet().size == ids.size) {
        "$label ids must be unique."
    }
}

private fun mapOfNonNullValues(vararg pairs: Pair<String, String?>): Map<String, String> =
    pairs.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

private val WORLD_ID_PATTERN = Regex("[a-z0-9][a-z0-9._:-]*")
