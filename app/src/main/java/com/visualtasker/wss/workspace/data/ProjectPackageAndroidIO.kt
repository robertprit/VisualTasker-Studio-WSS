package com.visualtasker.wss.workspace.data

import android.content.Context
import android.net.Uri
import com.visualtasker.wss.workspace.model.ProjectAssetResolver
import com.visualtasker.wss.workspace.model.ProjectPackageCancellation
import com.visualtasker.wss.workspace.model.ProjectPackageCodec
import com.visualtasker.wss.workspace.model.ProjectPackageCommitResult
import com.visualtasker.wss.workspace.model.ProjectPackageConflictMode
import com.visualtasker.wss.workspace.model.ProjectPackageDiagnostic
import com.visualtasker.wss.workspace.model.ProjectPackageDiagnosticCode
import com.visualtasker.wss.workspace.model.ProjectPackageExportResult
import com.visualtasker.wss.workspace.model.ProjectPackageFileRepository
import com.visualtasker.wss.workspace.model.ProjectPackageImportRequest
import com.visualtasker.wss.workspace.model.ProjectPackageLimits
import com.visualtasker.wss.workspace.model.ProjectPackagePreflightResult
import com.visualtasker.wss.workspace.model.ProjectResourceBridge
import com.visualtasker.wss.workspace.model.ResolvedProjectAsset
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectResource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed interface ProjectPackageIoResult {
    data class Exported(val fingerprint: String, val byteCount: Long) : ProjectPackageIoResult
    data class Imported(val project: ProjectDocument, val importedAsCopy: Boolean) : ProjectPackageIoResult
    data class Failed(val diagnostics: List<ProjectPackageDiagnostic>) : ProjectPackageIoResult
    data object Cancelled : ProjectPackageIoResult
}

object ProjectPackageAndroidIO {
    fun exportProject(
        context: Context,
        uri: Uri,
        project: ProjectDocument,
        limits: ProjectPackageLimits = ProjectPackageLimits(),
        cancellation: ProjectPackageCancellation = ProjectPackageCancellation.None,
    ): ProjectPackageIoResult {
        val exported = ProjectPackageCodec.export(
            project = project,
            assetResolver = AndroidProjectAssetResolver(context, limits),
            limits = limits,
            cancellation = cancellation,
        )
        val success = when (exported) {
            is ProjectPackageExportResult.Success -> exported
            is ProjectPackageExportResult.Failure -> return ProjectPackageIoResult.Failed(exported.diagnostics)
            ProjectPackageExportResult.Cancelled -> return ProjectPackageIoResult.Cancelled
        }
        if (cancellation.isCancelled()) return ProjectPackageIoResult.Cancelled

        val cacheDirectory = File(context.cacheDir, "project-package-export").apply { mkdirs() }
        val temporary = File(cacheDirectory, ".${UUID.randomUUID()}.vtpkg.tmp")
        return try {
            FileOutputStream(temporary).use { output ->
                output.write(success.bytes)
                output.fd.sync()
            }
            val verified = ProjectPackageCodec.preflight(
                bytes = temporary.readBytes(),
                limits = limits,
                cancellation = cancellation,
            )
            if (verified !is ProjectPackagePreflightResult.Ready) {
                val diagnostics = (verified as? ProjectPackagePreflightResult.Rejected)?.diagnostics
                    ?: listOf(ProjectPackageDiagnostic(ProjectPackageDiagnosticCode.Cancelled, "Package export was cancelled."))
                return if (verified == ProjectPackagePreflightResult.Cancelled) ProjectPackageIoResult.Cancelled
                else ProjectPackageIoResult.Failed(diagnostics)
            }
            if (cancellation.isCancelled()) return ProjectPackageIoResult.Cancelled
            val output = context.contentResolver.openOutputStream(uri, "wt")
                ?: return failure(ProjectPackageDiagnosticCode.InvalidManifest, "Export destination cannot be opened.")
            output.use { temporary.inputStream().use { input -> input.copyTo(it) } }
            ProjectPackageIoResult.Exported(success.manifest.contentFingerprint, success.bytes.size.toLong())
        } catch (error: Throwable) {
            failure(ProjectPackageDiagnosticCode.InvalidManifest, error.message ?: "Project export failed.")
        } finally {
            temporary.delete()
        }
    }

