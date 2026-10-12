package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.core.BlockId
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectManifest
import de.visualtasker.workflow.core.ProjectWorkflowRoot
import de.visualtasker.workflow.core.WorkflowRootId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectEditorSessionTest {
    @Test
    fun rootSwitchProjectsOnlySelectedRootIntoAllThreeEditors() {
        val initialProject = project()
        val session = ProjectEditorSession(initialProject)
        val first = requireNotNull(session.current)
        val opened = session.openRoot(ROOT_B) as ProjectRootOpenResult.Opened

        assertTrue(first.workflow.emscriptProjection.getOrThrow().contains("A"))
        assertFalse(first.workflow.emscriptProjection.getOrThrow().contains("B"))
        assertTrue(opened.state.workflow.emscriptProjection.getOrThrow().contains("B"))
        assertTrue(opened.state.workflow.document.blocks.isNotEmpty())
        assertTrue(opened.state.workflow.flowchartProjection.graph.nodes.isNotEmpty())
        assertEquals(initialProject.roots.getValue(ROOT_A).workspace, opened.state.project.roots.getValue(ROOT_A).workspace)
    }

    @Test
    fun selectionIsRememberedPerRootWithoutCrossRootIdentityLeak() {
        val session = ProjectEditorSession(project())
        val selectionA = WorkspaceSelectionState(blockId = BlockId("a"), source = "test:a")
        session.updateSelection(selectionA)
        session.openRoot(ROOT_B, selectionA)
        val selectionB = WorkspaceSelectionState(blockId = BlockId("b"), source = "test:b")
        session.updateSelection(selectionB)

        val reopened = session.openRoot(ROOT_A, selectionB) as ProjectRootOpenResult.Opened

        assertEquals(selectionA, reopened.state.selection)
        assertEquals(BlockId("a"), reopened.state.selection.blockId)
    }

    @Test
    fun missingRootDoesNotChangeCurrentProjection() {
        val session = ProjectEditorSession(project())
        val before = session.current

        val result = session.openRoot(WorkflowRootId("root:missing"))

        assertTrue(result is ProjectRootOpenResult.MissingRoot)
        assertEquals(before, session.current)
    }

    private fun project(): ProjectDocument = ProjectDocument(
        id = ProjectId("project:editors"),
        manifest = ProjectManifest("Editors", ROOT_A),
        roots = mapOf(
            ROOT_A to ProjectWorkflowRoot(ROOT_A, "A", workspace("click(\"A\")", "workspace-a")),
            ROOT_B to ProjectWorkflowRoot(ROOT_B, "B", workspace("click(\"B\")", "workspace-b")),
        ),
    )

    private fun workspace(source: String, id: String) = EmscriptWorkspaceImporter().import(source, id).let {
        check(it.isSuccess) { it.issues.joinToString { issue -> issue.message } }
        requireNotNull(it.document)
    }

    private companion object {
        val ROOT_A = WorkflowRootId("root:a")
        val ROOT_B = WorkflowRootId("root:b")
    }
}
