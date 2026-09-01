package com.visualtasker.wss.emscript.apply

import com.visualtasker.wss.emscript.parser.EmscriptParseIssue
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.domain.WorkspaceDocument
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockRegistry
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.blockeditor.validation.Validator

class EmscriptApplyGuard(
    private val importer: EmscriptWorkspaceImporter = EmscriptWorkspaceImporter(),
) {
    fun preview(
        draft: String,
        workspaceId: String = "workflow-main",
        registry: BlockRegistry? = null,
    ): EmscriptApplyGuardResult {
        val importResult = importer.import(draft, workspaceId = workspaceId)
        if (!importResult.isSuccess || importResult.document == null) {
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.PARSE_IMPORT,
                message = importResult.firstIssueMessage() ?: "Parse/Import fehlgeschlagen",
            )
        }

        val imported = importResult.document
        val effectiveRegistry = registry ?: imported.registryWithVariables()
        val validation = if (registry != null) {
            Validator.validate(imported, registry)
        } else {
            Validator.validate(imported, effectiveRegistry)
        }
        if (!validation.isValid) {
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.PRE_VALIDATE,
                message = "Pre-Validate fehlgeschlagen: ${validation.errors.first().message}",
            )
        }

        val roundtrip = runCatching {
            EmscriptGenerator(IrGenerator(effectiveRegistry)).generate(imported)
        }.getOrElse { error ->
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.ROUNDTRIP,
                message = "Roundtrip-Guard fehlgeschlagen: ${error.message ?: "unknown"}",
            )
        }

        val unsupportedCount = importResult.issues.count { it.message.contains("unsupported", ignoreCase = true) } +
            (registry?.let { blockRegistry ->
                imported.blocks.values.count { blockRegistry.getDefinition(it.type) == null }
            } ?: imported.blocks.values.count { effectiveRegistry.getDefinition(it.type) == null })

        return EmscriptApplyGuardResult.Success(
            importedDocument = imported,
            serializedWorkspaceJson = WorkspaceSerializer.serialize(imported),
            blockCount = imported.blocks.size,
            rootCount = imported.rootBlocks.size,
            variableCount = imported.variables.variables.size,
            unsupportedCount = unsupportedCount,
            roundtripLength = roundtrip.length,
        )
    }
}

enum class EmscriptApplyGuardStage {
    PARSE_IMPORT,
    PRE_VALIDATE,
    ROUNDTRIP,
}

sealed interface EmscriptApplyGuardResult {
    data class Success(
        val importedDocument: WorkspaceDocument,
        val serializedWorkspaceJson: String,
        val blockCount: Int,
        val rootCount: Int,
        val variableCount: Int,
        val unsupportedCount: Int,
        val roundtripLength: Int,
    ) : EmscriptApplyGuardResult {
        val summary: String
            get() = buildString {
                appendLine("Draft -> Parse -> Import -> Validate: OK")
                appendLine()
                appendLine("Blöcke")
                appendLine("  Ziel-Workspace: $blockCount Blöcke")
                appendLine("  Roots: $rootCount")
                appendLine()
                appendLine("Variablen")
                appendLine("  Ziel-Workspace: $variableCount")
                appendLine()
                appendLine("Nicht unterstützte Konstrukte: $unsupportedCount")
                appendLine("Roundtrip-Script-Länge: $roundtripLength")
                append("Hinweis: Apply ersetzt das Workspace-Dokument nach Guard-Prüfung.")
            }
    }

    data class Failure(
        val stage: EmscriptApplyGuardStage,
        val message: String,
    ) : EmscriptApplyGuardResult
}

private fun com.visualtasker.wss.emscript.parser.EmscriptImportResult.firstIssueMessage(): String? {
    val firstIssue = issues.firstOrNull() ?: return null
    return firstIssue.asApplyMessage()
}

private fun EmscriptParseIssue.asApplyMessage(): String =
    "Parse/Import Fehler $line:$column $message"

private fun WorkspaceDocument.registryWithVariables(): BlockRegistry =
    CompositeBlockRegistry().apply {
        variables.variables.values.forEach { variable ->
            register(VariableReporterFactory.create(variable))
        }
    }
