package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.PROJECT_SCHEMA_VERSION
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectReferenceTarget
import de.visualtasker.workflow.core.ProjectResource
import de.visualtasker.workflow.core.ProjectResourceScope
import de.visualtasker.workflow.serialization.ProjectDecodeResult
import de.visualtasker.workflow.serialization.ProjectSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

const val PROJECT_PACKAGE_EXTENSION = "vtpkg"
const val PROJECT_PACKAGE_MIME_TYPE = "application/vnd.visualtasker.project+zip"
const val PROJECT_PACKAGE_FORMAT_VERSION = 1

private const val PACKAGE_MANIFEST_PATH = "manifest.json"
private const val PACKAGE_PROJECT_PATH = "project.json"

data class ProjectPackageLimits(
    val maxEntryCount: Int = 2_048,
    val maxEntryBytes: Long = 64L * 1024L * 1024L,
    val maxTotalBytes: Long = 256L * 1024L * 1024L,
    val maxManifestBytes: Long = 2L * 1024L * 1024L,
    val maxProjectBytes: Long = 32L * 1024L * 1024L,
    val maxCompressionRatio: Long = 250L,
)

fun interface ProjectPackageCancellation {
    fun isCancelled(): Boolean

    companion object {
        val None = ProjectPackageCancellation { false }
    }
}

data class ResolvedProjectAsset(
    val bytes: ByteArray,
    val mimeType: String = "application/octet-stream",
)

fun interface ProjectAssetResolver {
    fun resolve(resource: ProjectResource): ResolvedProjectAsset?
}

enum class ProjectPackageDependencyKind {
    Bundled,
    Embedded,
    AppShared,
    External,
    VolatileCapture,
}

data class ProjectPackageDocumentEntry(
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
)

data class ProjectPackageResourceEntry(
    val resourceId: String,
    val type: String,
    val scope: String,
    val dependencyKind: ProjectPackageDependencyKind,
    val dependencyReference: String,
    val packagePath: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val sha256: String? = null,
)

data class ProjectPackageManifest(
    val packageFormatVersion: Int,
    val projectSchemaVersion: Int,
    val projectId: String,
    val projectName: String,
    val rootIds: List<String>,
    val resourceIds: List<String>,
    val projectDocument: ProjectPackageDocumentEntry,
    val resources: List<ProjectPackageResourceEntry>,
    val providerDependencies: List<String> = emptyList(),
    val exportedAtEpochMs: Long,
    val contentFingerprint: String,
)

enum class ProjectPackageDiagnosticCode {
    Cancelled,
    InvalidProject,
    MissingAsset,
    AssetTooLarge,
    PackageTooLarge,
    TooManyEntries,
    InvalidManifest,
    UnsupportedPackageVersion,
    UnsupportedProjectSchema,
    InvalidEntryPath,
    DuplicateEntry,
    DuplicateId,
    HashMismatch,
    SizeMismatch,
    CompressionLimitExceeded,
    MissingEntry,
    ProjectIdConflict,
    CopyProjectIdConflict,
    InvalidReference,
}

data class ProjectPackageDiagnostic(
    val code: ProjectPackageDiagnosticCode,
    val message: String,
    val entry: String? = null,
    val resourceId: String? = null,
)

sealed interface ProjectPackageExportResult {
    data class Success(
        val bytes: ByteArray,
        val manifest: ProjectPackageManifest,
    ) : ProjectPackageExportResult

    data class Failure(val diagnostics: List<ProjectPackageDiagnostic>) : ProjectPackageExportResult
    data object Cancelled : ProjectPackageExportResult
}

enum class ProjectPackageConflictMode { Abort, ImportAsCopy }

data class ProjectPackageImportRequest(
    val existingProjectIds: Set<ProjectId> = emptySet(),
    val conflictMode: ProjectPackageConflictMode = ProjectPackageConflictMode.Abort,
    val copyProjectId: ProjectId? = null,
)

