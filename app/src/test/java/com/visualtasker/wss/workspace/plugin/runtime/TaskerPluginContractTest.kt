package com.visualtasker.wss.workspace.plugin.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskerPluginContractTest {
    @Test
    fun parserDefaultsToRecordEvent() {
        val action = TaskerPluginBundleParser.parse(emptyMap())

        assertEquals(TaskerPluginContract.COMMAND_RECORD_EVENT, action.command)
        assertEquals("Tasker Event", action.eventName)
        assertEquals("Tasker Plugin ausgelöst", action.message)
        assertEquals("received", action.status)
        assertEquals("slot_1", action.eventSlot)
        assertTrue(action.runId.startsWith("tasker-"))
    }

    @Test
    fun parserKeepsConfiguredPayload() {
        val values = mapOf(
            TaskerPluginContract.KEY_COMMAND to TaskerPluginContract.COMMAND_RUN_SCRIPT_DRAFT,
            TaskerPluginContract.KEY_EVENT_NAME to "Demo",
            TaskerPluginContract.KEY_MESSAGE to "Run me",
            TaskerPluginContract.KEY_WORKSPACE to "Lab",
            TaskerPluginContract.KEY_SCRIPT to "log(\"tasker\")",
            TaskerPluginContract.KEY_RUN_ID to "run-42",
            TaskerPluginContract.KEY_STATUS to "done",
            TaskerPluginContract.KEY_EVENT_SLOT to "slot_2",
        )

        val action = TaskerPluginBundleParser.parse(values)

        assertEquals(TaskerPluginContract.COMMAND_RUN_SCRIPT_DRAFT, action.command)
        assertEquals("Demo", action.eventName)
        assertEquals("Run me", action.message)
        assertEquals("Lab", action.workspace)
        assertEquals("log(\"tasker\")", action.script)
        assertEquals("run-42", action.runId)
        assertEquals("done", action.status)
        assertEquals("slot_2", action.eventSlot)
        assertEquals("Run me", action.attributes["message"])
        assertEquals("run-42", action.attributes["runId"])
        assertEquals("slot_2", action.attributes["eventSlot"])
    }

    @Test
    fun blurbStaysShortEnoughForTaskerLists() {
        val values = mapOf(
            TaskerPluginContract.KEY_COMMAND to TaskerPluginContract.COMMAND_OPEN_WORKSPACE,
            TaskerPluginContract.KEY_EVENT_NAME to "Roundtrip",
            TaskerPluginContract.KEY_MESSAGE to "x".repeat(160),
            TaskerPluginContract.KEY_EVENT_SLOT to "slot_3",
            TaskerPluginContract.KEY_STATUS to "running",
        )

        val blurb = TaskerPluginContract.blurb(values)

        assertTrue(blurb.startsWith("WSS: WSS Event Slot 3/running - Workspace oeffnen"))
        assertTrue(blurb.length <= 90)
    }
}
