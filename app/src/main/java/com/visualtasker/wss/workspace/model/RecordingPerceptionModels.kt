package com.visualtasker.wss.workspace.model

enum class ObservationPolicy {
    RECORD,
    WATCHDOG,
    RUNTIME,
    INSPECT,
    PROBE,
}

data class WindowSnapshot(
    val id: String,
    val packageName: String?,
    val activityName: String?,
    val title: String? = null,
    val bounds: WorldviewRect? = null,
    val focused: Boolean = false,
    val active: Boolean = false,
    val displayId: Int? = null,
    val timestampMs: Long,
    val properties: Map<String, String> = emptyMap(),
) {
    init {
        require(id.isNotBlank() && id == id.trim()) {
            "Window snapshot id must be nonblank and trimmed."
        }
        require(timestampMs >= 0L) {
            "Window snapshot timestamp must not be negative."
        }
    }
}

data class WindowContext(
    val packageName: String?,
    val activityName: String?,
    val windows: List<WindowSnapshot>,
    val focusedWindowId: String? = windows.firstOrNull { it.focused }?.id,
    val activeWindowId: String? = windows.firstOrNull { it.active }?.id,
    val displayId: Int? = null,
    val timestampMs: Long,
) {
    init {
        require(timestampMs >= 0L) {
            "Window context timestamp must not be negative."
        }
        require(windows.map { it.id }.distinct().size == windows.size) {
            "Window context must not contain duplicate window ids."
        }
    }
}

enum class WindowTransitionKind {
    ACTIVITY_CHANGED,
    WINDOW_APPEARED,
    WINDOW_DISAPPEARED,
    FOCUS_CHANGED,
    ACTIVE_WINDOW_CHANGED,
    BOUNDS_CHANGED,
}

data class WindowTransitionEvidence(
    val kind: WindowTransitionKind,
    val previous: WindowSnapshot?,
    val current: WindowSnapshot?,
    val reason: String,
    val timestampMs: Long,
    val confidence: Float,
    val sceneBoundaryCandidate: Boolean = false,
    val properties: Map<String, String> = emptyMap(),
) {
    init {
        require(timestampMs >= 0L) {
            "Window transition timestamp must not be negative."
        }
        require(confidence in 0f..1f) {
            "Window transition confidence must stay in 0..1."
        }
        require(reason.isNotBlank() && reason == reason.trim()) {
            "Window transition reason must be nonblank and trimmed."
        }
    }
}

data class WindowRecordingEvidence(
    val kind: String,
    val label: String,
    val attributes: Map<String, String>,
)

class WindowRecordingEvidenceReducer {
    private var previousContext: WindowContext? = null

    fun accept(current: WindowContext): WindowRecordingEvidence? {
        val previous = previousContext
        val transitions = WindowTransitionDetector.detect(previous, current)
        previousContext = current
        if (previous == null) {
            return WindowRecordingEvidence(
                kind = "window.baseline",
                label = "Window Baseline",
                attributes = current.baselineAttributes(),
            )
        }
        if (transitions.isEmpty()) {
            return null
        }
        return WindowRecordingEvidence(
            kind = "window.transition",
            label = transitions.joinToString(separator = ", ") { it.kind.name },
            attributes = transitions.transitionAttributes(previous, current),
        )
    }
}

