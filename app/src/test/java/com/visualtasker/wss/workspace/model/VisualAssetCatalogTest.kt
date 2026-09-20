package com.visualtasker.wss.workspace.model

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAssetCatalogTest {
    @Test
    fun roundTripsBindingsAndToolboxMembership() {
        val catalog = VisualAssetCatalog()
            .upsertBinding(VisualAssetBinding("asset-1", EmaVisualAssetBindingTarget.Block, "control.if"))
            .toggleAssetInToolbox("custom", "asset-1")

        val decoded = VisualAssetCatalogCodec.decode(VisualAssetCatalogCodec.encode(catalog))
            as VisualAssetCatalogDecodeResult.Decoded

        assertEquals(catalog, decoded.catalog)
    }

    @Test
    fun bindingTargetsAreUniqueAndAssetRemovalCleansReferences() {
        val catalog = VisualAssetCatalog()
            .upsertBinding(VisualAssetBinding("first", EmaVisualAssetBindingTarget.FlowNode, "decision"))
            .upsertBinding(VisualAssetBinding("second", EmaVisualAssetBindingTarget.FlowNode, "decision"))
            .toggleAssetInToolbox("project", "second")

        assertEquals(listOf("second"), catalog.bindings.map { it.assetId })
        val cleaned = catalog.removeAsset("second")
        assertTrue(cleaned.bindings.isEmpty())
        assertFalse(cleaned.toolboxSets.first { it.id == "project" }.assetIds.contains("second"))
    }

    @Test
    fun persistsCatalogAtomically() {
        val file = Files.createTempDirectory("asset-catalog").resolve("catalog.json").toFile()
        val catalog = VisualAssetCatalog().toggleAssetInToolbox("favorites", "asset-2")

        VisualAssetCatalogStore.save(file, catalog)

        assertEquals(catalog, VisualAssetCatalogStore.load(file))
    }
}
