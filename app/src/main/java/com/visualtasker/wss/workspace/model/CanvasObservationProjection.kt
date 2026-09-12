package com.visualtasker.wss.workspace.model

enum class CanvasObservationFamily {
    AccessibilityVisible,
    AccessibilityClickable,
    AccessibilityFocusable,
    Ocr,
    Ocv,
    Yolo,
    Dom,
    Marker,
    Unknown,
}

enum class CanvasObservationLineStyle {
    Solid,
    Bold,
    Dashed,
    Underline,
    Capsule,
}

data class CanvasObservationFilters(
    val showAccessibility: Boolean = true,
    val showClickable: Boolean = true,
    val showVisible: Boolean = true,
    val showFocusable: Boolean = true,
    val showOcr: Boolean = true,
    val showOcv: Boolean = true,
    val showYolo: Boolean = true,
    val showDom: Boolean = false,
    val showMarkers: Boolean = true,
)

data class CanvasObservationItem(
    val id: String,
    val sourceId: String,
    val label: String,
    val family: CanvasObservationFamily,
    val lineStyle: CanvasObservationLineStyle,
    val bounds: WorldviewRect? = null,
    val point: WorldviewPoint? = null,
    val confidence: Float = 1f,
    val selected: Boolean = false,
    val evidenceRefs: Set<String> = emptySet(),
)

data class CanvasObservationProjection(
    val worldviewRevision: Long,
    val items: List<CanvasObservationItem>,
    val ambiguityCandidates: List<WorldAmbiguity>,
)

object CanvasObservationProjector {
    fun project(
        document: WorldviewDocument,
        filters: CanvasObservationFilters = CanvasObservationFilters(),
        selectedSourceId: String? = null,
    ): CanvasObservationProjection {
        val items = document.observations
            .mapNotNull { observation -> observation.toCanvasItem(document, selectedSourceId) }
            .filter { it.matches(filters) }
            .sortedWith(compareBy<CanvasObservationItem> { it.family.name }.thenBy { it.label }.thenBy { it.id })
        return CanvasObservationProjection(
            worldviewRevision = document.revision,
            items = items,
            ambiguityCandidates = items.toAmbiguityCandidates(document),
        )
    }
}

private fun WorldObservation.toCanvasItem(
    document: WorldviewDocument,
    selectedSourceId: String?,
): CanvasObservationItem? {
    val rect = bounds
    val pointValue = point
    if (rect == null && pointValue == null) return null
    val resourceId = entityId
        ?.removePrefix("entity:")
        ?.takeIf { document.resources.find(it) != null }
    val entity = entityId?.let(document::findEntity)
    val resource = resourceId?.let(document.resources::find)
    val family = canvasFamily(resource)
    return CanvasObservationItem(
        id = id,
        sourceId = resource?.id ?: id,
        label = resource?.label
            ?: entity?.label
            ?: properties["text"]
            ?: kind.name,
        family = family,
        lineStyle = canvasLineStyle(family, resource),
        bounds = rect,
        point = pointValue,
        confidence = confidence,
        selected = selectedSourceId != null && (selectedSourceId == id || selectedSourceId == resource?.id),
        evidenceRefs = setOf(id) + listOfNotNull(entityId, resource?.id),
    )
}

private fun WorldObservation.canvasFamily(resource: WorkspaceResource?): CanvasObservationFamily {
    if (provider == ObservationProvider.Accessibility) {
        return when {
            properties["clickable"].toBooleanLenient() -> CanvasObservationFamily.AccessibilityClickable
            properties["focusable"].toBooleanLenient() -> CanvasObservationFamily.AccessibilityFocusable
            else -> CanvasObservationFamily.AccessibilityVisible
        }
    }
    return when {
        provider == ObservationProvider.Ocr -> CanvasObservationFamily.Ocr
        provider == ObservationProvider.OpenCv || kind == ObservationKind.TemplateMatch -> CanvasObservationFamily.Ocv
        provider == ObservationProvider.Yolo || kind == ObservationKind.ObjectDetection -> CanvasObservationFamily.Yolo
        provider == ObservationProvider.Dom || kind == ObservationKind.DomElement -> CanvasObservationFamily.Dom
        provider == ObservationProvider.Import && resource != null -> resource.canvasFamilyFromResource()
        else -> CanvasObservationFamily.Unknown
    }
}

