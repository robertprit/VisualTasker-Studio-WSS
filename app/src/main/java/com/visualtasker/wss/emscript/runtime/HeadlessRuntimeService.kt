package com.visualtasker.wss.emscript.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.visualtasker.wss.MainActivity
import com.visualtasker.wss.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HeadlessRuntimeService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var host: AndroidRuntimeHost
    private var observerJob: Job? = null
    private var explicitStop = false

    override fun onCreate() {
        super.onCreate()
        host = RuntimeProcessHost.get(applicationContext)
        createNotificationChannel()
        promote(buildNotification(host.currentSession.value))
        observerJob = serviceScope.launch {
            host.currentSession.collectLatest { snapshot ->
                updateNotification(snapshot)
                if (snapshot != null && !snapshot.isActive) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.sessionIdOrNull()
                if (sessionId == null || !host.startReserved(sessionId)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
            ACTION_STOP -> {
                explicitStop = true
                val sessionId = intent.sessionIdOrNull()
                val reason = intent.getStringExtra(EXTRA_STOP_REASON)
                    ?.let { runCatching { ExecutionCancellationReason.valueOf(it) }.getOrNull() }
                    ?: ExecutionCancellationReason.NOTIFICATION_STOP
                if (sessionId == null || !host.stop(sessionId, reason)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
            else -> {
                // START_NOT_STICKY deliberately prevents implicit replay after process/service loss.
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observerJob?.cancel()
        if (!explicitStop && host.currentSession.value?.isActive == true) {
            host.stopCurrent(ExecutionCancellationReason.SERVICE_STOP)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun promote(notification: Notification) {
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
    }

    private fun updateNotification(snapshot: ExecutionSessionSnapshot?) {
        if (snapshot?.isActive != true) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(snapshot))
    }

    private fun buildNotification(snapshot: ExecutionSessionSnapshot?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = snapshot?.sessionId?.let { sessionId ->
            PendingIntent.getService(
                this,
                REQUEST_STOP,
                stopIntent(this, sessionId, ExecutionCancellationReason.NOTIFICATION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("VisualTasker Runtime")
            .setContentText(snapshot.notificationText())
            .setContentIntent(contentIntent)
            .setOngoing(snapshot?.isActive == true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .apply {
                if (stopIntent != null) addAction(0, "Stop", stopIntent)
            }
            .build()
    }

    private fun ExecutionSessionSnapshot?.notificationText(): String = when {
        this == null -> "Runtime wird vorbereitet"
        state == ExecutionSessionState.STOPPING -> "Workflow wird gestoppt"
        else -> "${state.name} · ${workflowId.take(36)}"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Workflow-Ausführung",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Status und Stop-Aktion für aktive VisualTasker-Workflows"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun Intent.sessionIdOrNull(): ExecutionSessionId? =
        getStringExtra(EXTRA_SESSION_ID)?.takeIf(String::isNotBlank)?.let(::ExecutionSessionId)

    companion object {
        const val ACTION_START = "com.visualtasker.wss.runtime.START_SESSION"
        const val ACTION_STOP = "com.visualtasker.wss.runtime.STOP_SESSION"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_STOP_REASON = "stopReason"
        const val CHANNEL_ID = "visualtasker-runtime"
        const val NOTIFICATION_ID = 5108
        private const val REQUEST_OPEN = 51080
        private const val REQUEST_STOP = 51081

        fun startIntent(context: Context, sessionId: ExecutionSessionId): Intent =
            Intent(context, HeadlessRuntimeService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_SESSION_ID, sessionId.value)

        fun stopIntent(
            context: Context,
            sessionId: ExecutionSessionId,
            reason: ExecutionCancellationReason,
        ): Intent = Intent(context, HeadlessRuntimeService::class.java)
            .setAction(ACTION_STOP)
            .putExtra(EXTRA_SESSION_ID, sessionId.value)
            .putExtra(EXTRA_STOP_REASON, reason.name)
    }
}
