package com.visualtasker.wss.workspace.model

enum class WorldviewInspectorSubjectKind {
    Scene,
    Entity,
    Observation,
    Resource,
    Record,
    Step,
    Ambiguity,
}

data class WorldviewInspectorSubject(
    val kind: WorldviewInspectorSubjectKind,
    val id: String,
) {
    init {
        require(id.matches(WORLDVIEW_REF_PATTERN)) {
            "Inspector subject id must use lowercase letters, numbers, dot, dash, underscore, or colon."
        }
    }
}

data class WorldviewInspectorRow(
    val label: String,
    val value: String,
) {
    init {
        require(label.isNotBlank() && label == label.trim()) {
            "Inspector row label must be nonblank and trimmed."
        }
    }
}

data class WorldviewInspectorProjection(
    val subject: WorldviewInspectorSubject,
    val title: String,
    val rows: List<WorldviewInspectorRow>,
    val observationIds: List<String> = emptyList(),
    val resourceIds: List<String> = emptyList(),
    val ambiguityIds: List<String> = emptyList(),
) {
    init {
        require(title.isNotBlank() && title == title.trim()) {
            "Inspector title must be nonblank and trimmed."
        }
    }
}

data class WorldviewDataProjection(
    val resources: List<WorldviewDataResourceItem>,
    val scenes: List<WorldviewDataSceneItem>,
    val observationGroups: List<WorldviewObservationGroupItem> = emptyList(),
    val ambiguityCount: Int,
)

data class WorldviewDataResourceItem(
    val id: String,
    val kind: WorkspaceResourceKind,
    val label: String,
    val pluginOwner: String,
    val hidden: Boolean,
    val locked: Boolean,
)

data class WorldviewDataSceneItem(
    val id: String,
    val label: String,
    val entityCount: Int,
    val observationCount: Int,
    val recordCount: Int,
)

data class WorldviewObservationGroupItem(
    val key: String,
    val label: String,
    val provider: ObservationProvider,
    val count: Int,
    val latestAtEpochMs: Long,
    val sampleObservationIds: List<String>,
)

enum class VisualUiMemoryProvider {
    A11Y,
    OCR,
    OCV,
    YOLO,
    Marker,
    Template,
    Runtime,
    RAG,
    AI,
    ML,
}

enum class VisualUiMemoryFacet {
    Scene,
    Entity,
    Observation,
    Memory,
    Dataset,
    Usage,
    Suggestion,
}

data class VisualUiMemoryProviderState(
    val provider: VisualUiMemoryProvider,
    val itemCount: Int,
    val active: Boolean,
)

data class VisualUiMemoryFacetCount(
    val facet: VisualUiMemoryFacet,
    val count: Int,
)

data class VisualUiMemorySuggestion(
    val id: String,
    val sourceId: String,
    val type: String,
    val label: String,
    val detail: String,
    val confidence: Float,
    val evidenceRefs: Set<String> = emptySet(),
    val proposedAction: String? = null,
)

data class VisualUiMemoryProjection(
    val title: String,
    val worldviewRevision: Long,
    val providerStates: List<VisualUiMemoryProviderState>,
    val facetCounts: List<VisualUiMemoryFacetCount>,
    val observationGroups: List<WorldviewObservationGroupItem> = emptyList(),
    val suggestions: List<VisualUiMemorySuggestion>,
) {
    val activeProviderCount: Int
        get() = providerStates.count { it.active }
}

