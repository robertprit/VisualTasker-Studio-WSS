package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.serialization.WorkspaceDecodeResult
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer

class WorkspaceSyncGuard {
    fun inspect(serializedJson: String): WorkspaceSyncGuardReport {
        val decoded = WorkspaceSerializer.decode(serializedJson)
        val document = when (decoded) {
            is WorkspaceDecodeResult.Decoded -> decoded.document
            is WorkspaceDecodeResult.Malformed -> return WorkspaceSyncGuardReport(
                isValid = false,
                messages = listOf("Workspace JSON ist fehlerhaft: ${decoded.reason}"),
            )
            is WorkspaceDecodeResult.UnsupportedSchema -> return WorkspaceSyncGuardReport(
                isValid = false,
                messages = listOf("Workspace Schema wird nicht unterstützt: ${decoded.version}"),
            )
        }
        val messages = mutableListOf<String>()
        val normalized = runCatching { WorkspaceSerializer.serialize(document) }
            .getOrElse { error ->
                return WorkspaceSyncGuardReport(
                    isValid = false,
                    messages = listOf("Workspace Serialisierung fehlgeschlagen: ${error.message ?: "unknown"}"),
                )
            }
        if (normalized.isBlank()) {
            return WorkspaceSyncGuardReport(
                isValid = false,
                messages = listOf("Workspace Serialisierung ist leer."),
            )
        }
        val emscript = runCatching { EmscriptGenerator().generate(document) }
        if (emscript.isFailure) {
            messages += "EMScript-Projektion fehlgeschlagen: ${emscript.exceptionOrNull()?.message ?: "unknown"}"
        } else {
            messages += "EMScript-Projektion OK (${emscript.getOrDefault("").length} Zeichen)."
        }
        val flowchart = runCatching { com.visualtasker.wss.flowchart.BlockEditorFlowchartProjector.project(document) }
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
            isValid = emscript.isSuccess && flowchart.isSuccess && missingDefinitions.isEmpty(),
            messages = messages,
        )
    }
}

data class WorkspaceSyncGuardReport(
    val isValid: Boolean,
    val messages: List<String>,
)
