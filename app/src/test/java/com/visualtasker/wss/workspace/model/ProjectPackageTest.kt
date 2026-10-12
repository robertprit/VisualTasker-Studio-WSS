package com.visualtasker.wss.workspace.model

import de.visualtasker.workflow.core.DefinitionId
import de.visualtasker.workflow.core.ProjectDefinition
import de.visualtasker.workflow.core.ProjectDefinitionKind
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectManifest
import de.visualtasker.workflow.core.ProjectResource
import de.visualtasker.workflow.core.ProjectResourceScope
import de.visualtasker.workflow.core.ProjectResourceType
import de.visualtasker.workflow.core.ProjectVariable
import de.visualtasker.workflow.core.ProjectWorkflowRoot
import de.visualtasker.workflow.core.ResourceId
import de.visualtasker.workflow.core.VariableDefinition
import de.visualtasker.workflow.core.VariableScope
import de.visualtasker.workflow.core.WorkflowRootId
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.serialization.ProjectSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectPackageTest {
    @Test
    fun emptyProjectRoundtripsWithoutMutation() {
        val project = project()
        val before = ProjectSerializer.serialize(project)
        val normalized = ProjectSerializer.deserialize(before)

        val exported = export(project)
        val imported = preflight(exported.bytes)

        assertEquals(normalized, imported.project)
        assertEquals(before, ProjectSerializer.serialize(project))
        assertEquals(project.revision, imported.project.revision)
    }

    @Test
    fun singleAndMultiRootProjectsPreserveIds() {
        val first = project()
        val secondRoot = ProjectWorkflowRoot(WorkflowRootId("root:second"), "Second", WorkspaceDocument("workspace:second"))
        val multi = first.copy(roots = first.roots + (secondRoot.id to secondRoot))

        assertEquals(first.roots.keys, preflight(export(first).bytes).project.roots.keys)
        assertEquals(multi.roots.keys, preflight(export(multi).bytes).project.roots.keys)
    }

    @Test
    fun definitionsVariablesMetadataAndRecorderProvenanceRemainLossless() {
        val definition = ProjectDefinition(
            DefinitionId("definition:helper"),
            "Helper",
            ProjectDefinitionKind.Subworkflow,
            WorkspaceDocument("workspace:helper"),
            metadata = mapOf("sourceAnchor" to "anchor:helper"),
        )
        val variable = ProjectVariable(
            VariableDefinition("counter", "Counter", "Number", VariableScope.Project, "0"),
            metadata = mapOf("owner" to "project"),
        )
        val source = project().copy(
            definitions = mapOf(definition.id to definition),
            variables = mapOf("counter" to variable),
            metadata = mapOf("recorder.provenance" to "session:42"),
        )

        val normalized = ProjectSerializer.deserialize(ProjectSerializer.serialize(source))
        assertEquals(normalized, preflight(export(source).bytes).project)
    }

    @Test
    fun imageTemplatePointRegionPathAndShapeAssetsAreBundledAndHashed() {
        val resources = listOf(
            resource("image", ProjectResourceType.Image),
            resource("template", ProjectResourceType.Template),
            resource("point", ProjectResourceType.Point),
            resource("region", ProjectResourceType.Region),
            resource("path", ProjectResourceType.Path),
            resource("shape", ProjectResourceType.Shape),
        )
        val payloads = resources.associate { it.id.value to "payload:${it.id.value}".toByteArray() }
        val exported = export(project(resources), payloads)

        assertEquals(resources.size, exported.manifest.resources.count { it.dependencyKind == ProjectPackageDependencyKind.Bundled })
        exported.manifest.resources.forEach { entry ->
            assertTrue(entry.packagePath?.startsWith("assets/") == true)
            assertTrue(entry.dependencyReference.startsWith("package:"))
            assertTrue(entry.sha256?.length == 64)
            assertTrue((entry.sizeBytes ?: 0L) > 0L)
        }
        assertEquals(resources.map { it.id }.toSet(), preflight(exported.bytes).project.resources.keys)
    }

    @Test
    fun embeddedAssetUsesPortableReferenceUntilAtomicRelocation() {
        val embedded = resource("embedded", ProjectResourceType.Shape, ProjectResourceScope.Embedded, "inline:workspace-resource:embedded")
        val plan = preflight(export(project(listOf(embedded)), mapOf(embedded.id.value to "shape".toByteArray())).bytes)

        assertEquals(ProjectPackageDependencyKind.Embedded, plan.manifest.resources.single().dependencyKind)
        assertEquals("embedded:${embedded.id.value}", plan.project.resources.getValue(embedded.id).location)
    }

    @Test
    fun appSharedAndExternalDependenciesAreDeclaredButNotCopied() {
        val shared = resource("shared", ProjectResourceType.Template, ProjectResourceScope.AppShared, "appshared:login")
        val external = resource("external", ProjectResourceType.Provider, ProjectResourceScope.External, "provider:vision")
        var resolverCalls = 0
        val result = ProjectPackageCodec.export(
            project(shared, external),
            ProjectAssetResolver { resolverCalls += 1; null },
            exportedAtEpochMs = 1L,
        ) as ProjectPackageExportResult.Success

        assertEquals(0, resolverCalls)
        assertEquals(ProjectPackageDependencyKind.AppShared, result.manifest.resources.first { it.resourceId == shared.id.value }.dependencyKind)
        assertEquals(ProjectPackageDependencyKind.External, result.manifest.resources.first { it.resourceId == external.id.value }.dependencyKind)
        assertEquals(listOf("provider:vision"), result.manifest.providerDependencies)
    }

    @Test
    fun deviceLocalExternalDependencyIsRejectedInsteadOfLeakingAPath() {
        val external = resource("external-file", ProjectResourceType.Provider, ProjectResourceScope.External, "/data/local/tmp/provider.bin")

        val result = ProjectPackageCodec.export(project(external), ProjectAssetResolver { null })

        assertEquals(ProjectPackageDiagnosticCode.InvalidProject, (result as ProjectPackageExportResult.Failure).diagnostics.single().code)
    }

    @Test
    fun volatileCaptureIsDeclaredButNotExported() {
        val capture = resource("capture", ProjectResourceType.Capture).copy(metadata = mapOf("capture.transient" to "true"))
        val exported = ProjectPackageCodec.export(project(capture), ProjectAssetResolver { error("resolver must not run") }) as ProjectPackageExportResult.Success

        val entry = exported.manifest.resources.single()
        assertEquals(ProjectPackageDependencyKind.VolatileCapture, entry.dependencyKind)
        assertEquals(null, entry.packagePath)
        assertEquals("capture-transient:${capture.id.value}", preflight(exported.bytes).project.resources.getValue(capture.id).location)
    }

    @Test
    fun missingRequiredAssetStopsExport() {
        val result = ProjectPackageCodec.export(project(resource("missing", ProjectResourceType.Image)), ProjectAssetResolver { null })

        assertEquals(ProjectPackageDiagnosticCode.MissingAsset, (result as ProjectPackageExportResult.Failure).diagnostics.single().code)
    }

    @Test
    fun exportTimestampDoesNotAffectContentFingerprint() {
        val item = resource("image", ProjectResourceType.Image)
        val payloads = mapOf(item.id.value to "pixels".toByteArray())

        val first = export(project(item), payloads, exportedAt = 1L)
        val second = export(project(item), payloads, exportedAt = 99_999L)

        assertEquals(first.manifest.contentFingerprint, second.manifest.contentFingerprint)
        assertNotEquals(first.manifest.exportedAtEpochMs, second.manifest.exportedAtEpochMs)
    }

    @Test
    fun projectLocalAbsoluteLocationsAreNotWrittenToPortablePackage() {
        val absolute = resource("private-image", ProjectResourceType.Image, location = "/data/user/0/example/private.png")
        val exported = export(project(absolute), mapOf(absolute.id.value to "pixels".toByteArray()))
        val entries = unzip(exported.bytes)

        assertFalse(entries.getValue("manifest.json").toString(Charsets.UTF_8).contains(absolute.location))
        assertFalse(entries.getValue("project.json").toString(Charsets.UTF_8).contains(absolute.location))
        assertEquals("package:${absolute.id.value}", preflight(exported.bytes).project.resources.getValue(absolute.id).location)
    }

    @Test
    fun exportImportExportFingerprintSurvivesRelocation() {
        val item = resource("image", ProjectResourceType.Image)
        val payload = "pixels".toByteArray()
        val first = export(project(item), mapOf(item.id.value to payload))
        val plan = preflight(first.bytes)
        val root = Files.createTempDirectory("vtpkg-roundtrip").toFile()
        try {
            val committed = ProjectPackageFileRepository(root).commit(plan) as ProjectPackageCommitResult.Success
            val second = export(committed.project, mapOf(item.id.value to payload), exportedAt = 77L)
            assertEquals(first.manifest.contentFingerprint, second.manifest.contentFingerprint)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun invalidAssetHashIsRejected() {
        val item = resource("image", ProjectResourceType.Image)
        val exported = export(project(item), mapOf(item.id.value to "pixels".toByteArray()))
        val path = exported.manifest.resources.single().packagePath!!
        val tampered = rewriteZip(exported.bytes) { entries -> entries[path] = "tampered".toByteArray() }

        val result = ProjectPackageCodec.preflight(tampered)

        assertTrue(result is ProjectPackagePreflightResult.Rejected)
        assertTrue((result as ProjectPackagePreflightResult.Rejected).diagnostics.any { it.code == ProjectPackageDiagnosticCode.HashMismatch })
    }

    @Test
    fun malformedManifestIsRejected() {
        val exported = export(project())
        val malformed = rewriteZip(exported.bytes) { it["manifest.json"] = "{".toByteArray() }

        assertEquals(ProjectPackageDiagnosticCode.InvalidManifest, rejected(malformed).first().code)
    }

    @Test
    fun unsupportedPackageVersionIsRejected() {
        val exported = export(project())
        val changed = rewriteManifest(exported.bytes) { it.put("packageFormatVersion", 99) }

        assertTrue(rejected(changed).any { it.code == ProjectPackageDiagnosticCode.UnsupportedPackageVersion })
    }

    @Test
    fun duplicateManifestResourceIdsAndPathsAreRejected() {
        val first = resource("one", ProjectResourceType.Image)
        val second = resource("two", ProjectResourceType.Template)
        val exported = export(project(first, second), mapOf(first.id.value to byteArrayOf(1), second.id.value to byteArrayOf(2)))
        val changed = rewriteManifest(exported.bytes) { manifest ->
            val resources = manifest.getJSONArray("resources")
            resources.getJSONObject(1).put("resourceId", first.id.value)
            resources.getJSONObject(1).put("packagePath", resources.getJSONObject(0).getString("packagePath"))
        }

        val diagnostics = rejected(changed)
        assertTrue(diagnostics.any { it.code == ProjectPackageDiagnosticCode.DuplicateId })
        assertTrue(diagnostics.any { it.code == ProjectPackageDiagnosticCode.DuplicateEntry })
    }

    @Test
    fun manifestNameAndDependencyKindMustMatchCanonicalProject() {
        val item = resource("image", ProjectResourceType.Image)
        val exported = export(project(item), mapOf(item.id.value to byteArrayOf(1)))
        val changed = rewriteManifest(exported.bytes) { manifest ->
            manifest.put("projectName", "Different")
            manifest.getJSONArray("resources").getJSONObject(0).put("dependencyKind", ProjectPackageDependencyKind.Embedded.name)
        }

        val diagnostics = rejected(changed)
        assertTrue(diagnostics.any { it.message.contains("name") })
        assertTrue(diagnostics.any { it.message.contains("contract") })
    }

    @Test
    fun traversalAndAbsoluteZipEntriesAreRejectedBeforeExtraction() {
        val traversal = zip(mapOf("../escape" to byteArrayOf(1)))
        val absolute = zip(mapOf("/absolute" to byteArrayOf(1)))

        assertEquals(ProjectPackageDiagnosticCode.InvalidEntryPath, rejected(traversal).single().code)
        assertEquals(ProjectPackageDiagnosticCode.InvalidEntryPath, rejected(absolute).single().code)
    }

    @Test
    fun packageAndEntryLimitsStopZipBombStylePayloads() {
        val oversized = zip(mapOf("manifest.json" to ByteArray(512) { 1 }))
        val result = ProjectPackageCodec.preflight(
            oversized,
            limits = ProjectPackageLimits(maxEntryBytes = 64, maxTotalBytes = 128),
        )

        assertTrue(result is ProjectPackagePreflightResult.Rejected)
        assertTrue((result as ProjectPackagePreflightResult.Rejected).diagnostics.any {
            it.code == ProjectPackageDiagnosticCode.AssetTooLarge || it.code == ProjectPackageDiagnosticCode.PackageTooLarge
        })
    }

    @Test
    fun cancellationStopsExportAndPreflight() {
        val cancelled = ProjectPackageCancellation { true }

        assertEquals(ProjectPackageExportResult.Cancelled, ProjectPackageCodec.export(project(), ProjectAssetResolver { null }, cancellation = cancelled))
        assertEquals(ProjectPackagePreflightResult.Cancelled, ProjectPackageCodec.preflight(byteArrayOf(1), cancellation = cancelled))
    }

    @Test
    fun cancellationBeforeAtomicCommitLeavesNoProjectDirectory() {
        val plan = preflight(export(project()).bytes)
        val root = Files.createTempDirectory("vtpkg-cancelled-commit").toFile()
        try {
            val result = ProjectPackageFileRepository(root).commit(plan, ProjectPackageCancellation { true })

            assertEquals(ProjectPackageCommitResult.Cancelled, result)
            assertFalse(fileAt(root, "projects").listFiles().orEmpty().any { !it.name.startsWith(".staging-") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun projectConflictAbortsWithoutMutation() {
        val source = project()
        val result = ProjectPackageCodec.preflight(
            export(source).bytes,
            ProjectPackageImportRequest(existingProjectIds = setOf(source.id)),
        )

        assertEquals(ProjectPackageDiagnosticCode.ProjectIdConflict, (result as ProjectPackagePreflightResult.Rejected).diagnostics.single().code)
        assertEquals("project:test", source.id.value)
    }

    @Test
    fun importAsCopyChangesOnlyProjectIdentityAndPreservesInternalIds() {
        val source = project()
        val copyId = ProjectId("project:copy")
        val plan = preflight(
            export(source).bytes,
            ProjectPackageImportRequest(
                existingProjectIds = setOf(source.id),
                conflictMode = ProjectPackageConflictMode.ImportAsCopy,
                copyProjectId = copyId,
            ),
        )

        assertTrue(plan.importedAsCopy)
        assertEquals(copyId, plan.project.id)
        assertEquals(source.roots.keys, plan.project.roots.keys)
        assertEquals(source.definitions.keys, plan.project.definitions.keys)
        assertEquals(source.resources.keys, plan.project.resources.keys)
    }

    @Test
    fun atomicCommitRelocatesAssetsAndCleansStaging() {
        val item = resource("image", ProjectResourceType.Image)
        val payload = "pixels".toByteArray()
        val plan = preflight(export(project(item), mapOf(item.id.value to payload)).bytes)
        val root = Files.createTempDirectory("vtpkg-commit").toFile()
        try {
            val result = ProjectPackageFileRepository(root).commit(plan) as ProjectPackageCommitResult.Success
            val relocated = result.project.resources.getValue(item.id)
            assertTrue(relocated.location.startsWith("projects/project-"))
            assertTrue(relocated.location.endsWith(".bin"))
            assertTrue(fileAt(root, relocated.location).isFile)
            assertFalse(fileAt(root, "projects").listFiles().orEmpty().any { it.name.startsWith(".staging-") })
            assertEquals(result.project, ProjectSerializer.deserialize(fileAt(result.projectDirectory, "project.json").readText()))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun failedCommitRollsBackAndDoesNotTouchForeignAssets() {
        val item = resource("image", ProjectResourceType.Image)
        val validPlan = preflight(export(project(item), mapOf(item.id.value to "pixels".toByteArray())).bytes)
        val brokenPlan = validPlan.copy(assetsByPackagePath = emptyMap())
        val root = Files.createTempDirectory("vtpkg-rollback").toFile()
        val foreign = fileAt(root, "foreign/keep.bin").apply { parentFile!!.mkdirs(); writeText("keep") }
        try {
            val result = ProjectPackageFileRepository(root).commit(brokenPlan)
            assertTrue(result is ProjectPackageCommitResult.Failure)
            assertEquals("keep", foreign.readText())
            assertFalse(fileAt(root, "projects").listFiles().orEmpty().any { !it.name.startsWith(".staging-") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun registeredSnapshotSurvivesSaveReloadAndIsNotOverwritten() {
        val root = Files.createTempDirectory("vtpkg-catalog").toFile()
        try {
            val repository = ProjectPackageFileRepository(root)
            val source = project()
            assertTrue(repository.ensureRegisteredSnapshot(source))
            assertTrue(source.id in repository.registeredProjectIds())
            assertTrue(repository.ensureRegisteredSnapshot(source.copy(manifest = source.manifest.copy(name = "Changed"))))
            assertTrue(source.id in repository.registeredProjectIds())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun releaseRequiresAllProjectsAndRecoveryHistoryToLetGo() {
        val item = resource("image", ProjectResourceType.Image)
        val location = item.location

        assertTrue(ProjectAssetLifecycle.evaluateRelease(location, listOf(project(item))) is ProjectAssetReleaseDecision.Blocked)
        assertTrue(ProjectAssetLifecycle.evaluateRelease(location, emptyList(), setOf(location)) is ProjectAssetReleaseDecision.Blocked)
        assertEquals(ProjectAssetReleaseDecision.Allowed, ProjectAssetLifecycle.evaluateRelease(location, emptyList()))
    }

    private fun export(
        project: ProjectDocument,
        payloads: Map<String, ByteArray> = emptyMap(),
        exportedAt: Long = 123L,
    ): ProjectPackageExportResult.Success = ProjectPackageCodec.export(
        project = project,
        assetResolver = ProjectAssetResolver { resource -> payloads[resource.id.value]?.let { ResolvedProjectAsset(it) } },
        exportedAtEpochMs = exportedAt,
    ) as ProjectPackageExportResult.Success

    private fun preflight(
        bytes: ByteArray,
        request: ProjectPackageImportRequest = ProjectPackageImportRequest(),
    ): ProjectPackageImportPlan = (ProjectPackageCodec.preflight(bytes, request) as ProjectPackagePreflightResult.Ready).plan

    private fun rejected(bytes: ByteArray): List<ProjectPackageDiagnostic> =
        (ProjectPackageCodec.preflight(bytes) as ProjectPackagePreflightResult.Rejected).diagnostics

    private fun project(vararg resources: ProjectResource): ProjectDocument = project(resources.toList())

    private fun project(resources: List<ProjectResource> = emptyList()): ProjectDocument {
        val rootId = WorkflowRootId("root:test")
        return ProjectDocument(
            id = ProjectId("project:test"),
            manifest = ProjectManifest("Test", rootId, metadata = mapOf("owner" to "test")),
            roots = mapOf(rootId to ProjectWorkflowRoot(rootId, "Main", WorkspaceDocument("workspace:test"))),
            resources = resources.associateBy { it.id },
        )
    }

    private fun resource(
        suffix: String,
        type: ProjectResourceType,
        scope: ProjectResourceScope = ProjectResourceScope.ProjectLocal,
        location: String = "assets/$suffix.bin",
    ) = ProjectResource(ResourceId("resource:$suffix"), suffix, type, scope, location)

    private fun rewriteManifest(bytes: ByteArray, mutation: (JSONObject) -> Unit): ByteArray = rewriteZip(bytes) { entries ->
        val manifest = JSONObject(entries.getValue("manifest.json").toString(Charsets.UTF_8))
        mutation(manifest)
        entries["manifest.json"] = manifest.toString().toByteArray()
    }

    private fun rewriteZip(bytes: ByteArray, mutation: (MutableMap<String, ByteArray>) -> Unit): ByteArray {
        val entries = unzip(bytes).toMutableMap()
        mutation(entries)
        return zip(entries)
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                put(entry.name, zip.readBytes())
            }
        }
    }

    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name).apply { time = 0L })
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun fileAt(root: java.io.File, relative: String) = java.io.File(root, relative)
}