object WorldviewInspectorProjector {
    fun project(
        document: WorldviewDocument,
        subject: WorldviewInspectorSubject,
    ): WorldviewInspectorProjection? =
        when (subject.kind) {
            WorldviewInspectorSubjectKind.Scene -> document.findScene(subject.id)?.let { scene ->
                val entities = document.entities.filter { it.sceneId == scene.id }
                val observations = document.observations.filter { it.sceneId == scene.id }
                val records = document.records.filter { scene.id in it.sceneIds }
                WorldviewInspectorProjection(
                    subject = subject,
                    title = scene.label,
                    rows = listOf(
                        WorldviewInspectorRow("Type", "Scene"),
                        WorldviewInspectorRow("Entities", entities.size.toString()),
                        WorldviewInspectorRow("Observations", observations.size.toString()),
                        WorldviewInspectorRow("Records", records.size.toString()),
                    ) + scene.metadata.toRows(),
                    observationIds = observations.map { it.id },
                )
            }
            WorldviewInspectorSubjectKind.Entity -> document.findEntity(subject.id)?.let { entity ->
                val observations = document.observationsForEntity(entity.id)
                WorldviewInspectorProjection(
                    subject = subject,
                    title = entity.label,
                    rows = listOf(
                        WorldviewInspectorRow("Type", entity.kind.name),
                        WorldviewInspectorRow("State", entity.state.name),
                        WorldviewInspectorRow("Scene", entity.sceneId.orEmpty()),
                        WorldviewInspectorRow("Concept", entity.conceptId.orEmpty()),
                    ) + entity.properties.toRows(),
                    observationIds = observations.map { it.id },
                    resourceIds = entity.resourceIds.sorted(),
                    ambiguityIds = document.ambiguitiesForSubject(entity.id).map { it.id },
                )
            }
            WorldviewInspectorSubjectKind.Observation -> document.observations.firstOrNull { it.id == subject.id }?.let { observation ->
                WorldviewInspectorProjection(
                    subject = subject,
                    title = observation.kind.name,
                    rows = listOf(
                        WorldviewInspectorRow("Type", "Observation"),
                        WorldviewInspectorRow("Provider", observation.provider.name),
                        WorldviewInspectorRow("Confidence", observation.confidence.toString()),
                        WorldviewInspectorRow("Scene", observation.sceneId.orEmpty()),
                        WorldviewInspectorRow("Entity", observation.entityId.orEmpty()),
                    ) + observation.properties.toRows(),
                )
            }
            WorldviewInspectorSubjectKind.Resource -> document.resources.find(subject.id)?.let { resource ->
                WorldviewInspectorProjection(
                    subject = subject,
                    title = resource.label,
                    rows = listOf(
                        WorldviewInspectorRow("Type", resource.kind.name),
                        WorldviewInspectorRow("Plugin", resource.pluginOwner),
                        WorldviewInspectorRow("Uri", resource.uri.orEmpty()),
                        WorldviewInspectorRow("Hidden", resource.hidden.toString()),
                        WorldviewInspectorRow("Locked", resource.locked.toString()),
                    ) + resource.metadata.toRows(),
                    resourceIds = listOf(resource.id),
                )
            }
            WorldviewInspectorSubjectKind.Record -> document.records.firstOrNull { it.id == subject.id }?.let { record ->
                WorldviewInspectorProjection(
                    subject = subject,
                    title = record.label,
                    rows = listOf(
                        WorldviewInspectorRow("Type", "Record"),
                        WorldviewInspectorRow("Scenes", record.sceneIds.size.toString()),
                        WorldviewInspectorRow("Steps", record.stepIds.size.toString()),
                        WorldviewInspectorRow("Events", record.eventIds.size.toString()),
                    ),
                )
            }
            WorldviewInspectorSubjectKind.Step -> document.steps.firstOrNull { it.id == subject.id }?.let { step ->
                WorldviewInspectorProjection(
                    subject = subject,
                    title = step.label,
                    rows = listOf(
                        WorldviewInspectorRow("Type", "Step"),
                        WorldviewInspectorRow("Interpretation", step.interpretation.name),
                        WorldviewInspectorRow("Intent", step.proposedIntent.orEmpty()),
                    ),
                    observationIds = step.observationIds.sorted(),
                    ambiguityIds = document.ambiguitiesForSubject(step.id).map { it.id },
                )
            }
            WorldviewInspectorSubjectKind.Ambiguity -> document.ambiguities.firstOrNull { it.id == subject.id }?.let { ambiguity ->
                WorldviewInspectorProjection(
                    subject = subject,
                    title = ambiguity.type.name,
                    rows = listOf(
                        WorldviewInspectorRow("Type", "Ambiguity"),
                        WorldviewInspectorRow("State", ambiguity.resolutionState.name),
                        WorldviewInspectorRow("Confidence", ambiguity.confidence.toString()),
                        WorldviewInspectorRow("Impact", ambiguity.impact),
                        WorldviewInspectorRow("Resolution", ambiguity.humanResolution.orEmpty()),
                    ) + ambiguity.metadata.toRows(),
                    observationIds = ambiguity.evidenceObservationIds.sorted(),
                    ambiguityIds = listOf(ambiguity.id),
                )
            }
        }
}

