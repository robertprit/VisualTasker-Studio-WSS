package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.BlockId
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowNodeId

data class RailTraceFocusTarget(
    val railStepId: String,
    val sourceLine: Int? = null,
    val blockId: BlockId? = null,
    val flowNodeId: FlowNodeId? = null,
    val flowEdgeId: FlowEdgeId? = null,
    val edgeSourceBlockId: BlockId? = null,
    val edgeTargetBlockId: BlockId? = null,
    val edgeKind: String? = null,
)

fun RecorderStepUi.toRailTraceFocusTarget(): RailTraceFocusTarget {
    val sourceKind = properties["sourceKind"]?.trim()
    val sourceId = properties["sourceId"]?.trim().orEmpty()
    val sourceLine = properties.firstParsedInt("sourceLine", "line", "textLine")
    val explicitBlockId = properties.firstNonBlank("blockId", "sourceBlockId")?.toBlockIdOrNull()
    val explicitNodeId = properties.firstNonBlank("flowNodeId", "nodeId")?.toFlowNodeIdOrNull()
    val explicitEdgeId = properties.firstNonBlank("flowEdgeId", "edgeId")?.toFlowEdgeIdOrNull()
    return when (sourceKind) {
        "Block" -> {
            val blockId = explicitBlockId ?: sourceId.toBlockIdOrNull()
            RailTraceFocusTarget(
                railStepId = id,
                sourceLine = sourceLine,
                blockId = blockId,
                flowNodeId = explicitNodeId ?: blockId?.toFlowNodeId(),
                flowEdgeId = explicitEdgeId,
            )
        }

        "Edge" -> {
            val sourceBlockId = properties["sourceId"]?.toBlockIdOrNull()
            val targetBlockId = properties["edgeTargetId"]?.toBlockIdOrNull()
            RailTraceFocusTarget(
                railStepId = id,
                sourceLine = sourceLine,
                flowEdgeId = explicitEdgeId,
                edgeSourceBlockId = sourceBlockId,
                edgeTargetBlockId = targetBlockId,
                edgeKind = properties["edgeKind"]?.takeIf { it.isNotBlank() },
            )
        }

        "Text" -> RailTraceFocusTarget(
            railStepId = id,
            sourceLine = sourceLine,
        )

        else -> {
            val blockId = explicitBlockId
            RailTraceFocusTarget(
                railStepId = id,
                sourceLine = sourceLine,
                blockId = blockId,
                flowNodeId = explicitNodeId ?: blockId?.toFlowNodeId(),
                flowEdgeId = explicitEdgeId,
            )
        }
    }
}

fun WorkspaceSelectionState.selectRailTraceTarget(
    target: RailTraceFocusTarget,
    source: String = "railtrace",
): WorkspaceSelectionState {
    val focused = when {
        target.flowEdgeId != null -> selectFlowEdge(target.flowEdgeId, source = source)
        target.flowNodeId != null -> selectFlowNode(
            nodeId = target.flowNodeId,
            sourceLine = target.sourceLine ?: sourceLine,
            source = source,
        )
        target.blockId != null -> selectBlock(
            blockId = target.blockId,
            sourceLine = target.sourceLine ?: sourceLine,
            source = source,
        )
        else -> this
    }
    return focused.selectRailStep(
        stepId = target.railStepId,
        sourceLine = target.sourceLine ?: focused.sourceLine,
        source = source,
    )
}

private fun Map<String, String>.firstNonBlank(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> this[key]?.trim()?.takeIf { it.isNotBlank() } }

private fun Map<String, String>.firstParsedInt(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { key ->
        this[key]
            ?.trim()
            ?.toIntOrNull()
    }

private fun String.toBlockIdOrNull(): BlockId? =
    trim()
        .removePrefix("block:")
        .takeIf { it.isNotBlank() }
        ?.let(::BlockId)

private fun String.toFlowNodeIdOrNull(): FlowNodeId? =
    trim()
        .takeIf { it.isNotBlank() }
        ?.let(::FlowNodeId)

private fun String.toFlowEdgeIdOrNull(): FlowEdgeId? =
    trim()
        .takeIf { it.isNotBlank() }
        ?.let(::FlowEdgeId)
