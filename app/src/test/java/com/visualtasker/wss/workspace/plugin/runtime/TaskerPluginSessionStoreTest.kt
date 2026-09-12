package com.visualtasker.wss.workspace.plugin.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskerPluginSessionStoreTest {
    @Test
    fun recordsStatusTimelineAndLatestStatePerRunId() {
        val file = kotlin.io.path.createTempFile(prefix = "tasker-sessions", suffix = ".jsonl").toFile()
        file.writeText("")
        val base = TaskerPluginAction(
            command = TaskerPluginContract.COMMAND_RECORD_EVENT,
            eventName = "Roundtrip",
            message = "received",
            workspace = "",
            script = "",
            runId = "run-tasker-1",
            status = "received",
            eventSlot = "slot_2",
        )

        val first = TaskerPluginSessionStore.record(file, base)
        val second = TaskerPluginSessionStore.record(file, base.copy(status = "running", message = "running"))
        val third = TaskerPluginSessionStore.record(file, base.copy(status = "done", message = "done"))

        assertEquals(1, first.ordinal)
        assertEquals(2, second.ordinal)
        assertEquals(3, third.ordinal)
        val latest = TaskerPluginSessionStore.latestByRunId(file)
        assertEquals("done", latest.getValue("run-tasker-1").status)
        assertEquals("slot_2", latest.getValue("run-tasker-1").eventSlot)
        assertEquals(3, latest.getValue("run-tasker-1").ordinal)
        assertEquals("done", TaskerPluginSessionStore.lastResult(file)?.status)
        assertEquals("done", TaskerPluginSessionStore.lastResult(file, "run-tasker-1")?.status)
        assertEquals(null, TaskerPluginSessionStore.lastError(file, "run-tasker-1"))
    }

    @Test
    fun keepsIndependentRunCounters() {
        val file = kotlin.io.path.createTempFile(prefix = "tasker-sessions", suffix = ".jsonl").toFile()
        file.writeText("")
        val action = TaskerPluginAction(
            command = TaskerPluginContract.COMMAND_OPEN_WORKSPACE,
            eventName = "Open",
            message = "go",
            workspace = "Lab",
            script = "",
            runId = "run-a",
            status = "received",
            eventSlot = "slot_1",
        )

        TaskerPluginSessionStore.record(file, action)
        TaskerPluginSessionStore.record(file, action.copy(runId = "run-b"))
        TaskerPluginSessionStore.record(file, action.copy(status = "done"))

        val latest = TaskerPluginSessionStore.latestByRunId(file)
        assertEquals(2, latest.size)
        assertEquals(2, latest.getValue("run-a").ordinal)
        assertEquals(1, latest.getValue("run-b").ordinal)
    }

    @Test
    fun findsLastErrorGloballyOrByRunId() {
        val file = kotlin.io.path.createTempFile(prefix = "tasker-sessions", suffix = ".jsonl").toFile()
        file.writeText("")
        val action = TaskerPluginAction(
            command = TaskerPluginContract.COMMAND_RECORD_EVENT,
            eventName = "Roundtrip",
            message = "ok",
            workspace = "",
            script = "",
            runId = "run-ok",
            status = "done",
            eventSlot = "slot_1",
        )

        TaskerPluginSessionStore.record(file, action)
        TaskerPluginSessionStore.record(file, action.copy(runId = "run-bad", status = "error", message = "boom"))
        TaskerPluginSessionStore.record(file, action.copy(runId = "run-ok", status = "done", message = "still ok"))

        assertEquals("boom", TaskerPluginSessionStore.lastError(file)?.message)
        assertEquals("boom", TaskerPluginSessionStore.lastError(file, "run-bad")?.message)
        assertEquals(null, TaskerPluginSessionStore.lastError(file, "run-ok"))
    }
}
