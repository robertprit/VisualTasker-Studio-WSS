package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WorkspaceResourceCodecTest {
    @Test
    fun roundtripPreservesCompleteResource() {
        val resource = WorkspaceResource(
            id = "marker:login",
            kind = WorkspaceResourceKind.Marker,
            label = "Login",
            resourceVersion = 4,
            pluginOwner = "visualtasker.marker",
            uri = "workspace://marker/login",
            mimeType = "application/json",
            packageName = "com.example",
            activityClass = "MainActivity",
            markerMode = WorkspaceMarkerMode.Region,
            region = WorkspaceRegionBounds(0.1f, 0.2f, 0.7f, 0.8f),
            point = WorkspacePointBounds(0.4f, 0.5f),
            referenceWidthPx = 1080,
            referenceHeightPx = 2400,
            hidden = true,
            locked = true,
            tags = setOf("scene", "login"),
            metadata = mapOf("threshold" to "0.85"),
            createdAtEpochMs = 10L,
            updatedAtEpochMs = 20L,
        )
        val bundle = WorkspaceResourceBundle(revision = 4L, resources = listOf(resource))

        val decoded = WorkspaceResourceCodec.decode(WorkspaceResourceCodec.encode(bundle))

        assertTrue(decoded is WorkspaceResourceDecodeResult.Decoded)
        assertEquals(bundle, (decoded as WorkspaceResourceDecodeResult.Decoded).bundle)
    }

    @Test
    fun migratesUnversionedResourceBundle() {
        val raw = """{"resources":[{"id":"dataset:one","kind":"Dataset","label":"One","tags":"legacy,data"}]}"""

        val decoded = WorkspaceResourceCodec.decode(raw) as WorkspaceResourceDecodeResult.Decoded

        assertEquals(0, decoded.migratedFromVersion)
        assertEquals("visualtasker.legacy.import", decoded.bundle.resources.single().pluginOwner)
        assertEquals(setOf("legacy", "data"), decoded.bundle.resources.single().tags)
    }

    @Test
    fun migratesSchemaOneBundleToVisualAssetAwareSchema() {
        val raw = """{"schemaVersion":1,"resources":[{"id":"dataset:one","kind":"Dataset","label":"One"}]}"""

        val decoded = WorkspaceResourceCodec.decode(raw) as WorkspaceResourceDecodeResult.Decoded

        assertEquals(1, decoded.migratedFromVersion)
        assertEquals(WORKSPACE_RESOURCE_SCHEMA_VERSION, decoded.bundle.schemaVersion)
        assertEquals(WORKSPACE_RESOURCE_ITEM_VERSION, decoded.bundle.resources.single().resourceVersion)
    }

    @Test
    fun migratesSchemaTwoResourcesToItemVersionOne() {
        val raw = """{"schemaVersion":2,"resources":[{"id":"template:one","kind":"Template","label":"One"}]}"""

        val decoded = WorkspaceResourceCodec.decode(raw) as WorkspaceResourceDecodeResult.Decoded

        assertEquals(2, decoded.migratedFromVersion)
        assertEquals(WORKSPACE_RESOURCE_ITEM_VERSION, decoded.bundle.resources.single().resourceVersion)
    }

    @Test
    fun rejectsFutureSchemaVersion() {
        val decoded = WorkspaceResourceCodec.decode("""{"schemaVersion":99,"resources":[]}""")

        assertEquals(WorkspaceResourceDecodeResult.UnsupportedVersion(99), decoded)
    }

    @Test
    fun fileStorePersistsAndLoadsBundleAtomically() {
        val directory = Files.createTempDirectory("wss-resource-store").toFile()
        val target = directory.resolve("nested/resources.json")
        val bundle = WorkspaceResourceBundle(
            revision = 2,
            resources = listOf(
                WorkspaceResource(
                    id = "dataset:test",
                    kind = WorkspaceResourceKind.Dataset,
                    label = "Test",
                ),
            ),
        )

        WorkspaceResourceFileStore.save(target, bundle)
        val decoded = WorkspaceResourceFileStore.load(target) as WorkspaceResourceDecodeResult.Decoded

        assertEquals(bundle, decoded.bundle)
        assertTrue(target.isFile)
        assertTrue(target.parentFile.listFiles().none { it.name.endsWith(".tmp") })
    }
}
