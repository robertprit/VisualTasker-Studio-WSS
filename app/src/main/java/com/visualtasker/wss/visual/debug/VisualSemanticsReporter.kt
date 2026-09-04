package com.visualtasker.wss.visual.debug

import com.visualtasker.wss.visual.descriptor.OutlineRole
import com.visualtasker.wss.visual.policy.DefaultVisualPolicyResolver
import com.visualtasker.wss.visual.projections.FlowchartNodeVisualAdapter
import com.visualtasker.wss.visual.projections.FlowchartNodeVisualSubject
import com.visualtasker.wss.visual.semantics.ProjectionKind
import com.visualtasker.wss.visual.semantics.VisualActivity
import com.visualtasker.wss.visual.semantics.VisualContext
import com.visualtasker.wss.visual.semantics.VisualRole
import com.visualtasker.wss.visual.semantics.VisualValidation
import de.visualtasker.flowchart.domain.FlowGraphDocument
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot

object VisualSemanticsReporter {
    fun summarizeFlowchart(
        graph: FlowGraphDocument,
        runtimeSnapshot: FlowRuntimeSnapshot?,
        maxLines: Int = 8,
    ): List<String> {
        if (graph.nodes.isEmpty()) return listOf("VAL Flowchart: keine Nodes.")
        val context = VisualContext(projection = ProjectionKind.Flowchart)
        val states = graph.nodes.map { node ->
            FlowchartNodeVisualAdapter.map(
                FlowchartNodeVisualSubject(
                    node = node,
                    graph = graph,
                    runtimeSnapshot = runtimeSnapshot,
                    selectedNodeId = runtimeSnapshot?.activeNodeId,
                ),
                context,
            )
        }
        val descriptors = states.map { state -> DefaultVisualPolicyResolver.resolve(state, context) }
        val lines = mutableListOf<String>()
        lines += "VAL Flowchart: ${graph.nodes.size} Nodes, ${states.count { it.validation != VisualValidation.Valid }} Hinweise, ${states.count { it.activity != VisualActivity.Idle }} Runtime-States."
        lines += states.groupingBy { it.role }.eachCount().toDisplayLine("VAL Rollen")
        lines += states.groupingBy { it.activity }.eachCount()
            .filterKeys { it != VisualActivity.Idle }
            .toDisplayLine("VAL Aktivitaet")
            .ifBlank { "VAL Aktivitaet: idle" }
        lines += descriptors.groupingBy { it.outlineRole }.eachCount()
            .filterKeys { it != OutlineRole.None }
            .toDisplayLine("VAL Outlines")
            .ifBlank { "VAL Outlines: keine" }
        return lines.take(maxLines.coerceAtLeast(1))
    }

    private fun <T : Enum<T>> Map<T, Int>.toDisplayLine(label: String): String =
        if (isEmpty()) {
            ""
        } else {
            entries
                .sortedWith(compareBy<Map.Entry<T, Int>> { it.key.name }.thenBy { it.value })
                .joinToString(prefix = "$label: ") { "${it.key.name}=${it.value}" }
        }
}
