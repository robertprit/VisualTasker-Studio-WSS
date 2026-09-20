package com.visualtasker.wss.workspace.model

import com.compose.canvas.asset.EmaCodec as ShapeMakerEmaCodec
import com.compose.canvas.asset.VisualAssetIdentity
import com.compose.canvas.asset.VisualAssetType
import com.compose.canvas.asset.newVisualAsset
import com.compose.canvas.core.ShapeMetadata
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.geometry.RectF
import com.compose.canvas.metadata.ContentArea
import com.compose.canvas.metadata.Port
import com.compose.canvas.metadata.PortType
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

    @Test
    fun createsVersionHistoryAndIndependentDuplicatesWithoutChangingDesign() {
        val directory = Files.createTempDirectory("ema-versions").toFile()
        val imported = EmaVisualAssetStore.importRaw(directory, validEma) as EmaVisualAssetImportResult.Imported

        val next = EmaVisualAssetStore.createNextVersion(directory, imported.asset, "Alert Block Renamed")
            as EmaVisualAssetImportResult.Imported
        val duplicate = EmaVisualAssetStore.duplicate(directory, imported.asset, "block-alert-copy")
            as EmaVisualAssetImportResult.Imported

        assertEquals(4, next.asset.descriptor.version)
        assertEquals("Alert Block Renamed", next.asset.descriptor.name)
        assertEquals(1, duplicate.asset.descriptor.version)
        assertEquals("block-alert-copy", duplicate.asset.descriptor.assetId)
        assertEquals(3, EmaVisualAssetStore.list(directory).size)
        assertEquals(2, EmaVisualAssetStore.latestByAssetId(directory).size)
        assertTrue(next.asset.file.readText().contains("\"name\": \"Alert\""))
    }

    @Test
    fun roundTripsRealShapeMakerSemanticsThroughWssCatalog() {
        val document = VectorDocument(
            name = "Semantic Shape",
            metadata = ShapeMetadata(
                ports = listOf(
                    Port(name = "value", x = 10f, y = 20f, portType = PortType.VALUE_INPUT, valueType = "Number"),
                    Port(name = "next", x = 90f, y = 80f, portType = PortType.NEXT),
                ),
                contentAreas = listOf(
                    ContentArea(name = "body", bounds = RectF(10f, 10f, 90f, 70f), contentType = "compose"),
                ),
            ),
        )
        val shapeMakerAsset = newVisualAsset("semantic-block", "Semantic Block", document).copy(
            identity = VisualAssetIdentity(
                assetId = "semantic-block",
                name = "Semantic Block",
                type = VisualAssetType.BLOCK_SHAPE,
                version = 2,
            ),
        )
        val directory = Files.createTempDirectory("ema-real-roundtrip").toFile()

        val imported = EmaVisualAssetStore.importRaw(directory, ShapeMakerEmaCodec.encode(shapeMakerAsset))
            as EmaVisualAssetImportResult.Imported
        val decodedByShapeMaker = ShapeMakerEmaCodec.decode(imported.asset.file.readText())

        assertEquals(2, imported.asset.descriptor.portCount)
        assertEquals(1, imported.asset.descriptor.contentAreaCount)
        assertEquals("Number", decodedByShapeMaker.design.metadata.ports.first().valueType)
        assertEquals("compose", decodedByShapeMaker.design.metadata.contentAreas.single().contentType)
    }
}
