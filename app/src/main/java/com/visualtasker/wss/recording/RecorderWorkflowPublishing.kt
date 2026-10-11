package com.visualtasker.wss.recording

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.apply.EmscriptSemanticSourceApply
import com.visualtasker.wss.emscript.apply.SemanticSourceApplyPlan
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityGate
import com.visualtasker.wss.emscript.runtime.RuntimeCapabilityReport
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.SemanticEntityId
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.SemanticRelationKind
import de.visualtasker.workflow.core.SemanticRelationRoleKind
import de.visualtasker.workflow.core.SemanticRelationId
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.core.WorkspaceOperation
import de.visualtasker.workflow.core.WorkspaceOperationResult
import de.visualtasker.workflow.core.WorkspaceTransaction
import java.security.MessageDigest

const val RECORDER_PUBLISH_MAPPING_VERSION = 1

data class RecorderPublishConfirmation(
    val previewToken: String,
    val confirmedAtEpochMs: Long,
)

data class RecorderPublishRequest(
    val recorderDocumentId: String,
    val sourceRevision: Int,
    val targetWorkspaceId: String,
    val targetWorkflowEntityId: String,
    val targetRootEntityId: String? = null,
    val expectedWorkspaceRevision: Long,
    val allowPartial: Boolean = false,
    val confirmation: RecorderPublishConfirmation? = null,
)

enum class RecorderPublishDiagnosticSeverity { INFO, WARNING, ERROR }

data class RecorderPublishDiagnostic(
    val code: String,
    val message: String,
    val severity: RecorderPublishDiagnosticSeverity,
    val candidateId: String? = null,
)

enum class RecorderStepMappingKind {
    SEMANTIC_CLICK,
    COORDINATE_CLICK,
    SWIPE,
    POINTER_PATH,
    WAIT,
    SCREENSHOT,
}

data class RecorderStepMapping(
    val sourceStepId: String,
    val sourceCandidateId: String,
    val targetEntityId: SemanticEntityId,
    val kind: RecorderStepMappingKind,
    val commandSource: String,
    val capability: String,
    val coordinateFallback: Boolean,
    val provenance: Map<String, String>,
)

data class RecorderPublishPlan(
    val request: RecorderPublishRequest,
    val mappings: List<RecorderStepMapping>,
    val transaction: WorkspaceTransaction,
    val sourceApplyPlan: SemanticSourceApplyPlan,
    val targetDocument: WorkspaceDocument,
    val runtimeCapabilities: RuntimeCapabilityReport,
    val addedEntityCount: Int,
    val addedRelationCount: Int,
    val omittedStepCount: Int,
)

data class RecorderPublishPreview(
    val request: RecorderPublishRequest,
    val confirmationToken: String,
    val plan: RecorderPublishPlan?,
    val diagnostics: List<RecorderPublishDiagnostic>,
    val alreadyPublished: Boolean = false,
) {
    val canPublish: Boolean
        get() = plan != null && diagnostics.none { it.severity == RecorderPublishDiagnosticSeverity.ERROR }

    val newStepCount: Int get() = plan?.mappings?.size ?: 0
    val usedCapabilities: Set<String> get() = plan?.mappings?.mapTo(linkedSetOf(), RecorderStepMapping::capability).orEmpty()
    val coordinateFallbackCount: Int get() = plan?.mappings?.count(RecorderStepMapping::coordinateFallback) ?: 0
}

sealed interface RecorderPublishResult {
    data class Success(
        val document: WorkspaceDocument,
        val transactionId: String,
        val publishedEntityIds: List<SemanticEntityId>,
    ) : RecorderPublishResult

    data class AlreadyPublished(val entityIds: List<SemanticEntityId>) : RecorderPublishResult
    data class Failure(val diagnostics: List<RecorderPublishDiagnostic>) : RecorderPublishResult
}

