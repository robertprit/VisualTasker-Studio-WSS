package com.visualtasker.wss.workspace.model

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmaVisualAssetTest {
    private val validEma = """
        {
          "format": "emscript_motion_asset",
          "schemaVersion": 1,
          "asset": {
            "assetId": "block-alert",
            "name": "Alert Block",
            "type": "BLOCK_SHAPE",
            "version": 3,
            "tags": ["block", "warning"],
            "design": {"version": 1, "name": "Alert"},
            "states": [{"id":"start"}, {"id":"end"}],
            "transitions": [{"id":"pulse"}],
            "animations": [{"id":"attention"}],
            "sequences": [{"id":"default"}],
            "metadata": {"portPolicy":"statement"}
          }
        }
    """.trimIndent()

    @Test
    fun readsCanonicalShapeMakerEnvelopeWithoutOwningDesignModel() {
        val result = EmaVisualAssetCodec.decodeDescriptor(validEma) as EmaDescriptorDecodeResult.Decoded

        assertEquals("block-alert", result.descriptor.assetId)
        assertEquals(EmaVisualAssetType.BLOCK_SHAPE, result.descriptor.type)
        assertEquals(2, result.descriptor.stateCount)
        assertEquals(1, result.descriptor.transitionCount)
        assertEquals("statement", result.descriptor.metadata["portPolicy"])
    }

    @Test
    fun importsLosslesslyAndProjectsWorkspaceResource() {
        val directory = Files.createTempDirectory("ema-store").toFile()
        val imported = EmaVisualAssetStore.importRaw(directory, validEma) as EmaVisualAssetImportResult.Imported

        assertEquals(validEma, imported.asset.file.readText())
        assertEquals(1, EmaVisualAssetStore.list(directory).size)
        val resource = imported.asset.toWorkspaceResource()
        assertEquals(WorkspaceResourceKind.VisualAsset, resource.kind)
        assertEquals(EMA_MIME_TYPE, resource.mimeType)
        assertEquals("BLOCK_SHAPE", resource.metadata["assetType"])
    }

    @Test
    fun rejectsUnknownSchemaAndMissingDesign() {
        val future = validEma.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
        val noDesign = validEma.replace("\"design\": {\"version\": 1, \"name\": \"Alert\"},", "")

        assertEquals(EmaDescriptorDecodeResult.UnsupportedVersion(99), EmaVisualAssetCodec.decodeDescriptor(future))
        assertTrue(EmaVisualAssetCodec.decodeDescriptor(noDesign) is EmaDescriptorDecodeResult.Invalid)
    }

    @Test
    fun reportsMissingAndMismatchedAssetDependencies() {
        val directory = Files.createTempDirectory("ema-integrity").toFile()
        val imported = EmaVisualAssetStore.importRaw(directory, validEma) as EmaVisualAssetImportResult.Imported
        val validResource = imported.asset.toWorkspaceResource()
        val mismatchedResource = validResource.copy(
            mimeType = "application/json",
            metadata = validResource.metadata + mapOf("assetId" to "other", "assetVersion" to "2"),
        )
        val missingResource = validResource.copy(
            id = "visual-asset:missing",
            uri = directory.resolve("missing.ema").absolutePath,
        )

        assertTrue(EmaVisualAssetIntegrity.inspect(validResource).isEmpty())
        val issues = EmaVisualAssetIntegrity.inspect(
            WorkspaceResourceBundle(resources = listOf(mismatchedResource, missingResource))
        )
        assertEquals(
            setOf("EMA_MIME_UNSUPPORTED", "EMA_ASSET_ID_MISMATCH", "EMA_VERSION_MISMATCH", "EMA_FILE_MISSING"),
            issues.mapTo(mutableSetOf()) { it.code },
        )
    }

    @Test
    fun removesOnlyAssetsInsideManagedCatalog() {
        val directory = Files.createTempDirectory("ema-remove").toFile()
        val imported = EmaVisualAssetStore.importRaw(directory, validEma) as EmaVisualAssetImportResult.Imported
        val externalFile = Files.createTempFile("external-asset", ".ema").toFile().apply { writeText(validEma) }
        val external = imported.asset.copy(file = externalFile)

        assertTrue(EmaVisualAssetStore.remove(directory, imported.asset))
        assertFalse(imported.asset.file.exists())
        assertFalse(EmaVisualAssetStore.remove(directory, external))
        assertTrue(externalFile.exists())
    }

    @Test
    fun exposesShapeMakerSemanticsAndBindingTargets() {
        val semanticEma = validEma.replace(
            "{\"version\": 1, \"name\": \"Alert\"}",
            """{
              "version": 1,
              "name": "Alert",
              "objects": [{
                "ports": [{"id":"in"}, {"id":"out"}],
                "anchors": [{"id":"center"}],
                "contentAreas": [{"id":"body"}]
              }]
            }""".trimIndent(),
        )

        val descriptor = (EmaVisualAssetCodec.decodeDescriptor(semanticEma) as EmaDescriptorDecodeResult.Decoded).descriptor

        assertEquals(2, descriptor.portCount)
        assertEquals(1, descriptor.anchorCount)
        assertEquals(1, descriptor.contentAreaCount)
        assertEquals(setOf(EmaVisualAssetBindingTarget.Block), descriptor.bindingTargets)
        val portDescriptor = descriptor.copy(type = EmaVisualAssetType.PORT_SHAPE)
        assertEquals(setOf(EmaVisualAssetBindingTarget.Port), portDescriptor.bindingTargets)
    }
}
