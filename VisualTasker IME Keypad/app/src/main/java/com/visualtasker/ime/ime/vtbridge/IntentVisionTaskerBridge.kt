package com.visualtasker.ime.ime.vtbridge

import android.content.Context
import android.content.Intent
import android.os.Bundle

class IntentVisionTaskerBridge(private val context: Context) : VisionTaskerBridge {
    override fun dispatch(action: String, payload: Map<String, String>): Boolean {
        if (!isWssInstalled()) return false
        val pluginCommand = mapActionToPluginCommand(action)
        val eventName = "ime.$action"
        val message = payload["message"] ?: "IME command: $action"
        val script = payload["script"] ?: buildFallbackScript(action, payload)
        val workspace = payload["workspace"].orEmpty()
        val runId = "ime-${System.currentTimeMillis()}"

        val bundle = Bundle().apply {
            putString(KEY_COMMAND, pluginCommand)
            putString(KEY_EVENT_NAME, eventName)
            putString(KEY_MESSAGE, message)
            putString(KEY_WORKSPACE, workspace)
            putString(KEY_SCRIPT, script)
            putString(KEY_RUN_ID, runId)
            putString(KEY_STATUS, "received")
            putString(KEY_EVENT_SLOT, payload["eventSlot"] ?: "slot_1")
        }

        val intent = Intent(ACTION_FIRE_SETTING).apply {
            setPackage(WSS_PACKAGE)
            putExtra(EXTRA_BUNDLE, bundle)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        context.sendBroadcast(intent)
        return true
    }

    private fun isWssInstalled(): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(WSS_PACKAGE, 0)
        }.isSuccess
    }

    private fun mapActionToPluginCommand(action: String): String {
        return when (action) {
            "start_workflow" -> COMMAND_RUN_SCRIPT_DRAFT
            else -> COMMAND_RECORD_EVENT
        }
    }

    private fun buildFallbackScript(action: String, payload: Map<String, String>): String {
        return when (action) {
            "ocr" -> "readText()"
            "screenshot" -> "screenshot()"
            "toggle_recorder" -> "recorder.toggle()"
            "select_point" -> "point.select()"
            "select_region" -> "region.select()"
            "start_workflow" -> "workflow.start(\"${payload["name"] ?: "default"}\")"
            else -> action
        }
    }

    companion object {
        private const val WSS_PACKAGE = "com.visualtasker.wss"
        private const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
        private const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"

        private const val KEY_COMMAND = "com.visualtasker.wss.tasker.COMMAND"
        private const val KEY_EVENT_NAME = "com.visualtasker.wss.tasker.EVENT_NAME"
        private const val KEY_MESSAGE = "com.visualtasker.wss.tasker.MESSAGE"
        private const val KEY_WORKSPACE = "com.visualtasker.wss.tasker.WORKSPACE"
        private const val KEY_SCRIPT = "com.visualtasker.wss.tasker.SCRIPT"
        private const val KEY_RUN_ID = "com.visualtasker.wss.tasker.RUN_ID"
        private const val KEY_STATUS = "com.visualtasker.wss.tasker.STATUS"
        private const val KEY_EVENT_SLOT = "com.visualtasker.wss.tasker.EVENT_SLOT"

        private const val COMMAND_RECORD_EVENT = "record_event"
        private const val COMMAND_RUN_SCRIPT_DRAFT = "run_script_draft"
    }
}
