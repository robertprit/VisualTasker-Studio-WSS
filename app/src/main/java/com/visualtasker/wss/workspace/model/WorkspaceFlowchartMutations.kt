package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceAction
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.domain.BlockNode
import de.visualtasker.blockeditor.domain.Connection
import de.visualtasker.blockeditor.domain.ConnectionId
import de.visualtasker.blockeditor.domain.ConnectionKind
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.domain.StatementInput
import de.visualtasker.blockeditor.domain.ValueInput
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.WorkspaceGraph
import de.visualtasker.blockeditor.domain.WorkspacePoint
import de.visualtasker.blockeditor.domain.WorkspaceReducer
import de.visualtasker.blockeditor.domain.allConnections
import de.visualtasker.blockeditor.domain.newBlockId
import de.visualtasker.blockeditor.domain.rootOffset
import de.visualtasker.blockeditor.domain.withConnectionUpdated
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.asFactory
import de.visualtasker.blockeditor.registry.createNode
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowViewDocument
import kotlin.math.abs

const val WORKFLOW_SOURCE_FLOWCHART_PREFIX = "flowchart:"
private const val FLOW_BLOCK_NODE_PREFIX = "block:"
private const val MAX_IF_BRANCHES = 8

data class FlowchartConnectionOption(
    val kind: FlowEdgeKind,
    val label: String?,
    val displayLabel: String,
)

sealed interface FlowchartWorkspaceMutation {
    data class AddNode(
        val definitionId: String,
        val afterNodeId: FlowNodeId? = null,
        val position: FlowPoint? = null,
    ) : FlowchartWorkspaceMutation
    data class DeleteNode(val nodeId: FlowNodeId) : FlowchartWorkspaceMutation
    data class DeleteNodes(val nodeIds: Set<FlowNodeId>) : FlowchartWorkspaceMutation
    data class DisconnectEdge(
        val graph: FlowGraphDocument,
        val edgeId: FlowEdgeId,
    ) : FlowchartWorkspaceMutation

    data class ConnectNodes(
        val sourceNodeId: FlowNodeId,
        val targetNodeId: FlowNodeId,
        val kind: FlowEdgeKind = FlowEdgeKind.SEQUENCE,
        val label: String? = null,
    ) : FlowchartWorkspaceMutation

    data class ConnectPorts(
        val sourceNodeId: FlowNodeId,
        val sourcePortName: String,
        val targetNodeId: FlowNodeId,
        val targetPortName: String,
        val fallbackKind: FlowEdgeKind,
    ) : FlowchartWorkspaceMutation

    data class UpdateNodeField(
        val nodeId: FlowNodeId,
        val fieldKey: String,
        val rawValue: String,
    ) : FlowchartWorkspaceMutation

    data class ReplaceNodeType(
        val nodeId: FlowNodeId,
        val definitionId: String,
    ) : FlowchartWorkspaceMutation

    data class AddIfBranch(val nodeId: FlowNodeId) : FlowchartWorkspaceMutation
    data class RemoveIfBranch(val nodeId: FlowNodeId) : FlowchartWorkspaceMutation
    data class SyncViewPositions(val viewDocument: FlowViewDocument) : FlowchartWorkspaceMutation
}

data class FlowchartWorkspaceMutationResult(
    val document: WorkspaceDocument,
    val applied: Boolean,
    val mutation: FlowchartWorkspaceMutation,
)

