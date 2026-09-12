package com.visualtasker.wss.workspace.model

enum class WssDragPayloadKind {
    Command,
    Block,
    FlowNode,
    Marker,
    Resource,
    DatasetEntry,
    InspectorField,
    RailTraceStep,
    VisualAsset,
    TextSnippet,
}

enum class WssDragTargetKind {
    TextEditor,
    BlockEditor,
    FlowEditor,
    MarkerPanel,
    RailTrace,
    Datastore,
    Inspector,
    Toolbox,
    VisualAssetManager,
}

enum class WssDragTransferMode {
    Copy,
    Move,
    Link,
    Generate,
}

data class WssDragPayload(
    val id: String,
    val kind: WssDragPayloadKind,
    val label: String,
    val sourcePanelId: String,
    val sourcePanelType: PanelType? = null,
    val mimeType: String? = null,
    val tags: Set<String> = emptySet(),
    val data: Map<String, String> = emptyMap(),
) {
    init {
        require(id.matches(WSS_DRAG_ID_PATTERN)) {
            "Drag payload id must use lowercase letters, numbers, dot, dash, underscore, or colon."
        }
        require(label.isNotBlank() && label == label.trim()) {
            "Drag payload label must be nonblank and trimmed."
        }
        require(sourcePanelId.isNotBlank() && sourcePanelId == sourcePanelId.trim()) {
            "Source panel id must be nonblank and trimmed."
        }
        require(tags.all { it.isNotBlank() && it == it.trim() }) {
            "Drag payload tags must be nonblank and trimmed."
        }
        require(data.keys.all { it.isNotBlank() && it == it.trim() }) {
            "Drag payload data keys must be nonblank and trimmed."
        }
    }
}

data class WssDropTarget(
    val id: String,
    val kind: WssDragTargetKind,
    val panelId: String,
    val acceptedKinds: Set<WssDragPayloadKind>,
    val acceptedModes: Set<WssDragTransferMode>,
    val maxChildren: Int? = null,
    val requiredTags: Set<String> = emptySet(),
) {
    init {
        require(id.matches(WSS_DRAG_ID_PATTERN)) {
            "Drop target id must use lowercase letters, numbers, dot, dash, underscore, or colon."
        }
        require(panelId.isNotBlank() && panelId == panelId.trim()) {
            "Drop target panel id must be nonblank and trimmed."
        }
        require(acceptedKinds.isNotEmpty()) {
            "Drop target must accept at least one payload kind."
        }
        require(acceptedModes.isNotEmpty()) {
            "Drop target must accept at least one transfer mode."
        }
        require(maxChildren == null || maxChildren >= 0) {
            "Drop target child capacity must be non-negative."
        }
        require(requiredTags.all { it.isNotBlank() && it == it.trim() }) {
            "Drop target required tags must be nonblank and trimmed."
        }
    }
}

data class WssDropDecision(
    val accepted: Boolean,
    val mode: WssDragTransferMode?,
    val reason: String,
) {
    companion object {
        fun accept(mode: WssDragTransferMode, reason: String = "accepted"): WssDropDecision =
            WssDropDecision(accepted = true, mode = mode, reason = reason)

        fun reject(reason: String): WssDropDecision =
            WssDropDecision(accepted = false, mode = null, reason = reason)
    }
}

data class WssDropResult(
    val target: WssDropTarget,
    val payload: WssDragPayload,
    val decision: WssDropDecision,
    val generatedPanelAction: PanelAction? = null,
)

data class WssDragSession(
    val payload: WssDragPayload,
    val requestedMode: WssDragTransferMode? = null,
    val hoveredTargetId: String? = null,
)

data class WssDragBusState(
    val targets: Map<String, WssDropTarget> = emptyMap(),
    val activeSession: WssDragSession? = null,
) {
    val activePayload: WssDragPayload?
        get() = activeSession?.payload

    val hoveredTarget: WssDropTarget?
        get() = activeSession?.hoveredTargetId?.let(targets::get)
}

