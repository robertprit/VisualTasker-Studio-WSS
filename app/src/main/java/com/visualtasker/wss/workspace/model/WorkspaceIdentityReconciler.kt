package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.BlockNode
import de.visualtasker.workflow.core.Connection
import de.visualtasker.workflow.core.ConnectionId
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.core.CanonicalCompatibilityProjection
import de.visualtasker.workflow.core.CanonicalWorkspaceMigration
import de.visualtasker.workflow.core.SemanticRelation
import de.visualtasker.workflow.core.allConnections

data class WorkspaceIdentityDiagnostic(
    val code: String,
    val message: String,
    val importedBlockId: BlockId? = null,
    val candidateBlockIds: List<BlockId> = emptyList(),
)

data class WorkspaceIdentityReconcileResult(
    val document: WorkspaceDocument,
    val diagnostics: List<WorkspaceIdentityDiagnostic> = emptyList(),
) {
    val isUnambiguous: Boolean get() = diagnostics.isEmpty()
}

/** Preserves semantic block identities when an EMScript draft is parsed again. */
object WorkspaceIdentityReconciler {
    fun reconcile(previous: WorkspaceDocument?, imported: WorkspaceDocument): WorkspaceDocument =
        reconcileWithDiagnostics(previous, imported).document

    fun reconcileWithDiagnostics(
        previous: WorkspaceDocument?,
        imported: WorkspaceDocument,
    ): WorkspaceIdentityReconcileResult {
        if (previous == null || previous.blocks.isEmpty() || imported.blocks.isEmpty()) {
            return WorkspaceIdentityReconcileResult(imported)
        }

        val previousPaths = semanticPaths(previous)
        val importedPaths = semanticPaths(imported)
        val usedPreviousIds = mutableSetOf<BlockId>()
        val blockIds = linkedMapOf<BlockId, BlockId>()
        val diagnostics = mutableListOf<WorkspaceIdentityDiagnostic>()

        val previousBySignature = previous.blocks.keys.groupBy { previous.blocks.getValue(it).identitySignature() }
        val importedBySignature = imported.blocks.keys.groupBy { imported.blocks.getValue(it).identitySignature() }
        importedBySignature.forEach { (signature, importedIds) ->
            val previousIds = previousBySignature[signature].orEmpty()
            if (importedIds.size > 1 && previousIds.isNotEmpty() && importedIds.size != previousIds.size) {
                diagnostics += WorkspaceIdentityDiagnostic(
                    code = "IDENTITY_RECONCILIATION_AMBIGUOUS",
                    message = "Repeated source elements with signature '$signature' changed cardinality; identity cannot be proven losslessly.",
                    importedBlockId = importedIds.first(),
                    candidateBlockIds = previousIds.sortedBy(BlockId::value),
                )
            }
            if (importedIds.size == 1 && previousIds.size == 1) {
                val importedId = importedIds.single()
                val previousId = previousIds.single()
                blockIds[importedId] = previousId
                usedPreviousIds += previousId
            }
        }

        val previousByPath = previousPaths.entries
            .groupBy({ (id, path) -> IdentityKey(path, previous.blocks.getValue(id).identityType()) }, { it.key })
        importedPaths.entries.sortedBy { it.value }.forEach { (importedId, path) ->
            if (importedId in blockIds) return@forEach
            val type = imported.blocks.getValue(importedId).identityType()
            val candidates = previousByPath[IdentityKey(path, type)].orEmpty().filterNot(usedPreviousIds::contains)
            if (candidates.size == 1) {
                blockIds[importedId] = candidates.single()
                usedPreviousIds += candidates.single()
            } else if (candidates.size > 1) {
                diagnostics += WorkspaceIdentityDiagnostic(
                    code = "IDENTITY_RECONCILIATION_AMBIGUOUS",
                    message = "Source path '$path' matches more than one previous semantic element.",
                    importedBlockId = importedId,
                    candidateBlockIds = candidates.sortedBy(BlockId::value),
                )
            }
        }

        importedPaths.entries.sortedBy { it.value }.forEach { (importedId, path) ->
            if (importedId in blockIds) return@forEach
            val type = imported.blocks.getValue(importedId).identityType()
            val candidates = previous.blocks.keys.filter { previousId ->
                previousId !in usedPreviousIds && previous.blocks.getValue(previousId).identityType() == type
            }
            when (candidates.size) {
                0 -> blockIds[importedId] = importedId
                1 -> {
                    blockIds[importedId] = candidates.single()
                    usedPreviousIds += candidates.single()
                }
                else -> {
                    diagnostics += WorkspaceIdentityDiagnostic(
                        code = "IDENTITY_RECONCILIATION_AMBIGUOUS",
                        message = "Source element at '$path' has ${candidates.size} equally valid previous identity candidates.",
                        importedBlockId = importedId,
                        candidateBlockIds = candidates.sortedBy(BlockId::value),
                    )
                    blockIds[importedId] = importedId
                }
            }
        }
        imported.blocks.keys.filterNot(blockIds::containsKey).forEach { blockIds[it] = it }

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
        val remapped = imported.copy(
            blocks = remappedBlocks,
            rootBlocks = imported.rootBlocks.map(blockIds::getValue),
            rootPositions = imported.rootPositions.mapKeys { (id, _) -> blockIds.getValue(id) },
            canonical = null,
        )
        val migrated = CanonicalWorkspaceMigration.toCurrent(remapped).document
        val previousCanonical = CanonicalWorkspaceMigration.toCurrent(previous).document.canonical
            ?: return WorkspaceIdentityReconcileResult(migrated, diagnostics)
        val importedCanonical = migrated.canonical
            ?: return WorkspaceIdentityReconcileResult(migrated, diagnostics)
        val previousEntities = previousCanonical.entities.associateBy { it.ref.id }
        val previousRelations = previousCanonical.relations.associateBy(::relationIdentity)
        val reconciledCanonical = importedCanonical.copy(
            entities = importedCanonical.entities
                .map { entity ->
                    previousEntities[entity.ref.id]
                        ?.takeIf { it.ref.kind == entity.ref.kind }
                        ?: entity
                }
                .sortedBy { it.ref.id.value },
            relations = importedCanonical.relations
                .map { relation ->
                    previousRelations[relationIdentity(relation)]
                        ?.let { previousRelation -> relation.copy(id = previousRelation.id) }
                        ?: relation
                }
                .sortedBy { it.id.value },
        )
        val reconciled = migrated.copy(canonical = reconciledCanonical)
        return WorkspaceIdentityReconcileResult(
            document = CanonicalCompatibilityProjection.project(reconciled).document,
            diagnostics = diagnostics.distinctBy { diagnostic ->
                listOf(
                    diagnostic.code,
                    diagnostic.importedBlockId?.value.orEmpty(),
                    diagnostic.candidateBlockIds.joinToString { it.value },
                ).joinToString("|")
            },
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

    private fun BlockNode.identityType(): String = when (type) {
        BlockTypes.CONTROL_IF,
        BlockTypes.CONTROL_IF_ELSE,
        BlockTypes.CONTROL_IF_ELSEIF_ELSE,
        -> "control_if_family"
        else -> type
    }

    private fun BlockNode.identitySignature(): String = buildString {
        append(identityType())
        fields.entries
            .filterNot { (key, _) ->
                key == "displayLabel" || key == "note" || key.endsWith(".source")
            }
            .sortedBy { it.key }
            .forEach { (key, value) ->
                append('|')
                append(key)
                append('=')
                append(value)
            }
    }

    private fun relationIdentity(relation: SemanticRelation): RelationIdentity = RelationIdentity(
        kind = relation.kind.name,
        source = relation.source.id.value,
        target = relation.target.id.value,
        role = relation.role.kind.name,
        roleName = relation.role.name,
        branchRole = relation.role.branchRole?.name,
    )

    private data class RelationIdentity(
        val kind: String,
        val source: String,
        val target: String,
        val role: String,
        val roleName: String?,
        val branchRole: String?,
    )
}
