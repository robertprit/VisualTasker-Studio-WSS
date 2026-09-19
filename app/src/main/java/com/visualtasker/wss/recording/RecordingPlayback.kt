package com.visualtasker.wss.recording

import com.visualtasker.wss.recording.persistence.RecorderSessionStore
import com.visualtasker.wss.recording.persistence.ScreenshotAssetStore
import java.io.File
import java.security.MessageDigest

enum class RecordingPlaybackTransitionStatus {
    UNCHANGED,
    CHANGED,
    PARTIAL,
    MISSING_BEFORE,
    MISSING_AFTER,
    FAILED,
}

enum class RecordingPlaybackDiagnosticSeverity { INFO, WARNING, ERROR }

data class RecordingPlaybackDiagnostic(
    val code: String,
    val message: String,
    val severity: RecordingPlaybackDiagnosticSeverity,
    val referenceId: String? = null,
)

data class RecordingPlaybackFrame(
    val frame: CaptureFrame,
    val asset: ScreenshotAsset?,
    val file: File?,
    val diagnostics: List<RecordingPlaybackDiagnostic> = emptyList(),
)

data class RecordingPlaybackScene(
    val scene: RecordingScene,
    val primaryFrame: RecordingPlaybackFrame?,
    val a11ySnapshot: A11ySnapshot?,
    val diagnostics: List<RecordingPlaybackDiagnostic> = emptyList(),
)

data class RecordingPlaybackInteraction(
    val tap: TapInteraction,
    val targetNode: A11yNodeSnapshot?,
    val diagnostics: List<RecordingPlaybackDiagnostic> = emptyList(),
)

data class RecordingPlaybackEntry(
    val entryId: String,
    val sequence: Long,
    val beforeScene: RecordingPlaybackScene?,
    val interaction: RecordingPlaybackInteraction,
    val afterScene: RecordingPlaybackScene?,
    val transitionStatus: RecordingPlaybackTransitionStatus,
    val occurredAtEpochMs: Long,
    val occurredAtElapsedRealtimeNanos: Long,
    val diagnostics: List<RecordingPlaybackDiagnostic> = emptyList(),
)

data class RecordingPlaybackDocument(
    val sessionId: String,
    val schemaVersion: Int,
    val sessionStatus: RecordingSessionStatus,
    val startedAtEpochMs: Long,
    val stoppedAtEpochMs: Long?,
    val durationMs: Long,
    val initialScene: RecordingPlaybackScene?,
    val scenes: List<RecordingPlaybackScene>,
    val entries: List<RecordingPlaybackEntry>,
    val diagnostics: List<RecordingPlaybackDiagnostic>,
)

data class RecordingPlaybackSessionSummary(
    val sessionId: String,
    val status: RecordingSessionStatus,
    val startedAtEpochMs: Long,
    val stoppedAtEpochMs: Long?,
    val interactionCount: Int,
)

class RecordingPlaybackRepository(
    private val store: RecorderSessionStore,
    private val assetStore: ScreenshotAssetStore,
) {
    suspend fun listSessions(): List<RecordingPlaybackSessionSummary> =
        store.listSessions().map { session ->
            RecordingPlaybackSessionSummary(
                sessionId = session.sessionId,
                status = session.status,
                startedAtEpochMs = session.startedAtEpochMs,
                stoppedAtEpochMs = session.stoppedAtEpochMs,
                interactionCount = store.load(session.sessionId)?.interactions?.size ?: 0,
            )
        }

    suspend fun load(sessionId: String): RecordingPlaybackDocument? =
        store.load(sessionId)?.let { RecordingPlaybackProjector.project(it, assetStore::resolve) }
}

