package com.visualtasker.wss.emscript.apply

import com.visualtasker.wss.emscript.parser.EMSCRIPT_SOURCE_PROPERTIES_METADATA
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.BlockNode
import de.visualtasker.workflow.core.CanonicalWorkspaceMigration
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.SemanticEntity
import de.visualtasker.workflow.core.SemanticEntityId
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.SemanticPropertyId
import de.visualtasker.workflow.core.SemanticRelation
import de.visualtasker.workflow.core.SemanticRelationId
import de.visualtasker.workflow.core.VariableDefinition
import de.visualtasker.workflow.core.VariableRegistry
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.core.WorkspaceOperation
import de.visualtasker.workflow.core.WorkspaceOperationExecutor
import de.visualtasker.workflow.core.WorkspaceOperationId
import de.visualtasker.workflow.core.WorkspaceOperationResult
import de.visualtasker.workflow.core.WorkspacePropertyValue
import de.visualtasker.workflow.core.WorkspaceTransaction
import de.visualtasker.workflow.semantics.VisualTaskerSemanticPropertySchema

enum class SemanticSourcePropertyDisposition {
    ExplicitChange,
    Unchanged,
    NotRepresentedPreserved,
    NewEntityDefault,
}

data class SemanticSourcePropertyDecision(
    val entityId: SemanticEntityId,
    val propertyId: String,
    val disposition: SemanticSourcePropertyDisposition,
)

data class SemanticSourceApplyPlan(
    val transaction: WorkspaceTransaction,
    val baselineDocument: WorkspaceDocument,
    val stagedDocument: WorkspaceDocument,
    val targetDocument: WorkspaceDocument,
    val unchangedEntityIds: Set<SemanticEntityId>,
    val unchangedRelationIds: Set<SemanticRelationId>,
    val propertyDecisions: List<SemanticSourcePropertyDecision>,
) {
    val operationCounts: Map<String, Int> = transaction.operations
        .groupingBy { it::class.simpleName ?: "Unknown" }
        .eachCount()

    val preservedPropertyCount: Int = propertyDecisions.count {
        it.disposition == SemanticSourcePropertyDisposition.NotRepresentedPreserved
    }
}

