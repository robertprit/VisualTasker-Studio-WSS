package com.visualtasker.wss.workspace.plugin.flowchart

import de.visualtasker.blockeditor.registry.BlockNodePresentationContract
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.SemanticPropertyCategory
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticKind
import de.visualtasker.flowchart.domain.FlowSemanticValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowchartInspectorPresentationContractTest {
    @Test
    fun flowchartInspectorFieldsUseSameSemanticPropertyIdAsBlockInspector() {
        val node = FlowGraphNode(
            id = FlowNodeId("block:block-42"),
            kind = FlowSemanticKind(FlowNodeKind.ACTION),
            label = "Wait",
            properties = mapOf(
                "blockType" to FlowSemanticValue.StringValue(BlockTypes.ACTION_WAIT),
                "waitMs" to FlowSemanticValue.NumberValue("750"),
            ),
        )

        val field = flowchartInspectorProperties(node).first { it.fieldKey == "ms" }
        assertEquals(
            BlockNodePresentationContract.fieldPropertyId("block-42", "ms"),
            field.semanticPropertyId,
        )
        assertEquals(SemanticPropertyCategory.INPUT, field.semanticCategory)
    }

    @Test
    fun commandArgumentsMapToInputCategoryAndStableIds() {
        val node = FlowGraphNode(
            id = FlowNodeId("block:block-77"),
            kind = FlowSemanticKind(FlowNodeKind.ACTION),
            label = "Command",
            properties = mapOf(
                "commandId" to FlowSemanticValue.StringValue("scene.save"),
                "args" to FlowSemanticValue.StringValue("\"https://\",true"),
            ),
        )

        val fields = flowchartInspectorProperties(node).filter { it.fieldKey.startsWith("args:") }
        assertTrue(fields.isNotEmpty())
        assertTrue(fields.all { it.semanticCategory == SemanticPropertyCategory.INPUT })
        assertTrue(
            fields.all { field ->
                field.semanticPropertyId == BlockNodePresentationContract.fieldPropertyId("block-77", field.fieldKey)
            },
        )
    }
}
