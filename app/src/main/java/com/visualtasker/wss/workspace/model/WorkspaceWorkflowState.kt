package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.flowchart.FlowchartProjectionResult
import com.visualtasker.wss.flowchart.IrGraphFlowchartProjector
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGraph
import de.visualtasker.blockeditor.ir.IrGraphGenerator
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer

data class WorkspaceWorkflowState(
    val document: WorkspaceDocument,
    val serializedJson: String,
    val irGraph: IrGraph,
    val emscriptProjection: Result<String>,
    val flowchartProjection: FlowchartProjectionResult,
    val mutationSource: String,
) {
    val revision: Int = serializedJson.hashCode()

    companion object {
        fun fromSerialized(
            serializedJson: String,
            mutationSource: String = WORKFLOW_SOURCE_INITIAL,
        ): WorkspaceWorkflowState {
            val document = WorkspaceSerializer.deserialize(serializedJson)
            return fromDocument(document, mutationSource)
        }

        fun fromDocument(
            document: WorkspaceDocument,
            mutationSource: String,
        ): WorkspaceWorkflowState {
            val normalizedJson = WorkspaceSerializer.serialize(document)
            val irGraph = IrGraphGenerator().generate(document)
            return WorkspaceWorkflowState(
                document = document,
                serializedJson = normalizedJson,
                irGraph = irGraph,
                emscriptProjection = runCatching { EmscriptGenerator().generate(document) },
                flowchartProjection = IrGraphFlowchartProjector.project(irGraph),
                mutationSource = mutationSource,
            )
        }
    }
}

const val WORKFLOW_SOURCE_INITIAL = "initial"
const val WORKFLOW_SOURCE_BLOCKEDITOR_PREFIX = "blockeditor:"
const val WORKFLOW_SOURCE_EMSCRIPT_APPLY = "emscript:apply"
