package com.visualtasker.wss.workspace.plugin.flowchart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.visualtasker.flowchart.compose.FlowchartColorTokens
import de.visualtasker.flowchart.compose.FlowchartHost
import de.visualtasker.flowchart.compose.FlowchartHostCallbacks
import de.visualtasker.flowchart.compose.FlowchartNodeShapeProvider
import de.visualtasker.flowchart.compose.FlowchartShapeTokens
import de.visualtasker.flowchart.compose.FlowchartUiConfig
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowDiagnosticSeverity
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowGraphEdge
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.interaction.FlowInteractionAction
import de.visualtasker.blockeditor.registry.BlockTypes

@Composable
fun FlowchartShellPanel(
    session: FlowchartShellEditorSession,
    modifier: Modifier = Modifier,
    runtimeSnapshot: FlowRuntimeSnapshot? = null,
    onSave: (() -> Unit)? = null,
    onRunDry: (() -> Unit)? = null,
    onStepBack: (() -> Unit)? = null,
    onStepForward: (() -> Unit)? = null,
    canStepBack: Boolean = false,
    canStepForward: Boolean = false,
    stepLabel: String? = null,
    onNodeSelected: ((FlowNodeId) -> Unit)? = null,
    onDeleteNode: ((FlowNodeId) -> Unit)? = null,
    onViewChanged: ((FlowViewDocument) -> Unit)? = null,
) {
    val controller = session.controller
    var gridVisible by remember(session.sessionId) { mutableStateOf(true) }
    var selectedNodeId by remember(session.sessionId) { mutableStateOf<FlowNodeId?>(null) }
    var selectedEdgeId by remember(session.sessionId) { mutableStateOf<FlowEdgeId?>(null) }
    val handleViewChanged: (FlowViewDocument) -> Unit = remember(session, onViewChanged) {
        { view ->
            session.onViewDocumentChanged(view)
            onViewChanged?.invoke(view)
        }
    }
    val callbacks = remember(session, onNodeSelected, handleViewChanged) {
        FlowchartHostCallbacks(
            onViewDocumentChanged = handleViewChanged,
            onStatusMessage = session::onStatusMessage,
            onNodeSelected = {
                selectedNodeId = it
                if (it != null) selectedEdgeId = null
                if (it != null) onNodeSelected?.invoke(it)
            },
            onEdgeSelected = {
                selectedEdgeId = it
                if (it != null) selectedNodeId = null
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
                edgeStrokeWidthDp = 2f,
                connectorRadiusDp = 3.2f,
            ),
        )
    }

    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
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
        FlowchartShellToolbar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            onUndo = { controller.dispatch(FlowInteractionAction.UndoViewChange) },
            onRedo = { controller.dispatch(FlowInteractionAction.RedoViewChange) },
            onZoomOut = { controller.dispatch(FlowInteractionAction.ZoomViewport(1 / 1.2, FlowPoint(0.0, 0.0))) },
            onZoomIn = { controller.dispatch(FlowInteractionAction.ZoomViewport(1.2, FlowPoint(0.0, 0.0))) },
            onCenter = { controller.attachGraph(controller.snapshot().graph ?: session.graphDocument, null) },
            onArrange = {
                controller.replaceLayout()?.let(handleViewChanged)
            },
            onGridToggle = { gridVisible = !gridVisible },
            onSave = { onSave?.invoke() ?: session.requestSave() },
            onDeleteSelected = selectedNodeId?.let { nodeId ->
                onDeleteNode?.let { deleteNode -> { deleteNode(nodeId) } }
            },
            onRunDry = onRunDry,
            onStepBack = onStepBack,
            onStepForward = onStepForward,
            canStepBack = canStepBack,
            canStepForward = canStepForward,
            stepLabel = stepLabel,
            gridVisible = gridVisible,
        )
        FlowchartProjectionDiagnostics(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            session = session,
            runtimeSnapshot = runtimeSnapshot,
        )
        FlowchartRuntimeInspector(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            session = session,
            selectedNodeId = selectedNodeId,
            selectedEdgeId = selectedEdgeId,
            runtimeSnapshot = runtimeSnapshot,
        )
    }
}

@Composable
private fun FlowchartRuntimeInspector(
    modifier: Modifier,
    session: FlowchartShellEditorSession,
    selectedNodeId: FlowNodeId?,
    selectedEdgeId: FlowEdgeId?,
    runtimeSnapshot: FlowRuntimeSnapshot?,
) {
    val node = selectedNodeId?.let { id -> session.graphDocument.nodes.firstOrNull { it.id == id } }
    val edge = selectedEdgeId?.let { id -> session.graphDocument.edges.firstOrNull { it.id == id } }
    if (node == null && edge == null) return
    Surface(
        modifier = modifier.fillMaxWidth(0.58f),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF171121).copy(alpha = 0.96f),
        contentColor = Color(0xFFE9DFF5),
        tonalElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (node != null) "Runtime Inspector" else "Edge Inspector",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFE9DFF5),
            )
            if (node != null) {
                FlowchartNodeInspectorRows(
                    node = node,
                    edges = session.graphDocument.edges,
                    runtimeSnapshot = runtimeSnapshot,
                )
            } else if (edge != null) {
                FlowchartEdgeInspectorRows(
                    edge = edge,
                    runtimeSnapshot = runtimeSnapshot,
                )
            }
        }
    }
}

