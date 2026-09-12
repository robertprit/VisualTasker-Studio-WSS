package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WssDragPanelProjectionsTest {
    @Test
    fun resourcesBecomeGroupedDragTree() {
        val bundle = WorkspaceResourceBundle(
            resources = listOf(
                WorkspaceResource(
                    id = "marker:login",
                    kind = WorkspaceResourceKind.Marker,
                    label = "Login Marker",
                    markerMode = WorkspaceMarkerMode.Region,
                ),
                WorkspaceResource(
                    id = "dataset:user",
                    kind = WorkspaceResourceKind.Dataset,
                    label = "User Dataset",
                ),
            ),
        )

        val projection = WssPanelDragProjector.forResources("datastore", bundle)

        assertEquals(PanelType.Datastore, projection.panelType)
        assertEquals(WssDragTargetKind.Datastore, projection.dropTarget.kind)
        assertEquals(listOf("Dataset", "Marker"), projection.tree.items.map { it.payload.label })
        assertEquals(WssDragPayloadKind.DatasetEntry, projection.tree.find("item:dataset:user")!!.payload.kind)
        assertEquals(WssDragPayloadKind.Marker, projection.tree.find("item:marker:login")!!.payload.kind)
    }

    @Test
    fun recorderStepsBecomeRailTraceItems() {
        val projection = WssPanelDragProjector.forRecorderSteps(
            panelId = "rail",
            steps = listOf(
                RecorderStepUi("step-1", "Open", "activity", StepStatus.Recorded),
                RecorderStepUi("step-2", "Tap", "click", StepStatus.Executed),
            ),
        )

        assertEquals(WssDragTargetKind.RailTrace, projection.dropTarget.kind)
        assertEquals(listOf("Open", "Tap"), projection.tree.items.map { it.payload.label })
        assertEquals("click", projection.tree.find("item:step-2")!!.payload.data["actionType"])
    }

    @Test
    fun commandPaletteIsGroupedByCategory() {
        val projection = WssPanelDragProjector.forCommandPalette(
            panelId = "palette",
            commands = listOf(
                WssCommandPaletteItem("wait", "Wait", "Core", "wait(100)"),
                WssCommandPaletteItem("log", "Log", "Debug", "log(\"x\")"),
                WssCommandPaletteItem("beep", "Beep", "Core", "beep(1)"),
            ),
        )

        assertEquals(listOf("Core", "Debug"), projection.tree.items.map { it.payload.label })
        val core = projection.tree.find("group:palette:core")!!
        assertTrue(core.acceptsChildren)
        assertEquals(listOf("Beep", "Wait"), core.children.map { it.payload.label })
        assertEquals(WssDragPayloadKind.Command, core.children.first().payload.kind)
    }

    @Test
    fun junctionPlanExposesEvidenceAndCandidates() {
        val plan = JunctionatorSeed.fromRailTraceStep(
            RecorderStepUi(
                id = "step-1",
                label = "Tap Login",
                actionType = "click",
                status = StepStatus.Executed,
                detail = "Button text Login",
            ),
        )

        val projection = WssPanelDragProjector.forJunctionPlan("junction", plan)

        assertEquals(listOf("Evidence", "Candidates"), projection.tree.items.map { it.payload.label })
        val candidatePayload = projection.tree.find("item:candidate:step-1:direct")!!.payload
        assertEquals(WssDragPayloadKind.Marker, candidatePayload.kind)
        assertTrue(candidatePayload.tags.contains("block"))
        assertTrue(candidatePayload.tags.contains("flownode"))
        assertEquals("Verified", candidatePayload.data["confidence"])
    }
}
