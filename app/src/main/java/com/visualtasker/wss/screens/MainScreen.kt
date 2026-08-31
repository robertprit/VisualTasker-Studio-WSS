package com.visualtasker.wss.screens

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.visualtasker.wss.components.DarkPanel
import com.visualtasker.wss.components.FabAction
import com.visualtasker.wss.components.IconMotionConfig
import com.visualtasker.wss.components.IconMotionEngine
import com.visualtasker.wss.components.ListTestPanel
import com.visualtasker.wss.components.M3EExpandableFAB
import com.visualtasker.wss.components.PanelKeyboard
import com.visualtasker.wss.components.BrowserPanel
import com.visualtasker.wss.components.BrowserPanelState
import com.visualtasker.wss.data.PanelSessionSnapshot
import com.visualtasker.wss.data.PanelSessionStore
import com.visualtasker.wss.data.PanelState
import com.visualtasker.wss.data.PanelType
import com.visualtasker.wss.emscript.editor.EmScriptEditorScreen
import com.visualtasker.wss.emscript.editor.EmscriptEditorDiagnostic
import com.visualtasker.wss.emscript.editor.EmscriptEditorDiagnosticSeverity
import com.visualtasker.wss.emscript.editor.EmscriptEditorSession
import com.visualtasker.wss.emscript.editor.EmscriptEditorUiState
import com.visualtasker.wss.emscript.editor.SyntaxHighlighter
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.grid.GridSystem
import com.visualtasker.wss.logging.StudioLogStore
import com.visualtasker.wss.logging.StudioLogLevel
import com.visualtasker.wss.logging.StudioLogFilters
import com.visualtasker.wss.logging.StudioLogEntry
import com.visualtasker.wss.overlay.StudioOverlayService
import com.visualtasker.wss.ui.theme.M3EColors
import com.visualtasker.wss.flowchart.BlockEditorFlowchartProjector
import com.visualtasker.wss.flowchart.FlowchartProjectionStatus
import de.visualtasker.blockeditor.compose.host.BlockEditorController
import de.visualtasker.blockeditor.compose.host.BlockEditorHost
import de.visualtasker.blockeditor.compose.host.BlockEditorHostCallbacks
import de.visualtasker.blockeditor.compose.host.BlockEditorHostUiConfig
import de.visualtasker.blockeditor.compose.host.BlockEditorRuntimeState
import de.visualtasker.blockeditor.compose.icons.BlockIcons
import de.visualtasker.blockeditor.compose.icons.CategoryIcons
import de.visualtasker.blockeditor.compose.theme.blockEditorColors
import de.visualtasker.blockeditor.compose.theme.defaultBlockCategoryColor
import de.visualtasker.blockeditor.compose.theme.setBlockCategoryColorOverride
import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.BlockRegistry
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.blockeditor.domain.BlockNode
import de.visualtasker.blockeditor.domain.FieldValue
import de.visualtasker.blockeditor.domain.WorkspaceGraph
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.blockeditor.validation.ValidationError
import de.visualtasker.blockeditor.validation.Validator
import de.visualtasker.flowchart.compose.FlowchartHost
import de.visualtasker.flowchart.compose.FlowchartHostCallbacks
import de.visualtasker.flowchart.compose.FlowchartColorTokens
import de.visualtasker.flowchart.compose.FlowchartNodeShapeProvider
import de.visualtasker.flowchart.compose.FlowchartUiConfig
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowSurfaceId
import de.visualtasker.flowchart.interaction.FlowchartController
import de.visualtasker.flowchart.interaction.FlowInteractionAction
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGenerator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File

private const val EMSCRIPT_STATUS_READ_ONLY_PROJECTION = "READ_ONLY_PROJECTION"
private const val EMSCRIPT_PROJECTION_STATUS_RUNNING = "RUNNING"
private const val EMSCRIPT_EDITING_STATUS_NOT_IMPLEMENTED = "NOT_IMPLEMENTED"

private enum class FloatingOverlayTarget {
    PANEL,
    TOOLBAR,
    INSPECTOR,
}

private class LogConsoleUiState {
    var autoScroll by mutableStateOf(true)
    var query by mutableStateOf("")
    var selectedLevels by mutableStateOf(StudioLogLevel.entries.toSet())
    var selectedSources by mutableStateOf<Set<String>>(emptySet())
}

private class FlowchartPanelUiState {
    var gridVisible by mutableStateOf(true)
    var compactView by mutableStateOf(false)
    var inspectorVisible by mutableStateOf(false)
    var selectedNodeId by mutableStateOf<String?>(null)
}

private class EmscriptFileManagerUiState {
    var currentName by mutableStateOf("draft")
    val scripts = mutableStateMapOf<String, String>()
}

private data class StudioAppearance(
    val syntaxKeyword: Color = Color(0xFF82B1FF),
    val syntaxControl: Color = Color(0xFFCE93D8),
    val syntaxString: Color = Color(0xFF81C784),
    val syntaxNumber: Color = Color(0xFF81C784),
    val syntaxComment: Color = Color(0xFF9E9E9E),
    val syntaxOperator: Color = Color(0xFFFFB74D),
    val syntaxPlain: Color = Color(0xFFE0E0E0),
    val flowEvent: Color = Color(0xFF5B470A),
    val flowControl: Color = Color(0xFF6C3F16),
    val flowLogic: Color = Color(0xFF1E4C71),
    val flowVariable: Color = Color(0xFF1F5A36),
    val blockEvent: Color = defaultBlockCategoryColor(BlockCategories.EVENT),
    val blockAction: Color = defaultBlockCategoryColor(BlockCategories.ACTION),
    val blockEmscript: Color = defaultBlockCategoryColor(BlockCategories.EMSCRIPT),
    val blockInput: Color = defaultBlockCategoryColor(BlockCategories.INPUT),
    val blockPerception: Color = defaultBlockCategoryColor(BlockCategories.PERCEPTION),
    val blockControl: Color = defaultBlockCategoryColor(BlockCategories.CONTROL),
    val blockLogic: Color = defaultBlockCategoryColor(BlockCategories.LOGIC),
    val blockVariables: Color = defaultBlockCategoryColor(BlockCategories.VARIABLES),
    val blockFlow: Color = defaultBlockCategoryColor(BlockCategories.FLOW),
    val blockRuntime: Color = defaultBlockCategoryColor(BlockCategories.RUNTIME),
    val blockDebug: Color = defaultBlockCategoryColor(BlockCategories.DEBUG),
    val blockVariable: Color = defaultBlockCategoryColor(BlockCategories.VARIABLE),
    val blockCustom: Color = defaultBlockCategoryColor(BlockCategories.CUSTOM),
)

