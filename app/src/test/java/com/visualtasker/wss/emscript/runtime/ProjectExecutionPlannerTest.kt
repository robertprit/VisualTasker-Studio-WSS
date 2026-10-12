package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.workflow.core.ProjectDocument
import de.visualtasker.workflow.core.ProjectId
import de.visualtasker.workflow.core.ProjectManifest
import de.visualtasker.workflow.core.ProjectRevision
import de.visualtasker.workflow.core.ProjectWorkflowRoot
import de.visualtasker.workflow.core.WorkflowRootId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectExecutionPlannerTest {
    @Test
    fun selectedRootAloneProducesExecutionPlanWithProjectIdentity() {
        val project = project()
        val result = ProjectExecutionPlanner().plan(project, ROOT_B, "request:b") as ExecutionPlanResult.Ready

        assertEquals(project.id.value, result.plan.projectId)
        assertEquals(ROOT_B.value, result.plan.workflowRootId)
        assertEquals(project.revision.value, result.plan.projectRevision)
        assertEquals("request:b", result.plan.requestId)
        assertTrue(result.plan.steps.isNotEmpty())
    }

    @Test
    fun selectingRootDoesNotMergeOtherRootSteps() {
        val project = project()
        val planA = (ProjectExecutionPlanner().plan(project, ROOT_A, "request:a") as ExecutionPlanResult.Ready).plan
        val planB = (ProjectExecutionPlanner().plan(project, ROOT_B, "request:b") as ExecutionPlanResult.Ready).plan

        assertEquals(1, planA.steps.size)
        assertEquals(2, planB.steps.size)
        assertTrue(planA.steps.map { it.entityId }.intersect(planB.steps.map { it.entityId }.toSet()).isEmpty())
    }

    @Test
    fun missingRootReturnsTypedDiagnostic() {
        val result = ProjectExecutionPlanner().plan(project(), WorkflowRootId("root:missing"), "request:missing")

        assertTrue(result is ExecutionPlanResult.Rejected)
        assertEquals("PROJECT_ROOT_NOT_FOUND", (result as ExecutionPlanResult.Rejected).diagnostics.single().code)
    }

    private fun project(): ProjectDocument {
        val a = workspace("wait(1)", "workspace-a")
        val b = workspace("wait(2)\nwait(3)", "workspace-b")
        return ProjectDocument(
            id = ProjectId("project:runtime"),
            revision = ProjectRevision(7),
            manifest = ProjectManifest("Runtime", ROOT_A),
            roots = mapOf(
                ROOT_A to ProjectWorkflowRoot(ROOT_A, "A", a),
                ROOT_B to ProjectWorkflowRoot(ROOT_B, "B", b),
            ),
        )
    }

    private fun workspace(script: String, id: String) = EmscriptWorkspaceImporter().import(script, id).let {
        check(it.isSuccess) { it.issues.joinToString { issue -> issue.message } }
        requireNotNull(it.document)
    }

    private companion object {
        val ROOT_A = WorkflowRootId("root:a")
        val ROOT_B = WorkflowRootId("root:b")
    }
}
