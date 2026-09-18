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

object JunktorSeed {
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
        val confidence = if (step.status == StepStatus.Executed) {
            JunctionConfidence.Verified
        } else {
            JunctionConfidence.Medium
        }
        val command = step.toJunktorEmscript()
        val markerCommand = step.toJunktorMarkerEmscript()
        val candidates = buildList {
            markerCommand?.let { emscript ->
                add(
                    step.toCandidate(
                        suffix = "marker",
                        target = JunctionOutputTarget.Marker,
                        description = "Marker aus Recorder-Geometrie ${step.id}",
                        confidence = confidence,
                        evidenceId = evidence.id,
                        emscript = emscript,
                    )
                )
            }
            command?.let { emscript ->
                listOf(
                    JunctionOutputTarget.Block,
                    JunctionOutputTarget.FlowNode,
                    JunctionOutputTarget.Emscript,
                ).forEach { target ->
                    add(
                        step.toCandidate(
                            suffix = target.name.lowercase(),
                            target = target,
                            description = "${target.name}-Vorschlag aus Recorder-Step ${step.id}",
                            confidence = confidence,
                            evidenceId = evidence.id,
                            emscript = emscript,
                        )
                    )
                }
            }
            if (isEmpty()) {
                add(
                    step.toCandidate(
                        suffix = "dataset",
                        target = JunctionOutputTarget.Dataset,
                        description = "Recorder-Evidence fuer Datastore aus Step ${step.id}",
                        confidence = confidence,
                        evidenceId = evidence.id,
                    )
                )
            }
        }
        return JunctionPlan(
            id = "junction:${step.id}",
            label = "Pfad aus ${step.label}",
            evidence = listOf(evidence),
            candidates = candidates,
        )
    }
}

private fun RecorderStepUi.toCandidate(
    suffix: String,
    target: JunctionOutputTarget,
    description: String,
    confidence: JunctionConfidence,
    evidenceId: String,
    emscript: String? = null,
): JunctionCandidate =
    JunctionCandidate(
        id = "candidate:$id:$suffix",
        label = label,
        description = description,
        confidence = confidence,
        evidenceIds = listOf(evidenceId),
        outputTargets = setOf(target),
        parameters = buildMap {
            put("stepId", id)
            put("actionType", actionType)
            put("suggestionTarget", target.name)
            emscript?.let { put("emscript", it) }
            activityName?.let { put("activityName", it) }
            detail?.let { put("detail", it) }
        },
    )

private fun RecorderStepUi.toJunktorEmscript(): String? {
    val normalizedType = actionType.trim().lowercase()
    val text = properties["text"]
        ?: properties["contentDescription"]
        ?: label.removePrefix("Click").removePrefix("Tap").trim().takeIf { it.isNotBlank() }
    return when {
        normalizedType in setOf("click", "tap", "longclick", "long_click") -> when {
            point != null -> "clickPoint(${point.x.toInt()}, ${point.y.toInt()}, 1)"
            !text.isNullOrBlank() -> "click(\"${text.escapeJunktorString()}\")"
            else -> null
        }
        normalizedType in setOf("swipe", "scroll") && bounds != null -> {
            val x = ((bounds.left + bounds.right) / 2f).toInt()
            "swipe([$x, ${bounds.bottom.toInt()}, $x, ${bounds.top.toInt()}], 1)"
        }
        normalizedType in setOf("text.change", "text", "input") ->
            text?.let { "log(\"Text geaendert: ${it.escapeJunktorString()}\")" }
        normalizedType == "wait" ->
            "wait(${detail.orEmpty().filter(Char::isDigit).ifBlank { "100" }})"
        normalizedType == "beep" -> "beep()"
        normalizedType == "vibrate" -> "vibrate(40)"
        normalizedType == "log" -> "log(\"${(detail ?: label).escapeJunktorString()}\")"
        else -> null
    }
}

private fun RecorderStepUi.toJunktorMarkerEmscript(): String? {
    val markerLabel = properties["text"]
        ?: properties["contentDescription"]
        ?: label
    return when {
        bounds != null -> {
            val width = (bounds.right - bounds.left).toInt().coerceAtLeast(1)
            val height = (bounds.bottom - bounds.top).toInt().coerceAtLeast(1)
            "markerSave(\"${markerLabel.escapeJunktorString()}\", " +
                "region(${bounds.left.toInt()}, ${bounds.top.toInt()}, $width, $height))"
        }
        point != null ->
            "markerSave(\"${markerLabel.escapeJunktorString()}\", point(${point.x.toInt()}, ${point.y.toInt()}))"
        else -> null
    }
}

private fun String.escapeJunktorString(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")
