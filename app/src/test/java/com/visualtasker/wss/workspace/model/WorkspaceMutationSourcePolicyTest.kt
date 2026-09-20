package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceMutationSourcePolicyTest {
    @Test
    fun repeatedAutoSyncAndFlowMoveAreCoalesced() {
        assertTrue(
            WorkspaceMutationSourcePolicy.coalesces(
                WORKFLOW_SOURCE_EMSCRIPT_AUTO,
                WORKFLOW_SOURCE_EMSCRIPT_AUTO,
            ),
        )
        assertTrue(
            WorkspaceMutationSourcePolicy.coalesces(
                "flowchart:panel-1:move",
                "flowchart:panel-1:move",
            ),
        )
    }

    @Test
    fun explicitApplyFileLoadAndSemanticChangesRemainUndoBoundaries() {
        assertFalse(
            WorkspaceMutationSourcePolicy.coalesces(
                WORKFLOW_SOURCE_EMSCRIPT_CONFIRM,
                WORKFLOW_SOURCE_EMSCRIPT_CONFIRM,
            ),
        )
        assertFalse(
            WorkspaceMutationSourcePolicy.coalesces(
                "${WORKFLOW_SOURCE_EMSCRIPT_FILE_PREFIX}example",
                "${WORKFLOW_SOURCE_EMSCRIPT_FILE_PREFIX}example",
            ),
        )
        assertFalse(
            WorkspaceMutationSourcePolicy.coalesces(
                "blockeditor:panel-1",
                "blockeditor:panel-1",
            ),
        )
    }
}
