package com.visualtasker.wss.workspace.model

enum class JunctionEvidenceKind {
    RecordingStep,
    ScreenshotRegion,
    Marker,
    TemplateMatch,
    AccessibilityNode,
    OcrText,
    YoloObject,
    RuntimeTrace,
    UserIntent,
    DatastoreFact,
}

enum class JunctionConfidence {
    Low,
    Medium,
    High,
    Verified,
}

enum class JunctionOutputTarget {
    Marker,
    Block,
    FlowNode,
    Emscript,
    Dataset,
}

data class JunctionEvidence(
    val id: String,
    val kind: JunctionEvidenceKind,
    val label: String,
    val sourceId: String? = null,
    val activityName: String? = null,
    val payload: Map<String, String> = emptyMap(),
)

data class JunctionCandidate(
    val id: String,
    val label: String,
    val description: String,
    val confidence: JunctionConfidence,
    val evidenceIds: List<String>,
    val outputTargets: Set<JunctionOutputTarget>,
    val parameters: Map<String, String> = emptyMap(),
)

data class JunctionPlan(
    val id: String,
    val label: String,
    val candidates: List<JunctionCandidate>,
    val evidence: List<JunctionEvidence>,
) {
    val primaryCandidate: JunctionCandidate?
        get() = candidates.maxByOrNull { it.confidence.ordinal }
}

object JunctionatorSeed {
    fun fromRailTraceStep(step: RecorderStepUi): JunctionPlan {
        val evidence = JunctionEvidence(
            id = "evidence:${step.id}",
            kind = JunctionEvidenceKind.RecordingStep,
            label = step.label,
            sourceId = step.id,
            activityName = step.activityName,
            payload = buildMap {
                put("actionType", step.actionType)
                step.timestampMs?.let { put("timestampMs", it.toString()) }
                step.durationMs?.let { put("durationMs", it.toString()) }
                step.detail?.let { put("detail", it) }
            },
        )
        val target = when {
            step.actionType.contains("click", ignoreCase = true) -> setOf(
                JunctionOutputTarget.Marker,
                JunctionOutputTarget.Block,
                JunctionOutputTarget.FlowNode,
                JunctionOutputTarget.Emscript,
            )
            step.actionType.contains("activity", ignoreCase = true) -> setOf(
                JunctionOutputTarget.Marker,
                JunctionOutputTarget.Dataset,
            )
            else -> setOf(JunctionOutputTarget.Dataset)
        }
        return JunctionPlan(
            id = "junction:${step.id}",
            label = "Pfad aus ${step.label}",
            evidence = listOf(evidence),
            candidates = listOf(
                JunctionCandidate(
                    id = "candidate:${step.id}:direct",
                    label = step.label,
                    description = "Direkter Vorschlag aus RailTrace-Step ${step.id}",
                    confidence = if (step.status == StepStatus.Executed) JunctionConfidence.Verified else JunctionConfidence.Medium,
                    evidenceIds = listOf(evidence.id),
                    outputTargets = target,
                    parameters = evidence.payload,
                )
            ),
        )
    }
}
