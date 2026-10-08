package com.visualtasker.wss.emscript.apply

import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.WorkspaceOperation
import de.visualtasker.workflow.serialization.WorkflowSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmscriptSemanticSourceApplyTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun formattingOnlyApplyKeepsIdentityRevisionAndProducesNoOperations() {
        val source = "LET a = 1\nclick(\"Login\")"
        val before = success(source)
        val formatted = """
            // presentation only

            LET a = 1

            click("Login")
        """.trimIndent()

        val after = success(formatted, before.importedDocument)

        assertEquals(before.importedDocument.blocks.keys, after.importedDocument.blocks.keys)
        assertEquals(before.importedDocument.version, after.importedDocument.version)
        assertTrue(after.sourceApplyPlan.transaction.operations.isEmpty())
    }

    @Test
    fun explicitCommandChangeUsesSetPropertyAndPreservesUnrepresentedPayload() {
        val before = success("click(\"Login\")").importedDocument
        val command = before.blocks.values.single { it.type != BlockTypes.EVENT_START }
        val decorated = command.copy(
            fields = command.fields +
                ("displayLabel" to FieldValue.Text("Primary login")) +
                ("note" to FieldValue.Text("Keep this note")) +
                ("target.source" to FieldValue.Text("a11y")),
            collapsed = true,
            metadata = command.metadata + ("custom.owner" to "human"),
        )
        val edited = before.copy(blocks = before.blocks + (command.id to decorated))

        val applied = success("click(\"Continue\")", edited)
        val retained = applied.importedDocument.blocks.getValue(command.id)

        assertTrue(applied.sourceApplyPlan.transaction.operations.any { it is WorkspaceOperation.SetProperty })
        assertEquals(edited.version + 1, applied.importedDocument.version)
        assertEquals(FieldValue.Text("Primary login"), retained.fields["displayLabel"])
        assertEquals(FieldValue.Text("Keep this note"), retained.fields["note"])
        assertEquals(FieldValue.Text("a11y"), retained.fields["target.source"])
        assertEquals("human", retained.metadata["custom.owner"])
        assertTrue(retained.collapsed)
        assertTrue(applied.sourceApplyPlan.preservedPropertyCount >= 4)
    }

    @Test
    fun addingAndRemovingElseKeepsIfIdentityAndUsesStructuralOperations() {
        val withoutElse = """
            LET a = 1
            IF a > 0
                click("Login")
            END IF
        """.trimIndent()
        val withElse = """
            LET a = 1
            IF a > 0
                click("Login")
            ELSE
                screenshot("login.png")
            END IF
        """.trimIndent()
        val first = success(withoutElse)
        val firstIf = first.importedDocument.ifBlockId()

        val added = success(withElse, first.importedDocument)
        val removed = success(withoutElse, added.importedDocument)

        assertEquals(firstIf, added.importedDocument.ifBlockId())
        assertEquals(firstIf, removed.importedDocument.ifBlockId())
        assertTrue(added.sourceApplyPlan.transaction.operations.any { it is WorkspaceOperation.CreateEntity })
        assertTrue(removed.sourceApplyPlan.transaction.operations.any { it is WorkspaceOperation.DeleteEntity })
    }

    @Test
    fun elseIfBranchAndRelationsRemainStableAcrossFormattingRoundtrip() {
        val source = """
            LET score = 8
            IF score < 3
                log("low")
            ELSEIF score >= 7
                log("high")
            ELSE
                log("middle")
            END IF
        """.trimIndent()
        val first = success(source)
        val formatted = source.replace("ELSEIF", "\nELSEIF").replace("    log", "        log")

        val second = success(formatted, first.importedDocument)
        val firstCanonical = requireNotNull(first.importedDocument.canonical)
        val secondCanonical = requireNotNull(second.importedDocument.canonical)

        assertEquals(firstCanonical.entities.map { it.ref.id }.toSet(), secondCanonical.entities.map { it.ref.id }.toSet())
        assertEquals(firstCanonical.relations.map { it.id }.toSet(), secondCanonical.relations.map { it.id }.toSet())
    }

    @Test
    fun expressionValueReplacementKeepsExpressionIdentity() {
        val before = success("LET a = 1")
        val literalBefore = before.importedDocument.blocks.values.single { it.type == BlockTypes.LITERAL_NUMBER }

        val after = success("LET a = 2", before.importedDocument)
        val literalAfter = after.importedDocument.blocks.values.single { it.type == BlockTypes.LITERAL_NUMBER }

        assertEquals(literalBefore.id, literalAfter.id)
        assertEquals(FieldValue.Number(2.0), literalAfter.fields["value"])
        assertTrue(after.sourceApplyPlan.transaction.operations.any { it is WorkspaceOperation.SetProperty })
    }

    @Test
    fun variableSourceChangePreservesUnrepresentedScope() {
        val before = success("LET score = 1").importedDocument
        val variable = before.variables.variables.getValue("score")
        val edited = before.copy(
            variables = before.variables.copy(
                variables = before.variables.variables + ("score" to variable.copy(scope = VariableScope.Local)),
            ),
        )

        val after = success("LET score = 2", edited)
        val retained = after.importedDocument.variables.variables.getValue("score")

        assertEquals(VariableScope.Local, retained.scope)
        assertEquals("2", retained.defaultValue)
        assertTrue(after.sourceApplyPlan.propertyDecisions.any {
            it.propertyId == "scope" && it.disposition == SemanticSourcePropertyDisposition.NotRepresentedPreserved
        })
    }

    @Test
    fun insertionAndDeletionKeepUnaffectedStatementIdentities() {
        val original = "wait(1)\nbeep()\nwait(2)"
        val inserted = "wait(1)\nclick(\"middle\")\nbeep()\nwait(2)"
        val before = success(original)
        val stableIds = before.importedDocument.blocks.values
            .filter { it.type != BlockTypes.EVENT_START }
            .associate { block -> block.fields.toString() to block.id }

        val withInsertion = success(inserted, before.importedDocument)
        val afterDeletion = success(original, withInsertion.importedDocument)

        stableIds.forEach { (signature, id) ->
            assertEquals(id, withInsertion.importedDocument.blocks.values.single { it.fields.toString() == signature }.id)
            assertEquals(id, afterDeletion.importedDocument.blocks.values.single { it.fields.toString() == signature }.id)
        }
    }

    @Test
    fun repeatedElementCardinalityChangeReturnsAmbiguityDiagnostic() {
        val before = success("click(\"same\")\nclick(\"same\")").importedDocument

        val result = guard.preview(
            "click(\"same\")\nclick(\"same\")\nclick(\"same\")",
            previousDocument = before,
        )

        assertTrue(result is EmscriptApplyGuardResult.Failure)
        result as EmscriptApplyGuardResult.Failure
        assertEquals(EmscriptApplyGuardStage.IDENTITY_RECONCILE, result.stage)
        assertEquals("IDENTITY_RECONCILIATION_AMBIGUOUS", result.diagnosticCode)
    }

    @Test
    fun invalidApplyLeavesOriginalDocumentUntouched() {
        val before = success("LET a = 1").importedDocument

        val result = guard.preview("IF ???", previousDocument = before)

        assertTrue(result is EmscriptApplyGuardResult.Failure)
        assertEquals("1", before.variables.variables.getValue("a").defaultValue)
    }

    @Test
    fun textApplySurvivesSchemaTwoSaveReloadAndThreeEditorProjection() {
        val source = """
            LET a = 1
            IF a > 0
                click("Login")
            ELSE
                screenshot("login.png")
            END IF
        """.trimIndent()
        val applied = success(source)
        val reloaded = WorkflowSerializer.deserialize(WorkflowSerializer.serialize(applied.importedDocument))
        val state = WorkspaceWorkflowState.fromDocument(reloaded, "m3-2-test")
        val generated = EmscriptGenerator().generate(reloaded)
        val roundtrip = success(generated, reloaded)

        assertEquals(applied.importedDocument.blocks.keys, reloaded.blocks.keys)
        assertEquals(reloaded.blocks.keys, roundtrip.importedDocument.blocks.keys)
        assertFalse(state.flowchartProjection.graph.nodes.isEmpty())
        assertNotNull(state.emscriptProjection.getOrNull())
    }

    @Test
    fun automaticDraftGateSkipsRestartSnapshotButAllowsLaterEdit() {
        val gate = EmscriptAutomaticDraftApplyGate()

        assertFalse(gate.shouldApply("LET persisted = 1"))
        assertTrue(gate.shouldApply("LET persisted = 2"))
        assertFalse(gate.shouldApply(""))
    }

    private fun success(
        source: String,
        previous: de.visualtasker.workflow.core.WorkspaceDocument? = null,
    ): EmscriptApplyGuardResult.Success {
        val result = guard.preview(source, previousDocument = previous)
        assertTrue(result.toString(), result is EmscriptApplyGuardResult.Success)
        return result as EmscriptApplyGuardResult.Success
    }

    private fun de.visualtasker.workflow.core.WorkspaceDocument.ifBlockId() = blocks.values.single {
        it.type in setOf(BlockTypes.CONTROL_IF, BlockTypes.CONTROL_IF_ELSE, BlockTypes.CONTROL_IF_ELSEIF_ELSE)
    }.id
}
