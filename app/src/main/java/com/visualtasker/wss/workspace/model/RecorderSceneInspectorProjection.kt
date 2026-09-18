package com.visualtasker.wss.workspace.model

data class RecorderSceneInspectorProjection(
    val stepId: String,
    val sceneLabel: String,
    val actionLabel: String,
    val status: StepStatus,
    val timestampMs: Long?,
    val durationMs: Long?,
    val evidence: List<Pair<String, String>>,
    val observations: List<WorldObservation>,
    val canvasProjection: RecorderCanvasProjection,
)

object RecorderSceneInspectorProjector {
    fun project(
        step: RecorderStepUi,
        recorderObservations: List<WorldObservation>,
    ): RecorderSceneInspectorProjection {
        val sceneLabel = step.activityName
            ?: step.properties.firstNonBlank("activity", "window.activity", "window.currentActivity", "package")
            ?: "Unbekannte Scene"
        val matchingObservations = recorderObservations
            .filter { observation ->
                observation.properties["activity"] == step.activityName ||
                    observation.properties["stepId"] == step.id ||
                    observation.id.endsWith(step.id)
            }
            .distinctBy(WorldObservation::id)
        val evidence = buildList {
            step.detail?.takeIf(String::isNotBlank)?.let { add("Detail" to it) }
            step.properties.entries
                .filter { (key, value) ->
                    value.isNotBlank() && (
                        key.startsWith("recording.") ||
                            key.startsWith("window.") ||
                            key in EvidenceKeys
                        )
                }
                .sortedBy(Map.Entry<String, String>::key)
                .forEach { (key, value) -> add(key to value) }
        }
        return RecorderSceneInspectorProjection(
            stepId = step.id,
            sceneLabel = sceneLabel,
            actionLabel = step.label,
            status = step.status,
            timestampMs = step.timestampMs,
            durationMs = step.durationMs,
            evidence = evidence,
            observations = matchingObservations,
            canvasProjection = step.toRecorderCanvasProjection(),
        )
    }
}

private val EvidenceKeys = setOf(
    "source",
    "kind",
    "bounds",
    "x",
    "y",
    "screenshotPath",
    "observationPolicy",
    "eventClass",
    "package",
)

private fun Map<String, String>.firstNonBlank(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> this[key]?.trim()?.takeIf(String::isNotBlank) }
