package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldviewEntityUnifierTest {
    @Test
    fun observationsFromDifferentProvidersShareStableEntity() {
        val observations = listOf(
            WorldObservation(
                id = "observation:a11y:login",
                provider = ObservationProvider.Accessibility,
                kind = ObservationKind.Bounds,
                sceneId = "scene:login",
                properties = mapOf("viewId" to "login-button", "text" to "Login"),
            ),
            WorldObservation(
                id = "observation:ocr:login",
                provider = ObservationProvider.Ocr,
                kind = ObservationKind.Text,
                sceneId = "scene:login",
                properties = mapOf("viewId" to "login-button", "text" to "Login"),
            ),
        )

        val unified = WorldviewEntityUnifier.unify(
            WorldviewDocument(
                scenes = listOf(WorldScene("scene:login", "Login")),
                observations = observations,
            )
        )

        assertEquals(1, unified.entities.size)
        assertEquals(unified.observations.map { it.entityId }.toSet().single(), unified.entities.single().id)
        assertEquals(setOf("Accessibility", "Ocr"), unified.entities.single().properties.getValue("providers").split(',').toSet())
        assertEquals(2, unified.relations.size)
    }

    @Test
    fun versionedObservationResourceBindsOriginalObservationToResourceEntity() {
        val observation = WorldObservation(
            id = "observation:yolo:button",
            provider = ObservationProvider.Yolo,
            kind = ObservationKind.ObjectDetection,
            properties = mapOf("text" to "Button"),
        )
        val resource = WorkspaceObservationResourceProjector.project(listOf(observation), emptyList()).single()
        val base = WorldviewDocument.fromResources(WorkspaceResourceBundle(resources = listOf(resource)))

        val unified = WorldviewEntityUnifier.unify(
            base.copy(observations = base.observations + observation),
        )

        val original = unified.observations.single { it.id == observation.id }
        assertEquals("entity:${resource.id}", original.entityId)
        assertTrue(observation.id in unified.findEntity("entity:${resource.id}")!!.observationIds)
    }
}
