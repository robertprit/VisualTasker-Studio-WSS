package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldviewPanelProjectionsTest {
    @Test
    fun projectsEntityForInspectorWithEvidenceResourcesAndAmbiguities() {
        val resource = WorkspaceResource(
            id = "marker:login",
            kind = WorkspaceResourceKind.Marker,
            label = "Login",
            metadata = mapOf("source" to "user"),
        )
        val bundle = WorkspaceResourceReducer.upsert(WorkspaceResourceBundle(), resource)
        val base = WorldviewDocument.fromResources(bundle)
        val ambiguity = WorldAmbiguity(
            id = "ambiguity:login",
            type = AmbiguityType.SelectionAmbiguity,
            subjectRefs = setOf("entity:marker:login"),
            evidenceObservationIds = setOf("observation:marker:login:import"),
            confidence = 0.5f,
            impact = "Marker may match multiple login buttons.",
        )
        val worldview = WorldviewReducer.upsertAmbiguity(base, ambiguity)

        val projection = WorldviewInspectorProjector.project(
            worldview,
            WorldviewInspectorSubject(WorldviewInspectorSubjectKind.Entity, "entity:marker:login"),
        )

        assertEquals("Login", projection?.title)
        assertTrue(projection!!.rows.any { it.label == "Type" && it.value == WorldEntityKind.UiElement.name })
        assertEquals(listOf("observation:marker:login:import"), projection.observationIds)
        assertEquals(listOf("marker:login"), projection.resourceIds)
        assertEquals(listOf("ambiguity:login"), projection.ambiguityIds)
    }

    @Test
    fun projectsResourceAndObservationForInspector() {
        val resource = WorkspaceResource(
            id = "template:confirm",
            kind = WorkspaceResourceKind.Template,
            label = "Confirm",
            uri = "workspace://templates/confirm.png",
            mimeType = "image/png",
        )
        val worldview = WorldviewDocument.fromResources(
            WorkspaceResourceReducer.upsert(WorkspaceResourceBundle(), resource),
        )

        val resourceProjection = WorldviewInspectorProjector.project(
            worldview,
            WorldviewInspectorSubject(WorldviewInspectorSubjectKind.Resource, "template:confirm"),
        )
        val observationProjection = WorldviewInspectorProjector.project(
            worldview,
            WorldviewInspectorSubject(WorldviewInspectorSubjectKind.Observation, "observation:template:confirm:import"),
        )

        assertEquals("Confirm", resourceProjection?.title)
        assertTrue(resourceProjection!!.rows.any { it.label == "Uri" && it.value == "workspace://templates/confirm.png" })
        assertEquals(ObservationProvider.Import.name, observationProjection?.rows?.single { it.label == "Provider" }?.value)
        assertEquals(ObservationKind.ResourceImport.name, observationProjection?.title)
    }

    @Test
    fun projectsDataOverviewByResourcesScenesAndAmbiguities() {
        val marker = WorkspaceResource(id = "marker:login", kind = WorkspaceResourceKind.Marker, label = "Login")
        val template = WorkspaceResource(id = "template:login", kind = WorkspaceResourceKind.Template, label = "Login Template")
        val resources = WorkspaceResourceReducer
            .upsert(WorkspaceResourceBundle(), template)
            .let { WorkspaceResourceReducer.upsert(it, marker) }
        val worldview = WorldviewReducer.upsertAmbiguity(
            WorldviewDocument.fromResources(resources),
            WorldAmbiguity(
                id = "ambiguity:resource",
                type = AmbiguityType.IdentityAmbiguity,
                subjectRefs = setOf("entity:marker:login"),
                confidence = 0.25f,
                impact = "Marker and template might refer to different UI elements.",
            ),
        )

        val projection = WorldviewDataProjector.project(worldview)

        assertEquals(listOf("marker:login", "template:login"), projection.resources.map { it.id })
        assertEquals(2, projection.scenes.single().entityCount)
        assertEquals(2, projection.scenes.single().observationCount)
        assertEquals(1, projection.ambiguityCount)
    }

    @Test
    fun projectsVisualUiMemoryAcrossProvidersFacetsAndSuggestions() {
        val marker = WorkspaceResource(id = "marker:play", kind = WorkspaceResourceKind.Marker, label = "Play")
        val template = WorkspaceResource(id = "template:play", kind = WorkspaceResourceKind.Template, label = "Play Template")
        val dataset = WorkspaceResource(id = "dataset:play", kind = WorkspaceResourceKind.Dataset, label = "Play Samples")
        val resources = listOf(marker, template, dataset).fold(WorkspaceResourceBundle()) { bundle, resource ->
            WorkspaceResourceReducer.upsert(bundle, resource)
        }
        val base = WorldviewDocument.fromResources(resources)
        val withVision = WorldviewReducer.upsertObservation(
            base,
            WorldObservation(
                id = "observation:ocr:play",
                provider = ObservationProvider.Ocr,
                kind = ObservationKind.Text,
                sceneId = "scene:resources",
                confidence = 0.91f,
                properties = mapOf("text" to "PLAY"),
            )
        )
        val worldview = WorldviewReducer.upsertAmbiguity(
            withVision,
            WorldAmbiguity(
                id = "ambiguity:play-target",
                type = AmbiguityType.SelectionAmbiguity,
                subjectRefs = setOf("entity:marker:play"),
                confidence = 0.66f,
                impact = "Mehrere Play-Ziele koennten passen.",
            )
        )

        val projection = VisualUiMemoryProjector.project(worldview)

        assertEquals("Visual UI Memory", projection.title)
        assertTrue(projection.providerStates.single { it.provider == VisualUiMemoryProvider.OCR }.active)
        assertEquals(1, projection.providerStates.single { it.provider == VisualUiMemoryProvider.Marker }.itemCount)
        assertEquals(1, projection.providerStates.single { it.provider == VisualUiMemoryProvider.Template }.itemCount)
        assertEquals(1, projection.providerStates.single { it.provider == VisualUiMemoryProvider.ML }.itemCount)
        assertEquals(1, projection.facetCounts.single { it.facet == VisualUiMemoryFacet.Dataset }.count)
        assertTrue(projection.suggestions.any { it.id == "suggestion:marker:play:block" })
        assertTrue(projection.suggestions.any { it.id == "suggestion:template:play:template" })
        assertTrue(projection.suggestions.any { it.id == "suggestion:ambiguity:play-target:resolve" })
        assertEquals(
            "marker:play",
            projection.suggestions.single { it.id == "suggestion:marker:play:block" }.sourceId,
        )
        assertEquals(
            "CREATE_TEMPLATE",
            projection.suggestions.single { it.id == "suggestion:template:play:template" }.type,
        )
        assertEquals(
            setOf("entity:marker:play"),
            projection.suggestions.single { it.id == "suggestion:ambiguity:play-target:resolve" }.evidenceRefs,
        )
    }

    @Test
    fun projectsObservationHistoryAndRecorderSuggestions() {
        val recorderObservation = WorldObservation(
            id = "observation:record:login-click",
            provider = ObservationProvider.Accessibility,
            kind = ObservationKind.Touch,
            observedAtEpochMs = 1200L,
            bounds = WorldviewRect(
                10f,
                20f,
                110f,
                70f,
                coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
            ),
            point = WorldviewPoint(
                60f,
                45f,
                coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
            ),
            properties = mapOf(
                "text" to "Click Login",
                "activity" to "LoginActivity",
                "source" to "railtrace-recording",
            ),
        )
        val document = WorldviewReducer.upsertObservation(WorldviewDocument(), recorderObservation)

        val dataProjection = WorldviewDataProjector.project(document)
        val memoryProjection = VisualUiMemoryProjector.project(document)

        assertEquals(1, dataProjection.observationGroups.size)
        assertEquals("LoginActivity", dataProjection.observationGroups.single().label)
        assertEquals(ObservationProvider.Accessibility, dataProjection.observationGroups.single().provider)
        assertTrue(memoryProjection.suggestions.any { it.type == "CREATE_MARKER_FROM_RECORD" })
        assertTrue(memoryProjection.suggestions.any { it.type == "CREATE_CLICK_FROM_RECORD" })
        assertEquals(
            "observation:record:login-click",
            memoryProjection.suggestions.single { it.type == "CREATE_CLICK_FROM_RECORD" }.sourceId,
        )
    }

    @Test
    fun returnsNullForMissingInspectorSubjectAndRejectsInvalidSubjectId() {
        assertNull(
            WorldviewInspectorProjector.project(
                WorldviewDocument(),
                WorldviewInspectorSubject(WorldviewInspectorSubjectKind.Entity, "entity:missing"),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            WorldviewInspectorSubject(WorldviewInspectorSubjectKind.Entity, "Bad Id")
        }
    }
}
