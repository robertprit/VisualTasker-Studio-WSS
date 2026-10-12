package com.visualtasker.wss.workspace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.visualtasker.wss.workspace.model.PanelType
import com.visualtasker.wss.workspace.model.ProjectExplorerItem
import com.visualtasker.wss.workspace.model.ProjectExplorerItemKind
import com.visualtasker.wss.workspace.model.ProjectExplorerProjection
import com.visualtasker.wss.workspace.model.ProjectExplorerSectionKind
import com.visualtasker.wss.workspace.model.ProjectExplorerUiState
import com.visualtasker.wss.workspace.model.rootIdOrNull
import de.visualtasker.workflow.core.WorkflowRootId

internal data class ProjectExplorerPanelBindings(
    val projection: ProjectExplorerProjection,
    val uiState: ProjectExplorerUiState,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val diagnosticMessage: String?,
    val onUiStateChange: (ProjectExplorerUiState) -> Unit,
    val onCreateRoot: (String) -> Unit,
    val onRenameRoot: (WorkflowRootId, String) -> Unit,
    val onDeleteItem: (ProjectExplorerItem) -> Unit,
    val onOpenRoot: (WorkflowRootId, PanelType) -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
)

@Composable
internal fun ProjectExplorerPanel(
    projection: ProjectExplorerProjection,
    uiState: ProjectExplorerUiState,
    canUndo: Boolean,
    canRedo: Boolean,
    diagnosticMessage: String?,
    onUiStateChange: (ProjectExplorerUiState) -> Unit,
    onCreateRoot: (String) -> Unit,
    onRenameRoot: (WorkflowRootId, String) -> Unit,
    onDeleteItem: (ProjectExplorerItem) -> Unit,
    onOpenRoot: (WorkflowRootId, PanelType) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    var dialog by remember { mutableStateOf<ProjectExplorerDialog?>(null) }
    val selected = projection.item(uiState.selectedItemKey)
    val query = uiState.searchQuery.trim()
    val visibleSections = projection.sections.mapNotNull { section ->
        val filtered = if (query.isBlank()) section.items else section.items.filter { item ->
            item.label.contains(query, ignoreCase = true) ||
                item.detail.contains(query, ignoreCase = true) ||
                item.stableId.contains(query, ignoreCase = true)
        }
        section.takeIf { filtered.isNotEmpty() || query.isBlank() }?.copy(items = filtered)
    }

    Column(modifier = Modifier.fillMaxSize().testTag("project-explorer")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(projection.projectName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Revision ${projection.projectRevision}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.semantics { contentDescription = "Projektänderung rückgängig" }) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.semantics { contentDescription = "Projektänderung wiederholen" }) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null)
            }
            FilledTonalIconButton(onClick = { dialog = ProjectExplorerDialog.CreateRoot }, modifier = Modifier.semantics { contentDescription = "Workflow anlegen" }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { onUiStateChange(uiState.copy(searchQuery = it)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            singleLine = true,
            label = { Text("Projekt durchsuchen") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
        diagnosticMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            visibleSections.forEach { section ->
                val expanded = query.isNotBlank() || section.kind in uiState.expandedSections
                item(key = "section:${section.kind.name}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project-section:${section.kind.name}")
                            .clickable {
                                val next = if (expanded) uiState.expandedSections - section.kind else uiState.expandedSections + section.kind
                                onUiStateChange(uiState.copy(expandedSections = next))
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(section.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text(section.items.size.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (expanded) {
                    items(section.items, key = { it.key }) { item ->
                        ProjectExplorerRow(
                            item = item,
                            selected = item.key == uiState.selectedItemKey,
                            onClick = { onUiStateChange(uiState.copy(selectedItemKey = item.key)) },
                        )
                    }
                }
            }
        }
        HorizontalDivider()
        ProjectExplorerInspector(
            item = selected,
            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp, max = 176.dp),
            onRename = { item -> dialog = ProjectExplorerDialog.RenameRoot(item, item.label) },
            onDelete = { item -> dialog = ProjectExplorerDialog.DeleteItem(item) },
            onOpenRoot = onOpenRoot,
        )
    }

    when (val pending = dialog) {
        ProjectExplorerDialog.CreateRoot -> ProjectExplorerNameDialog(
            title = "Workflow anlegen",
            initialValue = "Workflow ${projection.sections.firstOrNull { it.kind == ProjectExplorerSectionKind.Workflows }?.items?.size?.plus(1) ?: 1}",
            confirmLabel = "Anlegen",
            onDismiss = { dialog = null },
            onConfirm = { name -> dialog = null; onCreateRoot(name) },
        )
        is ProjectExplorerDialog.RenameRoot -> ProjectExplorerNameDialog(
            title = "Workflow umbenennen",
            initialValue = pending.name,
            confirmLabel = "Umbenennen",
            onDismiss = { dialog = null },
            onConfirm = { name -> dialog = null; pending.item.rootIdOrNull()?.let { onRenameRoot(it, name) } },
        )
        is ProjectExplorerDialog.DeleteItem -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("${pending.item.label} löschen?") },
            text = { Text("Referenzierte, ausgeführte oder vom Recorder verwendete Inhalte werden nicht gelöscht und erzeugen stattdessen eine Diagnose.") },
            confirmButton = {
                Button(onClick = { dialog = null; onDeleteItem(pending.item) }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Abbrechen") } },
        )
        null -> Unit
    }
}

@Composable
private fun ProjectExplorerRow(item: ProjectExplorerItem, selected: Boolean, onClick: () -> Unit) {
    val statusText = when {
        item.active -> "Aktiv"
        item.missing -> "Fehlt"
        else -> null
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 1.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("project-item:${item.key}")
                .heightIn(min = 48.dp)
                .clickable(enabled = item.enabled, onClick = onClick)
                .padding(start = 14.dp, end = 8.dp, top = 5.dp, bottom = 5.dp)
                .semantics { contentDescription = listOfNotNull(item.label, statusText, item.detail).joinToString(", ") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(itemIcon(item.kind), contentDescription = null, tint = itemColor(item), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    statusText?.let { Text(it, color = itemColor(item), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                }
                Text(item.detail, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.whereUsedCount > 0) Text("×${item.whereUsedCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectExplorerInspector(
    item: ProjectExplorerItem?,
    modifier: Modifier,
    onRename: (ProjectExplorerItem) -> Unit,
    onDelete: (ProjectExplorerItem) -> Unit,
    onOpenRoot: (WorkflowRootId, PanelType) -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        if (item == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ein Element auswählen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.fillMaxHeight().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (item.kind == ProjectExplorerItemKind.Workflow) {
                        IconButton(onClick = { onRename(item) }) { Icon(Icons.Default.Edit, contentDescription = "Umbenennen") }
                    }
                    if (item.kind in setOf(ProjectExplorerItemKind.Workflow, ProjectExplorerItemKind.Definition, ProjectExplorerItemKind.Resource)) {
                        IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, contentDescription = "Löschen") }
                    }
                }
                Text(item.stableId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.detail, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.rootIdOrNull()?.let { rootId ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExplorerOpenButton(Icons.AutoMirrored.Filled.Article, "Text") { onOpenRoot(rootId, PanelType.TextEditor) }
                        ExplorerOpenButton(Icons.Default.ViewKanban, "Block") { onOpenRoot(rootId, PanelType.BlockEditor) }
                        ExplorerOpenButton(Icons.Default.AccountTree, "Flow") { onOpenRoot(rootId, PanelType.Flowchart) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorerOpenButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label)
        Spacer(Modifier.width(2.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ProjectExplorerNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { Button(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

private fun itemIcon(kind: ProjectExplorerItemKind): ImageVector = when (kind) {
    ProjectExplorerItemKind.Workflow -> Icons.Default.AccountTree
    ProjectExplorerItemKind.Definition -> Icons.Default.Functions
    ProjectExplorerItemKind.Variable -> Icons.AutoMirrored.Filled.Article
    ProjectExplorerItemKind.Resource -> Icons.Default.Image
    ProjectExplorerItemKind.Diagnostic -> Icons.Default.BugReport
}

@Composable
private fun itemColor(item: ProjectExplorerItem): Color = when {
    item.missing -> MaterialTheme.colorScheme.error
    item.active -> MaterialTheme.colorScheme.primary
    item.kind == ProjectExplorerItemKind.Resource -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private sealed interface ProjectExplorerDialog {
    data object CreateRoot : ProjectExplorerDialog
    data class RenameRoot(val item: ProjectExplorerItem, val name: String) : ProjectExplorerDialog
    data class DeleteItem(val item: ProjectExplorerItem) : ProjectExplorerDialog
}