object WssDragBusReducer {
    fun registerTarget(state: WssDragBusState, target: WssDropTarget): WssDragBusState =
        state.copy(targets = state.targets + (target.id to target))

    fun unregisterTarget(state: WssDragBusState, targetId: String): WssDragBusState =
        state.copy(
            targets = state.targets - targetId,
            activeSession = state.activeSession?.takeUnless { it.hoveredTargetId == targetId },
        )

    fun beginDrag(
        state: WssDragBusState,
        payload: WssDragPayload,
        requestedMode: WssDragTransferMode? = null,
    ): WssDragBusState =
        state.copy(activeSession = WssDragSession(payload = payload, requestedMode = requestedMode))

    fun hoverTarget(state: WssDragBusState, targetId: String?): WssDragBusState {
        val session = state.activeSession ?: return state
        val normalizedTargetId = targetId?.takeIf { it in state.targets }
        return state.copy(activeSession = session.copy(hoveredTargetId = normalizedTargetId))
    }

    fun cancelDrag(state: WssDragBusState): WssDragBusState =
        state.copy(activeSession = null)

    fun drop(
        state: WssDragBusState,
        targetId: String? = state.activeSession?.hoveredTargetId,
        currentChildCount: Int = 0,
    ): Pair<WssDragBusState, WssDropResult?> {
        val session = state.activeSession ?: return state to null
        val target = targetId?.let(state.targets::get) ?: return cancelDrag(state) to null
        val decision = WssDragRules.decide(
            payload = session.payload,
            target = target,
            requestedMode = session.requestedMode,
            currentChildCount = currentChildCount,
        )
        return cancelDrag(state) to WssDropResult(
            target = target,
            payload = session.payload,
            decision = decision,
            generatedPanelAction = panelActionFor(target, session.payload, decision),
        )
    }

    private fun panelActionFor(
        target: WssDropTarget,
        payload: WssDragPayload,
        decision: WssDropDecision,
    ): PanelAction? {
        if (!decision.accepted) return null
        return when {
            target.kind == WssDragTargetKind.RailTrace &&
                payload.kind == WssDragPayloadKind.RailTraceStep &&
                decision.mode == WssDragTransferMode.Move ->
                payload.data["stepId"]?.let(PanelAction::SelectStep)
            else -> null
        }
    }
}

