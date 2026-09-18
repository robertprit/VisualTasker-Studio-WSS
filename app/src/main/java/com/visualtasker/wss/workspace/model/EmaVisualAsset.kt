package com.visualtasker.wss.workspace.model

import java.io.File
import java.net.URI
import org.json.JSONArray
import org.json.JSONObject

const val EMA_FORMAT = "emscript_motion_asset"
const val EMA_SCHEMA_VERSION = 1
const val EMA_MIME_TYPE = "application/vnd.emscript.motion+json"

enum class EmaVisualAssetType {
    SHAPE,
    ANIMATED_SHAPE,
    BLOCK_SHAPE,
    NODE_SHAPE,
    PORT_SHAPE,
    ICON,
    VISUAL_COMPONENT,
    OVERLAY_SHAPE,
    ACTOR,
}

enum class EmaVisualAssetBindingTarget {
    Block,
    FlowNode,
    Port,
    Icon,
    Overlay,
}

data class EmaVisualAssetDescriptor(
    val assetId: String,
    val name: String,
    val type: EmaVisualAssetType,
    val version: Int,
    val tags: Set<String>,
    val stateCount: Int,
    val transitionCount: Int,
    val animationCount: Int,
    val sequenceCount: Int,
    val portCount: Int,
    val anchorCount: Int,
    val contentAreaCount: Int,
    val metadata: Map<String, String>,
) {
    val bindingTargets: Set<EmaVisualAssetBindingTarget>
        get() = when (type) {
            EmaVisualAssetType.BLOCK_SHAPE -> setOf(EmaVisualAssetBindingTarget.Block)
            EmaVisualAssetType.NODE_SHAPE -> setOf(EmaVisualAssetBindingTarget.FlowNode)
            EmaVisualAssetType.PORT_SHAPE -> setOf(EmaVisualAssetBindingTarget.Port)
            EmaVisualAssetType.ICON -> setOf(EmaVisualAssetBindingTarget.Icon)
            EmaVisualAssetType.VISUAL_COMPONENT,
            EmaVisualAssetType.OVERLAY_SHAPE,
            EmaVisualAssetType.ACTOR,
            -> setOf(EmaVisualAssetBindingTarget.Overlay)
            EmaVisualAssetType.SHAPE,
            EmaVisualAssetType.ANIMATED_SHAPE,
            -> setOf(
                EmaVisualAssetBindingTarget.Block,
                EmaVisualAssetBindingTarget.FlowNode,
                EmaVisualAssetBindingTarget.Overlay,
            )
        }
}

sealed interface EmaDescriptorDecodeResult {
    data class Decoded(val descriptor: EmaVisualAssetDescriptor) : EmaDescriptorDecodeResult
    data class UnsupportedVersion(val version: Int) : EmaDescriptorDecodeResult
    data class Invalid(val message: String) : EmaDescriptorDecodeResult
}

data class StoredEmaVisualAsset(
    val descriptor: EmaVisualAssetDescriptor,
    val file: File,
)

sealed interface EmaVisualAssetImportResult {
    data class Imported(val asset: StoredEmaVisualAsset) : EmaVisualAssetImportResult
    data class Rejected(val message: String) : EmaVisualAssetImportResult
}

enum class EmaVisualAssetIssueSeverity {
    WARNING,
    ERROR,
}

data class EmaVisualAssetIssue(
    val resourceId: String,
    val code: String,
    val message: String,
    val severity: EmaVisualAssetIssueSeverity,
)

object EmaVisualAssetIntegrity {
    fun inspect(resources: WorkspaceResourceBundle): List<EmaVisualAssetIssue> =
        resources.byKind(WorkspaceResourceKind.VisualAsset).flatMap(::inspect)

