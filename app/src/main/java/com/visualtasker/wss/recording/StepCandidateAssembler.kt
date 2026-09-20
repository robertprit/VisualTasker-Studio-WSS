package com.visualtasker.wss.recording

object StepCandidateAssembler {
    fun assemble(playback: RecordingPlaybackDocument): StepCandidateDocument {
        val candidates = playback.entries.sortedBy(RecordingPlaybackEntry::sequence).mapNotNull { entry ->
            assembleTap(playback, entry)
        }.mapIndexed { index, candidate -> candidate.copy(ordinal = index + 1) }
        return StepCandidateDocument(
            documentId = "step-candidates:${playback.sessionId}:record-v${playback.schemaVersion}",
            sessionId = playback.sessionId,
            sourceRecordVersion = playback.schemaVersion,
            generatedAtEpochMs = playback.stoppedAtEpochMs ?: playback.startedAtEpochMs,
            candidates = candidates,
            diagnostics = playback.diagnostics.map { diagnostic ->
                StepCandidateDiagnostic(
                    code = "PLAYBACK_${diagnostic.code}",
                    message = diagnostic.message,
                    severity = when (diagnostic.severity) {
                        RecordingPlaybackDiagnosticSeverity.INFO -> StepCandidateDiagnosticSeverity.INFO
                        RecordingPlaybackDiagnosticSeverity.WARNING -> StepCandidateDiagnosticSeverity.WARNING
                        RecordingPlaybackDiagnosticSeverity.ERROR -> StepCandidateDiagnosticSeverity.ERROR
                    },
                    referenceId = diagnostic.referenceId,
                )
            },
        )
    }

