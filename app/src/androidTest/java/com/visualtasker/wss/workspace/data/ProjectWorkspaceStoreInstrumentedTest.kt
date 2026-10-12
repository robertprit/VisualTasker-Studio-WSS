package com.visualtasker.wss.workspace.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.visualtasker.wss.workspace.model.ProjectExplorerSectionKind
import com.visualtasker.wss.workspace.model.ProjectExplorerUiState
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectManifest
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectWorkflowRoot
import de.visualtasker.workflow.core.WorkflowRootId
import de.visualtasker.workflow.core.WorkspaceDocument
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectWorkspaceStoreInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun clearStore() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.getSharedPreferences(ProjectWorkspaceStore.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun projectAndExplorerUiStateSurviveReloadWithoutChangingIds() {
        val rootId = WorkflowRootId("root:persisted")
        val workspace = WorkspaceDocument("workspace:persisted")
        val project = ProjectDocument(
            id = ProjectId("project:persisted"),
            manifest = ProjectManifest("Persisted", rootId),
            roots = mapOf(rootId to ProjectWorkflowRoot(rootId, "Main", workspace)),
        )
        val uiState = ProjectExplorerUiState(
            selectedItemKey = "workflow:${rootId.value}",
            expandedSections = setOf(ProjectExplorerSectionKind.Workflows, ProjectExplorerSectionKind.Paths),
            searchQuery = "Main",
        )
        ProjectWorkspaceStore(context).apply {
            saveProject(project)
            saveExplorerState(uiState)
        }

        val reloaded = ProjectWorkspaceStore(context)
        val loadedProject = reloaded.loadProject(WorkspaceDocument("legacy"))

        assertEquals(project.id, loadedProject.id)
        assertEquals(project.manifest, loadedProject.manifest)
        assertEquals(project.revision, loadedProject.revision)
        assertEquals(project.roots.keys, loadedProject.roots.keys)
        assertEquals(project.roots.getValue(rootId).name, loadedProject.roots.getValue(rootId).name)
        assertEquals(workspace.id, loadedProject.roots.getValue(rootId).workspace.id)
        assertEquals(workspace.version, loadedProject.roots.getValue(rootId).workspace.version)
        assertEquals(uiState, reloaded.loadExplorerState())
    }

    @Test
    fun legacySingleRootWorkspaceMigratesDeterministically() {
        val legacy = WorkspaceDocument("legacy-workspace")

        val first = ProjectWorkspaceStore(context).loadProject(legacy)
        val second = ProjectWorkspaceStore(context).loadProject(legacy)

        assertEquals(first.id, second.id)
        assertEquals(first.manifest.activeRootId, second.manifest.activeRootId)
        assertEquals(legacy, first.activeRoot()?.workspace)
    }
}
