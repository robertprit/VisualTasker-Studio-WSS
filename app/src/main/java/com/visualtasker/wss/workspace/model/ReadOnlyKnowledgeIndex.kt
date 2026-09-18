package com.visualtasker.wss.workspace.model

enum class KnowledgeIndexSourceKind {
    Scene,
    Entity,
    Observation,
    Resource,
    Step,
    Record,
    Ambiguity,
}

enum class KnowledgeVerificationState {
    Structured,
    Observed,
    Imported,
    Proposed,
    Conflicting,
}

data class KnowledgeIndexEntry(
    val id: String,
    val sourceKind: KnowledgeIndexSourceKind,
    val sourceId: String,
    val title: String,
    val content: String,
    val sourceRefs: Set<String>,
    val verificationState: KnowledgeVerificationState,
    val confidence: Float,
    val updatedAtEpochMs: Long = 0L,
    val terms: Set<String>,
) {
    init {
        require(id.startsWith("knowledge:")) { "Knowledge entry id must use the knowledge namespace." }
        require(title.isNotBlank() && title == title.trim()) { "Knowledge title must be nonblank and trimmed." }
        require(content.isNotBlank() && content == content.trim()) { "Knowledge content must be nonblank and trimmed." }
        require(sourceRefs.isNotEmpty()) { "Knowledge entry must retain at least one source reference." }
        require(confidence in 0f..1f) { "Knowledge confidence must stay in 0..1." }
    }
}

data class KnowledgeIndexHit(
    val entry: KnowledgeIndexEntry,
    val relevance: Float,
    val reason: String,
)

data class ReadOnlyKnowledgeIndex(
    val worldviewRevision: Long,
    val resourceRevision: Long,
    val entries: List<KnowledgeIndexEntry>,
) {
    fun search(query: String, limit: Int = 12): List<KnowledgeIndexHit> {
        require(limit > 0) { "Knowledge search limit must be positive." }
        val queryTerms = query.toKnowledgeTerms()
        if (queryTerms.isEmpty()) return emptyList()
        return entries.mapNotNull { entry ->
            val matchingTerms = queryTerms.intersect(entry.terms)
            if (matchingTerms.isEmpty()) return@mapNotNull null
            val titleTerms = entry.title.toKnowledgeTerms()
            val titleMatches = queryTerms.intersect(titleTerms).size
            val coverage = matchingTerms.size.toFloat() / queryTerms.size
            val titleBoost = titleMatches.toFloat() / queryTerms.size * 0.25f
            KnowledgeIndexHit(
                entry = entry,
                relevance = (coverage * 0.75f + titleBoost).coerceIn(0f, 1f),
                reason = "Treffer: ${matchingTerms.sorted().joinToString(", ")}",
            )
        }
            .sortedWith(
                compareByDescending<KnowledgeIndexHit> { it.relevance }
                    .thenByDescending { it.entry.confidence }
                    .thenByDescending { it.entry.updatedAtEpochMs }
                    .thenBy { it.entry.id },
            )
            .take(limit)
    }
}

