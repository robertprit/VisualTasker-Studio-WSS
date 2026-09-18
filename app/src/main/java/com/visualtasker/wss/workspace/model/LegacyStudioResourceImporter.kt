package com.visualtasker.wss.workspace.model

import org.json.JSONArray
import org.json.JSONObject

enum class LegacyStudioImportFormat {
    RESOURCE_EXPORT,
    TEMPLATE_MANIFEST,
    TEMPLATE_FIND_EXPORT,
}

data class LegacyStudioImportDiagnostic(
    val code: String,
    val message: String,
)

sealed interface LegacyStudioResourceImportResult {
    data class Imported(
        val bundle: WorkspaceResourceBundle,
        val format: LegacyStudioImportFormat,
        val diagnostics: List<LegacyStudioImportDiagnostic> = emptyList(),
    ) : LegacyStudioResourceImportResult {
        val importedCount: Int get() = bundle.resources.size
    }

    data class Invalid(val message: String) : LegacyStudioResourceImportResult
}

/**
 * Reads the concrete JSON shapes emitted or persisted by the legacy Studio app.
 * Invalid entries are isolated and reported; valid siblings remain importable.
 */
object LegacyStudioResourceImporter {
    const val FORMAT = "visualtasker.legacy-studio-resources"

    fun decode(raw: String): LegacyStudioResourceImportResult {
        val root = runCatching { JSONObject(raw) }
            .getOrElse { return LegacyStudioResourceImportResult.Invalid(it.message ?: "Ungueltiges JSON") }
        return when {
            root.has("templateRegion") && root.has("sourceScreenshotFile") -> importTemplateManifest(root)
            root.optString("action").equals("find_template", ignoreCase = true) -> importTemplateFindExport(root)
            hasResourceCollections(root) -> importResourceExport(root)
            else -> LegacyStudioResourceImportResult.Invalid(
                "Kein unterstuetztes VisualTasker-Studio-Ressourcenformat erkannt.",
            )
        }
    }

    private fun importResourceExport(root: JSONObject): LegacyStudioResourceImportResult {
        val diagnostics = mutableListOf<LegacyStudioImportDiagnostic>()
        val screenshots = parseArray(root, "screenshots", diagnostics, ::parseScreenshot)
        val screenshotById = screenshots.associateBy { it.screenshotId }
        val templates = parseArray(root, "templates", diagnostics, ::parseTemplate)
        val savedMarkers = parseArray(root, "savedMarkers", diagnostics, ::parseSavedMarker)
        val pointMarkers = parseArray(root, "pointMarkers", diagnostics, ::parsePointMarker)
        val regionMarkers = parseArray(root, "regionMarkers", diagnostics, ::parseRegionMarker)
        val fallbackWidth = root.positiveInt("referenceWidthPx") ?: root.positiveInt("screenshotWidth")
        val fallbackHeight = root.positiveInt("referenceHeightPx") ?: root.positiveInt("screenshotHeight")

        val resources = buildList {
            screenshots.mapTo(this, LegacyStudioResourceMapper::screenshot)
            templates.forEach { template ->
                val screenshot = screenshotById[template.screenshotId]
                val referenceWidth = screenshot?.width ?: fallbackWidth ?: template.derivedReferenceWidth()
                val referenceHeight = screenshot?.height ?: fallbackHeight ?: template.derivedReferenceHeight()
                if (screenshot == null && (fallbackWidth == null || fallbackHeight == null)) {
                    diagnostics += LegacyStudioImportDiagnostic(
                        code = "LEGACY_TEMPLATE_REFERENCE_DERIVED",
                        message = "Template ${template.id}: Referenzgroesse aus Regionen abgeleitet.",
                    )
                }
                add(LegacyStudioResourceMapper.template(template, referenceWidth, referenceHeight))
            }
            savedMarkers.forEachIndexed { index, marker ->
                val item = root.optJSONArray("savedMarkers")?.optJSONObject(index)
                val width = item?.positiveInt("referenceWidthPx") ?: fallbackWidth
                val height = item?.positiveInt("referenceHeightPx") ?: fallbackHeight
                if (width == null || height == null) {
                    diagnostics += LegacyStudioImportDiagnostic(
                        code = "LEGACY_SAVED_MARKER_REFERENCE_MISSING",
                        message = "savedMarkers[$index]: Referenzgroesse fehlt; Eintrag uebersprungen.",
                    )
                } else {
                    add(LegacyStudioResourceMapper.savedMarker(marker, width, height))
                }
            }
            pointMarkers.mapTo(this, LegacyStudioResourceMapper::pointMarker)
            regionMarkers.mapTo(this, LegacyStudioResourceMapper::regionMarker)
        }.preferNewestById()

        if (resources.isEmpty()) {
            return LegacyStudioResourceImportResult.Invalid(
                diagnostics.joinToString(" ") { it.message }.ifBlank { "Ressourcenexport ist leer." },
            )
        }
        return LegacyStudioResourceImportResult.Imported(
            bundle = WorkspaceResourceBundle(resources = resources),
            format = LegacyStudioImportFormat.RESOURCE_EXPORT,
            diagnostics = diagnostics,
        )
    }

