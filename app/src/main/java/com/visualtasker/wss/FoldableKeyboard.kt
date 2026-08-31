package com.visualtasker.wss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KeyboardMode { BOTTOM_SHEET, FLOATING }

data class KeyboardSettings(
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val repeatDelayMs: Long = 400L,
)

private data class KeyData(
    val label: String,
    val width: Float = 1f,
    val type: KeyType = KeyType.NORMAL,
)

private enum class KeyType {
    NORMAL, MODIFIER, SPACE, BACKSPACE, TOGGLE,
}

private val numberKeys = listOf(
    listOf(
        KeyData("1"), KeyData("2"), KeyData("3"), KeyData("4"), KeyData("5"),
        KeyData("6"), KeyData("7"), KeyData("8"), KeyData("9"), KeyData("0"),
    )
)

private val leftKeys = listOf(
    listOf(KeyData("Q"), KeyData("W"), KeyData("E"), KeyData("R"), KeyData("T")),
    listOf(KeyData("A"), KeyData("S"), KeyData("D"), KeyData("F"), KeyData("G")),
    listOf(KeyData("Z"), KeyData("X"), KeyData("C"), KeyData("V"), KeyData("B")),
)

private val rightKeys = listOf(
    listOf(KeyData("Y"), KeyData("U"), KeyData("I"), KeyData("O"), KeyData("P")),
    listOf(KeyData("H"), KeyData("J"), KeyData("K"), KeyData("L"), KeyData(";", type = KeyType.NORMAL)),
    listOf(KeyData("N"), KeyData("M"), KeyData(","), KeyData("."), KeyData("/")),
)

@Composable
fun KeyboardIMEView(
    mode: KeyboardMode,
    isNumpadMode: Boolean,
    capsLock: Boolean,
    @Suppress("UNUSED_PARAMETER") settings: KeyboardSettings,
    onKeyPress: (String) -> Unit,
    onToggleNumpadMode: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
) {
    if (mode == KeyboardMode.FLOATING) {
        FloatingKeyboardContent(
            isNumpadMode = isNumpadMode,
            capsLock = capsLock,
            onKeyPress = onKeyPress,
            onToggleNumpadMode = onToggleNumpadMode,
            onToggleMode = onToggleMode,
            onOpenSettings = onOpenSettings,
            onCut = onCut,
            onCopy = onCopy,
            onPaste = onPaste,
        )
    } else {
        BottomSheetKeyboardContent(
            isNumpadMode = isNumpadMode,
            capsLock = capsLock,
            onKeyPress = onKeyPress,
            onToggleNumpadMode = onToggleNumpadMode,
            onToggleMode = onToggleMode,
            onOpenSettings = onOpenSettings,
            onCut = onCut,
            onCopy = onCopy,
            onPaste = onPaste,
        )
    }
}

@Composable
fun FoldableKeyboardScreen() {
    var isNumpadMode by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var capsLock by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(KeyboardMode.BOTTOM_SHEET) }
    var settings by remember { mutableStateOf(KeyboardSettings()) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surface,
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxSize().padding(16.dp),
                textStyle = MaterialTheme.typography.headlineMedium,
                placeholder = { Text("Tippe hier...") },
            )
        }

        KeyboardIMEView(
            mode = mode,
            isNumpadMode = isNumpadMode,
            capsLock = capsLock,
            settings = settings,
            onKeyPress = { key ->
                when (key) {
                    "BACK" -> if (inputText.isNotEmpty()) inputText = inputText.dropLast(1)
                    "SPACE" -> inputText += " "
                    "ENTER" -> inputText += "\n"
                    "CAPS" -> capsLock = !capsLock
                    else -> inputText += if (capsLock) key.uppercase() else key.lowercase()
                }
            },
            onToggleNumpadMode = { isNumpadMode = !isNumpadMode },
            onToggleMode = {
                mode = when (mode) {
                    KeyboardMode.BOTTOM_SHEET -> KeyboardMode.FLOATING
                    KeyboardMode.FLOATING -> KeyboardMode.BOTTOM_SHEET
                }
            },
            onOpenSettings = { /* TODO settings dialog */ },
            onCut = {},
            onCopy = {},
            onPaste = {},
        )
    }
}

