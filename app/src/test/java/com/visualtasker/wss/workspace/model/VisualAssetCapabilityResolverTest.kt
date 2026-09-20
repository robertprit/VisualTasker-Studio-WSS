package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAssetCapabilityResolverTest {
    @Test
    fun embeddedCapabilitiesDoNotDependOnCompanionInstallation() {
        val status = VisualAssetCapabilityResolver.resolve(externalEditorAvailable = false)

        assertTrue(status.embeddedDesigner)
        assertTrue(status.emaImport)
        assertTrue(status.emaExport)
        assertFalse(status.externalEditor)
        assertTrue(status.assetRuntime)
    }

    @Test
    fun externalEditorIsReportedIndependently() {
        assertTrue(VisualAssetCapabilityResolver.resolve(externalEditorAvailable = true).externalEditor)
    }
}