object WssDragRules {
    fun defaultTargetForPanel(panelId: String, panelType: PanelType): WssDropTarget =
        when (panelType) {
            PanelType.TextEditor,
            PanelType.Emscript,
            -> WssDropTarget(
                id = "drop:$panelId:text",
                kind = WssDragTargetKind.TextEditor,
                panelId = panelId,
                acceptedKinds = setOf(
                    WssDragPayloadKind.Command,
                    WssDragPayloadKind.TextSnippet,
                    WssDragPayloadKind.Marker,
                    WssDragPayloadKind.DatasetEntry,
                    WssDragPayloadKind.RailTraceStep,
                ),
                acceptedModes = setOf(WssDragTransferMode.Copy, WssDragTransferMode.Generate),
            )
            PanelType.BlockEditor -> WssDropTarget(
                id = "drop:$panelId:blockeditor",
                kind = WssDragTargetKind.BlockEditor,
                panelId = panelId,
                acceptedKinds = setOf(
                    WssDragPayloadKind.Command,
                    WssDragPayloadKind.Block,
                    WssDragPayloadKind.Marker,
                    WssDragPayloadKind.VisualAsset,
                    WssDragPayloadKind.RailTraceStep,
                ),
                acceptedModes = setOf(WssDragTransferMode.Copy, WssDragTransferMode.Generate),
            )
            PanelType.Flowchart -> WssDropTarget(
                id = "drop:$panelId:flow",
                kind = WssDragTargetKind.FlowEditor,
                panelId = panelId,
                acceptedKinds = setOf(
                    WssDragPayloadKind.Command,
                    WssDragPayloadKind.FlowNode,
                    WssDragPayloadKind.Marker,
                    WssDragPayloadKind.VisualAsset,
                    WssDragPayloadKind.RailTraceStep,
                ),
                acceptedModes = setOf(WssDragTransferMode.Copy, WssDragTransferMode.Generate),
            )
            PanelType.Marker -> WssDropTarget(
                id = "drop:$panelId:marker",
                kind = WssDragTargetKind.MarkerPanel,
                panelId = panelId,
                acceptedKinds = setOf(
                    WssDragPayloadKind.Marker,
                    WssDragPayloadKind.Resource,
                    WssDragPayloadKind.DatasetEntry,
                    WssDragPayloadKind.RailTraceStep,
                ),
                acceptedModes = setOf(WssDragTransferMode.Copy, WssDragTransferMode.Move, WssDragTransferMode.Link, WssDragTransferMode.Generate),
            )
            PanelType.RecorderSteps -> WssDropTarget(
                id = "drop:$panelId:railtrace",
                kind = WssDragTargetKind.RailTrace,
                panelId = panelId,
                acceptedKinds = setOf(WssDragPayloadKind.RailTraceStep, WssDragPayloadKind.Marker, WssDragPayloadKind.DatasetEntry),
                acceptedModes = setOf(WssDragTransferMode.Move, WssDragTransferMode.Link),
            )
            PanelType.Datastore -> WssDropTarget(
                id = "drop:$panelId:datastore",
                kind = WssDragTargetKind.Datastore,
                panelId = panelId,
                acceptedKinds = WssDragPayloadKind.entries.toSet(),
                acceptedModes = setOf(WssDragTransferMode.Copy, WssDragTransferMode.Move, WssDragTransferMode.Link),
            )
            PanelType.DebugInfo -> WssDropTarget(
                id = "drop:$panelId:inspector",
                kind = WssDragTargetKind.Inspector,
                panelId = panelId,
                acceptedKinds = setOf(WssDragPayloadKind.InspectorField, WssDragPayloadKind.Resource, WssDragPayloadKind.DatasetEntry),
                acceptedModes = setOf(WssDragTransferMode.Copy, WssDragTransferMode.Link),
            )
            PanelType.M3Director -> WssDropTarget(
                id = "drop:$panelId:visual-assets",
                kind = WssDragTargetKind.VisualAssetManager,
                panelId = panelId,
                acceptedKinds = setOf(WssDragPayloadKind.VisualAsset, WssDragPayloadKind.Resource, WssDragPayloadKind.Marker),
                acceptedModes = setOf(WssDragTransferMode.Copy, WssDragTransferMode.Move, WssDragTransferMode.Link),
            )
            else -> WssDropTarget(
                id = "drop:$panelId:toolbox",
                kind = WssDragTargetKind.Toolbox,
                panelId = panelId,
                acceptedKinds = setOf(WssDragPayloadKind.Command, WssDragPayloadKind.Block, WssDragPayloadKind.FlowNode, WssDragPayloadKind.VisualAsset),
                acceptedModes = setOf(WssDragTransferMode.Copy, WssDragTransferMode.Move),
            )
        }

    fun decide(
        payload: WssDragPayload,
        target: WssDropTarget,
        requestedMode: WssDragTransferMode? = null,
        currentChildCount: Int = 0,
    ): WssDropDecision {
        if (payload.kind !in target.acceptedKinds) {
            return WssDropDecision.reject("${target.kind} accepts no ${payload.kind} payloads.")
        }
        if (!payload.tags.containsAll(target.requiredTags)) {
            return WssDropDecision.reject("Payload misses required target tags: ${target.requiredTags - payload.tags}.")
        }
        if (target.maxChildren != null && currentChildCount >= target.maxChildren) {
            return WssDropDecision.reject("Drop target capacity reached.")
        }
        val mode = requestedMode?.takeIf { it in target.acceptedModes }
            ?: preferredMode(payload, target)
            ?: return WssDropDecision.reject("No compatible transfer mode.")
        return WssDropDecision.accept(mode)
    }

