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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.overlay.StudioOverlayService
import com.visualtasker.wss.emscript.editor.EmScriptEditorScreen
import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.editor.EmscriptEditorSession
import com.visualtasker.wss.emscript.editor.EmscriptEditorUiState
import com.visualtasker.wss.emscript.editor.SyntaxHighlighter
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.RuntimeAdapterResult
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityGate
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityReport
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityStatus
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntimeEnvironment
import com.visualtasker.wss.emscript.runtime.WorkspaceDryRunRuntime
import com.visualtasker.wss.emscript.runtime.RuntimeAutomationRegion
import com.visualtasker.wss.emscript.runtime.RuntimeTemplateMatch
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
import com.visualtasker.wss.workspace.model.JunctionatorSeed
import com.visualtasker.wss.workspace.model.RailMode
import com.visualtasker.wss.workspace.model.RailProjection
import com.visualtasker.wss.workspace.model.RailScaleMode
import com.visualtasker.wss.workspace.model.RailSurfaceMode
import com.visualtasker.wss.workspace.model.RailTimelineMarker
import com.visualtasker.wss.workspace.model.RailTrackKind
import com.visualtasker.wss.workspace.model.RailTrack
import com.visualtasker.wss.workspace.model.RecordingEventStore
import com.visualtasker.wss.workspace.model.RecordingSessionUi
import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.StepStatus
import com.visualtasker.wss.workspace.model.CanvasObservationFilters
import com.visualtasker.wss.workspace.model.CanvasObservationFamily
import com.visualtasker.wss.workspace.model.CanvasObservationItem
import com.visualtasker.wss.workspace.model.CanvasObservationLineStyle
import com.visualtasker.wss.workspace.model.CanvasObservationProjection
import com.visualtasker.wss.workspace.model.CanvasObservationProjector
import com.visualtasker.wss.workspace.model.CanvasVisionObservationFactory
import com.visualtasker.wss.workspace.model.VisualUiMemoryProjector
import com.visualtasker.wss.workspace.model.VisualUiMemoryProjection
import com.visualtasker.wss.workspace.model.VisualUiMemorySuggestion
import com.visualtasker.wss.workspace.model.CoordinateSpaceKind
import com.visualtasker.wss.workspace.model.WorldObservation
import com.visualtasker.wss.workspace.model.WorldviewDataProjector
import com.visualtasker.wss.workspace.model.WorldviewDocument
import com.visualtasker.wss.workspace.model.WorldviewRect
import com.visualtasker.wss.workspace.model.WorldviewInspectorProjector
import com.visualtasker.wss.workspace.model.WorldviewInspectorSubject
import com.visualtasker.wss.workspace.model.WorldviewInspectorSubjectKind
import com.visualtasker.wss.workspace.model.WorkspaceMarkerMode
import com.visualtasker.wss.workspace.model.WorkspacePointBounds
import com.visualtasker.wss.workspace.model.WorkspaceRegionBounds
import com.visualtasker.wss.workspace.model.WorkspaceResource
import com.visualtasker.wss.workspace.model.WorkspaceResourceBundle
import com.visualtasker.wss.workspace.model.WorkspaceResourceKind
import com.visualtasker.wss.workspace.model.WssDragTreeItem
import com.visualtasker.wss.workspace.model.WssDropEffectResolver
import com.visualtasker.wss.workspace.model.WssDropResult
import com.visualtasker.wss.workspace.model.WssDragRules
import com.visualtasker.wss.workspace.model.WssPanelDragProjection
import com.visualtasker.wss.workspace.model.WssPanelDragProjector
import com.visualtasker.wss.workspace.model.defaultRailMode
import com.visualtasker.wss.workspace.model.toRecorderWorldObservations
import com.visualtasker.wss.workspace.model.toRailProjection
import com.visualtasker.wss.workspace.model.toSurfaceMode
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
import com.visualtasker.wss.workspace.plugin.runtime.CustomChromeTabRegistration
import com.visualtasker.wss.workspace.plugin.runtime.CustomChromeTabSettings
import com.visualtasker.wss.workspace.plugin.runtime.ShizukuRegistration
import com.visualtasker.wss.workspace.plugin.runtime.TaskerPluginContract
import com.visualtasker.wss.workspace.plugin.runtime.TaskerPluginSettings
import com.visualtasker.wss.workspace.plugin.runtime.TaskerPluginSessionStore
import com.visualtasker.wss.workspace.plugin.runtime.TaskerRegistration
import com.visualtasker.wss.workspace.plugin.runtime.TermuxRegistration
import com.visualtasker.wss.workspace.vt2vt.VT2VT_MESSAGE_FORMAT
import com.visualtasker.wss.workspace.vt2vt.Vt2VtConnectionState
import com.visualtasker.wss.workspace.vt2vt.Vt2VtLanEndpoint
import com.visualtasker.wss.workspace.vt2vt.Vt2VtLanTransport
import com.visualtasker.wss.workspace.vt2vt.Vt2VtMessage
import com.visualtasker.wss.workspace.vt2vt.Vt2VtMessageCodec
import com.visualtasker.wss.workspace.vt2vt.Vt2VtMessageType
import com.visualtasker.wss.workspace.vt2vt.Vt2VtPeer
import com.visualtasker.wss.workspace.vt2vt.Vt2VtRole
import com.visualtasker.wss.workspace.vt2vt.Vt2VtTransport
import com.visualtasker.wss.workspace.vt2vt.Vt2VtUsbAdbBridge
import com.visualtasker.wss.workspace.vt2vt.Vt2VtUsbBridgeConfig
import com.visualtasker.wss.workspace.vt2vt.defaultVt2VtSession
import com.visualtasker.wss.workspace.vt2vt.detectVt2VtLanAddresses
import com.visualtasker.wss.workspace.vt2vt.logPayload
import com.visualtasker.wss.workspace.vt2vt.runtimePayload
import com.visualtasker.wss.workspace.vt2vt.selectionPayload
import com.visualtasker.wss.workspace.vt2vt.withLoopbackHello
import com.visualtasker.wss.workspace.vt2vt.workspacePayload
import com.visualtasker.wss.ui.theme.M3EColors
import com.visualtasker.wss.visual.debug.VisualSemanticsReporter
import com.visualtasker.wss.visual.interaction.DefaultEditorInteractionPolicy
import com.visualtasker.wss.visual.interaction.EditorActionDescriptor
import com.visualtasker.wss.visual.interaction.EditorActionId
import com.visualtasker.wss.visual.interaction.EditorProjection
import de.visualtasker.blockeditor.compose.host.BlockPaletteInsertMode
import de.visualtasker.blockeditor.compose.icons.CategoryIcons
import de.visualtasker.blockeditor.compose.theme.defaultBlockCategoryColor
import de.visualtasker.blockeditor.compose.theme.setBlockCategoryColorOverride
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.registry.BlockDefinition
import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
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
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.interaction.FlowInteractionAction
import de.visualtasker.flowchart.layout.FlowLayoutConfig
import de.visualtasker.flowchart.layout.FlowPinnedNodePolicy
import de.visualtasker.flowchart.serialization.FlowGraphJsonCodec
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
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
private const val WORKSPACE_MINIMIZED_DOCK_HEIGHT_DP = 44f
private const val BLOCKEDITOR_WORKSPACE_PREF_KEY = "blockeditor_workspace_json"
private const val BLOCKEDITOR_TEST_WORKSPACE_VERSION_PREF_KEY = "blockeditor_test_workspace_version"
private const val BLOCKEDITOR_PALETTE_INSERT_MODE_PREF_KEY = "blockeditor_palette_insert_mode"
private const val BLOCKEDITOR_MINIMAP_VISIBLE_PREF_KEY = "blockeditor_minimap_visible"
private const val FLOWCHART_MINIMAP_VISIBLE_PREF_KEY = "flowchart_minimap_visible"
private const val FLOWCHART_DATAFLOW_VISIBLE_PREF_KEY = "flowchart_dataflow_visible"
private const val FLOWCHART_RUNTIME_VISIBLE_PREF_KEY = "flowchart_runtime_visible"
private const val FLOWCHART_DIAGNOSTICS_VISIBLE_PREF_KEY = "flowchart_diagnostics_visible"
private const val CHROME_TAB_SHOW_TITLE_PREF_KEY = "chrometab_show_title"
private const val CHROME_TAB_SHARE_ENABLED_PREF_KEY = "chrometab_share_enabled"
private const val CHROME_TAB_DOWNLOAD_MENU_PREF_KEY = "chrometab_download_menu"
private const val CHROME_TAB_FAVORITE_MENU_PREF_KEY = "chrometab_favorite_menu"
private const val CHROME_TAB_BOTTOM_BAR_PREF_KEY = "chrometab_bottom_bar"
private const val CHROME_TAB_ACTION_ICON_PREF_KEY = "chrometab_action_icon"
private const val CHROME_TAB_CLOSE_ICON_PREF_KEY = "chrometab_close_icon"
private const val PANEL_RAIL_EXPANDED_PREF_PREFIX = "workspace_panel_rail_expanded:"
private const val TEXT_EDITOR_DRAFT_PREF_KEY = "workspace_text_editor_draft"
private const val TEXT_EDITOR_TEST_SCRIPT_VERSION_PREF_KEY = "workspace_text_editor_test_script_version"
private const val STEPPER_STATE_PREF_KEY = "workspace_stepper_state"
private const val M3_SHAPEMAKER_PACKAGE = "com.m3shapes.editor"

private data class StepperPanelState(
    val selectedStepId: String? = null,
    val replayIndex: Int = 0,
    val replayPositionMs: Long = 0L,
    val speed: Float = 1f,
    val railMode: RailMode = RailMode.Step,
    val scaleMode: RailScaleMode = RailScaleMode.Temporal,
    val timelineZoom: Float = 1f,
) {
    fun encode(): String = JSONObject()
        .put("selectedStepId", selectedStepId)
        .put("replayIndex", replayIndex)
        .put("replayPositionMs", replayPositionMs)
        .put("speed", speed.toDouble())
        .put("railMode", railMode.name)
        .put("scaleMode", scaleMode.name)
        .put("timelineZoom", timelineZoom.toDouble())
        .toString()

    companion object {
        fun decode(raw: String?): StepperPanelState = runCatching {
            if (raw.isNullOrBlank()) return@runCatching StepperPanelState()
            val root = JSONObject(raw)
            StepperPanelState(
                selectedStepId = root.optString("selectedStepId").takeIf { it.isNotBlank() },
                replayIndex = root.optInt("replayIndex", 0).coerceAtLeast(0),
                replayPositionMs = root.optLong("replayPositionMs", 0L).coerceAtLeast(0L),
                speed = root.optDouble("speed", 1.0).toFloat().coerceIn(0.2f, 4f),
                railMode = root.optString("railMode")
                    .takeIf { it.isNotBlank() }
                    ?.let { raw -> runCatching { RailMode.valueOf(raw) }.getOrNull() }
                    ?: RailMode.Step,
                scaleMode = root.optString("scaleMode")
                    .takeIf { it.isNotBlank() }
                    ?.let { raw -> runCatching { RailScaleMode.valueOf(raw) }.getOrNull() }
                    ?: RailScaleMode.Temporal,
                timelineZoom = root.optDouble("timelineZoom", 1.0).toFloat().coerceIn(0.5f, 4f),
            )
        }.getOrDefault(StepperPanelState())
    }
}

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

private data class ScreenshotCanvasBezier(
    val startX: Int,
    val startY: Int,
    val controlX: Int,
    val controlY: Int,
    val endX: Int,
    val endY: Int,
)

private data class ScreenshotCanvasSavedMarker(
    val id: String,
    val label: String,
    val assetId: String?,
    val assetLabel: String?,
    val region: ScreenshotCanvasRegion,
    val path: ScreenshotCanvasBezier? = null,
    val markerMode: ScreenshotCanvasMarkerMode,
    val matchKind: ScreenshotCanvasMatchKind,
    val processingMode: ScreenshotCanvasProcessingMode,
    val threshold: Float,
    val rotationDegrees: Float,
    val matchReadMe: String,
    val colourHex: String,
    val updatedAt: Long,
)

private enum class ScreenshotCanvasMarkerMode { Template, Region, Point, Swipe, Spline, Path }

private enum class ScreenshotCanvasDrawTool { Line, Circle, Curve, Spline, Path, Polygon, Text, Image, Shape }

private enum class FloatingOverlayTarget {
    Panel,
    Toolbar,
    Inspector,
}

private enum class ScreenshotCanvasMatchKind { OCR, OCV }

private enum class ScreenshotCanvasProcessingMode { Original, Grayscale, HighContrast, Edge, Inverse }

private enum class ScreenshotCanvasOverlayHandle {
    ScanType,
    Close,
    ResizeBottomLeft,
    ResizeBottomRight,
    PathStart,
    PathControl,
    PathEnd,
    None,
}

private enum class ScreenshotCanvasTouchMode {
    Create,
    Move,
    MovePath,
    MovePathStart,
    MovePathControl,
    MovePathEnd,
    ResizeBottomLeft,
    ResizeBottomRight,
    TapHandle,
    PointTap,
}

private data class ScreenshotCanvasGestureSetup(
    val mode: ScreenshotCanvasTouchMode,
    val createAnchorImage: Offset,
    val regionStart: ScreenshotCanvasRegion?,
    val pathStart: ScreenshotCanvasBezier? = null,
    val handle: ScreenshotCanvasOverlayHandle,
)

private class ScreenshotViewState {
    var selectedAssetId by mutableStateOf<String?>(null)
    var assetRevision by mutableIntStateOf(0)
    var zoom by mutableFloatStateOf(1f)
    var selectedRegion by mutableStateOf<ScreenshotCanvasRegion?>(null)
    var selectedPath by mutableStateOf<ScreenshotCanvasBezier?>(null)
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
    var selectedCanvasSourceId by mutableStateOf<String?>(null)
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
    var multiMarkerMode by mutableStateOf(false)
    var drawTool by mutableStateOf(ScreenshotCanvasDrawTool.Line)
    val savedMarkers = mutableStateListOf<ScreenshotCanvasSavedMarker>()
    var selectedSavedMarkerId by mutableStateOf<String?>(null)
}

