package com.visualtasker.wss.workspace.ui

import android.app.Activity
import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.visualtasker.wss.components.DarkPanel
import com.visualtasker.wss.components.FabAction
import com.visualtasker.wss.components.M3EExpandableFAB
import com.visualtasker.wss.data.PanelState as MainPanelState
import com.visualtasker.wss.data.PanelType as MainPanelType
import com.visualtasker.wss.flowchart.BlockEditorFlowchartProjector
import com.visualtasker.wss.grid.GridSystem
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
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartShellPanel
import com.visualtasker.wss.workspace.plugin.flowchart.FlowchartShellPlugin
import com.visualtasker.wss.ui.theme.M3EColors
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.blockeditor.compose.ui.CategoryPalettePanel
import de.visualtasker.blockeditor.compose.ui.EditorNavigationRail
import de.visualtasker.blockeditor.serialization.BlockEditorDocumentFormats
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.flowchart.serialization.FlowGraphJsonCodec
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
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
private const val BLOCKEDITOR_WORKSPACE_PREF_KEY = "blockeditor_workspace_json"

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
    var showAddPanelDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var settingsTab by remember { mutableIntStateOf(2) }
    var workspaceJson by remember(uiPrefs) { mutableStateOf(loadBlockEditorWorkspaceJson(uiPrefs)) }
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, uiScale) {
        Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * uiScale
        )
    }
    val density = scaledDensity.density
    val gridSizeDp = if (useLargeGrid) GridSystem.GRID_SIZE_DP_LARGE else GridSystem.GRID_SIZE_DP_SMALL

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
                y = 96f,
                width = PANEL_DEFAULT_W,
                height = PANEL_DEFAULT_H,
                zIndex = nextZ++,
                minimized = false,
                accentColor = defaultAccentForPanelType(type)
            )
        )
        focusedPanelId = id
        bridge.onPanelAction(PanelAction.OpenPanel(type))
    }

    LaunchedEffect(Unit) {
        snapshotFlow { WorkspaceSessionSnapshot(panels = panels.toList()) }
            .debounce(300)
            .collect { sessionStore.save(it) }
    }
    LaunchedEffect(hideSystemBars, dockAtTop, useLargeGrid, snapEnabled, uiScale) {
        uiPrefs.edit()
            .putBoolean("hide_system_bars", hideSystemBars)
            .putBoolean("dock_top", dockAtTop)
            .putBoolean("grid_large", useLargeGrid)
            .putBoolean("snap_enabled", snapEnabled)
            .putFloat("ui_scale", uiScale)
            .apply()
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

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onSizeChanged { surfaceSize = it }
        ) {
            GridBackground(visible = snapEnabled, stepDp = gridSizeDp.toFloat())

        panels.sortedBy { it.zIndex }.forEach { panel ->
            if (panel.minimized) return@forEach
            key(panel.id) {
                val maxWidthDp = ((surfaceSize.width - panel.x - 16f) / density).toInt().coerceAtLeast(PANEL_MIN_W.toInt())
                val maxHeightDp = ((surfaceSize.height - panel.y - 16f) / density).toInt().coerceAtLeast(PANEL_MIN_H.toInt())
                val isBlockEditorPanel = panel.type == PanelType.BlockEditor
                val blockEditorSessionState = remember(panel.id) { mutableStateOf<BlockEditorShellEditorSession?>(null) }
                val blockEditorExpandedCategory = blockEditorSessionState.value?.controller?.expandedCategory
                DarkPanel(
                    panel = panel.toMainPanelState(),
                    snapEnabled = snapEnabled,
                    gridSizeDp = gridSizeDp,
                    isActiveTarget = panel.id == focusedPanelId,
                    maxWidth = maxWidthDp,
                    maxHeight = maxHeightDp,
                    showRail = true,
                    showDefaultRailIcons = !isBlockEditorPanel,
                    showRailColorPicker = !isBlockEditorPanel,
                    railExpandedWidth = when {
                        !isBlockEditorPanel -> 186.dp
                        blockEditorExpandedCategory == null -> 96.dp
                        else -> 352.dp
                    },
                    railExpandedFillHeight = isBlockEditorPanel,
                    railContent = {
                        if (isBlockEditorPanel) {
                            BlockEditorPanelRail(session = blockEditorSessionState.value)
                        }
                    },
                    onPositionChange = { newPos ->
                        updatePanel(panels, panel.id) {
                            val panelWidthPx = it.width * density
                            val panelHeightPx = it.height * density
                            it.copy(
                                x = newPos.x.coerceIn(0f, max(0f, surfaceSize.width - panelWidthPx - 16f)),
                                y = newPos.y.coerceIn(0f, max(0f, surfaceSize.height - panelHeightPx - 16f))
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
                        bridge.onPanelAction(PanelAction.ClosePanel(panel.id))
                    },
                    onColorChange = { color ->
                        updatePanel(panels, panel.id) { it.copy(accentColor = color) }
                    }
                ) {
                    WorkspacePanelContent(
                        panel = panel,
                        steps = projectedSteps,
                        actionSink = bridge,
                        uiPrefs = uiPrefs,
                        workspaceJson = workspaceJson,
                        onBlockEditorSessionReady = { session ->
                            blockEditorSessionState.value = session
                        },
                        onWorkspaceJsonChange = { updated ->
                            if (updated != workspaceJson) {
                                workspaceJson = updated
                                uiPrefs.edit().putString(BLOCKEDITOR_WORKSPACE_PREF_KEY, updated).apply()
                            }
                        }
                    )
                }
            }
        }

        WorkspaceRail(
            snapEnabled = snapEnabled,
            onSnapToggle = { snapEnabled = !snapEnabled },
            onAutoArrange = {
                autoArrangePanels(
                    panels = panels,
                    surfaceSize = surfaceSize,
                    focusedPanelId = focusedPanelId
                )
            },
            onOpenPanel = openPanel,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )

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
                    top = if (dockAtTop) 12.dp else 0.dp,
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
                    focusedPanelId = focusedPanelId
                )
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

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
    workspaceJson: String,
    onBlockEditorSessionReady: (BlockEditorShellEditorSession?) -> Unit = {},
    onWorkspaceJsonChange: (String) -> Unit
) {
    when (panel.type) {
        PanelType.RecorderSteps -> RecorderStepsPanel(steps = steps, actionSink = actionSink)
        PanelType.BlockEditor -> BlockEditorPanel(
            panelId = panel.id,
            uiPrefs = uiPrefs,
            workspaceJson = workspaceJson,
            onSessionReady = onBlockEditorSessionReady,
            onWorkspaceJsonChange = onWorkspaceJsonChange
        )
        PanelType.Flowchart -> FlowchartPanel(
            panelId = panel.id,
            uiPrefs = uiPrefs,
            workspaceJson = workspaceJson
        )
        PanelType.Screenshot,
        PanelType.Marker,
        PanelType.Emscript,
        PanelType.M3Director -> Unit
        PanelType.RuntimeLog -> RuntimeLogPanel()
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
                PanelType.RuntimeLog
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
private fun RuntimeLogPanel() {
    val sample = remember {
        listOf(
            "12:01:14  runtime: waiting",
            "12:01:17  recorder: 4 steps loaded",
            "12:01:21  shell: no runtime binding active"
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("RuntimeLogPanel", style = MaterialTheme.typography.titleSmall)
        sample.forEach {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BlockEditorPanel(
    panelId: String,
    uiPrefs: android.content.SharedPreferences,
    workspaceJson: String,
    onSessionReady: (BlockEditorShellEditorSession?) -> Unit,
    onWorkspaceJsonChange: (String) -> Unit
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
                content = workspaceJson
            )
        )
    }
    val session = boundEditor.session as BlockEditorShellEditorSession
    LaunchedEffect(session) {
        onSessionReady(session)
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
                onWorkspaceJsonChange(WorkspaceSerializer.serialize(document))
            }
    }

    BlockEditorShellPanel(
        session = session,
        onSave = { persistBlockEditorSession(uiPrefs, session) },
        uiConfig = de.visualtasker.blockeditor.compose.host.BlockEditorHostUiConfig(
            showBottomPanel = true,
            showBlockFactory = true,
            showToolbox = false,
            allowClearWorkspace = true
        ),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun BlockEditorPanelRail(
    session: BlockEditorShellEditorSession?
) {
    if (session == null) {
        Text(
            text = "BlockEditor",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(modifier = Modifier.fillMaxHeight()) {
        EditorNavigationRail(
            expandedCategory = session.controller.expandedCategory,
            onCategoryClick = session.controller::onCategoryClick,
            extraCategories = emptyList(),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
        )
        CategoryPalettePanel(
            category = session.controller.expandedCategory,
            definitions = session.controller.definitionsForExpandedCategory(),
            onAddBlock = session.controller::addBlockFromPalette,
            onCreateVariable = session.controller::createVariable,
            onDismiss = session.controller::dismissCategory,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun FlowchartPanel(
    panelId: String,
    uiPrefs: android.content.SharedPreferences,
    workspaceJson: String
) {
    val graphContent = remember(workspaceJson) { loadFlowchartGraphJson(workspaceJson) }
    val hostServices = remember(panelId) { WorkspaceShellUiPluginHostAdapter() }
    val pluginRegistry = remember { defaultWorkspaceShellPluginRegistry() }
    val coordinator = remember(hostServices, pluginRegistry) {
        WorkspaceShellPluginHostCoordinator(
            hostServices = hostServices,
            pluginLookup = pluginRegistry::findEditorPlugin
        )
    }
    val boundEditor = remember(panelId, graphContent) {
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

    DisposableEffect(boundEditor, uiPrefs) {
        onDispose {
            persistFlowchartViewSession(uiPrefs, session)
            boundEditor.close()
        }
    }

    FlowchartShellPanel(session = session)
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
    workspacePanel("panel-1", PanelType.RecorderSteps, "Stepper 1", 84f, 72f, 360f, 360f, 1),
    workspacePanel("panel-2", PanelType.BlockEditor, "BlockEditor 2", 470f, 84f, 360f, 300f, 2),
    workspacePanel("panel-3", PanelType.Flowchart, "Flowchart 3", 470f, 420f, 360f, 300f, 3),
    workspacePanel("panel-4", PanelType.RuntimeLog, "RuntimeLog 4", 860f, 110f, 320f, 240f, 4)
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
    focusedPanelId: String
) {
    val visible = panels.filter { !it.minimized }.sortedBy { it.zIndex }
    if (visible.isEmpty() || surfaceSize.width <= 0 || surfaceSize.height <= 0) return

    val columns = max(1, ceil(sqrt(visible.size.toFloat())).toInt())
    val gap = 14f
    val availableWidth = surfaceSize.width - 64f
    val availableHeight = surfaceSize.height - 48f
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
        val ny = 16f + row * (cellH + gap)
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
        PanelType.RuntimeLog
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
    PanelType.Emscript -> "Debug"
    PanelType.RuntimeLog -> "RuntimeLog"
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
    PanelType.Emscript -> MainPanelType.EMSCRIPT
    PanelType.Screenshot,
    PanelType.Marker,
    PanelType.M3Director -> MainPanelType.LIST_TEST
}

private fun loadBlockEditorWorkspaceJson(
    uiPrefs: android.content.SharedPreferences
): String {
    val persisted = uiPrefs.getString(BLOCKEDITOR_WORKSPACE_PREF_KEY, null)
    return persisted
        ?.let { runCatching { WorkspaceSerializer.serialize(WorkspaceSerializer.deserialize(it)) }.getOrNull() }
        ?: WorkspaceSerializer.serialize(WorkspaceBootstrap.starter())
}

private fun loadFlowchartGraphJson(
    workspaceJson: String
): String {
    val workspace = WorkspaceSerializer.deserialize(workspaceJson)
    val graph = BlockEditorFlowchartProjector.project(workspace).graph
    return FlowGraphJsonCodec().encodeCanonical(graph)
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
    onResetPanels: () -> Unit,
    onAutoArrange: () -> Unit,
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
                Text("Gemeinsame Kategorien (Flowchart + Blockeditor)", style = MaterialTheme.typography.labelLarge)
                WorkspaceColorPreviewRow("Event", M3EColors.Limepop)
                WorkspaceColorPreviewRow("Control", M3EColors.Sunsetcoral)
                WorkspaceColorPreviewRow("Logic", M3EColors.Oceanneon)
                WorkspaceColorPreviewRow("Variable", M3EColors.Mint)
                Spacer(Modifier.height(8.dp))
                Text("Panel-Akzent", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(M3EColors.Limepop, M3EColors.Oceanneon, M3EColors.Ultraviolet, M3EColors.Amber).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            1 -> WorkspaceSettingsInfoTab("Flowchart", listOf("Flowchart-Panels sind direkt im Workspace-FAB und in der linken Rail verfügbar."))
            2 -> WorkspaceSettingsInfoTab("Blockeditor", listOf("BlockEditor-Panels sind direkt im Workspace-FAB und in der linken Rail verfügbar."))
            3 -> WorkspaceSettingsInfoTab("Texteditor", listOf("Die Workspace-Shell hält Texteditor-Funktionalität außerhalb der Shell-Plugin-Panels."))
            4 -> WorkspaceSettingsInfoTab("Browser", listOf("Browser-Panels sind in dieser Shell nicht als Platzhalter angeboten."))
            5 -> WorkspaceSettingsInfoTab("Extras", listOf("Keine Platzhalter-Panels im Workspace-Menü."))
            else -> WorkspaceSettingsInfoTab("Keypad", listOf("Keypad-Mapping bleibt im MainScreen-Studio gebunden."))
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
private fun WorkspaceSettingsInfoTab(title: String, messages: List<String>) {
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
    }
}

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
