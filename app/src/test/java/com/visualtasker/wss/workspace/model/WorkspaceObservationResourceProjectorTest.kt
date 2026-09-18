package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceObservationResourceProjectorTest {
    @Test
    fun recorderAndVisionObservationsBecomeVersionedResources() {
        val recorder = WorldObservation(
            id = "observation:record:tap-login",
            provider = ObservationProvider.Accessibility,
            kind = ObservationKind.Touch,
            observedAtEpochMs = 12L,
            properties = mapOf("text" to "Tap Login"),
        )
        val vision = WorldObservation(
            id = "observation:yolo:button",
            provider = ObservationProvider.Yolo,
            kind = ObservationKind.ObjectDetection,
            confidence = 0.92f,
            observedAtEpochMs = 24L,
            properties = mapOf("text" to "Button"),
        )

        val resources = WorkspaceObservationResourceProjector.project(
            visionObservations = listOf(vision),
            recorderObservations = listOf(recorder),
        )

        assertEquals(2, resources.size)
        val recordResource = resources.single { "recorder" in it.tags }
        val visionResource = resources.single { "vision" in it.tags }
        assertEquals(WORKSPACE_RESOURCE_ITEM_VERSION, recordResource.resourceVersion)
        assertEquals("observation:record:tap-login", recordResource.metadata["observationId"])
        assertEquals("visualtasker.vision", visionResource.pluginOwner)
        assertTrue("yolo" in visionResource.tags)
    }
}
