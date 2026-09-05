package com.visualtasker.wss.workspace.plugin.flowchart

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import com.visualtasker.wss.visual.descriptor.VisualDescriptor
import com.visualtasker.wss.visual.policy.DefaultVisualPolicyResolver
import com.visualtasker.wss.visual.projections.FlowchartNodeVisualAdapter
import com.visualtasker.wss.visual.projections.FlowchartNodeVisualSubject
import com.visualtasker.wss.visual.semantics.ProjectionKind
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualSemanticState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.visualtasker.wss.workspace.model.FlowchartConnectionOption
import de.visualtasker.flowchart.compose.FlowchartColorTokens
import de.visualtasker.flowchart.compose.FlowchartHost
import de.visualtasker.flowchart.compose.FlowchartHostCallbacks
import de.visualtasker.flowchart.compose.FlowchartNodeShapeProvider
import de.visualtasker.flowchart.compose.FlowchartNodePortRef
import de.visualtasker.flowchart.compose.FlowchartShapeTokens
import de.visualtasker.flowchart.compose.FlowchartUiConfig
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowDiagnosticSeverity
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowEdgeKind
import de.visualtasker.flowchart.domain.FlowGraphEdge
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.interaction.FlowInteractionAction
import de.visualtasker.flowchart.layout.FlowLayoutConfig
import de.visualtasker.flowchart.layout.FlowPinnedNodePolicy
import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog

