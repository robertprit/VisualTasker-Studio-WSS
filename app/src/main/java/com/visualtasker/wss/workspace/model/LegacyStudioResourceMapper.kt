package com.visualtasker.wss.workspace.model

data class LegacyStudioTemplateRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

enum class LegacyStudioTemplateMarkerMode {
    REGION,
    POINT,
    SWIPE,
    PATH,
    NODE,
    MOBILE_SAM,
}

enum class LegacyStudioTemplateProcessingMode {
    ORIGINAL,
    GRAYSCALE,
    HIGH_CONTRAST,
    EDGE,
    INVERSE,
}

data class LegacyStudioScreenshotAsset(
    val screenshotId: String,
    val fileName: String,
    val path: String,
    val width: Int,
    val height: Int,
    val timestamp: Long,
)

data class LegacyStudioTemplateAsset(
    val id: Long,
    val templateName: String,
    val sourceApp: String,
    val imagePath: String,
    val screenshotId: String,
    val screenshotPath: String,
    val region: LegacyStudioTemplateRegion,
    val width: Int,
    val height: Int,
    val creationDate: Long,
    val matchThreshold: Float,
    val searchRegionName: String,
    val searchRegion: LegacyStudioTemplateRegion,
    val processingMode: LegacyStudioTemplateProcessingMode,
    val version: Int = 1,
    val markerMode: LegacyStudioTemplateMarkerMode = LegacyStudioTemplateMarkerMode.REGION,
    val colourHex: String? = null,
    val ocrText: String? = null,
)

data class LegacyStudioTemplateSavedMarker(
    val id: Long,
    val label: String,
    val region: LegacyStudioTemplateRegion,
    val markerMode: LegacyStudioTemplateMarkerMode,
    val colourHex: String? = null,
    val ocrText: String? = null,
)

data class LegacyStudioPointMarker(
    val id: String,
    val packageName: String?,
    val activityClass: String,
    val createdAt: Long,
    val xPx: Int,
    val yPx: Int,
    val referenceWidthPx: Int,
    val referenceHeightPx: Int,
    val normalizedX: Float,
    val normalizedY: Float,
    val elementReference: String?,
)

data class LegacyStudioRegionMarker(
    val id: String,
    val packageName: String?,
    val activityClass: String,
    val createdAt: Long,
    val updatedAt: Long,
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int,
    val referenceWidthPx: Int,
    val referenceHeightPx: Int,
    val normalizedLeft: Float,
    val normalizedTop: Float,
    val normalizedRight: Float,
    val normalizedBottom: Float,
    val type: String,
    val hidden: Boolean,
    val locked: Boolean,
)

object LegacyStudioResourceMapper {
    fun screenshot(asset: LegacyStudioScreenshotAsset): WorkspaceResource =
        WorkspaceResource(
            id = legacyResourceId("screenshot", asset.screenshotId),
            kind = WorkspaceResourceKind.Screenshot,
            label = trimmedOrFallback(asset.fileName, "Screenshot ${asset.screenshotId}"),
            pluginOwner = "visualtasker.legacy.studio",
            uri = asset.path.ifBlank { null },
            mimeType = imageMimeType(asset.path),
            referenceWidthPx = asset.width,
            referenceHeightPx = asset.height,
            metadata = mapOfNonBlank(
                "legacyType" to "ScreenshotAsset",
                "screenshotId" to asset.screenshotId,
                "fileName" to asset.fileName,
            ),
            createdAtEpochMs = asset.timestamp,
            updatedAtEpochMs = asset.timestamp,
        )

