package com.visualtasker.wss.workspace.plugin.runtime

import android.os.Bundle
import java.util.UUID

object TaskerPluginContract {
    const val ACTION_EDIT_SETTING = "com.twofortyfouram.locale.intent.action.EDIT_SETTING"
    const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    const val CATEGORY_SETTING = "com.twofortyfouram.locale.intent.category.SETTING"
    const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    const val EXTRA_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"

    const val KEY_COMMAND = "com.visualtasker.wss.tasker.COMMAND"
    const val KEY_EVENT_NAME = "com.visualtasker.wss.tasker.EVENT_NAME"
    const val KEY_MESSAGE = "com.visualtasker.wss.tasker.MESSAGE"
    const val KEY_WORKSPACE = "com.visualtasker.wss.tasker.WORKSPACE"
    const val KEY_SCRIPT = "com.visualtasker.wss.tasker.SCRIPT"
    const val KEY_RUN_ID = "com.visualtasker.wss.tasker.RUN_ID"
    const val KEY_STATUS = "com.visualtasker.wss.tasker.STATUS"
    const val KEY_EVENT_SLOT = "com.visualtasker.wss.tasker.EVENT_SLOT"

    const val COMMAND_RECORD_EVENT = "record_event"
    const val COMMAND_OPEN_WORKSPACE = "open_workspace"
    const val COMMAND_RUN_SCRIPT_DRAFT = "run_script_draft"

    val commandLabels = listOf(
        COMMAND_RECORD_EVENT to "Event an RailTrace senden",
        COMMAND_OPEN_WORKSPACE to "Workspace oeffnen",
        COMMAND_RUN_SCRIPT_DRAFT to "Script-Draft an WSS senden",
    )

    val eventSlotLabels = listOf(
        "slot_1" to "WSS Event Slot 1",
        "slot_2" to "WSS Event Slot 2",
        "slot_3" to "WSS Event Slot 3",
    )

    val statusLabels = listOf(
        "received" to "Received",
        "running" to "Running",
        "done" to "Done",
        "error" to "Error",
    )

    fun buildBundle(
        command: String,
        eventName: String,
        message: String,
        workspace: String,
        script: String,
        runId: String = "",
        status: String = "received",
        eventSlot: String = "slot_1",
    ): Bundle = Bundle().apply {
        putString(KEY_COMMAND, command)
        putString(KEY_EVENT_NAME, eventName)
        putString(KEY_MESSAGE, message)
        putString(KEY_WORKSPACE, workspace)
        putString(KEY_SCRIPT, script)
        putString(KEY_RUN_ID, runId)
        putString(KEY_STATUS, status)
        putString(KEY_EVENT_SLOT, eventSlot)
    }

    fun blurb(bundle: Bundle): String {
        val values = mapOf(
            KEY_COMMAND to bundle.getString(KEY_COMMAND).orEmpty(),
            KEY_EVENT_NAME to bundle.getString(KEY_EVENT_NAME).orEmpty(),
            KEY_MESSAGE to bundle.getString(KEY_MESSAGE).orEmpty(),
            KEY_EVENT_SLOT to bundle.getString(KEY_EVENT_SLOT).orEmpty(),
            KEY_STATUS to bundle.getString(KEY_STATUS).orEmpty(),
        )
        return blurb(values)
    }

    fun blurb(values: Map<String, String>): String {
        val command = values[KEY_COMMAND].orEmpty().ifBlank { COMMAND_RECORD_EVENT }
        val label = commandLabels.firstOrNull { it.first == command }?.second ?: command
        val slot = values[KEY_EVENT_SLOT].orEmpty().ifBlank { "slot_1" }
        val slotLabel = eventSlotLabels.firstOrNull { it.first == slot }?.second ?: slot
        val status = values[KEY_STATUS].orEmpty().ifBlank { "received" }
        val event = values[KEY_EVENT_NAME].orEmpty().ifBlank { "Tasker Event" }
        val message = values[KEY_MESSAGE].orEmpty()
        return if (message.isBlank()) {
            "WSS: $slotLabel/$status - $label ($event)"
        } else {
            "WSS: $slotLabel/$status - $label ($event: $message)"
        }.take(90)
    }

    fun newRunId(): String = "tasker-${UUID.randomUUID()}"
}

data class TaskerPluginAction(
    val command: String,
    val eventName: String,
    val message: String,
    val workspace: String,
    val script: String,
    val runId: String,
    val status: String,
    val eventSlot: String,
) {
    val attributes: Map<String, String>
        get() = buildMap {
            put("command", command)
            put("eventName", eventName)
            put("message", message)
            put("runId", runId)
            put("status", status)
            put("eventSlot", eventSlot)
            if (workspace.isNotBlank()) put("workspace", workspace)
            if (script.isNotBlank()) put("script", script)
        }
}

object TaskerPluginBundleParser {
    fun parse(bundle: Bundle?): TaskerPluginAction {
        val source = bundle ?: Bundle.EMPTY
        return parse(
            mapOf(
                TaskerPluginContract.KEY_COMMAND to source.getString(TaskerPluginContract.KEY_COMMAND).orEmpty(),
                TaskerPluginContract.KEY_EVENT_NAME to source.getString(TaskerPluginContract.KEY_EVENT_NAME).orEmpty(),
                TaskerPluginContract.KEY_MESSAGE to source.getString(TaskerPluginContract.KEY_MESSAGE).orEmpty(),
                TaskerPluginContract.KEY_WORKSPACE to source.getString(TaskerPluginContract.KEY_WORKSPACE).orEmpty(),
                TaskerPluginContract.KEY_SCRIPT to source.getString(TaskerPluginContract.KEY_SCRIPT).orEmpty(),
                TaskerPluginContract.KEY_RUN_ID to source.getString(TaskerPluginContract.KEY_RUN_ID).orEmpty(),
                TaskerPluginContract.KEY_STATUS to source.getString(TaskerPluginContract.KEY_STATUS).orEmpty(),
                TaskerPluginContract.KEY_EVENT_SLOT to source.getString(TaskerPluginContract.KEY_EVENT_SLOT).orEmpty(),
            ),
        )
    }

    fun parse(values: Map<String, String>): TaskerPluginAction {
        val command = values[TaskerPluginContract.KEY_COMMAND]
            ?.takeIf { it.isNotBlank() }
            ?: TaskerPluginContract.COMMAND_RECORD_EVENT
        return TaskerPluginAction(
            command = command,
            eventName = values[TaskerPluginContract.KEY_EVENT_NAME].orEmpty()
                .ifBlank { "Tasker Event" },
            message = values[TaskerPluginContract.KEY_MESSAGE].orEmpty()
                .ifBlank { "Tasker Plugin ausgelöst" },
            workspace = values[TaskerPluginContract.KEY_WORKSPACE].orEmpty(),
            script = values[TaskerPluginContract.KEY_SCRIPT].orEmpty(),
            runId = values[TaskerPluginContract.KEY_RUN_ID].orEmpty()
                .ifBlank { TaskerPluginContract.newRunId() },
            status = values[TaskerPluginContract.KEY_STATUS].orEmpty()
                .ifBlank { "received" },
            eventSlot = values[TaskerPluginContract.KEY_EVENT_SLOT].orEmpty()
                .ifBlank { "slot_1" },
        )
    }
}
