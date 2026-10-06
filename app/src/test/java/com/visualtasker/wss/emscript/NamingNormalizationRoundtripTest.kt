package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import com.visualtasker.wss.flowchart.BlockEditorFlowchartProjector
import de.visualtasker.workflow.core.FieldValue
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.workflow.semantics.ir.IrGraphGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.workflow.semantics.EmscriptV1NamingNormalizations
import de.visualtasker.workflow.semantics.VisualTaskerCommandCatalog
import de.visualtasker.workflow.serialization.WorkflowDecodeResult
import de.visualtasker.workflow.serialization.WorkflowSerializer
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NamingNormalizationRoundtripTest {
    private data class Case(
        val stableId: String,
        val canonicalSource: String,
        val legacySource: String,
        val canonicalOutput: String,
    )

    private val cases = listOf(
        Case(
            stableId = EmscriptV1NamingNormalizations.FILE_WRITE_TEXT.stableId,
            canonicalSource = "file.writeText(\"a.txt\", \"value\")",
            legacySource = "File.writeText(\"a.txt\", \"value\")",
            canonicalOutput = "file.writeText(\"a.txt\",\"value\");",
        ),
        Case(
            stableId = EmscriptV1NamingNormalizations.CLIPBOARD_SET.stableId,
            canonicalSource = "clipboard.set(\"value\")",
            legacySource = "Clipboard.set(\"value\")",
            canonicalOutput = "clipboard.set(\"value\");",
        ),
        Case(
            stableId = EmscriptV1NamingNormalizations.CACHE_CLEAR.stableId,
            canonicalSource = "cache.clear()",
            legacySource = "Cache.clear()",
            canonicalOutput = "cache.clear();",
        ),
    )

    @Test
    fun `canonical and legacy spellings retain identity and write canonical V1`() {
        cases.forEach { case ->
            val catalogEntry = VisualTaskerCommandCatalog.findById(case.stableId)!!
            val canonical = importSingleCommand(case.canonicalSource, "canonical-${case.stableId}")
            val legacy = importSingleCommand(case.legacySource, "legacy-${case.stableId}")

            assertEquals(canonical.type, legacy.type)
            assertEquals("${BlockTypes.EMSCRIPT_COMMAND_PREFIX}${case.stableId}", canonical.type)
            assertEquals(case.stableId, VisualTaskerCommandCatalog.findByBlockType(canonical.type)?.id)
            assertEquals(FieldValue.Text(catalogEntry.canonicalName), canonical.fields.getValue("command"))
            assertEquals(FieldValue.Text(catalogEntry.canonicalName), legacy.fields.getValue("command"))

            listOf(case.canonicalSource, case.legacySource).forEachIndexed { index, source ->
                val imported = EmscriptWorkspaceImporter().import(source, workspaceId = "naming-${case.stableId}-$index")
                assertTrue(imported.issues.joinToString { it.message }, imported.isSuccess)
                val document = imported.document!!
                val serialized = WorkflowSerializer.serialize(document)
                val decoded = (WorkflowSerializer.decode(serialized) as WorkflowDecodeResult.Decoded).document
                val generated = EmscriptGenerator(IrGenerator()).generate(decoded, scriptName = case.stableId)
                assertEquals(case.canonicalOutput, generated)
                assertFalse(generated.contains(case.legacySource.substringBefore('(')))

                val irNode = IrGraphGenerator().generate(decoded).nodes.single {
                    it.properties["commandId"] == case.stableId
                }
                assertEquals(case.stableId, irNode.properties["commandId"])
                assertEquals(catalogEntry.canonicalName, irNode.properties["commandName"])

                val flow = BlockEditorFlowchartProjector.project(decoded).graph
                assertTrue(flow.nodes.any {
                    it.kind.standard == FlowNodeKind.ACTION &&
                        it.properties["command"] == FlowSemanticValue.StringValue(catalogEntry.canonicalName)
                })

                val second = EmscriptWorkspaceImporter().import(generated, workspaceId = "second-${case.stableId}-$index")
                assertTrue(second.issues.joinToString { it.message }, second.isSuccess)
                val secondBlock = second.document!!.blocks.values.single { it.type == canonical.type }
                assertEquals(case.stableId, VisualTaskerCommandCatalog.findByBlockType(secondBlock.type)?.id)
            }
        }
    }

    private fun importSingleCommand(source: String, workspaceId: String) =
        EmscriptWorkspaceImporter().import(source, workspaceId).let { result ->
            assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
            result.document!!.blocks.values.single { it.type.startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX) }
        }
}
