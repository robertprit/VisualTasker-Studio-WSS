package com.visualtasker.wss.workspace.model

import com.compose.canvas.asset.EmaCodec as ShapeMakerEmaCodec
import com.compose.canvas.asset.VisualAssetIdentity
import com.compose.canvas.asset.VisualAssetType
import com.compose.canvas.asset.newVisualAsset
import com.compose.canvas.core.ObjectSemantics
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.RectF
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.metadata.ContentArea
import com.compose.canvas.metadata.Port
import com.compose.canvas.metadata.PortType
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VisualAssetPresentationRuntimeTest {
    @Test
    fun resolvesBoundShapeAndSemanticsWithoutOwningEditorState() {
        val directory = Files.createTempDirectory("asset-runtime").toFile()
        val objectNode = VectorObject(
            name = "Block silhouette",
            geometry = RectGeometry(width = 200f, height = 80f, cornerRadius = 12f),
            semantics = ObjectSemantics(
                ports = listOf(
                    Port(name = "previous", x = 100f, y = 0f, portType = PortType.PREVIOUS),
                    Port(name = "next", x = 100f, y = 80f, portType = PortType.NEXT),
                ),
                contentAreas = listOf(
                    ContentArea(name = "body", bounds = RectF(20f, 10f, 180f, 70f), contentType = "compose"),
                ),
            ),
        )
        val document = VectorDocument(name = "Bound").addObjectToLayer(0, objectNode)
        val asset = newVisualAsset("bound-block", "Bound Block", document).copy(
            identity = VisualAssetIdentity("bound-block", "Bound Block", VisualAssetType.BLOCK_SHAPE),
        )
        EmaVisualAssetStore.importRaw(directory, ShapeMakerEmaCodec.encode(asset))
        VisualAssetCatalogStore.save(
            directory.resolve("catalog.json"),
            VisualAssetCatalog(
                bindings = listOf(
                    VisualAssetBinding("bound-block", EmaVisualAssetBindingTarget.Block, "control.if"),
                ),
            ),
        )

        val runtime = VisualAssetPresentationRuntime.load(directory)
        val geometry = runtime.geometryFor(EmaVisualAssetBindingTarget.Block, "control.if")
        val semantics = runtime.semanticsFor(EmaVisualAssetBindingTarget.Block, "control.if")

        assertNotNull(geometry)
        assertEquals(true, geometry!!.segments.isNotEmpty())
        assertEquals(listOf("previous", "next"), semantics!!.ports.map { it.name })
        assertEquals("compose", semantics.contentAreas.single().contentType)
        assertNull(runtime.geometryFor(EmaVisualAssetBindingTarget.Block, "action.wait"))
    }
}
