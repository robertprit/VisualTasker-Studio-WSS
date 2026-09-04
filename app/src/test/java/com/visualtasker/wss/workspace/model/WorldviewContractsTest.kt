package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldviewContractsTest {
    @Test
    fun keepsObservationsSeparateFromResolvedEntity() {
        val scene = WorldScene(id = "scene:login", label = "Login Scene")
        val entity = WorldEntity(
            id = "entity:login.button",
            kind = WorldEntityKind.UiElement,
            label = "Login Button",
            sceneId = scene.id,
            observationIds = setOf("observation:ocr-login", "observation:a11y-login"),
        )
        val ocrObservation = WorldObservation(
            id = "observation:ocr-login",
            provider = ObservationProvider.Ocr,
            kind = ObservationKind.Text,
            sceneId = scene.id,
            entityId = entity.id,
            confidence = 0.72f,
            properties = mapOf("text" to "Logln"),
        )
        val a11yObservation = WorldObservation(
            id = "observation:a11y-login",
            provider = ObservationProvider.Accessibility,
            kind = ObservationKind.Text,
            sceneId = scene.id,
            entityId = entity.id,
            confidence = 1f,
            properties = mapOf("text" to "Login"),
        )

        val worldview = WorldviewReducer
            .upsertScene(WorldviewDocument(), scene)
            .let { WorldviewReducer.upsertEntity(it, entity) }
            .let { WorldviewReducer.upsertObservation(it, ocrObservation) }
            .let { WorldviewReducer.upsertObservation(it, a11yObservation) }

        assertEquals("Login Button", worldview.findEntity(entity.id)?.label)
        assertEquals(
            setOf(ocrObservation.id, a11yObservation.id),
            worldview.observationsForEntity(entity.id).map { it.id }.toSet(),
        )
        assertEquals(4, worldview.revision)
    }

    @Test
    fun recordsInterpretedStepWithoutAcceptingWorkflowTruth() {
        val step = WorldStep(
            id = "step:tap-login",
            label = "Tap Login",
            sceneId = "scene:login",
            entityIds = setOf("entity:login.button"),
            interpretation = InterpretationState.IntendedProposal,
            proposedIntent = "click(\"Login\")",
        )
        val record = WorldRecord(
            id = "record:login",
            label = "Login Recording",
            sceneIds = setOf("scene:login"),
            stepIds = setOf(step.id),
        )
        val worldview = WorldviewDocument(steps = listOf(step), records = listOf(record))

        assertEquals(InterpretationState.IntendedProposal, worldview.steps.single().interpretation)
        assertEquals("click(\"Login\")", worldview.steps.single().proposedIntent)
        assertTrue(worldview.records.single().stepIds.contains(step.id))
    }

    @Test
    fun tracksProviderConflictAsAmbiguity() {
        val ambiguity = WorldAmbiguity(
            id = "ambiguity:login-label",
            type = AmbiguityType.ProviderConflict,
            subjectRefs = setOf("entity:login.button"),
            sceneId = "scene:login",
            evidenceObservationIds = setOf("observation:ocr-login", "observation:a11y-login"),
            candidateRefs = setOf("text:logln", "text:login"),
            confidence = 0.64f,
            impact = "OCR and accessibility disagree about the button label.",
        )
        val worldview = WorldviewReducer.upsertAmbiguity(WorldviewDocument(), ambiguity)

        assertEquals(listOf(ambiguity), worldview.ambiguitiesForSubject("entity:login.button"))
        assertEquals(AmbiguityResolutionState.Open, worldview.ambiguities.single().resolutionState)
    }

    @Test
    fun projectsWorkspaceResourcesIntoWorldviewEntitiesAndObservations() {
        val marker = WorkspaceResource(
            id = "marker:login",
            kind = WorkspaceResourceKind.Marker,
            label = "Login",
            markerMode = WorkspaceMarkerMode.Region,
            region = WorkspaceRegionBounds(0.1f, 0.2f, 0.3f, 0.4f),
            pluginOwner = "visualtasker.accessibility.overlay",
        )
        val bundle = WorkspaceResourceReducer.upsert(WorkspaceResourceBundle(), marker)
        val worldview = WorldviewDocument.fromResources(bundle)

        assertEquals(bundle, worldview.resources)
        assertEquals(bundle.revision, worldview.revision)
        assertEquals("Imported Resources", worldview.scenes.single().label)
        assertEquals(WorldEntityKind.UiElement, worldview.findEntity("entity:marker:login")?.kind)
        assertEquals(listOf(marker), worldview.resourcesForEntity("entity:marker:login"))
        assertEquals(WorldviewRect(0.1f, 0.2f, 0.3f, 0.4f), worldview.observations.single().bounds)
    }

    @Test
    fun rejectsInvalidIdsDuplicateIdsConfidenceAndBounds() {
        assertThrows(IllegalArgumentException::class.java) {
            WorldScene(id = "Scene Bad", label = "Bad")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldObservation(
                id = "observation:bad-confidence",
                provider = ObservationProvider.Ocr,
                kind = ObservationKind.Text,
                confidence = 1.2f,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldviewRect(left = 0.6f, top = 0.2f, right = 0.4f, bottom = 0.8f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldviewDocument(
                entities = listOf(
                    WorldEntity(id = "entity:duplicate", kind = WorldEntityKind.UiElement, label = "First"),
                    WorldEntity(id = "entity:duplicate", kind = WorldEntityKind.UiElement, label = "Second"),
                ),
            )
        }
    }

    @Test
    fun workflowStateCarriesWorldviewFromResources() {
        val resource = WorkspaceResource(
            id = "template:confirm",
            kind = WorkspaceResourceKind.Template,
            label = "Confirm Template",
            uri = "workspace://templates/confirm.png",
        )
        val state = WorkspaceWorkflowState.fromDocument(
            document = WorkspaceBootstrap.starter(),
            mutationSource = "worldview-test",
            resources = WorkspaceResourceReducer.upsert(WorkspaceResourceBundle(), resource),
        )

        assertEquals("worldview-test", state.mutationSource)
        assertEquals(WorldEntityKind.VisualObject, state.worldview.findEntity("entity:template:confirm")?.kind)
        assertEquals(listOf(resource), state.worldview.resourcesForEntity("entity:template:confirm"))
    }
}
