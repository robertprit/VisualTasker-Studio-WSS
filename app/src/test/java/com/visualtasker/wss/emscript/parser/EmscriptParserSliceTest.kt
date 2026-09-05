package com.visualtasker.wss.emscript.parser

import com.visualtasker.wss.emscript.editor.EditorDefaults
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceDryRunRuntime
import com.visualtasker.wss.flowchart.IrGraphFlowchartProjector
import de.visualtasker.blockeditor.domain.WorkspaceGraph
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.ir.IrGraphGenerator
import de.visualtasker.blockeditor.ir.validateIntegrity
import de.visualtasker.blockeditor.ir.validateSemantics
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CommandArgument
import de.visualtasker.blockeditor.registry.CommandArgumentType
import de.visualtasker.blockeditor.registry.CommandCatalogKind
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import de.visualtasker.blockeditor.serialization.WorkspaceDecodeResult
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
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
    fun parse_escapedStringLiterals_preservesRuntimeCharacters() {
        val source = """
            click("A\tB")
            log("line 1\nline 2")
            if (screenContains("He said \"OK\"")) {
                log("quoted")
            }
        """.trimIndent()
        val result = EmscriptParserSlice().parse(source)

        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        val statements = result.ir!!.statements
        assertEquals(EmscriptIrStatement.ClickText("A\tB"), statements[0])
        assertEquals(EmscriptIrStatement.Output(EmscriptIrExpression.StringLiteral("line 1\nline 2")), statements[1])
        val condition = (statements[2] as EmscriptIrStatement.If).condition
        assertEquals(
            EmscriptIrExpression.FunctionCall("screenContains", listOf(EmscriptIrExpression.StringLiteral("He said \"OK\""))),
            condition,
        )
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
    fun parse_reporterFunctionExpressions_buildsExpressionCalls() {
        val source = """
            if (screenContains("Ready")) {
                log("visible");
            }
            while (screenContains("Spinner")) {
                wait(50);
            }
        """.trimIndent()
        val result = EmscriptParserSlice().parse(source)

        assertTrue(result.issues.joinToString { "${it.line}:${it.column} ${it.message}" }, result.isSuccess)
        val ifStatement = result.ir!!.statements[0] as EmscriptIrStatement.If
        val whileStatement = result.ir.statements[1] as EmscriptIrStatement.While
        assertEquals(
            EmscriptIrExpression.FunctionCall("screenContains", listOf(EmscriptIrExpression.StringLiteral("Ready"))),
            ifStatement.condition,
        )
        assertEquals(
            EmscriptIrExpression.FunctionCall("screenContains", listOf(EmscriptIrExpression.StringLiteral("Spinner"))),
            whileStatement.condition,
        )
    }

    @Test
    fun parse_expressionCatalogEntries_acceptTypedSamples() {
        val entries = VisualTaskerCommandCatalog.allEntries()
            .filter { it.kind == CommandCatalogKind.REPORTER || it.kind == CommandCatalogKind.OPERATOR }
            .filter { it.block != null }
            .sortedBy { it.id }
        val source = entries.joinToString(separator = "\n") { entry ->
            val variableName = "expr_${entry.id.replace(Regex("[^A-Za-z0-9_]"), "_")}"
            val arguments = entry.arguments.joinToString(",") { it.sampleExpressionArgument() }
            "let $variableName = ${entry.canonicalName}($arguments)"
        }

        val result = EmscriptParserSlice().parse(source)

        assertTrue(result.issues.joinToString { "${it.line}:${it.column} ${it.message}" }, result.isSuccess)
        assertEquals(entries.size, result.ir!!.statements.size)
        assertTrue(result.ir.statements.all { it is EmscriptIrStatement.Let })
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
    fun parse_cataloguedGenericCommand_buildsCommandCall() {
        val result = EmscriptParserSlice().parse("findTemplate(\"button.png\", 0.8, 1000)")

        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        assertEquals(
            EmscriptIrStatement.CommandCall("findTemplate", "\"button.png\",0.8,1000"),
            result.ir!!.statements.single(),
        )
    }

    @Test
    fun parse_cataloguedCommand_rejectsInvalidArgumentCountAndType() {
        val missingRequired = EmscriptParserSlice().parse("clickPoint(12)")
        val invalidType = EmscriptParserSlice().parse("clickPoint(\"x\", 20)")
        val tooMany = EmscriptParserSlice().parse("screenshot(\"a.png\", \"extra\")")

        assertFalse(missingRequired.isSuccess)
        assertTrue(missingRequired.issues.single().message.contains("mindestens 2 Parameter"))
        assertFalse(invalidType.isSuccess)
        assertTrue(invalidType.issues.single().message.contains("NUMBER"))
        assertFalse(tooMany.isSuccess)
        assertTrue(tooMany.issues.single().message.contains("maximal 1 Parameter"))
    }

    @Test
    fun parse_reporterFunction_rejectsInvalidArgumentShape() {
        val result = EmscriptParserSlice().parse("if (screenContains(123)) { log(\"bad\"); }")

        assertFalse(result.isSuccess)
        assertTrue(result.issues.single().message.contains("TEXT"))
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
    fun import_multipleElseIfBranches_preservesEveryBranchSlot() {
        val source = """
            if (false) {
                log("then");
            } else if (false) {
                log("first");
            } else if (true) {
                log("second");
            } else {
                log("else");
            }
        """.trimIndent()
        val result = EmscriptWorkspaceImporter().import(source)

        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        val document = result.document!!
        val ifBlock = document.blocks.values.single { it.type == BlockTypes.CONTROL_IF_ELSEIF_ELSE }
        assertTrue(ifBlock.valueInputs.any { it.name == "ELIF_CONDITION" })
        assertTrue(ifBlock.valueInputs.any { it.name == "ELIF_CONDITION_1" })
        assertTrue(ifBlock.statementInputs.any { it.name == BlockTypes.SLOT_ELIF })
        assertTrue(ifBlock.statementInputs.any { it.name == "ELIF_1" })

        val regenerated = EmscriptGenerator(IrGenerator()).generate(document, scriptName = "multi-elseif")
        val irGraph = IrGraphGenerator().generate(document)
        assertTrue(regenerated.contains("} else if (false) {"))
        assertTrue(regenerated.contains("} else if (true) {"))
        assertTrue(regenerated.contains("log(\"second\");"))
        assertTrue(irGraph.validateIntegrity().joinToString { it.message }, irGraph.validateIntegrity().isEmpty())
        assertTrue(irGraph.validateSemantics().joinToString { it.message }, irGraph.validateSemantics().isEmpty())
        assertTrue(irGraph.edges.any { it.kind.name == "CONDITION" && it.label == "ELIF_CONDITION_1" })
        assertTrue(irGraph.branches.any { it.role.name == "ELSE_IF" && it.conditionNodeId?.value?.startsWith("block:") == true })
    }

    @Test
    fun import_screenContainsCondition_buildsReporterBlockAndRunsDryRun() {
        val source = """
            if (screenContains("Ready")) {
                log("visible");
            }
        """.trimIndent()
        val result = EmscriptWorkspaceImporter().import(source)

        assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
        val document = result.document!!
        assertTrue(document.blocks.values.any { it.type == BlockTypes.LOGIC_SCREEN_CONTAINS })
        assertTrue(WorkspaceDryRunRuntime().run(document) is EmscriptDryRunResult.Success)
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

    @Test
    fun roundtrip_catalogCommands_surviveParserWorkspaceIrAndDryRun() {
        val source = """
            Tasker.runTask("Morning", {})
            Termux.shell("echo ok")
            Shizuku.exec("cmd", ["package", "list"])
            ChromeTab.open("https://example.com")
            Chart.create("line", {})
            screenshot("screen.png")
        """.trimIndent()
        val parsed = EmscriptParserSlice().parse(source)
        assertTrue(parsed.issues.joinToString { it.message }, parsed.isSuccess)
        assertTrue(parsed.ir!!.statements.all { it is EmscriptIrStatement.CommandCall })

        val imported = EmscriptWorkspaceImporter().import(source)
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val document = imported.document!!
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}tasker.runTask" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}termux.shell" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}shizuku.exec" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}chromeTab.open" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}chart.create" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}vision.screenshot" })

        val irGraph = IrGraphGenerator().generate(document)
        assertTrue(irGraph.validateIntegrity().joinToString { it.message }, irGraph.validateIntegrity().isEmpty())
        assertTrue(irGraph.nodes.any { it.properties["commandName"] == "Tasker.runTask" })
        assertTrue(irGraph.nodes.any { it.properties["commandName"] == "Termux.shell" })

        val regenerated = EmscriptGenerator(IrGenerator()).generate(document, scriptName = "catalog-roundtrip")
        assertTrue(regenerated.contains("Tasker.runTask(\"Morning\",{});"))
        assertTrue(regenerated.contains("Termux.shell(\"echo ok\");"))
        assertTrue(regenerated.contains("Shizuku.exec(\"cmd\",[\"package\",\"list\"]);"))
        assertTrue(regenerated.contains("ChromeTab.open(\"https://example.com\");"))
        assertTrue(regenerated.contains("Chart.create(\"line\",{});"))
        assertTrue(regenerated.contains("screenshot(\"screen.png\");"))
    }

    @Test
    fun commandCatalogBreadthScript_survivesParserWorkspaceIrFlowchartAndDryRun() {
        val source = EditorDefaults.commandCatalogBreadthTestScript
        val parsed = EmscriptParserSlice().parse(source)
        assertTrue(parsed.issues.joinToString { "${it.line}:${it.column} ${it.message}" }, parsed.isSuccess)
        assertTrue(flattenStatements(parsed.ir!!.statements).filterIsInstance<EmscriptIrStatement.CommandCall>().size >= 10)

        val imported = EmscriptWorkspaceImporter().import(source)
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val serialized = WorkspaceSerializer.serialize(imported.document!!)
        val decoded = WorkspaceSerializer.decode(serialized) as WorkspaceDecodeResult.Decoded
        val document = decoded.document
        val irGraph = IrGraphGenerator().generate(document)
        val flowchart = IrGraphFlowchartProjector.project(irGraph).graph

        assertTrue(irGraph.validateIntegrity().joinToString { it.message }, irGraph.validateIntegrity().isEmpty())
        assertTrue(flowchart.diagnostics.joinToString { it.message }, flowchart.diagnostics.none { it.severity.name == "ERROR" })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.ACTION_CLICK_TEXT })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.FEEDBACK_BEEP })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.FEEDBACK_VIBRATE })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.VARIABLE_SET })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.CONTROL_REPEAT })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.CONTROL_WHILE })
        assertTrue(document.blocks.values.any { it.type == BlockTypes.CONTROL_IF_ELSEIF_ELSE })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}vision.screenshot" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}clipboard.set" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}clipboard.get" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}file.writeText" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}file.readText" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}system.info" })
        assertTrue(document.blocks.values.any { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}system.env" })
        assertTrue(document.blocks.values.none { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}tasker.runTask" })
        assertTrue(document.blocks.values.none { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}termux.run" })
        assertTrue(document.blocks.values.none { it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}scrcpy.start" })
        assertTrue(irGraph.nodes.any { it.properties["commandName"] == "screenshot" })
        assertTrue(irGraph.nodes.any { it.properties["commandName"] == "Clipboard.set" })
        assertTrue(irGraph.nodes.any { it.properties["commandName"] == "File.writeText" })
        assertTrue(irGraph.nodes.any { it.properties["commandName"] == "Sys.info" })
        assertTrue(irGraph.nodes.any { it.properties["commandName"] == "Env.get" })

        val regenerated = EmscriptGenerator(IrGenerator()).generate(document, scriptName = "catalog-breadth")
        assertTrue(regenerated.contains("click(\"Start\")"))
        assertTrue(regenerated.contains("clickPoint(120,240,1);"))
        assertTrue(regenerated.contains("swipe([120,640,120,220],1);"))
        assertTrue(regenerated.contains("screenshot(\"core-screen.png\");"))
        assertTrue(regenerated.contains("Clipboard.set(\"visualtasker\");"))
        assertTrue(regenerated.contains("File.writeText(\"core-runtime.txt\",\"hello\");"))
        assertTrue(regenerated.contains("repeat (3) {"))
        assertTrue(regenerated.contains("while ("))
        assertTrue(regenerated.contains("beep("))
        assertTrue(regenerated.contains("vibrate("))

        assertTrue(EmscriptDryRunRuntime().run(source) is EmscriptDryRunResult.Success)
        assertTrue(WorkspaceDryRunRuntime().run(document) is EmscriptDryRunResult.Success)
    }

    @Test
    fun statementCommandCatalog_survivesParserWorkspaceAndDryRun() {
        val entries = VisualTaskerCommandCatalog.allEntries()
            .filter { it.kind == CommandCatalogKind.STATEMENT && it.block != null }
            .sortedBy { it.canonicalName }
        val source = entries.joinToString(separator = "\n") { entry ->
            "${entry.canonicalName}(${entry.arguments.joinToString(",") { it.sampleArgument() }})"
        }

        val parsed = EmscriptParserSlice().parse(source)
        assertTrue(parsed.issues.joinToString { "${it.line}:${it.column} ${it.message}" }, parsed.isSuccess)

        val imported = EmscriptWorkspaceImporter().import(source, workspaceId = "catalog-generated-smoke")
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
        val document = imported.document!!
        val dryRun = WorkspaceDryRunRuntime().run(document)

        assertTrue(dryRun is EmscriptDryRunResult.Success)
        val events = (dryRun as EmscriptDryRunResult.Success).events
        val missing = entries
            .map { it.canonicalName }
            .filterNot { command -> events.any { it.command == command || it.message.contains(command) } }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun adapterGatedCatalogCommands_dryRunAsWarningsInsteadOfErrors() {
        val entries = VisualTaskerCommandCatalog.allEntries()
            .filter { it.runtime?.dryRunBehavior == "adapter-gated" }
            .sortedBy { it.canonicalName }
        val source = entries.joinToString(separator = "\n") { entry ->
            "${entry.canonicalName}(${entry.arguments.joinToString(",") { it.sampleArgument() }})"
        }

        val parsed = EmscriptParserSlice().parse(source)
        val dryRun = EmscriptDryRunRuntime().run(source)

        assertTrue(parsed.issues.joinToString { "${it.line}:${it.column} ${it.message}" }, parsed.isSuccess)
        assertTrue(dryRun is EmscriptDryRunResult.Success)
        val events = (dryRun as EmscriptDryRunResult.Success).events
        val warnings = events.filter { it.severity.name == "WARNING" }
        assertEquals(entries.map { it.canonicalName }.toSet(), warnings.mapNotNull { it.command }.toSet())
        assertTrue(warnings.all { it.message.contains("Adapter noch nicht live") || it.message.contains("Live-Capability noch blockiert") })
    }

    @Test
    fun roundtrip_integrationScriptSurvivesWorkspaceSerializationIrAndFlowchartProjection() {
        val imported = EmscriptWorkspaceImporter().import(EditorDefaults.integrationTestScript)
        assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)

        val serialized = WorkspaceSerializer.serialize(imported.document!!)
        val decoded = WorkspaceSerializer.decode(serialized) as WorkspaceDecodeResult.Decoded
        val document = decoded.document
        val irGraph = IrGraphGenerator().generate(document)
        val flowchart = IrGraphFlowchartProjector.project(irGraph).graph
        val regenerated = EmscriptGenerator(IrGenerator()).generate(document, scriptName = "roundtrip")

        assertTrue(irGraph.validateIntegrity().joinToString { it.message }, irGraph.validateIntegrity().isEmpty())
        assertTrue(flowchart.nodes.isNotEmpty())
        assertTrue(flowchart.edges.isNotEmpty())
        assertTrue(flowchart.diagnostics.joinToString { it.message }, flowchart.diagnostics.none { it.severity.name == "ERROR" })
        assertTrue(irGraph.facets.any { it.id == "facet:emscript:vars:init" && it.properties["editorFacetKind"] == "variable-bulk" })
        assertTrue(irGraph.facets.any { it.id == "facet:emscript:flow:main-loop" && it.properties["startLine"] != null })
        assertTrue(regenerated.contains("repeat (10) {"))
        assertTrue(regenerated.contains("if ("))
        assertTrue(regenerated.contains("} else if ("))
        assertTrue(regenerated.contains("beep("))
        assertTrue(regenerated.contains("vibrate("))
        assertTrue(regenerated.contains("click(\"Start\")"))
    }

    private fun flattenStatements(statements: List<EmscriptIrStatement>): List<EmscriptIrStatement> =
        statements.flatMap { statement ->
            when (statement) {
                is EmscriptIrStatement.If -> listOf(statement) +
                    flattenStatements(statement.thenBranch) +
                    statement.elseIfBranches.flatMap { flattenStatements(it.body) } +
                    flattenStatements(statement.elseBranch)
                is EmscriptIrStatement.Loop -> listOf(statement) + flattenStatements(statement.body)
                is EmscriptIrStatement.While -> listOf(statement) + flattenStatements(statement.body)
                else -> listOf(statement)
            }
        }

    private fun CommandArgument.sampleArgument(): String =
        when (type) {
            CommandArgumentType.BOOLEAN -> defaultValue ?: "true"
            CommandArgumentType.NUMBER,
            CommandArgumentType.DURATION_MS,
            CommandArgumentType.FREQUENCY_HZ,
            CommandArgumentType.PERCENT -> defaultValue ?: "1"
            CommandArgumentType.TEXT,
            CommandArgumentType.VARIABLE_REF,
            CommandArgumentType.IMAGE_TEMPLATE,
            CommandArgumentType.REGION -> "\"${defaultValue ?: name}\""
            CommandArgumentType.ANY -> defaultValue?.takeIf { it.isNotBlank() } ?: "{}"
            CommandArgumentType.STATEMENT_BODY -> "{}"
        }

    private fun CommandArgument.sampleExpressionArgument(): String =
        when (type) {
            CommandArgumentType.BOOLEAN -> defaultValue ?: "true"
            CommandArgumentType.NUMBER,
            CommandArgumentType.DURATION_MS,
            CommandArgumentType.FREQUENCY_HZ,
            CommandArgumentType.PERCENT -> defaultValue ?: "1"
            CommandArgumentType.TEXT,
            CommandArgumentType.VARIABLE_REF,
            CommandArgumentType.IMAGE_TEMPLATE,
            CommandArgumentType.REGION -> "\"${defaultValue ?: name}\""
            CommandArgumentType.ANY -> defaultValue?.takeIf { it.isNotBlank() } ?: "1"
            CommandArgumentType.STATEMENT_BODY -> "{}"
        }
}
