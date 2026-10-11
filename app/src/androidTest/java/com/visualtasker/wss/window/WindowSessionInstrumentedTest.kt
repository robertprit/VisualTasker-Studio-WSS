package com.visualtasker.wss.window

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindowSessionInstrumentedTest {
    @Test
    fun deviceDisplayContextHasPhysicalMetricsDensityAndRotation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val display = AndroidDisplayContextFactory.from(context)
        assertTrue(display.physicalWidthPx > 0)
        assertTrue(display.physicalHeightPx > 0)
        assertTrue(display.density > 0f)
        assertTrue(display.rotation in setOf(0, 90, 180, 270))
    }

    @Test
    fun processRegistryIsUniqueWithinApplicationProcess() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertSame(WindowSessionProcessRegistry.get(context), WindowSessionProcessRegistry.get(context))
    }

    @Test
    fun overlayPermissionIsDeclaredWithoutChangingPermissionState() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        assertTrue(Manifest.permission.SYSTEM_ALERT_WINDOW in packageInfo.requestedPermissions.orEmpty())
        assertEquals(context.packageName, packageInfo.packageName)
    }
}