@Composable
private fun BottomSheetKeyboardContent(
    isNumpadMode: Boolean,
    capsLock: Boolean,
    onKeyPress: (String) -> Unit,
    onToggleNumpadMode: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column {
            KeyboardToolbar(
                isNumpadMode = isNumpadMode,
                mode = KeyboardMode.BOTTOM_SHEET,
                onToggleNumpadMode = onToggleNumpadMode,
                onToggleMode = onToggleMode,
                onOpenSettings = onOpenSettings,
                onCut = onCut,
                onCopy = onCopy,
                onPaste = onPaste,
            )
            KeyboardBody(
                isNumpadMode = isNumpadMode,
                capsLock = capsLock,
                onKeyPress = onKeyPress,
            )
        }
    }
}

@Composable
private fun FloatingKeyboardContent(
    isNumpadMode: Boolean,
    capsLock: Boolean,
    onKeyPress: (String) -> Unit,
    onToggleNumpadMode: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column {
                KeyboardToolbar(
                    isNumpadMode = isNumpadMode,
                    mode = KeyboardMode.FLOATING,
                    onToggleNumpadMode = onToggleNumpadMode,
                    onToggleMode = onToggleMode,
                    onOpenSettings = onOpenSettings,
                    onCut = onCut,
                    onCopy = onCopy,
                    onPaste = onPaste,
                )
                KeyboardBody(
                    isNumpadMode = isNumpadMode,
                    capsLock = capsLock,
                    onKeyPress = onKeyPress,
                )
            }
        }
    }
}

