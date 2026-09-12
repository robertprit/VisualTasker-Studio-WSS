package com.visualtasker.wss.workspace.model

object RecorderObservationProjector {
    fun project(steps: List<RecorderStepUi>): List<WorldObservation> =
        steps.mapNotNull { step -> step.toWorldObservationOrNull() }
            .distinctBy { it.id }
            .sortedBy { it.id }
}

fun List<RecorderStepUi>.toRecorderWorldObservations(): List<WorldObservation> =
    RecorderObservationProjector.project(this)

private fun RecorderStepUi.toWorldObservationOrNull(): WorldObservation? {
    if (bounds == null && point == null) return null
    val normalizedType = actionType.trim().lowercase()
    val provider = if (normalizedType in AccessibilityStepTypes) {
        ObservationProvider.Accessibility
    } else {
        ObservationProvider.Runtime
    }
    val kind = when (normalizedType) {
        "click", "tap", "longclick", "long_click", "swipe", "gesture.start", "gesture.end" -> ObservationKind.Touch
        "text.change" -> ObservationKind.Text
        "activity.change" -> ObservationKind.RuntimeEvent
        "focus" -> ObservationKind.Bounds
        else -> ObservationKind.RuntimeEvent
    }
    return WorldObservation(
        id = "observation:record:${id.stableRecorderObservationId()}",
        provider = provider,
        kind = kind,
        confidence = if (status == StepStatus.Invalid) 0.2f else 1f,
        observedAtEpochMs = timestampMs ?: 0L,
        bounds = bounds,
        point = point,
        properties = mapOf(
            "text" to label,
            "actionType" to actionType,
            "status" to status.name,
            "activity" to activityName.orEmpty(),
            "durationMs" to durationMs?.toString().orEmpty(),
            "source" to "railtrace-recording",
            "clickable" to (normalizedType == "click" || normalizedType == "tap").toString(),
            "focusable" to (normalizedType == "focus").toString(),
            "visible" to "true",
        ).filterValues { it.isNotBlank() } + properties,
    )
}

private val AccessibilityStepTypes = setOf(
    "click",
    "tap",
    "longclick",
    "long_click",
    "text.change",
    "scroll",
    "focus",
    "activity.change",
    "gesture.start",
    "gesture.end",
)

private fun String.stableRecorderObservationId(): String =
    lowercase()
        .replace(Regex("[^a-z0-9._:-]"), "-")
        .trim('-')
        .ifBlank { hashCode().toString().replace("-", "n") }
