package com.visualtasker.wss.workspace.plugin.flowchart

import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowchartNodePaletteTest {
    @Test
    fun `flowchart palette exposes all visible registry blocks`() {
        val entries = flowchartNodePaletteEntries()
        val entryIds = entries.map { it.definitionId }.toSet()
        val visibleRegistryIds = DefaultBlockRegistry.allDefinitions()
            .filter { it.paletteVisible }
            .map { it.id }
            .filterNot { it == BlockTypes.VARIABLE_REPORTER }
            .toSet()

        assertEquals(visibleRegistryIds, entryIds)
    }

    @Test
    fun `flowchart palette includes generated emscript command nodes`() {
        val entries = flowchartNodePaletteEntries()
        val ids = entries.map { it.definitionId }.toSet()

        assertTrue("${BlockTypes.EMSCRIPT_COMMAND_PREFIX}vision.screenshot" in ids)
        assertTrue("${BlockTypes.EMSCRIPT_COMMAND_PREFIX}clipboard.set" in ids)
        assertTrue("${BlockTypes.EMSCRIPT_COMMAND_PREFIX}chromeTab.open" in ids)
        assertTrue("${BlockTypes.EMSCRIPT_COMMAND_PREFIX}tasker.runTask" in ids)
        assertTrue("${BlockTypes.EMSCRIPT_COMMAND_PREFIX}termux.shell" in ids)
        assertTrue("${BlockTypes.EMSCRIPT_COMMAND_PREFIX}scrcpy.start" in ids)
    }
}
