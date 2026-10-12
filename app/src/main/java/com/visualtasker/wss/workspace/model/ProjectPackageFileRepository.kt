package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectResourceScope
import de.visualtasker.workflow.serialization.ProjectDecodeResult
import de.visualtasker.workflow.serialization.ProjectSerializer
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

sealed interface ProjectPackageCommitResult {
    data class Success(
        val project: ProjectDocument,
        val projectDirectory: File,
    ) : ProjectPackageCommitResult

    data class Failure(val diagnostics: List<ProjectPackageDiagnostic>) : ProjectPackageCommitResult
    data object Cancelled : ProjectPackageCommitResult
}

/** Owns only imported package directories below [projectsRoot]. Existing app assets are never deleted. */
class ProjectPackageFileRepository(
    private val runtimeRoot: File,
    private val projectsRoot: File = File(runtimeRoot, "projects"),
) {
    fun registeredProjectIds(): Set<ProjectId> = projectsRoot.listFiles()
        .orEmpty()
        .asSequence()
        .filter { it.isDirectory && !it.name.startsWith(".staging-") }
        .mapNotNull { directory ->
            val raw = File(directory, PROJECT_SNAPSHOT_NAME).takeIf(File::isFile)?.readText() ?: return@mapNotNull null
            (ProjectSerializer.decode(raw) as? ProjectDecodeResult.Decoded)?.project?.id
        }
        .toSet()

    fun ensureRegisteredSnapshot(project: ProjectDocument): Boolean {
        projectsRoot.mkdirs()
        val targetDirectory = projectDirectory(project.id)
        val target = File(targetDirectory, PROJECT_SNAPSHOT_NAME)
        if (target.isFile) return true
        if (targetDirectory.exists()) return false
        val staging = File(projectsRoot, ".staging-snapshot-${UUID.randomUUID()}")
        return try {
            staging.mkdirs()
            writeVerified(File(staging, PROJECT_SNAPSHOT_NAME), ProjectSerializer.serialize(project).toByteArray(Charsets.UTF_8))
            staging.renameTo(targetDirectory)
        } finally {
            staging.deleteRecursively()
        }
    }

    fun commit(
        plan: ProjectPackageImportPlan,
        cancellation: ProjectPackageCancellation = ProjectPackageCancellation.None,
    ): ProjectPackageCommitResult {
        if (cancellation.isCancelled()) return ProjectPackageCommitResult.Cancelled
        projectsRoot.mkdirs()
        val directoryName = projectDirectoryName(plan.project.id)
        val finalDirectory = File(projectsRoot, directoryName)
        if (finalDirectory.exists()) {
            return failure(ProjectPackageDiagnosticCode.ProjectIdConflict, "Project ${plan.project.id.value} is already registered.")
        }
        val staging = File(projectsRoot, ".staging-${UUID.randomUUID()}")
        return try {
            check(staging.mkdirs()) { "Could not create package staging directory." }
            val entriesById = plan.manifest.resources.associateBy { it.resourceId }
            val relocatedResources = plan.project.resources.mapValues { (_, resource) ->
                if (cancellation.isCancelled()) throw ProjectPackageFileCancelledException()
                val manifestEntry = entriesById[resource.id.value] ?: return@mapValues resource
                if (manifestEntry.dependencyKind !in setOf(ProjectPackageDependencyKind.Bundled, ProjectPackageDependencyKind.Embedded)) {
                    return@mapValues resource
                }
                val packagePath = requireNotNull(manifestEntry.packagePath) { "Bundled resource ${resource.id.value} has no package path." }
                val payload = requireNotNull(plan.assetsByPackagePath[packagePath]) { "Bundled resource ${resource.id.value} has no payload." }
                val target = safeChild(staging, packagePath)
                target.parentFile?.mkdirs()
                writeVerified(target, payload)
                val expectedHash = requireNotNull(manifestEntry.sha256)
                check(payload.sha256() == expectedHash) { "Resource ${resource.id.value} failed hash verification." }
                resource.copy(
                    scope = if (manifestEntry.dependencyKind == ProjectPackageDependencyKind.Bundled) {
                        ProjectResourceScope.ProjectLocal
                    } else {
                        resource.scope
                    },
                    location = "projects/$directoryName/$packagePath",
                    contentHash = "sha256:$expectedHash",
                    metadata = resource.metadata + mapOf("package.dependencyReference" to manifestEntry.dependencyReference),
                )
            }
            val relocatedProject = plan.project.copy(resources = relocatedResources)
            val serialized = ProjectSerializer.serialize(relocatedProject).toByteArray(Charsets.UTF_8)
            writeVerified(File(staging, PROJECT_SNAPSHOT_NAME), serialized)
            val decoded = ProjectSerializer.decode(File(staging, PROJECT_SNAPSHOT_NAME).readText())
            check((decoded as? ProjectDecodeResult.Decoded)?.project == relocatedProject) { "Staged project verification failed." }
            if (cancellation.isCancelled()) throw ProjectPackageFileCancelledException()
            check(staging.renameTo(finalDirectory)) { "Could not atomically finalize imported project." }
            val finalized = File(finalDirectory, PROJECT_SNAPSHOT_NAME)
            check(finalized.isFile && ProjectSerializer.deserialize(finalized.readText()) == relocatedProject) {
                "Finalized project verification failed."
            }
            ProjectPackageCommitResult.Success(relocatedProject, finalDirectory)
        } catch (_: ProjectPackageFileCancelledException) {
            ProjectPackageCommitResult.Cancelled
        } catch (error: Throwable) {
            if (finalDirectory.exists()) finalDirectory.deleteRecursively()
            failure(ProjectPackageDiagnosticCode.InvalidProject, error.message ?: "Atomic package import failed.")
        } finally {
            staging.deleteRecursively()
        }
    }

    fun rollbackImportedProject(projectId: ProjectId): Boolean {
        val directory = projectDirectory(projectId)
        if (!directory.exists()) return true
        val snapshot = File(directory, PROJECT_SNAPSHOT_NAME).takeIf(File::isFile)?.readText()
        val decodedId = snapshot
            ?.let(ProjectSerializer::decode)
            ?.let { it as? ProjectDecodeResult.Decoded }
            ?.project
            ?.id
        if (decodedId != projectId) return false
        return directory.deleteRecursively()
    }

    private fun projectDirectory(projectId: ProjectId): File = File(projectsRoot, projectDirectoryName(projectId))

    private fun projectDirectoryName(projectId: ProjectId): String = "project-${projectId.value.toByteArray(Charsets.UTF_8).sha256().take(24)}"

    private fun safeChild(root: File, relativePath: String): File {
        val child = File(root, relativePath)
        val rootPath = root.canonicalPath + File.separator
        require(child.canonicalPath.startsWith(rootPath)) { "Asset path escapes project staging: $relativePath" }
        return child
    }

    private fun writeVerified(target: File, bytes: ByteArray) {
        FileOutputStream(target).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        check(target.length() == bytes.size.toLong() && target.readBytes().contentEquals(bytes)) {
            "Could not verify ${target.name}."
        }
    }

    private fun failure(code: ProjectPackageDiagnosticCode, message: String) =
        ProjectPackageCommitResult.Failure(listOf(ProjectPackageDiagnostic(code, message)))

    private companion object {
        const val PROJECT_SNAPSHOT_NAME = "project.json"
    }
}

sealed interface ProjectAssetReleaseDecision {
    data object Allowed : ProjectAssetReleaseDecision
    data class Blocked(val reasons: List<String>) : ProjectAssetReleaseDecision
}

object ProjectAssetLifecycle {
    fun evaluateRelease(
        location: String,
        projects: Collection<ProjectDocument>,
        recoveryRetainedLocations: Set<String> = emptySet(),
    ): ProjectAssetReleaseDecision {
        val reasons = buildList {
            projects.forEach { project ->
                project.resources.values
                    .filter { it.location == location }
                    .forEach { resource -> add("${project.id.value} retains ${resource.id.value}") }
            }
            if (location in recoveryRetainedLocations) add("Undo or recovery history retains $location")
        }
        return if (reasons.isEmpty()) ProjectAssetReleaseDecision.Allowed else ProjectAssetReleaseDecision.Blocked(reasons.sorted())
    }
}

private class ProjectPackageFileCancelledException : RuntimeException()

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }
