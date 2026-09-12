package com.visualtasker.wss.workspace.model

data class WssCommandPaletteItem(
    val id: String,
    val label: String,
    val category: String,
    val emscript: String,
    val tags: Set<String> = emptySet(),
) {
    init {
        require(id.matches(WSS_PANEL_DRAG_ID_PATTERN)) {
            "Command palette item id must use lowercase letters, numbers, dot, dash, underscore, or colon."
        }
        require(label.isNotBlank() && label == label.trim()) {
            "Command palette item label must be nonblank and trimmed."
        }
        require(category.isNotBlank() && category == category.trim()) {
            "Command palette item category must be nonblank and trimmed."
        }
        require(emscript.isNotBlank()) {
            "Command palette item script must be nonblank."
        }
    }
}

data class WssPanelDragProjection(
    val panelId: String,
    val panelType: PanelType,
    val dropTarget: WssDropTarget,
    val tree: WssDragTree,
)

object WssPanelDragProjector {
    fun forResources(
        panelId: String,
        bundle: WorkspaceResourceBundle,
        panelType: PanelType = PanelType.Datastore,
    ): WssPanelDragProjection =
        WssPanelDragProjection(
            panelId = panelId,
            panelType = panelType,
            dropTarget = WssDragRules.defaultTargetForPanel(panelId, panelType),
            tree = WssDragTree(
                items = bundle.resources
                    .groupBy { it.kind }
                    .toSortedMap(compareBy { it.name })
                    .map { (kind, resources) ->
                        WssDragTreeItem(
                            id = "group:${panelId}:${kind.name.lowercase()}",
                            payload = WssDragPayload(
                                id = "payload:${panelId}:${kind.name.lowercase()}",
                                kind = WssDragPayloadKind.DatasetEntry,
                                label = kind.name,
                                sourcePanelId = panelId,
                                sourcePanelType = panelType,
                                tags = setOf("group", kind.name.lowercase()),
                                data = mapOf("resourceKind" to kind.name),
                            ),
                            acceptsChildren = true,
                            children = resources
                                .sortedWith(compareBy<WorkspaceResource> { it.label }.thenBy { it.id })
                                .map { resource ->
                                    WssDragTreeItem(
                                        id = "item:${resource.id}",
                                        payload = WssDragPayloadFactory.fromResource(resource, panelId),
                                    )
                                },
                        )
                    },
            ),
        )

    fun forRecorderSteps(
        panelId: String,
        steps: List<RecorderStepUi>,
    ): WssPanelDragProjection =
        WssPanelDragProjection(
            panelId = panelId,
            panelType = PanelType.RecorderSteps,
            dropTarget = WssDragRules.defaultTargetForPanel(panelId, PanelType.RecorderSteps),
            tree = WssDragTree(
                items = steps.map { step ->
                    WssDragTreeItem(
                        id = "item:${step.id}",
                        payload = WssDragPayloadFactory.fromRecorderStep(step, panelId),
                    )
                },
            ),
        )

    fun forCommandPalette(
        panelId: String,
        commands: List<WssCommandPaletteItem>,
        panelType: PanelType = PanelType.TextEditor,
    ): WssPanelDragProjection =
        WssPanelDragProjection(
            panelId = panelId,
            panelType = panelType,
            dropTarget = WssDragRules.defaultTargetForPanel(panelId, panelType),
            tree = WssDragTree(
                items = commands
                    .groupBy { it.category }
                    .toSortedMap()
                    .map { (category, items) ->
                        WssDragTreeItem(
                            id = "group:${panelId}:${category.toDragIdSegment()}",
                            payload = WssDragPayload(
                                id = "payload:${panelId}:${category.toDragIdSegment()}",
                                kind = WssDragPayloadKind.Command,
                                label = category,
                                sourcePanelId = panelId,
                                sourcePanelType = panelType,
                                tags = setOf("group", "command", category.lowercase()),
                                data = mapOf("category" to category),
                            ),
                            acceptsChildren = true,
                            children = items
                                .sortedWith(compareBy<WssCommandPaletteItem> { it.label }.thenBy { it.id })
                                .map { command ->
                                    WssDragTreeItem(
                                        id = "item:command:${command.id}",
                                        payload = WssDragPayloadFactory.fromCommand(
                                            commandId = command.id,
                                            label = command.label,
                                            sourcePanelId = panelId,
                                            emscript = command.emscript,
                                        ).copy(tags = command.tags + "command" + category.lowercase()),
                                    )
                                },
                        )
                    },
            ),
        )

    fun forJunctionPlan(
        panelId: String,
        plan: JunctionPlan,
        panelType: PanelType = PanelType.Datastore,
    ): WssPanelDragProjection =
        WssPanelDragProjection(
            panelId = panelId,
            panelType = panelType,
            dropTarget = WssDragRules.defaultTargetForPanel(panelId, panelType),
            tree = WssDragTree(
                items = listOf(
                    WssDragTreeItem(
                        id = "group:${panelId}:evidence",
                        payload = WssDragPayload(
                            id = "payload:${panelId}:evidence",
                            kind = WssDragPayloadKind.DatasetEntry,
                            label = "Evidence",
                            sourcePanelId = panelId,
                            sourcePanelType = panelType,
                            tags = setOf("junction", "evidence"),
                            data = mapOf("junctionPlanId" to plan.id),
                        ),
                        acceptsChildren = true,
                        children = plan.evidence.map { evidence ->
                            WssDragTreeItem(
                                id = "item:${evidence.id}",
                                payload = WssDragPayload(
                                    id = "payload:${evidence.id}",
                                    kind = WssDragPayloadKind.DatasetEntry,
                                    label = evidence.label,
                                    sourcePanelId = panelId,
                                    sourcePanelType = panelType,
                                    tags = setOf("junction", "evidence", evidence.kind.name.lowercase()),
                                    data = evidence.payload + mapOf(
                                        "evidenceId" to evidence.id,
                                        "evidenceKind" to evidence.kind.name,
                                    ),
                                ),
                            )
                        },
                    ),
                    WssDragTreeItem(
                        id = "group:${panelId}:candidates",
                        payload = WssDragPayload(
                            id = "payload:${panelId}:candidates",
                            kind = WssDragPayloadKind.DatasetEntry,
                            label = "Candidates",
                            sourcePanelId = panelId,
                            sourcePanelType = panelType,
                            tags = setOf("junction", "candidate"),
                            data = mapOf("junctionPlanId" to plan.id),
                        ),
                        acceptsChildren = true,
                        children = plan.candidates.map { candidate ->
                            WssDragTreeItem(
                                id = "item:${candidate.id}",
                                payload = WssDragPayloadFactory.fromJunctionCandidate(candidate, panelId),
                            )
                        },
                    ),
                ),
            ),
        )
}

private fun String.toDragIdSegment(): String =
    lowercase()
        .replace(Regex("[^a-z0-9._:-]+"), "-")
        .trim('-')
        .ifBlank { "group" }

private val WSS_PANEL_DRAG_ID_PATTERN = Regex("[a-z0-9][a-z0-9._:-]*")