fun applyFlowchartWorkspaceMutation(
    document: WorkspaceDocument,
    mutation: FlowchartWorkspaceMutation,
): FlowchartWorkspaceMutationResult {
    val updated = when (mutation) {
        is FlowchartWorkspaceMutation.AddNode -> addFlowchartNodeToWorkspace(
            document,
            mutation.definitionId,
            mutation.afterNodeId,
            mutation.position,
        )
        is FlowchartWorkspaceMutation.DeleteNode -> deleteFlowchartNodeFromWorkspace(document, mutation.nodeId)
        is FlowchartWorkspaceMutation.DeleteNodes -> deleteFlowchartNodesFromWorkspace(document, mutation.nodeIds)
        is FlowchartWorkspaceMutation.DisconnectEdge -> disconnectFlowchartEdgeFromWorkspace(document, mutation.graph, mutation.edgeId)
        is FlowchartWorkspaceMutation.ConnectNodes -> connectFlowchartNodesInWorkspace(
            document = document,
            sourceNodeId = mutation.sourceNodeId,
            targetNodeId = mutation.targetNodeId,
            kind = mutation.kind,
            label = mutation.label,
        )
        is FlowchartWorkspaceMutation.ConnectPorts -> connectFlowchartPortsInWorkspace(
            document = document,
            sourceNodeId = mutation.sourceNodeId,
            sourcePortName = mutation.sourcePortName,
            targetNodeId = mutation.targetNodeId,
            targetPortName = mutation.targetPortName,
            fallbackKind = mutation.fallbackKind,
        )
        is FlowchartWorkspaceMutation.UpdateNodeField -> updateFlowchartNodeFieldInWorkspace(
            document = document,
            nodeId = mutation.nodeId,
            fieldKey = mutation.fieldKey,
            rawValue = mutation.rawValue,
        )
        is FlowchartWorkspaceMutation.ReplaceNodeType -> replaceFlowchartNodeTypeInWorkspace(
            document = document,
            nodeId = mutation.nodeId,
            definitionId = mutation.definitionId,
        )
        is FlowchartWorkspaceMutation.AddIfBranch -> addFlowchartIfBranchInWorkspace(document, mutation.nodeId)
        is FlowchartWorkspaceMutation.RemoveIfBranch -> removeFlowchartIfBranchInWorkspace(document, mutation.nodeId)
        is FlowchartWorkspaceMutation.SyncViewPositions -> syncRootPositionsFromFlowchartView(document, mutation.viewDocument)
    }
    return FlowchartWorkspaceMutationResult(
        document = updated,
        applied = updated != document,
        mutation = mutation,
    )
}

fun addFlowchartNodeToWorkspace(
    document: WorkspaceDocument,
    definitionId: String,
    afterNodeId: FlowNodeId? = null,
    position: FlowPoint? = null,
): WorkspaceDocument {
    val afterBlockId = afterNodeId?.toWorkspaceBlockId()
    val afterBlock = afterBlockId?.let { document.blocks[it] }
    val anchor = afterBlockId?.let { document.rootOffset(it) }
    val x = position?.x?.toFloat() ?: anchor?.x ?: 96f
    val y = position?.y?.toFloat() ?: anchor?.y?.plus(96f) ?: (120f + document.rootBlocks.size * 32f)
    val (withNode, insertedId) = instantiateFlowchartBlock(document, definitionId, x, y)
        ?: return document
    if (afterBlock == null || afterBlock.next == null) return withNode
    val inserted = withNode.blocks[insertedId] ?: return withNode
    val source = withNode.blocks[afterBlockId]?.next?.id ?: return withNode
    val target = inserted.previous?.id ?: return withNode
    return WorkspaceReducer.reduce(withNode, WorkspaceAction.Connect(source, target))
}

private fun instantiateFlowchartBlock(
    document: WorkspaceDocument,
    definitionId: String,
    x: Float,
    y: Float,
): Pair<WorkspaceDocument, BlockId>? {
    val id = newBlockId()
    val block = DefaultBlockRegistry.getDefinition(definitionId)?.createNode(id) ?: return null
    val withBlock = document.copy(
        version = document.version + 1,
        blocks = document.blocks + (id to block),
    )
    val roots = WorkspaceGraph.pruneRootBlocks(withBlock, document.rootBlocks + id)
    return withBlock.copy(
        rootBlocks = roots,
        rootPositions = withBlock.rootPositions + (id to WorkspacePoint(x, y)),
    ) to id
}

fun deleteFlowchartNodeFromWorkspace(
    document: WorkspaceDocument,
    nodeId: FlowNodeId,
): WorkspaceDocument {
    val blockId = nodeId.toWorkspaceBlockId() ?: return document
    if (blockId !in document.blocks) return document
    return WorkspaceReducer.reduce(document, WorkspaceAction.DeleteBlock(blockId))
}

fun deleteFlowchartNodesFromWorkspace(
    document: WorkspaceDocument,
    nodeIds: Collection<FlowNodeId>,
): WorkspaceDocument {
    val toRemove = nodeIds
        .mapNotNull { it.toWorkspaceBlockId() }
        .filterTo(mutableSetOf()) { it in document.blocks }
        .flatMapTo(mutableSetOf()) { blockId ->
            WorkspaceGraph.descendants(document, blockId) + blockId
        }
    if (toRemove.isEmpty()) return document

    var blocks = document.blocks.toMutableMap()
    val promotedRoots = mutableListOf<BlockId>()
    toRemove.forEach { removedId ->
        document.blocks[removedId]?.allConnections().orEmpty().forEach { connection ->
            val partnerId = connection.connectedTo ?: return@forEach
            val (partnerBlockId, partnerConnection) = WorkspaceGraph.findConnection(document, partnerId) ?: return@forEach
            if (partnerBlockId !in toRemove) {
                blocks[partnerBlockId] = blocks[partnerBlockId]
                    ?.withConnectionUpdated(partnerId) { it.copy(connectedTo = null) }
                    ?: return@forEach
                if (partnerConnection.kind == ConnectionKind.Previous) {
                    promotedRoots += partnerBlockId
                }
            }
        }
    }
    toRemove.forEach(blocks::remove)
    val reduced = document.copy(
        version = document.version + 1,
        blocks = blocks,
        rootBlocks = document.rootBlocks.filter { it !in toRemove },
        rootPositions = document.rootPositions - toRemove,
    )
    val roots = WorkspaceGraph.pruneRootBlocks(reduced, reduced.rootBlocks + promotedRoots)
    return reduced.copy(
        rootBlocks = roots,
        rootPositions = reduced.rootPositions.filterKeys { it in roots },
    )
}

