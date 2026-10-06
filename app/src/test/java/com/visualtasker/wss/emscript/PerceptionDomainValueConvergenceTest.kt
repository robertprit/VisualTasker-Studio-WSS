package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.runtime.EmscriptCoordinateSpace
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptPathValue
import com.visualtasker.wss.emscript.runtime.EmscriptPointValue
import com.visualtasker.wss.emscript.runtime.EmscriptRegionValue
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeAdapterResult
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes
import com.visualtasker.wss.emscript.runtime.RuntimeTextMatchCandidate
import com.visualtasker.wss.emscript.runtime.TextMatchQueryRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntimeEnvironment
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.ir.IrExpression
import de.visualtasker.workflow.semantics.ir.IrGenerator
import de.visualtasker.workflow.semantics.ir.IrStatement
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.workflow.semantics.VisualTaskerCommandCatalog
import de.visualtasker.workflow.serialization.WorkflowSerializer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerceptionDomainValueConvergenceTest {
    private val guard = EmscriptApplyGuard()
    private val pixelRegion = EmscriptRegionValue(
        x = 10.0,
        y = 20.0,
        width = 30.0,
        height = 40.0,
        coordinateSpace = EmscriptCoordinateSpace.PIXEL,
    )
    private val imageMatch = EmscriptValue.ImageMatchValue(
        templateId = "login-template",
        label = "Login",
        region = pixelRegion,
        score = 0.94,
    )
    private val textMatch = EmscriptValue.TextMatchValue(
        text = "Login",
        region = pixelRegion,
        confidence = 0.91,
        source = "ocr",
    )
    private val marker = EmscriptValue.MarkerValue(
        markerId = "marker-login",
        label = "Login region",
        region = pixelRegion,
        path = EmscriptPathValue(
            start = EmscriptPointValue(10.0, 20.0, EmscriptCoordinateSpace.PIXEL),
            control = EmscriptPointValue(20.0, 25.0, EmscriptCoordinateSpace.PIXEL),
            end = EmscriptPointValue(40.0, 60.0, EmscriptCoordinateSpace.PIXEL),
        ),
        mode = "region",
        assetId = "asset-login",
        threshold = 0.85,
    )

    @Test
    fun `domain values preserve geometry identity confidence and optional metadata`() {
        assertEquals(pixelRegion, imageMatch.region)
        assertEquals(0.94, imageMatch.score, 0.0)
        assertEquals("ocr", textMatch.source)
        assertEquals("marker-login", marker.markerId)
        assertEquals("asset-login", marker.assetId)
        assertEquals(EmscriptCoordinateSpace.PIXEL, marker.path!!.start.coordinateSpace)
        assertEquals(0.0, imageMatch.copy(score = 0.0).score, 0.0)
        assertEquals(0.0, textMatch.copy(confidence = 0.0).confidence, 0.0)

        assertTrue(runCatching { imageMatch.copy(score = 1.01) }.isFailure)
        assertTrue(runCatching { textMatch.copy(confidence = Double.NaN) }.isFailure)
        assertTrue(runCatching { marker.copy(markerId = "") }.isFailure)
        assertTrue(runCatching {
            EmscriptRegionValue(0.9, 0.2, 0.2, 0.2, EmscriptCoordinateSpace.NORMALIZED)
        }.isFailure)
    }

    @Test
    fun `source workspace IR serializer and aliases preserve perception reporters`() {
        val source = """
            LET image:ImageMatch? = findTemplate("login.png", 0.8, 1000, 1, region(10, 20, 30, 40))
            LET text:TextMatch? = findText("Login", 1000)
            LET savedMarker:Marker? = markerLoad("login")
            SET image = FIND_TEMPLATE("login.png", 0.9, 500)
            SET text = FIND_TEXT("Login")
            SET savedMarker = LOAD_MARKER("login")
        """.trimIndent()
        val preview = guard.preview(source)
        assertTrue(preview.toString(), preview is EmscriptApplyGuardResult.Success)
        preview as EmscriptApplyGuardResult.Success
        val document = preview.importedDocument

        val commandBlocks = document.blocks.values.filter {
            it.type.startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX) &&
                it.metadata[VisualTaskerCommandCatalog.METADATA_COMMAND_ID] in COMMAND_IDS
        }
        assertEquals(6, commandBlocks.size)
        assertTrue(commandBlocks.all { it.output?.provides in RETURN_TYPES.values })

        val registry = CompositeBlockRegistry().apply {
            document.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val ir = IrGenerator(registry).generate(document)
        val calls = ir.statements.filterIsInstance<IrStatement.SetVariable>()
            .map { it.expression as IrExpression.CommandCall }
        assertEquals(6, calls.size)
        assertEquals(COMMAND_IDS, calls.map { it.commandId }.toSet())
        assertTrue(calls.all { it.returnType == RETURN_TYPES.getValue(it.commandId) })

        val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)
        assertTrue(generated, generated.contains("findTemplate("))
        assertTrue(generated, generated.contains("findText("))
        assertTrue(generated, generated.contains("markerLoad("))
        assertFalse(generated, generated.contains("FIND_TEMPLATE"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)

        val decoded = WorkflowSerializer.deserialize(WorkflowSerializer.serialize(document))
        assertEquals(document.blocks.keys, decoded.blocks.keys)
        assertEquals(
            commandBlocks.associate { it.id to it.metadata[VisualTaskerCommandCatalog.METADATA_COMMAND_ID] },
            decoded.blocks.values
                .filter { it.id in commandBlocks.map { block -> block.id } }
                .associate { it.id to it.metadata[VisualTaskerCommandCatalog.METADATA_COMMAND_ID] },
        )
    }

    @Test
    fun `perception reporters reject incompatible assignment targets`() {
        listOf(
            "LET value:String = findTemplate(\"login.png\", 0.8, 1000)",
            "LET value:Number = findText(\"Login\")",
            "LET value:Bool = markerLoad(\"login\")",
            "LET value:ImageMatch? = findText(\"Login\")",
            "LET value:Marker? = findTemplate(\"login.png\", 0.8, 1000)",
        ).forEach { source ->
            assertTrue("$source must fail", guard.preview(source) is EmscriptApplyGuardResult.Failure)
        }
    }

    @Test
    fun `runtime keeps typed values absence provider failures and invalid values distinct`() = runBlocking {
        val valueResult = runtime(
            findTemplate = RuntimeAdapterResult.success(imageMatch),
            findText = RuntimeAdapterResult.success(textMatch),
            markerLoad = RuntimeAdapterResult.success(marker),
        ).run(runtimeDocument())
        assertTrue(valueResult.toString(), valueResult is EmscriptDryRunResult.Success)
        valueResult as EmscriptDryRunResult.Success
        assertEquals(imageMatch, valueResult.variables["image"])
        assertEquals(textMatch, valueResult.variables["text"])
        assertEquals(marker, valueResult.variables["savedMarker"])

        val absentResult = runtime(
            findTemplate = RuntimeAdapterResult.success(EmscriptValue.NullValue),
            findText = RuntimeAdapterResult.success(EmscriptValue.NullValue),
            markerLoad = RuntimeAdapterResult.success(EmscriptValue.NullValue),
        ).run(runtimeDocument())
        assertTrue(absentResult is EmscriptDryRunResult.Success)
        absentResult as EmscriptDryRunResult.Success
        assertTrue(listOf("image", "text", "savedMarker").all {
            absentResult.variables[it] === EmscriptValue.NullValue
        })

        val failure = runtime(
            findTemplate = RuntimeAdapterResult.failure(
                RuntimeQueryDiagnosticCodes.TEMPLATE_MATCH_FAILED,
                "capture failed",
            ),
            findText = RuntimeAdapterResult.success(textMatch),
            markerLoad = RuntimeAdapterResult.success(marker),
        ).run(runtimeDocument())
        assertTrue(failure is EmscriptDryRunResult.Failure)
        failure as EmscriptDryRunResult.Failure
        assertTrue(failure.events.any { it.diagnosticCode == RuntimeQueryDiagnosticCodes.TEMPLATE_MATCH_FAILED })

        val invalid = runtime(
            findTemplate = RuntimeAdapterResult.success(EmscriptValue.StringValue("not a match")),
            findText = RuntimeAdapterResult.success(textMatch),
            markerLoad = RuntimeAdapterResult.success(marker),
        ).run(runtimeDocument())
        assertTrue(invalid is EmscriptDryRunResult.Failure)
        invalid as EmscriptDryRunResult.Failure
        assertTrue(invalid.events.any { it.diagnosticCode == RuntimeQueryDiagnosticCodes.VISION_RESULT_INVALID })
    }

    @Test
    fun `perception provider failures retain command specific diagnostics`() = runBlocking {
        val cases = listOf(
            Triple(
                RuntimeAdapterResult.failure(RuntimeQueryDiagnosticCodes.VISION_CAPTURE_FAILED, "capture"),
                RuntimeAdapterResult.success(textMatch),
                RuntimeAdapterResult.success(marker),
            ) to RuntimeQueryDiagnosticCodes.VISION_CAPTURE_FAILED,
            Triple(
                RuntimeAdapterResult.success(imageMatch),
                RuntimeAdapterResult.failure(RuntimeQueryDiagnosticCodes.OCR_FAILED, "ocr"),
                RuntimeAdapterResult.success(marker),
            ) to RuntimeQueryDiagnosticCodes.OCR_FAILED,
            Triple(
                RuntimeAdapterResult.success(imageMatch),
                RuntimeAdapterResult.success(textMatch),
                RuntimeAdapterResult.failure(RuntimeQueryDiagnosticCodes.MARKER_REPOSITORY_UNAVAILABLE, "repository"),
            ) to RuntimeQueryDiagnosticCodes.MARKER_REPOSITORY_UNAVAILABLE,
            Triple(
                RuntimeAdapterResult.success(imageMatch),
                RuntimeAdapterResult.success(textMatch),
                RuntimeAdapterResult.failure(RuntimeQueryDiagnosticCodes.MARKER_DECODE_FAILED, "decode"),
            ) to RuntimeQueryDiagnosticCodes.MARKER_DECODE_FAILED,
        )

        cases.forEach { (results, diagnosticCode) ->
            val result = runtime(results.first, results.second, results.third).run(runtimeDocument())
            assertTrue("$diagnosticCode -> $result", result is EmscriptDryRunResult.Failure)
            result as EmscriptDryRunResult.Failure
            assertTrue(result.events.any { it.diagnosticCode == diagnosticCode })
        }
    }

    @Test
    fun `text match selection is deterministic and absence is not failure`() {
        val candidates = listOf(
            RuntimeTextMatchCandidate("later", "Login", pixelRegion, 0.7, "ocr", 20),
            RuntimeTextMatchCandidate("higher", "Login button", pixelRegion, 0.99, "ocr", 30),
            RuntimeTextMatchCandidate("exact-high", "LOGIN", pixelRegion, 0.8, "a11y", 10),
            RuntimeTextMatchCandidate("exact-new", "Login", pixelRegion, 0.8, "ocr", 40),
        )
        val found = TextMatchQueryRuntime.findBest("login", candidates)
        assertTrue(found.success)
        assertEquals("Login", (found.value as EmscriptValue.TextMatchValue).text)
        assertEquals("ocr", found.value.source)

        val absent = TextMatchQueryRuntime.findBest("missing", candidates)
        assertTrue(absent.success)
        assertTrue(absent.value === EmscriptValue.NullValue)

        val invalid = TextMatchQueryRuntime.findBest(" ", candidates)
        assertFalse(invalid.success)
        assertEquals(RuntimeQueryDiagnosticCodes.VISION_RESULT_INVALID, invalid.diagnosticCode)
    }

    private fun runtimeDocument() = (guard.preview(
        """
        LET image:ImageMatch? = findTemplate("login.png", 0.8, 1000)
        LET text:TextMatch? = findText("Login", 1000)
        LET savedMarker:Marker? = markerLoad("login")
        """.trimIndent(),
    ) as EmscriptApplyGuardResult.Success).importedDocument

    private fun runtime(
        findTemplate: RuntimeAdapterResult,
        findText: RuntimeAdapterResult,
        markerLoad: RuntimeAdapterResult,
    ) = WorkspaceBasicRuntime(
        environment = WorkspaceBasicRuntimeEnvironment(
            delayMs = {},
            playBeep = { _, _, _ -> },
            vibrate = {},
            log = {},
            findTemplate = { _, _, _, _ -> findTemplate },
            findText = { _, _ -> findText },
            markerLoad = { markerLoad },
        ),
    )

    private companion object {
        val COMMAND_IDS = setOf("action.findTemplate", "vision.findText", "vision.markerLoad")
        val RETURN_TYPES = mapOf(
            "action.findTemplate" to "ImageMatch?",
            "vision.findText" to "TextMatch?",
            "vision.markerLoad" to "Marker?",
        )
    }
}