    private fun assembleTap(
        playback: RecordingPlaybackDocument,
        entry: RecordingPlaybackEntry,
    ): StepCandidate? {
        val interaction = entry.interaction.interaction
        val tap = interaction.payload as? RecordingInteractionPayload.Tap ?: return null
        val before = entry.beforeScene
        val root = before?.a11ySnapshot?.rootNode
        val explicit = tap.targetReference?.let { root?.findById(it) }
        val ranked = root?.rankedContainingNodes(tap.position.xPx, tap.position.yPx).orEmpty()
        val diagnostics = mutableListOf<StepCandidateDiagnostic>()
        val confidenceReasons = mutableListOf<ConfidenceReason>()
        var status = StepReviewStatus.UNREVIEWED

        val targetNode = when {
            explicit != null -> {
                confidenceReasons += ConfidenceReason.EXPLICIT_RECORDED_TARGET
                explicit
            }
            tap.targetReference != null -> {
                diagnostics += diagnostic("RECORDED_TARGET_MISSING", "Das aufgezeichnete A11y-Ziel fehlt im Vorher-Snapshot.", tap.targetReference)
                confidenceReasons += ConfidenceReason.CORRUPT_SOURCE_REFERENCE
                status = StepReviewStatus.NEEDS_REVIEW
                ranked.firstOrNull()?.node
            }
            ranked.isNotEmpty() -> {
                confidenceReasons += ConfidenceReason.SINGLE_CONTAINING_NODE
                ranked.first().node
            }
            else -> null
        }

        val topTies = ranked.takeWhile { candidate -> ranked.firstOrNull()?.sameRank(candidate) == true }
        if (explicit == null && topTies.size > 1) {
            confidenceReasons -= ConfidenceReason.SINGLE_CONTAINING_NODE
            confidenceReasons += ConfidenceReason.MULTIPLE_TARGETS
            status = StepReviewStatus.NEEDS_REVIEW
            diagnostics += diagnostic("AMBIGUOUS_TARGET", "Mehrere gleich plausible A11y-Ziele enthalten den Tap.", interaction.interactionId)
        }
        if (before == null) {
            confidenceReasons += ConfidenceReason.MISSING_BEFORE_SCENE
            status = StepReviewStatus.UNSUPPORTED
            diagnostics += diagnostic("BEFORE_SCENE_MISSING", "Ohne Vorher-Szene kann kein belastbares Ziel abgeleitet werden.", interaction.beforeSceneId)
        } else if (root == null) {
            confidenceReasons += ConfidenceReason.MISSING_A11Y_SNAPSHOT
            status = StepReviewStatus.NEEDS_REVIEW
            diagnostics += diagnostic("A11Y_SNAPSHOT_MISSING", "Der Vorher-Szene fehlt ein A11y-Snapshot.", interaction.beforeSceneId)
        }
        if (entry.afterScene == null && entry.transitionStatus != RecordingPlaybackTransitionStatus.UNCHANGED) {
            confidenceReasons += ConfidenceReason.MISSING_AFTER_SCENE
            if (status == StepReviewStatus.UNREVIEWED) status = StepReviewStatus.NEEDS_REVIEW
        }

        targetNode?.let { node ->
            if (!node.clickable) confidenceReasons += ConfidenceReason.TARGET_NOT_CLICKABLE
            if (!node.visibleToUser) confidenceReasons += ConfidenceReason.TARGET_NOT_VISIBLE
            if (!node.enabled) confidenceReasons += ConfidenceReason.TARGET_NOT_ENABLED
            if (!node.contains(tap.position.xPx, tap.position.yPx)) confidenceReasons += ConfidenceReason.TAP_OUTSIDE_TARGET
        } ?: run {
            confidenceReasons += ConfidenceReason.COORDINATE_ONLY
            if (status == StepReviewStatus.UNREVIEWED) status = StepReviewStatus.NEEDS_REVIEW
        }

        val target = targetNode?.toCandidateTarget(
            tap = tap,
            scene = before,
            alternatives = topTies.map { it.node.stableSnapshotNodeId }.filterNot { it == targetNode.stableSnapshotNodeId },
        ) ?: CandidateTarget.Coordinate(tap.position.xPx, tap.position.yPx)
        val confidence = CandidateConfidence(
            level = confidenceLevel(explicit != null, targetNode, topTies.size, before != null, root != null),
            reasons = confidenceReasons.distinct(),
        )
        val beforeRef = before?.toSceneReference()
        val outcome = entry.toObservedOutcome()
        val evidence = CandidateEvidence(
            beforeFrameId = before?.primaryFrame?.frame?.frameId,
            beforeAssetReference = before?.primaryFrame?.frame?.assetReference,
            beforeA11ySnapshotId = before?.a11ySnapshot?.snapshotId,
            afterFrameId = entry.afterScene?.primaryFrame?.frame?.frameId,
            afterAssetReference = entry.afterScene?.primaryFrame?.frame?.assetReference,
            afterA11ySnapshotId = entry.afterScene?.a11ySnapshot?.snapshotId,
            tapXpx = tap.position.xPx,
            tapYpx = tap.position.yPx,
            targetBounds = (target as? CandidateTarget.A11y)?.let { listOf(it.left, it.top, it.right, it.bottom) },
            evidenceRefs = entry.evidence.map(RecordingEvidenceRef::evidenceId),
        )
        return StepCandidate(
            candidateId = "step-candidate:${playback.sessionId}:${interaction.interactionId}:v${playback.schemaVersion}",
            sessionId = playback.sessionId,
            ordinal = 0,
            sourceEntryId = entry.entryId,
            sourceInteractionId = interaction.interactionId,
            action = CandidateAction.TAP,
            target = target,
            displayLabel = target.defaultLabel(),
            beforeSceneRef = beforeRef,
            observedOutcome = outcome,
            evidence = evidence,
            confidence = confidence,
            reviewStatus = status,
            diagnostics = diagnostics,
        )
    }

    private fun confidenceLevel(
        explicit: Boolean,
        target: A11yNodeSnapshot?,
        tieCount: Int,
        hasBefore: Boolean,
        hasSnapshot: Boolean,
    ): ConfidenceLevel = when {
        !hasBefore || !hasSnapshot -> ConfidenceLevel.UNKNOWN
        target == null -> ConfidenceLevel.LOW
        tieCount > 1 -> ConfidenceLevel.LOW
        explicit && target.visibleToUser && target.enabled && target.clickable -> ConfidenceLevel.HIGH
        target.visibleToUser && target.enabled && target.clickable -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }

    private fun diagnostic(code: String, message: String, referenceId: String?) =
        StepCandidateDiagnostic(code, message, StepCandidateDiagnosticSeverity.WARNING, referenceId)
}

