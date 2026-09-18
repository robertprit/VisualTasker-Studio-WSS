package com.visualtasker.wss.workspace.model

import java.util.Locale

object WorldviewEntityUnifier {
    fun unify(document: WorldviewDocument): WorldviewDocument {
        val resourceEntityByObservation = document.resources.resources
            .mapNotNull { resource ->
                resource.metadata["observationId"]?.let { observationId ->
                    observationId to "entity:${resource.id}"
                }
            }
            .toMap()
        val entities = document.entities.associateByTo(linkedMapOf(), WorldEntity::id)
        val observations = document.observations.map { observation ->
            val entityId = observation.entityId
                ?: resourceEntityByObservation[observation.id]
                ?: observation.stableEntityId()
            val existing = entities[entityId]
            val providerNames = existing?.properties?.get("providers")
                .orEmpty()
                .split(',')
                .filter(String::isNotBlank)
                .toSet() + observation.provider.name
            entities[entityId] = if (existing != null) {
                existing.copy(
                    observationIds = existing.observationIds + observation.id,
                    properties = existing.properties + ("providers" to providerNames.sorted().joinToString(",")),
                )
            } else {
                WorldEntity(
                    id = entityId,
                    kind = observation.toEntityKind(),
                    label = observation.entityLabel(),
                    sceneId = observation.sceneId,
                    observationIds = setOf(observation.id),
                    properties = mapOf(
                        "providers" to observation.provider.name,
                        "identitySource" to observation.identitySource(),
                    ),
                )
            }
            if (observation.entityId == entityId) observation else observation.copy(entityId = entityId)
        }
        val relations = (document.relations + observations.map { observation ->
            WorldRelation(
                id = "relation:${observation.id}:observed-as",
                kind = WorldRelationKind.ObservedAs,
                fromId = observation.id,
                toId = checkNotNull(observation.entityId),
                confidence = observation.confidence,
                metadata = mapOf("provider" to observation.provider.name),
            )
        })
            .associateBy { it.id }
            .values
            .sortedBy { it.id }
        val nextEntities = entities.values.sortedWith(compareBy(WorldEntity::label, WorldEntity::id))
        val changed = observations != document.observations ||
            nextEntities != document.entities ||
            relations != document.relations
        return if (changed) {
            document.copy(
                revision = document.revision + 1,
                entities = nextEntities,
                observations = observations.sortedBy { it.id },
                relations = relations,
            )
        } else {
            document
        }
    }
}

private fun WorldObservation.stableEntityId(): String {
    val explicitIdentity = listOf(
        "worldviewKey",
        "resourceId",
        "accessibilityId",
        "viewId",
        "domId",
        "automationId",
    ).firstNotNullOfOrNull { key -> properties[key]?.takeIf(String::isNotBlank)?.let { "$key:$it" } }
    val identity = explicitIdentity ?: buildString {
        append(sceneId ?: "scene-unknown")
        append('|')
        append(properties["text"] ?: properties["description"] ?: kind.name)
        bounds?.let {
            append('|')
            append(it.coordinateSpace.kind.name)
            append(':')
            append(listOf(it.left, it.top, it.right, it.bottom).joinToString(",") { value -> "%.2f".format(Locale.ROOT, value) })
        }
        point?.let {
            append('|')
            append(it.coordinateSpace.kind.name)
            append(':')
            append("%.2f,%.2f".format(Locale.ROOT, it.x, it.y))
        }
    }
    return "entity:observed:${identity.hashCode().toUInt().toString(16)}"
}

private fun WorldObservation.identitySource(): String =
    when {
        properties.keys.any { it in setOf("worldviewKey", "resourceId", "accessibilityId", "viewId", "domId", "automationId") } -> "provider-id"
        bounds != null || point != null -> "scene-geometry"
        else -> "scene-label"
    }

private fun WorldObservation.toEntityKind(): WorldEntityKind =
    when (kind) {
        ObservationKind.Touch,
        ObservationKind.Bounds,
        ObservationKind.Text,
        ObservationKind.Role,
        ObservationKind.DomElement,
        -> WorldEntityKind.UiElement
        ObservationKind.TemplateMatch,
        ObservationKind.ObjectDetection,
        -> WorldEntityKind.VisualObject
        ObservationKind.RuntimeEvent,
        ObservationKind.ResourceImport,
        -> WorldEntityKind.Resource
        ObservationKind.Unknown -> WorldEntityKind.Unknown
    }

private fun WorldObservation.entityLabel(): String =
    properties["text"]
        ?: properties["description"]
        ?: properties["role"]
        ?: "${provider.name} ${kind.name}"
