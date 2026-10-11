package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.workspace.plugin.runtime.CustomChromeTabStatus
import com.visualtasker.wss.workspace.plugin.runtime.ShizukuRegistrationStatus
import com.visualtasker.wss.workspace.plugin.runtime.TaskerRegistrationStatus
import com.visualtasker.wss.workspace.plugin.runtime.TermuxRegistrationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidOptionalProviderInventoryTest {
    @Test
    fun installedDoesNotImplyExecutable() {
        val entries = AndroidOptionalProviderInventory.fromSnapshot(
            snapshot(
                tasker = readyTasker().copy(runTaskPermissionGranted = false),
                shizuku = readyShizuku().copy(binderAlive = false),
                termux = readyTermux().copy(runCommandPermissionGranted = false),
            ),
        )

        assertFalse(entries.single { it.descriptor.id.value.endsWith(".tasker") }.availability.isExecutable)
        assertFalse(entries.single { it.descriptor.id.value.endsWith(".shizuku") }.availability.isExecutable)
        assertFalse(entries.single { it.descriptor.id.value.endsWith(".termux") }.availability.isExecutable)
    }

    @Test
    fun readyExternalHostsAreDiscoverableButRemainPartial() {
        val entries = AndroidOptionalProviderInventory.fromSnapshot(snapshot())

        assertEquals(4, entries.size)
        assertTrue(entries.all { it.availability.isExecutable })
        assertTrue(entries.all { it.executionStatus == "PARTIAL" })
        assertTrue(entries.all { !it.gap.isNullOrBlank() })
    }

    @Test
    fun inventoryOrderAndProviderIdsAreStable() {
        val first = AndroidOptionalProviderInventory.fromSnapshot(snapshot())
        val second = AndroidOptionalProviderInventory.fromSnapshot(snapshot())

        assertEquals(first.map { it.descriptor.id }, second.map { it.descriptor.id })
        assertEquals(first.map { it.descriptor.id.value }.sorted(), first.map { it.descriptor.id.value })
    }

    @Test
    fun customTabsWithoutBrowserIsNotInstalledOrExecutable() {
        val entry = AndroidOptionalProviderInventory.fromSnapshot(
            snapshot(customTabs = CustomChromeTabStatus(false, null, 0)),
        ).single { it.descriptor.id.value.contains("customtabs") }

        assertEquals(ProviderInstallationState.NOT_INSTALLED, entry.availability.installation)
        assertEquals(ProviderCapabilityState.UNAVAILABLE, entry.availability.capability)
        assertFalse(entry.availability.isExecutable)
    }

    private fun snapshot(
        tasker: TaskerRegistrationStatus = readyTasker(),
        shizuku: ShizukuRegistrationStatus = readyShizuku(),
        termux: TermuxRegistrationStatus = readyTermux(),
        customTabs: CustomChromeTabStatus = CustomChromeTabStatus(true, "com.android.chrome", 1),
    ) = OptionalProviderStatusSnapshot(tasker, shizuku, termux, customTabs)

    private fun readyTasker() = TaskerRegistrationStatus(
        installed = true,
        packageName = "net.dinglisch.android.taskerm",
        launchable = true,
        runTaskPermissionGranted = true,
        taskerEnabled = true,
        externalAccessAllowed = true,
        receiverAvailable = true,
    )

    private fun readyShizuku() = ShizukuRegistrationStatus(
        installed = true,
        permissionGranted = true,
        launchable = true,
        binderAlive = true,
        uid = 2000,
    )

    private fun readyTermux() = TermuxRegistrationStatus(
        installed = true,
        apiInstalled = true,
        runCommandPermissionGranted = true,
        launchable = true,
    )
}