private fun WorkspaceResource.canvasFamilyFromResource(): CanvasObservationFamily =
    when {
        metadata["matchKind"].equals("OCR", ignoreCase = true) -> CanvasObservationFamily.Ocr
        metadata["matchKind"].equals("OCV", ignoreCase = true) -> CanvasObservationFamily.Ocv
        kind == WorkspaceResourceKind.Template -> CanvasObservationFamily.Ocv
        kind == WorkspaceResourceKind.Marker || kind == WorkspaceResourceKind.Region -> CanvasObservationFamily.Marker
        else -> CanvasObservationFamily.Unknown
    }

private fun canvasLineStyle(
    family: CanvasObservationFamily,
    resource: WorkspaceResource?,
): CanvasObservationLineStyle =
    when {
        resource?.kind == WorkspaceResourceKind.Template -> CanvasObservationLineStyle.Dashed
        family == CanvasObservationFamily.AccessibilityClickable -> CanvasObservationLineStyle.Bold
        family == CanvasObservationFamily.AccessibilityFocusable -> CanvasObservationLineStyle.Dashed
        family == CanvasObservationFamily.Ocr -> CanvasObservationLineStyle.Underline
        family == CanvasObservationFamily.Ocv -> CanvasObservationLineStyle.Dashed
        family == CanvasObservationFamily.Yolo -> CanvasObservationLineStyle.Capsule
        family == CanvasObservationFamily.Marker -> CanvasObservationLineStyle.Solid
        else -> CanvasObservationLineStyle.Solid
    }

private fun CanvasObservationItem.matches(filters: CanvasObservationFilters): Boolean =
    when (family) {
        CanvasObservationFamily.AccessibilityVisible -> filters.showAccessibility && filters.showVisible
        CanvasObservationFamily.AccessibilityClickable -> filters.showAccessibility && filters.showClickable
        CanvasObservationFamily.AccessibilityFocusable -> filters.showAccessibility && filters.showFocusable
        CanvasObservationFamily.Ocr -> filters.showOcr
        CanvasObservationFamily.Ocv -> filters.showOcv
        CanvasObservationFamily.Yolo -> filters.showYolo
        CanvasObservationFamily.Dom -> filters.showDom
        CanvasObservationFamily.Marker -> filters.showMarkers
        CanvasObservationFamily.Unknown -> true
    }

private fun List<CanvasObservationItem>.toAmbiguityCandidates(document: WorldviewDocument): List<WorldAmbiguity> {
    val byLabel = filter { it.bounds != null }
        .groupBy { it.label.trim().lowercase() }
        .filterKeys { it.isNotBlank() }
    return byLabel.values
        .filter { group -> group.map { it.family }.toSet().size > 1 }
        .mapIndexed { index, group ->
            WorldAmbiguity(
                id = "ambiguity:canvas:${document.revision}:$index",
                type = AmbiguityType.ProviderConflict,
                subjectRefs = group.map { it.sourceId }.toSet(),
                evidenceObservationIds = group.flatMap { it.evidenceRefs }.filter { it.startsWith("observation:") }.toSet(),
                candidateRefs = group.map { it.sourceId }.toSet(),
                confidence = group.minOf { it.confidence }.coerceIn(0f, 1f),
                impact = "Mehrere Provider melden '${group.first().label}' im Canvas-Kontext.",
                metadata = mapOf(
                    "families" to group.map { it.family.name }.distinct().joinToString(","),
                    "source" to "canvas-observation-projection",
                ),
            )
        }
}

private fun String?.toBooleanLenient(): Boolean =
    equals("true", ignoreCase = true) || equals("1") || equals("yes", ignoreCase = true)
