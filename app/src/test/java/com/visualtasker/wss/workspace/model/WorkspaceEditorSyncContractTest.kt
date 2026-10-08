package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.WorkspacePoint
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.flowchart.domain.FlowNodeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceEditorSyncContractTest {
    private val script = """
        LET a = 1
        IF a > 0
            click("Login")
        ELSE
            screenshot("login.png")
        END IF
    """.trimIndent()

    @Test
    fun textWorkflowBlockFlowTextKeepsSemanticIdentity() {
        val guard = EmscriptApplyGuard()
        val first = guard.preview(script) as EmscriptApplyGuardResult.Success
        val second = guard.preview(script, previousDocument = first.importedDocument) as EmscriptApplyGuardResult.Success
        val document = second.importedDocument
        val sourceLines = WorkspaceSelectionResolver.sourceLines(document, script)
        val ifBlock = document.blocks.values.single {
            it.type in setOf(BlockTypes.CONTROL_IF, BlockTypes.CONTROL_IF_ELSE, BlockTypes.CONTROL_IF_ELSEIF_ELSE)
        }

        assertEquals(first.importedDocument.blocks.keys, document.blocks.keys)
        assertEquals(ifBlock.id, WorkspaceSelectionResolver.blockAtSourceLine(document, 2, sourceLines))
        assertEquals(2, WorkspaceSelectionResolver.sourceLine(document, ifBlock.id, sourceLines))

        val flowNodeId = ifBlock.id.toFlowNodeId()
        val workflow = WorkspaceWorkflowState.fromDocument(document, "sync-contract")
        assertTrue(workflow.flowchartProjection.graph.nodes.any { it.id == flowNodeId })
        assertEquals(ifBlock.id, flowNodeId.toBlockIdOrNull())
    }

    @Test
    fun selectionReconciliationRestoresUndoRedoTargetWithoutInventingIds() {
        val guard = EmscriptApplyGuard()
        val before = (guard.preview(script) as EmscriptApplyGuardResult.Success).importedDocument
        val ifBlock = before.blocks.values.single {
            it.type in setOf(BlockTypes.CONTROL_IF, BlockTypes.CONTROL_IF_ELSE, BlockTypes.CONTROL_IF_ELSEIF_ELSE)
        }
        val historyEntry = WorkspaceHistoryEntry(
            serializedJson = "fixture",
            selection = WorkspaceSelectionState().selectBlock(ifBlock.id, sourceLine = 2),
        )
        val restored = (guard.preview(script, previousDocument = before) as EmscriptApplyGuardResult.Success).importedDocument

        val reconciled = WorkspaceSelectionResolver.reconcile(restored, historyEntry.selection)

        assertEquals(ifBlock.id, reconciled.blockId)
        assertEquals(FlowNodeId("block:${ifBlock.id.value}"), reconciled.flowNodeId)
        assertEquals(2, reconciled.sourceLine)
    }

    @Test
    fun fieldChangeKeepsSemanticIdsAndSelectionAcrossAllProjections() {
        val guard = EmscriptApplyGuard()
        val before = (guard.preview(script) as EmscriptApplyGuardResult.Success).importedDocument
        val selectedBefore = before.blocks.values.single {
            it.type in setOf(BlockTypes.CONTROL_IF, BlockTypes.CONTROL_IF_ELSE, BlockTypes.CONTROL_IF_ELSEIF_ELSE)
        }
        val changedScript = script.replace("LET a = 1", "LET a = 2")

        val changed = (
            guard.preview(changedScript, previousDocument = before) as EmscriptApplyGuardResult.Success
        ).importedDocument
        val selection = WorkspaceSelectionResolver.reconcile(
            changed,
            WorkspaceSelectionState().selectBlock(selectedBefore.id, sourceLine = 2),
        )
        val workflow = WorkspaceWorkflowState.fromDocument(changed, "sync-contract:field-change")

        assertEquals(before.blocks.keys, changed.blocks.keys)
        assertEquals(selectedBefore.id, selection.blockId)
        assertEquals(selectedBefore.id.toFlowNodeId(), selection.flowNodeId)
        assertEquals(2, selection.sourceLine)
        assertTrue(workflow.flowchartProjection.graph.nodes.any { it.id == selection.flowNodeId })
        assertEquals(
            selectedBefore.id,
            WorkspaceSelectionResolver.blockAtSourceLine(
                changed,
                selection.sourceLine!!,
                WorkspaceSelectionResolver.sourceLines(changed, changedScript),
            ),
        )
    }

    @Test
    fun projectionSelectionDirectionsResolveToOneSemanticIdentity() {
        val document = (EmscriptApplyGuard().preview(script) as EmscriptApplyGuardResult.Success).importedDocument
        val sourceLines = WorkspaceSelectionResolver.sourceLines(document, script)
        val blockId = WorkspaceSelectionResolver.blockAtSourceLine(document, 2, sourceLines)!!

        val fromText = WorkspaceSelectionState(sourceLine = 2, source = "texteditor")
            .let { WorkspaceSelectionResolver.reconcile(document, it) }
        val fromBlock = WorkspaceSelectionState().selectBlock(blockId, sourceLine = 2)
        val fromFlow = WorkspaceSelectionState().selectFlowNode(blockId.toFlowNodeId(), sourceLine = 2)

        assertEquals(blockId, fromText.blockId)
        assertEquals(fromText.blockId, fromBlock.blockId)
        assertEquals(fromBlock.blockId, fromFlow.blockId)
        assertEquals(fromText.flowNodeId, fromBlock.flowNodeId)
        assertEquals(fromBlock.flowNodeId, fromFlow.flowNodeId)
    }

    @Test
    fun missingSelectionIsClearedInsteadOfMappedByRenderOrder() {
        val document = (EmscriptApplyGuard().preview(script) as EmscriptApplyGuardResult.Success).importedDocument

        val reconciled = WorkspaceSelectionResolver.reconcile(
            document,
            WorkspaceSelectionState(blockId = BlockId("missing"), flowNodeId = FlowNodeId("block:missing")),
        )

        assertNull(reconciled.blockId)
        assertNull(reconciled.flowNodeId)
    }

    @Test
    fun selectionIsClearedWhenItsSemanticElementWasRemoved() {
        val guard = EmscriptApplyGuard()
        val before = (guard.preview(script) as EmscriptApplyGuardResult.Success).importedDocument
        val screenshotBlock = before.blocks.values.single {
            it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}vision.screenshot"
        }
        val withoutElse = """
            LET a = 1
            IF a > 0
                click("Login")
            END IF
        """.trimIndent()
        val applyResult = guard.preview(withoutElse, previousDocument = before)
        assertTrue(applyResult.toString(), applyResult is EmscriptApplyGuardResult.Success)
        val after = (applyResult as EmscriptApplyGuardResult.Success).importedDocument

        val reconciled = WorkspaceSelectionResolver.reconcile(
            after,
            WorkspaceSelectionState().selectBlock(screenshotBlock.id),
        )

        assertNull(reconciled.blockId)
        assertNull(reconciled.flowNodeId)
    }

    @Test
    fun invalidDraftCannotReplaceCanonicalWorkflow() {
        val valid = EmscriptApplyGuard().preview(script) as EmscriptApplyGuardResult.Success
        val invalid = EmscriptApplyGuard().preview("IF ???", previousDocument = valid.importedDocument)

        assertFalse(invalid is EmscriptApplyGuardResult.Success)
        assertTrue(valid.importedDocument.blocks.isNotEmpty())
    }

    @Test
    fun sourceMappingCacheReusesExactSourceAcrossPresentationOnlyChanges() {
        val document = (EmscriptApplyGuard().preview(script) as EmscriptApplyGuardResult.Success).importedDocument
        val cache = WorkspaceSelectionResolver.SourceMappingCache()

        val first = WorkspaceSelectionResolver.sourceLines(document, script, cache = cache)
        val moved = document.copy(
            version = document.version + 1,
            rootPositions = document.rootPositions +
                (document.rootBlocks.first() to WorkspacePoint(900f, 700f)),
        )
        val second = WorkspaceSelectionResolver.sourceLines(moved, script, cache = cache)

        assertEquals(first, second)
        assertEquals(1, cache.misses)
        assertEquals(1, cache.hits)
    }

    @Test
    fun sourceMappingCacheInvalidatesWhenExactSourceChanges() {
        val document = (EmscriptApplyGuard().preview(script) as EmscriptApplyGuardResult.Success).importedDocument
        val cache = WorkspaceSelectionResolver.SourceMappingCache()

        WorkspaceSelectionResolver.sourceLines(document, script, cache = cache)
        WorkspaceSelectionResolver.sourceLines(
            document,
            script.replace("click(\"Login\")", "click(\"Continue\")"),
            cache = cache,
        )

        assertEquals(2, cache.misses)
        assertEquals(0, cache.hits)
    }

    @Test
    fun projectedAndDraftMappingShareOneDerivedImportForSameSource() {
        val document = (EmscriptApplyGuard().preview(script) as EmscriptApplyGuardResult.Success).importedDocument
        val cache = WorkspaceSelectionResolver.SourceMappingCache()

        WorkspaceSelectionResolver.sourceLines(document, script, cache = cache)
        WorkspaceSelectionResolver.derivedSourceLines(document, script, cache = cache)

        assertEquals(1, cache.misses)
        assertEquals(1, cache.hits)
    }
}