    fun importProject(
        context: Context,
        uri: Uri,
        currentProject: ProjectDocument,
        conflictMode: ProjectPackageConflictMode,
        limits: ProjectPackageLimits = ProjectPackageLimits(),
        cancellation: ProjectPackageCancellation = ProjectPackageCancellation.None,
    ): ProjectPackageIoResult {
        val runtimeRoot = File(context.filesDir, "emscript-runtime").apply { mkdirs() }
        val repository = ProjectPackageFileRepository(runtimeRoot)
        if (!repository.ensureRegisteredSnapshot(currentProject)) {
            return failure(ProjectPackageDiagnosticCode.InvalidProject, "Current project could not be registered before import.")
        }
        val packageBytes = readBounded(context, uri, limits.maxTotalBytes, cancellation)
            ?: return if (cancellation.isCancelled()) ProjectPackageIoResult.Cancelled
            else failure(ProjectPackageDiagnosticCode.PackageTooLarge, "Project package cannot be read or exceeds the size limit.")
        val copyId = if (conflictMode == ProjectPackageConflictMode.ImportAsCopy) {
            ProjectId("project:import:${UUID.randomUUID()}")
        } else {
            null
        }
        val preflight = ProjectPackageCodec.preflight(
            bytes = packageBytes,
            request = ProjectPackageImportRequest(
                existingProjectIds = repository.registeredProjectIds() + currentProject.id,
                conflictMode = conflictMode,
                copyProjectId = copyId,
            ),
            limits = limits,
            cancellation = cancellation,
        )
        val plan = when (preflight) {
            is ProjectPackagePreflightResult.Ready -> preflight.plan
            is ProjectPackagePreflightResult.Rejected -> return ProjectPackageIoResult.Failed(preflight.diagnostics)
            ProjectPackagePreflightResult.Cancelled -> return ProjectPackageIoResult.Cancelled
        }
        val committed = repository.commit(plan, cancellation)
        val imported = when (committed) {
            is ProjectPackageCommitResult.Success -> committed.project
            is ProjectPackageCommitResult.Failure -> return ProjectPackageIoResult.Failed(committed.diagnostics)
            ProjectPackageCommitResult.Cancelled -> return ProjectPackageIoResult.Cancelled
        }
        val projectStore = ProjectWorkspaceStore(context.applicationContext)
        val activated = runCatching { projectStore.saveProjectAtomically(imported) }.getOrDefault(false)
        if (!activated) {
            repository.rollbackImportedProject(imported.id)
            return failure(ProjectPackageDiagnosticCode.InvalidProject, "Imported project could not be activated; import was rolled back.")
        }
        return ProjectPackageIoResult.Imported(imported, plan.importedAsCopy)
    }

    private fun readBounded(
        context: Context,
        uri: Uri,
        maxBytes: Long,
        cancellation: ProjectPackageCancellation,
    ): ByteArray? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                if (cancellation.isCancelled()) return null
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) return null
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }.getOrNull()

    private fun failure(code: ProjectPackageDiagnosticCode, message: String) =
        ProjectPackageIoResult.Failed(listOf(ProjectPackageDiagnostic(code, message)))
}

private class AndroidProjectAssetResolver(
    private val context: Context,
    private val limits: ProjectPackageLimits,
) : ProjectAssetResolver {
    override fun resolve(resource: ProjectResource): ResolvedProjectAsset? {
        ProjectResourceBridge.resolveInlineAsset(resource)?.let { return it }
        val bytes = when {
            resource.location.startsWith("content:") -> Uri.parse(resource.location).readBytes()
            resource.location.startsWith("file:") -> Uri.parse(resource.location).path?.let(::File)?.readBytesBounded()
            File(resource.location).isAbsolute -> File(resource.location).readBytesBounded()
            else -> resolveRelative(resource.location)?.readBytesBounded()
        } ?: return null
        return ResolvedProjectAsset(bytes, resource.mimeType())
    }

    private fun resolveRelative(location: String): File? = listOfNotNull(
        File(context.filesDir, "emscript-runtime"),
        context.filesDir,
        context.getExternalFilesDir(null),
    ).asSequence()
        .mapNotNull { root ->
            File(root, location).takeIf { candidate -> candidate.isFile && candidate.isInside(root) }
        }
        .firstOrNull()

    private fun Uri.readBytes(): ByteArray? = context.contentResolver.openInputStream(this)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > limits.maxEntryBytes) return null
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }

    private fun File.readBytesBounded(): ByteArray? = takeIf { isFile && length() <= limits.maxEntryBytes }?.readBytes()

    private fun File.isInside(root: File): Boolean = runCatching {
        canonicalPath.startsWith(root.canonicalPath + File.separator)
    }.getOrDefault(false)

    private fun ProjectResource.mimeType(): String = ProjectResourceBridge.toWorkspaceResource(this)?.mimeType
        ?: when (location.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "json" -> "application/json"
            "ema" -> "application/vnd.visualtasker.asset+json"
            else -> "application/octet-stream"
        }
}
