package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.flowchart.FlowchartProjectionResult
import com.visualtasker.wss.flowchart.IrGraphFlowchartProjector
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.compose.debug.BlockEditorDropTrace
import de.visualtasker.blockeditor.ir.IrGraph
import de.visualtasker.blockeditor.ir.IrGraphGenerator
import de.visualtasker.workflow.serialization.WorkflowSerializer

data class WorkspaceWorkflowState(
    val document: WorkspaceDocument,
    val serializedJson: String,
    val irGraph: IrGraph,
    val emscriptProjection: Result<String>,
    val flowchartProjection: FlowchartProjectionResult,
    val resources: WorkspaceResourceBundle = WorkspaceResourceBundle(),
    val worldview: WorldviewDocument = WorldviewDocument.fromResources(resources),
    val mutationSource: String,
) {
    val revision: Int = serializedJson.hashCode()

    companion object {
        fun fromSerialized(
            serializedJson: String,
            mutationSource: String = WORKFLOW_SOURCE_INITIAL,
            resources: WorkspaceResourceBundle = WorkspaceResourceBundle(),
        ): WorkspaceWorkflowState {
            BlockEditorDropTrace.markActive("HOST_STATE_DESERIALIZE_ENTER")
            val document = WorkflowSerializer.deserialize(serializedJson)
            BlockEditorDropTrace.markActive(
                "HOST_STATE_DESERIALIZE_RETURN",
                "blocks=${document.blocks.size}",
            )
            return fromDocument(document, mutationSource, resources)
        }

        fun fromDocument(
            document: WorkspaceDocument,
            mutationSource: String,
            resources: WorkspaceResourceBundle = WorkspaceResourceBundle(),
        ): WorkspaceWorkflowState {
            BlockEditorDropTrace.markActive("HOST_STATE_NORMALIZE_SERIALIZE_ENTER")
            val normalizedJson = WorkflowSerializer.serialize(document)
            BlockEditorDropTrace.markActive(
                "HOST_STATE_NORMALIZE_SERIALIZE_RETURN",
                "bytes=${normalizedJson.toByteArray(Charsets.UTF_8).size}",
            )
            BlockEditorDropTrace.markActive("HOST_STATE_IR_GENERATION_ENTER")
            val irGraph = IrGraphGenerator().generate(document)
            BlockEditorDropTrace.markActive(
                "HOST_STATE_IR_GENERATION_RETURN",
                "nodes=${irGraph.nodes.size} edges=${irGraph.edges.size}",
            )
            BlockEditorDropTrace.markActive("HOST_STATE_EMSCRIPT_GENERATION_ENTER")
            val emscriptProjection = runCatching { EmscriptGenerator().generate(document) }
            BlockEditorDropTrace.markActive(
                "HOST_STATE_EMSCRIPT_GENERATION_RETURN",
                "success=${emscriptProjection.isSuccess} chars=${emscriptProjection.getOrNull()?.length ?: 0}",
            )
            BlockEditorDropTrace.markActive("HOST_STATE_FLOWCHART_PROJECTION_ENTER")
            val flowchartProjection = IrGraphFlowchartProjector.project(irGraph)
            BlockEditorDropTrace.markActive(
                "HOST_STATE_FLOWCHART_PROJECTION_RETURN",
                "nodes=${flowchartProjection.graph.nodes.size} edges=${flowchartProjection.graph.edges.size}",
            )
            return WorkspaceWorkflowState(
                document = document,
                serializedJson = normalizedJson,
                irGraph = irGraph,
                emscriptProjection = emscriptProjection,
                flowchartProjection = flowchartProjection,
                resources = resources,
                worldview = WorldviewDocument.fromResources(resources),
                mutationSource = mutationSource,
            )
        }
    }
}

const val WORKFLOW_SOURCE_INITIAL = "initial"
const val WORKFLOW_SOURCE_BLOCKEDITOR_PREFIX = "blockeditor:"
const val WORKFLOW_SOURCE_EMSCRIPT_APPLY = "emscript:apply"
const val WORKFLOW_SOURCE_EMSCRIPT_AUTO = "$WORKFLOW_SOURCE_EMSCRIPT_APPLY:auto"
const val WORKFLOW_SOURCE_EMSCRIPT_CONFIRM = "$WORKFLOW_SOURCE_EMSCRIPT_APPLY:confirm"
const val WORKFLOW_SOURCE_EMSCRIPT_FILE_PREFIX = "$WORKFLOW_SOURCE_EMSCRIPT_APPLY:file:"
const val WORKFLOW_SOURCE_VT2VT_PREFIX = "vt2vt:"

object WorkspaceMutationSourcePolicy {
    fun coalesces(current: String, previous: String?): Boolean =
        current == previous && (
            (current.startsWith(WORKFLOW_SOURCE_FLOWCHART_PREFIX) && current.endsWith(":move")) ||
                current == WORKFLOW_SOURCE_EMSCRIPT_AUTO
            )
}
