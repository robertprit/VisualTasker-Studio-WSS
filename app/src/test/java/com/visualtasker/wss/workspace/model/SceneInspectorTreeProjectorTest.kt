package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneInspectorTreeProjectorTest {
    @Test
    fun projectsSceneInteractionFrameEvidenceAndProviderElements() {
        val step = RecorderStepUi(
            id = "record-login-1",
            label = "Login antippen",
            actionType = "click",
            status = StepStatus.Recorded,
            timestampMs = 120L,
            activityName = "LoginActivity",
            point = WorldviewPoint(320f, 840f, CoordinateSpace(CoordinateSpaceKind.Screen)),
            properties = mapOf(
                "sceneId" to "scene:login",
                "package" to "com.example",
                "window.activity" to "LoginActivity",
                "screenshotPath" to "/data/login.png",
                "recording.evidence" to "window.baseline",
            ),
        )
        val observation = WorldObservation(
            id = "observation:a11y:login-button",
            provider = ObservationProvider.Accessibility,
            kind = ObservationKind.Role,
            sceneId = "scene:login",
            confidence = 0.98f,
            observedAtEpochMs = 1_000L,
            bounds = WorldviewRect(
                200f,
                760f,
                440f,
                900f,
                CoordinateSpace(CoordinateSpaceKind.Screen),
            ),
            properties = mapOf("text" to "Login", "activity" to "LoginActivity", "role" to "Button"),
        )

        val tree = SceneInspectorTreeProjector.project("panel-scene", step, listOf(observation))

        assertEquals("scene:login", tree.sceneId)
        assertEquals(SceneInspectorNodeKind.Scene, tree.root.kind)
        assertNotNull(tree.root.children.firstOrNull { it.kind == SceneInspectorNodeKind.Interaction })
        assertNotNull(tree.root.children.firstOrNull { it.kind == SceneInspectorNodeKind.Frame })
        val elements = tree.root.children.first { it.kind == SceneInspectorNodeKind.Elements }
        assertEquals(1, elements.children.size)
        assertEquals(ObservationProvider.Accessibility, elements.children.single().provider)
        assertEquals(WssDragPayloadKind.InspectorField, elements.children.single().payload.kind)
        assertEquals(PanelType.SceneInspector, elements.children.single().payload.sourcePanelType)
        assertTrue(elements.children.single().children.any { it.label == "bounds" })
    }

    @Test
    fun keepsEmptyElementAndEvidenceBranchesExpandable() {
        val step = RecorderStepUi(
            id = "record-empty-1",
            label = "Unbekannt",
            actionType = "runtime",
            status = StepStatus.Recorded,
        )

        val tree = SceneInspectorTreeProjector.project("panel-scene", step, emptyList())

        assertTrue(tree.root.children.any { it.kind == SceneInspectorNodeKind.Elements })
        assertTrue(tree.root.children.any { it.kind == SceneInspectorNodeKind.Evidence })
        assertEquals(tree.root, tree.find(tree.root.id))
    }
}