    private fun importTemplateManifest(root: JSONObject): LegacyStudioResourceImportResult {
        val result = runCatching {
            val width = root.requirePositiveInt("screenshotWidth")
            val height = root.requirePositiveInt("screenshotHeight")
            val sourceFile = root.requireString("sourceScreenshotFile")
            val templateFile = root.requireString("templateFile")
            val creationDate = root.optLong("creationDate", 0L)
            val screenshot = LegacyStudioScreenshotAsset(
                screenshotId = sourceFile,
                fileName = sourceFile,
                path = root.requireString("screenshotPath"),
                width = width,
                height = height,
                timestamp = creationDate,
            )
            val templateRegion = root.requireRegion("templateRegion")
            val template = LegacyStudioTemplateAsset(
                id = stableLegacyLongId("${root.optString("packageName")}:$templateFile"),
                templateName = templateFile.substringBeforeLast('.').ifBlank { templateFile },
                sourceApp = root.optString("packageName"),
                imagePath = templateFile,
                screenshotId = sourceFile,
                screenshotPath = screenshot.path,
                region = templateRegion,
                width = templateRegion.width,
                height = templateRegion.height,
                creationDate = creationDate,
                matchThreshold = root.optDouble("matchThreshold", 0.8).toFloat(),
                searchRegionName = root.optString("searchRegionName", "screen"),
                searchRegion = root.requireRegion("searchRegion"),
                processingMode = enumOrDefault(
                    root.optString("processingMode"),
                    LegacyStudioTemplateProcessingMode.ORIGINAL,
                ),
                version = root.optInt("schemaVersion", 1).coerceAtLeast(1),
                markerMode = enumOrDefault(
                    root.optString("markerMode"),
                    LegacyStudioTemplateMarkerMode.REGION,
                ),
                colourHex = root.nullableString("colourHex"),
                ocrText = root.nullableString("ocrText"),
            )
            WorkspaceResourceBundle(
                resources = listOf(
                    LegacyStudioResourceMapper.screenshot(screenshot),
                    LegacyStudioResourceMapper.template(template, width, height),
                ),
            )
        }
        return result.fold(
            onSuccess = {
                LegacyStudioResourceImportResult.Imported(
                    bundle = it,
                    format = LegacyStudioImportFormat.TEMPLATE_MANIFEST,
                )
            },
            onFailure = { LegacyStudioResourceImportResult.Invalid(it.message ?: "Ungueltiges Template-Manifest.") },
        )
    }

    private fun importTemplateFindExport(root: JSONObject): LegacyStudioResourceImportResult {
        val result = runCatching {
            val templateBounds = root.requireRegionArray("templateBounds")
            val searchBounds = root.requireRegionArray("searchBounds")
            val referenceWidth = maxOf(
                templateBounds.x + templateBounds.width,
                searchBounds.x + searchBounds.width,
            ).coerceAtLeast(1)
            val referenceHeight = maxOf(
                templateBounds.y + templateBounds.height,
                searchBounds.y + searchBounds.height,
            ).coerceAtLeast(1)
            val name = root.requireString("template")
            val template = LegacyStudioTemplateAsset(
                id = stableLegacyLongId(name),
                templateName = name.substringBeforeLast('.').ifBlank { name },
                sourceApp = "",
                imagePath = name,
                screenshotId = "",
                screenshotPath = "",
                region = templateBounds,
                width = templateBounds.width,
                height = templateBounds.height,
                creationDate = 0L,
                matchThreshold = root.optDouble("threshold", 0.8).toFloat(),
                searchRegionName = root.optString("searchRegion", "screen"),
                searchRegion = searchBounds,
                processingMode = enumOrDefault(
                    root.optString("processingMode"),
                    LegacyStudioTemplateProcessingMode.ORIGINAL,
                ),
            )
            WorkspaceResourceBundle(
                resources = listOf(LegacyStudioResourceMapper.template(template, referenceWidth, referenceHeight)),
            )
        }
        return result.fold(
            onSuccess = {
                LegacyStudioResourceImportResult.Imported(
                    bundle = it,
                    format = LegacyStudioImportFormat.TEMPLATE_FIND_EXPORT,
                    diagnostics = listOf(
                        LegacyStudioImportDiagnostic(
                            code = "LEGACY_TEMPLATE_REFERENCE_DERIVED",
                            message = "Referenzgroesse aus den exportierten Suchgrenzen abgeleitet.",
                        ),
                    ),
                )
            },
            onFailure = { LegacyStudioResourceImportResult.Invalid(it.message ?: "Ungueltiger Template-Export.") },
        )
    }

