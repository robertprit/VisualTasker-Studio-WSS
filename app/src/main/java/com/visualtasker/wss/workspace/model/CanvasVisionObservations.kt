package com.visualtasker.wss.workspace.model

object CanvasVisionObservationFactory {
    fun create(
        idSeed: String,
        label: String,
        matchKind: String,
        processingMode: String,
        bounds: WorldviewRect,
        score: Float,
        threshold: Float,
        assetId: String?,
        referenceId: String?,
        observedAtEpochMs: Long,
    ): WorldObservation {
        val normalizedMatchKind = matchKind.trim().uppercase()
        val provider = when (normalizedMatchKind) {
            "OCR" -> ObservationProvider.Ocr
            "OCV" -> ObservationProvider.OpenCv
            "YOLO" -> ObservationProvider.Yolo
            "A11Y" -> ObservationProvider.Accessibility
            "DOM" -> ObservationProvider.Dom
            else -> ObservationProvider.Unknown
        }
        val kind = when (provider) {
            ObservationProvider.Ocr -> ObservationKind.Text
            ObservationProvider.OpenCv -> ObservationKind.TemplateMatch
            ObservationProvider.Yolo -> ObservationKind.ObjectDetection
            ObservationProvider.Accessibility -> ObservationKind.Bounds
            ObservationProvider.Dom -> ObservationKind.DomElement
            else -> ObservationKind.Unknown
        }
        val safeSeed = idSeed.stableWorldIdSegment()
        return WorldObservation(
            id = "observation:vision:$safeSeed",
            provider = provider,
            kind = kind,
            confidence = score.coerceIn(0f, 1f),
            observedAtEpochMs = observedAtEpochMs,
            bounds = bounds,
            properties = mapOf(
                "text" to label.trim().ifBlank { normalizedMatchKind.ifBlank { "Vision" } },
                "matchKind" to normalizedMatchKind,
                "processingMode" to processingMode.trim().ifBlank { "Original" },
                "score" to score.coerceIn(0f, 1f).toString(),
                "threshold" to threshold.coerceIn(0f, 1f).toString(),
                "assetId" to assetId.orEmpty(),
                "referenceId" to referenceId.orEmpty(),
                "source" to "vision-panel",
            ).filterValues { it.isNotBlank() },
        )
    }
}

private fun String.stableWorldIdSegment(): String =
    lowercase()
        .replace(Regex("[^a-z0-9._:-]"), "-")
        .trim('-')
        .ifBlank { hashCode().toString().replace("-", "n") }