    fun template(asset: LegacyStudioTemplateAsset): WorkspaceResource {
        val searchRight = asset.searchRegion.x + asset.searchRegion.width
        val searchBottom = asset.searchRegion.y + asset.searchRegion.height
        return WorkspaceResource(
            id = legacyResourceId("template", asset.id.toString()),
            kind = WorkspaceResourceKind.Template,
            label = trimmedOrFallback(asset.templateName, "Template ${asset.id}"),
            pluginOwner = "visualtasker.vision.template",
            uri = asset.imagePath.ifBlank { null },
            mimeType = imageMimeType(asset.imagePath),
            packageName = asset.sourceApp.ifBlank { null },
            markerMode = asset.markerMode.toWorkspaceMarkerMode(),
            region = asset.region.toWorkspaceRegion(asset.width, asset.height),
            referenceWidthPx = asset.width,
            referenceHeightPx = asset.height,
            tags = setOf("legacy-studio", "template"),
            metadata = mapOfNonBlank(
                "legacyType" to "TemplateAsset",
                "legacyId" to asset.id.toString(),
                "sourceApp" to asset.sourceApp,
                "screenshotId" to asset.screenshotId,
                "screenshotPath" to asset.screenshotPath,
                "matchThreshold" to asset.matchThreshold.toString(),
                "searchRegionName" to asset.searchRegionName,
                "searchRegion.left" to normalizedX(asset.searchRegion.x, asset.width).toString(),
                "searchRegion.top" to normalizedY(asset.searchRegion.y, asset.height).toString(),
                "searchRegion.right" to normalizedX(searchRight, asset.width).toString(),
                "searchRegion.bottom" to normalizedY(searchBottom, asset.height).toString(),
                "processingMode" to asset.processingMode.name,
                "version" to asset.version.toString(),
                "colourHex" to asset.colourHex.orEmpty(),
                "ocrText" to asset.ocrText.orEmpty(),
            ),
            createdAtEpochMs = asset.creationDate,
            updatedAtEpochMs = asset.creationDate,
        )
    }

    fun savedMarker(
        marker: LegacyStudioTemplateSavedMarker,
        referenceWidthPx: Int,
        referenceHeightPx: Int,
    ): WorkspaceResource =
        WorkspaceResource(
            id = legacyResourceId("marker:template", marker.id.toString()),
            kind = WorkspaceResourceKind.Marker,
            label = trimmedOrFallback(marker.label, "Template Marker ${marker.id}"),
            pluginOwner = "visualtasker.vision.template",
            markerMode = marker.markerMode.toWorkspaceMarkerMode(),
            region = marker.region.toWorkspaceRegion(referenceWidthPx, referenceHeightPx),
            referenceWidthPx = referenceWidthPx,
            referenceHeightPx = referenceHeightPx,
            tags = setOf("legacy-studio", "template-marker"),
            metadata = mapOfNonBlank(
                "legacyType" to "TemplateSavedMarker",
                "legacyId" to marker.id.toString(),
                "colourHex" to marker.colourHex.orEmpty(),
                "ocrText" to marker.ocrText.orEmpty(),
            ),
        )

    fun pointMarker(marker: LegacyStudioPointMarker): WorkspaceResource =
        WorkspaceResource(
            id = legacyResourceId("marker:point", marker.id),
            kind = WorkspaceResourceKind.Marker,
            label = trimmedOrFallback(marker.elementReference.orEmpty(), "Point ${marker.id}"),
            pluginOwner = "visualtasker.accessibility.overlay",
            packageName = marker.packageName,
            activityClass = marker.activityClass,
            markerMode = WorkspaceMarkerMode.Point,
            point = validPoint(marker.normalizedX, marker.normalizedY)
                ?: normalizedPointFromPixels(
                    xPx = marker.xPx,
                    yPx = marker.yPx,
                    referenceWidthPx = marker.referenceWidthPx,
                    referenceHeightPx = marker.referenceHeightPx,
                ),
            referenceWidthPx = marker.referenceWidthPx,
            referenceHeightPx = marker.referenceHeightPx,
            tags = setOf("legacy-studio", "overlay-marker"),
            metadata = mapOfNonBlank(
                "legacyType" to "PointMarker",
                "legacyId" to marker.id,
                "xPx" to marker.xPx.toString(),
                "yPx" to marker.yPx.toString(),
                "elementReference" to marker.elementReference.orEmpty(),
            ),
            createdAtEpochMs = marker.createdAt,
            updatedAtEpochMs = marker.createdAt,
        )

    fun regionMarker(marker: LegacyStudioRegionMarker): WorkspaceResource =
        WorkspaceResource(
            id = legacyResourceId("marker:region", marker.id),
            kind = WorkspaceResourceKind.Marker,
            label = "${marker.type.lowercase()} ${marker.id}".trim(),
            pluginOwner = "visualtasker.accessibility.overlay",
            packageName = marker.packageName,
            activityClass = marker.activityClass,
            markerMode = WorkspaceMarkerMode.Region,
            region = validRegion(
                marker.normalizedLeft,
                marker.normalizedTop,
                marker.normalizedRight,
                marker.normalizedBottom,
            ) ?: normalizedRegionFromPixels(
                leftPx = marker.leftPx,
                topPx = marker.topPx,
                rightPx = marker.rightPx,
                bottomPx = marker.bottomPx,
                referenceWidthPx = marker.referenceWidthPx,
                referenceHeightPx = marker.referenceHeightPx,
            ),
            referenceWidthPx = marker.referenceWidthPx,
            referenceHeightPx = marker.referenceHeightPx,
            hidden = marker.hidden,
            locked = marker.locked,
            tags = setOf("legacy-studio", "overlay-marker", marker.type.lowercase()),
            metadata = mapOfNonBlank(
                "legacyType" to "RegionMarker",
                "legacyId" to marker.id,
                "type" to marker.type,
                "leftPx" to marker.leftPx.toString(),
                "topPx" to marker.topPx.toString(),
                "rightPx" to marker.rightPx.toString(),
                "bottomPx" to marker.bottomPx.toString(),
            ),
            createdAtEpochMs = marker.createdAt,
            updatedAtEpochMs = marker.updatedAt,
        )

