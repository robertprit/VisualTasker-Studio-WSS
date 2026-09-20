package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSelectionEchoGuardTest {
    @Test
    fun externalSelectionIsConsumedExactlyOnce() {
        val guard = WorkspaceSelectionEchoGuard<String>()

        guard.expect("block-a")

        assertTrue(guard.consumeIfEcho("block-a"))
        assertFalse(guard.consumeIfEcho("block-a"))
    }

    @Test
    fun differentUserSelectionIsNeverSuppressed() {
        val guard = WorkspaceSelectionEchoGuard<String>()

        guard.expect("block-a")

        assertFalse(guard.consumeIfEcho("block-b"))
        assertFalse(guard.consumeIfEcho("block-a"))
    }
}
