package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasObservationProjectionTest {
    @Test
    fun projectsWorkspaceResourcesIntoProviderStyledCanvasItems() {
        val marker = WorkspaceResource(
            id = "marker:login",
            kind = WorkspaceResourceKind.Marker,
            label = "Login",
            region = WorkspaceRegionBounds(0.10f, 0.20f, 0.40f, 0.30f),
            metadata = mapOf("matchKind" to "OCR"),
        )
        val template = WorkspaceResource(
            id = "template:login",
            kind = WorkspaceResourceKind.Template,
            label = "Login",
            region = WorkspaceRegionBounds(0.11f, 0.21f, 0.39f, 0.31f),
            metadata = mapOf("matchKind" to "OCV"),
        )
        val document = WorldviewDocument.fromResources(
            listOf(marker, template).fold(WorkspaceResourceBundle()) { bundle, resource ->
                WorkspaceResourceReducer.upsert(bundle, resource)
            },
        )

        val projection = CanvasObservationProjector.project(
            document,
            selectedSourceId = "marker:login",
        )

        assertEquals(2, projection.items.size)
        assertEquals(
            CanvasObservationFamily.Ocr,
            projection.items.single { it.sourceId == "marker:login" }.family,
        )
        assertEquals(
            CanvasObservationLineStyle.Underline,
            projection.items.single { it.sourceId == "marker:login" }.lineStyle,
        )
        assertTrue(projection.items.single { it.sourceId == "marker:login" }.selected)
        assertEquals(
            CanvasObservationFamily.Ocv,
            projection.items.single { it.sourceId == "template:login" }.family,
        )
        assertEquals(
            CanvasObservationLineStyle.Dashed,
            projection.items.single { it.sourceId == "template:login" }.lineStyle,
        )
        assertEquals(1, projection.ambiguityCandidates.size)
        assertEquals(
            setOf("marker:login", "template:login"),
            projection.ambiguityCandidates.single().subjectRefs,
        )
    }

    @Test
    fun filtersAccessibilityFamiliesIndependently() {
        val document = WorldviewDocument(
            observations = listOf(
                accessibilityObservation("observation:a11y:visible", "Visible", 0.10f, mapOf("visible" to "true")),
                accessibilityObservation("observation:a11y:clickable", "Click", 0.20f, mapOf("clickable" to "true")),
                accessibilityObservation("observation:a11y:focusable", "Focus", 0.30f, mapOf("focusable" to "true")),
            ),
        )

        val projection = CanvasObservationProjector.project(
            document,
            filters = CanvasObservationFilters(
                showClickable = false,
                showFocusable = false,
            ),
        )

        assertEquals(listOf(CanvasObservationFamily.AccessibilityVisible), projection.items.map { it.family })
        assertEquals("Visible", projection.items.single().label)
    }

    @Test
    fun keepsCanvasOverlaysOutWhenProviderLayerIsDisabled() {
        val document = WorldviewDocument(
            observations = listOf(
                WorldObservation(
                    id = "observation:ocr:title",
                    provider = ObservationProvider.Ocr,
                    kind = ObservationKind.Text,
                    bounds = WorldviewRect(0.10f, 0.10f, 0.30f, 0.20f),
                    properties = mapOf("text" to "Title"),
                ),
                WorldObservation(
                    id = "observation:yolo:button",
                    provider = ObservationProvider.Yolo,
                    kind = ObservationKind.ObjectDetection,
                    bounds = WorldviewRect(0.35f, 0.35f, 0.55f, 0.55f),
                    properties = mapOf("text" to "Button"),
                ),
            ),
        )

        val projection = CanvasObservationProjector.project(
            document,
            filters = CanvasObservationFilters(showOcr = false, showYolo = true),
        )

        assertFalse(projection.items.any { it.family == CanvasObservationFamily.Ocr })
        assertEquals(CanvasObservationFamily.Yolo, projection.items.single().family)
        assertEquals(CanvasObservationLineStyle.Capsule, projection.items.single().lineStyle)
    }

    private fun accessibilityObservation(
        id: String,
        label: String,
        offset: Float,
        properties: Map<String, String>,
    ): WorldObservation =
        WorldObservation(
            id = id,
            provider = ObservationProvider.Accessibility,
            kind = ObservationKind.Bounds,
            bounds = WorldviewRect(offset, offset, offset + 0.10f, offset + 0.10f),
            properties = properties + ("text" to label),
        )
}
