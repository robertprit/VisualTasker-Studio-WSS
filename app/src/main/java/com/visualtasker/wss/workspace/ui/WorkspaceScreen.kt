package com.visualtasker.wss.workspace.ui

import android.Manifest
import android.app.Activity
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CenterFocusStrong
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
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.visualtasker.wss.accessibility.RuntimePoint
import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
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
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntimeEnvironment
import com.visualtasker.wss.emscript.runtime.WorkspaceDryRunRuntime
import com.visualtasker.wss.emscript.runtime.traceSummary
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
import com.visualtasker.wss.visual.debug.VisualSemanticsReporter
import de.visualtasker.blockeditor.compose.host.BlockPaletteInsertMode
import de.visualtasker.blockeditor.compose.icons.CategoryIcons
import de.visualtasker.blockeditor.compose.theme.defaultBlockCategoryColor
import de.visualtasker.blockeditor.compose.theme.setBlockCategoryColorOverride
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.registry.BlockDefinition
import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.blockeditor.compose.ui.CategoryPalettePanel
import de.visualtasker.blockeditor.serialization.BlockEditorDocumentFormats
import de.visualtasker.blockeditor.serialization.WorkspaceDecodeResult
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.interaction.FlowInteractionAction
import de.visualtasker.flowchart.layout.FlowLayoutConfig
import de.visualtasker.flowchart.layout.FlowPinnedNodePolicy
import de.visualtasker.flowchart.serialization.FlowGraphJsonCodec
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
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
private const val BLOCKEDITOR_MINIMAP_VISIBLE_PREF_KEY = "blockeditor_minimap_visible"
private const val FLOWCHART_MINIMAP_VISIBLE_PREF_KEY = "flowchart_minimap_visible"
private const val PANEL_RAIL_EXPANDED_PREF_PREFIX = "workspace_panel_rail_expanded:"
private const val TEXT_EDITOR_DRAFT_PREF_KEY = "workspace_text_editor_draft"
private const val TEXT_EDITOR_TEST_SCRIPT_VERSION_PREF_KEY = "workspace_text_editor_test_script_version"

private data class ScreenshotCanvasAsset(
    val file: File,
    val label: String,
    val app: String = "VisualTasker",
    val scene: String = "Screenshot",
) {
    val id: String = file.absolutePath
    val dateLabel: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
}

private data class ScreenshotCanvasRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

private data class ScreenshotCanvasSavedMarker(
    val id: String,
    val label: String,
    val assetId: String?,
    val assetLabel: String?,
    val region: ScreenshotCanvasRegion,
    val markerMode: ScreenshotCanvasMarkerMode,
    val matchKind: ScreenshotCanvasMatchKind,
    val processingMode: ScreenshotCanvasProcessingMode,
    val threshold: Float,
    val rotationDegrees: Float,
    val matchReadMe: String,
    val colourHex: String,
    val updatedAt: Long,
)

private enum class ScreenshotCanvasMarkerMode { Template, Region, Point, Swipe, Path }

private enum class ScreenshotCanvasMatchKind { OCR, OCV }

private enum class ScreenshotCanvasProcessingMode { Original, Grayscale, HighContrast, Edge, Inverse }

private enum class ScreenshotCanvasOverlayHandle { ScanType, Close, ResizeBottomLeft, ResizeBottomRight, None }

private enum class ScreenshotCanvasTouchMode { Create, Move, ResizeBottomLeft, ResizeBottomRight, TapHandle, PointTap }

private data class ScreenshotCanvasGestureSetup(
    val mode: ScreenshotCanvasTouchMode,
    val createAnchorImage: Offset,
    val regionStart: ScreenshotCanvasRegion?,
    val handle: ScreenshotCanvasOverlayHandle,
)

private class ScreenshotViewState {
    var selectedAssetId by mutableStateOf<String?>(null)
    var assetRevision by mutableIntStateOf(0)
    var zoom by mutableFloatStateOf(1f)
    var selectedRegion by mutableStateOf<ScreenshotCanvasRegion?>(null)
    var showScreenshotBg by mutableStateOf(true)
    var filterHideClickable by mutableStateOf(false)
    var filterHideInvisible by mutableStateOf(false)
    var filterHideNonFocusable by mutableStateOf(false)
    var showAccessibilityNodes by mutableStateOf(true)
    var showDomNodes by mutableStateOf(false)
    var showOcrNodes by mutableStateOf(true)
    var showVisionTemplateNodes by mutableStateOf(true)
    var showYoloNodes by mutableStateOf(true)
    var showMarkers by mutableStateOf(true)
    var inspectorVisible by mutableStateOf(true)
}

private class MarkerConsoleState {
    var markerMode by mutableStateOf(ScreenshotCanvasMarkerMode.Region)
    var matchKind by mutableStateOf(ScreenshotCanvasMatchKind.OCR)
    var processingMode by mutableStateOf(ScreenshotCanvasProcessingMode.Original)
    var templateName by mutableStateOf("Marker")
    var threshold by mutableFloatStateOf(0.85f)
    var rotationDegrees by mutableFloatStateOf(0f)
    var matchReadMe by mutableStateOf("")
    var colourHex by mutableStateOf("#4FC3F7")
    var markerStatusMessage by mutableStateOf("")
    val savedMarkers = mutableStateListOf<ScreenshotCanvasSavedMarker>()
    var selectedSavedMarkerId by mutableStateOf<String?>(null)
}

private class VisionCropState {
    var visualTestScore by mutableStateOf<Float?>(null)
    var referenceMarkerId by mutableStateOf<String?>(null)
    var liveProcessingMode by mutableStateOf(ScreenshotCanvasProcessingMode.Original)
    var referenceProcessingMode by mutableStateOf(ScreenshotCanvasProcessingMode.Original)
}