    fun inspect(resource: WorkspaceResource): List<EmaVisualAssetIssue> {
        if (resource.kind != WorkspaceResourceKind.VisualAsset) return emptyList()
        val issues = mutableListOf<EmaVisualAssetIssue>()
        if (resource.mimeType != EMA_MIME_TYPE) {
            issues += resource.issue(
                code = "EMA_MIME_UNSUPPORTED",
                message = "Erwartet $EMA_MIME_TYPE, gefunden ${resource.mimeType ?: "kein MIME-Typ"}.",
            )
        }
        val file = resource.uri?.trim()?.takeIf(String::isNotEmpty)?.toResourceFile()
        if (file == null) {
            issues += resource.issue("EMA_URI_MISSING", "Visual Asset besitzt keinen gueltigen Dateipfad.")
            return issues
        }
        if (!file.isFile) {
            issues += resource.issue("EMA_FILE_MISSING", "Visual-Asset-Datei fehlt: ${file.absolutePath}")
            return issues
        }
        val decoded = runCatching { EmaVisualAssetCodec.decodeDescriptor(file.readText()) }
            .getOrElse {
                issues += resource.issue("EMA_FILE_UNREADABLE", it.message ?: "Visual-Asset-Datei ist nicht lesbar.")
                return issues
            }
        when (decoded) {
            is EmaDescriptorDecodeResult.UnsupportedVersion -> issues += resource.issue(
                "EMA_SCHEMA_UNSUPPORTED",
                "Nicht unterstuetzte EMA-Schemaversion ${decoded.version}.",
            )
            is EmaDescriptorDecodeResult.Invalid -> issues += resource.issue("EMA_INVALID", decoded.message)
            is EmaDescriptorDecodeResult.Decoded -> {
                val descriptor = decoded.descriptor
                resource.metadata["assetId"]?.takeIf { it != descriptor.assetId }?.let { storedId ->
                    issues += resource.issue(
                        "EMA_ASSET_ID_MISMATCH",
                        "Ressource verweist auf $storedId, Datei enthaelt ${descriptor.assetId}.",
                        EmaVisualAssetIssueSeverity.WARNING,
                    )
                }
                resource.metadata["assetVersion"]?.toIntOrNull()
                    ?.takeIf { it != descriptor.version }
                    ?.let { storedVersion ->
                        issues += resource.issue(
                            "EMA_VERSION_MISMATCH",
                            "Ressource meldet Version $storedVersion, Datei enthaelt ${descriptor.version}.",
                            EmaVisualAssetIssueSeverity.WARNING,
                        )
                    }
            }
        }
        return issues
    }
}

object EmaVisualAssetCodec {
    fun decodeDescriptor(raw: String): EmaDescriptorDecodeResult {
        val root = runCatching { JSONObject(raw) }
            .getOrElse { return EmaDescriptorDecodeResult.Invalid(it.message ?: "Invalid EMA JSON") }
        if (root.optString("format") != EMA_FORMAT) {
            return EmaDescriptorDecodeResult.Invalid("Unsupported EMA format: ${root.optString("format")}")
        }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != EMA_SCHEMA_VERSION) {
            return EmaDescriptorDecodeResult.UnsupportedVersion(schemaVersion)
        }
        return runCatching {
            val asset = root.getJSONObject("asset")
            require(asset.has("design") && asset.optJSONObject("design") != null) {
                "EMA asset requires a canonical design document."
            }
            val assetId = asset.getString("assetId").trim()
            val name = asset.getString("name").trim()
            val version = asset.optInt("version", 1)
            require(assetId.isNotBlank()) { "EMA assetId must not be blank." }
            require(name.isNotBlank()) { "EMA name must not be blank." }
            require(version >= 1) { "EMA asset version must be at least 1." }
            val design = asset.getJSONObject("design")
            EmaDescriptorDecodeResult.Decoded(
                EmaVisualAssetDescriptor(
                    assetId = assetId,
                    name = name,
                    type = enumValueOrDefault(asset.optString("type"), EmaVisualAssetType.ANIMATED_SHAPE),
                    version = version,
                    tags = asset.optJSONArray("tags").stringSet(),
                    stateCount = asset.optJSONArray("states")?.length() ?: 0,
                    transitionCount = asset.optJSONArray("transitions")?.length() ?: 0,
                    animationCount = asset.optJSONArray("animations")?.length() ?: 0,
                    sequenceCount = asset.optJSONArray("sequences")?.length() ?: 0,
                    portCount = design.countArrayItemsNamed("ports"),
                    anchorCount = design.countArrayItemsNamed("anchors"),
                    contentAreaCount = design.countArrayItemsNamed("contentAreas"),
                    metadata = asset.optJSONObject("metadata").stringMap(),
                )
            )
        }.getOrElse { EmaDescriptorDecodeResult.Invalid(it.message ?: "Invalid EMA asset") }
    }
}

object EmaVisualAssetStore {
    fun importRaw(directory: File, raw: String): EmaVisualAssetImportResult {
        val decoded = EmaVisualAssetCodec.decodeDescriptor(raw)
        if (decoded !is EmaDescriptorDecodeResult.Decoded) {
            val message = when (decoded) {
                is EmaDescriptorDecodeResult.UnsupportedVersion -> "Unsupported EMA schema version ${decoded.version}."
                is EmaDescriptorDecodeResult.Invalid -> decoded.message
                is EmaDescriptorDecodeResult.Decoded -> error("unreachable")
            }
            return EmaVisualAssetImportResult.Rejected(message)
        }
        directory.mkdirs()
        val descriptor = decoded.descriptor
        val file = directory.resolve("${descriptor.assetId.toStableAssetFileName()}-v${descriptor.version}.ema")
        val temporary = directory.resolve(".${file.name}.tmp")
        return runCatching {
            temporary.writeText(raw)
            if (!temporary.renameTo(file)) {
                file.writeText(temporary.readText())
                temporary.delete()
            }
            EmaVisualAssetImportResult.Imported(StoredEmaVisualAsset(descriptor, file))
        }.getOrElse { EmaVisualAssetImportResult.Rejected(it.message ?: "EMA import failed") }
    }

