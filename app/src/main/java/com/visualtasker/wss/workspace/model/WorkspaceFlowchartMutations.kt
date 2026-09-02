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
import de.visualtasker.blockeditor.domain.withConnectionUpdated
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.asFactory
import de.visualtasker.blockeditor.registry.createNode
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowNodeId
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

fun addFlowchartNodeToWorkspace(
    document: WorkspaceDocument,
    definitionId: String,
): WorkspaceDocument {
    val x = 96f
    val y = 120f + (document.rootBlocks.size * 32f)
    return WorkspaceReducer.reduce(
        document,
        WorkspaceAction.InstantiateBlock(definitionId, x, y),
        DefaultBlockRegistry.asFactory(),
    )
}

fun deleteFlowchartNodeFromWorkspace(
    document: WorkspaceDocument,
    nodeId: FlowNodeId,
): WorkspaceDocument {
    val blockId = nodeId.toWorkspaceBlockId() ?: return document
    if (blockId !in document.blocks) return document
    return WorkspaceReducer.reduce(document, WorkspaceAction.DeleteBlock(blockId))
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
            val source = sourceBlock.statementInputs.firstOrNull { it.name == slotName }?.connection?.id
                ?: return document
            val target = targetBlock.previous?.id ?: return document
            WorkspaceAction.Connect(source, target)
        }
        FlowEdgeKind.DATA_FLOW,
        FlowEdgeKind.CONDITION -> {
            val inputName = label ?: defaultValueInputName(targetBlock.type, kind) ?: return document
            val source = sourceBlock.output?.id ?: return document
            val target = targetBlock.valueInputs.firstOrNull { it.name == inputName }?.connection?.id
                ?: return document
            WorkspaceAction.Connect(source, target)
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
                val kind = if (input.name == "CONDITION") {
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
        else -> null
    }

private fun statementSlotDisplayLabel(slotName: String): String =
    when (slotName) {
        BlockTypes.SLOT_THEN -> "Branch: then"
        BlockTypes.SLOT_ELIF -> "Branch: elseif"
        BlockTypes.SLOT_ELSE -> "Branch: else"
        BlockTypes.SLOT_DO -> "Loop: do"
        BlockTypes.SLOT_BODY -> "Loop: body"
        else -> "Branch: $slotName"
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
        else -> "Input: $inputName"
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