/** Builds an immutable retrieval projection. It has no reducer or mutation authority. */
object ReadOnlyKnowledgeIndexProjector {
    fun project(document: WorldviewDocument): ReadOnlyKnowledgeIndex {
        val entries = buildList {
            document.scenes.forEach { scene ->
                add(
                    entry(
                        id = "knowledge:scene:${scene.id}",
                        sourceKind = KnowledgeIndexSourceKind.Scene,
                        sourceId = scene.id,
                        title = scene.label,
                        fields = scene.metadata,
                        sourceRefs = setOf(scene.id),
                        verificationState = KnowledgeVerificationState.Structured,
                        confidence = 1f,
                        updatedAtEpochMs = scene.endedAtEpochMs ?: scene.startedAtEpochMs,
                    ),
                )
            }
            document.entities.forEach { entity ->
                add(
                    entry(
                        id = "knowledge:entity:${entity.id}",
                        sourceKind = KnowledgeIndexSourceKind.Entity,
                        sourceId = entity.id,
                        title = entity.label,
                        fields = entity.properties + mapOf("kind" to entity.kind.name, "state" to entity.state.name),
                        sourceRefs = setOf(entity.id) + entity.observationIds + entity.resourceIds + listOfNotNull(entity.sceneId),
                        verificationState = if (entity.state == KnowledgeState.Conflicting) {
                            KnowledgeVerificationState.Conflicting
                        } else {
                            KnowledgeVerificationState.Structured
                        },
                        confidence = if (entity.state == KnowledgeState.Conflicting) 0.5f else 1f,
                    ),
                )
            }
            document.observations.forEach { observation ->
                add(
                    entry(
                        id = "knowledge:observation:${observation.id}",
                        sourceKind = KnowledgeIndexSourceKind.Observation,
                        sourceId = observation.id,
                        title = observation.properties["text"]?.takeIf { it.isNotBlank() } ?: observation.kind.name,
                        fields = observation.properties + mapOf(
                            "provider" to observation.provider.name,
                            "kind" to observation.kind.name,
                        ),
                        sourceRefs = setOf(observation.id) + listOfNotNull(observation.sceneId, observation.entityId),
                        verificationState = KnowledgeVerificationState.Observed,
                        confidence = observation.confidence,
                        updatedAtEpochMs = observation.observedAtEpochMs,
                    ),
                )
            }
            document.resources.resources.forEach { resource ->
                add(
                    entry(
                        id = "knowledge:resource:${resource.id}",
                        sourceKind = KnowledgeIndexSourceKind.Resource,
                        sourceId = resource.id,
                        title = resource.label,
                        fields = resource.metadata + mapOf(
                            "kind" to resource.kind.name,
                            "pluginOwner" to resource.pluginOwner,
                            "mimeType" to resource.mimeType.orEmpty(),
                        ),
                        sourceRefs = setOf(resource.id),
                        verificationState = KnowledgeVerificationState.Imported,
                        confidence = 1f,
                        updatedAtEpochMs = resource.updatedAtEpochMs,
                    ),
                )
            }
            document.steps.forEach { step ->
                add(
                    entry(
                        id = "knowledge:step:${step.id}",
                        sourceKind = KnowledgeIndexSourceKind.Step,
                        sourceId = step.id,
                        title = step.label,
                        fields = mapOf(
                            "interpretation" to step.interpretation.name,
                            "proposedIntent" to step.proposedIntent.orEmpty(),
                        ),
                        sourceRefs = setOf(step.id) + step.eventIds + step.observationIds + step.entityIds + listOfNotNull(step.sceneId),
                        verificationState = if (step.interpretation == InterpretationState.IntendedProposal) {
                            KnowledgeVerificationState.Proposed
                        } else {
                            KnowledgeVerificationState.Structured
                        },
                        confidence = if (step.interpretation == InterpretationState.IntendedProposal) 0.6f else 1f,
                    ),
                )
            }
            document.records.forEach { record ->
                add(
                    entry(
                        id = "knowledge:record:${record.id}",
                        sourceKind = KnowledgeIndexSourceKind.Record,
                        sourceId = record.id,
                        title = record.label,
                        fields = mapOf("stepCount" to record.stepIds.size.toString()),
                        sourceRefs = setOf(record.id) + record.sceneIds + record.stepIds + record.eventIds,
                        verificationState = KnowledgeVerificationState.Structured,
                        confidence = 1f,
                        updatedAtEpochMs = record.endedAtEpochMs ?: record.startedAtEpochMs,
                    ),
                )
            }
            document.ambiguities.forEach { ambiguity ->
                add(
                    entry(
                        id = "knowledge:ambiguity:${ambiguity.id}",
                        sourceKind = KnowledgeIndexSourceKind.Ambiguity,
                        sourceId = ambiguity.id,
                        title = ambiguity.type.name,
                        fields = ambiguity.metadata + mapOf(
                            "impact" to ambiguity.impact,
                            "resolutionState" to ambiguity.resolutionState.name,
                        ),
                        sourceRefs = setOf(ambiguity.id) + ambiguity.subjectRefs + ambiguity.evidenceObservationIds + ambiguity.candidateRefs,
                        verificationState = if (ambiguity.resolutionState == AmbiguityResolutionState.Proposed) {
                            KnowledgeVerificationState.Proposed
                        } else {
                            KnowledgeVerificationState.Conflicting
                        },
                        confidence = ambiguity.confidence,
                    ),
                )
            }
        }.sortedWith(compareBy<KnowledgeIndexEntry> { it.sourceKind.name }.thenBy { it.title }.thenBy { it.id })
        return ReadOnlyKnowledgeIndex(
            worldviewRevision = document.revision,
            resourceRevision = document.resources.revision,
            entries = entries,
        )
    }

    private fun entry(
        id: String,
        sourceKind: KnowledgeIndexSourceKind,
        sourceId: String,
        title: String,
        fields: Map<String, String>,
        sourceRefs: Set<String>,
        verificationState: KnowledgeVerificationState,
        confidence: Float,
        updatedAtEpochMs: Long = 0L,
    ): KnowledgeIndexEntry {
        val content = buildString {
            append(title)
            fields.toSortedMap().forEach { (key, value) ->
                if (value.isNotBlank()) append(" | $key=$value")
            }
        }.trim()
        return KnowledgeIndexEntry(
            id = id,
            sourceKind = sourceKind,
            sourceId = sourceId,
            title = title.trim(),
            content = content,
            sourceRefs = sourceRefs,
            verificationState = verificationState,
            confidence = confidence,
            updatedAtEpochMs = updatedAtEpochMs,
            terms = content.toKnowledgeTerms(),
        )
    }
}

private fun String.toKnowledgeTerms(): Set<String> =
    lowercase()
        .split(Regex("[^\\p{L}\\p{N}._:-]+"))
        .map(String::trim)
        .filter { it.length >= 2 }
        .toSet()