private data class ApplyGuardPreview(
    val summary: String,
    val importedDocument: WorkspaceDocument,
)

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun MainScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onWorkspaceScreenRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiPrefs = remember(context) { context.getSharedPreferences("panel_ui_options", Context.MODE_PRIVATE) }
    val sessionStore = remember(context) { PanelSessionStore(context) }
    val saved = remember { sessionStore.load() }
    val initialBlockEditorDocument = remember(uiPrefs) {
        val persisted = uiPrefs.getString("blockeditor_workspace_json", null)
        persisted
            ?.let { runCatching { WorkspaceSerializer.deserialize(it) }.getOrNull() }
            ?: WorkspaceBootstrap.starter()
    }
    val initialEmscriptDraft = remember(uiPrefs) {
        uiPrefs.getString("emscript_editor_draft", "").orEmpty()
    }
    val initialEmscriptDraftDirty = remember(uiPrefs) {
        uiPrefs.getBoolean("emscript_editor_draft_dirty", false)
    }
    var appearance by remember(uiPrefs) {
        mutableStateOf(
            StudioAppearance(
                syntaxKeyword = loadColorPref(uiPrefs, "color.syntax.keyword", Color(0xFF82B1FF)),
                syntaxControl = loadColorPref(uiPrefs, "color.syntax.control", Color(0xFFCE93D8)),
                syntaxString = loadColorPref(uiPrefs, "color.syntax.string", Color(0xFF81C784)),
                syntaxNumber = loadColorPref(uiPrefs, "color.syntax.number", Color(0xFF81C784)),
                syntaxComment = loadColorPref(uiPrefs, "color.syntax.comment", Color(0xFF9E9E9E)),
                syntaxOperator = loadColorPref(uiPrefs, "color.syntax.operator", Color(0xFFFFB74D)),
                syntaxPlain = loadColorPref(uiPrefs, "color.syntax.plain", Color(0xFFE0E0E0)),
                flowEvent = loadColorPref(uiPrefs, "color.flow.event", loadColorPref(uiPrefs, "color.block.event", Color(0xFF5B470A))),
                flowControl = loadColorPref(uiPrefs, "color.flow.control", loadColorPref(uiPrefs, "color.block.control", Color(0xFF6C3F16))),
                flowLogic = loadColorPref(uiPrefs, "color.flow.logic", loadColorPref(uiPrefs, "color.block.logic", Color(0xFF1E4C71))),
                flowVariable = loadColorPref(uiPrefs, "color.flow.variable", loadColorPref(uiPrefs, "color.block.variable", Color(0xFF1F5A36))),
                blockEvent = loadColorPref(uiPrefs, "color.block.event", defaultBlockCategoryColor(BlockCategories.EVENT)),
                blockAction = loadColorPref(uiPrefs, "color.block.action", defaultBlockCategoryColor(BlockCategories.ACTION)),
                blockEmscript = loadColorPref(uiPrefs, "color.block.emscript", defaultBlockCategoryColor(BlockCategories.EMSCRIPT)),
                blockInput = loadColorPref(uiPrefs, "color.block.input", defaultBlockCategoryColor(BlockCategories.INPUT)),
                blockPerception = loadColorPref(uiPrefs, "color.block.perception", defaultBlockCategoryColor(BlockCategories.PERCEPTION)),
                blockControl = loadColorPref(uiPrefs, "color.block.control", defaultBlockCategoryColor(BlockCategories.CONTROL)),
                blockLogic = loadColorPref(uiPrefs, "color.block.logic", defaultBlockCategoryColor(BlockCategories.LOGIC)),
                blockVariables = loadColorPref(uiPrefs, "color.block.variables", defaultBlockCategoryColor(BlockCategories.VARIABLES)),
                blockFlow = loadColorPref(uiPrefs, "color.block.flow", defaultBlockCategoryColor(BlockCategories.FLOW)),
                blockRuntime = loadColorPref(uiPrefs, "color.block.runtime", defaultBlockCategoryColor(BlockCategories.RUNTIME)),
                blockDebug = loadColorPref(uiPrefs, "color.block.debug", defaultBlockCategoryColor(BlockCategories.DEBUG)),
                blockVariable = loadColorPref(uiPrefs, "color.block.variable", defaultBlockCategoryColor(BlockCategories.VARIABLE)),
                blockCustom = loadColorPref(uiPrefs, "color.block.custom", defaultBlockCategoryColor(BlockCategories.CUSTOM)),
            ),
        )
    }

    var hideSystemBars by remember { mutableStateOf(uiPrefs.getBoolean("hide_system_bars", false)) }
    var dockAtTop by remember { mutableStateOf(uiPrefs.getBoolean("dock_top", false)) }
    var useLargeGrid by remember { mutableStateOf(uiPrefs.getBoolean("grid_large", false)) }
    var uiScale by remember { mutableStateOf(uiPrefs.getFloat("ui_scale", 1f).coerceIn(0.7f, 1.5f)) }
    var snapEnabled by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAddPanelSheet by remember { mutableStateOf(false) }
    var settingsTab by remember { mutableIntStateOf(0) }
    var screenSize by remember { mutableStateOf(Offset(0f, 0f)) }
    var previousScreenSize by remember { mutableStateOf<Offset?>(null) }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, uiScale) {
        Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * uiScale,
        )
    }
    val density = scaledDensity.density

    val gridSizeDp = if (useLargeGrid) GridSystem.GRID_SIZE_DP_LARGE else GridSystem.GRID_SIZE_DP_SMALL
    val accentPalette = M3EColors.allColors
    val panels = remember {
        mutableStateListOf<PanelState>().apply {
            addAll(saved?.panels.orEmpty().map(::migrateLegacyPanelTitle))
        }
    }
    var nextZIndex by remember { mutableIntStateOf((panels.maxOfOrNull { it.zIndex } ?: 0) + 1) }
    var activeTargetPanelId by remember { mutableStateOf(saved?.activeTargetPanelId ?: "1") }
    var focusedPanelId by remember { mutableStateOf(saved?.activeTargetPanelId ?: "1") }

    LaunchedEffect(Unit) {
        panels.indices.forEach { index ->
            val migrated = migrateLegacyPanelTitle(panels[index])
            if (migrated != panels[index]) {
                panels[index] = migrated
            }
        }
    }

    val editorValues = remember {
        mutableStateMapOf<String, TextFieldValue>().apply {
            val initialTexts = saved?.panelTexts ?: emptyMap()
            val initialCursors = saved?.panelCursors ?: emptyMap()
            initialTexts.forEach { (id, text) ->
                val cursor = initialCursors[id]?.coerceIn(0, text.length) ?: text.length
                this[id] = TextFieldValue(text = text, selection = TextRange(cursor))
            }
        }
    }
    val insertModes = remember { mutableStateMapOf<String, Boolean>().apply { putAll(saved?.insertModes ?: emptyMap()) } }
    val functionActions = remember {
        mutableStateMapOf<String, String>().apply {
            putAll(defaultFunctionKeyActions())
            saved?.functionKeyActions?.forEach { (k, v) -> this[k] = v }
        }
    }
    val browserStates = remember { mutableStateMapOf<String, BrowserPanelState>() }
    val logConsoleStates = remember { mutableStateMapOf<String, LogConsoleUiState>() }
    val flowchartPanelStates = remember { mutableStateMapOf<String, FlowchartPanelUiState>() }
    val emscriptFileManager = remember {
        EmscriptFileManagerUiState().apply {
            val restoredScripts = saved?.emscriptFileManagerScripts.orEmpty()
            if (restoredScripts.isNotEmpty()) {
                scripts.putAll(restoredScripts)
            } else {
                scripts["draft"] = initialEmscriptDraft
            }
            currentName = saved?.emscriptFileManagerCurrentName
                ?.takeIf { it.isNotBlank() }
                ?: "draft"
        }
    }
    val emscriptEditorUiState = remember {
        EmscriptEditorUiState().apply {
            fontSizeSp = saved?.emscriptFontSizeSp?.coerceIn(9f, 24f) ?: 12f
            selectionStarts.putAll(saved?.emscriptSelectionStarts.orEmpty())
            selectionEnds.putAll(saved?.emscriptSelectionEnds.orEmpty())
            saved?.emscriptFoldedKeysByTab
                .orEmpty()
                .forEach { (tabId, csv) ->
                    val values = csv.split('|').map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    foldedKeysByTab[tabId] = values
                }
        }
    }
    var latestBlockEditorDocument by remember { mutableStateOf(initialBlockEditorDocument) }
    var latestBlockEditorRuntime by remember { mutableStateOf<BlockEditorRuntimeState?>(null) }
    var latestBlockEditorValidationErrors by remember { mutableStateOf<List<ValidationError>>(emptyList()) }
    var latestEmscriptProjected by remember { mutableStateOf("") }
    var latestEmscriptGenerationFailure by remember { mutableStateOf<String?>(null) }
    var emscriptDraftValue by remember {
        mutableStateOf(TextFieldValue(initialEmscriptDraft, TextRange(initialEmscriptDraft.length)))
    }
    var emscriptDraftDirty by remember { mutableStateOf(initialEmscriptDraftDirty) }
    var emscriptRevision by remember { mutableIntStateOf(initialBlockEditorDocument.version.toInt()) }
    var emscriptStatus by remember { mutableStateOf(EMSCRIPT_STATUS_READ_ONLY_PROJECTION) }
    var emscriptSession by remember {
        mutableStateOf(
            EmscriptEditorSession.create(
                manualContent = initialEmscriptDraft,
                generatedContent = latestEmscriptProjected,
            ).selectTab(saved?.emscriptActiveTabId ?: EmscriptEditorSession.MANUAL_TAB_ID)
        )
    }
    val emscriptImporter = remember { EmscriptWorkspaceImporter() }
    var editorParserDiagnostics by remember { mutableStateOf<List<String>>(emptyList()) }
    var editorApplyDiagnostics by remember { mutableStateOf<List<String>>(emptyList()) }
    var editorPreparedWorkspace by remember { mutableStateOf<WorkspaceDocument?>(null) }
    var lastEditorParserFingerprint by remember { mutableStateOf<String?>(null) }
    var flowchartProjectionStatus by remember { mutableStateOf(FlowchartProjectionStatus.DEGRADED) }
    var lastRuntimeStatusName by remember { mutableStateOf<String?>(null) }
    var lastValidationFingerprint by remember { mutableStateOf<String?>(null) }
    val studioLogStore = remember { StudioLogStore(maxEntries = 800) }
    var projectedFlowGraph by remember {
        mutableStateOf<FlowGraphDocument>(
            BlockEditorFlowchartProjector.project(initialBlockEditorDocument).graph,
        )
    }
    val sharedBlockEditorCallbacks = remember {
        object : BlockEditorHostCallbacks {
            override fun onWorkspaceDocumentChanged(serializedJson: String) {
                uiPrefs.edit().putString("blockeditor_workspace_json", serializedJson).apply()
                runCatching { WorkspaceSerializer.deserialize(serializedJson) }
                    .onSuccess { doc ->
                        val previousStatus = flowchartProjectionStatus
                        latestBlockEditorDocument = doc
                        emscriptRevision = doc.version.toInt()
                        val projection = BlockEditorFlowchartProjector.project(doc)
                        projectedFlowGraph = projection.graph
                        flowchartProjectionStatus = projection.status
                        studioLogStore.append(
                            level = StudioLogLevel.DEBUG,
                            source = "WORKSPACE",
                            message = "Workspace-Dokument aktualisiert",
                            details = "Revision ${doc.version}",
                            documentRevision = doc.version,
                            groupKey = "workspace:revision-updated",
                        )
                        if (projection.status != previousStatus) {
                            studioLogStore.append(
                                level = if (projection.status == FlowchartProjectionStatus.RUNNING) {
                                    StudioLogLevel.INFO
                                } else {
                                    StudioLogLevel.WARNING
                                },
                                source = "FLOWCHART",
                                message = "Projektionsstatus: ${projection.status}",
                                details = "Vorher: $previousStatus",
                                documentRevision = doc.version,
                                groupKey = "flowchart:status:${projection.status}",
                            )
                        }
                        projection.graph.diagnostics.forEach { diagnostic ->
                            studioLogStore.append(
                                level = StudioLogLevel.WARNING,
                                source = "FLOWCHART",
                                message = "Projektiondiagnose",
                                details = diagnostic.message,
                                documentRevision = doc.version,
                                groupKey = "flowchart:diag:${diagnostic.code}:${diagnostic.message}",
                            )
                        }
                    }
                    .onFailure { error ->
                        studioLogStore.append(
                            level = StudioLogLevel.ERROR,
                            source = "WORKSPACE",
                            message = "Workspace-Deserialize fehlgeschlagen",
                            details = error.message ?: "Unknown error",
                            groupKey = "workspace:deserialize-error",
                        )
                    }
            }

            override fun onEmscriptDraftChanged(emscript: String) {
                latestEmscriptProjected = emscript
                latestEmscriptGenerationFailure = null
                emscriptSession = emscriptSession.updateGeneratedFromBlocks(emscript)
                if (!emscriptDraftDirty) {
                    emscriptDraftValue = TextFieldValue(emscript, TextRange(emscript.length))
                    emscriptSession = emscriptSession.updateManualContent(emscript)
                }
                studioLogStore.append(
                    level = StudioLogLevel.INFO,
                    source = "EMSCRIPT",
                    message = "Projektion aktualisiert",
                    details = "Länge=${emscript.length}",
                    documentRevision = latestBlockEditorDocument.version,
                    groupKey = "emscript:projection-updated",
                )
            }

            override fun onEmscriptGenerationFailed(message: String) {
                latestEmscriptGenerationFailure = message
                emscriptSession = emscriptSession.updateGeneratedFromBlocks(
                    content = latestEmscriptProjected,
                    diagnostics = listOf(
                        EmscriptEditorDiagnostic(
                            severity = EmscriptEditorDiagnosticSeverity.ERROR,
                            message = message,
                            code = "projection.failed",
                        )
                    ),
                )
                studioLogStore.append(
                    level = StudioLogLevel.ERROR,
                    source = "EMSCRIPT",
                    message = "Projektion fehlgeschlagen",
                    details = message,
                    documentRevision = latestBlockEditorDocument.version,
                    groupKey = "emscript:projection-error:$message",
                )
            }

            override fun onValidationErrors(errors: List<ValidationError>) {
                latestBlockEditorValidationErrors = errors
                val newFingerprint = errors.joinToString(separator = "|") { it.message }
                if (errors.isEmpty() && lastValidationFingerprint?.isNotBlank() == true) {
                    studioLogStore.append(
                        level = StudioLogLevel.INFO,
                        source = "BLOCKEDITOR",
                        message = "Validierung wieder erfolgreich",
                        details = "Dokument ist wieder valide",
                        documentRevision = latestBlockEditorDocument.version,
                        groupKey = "blockeditor:validation-recovered",
                    )
                } else if (errors.isNotEmpty() && newFingerprint != lastValidationFingerprint) {
                    studioLogStore.append(
                        level = StudioLogLevel.WARNING,
                        source = "BLOCKEDITOR",
                        message = "Persistente Validierungsfehler (${errors.size})",
                        details = errors.joinToString("\n") { it.message },
                        documentRevision = latestBlockEditorDocument.version,
                        groupKey = "blockeditor:validation:${newFingerprint.hashCode()}",
                    )
                }
                lastValidationFingerprint = newFingerprint
            }
            override fun onRuntimeStateChanged(state: BlockEditorRuntimeState) {
                latestBlockEditorRuntime = state
                val statusName = state.status.name
                if (statusName != lastRuntimeStatusName) {
                    studioLogStore.append(
                        level = when (statusName) {
                            "RUNNING" -> StudioLogLevel.INFO
                            "RUNNING_WITH_GUARDS" -> StudioLogLevel.WARNING
                            else -> StudioLogLevel.ERROR
                        },
                        source = "BLOCKEDITOR",
                        message = "Runtime-Status: $statusName",
                        details = state.toString(),
                        documentRevision = latestBlockEditorDocument.version,
                        groupKey = "blockeditor:runtime:$statusName",
                    )
                    lastRuntimeStatusName = statusName
                }
            }
        }
    }
    val sharedBlockEditorController = remember {
        BlockEditorController(
            initialDocument = initialBlockEditorDocument,
            callbacks = sharedBlockEditorCallbacks,
        )
    }

    fun buildApplyPreviewFromDraft(draft: String): ApplyGuardPreview? {
        val importResult = emscriptImporter.import(draft, workspaceId = latestBlockEditorDocument.id)
        if (!importResult.isSuccess || importResult.document == null) {
            val firstIssue = importResult.issues.firstOrNull()
            val message = firstIssue?.let { "Parse/Import Fehler ${it.line}:${it.column} ${it.message}" }
                ?: "Parse/Import fehlgeschlagen"
            editorApplyDiagnostics = listOf(message)
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = "EMSCRIPT",
                message = "Apply Guard abgebrochen",
                details = message,
                documentRevision = latestBlockEditorDocument.version,
                groupKey = "emscript:apply:parse-import-failed",
            )
            return null
        }
        val imported = importResult.document
        val preValidation = Validator.validate(imported, sharedBlockEditorController.blockRegistry)
        if (!preValidation.isValid) {
            val firstError = preValidation.errors.first().message
            val message = "Pre-Validate fehlgeschlagen: $firstError"
            editorApplyDiagnostics = listOf(message)
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = "EMSCRIPT",
                message = "Apply Guard abgebrochen",
                details = message,
                documentRevision = latestBlockEditorDocument.version,
                groupKey = "emscript:apply:pre-validate-failed",
            )
            return null
        }
        val current = latestBlockEditorDocument
        val scriptRoundtrip = runCatching {
            EmscriptGenerator(IrGenerator(sharedBlockEditorController.blockRegistry)).generate(imported)
        }.getOrElse { error ->
            val message = "Roundtrip-Guard fehlgeschlagen: ${error.message ?: "unknown"}"
            editorApplyDiagnostics = listOf(message)
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = "EMSCRIPT",
                message = "Apply Guard abgebrochen",
                details = message,
                documentRevision = latestBlockEditorDocument.version,
                groupKey = "emscript:apply:roundtrip-failed",
            )
            return null
        }
        val preview = buildApplySemanticPreview(
            before = current,
            after = imported,
            registry = sharedBlockEditorController.blockRegistry,
            unsupportedCount = importResult.issues.count { it.message.contains("unsupported", ignoreCase = true) } +
                imported.blocks.values.count { sharedBlockEditorController.blockRegistry.getDefinition(it.type) == null },
            roundtripLength = scriptRoundtrip.length,
        )
        return ApplyGuardPreview(summary = preview, importedDocument = imported)
    }

    fun applyDraftWithGuards(draft: String) {
        val preview = buildApplyPreviewFromDraft(draft) ?: return
        val currentBeforeApply = latestBlockEditorDocument
        val applyResult = runCatching {
            sharedBlockEditorController.replaceWorkspaceDocument(
                newDocument = preview.importedDocument,
                recordHistory = true,
            )
            Validator.validate(sharedBlockEditorController.document, sharedBlockEditorController.blockRegistry)
        }
        val postValidation = applyResult.getOrElse { error ->
            sharedBlockEditorController.replaceWorkspaceDocument(
                newDocument = currentBeforeApply,
                recordHistory = false,
            )
            val message = "Apply-Ausnahme: ${error.message ?: "unknown"}, Workspace wurde zurückgesetzt."
            editorApplyDiagnostics = listOf(message)
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = "EMSCRIPT",
                message = "Apply fehlgeschlagen",
                details = message,
                documentRevision = latestBlockEditorDocument.version,
                groupKey = "emscript:apply:exception",
            )
            return
        }
        if (!postValidation.isValid) {
            sharedBlockEditorController.replaceWorkspaceDocument(
                newDocument = currentBeforeApply,
                recordHistory = false,
            )
            val message = "Post-Validate fehlgeschlagen, Workspace wurde vollständig zurückgesetzt."
            editorApplyDiagnostics = listOf(message, postValidation.errors.first().message)
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = "EMSCRIPT",
                message = "Apply fehlgeschlagen",
                details = "${postValidation.errors.first().message}\nRollback auf vorheriges WorkspaceDocument ausgeführt",
                documentRevision = latestBlockEditorDocument.version,
                groupKey = "emscript:apply:post-validate-failed",
            )
            return
        }
        editorApplyDiagnostics = listOf("Apply erfolgreich: Workspace atomar ersetzt und erneut validiert.")
        studioLogStore.append(
            level = StudioLogLevel.INFO,
            source = "EMSCRIPT",
            message = "Apply erfolgreich",
            details = "Undo-Eintrag erstellt, Blockeditor+Flowchart aktualisiert",
            documentRevision = sharedBlockEditorController.document.version,
            groupKey = "emscript:apply:success",
        )
    }

    LaunchedEffect(emscriptSession) {
        editorApplyDiagnostics = emptyList()
        val manual = emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
        if (manual == null) {
            editorParserDiagnostics = emptyList()
            editorPreparedWorkspace = null
            return@LaunchedEffect
        }
        val importResult = emscriptImporter.import(manual.content, workspaceId = latestBlockEditorDocument.id)
        if (importResult.isSuccess) {
            editorPreparedWorkspace = importResult.document
            editorParserDiagnostics = listOf(
                "Parser-Slice: READY (LET/SET, Literale, Variablen, Arithmetik, Compare, IF)",
                "Workspace-Import vorbereitet (Apply manuell mit Guard-Flow aktiv, Compile/Run weiter deaktiviert)",
            )
            if (lastEditorParserFingerprint != "ok") {
                studioLogStore.append(
                    level = StudioLogLevel.INFO,
                    source = "EMSCRIPT",
                    message = "Parser-Slice erfolgreich",
                    details = "IR und Workspace-Dokument aus Draft erzeugt",
                    documentRevision = latestBlockEditorDocument.version,
                    groupKey = "emscript:parser-ready",
                )
                lastEditorParserFingerprint = "ok"
            }
        } else {
            editorPreparedWorkspace = null
            val firstIssue = importResult.issues.firstOrNull()
            editorParserDiagnostics = buildList {
                add("Parser-Slice: ERROR")
                if (firstIssue != null) {
                    add("Zeile ${firstIssue.line}:${firstIssue.column} ${firstIssue.message}")
                }
            }
            val fingerprint = importResult.issues.joinToString("|") {
                "${it.line}:${it.column}:${it.message}"
            }
            if (fingerprint != lastEditorParserFingerprint) {
                studioLogStore.append(
                    level = StudioLogLevel.WARNING,
                    source = "EMSCRIPT",
                    message = "Parser-Slice Diagnose",
                    details = firstIssue?.let { "Zeile ${it.line}:${it.column} ${it.message}" } ?: "Unbekannter Fehler",
                    documentRevision = latestBlockEditorDocument.version,
                    groupKey = "emscript:parser-issue:${fingerprint.hashCode()}",
                )
                lastEditorParserFingerprint = fingerprint
            }
        }
    }

    LaunchedEffect(Unit) {
        studioLogStore.append(
            level = StudioLogLevel.INFO,
            source = "WORKSPACE",
            message = "Workspace geladen",
            details = "Panels=${panels.size}, Blockeditor-Revision=${initialBlockEditorDocument.version}",
            documentRevision = initialBlockEditorDocument.version,
            groupKey = "workspace:loaded",
        )
    }

    LaunchedEffect(hideSystemBars, dockAtTop, useLargeGrid, uiScale) {
        uiPrefs.edit()
            .putBoolean("hide_system_bars", hideSystemBars)
            .putBoolean("dock_top", dockAtTop)
            .putBoolean("grid_large", useLargeGrid)
            .putFloat("ui_scale", uiScale)
            .apply()
    }

    LaunchedEffect(appearance) {
        uiPrefs.edit()
            .putLong("color.syntax.keyword", appearance.syntaxKeyword.value.toLong())
            .putLong("color.syntax.control", appearance.syntaxControl.value.toLong())
            .putLong("color.syntax.string", appearance.syntaxString.value.toLong())
            .putLong("color.syntax.number", appearance.syntaxNumber.value.toLong())
            .putLong("color.syntax.comment", appearance.syntaxComment.value.toLong())
            .putLong("color.syntax.operator", appearance.syntaxOperator.value.toLong())
            .putLong("color.syntax.plain", appearance.syntaxPlain.value.toLong())
            .putLong("color.flow.event", appearance.flowEvent.value.toLong())
            .putLong("color.flow.control", appearance.flowControl.value.toLong())
            .putLong("color.flow.logic", appearance.flowLogic.value.toLong())
            .putLong("color.flow.variable", appearance.flowVariable.value.toLong())
            .putLong("color.block.event", appearance.blockEvent.value.toLong())
            .putLong("color.block.action", appearance.blockAction.value.toLong())
            .putLong("color.block.emscript", appearance.blockEmscript.value.toLong())
            .putLong("color.block.input", appearance.blockInput.value.toLong())
            .putLong("color.block.perception", appearance.blockPerception.value.toLong())
            .putLong("color.block.control", appearance.blockControl.value.toLong())
            .putLong("color.block.logic", appearance.blockLogic.value.toLong())
            .putLong("color.block.variables", appearance.blockVariables.value.toLong())
            .putLong("color.block.flow", appearance.blockFlow.value.toLong())
            .putLong("color.block.runtime", appearance.blockRuntime.value.toLong())
            .putLong("color.block.debug", appearance.blockDebug.value.toLong())
            .putLong("color.block.variable", appearance.blockVariable.value.toLong())
            .putLong("color.block.custom", appearance.blockCustom.value.toLong())
            .putInt("color.argb.syntax.keyword", appearance.syntaxKeyword.toArgb())
            .putInt("color.argb.syntax.control", appearance.syntaxControl.toArgb())
            .putInt("color.argb.syntax.string", appearance.syntaxString.toArgb())
            .putInt("color.argb.syntax.number", appearance.syntaxNumber.toArgb())
            .putInt("color.argb.syntax.comment", appearance.syntaxComment.toArgb())
            .putInt("color.argb.syntax.operator", appearance.syntaxOperator.toArgb())
            .putInt("color.argb.syntax.plain", appearance.syntaxPlain.toArgb())
            .putInt("color.argb.flow.event", appearance.flowEvent.toArgb())
            .putInt("color.argb.flow.control", appearance.flowControl.toArgb())
            .putInt("color.argb.flow.logic", appearance.flowLogic.toArgb())
            .putInt("color.argb.flow.variable", appearance.flowVariable.toArgb())
            .putInt("color.argb.block.event", appearance.blockEvent.toArgb())
            .putInt("color.argb.block.action", appearance.blockAction.toArgb())
            .putInt("color.argb.block.emscript", appearance.blockEmscript.toArgb())
            .putInt("color.argb.block.input", appearance.blockInput.toArgb())
            .putInt("color.argb.block.perception", appearance.blockPerception.toArgb())
            .putInt("color.argb.block.control", appearance.blockControl.toArgb())
            .putInt("color.argb.block.logic", appearance.blockLogic.toArgb())
            .putInt("color.argb.block.variables", appearance.blockVariables.toArgb())
            .putInt("color.argb.block.flow", appearance.blockFlow.toArgb())
            .putInt("color.argb.block.runtime", appearance.blockRuntime.toArgb())
            .putInt("color.argb.block.debug", appearance.blockDebug.toArgb())
            .putInt("color.argb.block.variable", appearance.blockVariable.toArgb())
            .putInt("color.argb.block.custom", appearance.blockCustom.toArgb())
            .commit()

        setBlockCategoryColorOverride(BlockCategories.EVENT, appearance.blockEvent)
        setBlockCategoryColorOverride(BlockCategories.ACTION, appearance.blockAction)
        setBlockCategoryColorOverride(BlockCategories.EMSCRIPT, appearance.blockEmscript)
        setBlockCategoryColorOverride(BlockCategories.INPUT, appearance.blockInput)
        setBlockCategoryColorOverride(BlockCategories.PERCEPTION, appearance.blockPerception)
        setBlockCategoryColorOverride(BlockCategories.CONTROL, appearance.blockControl)
        setBlockCategoryColorOverride(BlockCategories.LOGIC, appearance.blockLogic)
        setBlockCategoryColorOverride(BlockCategories.VARIABLES, appearance.blockVariables)
        setBlockCategoryColorOverride(BlockCategories.FLOW, appearance.blockFlow)
        setBlockCategoryColorOverride(BlockCategories.RUNTIME, appearance.blockRuntime)
        setBlockCategoryColorOverride(BlockCategories.DEBUG, appearance.blockDebug)
        setBlockCategoryColorOverride(BlockCategories.VARIABLE, appearance.blockVariable)
        setBlockCategoryColorOverride(BlockCategories.CUSTOM, appearance.blockCustom)
    }

    LaunchedEffect(hideSystemBars, context) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (hideSystemBars) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    LaunchedEffect(screenSize.x, screenSize.y) {
        if (screenSize.x <= 0f || screenSize.y <= 0f) return@LaunchedEffect
        val previous = previousScreenSize
        if (previous == null || previous.x <= 0f || previous.y <= 0f) {
            previousScreenSize = screenSize
            return@LaunchedEffect
        }
        if (previous == screenSize) return@LaunchedEffect

        val oldCenterX = previous.x / 2f
        val oldCenterY = previous.y / 2f
        val newCenterX = screenSize.x / 2f
        val newCenterY = screenSize.y / 2f

        panels.indices.forEach { i ->
            val panel = panels[i]
            val panelWidthPx = panel.width * density
            val panelHeightPx = panel.height * density
            val relativeFromCenterX = (panel.position.x - oldCenterX) / previous.x
            val relativeFromCenterY = (panel.position.y - oldCenterY) / previous.y
            val mappedX = newCenterX + relativeFromCenterX * screenSize.x
            val mappedY = newCenterY + relativeFromCenterY * screenSize.y
            val maxX = (screenSize.x - panelWidthPx - 16f).coerceAtLeast(0f)
            val maxY = (screenSize.y - panelHeightPx - 16f).coerceAtLeast(0f)
            panels[i] = panel.copy(
                position = Offset(
                    x = mappedX.coerceIn(0f, maxX),
                    y = mappedY.coerceIn(0f, maxY),
                ),
            )
        }
        previousScreenSize = screenSize
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            PanelSessionSnapshot(
                panels = panels.toList(),
                activeTargetPanelId = activeTargetPanelId,
                panelTexts = editorValues.mapValues { it.value.text },
                panelCursors = editorValues.mapValues { it.value.selection.start },
                insertModes = insertModes.toMap(),
                functionKeyActions = functionActions.toMap(),
                emscriptActiveTabId = emscriptSession.activeTabId,
                emscriptFontSizeSp = emscriptEditorUiState.fontSizeSp,
                emscriptSelectionStarts = emscriptEditorUiState.selectionStarts.toMap(),
                emscriptSelectionEnds = emscriptEditorUiState.selectionEnds.toMap(),
                emscriptFoldedKeysByTab = emscriptEditorUiState.foldedKeysByTab
                    .mapValues { entry -> entry.value.sorted().joinToString("|") },
                emscriptFileManagerCurrentName = emscriptFileManager.currentName,
                emscriptFileManagerScripts = emscriptFileManager.scripts.toMap(),
            )
        }.debounce(400).collect { sessionStore.save(it) }
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onSizeChanged { screenSize = Offset(it.width.toFloat(), it.height.toFloat()) }
        ) {
            if (snapEnabled) GridBackground(gridSizeDp)

        panels.sortedBy { it.zIndex }.forEach { panel ->
            if (panel.isMinimized) return@forEach
            key(panel.id) {
                val maxWidthDp = ((screenSize.x - panel.position.x - 16f) / density).toInt().coerceAtLeast(144)
                val maxHeightDp = ((screenSize.y - panel.position.y - 16f) / density).toInt().coerceAtLeast(144)
                DarkPanel(
                    panel = panel,
                    snapEnabled = snapEnabled,
                    gridSizeDp = gridSizeDp,
                    isActiveTarget = panel.id == focusedPanelId,
                    showRail = panel.panelType != PanelType.KEYBOARD,
                    showDefaultRailIcons = panel.panelType !in setOf(
                        PanelType.BROWSER,
                        PanelType.BLOCKEDITOR,
                        PanelType.LOG_CONSOLE,
                        PanelType.FLOWCHART,
                    ),
                    compactRailContent = { onExpandRequested ->
                        if (panel.panelType == PanelType.BROWSER) {
                            val state = browserStates.getOrPut(panel.id) { BrowserPanelState() }
                            BrowserCompactRail(
                                state = state,
                                onExpandRequested = onExpandRequested,
                            )
                        } else if (panel.panelType == PanelType.BLOCKEDITOR) {
                            BlockEditorPanelCompactRail(
                                controller = sharedBlockEditorController,
                                onExpandRequested = onExpandRequested,
                            )
                        } else if (panel.panelType == PanelType.LOG_CONSOLE) {
                            val state = logConsoleStates.getOrPut(panel.id) { LogConsoleUiState() }
                            LogConsoleCompactRail(
                                store = studioLogStore,
                                uiState = state,
                            )
                        } else if (panel.panelType == PanelType.FLOWCHART) {
                            FlowchartCompactShapeRail(onExpandRequested = onExpandRequested)
                        } else if (panel.panelType == PanelType.EDITOR) {
                            EmscriptCompactRail(
                                onExpandRequested = onExpandRequested,
                                onSave = {
                                    val manual = emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                    if (manual != null) {
                                        val key = emscriptFileManager.currentName.trim().ifBlank { "draft" }
                                        emscriptFileManager.currentName = key
                                        emscriptFileManager.scripts[key] = manual.content
                                        studioLogStore.append(
                                            level = StudioLogLevel.INFO,
                                            source = "EMSCRIPT",
                                            message = "Script gespeichert",
                                            details = "Name=$key",
                                            documentRevision = latestBlockEditorDocument.version,
                                            groupKey = "emscript:file-saved:$key",
                                        )
                                    }
                                },
                                onLoad = {
                                    val key = emscriptFileManager.currentName.trim().ifBlank { return@EmscriptCompactRail }
                                    val content = emscriptFileManager.scripts[key] ?: return@EmscriptCompactRail
                                    emscriptSession = emscriptSession
                                        .selectTab(EmscriptEditorSession.MANUAL_TAB_ID)
                                        .updateManualContent(content)
                                    emscriptDraftValue = TextFieldValue(content, TextRange(content.length))
                                    emscriptDraftDirty = true
                                    uiPrefs.edit()
                                        .putString("emscript_editor_draft", content)
                                        .putBoolean("emscript_editor_draft_dirty", true)
                                        .apply()
                                    studioLogStore.append(
                                        level = StudioLogLevel.INFO,
                                        source = "EMSCRIPT",
                                        message = "Script geladen",
                                        details = "Name=$key",
                                        documentRevision = latestBlockEditorDocument.version,
                                        groupKey = "emscript:file-loaded:$key",
                                    )
                                },
                                canLoad = emscriptFileManager.scripts.containsKey(emscriptFileManager.currentName.trim()),
                            )
                        }
                    },
                    railContent = {
                        if (panel.panelType == PanelType.BROWSER) {
                            val state = browserStates.getOrPut(panel.id) { BrowserPanelState() }
                            BrowserRailControls(state = state)
                        } else if (panel.panelType == PanelType.BLOCKEDITOR) {
                            BlockEditorPanelExpandedRail(controller = sharedBlockEditorController)
                        } else if (panel.panelType == PanelType.LOG_CONSOLE) {
                            val state = logConsoleStates.getOrPut(panel.id) { LogConsoleUiState() }
                            LogConsoleExpandedRail(
                                store = studioLogStore,
                                uiState = state,
                            )
                        } else if (panel.panelType == PanelType.FLOWCHART) {
                            FlowchartShapeLegendRail()
                        } else if (panel.panelType == PanelType.EDITOR) {
                            EmscriptExpandedRail(
                                manager = emscriptFileManager,
                                onSave = {
                                    val manual = emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                    if (manual != null) {
                                        val key = emscriptFileManager.currentName.trim().ifBlank { "draft" }
                                        emscriptFileManager.currentName = key
                                        emscriptFileManager.scripts[key] = manual.content
                                        studioLogStore.append(
                                            level = StudioLogLevel.INFO,
                                            source = "EMSCRIPT",
                                            message = "Script gespeichert",
                                            details = "Name=$key",
                                            documentRevision = latestBlockEditorDocument.version,
                                            groupKey = "emscript:file-saved:$key",
                                        )
                                    }
                                },
                                onLoad = { name ->
                                    val content = emscriptFileManager.scripts[name] ?: return@EmscriptExpandedRail
                                    emscriptFileManager.currentName = name
                                    emscriptSession = emscriptSession
                                        .selectTab(EmscriptEditorSession.MANUAL_TAB_ID)
                                        .updateManualContent(content)
                                    emscriptDraftValue = TextFieldValue(content, TextRange(content.length))
                                    emscriptDraftDirty = true
                                    uiPrefs.edit()
                                        .putString("emscript_editor_draft", content)
                                        .putBoolean("emscript_editor_draft_dirty", true)
                                        .apply()
                                    studioLogStore.append(
                                        level = StudioLogLevel.INFO,
                                        source = "EMSCRIPT",
                                        message = "Script geladen",
                                        details = "Name=$name",
                                        documentRevision = latestBlockEditorDocument.version,
                                        groupKey = "emscript:file-loaded:$name",
                                    )
                                },
                                onDelete = { name ->
                                    if (name == "draft") return@EmscriptExpandedRail
                                    emscriptFileManager.scripts.remove(name)
                                    if (emscriptFileManager.currentName == name) {
                                        emscriptFileManager.currentName = "draft"
                                    }
                                    studioLogStore.append(
                                        level = StudioLogLevel.WARNING,
                                        source = "EMSCRIPT",
                                        message = "Script gelöscht",
                                        details = "Name=$name",
                                        documentRevision = latestBlockEditorDocument.version,
                                        groupKey = "emscript:file-deleted:$name",
                                    )
                                },
                                onNew = {
                                    val base = "script"
                                    var idx = 1
                                    var next = "$base-$idx"
                                    while (emscriptFileManager.scripts.containsKey(next)) {
                                        idx++
                                        next = "$base-$idx"
                                    }
                                    emscriptFileManager.currentName = next
                                    emscriptFileManager.scripts[next] = ""
                                    emscriptSession = emscriptSession
                                        .selectTab(EmscriptEditorSession.MANUAL_TAB_ID)
                                        .updateManualContent("")
                                    emscriptDraftValue = TextFieldValue("", TextRange(0))
                                    emscriptDraftDirty = true
                                },
                            )
                        }
                    },
                    maxWidth = maxWidthDp,
                    maxHeight = maxHeightDp,
                    onPositionChange = { newPos ->
                        val i = panels.indexOfFirst { it.id == panel.id }
                        if (i >= 0) {
                            val p = panels[i]
                            val panelWidthPx = p.width * density
                            val panelHeightPx = p.height * density
                            val x = newPos.x.coerceIn(0f, (screenSize.x - panelWidthPx - 16f).coerceAtLeast(0f))
                            val y = newPos.y.coerceIn(0f, (screenSize.y - panelHeightPx - 16f).coerceAtLeast(0f))
                            panels[i] = p.copy(position = Offset(x, y))
                        }
                    },
                    onSizeChange = { w, h ->
                        val i = panels.indexOfFirst { it.id == panel.id }
                        if (i >= 0) {
                            val p = panels[i]
                            val reqW = w.coerceAtLeast(144)
                            val reqH = h.coerceAtLeast(144)
                            val boundW = ((screenSize.x - 16f) / density).toInt().coerceAtLeast(144)
                            val boundH = ((screenSize.y - 16f) / density).toInt().coerceAtLeast(144)
                            val newW = reqW.coerceAtMost(boundW)
                            val newH = reqH.coerceAtMost(boundH)
                            val newWidthPx = newW * density
                            val newHeightPx = newH * density
                            val newX = p.position.x.coerceAtMost((screenSize.x - newWidthPx - 16f).coerceAtLeast(0f))
                            val newY = p.position.y.coerceAtMost((screenSize.y - newHeightPx - 16f).coerceAtLeast(0f))
                            panels[i] = p.copy(position = Offset(newX, newY), width = newW, height = newH)
                        }
                    },
                    onZIndexChange = {
                        val i = panels.indexOfFirst { it.id == panel.id }
                        if (i >= 0) {
                            panels[i] = panels[i].copy(zIndex = nextZIndex)
                            nextZIndex++
                        }
                    },
                    onFocusRequest = {
                        focusedPanelId = panel.id
                        if (panel.panelType == PanelType.EDITOR) activeTargetPanelId = panel.id
                    },
                    onMinimizeToggle = {
                        val i = panels.indexOfFirst { it.id == panel.id }
                        if (i >= 0) panels[i] = panels[i].copy(isMinimized = !panels[i].isMinimized)
                    },
                    onMaximizeToggle = {
                        val i = panels.indexOfFirst { it.id == panel.id }
                        if (i >= 0) panels[i] = panels[i].copy(isMaximized = !panels[i].isMaximized)
                    },
                    onClose = {
                        val i = panels.indexOfFirst { it.id == panel.id }
                        if (i >= 0) {
                            val removed = panels.removeAt(i)
                            studioLogStore.append(
                                level = StudioLogLevel.INFO,
                                source = "WORKSPACE",
                                message = "Panel geschlossen",
                                details = "${removed.panelType} (${removed.title})",
                                groupKey = "workspace:panel-closed:${removed.panelType}",
                            )
                            editorValues.remove(removed.id)
                            insertModes.remove(removed.id)
                            browserStates.remove(removed.id)
                            logConsoleStates.remove(removed.id)
                            flowchartPanelStates.remove(removed.id)
                            if (focusedPanelId == removed.id) focusedPanelId = panels.lastOrNull()?.id ?: ""
                            if (activeTargetPanelId == removed.id) activeTargetPanelId = panels.firstOrNull { it.panelType == PanelType.EDITOR }?.id ?: ""
                        }
                    },
                    onColorChange = { c ->
                        val i = panels.indexOfFirst { it.id == panel.id }
                        if (i >= 0) panels[i] = panels[i].copy(accentColor = c)
                    }
                ) {
                    when (panel.panelType) {
                        PanelType.EDITOR -> EmScriptEditorScreen(
                            session = emscriptSession,
                            projectionStatus = EMSCRIPT_PROJECTION_STATUS_RUNNING,
                            overallStatus = emscriptStatus,
                            revision = emscriptRevision,
                            uiState = emscriptEditorUiState,
                            onSessionChange = {
                                emscriptSession = it
                                val manual = it.tabs.firstOrNull { tab -> tab.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                if (manual != null) {
                                    emscriptDraftValue = TextFieldValue(manual.content, TextRange(manual.content.length))
                                    emscriptDraftDirty = manual.dirty
                                }
                                uiPrefs.edit()
                                    .putString("emscript_editor_draft", emscriptDraftValue.text)
                                    .putBoolean("emscript_editor_draft_dirty", emscriptDraftDirty)
                                    .apply()
                            },
                            onSaveDraft = {
                                uiPrefs.edit()
                                    .putString("emscript_editor_draft", emscriptDraftValue.text)
                                    .putBoolean("emscript_editor_draft_dirty", emscriptDraftDirty)
                                    .apply()
                                studioLogStore.append(
                                    level = StudioLogLevel.INFO,
                                    source = "EMSCRIPT",
                                    message = "Lokaler Draft gespeichert",
                                    details = "Draft ist nicht auf Workspace angewendet",
                                    documentRevision = latestBlockEditorDocument.version,
                                    groupKey = "emscript:draft-saved",
                                )
                            },
                            onUseProjection = {
                                val projection = latestEmscriptProjected
                                emscriptDraftValue = TextFieldValue(projection, TextRange(projection.length))
                                emscriptDraftDirty = false
                                emscriptSession = emscriptSession.copyGeneratedToManual()
                                uiPrefs.edit()
                                    .putString("emscript_editor_draft", projection)
                                    .putBoolean("emscript_editor_draft_dirty", false)
                                    .apply()
                                studioLogStore.append(
                                    level = StudioLogLevel.INFO,
                                    source = "EMSCRIPT",
                                    message = "Projektion in lokalen Draft übernommen",
                                    details = "Workspace bleibt unverändert",
                                    documentRevision = latestBlockEditorDocument.version,
                                    groupKey = "emscript:draft-replaced-by-projection",
                                )
                            },
                            canApplyDraft = emscriptSession.activeTab.id == EmscriptEditorSession.MANUAL_TAB_ID,
                            onRequestApplyPreview = {
                                val manual = emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                if (manual == null) {
                                    null
                                } else {
                                    buildApplyPreviewFromDraft(manual.content)?.summary
                                }
                            },
                            onConfirmApply = {
                                val manual = emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                if (manual != null) {
                                    applyDraftWithGuards(manual.content)
                                }
                            },
                            diagnostics = editorApplyDiagnostics + editorParserDiagnostics,
                            syntaxPaletteOverride = SyntaxHighlighter.Palette(
                                keyword = appearance.syntaxKeyword,
                                control = appearance.syntaxControl,
                                parameter = Color(0xFFFFB74D),
                                string = appearance.syntaxString,
                                number = appearance.syntaxNumber,
                                comment = appearance.syntaxComment,
                                operator = appearance.syntaxOperator,
                                plain = appearance.syntaxPlain,
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )
                        PanelType.KEYBOARD -> {
                            Text(text = "Aktives Ziel: ${panels.firstOrNull { it.id == activeTargetPanelId }?.title ?: "Keins"}", color = M3EColors.Amber)
                            PanelKeyboard(
                                modifier = Modifier.padding(top = 4.dp),
                                onKeyPress = { key ->
                                    val target = panels.firstOrNull { it.id == activeTargetPanelId && it.panelType == PanelType.EDITOR } ?: return@PanelKeyboard
                                    val old = editorValues[target.id] ?: TextFieldValue("")
                                    val insert = insertModes[target.id] ?: false
                                    val result = applyKeyPress(old, key, insert, functionActions)
                                    editorValues[target.id] = result.first
                                    insertModes[target.id] = result.second
                                }
                            )
                        }
                        PanelType.LIST_TEST -> ListTestPanel(modifier = Modifier.fillMaxSize())
                        PanelType.BROWSER -> {
                            val state = browserStates.getOrPut(panel.id) {
                                BrowserPanelState("https://example.org")
                            }
                            BrowserPanel(state = state, modifier = Modifier.fillMaxSize())
                        }
                        PanelType.BLOCKEDITOR -> BlockEditorEmbeddedPanel(
                            modifier = Modifier.fillMaxSize(),
                            controller = sharedBlockEditorController,
                        )
                        PanelType.FLOWCHART -> FlowchartEmbeddedPanel(
                            modifier = Modifier.fillMaxSize(),
                            graphDocument = projectedFlowGraph,
                            panelUi = flowchartPanelStates.getOrPut(panel.id) { FlowchartPanelUiState() },
                            logStore = studioLogStore,
                            appearance = appearance,
                        )
                        PanelType.EMSCRIPT -> EmscriptEmbeddedPanel(
                            modifier = Modifier.fillMaxSize(),
                            projectionStatus = EMSCRIPT_PROJECTION_STATUS_RUNNING,
                            editingStatus = EMSCRIPT_EDITING_STATUS_NOT_IMPLEMENTED,
                            overallStatus = emscriptStatus,
                            revision = emscriptRevision,
                            projectedScript = latestEmscriptProjected,
                            draft = emscriptDraftValue,
                            onSaveDraft = {
                                uiPrefs.edit()
                                    .putString("emscript_editor_draft", emscriptDraftValue.text)
                                    .putBoolean("emscript_editor_draft_dirty", emscriptDraftDirty)
                                    .apply()
                                studioLogStore.append(
                                    level = StudioLogLevel.INFO,
                                    source = "EMSCRIPT",
                                    message = "Lokaler Draft gespeichert",
                                    details = "Draft ist nicht auf Workspace angewendet",
                                    documentRevision = latestBlockEditorDocument.version,
                                    groupKey = "emscript:draft-saved",
                                )
                            },
                            onUseProjection = {
                                val projection = latestEmscriptProjected
                                emscriptDraftValue = TextFieldValue(projection, TextRange(projection.length))
                                emscriptDraftDirty = false
                                uiPrefs.edit()
                                    .putString("emscript_editor_draft", projection)
                                    .putBoolean("emscript_editor_draft_dirty", false)
                                    .apply()
                                studioLogStore.append(
                                    level = StudioLogLevel.INFO,
                                    source = "EMSCRIPT",
                                    message = "Projektion in lokalen Draft übernommen",
                                    details = "Workspace bleibt unverändert",
                                    documentRevision = latestBlockEditorDocument.version,
                                    groupKey = "emscript:draft-replaced-by-projection",
                                )
                            },
                            diagnostics = buildList {
                                add("EMScript Parser-Slice ist integriert (LET/SET/Literale/Variablen/Arithmetik/Compare/IF).")
                                add("Automatisches Anwenden auf den Workspace bleibt vorerst deaktiviert.")
                                if (editorPreparedWorkspace != null) {
                                    add("Draft konnte erfolgreich in ein Workspace-Dokument übersetzt werden.")
                                }
                                latestEmscriptGenerationFailure?.let(::add)
                            },
                        )
                        PanelType.LOG_CONSOLE -> LogConsoleEmbeddedPanel(
                            modifier = Modifier.fillMaxSize(),
                            store = studioLogStore,
                            uiState = logConsoleStates.getOrPut(panel.id) { LogConsoleUiState() },
                        )
                    }
                }
            }
        }

        MinimizedPanelDock(
            panels = panels.filter { it.isMinimized },
            modifier = Modifier
                .align(if (dockAtTop) Alignment.TopStart else Alignment.BottomStart)
                .padding(start = 12.dp, end = 96.dp, top = if (dockAtTop) 12.dp else 0.dp, bottom = if (dockAtTop) 0.dp else 14.dp),
            onRestore = { id ->
                val i = panels.indexOfFirst { it.id == id }
                if (i >= 0) {
                    panels[i] = panels[i].copy(isMinimized = false, zIndex = nextZIndex++)
                    focusedPanelId = id
                }
            }
        )

            M3EExpandableFAB(
                actions = listOf(
                    FabAction(Icons.Default.AddCircle, "Neues Panel", M3EColors.Limepop) {
                        showAddPanelSheet = true
                    },
                    FabAction(Icons.Default.AutoAwesome, "Auto Anordnen", M3EColors.Oceanneon) {
                        val positions = GridSystem.autoArrangePositions(
                            panelCount = panels.size,
                            screenWidth = screenSize.x.toInt(),
                            screenHeight = screenSize.y.toInt(),
                            gridSizeDp = gridSizeDp
                        )
                        positions.forEachIndexed { i, pos ->
                            if (i < panels.size) panels[i] = panels[i].copy(position = pos, isMinimized = false, isMaximized = false)
                        }
                    },
                    FabAction(Icons.Default.Settings, "Einstellungen", M3EColors.Ultraviolet) { showSettingsSheet = true },
                    FabAction(Icons.Default.ViewList, "Workspace starten", M3EColors.Amber) {
                        onWorkspaceScreenRequested()
                    }
                ),
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            tabIndex = settingsTab,
            onTabChange = { settingsTab = it },
            currentPanel = panels.firstOrNull { it.id == focusedPanelId },
            functionActions = functionActions,
            appearance = appearance,
            onAppearanceChange = { appearance = it },
            hideSystemBars = hideSystemBars,
            onHideSystemBarsChange = { hideSystemBars = it },
            dockAtTop = dockAtTop,
            onDockAtTopChange = { dockAtTop = it },
            useLargeGrid = useLargeGrid,
            onUseLargeGridChange = { useLargeGrid = it },
            uiScale = uiScale,
            onUiScaleChange = { uiScale = it.coerceIn(0.7f, 1.5f) },
            snapEnabled = snapEnabled,
            onSnapEnabledChange = { snapEnabled = it },
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onResetPanels = {
                val arranged = GridSystem.autoArrangePositions(
                    panelCount = panels.size,
                    screenWidth = screenSize.x.toInt(),
                    screenHeight = screenSize.y.toInt(),
                    gridSizeDp = gridSizeDp
                )
                panels.indices.forEach { i ->
                    panels[i] = panels[i].copy(
                        position = arranged.getOrElse(i) { panels[i].position },
                        width = 320,
                        height = 260,
                        isMinimized = false,
                        isMaximized = false,
                        zIndex = i + 1
                    )
                }
                nextZIndex = panels.size + 1
            },
            onColorPick = { c ->
                val i = panels.indexOfFirst { it.id == focusedPanelId }
                if (i >= 0) panels[i] = panels[i].copy(accentColor = c)
            },
            onToggleIconEngine = {
                IconMotionConfig.engine = if (IconMotionConfig.engine == IconMotionEngine.MATERIAL) IconMotionEngine.RIVE else IconMotionEngine.MATERIAL
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showAddPanelSheet) {
        AddPanelSheet(
            onSelect = { type ->
                val newId = (panels.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1
                val title = when (type) {
                    PanelType.EDITOR -> "TextEditor $newId"
                    PanelType.LIST_TEST -> "Stepper $newId"
                    PanelType.BROWSER -> "Browser $newId"
                    PanelType.KEYBOARD -> "VisualTasker Studio WSS $newId"
                    PanelType.BLOCKEDITOR -> "BlockEditor $newId"
                    PanelType.FLOWCHART -> "Flowchart $newId"
                    PanelType.EMSCRIPT -> "Debug $newId"
                    PanelType.LOG_CONSOLE -> "Konsole $newId"
                }
                panels.add(
                    PanelState(
                        id = "$newId",
                        position = Offset(100f, 200f),
                        width = if (type == PanelType.KEYBOARD) 700 else 320,
                        height = if (type == PanelType.KEYBOARD) 420 else 260,
                        accentColor = accentPalette[panels.size % accentPalette.size],
                        title = title,
                        panelType = type,
                        zIndex = nextZIndex++
                    )
                )
                if (type == PanelType.EDITOR) {
                    editorValues[newId.toString()] = TextFieldValue("")
                    insertModes[newId.toString()] = false
                }
                if (type == PanelType.BROWSER) {
                    browserStates[newId.toString()] = BrowserPanelState("https://example.org")
                }
                studioLogStore.append(
                    level = StudioLogLevel.INFO,
                    source = "WORKSPACE",
                    message = "Panel geöffnet",
                    details = "$type ($title)",
                    groupKey = "workspace:panel-opened:$type",
                )
                showAddPanelSheet = false
            },
            onLaunchFloatingOverlay = { target ->
                if (!Settings.canDrawOverlays(context)) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    studioLogStore.append(
                        level = StudioLogLevel.WARNING,
                        source = "WORKSPACE",
                        message = "Overlay-Berechtigung erforderlich",
                        details = "Bitte Berechtigung aktivieren und erneut auswählen",
                        groupKey = "workspace:overlay-permission-required",
                    )
                    return@AddPanelSheet
                }
                val action = when (target) {
                    FloatingOverlayTarget.PANEL -> StudioOverlayService.ACTION_SHOW_FLOATING_PANEL
                    FloatingOverlayTarget.TOOLBAR -> StudioOverlayService.ACTION_SHOW_FLOATING_TOOLBAR
                    FloatingOverlayTarget.INSPECTOR -> StudioOverlayService.ACTION_SHOW_FLOATING_INSPECTOR
                }
                context.startService(
                    Intent(context, StudioOverlayService::class.java).apply {
                        this.action = action
                    },
                )
                studioLogStore.append(
                    level = StudioLogLevel.INFO,
                    source = "WORKSPACE",
                    message = "Floating Overlay gestartet",
                    details = target.name,
                    groupKey = "workspace:overlay-started:${target.name}",
                )
                showAddPanelSheet = false
            },
            onDismiss = { showAddPanelSheet = false }
        )
    }
}

private fun migrateLegacyPanelTitle(panel: PanelState): PanelState {
    val updated = when (panel.panelType) {
        PanelType.EDITOR -> migrateTitleByPattern(panel.title, Regex("^\\s*eingabe(?:panel)?\\s*(.*)$", RegexOption.IGNORE_CASE), "TextEditor")
        PanelType.LIST_TEST -> migrateTitleByPattern(panel.title, Regex("^\\s*liste\\s*(.*)$", RegexOption.IGNORE_CASE), "Stepper")
        PanelType.BLOCKEDITOR -> migrateTitleByPattern(panel.title, Regex("^\\s*(?:native\\s+)?blockeditor\\s*(.*)$", RegexOption.IGNORE_CASE), "BlockEditor")
        PanelType.EMSCRIPT -> migrateTitleByPattern(panel.title, Regex("^\\s*emscript\\s*(.*)$", RegexOption.IGNORE_CASE), "Debug")
        else -> panel.title
    }
    return if (updated != panel.title) panel.copy(title = updated) else panel
}

private fun migrateTitleByPattern(title: String, pattern: Regex, replacementBase: String): String {
    val match = pattern.find(title) ?: return title
    val suffix = match.groupValues.getOrNull(1).orEmpty().trim()
    return if (suffix.isBlank()) replacementBase else "$replacementBase $suffix"
}

private fun buildApplySemanticPreview(
    before: WorkspaceDocument,
    after: WorkspaceDocument,
    registry: BlockRegistry,
    unsupportedCount: Int,
    roundtripLength: Int,
): String {
    val beforeBlockCounts = before.blocks.values.groupingBy { blockSemanticKey(it, registry) }.eachCount()
    val afterBlockCounts = after.blocks.values.groupingBy { blockSemanticKey(it, registry) }.eachCount()
    val addedBlocks = countAdded(beforeBlockCounts, afterBlockCounts)
    val removedBlocks = countRemoved(beforeBlockCounts, afterBlockCounts)

    val sharedIds = before.blocks.keys.intersect(after.blocks.keys)
    val changedType = sharedIds.count { id -> before.blocks[id]?.type != after.blocks[id]?.type }
    val changedFields = sharedIds.count { id -> before.blocks[id]?.fields != after.blocks[id]?.fields }

    val beforeConnections = semanticConnections(before, registry)
    val afterConnections = semanticConnections(after, registry)
    val addedConnections = afterConnections - beforeConnections
    val removedConnections = beforeConnections - afterConnections

    val beforeVars = before.variables.variables
    val afterVars = after.variables.variables
    val addedVariables = afterVars.keys - beforeVars.keys
    val removedVariables = beforeVars.keys - afterVars.keys
    val changedVariables = beforeVars.keys.intersect(afterVars.keys).mapNotNull { id ->
        val old = beforeVars[id] ?: return@mapNotNull null
        val new = afterVars[id] ?: return@mapNotNull null
        if (old.name != new.name || old.type != new.type || old.defaultValue != new.defaultValue) {
            VariableChange(old.name, new.name, old.type, new.type)
        } else {
            null
        }
    }

    val structure = computeStructureDiff(before, after, registry)

    val totalReplacements = addedBlocks + removedBlocks
    val replacementThreshold = maxOf(8, (before.blocks.size * 0.6).toInt())
    val hasLargeReplacement = totalReplacements >= replacementThreshold

    val fieldExamples = sharedIds.asSequence().mapNotNull { id ->
        val oldBlock = before.blocks[id] ?: return@mapNotNull null
        val newBlock = after.blocks[id] ?: return@mapNotNull null
        if (oldBlock.fields == newBlock.fields) return@mapNotNull null
        val oldFieldMap = oldBlock.fields.mapValues { renderFieldValue(it.value) }
        val newFieldMap = newBlock.fields.mapValues { renderFieldValue(it.value) }
        val changedFieldName = (oldFieldMap.keys + newFieldMap.keys).firstOrNull { key ->
            oldFieldMap[key] != newFieldMap[key]
        } ?: return@mapNotNull null
        val label = blockDisplayLabel(newBlock, registry)
        "  ~ $label.$changedFieldName: ${oldFieldMap[changedFieldName] ?: "∅"} -> ${newFieldMap[changedFieldName] ?: "∅"}"
    }.take(3).toList()

    return buildString {
        appendLine("Draft -> Parse -> Import -> Validate: OK")
        appendLine()
        appendLine("Blöcke")
        appendLine("  + $addedBlocks hinzugefügt")
        appendLine("  - $removedBlocks entfernt")
        appendLine("  ~ ${changedType + changedFields} geändert")
        if (changedType > 0) appendLine("    (Typwechsel: $changedType)")
        if (changedFields > 0) appendLine("    (Feldänderungen: $changedFields)")
        fieldExamples.forEach { appendLine(it) }
        appendLine()
        appendLine("Verbindungen")
        appendLine("  + ${addedConnections.size} hinzugefügt")
        appendLine("  - ${removedConnections.size} entfernt")
        appendLine()
        appendLine("Variablen")
        appendLine("  + ${addedVariables.size} hinzugefügt")
        appendLine("  - ${removedVariables.size} entfernt")
        appendLine("  ~ ${changedVariables.size} geändert")
        addedVariables.take(3).forEach { id ->
            val name = afterVars[id]?.name ?: id
            appendLine("    + $name")
        }
        changedVariables.take(3).forEach { change ->
            appendLine("    ~ ${change.newName}: ${change.oldType} -> ${change.newType}")
        }
        appendLine()
        appendLine("Struktur")
        appendLine("  ~ Roots betroffen: +${structure.addedRoots} / -${structure.removedRoots}")
        if (structure.changedIfConditions > 0) appendLine("  ~ IF-Bedingung geändert: ${structure.changedIfConditions}")
        if (structure.removedElseBranches > 0) appendLine("  - ELSE-Zweig entfernt: ${structure.removedElseBranches}")
        appendLine()
        if (hasLargeReplacement) {
            appendLine("WARNUNG: Umfangreicher Ersatz erkannt ($totalReplacements Blockänderungen).")
        }
        appendLine("Nicht unterstützte Konstrukte: $unsupportedCount")
        appendLine("Roundtrip-Script-Länge: $roundtripLength")
        append("Hinweis: Apply erfolgt atomar mit Undo-Eintrag.")
    }
}

private fun semanticConnections(
    document: WorkspaceDocument,
    registry: BlockRegistry,
): Set<String> {
    val result = mutableSetOf<String>()
    document.blocks.forEach { (parentId, parent) ->
        val parentLabel = blockDisplayLabel(parent, registry)
        parent.next?.connectedTo?.let { target ->
            val child = WorkspaceGraph.findConnection(document, target)?.first
            val childLabel = child?.let { id -> document.blocks[id]?.let { blockDisplayLabel(it, registry) } } ?: "?"
            result += "NEXT:$parentLabel->$childLabel"
        }
        parent.valueInputs.forEach { input ->
            val target = input.connection.connectedTo ?: return@forEach
            val childId = WorkspaceGraph.findConnection(document, target)?.first ?: return@forEach
            val child = document.blocks[childId] ?: return@forEach
            result += "VALUE:${input.name}:${parentLabel}->${blockDisplayLabel(child, registry)}"
        }
        parent.statementInputs.forEach { input ->
            val target = input.connection.connectedTo ?: return@forEach
            val childId = WorkspaceGraph.findConnection(document, target)?.first ?: return@forEach
            val child = document.blocks[childId] ?: return@forEach
            result += "STMT:${input.name}:${parentLabel}->${blockDisplayLabel(child, registry)}"
        }
    }
    return result
}

private fun computeStructureDiff(
    before: WorkspaceDocument,
    after: WorkspaceDocument,
    registry: BlockRegistry,
): StructureDiff {
    val beforeRoots = WorkspaceGraph.topLevelRoots(before)
        .mapNotNull { id -> before.blocks[id] }
        .groupingBy { blockSemanticKey(it, registry) }
        .eachCount()
    val afterRoots = WorkspaceGraph.topLevelRoots(after)
        .mapNotNull { id -> after.blocks[id] }
        .groupingBy { blockSemanticKey(it, registry) }
        .eachCount()

    val changedIfConditions = before.blocks.keys.intersect(after.blocks.keys).count { id ->
        val oldBlock = before.blocks[id] ?: return@count false
        val newBlock = after.blocks[id] ?: return@count false
        val ifType = oldBlock.type == BlockTypes.CONTROL_IF ||
            oldBlock.type == BlockTypes.CONTROL_IF_ELSE ||
            oldBlock.type == BlockTypes.CONTROL_IF_ELSEIF_ELSE
        if (!ifType) return@count false
        val oldCondition = connectedValueSummary(before, id, "CONDITION", registry)
        val newCondition = connectedValueSummary(after, id, "CONDITION", registry)
        oldCondition != newCondition
    }

    val removedElseBranches = before.blocks.keys.intersect(after.blocks.keys).count { id ->
        val oldElseSize = WorkspaceGraph.statementStack(before, id, BlockTypes.SLOT_ELSE).size
        val newElseSize = WorkspaceGraph.statementStack(after, id, BlockTypes.SLOT_ELSE).size
        oldElseSize > 0 && newElseSize == 0
    }

    return StructureDiff(
        addedRoots = countAdded(beforeRoots, afterRoots),
        removedRoots = countRemoved(beforeRoots, afterRoots),
        changedIfConditions = changedIfConditions,
        removedElseBranches = removedElseBranches,
    )
}

private fun connectedValueSummary(
    document: WorkspaceDocument,
    blockId: de.visualtasker.blockeditor.domain.BlockId,
    inputName: String,
    registry: BlockRegistry,
): String? {
    val parent = document.blocks[blockId] ?: return null
    val target = parent.valueInputs.firstOrNull { it.name == inputName }?.connection?.connectedTo ?: return null
    val childId = WorkspaceGraph.findConnection(document, target)?.first ?: return null
    val child = document.blocks[childId] ?: return null
    return blockSemanticKey(child, registry)
}

private fun blockSemanticKey(block: BlockNode, registry: BlockRegistry): String {
    val label = blockDisplayLabel(block, registry)
    val fields = block.fields.toList()
        .sortedBy { it.first }
        .joinToString("|") { (key, value) -> "$key=${renderFieldValue(value)}" }
    return "${block.type}#$label#$fields"
}

private fun blockDisplayLabel(block: BlockNode, registry: BlockRegistry): String {
    val typeLabel = registry.getDefinition(block.type)?.label ?: block.type.substringAfterLast('.')
    val visible = listOf(
        block.fields["variableLabel"],
        block.fields["variable"],
        block.fields["name"],
        block.fields["value"],
        block.fields["operator"],
    ).firstOrNull { candidate ->
        candidate != null && renderFieldValue(candidate).isNotBlank()
    }?.let(::renderFieldValue)
    return if (visible.isNullOrBlank()) typeLabel else "$typeLabel($visible)"
}

private fun renderFieldValue(value: FieldValue): String = when (value) {
    is FieldValue.Text -> value.value
    is FieldValue.Number -> value.value.toString()
    is FieldValue.Bool -> value.value.toString()
}

private fun countAdded(before: Map<String, Int>, after: Map<String, Int>): Int =
    after.entries.sumOf { (key, value) -> (value - (before[key] ?: 0)).coerceAtLeast(0) }

private fun countRemoved(before: Map<String, Int>, after: Map<String, Int>): Int =
    before.entries.sumOf { (key, value) -> (value - (after[key] ?: 0)).coerceAtLeast(0) }

private data class StructureDiff(
    val addedRoots: Int,
    val removedRoots: Int,
    val changedIfConditions: Int,
    val removedElseBranches: Int,
)

private data class VariableChange(
    val oldName: String,
    val newName: String,
    val oldType: String,
    val newType: String,
)

private fun loadColorPref(prefs: android.content.SharedPreferences, key: String, fallback: Color): Color {
    val argbKey = "color.argb.${key.removePrefix("color.")}"
    if (prefs.contains(argbKey)) {
        val argb = prefs.getInt(argbKey, fallback.toArgb())
        return Color(argb)
    }
    if (!prefs.contains(key)) return fallback
    val legacyRaw = runCatching { prefs.getLong(key, fallback.toArgb().toLong()) }.getOrNull()
        ?: return fallback
    val legacyPackedColor = runCatching { Color(legacyRaw.toULong()) }.getOrNull()
    return legacyPackedColor ?: Color(legacyRaw.toInt())
}

private fun defaultFunctionKeyActions(): Map<String, String> = mapOf(
    "F1" to "[]", "F2" to "()", "F3" to "{}", "F4" to "<>", "F5" to "=>", "F6" to "TODO()",
    "F7" to "println()", "F8" to "if () {\n\n}", "F9" to "for () {\n\n}", "F10" to "while () {\n\n}",
    "F11" to "/* */", "F12" to "::"
)

private fun applyKeyPress(
    value: TextFieldValue,
    key: String,
    insertMode: Boolean,
    functionActions: Map<String, String>
): Pair<TextFieldValue, Boolean> {
    val text = value.text
    val start = value.selection.start.coerceIn(0, text.length)
    val end = value.selection.end.coerceIn(0, text.length)

    fun withText(newText: String, cursor: Int): TextFieldValue {
        val safe = cursor.coerceIn(0, newText.length)
        return TextFieldValue(newText, TextRange(safe))
    }
    fun replaceSelection(insert: String): TextFieldValue {
        val head = text.substring(0, start)
        val tail = text.substring(end)
        return withText(head + insert + tail, head.length + insert.length)
    }
    fun insertAtCursor(insert: String): TextFieldValue {
        if (start != end) return replaceSelection(insert)
        val head = text.substring(0, start)
        val tail = text.substring(start)
        val adjustedTail = if (insertMode) tail.drop(insert.length.coerceAtMost(tail.length)) else tail
        return withText(head + insert + adjustedTail, head.length + insert.length)
    }

    if (key.matches(Regex("F(1[0-2]|[1-9])"))) return insertAtCursor(functionActions[key].orEmpty()) to insertMode
    return when (key) {
        "BACK" -> if (start != end) replaceSelection("") to insertMode else if (start > 0) withText(text.removeRange(start - 1, start), start - 1) to insertMode else value to insertMode
        "DEL" -> if (start != end) replaceSelection("") to insertMode else if (start < text.length) withText(text.removeRange(start, start + 1), start) to insertMode else value to insertMode
        "SPACE" -> insertAtCursor(" ") to insertMode
        "TAB" -> insertAtCursor("\t") to insertMode
        "ENTER" -> insertAtCursor("\n") to insertMode
        "INS" -> value to !insertMode
        "HOME", "POS1" -> withText(text, 0) to insertMode
        "END" -> withText(text, text.length) to insertMode
        "LEFT" -> withText(text, (start - 1).coerceAtLeast(0)) to insertMode
        "RIGHT" -> withText(text, (start + 1).coerceAtMost(text.length)) to insertMode
        "UP" -> withText(text, (start - 20).coerceAtLeast(0)) to insertMode
        "DOWN" -> withText(text, (start + 20).coerceAtMost(text.length)) to insertMode
        "PGUP" -> withText(text, (start - 80).coerceAtLeast(0)) to insertMode
        "PGDN" -> withText(text, (start + 80).coerceAtMost(text.length)) to insertMode
        else -> insertAtCursor(key) to insertMode
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    currentPanel: PanelState?,
    functionActions: MutableMap<String, String>,
    appearance: StudioAppearance,
    onAppearanceChange: (StudioAppearance) -> Unit,
    hideSystemBars: Boolean,
    onHideSystemBarsChange: (Boolean) -> Unit,
    dockAtTop: Boolean,
    onDockAtTopChange: (Boolean) -> Unit,
    useLargeGrid: Boolean,
    onUseLargeGridChange: (Boolean) -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    snapEnabled: Boolean,
    onSnapEnabledChange: (Boolean) -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onResetPanels: () -> Unit,
    onColorPick: (Color) -> Unit,
    onToggleIconEngine: () -> Unit,
    onDismiss: () -> Unit
) {
    val draft = remember(functionActions.toMap()) { mutableStateMapOf<String, String>().apply { putAll(functionActions) } }
    val fkeys = remember { (1..12).map { "F$it" } }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ScrollableTabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { onTabChange(0) }, text = { Text("Layout") })
            Tab(selected = tabIndex == 1, onClick = { onTabChange(1) }, text = { Text("Flowchart") })
            Tab(selected = tabIndex == 2, onClick = { onTabChange(2) }, text = { Text("Blockeditor") })
            Tab(selected = tabIndex == 3, onClick = { onTabChange(3) }, text = { Text("Texteditor") })
            Tab(selected = tabIndex == 4, onClick = { onTabChange(4) }, text = { Text("Browser") })
            Tab(selected = tabIndex == 5, onClick = { onTabChange(5) }, text = { Text("Extras") })
            Tab(selected = tabIndex == 6, onClick = { onTabChange(6) }, text = { Text("Farben") })
            Tab(selected = tabIndex == 7, onClick = { onTabChange(7) }, text = { Text("Keypad") })
        }

        when (tabIndex) {
            6 -> Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Fokus-Panel: ${currentPanel?.title ?: "Keins"}", color = M3EColors.Amber)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Syntax Highlighting", style = MaterialTheme.typography.labelLarge)
                ColorAssignmentRow("Keyword", appearance.syntaxKeyword) { onAppearanceChange(appearance.copy(syntaxKeyword = it)) }
                ColorAssignmentRow("Control", appearance.syntaxControl) { onAppearanceChange(appearance.copy(syntaxControl = it)) }
                ColorAssignmentRow("String", appearance.syntaxString) { onAppearanceChange(appearance.copy(syntaxString = it)) }
                ColorAssignmentRow("Number", appearance.syntaxNumber) { onAppearanceChange(appearance.copy(syntaxNumber = it)) }
                ColorAssignmentRow("Comment", appearance.syntaxComment) { onAppearanceChange(appearance.copy(syntaxComment = it)) }
                ColorAssignmentRow("Operator", appearance.syntaxOperator) { onAppearanceChange(appearance.copy(syntaxOperator = it)) }
                ColorAssignmentRow("Plain Text", appearance.syntaxPlain) { onAppearanceChange(appearance.copy(syntaxPlain = it)) }
                Spacer(Modifier.height(8.dp))
                Text("Gemeinsame Kategorien (Flowchart + Blockeditor)", style = MaterialTheme.typography.labelLarge)
                ColorAssignmentRow("Event", appearance.blockEvent) {
                    onAppearanceChange(appearance.copy(blockEvent = it, flowEvent = it))
                }
                ColorAssignmentRow("Control", appearance.blockControl) {
                    onAppearanceChange(appearance.copy(blockControl = it, flowControl = it))
                }
                ColorAssignmentRow("Logic", appearance.blockLogic) {
                    onAppearanceChange(appearance.copy(blockLogic = it, flowLogic = it))
                }
                ColorAssignmentRow("Variable", appearance.blockVariable) {
                    onAppearanceChange(appearance.copy(blockVariable = it, flowVariable = it))
                }
                Spacer(Modifier.height(8.dp))
                Text("Block Kategorien", style = MaterialTheme.typography.labelLarge)
                ColorAssignmentRow("Action", appearance.blockAction) { onAppearanceChange(appearance.copy(blockAction = it)) }
                ColorAssignmentRow("EMScript", appearance.blockEmscript) { onAppearanceChange(appearance.copy(blockEmscript = it)) }
                ColorAssignmentRow("Input", appearance.blockInput) { onAppearanceChange(appearance.copy(blockInput = it)) }
                ColorAssignmentRow("Perception", appearance.blockPerception) { onAppearanceChange(appearance.copy(blockPerception = it)) }
                ColorAssignmentRow("Control", appearance.blockControl) { onAppearanceChange(appearance.copy(blockControl = it)) }
                ColorAssignmentRow("Logic", appearance.blockLogic) { onAppearanceChange(appearance.copy(blockLogic = it)) }
                ColorAssignmentRow("Variables", appearance.blockVariables) { onAppearanceChange(appearance.copy(blockVariables = it)) }
                ColorAssignmentRow("Flow", appearance.blockFlow) { onAppearanceChange(appearance.copy(blockFlow = it)) }
                ColorAssignmentRow("Runtime", appearance.blockRuntime) { onAppearanceChange(appearance.copy(blockRuntime = it)) }
                ColorAssignmentRow("Debug", appearance.blockDebug) { onAppearanceChange(appearance.copy(blockDebug = it)) }
                ColorAssignmentRow("Variable", appearance.blockVariable) { onAppearanceChange(appearance.copy(blockVariable = it)) }
                ColorAssignmentRow("Custom", appearance.blockCustom) { onAppearanceChange(appearance.copy(blockCustom = it)) }
                Spacer(Modifier.height(8.dp))
                Text("Panel-Akzent", style = MaterialTheme.typography.labelLarge)
                ColorPalettePicker(onSelect = onColorPick)
            }
            7 -> Column(
                Modifier.fillMaxWidth().height(380.dp).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Keypad-Mapping")
                fkeys.forEach { key ->
                    OutlinedTextField(
                        value = draft[key].orEmpty(),
                        onValueChange = {
                            draft[key] = it
                            functionActions[key] = it
                        },
                        label = { Text("$key Aktion") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AssistChip(onClick = onToggleIconEngine, label = { Text("Icon-Engine: ${IconMotionConfig.engine.name}") })
            }
            0 -> Column(
                Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Systemleisten verstecken")
                    Switch(checked = hideSystemBars, onCheckedChange = onHideSystemBarsChange)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Minimiert-Leiste oben")
                    Switch(checked = dockAtTop, onCheckedChange = onDockAtTopChange)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Grid groß (8x8)")
                    Switch(checked = useLargeGrid, onCheckedChange = onUseLargeGridChange)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Snap aktiv")
                    Switch(checked = snapEnabled, onCheckedChange = onSnapEnabledChange)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("UI Skalierung: ${(uiScale * 100).toInt()}%")
                    Slider(
                        value = uiScale,
                        onValueChange = onUiScaleChange,
                        valueRange = 0.7f..1.5f,
                    )
                }
                Text("Theme")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { onThemeModeChange("system") },
                        label = { Text("System") }
                    )
                    AssistChip(
                        onClick = { onThemeModeChange("light") },
                        label = { Text("Hell") }
                    )
                    AssistChip(
                        onClick = { onThemeModeChange("dark") },
                        label = { Text("Dunkel") }
                    )
                }
                Text("Aktuell: $themeMode")
                Button(onClick = onResetPanels, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Panels zurücksetzen")
                }
                Text("Standard bleibt 4x4 (kleines Grid).")
            }
            1 -> PlaceholderSettingsTab("Flowchart", "Nodefarben und Grid sind im Farben/Layout-Tab konfigurierbar.")
            2 -> PlaceholderSettingsTab("Blockeditor", "Block-Kategoriefarben und Sidebar-Regeln sind im Farben-Tab konfigurierbar.")
            3 -> PlaceholderSettingsTab("Texteditor", "Syntax-Highlighting-Farben sind im Farben-Tab konfigurierbar.")
            4 -> PlaceholderSettingsTab("Browser", "Browser-spezifische Optionen folgen im nächsten Slice.")
            else -> ExtrasPermissionsTab()
        }
    }
}

