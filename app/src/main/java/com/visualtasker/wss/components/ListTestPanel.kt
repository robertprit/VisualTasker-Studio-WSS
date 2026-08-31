package com.visualtasker.wss.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val ROW_HEIGHT_DP = 72

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListTestPanel(modifier: Modifier = Modifier) {
    val items = remember { mutableStateListOf(*List(20) { "Dummy-Eintrag ${it + 1}" }.toTypedArray()) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val listState = rememberLazyListState()
    var deletedItem by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT_DP.dp.toPx() }

    Column(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = deletedItem != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eintrag gelöscht",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        val cached = deletedItem
                        if (cached != null) {
                            val idx = cached.first.coerceIn(0, items.size)
                            items.add(idx, cached.second)
                            deletedItem = null
                        }
                    }
                ) {
                    Text("Undo")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = items,
                key = { it }
            ) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                            val index = items.indexOf(item)
                            if (index >= 0) {
                                deletedItem = index to item
                                items.remove(item)
                            }
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Löschen",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                ) {
                    ReorderableCard(
                        item = item,
                        expanded = expanded[item] == true,
                        rowHeightPx = rowHeightPx,
                        onExpandToggle = { expanded[item] = !(expanded[item] ?: false) },
                        onMove = { step ->
                            val index = items.indexOf(item)
                            if (index < 0) return@ReorderableCard
                            val target = (index + step).coerceIn(0, items.lastIndex)
                            if (target != index) {
                                val moved = items.removeAt(index)
                                items.add(target, moved)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReorderableCard(
    item: String,
    expanded: Boolean,
    rowHeightPx: Float,
    onExpandToggle: () -> Unit,
    onMove: (Int) -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragAlpha by animateFloatAsState(
        targetValue = if (dragOffset == 0f) 1f else 0.88f,
        label = "drag_alpha"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (dragOffset == 0f) 1f else 1.02f,
        label = "drag_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .alpha(dragAlpha)
            .graphicsLayer {
                translationY = dragOffset * 0.1f
                scaleX = dragScale
                scaleY = dragScale
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .pointerInput(item) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                    val step = (dragOffset / (rowHeightPx * 0.9f)).toInt().coerceIn(-1, 1)
                                    if (step != 0) {
                                        onMove(step)
                                        dragOffset -= step * (rowHeightPx * 0.9f)
                                    }
                                }
                            )
                        }
                )
                Text(
                    text = item,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(if (expanded) "Einklappen" else "Ausklappen") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Einklappen" else "Ausklappen"
                        )
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = "Details zu $item für Expand/Collapse-Tests, inklusive zusätzlichem Text für Scroll- und Layout-Verhalten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

