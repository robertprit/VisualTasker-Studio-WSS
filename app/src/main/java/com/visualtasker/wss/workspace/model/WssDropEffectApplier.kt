package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog

data class WssTextInsertionResult(
    val text: String,
    val cursor: Int,
)

data class WssWorkspaceDropResult(
    val document: WorkspaceDocument,
    val applied: Boolean,
    val command: WssEditorDropCommand?,
    val definitionId: String?,
)

object WssDropEffectApplier {
    fun estimateTextCursorForDropY(
        currentText: String,
        dropY: Float?,
        lineHeight: Float = 22f,
        topInset: Float = 64f,
    ): Int {
        if (dropY == null || dropY.isNaN() || dropY.isInfinite() || lineHeight <= 0f) {
            return currentText.length
        }
        val targetLine = ((dropY - topInset).coerceAtLeast(0f) / lineHeight).toInt()
        if (targetLine <= 0) return 0
        var line = 0
        currentText.forEachIndexed { index, char ->
            if (line >= targetLine) return index
            if (char == '\n') line += 1
        }
        return currentText.length
    }

    fun insertText(
        currentText: String,
        effect: WssDropEffect,
        cursor: Int = currentText.length,
    ): WssTextInsertionResult {
        require(cursor in 0..currentText.length) {
            "Text insert cursor must be inside the current text."
        }
        if (effect.kind != WssDropEffectKind.InsertText || effect.text.isNullOrBlank()) {
            return WssTextInsertionResult(currentText, cursor)
        }
        val snippet = effect.text.trim()
        val prefix = currentText.take(cursor)
        val suffix = currentText.drop(cursor)
        val insertion = buildString {
            if (prefix.isNotEmpty() && !prefix.endsWith('\n')) append('\n')
            append(snippet)
            if (suffix.isNotEmpty() && !suffix.startsWith('\n')) append('\n')
        }
        val nextText = prefix + insertion + suffix
        return WssTextInsertionResult(
            text = nextText,
            cursor = (prefix.length + insertion.length).coerceIn(0, nextText.length),
        )
    }

    fun panelAction(effect: WssDropEffect): PanelAction? =
        when (effect.kind) {
            WssDropEffectKind.SelectRailStep -> effect.stepId?.let(PanelAction::SelectStep)
            else -> null
        }

    fun editorCommand(effect: WssDropEffect): WssEditorDropCommand? =
        when (effect.kind) {
            WssDropEffectKind.CreateBlock -> WssEditorDropCommand(
                targetPanelId = effect.targetPanelId,
                kind = WssEditorDropCommandKind.CreateBlock,
                label = effect.label,
                script = effect.text,
                commandId = effect.commandId,
                resourceId = effect.resourceId,
                stepId = effect.stepId,
                metadata = effect.metadata,
            )
            WssDropEffectKind.CreateFlowNode -> WssEditorDropCommand(
                targetPanelId = effect.targetPanelId,
                kind = WssEditorDropCommandKind.CreateFlowNode,
                label = effect.label,
                script = effect.text,
                commandId = effect.commandId,
                resourceId = effect.resourceId,
                stepId = effect.stepId,
                metadata = effect.metadata,
            )
            WssDropEffectKind.LinkResource -> WssEditorDropCommand(
                targetPanelId = effect.targetPanelId,
                kind = WssEditorDropCommandKind.LinkResource,
                label = effect.label,
                script = null,
                commandId = effect.commandId,
                resourceId = effect.resourceId,
                stepId = effect.stepId,
                metadata = effect.metadata,
            )
            else -> null
        }

    fun applyToWorkspaceDocument(
        document: WorkspaceDocument,
        effect: WssDropEffect,
    ): WssWorkspaceDropResult {
        val command = editorCommand(effect)
        val definitionId = command?.definitionId()
        val updated = when (command?.kind) {
            WssEditorDropCommandKind.CreateBlock,
            WssEditorDropCommandKind.CreateFlowNode,
            -> definitionId?.let {
                addFlowchartNodeToWorkspace(
                    document = document,
                    definitionId = it,
                    position = effect.dropPosition(),
                )
            } ?: document
            WssEditorDropCommandKind.LinkResource,
            null,
            -> document
        }
        return WssWorkspaceDropResult(
            document = updated,
            applied = updated != document,
            command = command,
            definitionId = definitionId,
        )
    }
}

private fun WssDropEffect.dropPosition(): FlowPoint? {
    val x = metadata["dropX"]?.toDoubleOrNull() ?: return null
    val y = metadata["dropY"]?.toDoubleOrNull() ?: return null
    return FlowPoint(x, y)
}

private fun WssEditorDropCommand.definitionId(): String? =
    sequenceOf(
        metadata["blockType"],
        metadata["definitionId"],
        commandId,
        commandId?.let { VisualTaskerCommandCatalog.findById(it)?.block?.blockType },
        commandId?.let { VisualTaskerCommandCatalog.findByCanonicalName(it)?.block?.blockType },
        commandId?.let { VisualTaskerCommandCatalog.findByAcceptedName(it)?.block?.blockType },
        commandId?.let { "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}$it" },
    )
        .filterNotNull()
        .firstOrNull { DefaultBlockRegistry.getDefinition(it) != null }

enum class WssEditorDropCommandKind {
    CreateBlock,
    CreateFlowNode,
    LinkResource,
}

data class WssEditorDropCommand(
    val targetPanelId: String,
    val kind: WssEditorDropCommandKind,
    val label: String,
    val script: String?,
    val commandId: String?,
    val resourceId: String?,
    val stepId: String?,
    val metadata: Map<String, String> = emptyMap(),
)
