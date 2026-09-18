package com.visualtasker.wss.emscript.editor

import com.visualtasker.wss.emscript.parser.EmscriptParserSlice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmsScriptFormatterTest {
    @Test
    fun commandLinesDoNotReceiveArtificialIndentation() {
        val formatted = EmsScriptFormatter.format("wait(10)\nlog(\"ready now\")")

        assertEquals("wait(10)\nlog(\"ready now\")", formatted)
    }

    @Test
    fun keywordAndBraceControlFlowFormatsDeterministically() {
        val keywordSource = "IF true\nlog(\"yes\")\nELSEIF false\nwait(1)\nEND IF"
        val braceSource = "if (true) {\nlog(\"yes\")\n} else {\nwait(1)\n}"

        assertEquals(
            "IF true\n    log(\"yes\")\nELSE IF false\n    wait(1)\nEND IF",
            EmsScriptFormatter.format(keywordSource),
        )
        assertEquals(
            "IF (true) {\n    log(\"yes\")\n} else {\n    wait(1)\n}",
            EmsScriptFormatter.format(braceSource),
        )
    }

    @Test
    fun stableV1SuitesRemainParseableAndFormattingIsIdempotent() {
        EditorDefaults.stableV1TestSuite.forEach { (name, source) ->
            val formatted = EmsScriptFormatter.format(source)
            assertFalse("$name starts indented", formatted.lineSequence().first().startsWith(' '))
            assertEquals("$name formatter is not idempotent", formatted, EmsScriptFormatter.format(formatted))

            val parsed = EmscriptParserSlice().parse(formatted)
            assertTrue(
                "$name parser: ${parsed.issues.joinToString { "${it.line}:${it.column} ${it.message}" }}",
                parsed.isSuccess,
            )
        }
    }
}