/** Pure source-to-operation planner. It contains no editor, Compose or publication state. */
object EmscriptSemanticSourceApply {
    fun plan(
        current: WorkspaceDocument?,
        imported: WorkspaceDocument,
    ): SemanticSourceApplyPlan {
        val currentMigration = CanonicalWorkspaceMigration.toCurrent(
            current ?: WorkspaceDocument(id = imported.id),
        )
        require(currentMigration.isValid) { "Current workspace is not canonical and valid." }
        val targetMigration = CanonicalWorkspaceMigration.toCurrent(imported)
        require(targetMigration.isValid) { "Imported workspace is not canonical and valid." }
        val baseline = currentMigration.document
        val target = targetMigration.document
        val baselineCanonical = requireNotNull(baseline.canonical)
        val targetCanonical = requireNotNull(target.canonical)
        val baselineEntities = baselineCanonical.entities.associateBy { it.ref.id }
        val targetEntities = targetCanonical.entities.associateBy { it.ref.id }
        val baselineRelations = baselineCanonical.relations.associateBy(SemanticRelation::id)
        val targetRelations = targetCanonical.relations.associateBy(SemanticRelation::id)
        val propertyDecisions = mutableListOf<SemanticSourcePropertyDecision>()

        val mergedTargetBlocks = target.blocks.mapValues { (blockId, candidate) ->
            val previous = baseline.blocks[blockId]
            if (previous == null) {
                candidate.sourceProperties().forEach { property ->
                    candidate.entityId(target)?.let { entityId ->
                        propertyDecisions += SemanticSourcePropertyDecision(
                            entityId,
                            property,
                            SemanticSourcePropertyDisposition.NewEntityDefault,
                        )
                    }
                }
                candidate
            } else {
                mergeRetainedBlock(previous, candidate, target, propertyDecisions)
            }
        }
        val mergedVariables = target.variables.variables.mapValues { (variableId, candidate) ->
            val previous = baseline.variables.variables[variableId]
            if (previous == null) {
                candidate.variableEntityId().let { entityId ->
                    listOf("name", "type", "defaultValue").forEach { property ->
                        propertyDecisions += SemanticSourcePropertyDecision(
                            entityId,
                            property,
                            SemanticSourcePropertyDisposition.NewEntityDefault,
                        )
                    }
                }
                candidate
            } else {
                mergeRetainedVariable(previous, candidate, propertyDecisions)
            }
        }
        val staged = baseline.copy(
            blocks = baseline.blocks + mergedTargetBlocks,
            variables = VariableRegistry(baseline.variables.variables + mergedVariables),
        )

        val transactionSeed = buildString {
            targetCanonical.entities.forEach { append(it.ref.id.value).append('|') }
            targetCanonical.relations.forEach { append(it.id.value).append(':').append(it.order).append('|') }
            mergedTargetBlocks.toSortedMap(compareBy(BlockId::value)).forEach { (id, block) ->
                append(id.value).append(':').append(block.fields).append('|')
            }
        }
        val transactionId = WorkspaceOperationId(
            "source-apply-${baseline.version}-${transactionSeed.hashCode().toUInt()}",
        )
        var operationIndex = 0
        fun operationId(kind: String, stableId: String): WorkspaceOperationId = WorkspaceOperationId(
            "${transactionId.value}:${operationIndex++}:$kind:${stableId.hashCode().toUInt()}",
        )
        val operations = mutableListOf<WorkspaceOperation>()

        baselineRelations.values
            .filter { existing ->
                val desired = targetRelations[existing.id]
                desired == null || !existing.sameStructure(desired)
            }
            .sortedBy { it.id.value }
            .forEach { relation ->
                operations += WorkspaceOperation.RemoveRelation(
                    operationId("remove-relation", relation.id.value),
                    relation.id,
                )
            }
        baselineEntities.values
            .filter { it.ref.id !in targetEntities }
            .sortedBy { it.ref.id.value }
            .forEach { entity ->
                operations += WorkspaceOperation.DeleteEntity(
                    operationId("delete-entity", entity.ref.id.value),
                    entity.ref.id,
                )
            }
        targetEntities.values
            .filter { it.ref.id !in baselineEntities }
            .sortedBy { it.ref.id.value }
            .forEach { entity ->
                operations += WorkspaceOperation.CreateEntity(
                    operationId("create-entity", entity.ref.id.value),
                    entity,
                )
            }
        targetRelations.values
            .filter { desired ->
                val existing = baselineRelations[desired.id]
                existing == null || !existing.sameStructure(desired)
            }
            .sortedBy { it.id.value }
            .forEach { relation ->
                operations += WorkspaceOperation.CreateRelation(
                    operationId("create-relation", relation.id.value),
                    relation,
                )
            }
        targetRelations.values
            .filter { desired ->
                val existing = baselineRelations[desired.id]
                existing != null && existing.sameStructure(desired) && existing.order != desired.order && desired.order != null
            }
            .sortedBy { it.id.value }
            .forEach { relation ->
                operations += WorkspaceOperation.SetOrder(
                    operationId("set-order", relation.id.value),
                    relation.id,
                    requireNotNull(relation.order),
                )
            }

        mergedTargetBlocks.toSortedMap(compareBy(BlockId::value)).forEach { (blockId, mergedBlock) ->
            val previous = baseline.blocks[blockId] ?: return@forEach
            val entityId = mergedBlock.entityId(target) ?: return@forEach
            if (entityId !in baselineEntities || entityId !in targetEntities) return@forEach
            target.blocks.getValue(blockId).sourceProperties().sorted().forEach { property ->
                val desired = mergedBlock.fields[property] ?: return@forEach
                if (previous.fields[property] != desired) {
                    operations += WorkspaceOperation.SetProperty(
                        operationId("set-property", "${entityId.value}:$property"),
                        entityId,
                        SemanticPropertyId(property),
                        desired.toWorkspacePropertyValue(),
                    )
                }
            }
        }
        mergedVariables.toSortedMap().forEach { (variableId, merged) ->
            val previous = baseline.variables.variables[variableId] ?: return@forEach
            val entityId = merged.variableEntityId()
            if (entityId !in baselineEntities || entityId !in targetEntities) return@forEach
            listOf(
                "name" to WorkspacePropertyValue.Text(merged.name),
                "type" to WorkspacePropertyValue.Text(merged.type),
                "defaultValue" to (merged.defaultValue?.let(WorkspacePropertyValue::Text) ?: WorkspacePropertyValue.Null),
            ).forEach { (property, desired) ->
                val previousValue = when (property) {
                    "name" -> WorkspacePropertyValue.Text(previous.name)
                    "type" -> WorkspacePropertyValue.Text(previous.type)
                    else -> previous.defaultValue?.let(WorkspacePropertyValue::Text) ?: WorkspacePropertyValue.Null
                }
                if (previousValue != desired) {
                    operations += WorkspaceOperation.SetProperty(
                        operationId("set-property", "${entityId.value}:$property"),
                        entityId,
                        SemanticPropertyId(property),
                        desired,
                    )
                }
            }
        }

        val unchangedEntities = baselineEntities.keys.intersect(targetEntities.keys).filterTo(linkedSetOf()) { id ->
            baselineEntities[id] == targetEntities[id] && operations.none {
                (it as? WorkspaceOperation.SetProperty)?.target == id
            }
        }
        val unchangedRelations = baselineRelations.keys.intersect(targetRelations.keys).filterTo(linkedSetOf()) { id ->
            baselineRelations[id] == targetRelations[id]
        }
        return SemanticSourceApplyPlan(
            transaction = WorkspaceTransaction(
                transactionId = transactionId,
                operations = operations,
                expectedRevision = baseline.version,
            ),
            baselineDocument = baseline,
            stagedDocument = staged,
            targetDocument = target,
            unchangedEntityIds = unchangedEntities,
            unchangedRelationIds = unchangedRelations,
            propertyDecisions = propertyDecisions.distinct(),
        )
    }