class RecorderWorkflowPublisher(
    private val applyGuard: EmscriptApplyGuard = EmscriptApplyGuard(),
    private val generator: EmscriptGenerator = EmscriptGenerator(),
    private val runtimeGate: RuntimeCapabilityGate = RuntimeCapabilityGate(),
    private val assetExists: (String) -> Boolean = { true },
) {
    fun preview(
        request: RecorderPublishRequest,
        candidates: StepCandidateDocument,
        reviewed: ReviewedStepDocument,
        current: WorkspaceDocument,
    ): RecorderPublishPreview {
        val diagnostics = validateRequest(request, candidates, reviewed, current).toMutableList()
        val provisionalMappings = reviewed.steps.sortedWith(compareBy(ReviewedStep::ordinal, ReviewedStep::stepId)).mapNotNull { step ->
            mapStep(request, reviewed, step, diagnostics)
        }
        val publishedCandidateIds = current.blocks.values.mapNotNullTo(hashSetOf()) {
            it.metadata["recorder.publish.documentId"]
                ?.takeIf { documentId -> documentId == request.recorderDocumentId }
                ?.let { _ -> it.metadata["recorder.publish.candidateId"] }
        }
        val requestedCandidateIds = provisionalMappings.mapTo(linkedSetOf(), RecorderStepMapping::sourceCandidateId)
        val existingPublished = requestedCandidateIds.intersect(publishedCandidateIds)
        val tokenSeed = buildString {
            append(request.recorderDocumentId).append('|')
            append(request.sourceRevision).append('|')
            append(request.targetWorkspaceId).append('|')
            append(request.targetWorkflowEntityId).append('|')
            append(request.targetRootEntityId).append('|')
            append(request.expectedWorkspaceRevision).append('|')
            provisionalMappings.forEach { append(it.sourceCandidateId).append(':').append(it.commandSource).append('|') }
        }
        val token = "recorder-publish:${tokenSeed.sha256(32)}"

        if (requestedCandidateIds.isNotEmpty() && existingPublished.size == requestedCandidateIds.size) {
            diagnostics += diagnostic(
                "PUBLISH_ALREADY_APPLIED",
                "Alle Recorder-Schritte dieses Plans sind im Ziel-Workspace bereits vorhanden.",
                RecorderPublishDiagnosticSeverity.INFO,
            )
            return RecorderPublishPreview(request, token, null, diagnostics, alreadyPublished = true)
        }
        if (existingPublished.isNotEmpty()) {
            diagnostics += diagnostic(
                "PUBLISH_PARTIAL_ID_COLLISION",
                "Ein Teil der stabilen Recorder-Entitaeten ist bereits vorhanden; Publish wird nicht geraten.",
                RecorderPublishDiagnosticSeverity.ERROR,
            )
        }
        if (provisionalMappings.isEmpty()) {
            diagnostics += diagnostic(
                "PUBLISH_NO_STEPS",
                "Es sind keine veroeffentlichbaren Recorder-Schritte vorhanden.",
                RecorderPublishDiagnosticSeverity.ERROR,
            )
        }
        if (diagnostics.any { it.severity == RecorderPublishDiagnosticSeverity.ERROR }) {
            return RecorderPublishPreview(request, token, null, diagnostics)
        }

        val currentSource = runCatching { generator.generate(current) }.getOrElse { error ->
            diagnostics += diagnostic(
                "PUBLISH_SOURCE_PROJECTION_FAILED",
                error.message ?: "Der aktuelle Workflow kann nicht als EMScript projiziert werden.",
                RecorderPublishDiagnosticSeverity.ERROR,
            )
            return RecorderPublishPreview(request, token, null, diagnostics)
        }
        // New entities are intentionally unanchored here. M3 source anchors may only address
        // identities already present in a populated baseline; the validated plan assigns the new
        // canonical IDs, which are then retained from preview through the atomic apply.
        val additions = provisionalMappings.joinToString(separator = "\n", transform = RecorderStepMapping::commandSource)
        val combinedSource = listOf(currentSource.trimEnd(), additions)
            .filter(String::isNotBlank)
            .joinToString("\n")
        val guard = applyGuard.preview(
            draft = combinedSource,
            workspaceId = current.id,
            previousDocument = current,
        )
        val guarded = guard as? EmscriptApplyGuardResult.Success ?: run {
            val failure = guard as EmscriptApplyGuardResult.Failure
            diagnostics += diagnostic(
                "PUBLISH_${failure.stage.name}",
                failure.message,
                RecorderPublishDiagnosticSeverity.ERROR,
            )
            return RecorderPublishPreview(request, token, null, diagnostics)
        }
        val destructive = guarded.sourceApplyPlan.transaction.operations.filter {
            it is WorkspaceOperation.DeleteEntity || it is WorkspaceOperation.RemoveRelation
        }
        if (destructive.isNotEmpty()) {
            diagnostics += diagnostic(
                "PUBLISH_DESTRUCTIVE_PLAN",
                "Der Recorder-Publish wuerde bestehende Workflow-Struktur entfernen.",
                RecorderPublishDiagnosticSeverity.ERROR,
            )
            return RecorderPublishPreview(request, token, null, diagnostics)
        }
        val stablePlan = guarded.sourceApplyPlan.withStableRecorderIdentities(provisionalMappings, current, diagnostics)
        val mapped = stablePlan.bindNewStatementIds(provisionalMappings, current, diagnostics)
        if (diagnostics.any { it.severity == RecorderPublishDiagnosticSeverity.ERROR }) {
            return RecorderPublishPreview(request, token, null, diagnostics)
        }
        val annotatedPlan = stablePlan.withRecorderProvenance(mapped)
        val validated = EmscriptSemanticSourceApply.execute(annotatedPlan)
        val targetDocument = when (validated) {
            is WorkspaceOperationResult.Success -> validated.document
            is WorkspaceOperationResult.Failure -> {
                diagnostics += diagnostic(
                    "PUBLISH_TRANSACTION_REJECTED",
                    validated.diagnostic.message,
                    RecorderPublishDiagnosticSeverity.ERROR,
                )
                return RecorderPublishPreview(request, token, null, diagnostics)
            }
        }
        val transaction = annotatedPlan.transaction
        val plan = RecorderPublishPlan(
            request = request,
            mappings = mapped,
            transaction = transaction,
            sourceApplyPlan = annotatedPlan,
            targetDocument = targetDocument,
            runtimeCapabilities = runtimeGate.inspect(targetDocument),
            addedEntityCount = transaction.operations.count { it is WorkspaceOperation.CreateEntity },
            addedRelationCount = transaction.operations.count { it is WorkspaceOperation.CreateRelation },
            omittedStepCount = reviewed.unresolvedCount + reviewed.rejectedCount,
        )
        return RecorderPublishPreview(request, token, plan, diagnostics)
    }

    fun apply(
        request: RecorderPublishRequest,
        preview: RecorderPublishPreview,
        current: WorkspaceDocument,
    ): RecorderPublishResult {
        if (preview.alreadyPublished) {
            return RecorderPublishResult.AlreadyPublished(
                current.blocks.values.mapNotNull { block ->
                    block.metadata["recorder.publish.documentId"]
                        ?.takeIf { it == preview.request.recorderDocumentId }
                        ?.let { current.canonical?.entities.orEmpty().singleOrNull { entity ->
                            entity.legacySourceId == block.id.value && entity.ref.kind == SemanticEntityKind.Statement
                        }?.ref?.id }
                }
            )
        }
        val confirmation = request.confirmation
        if (confirmation == null || confirmation.previewToken != preview.confirmationToken) {
            return RecorderPublishResult.Failure(
                listOf(diagnostic(
                    "PUBLISH_CONFIRMATION_REQUIRED",
                    "Der unveraenderte Publish-Preview muss ausdruecklich bestaetigt werden.",
                    RecorderPublishDiagnosticSeverity.ERROR,
                ))
            )
        }
        val plan = preview.plan ?: return RecorderPublishResult.Failure(preview.diagnostics)
        if (request.copy(confirmation = null) != preview.request.copy(confirmation = null)) {
            return RecorderPublishResult.Failure(
                listOf(diagnostic(
                    "PUBLISH_REQUEST_CHANGED",
                    "Der bestaetigte Request weicht vom Preview ab.",
                    RecorderPublishDiagnosticSeverity.ERROR,
                ))
            )
        }
        if (current.id != request.targetWorkspaceId || current.version != request.expectedWorkspaceRevision ||
            current.canonical != plan.sourceApplyPlan.baselineDocument.canonical
        ) {
            return RecorderPublishResult.Failure(
                listOf(diagnostic(
                    "PUBLISH_WORKSPACE_REVISION_CONFLICT",
                    "Der Workspace wurde seit dem Preview veraendert.",
                    RecorderPublishDiagnosticSeverity.ERROR,
                ))
            )
        }
        return when (val result = EmscriptSemanticSourceApply.execute(plan.sourceApplyPlan)) {
            is WorkspaceOperationResult.Success -> RecorderPublishResult.Success(
                document = result.document,
                transactionId = plan.transaction.transactionId.value,
                publishedEntityIds = plan.mappings.map(RecorderStepMapping::targetEntityId),
            )
            is WorkspaceOperationResult.Failure -> RecorderPublishResult.Failure(
                listOf(diagnostic(
                    "PUBLISH_ATOMIC_APPLY_FAILED",
                    result.diagnostic.message,
                    RecorderPublishDiagnosticSeverity.ERROR,
                ))
            )
        }
    }

    private fun validateRequest(
        request: RecorderPublishRequest,
        candidates: StepCandidateDocument,
        reviewed: ReviewedStepDocument,
        current: WorkspaceDocument,
    ): List<RecorderPublishDiagnostic> = buildList {
        fun error(code: String, message: String) = add(diagnostic(code, message, RecorderPublishDiagnosticSeverity.ERROR))
        if (request.recorderDocumentId != candidates.documentId || request.recorderDocumentId != reviewed.sourceCandidateDocumentId) {
            error("PUBLISH_RECORDER_DOCUMENT_MISMATCH", "Recorder-Dokument und Review-Dokument stimmen nicht ueberein.")
        }
        if (request.sourceRevision != candidates.sourceRecordVersion || request.sourceRevision != reviewed.sourceRecordVersion) {
            error("PUBLISH_RECORDER_REVISION_CONFLICT", "Die Recorder-Revision ist veraltet.")
        }
        if (request.targetWorkspaceId != current.id) {
            error("PUBLISH_WORKSPACE_MISMATCH", "Der Ziel-Workspace stimmt nicht mit dem aktiven Workspace ueberein.")
        }
        if (request.expectedWorkspaceRevision != current.version) {
            error("PUBLISH_WORKSPACE_REVISION_CONFLICT", "Die erwartete Workspace-Revision ist veraltet.")
        }
        val canonical = current.canonical
        val workflow = canonical?.entities?.singleOrNull {
            it.ref.kind == SemanticEntityKind.Workflow && it.ref.id.value == request.targetWorkflowEntityId
        }
        if (workflow == null) error("PUBLISH_WORKFLOW_NOT_FOUND", "Der Ziel-Workflow ist nicht eindeutig vorhanden.")
        val roots = canonical?.relations.orEmpty().filter {
            it.kind == SemanticRelationKind.Containment &&
                it.role.kind == SemanticRelationRoleKind.Root &&
                it.source.id.value == request.targetWorkflowEntityId
        }
        if (roots.size > 1) error("PUBLISH_MULTI_ROOT_UNSUPPORTED", "M7-1 unterstuetzt keinen Workflow mit mehreren Roots.")
        request.targetRootEntityId?.let { targetRoot ->
            if (roots.singleOrNull()?.target?.id?.value != targetRoot) {
                error("PUBLISH_ROOT_NOT_FOUND", "Der angeforderte Ziel-Root ist nicht der aktive Workflow-Root.")
            }
        }
        if (reviewed.unresolvedCount > 0 && !request.allowPartial) {
            error("PUBLISH_UNRESOLVED_STEPS", "${reviewed.unresolvedCount} Schritte sind noch nicht eindeutig geprueft.")
        }
        if (reviewed.unresolvedCount > 0 && request.allowPartial) {
            add(diagnostic(
                "PUBLISH_PARTIAL_CONFIRMED",
                "${reviewed.unresolvedCount} offene Schritte werden aufgrund der Teilveroeffentlichung ausgelassen.",
                RecorderPublishDiagnosticSeverity.WARNING,
            ))
        }
        if (reviewed.rejectedCount > 0) {
            add(diagnostic(
                "PUBLISH_REJECTED_SKIPPED",
                "${reviewed.rejectedCount} verworfene Schritte werden ausgelassen.",
                RecorderPublishDiagnosticSeverity.INFO,
            ))
        }
    }

    private fun mapStep(
        request: RecorderPublishRequest,
        reviewed: ReviewedStepDocument,
        step: ReviewedStep,
        diagnostics: MutableList<RecorderPublishDiagnostic>,
    ): RecorderStepMapping? {
        if (step.reviewStatus !in setOf(StepReviewStatus.CONFIRMED, StepReviewStatus.CORRECTED)) {
            diagnostics += diagnostic(
                "PUBLISH_STEP_NOT_CONFIRMED",
                "Nur bestaetigte oder korrigierte Schritte duerfen veroeffentlicht werden.",
                RecorderPublishDiagnosticSeverity.ERROR,
                step.sourceCandidateId,
            )
            return null
        }
        val mapped = when (step.action) {
            CandidateAction.TAP -> mapTap(step, diagnostics)
            CandidateAction.SWIPE -> mapSwipe(step, diagnostics)
            CandidateAction.WAIT -> mapWait(step, diagnostics)
            CandidateAction.SCREENSHOT -> mapScreenshot(step, diagnostics)
            CandidateAction.UNSUPPORTED -> null.also {
                diagnostics += diagnostic(
                    "PUBLISH_STEP_UNSUPPORTED",
                    "Der Recording-Schritt ${step.sourceCandidateId} besitzt kein EMScript-Mapping.",
                    RecorderPublishDiagnosticSeverity.ERROR,
                    step.sourceCandidateId,
                )
            }
        } ?: return null
        val entityId = SemanticEntityId(
            "recorder:${request.recorderDocumentId.sha256(12)}:${step.sourceCandidateId.sha256(16)}"
        )
        val provenance = linkedMapOf(
            "recorder.publish.documentId" to reviewed.sourceCandidateDocumentId,
            "recorder.publish.sourceRevision" to reviewed.sourceRecordVersion.toString(),
            "recorder.publish.candidateId" to step.sourceCandidateId,
            "recorder.publish.stepId" to step.stepId,
            "recorder.publish.reviewStatus" to step.reviewStatus.name,
            "recorder.publish.reviewDecision" to step.reviewDecisionRef,
            "recorder.publish.mappingVersion" to RECORDER_PUBLISH_MAPPING_VERSION.toString(),
        ).apply {
            step.evidence.beforeFrameId?.let { put("recorder.publish.captureFrameId", it) }
            if (step.evidence.evidenceRefs.isNotEmpty()) {
                put("recorder.publish.evidenceRefs", step.evidence.evidenceRefs.joinToString(","))
            }
        }
        return RecorderStepMapping(
            sourceStepId = step.stepId,
            sourceCandidateId = step.sourceCandidateId,
            targetEntityId = entityId,
            kind = mapped.kind,
            commandSource = mapped.command,
            capability = mapped.capability,
            coordinateFallback = mapped.coordinateFallback,
            provenance = provenance,
        )
    }

    private fun mapTap(
        step: ReviewedStep,
        diagnostics: MutableList<RecorderPublishDiagnostic>,
    ): MappedCommand? = when (val target = step.finalTarget) {
        is CandidateTarget.A11y -> {
            if (target.alternativeNodeIds.isNotEmpty() && step.reviewStatus != StepReviewStatus.CORRECTED) {
                diagnostics += diagnostic(
                    "PUBLISH_TARGET_AMBIGUOUS",
                    "Das semantische Tap-Ziel ist weiterhin mehrdeutig.",
                    RecorderPublishDiagnosticSeverity.ERROR,
                    step.sourceCandidateId,
                )
                null
            } else {
                val label = target.text?.takeIf(String::isNotBlank)
                    ?: target.contentDescription?.takeIf(String::isNotBlank)
                if (label == null) {
                    diagnostics += diagnostic(
                        "PUBLISH_SEMANTIC_TARGET_MISSING_LABEL",
                        "Das A11y-Ziel kann nicht verlustfrei als click(text) dargestellt werden.",
                        RecorderPublishDiagnosticSeverity.ERROR,
                        step.sourceCandidateId,
                    )
                    null
                } else {
                    MappedCommand(RecorderStepMappingKind.SEMANTIC_CLICK, "click(\"${label.escapeEmscript()}\")", "A11Y")
                }
            }
        }
        is CandidateTarget.Coordinate -> {
            diagnostics += diagnostic(
                "PUBLISH_COORDINATE_FALLBACK",
                "Der bestaetigte Schritt verwendet explizit Koordinaten statt eines semantischen Ziels.",
                RecorderPublishDiagnosticSeverity.WARNING,
                step.sourceCandidateId,
            )
            MappedCommand(
                RecorderStepMappingKind.COORDINATE_CLICK,
                "clickPoint(${target.xPx}, ${target.yPx}, 1)",
                "A11Y",
                coordinateFallback = true,
            )
        }
        else -> null.also {
            diagnostics += diagnostic(
                "PUBLISH_TAP_TARGET_INVALID",
                "Der Tap-Schritt besitzt keinen Tap-kompatiblen Zieltyp.",
                RecorderPublishDiagnosticSeverity.ERROR,
                step.sourceCandidateId,
            )
        }
    }

    private fun mapSwipe(
        step: ReviewedStep,
        diagnostics: MutableList<RecorderPublishDiagnostic>,
    ): MappedCommand? {
        val target = step.finalTarget as? CandidateTarget.SwipePath ?: run {
            diagnostics += diagnostic(
                "PUBLISH_SWIPE_TARGET_INVALID",
                "Der Swipe-Schritt besitzt keinen aufgezeichneten Pfad.",
                RecorderPublishDiagnosticSeverity.ERROR,
                step.sourceCandidateId,
            )
            return null
        }
        val points = target.points.ifEmpty { listOf(target.start, target.end) }
            .fold(mutableListOf<RecordingPoint>()) { result, point ->
                if (result.lastOrNull() != point) result += point
                result
            }
        if (points.size < 2 || target.durationMs <= 0L || (target.pointerId ?: 0) < 0) {
            diagnostics += diagnostic(
                "PUBLISH_SWIPE_PARAMETER_INVALID",
                "Swipe benoetigt mindestens zwei Punkte, positive Dauer und eine gueltige Pointer-ID.",
                RecorderPublishDiagnosticSeverity.ERROR,
                step.sourceCandidateId,
            )
            return null
        }
        val path = points.joinToString(",") { "point(${it.xPx},${it.yPx})" }
        val pointer = target.pointerId?.let { ",pointerId=$it" }.orEmpty()
        return MappedCommand(
            kind = if (points.size > 2) RecorderStepMappingKind.POINTER_PATH else RecorderStepMappingKind.SWIPE,
            command = "swipe(pointerPath(path($path),duration=${target.durationMs}$pointer))",
            capability = "A11Y",
        )
    }

    private fun mapWait(
        step: ReviewedStep,
        diagnostics: MutableList<RecorderPublishDiagnostic>,
    ): MappedCommand? {
        val target = step.finalTarget as? CandidateTarget.Delay
        if (target == null || target.durationMs < 0L) {
            diagnostics += diagnostic(
                "PUBLISH_WAIT_PARAMETER_INVALID",
                "Wait benoetigt eine nicht-negative Dauer.",
                RecorderPublishDiagnosticSeverity.ERROR,
                step.sourceCandidateId,
            )
            return null
        }
        return MappedCommand(RecorderStepMappingKind.WAIT, "wait(${target.durationMs})", "TIMING")
    }

    private fun mapScreenshot(
        step: ReviewedStep,
        diagnostics: MutableList<RecorderPublishDiagnostic>,
    ): MappedCommand? {
        val target = step.finalTarget as? CandidateTarget.Capture
        if (target == null || target.assetReference.isBlank()) {
            diagnostics += diagnostic(
                "PUBLISH_SCREENSHOT_PARAMETER_MISSING",
                "Screenshot benoetigt eine dauerhafte Asset-Referenz.",
                RecorderPublishDiagnosticSeverity.ERROR,
                step.sourceCandidateId,
            )
            return null
        }
        if (!assetExists(target.assetReference)) {
            diagnostics += diagnostic(
                "PUBLISH_CAPTURE_ASSET_MISSING",
                "Das referenzierte Capture-Asset fehlt; der Workflow bleibt strukturell gueltig, ist aber aktuell nicht vollstaendig ausfuehrbar.",
                RecorderPublishDiagnosticSeverity.WARNING,
                step.sourceCandidateId,
            )
        }
        return MappedCommand(
            RecorderStepMappingKind.SCREENSHOT,
            "screenshot(\"${target.assetReference.escapeEmscript()}\")",
            "SCREEN_CAPTURE",
        )
    }

    private data class MappedCommand(
        val kind: RecorderStepMappingKind,
        val command: String,
        val capability: String,
        val coordinateFallback: Boolean = false,
    )
}

