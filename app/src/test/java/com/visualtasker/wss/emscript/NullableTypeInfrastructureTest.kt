package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.parser.EmscriptIrStatement
import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunRuntime
import com.visualtasker.wss.emscript.runtime.EmscriptRuntimeTypeSafety
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.NullableQuerySemanticsAudit
import de.visualtasker.blockeditor.registry.QueryReturnContractAudit
import de.visualtasker.blockeditor.registry.LegacyCommandDefinitionBridge
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import de.visualtasker.blockeditor.serialization.WorkspaceSerializer
import de.visualtasker.emscript.contract.LanguageTypeCompatibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NullableTypeInfrastructureTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun typedLetParsesImportsSerializesAndRegeneratesLosslessly() {
        val source = "LET value:String? = \"ok\"\nSET value = \"next\""
        val parsed = EmscriptParserSlice().parse(source)
        assertTrue(parsed.issues.toString(), parsed.isSuccess)
        val declaration = parsed.ir!!.statements.first() as EmscriptIrStatement.Let
        assertEquals("String?", LanguageTypeCompatibility.sourceName(requireNotNull(declaration.declaredType)))

        val result = guard.preview(source)
        assertTrue(result.toString(), result is EmscriptApplyGuardResult.Success)
        result as EmscriptApplyGuardResult.Success
        val document = result.importedDocument
        assertEquals("String?", document.variables.variables.getValue("value").type)
        assertTrue(result.serializedWorkspaceJson.contains("String?"))
        assertEquals("String?", WorkspaceSerializer.deserialize(result.serializedWorkspaceJson)
            .variables.variables.getValue("value").type)

        val registry = CompositeBlockRegistry().apply {
            document.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)
        assertTrue(generated, generated.contains("let value:String? = \"ok\";"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)
    }

    @Test
    fun allCoreNullableSourceTypesParseGenerically() {
        val source = """
            LET text:String? = "value"
            LET count:Number? = 1
            LET flag:Bool? = true
            LET anything:Any? = "value"
        """.trimIndent()

        val result = EmscriptParserSlice().parse(source)

        assertTrue(result.issues.toString(), result.isSuccess)
        assertEquals(
            listOf("String?", "Number?", "Bool?", "Any?"),
            result.ir!!.statements.filterIsInstance<EmscriptIrStatement.Let>().map {
                LanguageTypeCompatibility.sourceName(requireNotNull(it.declaredType))
            },
        )
    }

    @Test
    fun nullableToNonNullAssignmentHasStructuredDiagnostic() {
        val result = guard.preview("LET maybe:String? = \"x\"\nLET strict:String = maybe")

        assertTrue(result.toString(), result is EmscriptApplyGuardResult.Failure)
        assertEquals("NULLABLE_TO_NONNULL_ASSIGNMENT", (result as EmscriptApplyGuardResult.Failure).diagnosticCode)
    }

    @Test
    fun nullableBoolIsRejectedAsControlCondition() {
        val source = """
            LET flag:Bool? = true
            IF flag
                log("yes")
            END IF
        """.trimIndent()
        val result = guard.preview(source)

        assertTrue(result.toString(), result is EmscriptApplyGuardResult.Failure)
        assertEquals("NULLABLE_VALUE_IN_NONNULL_CONTEXT", (result as EmscriptApplyGuardResult.Failure).diagnosticCode)
        assertTrue(EmscriptDryRunRuntime().run(source) is EmscriptDryRunResult.Failure)
    }

    @Test
    fun runtimeSeparatesEmptyAbsentAndFailure() {
        assertTrue(EmscriptRuntimeTypeSafety.matches(EmscriptValue.StringValue(""), "String"))
        assertTrue(EmscriptRuntimeTypeSafety.matches(EmscriptValue.NullValue, "String?"))
        assertFalse(EmscriptRuntimeTypeSafety.matches(EmscriptValue.NullValue, "String"))
        val failure = runCatching {
            EmscriptRuntimeTypeSafety.requireMatches(EmscriptValue.NullValue, "String", "test")
        }.exceptionOrNull()
        assertTrue(failure?.message.orEmpty().contains("NULLABLE_VALUE_IN_NONNULL_CONTEXT"))
    }

    @Test
    fun nullableCoreQueriesTemplateCompareAndChromeTabLeaveSixteenDQueryReturnEntries() {
        val decisions = QueryReturnContractAudit.ALL
        val bridge = LegacyCommandDefinitionBridge().analyze()
        assertEquals(12, decisions.size)
        assertEquals(12, QueryReturnContractAudit.currentDQueryIds(bridge).size)
        listOf("clipboard.get", "system.info", "system.env").forEach { id ->
            assertEquals("String", VisualTaskerCommandCatalog.findById(id)?.returnType)
        }
        NullableQuerySemanticsAudit.ALL.forEach { decision ->
            assertEquals("String?", VisualTaskerCommandCatalog.findById(decision.stableId)?.returnType)
        }
        assertFalse(
            EmscriptParserSlice().parse("LET x = null").isSuccess,
        )
    }
}