    fun execute(plan: SemanticSourceApplyPlan): WorkspaceOperationResult {
        val result = WorkspaceOperationExecutor.executeWithStagedPayload(
            document = plan.baselineDocument,
            transaction = plan.transaction,
            stagedPayload = plan.stagedDocument,
            propertySchema = VisualTaskerSemanticPropertySchema,
        )
        if (result !is WorkspaceOperationResult.Success) return result
        val rootPositions = result.document.rootBlocks.mapNotNull { blockId ->
            val position = plan.baselineDocument.rootPositions[blockId]
                ?: plan.targetDocument.rootPositions[blockId]
            position?.let { blockId to it }
        }.toMap()
        return result.copy(document = result.document.copy(rootPositions = rootPositions))
    }

    private fun mergeRetainedBlock(
        previous: BlockNode,
        candidate: BlockNode,
        target: WorkspaceDocument,
        decisions: MutableList<SemanticSourcePropertyDecision>,
    ): BlockNode {
        val represented = candidate.sourceProperties()
        val entityId = candidate.entityId(target)
        val mergedFields = candidate.fields.toMutableMap().apply {
            putAll(previous.fields)
            represented.forEach { property -> candidate.fields[property]?.let { put(property, it) } }
        }
        if (entityId != null) {
            represented.forEach { property ->
                decisions += SemanticSourcePropertyDecision(
                    entityId,
                    property,
                    if (previous.fields[property] == candidate.fields[property]) {
                        SemanticSourcePropertyDisposition.Unchanged
                    } else {
                        SemanticSourcePropertyDisposition.ExplicitChange
                    },
                )
            }
            previous.fields.keys.filterNot(represented::contains).forEach { property ->
                decisions += SemanticSourcePropertyDecision(
                    entityId,
                    property,
                    SemanticSourcePropertyDisposition.NotRepresentedPreserved,
                )
            }
            previous.metadata.keys.forEach { property ->
                decisions += SemanticSourcePropertyDecision(
                    entityId,
                    "metadata.$property",
                    SemanticSourcePropertyDisposition.NotRepresentedPreserved,
                )
            }
            decisions += SemanticSourcePropertyDecision(
                entityId,
                "collapsed",
                SemanticSourcePropertyDisposition.NotRepresentedPreserved,
            )
        }
        val mergedMetadata = candidate.metadata.toMutableMap().apply {
            putAll(previous.metadata)
            candidate.metadata[EMSCRIPT_SOURCE_PROPERTIES_METADATA]?.let {
                put(EMSCRIPT_SOURCE_PROPERTIES_METADATA, it)
            }
            candidate.metadata["if.branchCount"]?.let { put("if.branchCount", it) }
            candidate.metadata["emscript.declaredType"]?.let { put("emscript.declaredType", it) }
        }
        return candidate.copy(
            fields = mergedFields,
            collapsed = previous.collapsed,
            metadata = mergedMetadata,
        )
    }

