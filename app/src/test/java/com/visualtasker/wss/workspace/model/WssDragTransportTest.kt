package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WssDragTransportTest {
    @Test
    fun commandDropsGenerateEditorRepresentations() {
        val command = WssDragPayloadFactory.fromCommand(
            commandId = "wait",
            label = "Wait",
            sourcePanelId = "palette",
            emscript = "wait(100)",
        )

        listOf(
            PanelType.TextEditor,
            PanelType.BlockEditor,
            PanelType.Flowchart,
        ).forEach { panelType ->
            val target = WssDragRules.defaultTargetForPanel("target-${panelType.name.lowercase()}", panelType)

            val decision = WssDragRules.decide(command, target)

            assertTrue("Expected $panelType to accept command payload", decision.accepted)
            assertEquals(WssDragTransferMode.Generate, decision.mode)
        }
    }

    @Test
    fun datastoreLinksBroadPayloadsByDefault() {
        val marker = WssDragPayload(
            id = "marker:login",
            kind = WssDragPayloadKind.Marker,
            label = "Login Button",
            sourcePanelId = "marker-panel",
        )
        val target = WssDragRules.defaultTargetForPanel("datastore", PanelType.Datastore)

        val decision = WssDragRules.decide(marker, target)

        assertTrue(decision.accepted)
        assertEquals(WssDragTransferMode.Link, decision.mode)
    }

    @Test
    fun incompatiblePayloadIsRejected() {
        val asset = WssDragPayload(
            id = "asset:node.fancy",
            kind = WssDragPayloadKind.VisualAsset,
            label = "Fancy Node",
            sourcePanelId = "assets",
        )
        val target = WssDragRules.defaultTargetForPanel("rail", PanelType.RecorderSteps)

        val decision = WssDragRules.decide(asset, target)

        assertFalse(decision.accepted)
        assertNull(decision.mode)
    }

    @Test
    fun targetTagsGateDrops() {
        val target = WssDropTarget(
            id = "drop:dataset:vision",
            kind = WssDragTargetKind.Datastore,
            panelId = "datastore",
            acceptedKinds = setOf(WssDragPayloadKind.DatasetEntry),
            acceptedModes = setOf(WssDragTransferMode.Link),
            requiredTags = setOf("vision"),
        )
        val payload = WssDragPayload(
            id = "dataset:plain",
            kind = WssDragPayloadKind.DatasetEntry,
            label = "Plain fact",
            sourcePanelId = "inspector",
            tags = setOf("manual"),
        )

        val decision = WssDragRules.decide(payload, target)

        assertFalse(decision.accepted)
        assertTrue(decision.reason.contains("vision"))
    }

    @Test
    fun treeReducerMovesItemsIntoNestedLists() {
        val parent = item("group", acceptsChildren = true)
        val first = item("first")
        val second = item("second")
        val tree = WssDragTreeReducer.insert(
            WssDragTree(listOf(parent, first, second)),
            item("third"),
            parentId = "group",
        )

        val moved = WssDragTreeReducer.move(tree, "second", targetParentId = "group", targetIndex = 0)

        assertNull(moved.items.firstOrNull { it.id == "second" })
        val group = moved.find("group")
        assertNotNull(group)
        assertEquals(listOf("second", "third"), group!!.children.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun treeReducerRejectsChildrenForLeafTargets() {
        WssDragTreeReducer.insert(
            tree = WssDragTree(listOf(item("leaf"))),
            item = item("child"),
            parentId = "leaf",
        )
    }

    @Test
    fun dragBusDropsOnHoveredTargetAndClearsSession() {
        val railTarget = WssDragRules.defaultTargetForPanel("rail", PanelType.RecorderSteps)
        val stepPayload = WssDragPayloadFactory.fromRecorderStep(
            step = RecorderStepUi(
                id = "step-1",
                label = "Click Login",
                actionType = "click",
                status = StepStatus.Recorded,
            ),
            sourcePanelId = "rail",
        )
        val state = WssDragBusReducer.hoverTarget(
            WssDragBusReducer.beginDrag(
                WssDragBusReducer.registerTarget(WssDragBusState(), railTarget),
                stepPayload,
                requestedMode = WssDragTransferMode.Move,
            ),
            railTarget.id,
        )

        val (nextState, result) = WssDragBusReducer.drop(state)

        assertNull(nextState.activeSession)
        assertNotNull(result)
        assertTrue(result!!.decision.accepted)
        assertEquals(WssDragTransferMode.Move, result.decision.mode)
        assertEquals(PanelAction.SelectStep("step-1"), result.generatedPanelAction)
    }

    @Test
    fun dragBusRejectsUnknownHoveredTargets() {
        val payload = WssDragPayloadFactory.fromCommand(
            commandId = "log",
            label = "Log",
            sourcePanelId = "palette",
            emscript = "log(\"x\")",
        )

        val state = WssDragBusReducer.hoverTarget(
            WssDragBusReducer.beginDrag(WssDragBusState(), payload),
            "missing",
        )
        val (nextState, result) = WssDragBusReducer.drop(state)

        assertNull(result)
        assertNull(nextState.activeSession)
    }

    private fun item(id: String, acceptsChildren: Boolean = false): WssDragTreeItem =
        WssDragTreeItem(
            id = id,
            payload = WssDragPayload(
                id = "payload:$id",
                kind = WssDragPayloadKind.DatasetEntry,
                label = id.replaceFirstChar { it.uppercase() },
                sourcePanelId = "test",
            ),
            acceptsChildren = acceptsChildren,
        )
}
