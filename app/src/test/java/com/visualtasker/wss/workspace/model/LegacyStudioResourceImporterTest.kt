package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyStudioResourceImporterTest {
    @Test
    fun importsTemplateManifestUsingScreenshotReferenceSize() {
        val result = LegacyStudioResourceImporter.decode(
            """
            {
              "schemaVersion": 1,
              "sourceScreenshotFile": "screen.png",
              "templateFile": "login_tpl.png",
              "screenshotPath": "/screens/screen.png",
              "packageName": "com.example",
              "screenshotWidth": 1000,
              "screenshotHeight": 2000,
              "templateRegion": {"x": 100, "y": 400, "width": 200, "height": 100},
              "searchRegion": {"x": 0, "y": 200, "width": 1000, "height": 1200},
              "searchRegionName": "content",
              "matchThreshold": 0.86,
              "processingMode": "GRAYSCALE",
              "creationDate": 1234,
              "markerMode": "REGION",
              "ocrText": "Login"
            }
            """.trimIndent(),
        ) as LegacyStudioResourceImportResult.Imported

        assertEquals(LegacyStudioImportFormat.TEMPLATE_MANIFEST, result.format)
        assertEquals(2, result.importedCount)
        val template = result.bundle.byKind(WorkspaceResourceKind.Template).single()
        assertEquals(0.1f, template.region?.left)
        assertEquals(0.2f, template.region?.top)
        assertEquals(1000, template.referenceWidthPx)
        assertEquals("Login", template.metadata["ocrText"])
    }

    @Test
    fun importsAggregateRoomRowsAndKeepsValidSiblings() {
        val result = LegacyStudioResourceImporter.decode(
            """
            {
              "format": "visualtasker.legacy-studio-resources",
              "screenshots": [
                {"screenshotId":"screen-1","fileName":"screen.png","path":"/screen.png","width":1080,"height":2400,"timestamp":10}
              ],
              "templates": [
                {
                  "id":7,"templateName":"Login","sourceApp":"com.example","imagePath":"/login.png",
                  "screenshotId":"screen-1","screenshotPath":"/screen.png",
                  "regionX":108,"regionY":240,"regionWidth":216,"regionHeight":120,
                  "width":216,"height":120,"creationDate":11,"matchThreshold":0.8,
                  "searchRegionName":"screen","searchRegionX":0,"searchRegionY":0,
                  "searchRegionWidth":1080,"searchRegionHeight":2400,"processingMode":"ORIGINAL"
                },
                {"id":8,"templateName":"Broken"}
              ],
              "pointMarkers": [
                {
                  "id":"tap-login","packageName":"com.example","activityClass":"MainActivity","createdAt":12,
                  "xPx":540,"yPx":1200,"referenceWidthPx":1080,"referenceHeightPx":2400,
                  "normalizedX":0.5,"normalizedY":0.5,"elementReference":"button.login"
                }
              ]
            }
            """.trimIndent(),
        ) as LegacyStudioResourceImportResult.Imported

        assertEquals(3, result.importedCount)
        assertTrue(result.diagnostics.any { it.code == "LEGACY_TEMPLATES_ENTRY_INVALID" })
        val template = result.bundle.find("template:7")!!
        assertEquals(0.1f, template.region?.left)
        assertEquals(0.15f, template.region?.bottom)
        assertEquals(1080, template.referenceWidthPx)
    }

    @Test
    fun importsLegacyFindTemplateExport() {
        val result = LegacyStudioResourceImporter.decode(
            """
            {
              "action":"find_template",
              "template":"login_tpl.png",
              "threshold":0.9,
              "templateBounds":[100,200,300,100],
              "searchRegion":"screen",
              "searchBounds":[0,0,1080,2400],
              "processingMode":"HIGH_CONTRAST"
            }
            """.trimIndent(),
        ) as LegacyStudioResourceImportResult.Imported

        assertEquals(LegacyStudioImportFormat.TEMPLATE_FIND_EXPORT, result.format)
        assertEquals(1, result.importedCount)
        assertEquals("HIGH_CONTRAST", result.bundle.resources.single().metadata["processingMode"])
        assertTrue(result.diagnostics.any { it.code == "LEGACY_TEMPLATE_REFERENCE_DERIVED" })
    }

    @Test
    fun rejectsUnknownJsonShape() {
        val result = LegacyStudioResourceImporter.decode("{\"hello\":\"world\"}")
        assertTrue(result is LegacyStudioResourceImportResult.Invalid)
    }
}
