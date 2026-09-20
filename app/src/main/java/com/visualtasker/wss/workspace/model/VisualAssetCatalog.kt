package com.visualtasker.wss.workspace.model

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

const val VISUAL_ASSET_CATALOG_SCHEMA_VERSION = 1

data class VisualAssetBinding(
    val assetId: String,
    val target: EmaVisualAssetBindingTarget,
    val targetId: String = "default",
)

data class VisualAssetToolboxSet(
    val id: String,
    val name: String,
    val assetIds: Set<String> = emptySet(),
)

data class VisualAssetCatalog(
    val schemaVersion: Int = VISUAL_ASSET_CATALOG_SCHEMA_VERSION,
    val bindings: List<VisualAssetBinding> = emptyList(),
    val toolboxSets: List<VisualAssetToolboxSet> = defaultVisualAssetToolboxSets(),
) {
    fun upsertBinding(binding: VisualAssetBinding): VisualAssetCatalog = copy(
        bindings = bindings
            .filterNot { it.target == binding.target && it.targetId == binding.targetId }
            .plus(binding),
    )

    fun removeAsset(assetId: String): VisualAssetCatalog = copy(
        bindings = bindings.filterNot { it.assetId == assetId },
        toolboxSets = toolboxSets.map { it.copy(assetIds = it.assetIds - assetId) },
    )

    fun toggleAssetInToolbox(setId: String, assetId: String): VisualAssetCatalog = copy(
        toolboxSets = toolboxSets.map { set ->
            if (set.id != setId) set else set.copy(
                assetIds = if (assetId in set.assetIds) set.assetIds - assetId else set.assetIds + assetId,
            )
        },
    )
}

fun defaultVisualAssetToolboxSets(): List<VisualAssetToolboxSet> = listOf(
    VisualAssetToolboxSet("custom", "Custom"),
    VisualAssetToolboxSet("project", "Projekt"),
    VisualAssetToolboxSet("favorites", "Favoriten"),
)

sealed interface VisualAssetCatalogDecodeResult {
    data class Decoded(val catalog: VisualAssetCatalog) : VisualAssetCatalogDecodeResult
    data class UnsupportedVersion(val version: Int) : VisualAssetCatalogDecodeResult
    data class Invalid(val message: String) : VisualAssetCatalogDecodeResult
}

object VisualAssetCatalogCodec {
    fun encode(catalog: VisualAssetCatalog): String = JSONObject().apply {
        put("schemaVersion", catalog.schemaVersion)
        put("bindings", JSONArray().apply {
            catalog.bindings.forEach { binding ->
                put(JSONObject().apply {
                    put("assetId", binding.assetId)
                    put("target", binding.target.name)
                    put("targetId", binding.targetId)
                })
            }
        })
        put("toolboxSets", JSONArray().apply {
            catalog.toolboxSets.forEach { set ->
                put(JSONObject().apply {
                    put("id", set.id)
                    put("name", set.name)
                    put("assetIds", JSONArray().apply { set.assetIds.sorted().forEach(::put) })
                })
            }
        })
    }.toString(2)

    fun decode(raw: String): VisualAssetCatalogDecodeResult = runCatching {
        val root = JSONObject(raw)
        val version = root.optInt("schemaVersion", -1)
        if (version != VISUAL_ASSET_CATALOG_SCHEMA_VERSION) {
            return VisualAssetCatalogDecodeResult.UnsupportedVersion(version)
        }
        val bindings = root.optJSONArray("bindings").objects().map { item ->
            VisualAssetBinding(
                assetId = item.getString("assetId"),
                target = enumValueOf(item.getString("target")),
                targetId = item.optString("targetId", "default"),
            )
        }
        val toolboxSets = root.optJSONArray("toolboxSets").objects().map { item ->
            VisualAssetToolboxSet(
                id = item.getString("id"),
                name = item.getString("name"),
                assetIds = item.optJSONArray("assetIds").strings(),
            )
        }.ifEmpty(::defaultVisualAssetToolboxSets)
        VisualAssetCatalogDecodeResult.Decoded(
            VisualAssetCatalog(bindings = bindings, toolboxSets = toolboxSets),
        )
    }.getOrElse { VisualAssetCatalogDecodeResult.Invalid(it.message ?: "Invalid visual asset catalog") }
}

object VisualAssetCatalogStore {
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()

    fun load(file: File): VisualAssetCatalog {
        if (!file.isFile) return VisualAssetCatalog()
        return when (val decoded = VisualAssetCatalogCodec.decode(file.readText())) {
            is VisualAssetCatalogDecodeResult.Decoded -> decoded.catalog
            is VisualAssetCatalogDecodeResult.Invalid,
            is VisualAssetCatalogDecodeResult.UnsupportedVersion,
            -> VisualAssetCatalog()
        }
    }

    fun save(file: File, catalog: VisualAssetCatalog) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        temporary.writeText(VisualAssetCatalogCodec.encode(catalog))
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            temporary.delete()
        }
        mutableRevision.value += 1L
    }
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull(::optJSONObject)

private fun JSONArray?.strings(): Set<String> =
    if (this == null) emptySet() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }.toSet()