private class ScreenshotCanvasUiState(
    val screenshot: ScreenshotViewState = ScreenshotViewState(),
    val marker: MarkerConsoleState = MarkerConsoleState(),
    val vision: VisionCropState = VisionCropState(),
) {
    var selectedAssetId: String?
        get() = screenshot.selectedAssetId
        set(value) {
            screenshot.selectedAssetId = value
        }
    var assetRevision: Int
        get() = screenshot.assetRevision
        set(value) {
            screenshot.assetRevision = value
        }
    var zoom: Float
        get() = screenshot.zoom
        set(value) {
            screenshot.zoom = value
        }
    var selectedRegion: ScreenshotCanvasRegion?
        get() = screenshot.selectedRegion
        set(value) {
            screenshot.selectedRegion = value
        }
    var markerMode: ScreenshotCanvasMarkerMode
        get() = marker.markerMode
        set(value) {
            marker.markerMode = value
        }
    var matchKind: ScreenshotCanvasMatchKind
        get() = marker.matchKind
        set(value) {
            marker.matchKind = value
        }
    var processingMode: ScreenshotCanvasProcessingMode
        get() = marker.processingMode
        set(value) {
            marker.processingMode = value
        }
    var templateName: String
        get() = marker.templateName
        set(value) {
            marker.templateName = value
        }
    var threshold: Float
        get() = marker.threshold
        set(value) {
            marker.threshold = value
        }
    var rotationDegrees: Float
        get() = marker.rotationDegrees
        set(value) {
            marker.rotationDegrees = value
        }
    var matchReadMe: String
        get() = marker.matchReadMe
        set(value) {
            marker.matchReadMe = value
        }
    var colourHex: String
        get() = marker.colourHex
        set(value) {
            marker.colourHex = value
        }
    var visualTestScore: Float?
        get() = vision.visualTestScore
        set(value) {
            vision.visualTestScore = value
        }
    var referenceMarkerId: String?
        get() = vision.referenceMarkerId
        set(value) {
            vision.referenceMarkerId = value
        }
    var liveProcessingMode: ScreenshotCanvasProcessingMode
        get() = vision.liveProcessingMode
        set(value) {
            vision.liveProcessingMode = value
        }
    var referenceProcessingMode: ScreenshotCanvasProcessingMode
        get() = vision.referenceProcessingMode
        set(value) {
            vision.referenceProcessingMode = value
        }
    var markerStatusMessage: String
        get() = marker.markerStatusMessage
        set(value) {
            marker.markerStatusMessage = value
        }
    val savedMarkers get() = marker.savedMarkers
    var selectedSavedMarkerId: String?
        get() = marker.selectedSavedMarkerId
        set(value) {
            marker.selectedSavedMarkerId = value
        }
    var showScreenshotBg: Boolean
        get() = screenshot.showScreenshotBg
        set(value) {
            screenshot.showScreenshotBg = value
        }
    var filterHideClickable: Boolean
        get() = screenshot.filterHideClickable
        set(value) {
            screenshot.filterHideClickable = value
        }
    var filterHideInvisible: Boolean
        get() = screenshot.filterHideInvisible
        set(value) {
            screenshot.filterHideInvisible = value
        }
    var filterHideNonFocusable: Boolean
        get() = screenshot.filterHideNonFocusable
        set(value) {
            screenshot.filterHideNonFocusable = value
        }
    var showAccessibilityNodes: Boolean
        get() = screenshot.showAccessibilityNodes
        set(value) {
            screenshot.showAccessibilityNodes = value
        }
    var showDomNodes: Boolean
        get() = screenshot.showDomNodes
        set(value) {
            screenshot.showDomNodes = value
        }
    var showOcrNodes: Boolean
        get() = screenshot.showOcrNodes
        set(value) {
            screenshot.showOcrNodes = value
        }
    var showVisionTemplateNodes: Boolean
        get() = screenshot.showVisionTemplateNodes
        set(value) {
            screenshot.showVisionTemplateNodes = value
        }
    var showYoloNodes: Boolean
        get() = screenshot.showYoloNodes
        set(value) {
            screenshot.showYoloNodes = value
        }
    var showMarkers: Boolean
        get() = screenshot.showMarkers
        set(value) {
            screenshot.showMarkers = value
        }
    var inspectorVisible: Boolean
        get() = screenshot.inspectorVisible
        set(value) {
            screenshot.inspectorVisible = value
        }

    fun zoomIn() {
        zoom = (zoom * 1.2f).coerceIn(1f, 5f)
    }

    fun zoomOut() {
        zoom = (zoom / 1.2f).coerceIn(1f, 5f)
    }

    fun center() {
        zoom = 1f
    }

    fun refreshAssets() {
        assetRevision += 1
    }
}

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
    val coroutineScope = rememberCoroutineScope()
    val uiPrefs = remember(context) { context.getSharedPreferences("panel_ui_options", Context.MODE_PRIVATE) }
    val sessionStore = remember(context) { WorkspaceSessionStore(context) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator.release() }
    }
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
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
    var blockEditorMiniMapVisible by remember {
        mutableStateOf(uiPrefs.getBoolean(BLOCKEDITOR_MINIMAP_VISIBLE_PREF_KEY, true))
    }
    var flowchartMiniMapVisible by remember {
        mutableStateOf(uiPrefs.getBoolean(FLOWCHART_MINIMAP_VISIBLE_PREF_KEY, true))
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
    var selectedFlowchartNodeForInsert by remember { mutableStateOf<FlowNodeId?>(null) }
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
    val studioLogStore = remember { StudioLogStore(maxEntries = 800) }
    val workspaceDryRunRuntime = remember { WorkspaceDryRunRuntime() }
    val workspaceBasicRuntime = remember(context, toneGenerator, vibrator) {
        WorkspaceBasicRuntime(
            capabilityGate = { workspaceRuntimeCapabilityGate() },
            environment = WorkspaceBasicRuntimeEnvironment(
                delayMs = { ms -> delay(ms.coerceAtLeast(0L)) },
                playBeep = { frequencyHz, durationMs, volumePercent ->
                    playRuntimeBeep(frequencyHz, durationMs, volumePercent)
                },
                vibrate = { patternMs ->
                    val sanitized = patternMs.map { it.coerceAtLeast(0L) }.filter { it > 0L }
                    if (sanitized.isNotEmpty()) {
                        val effect = if (sanitized.size == 1) {
                            VibrationEffect.createOneShot(sanitized.single(), VibrationEffect.DEFAULT_AMPLITUDE)
                        } else {
                            VibrationEffect.createWaveform(sanitized.toLongArray(), -1)
                        }
                        vibrator?.vibrate(effect)
                    }
                },
                log = { message ->
                    studioLogStore.append(
                        level = StudioLogLevel.INFO,
                        source = "RUNTIME",
                        message = message,
                        groupKey = "workspace:basic-runtime:log:$message",
                    )
                },
                clickText = { text ->
                    VisualTaskerAccessibilityService.current()?.clickText(text) ?: false
                },
                clickPoint = { x, y ->
                    VisualTaskerAccessibilityService.current()?.clickPoint(x, y) ?: false
                },
                swipe = { points, durationMs ->
                    VisualTaskerAccessibilityService.current()
                        ?.swipe(points.map { RuntimePoint(it.x, it.y) }, durationMs)
                        ?: false
                },
                clipboardGet = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                },
                clipboardSet = { text ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("VisualTasker", text))
                },
                cacheClear = {
                    clearRuntimeDirectory(context.cacheDir)
                },
                systemInfo = {
                    "package=${context.packageName}; sdk=${Build.VERSION.SDK_INT}; device=${Build.MANUFACTURER} ${Build.MODEL}"
                },
                envGet = { name ->
                    when (name.uppercase()) {
                        "PACKAGE_NAME" -> context.packageName
                        "ANDROID_VERSION" -> Build.VERSION.RELEASE.orEmpty()
                        "SDK_INT" -> Build.VERSION.SDK_INT.toString()
                        "DEVICE_MODEL" -> "${Build.MANUFACTURER} ${Build.MODEL}"
                        "FILES_DIR" -> runtimeFilesRoot(context).absolutePath
                        "CACHE_DIR" -> context.cacheDir.absolutePath
                        else -> ""
                    }
                },
                fileReadText = { path ->
                    runtimeFileFor(context, path)
                        ?.takeIf { it.isFile }
                        ?.readText()
                },
                fileWriteText = { path, text ->
                    runtimeFileFor(context, path)?.let { file ->
                        file.parentFile?.mkdirs()
                        runCatching {
                            file.writeText(text)
                            true
                        }.getOrDefault(false)
                    } ?: false
                },
                screenshot = { path ->
                    val target = runtimeFileFor(context, path.ifBlank { "screenshots/latest.png" })
                    if (target != null) {
                        VisualTaskerAccessibilityService.current()?.takeScreenshotTo(target) ?: false
                    } else {
                        false
                    }
                },
            ),
        )
    }
    val workspaceSyncGuard = remember { WorkspaceSyncGuard() }
    var workspaceDryRunSequence by remember { mutableStateOf(0L) }
    var workspaceDryRunResult by remember { mutableStateOf<EmscriptDryRunResult?>(null) }
    var workspaceDryRunStepIndex by remember { mutableIntStateOf(0) }
    val activeBlockEditorSessionState = remember { mutableStateOf<BlockEditorShellEditorSession?>(null) }
    val activeFlowchartSessionState = remember { mutableStateOf<FlowchartShellEditorSession?>(null) }
    var selectedFlowchartNodeId by remember { mutableStateOf<FlowNodeId?>(null) }
    var selectedFlowchartEdgeId by remember { mutableStateOf<FlowEdgeId?>(null) }
    val emscriptFileManager = remember {
        EmscriptFileManagerUiState().apply {
            scripts["draft"] = initialTextEditorDraft
            EditorDefaults.allSamples.forEach { (name, script) ->
                scripts.putIfAbsent(name, script)
            }
        }
    }
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
            FlowchartWorkspaceMutation.AddNode(
                definitionId = definitionId,
                afterNodeId = selectedFlowchartNodeForInsert,
            ),
            selectedFlowchartNodeForInsert?.let { "${it.value}:insert:$definitionId" } ?: definitionId,
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
    val replaceFlowchartNodeType: (FlowNodeId, String) -> Unit = { nodeId, definitionId ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.ReplaceNodeType(
                nodeId = nodeId,
                definitionId = definitionId,
            ),
            "${nodeId.value}:$definitionId:replace-type",
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
                val summary = result.traceSummary()
                val preview = result.events.takeLast(8).joinToString(separator = "\n") {
                    "#${it.index} ${it.kind.uppercase()}: ${it.message}"
                }
                studioLogStore.append(
                    level = if (summary.hasWarnings || summary.hasErrors) StudioLogLevel.WARNING else StudioLogLevel.INFO,
                    source = source,
                    message = if (summary.hasWarnings || summary.hasErrors) {
                        "Workspace Dry-Run mit Hinweisen abgeschlossen"
                    } else {
                        "Workspace Dry-Run erfolgreich"
                    },
                    details = "${summary.message}\n$preview",
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "workspace:dry-run:success:${snapshot.sequence}"
                )
            }
            is EmscriptDryRunResult.Failure -> {
                val summary = result.traceSummary()
                studioLogStore.append(
                    level = StudioLogLevel.ERROR,
                    source = source,
                    message = "Workspace Dry-Run fehlgeschlagen",
                    details = summary.message,
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "workspace:dry-run:failure:${snapshot.sequence}"
                )
            }
        }
    }
    fun runCurrentWorkspaceLive(source: String) {
        if (workflowState.emscriptProjection.isFailure) {
            val message = workflowState.emscriptProjection.exceptionOrNull()?.message ?: "EMScript-Projektion nicht verfügbar."
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = source,
                message = "Basic-Run abgebrochen",
                details = message,
                documentRevision = workflowState.revision.toLong(),
                groupKey = "workspace:basic-run:projection-missing"
            )
            return
        }
        coroutineScope.launch {
            studioLogStore.append(
                level = StudioLogLevel.INFO,
                source = source,
                message = "Workspace Basic-Run gestartet",
                details = workspaceRuntimeCapabilityGate().inspect(workflowState.document).summary,
                documentRevision = workflowState.revision.toLong(),
                groupKey = "workspace:basic-run:start:${workflowState.revision}"
            )
            val result = workspaceBasicRuntime.run(workflowState.document)
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
                    groupKey = "workspace:basic-run:diag:${diagnostic.code}:${diagnostic.nodeId?.value}:${diagnostic.message}"
                )
            }
            when (result) {
                is EmscriptDryRunResult.Success -> {
                    val summary = result.traceSummary()
                    val preview = result.events.takeLast(8).joinToString(separator = "\n") {
                        "#${it.index} ${it.kind.uppercase()}: ${it.message}"
                    }
                    studioLogStore.append(
                        level = if (summary.hasWarnings || summary.hasErrors) StudioLogLevel.WARNING else StudioLogLevel.INFO,
                        source = source,
                        message = if (summary.hasWarnings || summary.hasErrors) {
                            "Workspace Basic-Run mit Hinweisen abgeschlossen"
                        } else {
                            "Workspace Basic-Run erfolgreich"
                        },
                        details = "${summary.message}\n$preview",
                        documentRevision = workflowState.revision.toLong(),
                        groupKey = "workspace:basic-run:success:${snapshot.sequence}"
                    )
                }
                is EmscriptDryRunResult.Failure -> {
                    val summary = result.traceSummary()
                    studioLogStore.append(
                        level = StudioLogLevel.ERROR,
                        source = source,
                        message = "Workspace Basic-Run fehlgeschlagen",
                        details = summary.message,
                        documentRevision = workflowState.revision.toLong(),
                        groupKey = "workspace:basic-run:failure:${snapshot.sequence}"
                    )
                }
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
    val workspaceCanvasState = remember(context) {
        ScreenshotCanvasUiState().apply {
            savedMarkers.addAll(loadScreenshotCanvasSavedMarkers(context))
            selectedSavedMarkerId = savedMarkers.firstOrNull()?.id
        }
    }
    LaunchedEffect(workspaceCanvasState) {
        snapshotFlow { workspaceCanvasState.savedMarkers.toList() }
            .debounce(250)
            .collect { markers ->
                persistScreenshotCanvasSavedMarkers(context, markers)
            }
    }
    val workspaceCanvasAssets = remember(workspaceCanvasState.assetRevision) {
        loadScreenshotCanvasAssets(context)
    }

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
        val title = displayNameForPanelType(type)
        panels.add(
            PanelState(
                id = id,
                type = type,
                title = title,
                x = if (type == PanelType.Screenshot || type == PanelType.Marker || type == PanelType.Vision || type == PanelType.Datastore) 32f else 96f,
                y = max(96f, workspaceTopGuardPx),
                width = if (type == PanelType.Screenshot || type == PanelType.Marker || type == PanelType.Vision || type == PanelType.Datastore) {
                    ((surfaceSize.width / density) - 64f).coerceAtLeast(PANEL_DEFAULT_W)
                } else {
                    PANEL_DEFAULT_W
                },
                height = if (type == PanelType.Screenshot || type == PanelType.Marker || type == PanelType.Vision || type == PanelType.Datastore) {
                    ((surfaceSize.height / density) - (workspaceTopGuardPx / density) - 48f).coerceAtLeast(PANEL_DEFAULT_H)
                } else {
                    PANEL_DEFAULT_H
                },
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
    LaunchedEffect(
        hideSystemBars,
        dockAtTop,
        useLargeGrid,
        snapEnabled,
        uiScale,
        blockPaletteInsertMode,
        blockEditorMiniMapVisible,
        flowchartMiniMapVisible,
    ) {
        uiPrefs.edit()
            .putBoolean("hide_system_bars", hideSystemBars)
            .putBoolean("dock_top", dockAtTop)
            .putBoolean("grid_large", useLargeGrid)
            .putBoolean("snap_enabled", snapEnabled)
            .putFloat("ui_scale", uiScale)
            .putString(BLOCKEDITOR_PALETTE_INSERT_MODE_PREF_KEY, blockPaletteInsertMode.name)
            .putBoolean(BLOCKEDITOR_MINIMAP_VISIBLE_PREF_KEY, blockEditorMiniMapVisible)
            .putBoolean(FLOWCHART_MINIMAP_VISIBLE_PREF_KEY, flowchartMiniMapVisible)
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
            WorkspaceScreenshotCanvasBackground(
                state = workspaceCanvasState,
                assets = workspaceCanvasAssets,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = WORKSPACE_TOP_BAR_HEIGHT_DP.dp)
            )

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
                val isScreenshotPanel = panel.type == PanelType.Screenshot || panel.type == PanelType.Marker || panel.type == PanelType.Vision || panel.type == PanelType.Datastore
                val isLogConsolePanel = panel.type == PanelType.LogConsole || panel.type == PanelType.RuntimeLog
                val isEmscriptPanel = panel.type == PanelType.TextEditor || panel.type == PanelType.Emscript || panel.type == PanelType.DebugInfo
                val blockEditorSessionState = remember(panel.id) { mutableStateOf<BlockEditorShellEditorSession?>(null) }
                val flowchartSessionState = remember(panel.id) { mutableStateOf<FlowchartShellEditorSession?>(null) }
                val panelScreenshotCanvasState = remember(panel.id) { ScreenshotCanvasUiState() }
                val screenshotCanvasState = if (isScreenshotPanel) workspaceCanvasState else panelScreenshotCanvasState
                val panelScreenshotAssets = remember(panel.id, panelScreenshotCanvasState.assetRevision) {
                    loadScreenshotCanvasAssets(context)
                }
                val screenshotAssets = if (isScreenshotPanel) workspaceCanvasAssets else panelScreenshotAssets
                var railExpanded by remember(panel.id) {
                    mutableStateOf(loadPanelRailExpanded(uiPrefs, panel.id))
                }
                DarkPanel(
                    panel = panel.toMainPanelState(),
                    snapEnabled = snapEnabled,
                    gridSizeDp = gridSizeDp,
                    isActiveTarget = panel.id == focusedPanelId,
                    maxWidth = maxWidthDp,
                    maxHeight = maxHeightDp,
                    minPositionYPx = workspaceTopGuardPx,
                    maxPositionXPx = max(0f, surfaceSize.width - panel.width * density - 16f),
                    maxPositionYPx = max(workspaceTopGuardPx, surfaceSize.height - panel.height * density - 16f),
                    showRail = true,
                    railExpandedOverride = railExpanded,
                    onRailExpandedChange = { expanded ->
                        railExpanded = expanded
                        persistPanelRailExpanded(uiPrefs, panel.id, expanded)
                    },
                    showDefaultRailIcons = !(isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel || isScreenshotPanel),
                    showRailColorPicker = !(isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel || isScreenshotPanel),
                    railExpandedWidth = when {
                        isBlockEditorPanel -> 300.dp
                        isFlowchartPanel -> 236.dp
                        isLogConsolePanel -> 220.dp
                        isEmscriptPanel -> 240.dp
                        isScreenshotPanel -> 220.dp
                        else -> 186.dp
                    },
                    railExpandedFillHeight = isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel || isScreenshotPanel,
                    compactRailContent = { onExpandRequested ->
                        when {
                            isScreenshotPanel -> ScreenshotCanvasCompactRail(
                                state = screenshotCanvasState,
                                onExpandRequested = onExpandRequested,
                            )
                            isBlockEditorPanel -> BlockEditorCompactCategoryRail(
                                session = blockEditorSessionState.value,
                                onExpandRequested = onExpandRequested,
                                onSave = {
                                    blockEditorSessionState.value?.let { persistBlockEditorSession(uiPrefs, it) }
                                }
                            )
                            isFlowchartPanel -> FlowchartCompactActionRail(
                                session = flowchartSessionState.value,
                                selectedNodeId = selectedFlowchartNodeId,
                                selectedEdgeId = selectedFlowchartEdgeId,
                                onExpandRequested = onExpandRequested,
                                onSave = {
                                    flowchartSessionState.value?.let { persistFlowchartViewSession(uiPrefs, it) }
                                },
                                onRunDry = { runCurrentWorkspaceDryRun("FLOWCHART") },
                                onRunLive = { runCurrentWorkspaceLive("FLOWCHART") },
                                onStepBack = { renderWorkspaceDryRunStep(workspaceDryRunStepIndex - 1) },
                                onStepForward = {
                                    if (workspaceDryRunResult == null) {
                                        runCurrentWorkspaceDryRun("FLOWCHART")
                                    } else {
                                        renderWorkspaceDryRunStep(workspaceDryRunStepIndex + 1)
                                    }
                                },
                                canStepBack = workspaceDryRunResult != null && workspaceDryRunStepIndex > 0,
                                canStepForward = workspaceDryRunStepIndex < dryRunEventCount(workspaceDryRunResult),
                                onDeleteNode = deleteFlowchartNode,
                                onDisconnectEdge = disconnectFlowchartEdge,
                                onUndoWorkspace = undoWorkspaceChange,
                                onRedoWorkspace = redoWorkspaceChange,
                            )
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
                            isScreenshotPanel -> ScreenshotCanvasRail(
                                state = screenshotCanvasState,
                                assets = screenshotAssets,
                                selectedAssetId = screenshotCanvasState.selectedAssetId,
                                onSelect = { screenshotCanvasState.selectedAssetId = it },
                                onRefresh = { screenshotCanvasState.refreshAssets() },
                                onCapture = {
                                    val target = File(runtimeFilesRoot(context), "screenshots/capture-${System.currentTimeMillis()}.png")
                                    target.parentFile?.mkdirs()
                                    coroutineScope.launch {
                                        val ok = VisualTaskerAccessibilityService.current()?.takeScreenshotTo(target) ?: false
                                        studioLogStore.append(
                                            level = if (ok) StudioLogLevel.INFO else StudioLogLevel.ERROR,
                                            source = "SCREENSHOT",
                                            message = if (ok) "Screenshot gespeichert" else "Screenshot fehlgeschlagen",
                                            details = target.absolutePath,
                                            groupKey = "screenshot:capture"
                                        )
                                        screenshotCanvasState.refreshAssets()
                                    }
                                }
                            )
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
                        blockEditorMiniMapVisible = blockEditorMiniMapVisible,
                        flowchartMiniMapVisible = flowchartMiniMapVisible,
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
                        onRunWorkspaceLive = { runCurrentWorkspaceLive("FLOWCHART") },
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
                        onFlowchartNodeSelected = { nodeId ->
                            selectedFlowchartNodeForInsert = nodeId
                            selectedFlowchartNodeId = nodeId
                            selectedFlowchartEdgeId = null
                            focusBlockFromFlowNode(nodeId)
                        },
                        onFlowchartSelectionChanged = { nodeId, edgeId ->
                            selectedFlowchartNodeId = nodeId
                            selectedFlowchartEdgeId = edgeId
                            selectedFlowchartNodeForInsert = nodeId
                        },
                        onBlockEditorBlockSelected = ::focusFlowNodeFromBlock,
                        onFlowchartNodeDelete = deleteFlowchartNode,
                        onFlowchartNodesDelete = deleteFlowchartNodes,
                        onFlowchartNodesConnect = connectFlowchartNodes,
                        onFlowchartPortsConnect = connectFlowchartPorts,
                        flowchartConnectionOptionsFor = flowchartConnectionOptionsFor,
                        onFlowchartEdgeDisconnect = disconnectFlowchartEdge,
                        onFlowchartNodeFieldUpdate = updateFlowchartNodeField,
                        onFlowchartNodeTypeReplace = replaceFlowchartNodeType,
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
                        screenshotCanvasState = screenshotCanvasState,
                        screenshotAssets = screenshotAssets,
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
                .align(if (dockAtTop) Alignment.TopEnd else Alignment.BottomEnd)
                .padding(
                    start = 12.dp,
                    end = 84.dp,
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
                    icon = Icons.Default.Photo,
                    label = "Canvas öffnen",
                    color = M3EColors.Mint
                ) {
                    openPanel(PanelType.Screenshot)
                },
                FabAction(
                    icon = Icons.Default.CenterFocusStrong,
                    label = "Vision öffnen",
                    color = M3EColors.Oceanneon
                ) {
                    openPanel(PanelType.Vision)
                },
                FabAction(
                    icon = Icons.Default.FolderOpen,
                    label = "Datastore öffnen",
                    color = M3EColors.Violet
                ) {
                    openPanel(PanelType.Datastore)
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
            blockEditorMiniMapVisible = blockEditorMiniMapVisible,
            onBlockEditorMiniMapVisibleChange = { blockEditorMiniMapVisible = it },
            flowchartMiniMapVisible = flowchartMiniMapVisible,
            onFlowchartMiniMapVisibleChange = { flowchartMiniMapVisible = it },
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
                        text = displayTitleForPanel(panel),
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
    blockEditorMiniMapVisible: Boolean,
    flowchartMiniMapVisible: Boolean,
    onEmscriptSessionChange: (EmscriptEditorSession) -> Unit,
    logStore: StudioLogStore,
    logConsoleState: LogConsoleUiState,
    onBlockEditorSessionReady: (BlockEditorShellEditorSession?) -> Unit = {},
    onFlowchartSessionReady: (FlowchartShellEditorSession?) -> Unit = {},
    onRunWorkspaceDry: () -> Unit = {},
    onRunWorkspaceLive: () -> Unit = {},
    onDryRunStepBack: () -> Unit = {},
    onDryRunStepForward: () -> Unit = {},
    canDryRunStepBack: Boolean = false,
    canDryRunStepForward: Boolean = false,
    dryRunStepLabel: String? = null,
    onFlowchartNodeSelected: (FlowNodeId) -> Unit = {},
    onFlowchartSelectionChanged: (FlowNodeId?, FlowEdgeId?) -> Unit = { _, _ -> },
    onBlockEditorBlockSelected: (BlockId?) -> Unit = {},
    onFlowchartNodeDelete: (FlowNodeId) -> Unit = {},
    onFlowchartNodesDelete: (Set<FlowNodeId>) -> Unit = {},
    onFlowchartNodesConnect: (FlowNodeId, FlowNodeId, FlowEdgeKind, String?) -> Unit = { _, _, _, _ -> },
    onFlowchartPortsConnect: (FlowNodeId, String, FlowNodeId, String, FlowEdgeKind) -> Unit = { _, _, _, _, _ -> },
    flowchartConnectionOptionsFor: (FlowNodeId, FlowNodeId) -> List<com.visualtasker.wss.workspace.model.FlowchartConnectionOption> = { _, _ -> emptyList() },
    onFlowchartEdgeDisconnect: (FlowEdgeId) -> Unit = {},
    onFlowchartNodeFieldUpdate: (FlowNodeId, String, String) -> Unit = { _, _, _ -> },
    onFlowchartNodeTypeReplace: (FlowNodeId, String) -> Unit = { _, _ -> },
    onFlowchartIfBranchAdd: (FlowNodeId) -> Unit = {},
    onFlowchartIfBranchRemove: (FlowNodeId) -> Unit = {},
    onFlowchartViewChanged: (FlowViewDocument) -> Unit = {},
    onWorkspaceUndo: () -> Boolean = { false },
    onWorkspaceRedo: () -> Boolean = { false },
    onFlowRuntimeSnapshotChange: (FlowRuntimeSnapshot) -> Unit = {},
    screenshotCanvasState: ScreenshotCanvasUiState = remember { ScreenshotCanvasUiState() },
    screenshotAssets: List<ScreenshotCanvasAsset> = emptyList(),
    onWorkspaceJsonChange: (String, String) -> Unit
) {
    when (panel.type) {
        PanelType.RecorderSteps -> RecorderStepsPanel(steps = steps, actionSink = actionSink)
        PanelType.BlockEditor -> BlockEditorPanel(
            panelId = panel.id,
            uiPrefs = uiPrefs,
            workflowState = workflowState,
            paletteInsertMode = paletteInsertMode,
            showMiniMap = blockEditorMiniMapVisible,
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
            onRunLive = onRunWorkspaceLive,
            onStepBack = onDryRunStepBack,
            onStepForward = onDryRunStepForward,
            canStepBack = canDryRunStepBack,
            canStepForward = canDryRunStepForward,
            stepLabel = dryRunStepLabel,
            showMiniMap = flowchartMiniMapVisible,
            onNodeSelected = onFlowchartNodeSelected,
            onSelectionChanged = onFlowchartSelectionChanged,
            onNodeDelete = onFlowchartNodeDelete,
            onNodesDelete = onFlowchartNodesDelete,
            onNodesConnect = onFlowchartNodesConnect,
            onPortsConnect = onFlowchartPortsConnect,
            connectionOptionsFor = flowchartConnectionOptionsFor,
            onEdgeDisconnect = onFlowchartEdgeDisconnect,
            onNodeFieldUpdate = onFlowchartNodeFieldUpdate,
            onNodeTypeReplace = onFlowchartNodeTypeReplace,
            onIfBranchAdd = onFlowchartIfBranchAdd,
            onIfBranchRemove = onFlowchartIfBranchRemove,
            onViewChanged = onFlowchartViewChanged,
            onWorkspaceUndo = onWorkspaceUndo,
            onWorkspaceRedo = onWorkspaceRedo,
            onSessionReady = onFlowchartSessionReady
        )
        PanelType.TextEditor,
        PanelType.Emscript -> {
            val capabilityReport = workspaceRuntimeCapabilityGate().inspect(workflowState.document)
            EmscriptTextEditorPanel(
                session = emscriptSession,
                uiState = emscriptEditorUiState,
                latestEmscriptProjected = latestEmscriptProjected,
                onSessionChange = onEmscriptSessionChange,
                logStore = logStore,
                workspaceJson = workflowState.serializedJson,
                currentFlowGraph = workflowState.flowchartProjection.graph,
                onWorkspaceJsonChange = { updated -> onWorkspaceJsonChange(updated, WORKFLOW_SOURCE_EMSCRIPT_APPLY) },
                onDryRunRuntimeSnapshot = onFlowRuntimeSnapshotChange,
                onLiveRun = { onRunWorkspaceLive() },
                canLiveRun = capabilityReport.realRunAllowed,
                liveRunStatus = capabilityReport.summary,
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
        }
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
                val capabilityReport = workspaceRuntimeCapabilityGate().inspect(workflowState.document)
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
                addAll(
                    VisualSemanticsReporter.summarizeFlowchart(
                        graph = workflowState.flowchartProjection.graph,
                        runtimeSnapshot = flowRuntimeSnapshot,
                    )
                )
                flowRuntimeSnapshot?.diagnostics?.take(8)?.forEach { diagnostic ->
                    add("${diagnostic.severity.name} ${diagnostic.code}: ${diagnostic.message}")
                }
                latestEmscriptGenerationFailure?.let(::add)
            }
        )
        PanelType.Screenshot -> ScreenshotCanvasPanel(
            state = screenshotCanvasState,
            assets = screenshotAssets,
            markerPanel = false,
        )
        PanelType.Marker -> MarkerCanvasPanel(
            state = screenshotCanvasState,
            assets = screenshotAssets,
        )
        PanelType.Vision -> VisionCropCanvasPanel(
            state = screenshotCanvasState,
            assets = screenshotAssets,
        )
        PanelType.Datastore -> DatastorePanel(
            state = screenshotCanvasState,
            assets = screenshotAssets,
            workflowState = workflowState,
            logStore = logStore,
        )
        PanelType.M3Director -> Unit
    }
}

@Composable
private fun ColumnScope.ScreenshotCanvasCompactRail(
    state: ScreenshotCanvasUiState,
    onExpandRequested: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenshotMarkerModeRailButton("Template", Icons.Default.CenterFocusStrong, state.markerMode == ScreenshotCanvasMarkerMode.Template) {
            state.markerMode = ScreenshotCanvasMarkerMode.Template
        }
        ScreenshotMarkerModeRailButton("Region", Icons.Default.GridView, state.markerMode == ScreenshotCanvasMarkerMode.Region) {
            state.markerMode = ScreenshotCanvasMarkerMode.Region
        }
        ScreenshotMarkerModeRailButton("Point", Icons.Default.TouchApp, state.markerMode == ScreenshotCanvasMarkerMode.Point) {
            state.markerMode = ScreenshotCanvasMarkerMode.Point
        }
        ScreenshotMarkerModeRailButton("Swipe", Icons.Default.ArrowForward, state.markerMode == ScreenshotCanvasMarkerMode.Swipe) {
            state.markerMode = ScreenshotCanvasMarkerMode.Swipe
        }
        ScreenshotMarkerModeRailButton("Path", Icons.Default.Polyline, state.markerMode == ScreenshotCanvasMarkerMode.Path) {
            state.markerMode = ScreenshotCanvasMarkerMode.Path
        }
        Spacer(modifier = Modifier.weight(1f))
        TooltipIconButton(tooltip = "Screenshots und Marker", onClick = onExpandRequested, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Photo, contentDescription = "Screenshots und Marker", modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun ColumnScope.ScreenshotCanvasRail(
    state: ScreenshotCanvasUiState,
    assets: List<ScreenshotCanvasAsset>,
    selectedAssetId: String?,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    onCapture: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Screenshots",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
        )
        TooltipIconButton(tooltip = "Aktualisieren", onClick = onRefresh, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Aktualisieren", modifier = Modifier.size(17.dp))
        }
        TooltipIconButton(tooltip = "Aufnehmen", onClick = onCapture, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Photo, contentDescription = "Aufnehmen", modifier = Modifier.size(17.dp))
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (assets.isEmpty()) {
            item {
                Text(
                    text = "Noch keine gespeicherten Screenshots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(assets, key = { it.id }) { asset ->
            val selected = asset.id == selectedAssetId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(asset.id) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.84f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.56f)
                },
                tonalElevation = if (selected) 2.dp else 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(asset.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    Text(
                        asset.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Marker",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (state.savedMarkers.isEmpty()) {
            item {
                Text(
                    text = "Noch keine gespeicherten Marker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.savedMarkers, key = { it.id }) { marker ->
            ScreenshotRailSavedMarkerItem(
                marker = marker,
                selected = marker.id == state.selectedSavedMarkerId,
                onSelect = {
                    state.selectedSavedMarkerId = marker.id
                    state.selectedAssetId = marker.assetId ?: state.selectedAssetId
                    state.selectedRegion = marker.region
                    state.markerMode = marker.markerMode
                    state.matchKind = marker.matchKind
                    state.processingMode = marker.processingMode
                    state.threshold = marker.threshold
                    state.rotationDegrees = marker.rotationDegrees
                    state.matchReadMe = marker.matchReadMe
                    state.colourHex = marker.colourHex
                    state.templateName = marker.label
                    state.markerStatusMessage = "Marker geladen."
                },
            )
        }
    }
}

@Composable
private fun ScreenshotMarkerModeRailButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    TooltipIconButton(tooltip = label, onClick = onClick, modifier = Modifier.size(34.dp)) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ScreenshotRailSavedMarkerItem(
    marker: ScreenshotCanvasSavedMarker,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    var expanded by remember(marker.id) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect()
                expanded = !expanded
            },
        shape = RoundedCornerShape(9.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.84f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.58f)
        },
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = iconForMarkerMode(marker.markerMode),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = marker.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${marker.markerMode.name} / ${marker.assetLabel ?: "Screenshot"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.Minimize else Icons.Default.AddCircle,
                    contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                val export = markerExportCode(marker)
                Text(
                    text = "Measure ${marker.region.x},${marker.region.y} ${marker.region.width}x${marker.region.height}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Mode ${marker.processingMode.name}  ${marker.matchKind.name}  ${"%.0f".format(marker.threshold * 100)}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(7.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                ) {
                    Text(
                        text = export,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(7.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(export)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Code Export", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun iconForMarkerMode(mode: ScreenshotCanvasMarkerMode): androidx.compose.ui.graphics.vector.ImageVector =
    when (mode) {
        ScreenshotCanvasMarkerMode.Template -> Icons.Default.CenterFocusStrong
        ScreenshotCanvasMarkerMode.Region -> Icons.Default.GridView
        ScreenshotCanvasMarkerMode.Point -> Icons.Default.TouchApp
        ScreenshotCanvasMarkerMode.Swipe -> Icons.Default.ArrowForward
        ScreenshotCanvasMarkerMode.Path -> Icons.Default.Polyline
    }

private fun markerExportCode(marker: ScreenshotCanvasSavedMarker): String {
    val region = marker.region
    return when (marker.markerMode) {
        ScreenshotCanvasMarkerMode.Template ->
            "template(\"${marker.label}\", bbox(${region.x}, ${region.y}, ${region.width}, ${region.height}))"
        ScreenshotCanvasMarkerMode.Region ->
            "region(\"${marker.label}\", ${region.x}, ${region.y}, ${region.width}, ${region.height})"
        ScreenshotCanvasMarkerMode.Point ->
            "point(\"${marker.label}\", ${region.x + region.width / 2}, ${region.y + region.height / 2})"
        ScreenshotCanvasMarkerMode.Swipe ->
            "swipe(${region.x}, ${region.y}, ${region.x + region.width}, ${region.y + region.height})"
        ScreenshotCanvasMarkerMode.Path ->
            "path(${region.x}, ${region.y}, ${region.x + region.width}, ${region.y + region.height})"
    }
}

@Composable
private fun DatastorePanel(
    state: ScreenshotCanvasUiState,
    assets: List<ScreenshotCanvasAsset>,
    workflowState: WorkspaceWorkflowState,
    logStore: StudioLogStore,
) {
    val logChangeToken = logStore.changeToken
    val logEntries = remember(logChangeToken) { logStore.allEntries() }
    val selectedMarker = remember(state.selectedSavedMarkerId, state.savedMarkers.toList()) {
        state.savedMarkers.firstOrNull { it.id == state.selectedSavedMarkerId }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Datastore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DatastoreSection("Dataset") {
                    DatastoreMetric("Screenshots", assets.size.toString())
                    DatastoreMetric("Marker", state.savedMarkers.size.toString())
                    DatastoreMetric("Auswahl", selectedMarker?.label ?: "-")
                    DatastoreMetric("Region", state.selectedRegion?.let { "${it.x},${it.y} ${it.width}x${it.height}" } ?: "-")
                }
            }
            item {
                DatastoreSection("Ressourcen") {
                    DatastoreMetric("Workflow ID", workflowState.document.id)
                    DatastoreMetric("Blöcke", workflowState.document.blocks.size.toString())
                    DatastoreMetric("Roots", workflowState.document.rootBlocks.size.toString())
                    DatastoreMetric("Revision", workflowState.revision.toString())
                }
            }
            item {
                DatastoreSection("Flowgraph") {
                    DatastoreMetric("Nodes", workflowState.flowchartProjection.graph.nodes.size.toString())
                    DatastoreMetric("Edges", workflowState.flowchartProjection.graph.edges.size.toString())
                    DatastoreMetric("Quelle", workflowState.mutationSource)
                }
            }
            item {
                DatastoreSection("Logs") {
                    DatastoreMetric("Einträge", logEntries.size.toString())
                    DatastoreMetric("Quellen", logStore.availableSources().joinToString(", ").ifBlank { "-" })
                    DatastoreMetric("Status", if (logStore.isPaused) "Pausiert" else "Aktiv")
                }
            }
            if (state.savedMarkers.isNotEmpty()) {
                item {
                    DatastoreSection("Marker") {
                        state.savedMarkers.take(8).forEach { marker ->
                            DatastoreMetric(
                                marker.label,
                                "${marker.markerMode.name} ${marker.region.x},${marker.region.y} ${marker.region.width}x${marker.region.height}",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DatastoreSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.62f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun DatastoreMetric(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(98.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MarkerCanvasPanel(
    state: ScreenshotCanvasUiState,
    assets: List<ScreenshotCanvasAsset>,
) {
    val selectedAsset = remember(assets, state.selectedAssetId) {
        assets.firstOrNull { it.id == state.selectedAssetId } ?: assets.firstOrNull()
    }
    LaunchedEffect(selectedAsset?.id) {
        if (selectedAsset != null && state.selectedAssetId != selectedAsset.id) {
            state.selectedAssetId = selectedAsset.id
        }
    }
    val bitmap = remember(selectedAsset?.id, selectedAsset?.file?.lastModified()) {
        selectedAsset?.file?.absolutePath?.let { path ->
            runCatching { decodeScreenshotBitmap(path) }.getOrNull()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenshotCanvasInspector(
            modifier = Modifier.fillMaxWidth(),
            asset = selectedAsset,
            imageSize = bitmap?.let { "${it.width} x ${it.height}px" } ?: "-",
            state = state,
            markerPanel = true,
        )
        MarkerUnderScreenshotPanel(
            state = state,
            asset = selectedAsset,
            imageSize = bitmap?.let { it.width to it.height },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun VisionCropCanvasPanel(
    state: ScreenshotCanvasUiState,
    assets: List<ScreenshotCanvasAsset>,
) {
    val selectedAsset = remember(assets, state.selectedAssetId) {
        assets.firstOrNull { it.id == state.selectedAssetId } ?: assets.firstOrNull()
    }
    val liveBitmap = remember(selectedAsset?.id, selectedAsset?.file?.lastModified()) {
        selectedAsset?.file?.absolutePath?.let { path ->
            runCatching { decodeScreenshotBitmap(path) }.getOrNull()
        }
    }
    val compatibleMarkers = remember(state.savedMarkers.toList(), selectedAsset?.id) {
        state.savedMarkers.filter { marker ->
            marker.assetId == null || selectedAsset?.id == null || marker.assetId == selectedAsset.id
        }
    }
    LaunchedEffect(compatibleMarkers.map { it.id }) {
        if (state.referenceMarkerId == null || compatibleMarkers.none { it.id == state.referenceMarkerId }) {
            state.referenceMarkerId = compatibleMarkers.firstOrNull()?.id
        }
    }
    val referenceMarker = compatibleMarkers.firstOrNull { it.id == state.referenceMarkerId }
    val referenceAsset = remember(assets, referenceMarker?.assetId) {
        assets.firstOrNull { it.id == referenceMarker?.assetId } ?: selectedAsset
    }
    val referenceBitmap = remember(referenceAsset?.id, referenceAsset?.file?.lastModified()) {
        referenceAsset?.file?.absolutePath?.let { path ->
            runCatching { decodeScreenshotBitmap(path) }.getOrNull()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050509), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Vision", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    text = state.visualTestScore?.let { "Match ${"%.1f".format(it * 100)}%" }
                        ?: state.selectedRegion?.let { "${it.width}x${it.height}" }
                        ?: "Kein Crop",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VisionProcessingSelector(
                label = "Live",
                selected = state.liveProcessingMode,
                onSelected = { state.liveProcessingMode = it },
                modifier = Modifier.weight(1f),
            )
            VisionProcessingSelector(
                label = "Referenz",
                selected = state.referenceProcessingMode,
                onSelected = { state.referenceProcessingMode = it },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    state.visualTestScore = compareScreenshotRegions(
                        liveBitmap = liveBitmap,
                        liveRegion = state.selectedRegion,
                        liveMode = state.liveProcessingMode,
                        referenceBitmap = referenceBitmap,
                        referenceRegion = referenceMarker?.region,
                        referenceMode = state.referenceProcessingMode,
                    )
                    state.markerStatusMessage = state.visualTestScore?.let { "Vision Match ${"%.1f".format(it * 100)}%" }
                        ?: "Vision Match nicht möglich."
                },
                enabled = liveBitmap != null && state.selectedRegion != null && referenceBitmap != null && referenceMarker != null,
                modifier = Modifier.height(34.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                Text("Test", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (compatibleMarkers.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(compatibleMarkers, key = { it.id }) { marker ->
                    FilterChip(
                        selected = marker.id == state.referenceMarkerId,
                        onClick = { state.referenceMarkerId = marker.id },
                        label = {
                            Text(
                                marker.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.height(30.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VisionCropPreview(
                title = "Live Crop",
                bitmap = liveBitmap,
                region = state.selectedRegion,
                mode = state.liveProcessingMode,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            VisionCropPreview(
                title = "Referenz",
                bitmap = referenceBitmap,
                region = referenceMarker?.region,
                mode = state.referenceProcessingMode,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun VisionProcessingSelector(
    label: String,
    selected: ScreenshotCanvasProcessingMode,
    onSelected: (ScreenshotCanvasProcessingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ScreenshotCanvasProcessingMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                label = {
                    Text(
                        when (mode) {
                            ScreenshotCanvasProcessingMode.Original -> "Original"
                            ScreenshotCanvasProcessingMode.Grayscale -> "Grau"
                            ScreenshotCanvasProcessingMode.HighContrast -> "Kontrast"
                            ScreenshotCanvasProcessingMode.Edge -> "Kanten"
                            ScreenshotCanvasProcessingMode.Inverse -> "Invers"
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                modifier = Modifier.height(28.dp),
            )
        }
    }
}

@Composable
private fun VisionCropPreview(
    title: String,
    bitmap: android.graphics.Bitmap?,
    region: ScreenshotCanvasRegion?,
    mode: ScreenshotCanvasProcessingMode,
    modifier: Modifier = Modifier,
) {
    val processedCrop = remember(bitmap, region, mode) {
        createProcessedCropBitmap(bitmap, region, mode)?.asImageBitmap()
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.40f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (processedCrop != null) {
                VisionImageCanvas(
                    bitmap = processedCrop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun VisionImageCanvas(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(Color.Black, size = size)
        val scale = min(size.width / bitmap.width, size.height / bitmap.height)
        val dstWidth = bitmap.width * scale
        val dstHeight = bitmap.height * scale
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(((size.width - dstWidth) / 2f).roundToInt(), ((size.height - dstHeight) / 2f).roundToInt()),
            dstSize = IntSize(dstWidth.roundToInt().coerceAtLeast(1), dstHeight.roundToInt().coerceAtLeast(1)),
        )
        drawRect(
            color = Color(0xFF4FC3F7),
            topLeft = Offset(((size.width - dstWidth) / 2f), ((size.height - dstHeight) / 2f)),
            size = Size(dstWidth, dstHeight),
            style = Stroke(2.dp.toPx()),
        )
    }
}

@Composable
private fun WorkspaceScreenshotCanvasBackground(
    state: ScreenshotCanvasUiState,
    assets: List<ScreenshotCanvasAsset>,
    modifier: Modifier = Modifier,
) {
    val selectedAsset = remember(assets, state.selectedAssetId) {
        assets.firstOrNull { it.id == state.selectedAssetId } ?: assets.firstOrNull()
    }
    LaunchedEffect(selectedAsset?.id) {
        if (selectedAsset != null && state.selectedAssetId != selectedAsset.id) {
            state.selectedAssetId = selectedAsset.id
        }
    }
    val bitmap = remember(selectedAsset?.id, selectedAsset?.file?.lastModified()) {
        selectedAsset?.file?.absolutePath?.let { path ->
            runCatching { decodeScreenshotBitmap(path)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(
        modifier = modifier
            .background(Color(0xFF050509))
            .clipToBounds()
    ) {
        if (bitmap != null) {
            ScreenshotRegionCanvas(
                bitmap = bitmap,
                asset = selectedAsset,
                state = state,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ScreenshotCanvasGrid(Modifier.matchParentSize())
        }
    }
}

@Composable
private fun ScreenshotCanvasPanel(
    state: ScreenshotCanvasUiState,
    assets: List<ScreenshotCanvasAsset>,
    markerPanel: Boolean,
) {
    val selectedAsset = remember(assets, state.selectedAssetId) {
        assets.firstOrNull { it.id == state.selectedAssetId } ?: assets.firstOrNull()
    }
    LaunchedEffect(selectedAsset?.id) {
        if (selectedAsset != null && state.selectedAssetId != selectedAsset.id) {
            state.selectedAssetId = selectedAsset.id
        }
    }
    val bitmap = remember(selectedAsset?.id, selectedAsset?.file?.lastModified()) {
        selectedAsset?.file?.absolutePath?.let { path ->
            runCatching { decodeScreenshotBitmap(path)?.asImageBitmap() }.getOrNull()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
    ) {
        ScreenshotCanvasActionBar(
            modifier = Modifier.fillMaxWidth(),
            state = state,
            markerPanel = markerPanel,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScreenshotCanvasInspector(
                modifier = Modifier.fillMaxWidth(),
                asset = selectedAsset,
                imageSize = bitmap?.let { "${it.width} x ${it.height}px" } ?: "-",
                state = state,
                markerPanel = markerPanel,
            )
            Text(
                text = markerMetricsTitle(state.selectedRegion, state.markerMode, bitmap?.let { it.width to it.height }),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (markerPanel) {
            MarkerUnderScreenshotPanel(
                state = state,
                asset = selectedAsset,
                imageSize = bitmap?.let { it.width to it.height },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp),
            )
            }
        }
    }
}

private data class ScreenshotFittedImageLayout(
    val offsetX: Float,
    val offsetY: Float,
    val drawnWidth: Float,
    val drawnHeight: Float,
) {
    val imageBounds: Rect
        get() = Rect(offsetX, offsetY, offsetX + drawnWidth, offsetY + drawnHeight)

    fun screenToImage(point: Offset, imageWidth: Int, imageHeight: Int): Offset? {
        if (!imageBounds.contains(point)) return null
        return Offset(
            x = ((point.x - offsetX) / drawnWidth * imageWidth).coerceIn(0f, imageWidth.toFloat()),
            y = ((point.y - offsetY) / drawnHeight * imageHeight).coerceIn(0f, imageHeight.toFloat()),
        )
    }

    fun screenToImageClamped(point: Offset, imageWidth: Int, imageHeight: Int): Offset =
        Offset(
            x = ((point.x.coerceIn(imageBounds.left, imageBounds.right) - offsetX) / drawnWidth * imageWidth)
                .coerceIn(0f, imageWidth.toFloat()),
            y = ((point.y.coerceIn(imageBounds.top, imageBounds.bottom) - offsetY) / drawnHeight * imageHeight)
                .coerceIn(0f, imageHeight.toFloat()),
        )

    fun regionToScreenRect(region: ScreenshotCanvasRegion, imageWidth: Int, imageHeight: Int): Rect =
        Rect(
            left = offsetX + region.x.toFloat() / imageWidth * drawnWidth,
            top = offsetY + region.y.toFloat() / imageHeight * drawnHeight,
            right = offsetX + (region.x + region.width).toFloat() / imageWidth * drawnWidth,
            bottom = offsetY + (region.y + region.height).toFloat() / imageHeight * drawnHeight,
        )
}

@Composable
private fun ScreenshotRegionCanvas(
    bitmap: ImageBitmap,
    asset: ScreenshotCanvasAsset?,
    state: ScreenshotCanvasUiState,
    modifier: Modifier = Modifier,
) {
    var dragPreviewRegion by remember(asset?.id) { mutableStateOf<ScreenshotCanvasRegion?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val imageWidth = bitmap.width
    val imageHeight = bitmap.height
    val displayRegion = dragPreviewRegion ?: state.selectedRegion
    val layout = remember(canvasSize, imageWidth, imageHeight, state.zoom) {
        fittedScreenshotLayout(
            canvasWidth = canvasSize.width.toFloat(),
            canvasHeight = canvasSize.height.toFloat(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            zoom = state.zoom,
            selectedRegion = null,
        )
    }
    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(asset?.id, canvasSize, imageWidth, imageHeight) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    down.consume()
                    val startScreen = down.position
                    val startImage = layout.screenToImage(startScreen, imageWidth, imageHeight) ?: return@awaitEachGesture
                    val regionAtStart = state.selectedRegion
                    val screenRect = regionAtStart?.let { layout.regionToScreenRect(it, imageWidth, imageHeight) }
                    val handleRadiusPx = 26.dp.toPx()
                    val pointHitRadiusPx = 32.dp.toPx()
                    val pointMode = state.markerMode == ScreenshotCanvasMarkerMode.Point
                    val setup = if (screenRect != null) {
                        val handle = if (pointMode) {
                            ScreenshotCanvasOverlayHandle.None
                        } else {
                            detectScreenshotOverlayHandle(startScreen, screenRect, handleRadiusPx)
                        }
                        val mode = when (handle) {
                            ScreenshotCanvasOverlayHandle.ScanType,
                            ScreenshotCanvasOverlayHandle.Close -> ScreenshotCanvasTouchMode.TapHandle
                            ScreenshotCanvasOverlayHandle.ResizeBottomLeft -> ScreenshotCanvasTouchMode.ResizeBottomLeft
                            ScreenshotCanvasOverlayHandle.ResizeBottomRight -> ScreenshotCanvasTouchMode.ResizeBottomRight
                            ScreenshotCanvasOverlayHandle.None -> when {
                                pointMode && (startScreen - screenRect.center).getDistance() <= pointHitRadiusPx -> ScreenshotCanvasTouchMode.Move
                                pointMode -> ScreenshotCanvasTouchMode.PointTap
                                screenRect.contains(startScreen) -> ScreenshotCanvasTouchMode.Move
                                else -> ScreenshotCanvasTouchMode.Create
                            }
                        }
                        ScreenshotCanvasGestureSetup(mode, startImage, regionAtStart, handle)
                    } else {
                        ScreenshotCanvasGestureSetup(
                            mode = if (pointMode) ScreenshotCanvasTouchMode.PointTap else ScreenshotCanvasTouchMode.Create,
                            createAnchorImage = startImage,
                            regionStart = null,
                            handle = ScreenshotCanvasOverlayHandle.None,
                        )
                    }

                    if (setup.mode == ScreenshotCanvasTouchMode.TapHandle) {
                        if (waitForUpOrCancellation() != null) {
                            when (setup.handle) {
                                ScreenshotCanvasOverlayHandle.ScanType -> {
                                    state.matchKind = if (state.matchKind == ScreenshotCanvasMatchKind.OCR) {
                                        ScreenshotCanvasMatchKind.OCV
                                    } else {
                                        ScreenshotCanvasMatchKind.OCR
                                    }
                                }
                                ScreenshotCanvasOverlayHandle.Close -> state.selectedRegion = null
                                else -> Unit
                            }
                        }
                        return@awaitEachGesture
                    }

                    if (setup.mode == ScreenshotCanvasTouchMode.PointTap) {
                        if (waitForUpOrCancellation() != null) {
                            state.selectedRegion = pointRegion(startImage, imageWidth, imageHeight)
                        }
                        return@awaitEachGesture
                    }

                    var lastDragRegion: ScreenshotCanvasRegion? = null
                    drag(down.id) { change ->
                        change.consume()
                        val currentImage = layout.screenToImageClamped(change.position, imageWidth, imageHeight)
                        val newRegion = when (setup.mode) {
                            ScreenshotCanvasTouchMode.Create -> regionFromPoints(setup.createAnchorImage, currentImage, imageWidth, imageHeight)
                            ScreenshotCanvasTouchMode.Move -> {
                                if (state.markerMode == ScreenshotCanvasMarkerMode.Point) {
                                    pointRegion(currentImage, imageWidth, imageHeight)
                                } else {
                                    val base = setup.regionStart ?: return@drag
                                    val dx = (currentImage.x - setup.createAnchorImage.x).roundToInt()
                                    val dy = (currentImage.y - setup.createAnchorImage.y).roundToInt()
                                    moveScreenshotRegion(base, dx, dy, imageWidth, imageHeight)
                                }
                            }
                            ScreenshotCanvasTouchMode.ResizeBottomLeft -> resizeScreenshotRegionFromBottomLeft(
                                setup.regionStart ?: return@drag,
                                currentImage,
                                imageWidth,
                                imageHeight,
                            )
                            ScreenshotCanvasTouchMode.ResizeBottomRight -> resizeScreenshotRegionFromBottomRight(
                                setup.regionStart ?: return@drag,
                                currentImage,
                                imageWidth,
                                imageHeight,
                            )
                            ScreenshotCanvasTouchMode.TapHandle,
                            ScreenshotCanvasTouchMode.PointTap -> return@drag
                        }
                        lastDragRegion = newRegion
                        dragPreviewRegion = newRegion
                    }
                    dragPreviewRegion = null
                    lastDragRegion?.let { state.selectedRegion = it }
                }
            }
    ) {
        drawRect(Color.Black, size = size)
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(layout.offsetX.roundToInt(), layout.offsetY.roundToInt()),
            dstSize = IntSize(layout.drawnWidth.roundToInt().coerceAtLeast(1), layout.drawnHeight.roundToInt().coerceAtLeast(1)),
            alpha = if (state.showScreenshotBg) 1f else 0.05f,
        )
        drawScreenshotRulers(layout = layout, imageWidth = imageWidth, imageHeight = imageHeight)
        drawPanelEdgeRulers(imageWidth = imageWidth, imageHeight = imageHeight)
        drawCanvasLayerHints(layout = layout, state = state)
        drawScreenshotRegionOverlay(
            layout = layout,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            region = displayRegion,
            markerMode = state.markerMode,
            matchKind = state.matchKind,
        )
    }
}

private fun fittedScreenshotLayout(
    canvasWidth: Float,
    canvasHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
    zoom: Float,
    selectedRegion: ScreenshotCanvasRegion?,
): ScreenshotFittedImageLayout {
    val usableWidth = (canvasWidth - 36f).coerceAtLeast(1f)
    val usableHeight = (canvasHeight - 36f).coerceAtLeast(1f)
    val safeZoom = zoom.coerceIn(1f, 5f)
    val scale = min(usableWidth / imageWidth, usableHeight / imageHeight) * safeZoom
    val drawnWidth = imageWidth * scale
    val drawnHeight = imageHeight * scale
    val baseOffsetX = 28f + (usableWidth - drawnWidth) / 2f
    val baseOffsetY = 8f + (usableHeight - drawnHeight) / 2f
    if (selectedRegion == null || safeZoom <= 1f) {
        return ScreenshotFittedImageLayout(baseOffsetX, baseOffsetY, drawnWidth, drawnHeight)
    }
    val regionCx = selectedRegion.x + selectedRegion.width / 2f
    val regionCy = selectedRegion.y + selectedRegion.height / 2f
    val screenCx = baseOffsetX + regionCx / imageWidth * drawnWidth
    val screenCy = baseOffsetY + regionCy / imageHeight * drawnHeight
    val maxPanX = max(0f, drawnWidth - usableWidth) / 2f
    val maxPanY = max(0f, drawnHeight - usableHeight) / 2f
    val panX = (canvasWidth / 2f - screenCx).coerceIn(-maxPanX, maxPanX)
    val panY = (canvasHeight / 2f - screenCy).coerceIn(-maxPanY, maxPanY)
    return ScreenshotFittedImageLayout(baseOffsetX + panX, baseOffsetY + panY, drawnWidth, drawnHeight)
}

private fun regionFromPoints(
    start: Offset,
    end: Offset,
    imageWidth: Int,
    imageHeight: Int,
): ScreenshotCanvasRegion {
    val left = min(start.x, end.x).roundToInt().coerceIn(0, imageWidth - 1)
    val top = min(start.y, end.y).roundToInt().coerceIn(0, imageHeight - 1)
    val right = max(start.x, end.x).roundToInt().coerceIn(left + 1, imageWidth)
    val bottom = max(start.y, end.y).roundToInt().coerceIn(top + 1, imageHeight)
    return ScreenshotCanvasRegion(left, top, right - left, bottom - top)
}

private fun pointRegion(point: Offset, imageWidth: Int, imageHeight: Int): ScreenshotCanvasRegion {
    val size = 48
    val half = size / 2
    val x = (point.x.roundToInt() - half).coerceIn(0, max(0, imageWidth - size))
    val y = (point.y.roundToInt() - half).coerceIn(0, max(0, imageHeight - size))
    return ScreenshotCanvasRegion(x, y, min(size, imageWidth - x), min(size, imageHeight - y))
}

private fun detectScreenshotOverlayHandle(
    point: Offset,
    rect: Rect,
    radius: Float,
): ScreenshotCanvasOverlayHandle = when {
    (point - Offset(rect.left, rect.top)).getDistance() <= radius -> ScreenshotCanvasOverlayHandle.ScanType
    (point - Offset(rect.right, rect.top)).getDistance() <= radius -> ScreenshotCanvasOverlayHandle.Close
    (point - Offset(rect.left, rect.bottom)).getDistance() <= radius -> ScreenshotCanvasOverlayHandle.ResizeBottomLeft
    (point - Offset(rect.right, rect.bottom)).getDistance() <= radius -> ScreenshotCanvasOverlayHandle.ResizeBottomRight
    else -> ScreenshotCanvasOverlayHandle.None
}

private fun moveScreenshotRegion(
    base: ScreenshotCanvasRegion,
    dx: Int,
    dy: Int,
    imageWidth: Int,
    imageHeight: Int,
): ScreenshotCanvasRegion {
    val width = base.width.coerceIn(1, imageWidth)
    val height = base.height.coerceIn(1, imageHeight)
    val x = (base.x + dx).coerceIn(0, (imageWidth - width).coerceAtLeast(0))
    val y = (base.y + dy).coerceIn(0, (imageHeight - height).coerceAtLeast(0))
    return ScreenshotCanvasRegion(x, y, width, height)
}

private fun resizeScreenshotRegionFromBottomLeft(
    base: ScreenshotCanvasRegion,
    current: Offset,
    imageWidth: Int,
    imageHeight: Int,
): ScreenshotCanvasRegion {
    val right = (base.x + base.width).coerceIn(1, imageWidth)
    val top = base.y.coerceIn(0, imageHeight - 1)
    val left = current.x.roundToInt().coerceIn(0, right - 1)
    val bottom = current.y.roundToInt().coerceIn(top + 1, imageHeight)
    return ScreenshotCanvasRegion(left, top, right - left, bottom - top)
}

private fun resizeScreenshotRegionFromBottomRight(
    base: ScreenshotCanvasRegion,
    current: Offset,
    imageWidth: Int,
    imageHeight: Int,
): ScreenshotCanvasRegion {
    val left = base.x.coerceIn(0, imageWidth - 1)
    val top = base.y.coerceIn(0, imageHeight - 1)
    val right = current.x.roundToInt().coerceIn(left + 1, imageWidth)
    val bottom = current.y.roundToInt().coerceIn(top + 1, imageHeight)
    return ScreenshotCanvasRegion(left, top, right - left, bottom - top)
}

private fun DrawScope.drawCanvasLayerHints(
    layout: ScreenshotFittedImageLayout,
    state: ScreenshotCanvasUiState,
) {
    if (state.showAccessibilityNodes) {
        drawRect(Color(0xFF4FC3F7).copy(alpha = 0.26f), topLeft = Offset(layout.imageBounds.left + 36f, layout.imageBounds.top + 42f), size = Size(180f, 72f), style = Stroke(2f))
    }
    if (state.showDomNodes) {
        drawRect(Color(0xFFCE93D8).copy(alpha = 0.28f), topLeft = Offset(layout.imageBounds.right - 210f, layout.imageBounds.top + 96f), size = Size(160f, 56f), style = Stroke(2f))
    }
    if (state.showOcrNodes) {
        drawLine(Color(0xFFFFF176).copy(alpha = 0.72f), Offset(layout.imageBounds.left + 18f, layout.imageBounds.center.y), Offset(layout.imageBounds.right - 18f, layout.imageBounds.center.y), 1.8f)
    }
    if (state.showVisionTemplateNodes) {
        drawRect(Color(0xFF81C784).copy(alpha = 0.24f), topLeft = Offset(layout.imageBounds.left + 72f, layout.imageBounds.bottom - 132f), size = Size(210f, 72f), style = Stroke(2f))
    }
    if (state.showYoloNodes) {
        drawCircle(Color(0xFFFF7043).copy(alpha = 0.34f), radius = 24f, center = Offset(layout.imageBounds.right - 88f, layout.imageBounds.top + 86f), style = Stroke(3f))
    }
}

private fun DrawScope.drawScreenshotRegionOverlay(
    layout: ScreenshotFittedImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    region: ScreenshotCanvasRegion?,
    markerMode: ScreenshotCanvasMarkerMode,
    matchKind: ScreenshotCanvasMatchKind,
) {
    if (region == null) {
        drawCanvasHint(layout.imageBounds, if (markerMode == ScreenshotCanvasMarkerMode.Point) "Tippen zum Setzen des Punkts" else "Bereich ziehen zum Markieren")
        return
    }
    val rect = layout.regionToScreenRect(region, imageWidth, imageHeight)
    if (markerMode == ScreenshotCanvasMarkerMode.Point) {
        val arm = 28f
        drawLine(Color(0xFFFFEB3B), Offset(rect.center.x - arm, rect.center.y), Offset(rect.center.x + arm, rect.center.y), 2.5f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFEB3B), Offset(rect.center.x, rect.center.y - arm), Offset(rect.center.x, rect.center.y + arm), 2.5f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFFF5722), radius = 11f, center = rect.center, style = Stroke(2.5f))
        return
    }
    val dim = Color.Black.copy(alpha = 0.30f)
    drawRect(dim, topLeft = Offset(layout.imageBounds.left, layout.imageBounds.top), size = Size(layout.imageBounds.width, (rect.top - layout.imageBounds.top).coerceAtLeast(0f)))
    drawRect(dim, topLeft = Offset(layout.imageBounds.left, rect.bottom), size = Size(layout.imageBounds.width, (layout.imageBounds.bottom - rect.bottom).coerceAtLeast(0f)))
    drawRect(dim, topLeft = Offset(layout.imageBounds.left, rect.top), size = Size((rect.left - layout.imageBounds.left).coerceAtLeast(0f), rect.height))
    drawRect(dim, topLeft = Offset(rect.right, rect.top), size = Size((layout.imageBounds.right - rect.right).coerceAtLeast(0f), rect.height))
    drawRect(Color(0xFF4FC3F7), topLeft = Offset(rect.left, rect.top), size = Size(rect.width, rect.height), style = Stroke(2.5f))
    if (markerMode == ScreenshotCanvasMarkerMode.Swipe || markerMode == ScreenshotCanvasMarkerMode.Path) {
        val color = if (markerMode == ScreenshotCanvasMarkerMode.Swipe) Color(0xFFFFD54F) else Color(0xFF69F0AE)
        drawLine(color, Offset(rect.left, rect.top), Offset(rect.right, rect.bottom), 3.5f, cap = StrokeCap.Round)
        drawCircle(color, radius = 6f, center = Offset(rect.left, rect.top))
        drawCircle(color, radius = 8f, center = Offset(rect.right, rect.bottom), style = Stroke(2.5f))
    }
    drawCanvasHandle(rect.left, rect.top, if (matchKind == ScreenshotCanvasMatchKind.OCR) "OCR" else "OCV", Color(0xFF1976D2))
    drawCanvasHandle(rect.right, rect.top, "X", Color(0xFFD32F2F))
    drawCanvasHandle(rect.left, rect.bottom, "R", Color(0xFF2E7D32))
    drawCanvasHandle(rect.right, rect.bottom, "R", Color(0xFF2E7D32))
}

private fun DrawScope.drawCanvasHandle(cx: Float, cy: Float, label: String, color: Color) {
    drawCircle(color, radius = 9f, center = Offset(cx, cy))
    drawCircle(Color.White, radius = 9f, center = Offset(cx, cy), style = Stroke(1.5f))
    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 10.dp.toPx()
    }
    drawContext.canvas.nativeCanvas.drawText(label, cx, cy - ((paint.descent() + paint.ascent()) / 2f), paint)
}

private fun DrawScope.drawCanvasHint(bounds: Rect, text: String) {
    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(150, 255, 255, 255)
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 13.dp.toPx()
    }
    drawContext.canvas.nativeCanvas.drawText(text, bounds.center.x, bounds.bottom - 18.dp.toPx(), paint)
}

private fun DrawScope.drawScreenshotRulers(
    layout: ScreenshotFittedImageLayout,
    imageWidth: Int,
    imageHeight: Int,
) {
    val bounds = layout.imageBounds
    val rulerColor = Color(0xFFC9D3FF).copy(alpha = 0.68f)
    val majorColor = Color(0xFFFFFFFF).copy(alpha = 0.84f)
    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(210, 255, 255, 255)
        textSize = 9.dp.toPx()
    }
    fun xFor(value: Int): Float = bounds.left + value.toFloat() / imageWidth * bounds.width
    fun yFor(value: Int): Float = bounds.top + value.toFloat() / imageHeight * bounds.height
    val xStep = chooseRulerStep(imageWidth)
    val yStep = chooseRulerStep(imageHeight)
    var x = 0
    while (x <= imageWidth) {
        val sx = xFor(x)
        val major = x % (xStep * 5) == 0
        drawLine(if (major) majorColor else rulerColor, Offset(sx, bounds.top), Offset(sx, bounds.top + if (major) 14f else 8f), 1f)
        drawLine(if (major) majorColor else rulerColor, Offset(sx, bounds.bottom), Offset(sx, bounds.bottom - if (major) 14f else 8f), 1f)
        if (major) drawContext.canvas.nativeCanvas.drawText(x.toString(), sx + 2f, bounds.top + 26f, paint)
        x += xStep
    }
    var y = 0
    while (y <= imageHeight) {
        val sy = yFor(y)
        val major = y % (yStep * 5) == 0
        drawLine(if (major) majorColor else rulerColor, Offset(bounds.left, sy), Offset(bounds.left + if (major) 14f else 8f, sy), 1f)
        drawLine(if (major) majorColor else rulerColor, Offset(bounds.right, sy), Offset(bounds.right - if (major) 14f else 8f, sy), 1f)
        if (major) drawContext.canvas.nativeCanvas.drawText(y.toString(), bounds.left + 18f, sy - 2f, paint)
        y += yStep
    }
}

private fun DrawScope.drawPanelEdgeRulers(
    imageWidth: Int,
    imageHeight: Int,
) {
    val rulerColor = Color(0xFF8EA0D4).copy(alpha = 0.48f)
    val majorColor = Color.White.copy(alpha = 0.72f)
    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(185, 255, 255, 255)
        textSize = 8.dp.toPx()
    }
    val left = 0f
    val top = 0f
    val right = size.width
    val bottom = size.height
    val xStep = chooseRulerStep(imageWidth)
    val yStep = chooseRulerStep(imageHeight)
    var x = 0
    while (x <= imageWidth) {
        val sx = x.toFloat() / imageWidth.coerceAtLeast(1) * right
        val major = x % (xStep * 5) == 0
        drawLine(if (major) majorColor else rulerColor, Offset(sx, top), Offset(sx, top + if (major) 18f else 10f), 1f)
        drawLine(if (major) majorColor else rulerColor, Offset(sx, bottom), Offset(sx, bottom - if (major) 18f else 10f), 1f)
        if (major && sx < right - 28f) {
            drawContext.canvas.nativeCanvas.drawText(x.toString(), sx + 3f, top + 29f, paint)
        }
        x += xStep
    }
    var y = 0
    while (y <= imageHeight) {
        val sy = y.toFloat() / imageHeight.coerceAtLeast(1) * bottom
        val major = y % (yStep * 5) == 0
        drawLine(if (major) majorColor else rulerColor, Offset(left, sy), Offset(left + if (major) 18f else 10f, sy), 1f)
        drawLine(if (major) majorColor else rulerColor, Offset(right, sy), Offset(right - if (major) 18f else 10f, sy), 1f)
        if (major && sy > 12f) {
            drawContext.canvas.nativeCanvas.drawText(y.toString(), left + 20f, sy - 3f, paint)
        }
        y += yStep
    }
}

private fun chooseRulerStep(size: Int): Int = when {
    size > 2400 -> 200
    size > 1200 -> 100
    size > 600 -> 50
    else -> 25
}

private fun decodeScreenshotBitmap(path: String): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 4096 || bounds.outHeight / sampleSize > 4096) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
    )
}

private fun createProcessedCropBitmap(
    bitmap: android.graphics.Bitmap?,
    region: ScreenshotCanvasRegion?,
    mode: ScreenshotCanvasProcessingMode,
): android.graphics.Bitmap? {
    if (bitmap == null || region == null) return null
    val safe = safeBitmapRegion(bitmap, region) ?: return null
    val output = android.graphics.Bitmap.createBitmap(safe.width, safe.height, android.graphics.Bitmap.Config.ARGB_8888)
    for (y in 0 until safe.height) {
        for (x in 0 until safe.width) {
            val sourceX = safe.x + x
            val sourceY = safe.y + y
            val pixel = bitmap.getPixel(sourceX, sourceY)
            val value = processedPixelValue(bitmap, sourceX, sourceY, mode)
            val color = when (mode) {
                ScreenshotCanvasProcessingMode.Original -> pixel
                ScreenshotCanvasProcessingMode.Grayscale,
                ScreenshotCanvasProcessingMode.Edge,
                ScreenshotCanvasProcessingMode.Inverse -> android.graphics.Color.rgb(value, value, value)
                ScreenshotCanvasProcessingMode.HighContrast -> {
                    if (value >= 128) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                }
            }
            output.setPixel(x, y, color)
        }
    }
    return output
}

private fun compareScreenshotRegions(
    liveBitmap: android.graphics.Bitmap?,
    liveRegion: ScreenshotCanvasRegion?,
    liveMode: ScreenshotCanvasProcessingMode,
    referenceBitmap: android.graphics.Bitmap?,
    referenceRegion: ScreenshotCanvasRegion?,
    referenceMode: ScreenshotCanvasProcessingMode,
): Float? {
    val liveSafe = liveBitmap?.let { safeBitmapRegion(it, liveRegion) } ?: return null
    val referenceSafe = referenceBitmap?.let { safeBitmapRegion(it, referenceRegion) } ?: return null
    val samplesX = min(32, min(liveSafe.width, referenceSafe.width)).coerceAtLeast(1)
    val samplesY = min(32, min(liveSafe.height, referenceSafe.height)).coerceAtLeast(1)
    var totalDelta = 0L
    var count = 0
    for (sampleY in 0 until samplesY) {
        for (sampleX in 0 until samplesX) {
            val liveX = liveSafe.x + ((sampleX + 0.5f) * liveSafe.width / samplesX).toInt().coerceIn(0, liveSafe.width - 1)
            val liveY = liveSafe.y + ((sampleY + 0.5f) * liveSafe.height / samplesY).toInt().coerceIn(0, liveSafe.height - 1)
            val refX = referenceSafe.x + ((sampleX + 0.5f) * referenceSafe.width / samplesX).toInt().coerceIn(0, referenceSafe.width - 1)
            val refY = referenceSafe.y + ((sampleY + 0.5f) * referenceSafe.height / samplesY).toInt().coerceIn(0, referenceSafe.height - 1)
            totalDelta += abs(
                processedPixelValue(liveBitmap, liveX, liveY, liveMode) -
                    processedPixelValue(referenceBitmap, refX, refY, referenceMode)
            )
            count++
        }
    }
    if (count == 0) return null
    val averageDelta = totalDelta.toFloat() / count.toFloat()
    return (1f - averageDelta / 255f).coerceIn(0f, 1f)
}

private fun safeBitmapRegion(
    bitmap: android.graphics.Bitmap,
    region: ScreenshotCanvasRegion?,
): ScreenshotCanvasRegion? {
    if (region == null || bitmap.width <= 0 || bitmap.height <= 0) return null
    val x = region.x.coerceIn(0, bitmap.width - 1)
    val y = region.y.coerceIn(0, bitmap.height - 1)
    val width = region.width.coerceIn(1, bitmap.width - x)
    val height = region.height.coerceIn(1, bitmap.height - y)
    return ScreenshotCanvasRegion(x, y, width, height)
}

private fun processedPixelValue(
    bitmap: android.graphics.Bitmap,
    x: Int,
    y: Int,
    mode: ScreenshotCanvasProcessingMode,
): Int {
    val gray = grayscaleValue(bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1)))
    return when (mode) {
        ScreenshotCanvasProcessingMode.Original,
        ScreenshotCanvasProcessingMode.Grayscale -> gray
        ScreenshotCanvasProcessingMode.HighContrast -> if (gray >= 128) 255 else 0
        ScreenshotCanvasProcessingMode.Inverse -> 255 - gray
        ScreenshotCanvasProcessingMode.Edge -> {
            val right = grayscaleValue(bitmap.getPixel((x + 1).coerceAtMost(bitmap.width - 1), y))
            val bottom = grayscaleValue(bitmap.getPixel(x, (y + 1).coerceAtMost(bitmap.height - 1)))
            (abs(gray - right) + abs(gray - bottom)).coerceIn(0, 255)
        }
    }
}

private fun grayscaleValue(pixel: Int): Int {
    val red = android.graphics.Color.red(pixel)
    val green = android.graphics.Color.green(pixel)
    val blue = android.graphics.Color.blue(pixel)
    return ((red * 30 + green * 59 + blue * 11) / 100).coerceIn(0, 255)
}

@Composable
private fun ScreenshotCanvasActionBar(
    modifier: Modifier,
    state: ScreenshotCanvasUiState,
    markerPanel: Boolean,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TooltipIconButton(tooltip = "Undo", onClick = {}, modifier = Modifier.size(34.dp), enabled = false) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
            }
            TooltipIconButton(tooltip = "Redo", onClick = {}, modifier = Modifier.size(34.dp), enabled = false) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenshotLayerChip("BG", state.showScreenshotBg) { state.showScreenshotBg = !state.showScreenshotBg }
                ScreenshotLayerChip("A11Y", state.showAccessibilityNodes) { state.showAccessibilityNodes = !state.showAccessibilityNodes }
                ScreenshotLayerChip("DOM", state.showDomNodes) { state.showDomNodes = !state.showDomNodes }
                ScreenshotLayerChip("OCR", state.showOcrNodes) { state.showOcrNodes = !state.showOcrNodes }
                ScreenshotLayerChip("TPL", state.showVisionTemplateNodes) { state.showVisionTemplateNodes = !state.showVisionTemplateNodes }
                ScreenshotLayerChip("YOLO", state.showYoloNodes) { state.showYoloNodes = !state.showYoloNodes }
                ScreenshotLayerChip("MRK", state.showMarkers) { state.showMarkers = !state.showMarkers }
                ScreenshotLayerChip("INV", !state.filterHideInvisible) { state.filterHideInvisible = !state.filterHideInvisible }
                ScreenshotLayerChip("CLK", !state.filterHideClickable) { state.filterHideClickable = !state.filterHideClickable }
                ScreenshotLayerChip("FOC", !state.filterHideNonFocusable) { state.filterHideNonFocusable = !state.filterHideNonFocusable }
                if (markerPanel) {
                    ScreenshotLayerChip("TPL-M", state.markerMode == ScreenshotCanvasMarkerMode.Template) { state.markerMode = ScreenshotCanvasMarkerMode.Template }
                    ScreenshotLayerChip("REG", state.markerMode == ScreenshotCanvasMarkerMode.Region) { state.markerMode = ScreenshotCanvasMarkerMode.Region }
                    ScreenshotLayerChip("PT", state.markerMode == ScreenshotCanvasMarkerMode.Point) { state.markerMode = ScreenshotCanvasMarkerMode.Point }
                    ScreenshotLayerChip("SWP", state.markerMode == ScreenshotCanvasMarkerMode.Swipe) { state.markerMode = ScreenshotCanvasMarkerMode.Swipe }
                    ScreenshotLayerChip("PATH", state.markerMode == ScreenshotCanvasMarkerMode.Path) { state.markerMode = ScreenshotCanvasMarkerMode.Path }
                }
            }
            TooltipIconButton(tooltip = "Zoom -", onClick = { state.zoomOut() }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom -", modifier = Modifier.size(18.dp))
            }
            Text(
                text = "${(state.zoom * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TooltipIconButton(tooltip = "Zoom +", onClick = { state.zoomIn() }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom +", modifier = Modifier.size(18.dp))
            }
            TooltipIconButton(tooltip = "Zentrieren", onClick = { state.center() }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Zentrieren", modifier = Modifier.size(18.dp))
            }
            TooltipIconButton(tooltip = "Inspector", onClick = { state.inspectorVisible = !state.inspectorVisible }, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Inspector",
                    tint = if (state.inspectorVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ScreenshotLayerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        },
        modifier = Modifier.height(30.dp),
    )
}

@Composable
private fun MarkerUnderScreenshotPanel(
    state: ScreenshotCanvasUiState,
    asset: ScreenshotCanvasAsset?,
    imageSize: Pair<Int, Int>?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = {
                    state.visualTestScore = if (state.selectedRegion == null) null else 0.91f
                    state.markerStatusMessage = if (state.selectedRegion == null) {
                        "Keine Region markiert."
                    } else {
                        "Pipeline-Test vorbereitet."
                    }
                },
                enabled = state.selectedRegion != null,
                modifier = Modifier.height(34.dp),
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("Crop", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 2.dp))
            }
            MarkerModeButton("Template", state.markerMode == ScreenshotCanvasMarkerMode.Template) {
                state.markerMode = ScreenshotCanvasMarkerMode.Template
            }
            MarkerModeButton("Region", state.markerMode == ScreenshotCanvasMarkerMode.Region) {
                state.markerMode = ScreenshotCanvasMarkerMode.Region
            }
            MarkerModeButton("Point", state.markerMode == ScreenshotCanvasMarkerMode.Point) {
                state.markerMode = ScreenshotCanvasMarkerMode.Point
            }
            MarkerModeButton("Swipe", state.markerMode == ScreenshotCanvasMarkerMode.Swipe) {
                state.markerMode = ScreenshotCanvasMarkerMode.Swipe
            }
            MarkerModeButton("Path", state.markerMode == ScreenshotCanvasMarkerMode.Path) {
                state.markerMode = ScreenshotCanvasMarkerMode.Path
            }
        }
        Text(
            text = markerMetricsTitle(state.selectedRegion, state.markerMode, imageSize),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.savedMarkers.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.savedMarkers, key = { it.id }) { marker ->
                    MarkerSavedItemCard(
                        marker = marker,
                        selected = marker.id == state.selectedSavedMarkerId,
                        onClick = {
                            state.selectedSavedMarkerId = marker.id
                            state.selectedAssetId = marker.assetId ?: state.selectedAssetId
                            state.selectedRegion = marker.region
                            state.markerMode = marker.markerMode
                            state.matchKind = marker.matchKind
                            state.processingMode = marker.processingMode
                            state.threshold = marker.threshold
                            state.rotationDegrees = marker.rotationDegrees
                            state.matchReadMe = marker.matchReadMe
                            state.colourHex = marker.colourHex
                            state.templateName = marker.label
                            state.markerStatusMessage = "Marker geladen."
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ScreenshotCanvasProcessingMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.processingMode == mode,
                    onClick = { state.processingMode = mode },
                    label = {
                        Text(
                            text = when (mode) {
                                ScreenshotCanvasProcessingMode.Original -> "Original"
                                ScreenshotCanvasProcessingMode.Grayscale -> "Graustufen"
                                ScreenshotCanvasProcessingMode.HighContrast -> "Kontrast"
                                ScreenshotCanvasProcessingMode.Edge -> "Kanten"
                                ScreenshotCanvasProcessingMode.Inverse -> "Invers"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    modifier = Modifier.height(28.dp),
                )
            }
        }
        OutlinedTextField(
            value = state.templateName,
            onValueChange = { state.templateName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name", style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Rotation ${state.rotationDegrees.toInt()}°", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = state.rotationDegrees,
                    onValueChange = { state.rotationDegrees = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Schwelle ${"%.0f".format(state.threshold * 100)}%", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = state.threshold,
                    onValueChange = { state.threshold = it },
                    valueRange = 0.5f..0.99f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = state.matchReadMe,
                onValueChange = { state.matchReadMe = it },
                modifier = Modifier.weight(1f),
                label = { Text("Match", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = state.colourHex,
                onValueChange = { state.colourHex = it },
                modifier = Modifier.width(108.dp),
                label = { Text("Colour", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
        }
        state.visualTestScore?.let { score ->
            val passed = score >= state.threshold
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (passed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = "Pipeline-Test: ${"%.1f".format(score * 100)}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        if (state.markerStatusMessage.isNotBlank()) {
            Text(
                text = state.markerStatusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(
                onClick = {
                    state.visualTestScore = if (state.selectedRegion == null) null else 0.91f
                    state.markerStatusMessage = if (state.selectedRegion == null) "Keine Region markiert." else "Pipeline-Test OK."
                },
                enabled = state.selectedRegion != null,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("PipelineTEST", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = {
                    val region = state.selectedRegion
                    state.markerStatusMessage = if (region == null) {
                        "Keine Region markiert."
                    } else {
                        val now = System.currentTimeMillis()
                        val id = state.selectedSavedMarkerId ?: "marker:${now}"
                        val marker = ScreenshotCanvasSavedMarker(
                            id = id,
                            label = state.templateName.trim().ifBlank { "Marker" },
                            assetId = asset?.id,
                            assetLabel = asset?.label,
                            region = region,
                            markerMode = state.markerMode,
                            matchKind = state.matchKind,
                            processingMode = state.processingMode,
                            threshold = state.threshold,
                            rotationDegrees = state.rotationDegrees,
                            matchReadMe = state.matchReadMe,
                            colourHex = state.colourHex,
                            updatedAt = now,
                        )
                        val index = state.savedMarkers.indexOfFirst { it.id == id }
                        if (index >= 0) {
                            state.savedMarkers[index] = marker
                        } else {
                            state.savedMarkers.add(0, marker)
                        }
                        state.selectedSavedMarkerId = id
                        "${state.markerMode.name} '${marker.label}' gespeichert."
                    }
                },
                enabled = state.selectedRegion != null,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("SPEICHERN", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun MarkerModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MarkerSavedItemCard(
    marker: ScreenshotCanvasSavedMarker,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier
            .width(132.dp)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = marker.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${marker.markerMode.name} / ${marker.processingMode.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = marker.assetLabel ?: "Kein Screenshot",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun markerMetricsTitle(
    region: ScreenshotCanvasRegion?,
    markerMode: ScreenshotCanvasMarkerMode,
    imageSize: Pair<Int, Int>?,
): String {
    if (region == null) {
        val size = imageSize?.let { "  image=${it.first}x${it.second}" }.orEmpty()
        return when (markerMode) {
            ScreenshotCanvasMarkerMode.Point -> "Punkt: x=-  y=-$size"
            ScreenshotCanvasMarkerMode.Swipe -> "Swipe: start=-  end=-$size"
            ScreenshotCanvasMarkerMode.Path -> "Path: punkte=-$size"
            ScreenshotCanvasMarkerMode.Template -> "Template: bbox=-$size"
            ScreenshotCanvasMarkerMode.Region -> "Region: x=-  y=-  w=-  h=-$size"
        }
    }
    return when (markerMode) {
        ScreenshotCanvasMarkerMode.Point -> {
            val cx = region.x + region.width / 2
            val cy = region.y + region.height / 2
            "Punkt: x=$cx  y=$cy"
        }
        ScreenshotCanvasMarkerMode.Swipe -> "Swipe: start=(${region.x},${region.y})  end=(${region.x + region.width},${region.y + region.height})"
        ScreenshotCanvasMarkerMode.Path -> "Path: start=(${region.x},${region.y})  end=(${region.x + region.width},${region.y + region.height})"
        ScreenshotCanvasMarkerMode.Template -> "Template: bbox=${region.x},${region.y} ${region.width}x${region.height}"
        ScreenshotCanvasMarkerMode.Region -> "Region: x=${region.x}  y=${region.y}  w=${region.width}  h=${region.height}"
    }
}

@Composable
private fun ScreenshotCanvasInspector(
    modifier: Modifier,
    asset: ScreenshotCanvasAsset?,
    imageSize: String,
    state: ScreenshotCanvasUiState,
    markerPanel: Boolean,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(122.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("Inspector", style = MaterialTheme.typography.titleSmall)
            ScreenshotInspectorRow("Name", asset?.label ?: "-")
            ScreenshotInspectorRow("Datum", asset?.dateLabel ?: "-")
            ScreenshotInspectorRow("App / Scene", asset?.let { "${it.app} / ${it.scene}" } ?: "-")
            ScreenshotInspectorRow("Bild", imageSize)
            if (markerPanel) {
                ScreenshotInspectorRow(
                    "Marker",
                    "${state.markerMode.name} / ${state.matchKind.name} / ${state.selectedRegion?.let { "${it.x},${it.y} ${it.width}x${it.height}" } ?: "-"}",
                )
            }
        }
    }
}

@Composable
private fun ScreenshotInspectorRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(86.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun ScreenshotCanvasGrid(modifier: Modifier = Modifier) {
    val dotColor = Color(0xFF302743).copy(alpha = 0.62f)
    Canvas(modifier = modifier) {
        val spacing = 18.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            var y = 0f
            while (y <= size.height) {
                drawCircle(color = dotColor, radius = 1.1f, center = Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
private fun ScreenshotOverlayGuide(
    modifier: Modifier,
    color: Color,
    insetFraction: Float,
) {
    Canvas(modifier = modifier) {
        val insetX = size.width * insetFraction
        val insetY = size.height * insetFraction
        drawLine(color.copy(alpha = 0.78f), Offset(insetX, insetY), Offset(size.width - insetX, insetY), 2.5f)
        drawLine(color.copy(alpha = 0.78f), Offset(size.width - insetX, insetY), Offset(size.width - insetX, size.height - insetY), 2.5f)
        drawLine(color.copy(alpha = 0.78f), Offset(size.width - insetX, size.height - insetY), Offset(insetX, size.height - insetY), 2.5f)
        drawLine(color.copy(alpha = 0.78f), Offset(insetX, size.height - insetY), Offset(insetX, insetY), 2.5f)
    }
}

private fun loadScreenshotCanvasAssets(context: Context): List<ScreenshotCanvasAsset> {
    val roots = listOfNotNull(
        File(runtimeFilesRoot(context), "screenshots"),
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        File(context.filesDir, "screenshots"),
    )
    return roots
        .flatMap { root -> root.listFiles().orEmpty().asIterable() }
        .filter { it.isFile && it.extension.lowercase(Locale.ROOT) in setOf("png", "jpg", "jpeg", "webp") }
        .distinctBy { it.absolutePath }
        .sortedByDescending { it.lastModified() }
        .map { file ->
            ScreenshotCanvasAsset(
                file = file,
                label = file.nameWithoutExtension.ifBlank { file.name },
                app = "VisualTasker Studio WSS",
                scene = file.parentFile?.name ?: "Screenshot",
            )
        }
}

private fun screenshotCanvasMarkerStoreFile(context: Context): File =
    File(runtimeFilesRoot(context), "markers/saved-markers.json")

private fun loadScreenshotCanvasSavedMarkers(context: Context): List<ScreenshotCanvasSavedMarker> {
    val file = screenshotCanvasMarkerStoreFile(context)
    if (!file.isFile) return emptyList()
    return runCatching {
        val root = JSONObject(file.readText())
        val markers = root.optJSONArray("markers") ?: JSONArray()
        buildList {
            for (index in 0 until markers.length()) {
                val item = markers.optJSONObject(index) ?: continue
                val region = item.optJSONObject("region") ?: continue
                val marker = ScreenshotCanvasSavedMarker(
                    id = item.optString("id").takeIf { it.isNotBlank() } ?: "marker:${index}:${System.currentTimeMillis()}",
                    label = item.optString("label", "Marker").ifBlank { "Marker" },
                    assetId = item.optString("assetId").takeIf { it.isNotBlank() },
                    assetLabel = item.optString("assetLabel").takeIf { it.isNotBlank() },
                    region = ScreenshotCanvasRegion(
                        x = region.optInt("x", 0),
                        y = region.optInt("y", 0),
                        width = region.optInt("width", 1).coerceAtLeast(1),
                        height = region.optInt("height", 1).coerceAtLeast(1),
                    ),
                    markerMode = enumValueOrDefault(item.optString("markerMode"), ScreenshotCanvasMarkerMode.Region),
                    matchKind = enumValueOrDefault(item.optString("matchKind"), ScreenshotCanvasMatchKind.OCR),
                    processingMode = enumValueOrDefault(item.optString("processingMode"), ScreenshotCanvasProcessingMode.Original),
                    threshold = item.optDouble("threshold", 0.85).toFloat().coerceIn(0f, 1f),
                    rotationDegrees = item.optDouble("rotationDegrees", 0.0).toFloat(),
                    matchReadMe = item.optString("matchReadMe", ""),
                    colourHex = item.optString("colourHex", "#4FC3F7"),
                    updatedAt = item.optLong("updatedAt", 0L),
                )
                add(marker)
            }
        }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())
}

private fun persistScreenshotCanvasSavedMarkers(
    context: Context,
    markers: List<ScreenshotCanvasSavedMarker>,
) {
    runCatching {
        val file = screenshotCanvasMarkerStoreFile(context)
        file.parentFile?.mkdirs()
        val root = JSONObject()
        root.put("schemaVersion", 1)
        val array = JSONArray()
        markers.forEach { marker ->
            val item = JSONObject()
                .put("id", marker.id)
                .put("label", marker.label)
                .put("assetId", marker.assetId ?: "")
                .put("assetLabel", marker.assetLabel ?: "")
                .put("markerMode", marker.markerMode.name)
                .put("matchKind", marker.matchKind.name)
                .put("processingMode", marker.processingMode.name)
                .put("threshold", marker.threshold.toDouble())
                .put("rotationDegrees", marker.rotationDegrees.toDouble())
                .put("matchReadMe", marker.matchReadMe)
                .put("colourHex", marker.colourHex)
                .put("updatedAt", marker.updatedAt)
            item.put(
                "region",
                JSONObject()
                    .put("x", marker.region.x)
                    .put("y", marker.region.y)
                    .put("width", marker.region.width)
                    .put("height", marker.region.height)
            )
            array.put(item)
        }
        root.put("markers", array)
        file.writeText(root.toString(2))
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T =
    raw?.takeIf { it.isNotBlank() }
        ?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() }
        ?: fallback

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
                TooltipIconButton(tooltip = "Canvas öffnen", onClick = { onOpenPanel(PanelType.Screenshot) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Photo, contentDescription = "Canvas öffnen")
                }
                TooltipIconButton(tooltip = "Marker öffnen", onClick = { onOpenPanel(PanelType.Marker) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.TouchApp, contentDescription = "Marker öffnen")
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
                PanelType.Screenshot,
                PanelType.Marker,
                PanelType.Vision,
                PanelType.Datastore,
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
    showMiniMap: Boolean,
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
            showMiniMap = showMiniMap,
            showTopIconBar = false,
            soundEffectsEnabled = true,
            hapticFeedbackEnabled = true
        ),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ColumnScope.BlockEditorCompactCategoryRail(
    session: BlockEditorShellEditorSession?,
    onExpandRequested: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        WorkspaceRailActionList(
            listOf(
                WorkspaceRailActionSpec("Speichern", Icons.Default.Save, enabled = session != null, onClick = onSave),
                WorkspaceRailActionSpec("Undo", Icons.AutoMirrored.Filled.Undo, enabled = session != null) { session?.controller?.undo() },
                WorkspaceRailActionSpec("Redo", Icons.AutoMirrored.Filled.Redo, enabled = session != null) { session?.controller?.redo() },
                WorkspaceRailActionSpec("Zoom +", Icons.Default.ZoomIn, enabled = session != null) { session?.controller?.zoomIn() },
                WorkspaceRailActionSpec("Zoom -", Icons.Default.ZoomOut, enabled = session != null) { session?.controller?.zoomOut() },
                WorkspaceRailActionSpec("Einpassen", Icons.Default.CenterFocusStrong, enabled = session != null) {
                    session?.controller?.fitWorkspaceToCanvas(force = true)
                },
                WorkspaceRailActionSpec("Auto anordnen", Icons.Default.AutoAwesomeMosaic, enabled = session != null) {
                    session?.controller?.autoArrangeWorkspace()
                },
                WorkspaceRailActionSpec(
                    label = if (session?.controller?.selectedBlockCollapsed == true) "Ausklappen" else "Einklappen",
                    icon = if (session?.controller?.selectedBlockCollapsed == true) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    enabled = session?.controller?.canToggleSelectedBlockCollapse == true,
                    selected = session?.controller?.selectedBlockCollapsed == true,
                ) {
                    session?.controller?.toggleSelectedBlockCollapse()
                },
                WorkspaceRailActionSpec(
                    label = "Block löschen",
                    icon = Icons.Default.DeleteSweep,
                    enabled = session?.controller?.selectedBlockIds?.isNotEmpty() == true,
                ) {
                    session?.controller?.deleteSelectedBlock()
                },
                WorkspaceRailActionSpec("Blockdesigner", Icons.Default.GridView, enabled = session != null) {
                    session?.controller?.openBlockFactory()
                },
                WorkspaceRailActionSpec("Workspace leeren", Icons.Default.DeleteSweep, enabled = session != null) {
                    session?.controller?.clearWorkspace()
                },
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        WorkspaceRailActionButton("Blockpalette", Icons.Default.AddCircle, enabled = session != null) {
            if (session?.controller?.expandedCategory == null) {
                session?.controller?.onCategoryClick(BlockCategories.ACTION)
            }
            onExpandRequested()
        }
    }
}

@Composable
private fun ColumnScope.WorkspaceRailActionList(
    actions: List<WorkspaceRailActionSpec>,
) {
    actions.forEach { action ->
        WorkspaceRailActionButton(
            label = action.label,
            icon = action.icon,
            enabled = action.enabled,
            selected = action.selected,
            onClick = action.onClick,
        )
    }
}

private data class WorkspaceRailActionSpec(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun WorkspaceRailActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
    }
    TooltipIconButton(tooltip = label, onClick = onClick, modifier = Modifier.size(34.dp), enabled = enabled) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(18.dp))
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
    val allDefinitions = remember(session) {
        session.controller.registry.allDefinitions()
            .filter { it.paletteVisible }
            .filter { it.category != BlockCategories.VARIABLE || it.id != BlockTypes.VARIABLE_GET }
            .distinctBy { it.id }
            .sortedWith(
                compareBy<BlockDefinition>(
                    { BlockCategories.metaFor(it.category).label },
                    { it.paletteOrder },
                    { it.label.lowercase() },
                )
            )
    }
    val categories = remember(allDefinitions) {
        allDefinitions
            .map { BlockCategories.metaFor(it.category) }
            .distinctBy { it.id }
            .sortedBy { it.label.lowercase() }
    }
    var activeCategory by remember(session) { mutableStateOf(session.controller.expandedCategory ?: BlockCategories.ACTION) }
    var query by remember(session) { mutableStateOf("") }
    val activeMeta = BlockCategories.metaFor(activeCategory)
    val activeAccent = Color(activeMeta.accentArgb)
    val filteredDefinitions = remember(allDefinitions, activeCategory, query) {
        val needle = query.trim().lowercase()
        allDefinitions.filter { definition ->
            definition.category == activeCategory &&
                (needle.isEmpty() ||
                    definition.label.lowercase().contains(needle) ||
                    definition.id.lowercase().contains(needle))
        }
    }
    LaunchedEffect(session) {
        if (session.controller.expandedCategory == null) {
            session.controller.onCategoryClick(BlockCategories.ACTION)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            categories.forEach { category ->
                val selected = activeCategory == category.id
                val accent = Color(category.accentArgb)
                TooltipIconButton(
                    tooltip = category.label,
                    onClick = {
                        activeCategory = category.id
                        if (session.controller.expandedCategory != category.id) {
                            session.controller.onCategoryClick(category.id)
                        }
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (selected) accent.copy(alpha = 0.72f) else accent.copy(alpha = 0.26f))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else accent.copy(alpha = 0.55f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = CategoryIcons.forCategory(category.id),
                            contentDescription = category.label,
                            tint = if (selected) Color.White else accent,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = activeMeta.label,
                style = MaterialTheme.typography.titleSmall,
                color = activeAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (paletteInsertMode == BlockPaletteInsertMode.DragFromPalette) {
                    "Drag-from-palette vorbereitet"
                } else {
                    "Tippe zum Hinzufügen"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Suche") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Suche löschen")
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodySmall,
            )
            if (activeCategory == BlockCategories.VARIABLE) {
                FilledTonalButton(
                    onClick = { session.controller.createVariable("variable", "Any") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text("Neue Variable", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp))
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (filteredDefinitions.isEmpty()) {
                    item {
                        Text(
                            text = "Keine Blöcke in dieser Kategorie",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(filteredDefinitions, key = { it.id }) { definition ->
                    BlockEditorRailBlockChip(
                        definition = definition,
                        onAddBlock = session.controller::addBlockFromPalette,
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockEditorRailBlockChip(
    definition: BlockDefinition,
    onAddBlock: (BlockDefinition) -> Unit,
) {
    val accent = Color(BlockCategories.metaFor(definition.category).accentArgb)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddBlock(definition) },
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.26f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = CategoryIcons.forCategory(definition.category),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = definition.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = definition.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.FlowchartCompactActionRail(
    session: FlowchartShellEditorSession?,
    selectedNodeId: FlowNodeId?,
    selectedEdgeId: FlowEdgeId?,
    onExpandRequested: () -> Unit,
    onSave: () -> Unit,
    onRunDry: () -> Unit,
    onRunLive: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    canStepBack: Boolean,
    canStepForward: Boolean,
    onDeleteNode: (FlowNodeId) -> Unit,
    onDisconnectEdge: (FlowEdgeId) -> Unit,
    onUndoWorkspace: () -> Boolean,
    onRedoWorkspace: () -> Boolean,
) {
    val controller = session?.controller
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        WorkspaceRailActionList(
            listOf(
                WorkspaceRailActionSpec("Speichern", Icons.Default.Save, enabled = session != null, onClick = onSave),
                WorkspaceRailActionSpec("Undo", Icons.AutoMirrored.Filled.Undo, enabled = session != null) {
                    if (!onUndoWorkspace()) controller?.dispatch(FlowInteractionAction.UndoViewChange)
                },
                WorkspaceRailActionSpec("Redo", Icons.AutoMirrored.Filled.Redo, enabled = session != null) {
                    if (!onRedoWorkspace()) controller?.dispatch(FlowInteractionAction.RedoViewChange)
                },
                WorkspaceRailActionSpec("Zoom +", Icons.Default.ZoomIn, enabled = session != null) {
                    controller?.dispatch(FlowInteractionAction.ZoomViewport(1.2, FlowPoint(0.0, 0.0)))
                },
                WorkspaceRailActionSpec("Zoom -", Icons.Default.ZoomOut, enabled = session != null) {
                    controller?.dispatch(FlowInteractionAction.ZoomViewport(1 / 1.2, FlowPoint(0.0, 0.0)))
                },
                WorkspaceRailActionSpec("Zentrieren", Icons.Default.CenterFocusStrong, enabled = session != null) {
                    controller?.attachGraph(controller.snapshot().graph ?: session.graphDocument, null)
                },
                WorkspaceRailActionSpec("Auto anordnen", Icons.Default.AutoAwesomeMosaic, enabled = session != null) {
                    controller?.replaceLayout(
                        FlowLayoutConfig(
                            layerSpacing = 148.0,
                            nodeSpacing = 92.0,
                            componentSpacing = 168.0,
                            routingClearance = 36.0,
                            pinnedNodePolicy = FlowPinnedNodePolicy.IGNORE,
                        )
                    )?.let { view -> session.onViewDocumentChanged(view) }
                },
                WorkspaceRailActionSpec("Dry Run", Icons.Default.PlayArrow, enabled = session != null, onClick = onRunDry),
                WorkspaceRailActionSpec("Live Run", Icons.Default.PlayCircle, enabled = session != null, onClick = onRunLive),
                WorkspaceRailActionSpec("Step zurück", Icons.Default.ArrowBack, enabled = session != null && canStepBack, onClick = onStepBack),
                WorkspaceRailActionSpec("Step vor", Icons.Default.ArrowForward, enabled = session != null && canStepForward, onClick = onStepForward),
                WorkspaceRailActionSpec(
                    label = if (selectedEdgeId != null) "Kante löschen" else "Node löschen",
                    icon = Icons.Default.DeleteSweep,
                    enabled = session != null && (selectedNodeId != null || selectedEdgeId != null),
                ) {
                    selectedNodeId?.let(onDeleteNode) ?: selectedEdgeId?.let(onDisconnectEdge)
                },
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        WorkspaceRailActionButton("Node-Palette", Icons.Default.AddCircle, enabled = session != null, onClick = onExpandRequested)
    }
}

@Composable
private fun FlowchartPanel(
    panelId: String,
    uiPrefs: android.content.SharedPreferences,
    graphContent: String,
    runtimeSnapshot: FlowRuntimeSnapshot?,
    onRunDry: () -> Unit,
    onRunLive: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    canStepBack: Boolean,
    canStepForward: Boolean,
    stepLabel: String?,
    showMiniMap: Boolean,
    onNodeSelected: (FlowNodeId) -> Unit,
    onSelectionChanged: (FlowNodeId?, FlowEdgeId?) -> Unit,
    onNodeDelete: (FlowNodeId) -> Unit,
    onNodesDelete: (Set<FlowNodeId>) -> Unit,
    onNodesConnect: (FlowNodeId, FlowNodeId, FlowEdgeKind, String?) -> Unit,
    onPortsConnect: (FlowNodeId, String, FlowNodeId, String, FlowEdgeKind) -> Unit,
    connectionOptionsFor: (FlowNodeId, FlowNodeId) -> List<com.visualtasker.wss.workspace.model.FlowchartConnectionOption>,
    onEdgeDisconnect: (FlowEdgeId) -> Unit,
    onNodeFieldUpdate: (FlowNodeId, String, String) -> Unit,
    onNodeTypeReplace: (FlowNodeId, String) -> Unit,
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
        onRunLive = onRunLive,
        onStepBack = onStepBack,
        onStepForward = onStepForward,
        canStepBack = canStepBack,
        canStepForward = canStepForward,
        stepLabel = stepLabel,
        onNodeSelected = onNodeSelected,
        onSelectionChanged = onSelectionChanged,
        onDeleteNode = onNodeDelete,
        onDeleteNodes = onNodesDelete,
        onConnectNodes = onNodesConnect,
        onConnectPorts = onPortsConnect,
        connectionOptionsFor = connectionOptionsFor,
        onDisconnectEdge = onEdgeDisconnect,
        onUpdateNodeField = onNodeFieldUpdate,
        onReplaceNodeType = onNodeTypeReplace,
        onAddIfBranch = onIfBranchAdd,
        onRemoveIfBranch = onIfBranchRemove,
        onViewChanged = onViewChanged,
        onUndoWorkspace = onWorkspaceUndo,
        onRedoWorkspace = onWorkspaceRedo,
        showMiniMap = showMiniMap,
        showTopToolbar = false,
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
                AssistChip(onClick = { onRestore(panel.id) }, label = { Text(displayTitleForPanel(panel)) })
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
    workspacePanel("panel-1", PanelType.RecorderSteps, "Stepper", 84f, 112f, 360f, 360f, 1),
    workspacePanel("panel-2", PanelType.BlockEditor, "BlockEditor", 470f, 124f, 360f, 300f, 2),
    workspacePanel("panel-3", PanelType.Flowchart, "Flowchart", 470f, 460f, 360f, 300f, 3),
    workspacePanel("panel-4", PanelType.LogConsole, "LogConsole", 860f, 150f, 320f, 240f, 4)
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
    PanelType.Vision -> Icons.Default.CenterFocusStrong
    PanelType.Datastore -> Icons.Default.FolderOpen
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
        PanelType.Screenshot,
        PanelType.Marker,
        PanelType.Vision,
        PanelType.Datastore,
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
    PanelType.Screenshot -> "Canvas"
    PanelType.Marker -> "Marker"
    PanelType.Vision -> "Vision"
    PanelType.Datastore -> "Datastore"
    PanelType.Emscript -> "EMScript"
    PanelType.RuntimeLog -> "RuntimeLog"
    PanelType.TextEditor -> "TextEditor"
    PanelType.LogConsole -> "LogConsole"
    PanelType.DebugInfo -> "Debug"
    PanelType.M3Director -> "M3Director"
}

private fun displayTitleForPanel(panel: PanelState): String {
    val canonical = displayNameForPanelType(panel.type)
    val trailingNumber = Regex("^\\s*${Regex.escape(canonical)}\\s+\\d+\\s*$", RegexOption.IGNORE_CASE)
    val generatedPanelTitle = supportedWorkspacePanelTypes.any { type ->
        val name = displayNameForPanelType(type)
        Regex("^\\s*${Regex.escape(name)}(?:\\s+\\d+)?\\s*$", RegexOption.IGNORE_CASE).matches(panel.title)
    }
    return if (trailingNumber.matches(panel.title) || generatedPanelTitle) canonical else panel.title
}

private fun PanelState.toMainPanelState(): MainPanelState =
    MainPanelState(
        id = id,
        position = Offset(x, y),
        width = width.toInt(),
        height = height.toInt(),
        accentColor = accentColor,
        title = displayTitleForPanel(this),
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
    PanelType.Vision,
    PanelType.Datastore,
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

private fun loadPanelRailExpanded(
    uiPrefs: android.content.SharedPreferences,
    panelId: String
): Boolean = uiPrefs.getBoolean(PANEL_RAIL_EXPANDED_PREF_PREFIX + panelId, false)

private fun persistPanelRailExpanded(
    uiPrefs: android.content.SharedPreferences,
    panelId: String,
    expanded: Boolean
) {
    uiPrefs.edit()
        .putBoolean(PANEL_RAIL_EXPANDED_PREF_PREFIX + panelId, expanded)
        .apply()
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
    blockEditorMiniMapVisible: Boolean,
    onBlockEditorMiniMapVisibleChange: (Boolean) -> Unit,
    flowchartMiniMapVisible: Boolean,
    onFlowchartMiniMapVisibleChange: (Boolean) -> Unit,
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

            1 -> FlowchartSettingsTab(
                miniMapVisible = flowchartMiniMapVisible,
                onMiniMapVisibleChange = onFlowchartMiniMapVisibleChange,
            )
            2 -> BlockEditorSettingsTab(
                paletteInsertMode = blockPaletteInsertMode,
                onPaletteInsertModeChange = onBlockPaletteInsertModeChange,
                miniMapVisible = blockEditorMiniMapVisible,
                onMiniMapVisibleChange = onBlockEditorMiniMapVisibleChange,
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
private fun FlowchartSettingsTab(
    miniMapVisible: Boolean,
    onMiniMapVisibleChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Flowchart", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Minimap anzeigen")
            Switch(checked = miniMapVisible, onCheckedChange = onMiniMapVisibleChange)
        }
    }
}

@Composable
private fun BlockEditorSettingsTab(
    paletteInsertMode: BlockPaletteInsertMode,
    onPaletteInsertModeChange: (BlockPaletteInsertMode) -> Unit,
    miniMapVisible: Boolean,
    onMiniMapVisibleChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Palette/Flyout", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Minimap anzeigen")
            Switch(checked = miniMapVisible, onCheckedChange = onMiniMapVisibleChange)
        }
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

private fun workspaceRuntimeCapabilityGate(): RuntimeCapabilityGate =
    if (VisualTaskerAccessibilityService.isConnected()) {
        RuntimeCapabilityGate.withAccessibilityAdapter()
    } else {
        RuntimeCapabilityGate()
    }

private fun runtimeFilesRoot(context: Context): java.io.File =
    java.io.File(context.filesDir, "emscript-runtime").apply { mkdirs() }

private fun runtimeFileFor(context: Context, rawPath: String): java.io.File? {
    val clean = rawPath.trim().trim('"').replace('\\', '/').trimStart('/')
    if (clean.isBlank() || clean.contains("..")) return null
    val root = runtimeFilesRoot(context)
    val file = java.io.File(root, clean)
    return if (file.canonicalPath.startsWith(root.canonicalPath)) file else null
}

private fun clearRuntimeDirectory(directory: java.io.File): Int {
    if (!directory.exists()) return 0
    var removed = 0
    directory.listFiles().orEmpty().forEach { child ->
        if (child.deleteRecursively()) removed += 1
    }
    return removed
}

private fun playRuntimeBeep(frequencyHz: Int, durationMs: Int, volumePercent: Int) {
    val sampleRate = 44_100
    val safeFrequency = frequencyHz.coerceIn(20, 20_000)
    val safeDurationMs = durationMs.coerceIn(10, 10_000)
    val amplitude = (volumePercent.coerceIn(0, 100) / 100.0 * Short.MAX_VALUE * 0.65).toInt()
    val sampleCount = (sampleRate * safeDurationMs / 1_000.0).toInt().coerceAtLeast(1)
    val samples = ShortArray(sampleCount) { index ->
        val envelope = when {
            index < sampleRate / 200 -> index / (sampleRate / 200.0)
            index > sampleCount - sampleRate / 200 -> (sampleCount - index).coerceAtLeast(0) / (sampleRate / 200.0)
            else -> 1.0
        }.coerceIn(0.0, 1.0)
        (sin(2.0 * PI * safeFrequency * index / sampleRate) * amplitude * envelope).toInt().toShort()
    }
    Thread {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
                .build()
            try {
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(safeDurationMs.toLong() + 40L)
            } finally {
                track.release()
            }
        }
    }.start()
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
