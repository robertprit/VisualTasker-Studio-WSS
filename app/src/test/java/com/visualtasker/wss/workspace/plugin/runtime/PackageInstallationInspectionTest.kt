package com.visualtasker.wss.workspace.plugin.runtime

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageInstallationInspectionTest {
    @Test
    fun `found package produces successful true`() {
        val result = inspectSupportedPackages(listOf("provider.app")) { }

        assertTrue(result.installed)
        assertEquals("provider.app", result.packageName)
        assertNull(result.failure)
    }

    @Test
    fun `NameNotFound produces successful false`() {
        val result = inspectSupportedPackages(listOf("provider.app")) {
            throw PackageManager.NameNotFoundException(it)
        }

        assertFalse(result.installed)
        assertNull(result.packageName)
        assertNull(result.failure)
    }

    @Test
    fun `technical PackageManager error remains failure`() {
        val result = inspectSupportedPackages(listOf("provider.app")) {
            throw SecurityException("visibility denied")
        }

        assertFalse(result.installed)
        assertNull(result.packageName)
        assertNotNull(result.failure)
        assertTrue(result.failure is SecurityException)
    }

    @Test
    fun `Tasker style alternatives succeed when one supported package is found`() {
        val result = inspectSupportedPackages(listOf(TASKER_PACKAGE, TASKER_MARKET_PACKAGE)) { packageName ->
            if (packageName == TASKER_PACKAGE) throw PackageManager.NameNotFoundException(packageName)
        }

        assertTrue(result.installed)
        assertEquals(TASKER_MARKET_PACKAGE, result.packageName)
        assertNull(result.failure)
    }

    @Test
    fun `technical failure does not hide a later supported package`() {
        val result = inspectSupportedPackages(listOf(TASKER_PACKAGE, TASKER_MARKET_PACKAGE)) { packageName ->
            if (packageName == TASKER_PACKAGE) throw IllegalStateException("transient failure")
        }

        assertTrue(result.installed)
        assertEquals(TASKER_MARKET_PACKAGE, result.packageName)
        assertNull(result.failure)
    }
}