private class VisionCropState {
    var visualTestScore by mutableStateOf<Float?>(null)
    var referenceMarkerId by mutableStateOf<String?>(null)
    var liveProcessingMode by mutableStateOf(ScreenshotCanvasProcessingMode.Original)
    var referenceProcessingMode by mutableStateOf(ScreenshotCanvasProcessingMode.Original)
    val observations = mutableStateListOf<WorldObservation>()
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
    var selectedPath: ScreenshotCanvasBezier?
        get() = screenshot.selectedPath
        set(value) {
            screenshot.selectedPath = value
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
    var multiMarkerMode: Boolean
        get() = marker.multiMarkerMode
        set(value) {
            marker.multiMarkerMode = value
        }
    var drawTool: ScreenshotCanvasDrawTool
        get() = marker.drawTool
        set(value) {
            marker.drawTool = value
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
    var selectedCanvasSourceId: String?
        get() = screenshot.selectedCanvasSourceId
        set(value) {
            screenshot.selectedCanvasSourceId = value
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
    var fontScale by remember { mutableStateOf(uiPrefs.getFloat("font_scale", 1f).coerceIn(0.75f, 1.6f)) }
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
    var flowchartDataFlowVisible by remember {
        mutableStateOf(uiPrefs.getBoolean(FLOWCHART_DATAFLOW_VISIBLE_PREF_KEY, true))
    }
    var flowchartRuntimeVisible by remember {
        mutableStateOf(uiPrefs.getBoolean(FLOWCHART_RUNTIME_VISIBLE_PREF_KEY, true))
    }
    var flowchartDiagnosticsVisible by remember {
        mutableStateOf(uiPrefs.getBoolean(FLOWCHART_DIAGNOSTICS_VISIBLE_PREF_KEY, true))
    }
    var chromeTabSettings by remember {
        mutableStateOf(
            CustomChromeTabSettings(
                showTitle = uiPrefs.getBoolean(CHROME_TAB_SHOW_TITLE_PREF_KEY, true),
                shareEnabled = uiPrefs.getBoolean(CHROME_TAB_SHARE_ENABLED_PREF_KEY, false),
                downloadMenuEnabled = uiPrefs.getBoolean(CHROME_TAB_DOWNLOAD_MENU_PREF_KEY, true),
                favoriteMenuEnabled = uiPrefs.getBoolean(CHROME_TAB_FAVORITE_MENU_PREF_KEY, true),
                bottomBarEnabled = uiPrefs.getBoolean(CHROME_TAB_BOTTOM_BAR_PREF_KEY, true),
                actionButtonIcon = uiPrefs.getString(CHROME_TAB_ACTION_ICON_PREF_KEY, "open") ?: "open",
                closeButtonIcon = uiPrefs.getString(CHROME_TAB_CLOSE_ICON_PREF_KEY, "close") ?: "close",
            )
        )
    }
    var stepperPanelState by remember(uiPrefs) {
        mutableStateOf(StepperPanelState.decode(uiPrefs.getString(STEPPER_STATE_PREF_KEY, null)))
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
    val runtimeDatastore = remember(context) {
        mutableStateMapOf<String, String>().apply {
            putAll(loadRuntimeDatastore(context))
        }
    }
    LaunchedEffect(runtimeDatastore) {
        snapshotFlow { runtimeDatastore.toMap() }
            .debounce(250)
            .collect { values ->
                persistRuntimeDatastore(context, values)
            }
    }
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
    val snackbarHostState = remember { SnackbarHostState() }
    val workspaceDryRunRuntime = remember { WorkspaceDryRunRuntime() }
    val workspaceBasicRuntime = remember(context, toneGenerator, vibrator, chromeTabSettings) {
        WorkspaceBasicRuntime(
            capabilityGate = { workspaceRuntimeCapabilityGate(context) },
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
                findTemplate = { name, threshold, _, searchRegion ->
                    val marker = workspaceCanvasState.savedMarkers.firstOrNull {
                        it.markerMode == ScreenshotCanvasMarkerMode.Template &&
                            it.matchesRuntimeTemplateName(name)
                    }
                    if (marker == null) {
                        null
                    } else {
                        val assets = loadScreenshotCanvasAssets(context)
                        val liveAsset = assets.firstOrNull { it.id == workspaceCanvasState.selectedAssetId } ?: assets.firstOrNull()
                        val referenceAsset = assets.firstOrNull { it.id == marker.assetId } ?: liveAsset
                        val liveBitmap = liveAsset?.file?.absolutePath?.let(::decodeScreenshotBitmap)
                        val referenceBitmap = referenceAsset?.file?.absolutePath?.let(::decodeScreenshotBitmap)
                        val liveRegion = searchRegion?.toScreenshotRegion() ?: marker.region
                        val score = compareScreenshotRegions(
                            liveBitmap = liveBitmap,
                            liveRegion = liveRegion,
                            liveMode = marker.processingMode,
                            referenceBitmap = referenceBitmap,
                            referenceRegion = marker.region,
                            referenceMode = marker.processingMode,
                        )
                        workspaceCanvasState.referenceMarkerId = marker.id
                        workspaceCanvasState.selectedSavedMarkerId = marker.id
                        workspaceCanvasState.selectedRegion = liveRegion
                        workspaceCanvasState.visualTestScore = score
                        score
                            ?.takeIf { it >= threshold }
                            ?.let {
                                RuntimeTemplateMatch(
                                    name = marker.label,
                                    region = liveRegion.toRuntimeRegion(),
                                    score = it,
                                )
                            }
                    }
                },
                markerSave = { name, region, mode, threshold ->
                    val now = System.currentTimeMillis()
                    val markerMode = markerModeFromRuntime(mode)
                    val selectedAsset = loadScreenshotCanvasAssets(context)
                        .firstOrNull { it.id == workspaceCanvasState.selectedAssetId }
                    val id = "runtime-marker:${name.trim().lowercase(Locale.ROOT)}"
                    val marker = ScreenshotCanvasSavedMarker(
                        id = id,
                        label = name.trim().ifBlank { "marker" },
                        assetId = selectedAsset?.id,
                        assetLabel = selectedAsset?.label,
                        region = region.toScreenshotRegion(),
                        path = null,
                        markerMode = markerMode,
                        matchKind = ScreenshotCanvasMatchKind.OCV,
                        processingMode = workspaceCanvasState.processingMode,
                        threshold = threshold,
                        rotationDegrees = workspaceCanvasState.rotationDegrees,
                        matchReadMe = "runtime",
                        colourHex = workspaceCanvasState.colourHex,
                        updatedAt = now,
                    )
                    val index = workspaceCanvasState.savedMarkers.indexOfFirst { it.id == id || it.label.equals(marker.label, ignoreCase = true) }
                    if (index >= 0) {
                        workspaceCanvasState.savedMarkers[index] = marker
                    } else {
                        workspaceCanvasState.savedMarkers.add(0, marker)
                    }
                    workspaceCanvasState.selectedSavedMarkerId = marker.id
                    workspaceCanvasState.selectedRegion = marker.region
                    workspaceCanvasState.selectedPath = marker.path
                    true
                },
                markerLoad = { name ->
                    val marker = workspaceCanvasState.savedMarkers.firstOrNull { it.label.equals(name, ignoreCase = true) || it.id == name }
                    if (marker != null) {
                        workspaceCanvasState.selectedSavedMarkerId = marker.id
                        workspaceCanvasState.selectedAssetId = marker.assetId ?: workspaceCanvasState.selectedAssetId
                        workspaceCanvasState.selectedRegion = marker.region
                        workspaceCanvasState.selectedPath = marker.path
                        workspaceCanvasState.markerMode = marker.markerMode
                    }
                    marker?.region?.toRuntimeRegion()
                },
                markerDelete = { name ->
                    val before = workspaceCanvasState.savedMarkers.size
                    workspaceCanvasState.savedMarkers.removeAll { it.label.equals(name, ignoreCase = true) || it.id == name }
                    workspaceCanvasState.selectedSavedMarkerId = workspaceCanvasState.savedMarkers.firstOrNull()?.id
                    before != workspaceCanvasState.savedMarkers.size
                },
                templateDefine = { name, region, processing ->
                    val selectedAsset = loadScreenshotCanvasAssets(context)
                        .firstOrNull { it.id == workspaceCanvasState.selectedAssetId }
                    val marker = ScreenshotCanvasSavedMarker(
                        id = "runtime-template:${name.trim().lowercase(Locale.ROOT)}",
                        label = name.trim().ifBlank { "template" },
                        assetId = selectedAsset?.id,
                        assetLabel = selectedAsset?.label,
                        region = region.toScreenshotRegion(),
                        path = null,
                        markerMode = ScreenshotCanvasMarkerMode.Template,
                        matchKind = ScreenshotCanvasMatchKind.OCV,
                        processingMode = processingModeFromRuntime(processing),
                        threshold = workspaceCanvasState.threshold,
                        rotationDegrees = workspaceCanvasState.rotationDegrees,
                        matchReadMe = "runtime-template",
                        colourHex = workspaceCanvasState.colourHex,
                        updatedAt = System.currentTimeMillis(),
                    )
                    val index = workspaceCanvasState.savedMarkers.indexOfFirst { it.id == marker.id || it.label.equals(marker.label, ignoreCase = true) }
                    if (index >= 0) {
                        workspaceCanvasState.savedMarkers[index] = marker
                    } else {
                        workspaceCanvasState.savedMarkers.add(0, marker)
                    }
                    workspaceCanvasState.referenceMarkerId = marker.id
                    workspaceCanvasState.selectedRegion = marker.region
                    true
                },
                templateCompare = { name, region, processing ->
                    val marker = workspaceCanvasState.savedMarkers.firstOrNull {
                        it.markerMode == ScreenshotCanvasMarkerMode.Template &&
                            (it.label.equals(name, ignoreCase = true) || it.id == name)
                    }
                    if (marker == null) {
                        null
                    } else {
                        val assets = loadScreenshotCanvasAssets(context)
                        val liveAsset = assets.firstOrNull { it.id == workspaceCanvasState.selectedAssetId } ?: assets.firstOrNull()
                        val referenceAsset = assets.firstOrNull { it.id == marker.assetId } ?: liveAsset
                        val liveBitmap = liveAsset?.file?.absolutePath?.let(::decodeScreenshotBitmap)
                        val referenceBitmap = referenceAsset?.file?.absolutePath?.let(::decodeScreenshotBitmap)
                        val mode = processingModeFromRuntime(processing)
                        val score = compareScreenshotRegions(
                            liveBitmap = liveBitmap,
                            liveRegion = region.toScreenshotRegion(),
                            liveMode = mode,
                            referenceBitmap = referenceBitmap,
                            referenceRegion = marker.region,
                            referenceMode = marker.processingMode,
                        )
                        workspaceCanvasState.referenceMarkerId = marker.id
                        workspaceCanvasState.selectedRegion = region.toScreenshotRegion()
                        workspaceCanvasState.visualTestScore = score
                        score
                    }
                },
                datastorePut = { key, value ->
                    runtimeDatastore[key] = value
                },
                datastoreGet = { key ->
                    runtimeDatastore[key]
                },
                chromeTabCommand = { command, args ->
                    val status = CustomChromeTabRegistration.inspect(context)
                    when (command) {
                        "chrometab.issupported" -> RuntimeAdapterResult(
                            success = status.supported,
                            message = "ChromeTab.isSupported = ${status.supported}; ${status.summary}",
                            warning = !status.supported,
                        )
                        "chrometab.open",
                        "chrometab.create",
                        -> {
                            val url = args.firstOrNull().orEmpty().ifBlank { "https://example.com" }
                            val result = CustomChromeTabRegistration.open(context, url, chromeTabSettings)
                            RuntimeAdapterResult(result.success, result.message, result.warning)
                        }
                        "chrometab.bind",
                        "chrometab.maylaunchurl",
                        -> RuntimeAdapterResult(
                            success = status.supported,
                            message = "$command vorbereitet: ${status.summary}",
                            warning = !status.supported,
                        )
                        "chrometab.close",
                        "chrometab.requestpostmessagechannel",
                        "chrometab.postmessage",
                        "chrometab.validaterelationship",
                        -> RuntimeAdapterResult(
                            success = false,
                            message = "$command benötigt den nächsten CustomTabs Session-/Relationship-Slice.",
                        )
                        else -> RuntimeAdapterResult(false, "$command ist im CustomChromeTab-Adapter unbekannt.")
                    }
                },
                taskerCommand = { command, args ->
                    val status = TaskerRegistration.inspect(context)
                    when (command) {
                        "tasker.isinstalled" -> RuntimeAdapterResult(true, "Tasker.isInstalled = ${status.installed}", warning = false)
                        "tasker.isenabled" -> RuntimeAdapterResult(status.available, "Tasker.isEnabled = ${status.available}; ${status.summary}", warning = !status.available)
                        "tasker.lastresult" -> {
                            val runId = args.firstOrNull()?.trim()?.trim('"')?.takeIf { it.isNotBlank() }
                            val result = TaskerPluginSessionStore.lastResult(context, runId)
                            RuntimeAdapterResult(
                                success = result != null,
                                message = result?.let {
                                    "Tasker.lastResult${runId?.let { id -> "($id)" }.orEmpty()} = ${it.status}; runId=${it.runId}; slot=${it.eventSlot}; event=${it.eventName}; message=${it.message}; ordinal=${it.ordinal}"
                                } ?: "Tasker.lastResult${runId?.let { id -> "($id)" }.orEmpty()} = leer",
                                warning = result == null,
                            )
                        }
                        "tasker.error" -> {
                            val runId = args.firstOrNull()?.trim()?.trim('"')?.takeIf { it.isNotBlank() }
                            val error = TaskerPluginSessionStore.lastError(context, runId)
                            RuntimeAdapterResult(
                                success = true,
                                message = error?.let {
                                    "Tasker.error${runId?.let { id -> "($id)" }.orEmpty()} = ${it.message}; runId=${it.runId}; slot=${it.eventSlot}; event=${it.eventName}; ordinal=${it.ordinal}"
                                } ?: "Tasker.error${runId?.let { id -> "($id)" }.orEmpty()} = kein Fehler",
                                warning = error != null,
                            )
                        }
                        "tasker.action",
                        "tasker.runtask",
                        -> {
                            val request = parseTaskerRunRequest(args)
                            val result = TaskerRegistration.runTask(
                                context = context,
                                taskName = request.taskName,
                                parameters = request.parameters,
                                variables = request.variables,
                            )
                            RuntimeAdapterResult(result.success, result.message, warning = !result.success)
                        }
                        "tasker.getvariable",
                        "tasker.clearvariable",
                        "tasker.getvariables",
                        "tasker.pluginaction",
                        "tasker.profileenable",
                        "tasker.profiledisable",
                        "tasker.profiletoggle",
                        "tasker.profilestate",
                        "tasker.cancel",
                        -> RuntimeAdapterResult(
                            success = false,
                            message = "$command registriert, Tasker Runner/Receiver-Vertrag noch nicht verbunden (${args.joinToString(",")})",
                        )
                        else -> RuntimeAdapterResult(false, "$command ist im Tasker-Adapter unbekannt.")
                    }
                },
                shizukuCommand = { command, args ->
                    val status = ShizukuRegistration.inspect(context)
                    when (command) {
                        "shizuku.isinstalled" -> RuntimeAdapterResult(true, "Shizuku.isInstalled = ${status.installed}", warning = false)
                        "shizuku.isavailable" -> RuntimeAdapterResult(status.available, "Shizuku.isAvailable = ${status.available}")
                        "shizuku.permissionstate" -> RuntimeAdapterResult(status.permissionGranted, "Shizuku.permissionState = ${if (status.permissionGranted) "granted" else "missing"}")
                        "shizuku.requestpermission" -> {
                            val requested = ShizukuRegistration.requestPermissionIfPossible()
                            if (!requested) context.safeStartActivity(ShizukuRegistration.settingsIntent(status))
                            RuntimeAdapterResult(status.available, if (requested) "Shizuku Permission angefragt" else "Shizuku Permission/Settings geöffnet")
                        }
                        "shizuku.getuid" -> RuntimeAdapterResult(status.available, "Shizuku.getUid = ${status.uid ?: -1}")
                        "shizuku.exec",
                        "shizuku.shell",
                        -> {
                            val shellLine = args.firstOrNull().orEmpty()
                            val result = ShizukuRegistration.runShell(context, shellLine)
                            RuntimeAdapterResult(
                                success = result.success,
                                message = result.summary,
                                warning = !result.success,
                            )
                        }
                        "shizuku.systemservice" -> {
                            val serviceName = args.firstOrNull().orEmpty().trimLiteral()
                            val result = ShizukuRegistration.runShell(
                                context,
                                "service check ${serviceName.shellQuote()}",
                            )
                            RuntimeAdapterResult(
                                success = result.success,
                                message = result.summary,
                                warning = !result.success,
                            )
                        }
                        "shizuku.call" -> {
                            val serviceName = args.getOrNull(0).orEmpty().trimLiteral()
                            val method = args.getOrNull(1).orEmpty().trimLiteral()
                            val tail = args.getOrNull(2).orEmpty().trim()
                            val callArgs = if (tail.startsWith("[") && tail.endsWith("]")) {
                                splitTopLevel(tail.removeSurrounding("[", "]"))
                                    .joinToString(" ") { it.trimLiteral().shellQuote() }
                            } else {
                                tail.trimLiteral()
                            }
                            val shellLine = buildString {
                                append("service call ")
                                append(serviceName.shellQuote())
                                append(' ')
                                append(method.shellQuote())
                                if (callArgs.isNotBlank()) {
                                    append(' ')
                                    append(callArgs)
                                }
                            }
                            val result = ShizukuRegistration.runShell(context, shellLine)
                            RuntimeAdapterResult(
                                success = result.success,
                                message = result.summary,
                                warning = !result.success,
                            )
                        }
                        "shizuku.binduserservice",
                        "shizuku.unbinduserservice",
                        -> RuntimeAdapterResult(
                            success = false,
                            message = "$command registriert, Binder-Ausführung noch nicht verbunden (${args.joinToString(",")})",
                        )
                        else -> RuntimeAdapterResult(false, "$command ist im Shizuku-Adapter unbekannt.")
                    }
                },
                termuxCommand = { command, args ->
                    val status = TermuxRegistration.inspect(context)
                    when (command) {
                        "termux.isinstalled" -> RuntimeAdapterResult(true, "Termux.isInstalled = ${status.installed}", warning = false)
                        "termux.canruncommands" -> RuntimeAdapterResult(status.canRunCommands, "Termux.canRunCommands = ${status.canRunCommands}; ${status.summary}")
                        "termux.get" -> {
                            val key = args.firstOrNull().orEmpty()
                            val value = when (key.lowercase()) {
                                "installed" -> status.installed.toString()
                                "apiinstalled" -> status.apiInstalled.toString()
                                "canruncommands" -> status.canRunCommands.toString()
                                "permission" -> if (status.runCommandPermissionGranted) "granted" else "missing"
                                "summary", "" -> status.summary
                                else -> ""
                            }
                            RuntimeAdapterResult(true, "Termux.get($key) = $value", warning = false)
                        }
                        "termux.shell" -> {
                            val commandLine = args.firstOrNull().orEmpty()
                            val result = TermuxRegistration.runCommand(
                                context,
                                TermuxRegistration.buildShellRequest(commandLine),
                            )
                            RuntimeAdapterResult(result.success, result.message, warning = !result.success)
                        }
                        "termux.run" -> {
                            val path = args.firstOrNull().orEmpty()
                            val commandArgs = args.drop(1)
                            val result = TermuxRegistration.runCommand(
                                context,
                                TermuxRegistration.buildRunRequest(path, commandArgs),
                            )
                            RuntimeAdapterResult(result.success, result.message, warning = !result.success)
                        }
                        "termux.api" -> {
                            val apiCommand = args.firstOrNull().orEmpty().ifBlank { "battery-status" }
                            val result = TermuxRegistration.runCommand(
                                context,
                                TermuxRegistration.buildApiRequest(apiCommand, args.drop(1)),
                            )
                            RuntimeAdapterResult(
                                success = result.success,
                                message = if (status.apiInstalled) result.message else "${result.message}; Termux:API App nicht erkannt",
                                warning = !result.success || !status.apiInstalled,
                            )
                        }
                        "termux.writestdin",
                        "termux.cancel",
                        -> RuntimeAdapterResult(
                            success = false,
                            message = "$command braucht Termux Result-/Session-Bridge und ist als nächster Slice vorgesehen.",
                        )
                        else -> RuntimeAdapterResult(false, "$command ist im Termux-Adapter unbekannt.")
                    }
                },
                scrcpyCommand = { command, args ->
                    val status = Vt2VtUsbAdbBridge.detect(context)
                    val endpoint = Vt2VtUsbBridgeConfig().endpoint
                    when (command) {
                        "scrcpy.hostavailable" -> RuntimeAdapterResult(status.bridgeReady, "scrcpy.hostAvailable = ${status.bridgeReady}; ${status.summary}")
                        "scrcpy.devices" -> RuntimeAdapterResult(status.bridgeReady, "scrcpy.devices via USB/ADB: ${status.connectedPeripheralCount}")
                        "scrcpy.connect",
                        "scrcpy.start",
                        -> RuntimeAdapterResult(status.bridgeReady, "scrcpy USB/ADB Bridge ${if (status.bridgeReady) "bereit" else "nicht bereit"}: ${endpoint.host}:${endpoint.port}")
                        "scrcpy.disconnect",
                        "scrcpy.stop",
                        -> RuntimeAdapterResult(true, "scrcpy Bridge-Session lokal freigegeben", warning = false)
                        "scrcpy.isrunning" -> RuntimeAdapterResult(status.bridgeReady, "scrcpy.isRunning = ${status.bridgeReady}")
                        "scrcpy.get" -> RuntimeAdapterResult(true, "scrcpy.get(${args.firstOrNull().orEmpty()}) = ${status.summary}", warning = false)
                        "scrcpy.key",
                        "scrcpy.text",
                        "scrcpy.scroll",
                        "scrcpy.setclipboard",
                        "scrcpy.setscreenpower",
                        "scrcpy.rotate",
                        "scrcpy.touch",
                        -> RuntimeAdapterResult(
                            success = status.bridgeReady,
                            message = "$command an USB/ADB Bridge vorbereitet (${args.joinToString(",")})",
                            warning = !status.bridgeReady,
                        )
                        else -> RuntimeAdapterResult(false, "$command ist im scrcpy/ADB-Adapter unbekannt.")
                    }
                },
            ),
        )
    }
    val workspaceSyncGuard = remember { WorkspaceSyncGuard() }
    var workspaceDryRunSequence by remember { mutableStateOf(0L) }
    var workspaceDryRunResult by remember { mutableStateOf<EmscriptDryRunResult?>(null) }
    var workspaceDryRunRevision by remember { mutableStateOf<Long?>(null) }
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
    val emscriptApplyGuard = remember { EmscriptApplyGuard() }
    fun replaceWorkflowStateFromJson(updated: String, source: String) {
        workflowState = WorkspaceWorkflowState.fromSerialized(updated, mutationSource = source)
        flowRuntimeSnapshot = null
        workspaceDryRunResult = null
        workspaceDryRunRevision = null
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
    fun applyLoadedEmscriptScript(name: String, content: String) {
        emscriptSession = emscriptSession
            .selectTab(EmscriptEditorSession.MANUAL_TAB_ID)
            .updateManualContent(content)
        uiPrefs.edit().putString(TEXT_EDITOR_DRAFT_PREF_KEY, content).apply()
        when (val preview = emscriptApplyGuard.preview(content, workspaceId = "workflow-main")) {
            is EmscriptApplyGuardResult.Success -> {
                applyWorkspaceJsonChange(preview.serializedWorkspaceJson, WORKFLOW_SOURCE_EMSCRIPT_APPLY)
                studioLogStore.append(
                    level = StudioLogLevel.INFO,
                    source = "EMSCRIPT",
                    message = "Script geladen und angewendet",
                    details = "Name=$name, Blöcke=${preview.blockCount}, Roots=${preview.rootCount}",
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "emscript:file-loaded-applied:$name"
                )
            }
            is EmscriptApplyGuardResult.Failure -> {
                studioLogStore.append(
                    level = StudioLogLevel.ERROR,
                    source = "EMSCRIPT",
                    message = "Script geladen, Apply fehlgeschlagen",
                    details = "Name=$name\n${preview.message}",
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "emscript:file-loaded-apply-failed:$name"
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow {
            emscriptSession.tabs
                .firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                ?.content
                .orEmpty()
        }
            .debounce(650)
            .collect { content ->
                if (content.isBlank()) return@collect
                when (val preview = emscriptApplyGuard.preview(content, workspaceId = "workflow-main")) {
                    is EmscriptApplyGuardResult.Success -> {
                        if (preview.serializedWorkspaceJson != workflowState.serializedJson) {
                            applyWorkspaceJsonChange(preview.serializedWorkspaceJson, WORKFLOW_SOURCE_EMSCRIPT_APPLY)
                            studioLogStore.append(
                                level = StudioLogLevel.DEBUG,
                                source = "EMSCRIPT",
                                message = "Texteditor automatisch synchronisiert",
                                details = "Blöcke=${preview.blockCount}, Roots=${preview.rootCount}",
                                documentRevision = workflowState.revision.toLong(),
                                groupKey = "emscript:auto-apply:${preview.serializedWorkspaceJson.hashCode()}"
                            )
                        }
                    }
                    is EmscriptApplyGuardResult.Failure -> Unit
                }
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
        val visibleInsertPosition = if (selectedFlowchartNodeForInsert == null) {
            activeFlowchartSessionState.value
                ?.controller
                ?.snapshot()
                ?.view
                ?.viewport
                ?.let { viewport ->
                    FlowPoint(
                        x = (320.0 - viewport.pan.x) / viewport.zoom,
                        y = (260.0 - viewport.pan.y) / viewport.zoom,
                    )
                }
        } else {
            null
        }
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.AddNode(
                definitionId = definitionId,
                afterNodeId = selectedFlowchartNodeForInsert,
                position = visibleInsertPosition,
            ),
            selectedFlowchartNodeForInsert?.let { "${it.value}:insert:$definitionId" } ?: definitionId,
        )
    }
    val addSceneSaveNode: (ScreenshotCanvasSavedMarker) -> Unit = { marker ->
        applyFlowchartMutation(
            FlowchartWorkspaceMutation.AddNode(
                definitionId = "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}scene.save",
                afterNodeId = selectedFlowchartNodeForInsert,
                initialFields = mapOf(
                    "command" to "sceneSave",
                    "args" to sceneSaveArgsForMarker(marker),
                ),
            ),
            "scene-save:${marker.id}",
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
    fun focusRailTraceFromBlockId(blockId: String) {
        val result = workspaceDryRunResult ?: return
        val event = result.eventsForRailTrace().lastOrNull { it.blockId == blockId } ?: return
        val updated = stepperPanelState.copy(
            selectedStepId = "dry-run-${event.index}",
            replayIndex = (event.index - 1).coerceAtLeast(0),
            replayPositionMs = event.index * 180L,
        )
        if (updated != stepperPanelState) {
            stepperPanelState = updated
            persistStepperState(uiPrefs, updated)
        }
    }
    fun focusRailTraceFromFlowEdge(edgeId: FlowEdgeId) {
        val result = workspaceDryRunResult ?: return
        val edge = workflowState.flowchartProjection.graph.edges.firstOrNull { it.id == edgeId } ?: return
        val sourceBlockId = edge.sourceNodeId.value.removePrefix("block:").takeIf { it != edge.sourceNodeId.value }
        val targetBlockId = edge.targetNodeId.value.removePrefix("block:").takeIf { it != edge.targetNodeId.value }
        if (sourceBlockId == null && targetBlockId == null) return
        val event = result.eventsForRailTrace().lastOrNull { dryRunEvent ->
            (sourceBlockId == null || dryRunEvent.edgeSourceBlockId == sourceBlockId) &&
                (targetBlockId == null || dryRunEvent.edgeTargetBlockId == targetBlockId)
        } ?: return
        val updated = stepperPanelState.copy(
            selectedStepId = "dry-run-${event.index}",
            replayIndex = (event.index - 1).coerceAtLeast(0),
            replayPositionMs = event.index * 180L,
        )
        if (updated != stepperPanelState) {
            stepperPanelState = updated
            persistStepperState(uiPrefs, updated)
        }
    }
    fun focusBlockFromFlowNode(nodeId: FlowNodeId) {
        val blockId = nodeId.value.removePrefix("block:").takeIf { it != nodeId.value } ?: return
        val session = activeBlockEditorSessionState.value ?: return
        val target = BlockId(blockId).takeIf { it in session.controller.document.blocks } ?: return
        focusRailTraceFromBlockId(blockId)
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
        selectedFlowchartNodeForInsert = target
        selectedFlowchartNodeId = target
        selectedFlowchartEdgeId = null
        session.controller.dispatch(FlowInteractionAction.SelectNode(target))
        focusRailTraceFromBlockId(blockId.value)
        studioLogStore.append(
            level = StudioLogLevel.DEBUG,
            source = "BLOCKEDITOR",
            message = "Flowchart auf Block fokussiert",
            details = "Block=${blockId.value}, Node=${target.value}",
            documentRevision = workflowState.revision.toLong(),
            groupKey = "blockeditor:flow-focus:${blockId.value}"
        )
    }
    fun focusRuntimeSnapshotActiveNode(snapshot: FlowRuntimeSnapshot) {
        val target = snapshot.activeNodeId ?: return
        val session = activeFlowchartSessionState.value
        if (session?.graphDocument?.nodes?.none { it.id == target } == true) return
        selectedFlowchartNodeForInsert = target
        selectedFlowchartNodeId = target
        selectedFlowchartEdgeId = null
        session?.controller?.dispatch(FlowInteractionAction.SelectNode(target))
        focusBlockFromFlowNode(target)
    }
    fun renderWorkspaceDryRunStep(stepIndex: Int) {
        val result = workspaceDryRunResult ?: return
        val eventCount = dryRunEventCount(result)
        workspaceDryRunStepIndex = stepIndex.coerceIn(0, eventCount)
        syncRailTraceToDryRunStep(
            result = result,
            stepIndex = workspaceDryRunStepIndex,
            currentState = stepperPanelState,
            onStateChanged = { updated ->
                stepperPanelState = updated
                persistStepperState(uiPrefs, updated)
            },
        )
        workspaceDryRunSequence += 1
        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = workflowState.irGraph,
            graph = workflowState.flowchartProjection.graph,
            result = result,
            sequence = workspaceDryRunSequence,
            maxEventIndex = workspaceDryRunStepIndex,
        )
        flowRuntimeSnapshot = snapshot
        focusRuntimeSnapshotActiveNode(snapshot)
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
        workspaceDryRunRevision = workflowState.revision.toLong()
        workspaceDryRunStepIndex = dryRunEventCount(result)
        syncRailTraceToDryRunStep(
            result = result,
            stepIndex = workspaceDryRunStepIndex,
            currentState = stepperPanelState,
            onStateChanged = { updated ->
                stepperPanelState = updated
                persistStepperState(uiPrefs, updated)
            },
        )
        workspaceDryRunSequence += 1
        val snapshot = EmscriptDryRunFlowRuntimeMapper.map(
            irGraph = workflowState.irGraph,
            graph = workflowState.flowchartProjection.graph,
            result = result,
            sequence = workspaceDryRunSequence,
        )
        flowRuntimeSnapshot = snapshot
        focusRuntimeSnapshotActiveNode(snapshot)
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
        val capabilityReport = workspaceRuntimeCapabilityGate(context).inspect(workflowState.document)
        val accessibilityBlockers = capabilityReport.accessibilityBlockedCommands()
        if (accessibilityBlockers.isNotEmpty() && !VisualTaskerAccessibilityService.isConnected()) {
            studioLogStore.append(
                level = StudioLogLevel.ERROR,
                source = source,
                message = "Live-Run wartet auf Accessibility",
                details = "Blockierte Commands: ${accessibilityBlockers.joinToString(", ")}",
                documentRevision = workflowState.revision.toLong(),
                groupKey = "workspace:basic-run:accessibility-required"
            )
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Accessibility aktivieren für: ${accessibilityBlockers.take(3).joinToString(", ")}",
                    actionLabel = "Öffnen",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.safeStartActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
            return
        }
        coroutineScope.launch {
            studioLogStore.append(
                level = StudioLogLevel.INFO,
                source = source,
                message = "Workspace Basic-Run gestartet",
                details = capabilityReport.summary,
                documentRevision = workflowState.revision.toLong(),
                groupKey = "workspace:basic-run:start:${workflowState.revision}"
            )
            val result = workspaceBasicRuntime.run(workflowState.document)
            workspaceDryRunResult = result
            workspaceDryRunStepIndex = dryRunEventCount(result)
            syncRailTraceToDryRunStep(
                result = result,
                stepIndex = workspaceDryRunStepIndex,
                currentState = stepperPanelState,
                onStateChanged = { updated ->
                    stepperPanelState = updated
                    persistStepperState(uiPrefs, updated)
                },
            )
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
    val scaledDensity = remember(baseDensity, uiScale, fontScale) {
        Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * fontScale
        )
    }
    val density = scaledDensity.density
    val gridSizeDp = if (useLargeGrid) GridSystem.GRID_SIZE_DP_LARGE else GridSystem.GRID_SIZE_DP_SMALL
    val workspaceTopGuardPx = (WORKSPACE_TOP_BAR_HEIGHT_DP + WORKSPACE_PANEL_MARGIN_DP) * density
    val workspaceBottomGuardPx = if (dockAtTop) 0f else WORKSPACE_MINIMIZED_DOCK_HEIGHT_DP * density

    val demoRecorderSteps = remember {
        mutableStateListOf(
            RecorderStepUi("step-1", "Start App", "launch", StepStatus.Recorded, timestampMs = 0, durationMs = 900, activityName = "Launcher"),
            RecorderStepUi("step-2", "Tippe Login", "tap", StepStatus.Edited, timestampMs = 1200, durationMs = 180, activityName = "LoginActivity"),
            RecorderStepUi("step-3", "Warte auf Element", "wait", StepStatus.Invalid, timestampMs = 1900, durationMs = 1400, activityName = "LoginActivity"),
            RecorderStepUi("step-4", "Bestaetigen", "tap", StepStatus.Executed, timestampMs = 3600, durationMs = 220, activityName = "DashboardActivity")
        )
    }
    var recordingSessions by remember(context) { mutableStateOf(RecordingEventStore.recordingSessions(context)) }
    var selectedRecordingSessionPath by remember(context) { mutableStateOf(recordingSessions.firstOrNull()?.path) }
    var recordingSteps by remember(context) {
        mutableStateOf(RecordingEventStore.recordingStepsFor(selectedRecordingSessionPath))
    }
    LaunchedEffect(context) {
        var lastRecordingSignature = RecordingEventStore.latestRecordingFile(context)?.recordingSignature().orEmpty()
        while (true) {
            delay(1_000)
            val latestFile = RecordingEventStore.latestRecordingFile(context)
            val currentSignature = latestFile?.recordingSignature().orEmpty()
            if (currentSignature != lastRecordingSignature) {
                lastRecordingSignature = currentSignature
                recordingSessions = RecordingEventStore.recordingSessions(context)
                if (selectedRecordingSessionPath == null || recordingSessions.none { it.path == selectedRecordingSessionPath }) {
                    selectedRecordingSessionPath = recordingSessions.firstOrNull()?.path
                }
                recordingSteps = RecordingEventStore.recordingStepsFor(selectedRecordingSessionPath)
            }
        }
    }
    LaunchedEffect(selectedRecordingSessionPath) {
        recordingSteps = RecordingEventStore.recordingStepsFor(selectedRecordingSessionPath)
    }
    // Workspace shell stays truth-neutral: runtime projection wins over external projection, then demo data.
    val dryRunRecorderSteps = remember(workspaceDryRunResult, workspaceDryRunRevision, workflowState.revision) {
        if (workspaceDryRunRevision == workflowState.revision.toLong()) {
            workspaceDryRunResult?.toRecorderSteps().orEmpty()
        } else {
            emptyList()
        }
    }
    val projectedSteps = when (stepperPanelState.railMode.toSurfaceMode()) {
        RailSurfaceMode.Run -> when {
            dryRunRecorderSteps.isNotEmpty() -> dryRunRecorderSteps
            recorderStepsProjection != null -> recorderStepsProjection.invoke()
            else -> demoRecorderSteps
        }
        RailSurfaceMode.Records -> when {
            recordingSteps.isNotEmpty() -> recordingSteps
            recorderStepsProjection != null -> recorderStepsProjection.invoke()
            else -> demoRecorderSteps
        }
        RailSurfaceMode.WatchDog -> when {
            recorderStepsProjection != null -> recorderStepsProjection.invoke()
            recordingSteps.isNotEmpty() -> recordingSteps
            else -> demoRecorderSteps
        }
    }
    var selectedRailTraceStepId by remember { mutableStateOf<String?>(null) }
    val selectedRailTraceStep = remember(projectedSteps, selectedRailTraceStepId) {
        selectedRailTraceStepId?.let { id -> projectedSteps.firstOrNull { it.id == id } }
    }
    val recorderObservations = remember(projectedSteps) {
        projectedSteps.toRecorderWorldObservations()
    }
    val workspaceCanvasAssets = remember(workspaceCanvasState.assetRevision) {
        loadScreenshotCanvasAssets(context)
    }
    LaunchedEffect(context, workspaceCanvasState) {
        var lastSignature = screenshotCanvasAssetSignature(context)
        while (true) {
            delay(1_000)
            val currentSignature = screenshotCanvasAssetSignature(context)
            if (currentSignature != lastSignature) {
                lastSignature = currentSignature
                workspaceCanvasState.refreshAssets()
            }
        }
    }

    val bridge = remember(actionSink, recorderStepsProjection, demoRecorderSteps, dryRunRecorderSteps, workspaceDryRunResult, workflowState.revision) {
        object : PanelActionSink {
            override fun onPanelAction(action: PanelAction) {
                if (action is PanelAction.SelectStep && workspaceDryRunResult != null) {
                    action.stepId.dryRunEventIndexOrNull()?.let { eventIndex ->
                        renderWorkspaceDryRunStep(eventIndex)
                    }
                }
                if (action is PanelAction.SelectStep) {
                    selectedRailTraceStepId = action.stepId
                }
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
                x = if (type == PanelType.Screenshot || type == PanelType.Marker || type == PanelType.Vision || type == PanelType.Datastore || type == PanelType.M3Director || type == PanelType.Vt2Vt) 32f else 96f,
                y = max(96f, workspaceTopGuardPx),
                width = if (type == PanelType.Screenshot || type == PanelType.Marker || type == PanelType.Vision || type == PanelType.Datastore || type == PanelType.M3Director || type == PanelType.Vt2Vt) {
                    ((surfaceSize.width / density) - 64f).coerceAtLeast(PANEL_DEFAULT_W)
                } else {
                    PANEL_DEFAULT_W
                },
                height = if (type == PanelType.Screenshot || type == PanelType.Marker || type == PanelType.Vision || type == PanelType.Datastore || type == PanelType.M3Director || type == PanelType.Vt2Vt) {
                    ((surfaceSize.height - workspaceTopGuardPx - workspaceBottomGuardPx) / density).coerceAtLeast(PANEL_DEFAULT_H)
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
    val launchFloatingOverlay: (FloatingOverlayTarget) -> Unit = { target ->
        if (!Settings.canDrawOverlays(context)) {
            context.safeStartActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                )
            )
            studioLogStore.append(
                level = StudioLogLevel.WARNING,
                source = "WORKSPACE",
                message = "Overlay-Berechtigung erforderlich",
                details = "Bitte Berechtigung aktivieren und erneut auswaehlen.",
                groupKey = "workspace:overlay-permission-required",
            )
        } else {
            val action = when (target) {
                FloatingOverlayTarget.Panel -> StudioOverlayService.ACTION_SHOW_FLOATING_PANEL
                FloatingOverlayTarget.Toolbar -> StudioOverlayService.ACTION_SHOW_FLOATING_TOOLBAR
                FloatingOverlayTarget.Inspector -> StudioOverlayService.ACTION_SHOW_FLOATING_INSPECTOR
            }
            context.startService(
                Intent(context, StudioOverlayService::class.java).apply {
                    this.action = action
                }
            )
            studioLogStore.append(
                level = StudioLogLevel.INFO,
                source = "WORKSPACE",
                message = "Floating Overlay gestartet",
                details = target.name,
                groupKey = "workspace:overlay-started:${target.name}",
            )
        }
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
        fontScale,
        blockPaletteInsertMode,
        blockEditorMiniMapVisible,
        flowchartMiniMapVisible,
        flowchartDataFlowVisible,
        flowchartRuntimeVisible,
        flowchartDiagnosticsVisible,
        chromeTabSettings,
    ) {
        uiPrefs.edit()
            .putBoolean("hide_system_bars", hideSystemBars)
            .putBoolean("dock_top", dockAtTop)
            .putBoolean("grid_large", useLargeGrid)
            .putBoolean("snap_enabled", snapEnabled)
            .putFloat("ui_scale", uiScale)
            .putFloat("font_scale", fontScale)
            .putString(BLOCKEDITOR_PALETTE_INSERT_MODE_PREF_KEY, blockPaletteInsertMode.name)
            .putBoolean(BLOCKEDITOR_MINIMAP_VISIBLE_PREF_KEY, blockEditorMiniMapVisible)
            .putBoolean(FLOWCHART_MINIMAP_VISIBLE_PREF_KEY, flowchartMiniMapVisible)
            .putBoolean(FLOWCHART_DATAFLOW_VISIBLE_PREF_KEY, flowchartDataFlowVisible)
            .putBoolean(FLOWCHART_RUNTIME_VISIBLE_PREF_KEY, flowchartRuntimeVisible)
            .putBoolean(FLOWCHART_DIAGNOSTICS_VISIBLE_PREF_KEY, flowchartDiagnosticsVisible)
            .putBoolean(CHROME_TAB_SHOW_TITLE_PREF_KEY, chromeTabSettings.showTitle)
            .putBoolean(CHROME_TAB_SHARE_ENABLED_PREF_KEY, chromeTabSettings.shareEnabled)
            .putBoolean(CHROME_TAB_DOWNLOAD_MENU_PREF_KEY, chromeTabSettings.downloadMenuEnabled)
            .putBoolean(CHROME_TAB_FAVORITE_MENU_PREF_KEY, chromeTabSettings.favoriteMenuEnabled)
            .putBoolean(CHROME_TAB_BOTTOM_BAR_PREF_KEY, chromeTabSettings.bottomBarEnabled)
            .putString(CHROME_TAB_ACTION_ICON_PREF_KEY, chromeTabSettings.actionButtonIcon)
            .putString(CHROME_TAB_CLOSE_ICON_PREF_KEY, chromeTabSettings.closeButtonIcon)
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
                projectName = emscriptFileManager.currentName.ifBlank { "draft" },
                canRunLive = workflowState.document.blocks.isNotEmpty(),
                onRunLive = { runCurrentWorkspaceLive("WORKSPACE") },
                onPause = {
                    studioLogStore.append(
                        level = StudioLogLevel.WARNING,
                        source = "WORKSPACE",
                        message = "Pause angefordert",
                        details = "Runtime-Pause wird im nächsten Job-Controller-Slice hart verdrahtet.",
                        documentRevision = workflowState.revision.toLong(),
                        groupKey = "workspace:runtime-pause-requested"
                    )
                },
                onStop = {
                    workspaceDryRunResult = null
                    workspaceDryRunStepIndex = 0
                    flowRuntimeSnapshot = null
                    studioLogStore.append(
                        level = StudioLogLevel.INFO,
                        source = "WORKSPACE",
                        message = "Runtime-Anzeige gestoppt",
                        details = "Dry/Live Snapshot geleert.",
                        documentRevision = workflowState.revision.toLong(),
                        groupKey = "workspace:runtime-stop"
                    )
                },
                onLoadProject = {
                    replaceWorkflowStateFromJson(loadBlockEditorWorkspaceJson(uiPrefs), "workspace:project-load")
                    studioLogStore.append(
                        level = StudioLogLevel.INFO,
                        source = "WORKSPACE",
                        message = "Projekt geladen",
                        details = emscriptFileManager.currentName.ifBlank { "draft" },
                        documentRevision = workflowState.revision.toLong(),
                        groupKey = "workspace:project-load"
                    )
                },
                onSaveProject = {
                    uiPrefs.edit()
                        .putString(BLOCKEDITOR_WORKSPACE_PREF_KEY, workflowState.serializedJson)
                        .putString(TEXT_EDITOR_DRAFT_PREF_KEY, emscriptSession.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }?.content.orEmpty())
                        .apply()
                    sessionStore.save(WorkspaceSessionSnapshot(panels = panels.toList()))
                    studioLogStore.append(
                        level = StudioLogLevel.INFO,
                        source = "WORKSPACE",
                        message = "Projekt gespeichert",
                        details = "Workspace=${workflowState.serializedJson.length} Zeichen",
                        documentRevision = workflowState.revision.toLong(),
                        groupKey = "workspace:project-save"
                    )
                },
                onClearProject = {
                    applyWorkspaceJsonChange(
                        WorkspaceSerializer.serialize(WorkspaceBootstrap.starter()),
                        "workspace:project-clear"
                    )
                    emscriptSession = emscriptSession.updateGeneratedFromBlocks("// Leerer Workspace")
                },
                onClearCanvas = {
                    workspaceCanvasState.selectedAssetId = null
                    workspaceCanvasState.selectedRegion = null
                    workspaceCanvasState.selectedPath = null
                    workspaceCanvasState.referenceMarkerId = null
                    workspaceCanvasState.visualTestScore = null
                    studioLogStore.append(
                        level = StudioLogLevel.INFO,
                        source = "CANVAS",
                        message = "Workspace Canvas geleert",
                        groupKey = "workspace:canvas-clear"
                    )
                },
                onOpenSettings = { showSettingsSheet = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(900_000f)
            )

        panels.sortedBy { it.zIndex }.forEach { panel ->
            if (panel.minimized) return@forEach
            key(panel.id) {
                val panelTopPx = max(panel.y, workspaceTopGuardPx)
                val maxWidthDp = (surfaceSize.width.coerceAtLeast(0) / density).toInt().coerceAtLeast(PANEL_MIN_W.toInt())
                val maxHeightDp = ((surfaceSize.height - workspaceTopGuardPx - workspaceBottomGuardPx).coerceAtLeast(0f) / density).toInt().coerceAtLeast(PANEL_MIN_H.toInt())
                val panelWidthPx = panel.width * density
                val panelHeightPx = panel.height * density
                val panelMaxXPx = max(0f, surfaceSize.width - panelWidthPx)
                val panelMaxYPx = max(workspaceTopGuardPx, surfaceSize.height - workspaceBottomGuardPx - panelHeightPx)
                val snapTargets = panelSnapTargets(
                    panels = panels,
                    activePanel = panel,
                    density = density,
                    surfaceSize = surfaceSize,
                    topGuardPx = workspaceTopGuardPx,
                    bottomGuardPx = workspaceBottomGuardPx,
                )
                val isRailTracePanel = panel.type == PanelType.RecorderSteps
                val isBlockEditorPanel = panel.type == PanelType.BlockEditor
                val isFlowchartPanel = panel.type == PanelType.Flowchart
                val isScreenshotPanel = panel.type == PanelType.Screenshot || panel.type == PanelType.Marker || panel.type == PanelType.Vision || panel.type == PanelType.Datastore || panel.type == PanelType.M3Director || panel.type == PanelType.Vt2Vt
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
                    maxPositionXPx = panelMaxXPx,
                    maxPositionYPx = panelMaxYPx,
                    snapTargetXPx = snapTargets.x,
                    snapTargetYPx = snapTargets.y,
                    showRail = true,
                    railExpandedOverride = railExpanded,
                    onRailExpandedChange = { expanded ->
                        railExpanded = expanded
                        persistPanelRailExpanded(uiPrefs, panel.id, expanded)
                    },
                    showDefaultRailIcons = !(isRailTracePanel || isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel || isScreenshotPanel),
                    showRailColorPicker = !(isRailTracePanel || isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel || isScreenshotPanel),
	                    railExpandedWidth = when {
                        isRailTracePanel -> 240.dp
	                        isBlockEditorPanel -> 300.dp
	                        isFlowchartPanel -> 300.dp
                        isLogConsolePanel -> 220.dp
                        isEmscriptPanel -> 240.dp
                        isScreenshotPanel -> 220.dp
                        else -> 186.dp
                    },
                    railExpandedFillHeight = isRailTracePanel || isBlockEditorPanel || isFlowchartPanel || isLogConsolePanel || isEmscriptPanel || isScreenshotPanel,
                    headerLeadingContent = {
                        PanelTypeSwitchButton(
                            currentType = panel.type,
                            onSelect = { type ->
                                updatePanel(panels, panel.id) {
                                    it.copy(
                                        type = type,
                                        title = displayNameForPanelType(type),
                                        accentColor = defaultAccentForPanelType(type),
                                    )
                                }
                                focusedPanelId = panel.id
                                bridge.onPanelAction(PanelAction.OpenPanel(type))
                            },
                        )
                    },
                    compactRailContent = { onExpandRequested ->
                        when {
                            isScreenshotPanel -> ScreenshotCanvasCompactRail(
                                state = screenshotCanvasState,
                                onExpandRequested = onExpandRequested,
                            )
                            isRailTracePanel -> RailTraceCompactRail(
                                onSave = {
                                    persistStepperState(uiPrefs, stepperPanelState)
                                    sessionStore.save(WorkspaceSessionSnapshot(panels = panels.toList()))
                                    studioLogStore.append(
                                        level = StudioLogLevel.INFO,
                                        source = "RAILTRACE",
                                        message = "RailTrace Workspace gespeichert",
                                        details = "Mode=${stepperPanelState.railMode.name}, Scale=${stepperPanelState.scaleMode.name}, Zoom=${stepperPanelState.timelineZoom}",
                                        documentRevision = workflowState.revision.toLong(),
                                        groupKey = "railtrace:state-saved"
                                    )
                                },
                                onUndo = { undoWorkspaceChange() },
                                onRedo = { redoWorkspaceChange() },
                                onRunDry = { runCurrentWorkspaceDryRun("RAILTRACE") },
                                onRunLive = { runCurrentWorkspaceLive("RAILTRACE") },
                                onStepBack = { renderWorkspaceDryRunStep(workspaceDryRunStepIndex - 1) },
                                onStepForward = {
                                    if (workspaceDryRunResult == null) {
                                        runCurrentWorkspaceDryRun("RAILTRACE")
                                    } else {
                                        renderWorkspaceDryRunStep(workspaceDryRunStepIndex + 1)
                                    }
                                },
                                canRunDry = workflowState.emscriptProjection.isSuccess,
                                canRunLive = workspaceRuntimeCapabilityGate(context).inspect(workflowState.document).realRunAllowed,
                                canStepBack = workspaceDryRunResult != null && workspaceDryRunStepIndex > 0,
                                canStepForward = workspaceDryRunStepIndex < dryRunEventCount(workspaceDryRunResult),
                                onZoomIn = {
                                    stepperPanelState = stepperPanelState.copy(
                                        timelineZoom = (stepperPanelState.timelineZoom * 1.25f).coerceAtMost(4f)
                                    )
                                    persistStepperState(uiPrefs, stepperPanelState)
                                },
                                onZoomOut = {
                                    stepperPanelState = stepperPanelState.copy(
                                        timelineZoom = (stepperPanelState.timelineZoom / 1.25f).coerceAtLeast(0.5f)
                                    )
                                    persistStepperState(uiPrefs, stepperPanelState)
                                },
                                onExpandRequested = onExpandRequested,
                            )
                            isBlockEditorPanel -> BlockEditorCompactCategoryRail(
                                session = blockEditorSessionState.value,
                                onExpandRequested = onExpandRequested,
                                onSave = {
                                    blockEditorSessionState.value?.let { persistBlockEditorSession(uiPrefs, it) }
                                },
                            )
                            isFlowchartPanel -> FlowchartCompactActionRail(
                                session = flowchartSessionState.value,
                                panelSizePx = IntSize(
                                    width = (panel.width * density).roundToInt().coerceAtLeast(1),
                                    height = (panel.height * density).roundToInt().coerceAtLeast(1),
                                ),
                                selectedNodeId = selectedFlowchartNodeId,
                                selectedEdgeId = selectedFlowchartEdgeId,
                                onExpandRequested = onExpandRequested,
                                onSave = {
                                    flowchartSessionState.value?.let { persistFlowchartViewSession(uiPrefs, it) }
                                },
                                dataFlowVisible = flowchartDataFlowVisible,
                                runtimeVisible = flowchartRuntimeVisible,
                                diagnosticsVisible = flowchartDiagnosticsVisible,
                                onDataFlowToggle = { flowchartDataFlowVisible = !flowchartDataFlowVisible },
                                onRuntimeToggle = { flowchartRuntimeVisible = !flowchartRuntimeVisible },
                                onDiagnosticsToggle = { flowchartDiagnosticsVisible = !flowchartDiagnosticsVisible },
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
                                onCompileCheck = {
                                    val manual = emscriptSession.tabs
                                        .firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                        ?.content
                                        .orEmpty()
                                    val result = EmscriptParserSlice().parse(manual)
                                    if (result.isSuccess) {
                                        studioLogStore.append(
                                            level = StudioLogLevel.INFO,
                                            source = "EMSCRIPT",
                                            message = "Compile Check erfolgreich",
                                            details = "Top-Level-Statements=${result.ir?.statements?.size ?: 0}",
                                            documentRevision = workflowState.revision.toLong(),
                                            groupKey = "emscript:compile-check:success"
                                        )
                                    } else {
                                        val details = result.issues.joinToString(separator = "\n") { issue ->
                                            "${issue.line}:${issue.column} ${issue.message}"
                                        }
                                        studioLogStore.append(
                                            level = StudioLogLevel.ERROR,
                                            source = "EMSCRIPT",
                                            message = "Compile Check fehlgeschlagen",
                                            details = details,
                                            documentRevision = workflowState.revision.toLong(),
                                            groupKey = "emscript:compile-check:failure"
                                        )
                                    }
                                },
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
                                    applyLoadedEmscriptScript(key, content)
                                },
                                canCompile = emscriptSession.tabs
                                    .firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                                    ?.content
                                    ?.isNotBlank() == true,
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
                            isRailTracePanel -> RailTraceExpandedRail(
                                state = stepperPanelState,
                                onModeChange = { mode ->
                                    val updated = stepperPanelState.copy(railMode = mode)
                                    stepperPanelState = updated
                                    persistStepperState(uiPrefs, updated)
                                },
                                onScaleChange = { scale ->
                                    val updated = stepperPanelState.copy(scaleMode = scale)
                                    stepperPanelState = updated
                                    persistStepperState(uiPrefs, updated)
                                },
                                onZoomChange = { zoom ->
                                    val updated = stepperPanelState.copy(timelineZoom = zoom.coerceIn(0.5f, 4f))
                                    stepperPanelState = updated
                                    persistStepperState(uiPrefs, updated)
                                },
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
                                    applyLoadedEmscriptScript(name, content)
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
                            val maxPanelY = max(workspaceTopGuardPx, surfaceSize.height - workspaceBottomGuardPx - panelHeightPx)
                            it.copy(
                                x = newPos.x.coerceIn(0f, max(0f, surfaceSize.width - panelWidthPx)),
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
                        focusedFlowchartNodeId = selectedFlowchartNodeId,
                        focusedFlowchartEdgeId = selectedFlowchartEdgeId,
                        blockEditorMiniMapVisible = blockEditorMiniMapVisible,
                        flowchartMiniMapVisible = flowchartMiniMapVisible,
                        flowchartDataFlowVisible = flowchartDataFlowVisible,
                        flowchartRuntimeVisible = flowchartRuntimeVisible,
                        flowchartDiagnosticsVisible = flowchartDiagnosticsVisible,
                        stepperPanelState = stepperPanelState,
                        selectedRailTraceStep = selectedRailTraceStep,
                        recorderObservations = recorderObservations,
                        recordingSessions = recordingSessions,
                        selectedRecordingSessionPath = selectedRecordingSessionPath,
                        onFlowchartDataFlowVisibleChange = { flowchartDataFlowVisible = it },
                        onFlowchartRuntimeVisibleChange = { flowchartRuntimeVisible = it },
                        onFlowchartDiagnosticsVisibleChange = { flowchartDiagnosticsVisible = it },
                        onRecordingSessionSelected = { path -> selectedRecordingSessionPath = path },
                        onStepperStateChange = { state ->
                            stepperPanelState = state
                            persistStepperState(uiPrefs, state)
                        },
                        onStepperStateSave = { state ->
                            stepperPanelState = state
                            persistStepperState(uiPrefs, state)
                            studioLogStore.append(
                                level = StudioLogLevel.INFO,
                                source = "RAILTRACE",
                                message = "RailTrace-Stand gespeichert",
                                details = "Index ${state.replayIndex}, Position ${state.replayPositionMs} ms, Speed ${state.speed.formatSpeedStep()}x",
                                documentRevision = workflowState.revision.toLong(),
                                groupKey = "stepper:state-saved"
                            )
                        },
                        onEmscriptSessionChange = { updated ->
                            emscriptSession = updated
                            val manual = updated.tabs.firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                            if (manual != null) {
                                uiPrefs.edit().putString(TEXT_EDITOR_DRAFT_PREF_KEY, manual.content).apply()
                            }
                        },
                        logStore = studioLogStore,
                        logConsoleState = logConsoleState,
                        runtimeDatastore = runtimeDatastore,
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
                        activeRuntimeStepIndex = workspaceDryRunResult?.let {
                            (workspaceDryRunStepIndex - 1).coerceIn(0, (dryRunEventCount(it) - 1).coerceAtLeast(0))
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
                            nodeId?.let(::focusBlockFromFlowNode)
                            edgeId?.let(::focusRailTraceFromFlowEdge)
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
                        onSceneMarkerSaved = addSceneSaveNode,
                        onWorkspaceJsonChange = applyWorkspaceJsonChange
                    )
                }
            }
        }

        MinimizedDock(
            panels = panels,
            focusedPanelId = focusedPanelId,
            onSelect = { id ->
                updatePanel(panels, id) { it.copy(minimized = false, zIndex = nextZ++) }
                focusedPanelId = id
            },
            modifier = Modifier
                .align(if (dockAtTop) Alignment.TopEnd else Alignment.BottomStart)
                .padding(
                    start = 0.dp,
                    end = if (dockAtTop) 12.dp else 84.dp,
                    top = if (dockAtTop) (WORKSPACE_TOP_BAR_HEIGHT_DP + WORKSPACE_PANEL_MARGIN_DP).dp else 0.dp,
                    bottom = 0.dp
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
                    icon = Icons.Default.Visibility,
                    label = "Floating Shot",
                    color = M3EColors.Amber
                ) {
                    launchFloatingOverlay(FloatingOverlayTarget.Toolbar)
                },
                FabAction(
                    icon = Icons.Default.AutoAwesomeMosaic,
                    label = "Auto anordnen",
                    color = M3EColors.Oceanneon
                ) {
                    autoArrangePanels(
                        panels = panels,
                        surfaceSize = surfaceSize,
                        focusedPanelId = focusedPanelId,
                        topInsetPx = workspaceTopGuardPx,
                        bottomInsetPx = workspaceBottomGuardPx,
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
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(1_000_000f)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (WORKSPACE_TOP_BAR_HEIGHT_DP + 8).dp)
                .zIndex(1_000_001f)
        )
        }
    }

    if (showAddPanelDialog) {
        AddPanelDialog(
            onSelect = { type ->
                openPanel(type)
                showAddPanelDialog = false
            },
            onLaunchFloatingOverlay = { target ->
                launchFloatingOverlay(target)
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
            fontScale = fontScale,
            onFontScaleChange = { fontScale = it.coerceIn(0.75f, 1.6f) },
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
            chromeTabSettings = chromeTabSettings,
            onChromeTabSettingsChange = { chromeTabSettings = it },
            onResetPanels = {
                panels.clear()
                panels.addAll(defaultPanels())
                nextId = (panels.maxOfOrNull { it.id.removePrefix("panel-").toIntOrNull() ?: 0 } ?: 0) + 1
                nextZ = (panels.maxOfOrNull { it.zIndex } ?: 0) + 1
                focusedPanelId = panels.maxByOrNull { it.zIndex }?.id.orEmpty()
            },
            onSaveLayout = {
                sessionStore.save(WorkspaceSessionSnapshot(panels = panels.toList()))
                studioLogStore.append(
                    level = StudioLogLevel.INFO,
                    source = "WORKSPACE",
                    message = "Layout gespeichert",
                    details = "Panels=${panels.size}",
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "workspace:layout-save"
                )
            },
            onDeleteLayout = {
                sessionStore.clear()
                panels.clear()
                panels.addAll(defaultPanels())
                nextId = (panels.maxOfOrNull { it.id.removePrefix("panel-").toIntOrNull() ?: 0 } ?: 0) + 1
                nextZ = (panels.maxOfOrNull { it.zIndex } ?: 0) + 1
                focusedPanelId = panels.maxByOrNull { it.zIndex }?.id.orEmpty()
                studioLogStore.append(
                    level = StudioLogLevel.WARNING,
                    source = "WORKSPACE",
                    message = "Layout gelöscht",
                    details = "Standard-Layout wiederhergestellt.",
                    documentRevision = workflowState.revision.toLong(),
                    groupKey = "workspace:layout-delete"
                )
            },
            onAutoArrange = {
                autoArrangePanels(
                    panels = panels,
                    surfaceSize = surfaceSize,
                    focusedPanelId = focusedPanelId,
                    topInsetPx = workspaceTopGuardPx,
                    bottomInsetPx = workspaceBottomGuardPx,
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
    focusedFlowchartNodeId: FlowNodeId? = null,
    focusedFlowchartEdgeId: FlowEdgeId? = null,
    blockEditorMiniMapVisible: Boolean,
    flowchartMiniMapVisible: Boolean,
    flowchartDataFlowVisible: Boolean,
    flowchartRuntimeVisible: Boolean,
    flowchartDiagnosticsVisible: Boolean,
    stepperPanelState: StepperPanelState = StepperPanelState(),
    selectedRailTraceStep: RecorderStepUi? = null,
    recorderObservations: List<WorldObservation> = emptyList(),
    recordingSessions: List<RecordingSessionUi> = emptyList(),
    selectedRecordingSessionPath: String? = null,
    onFlowchartDataFlowVisibleChange: (Boolean) -> Unit = {},
    onFlowchartRuntimeVisibleChange: (Boolean) -> Unit = {},
    onFlowchartDiagnosticsVisibleChange: (Boolean) -> Unit = {},
    onStepperStateChange: (StepperPanelState) -> Unit = {},
    onStepperStateSave: (StepperPanelState) -> Unit = {},
    onRecordingSessionSelected: (String) -> Unit = {},
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
    activeRuntimeStepIndex: Int? = null,
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
    onSceneMarkerSaved: (ScreenshotCanvasSavedMarker) -> Unit = {},
    runtimeDatastore: Map<String, String> = emptyMap(),
    onWorkspaceJsonChange: (String, String) -> Unit
) {
    val context = LocalContext.current
    when (panel.type) {
        PanelType.RecorderSteps -> RecorderStepsPanel(
            steps = steps,
            actionSink = actionSink,
            activeRuntimeStepIndex = activeRuntimeStepIndex,
            initialState = stepperPanelState,
            timelineZoom = stepperPanelState.timelineZoom,
            onTimelineZoomChange = { zoom -> onStepperStateChange(stepperPanelState.copy(timelineZoom = zoom)) },
            onViewStateChange = onStepperStateChange,
            onSaveState = onStepperStateSave,
            recordingSessions = recordingSessions,
            selectedRecordingSessionPath = selectedRecordingSessionPath,
            onRecordingSessionSelected = onRecordingSessionSelected,
        )
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
            focusedNodeId = focusedFlowchartNodeId,
            focusedEdgeId = focusedFlowchartEdgeId,
            stepLabel = dryRunStepLabel,
            showMiniMap = flowchartMiniMapVisible,
            dataFlowVisible = flowchartDataFlowVisible,
            runtimeLayerVisible = flowchartRuntimeVisible,
            diagnosticsVisible = flowchartDiagnosticsVisible,
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
            onDataFlowVisibleChange = onFlowchartDataFlowVisibleChange,
            onRuntimeVisibleChange = onFlowchartRuntimeVisibleChange,
            onDiagnosticsVisibleChange = onFlowchartDiagnosticsVisibleChange,
            onSessionReady = onFlowchartSessionReady
        )
        PanelType.TextEditor,
        PanelType.Emscript -> {
            val capabilityReport = workspaceRuntimeCapabilityGate(context).inspect(workflowState.document)
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
                onWorkspaceDryRun = { onRunWorkspaceDry() },
                onLiveRun = { onRunWorkspaceLive() },
                canLiveRun = capabilityReport.realRunAllowed,
                liveRunStatus = capabilityReport.summary,
                activeSourceLine = activeRuntimeSourceLine(
                    workflowState = workflowState,
                    runtimeSnapshot = flowRuntimeSnapshot,
                    visibleScriptText = emscriptSession.tabs
                        .firstOrNull { it.id == EmscriptEditorSession.MANUAL_TAB_ID }
                        ?.content
                        .orEmpty(),
                    projectedScriptText = latestEmscriptProjected,
                ),
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
                val capabilityReport = workspaceRuntimeCapabilityGate(context).inspect(workflowState.document)
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
            baseResources = workflowState.resources,
            recorderObservations = recorderObservations,
            markerPanel = false,
        )
        PanelType.Marker -> MarkerCanvasPanel(
            state = screenshotCanvasState,
            assets = screenshotAssets,
            baseResources = workflowState.resources,
            selectedRailTraceStep = selectedRailTraceStep,
            recorderObservations = recorderObservations,
            onSceneMarkerSaved = onSceneMarkerSaved,
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
            datastore = runtimeDatastore,
            recorderObservations = recorderObservations,
        )
        PanelType.M3Director -> VisualAssetManagerPanel(
            workflowState = workflowState,
            screenshotState = screenshotCanvasState,
            screenshotAssets = screenshotAssets,
        )
        PanelType.Vt2Vt -> Vt2VtPanel(
            workflowState = workflowState,
            logStore = logStore,
            flowRuntimeSnapshot = flowRuntimeSnapshot,
            focusedFlowchartNodeId = focusedFlowchartNodeId,
            focusedFlowchartEdgeId = focusedFlowchartEdgeId,
            activeRuntimeStepIndex = activeRuntimeStepIndex,
        )
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
        ScreenshotMarkerModeRailButton("Spline", Icons.Default.Polyline, state.markerMode == ScreenshotCanvasMarkerMode.Spline) {
            state.markerMode = ScreenshotCanvasMarkerMode.Spline
        }
        ScreenshotMarkerModeRailButton("Path", Icons.Default.Polyline, state.markerMode == ScreenshotCanvasMarkerMode.Path) {
            state.markerMode = ScreenshotCanvasMarkerMode.Path
        }
        ScreenshotMarkerModeRailButton("Multi", Icons.Default.AddCircle, state.multiMarkerMode) {
            state.multiMarkerMode = !state.multiMarkerMode
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
                    state.selectedPath = marker.path
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
        ScreenshotCanvasMarkerMode.Spline -> Icons.Default.Polyline
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
        ScreenshotCanvasMarkerMode.Spline -> {
            val path = marker.path ?: defaultBezierForRegion(region, region.x + region.width + 1, region.y + region.height + 1)
            "spline(${path.startX}, ${path.startY}, ${path.endX}, ${path.endY}, ${path.controlX}, ${path.controlY})"
        }
        ScreenshotCanvasMarkerMode.Path -> {
            val path = marker.path ?: defaultBezierForRegion(region, region.x + region.width + 1, region.y + region.height + 1)
            "path(${path.startX}, ${path.startY}, ${path.controlX}, ${path.controlY}, ${path.endX}, ${path.endY})"
        }
    }
}

private fun buildCanvasWorldview(
    assets: List<ScreenshotCanvasAsset>,
    markers: List<ScreenshotCanvasSavedMarker>,
    baseResources: WorkspaceResourceBundle = WorkspaceResourceBundle(),
    visionObservations: List<WorldObservation> = emptyList(),
    recorderObservations: List<WorldObservation> = emptyList(),
): WorldviewDocument {
    val assetSizes = assets.associate { asset ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(asset.file.absolutePath, options)
        asset.id to (options.outWidth.coerceAtLeast(1) to options.outHeight.coerceAtLeast(1))
    }
    val assetResources = assets.map { asset ->
        WorkspaceResource(
            id = "screenshot:${asset.id.stableResourceSuffix()}",
            kind = WorkspaceResourceKind.Screenshot,
            label = asset.label,
            uri = asset.file.absolutePath,
            mimeType = "image/png",
            packageName = asset.app,
            activityClass = asset.scene,
            tags = setOf("canvas", "scene"),
            metadata = mapOf(
                "date" to asset.dateLabel,
                "source" to "canvas",
            ),
            updatedAtEpochMs = asset.file.lastModified(),
        )
    }
    val markerResources = markers.mapNotNull { marker ->
        val size = marker.assetId?.let(assetSizes::get) ?: assetSizes.values.firstOrNull()
        val referenceWidth = size?.first ?: return@mapNotNull null
        val referenceHeight = size.second
        val region = runCatching {
            WorkspaceRegionBounds(
                left = (marker.region.x.toFloat() / referenceWidth).coerceIn(0f, 1f),
                top = (marker.region.y.toFloat() / referenceHeight).coerceIn(0f, 1f),
                right = ((marker.region.x + marker.region.width).toFloat() / referenceWidth).coerceIn(0f, 1f),
                bottom = ((marker.region.y + marker.region.height).toFloat() / referenceHeight).coerceIn(0f, 1f),
            )
        }.getOrNull()
        val point = if (marker.markerMode == ScreenshotCanvasMarkerMode.Point) {
            WorkspacePointBounds(
                x = ((marker.region.x + marker.region.width / 2f) / referenceWidth).coerceIn(0f, 1f),
                y = ((marker.region.y + marker.region.height / 2f) / referenceHeight).coerceIn(0f, 1f),
            )
        } else {
            null
        }
        WorkspaceResource(
            id = marker.id.stableMarkerResourceId(marker.markerMode),
            kind = when (marker.markerMode) {
                ScreenshotCanvasMarkerMode.Template -> WorkspaceResourceKind.Template
                ScreenshotCanvasMarkerMode.Region -> WorkspaceResourceKind.Region
                ScreenshotCanvasMarkerMode.Point,
                ScreenshotCanvasMarkerMode.Swipe,
                ScreenshotCanvasMarkerMode.Spline,
                ScreenshotCanvasMarkerMode.Path,
                -> WorkspaceResourceKind.Marker
            },
            label = marker.label,
            pluginOwner = "visualtasker.canvas",
            uri = marker.assetId,
            mimeType = if (marker.markerMode == ScreenshotCanvasMarkerMode.Template) "image/png" else null,
            markerMode = when (marker.markerMode) {
                ScreenshotCanvasMarkerMode.Template,
                ScreenshotCanvasMarkerMode.Region,
                -> WorkspaceMarkerMode.Region
                ScreenshotCanvasMarkerMode.Point -> WorkspaceMarkerMode.Point
                ScreenshotCanvasMarkerMode.Swipe -> WorkspaceMarkerMode.Swipe
                ScreenshotCanvasMarkerMode.Spline,
                ScreenshotCanvasMarkerMode.Path,
                -> WorkspaceMarkerMode.Path
            },
            region = region,
            point = point,
            referenceWidthPx = referenceWidth,
            referenceHeightPx = referenceHeight,
            tags = setOf("canvas", "marker", marker.markerMode.name.lowercase()),
            metadata = mapOf(
                "assetLabel" to (marker.assetLabel ?: "-"),
                "matchKind" to marker.matchKind.name,
                "processingMode" to marker.processingMode.name,
                "threshold" to marker.threshold.toString(),
                "rotationDegrees" to marker.rotationDegrees.toString(),
                "export" to markerExportCode(marker),
            ),
            updatedAtEpochMs = marker.updatedAt,
        )
    }
    val merged = (baseResources.resources + assetResources + markerResources)
        .distinctBy { it.id }
    val projected = WorldviewDocument.fromResources(baseResources.copy(resources = merged))
    val mergedObservations = (projected.observations + visionObservations + recorderObservations)
        .distinctBy { it.id }
        .sortedBy { it.id }
    return projected.copy(
        observations = mergedObservations,
        revision = projected.revision + visionObservations.size + recorderObservations.size,
    )
}

private fun String.stableResourceSuffix(): String =
    hashCode().toString().replace("-", "n")

private fun String.stableMarkerResourceId(mode: ScreenshotCanvasMarkerMode): String {
    val normalized = lowercase()
        .replace(Regex("[^a-z0-9._:-]"), "-")
        .trim('-')
        .ifBlank { stableResourceSuffix() }
    val prefix = when (mode) {
        ScreenshotCanvasMarkerMode.Template -> "template"
        ScreenshotCanvasMarkerMode.Region -> "region"
        ScreenshotCanvasMarkerMode.Point,
        ScreenshotCanvasMarkerMode.Swipe,
        ScreenshotCanvasMarkerMode.Spline,
        ScreenshotCanvasMarkerMode.Path,
        -> "marker"
    }
    return if (normalized.startsWith("$prefix:")) normalized else "$prefix:$normalized"
}

private fun ScreenshotCanvasUiState.toCanvasObservationFilters(): CanvasObservationFilters =
    CanvasObservationFilters(
        showAccessibility = showAccessibilityNodes,
        showClickable = !filterHideClickable,
        showVisible = !filterHideInvisible,
        showFocusable = !filterHideNonFocusable,
        showOcr = showOcrNodes,
        showOcv = showVisionTemplateNodes,
        showYolo = showYoloNodes,
        showDom = showDomNodes,
        showMarkers = showMarkers,
    )

private fun ScreenshotCanvasUiState.selectCanvasObservation(item: CanvasObservationItem) {
    selectedCanvasSourceId = item.sourceId
    savedMarkers.firstOrNull { marker ->
        marker.id == item.sourceId || marker.id.stableMarkerResourceId(marker.markerMode) == item.sourceId
    }?.let { marker ->
        selectedSavedMarkerId = marker.id
        selectedRegion = marker.region
        selectedPath = marker.path
        markerMode = marker.markerMode
        matchKind = marker.matchKind
        processingMode = marker.processingMode
        templateName = marker.label
        colourHex = marker.colourHex
        threshold = marker.threshold
    } ?: run {
        templateName = item.label
    }
}

private fun ScreenshotCanvasUiState.recordVisionObservation(
    asset: ScreenshotCanvasAsset?,
    bitmap: android.graphics.Bitmap?,
    referenceMarker: ScreenshotCanvasSavedMarker?,
    score: Float?,
) {
    val region = selectedRegion ?: return
    val imageWidth = bitmap?.width ?: return
    val imageHeight = bitmap.height
    val bounds = region.toWorldviewRect(imageWidth, imageHeight) ?: return
    val normalizedReferenceId = referenceMarker?.id?.stableMarkerResourceId(referenceMarker.markerMode)
    val seed = listOfNotNull(
        asset?.id,
        referenceMarker?.id,
        region.x.toString(),
        region.y.toString(),
        region.width.toString(),
        region.height.toString(),
        liveProcessingMode.name,
        referenceProcessingMode.name,
        matchKind.name,
    ).joinToString(":")
    val observation = CanvasVisionObservationFactory.create(
        idSeed = seed,
        label = referenceMarker?.label ?: templateName,
        matchKind = matchKind.name,
        processingMode = liveProcessingMode.name,
        bounds = bounds,
        score = score ?: 0f,
        threshold = threshold,
        assetId = asset?.id,
        referenceId = normalizedReferenceId,
        observedAtEpochMs = System.currentTimeMillis(),
    )
    vision.observations.removeAll { it.id == observation.id }
    vision.observations.add(observation)
    selectedCanvasSourceId = observation.id
}

private fun ScreenshotCanvasRegion.toWorldviewRect(
    imageWidth: Int,
    imageHeight: Int,
): WorldviewRect? {
    if (imageWidth <= 0 || imageHeight <= 0 || width <= 0 || height <= 0) return null
    val left = (x.toFloat() / imageWidth).coerceIn(0f, 1f)
    val top = (y.toFloat() / imageHeight).coerceIn(0f, 1f)
    val right = ((x + width).toFloat() / imageWidth).coerceIn(0f, 1f)
    val bottom = ((y + height).toFloat() / imageHeight).coerceIn(0f, 1f)
    return runCatching {
        WorldviewRect(left, top, right, bottom)
    }.getOrNull()
}

@Composable
private fun DatastorePanel(
    state: ScreenshotCanvasUiState,
    assets: List<ScreenshotCanvasAsset>,
    workflowState: WorkspaceWorkflowState,
    logStore: StudioLogStore,
    datastore: Map<String, String>,
    recorderObservations: List<WorldObservation> = emptyList(),
) {
    val logChangeToken = logStore.changeToken
    val logEntries = remember(logChangeToken) { logStore.allEntries() }
    val selectedMarker = remember(state.selectedSavedMarkerId, state.savedMarkers.toList()) {
        state.savedMarkers.firstOrNull { it.id == state.selectedSavedMarkerId }
    }
    val templateMarkers = remember(state.savedMarkers.toList()) {
        state.savedMarkers.filter { it.markerMode == ScreenshotCanvasMarkerMode.Template }
    }
    val actionMarkers = remember(state.savedMarkers.toList()) {
        state.savedMarkers.filter { it.markerMode != ScreenshotCanvasMarkerMode.Template }
    }
    val canvasWorldview = remember(
        assets,
        state.savedMarkers.toList(),
        state.vision.observations.toList(),
        recorderObservations,
        workflowState.resources.revision,
    ) {
        buildCanvasWorldview(
            assets = assets,
            markers = state.savedMarkers,
            baseResources = workflowState.resources,
            visionObservations = state.vision.observations,
            recorderObservations = recorderObservations,
        )
    }
    val visualMemory = remember(canvasWorldview) {
        VisualUiMemoryProjector.project(canvasWorldview)
    }
    val dataProjection = remember(canvasWorldview) {
        WorldviewDataProjector.project(canvasWorldview)
    }
    val canvasObservationProjection = remember(
        canvasWorldview,
        state.showAccessibilityNodes,
        state.filterHideClickable,
        state.filterHideInvisible,
        state.filterHideNonFocusable,
        state.showOcrNodes,
        state.showVisionTemplateNodes,
        state.showYoloNodes,
        state.showDomNodes,
        state.showMarkers,
        state.selectedCanvasSourceId,
    ) {
        CanvasObservationProjector.project(
            document = canvasWorldview,
            filters = state.toCanvasObservationFilters(),
            selectedSourceId = state.selectedCanvasSourceId,
        )
    }
    val dragProjection = remember(canvasWorldview.resources.revision, canvasWorldview.resources.resources) {
        WssPanelDragProjector.forResources(
            panelId = "datastore",
            bundle = canvasWorldview.resources,
            panelType = PanelType.Datastore,
        )
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
                DatastoreSection(visualMemory.title) {
                    DatastoreMetric("Revision", visualMemory.worldviewRevision.toString())
                    DatastoreMetric("Provider aktiv", "${visualMemory.activeProviderCount}/${visualMemory.providerStates.size}")
                    DatastoreMetric(
                        "Facetten",
                        visualMemory.facetCounts.joinToString(", ") { "${it.facet.name}:${it.count}" },
                    )
                    visualMemory.providerStates.forEach { provider ->
                        DatastoreMetric(
                            provider.provider.name,
                            if (provider.active) "${provider.itemCount} aktiv" else "${provider.itemCount} bereit",
                        )
                    }
                }
            }
            item {
                DatastoreSection("Dataset") {
                    DatastoreMetric("Screenshots", assets.size.toString())
                    DatastoreMetric("Marker", state.savedMarkers.size.toString())
                    DatastoreMetric("Templates", templateMarkers.size.toString())
                    DatastoreMetric("Visual Assets", "ShapeMaker vorbereitet")
                    DatastoreMetric("Key/Value", datastore.size.toString())
                    DatastoreMetric("Auswahl", selectedMarker?.label ?: "-")
                    DatastoreMetric("Region", state.selectedRegion?.let { "${it.x},${it.y} ${it.width}x${it.height}" } ?: "-")
                }
            }
            item {
                WssDragTreeSection(
                    title = "WSS Drag Bus",
                    projection = dragProjection,
                    maxItems = 14,
                )
            }
            if (visualMemory.suggestions.isNotEmpty()) {
                item {
                    DatastoreSection("Worldview Suggestions") {
                        visualMemory.suggestions.take(8).forEach { suggestion ->
                            DatastoreMetric(
                                suggestion.label,
                                "${"%.0f".format(suggestion.confidence * 100)}% | ${suggestion.detail}",
                            )
                        }
                    }
                }
            }
            if (dataProjection.observationGroups.isNotEmpty()) {
                item {
                    DatastoreSection("Observation History") {
                        dataProjection.observationGroups.take(10).forEach { group ->
                            DatastoreMetric(
                                "${group.provider.name} ${group.label}",
                                "${group.count} Beobachtungen | latest=${group.latestAtEpochMs} | ${group.sampleObservationIds.take(2).joinToString()}",
                            )
                        }
                    }
                }
            }
            if (assets.isNotEmpty()) {
                item {
                    DatastoreSection("Screenshot Assets") {
                        assets.take(6).forEach { asset ->
                            DatastoreMetric(
                                asset.label,
                                "${asset.scene} | ${asset.dateLabel}",
                            )
                        }
                    }
                }
            }
            item {
                DatastoreSection("Vision Quellen") {
                    DatastoreMetric("Canvas", if (assets.isNotEmpty()) "bereit" else "leer")
                    DatastoreMetric("Marker", if (state.savedMarkers.isNotEmpty()) "persistiert" else "leer")
                    DatastoreMetric("Templates", if (templateMarkers.isNotEmpty()) "persistiert" else "leer")
                    DatastoreMetric("A11Y", if (state.showAccessibilityNodes) "sichtbar" else "ausgeblendet")
                    DatastoreMetric("OCR", if (state.showOcrNodes) "sichtbar" else "ausgeblendet")
                    DatastoreMetric("OCV", if (state.showVisionTemplateNodes) "sichtbar" else "ausgeblendet")
                    DatastoreMetric("YOLO", if (state.showYoloNodes) "sichtbar" else "ausgeblendet")
                }
            }
            item {
                DatastoreSection("Pipeline Status") {
                    DatastoreMetric("Screenshot", "lokal")
                    DatastoreMetric("Marker Save/Load", "lokal")
                    DatastoreMetric("Template Compare", if (templateMarkers.isNotEmpty()) "lokal testbar" else "wartet auf Template")
                    DatastoreMetric("OCR Adapter", "geplant")
                    DatastoreMetric("OCV Adapter", "geplant")
                    DatastoreMetric("YOLO Adapter", "geplant")
                    DatastoreMetric("A11Y Scan", "geplant")
                }
            }
            item {
                DatastoreSection("Ressourcen") {
                    DatastoreMetric("Workflow ID", workflowState.document.id)
                    DatastoreMetric("Blöcke", workflowState.document.blocks.size.toString())
                    DatastoreMetric("Roots", workflowState.document.rootBlocks.size.toString())
                    DatastoreMetric("Revision", workflowState.revision.toString())
                    DatastoreMetric("Asset Format", "application/vnd.emscript.motion+json")
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
                        actionMarkers.take(8).forEach { marker ->
                            DatastoreMetric(
                                marker.label,
                                "${marker.markerMode.name} ${marker.region.x},${marker.region.y} ${marker.region.width}x${marker.region.height}",
                            )
                        }
                        if (actionMarkers.isEmpty()) {
                            DatastoreMetric("Status", "Keine Aktionsmarker")
                        }
                    }
                }
                item {
                    DatastoreSection("Templates") {
                        templateMarkers.take(8).forEach { marker ->
                            DatastoreMetric(
                                marker.label,
                                "${marker.processingMode.name} ${marker.region.width}x${marker.region.height} @ ${marker.threshold}",
                            )
                        }
                        if (templateMarkers.isEmpty()) {
                            DatastoreMetric("Status", "Keine Templates")
                        }
                    }
                }
            }
            selectedMarker?.let { marker ->
                item {
                    DatastoreSection("Marker Detail") {
                        DatastoreMetric("ID", marker.id)
                        DatastoreMetric("Asset", marker.assetLabel ?: marker.assetId ?: "-")
                        DatastoreMetric("Modus", marker.markerMode.name)
                        DatastoreMetric("Match", marker.matchKind.name)
                        DatastoreMetric("Processing", marker.processingMode.name)
                        DatastoreMetric("Threshold", marker.threshold.toString())
                        DatastoreMetric("Export", markerExportCode(marker))
                    }
                }
            }
            if (datastore.isNotEmpty()) {
                item {
                    DatastoreSection("Key/Value") {
                        datastore.entries
                            .sortedBy { it.key }
                            .take(12)
                            .forEach { (key, value) ->
                                DatastoreMetric(key, value)
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualAssetManagerPanel(
    workflowState: WorkspaceWorkflowState,
    screenshotState: ScreenshotCanvasUiState,
    screenshotAssets: List<ScreenshotCanvasAsset>,
) {
    val context = LocalContext.current
    val launchIntent = remember(context) {
        context.packageManager.getLaunchIntentForPackage(M3_SHAPEMAKER_PACKAGE)
    }
    val templateCount = remember(screenshotState.savedMarkers.toList()) {
        screenshotState.savedMarkers.count { it.markerMode == ScreenshotCanvasMarkerMode.Template }
    }
    val visualAssetSteps = remember {
        listOf(
            "VisualAsset als kanonisches Asset-Modell verwenden (.ema).",
            "Block-Shape, Node-Shape, Port-Shape und Icon als Asset-Typen trennen.",
            "Ports, Anchors und ContentAreas als Semantik aus ShapeMaker übernehmen.",
            "Compose-Overlay nur an selektierte Blöcke/Nodes koppeln, nicht als dauerhafte Canvas-Wahrheit.",
            "BlockDesigner mit paralleler FlowNode-Vorschau verbinden.",
            "Toolbox-Sets für Standard, Custom, Mixed, JavaScript und spätere Plugin-Familien speichern.",
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Visual Assets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "M3ShapeMaker Bridge",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = {
                    launchIntent?.let { context.startActivity(it) }
                },
                enabled = launchIntent != null,
            ) {
                Text(if (launchIntent != null) "ShapeMaker öffnen" else "ShapeMaker fehlt")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DatastoreSection("Asset Status") {
                    DatastoreMetric("Format", "application/vnd.emscript.motion+json")
                    DatastoreMetric("Dateiendung", ".ema")
                    DatastoreMetric("ShapeMaker", if (launchIntent != null) "installiert" else "nicht installiert")
                    DatastoreMetric("Screenshots", screenshotAssets.size.toString())
                    DatastoreMetric("Templates", templateCount.toString())
                    DatastoreMetric("Workflow Revision", workflowState.revision.toString())
                }
            }
            item {
                DatastoreSection("WSS Bindings") {
                    DatastoreMetric("Block Shapes", "geplant")
                    DatastoreMetric("Flow Nodes", "geplant")
                    DatastoreMetric("Ports", "geplant")
                    DatastoreMetric("Icons", "geplant")
                    DatastoreMetric("Compose Overlay", "selektierte Elemente")
                    DatastoreMetric("Toolbox Sets", "Standard / Custom / Mixed / JavaScript")
                }
            }
            item {
                DatastoreSection("Nächste Schritte") {
                    visualAssetSteps.forEachIndexed { index, step ->
                        DatastoreMetric("${index + 1}", step)
                    }
                }
            }
        }
    }
}

@Composable
private fun Vt2VtPanel(
    workflowState: WorkspaceWorkflowState,
    logStore: StudioLogStore,
    flowRuntimeSnapshot: FlowRuntimeSnapshot?,
    focusedFlowchartNodeId: FlowNodeId?,
    focusedFlowchartEdgeId: FlowEdgeId?,
    activeRuntimeStepIndex: Int?,
) {
    val deviceLabel = remember {
        listOfNotNull(Build.MANUFACTURER, Build.MODEL)
            .joinToString(" ")
            .ifBlank { "VT Studio WSS" }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lanAddresses = remember { runCatching { detectVt2VtLanAddresses() }.getOrDefault(emptyList()) }
    val usbBridgeConfig = remember { Vt2VtUsbBridgeConfig() }
    var usbBridgeStatus by remember { mutableStateOf(Vt2VtUsbAdbBridge.detect(context)) }
    val shizukuStatus = remember { ShizukuRegistration.inspect(context) }
    val termuxStatus = remember { TermuxRegistration.inspect(context) }
    var session by remember { mutableStateOf(defaultVt2VtSession(deviceLabel)) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var remoteHost by remember { mutableStateOf("") }
    var lanPort by remember { mutableStateOf("47272") }
    var lanStatus by remember { mutableStateOf("LAN bereit") }
    var mirrorEnabled by remember { mutableStateOf(false) }
    var listenerJob by remember { mutableStateOf<Job?>(null) }
    val logChangeToken = logStore.changeToken
    val logEntries = remember(logChangeToken) { logStore.allEntries().takeLast(6) }
    val latestLogEntry = logEntries.lastOrNull()
    val latestMessage = session.inbound.lastOrNull() ?: session.outbound.lastOrNull()

    fun runtimeMessage(type: Vt2VtMessageType = Vt2VtMessageType.RuntimeStepChanged): Vt2VtMessage =
        Vt2VtMessage(
            id = "vt2vt-${session.eventCount + 1}",
            type = type,
            sourcePeerId = session.localPeer.id,
            timestampMs = System.currentTimeMillis(),
            revision = workflowState.revision.toLong(),
            payload = when (type) {
                Vt2VtMessageType.WorkspaceState -> workspacePayload(workflowState)
                Vt2VtMessageType.SelectionChanged -> selectionPayload(focusedFlowchartNodeId, focusedFlowchartEdgeId)
                Vt2VtMessageType.LogEntryAdded -> logPayload(latestLogEntry)
                else -> runtimePayload(
                    workflowState = workflowState,
                    snapshot = flowRuntimeSnapshot,
                    activeRuntimeStepIndex = activeRuntimeStepIndex,
                    focusedNodeId = focusedFlowchartNodeId,
                    focusedEdgeId = focusedFlowchartEdgeId
                )
            }
        )

    fun receiveExchange(port: Int) {
        coroutineScope.launch {
            runCatching {
                Vt2VtLanTransport.receiveOnce(
                    port = port,
                    localPeerId = session.localPeer.id,
                    responsePayload = mapOf("pairingCode" to session.pairingCode)
                )
            }.onSuccess { exchange ->
                val remotePeer = Vt2VtPeer(
                    id = exchange.inbound.sourcePeerId,
                    label = exchange.remoteAddress,
                    role = Vt2VtRole.Secondary,
                    transport = Vt2VtTransport.LanTcp,
                    state = Vt2VtConnectionState.Connected
                )
                session = session.copy(
                    connectionState = Vt2VtConnectionState.Connected,
                    localPeer = session.localPeer.copy(transport = Vt2VtTransport.LanTcp),
                    remotePeers = (session.remotePeers.filterNot { it.id == remotePeer.id } + remotePeer).takeLast(8),
                    inbound = (session.inbound + exchange.inbound).takeLast(64),
                    outbound = (session.outbound + exchange.outbound).takeLast(64),
                    lastError = null
                )
                lanStatus = "Empfangen von ${exchange.remoteAddress}: ${exchange.inbound.type.name}"
            }.onFailure { error ->
                session = session.copy(connectionState = Vt2VtConnectionState.Error, lastError = error.message)
                lanStatus = "Empfang fehlgeschlagen: ${error.message ?: error::class.java.simpleName}"
            }
        }
    }

    fun startListener(port: Int) {
        listenerJob?.cancel()
        listenerJob = coroutineScope.launch {
            lanStatus = "Listener aktiv auf Port $port"
            session = session.copy(
                connectionState = Vt2VtConnectionState.Pairing,
                localPeer = session.localPeer.copy(transport = Vt2VtTransport.LanTcp)
            )
            while (isActive) {
                runCatching {
                    Vt2VtLanTransport.receiveOnce(
                        port = port,
                        localPeerId = session.localPeer.id,
                        timeoutMs = 30_000,
                        responsePayload = mapOf("pairingCode" to session.pairingCode)
                    )
                }.onSuccess { exchange ->
                    val remotePeer = Vt2VtPeer(
                        id = exchange.inbound.sourcePeerId,
                        label = exchange.remoteAddress,
                        role = Vt2VtRole.Secondary,
                        transport = Vt2VtTransport.LanTcp,
                        state = Vt2VtConnectionState.Connected
                    )
                    session = session.copy(
                        connectionState = Vt2VtConnectionState.Connected,
                        localPeer = session.localPeer.copy(transport = Vt2VtTransport.LanTcp),
                        remotePeers = (session.remotePeers.filterNot { it.id == remotePeer.id } + remotePeer).takeLast(8),
                        inbound = (session.inbound + exchange.inbound).takeLast(64),
                        outbound = (session.outbound + exchange.outbound).takeLast(64),
                        lastError = null
                    )
                    lanStatus = "Listener: ${exchange.inbound.type.name} von ${exchange.remoteAddress}"
                }.onFailure { error ->
                    if (isActive) {
                        lanStatus = "Listener wartet weiter: ${error.message ?: error::class.java.simpleName}"
                    }
                }
            }
        }
    }

    fun stopListener() {
        listenerJob?.cancel()
        listenerJob = null
        lanStatus = "Listener gestoppt"
    }

    fun emitLoopback(type: Vt2VtMessageType) {
        val message = runtimeMessage(type)
        session = session.copy(
            connectionState = Vt2VtConnectionState.Observing,
            outbound = (session.outbound + message).takeLast(64),
            inbound = (session.inbound + Vt2VtMessageCodec.decode(Vt2VtMessageCodec.encode(message))).takeLast(64),
            lastError = null
        )
    }

    fun sendLan(type: Vt2VtMessageType) {
        val endpoint = Vt2VtLanEndpoint(remoteHost, lanPort.toIntOrNull() ?: 47272)
        val message = runtimeMessage(type)
        lanStatus = "Sende ${type.name} an ${endpoint.host}:${endpoint.port} ..."
        session = session.copy(
            connectionState = Vt2VtConnectionState.Syncing,
            localPeer = session.localPeer.copy(transport = Vt2VtTransport.LanTcp),
            outbound = (session.outbound + message).takeLast(64),
            lastError = null
        )
        coroutineScope.launch {
            runCatching { Vt2VtLanTransport.send(endpoint, message) }
                .onSuccess { ack ->
                    val remotePeer = Vt2VtPeer(
                        id = ack.sourcePeerId,
                        label = endpoint.host,
                        role = Vt2VtRole.Observer,
                        transport = Vt2VtTransport.LanTcp,
                        state = Vt2VtConnectionState.Connected
                    )
                    session = session.copy(
                        connectionState = Vt2VtConnectionState.Connected,
                        remotePeers = (session.remotePeers.filterNot { it.id == remotePeer.id } + remotePeer).takeLast(8),
                        inbound = (session.inbound + ack).takeLast(64),
                        lastError = null
                    )
                    lanStatus = "ACK von ${endpoint.host}: ${ack.payload["ack"] ?: ack.id}"
                }.onFailure { error ->
                    session = session.copy(connectionState = Vt2VtConnectionState.Error, lastError = error.message)
                    lanStatus = "Senden fehlgeschlagen: ${error.message ?: error::class.java.simpleName}"
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose { listenerJob?.cancel() }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            val status = Vt2VtUsbAdbBridge.detect(context)
            usbBridgeStatus = status
            if (
                status.bridgeReady &&
                usbBridgeConfig.autoConnect &&
                session.localPeer.transport != Vt2VtTransport.UsbAdbBridge
            ) {
                remoteHost = usbBridgeConfig.endpoint.host
                lanPort = usbBridgeConfig.endpoint.port.toString()
                lanStatus = "USB/ADB Auto-Bridge bereit: ${usbBridgeConfig.reverseCommand}"
                session = session.copy(
                    connectionState = Vt2VtConnectionState.Pairing,
                    localPeer = session.localPeer.copy(transport = Vt2VtTransport.UsbAdbBridge),
                    lastError = null
                )
            }
            kotlinx.coroutines.delay(2_000)
        }
    }

    LaunchedEffect(mirrorEnabled, remoteHost, flowRuntimeSnapshot?.sequence, activeRuntimeStepIndex) {
        if (mirrorEnabled && remoteHost.isNotBlank() && flowRuntimeSnapshot != null) {
            sendLan(Vt2VtMessageType.RuntimeStepChanged)
        }
    }

    LaunchedEffect(mirrorEnabled, remoteHost, latestLogEntry?.id, latestLogEntry?.repeatCount) {
        if (mirrorEnabled && remoteHost.isNotBlank() && latestLogEntry != null) {
            sendLan(Vt2VtMessageType.LogEntryAdded)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("VT2VT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${session.connectionState.name} · ${session.localPeer.role.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                FilledTonalButton(onClick = { roleMenuExpanded = true }) {
                    Text(session.localPeer.role.name)
                }
                DropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                    Vt2VtRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = {
                                session = session.copy(
                                    localPeer = session.localPeer.copy(role = role),
                                    connectionState = Vt2VtConnectionState.Pairing
                                )
                                roleMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(onClick = {}, label = { Text("Pair ${session.pairingCode}") })
            AssistChip(onClick = {}, label = { Text(VT2VT_MESSAGE_FORMAT.substringAfterLast('.')) })
            AssistChip(onClick = {}, label = { Text(lanAddresses.firstOrNull() ?: "Keine LAN-IP") })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = { usbBridgeStatus = Vt2VtUsbAdbBridge.detect(context) },
                label = { Text(usbBridgeStatus.summary) },
            )
            AssistChip(
                onClick = { context.safeStartActivity(ShizukuRegistration.settingsIntent(shizukuStatus)) },
                label = { Text(if (shizukuStatus.available) "Shizuku aktiv" else shizukuStatus.summary) },
            )
            AssistChip(
                onClick = { context.safeStartActivity(TermuxRegistration.settingsIntent(termuxStatus)) },
                label = { Text(if (termuxStatus.canRunCommands) "Termux aktiv" else termuxStatus.summary) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { session = session.withLoopbackHello() }) {
                Text("Loopback verbinden")
            }
            OutlinedButton(
                onClick = { emitLoopback(Vt2VtMessageType.WorkspaceState) }
            ) {
                Text("Loopback Snapshot")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Switch(checked = mirrorEnabled, onCheckedChange = { mirrorEnabled = it })
                Text("Live Mirror", style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = remoteHost,
                onValueChange = { remoteHost = it.trim() },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Remote IP") },
                placeholder = { Text("192.168.x.x") },
            )
            OutlinedTextField(
                value = lanPort,
                onValueChange = { value -> lanPort = value.filter { it.isDigit() }.take(5) },
                modifier = Modifier.width(104.dp),
                singleLine = true,
                label = { Text("Port") },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    val port = lanPort.toIntOrNull() ?: 47272
                    if (listenerJob == null) startListener(port) else stopListener()
                }
            ) {
                Text(if (listenerJob == null) "Listener starten" else "Listener stoppen")
            }
            OutlinedButton(onClick = { receiveExchange(lanPort.toIntOrNull() ?: 47272) }) {
                Text("Einmal empfangen")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                enabled = remoteHost.isNotBlank(),
                onClick = { sendLan(Vt2VtMessageType.WorkspaceState) }
            ) {
                Text("Workspace")
            }
            OutlinedButton(
                enabled = remoteHost.isNotBlank(),
                onClick = { sendLan(Vt2VtMessageType.SelectionChanged) }
            ) {
                Text("Selection")
            }
            OutlinedButton(
                enabled = remoteHost.isNotBlank(),
                onClick = { sendLan(Vt2VtMessageType.RuntimeStepChanged) }
            ) {
                Text("Runtime")
            }
            OutlinedButton(
                enabled = remoteHost.isNotBlank(),
                onClick = { sendLan(Vt2VtMessageType.LogEntryAdded) }
            ) {
                Text("Log")
            }
        }
        OutlinedButton(
            enabled = usbBridgeStatus.bridgeReady,
            onClick = {
                remoteHost = usbBridgeConfig.endpoint.host
                lanPort = usbBridgeConfig.endpoint.port.toString()
                session = session.copy(
                    connectionState = Vt2VtConnectionState.Pairing,
                    localPeer = session.localPeer.copy(transport = Vt2VtTransport.UsbAdbBridge),
                    lastError = null
                )
                lanStatus = "USB/ADB Bridge gewählt: ${usbBridgeConfig.reverseCommand}"
            }
        ) {
            Text("USB/ADB Bridge verwenden")
        }
        Text(lanStatus, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DatastoreSection("Session") {
                    DatastoreMetric("Lokales Gerät", session.localPeer.label)
                    DatastoreMetric("Peer ID", session.localPeer.id)
                    DatastoreMetric("Remote Peers", session.remotePeers.size.toString())
                    DatastoreMetric("Outbound", session.outbound.size.toString())
                    DatastoreMetric("Inbound", session.inbound.size.toString())
                    DatastoreMetric("Transport", session.localPeer.transport.name)
                    DatastoreMetric("LAN IP", lanAddresses.joinToString().ifBlank { "-" })
                    DatastoreMetric("Status", lanStatus)
                }
            }
            item {
                DatastoreSection("Workspace Snapshot") {
                    DatastoreMetric("Revision", workflowState.revision.toString())
                    DatastoreMetric("Blöcke", workflowState.document.blocks.size.toString())
                    DatastoreMetric("Flow Nodes", workflowState.flowchartProjection.graph.nodes.size.toString())
                    DatastoreMetric("Flow Edges", workflowState.flowchartProjection.graph.edges.size.toString())
                    DatastoreMetric("Runtime Events", flowRuntimeSnapshot?.nodeStates?.size?.toString() ?: "-")
                    DatastoreMetric("Aktiver Step", activeRuntimeStepIndex?.toString() ?: "-")
                }
            }
            item {
                DatastoreSection("Remote Events") {
                    latestMessage?.let { message ->
                        DatastoreMetric("Letzte Nachricht", message.type.name)
                        DatastoreMetric("Quelle", message.sourcePeerId)
                        DatastoreMetric("Revision", message.revision?.toString() ?: "-")
                        DatastoreMetric("Payload", message.payload.entries.joinToString { "${it.key}=${it.value}" }.ifBlank { "-" })
                    } ?: DatastoreMetric("Status", "Noch keine Nachricht")
                }
            }
            if (logEntries.isNotEmpty()) {
                item {
                    DatastoreSection("Log Mirror") {
                        logEntries.forEach { entry ->
                            DatastoreMetric(entry.level.name, "${entry.source}: ${entry.message}")
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
private fun WssDragTreeSection(
    title: String,
    projection: WssPanelDragProjection,
    maxItems: Int,
    modifier: Modifier = Modifier,
) {
    val visibleItems = remember(projection.tree, maxItems) {
        projection.tree.items.flatMap { it.flattenForPreview() }.take(maxItems)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = projection.dropTarget.kind.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                projection.dropTarget.acceptedModes.forEach { mode ->
                    AssistChip(
                        onClick = {},
                        label = { Text(mode.name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            if (visibleItems.isEmpty()) {
                Text(
                    text = "Keine transportierbaren Items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                visibleItems.forEach { item ->
                    WssDragTreePreviewRow(item, projection)
                }
                val remaining = projection.tree.items.sumOf { it.countDeep() } - visibleItems.size
                if (remaining > 0) {
                    Text(
                        text = "+$remaining weitere Items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WssDragTreePreviewRow(
    item: WssDragTreePreviewItem,
    projection: WssPanelDragProjection,
) {
    val indent = (item.depth * 12).dp
    val dropEffect = remember(item.payload, projection.dropTarget) {
        val decision = WssDragRules.decide(item.payload, projection.dropTarget)
        WssDropEffectResolver.resolve(
            WssDropResult(
                target = projection.dropTarget,
                payload = item.payload,
                decision = decision,
            ),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.payload.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.payload.kind.name} -> ${dropEffect.kind.name}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.acceptsChildren) {
            Text(
                text = item.children.size.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private data class WssDragTreePreviewItem(
    val payload: com.visualtasker.wss.workspace.model.WssDragPayload,
    val acceptsChildren: Boolean,
    val children: List<WssDragTreeItem>,
    val depth: Int,
)

private fun WssDragTreeItem.flattenForPreview(depth: Int = 0): List<WssDragTreePreviewItem> =
    listOf(
        WssDragTreePreviewItem(
            payload = payload,
            acceptsChildren = acceptsChildren,
            children = children,
            depth = depth,
        )
    ) + children.flatMap { it.flattenForPreview(depth + 1) }

private fun WssDragTreeItem.countDeep(): Int =
    1 + children.sumOf { it.countDeep() }

@Composable
private fun MarkerCanvasPanel(
    state: ScreenshotCanvasUiState,
    assets: List<ScreenshotCanvasAsset>,
    baseResources: WorkspaceResourceBundle = WorkspaceResourceBundle(),
    selectedRailTraceStep: RecorderStepUi? = null,
    recorderObservations: List<WorldObservation> = emptyList(),
    onSceneMarkerSaved: (ScreenshotCanvasSavedMarker) -> Unit = {},
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
    val selectedMarker = remember(state.selectedSavedMarkerId, state.savedMarkers.toList()) {
        state.savedMarkers.firstOrNull { it.id == state.selectedSavedMarkerId }
    }
    val canvasWorldview = remember(
        assets,
        state.savedMarkers.toList(),
        state.vision.observations.toList(),
        recorderObservations,
        baseResources.revision,
    ) {
        buildCanvasWorldview(
            assets = assets,
            markers = state.savedMarkers,
            baseResources = baseResources,
            visionObservations = state.vision.observations,
            recorderObservations = recorderObservations,
        )
    }
    val visualMemory = remember(canvasWorldview) {
        VisualUiMemoryProjector.project(canvasWorldview)
    }
    val canvasObservationProjection = remember(
        canvasWorldview,
        state.showAccessibilityNodes,
        state.filterHideClickable,
        state.filterHideInvisible,
        state.filterHideNonFocusable,
        state.showOcrNodes,
        state.showVisionTemplateNodes,
        state.showYoloNodes,
        state.showDomNodes,
        state.showMarkers,
        state.selectedCanvasSourceId,
    ) {
        CanvasObservationProjector.project(
            document = canvasWorldview,
            filters = state.toCanvasObservationFilters(),
            selectedSourceId = state.selectedCanvasSourceId,
        )
    }
    val dragProjection = remember(canvasWorldview.resources.revision, canvasWorldview.resources.resources) {
        WssPanelDragProjector.forResources(
            panelId = "marker",
            bundle = canvasWorldview.resources,
            panelType = PanelType.Marker,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MarkerModeToolbar(state = state)
        DrawCommandToolbar(state = state)
        MarkerUnderScreenshotPanel(
            state = state,
            asset = selectedAsset,
            imageSize = bitmap?.let { it.width to it.height },
            observationProjection = canvasObservationProjection,
            onSceneMarkerSaved = onSceneMarkerSaved,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        MarkerTraceContextCard(selectedRailTraceStep)
        WssDragTreeSection(
            title = "Marker Drag Bus",
            projection = dragProjection,
            maxItems = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        MarkerVisualMemoryCard(
            marker = selectedMarker,
            worldview = canvasWorldview,
            visualMemory = visualMemory,
            selectedSourceId = state.selectedCanvasSourceId,
        )
        ScreenshotCanvasInspector(
            modifier = Modifier.fillMaxWidth(),
            asset = selectedAsset,
            imageSize = bitmap?.let { "${it.width} x ${it.height}px" } ?: "-",
            state = state,
            markerPanel = true,
        )
    }
}

@Composable
private fun MarkerVisualMemoryCard(
    marker: ScreenshotCanvasSavedMarker?,
    worldview: WorldviewDocument,
    visualMemory: VisualUiMemoryProjection,
    selectedSourceId: String? = null,
) {
    val clipboard = LocalClipboardManager.current
    val markerResourceId = marker?.id?.stableMarkerResourceId(marker.markerMode)
    val selectedObservation = remember(worldview, selectedSourceId) {
        selectedSourceId?.let { sourceId -> worldview.observations.firstOrNull { it.id == sourceId } }
    }
    val inspectorProjection = remember(worldview, markerResourceId, selectedObservation?.id) {
        when {
            selectedObservation != null -> WorldviewInspectorProjector.project(
                document = worldview,
                subject = WorldviewInspectorSubject(
                    kind = WorldviewInspectorSubjectKind.Observation,
                    id = selectedObservation.id,
                ),
            )
            markerResourceId != null -> WorldviewInspectorProjector.project(
                document = worldview,
                subject = WorldviewInspectorSubject(
                    kind = WorldviewInspectorSubjectKind.Resource,
                    id = markerResourceId,
                ),
            )
            else -> null
        }
    }
    val suggestionSourceId = selectedObservation?.id ?: markerResourceId
    val suggestions = remember(visualMemory, suggestionSourceId) {
        suggestionSourceId
            ?.let { sourceId -> visualMemory.suggestions.filter { it.sourceId == sourceId } }
            .orEmpty()
    }
    var selectedSuggestionId by remember(markerResourceId, suggestions) {
        mutableStateOf(suggestions.firstOrNull()?.id)
    }
    val selectedSuggestion = suggestions.firstOrNull { it.id == selectedSuggestionId }
        ?: suggestions.firstOrNull()
    val preparedAction = remember(marker, selectedObservation, selectedSuggestion) {
        selectedSuggestion?.let { suggestion ->
            selectedObservation?.let { observationSuggestionExportCode(it, suggestion) }
                ?: marker?.let { markerSuggestionExportCode(it, suggestion) }
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Visual UI Memory",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = inspectorProjection?.title
                    ?: marker?.label
                    ?: "Kein Marker ausgewaehlt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val rows = inspectorProjection?.rows.orEmpty()
            if (rows.isNotEmpty()) {
                val rowText = rows.take(3).joinToString(" | ") { "${it.label}: ${it.value.ifBlank { "-" }}" }
                Text(
                    text = rowText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.76f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = "Provider aktiv: ${visualMemory.activeProviderCount}/${visualMemory.providerStates.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            suggestions.firstOrNull()?.let { suggestion ->
                Text(
                    text = "${suggestion.type}: ${suggestion.label} (${("%.0f".format(suggestion.confidence * 100))}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    suggestions.forEach { suggestion ->
                        FilterChip(
                            selected = suggestion.id == selectedSuggestion?.id,
                            onClick = { selectedSuggestionId = suggestion.id },
                            label = {
                                Text(
                                    suggestion.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
            preparedAction?.let { action ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                ) {
                    Text(
                        text = action,
                        modifier = Modifier.padding(7.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(action)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Suggestion Code", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun markerSuggestionExportCode(
    marker: ScreenshotCanvasSavedMarker,
    suggestion: VisualUiMemorySuggestion,
): String =
    when (suggestion.type) {
        "CREATE_TEMPLATE" -> {
            val region = marker.region
            "templateCompare(\"${marker.label}\", region(${region.x}, ${region.y}, ${region.width}, ${region.height}), \"${marker.processingMode.name.lowercase()}\")"
        }
        "CREATE_CLICK_ACTION" -> when (marker.markerMode) {
            ScreenshotCanvasMarkerMode.Point -> {
                val region = marker.region
                "clickPoint(${region.x + region.width / 2}, ${region.y + region.height / 2})"
            }
            ScreenshotCanvasMarkerMode.Template -> "findTemplate(\"${marker.label}\")"
            else -> markerExportCode(marker)
        }
        else -> markerExportCode(marker)
    }

private fun observationSuggestionExportCode(
    observation: WorldObservation,
    suggestion: VisualUiMemorySuggestion,
): String =
    when (suggestion.type) {
        "CREATE_CLICK_FROM_RECORD" -> observation.point?.let { point ->
            "clickPoint(${point.x.roundToInt()}, ${point.y.roundToInt()})"
        } ?: observation.bounds?.let { bounds ->
            val centerX = ((bounds.left + bounds.right) / 2f).roundToInt()
            val centerY = ((bounds.top + bounds.bottom) / 2f).roundToInt()
            "clickPoint($centerX, $centerY)"
        } ?: "// Keine Koordinate in ${observation.id}"
        "CREATE_MARKER_FROM_RECORD" -> observation.bounds?.let { bounds ->
            val x = bounds.left.roundToInt()
            val y = bounds.top.roundToInt()
            val width = (bounds.right - bounds.left).roundToInt().coerceAtLeast(1)
            val height = (bounds.bottom - bounds.top).roundToInt().coerceAtLeast(1)
            val label = observation.properties["text"] ?: observation.kind.name
            "markerSave(\"${label.escapeEmscriptString()}\", region($x, $y, $width, $height))"
        } ?: observation.point?.let { point ->
            val label = observation.properties["text"] ?: observation.kind.name
            "markerSave(\"${label.escapeEmscriptString()}\", point(${point.x.roundToInt()}, ${point.y.roundToInt()}))"
        } ?: "// Keine Marker-Geometrie in ${observation.id}"
        else -> observation.properties["text"] ?: observation.id
    }

private fun String.escapeEmscriptString(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

@Composable
private fun MarkerTraceContextCard(
    step: RecorderStepUi?,
) {
    val junctionPlan = remember(step) { step?.let(JunctionatorSeed::fromRailTraceStep) }
    val primaryCandidate = junctionPlan?.primaryCandidate
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                "RailTrace Sync",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = step?.let { "${it.label} | ${it.actionType} | ${it.activityName ?: "Scene offen"}" }
                    ?: "Kein RailTrace-Step ausgewaehlt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            step?.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            primaryCandidate?.let { candidate ->
                Text(
                    text = "Junktionator: ${candidate.confidence.name} -> ${candidate.outputTargets.joinToString { it.name }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MarkerModeToolbar(
    state: ScreenshotCanvasUiState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        MarkerModeButton("Spline", state.markerMode == ScreenshotCanvasMarkerMode.Spline) {
            state.markerMode = ScreenshotCanvasMarkerMode.Spline
        }
        MarkerModeButton("Path", state.markerMode == ScreenshotCanvasMarkerMode.Path) {
            state.markerMode = ScreenshotCanvasMarkerMode.Path
        }
        MarkerModeButton("Multi", state.multiMarkerMode) {
            state.multiMarkerMode = !state.multiMarkerMode
        }
    }
}

@Composable
private fun DrawCommandToolbar(
    state: ScreenshotCanvasUiState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScreenshotCanvasDrawTool.entries.forEach { tool ->
            MarkerModeButton(
                label = when (tool) {
                    ScreenshotCanvasDrawTool.Line -> "Draw.line"
                    ScreenshotCanvasDrawTool.Circle -> "Draw.circle"
                    ScreenshotCanvasDrawTool.Curve -> "Draw.curve"
                    ScreenshotCanvasDrawTool.Spline -> "Draw.spline"
                    ScreenshotCanvasDrawTool.Path -> "Draw.path"
                    ScreenshotCanvasDrawTool.Polygon -> "Draw.polygon"
                    ScreenshotCanvasDrawTool.Text -> "Draw.text"
                    ScreenshotCanvasDrawTool.Image -> "Draw.image"
                    ScreenshotCanvasDrawTool.Shape -> "Draw.shape"
                },
                selected = state.drawTool == tool,
                onClick = { state.drawTool = tool },
            )
        }
    }
}

@Composable
private fun ScreenshotAssetCarousel(
    assets: List<ScreenshotCanvasAsset>,
    selectedAssetId: String?,
    onSelect: (ScreenshotCanvasAsset) -> Unit,
) {
    if (assets.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = "Keine gespeicherten Screenshots.",
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(assets, key = { it.id }) { asset ->
            FilterChip(
                selected = asset.id == selectedAssetId,
                onClick = { onSelect(asset) },
                label = {
                    Column(modifier = Modifier.width(132.dp)) {
                        Text(
                            asset.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            asset.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                },
            )
        }
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
            FilledTonalButton(
                onClick = {
                    state.visualTestScore = if (state.selectedRegion == null) null else 0.91f
                    state.markerStatusMessage = if (state.selectedRegion == null) {
                        "Keine Region markiert."
                    } else {
                        "Crop vorbereitet."
                    }
                },
                enabled = state.selectedRegion != null,
                modifier = Modifier.height(34.dp),
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("Crop", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 2.dp))
            }
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
                    val score = compareScreenshotRegions(
                        liveBitmap = liveBitmap,
                        liveRegion = state.selectedRegion,
                        liveMode = state.liveProcessingMode,
                        referenceBitmap = referenceBitmap,
                        referenceRegion = referenceMarker?.region,
                        referenceMode = state.referenceProcessingMode,
                    )
                    state.visualTestScore = score
                    state.recordVisionObservation(
                        asset = selectedAsset,
                        bitmap = liveBitmap,
                        referenceMarker = referenceMarker,
                        score = score,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
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
    val canvasWorldview = remember(
        assets,
        state.savedMarkers.toList(),
        state.vision.observations.toList(),
    ) {
        buildCanvasWorldview(
            assets = assets,
            markers = state.savedMarkers,
            visionObservations = state.vision.observations,
        )
    }
    val observationProjection = remember(
        canvasWorldview,
        state.showAccessibilityNodes,
        state.filterHideClickable,
        state.filterHideInvisible,
        state.filterHideNonFocusable,
        state.showOcrNodes,
        state.showVisionTemplateNodes,
        state.showYoloNodes,
        state.showDomNodes,
        state.showMarkers,
        state.selectedCanvasSourceId,
    ) {
        CanvasObservationProjector.project(
            document = canvasWorldview,
            filters = state.toCanvasObservationFilters(),
            selectedSourceId = state.selectedCanvasSourceId,
        )
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
                observationProjection = observationProjection,
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
    baseResources: WorkspaceResourceBundle = WorkspaceResourceBundle(),
    recorderObservations: List<WorldObservation> = emptyList(),
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
    val canvasWorldview = remember(
        assets,
        state.savedMarkers.toList(),
        state.vision.observations.toList(),
        recorderObservations,
        baseResources.revision,
    ) {
        buildCanvasWorldview(
            assets = assets,
            markers = state.savedMarkers,
            baseResources = baseResources,
            visionObservations = state.vision.observations,
            recorderObservations = recorderObservations,
        )
    }
    val observationProjection = remember(
        canvasWorldview,
        state.showAccessibilityNodes,
        state.filterHideClickable,
        state.filterHideInvisible,
        state.filterHideNonFocusable,
        state.showOcrNodes,
        state.showVisionTemplateNodes,
        state.showYoloNodes,
        state.showDomNodes,
        state.showMarkers,
        state.selectedCanvasSourceId,
    ) {
        CanvasObservationProjector.project(
            document = canvasWorldview,
            filters = state.toCanvasObservationFilters(),
            selectedSourceId = state.selectedCanvasSourceId,
        )
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
                observationProjection = observationProjection,
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

    fun imageToScreenPoint(x: Int, y: Int, imageWidth: Int, imageHeight: Int): Offset =
        Offset(
            x = offsetX + x.toFloat() / imageWidth * drawnWidth,
            y = offsetY + y.toFloat() / imageHeight * drawnHeight,
        )
}

@Composable
private fun ScreenshotRegionCanvas(
    bitmap: ImageBitmap,
    asset: ScreenshotCanvasAsset?,
    state: ScreenshotCanvasUiState,
    observationProjection: CanvasObservationProjection,
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
                    observationProjection.hitTest(startScreen, layout, imageWidth, imageHeight)?.let { item ->
                        state.selectCanvasObservation(item)
                    }
                    val startImage = layout.screenToImage(startScreen, imageWidth, imageHeight) ?: return@awaitEachGesture
                    val regionAtStart = state.selectedRegion
                    val screenRect = regionAtStart?.let { layout.regionToScreenRect(it, imageWidth, imageHeight) }
                    val handleRadiusPx = 26.dp.toPx()
                    val pointHitRadiusPx = 32.dp.toPx()
                    val pointMode = state.markerMode == ScreenshotCanvasMarkerMode.Point
                    val pathMode = state.markerMode == ScreenshotCanvasMarkerMode.Spline || state.markerMode == ScreenshotCanvasMarkerMode.Path
                    val pathAtStart = if (pathMode) {
                        state.selectedPath ?: regionAtStart?.let { defaultBezierForRegion(it, imageWidth, imageHeight) }
                    } else {
                        null
                    }
                    val setup = if (screenRect != null) {
                        val handle = if (pointMode) {
                            ScreenshotCanvasOverlayHandle.None
                        } else if (pathMode && pathAtStart != null) {
                            detectPathOverlayHandle(startScreen, pathAtStart, layout, imageWidth, imageHeight, handleRadiusPx)
                                .takeUnless { it == ScreenshotCanvasOverlayHandle.None }
                                ?: detectScreenshotOverlayHandle(startScreen, screenRect, handleRadiusPx)
                        } else {
                            detectScreenshotOverlayHandle(startScreen, screenRect, handleRadiusPx)
                        }
                        val mode = when (handle) {
                            ScreenshotCanvasOverlayHandle.ScanType,
                            ScreenshotCanvasOverlayHandle.Close -> ScreenshotCanvasTouchMode.TapHandle
                            ScreenshotCanvasOverlayHandle.PathStart -> ScreenshotCanvasTouchMode.MovePathStart
                            ScreenshotCanvasOverlayHandle.PathControl -> ScreenshotCanvasTouchMode.MovePathControl
                            ScreenshotCanvasOverlayHandle.PathEnd -> ScreenshotCanvasTouchMode.MovePathEnd
                            ScreenshotCanvasOverlayHandle.ResizeBottomLeft -> ScreenshotCanvasTouchMode.ResizeBottomLeft
                            ScreenshotCanvasOverlayHandle.ResizeBottomRight -> ScreenshotCanvasTouchMode.ResizeBottomRight
                            ScreenshotCanvasOverlayHandle.None -> when {
                                pointMode && (startScreen - screenRect.center).getDistance() <= pointHitRadiusPx -> ScreenshotCanvasTouchMode.Move
                                pointMode -> ScreenshotCanvasTouchMode.PointTap
                                pathMode && screenRect.contains(startScreen) -> ScreenshotCanvasTouchMode.MovePath
                                screenRect.contains(startScreen) -> ScreenshotCanvasTouchMode.Move
                                else -> ScreenshotCanvasTouchMode.Create
                            }
                        }
                        ScreenshotCanvasGestureSetup(mode, startImage, regionAtStart, pathAtStart, handle)
                    } else {
                        ScreenshotCanvasGestureSetup(
                            mode = if (pointMode) ScreenshotCanvasTouchMode.PointTap else ScreenshotCanvasTouchMode.Create,
                            createAnchorImage = startImage,
                            regionStart = null,
                            pathStart = null,
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
                            ScreenshotCanvasTouchMode.Create -> {
                                if (pathMode) {
                                    val bezier = bezierFromDrag(setup.createAnchorImage, currentImage, imageWidth, imageHeight)
                                    state.selectedPath = bezier
                                    bezier.bounds(imageWidth, imageHeight)
                                } else {
                                    regionFromPoints(setup.createAnchorImage, currentImage, imageWidth, imageHeight)
                                }
                            }
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
                            ScreenshotCanvasTouchMode.MovePath -> {
                                val base = setup.pathStart ?: return@drag
                                val dx = (currentImage.x - setup.createAnchorImage.x).roundToInt()
                                val dy = (currentImage.y - setup.createAnchorImage.y).roundToInt()
                                val bezier = moveBezier(base, dx, dy, imageWidth, imageHeight)
                                state.selectedPath = bezier
                                bezier.bounds(imageWidth, imageHeight)
                            }
                            ScreenshotCanvasTouchMode.MovePathStart -> {
                                val base = setup.pathStart ?: return@drag
                                val bezier = base.copy(
                                    startX = currentImage.x.roundToInt().coerceIn(0, imageWidth),
                                    startY = currentImage.y.roundToInt().coerceIn(0, imageHeight),
                                )
                                state.selectedPath = bezier
                                bezier.bounds(imageWidth, imageHeight)
                            }
                            ScreenshotCanvasTouchMode.MovePathControl -> {
                                val base = setup.pathStart ?: return@drag
                                val bezier = base.copy(
                                    controlX = currentImage.x.roundToInt().coerceIn(0, imageWidth),
                                    controlY = currentImage.y.roundToInt().coerceIn(0, imageHeight),
                                )
                                state.selectedPath = bezier
                                bezier.bounds(imageWidth, imageHeight)
                            }
                            ScreenshotCanvasTouchMode.MovePathEnd -> {
                                val base = setup.pathStart ?: return@drag
                                val bezier = base.copy(
                                    endX = currentImage.x.roundToInt().coerceIn(0, imageWidth),
                                    endY = currentImage.y.roundToInt().coerceIn(0, imageHeight),
                                )
                                state.selectedPath = bezier
                                bezier.bounds(imageWidth, imageHeight)
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
        drawCanvasObservationProjection(
            layout = layout,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            projection = observationProjection,
        )
        drawScreenshotRegionOverlay(
            layout = layout,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            region = displayRegion,
            path = state.selectedPath,
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

private fun bezierFromDrag(
    start: Offset,
    end: Offset,
    imageWidth: Int,
    imageHeight: Int,
): ScreenshotCanvasBezier {
    val startX = start.x.roundToInt().coerceIn(0, imageWidth)
    val startY = start.y.roundToInt().coerceIn(0, imageHeight)
    val endX = end.x.roundToInt().coerceIn(0, imageWidth)
    val endY = end.y.roundToInt().coerceIn(0, imageHeight)
    val curveLift = max(36, abs(endX - startX) / 3 + abs(endY - startY) / 6)
    return ScreenshotCanvasBezier(
        startX = startX,
        startY = startY,
        controlX = ((startX + endX) / 2).coerceIn(0, imageWidth),
        controlY = (min(startY, endY) - curveLift).coerceIn(0, imageHeight),
        endX = endX,
        endY = endY,
    )
}

private fun defaultBezierForRegion(
    region: ScreenshotCanvasRegion,
    imageWidth: Int,
    imageHeight: Int,
): ScreenshotCanvasBezier =
    bezierFromDrag(
        start = Offset(region.x.toFloat(), region.y.toFloat()),
        end = Offset((region.x + region.width).toFloat(), (region.y + region.height).toFloat()),
        imageWidth = imageWidth,
        imageHeight = imageHeight,
    )

private fun moveBezier(
    base: ScreenshotCanvasBezier,
    dx: Int,
    dy: Int,
    imageWidth: Int,
    imageHeight: Int,
): ScreenshotCanvasBezier =
    ScreenshotCanvasBezier(
        startX = (base.startX + dx).coerceIn(0, imageWidth),
        startY = (base.startY + dy).coerceIn(0, imageHeight),
        controlX = (base.controlX + dx).coerceIn(0, imageWidth),
        controlY = (base.controlY + dy).coerceIn(0, imageHeight),
        endX = (base.endX + dx).coerceIn(0, imageWidth),
        endY = (base.endY + dy).coerceIn(0, imageHeight),
    )

private fun ScreenshotCanvasBezier.bounds(imageWidth: Int, imageHeight: Int): ScreenshotCanvasRegion {
    val left = min(startX, min(controlX, endX)).coerceIn(0, imageWidth - 1)
    val top = min(startY, min(controlY, endY)).coerceIn(0, imageHeight - 1)
    val right = max(startX, max(controlX, endX)).coerceIn(left + 1, imageWidth)
    val bottom = max(startY, max(controlY, endY)).coerceIn(top + 1, imageHeight)
    return ScreenshotCanvasRegion(left, top, right - left, bottom - top)
}

private fun detectPathOverlayHandle(
    point: Offset,
    path: ScreenshotCanvasBezier,
    layout: ScreenshotFittedImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    radius: Float,
): ScreenshotCanvasOverlayHandle {
    val start = layout.imageToScreenPoint(path.startX, path.startY, imageWidth, imageHeight)
    val control = layout.imageToScreenPoint(path.controlX, path.controlY, imageWidth, imageHeight)
    val end = layout.imageToScreenPoint(path.endX, path.endY, imageWidth, imageHeight)
    return when {
        (point - start).getDistance() <= radius -> ScreenshotCanvasOverlayHandle.PathStart
        (point - control).getDistance() <= radius -> ScreenshotCanvasOverlayHandle.PathControl
        (point - end).getDistance() <= radius -> ScreenshotCanvasOverlayHandle.PathEnd
        else -> ScreenshotCanvasOverlayHandle.None
    }
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

private fun DrawScope.drawCanvasObservationProjection(
    layout: ScreenshotFittedImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    projection: CanvasObservationProjection,
) {
    if (projection.items.isEmpty()) {
        drawCanvasHint(layout.imageBounds, "Keine aktiven Observations")
        return
    }
    projection.items.forEach { item ->
        val rect = item.screenRect(layout, imageWidth, imageHeight)
        val color = item.canvasColor()
        drawObservationRect(
            rect = rect,
            color = color,
            label = item.label,
            style = item.lineStyle,
            fillAlpha = if (item.selected) 0.20f else 0.09f,
        )
        if (item.point != null) {
            drawLine(color, Offset(rect.center.x - 14f, rect.center.y), Offset(rect.center.x + 14f, rect.center.y), 2f, cap = StrokeCap.Round)
            drawLine(color, Offset(rect.center.x, rect.center.y - 14f), Offset(rect.center.x, rect.center.y + 14f), 2f, cap = StrokeCap.Round)
        }
    }
}

private val A11Y_VISIBLE_COLOR = Color(0xFF40C4FF)
private val A11Y_CLICKABLE_COLOR = Color(0xFF00E676)
private val A11Y_FOCUSABLE_COLOR = Color(0xFFFFD54F)
private val OCR_COLOR = Color(0xFFFFF176)
private val OCV_COLOR = Color(0xFF69F0AE)
private val YOLO_COLOR = Color(0xFFFF7043)
private val DOM_COLOR = Color(0xFFCE93D8)

private fun CanvasObservationItem.canvasColor(): Color =
    when (family) {
        CanvasObservationFamily.AccessibilityVisible -> A11Y_VISIBLE_COLOR
        CanvasObservationFamily.AccessibilityClickable -> A11Y_CLICKABLE_COLOR
        CanvasObservationFamily.AccessibilityFocusable -> A11Y_FOCUSABLE_COLOR
        CanvasObservationFamily.Ocr -> OCR_COLOR
        CanvasObservationFamily.Ocv -> OCV_COLOR
        CanvasObservationFamily.Yolo -> YOLO_COLOR
        CanvasObservationFamily.Dom -> DOM_COLOR
        CanvasObservationFamily.Marker -> OCV_COLOR
        CanvasObservationFamily.Unknown -> Color(0xFFE0E0E0)
    }

private fun CanvasObservationItem.screenRect(
    layout: ScreenshotFittedImageLayout,
    imageWidth: Int,
    imageHeight: Int,
): Rect {
    bounds?.let { bounds ->
        val isNormalized = bounds.coordinateSpace.kind == CoordinateSpaceKind.Normalized
        val leftPx = if (isNormalized) bounds.left * imageWidth else bounds.left
        val topPx = if (isNormalized) bounds.top * imageHeight else bounds.top
        val rightPx = if (isNormalized) bounds.right * imageWidth else bounds.right
        val bottomPx = if (isNormalized) bounds.bottom * imageHeight else bounds.bottom
        return layout.regionToScreenRect(
            ScreenshotCanvasRegion(
                x = leftPx.roundToInt().coerceIn(0, imageWidth - 1),
                y = topPx.roundToInt().coerceIn(0, imageHeight - 1),
                width = (rightPx - leftPx).roundToInt().coerceAtLeast(1),
                height = (bottomPx - topPx).roundToInt().coerceAtLeast(1),
            ),
            imageWidth,
            imageHeight,
        )
    }
    val pointValue = point ?: return Rect.Zero
    val isNormalized = pointValue.coordinateSpace.kind == CoordinateSpaceKind.Normalized
    val center = layout.imageToScreenPoint(
        (if (isNormalized) pointValue.x * imageWidth else pointValue.x).roundToInt(),
        (if (isNormalized) pointValue.y * imageHeight else pointValue.y).roundToInt(),
        imageWidth,
        imageHeight,
    )
    val radius = 24f
    return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
}

private fun CanvasObservationProjection.hitTest(
    point: Offset,
    layout: ScreenshotFittedImageLayout,
    imageWidth: Int,
    imageHeight: Int,
): CanvasObservationItem? =
    items.asReversed().firstOrNull { item ->
        item.screenRect(layout, imageWidth, imageHeight).expandedBy(8f).contains(point)
    }

private fun Rect.expandedBy(delta: Float): Rect =
    Rect(left - delta, top - delta, right + delta, bottom + delta)

private fun demoRegionRect(
    layout: ScreenshotFittedImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): Rect =
    layout.regionToScreenRect(
        ScreenshotCanvasRegion(
            x = (imageWidth * x).roundToInt(),
            y = (imageHeight * y).roundToInt(),
            width = (imageWidth * width).roundToInt().coerceAtLeast(1),
            height = (imageHeight * height).roundToInt().coerceAtLeast(1),
        ),
        imageWidth,
        imageHeight,
    )

private fun DrawScope.drawObservationRect(
    rect: Rect,
    color: Color,
    label: String,
    style: CanvasObservationLineStyle,
    fillAlpha: Float = 0.10f,
) {
    val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    drawRoundRect(
        color = color.copy(alpha = fillAlpha),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = radius,
    )
    val stroke = when (style) {
        CanvasObservationLineStyle.Solid -> Stroke(width = 2.dp.toPx())
        CanvasObservationLineStyle.Bold -> Stroke(width = 3.2.dp.toPx())
        CanvasObservationLineStyle.Dashed -> Stroke(width = 2.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)))
        CanvasObservationLineStyle.Underline -> Stroke(width = 2.dp.toPx())
        CanvasObservationLineStyle.Capsule -> Stroke(width = 2.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f)))
    }
    if (style == CanvasObservationLineStyle.Underline) {
        drawLine(color, Offset(rect.left, rect.bottom), Offset(rect.right, rect.bottom), stroke.width, cap = StrokeCap.Round)
        drawLine(color.copy(alpha = 0.52f), Offset(rect.left, rect.center.y), Offset(rect.right, rect.center.y), 1.4f, cap = StrokeCap.Round)
    } else {
        drawRoundRect(
            color = color,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = radius,
            style = stroke,
        )
    }
    drawObservationLabel(rect, label, color)
}

private fun DrawScope.drawObservationLabel(rect: Rect, label: String, color: Color) {
    val text = label.take(24)
    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        textAlign = android.graphics.Paint.Align.LEFT
        textSize = 10.dp.toPx()
        isFakeBoldText = true
    }
    val baseline = (rect.top - 5.dp.toPx()).coerceAtLeast(12.dp.toPx())
    drawContext.canvas.nativeCanvas.drawText(text, rect.left + 3.dp.toPx(), baseline, paint)
}

private fun markerColor(marker: ScreenshotCanvasSavedMarker): Color =
    parseHexColor(marker.colourHex) ?: when (marker.matchKind) {
        ScreenshotCanvasMatchKind.OCR -> OCR_COLOR
        ScreenshotCanvasMatchKind.OCV -> OCV_COLOR
    }

private fun parseHexColor(raw: String): Color? =
    runCatching { Color(android.graphics.Color.parseColor(raw)) }.getOrNull()

private fun DrawScope.drawScreenshotRegionOverlay(
    layout: ScreenshotFittedImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    region: ScreenshotCanvasRegion?,
    path: ScreenshotCanvasBezier?,
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
    if (markerMode == ScreenshotCanvasMarkerMode.Spline || markerMode == ScreenshotCanvasMarkerMode.Path) {
        val bezier = path ?: defaultBezierForRegion(region, imageWidth, imageHeight)
        val start = layout.imageToScreenPoint(bezier.startX, bezier.startY, imageWidth, imageHeight)
        val control = layout.imageToScreenPoint(bezier.controlX, bezier.controlY, imageWidth, imageHeight)
        val end = layout.imageToScreenPoint(bezier.endX, bezier.endY, imageWidth, imageHeight)
        val color = Color(0xFF69F0AE)
        drawLine(color.copy(alpha = 0.42f), start, control, 1.6f, cap = StrokeCap.Round)
        drawLine(color.copy(alpha = 0.42f), control, end, 1.6f, cap = StrokeCap.Round)
        drawPath(
            path = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(control.x, control.y, end.x, end.y)
            },
            color = color,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )
        drawPathHandle(start, "S", color)
        drawPathHandle(control, "B", Color(0xFFFFF176))
        drawPathHandle(end, "E", color)
    } else if (markerMode == ScreenshotCanvasMarkerMode.Swipe) {
        val color = Color(0xFFFFD54F)
        drawLine(color, Offset(rect.left, rect.top), Offset(rect.right, rect.bottom), 3.5f, cap = StrokeCap.Round)
        drawCircle(color, radius = 6f, center = Offset(rect.left, rect.top))
        drawCircle(color, radius = 8f, center = Offset(rect.right, rect.bottom), style = Stroke(2.5f))
    }
    drawCanvasHandle(rect.left, rect.top, if (matchKind == ScreenshotCanvasMatchKind.OCR) "OCR" else "OCV", Color(0xFF1976D2))
    drawCanvasHandle(rect.right, rect.top, "X", Color(0xFFD32F2F))
    drawCanvasHandle(rect.left, rect.bottom, "R", Color(0xFF2E7D32))
    drawCanvasHandle(rect.right, rect.bottom, "R", Color(0xFF2E7D32))
}

private fun DrawScope.drawPathHandle(center: Offset, label: String, color: Color) {
    drawCircle(Color.Black.copy(alpha = 0.56f), radius = 13f, center = center)
    drawCircle(color, radius = 10f, center = center)
    drawCircle(Color.White, radius = 10f, center = center, style = Stroke(1.4f))
    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.BLACK
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 10.dp.toPx()
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.drawText(label, center.x, center.y - ((paint.descent() + paint.ascent()) / 2f), paint)
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
                    ScreenshotLayerChip("SPL", state.markerMode == ScreenshotCanvasMarkerMode.Spline) { state.markerMode = ScreenshotCanvasMarkerMode.Spline }
                    ScreenshotLayerChip("PATH", state.markerMode == ScreenshotCanvasMarkerMode.Path) { state.markerMode = ScreenshotCanvasMarkerMode.Path }
                    ScreenshotLayerChip("MULTI", state.multiMarkerMode) { state.multiMarkerMode = !state.multiMarkerMode }
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
    observationProjection: CanvasObservationProjection,
    onSceneMarkerSaved: (ScreenshotCanvasSavedMarker) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = markerMetricsTitle(state.selectedRegion, state.markerMode, imageSize),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        CanvasObservationSummaryCard(
            projection = observationProjection,
            selectedSourceId = state.selectedCanvasSourceId,
        )
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = state.colourHex,
                onValueChange = { state.colourHex = it },
                modifier = Modifier.weight(1f),
                label = { Text("Colour", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
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
            Button(
                onClick = {
                    val region = state.selectedRegion
                    state.markerStatusMessage = if (region == null) {
                        "Keine Region markiert."
                    } else {
                        val now = System.currentTimeMillis()
                        val id = if (state.multiMarkerMode) "marker:${now}" else state.selectedSavedMarkerId ?: "marker:${now}"
                        val path = if (state.markerMode == ScreenshotCanvasMarkerMode.Spline || state.markerMode == ScreenshotCanvasMarkerMode.Path) {
                            state.selectedPath ?: imageSize?.let { (imageWidth, imageHeight) ->
                                defaultBezierForRegion(region, imageWidth, imageHeight)
                            }
                        } else {
                            null
                        }
                        val marker = ScreenshotCanvasSavedMarker(
                            id = id,
                            label = state.templateName.trim().ifBlank { "Marker" },
                            assetId = asset?.id,
                            assetLabel = asset?.label,
                            region = region,
                            path = path,
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
                        onSceneMarkerSaved(marker)
                        "${state.markerMode.name} '${marker.label}' gespeichert."
                    }
                },
                enabled = state.selectedRegion != null,
                modifier = Modifier
                    .fillMaxWidth()
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

@Composable
private fun CanvasObservationSummaryCard(
    projection: CanvasObservationProjection,
    selectedSourceId: String?,
) {
    val selected = remember(projection, selectedSourceId) {
        projection.items.firstOrNull { it.sourceId == selectedSourceId || it.id == selectedSourceId }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = selected?.let { "Observation: ${it.label}" } ?: "Observations: ${projection.items.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val familySummary = projection.items
                .groupingBy { it.family }
                .eachCount()
                .entries
                .sortedBy { it.key.name }
                .joinToString("  ") { "${it.key.name}:${it.value}" }
                .ifBlank { "Keine aktiven Layer" }
            Text(
                text = familySummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (projection.ambiguityCandidates.isNotEmpty()) {
                Text(
                    text = "Perugger: ${projection.ambiguityCandidates.size} Klaerfall vorbereitet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
            ScreenshotCanvasMarkerMode.Spline -> "Spline: punkte=-$size"
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
        ScreenshotCanvasMarkerMode.Spline -> "Spline: start=(${region.x},${region.y})  ende=(${region.x + region.width},${region.y + region.height})"
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
    return screenshotCanvasAssetRoots(context)
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

private fun screenshotCanvasAssetSignature(context: Context): Long =
    screenshotCanvasAssetRoots(context)
        .flatMap { root -> root.listFiles().orEmpty().asIterable() }
        .filter { it.isFile && it.extension.lowercase(Locale.ROOT) in setOf("png", "jpg", "jpeg", "webp") }
        .fold(17L) { signature, file ->
            31L * signature + file.absolutePath.hashCode() + file.lastModified() + file.length()
        }

private fun File.recordingSignature(): String =
    "$absolutePath:${lastModified()}:${length()}"

private fun screenshotCanvasAssetRoots(context: Context): List<File> =
    listOfNotNull(
        File(runtimeFilesRoot(context), "screenshots"),
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        File(context.filesDir, "screenshots"),
    )

private fun screenshotCanvasMarkerStoreFile(context: Context): File =
    File(runtimeFilesRoot(context), "markers/saved-markers.json")

private fun runtimeDatastoreFile(context: Context): File =
    File(runtimeFilesRoot(context), "datastore/runtime-values.json")

private fun loadRuntimeDatastore(context: Context): Map<String, String> {
    val file = runtimeDatastoreFile(context)
    if (!file.isFile) return emptyMap()
    return runCatching {
        val root = JSONObject(file.readText())
        val values = root.optJSONObject("values") ?: JSONObject()
        buildMap {
            values.keys().forEach { key ->
                put(key, values.optString(key, ""))
            }
        }
    }.getOrDefault(emptyMap())
}

private fun persistRuntimeDatastore(
    context: Context,
    values: Map<String, String>,
) {
    runCatching {
        val file = runtimeDatastoreFile(context)
        file.parentFile?.mkdirs()
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("updatedAt", System.currentTimeMillis())
        root.put("values", JSONObject(values))
        file.writeText(root.toString(2))
    }
}

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
                val path = item.optJSONObject("path")?.let { path ->
                    ScreenshotCanvasBezier(
                        startX = path.optInt("startX", region.optInt("x", 0)),
                        startY = path.optInt("startY", region.optInt("y", 0)),
                        controlX = path.optInt("controlX", region.optInt("x", 0) + region.optInt("width", 1) / 2),
                        controlY = path.optInt("controlY", region.optInt("y", 0)),
                        endX = path.optInt("endX", region.optInt("x", 0) + region.optInt("width", 1)),
                        endY = path.optInt("endY", region.optInt("y", 0) + region.optInt("height", 1)),
                    )
                }
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
                    path = path,
                    markerMode = markerModeFromStorage(item.optString("markerMode"), path),
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
            marker.path?.let { path ->
                item.put(
                    "path",
                    JSONObject()
                        .put("startX", path.startX)
                        .put("startY", path.startY)
                        .put("controlX", path.controlX)
                        .put("controlY", path.controlY)
                        .put("endX", path.endX)
                        .put("endY", path.endY)
                )
            }
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

private fun markerModeFromStorage(raw: String?, path: ScreenshotCanvasBezier?): ScreenshotCanvasMarkerMode {
    val value = raw.orEmpty().trim()
    return when {
        value.equals("Path", ignoreCase = true) && path != null -> ScreenshotCanvasMarkerMode.Spline
        value.equals("Spline", ignoreCase = true) -> ScreenshotCanvasMarkerMode.Spline
        value.equals("Path", ignoreCase = true) -> ScreenshotCanvasMarkerMode.Path
        else -> runCatching { enumValueOf<ScreenshotCanvasMarkerMode>(value) }.getOrNull()
            ?: ScreenshotCanvasMarkerMode.Region
    }
}

private fun RuntimeAutomationRegion.toScreenshotRegion(): ScreenshotCanvasRegion =
    ScreenshotCanvasRegion(
        x = x,
        y = y,
        width = width.coerceAtLeast(1),
        height = height.coerceAtLeast(1),
    )

private fun ScreenshotCanvasRegion.toRuntimeRegion(): RuntimeAutomationRegion =
    RuntimeAutomationRegion(
        x = x,
        y = y,
        width = width.coerceAtLeast(1),
        height = height.coerceAtLeast(1),
    )

private fun ScreenshotCanvasSavedMarker.matchesRuntimeTemplateName(raw: String): Boolean {
    val requested = raw.substringAfterLast('/').substringBeforeLast('.').trim()
    val candidates = listOf(
        raw,
        requested,
        id,
        label,
        assetLabel.orEmpty(),
        assetLabel.orEmpty().substringBeforeLast('.'),
    ).map { it.trim() }
    return candidates.any { it.isNotBlank() && it.equals(requested, ignoreCase = true) } ||
        candidates.any { it.isNotBlank() && it.equals(raw.trim(), ignoreCase = true) }
}

private fun markerModeFromRuntime(raw: String): ScreenshotCanvasMarkerMode =
    when (raw.trim().lowercase(Locale.ROOT)) {
        "template", "tpl" -> ScreenshotCanvasMarkerMode.Template
        "point", "tap" -> ScreenshotCanvasMarkerMode.Point
        "swipe" -> ScreenshotCanvasMarkerMode.Swipe
        "spline", "curve", "bezier" -> ScreenshotCanvasMarkerMode.Spline
        "path" -> ScreenshotCanvasMarkerMode.Path
        else -> ScreenshotCanvasMarkerMode.Region
    }

private fun sceneSaveArgsForMarker(marker: ScreenshotCanvasSavedMarker): String =
    listOf(
        emscriptStringLiteral(marker.label),
        emscriptStringLiteral(marker.markerMode.name.lowercase(Locale.ROOT)),
        marker.region.toSceneRegionLiteral(),
        emscriptStringLiteral(marker.assetLabel ?: marker.assetId.orEmpty()),
    ).joinToString(",")

private fun ScreenshotCanvasRegion.toSceneRegionLiteral(): String =
    "region($x,$y,$width,$height)"

private fun emscriptStringLiteral(raw: String): String =
    "\"" + raw
        .replace("\\", "\\\\")
        .replace("\"", "\\\"") + "\""

private fun processingModeFromRuntime(raw: String): ScreenshotCanvasProcessingMode =
    when (raw.trim().lowercase(Locale.ROOT)) {
        "gray", "grey", "grayscale", "grau", "graustufen" -> ScreenshotCanvasProcessingMode.Grayscale
        "contrast", "highcontrast", "kontrast" -> ScreenshotCanvasProcessingMode.HighContrast
        "edge", "edges", "kanten" -> ScreenshotCanvasProcessingMode.Edge
        "inverse", "invert", "invers" -> ScreenshotCanvasProcessingMode.Inverse
        else -> ScreenshotCanvasProcessingMode.Original
    }

@Composable
private fun WorkspaceTopAppBar(
    projectName: String,
    canRunLive: Boolean,
    onRunLive: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onLoadProject: () -> Unit,
    onSaveProject: () -> Unit,
    onClearProject: () -> Unit,
    onClearCanvas: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topBarContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(WORKSPACE_TOP_BAR_HEIGHT_DP.dp)
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        tonalElevation = 3.dp
    ) {
        CompositionLocalProvider(LocalContentColor provides topBarContentColor) {
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
                        text = "VT Studio WSS · $projectName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = topBarContentColor
                    )
                    TooltipIconButton(tooltip = "Workflow starten", onClick = onRunLive, modifier = Modifier.size(34.dp), enabled = canRunLive) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Workflow starten")
                    }
                    TooltipIconButton(tooltip = "Pause", onClick = onPause, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                    }
                    TooltipIconButton(tooltip = "Stop", onClick = onStop, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TooltipIconButton(tooltip = "Projekt laden", onClick = onLoadProject, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Projekt laden")
                    }
                    TooltipIconButton(tooltip = "Projekt speichern", onClick = onSaveProject, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Save, contentDescription = "Projekt speichern")
                    }
                    TooltipIconButton(tooltip = "Projekt leeren", onClick = onClearProject, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Projekt leeren")
                    }
                    TooltipIconButton(tooltip = "Canvas Screenshot leeren", onClick = onClearCanvas, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Photo, contentDescription = "Canvas Screenshot leeren")
                    }
                    TooltipIconButton(tooltip = "Einstellungen", onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
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
                PanelType.M3Director,
                PanelType.Vt2Vt,
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

@Composable
private fun RailTraceModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun RailTraceSurfaceChip(
    mode: RailSurfaceMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(mode.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun RailTraceTrackChip(track: RailTrack) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                "${track.label} ${track.items.size}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun ColumnScope.RailTraceCompactRail(
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRunDry: () -> Unit,
    onRunLive: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    canRunDry: Boolean,
    canRunLive: Boolean,
    canStepBack: Boolean,
    canStepForward: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onExpandRequested: () -> Unit,
) {
    val actions = listOf(
        WorkspaceRailActionSpec("Save Workspace", Icons.Default.Save, onClick = onSave),
        WorkspaceRailActionSpec("Undo", Icons.AutoMirrored.Filled.Undo, onClick = onUndo),
        WorkspaceRailActionSpec("Redo", Icons.AutoMirrored.Filled.Redo, onClick = onRedo),
        WorkspaceRailActionSpec("Dry Run", Icons.Default.PlayArrow, enabled = canRunDry, onClick = onRunDry),
        WorkspaceRailActionSpec("Live Run", Icons.Default.PlayCircle, enabled = canRunLive, onClick = onRunLive),
        WorkspaceRailActionSpec("Step zurück", Icons.Default.SkipPrevious, enabled = canStepBack, onClick = onStepBack),
        WorkspaceRailActionSpec("Step vor", Icons.Default.SkipNext, enabled = canStepForward, onClick = onStepForward),
        WorkspaceRailActionSpec("Zoom +", Icons.Default.ZoomIn, onClick = onZoomIn),
        WorkspaceRailActionSpec("Zoom -", Icons.Default.ZoomOut, onClick = onZoomOut),
        WorkspaceRailActionSpec("RailTrace Tracks", Icons.Default.ViewKanban, onClick = onExpandRequested),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        WorkspaceRailActionList(actions)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RailTraceExpandedRail(
    state: StepperPanelState,
    onModeChange: (RailMode) -> Unit,
    onScaleChange: (RailScaleMode) -> Unit,
    onZoomChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("RailTrace", style = MaterialTheme.typography.titleSmall)
        Text("Rail", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RailSurfaceMode.entries.forEach { mode ->
            RailTraceSurfaceChip(
                mode = mode,
                selected = state.railMode.toSurfaceMode() == mode,
                onClick = { onModeChange(mode.defaultRailMode()) },
            )
        }
        Text(
            text = state.railMode.toSurfaceMode().description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        Text("Detailmodus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val detailModes = when (state.railMode.toSurfaceMode()) {
            RailSurfaceMode.Records -> listOf(RailMode.Replay, RailMode.Curate)
            RailSurfaceMode.Run -> listOf(RailMode.Step, RailMode.Program)
            RailSurfaceMode.WatchDog -> listOf(RailMode.Live)
        }
        detailModes.forEach { mode ->
            RailTraceModeChip(
                label = mode.name.uppercase(),
                selected = state.railMode == mode,
                onClick = { onModeChange(mode) },
            )
        }
        HorizontalDivider()
        Text("Skalierung", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RailTraceModeChip(
            label = "LOGICAL",
            selected = state.scaleMode == RailScaleMode.Logical,
            onClick = { onScaleChange(RailScaleMode.Logical) },
        )
        RailTraceModeChip(
            label = "TEMPORAL",
            selected = state.scaleMode == RailScaleMode.Temporal,
            onClick = { onScaleChange(RailScaleMode.Temporal) },
        )
        HorizontalDivider()
        Text("Timeline Zoom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = state.timelineZoom,
            onValueChange = { onZoomChange(it) },
            valueRange = 0.5f..4f,
        )
        Text(
            "${"%.1f".format(state.timelineZoom)}x",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RecorderStepsPanel(
    steps: List<RecorderStepUi>,
    actionSink: PanelActionSink,
    activeRuntimeStepIndex: Int? = null,
    initialState: StepperPanelState = StepperPanelState(),
    timelineZoom: Float = 1f,
    onTimelineZoomChange: (Float) -> Unit = {},
    onViewStateChange: (StepperPanelState) -> Unit = {},
    onSaveState: (StepperPanelState) -> Unit = {},
    recordingSessions: List<RecordingSessionUi> = emptyList(),
    selectedRecordingSessionPath: String? = null,
    onRecordingSessionSelected: (String) -> Unit = {},
) {
    var selectedStepId by remember { mutableStateOf(initialState.selectedStepId) }
    var replayIndex by remember { mutableIntStateOf(initialState.replayIndex) }
    var replayPositionMs by remember { mutableLongStateOf(initialState.replayPositionMs) }
    var railMode by remember { mutableStateOf(initialState.railMode) }
    var railScaleMode by remember { mutableStateOf(initialState.scaleMode) }
    var localTimelineZoom by remember { mutableFloatStateOf(timelineZoom.coerceIn(0.5f, 4f)) }
    var playing by remember { mutableStateOf(false) }
    val speedSteps = remember { listOf(0.2f, 0.5f, 1f, 2f, 4f) }
    var speedStepIndex by remember {
        mutableIntStateOf(speedSteps.indexOfClosest(initialState.speed).coerceAtLeast(0))
    }
    val speed = speedSteps[speedStepIndex]
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val thresholdPx = 56f
    val timelinePoints = remember(steps) { buildRecorderTimelinePoints(steps) }
    val activityStepGroups = remember(steps, timelinePoints) { buildRecorderActivityStepGroups(steps, timelinePoints) }
    val stepListState = rememberLazyListState()
    val timelineStartMs = timelinePoints.firstOrNull()?.startMs ?: 0L
    val timelineEndMs = timelinePoints.maxOfOrNull { it.endMs }?.coerceAtLeast(timelineStartMs + 1L) ?: 1L
    val safeIndex = replayIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
    val activeStep = steps.getOrNull(safeIndex)
    var sessionMenuExpanded by remember { mutableStateOf(false) }
    val selectedSession = recordingSessions.firstOrNull { it.path == selectedRecordingSessionPath }
    val railProjection = remember(steps, railMode, railScaleMode) {
        steps.toRailProjection(mode = railMode, scaleMode = railScaleMode)
    }
    val progressFraction = remember(steps.size, safeIndex, replayPositionMs, timelineStartMs, timelineEndMs, railScaleMode) {
        if (steps.isEmpty()) {
            0f
        } else if (railScaleMode == RailScaleMode.Logical) {
            (safeIndex + 1).toFloat() / steps.size.toFloat()
        } else {
            ((replayPositionMs - timelineStartMs).toFloat() / (timelineEndMs - timelineStartMs).toFloat()).coerceIn(0f, 1f)
        }
    }
    LaunchedEffect(timelineZoom) {
        localTimelineZoom = timelineZoom.coerceIn(0.5f, 4f)
    }
    LaunchedEffect(initialState.railMode, initialState.scaleMode) {
        railMode = initialState.railMode
        railScaleMode = initialState.scaleMode
    }

    LaunchedEffect(steps.size) {
        if (replayIndex > steps.lastIndex) replayIndex = steps.lastIndex.coerceAtLeast(0)
        replayPositionMs = replayPositionMs.coerceIn(timelineStartMs, timelineEndMs)
        if (steps.isEmpty()) playing = false
    }
    LaunchedEffect(activeRuntimeStepIndex, steps.size) {
        val index = activeRuntimeStepIndex ?: return@LaunchedEffect
        if (index !in steps.indices) return@LaunchedEffect
        replayIndex = index
        selectedStepId = steps[index].id
        replayPositionMs = timelinePoints.getOrNull(index)?.startMs ?: replayPositionMs
    }
    LaunchedEffect(safeIndex, activityStepGroups) {
        if (steps.isEmpty()) return@LaunchedEffect
        val groupIndex = activityStepGroups.indexOfFirst { group ->
            group.steps.any { it.index == safeIndex }
        }
        if (groupIndex >= 0) stepListState.animateScrollToItem(groupIndex)
    }
    LaunchedEffect(playing, speed, steps.size, timelineStartMs, timelineEndMs) {
        if (!playing || steps.isEmpty()) return@LaunchedEffect
        if (replayPositionMs >= timelineEndMs) replayPositionMs = timelineStartMs
        while (playing && replayPositionMs < timelineEndMs) {
            delay(50)
            replayPositionMs = (replayPositionMs + (50f * speed.coerceIn(0.25f, 4f)).toLong())
                .coerceAtMost(timelineEndMs)
            val nextIndex = nearestTimelineIndex(timelinePoints, replayPositionMs)
            if (nextIndex != replayIndex && nextIndex in steps.indices) {
                replayIndex = nextIndex
                selectedStepId = steps[nextIndex].id
                actionSink.onPanelAction(PanelAction.SelectStep(steps[nextIndex].id))
            }
        }
        if (replayPositionMs >= timelineEndMs) playing = false
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(railProjection.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            railProjection.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "${railMode.name.uppercase()} | ${railScaleMode.name.uppercase()} | ${"%.1f".format(localTimelineZoom)}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (steps.isEmpty()) "0 / 0" else "${safeIndex + 1} / ${steps.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TooltipIconButton(
                        tooltip = "RailTrace-Stand speichern",
                        onClick = {
                            onSaveState(
                                StepperPanelState(
                                    selectedStepId = selectedStepId,
                                    replayIndex = safeIndex,
                                    replayPositionMs = replayPositionMs,
                                    speed = speed,
                                    railMode = railMode,
                                    scaleMode = railScaleMode,
                                    timelineZoom = localTimelineZoom,
                                )
                            )
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "RailTrace-Stand speichern", modifier = Modifier.size(18.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = replayPositionMs.formatTimelineMillis(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                    )
                    Text(
                        text = timelineEndMs.formatTimelineMillis(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = railProjection.tracks.joinToString(separator = "  ") { "${it.label} ${it.items.size}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${RailTimelineMarker.Pending.symbol} ${RailTimelineMarker.Recorded.symbol} ${RailTimelineMarker.Active.symbol} ${RailTimelineMarker.Data.symbol} ${RailTimelineMarker.Position.symbol} ${RailTimelineMarker.Warning.symbol}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (recordingSessions.isNotEmpty()) {
                    Box {
                        AssistChip(
                            onClick = { sessionMenuExpanded = true },
                            leadingIcon = {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    selectedSession?.let { "${it.label} | ${it.stepCount} Steps" }
                                        ?: "Recording Session waehlen",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = sessionMenuExpanded,
                            onDismissRequest = { sessionMenuExpanded = false },
                        ) {
                            recordingSessions.forEach { session ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${session.label} | ${session.stepCount} Steps | ${session.durationMs.formatTimelineMillis()}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = {
                                        sessionMenuExpanded = false
                                        onRecordingSessionSelected(session.path)
                                    },
                                )
                            }
                        }
                    }
                }
                RailTraceTimeline(
                    projection = railProjection,
                    steps = steps,
                    activeIndex = safeIndex,
                    selectedStepId = selectedStepId,
                    replayPositionMs = replayPositionMs,
                    zoom = localTimelineZoom,
                    onSeek = { positionMs ->
                        replayPositionMs = positionMs.coerceIn(timelineStartMs, timelineEndMs)
                        val index = nearestTimelineIndex(timelinePoints, replayPositionMs)
                        replayIndex = index
                        steps.getOrNull(index)?.let {
                            selectedStepId = it.id
                            actionSink.onPanelAction(PanelAction.SelectStep(it.id))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(154.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TooltipIconButton(
                        tooltip = "Zum Anfang",
                        onClick = {
                            replayIndex = 0
                            replayPositionMs = timelineStartMs
                            playing = false
                            steps.firstOrNull()?.let {
                                selectedStepId = it.id
                                actionSink.onPanelAction(PanelAction.SelectStep(it.id))
                            }
                        },
                        modifier = Modifier.size(34.dp),
                        enabled = steps.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Zum Anfang", modifier = Modifier.size(18.dp))
                    }
                    TooltipIconButton(
                        tooltip = "Schritt zurueck",
                        onClick = {
                            replayIndex = (safeIndex - 1).coerceAtLeast(0)
                            replayPositionMs = timelinePoints.getOrNull(replayIndex)?.startMs ?: timelineStartMs
                            steps.getOrNull(replayIndex)?.let {
                                selectedStepId = it.id
                                actionSink.onPanelAction(PanelAction.SelectStep(it.id))
                            }
                        },
                        modifier = Modifier.size(34.dp),
                        enabled = steps.isNotEmpty() && safeIndex > 0,
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Schritt zurueck", modifier = Modifier.size(18.dp))
                    }
                    TooltipIconButton(
                        tooltip = if (playing) "Pause" else "Replay",
                        onClick = { playing = !playing },
                        modifier = Modifier.size(38.dp),
                        enabled = steps.isNotEmpty(),
                    ) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    TooltipIconButton(
                        tooltip = "Schritt vor",
                        onClick = {
                            replayIndex = (safeIndex + 1).coerceAtMost(steps.lastIndex)
                            replayPositionMs = timelinePoints.getOrNull(replayIndex)?.startMs ?: timelineEndMs
                            steps.getOrNull(replayIndex)?.let {
                                selectedStepId = it.id
                                actionSink.onPanelAction(PanelAction.SelectStep(it.id))
                            }
                        },
                        modifier = Modifier.size(34.dp),
                        enabled = steps.isNotEmpty() && safeIndex < steps.lastIndex,
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Schritt vor", modifier = Modifier.size(18.dp))
                    }
                    TooltipIconButton(
                        tooltip = "Stop",
                        onClick = {
                            playing = false
                            replayPositionMs = timelineStartMs
                            replayIndex = 0
                        },
                        modifier = Modifier.size(34.dp),
                        enabled = playing,
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = localTimelineZoom,
                        onValueChange = {
                            localTimelineZoom = it.coerceIn(0.5f, 4f)
                            onTimelineZoomChange(localTimelineZoom)
                        },
                        valueRange = 0.5f..4f,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${"%.1f".format(localTimelineZoom)}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = speedStepIndex.toFloat(),
                        onValueChange = { speedStepIndex = it.roundToInt().coerceIn(0, speedSteps.lastIndex) },
                        valueRange = 0f..speedSteps.lastIndex.toFloat(),
                        steps = speedSteps.size - 2,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${speed.formatSpeedStep()}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = activeStep?.let { step ->
                        "${step.label}  |  ${step.actionType}  |  ${step.status.name}"
                    } ?: "Keine Recording- oder Runtime-Session geladen",
                    style = MaterialTheme.typography.bodySmall,
                    color = activeStep?.status?.let { statusColor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text("Tippen = Select, Long-Drag = Reorder, Swipe = Delete", color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyColumn(
            state = stepListState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
	            items(activityStepGroups, key = { it.key }) { group ->
	                Surface(
	                    modifier = Modifier
	                        .fillMaxWidth()
	                        .animateItem(),
	                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(group.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            Text(
                                "${group.startMs.formatTimelineMillis()} - ${group.endMs.formatTimelineMillis()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        group.steps.forEach { entry ->
                            RecorderStepListRow(
                                step = entry.step,
                                index = entry.index,
                                safeIndex = safeIndex,
                                selectedStepId = selectedStepId,
                                timelinePoints = timelinePoints,
                                onSelect = { stepIndex, step ->
                                    selectedStepId = step.id
                                    replayIndex = stepIndex
                                    replayPositionMs = timelinePoints.getOrNull(stepIndex)?.startMs ?: replayPositionMs
                                    actionSink.onPanelAction(PanelAction.SelectStep(step.id))
                                },
                                onDelete = { actionSink.onPanelAction(PanelAction.DeleteStep(it.id)) },
                                onReorder = { from, to -> actionSink.onPanelAction(PanelAction.ReorderStep(from, to)) },
                                onDragDelta = { deltaY ->
                                    dragAccumulator += deltaY
                                    val index = entry.index
                                    if (dragAccumulator > thresholdPx && index < steps.lastIndex) {
                                        dragAccumulator = 0f
                                        index to (index + 1)
                                    } else if (dragAccumulator < -thresholdPx && index > 0) {
                                        dragAccumulator = 0f
                                        index to (index - 1)
                                    } else {
                                        null
                                    }
                                },
                                onDragFinished = { dragAccumulator = 0f },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun EmscriptDryRunResult.toRecorderSteps(): List<RecorderStepUi> {
    return eventsForRailTrace().map { event ->
        val label = buildString {
            append("#")
            append(event.index)
            append(" ")
            append(event.command ?: event.kind)
        }
        RecorderStepUi(
            id = "dry-run-${event.index}",
            label = label,
            actionType = event.kind,
            status = when (event.severity.name) {
                "ERROR" -> StepStatus.Invalid
                "WARNING" -> StepStatus.Edited
                else -> StepStatus.Executed
            },
            timestampMs = event.index * 180L,
            durationMs = 140L,
            activityName = when (event.severity.name) {
                "ERROR" -> "Runtime Fehler"
                "WARNING" -> "Runtime Hinweise"
                else -> "DryRun Runtime"
            },
            detail = event.message,
        )
    }
}

private fun EmscriptDryRunResult.eventsForRailTrace() = when (this) {
    is EmscriptDryRunResult.Success -> events
    is EmscriptDryRunResult.Failure -> events
}

private fun syncRailTraceToDryRunStep(
    result: EmscriptDryRunResult,
    stepIndex: Int,
    currentState: StepperPanelState,
    onStateChanged: (StepperPanelState) -> Unit,
) {
    val steps = result.toRecorderSteps()
    val activeIndex = (stepIndex - 1).takeIf { stepIndex > 0 }?.coerceIn(0, (steps.size - 1).coerceAtLeast(0)) ?: 0
    val activeStep = if (stepIndex > 0) steps.getOrNull(activeIndex) else null
    val updated = currentState.copy(
        selectedStepId = activeStep?.id,
        replayIndex = activeIndex,
        replayPositionMs = activeStep?.timestampMs ?: 0L,
        railMode = RailMode.Step,
    )
    if (updated != currentState) {
        onStateChanged(updated)
    }
}

private fun String.dryRunEventIndexOrNull(): Int? =
    removePrefix("dry-run-")
        .takeIf { it != this }
        ?.toIntOrNull()

private fun activeRuntimeSourceLine(
    workflowState: WorkspaceWorkflowState,
    runtimeSnapshot: FlowRuntimeSnapshot?,
    visibleScriptText: String,
    projectedScriptText: String,
): Int? {
    val activeNodeId = runtimeSnapshot?.activeNodeId ?: return null
    val node = workflowState.flowchartProjection.graph.nodes.firstOrNull { it.id == activeNodeId } ?: return null
    node.properties.textValue("sourceLine")?.toDoubleOrNull()?.toInt()?.let { return it }
    val commandName = node.properties.textValue("commandName")
        ?: node.properties.textValue("command")
        ?: node.properties.textValue("blockType")
            ?.let { blockType -> VisualTaskerCommandCatalog.findByBlockType(blockType)?.canonicalName }
        ?: return null
    return findCommandLine(visibleScriptText, commandName)
        ?: findCommandLine(projectedScriptText, commandName)
}

private fun Map<String, FlowSemanticValue>.textValue(key: String): String? =
    when (val value = this[key]) {
        is FlowSemanticValue.StringValue -> value.value
        is FlowSemanticValue.NumberValue -> value.canonicalValue
        is FlowSemanticValue.BooleanValue -> value.value.toString()
        else -> null
    }

private fun findCommandLine(script: String, commandName: String): Int? {
    if (script.isBlank() || commandName.isBlank()) return null
    val escaped = Regex.escape(commandName)
    val functionCall = Regex("""(^|\s)$escaped\s*\(""", RegexOption.IGNORE_CASE)
    return script
        .lineSequence()
        .withIndex()
        .firstOrNull { (_, line) ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() &&
                !trimmed.startsWith("//") &&
                (functionCall.containsMatchIn(trimmed) || trimmed.equals(commandName, ignoreCase = true))
        }
        ?.let { it.index + 1 }
}

private data class RecorderTimelinePoint(
    val step: RecorderStepUi,
    val startMs: Long,
    val endMs: Long,
)

private data class RecorderActivitySegment(
    val label: String,
    val startMs: Long,
    val endMs: Long,
)

private data class RecorderStepListEntry(
    val index: Int,
    val step: RecorderStepUi,
    val point: RecorderTimelinePoint,
)

private data class RecorderActivityStepGroup(
    val key: String,
    val label: String,
    val startMs: Long,
    val endMs: Long,
    val steps: List<RecorderStepListEntry>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecorderStepListRow(
    step: RecorderStepUi,
    index: Int,
    safeIndex: Int,
    selectedStepId: String?,
    timelinePoints: List<RecorderTimelinePoint>,
    onSelect: (Int, RecorderStepUi) -> Unit,
    onDelete: (RecorderStepUi) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onDragDelta: (Float) -> Pair<Int, Int>?,
    onDragFinished: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete(step)
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
                    .clickable { onSelect(index, step) }
                    .pointerInput(step.id, timelinePoints.size) {
                        detectDragGesturesAfterLongPress(
                            onDragEnd = onDragFinished,
                            onDragCancel = onDragFinished,
                            onDrag = { change, drag ->
                                change.consume()
                                onDragDelta(drag.y)?.let { (from, to) -> onReorder(from, to) }
                            }
                        )
                    },
                shape = RoundedCornerShape(9.dp),
                color = if (selectedStepId == step.id) {
                    MaterialTheme.colorScheme.primaryContainer
                } else if (index == safeIndex) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.74f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(step.label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Intent: ${step.actionType} - Result: ${step.status.name}",
                            color = statusColor(step.status),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        step.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                            Text(
                                "Observed: $detail",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    AssistChip(
                        onClick = { onSelect(index, step) },
                        label = { Text("Select") }
                    )
                }
            }
        }
    )
}

@Composable
private fun RecorderTimeline(
    steps: List<RecorderStepUi>,
    activeIndex: Int,
    selectedStepId: String?,
    replayPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val passiveTrack = MaterialTheme.colorScheme.tertiaryContainer
    val passiveTrackAlt = MaterialTheme.colorScheme.secondaryContainer
    val timelinePoints = remember(steps) { buildRecorderTimelinePoints(steps) }
    val activitySegments = remember(timelinePoints) { buildRecorderActivitySegments(timelinePoints) }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(surface.copy(alpha = 0.48f))
            .pointerInput(timelinePoints) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (steps.isNotEmpty()) {
                        val timelineStart = timelinePoints.firstOrNull()?.startMs ?: 0L
                        val timelineEnd = timelinePoints.maxOfOrNull { it.endMs }?.coerceAtLeast(timelineStart + 1L) ?: 1L
                        val fraction = (down.position.x / size.width).coerceIn(0f, 1f)
                        val targetTime = timelineStart + ((timelineEnd - timelineStart) * fraction).toLong()
                        onSeek(targetTime)
                    }
                    waitForUpOrCancellation()
                }
            }
    ) {
        val labelPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 10.dp.toPx()
            color = onSurface.toArgb()
        }
        val smallLabelPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 9.dp.toPx()
            color = onSurfaceVariant.toArgb()
        }
        val activeCenterY = size.height * 0.31f
        val durationCenterY = size.height * 0.48f
        val passiveTop = size.height * 0.58f
        val passiveBottom = size.height - 14f
        val passiveCenterY = (passiveTop + passiveBottom) * 0.5f
        val left = 18f
        val right = size.width - 18f
        val timelineStart = timelinePoints.firstOrNull()?.startMs ?: 0L
        val timelineEnd = timelinePoints.maxOfOrNull { it.endMs }?.coerceAtLeast(timelineStart + 1L) ?: 1L
        fun xForTime(timeMs: Long): Float {
            val fraction = ((timeMs - timelineStart).toFloat() / (timelineEnd - timelineStart).toFloat()).coerceIn(0f, 1f)
            return left + (right - left) * fraction
        }

        drawLine(
            color = outline.copy(alpha = 0.70f),
            start = Offset(left, activeCenterY),
            end = Offset(right, activeCenterY),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = outline.copy(alpha = 0.64f),
            start = Offset(left, durationCenterY),
            end = Offset(right, durationCenterY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round,
        )
        drawContext.canvas.nativeCanvas.drawText("Actions", left, 13.dp.toPx(), smallLabelPaint)
        drawContext.canvas.nativeCanvas.drawText("${(timelineStart / 1_000f).formatTimelineSeconds()}", left, durationCenterY - 7f, smallLabelPaint)
        drawContext.canvas.nativeCanvas.drawText("Activity", left, passiveTop - 5f, smallLabelPaint)
        if (steps.isEmpty()) return@Canvas
        val playheadX = xForTime(replayPositionMs.coerceIn(timelineStart, timelineEnd))
        drawLine(
            color = primary.copy(alpha = 0.90f),
            start = Offset(left, durationCenterY),
            end = Offset(playheadX, durationCenterY),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = primary,
            radius = 5.2f,
            center = Offset(playheadX, durationCenterY),
        )

        activitySegments.forEachIndexed { index, segment ->
            val x1 = xForTime(segment.startMs).coerceIn(left, right)
            val x2 = xForTime(segment.endMs).coerceIn(left, right).coerceAtLeast(x1 + 8f)
            val color = if (index % 2 == 0) passiveTrack else passiveTrackAlt
            drawRoundRect(
                color = color.copy(alpha = 0.76f),
                topLeft = Offset(x1, passiveTop),
                size = androidx.compose.ui.geometry.Size((x2 - x1).coerceAtLeast(10f), passiveBottom - passiveTop),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            )
            drawLine(
                color = outline.copy(alpha = 0.75f),
                start = Offset(x1, passiveTop - 4f),
                end = Offset(x1, passiveBottom + 4f),
                strokeWidth = 1.3f,
            )
            val available = (x2 - x1 - 8f).coerceAtLeast(0f)
            if (available > 34f) {
                val label = segment.label.take((available / 7f).toInt().coerceAtLeast(4))
                drawContext.canvas.nativeCanvas.drawText(label, x1 + 5f, passiveCenterY + 3.5f, labelPaint)
            }
        }
        val lastSegment = activitySegments.lastOrNull()
        if (lastSegment != null) {
            val x = xForTime(lastSegment.endMs)
            drawLine(
                color = outline.copy(alpha = 0.75f),
                start = Offset(x, passiveTop - 4f),
                end = Offset(x, passiveBottom + 4f),
                strokeWidth = 1.3f,
            )
        }

        timelinePoints.forEachIndexed { index, point ->
            val step = point.step
            val x = if (timelinePoints.size == 1) size.width * 0.5f else xForTime(point.startMs)
            val selected = step.id == selectedStepId
            val active = index == activeIndex
            val radius = when {
                selected -> 9f
                active -> 8f
                else -> 6f
            }
            drawCircle(
                color = statusColor(step.status).copy(alpha = if (active || selected) 1f else 0.72f),
                radius = radius,
                center = Offset(x, activeCenterY),
            )
            if (active || selected) {
                drawCircle(
                    color = primary.copy(alpha = 0.72f),
                    radius = radius + 5f,
                    center = Offset(x, activeCenterY),
                    style = Stroke(width = 2f),
                )
            }
            drawLine(
                color = statusColor(step.status).copy(alpha = 0.55f),
                start = Offset(x, activeCenterY + radius + 5f),
                end = Offset(x, passiveTop - 5f),
                strokeWidth = 1.4f,
            )
        }
        drawLine(
            color = primary.copy(alpha = 0.82f),
            start = Offset(playheadX, 8f),
            end = Offset(playheadX, passiveBottom + 7f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = primary,
            radius = 4f,
            center = Offset(playheadX, 8f),
        )
    }
}

@Composable
private fun RailTraceTimeline(
    projection: RailProjection,
    steps: List<RecorderStepUi>,
    activeIndex: Int,
    selectedStepId: String?,
    replayPositionMs: Long,
    zoom: Float,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timelinePoints = remember(steps) { buildRecorderTimelinePoints(steps) }
    val activitySegments = remember(timelinePoints) { buildRecorderActivitySegments(timelinePoints) }
    val minWidth = 360.dp
    val timelineWidth = maxOf(minWidth, ((steps.size.coerceAtLeast(8) * 42f * zoom.coerceIn(0.5f, 4f))).dp)
    val timelineScrollState = rememberScrollState()
    val density = LocalDensity.current
    val timelineStartMs = timelinePoints.firstOrNull()?.startMs ?: 0L
    val timelineEndMs = timelinePoints.maxOfOrNull { it.endMs }?.coerceAtLeast(timelineStartMs + 1L) ?: 1L
    LaunchedEffect(activeIndex, replayPositionMs, timelineWidth, timelineScrollState.maxValue) {
        if (timelinePoints.isEmpty() || timelineScrollState.maxValue <= 0) return@LaunchedEffect
        val fraction = if (projection.scaleMode == RailScaleMode.Logical) {
            activeIndex.toFloat() / (timelinePoints.size - 1).coerceAtLeast(1).toFloat()
        } else {
            ((replayPositionMs - timelineStartMs).toFloat() / (timelineEndMs - timelineStartMs).toFloat()).coerceIn(0f, 1f)
        }
        val target = with(density) { timelineWidth.toPx() * fraction - 140.dp.toPx() }
        timelineScrollState.animateScrollTo(target.roundToInt().coerceIn(0, timelineScrollState.maxValue))
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.44f))
    ) {
        RailTraceLaneLabels(
            tracks = projection.tracks.take(5),
            modifier = Modifier
                .width(78.dp)
                .fillMaxHeight(),
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight().horizontalScroll(timelineScrollState)) {
            RailTraceTimelineCanvas(
                projection = projection,
                timelinePoints = timelinePoints,
                activitySegments = activitySegments,
                activeIndex = activeIndex,
                selectedStepId = selectedStepId,
                replayPositionMs = replayPositionMs,
                onSeek = onSeek,
                modifier = Modifier
                    .width(timelineWidth)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun RailTraceLaneLabels(
    tracks: List<RailTrack>,
    modifier: Modifier = Modifier,
) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f))
            .padding(start = 6.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Scene", style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1)
        tracks.forEach { track ->
            Text(
                track.label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RailTraceTimelineCanvas(
    projection: RailProjection,
    timelinePoints: List<RecorderTimelinePoint>,
    activitySegments: List<RecorderActivitySegment>,
    activeIndex: Int,
    selectedStepId: String?,
    replayPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val scenePrimary = MaterialTheme.colorScheme.secondaryContainer
    val sceneSecondary = MaterialTheme.colorScheme.tertiaryContainer
    val trackColors = mapOf(
        RailTrackKind.Workflow to MaterialTheme.colorScheme.primary,
        RailTrackKind.Runtime to MaterialTheme.colorScheme.tertiary,
        RailTrackKind.Events to MaterialTheme.colorScheme.secondary,
        RailTrackKind.Data to MaterialTheme.colorScheme.error,
        RailTrackKind.Observation to MaterialTheme.colorScheme.primaryContainer,
        RailTrackKind.Worldview to MaterialTheme.colorScheme.tertiaryContainer,
    )
    Canvas(
        modifier = modifier.pointerInput(timelinePoints) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (timelinePoints.isNotEmpty()) {
                    val timelineStart = timelinePoints.firstOrNull()?.startMs ?: 0L
                    val timelineEnd = timelinePoints.maxOfOrNull { it.endMs }?.coerceAtLeast(timelineStart + 1L) ?: 1L
                    val fraction = (down.position.x / size.width).coerceIn(0f, 1f)
                    val targetTime = timelineStart + ((timelineEnd - timelineStart) * fraction).toLong()
                    onSeek(targetTime)
                }
                waitForUpOrCancellation()
            }
        }
    ) {
        val labelPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 10.dp.toPx()
            color = onSurface.toArgb()
        }
        val smallLabelPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 9.dp.toPx()
            color = onSurfaceVariant.toArgb()
        }
        val left = 18f
        val right = size.width - 18f
        val timelineStart = timelinePoints.firstOrNull()?.startMs ?: 0L
        val timelineEnd = timelinePoints.maxOfOrNull { it.endMs }?.coerceAtLeast(timelineStart + 1L) ?: 1L
        fun xForTime(timeMs: Long): Float {
            if (projection.scaleMode == RailScaleMode.Logical && timelinePoints.isNotEmpty()) {
                val index = timelinePoints.indexOfFirst { timeMs <= it.endMs }.coerceAtLeast(0)
                val denom = (timelinePoints.size - 1).coerceAtLeast(1).toFloat()
                return left + (right - left) * (index / denom)
            }
            val fraction = ((timeMs - timelineStart).toFloat() / (timelineEnd - timelineStart).toFloat()).coerceIn(0f, 1f)
            return left + (right - left) * fraction
        }
        val sceneTop = 8f
        val sceneBottom = 30f
        val sceneCenterY = (sceneTop + sceneBottom) * 0.5f
        activitySegments.forEachIndexed { index, segment ->
            val x1 = xForTime(segment.startMs).coerceIn(left, right)
            val x2 = xForTime(segment.endMs).coerceIn(left, right).coerceAtLeast(x1 + 10f)
            val color = if (index % 2 == 0) scenePrimary else sceneSecondary
            drawRoundRect(
                color = color.copy(alpha = 0.72f),
                topLeft = Offset(x1, sceneTop),
                size = androidx.compose.ui.geometry.Size(x2 - x1, sceneBottom - sceneTop),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            )
            val labelSpace = x2 - x1 - 8f
            if (labelSpace > 34f) {
                drawContext.canvas.nativeCanvas.drawText(
                    segment.label.take((labelSpace / 7f).toInt().coerceAtLeast(4)),
                    x1 + 5f,
                    sceneCenterY + 3.5f,
                    smallLabelPaint,
                )
            }
        }
        val tracks = projection.tracks.take(5)
        val laneTop = 38f
        val trackGap = if (tracks.isEmpty()) 26f else (size.height - laneTop - 10f) / tracks.size.coerceAtLeast(1)
        tracks.forEachIndexed { trackIndex, track ->
            val y = laneTop + trackGap * trackIndex + trackGap * 0.44f
            val color = trackColors[track.kind] ?: primary
            drawLine(
                color = outline.copy(alpha = 0.45f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
            track.items.forEach { item ->
                val point = timelinePoints.firstOrNull { it.step.id == item.sourceId } ?: return@forEach
                val x = xForTime(point.startMs)
                val selected = item.sourceId == selectedStepId
                val active = timelinePoints.indexOf(point) == activeIndex
                val marker = railTimelineMarkerFor(track.kind, item.status, active = active, selected = selected)
                val markerPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textSize = if (active || selected) 16.dp.toPx() else 13.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    this.color = railTimelineStatusColor(track.kind, item.status, color).toArgb()
                }
                val endX = xForTime(item.endMs ?: point.endMs).coerceAtLeast(x + 4f)
                if (projection.scaleMode == RailScaleMode.Temporal && endX - x > 10f) {
                    drawLine(
                        color = color.copy(alpha = if (active || selected) 0.95f else 0.56f),
                        start = Offset(x, y),
                        end = Offset(endX, y),
                        strokeWidth = if (active || selected) 5f else 3f,
                        cap = StrokeCap.Round,
                    )
                }
                if (active || selected) {
                    drawCircle(
                        color = primary.copy(alpha = 0.70f),
                        radius = 10.5f,
                        center = Offset(x, y),
                        style = Stroke(width = 2f),
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(marker.symbol, x, y + 4.5f, markerPaint)
                if ((selected || active) && item.label.isNotBlank()) {
                    drawContext.canvas.nativeCanvas.drawText(item.label.take(18), x + 9f, y - 8f, labelPaint)
                }
            }
        }
        if (timelinePoints.isNotEmpty()) {
            val playheadX = xForTime(replayPositionMs.coerceIn(timelineStart, timelineEnd))
            drawLine(
                color = primary.copy(alpha = 0.88f),
                start = Offset(playheadX, 8f),
                end = Offset(playheadX, size.height - 8f),
                strokeWidth = 1.8f,
                cap = StrokeCap.Round,
            )
            drawCircle(color = primary, radius = 4f, center = Offset(playheadX, 8f))
        }
    }
}

private fun buildRecorderTimelinePoints(steps: List<RecorderStepUi>): List<RecorderTimelinePoint> =
    steps.mapIndexed { index, step ->
        val fallbackStart = index * 1_000L
        val start = step.timestampMs ?: fallbackStart
        val fallbackEnd = steps.getOrNull(index + 1)?.timestampMs ?: (start + 700L)
        val end = (step.durationMs?.let { start + it } ?: fallbackEnd).coerceAtLeast(start + 80L)
        RecorderTimelinePoint(step, start, end)
    }

private fun railTimelineMarkerFor(
    trackKind: RailTrackKind,
    status: com.visualtasker.wss.workspace.model.RailItemStatus,
    active: Boolean,
    selected: Boolean,
): RailTimelineMarker =
    when {
        active || selected -> RailTimelineMarker.Active
        status == com.visualtasker.wss.workspace.model.RailItemStatus.Invalid ||
            status == com.visualtasker.wss.workspace.model.RailItemStatus.Failed -> RailTimelineMarker.Warning
        trackKind == RailTrackKind.Data -> RailTimelineMarker.Data
        status == com.visualtasker.wss.workspace.model.RailItemStatus.Recorded -> RailTimelineMarker.Recorded
        else -> RailTimelineMarker.Pending
    }

private fun railTimelineStatusColor(
    trackKind: RailTrackKind,
    status: com.visualtasker.wss.workspace.model.RailItemStatus,
    fallback: Color,
): Color =
    when {
        status == com.visualtasker.wss.workspace.model.RailItemStatus.Invalid ||
            status == com.visualtasker.wss.workspace.model.RailItemStatus.Failed -> Color(0xFFFFC857)
        status == com.visualtasker.wss.workspace.model.RailItemStatus.Success -> Color(0xFF52D273)
        status == com.visualtasker.wss.workspace.model.RailItemStatus.Cancelled -> Color(0xFFE57373)
        status == com.visualtasker.wss.workspace.model.RailItemStatus.Running -> Color(0xFF64B5F6)
        trackKind == RailTrackKind.Data -> Color(0xFFB388FF)
        else -> fallback
    }

private fun buildRecorderActivitySegments(points: List<RecorderTimelinePoint>): List<RecorderActivitySegment> {
    if (points.isEmpty()) return emptyList()
    val segments = mutableListOf<RecorderActivitySegment>()
    var label = points.first().step.activityName?.takeIf { it.isNotBlank() } ?: "Unknown Activity"
    var start = points.first().startMs
    var end = points.first().endMs
    points.drop(1).forEach { point ->
        val nextLabel = point.step.activityName?.takeIf { it.isNotBlank() } ?: label
        if (nextLabel == label) {
            end = maxOf(end, point.endMs)
        } else {
            segments += RecorderActivitySegment(label, start, maxOf(end, point.startMs))
            label = nextLabel
            start = point.startMs
            end = point.endMs
        }
    }
    segments += RecorderActivitySegment(label, start, end)
    return segments
}

private fun buildRecorderActivityStepGroups(
    steps: List<RecorderStepUi>,
    points: List<RecorderTimelinePoint>,
): List<RecorderActivityStepGroup> {
    if (steps.isEmpty()) return emptyList()
    val entries = steps.mapIndexed { index, step ->
        RecorderStepListEntry(
            index = index,
            step = step,
            point = points.getOrNull(index) ?: RecorderTimelinePoint(step, index * 1_000L, index * 1_000L + 700L),
        )
    }
    val groups = mutableListOf<RecorderActivityStepGroup>()
    var current = mutableListOf(entries.first())
    var label = entries.first().step.activityName?.takeIf { it.isNotBlank() } ?: "Unknown Activity"
    entries.drop(1).forEach { entry ->
        val nextLabel = entry.step.activityName?.takeIf { it.isNotBlank() } ?: label
        if (nextLabel == label) {
            current += entry
        } else {
            groups += current.toActivityStepGroup(label)
            label = nextLabel
            current = mutableListOf(entry)
        }
    }
    groups += current.toActivityStepGroup(label)
    return groups
}

private fun List<RecorderStepListEntry>.toActivityStepGroup(label: String): RecorderActivityStepGroup {
    val start = minOf { it.point.startMs }
    val end = maxOf { it.point.endMs }
    return RecorderActivityStepGroup(
        key = "$label:$start:$end:${first().step.id}",
        label = label,
        startMs = start,
        endMs = end,
        steps = this,
    )
}

private fun nearestTimelineIndex(points: List<RecorderTimelinePoint>, positionMs: Long): Int =
    points
        .mapIndexed { index, point ->
            val diff = if (point.startMs >= positionMs) point.startMs - positionMs else positionMs - point.startMs
            index to diff
        }
        .minByOrNull { it.second }
        ?.first
        ?: 0

private fun List<Float>.indexOfClosest(value: Float): Int =
    mapIndexed { index, candidate -> index to abs(candidate - value) }
        .minByOrNull { it.second }
        ?.first
        ?: 0

private fun Float.formatTimelineSeconds(): String =
    if (this < 1f) {
        "${(this * 1_000f).toInt()}ms"
    } else {
        "${"%.1f".format(this)}s"
    }

private fun Long.formatTimelineMillis(): String =
    if (this < 1_000L) {
        "${this}ms"
    } else {
        "${"%.1f".format(this / 1_000f)}s"
    }

private fun Float.formatSpeedStep(): String =
    if (this == 1f || this == 2f || this == 4f) {
        this.toInt().toString()
    } else {
        "%.1f".format(this)
    }

@Composable
private fun PanelTypeSwitchButton(
    currentType: PanelType,
    onSelect: (PanelType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TooltipIconButton(
            tooltip = "Panelinhalt wechseln",
            onClick = { expanded = true },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = iconForPanelType(currentType),
                contentDescription = "Panelinhalt wechseln",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            supportedWorkspacePanelTypes
                .sortedBy { displayNameForPanelType(it) }
                .forEach { type ->
                    DropdownMenuItem(
                        text = { Text(displayNameForPanelType(type)) },
                        leadingIcon = { Icon(iconForPanelType(type), contentDescription = null) },
                        enabled = type != currentType,
                        onClick = {
                            expanded = false
                            onSelect(type)
                        },
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
    val actions = DefaultEditorInteractionPolicy.actionsFor(EditorProjection.BlockEditor).mapNotNull { descriptor ->
        descriptor.toBlockEditorRailAction(
            session = session,
            onExpandRequested = onExpandRequested,
            onSave = onSave,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        WorkspaceRailActionList(actions)
        Spacer(modifier = Modifier.weight(1f))
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

private fun EditorActionDescriptor.toBlockEditorRailAction(
    session: BlockEditorShellEditorSession?,
    onExpandRequested: () -> Unit,
    onSave: () -> Unit,
): WorkspaceRailActionSpec? {
    val controller = session?.controller
    val hasSession = session != null
    return when (id) {
        EditorActionId.Save -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession, onClick = onSave)
        EditorActionId.Undo -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession) { controller?.undo() }
        EditorActionId.Redo -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession) { controller?.redo() }
        EditorActionId.AutoArrange -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession) {
            controller?.autoArrangeWorkspace()
        }
        EditorActionId.ToggleCollapse -> WorkspaceRailActionSpec(
            label = if (controller?.selectedBlockCollapsed == true) "Ausklappen" else "Einklappen",
            icon = if (controller?.selectedBlockCollapsed == true) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            enabled = controller?.canToggleSelectedBlockCollapse == true,
            selected = controller?.selectedBlockCollapsed == true,
        ) {
            controller?.toggleSelectedBlockCollapse()
        }
        EditorActionId.DeleteSelection -> WorkspaceRailActionSpec(
            label = "Block löschen",
            icon = editorActionIcon(id),
            enabled = controller?.selectedBlockIds?.isNotEmpty() == true,
        ) {
            controller?.deleteSelectedBlock()
        }
        EditorActionId.OpenBlockDesigner -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession) {
            controller?.openBlockFactory()
        }
        EditorActionId.ClearWorkspace -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession) {
            controller?.clearWorkspace()
        }
        EditorActionId.OpenPalette -> WorkspaceRailActionSpec("Blockpalette", editorActionIcon(id), enabled = hasSession) {
            if (controller?.expandedCategory == null) {
                controller?.onCategoryClick(BlockCategories.ACTION)
            }
            onExpandRequested()
        }
        EditorActionId.RunDry,
        EditorActionId.RunLive,
        EditorActionId.ZoomIn,
        EditorActionId.ZoomOut,
        EditorActionId.FitViewport,
        EditorActionId.ToggleDataFlow,
        EditorActionId.ToggleRuntime,
        EditorActionId.ToggleDiagnostics,
        EditorActionId.StepBack,
        EditorActionId.StepForward -> null
    }
}

private fun EditorActionDescriptor.toFlowchartRailAction(
    session: FlowchartShellEditorSession?,
    panelSizePx: IntSize,
    selectedNodeId: FlowNodeId?,
    selectedEdgeId: FlowEdgeId?,
    onExpandRequested: () -> Unit,
    onSave: () -> Unit,
    dataFlowVisible: Boolean,
    runtimeVisible: Boolean,
    diagnosticsVisible: Boolean,
    onDataFlowToggle: () -> Unit,
    onRuntimeToggle: () -> Unit,
    onDiagnosticsToggle: () -> Unit,
    onDeleteNode: (FlowNodeId) -> Unit,
    onDisconnectEdge: (FlowEdgeId) -> Unit,
    onUndoWorkspace: () -> Boolean,
    onRedoWorkspace: () -> Boolean,
): WorkspaceRailActionSpec? {
    val controller = session?.controller
    val hasSession = session != null
    return when (id) {
        EditorActionId.Save -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession, onClick = onSave)
        EditorActionId.Undo -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession) {
            if (!onUndoWorkspace()) controller?.dispatch(FlowInteractionAction.UndoViewChange)
        }
        EditorActionId.Redo -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession) {
            if (!onRedoWorkspace()) controller?.dispatch(FlowInteractionAction.RedoViewChange)
        }
        EditorActionId.AutoArrange -> WorkspaceRailActionSpec(label, editorActionIcon(id), enabled = hasSession) {
            controller?.replaceLayout(
                FlowLayoutConfig(
                    layerSpacing = 104.0,
                    nodeSpacing = 64.0,
                    componentSpacing = 128.0,
                    routingClearance = 28.0,
                    wrapAfterNodes = 11,
                    semanticWrapEnabled = true,
                    pinnedNodePolicy = FlowPinnedNodePolicy.IGNORE,
                )
            )
        }
        EditorActionId.ToggleDataFlow -> WorkspaceRailActionSpec(
            if (dataFlowVisible) "Dataflow aus" else "Dataflow an",
            if (dataFlowVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            enabled = hasSession,
            onClick = onDataFlowToggle,
        )
        EditorActionId.ToggleRuntime -> WorkspaceRailActionSpec(
            if (runtimeVisible) "Runtime aus" else "Runtime an",
            if (runtimeVisible) Icons.Default.PlayCircle else Icons.Default.PlayArrow,
            enabled = hasSession,
            onClick = onRuntimeToggle,
        )
        EditorActionId.ToggleDiagnostics -> WorkspaceRailActionSpec(
            if (diagnosticsVisible) "Diagnose aus" else "Diagnose an",
            if (diagnosticsVisible) Icons.Default.Warning else Icons.Default.Info,
            enabled = hasSession,
            onClick = onDiagnosticsToggle,
        )
        EditorActionId.DeleteSelection -> WorkspaceRailActionSpec(
            label = if (selectedEdgeId != null) "Kante löschen" else "Node löschen",
            icon = editorActionIcon(id),
            enabled = hasSession && (selectedNodeId != null || selectedEdgeId != null),
        ) {
            selectedNodeId?.let(onDeleteNode) ?: selectedEdgeId?.let(onDisconnectEdge)
        }
        EditorActionId.OpenPalette -> WorkspaceRailActionSpec("Node-Palette", editorActionIcon(id), enabled = hasSession, onClick = onExpandRequested)
        EditorActionId.ZoomIn,
        EditorActionId.ZoomOut,
        EditorActionId.FitViewport,
        EditorActionId.RunDry,
        EditorActionId.RunLive,
        EditorActionId.StepBack,
        EditorActionId.StepForward,
        EditorActionId.ToggleCollapse,
        EditorActionId.OpenBlockDesigner,
        EditorActionId.ClearWorkspace -> null
    }
}

private fun editorActionIcon(id: EditorActionId): androidx.compose.ui.graphics.vector.ImageVector =
    when (id) {
        EditorActionId.Save -> Icons.Default.Save
        EditorActionId.Undo -> Icons.AutoMirrored.Filled.Undo
        EditorActionId.Redo -> Icons.AutoMirrored.Filled.Redo
        EditorActionId.ZoomIn -> Icons.Default.ZoomIn
        EditorActionId.ZoomOut -> Icons.Default.ZoomOut
        EditorActionId.FitViewport -> Icons.Default.CenterFocusStrong
        EditorActionId.AutoArrange -> Icons.Default.AutoAwesomeMosaic
        EditorActionId.ToggleCollapse -> Icons.Default.VisibilityOff
        EditorActionId.ToggleDataFlow -> Icons.Default.Visibility
        EditorActionId.ToggleRuntime,
        EditorActionId.RunLive -> Icons.Default.PlayCircle
        EditorActionId.ToggleDiagnostics -> Icons.Default.Info
        EditorActionId.RunDry -> Icons.Default.PlayArrow
        EditorActionId.StepBack -> Icons.Default.ArrowBack
        EditorActionId.StepForward -> Icons.Default.ArrowForward
        EditorActionId.DeleteSelection,
        EditorActionId.ClearWorkspace -> Icons.Default.DeleteSweep
        EditorActionId.OpenBlockDesigner -> Icons.Default.GridView
        EditorActionId.OpenPalette -> Icons.Default.AddCircle
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
    panelSizePx: IntSize,
    selectedNodeId: FlowNodeId?,
    selectedEdgeId: FlowEdgeId?,
    onExpandRequested: () -> Unit,
    onSave: () -> Unit,
    dataFlowVisible: Boolean,
    runtimeVisible: Boolean,
    diagnosticsVisible: Boolean,
    onDataFlowToggle: () -> Unit,
    onRuntimeToggle: () -> Unit,
    onDiagnosticsToggle: () -> Unit,
    onDeleteNode: (FlowNodeId) -> Unit,
    onDisconnectEdge: (FlowEdgeId) -> Unit,
    onUndoWorkspace: () -> Boolean,
    onRedoWorkspace: () -> Boolean,
) {
    val actions = DefaultEditorInteractionPolicy.actionsFor(EditorProjection.Flowchart).mapNotNull { descriptor ->
        descriptor.toFlowchartRailAction(
            session = session,
            panelSizePx = panelSizePx,
            selectedNodeId = selectedNodeId,
            selectedEdgeId = selectedEdgeId,
            onExpandRequested = onExpandRequested,
            onSave = onSave,
            dataFlowVisible = dataFlowVisible,
            runtimeVisible = runtimeVisible,
            diagnosticsVisible = diagnosticsVisible,
            onDataFlowToggle = onDataFlowToggle,
            onRuntimeToggle = onRuntimeToggle,
            onDiagnosticsToggle = onDiagnosticsToggle,
            onDeleteNode = onDeleteNode,
            onDisconnectEdge = onDisconnectEdge,
            onUndoWorkspace = onUndoWorkspace,
            onRedoWorkspace = onRedoWorkspace,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        WorkspaceRailActionList(actions)
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun fitFlowchartViewport(
    view: FlowViewDocument,
    panelSize: IntSize,
): de.visualtasker.flowchart.domain.FlowViewport {
    if (panelSize.width <= 0 || panelSize.height <= 0 || view.nodeViews.isEmpty()) return view.viewport
    val minX = view.nodeViews.minOf { it.position.x }
    val minY = view.nodeViews.minOf { it.position.y }
    val maxX = view.nodeViews.maxOf { it.position.x + (it.size?.width ?: 160.0) }
    val maxY = view.nodeViews.maxOf { it.position.y + (it.size?.height ?: 72.0) }
    val contentWidth = (maxX - minX).coerceAtLeast(1.0)
    val contentHeight = (maxY - minY).coerceAtLeast(1.0)
    val horizontalPadding = 72.0
    val topPadding = 72.0
    val bottomPadding = 156.0
    val availableWidth = (panelSize.width - horizontalPadding * 2.0).coerceAtLeast(120.0)
    val availableHeight = (panelSize.height - topPadding - bottomPadding).coerceAtLeast(120.0)
    val zoom = minOf(1.8, maxOf(0.18, minOf(availableWidth / contentWidth, availableHeight / contentHeight)))
    val pan = FlowPoint(
        x = horizontalPadding + (availableWidth - contentWidth * zoom) / 2.0 - minX * zoom,
        y = topPadding + (availableHeight - contentHeight * zoom) / 2.0 - minY * zoom,
    )
    return de.visualtasker.flowchart.domain.FlowViewport(pan = pan, zoom = zoom)
}

@Composable
private fun FlowchartPanel(
    panelId: String,
    uiPrefs: android.content.SharedPreferences,
    graphContent: String,
    runtimeSnapshot: FlowRuntimeSnapshot?,
    focusedNodeId: FlowNodeId?,
    focusedEdgeId: FlowEdgeId?,
    stepLabel: String?,
    showMiniMap: Boolean,
    dataFlowVisible: Boolean,
    runtimeLayerVisible: Boolean,
    diagnosticsVisible: Boolean,
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
    onDataFlowVisibleChange: (Boolean) -> Unit,
    onRuntimeVisibleChange: (Boolean) -> Unit,
    onDiagnosticsVisibleChange: (Boolean) -> Unit,
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
        focusedNodeId = focusedNodeId,
        focusedEdgeId = focusedEdgeId,
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
        dataFlowVisible = dataFlowVisible,
        runtimeLayerVisible = runtimeLayerVisible,
        diagnosticsVisible = diagnosticsVisible,
        onDataFlowVisibleChange = onDataFlowVisibleChange,
        onRuntimeLayerVisibleChange = onRuntimeVisibleChange,
        onDiagnosticsVisibleChange = onDiagnosticsVisibleChange,
        showTopToolbar = false,
        onSave = { persistFlowchartViewSession(uiPrefs, session) },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimizedDock(
    panels: List<PanelState>,
    focusedPanelId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (panels.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topEnd = 12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(panels, key = { _, panel -> panel.id }) { _, panel ->
                val alpha = when {
                    panel.minimized -> 1f
                    panel.id == focusedPanelId -> 0.76f
                    else -> 0.48f
                }
	                AssistChip(
	                    onClick = { onSelect(panel.id) },
                    leadingIcon = {
                        Icon(
                            imageVector = iconForPanelType(panel.type),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text(displayTitleForPanel(panel)) },
	                    modifier = Modifier
	                        .animateItem()
	                        .background(Color.Transparent)
	                        .clip(RoundedCornerShape(10.dp)),
                    colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                        containerColor = panel.accentColor.copy(alpha = alpha * 0.22f),
                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        leadingIconContentColor = panel.accentColor.copy(alpha = alpha),
                    ),
                    border = BorderStroke(1.dp, panel.accentColor.copy(alpha = alpha * 0.58f)),
                )
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
    workspacePanel("panel-1", PanelType.RecorderSteps, "RailTrace", 84f, 112f, 360f, 360f, 1),
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
    topInsetPx: Float = WORKSPACE_TOP_BAR_HEIGHT_DP + WORKSPACE_PANEL_MARGIN_DP,
    bottomInsetPx: Float = WORKSPACE_MINIMIZED_DOCK_HEIGHT_DP,
) {
    val visible = panels.filter { !it.minimized }.sortedBy { it.zIndex }
    if (visible.isEmpty() || surfaceSize.width <= 0 || surfaceSize.height <= 0) return

    val columns = max(1, ceil(sqrt(visible.size.toFloat())).toInt())
    val gap = 14f
    val availableWidth = surfaceSize.width - 64f
    val availableHeight = surfaceSize.height - topInsetPx - bottomInsetPx
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
        val maxY = max(topInsetPx, surfaceSize.height - bottomInsetPx - targetH)
        val idx = panels.indexOfFirst { it.id == panel.id }
        if (idx >= 0) {
            panels[idx] = panels[idx].copy(
                x = nx,
                y = ny.coerceIn(topInsetPx, maxY),
                width = targetW,
                height = targetH
            )
        }
    }
}

private data class PanelSnapTargets(
    val x: List<Float>,
    val y: List<Float>,
)

private fun panelSnapTargets(
    panels: List<PanelState>,
    activePanel: PanelState,
    density: Float,
    surfaceSize: IntSize,
    topGuardPx: Float,
    bottomGuardPx: Float,
): PanelSnapTargets {
    if (surfaceSize.width <= 0 || surfaceSize.height <= 0) return PanelSnapTargets(emptyList(), emptyList())
    val activeWidthPx = activePanel.width * density
    val activeHeightPx = activePanel.height * density
    val maxX = max(0f, surfaceSize.width - activeWidthPx)
    val maxY = max(topGuardPx, surfaceSize.height - bottomGuardPx - activeHeightPx)
    val xTargets = mutableListOf(0f, maxX)
    val yTargets = mutableListOf(topGuardPx, maxY)

    panels.asSequence()
        .filter { it.id != activePanel.id && !it.minimized }
        .forEach { panel ->
            val left = panel.x
            val top = max(panel.y, topGuardPx)
            val right = panel.x + panel.width * density
            val bottom = top + panel.height * density
            xTargets += left
            xTargets += right
            xTargets += left - activeWidthPx
            xTargets += right - activeWidthPx
            yTargets += top
            yTargets += bottom
            yTargets += top - activeHeightPx
            yTargets += bottom - activeHeightPx
        }

    return PanelSnapTargets(
        x = xTargets.map { it.coerceIn(0f, maxX) }.distinct(),
        y = yTargets.map { it.coerceIn(topGuardPx, maxY) }.distinct(),
    )
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

private fun persistStepperState(
    uiPrefs: android.content.SharedPreferences,
    state: StepperPanelState,
) {
    uiPrefs.edit().putString(STEPPER_STATE_PREF_KEY, state.encode()).apply()
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
    PanelType.M3Director -> Icons.Default.AutoAwesomeMosaic
    PanelType.Vt2Vt -> Icons.Default.SyncAlt
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPanelDialog(
    onSelect: (PanelType) -> Unit,
    onLaunchFloatingOverlay: (FloatingOverlayTarget) -> Unit,
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
        PanelType.M3Director,
        PanelType.Vt2Vt,
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
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(panelTypes) { type ->
                    val accent = defaultAccentForPanelType(type)
                    Surface(
                        onClick = { onSelect(type) },
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                iconForPanelType(type),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                displayNameForPanelType(type),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Floating Overlays", style = MaterialTheme.typography.titleSmall)
            AssistChip(
                onClick = { onLaunchFloatingOverlay(FloatingOverlayTarget.Toolbar) },
                label = { Text("Screenshot Toolbar") },
                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
            )
            AssistChip(
                onClick = { onLaunchFloatingOverlay(FloatingOverlayTarget.Inspector) },
                label = { Text("Floating Inspector") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            )
            AssistChip(
                onClick = { onLaunchFloatingOverlay(FloatingOverlayTarget.Panel) },
                label = { Text("Floating Panel") },
                leadingIcon = { Icon(Icons.Default.ViewKanban, contentDescription = null) },
            )
        }
    }
}

private fun displayNameForPanelType(type: PanelType): String = when (type) {
    PanelType.RecorderSteps -> "RailTrace"
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
    PanelType.M3Director -> "VisualAssets"
    PanelType.Vt2Vt -> "VT2VT"
}

private fun displayTitleForPanel(panel: PanelState): String {
    val canonical = displayNameForPanelType(panel.type)
    val trailingNumber = Regex("^\\s*${Regex.escape(canonical)}\\s+\\d+\\s*$", RegexOption.IGNORE_CASE)
    val generatedPanelTitle = supportedWorkspacePanelTypes.any { type ->
        val name = displayNameForPanelType(type)
        Regex("^\\s*${Regex.escape(name)}(?:\\s+\\d+)?\\s*$", RegexOption.IGNORE_CASE).matches(panel.title)
    } || (panel.type == PanelType.RecorderSteps && Regex("^\\s*Stepper(?:\\s+\\d+)?\\s*$", RegexOption.IGNORE_CASE).matches(panel.title))
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
    PanelType.Vt2Vt -> MainPanelType.LIST_TEST
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
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
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
    chromeTabSettings: CustomChromeTabSettings,
    onChromeTabSettingsChange: (CustomChromeTabSettings) -> Unit,
    onResetPanels: () -> Unit,
    onSaveLayout: () -> Unit,
    onDeleteLayout: () -> Unit,
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
            Tab(selected = tabIndex == 4, onClick = { onTabChange(4) }, text = { Text("ChromeTab") })
            Tab(selected = tabIndex == 5, onClick = { onTabChange(5) }, text = { Text("Plugins & Extras") })
            Tab(selected = tabIndex == 6, onClick = { onTabChange(6) }, text = { Text("Tasker") })
            Tab(selected = tabIndex == 7, onClick = { onTabChange(7) }, text = { Text("Farben") })
            Tab(selected = tabIndex == 8, onClick = { onTabChange(8) }, text = { Text("Keypad") })
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Schriftgrad: ${(fontScale * 100).toInt()}%")
                    Slider(
                        value = fontScale,
                        onValueChange = onFontScaleChange,
                        valueRange = 0.75f..1.6f
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onSaveLayout, modifier = Modifier.weight(1f)) {
                        Text("Layout speichern")
                    }
                    OutlinedButton(onClick = onDeleteLayout, modifier = Modifier.weight(1f)) {
                        Text("Layout löschen")
                    }
                }
                Button(onClick = onAutoArrange) {
                    Text("Panels auto anordnen")
                }
                Text("Standard bleibt 4x4 (kleines Grid).")
            }

            7 -> Column(
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
            3 -> WorkspaceSettingsInfoTab("Texteditor", listOf("Texteditor lädt und speichert EMScript-Drafts und synchronisiert gültige Skripte in den gemeinsamen Workflow."))
            4 -> ChromeTabSettingsTab(
                settings = chromeTabSettings,
                onSettingsChange = onChromeTabSettingsChange,
            )
            5 -> PluginSettingsTab()
            6 -> TaskerSettingsTab()
            8 -> WorkspaceSettingsInfoTab(
                title = "Keypad",
                messages = listOf("Keypad-Mapping wird als eigenes Workspace-Panel/Plugin migriert. Icon-Engine kann hier bereits umgeschaltet werden."),
                actionLabel = "Icon-Engine: ${IconMotionConfig.engine.name}",
                onAction = onToggleIconEngine
            )
            else -> WorkspaceSettingsInfoTab("Farben", listOf("Farboptionen sind im Tab Farben erreichbar."))
        }
    }
}

@Composable
private fun ChromeTabSettingsTab(
    settings: CustomChromeTabSettings,
    onSettingsChange: (CustomChromeTabSettings) -> Unit,
) {
    val context = LocalContext.current
    val status = CustomChromeTabRegistration.inspect(context)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Custom Chrome Tab", style = MaterialTheme.typography.titleSmall)
        Text(status.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        SettingSwitchRow("Titel anzeigen", settings.showTitle) {
            onSettingsChange(settings.copy(showTitle = it))
        }
        SettingSwitchRow("Share aktiv", settings.shareEnabled) {
            onSettingsChange(settings.copy(shareEnabled = it))
        }
        SettingSwitchRow("Download-Menüeintrag", settings.downloadMenuEnabled) {
            onSettingsChange(settings.copy(downloadMenuEnabled = it))
        }
        SettingSwitchRow("Favorit-Menüeintrag", settings.favoriteMenuEnabled) {
            onSettingsChange(settings.copy(favoriteMenuEnabled = it))
        }
        SettingSwitchRow("App-BottomBar anzeigen", settings.bottomBarEnabled) {
            onSettingsChange(settings.copy(bottomBarEnabled = it))
        }
        Text("ActionButton Icon", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("open", "star", "save", "share").forEach { icon ->
                FilterChip(
                    selected = settings.actionButtonIcon == icon,
                    onClick = { onSettingsChange(settings.copy(actionButtonIcon = icon)) },
                    label = { Text(icon) },
                )
            }
        }
        Text("Close Icon", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("close", "back", "x").forEach { icon ->
                FilterChip(
                    selected = settings.closeButtonIcon == icon,
                    onClick = { onSettingsChange(settings.copy(closeButtonIcon = icon)) },
                    label = { Text(icon) },
                )
            }
        }
        ColorAssignmentRow("Toolbar", Color(settings.toolbarColor)) {
            onSettingsChange(settings.copy(toolbarColor = it.toArgb()))
        }
        ColorAssignmentRow("Navigation", Color(settings.navigationBarColor)) {
            onSettingsChange(settings.copy(navigationBarColor = it.toArgb()))
        }
    }
}

@Composable
private fun PluginSettingsTab() {
    val context = LocalContext.current
    val chromeTab = CustomChromeTabRegistration.inspect(context)
    var shizuku by remember { mutableStateOf(ShizukuRegistration.inspect(context)) }
    val termux = TermuxRegistration.inspect(context)
    val tasker = TaskerRegistration.inspect(context)
    val usb = Vt2VtUsbAdbBridge.detect(context)
    LaunchedEffect(context) {
        while (isActive) {
            shizuku = ShizukuRegistration.inspect(context)
            delay(2_000)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Plugin Adapter", style = MaterialTheme.typography.titleSmall)
        PluginStatusRow("CustomChromeTab", chromeTab.supported, chromeTab.summary) {}
        PluginStatusRow("Shizuku Legitimation", shizuku.legitimized, shizuku.summary) {
            context.safeStartActivity(ShizukuRegistration.settingsIntent(shizuku))
        }
        TaskerSettingLine("Shizuku Binder live", shizuku.binderAlive.toString())
        TaskerSettingLine("Shizuku Runtime bereit", shizuku.available.toString())
        PluginStatusRow("Termux", termux.canRunCommands, termux.summary) {
            context.safeStartActivity(TermuxRegistration.settingsIntent(termux))
        }
        PluginStatusRow("Tasker", tasker.available, tasker.summary) {
            context.safeStartActivity(TaskerRegistration.settingsIntent(tasker))
        }
        PluginStatusRow("scrcpy/VT2VT USB", usb.bridgeReady, usb.summary) {}
        HorizontalDivider()
        ExtrasPermissionsContent()
    }
}

@Composable
private fun TaskerSettingsTab() {
    val context = LocalContext.current
    val tasker = TaskerRegistration.inspect(context)
    var settings by remember(context) { mutableStateOf(TaskerPluginSettings.load(context)) }
    var sessionRevision by remember { mutableIntStateOf(0) }
    val latestResult = remember(context, sessionRevision) { TaskerPluginSessionStore.lastResult(context) }
    val latestError = remember(context, sessionRevision) { TaskerPluginSessionStore.lastError(context) }
    val updateSettings: (TaskerPluginSettings) -> Unit = { next ->
        settings = next
        next.save(context)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Tasker", style = MaterialTheme.typography.titleSmall)
        PluginStatusRow("Tasker Adapter", tasker.available, tasker.summary) {
            context.safeStartActivity(TaskerRegistration.settingsIntent(tasker))
        }
        HorizontalDivider()
        Text("Verbindung", style = MaterialTheme.typography.labelLarge)
        TaskerSettingLine("Paket", tasker.packageName ?: "nicht gefunden")
        TaskerSettingLine("Installiert", tasker.installed.toString())
        TaskerSettingLine("Tasker startbar", tasker.launchable.toString())
        TaskerSettingLine("RUN_TASKS Permission", if (tasker.runTaskPermissionGranted) "granted" else "missing")
        TaskerSettingLine("Tasker enabled", tasker.taskerEnabled?.toString() ?: "unbekannt")
        TaskerSettingLine("External Access", tasker.externalAccessAllowed?.toString() ?: "unbekannt")
        TaskerSettingLine("Task Receiver", tasker.receiverAvailable.toString())
        Button(
            onClick = { context.safeStartActivity(TaskerRegistration.settingsIntent(tasker)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (tasker.installed) "Tasker External Access öffnen" else "Tasker installieren/einrichten")
        }
        HorizontalDivider()
        Text("Empfang", style = MaterialTheme.typography.labelLarge)
        SettingSwitchRow("Tasker-Events in RailTrace aufzeichnen", settings.recordToRailTrace) {
            updateSettings(settings.copy(recordToRailTrace = it))
        }
        SettingSwitchRow("Toast-Rückmeldung anzeigen", settings.showToasts) {
            updateSettings(settings.copy(showToasts = it))
        }
        SettingSwitchRow("Workspace bei Open/Script-Draft öffnen", settings.autoOpenWorkspace) {
            updateSettings(settings.copy(autoOpenWorkspace = it))
        }
        HorizontalDivider()
        Text("WSS Plugin Action", style = MaterialTheme.typography.labelLarge)
        Text(
            "Tasker findet WSS unter Plugin > VisualTasker Studio WSS. Bestehende Actions einmal öffnen und speichern, um neue Felder zu übernehmen.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Event-Slots")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TaskerPluginContract.eventSlotLabels.forEach { (_, label) ->
                AssistChip(onClick = {}, label = { Text(label) })
            }
        }
        Text("Statuswerte")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TaskerPluginContract.statusLabels.forEach { (value, label) ->
                AssistChip(onClick = {}, label = { Text("$label/$value") })
            }
        }
        HorizontalDivider()
        Text("Session-Dateien", style = MaterialTheme.typography.labelLarge)
        TaskerSettingLine("Recorder", "emscript-runtime/records/external-tasker-feedback.jsonl")
        TaskerSettingLine("Result/Ack", "emscript-runtime/tasker/tasker-plugin-sessions.jsonl")
        HorizontalDivider()
        Text("Letzte Session", style = MaterialTheme.typography.labelLarge)
        latestResult?.let { result ->
            TaskerSettingLine("LastResult", "${result.status} | ${result.runId} | ${result.eventName} | #${result.ordinal}")
        } ?: Text("Noch kein Tasker Result empfangen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        latestError?.let { error ->
            TaskerSettingLine("LastError", "${error.message} | ${error.runId} | ${error.eventName} | #${error.ordinal}")
        } ?: Text("Kein Tasker Fehler gespeichert.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = {
                TaskerPluginSessionStore.clear(context)
                sessionRevision++
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Tasker Sessions leeren")
        }
    }
}

@Composable
private fun TaskerSettingLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.42f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PluginStatusRow(
    label: String,
    ready: Boolean,
    detail: String,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
            .clickable { onOpen() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(if (ready) Color(0xFF4CAF50) else Color(0xFFFFC857), CircleShape)
        )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ExtrasPermissionsContent()
    }
}

@Composable
private fun ExtrasPermissionsContent() {
    val context = LocalContext.current
    val packageName = context.packageName
    val overlayGranted = Settings.canDrawOverlays(context)
    val fileAccessGranted = isFileAccessGranted(context)
    val batteryGranted = isBatteryOptimizationDisabled(context)
    val notificationsGranted = isNotificationAccessGranted(context)
    val microphoneGranted = hasPermission(context, Manifest.permission.RECORD_AUDIO)
    val accessibilityGranted = isAccessibilityEnabledForApp(context)
    val shizukuStatus = ShizukuRegistration.inspect(context)
    val termuxStatus = TermuxRegistration.inspect(context)
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
            granted = shizukuStatus.legitimized,
            open = {
                context.safeStartActivity(ShizukuRegistration.settingsIntent(shizukuStatus))
            },
        ),
        PermissionEntry(
            label = "Termux RUN_COMMAND",
            granted = termuxStatus.canRunCommands,
            open = {
                context.safeStartActivity(TermuxRegistration.settingsIntent(termuxStatus))
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

private fun workspaceRuntimeCapabilityGate(context: Context): RuntimeCapabilityGate =
    RuntimeCapabilityGate.withDeviceAdapters(
        accessibilityAvailable = VisualTaskerAccessibilityService.isConnected(),
        customChromeTabAvailable = CustomChromeTabRegistration.inspect(context).supported,
        shizukuAvailable = ShizukuRegistration.inspect(context).available,
        termuxAvailable = TermuxRegistration.inspect(context).canRunCommands,
        taskerAvailable = TaskerRegistration.inspect(context).available,
        usbAdbBridgeAvailable = Vt2VtUsbAdbBridge.detect(context).bridgeReady,
    )

private fun RuntimeCapabilityReport.accessibilityBlockedCommands(): List<String> {
    val accessibilityCommands = setOf("click", "clickPoint", "swipe", "screenshot")
    return capabilities
        .filter { it.status == RuntimeCapabilityStatus.BLOCKED }
        .map { it.command }
        .filter { command -> accessibilityCommands.any { it.equals(command, ignoreCase = true) } }
        .distinct()
}

private data class TaskerRunRequest(
    val taskName: String,
    val parameters: List<String>,
    val variables: Map<String, String>,
)

private fun parseTaskerRunRequest(args: List<String>): TaskerRunRequest {
    val taskName = args.firstOrNull().orEmpty().trimLiteral()
    val tail = args.drop(1)
    val parameters = mutableListOf<String>()
    val variables = linkedMapOf<String, String>()
    tail.forEach { raw ->
        val value = raw.trim()
        when {
            value.startsWith("[") && value.endsWith("]") -> {
                splitTopLevel(value.removeSurrounding("[", "]")).forEach { parameters += it.trimLiteral() }
            }
            value.startsWith("{") && value.endsWith("}") -> {
                parseTaskerVariableObject(value).forEach { (name, variableValue) -> variables[name] = variableValue }
            }
            value.isNotBlank() -> parameters += value.trimLiteral()
        }
    }
    return TaskerRunRequest(taskName, parameters, variables)
}

private fun parseTaskerVariableObject(raw: String): Map<String, String> {
    val body = raw.trim().removeSurrounding("{", "}").trim()
    if (body.isBlank()) return emptyMap()
    return splitTopLevel(body).mapNotNull { pair ->
        val separator = pair.indexOfTopLevel(':')
        if (separator < 0) return@mapNotNull null
        val key = pair.substring(0, separator).trimLiteral()
        val value = pair.substring(separator + 1).trimLiteral()
        key.takeIf { it.isNotBlank() }?.let { it to value }
    }.toMap()
}

private fun splitTopLevel(source: String): List<String> {
    if (source.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var bracketDepth = 0
    var braceDepth = 0
    var parenDepth = 0
    var inString = false
    var escaped = false
    source.forEach { char ->
        if (escaped) {
            current.append(char)
            escaped = false
            return@forEach
        }
        if (char == '\\' && inString) {
            current.append(char)
            escaped = true
            return@forEach
        }
        if (char == '"') {
            inString = !inString
            current.append(char)
            return@forEach
        }
        if (!inString) {
            when (char) {
                '[' -> bracketDepth += 1
                ']' -> bracketDepth = (bracketDepth - 1).coerceAtLeast(0)
                '{' -> braceDepth += 1
                '}' -> braceDepth = (braceDepth - 1).coerceAtLeast(0)
                '(' -> parenDepth += 1
                ')' -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
                ',' -> if (bracketDepth == 0 && braceDepth == 0 && parenDepth == 0) {
                    result += current.toString().trim()
                    current.clear()
                    return@forEach
                }
            }
        }
        current.append(char)
    }
    current.toString().trim().takeIf { it.isNotBlank() }?.let(result::add)
    return result
}

private fun String.shellQuote(): String {
    val value = trim()
    if (value.isBlank()) return "''"
    if (value.all { it.isLetterOrDigit() || it in setOf('_', '-', '.', '/', ':') }) return value
    return "'${value.replace("'", "'\"'\"'")}'"
}

private fun String.indexOfTopLevel(target: Char): Int {
    var bracketDepth = 0
    var braceDepth = 0
    var parenDepth = 0
    var inString = false
    var escaped = false
    forEachIndexed { index, char ->
        if (escaped) {
            escaped = false
            return@forEachIndexed
        }
        if (char == '\\' && inString) {
            escaped = true
            return@forEachIndexed
        }
        if (char == '"') {
            inString = !inString
            return@forEachIndexed
        }
        if (!inString) {
            when (char) {
                '[' -> bracketDepth += 1
                ']' -> bracketDepth = (bracketDepth - 1).coerceAtLeast(0)
                '{' -> braceDepth += 1
                '}' -> braceDepth = (braceDepth - 1).coerceAtLeast(0)
                '(' -> parenDepth += 1
                ')' -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
                target -> if (bracketDepth == 0 && braceDepth == 0 && parenDepth == 0) return index
            }
        }
    }
    return -1
}

private fun String.trimLiteral(): String =
    trim()
        .removeSurrounding("\"")
        .replace("\\\"", "\"")

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