@Composable
private fun PlaceholderSettingsTab(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExtrasPermissionsTab() {
    val context = LocalContext.current
    val packageName = context.packageName
    val overlayGranted = Settings.canDrawOverlays(context)
    val fileAccessGranted = isFileAccessGranted(context)
    val batteryGranted = isBatteryOptimizationDisabled(context)
    val notificationsGranted = isNotificationAccessGranted(context)
    val microphoneGranted = hasPermission(context, Manifest.permission.RECORD_AUDIO)
    val accessibilityGranted = isAccessibilityEnabledForApp(context)
    val shizukuGranted = isPackageInstalled(context, "moe.shizuku.privileged.api")
    val rootGranted = isRootAvailable()

    val entries = listOf(
        PermissionEntry(
            label = "Overlay",
            granted = overlayGranted,
            open = {
                context.safeStartActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"),
                    ),
                )
            },
        ),
        PermissionEntry(
            label = "Dateizugriff",
            granted = fileAccessGranted,
            open = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"),
                    )
                } else {
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName"),
                    )
                }
                context.safeStartActivity(intent)
            },
        ),
        PermissionEntry(
            label = "Akku Optimierung",
            granted = batteryGranted,
            open = {
                context.safeStartActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName"),
                    ),
                )
            },
        ),
        PermissionEntry(
            label = "Benachrichtigungen",
            granted = notificationsGranted,
            open = {
                context.safeStartActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    },
                )
            },
        ),
        PermissionEntry(
            label = "Mikrofon",
            granted = microphoneGranted,
            open = {
                context.safeStartActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName"),
                    ),
                )
            },
        ),
        PermissionEntry(
            label = "Bedienungshilfen",
            granted = accessibilityGranted,
            open = {
                context.safeStartActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
        ),
        PermissionEntry(
            label = "Shizuku",
            granted = shizukuGranted,
            open = {
                val intent = if (shizukuGranted) {
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:moe.shizuku.privileged.api"),
                    )
                } else {
                    Intent(Settings.ACTION_SETTINGS)
                }
                context.safeStartActivity(intent)
            },
        ),
        PermissionEntry(
            label = "Root",
            granted = rootGranted,
            open = {
                context.safeStartActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            },
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Permissions einstellen", style = MaterialTheme.typography.titleMedium)
                entries.forEach { entry ->
                    PermissionStatusRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(entry: PermissionEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(entry.label, style = MaterialTheme.typography.labelLarge)
            Text(
                if (entry.granted) "Status: erteilt" else "Status: ausstehend",
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.granted) Color(0xFF81C784) else MaterialTheme.colorScheme.error,
            )
        }
        AssistChip(
            onClick = entry.open,
            label = { Text("Öffnen") },
        )
    }
}

