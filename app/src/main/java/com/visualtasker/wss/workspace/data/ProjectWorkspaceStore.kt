package com.visualtasker.wss.workspace.data

import android.content.Context
import com.visualtasker.wss.workspace.model.ProjectExplorerSectionKind
import com.visualtasker.wss.workspace.model.ProjectExplorerUiState
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.WorkspaceDocument
import de.visualtasker.workflow.serialization.ProjectDecodeResult
import de.visualtasker.workflow.serialization.ProjectSerializer
import org.json.JSONArray
import org.json.JSONObject

class ProjectWorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadProject(legacyWorkspace: WorkspaceDocument): ProjectDocument {
        val raw = prefs.getString(PROJECT_KEY, null)
        val decoded = raw?.let(ProjectSerializer::decode)
        return (decoded as? ProjectDecodeResult.Decoded)?.project
            ?: ProjectDocument.fromLegacyWorkspace(legacyWorkspace, "VisualTasker Project")
    }

    fun saveProject(project: ProjectDocument) {
        prefs.edit().putString(PROJECT_KEY, ProjectSerializer.serialize(project)).apply()
    }

    fun saveProjectAtomically(project: ProjectDocument): Boolean =
        prefs.edit().putString(PROJECT_KEY, ProjectSerializer.serialize(project)).commit()

    fun loadExplorerState(): ProjectExplorerUiState {
        val raw = prefs.getString(EXPLORER_STATE_KEY, null) ?: return ProjectExplorerUiState()
        return runCatching {
            val json = JSONObject(raw)
            val expanded = json.optJSONArray("expanded") ?: JSONArray()
            ProjectExplorerUiState(
                selectedItemKey = json.optString("selected", "").ifBlank { null },
                expandedSections = buildSet {
                    for (index in 0 until expanded.length()) {
                        runCatching { ProjectExplorerSectionKind.valueOf(expanded.getString(index)) }.getOrNull()?.let(::add)
                    }
                },
                searchQuery = json.optString("query", ""),
            )
        }.getOrDefault(ProjectExplorerUiState())
    }

    fun saveExplorerState(state: ProjectExplorerUiState) {
        val expanded = JSONArray().apply { state.expandedSections.sortedBy { it.name }.forEach { put(it.name) } }
        val json = JSONObject()
            .put("selected", state.selectedItemKey.orEmpty())
            .put("expanded", expanded)
            .put("query", state.searchQuery)
        prefs.edit().putString(EXPLORER_STATE_KEY, json.toString()).apply()
    }

    companion object {
        internal const val PREFERENCES_NAME = "workspace_project"
        internal const val PROJECT_KEY = "project_json"
        internal const val EXPLORER_STATE_KEY = "explorer_state_json"
    }
}
