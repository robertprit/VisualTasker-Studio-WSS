package com.visualtasker.integrations.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationContractsTest {
    @Test
    fun catalog_has_unique_ids_and_capabilities() {
        val entries = VisualTaskerIntegrationCatalog.descriptors

        assertEquals(entries.size, entries.map { it.id }.distinct().size)
        assertTrue(entries.all { it.capabilities.isNotEmpty() })
        assertTrue(entries.flatMap { it.capabilities }.all { it.id.contains('.') })
    }
}
