package com.visualtasker.wss.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCandidateAssemblerTest {
    @Test
    fun explicitRecordedTargetProducesHighProposedCandidateAndStableId() {
        val playback = playback(targetNodeId = "button", children = listOf(node("button", 10, 10, 60, 60)))

        val first = StepCandidateAssembler.assemble(playback)
        val second = StepCandidateAssembler.assemble(playback)
        val candidate = first.candidates.single()

        assertEquals(candidate.candidateId, second.candidates.single().candidateId)
        assertEquals("button", (candidate.target as CandidateTarget.A11y).nodeId)
        assertEquals(ConfidenceLevel.HIGH, candidate.confidence.level)
        assertTrue(ConfidenceReason.EXPLICIT_RECORDED_TARGET in candidate.confidence.reasons)
        assertEquals(StepReviewStatus.UNREVIEWED, candidate.reviewStatus)
    }

    @Test
    fun uniqueGeometricTargetIsProposedWithProvenance() {
        val playback = playback(targetNodeId = null, children = listOf(node("button", 10, 10, 60, 60)))

        val candidate = StepCandidateAssembler.assemble(playback).candidates.single()

        assertEquals("button", (candidate.target as CandidateTarget.A11y).nodeId)
        assertEquals(ConfidenceLevel.MEDIUM, candidate.confidence.level)
        assertTrue(ConfidenceReason.SINGLE_CONTAINING_NODE in candidate.confidence.reasons)
        assertEquals("snapshot-before", candidate.beforeSceneRef?.a11ySnapshotId)
        assertEquals("interaction-1", candidate.sourceInteractionId)
    }

    @Test
    fun equallyPlausibleTargetsStayAmbiguousAndOrdered() {
        val playback = playback(
            targetNodeId = null,
            children = listOf(
                node("first", 10, 10, 60, 60, order = 0),
                node("second", 10, 10, 60, 60, order = 1),
            ),
        )

        val candidate = StepCandidateAssembler.assemble(playback).candidates.single()
        val target = candidate.target as CandidateTarget.A11y

        assertEquals("first", target.nodeId)
        assertEquals(listOf("second"), target.alternativeNodeIds)
        assertEquals(StepReviewStatus.NEEDS_REVIEW, candidate.reviewStatus)
        assertTrue(ConfidenceReason.MULTIPLE_TARGETS in candidate.confidence.reasons)
    }

    @Test
    fun missingSuitableNodeUsesCoordinateFallback() {
        val playback = playback(targetNodeId = null, children = emptyList())

        val candidate = StepCandidateAssembler.assemble(playback).candidates.single()

        assertTrue(candidate.target is CandidateTarget.Coordinate)
        assertEquals(ConfidenceLevel.LOW, candidate.confidence.level)
        assertEquals(StepReviewStatus.NEEDS_REVIEW, candidate.reviewStatus)
        assertTrue(ConfidenceReason.COORDINATE_ONLY in candidate.confidence.reasons)
    }

    @Test
    fun unchangedAndSceneTransitionRemainObservedEvidence() {
        val unchanged = StepCandidateAssembler.assemble(
            playback(targetNodeId = "button", children = listOf(node("button", 10, 10, 60, 60)), changed = false),
        ).candidates.single()
        val changed = StepCandidateAssembler.assemble(
            playback(targetNodeId = "button", children = listOf(node("button", 10, 10, 60, 60)), changed = true),
        ).candidates.single()

        assertEquals(ObservedOutcomeKind.UNCHANGED, unchanged.observedOutcome.kind)
        assertEquals("scene-before", unchanged.observedOutcome.afterSceneId)
        assertEquals(ObservedOutcomeKind.SCENE_TRANSITION, changed.observedOutcome.kind)
        assertEquals("scene-after", changed.observedOutcome.afterSceneId)
    }

    @Test
    fun reviewedProjectionAllowsOnlyConfirmedAndCorrectedAndDetectsStale() {
        val candidates = StepCandidateAssembler.assemble(
            playback(targetNodeId = "button", children = listOf(node("button", 10, 10, 60, 60))),
        )
        val candidate = candidates.candidates.single()
        val confirmed = decision(candidate, candidates, StepReviewStatus.CONFIRMED)

        val accepted = ReviewedStepProjector.project(candidates, listOf(confirmed))
        val rejected = ReviewedStepProjector.project(candidates, listOf(confirmed.copy(status = StepReviewStatus.REJECTED)))
        val stale = ReviewedStepProjector.project(candidates, listOf(confirmed.copy(sourceRecordVersion = 99)))

        assertEquals(1, accepted.steps.size)
        assertEquals(0, accepted.unresolvedCount)
        assertEquals(0, rejected.steps.size)
        assertEquals(1, rejected.rejectedCount)
        assertEquals(0, stale.steps.size)
        assertEquals(1, stale.unresolvedCount)
        assertTrue(stale.diagnostics.any { it.code == "STALE_REVIEW" })
    }

    @Test
    fun correctionKeepsOriginalProposalSeparate() {
        val candidates = StepCandidateAssembler.assemble(
            playback(targetNodeId = "button", children = listOf(node("button", 10, 10, 60, 60))),
        )
        val candidate = candidates.candidates.single()
        val corrected = candidate.proposalSnapshot().copy(
            target = CandidateTarget.Coordinate(30, 30),
            displayLabel = "Koordinate",
        )
        val decision = decision(candidate, candidates, StepReviewStatus.CORRECTED).copy(correctedProposal = corrected)
        val reviewed = ReviewedStepProjector.project(candidates, listOf(decision))

        assertNotEquals(decision.originalProposal, decision.correctedProposal)
        assertTrue(decision.originalProposal.target is CandidateTarget.A11y)
        assertTrue(reviewed.steps.single().finalTarget is CandidateTarget.Coordinate)
        assertEquals("Koordinate", reviewed.steps.single().displayLabel)
    }

    private fun decision(candidate: StepCandidate, document: StepCandidateDocument, status: StepReviewStatus) = StepReviewDecision(
        decisionId = "review:${candidate.candidateId}",
        candidateId = candidate.candidateId,
        sourceSessionId = document.sessionId,
        sourceRecordVersion = document.sourceRecordVersion,
        status = status,
        originalProposal = candidate.proposalSnapshot(),
        correctedProposal = null,
        selectedTargetNodeId = (candidate.target as? CandidateTarget.A11y)?.nodeId,
        reasonCode = null,
        note = null,
        decidedAtEpochMs = 2_000L,
    )

    private fun playback(
        targetNodeId: String?,
        children: List<A11yNodeSnapshot>,
        changed: Boolean = true,
    ): RecordingPlaybackDocument {
        val before = scene("scene-before", 1, "snapshot-before", children)
        val after = if (changed) scene("scene-after", 2, "snapshot-after", children) else before
        val tap = recordingTapInteraction(
            interactionId = "interaction-1",
            rawEventId = "raw-1",
            sessionId = "session-1",
            sequence = 1,
            occurredAtEpochMs = 1_100L,
            occurredAtElapsedRealtimeNanos = 1_100_000_000L,
            xPx = 30,
            yPx = 30,
            beforeSceneId = before.scene.sceneId,
            afterSceneId = after.scene.sceneId,
            targetA11yNodeId = targetNodeId,
            status = if (changed) RecordingInteractionStatus.CHANGED else RecordingInteractionStatus.UNCHANGED,
        )
        val entry = RecordingPlaybackEntry(
            entryId = "entry:interaction-1",
            sequence = 1,
            beforeScene = before,
            interaction = RecordingPlaybackInteraction(tap, targetNodeId?.let { id -> children.firstOrNull { it.stableSnapshotNodeId == id } }),
            afterScene = after,
            transitionStatus = if (changed) RecordingPlaybackTransitionStatus.CHANGED else RecordingPlaybackTransitionStatus.UNCHANGED,
            occurredAtEpochMs = tap.occurredAtEpochMs,
            occurredAtElapsedRealtimeNanos = tap.occurredAtElapsedRealtimeNanos,
        )
        return RecordingPlaybackDocument(
            sessionId = "session-1",
            schemaVersion = RECORDING_SCHEMA_VERSION,
            sessionStatus = RecordingSessionStatus.COMPLETED,
            startedAtEpochMs = 1_000L,
            stoppedAtEpochMs = 1_500L,
            durationMs = 500L,
            initialScene = before,
            scenes = listOf(before, after).distinctBy { it.scene.sceneId },
            entries = listOf(entry),
            diagnostics = emptyList(),
        )
    }

    private fun scene(id: String, sequence: Long, snapshotId: String, children: List<A11yNodeSnapshot>): RecordingPlaybackScene {
        val frameId = "frame-$id"
        return RecordingPlaybackScene(
            scene = RecordingScene(
                sceneId = id,
                sessionId = "session-1",
                sequence = sequence,
                openedAtEpochMs = 1_000L + sequence,
                openedAtElapsedRealtimeNanos = 1_000_000_000L + sequence,
                primaryFrameId = frameId,
                windowContext = RecordingWindowContext("com.example", "Activity$sequence", "window-$sequence"),
                status = RecordingSceneStatus.OPEN,
            ),
            primaryFrame = null,
            a11ySnapshot = A11ySnapshot(
                snapshotId = snapshotId,
                sessionId = "session-1",
                sceneId = id,
                frameId = frameId,
                capturedAtEpochMs = 1_000L,
                rootNode = node("root", 0, 0, 100, 100, clickable = false, children = children),
                windowId = "window-$sequence",
                packageName = "com.example",
                status = RecordingCaptureStatus.CAPTURED,
            ),
        )
    }

    private fun node(
        id: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        order: Int = 0,
        clickable: Boolean = true,
        children: List<A11yNodeSnapshot> = emptyList(),
    ) = A11yNodeSnapshot(
        stableSnapshotNodeId = id,
        parentNodeId = null,
        childOrder = order,
        className = if (clickable) "Button" else "FrameLayout",
        viewIdResourceName = if (clickable) "com.example:id/$id" else null,
        text = if (clickable) id else null,
        contentDescription = null,
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        clickable = clickable,
        longClickable = false,
        scrollable = false,
        editable = false,
        enabled = true,
        selected = false,
        checked = false,
        visibleToUser = true,
        actions = if (clickable) listOf("ACTION_CLICK") else emptyList(),
        children = children,
    )
}
