package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyStudioResourceMapperTest {
    @Test
    fun mapsScreenshotAssetToWorkspaceResource() {
        val resource = LegacyStudioResourceMapper.screenshot(
            LegacyStudioScreenshotAsset(
                screenshotId = "Home Screen 01",
                fileName = "home.png",
                path = "/captures/home.png",
                width = 1080,
                height = 2400,
                timestamp = 42L,
            ),
        )

        assertEquals("screenshot:home-screen-01", resource.id)
        assertEquals(WorkspaceResourceKind.Screenshot, resource.kind)
        assertEquals("home.png", resource.label)
        assertEquals("image/png", resource.mimeType)
        assertEquals(1080, resource.referenceWidthPx)
        assertEquals(2400, resource.referenceHeightPx)
        assertEquals("Home Screen 01", resource.metadata["screenshotId"])
        assertEquals(42L, resource.createdAtEpochMs)
    }

    @Test
    fun mapsTemplateAssetWithRegionAndSearchMetadata() {
        val resource = LegacyStudioResourceMapper.template(
            LegacyStudioTemplateAsset(
                id = 7L,
                templateName = " Login Button ",
                sourceApp = "com.example.app",
                imagePath = "/templates/login.webp",
                screenshotId = "screen-7",
                screenshotPath = "/screens/screen-7.png",
                region = LegacyStudioTemplateRegion(x = 100, y = 200, width = 300, height = 400),
                width = 1000,
                height = 2000,
                creationDate = 99L,
                matchThreshold = 0.83f,
                searchRegionName = "main",
                searchRegion = LegacyStudioTemplateRegion(x = 50, y = 100, width = 900, height = 1800),
                processingMode = LegacyStudioTemplateProcessingMode.HIGH_CONTRAST,
                version = 3,
                markerMode = LegacyStudioTemplateMarkerMode.REGION,
                colourHex = "#ff00aa",
                ocrText = "Login",
            ),
        )

        assertEquals("template:7", resource.id)
        assertEquals(WorkspaceResourceKind.Template, resource.kind)
        assertEquals("Login Button", resource.label)
        assertEquals("visualtasker.vision.template", resource.pluginOwner)
        assertEquals("image/webp", resource.mimeType)
        assertEquals("com.example.app", resource.packageName)
        assertEquals(WorkspaceMarkerMode.Region, resource.markerMode)
        assertEquals(0.1f, resource.region!!.left)
        assertEquals(0.1f, resource.region!!.top)
        assertEquals(0.4f, resource.region!!.right)
        assertEquals(0.3f, resource.region!!.bottom)
        assertEquals("0.83", resource.metadata["matchThreshold"])
        assertEquals("0.05", resource.metadata["searchRegion.left"])
        assertEquals("0.95", resource.metadata["searchRegion.right"])
        assertEquals("HIGH_CONTRAST", resource.metadata["processingMode"])
        assertEquals("#ff00aa", resource.metadata["colourHex"])
        assertEquals("Login", resource.metadata["ocrText"])
    }

    @Test
    fun mapsSavedMarkerAndLegacyMobileSamAsRegion() {
        val resource = LegacyStudioResourceMapper.savedMarker(
            marker = LegacyStudioTemplateSavedMarker(
                id = 12L,
                label = "Crop",
                region = LegacyStudioTemplateRegion(x = 10, y = 20, width = 40, height = 80),
                markerMode = LegacyStudioTemplateMarkerMode.MOBILE_SAM,
                colourHex = "#00ffaa",
                ocrText = null,
            ),
            referenceWidthPx = 100,
            referenceHeightPx = 200,
        )

        assertEquals("marker:template:12", resource.id)
        assertEquals(WorkspaceResourceKind.Marker, resource.kind)
        assertEquals(WorkspaceMarkerMode.Region, resource.markerMode)
        assertEquals(0.1f, resource.region!!.left)
        assertEquals(0.5f, resource.region!!.bottom)
        assertEquals("#00ffaa", resource.metadata["colourHex"])
        assertNull(resource.metadata["ocrText"])
    }

    @Test
    fun mapsOverlayPointMarkerUsingStoredNormalizedCoordinates() {
        val resource = LegacyStudioResourceMapper.pointMarker(
            LegacyStudioPointMarker(
                id = "Tap Login",
                packageName = "com.example.app",
                activityClass = "MainActivity",
                createdAt = 123L,
                xPx = 100,
                yPx = 200,
                referenceWidthPx = 1000,
                referenceHeightPx = 2000,
                normalizedX = 0.42f,
                normalizedY = 0.24f,
                elementReference = "button.login",
            ),
        )

        assertEquals("marker:point:tap-login", resource.id)
        assertEquals(WorkspaceMarkerMode.Point, resource.markerMode)
        assertEquals(0.42f, resource.point!!.x)
        assertEquals(0.24f, resource.point!!.y)
        assertEquals("button.login", resource.label)
        assertEquals("visualtasker.accessibility.overlay", resource.pluginOwner)
    }

    @Test
    fun mapsOverlayRegionMarkerAndFallsBackToPixelBoundsWhenNormalizedBoundsAreInvalid() {
        val resource = LegacyStudioResourceMapper.regionMarker(
            LegacyStudioRegionMarker(
                id = "Region A",
                packageName = null,
                activityClass = "MainActivity",
                createdAt = 123L,
                updatedAt = 456L,
                leftPx = 25,
                topPx = 50,
                rightPx = 125,
                bottomPx = 250,
                referenceWidthPx = 500,
                referenceHeightPx = 1000,
                normalizedLeft = -1f,
                normalizedTop = -1f,
                normalizedRight = -1f,
                normalizedBottom = -1f,
                type = "OCR",
                hidden = true,
                locked = true,
            ),
        )

        assertEquals("marker:region:region-a", resource.id)
        assertEquals(WorkspaceResourceKind.Marker, resource.kind)
        assertEquals(WorkspaceMarkerMode.Region, resource.markerMode)
        assertEquals(0.05f, resource.region!!.left)
        assertEquals(0.25f, resource.region!!.bottom)
        assertEquals(true, resource.hidden)
        assertEquals(true, resource.locked)
        assertEquals("OCR", resource.metadata["type"])
        assertEquals(456L, resource.updatedAtEpochMs)
    }

    @Test
    fun buildsBundleWithoutDuplicateLegacyIds() {
        val first = LegacyStudioScreenshotAsset(
            screenshotId = "Same",
            fileName = "first.jpg",
            path = "/a/first.jpg",
            width = 10,
            height = 20,
            timestamp = 1L,
        )
        val duplicate = first.copy(fileName = "second.jpg")

        val bundle = LegacyStudioResourceMapper.bundle(
            screenshots = listOf(first, duplicate),
            savedMarkerReferenceWidthPx = 10,
            savedMarkerReferenceHeightPx = 20,
        )

        assertEquals(1, bundle.resources.size)
        assertNotNull(bundle.find("screenshot:same"))
    }
}