fun disconnectFlowchartEdgeFromWorkspace(
    document: WorkspaceDocument,
    graph: FlowGraphDocument,
    edgeId: FlowEdgeId,
): WorkspaceDocument {
    val edge = graph.edges.firstOrNull { it.id == edgeId } ?: return document
    val sourceBlockId = edge.sourceNodeId.toWorkspaceBlockId() ?: return document
    val targetBlockId = edge.targetNodeId.toWorkspaceBlockId() ?: return document
    val sourceBlock = document.blocks[sourceBlockId] ?: return document
    val targetBlock = document.blocks[targetBlockId] ?: return document

    val connectionId = when (edge.kind) {
        FlowEdgeKind.SEQUENCE,
        FlowEdgeKind.LOOP_EXIT -> sourceBlock.next?.id
        FlowEdgeKind.TRUE_BRANCH,
        FlowEdgeKind.FALSE_BRANCH,
        FlowEdgeKind.ELSE_IF_BRANCH,
        FlowEdgeKind.LOOP_BODY -> sourceBlock.statementInputs
            .firstOrNull { input ->
                input.name == edge.label ||
                    input.connection.connectedTo == targetBlock.previous?.id
            }
            ?.connection
            ?.id
        FlowEdgeKind.DATA_FLOW,
        FlowEdgeKind.CONDITION -> targetBlock.valueInputs
            .firstOrNull { input ->
                input.name == edge.label ||
                    input.connection.connectedTo == sourceBlock.output?.id
            }
            ?.connection
            ?.id
        else -> null
    } ?: return document

    val (_, connection) = WorkspaceGraph.findConnection(document, connectionId) ?: return document
    if (connection.connectedTo == null) return document
    val disconnected = WorkspaceReducer.reduce(document, WorkspaceAction.Disconnect(connectionId))
    val promotedRoots = when (edge.kind) {
        FlowEdgeKind.SEQUENCE,
        FlowEdgeKind.LOOP_EXIT,
        FlowEdgeKind.TRUE_BRANCH,
        FlowEdgeKind.FALSE_BRANCH,
        FlowEdgeKind.ELSE_IF_BRANCH,
        FlowEdgeKind.LOOP_BODY -> listOf(targetBlockId)
        FlowEdgeKind.DATA_FLOW,
        FlowEdgeKind.CONDITION -> listOf(sourceBlockId)
        else -> emptyList()
    }.filter { it in disconnected.blocks }
    if (promotedRoots.isEmpty()) return disconnected
    return disconnected.copy(
        rootBlocks = WorkspaceGraph.pruneRootBlocks(
            disconnected,
            disconnected.rootBlocks + promotedRoots,
        ),
    )
}

