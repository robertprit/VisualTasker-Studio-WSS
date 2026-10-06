package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.compose.debug.BlockEditorDropTrace
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.WorkspaceDocument

object WorkspaceSelectionResolver {
    class SourceMappingCache(
        private val capacity: Int = 4,
    ) {
        private val importedBySource = object : LinkedHashMap<SemanticSourceKey, WorkspaceDocument>(
            capacity,
            0.75f,
            true,
        ) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<SemanticSourceKey, WorkspaceDocument>?,
            ): Boolean = size > capacity
        }

        var hits: Int = 0
            private set
        var misses: Int = 0
            private set

        internal fun importedDocument(
            script: String,
            workspaceId: String,
            traceLabel: String,
        ): WorkspaceDocument? {
            val key = SemanticSourceKey(script)
            importedBySource[key]?.let { cached ->
                hits += 1
                BlockEditorDropTrace.markActive(
                    "${traceLabel}_CACHE_HIT",
                    "hits=$hits misses=$misses chars=${script.length}",
                )
                return cached
            }
            misses += 1
            BlockEditorDropTrace.markActive(
                "${traceLabel}_CACHE_MISS",
                "hits=$hits misses=$misses chars=${script.length}",
            )
            return EmscriptWorkspaceImporter()
                .import(script, workspaceId = workspaceId, traceLabel = traceLabel)
                .document
                ?.also { importedBySource[key] = it }
        }

        fun clear() {
            importedBySource.clear()
        }

        private data class SemanticSourceKey(val exactSource: String)
    }

    fun sourceLines(
        document: WorkspaceDocument,
        projectedScript: String,
        traceLabel: String = "SOURCE",
        cache: SourceMappingCache? = null,
    ): Map<BlockId, Int> {
        BlockEditorDropTrace.markActive("${traceLabel}_DIRECT_SOURCE_LINES_ENTER")
        val direct = document.blocks.mapNotNull { (id, block) ->
            block.metadata[SOURCE_LINE_KEY]?.toIntOrNull()?.let { id to it }
        }.toMap()
        BlockEditorDropTrace.markActive(
            "${traceLabel}_DIRECT_SOURCE_LINES_RETURN",
            "entries=${direct.size}",
        )
        if (projectedScript.isBlank()) return direct

        return direct + derivedSourceLines(
            document = document,
            script = projectedScript,
            traceLabel = "${traceLabel}_DERIVED",
            cache = cache,
        )
    }

    fun derivedSourceLines(
        document: WorkspaceDocument,
        script: String,
        traceLabel: String = "DERIVED",
        cache: SourceMappingCache? = null,
    ): Map<BlockId, Int> {
        if (script.isBlank()) return emptyMap()
        BlockEditorDropTrace.markActive(
            "${traceLabel}_IMPORT_ENTER",
            "chars=${script.length}",
        )
        val projected = runCatching {
            cache?.importedDocument(script, document.id, traceLabel)
                ?: EmscriptWorkspaceImporter()
                    .import(script, workspaceId = document.id, traceLabel = traceLabel)
                    .document
        }.getOrNull()
        BlockEditorDropTrace.markActive(
            "${traceLabel}_IMPORT_RETURN",
            "blocks=${projected?.blocks?.size ?: 0}",
        )
        if (projected == null) return emptyMap()
        BlockEditorDropTrace.markActive("${traceLabel}_IDENTITY_RECONCILE_ENTER")
        val reconciled = WorkspaceIdentityReconciler.reconcile(document, projected)
        BlockEditorDropTrace.markActive(
            "${traceLabel}_IDENTITY_RECONCILE_RETURN",
            "blocks=${reconciled.blocks.size}",
        )

        return buildMap {
            reconciled.blocks.forEach { (id, block) ->
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
                compareByDescending<de.visualtasker.workflow.core.BlockNode> {
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
