package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.domain.BlockId
import de.visualtasker.blockeditor.domain.BlockNode
import de.visualtasker.blockeditor.domain.Connection
import de.visualtasker.blockeditor.domain.ConnectionId
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.domain.allConnections

/** Preserves semantic block identities when an EMScript draft is parsed again. */
object WorkspaceIdentityReconciler {
    fun reconcile(previous: WorkspaceDocument?, imported: WorkspaceDocument): WorkspaceDocument {
        if (previous == null || previous.blocks.isEmpty() || imported.blocks.isEmpty()) return imported

        val previousPaths = semanticPaths(previous)
        val importedPaths = semanticPaths(imported)
        val previousByKey = previousPaths.entries
            .groupBy({ (id, path) -> IdentityKey(path, previous.blocks.getValue(id).type) }, { it.key })
            .mapValues { (_, ids) -> ArrayDeque(ids.sortedBy(BlockId::value)) }
            .toMutableMap()
        val usedPreviousIds = mutableSetOf<BlockId>()
        val blockIds = linkedMapOf<BlockId, BlockId>()

        importedPaths.entries.sortedBy { it.value }.forEach { (importedId, path) ->
            val type = imported.blocks.getValue(importedId).type
            val queue = previousByKey[IdentityKey(path, type)]
            val previousId = queue?.removeFirstOrNull()?.takeIf(usedPreviousIds::add)
            blockIds[importedId] = previousId ?: importedId
        }
        imported.blocks.keys.filterNot(blockIds::containsKey).forEach { blockIds[it] = it }
        if (blockIds.all { (old, new) -> old == new }) return imported

        val connectionIds = imported.blocks.values
            .flatMap(BlockNode::allConnections)
            .associate { connection ->
                val newOwner = blockIds.getValue(connection.owner)
                connection.id to ConnectionId(
                    connection.id.value.replaceFirst(connection.owner.value, newOwner.value),
                )
            }

        fun Connection.remap(): Connection = copy(
            id = connectionIds.getValue(id),
            owner = blockIds.getValue(owner),
            connectedTo = connectedTo?.let(connectionIds::getValue),
        )

        val remappedBlocks = imported.blocks.entries.associateTo(linkedMapOf()) { (oldId, block) ->
            val newId = blockIds.getValue(oldId)
            newId to block.copy(
                id = newId,
                previous = block.previous?.remap(),
                next = block.next?.remap(),
                output = block.output?.remap(),
                valueInputs = block.valueInputs.map { it.copy(connection = it.connection.remap()) },
                statementInputs = block.statementInputs.map { it.copy(connection = it.connection.remap()) },
            )
        }
        return imported.copy(
            blocks = remappedBlocks,
            rootBlocks = imported.rootBlocks.map(blockIds::getValue),
            rootPositions = imported.rootPositions.mapKeys { (id, _) -> blockIds.getValue(id) },
        )
    }

    private fun semanticPaths(document: WorkspaceDocument): Map<BlockId, String> {
        val connectionOwners = document.blocks.values
            .flatMap(BlockNode::allConnections)
            .associate { it.id to it.owner }
        val paths = linkedMapOf<BlockId, String>()

        fun target(connection: Connection?): BlockId? =
            connection?.connectedTo?.let(connectionOwners::get)

        fun visit(blockId: BlockId, path: String) {
            if (blockId in paths || blockId !in document.blocks) return
            paths[blockId] = path
            val block = document.blocks.getValue(blockId)
            block.valueInputs.forEach { input ->
                target(input.connection)?.let { visit(it, "$path/value:${input.name}") }
            }
            block.statementInputs.forEach { input ->
                target(input.connection)?.let { visit(it, "$path/statement:${input.name}") }
            }
            target(block.next)?.let { visit(it, "$path/next") }
        }

        document.rootBlocks.forEachIndexed { index, id -> visit(id, "root:$index") }
        document.blocks.keys
            .filterNot(paths::containsKey)
            .sortedBy(BlockId::value)
            .forEachIndexed { index, id -> visit(id, "orphan:$index") }
        return paths
    }

    private data class IdentityKey(val path: String, val type: String)
}
