package com.visualtasker.wss.workspace.plugin.runtime

import com.visualtasker.wss.emscript.runtime.EmscriptValue
import com.visualtasker.wss.emscript.runtime.RuntimeQueryDiagnosticCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuAvailabilityInspectionTest {
    @Test
    fun `not installed is a successful false without probing Binder or permission`() {
        var probed = false
        val result = inspectShizukuAvailability(
            installation = installation(installed = false),
            binderProbe = { probed = true; true },
            permissionProbe = { probed = true; true },
        )

        assertEquals(ShizukuAvailabilityState.NOT_INSTALLED, result.state)
        assertFalse(result.available)
        assertFalse(probed)
        assertEquals(EmscriptValue.BooleanValue(false), result.toAvailabilityAdapterResult().value)
    }

    @Test
    fun `missing permission is a successful false with explicit provenance`() {
        val result = inspectShizukuAvailability(
            installation = installation(),
            binderProbe = { true },
            permissionProbe = { false },
        )

        assertEquals(ShizukuAvailabilityState.PERMISSION_NOT_GRANTED, result.state)
        assertFalse(result.available)
        assertEquals(EmscriptValue.BooleanValue(false), result.toAvailabilityAdapterResult().value)
    }

    @Test
    fun `dead Binder is a successful false with explicit provenance`() {
        val result = inspectShizukuAvailability(
            installation = installation(),
            binderProbe = { false },
            permissionProbe = { true },
        )

        assertEquals(ShizukuAvailabilityState.BINDER_NOT_ALIVE, result.state)
        assertFalse(result.available)
        assertEquals(EmscriptValue.BooleanValue(false), result.toAvailabilityAdapterResult().value)
    }

    @Test
    fun `all prerequisites true produces successful true`() {
        val result = inspectShizukuAvailability(
            installation = installation(),
            binderProbe = { true },
            permissionProbe = { true },
            uidProbe = { 2000 },
        )

        assertEquals(ShizukuAvailabilityState.AVAILABLE, result.state)
        assertTrue(result.available)
        assertEquals(2000, result.uid)
        assertEquals(EmscriptValue.BooleanValue(true), result.toAvailabilityAdapterResult().value)
    }

    @Test
    fun `UID failure does not change availability but remains query failure provenance`() {
        val inspection = inspectShizukuAvailability(
            installation = installation(),
            binderProbe = { true },
            permissionProbe = { true },
            uidProbe = { throw IllegalStateException("uid service") },
        )

        assertTrue(inspection.available)
        assertTrue(inspection.uidFailure is IllegalStateException)
        assertEquals(EmscriptValue.BooleanValue(true), inspection.toAvailabilityAdapterResult().value)
        val uidResult = inspection.toUidAdapterResult()
        assertFalse(uidResult.success)
        assertEquals(RuntimeQueryDiagnosticCodes.SHIZUKU_UID_QUERY_FAILED, uidResult.diagnosticCode)
    }

    @Test
    fun `installation failure remains a structured failure`() {
        val result = inspectShizukuAvailability(
            installation = installation(
                installed = false,
                failure = SecurityException("package visibility"),
            ),
            binderProbe = { true },
            permissionProbe = { true },
        ).toAvailabilityAdapterResult()

        assertFalse(result.success)
        assertNull(result.value)
        assertEquals(RuntimeQueryDiagnosticCodes.SHIZUKU_INSTALLATION_CHECK_FAILED, result.diagnosticCode)
    }

    @Test
    fun `permission failure remains a structured failure`() {
        val inspection = inspectShizukuAvailability(
            installation = installation(),
            binderProbe = { true },
            permissionProbe = { throw SecurityException("permission service") },
        )
        val result = inspection.toAvailabilityAdapterResult()

        assertEquals(ShizukuAvailabilityFailureStage.PERMISSION, inspection.failureStage)
        assertFalse(result.success)
        assertNull(result.value)
        assertEquals(RuntimeQueryDiagnosticCodes.SHIZUKU_PERMISSION_CHECK_FAILED, result.diagnosticCode)
    }

    @Test
    fun `Binder failure remains a structured failure`() {
        val inspection = inspectShizukuAvailability(
            installation = installation(),
            binderProbe = { throw IllegalStateException("binder service") },
            permissionProbe = { true },
        )
        val result = inspection.toAvailabilityAdapterResult()

        assertEquals(ShizukuAvailabilityFailureStage.BINDER, inspection.failureStage)
        assertFalse(result.success)
        assertNull(result.value)
        assertEquals(RuntimeQueryDiagnosticCodes.SHIZUKU_BINDER_CHECK_FAILED, result.diagnosticCode)
    }

    private fun installation(
        installed: Boolean = true,
        failure: Exception? = null,
    ) = PackageInstallationInspection(
        installed = installed,
        packageName = if (installed) SHIZUKU_PACKAGE else null,
        failure = failure,
    )
}