@Composable
fun FlowchartShellPanel(
    session: FlowchartShellEditorSession,
    modifier: Modifier = Modifier,
    runtimeSnapshot: FlowRuntimeSnapshot? = null,
    onSave: (() -> Unit)? = null,
    onRunDry: (() -> Unit)? = null,
    onRunLive: (() -> Unit)? = null,
    onStepBack: (() -> Unit)? = null,
    onStepForward: (() -> Unit)? = null,
    canStepBack: Boolean = false,
    canStepForward: Boolean = false,
    stepLabel: String? = null,
    onNodeSelected: ((FlowNodeId) -> Unit)? = null,
    onDeleteNode: ((FlowNodeId) -> Unit)? = null,
    onDeleteNodes: ((Set<FlowNodeId>) -> Unit)? = null,
    onConnectNodes: ((FlowNodeId, FlowNodeId, FlowEdgeKind, String?) -> Unit)? = null,
    onConnectPorts: ((FlowNodeId, String, FlowNodeId, String, FlowEdgeKind) -> Unit)? = null,
    connectionOptionsFor: (FlowNodeId, FlowNodeId) -> List<FlowchartConnectionOption> = { _, _ -> emptyList() },
    onDisconnectEdge: ((FlowEdgeId) -> Unit)? = null,
    onSelectionChanged: ((FlowNodeId?, FlowEdgeId?) -> Unit)? = null,
    onUpdateNodeField: ((FlowNodeId, String, String) -> Unit)? = null,
    onReplaceNodeType: ((FlowNodeId, String) -> Unit)? = null,
    onAddIfBranch: ((FlowNodeId) -> Unit)? = null,
    onRemoveIfBranch: ((FlowNodeId) -> Unit)? = null,
    onViewChanged: ((FlowViewDocument) -> Unit)? = null,
    onUndoWorkspace: (() -> Boolean)? = null,
    onRedoWorkspace: (() -> Boolean)? = null,
    showMiniMap: Boolean = true,
    showTopToolbar: Boolean = true,
) {
    val controller = session.controller
    var gridVisible by remember(session.sessionId) { mutableStateOf(true) }
    var selectedNodeId by remember(session.sessionId) { mutableStateOf<FlowNodeId?>(null) }
    var selectedEdgeId by remember(session.sessionId) { mutableStateOf<FlowEdgeId?>(null) }
    var pendingConnectionStart by remember(session.sessionId) { mutableStateOf<FlowNodeId?>(null) }
    var connectionMenu by remember(session.sessionId) { mutableStateOf<PendingConnectionMenu?>(null) }
    var arrangeMode by remember(session.sessionId) { mutableStateOf(FlowchartArrangeMode.CodeFlow) }
    var panelSize by remember(session.sessionId) { mutableStateOf(IntSize.Zero) }
    var draggedNodeId by remember(session.sessionId) { mutableStateOf<FlowNodeId?>(null) }
    var draggedNodePoint by remember(session.sessionId) { mutableStateOf<FlowPoint?>(null) }
    val density = LocalDensity.current
    val platformView = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val trashSizePx = with(density) { 96.dp.toPx() }
    val trashMarginPx = with(density) { 16.dp.toPx() }
    fun isOverTrash(point: FlowPoint): Boolean {
        if (panelSize == IntSize.Zero) return false
        val left = panelSize.width - trashSizePx - trashMarginPx
        val top = panelSize.height - trashSizePx - trashMarginPx
        val right = panelSize.width - trashMarginPx
        val bottom = panelSize.height - trashMarginPx
        return point.x in left..right && point.y in top..bottom
    }
    fun centerViewport() {
        val current = controller.snapshot().view ?: session.viewDocument ?: return
        val viewport = fitFlowchartViewport(current, panelSize)
        controller.replaceViewport(viewport)
    }
    val handleViewChanged: (FlowViewDocument) -> Unit = remember(session, onViewChanged) {
        { view ->
            session.onViewDocumentChanged(view)
            onViewChanged?.invoke(view)
        }
    }
    val callbacks = remember(
        session,
        onNodeSelected,
        onDeleteNode,
        onDeleteNodes,
        onConnectNodes,
        onConnectPorts,
        connectionOptionsFor,
        pendingConnectionStart,
        panelSize,
        handleViewChanged,
        onSelectionChanged,
    ) {
        FlowchartHostCallbacks(
            onViewDocumentChanged = handleViewChanged,
            onStatusMessage = session::onStatusMessage,
            onNodeSelected = {
                val source = pendingConnectionStart
                if (it != null && source != null && source != it) {
                    val options = connectionOptionsFor(source, it)
                    pendingConnectionStart = null
                    selectedNodeId = it
                    selectedEdgeId = null
                    onSelectionChanged?.invoke(it, null)
                    when (options.size) {
                        0 -> Unit
                        1 -> options.single().let { option ->
                            onConnectNodes?.invoke(source, it, option.kind, option.label)
                        }
                        else -> connectionMenu = PendingConnectionMenu(source, it, options)
                    }
                    onNodeSelected?.invoke(it)
                } else {
                    selectedNodeId = it
                    if (it != null) selectedEdgeId = null
                    onSelectionChanged?.invoke(it, null)
                    if (it != null) onNodeSelected?.invoke(it)
                }
            },
            onEdgeSelected = {
                selectedEdgeId = it
                if (it != null) selectedNodeId = null
                if (it != null) pendingConnectionStart = null
                if (it != null) connectionMenu = null
                onSelectionChanged?.invoke(null, it)
            },
            onPortConnectionRequested = { source, target ->
                selectedNodeId = target.nodeId
                selectedEdgeId = null
                pendingConnectionStart = null
                connectionMenu = null
                onSelectionChanged?.invoke(target.nodeId, null)
                onConnectPorts?.invoke(source.nodeId, source.portName, target.nodeId, target.portName, source.kind)
            },
            onNodeDragChanged = { nodeId, point ->
                draggedNodeId = nodeId
                draggedNodePoint = point
            },
            onNodeDragFinished = { nodeId, nodeIds, point ->
                if (isOverTrash(point) && (onDeleteNodes != null || onDeleteNode != null)) {
                    playFlowchartDeleteFeedback(platformView, hapticFeedback)
                    selectedNodeId = null
                    selectedEdgeId = null
                    pendingConnectionStart = null
                    connectionMenu = null
                    onSelectionChanged?.invoke(null, null)
                    if (nodeIds.size > 1) {
                        onDeleteNodes?.invoke(nodeIds) ?: nodeIds.forEach { onDeleteNode?.invoke(it) }
                    } else {
                        onDeleteNode?.invoke(nodeId)
                    }
                }
            },
        )
    }
    val nodeShapeProvider = remember {
        FlowchartNodeShapeProvider { node, width, height ->
            flowchartMaterialNodePath(node = node, width = width, height = height)
        }
    }
    val uiConfig = remember {
        FlowchartUiConfig(
            controlsEnabled = false,
            zoomEnabled = true,
            panEnabled = true,
            nodeDraggingEnabled = true,
            soundEffectsEnabled = true,
            hapticFeedbackEnabled = true,
            colorTokens = FlowchartColorTokens(
                background = Color.Transparent,
                nodeFill = Color(0xFF24212B),
                eventNodeFill = Color(0xFFFFB300),
                controlNodeFill = Color(0xFF8A5067),
                logicNodeFill = Color(0xFF5E4569),
                variableNodeFill = Color(0xFF165349),
                feedbackNodeFill = Color(0xFF8A5F76),
                nodeStroke = Color(0xFFC9C3D8),
                selectedStroke = Color(0xFF31C4FF),
                succeededStroke = Color(0xFF68D391),
                skippedStroke = Color(0xFF5E596A),
                traversedEdge = Color(0xFF68D391),
                edge = Color(0xFF8D8798),
                branchEdge = Color(0xFFBDA7FF),
                dataEdge = Color(0xFF63C7FF),
                loopEdge = Color(0xFF65D69C),
                errorEdge = Color(0xFFFF8A80),
            ),
            shapeTokens = FlowchartShapeTokens(
                nodeCornerRadiusDp = 8f,
                nodeStrokeWidthDp = 1.4f,
                edgeStrokeWidthDp = 2.8f,
                connectorRadiusDp = 5.8f,
            ),
        )
    }

    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { panelSize = it }
            .background(Color(0xFF09070F), RoundedCornerShape(18.dp))
    ) {
        if (gridVisible) {
            FlowchartShellGrid(Modifier.matchParentSize())
        }
        FlowchartHost(
            graphDocument = session.graphDocument,
            viewDocument = session.viewDocument,
            runtimeSnapshot = runtimeSnapshot,
            controller = controller,
            uiConfig = uiConfig,
            callbacks = callbacks,
            nodeShapeProvider = nodeShapeProvider,
        )
        FlowchartTrashDropTarget(
            active = draggedNodeId != null && draggedNodePoint?.let(::isOverTrash) == true,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
        )
        FlowchartFloatingViewportControls(
            onZoomIn = { controller.dispatch(FlowInteractionAction.ZoomViewport(1.2, FlowPoint(0.0, 0.0))) },
            onZoomOut = { controller.dispatch(FlowInteractionAction.ZoomViewport(1 / 1.2, FlowPoint(0.0, 0.0))) },
            onCenter = ::centerViewport,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 124.dp),
        )
        if (showMiniMap) {
            FlowchartMiniMap(
                viewDocument = session.viewDocument ?: controller.snapshot().view,
                panelSize = panelSize,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 62.dp, end = 14.dp),
            )
        }
        if (showTopToolbar) {
        FlowchartShellToolbar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            onUndo = {
                if (onUndoWorkspace?.invoke() != true) {
                    controller.dispatch(FlowInteractionAction.UndoViewChange)
                }
            },
            onRedo = {
                if (onRedoWorkspace?.invoke() != true) {
                    controller.dispatch(FlowInteractionAction.RedoViewChange)
                }
            },
            onZoomOut = { controller.dispatch(FlowInteractionAction.ZoomViewport(1 / 1.2, FlowPoint(0.0, 0.0))) },
            onZoomIn = { controller.dispatch(FlowInteractionAction.ZoomViewport(1.2, FlowPoint(0.0, 0.0))) },
            onCenter = ::centerViewport,
            onArrange = {
                controller.replaceLayout(arrangeMode.layoutConfig())
            },
            arrangeMode = arrangeMode,
            onArrangeModeSelected = { mode ->
                arrangeMode = mode
                controller.replaceLayout(mode.layoutConfig())
            },
            onGridToggle = { gridVisible = !gridVisible },
            onSave = { onSave?.invoke() ?: session.requestSave() },
            onBeginConnect = selectedNodeId?.let { nodeId ->
                if (onConnectNodes == null) {
                    null
                } else {
                    { pendingConnectionStart = if (pendingConnectionStart == nodeId) null else nodeId }
                }
            },
            onDeleteSelected = selectedNodeId?.let { nodeId ->
                onDeleteNode?.let { deleteNode -> { deleteNode(nodeId) } }
            } ?: selectedEdgeId?.let { edgeId ->
                onDisconnectEdge?.let { disconnectEdge -> { disconnectEdge(edgeId) } }
            },
            onRunDry = onRunDry,
            onRunLive = onRunLive,
            onStepBack = onStepBack,
            onStepForward = onStepForward,
            canStepBack = canStepBack,
            canStepForward = canStepForward,
            stepLabel = stepLabel,
            gridVisible = gridVisible,
            connecting = pendingConnectionStart != null,
        )
        }
        FlowchartProjectionDiagnostics(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            session = session,
            runtimeSnapshot = runtimeSnapshot,
        )
        FlowchartRuntimeInspectorBottomSheet(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            session = session,
            selectedNodeId = selectedNodeId,
            selectedEdgeId = selectedEdgeId,
            runtimeSnapshot = runtimeSnapshot,
            onUpdateNodeField = onUpdateNodeField,
            onReplaceNodeType = onReplaceNodeType,
            onAddIfBranch = onAddIfBranch,
            onRemoveIfBranch = onRemoveIfBranch,
            onDisconnectEdge = onDisconnectEdge,
        )
        FlowchartConnectionMenu(
            expanded = connectionMenu != null,
            options = connectionMenu?.options.orEmpty(),
            onDismiss = { connectionMenu = null },
            onSelect = { option ->
                val pending = connectionMenu ?: return@FlowchartConnectionMenu
                connectionMenu = null
                onConnectNodes?.invoke(pending.sourceNodeId, pending.targetNodeId, option.kind, option.label)
            },
        )
    }
}

