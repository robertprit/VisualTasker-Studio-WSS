package com.visualtasker.wss.workspace.conformance

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.serialization.FlowDecodeResult
import de.visualtasker.flowchart.serialization.FlowViewJsonCodec
import de.visualtasker.workflow.core.BlockNode
import de.visualtasker.workflow.core.CanonicalWorkspaceDocument
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.serialization.WorkflowSerializer
import java.security.MessageDigest

internal data class ConformanceFingerprints(
    val semantics: String,
    val identity: String,
    val metadata: String,
    val persistence: String,
)

internal data class ConformanceSnapshot(
    val document: WorkspaceDocument,
    val flowView: FlowViewDocument?,
    val semanticStructure: List<String>,
    val identityStructure: List<String>,
    val metadataStructure: List<String>,
    val persistenceStructure: List<String>,
    val fingerprints: ConformanceFingerprints,
)

internal data class ThreeEditorRoundtrip(
    val sourceDocument: WorkspaceDocument,
    val sourceSnapshot: ConformanceSnapshot,
    val blockProjection: Map<String, BlockNode>,
    val flowNodeIds: List<String>,
    val generatedSource: String,
    val reappliedDocument: WorkspaceDocument,
    val reappliedSnapshot: ConformanceSnapshot,
    val sourceApplyOperationCount: Int,
)

/** Test-only orchestration over production parser, projections, source apply, and serializers. */
internal object ThreeEditorConformanceHarness {
    private val guard = EmscriptApplyGuard()

    fun importSource(source: String, workspaceId: String): EmscriptApplyGuardResult =
        guard.preview(source, workspaceId = workspaceId)

    fun applySource(
        source: String,
        workspaceId: String,
        previousDocument: WorkspaceDocument,
    ): EmscriptApplyGuardResult = guard.preview(
        source,
        workspaceId = workspaceId,
        previousDocument = previousDocument,
    )

    fun roundtrip(
        source: String,
        workspaceId: String,
        decorate: (WorkspaceDocument) -> WorkspaceDocument = { it },
        flowView: (WorkspaceWorkflowState) -> FlowViewDocument? = { null },
    ): ThreeEditorRoundtrip {
        val imported = requireSuccess(guard.preview(source, workspaceId = workspaceId))
        val sourceDocument = decorate(imported.importedDocument)
        val sourceState = WorkspaceWorkflowState.fromDocument(sourceDocument, "m4-0:source")
        val projectedFlowView = flowView(sourceState)
        val sourceSnapshot = snapshot(sourceState.document, projectedFlowView)
        val generated = EmscriptGenerator().generate(sourceState.document)
        val reapplied = requireSuccess(
            guard.preview(generated, workspaceId = workspaceId, previousDocument = sourceState.document),
        )
        val reappliedState = WorkspaceWorkflowState.fromDocument(reapplied.importedDocument, "m4-0:reapply")
        val reappliedSnapshot = snapshot(reappliedState.document, projectedFlowView)
        return ThreeEditorRoundtrip(
            sourceDocument = sourceState.document,
            sourceSnapshot = sourceSnapshot,
            blockProjection = sourceState.document.blocks.toSortedMap(compareBy { it.value })
                .mapKeys { it.key.value },
            flowNodeIds = sourceState.flowchartProjection.graph.nodes.map { it.id.value }.sorted(),
            generatedSource = generated,
            reappliedDocument = reappliedState.document,
            reappliedSnapshot = reappliedSnapshot,
            sourceApplyOperationCount = reapplied.sourceApplyPlan.transaction.operations.size,
        )
    }

    fun saveReload(document: WorkspaceDocument): WorkspaceDocument =
        WorkflowSerializer.deserialize(WorkflowSerializer.serialize(document))

    fun saveReload(flowView: FlowViewDocument, document: WorkspaceDocument): FlowViewDocument {
        val graph = WorkspaceWorkflowState.fromDocument(document, "m4-0:flow-codec").flowchartProjection.graph
        val codec = FlowViewJsonCodec(graph)
        val decoded = codec.decode(codec.encodeCanonical(flowView))
        require(decoded is FlowDecodeResult.Success) { "FlowView reload failed: $decoded" }
        return decoded.value
    }

    fun snapshot(document: WorkspaceDocument, flowView: FlowViewDocument? = null): ConformanceSnapshot {
        val canonical = requireNotNull(document.canonical)
        val semantics = semanticStructure(document, canonical)
        val identity = identityStructure(canonical)
        val metadata = metadataStructure(document, flowView)
        val persistence = persistenceStructure(document, flowView)
        return ConformanceSnapshot(
            document = document,
            flowView = flowView,
            semanticStructure = semantics,
            identityStructure = identity,
            metadataStructure = metadata,
            persistenceStructure = persistence,
            fingerprints = ConformanceFingerprints(
                semantics = digest(semantics),
                identity = digest(identity),
                metadata = digest(metadata),
                persistence = digest(persistence),
            ),
        )
    }

