package com.visualtasker.wss.workspace.plugin.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.visualtasker.wss.MainActivity
import com.visualtasker.wss.workspace.model.RecordingEventStore

class TaskerPluginReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TaskerPluginContract.ACTION_FIRE_SETTING) return
        val action = TaskerPluginBundleParser.parse(
            intent.getBundleExtra(TaskerPluginContract.EXTRA_BUNDLE) ?: intent.extras,
        )
        val appContext = context.applicationContext
        val settings = TaskerPluginSettings.load(appContext)
        val session = TaskerPluginSessionStore.record(appContext, action)
        if (settings.recordToRailTrace) {
            RecordingEventStore.recordExternalEvent(
                context = appContext,
                source = "tasker-plugin",
                kind = "tasker.plugin.${action.command}.${action.status}",
                label = "Tasker ${action.eventSlot}/${action.status}: ${action.eventName}",
                attributes = action.attributes + mapOf("sessionOrdinal" to session.ordinal.toString()),
            )
        }
        if (settings.autoOpenWorkspace) {
            when (action.command) {
                TaskerPluginContract.COMMAND_OPEN_WORKSPACE,
                TaskerPluginContract.COMMAND_RUN_SCRIPT_DRAFT -> openWorkspace(context)
            }
        }
        if (settings.showToasts) {
            Toast.makeText(appContext, "WSS Tasker: ${action.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWorkspace(context: Context) {
        val launch = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(launch)
    }
}
