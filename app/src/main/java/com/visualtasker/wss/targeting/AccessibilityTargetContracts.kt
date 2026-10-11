package com.visualtasker.wss.targeting

data class ElementBoundsPx(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val isValid: Boolean get() = right > left && bottom > top
    val centerX: Int get() = left + (right - left) / 2
    val centerY: Int get() = top + (bottom - top) / 2
}

enum class TextMatchMode { EXACT, CONTAINS }

data class ElementQuery(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val className: String? = null,
    val packageName: String? = null,
    val bounds: ElementBoundsPx? = null,
    val clickable: Boolean? = null,
    val enabled: Boolean? = true,
    val visible: Boolean? = true,
    val displayId: Int,
    val textMatchMode: TextMatchMode = TextMatchMode.EXACT,
    val matchTextOrDescription: Boolean = false,
) {
    val queryType: String
        get() = when {
            viewId != null -> "VIEW_ID"
            contentDescription != null -> "CONTENT_DESCRIPTION"
            text != null -> "TEXT"
            bounds != null -> "BOUNDS"
            else -> "COMPOSITE"
        }
}

data class ElementRef(
    val snapshotNodeId: String,
    val packageName: String?,
    val className: String,
    val viewId: String?,
    val bounds: ElementBoundsPx,
    val windowId: String?,
    val displayId: Int,
)

data class ObservedAccessibilityElement(
    val ref: ElementRef,
    val parentNodeId: String?,
    val childOrder: Int,
    val text: String?,
    val contentDescription: String?,
    val clickable: Boolean,
    val enabled: Boolean,
    val visible: Boolean,
    val children: List<ObservedAccessibilityElement> = emptyList(),
)

data class AccessibilityTreeSnapshot(
    val snapshotId: String,
    val capturedAtEpochMs: Long,
    val displayId: Int,
    val windowId: String?,
    val packageName: String?,
    val root: ObservedAccessibilityElement,
)

data class ElementMatch(
    val observed: ElementRef,
    val actionTarget: ElementRef,
    val usedClickableAncestor: Boolean,
)

enum class TargetResolutionStatus {
    FOUND,
    NOT_FOUND,
    AMBIGUOUS,
    STALE,
    NOT_ACTIONABLE,
    PERMISSION_DENIED,
    SERVICE_UNAVAILABLE,
    DISPLAY_UNSUPPORTED,
}

data class TargetResolutionPolicy(
    val displayId: Int,
    val allowClickableAncestor: Boolean = true,
    val allowCoordinateFallback: Boolean = false,
)

data class TargetResolution(
    val status: TargetResolutionStatus,
    val query: ElementQuery,
    val match: ElementMatch? = null,
    val candidateCount: Int = 0,
    val candidateRefs: List<ElementRef> = emptyList(),
)

enum class TargetActionSubmission { ACCEPTED, STALE, REJECTED }

object AccessibilityTargetResolver {
    fun resolve(
        snapshot: AccessibilityTreeSnapshot,
        query: ElementQuery,
        policy: TargetResolutionPolicy,
    ): TargetResolution {
        if (query.displayId != policy.displayId || snapshot.displayId != policy.displayId) {
            return TargetResolution(TargetResolutionStatus.DISPLAY_UNSUPPORTED, query)
        }
        val flattened = snapshot.root.flattenPreOrder()
        val parentById = flattened.associateBy { it.ref.snapshotNodeId }
        val candidates = flattened.filter { it.matches(query) }
        if (candidates.isEmpty()) return TargetResolution(TargetResolutionStatus.NOT_FOUND, query)

        val matches = candidates.mapNotNull { observed ->
            val action = when {
                observed.clickable -> observed
                policy.allowClickableAncestor -> observed.nearestClickableAncestor(parentById)
                else -> null
            }
            action?.let {
                ElementMatch(
                    observed = observed.ref,
                    actionTarget = it.ref,
                    usedClickableAncestor = it.ref.snapshotNodeId != observed.ref.snapshotNodeId,
                )
            }
        }
        val uniqueMatches = matches.distinctBy { it.actionTarget.snapshotNodeId }
        if (uniqueMatches.isEmpty()) {
            return TargetResolution(
                status = TargetResolutionStatus.NOT_ACTIONABLE,
                query = query,
                candidateCount = candidates.size,
                candidateRefs = candidates.map { it.ref },
            )
        }
        if (uniqueMatches.size != 1) {
            return TargetResolution(
                status = TargetResolutionStatus.AMBIGUOUS,
                query = query,
                candidateCount = uniqueMatches.size,
                candidateRefs = uniqueMatches.map { it.actionTarget },
            )
        }
        return TargetResolution(
            status = TargetResolutionStatus.FOUND,
            query = query,
            match = uniqueMatches.single(),
            candidateCount = 1,
            candidateRefs = listOf(uniqueMatches.single().actionTarget),
        )
    }

    private fun ObservedAccessibilityElement.flattenPreOrder(): List<ObservedAccessibilityElement> =
        buildList {
            add(this@flattenPreOrder)
            children.forEach { addAll(it.flattenPreOrder()) }
        }

    private fun ObservedAccessibilityElement.nearestClickableAncestor(
        byId: Map<String, ObservedAccessibilityElement>,
    ): ObservedAccessibilityElement? {
        var currentId = parentNodeId
        while (currentId != null) {
            val parent = byId[currentId] ?: return null
            if (parent.clickable && parent.enabled && parent.visible) return parent
            currentId = parent.parentNodeId
        }
        return null
    }

    private fun ObservedAccessibilityElement.matches(query: ElementQuery): Boolean {
        if (query.enabled != null && enabled != query.enabled) return false
        if (query.visible != null && visible != query.visible) return false
        if (query.clickable != null && clickable != query.clickable) return false
        if (query.viewId != null && ref.viewId != query.viewId) return false
        if (query.className != null && ref.className != query.className) return false
        if (query.packageName != null && ref.packageName != query.packageName) return false
        if (query.bounds != null && ref.bounds != query.bounds) return false
        if (query.contentDescription != null && !contentDescription.matchesText(query.contentDescription, query.textMatchMode)) {
            return false
        }
        if (query.text != null) {
            val values = if (query.matchTextOrDescription) listOf(text, contentDescription) else listOf(text)
            if (values.none { it.matchesText(query.text, query.textMatchMode) }) return false
        }
        return true
    }

    private fun String?.matchesText(expected: String, mode: TextMatchMode): Boolean {
        val actual = this?.trim().orEmpty()
        val needle = expected.trim()
        if (actual.isEmpty() || needle.isEmpty()) return false
        return when (mode) {
            TextMatchMode.EXACT -> actual.equals(needle, ignoreCase = true)
            TextMatchMode.CONTAINS -> actual.contains(needle, ignoreCase = true)
        }
    }
}
