package com.visualtasker.wss.workspace.model

enum class SceneInspectorNodeKind {
    Scene,
    Window,
    Interaction,
    Frame,
    Elements,
    Element,
    Evidence,
    Property,
}

data class SceneInspectorNode(
    val id: String,
    val label: String,
    val kind: SceneInspectorNodeKind,
    val subtitle: String? = null,
    val provider: ObservationProvider? = null,
    val properties: Map<String, String> = emptyMap(),
    val payload: WssDragPayload,
    val children: List<SceneInspectorNode> = emptyList(),
)

data class SceneInspectorTree(
    val sceneId: String,
    val root: SceneInspectorNode,
) {
    fun find(nodeId: String): SceneInspectorNode? = root.findDeep(nodeId)
}

object SceneInspectorTreeProjector {
    fun project(
        panelId: String,
        step: RecorderStepUi,
        recorderObservations: List<WorldObservation>,
    ): SceneInspectorTree {
        val scene = RecorderSceneInspectorProjector.project(step, recorderObservations)
        val sceneId = step.properties.firstNonBlank(
            "sceneId",
            "scene.id",
            "window.currentActivity",
            "window.activity",
        ) ?: "scene:${scene.sceneLabel.toSceneInspectorIdSegment()}"
        val windowProperties = buildMap {
            step.activityName?.let { put("activity", it) }
            step.properties
                .filterKeys { key -> key == "package" || key.startsWith("window.") }
                .forEach { (key, value) -> put(key, value) }
        }
        val frameProperties = step.properties
            .filterKeys { key -> key in FramePropertyKeys || key.startsWith("frame.") }
        val elementNodes = scene.observations.map { observation ->
            observation.toSceneInspectorNode(panelId)
        }
        val evidenceNodes = scene.evidence.mapIndexed { index, (key, value) ->
            sceneInspectorLeaf(
                panelId = panelId,
                id = "evidence:${key.toSceneInspectorIdSegment()}:$index",
                label = key,
                subtitle = value,
                kind = SceneInspectorNodeKind.Property,
                properties = mapOf("key" to key, "value" to value, "sceneId" to sceneId),
                tags = setOf("scene", "evidence", "property"),
            )
        }
        val children = buildList {
            add(
                sceneInspectorBranch(
                    panelId = panelId,
                    id = "window:${sceneId.toSceneInspectorIdSegment()}",
                    label = step.activityName ?: scene.sceneLabel,
                    subtitle = windowProperties["package"] ?: "WindowContext",
                    kind = SceneInspectorNodeKind.Window,
                    properties = windowProperties + ("sceneId" to sceneId),
                    tags = setOf("scene", "window"),
                    children = windowProperties.toPropertyNodes(panelId, "window", sceneId),
                )
            )
            add(
                sceneInspectorBranch(
                    panelId = panelId,
                    id = "interaction:${step.id.toSceneInspectorIdSegment()}",
                    label = scene.actionLabel,
                    subtitle = step.actionType,
                    kind = SceneInspectorNodeKind.Interaction,
                    properties = buildMap {
                        put("stepId", step.id)
                        put("actionType", step.actionType)
                        put("status", step.status.name)
                        step.timestampMs?.let { put("timestampMs", it.toString()) }
                        step.durationMs?.let { put("durationMs", it.toString()) }
                        step.bounds?.let { put("bounds", "${it.left},${it.top},${it.right},${it.bottom}") }
                        step.point?.let { put("point", "${it.x},${it.y}") }
                    },
                    tags = setOf("scene", "interaction", step.actionType.lowercase()),
                )
            )
            if (frameProperties.isNotEmpty()) {
                add(
                    sceneInspectorBranch(
                        panelId = panelId,
                        id = "frames:${sceneId.toSceneInspectorIdSegment()}",
                        label = "Frames",
                        subtitle = "${frameProperties.size} Referenzen",
                        kind = SceneInspectorNodeKind.Frame,
                        properties = frameProperties + ("sceneId" to sceneId),
                        tags = setOf("scene", "frame", "resource"),
                        children = frameProperties.toPropertyNodes(panelId, "frame", sceneId),
                    )
                )
            }
            add(
                sceneInspectorBranch(
                    panelId = panelId,
                    id = "elements:${sceneId.toSceneInspectorIdSegment()}",
                    label = "Elemente",
                    subtitle = "${elementNodes.size} Observations",
                    kind = SceneInspectorNodeKind.Elements,
                    properties = mapOf("sceneId" to sceneId, "count" to elementNodes.size.toString()),
                    tags = setOf("scene", "elements", "observations"),
                    children = elementNodes,
                )
            )
            add(
                sceneInspectorBranch(
                    panelId = panelId,
                    id = "evidence:${sceneId.toSceneInspectorIdSegment()}",
                    label = "Evidence",
                    subtitle = "${evidenceNodes.size} Eintraege",
                    kind = SceneInspectorNodeKind.Evidence,
                    properties = mapOf("sceneId" to sceneId, "count" to evidenceNodes.size.toString()),
                    tags = setOf("scene", "evidence"),
                    children = evidenceNodes,
                )
            )
        }
        val root = sceneInspectorBranch(
            panelId = panelId,
            id = sceneId,
            label = scene.sceneLabel,
            subtitle = "Scene fuer ${step.label}",
            kind = SceneInspectorNodeKind.Scene,
            properties = mapOf(
                "sceneId" to sceneId,
                "stepId" to step.id,
                "status" to step.status.name,
            ),
            tags = setOf("scene", "root"),
            children = children,
        )
        return SceneInspectorTree(sceneId = sceneId, root = root)
    }
}