    private fun mergeRetainedVariable(
        previous: VariableDefinition,
        candidate: VariableDefinition,
        decisions: MutableList<SemanticSourcePropertyDecision>,
    ): VariableDefinition {
        val entityId = candidate.variableEntityId()
        listOf(
            "name" to (previous.name == candidate.name),
            "type" to (previous.type == candidate.type),
            "defaultValue" to (previous.defaultValue == candidate.defaultValue),
        ).forEach { (property, unchanged) ->
            decisions += SemanticSourcePropertyDecision(
                entityId,
                property,
                if (unchanged) SemanticSourcePropertyDisposition.Unchanged else SemanticSourcePropertyDisposition.ExplicitChange,
            )
        }
        decisions += SemanticSourcePropertyDecision(
            entityId,
            "scope",
            SemanticSourcePropertyDisposition.NotRepresentedPreserved,
        )
        return candidate.copy(scope = previous.scope)
    }

    private fun BlockNode.sourceProperties(): Set<String> =
        metadata[EMSCRIPT_SOURCE_PROPERTIES_METADATA]
            .orEmpty()
            .split(',')
            .filterTo(linkedSetOf(), String::isNotBlank)

    private fun BlockNode.entityId(document: WorkspaceDocument): SemanticEntityId? =
        document.canonical?.entities?.firstOrNull { entity ->
            entity.ref.kind in setOf(SemanticEntityKind.Statement, SemanticEntityKind.Expression) &&
                entity.legacySourceId == id.value
        }?.ref?.id

    private fun VariableDefinition.variableEntityId(): SemanticEntityId = SemanticEntityId("variable:$id")

    private fun SemanticRelation.sameStructure(other: SemanticRelation): Boolean =
        copy(order = null) == other.copy(order = null)

    private fun FieldValue.toWorkspacePropertyValue(): WorkspacePropertyValue = when (this) {
        is FieldValue.Text -> WorkspacePropertyValue.Text(value)
        is FieldValue.Number -> WorkspacePropertyValue.Number(value)
        is FieldValue.Bool -> WorkspacePropertyValue.Bool(value)
    }
}

/** Prevents a persisted draft from being treated as a fresh edit during workspace startup. */
class EmscriptAutomaticDraftApplyGate {
    private var initialObservationConsumed = false

    fun shouldApply(content: String): Boolean {
        if (!initialObservationConsumed) {
            initialObservationConsumed = true
            return false
        }
        return content.isNotBlank()
    }
}
