package com.visualtasker.wss.workspace.model

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

const val VISUAL_ASSET_PACKAGE_FORMAT = "visualtasker_visual_asset_package"
const val VISUAL_ASSET_PACKAGE_SCHEMA_VERSION = 1

data class VisualAssetPackage(
    val catalog: VisualAssetCatalog,
    val assetDocuments: List<String>,
)

sealed interface VisualAssetPackageDecodeResult {
    data class Decoded(val value: VisualAssetPackage) : VisualAssetPackageDecodeResult
    data class UnsupportedVersion(val version: Int) : VisualAssetPackageDecodeResult
    data class Invalid(val message: String) : VisualAssetPackageDecodeResult
}

data class VisualAssetPackageImportResult(
    val catalog: VisualAssetCatalog,
    val importedAssets: List<StoredEmaVisualAsset>,
    val rejectedDocuments: Int,
)

object VisualAssetPackageCodec {
    fun encode(catalog: VisualAssetCatalog, assets: List<StoredEmaVisualAsset>): String = JSONObject().apply {
        put("format", VISUAL_ASSET_PACKAGE_FORMAT)
        put("schemaVersion", VISUAL_ASSET_PACKAGE_SCHEMA_VERSION)
        put("catalog", JSONObject(VisualAssetCatalogCodec.encode(catalog)))
        put("assets", JSONArray().apply {
            assets
                .sortedWith(compareBy<StoredEmaVisualAsset> { it.descriptor.assetId }.thenBy { it.descriptor.version })
                .forEach { asset -> put(JSONObject(asset.file.readText())) }
        })
    }.toString(2)

    fun decode(raw: String): VisualAssetPackageDecodeResult = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == VISUAL_ASSET_PACKAGE_FORMAT) {
            "Unsupported visual asset package format."
        }
        val version = root.optInt("schemaVersion", -1)
        if (version != VISUAL_ASSET_PACKAGE_SCHEMA_VERSION) {
            return VisualAssetPackageDecodeResult.UnsupportedVersion(version)
        }
        val catalogRaw = root.optJSONObject("catalog")?.toString()
            ?: return VisualAssetPackageDecodeResult.Invalid("Asset package catalog is missing.")
        val catalog = when (val decoded = VisualAssetCatalogCodec.decode(catalogRaw)) {
            is VisualAssetCatalogDecodeResult.Decoded -> decoded.catalog
            is VisualAssetCatalogDecodeResult.Invalid -> return VisualAssetPackageDecodeResult.Invalid(decoded.message)
            is VisualAssetCatalogDecodeResult.UnsupportedVersion -> {
                return VisualAssetPackageDecodeResult.Invalid("Unsupported catalog version ${decoded.version}.")
            }
        }
        val assets = root.optJSONArray("assets") ?: JSONArray()
        VisualAssetPackageDecodeResult.Decoded(
            VisualAssetPackage(
                catalog = catalog,
                assetDocuments = (0 until assets.length()).mapNotNull { index ->
                    assets.optJSONObject(index)?.toString()
                },
            ),
        )
    }.getOrElse { VisualAssetPackageDecodeResult.Invalid(it.message ?: "Invalid visual asset package.") }
}

object VisualAssetPackageStore {
    fun import(
        directory: File,
        currentCatalog: VisualAssetCatalog,
        raw: String,
    ): VisualAssetPackageImportResult? {
        val decoded = VisualAssetPackageCodec.decode(raw) as? VisualAssetPackageDecodeResult.Decoded ?: return null
        val imported = mutableListOf<StoredEmaVisualAsset>()
        var rejected = 0
        decoded.value.assetDocuments.forEach { document ->
            when (val result = EmaVisualAssetStore.importRaw(directory, document)) {
                is EmaVisualAssetImportResult.Imported -> imported += result.asset
                is EmaVisualAssetImportResult.Rejected -> rejected += 1
            }
        }
        val importedIds = imported.mapTo(mutableSetOf()) { it.descriptor.assetId }
        val incoming = decoded.value.catalog
        val mergedBindings = incoming.bindings
            .filter { it.assetId in importedIds }
            .fold(currentCatalog.bindings) { bindings, binding ->
                bindings
                    .filterNot { it.target == binding.target && it.targetId == binding.targetId }
                    .plus(binding)
            }
        val incomingSets = incoming.toolboxSets.associateBy { it.id }
        val mergedSets = (currentCatalog.toolboxSets.map { it.id } + incoming.toolboxSets.map { it.id })
            .distinct()
            .map { id ->
                val current = currentCatalog.toolboxSets.firstOrNull { it.id == id }
                val addition = incomingSets[id]
                VisualAssetToolboxSet(
                    id = id,
                    name = addition?.name ?: current?.name ?: id,
                    assetIds = current?.assetIds.orEmpty() + addition?.assetIds.orEmpty().filter { it in importedIds },
                )
            }
        return VisualAssetPackageImportResult(
            catalog = currentCatalog.copy(bindings = mergedBindings, toolboxSets = mergedSets),
            importedAssets = imported,
            rejectedDocuments = rejected,
        )
    }
}
