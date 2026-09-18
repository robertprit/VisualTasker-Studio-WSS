package com.visualtasker.wss.workspace.model

import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.BlockDefinition
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolboxSetModeTest {
    @Test
    fun separatesCoreCustomJavascriptAndPluginDefinitions() {
        val core = DefaultBlockRegistry.allDefinitions().first { it.id == "action.wait" }
        val custom = BlockDefinition("custom.preview", "Preview", BlockCategories.CUSTOM, true, true)
        val javascript = BlockDefinition(
            "javascript.eval",
            "JavaScript",
            BlockCategories.CUSTOM,
            true,
            true,
            metadata = mapOf("language" to "javascript"),
        )
        val plugin = DefaultBlockRegistry.allDefinitions().first { it.category == BlockCategories.TASKER }

        assertTrue(ToolboxSetMode.Standard.accepts(core))
        assertFalse(ToolboxSetMode.Standard.accepts(plugin))
        assertTrue(ToolboxSetMode.Custom.accepts(custom))
        assertTrue(ToolboxSetMode.JavaScript.accepts(javascript))
        assertTrue(ToolboxSetMode.Plugins.accepts(plugin))
        assertTrue(listOf(core, custom, javascript, plugin).all(ToolboxSetMode.Mixed::accepts))
    }
}
