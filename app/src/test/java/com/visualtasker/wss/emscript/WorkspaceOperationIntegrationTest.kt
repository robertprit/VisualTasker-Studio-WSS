package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.workflow.core.SemanticPropertyId
import de.visualtasker.workflow.core.SemanticRelationKind
import de.visualtasker.workflow.core.SemanticRelationRoleKind
import de.visualtasker.workflow.core.WorkspaceOperation
import de.visualtasker.workflow.core.WorkspaceOperationExecutor
import de.visualtasker.workflow.core.WorkspaceOperationId
import de.visualtasker.workflow.core.WorkspaceOperationResult
import de.visualtasker.workflow.core.WorkspacePropertyValue
import de.visualtasker.workflow.core.WorkspaceTransaction
import de.visualtasker.workflow.semantics.WorkflowElementTypes
import de.visualtasker.workflow.semantics.ir.IrGraphGenerator
import de.visualtasker.workflow.serialization.WorkflowSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceOperationIntegrationTest {
    @Test
    fun semanticFixtureSurvivesPropertyStructurePersistenceAndIrRoundtrip() {
        val source = """
            LET x = 1 + 2
            IF x > 2
                click("Login")
            ELSE
                wait(500)
            END IF
        """.trimIndent()
        val imported = EmscriptWorkspaceImporter().import(source, workspaceId = "m3-operation-integration")
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val document = requireNotNull(imported.document)
        val click = document.blocks.values.single { it.type == WorkflowElementTypes.ACTION_CLICK_TEXT }
        val wait = document.blocks.values.single { it.type == WorkflowElementTypes.ACTION_WAIT }
        val clickKey = click.fields.entries.single { (_, value) -> value == FieldValue.Text("Login") }.key
        val waitKey = wait.fields.entries.single { (_, value) -> value == FieldValue.Number(500.0) }.key
        val canonical = requireNotNull(document.canonical)
        val clickEntity = canonical.entities.single { it.legacySourceId == click.id.value }.ref.id
        val waitEntity = canonical.entities.single { it.legacySourceId == wait.id.value }.ref.id

        val propertyResult = execute(
            document,
            WorkspaceTransaction(
                transactionId = id("properties"),
                operations = listOf(
                    WorkspaceOperation.SetProperty(id("click-text"), clickEntity, SemanticPropertyId(clickKey), WorkspacePropertyValue.Text("Continue")),
                    WorkspaceOperation.SetProperty(id("wait-duration"), waitEntity, SemanticPropertyId(waitKey), WorkspacePropertyValue.Number(750.0)),
                ),
            ),
        )

        val expression = propertyResult.document.canonical!!.relations.first { it.kind == SemanticRelationKind.Expression }
        val detachedExpression = execute(
            propertyResult.document,
            WorkspaceTransaction(id("detach-expression"), listOf(WorkspaceOperation.RemoveRelation(id("remove-expression"), expression.id))),
        )
        val restoredExpression = execute(
            detachedExpression.document,
            WorkspaceTransaction(id("restore-expression"), listOf(WorkspaceOperation.CreateRelation(id("create-expression"), expression))),
        )

        val sequence = restoredExpression.document.canonical!!.relations.first { it.kind == SemanticRelationKind.Sequence }
        val detachedStatement = execute(
            restoredExpression.document,
            WorkspaceTransaction(id("detach-statement"), listOf(WorkspaceOperation.RemoveRelation(id("remove-statement"), sequence.id))),
        )
        val restoredStatement = execute(
            detachedStatement.document,
            WorkspaceTransaction(id("restore-statement"), listOf(WorkspaceOperation.CreateRelation(id("create-statement"), sequence))),
        )

        val branches = restoredStatement.document.canonical!!.relations
            .filter { it.kind == SemanticRelationKind.Containment && it.role.kind == SemanticRelationRoleKind.Branch }
            .sortedBy { it.order }
        val reordered = if (branches.size >= 2) {
            execute(
                restoredStatement.document,
                WorkspaceTransaction(
                    transactionId = id("branch-order"),
                    operations = listOf(
                        WorkspaceOperation.SetOrder(id("branch-first"), branches[0].id, 1),
                        WorkspaceOperation.SetOrder(id("branch-second"), branches[1].id, 0),
                    ),
                ),
            )
        } else {
            restoredStatement
        }

        val serialized = WorkflowSerializer.serialize(reordered.document)
        val reloaded = WorkflowSerializer.deserialize(serialized)
        val ir = IrGraphGenerator().generate(reloaded)
        val generated = EmscriptGenerator().generate(reloaded)

        assertEquals(reordered.document.canonical, reloaded.canonical)
        assertEquals(reordered.document.blocks, reloaded.blocks)
        assertTrue(ir.nodes.isNotEmpty())
        assertTrue(generated.contains("Continue"))
        assertTrue(generated.contains("750"))
        assertEquals(FieldValue.Text("Continue"), reloaded.blocks.getValue(click.id).fields[clickKey])
        assertEquals(FieldValue.Number(750.0), reloaded.blocks.getValue(wait.id).fields[waitKey])
    }

    private fun execute(
        document: de.visualtasker.workflow.core.WorkspaceDocument,
        transaction: WorkspaceTransaction,
    ): WorkspaceOperationResult.Success =
        WorkspaceOperationExecutor.execute(document, transaction) as WorkspaceOperationResult.Success

    private fun id(value: String) = WorkspaceOperationId(value)
}
