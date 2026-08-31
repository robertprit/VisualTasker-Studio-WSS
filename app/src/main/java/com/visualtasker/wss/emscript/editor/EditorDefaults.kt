package com.visualtasker.wss.emscript.editor

object EditorDefaults {
    val sampleScript: String = """
        LET v1 = 1
        LET v2 = 2
        LET v3 = 3
        IF (v1 + v2) >= v3
            OUTPUT "ok"
        END IF
    """.trimIndent()

    val allSamples: Map<String, String> = mapOf(
        "Referenz" to sampleScript,
        "Loop" to """
            SET i = 0
            LOOP 3
                SET i = i + 1
                OUTPUT i
            END LOOP
        """.trimIndent(),
    )
}
