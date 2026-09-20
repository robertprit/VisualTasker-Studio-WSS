package com.visualtasker.wss.workspace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.visualtasker.wss.recording.RecordingPlaybackPhase
import com.visualtasker.wss.recording.RecordingPlaybackSessionSummary
import com.visualtasker.wss.recording.RecordingPlaybackState
import com.visualtasker.wss.recording.RecordingPlaybackStatus
import com.visualtasker.wss.recording.RecordingPlaybackViewportTransform
import com.visualtasker.wss.recording.ReviewedStepDocument
import com.visualtasker.wss.recording.StepCandidate
import com.visualtasker.wss.recording.StepCandidateDocument
import com.visualtasker.wss.recording.StepReviewDecision
import com.visualtasker.wss.recording.StepReviewStatus
import com.visualtasker.wss.recording.CandidateTarget
import com.visualtasker.wss.recording.deepestNodeAt
import com.visualtasker.wss.recording.findPlaybackNode
import com.visualtasker.wss.workspace.model.ObservationProvider
import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.SceneInspectorNode
import com.visualtasker.wss.workspace.model.SceneInspectorNodeKind
import com.visualtasker.wss.workspace.model.SceneInspectorTreeProjector
import com.visualtasker.wss.workspace.model.RecordingPlaybackSceneTreeProjector
import com.visualtasker.wss.workspace.model.WorldObservation
import com.visualtasker.wss.workspace.model.WssDragPayload

@Composable
internal fun SceneInspectorPanel(
    panelId: String,
    step: RecorderStepUi?,
    recorderObservations: List<WorldObservation>,
    playbackState: RecordingPlaybackState? = null,
    playbackSessions: List<RecordingPlaybackSessionSummary> = emptyList(),
    candidateDocument: StepCandidateDocument? = null,
    reviewDecisions: List<StepReviewDecision> = emptyList(),
    reviewedDocument: ReviewedStepDocument? = null,
    reviewError: String? = null,
    onPlaybackSessionSelected: (String) -> Unit = {},
    onPlaybackPlayPause: () -> Unit = {},
    onPlaybackPrevious: () -> Unit = {},
    onPlaybackNext: () -> Unit = {},
    onPlaybackRestart: () -> Unit = {},
    onPlaybackSpeedChange: (Float) -> Unit = {},
    onPlaybackNodeSelected: (String?) -> Unit = {},
    onReviewStatus: (String, StepReviewStatus, String) -> Unit = { _, _, _ -> },
    onCorrectTarget: (String, String, String) -> Unit = { _, _, _ -> },
    onUseCoordinateTarget: (String, String) -> Unit = { _, _ -> },
    onNextUnreviewed: () -> Unit = {},
    onBeginTargetCorrection: () -> Unit = {},
    onPayloadDropped: (WssDragPayload, Offset) -> Unit,
    onPayloadDragPositionChange: (WssDragPayload?, Offset?) -> Unit,
) {
    if (playbackState?.document != null) {
        RecordingPlaybackInspector(
            panelId = panelId,
            state = playbackState,
            sessions = playbackSessions,
            candidateDocument = candidateDocument,
            reviewDecisions = reviewDecisions,
            reviewedDocument = reviewedDocument,
            reviewError = reviewError,
            onSessionSelected = onPlaybackSessionSelected,
            onPlayPause = onPlaybackPlayPause,
            onPrevious = onPlaybackPrevious,
            onNext = onPlaybackNext,
            onRestart = onPlaybackRestart,
            onSpeedChange = onPlaybackSpeedChange,
            onNodeSelected = onPlaybackNodeSelected,
            onReviewStatus = onReviewStatus,
            onCorrectTarget = onCorrectTarget,
            onUseCoordinateTarget = onUseCoordinateTarget,
            onNextUnreviewed = onNextUnreviewed,
            onBeginTargetCorrection = onBeginTargetCorrection,
            onPayloadDropped = onPayloadDropped,
            onPayloadDragPositionChange = onPayloadDragPositionChange,
        )
        return
    }
    if (step == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Keine Scene ausgewaehlt",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    val tree = remember(panelId, step, recorderObservations) {
        SceneInspectorTreeProjector.project(panelId, step, recorderObservations)
    }
    var expandedIds by remember(tree.sceneId) {
        mutableStateOf(setOf(tree.root.id) + tree.root.children.filter { it.children.isNotEmpty() }.map { it.id })
    }
    var selectedNodeId by remember(tree.sceneId) { mutableStateOf(tree.root.id) }
    val visibleNodes = remember(tree, expandedIds) { tree.root.flattenVisible(expandedIds) }
    val selectedNode = tree.find(selectedNodeId) ?: tree.root

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = SceneAccent,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tree.root.label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${visibleNodes.size} sichtbare Eintraege",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = SceneAccent.copy(alpha = 0.24f))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
        ) {
            items(visibleNodes, key = { it.node.id }) { visible ->
                SceneInspectorTreeRow(
                    node = visible.node,
                    depth = visible.depth,
                    expanded = visible.node.id in expandedIds,
                    selected = visible.node.id == selectedNode.id,
                    onClick = {
                        selectedNodeId = visible.node.id
                        if (visible.node.children.isNotEmpty()) {
                            expandedIds = if (visible.node.id in expandedIds) {
                                expandedIds - visible.node.id
                            } else {
                                expandedIds + visible.node.id
                            }
                        }
                    },
                    onPayloadDropped = onPayloadDropped,
                    onPayloadDragPositionChange = onPayloadDragPositionChange,
                )
            }
        }
        SceneInspectorDetails(node = selectedNode)
    }
}

