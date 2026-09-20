package com.visualtasker.wss.workspace.model

import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VisualAssetPackageTest {
    @Test
    fun roundTripsAssetsBindingsAndToolboxSets() {
        val source = Files.createTempDirectory("asset-package-source").toFile()
        val rawAsset = JSONObject()
            .put("format", EMA_FORMAT)
            .put("schemaVersion", EMA_SCHEMA_VERSION)
            .put("asset", JSONObject()
                .put("assetId", "shape-one")
                .put("name", "Shape One")
                .put("type", EmaVisualAssetType.BLOCK_SHAPE.name)
                .put("version", 1)
                .put("design", JSONObject()))
            .toString()
        val stored = (EmaVisualAssetStore.importRaw(source, rawAsset) as EmaVisualAssetImportResult.Imported).asset
        val catalog = VisualAssetCatalog(
            bindings = listOf(VisualAssetBinding("shape-one", EmaVisualAssetBindingTarget.Block, "event.start")),
            toolboxSets = listOf(VisualAssetToolboxSet("custom", "Custom", setOf("shape-one"))),
        )

        val encoded = VisualAssetPackageCodec.encode(catalog, listOf(stored))
        val decoded = VisualAssetPackageCodec.decode(encoded) as? VisualAssetPackageDecodeResult.Decoded
        assertNotNull(decoded)
        assertEquals(1, decoded!!.value.assetDocuments.size)

        val target = Files.createTempDirectory("asset-package-target").toFile()
        val imported = VisualAssetPackageStore.import(target, VisualAssetCatalog(), encoded)
        assertNotNull(imported)
        assertEquals(listOf("shape-one"), imported!!.importedAssets.map { it.descriptor.assetId })
        assertEquals("event.start", imported.catalog.bindings.single().targetId)
        assertEquals(setOf("shape-one"), imported.catalog.toolboxSets.first { it.id == "custom" }.assetIds)
        assertEquals(0, imported.rejectedDocuments)
    }

    @Test
    fun rejectsUnknownPackageVersion() {
        val raw = JSONObject()
            .put("format", VISUAL_ASSET_PACKAGE_FORMAT)
            .put("schemaVersion", 99)
            .toString()
        assertEquals(
            VisualAssetPackageDecodeResult.UnsupportedVersion(99),
            VisualAssetPackageCodec.decode(raw),
        )
    }
}
