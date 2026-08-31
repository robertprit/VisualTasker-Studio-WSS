package com.visualtasker.wss.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.visualtasker.wss.ui.theme.M3EColors

data class RailIconItem(
    val icon: ImageVector,
    val color: Color,
    val label: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollapsibleIconRail(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    onColorChange: (Color) -> Unit,
    icons: List<RailIconItem> = defaultRailIcons,
    showDefaultIcons: Boolean = true,
    showColorPicker: Boolean = true,
    expandedWidth: Dp = 186.dp,
    expandedFillHeight: Boolean = false,
    compactRailContent: @Composable ColumnScope.(onExpandRequested: () -> Unit) -> Unit = {},
    railContent: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "rail_toggle"
    )
    val toggleTint = if (accentColor.luminance() < 0.18f) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        accentColor
    }

    Row(
        modifier = modifier.fillMaxHeight()
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(if (isExpanded) "Siderail einklappen" else "Siderail ausklappen") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier
                            .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(toggleTint.copy(alpha = 0.25f))
                    ) {
                        AnimatedPanelIcon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                            tint = toggleTint,
                            modifier = Modifier.size(16.dp).rotate(rotation),
                            riveArtboard = "rail_chevron",
                            riveStateMachine = "toggle"
                        )
                    }
                }
                compactRailContent {
                    if (!isExpanded) onToggle()
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally(animationSpec = tween(220)),
                exit = shrinkHorizontally(animationSpec = tween(180)),
                modifier = Modifier
                    .offset(x = 44.dp, y = 8.dp)
                    .zIndex(2f)
            ) {
                Surface(
                    modifier = if (expandedFillHeight) Modifier.fillMaxHeight() else Modifier,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val expandedModifier = if (expandedFillHeight) {
                        Modifier
                            .fillMaxHeight()
                            .width(expandedWidth)
                            .padding(6.dp)
                    } else {
                        Modifier
                            .width(expandedWidth)
                            .padding(6.dp)
                    }
                    Column(
                        modifier = expandedModifier,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (showColorPicker) {
                            ColorPickerButton(
                                currentColor = accentColor,
                                onColorSelected = onColorChange
                            )
                        }
                        if (showDefaultIcons) {
                            icons.forEach { item ->
                                RailIconDot(icon = item.icon, color = item.color, label = item.label)
                            }
                        }
                        railContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun RailIconDot(icon: ImageVector, color: Color, label: String) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        AnimatedPanelIcon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun ColorPickerButton(currentColor: Color, onColorSelected: (Color) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(currentColor.copy(alpha = 0.3f))
            .clickable { showPicker = true },
        contentAlignment = Alignment.Center
    ) {
        AnimatedPanelIcon(
            imageVector = Icons.Default.Palette,
            contentDescription = "Farbe ändern",
            tint = currentColor,
            modifier = Modifier.size(14.dp),
            riveArtboard = "palette",
            riveStateMachine = "pulse"
        )
    }

    if (showPicker) {
        ColorPickerDialog(
            currentColor = currentColor,
            onColorSelected = {
                onColorSelected(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Panel-Farbe wählen", color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(M3EColors.allColors.size) { index ->
                    val color = M3EColors.allColors[index]
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == currentColor) {
                            AnimatedPanelIcon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

val defaultRailIcons = listOf(
    RailIconItem(Icons.Default.Star, M3EColors.Limepop, "Favoriten"),
    RailIconItem(Icons.Default.Lock, M3EColors.Cherryfire, "Sicherheit"),
    RailIconItem(Icons.Default.Settings, M3EColors.Oceanneon, "Einstellungen"),
    RailIconItem(Icons.Default.Favorite, M3EColors.Sunsetcoral, "Likes"),
    RailIconItem(Icons.Default.Notifications, M3EColors.Auroraint, "Benachrichtigungen"),
)
