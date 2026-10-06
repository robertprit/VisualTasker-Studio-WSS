package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.parser.EmscriptIrStatement
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.workflow.semantics.ir.IrStatement
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.LegacyTouchClassification
import de.visualtasker.blockeditor.registry.LegacyTouchStructuralClassifier
import de.visualtasker.workflow.serialization.WorkflowDecodeResult
import de.visualtasker.workflow.serialization.WorkflowSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyTouchStructuralRoundtripTest {
    private data class Fixture(
        val source: String,
        val normalizedRawArgument: String,
    )

    private val fixtures = listOf(
        Fixture("touch([540, 1100])", "[540,1100]"),
        Fixture("touch([\"down\", 120, 240, \"up\"])", "[\"down\",120,240,\"up\"]"),
        Fixture("touch(\"down(10,20);move(20,30);up(20,30)\")", "\"down(10,20);move(20,30);up(20,30)\""),
    )

    @Test
    fun `legacy touch fixtures preserve raw structural truth through parser workspace IR and source`() {
        fixtures.forEachIndexed { index, fixture ->
            val parsed = EmscriptParserSlice().parse(fixture.source)
            assertTrue(parsed.issues.joinToString { it.message }, parsed.isSuccess)
            val parsedCall = parsed.ir!!.statements.single() as EmscriptIrStatement.CommandCall
            assertEquals("touch", parsedCall.command)
            assertEquals(fixture.normalizedRawArgument, parsedCall.arguments)

            val imported = EmscriptWorkspaceImporter().import(fixture.source, workspaceId = "touch-$index")
            assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
            val commandBlock = imported.document!!.blocks.values.single {
                it.type == "${BlockTypes.EMSCRIPT_COMMAND_PREFIX}input.touch"
            }
            assertEquals(FieldValue.Text(fixture.normalizedRawArgument), commandBlock.fields["args"])

            val serialized = WorkflowSerializer.serialize(imported.document)
            val decoded = (WorkflowSerializer.decode(serialized) as WorkflowDecodeResult.Decoded).document
            val irCall = IrGenerator().generate(decoded).statements.last() as IrStatement.CommandCall
            assertEquals("touch", irCall.command)
            assertEquals(fixture.normalizedRawArgument, irCall.arguments)

            val generated = EmscriptGenerator(IrGenerator()).generate(decoded)
            assertEquals("touch(${fixture.normalizedRawArgument});", generated)
            assertEquals(
                LegacyTouchClassification.PARTIAL,
                LegacyTouchStructuralClassifier.classify("touch", irCall.arguments).classification,
            )
        }
    }
}
