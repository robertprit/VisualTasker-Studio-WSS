package com.visualtasker.wss.workspace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.editor.EmScriptEditorScreen
import com.visualtasker.wss.emscript.editor.EmscriptEditorSession
import com.visualtasker.wss.emscript.editor.EmscriptEditorUiState
import com.visualtasker.wss.emscript.editor.SyntaxHighlighter
import com.visualtasker.wss.logging.StudioLogLevel
import com.visualtasker.wss.logging.StudioLogStore
import com.visualtasker.wss.ui.theme.M3EColors
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer

internal const val EMSCRIPT_STATUS_READ_ONLY_PROJECTION = "READ_ONLY_PROJECTION"
internal const val EMSCRIPT_PROJECTION_STATUS_RUNNING = "RUNNING"
internal const val EMSCRIPT_EDITING_STATUS_NOT_IMPLEMENTED = "NOT_IMPLEMENTED"

internal class EmscriptFileManagerUiState {
    var currentName by mutableStateOf("draft")
    val scripts = mutableStateMapOf<String, String>()
}

@Composable
internal fun EmscriptTextEditorPanel(
    session: EmscriptEditorSession,
    uiState: EmscriptEditorUiState,
    latestEmscriptProjected: String,
    onSessionChange: (EmscriptEditorSession) -> Unit,
    logStore: StudioLogStore,
    workspaceJson: String,
    onWorkspaceJsonChange: (String) -> Unit
) {
    val applyGuard = remember { EmscriptApplyGuard() }
    var pendingApplyJson by remember { mutableStateOf<String?>(null) }
    var applyDiagnostics by remember { mutableStateOf<List<String>>(emptyList()) }

    fun buildApplyPreview(): String? {
        val manual = session.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
            ?: return null
        return when (val preview = applyGuard.preview(manual.content, workspaceId = "workflow-main")) {
            is EmscriptApplyGuardResult.Failure -> {
                applyDiagnostics = listOf(preview.message)
                logStore.append(
                    level = StudioLogLevel.ERROR,
                    source = "EMSCRIPT",
                    message = "Apply Guard abgebrochen",
                    details = preview.message,
                    documentRevision = workspaceJson.hashCode().toLong(),
                    groupKey = "emscript:apply:${preview.stage.name.lowercase()}"
                )
                null
            }
            is EmscriptApplyGuardResult.Success -> {
                pendingApplyJson = preview.serializedWorkspaceJson
                applyDiagnostics = listOf("Apply Vorschau bereit.")
                preview.summary
            }
        }
    }

    EmScriptEditorScreen(
        session = session,
        projectionStatus = EMSCRIPT_PROJECTION_STATUS_RUNNING,
        overallStatus = EMSCRIPT_STATUS_READ_ONLY_PROJECTION,
        revision = workspaceJson.hashCode(),
        uiState = uiState,
        onSessionChange = onSessionChange,
        onSaveDraft = {
            val manual = session.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
            logStore.append(
                level = StudioLogLevel.INFO,
                source = "EMSCRIPT",
                message = "Lokaler Draft gespeichert",
                details = "Zeichen=${manual?.content?.length ?: 0}",
                documentRevision = workspaceJson.hashCode().toLong(),
                groupKey = "emscript:draft-saved"
            )
        },
        onUseProjection = {
            onSessionChange(session.copyGeneratedToManual())
            logStore.append(
                level = StudioLogLevel.INFO,
                source = "EMSCRIPT",
                message = "Projektion in lokalen Draft übernommen",
                details = "Workspace bleibt unverändert",
                documentRevision = workspaceJson.hashCode().toLong(),
                groupKey = "emscript:draft-replaced-by-projection"
            )
        },
        canApplyDraft = session.activeTab.id == EmscriptEditorSession.MANUAL_TAB_ID,
        onRequestApplyPreview = ::buildApplyPreview,
        onConfirmApply = {
            val nextJson = pendingApplyJson ?: buildApplyPreview()?.let { pendingApplyJson }
            if (nextJson != null) {
                onWorkspaceJsonChange(nextJson)
                applyDiagnostics = listOf("Apply erfolgreich: Workspace atomar ersetzt und erneut validiert.")
                logStore.append(
                    level = StudioLogLevel.INFO,
                    source = "EMSCRIPT",
                    message = "Apply erfolgreich",
                    details = "Blockeditor+Flowchart aktualisiert",
                    documentRevision = nextJson.hashCode().toLong(),
                    groupKey = "emscript:apply:success"
                )
                pendingApplyJson = null
            }
        },
        diagnostics = applyDiagnostics + listOf(
            "EMScript Parser-Slice ist integriert (LET/SET/Literale/Variablen/Arithmetik/Compare/IF).",
            "Generierte Projektion: ${latestEmscriptProjected.length} Zeichen."
        ),
        syntaxPaletteOverride = SyntaxHighlighter.Palette(
            keyword = M3EColors.Oceanneon,
            control = M3EColors.Ultraviolet,
            parameter = Color(0xFFFFB74D),
            string = Color(0xFF81C784),
            number = Color(0xFFFFF176),
            comment = Color(0xFF90A4AE),
            operator = Color(0xFFFF8A65),
            plain = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
internal fun ColumnScope.EmscriptCompactRail(
    onExpandRequested: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    canLoad: Boolean
) {
    val compactIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
    TooltipIconButton(
        tooltip = "Dateimanager öffnen",
        onClick = onExpandRequested
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = "Dateimanager", tint = compactIconTint)
    }
    TooltipIconButton(
        tooltip = "Script speichern",
        onClick = onSave
    ) {
        Icon(Icons.Default.Save, contentDescription = "Speichern", tint = compactIconTint)
    }
    TooltipIconButton(
        tooltip = "Script laden",
        onClick = onLoad,
        enabled = canLoad
    ) {
        Icon(Icons.Default.Upload, contentDescription = "Laden", tint = compactIconTint)
    }
}

@Composable
internal fun ColumnScope.EmscriptExpandedRail(
    manager: EmscriptFileManagerUiState,
    onSave: () -> Unit,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit
) {
    Text(
        text = "Script-Dateien",
        style = MaterialTheme.typography.labelMedium
    )
    OutlinedTextField(
        value = manager.currentName,
        onValueChange = { manager.currentName = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Name") }
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TooltipIconButton(tooltip = "Neu", onClick = onNew) {
            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Neu")
        }
        TooltipIconButton(tooltip = "Speichern", onClick = onSave) {
            Icon(Icons.Default.Save, contentDescription = "Speichern")
        }
        TooltipIconButton(
            tooltip = "Laden",
            onClick = { onLoad(manager.currentName.trim()) },
            enabled = manager.scripts.containsKey(manager.currentName.trim())
        ) {
            Icon(Icons.Default.Upload, contentDescription = "Laden")
        }
        TooltipIconButton(
            tooltip = "Löschen",
            onClick = { onDelete(manager.currentName.trim()) },
            enabled = manager.currentName.trim().isNotBlank() && manager.currentName.trim() != "draft"
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Löschen")
        }
    }
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        manager.scripts.keys.sorted().forEach { name ->
            AssistChip(
                onClick = {
                    manager.currentName = name
                    onLoad(name)
                },
                label = { Text(name) },
                leadingIcon = { Text(if (name == manager.currentName) "✓" else "•") },
                trailingIcon = {
                    if (name != "draft") {
                        Icon(Icons.Default.Close, contentDescription = "Löschen")
                    }
                }
            )
        }
    }
}

@Composable
internal fun DebugInfoPanel(
    projectionStatus: String,
    editingStatus: String,
    overallStatus: String,
    revision: Int,
    projectedScript: String,
    draft: String,
    onSaveDraft: () -> Unit,
    onUseProjection: () -> Unit,
    diagnostics: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "EMScript-Projektion: $projectionStatus | EMScript-Bearbeitung: $editingStatus",
            color = M3EColors.Amber,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "EMScript-Gesamtstatus: $overallStatus | Revision: $revision",
            color = M3EColors.Amber,
            style = MaterialTheme.typography.labelMedium
        )
        diagnostics.forEach { message ->
            Text(
                text = "• $message",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = onUseProjection,
                label = { Text("Projektion in Entwurf übernehmen") }
            )
            AssistChip(
                onClick = onSaveDraft,
                label = { Text("Draft lokal speichern") }
            )
        }
        Text(
            text = "LOKALER ENTWURF - NICHT AUF WORKSPACE ANGEWENDET",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Text(
                text = draft.ifBlank { "// Leerer Workspace" },
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "GENERIERTE PROJEKTION",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Text(
                text = projectedScript.ifBlank { "// Leerer Workspace" },
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

internal fun generateEmscriptProjection(workspaceJson: String): Result<String> =
    runCatching {
        EmscriptGenerator().generate(WorkspaceSerializer.deserialize(workspaceJson))
    }
