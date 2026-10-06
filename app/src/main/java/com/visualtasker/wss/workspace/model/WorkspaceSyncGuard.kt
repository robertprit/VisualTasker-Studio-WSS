package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import de.visualtasker.blockeditor.compose.debug.BlockEditorDropTrace
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrGraphGenerator
import de.visualtasker.workflow.semantics.ir.validateIntegrity
import de.visualtasker.workflow.semantics.ir.validateSemantics
import de.visualtasker.workflow.serialization.WorkflowDecodeResult
import de.visualtasker.workflow.serialization.WorkflowSerializer

class WorkspaceSyncGuard {
    fun inspect(serializedJson: String): WorkspaceSyncGuardReport {
        BlockEditorDropTrace.markActive("SYNC_GUARD_DESERIALIZE_ENTER")
        val decoded = WorkflowSerializer.decode(serializedJson)
        val document = when (decoded) {
            is WorkflowDecodeResult.Decoded -> decoded.document
            is WorkflowDecodeResult.Malformed -> return WorkspaceSyncGuardReport(
                isValid = false,
                messages = listOf("Workspace JSON ist fehlerhaft: ${decoded.reason}"),
            )
            is WorkflowDecodeResult.UnsupportedSchema -> return WorkspaceSyncGuardReport(
                isValid = false,
                messages = listOf("Workspace Schema wird nicht unterstützt: ${decoded.version}"),
            )
        }
        BlockEditorDropTrace.markActive("SYNC_GUARD_DESERIALIZE_RETURN", "blocks=${document.blocks.size}")
        val messages = mutableListOf<String>()
        BlockEditorDropTrace.markActive("SYNC_GUARD_NORMALIZE_ENTER")
        val normalized = runCatching { WorkflowSerializer.serialize(document) }
            .getOrElse { error ->
                return WorkspaceSyncGuardReport(
                    isValid = false,
                    messages = listOf("Workspace Serialisierung fehlgeschlagen: ${error.message ?: "unknown"}"),
                )
            }
        BlockEditorDropTrace.markActive(
            "SYNC_GUARD_NORMALIZE_RETURN",
            "bytes=${normalized.toByteArray(Charsets.UTF_8).size}",
        )
        if (normalized.isBlank()) {
            return WorkspaceSyncGuardReport(
                isValid = false,
                messages = listOf("Workspace Serialisierung ist leer."),
            )
        }
        BlockEditorDropTrace.markActive("SYNC_GUARD_EMSCRIPT_ENTER")
        val emscript = runCatching { EmscriptGenerator().generate(document) }
        BlockEditorDropTrace.markActive(
            "SYNC_GUARD_EMSCRIPT_RETURN",
            "success=${emscript.isSuccess} chars=${emscript.getOrNull()?.length ?: 0}",
        )
        if (emscript.isFailure) {
            messages += "EMScript-Projektion fehlgeschlagen: ${emscript.exceptionOrNull()?.message ?: "unknown"}"
        } else {
            messages += "EMScript-Projektion OK (${emscript.getOrDefault("").length} Zeichen)."
        }
        BlockEditorDropTrace.markActive("SYNC_GUARD_EMSCRIPT_REPARSE_ENTER")
        val emscriptReparse = emscript.getOrNull()?.let { generated -> EmscriptParserSlice().parse(generated) }
        BlockEditorDropTrace.markActive(
            "SYNC_GUARD_EMSCRIPT_REPARSE_RETURN",
            "success=${emscriptReparse?.isSuccess}",
        )
        when {
            emscriptReparse == null -> Unit
            emscriptReparse.isSuccess -> messages += "EMScript-Reparse OK."
            else -> messages += "EMScript-Reparse fehlgeschlagen: ${emscriptReparse.issues.joinToString { issue -> "${issue.line}:${issue.column} ${issue.message}" }}"
        }
        BlockEditorDropTrace.markActive("SYNC_GUARD_IR_ENTER")
        val irGraph = runCatching { IrGraphGenerator().generate(document) }
        BlockEditorDropTrace.markActive(
            "SYNC_GUARD_IR_RETURN",
            "success=${irGraph.isSuccess} nodes=${irGraph.getOrNull()?.nodes?.size ?: 0}",
        )
        BlockEditorDropTrace.markActive("SYNC_GUARD_IR_VALIDATION_ENTER")
        val irDiagnostics = irGraph
            .getOrNull()
            ?.let { graph -> graph.diagnostics + graph.validateIntegrity() + graph.validateSemantics() }
            .orEmpty()
        BlockEditorDropTrace.markActive(
            "SYNC_GUARD_IR_VALIDATION_RETURN",
            "diagnostics=${irDiagnostics.size}",
        )
        if (irGraph.isFailure) {
            messages += "IR-Graph-Erzeugung fehlgeschlagen: ${irGraph.exceptionOrNull()?.message ?: "unknown"}"
        } else {
            messages += "IR-Graph OK (${irGraph.getOrThrow().nodes.size} Nodes, ${irGraph.getOrThrow().edges.size} Kanten, ${irDiagnostics.size} Diagnosen)."
            irDiagnostics.take(3).forEach { diagnostic ->
                messages += "${diagnostic.code}: ${diagnostic.message}"
            }
        }
        BlockEditorDropTrace.markActive("SYNC_GUARD_FLOWCHART_ENTER")
        val flowchart = runCatching {
            com.visualtasker.wss.flowchart.IrGraphFlowchartProjector.project(irGraph.getOrThrow())
        }
        BlockEditorDropTrace.markActive(
            "SYNC_GUARD_FLOWCHART_RETURN",
            "success=${flowchart.isSuccess} nodes=${flowchart.getOrNull()?.graph?.nodes?.size ?: 0}",
        )
        if (flowchart.isFailure) {
            messages += "Flowchart-Projektion fehlgeschlagen: ${flowchart.exceptionOrNull()?.message ?: "unknown"}"
        } else {
            val graph = flowchart.getOrThrow().graph
            val errors = graph.diagnostics.count { it.severity == de.visualtasker.flowchart.domain.FlowDiagnosticSeverity.ERROR }
            messages += "Flowchart-Projektion OK (${graph.nodes.size} Nodes, ${graph.edges.size} Kanten, $errors Fehler)."
            graph.diagnostics.take(3).forEach { diagnostic ->
                messages += "${diagnostic.code}: ${diagnostic.message}"
            }
        }
        val missingDefinitions = document.blocks.values
            .map { it.type }
            .distinct()
            .filter { type ->
                de.visualtasker.blockeditor.registry.DefaultBlockRegistry.getDefinition(type) == null &&
                    !type.startsWith(de.visualtasker.blockeditor.registry.BlockTypes.VARIABLE_REPORTER_PREFIX)
            }
        if (missingDefinitions.isNotEmpty()) {
            messages += "Fehlende Blockdefinitionen: ${missingDefinitions.joinToString()}"
        }
        return WorkspaceSyncGuardReport(
            isValid = emscript.isSuccess &&
                (emscriptReparse?.isSuccess != false) &&
                irGraph.isSuccess &&
                irDiagnostics.isEmpty() &&
                flowchart.isSuccess &&
                missingDefinitions.isEmpty(),
            messages = messages,
        )
    }
}

data class WorkspaceSyncGuardReport(
    val isValid: Boolean,
    val messages: List<String>,
)
