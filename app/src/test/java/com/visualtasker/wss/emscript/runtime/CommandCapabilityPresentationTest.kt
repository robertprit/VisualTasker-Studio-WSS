package com.visualtasker.wss.emscript.runtime

import de.visualtasker.blockeditor.registry.VisualTaskerCommandCatalog
import de.visualtasker.blockeditor.registry.toCapabilityDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandCapabilityPresentationTest {
    @Test
    fun distinguishesLocalAdapterAndPlannedCommands() {
        val wait = VisualTaskerCommandCatalog.findByCanonicalName("wait")!!
            .toCapabilityDescriptor()
            .toToolboxCapability()
        val click = VisualTaskerCommandCatalog.findByCanonicalName("click")!!
            .toCapabilityDescriptor()
            .toToolboxCapability()
        val touch = VisualTaskerCommandCatalog.findByCanonicalName("touch")!!
            .toCapabilityDescriptor()
            .toToolboxCapability()

        assertEquals(CommandToolboxAvailability.LOCAL, wait.availability)
        assertEquals(CommandToolboxAvailability.ADAPTER, click.availability)
        assertEquals(CommandToolboxAvailability.PLANNED, touch.availability)
        assertTrue(click.shortLabel.isNotBlank())
        assertTrue(touch.description.contains("CAPABILITY_"))
    }
}
