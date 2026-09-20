package com.visualtasker.ime.tasker

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.visualtasker.ime.storage.PrefsStore

class TaskerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT)
            ?: intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE")
                ?.getString(EXTRA_TEXT)
            ?: return

        PrefsStore(context).setPendingTaskerText(text)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Tasker IME", text))

        context.sendBroadcast(Intent(ACTION_TASKER_INSERT_TEXT).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_TEXT, text)
        })
    }

    companion object {
        const val ACTION_TASKER_INSERT_TEXT = "com.visualtasker.ime.action.INSERT_TEXT"
        const val EXTRA_TEXT = "text"
    }
}
