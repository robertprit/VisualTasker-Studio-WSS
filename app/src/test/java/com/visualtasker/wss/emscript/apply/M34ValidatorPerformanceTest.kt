package com.visualtasker.wss.emscript.apply

import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.semantics.validation.Validator
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class M34ValidatorPerformanceTest {
    private val guard = EmscriptApplyGuard()

    @Test
    fun validatorPerformanceMatrixRemainsReproducible() {
        val rows = mutableListOf<String>()
        rows += "statements,path,change,apply_ms,validation_median_ms,validation_max_ms,blocks"

        listOf(40, 80, 160, 320).forEach { statementCount ->
            val legacySource = (1..statementCount).joinToString("\n") { index -> "wait($index)" }
            val before = success(legacySource).importedDocument
            val anchoredSource = EmscriptGenerator().generate(before)
            val scenarios = listOf(
                Scenario("legacy", "format-only", "// format-only\n\n$legacySource"),
                Scenario("legacy", "structural", "$legacySource\nwait(${statementCount + 1})"),
                Scenario("anchor", "format-only", "// format-only\n\n$anchoredSource"),
                Scenario("anchor", "structural", "$anchoredSource\nwait(${statementCount + 1})"),
            )

            scenarios.forEach { scenario ->
                var applied: EmscriptApplyGuardResult.Success? = null
                val applyNanos = measureNanoTime {
                    applied = success(scenario.source, before)
                }
                val document = requireNotNull(applied).importedDocument
                assertTrue(Validator.validate(document).errors.toString(), Validator.validate(document).isValid)
                Validator.validate(document)
                val validationSamples = List(5) {
                    measureNanoTime { Validator.validate(document) }
                }.sorted()
                val medianMs = validationSamples[validationSamples.size / 2] / NANOS_PER_MILLI
                val maxMs = validationSamples.last() / NANOS_PER_MILLI
                rows += listOf(
                    statementCount,
                    scenario.path,
                    scenario.change,
                    formatMs(applyNanos / NANOS_PER_MILLI),
                    formatMs(medianMs),
                    formatMs(maxMs),
                    document.blocks.size,
                ).joinToString(",")

                if (statementCount == 320) {
                    assertTrue(
                        "Validator regression for ${scenario.path}/${scenario.change}: ${formatMs(medianMs)} ms",
                        medianMs < 250.0,
                    )
                }
            }
        }

        println("M3_4_VALIDATOR_PROFILE_START")
        rows.forEach(::println)
        println("M3_4_VALIDATOR_PROFILE_END")
    }

    private fun success(
        source: String,
        previous: de.visualtasker.workflow.core.WorkspaceDocument? = null,
    ): EmscriptApplyGuardResult.Success {
        val result = guard.preview(source, previousDocument = previous)
        assertTrue(result.toString(), result is EmscriptApplyGuardResult.Success)
        return result as EmscriptApplyGuardResult.Success
    }

    private fun formatMs(value: Double): String = "%.3f".format(java.util.Locale.ROOT, value)

    private data class Scenario(
        val path: String,
        val change: String,
        val source: String,
    )

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000.0
    }
}
