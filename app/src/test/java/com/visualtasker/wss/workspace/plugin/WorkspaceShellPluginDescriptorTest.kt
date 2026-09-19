package com.visualtasker.wss.workspace.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceShellPluginDescriptorTest {
    @Test
    fun defaultCatalogUsesUniqueIdsAndSupportedApiVersion() {
        val descriptors = defaultShellPluginDescriptors()

        assertEquals(descriptors.size, descriptors.map { it.pluginId }.distinct().size)
        assertTrue(descriptors.all { it.apiVersion == SHELL_PLUGIN_API_VERSION })
    }

    @Test
    fun registryExposesNonEditorPluginDescriptors() {
        val registry = defaultWorkspaceShellPluginRegistry()

        assertNotNull(registry.findDescriptor(ShellPluginId("m3-shapemaker")))
        assertNotNull(registry.findDescriptor(ShellPluginId("chartgraph")))
        assertNotNull(registry.findDescriptor(ShellPluginId("visualtasker-ime")))
        assertNotNull(registry.findDescriptor(ShellPluginId("vision-ai")))
    }
}
