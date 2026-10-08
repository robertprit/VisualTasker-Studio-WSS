package com.visualtasker.wss.emscript.apply

import com.visualtasker.wss.emscript.parser.EmscriptParseIssue
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.workspace.model.WorkspaceIdentityReconciler
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.core.CanonicalWorkspaceMigration
import de.visualtasker.workflow.core.WorkspaceOperationResult
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockRegistry
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.workflow.serialization.WorkflowSerializer
import de.visualtasker.workflow.semantics.validation.Validator

class EmscriptApplyGuard(
    private val importer: EmscriptWorkspaceImporter = EmscriptWorkspaceImporter(),
) {
    fun preview(
        draft: String,
        workspaceId: String = "workflow-main",
        registry: BlockRegistry? = null,
        previousDocument: WorkspaceDocument? = null,
    ): EmscriptApplyGuardResult {
        val importResult = importer.import(draft, workspaceId = workspaceId)
        if (!importResult.isSuccess || importResult.document == null) {
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.PARSE_IMPORT,
                message = importResult.firstIssueMessage() ?: "Parse/Import fehlgeschlagen",
            )
        }

        val reconciliation = WorkspaceIdentityReconciler.reconcileWithDiagnostics(previousDocument, importResult.document)
        if (!reconciliation.isUnambiguous) {
            val diagnostic = reconciliation.diagnostics.first()
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.IDENTITY_RECONCILE,
                message = diagnostic.message,
                diagnosticCode = diagnostic.code,
            )
        }
        val migration = CanonicalWorkspaceMigration.toCurrent(reconciliation.document)
        if (!migration.isValid) {
            val issue = migration.issues.firstOrNull()
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.PRE_VALIDATE,
                message = "Canonical migration failed: ${issue?.message ?: "unknown"}",
                diagnosticCode = issue?.code,
            )
        }
        val candidate = migration.document
        val candidateRegistry = registry ?: candidate.registryWithVariables()
        val candidateValidation = Validator.validate(candidate, candidateRegistry)
        if (!candidateValidation.isValid) {
            val diagnostic = candidateValidation.errors.first()
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.PRE_VALIDATE,
                message = "Pre-Validate fehlgeschlagen: ${diagnostic.message}",
                diagnosticCode = diagnostic.code,
            )
        }
        val plan = runCatching {
            EmscriptSemanticSourceApply.plan(previousDocument, candidate)
        }.getOrElse { error ->
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.APPLY_TRANSACTION,
                message = "Semantic Source Apply Plan fehlgeschlagen: ${error.message ?: "unknown"}",
            )
        }
        val applyResult = EmscriptSemanticSourceApply.execute(plan)
        val imported = when (applyResult) {
            is WorkspaceOperationResult.Success -> applyResult.document
            is WorkspaceOperationResult.Failure -> return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.APPLY_TRANSACTION,
                message = "Semantic Source Apply abgelehnt: ${applyResult.diagnostic.message}",
                diagnosticCode = applyResult.diagnostic.code.name,
            )
        }
        val effectiveRegistry = registry ?: imported.registryWithVariables()
        val validation = if (registry != null) {
            Validator.validate(imported, registry)
        } else {
            Validator.validate(imported, effectiveRegistry)
        }
        if (!validation.isValid) {
            val diagnostic = validation.errors.first()
            return EmscriptApplyGuardResult.Failure(
                stage = EmscriptApplyGuardStage.PRE_VALIDATE,
                message = "Pre-Validate fehlgeschlagen: ${diagnostic.message}",
                diagnosticCode = diagnostic.code,
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
            serializedWorkspaceJson = WorkflowSerializer.serialize(imported),
            blockCount = imported.blocks.size,
            rootCount = imported.rootBlocks.size,
            variableCount = imported.variables.variables.size,
            unsupportedCount = unsupportedCount,
            roundtripLength = roundtrip.length,
            sourceApplyPlan = plan,
        )
    }
}

enum class EmscriptApplyGuardStage {
    PARSE_IMPORT,
    IDENTITY_RECONCILE,
    PRE_VALIDATE,
    APPLY_TRANSACTION,
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
        val sourceApplyPlan: SemanticSourceApplyPlan,
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
                append("Semantic Apply: ${sourceApplyPlan.transaction.operations.size} Operationen, " +
                    "${sourceApplyPlan.preservedPropertyCount} nicht dargestellte Properties erhalten.")
            }
    }

    data class Failure(
        val stage: EmscriptApplyGuardStage,
        val message: String,
        val diagnosticCode: String? = null,
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
