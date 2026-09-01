package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.ir.IrGraphEdgeKind
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.WorkspaceBootstrap
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.flowchart.domain.FlowSemanticValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceWorkflowStateTest {
    @Test
    fun buildsAllEditorProjectionsFromOneWorkspaceDocument() {
        val document = WorkspaceBootstrap.starter()
        val state = WorkspaceWorkflowState.fromDocument(
            document = document,
            mutationSource = "test"
        )

        assertEquals(WorkspaceSerializer.serialize(document), state.serializedJson)
        assertTrue(state.emscriptProjection.isSuccess)
        assertEquals(document.rootBlocks.size, state.document.rootBlocks.size)
        assertTrue(state.flowchartProjection.graph.nodes.isNotEmpty())
        assertEquals("test", state.mutationSource)
    }

    @Test
    fun integrationTestScriptFeedsBlockeditorEmscriptAndFlowchartFromOneState() {
        val imported = EmscriptWorkspaceImporter()
            .import(EditorDefaults.integrationTestScript, workspaceId = "workflow-main")

        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val state = WorkspaceWorkflowState.fromDocument(
            document = imported.document!!,
            mutationSource = "test-script"
        )

        assertTrue(state.document.blocks.values.any { it.type == BlockTypes.CONTROL_REPEAT })
        assertTrue(state.document.blocks.values.any { it.type == BlockTypes.CONTROL_IF_ELSEIF_ELSE })
        assertTrue(state.document.blocks.values.count { it.type == BlockTypes.CONTROL_IF_ELSE } >= 2)
        assertTrue(state.emscriptProjection.getOrThrow().contains("LOOP 10"))
        assertTrue(state.emscriptProjection.getOrThrow().contains("ELSEIF"))
        assertTrue(state.irGraph.edges.any { it.kind == IrGraphEdgeKind.CONDITION })
        assertTrue(state.irGraph.edges.any { it.kind == IrGraphEdgeKind.DATA_FLOW })
        assertTrue(state.flowchartProjection.graph.nodes.any { it.label == "IF" })
        assertEquals(
            FlowSemanticValue.StringValue("ir-graph"),
            state.flowchartProjection.graph.extensions.single { it.key == "visualtasker.projection-source" }.value,
        )
    }
}