    private fun parseScreenshot(json: JSONObject): LegacyStudioScreenshotAsset =
        LegacyStudioScreenshotAsset(
            screenshotId = json.requireString("screenshotId"),
            fileName = json.requireString("fileName"),
            path = json.requireString("path"),
            width = json.requirePositiveInt("width"),
            height = json.requirePositiveInt("height"),
            timestamp = json.optLong("timestamp", 0L),
        )

    private fun parseTemplate(json: JSONObject): LegacyStudioTemplateAsset {
        val region = json.optJSONObject("region")?.toRegion()
            ?: json.toFlatRegion("region")
        val searchRegion = json.optJSONObject("searchRegion")?.toRegion()
            ?: json.toFlatRegion("searchRegion")
        return LegacyStudioTemplateAsset(
            id = json.getLong("id"),
            templateName = json.requireString("templateName"),
            sourceApp = json.optString("sourceApp"),
            imagePath = json.requireString("imagePath"),
            screenshotId = json.optString("screenshotId"),
            screenshotPath = json.optString("screenshotPath"),
            region = region,
            width = json.requirePositiveInt("width"),
            height = json.requirePositiveInt("height"),
            creationDate = json.optLong("creationDate", 0L),
            matchThreshold = json.optDouble("matchThreshold", 0.8).toFloat(),
            searchRegionName = json.optString("searchRegionName", "screen"),
            searchRegion = searchRegion,
            processingMode = enumOrDefault(
                json.optString("processingMode"),
                LegacyStudioTemplateProcessingMode.ORIGINAL,
            ),
            version = json.optInt("version", 1).coerceAtLeast(1),
            markerMode = enumOrDefault(
                json.optString("markerMode"),
                LegacyStudioTemplateMarkerMode.REGION,
            ),
            colourHex = json.nullableString("colourHex"),
            ocrText = json.nullableString("ocrText"),
        )
    }

    private fun parseSavedMarker(json: JSONObject): LegacyStudioTemplateSavedMarker =
        LegacyStudioTemplateSavedMarker(
            id = json.getLong("id"),
            label = json.requireString("label"),
            region = json.requireRegion("region"),
            markerMode = enumOrDefault(
                json.optString("markerMode"),
                LegacyStudioTemplateMarkerMode.REGION,
            ),
            colourHex = json.nullableString("colourHex"),
            ocrText = json.nullableString("ocrText"),
        )

    private fun parsePointMarker(json: JSONObject): LegacyStudioPointMarker =
        LegacyStudioPointMarker(
            id = json.requireString("id"),
            packageName = json.nullableString("packageName"),
            activityClass = json.requireString("activityClass"),
            createdAt = json.optLong("createdAt", 0L),
            xPx = json.getInt("xPx"),
            yPx = json.getInt("yPx"),
            referenceWidthPx = json.requirePositiveInt("referenceWidthPx"),
            referenceHeightPx = json.requirePositiveInt("referenceHeightPx"),
            normalizedX = json.optDouble("normalizedX", -1.0).toFloat(),
            normalizedY = json.optDouble("normalizedY", -1.0).toFloat(),
            elementReference = json.nullableString("elementReference"),
        )

