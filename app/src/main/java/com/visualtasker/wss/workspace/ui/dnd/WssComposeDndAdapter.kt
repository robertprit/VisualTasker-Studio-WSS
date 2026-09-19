package com.visualtasker.wss.workspace.ui.dnd

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.drag.draggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.visualtasker.wss.workspace.model.WssDragPayload

data class WssDndEnvelope(
    val key: String,
    val payload: WssDragPayload,
)

@Composable
fun rememberWssDragAndDropState(): DragAndDropState<WssDndEnvelope> = rememberDragAndDropState()

@Composable
fun WssDragAndDropContainer(
    state: DragAndDropState<WssDndEnvelope>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    DragAndDropContainer(
        state = state,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

fun Modifier.wssDraggableItem(
    state: DragAndDropState<WssDndEnvelope>,
    envelope: WssDndEnvelope,
    enabled: Boolean = true,
    dragShadow: @Composable () -> Unit,
): Modifier = draggableItem(
    key = envelope.key,
    data = envelope,
    state = state,
    enabled = enabled,
    draggableContent = dragShadow,
)

fun Modifier.wssDropTarget(
    key: String,
    state: DragAndDropState<WssDndEnvelope>,
    canDrop: Boolean = true,
    accepts: (WssDragPayload) -> Boolean,
    onDrop: (WssDragPayload) -> Unit,
): Modifier = dropTarget(
    key = key,
    state = state,
    canDrop = canDrop,
    onDrop = { dragged ->
        dragged.data.payload.takeIf(accepts)?.let(onDrop)
    },
)