@Composable
private fun FlowchartFloatingViewportControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF221C2C).copy(alpha = 0.92f),
        contentColor = Color(0xFFECE6F3),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FlowchartToolbarButton("Zoom +", onZoomIn) { Icon(Icons.Default.ZoomIn, contentDescription = null) }
            FlowchartToolbarButton("Zoom -", onZoomOut) { Icon(Icons.Default.ZoomOut, contentDescription = null) }
            FlowchartToolbarButton("Zentrieren", onCenter) { Icon(Icons.Default.CenterFocusStrong, contentDescription = null) }
        }
    }
}

@Composable
private fun FlowchartMiniMap(
    viewDocument: FlowViewDocument?,
    panelSize: IntSize,
    modifier: Modifier = Modifier,
) {
    if (viewDocument == null) return
    val nodes = viewDocument.nodeViews
    if (nodes.size <= 1 || panelSize.width <= 0 || panelSize.height <= 0) return
    val left = nodes.minOf { it.position.x }.toFloat()
    val top = nodes.minOf { it.position.y }.toFloat()
    val right = nodes.maxOf { it.position.x + (it.size?.width ?: 160.0) }.toFloat()
    val bottom = nodes.maxOf { it.position.y + (it.size?.height ?: 72.0) }.toFloat()
    val contentWidth = (right - left).coerceAtLeast(1f)
    val contentHeight = (bottom - top).coerceAtLeast(1f)
    val viewport = viewDocument.viewport
    Surface(
        modifier = modifier.size(width = 118.dp, height = 82.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF221C2C).copy(alpha = 0.68f),
        contentColor = Color(0xFFECE6F3),
        tonalElevation = 2.dp,
    ) {
        Canvas(Modifier.fillMaxSize().padding(7.dp)) {
            val scale = minOf(size.width / contentWidth, size.height / contentHeight)
            val drawWidth = contentWidth * scale
            val drawHeight = contentHeight * scale
            val offsetX = (size.width - drawWidth) / 2f
            val offsetY = (size.height - drawHeight) / 2f
            nodes.forEach { node ->
                val nodeWidth = (node.size?.width ?: 160.0).toFloat()
                val nodeHeight = (node.size?.height ?: 72.0).toFloat()
                drawRoundRect(
                    color = Color(0xFF63C7FF).copy(alpha = 0.64f),
                    topLeft = Offset(
                        offsetX + (node.position.x.toFloat() - left) * scale,
                        offsetY + (node.position.y.toFloat() - top) * scale,
                    ),
                    size = Size(
                        (nodeWidth * scale).coerceAtLeast(2f),
                        (nodeHeight * scale).coerceAtLeast(2f),
                    ),
                    cornerRadius = CornerRadius(2f, 2f),
                )
            }
            val visibleLeft = (-viewport.pan.x / viewport.zoom).toFloat()
            val visibleTop = (-viewport.pan.y / viewport.zoom).toFloat()
            val visibleRight = ((panelSize.width - viewport.pan.x) / viewport.zoom).toFloat()
            val visibleBottom = ((panelSize.height - viewport.pan.y) / viewport.zoom).toFloat()
            drawRect(
                color = Color(0xFFBDA7FF),
                topLeft = Offset(
                    offsetX + (visibleLeft - left) * scale,
                    offsetY + (visibleTop - top) * scale,
                ),
                size = Size(
                    ((visibleRight - visibleLeft) * scale).coerceAtLeast(4f),
                    ((visibleBottom - visibleTop) * scale).coerceAtLeast(4f),
                ),
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }
    }
}

private fun playFlowchartDeleteFeedback(
    platformView: android.view.View,
    haptic: HapticFeedback,
) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    platformView.performHapticFeedback(
        HapticFeedbackConstants.LONG_PRESS,
        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
    )
    platformView.playSoundEffect(SoundEffectConstants.CLICK)
    runCatching {
        val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 32)
        generator.startTone(ToneGenerator.TONE_PROP_NACK, 54)
        platformView.postDelayed({ generator.release() }, 94L)
    }
}

private data class PendingConnectionMenu(
    val sourceNodeId: FlowNodeId,
    val targetNodeId: FlowNodeId,
    val options: List<FlowchartConnectionOption>,
)

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