@Composable
private fun KeyboardToolbar(
    isNumpadMode: Boolean,
    mode: KeyboardMode,
    onToggleNumpadMode: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarIcon(
            icon = null,
            label = if (isNumpadMode) "ABC" else "123",
            contentDescription = if (isNumpadMode) "Textmodus" else "Ziffernblock",
            onClick = onToggleNumpadMode,
        )
        ToolbarIcon(
            icon = if (mode == KeyboardMode.BOTTOM_SHEET) Icons.Default.KeyboardArrowUp
                   else Icons.Default.KeyboardArrowDown,
            contentDescription = if (mode == KeyboardMode.BOTTOM_SHEET) "Floating" else "Bottom Sheet",
            label = if (mode == KeyboardMode.BOTTOM_SHEET) "⬡" else "▬",
            onClick = onToggleMode,
        )
        ToolbarIcon(
            icon = Icons.Default.Settings,
            contentDescription = "Einstellungen",
            onClick = onOpenSettings,
        )
        ToolbarIcon(
            icon = null,
            label = "✂",
            contentDescription = "Ausschneiden",
            onClick = onCut,
        )
        ToolbarIcon(
            icon = null,
            label = "📋",
            contentDescription = "Kopieren",
            onClick = onCopy,
        )
        ToolbarIcon(
            icon = null,
            label = "📄",
            contentDescription = "Einfügen",
            onClick = onPaste,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ToolbarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    contentDescription: String,
    onClick: () -> Unit,
    label: String? = null,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(contentDescription) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            if (label != null) {
                Text(text = label, fontSize = 13.sp)
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KeyboardBody(
    isNumpadMode: Boolean,
    capsLock: Boolean,
    onKeyPress: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
    ) {
        if (isNumpadMode) {
            NumpadSpecialView(onKeyPress)
        } else {
            FullKeyboardView(capsLock, onKeyPress)
        }
    }
}

private data class NumpadKeyData(
    val action: String,
    val display: String = action,
)

private val numpadSpecialKeys = listOf(
    listOf(
        NumpadKeyData("7"), NumpadKeyData("8"), NumpadKeyData("9"),
        NumpadKeyData("BACK", "⌫"),
    ),
    listOf(
        NumpadKeyData("4"), NumpadKeyData("5"), NumpadKeyData("6"),
        NumpadKeyData("UP", "↑"),
    ),
    listOf(
        NumpadKeyData("1"), NumpadKeyData("2"), NumpadKeyData("3"),
        NumpadKeyData("DOWN", "↓"),
    ),
    listOf(
        NumpadKeyData("LEFT", "←"), NumpadKeyData("RIGHT", "→"),
        NumpadKeyData("PGUP", "PgUp"), NumpadKeyData("PGDN", "PgDn"),
    ),
    listOf(
        NumpadKeyData("@"), NumpadKeyData("$"),
        NumpadKeyData("("), NumpadKeyData(")"),
    ),
    listOf(
        NumpadKeyData("\""), NumpadKeyData("#"),
        NumpadKeyData("!"), NumpadKeyData("/"),
    ),
    listOf(
        NumpadKeyData("+"), NumpadKeyData("-"),
        NumpadKeyData("×"), NumpadKeyData("÷"),
    ),
    listOf(
        NumpadKeyData("="), NumpadKeyData("_"),
        NumpadKeyData("|"), NumpadKeyData("<"),
    ),
    listOf(
        NumpadKeyData(">"), NumpadKeyData("0"), NumpadKeyData("."),
        NumpadKeyData("SPACE", "␣"),
    ),
)

@Composable
private fun NumpadSpecialView(
    onKeyPress: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        numpadSpecialKeys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { key ->
                    NumpadKey(key = key, onKeyPress = onKeyPress)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun NumpadKey(
    key: NumpadKeyData,
    onKeyPress: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(48.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onKeyPress(key.action) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.display,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Full keyboard (unfolded state) ─────────────────────────────────────

@Composable
private fun FullKeyboardView(
    capsLock: Boolean,
    onKeyPress: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NumberRowView(capsLock, onKeyPress)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                KeyGrid(leftKeys, capsLock, onKeyPress)
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            )
            Box(modifier = Modifier.weight(1f)) {
                KeyGrid(rightKeys, capsLock, onKeyPress)
            }
        }

        BottomRow(capsLock, onKeyPress)
    }
}

@Composable
private fun NumberRowView(
    @Suppress("UNUSED_PARAMETER") capsLock: Boolean,
    onKeyPress: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        numberKeys.first().forEach { key ->
            NumberKey(key = key, capsLock = capsLock, onKeyPress = onKeyPress)
        }
    }
}

@Composable
private fun RowScope.NumberKey(
    key: KeyData,
    @Suppress("UNUSED_PARAMETER") capsLock: Boolean,
    onKeyPress: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(key.width)
            .height(36.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onKeyPress(key.label) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun KeyGrid(
    rows: List<List<KeyData>>,
    capsLock: Boolean,
    onKeyPress: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { key ->
                    Key(key = key, capsLock = capsLock, onKeyPress = onKeyPress)
                }
            }
        }
    }
}

@Composable
private fun RowScope.Key(
    key: KeyData,
    capsLock: Boolean,
    onKeyPress: (String) -> Unit,
) {
    val bg = when (key.type) {
        KeyType.MODIFIER -> MaterialTheme.colorScheme.secondaryContainer
        KeyType.SPACE, KeyType.BACKSPACE -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val label = when {
        key.type == KeyType.BACKSPACE -> "⌫"
        key.type == KeyType.SPACE -> "␣"
        key.type == KeyType.MODIFIER -> if (capsLock) "⇪" else "⇧"
        capsLock -> key.label.uppercase()
        else -> key.label.lowercase()
    }

    Box(
        modifier = Modifier
            .weight(key.width)
            .aspectRatio(1.3f)
            .shadow(2.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable { onKeyPress(key.label) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BottomRow(
    capsLock: Boolean,
    onKeyPress: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(40.dp)
                .shadow(2.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (capsLock) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer
                )
                .clickable { onKeyPress("CAPS") },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⇪",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (capsLock) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .shadow(2.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onKeyPress("SPACE") },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Leerzeichen",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        Box(
            modifier = Modifier
                .width(56.dp)
                .height(40.dp)
                .shadow(2.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable { onKeyPress("BACK") },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⌫",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