fun connectFlowchartNodesInWorkspace(
    document: WorkspaceDocument,
    sourceNodeId: FlowNodeId,
    targetNodeId: FlowNodeId,
    kind: FlowEdgeKind = FlowEdgeKind.SEQUENCE,
    label: String? = null,
): WorkspaceDocument {
    val sourceBlockId = sourceNodeId.toWorkspaceBlockId() ?: return document
    val targetBlockId = targetNodeId.toWorkspaceBlockId() ?: return document
    val sourceBlock = document.blocks[sourceBlockId] ?: return document
    val targetBlock = document.blocks[targetBlockId] ?: return document
    if (sourceBlockId == targetBlockId) return document

    val action = when (kind) {
        FlowEdgeKind.SEQUENCE,
        FlowEdgeKind.LOOP_EXIT -> {
            val source = sourceBlock.next?.id ?: return document
            val target = targetBlock.previous?.id ?: return document
            WorkspaceAction.Connect(source, target)
        }
        FlowEdgeKind.TRUE_BRANCH,
        FlowEdgeKind.FALSE_BRANCH,
        FlowEdgeKind.ELSE_IF_BRANCH,
        FlowEdgeKind.LOOP_BODY -> {
            val slotName = label ?: defaultStatementSlotName(sourceBlock.type, kind) ?: return document
            val source = sourceBlock.statementInputFor(slotName, kind)?.connection?.id
                ?: return document
            val target = targetBlock.previous?.id ?: return document
            WorkspaceAction.Connect(source, target)
        }
        FlowEdgeKind.DATA_FLOW,
        FlowEdgeKind.CONDITION -> {
            val inputName = label ?: defaultValueInputName(targetBlock.type, kind) ?: return document
            val source = sourceBlock.output?.id ?: return document
            val targetConnection = targetBlock.valueInputs.firstOrNull { it.name == inputName }?.connection
                ?: return document
            val formerReporterBlockId = targetConnection.connectedTo
                ?.let { WorkspaceGraph.findConnection(document, it) }
                ?.takeIf { (_, connection) -> connection.kind == ConnectionKind.Output }
                ?.first
            val prepared = if (targetConnection.connectedTo != null && targetConnection.connectedTo != source) {
                val disconnected = WorkspaceReducer.reduce(document, WorkspaceAction.Disconnect(targetConnection.id))
                if (formerReporterBlockId != null && formerReporterBlockId in disconnected.blocks) {
                    disconnected.copy(
                        rootBlocks = WorkspaceGraph.pruneRootBlocks(
                            disconnected,
                            disconnected.rootBlocks + formerReporterBlockId,
                        ),
                    )
                } else {
                    disconnected
                }
            } else {
                document
            }
            return WorkspaceReducer.reduce(prepared, WorkspaceAction.Connect(source, targetConnection.id))
        }
        else -> return document
    }

    return WorkspaceReducer.reduce(document, action)
}

fun connectFlowchartPortsInWorkspace(
    document: WorkspaceDocument,
    sourceNodeId: FlowNodeId,
    sourcePortName: String,
    targetNodeId: FlowNodeId,
    targetPortName: String,
    fallbackKind: FlowEdgeKind,
): WorkspaceDocument {
    val kind = when {
        targetPortName == "previous" -> when {
            sourcePortName == "next" -> fallbackKind.takeIf { it == FlowEdgeKind.LOOP_EXIT } ?: FlowEdgeKind.SEQUENCE
            sourcePortName == BlockTypes.SLOT_THEN -> FlowEdgeKind.TRUE_BRANCH
            sourcePortName == BlockTypes.SLOT_ELSE -> FlowEdgeKind.FALSE_BRANCH
            sourcePortName == BlockTypes.SLOT_ELIF || sourcePortName.startsWith("ELIF_") -> FlowEdgeKind.ELSE_IF_BRANCH
            sourcePortName == BlockTypes.SLOT_DO || sourcePortName == BlockTypes.SLOT_BODY -> FlowEdgeKind.LOOP_BODY
            else -> fallbackKind
        }
        targetPortName == "CONDITION" || targetPortName.startsWith("ELIF_CONDITION") -> FlowEdgeKind.CONDITION
        else -> FlowEdgeKind.DATA_FLOW
    }
    val label = when (kind) {
        FlowEdgeKind.TRUE_BRANCH,
        FlowEdgeKind.FALSE_BRANCH,
        FlowEdgeKind.ELSE_IF_BRANCH,
        FlowEdgeKind.LOOP_BODY -> sourcePortName
        FlowEdgeKind.DATA_FLOW,
        FlowEdgeKind.CONDITION -> targetPortName
        else -> null
    }
    return connectFlowchartNodesInWorkspace(document, sourceNodeId, targetNodeId, kind, label)
}

