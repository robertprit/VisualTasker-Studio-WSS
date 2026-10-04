package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.serialization.WorkflowDecodeResult
import de.visualtasker.workflow.serialization.WorkflowSerializer
import de.visualtasker.flowchart.domain.FlowNodeView
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowSurfaceId
import de.visualtasker.flowchart.domain.FlowViewDocument
import de.visualtasker.flowchart.serialization.FlowDecodeResult
import de.visualtasker.flowchart.serialization.FlowViewJsonCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePersistenceCompatibilityTest {
    @Test
    fun restoresWorkflowFlowViewAndResourcesAsSeparateCompatibleDocuments() {
        val imported = EmscriptWorkspaceImporter().import(EditorDefaults.integrationTestScript)
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val original = WorkspaceWorkflowState.fromDocument(imported.document!!, "test")

        val workspaceJson = WorkflowSerializer.serialize(original.document)
        val restoredWorkspace = WorkflowSerializer.decode(workspaceJson) as WorkflowDecodeResult.Decoded
        val restoredState = WorkspaceWorkflowState.fromDocument(restoredWorkspace.document, "restart")
        assertEquals(original.document, restoredState.document)
        assertEquals(original.irGraph.nodes.map { it.id }, restoredState.irGraph.nodes.map { it.id })

        val graph = original.flowchartProjection.graph
        val flowView = FlowViewDocument(
            documentId = graph.documentId,
            compatibleDocumentRevision = graph.documentRevision,
            surfaceId = FlowSurfaceId("flow-main"),
            nodeViews = graph.nodes.take(3).mapIndexed { index, node ->
                FlowNodeView(node.id, FlowPoint(index * 192.0, index * 192.0))
            },
        )
        val flowViewCodec = FlowViewJsonCodec(graph)
        val restoredFlowView = flowViewCodec.decode(flowViewCodec.encodeCanonical(flowView)) as FlowDecodeResult.Success
        assertEquals(flowView, restoredFlowView.value)
        assertTrue(restoredFlowView.validation.isValid)

        val resourceBundle = WorkspaceResourceBundle(
            revision = 4,
            resources = listOf(
                WorkspaceResource(
                    id = "marker:login",
                    kind = WorkspaceResourceKind.Marker,
                    label = "Login",
                    pluginOwner = "visualtasker.marker",
                    markerMode = WorkspaceMarkerMode.Point,
                    point = WorkspacePointBounds(0.5f, 0.5f),
                )
            ),
        )
        val restoredResources = WorkspaceResourceCodec.decode(
            WorkspaceResourceCodec.encode(resourceBundle)
        ) as WorkspaceResourceDecodeResult.Decoded
        assertEquals(resourceBundle, restoredResources.bundle)
    }
}
