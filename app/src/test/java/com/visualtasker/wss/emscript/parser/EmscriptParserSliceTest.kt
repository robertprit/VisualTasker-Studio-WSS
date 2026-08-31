package com.visualtasker.wss.emscript.parser

import de.visualtasker.blockeditor.domain.WorkspaceGraph
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
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
                SET result = "ok"
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
            LET sum = (5 + 3)
            SET result = (sum * 2)
            IF (result >= 10.0)
            END IF
            """.trimIndent(),
            regenerated,
        )
    }
}
