package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualUiMemorySurfaceProjectorTest {
    private val memory = VisualUiMemoryProjection(
        title = "Visual UI Memory",
        worldviewRevision = 42L,
        providerStates = VisualUiMemoryProvider.entries.map { provider ->
            VisualUiMemoryProviderState(provider, itemCount = 1, active = true)
        },
        facetCounts = VisualUiMemoryFacet.entries.map { facet ->
            VisualUiMemoryFacetCount(facet, count = 2)
        },
        suggestions = listOf(
            VisualUiMemorySuggestion(
                id = "suggestion:marker",
                sourceId = "marker:login",
                type = "CREATE_CLICK_ACTION",
                label = "Click",
                detail = "Create click",
                confidence = 0.9f,
                evidenceRefs = setOf("observation:login"),
            ),
            VisualUiMemorySuggestion(
                id = "suggestion:other",
                sourceId = "marker:other",
                type = "CREATE_TEMPLATE",
                label = "Template",
                detail = "Create template",
                confidence = 0.8f,
            ),
        ),
    )

    @Test
    fun visionSurfaceOnlyContainsPerceptionProviders() {
        val projection = VisualUiMemorySurfaceProjector.project(memory, VisualUiMemorySurface.Vision)

        assertEquals(42L, projection.worldviewRevision)
        assertEquals(
            setOf(
                VisualUiMemoryProvider.A11Y,
                VisualUiMemoryProvider.OCR,
                VisualUiMemoryProvider.OCV,
                VisualUiMemoryProvider.YOLO,
                VisualUiMemoryProvider.Template,
            ),
            projection.providerStates.map { it.provider }.toSet(),
        )
        assertFalse(projection.providerStates.any { it.provider == VisualUiMemoryProvider.Runtime })
    }

    @Test
    fun selectedSourceFiltersInspectorAndJunktorSuggestions() {
        val inspector = VisualUiMemorySurfaceProjector.project(
            memory = memory,
            surface = VisualUiMemorySurface.Inspector,
            selectedSourceId = "observation:login",
        )
        val junktor = VisualUiMemorySurfaceProjector.project(
            memory = memory,
            surface = VisualUiMemorySurface.Junktor,
            selectedSourceId = "marker:login",
        )

        assertEquals(listOf("suggestion:marker"), inspector.suggestions.map { it.id })
        assertEquals(listOf("suggestion:marker"), junktor.suggestions.map { it.id })
        assertTrue(junktor.facetCounts.any { it.facet == VisualUiMemoryFacet.Suggestion })
    }

    @Test
    fun datastoreRetainsCompleteProjection() {
        val projection = VisualUiMemorySurfaceProjector.project(memory, VisualUiMemorySurface.Datastore)

        assertEquals(VisualUiMemoryProvider.entries.size, projection.providerStates.size)
        assertEquals(VisualUiMemoryFacet.entries.size, projection.facetCounts.size)
        assertEquals(memory.suggestions, projection.suggestions)
    }
}