object WorldviewDataProjector {
    fun project(document: WorldviewDocument): WorldviewDataProjection =
        WorldviewDataProjection(
            resources = document.resources.resources
                .map {
                    WorldviewDataResourceItem(
                        id = it.id,
                        kind = it.kind,
                        label = it.label,
                        pluginOwner = it.pluginOwner,
                        hidden = it.hidden,
                        locked = it.locked,
                    )
                }
                .sortedWith(compareBy<WorldviewDataResourceItem> { it.kind.name }.thenBy { it.label }.thenBy { it.id }),
            scenes = document.scenes
                .map { scene ->
                    WorldviewDataSceneItem(
                        id = scene.id,
                        label = scene.label,
                        entityCount = document.entities.count { it.sceneId == scene.id },
                        observationCount = document.observations.count { it.sceneId == scene.id },
                        recordCount = document.records.count { scene.id in it.sceneIds },
                    )
                }
                .sortedWith(compareBy(WorldviewDataSceneItem::label, WorldviewDataSceneItem::id)),
            observationGroups = document.toObservationGroups(),
            ambiguityCount = document.ambiguities.size,
        )
}

object VisualUiMemoryProjector {
    fun project(document: WorldviewDocument): VisualUiMemoryProjection {
        val providers = listOf(
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.A11Y,
                itemCount = document.observations.count { it.provider == ObservationProvider.Accessibility },
                active = document.observations.any { it.provider == ObservationProvider.Accessibility },
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.OCR,
                itemCount = document.observations.count { it.provider == ObservationProvider.Ocr },
                active = document.observations.any { it.provider == ObservationProvider.Ocr },
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.OCV,
                itemCount = document.observations.count { it.provider == ObservationProvider.OpenCv || it.kind == ObservationKind.TemplateMatch },
                active = document.observations.any { it.provider == ObservationProvider.OpenCv || it.kind == ObservationKind.TemplateMatch },
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.YOLO,
                itemCount = document.observations.count { it.provider == ObservationProvider.Yolo || it.kind == ObservationKind.ObjectDetection },
                active = document.observations.any { it.provider == ObservationProvider.Yolo || it.kind == ObservationKind.ObjectDetection },
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.Marker,
                itemCount = document.resources.byKind(WorkspaceResourceKind.Marker).size + document.resources.byKind(WorkspaceResourceKind.Region).size,
                active = document.resources.byKind(WorkspaceResourceKind.Marker).isNotEmpty() || document.resources.byKind(WorkspaceResourceKind.Region).isNotEmpty(),
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.Template,
                itemCount = document.resources.byKind(WorkspaceResourceKind.Template).size,
                active = document.resources.byKind(WorkspaceResourceKind.Template).isNotEmpty(),
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.Runtime,
                itemCount = document.events.count { it.kind == RecordedEventKind.RuntimeAction } + document.observations.count { it.provider == ObservationProvider.Runtime },
                active = document.events.any { it.kind == RecordedEventKind.RuntimeAction } || document.observations.any { it.provider == ObservationProvider.Runtime },
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.RAG,
                itemCount = document.resources.byKind(WorkspaceResourceKind.Dataset).size,
                active = false,
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.AI,
                itemCount = document.ambiguities.count { it.resolutionState == AmbiguityResolutionState.Proposed },
                active = document.ambiguities.any { it.resolutionState == AmbiguityResolutionState.Proposed },
            ),
            VisualUiMemoryProviderState(
                provider = VisualUiMemoryProvider.ML,
                itemCount = document.resources.byKind(WorkspaceResourceKind.Dataset).size,
                active = document.resources.byKind(WorkspaceResourceKind.Dataset).isNotEmpty(),
            ),
        )
        val observationGroups = document.toObservationGroups()
        val suggestions = buildList {
            document.resources.byKind(WorkspaceResourceKind.Marker).forEach { resource ->
                add(
                    VisualUiMemorySuggestion(
                        id = "suggestion:${resource.id}:block",
                        sourceId = resource.id,
                        type = "CREATE_CLICK_ACTION",
                        label = "Marker als Block/Node nutzen",
                        detail = "${resource.label} kann als click/marker/template Kandidat in Workflow-Projektionen einfliessen.",
                        confidence = 0.72f,
                        evidenceRefs = setOf(resource.id),
                        proposedAction = "createWorkflowTarget",
                    )
                )
            }
            document.resources.byKind(WorkspaceResourceKind.Template).forEach { resource ->
                add(
                    VisualUiMemorySuggestion(
                        id = "suggestion:${resource.id}:template",
                        sourceId = resource.id,
                        type = "CREATE_TEMPLATE",
                        label = "Template als Vision-Kandidat nutzen",
                        detail = "${resource.label} kann OCV/Template-Compare, Marker und Dataset verbinden.",
                        confidence = 0.78f,
                        evidenceRefs = setOf(resource.id),
                        proposedAction = "createTemplateCompare",
                    )
                )
            }
            document.observations
                .filter { observation ->
                    observation.provider == ObservationProvider.Accessibility &&
                        observation.properties["source"] == "railtrace-recording" &&
                        (observation.bounds != null || observation.point != null)
                }
                .forEach { observation ->
                    add(
                        VisualUiMemorySuggestion(
                            id = "suggestion:${observation.id}:marker",
                            sourceId = observation.id,
                            type = "CREATE_MARKER_FROM_RECORD",
                            label = "Recorder-Step als Marker nutzen",
                            detail = "${observation.properties["text"] ?: observation.kind.name} kann als Marker, Click oder Template-Ausgangspunkt dienen.",
                            confidence = if (observation.kind == ObservationKind.Touch) 0.82f else 0.68f,
                            evidenceRefs = setOf(observation.id),
                            proposedAction = "createMarkerFromObservation",
                        )
                    )
                    if (observation.kind == ObservationKind.Touch) {
                        add(
                            VisualUiMemorySuggestion(
                                id = "suggestion:${observation.id}:click",
                                sourceId = observation.id,
                                type = "CREATE_CLICK_FROM_RECORD",
                                label = "Recorder-Step als Click nutzen",
                                detail = "${observation.properties["text"] ?: "Click"} kann direkt als click/touch Befehl erzeugt werden.",
                                confidence = 0.88f,
                                evidenceRefs = setOf(observation.id),
                                proposedAction = "createClickFromObservation",
                            )
                        )
                    }
                }
            document.ambiguities.filter { it.resolutionState == AmbiguityResolutionState.Open }.forEach { ambiguity ->
                add(
                    VisualUiMemorySuggestion(
                        id = "suggestion:${ambiguity.id}:resolve",
                        sourceId = ambiguity.id,
                        type = "ASK_PERUGGER",
                        label = "Unsicherheit klaeren",
                        detail = ambiguity.impact,
                        confidence = ambiguity.confidence,
                        evidenceRefs = ambiguity.evidenceObservationIds + ambiguity.subjectRefs,
                        proposedAction = "openClarification",
                    )
                )
            }
        }.sortedWith(compareByDescending<VisualUiMemorySuggestion> { it.confidence }.thenBy { it.id })

