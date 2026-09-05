package com.visualtasker.wss.emscript.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmscriptDryRunRuntimeTest {
    @Test
    fun `dry run executes variables control flow and safe commands`() {
        val script = """
            SET loopIndex = 0
            SET total = 0
            LOOP 3
                SET loopIndex = loopIndex + 1
                IF loopIndex == 1
                    log("first")
                ELSEIF loopIndex == 2
                    beep(880, 100, 50)
                ELSE
                    vibrate(0, 40, 20, 40)
                END IF
                SET total = total + loopIndex
            END LOOP
            WHILE loopIndex < 5
                SET loopIndex = loopIndex + 1
                wait(10)
            END WHILE
            click("Done")
        """.trimIndent()

        val result = EmscriptDryRunRuntime().run(script)

        assertTrue(result is EmscriptDryRunResult.Success)
        result as EmscriptDryRunResult.Success
        assertEquals(EmscriptValue.NumberValue(5.0), result.variables["loopIndex"])
        assertEquals(EmscriptValue.NumberValue(6.0), result.variables["total"])
        assertTrue(result.events.any { it.kind == "log" && it.message == "first" })
        assertTrue(result.events.any { it.kind == "beep" })
        assertTrue(result.events.any { it.kind == "vibrate" })
        assertTrue(result.events.any { it.kind == "wait" })
        assertTrue(result.events.any { it.kind == "click" && it.message.contains("würde Text \"Done\"") })
    }

    @Test
    fun `dry run stops runaway while loops`() {
        val result = EmscriptDryRunRuntime(
            config = EmscriptDryRunConfig(maxSteps = 50, maxLoopIterations = 3),
        ).run(
            """
                WHILE true
                    log("tick")
                END WHILE
            """.trimIndent(),
        )

        assertTrue(result is EmscriptDryRunResult.Failure)
        result as EmscriptDryRunResult.Failure
        assertTrue(result.message.contains("WHILE"))
    }

    @Test
    fun `dry run marks only missing catalog adapters as capability warnings`() {
        val result = EmscriptDryRunRuntime().run(
            """
                findTemplate("button.png", 0.8, 1000)
                markerSave("button", region(10, 20, 30, 40), "region", 0.90)
                templateCompare("buttonTpl", region(10, 20, 30, 40), "grayscale")
                Termux.shell("echo ok")
            """.trimIndent(),
        )

        assertTrue(result is EmscriptDryRunResult.Success)
        result as EmscriptDryRunResult.Success
        assertFalse(result.events.any { it.kind == "capability" && it.command == "findTemplate" })
        assertFalse(result.events.any { it.kind == "capability" && it.command == "markerSave" })
        assertFalse(result.events.any { it.kind == "capability" && it.command == "templateCompare" })
        assertTrue(result.events.any {
            it.kind == "capability" &&
                it.command == "Termux.shell" &&
                it.pluginOwner == "visualtasker.termux"
        })
    }
}