    fun assertStructurallyEqual(expected: ConformanceSnapshot, actual: ConformanceSnapshot) {
        check(expected.semanticStructure == actual.semanticStructure) { "Semantic structures differ." }
        check(expected.identityStructure == actual.identityStructure) { "Identity structures differ." }
        check(expected.metadataStructure == actual.metadataStructure) {
            "Metadata structures differ. expectedOnly=${expected.metadataStructure - actual.metadataStructure.toSet()} " +
                "actualOnly=${actual.metadataStructure - expected.metadataStructure.toSet()}"
        }
    }

    private fun semanticStructure(
        document: WorkspaceDocument,
        canonical: CanonicalWorkspaceDocument,
    ): List<String> = buildList {
        canonical.entities.sortedBy { it.ref.id.value }.forEach { entity ->
            add("entity|${entity.ref.id.value}|${entity.ref.kind}|${entity.origin}|${entity.legacySourceId.orEmpty()}")
        }
        canonical.relations.sortedBy { it.id.value }.forEach { relation ->
            add(
                "relation|${relation.id.value}|${relation.kind}|${relation.source.id.value}|" +
                    "${relation.target.id.value}|${relation.role.kind}|${relation.role.name.orEmpty()}|" +
                    "${relation.role.branchRole?.name.orEmpty()}|${relation.order ?: -1}",
            )
        }
        document.blocks.toSortedMap(compareBy { it.value }).forEach { (id, block) ->
            add("block|${id.value}|${block.type}|${stableFields(block.fields)}")
        }
        document.variables.variables.toSortedMap().forEach { (id, variable) ->
            add("variable|$id|${variable.name}|${variable.type}|${variable.scope}|${variable.defaultValue.orEmpty()}")
        }
    }

    private fun identityStructure(canonical: CanonicalWorkspaceDocument): List<String> = buildList {
        canonical.entities.sortedBy { it.ref.id.value }.forEach { entity ->
            add("entity|${entity.ref.kind}|${entity.ref.id.value}")
        }
        canonical.relations.sortedBy { it.id.value }.forEach { relation ->
            add("relation|${relation.id.value}|${relation.source.id.value}|${relation.target.id.value}")
        }
        canonical.entities
            .filter { it.ref.kind == SemanticEntityKind.Branch }
            .sortedBy { it.ref.id.value }
            .forEach { add("branch|${it.ref.id.value}") }
    }

    private fun metadataStructure(document: WorkspaceDocument, flowView: FlowViewDocument?): List<String> = buildList {
        document.blocks.toSortedMap(compareBy { it.value }).forEach { (id, block) ->
            val labelsAndNotes = block.fields
                .filterKeys { key -> key.contains("label", ignoreCase = true) || key.contains("note", ignoreCase = true) }
            val retainedMetadata = block.metadata.filterKeys { key -> !key.startsWith("emscript.source.") }
            add("block-meta|${id.value}|${stableMap(retainedMetadata)}|${stableFields(labelsAndNotes)}|${block.collapsed}")
        }
        document.rootPositions.toSortedMap(compareBy { it.value }).forEach { (id, point) ->
            add("block-view|${id.value}|${point.x}|${point.y}")
        }
        flowView?.let { view ->
            add("flow-viewport|${view.surfaceId.value}|${view.viewport.pan.x}|${view.viewport.pan.y}|${view.viewport.zoom}")
            view.nodeViews.sortedBy { it.nodeId.value }.forEach { node ->
                add(
                    "flow-node|${node.nodeId.value}|${node.position.x}|${node.position.y}|" +
                        "${node.size?.width ?: -1.0}|${node.size?.height ?: -1.0}|${node.collapsed}|" +
                        "${node.visualToken.orEmpty()}|${node.pinned}",
                )
            }
            view.annotations.sortedBy { it.id }.forEach { annotation ->
                add("flow-note|${annotation.id}|${annotation.label}|${annotation.position.x}|${annotation.position.y}")
            }
        }
    }

    private fun persistenceStructure(document: WorkspaceDocument, flowView: FlowViewDocument?): List<String> =
        buildList {
            addAll(semanticStructure(document, requireNotNull(document.canonical)))
            addAll(identityStructure(requireNotNull(document.canonical)))
            addAll(metadataStructure(document, flowView))
            add("workspace|${document.id}|${document.version}|${document.rootBlocks.joinToString { it.value }}")
            flowView?.let { add("flow-view|${it.schemaVersion}|${it.documentId.value}|${it.compatibleDocumentRevision.value}") }
        }

    private fun stableFields(fields: Map<String, FieldValue>): String = fields.toSortedMap().entries.joinToString(";") {
        (key, value) -> "$key=${value.stableValue()}"
    }

    private fun stableMap(values: Map<String, String>): String =
        values.toSortedMap().entries.joinToString(";") { (key, value) -> "$key=$value" }

    private fun FieldValue.stableValue(): String = when (this) {
        is FieldValue.Bool -> "bool:$value"
        is FieldValue.Number -> "number:$value"
        is FieldValue.Text -> "text:$value"
    }

    private fun digest(lines: List<String>): String = MessageDigest.getInstance("SHA-256")
        .digest(lines.joinToString("\n").toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun requireSuccess(result: EmscriptApplyGuardResult): EmscriptApplyGuardResult.Success {
        require(result is EmscriptApplyGuardResult.Success) { "EMScript production path rejected fixture: $result" }
        return result
    }
}
