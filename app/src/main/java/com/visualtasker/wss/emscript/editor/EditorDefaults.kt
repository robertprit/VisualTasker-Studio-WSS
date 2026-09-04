package com.visualtasker.wss.emscript.editor

object EditorDefaults {
    const val integrationTestScriptVersion: Int = 6

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
        REM @vt.group.start id="catalog:variables" label="Katalog Variablen" kind="variable-bulk"
        LET catalogIndex = 0
        LET thresholdLow = 2
        LET thresholdHigh = 6
        LET result = 0
        REM @vt.group.end id="catalog:variables"

        log("catalog-breadth-start")
        wait(50)
        click("Start")
        clickPoint(120, 240, 1)
        touch(["down", 120, 240, "up"])
        swipe([120, 640, 120, 220], 1)
        screenshot("catalog-screen.png")
        findTemplate("button.png", 0.82, 1000)
        ocr("", 1500)
        findText("Login", 1500)
        highlight("0,0,220,120")
        Clipboard.set("visualtasker")
        Clipboard.get()
        File.writeText("catalog.txt", "hello")
        File.readText("catalog.txt")
        Cache.clear()
        Sys.info()
        Env.get("ANDROID_VERSION")

        LOOP 3
            SET catalogIndex = catalogIndex + 1
            SET result = result + catalogIndex
            log("catalog-loop")
            beep(880, 80, 60)
            vibrate(0, 30, 20, 30)

            IF (result + catalogIndex) < thresholdLow
                Tasker.setVariable("%VT_LOW", result)
                ChromeTab.isSupported()
                ChromeTab.bind()
                ChromeTab.create("https://example.com", {"toolbar":"compact"})
                ChromeTab.mayLaunchUrl("session", "https://example.com", [])
            ELSEIF (result + catalogIndex) >= thresholdHigh
                Tasker.runTask("VT_Test", {"value":result})
                Tasker.getVariable("%VT_LOW", 0)
                Tasker.getVariables(["%VT_LOW", "%VT_HIGH"])
                Shizuku.isInstalled()
                Shizuku.isAvailable()
                Shizuku.permissionState()
                Shizuku.exec("cmd", ["package", "list"])

                IF result >= 8
                    Scrcpy.hostAvailable()
                    Scrcpy.devices()
                    Scrcpy.connect("127.0.0.1:5555")
                    Scrcpy.start("default")
                    Scrcpy.touch("session", "tap", [120, 240])
                    Scrcpy.key("session", "HOME")
                ELSE
                    Termux.isInstalled()
                    Termux.canRunCommands()
                    Termux.run("/data/data/com.termux/files/home/vt.sh", ["catalog"])
                    Termux.shell("echo vt")
                    Termux.api("battery-status")
                END IF
            ELSE
                Tasker.isInstalled()
                Tasker.isEnabled()
                Tasker.action("Flash", {"text":"Catalog"}, false, 0)
                Tasker.pluginAction("pkg", "action", {}, 0)
                Tasker.emitEvent("VT_EVENT", {"source":"catalog"})
                Tasker.profileEnable("VT_Profile")
                Tasker.profileState("VT_Profile")

                IF (catalogIndex + result) != thresholdHigh
                    Chart.create("line", {"labels":["a","b"], "values":[1,2]})
                    Chart.setData("chart1", {"values":[catalogIndex,result]})
                    Chart.setOptions("chart1", {"theme":"dark"})
                    Chart.show("chart1")
                    Chart.capture("chart1", "chart.png")
                ELSE
                    Chart.exists("chart1")
                    Chart.add("chart1", {"x":catalogIndex, "y":result})
                    Chart.update("chart1", {"title":"Catalog"})
                    Chart.removeData("chart1", "last")
                    Chart.clear("chart1")
                    Chart.export("chart1", "chart.json")
                END IF
            END IF
        END LOOP

        WHILE catalogIndex < 5
            SET catalogIndex = catalogIndex + 1
            log("catalog-while")
            ChromeTab.requestPostMessageChannel("session", "https://example.com")
            ChromeTab.postMessage("session", "ping", {})
            ChromeTab.validateRelationship("session", 1, "https://example.com")
            ChromeTab.open("https://example.com")
            ChromeTab.unbind()
            Shizuku.getUid()
            Shizuku.requestPermission(42)
            Shizuku.bindUserService("demo")
            Shizuku.systemService("activity")
            Shizuku.call("activity", "android.app.IActivityManager", "getTasks", [])
            Shizuku.unbindUserService("demo")
            Termux.writeStdin("session", "exit")
            Termux.get("session")
            Termux.cancel("session")
            Scrcpy.text("session", "hello")
            Scrcpy.scroll("session", 0, -4)
            Scrcpy.setClipboard("session", "clip")
            Scrcpy.setScreenPower("session", true)
            Scrcpy.rotate("session")
            Scrcpy.get("session")
            Scrcpy.stop("session")
            Scrcpy.disconnect("127.0.0.1:5555")
            Tasker.profileToggle("VT_Profile")
            Tasker.profileDisable("VT_Profile")
            Tasker.clearVariable("%VT_LOW")
            Tasker.cancel("VT_Test")
        END WHILE

        Chart.get("chart1")
        Chart.hide("chart1")
        Chart.remove("chart1")
        log("catalog-breadth-end")
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
