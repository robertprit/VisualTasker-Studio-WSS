package com.visualtasker.wss.workspace.plugin.runtime

import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScalarProviderRuntimeAdapterTest {
    @Test
    fun `Tasker enabled keeps true false and technical failure distinct`() {
        assertEquals(
            EmscriptValue.BooleanValue(true),
            TaskerEnabledInspection(installed = true, enabled = true).toEnabledAdapterResult().value,
        )
        assertEquals(
            EmscriptValue.BooleanValue(false),
            TaskerEnabledInspection(installed = false, enabled = false).toEnabledAdapterResult().value,
        )
        assertEquals(
            EmscriptValue.BooleanValue(false),
            TaskerEnabledInspection(installed = true, enabled = false).toEnabledAdapterResult().value,
        )

        val failure = TaskerEnabledInspection(failure = SecurityException("provider")).toEnabledAdapterResult()
        assertFalse(failure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.TASKER_INSTALLATION_CHECK_FAILED, failure.diagnosticCode)

        val preferenceFailure = TaskerEnabledInspection(
            installed = true,
            failure = IllegalStateException("preference"),
        ).toEnabledAdapterResult()
        assertFalse(preferenceFailure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.TASKER_ENABLED_CHECK_FAILED, preferenceFailure.diagnosticCode)
    }

    @Test
    fun `Tasker variable keeps empty String absent and failure distinct`() {
        assertEquals(
            EmscriptValue.StringValue("abc"),
            TaskerVariableQueryResult(value = "abc", present = true).toVariableAdapterResult().value,
        )
        assertEquals(
            EmscriptValue.StringValue(""),
            TaskerVariableQueryResult(value = "", present = true).toVariableAdapterResult().value,
        )
        assertEquals(EmscriptValue.NullValue, TaskerVariableQueryResult().toVariableAdapterResult().value)

        val failure = TaskerVariableQueryResult(failure = IllegalStateException("snapshot")).toVariableAdapterResult()
        assertFalse(failure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.TASKER_VARIABLE_QUERY_FAILED, failure.diagnosticCode)
    }

    @Test
    fun `Tasker variable checks provider installation before reading its snapshot`() {
        var queried = false
        val absentProvider = taskerVariableAdapterResult(
            installation = PackageInstallationInspection(installed = false),
        ) {
            queried = true
            TaskerVariableQueryResult(value = "stale", present = true)
        }
        assertFalse(absentProvider.success)
        assertFalse(queried)
        assertEquals(RuntimeQueryDiagnosticCodes.TASKER_NOT_INSTALLED, absentProvider.diagnosticCode)

        val inspectionFailure = taskerVariableAdapterResult(
            installation = PackageInstallationInspection(
                installed = false,
                failure = SecurityException("packages"),
            ),
        ) { TaskerVariableQueryResult() }
        assertFalse(inspectionFailure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.TASKER_INSTALLATION_CHECK_FAILED, inspectionFailure.diagnosticCode)

        val installed = taskerVariableAdapterResult(
            installation = PackageInstallationInspection(installed = true, packageName = TASKER_PACKAGE),
        ) { TaskerVariableQueryResult(value = "fresh", present = true) }
        assertTrue(installed.success)
        assertEquals(EmscriptValue.StringValue("fresh"), installed.value)
    }

    @Test
    fun `Tasker plugin variable transport preserves names empty values and newlines`() {
        val variables = linkedMapOf(
            "%Name" to "",
            "message" to "first\nsecond",
            "key=name" to "value",
        )

        assertEquals(
            linkedMapOf(
                "%Name" to "",
                "%message" to "first\nsecond",
                "%key=name" to "value",
            ),
            TaskerPluginContract.decodeVariables(TaskerPluginContract.encodeVariables(variables)),
        )
    }

    @Test
    fun `Shizuku UID keeps zero absent invalid and failure distinct`() {
        assertEquals(EmscriptValue.NumberValue(2000.0), shizukuInspection(uid = 2000).toUidAdapterResult().value)
        assertEquals(EmscriptValue.NumberValue(0.0), shizukuInspection(uid = 0).toUidAdapterResult().value)
        assertEquals(
            EmscriptValue.NullValue,
            shizukuInspection(state = ShizukuAvailabilityState.BINDER_NOT_ALIVE).toUidAdapterResult().value,
        )

        val invalid = shizukuInspection(uid = -1).toUidAdapterResult()
        assertFalse(invalid.success)
        assertEquals(RuntimeQueryDiagnosticCodes.SHIZUKU_UID_QUERY_FAILED, invalid.diagnosticCode)

        val failure = shizukuInspection(uidFailure = IllegalStateException("uid")).toUidAdapterResult()
        assertFalse(failure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.SHIZUKU_UID_QUERY_FAILED, failure.diagnosticCode)
    }

    @Test
    fun `Termux get freezes supported values and separates unknown from failure`() {
        val inspection = TermuxStatusInspection(
            status = TermuxRegistrationStatus(
                installed = true,
                apiInstalled = false,
                runCommandPermissionGranted = false,
                launchable = true,
            ),
        )

        assertEquals(EmscriptValue.StringValue("true"), inspection.toGetAdapterResult("installed").value)
        assertEquals(EmscriptValue.StringValue("false"), inspection.toGetAdapterResult("apiInstalled").value)
        assertEquals(EmscriptValue.StringValue("missing"), inspection.toGetAdapterResult("permission").value)
        assertEquals(EmscriptValue.NullValue, inspection.toGetAdapterResult("unknown").value)

        val failure = TermuxStatusInspection(failure = SecurityException("packages")).toGetAdapterResult("summary")
        assertFalse(failure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.TERMUX_STATUS_CHECK_FAILED, failure.diagnosticCode)
    }

    @Test
    fun `scrcpy queries inspect sessions rather than bridge readiness`() {
        val absent = ScrcpySessionInspection()
        assertEquals(EmscriptValue.BooleanValue(false), absent.toIsRunningAdapterResult().value)
        assertEquals(EmscriptValue.NullValue, absent.toGetAdapterResult("state").value)

        val running = ScrcpySessionInspection(
            session = ScrcpySessionSnapshot(
                sessionId = "scrcpy-1",
                serial = "device-1",
                state = "running",
                transport = "usb",
                host = "127.0.0.1",
                port = 27183,
            ),
        )
        assertEquals(EmscriptValue.BooleanValue(true), running.toIsRunningAdapterResult().value)
        assertEquals(EmscriptValue.StringValue("device-1"), running.toGetAdapterResult("serial").value)
        assertEquals(EmscriptValue.StringValue("27183"), running.toGetAdapterResult("port").value)

        val missingValue = running.copy(session = running.session?.copy(host = "", port = 0))
        assertEquals(EmscriptValue.NullValue, missingValue.toGetAdapterResult("host").value)
        assertEquals(EmscriptValue.NullValue, missingValue.toGetAdapterResult("port").value)

        val unknown = running.toGetAdapterResult("bridgeReady")
        assertFalse(unknown.success)
        assertNull(unknown.value)
        assertEquals(RuntimeQueryDiagnosticCodes.SCRCPY_UNKNOWN_KEY, unknown.diagnosticCode)

        val failure = ScrcpySessionInspection(failure = IllegalStateException("store")).toIsRunningAdapterResult()
        assertFalse(failure.success)
        assertEquals(RuntimeQueryDiagnosticCodes.SCRCPY_SESSION_CHECK_FAILED, failure.diagnosticCode)
    }

    private fun shizukuInspection(
        state: ShizukuAvailabilityState = ShizukuAvailabilityState.AVAILABLE,
        uid: Int? = null,
        uidFailure: Exception? = null,
    ) = ShizukuAvailabilityInspection(
        installation = PackageInstallationInspection(
            installed = true,
            packageName = SHIZUKU_PACKAGE,
        ),
        state = state,
        permissionGranted = state != ShizukuAvailabilityState.PERMISSION_NOT_GRANTED,
        binderAlive = state == ShizukuAvailabilityState.AVAILABLE,
        uid = uid,
        uidFailure = uidFailure,
    )
}
