package com.visualtasker.wss.emscript.apply

import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.SemanticEntityKind
import de.visualtasker.workflow.core.SemanticEntityId
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.WorkspaceOperation
import de.visualtasker.workflow.serialization.WorkflowSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

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
    fun persistentAnchorsDisambiguateDeletionAndInsertionOfIdenticalStatements() {
        val before = success("click(\"same\")\nclick(\"same\")").importedDocument
        val originalStatements = before.statementEntities()
        val first = originalStatements[0]
        val second = originalStatements[1]

        val afterDeletion = success(
            anchoredStatement(first.ref.id, "click(\"same\")"),
            before,
        ).importedDocument
        assertEquals(setOf(first.ref.id), afterDeletion.statementEntities().map { it.ref.id }.toSet())

        val afterInsertion = success(
            listOf(
                anchoredStatement(first.ref.id, "click(\"same\")"),
                anchoredStatement(second.ref.id, "click(\"same\")"),
                "click(\"same\")",
            ).joinToString("\n"),
            before,
        ).importedDocument
        val insertedIds = afterInsertion.statementEntities().map { it.ref.id }.toSet()
        assertTrue(first.ref.id in insertedIds)
        assertTrue(second.ref.id in insertedIds)
        assertEquals(3, insertedIds.size)
    }

    @Test
    fun stalePersistentAnchorFailsInsteadOfGuessing() {
        val before = success("click(\"same\")").importedDocument

        val result = guard.preview(
            anchoredStatement(SemanticEntityId("entity:deleted"), "click(\"same\")"),
            previousDocument = before,
        )

        assertTrue(result is EmscriptApplyGuardResult.Failure)
        result as EmscriptApplyGuardResult.Failure
        assertEquals(EmscriptApplyGuardStage.IDENTITY_RECONCILE, result.stage)
        assertEquals("SOURCE_ANCHOR_TARGET_MISSING", result.diagnosticCode)
    }

    @Test
    fun persistentAnchorsSurviveFormattingSaveReloadAndBranchRoundtrip() {
        val initial = success(
            """
                LET a = 1
                IF a > 0
                    click("Login")
                ELSE
                    screenshot("login.png")
                END IF
            """.trimIndent(),
        ).importedDocument
        val generated = EmscriptGenerator().generate(initial)
        val formatted = "// formatting only\n\n" + generated.replace(";\n", ";\n\n")
        val reloaded = WorkflowSerializer.deserialize(WorkflowSerializer.serialize(initial))

        val reapplied = success(formatted, reloaded).importedDocument

        assertEquals(
            requireNotNull(initial.canonical).entities.map { it.ref.id }.toSet(),
            requireNotNull(reapplied.canonical).entities.map { it.ref.id }.toSet(),
        )
        assertEquals(
            requireNotNull(initial.canonical).relations.map { it.id }.toSet(),
            requireNotNull(reapplied.canonical).relations.map { it.id }.toSet(),
        )
    }

    @Test
    fun repeatedExpressionsKeepEntityIdentityWhenStatementsAreReordered() {
        val before = success("LET a = 1 + 1\nLET b = 1 + 1").importedDocument
        val anchored = EmscriptGenerator().generate(before)
        val bundles = anchored.statementBundles()
        assertEquals(2, bundles.size)
        val relationPrelude = anchored.lineSequence()
            .filter { it.startsWith("@source.relation") }
            .joinToString("\n")

        val reorderedSource = listOf(relationPrelude, bundles[1], bundles[0])
            .filter(String::isNotBlank)
            .joinToString("\n")
        val reordered = success(reorderedSource, before).importedDocument

        assertEquals(
            requireNotNull(before.canonical).entities.map { it.ref.id }.toSet(),
            requireNotNull(reordered.canonical).entities.map { it.ref.id }.toSet(),
        )
        assertEquals(before.blocks.keys, reordered.blocks.keys)
    }

    @Test
    fun sourceApplyPerformanceComparisonIsReproducibleForLargeWorkspace() {
        val statementCount = 80
        val legacySource = (1..statementCount).joinToString("\n") { index -> "wait($index)" }
        val before = success(legacySource).importedDocument
        val anchoredSource = EmscriptGenerator().generate(before)

        guard.preview(legacySource, previousDocument = before)
        guard.preview(anchoredSource, previousDocument = before)
        var legacyResult: EmscriptApplyGuardResult? = null
        var anchoredResult: EmscriptApplyGuardResult? = null
        val legacyNanos = measureNanoTime {
            legacyResult = guard.preview(legacySource, previousDocument = before)
        }
        val anchoredNanos = measureNanoTime {
            anchoredResult = guard.preview(anchoredSource, previousDocument = before)
        }

        assertTrue(legacyResult.toString(), legacyResult is EmscriptApplyGuardResult.Success)
        assertTrue(anchoredResult.toString(), anchoredResult is EmscriptApplyGuardResult.Success)
        assertEquals(
            before.blocks.keys,
            (anchoredResult as EmscriptApplyGuardResult.Success).importedDocument.blocks.keys,
        )
        println(
            "M3-3 source-apply comparison statements=$statementCount " +
                "legacyMs=${legacyNanos / 1_000_000.0} anchoredMs=${anchoredNanos / 1_000_000.0}",
        )
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

    private fun de.visualtasker.workflow.core.WorkspaceDocument.statementEntities() =
        requireNotNull(canonical).entities
            .filter { it.ref.kind == SemanticEntityKind.Statement }
            .filterNot { entity ->
                entity.legacySourceId
                    ?.let { de.visualtasker.workflow.core.BlockId(it) }
                    ?.let(blocks::get)
                    ?.type == BlockTypes.EVENT_START
            }
            .sortedBy { it.legacySourceId }

    private fun anchoredStatement(entityId: SemanticEntityId, statement: String): String =
        "@source.entity(\"block\", \"${entityId.value}\", \"Statement\")\n$statement"

    private fun String.statementBundles(): List<String> {
        val bundles = mutableListOf<String>()
        val pending = mutableListOf<String>()
        lineSequence().forEach { line ->
            when {
                line.startsWith("@source.relation") || line.isBlank() -> Unit
                line.startsWith("@source.entity") -> pending += line
                else -> {
                    pending += line
                    bundles += pending.joinToString("\n")
                    pending.clear()
                }
            }
        }
        check(pending.isEmpty()) { "Incomplete source-anchor statement bundle." }
        return bundles
    }
}
