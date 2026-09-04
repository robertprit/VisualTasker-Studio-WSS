package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceResourcesTest {
    @Test
    fun acceptsMarkerTemplateAndNormalizedRegionResources() {
        val marker = WorkspaceResource(
            id = "marker:login-button",
            kind = WorkspaceResourceKind.Marker,
            label = "Login Button",
            packageName = "com.example.app",
            activityClass = "com.example.app.MainActivity",
            markerMode = WorkspaceMarkerMode.Region,
            region = WorkspaceRegionBounds(0.1f, 0.2f, 0.4f, 0.5f),
            referenceWidthPx = 1080,
            referenceHeightPx = 2400,
            tags = setOf("login", "a11y"),
        )
        val template = WorkspaceResource(
            id = "template:login-button",
            kind = WorkspaceResourceKind.Template,
            label = "Login Button Template",
            uri = "workspace://templates/login-button.png",
            mimeType = "image/png",
            metadata = mapOf("threshold" to "0.82"),
        )

        val bundle = WorkspaceResourceReducer
            .upsert(WorkspaceResourceBundle(), marker)
            .let { WorkspaceResourceReducer.upsert(it, template) }

        assertEquals(2, bundle.revision)
        assertEquals(marker, bundle.find("marker:login-button"))
        assertEquals("com.example.app", bundle.find("marker:login-button")?.packageName)
        assertEquals(WorkspaceMarkerMode.Region, bundle.find("marker:login-button")?.markerMode)
        assertEquals(listOf(template), bundle.byKind(WorkspaceResourceKind.Template))
    }

    @Test
    fun acceptsPointMarkerResources() {
        val marker = WorkspaceResource(
            id = "marker:tap-login",
            kind = WorkspaceResourceKind.Marker,
            label = "Tap Login",
            markerMode = WorkspaceMarkerMode.Point,
            point = WorkspacePointBounds(0.5f, 0.75f),
            referenceWidthPx = 1080,
            referenceHeightPx = 2400,
            metadata = mapOf("elementReference" to "button.login"),
        )

        val bundle = WorkspaceResourceReducer.upsert(WorkspaceResourceBundle(), marker)

        assertEquals(marker, bundle.find("marker:tap-login"))
        assertEquals(WorkspaceMarkerMode.Point, marker.markerMode)
        assertEquals(0.5f, marker.point!!.x)
    }

    @Test
    fun rejectsInvalidRegionPointReferenceBoundsAndDuplicateIds() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkspaceRegionBounds(0.8f, 0.1f, 0.2f, 0.4f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePointBounds(1.2f, 0.4f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkspaceResource(
                id = "marker:bad-reference",
                kind = WorkspaceResourceKind.Marker,
                label = "Bad Reference",
                referenceWidthPx = 0,
            )
        }

        val marker = WorkspaceResource(
            id = "marker:duplicate",
            kind = WorkspaceResourceKind.Marker,
            label = "Duplicate",
        )
        assertThrows(IllegalArgumentException::class.java) {
            WorkspaceResourceBundle(resources = listOf(marker, marker))
        }
    }

    @Test
    fun upsertReplacesExistingResourceAndRemoveBumpsOnlyWhenFound() {
        val first = WorkspaceResource(
            id = "screenshot:home",
            kind = WorkspaceResourceKind.Screenshot,
            label = "Home",
            uri = "workspace://screens/home-1.png",
        )
        val second = first.copy(uri = "workspace://screens/home-2.png")

        val inserted = WorkspaceResourceReducer.upsert(WorkspaceResourceBundle(), first)
        val replaced = WorkspaceResourceReducer.upsert(inserted, second)
        val missingRemove = WorkspaceResourceReducer.remove(replaced, "screenshot:missing")
        val removed = WorkspaceResourceReducer.remove(replaced, "screenshot:home")

        assertEquals(1, inserted.revision)
        assertEquals(2, replaced.revision)
        assertEquals(second, replaced.find("screenshot:home"))
        assertEquals(replaced.revision, missingRemove.revision)
        assertEquals(3, removed.revision)
        assertNull(removed.find("screenshot:home"))
        assertTrue(removed.resources.isEmpty())
    }

    @Test
    fun normalizesLegacyPixelRegionAndPointCoordinates() {
        val region = normalizedRegionFromPixels(
            leftPx = 108,
            topPx = 240,
            rightPx = 540,
            bottomPx = 1200,
            referenceWidthPx = 1080,
            referenceHeightPx = 2400,
        )
        val point = normalizedPointFromPixels(
            xPx = 2160,
            yPx = -10,
            referenceWidthPx = 1080,
            referenceHeightPx = 2400,
        )

        assertEquals(0.1f, region.left)
        assertEquals(0.1f, region.top)
        assertEquals(0.5f, region.right)
        assertEquals(0.5f, region.bottom)
        assertEquals(1.0f, point.x)
        assertEquals(0.0f, point.y)
    }
}
