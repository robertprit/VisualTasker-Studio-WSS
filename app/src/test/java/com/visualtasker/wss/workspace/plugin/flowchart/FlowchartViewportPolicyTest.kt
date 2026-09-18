package com.visualtasker.wss.workspace.plugin.flowchart

import androidx.compose.ui.unit.IntSize
import de.visualtasker.flowchart.domain.FlowDocumentId
import de.visualtasker.flowchart.domain.FlowDocumentRevision
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowGraphExtension
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowNodeView
import de.visualtasker.flowchart.domain.FlowPoint
import de.visualtasker.flowchart.domain.FlowSemanticKind
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowSize
import de.visualtasker.flowchart.domain.FlowSurfaceId
import de.visualtasker.flowchart.domain.FlowViewDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowchartViewportPolicyTest {
    @Test
    fun `arrange modes expose the four editor intents`() {
        assertEquals(
            listOf("Semantik", "Analyse", "Kompakt", "Manuell"),
            FlowchartArrangeMode.entries.map { it.displayLabel },
        )
        assertTrue(FlowchartArrangeMode.Semantic.layoutConfig(FlowchartViewOrientation.Vertical).semanticWrapEnabled)
        assertEquals(
            0,
            FlowchartArrangeMode.Manual.layoutConfig(FlowchartViewOrientation.Vertical).wrapAfterNodes,
        )
    }

    @Test
    fun `collapsed facet members do not dominate viewport fit`() {
        val start = node("start", FlowNodeKind.ENTRY)
        val remote = node("remote", FlowNodeKind.ACTION)
        val facet = FlowGraphNode(
            id = FlowNodeId("facet"),
            kind = FlowSemanticKind(FlowNodeKind.SYNTHETIC),
            label = "Details",
            properties = mapOf(
                "visualFacet" to FlowSemanticValue.BooleanValue(true),
                "nodeIds" to FlowSemanticValue.ListValue(
                    listOf(FlowSemanticValue.StringValue(remote.id.value)),
                ),
            ),
        )
        val graph = graph(listOf(start, remote, facet))
        val baseView = view(
            listOf(
                FlowNodeView(start.id, FlowPoint(0.0, 0.0), FlowSize(96.0, 96.0)),
                FlowNodeView(remote.id, FlowPoint(4_000.0, 4_000.0), FlowSize(96.0, 96.0)),
                FlowNodeView(facet.id, FlowPoint(-500.0, -500.0), FlowSize(5_000.0, 5_000.0)),
            ),
        )
        val expanded = fitFlowchartViewport(baseView, graph, IntSize(1_000, 1_000), start.id)
        val collapsed = fitFlowchartViewport(
            baseView.copy(
                extensions = listOf(
                    FlowGraphExtension(
                        key = "visualtasker.collapsed-facets",
                        value = FlowSemanticValue.ListValue(
                            listOf(FlowSemanticValue.StringValue(facet.id.value)),
                        ),
                    ),
                ),
            ),
            graph,
            IntSize(1_000, 1_000),
            start.id,
        )

        assertTrue(collapsed.zoom > expanded.zoom)
        assertEquals(1.8, collapsed.zoom, 0.001)
    }

    private fun node(id: String, kind: FlowNodeKind) = FlowGraphNode(
        id = FlowNodeId(id),
        kind = FlowSemanticKind(kind),
        label = id,
    )

    private fun graph(nodes: List<FlowGraphNode>) = FlowGraphDocument(
        documentId = FlowDocumentId("viewport-policy"),
        documentRevision = FlowDocumentRevision("1"),
        producerId = "test",
        producerVersion = "1",
        sourceRevision = "1",
        sourceHash = "hash",
        nodes = nodes,
        edges = emptyList(),
        entryNodeId = nodes.first().id,
    )

    private fun view(nodes: List<FlowNodeView>) = FlowViewDocument(
        documentId = FlowDocumentId("viewport-policy"),
        compatibleDocumentRevision = FlowDocumentRevision("1"),
        surfaceId = FlowSurfaceId("main"),
        nodeViews = nodes,
    )
}