    fun bundle(
        screenshots: List<LegacyStudioScreenshotAsset> = emptyList(),
        templates: List<LegacyStudioTemplateAsset> = emptyList(),
        savedMarkers: List<LegacyStudioTemplateSavedMarker> = emptyList(),
        pointMarkers: List<LegacyStudioPointMarker> = emptyList(),
        regionMarkers: List<LegacyStudioRegionMarker> = emptyList(),
        savedMarkerReferenceWidthPx: Int,
        savedMarkerReferenceHeightPx: Int,
    ): WorkspaceResourceBundle {
        val resources = buildList {
            screenshots.mapTo(this) { screenshot(it) }
            templates.mapTo(this) { template(it) }
            savedMarkers.mapTo(this) {
                savedMarker(
                    marker = it,
                    referenceWidthPx = savedMarkerReferenceWidthPx,
                    referenceHeightPx = savedMarkerReferenceHeightPx,
                )
            }
            pointMarkers.mapTo(this) { pointMarker(it) }
            regionMarkers.mapTo(this) { regionMarker(it) }
        }
        return WorkspaceResourceBundle(resources = resources.distinctBy { it.id })
    }
}

private fun LegacyStudioTemplateRegion.toWorkspaceRegion(
    referenceWidthPx: Int,
    referenceHeightPx: Int,
): WorkspaceRegionBounds =
    normalizedRegionFromPixels(
        leftPx = x,
        topPx = y,
        rightPx = x + width,
        bottomPx = y + height,
        referenceWidthPx = referenceWidthPx,
        referenceHeightPx = referenceHeightPx,
    )

private fun LegacyStudioTemplateMarkerMode.toWorkspaceMarkerMode(): WorkspaceMarkerMode =
    when (this) {
        LegacyStudioTemplateMarkerMode.REGION,
        LegacyStudioTemplateMarkerMode.MOBILE_SAM -> WorkspaceMarkerMode.Region
        LegacyStudioTemplateMarkerMode.POINT -> WorkspaceMarkerMode.Point
        LegacyStudioTemplateMarkerMode.SWIPE -> WorkspaceMarkerMode.Swipe
        LegacyStudioTemplateMarkerMode.PATH -> WorkspaceMarkerMode.Path
        LegacyStudioTemplateMarkerMode.NODE -> WorkspaceMarkerMode.Node
    }

private fun validPoint(x: Float, y: Float): WorkspacePointBounds? =
    if (x in 0f..1f && y in 0f..1f) WorkspacePointBounds(x, y) else null

private fun validRegion(left: Float, top: Float, right: Float, bottom: Float): WorkspaceRegionBounds? =
    if (left in 0f..1f && right in 0f..1f && top in 0f..1f && bottom in 0f..1f && left < right && top < bottom) {
        WorkspaceRegionBounds(left, top, right, bottom)
    } else {
        null
    }

private fun normalizedX(xPx: Int, referenceWidthPx: Int): Float =
    (xPx.toFloat() / referenceWidthPx).coerceIn(0f, 1f)

private fun normalizedY(yPx: Int, referenceHeightPx: Int): Float =
    (yPx.toFloat() / referenceHeightPx).coerceIn(0f, 1f)

private fun legacyResourceId(prefix: String, raw: String): String {
    val safeRaw = raw
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9._:-]+"), "-")
        .trim('-', '.', '_', ':')
        .ifBlank { "unknown" }
    return "$prefix:$safeRaw"
}

private fun trimmedOrFallback(value: String, fallback: String): String =
    value.trim().ifBlank { fallback }

private fun mapOfNonBlank(vararg pairs: Pair<String, String>): Map<String, String> =
    pairs
        .mapNotNull { (key, value) ->
            val trimmedValue = value.trim()
            if (trimmedValue.isBlank()) null else key to trimmedValue
        }
        .toMap()

private fun imageMimeType(path: String): String? =
    when (path.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> null
    }
