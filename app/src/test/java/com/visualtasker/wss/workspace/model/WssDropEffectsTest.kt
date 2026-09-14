package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WssDropEffectsTest {
    @Test
    fun textEditorDropResolvesToInsertTextEffect() {
        val result = acceptedDrop(
            target = WssDragRules.defaultTargetForPanel("text", PanelType.TextEditor),
            payload = WssDragPayloadFactory.fromCommand(
                commandId = "wait",
                label = "Wait",
                sourcePanelId = "palette",
                emscript = "wait(100)",
            ),
        )

        val effect = WssDropEffectResolver.resolve(result)

        assertEquals(WssDropEffectKind.InsertText, effect.kind)
        assertEquals("wait(100)", effect.text)
        assertEquals("wait", effect.commandId)
        assertTrue(effect.isProductive)
    }

    @Test
    fun commandDropResolvesToBlockAndFlowCreateEffects() {
        val payload = WssDragPayloadFactory.fromCommand(
            commandId = "log",
            label = "Log",
            sourcePanelId = "palette",
            emscript = "log(\"ok\")",
        )

        val blockEffect = WssDropEffectResolver.resolve(
            acceptedDrop(WssDragRules.defaultTargetForPanel("block", PanelType.BlockEditor), payload),
        )
        val flowEffect = WssDropEffectResolver.resolve(
            acceptedDrop(WssDragRules.defaultTargetForPanel("flow", PanelType.Flowchart), payload),
        )

        assertEquals(WssDropEffectKind.CreateBlock, blockEffect.kind)
        assertEquals("log(\"ok\")", blockEffect.text)
        assertEquals(WssDropEffectKind.CreateFlowNode, flowEffect.kind)
        assertEquals("log(\"ok\")", flowEffect.text)
    }

    @Test
    fun railTraceStepUsesSafeSnippetForTextAndSelectionForRailTrace() {
        val payload = WssDragPayloadFactory.fromRecorderStep(
            step = RecorderStepUi(
                id = "step-42",
                label = "Click Login",
                actionType = "click",
                status = StepStatus.Recorded,
                detail = "Login",
            ),
            sourcePanelId = "rail",
        )

        val textEffect = WssDropEffectResolver.resolve(
            acceptedDrop(WssDragRules.defaultTargetForPanel("text", PanelType.TextEditor), payload),
        )
        val railEffect = WssDropEffectResolver.resolve(
            acceptedDrop(
                target = WssDragRules.defaultTargetForPanel("rail", PanelType.RecorderSteps),
                payload = payload,
                mode = WssDragTransferMode.Move,
            ),
        )

        assertEquals(WssDropEffectKind.InsertText, textEffect.kind)
        assertEquals("click(\"Login\")", textEffect.text)
        assertEquals("step-42", textEffect.stepId)
        assertEquals(WssDropEffectKind.SelectRailStep, railEffect.kind)
        assertEquals("step-42", railEffect.stepId)
    }

    @Test
    fun railTraceClickStepExportsClickSnippet() {
        val payload = WssDragPayloadFactory.fromRecorderStep(
            step = RecorderStepUi(
                id = "step-click",
                label = "Click Login",
                actionType = "click",
                status = StepStatus.Recorded,
                properties = mapOf("text" to "Login"),
            ),
            sourcePanelId = "rail",
        )

        val effect = WssDropEffectResolver.resolve(
            acceptedDrop(WssDragRules.defaultTargetForPanel("text", PanelType.TextEditor), payload),
        )

        assertEquals("click(\"Login\")", effect.text)
    }

    @Test
    fun railTraceClickStepFallsBackToTouchSnippetWithPoint() {
        val payload = WssDragPayloadFactory.fromRecorderStep(
            step = RecorderStepUi(
                id = "step-touch",
                label = "Click",
                actionType = "click",
                status = StepStatus.Recorded,
                point = WorldviewPoint(
                    x = 120f,
                    y = 240f,
                    coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
                ),
            ),
            sourcePanelId = "rail",
        )

        val effect = WssDropEffectResolver.resolve(
            acceptedDrop(WssDragRules.defaultTargetForPanel("text", PanelType.TextEditor), payload),
        )

        assertEquals("touch([\"down\", 120, 240, \"up\"])", effect.text)
    }

    @Test
    fun railTraceSwipeStepExportsSwipeSnippetWithBounds() {
        val payload = WssDragPayloadFactory.fromRecorderStep(
            step = RecorderStepUi(
                id = "step-swipe",
                label = "Scroll list",
                actionType = "scroll",
                status = StepStatus.Recorded,
                bounds = WorldviewRect(
                    left = 100f,
                    top = 200f,
                    right = 300f,
                    bottom = 800f,
                    coordinateSpace = CoordinateSpace(CoordinateSpaceKind.Screen),
                ),
            ),
            sourcePanelId = "rail",
        )

        val effect = WssDropEffectResolver.resolve(
            acceptedDrop(WssDragRules.defaultTargetForPanel("text", PanelType.TextEditor), payload),
        )

        assertEquals("swipe([200, 800, 200, 200], 1)", effect.text)
    }

    @Test
    fun resourcesResolveToLinkEffectsInDatastoreAndInspector() {
        val payload = WssDragPayload(
            id = "resource:login",
            kind = WssDragPayloadKind.Resource,
            label = "Login Resource",
            sourcePanelId = "marker",
            data = mapOf("resourceId" to "marker-login"),
        )

        val datastoreEffect = WssDropEffectResolver.resolve(
            acceptedDrop(WssDragRules.defaultTargetForPanel("datastore", PanelType.Datastore), payload),
        )
        val inspectorEffect = WssDropEffectResolver.resolve(
            acceptedDrop(WssDragRules.defaultTargetForPanel("debug", PanelType.DebugInfo), payload),
        )

        assertEquals(WssDropEffectKind.LinkResource, datastoreEffect.kind)
        assertEquals("marker-login", datastoreEffect.resourceId)
        assertEquals(WssDropEffectKind.LinkResource, inspectorEffect.kind)
        assertEquals("marker-login", inspectorEffect.resourceId)
    }

    @Test
    fun rejectedOrMissingDropsResolveToNoop() {
        val target = WssDragRules.defaultTargetForPanel("rail", PanelType.RecorderSteps)
        val payload = WssDragPayload(
            id = "asset:fancy",
            kind = WssDragPayloadKind.VisualAsset,
            label = "Fancy Node",
            sourcePanelId = "assets",
        )
        val rejected = WssDropResult(
            target = target,
            payload = payload,
            decision = WssDragRules.decide(payload, target),
        )

        val rejectedEffect = WssDropEffectResolver.resolve(rejected)
        val missingEffect = WssDropEffectResolver.resolve(null)

        assertEquals(WssDropEffectKind.None, rejectedEffect.kind)
        assertFalse(rejectedEffect.isProductive)
        assertEquals(WssDropEffectKind.None, missingEffect.kind)
        assertNull(missingEffect.text)
    }

    private fun acceptedDrop(
        target: WssDropTarget,
        payload: WssDragPayload,
        mode: WssDragTransferMode? = null,
    ): WssDropResult =
        WssDropResult(
            target = target,
            payload = payload,
            decision = WssDragRules.decide(payload, target, requestedMode = mode),
        )
}
