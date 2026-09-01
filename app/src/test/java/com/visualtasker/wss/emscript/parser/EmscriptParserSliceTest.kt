package com.visualtasker.wss.emscript.parser

import de.visualtasker.blockeditor.domain.WorkspaceGraph
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import com.visualtasker.wss.emscript.editor.EditorDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmscriptParserSliceTest {
    @Test
    fun parse_validLetSetIfSlice_buildsIr() {
        val source = """
            LET score = 1
            SET score = score + 2
            IF score >= 3
                log("ok")
            END IF
        """.trimIndent()
        val result = EmscriptParserSlice().parse(source)

        assertTrue(result.isSuccess)
        val ir = result.ir
        assertNotNull(ir)
        assertEquals(3, ir!!.statements.size)
        assertTrue(ir.statements[0] is EmscriptIrStatement.Let)
        assertTrue(ir.statements[1] is EmscriptIrStatement.Set)
        assertTrue(ir.statements[2] is EmscriptIrStatement.If)
    }

    @Test
    fun parse_integrationTestScript_buildsLoopAndNestedBranches() {
        val result = EmscriptParserSlice().parse(EditorDefaults.integrationTestScript)

        assertTrue(result.issues.joinToString { "${it.line}:${it.column} ${it.message}" }, result.isSuccess)
        val ir = result.ir!!
        assertTrue(ir.statements.any { it is EmscriptIrStatement.Loop })
        val loop = ir.statements.filterIsInstance<EmscriptIrStatement.Loop>().single()
        val topIf = loop.body.filterIsInstance<EmscriptIrStatement.If>().single()
        assertEquals(1, topIf.elseIfBranches.size)
        assertTrue(topIf.elseBranch.isNotEmpty())
        assertTrue(topIf.elseIfBranches.single().body.any { it is EmscriptIrStatement.If })
        assertTrue(topIf.elseBranch.any { it is EmscriptIrStatement.If })
        assertTrue(loop.body.filterIsInstance<EmscriptIrStatement.If>().single().thenBranch.any { it is EmscriptIrStatement.Beep })
        assertTrue(loop.body.filterIsInstance<EmscriptIrStatement.If>().single().elseIfBranches.single().body.any { it is EmscriptIrStatement.Vibrate })
        assertTrue(ir.statements.any { it is EmscriptIrStatement.ClickText })
        assertTrue(ir.statements.any { it is EmscriptIrStatement.Output })
        assertTrue(ir.statements.any { it is EmscriptIrStatement.Wait })
        assertTrue(ir.statements.any { it is EmscriptIrStatement.While })
    }

    @Test
    fun parse_basicActionFunctions_acceptsCanonicalFunctionsAndLegacyStatements() {
        val source = """
            wait(125)
            click("OK")
            log("ready")
            WAIT 250
            CLICK "Legacy"
            OUTPUT status
            beep()
            beep(880, 150, 75)
            vibrate(80)
            vibrate(0, 80, 40, 120)
            BEEP 660 50 40
        """.trimIndent()
        val result = EmscriptParserSlice().parse(source)

        assertTrue(result.issues.joinToString { "${it.line}:${it.column} ${it.message}" }, result.isSuccess)
        val statements = result.ir!!.statements
        assertEquals(11, statements.size)
        assertEquals(EmscriptIrStatement.Wait(EmscriptIrExpression.NumberLiteral(125.0, "125")), statements[0])
        assertEquals(EmscriptIrStatement.ClickText("OK"), statements[1])
        assertEquals(EmscriptIrStatement.Output(EmscriptIrExpression.StringLiteral("ready")), statements[2])
        assertEquals(EmscriptIrStatement.Wait(EmscriptIrExpression.NumberLiteral(250.0, "250")), statements[3])
        assertEquals(EmscriptIrStatement.ClickText("Legacy"), statements[4])
        assertEquals(EmscriptIrStatement.Output(EmscriptIrExpression.VariableRef("status")), statements[5])
        assertEquals(EmscriptIrStatement.Beep(), statements[6])
        assertEquals(EmscriptIrStatement.Beep(frequency = 880, durationMs = 150, volume = 75), statements[7])
        assertEquals(EmscriptIrStatement.Vibrate(listOf(80L)), statements[8])
        assertEquals(EmscriptIrStatement.Vibrate(listOf(0L, 80L, 40L, 120L)), statements[9])
        assertEquals(EmscriptIrStatement.Beep(frequency = 660, durationMs = 50, volume = 40), statements[10])
    }

    @Test
    fun parse_operatorPrecedence_keepsMultiplicationBeforeAddition() {
        val source = "SET a = 1 + 2 * 3"
        val result = EmscriptParserSlice().parse(source)
        val stmt = result.ir!!.statements.first() as EmscriptIrStatement.Set
        val expr = stmt.value as EmscriptIrExpression.Binary

        assertEquals(EmscriptBinaryOp.ADD, expr.op)
        assertTrue(expr.right is EmscriptIrExpression.Binary)
        assertEquals(EmscriptBinaryOp.MUL, (expr.right as EmscriptIrExpression.Binary).op)
    }

    @Test
    fun parse_invalidScript_reportsLineAndColumn() {
        val source = """
            LET value = 1
            IF value >=
                SET value = 2
            END IF
        """.trimIndent()
        val result = EmscriptParserSlice().parse(source)

        assertTrue(!result.isSuccess)
        assertTrue(result.issues.isNotEmpty())
        assertEquals(2, result.issues.first().line)
    }

    @Test
    fun import_validScript_buildsWorkspaceWithIfAndCompare() {
        val source = """
            LET v1 = 1
            LET v2 = 2
            IF v1 + v2 >= 3
                SET passed = true
            END IF
            IF true
            END IF
        """.trimIndent()
        val result = EmscriptWorkspaceImporter().import(source)

        assertTrue(result.isSuccess)
        val document = result.document!!
        assertTrue(document.blocks.values.any { it.type == BlockTypes.EVENT_START })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.CONTROL_IF })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.LOGIC_COMPARE })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.LOGIC_OPERATE })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.LITERAL_NUMBER })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.LITERAL_BOOLEAN })
        assertFalse(document.variables.variables.keys.any { it.startsWith("__lit_") })

        val ifBlockId = document.blocks.entries.first { it.value.type == BlockTypes.CONTROL_IF }.key
        val conditionHead = document.blocks[ifBlockId]!!
            .valueInputs
            .first { it.name == "CONDITION" }
            .connection
            .connectedTo
        assertNotNull(conditionHead)
        val conditionBlockId = WorkspaceGraph.findConnection(document, conditionHead!!)?.first
        assertNotNull(conditionBlockId)
    }

    @Test
    fun import_integrationTestScript_buildsRepeatAndBranchBlocks() {
        val result = EmscriptWorkspaceImporter().import(EditorDefaults.integrationTestScript)

        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        val document = result.document!!
        assertTrue(document.blocks.values.any { it.type == BlockTypes.CONTROL_REPEAT })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.CONTROL_IF_ELSEIF_ELSE })
        assertTrue(document.blocks.values.count { it.type == BlockTypes.CONTROL_IF_ELSE } >= 2)
        assertTrue(document.blocks.values.any { it.type == BlockTypes.CONTROL_WHILE })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.ACTION_WAIT })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.ACTION_CLICK_TEXT })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.DEBUG_LOG })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.LOGIC_COMPARE })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.LOGIC_OPERATE })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.FEEDBACK_BEEP })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.FEEDBACK_VIBRATE })
    }

    @Test
    fun import_feedbackCommands_buildsFeedbackBlocksAndCanonicalRoundtrip() {
        val source = """
            BEEP 880 150 75
            vibrate(0, 80, 40, 120)
        """.trimIndent()
        val imported = EmscriptWorkspaceImporter().import(source)

        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val document = imported.document!!
        assertTrue(document.blocks.values.any { it.type == BlockTypes.FEEDBACK_BEEP })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.FEEDBACK_VIBRATE })

        val regenerated = EmscriptGenerator(
            irGenerator = IrGenerator(),
        ).generate(document, scriptName = "feedback-roundtrip")

        assertEquals(
            """
            beep(880, 150, 75)
            vibrate(0, 80, 40, 120)
            """.trimIndent(),
            regenerated,
        )
    }

    @Test
    fun roundtrip_preservesLetSetAndExpressionStructure() {
        val source = """
            LET sum = 5 + 3
            SET result = sum * 2
            IF result >= 10
            END IF
        """.trimIndent()
        val imported = EmscriptWorkspaceImporter().import(source)
        assertTrue(imported.isSuccess)

        val regenerated = EmscriptGenerator(
            irGenerator = IrGenerator(),
        ).generate(imported.document!!, scriptName = "roundtrip")

        assertEquals(
            """
            SET sum = (5 + 3)
            SET result = (sum * 2)
            IF (result >= 10)
            END IF
            """.trimIndent(),
            regenerated,
        )
    }
}