fun updateFlowchartNodeFieldInWorkspace(
    document: WorkspaceDocument,
    nodeId: FlowNodeId,
    fieldKey: String,
    rawValue: String,
): WorkspaceDocument {
    val blockId = nodeId.toWorkspaceBlockId() ?: return document
    val block = document.blocks[blockId] ?: return document
    if (fieldKey.startsWith("args:")) {
        val index = fieldKey.removePrefix("args:").toIntOrNull() ?: return document
        if (index < 0) return document
        val args = splitFlowchartRawArguments((block.fields["args"] as? FieldValue.Text)?.value.orEmpty()).toMutableList()
        while (args.size <= index) args += ""
        args[index] = rawValue
        return WorkspaceReducer.reduce(
            document,
            WorkspaceAction.UpdateField(blockId, "args", FieldValue.Text(args.joinToString(","))),
        )
    }
    val current = block.fields[fieldKey]
    val parsed = when (current) {
        is FieldValue.Number -> rawValue.toDoubleOrNull()?.let(FieldValue::Number) ?: return document
        is FieldValue.Bool -> rawValue.equals("true", ignoreCase = true)
            .takeIf { it || rawValue.equals("false", ignoreCase = true) }
            ?.let(FieldValue::Bool) ?: return document
        is FieldValue.Text -> FieldValue.Text(rawValue)
        null -> when (fieldKey) {
            "ms", "frequency", "durationMs", "volume", "repeatCount" ->
                rawValue.toDoubleOrNull()?.let(FieldValue::Number) ?: return document
            "active", "value" ->
                rawValue.toBooleanStrictOrNull()?.let(FieldValue::Bool) ?: FieldValue.Text(rawValue)
            else -> FieldValue.Text(rawValue)
        }
    }
    return WorkspaceReducer.reduce(document, WorkspaceAction.UpdateField(blockId, fieldKey, parsed))
}

fun replaceFlowchartNodeTypeInWorkspace(
    document: WorkspaceDocument,
    nodeId: FlowNodeId,
    definitionId: String,
): WorkspaceDocument {
    val blockId = nodeId.toWorkspaceBlockId() ?: return document
    val source = document.blocks[blockId] ?: return document
    if (source.type == definitionId) return document
    val targetDefinition = DefaultBlockRegistry.getDefinition(definitionId) ?: return document
    val targetTemplate = targetDefinition.createNode(blockId)
    if (!source.hasCompatibleEditorSurface(targetTemplate)) return document
    val targetConnectionIds = targetTemplate.allConnections().map { it.id }.toSet()
    val blocks = document.blocks.toMutableMap()
    val promotedRoots = mutableListOf<BlockId>()

    source.allConnections()
        .filter { it.id !in targetConnectionIds }
        .forEach { removedConnection ->
            val partnerId = removedConnection.connectedTo ?: return@forEach
            val (partnerBlockId, partnerConnection) = WorkspaceGraph.findConnection(document, partnerId)
                ?: return@forEach
            blocks[partnerBlockId] = blocks[partnerBlockId]
                ?.withConnectionUpdated(partnerConnection.id) { it.copy(connectedTo = null) }
                ?: return@forEach
            if (partnerConnection.kind == ConnectionKind.Previous ||
                partnerConnection.kind == ConnectionKind.Output
            ) {
                promotedRoots += partnerBlockId
            }
        }

    val sourceValueInputs = source.valueInputs.associateBy { it.name }
    val sourceStatementInputs = source.statementInputs.associateBy { it.name }
    blocks[blockId] = targetTemplate.copy(
        fields = targetTemplate.fields + source.fields.filterKeys { it in targetTemplate.fields },
        previous = source.previous?.takeIf { targetTemplate.previous != null },
        next = source.next?.takeIf { targetTemplate.next != null },
        output = source.output?.takeIf { targetTemplate.output != null },
        valueInputs = targetTemplate.valueInputs.map { input ->
            sourceValueInputs[input.name]?.let { existing ->
                input.copy(connection = existing.connection)
            } ?: input
        },
        statementInputs = targetTemplate.statementInputs.map { input ->
            sourceStatementInputs[input.name]?.let { existing ->
                input.copy(connection = existing.connection)
            } ?: input
        },
        metadata = source.metadata,
        collapsed = source.collapsed,
    )
    val updated = document.copy(
        version = document.version + 1,
        blocks = blocks,
        rootBlocks = WorkspaceGraph.pruneRootBlocks(document.copy(blocks = blocks), document.rootBlocks + promotedRoots),
    )
    return updated.copy(rootPositions = updated.rootPositions.filterKeys { it in updated.rootBlocks })
}

fun addFlowchartIfBranchInWorkspace(
    document: WorkspaceDocument,
    nodeId: FlowNodeId,
): WorkspaceDocument {
    val blockId = nodeId.toWorkspaceBlockId() ?: return document
    val block = document.blocks[blockId] ?: return document
    if (!block.canEditIfBranches()) return document
    val nextCount = (block.ifBranchCount() + 1).coerceAtMost(MAX_IF_BRANCHES)
    if (nextCount == block.ifBranchCount()) return document
    return replaceFlowchartIfBranchShape(document, blockId, nextCount)
}

fun removeFlowchartIfBranchInWorkspace(
    document: WorkspaceDocument,
    nodeId: FlowNodeId,
): WorkspaceDocument {
    val blockId = nodeId.toWorkspaceBlockId() ?: return document
    val block = document.blocks[blockId] ?: return document
    if (!block.canEditIfBranches()) return document
    val nextCount = (block.ifBranchCount() - 1).coerceAtLeast(1)
    if (nextCount == block.ifBranchCount()) return document
    return replaceFlowchartIfBranchShape(document, blockId, nextCount)
}