private enum class FlowchartArrangeMode(
    val displayLabel: String,
    val description: String,
) {
    CodeFlow("Code Flow", "Vertikaler Hauptstamm, Branches treppenfoermig"),
    Compact("Kompakt", "Engere Abstaende fuer kleine Screens"),
    Wide("Weit", "Mehr Abstand fuer Kanten-Lanes"),
    PreserveManual("Manuell", "Vorhandene Node-Positionen respektieren");

    fun layoutConfig(): FlowLayoutConfig =
        when (this) {
            CodeFlow -> FlowLayoutConfig(
                layerSpacing = 156.0,
                nodeSpacing = 112.0,
                componentSpacing = 192.0,
                routingClearance = 40.0,
                pinnedNodePolicy = FlowPinnedNodePolicy.IGNORE,
            )
            Compact -> FlowLayoutConfig(
                layerSpacing = 104.0,
                nodeSpacing = 56.0,
                componentSpacing = 112.0,
                routingClearance = 22.0,
                pinnedNodePolicy = FlowPinnedNodePolicy.IGNORE,
            )
            Wide -> FlowLayoutConfig(
                layerSpacing = 176.0,
                nodeSpacing = 136.0,
                componentSpacing = 220.0,
                routingClearance = 48.0,
                pinnedNodePolicy = FlowPinnedNodePolicy.IGNORE,
            )
            PreserveManual -> FlowLayoutConfig(
                pinnedNodePolicy = FlowPinnedNodePolicy.HONOR_VIEW,
            )
        }
}

@Composable
private fun FlowchartConnectionMenu(
    expanded: Boolean,
    options: List<FlowchartConnectionOption>,
    onDismiss: () -> Unit,
    onSelect: (FlowchartConnectionOption) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.displayLabel) },
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun FlowchartTrashDropTarget(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(96.dp),
        shape = CircleShape,
        color = if (active) Color(0xFFB3261E).copy(alpha = 0.86f) else Color(0xFF3C3746).copy(alpha = 0.82f),
        contentColor = if (active) Color.White else Color(0xFFECE6F3),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun FlowchartRuntimeInspectorBottomSheet(
    modifier: Modifier,
    session: FlowchartShellEditorSession,
    selectedNodeId: FlowNodeId?,
    selectedEdgeId: FlowEdgeId?,
    runtimeSnapshot: FlowRuntimeSnapshot?,
    onUpdateNodeField: ((FlowNodeId, String, String) -> Unit)?,
    onReplaceNodeType: ((FlowNodeId, String) -> Unit)?,
    onAddIfBranch: ((FlowNodeId) -> Unit)?,
    onRemoveIfBranch: ((FlowNodeId) -> Unit)?,
    onDisconnectEdge: ((FlowEdgeId) -> Unit)?,
) {
    val node = selectedNodeId?.let { id -> session.graphDocument.nodes.firstOrNull { it.id == id } }
    val edge = selectedEdgeId?.let { id -> session.graphDocument.edges.firstOrNull { it.id == id } }
    if (node == null && edge == null) return
    val density = LocalDensity.current
    var sheetHeightDp by remember { mutableFloatStateOf(196f) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(sheetHeightDp.dp),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
        color = Color(0xFF171121).copy(alpha = 0.96f),
        contentColor = Color(0xFFE9DFF5),
        tonalElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 42.dp, height = 5.dp)
                    .background(Color(0xFFE9DFF5).copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            sheetHeightDp = (sheetHeightDp - dragAmount.y / density.density).coerceIn(112f, 340f)
                        }
                    }
            )
            Text(
                text = if (node != null) "Runtime Inspector" else "Edge Inspector",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFE9DFF5),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (node != null) {
                    FlowchartNodeInspectorRows(
                        node = node,
                        graphDocument = session.graphDocument,
                        selectedNodeId = selectedNodeId,
                        edges = session.graphDocument.edges,
                        runtimeSnapshot = runtimeSnapshot,
                        onUpdateNodeField = onUpdateNodeField,
                        onReplaceNodeType = onReplaceNodeType,
                        onAddIfBranch = onAddIfBranch,
                        onRemoveIfBranch = onRemoveIfBranch,
                    )
                } else if (edge != null) {
                    FlowchartEdgeInspectorRows(
                        edge = edge,
                        runtimeSnapshot = runtimeSnapshot,
                        onDisconnectEdge = onDisconnectEdge,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowchartNodeInspectorRows(
    node: FlowGraphNode,
    graphDocument: de.visualtasker.flowchart.domain.FlowGraphDocument,
    selectedNodeId: FlowNodeId?,
    edges: List<FlowGraphEdge>,
    runtimeSnapshot: FlowRuntimeSnapshot?,
    onUpdateNodeField: ((FlowNodeId, String, String) -> Unit)?,
    onReplaceNodeType: ((FlowNodeId, String) -> Unit)?,
    onAddIfBranch: ((FlowNodeId) -> Unit)?,
    onRemoveIfBranch: ((FlowNodeId) -> Unit)?,
) {
    val status = runtimeSnapshot?.nodeStates?.get(node.id)?.name ?: "NO TRACE"
    val blockType = node.properties.stringValue("blockType") ?: "?"
    val blockId = node.properties.stringValue("blockId") ?: node.id.value.removePrefix("block:")
    val incoming = edges.count { it.targetNodeId == node.id }
    val outgoing = edges.count { it.sourceNodeId == node.id }
    val traversedIncoming = runtimeSnapshot?.traversedEdgeIds.orEmpty().count { edgeId ->
        edges.any { it.id == edgeId && it.targetNodeId == node.id }
    }
    val traversedOutgoing = runtimeSnapshot?.traversedEdgeIds.orEmpty().count { edgeId ->
        edges.any { it.id == edgeId && it.sourceNodeId == node.id }
    }
    val nodeEvents = runtimeSnapshot?.runtimeEventsFor(node.id).orEmpty()
    val lastEvent = nodeEvents.lastOrNull()
    val lastNotice = nodeEvents.lastOrNull { it.severity != "INFO" }
    val branchEvent = nodeEvents.lastOrNull { it.kind in setOf("if", "elseif", "else", "while", "loop") }
    val variables = runtimeSnapshot?.runtimeVariables().orEmpty()
    val commandId = node.properties.stringValue("commandId")
    val commandName = node.properties.stringValue("commandName") ?: lastEvent?.command
    val commandKind = node.properties.stringValue("commandKind")
    val commandCapabilities = node.properties.stringValue("commandCapabilities") ?: lastEvent?.capability
    val commandPluginOwner = node.properties.stringValue("commandPluginOwner") ?: lastEvent?.pluginOwner
    val sourceLine = node.properties.stringValue("sourceLine")
    val sourceColumn = node.properties.stringValue("sourceColumn")
    val visualState = FlowchartNodeVisualAdapter.map(
        FlowchartNodeVisualSubject(
            node = node,
            graph = graphDocument,
            runtimeSnapshot = runtimeSnapshot,
            selectedNodeId = selectedNodeId,
        ),
        VisualContext(projection = ProjectionKind.Flowchart),
    )
    val visualDescriptor = DefaultVisualPolicyResolver.resolve(
        visualState,
        VisualContext(projection = ProjectionKind.Flowchart),
    )
    InspectorLine("Label", node.label)
    InspectorLine("Status", status)
    InspectorLine("VAL State", visualState.describeForInspector())
    InspectorLine("VAL Descriptor", visualDescriptor.describeForInspector())
    commandId?.let { InspectorLine("Command-ID", it) }
    commandName?.let { InspectorLine("Command", it) }
    commandKind?.let { InspectorLine("Command-Typ", it) }
    commandCapabilities?.let { InspectorLine("Capability", it) }
    commandPluginOwner?.let { InspectorLine("Plugin", it) }
    InspectorLine("Block", "$blockType / $blockId")
    FlowchartNodeTypeMenu(
        node = node,
        currentBlockType = blockType,
        onReplaceNodeType = onReplaceNodeType,
    )
    sourceLine?.let { line ->
        InspectorLine("Quelle", "EMScript Zeile $line${sourceColumn?.let { ", Spalte $it" }.orEmpty()}")
    }
    InspectorLine("Kanten", "in $traversedIncoming/$incoming, out $traversedOutgoing/$outgoing")
    lastNotice?.let { InspectorLine("Runtime", "${it.severity}: ${it.message}") }
    branchEvent?.let { InspectorLine("Entscheidung", "#${it.index} ${it.kind}: ${it.message}") }
    InspectorLine("Event", lastEvent?.let { "#${it.index} ${it.kind}: ${it.message}" } ?: "-")
    if (nodeEvents.size > 1) {
        InspectorLine(
            "Events",
            nodeEvents.takeLast(3).joinToString(separator = " | ") { "#${it.index} ${it.kind}" },
        )
    }
    if (variables.isNotEmpty()) {
        InspectorLine(
            "Variablen",
            variables.entries.take(4).joinToString { "${it.key}=${it.value}" },
        )
    }
    val diagnostics = runtimeSnapshot?.diagnostics.orEmpty().filter { it.nodeId == node.id }
    if (diagnostics.isNotEmpty()) {
        InspectorLine("Diagnose", diagnostics.joinToString { "${it.severity.name} ${it.code}: ${it.message}" })
    }
    editableNodeFields(node).forEach { field ->
        OutlinedTextField(
            value = field.value,
            onValueChange = { value -> onUpdateNodeField?.invoke(node.id, field.fieldKey, value) },
            enabled = onUpdateNodeField != null,
            singleLine = true,
            label = { Text(field.label) },
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (blockType in setOf(BlockTypes.CONTROL_IF, BlockTypes.CONTROL_IF_ELSE, BlockTypes.CONTROL_IF_ELSEIF_ELSE)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(
                onClick = { onAddIfBranch?.invoke(node.id) },
                enabled = onAddIfBranch != null,
            ) {
                Text("Branch +")
            }
            TextButton(
                onClick = { onRemoveIfBranch?.invoke(node.id) },
                enabled = onRemoveIfBranch != null,
            ) {
                Text("Branch -")
            }
        }
    }
}

@Composable
private fun FlowchartNodeTypeMenu(
    node: FlowGraphNode,
    currentBlockType: String,
    onReplaceNodeType: ((FlowNodeId, String) -> Unit)?,
) {
    val currentSurface = currentBlockType.editorSurface()
    val entries = remember(currentBlockType) {
        flowchartNodePaletteEntries()
            .filter { it.definitionId != currentBlockType }
            .filter { it.definitionId.editorSurface() == currentSurface }
    }
    if (entries.isEmpty()) return
    var expanded by remember(node.id) { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            enabled = onReplaceNodeType != null,
        ) {
            Text("Typ ändern")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    leadingIcon = { FlowchartNodeGlyph(entry = entry, modifier = Modifier.size(22.dp)) },
                    onClick = {
                        expanded = false
                        onReplaceNodeType?.invoke(node.id, entry.definitionId)
                    },
                )
            }
        }
    }
}

