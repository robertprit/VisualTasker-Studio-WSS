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
            ambiguityCount = document.ambiguities.size,
        )
}

private fun Map<String, String>.toRows(): List<WorldviewInspectorRow> =
    entries
        .sortedBy { it.key }
        .map { (key, value) -> WorldviewInspectorRow(key, value) }

private val WORLDVIEW_REF_PATTERN = Regex("[a-z0-9][a-z0-9._:-]*")