@Composable
private fun FlowchartNodeInspectorRows(
    node: FlowGraphNode,
    edges: List<FlowGraphEdge>,
    runtimeSnapshot: FlowRuntimeSnapshot?,
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
    val branchEvent = nodeEvents.lastOrNull { it.kind in setOf("if", "elseif", "else", "while", "loop") }
    val variables = runtimeSnapshot?.runtimeVariables().orEmpty()
    InspectorLine("Label", node.label)
    InspectorLine("Status", status)
    InspectorLine("Block", "$blockType / $blockId")
    InspectorLine("Kanten", "in $traversedIncoming/$incoming, out $traversedOutgoing/$outgoing")
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
        InspectorLine("Diagnose", diagnostics.joinToString { it.message })
    }
}

@Composable
private fun FlowchartEdgeInspectorRows(
    edge: FlowGraphEdge,
    runtimeSnapshot: FlowRuntimeSnapshot?,
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
    onGridToggle: () -> Unit,
    onSave: () -> Unit,
    onDeleteSelected: (() -> Unit)?,
    onRunDry: (() -> Unit)?,
    onStepBack: (() -> Unit)?,
    onStepForward: (() -> Unit)?,
    canStepBack: Boolean,
    canStepForward: Boolean,
    stepLabel: String?,
    gridVisible: Boolean,
) {
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
            FlowchartToolbarButton("Node löschen", onDeleteSelected ?: {}, enabled = onDeleteSelected != null) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
            FlowchartToolbarButton("Run Dry", onRunDry ?: {}, enabled = onRunDry != null) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
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
            FlowchartToolbarButton("Undo", onUndo) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null) }
            FlowchartToolbarButton("Redo", onRedo) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null) }
            FlowchartToolbarButton("Zoom -", onZoomOut) { Icon(Icons.Default.ZoomOut, contentDescription = null) }
            FlowchartToolbarButton("Zoom +", onZoomIn) { Icon(Icons.Default.ZoomIn, contentDescription = null) }
            FlowchartToolbarButton("Zentrieren", onCenter) { Icon(Icons.Default.CenterFocusStrong, contentDescription = null) }
            FlowchartToolbarButton("Auto-Arrange", onArrange) { Icon(Icons.Default.AccountTree, contentDescription = null) }
            FlowchartToolbarButton("Grid", onGridToggle) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = null,
                    tint = if (gridVisible) Color(0xFFA9D7FF) else Color(0xFF8F879B),
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

private data class FlowchartNodePaletteEntry(
    val shapeId: Int,
    val category: String,
    val label: String,
    val definitionId: String,
    val fillColor: Color,
)

private fun flowchartNodePaletteEntries(): List<FlowchartNodePaletteEntry> = listOf(
    FlowchartNodePaletteEntry(1, "Start / Ereignis", "Script Start", BlockTypes.EVENT_START, Color(0xFFFFB300)),
    FlowchartNodePaletteEntry(2, "Aktionen", "Click Text", BlockTypes.ACTION_CLICK_TEXT, Color(0xFF5C638F)),
    FlowchartNodePaletteEntry(2, "Aktionen", "Wait", BlockTypes.ACTION_WAIT, Color(0xFF5C638F)),
    FlowchartNodePaletteEntry(8, "Feedback", "Beep", BlockTypes.FEEDBACK_BEEP, Color(0xFF8A5F76)),
    FlowchartNodePaletteEntry(8, "Feedback", "Vibrate", BlockTypes.FEEDBACK_VIBRATE, Color(0xFF8A5F76)),
    FlowchartNodePaletteEntry(4, "Kontrolle", "If", BlockTypes.CONTROL_IF, Color(0xFF7A5DB8)),
    FlowchartNodePaletteEntry(4, "Kontrolle", "If / Else", BlockTypes.CONTROL_IF_ELSE, Color(0xFF7A5DB8)),
    FlowchartNodePaletteEntry(4, "Kontrolle", "If / Else If / Else", BlockTypes.CONTROL_IF_ELSEIF_ELSE, Color(0xFF7A5DB8)),
    FlowchartNodePaletteEntry(9, "Kontrolle", "Repeat", BlockTypes.CONTROL_REPEAT, Color(0xFF32856C)),
    FlowchartNodePaletteEntry(9, "Kontrolle", "While", BlockTypes.CONTROL_WHILE, Color(0xFF32856C)),
    FlowchartNodePaletteEntry(5, "Logik", "Compare", BlockTypes.LOGIC_COMPARE, Color(0xFF2B8CD6)),
    FlowchartNodePaletteEntry(7, "Logik", "Operate", BlockTypes.LOGIC_OPERATE, Color(0xFF6A4B78)),
    FlowchartNodePaletteEntry(7, "Logik", "Boolean", BlockTypes.LOGIC_BOOLEAN, Color(0xFF6A4B78)),
    FlowchartNodePaletteEntry(6, "Variablen", "Get Variable", BlockTypes.VARIABLE_GET, Color(0xFF2A9D5E)),
    FlowchartNodePaletteEntry(6, "Variablen", "Set Variable", BlockTypes.VARIABLE_SET, Color(0xFF2A9D5E)),
    FlowchartNodePaletteEntry(14, "Diagnose", "Log", BlockTypes.DEBUG_LOG, Color(0xFFE0A43E)),
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
