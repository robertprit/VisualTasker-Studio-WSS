package com.visualtasker.wss.emscript.editor

object EditorDefaults {
    const val integrationTestScriptVersion: Int = 7

    val sampleScript: String = """
        LET v1 = 1
        LET v2 = 2
        LET v3 = 3
        IF (v1 + v2) >= v3
            log("ok")
        END IF
    """.trimIndent()

    val integrationTestScript: String = """
        REM @vt.group.start id="vars:init" label="Variablen initialisieren" kind="variable-bulk"
        LET loopIndex = 0
        LET thresholdLow = 3
        LET thresholdHigh = 7
        LET score = 0
        LET nestedScore = 0
        LET result = 0
        REM @vt.group.end id="vars:init"

        log("integration-start")
        wait(100)
        click("Start")

        REM @vt.group.start id="flow:main-loop" label="Hauptschleife" kind="loop-region"
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
        REM @vt.group.end id="flow:main-loop"

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

    val commandCatalogBreadthTestScript: String = """
        REM @vt.group.start id="core:variables" label="Core Variablen" kind="variable-bulk"
        LET catalogIndex = 0
        LET thresholdLow = 2
        LET thresholdHigh = 6
        LET result = 0
        REM @vt.group.end id="core:variables"

        log("core-runtime-start")
        wait(50)
        click("Start")
        clickPoint(120, 240, 1)
        swipe([120, 640, 120, 220], 1)
        screenshot("core-screen.png")
        Clipboard.set("visualtasker")
        Clipboard.get()
        File.writeText("core-runtime.txt", "hello")
        File.readText("core-runtime.txt")
        Cache.clear()
        Sys.info()
        Env.get("ANDROID_VERSION")

        LOOP 3
            SET catalogIndex = catalogIndex + 1
            SET result = result + catalogIndex
            log("core-loop")
            beep(880, 80, 60)
            vibrate(0, 30, 20, 30)

            IF (result + catalogIndex) < thresholdLow
                SET result = result + 1
                log("low branch")
                click("low branch")
            ELSEIF (result + catalogIndex) >= thresholdHigh
                SET result = result * 2
                vibrate(40)
                log("high branch")

                IF result >= 8
                    SET result = result + catalogIndex
                    beep(660, 60, 45)
                    clickPoint(160, 260, 1)
                ELSE
                    SET result = result - 1
                    wait(30)
                END IF
            ELSE
                SET result = result + thresholdLow
                beep()
                log("middle branch")

                IF (catalogIndex + result) != thresholdHigh
                    SET result = result + 2
                    vibrate(25)
                ELSE
                    SET result = result + 1
                    beep(440, 60, 35)
                    click("fallback")
                END IF
            END IF
        END LOOP

        WHILE catalogIndex < 5
            SET catalogIndex = catalogIndex + 1
            SET result = result + catalogIndex
            log("core-while")
            wait(20)
        END WHILE

        log("core-runtime-end")
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
        "Katalog: Breite" to commandCatalogBreadthTestScript,
        "Branch: ElseIf" to elseifBranchTestScript,
        "Branch: Fallback" to fallbackBranchTestScript,
    )
}
