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