private data class RankedNode(
    val node: A11yNodeSnapshot,
    val depth: Int,
    val traversalOrder: Int,
) {
    val area: Long = (node.right - node.left).toLong().coerceAtLeast(0L) * (node.bottom - node.top).toLong().coerceAtLeast(0L)

    fun sameRank(other: RankedNode): Boolean =
        node.visibleToUser == other.node.visibleToUser &&
            node.enabled == other.node.enabled &&
            node.clickable == other.node.clickable &&
            area == other.area &&
            depth == other.depth &&
            (node.viewIdResourceName != null) == (other.node.viewIdResourceName != null) &&
            node.hasLabel() == other.node.hasLabel()
}

private fun A11yNodeSnapshot.rankedContainingNodes(x: Int, y: Int): List<RankedNode> {
    val flattened = mutableListOf<RankedNode>()
    var order = 0
    fun visit(node: A11yNodeSnapshot, depth: Int) {
        if (node.contains(x, y) && node.visibleToUser && node.enabled && node.clickable) {
            flattened += RankedNode(node, depth, order)
        }
        order += 1
        node.children.sortedBy(A11yNodeSnapshot::childOrder).forEach { visit(it, depth + 1) }
    }
    visit(this, 0)
    return flattened.sortedWith(
        compareByDescending<RankedNode> { it.node.visibleToUser }
            .thenByDescending { it.node.enabled }
            .thenByDescending { it.node.clickable }
            .thenBy { it.area }
            .thenByDescending { it.depth }
            .thenByDescending { it.node.viewIdResourceName != null }
            .thenByDescending { it.node.hasLabel() }
            .thenBy { it.traversalOrder },
    )
}

private fun A11yNodeSnapshot.findById(id: String): A11yNodeSnapshot? =
    if (stableSnapshotNodeId == id) this else children.firstNotNullOfOrNull { it.findById(id) }

private fun A11yNodeSnapshot.contains(x: Int, y: Int): Boolean = x in left..right && y in top..bottom
private fun A11yNodeSnapshot.hasLabel(): Boolean = !text.isNullOrBlank() || !contentDescription.isNullOrBlank()

private fun A11yNodeSnapshot.toCandidateTarget(
    tap: RecordingInteractionPayload.Tap,
    scene: RecordingPlaybackScene?,
    alternatives: List<String>,
) = CandidateTarget.A11y(
    nodeId = stableSnapshotNodeId,
    resourceId = viewIdResourceName,
    text = text,
    contentDescription = contentDescription,
    className = className,
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    clickable = clickable,
    visible = visibleToUser,
    enabled = enabled,
    packageName = scene?.scene?.windowContext?.packageName,
    windowId = scene?.scene?.windowContext?.windowId,
    xPx = tap.position.xPx,
    yPx = tap.position.yPx,
    alternativeNodeIds = alternatives,
)

private fun RecordingPlaybackScene.toSceneReference() = SceneReference(
    sceneId = scene.sceneId,
    sequence = scene.sequence,
    packageName = scene.windowContext.packageName,
    activityName = scene.windowContext.activityName,
    windowId = scene.windowContext.windowId,
    frameId = primaryFrame?.frame?.frameId,
    a11ySnapshotId = a11ySnapshot?.snapshotId,
)

private fun RecordingPlaybackEntry.toObservedOutcome(): ObservedOutcome {
    val kind = when (transitionStatus) {
        RecordingPlaybackTransitionStatus.UNCHANGED -> ObservedOutcomeKind.UNCHANGED
        RecordingPlaybackTransitionStatus.CHANGED -> ObservedOutcomeKind.SCENE_TRANSITION
        RecordingPlaybackTransitionStatus.MISSING_AFTER -> ObservedOutcomeKind.MISSING_AFTER
        RecordingPlaybackTransitionStatus.FAILED -> ObservedOutcomeKind.FAILED
        else -> ObservedOutcomeKind.PARTIAL
    }
    val beforeWindow = beforeScene?.scene?.windowContext
    val afterWindow = afterScene?.scene?.windowContext
    return ObservedOutcome(
        kind = kind,
        beforeSceneId = beforeScene?.scene?.sceneId,
        afterSceneId = afterScene?.scene?.sceneId,
        windowChanged = afterWindow != null && beforeWindow != afterWindow,
    )
}

private fun CandidateTarget.defaultLabel(): String = when (this) {
    is CandidateTarget.A11y -> text?.takeIf(String::isNotBlank)
        ?: contentDescription?.takeIf(String::isNotBlank)
        ?: resourceId?.substringAfterLast('/')
        ?: className.substringAfterLast('.')
    is CandidateTarget.Coordinate -> "Tap $xPx, $yPx"
}
