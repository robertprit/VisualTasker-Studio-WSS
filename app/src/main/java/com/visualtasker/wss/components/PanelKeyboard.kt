package com.visualtasker.wss.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PanelKey(
    val action: String,
    val label: String = action,
    val weight: Float = 1f
)

private val functionRows = listOf(
    listOf("F1", "F2", "F3", "F4", "F5", "F6"),
    listOf("F7", "F8", "F9", "F10", "F11", "F12")
)

private val navigationRows = listOf(
    listOf(
        PanelKey("POS1"),
        PanelKey("HOME"),
        PanelKey("PGUP"),
        PanelKey("PGDN"),
        PanelKey("INS"),
        PanelKey("DEL"),
        PanelKey("END")
    ),
    listOf(
        PanelKey("UP", "↑"),
        PanelKey("DOWN", "↓"),
        PanelKey("LEFT", "←"),
        PanelKey("RIGHT", "→"),
        PanelKey("TAB"),
        PanelKey("ENTER", "↵")
    )
)

private val symbolRows = listOf(
    listOf("(", ")", "{", "}", "[", "]"),
    listOf("<", ">", "=", "+", "-", "_"),
    listOf("/", "\\", "|", ";", ":", "\""),
    listOf("'", "`", "~", "@", "#", "$")
)

private val numpadRows = listOf(
    listOf(PanelKey("7"), PanelKey("8"), PanelKey("9")),
    listOf(PanelKey("4"), PanelKey("5"), PanelKey("6")),
    listOf(PanelKey("1"), PanelKey("2"), PanelKey("3")),
    listOf(PanelKey("0", "0", 2f), PanelKey(".", ".")),
)

@Composable
fun PanelKeyboard(
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        functionRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        key = PanelKey(action = key),
                        modifier = Modifier.weight(1f),
                        onClick = onKeyPress
                    )
                }
            }
        }

        navigationRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = onKeyPress
                    )
                }
            }
        }

        symbolRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        key = PanelKey(action = key),
                        modifier = Modifier.weight(1f),
                        onClick = onKeyPress
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.weight(2f)) {
                KeyboardKey(
                    key = PanelKey("SPACE", "Leer", 1f),
                    onClick = onKeyPress,
                    fixedHeight = 42.dp
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                KeyboardKey(
                    key = PanelKey("BACK", "⌫", 1f),
                    onClick = onKeyPress,
                    fixedHeight = 42.dp
                )
            }
        }

        Text(
            text = "NumPad",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        numpadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        key = key,
                        modifier = Modifier.weight(key.weight),
                        onClick = onKeyPress
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    key: PanelKey,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    fixedHeight: androidx.compose.ui.unit.Dp = 34.dp
) {
    Box(
        modifier = modifier
            .height(fixedHeight)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick(key.action) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

