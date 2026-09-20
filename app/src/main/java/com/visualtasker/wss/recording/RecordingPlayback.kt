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
    val interaction: RecordingInteraction,
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
    val evidence: List<RecordingEvidenceRef> = emptyList(),
    val diagnostics: List<RecordingPlaybackDiagnostic> = emptyList(),
)

data class RecordingPlaybackDocument(
    val sessionId: String,
    val resumedFromSessionId: String? = null,
    val schemaVersion: Int,
    val sessionStatus: RecordingSessionStatus,
    val startedAtEpochMs: Long,
    val stoppedAtEpochMs: Long?,
    val durationMs: Long,
    val initialScene: RecordingPlaybackScene?,
    val scenes: List<RecordingPlaybackScene>,
    val entries: List<RecordingPlaybackEntry>,
    val diagnostics: List<RecordingPlaybackDiagnostic>,
    val integrityReport: RecordingIntegrityReport? = null,
)

data class RecordingPlaybackSessionSummary(
    val sessionId: String,
    val resumedFromSessionId: String? = null,
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
                resumedFromSessionId = session.resumedFromSessionId,
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
        if (record.session.status == RecordingSessionStatus.INTERRUPTED) {
            documentDiagnostics += diagnostic(
                "INTERRUPTED_SESSION",
                record.session.failure ?: "Die Session wurde durch einen Prozessabbruch unterbrochen.",
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
        validateSequences(record.interactions.map(RecordingInteraction::sequence), "INTERACTION", documentDiagnostics)
        validateMonotonicTimes(record, documentDiagnostics)

        val framesById = record.frames.associateBy(CaptureFrame::frameId)
        val assetsByHash = record.assets.associateBy(ScreenshotAsset::assetHash)
        val snapshotsByScene = record.snapshots.groupBy(A11ySnapshot::sceneId)
        val scenes = record.scenes.sortedWith(RecordingCanonicalOrder.scenes).map { scene ->
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
        val rawEventsById = record.rawEvents.associateBy(RawRecordingEvent::rawEventId)
        val entries = record.interactions.sortedWith(RecordingCanonicalOrder.interactions).map { interaction ->
            val diagnostics = mutableListOf<RecordingPlaybackDiagnostic>()
            val before = interaction.beforeSceneId?.let(scenesById::get)
            val after = interaction.afterSceneId?.let(scenesById::get)
            if (interaction.beforeSceneId != null && before == null) diagnostics += diagnostic("BEFORE_SCENE_MISSING", "Vorher-Szene fehlt.", RecordingPlaybackDiagnosticSeverity.ERROR, interaction.beforeSceneId)
            if (interaction.afterSceneId == null && interaction.beforeSceneId != null) diagnostics += diagnostic("AFTER_SCENE_UNSET", "Nachher-Szene wurde nicht erfasst.", RecordingPlaybackDiagnosticSeverity.WARNING, interaction.interactionId)
            if (interaction.afterSceneId != null && after == null) diagnostics += diagnostic("AFTER_SCENE_MISSING", "Nachher-Szene fehlt.", RecordingPlaybackDiagnosticSeverity.ERROR, interaction.afterSceneId)
            val tap = interaction.payload as? RecordingInteractionPayload.Tap
            val targetReference = tap?.targetReference
            val target = targetReference?.let { nodeId -> before?.a11ySnapshot?.rootNode?.findNode(nodeId) }
            if (targetReference != null && target == null) {
                diagnostics += diagnostic("TARGET_NODE_MISSING", "Gespeicherter A11Y-Zielknoten ist nicht vorhanden.", RecordingPlaybackDiagnosticSeverity.WARNING, targetReference)
            }
            if (tap != null && targetReference == null) {
                diagnostics += diagnostic("UNGROUNDED_TAP", "Tap besitzt keinen zugeordneten A11Y-Zielknoten.", RecordingPlaybackDiagnosticSeverity.INFO, interaction.interactionId)
            }
            val status = when {
                interaction.status == RecordingInteractionStatus.CAPTURE_FAILED -> RecordingPlaybackTransitionStatus.FAILED
                interaction.beforeSceneId == null && interaction.afterSceneId == null -> RecordingPlaybackTransitionStatus.UNCHANGED
                before == null -> RecordingPlaybackTransitionStatus.MISSING_BEFORE
                interaction.afterSceneId == null -> RecordingPlaybackTransitionStatus.MISSING_AFTER
                after == null -> RecordingPlaybackTransitionStatus.MISSING_AFTER
                interaction.status == RecordingInteractionStatus.UNCHANGED || interaction.beforeSceneId == interaction.afterSceneId -> RecordingPlaybackTransitionStatus.UNCHANGED
                interaction.status == RecordingInteractionStatus.CHANGED -> RecordingPlaybackTransitionStatus.CHANGED
                else -> RecordingPlaybackTransitionStatus.PARTIAL
            }
            RecordingPlaybackEntry(
                entryId = "entry:${interaction.interactionId}",
                sequence = interaction.sequence,
                beforeScene = before,
                interaction = RecordingPlaybackInteraction(interaction, target, diagnostics),
                afterScene = after,
                transitionStatus = status,
                occurredAtEpochMs = interaction.occurredAtEpochMs,
                occurredAtElapsedRealtimeNanos = interaction.occurredAtElapsedRealtimeNanos,
                evidence = evidenceFor(
                    sessionId = record.session.sessionId,
                    interaction = interaction,
                    rawEvents = interaction.rawEventIds.mapNotNull(rawEventsById::get),
                    before = before,
                    after = after,
                ),
                diagnostics = diagnostics,
            )
        }
        val initialScene = record.session.initialSceneId?.let(scenesById::get)
        if (initialScene == null) {
            documentDiagnostics += diagnostic("INITIAL_SCENE_MISSING", "Initiale Szene fehlt.", RecordingPlaybackDiagnosticSeverity.ERROR, record.session.initialSceneId)
        }
        documentDiagnostics += scenes.flatMap(RecordingPlaybackScene::diagnostics)
        documentDiagnostics += entries.flatMap(RecordingPlaybackEntry::diagnostics)
        val document = RecordingPlaybackDocument(
            sessionId = record.session.sessionId,
            resumedFromSessionId = record.session.resumedFromSessionId,
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
        return document.copy(
            integrityReport = RecordingIntegrityValidator.validate(record, document.toIntegritySnapshot()),
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
        record.scenes
            .sortedWith(compareBy<RecordingScene> { it.sequence }.thenBy { it.sceneId })
            .zipWithNext()
            .forEach { (first, second) ->
            if (second.openedAtElapsedRealtimeNanos < first.openedAtElapsedRealtimeNanos) {
                diagnostics += diagnostic(
                    "SCENE_MONOTONIC_TIME_INVALID",
                    "Scene ${second.sceneId} liegt monoton vor ${first.sceneId}.",
                    RecordingPlaybackDiagnosticSeverity.ERROR,
                    second.sceneId,
                )
            }
        }
        record.interactions
            .sortedWith(compareBy<RecordingInteraction> { it.sequence }.thenBy { it.interactionId })
            .zipWithNext()
            .forEach { (first, second) ->
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
            val before = interaction.beforeSceneId?.let { beforeId -> record.scenes.firstOrNull { it.sceneId == beforeId } }
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

    private fun evidenceFor(
        sessionId: String,
        interaction: RecordingInteraction,
        rawEvents: List<RawRecordingEvent>,
        before: RecordingPlaybackScene?,
        after: RecordingPlaybackScene?,
    ): List<RecordingEvidenceRef> {
        val stepId = "entry:${interaction.interactionId}"
        fun evidence(
            suffix: String,
            kind: RecordingEvidenceKind,
            resources: List<RecordingResourceRef>,
            properties: Map<String, String> = emptyMap(),
        ) = RecordingEvidenceRef(
            evidenceId = "evidence:${interaction.interactionId}:$suffix",
            sessionId = sessionId,
            stepId = stepId,
            sequence = interaction.sequence,
            occurredAtEpochMs = interaction.occurredAtEpochMs,
            occurredAtElapsedRealtimeNanos = interaction.occurredAtElapsedRealtimeNanos,
            kind = kind,
            resources = resources.distinct(),
            properties = properties,
        )

        val result = mutableListOf<RecordingEvidenceRef>()
        result += evidence(
            suffix = "input",
            kind = RecordingEvidenceKind.INPUT,
            resources = listOfNotNull(
                *rawEvents.map { RecordingResourceRef(RecordingResourceKind.RAW_EVENT, it.rawEventId) }.toTypedArray(),
            ),
            properties = rawEvents.flatMap { it.payload.entries }.associate { it.toPair() } +
                mapOf("interactionId" to interaction.interactionId, "interactionType" to interaction.type.name),
        )
        val scenes = listOfNotNull(before, after).distinctBy { it.scene.sceneId }
        if (scenes.isNotEmpty()) {
            result += evidence(
                suffix = "window",
                kind = RecordingEvidenceKind.WINDOW,
                resources = scenes.map { RecordingResourceRef(RecordingResourceKind.SCENE, it.scene.sceneId) },
            )
        }
        val snapshots = scenes.mapNotNull(RecordingPlaybackScene::a11ySnapshot)
            .distinctBy(A11ySnapshot::snapshotId)
        if (snapshots.isNotEmpty()) {
            result += evidence(
                suffix = "accessibility",
                kind = RecordingEvidenceKind.ACCESSIBILITY,
                resources = snapshots.map { RecordingResourceRef(RecordingResourceKind.A11Y_SNAPSHOT, it.snapshotId) },
            )
        }
        val frames = scenes.mapNotNull(RecordingPlaybackScene::primaryFrame)
            .distinctBy { it.frame.frameId }
        val payloadResource = (interaction.payload as? RecordingInteractionPayload.Screenshot)?.resource
        val visualResources = frames.flatMap { frame ->
            listOf(
                RecordingResourceRef(RecordingResourceKind.CAPTURE_FRAME, frame.frame.frameId),
                RecordingResourceRef(RecordingResourceKind.SCREENSHOT_ASSET, frame.frame.assetHash),
            )
        } + listOfNotNull(payloadResource)
        if (visualResources.isNotEmpty()) {
            result += evidence(
                suffix = "visual",
                kind = RecordingEvidenceKind.VISUAL,
                resources = visualResources,
            )
        }
        return result
    }
}

private fun RecordingPlaybackFrame?.orEmptyDiagnostics(): List<RecordingPlaybackDiagnostic> = this?.diagnostics.orEmpty()

private fun A11yNodeSnapshot.findNode(id: String): A11yNodeSnapshot? =
    if (stableSnapshotNodeId == id) this else children.firstNotNullOfOrNull { it.findNode(id) }

private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(readBytes())
    .joinToString("") { "%02x".format(it) }
