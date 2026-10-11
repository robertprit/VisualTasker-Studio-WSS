package com.visualtasker.wss.recording

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.runtime.ExecutionPlanResult
import com.visualtasker.wss.emscript.runtime.ExecutionRequest
import com.visualtasker.wss.emscript.runtime.WorkflowExecutionPlanner
import com.visualtasker.wss.workspace.model.WorkspaceSelectionState
import com.visualtasker.wss.workspace.model.WorkspaceShellHistoryController
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.serialization.WorkflowSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecorderWorkflowPublisherTest {
    private val publisher = RecorderWorkflowPublisher()

    @Test
    fun confirmedSemanticTapPublishesCanonicalClick() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val result = fixture.publish()

        assertTrue(result.toString(), result is RecorderPublishResult.Success)
        assertTrue(source(result.document()).contains("click(\"Login\")"))
    }

    @Test
    fun correctedTapUsesCorrectedSemanticTarget() {
        val original = step(CandidateAction.TAP, coordinate(10, 20), status = StepReviewStatus.CORRECTED)
        val corrected = original.copy(finalTarget = a11y("Continue"))
        val fixture = fixture(corrected)

        assertTrue(source(fixture.publish().document()).contains("click(\"Continue\")"))
    }

    @Test
    fun explicitCoordinateFallbackPublishesClickPointAndWarning() {
        val fixture = fixture(step(CandidateAction.TAP, coordinate(120, 240)))
        val preview = fixture.preview()

        assertEquals(1, preview.coordinateFallbackCount)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_COORDINATE_FALLBACK" })
        val generated = source(fixture.publish(preview).document())
        assertTrue(generated.contains("clickPoint"))
        assertTrue(generated.contains("120") && generated.contains("240"))
    }

    @Test
    fun linearSwipePublishesExistingPointerPathContract() {
        val target = swipe(listOf(point(10, 20), point(80, 90)), 350)
        val preview = fixture(step(CandidateAction.SWIPE, target)).preview()

        assertEquals(RecorderStepMappingKind.SWIPE, preview.plan!!.mappings.single().kind)
        assertTrue(preview.plan!!.mappings.single().commandSource.contains("duration=350"))
    }

    @Test
    fun multiPointSwipeIsClassifiedAsPointerPath() {
        val target = swipe(listOf(point(10, 20), point(40, 45), point(80, 90)), 500)
        val preview = fixture(step(CandidateAction.SWIPE, target)).preview()

        assertEquals(RecorderStepMappingKind.POINTER_PATH, preview.plan!!.mappings.single().kind)
        assertTrue(preview.plan!!.mappings.single().commandSource.contains("point(40,45)"))
    }

    @Test
    fun waitPublishesExistingTimingCommand() {
        val fixture = fixture(step(CandidateAction.WAIT, CandidateTarget.Delay(750)))

        assertTrue(source(fixture.publish().document()).contains("wait(750)"))
    }

    @Test
    fun screenshotPublishesExistingCaptureCommand() {
        val fixture = fixture(step(CandidateAction.SCREENSHOT, CandidateTarget.Capture("captures/a.png")))

        assertTrue(source(fixture.publish().document()).contains("screenshot(\"captures/a.png\")"))
    }

    @Test
    fun multipleStepsKeepRecorderOrdinalOrder() {
        val fixture = fixture(
            step(CandidateAction.WAIT, CandidateTarget.Delay(10), ordinal = 2, id = "wait"),
            step(CandidateAction.TAP, a11y("First"), ordinal = 1, id = "tap"),
        )
        val generated = source(fixture.publish().document())

        assertTrue(generated.indexOf("click(\"First\")") < generated.indexOf("wait(10)"))
    }

    @Test
    fun rejectedStepsAreOmittedAndReported() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")), rejectedCount = 1)
        val preview = fixture.preview()

        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_REJECTED_SKIPPED" })
        assertEquals(1, preview.plan!!.mappings.size)
    }

    @Test
    fun deferredOrUnresolvedStepBlocksDefaultPublish() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")), unresolvedCount = 1)
        val preview = fixture.preview()

        assertFalse(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_UNRESOLVED_STEPS" })
    }

    @Test
    fun explicitPartialDecisionOmitsUnresolvedStep() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")), unresolvedCount = 1, allowPartial = true)
        val preview = fixture.preview()

        assertTrue(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_PARTIAL_CONFIRMED" })
    }

    @Test
    fun ambiguousSemanticTargetBlocksPublish() {
        val ambiguous = a11y("Login").copy(alternativeNodeIds = listOf("other"))
        val preview = fixture(step(CandidateAction.TAP, ambiguous)).preview()

        assertFalse(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_TARGET_AMBIGUOUS" })
    }

    @Test
    fun correctedAmbiguousTargetMayPublish() {
        val ambiguous = a11y("Login").copy(alternativeNodeIds = listOf("other"))
        val preview = fixture(step(CandidateAction.TAP, ambiguous, status = StepReviewStatus.CORRECTED)).preview()

        assertTrue(preview.canPublish)
    }

    @Test
    fun unsupportedStepBlocksPublishWithDiagnostic() {
        val preview = fixture(step(CandidateAction.UNSUPPORTED, CandidateTarget.Unsupported("TEXT"))).preview()

        assertFalse(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_STEP_UNSUPPORTED" })
    }

    @Test
    fun missingScreenshotReferenceBlocksPublish() {
        val preview = fixture(step(CandidateAction.SCREENSHOT, CandidateTarget.Capture(""))).preview()

        assertFalse(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_SCREENSHOT_PARAMETER_MISSING" })
    }

    @Test
    fun missingCaptureAssetIsWarningNotStructuralFailure() {
        val localPublisher = RecorderWorkflowPublisher(assetExists = { false })
        val fixture = fixture(step(CandidateAction.SCREENSHOT, CandidateTarget.Capture("missing.png")))
        val preview = localPublisher.preview(fixture.request, fixture.candidates, fixture.reviewed, fixture.current)

        assertTrue(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_CAPTURE_ASSET_MISSING" })
    }

    @Test
    fun previewDoesNotMutateWorkspaceOrRawReviewDocuments() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val before = WorkflowSerializer.serialize(fixture.current)
        val candidates = fixture.candidates.copy()
        val reviewed = fixture.reviewed.copy()

        fixture.preview()

        assertEquals(before, WorkflowSerializer.serialize(fixture.current))
        assertEquals(candidates, fixture.candidates)
        assertEquals(reviewed, fixture.reviewed)
    }

    @Test
    fun applyRequiresExplicitMatchingConfirmation() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val preview = fixture.preview()
        val result = publisher.apply(fixture.request, preview, fixture.current)

        assertTrue(result is RecorderPublishResult.Failure)
        assertTrue((result as RecorderPublishResult.Failure).diagnostics.any { it.code == "PUBLISH_CONFIRMATION_REQUIRED" })
    }

    @Test
    fun changedPreviewTokenIsRejected() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val preview = fixture.preview()
        val request = fixture.request.copy(confirmation = RecorderPublishConfirmation("wrong", 123))

        assertTrue(publisher.apply(request, preview, fixture.current) is RecorderPublishResult.Failure)
    }

    @Test
    fun staleWorkspaceRevisionBlocksPreview() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val preview = publisher.preview(
            fixture.request.copy(expectedWorkspaceRevision = fixture.current.version + 1),
            fixture.candidates,
            fixture.reviewed,
            fixture.current,
        )

        assertFalse(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_WORKSPACE_REVISION_CONFLICT" })
    }

    @Test
    fun staleRecorderRevisionBlocksPreview() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val preview = publisher.preview(
            fixture.request.copy(sourceRevision = fixture.request.sourceRevision + 1),
            fixture.candidates,
            fixture.reviewed,
            fixture.current,
        )

        assertFalse(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_RECORDER_REVISION_CONFLICT" })
    }

    @Test
    fun workspaceChangeAfterPreviewRejectsApplyAtomically() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val preview = fixture.preview()
        val changed = import("wait(2)", fixture.current.id)
        val confirmed = fixture.request.confirm(preview)

        val result = publisher.apply(confirmed, preview, changed)

        assertTrue(result is RecorderPublishResult.Failure)
        assertFalse(source(changed).contains("Login"))
    }

    @Test
    fun secondIdenticalPublishIsIdempotent() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val first = fixture.publish().document()
        val secondRequest = fixture.request.copy(expectedWorkspaceRevision = first.version)
        val secondPreview = publisher.preview(secondRequest, fixture.candidates, fixture.reviewed, first)

        assertTrue(secondPreview.alreadyPublished)
        assertFalse(secondPreview.canPublish)
    }

    @Test
    fun stableEntityIdsAreDeterministicAcrossEquivalentPreviews() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val first = fixture.preview().plan!!.mappings.single().targetEntityId
        val second = fixture.preview().plan!!.mappings.single().targetEntityId

        assertEquals(first, second)
    }

    @Test
    fun differentCandidatesReceiveDifferentStableEntityIds() {
        val fixture = fixture(
            step(CandidateAction.TAP, a11y("One"), id = "one"),
            step(CandidateAction.TAP, a11y("Two"), id = "two", ordinal = 2),
        )
        val ids = fixture.preview().plan!!.mappings.map { it.targetEntityId }

        assertEquals(2, ids.distinct().size)
    }

    @Test
    fun provenanceIsAttachedWithoutEmbeddingImageData() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val result = fixture.publish().document()
        val published = result.blocks.values.single { it.metadata["recorder.publish.documentId"] == "candidate-doc" }

        assertEquals("candidate-1", published.metadata["recorder.publish.candidateId"])
        assertEquals("frame-before", published.metadata["recorder.publish.captureFrameId"])
        assertFalse(published.metadata.values.any { it.startsWith("data:image") })
    }

    @Test
    fun saveReloadPreservesPublishedIdentityAndProvenance() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val published = fixture.publish().document()
        val reloaded = WorkflowSerializer.deserialize(WorkflowSerializer.serialize(published))

        assertEquals(published.canonical, reloaded.canonical)
        assertTrue(reloaded.blocks.values.any { it.metadata["recorder.publish.candidateId"] == "candidate-1" })
    }

    @Test
    fun textBlockAndFlowProjectionsSeePublishedEntity() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val published = fixture.publish().document()
        val state = WorkspaceWorkflowState.fromDocument(published, "m7-1:test")
        val blockId = published.blocks.values.single { it.metadata["recorder.publish.candidateId"] == "candidate-1" }.id

        assertTrue(state.emscriptProjection.getOrThrow().contains("Login"))
        assertTrue(state.document.blocks.containsKey(blockId))
        assertTrue(state.flowchartProjection.graph.nodes.any { it.id.value == "block:${blockId.value}" })
    }

    @Test
    fun publishedSourceAnchorSurvivesReapply() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val published = fixture.publish().document()
        val generated = source(published)
        val reapplied = (EmscriptApplyGuard().preview(
            generated,
            workspaceId = published.id,
            previousDocument = published,
        ) as EmscriptApplyGuardResult.Success).importedDocument

        val beforeIds = published.canonical!!.entities.map { it.ref.id }.toSet()
        val afterIds = reapplied.canonical!!.entities.map { it.ref.id }.toSet()
        assertEquals(beforeIds, afterIds)
    }

    @Test
    fun runtimeCapabilityReportDistinguishesStructureFromProviderReadiness() {
        val preview = fixture(step(CandidateAction.TAP, a11y("Login"))).preview()

        assertTrue(preview.canPublish)
        assertFalse(preview.plan!!.runtimeCapabilities.realRunAllowed)
        assertTrue(preview.plan!!.runtimeCapabilities.capabilities.isNotEmpty())
    }

    @Test
    fun publishedWorkflowProducesExecutionPlanThroughProductivePlanner() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val published = fixture.publish().document()
        val result = WorkflowExecutionPlanner().plan(
            ExecutionRequest("request:m7-1", fixture.request.targetWorkflowEntityId, published),
        )

        assertTrue(result.toString(), result is ExecutionPlanResult.Ready)
        val plan = (result as ExecutionPlanResult.Ready).plan
        assertTrue(plan.steps.isNotEmpty())
        assertTrue(plan.steps.any { it.entityId in published.blocks.keys.map { blockId -> blockId.value } })
    }

    @Test
    fun publishedWorkflowUsesExistingShellUndoRedoHistory() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val published = fixture.publish().document()
        val before = WorkspaceWorkflowState.fromDocument(fixture.current, "m7-1:before")
        val after = WorkspaceWorkflowState.fromDocument(published, "m7-1:publish")
        val history = WorkspaceShellHistoryController()
        val selection = WorkspaceSelectionState()

        assertTrue(history.recordAcceptedTransition(before, after, selection, "recorder:publish:candidate-doc"))
        val undo = history.undo(after, selection)!!
        val restoredBefore = WorkspaceWorkflowState.fromSerialized(undo.serializedJson, "workspace:undo")
        assertFalse(source(restoredBefore.document).contains("Login"))

        val redo = history.redo(restoredBefore, selection)!!
        val restoredAfter = WorkspaceWorkflowState.fromSerialized(redo.serializedJson, "workspace:redo")
        assertTrue(source(restoredAfter.document).contains("click(\"Login\")"))
    }

    @Test
    fun wrongWorkspaceIdBlocksWithoutMutation() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val request = fixture.request.copy(targetWorkspaceId = "other")
        val preview = publisher.preview(request, fixture.candidates, fixture.reviewed, fixture.current)

        assertFalse(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_WORKSPACE_MISMATCH" })
    }

    @Test
    fun missingWorkflowBlocksWithoutMutation() {
        val fixture = fixture(step(CandidateAction.TAP, a11y("Login")))
        val request = fixture.request.copy(targetWorkflowEntityId = "missing")
        val preview = publisher.preview(request, fixture.candidates, fixture.reviewed, fixture.current)

        assertFalse(preview.canPublish)
        assertTrue(preview.diagnostics.any { it.code == "PUBLISH_WORKFLOW_NOT_FOUND" })
    }

    private fun fixture(
        vararg steps: ReviewedStep,
        unresolvedCount: Int = 0,
        rejectedCount: Int = 0,
        allowPartial: Boolean = false,
    ): Fixture {
        val current = import("wait(1)", "workspace-m7-1")
        val workflow = current.canonical!!.entities.single { it.ref.kind == SemanticEntityKind.Workflow }.ref.id.value
        val candidates = StepCandidateDocument(
            documentId = "candidate-doc",
            sessionId = "session-1",
            sourceRecordVersion = 7,
            generatedAtEpochMs = 100,
            candidates = steps.map { it.toCandidate() },
            diagnostics = emptyList(),
        )
        val reviewed = ReviewedStepDocument(
            sessionId = candidates.sessionId,
            sourceCandidateDocumentId = candidates.documentId,
            sourceRecordVersion = candidates.sourceRecordVersion,
            steps = steps.toList(),
            unresolvedCount = unresolvedCount,
            rejectedCount = rejectedCount,
            diagnostics = emptyList(),
        )
        return Fixture(
            current,
            candidates,
            reviewed,
            RecorderPublishRequest(
                recorderDocumentId = candidates.documentId,
                sourceRevision = candidates.sourceRecordVersion,
                targetWorkspaceId = current.id,
                targetWorkflowEntityId = workflow,
                expectedWorkspaceRevision = current.version,
                allowPartial = allowPartial,
            ),
        )
    }

    private inner class Fixture(
        val current: WorkspaceDocument,
        val candidates: StepCandidateDocument,
        val reviewed: ReviewedStepDocument,
        val request: RecorderPublishRequest,
    ) {
        fun preview() = publisher.preview(request, candidates, reviewed, current)
        fun publish(preview: RecorderPublishPreview = preview()): RecorderPublishResult =
            publisher.apply(request.confirm(preview), preview, current)
    }

    private fun RecorderPublishRequest.confirm(preview: RecorderPublishPreview) = copy(
        confirmation = RecorderPublishConfirmation(preview.confirmationToken, 123),
    )

    private fun RecorderPublishResult.document(): WorkspaceDocument = when (this) {
        is RecorderPublishResult.Success -> document
        else -> error("Expected publish success, got $this")
    }

    private fun import(source: String, id: String): WorkspaceDocument =
        (EmscriptApplyGuard().preview(source, workspaceId = id) as EmscriptApplyGuardResult.Success).importedDocument

    private fun source(document: WorkspaceDocument): String = EmscriptGenerator().generate(document)

    private fun step(
        action: CandidateAction,
        target: CandidateTarget,
        ordinal: Int = 1,
        id: String = "candidate-1",
        status: StepReviewStatus = StepReviewStatus.CONFIRMED,
    ) = ReviewedStep(
        stepId = "reviewed:$id",
        ordinal = ordinal,
        sourceCandidateId = id,
        reviewStatus = status,
        action = action,
        finalTarget = target,
        displayLabel = id,
        beforeSceneRef = null,
        observedOutcome = ObservedOutcome(ObservedOutcomeKind.UNCHANGED, "before", "after", false),
        evidence = CandidateEvidence(
            beforeFrameId = "frame-before",
            beforeAssetReference = "capture.png",
            beforeA11ySnapshotId = "a11y-before",
            afterFrameId = "frame-after",
            afterAssetReference = null,
            afterA11ySnapshotId = null,
            tapXpx = target.xPx,
            tapYpx = target.yPx,
            targetBounds = null,
            evidenceRefs = listOf("evidence-1"),
        ),
        reviewDecisionRef = "decision:$id",
    )

    private fun ReviewedStep.toCandidate() = StepCandidate(
        candidateId = sourceCandidateId,
        sessionId = "session-1",
        ordinal = ordinal,
        sourceEntryId = "entry:$sourceCandidateId",
        sourceInteractionId = "interaction:$sourceCandidateId",
        action = action,
        target = finalTarget,
        displayLabel = displayLabel,
        beforeSceneRef = beforeSceneRef,
        observedOutcome = observedOutcome,
        evidence = evidence,
        confidence = CandidateConfidence(ConfidenceLevel.HIGH, listOf(ConfidenceReason.EXPLICIT_RECORDED_TARGET)),
        reviewStatus = reviewStatus,
        diagnostics = emptyList(),
    )

    private fun a11y(text: String) = CandidateTarget.A11y(
        nodeId = "node-$text",
        resourceId = "id/$text",
        text = text,
        contentDescription = null,
        className = "android.widget.Button",
        left = 0,
        top = 0,
        right = 100,
        bottom = 100,
        clickable = true,
        visible = true,
        enabled = true,
        packageName = "example",
        windowId = "window",
        xPx = 50,
        yPx = 50,
    )

    private fun coordinate(x: Int, y: Int) = CandidateTarget.Coordinate(x, y)
    private fun point(x: Int, y: Int) = RecordingPoint(x, y)
    private fun swipe(points: List<RecordingPoint>, duration: Long) = CandidateTarget.SwipePath(
        start = points.first(),
        end = points.last(),
        points = points,
        durationMs = duration,
        pointerId = 0,
    )
}