private fun SemanticSourceApplyPlan.withStableRecorderIdentities(
    mappings: List<RecorderStepMapping>,
    baseline: WorkspaceDocument,
    diagnostics: MutableList<RecorderPublishDiagnostic>,
): SemanticSourceApplyPlan {
    val baselineIds = baseline.canonical?.entities.orEmpty().mapTo(hashSetOf()) { it.ref.id }
    val statements = orderedNewStatements(baseline)
    if (statements.size != mappings.size) {
        diagnostics += diagnostic(
            "PUBLISH_MAPPING_CARDINALITY_MISMATCH",
            "${mappings.size} Recorder-Schritte ergeben ${statements.size} neue kanonische Statements.",
            RecorderPublishDiagnosticSeverity.ERROR,
        )
        return this
    }
    val idRemap = statements.zip(mappings).associate { (entity, mapping) ->
        entity.ref.id to mapping.targetEntityId
    }
    val canonical = requireNotNull(targetDocument.canonical)
    val remappedEntities = canonical.entities.map { entity ->
        idRemap[entity.ref.id]?.let { entity.copy(ref = entity.ref.copy(id = it)) } ?: entity
    }
    val kinds = remappedEntities.associate { it.ref.id to it.ref.kind }
    val remappedRelations = canonical.relations.map { relation ->
        val sourceId = idRemap[relation.source.id] ?: relation.source.id
        val targetId = idRemap[relation.target.id] ?: relation.target.id
        val touchesPublishedStep = sourceId in idRemap.values || targetId in idRemap.values
        relation.copy(
            id = if (touchesPublishedStep) {
                SemanticRelationId(
                    "recorder-rel:${listOf(
                        sourceId.value,
                        targetId.value,
                        relation.kind.name,
                        relation.role.kind.name,
                        relation.role.name.orEmpty(),
                        relation.role.branchRole?.name.orEmpty(),
                        relation.order?.toString().orEmpty(),
                    ).joinToString("|").sha256(24)}"
                )
            } else relation.id,
            source = relation.source.copy(id = sourceId, kind = kinds.getValue(sourceId)),
            target = relation.target.copy(id = targetId, kind = kinds.getValue(targetId)),
        )
    }
    val stableTarget = targetDocument.copy(
        canonical = canonical.copy(
            entities = remappedEntities.sortedBy { it.ref.id.value },
            relations = remappedRelations.sortedBy { it.id.value },
        ),
    )
    return EmscriptSemanticSourceApply.plan(baseline, stableTarget)
}

