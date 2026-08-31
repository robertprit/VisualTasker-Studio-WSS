package com.visualtasker.wss.workspace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.visualtasker.wss.logging.StudioLogFilters
import com.visualtasker.wss.logging.StudioLogLevel
import com.visualtasker.wss.logging.StudioLogStore
import com.visualtasker.wss.ui.theme.M3EColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class LogConsoleUiState {
    var query by mutableStateOf("")
    var selectedLevels by mutableStateOf(StudioLogLevel.entries.toSet())
    var selectedSources by mutableStateOf(emptySet<String>())
    var autoScroll by mutableStateOf(true)
}

@Composable
internal fun LogConsolePanel(
    store: StudioLogStore,
    uiState: LogConsoleUiState
) {
    val token = store.changeToken
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.GERMANY) }
    val filters = StudioLogFilters(
        levels = uiState.selectedLevels,
        sources = uiState.selectedSources,
        query = uiState.query
    )
    val entries = remember(token, uiState.selectedLevels, uiState.selectedSources, uiState.query) {
        store.visibleEntries(filters)
    }
    val expandedIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(entries.size, uiState.autoScroll, store.isPaused) {
        if (uiState.autoScroll && !store.isPaused && entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TooltipIconButton(
                tooltip = if (store.isPaused) "Fortsetzen" else "Pausieren",
                onClick = { store.setEmissionPaused(!store.isPaused) },
            ) {
                Icon(
                    imageVector = if (store.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (store.isPaused) "Fortsetzen" else "Pausieren"
                )
            }
            TooltipIconButton(
                tooltip = if (uiState.autoScroll) "Auto-Scroll aus" else "Auto-Scroll an",
                onClick = { uiState.autoScroll = !uiState.autoScroll },
            ) {
                Icon(
                    imageVector = if (uiState.autoScroll) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                    contentDescription = if (uiState.autoScroll) "Auto-Scroll an" else "Auto-Scroll aus"
                )
            }
            TooltipIconButton(
                tooltip = "Sichtbare löschen",
                onClick = { store.clearVisible(filters) },
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Sichtbare loeschen")
            }
            Text(
                text = "Einträge: ${entries.size}${if (store.isPaused) " (PAUSIERT)" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = M3EColors.Amber,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.id }) { _, entry ->
                val isExpanded = expandedIds[entry.id] == true
                val levelTag = entry.level.name.padEnd(7, ' ')
                val sourceTag = entry.source.padEnd(10, ' ')
                val repeatTag = if (entry.repeatCount > 1) " × ${entry.repeatCount}" else ""
                val headline = "${timeFormat.format(Date(entry.timestamp))}  $levelTag  $sourceTag  ${entry.message}$repeatTag"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = levelColor(entry.level),
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TooltipIconButton(
                                tooltip = "Details umschalten",
                                onClick = { expandedIds[entry.id] = !isExpanded }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                                    contentDescription = "Details umschalten"
                                )
                            }
                            TooltipIconButton(
                                tooltip = "Eintrag kopieren",
                                onClick = {
                                    val copy = buildString {
                                        append(headline)
                                        entry.details?.let {
                                            append("\n")
                                            append(it)
                                        }
                                    }
                                    clipboard.setText(AnnotatedString(copy))
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Eintrag kopieren")
                            }
                        }
                    }
                    if (isExpanded) {
                        entry.documentRevision?.let { rev ->
                            Text(
                                text = "Revision: $rev",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        entry.details?.let { details ->
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.LogConsoleCompactRail(
    store: StudioLogStore,
    uiState: LogConsoleUiState
) {
    val compactIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
    val filters = StudioLogFilters(
        levels = uiState.selectedLevels,
        sources = uiState.selectedSources,
        query = uiState.query
    )
    TooltipIconButton(tooltip = if (store.isPaused) "Fortsetzen" else "Pausieren", onClick = { store.setEmissionPaused(!store.isPaused) }) {
        Icon(
            imageVector = if (store.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = if (store.isPaused) "Fortsetzen" else "Pausieren",
            tint = compactIconTint
        )
    }
    TooltipIconButton(tooltip = if (uiState.autoScroll) "Auto-Scroll aus" else "Auto-Scroll an", onClick = { uiState.autoScroll = !uiState.autoScroll }) {
        Icon(
            imageVector = if (uiState.autoScroll) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
            contentDescription = if (uiState.autoScroll) "Auto-Scroll an" else "Auto-Scroll aus",
            tint = compactIconTint
        )
    }
    TooltipIconButton(tooltip = "Sichtbare löschen", onClick = { store.clearVisible(filters) }) {
        Icon(Icons.Default.DeleteSweep, contentDescription = "Sichtbare loeschen", tint = compactIconTint)
    }
}

@Composable
internal fun ColumnScope.LogConsoleExpandedRail(
    store: StudioLogStore,
    uiState: LogConsoleUiState
) {
    val token = store.changeToken
    val sources = remember(token) { store.availableSources() }
    Text(
        text = "Filter",
        style = MaterialTheme.typography.labelMedium
    )
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { uiState.query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Suche") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
            singleLine = true
        )
        Text(
            text = "Level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StudioLogLevel.entries.forEach { level ->
            val selected = level in uiState.selectedLevels
            AssistChip(
                onClick = {
                    uiState.selectedLevels = if (selected) {
                        (uiState.selectedLevels - level).ifEmpty { setOf(level) }
                    } else {
                        uiState.selectedLevels + level
                    }
                },
                label = { Text(level.name) },
                leadingIcon = { Text(if (selected) "✓" else "•") }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Quellen",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AssistChip(
            onClick = { uiState.selectedSources = emptySet() },
            label = { Text("Alle") },
            leadingIcon = { Text(if (uiState.selectedSources.isEmpty()) "✓" else "•") }
        )
        sources.forEach { source ->
            val selected = source in uiState.selectedSources
            AssistChip(
                onClick = {
                    uiState.selectedSources = if (selected) {
                        uiState.selectedSources - source
                    } else {
                        uiState.selectedSources + source
                    }
                },
                label = { Text(source) },
                leadingIcon = { Text(if (selected) "✓" else "•") }
            )
        }
    }
}

private fun levelColor(level: StudioLogLevel): Color = when (level) {
    StudioLogLevel.DEBUG -> Color(0xFF90A4AE)
    StudioLogLevel.INFO -> Color(0xFF80CBC4)
    StudioLogLevel.WARNING -> Color(0xFFFFCC80)
    StudioLogLevel.ERROR -> Color(0xFFEF9A9A)
}