@Composable
private fun RecordingPlaybackInspector(
    panelId: String,
    state: RecordingPlaybackState,
    sessions: List<RecordingPlaybackSessionSummary>,
    candidateDocument: StepCandidateDocument?,
    reviewDecisions: List<StepReviewDecision>,
    reviewedDocument: ReviewedStepDocument?,
    reviewError: String?,
    onSessionSelected: (String) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onNodeSelected: (String?) -> Unit,
    onReviewStatus: (String, StepReviewStatus, String) -> Unit,
    onCorrectTarget: (String, String, String) -> Unit,
    onUseCoordinateTarget: (String, String) -> Unit,
    onNextUnreviewed: () -> Unit,
    onBeginTargetCorrection: () -> Unit,
    onPayloadDropped: (WssDragPayload, Offset) -> Unit,
    onPayloadDragPositionChange: (WssDragPayload?, Offset?) -> Unit,
) {
    val document = requireNotNull(state.document)
    val scene = state.selectedScene
    val entry = state.selectedEntry
    val candidate = candidateDocument?.candidates?.getOrNull(state.selectedEntryIndex)
    val decision = candidate?.let { selected -> reviewDecisions.firstOrNull { it.candidateId == selected.candidateId } }
    val effectiveReviewStatus = when {
        decision != null && decision.sourceRecordVersion != candidateDocument?.sourceRecordVersion -> StepReviewStatus.STALE
        decision != null -> decision.status
        else -> candidate?.reviewStatus
    }
    val tree = remember(panelId, state.selectedEntryIndex, state.phase, scene?.scene?.sceneId) {
        RecordingPlaybackSceneTreeProjector.project(panelId, state)
    }
    var expandedIds by remember(document.sessionId) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(tree?.sceneId) {
        tree?.let { next ->
            expandedIds = expandedIds + next.root.id + next.root.children.filter { it.children.isNotEmpty() }.map { it.id }
        }
    }
    var correctionMode by remember(candidate?.candidateId) { mutableStateOf(false) }
    var pendingTargetNodeId by remember(candidate?.candidateId) { mutableStateOf<String?>(null) }
    var reviewLabel by remember(candidate?.candidateId, decision?.decisionId, decision?.decidedAtEpochMs) {
        mutableStateOf(decision?.correctedProposal?.displayLabel ?: candidate?.displayLabel.orEmpty())
    }
    val highlightedNodeId = when {
        pendingTargetNodeId != null -> pendingTargetNodeId
        decision?.status == StepReviewStatus.CORRECTED -> decision.selectedTargetNodeId
        else -> state.selectedA11yNodeId
    }
    val selectedTreeId = highlightedNodeId?.let { "a11y:$it" } ?: tree?.root?.id
    val selectedNode = selectedTreeId?.let { tree?.find(it) } ?: tree?.root
    val visibleNodes = remember(tree, expandedIds) { tree?.root?.flattenVisible(expandedIds).orEmpty() }
    val listState = rememberLazyListState()
    LaunchedEffect(selectedTreeId, visibleNodes) {
        val index = visibleNodes.indexOfFirst { it.node.id == selectedTreeId }
        if (index >= 0) listState.animateScrollToItem(index)
    }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 76.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    sessions.forEach { session ->
                        AssistChip(
                            onClick = { onSessionSelected(session.sessionId) },
                            label = { Text("${session.sessionId.takeLast(8)} | ${session.status.name}", maxLines = 1) },
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TooltipIconButton("Zurueck", onPrevious, Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Zurueck", modifier = Modifier.size(18.dp))
            }
            TooltipIconButton(if (state.status == RecordingPlaybackStatus.PLAYING) "Pause" else "Play", onPlayPause, Modifier.size(36.dp)) {
                Icon(
                    if (state.status == RecordingPlaybackStatus.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            TooltipIconButton("Weiter", onNext, Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Weiter", modifier = Modifier.size(18.dp))
            }
            TooltipIconButton("Neu starten", onRestart, Modifier.size(34.dp)) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Neu starten", modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            RecordingPlaybackControllerSpeeds.forEach { speed ->
                AssistChip(onClick = { onSpeedChange(speed) }, label = { Text("${speed}x") })
            }
        }
        Text(
            text = "${state.selectedEntryIndex + 1}/${document.entries.size.coerceAtLeast(1)}  ${state.phase.name}  ${entry?.transitionStatus?.name ?: document.sessionStatus.name}",
            modifier = Modifier.padding(horizontal = 10.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = SceneAccent,
        )
        RecordingPlaybackScreenshot(
            state = state,
            highlightedNodeId = highlightedNodeId,
            onNodeSelected = onNodeSelected,
            modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 8.dp),
        )
        HorizontalDivider(color = SceneAccent.copy(alpha = 0.24f))
        candidate?.let { selectedCandidate ->
            StepCandidateReviewCard(
                candidate = selectedCandidate,
                decision = decision,
                status = effectiveReviewStatus ?: selectedCandidate.reviewStatus,
                reviewedDocument = reviewedDocument,
                reviewError = reviewError,
                label = reviewLabel,
                correctionMode = correctionMode,
                pendingTargetNodeId = pendingTargetNodeId,
                onLabelChange = { reviewLabel = it },
                onConfirm = { onReviewStatus(selectedCandidate.candidateId, StepReviewStatus.CONFIRMED, reviewLabel) },
                onCorrectionModeChange = { enabled ->
                    if (enabled) onBeginTargetCorrection()
                    correctionMode = enabled
                    if (!enabled) pendingTargetNodeId = null
                },
                onApplyCorrection = {
                    pendingTargetNodeId?.let { nodeId ->
                        onCorrectTarget(selectedCandidate.candidateId, nodeId, reviewLabel)
                        correctionMode = false
                        pendingTargetNodeId = null
                    }
                },
                onUseCoordinate = { onUseCoordinateTarget(selectedCandidate.candidateId, reviewLabel) },
                onReject = { onReviewStatus(selectedCandidate.candidateId, StepReviewStatus.REJECTED, reviewLabel) },
                onLater = { onReviewStatus(selectedCandidate.candidateId, StepReviewStatus.DEFERRED, reviewLabel) },
                onNextUnreviewed = onNextUnreviewed,
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
        ) {
            items(visibleNodes, key = { it.node.id }) { visible ->
                SceneInspectorTreeRow(
                    node = visible.node,
                    depth = visible.depth,
                    expanded = visible.node.id in expandedIds,
                    selected = visible.node.id == selectedNode?.id,
                    onClick = {
                        visible.node.properties["a11yNodeId"]?.let { nodeId ->
                            if (correctionMode) pendingTargetNodeId = nodeId else onNodeSelected(nodeId)
                        }
                        if (visible.node.children.isNotEmpty()) {
                            expandedIds = if (visible.node.id in expandedIds) expandedIds - visible.node.id else expandedIds + visible.node.id
                        }
                    },
                    onPayloadDropped = onPayloadDropped,
                    onPayloadDragPositionChange = onPayloadDragPositionChange,
                )
            }
        }
        selectedNode?.let { SceneInspectorDetails(it) }
    }
}

@Composable
private fun StepCandidateReviewCard(
    candidate: StepCandidate,
    decision: StepReviewDecision?,
    status: StepReviewStatus,
    reviewedDocument: ReviewedStepDocument?,
    reviewError: String?,
    label: String,
    correctionMode: Boolean,
    pendingTargetNodeId: String?,
    onLabelChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCorrectionModeChange: (Boolean) -> Unit,
    onApplyCorrection: () -> Unit,
    onUseCoordinate: () -> Unit,
    onReject: () -> Unit,
    onLater: () -> Unit,
    onNextUnreviewed: () -> Unit,
) {
    val reviewed = reviewedDocument?.steps?.size ?: 0
    val open = reviewedDocument?.unresolvedCount ?: 0
    val rejected = reviewedDocument?.rejectedCount ?: 0
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        shape = RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, SceneAccent.copy(alpha = 0.46f)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Step Review", style = MaterialTheme.typography.titleSmall, color = SceneAccent)
                Text(status.name, style = MaterialTheme.typography.labelSmall, color = reviewStatusColor(status))
                Spacer(Modifier.weight(1f))
                Text("Geprueft $reviewed | Offen $open | Verworfen $rejected", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Sichtbare Bezeichnung") },
                textStyle = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Vorschlag: Tap -> ${candidate.target.reviewLabel()} | ${candidate.confidence.level.name}: ${candidate.confidence.reasons.joinToString { it.name }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            decision?.correctedProposal?.let { corrected ->
                Text(
                    "Korrektur: Tap -> ${corrected.target.reviewLabel()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = reviewStatusColor(StepReviewStatus.CORRECTED),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "Vorher ${candidate.beforeSceneRef?.sceneId ?: "fehlt"} | Ergebnis ${candidate.observedOutcome.kind.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            candidate.diagnostics.firstOrNull()?.let { diagnostic ->
                Text("${diagnostic.code}: ${diagnostic.message}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            reviewError?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onConfirm) { Text("Bestaetigen") }
                TextButton(onClick = { onCorrectionModeChange(!correctionMode) }) {
                    Text(if (correctionMode) "Abbrechen" else "Ziel korrigieren")
                }
                if (correctionMode) {
                    Button(onClick = onApplyCorrection, enabled = pendingTargetNodeId != null) { Text("Uebernehmen") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onUseCoordinate) { Text("Koordinate") }
                TextButton(onClick = onReject) { Text("Verwerfen") }
                TextButton(onClick = onLater) { Text("Spaeter pruefen") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onNextUnreviewed) { Text("Naechster offener") }
            }
        }
    }
}

private fun CandidateTarget.reviewLabel(): String = when (this) {
    is CandidateTarget.A11y -> text?.takeIf(String::isNotBlank)
        ?: contentDescription?.takeIf(String::isNotBlank)
        ?: resourceId?.substringAfterLast('/')
        ?: className.substringAfterLast('.')
    is CandidateTarget.Coordinate -> "Koordinate $xPx,$yPx"
}

private fun reviewStatusColor(status: StepReviewStatus): Color = when (status) {
    StepReviewStatus.CONFIRMED -> Color(0xFF45C995)
    StepReviewStatus.CORRECTED -> Color(0xFF52B7FF)
    StepReviewStatus.REJECTED -> Color(0xFFFF7A90)
    StepReviewStatus.NEEDS_REVIEW, StepReviewStatus.DEFERRED, StepReviewStatus.STALE -> Color(0xFFFFC857)
    StepReviewStatus.UNSUPPORTED -> Color(0xFFD7A5FF)
    StepReviewStatus.PROPOSED, StepReviewStatus.UNREVIEWED -> Color(0xFF9BA7C6)
}

@Composable
private fun RecordingPlaybackScreenshot(
    state: RecordingPlaybackState,
    highlightedNodeId: String? = null,
    onNodeSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scene = state.selectedScene
    val frame = scene?.primaryFrame?.frame
    val file = scene?.primaryFrame?.file
    val bitmap = remember(file?.absolutePath, file?.lastModified(), frame?.rotation) {
        file?.takeIf { it.isFile }?.let { screenshotFile ->
            BitmapFactory.decodeFile(screenshotFile.absolutePath)?.let { source ->
                val rotation = frame?.rotation?.let { ((it % 360) + 360) % 360 } ?: 0
                if (rotation == 0) {
                    source
                } else {
                    android.graphics.Bitmap.createBitmap(
                            source,
                            0,
                            0,
                            source.width,
                            source.height,
                            Matrix().apply { postRotate(rotation.toFloat()) },
                            true,
                        ).also { if (it !== source) source.recycle() }
                }
            }?.asImageBitmap()
        }
    }
    var viewportSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val root = scene?.a11ySnapshot?.rootNode
    val selectedNode = highlightedNodeId?.let { root?.findPlaybackNode(it) }
        ?: state.selectedA11yNodeId?.let { root?.findPlaybackNode(it) }
        ?: state.selectedEntry?.interaction?.targetNode
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF090810),
        border = BorderStroke(1.dp, SceneAccent.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { viewportSize = it.size }
                .pointerInput(scene?.scene?.sceneId, viewportSize) {
                    detectTapGestures { offset ->
                        val sourceFrame = frame ?: return@detectTapGestures
                        val transform = RecordingPlaybackViewportTransform(
                            sourceFrame.widthPx, sourceFrame.heightPx,
                            viewportSize.width.toFloat(), viewportSize.height.toFloat(), sourceFrame.rotation,
                        )
                        transform.unmapPoint(offset.x, offset.y)?.let { source ->
                            onNodeSelected(root?.deepestNodeAt(source.x, source.y)?.stableSnapshotNodeId)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = "Recording Screenshot", modifier = Modifier.fillMaxSize())
            } else {
                Text("Screenshot nicht verfuegbar", color = MaterialTheme.colorScheme.error)
            }
            Canvas(Modifier.fillMaxSize()) {
                val sourceFrame = frame ?: return@Canvas
                val transform = RecordingPlaybackViewportTransform(
                    sourceFrame.widthPx,
                    sourceFrame.heightPx,
                    size.width,
                    size.height,
                    sourceFrame.rotation,
                )
                selectedNode?.let { node ->
                    val rect = transform.mapRect(node.left.toFloat(), node.top.toFloat(), node.right.toFloat(), node.bottom.toFloat())
                    drawRect(
                        color = Color(0xFF40C4FF),
                        topLeft = Offset(rect.left, rect.top),
                        size = androidx.compose.ui.geometry.Size(rect.right - rect.left, rect.bottom - rect.top),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
                (state.selectedEntry?.interaction?.interaction?.payload as? com.visualtasker.wss.recording.RecordingInteractionPayload.Tap)?.let { tap ->
                    val point = transform.mapPoint(tap.position.xPx.toFloat(), tap.position.yPx.toFloat())
                    drawCircle(Color(0xFFFFC857), radius = 9.dp.toPx(), center = Offset(point.x, point.y), style = Stroke(width = 3.dp.toPx()))
                    drawCircle(Color(0xFFFF5DA2), radius = 2.5.dp.toPx(), center = Offset(point.x, point.y))
                }
            }
        }
    }
}

private val RecordingPlaybackControllerSpeeds = listOf(0.5f, 1f, 2f)

@Composable
private fun SceneInspectorTreeRow(
    node: SceneInspectorNode,
    depth: Int,
    expanded: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onPayloadDropped: (WssDragPayload, Offset) -> Unit,
    onPayloadDragPositionChange: (WssDragPayload?, Offset?) -> Unit,
) {
    val accent = node.accentColor()
    var rowWindowPosition by remember(node.id) { mutableStateOf(Offset.Zero) }
    var dragWindowPosition by remember(node.id) { mutableStateOf<Offset?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
            .background(
                color = if (selected) accent.copy(alpha = 0.19f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .onGloballyPositioned { rowWindowPosition = it.positionInWindow() }
            .pointerInput(node.payload.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { local ->
                        dragWindowPosition = rowWindowPosition + local
                        onPayloadDragPositionChange(node.payload, dragWindowPosition)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragWindowPosition = (dragWindowPosition ?: rowWindowPosition + change.position) + amount
                        onPayloadDragPositionChange(node.payload, dragWindowPosition)
                    },
                    onDragEnd = {
                        dragWindowPosition?.let { onPayloadDropped(node.payload, it) }
                        dragWindowPosition = null
                        onPayloadDragPositionChange(null, null)
                    },
                    onDragCancel = {
                        dragWindowPosition = null
                        onPayloadDragPositionChange(null, null)
                    },
                )
            }
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (node.children.isNotEmpty()) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                tint = accent,
                modifier = Modifier.size(17.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(17.dp))
        }
        Icon(
            imageVector = node.kind.icon(),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            node.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (node.children.isNotEmpty()) {
            Text(
                text = node.children.size.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Item ziehen",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun SceneInspectorDetails(node: SceneInspectorNode) {
    val accent = node.accentColor()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 118.dp, max = 190.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(node.kind.icon(), contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text("Inspector", style = MaterialTheme.typography.titleSmall, color = accent)
                Text(
                    text = node.kind.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SceneInspectorDetailRow("Label", node.label)
            node.subtitle?.let { SceneInspectorDetailRow("Info", it) }
            node.provider?.let { SceneInspectorDetailRow("Provider", it.name) }
            node.properties.entries.sortedBy { it.key }.forEach { (key, value) ->
                SceneInspectorDetailRow(key, value)
            }
        }
    }
}

@Composable
private fun SceneInspectorDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private data class VisibleSceneInspectorNode(val node: SceneInspectorNode, val depth: Int)

private fun SceneInspectorNode.flattenVisible(expandedIds: Set<String>, depth: Int = 0): List<VisibleSceneInspectorNode> =
    buildList {
        add(VisibleSceneInspectorNode(this@flattenVisible, depth))
        if (id in expandedIds) {
            children.forEach { child -> addAll(child.flattenVisible(expandedIds, depth + 1)) }
        }
    }

private fun SceneInspectorNode.accentColor(): Color = provider?.accentColor() ?: when (kind) {
    SceneInspectorNodeKind.Scene -> SceneAccent
    SceneInspectorNodeKind.Window -> Color(0xFF42A5F5)
    SceneInspectorNodeKind.Interaction -> Color(0xFFFF5DA2)
    SceneInspectorNodeKind.Frame -> Color(0xFF7C4DFF)
    SceneInspectorNodeKind.Elements -> Color(0xFF26C6DA)
    SceneInspectorNodeKind.Element -> Color(0xFF80CBC4)
    SceneInspectorNodeKind.Evidence -> Color(0xFFFFC857)
    SceneInspectorNodeKind.Property -> Color(0xFFB0BEC5)
}

private fun ObservationProvider.accentColor(): Color = when (this) {
    ObservationProvider.Accessibility -> Color(0xFF40C4FF)
    ObservationProvider.Ocr -> Color(0xFFFFD740)
    ObservationProvider.OpenCv -> Color(0xFF69F0AE)
    ObservationProvider.Yolo -> Color(0xFFFF7043)
    ObservationProvider.Dom -> Color(0xFFCE93D8)
    ObservationProvider.User -> Color(0xFFFF5DA2)
    ObservationProvider.Runtime -> Color(0xFF82B1FF)
    ObservationProvider.Import -> Color(0xFFAED581)
    ObservationProvider.Unknown -> Color(0xFF90A4AE)
}

private fun SceneInspectorNodeKind.icon() = when (this) {
    SceneInspectorNodeKind.Scene -> Icons.Default.Layers
    SceneInspectorNodeKind.Window -> Icons.Default.WebAsset
    SceneInspectorNodeKind.Interaction -> Icons.Default.TouchApp
    SceneInspectorNodeKind.Frame -> Icons.Default.Photo
    SceneInspectorNodeKind.Elements -> Icons.Default.AccountTree
    SceneInspectorNodeKind.Element -> Icons.Default.Adjust
    SceneInspectorNodeKind.Evidence -> Icons.Default.Description
    SceneInspectorNodeKind.Property -> Icons.AutoMirrored.Filled.Label
}

private val SceneAccent = Color(0xFF00D4AA)
