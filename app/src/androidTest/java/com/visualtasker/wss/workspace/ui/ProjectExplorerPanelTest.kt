package com.visualtasker.wss.workspace.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.visualtasker.wss.workspace.model.PanelType
import com.visualtasker.wss.workspace.model.ProjectExplorerProjector
import com.visualtasker.wss.workspace.model.ProjectExplorerUiState
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectManifest
import de.visualtasker.workflow.core.ProjectWorkflowRoot
import de.visualtasker.workflow.core.WorkflowRootId
import de.visualtasker.workflow.core.WorkspaceDocument
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectExplorerPanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun activeRootCanBeSelectedAndOpenedInBlockProjection() {
        val rootId = WorkflowRootId("root:main")
        val projection = ProjectExplorerProjector.project(
            ProjectDocument(
                id = ProjectId("project:test"),
                manifest = ProjectManifest("Test", rootId),
                roots = mapOf(rootId to ProjectWorkflowRoot(rootId, "Main", WorkspaceDocument("workspace:test"))),
            ),
        )
        var state by mutableStateOf(ProjectExplorerUiState())
        var opened: Pair<WorkflowRootId, PanelType>? = null
        compose.setContent {
            MaterialTheme {
                ProjectExplorerPanel(
                    projection = projection,
                    uiState = state,
                    canUndo = false,
                    canRedo = false,
                    diagnosticMessage = null,
                    onUiStateChange = { state = it },
                    onCreateRoot = {},
                    onRenameRoot = { _, _ -> },
                    onDeleteItem = {},
                    onOpenRoot = { root, panel -> opened = root to panel },
                    onUndo = {},
                    onRedo = {},
                )
            }
        }

        compose.onNodeWithTag("project-item:workflow:${rootId.value}")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Aktiv", substring = true)
            .performClick()
        compose.onNodeWithText("Block").assertIsDisplayed().performClick()

        assertEquals(rootId to PanelType.BlockEditor, opened)
    }
}