    private fun preferredMode(payload: WssDragPayload, target: WssDropTarget): WssDragTransferMode? {
        val preferred = when (target.kind) {
            WssDragTargetKind.TextEditor,
            WssDragTargetKind.BlockEditor,
            WssDragTargetKind.FlowEditor,
            -> if (payload.kind == WssDragPayloadKind.Command || payload.kind == WssDragPayloadKind.RailTraceStep) {
                WssDragTransferMode.Generate
            } else {
                WssDragTransferMode.Copy
            }
            WssDragTargetKind.Datastore,
            WssDragTargetKind.Inspector,
            -> WssDragTransferMode.Link
            WssDragTargetKind.RailTrace,
            WssDragTargetKind.Toolbox,
            -> WssDragTransferMode.Move
            WssDragTargetKind.MarkerPanel,
            WssDragTargetKind.VisualAssetManager,
            -> WssDragTransferMode.Copy
        }
        return preferred.takeIf { it in target.acceptedModes } ?: target.acceptedModes.firstOrNull()
    }
}

data class WssDragTreeItem(
    val id: String,
    val payload: WssDragPayload,
    val children: List<WssDragTreeItem> = emptyList(),
    val acceptsChildren: Boolean = false,
    val collapsed: Boolean = false,
) {
    init {
        require(id.matches(WSS_DRAG_ID_PATTERN)) {
            "Drag tree item id must use lowercase letters, numbers, dot, dash, underscore, or colon."
        }
        require(children.map { it.id }.toSet().size == children.size) {
            "Drag tree item children must have unique ids."
        }
    }
}

data class WssDragTree(
    val items: List<WssDragTreeItem> = emptyList(),
) {
    init {
        require(items.map { it.id }.toSet().size == items.size) {
            "Drag tree root items must have unique ids."
        }
    }

    fun find(itemId: String): WssDragTreeItem? =
        items.firstNotNullOfOrNull { it.findDeep(itemId) }
}

object WssDragTreeReducer {
    fun insert(
        tree: WssDragTree,
        item: WssDragTreeItem,
        parentId: String? = null,
        index: Int? = null,
    ): WssDragTree {
        require(tree.find(item.id) == null) {
            "Cannot insert duplicate drag tree item ${item.id}."
        }
        return if (parentId == null) {
            tree.copy(items = tree.items.insertAt(item, index))
        } else {
            tree.copy(items = tree.items.map { it.insertChild(parentId, item, index) })
        }
    }

    fun remove(tree: WssDragTree, itemId: String): WssDragTree =
        tree.copy(items = tree.items.removeDeep(itemId))

    fun move(
        tree: WssDragTree,
        itemId: String,
        targetParentId: String? = null,
        targetIndex: Int? = null,
    ): WssDragTree {
        val item = tree.find(itemId) ?: return tree
        val withoutItem = remove(tree, itemId)
        return insert(withoutItem, item, targetParentId, targetIndex)
    }
}

object WssDragPayloadFactory {
    fun fromResource(resource: WorkspaceResource, sourcePanelId: String): WssDragPayload =
        WssDragPayload(
            id = "resource:${resource.id}",
            kind = when (resource.kind) {
                WorkspaceResourceKind.Marker,
                WorkspaceResourceKind.Region,
                WorkspaceResourceKind.Template,
                -> WssDragPayloadKind.Marker
                WorkspaceResourceKind.Dataset -> WssDragPayloadKind.DatasetEntry
                else -> WssDragPayloadKind.Resource
            },
            label = resource.label,
            sourcePanelId = sourcePanelId,
            sourcePanelType = PanelType.Datastore,
            mimeType = resource.mimeType,
            tags = resource.tags + resource.kind.name.lowercase(),
            data = buildMap {
                put("resourceId", resource.id)
                put("resourceKind", resource.kind.name)
                resource.markerMode?.let { put("markerMode", it.name) }
                resource.uri?.let { put("uri", it) }
            },
        )