private enum class FlowchartEditorSurface {
    Statement,
    Reporter,
    Unknown,
}

private fun String.editorSurface(): FlowchartEditorSurface =
    when {
        startsWith("logic.") ||
            startsWith("variable.") ||
            startsWith("variables.") ||
            startsWith("literal.") -> FlowchartEditorSurface.Reporter
        startsWith("event.") ||
            startsWith("action.") ||
            startsWith("control.") ||
            startsWith("debug.") ||
            startsWith("feedback.") ||
            startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX) ||
            startsWith("command.") -> FlowchartEditorSurface.Statement
        else -> FlowchartEditorSurface.Unknown
    }

private data class EditableFlowchartNodeField(
    val label: String,
    val fieldKey: String,
    val value: String,
)

private fun editableNodeFields(node: FlowGraphNode): List<EditableFlowchartNodeField> =
    listOfNotNull(
        node.properties.textFor("waitMs")?.let { EditableFlowchartNodeField("Wartezeit ms", "ms", it) },
        node.properties.textFor("frequency")?.let { EditableFlowchartNodeField("Frequenz", "frequency", it) },
        node.properties.textFor("durationMs")?.let { EditableFlowchartNodeField("Dauer ms", "durationMs", it) },
        node.properties.textFor("volume")?.let { EditableFlowchartNodeField("Lautstärke", "volume", it) },
        node.properties.textFor("pattern")?.let { EditableFlowchartNodeField("Muster", "pattern", it) },
        node.properties.textFor("message")?.let { EditableFlowchartNodeField("Nachricht", "message", it) },
        node.properties.textFor("text")?.let { EditableFlowchartNodeField("Text", "text", it) },
        node.properties.textFor("args")?.let { EditableFlowchartNodeField("Argumente", "args", it) }
            ?.takeIf { node.properties.stringValue("commandId") == null },
        node.properties.textFor("operator")?.let { EditableFlowchartNodeField("Operator", "operator", it) },
        node.properties.textFor("literalNumber")?.let { EditableFlowchartNodeField("Wert", "value", it) },
        node.properties.textFor("literalString")?.let { EditableFlowchartNodeField("Wert", "value", it) },
        node.properties.textFor("literalBoolean")?.let { EditableFlowchartNodeField("Wert", "value", it) },
        node.properties.textFor("variableLabel")?.let { EditableFlowchartNodeField("Variable", "variableLabel", it) },
    ) + editableCommandArgumentFields(node)

