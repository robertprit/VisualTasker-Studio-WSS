package com.visualtasker.wss.workspace.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.visualtasker.wss.components.IconMotionConfig
import com.visualtasker.wss.components.IconMotionEngine
import com.visualtasker.wss.components.DarkPanel
import com.visualtasker.wss.components.FabAction
import com.visualtasker.wss.components.M3EExpandableFAB
import com.visualtasker.wss.emscript.editor.EmScriptEditorScreen
import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.editor.EmscriptEditorSession
import com.visualtasker.wss.emscript.editor.EmscriptEditorUiState
import com.visualtasker.wss.emscript.editor.SyntaxHighlighter
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityGate
import com.visualtasker.wss.emscript.runtime.WorkspaceDryRunRuntime
import com.visualtasker.wss.flowchart.EmscriptDryRunFlowRuntimeMapper
import com.visualtasker.wss.data.PanelState as MainPanelState
import com.visualtasker.wss.data.PanelType as MainPanelType
import com.visualtasker.wss.grid.GridSystem
import com.visualtasker.wss.logging.StudioLogFilters
import com.visualtasker.wss.logging.StudioLogLevel
import com.visualtasker.wss.logging.StudioLogStore
import com.visualtasker.wss.workspace.model.WORKFLOW_SOURCE_BLOCKEDITOR_PREFIX
import com.visualtasker.wss.workspace.model.WORKFLOW_SOURCE_EMSCRIPT_APPLY
import com.visualtasker.wss.workspace.model.WORKFLOW_SOURCE_FLOWCHART_PREFIX
import com.visualtasker.wss.workspace.model.FlowchartWorkspaceMutation
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import com.visualtasker.wss.workspace.model.WorkspaceSyncGuard
import com.visualtasker.wss.workspace.model.applyFlowchartWorkspaceMutation
import com.visualtasker.wss.workspace.model.flowchartConnectionOptions
import com.visualtasker.wss.workspace.data.WorkspaceSessionSnapshot
import com.visualtasker.wss.workspace.data.WorkspaceSessionStore
import com.visualtasker.wss.workspace.data.defaultAccentForPanelType
import com.visualtasker.wss.workspace.data.supportedWorkspacePanelTypes
import com.visualtasker.wss.workspace.model.PanelAction
import com.visualtasker.wss.workspace.model.PanelActionSink
import com.visualtasker.wss.workspace.model.PanelState
import com.visualtasker.wss.workspace.model.PanelType
import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.StepStatus
import com.visualtasker.wss.workspace.plugin.ShellDocumentId
import com.visualtasker.wss.workspace.plugin.ShellEditorInput
import com.visualtasker.wss.workspace.plugin.ShellEditorOutput
import com.visualtasker.wss.workspace.plugin.ShellEditorOutputDisposition
import com.visualtasker.wss.workspace.plugin.ShellPluginHostServices
import com.visualtasker.wss.workspace.plugin.ShellPluginRuntimeState
import com.visualtasker.wss.workspace.plugin.ShellPluginSessionId
import com.visualtasker.wss.workspace.plugin.ShellSaveRequest
import com.visualtasker.wss.workspace.plugin.ShellDirtyState
import com.visualtasker.wss.workspace.plugin.ShellValidationResult
import com.visualtasker.wss.workspace.plugin.WorkspaceShellPluginHostCoordinator
import com.visualtasker.wss.workspace.plugin.blockeditor.BlockEditorShellEditorSession
import com.visualtasker.wss.workspace.plugin.blockeditor.BlockEditorShellPanel
import com.visualtasker.wss.workspace.plugin.defaultWorkspaceShellPluginRegistry
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartShellEditorSession
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartCompactNodeRail
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartNodeToolboxRail
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartShellPanel
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartShellPlugin
import com.visualtasker.wss.ui.theme.M3EColors
import de.visualtasker.blockeditor.compose.host.BlockPaletteInsertMode
import de.visualtasker.blockeditor.compose.icons.CategoryIcons
import de.visualtasker.blockeditor.compose.theme.defaultBlockCategoryColor
import de.visualtasker.blockeditor.compose.theme.setBlockCategoryColorOverride
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.blockeditor.compose.ui.CategoryPalettePanel
import de.visualtasker.blockeditor.serialization.BlockEditorDocumentFormats
import de.visualtasker.blockeditor.serialization.WorkspaceDecodeResult
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.interaction.FlowInteractionAction
import de.visualtasker.flowchart.serialization.FlowGraphJsonCodec
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val PANEL_MIN_W = 240f
private const val PANEL_MIN_H = 180f
private const val PANEL_DEFAULT_W = 320f
private const val PANEL_DEFAULT_H = 240f
private const val GRID_STEP_SMALL = 24f
private const val GRID_STEP_LARGE = 48f
private const val WORKSPACE_TOP_BAR_HEIGHT_DP = 64f
private const val WORKSPACE_PANEL_MARGIN_DP = 12f
private const val BLOCKEDITOR_WORKSPACE_PREF_KEY = "blockeditor_workspace_json"
private const val BLOCKEDITOR_TEST_WORKSPACE_VERSION_PREF_KEY = "blockeditor_test_workspace_version"
private const val BLOCKEDITOR_PALETTE_INSERT_MODE_PREF_KEY = "blockeditor_palette_insert_mode"
private const val TEXT_EDITOR_DRAFT_PREF_KEY = "workspace_text_editor_draft"
private const val TEXT_EDITOR_TEST_SCRIPT_VERSION_PREF_KEY = "workspace_text_editor_test_script_version"