fun flowchartConnectionOptions(
    document: WorkspaceDocument,
    sourceNodeId: FlowNodeId,
    targetNodeId: FlowNodeId,
): List<FlowchartConnectionOption> {
    val sourceBlockId = sourceNodeId.toWorkspaceBlockId() ?: return emptyList()
    val targetBlockId = targetNodeId.toWorkspaceBlockId() ?: return emptyList()
    val sourceBlock = document.blocks[sourceBlockId] ?: return emptyList()
    val targetBlock = document.blocks[targetBlockId] ?: return emptyList()
    if (sourceBlockId == targetBlockId) return emptyList()

    val candidates = buildList {
        if (sourceBlock.next != null && targetBlock.previous != null) {
            add(FlowchartConnectionOption(FlowEdgeKind.SEQUENCE, null, "Next"))
        }
        if (targetBlock.previous != null) {
            sourceBlock.statementInputs.forEach { input ->
                val kind = statementSlotEdgeKind(sourceBlock.type, input.name) ?: return@forEach
                add(FlowchartConnectionOption(kind, input.name, statementSlotDisplayLabel(input.name)))
            }
        }
        if (sourceBlock.output != null) {
            targetBlock.valueInputs.forEach { input ->
                val kind = if (input.name == "CONDITION" || input.name.startsWith("ELIF_CONDITION")) {
                    FlowEdgeKind.CONDITION
                } else {
                    FlowEdgeKind.DATA_FLOW
                }
                add(FlowchartConnectionOption(kind, input.name, valueInputDisplayLabel(input.name)))
            }
        }
    }

    return candidates.filter { option ->
        connectFlowchartNodesInWorkspace(
            document = document,
            sourceNodeId = sourceNodeId,
            targetNodeId = targetNodeId,
            kind = option.kind,
            label = option.label,
        ) != document
    }
}

fun syncRootPositionsFromFlowchartView(
    document: WorkspaceDocument,
    viewDocument: FlowViewDocument,
): WorkspaceDocument {
    var updated = document
    viewDocument.nodeViews.forEach { nodeView ->
        val blockId = nodeView.nodeId.toWorkspaceBlockId() ?: return@forEach
        if (blockId !in updated.rootBlocks || blockId !in updated.blocks) return@forEach
        val current = updated.rootPositions[blockId]
        val nextX = nodeView.position.x.toFloat()
        val nextY = nodeView.position.y.toFloat()
        if (current != null && current.closeTo(nextX, nextY)) return@forEach
        updated = WorkspaceReducer.reduce(
            updated,
            WorkspaceAction.MoveRoot(blockId, nextX, nextY),
        )
    }
    return updated
}

fun FlowNodeId.toWorkspaceBlockId(): de.visualtasker.blockeditor.domain.BlockId? {
    val value = value.removePrefix(FLOW_BLOCK_NODE_PREFIX)
    if (value == this.value || value.isBlank()) return null
    return de.visualtasker.blockeditor.domain.BlockId(value)
}

private fun defaultStatementSlotName(sourceBlockType: String, kind: FlowEdgeKind): String? =
    when (kind) {
        FlowEdgeKind.TRUE_BRANCH -> BlockTypes.SLOT_THEN
        FlowEdgeKind.FALSE_BRANCH -> BlockTypes.SLOT_ELSE
        FlowEdgeKind.ELSE_IF_BRANCH -> BlockTypes.SLOT_ELIF
        FlowEdgeKind.LOOP_BODY -> when (sourceBlockType) {
            BlockTypes.CONTROL_REPEAT -> BlockTypes.SLOT_DO
            BlockTypes.CONTROL_WHILE -> BlockTypes.SLOT_BODY
            else -> null
        }
        else -> null
    }

private fun statementSlotEdgeKind(sourceBlockType: String, slotName: String): FlowEdgeKind? =
    when (slotName) {
        BlockTypes.SLOT_THEN -> FlowEdgeKind.TRUE_BRANCH
        BlockTypes.SLOT_ELSE -> FlowEdgeKind.FALSE_BRANCH
        BlockTypes.SLOT_ELIF -> FlowEdgeKind.ELSE_IF_BRANCH
        BlockTypes.SLOT_DO -> if (sourceBlockType == BlockTypes.CONTROL_REPEAT) FlowEdgeKind.LOOP_BODY else null
        BlockTypes.SLOT_BODY -> if (sourceBlockType == BlockTypes.CONTROL_WHILE) FlowEdgeKind.LOOP_BODY else null
        else -> if (slotName.startsWith("ELIF_")) FlowEdgeKind.ELSE_IF_BRANCH else null
    }