private fun editableCommandArgumentFields(node: FlowGraphNode): List<EditableFlowchartNodeField> {
    val commandId = node.properties.stringValue("commandId") ?: return emptyList()
    val command = VisualTaskerCommandCatalog.findById(commandId) ?: return emptyList()
    val rawArgs = splitInspectorArgs(node.properties.textFor("args").orEmpty())
    return command.arguments
        .filter { it.type != de.visualtasker.blockeditor.registry.CommandArgumentType.STATEMENT_BODY }
        .mapIndexed { index, argument ->
            EditableFlowchartNodeField(
                label = argument.name,
                fieldKey = "args:$index",
                value = rawArgs.getOrNull(index) ?: argument.defaultValue.orEmpty(),
            )
        }
}

private fun VisualSemanticState.describeForInspector(): String =
    listOf(
        "role=${role.name}",
        "activity=${activity.name}",
        "validation=${validation.name}",
        "focus=${focus.name}",
        "authority=${authority.name}",
        "certainty=${certainty.name}",
    ).joinToString(" | ")

private fun VisualDescriptor.describeForInspector(): String =
    listOf(
        "shape=${shapeRole.name}",
        "surface=${surfaceRole.name}",
        "outline=${outlineRole.name}",
        "motion=${motionRole.name}",
        "badges=${badges.joinToString { it.role.name }.ifBlank { "-" }}",
    ).joinToString(" | ")

@Composable
private fun FlowchartEdgeInspectorRows(
    edge: FlowGraphEdge,
    runtimeSnapshot: FlowRuntimeSnapshot?,
    onDisconnectEdge: ((FlowEdgeId) -> Unit)?,
) {
    val status = if (edge.id in runtimeSnapshot?.traversedEdgeIds.orEmpty()) "TRAVERSED" else "NOT TRAVERSED"
    InspectorLine("Status", status)
    InspectorLine("Typ", edge.kind.name)
    InspectorLine("Von", edge.sourceNodeId.value)
    InspectorLine("Nach", edge.targetNodeId.value)
    edge.label?.let { InspectorLine("Label", it) }
    val diagnostics = runtimeSnapshot?.diagnostics.orEmpty().filter { it.edgeId == edge.id }
    if (diagnostics.isNotEmpty()) {
        InspectorLine("Diagnose", diagnostics.joinToString { it.message })
    }
    TextButton(
        onClick = { onDisconnectEdge?.invoke(edge.id) },
        enabled = onDisconnectEdge != null,
    ) {
        Text("Verbindung trennen")
    }
}

@Composable
private fun InspectorLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFBDA7FF),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE9DFF5).copy(alpha = 0.9f),
            maxLines = 2,
        )
    }
}