object WindowTransitionDetector {
    fun detect(previous: WindowContext?, current: WindowContext): List<WindowTransitionEvidence> {
        if (previous == null) return emptyList()
        val transitions = mutableListOf<WindowTransitionEvidence>()

        val packageChanged = !current.packageName.isNullOrBlank() &&
            previous.packageName != current.packageName
        val activityChanged = !current.activityName.isNullOrBlank() &&
            !previous.activityName.isNullOrBlank() &&
            previous.activityName != current.activityName
        if (packageChanged || activityChanged) {
            transitions += WindowTransitionEvidence(
                kind = WindowTransitionKind.ACTIVITY_CHANGED,
                previous = previous.primarySnapshot(),
                current = current.primarySnapshot(),
                reason = if (activityChanged) "activity-context-changed" else "package-context-changed",
                timestampMs = current.timestampMs,
                confidence = if (activityChanged) 1f else 0.6f,
                properties = mapOf(
                    "previousPackage" to previous.packageName.orEmpty(),
                    "currentPackage" to current.packageName.orEmpty(),
                    "previousActivity" to previous.activityName.orEmpty(),
                    "currentActivity" to current.activityName.orEmpty(),
                ),
            )
        }

        val previousById = previous.windows.associateBy { it.id }
        val currentById = current.windows.associateBy { it.id }
        val previousIds = previousById.keys
        val currentIds = currentById.keys

        (currentIds - previousIds).sorted().forEach { id ->
            transitions += WindowTransitionEvidence(
                kind = WindowTransitionKind.WINDOW_APPEARED,
                previous = null,
                current = currentById.getValue(id),
                reason = "window-added",
                timestampMs = current.timestampMs,
                confidence = 1f,
            )
        }

        (previousIds - currentIds).sorted().forEach { id ->
            transitions += WindowTransitionEvidence(
                kind = WindowTransitionKind.WINDOW_DISAPPEARED,
                previous = previousById.getValue(id),
                current = null,
                reason = "window-removed",
                timestampMs = current.timestampMs,
                confidence = 1f,
            )
        }

        (previousIds intersect currentIds).sorted().forEach { id ->
            val before = previousById.getValue(id)
            val after = currentById.getValue(id)
            if (before.focused != after.focused) {
                transitions += WindowTransitionEvidence(
                    kind = WindowTransitionKind.FOCUS_CHANGED,
                    previous = before,
                    current = after,
                    reason = "window-focus-changed",
                    timestampMs = current.timestampMs,
                    confidence = 1f,
                )
            }
            if (before.active != after.active) {
                transitions += WindowTransitionEvidence(
                    kind = WindowTransitionKind.ACTIVE_WINDOW_CHANGED,
                    previous = before,
                    current = after,
                    reason = "active-window-changed",
                    timestampMs = current.timestampMs,
                    confidence = 1f,
                )
            }
            if (before.bounds != after.bounds) {
                transitions += WindowTransitionEvidence(
                    kind = WindowTransitionKind.BOUNDS_CHANGED,
                    previous = before,
                    current = after,
                    reason = "window-bounds-changed",
                    timestampMs = current.timestampMs,
                    confidence = 1f,
                )
            }
        }

        return transitions
    }

    private fun WindowContext.primarySnapshot(): WindowSnapshot? =
        activeWindowId?.let { id -> windows.firstOrNull { it.id == id } }
            ?: focusedWindowId?.let { id -> windows.firstOrNull { it.id == id } }
            ?: windows.firstOrNull()
}

private fun WindowContext.baselineAttributes(): Map<String, String> =
    buildMap {
        put("recording.evidence", "window.baseline")
        put("observationPolicy", ObservationPolicy.RECORD.name)
        put("window.count", windows.size.toString())
        put("window.timestampMs", timestampMs.toString())
        packageName?.let { put("window.package", it) }
        activityName?.let { put("window.activity", it) }
        activeWindowId?.let { put("window.activeId", it) }
        focusedWindowId?.let { put("window.focusedId", it) }
        put("window.ids", windows.joinToString(separator = ",") { it.id })
    }

private fun List<WindowTransitionEvidence>.transitionAttributes(
    previous: WindowContext,
    current: WindowContext,
): Map<String, String> {
    val transitions = this
    val firstTransition = transitions.firstOrNull()
    return buildMap {
        put("recording.evidence", "window.transition")
        put("observationPolicy", ObservationPolicy.RECORD.name)
        put("window.transitionCount", transitions.size.toString())
        put("window.transitionKinds", transitions.joinToString(separator = ",") { it.kind.name })
        put("window.transitionReasons", transitions.joinToString(separator = ",") { it.reason })
        put("window.timestampMs", current.timestampMs.toString())
        put("window.previousPackage", previous.packageName.orEmpty())
        put("window.currentPackage", current.packageName.orEmpty())
        put("window.previousActivity", previous.activityName.orEmpty())
        put("window.currentActivity", current.activityName.orEmpty())
        put("window.previousCount", previous.windows.size.toString())
        put("window.currentCount", current.windows.size.toString())
        firstTransition?.confidence?.let { put("window.confidence", it.toString()) }
        firstTransition?.previous?.id?.let { put("window.previousId", it) }
        firstTransition?.current?.id?.let { put("window.currentId", it) }
    }
}