data class ProjectPackageImportPlan(
    val project: ProjectDocument,
    val sourceProjectId: ProjectId,
    val manifest: ProjectPackageManifest,
    val assetsByPackagePath: Map<String, ByteArray>,
    val importedAsCopy: Boolean,
)

sealed interface ProjectPackagePreflightResult {
    data class Ready(val plan: ProjectPackageImportPlan) : ProjectPackagePreflightResult
    data class Rejected(val diagnostics: List<ProjectPackageDiagnostic>) : ProjectPackagePreflightResult
    data object Cancelled : ProjectPackagePreflightResult
}

object ProjectPackageCodec {
    fun export(
        project: ProjectDocument,
        assetResolver: ProjectAssetResolver,
        exportedAtEpochMs: Long = System.currentTimeMillis(),
        limits: ProjectPackageLimits = ProjectPackageLimits(),
        cancellation: ProjectPackageCancellation = ProjectPackageCancellation.None,
    ): ProjectPackageExportResult {
        if (cancellation.isCancelled()) return ProjectPackageExportResult.Cancelled
        val assets = linkedMapOf<String, ByteArray>()
        val resourceEntries = mutableListOf<ProjectPackageResourceEntry>()
        project.resources.values.sortedBy { it.id.value }.forEachIndexed { index, resource ->
            if (cancellation.isCancelled()) return ProjectPackageExportResult.Cancelled
            val dependencyKind = dependencyKind(resource)
            if (dependencyKind !in setOf(ProjectPackageDependencyKind.Bundled, ProjectPackageDependencyKind.Embedded)) {
                if (dependencyKind in setOf(ProjectPackageDependencyKind.AppShared, ProjectPackageDependencyKind.External) &&
                    !resource.location.isPortableDependencyReference()
                ) {
                    return failure(
                        ProjectPackageDiagnosticCode.InvalidProject,
                        "Dependency ${resource.id.value} uses a device-local reference and cannot be exported portably.",
                        resourceId = resource.id.value,
                    )
                }
                resourceEntries += resource.toManifestEntry(dependencyKind)
                return@forEachIndexed
            }
            val resolved = assetResolver.resolve(resource)
                ?: return failure(
                    ProjectPackageDiagnosticCode.MissingAsset,
                    "Required resource ${resource.id.value} cannot be resolved.",
                    resourceId = resource.id.value,
                )
            if (resolved.bytes.size > limits.maxEntryBytes) {
                return failure(
                    ProjectPackageDiagnosticCode.AssetTooLarge,
                    "Resource ${resource.id.value} exceeds ${limits.maxEntryBytes} bytes.",
                    resourceId = resource.id.value,
                )
            }
            val packagePath = resourcePackagePath(index, resource, resolved.mimeType)
            assets[packagePath] = resolved.bytes
            resourceEntries += resource.toManifestEntry(
                dependencyKind = dependencyKind,
                packagePath = packagePath,
                mimeType = resolved.mimeType,
                sizeBytes = resolved.bytes.size.toLong(),
                sha256 = resolved.bytes.sha256(),
            )
        }
        val portableProject = project.portableForPackage(resourceEntries)
        val projectJson = runCatching { ProjectSerializer.serialize(portableProject) }.getOrElse { error ->
            return ProjectPackageExportResult.Failure(
                listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidProject, error.message ?: "Project serialization failed.")),
            )
        }
        val projectBytes = projectJson.toByteArray(Charsets.UTF_8)
        if (projectBytes.size > limits.maxProjectBytes) {
            return failure(ProjectPackageDiagnosticCode.PackageTooLarge, "Project document exceeds ${limits.maxProjectBytes} bytes.", PACKAGE_PROJECT_PATH)
        }
        val projectedTotal = projectBytes.size.toLong() + assets.values.sumOf { it.size.toLong() }
        if (projectedTotal > limits.maxTotalBytes) {
            return failure(ProjectPackageDiagnosticCode.PackageTooLarge, "Package content exceeds ${limits.maxTotalBytes} bytes.")
        }

        val projectEntry = ProjectPackageDocumentEntry(PACKAGE_PROJECT_PATH, projectBytes.size.toLong(), projectBytes.sha256())
        val fingerprint = contentFingerprint(project, resourceEntries)
        val manifest = ProjectPackageManifest(
            packageFormatVersion = PROJECT_PACKAGE_FORMAT_VERSION,
            projectSchemaVersion = project.schemaVersion,
            projectId = project.id.value,
            projectName = project.manifest.name,
            rootIds = project.roots.keys.map { it.value }.sorted(),
            resourceIds = project.resources.keys.map { it.value }.sorted(),
            projectDocument = projectEntry,
            resources = resourceEntries.sortedBy { it.resourceId },
            providerDependencies = providerDependencies(project),
            exportedAtEpochMs = exportedAtEpochMs,
            contentFingerprint = fingerprint,
        )
        val manifestBytes = encodeManifest(manifest).toByteArray(Charsets.UTF_8)
        if (manifestBytes.size > limits.maxManifestBytes) {
            return failure(ProjectPackageDiagnosticCode.PackageTooLarge, "Package manifest exceeds ${limits.maxManifestBytes} bytes.")
        }
        if (assets.size + 2 > limits.maxEntryCount) {
            return failure(ProjectPackageDiagnosticCode.TooManyEntries, "Package exceeds ${limits.maxEntryCount} entries.")
        }

        return runCatching {
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zip ->
                writeDeterministicEntry(zip, PACKAGE_MANIFEST_PATH, manifestBytes)
                writeDeterministicEntry(zip, PACKAGE_PROJECT_PATH, projectBytes)
                assets.toSortedMap().forEach { (path, bytes) ->
                    if (cancellation.isCancelled()) throw ProjectPackageCancelledException()
                    writeDeterministicEntry(zip, path, bytes)
                }
            }
            if (cancellation.isCancelled()) ProjectPackageExportResult.Cancelled
            else ProjectPackageExportResult.Success(output.toByteArray(), manifest)
        }.getOrElse { error ->
            if (error is ProjectPackageCancelledException) ProjectPackageExportResult.Cancelled
            else ProjectPackageExportResult.Failure(
                listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, error.message ?: "Package export failed.")),
            )
        }
    }

    fun preflight(
        bytes: ByteArray,
        request: ProjectPackageImportRequest = ProjectPackageImportRequest(),
        limits: ProjectPackageLimits = ProjectPackageLimits(),
        cancellation: ProjectPackageCancellation = ProjectPackageCancellation.None,
    ): ProjectPackagePreflightResult {
        if (cancellation.isCancelled()) return ProjectPackagePreflightResult.Cancelled
        if (bytes.size.toLong() > limits.maxTotalBytes) {
            return rejected(ProjectPackageDiagnosticCode.PackageTooLarge, "Compressed package exceeds ${limits.maxTotalBytes} bytes.")
        }
        val entries = when (val read = readEntries(bytes, limits, cancellation)) {
            is EntryReadResult.Success -> read.entries
            is EntryReadResult.Failure -> return ProjectPackagePreflightResult.Rejected(read.diagnostics)
            EntryReadResult.Cancelled -> return ProjectPackagePreflightResult.Cancelled
        }
        val manifestBytes = entries[PACKAGE_MANIFEST_PATH]
            ?: return rejected(ProjectPackageDiagnosticCode.MissingEntry, "Package manifest is missing.", PACKAGE_MANIFEST_PATH)
        val projectBytes = entries[PACKAGE_PROJECT_PATH]
            ?: return rejected(ProjectPackageDiagnosticCode.MissingEntry, "Project document is missing.", PACKAGE_PROJECT_PATH)
        if (manifestBytes.size > limits.maxManifestBytes) {
            return rejected(ProjectPackageDiagnosticCode.PackageTooLarge, "Manifest exceeds ${limits.maxManifestBytes} bytes.", PACKAGE_MANIFEST_PATH)
        }
        if (projectBytes.size > limits.maxProjectBytes) {
            return rejected(ProjectPackageDiagnosticCode.PackageTooLarge, "Project document exceeds ${limits.maxProjectBytes} bytes.", PACKAGE_PROJECT_PATH)
        }
        val manifest = runCatching { decodeManifest(manifestBytes.toString(Charsets.UTF_8)) }
            .getOrElse { error ->
                return rejected(ProjectPackageDiagnosticCode.InvalidManifest, error.message ?: "Manifest cannot be decoded.", PACKAGE_MANIFEST_PATH)
            }
        validateManifest(manifest, projectBytes, entries)?.let { return ProjectPackagePreflightResult.Rejected(it) }

        val decoded = ProjectSerializer.decode(projectBytes.toString(Charsets.UTF_8))
        val sourceProject = when (decoded) {
            is ProjectDecodeResult.Decoded -> decoded.project
            is ProjectDecodeResult.Malformed -> return rejected(ProjectPackageDiagnosticCode.InvalidProject, decoded.reason, PACKAGE_PROJECT_PATH)
            is ProjectDecodeResult.UnsupportedSchema -> return rejected(
                ProjectPackageDiagnosticCode.UnsupportedProjectSchema,
                decoded.diagnostics.joinToString("; ") { it.message },
                PACKAGE_PROJECT_PATH,
            )
        }
        validateProjectAgainstManifest(sourceProject, manifest)?.let { return ProjectPackagePreflightResult.Rejected(it) }

        val hasConflict = sourceProject.id in request.existingProjectIds
        val importedAsCopy = hasConflict && request.conflictMode == ProjectPackageConflictMode.ImportAsCopy
        if (hasConflict && !importedAsCopy) {
            return rejected(ProjectPackageDiagnosticCode.ProjectIdConflict, "Project ${sourceProject.id.value} already exists.")
        }
        val importedProject = if (importedAsCopy) {
            val targetId = request.copyProjectId ?: ProjectId("${sourceProject.id.value}:copy:${manifest.contentFingerprint.take(12)}")
            if (targetId in request.existingProjectIds) {
                return rejected(ProjectPackageDiagnosticCode.CopyProjectIdConflict, "Copy project ${targetId.value} already exists.")
            }
            sourceProject.copy(
                id = targetId,
                manifest = sourceProject.manifest.copy(name = "${sourceProject.manifest.name} (Copy)"),
                metadata = sourceProject.metadata + mapOf("import.sourceProjectId" to sourceProject.id.value),
            )
        } else {
            sourceProject
        }
        val assetPaths = manifest.resources.mapNotNull { it.packagePath }.toSet()
        return ProjectPackagePreflightResult.Ready(
            ProjectPackageImportPlan(
                project = importedProject,
                sourceProjectId = sourceProject.id,
                manifest = manifest,
                assetsByPackagePath = entries.filterKeys { it in assetPaths }.toSortedMap(),
                importedAsCopy = importedAsCopy,
            ),
        )
    }

    private fun validateManifest(
        manifest: ProjectPackageManifest,
        projectBytes: ByteArray,
        entries: Map<String, ByteArray>,
    ): List<ProjectPackageDiagnostic>? {
        val diagnostics = mutableListOf<ProjectPackageDiagnostic>()
        if (manifest.packageFormatVersion != PROJECT_PACKAGE_FORMAT_VERSION) {
            diagnostics += ProjectPackageDiagnostic(
                ProjectPackageDiagnosticCode.UnsupportedPackageVersion,
                "Unsupported package format ${manifest.packageFormatVersion}.",
            )
        }
        if (manifest.projectSchemaVersion != PROJECT_SCHEMA_VERSION) {
            diagnostics += ProjectPackageDiagnostic(
                ProjectPackageDiagnosticCode.UnsupportedProjectSchema,
                "Unsupported project schema ${manifest.projectSchemaVersion}.",
            )
        }
        if (manifest.projectDocument.path != PACKAGE_PROJECT_PATH) {
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Project document path must be $PACKAGE_PROJECT_PATH.")
        }
        if (manifest.projectDocument.sizeBytes != projectBytes.size.toLong()) {
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.SizeMismatch, "Project document size does not match manifest.", PACKAGE_PROJECT_PATH)
        }
        if (manifest.projectDocument.sha256 != projectBytes.sha256()) {
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.HashMismatch, "Project document hash does not match manifest.", PACKAGE_PROJECT_PATH)
        }
        duplicate(manifest.rootIds)?.let {
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.DuplicateId, "Duplicate root ID $it in manifest.")
        }
        duplicate(manifest.resourceIds)?.let {
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.DuplicateId, "Duplicate resource ID $it in manifest.")
        }
        duplicate(manifest.resources.map { it.resourceId })?.let {
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.DuplicateId, "Duplicate resource entry $it in manifest.")
        }
        duplicate(manifest.resources.mapNotNull { it.packagePath })?.let {
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.DuplicateEntry, "Duplicate package asset path $it.", it)
        }
        if (manifest.resources.map { it.resourceId }.sorted() != manifest.resourceIds.sorted()) {
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Resource entry IDs do not match the manifest inventory.")
        }
        manifest.resources.forEach { resource ->
            val path = resource.packagePath
            if (path != null && !path.isSafePackagePath()) {
                diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidEntryPath, "Unsafe asset path $path.", path, resource.resourceId)
                return@forEach
            }
            if (resource.dependencyKind in setOf(ProjectPackageDependencyKind.Bundled, ProjectPackageDependencyKind.Embedded)) {
                if (path == null || path !in entries) {
                    diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.MissingEntry, "Bundled resource ${resource.resourceId} is missing.", path, resource.resourceId)
                    return@forEach
                }
                val payload = entries.getValue(path)
                if (resource.sizeBytes != payload.size.toLong()) {
                    diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.SizeMismatch, "Resource ${resource.resourceId} size does not match.", path, resource.resourceId)
                }
                if (resource.sha256 != payload.sha256()) {
                    diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.HashMismatch, "Resource ${resource.resourceId} hash does not match.", path, resource.resourceId)
                }
            } else if (path != null) {
                diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Non-bundled resource ${resource.resourceId} must not contain a package path.", path, resource.resourceId)
            }
        }
        val allowedEntries = setOf(PACKAGE_MANIFEST_PATH, PACKAGE_PROJECT_PATH) +
            manifest.resources.mapNotNull { it.packagePath }
        (entries.keys - allowedEntries).sorted().forEach { path ->
            diagnostics += ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Unlisted package entry $path.", path)
        }
        return diagnostics.takeIf { it.isNotEmpty() }
    }

    private fun encodeManifest(manifest: ProjectPackageManifest): String = JSONObject()
        .put("packageFormatVersion", manifest.packageFormatVersion)
        .put("projectSchemaVersion", manifest.projectSchemaVersion)
        .put("projectId", manifest.projectId)
        .put("projectName", manifest.projectName)
        .put("rootIds", JSONArray(manifest.rootIds))
        .put("resourceIds", JSONArray(manifest.resourceIds))
        .put(
            "projectDocument",
            JSONObject()
                .put("path", manifest.projectDocument.path)
                .put("sizeBytes", manifest.projectDocument.sizeBytes)
                .put("sha256", manifest.projectDocument.sha256),
        )
        .put(
            "resources",
            JSONArray().apply {
                manifest.resources.forEach { resource ->
                    put(
                        JSONObject()
                            .put("resourceId", resource.resourceId)
                            .put("type", resource.type)
                            .put("scope", resource.scope)
                            .put("dependencyKind", resource.dependencyKind.name)
                            .put("dependencyReference", resource.dependencyReference)
                            .apply {
                                resource.packagePath?.let { put("packagePath", it) }
                                resource.mimeType?.let { put("mimeType", it) }
                                resource.sizeBytes?.let { put("sizeBytes", it) }
                                resource.sha256?.let { put("sha256", it) }
                            },
                    )
                }
            },
        )
        .put("providerDependencies", JSONArray(manifest.providerDependencies))
        .put("exportedAtEpochMs", manifest.exportedAtEpochMs)
        .put("contentFingerprint", manifest.contentFingerprint)
        .toString()

    private fun decodeManifest(raw: String): ProjectPackageManifest {
        val root = JSONObject(raw)
        val document = root.getJSONObject("projectDocument")
        val resources = root.getJSONArray("resources")
        return ProjectPackageManifest(
            packageFormatVersion = root.getInt("packageFormatVersion"),
            projectSchemaVersion = root.getInt("projectSchemaVersion"),
            projectId = root.getString("projectId"),
            projectName = root.getString("projectName"),
            rootIds = root.getJSONArray("rootIds").toStringList(),
            resourceIds = root.getJSONArray("resourceIds").toStringList(),
            projectDocument = ProjectPackageDocumentEntry(
                path = document.getString("path"),
                sizeBytes = document.getLong("sizeBytes"),
                sha256 = document.getString("sha256"),
            ),
            resources = buildList {
                for (index in 0 until resources.length()) {
                    val resource = resources.getJSONObject(index)
                    add(
                        ProjectPackageResourceEntry(
                            resourceId = resource.getString("resourceId"),
                            type = resource.getString("type"),
                            scope = resource.getString("scope"),
                            dependencyKind = ProjectPackageDependencyKind.valueOf(resource.getString("dependencyKind")),
                            dependencyReference = resource.getString("dependencyReference"),
                            packagePath = resource.optString("packagePath").ifBlank { null },
                            mimeType = resource.optString("mimeType").ifBlank { null },
                            sizeBytes = resource.optLong("sizeBytes").takeIf { resource.has("sizeBytes") },
                            sha256 = resource.optString("sha256").ifBlank { null },
                        ),
                    )
                }
            },
            providerDependencies = root.optJSONArray("providerDependencies")?.toStringList().orEmpty(),
            exportedAtEpochMs = root.getLong("exportedAtEpochMs"),
            contentFingerprint = root.getString("contentFingerprint"),
        )
    }

    private fun validateProjectAgainstManifest(
        project: ProjectDocument,
        manifest: ProjectPackageManifest,
    ): List<ProjectPackageDiagnostic>? {
        val diagnostics = buildList {
            if (project.id.value != manifest.projectId) add(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Project ID does not match manifest."))
            if (project.manifest.name != manifest.projectName) add(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Project name does not match manifest."))
            if (project.schemaVersion != manifest.projectSchemaVersion) add(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.UnsupportedProjectSchema, "Project schema does not match manifest."))
            if (project.roots.keys.map { it.value }.sorted() != manifest.rootIds.sorted()) add(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Root IDs do not match manifest."))
            if (project.resources.keys.map { it.value }.sorted() != manifest.resourceIds.sorted()) add(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Resource IDs do not match manifest."))
            if (providerDependencies(project) != manifest.providerDependencies.sorted()) {
                add(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Provider dependencies do not match project declarations."))
            }
            val manifestById = manifest.resources.associateBy { it.resourceId }
            project.resources.values.forEach { resource ->
                val manifestResource = manifestById[resource.id.value]
                if (manifestResource == null ||
                    manifestResource.type != resource.type.name ||
                    manifestResource.scope != resource.scope.name ||
                    manifestResource.dependencyKind != dependencyKind(resource)
                ) {
                    add(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, "Resource ${resource.id.value} contract does not match manifest.", resourceId = resource.id.value))
                }
            }
            if (contentFingerprint(project, manifest.resources) != manifest.contentFingerprint) {
                add(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.HashMismatch, "Package content fingerprint does not match manifest."))
            }
        }
        return diagnostics.takeIf { it.isNotEmpty() }
    }

    private fun readEntries(
        bytes: ByteArray,
        limits: ProjectPackageLimits,
        cancellation: ProjectPackageCancellation,
    ): EntryReadResult {
        val entries = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        return runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    if (cancellation.isCancelled()) return EntryReadResult.Cancelled
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (!name.isSafePackagePath()) {
                        return EntryReadResult.Failure(listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidEntryPath, "Unsafe package path $name.", name)))
                    }
                    if (entry.isDirectory) continue
                    if (name in entries) {
                        return EntryReadResult.Failure(listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.DuplicateEntry, "Duplicate package entry $name.", name)))
                    }
                    if (entries.size + 1 > limits.maxEntryCount) {
                        return EntryReadResult.Failure(listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.TooManyEntries, "Package exceeds ${limits.maxEntryCount} entries.")))
                    }
                    if (entry.size > limits.maxEntryBytes) {
                        return EntryReadResult.Failure(listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.AssetTooLarge, "Entry $name exceeds ${limits.maxEntryBytes} bytes.", name)))
                    }
                    if (entry.compressedSize > 0L && entry.size > 0L && entry.size / entry.compressedSize > limits.maxCompressionRatio) {
                        return EntryReadResult.Failure(listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.CompressionLimitExceeded, "Entry $name exceeds compression ratio limit.", name)))
                    }
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var entryBytes = 0L
                    while (true) {
                        if (cancellation.isCancelled()) return EntryReadResult.Cancelled
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        if (entryBytes > limits.maxEntryBytes || totalBytes > limits.maxTotalBytes) {
                            val code = if (entryBytes > limits.maxEntryBytes) ProjectPackageDiagnosticCode.AssetTooLarge else ProjectPackageDiagnosticCode.PackageTooLarge
                            return EntryReadResult.Failure(listOf(ProjectPackageDiagnostic(code, "Package size limit exceeded while reading $name.", name)))
                        }
                        output.write(buffer, 0, count)
                    }
                    entries[name] = output.toByteArray()
                }
            }
            EntryReadResult.Success(entries)
        }.getOrElse { error ->
            EntryReadResult.Failure(listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.InvalidManifest, error.message ?: "Package cannot be read.")))
        }
    }

    private fun writeDeterministicEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) {
        val entry = ZipEntry(path).apply { time = 0L }
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun ProjectResource.toManifestEntry(
        dependencyKind: ProjectPackageDependencyKind,
        packagePath: String? = null,
        mimeType: String? = null,
        sizeBytes: Long? = null,
        sha256: String? = null,
    ) = ProjectPackageResourceEntry(
        resourceId = id.value,
        type = type.name,
        scope = scope.name,
        dependencyKind = dependencyKind,
        dependencyReference = when (dependencyKind) {
            ProjectPackageDependencyKind.Bundled -> "package:${id.value}"
            ProjectPackageDependencyKind.Embedded -> "embedded:${id.value}"
            ProjectPackageDependencyKind.VolatileCapture -> "capture-transient:${id.value}"
            else -> location
        },
        packagePath = packagePath,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        sha256 = sha256,
    )

    private fun dependencyKind(resource: ProjectResource): ProjectPackageDependencyKind = when {
        resource.metadata["capture.transient"].equals("true", ignoreCase = true) -> ProjectPackageDependencyKind.VolatileCapture
        resource.scope == ProjectResourceScope.Embedded -> ProjectPackageDependencyKind.Embedded
        resource.scope == ProjectResourceScope.ProjectLocal -> ProjectPackageDependencyKind.Bundled
        resource.scope == ProjectResourceScope.AppShared -> ProjectPackageDependencyKind.AppShared
        else -> ProjectPackageDependencyKind.External
    }

    private fun providerDependencies(project: ProjectDocument): List<String> = buildSet {
        project.variables.values.mapNotNullTo(this) { variable ->
            variable.providerBinding?.trim()?.takeIf(String::isNotBlank)
        }
        project.references.values.mapNotNullTo(this) { reference ->
            (reference.target as? ProjectReferenceTarget.External)
                ?.providerReference
                ?.trim()
                ?.takeIf(String::isNotBlank)
        }
        project.resources.values
            .filter { it.scope == ProjectResourceScope.External }
            .mapTo(this) { it.location }
    }.sorted()

    private fun resourcePackagePath(index: Int, resource: ProjectResource, mimeType: String): String {
        val stem = resource.id.value.lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-').ifBlank { "resource" }
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "application/json", "application/vnd.visualtasker.workspace-resource+json" -> "json"
            else -> "bin"
        }
        return "assets/${index.toString().padStart(4, '0')}-$stem.$extension"
    }

    private fun contentFingerprint(
        project: ProjectDocument,
        resources: List<ProjectPackageResourceEntry>,
    ): String = buildString {
        val resourceEntries = resources.associateBy { it.resourceId }
        val portableProject = project.copy(
            resources = project.resources.mapValues { (_, resource) ->
                when (resourceEntries[resource.id.value]?.dependencyKind) {
                    ProjectPackageDependencyKind.Bundled,
                    ProjectPackageDependencyKind.Embedded,
                    ProjectPackageDependencyKind.VolatileCapture,
                    -> resource.copy(
                        location = resourceEntries.getValue(resource.id.value).dependencyReference,
                        contentHash = null,
                        metadata = resource.metadata - "package.dependencyReference" - "package.originalLocation",
                    )
                    else -> resource
                }
            },
        )
        append(ProjectSerializer.serialize(portableProject).toByteArray(Charsets.UTF_8).sha256()).append('\n')
        resources.sortedBy { it.resourceId }.forEach { resource ->
            append(resource.resourceId).append('|')
            append(resource.type).append('|')
            append(resource.scope).append('|')
            append(resource.dependencyKind.name).append('|')
            append(resource.packagePath.orEmpty()).append('|')
            append(resource.mimeType.orEmpty()).append('|')
            append(resource.sizeBytes ?: -1L).append('|')
            append(resource.sha256.orEmpty()).append('\n')
        }
    }.toByteArray(Charsets.UTF_8).sha256()

    private fun ProjectDocument.portableForPackage(
        resources: List<ProjectPackageResourceEntry>,
    ): ProjectDocument {
        val entriesById = resources.associateBy { it.resourceId }
        return copy(
            resources = this.resources.mapValues { (_, resource) ->
                when (val entry = entriesById[resource.id.value]) {
                    null -> resource
                    else -> when (entry.dependencyKind) {
                        ProjectPackageDependencyKind.Bundled -> resource.copy(location = entry.dependencyReference)
                        ProjectPackageDependencyKind.Embedded,
                        ProjectPackageDependencyKind.VolatileCapture,
                        -> resource.copy(location = entry.dependencyReference)
                        else -> resource
                    }
                }
            },
        )
    }

    private fun String.isPortableDependencyReference(): Boolean =
        isNotBlank() &&
            !startsWith('/') &&
            !startsWith('\\') &&
            !startsWith("file:", ignoreCase = true) &&
            !startsWith("content:", ignoreCase = true) &&
            !Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(this)

    private fun String.isSafePackagePath(): Boolean {
        if (isBlank() || startsWith('/') || startsWith('\\') || contains('\\') || Regex("^[A-Za-z]:").containsMatchIn(this)) return false
        val segments = split('/')
        return segments.none { it.isBlank() || it == "." || it == ".." }
    }

    private fun duplicate(values: List<String>): String? = values.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.key

    private fun failure(
        code: ProjectPackageDiagnosticCode,
        message: String,
        entry: String? = null,
        resourceId: String? = null,
    ) = ProjectPackageExportResult.Failure(listOf(ProjectPackageDiagnostic(code, message, entry, resourceId)))

    private fun rejected(
        code: ProjectPackageDiagnosticCode,
        message: String,
        entry: String? = null,
    ) = ProjectPackagePreflightResult.Rejected(listOf(ProjectPackageDiagnostic(code, message, entry)))

    private sealed interface EntryReadResult {
        data class Success(val entries: Map<String, ByteArray>) : EntryReadResult
        data class Failure(val diagnostics: List<ProjectPackageDiagnostic>) : EntryReadResult
        data object Cancelled : EntryReadResult
    }
}

private fun JSONArray.toStringList(): List<String> = buildList {
    for (index in 0 until length()) add(getString(index))
}

private class ProjectPackageCancelledException : RuntimeException()

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }
