package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadOnlyKnowledgeIndexTest {
    @Test
    fun indexesStableSourcesWithProvenanceAndRevision() {
        val resource = WorkspaceResource(
            id = "template:login",
            kind = WorkspaceResourceKind.Template,
            label = "Login Template",
            pluginOwner = "visualtasker.vision.template",
            metadata = mapOf("ocrText" to "Anmelden"),
            updatedAtEpochMs = 55L,
        )
        val document = WorldviewDocument.fromResources(
            WorkspaceResourceBundle(revision = 7L, resources = listOf(resource)),
        )

        val index = ReadOnlyKnowledgeIndexProjector.project(document)

        assertEquals(7L, index.worldviewRevision)
        assertEquals(7L, index.resourceRevision)
        val resourceEntry = index.entries.single { it.sourceId == resource.id }
        assertEquals(KnowledgeVerificationState.Imported, resourceEntry.verificationState)
        assertTrue(resource.id in resourceEntry.sourceRefs)
        assertTrue("anmelden" in resourceEntry.terms)
    }

    @Test
    fun searchIsDeterministicAndReturnsInclusionReason() {
        val resource = WorkspaceResource(
            id = "template:login",
            kind = WorkspaceResourceKind.Template,
            label = "Login Template",
            metadata = mapOf("ocrText" to "Anmelden"),
        )
        val index = ReadOnlyKnowledgeIndexProjector.project(
            WorldviewDocument.fromResources(WorkspaceResourceBundle(resources = listOf(resource))),
        )

        val hits = index.search("login anmelden")

        assertTrue(hits.isNotEmpty())
        assertEquals("template:login", hits.first().entry.sourceId)
        assertEquals(0.875f, hits.first().relevance)
        assertTrue(hits.first().reason.contains("login"))
    }

    @Test
    fun ambiguityRemainsConflictInsteadOfBecomingFact() {
        val ambiguity = WorldAmbiguity(
            id = "ambiguity:login-text",
            type = AmbiguityType.ProviderConflict,
            subjectRefs = setOf("entity:login"),
            confidence = 0.4f,
            impact = "OCR und A11Y widersprechen sich.",
        )

        val index = ReadOnlyKnowledgeIndexProjector.project(
            WorldviewDocument(ambiguities = listOf(ambiguity)),
        )

        val entry = index.entries.single()
        assertEquals(KnowledgeVerificationState.Conflicting, entry.verificationState)
        assertEquals(0.4f, entry.confidence)
    }
}
