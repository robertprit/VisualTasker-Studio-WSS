package com.visualtasker.wss.recording

import com.visualtasker.wss.recording.ScreenshotAssetStoreTest.Companion.PNG_A
import com.visualtasker.wss.recording.ScreenshotAssetStoreTest.Companion.PNG_B
import com.visualtasker.wss.recording.persistence.ScreenshotAssetStore
import com.visualtasker.wss.workspace.model.RecordingPlaybackSceneTreeProjector
import com.visualtasker.wss.workspace.model.toRecorderSteps
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingPlaybackTest {
    @Test
    fun projectsChangedTransitionWithStableReferencesAndEvidence() {
        val fixture = fixture(changed = true)

        val document = RecordingPlaybackProjector.project(fixture.record, fixture.assetStore::resolve)

        assertEquals(1, document.entries.size)
        val entry = document.entries.single()
        assertEquals("scene-1", entry.beforeScene?.scene?.sceneId)
        assertEquals("scene-2", entry.afterScene?.scene?.sceneId)
        assertEquals(RecordingPlaybackTransitionStatus.CHANGED, entry.transitionStatus)
        assertEquals("target", entry.interaction.targetNode?.stableSnapshotNodeId)
        assertTrue(entry.beforeScene?.primaryFrame?.file?.isFile == true)
        assertTrue(document.diagnostics.none { it.severity == RecordingPlaybackDiagnosticSeverity.ERROR })
        assertEquals(
            setOf(
                RecordingEvidenceKind.INPUT,
                RecordingEvidenceKind.WINDOW,
                RecordingEvidenceKind.ACCESSIBILITY,
                RecordingEvidenceKind.VISUAL,
            ),
            entry.evidence.map(RecordingEvidenceRef::kind).toSet(),
        )
        assertTrue(entry.evidence.all { it.stepId == entry.entryId })
        assertTrue(
            entry.evidence
                .flatMap(RecordingEvidenceRef::resources)
                .any { it.kind == RecordingResourceKind.SCREENSHOT_ASSET },
        )
        assertTrue(document.integrityReport?.isValid == true)
        assertEquals(entry.entryId, document.toRecorderSteps().single().integrityReport?.stepId)
    }

    @Test
    fun unchangedTransitionKeepsSingleSceneAndInteraction() {
        val fixture = fixture(changed = false)
        val document = RecordingPlaybackProjector.project(fixture.record, fixture.assetStore::resolve)

        assertEquals(1, document.scenes.size)
        assertEquals(1, document.entries.size)
        assertEquals(RecordingPlaybackTransitionStatus.UNCHANGED, document.entries.single().transitionStatus)
        assertEquals(document.entries.single().beforeScene, document.entries.single().afterScene)
    }

    @Test
    fun partialTransitionAndMissingAssetRemainInspectable() {
        val fixture = fixture(changed = true)
        fixture.record.frames.first().let { fixture.assetStore.resolve(it.assetReference)?.delete() }
        val partial = fixture.record.copy(
            session = fixture.record.session.copy(status = RecordingSessionStatus.PARTIAL, failure = "PROCESS_INTERRUPTED"),
            interactions = fixture.record.interactions.map { it.copy(afterSceneId = null, status = RecordingInteractionStatus.CAPTURE_FAILED) },
        )

        val document = RecordingPlaybackProjector.project(partial, fixture.assetStore::resolve)

        assertEquals(RecordingPlaybackTransitionStatus.FAILED, document.entries.single().transitionStatus)
        assertTrue(document.diagnostics.any { it.code == "ASSET_FILE_MISSING" })
        assertTrue(document.diagnostics.any { it.code == "PARTIAL_SESSION" })
        assertNotNull(document.entries.single().beforeScene?.a11ySnapshot)
    }

    @Test
    fun controllerNavigationIsDeterministic() {
        val fixture = fixture(changed = true)
        val document = RecordingPlaybackProjector.project(fixture.record, fixture.assetStore::resolve)
        val controller = RecordingPlaybackController(RecordingPlaybackPreviewPolicy(10, 20, 10))
        controller.load(document)

        controller.play()
        controller.advanceBy(20)
        assertEquals(RecordingPlaybackPhase.AFTER, controller.state.value.phase)
        controller.pause()
        assertEquals(RecordingPlaybackStatus.PAUSED, controller.state.value.status)
        controller.previous()
        assertEquals(RecordingPlaybackPhase.BEFORE, controller.state.value.phase)
        controller.next()
        assertEquals(RecordingPlaybackPhase.AFTER, controller.state.value.phase)
        controller.next()
        assertEquals(RecordingPlaybackStatus.COMPLETED, controller.state.value.status)
        controller.next()
        assertEquals(RecordingPlaybackStatus.COMPLETED, controller.state.value.status)
        controller.restart()
        assertEquals(0, controller.state.value.selectedEntryIndex)
        assertEquals(RecordingPlaybackPhase.BEFORE, controller.state.value.phase)
        controller.setPlaybackSpeed(2f)
        assertEquals(2f, controller.state.value.speed)
    }

    @Test
    fun screenshotTreeSelectionAndRotationUseSameCoordinates() {
        val fixture = fixture(changed = true)
        val document = RecordingPlaybackProjector.project(fixture.record, fixture.assetStore::resolve)
        val controller = RecordingPlaybackController().apply { load(document) }
        val state = controller.state.value
        val tree = RecordingPlaybackSceneTreeProjector.project("panel-scene", state)!!
        val target = state.selectedScene!!.a11ySnapshot!!.rootNode.findPlaybackNode("target")
        val transform = RecordingPlaybackViewportTransform(100, 200, 200f, 100f, 90)
        val mapped = transform.mapPoint(30f, 50f)
        val restored = transform.unmapPoint(mapped.x, mapped.y)

        assertNotNull(tree.find("a11y:target"))
        assertEquals("target", target?.stableSnapshotNodeId)
        assertEquals(30f, restored?.x ?: -1f, 0.01f)
        assertEquals(50f, restored?.y ?: -1f, 0.01f)
        assertEquals("target", state.selectedScene!!.a11ySnapshot!!.rootNode.deepestNodeAt(30f, 50f)?.stableSnapshotNodeId)
    }

    @Test
    fun blankAccessibilityLabelsRemainInspectable() {
        val fixture = fixture(changed = true)
        val blankRoot = fixture.record.snapshots.first().rootNode.copy(
            className = " ",
            viewIdResourceName = null,
            text = " ",
            contentDescription = "",
        )
        val damaged = fixture.record.copy(
            snapshots = fixture.record.snapshots.mapIndexed { index, snapshot ->
                if (index == 0) snapshot.copy(rootNode = blankRoot) else snapshot
            },
        )
        val document = RecordingPlaybackProjector.project(damaged, fixture.assetStore::resolve)
        val controller = RecordingPlaybackController().apply { load(document) }

        val tree = RecordingPlaybackSceneTreeProjector.project("panel-scene", controller.state.value)!!
        val node = tree.find("a11y:root")

        assertEquals("Element root", node?.label)
        assertEquals("Element root", node?.payload?.label)
    }

    @Test
    fun sceneChangeClearsSelectionThatDoesNotExistInAfterScene() {
        val fixture = fixture(changed = true)
        val withoutTarget = fixture.record.snapshots.last().copy(
            rootNode = fixture.record.snapshots.last().rootNode.copy(children = emptyList()),
        )
        val document = RecordingPlaybackProjector.project(
            fixture.record.copy(snapshots = fixture.record.snapshots.dropLast(1) + withoutTarget),
            fixture.assetStore::resolve,
        )
        val controller = RecordingPlaybackController().apply { load(document) }

        assertEquals("target", controller.state.value.selectedA11yNodeId)
        controller.next()

        assertEquals(RecordingPlaybackPhase.AFTER, controller.state.value.phase)
        assertEquals(null, controller.state.value.selectedA11yNodeId)
    }

    @Test
    fun damagedSchemaSequenceAndTimesProduceExplicitDiagnostics() {
        val fixture = fixture(changed = true)
        val damaged = fixture.record.copy(
            session = fixture.record.session.copy(schemaVersion = 99, stoppedAtEpochMs = 900L),
            scenes = fixture.record.scenes.mapIndexed { index, scene ->
                scene.copy(
                    sequence = 1L,
                    openedAtElapsedRealtimeNanos = if (index == 0) 2_000_000_000L else 1_000_000_000L,
                )
            },
        )

        val document = RecordingPlaybackProjector.project(damaged, fixture.assetStore::resolve)
        val codes = document.diagnostics.map(RecordingPlaybackDiagnostic::code).toSet()
        val controller = RecordingPlaybackController().apply { load(document) }
        val tree = RecordingPlaybackSceneTreeProjector.project("panel-scene", controller.state.value)!!

        assertTrue("UNSUPPORTED_SCHEMA" in codes)
        assertTrue("SESSION_TIME_INVALID" in codes)
        assertTrue("SCENE_SEQUENCE_DUPLICATE" in codes)
        assertTrue("SCENE_MONOTONIC_TIME_INVALID" in codes)
        assertTrue("INTERACTION_BEFORE_SCENE_TIME_INVALID" in codes)
        assertTrue(tree.root.flatten().all { it.payload.id.matches(Regex("[a-z0-9][a-z0-9._:-]*")) })
    }

    @Test
    fun hashMismatchAndMissingA11yStayInspectable() {
        val fixture = fixture(changed = true)
        val firstFrame = fixture.record.frames.first()
        fixture.assetStore.resolve(firstFrame.assetReference)!!.writeBytes(PNG_B)
        val damaged = fixture.record.copy(
            snapshots = fixture.record.snapshots.drop(1),
            interactions = fixture.record.interactions.map { interaction ->
                interaction.copy(
                    payload = (interaction.payload as RecordingInteractionPayload.Tap).copy(
                        targetReference = "missing-target",
                    ),
                )
            },
        )

        val document = RecordingPlaybackProjector.project(damaged, fixture.assetStore::resolve)
        val codes = document.diagnostics.map(RecordingPlaybackDiagnostic::code).toSet()

        assertTrue("ASSET_HASH_MISMATCH" in codes)
        assertTrue("A11Y_SNAPSHOT_MISSING" in codes)
        assertTrue("TARGET_NODE_MISSING" in codes)
        assertNotNull(document.entries.single().beforeScene)
    }

    @Test
    fun playbackProjectionAndNavigationAreReadOnly() {
        val fixture = fixture(changed = true)
        val recordBefore = fixture.record.copy()
        val filesBefore = fixture.assetStoreRootSnapshot()
        val document = RecordingPlaybackProjector.project(fixture.record, fixture.assetStore::resolve)
        val controller = RecordingPlaybackController().apply {
            load(document)
            next()
            previous()
            seekToEntry(0)
            restart()
        }

        assertEquals(recordBefore, fixture.record)
        assertEquals(filesBefore, fixture.assetStoreRootSnapshot())
        assertEquals("session-1", controller.state.value.document?.sessionId)
    }

    @Test
    fun interruptedRecordingHasDistinctDiagnostic() {
        val fixture = fixture(changed = true)
        val interrupted = fixture.record.copy(
            session = fixture.record.session.copy(
                status = RecordingSessionStatus.INTERRUPTED,
                failure = "PROCESS_INTERRUPTED",
            ),
        )

        val document = RecordingPlaybackProjector.project(interrupted, fixture.assetStore::resolve)

        assertTrue(document.diagnostics.any { it.code == "INTERRUPTED_SESSION" })
        assertTrue(document.diagnostics.none { it.code == "PARTIAL_SESSION" })
    }

    @Test
    fun replayBookmarkRestoresPositionButNeverAutoResumes() {
        val fixture = fixture(changed = true)
        val document = RecordingPlaybackProjector.project(fixture.record, fixture.assetStore::resolve)
        val source = RecordingPlaybackController().apply {
            load(document)
            play()
            advanceBy(100L)
            setPlaybackSpeed(2f)
        }
        val encoded = RecordingReplayBookmarkCodec.encode(source.bookmark()!!)
        val restoredBookmark = RecordingReplayBookmarkCodec.decode(encoded)!!
        val restored = RecordingPlaybackController().apply { load(document, restoredBookmark) }

        assertEquals(RecordingPlaybackStatus.PAUSED, restored.state.value.status)
        assertEquals(restoredBookmark.entryId, restored.state.value.selectedEntry?.entryId)
        assertEquals(restoredBookmark.positionMs, restored.state.value.phaseElapsedMs)
        assertEquals(2f, restored.state.value.speed)
    }

    private fun com.visualtasker.wss.workspace.model.SceneInspectorNode.flatten(): List<com.visualtasker.wss.workspace.model.SceneInspectorNode> =
        listOf(this) + children.flatMap { it.flatten() }

    private fun fixture(changed: Boolean): PlaybackFixture {
        val root = Files.createTempDirectory("recording-playback").toFile()
        val assetStore = ScreenshotAssetStore(root) { 1_000L }
        val firstAsset = assetStore.putPng(PNG_A).asset
        val secondAsset = if (changed) assetStore.putPng(PNG_B).asset else firstAsset
        val scene1 = scene("scene-1", 1, "frame-1", 1_000L, 1_000_000_000L)
        val scene2 = scene("scene-2", 2, "frame-2", 1_200L, 1_200_000_000L)
        val frame1 = frame("frame-1", scene1.sceneId, firstAsset, 1_000_000_000L)
        val frame2 = frame("frame-2", if (changed) scene2.sceneId else scene1.sceneId, secondAsset, 1_200_000_000L)
        val snapshot1 = snapshot("a11y-1", scene1.sceneId, frame1.frameId)
        val snapshot2 = snapshot("a11y-2", if (changed) scene2.sceneId else scene1.sceneId, frame2.frameId)
        val interaction = recordingTapInteraction(
            interactionId = "tap-1",
            rawEventId = "raw-1",
            sessionId = "session-1",
            sequence = 1,
            occurredAtEpochMs = 1_100L,
            occurredAtElapsedRealtimeNanos = 1_100_000_000L,
            xPx = 30,
            yPx = 50,
            beforeSceneId = scene1.sceneId,
            afterSceneId = if (changed) scene2.sceneId else scene1.sceneId,
            targetA11yNodeId = "target",
            status = if (changed) RecordingInteractionStatus.CHANGED else RecordingInteractionStatus.UNCHANGED,
        )
        val session = RecordingSession(
            sessionId = "session-1",
            status = RecordingSessionStatus.COMPLETED,
            startedAtEpochMs = 1_000L,
            startedAtElapsedRealtimeNanos = 1_000_000_000L,
            stoppedAtEpochMs = 1_300L,
            createdBy = "test",
            recordingPolicySnapshot = RecordingPolicySnapshot(),
            initialSceneId = scene1.sceneId,
            latestSceneId = if (changed) scene2.sceneId else scene1.sceneId,
        )
        return PlaybackFixture(
            PersistedRecordingSession(
                session = session,
                scenes = if (changed) listOf(scene1.copy(status = RecordingSceneStatus.CLOSED), scene2) else listOf(scene1),
                frames = listOf(frame1, frame2),
                assets = listOf(firstAsset, secondAsset).distinctBy(ScreenshotAsset::assetHash),
                snapshots = listOf(snapshot1, snapshot2),
                rawEvents = listOf(
                    RawRecordingEvent(
                        rawEventId = "raw-1",
                        sessionId = session.sessionId,
                        sequence = 1L,
                        occurredAtEpochMs = interaction.occurredAtEpochMs,
                        occurredAtElapsedRealtimeNanos = interaction.occurredAtElapsedRealtimeNanos,
                        kind = "tap",
                        payload = mapOf("x" to "30", "y" to "50"),
                    ),
                ),
                interactions = listOf(interaction),
            ),
            assetStore,
            root,
        )
    }

    private fun scene(id: String, sequence: Long, frameId: String, epoch: Long, elapsed: Long) = RecordingScene(
        sceneId = id, sessionId = "session-1", sequence = sequence,
        openedAtEpochMs = epoch, openedAtElapsedRealtimeNanos = elapsed,
        primaryFrameId = frameId,
        windowContext = RecordingWindowContext("com.example", "Activity$sequence", "window-$sequence"),
        status = RecordingSceneStatus.OPEN,
    )

    private fun frame(id: String, sceneId: String, asset: ScreenshotAsset, elapsed: Long) = CaptureFrame(
        frameId = id, sessionId = "session-1", sceneId = sceneId,
        capturedAtEpochMs = elapsed / 1_000_000L, capturedAtElapsedRealtimeNanos = elapsed,
        assetHash = asset.assetHash, assetReference = asset.assetReference,
        widthPx = 100, heightPx = 200, rotation = 0, densityDpi = 480,
        captureStatus = RecordingCaptureStatus.CAPTURED,
    )

    private fun snapshot(id: String, sceneId: String, frameId: String) = A11ySnapshot(
        snapshotId = id, sessionId = "session-1", sceneId = sceneId, frameId = frameId,
        capturedAtEpochMs = 1_000L, rootNode = rootNode(), windowId = "window", packageName = "com.example",
        status = RecordingCaptureStatus.CAPTURED,
    )

    private fun rootNode() = A11yNodeSnapshot(
        stableSnapshotNodeId = "root", parentNodeId = null, childOrder = 0, className = "FrameLayout",
        viewIdResourceName = null, text = null, contentDescription = null,
        left = 0, top = 0, right = 100, bottom = 200,
        clickable = false, longClickable = false, scrollable = false, editable = false,
        enabled = true, selected = false, checked = false, visibleToUser = true, actions = emptyList(),
        children = listOf(
            A11yNodeSnapshot(
                stableSnapshotNodeId = "target", parentNodeId = "root", childOrder = 0, className = "Button",
                viewIdResourceName = "button", text = "Open", contentDescription = null,
                left = 10, top = 20, right = 60, bottom = 80,
                clickable = true, longClickable = false, scrollable = false, editable = false,
                enabled = true, selected = false, checked = false, visibleToUser = true, actions = listOf("ACTION_CLICK"),
            )
        ),
    )
}

private data class PlaybackFixture(
    val record: PersistedRecordingSession,
    val assetStore: ScreenshotAssetStore,
    val assetRoot: java.io.File,
) {
    fun assetStoreRootSnapshot(): List<Pair<String, Int>> =
        assetRoot.walkTopDown()
            .filter(java.io.File::isFile)
            .map { it.relativeTo(assetRoot).path to it.readBytes().contentHashCode() }
            .sortedBy(Pair<String, Int>::first)
            .toList()
}
