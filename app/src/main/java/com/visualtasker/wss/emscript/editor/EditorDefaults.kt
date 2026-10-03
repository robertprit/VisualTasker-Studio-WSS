package com.visualtasker.wss.emscript.editor

object EditorDefaults {
    const val integrationTestScriptVersion: Int = 21

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
        rem.variableBulk("Runtime variables", "grid-horizontal", "loopIndex score nestedScore result")
        rem.variableBulk("Threshold variables", "grid-vertical", "thresholdLow thresholdHigh")

        rem.region("Runtime setup", "facet", "auto")
        log("integration-start")
        wait(100)
        click("Start")
        screenshot("screenshots/integration-live.png")
        datastorePut("integration.score", "0")
        LET integrationScoreText:String? = datastoreGet("integration.score")
        markerSave("integrationRegion", region(10, 20, 240, 160), "region", 0.85)
        LET integrationMarker:Marker? = markerLoad("integrationRegion")
        templateDefine("integrationTemplate", region(10, 20, 240, 160), "grayscale")
        LET integrationScore:Number = templateCompare("integrationTemplate", region(10, 20, 240, 160), "grayscale")
        LET integrationMatch:ImageMatch? = findTemplate("integrationTemplate.png", 0.80, 1000, 1, region(10, 20, 240, 160))

        rem.flowBreak("Main loop", "right")
        rem.group("Main loop", true)
        REM @vt.group.start id="flow:main-loop" label="Hauptschleife" kind="loop-region"
        LOOP 10
            SET loopIndex = loopIndex + 1
            SET score = score + loopIndex
            datastorePut("integration.score", "loop-updated")
            log("loop tick")
            wait(25)

            rem.expressionCapsule("Loop condition", "collapse")
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

        rem.offPageOut("TAIL")
        rem.offPageIn("TAIL")
        rem.layoutHint("vertical", "next")
        markerDelete("integrationRegion")

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
        log("interactive-input-actions-skipped")
        datastorePut("catalog.result", "catalog-ready")
        LET catalogStoredResult:String? = datastoreGet("catalog.result")
        markerSave("catalogRegion", region(10, 20, 240, 160), "region", 0.85)
        LET catalogMarker:Marker? = markerLoad("catalogRegion")
        templateDefine("catalogTemplate", region(10, 20, 240, 160), "grayscale")
        LET catalogScore:Number = templateCompare("catalogTemplate", region(10, 20, 240, 160), "grayscale")
        LET catalogMatch:ImageMatch? = findTemplate("catalogTemplate.png", 0.80, 1000, 1, region(10, 20, 240, 160))
        Clipboard.set("visualtasker")
        log(Clipboard.get())
        File.writeText("core-runtime.txt", "hello")
        LET catalogFileText:String? = File.readText("core-runtime.txt")
        Cache.clear()
        log(Sys.info())
        log(Env.get("ANDROID_VERSION"))
        LET catalogShizukuInstalled:Bool = shizuku.isInstalled()
        Shizuku.permissionState()
        LET catalogShizukuAvailable:Bool = shizuku.isAvailable()
        LET catalogShizukuUid:Number? = shizuku.getUid()
        LET catalogTaskerEnabled:Bool = tasker.isEnabled()
        LET catalogTaskerValue:String? = tasker.getVariable("%VT_TEST")
        LET catalogTaskerVariables:List<TaskerVariable> = tasker.getVariables("%VT_*")
        LET catalogTermuxSummary:String? = termux.get("summary")
        LET catalogScrcpyRunning:Bool = scrcpy.isRunning()
        LET catalogScrcpyState:String? = scrcpy.get("state")
        LET catalogChartExists:Bool = chart.exists("wss-demo-line")
        LET catalogChartSnapshot:ChartSnapshot? = chart.get("wss-demo-line")
        Shizuku.systemService("package")
        Shizuku.call("package", "1", ["s16", "com.visualtasker.wss"])
        Shizuku.shell("id")
        Shizuku.exec("cmd package list packages com.visualtasker.wss")

        LOOP 3
            SET catalogIndex = catalogIndex + 1
            SET result = result + catalogIndex
            log("core-loop")
            beep(880, 80, 60)
            vibrate(0, 30, 20, 30)

            IF (result + catalogIndex) < thresholdLow
                SET result = result + 1
                log("low branch")
            ELSEIF (result + catalogIndex) >= thresholdHigh
                SET result = result * 2
                vibrate(40)
                log("high branch")

