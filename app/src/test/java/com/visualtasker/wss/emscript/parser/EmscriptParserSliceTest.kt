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
            wait(125);
            click("OK");
            log("ready");
            WAIT 250
            CLICK "Legacy"
            OUTPUT status
            beep();
            beep(880, 150, 75);
            vibrate(80);
            vibrate(0, 80, 40, 120);
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
    fun parse_remGroupMarkers_ignoresEditorFacetsAsComments() {
        val source = """
            REM @vt.group.start id="vars:init" label="Variablen initialisieren" kind="variable-bulk"
            LET counter = 0
            REM @vt.group.end id="vars:init"
        """.trimIndent()

        val result = EmscriptParserSlice().parse(source)

        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        assertEquals(1, result.ir!!.statements.size)
        assertTrue(result.ir.statements.single() is EmscriptIrStatement.Let)
    }

    @Test
    fun scan_groupFacets_extractsRemAndSlashMarkers() {
        val source = """
            REM @vt.group.start id="vars:init" label="Variablen initialisieren" kind="variable-bulk"
            LET counter = 0
            REM @vt.group.end id="vars:init"
            // @vt.group.start id="flow:loop" label="Loop" kind="loop-region"
            LOOP 1
            END LOOP
        """.trimIndent()

        val facets = EmscriptEditorFacetScanner.scan(source)

        assertEquals(2, facets.size)
        assertEquals("vars:init", facets[0].id)
        assertEquals("Variablen initialisieren", facets[0].label)
        assertEquals("variable-bulk", facets[0].kind)
        assertEquals(1, facets[0].startLine)
        assertEquals(3, facets[0].endLine)
        assertEquals("flow:loop", facets[1].id)
        assertEquals(null, facets[1].endLine)
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
    fun parse_canonicalBraceControlFlow_buildsBranchesAndLoops() {
        val source = """
            set score = 1;
            if ((score >= 3) && true) {
                log("then");
            } else if (score == 2) {
                beep();
            } else {
                vibrate(40);
            }
            repeat (2) {
                wait(10);
            }
            while (score < 4) {
                set score = score + 1;
            }
        """.trimIndent()
        val result = EmscriptParserSlice().parse(source)

        assertTrue(result.issues.joinToString { "${it.line}:${it.column} ${it.message}" }, result.isSuccess)
        val statements = result.ir!!.statements
        assertTrue(statements[0] is EmscriptIrStatement.Set)
        val ifStatement = statements[1] as EmscriptIrStatement.If
        assertEquals(1, ifStatement.elseIfBranches.size)
        assertTrue(ifStatement.thenBranch.single() is EmscriptIrStatement.Output)
        assertTrue(ifStatement.elseIfBranches.single().body.single() is EmscriptIrStatement.Beep)
        assertTrue(ifStatement.elseBranch.single() is EmscriptIrStatement.Vibrate)
        assertTrue(statements[2] is EmscriptIrStatement.Loop)
        assertTrue(statements[3] is EmscriptIrStatement.While)
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
            beep(880, 150, 75);
            vibrate(0, 80, 40, 120);
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
            set sum = (5 + 3);
            set result = (sum * 2);
            if ((result >= 10)) {
            }
            """.trimIndent(),
            regenerated,
        )
    }
}
