package com.visualtasker.wss.workspace.plugin.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.visualtasker.wss.workspace.model.RecordingEventStore

const val TASKER_TEST_FEEDBACK_ACTION = "com.visualtasker.wss.action.TASKER_TEST_FEEDBACK"

class TaskerFeedbackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TASKER_TEST_FEEDBACK_ACTION) return
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME).orEmpty().ifBlank { "Tasker" }
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty().ifBlank { "Tasker Feedback empfangen" }
        val attributes = buildMap {
            put("taskName", taskName)
            put("message", message)
            intent.extras?.keySet().orEmpty()
                .filterNot { it in setOf(EXTRA_TASK_NAME, EXTRA_MESSAGE) }
                .sorted()
                .forEach { key ->
                    intent.extras?.get(key)?.toString()?.let { value -> put(key, value) }
                }
        }
        RecordingEventStore.recordExternalEvent(
            context = context.applicationContext,
            source = "tasker",
            kind = "tasker.feedback",
            label = "Tasker: $message",
            attributes = attributes,
        )
        Toast.makeText(context.applicationContext, "Tasker: $message", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_MESSAGE = "message"
    }
}
