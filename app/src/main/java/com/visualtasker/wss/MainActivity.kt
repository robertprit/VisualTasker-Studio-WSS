package com.visualtasker.wss

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.visualtasker.wss.recording.RecordingSessionRuntime
import com.visualtasker.wss.ui.theme.MultiPanelTheme
import com.visualtasker.wss.workspace.model.RecordingEventStore
import com.visualtasker.wss.workspace.model.M3ShapeMakerBridgeContract
import com.visualtasker.wss.workspace.model.VisualAssetBridgeInbox
import com.visualtasker.wss.workspace.ui.WorkspaceScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val watchDogScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val kind = when (action) {
                Intent.ACTION_SCREEN_OFF -> "screen.lock"
                Intent.ACTION_SCREEN_ON -> "screen.on"
                Intent.ACTION_USER_PRESENT -> "screen.unlock"
                else -> return
            }
            val label = when (kind) {
                "screen.lock" -> "Bildschirm gesperrt"
                "screen.unlock" -> "Bildschirm entsperrt"
                else -> "Bildschirm aktiv"
            }
            RecordingEventStore.recordExternalEvent(
                context = context.applicationContext,
                source = "watchdog",
                kind = kind,
                label = label,
                attributes = mapOf(
                    "status" to "done",
                    "category" to "screen",
                    "action" to action,
                ),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptVisualAssetIntent(intent)
        enableEdgeToEdge()
        lifecycleScope.launch {
            RecordingSessionRuntime.recover(applicationContext)
        }
        RecordingEventStore.recordExternalEvent(
            context = applicationContext,
            source = "watchdog",
            kind = "app.start",
            label = "App gestartet",
            attributes = mapOf(
                "status" to "done",
                "category" to "app",
                "package" to packageName,
            ),
        )
        ContextCompat.registerReceiver(
            this,
            watchDogScreenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        setContent {
            val prefs = remember { getSharedPreferences("panel_ui_options", MODE_PRIVATE) }
            var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "dark") ?: "dark") }
            MultiPanelTheme(themeMode = themeMode) {
                WorkspaceScreen(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        prefs.edit().putString("theme_mode", mode).apply()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptVisualAssetIntent(intent)
    }

    private fun acceptVisualAssetIntent(intent: Intent?) {
        if (intent?.action != M3ShapeMakerBridgeContract.ACTION_IMPORT_VISUAL_ASSET) return
        val uri = intent.data ?: return
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("VisualAsset konnte nicht gelesen werden.")
        }.onSuccess(VisualAssetBridgeInbox::offer)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(watchDogScreenReceiver) }
        super.onDestroy()
    }
}