private fun BlockNode.statementInputFor(slotName: String, kind: FlowEdgeKind): StatementInput? =
    statementInputs.firstOrNull { it.name == slotName }
        ?: when (kind) {
            FlowEdgeKind.ELSE_IF_BRANCH -> statementInputs.firstOrNull { it.name.startsWith("ELIF_") }
            else -> null
        }

private fun BlockNode.hasCompatibleEditorSurface(target: BlockNode): Boolean {
    val sourceIsReporter = output != null
    val targetIsReporter = target.output != null
    val sourceIsStatement = previous != null || next != null || statementInputs.isNotEmpty()
    val targetIsStatement = target.previous != null || target.next != null || target.statementInputs.isNotEmpty()
    return when {
        sourceIsReporter || targetIsReporter -> sourceIsReporter == targetIsReporter
        sourceIsStatement || targetIsStatement -> sourceIsStatement == targetIsStatement
        else -> false
    }
}

private fun statementSlotDisplayLabel(slotName: String): String =
    when (slotName) {
        BlockTypes.SLOT_THEN -> "Branch: then"
        BlockTypes.SLOT_ELIF -> "Branch: elseif"
        BlockTypes.SLOT_ELSE -> "Branch: else"
        BlockTypes.SLOT_DO -> "Loop: do"
        BlockTypes.SLOT_BODY -> "Loop: body"
        else -> if (slotName.startsWith("ELIF_")) {
            "Branch: elseif ${slotName.removePrefix("ELIF_")}"
        } else {
            "Branch: $slotName"
        }
    }

private fun valueInputDisplayLabel(inputName: String): String =
    when (inputName) {
        "CONDITION" -> "Condition"
        "ELIF_CONDITION" -> "Elseif condition"
        "LEFT" -> "Input: left"
        "RIGHT" -> "Input: right"
        "A" -> "Input: A"
        "B" -> "Input: B"
        "Input1" -> "Input: 1"
        "Input2" -> "Input: 2"
        else -> if (inputName.startsWith("ELIF_CONDITION_")) {
            "Elseif condition ${inputName.removePrefix("ELIF_CONDITION_")}"
        } else {
            "Input: $inputName"
        }
    }

private fun defaultValueInputName(targetBlockType: String, kind: FlowEdgeKind): String? =
    when (kind) {
        FlowEdgeKind.CONDITION -> "CONDITION"
        FlowEdgeKind.DATA_FLOW -> when (targetBlockType) {
            BlockTypes.LOGIC_COMPARE -> "LEFT"
            BlockTypes.LOGIC_AND,
            BlockTypes.LOGIC_OR -> "A"
            BlockTypes.LOGIC_OPERATE -> "Input1"
            else -> null
        }
        else -> null
    }

private fun WorkspacePoint.closeTo(x: Float, y: Float): Boolean =
    abs(this.x - x) < 0.5f && abs(this.y - y) < 0.5f

private fun splitFlowchartRawArguments(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inString = false
    var escape = false
    var depth = 0
    raw.forEach { char ->
        when {
            escape -> {
                current.append(char)
                escape = false
            }
            char == '\\' && inString -> {
                current.append(char)
                escape = true
            }
            char == '"' -> {
                current.append(char)
                inString = !inString
            }
            !inString && char in "([{<" -> {
                current.append(char)
                depth += 1
            }
            !inString && char in ")]}>" -> {
                current.append(char)
                depth = (depth - 1).coerceAtLeast(0)
            }
            !inString && depth == 0 && char == ',' -> {
                result += current.toString().trim()
                current.clear()
            }
            else -> current.append(char)
        }
    }
    val tail = current.toString().trim()
    if (tail.isNotEmpty() || result.isNotEmpty()) result += tail
    return result
}