@Composable
private fun FlowchartProjectionDiagnostics(
    modifier: Modifier,
    session: FlowchartShellEditorSession,
    runtimeSnapshot: FlowRuntimeSnapshot?,
) {
    val projectionDiagnostics = session.graphDocument.diagnostics
    val runtimeDiagnostics = runtimeSnapshot?.diagnostics.orEmpty()
    if (projectionDiagnostics.isEmpty() && runtimeDiagnostics.isEmpty()) return
    val hasError = projectionDiagnostics.any { it.severity == FlowDiagnosticSeverity.ERROR } ||
        runtimeDiagnostics.any { it.severity == FlowDiagnosticSeverity.ERROR }
    Surface(
        modifier = modifier.fillMaxWidth(0.82f),
        shape = RoundedCornerShape(12.dp),
        color = if (hasError) Color(0xFF3A1720).copy(alpha = 0.94f) else Color(0xFF2F2615).copy(alpha = 0.94f),
        contentColor = Color(0xFFFFD7DE),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (hasError) Color(0xFFFFB4AB) else Color(0xFFFFD180),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Flowchart-Projektion",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFD7DE),
                )
                projectionDiagnostics.take(2).forEach { diagnostic ->
                    Text(
                        text = diagnostic.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD7DE).copy(alpha = 0.88f),
                        maxLines = 2,
                    )
                }
                runtimeDiagnostics.take(2).forEach { diagnostic ->
                    Text(
                        text = diagnostic.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD7DE).copy(alpha = 0.88f),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.FlowchartCompactNodeRail(
    onExpandRequested: () -> Unit,
) {
    val entries = remember { flowchartNodePaletteEntries() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { entry ->
                FlowchartNodeGlyphButton(
                    entry = entry,
                    compact = true,
                    onClick = onExpandRequested,
                )
        }
    }
}

@Composable
fun ColumnScope.FlowchartNodeToolboxRail(
    onAddNode: (String) -> Unit = {},
) {
    val groups = remember { flowchartNodePaletteEntries().groupBy { it.category } }
    Column(
        modifier = Modifier
            .width(220.dp)
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        groups.forEach { (category, entries) ->
            Text(
                text = category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(entry.fillColor.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                        .clickable { onAddNode(entry.definitionId) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlowchartNodeGlyph(entry = entry, modifier = Modifier.size(24.dp))
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowchartShellToolbar(
    modifier: Modifier,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onCenter: () -> Unit,
    onArrange: () -> Unit,
    arrangeMode: FlowchartArrangeMode,
    onArrangeModeSelected: (FlowchartArrangeMode) -> Unit,
    onGridToggle: () -> Unit,
    onSave: () -> Unit,
    onBeginConnect: (() -> Unit)?,
    onDeleteSelected: (() -> Unit)?,
    onRunDry: (() -> Unit)?,
    onRunLive: (() -> Unit)?,
    onStepBack: (() -> Unit)?,
    onStepForward: (() -> Unit)?,
    canStepBack: Boolean,
    canStepForward: Boolean,
    stepLabel: String?,
    gridVisible: Boolean,
    connecting: Boolean,
) {
    var arrangeMenuExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF221C2C).copy(alpha = 0.94f),
        contentColor = Color(0xFFECE6F3),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowchartToolbarButton("Speichern", onSave) { Icon(Icons.Default.Save, contentDescription = null) }
            FlowchartToolbarButton("Undo", onUndo) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null) }
            FlowchartToolbarButton("Redo", onRedo) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null) }
            FlowchartToolbarButton("Zoom -", onZoomOut) { Icon(Icons.Default.ZoomOut, contentDescription = null) }
            FlowchartToolbarButton("Zoom +", onZoomIn) { Icon(Icons.Default.ZoomIn, contentDescription = null) }
            FlowchartToolbarButton("Zentrieren", onCenter) { Icon(Icons.Default.CenterFocusStrong, contentDescription = null) }
            Box {
                FlowchartToolbarButton(
                    tooltip = "Auto-Arrange: ${arrangeMode.displayLabel}",
                    onClick = { arrangeMenuExpanded = true },
                ) {
                    Icon(Icons.Default.AccountTree, contentDescription = null)
                }
                DropdownMenu(
                    expanded = arrangeMenuExpanded,
                    onDismissRequest = { arrangeMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Jetzt anwenden") },
                        onClick = {
                            arrangeMenuExpanded = false
                            onArrange()
                        },
                    )
                    FlowchartArrangeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(mode.displayLabel)
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                arrangeMenuExpanded = false
                                onArrangeModeSelected(mode)
                            },
                        )
                    }
                }
            }
            FlowchartToolbarButton("Grid", onGridToggle) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = null,
                    tint = if (gridVisible) Color(0xFFA9D7FF) else Color(0xFF8F879B),
                )
            }
            FlowchartToolbarButton(
                tooltip = if (connecting) "Verbindung abbrechen" else "Verbindung starten",
                onClick = onBeginConnect ?: {},
                enabled = onBeginConnect != null,
            ) {
                Icon(
                    Icons.Default.DeviceHub,
                    contentDescription = null,
                    tint = if (connecting) Color(0xFF63C7FF) else Color.Unspecified,
                )
            }
            FlowchartToolbarButton("Node löschen", onDeleteSelected ?: {}, enabled = onDeleteSelected != null) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
            FlowchartToolbarButton("Run Dry", onRunDry ?: {}, enabled = onRunDry != null) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }
            FlowchartToolbarButton("Run Live", onRunLive ?: {}, enabled = onRunLive != null) {
                Icon(Icons.Default.PlayCircle, contentDescription = null)
            }
            FlowchartToolbarButton("Step zurück", onStepBack ?: {}, enabled = onStepBack != null && canStepBack) {
                Icon(Icons.Default.SkipPrevious, contentDescription = null)
            }
            FlowchartToolbarButton("Step vor", onStepForward ?: {}, enabled = onStepForward != null && canStepForward) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
            }
            stepLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFECE6F3).copy(alpha = 0.78f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FlowchartToolbarButton(
    tooltip: String,
    onClick: () -> Unit,
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
            enabled = enabled,
            modifier = Modifier.size(42.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun FlowchartShellGrid(modifier: Modifier = Modifier) {
    val dotColor = Color(0xFF7F6FA3).copy(alpha = 0.22f)
    Canvas(modifier = modifier) {
        val step = 24.dp.toPx()
        var x = step
        while (x < size.width) {
            var y = step
            while (y < size.height) {
                drawCircle(dotColor, radius = 1.1.dp.toPx(), center = Offset(x, y))
                y += step
            }
            x += step
        }
    }
}

@Composable
private fun FlowchartNodeGlyphButton(
    entry: FlowchartNodePaletteEntry,
    compact: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (compact) 34.dp else 42.dp)
            .background(entry.fillColor.copy(alpha = 0.24f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        FlowchartNodeGlyph(entry = entry, modifier = Modifier.size(if (compact) 22.dp else 28.dp))
    }
}

@Composable
private fun FlowchartNodeGlyph(
    entry: FlowchartNodePaletteEntry,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val path = flowchartLegendShapePath(entry.shapeId, size.width, size.height)
        drawPath(path = path, color = entry.fillColor)
        drawPath(path = path, color = Color(0xFFE7ECFF), style = Stroke(width = 1.2.dp.toPx()))
    }
}

internal data class FlowchartNodePaletteEntry(
    val shapeId: Int,
    val category: String,
    val label: String,
    val definitionId: String,
    val fillColor: Color,
)

internal fun flowchartNodePaletteEntries(): List<FlowchartNodePaletteEntry> =
    DefaultBlockRegistry.allDefinitions()
        .filter { it.paletteVisible }
        .filterNot { it.id == BlockTypes.VARIABLE_REPORTER }
        .sortedWith(
            compareBy(
                { categoryOrder(it.category) },
                { BlockCategories.metaFor(it.category).label },
                { it.label },
                { it.id },
            )
        )
        .map { definition ->
            val metadata = VisualTaskerCommandCatalog.metadataForBlockType(definition.id)
            val displayLabel = metadata[VisualTaskerCommandCatalog.METADATA_SHORT_NAME]
                ?: metadata[VisualTaskerCommandCatalog.METADATA_DISPLAY_NAME]
                ?: definition.label
            FlowchartNodePaletteEntry(
                shapeId = flowchartPaletteShapeId(definition.id, definition.category),
                category = BlockCategories.metaFor(definition.category).label,
                label = displayLabel,
                definitionId = definition.id,
                fillColor = Color(BlockCategories.metaFor(definition.category).accentArgb),
            )
        }

private fun categoryOrder(category: String): Int =
    BlockCategories.all.indexOfFirst { it.id == category }.takeIf { it >= 0 } ?: Int.MAX_VALUE

private fun flowchartPaletteShapeId(blockType: String, category: String): Int =
    when {
        blockType.startsWith("event.") -> 1
        blockType.startsWith("control.if") -> 4
        blockType == BlockTypes.CONTROL_REPEAT -> 9
        blockType == BlockTypes.CONTROL_WHILE -> 9
        blockType == BlockTypes.LOGIC_COMPARE -> 5
        blockType == BlockTypes.VARIABLE_GET || blockType == BlockTypes.VARIABLE_SET -> 6
        blockType == BlockTypes.LOGIC_OPERATE || blockType == BlockTypes.LOGIC_AND || blockType == BlockTypes.LOGIC_OR -> 7
        blockType.startsWith("feedback.") -> 8
        category == BlockCategories.DEBUG -> 14
        category == BlockCategories.VISION || category == BlockCategories.PERCEPTION -> 13
        category == BlockCategories.CHROME_TAB -> 12
        category == BlockCategories.TASKER -> 11
        category == BlockCategories.TERMUX || category == BlockCategories.SHIZUKU || category == BlockCategories.SCRCPY -> 10
        else -> 2
    }

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
        blockType.startsWith("feedback.") -> 8
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
        2 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(10f, 10f)))
        3 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(4f, 18f)))
        4 -> {
            path.moveTo(w / 2f, 0f)
            path.lineTo(w, h / 2f)
            path.lineTo(w / 2f, h)
            path.lineTo(0f, h / 2f)
            path.close()
        }
        5 -> {
            path.moveTo(12f, 0f)
            path.lineTo(w - 12f, 0f)
            path.lineTo(w, h / 2f)
            path.lineTo(w - 12f, h)
            path.lineTo(12f, h)
            path.lineTo(0f, h / 2f)
            path.close()
        }
        6 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(18f, 18f)))
        7 -> {
            path.moveTo(6f, 0f)
            path.lineTo(w - 6f, 0f)
            path.lineTo(w, 6f)
            path.lineTo(w, h - 6f)
            path.lineTo(w - 6f, h)
            path.lineTo(6f, h)
            path.lineTo(0f, h - 6f)
            path.lineTo(0f, 6f)
            path.close()
        }
        8 -> {
            path.moveTo(8f, 0f)
            path.lineTo(w, 0f)
            path.lineTo(w - 8f, h)
            path.lineTo(0f, h)
            path.close()
        }
        9 -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(24f, 8f)))
        10 -> {
            path.moveTo(0f, 0f)
            path.lineTo(w - 10f, 0f)
            path.lineTo(w, h / 2f)
            path.lineTo(w - 10f, h)
            path.lineTo(0f, h)
            path.close()
        }
        13 -> {
            path.moveTo(0f, 6f)
            path.lineTo(6f, 0f)
            path.lineTo(w - 6f, 0f)
            path.lineTo(w, 6f)
            path.lineTo(w, h - 6f)
            path.lineTo(w - 6f, h)
            path.lineTo(6f, h)
            path.lineTo(0f, h - 6f)
            path.close()
        }
        14 -> {
            path.moveTo(w / 2f, 0f)
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
        }
        else -> path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(10f, 10f)))
    }
    return path
}

