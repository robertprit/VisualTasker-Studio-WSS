package com.visualtasker.wss.workspace.model

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

sealed interface WorkspaceResourceDecodeResult {
    data class Decoded(
        val bundle: WorkspaceResourceBundle,
        val migratedFromVersion: Int? = null,
    ) : WorkspaceResourceDecodeResult

    data class UnsupportedVersion(val version: Int) : WorkspaceResourceDecodeResult
    data class Invalid(val message: String) : WorkspaceResourceDecodeResult
}

object WorkspaceResourceCodec {
    const val FORMAT = "visualtasker.workspace-resources"

    fun encode(bundle: WorkspaceResourceBundle): String = JSONObject()
        .put("format", FORMAT)
        .put("schemaVersion", WORKSPACE_RESOURCE_SCHEMA_VERSION)
        .put("revision", bundle.revision)
        .put("resources", JSONArray().apply { bundle.resources.forEach { put(it.toJson()) } })
        .toString(2)

    fun decode(raw: String): WorkspaceResourceDecodeResult {
        val root = runCatching { JSONObject(raw) }
            .getOrElse { return WorkspaceResourceDecodeResult.Invalid(it.message ?: "Invalid JSON") }
        val version = root.optInt("schemaVersion", 0)
        if (version > WORKSPACE_RESOURCE_SCHEMA_VERSION || version < 0) {
            return WorkspaceResourceDecodeResult.UnsupportedVersion(version)
        }
        return runCatching {
            val array = root.optJSONArray("resources") ?: JSONArray()
            val resources = buildList {
                for (index in 0 until array.length()) add(array.getJSONObject(index).toResource(version))
            }
            WorkspaceResourceDecodeResult.Decoded(
                bundle = WorkspaceResourceBundle(
                    revision = root.optLong("revision", 0L),
                    resources = resources,
                ),
                migratedFromVersion = version.takeIf { it < WORKSPACE_RESOURCE_SCHEMA_VERSION },
            )
        }.getOrElse { WorkspaceResourceDecodeResult.Invalid(it.message ?: "Invalid resource bundle") }
    }
}

object WorkspaceResourceFileStore {
    fun save(file: File, bundle: WorkspaceResourceBundle) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        temporary.writeText(WorkspaceResourceCodec.encode(bundle))
        if (!temporary.renameTo(file)) {
            file.writeText(temporary.readText())
            temporary.delete()
        }
    }

    fun load(file: File): WorkspaceResourceDecodeResult =
        if (!file.isFile) {
            WorkspaceResourceDecodeResult.Invalid("Resource file does not exist: ${file.absolutePath}")
        } else {
            WorkspaceResourceCodec.decode(file.readText())
        }
}

private fun WorkspaceResource.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("kind", kind.name)
    .put("label", label)
    .put("resourceVersion", resourceVersion)
    .put("pluginOwner", pluginOwner)
    .putNullable("uri", uri)
    .putNullable("mimeType", mimeType)
    .putNullable("packageName", packageName)
    .putNullable("activityClass", activityClass)
    .putNullable("markerMode", markerMode?.name)
    .putNullable("region", region?.let { JSONObject().put("left", it.left).put("top", it.top).put("right", it.right).put("bottom", it.bottom) })
    .putNullable("point", point?.let { JSONObject().put("x", it.x).put("y", it.y) })
    .putNullable("referenceWidthPx", referenceWidthPx)
    .putNullable("referenceHeightPx", referenceHeightPx)
    .put("hidden", hidden)
    .put("locked", locked)
    .put("tags", JSONArray().apply { tags.sorted().forEach(::put) })
    .put("metadata", JSONObject(metadata))
    .put("createdAtEpochMs", createdAtEpochMs)
    .put("updatedAtEpochMs", updatedAtEpochMs)

private fun JSONObject.toResource(version: Int): WorkspaceResource {
    val tags = when (val rawTags = opt("tags")) {
        is JSONArray -> buildSet { for (index in 0 until rawTags.length()) add(rawTags.getString(index)) }
        is String -> rawTags.split(',').map(String::trim).filter(String::isNotBlank).toSet()
        else -> emptySet()
    }
    val metadataObject = optJSONObject("metadata") ?: JSONObject()
    val metadata = buildMap {
        metadataObject.keys().forEach { key -> put(key, metadataObject.optString(key, "")) }
    }
    val regionObject = optJSONObject("region")
    val pointObject = optJSONObject("point")
    return WorkspaceResource(
        id = getString("id"),
        kind = enumOrDefault(optString("kind"), WorkspaceResourceKind.Unknown),
        label = getString("label").trim(),
        resourceVersion = optInt("resourceVersion", WORKSPACE_RESOURCE_ITEM_VERSION).coerceAtLeast(1),
        pluginOwner = optString("pluginOwner", if (version == 0) "visualtasker.legacy.import" else "visualtasker.core").trim(),
        uri = nullableString("uri"),
        mimeType = nullableString("mimeType"),
        packageName = nullableString("packageName"),
        activityClass = nullableString("activityClass"),
        markerMode = nullableString("markerMode")?.let { enumOrNull<WorkspaceMarkerMode>(it) },
        region = regionObject?.let {
            WorkspaceRegionBounds(
                left = it.getDouble("left").toFloat(),
                top = it.getDouble("top").toFloat(),
                right = it.getDouble("right").toFloat(),
                bottom = it.getDouble("bottom").toFloat(),
            )
        },
        point = pointObject?.let { WorkspacePointBounds(it.getDouble("x").toFloat(), it.getDouble("y").toFloat()) },
        referenceWidthPx = nullableInt("referenceWidthPx"),
        referenceHeightPx = nullableInt("referenceHeightPx"),
        hidden = optBoolean("hidden", false),
        locked = optBoolean("locked", false),
        tags = tags,
        metadata = metadata,
        createdAtEpochMs = optLong("createdAtEpochMs", 0L),
        updatedAtEpochMs = optLong("updatedAtEpochMs", optLong("createdAtEpochMs", 0L)),
    )
}

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
    put(key, value ?: JSONObject.NULL)

private fun JSONObject.nullableString(key: String): String? =
    takeUnless { isNull(key) }?.optString(key)?.takeIf { it.isNotBlank() }

private fun JSONObject.nullableInt(key: String): Int? =
    takeUnless { isNull(key) }?.optInt(key)?.takeIf { it > 0 }

private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }

private inline fun <reified T : Enum<T>> enumOrDefault(value: String, fallback: T): T =
    enumOrNull<T>(value) ?: fallback