    fun list(directory: File): List<StoredEmaVisualAsset> =
        directory.listFiles { file -> file.isFile && file.extension.equals("ema", ignoreCase = true) }
            .orEmpty()
            .mapNotNull { file ->
                val descriptor = runCatching { EmaVisualAssetCodec.decodeDescriptor(file.readText()) }.getOrNull()
                (descriptor as? EmaDescriptorDecodeResult.Decoded)?.descriptor?.let { StoredEmaVisualAsset(it, file) }
            }
            .sortedWith(compareBy<StoredEmaVisualAsset> { it.descriptor.type.name }.thenBy { it.descriptor.name })

    fun remove(directory: File, asset: StoredEmaVisualAsset): Boolean = runCatching {
        val root = directory.canonicalFile
        val target = asset.file.canonicalFile
        require(target.parentFile == root) { "EMA asset is outside the managed catalog." }
        !target.exists() || target.delete()
    }.getOrDefault(false)
}

fun StoredEmaVisualAsset.toWorkspaceResource(): WorkspaceResource = WorkspaceResource(
    id = "visual-asset:${descriptor.assetId.toStableAssetFileName()}",
    kind = WorkspaceResourceKind.VisualAsset,
    label = descriptor.name,
    pluginOwner = "com.m3shapes.editor",
    uri = file.absolutePath,
    mimeType = EMA_MIME_TYPE,
    tags = descriptor.tags + setOf("visual-asset", descriptor.type.name.lowercase()),
    metadata = descriptor.metadata + mapOf(
        "assetId" to descriptor.assetId,
        "assetType" to descriptor.type.name,
        "assetVersion" to descriptor.version.toString(),
        "stateCount" to descriptor.stateCount.toString(),
        "transitionCount" to descriptor.transitionCount.toString(),
        "animationCount" to descriptor.animationCount.toString(),
        "sequenceCount" to descriptor.sequenceCount.toString(),
        "portCount" to descriptor.portCount.toString(),
        "anchorCount" to descriptor.anchorCount.toString(),
        "contentAreaCount" to descriptor.contentAreaCount.toString(),
        "bindingTargets" to descriptor.bindingTargets.joinToString(",") { it.name },
        "format" to EMA_FORMAT,
    ),
    createdAtEpochMs = file.lastModified(),
    updatedAtEpochMs = file.lastModified(),
)

private fun String.toStableAssetFileName(): String =
    lowercase()
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-', '.', '_')
        .ifBlank { "asset" }

private fun String.toResourceFile(): File? = runCatching {
    if (startsWith("file:", ignoreCase = true)) File(URI(this)) else File(this)
}.getOrNull()

private fun WorkspaceResource.issue(
    code: String,
    message: String,
    severity: EmaVisualAssetIssueSeverity = EmaVisualAssetIssueSeverity.ERROR,
): EmaVisualAssetIssue = EmaVisualAssetIssue(
    resourceId = id,
    code = code,
    message = message,
    severity = severity,
)

private fun JSONArray?.stringSet(): Set<String> = buildSet {
    val array = this@stringSet ?: return@buildSet
    for (index in 0 until array.length()) {
        array.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
    }
}

private fun JSONObject?.stringMap(): Map<String, String> = buildMap {
    val value = this@stringMap ?: return@buildMap
    value.keys().forEach { key -> put(key, value.optString(key, "")) }
}

private fun JSONObject.countArrayItemsNamed(name: String): Int {
    var count = 0
    keys().forEach { key ->
        when (val value = opt(key)) {
            is JSONObject -> count += value.countArrayItemsNamed(name)
            is JSONArray -> {
                if (key == name) count += value.length()
                for (index in 0 until value.length()) {
                    when (val item = value.opt(index)) {
                        is JSONObject -> count += item.countArrayItemsNamed(name)
                        is JSONArray -> count += item.countArrayItemsNamed(name)
                    }
                }
            }
        }
    }
    return count
}

private fun JSONArray.countArrayItemsNamed(name: String): Int {
    var count = 0
    for (index in 0 until length()) {
        when (val item = opt(index)) {
            is JSONObject -> count += item.countArrayItemsNamed(name)
            is JSONArray -> count += item.countArrayItemsNamed(name)
        }
    }
    return count
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: fallback