    fun fromRecorderStep(step: RecorderStepUi, sourcePanelId: String): WssDragPayload =
        WssDragPayload(
            id = "rail:${step.id}",
            kind = WssDragPayloadKind.RailTraceStep,
            label = step.label,
            sourcePanelId = sourcePanelId,
            sourcePanelType = PanelType.RecorderSteps,
            tags = setOf("railtrace", step.actionType.lowercase()),
            data = buildMap {
                put("stepId", step.id)
                put("actionType", step.actionType)
                put("status", step.status.name)
                step.activityName?.let { put("activityName", it) }
                step.detail?.let { put("detail", it) }
            },
        )

    fun fromCommand(commandId: String, label: String, sourcePanelId: String, emscript: String): WssDragPayload =
        WssDragPayload(
            id = "command:$commandId",
            kind = WssDragPayloadKind.Command,
            label = label,
            sourcePanelId = sourcePanelId,
            mimeType = "text/x-emscript",
            tags = setOf("command", "emscript"),
            data = mapOf("commandId" to commandId, "emscript" to emscript),
        )

    fun fromJunctionCandidate(
        candidate: JunctionCandidate,
        sourcePanelId: String,
        preferredTarget: JunctionOutputTarget? = null,
    ): WssDragPayload {
        val target = preferredTarget?.takeIf(candidate.outputTargets::contains)
            ?: candidate.outputTargets.firstOrNull()
        return WssDragPayload(
            id = "junction:${candidate.id}",
            kind = when (target) {
                JunctionOutputTarget.Marker -> WssDragPayloadKind.Marker
                JunctionOutputTarget.Block -> WssDragPayloadKind.Block
                JunctionOutputTarget.FlowNode -> WssDragPayloadKind.FlowNode
                JunctionOutputTarget.Emscript -> WssDragPayloadKind.Command
                JunctionOutputTarget.Dataset,
                null,
                -> WssDragPayloadKind.DatasetEntry
            },
            label = candidate.label,
            sourcePanelId = sourcePanelId,
            tags = setOf("junction", candidate.confidence.name.lowercase()) + candidate.outputTargets.map { it.name.lowercase() },
            data = candidate.parameters + mapOf(
                "candidateId" to candidate.id,
                "confidence" to candidate.confidence.name,
                "outputTargets" to candidate.outputTargets.joinToString(",") { it.name },
            ),
        )
    }
}

private fun WssDragTreeItem.findDeep(itemId: String): WssDragTreeItem? {
    if (id == itemId) return this
    return children.firstNotNullOfOrNull { it.findDeep(itemId) }
}

private fun WssDragTreeItem.insertChild(
    parentId: String,
    item: WssDragTreeItem,
    index: Int?,
): WssDragTreeItem {
    if (id == parentId) {
        require(acceptsChildren) {
            "Target drag tree item $parentId does not accept children."
        }
        return copy(children = children.insertAt(item, index))
    }
    return copy(children = children.map { it.insertChild(parentId, item, index) })
}

private fun List<WssDragTreeItem>.insertAt(item: WssDragTreeItem, index: Int?): List<WssDragTreeItem> {
    val boundedIndex = (index ?: size).coerceIn(0, size)
    return toMutableList().apply { add(boundedIndex, item) }
}

private fun List<WssDragTreeItem>.removeDeep(itemId: String): List<WssDragTreeItem> =
    filterNot { it.id == itemId }
        .map { it.copy(children = it.children.removeDeep(itemId)) }

private val WSS_DRAG_ID_PATTERN = Regex("[a-z0-9][a-z0-9._:-]*")
