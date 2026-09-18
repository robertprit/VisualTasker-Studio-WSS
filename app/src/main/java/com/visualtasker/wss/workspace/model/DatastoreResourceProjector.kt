package com.visualtasker.wss.workspace.model

import java.io.File

object DatastoreResourceProjector {
    fun project(
        base: WorkspaceResourceBundle,
        recordingSessions: List<RecordingSessionUi>,
        runtimeValues: Map<String, String>,
    ): WorkspaceResourceBundle {
        val generated = buildList {
            recordingSessions.forEach { session ->
                add(
                    WorkspaceResource(
                        id = stableId("recording", session.path),
                        kind = WorkspaceResourceKind.Dataset,
                        label = session.label.trim().ifBlank { session.fileName },
                        pluginOwner = "visualtasker.recorder",
                        uri = File(session.path).toURI().toString(),
                        mimeType = "application/x-ndjson",
                        tags = setOf("recording", "railtrace", "session"),
                        metadata = mapOf(
                            "fileName" to session.fileName,
                            "stepCount" to session.stepCount.toString(),
                            "durationMs" to session.durationMs.toString(),
                        ),
                        updatedAtEpochMs = session.lastModifiedMs,
                    ),
                )
            }
            runtimeValues.toSortedMap().forEach { (key, value) ->
                add(
                    WorkspaceResource(
                        id = stableId("runtime", key),
                        kind = WorkspaceResourceKind.Dataset,
                        label = key.trim().ifBlank { "runtime value" },
                        pluginOwner = "visualtasker.runtime",
                        mimeType = "text/plain",
                        tags = setOf("runtime", "key-value"),
                        metadata = mapOf("key" to key, "value" to value),
                    ),
                )
            }
        }
        val generatedIds = generated.mapTo(mutableSetOf()) { it.id }
        val merged = base.resources.filterNot { it.id in generatedIds } + generated
        return base.copy(
            revision = base.revision + if (generated.isEmpty()) 0 else 1,
            resources = merged.sortedWith(compareBy<WorkspaceResource> { it.kind.name }.thenBy { it.label }),
        )
    }

    private fun stableId(namespace: String, source: String): String {
        val slug = source.lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .takeLast(48)
            .ifBlank { "item" }
        return "$namespace:$slug:${source.hashCode().toUInt().toString(16)}"
    }
}