private fun replaceFlowchartIfBranchShape(
    document: WorkspaceDocument,
    blockId: BlockId,
    branchCount: Int,
): WorkspaceDocument {
    val source = document.blocks[blockId] ?: return document
    val targetType = when {
        branchCount <= 1 -> BlockTypes.CONTROL_IF
        branchCount == 2 -> BlockTypes.CONTROL_IF_ELSE
        else -> BlockTypes.CONTROL_IF_ELSEIF_ELSE
    }
    val targetDefinition = DefaultBlockRegistry.getDefinition(targetType) ?: return document
    val targetTemplate = targetDefinition.createNode(blockId).withIfBranches(branchCount)
    val targetConnectionIds = targetTemplate.allConnections().map { it.id }.toSet()
    val promotedRoots = mutableListOf<BlockId>()
    val blocks = document.blocks.toMutableMap()

    source.allConnections()
        .filter { it.id !in targetConnectionIds }
        .forEach { removedConnection ->
            val partnerId = removedConnection.connectedTo ?: return@forEach
            val (partnerBlockId, partnerConnection) = WorkspaceGraph.findConnection(document, partnerId)
                ?: return@forEach
            blocks[partnerBlockId] = blocks[partnerBlockId]
                ?.withConnectionUpdated(partnerConnection.id) { it.copy(connectedTo = null) }
                ?: return@forEach
            if (removedConnection.kind == ConnectionKind.StatementInput &&
                partnerConnection.kind == ConnectionKind.Previous
            ) {
                promotedRoots += partnerBlockId
            }
        }

    val sourceValueInputs = source.valueInputs.associateBy { it.name }
    val sourceStatementInputs = source.statementInputs.associateBy { it.name }
    blocks[blockId] = targetTemplate.copy(
        fields = targetTemplate.fields + source.fields,
        previous = source.previous?.takeIf { targetTemplate.previous != null },
        next = source.next?.takeIf { targetTemplate.next != null },
        output = source.output?.takeIf { targetTemplate.output != null },
        valueInputs = targetTemplate.valueInputs.map { input ->
            sourceValueInputs[input.name]?.let { existing ->
                input.copy(connection = existing.connection)
            } ?: input
        },
        statementInputs = targetTemplate.statementInputs.map { input ->
            sourceStatementInputs[input.name]?.let { existing ->
                input.copy(connection = existing.connection)
            } ?: input
        },
        collapsed = source.collapsed,
        metadata = source.metadata + ("if.branchCount" to branchCount.toString()),
    )
    val updated = document.copy(
        version = document.version + 1,
        blocks = blocks,
    )
    return updated.copy(
        rootBlocks = WorkspaceGraph.pruneRootBlocks(updated, document.rootBlocks + promotedRoots),
        rootPositions = updated.rootPositions.filterKeys { it in WorkspaceGraph.topLevelRoots(updated) },
    )
}

private fun BlockNode.ifBranchCount(): Int {
    val explicit = metadata["if.branchCount"]?.toIntOrNull()
    if (explicit != null) return explicit.coerceIn(1, MAX_IF_BRANCHES)
    return statementInputs.count { input ->
        input.name == BlockTypes.SLOT_THEN ||
            input.name == BlockTypes.SLOT_ELSE ||
            input.name == BlockTypes.SLOT_ELIF ||
            input.name.startsWith("ELIF_")
    }.coerceAtLeast(1)
}

private fun BlockNode.canEditIfBranches(): Boolean =
    type == BlockTypes.CONTROL_IF ||
        type == BlockTypes.CONTROL_IF_ELSE ||
        type == BlockTypes.CONTROL_IF_ELSEIF_ELSE

private fun BlockNode.withIfBranches(branchCount: Int): BlockNode {
    val count = branchCount.coerceIn(1, MAX_IF_BRANCHES)
    if (count <= 1) return this
    val elifCount = (count - 2).coerceAtLeast(0)
    val nextValueInputs = buildList {
        addAll(this@withIfBranches.valueInputs.filterNot { it.name.startsWith("ELIF_CONDITION_") })
        repeat(elifCount) { index ->
            val number = index + 1
            add(
                ValueInput(
                    name = "ELIF_CONDITION_$number",
                    connection = Connection(
                        id = ConnectionId("${id.value}:ELIF_CONDITION_$number"),
                        owner = id,
                        kind = ConnectionKind.ValueInput,
                        accepts = setOf("Bool", "Boolean"),
                        slotName = "ELIF_CONDITION_$number",
                    ),
                ),
            )
        }
    }
    val nextStatementInputs = buildList {
        add(statementInput(BlockTypes.SLOT_THEN))
        repeat(elifCount) { index ->
            add(statementInput("ELIF_${index + 1}"))
        }
        add(statementInput(BlockTypes.SLOT_ELSE))
    }
    return copy(valueInputs = nextValueInputs, statementInputs = nextStatementInputs)
}

private fun BlockNode.statementInput(name: String): StatementInput =
    statementInputs.find { it.name == name } ?: StatementInput(
        name = name,
        connection = Connection(
            id = ConnectionId("${id.value}:$name:stmt"),
            owner = id,
            kind = ConnectionKind.StatementInput,
            slotName = name,
        ),
    )
