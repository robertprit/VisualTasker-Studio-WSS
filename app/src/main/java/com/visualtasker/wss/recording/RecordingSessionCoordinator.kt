package com.visualtasker.wss.recording

import com.visualtasker.wss.recording.persistence.RecorderSessionStore
import com.visualtasker.wss.recording.persistence.ScreenshotAssetStore
import com.visualtasker.wss.recording.persistence.StoredScreenshotAsset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RecordingSessionCoordinator(
    private val store: RecorderSessionStore,
    private val assetStore: ScreenshotAssetStore,
    private val captureSource: RecorderCaptureSource,
    private val clock: RecorderClock = SystemRecorderClock,
    private val idGenerator: RecorderIdGenerator = UuidRecorderIdGenerator,
) {
    private val writerMutex = Mutex()
    private var activeSessionId: String? = null

    suspend fun start(
        createdBy: String,
        policy: RecordingPolicySnapshot = RecordingPolicySnapshot(),
    ): RecordingOperationResult<RecordingSession> = ioLocked {
        if (activeSessionId != null) return@ioLocked failure("ALREADY_RECORDING", "A recording session is already active.")
        val startedEpoch = clock.epochMillis()
        val startedElapsed = clock.elapsedRealtimeNanos()
        val preparing = RecordingSession(
            sessionId = idGenerator.nextId("session"),
            status = RecordingSessionStatus.PREPARING,
            startedAtEpochMs = startedEpoch,
            startedAtElapsedRealtimeNanos = startedElapsed,
            createdBy = createdBy,
            recordingPolicySnapshot = policy,
        )
        try {
            store.createSession(preparing)
        } catch (error: Throwable) {
            return@ioLocked failure("SESSION_CREATE_FAILED", "Could not persist recording session.", error)
        }

        val capture = try {
            captureSource.capture()
        } catch (error: Throwable) {
            persistTerminalFailure(preparing, RecordingSessionStatus.FAILED, "INITIAL_CAPTURE_FAILED: ${error.message}")
            return@ioLocked failure("INITIAL_CAPTURE_FAILED", "Initial screenshot or accessibility snapshot failed.", error)
        }
        val stored = try {
            assetStore.putPng(capture.pngBytes)
        } catch (error: Throwable) {
            persistTerminalFailure(preparing, RecordingSessionStatus.FAILED, "INITIAL_ASSET_FAILED: ${error.message}")
            return@ioLocked failure("INITIAL_ASSET_FAILED", "Initial screenshot asset could not be stored.", error)
        }

        val sceneId = idGenerator.nextId("scene")
        val frameId = idGenerator.nextId("frame")
        val scene = RecordingScene(
            sceneId = sceneId,
            sessionId = preparing.sessionId,
            sequence = 1,
            openedAtEpochMs = startedEpoch,
            openedAtElapsedRealtimeNanos = startedElapsed,
            primaryFrameId = frameId,
            windowContext = capture.windowContext,
            status = RecordingSceneStatus.OPEN,
        )
        val frame = capture.toFrame(preparing.sessionId, sceneId, frameId, stored)
        val snapshot = capture.toSnapshot(preparing.sessionId, sceneId, frameId)
        val recording = preparing.copy(
            status = RecordingSessionStatus.RECORDING,
            initialSceneId = sceneId,
            latestSceneId = sceneId,
        )
        try {
            store.persistInitialState(recording, scene, stored.asset, frame, snapshot)
        } catch (error: Throwable) {
            assetStore.deleteIfCreated(stored)
            persistTerminalFailure(preparing, RecordingSessionStatus.FAILED, "INITIAL_METADATA_FAILED: ${error.message}")
            return@ioLocked failure("INITIAL_METADATA_FAILED", "Initial recording metadata could not be stored.", error)
        }
        activeSessionId = recording.sessionId
        RecordingOperationResult.Success(recording)
    }

    suspend fun recordTap(
        xPx: Int,
        yPx: Int,
        targetA11yNodeId: String? = null,
    ): RecordingOperationResult<TapInteraction> = ioLocked {
        val sessionId = activeSessionId ?: return@ioLocked failure("NOT_RECORDING", "No recording session is active.")
        val record = store.load(sessionId) ?: return@ioLocked failure("SESSION_MISSING", "Active recording session is missing.")
        val beforeScene = record.scenes.firstOrNull { it.sceneId == record.session.latestSceneId }
            ?: return@ioLocked failure("SCENE_MISSING", "Active recording scene is missing.")
        val sequence = store.maxRawSequence(sessionId) + 1
        val occurredEpoch = clock.epochMillis()
        val occurredElapsed = clock.elapsedRealtimeNanos()
        val raw = RawRecordingEvent(
            rawEventId = idGenerator.nextId("raw"),
            sessionId = sessionId,
            sequence = sequence,
            occurredAtEpochMs = occurredEpoch,
            occurredAtElapsedRealtimeNanos = occurredElapsed,
            kind = "tap",
            payload = mapOf("xPx" to xPx.toString(), "yPx" to yPx.toString()),
        )
        val pending = TapInteraction(
            interactionId = idGenerator.nextId("tap"),
            rawEventId = raw.rawEventId,
            sessionId = sessionId,
            sequence = sequence,
            occurredAtEpochMs = occurredEpoch,
            occurredAtElapsedRealtimeNanos = occurredElapsed,
            xPx = xPx,
            yPx = yPx,
            beforeSceneId = beforeScene.sceneId,
            targetA11yNodeId = targetA11yNodeId,
            status = RecordingInteractionStatus.CAPTURING_AFTER,
        )
        try {
            store.persistTap(raw, pending)
        } catch (error: Throwable) {
            return@ioLocked failure("TAP_PERSIST_FAILED", "Tap event could not be persisted.", error)
        }

        val capture = try {
            captureSource.capture()
        } catch (error: Throwable) {
            val failed = pending.copy(status = RecordingInteractionStatus.CAPTURE_FAILED)
            val partial = record.session.copy(status = RecordingSessionStatus.PARTIAL, failure = "TAP_CAPTURE_FAILED: ${error.message}")
            runCatching { store.persistFailedInteraction(partial, failed) }
            activeSessionId = null
            return@ioLocked failure("TAP_CAPTURE_FAILED", "After-tap capture failed.", error)
        }
        val stored = try {
            assetStore.putPng(capture.pngBytes)
        } catch (error: Throwable) {
            val failed = pending.copy(status = RecordingInteractionStatus.CAPTURE_FAILED)
            val partial = record.session.copy(status = RecordingSessionStatus.PARTIAL, failure = "TAP_ASSET_FAILED: ${error.message}")
            runCatching { store.persistFailedInteraction(partial, failed) }
            activeSessionId = null
            return@ioLocked failure("TAP_ASSET_FAILED", "After-tap screenshot asset failed.", error)
        }

        val unchanged = beforeScene.windowContext == capture.windowContext &&
            record.frames.firstOrNull { it.frameId == beforeScene.primaryFrameId }?.assetHash == stored.asset.assetHash
        val frameId = idGenerator.nextId("frame")
        val result = if (unchanged) {
            val frame = capture.toFrame(sessionId, beforeScene.sceneId, frameId, stored)
            val snapshot = capture.toSnapshot(sessionId, beforeScene.sceneId, frameId)
            val interaction = pending.copy(
                afterSceneId = beforeScene.sceneId,
                status = RecordingInteractionStatus.UNCHANGED,
            )
            try {
                store.persistUnchangedCapture(record.session, stored.asset, frame, snapshot, interaction)
                RecordingOperationResult.Success(interaction)
            } catch (error: Throwable) {
                assetStore.deleteIfCreated(stored)
                markTapPersistenceFailure(record.session, pending, "UNCHANGED_CAPTURE_PERSIST_FAILED: ${error.message}")
                failure("UNCHANGED_CAPTURE_PERSIST_FAILED", "Unchanged after-state could not be persisted.", error)
            }
        } else {
            val sceneId = idGenerator.nextId("scene")
            val newScene = RecordingScene(
                sceneId = sceneId,
                sessionId = sessionId,
                sequence = beforeScene.sequence + 1,
                openedAtEpochMs = clock.epochMillis(),
                openedAtElapsedRealtimeNanos = clock.elapsedRealtimeNanos(),
                primaryFrameId = frameId,
                windowContext = capture.windowContext,
                status = RecordingSceneStatus.OPEN,
            )
            val closedScene = beforeScene.copy(closedAtEpochMs = newScene.openedAtEpochMs, status = RecordingSceneStatus.CLOSED)
            val frame = capture.toFrame(sessionId, sceneId, frameId, stored)
            val snapshot = capture.toSnapshot(sessionId, sceneId, frameId)
            val interaction = pending.copy(afterSceneId = sceneId, status = RecordingInteractionStatus.CHANGED)
            val updatedSession = record.session.copy(latestSceneId = sceneId)
            try {
                store.persistChangedCapture(updatedSession, closedScene, newScene, stored.asset, frame, snapshot, interaction)
                RecordingOperationResult.Success(interaction)
            } catch (error: Throwable) {
                assetStore.deleteIfCreated(stored)
                markTapPersistenceFailure(record.session, pending, "CHANGED_CAPTURE_PERSIST_FAILED: ${error.message}")
                failure("CHANGED_CAPTURE_PERSIST_FAILED", "Changed after-state could not be persisted.", error)
            }
        }
        result
    }

    suspend fun stop(): RecordingOperationResult<RecordingSession> = ioLocked {
        val sessionId = activeSessionId ?: return@ioLocked failure("NOT_RECORDING", "No recording session is active.")
        val record = store.load(sessionId) ?: return@ioLocked failure("SESSION_MISSING", "Active recording session is missing.")
        val stopping = record.session.copy(status = RecordingSessionStatus.STOPPING)
        try {
            store.updateSession(stopping)
            val stoppedAt = clock.epochMillis()
            val latestScene = record.scenes.firstOrNull { it.sceneId == record.session.latestSceneId }
                ?: return@ioLocked failure("SCENE_MISSING", "Latest recording scene is missing.")
            val completed = stopping.copy(status = RecordingSessionStatus.COMPLETED, stoppedAtEpochMs = stoppedAt)
            store.finalizeSession(
                completed,
                latestScene.copy(closedAtEpochMs = stoppedAt, status = RecordingSceneStatus.CLOSED),
            )
            activeSessionId = null
            RecordingOperationResult.Success(completed)
        } catch (error: Throwable) {
            failure("STOP_FAILED", "Recording session could not be finalized.", error)
        }
    }

    suspend fun recoverInterruptedSessions(): RecordingOperationResult<List<String>> = ioLocked {
        try {
            RecordingOperationResult.Success(store.recoverInterrupted("PROCESS_INTERRUPTED"))
        } catch (error: Throwable) {
            failure("RECOVERY_FAILED", "Interrupted sessions could not be recovered.", error)
        }
    }

    suspend fun load(sessionId: String): PersistedRecordingSession? = withContext(Dispatchers.IO) { store.load(sessionId) }

    private suspend fun persistTerminalFailure(session: RecordingSession, status: RecordingSessionStatus, message: String) {
        runCatching { store.updateSession(session.copy(status = status, failure = message)) }
    }

    private suspend fun markTapPersistenceFailure(
        session: RecordingSession,
        interaction: TapInteraction,
        message: String,
    ) {
        runCatching {
            store.persistFailedInteraction(
                session.copy(status = RecordingSessionStatus.PARTIAL, failure = message),
                interaction.copy(status = RecordingInteractionStatus.CAPTURE_FAILED),
            )
        }
        activeSessionId = null
    }

    private suspend fun <T> ioLocked(block: suspend () -> RecordingOperationResult<T>): RecordingOperationResult<T> =
        withContext(Dispatchers.IO) { writerMutex.withLock { block() } }

    private fun failure(code: String, message: String, cause: Throwable? = null) =
        RecordingOperationResult.Failure(code, message, cause)

    private fun RecorderCapture.toFrame(
        sessionId: String,
        sceneId: String,
        frameId: String,
        stored: StoredScreenshotAsset,
    ) = CaptureFrame(
        frameId = frameId,
        sessionId = sessionId,
        sceneId = sceneId,
        capturedAtEpochMs = clock.epochMillis(),
        capturedAtElapsedRealtimeNanos = clock.elapsedRealtimeNanos(),
        assetHash = stored.asset.assetHash,
        assetReference = stored.asset.assetReference,
        widthPx = widthPx,
        heightPx = heightPx,
        rotation = rotation,
        densityDpi = densityDpi,
        captureStatus = if (stored.created) RecordingCaptureStatus.CAPTURED else RecordingCaptureStatus.REUSED,
    )

    private fun RecorderCapture.toSnapshot(sessionId: String, sceneId: String, frameId: String) = A11ySnapshot(
        snapshotId = idGenerator.nextId("a11y"),
        sessionId = sessionId,
        sceneId = sceneId,
        frameId = frameId,
        capturedAtEpochMs = clock.epochMillis(),
        rootNode = a11yRoot,
        windowId = windowContext.windowId,
        packageName = windowContext.packageName,
        status = RecordingCaptureStatus.CAPTURED,
    )
}

object SystemRecorderClock : RecorderClock {
    override fun epochMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtimeNanos(): Long = android.os.SystemClock.elapsedRealtimeNanos()
}

object UuidRecorderIdGenerator : RecorderIdGenerator {
    override fun nextId(prefix: String): String = "$prefix-${UUID.randomUUID()}"
}
