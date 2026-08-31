package com.visualtasker.wss.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.visualtasker.wss.data.PanelState
import com.visualtasker.wss.grid.GridSystem
import kotlin.math.roundToInt

@Composable
fun DarkPanel(
    panel: PanelState,
    onPositionChange: (Offset) -> Unit,
    onSizeChange: (Int, Int) -> Unit,
    onZIndexChange: () -> Unit,
    onFocusRequest: () -> Unit,
    onMinimizeToggle: () -> Unit,
    onMaximizeToggle: () -> Unit,
    onClose: () -> Unit,
    onColorChange: (Color) -> Unit,
    showDefaultRailIcons: Boolean = true,
    showRailColorPicker: Boolean = true,
    railExpandedWidth: Dp = 186.dp,
    railExpandedFillHeight: Boolean = false,
    compactRailContent: @Composable ColumnScope.(onExpandRequested: () -> Unit) -> Unit = {},
    railContent: @Composable ColumnScope.() -> Unit = {},
    isActiveTarget: Boolean,
    snapEnabled: Boolean,
    gridSizeDp: Int,
    maxWidth: Int,
    maxHeight: Int,
    showRail: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var position by remember(panel.id) { mutableStateOf(panel.position) }
    var isDragging by remember { mutableStateOf(false) }
    var isResizing by remember { mutableStateOf(false) }
    var liveWidth by remember(panel.id) { mutableIntStateOf(panel.width) }
    var liveHeight by remember(panel.id) { mutableIntStateOf(panel.height) }
    var railExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density

    LaunchedEffect(panel.position) {
        if (!isDragging) {
            position = panel.position
        }
    }
    LaunchedEffect(panel.width, panel.height) {
        if (!isResizing) {
            liveWidth = panel.width
            liveHeight = panel.height
        }
    }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 2.dp,
        label = "elevation"
    )

    val targetWidth = when {
        panel.isMinimized -> 120
        panel.isMaximized -> maxWidth
        else -> liveWidth
    }
    val targetHeight = when {
        panel.isMinimized -> 48
        panel.isMaximized -> maxHeight
        else -> liveHeight
    }

    val shouldAnimateSize = panel.isMinimized || panel.isMaximized
    val animatedWidth by animateIntAsState(
        targetValue = targetWidth,
        animationSpec = tween(if (shouldAnimateSize) 260 else 0),
        label = "width_anim"
    )
    val animatedHeight by animateIntAsState(
        targetValue = targetHeight,
        animationSpec = tween(if (shouldAnimateSize) 260 else 0),
        label = "height_anim"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .width(animatedWidth.dp)
            .height(animatedHeight.dp)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(20.dp),
                ambientColor = panel.accentColor.copy(alpha = 0.15f),
                spotColor = panel.accentColor.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isActiveTarget) 2.dp else 1.dp,
                color = if (isActiveTarget) panel.accentColor else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (!panel.isMinimized && showRail) {
                CollapsibleIconRail(
                    isExpanded = railExpanded,
                    onToggle = { railExpanded = !railExpanded },
                    accentColor = panel.accentColor,
                    onColorChange = onColorChange,
                    showDefaultIcons = showDefaultRailIcons,
                    showColorPicker = showRailColorPicker,
                    expandedWidth = railExpandedWidth,
                    expandedFillHeight = railExpandedFillHeight,
                    compactRailContent = compactRailContent,
                    railContent = railContent
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(if (panel.isMinimized) 8.dp else 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .clickable {
                                onFocusRequest()
                                onZIndexChange()
                            }
                            .pointerInput(panel.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        onFocusRequest()
                                        onZIndexChange()
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        if (snapEnabled && !panel.isMaximized) {
                                            position = GridSystem.snapPosition(position, gridSizeDp)
                                            onPositionChange(position)
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        position += dragAmount
                                        onPositionChange(position)
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = panel.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (panel.isMinimized) panel.title.take(3) else panel.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }

                        if (!panel.isMinimized) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PanelIconButtonWithTooltip(
                                    tooltip = "Minimieren",
                                    onClick = onMinimizeToggle,
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Minimieren",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                PanelIconButtonWithTooltip(
                                    tooltip = if (panel.isMaximized) "Wiederherstellen" else "Maximieren",
                                    onClick = onMaximizeToggle,
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        if (panel.isMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Maximieren",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                PanelIconButtonWithTooltip(
                                    tooltip = "Schließen",
                                    onClick = onClose,
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Schließen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!panel.isMinimized) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true)
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                content()
                            }
                        }
                    }
                }

                if (!panel.isMinimized && !panel.isMaximized) {
                    ResizeHandle(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        accentColor = panel.accentColor,
                        onResizeStart = { isResizing = true },
                        onResizeEnd = { isResizing = false },
                        onResize = { delta ->
                            val deltaWidthDp = (delta.x / density).roundToInt()
                            val deltaHeightDp = (delta.y / density).roundToInt()
                            val newWidth = (liveWidth + deltaWidthDp).coerceAtLeast(144)
                            val newHeight = (liveHeight + deltaHeightDp).coerceAtLeast(144)
                            liveWidth = newWidth
                            liveHeight = newHeight
                            onSizeChange(newWidth, newHeight)
                        }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PanelIconButtonWithTooltip(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun ResizeHandle(
    accentColor: Color,
    onResizeStart: () -> Unit,
    onResizeEnd: () -> Unit,
    onResize: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onResizeStart() },
                    onDragEnd = { onResizeEnd() },
                    onDragCancel = { onResizeEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onResize(dragAmount)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Resize",
            tint = accentColor.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}