                IF result >= 8
                    SET result = result + catalogIndex
                    beep(660, 60, 45)
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
                END IF
            END IF
        END LOOP

        WHILE catalogIndex < 5
            SET catalogIndex = catalogIndex + 1
            SET result = result + catalogIndex
            log("core-while")
            wait(20)
        END WHILE

        markerDelete("catalogRegion")

        log("core-runtime-end")
    """.trimIndent()

    val nestedFlowStressTestScript: String = """
        REM @vt.group.start id="vars:stress-globals" label="Stress Variablen" kind="variable-bulk"
        LET outerIndex = 0
        LET innerIndex = 0
        LET retryIndex = 0
        LET phase = 0
        LET score = 0
        LET branchScore = 0
        LET thresholdLow = 4
        LET thresholdMid = 11
        LET thresholdHigh = 24
        LET hitCount = 0
        LET drift = 0
        LET guard = 0
        REM @vt.group.end id="vars:stress-globals"

        log("nested-stress-start")
        wait(40)
        screenshot("stress-start.png")
        datastorePut("stress.score", "initial")
        datastorePut("stress.phase", "initial")
        markerSave("stressRegion", region(16, 24, 320, 180), "region", 0.82)
        templateDefine("stressTemplate", region(16, 24, 320, 180), "grayscale")

        REM @vt.group.start id="flow:outer-loop" label="Outer Loop" kind="loop-region"
        LOOP 5
            SET outerIndex = outerIndex + 1
            SET phase = outerIndex
            SET innerIndex = 0
            SET retryIndex = 0
            SET score = score + outerIndex
            datastorePut("stress.outer", "outer-updated")
            log("outer tick")

            IF score < thresholdLow
                SET branchScore = score + 1
                beep(660, 45, 45)
                click("outer-low")

                LOOP 2
                    SET innerIndex = innerIndex + 1
                    SET branchScore = branchScore + innerIndex
                    log("low-inner")

                    IF branchScore >= thresholdLow
                        SET hitCount = hitCount + 1
                        vibrate(25)
                    ELSE
                        SET drift = drift + 1
                        wait(15)
                    END IF
                END LOOP
            ELSEIF score < thresholdMid
                SET branchScore = score + outerIndex
                swipe([120, 720, 120, 260], 1)
                log("outer-mid")

                LOOP 3
                    SET innerIndex = innerIndex + 1
                    SET branchScore = branchScore + innerIndex

                    IF (branchScore + drift) < thresholdMid
                        SET drift = drift + innerIndex
                        clickPoint(180, 320, 1)
                    ELSEIF (branchScore + hitCount) >= thresholdHigh
                        SET hitCount = hitCount + 2
                        beep(880, 50, 55)
                    ELSE
                        SET score = score + 1
                        vibrate(0, 20, 20, 20)
                    END IF
                END LOOP
            ELSEIF score < thresholdHigh
                SET branchScore = score * 2
                LET stressScore:Number = templateCompare("stressTemplate", region(16, 24, 320, 180), "grayscale")
                log("outer-high")

                IF branchScore >= thresholdHigh
                    SET hitCount = hitCount + 1
                    datastorePut("stress.hit", "hit-updated")

                    LOOP 2
                        SET innerIndex = innerIndex + 1
                        SET guard = guard + innerIndex

                        IF guard < thresholdMid
                            beep(440, 35, 35)
                        ELSE
                            vibrate(35)
                        END IF
                    END LOOP
                ELSE
                    SET drift = drift + 2
                    wait(20)
                END IF
            ELSE
                SET guard = 0
                log("outer-overflow")

                WHILE guard < 3
                    SET guard = guard + 1
                    SET retryIndex = retryIndex + 1

                    IF retryIndex < 2
                        click("retry-low")
                    ELSEIF retryIndex < 4
                        SET score = score - 1
                        beep()
                    ELSE
                        SET drift = drift + retryIndex
                        vibrate(40)
                    END IF
                END WHILE
            END IF

            IF (score + hitCount) >= thresholdHigh
                SET score = score - drift
                log("outer-normalize")
            ELSE
                SET score = score + 1
                wait(10)
            END IF
        END LOOP
        REM @vt.group.end id="flow:outer-loop"

        WHILE outerIndex < 8
            SET outerIndex = outerIndex + 1
            SET score = score + outerIndex

            IF score >= thresholdHigh
                SET hitCount = hitCount + 1
                log("tail-high")
            ELSE
                SET drift = drift + 1
                log("tail-low")
            END IF
        END WHILE

