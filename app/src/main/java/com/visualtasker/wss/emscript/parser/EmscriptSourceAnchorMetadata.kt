package com.visualtasker.wss.emscript.parser

import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.SemanticBranchRole
import de.visualtasker.workflow.core.SemanticEntityId
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.SemanticRelationId
import de.visualtasker.workflow.core.SemanticRelationKind
import de.visualtasker.workflow.core.SemanticRelationRoleKind
import de.visualtasker.workflow.core.WorkspaceDocument

internal data class WorkspaceEntitySourceAnchor(
    val entityId: SemanticEntityId,
    val entityKind: SemanticEntityKind,
    val legacySourceId: String,
    val ownerBlockId: BlockId,
)

internal data class WorkspaceRelationSourceAnchor(
    val relationId: SemanticRelationId,
    val relationKind: SemanticRelationKind,
    val sourceEntityId: SemanticEntityId,
    val targetEntityId: SemanticEntityId,
    val roleKind: SemanticRelationRoleKind,
    val roleName: String?,
    val branchRole: SemanticBranchRole?,
    val order: Int?,
    val ownerBlockId: BlockId,
)

internal fun WorkspaceDocument.sourceEntityAnchors(): List<WorkspaceEntitySourceAnchor> = buildList {
    blocks.forEach { (blockId, block) ->
        val entityId = block.metadata[EMSCRIPT_SOURCE_ANCHOR_ENTITY_METADATA]
        val entityKind = block.metadata[EMSCRIPT_SOURCE_ANCHOR_ENTITY_KIND_METADATA]
        if (entityId != null && entityKind != null) {
            add(
                WorkspaceEntitySourceAnchor(
                    entityId = SemanticEntityId(entityId),
                    entityKind = SemanticEntityKind.valueOf(entityKind),
                    legacySourceId = blockId.value,
                    ownerBlockId = blockId,
                ),
            )
        }
        block.metadata
            .filterKeys { it.startsWith(EMSCRIPT_SOURCE_ANCHOR_BRANCH_PREFIX) }
            .forEach { (key, anchoredId) ->
                val slotName = key.removePrefix(EMSCRIPT_SOURCE_ANCHOR_BRANCH_PREFIX)
                add(
                    WorkspaceEntitySourceAnchor(
                        entityId = SemanticEntityId(anchoredId),
                        entityKind = SemanticEntityKind.Branch,
                        legacySourceId = "${blockId.value}:$slotName",
                        ownerBlockId = blockId,
                    ),
                )
            }
    }
}.sortedWith(compareBy({ it.entityId.value }, { it.legacySourceId }))

internal fun WorkspaceDocument.sourceRelationAnchors(): List<WorkspaceRelationSourceAnchor> = buildList {
    blocks.forEach { (blockId, block) ->
        val count = block.metadata[EMSCRIPT_SOURCE_ANCHOR_RELATION_COUNT]?.toIntOrNull() ?: return@forEach
        repeat(count) { index ->
            val prefix = "$EMSCRIPT_SOURCE_ANCHOR_RELATION_PREFIX$index."
            val relationId = block.metadata["${prefix}id"] ?: error("SOURCE_ANCHOR_INVALID: Relation $index ohne ID.")
            val kind = block.metadata["${prefix}kind"] ?: error("SOURCE_ANCHOR_INVALID: Relation $index ohne Typ.")
            val source = block.metadata["${prefix}source"] ?: error("SOURCE_ANCHOR_INVALID: Relation $index ohne Quelle.")
            val target = block.metadata["${prefix}target"] ?: error("SOURCE_ANCHOR_INVALID: Relation $index ohne Ziel.")
            val role = block.metadata["${prefix}role"] ?: error("SOURCE_ANCHOR_INVALID: Relation $index ohne Rolle.")
            val roleName = block.metadata["${prefix}roleName"].orEmpty().ifBlank { null }
            val branchRole = block.metadata["${prefix}branchRole"].orEmpty().ifBlank { null }
            val order = block.metadata["${prefix}order"].orEmpty().ifBlank { null }
            add(
                WorkspaceRelationSourceAnchor(
                    relationId = SemanticRelationId(relationId),
                    relationKind = SemanticRelationKind.valueOf(kind),
                    sourceEntityId = SemanticEntityId(source),
                    targetEntityId = SemanticEntityId(target),
                    roleKind = SemanticRelationRoleKind.valueOf(role),
                    roleName = roleName,
                    branchRole = branchRole?.let(SemanticBranchRole::valueOf),
                    order = order?.toIntOrNull()
                        ?: order?.let { error("SOURCE_ANCHOR_INVALID: Relation $index hat ungültige Ordnung '$it'.") },
                    ownerBlockId = blockId,
                ),
            )
        }
    }
}.sortedBy { it.relationId.value }