private data class FlowRuntimeEventSummary(
    val index: Int,
    val kind: String,
    val message: String,
    val severity: String,
    val command: String?,
    val capability: String?,
    val pluginOwner: String?,
)

private fun FlowRuntimeSnapshot.runtimeEventsFor(nodeId: FlowNodeId): List<FlowRuntimeEventSummary> =
    extensions
        .firstOrNull { it.key == "visualtasker.runtime-events" }
        ?.value
        ?.let { it as? FlowSemanticValue.ListValue }
        ?.values
        .orEmpty()
        .mapNotNull { it as? FlowSemanticValue.ObjectValue }
        .mapNotNull { event ->
            val values = event.values
            if (values.stringValue("nodeId") != nodeId.value) return@mapNotNull null
            FlowRuntimeEventSummary(
                index = values.numberValue("index")?.toIntOrNull() ?: 0,
                kind = values.stringValue("kind") ?: "?",
                message = values.stringValue("message") ?: "",
                severity = values.stringValue("severity") ?: "INFO",
                command = values.stringValue("command"),
                capability = values.stringValue("capability"),
                pluginOwner = values.stringValue("pluginOwner"),
            )
        }
        .sortedBy { it.index }

private fun FlowRuntimeSnapshot.runtimeVariables(): Map<String, String> =
    extensions
        .firstOrNull { it.key == "visualtasker.runtime-variables" }
        ?.value
        ?.let { it as? FlowSemanticValue.ObjectValue }
        ?.values
        .orEmpty()
        .mapNotNull { (key, value) ->
            val rendered = (value as? FlowSemanticValue.StringValue)?.value ?: return@mapNotNull null
            key to rendered
        }
        .toMap()

private fun Map<String, FlowSemanticValue>.stringValue(key: String): String? =
    (this[key] as? FlowSemanticValue.StringValue)?.value

private fun Map<String, FlowSemanticValue>.numberValue(key: String): String? =
    (this[key] as? FlowSemanticValue.NumberValue)?.canonicalValue

private fun Map<String, FlowSemanticValue>.textFor(key: String): String? =
    when (val value = this[key]) {
        is FlowSemanticValue.StringValue -> value.value
        is FlowSemanticValue.NumberValue -> value.canonicalValue
        is FlowSemanticValue.BooleanValue -> value.value.toString()
        else -> null
    }

private fun splitInspectorArgs(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inString = false
    var escape = false
    var depth = 0
    raw.forEach { char ->
        when {
            escape -> {
                current.append(char)
                escape = false
            }
            char == '\\' && inString -> {
                current.append(char)
                escape = true
            }
            char == '"' -> {
                current.append(char)
                inString = !inString
            }
            !inString && char in "([{<" -> {
                current.append(char)
                depth += 1
            }
            !inString && char in ")]}>" -> {
                current.append(char)
                depth = (depth - 1).coerceAtLeast(0)
            }
            !inString && depth == 0 && char == ',' -> {
                result += current.toString().trim()
                current.clear()
            }
            else -> current.append(char)
        }
    }
    val tail = current.toString().trim()
    if (tail.isNotEmpty() || result.isNotEmpty()) result += tail
    return result
}
