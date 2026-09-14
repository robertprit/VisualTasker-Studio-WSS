package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.registry.BlockTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WssDropEffectApplierTest {
    @Test
    fun estimateTextCursorForDropYMapsPanelYToLineStart() {
        val text = "line1\nline2\nline3"

        val cursor = WssDropEffectApplier.estimateTextCursorForDropY(
            currentText = text,
            dropY = 64f + 22f,
            lineHeight = 22f,
            topInset = 64f,
        )

        assertEquals("line1\n".length, cursor)
    }

    @Test
    fun estimateTextCursorForDropYFallsBackToEndWithoutDropY() {
        val text = "line1\nline2"

        val cursor = WssDropEffectApplier.estimateTextCursorForDropY(text, dropY = null)

        assertEquals(text.length, cursor)
    }

    @Test
    fun insertTextAddsSnippetAtCursorWithLineBoundaries() {
        val effect = WssDropEffect(
            kind = WssDropEffectKind.InsertText,
            label = "Text einfügen",
            targetPanelId = "text",
            payloadId = "command:wait",
            transferMode = WssDragTransferMode.Generate,
            text = "wait(100)",
        )

        val result = WssDropEffectApplier.insertText(
            currentText = "log(\"a\")\nlog(\"b\")",
            effect = effect,
            cursor = "log(\"a\")".length,
        )

        assertEquals("log(\"a\")\nwait(100)\nlog(\"b\")", result.text)
        assertEquals("log(\"a\")\nwait(100)".length, result.cursor)
    }

    @Test
    fun insertTextIgnoresNonTextEffects() {
        val effect = WssDropEffect(
            kind = WssDropEffectKind.CreateBlock,
            label = "Block",
            targetPanelId = "block",
            payloadId = "command:log",
            transferMode = WssDragTransferMode.Generate,
            text = "log(\"x\")",
        )

        val result = WssDropEffectApplier.insertText("wait(1)", effect)

        assertEquals("wait(1)", result.text)
        assertEquals("wait(1)".length, result.cursor)
    }

    @Test
    fun railTraceEffectCreatesPanelAction() {
        val effect = WssDropEffect(
            kind = WssDropEffectKind.SelectRailStep,
            label = "Step",
            targetPanelId = "rail",
            payloadId = "rail:step-1",
            transferMode = WssDragTransferMode.Move,
            stepId = "step-1",
        )

        assertEquals(PanelAction.SelectStep("step-1"), WssDropEffectApplier.panelAction(effect))
    }

    @Test
    fun blockAndFlowEffectsCreateEditorCommands() {
        val block = WssDropEffect(
            kind = WssDropEffectKind.CreateBlock,
            label = "Block erzeugen",
            targetPanelId = "block",
            payloadId = "command:log",
            transferMode = WssDragTransferMode.Generate,
            text = "log(\"x\")",
            commandId = "log",
        )
        val flow = block.copy(kind = WssDropEffectKind.CreateFlowNode, targetPanelId = "flow")

        val blockCommand = WssDropEffectApplier.editorCommand(block)
        val flowCommand = WssDropEffectApplier.editorCommand(flow)

        assertEquals(WssEditorDropCommandKind.CreateBlock, blockCommand!!.kind)
        assertEquals("log(\"x\")", blockCommand.script)
        assertEquals("log", blockCommand.commandId)
        assertEquals(WssEditorDropCommandKind.CreateFlowNode, flowCommand!!.kind)
        assertEquals("log(\"x\")", flowCommand.script)
    }

    @Test
    fun applyToWorkspaceCreatesBlockFromShortCommandId() {
        val document = WorkspaceDocument(id = "drop-short-command")
        val effect = WssDropEffect(
            kind = WssDropEffectKind.CreateFlowNode,
            label = "Wait erzeugen",
            targetPanelId = "flow",
            payloadId = "command:wait",
            transferMode = WssDragTransferMode.Generate,
            text = "wait(100)",
            commandId = "wait",
        )

        val result = WssDropEffectApplier.applyToWorkspaceDocument(document, effect)

        assertTrue(result.applied)
        assertEquals(BlockTypes.ACTION_WAIT, result.definitionId)
        assertTrue(result.document.blocks.values.any { it.type == BlockTypes.ACTION_WAIT })
    }

    @Test
    fun applyToWorkspaceUsesDropPositionMetadata() {
        val document = WorkspaceDocument(id = "drop-position")
        val effect = WssDropEffect(
            kind = WssDropEffectKind.CreateFlowNode,
            label = "Wait erzeugen",
            targetPanelId = "flow",
            payloadId = "command:wait",
            transferMode = WssDragTransferMode.Generate,
            text = "wait(100)",
            commandId = "wait",
            metadata = mapOf("dropX" to "240.0", "dropY" to "180.0"),
        )

        val result = WssDropEffectApplier.applyToWorkspaceDocument(document, effect)
        val insertedId = result.document.rootBlocks.first()
        val position = result.document.rootPositions[insertedId]

        assertTrue(result.applied)
        assertEquals(240f, position?.x)
        assertEquals(180f, position?.y)
    }

    @Test
    fun applyToWorkspacePrefersExplicitBlockTypeMetadata() {
        val document = WorkspaceDocument(id = "drop-explicit-block")
        val effect = WssDropEffect(
            kind = WssDropEffectKind.CreateBlock,
            label = "Log erzeugen",
            targetPanelId = "block",
            payloadId = "command:custom-log",
            transferMode = WssDragTransferMode.Generate,
            text = "log(\"x\")",
            commandId = "missing",
            metadata = mapOf("blockType" to BlockTypes.DEBUG_LOG),
        )

        val result = WssDropEffectApplier.applyToWorkspaceDocument(document, effect)

        assertTrue(result.applied)
        assertEquals(BlockTypes.DEBUG_LOG, result.definitionId)
        assertTrue(result.document.blocks.values.any { it.type == BlockTypes.DEBUG_LOG })
    }

    @Test
    fun applyToWorkspaceLeavesUnknownCommandUnchanged() {
        val document = WorkspaceDocument(id = "drop-unknown-command")
        val effect = WssDropEffect(
            kind = WssDropEffectKind.CreateBlock,
            label = "Unbekannt",
            targetPanelId = "block",
            payloadId = "command:unknown",
            transferMode = WssDragTransferMode.Generate,
            text = "unknown()",
            commandId = "unknown.command",
        )

        val result = WssDropEffectApplier.applyToWorkspaceDocument(document, effect)

        assertFalse(result.applied)
        assertNull(result.definitionId)
        assertEquals(document, result.document)
    }

    @Test
    fun noopEffectCreatesNoActionOrEditorCommand() {
        val effect = WssDropEffect(
            kind = WssDropEffectKind.None,
            label = "Kein Drop",
            targetPanelId = "",
            payloadId = "",
            transferMode = null,
        )

        assertNull(WssDropEffectApplier.panelAction(effect))
        assertNull(WssDropEffectApplier.editorCommand(effect))
    }
}
