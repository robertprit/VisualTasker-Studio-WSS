package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.domain.WorkspaceDocument

object WorkspaceSelectionResolver {
    fun sourceLines(
        document: WorkspaceDocument,
        projectedScript: String,
    ): Map<BlockId, Int> {
        val direct = document.blocks.mapNotNull { (id, block) ->
            block.metadata[SOURCE_LINE_KEY]?.toIntOrNull()?.let { id to it }
        }.toMap()
        if (projectedScript.isBlank()) return direct

        return direct + derivedSourceLines(document, projectedScript)
    }

    fun derivedSourceLines(
        document: WorkspaceDocument,
        script: String,
    ): Map<BlockId, Int> {
        if (script.isBlank()) return emptyMap()
        val projected = runCatching {
            EmscriptWorkspaceImporter()
                .import(script, workspaceId = document.id)
                .document
                ?.let { WorkspaceIdentityReconciler.reconcile(document, it) }
        }.getOrNull() ?: return emptyMap()

        return buildMap {
            projected.blocks.forEach { (id, block) ->
                block.metadata[SOURCE_LINE_KEY]?.toIntOrNull()?.let { put(id, it) }
            }
        }
    }

    fun sourceLine(
        document: WorkspaceDocument,
        blockId: BlockId?,
        sourceLines: Map<BlockId, Int> = emptyMap(),
    ): Int? = blockId?.let { id ->
        sourceLines[id] ?: document.blocks[id]?.metadata?.get(SOURCE_LINE_KEY)?.toIntOrNull()
    }

    fun blockAtSourceLine(
        document: WorkspaceDocument,
        sourceLine: Int,
        sourceLines: Map<BlockId, Int> = emptyMap(),
    ): BlockId? =
        document.blocks.values
            .asSequence()
            .filter { block ->
                (sourceLines[block.id] ?: block.metadata[SOURCE_LINE_KEY]?.toIntOrNull()) == sourceLine
            }
            .sortedWith(
                compareByDescending<de.visualtasker.blockeditor.domain.BlockNode> {
                    it.previous != null || it.next != null || it.statementInputs.isNotEmpty()
                }.thenBy { it.output != null }
                    .thenBy { it.type }
                    .thenBy { it.id.value },
            )
            .firstOrNull()
            ?.id

    fun reconcile(document: WorkspaceDocument, selection: WorkspaceSelectionState): WorkspaceSelectionState {
        val selectedId = selection.blockId?.takeIf(document.blocks::containsKey)
            ?: selection.sourceLine?.let { blockAtSourceLine(document, it) }
            ?: return selection.clearVisualSelection(selection.source)
        return selection.selectBlock(
            blockId = selectedId,
            sourceLine = sourceLine(document, selectedId),
            source = selection.source,
        )
    }

    private const val SOURCE_LINE_KEY = "emscript.source.line"
}
