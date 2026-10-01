package com.visualtasker.wss.emscript

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.runtime.DatastoreGetQueryRuntime
import com.visualtasker.wss.emscript.runtime.EmscriptDryRunResult
import com.visualtasker.wss.emscript.runtime.EmscriptRuntimeDiagnosticException
import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.FileReadTextQueryRuntime
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntime
import com.visualtasker.wss.emscript.runtime.WorkspaceBasicRuntimeEnvironment
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.blockeditor.ir.IrGenerator
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.CommandCatalogKind
import de.visualtasker.blockeditor.registry.CompositeBlockRegistry
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.blockeditor.registry.QueryReturnContractAudit
import de.visualtasker.blockeditor.registry.VariableReporterFactory
import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import java.io.IOException
import java.nio.file.AccessDeniedException
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NullableCoreQueryConvergenceTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun productionContractsAreNullableReportersAndAuditReflectsLaterChromeTabMigration() {
        listOf("file.readText", "system.datastoreGet").forEach { id ->
            val entry = requireNotNull(VisualTaskerCommandCatalog.findById(id))
            val block = requireNotNull(DefaultBlockRegistry.getDefinition(requireNotNull(entry.block).blockType))

            assertEquals(CommandCatalogKind.REPORTER, entry.kind)
            assertEquals("String?", entry.returnType)
            assertTrue(block.isReporter)
            assertEquals("String?", block.outputType)
            assertFalse(block.hasPrevious)
            assertFalse(block.hasNext)
        }
        assertEquals(12, QueryReturnContractAudit.ALL.size)
    }

    @Test
    fun sourceWorkspaceIrAndSerializerPreserveBothNullableQueries() {
        val source = """
            LET fileValue:String? = File.readText("state.txt")
            LET storeValue:String? = datastoreGet("score")
            SET fileValue = File.readText("next.txt")
            SET storeValue = datastoreGet("next")
        """.trimIndent()
        val preview = guard.preview(source) as EmscriptApplyGuardResult.Success
        val document = preview.importedDocument

        assertEquals("String?", document.variables.variables.getValue("fileValue").type)
        assertEquals("String?", document.variables.variables.getValue("storeValue").type)
        val queryBlocks = document.blocks.values.filter {
            it.type in setOf(
                BlockTypes.EMSCRIPT_COMMAND_PREFIX + "file.readText",
                BlockTypes.EMSCRIPT_COMMAND_PREFIX + "system.datastoreGet",
            )
        }
        assertEquals(4, queryBlocks.size)

        val registry = CompositeBlockRegistry().apply {
            document.variables.variables.values.forEach { register(VariableReporterFactory.create(it)) }
        }
        val generated = EmscriptGenerator(IrGenerator(registry)).generate(document)

        assertTrue(generated, generated.contains("let fileValue:String? = File.readText(\"state.txt\");"))
        assertTrue(generated, generated.contains("let storeValue:String? = datastoreGet(\"score\");"))
        assertTrue(generated, generated.contains("set fileValue = File.readText(\"next.txt\");"))
        assertTrue(generated, generated.contains("set storeValue = datastoreGet(\"next\");"))
        assertTrue(guard.preview(generated) is EmscriptApplyGuardResult.Success)
    }

    @Test
    fun nullableQueriesCannotFlowIntoNonNullVariablesOrParameters() {
        val letResult = guard.preview("LET value:String = File.readText(\"state.txt\")")
        val setResult = guard.preview(
            """
                LET value:String = "seed"
                SET value = datastoreGet("score")
            """.trimIndent(),
        )
        val argumentResult = guard.preview("log(File.readText(\"state.txt\"))")

        assertEquals(
            "NULLABLE_TO_NONNULL_ASSIGNMENT",
            (letResult as EmscriptApplyGuardResult.Failure).diagnosticCode,
        )
        assertEquals(
            "NULLABLE_TO_NONNULL_ASSIGNMENT",
            (setResult as EmscriptApplyGuardResult.Failure).diagnosticCode,
        )
        assertEquals(
            "NULLABLE_ARGUMENT_TO_NONNULL_PARAMETER",
            (argumentResult as EmscriptApplyGuardResult.Failure).diagnosticCode,
        )
    }

    @Test
    fun fileRuntimeDistinguishesValueEmptyAbsentInvalidPermissionAndIoFailure() {
        val root = createTempDirectory("emscript-file-query").toFile()
        val content = root.resolve("content.txt").apply { writeText("abc") }
        val empty = root.resolve("empty.txt").apply { writeText("") }
        val missing = root.resolve("missing.txt")
        val directory = root.resolve("folder").apply { mkdirs() }

        assertEquals("abc", FileReadTextQueryRuntime.read(content.path, resolve = { java.io.File(it) }))
        assertEquals("", FileReadTextQueryRuntime.read(empty.path, resolve = { java.io.File(it) }))
        assertEquals(null, FileReadTextQueryRuntime.read(missing.path, resolve = { java.io.File(it) }))
        assertEquals(null, FileReadTextQueryRuntime.read(directory.path, resolve = { java.io.File(it) }))
        assertDiagnostic(RuntimeQueryDiagnosticCodes.FILE_INVALID_PATH) {
            FileReadTextQueryRuntime.read("invalid", resolve = { null })
        }
        assertDiagnostic(RuntimeQueryDiagnosticCodes.FILE_READ_PERMISSION_DENIED) {
            FileReadTextQueryRuntime.read(
                content.path,
                resolve = { java.io.File(it) },
                readText = { throw AccessDeniedException(it.path) },
            )
        }
        assertDiagnostic(RuntimeQueryDiagnosticCodes.FILE_READ_FAILED) {
            FileReadTextQueryRuntime.read(
                content.path,
                resolve = { java.io.File(it) },
                readText = { throw IOException("simulated") },
            )
        }
    }

    @Test
    fun datastoreRuntimeDistinguishesPresentEmptyMissingAndLoadFailure() {
        val values = mapOf("value" to "abc", "empty" to "")

        assertEquals("abc", DatastoreGetQueryRuntime.get(values, null, "value"))
        assertEquals("", DatastoreGetQueryRuntime.get(values, null, "empty"))
        assertEquals(null, DatastoreGetQueryRuntime.get(values, null, "missing"))
        assertEquals(null, DatastoreGetQueryRuntime.get(emptyMap(), null, "missing"))
        assertDiagnostic(RuntimeQueryDiagnosticCodes.DATASTORE_LOAD_FAILED) {
            DatastoreGetQueryRuntime.get(emptyMap(), IOException("broken store"), "missing")
        }
    }

    @Test
    fun liveWorkspaceRuntimeTransportsValuesAbsenceAndStructuredFailure() = runBlocking {
        val source = """
            LET fileValue:String? = File.readText("state.txt")
            LET storeValue:String? = datastoreGet("score")
        """.trimIndent()
        val document = (guard.preview(source) as EmscriptApplyGuardResult.Success).importedDocument
        val present = runtime(fileValue = "", datastoreValue = "42").run(document)
        val absent = runtime(fileValue = null, datastoreValue = null).run(document)
        val failed = runtime(fileFailure = IOException("read failed")).run(document)
        val datastoreFailed = runtime(datastoreFailure = IOException("store failed")).run(document)

        present as EmscriptDryRunResult.Success
        assertEquals(EmscriptValue.StringValue(""), present.variables["fileValue"])
        assertEquals(EmscriptValue.StringValue("42"), present.variables["storeValue"])
        absent as EmscriptDryRunResult.Success
        assertEquals(EmscriptValue.NullValue, absent.variables["fileValue"])
        assertEquals(EmscriptValue.NullValue, absent.variables["storeValue"])
        failed as EmscriptDryRunResult.Failure
        assertTrue(failed.events.any {
            it.diagnosticCode == RuntimeQueryDiagnosticCodes.FILE_READ_FAILED &&
                it.kind == "runtime_failure"
        })
        datastoreFailed as EmscriptDryRunResult.Failure
        assertTrue(datastoreFailed.events.any {
            it.diagnosticCode == RuntimeQueryDiagnosticCodes.DATASTORE_LOAD_FAILED &&
                it.kind == "runtime_failure"
        })
    }

    private fun runtime(
        fileValue: String? = null,
        datastoreValue: String? = null,
        fileFailure: Throwable? = null,
        datastoreFailure: Throwable? = null,
    ) = WorkspaceBasicRuntime(
        environment = WorkspaceBasicRuntimeEnvironment(
            delayMs = {},
            playBeep = { _, _, _ -> },
            vibrate = {},
            log = {},
            fileReadText = {
                fileFailure?.let {
                    throw EmscriptRuntimeDiagnosticException(
                        RuntimeQueryDiagnosticCodes.FILE_READ_FAILED,
                        "simulated file read failure",
                        it,
                    )
                }
                fileValue
            },
            datastoreGet = {
                datastoreFailure?.let {
                    throw EmscriptRuntimeDiagnosticException(
                        RuntimeQueryDiagnosticCodes.DATASTORE_LOAD_FAILED,
                        "simulated datastore load failure",
                        it,
                    )
                }
                datastoreValue
            },
        ),
    )

    private fun assertDiagnostic(code: String, block: () -> Unit) {
        val failure = runCatching(block).exceptionOrNull() as EmscriptRuntimeDiagnosticException
        assertEquals(code, failure.diagnosticCode)
    }
}
