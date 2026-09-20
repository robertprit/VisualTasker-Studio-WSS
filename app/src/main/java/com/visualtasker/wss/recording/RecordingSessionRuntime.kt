package com.visualtasker.wss.recording

import android.content.Context
import android.util.Log
import com.visualtasker.wss.recording.persistence.RecorderDatabase
import com.visualtasker.wss.recording.persistence.RoomRecorderSessionStore
import com.visualtasker.wss.recording.persistence.ScreenshotAssetStore
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

object RecordingSessionRuntime {
    private const val TAG = "RECORDER/R1"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coordinatorRef = AtomicReference<RecordingSessionCoordinator?>(null)
    private val activeSessionIdRef = AtomicReference<String?>(null)
    private val pendingTapJobs = Collections.synchronizedSet(mutableSetOf<Job>())

    suspend fun start(
        context: Context,
        createdBy: String = "floatingOverlay",
    ): RecordingOperationResult<RecordingSession> {
        val coordinator = coordinator(context)
        return coordinator.start(createdBy).also { result ->
            if (result is RecordingOperationResult.Success) {
                activeSessionIdRef.set(result.value.sessionId)
                Log.i(TAG, "Session ${result.value.sessionId} started")
            }
        }
    }

    suspend fun continueInterrupted(
        context: Context,
        interruptedSessionId: String,
        createdBy: String = "railtrace-recovery",
    ): RecordingOperationResult<RecordingSession> {
        val coordinator = coordinator(context)
        return coordinator.continueInterruptedSession(interruptedSessionId, createdBy).also { result ->
            if (result is RecordingOperationResult.Success) {
                activeSessionIdRef.set(result.value.sessionId)
                RecordingPlaybackRuntime.refresh(context)
                Log.i(
                    TAG,
                    "Session ${result.value.sessionId} continued from ${result.value.resumedFromSessionId}",
                )
            }
        }
    }

    fun recordTap(xPx: Int, yPx: Int) {
        if (activeSessionIdRef.get() == null) return
        val job = scope.launch {
            delay(180)
            when (val result = coordinatorRef.get()?.recordTap(xPx, yPx)) {
                is RecordingOperationResult.Failure -> Log.e(TAG, "Tap capture failed: ${result.code} ${result.message}", result.cause)
                is RecordingOperationResult.Success -> Log.i(TAG, "Tap ${result.value.interactionId}: ${result.value.status}")
                null -> Unit
            }
        }
        pendingTapJobs += job
        job.invokeOnCompletion { pendingTapJobs -= job }
    }

    suspend fun stop(): RecordingOperationResult<RecordingSession> {
        val coordinator = coordinatorRef.get()
            ?: return RecordingOperationResult.Failure("NOT_RECORDING", "No recording session is active.")
        synchronized(pendingTapJobs) { pendingTapJobs.toList() }.joinAll()
        return coordinator.stop().also { result ->
            if (result is RecordingOperationResult.Success) {
                activeSessionIdRef.set(null)
                RecordingPlaybackRuntime.refresh(appContext(), loadLatestWhenIdle = true)
                Log.i(TAG, "Session ${result.value.sessionId} completed")
            }
        }
    }

    suspend fun recover(context: Context): RecordingOperationResult<List<String>> =
        if (activeSessionIdRef.get() != null) {
            RecordingOperationResult.Success(emptyList())
        } else coordinator(context).recoverInterruptedSessions().also { result ->
            if (result is RecordingOperationResult.Success && result.value.isNotEmpty()) {
                Log.w(TAG, "Recovered interrupted sessions: ${result.value.joinToString()}")
            }
        }

    fun isRecording(): Boolean = activeSessionIdRef.get() != null

    private val contextRef = AtomicReference<Context?>(null)

    private fun appContext(): Context = requireNotNull(contextRef.get()) { "Recorder context is not initialized." }

    private fun coordinator(context: Context): RecordingSessionCoordinator =
        coordinatorRef.get() ?: synchronized(this) {
            contextRef.compareAndSet(null, context.applicationContext)
            coordinatorRef.get() ?: RecordingSessionCoordinator(
                store = RoomRecorderSessionStore(RecorderDatabase.get(context)),
                assetStore = ScreenshotAssetStore(File(context.filesDir, "recorder/assets")),
                captureSource = AndroidRecorderCaptureSource(context.applicationContext),
            ).also(coordinatorRef::set)
        }
}