object RecordingPlaybackProjector {
    fun project(
        record: PersistedRecordingSession,
        resolveAsset: (String) -> File?,
    ): RecordingPlaybackDocument {
        val documentDiagnostics = mutableListOf<RecordingPlaybackDiagnostic>()
        if (record.session.schemaVersion != RECORDING_SCHEMA_VERSION) {
            documentDiagnostics += diagnostic(
                "UNSUPPORTED_SCHEMA",
                "Schema ${record.session.schemaVersion} wird von Version $RECORDING_SCHEMA_VERSION nicht vollstaendig verstanden.",
                RecordingPlaybackDiagnosticSeverity.ERROR,
                record.session.sessionId,
            )
        }
        if (record.session.status == RecordingSessionStatus.PARTIAL) {
            documentDiagnostics += diagnostic(
                "PARTIAL_SESSION",
                record.session.failure ?: "Die Session wurde nur teilweise abgeschlossen.",
                RecordingPlaybackDiagnosticSeverity.WARNING,
                record.session.sessionId,
            )
        }
        if (record.session.stoppedAtEpochMs != null && record.session.stoppedAtEpochMs < record.session.startedAtEpochMs) {
            documentDiagnostics += diagnostic(
                "SESSION_TIME_INVALID",
                "Session-Ende liegt vor dem Session-Start.",
                RecordingPlaybackDiagnosticSeverity.ERROR,
                record.session.sessionId,
            )
        }
        validateSequences(record.scenes.map(RecordingScene::sequence), "SCENE", documentDiagnostics)
        validateSequences(record.interactions.map(TapInteraction::sequence), "INTERACTION", documentDiagnostics)
        validateMonotonicTimes(record, documentDiagnostics)

        val framesById = record.frames.associateBy(CaptureFrame::frameId)
        val assetsByHash = record.assets.associateBy(ScreenshotAsset::assetHash)
        val snapshotsByScene = record.snapshots.groupBy(A11ySnapshot::sceneId)
        val scenes = record.scenes.sortedBy(RecordingScene::sequence).map { scene ->
            val diagnostics = mutableListOf<RecordingPlaybackDiagnostic>()
            val frame = framesById[scene.primaryFrameId]
            val playbackFrame = frame?.let { source ->
                val frameDiagnostics = mutableListOf<RecordingPlaybackDiagnostic>()
                val asset = assetsByHash[source.assetHash]
                val file = resolveAsset(source.assetReference)
                if (asset == null) {
                    frameDiagnostics += diagnostic("ASSET_METADATA_MISSING", "Asset-Metadaten fehlen.", RecordingPlaybackDiagnosticSeverity.ERROR, source.assetHash)
                }
                if (file == null) {
                    frameDiagnostics += diagnostic("ASSET_FILE_MISSING", "Screenshot-Datei fehlt.", RecordingPlaybackDiagnosticSeverity.ERROR, source.assetReference)
                } else if (file.sha256() != source.assetHash) {
                    frameDiagnostics += diagnostic("ASSET_HASH_MISMATCH", "Screenshot-Pruefsumme stimmt nicht.", RecordingPlaybackDiagnosticSeverity.ERROR, source.assetHash)
                }
                RecordingPlaybackFrame(source, asset, file, frameDiagnostics)
            }
            if (frame == null) {
                diagnostics += diagnostic("PRIMARY_FRAME_MISSING", "Primaerer Frame fehlt.", RecordingPlaybackDiagnosticSeverity.ERROR, scene.primaryFrameId)
            }
            val snapshot = snapshotsByScene[scene.sceneId]
                .orEmpty()
                .firstOrNull { it.frameId == scene.primaryFrameId }
                ?: snapshotsByScene[scene.sceneId].orEmpty().maxByOrNull(A11ySnapshot::capturedAtEpochMs)
            if (snapshot == null) {
                diagnostics += diagnostic("A11Y_SNAPSHOT_MISSING", "A11Y-Snapshot wurde nicht aufgezeichnet.", RecordingPlaybackDiagnosticSeverity.WARNING, scene.sceneId)
            }
            RecordingPlaybackScene(scene, playbackFrame, snapshot, diagnostics + playbackFrame.orEmptyDiagnostics())
        }
        val scenesById = scenes.associateBy { it.scene.sceneId }
        val entries = record.interactions.sortedBy(TapInteraction::sequence).map { tap ->
            val diagnostics = mutableListOf<RecordingPlaybackDiagnostic>()
            val before = scenesById[tap.beforeSceneId]
            val after = tap.afterSceneId?.let(scenesById::get)
            if (before == null) diagnostics += diagnostic("BEFORE_SCENE_MISSING", "Vorher-Szene fehlt.", RecordingPlaybackDiagnosticSeverity.ERROR, tap.beforeSceneId)
            if (tap.afterSceneId == null) diagnostics += diagnostic("AFTER_SCENE_UNSET", "Nachher-Szene wurde nicht erfasst.", RecordingPlaybackDiagnosticSeverity.WARNING, tap.interactionId)
            if (tap.afterSceneId != null && after == null) diagnostics += diagnostic("AFTER_SCENE_MISSING", "Nachher-Szene fehlt.", RecordingPlaybackDiagnosticSeverity.ERROR, tap.afterSceneId)
            val target = tap.targetA11yNodeId?.let { nodeId -> before?.a11ySnapshot?.rootNode?.findNode(nodeId) }
            if (tap.targetA11yNodeId != null && target == null) {
                diagnostics += diagnostic("TARGET_NODE_MISSING", "Gespeicherter A11Y-Zielknoten ist nicht vorhanden.", RecordingPlaybackDiagnosticSeverity.WARNING, tap.targetA11yNodeId)
            }
            if (tap.targetA11yNodeId == null) {
                diagnostics += diagnostic("UNGROUNDED_TAP", "Tap besitzt keinen zugeordneten A11Y-Zielknoten.", RecordingPlaybackDiagnosticSeverity.INFO, tap.interactionId)
            }
            val status = when {
                before == null -> RecordingPlaybackTransitionStatus.MISSING_BEFORE
                tap.status == RecordingInteractionStatus.CAPTURE_FAILED -> RecordingPlaybackTransitionStatus.FAILED
                tap.afterSceneId == null -> RecordingPlaybackTransitionStatus.MISSING_AFTER
                after == null -> RecordingPlaybackTransitionStatus.MISSING_AFTER
                tap.status == RecordingInteractionStatus.UNCHANGED || tap.beforeSceneId == tap.afterSceneId -> RecordingPlaybackTransitionStatus.UNCHANGED
                tap.status == RecordingInteractionStatus.CHANGED -> RecordingPlaybackTransitionStatus.CHANGED
                else -> RecordingPlaybackTransitionStatus.PARTIAL
            }
            RecordingPlaybackEntry(
                entryId = "entry:${tap.interactionId}",
                sequence = tap.sequence,
                beforeScene = before,
                interaction = RecordingPlaybackInteraction(tap, target, diagnostics),
                afterScene = after,
                transitionStatus = status,
                occurredAtEpochMs = tap.occurredAtEpochMs,
                occurredAtElapsedRealtimeNanos = tap.occurredAtElapsedRealtimeNanos,
                diagnostics = diagnostics,
            )
        }
        val initialScene = record.session.initialSceneId?.let(scenesById::get)
        if (initialScene == null) {
            documentDiagnostics += diagnostic("INITIAL_SCENE_MISSING", "Initiale Szene fehlt.", RecordingPlaybackDiagnosticSeverity.ERROR, record.session.initialSceneId)
        }
        documentDiagnostics += scenes.flatMap(RecordingPlaybackScene::diagnostics)
        documentDiagnostics += entries.flatMap(RecordingPlaybackEntry::diagnostics)
        return RecordingPlaybackDocument(
            sessionId = record.session.sessionId,
            schemaVersion = record.session.schemaVersion,
            sessionStatus = record.session.status,
            startedAtEpochMs = record.session.startedAtEpochMs,
            stoppedAtEpochMs = record.session.stoppedAtEpochMs,
            durationMs = ((record.session.stoppedAtEpochMs ?: record.session.startedAtEpochMs) - record.session.startedAtEpochMs).coerceAtLeast(0L),
            initialScene = initialScene,
            scenes = scenes,
            entries = entries,
            diagnostics = documentDiagnostics.distinctBy { listOf(it.code, it.referenceId, it.message) },
        )
    }