    private fun parseRegionMarker(json: JSONObject): LegacyStudioRegionMarker =
        LegacyStudioRegionMarker(
            id = json.requireString("id"),
            packageName = json.nullableString("packageName"),
            activityClass = json.requireString("activityClass"),
            createdAt = json.optLong("createdAt", 0L),
            updatedAt = json.optLong("updatedAt", json.optLong("createdAt", 0L)),
            leftPx = json.getInt("leftPx"),
            topPx = json.getInt("topPx"),
            rightPx = json.getInt("rightPx"),
            bottomPx = json.getInt("bottomPx"),
            referenceWidthPx = json.requirePositiveInt("referenceWidthPx"),
            referenceHeightPx = json.requirePositiveInt("referenceHeightPx"),
            normalizedLeft = json.optDouble("normalizedLeft", -1.0).toFloat(),
            normalizedTop = json.optDouble("normalizedTop", -1.0).toFloat(),
            normalizedRight = json.optDouble("normalizedRight", -1.0).toFloat(),
            normalizedBottom = json.optDouble("normalizedBottom", -1.0).toFloat(),
            type = json.optString("type", "SEARCH"),
            hidden = json.optBoolean("hidden", false),
            locked = json.optBoolean("locked", false),
        )
}

private fun hasResourceCollections(root: JSONObject): Boolean =
    listOf("screenshots", "templates", "savedMarkers", "pointMarkers", "regionMarkers").any(root::has)

private inline fun <T> parseArray(
    root: JSONObject,
    key: String,
    diagnostics: MutableList<LegacyStudioImportDiagnostic>,
    parser: (JSONObject) -> T,
): List<T> {
    val array = root.optJSONArray(key) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            runCatching { parser(array.getJSONObject(index)) }
                .onSuccess(::add)
                .onFailure {
                    diagnostics += LegacyStudioImportDiagnostic(
                        code = "LEGACY_${key.uppercase()}_ENTRY_INVALID",
                        message = "$key[$index]: ${it.message ?: "ungueltiger Eintrag"}",
                    )
                }
        }
    }
}

private fun JSONObject.requireString(key: String): String =
    getString(key).trim().takeIf(String::isNotBlank) ?: error("$key fehlt.")

private fun JSONObject.requirePositiveInt(key: String): Int =
    getInt(key).takeIf { it > 0 } ?: error("$key muss positiv sein.")

private fun JSONObject.positiveInt(key: String): Int? =
    optInt(key, 0).takeIf { it > 0 }

private fun JSONObject.nullableString(key: String): String? =
    takeUnless { isNull(key) }?.optString(key)?.trim()?.takeIf(String::isNotBlank)

private fun JSONObject.requireRegion(key: String): LegacyStudioTemplateRegion =
    getJSONObject(key).toRegion()

private fun JSONObject.toRegion(): LegacyStudioTemplateRegion =
    LegacyStudioTemplateRegion(
        x = getInt("x"),
        y = getInt("y"),
        width = requirePositiveInt("width"),
        height = requirePositiveInt("height"),
    )

private fun JSONObject.toFlatRegion(prefix: String): LegacyStudioTemplateRegion =
    LegacyStudioTemplateRegion(
        x = getInt("${prefix}X"),
        y = getInt("${prefix}Y"),
        width = requirePositiveInt("${prefix}Width"),
        height = requirePositiveInt("${prefix}Height"),
    )

private fun JSONObject.requireRegionArray(key: String): LegacyStudioTemplateRegion {
    val array = getJSONArray(key)
    require(array.length() == 4) { "$key muss [x,y,width,height] enthalten." }
    return LegacyStudioTemplateRegion(
        x = array.getInt(0),
        y = array.getInt(1),
        width = array.getInt(2).takeIf { it > 0 } ?: error("$key width muss positiv sein."),
        height = array.getInt(3).takeIf { it > 0 } ?: error("$key height muss positiv sein."),
    )
}

private fun LegacyStudioTemplateAsset.derivedReferenceWidth(): Int =
    maxOf(region.x + region.width, searchRegion.x + searchRegion.width, width).coerceAtLeast(1)

private fun LegacyStudioTemplateAsset.derivedReferenceHeight(): Int =
    maxOf(region.y + region.height, searchRegion.y + searchRegion.height, height).coerceAtLeast(1)

private fun stableLegacyLongId(value: String): Long {
    var hash = -0x340d631b7bdddcdbL
    value.forEach { character ->
        hash = hash xor character.code.toLong()
        hash *= 0x100000001b3L
    }
    return (hash and Long.MAX_VALUE).coerceAtLeast(1L)
}

private inline fun <reified T : Enum<T>> enumOrDefault(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: fallback