        return VisualUiMemoryProjection(
            title = "Visual UI Memory",
            worldviewRevision = document.revision,
            providerStates = providers,
            facetCounts = listOf(
                VisualUiMemoryFacetCount(VisualUiMemoryFacet.Scene, document.scenes.size),
                VisualUiMemoryFacetCount(VisualUiMemoryFacet.Entity, document.entities.size),
                VisualUiMemoryFacetCount(VisualUiMemoryFacet.Observation, document.observations.size),
                VisualUiMemoryFacetCount(VisualUiMemoryFacet.Memory, document.relations.size),
                VisualUiMemoryFacetCount(VisualUiMemoryFacet.Dataset, document.resources.byKind(WorkspaceResourceKind.Dataset).size),
                VisualUiMemoryFacetCount(VisualUiMemoryFacet.Usage, document.relations.count { it.kind == WorldRelationKind.UsedByWorkflow || it.kind == WorldRelationKind.UsesResource }),
                VisualUiMemoryFacetCount(VisualUiMemoryFacet.Suggestion, suggestions.size),
            ),
            observationGroups = observationGroups,
            suggestions = suggestions,
        )
    }
}

private fun WorldviewDocument.toObservationGroups(): List<WorldviewObservationGroupItem> =
    observations
        .groupBy { observation ->
            val sceneKey = observation.sceneId
                ?: observation.properties["activity"]
                ?: observation.properties["assetId"]
                ?: "scene:unassigned"
            "${observation.provider.name}:$sceneKey"
        }
        .map { (key, group) ->
            val first = group.first()
            val label = first.sceneId
                ?.let(::findScene)
                ?.label
                ?: first.properties["activity"]
                ?: first.properties["assetId"]
                ?: "Unassigned"
            WorldviewObservationGroupItem(
                key = key,
                label = label,
                provider = first.provider,
                count = group.size,
                latestAtEpochMs = group.maxOf { it.observedAtEpochMs },
                sampleObservationIds = group
                    .sortedByDescending { it.observedAtEpochMs }
                    .take(5)
                    .map { it.id },
            )
        }
        .sortedWith(compareByDescending<WorldviewObservationGroupItem> { it.latestAtEpochMs }.thenBy { it.key })

private fun Map<String, String>.toRows(): List<WorldviewInspectorRow> =
    entries
        .sortedBy { it.key }
        .map { (key, value) -> WorldviewInspectorRow(key, value) }

private val WORLDVIEW_REF_PATTERN = Regex("[a-z0-9][a-z0-9._:-]*")