private fun SemanticSourceApplyPlan.orderedNewStatements(baseline: WorkspaceDocument) =
    targetDocument.canonical.orEmptyEntities()
        .filter { entity ->
            entity.ref.kind == SemanticEntityKind.Statement &&
                baseline.canonical?.entity(entity.ref.id) == null
        }
        .sortedWith(compareBy(
            { entity ->
                entity.legacySourceId?.let(::BlockId)?.let(targetDocument.blocks::get)
                    ?.metadata?.get("emscript.source.line")?.toIntOrNull() ?: Int.MAX_VALUE
            },
            { it.legacySourceId.orEmpty() },
        ))

private fun de.visualtasker.workflow.core.CanonicalWorkspaceDocument?.orEmptyEntities() =
    this?.entities.orEmpty()

private fun SemanticSourceApplyPlan.bindNewStatementIds(
    mappings: List<RecorderStepMapping>,
    baseline: WorkspaceDocument,
    diagnostics: MutableList<RecorderPublishDiagnostic>,
): List<RecorderStepMapping> {
    val newStatements = orderedNewStatements(baseline)
    if (newStatements.size != mappings.size) {
        diagnostics += diagnostic(
            "PUBLISH_MAPPING_CARDINALITY_MISMATCH",
            "${mappings.size} Recorder-Schritte ergeben ${newStatements.size} neue kanonische Statements.",
            RecorderPublishDiagnosticSeverity.ERROR,
        )
        return emptyList()
    }
    return mappings.zip(newStatements) { mapping, entity ->
        mapping.copy(targetEntityId = entity.ref.id)
    }
}

private fun SemanticSourceApplyPlan.withRecorderProvenance(
    mappings: List<RecorderStepMapping>,
): SemanticSourceApplyPlan {
    val provenanceByBlock = mappings.mapNotNull { mapping ->
        targetDocument.canonical?.entity(mapping.targetEntityId)?.legacySourceId?.let(::BlockId)?.let { blockId ->
            blockId to mapping.provenance
        }
    }.toMap()
    fun WorkspaceDocument.annotated(): WorkspaceDocument = copy(
        blocks = blocks.mapValues { (blockId, block) ->
            provenanceByBlock[blockId]?.let { provenance -> block.copy(metadata = block.metadata + provenance) } ?: block
        }
    )
    return copy(
        stagedDocument = stagedDocument.annotated(),
        targetDocument = targetDocument.annotated(),
    )
}

private fun diagnostic(
    code: String,
    message: String,
    severity: RecorderPublishDiagnosticSeverity,
    candidateId: String? = null,
) = RecorderPublishDiagnostic(code, message, severity, candidateId)

private fun String.sha256(length: Int): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray())
    .joinToString("") { "%02x".format(it) }
    .take(length)

private fun String.escapeEmscript(): String = buildString(length) {
    this@escapeEmscript.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}
