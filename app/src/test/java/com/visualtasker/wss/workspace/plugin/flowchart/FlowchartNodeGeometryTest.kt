package com.visualtasker.wss.workspace.plugin.flowchart

import de.visualtasker.flowchart.domain.FlowExecutionKind
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowLifecycleSemantics
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticKind
import de.visualtasker.flowchart.domain.FlowTerminatorRole
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.flowchart.domain.FlowSemanticValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FlowchartNodeGeometryTest {
    @Test
    fun everyStandardNodeKindHasAnExplicitShape() {
        FlowNodeKind.entries.forEach { kind ->
            assertNotEquals("Missing explicit shape for $kind", FlowchartNodeShape.Default, flowchartNodeShape(node(kind)))
        }
    }

    @Test
    fun workflowTerminatorsUseDifferentSilhouettes() {
        assertEquals(FlowchartNodeShape.Circle, flowchartNodeShape(terminator(FlowTerminatorRole.START, FlowExecutionKind.WORKFLOW)))
        assertEquals(FlowchartNodeShape.Arch, flowchartNodeShape(terminator(FlowTerminatorRole.END, FlowExecutionKind.WORKFLOW)))
    }

    @Test
    fun recordingAndDryRunTerminatorsRemainVisuallyDistinct() {
        val shapes = listOf(
            terminator(FlowTerminatorRole.START, FlowExecutionKind.RECORDING),
            terminator(FlowTerminatorRole.END, FlowExecutionKind.RECORDING),
            terminator(FlowTerminatorRole.START, FlowExecutionKind.DRY_RUN),
            terminator(FlowTerminatorRole.END, FlowExecutionKind.DRY_RUN),
        ).map(::flowchartNodeShape)
        assertEquals(shapes.size, shapes.distinct().size)
    }

    @Test
    fun dataAndControlFamiliesDoNotCollapseToOneShape() {
        val kinds = listOf(
            FlowNodeKind.ACTION, FlowNodeKind.INPUT, FlowNodeKind.OUTPUT, FlowNodeKind.ASSIGNMENT,
            FlowNodeKind.PROPERTY_ACCESS, FlowNodeKind.DECISION, FlowNodeKind.LOOP_START, FlowNodeKind.LOOP_END,
        )
        assertEquals(kinds.size, kinds.map { flowchartNodeShape(node(it)) }.distinct().size)
    }

    @Test
    fun knownReporterAndOperatorBlockTypesUseDistinctShapes() {
        val blockTypes = listOf(
            BlockTypes.LITERAL_NUMBER,
            BlockTypes.LITERAL_STRING,
            BlockTypes.LITERAL_BOOLEAN,
            BlockTypes.LOGIC_COMPARE,
            BlockTypes.LOGIC_OPERATE,
            BlockTypes.VARIABLE_GET,
            BlockTypes.VARIABLE_SET,
        )
        val shapes = blockTypes.map { type ->
            flowchartNodeShape(node(FlowNodeKind.INPUT, type))
        }

        assertEquals(blockTypes.size, shapes.distinct().size)
    }

    private fun node(kind: FlowNodeKind, blockType: String? = null): FlowGraphNode = FlowGraphNode(
        id = FlowNodeId("${kind.name.lowercase()}-${blockType.orEmpty()}"),
        kind = FlowSemanticKind(kind),
        label = kind.name,
        properties = blockType?.let { mapOf("blockType" to FlowSemanticValue.StringValue(it)) }.orEmpty(),
    )

    private fun terminator(role: FlowTerminatorRole, kind: FlowExecutionKind): FlowGraphNode = FlowGraphNode(
        id = FlowNodeId("${kind.wireValue}-${role.wireValue}"),
        kind = FlowSemanticKind(if (role == FlowTerminatorRole.START) FlowNodeKind.ENTRY else FlowNodeKind.EXIT),
        label = role.name,
        properties = FlowLifecycleSemantics.nodeProperties(kind, role),
    )
}
