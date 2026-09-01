package com.visualtasker.wss.emscript.editor

object EditorDefaults {
    const val integrationTestScriptVersion: Int = 4

    val sampleScript: String = """
        LET v1 = 1
        LET v2 = 2
        LET v3 = 3
        IF (v1 + v2) >= v3
            log("ok")
        END IF
    """.trimIndent()

    val integrationTestScript: String = """
        LET loopIndex = 0
        LET thresholdLow = 3
        LET thresholdHigh = 7
        LET score = 0
        LET nestedScore = 0
        LET result = 0

        log("integration-start")
        wait(100)
        click("Start")

        LOOP 10
            SET loopIndex = loopIndex + 1
            SET score = score + loopIndex
            log("loop tick")
            wait(25)

            IF (score + loopIndex) < thresholdLow
                SET result = score + 1
                beep(880, 80, 65)
                click("low branch")
            ELSEIF (score + loopIndex) >= thresholdHigh
                SET result = score * 2
                vibrate(40)
                log("high branch")

                IF (result + loopIndex) >= 20
                    SET nestedScore = result + loopIndex
                    beep(660, 60, 45)
                    click("nested high")
                ELSE
                    SET nestedScore = result - 1
                    vibrate(0, 30, 20, 30)
                    wait(30)
                END IF
            ELSE
                SET result = score + thresholdLow
                beep()
                log("middle branch")

                IF (result + nestedScore) != thresholdHigh
                    SET nestedScore = nestedScore + 2
                    vibrate(25)
                ELSE
                    SET nestedScore = nestedScore + 1
                    beep(440, 60, 35)
                    click("fallback")
                END IF
            END IF
        END LOOP

        WHILE loopIndex < 12
            SET loopIndex = loopIndex + 1
            log("while tick")
            wait(20)
        END WHILE

        log("integration-end")
    """.trimIndent()

    val fallbackBranchTestScript: String = """
        LET result = 3
        LET nestedScore = 4
        LET thresholdHigh = 7

        log("fallback-test-start")
        IF result > 10
            log("outer-then")
        ELSE
            log("outer-else")
            IF (result + nestedScore) != thresholdHigh
                log("nested-then")
            ELSE
                SET nestedScore = nestedScore + 1
                beep(440, 60, 35)
                click("fallback")
            END IF
        END IF
        log("fallback-test-end")
    """.trimIndent()

    val elseifBranchTestScript: String = """
        LET score = 8
        LET thresholdLow = 3
        LET thresholdHigh = 7

        log("elseif-test-start")
        IF score < thresholdLow
            log("low")
        ELSEIF score >= thresholdHigh
            vibrate(40)
            log("high")
        ELSE
            beep()
            log("middle")
        END IF
        log("elseif-test-end")
    """.trimIndent()

    val allSamples: Map<String, String> = mapOf(
        "Referenz" to sampleScript,
        "Loop" to """
            SET i = 0
            LOOP 3
                SET i = i + 1
                log(i)
            END LOOP
        """.trimIndent(),
        "Integrationstest" to integrationTestScript,
        "Branch: ElseIf" to elseifBranchTestScript,
        "Branch: Fallback" to fallbackBranchTestScript,
    )
}
