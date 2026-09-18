package com.visualtasker.wss.workspace.plugin.runtime

import com.visualtasker.wss.workspace.vt2vt.Vt2VtLanEndpoint
import com.visualtasker.wss.workspace.vt2vt.Vt2VtUsbAdbBridgeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginRuntimeReadinessTest {
    @Test
    fun reportsReadyBlockedAndMissingAdaptersConsistently() {
        val report = PluginRuntimeReadiness.inspect(
            customTabs = CustomChromeTabStatus(true, "com.android.chrome", 1),
            shizuku = ShizukuRegistrationStatus(
                installed = true,
                permissionGranted = true,
                launchable = true,
                binderAlive = false,
            ),
            termux = TermuxRegistrationStatus(
                installed = true,
                apiInstalled = false,
                runCommandPermissionGranted = true,
                launchable = true,
            ),
            tasker = TaskerRegistrationStatus(
                installed = false,
                packageName = null,
                launchable = false,
                runTaskPermissionGranted = false,
                taskerEnabled = null,
                externalAccessAllowed = null,
                receiverAvailable = false,
            ),
            usb = Vt2VtUsbAdbBridgeStatus(
                usbConnected = true,
                adbEnabled = false,
                usbConfigured = true,
                connectedPeripheralCount = 0,
                recommendedEndpoint = Vt2VtLanEndpoint("127.0.0.1", 47272),
            ),
        )

        assertEquals(2, report.readyCount)
        assertEquals(3, report.blockedCount)
        assertEquals(1, report.missingCount)
        assertTrue(report.entry(PluginRuntimeReadiness.CUSTOM_TABS).ready)
        assertEquals(PluginReadinessState.BLOCKED, report.entry(PluginRuntimeReadiness.SHIZUKU).state)
        assertEquals(PluginReadinessState.MISSING, report.entry(PluginRuntimeReadiness.TASKER).state)
        assertFalse(report.entry(PluginRuntimeReadiness.VT2VT_USB).ready)
        assertEquals(
            PluginReadinessState.BLOCKED,
            report.entry(PluginRuntimeReadiness.VISION_PROVIDERS).state,
        )
    }

    @Test
    fun shizukuRequiresLiveBinderAndTermuxRequiresRunPermission() {
        val report = PluginRuntimeReadiness.inspect(
            customTabs = CustomChromeTabStatus(false, null, 0),
            shizuku = ShizukuRegistrationStatus(true, true, true, true, 2000),
            termux = TermuxRegistrationStatus(true, true, false, true),
            tasker = TaskerRegistrationStatus(true, TASKER_PACKAGE, true, true, true, true, true),
            usb = Vt2VtUsbAdbBridgeStatus(true, true, true, 1),
        )

        assertEquals(PluginReadinessState.READY, report.entry(PluginRuntimeReadiness.SHIZUKU).state)
        assertEquals(PluginReadinessState.BLOCKED, report.entry(PluginRuntimeReadiness.TERMUX).state)
        assertEquals(PluginReadinessState.READY, report.entry(PluginRuntimeReadiness.TASKER).state)
        assertEquals(PluginReadinessState.READY, report.entry(PluginRuntimeReadiness.VT2VT_USB).state)
        assertEquals(PluginReadinessState.BLOCKED, report.entry(PluginRuntimeReadiness.VISION_PROVIDERS).state)
    }
}
