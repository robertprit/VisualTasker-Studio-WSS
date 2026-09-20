package com.visualtasker.wss.emscript.apply

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.workspace.model.WorkspaceSelectionResolver
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import de.visualtasker.blockeditor.domain.allConnections
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.layout.FlowLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmscriptApplyGuardTest {
    @Test
    fun previewAcceptsValidDraftAndProducesSerializedWorkspace() {
        val result = EmscriptApplyGuard().preview("LET foo = 1")

        assertTrue(result is EmscriptApplyGuardResult.Success)
        result as EmscriptApplyGuardResult.Success
        assertTrue(result.blockCount >= 2)
        assertTrue(result.serializedWorkspaceJson.contains("foo"))
        assertTrue(result.summary.contains("Draft -> Parse -> Import -> Validate: OK"))
    }

    @Test
    fun previewReturnsFailureForInvalidDraft() {
        val result = EmscriptApplyGuard().preview("THIS IS NOT EMSCRIPT")

        assertTrue(result is EmscriptApplyGuardResult.Failure)
        result as EmscriptApplyGuardResult.Failure
        assertEquals(EmscriptApplyGuardStage.PARSE_IMPORT, result.stage)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun previewAcceptsIntegrationTestDraft() {
        val result = EmscriptApplyGuard().preview(EditorDefaults.integrationTestScript)

        assertTrue(result is EmscriptApplyGuardResult.Success)
        result as EmscriptApplyGuardResult.Success
        assertTrue(result.blockCount > 20)
        assertTrue(result.summary.contains("Roundtrip-Script-Länge"))
    }

    @Test
    fun textWorkflowBlockFlowRoundtripPreservesSemanticIdentities() {
        val script = """
            LET a = 1
            IF a > 0
                click("Login")
            ELSE
                screenshot("login.png")
            END IF
        """.trimIndent()
        val guard = EmscriptApplyGuard()
        val first = guard.preview(script) as EmscriptApplyGuardResult.Success
        val second = guard.preview(script, previousDocument = first.importedDocument) as EmscriptApplyGuardResult.Success

        assertEquals(first.importedDocument.blocks.keys, second.importedDocument.blocks.keys)
        assertEquals(
            first.importedDocument.blocks.values.flatMap { it.allConnections() }.map { it.id }.toSet(),
            second.importedDocument.blocks.values.flatMap { it.allConnections() }.map { it.id }.toSet(),
        )

        val ifBlock = second.importedDocument.blocks.values.single {
            it.type == BlockTypes.CONTROL_IF_ELSE || it.type == BlockTypes.CONTROL_IF_ELSEIF_ELSE
        }
        assertEquals(ifBlock.id, WorkspaceSelectionResolver.blockAtSourceLine(second.importedDocument, 2))

        val workflow = WorkspaceWorkflowState.fromDocument(second.importedDocument, "test")
        assertTrue(workflow.flowchartProjection.graph.nodes.any { it.id == FlowNodeId("block:${ifBlock.id.value}") })
        val firstLayout = FlowLayoutEngine.layout(workflow.flowchartProjection.graph)
        val secondLayout = FlowLayoutEngine.layout(workflow.flowchartProjection.graph)
        assertEquals(firstLayout.nodeBounds, secondLayout.nodeBounds)
        assertEquals(firstLayout.routes, secondLayout.routes)
    }

    @Test
    fun generatedProjectionRestoresSourceLinesForBlockBuiltDocument() {
        val script = """
            LET score = 8
            LET thresholdLow = 3
            LET thresholdHigh = 7

            log("elseif-test-start")
            IF score < thresholdLow
                log("low")
            ELSEIF score >= thresholdHigh
                vibrate(40)
                log("high")
            ELSE
                beep()
                log("middle")
            END IF
            log("elseif-test-end")
        """.trimIndent()
        val imported = (EmscriptApplyGuard().preview(script) as EmscriptApplyGuardResult.Success).importedDocument
        val blockBuiltDocument = imported.copy(
            blocks = imported.blocks.mapValues { (_, block) ->
                block.copy(metadata = block.metadata + ("emscript.source.line" to "15"))
            },
        )

        val sourceLines = WorkspaceSelectionResolver.sourceLines(blockBuiltDocument, script)
        val compactProjection = script.replace("\n\n", "\n")
        val visibleDraftSourceLines = WorkspaceSelectionResolver.sourceLines(blockBuiltDocument, compactProjection) +
            WorkspaceSelectionResolver.derivedSourceLines(blockBuiltDocument, script)
        val ifBlock = blockBuiltDocument.blocks.values.single {
            it.type == BlockTypes.CONTROL_IF_ELSE || it.type == BlockTypes.CONTROL_IF_ELSEIF_ELSE
        }

        assertEquals(6, WorkspaceSelectionResolver.sourceLine(blockBuiltDocument, ifBlock.id, sourceLines))
        assertEquals(ifBlock.id, WorkspaceSelectionResolver.blockAtSourceLine(blockBuiltDocument, 6, sourceLines))
        assertEquals(6, WorkspaceSelectionResolver.sourceLine(blockBuiltDocument, ifBlock.id, visibleDraftSourceLines))
    }
}