private data class WorkspaceAppearance(
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

@OptIn(FlowPreview::class)
@Composable
fun WorkspaceScreen(
    actionSink: PanelActionSink? = null,
    recorderStepsProjection: (() -> List<RecorderStepUi>)? = null,
    themeMode: String = "dark",
    onThemeModeChange: (String) -> Unit = {},
    onMainScreenRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiPrefs = remember(context) { context.getSharedPreferences("panel_ui_options", Context.MODE_PRIVATE) }
    val sessionStore = remember(context) { WorkspaceSessionStore(context) }
    val saved = remember { sessionStore.load() }
    val panels = remember {
        mutableStateListOf<PanelState>().apply {
            if (saved?.panels.isNullOrEmpty()) {
                addAll(defaultPanels())
            } else {
                addAll(saved?.panels.orEmpty().filter { it.type in supportedWorkspacePanelTypes })
                if (isEmpty()) addAll(defaultPanels())
            }
        }
    }
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    var nextId by remember { mutableIntStateOf((panels.maxOfOrNull { it.id.removePrefix("panel-").toIntOrNull() ?: 0 } ?: 0) + 1) }
    var nextZ by remember { mutableIntStateOf((panels.maxOfOrNull { it.zIndex } ?: 0) + 1) }
    var focusedPanelId by remember { mutableStateOf(panels.maxByOrNull { it.zIndex }?.id.orEmpty()) }
    var hideSystemBars by remember { mutableStateOf(uiPrefs.getBoolean("hide_system_bars", false)) }
    var dockAtTop by remember { mutableStateOf(uiPrefs.getBoolean("dock_top", false)) }
    var useLargeGrid by remember { mutableStateOf(uiPrefs.getBoolean("grid_large", false)) }
    var uiScale by remember { mutableStateOf(uiPrefs.getFloat("ui_scale", 1f).coerceIn(0.7f, 1.5f)) }
    var snapEnabled by remember { mutableStateOf(uiPrefs.getBoolean("snap_enabled", true)) }
    var appearance by remember(uiPrefs) {
        mutableStateOf(
            WorkspaceAppearance(
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
            )
        )
    }
    var blockPaletteInsertMode by remember {
        mutableStateOf(
            runCatching {
                BlockPaletteInsertMode.valueOf(
                    uiPrefs.getString(
                        BLOCKEDITOR_PALETTE_INSERT_MODE_PREF_KEY,
                        BlockPaletteInsertMode.TapToAdd.name
                    ) ?: BlockPaletteInsertMode.TapToAdd.name
                )
            }.getOrDefault(BlockPaletteInsertMode.TapToAdd)
        )
    }
    var showAddPanelDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var settingsTab by remember { mutableIntStateOf(2) }
    var workflowState by remember(uiPrefs) {
        mutableStateOf(WorkspaceWorkflowState.fromSerialized(loadBlockEditorWorkspaceJson(uiPrefs)))
    }
    val workspaceUndoStack = remember(uiPrefs) { mutableStateListOf<String>() }
    val workspaceRedoStack = remember(uiPrefs) { mutableStateListOf<String>() }
    var lastWorkspaceChangeSource by remember(uiPrefs) { mutableStateOf<String?>(null) }
    var flowRuntimeSnapshot by remember { mutableStateOf<FlowRuntimeSnapshot?>(null) }
    val initialTextEditorDraft = remember(uiPrefs) {
        loadInitialTextEditorDraft(uiPrefs)
    }
    var emscriptSession by remember {
        mutableStateOf(
            EmscriptEditorSession.create(
                manualContent = initialTextEditorDraft,
                generatedContent = workflowState.emscriptProjection.getOrDefault("// Leerer Workspace")
            )
        )
    }
    val emscriptEditorUiState = remember { EmscriptEditorUiState() }
    val workspaceDryRunRuntime = remember { WorkspaceDryRunRuntime() }
    val workspaceSyncGuard = remember { WorkspaceSyncGuard() }
    var workspaceDryRunSequence by remember { mutableStateOf(0L) }
    var workspaceDryRunResult by remember { mutableStateOf<EmscriptDryRunResult?>(null) }
    var workspaceDryRunStepIndex by remember { mutableIntStateOf(0) }
    val activeBlockEditorSessionState = remember { mutableStateOf<BlockEditorShellEditorSession?>(null) }
    val activeFlowchartSessionState = remember { mutableStateOf<FlowchartShellEditorSession?>(null) }
    val emscriptFileManager = remember {
        EmscriptFileManagerUiState().apply {
            scripts["draft"] = initialTextEditorDraft
            EditorDefaults.allSamples.forEach { (name, script) ->
                scripts.putIfAbsent(name, script)
            }
        }
    }
    val studioLogStore = remember { StudioLogStore(maxEntries = 800) }
    val logConsoleState = remember { LogConsoleUiState() }
    fun replaceWorkflowStateFromJson(updated: String, source: String) {
        workflowState = WorkspaceWorkflowState.fromSerialized(updated, mutationSource = source)
        flowRuntimeSnapshot = null
        workspaceDryRunResult = null
        workspaceDryRunStepIndex = 0
        uiPrefs.edit().putString(BLOCKEDITOR_WORKSPACE_PREF_KEY, workflowState.serializedJson).apply()
    }
    val applyWorkspaceJsonChange: (String, String) -> Unit = applyWorkspaceJsonChange@{ updated, source ->
        if (updated != workflowState.serializedJson) {
            val syncReport = workspaceSyncGuard.inspect(updated)
            if (!syncReport.isValid) {
                studioLogStore.append(
                    level = StudioLogLevel.ERROR,
                    source = "WORKSPACE",
                    message = "Workspace Sync Guard abgebrochen",
                    details = syncReport.messages.joinToString(separator = "\n"),
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "workspace:sync-guard:$source"
                )
                return@applyWorkspaceJsonChange
            }
            if (!source.coalescesWith(lastWorkspaceChangeSource)) {
                workspaceUndoStack.add(workflowState.serializedJson)
            }
            workspaceRedoStack.clear()
            replaceWorkflowStateFromJson(updated, source)
            lastWorkspaceChangeSource = source
            val normalized = workflowState.serializedJson
            studioLogStore.append(
                level = StudioLogLevel.DEBUG,
                source = "WORKSPACE",
                message = "Blockeditor Workspace aktualisiert",
                details = "JSON=${normalized.length} Zeichen, Quelle=$source",
                documentRevision = workflowState.revision.toLong(),
                groupKey = "workspace:workflow-updated:$source"
            )
        }
    }
    val undoWorkspaceChange: () -> Boolean = {
        val previous = workspaceUndoStack.removeLastOrNull()
        if (previous == null) {
            false
        } else {
            workspaceRedoStack.add(workflowState.serializedJson)
            replaceWorkflowStateFromJson(previous, "workspace:undo")
            lastWorkspaceChangeSource = null
            true
        }
    }
    val redoWorkspaceChange: () -> Boolean = {
        val next = workspaceRedoStack.removeLastOrNull()
        if (next == null) {
            false
        } else {
            workspaceUndoStack.add(workflowState.serializedJson)
            replaceWorkflowStateFromJson(next, "workspace:redo")
            lastWorkspaceChangeSource = null
            true
        }
    }
    fun applyFlowchartMutation(
        mutation: FlowchartWorkspaceMutation,
        sourceSuffix: String,
    ) {
        val result = applyFlowchartWorkspaceMutation(workflowState.document, mutation)
        if (result.applied) {
            applyWorkspaceJsonChange(
                WorkspaceSerializer.serialize(result.document),
                "$WORKFLOW_SOURCE_FLOWCHART_PREFIX$sourceSuffix"
            )
        }
    }

    val addFlowchartNode: (String) -> Unit = { definitionId ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.AddNode(definitionId),
            definitionId,
        )
    }
    val deleteFlowchartNode: (FlowNodeId) -> Unit = { nodeId ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.DeleteNode(nodeId),
            "${nodeId.value}:delete",
        )
    }
    val deleteFlowchartNodes: (Set<FlowNodeId>) -> Unit = { nodeIds ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.DeleteNodes(nodeIds),
            "${nodeIds.joinToString(separator = ",") { it.value }}:delete-group",
        )
    }
    val disconnectFlowchartEdge: (FlowEdgeId) -> Unit = { edgeId ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.DisconnectEdge(
                graph = workflowState.flowchartProjection.graph,
                edgeId = edgeId,
            ),
            "${edgeId.value}:disconnect",
        )
    }
    val connectFlowchartNodes: (FlowNodeId, FlowNodeId, FlowEdgeKind, String?) -> Unit = { sourceNodeId, targetNodeId, kind, label ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.ConnectNodes(
                sourceNodeId = sourceNodeId,
                targetNodeId = targetNodeId,
                kind = kind,
                label = label,
            ),
            "${sourceNodeId.value}:${targetNodeId.value}:connect",
        )
    }
    val connectFlowchartPorts: (FlowNodeId, String, FlowNodeId, String, FlowEdgeKind) -> Unit = { sourceNodeId, sourcePortName, targetNodeId, targetPortName, fallbackKind ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.ConnectPorts(
                sourceNodeId = sourceNodeId,
                sourcePortName = sourcePortName,
                targetNodeId = targetNodeId,
                targetPortName = targetPortName,
                fallbackKind = fallbackKind,
            ),
            "${sourceNodeId.value}:$sourcePortName:${targetNodeId.value}:$targetPortName:connect",
        )
    }
    val updateFlowchartNodeField: (FlowNodeId, String, String) -> Unit = { nodeId, fieldKey, rawValue ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.UpdateNodeField(
                nodeId = nodeId,
                fieldKey = fieldKey,
                rawValue = rawValue,
            ),
            "${nodeId.value}:$fieldKey:update-field",
        )
    }
    val addFlowchartIfBranch: (FlowNodeId) -> Unit = { nodeId ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.AddIfBranch(nodeId),
            "${nodeId.value}:add-branch",
        )
    }
    val removeFlowchartIfBranch: (FlowNodeId) -> Unit = { nodeId ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.RemoveIfBranch(nodeId),
            "${nodeId.value}:remove-branch",
        )
    }
    val flowchartConnectionOptionsFor: (FlowNodeId, FlowNodeId) -> List<com.visualtasker.wss.workspace.model.FlowchartConnectionOption> = { sourceNodeId, targetNodeId ->
        flowchartConnectionOptions(
            document = workflowState.document,
            sourceNodeId = sourceNodeId,
            targetNodeId = targetNodeId,
        )
    }
    val syncFlowchartView: (FlowViewDocument) -> Unit = { viewDocument ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.SyncViewPositions(viewDocument),
            "${viewDocument.surfaceId.value}:move",
        )
    }
    val latestEmscriptProjected = workflowState.emscriptProjection.getOrDefault("// Leerer Workspace")
    val latestEmscriptGenerationFailure = workflowState.emscriptProjection.exceptionOrNull()?.message
    fun dryRunEventCount(result: EmscriptDryRunResult?): Int = when (result) {
        is EmscriptDryRunResult.Success -> result.events.size
        is EmscriptDryRunResult.Failure -> result.events.size
        null -> 0
    }
    fun renderWorkspaceDryRunStep(stepIndex: Int) {
        val result = workspaceDryRunResult ?: return
        val eventCount = dryRunEventCount(result)
        workspaceDryRunStepIndex = stepIndex.coerceIn(0, eventCount)
        workspaceDryRunSequence += 1
        flowRuntimeSnapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = workflowState.irGraph,
            graph = workflowState.flowchartProjection.graph,
            result = result,
            sequence = workspaceDryRunSequence,
            maxEventIndex = workspaceDryRunStepIndex,
        )
    }
    fun focusBlockFromFlowNode(nodeId: FlowNodeId) {
        val blockId = nodeId.value.removePrefix("block:").takeIf { it != nodeId.value } ?: return
        val session = activeBlockEditorSessionState.value ?: return
        val target = BlockId(blockId).takeIf { it in session.controller.document.blocks } ?: return
        session.controller.replaceWorkspaceDocument(
            newDocument = session.controller.document,
            recordHistory = false,
            focusBlockId = target,
            selectFocusedBlock = true,
        )
        studioLogStore.append(
            level = StudioLogLevel.DEBUG,
            source = "FLOWCHART",
            message = "Blockeditor auf Flowchart-Node fokussiert",
            details = "Node=${nodeId.value}, Block=$blockId",
            documentRevision = workflowState.revision.toLong(),
            groupKey = "flowchart:block-focus:$blockId"
        )
    }
    fun focusFlowNodeFromBlock(blockId: BlockId?) {
        val target = blockId?.let { FlowNodeId("block:${it.value}") } ?: return
        val session = activeFlowchartSessionState.value ?: return
        if (session.graphDocument.nodes.none { it.id == target }) return
        session.controller.dispatch(FlowInteractionAction.SelectNode(target))
        studioLogStore.append(
            level = StudioLogLevel.DEBUG,
            source = "BLOCKEDITOR",
            message = "Flowchart auf Block fokussiert",
            details = "Block=${blockId.value}, Node=${target.value}",
            documentRevision = workflowState.revision.toLong(),
            groupKey = "blockeditor:flow-focus:${blockId.value}"
        )
    }
    fun runCurrentWorkspaceDryRun(source: String) {
        if (workflowState.emscriptProjection.isFailure) {
            val message = workflowState.emscriptProjection.exceptionOrNull()?.message ?: "EMScript-Projektion nicht verfügbar."
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = source,
                message = "Dry-Run abgebrochen",
                details = message,
                documentRevision = workflowState.revision.toLong(),
                groupKey = "workspace:dry-run:projection-missing"
            )
            return
        }
        val result = workspaceDryRunRuntime.run(workflowState.document)
        workspaceDryRunResult = result
        workspaceDryRunStepIndex = dryRunEventCount(result)
        workspaceDryRunSequence += 1
        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = workflowState.irGraph,
            graph = workflowState.flowchartProjection.graph,
            result = result,
            sequence = workspaceDryRunSequence,
        )
        flowRuntimeSnapshot = snapshot
        snapshot.diagnostics.forEach { diagnostic ->
            studioLogStore.append(
                level = if (diagnostic.severity.name == "ERROR") StudioLogLevel.ERROR else StudioLogLevel.WARNING,
                source = source,
                message = "Runtime-Diagnose ${diagnostic.code}",
                details = diagnostic.message,
                documentRevision = workflowState.revision.toLong(),
                groupKey = "workspace:dry-run:diag:${diagnostic.code}:${diagnostic.nodeId?.value}:${diagnostic.message}"
            )
        }
        when (result) {
            is EmscriptDryRunResult.Success -> {
                val preview = result.events.takeLast(8).joinToString(separator = "\n") {
                    "#${it.index} ${it.kind.uppercase()}: ${it.message}"
                }
                studioLogStore.append(
                    level = StudioLogLevel.INFO,
                    source = source,
                    message = "Workspace Dry-Run erfolgreich",
                    details = "Events=${result.events.size}\n$preview",
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "workspace:dry-run:success:${snapshot.sequence}"
                )
            }
            is EmscriptDryRunResult.Failure -> {
                studioLogStore.append(
                    level = StudioLogLevel.ERROR,
                    source = source,
                    message = "Workspace Dry-Run fehlgeschlagen",
                    details = result.message,
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "workspace:dry-run:failure:${snapshot.sequence}"
                )
            }
        }
    }
    LaunchedEffect(latestEmscriptProjected, latestEmscriptGenerationFailure) {
        emscriptSession = emscriptSession.updateGeneratedFromBlocks(latestEmscriptProjected)
        latestEmscriptGenerationFailure?.let { message ->
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = "EMSCRIPT",
                message = "Projektion fehlgeschlagen",
                details = message,
                documentRevision = workflowState.revision.toLong(),
                groupKey = "emscript:projection-error:$message"
            )
        }
    }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, uiScale) {
        Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * uiScale
        )
    }
    val density = scaledDensity.density
    val gridSizeDp = if (useLargeGrid) GridSystem.GRID_SIZE_DP_LARGE else GridSystem.GRID_SIZE_DP_SMALL
    val workspaceTopGuardPx = (WORKSPACE_TOP_BAR_HEIGHT_DP + WORKSPACE_PANEL_MARGIN_DP) * density

    val demoRecorderSteps = remember {
        mutableStateListOf(
            RecorderStepUi("step-1", "Start App", "launch", StepStatus.Recorded),
            RecorderStepUi("step-2", "Tippe Login", "tap", StepStatus.Edited),
            RecorderStepUi("step-3", "Warte auf Element", "wait", StepStatus.Invalid),
            RecorderStepUi("step-4", "Bestaetigen", "tap", StepStatus.Executed)
        )
    }
    // Workspace shell stays truth-neutral: external projection wins over demo data.
    val projectedSteps = recorderStepsProjection?.invoke() ?: demoRecorderSteps

    val bridge = remember(actionSink, recorderStepsProjection, demoRecorderSteps) {
        object : PanelActionSink {
            override fun onPanelAction(action: PanelAction) {
                // Demo mutations are active only when no external projection is attached.
                if (recorderStepsProjection == null) {
                    when (action) {
                        is PanelAction.SelectStep -> Unit
                        is PanelAction.ReorderStep -> demoRecorderSteps.move(action.from, action.to)
                        is PanelAction.DeleteStep -> {
                            val index = demoRecorderSteps.indexOfFirst { it.id == action.stepId }
                            if (index >= 0) demoRecorderSteps.removeAt(index)
                        }
                        else -> Unit
                    }
                }
                actionSink?.onPanelAction(action)
            }
        }
    }
    val openPanel: (PanelType) -> Unit = { type ->
        val id = "panel-${nextId++}"
        val title = "${displayNameForPanelType(type)} ${id.removePrefix("panel-")}"
        panels.add(
            PanelState(
                id = id,
                type = type,
                title = title,
                x = 96f,
                y = max(96f, workspaceTopGuardPx),
                width = PANEL_DEFAULT_W,
                height = PANEL_DEFAULT_H,
                zIndex = nextZ++,
                minimized = false,
                accentColor = defaultAccentForPanelType(type)
            )
        )
        focusedPanelId = id
        studioLogStore.append(
            level = StudioLogLevel.INFO,
            source = "WORKSPACE",
            message = "Panel geöffnet",
            details = "$type ($title)",
            groupKey = "workspace:panel-opened:$type"
        )
        bridge.onPanelAction(PanelAction.OpenPanel(type))
    }

    LaunchedEffect(Unit) {
        snapshotFlow { WorkspaceSessionSnapshot(panels = panels.toList()) }
            .debounce(300)
            .collect { sessionStore.save(it) }
    }
    LaunchedEffect(hideSystemBars, dockAtTop, useLargeGrid, snapEnabled, uiScale, blockPaletteInsertMode) {
        uiPrefs.edit()
            .putBoolean("hide_system_bars", hideSystemBars)
            .putBoolean("dock_top", dockAtTop)
            .putBoolean("grid_large", useLargeGrid)
            .putBoolean("snap_enabled", snapEnabled)
            .putFloat("ui_scale", uiScale)
            .putString(BLOCKEDITOR_PALETTE_INSERT_MODE_PREF_KEY, blockPaletteInsertMode.name)
            .apply()
    }
    LaunchedEffect(appearance) {
        uiPrefs.edit()
            .putColor("color.syntax.keyword", appearance.syntaxKeyword)
            .putColor("color.syntax.control", appearance.syntaxControl)
            .putColor("color.syntax.string", appearance.syntaxString)
            .putColor("color.syntax.number", appearance.syntaxNumber)
            .putColor("color.syntax.comment", appearance.syntaxComment)
            .putColor("color.syntax.operator", appearance.syntaxOperator)
            .putColor("color.syntax.plain", appearance.syntaxPlain)
            .putColor("color.flow.event", appearance.flowEvent)
            .putColor("color.flow.control", appearance.flowControl)
            .putColor("color.flow.logic", appearance.flowLogic)
            .putColor("color.flow.variable", appearance.flowVariable)
            .putColor("color.block.event", appearance.blockEvent)
            .putColor("color.block.action", appearance.blockAction)
            .putColor("color.block.emscript", appearance.blockEmscript)
            .putColor("color.block.input", appearance.blockInput)
            .putColor("color.block.perception", appearance.blockPerception)
            .putColor("color.block.control", appearance.blockControl)
            .putColor("color.block.logic", appearance.blockLogic)
            .putColor("color.block.variables", appearance.blockVariables)
            .putColor("color.block.flow", appearance.blockFlow)
            .putColor("color.block.runtime", appearance.blockRuntime)
            .putColor("color.block.debug", appearance.blockDebug)
            .putColor("color.block.variable", appearance.blockVariable)
            .putColor("color.block.custom", appearance.blockCustom)
            .apply()

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
        if (hideSystemBars) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(Unit) {
        studioLogStore.append(
            level = StudioLogLevel.INFO,
            source = "workspace-shell",
            message = "Workspace Shell session gestartet",
            documentRevision = workflowState.revision.toLong()
        )
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onSizeChanged { surfaceSize = it }
        ) {
            GridBackground(visible = snapEnabled, stepDp = gridSizeDp.toFloat())

            WorkspaceTopAppBar(
                snapEnabled = snapEnabled,
                onSnapToggle = { snapEnabled = !snapEnabled },
                onAutoArrange = {
                    autoArrangePanels(
                        panels = panels,
                        surfaceSize = surfaceSize,
                        focusedPanelId = focusedPanelId,
                        topInsetPx = workspaceTopGuardPx
                    )
                },
                onOpenPanel = openPanel,
                onOpenSettings = { showSettingsSheet = true },
                onOpenMainScreen = onMainScreenRequested,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(10_000f)
            )

        panels.sortedBy { it.zIndex }.forEach { panel ->
            if (panel.minimized) return@forEach
            key(panel.id) {
                val maxWidthDp = ((surfaceSize.width - panel.x - 16f) / density).toInt().coerceAtLeast(PANEL_MIN_W.toInt())
                val panelTopPx = max(panel.y, workspaceTopGuardPx)
                val maxHeightDp = ((surfaceSize.height - panelTopPx - 16f) / density).toInt().coerceAtLeast(PANEL_MIN_H.toInt())
                val isBlockEditorPanel = panel.type == PanelType.BlockEditor
                val isFlowchartPanel = panel.type == PanelType.Flowchart
                val isLogConsolePanel = panel.type == PanelType.LogConsole || panel.type == PanelType.RuntimeLog
                val isEmscriptPanel = panel.type == PanelType.TextEditor || panel.type == PanelType.Emscript || panel.type == PanelType.DebugInfo
                val blockEditorSessionState = remember(panel.id) { mutableStateOf<BlockEditorShellEditorSession?>(null) }
                val flowchartSessionState = remember(panel.id) { mutableStateOf<FlowchartShellEditorSession?>(null) }
                DarkPanel(
                    panel = panel.toMainPanelState(),
                    snapEnabled = snapEnabled,
                    gridSizeDp = gridSizeDp,
                    isActiveTarget = panel.id == focusedPanelId,
                    maxWidth = maxWidthDp,
                    maxHeight = maxHeightDp,
                    showRail = true,
                    showDefaultRailIcons = !(isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel),
                    showRailColorPicker = !(isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel),
                    railExpandedWidth = when {
                        isBlockEditorPanel -> 300.dp
                        isFlowchartPanel -> 236.dp
                        isLogConsolePanel -> 220.dp
                        isEmscriptPanel -> 240.dp
                        else -> 186.dp
                    },
                    railExpandedFillHeight = isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel,
                    compactRailContent = { onExpandRequested ->
                        when {
                            isBlockEditorPanel -> BlockEditorCompactCategoryRail(
                                session = blockEditorSessionState.value,
                                onExpandRequested = onExpandRequested
                            )
                            isFlowchartPanel -> FlowchartCompactNodeRail(onExpandRequested = onExpandRequested)
                            isLogConsolePanel -> LogConsoleCompactRail(
                                store = studioLogStore,
                                uiState = logConsoleState
                            )
                            isEmscriptPanel -> EmscriptCompactRail(
                                onExpandRequested = onExpandRequested,
                                onSave = {
                                    val manual = emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                    if (manual != null) {
                                        val key = emscriptFileManager.currentName.trim().ifBlank { "draft" }
                                        emscriptFileManager.currentName = key
                                        emscriptFileManager.scripts[key] = manual.content
                                        uiPrefs.edit().putString(TEXT_EDITOR_DRAFT_PREF_KEY, manual.content).apply()
                                        studioLogStore.append(
                                            level = StudioLogLevel.INFO,
                                            source = "EMSCRIPT",
                                            message = "Script gespeichert",
                                            details = "Name=$key",
                                            documentRevision = workflowState.revision.toLong(),
                                            groupKey = "emscript:file-saved:$key"
                                        )
                                    }
                                },
                                onLoad = {
                                    val key = emscriptFileManager.currentName.trim().ifBlank { return@EmscriptCompactRail }
                                    val content = emscriptFileManager.scripts[key] ?: return@EmscriptCompactRail
                                    emscriptSession = emscriptSession
                                        .selectTab(EmscriptEditorSession.MANUAL_TAB_ID)
                                        .updateManualContent(content)
                                    uiPrefs.edit().putString(TEXT_EDITOR_DRAFT_PREF_KEY, content).apply()
                                    studioLogStore.append(
                                        level = StudioLogLevel.INFO,
                                        source = "EMSCRIPT",
                                        message = "Script geladen",
                                        details = "Name=$key",
                                        documentRevision = workflowState.revision.toLong(),
                                        groupKey = "emscript:file-loaded:$key"
                                    )
                                },
                                canLoad = emscriptFileManager.scripts.containsKey(emscriptFileManager.currentName.trim())
                            )
                        }
                    },
                    railContent = {
                        when {
                            isBlockEditorPanel -> BlockEditorPanelRail(
                                session = blockEditorSessionState.value,
                                paletteInsertMode = blockPaletteInsertMode
                            )
                            isFlowchartPanel -> FlowchartNodeToolboxRail(onAddNode = addFlowchartNode)
                            isLogConsolePanel -> LogConsoleExpandedRail(
                                store = studioLogStore,
                                uiState = logConsoleState
                            )
                            isEmscriptPanel -> EmscriptExpandedRail(
                                manager = emscriptFileManager,
                                onSave = {
                                    val manual = emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                    if (manual != null) {
                                        val key = emscriptFileManager.currentName.trim().ifBlank { "draft" }
                                        emscriptFileManager.currentName = key
                                        emscriptFileManager.scripts[key] = manual.content
                                        uiPrefs.edit().putString(TEXT_EDITOR_DRAFT_PREF_KEY, manual.content).apply()
                                        studioLogStore.append(
                                            level = StudioLogLevel.INFO,
                                            source = "EMSCRIPT",
                                            message = "Script gespeichert",
                                            details = "Name=$key",
                                            documentRevision = workflowState.revision.toLong(),
                                            groupKey = "emscript:file-saved:$key"
                                        )
                                    }
                                },
                                onLoad = { name ->
                                    val content = emscriptFileManager.scripts[name] ?: return@EmscriptExpandedRail
                                    emscriptFileManager.currentName = name
                                    emscriptSession = emscriptSession
                                        .selectTab(EmscriptEditorSession.MANUAL_TAB_ID)
                                        .updateManualContent(content)
                                    uiPrefs.edit().putString(TEXT_EDITOR_DRAFT_PREF_KEY, content).apply()
                                    studioLogStore.append(
                                        level = StudioLogLevel.INFO,
                                        source = "EMSCRIPT",
                                        message = "Script geladen",
                                        details = "Name=$name",
                                        documentRevision = workflowState.revision.toLong(),
                                        groupKey = "emscript:file-loaded:$name"
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
                                        documentRevision = workflowState.revision.toLong(),
                                        groupKey = "emscript:file-deleted:$name"
                                    )
                                },
                                onNew = {
                                    var idx = 1
                                    var next = "script-$idx"
                                    while (emscriptFileManager.scripts.containsKey(next)) {
                                        idx++
                                        next = "script-$idx"
                                    }
                                    emscriptFileManager.currentName = next
                                    emscriptFileManager.scripts[next] = ""
                                    emscriptSession = emscriptSession
                                        .selectTab(EmscriptEditorSession.MANUAL_TAB_ID)
                                        .updateManualContent("")
                                    uiPrefs.edit().putString(TEXT_EDITOR_DRAFT_PREF_KEY, "").apply()
                                }
                            )
                        }
                    },
                    onPositionChange = { newPos ->
                        updatePanel(panels, panel.id) {
                            val panelWidthPx = it.width * density
                            val panelHeightPx = it.height * density
                            val maxPanelY = max(workspaceTopGuardPx, surfaceSize.height - panelHeightPx - 16f)
                            it.copy(
                                x = newPos.x.coerceIn(0f, max(0f, surfaceSize.width - panelWidthPx - 16f)),
                                y = newPos.y.coerceIn(workspaceTopGuardPx, maxPanelY)
                            )
                        }
                    },
                    onSizeChange = { w, h ->
                        updatePanel(panels, panel.id) {
                            val boundedW = w.coerceIn(PANEL_MIN_W.toInt(), maxWidthDp)
                            val boundedH = h.coerceIn(PANEL_MIN_H.toInt(), maxHeightDp)
                            it.copy(width = boundedW.toFloat(), height = boundedH.toFloat())
                        }
                    },
                    onZIndexChange = {
                        updatePanel(panels, panel.id) { it.copy(zIndex = nextZ++) }
                    },
                    onFocusRequest = {
                        focusedPanelId = panel.id
                    },
                    onMinimizeToggle = {
                        updatePanel(panels, panel.id) { it.copy(minimized = true) }
                    },
                    onMaximizeToggle = {
                        updatePanel(panels, panel.id) { it.copy(isMaximized = !it.isMaximized) }
                    },
                    onClose = {
                        panels.removeAll { it.id == panel.id }
                        studioLogStore.append(
                            level = StudioLogLevel.INFO,
                            source = "WORKSPACE",
                            message = "Panel geschlossen",
                            details = "${panel.type} (${panel.title})",
                            groupKey = "workspace:panel-closed:${panel.type}"
                        )
                        bridge.onPanelAction(PanelAction.ClosePanel(panel.id))
                    },
                    onColorChange = { color ->
                        updatePanel(panels, panel.id) { it.copy(accentColor = color) }
                    },
                    modifier = Modifier.zIndex(panel.zIndex.toFloat())
                ) {
                    WorkspacePanelContent(
                        panel = panel,
                        steps = projectedSteps,
                        actionSink = bridge,
                        uiPrefs = uiPrefs,
                        workflowState = workflowState,
                        paletteInsertMode = blockPaletteInsertMode,
                        emscriptSession = emscriptSession,
                        emscriptEditorUiState = emscriptEditorUiState,
                        appearance = appearance,
                        latestEmscriptProjected = latestEmscriptProjected,
                        latestEmscriptGenerationFailure = latestEmscriptGenerationFailure,
                        flowRuntimeSnapshot = flowRuntimeSnapshot,
                        onEmscriptSessionChange = { updated ->
                            emscriptSession = updated
                            val manual = updated.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                            if (manual != null) {
                                uiPrefs.edit().putString(TEXT_EDITOR_DRAFT_PREF_KEY, manual.content).apply()
                            }
                        },
                        logStore = studioLogStore,
                        logConsoleState = logConsoleState,
                        onBlockEditorSessionReady = { session ->
                            blockEditorSessionState.value = session
                            if (session != null) {
                                activeBlockEditorSessionState.value = session
                            } else {
                                activeBlockEditorSessionState.value = null
                            }
                        },
                        onFlowchartSessionReady = { session ->
                            flowchartSessionState.value = session
                            if (session != null) {
                                activeFlowchartSessionState.value = session
                            } else {
                                activeFlowchartSessionState.value = null
                            }
                        },
                        onRunWorkspaceDry = { runCurrentWorkspaceDryRun("FLOWCHART") },
                        onDryRunStepBack = { renderWorkspaceDryRunStep(workspaceDryRunStepIndex - 1) },
                        onDryRunStepForward = {
                            if (workspaceDryRunResult == null) {
                                runCurrentWorkspaceDryRun("FLOWCHART")
                            } else {
                                renderWorkspaceDryRunStep(workspaceDryRunStepIndex + 1)
                            }
                        },
                        canDryRunStepBack = workspaceDryRunResult != null && workspaceDryRunStepIndex > 0,
                        canDryRunStepForward = workspaceDryRunStepIndex < dryRunEventCount(workspaceDryRunResult),
                        dryRunStepLabel = workspaceDryRunResult?.let { "${workspaceDryRunStepIndex}/${dryRunEventCount(it)}" },
                        onFlowchartNodeSelected = ::focusBlockFromFlowNode,
                        onBlockEditorBlockSelected = ::focusFlowNodeFromBlock,
                        onFlowchartNodeDelete = deleteFlowchartNode,
                        onFlowchartNodesDelete = deleteFlowchartNodes,
                        onFlowchartNodesConnect = connectFlowchartNodes,
                        onFlowchartPortsConnect = connectFlowchartPorts,
                        flowchartConnectionOptionsFor = flowchartConnectionOptionsFor,
                        onFlowchartEdgeDisconnect = disconnectFlowchartEdge,
                        onFlowchartNodeFieldUpdate = updateFlowchartNodeField,
                        onFlowchartIfBranchAdd = addFlowchartIfBranch,
                        onFlowchartIfBranchRemove = removeFlowchartIfBranch,
                        onFlowchartViewChanged = syncFlowchartView,
                        onWorkspaceUndo = undoWorkspaceChange,
                        onWorkspaceRedo = redoWorkspaceChange,
                        onFlowRuntimeSnapshotChange = { snapshot ->
                            flowRuntimeSnapshot = snapshot
                            studioLogStore.append(
                                level = StudioLogLevel.INFO,
                                source = "FLOWCHART",
                                message = "Dry-Run Runtime-Snapshot aktualisiert",
                                details = "Nodes=${snapshot.nodeStates.size}, Edges=${snapshot.traversedEdgeIds.size}, Diagnostics=${snapshot.diagnostics.size}",
                                documentRevision = workflowState.revision.toLong(),
                                groupKey = "flowchart:runtime-snapshot:${snapshot.sequence}"
                            )
                        },
                        onWorkspaceJsonChange = applyWorkspaceJsonChange
                    )
                }
            }
        }

        MinimizedDock(
            panels = panels.filter { it.minimized },
            onRestore = { id ->
                updatePanel(panels, id) { it.copy(minimized = false, zIndex = nextZ++) }
                focusedPanelId = id
            },
            modifier = Modifier
                .align(if (dockAtTop) Alignment.TopStart else Alignment.BottomStart)
                .padding(
                    start = 52.dp,
                    end = 12.dp,
                    top = if (dockAtTop) (WORKSPACE_TOP_BAR_HEIGHT_DP + WORKSPACE_PANEL_MARGIN_DP).dp else 0.dp,
                    bottom = if (dockAtTop) 0.dp else 12.dp
                )
        )

        M3EExpandableFAB(
            actions = listOf(
                FabAction(
                    icon = Icons.Default.AddCircle,
                    label = "Neues Panel",
                    color = MaterialTheme.colorScheme.primary
                ) { showAddPanelDialog = true },
                FabAction(
                    icon = Icons.Default.ViewKanban,
                    label = "BlockEditor öffnen",
                    color = M3EColors.Limepop
                ) {
                    openPanel(PanelType.BlockEditor)
                },
                FabAction(
                    icon = Icons.Default.Polyline,
                    label = "Flowchart öffnen",
                    color = M3EColors.Oceanneon
                ) {
                    openPanel(PanelType.Flowchart)
                },
                FabAction(
                    icon = Icons.Default.AutoAwesomeMosaic,
                    label = "Auto anordnen",
                    color = M3EColors.Oceanneon
                ) {
                    autoArrangePanels(
                        panels = panels,
                        surfaceSize = surfaceSize,
                        focusedPanelId = focusedPanelId
                    )
                },
                FabAction(
                    icon = Icons.Default.Settings,
                    label = "Einstellungen",
                    color = M3EColors.Ultraviolet
                ) {
                    showSettingsSheet = true
                },
                FabAction(
                    icon = Icons.Default.ViewKanban,
                    label = "MainScreen starten",
                    color = M3EColors.Amber
                ) {
                    onMainScreenRequested()
                }
            ),
            modifier = Modifier.align(Alignment.BottomEnd)
        )
        }
    }

    if (showAddPanelDialog) {
        AddPanelDialog(
            onSelect = { type ->
                openPanel(type)
                showAddPanelDialog = false
            },
            onDismiss = { showAddPanelDialog = false }
        )
    }

    if (showSettingsSheet) {
        WorkspaceSettingsBottomSheet(
            tabIndex = settingsTab,
            onTabChange = { settingsTab = it },
            hideSystemBars = hideSystemBars,
            onHideSystemBarsChange = { hideSystemBars = it },
            dockAtTop = dockAtTop,
            onDockAtTopChange = { dockAtTop = it },
            useLargeGrid = useLargeGrid,
            onUseLargeGridChange = { useLargeGrid = it },
            snapEnabled = snapEnabled,
            onSnapEnabledChange = { snapEnabled = it },
            uiScale = uiScale,
            onUiScaleChange = { uiScale = it.coerceIn(0.7f, 1.5f) },
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            appearance = appearance,
            onAppearanceChange = { appearance = it },
            blockPaletteInsertMode = blockPaletteInsertMode,
            onBlockPaletteInsertModeChange = { blockPaletteInsertMode = it },
            onResetPanels = {
                panels.clear()
                panels.addAll(defaultPanels())
                nextId = (panels.maxOfOrNull { it.id.removePrefix("panel-").toIntOrNull() ?: 0 } ?: 0) + 1
                nextZ = (panels.maxOfOrNull { it.zIndex } ?: 0) + 1
                focusedPanelId = panels.maxByOrNull { it.zIndex }?.id.orEmpty()
            },
            onAutoArrange = {
                autoArrangePanels(
                    panels = panels,
                    surfaceSize = surfaceSize,
                    focusedPanelId = focusedPanelId,
                    topInsetPx = workspaceTopGuardPx
                )
            },
            onColorPick = { color ->
                updatePanel(panels, focusedPanelId) { it.copy(accentColor = color) }
            },
            onToggleIconEngine = {
                IconMotionConfig.engine = if (IconMotionConfig.engine == IconMotionEngine.MATERIAL) {
                    IconMotionEngine.RIVE
                } else {
                    IconMotionEngine.MATERIAL
                }
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

private fun String.coalescesWith(previous: String?): Boolean =
    previous == this &&
        startsWith(WORKFLOW_SOURCE_FLOWCHART_PREFIX) &&
        endsWith(":move")

@Composable
private fun WorkspaceFloatingPanel(
    panel: PanelState,
    isFocused: Boolean,
    surfaceSize: IntSize,
    snapEnabled: Boolean,
    snapStep: Float,
    onFocus: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var liveX by remember(panel.id) { mutableFloatStateOf(panel.x) }
    var liveY by remember(panel.id) { mutableFloatStateOf(panel.y) }
    var liveW by remember(panel.id) { mutableFloatStateOf(panel.width) }
    var liveH by remember(panel.id) { mutableFloatStateOf(panel.height) }

    LaunchedEffect(panel.x, panel.y) {
        liveX = panel.x
        liveY = panel.y
    }
    LaunchedEffect(panel.width, panel.height) {
        liveW = panel.width
        liveH = panel.height
    }

    ElevatedCard(
        modifier = Modifier
            .offset { IntOffset(liveX.roundToInt(), liveY.roundToInt()) }
            .width(liveW.dp)
            .height(liveH.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .pointerInput(panel.id, snapEnabled, surfaceSize) {
                            detectDragGestures(
                                onDragStart = { onFocus() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    liveX += dragAmount.x
                                    liveY += dragAmount.y
                                    onMove(liveX, liveY)
                                },
                                onDragEnd = {
                                    if (snapEnabled) {
                                        val snappedX = snapValue(liveX, snapStep)
                                        val snappedY = snapValue(liveY, snapStep)
                                        liveX = snappedX
                                        liveY = snappedY
                                        onMove(snappedX, snappedY)
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = iconForPanelType(panel.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = panel.title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    TooltipIconButton(tooltip = "Minimieren", onClick = onMinimize, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Minimize, contentDescription = "Minimieren")
                    }
                    TooltipIconButton(tooltip = "Schließen", onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Schliessen")
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        content()
                    }
                }
            }

            if (!panel.locked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .pointerInput(panel.id) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                liveW += drag.x
                                liveH += drag.y
                                onResize(liveW, liveH)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Resize",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspacePanelContent(
    panel: PanelState,
    steps: List<RecorderStepUi>,
    actionSink: PanelActionSink,
    uiPrefs: android.content.SharedPreferences,
    workflowState: WorkspaceWorkflowState,
    paletteInsertMode: BlockPaletteInsertMode,
    emscriptSession: EmscriptEditorSession,
    emscriptEditorUiState: EmscriptEditorUiState,
    appearance: WorkspaceAppearance,
    latestEmscriptProjected: String,
    latestEmscriptGenerationFailure: String?,
    flowRuntimeSnapshot: FlowRuntimeSnapshot?,
    onEmscriptSessionChange: (EmscriptEditorSession) -> Unit,
    logStore: StudioLogStore,
    logConsoleState: LogConsoleUiState,
    onBlockEditorSessionReady: (BlockEditorShellEditorSession?) -> Unit = {},
    onFlowchartSessionReady: (FlowchartShellEditorSession?) -> Unit = {},
    onRunWorkspaceDry: () -> Unit = {},
    onDryRunStepBack: () -> Unit = {},
    onDryRunStepForward: () -> Unit = {},
    canDryRunStepBack: Boolean = false,
    canDryRunStepForward: Boolean = false,
    dryRunStepLabel: String? = null,
    onFlowchartNodeSelected: (FlowNodeId) -> Unit = {},
    onBlockEditorBlockSelected: (BlockId?) -> Unit = {},
    onFlowchartNodeDelete: (FlowNodeId) -> Unit = {},
    onFlowchartNodesDelete: (Set<FlowNodeId>) -> Unit = {},
    onFlowchartNodesConnect: (FlowNodeId, FlowNodeId, FlowEdgeKind, String?) -> Unit = { _, _, _, _ -> },
    onFlowchartPortsConnect: (FlowNodeId, String, FlowNodeId, String, FlowEdgeKind) -> Unit = { _, _, _, _, _ -> },
    flowchartConnectionOptionsFor: (FlowNodeId, FlowNodeId) -> List<com.visualtasker.wss.workspace.model.FlowchartConnectionOption> = { _, _ -> emptyList() },
    onFlowchartEdgeDisconnect: (FlowEdgeId) -> Unit = {},
    onFlowchartNodeFieldUpdate: (FlowNodeId, String, String) -> Unit = { _, _, _ -> },
    onFlowchartIfBranchAdd: (FlowNodeId) -> Unit = {},
    onFlowchartIfBranchRemove: (FlowNodeId) -> Unit = {},
    onFlowchartViewChanged: (FlowViewDocument) -> Unit = {},
    onWorkspaceUndo: () -> Boolean = { false },
    onWorkspaceRedo: () -> Boolean = { false },
    onFlowRuntimeSnapshotChange: (FlowRuntimeSnapshot) -> Unit = {},
    onWorkspaceJsonChange: (String, String) -> Unit
) {
    when (panel.type) {
        PanelType.RecorderSteps -> RecorderStepsPanel(steps = steps, actionSink = actionSink)
        PanelType.BlockEditor -> BlockEditorPanel(
            panelId = panel.id,
            uiPrefs = uiPrefs,
            workflowState = workflowState,
            paletteInsertMode = paletteInsertMode,
            onSessionReady = onBlockEditorSessionReady,
            onBlockSelected = onBlockEditorBlockSelected,
            onWorkspaceJsonChange = onWorkspaceJsonChange
        )
        PanelType.Flowchart -> FlowchartPanel(
            panelId = panel.id,
            uiPrefs = uiPrefs,
            graphContent = FlowGraphJsonCodec().encodeCanonical(workflowState.flowchartProjection.graph),
            runtimeSnapshot = flowRuntimeSnapshot,
            onRunDry = onRunWorkspaceDry,
            onStepBack = onDryRunStepBack,
            onStepForward = onDryRunStepForward,
            canStepBack = canDryRunStepBack,
            canStepForward = canDryRunStepForward,
            stepLabel = dryRunStepLabel,
            onNodeSelected = onFlowchartNodeSelected,
            onNodeDelete = onFlowchartNodeDelete,
            onNodesDelete = onFlowchartNodesDelete,
            onNodesConnect = onFlowchartNodesConnect,
            onPortsConnect = onFlowchartPortsConnect,
            connectionOptionsFor = flowchartConnectionOptionsFor,
            onEdgeDisconnect = onFlowchartEdgeDisconnect,
            onNodeFieldUpdate = onFlowchartNodeFieldUpdate,
            onIfBranchAdd = onFlowchartIfBranchAdd,
            onIfBranchRemove = onFlowchartIfBranchRemove,
            onViewChanged = onFlowchartViewChanged,
            onWorkspaceUndo = onWorkspaceUndo,
            onWorkspaceRedo = onWorkspaceRedo,
            onSessionReady = onFlowchartSessionReady
        )
        PanelType.TextEditor,
        PanelType.Emscript -> EmscriptTextEditorPanel(
            session = emscriptSession,
            uiState = emscriptEditorUiState,
            latestEmscriptProjected = latestEmscriptProjected,
            onSessionChange = onEmscriptSessionChange,
            logStore = logStore,
            workspaceJson = workflowState.serializedJson,
            currentFlowGraph = workflowState.flowchartProjection.graph,
            onWorkspaceJsonChange = { updated -> onWorkspaceJsonChange(updated, WORKFLOW_SOURCE_EMSCRIPT_APPLY) },
            onDryRunRuntimeSnapshot = onFlowRuntimeSnapshotChange,
            syntaxPaletteOverride = SyntaxHighlighter.Palette(
                keyword = appearance.syntaxKeyword,
                control = appearance.syntaxControl,
                parameter = Color(0xFFFFB74D),
                string = appearance.syntaxString,
                number = appearance.syntaxNumber,
                comment = appearance.syntaxComment,
                operator = appearance.syntaxOperator,
                plain = appearance.syntaxPlain,
            )
        )
        PanelType.RuntimeLog,
        PanelType.LogConsole -> LogConsolePanel(
            store = logStore,
            uiState = logConsoleState
        )
        PanelType.DebugInfo -> DebugInfoPanel(
            projectionStatus = EMSCRIPT_PROJECTION_STATUS_RUNNING,
            editingStatus = EMSCRIPT_EDITING_STATUS_NOT_IMPLEMENTED,
            overallStatus = EMSCRIPT_STATUS_READ_ONLY_PROJECTION,
            revision = workflowState.revision,
            projectedScript = latestEmscriptProjected,
            draft = emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }?.content.orEmpty(),
            flowRuntimeSnapshot = flowRuntimeSnapshot,
            onSaveDraft = {
                logStore.append(
                    level = StudioLogLevel.INFO,
                    source = "EMSCRIPT",
                    message = "Lokaler Draft gespeichert",
                    details = "Draft ist nicht auf Workspace angewendet",
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "emscript:draft-saved"
                )
            },
            onUseProjection = {
                onEmscriptSessionChange(emscriptSession.copyGeneratedToManual())
                logStore.append(
                    level = StudioLogLevel.INFO,
                    source = "EMSCRIPT",
                    message = "Projektion in lokalen Draft übernommen",
                    details = "Workspace bleibt unverändert",
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "emscript:draft-replaced-by-projection"
                )
            },
            diagnostics = buildList {
                add("EMScript Parser-Slice ist integriert (LET/SET/Literale/Variablen/Arithmetik/Compare/IF).")
                add("Automatisches Anwenden auf den Workspace bleibt vorerst deaktiviert.")
                add("Draft konnte erfolgreich in ein Workspace-Dokument übersetzt werden.")
                val syncReport = WorkspaceSyncGuard().inspect(workflowState.serializedJson)
                add(if (syncReport.isValid) "Workspace Sync Guard: OK" else "Workspace Sync Guard: BLOCKED")
                addAll(syncReport.messages.take(5))
                val capabilityReport = RuntimeCapabilityGate().inspect(workflowState.document)
                add(capabilityReport.summary)
                capabilityReport.capabilities
                    .groupingBy { it.status }
                    .eachCount()
                    .entries
                    .sortedBy { it.key.name }
                    .forEach { (status, count) -> add("Runtime $status: $count") }
                capabilityReport.capabilities.take(10).forEach { capability ->
                    add("${capability.command}: ${capability.status} - ${capability.details}")
                }
                flowRuntimeSnapshot?.diagnostics?.take(8)?.forEach { diagnostic ->
                    add("${diagnostic.severity.name} ${diagnostic.code}: ${diagnostic.message}")
                }
                latestEmscriptGenerationFailure?.let(::add)
            }
        )
        PanelType.Screenshot,
        PanelType.Marker,
        PanelType.M3Director -> Unit
    }
}

@Composable
private fun WorkspaceTopAppBar(
    snapEnabled: Boolean,
    onSnapToggle: () -> Unit,
    onAutoArrange: () -> Unit,
    onOpenPanel: (PanelType) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMainScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(WORKSPACE_TOP_BAR_HEIGHT_DP.dp)
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "VT Studio WSS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TooltipIconButton(tooltip = "Neues Panel", onClick = { onOpenPanel(PanelType.BlockEditor) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ViewKanban, contentDescription = "BlockEditor öffnen")
                }
                TooltipIconButton(tooltip = "Flowchart öffnen", onClick = { onOpenPanel(PanelType.Flowchart) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Polyline, contentDescription = "Flowchart öffnen")
                }
                TooltipIconButton(tooltip = "TextEditor öffnen", onClick = { onOpenPanel(PanelType.TextEditor) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Article, contentDescription = "TextEditor öffnen")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TooltipIconButton(tooltip = "Auto anordnen", onClick = onAutoArrange, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.AutoAwesomeMosaic, contentDescription = "Auto Arrange")
                }
                TooltipIconButton(tooltip = "Snap ${if (snapEnabled) "deaktivieren" else "aktivieren"}", onClick = onSnapToggle, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.GridView, contentDescription = "Snap ${if (snapEnabled) "an" else "aus"}")
                }
                TooltipIconButton(tooltip = "Einstellungen", onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                }
                TooltipIconButton(tooltip = "MainScreen starten", onClick = onOpenMainScreen, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.AutoAwesomeMosaic, contentDescription = "MainScreen starten")
                }
            }
        }
    }
}

@Composable
private fun WorkspaceRail(
    snapEnabled: Boolean,
    onSnapToggle: () -> Unit,
    onAutoArrange: () -> Unit,
    onOpenPanel: (PanelType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(0.78f).width(40.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TooltipIconButton(tooltip = "Auto anordnen", onClick = onAutoArrange, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.AutoAwesomeMosaic, contentDescription = "Auto Arrange")
            }
            TooltipIconButton(tooltip = "Snap umschalten", onClick = onSnapToggle, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.GridView, contentDescription = "Snap ${if (snapEnabled) "an" else "aus"}")
            }
            Spacer(modifier = Modifier.height(4.dp))
            listOf(
                PanelType.RecorderSteps,
                PanelType.BlockEditor,
                PanelType.Flowchart,
                PanelType.TextEditor,
                PanelType.LogConsole,
                PanelType.DebugInfo
            ).forEach { type ->
                val displayName = displayNameForPanelType(type)
                TooltipIconButton(tooltip = "Panel $displayName", onClick = { onOpenPanel(type) }, modifier = Modifier.size(28.dp)) {
                    Icon(iconForPanelType(type), contentDescription = displayName)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TooltipIconButton(
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
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecorderStepsPanel(
    steps: List<RecorderStepUi>,
    actionSink: PanelActionSink
) {
    var selectedStepId by remember { mutableStateOf<String?>(null) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val thresholdPx = 56f

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("RecorderStepsPanel", style = MaterialTheme.typography.titleSmall)
        Text("Tippen = Select, Drag Handle = Reorder, Swipe = Delete", color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(steps, key = { _, item -> item.id }) { index, step ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            actionSink.onPanelAction(PanelAction.DeleteStep(step.id))
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {},
                    content = {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedStepId = step.id
                                    actionSink.onPanelAction(PanelAction.SelectStep(step.id))
                                }
                                .pointerInput(step.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragEnd = { dragAccumulator = 0f },
                                        onDragCancel = { dragAccumulator = 0f },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            dragAccumulator += drag.y
                                            if (dragAccumulator > thresholdPx && index < steps.lastIndex) {
                                                actionSink.onPanelAction(PanelAction.ReorderStep(index, index + 1))
                                                dragAccumulator = 0f
                                            } else if (dragAccumulator < -thresholdPx && index > 0) {
                                                actionSink.onPanelAction(PanelAction.ReorderStep(index, index - 1))
                                                dragAccumulator = 0f
                                            }
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedStepId == step.id) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(step.label, fontWeight = FontWeight.Medium)
                                    Text("${step.actionType} - ${step.status.name}", color = statusColor(step.status))
                                }
                                AssistChip(
                                    onClick = {
                                        selectedStepId = step.id
                                        actionSink.onPanelAction(PanelAction.SelectStep(step.id))
                                    },
                                    label = { Text("Select") }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BlockEditorPanel(
    panelId: String,
    uiPrefs: android.content.SharedPreferences,
    workflowState: WorkspaceWorkflowState,
    paletteInsertMode: BlockPaletteInsertMode,
    onSessionReady: (BlockEditorShellEditorSession?) -> Unit,
    onBlockSelected: (BlockId?) -> Unit,
    onWorkspaceJsonChange: (String, String) -> Unit
) {
    val hostServices = remember(panelId) { WorkspaceShellUiPluginHostAdapter() }
    val pluginRegistry = remember { defaultWorkspaceShellPluginRegistry() }
    val coordinator = remember(hostServices, pluginRegistry) {
        WorkspaceShellPluginHostCoordinator(
            hostServices = hostServices,
            pluginLookup = pluginRegistry::findEditorPlugin
        )
    }
    val boundEditor = remember(panelId) {
        coordinator.openEditor(
            shellPanelTypeName = "BlockEditor",
            input = ShellEditorInput(
                sessionId = ShellPluginSessionId("blockeditor-$panelId"),
                documentId = ShellDocumentId("workflow-main"),
                formatId = BlockEditorDocumentFormats.WORKSPACE_JSON,
                revision = null,
                content = workflowState.serializedJson
            )
        )
    }
    val session = boundEditor.session as BlockEditorShellEditorSession
    val sessionSource = "$WORKFLOW_SOURCE_BLOCKEDITOR_PREFIX$panelId"
    LaunchedEffect(session) {
        onSessionReady(session)
    }
    LaunchedEffect(session, workflowState.revision, workflowState.mutationSource) {
        val current = WorkspaceSerializer.serialize(session.controller.document)
        if (
            workflowState.mutationSource != sessionSource &&
            current != workflowState.serializedJson
        ) {
            session.replaceInputDocument(
                ShellEditorInput(
                    sessionId = session.sessionId,
                    documentId = ShellDocumentId("workflow-main"),
                    formatId = BlockEditorDocumentFormats.WORKSPACE_JSON,
                    revision = workflowState.revision.toString(),
                    content = workflowState.serializedJson
                )
            )
            onSessionReady(session)
        }
    }

    DisposableEffect(boundEditor, uiPrefs, onSessionReady) {
        onDispose {
            persistBlockEditorSession(uiPrefs, session)
            onSessionReady(null)
            boundEditor.close()
        }
    }
    LaunchedEffect(session, onWorkspaceJsonChange) {
        snapshotFlow { session.controller.document }
            .collect { document ->
                onWorkspaceJsonChange(WorkspaceSerializer.serialize(document), sessionSource)
            }
    }
    LaunchedEffect(session, onBlockSelected) {
        snapshotFlow { session.controller.selectedBlockIds.singleOrNull() }
            .collect(onBlockSelected)
    }

    BlockEditorShellPanel(
        session = session,
        onSave = { persistBlockEditorSession(uiPrefs, session) },
        uiConfig = de.visualtasker.blockeditor.compose.host.BlockEditorHostUiConfig(
            showBottomPanel = false,
            showFloatingInspector = true,
            showBottomPanelToggle = false,
            showBlockFactory = true,
            showToolbox = false,
            allowClearWorkspace = true,
            paletteInsertMode = paletteInsertMode,
            soundEffectsEnabled = true,
            hapticFeedbackEnabled = true
        ),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ColumnScope.BlockEditorCompactCategoryRail(
    session: BlockEditorShellEditorSession?,
    onExpandRequested: () -> Unit
) {
    val categories = remember {
        BlockCategories.all.filter { it.id != BlockCategories.CUSTOM }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val accent = Color(category.accentArgb)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (session?.controller?.expandedCategory == category.id) {
                            accent.copy(alpha = 0.46f)
                        } else {
                            accent.copy(alpha = 0.30f)
                        }
                    )
                    .clickable {
                        session?.controller?.onCategoryClick(category.id)
                        onExpandRequested()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CategoryIcons.forCategory(category.id),
                    contentDescription = category.label,
                    tint = if (session?.controller?.expandedCategory == category.id) Color.White else accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun BlockEditorPanelRail(
    session: BlockEditorShellEditorSession?,
    paletteInsertMode: BlockPaletteInsertMode
) {
    if (session == null) {
        Text(
            text = "BlockEditor",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    CategoryPalettePanel(
        category = session.controller.expandedCategory,
        definitions = session.controller.definitionsForExpandedCategory(),
        allDefinitions = session.controller.registry.allDefinitions(),
        insertMode = paletteInsertMode,
        onAddBlock = session.controller::addBlockFromPalette,
        onCreateVariable = session.controller::createVariable,
        onDismiss = session.controller::dismissCategory,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    )
}

@Composable
private fun FlowchartPanel(
    panelId: String,
    uiPrefs: android.content.SharedPreferences,
    graphContent: String,
    runtimeSnapshot: FlowRuntimeSnapshot?,
    onRunDry: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    canStepBack: Boolean,
    canStepForward: Boolean,
    stepLabel: String?,
    onNodeSelected: (FlowNodeId) -> Unit,
    onNodeDelete: (FlowNodeId) -> Unit,
    onNodesDelete: (Set<FlowNodeId>) -> Unit,
    onNodesConnect: (FlowNodeId, FlowNodeId, FlowEdgeKind, String?) -> Unit,
    onPortsConnect: (FlowNodeId, String, FlowNodeId, String, FlowEdgeKind) -> Unit,
    connectionOptionsFor: (FlowNodeId, FlowNodeId) -> List<com.visualtasker.wss.workspace.model.FlowchartConnectionOption>,
    onEdgeDisconnect: (FlowEdgeId) -> Unit,
    onNodeFieldUpdate: (FlowNodeId, String, String) -> Unit,
    onIfBranchAdd: (FlowNodeId) -> Unit,
    onIfBranchRemove: (FlowNodeId) -> Unit,
    onViewChanged: (FlowViewDocument) -> Unit,
    onWorkspaceUndo: () -> Boolean,
    onWorkspaceRedo: () -> Boolean,
    onSessionReady: (FlowchartShellEditorSession?) -> Unit
) {
    val hostServices = remember(panelId) { WorkspaceShellUiPluginHostAdapter() }
    val pluginRegistry = remember { defaultWorkspaceShellPluginRegistry() }
    val coordinator = remember(hostServices, pluginRegistry) {
        WorkspaceShellPluginHostCoordinator(
            hostServices = hostServices,
            pluginLookup = pluginRegistry::findEditorPlugin
        )
    }
    val boundEditor = remember(panelId) {
        coordinator.openEditor(
            shellPanelTypeName = "Flowchart",
            input = ShellEditorInput(
                sessionId = ShellPluginSessionId("flowchart-$panelId"),
                documentId = ShellDocumentId("workflow-main-flowchart"),
                formatId = FlowchartShellPlugin.FLOW_GRAPH_JSON,
                revision = null,
                content = graphContent
            )
        )
    }
    val session = boundEditor.session as FlowchartShellEditorSession
    LaunchedEffect(session, onSessionReady) {
        onSessionReady(session)
    }
    LaunchedEffect(session, graphContent) {
        session.replaceGraphContent(graphContent)
    }

    DisposableEffect(boundEditor, uiPrefs, onSessionReady) {
        onDispose {
            persistFlowchartViewSession(uiPrefs, session)
            onSessionReady(null)
            boundEditor.close()
        }
    }

    FlowchartShellPanel(
        session = session,
        runtimeSnapshot = runtimeSnapshot,
        onRunDry = onRunDry,
        onStepBack = onStepBack,
        onStepForward = onStepForward,
        canStepBack = canStepBack,
        canStepForward = canStepForward,
        stepLabel = stepLabel,
        onNodeSelected = onNodeSelected,
        onDeleteNode = onNodeDelete,
        onDeleteNodes = onNodesDelete,
        onConnectNodes = onNodesConnect,
        onConnectPorts = onPortsConnect,
        connectionOptionsFor = connectionOptionsFor,
        onDisconnectEdge = onEdgeDisconnect,
        onUpdateNodeField = onNodeFieldUpdate,
        onAddIfBranch = onIfBranchAdd,
        onRemoveIfBranch = onIfBranchRemove,
        onViewChanged = onViewChanged,
        onUndoWorkspace = onWorkspaceUndo,
        onRedoWorkspace = onWorkspaceRedo,
        onSave = { persistFlowchartViewSession(uiPrefs, session) },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MinimizedDock(
    panels: List<PanelState>,
    onRestore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (panels.isEmpty()) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(panels, key = { _, panel -> panel.id }) { _, panel ->
                AssistChip(onClick = { onRestore(panel.id) }, label = { Text(panel.title) })
            }
        }
    }
}

@Composable
private fun GridBackground(visible: Boolean, stepDp: Float) {
    if (!visible) return
    val line = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        var x = 0f
        while (x < size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += stepDp * density
        }
        var y = 0f
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += stepDp * density
        }
    }
}

private fun defaultPanels(): List<PanelState> = listOf(
    workspacePanel("panel-1", PanelType.RecorderSteps, "Stepper 1", 84f, 112f, 360f, 360f, 1),
    workspacePanel("panel-2", PanelType.BlockEditor, "BlockEditor 2", 470f, 124f, 360f, 300f, 2),
    workspacePanel("panel-3", PanelType.Flowchart, "Flowchart 3", 470f, 460f, 360f, 300f, 3),
    workspacePanel("panel-4", PanelType.LogConsole, "LogConsole 4", 860f, 150f, 320f, 240f, 4)
)

private fun workspacePanel(
    id: String,
    type: PanelType,
    title: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    zIndex: Int
): PanelState =
    PanelState(
        id = id,
        type = type,
        title = title,
        x = x,
        y = y,
        width = width,
        height = height,
        zIndex = zIndex,
        minimized = false,
        accentColor = defaultAccentForPanelType(type)
    )

private fun autoArrangePanels(
    panels: MutableList<PanelState>,
    surfaceSize: IntSize,
    focusedPanelId: String,
    topInsetPx: Float = WORKSPACE_TOP_BAR_HEIGHT_DP + WORKSPACE_PANEL_MARGIN_DP
) {
    val visible = panels.filter { !it.minimized }.sortedBy { it.zIndex }
    if (visible.isEmpty() || surfaceSize.width <= 0 || surfaceSize.height <= 0) return

    val columns = max(1, ceil(sqrt(visible.size.toFloat())).toInt())
    val gap = 14f
    val availableWidth = surfaceSize.width - 64f
    val availableHeight = surfaceSize.height - topInsetPx - 32f
    val cellW = ((availableWidth - ((columns - 1) * gap)) / columns).coerceAtLeast(PANEL_MIN_W)
    val rows = ceil(visible.size / columns.toFloat()).toInt()
    val cellH = ((availableHeight - ((rows - 1) * gap)) / rows).coerceAtLeast(PANEL_MIN_H)

    visible.forEachIndexed { index, panel ->
        val col = index % columns
        val row = index / columns
        val isFocused = panel.id == focusedPanelId
        val targetW = if (isFocused) (cellW * 1.08f).coerceAtMost(availableWidth) else cellW
        val targetH = if (isFocused) (cellH * 1.08f).coerceAtMost(availableHeight) else cellH
        val nx = 52f + col * (cellW + gap)
        val ny = topInsetPx + row * (cellH + gap)
        val idx = panels.indexOfFirst { it.id == panel.id }
        if (idx >= 0) {
            panels[idx] = panels[idx].copy(
                x = nx,
                y = ny,
                width = targetW,
                height = targetH
            )
        }
    }
}

private fun updatePanel(
    panels: MutableList<PanelState>,
    id: String,
    updater: (PanelState) -> PanelState
) {
    val index = panels.indexOfFirst { it.id == id }
    if (index >= 0) panels[index] = updater(panels[index])
}

private fun MutableList<RecorderStepUi>.move(from: Int, to: Int) {
    if (from == to || from !in indices || to !in indices) return
    val item = removeAt(from)
    add(to, item)
}

private fun iconForPanelType(type: PanelType) = when (type) {
    PanelType.RecorderSteps -> Icons.AutoMirrored.Filled.Subject
    PanelType.BlockEditor -> Icons.Default.ViewKanban
    PanelType.Flowchart -> Icons.Default.Polyline
    PanelType.Screenshot -> Icons.Default.Photo
    PanelType.Marker -> Icons.Default.TouchApp
    PanelType.Emscript -> Icons.Default.Terminal
    PanelType.RuntimeLog -> Icons.Default.PlayArrow
    PanelType.TextEditor -> Icons.Default.Article
    PanelType.LogConsole -> Icons.Default.BugReport
    PanelType.DebugInfo -> Icons.Default.Terminal
    PanelType.M3Director -> Icons.Default.SmartToy
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPanelDialog(
    onSelect: (PanelType) -> Unit,
    onDismiss: () -> Unit
) {
    val panelTypes = listOf(
        PanelType.RecorderSteps,
        PanelType.BlockEditor,
        PanelType.Flowchart,
        PanelType.TextEditor,
        PanelType.LogConsole,
        PanelType.DebugInfo
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Panel-Typ auswählen", style = MaterialTheme.typography.titleMedium)
            panelTypes.forEach { type ->
                AssistChip(
                    onClick = { onSelect(type) },
                    label = { Text(displayNameForPanelType(type)) },
                    leadingIcon = { Icon(iconForPanelType(type), contentDescription = null) }
                )
            }
        }
    }
}

private fun displayNameForPanelType(type: PanelType): String = when (type) {
    PanelType.RecorderSteps -> "Stepper"
    PanelType.BlockEditor -> "BlockEditor"
    PanelType.Flowchart -> "Flowchart"
    PanelType.Screenshot -> "Screenshot"
    PanelType.Marker -> "Marker"
    PanelType.Emscript -> "EMScript"
    PanelType.RuntimeLog -> "RuntimeLog"
    PanelType.TextEditor -> "TextEditor"
    PanelType.LogConsole -> "LogConsole"
    PanelType.DebugInfo -> "Debug"
    PanelType.M3Director -> "M3Director"
}

private fun PanelState.toMainPanelState(): MainPanelState =
    MainPanelState(
        id = id,
        position = Offset(x, y),
        width = width.toInt(),
        height = height.toInt(),
        accentColor = accentColor,
        title = title,
        panelType = toMainPanelType(type),
        zIndex = zIndex,
        isMinimized = minimized,
        isMaximized = isMaximized
    )

private fun toMainPanelType(type: PanelType): MainPanelType = when (type) {
    PanelType.RecorderSteps -> MainPanelType.LIST_TEST
    PanelType.BlockEditor -> MainPanelType.BLOCKEDITOR
    PanelType.Flowchart -> MainPanelType.FLOWCHART
    PanelType.RuntimeLog -> MainPanelType.LOG_CONSOLE
    PanelType.LogConsole -> MainPanelType.LOG_CONSOLE
    PanelType.TextEditor -> MainPanelType.EDITOR
    PanelType.Emscript -> MainPanelType.EMSCRIPT
    PanelType.DebugInfo -> MainPanelType.EMSCRIPT
    PanelType.Screenshot,
    PanelType.Marker,
    PanelType.M3Director -> MainPanelType.LIST_TEST
}

private fun loadBlockEditorWorkspaceJson(
    uiPrefs: android.content.SharedPreferences
): String {
    val loadedTestVersion = uiPrefs.getInt(BLOCKEDITOR_TEST_WORKSPACE_VERSION_PREF_KEY, 0)
    val persisted = uiPrefs.getString(BLOCKEDITOR_WORKSPACE_PREF_KEY, null)
    if (loadedTestVersion < EditorDefaults.integrationTestScriptVersion || persisted.isNullOrBlank()) {
        importAndPersistIntegrationWorkspace(uiPrefs)?.let { return it }
    }
    return persisted
        ?.let {
            when (val decoded = WorkspaceSerializer.decode(it)) {
                is WorkspaceDecodeResult.Decoded -> WorkspaceSerializer.serialize(decoded.document)
                is WorkspaceDecodeResult.Malformed -> importAndPersistIntegrationWorkspace(uiPrefs)
                is WorkspaceDecodeResult.UnsupportedSchema -> importAndPersistIntegrationWorkspace(uiPrefs)
            }
        }
        ?: importAndPersistIntegrationWorkspace(uiPrefs)
        ?: WorkspaceSerializer.serialize(WorkspaceBootstrap.starter())
}

private fun loadInitialTextEditorDraft(
    uiPrefs: android.content.SharedPreferences
): String {
    val loadedVersion = uiPrefs.getInt(TEXT_EDITOR_TEST_SCRIPT_VERSION_PREF_KEY, 0)
    val persisted = uiPrefs.getString(TEXT_EDITOR_DRAFT_PREF_KEY, null)
    if (loadedVersion < EditorDefaults.integrationTestScriptVersion || persisted.isNullOrBlank()) {
        uiPrefs.edit()
            .putString(TEXT_EDITOR_DRAFT_PREF_KEY, EditorDefaults.commandCatalogBreadthTestScript)
            .putInt(TEXT_EDITOR_TEST_SCRIPT_VERSION_PREF_KEY, EditorDefaults.integrationTestScriptVersion)
            .apply()
        return EditorDefaults.commandCatalogBreadthTestScript
    }
    return persisted
}

private fun importAndPersistIntegrationWorkspace(
    uiPrefs: android.content.SharedPreferences,
): String? = EmscriptWorkspaceImporter()
    .import(EditorDefaults.commandCatalogBreadthTestScript, workspaceId = "workflow-main")
    .document
    ?.let { document ->
        val serialized = WorkspaceSerializer.serialize(document)
        uiPrefs.edit()
            .putString(BLOCKEDITOR_WORKSPACE_PREF_KEY, serialized)
            .putInt(BLOCKEDITOR_TEST_WORKSPACE_VERSION_PREF_KEY, EditorDefaults.integrationTestScriptVersion)
            .apply()
        serialized
    }

private fun persistBlockEditorSession(
    uiPrefs: android.content.SharedPreferences,
    session: BlockEditorShellEditorSession
) {
    val output = session.requestSave()
    if (
        output.disposition == ShellEditorOutputDisposition.DOCUMENT_SAVE &&
        output.formatId == BlockEditorDocumentFormats.WORKSPACE_JSON
    ) {
        uiPrefs.edit().putString(BLOCKEDITOR_WORKSPACE_PREF_KEY, output.content).apply()
        session.acknowledgeSave(output)
    }
}

private fun persistFlowchartViewSession(
    uiPrefs: android.content.SharedPreferences,
    session: FlowchartShellEditorSession
) {
    val output = session.requestSave()
    if (
        output.disposition == ShellEditorOutputDisposition.DRAFT_EXPORT &&
        output.formatId == FlowchartShellPlugin.FLOW_VIEW_JSON &&
        output.content.isNotBlank()
    ) {
        uiPrefs.edit().putString("flowchart_view_json", output.content).apply()
        session.acknowledgeSave(output)
    }
}

private class WorkspaceShellUiPluginHostAdapter : ShellPluginHostServices {
    var dirtyState: ShellDirtyState = ShellDirtyState.CLEAN
        private set
    var diagnostics: ShellValidationResult = ShellValidationResult(emptyList())
        private set
    var runtimeState: ShellPluginRuntimeState? = null
        private set
    var lastEmscriptDraft: ShellEditorOutput? = null
        private set

    override fun reportDirtyState(sessionId: ShellPluginSessionId, dirtyState: ShellDirtyState) {
        this.dirtyState = dirtyState
    }

    override fun requestSave(request: ShellSaveRequest) = Unit

    override fun publishOutput(output: ShellEditorOutput) {
        if (output.formatId == BlockEditorDocumentFormats.EMSCRIPT) {
            lastEmscriptDraft = output
        }
    }

    override fun reportDiagnostics(sessionId: ShellPluginSessionId, result: ShellValidationResult) {
        diagnostics = result
    }

    override fun reportRuntimeState(
        sessionId: ShellPluginSessionId,
        state: ShellPluginRuntimeState
    ) {
        runtimeState = state
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceSettingsBottomSheet(
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    hideSystemBars: Boolean,
    onHideSystemBarsChange: (Boolean) -> Unit,
    dockAtTop: Boolean,
    onDockAtTopChange: (Boolean) -> Unit,
    useLargeGrid: Boolean,
    onUseLargeGridChange: (Boolean) -> Unit,
    snapEnabled: Boolean,
    onSnapEnabledChange: (Boolean) -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    appearance: WorkspaceAppearance,
    onAppearanceChange: (WorkspaceAppearance) -> Unit,
    blockPaletteInsertMode: BlockPaletteInsertMode,
    onBlockPaletteInsertModeChange: (BlockPaletteInsertMode) -> Unit,
    onResetPanels: () -> Unit,
    onAutoArrange: () -> Unit,
    onColorPick: (Color) -> Unit,
    onToggleIconEngine: () -> Unit,
    onDismiss: () -> Unit
) {
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
            0 -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Systemleisten verstecken")
                    Switch(checked = hideSystemBars, onCheckedChange = onHideSystemBarsChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Minimiert-Leiste oben")
                    Switch(checked = dockAtTop, onCheckedChange = onDockAtTopChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Grid groß (8x8)")
                    Switch(checked = useLargeGrid, onCheckedChange = onUseLargeGridChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Snap aktiv")
                    Switch(checked = snapEnabled, onCheckedChange = onSnapEnabledChange)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("UI Skalierung: ${(uiScale * 100).toInt()}%")
                    Slider(
                        value = uiScale,
                        onValueChange = onUiScaleChange,
                        valueRange = 0.7f..1.5f
                    )
                }
                Text("Theme")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { onThemeModeChange("system") }, label = { Text("System") })
                    AssistChip(onClick = { onThemeModeChange("light") }, label = { Text("Hell") })
                    AssistChip(onClick = { onThemeModeChange("dark") }, label = { Text("Dunkel") })
                }
                Text("Aktuell: $themeMode")
                Button(onClick = onResetPanels, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Panels zurücksetzen")
                }
                Button(onClick = onAutoArrange) {
                    Text("Panels auto anordnen")
                }
                Text("Standard bleibt 4x4 (kleines Grid).")
            }

            6 -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Fokus-Panel: Workspace", color = M3EColors.Amber)
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

            1 -> WorkspaceSettingsInfoTab("Flowchart", listOf("Flowchart-Panels sind direkt im Workspace-FAB und in der linken Rail verfügbar."))
            2 -> BlockEditorSettingsTab(
                paletteInsertMode = blockPaletteInsertMode,
                onPaletteInsertModeChange = onBlockPaletteInsertModeChange
            )
            3 -> WorkspaceSettingsInfoTab("Texteditor", listOf("Die Workspace-Shell hält Texteditor-Funktionalität außerhalb der Shell-Plugin-Panels."))
            4 -> WorkspaceSettingsInfoTab("Browser", listOf("Browser-Panels sind in dieser Shell nicht als Platzhalter angeboten."))
            5 -> ExtrasPermissionsTab()
            else -> WorkspaceSettingsInfoTab(
                title = "Keypad",
                messages = listOf("Keypad-Mapping wird als eigenes Workspace-Panel/Plugin migriert. Icon-Engine kann hier bereits umgeschaltet werden."),
                actionLabel = "Icon-Engine: ${IconMotionConfig.engine.name}",
                onAction = onToggleIconEngine
            )
        }
    }
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
        items(M3EColors.allColors) { color ->
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(color, RoundedCornerShape(11.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp))
                    .clickable { onSelect(color) },
            )
        }
    }
}

@Composable
private fun WorkspaceColorPreviewRow(label: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color, RoundedCornerShape(14.dp))
        )
    }
}

@Composable
private fun BlockEditorSettingsTab(
    paletteInsertMode: BlockPaletteInsertMode,
    onPaletteInsertModeChange: (BlockPaletteInsertMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Palette/Flyout", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { onPaletteInsertModeChange(BlockPaletteInsertMode.TapToAdd) },
                label = { Text("Tap-to-add") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (paletteInsertMode == BlockPaletteInsertMode.TapToAdd) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )
            AssistChip(
                onClick = { onPaletteInsertModeChange(BlockPaletteInsertMode.DragFromPalette) },
                label = { Text("Drag-from-palette") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = if (paletteInsertMode == BlockPaletteInsertMode.DragFromPalette) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )
        }
        Text(
            text = "Aktuell: " + if (paletteInsertMode == BlockPaletteInsertMode.DragFromPalette) {
                "Drag-from-palette"
            } else {
                "Tap-to-add"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkspaceSettingsInfoTab(
    title: String,
    messages: List<String>,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        messages.forEach { message ->
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionLabel != null && onAction != null) {
            AssistChip(
                onClick = onAction,
                label = { Text(actionLabel) },
            )
        }
    }
}

private data class PermissionEntry(
    val label: String,
    val granted: Boolean,
    val open: () -> Unit,
)

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
                Text("Permissions und Capabilities", style = MaterialTheme.typography.titleMedium)
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

private fun isNotificationAccessGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

private fun isAccessibilityEnabledForApp(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabled.contains(context.packageName, ignoreCase = true)
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean =
    runCatching {
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

private fun isRootAvailable(): Boolean {
    val paths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su",
    )
    return paths.any { File(it).exists() }
}

private fun loadColorPref(prefs: SharedPreferences, key: String, fallback: Color): Color {
    val argbKey = "color.argb.${key.removePrefix("color.")}"
    if (prefs.contains(argbKey)) {
        return Color(prefs.getInt(argbKey, fallback.toArgb()))
    }
    if (!prefs.contains(key)) return fallback
    val legacyRaw = runCatching { prefs.getLong(key, fallback.toArgb().toLong()) }.getOrNull()
        ?: return fallback
    val legacyPackedColor = runCatching { Color(legacyRaw.toULong()) }.getOrNull()
    return legacyPackedColor ?: Color(legacyRaw.toInt())
}

private fun SharedPreferences.Editor.putColor(key: String, color: Color): SharedPreferences.Editor =
    putLong(key, color.value.toLong())
        .putInt("color.argb.${key.removePrefix("color.")}", color.toArgb())

@Composable
private fun statusColor(status: StepStatus): Color = when (status) {
    StepStatus.Recorded -> Color(0xFF6FCF97)
    StepStatus.Edited -> Color(0xFFF2C94C)
    StepStatus.Invalid -> Color(0xFFEB5757)
    StepStatus.Executed -> Color(0xFF56CCF2)
}

private fun snapValue(value: Float, step: Float): Float {
    if (step <= 0f) return value
    return (value / step).roundToInt() * step
}