        datastorePut("stress.score", "complete")
        LET stressMarker:Marker? = markerLoad("stressRegion")
        markerDelete("stressRegion")
        screenshot("stress-end.png")
        log("nested-stress-end")
    """.trimIndent()

    val basicTestScript: String = """
        LET counter = 0
        LET total = 0
        LET limit = 3

        log("basic-start")
        beep(660, 40, 35)
        vibrate(20)
        LOOP 3
            SET counter = counter + 1
            SET total = total + counter
            IF total < limit
                log("basic-low")
            ELSEIF total == limit
                wait(10)
            ELSE
                beep()
            END IF
        END LOOP
        WHILE counter < 4
            SET counter = counter + 1
        END WHILE
        log("basic-end")
    """.trimIndent()

    val visionTestScript: String = """
        log("vision-start")
        screenshot("screenshots/stable-vision.png")
        markerSave("stableVisionRegion", region(180, 420, 540, 780), "region", 0.85)
        LET stableVisionMarker:Marker? = markerLoad("stableVisionRegion")
        highlight(region(180, 420, 540, 780))
        ocr(region(180, 420, 540, 780), 3000)
        LET stableTextMatch:TextMatch? = findText("VisualTasker", 3000)
        templateDefine("stableVisionTemplate", region(180, 420, 540, 780), "grayscale")
        LET stableVisionScore:Number = templateCompare("stableVisionTemplate", region(180, 420, 540, 780), "grayscale")
        LET stableImageMatch:ImageMatch? = findTemplate("stableVisionTemplate.png", 0.85, 3000, 1, region(180, 420, 540, 780))
        sceneSave("stableVisionScene", "region", region(180, 420, 540, 780), "screenshots/stable-vision.png")
        markerDelete("stableVisionRegion")
        log("vision-end")
    """.trimIndent()

    val runtimeTestScript: String = """
        log("runtime-start")
        clickPoint(540, 1100, 1)
        touch([540, 1100])
        swipe([540, 1500, 540, 850], 1)
        screenshot("screenshots/stable-runtime.png")
        Clipboard.set("visualtasker-runtime")
        log(Clipboard.get())
        File.writeText("stable-runtime.txt", "ready")
        LET runtimeFileText:String? = File.readText("stable-runtime.txt")
        Cache.clear()
        log(Sys.info())
        log(Env.get("ANDROID_VERSION"))
        log("runtime-end")
    """.trimIndent()

    val pluginTestScript: String = """
        log("plugin-start")
        LET chromeTabSupported:Bool = chromeTab.isSupported()
        LET taskerInstalled:Bool = tasker.isInstalled()
        LET taskerEnabled:Bool = tasker.isEnabled()
        LET taskerTestValue:String? = tasker.getVariable("%VT_TEST")
        LET taskerVariables:List<TaskerVariable> = tasker.getVariables("%VT_*")
        Tasker.lastResult()
        Tasker.error()
        LET shizukuInstalled:Bool = shizuku.isInstalled()
        Shizuku.permissionState()
        LET shizukuAvailable:Bool = shizuku.isAvailable()
        LET shizukuUid:Number? = shizuku.getUid()
        LET termuxInstalled:Bool = termux.isInstalled()
        LET termuxSummary:String? = termux.get("summary")
        Termux.canRunCommands()
        Scrcpy.hostAvailable()
        Scrcpy.devices()
        LET scrcpyRunning:Bool = scrcpy.isRunning()
        LET scrcpyState:String? = scrcpy.get("state")
        ChromeTab.open("https://example.com")
        Tasker.runTask("VT WSS Demo Echo", ["stable-v1"])
        Shizuku.shell("id")
        Termux.shell("printf visualtasker")
        Scrcpy.start("")
        log("plugin-end")
    """.trimIndent()

    val stableV1TestSuite: Map<String, String> = linkedMapOf(
        "Test: Basic" to basicTestScript,
        "Test: Vision" to visionTestScript,
        "Test: Runtime" to runtimeTestScript,
        "Test: Plugins" to pluginTestScript,
        "Test: Stress" to nestedFlowStressTestScript,
    )

    val allSamples: Map<String, String> = linkedMapOf(
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
        "Stress: Verschachtelt" to nestedFlowStressTestScript,
        "Branch: ElseIf" to elseifBranchTestScript,
        "Branch: Fallback" to fallbackBranchTestScript,
    ) + stableV1TestSuite
}
