package com.visualtasker.wss.emscript.parser

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.workspace.model.WorkspaceIdentityReconciler
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrGraphGenerator
import de.visualtasker.workflow.semantics.ir.IrGraph
import de.visualtasker.workflow.core.WorkspaceDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceAssemblerBulkTest {
    private val source = """
        LET count = 1
        LET limit = 4
        SET count = count + 1
        LOOP 2
            IF (count + 1) < limit
                log("low")
            ELSEIF count >= limit
                wait(20)
            ELSE
                beep(440, 50, 30)
            END IF
        END LOOP
        WHILE count < limit
            SET count = count + 1
        END WHILE
    """.trimIndent()

    @Test
    fun bulkAssemblyIsSemanticallyEquivalentToSequentialReducerOracle() {
        val legacy = importWith(WorkspaceAssemblyMode.LEGACY_SEQUENTIAL)
        val bulk = importWith(WorkspaceAssemblyMode.BULK)
        val reconciledBulk = WorkspaceIdentityReconciler.reconcile(legacy.document, bulk.document!!)

        assertSemanticDocumentsEqual(legacy.document!!, reconciledBulk)
        assertEquals(
            EmscriptGenerator().generate(legacy.document),
            EmscriptGenerator().generate(reconciledBulk),
        )
        assertEquals(
            normalizedGraph(IrGraphGenerator().generate(legacy.document)),
            normalizedGraph(IrGraphGenerator().generate(reconciledBulk)),
        )
    }

    @Test
    fun bulkAssemblyPublishesOnceWithoutInteractiveReducerActions() {
        val legacy = importWith(WorkspaceAssemblyMode.LEGACY_SEQUENTIAL)
        val bulk = importWith(WorkspaceAssemblyMode.BULK)

        assertTrue(legacy.assemblyMetrics!!.reducerCalls > 0)
        assertEquals(0, bulk.assemblyMetrics!!.reducerCalls)
        assertTrue(bulk.assemblyMetrics.bulkObjectsConstructed > 0)
        assertEquals(1, bulk.assemblyMetrics.publicationCount)
        assertEquals(legacy.assemblyMetrics.actionCounts, bulk.assemblyMetrics.actionCounts)
    }

    @Test
    fun bulkAssemblyMatchesSequentialOracleForLargeNestedWorkflow() {
        assertEquivalent(EditorDefaults.integrationTestScript, "bulk-large-equivalence")
    }

    private fun importWith(mode: WorkspaceAssemblyMode): EmscriptImportResult =
        EmscriptWorkspaceImporter(assemblyMode = mode)
            .import(source, workspaceId = "bulk-equivalence")
            .also { result ->
                assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
                assertNotNull(result.document)
                assertNotNull(result.assemblyMetrics)
            }

    private fun semanticDocument(document: WorkspaceDocument): WorkspaceDocument =
        document.copy(
            version = 0L,
            canonical = document.canonical?.copy(revision = 0L),
        )

    private fun normalizedGraph(graph: IrGraph): IrGraph = graph.copy(
        sourceRevision = "normalized",
        nodes = graph.nodes.map { it.copy(source = it.source.copy(workspaceVersion = 0L)) },
        edges = graph.edges.map { it.copy(source = it.source.copy(workspaceVersion = 0L)) },
        diagnostics = graph.diagnostics.map { it.copy(source = it.source.copy(workspaceVersion = 0L)) },
        scopes = graph.scopes.map { it.copy(source = it.source.copy(workspaceVersion = 0L)) },
        branches = graph.branches.map { it.copy(source = it.source.copy(workspaceVersion = 0L)) },
        facets = graph.facets.map { it.copy(source = it.source.copy(workspaceVersion = 0L)) },
    )

    private fun assertEquivalent(script: String, workspaceId: String) {
        val legacy = EmscriptWorkspaceImporter(assemblyMode = WorkspaceAssemblyMode.LEGACY_SEQUENTIAL)
            .import(script, workspaceId)
        val bulk = EmscriptWorkspaceImporter(assemblyMode = WorkspaceAssemblyMode.BULK)
            .import(script, workspaceId)
        assertTrue(legacy.issues.joinToString { it.message }, legacy.isSuccess)
        assertTrue(bulk.issues.joinToString { it.message }, bulk.isSuccess)

        val reconciled = WorkspaceIdentityReconciler.reconcile(legacy.document, bulk.document!!)
        assertSemanticDocumentsEqual(legacy.document!!, reconciled)
        assertEquals(legacy.assemblyMetrics!!.actionCounts, bulk.assemblyMetrics!!.actionCounts)
    }

    private fun assertSemanticDocumentsEqual(
        expected: WorkspaceDocument,
        actual: WorkspaceDocument,
    ) {
        val normalizedExpected = semanticDocument(expected)
        val normalizedActual = semanticDocument(actual)
        val blockDifferences = (normalizedExpected.blocks.keys + normalizedActual.blocks.keys)
            .filter { normalizedExpected.blocks[it] != normalizedActual.blocks[it] }
            .take(12)
            .joinToString { blockId ->
                val expectedBlock = normalizedExpected.blocks[blockId]
                val actualBlock = normalizedActual.blocks[blockId]
                "${blockId.value}:${expectedBlock?.type ?: "missing"} expectedFields=${expectedBlock?.fields} actualFields=${actualBlock?.fields}"
            }
        assertEquals("workspace id", normalizedExpected.id, normalizedActual.id)
        assertEquals("block payload and compatibility projection differences=[$blockDifferences]", normalizedExpected.blocks, normalizedActual.blocks)
        assertEquals("root compatibility projection", normalizedExpected.rootBlocks, normalizedActual.rootBlocks)
        assertEquals("root positions", normalizedExpected.rootPositions, normalizedActual.rootPositions)
        assertEquals("variables", normalizedExpected.variables, normalizedActual.variables)
        assertEquals("canonical workflow", normalizedExpected.canonical, normalizedActual.canonical)
    }
}
