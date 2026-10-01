package com.visualtasker.wss.emscript

import com.visualtasker.wss.workspace.plugin.runtime.TASKER_PACKAGE
import com.visualtasker.wss.workspace.plugin.runtime.ShizukuRegistrationStatus
import com.visualtasker.wss.workspace.plugin.runtime.TaskerRegistrationStatus
import com.visualtasker.wss.workspace.plugin.runtime.TermuxRegistrationStatus
import de.visualtasker.blockeditor.registry.ProviderBoolSemanticsFreeze
import de.visualtasker.blockeditor.registry.QueryReturnContractAudit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderBoolSemanticsFreezeTest {
    @Test
    fun `installed status remains independent from operational readiness`() {
        val tasker = TaskerRegistrationStatus(
            installed = true,
            packageName = TASKER_PACKAGE,
            launchable = false,
            runTaskPermissionGranted = false,
            taskerEnabled = false,
            externalAccessAllowed = false,
            receiverAvailable = false,
        )
        val termux = TermuxRegistrationStatus(
            installed = true,
            apiInstalled = false,
            runCommandPermissionGranted = false,
            launchable = false,
        )

        assertTrue(tasker.installed)
        assertFalse(tasker.available)
        assertTrue(termux.installed)
        assertFalse(termux.canRunCommands)
    }

    @Test
    fun `Shizuku availability requires installation permission and live Binder`() {
        val states = listOf(
            ShizukuRegistrationStatus(false, false, false, false),
            ShizukuRegistrationStatus(true, false, true, false),
            ShizukuRegistrationStatus(true, true, true, false),
            ShizukuRegistrationStatus(true, false, true, true),
            ShizukuRegistrationStatus(true, true, true, true, 2000),
        )

        assertFalse(states[0].installed)
        assertTrue(states.drop(1).all { it.installed })
        assertTrue(states.dropLast(1).none { it.available })
        assertTrue(states.last().available)
    }

    @Test
    fun `3S freeze remains available after 3T and 3U migrations`() {
        ProviderBoolSemanticsFreeze.validate()

        assertTrue(QueryReturnContractAudit.MIGRATED_M1B_3T.containsAll(
            listOf("tasker.isInstalled", "shizuku.isInstalled", "termux.isInstalled"),
        ))
        assertTrue("shizuku.isAvailable" in QueryReturnContractAudit.MIGRATED_M1B_3U)
        assertTrue(QueryReturnContractAudit.ALL.none { it.stableId == "shizuku.isAvailable" })
    }
}
