package com.visualtasker.wss.workspace.plugin.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuRegistrationStatusTest {
    @Test
    fun distinguishesLegitimationFromLiveBinderAvailability() {
        val legitimizedOnly = ShizukuRegistrationStatus(
            installed = true,
            permissionGranted = true,
            launchable = true,
            binderAlive = false,
            uid = null,
        )

        assertTrue(legitimizedOnly.legitimized)
        assertFalse(legitimizedOnly.available)
        assertTrue(legitimizedOnly.summary.contains("Dienst nicht aktiv"))
    }

    @Test
    fun runtimeAvailableRequiresInstalledPermissionAndBinder() {
        val ready = ShizukuRegistrationStatus(
            installed = true,
            permissionGranted = true,
            launchable = true,
            binderAlive = true,
            uid = 2000,
        )

        assertTrue(ready.legitimized)
        assertTrue(ready.available)
    }
}
