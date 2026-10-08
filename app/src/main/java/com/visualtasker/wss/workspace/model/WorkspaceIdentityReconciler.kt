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
import de.visualtasker.workflow.core.SemanticEntityId
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.SemanticEntityRef
import de.visualtasker.workflow.core.SemanticRelationId
import com.visualtasker.wss.emscript.parser.WorkspaceEntitySourceAnchor
import com.visualtasker.wss.emscript.parser.WorkspaceRelationSourceAnchor
import com.visualtasker.wss.emscript.parser.sourceEntityAnchors
import com.visualtasker.wss.emscript.parser.sourceRelationAnchors
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
        val importedMigration = CanonicalWorkspaceMigration.toCurrent(imported)
        if (!importedMigration.isValid) {
            return WorkspaceIdentityReconcileResult(
                imported,
                importedMigration.issues.map { issue ->
                    WorkspaceIdentityDiagnostic(issue.code, issue.message)
                },
            )
        }
        val importedDocument = importedMigration.document
        val previousDocument = previous?.let(CanonicalWorkspaceMigration::toCurrent)?.document
        val previousCanonical = previousDocument?.canonical
        val importedAnchors = importedDocument.sourceEntityAnchors()
        val importedRelationAnchors = importedDocument.sourceRelationAnchors()
        val strictAnchorValidation = previousDocument?.blocks?.values
            ?.any { it.type != BlockTypes.EVENT_START }
            ?: false
        val diagnostics = mutableListOf<WorkspaceIdentityDiagnostic>()
        validateSourceAnchors(
            anchors = importedAnchors,
            relationAnchors = importedRelationAnchors,
            previousDocument = previousDocument,
            strict = strictAnchorValidation,
            diagnostics = diagnostics,
        )
        if (diagnostics.isNotEmpty()) {
            return WorkspaceIdentityReconcileResult(importedDocument, diagnostics.distinctDiagnostics())
        }

        val previousPaths = previousDocument?.let(::semanticPaths).orEmpty()
        val importedPaths = semanticPaths(importedDocument)
        val usedPreviousIds = mutableSetOf<BlockId>()
        val blockIds = linkedMapOf<BlockId, BlockId>()
        val previousEntitiesById = previousCanonical?.entities?.associateBy { it.ref.id }.orEmpty()

        importedAnchors
            .filter { it.entityKind in setOf(SemanticEntityKind.Statement, SemanticEntityKind.Expression) }
            .forEach { anchor ->
                val previousEntity = previousEntitiesById[anchor.entityId] ?: return@forEach
                val previousBlockId = previousEntity.legacySourceId?.let(::BlockId) ?: return@forEach
                if (previousBlockId !in usedPreviousIds) {
                    blockIds[anchor.ownerBlockId] = previousBlockId
                    usedPreviousIds += previousBlockId
                }
            }

        val previousBySignature = previousDocument?.blocks?.keys
            .orEmpty()
            .filterNot(usedPreviousIds::contains)
            .groupBy { previousDocument!!.blocks.getValue(it).identitySignature() }
        val importedBySignature = importedDocument.blocks.keys
            .filterNot(blockIds::containsKey)
            .groupBy { importedDocument.blocks.getValue(it).identitySignature() }
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
            .groupBy({ (id, path) -> IdentityKey(path, previousDocument!!.blocks.getValue(id).identityType()) }, { it.key })
        importedPaths.entries.sortedBy { it.value }.forEach { (importedId, path) ->
            if (importedId in blockIds) return@forEach
            val type = importedDocument.blocks.getValue(importedId).identityType()
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
            val type = importedDocument.blocks.getValue(importedId).identityType()
            val candidates = previousDocument?.blocks?.keys.orEmpty().filter { previousId ->
                previousId !in usedPreviousIds && previousDocument!!.blocks.getValue(previousId).identityType() == type
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
        importedDocument.blocks.keys.filterNot(blockIds::containsKey).forEach { blockIds[it] = it }

        val connectionIds = importedDocument.blocks.values
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

        val remappedBlocks = importedDocument.blocks.entries.associateTo(linkedMapOf()) { (oldId, block) ->
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
        val remapped = importedDocument.copy(
            blocks = remappedBlocks,
            rootBlocks = importedDocument.rootBlocks.map(blockIds::getValue),
            rootPositions = importedDocument.rootPositions.mapKeys { (id, _) -> blockIds.getValue(id) },
            canonical = null,
        )
        val migrated = CanonicalWorkspaceMigration.toCurrent(remapped).document
        val importedCanonical = migrated.canonical
            ?: return WorkspaceIdentityReconcileResult(migrated, diagnostics)
        val remappedAnchors = migrated.sourceEntityAnchors()
        val entityIdRemap = linkedMapOf<SemanticEntityId, SemanticEntityId>()
        remappedAnchors.forEach { anchor ->
            val generated = importedCanonical.entities.singleOrNull { entity ->
                entity.ref.kind == anchor.entityKind && entity.legacySourceId == anchor.legacySourceId
            }
            if (generated == null) {
                diagnostics += WorkspaceIdentityDiagnostic(
                    code = "SOURCE_ANCHOR_TARGET_MISSING",
                    message = "Source-Anker ${anchor.entityId.value} findet kein ${anchor.entityKind}-Ziel '${anchor.legacySourceId}'.",
                    importedBlockId = anchor.ownerBlockId,
                )
            } else {
                entityIdRemap[generated.ref.id] = anchor.entityId
            }
        }
        val anchoredIds = entityIdRemap.values
        if (anchoredIds.size != entityIdRemap.size ||
            importedCanonical.entities.any { entity ->
                entity.ref.id in anchoredIds && entity.ref.id !in entityIdRemap.keys
            }
        ) {
            diagnostics += WorkspaceIdentityDiagnostic(
                code = "SOURCE_ANCHOR_DUPLICATE",
                message = "Source-Anker kollidieren mit einer vorhandenen semantischen Entity-ID.",
            )
        }
        if (diagnostics.isNotEmpty()) {
            return WorkspaceIdentityReconcileResult(importedDocument, diagnostics.distinctDiagnostics())
        }

        val anchoredEntities = importedCanonical.entities.map { entity ->
            val desiredId = entityIdRemap[entity.ref.id] ?: entity.ref.id
            previousEntitiesById[desiredId]
                ?.takeIf { it.ref.kind == entity.ref.kind }
                ?: entity.copy(ref = entity.ref.copy(id = desiredId))
        }
        val entityKindById = anchoredEntities.associate { it.ref.id to it.ref.kind }
        val entityAnchoredRelations = importedCanonical.relations.map { relation ->
            val sourceId = entityIdRemap[relation.source.id] ?: relation.source.id
            val targetId = entityIdRemap[relation.target.id] ?: relation.target.id
            relation.copy(
                source = SemanticEntityRef(sourceId, entityKindById.getValue(sourceId)),
                target = SemanticEntityRef(targetId, entityKindById.getValue(targetId)),
            )
        }
        val remappedRelationAnchors = migrated.sourceRelationAnchors()
        val relationAnchorsByIdentity = linkedMapOf<RelationIdentity, SemanticRelationId>()
        val previousRelationsById = previousCanonical?.relations?.associateBy(SemanticRelation::id).orEmpty()
        remappedRelationAnchors.forEach { anchor ->
            val identity = relationIdentity(anchor)
            val matches = entityAnchoredRelations.filter { relationIdentity(it) == identity }
            if (matches.size > 1 || (matches.isEmpty() && anchor.relationId !in previousRelationsById)) {
                diagnostics += WorkspaceIdentityDiagnostic(
                    code = if (matches.isEmpty()) "SOURCE_ANCHOR_TARGET_MISSING" else "SOURCE_ANCHOR_AMBIGUOUS",
                    message = "Relation-Anker ${anchor.relationId.value} passt auf ${matches.size} strukturelle Relationen.",
                    importedBlockId = anchor.ownerBlockId,
                )
            } else if (matches.size == 1) {
                relationAnchorsByIdentity[identity] = anchor.relationId
            }
        }
        val previousRelations = previousCanonical?.relations?.associateBy(::relationIdentity).orEmpty()
        val reconciledCanonical = importedCanonical.copy(
            entities = anchoredEntities.sortedBy { it.ref.id.value },
            relations = entityAnchoredRelations
                .map { relation ->
                    val identity = relationIdentity(relation)
                    val desiredId = relationAnchorsByIdentity[identity]
                        ?: previousRelations[identity]?.id
                        ?: relation.id
                    relation.copy(id = desiredId)
                }
                .sortedBy { it.id.value },
        )
        if (reconciledCanonical.relations.map { it.id }.distinct().size != reconciledCanonical.relations.size) {
            diagnostics += WorkspaceIdentityDiagnostic(
                code = "SOURCE_ANCHOR_DUPLICATE",
                message = "Source-Anker erzeugen kollidierende semantische Relation-IDs.",
            )
        }
        if (diagnostics.isNotEmpty()) {
            return WorkspaceIdentityReconcileResult(importedDocument, diagnostics.distinctDiagnostics())
        }
        val reconciled = migrated.copy(canonical = reconciledCanonical)
        return WorkspaceIdentityReconcileResult(
            document = CanonicalCompatibilityProjection.project(reconciled).document,
            diagnostics = diagnostics.distinctDiagnostics(),
        )
    }

    private fun validateSourceAnchors(
        anchors: List<WorkspaceEntitySourceAnchor>,
        relationAnchors: List<WorkspaceRelationSourceAnchor>,
        previousDocument: WorkspaceDocument?,
        strict: Boolean,
        diagnostics: MutableList<WorkspaceIdentityDiagnostic>,
    ) {
        anchors.groupBy(WorkspaceEntitySourceAnchor::entityId)
            .filterValues { it.size > 1 }
            .forEach { (id, duplicates) ->
                diagnostics += WorkspaceIdentityDiagnostic(
                    code = "SOURCE_ANCHOR_DUPLICATE",
                    message = "Entity-Anker ${id.value} wird ${duplicates.size} mal verwendet.",
                    importedBlockId = duplicates.first().ownerBlockId,
                )
            }
        relationAnchors.groupBy(WorkspaceRelationSourceAnchor::relationId)
            .filterValues { it.size > 1 }
            .forEach { (id, duplicates) ->
                diagnostics += WorkspaceIdentityDiagnostic(
                    code = "SOURCE_ANCHOR_DUPLICATE",
                    message = "Relation-Anker ${id.value} wird ${duplicates.size} mal verwendet.",
                    importedBlockId = duplicates.first().ownerBlockId,
                )
            }
        val previousCanonical = previousDocument?.canonical ?: return
        val previousEntities = previousCanonical.entities.associateBy { it.ref.id }
        val previousRelations = previousCanonical.relations.associateBy(SemanticRelation::id)
        anchors.forEach { anchor ->
            val previousEntity = previousEntities[anchor.entityId]
            when {
                previousEntity == null && strict -> diagnostics += WorkspaceIdentityDiagnostic(
                    code = "SOURCE_ANCHOR_TARGET_MISSING",
                    message = "Entity-Anker ${anchor.entityId.value} verweist auf keine vorhandene Entity.",
                    importedBlockId = anchor.ownerBlockId,
                )
                previousEntity != null && previousEntity.ref.kind != anchor.entityKind ->
                    diagnostics += WorkspaceIdentityDiagnostic(
                        code = "SOURCE_ANCHOR_KIND_MISMATCH",
                        message = "Entity-Anker ${anchor.entityId.value} erwartet ${anchor.entityKind}, " +
                            "vorhanden ist ${previousEntity.ref.kind}.",
                        importedBlockId = anchor.ownerBlockId,
                    )
            }
        }
        relationAnchors.forEach { anchor ->
            val previousRelation = previousRelations[anchor.relationId]
            when {
                previousRelation == null && strict -> diagnostics += WorkspaceIdentityDiagnostic(
                    code = "SOURCE_ANCHOR_TARGET_MISSING",
                    message = "Relation-Anker ${anchor.relationId.value} verweist auf keine vorhandene Relation.",
                    importedBlockId = anchor.ownerBlockId,
                )
                previousRelation != null && relationIdentity(previousRelation) != relationIdentity(anchor) ->
                    diagnostics += WorkspaceIdentityDiagnostic(
                        code = "SOURCE_ANCHOR_CONFLICT",
                        message = "Relation-Anker ${anchor.relationId.value} widerspricht seiner vorhandenen Struktur.",
                        importedBlockId = anchor.ownerBlockId,
                    )
            }
        }
    }

    private fun List<WorkspaceIdentityDiagnostic>.distinctDiagnostics(): List<WorkspaceIdentityDiagnostic> =
        distinctBy { diagnostic ->
            listOf(
                diagnostic.code,
                diagnostic.importedBlockId?.value.orEmpty(),
                diagnostic.candidateBlockIds.joinToString { it.value },
            ).joinToString("|")
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

    private fun relationIdentity(anchor: WorkspaceRelationSourceAnchor): RelationIdentity = RelationIdentity(
        kind = anchor.relationKind.name,
        source = anchor.sourceEntityId.value,
        target = anchor.targetEntityId.value,
        role = anchor.roleKind.name,
        roleName = anchor.roleName,
        branchRole = anchor.branchRole?.name,
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