private fun WorldObservation.toSceneInspectorNode(panelId: String): SceneInspectorNode {
    val label = properties["text"]
        ?: properties["label"]
        ?: properties["role"]
        ?: kind.name
    val details = buildMap {
        put("observationId", id)
        put("provider", provider.name)
        put("kind", kind.name)
        put("confidence", confidence.toString())
        put("observedAtEpochMs", observedAtEpochMs.toString())
        sceneId?.let { put("sceneId", it) }
        entityId?.let { put("entityId", it) }
        bounds?.let { put("bounds", "${it.left},${it.top},${it.right},${it.bottom}") }
        point?.let { put("point", "${it.x},${it.y}") }
        putAll(properties)
    }
    return sceneInspectorBranch(
        panelId = panelId,
        id = "element:${id.toSceneInspectorIdSegment()}",
        label = label,
        subtitle = "${provider.name} - ${(confidence * 100).toInt()}%",
        kind = SceneInspectorNodeKind.Element,
        provider = provider,
        properties = details,
        tags = setOf("scene", "element", "observation", provider.name.lowercase()),
        children = details.toPropertyNodes(panelId, "element:${id.toSceneInspectorIdSegment()}", sceneId),
    )
}

private fun Map<String, String>.toPropertyNodes(
    panelId: String,
    parentId: String,
    sceneId: String?,
): List<SceneInspectorNode> = entries
    .filter { it.value.isNotBlank() }
    .sortedBy { it.key }
    .mapIndexed { index, (key, value) ->
        sceneInspectorLeaf(
            panelId = panelId,
            id = "$parentId:${key.toSceneInspectorIdSegment()}:$index",
            label = key,
            subtitle = value,
            kind = SceneInspectorNodeKind.Property,
            properties = buildMap {
                put("key", key)
                put("value", value)
                sceneId?.let { put("sceneId", it) }
            },
            tags = setOf("scene", "property"),
        )
    }

private fun sceneInspectorBranch(
    panelId: String,
    id: String,
    label: String,
    subtitle: String?,
    kind: SceneInspectorNodeKind,
    provider: ObservationProvider? = null,
    properties: Map<String, String>,
    tags: Set<String>,
    children: List<SceneInspectorNode> = emptyList(),
): SceneInspectorNode = SceneInspectorNode(
    id = id,
    label = label,
    subtitle = subtitle,
    kind = kind,
    provider = provider,
    properties = properties,
    payload = sceneInspectorPayload(panelId, id, label, kind, properties, tags),
    children = children,
)

private fun sceneInspectorLeaf(
    panelId: String,
    id: String,
    label: String,
    subtitle: String?,
    kind: SceneInspectorNodeKind,
    properties: Map<String, String>,
    tags: Set<String>,
): SceneInspectorNode = sceneInspectorBranch(
    panelId = panelId,
    id = id,
    label = label,
    subtitle = subtitle,
    kind = kind,
    properties = properties,
    tags = tags,
)

private fun sceneInspectorPayload(
    panelId: String,
    id: String,
    label: String,
    kind: SceneInspectorNodeKind,
    properties: Map<String, String>,
    tags: Set<String>,
): WssDragPayload = WssDragPayload(
    id = "inspector:${id.toSceneInspectorIdSegment()}",
    kind = WssDragPayloadKind.InspectorField,
    label = label,
    sourcePanelId = panelId,
    sourcePanelType = PanelType.SceneInspector,
    tags = tags + kind.name.lowercase(),
    data = properties + mapOf("inspectorNodeId" to id, "inspectorNodeKind" to kind.name),
)

private fun SceneInspectorNode.findDeep(nodeId: String): SceneInspectorNode? =
    if (id == nodeId) this else children.firstNotNullOfOrNull { it.findDeep(nodeId) }

private fun Map<String, String>.firstNonBlank(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> this[key]?.trim()?.takeIf(String::isNotBlank) }

private fun String.toSceneInspectorIdSegment(): String = lowercase()
    .replace(Regex("[^a-z0-9._:-]+"), "-")
    .trim('-', '.', ':', '_')
    .ifBlank { "unknown" }

private val FramePropertyKeys = setOf(
    "screenshotPath",
    "screenshotFile",
    "frameId",
    "assetHash",
    "assetReference",
)
