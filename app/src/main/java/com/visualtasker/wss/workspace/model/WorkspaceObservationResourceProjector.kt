package com.visualtasker.wss.workspace.model

object WorkspaceObservationResourceProjector {
    fun project(
        visionObservations: List<WorldObservation>,
        recorderObservations: List<WorldObservation>,
    ): List<WorkspaceResource> =
        buildList {
            visionObservations.forEach { observation ->
                add(observation.toVersionedResource(namespace = "vision-result", sourceTag = "vision"))
            }
            recorderObservations.forEach { observation ->
                add(observation.toVersionedResource(namespace = "record-step", sourceTag = "recorder"))
            }
        }
            .distinctBy { it.id }
            .sortedWith(compareBy<WorkspaceResource> { it.pluginOwner }.thenBy { it.id })
}

private fun WorldObservation.toVersionedResource(
    namespace: String,
    sourceTag: String,
): WorkspaceResource =
    WorkspaceResource(
        id = "$namespace:${id.toResourceIdSegment()}",
        kind = WorkspaceResourceKind.Dataset,
        label = properties["text"]?.trim()?.takeIf { it.isNotBlank() }
            ?: "${provider.name} ${kind.name}",
        pluginOwner = when (sourceTag) {
            "recorder" -> "visualtasker.recorder"
            else -> "visualtasker.vision"
        },
        mimeType = "application/vnd.visualtasker.observation+json",
        tags = setOf(sourceTag, "observation", provider.name.lowercase(), kind.name.lowercase()),
        metadata = buildMap {
            put("observationId", id)
            put("provider", provider.name)
            put("observationKind", kind.name)
            put("confidence", confidence.toString())
            sceneId?.let { put("sceneId", it) }
            entityId?.let { put("entityId", it) }
            point?.let {
                put("point", "${it.x},${it.y}")
                put("pointSpace", it.coordinateSpace.kind.name)
            }
            bounds?.let {
                put("bounds", "${it.left},${it.top},${it.right},${it.bottom}")
                put("boundsSpace", it.coordinateSpace.kind.name)
            }
            putAll(properties)
        },
        createdAtEpochMs = observedAtEpochMs,
        updatedAtEpochMs = observedAtEpochMs,
    )

private fun String.toResourceIdSegment(): String =
    lowercase()
        .replace(Regex("[^a-z0-9._:-]+"), "-")
        .trim('-', '.', ':', '_')
        .ifBlank { hashCode().toUInt().toString(16) }