    private fun validateSequences(
        values: List<Long>,
        prefix: String,
        diagnostics: MutableList<RecordingPlaybackDiagnostic>,
    ) {
        values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { sequence ->
            diagnostics += diagnostic("${prefix}_SEQUENCE_DUPLICATE", "Sequenz $sequence ist mehrfach vorhanden.", RecordingPlaybackDiagnosticSeverity.ERROR)
        }
        values.sorted().zipWithNext().filter { (first, second) -> second > first + 1 }.forEach { (first, second) ->
            diagnostics += diagnostic("${prefix}_SEQUENCE_GAP", "Sequenzluecke zwischen $first und $second.", RecordingPlaybackDiagnosticSeverity.WARNING)
        }
    }

    private fun validateMonotonicTimes(
        record: PersistedRecordingSession,
        diagnostics: MutableList<RecordingPlaybackDiagnostic>,
    ) {
        record.scenes.sortedBy(RecordingScene::sequence).zipWithNext().forEach { (first, second) ->
            if (second.openedAtElapsedRealtimeNanos < first.openedAtElapsedRealtimeNanos) {
                diagnostics += diagnostic(
                    "SCENE_MONOTONIC_TIME_INVALID",
                    "Scene ${second.sceneId} liegt monoton vor ${first.sceneId}.",
                    RecordingPlaybackDiagnosticSeverity.ERROR,
                    second.sceneId,
                )
            }
        }
        record.interactions.sortedBy(TapInteraction::sequence).zipWithNext().forEach { (first, second) ->
            if (second.occurredAtElapsedRealtimeNanos < first.occurredAtElapsedRealtimeNanos) {
                diagnostics += diagnostic(
                    "INTERACTION_MONOTONIC_TIME_INVALID",
                    "Interaktion ${second.interactionId} liegt monoton vor ${first.interactionId}.",
                    RecordingPlaybackDiagnosticSeverity.ERROR,
                    second.interactionId,
                )
            }
        }
        record.interactions.forEach { interaction ->
            val before = record.scenes.firstOrNull { it.sceneId == interaction.beforeSceneId }
            if (before != null && interaction.occurredAtElapsedRealtimeNanos < before.openedAtElapsedRealtimeNanos) {
                diagnostics += diagnostic(
                    "INTERACTION_BEFORE_SCENE_TIME_INVALID",
                    "Interaktion ${interaction.interactionId} liegt vor ihrer Vorher-Szene.",
                    RecordingPlaybackDiagnosticSeverity.ERROR,
                    interaction.interactionId,
                )
            }
        }
    }

    private fun diagnostic(code: String, message: String, severity: RecordingPlaybackDiagnosticSeverity, reference: String? = null) =
        RecordingPlaybackDiagnostic(code, message, severity, reference)
}

private fun RecordingPlaybackFrame?.orEmptyDiagnostics(): List<RecordingPlaybackDiagnostic> = this?.diagnostics.orEmpty()

private fun A11yNodeSnapshot.findNode(id: String): A11yNodeSnapshot? =
    if (stableSnapshotNodeId == id) this else children.firstNotNullOfOrNull { it.findNode(id) }

private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(readBytes())
    .joinToString("") { "%02x".format(it) }
