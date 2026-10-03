package com.visualtasker.wss.emscript.runtime

internal data class RuntimeTextMatchCandidate(
    val stableId: String,
    val text: String,
    val region: EmscriptRegionValue,
    val confidence: Double,
    val source: String,
    val observedAtEpochMs: Long,
) {
    init {
        require(stableId.isNotBlank()) { "Text candidate id must not be blank." }
        require(text.isNotBlank()) { "Text candidate text must not be blank." }
        require(source.isNotBlank()) { "Text candidate source must not be blank." }
        require(confidence.isFinite() && confidence in 0.0..1.0) {
            "Text candidate confidence must stay in 0..1."
        }
    }
}

internal object TextMatchQueryRuntime {
    fun findBest(
        query: String,
        candidates: List<RuntimeTextMatchCandidate>,
    ): RuntimeAdapterResult {
        val requestedText = query.trim()
        if (requestedText.isEmpty()) {
            return RuntimeAdapterResult.failure(
                diagnosticCode = RuntimeQueryDiagnosticCodes.VISION_RESULT_INVALID,
                message = "vision.findText erwartet einen nicht leeren Suchtext.",
            )
        }

        val best = candidates
            .asSequence()
            .filter { it.text.contains(requestedText, ignoreCase = true) }
            .sortedWith(
                compareByDescending<RuntimeTextMatchCandidate> {
                    it.text.equals(requestedText, ignoreCase = true)
                }.thenByDescending { it.confidence }
                    .thenByDescending { it.observedAtEpochMs }
                    .thenBy { it.stableId },
            )
            .firstOrNull()
            ?: return RuntimeAdapterResult.success(
                value = EmscriptValue.NullValue,
                message = "vision.findText -> ABSENT",
            )

        return RuntimeAdapterResult.success(
            value = EmscriptValue.TextMatchValue(
                text = best.text,
                region = best.region,
                confidence = best.confidence,
                source = best.source,
            ),
            message = "vision.findText -> VALUE(confidence=${best.confidence})",
        )
    }
}