private data class PermissionEntry(
    val label: String,
    val granted: Boolean,
    val open: () -> Unit,
)

private fun Context.safeStartActivity(intent: Intent) {
    runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun isFileAccessGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
        return true
    }
    val legacy = hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    if (legacy) return true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ||
            hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ||
            hasPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
    }
    return false
}

private fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return manager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun isNotificationAccessGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

private fun isAccessibilityEnabledForApp(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.contains(context.packageName, ignoreCase = true)
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
    }.isSuccess
}

private fun isRootAvailable(): Boolean {
    val paths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su",
    )
    return paths.any { File(it).exists() }
}

@Composable
private fun ColorAssignmentRow(
    label: String,
    current: Color,
    onAssign: (Color) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(current, RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
            )
        }
        ColorPalettePicker(onSelect = onAssign)
    }
}

@Composable
private fun ColorPalettePicker(
    onSelect: (Color) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier.height(92.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(M3EColors.allColors) { c ->
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(c, RoundedCornerShape(11.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp))
                    .clickable { onSelect(c) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPanelSheet(
    onSelect: (PanelType) -> Unit,
    onLaunchFloatingOverlay: (FloatingOverlayTarget) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryTab by remember { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Panel-Typ auswählen", style = MaterialTheme.typography.titleMedium)
            TabRow(selectedTabIndex = categoryTab) {
                Tab(
                    selected = categoryTab == 0,
                    onClick = { categoryTab = 0 },
                    text = { Text("Normal") },
                )
                Tab(
                    selected = categoryTab == 1,
                    onClick = { categoryTab = 1 },
                    text = { Text("Floating") },
                )
            }
            if (categoryTab == 0) {
                AssistChip(
                    onClick = { onSelect(PanelType.EDITOR) },
                    label = { Text("TextEditor") },
                    leadingIcon = { Icon(Icons.Default.Keyboard, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onSelect(PanelType.KEYBOARD) },
                    label = { Text("VisualTasker Studio WSS") },
                    leadingIcon = { Icon(Icons.Default.Keyboard, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onSelect(PanelType.LIST_TEST) },
                    label = { Text("Stepper") },
                    leadingIcon = { Icon(Icons.Default.ViewList, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onSelect(PanelType.BROWSER) },
                    label = { Text("Browser") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onSelect(PanelType.BLOCKEDITOR) },
                    label = { Text("BlockEditor") },
                    leadingIcon = { Icon(Icons.Default.ViewList, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onSelect(PanelType.FLOWCHART) },
                    label = { Text("Flowchart") },
                    leadingIcon = { Icon(Icons.Default.Polyline, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onSelect(PanelType.EMSCRIPT) },
                    label = { Text("Debug") },
                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onSelect(PanelType.LOG_CONSOLE) },
                    label = { Text("Log-Konsole") },
                    leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) }
                )
            } else {
                AssistChip(
                    onClick = { onLaunchFloatingOverlay(FloatingOverlayTarget.PANEL) },
                    label = { Text("Floating Panel") },
                    leadingIcon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                )
                AssistChip(
                    onClick = { onLaunchFloatingOverlay(FloatingOverlayTarget.TOOLBAR) },
                    label = { Text("Floating Toolbar") },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                )
                AssistChip(
                    onClick = { onLaunchFloatingOverlay(FloatingOverlayTarget.INSPECTOR) },
                    label = { Text("Floating Inspector") },
                    leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun EmscriptEmbeddedPanel(
    modifier: Modifier = Modifier,
    projectionStatus: String,
    editingStatus: String,
    overallStatus: String,
    revision: Int,
    projectedScript: String,
    draft: TextFieldValue,
    onSaveDraft: () -> Unit,
    onUseProjection: () -> Unit,
    diagnostics: List<String>,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "EMScript-Projektion: $projectionStatus | EMScript-Bearbeitung: $editingStatus",
            color = M3EColors.Amber,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = "EMScript-Gesamtstatus: $overallStatus | Revision: $revision",
            color = M3EColors.Amber,
            style = MaterialTheme.typography.labelMedium,
        )
        diagnostics.forEach { message ->
            Text(
                text = "• $message",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = onUseProjection,
                label = { Text("Projektion in Entwurf übernehmen") },
            )
            AssistChip(
                onClick = onSaveDraft,
                label = { Text("Draft lokal speichern") },
            )
        }
        Text(
            text = "LOKALER ENTWURF - NICHT AUF WORKSPACE ANGEWENDET",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Text(
                text = draft.text.ifBlank { "// Leerer Workspace" },
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "GENERIERTE PROJEKTION",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Text(
                text = projectedScript.ifBlank { "// Leerer Workspace" },
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ColumnScope.BlockEditorPanelCompactRail(
    controller: BlockEditorController,
    onExpandRequested: () -> Unit,
) {
    val categories = remember {
        BlockCategories.all.filter { it.id != BlockCategories.CUSTOM }
    }
    categories.forEach { category ->
        TooltipIconButton(
            tooltip = category.label,
            onClick = {
                controller.onCategoryClick(category.id)
                onExpandRequested()
            },
            modifier = Modifier.size(36.dp),
        ) {
            val accent = blockEditorColors(category.id)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accent.copy(alpha = 0.22f), CircleShape)
                    .border(width = 1.6.dp, color = accent.copy(alpha = 0.95f), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CategoryIcons.forCategory(category.id),
                    contentDescription = category.label,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.BlockEditorPanelExpandedRail(
    controller: BlockEditorController,
) {
    val selectedCategory = controller.expandedCategory
    val category = remember(selectedCategory) {
        BlockCategories.all.firstOrNull { it.id == selectedCategory }
    }
    val blockDefinitions = controller.definitionsForExpandedCategory()
    Text(
        text = category?.label ?: "Blöcke",
        style = MaterialTheme.typography.labelMedium,
    )
    if (category == null) {
        Text(
            text = "Kategorie links per Icon waehlen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        blockDefinitions.forEach { definition ->
            AssistChip(
                onClick = { controller.addBlockFromPalette(definition) },
                label = { Text(definition.label) },
                leadingIcon = {
                    val accent = blockEditorColors(category.id)
                    Icon(
                        imageVector = BlockIcons.forBlockType(definition.id),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun LogConsoleEmbeddedPanel(
    modifier: Modifier = Modifier,
    store: StudioLogStore,
    uiState: LogConsoleUiState,
) {
    val token = store.changeToken
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.GERMANY) }
    val filters = StudioLogFilters(
        levels = uiState.selectedLevels,
        sources = uiState.selectedSources,
        query = uiState.query,
    )
    val entries = remember(token, uiState.selectedLevels, uiState.selectedSources, uiState.query) {
        store.visibleEntries(filters)
    }
    val expandedIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(entries.size, uiState.autoScroll, store.isPaused) {
        if (uiState.autoScroll && !store.isPaused && entries.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(entries.lastIndex)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TooltipIconButton(
                tooltip = if (store.isPaused) "Fortsetzen" else "Pausieren",
                onClick = { store.setEmissionPaused(!store.isPaused) },
            ) {
                Icon(
                    imageVector = if (store.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (store.isPaused) "Fortsetzen" else "Pausieren",
                )
            }
            TooltipIconButton(
                tooltip = if (uiState.autoScroll) "Auto-Scroll aus" else "Auto-Scroll an",
                onClick = { uiState.autoScroll = !uiState.autoScroll },
            ) {
                Icon(
                    imageVector = if (uiState.autoScroll) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                    contentDescription = if (uiState.autoScroll) "Auto-Scroll an" else "Auto-Scroll aus",
                )
            }
            TooltipIconButton(
                tooltip = "Sichtbare löschen",
                onClick = { store.clearVisible(filters) },
            ) { Icon(Icons.Default.DeleteSweep, contentDescription = "Sichtbare loeschen") }
            Text(
                text = "Einträge: ${entries.size}${if (store.isPaused) " (PAUSIERT)" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = M3EColors.Amber,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = levelColor(entry.level),
                            modifier = Modifier.weight(1f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TooltipIconButton(
                                tooltip = "Details umschalten",
                                onClick = { expandedIds[entry.id] = !isExpanded },
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                                    contentDescription = "Details umschalten",
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
                                },
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        entry.details?.let { details ->
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.LogConsoleCompactRail(
    store: StudioLogStore,
    uiState: LogConsoleUiState,
) {
    val compactIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
    val filters = StudioLogFilters(
        levels = uiState.selectedLevels,
        sources = uiState.selectedSources,
        query = uiState.query,
    )
    TooltipIconButton(tooltip = if (store.isPaused) "Fortsetzen" else "Pausieren", onClick = { store.setEmissionPaused(!store.isPaused) }) {
        Icon(
            imageVector = if (store.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = if (store.isPaused) "Fortsetzen" else "Pausieren",
            tint = compactIconTint,
        )
    }
    TooltipIconButton(tooltip = if (uiState.autoScroll) "Auto-Scroll aus" else "Auto-Scroll an", onClick = { uiState.autoScroll = !uiState.autoScroll }) {
        Icon(
            imageVector = if (uiState.autoScroll) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
            contentDescription = if (uiState.autoScroll) "Auto-Scroll an" else "Auto-Scroll aus",
            tint = compactIconTint,
        )
    }
    TooltipIconButton(tooltip = "Sichtbare löschen", onClick = { store.clearVisible(filters) }) {
        Icon(Icons.Default.DeleteSweep, contentDescription = "Sichtbare loeschen", tint = compactIconTint)
    }
}

@Composable
private fun ColumnScope.LogConsoleExpandedRail(
    store: StudioLogStore,
    uiState: LogConsoleUiState,
) {
    val token = store.changeToken
    val sources = remember(token) { store.availableSources() }
    Text(
        text = "Filter",
        style = MaterialTheme.typography.labelMedium,
    )
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { uiState.query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Suche") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
            singleLine = true,
        )
        Text(
            text = "Level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                leadingIcon = { Text(if (selected) "✓" else "•") },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Quellen",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AssistChip(
            onClick = { uiState.selectedSources = emptySet() },
            label = { Text("Alle") },
            leadingIcon = { Text(if (uiState.selectedSources.isEmpty()) "✓" else "•") },
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
                leadingIcon = { Text(if (selected) "✓" else "•") },
            )
        }
    }
}

@Composable
private fun ColumnScope.EmscriptCompactRail(
    onExpandRequested: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    canLoad: Boolean,
) {
    val compactIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
    TooltipIconButton(
        tooltip = "Dateimanager öffnen",
        onClick = onExpandRequested,
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = "Dateimanager", tint = compactIconTint)
    }
    TooltipIconButton(
        tooltip = "Script speichern",
        onClick = onSave,
    ) {
        Icon(Icons.Default.Save, contentDescription = "Speichern", tint = compactIconTint)
    }
    TooltipIconButton(
        tooltip = "Script laden",
        onClick = onLoad,
        enabled = canLoad,
    ) {
        Icon(Icons.Default.Upload, contentDescription = "Laden", tint = compactIconTint)
    }
}

@Composable
private fun ColumnScope.EmscriptExpandedRail(
    manager: EmscriptFileManagerUiState,
    onSave: () -> Unit,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit,
) {
    Text(
        text = "Script-Dateien",
        style = MaterialTheme.typography.labelMedium,
    )
    OutlinedTextField(
        value = manager.currentName,
        onValueChange = { manager.currentName = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Name") },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
            enabled = manager.scripts.containsKey(manager.currentName.trim()),
        ) {
            Icon(Icons.Default.Upload, contentDescription = "Laden")
        }
        TooltipIconButton(
            tooltip = "Löschen",
            onClick = { onDelete(manager.currentName.trim()) },
            enabled = manager.currentName.trim().isNotBlank() && manager.currentName.trim() != "draft",
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
                },
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

@Composable
private fun BlockEditorEmbeddedPanel(
    modifier: Modifier = Modifier,
    controller: BlockEditorController,
) {
    LaunchedEffect(controller) {
        if (controller.expandedCategory == null) {
            controller.onCategoryClick(BlockCategories.ACTION)
        }
    }
    BlockEditorHost(
        controller = controller,
        uiConfig = BlockEditorHostUiConfig(
            showBottomPanel = true,
            showBlockFactory = true,
            showToolbox = false,
            allowClearWorkspace = true
        ),
        modifier = modifier
    )
}

@Composable
private fun FlowchartEmbeddedPanel(
    modifier: Modifier = Modifier,
    graphDocument: FlowGraphDocument,
    panelUi: FlowchartPanelUiState,
    logStore: StudioLogStore,
    appearance: StudioAppearance,
) {
    val controller = remember { FlowchartController(FlowSurfaceId("main-screen-flowchart")) }
    val flowBackground = MaterialTheme.colorScheme.surfaceContainerLowest
    val callbacks = remember {
        FlowchartHostCallbacks(
            onNodeSelected = { nodeId -> panelUi.selectedNodeId = nodeId?.value },
        )
    }
    val nodeShapeProvider = remember {
        FlowchartNodeShapeProvider { node, width, height ->
            flowchartMaterialNodePath(node = node, width = width, height = height)
        }
    }
    val flowchartUiConfig = remember(flowBackground, appearance) {
        FlowchartUiConfig(
            zoomEnabled = false,
            colorTokens = FlowchartColorTokens(
                background = flowBackground,
                nodeFill = Color(0xFF1C1F2B),
                eventNodeFill = appearance.blockEvent,
                controlNodeFill = appearance.blockControl,
                logicNodeFill = appearance.blockLogic,
                variableNodeFill = appearance.blockVariable,
                nodeStroke = Color(0xFFC6CAE0),
                selectedStroke = Color(0xFF7DA8FF),
                edge = Color(0xFF93A1C5),
                branchEdge = Color(0xFFB094FF),
                dataEdge = Color(0xFF59A6FF),
                loopEdge = Color(0xFF63D39D),
                errorEdge = Color(0xFFFF8A80),
            ),
        )
    }
    DisposableEffect(controller) {
        onDispose(controller::close)
    }
    Box(modifier = modifier.fillMaxSize()) {
        if (panelUi.gridVisible) {
            FlowchartGridOverlay(Modifier.matchParentSize())
        }
        FlowchartHost(
            graphDocument = graphDocument,
            viewDocument = null,
            runtimeSnapshot = null,
            controller = controller,
            uiConfig = flowchartUiConfig,
            callbacks = callbacks,
            nodeShapeProvider = nodeShapeProvider,
        )
        FlowchartActionBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            onUndo = {
                logStore.append(
                    level = StudioLogLevel.INFO,
                    source = "Flowchart",
                    message = "Undo ist aktuell noch Dummy",
                )
            },
            onRedo = {
                logStore.append(
                    level = StudioLogLevel.INFO,
                    source = "Flowchart",
                    message = "Redo ist aktuell noch Dummy",
                )
            },
            onZoomIn = {
                controller.dispatch(FlowInteractionAction.ZoomViewport(1.2, FlowPoint(0.0, 0.0)))
            },
            onZoomOut = {
                controller.dispatch(FlowInteractionAction.ZoomViewport(1 / 1.2, FlowPoint(0.0, 0.0)))
            },
            onViewToggle = { panelUi.compactView = !panelUi.compactView },
            onCenter = {
                controller.attachGraph(controller.snapshot().graph ?: graphDocument, null)
            },
            onGridToggle = { panelUi.gridVisible = !panelUi.gridVisible },
            gridEnabled = panelUi.gridVisible,
            compactView = panelUi.compactView,
        )
        FloatingActionButton(
            onClick = { panelUi.inspectorVisible = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(Icons.Default.Code, contentDescription = "Node-Inspector öffnen")
        }
        if (panelUi.inspectorVisible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .clickable { panelUi.inspectorVisible = false },
            )
            FlowchartNodeInspectorBottomSheet(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                graphDocument = graphDocument,
                selectedNodeId = panelUi.selectedNodeId,
                onDismiss = { panelUi.inspectorVisible = false },
            )
        }
    }
}

@Composable
private fun FlowchartActionBar(
    modifier: Modifier = Modifier,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onViewToggle: () -> Unit,
    onCenter: () -> Unit,
    onGridToggle: () -> Unit,
    gridEnabled: Boolean,
    compactView: Boolean,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TooltipIconButton(tooltip = "Undo", onClick = onUndo, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            TooltipIconButton(tooltip = "Redo", onClick = onRedo, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
            TooltipIconButton(tooltip = "Zoom -", onClick = onZoomOut, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom -")
            }
            TooltipIconButton(tooltip = "Zoom +", onClick = onZoomIn, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom +")
            }
            TooltipIconButton(tooltip = "Ansicht", onClick = onViewToggle, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = "Ansicht",
                    tint = if (compactView) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TooltipIconButton(tooltip = "Zentrieren", onClick = onCenter, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Zentrieren")
            }
            TooltipIconButton(tooltip = "Grid", onClick = onGridToggle, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = "Grid",
                    tint = if (gridEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FlowchartGridOverlay(
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
    Canvas(modifier = modifier) {
        val spacing = 24.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            x += spacing
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += spacing
        }
    }
}

@Composable
private fun FlowchartNodeInspectorBottomSheet(
    modifier: Modifier = Modifier,
    graphDocument: FlowGraphDocument,
    selectedNodeId: String?,
    onDismiss: () -> Unit,
) {
    val selectedNode = remember(graphDocument, selectedNodeId) {
        graphDocument.nodes.firstOrNull { it.id.value == selectedNodeId }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Flowchart Node Inspector",
                    style = MaterialTheme.typography.titleMedium,
                )
                TooltipIconButton(tooltip = "Schließen", onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Inspector schließen")
                }
            }
            if (selectedNode == null) {
                Text(
                    text = "Kein Node ausgewählt. Tippe im Flowchart auf einen Node und öffne den Inspector erneut.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                InfoInspectorRow("Label", selectedNode.label)
                InfoInspectorRow("Node-ID", selectedNode.id.value, mono = true)
                InfoInspectorRow(
                    "Semantic Kind",
                    selectedNode.kind.displayName ?: selectedNode.kind.standard?.name.orEmpty(),
                )
                val blockType = (selectedNode.properties["blockType"] as? FlowSemanticValue.StringValue)?.value
                blockType?.let { InfoInspectorRow("Block-Typ", it, mono = true) }
                val incoming = graphDocument.edges.count { it.targetNodeId == selectedNode.id }
                val outgoing = graphDocument.edges.count { it.sourceNodeId == selectedNode.id }
                InfoInspectorRow("Kanten", "in: $incoming | out: $outgoing")
                if (selectedNode.properties.isNotEmpty()) {
                    Text(
                        text = "Properties",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    selectedNode.properties.entries.sortedBy { it.key }.forEach { (key, value) ->
                        InfoInspectorRow(
                            label = key,
                            value = flowSemanticValueToText(value),
                            mono = true,
                        )
                    }
                }
                val diagnostics = graphDocument.diagnostics.filter { it.nodeId == selectedNode.id }
                if (diagnostics.isNotEmpty()) {
                    Text(
                        text = "Diagnostik",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    diagnostics.forEach { diagnostic ->
                        Text(
                            text = "• ${diagnostic.severity}: ${diagnostic.code} - ${diagnostic.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        ) {
            content()
        }
    }
}

@Composable
private fun InfoInspectorRow(
    label: String,
    value: String,
    mono: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (mono) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyMedium
            },
        )
    }
}

private fun flowSemanticValueToText(value: FlowSemanticValue): String = when (value) {
    FlowSemanticValue.NullValue -> "null"
    is FlowSemanticValue.BooleanValue -> value.value.toString()
    is FlowSemanticValue.NumberValue -> value.canonicalValue
    is FlowSemanticValue.StringValue -> value.value
    is FlowSemanticValue.ListValue -> value.values.joinToString(prefix = "[", postfix = "]") { flowSemanticValueToText(it) }
    is FlowSemanticValue.ObjectValue -> value.values.entries.joinToString(prefix = "{", postfix = "}") {
        "${it.key}=${flowSemanticValueToText(it.value)}"
    }
}

@Composable
private fun ColumnScope.FlowchartShapeLegendRail() {
    Text(
        text = "M3 Shape-Legende",
        style = MaterialTheme.typography.labelMedium,
    )
    val legend = remember { flowchartShapeLegendEntries() }
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        legend.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val path = flowchartLegendShapePath(entry.shapeId, size.width, size.height)
                    drawPath(
                        path = path,
                        color = entry.fillColor,
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFFC9D3FF),
                        style = Stroke(width = 1.5f),
                    )
                }
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.FlowchartCompactShapeRail(
    onExpandRequested: () -> Unit,
) {
    val legend = remember { flowchartShapeLegendEntries() }
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(scroll)
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        legend.forEach { entry ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(entry.fillColor.copy(alpha = 0.28f), CircleShape)
                    .clickable { onExpandRequested() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    val path = flowchartLegendShapePath(entry.shapeId, size.width, size.height)
                    drawPath(path = path, color = entry.fillColor)
                    drawPath(path = path, color = Color(0xFFE7ECFF), style = Stroke(width = 1.2f))
                }
            }
        }
    }
}

private data class FlowchartShapeLegendEntry(
    val shapeId: Int,
    val label: String,
    val fillColor: Color,
)

private fun flowchartShapeLegendEntries(): List<FlowchartShapeLegendEntry> = listOf(
    FlowchartShapeLegendEntry(1, "1 Start / Event", Color(0xFF7A6214)),
    FlowchartShapeLegendEntry(2, "2 Aktion", Color(0xFF4A5B8F)),
    FlowchartShapeLegendEntry(3, "3 Kontrolle", Color(0xFF7D4A22)),
    FlowchartShapeLegendEntry(4, "4 Entscheidung", Color(0xFF8454D2)),
    FlowchartShapeLegendEntry(5, "5 Vergleich", Color(0xFF2B8CD6)),
    FlowchartShapeLegendEntry(6, "6 Variable", Color(0xFF2A9D5E)),
    FlowchartShapeLegendEntry(7, "7 Berechnung", Color(0xFF4E7BB8)),
    FlowchartShapeLegendEntry(8, "8 IO / Daten", Color(0xFF5A8FC7)),
    FlowchartShapeLegendEntry(9, "9 Schleife", Color(0xFF2D8A6A)),
    FlowchartShapeLegendEntry(10, "10 Rueckgabe", Color(0xFF6E75A8)),
    FlowchartShapeLegendEntry(11, "11 Bedingung true", Color(0xFF3EA66A)),
    FlowchartShapeLegendEntry(12, "12 Bedingung false", Color(0xFFC76464)),
    FlowchartShapeLegendEntry(13, "13 Extern", Color(0xFF7A78C8)),
    FlowchartShapeLegendEntry(14, "14 Warnung", Color(0xFFE0A43E)),
    FlowchartShapeLegendEntry(15, "15 Unbekannt", Color(0xFF6C748A)),
)

private fun flowchartMaterialNodePath(
    node: FlowGraphNode,
    width: Float,
    height: Float,
): Path {
    val blockType = (node.properties["blockType"] as? FlowSemanticValue.StringValue)?.value.orEmpty()
    val shapeId = when {
        blockType.startsWith("event.") -> 1
        blockType.startsWith("control.if") -> 4
        blockType.startsWith("logic.compare") -> 5
        blockType.startsWith("variable.") || blockType.startsWith("variables.") -> 6
        blockType.startsWith("logic.") -> 7
        blockType.startsWith("control.") -> 9
        else -> 2
    }
    return flowchartLegendShapePath(shapeId, width, height)
}

private fun flowchartLegendShapePath(
    shapeId: Int,
    width: Float,
    height: Float,
): Path {
    val w = width.coerceAtLeast(8f)
    val h = height.coerceAtLeast(8f)
    val path = Path()
    when (shapeId) {
        1 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(h / 2f, h / 2f)))
        2 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(14f, 14f)))
        3 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(4f, 20f)))
        4 -> {
            path.moveTo(w / 2f, 0f)
            path.lineTo(w, h / 2f)
            path.lineTo(w / 2f, h)
            path.lineTo(0f, h / 2f)
            path.close()
        }
        5 -> {
            path.moveTo(12f, 0f); path.lineTo(w - 12f, 0f); path.lineTo(w, h / 2f)
            path.lineTo(w - 12f, h); path.lineTo(12f, h); path.lineTo(0f, h / 2f); path.close()
        }
        6 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(20f, 20f)))
        7 -> {
            path.moveTo(6f, 0f); path.lineTo(w - 6f, 0f); path.lineTo(w, 6f); path.lineTo(w, h - 6f)
            path.lineTo(w - 6f, h); path.lineTo(6f, h); path.lineTo(0f, h - 6f); path.lineTo(0f, 6f); path.close()
        }
        8 -> {
            path.moveTo(8f, 0f); path.lineTo(w, 0f); path.lineTo(w - 8f, h); path.lineTo(0f, h); path.close()
        }
        9 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(28f, 8f)))
        10 -> {
            path.moveTo(0f, 0f); path.lineTo(w - 10f, 0f); path.lineTo(w, h / 2f); path.lineTo(w - 10f, h)
            path.lineTo(0f, h); path.close()
        }
        11 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(24f, 4f)))
        12 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(4f, 24f)))
        13 -> {
            path.moveTo(0f, 6f); path.lineTo(6f, 0f); path.lineTo(w - 6f, 0f); path.lineTo(w, 6f)
            path.lineTo(w, h - 6f); path.lineTo(w - 6f, h); path.lineTo(6f, h); path.lineTo(0f, h - 6f); path.close()
        }
        14 -> {
            path.moveTo(w / 2f, 0f); path.lineTo(w, h); path.lineTo(0f, h); path.close()
        }
        else -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(10f, 10f)))
    }
    return path
}

@Composable
private fun ColumnScope.BrowserCompactRail(
    state: BrowserPanelState,
    onExpandRequested: () -> Unit,
) {
    val compactIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
    TooltipIconButton(
        tooltip = "Aktuelle URL speichern",
        onClick = { state.addCurrentToSavedLinks() },
    ) {
        Icon(Icons.Default.Favorite, contentDescription = "Favorit speichern", tint = compactIconTint)
    }
    TooltipIconButton(
        tooltip = "Linkliste öffnen",
        onClick = onExpandRequested,
    ) {
        Icon(Icons.Default.ViewList, contentDescription = "Linkliste", tint = compactIconTint)
    }
}

@Composable
private fun BrowserRailControls(state: BrowserPanelState) {
    var showUrlDialog by remember { mutableStateOf(false) }
    val savedLinksSnapshot = remember(state.savedLinks.size) { state.savedLinks.toList() }
    val iconTint = MaterialTheme.colorScheme.onSurface
    val iconBg = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier.width(186.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TooltipIconButton(
            tooltip = "URL laden",
            onClick = { showUrlDialog = true },
            modifier = Modifier.background(iconBg, CircleShape),
        ) {
            Icon(Icons.Default.Link, contentDescription = "Laden", tint = iconTint)
        }
        TooltipIconButton(
            tooltip = "Zurück",
            onClick = { state.goBack() },
            enabled = state.canGoBack,
            modifier = Modifier.background(iconBg, CircleShape),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = iconTint)
        }
        TooltipIconButton(
            tooltip = "Vor",
            onClick = { state.goForward() },
            enabled = state.canGoForward,
            modifier = Modifier.background(iconBg, CircleShape),
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Vor", tint = iconTint)
        }
        TooltipIconButton(
            tooltip = "Neu laden",
            onClick = { state.reload() },
            modifier = Modifier.background(iconBg, CircleShape),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Neu laden", tint = iconTint)
        }
        TooltipIconButton(
            tooltip = "Aktuelle URL als Favorit speichern",
            onClick = { state.addCurrentToSavedLinks() },
            modifier = Modifier.background(iconBg, CircleShape),
        ) {
            Icon(Icons.Default.Favorite, contentDescription = "Favorit speichern", tint = iconTint)
        }
        Text(
            text = "Linkliste",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(savedLinksSnapshot, key = { it }) { url ->
                AssistChip(
                    onClick = { state.loadSavedLink(url) },
                    label = { Text(url) },
                )
            }
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("URL eingeben") },
            text = {
                OutlinedTextField(
                    value = state.urlInput,
                    onValueChange = { state.urlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("https://...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            state.loadUrl(state.urlInput)
                            showUrlDialog = false
                        },
                        onDone = {
                            state.loadUrl(state.urlInput)
                            showUrlDialog = false
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}

@Composable
private fun MinimizedPanelDock(
    panels: List<PanelState>,
    onRestore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (panels.isEmpty()) return
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(panels, key = { it.id }) { panel ->
                AssistChip(
                    onClick = { onRestore(panel.id) },
                    label = { Text(panel.title) },
                    leadingIcon = {
                        Box(Modifier.size(10.dp).background(panel.accentColor, RoundedCornerShape(5.dp)))
                    }
                )
            }
        }
    }
}

@Composable
private fun GridBackground(gridSizeDp: Int) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = gridSizeDp * density
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridSize
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridSize
        }
    }
}
