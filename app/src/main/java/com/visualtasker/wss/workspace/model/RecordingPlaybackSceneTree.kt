package com.visualtasker.wss.workspace.model

import com.visualtasker.wss.recording.A11yNodeSnapshot
import com.visualtasker.wss.recording.RecordingPlaybackDiagnostic
import com.visualtasker.wss.recording.RecordingPlaybackEntry
import com.visualtasker.wss.recording.RecordingPlaybackPhase
import com.visualtasker.wss.recording.RecordingPlaybackScene
import com.visualtasker.wss.recording.RecordingPlaybackState

object RecordingPlaybackSceneTreeProjector {
    fun project(panelId: String, state: RecordingPlaybackState): SceneInspectorTree? {
        val scene = state.selectedScene ?: return null
        val entry = state.selectedEntry
        val sceneId = scene.scene.sceneId
        val children = buildList {
            add(windowNode(panelId, scene))
            entry?.let { add(interactionNode(panelId, it, state.phase)) }
            add(frameNode(panelId, scene))
            add(a11yNode(panelId, scene))
            add(providerStatusNode(panelId, sceneId, "OCR", ObservationProvider.Ocr))
            add(providerStatusNode(panelId, sceneId, "OCV", ObservationProvider.OpenCv))
            add(providerStatusNode(panelId, sceneId, "DOM", ObservationProvider.Dom))
            add(providerStatusNode(panelId, sceneId, "YOLO", ObservationProvider.Yolo))
            add(
                diagnosticsNode(
                    panelId,
                    sceneId,
                    (state.document?.diagnostics.orEmpty() + scene.diagnostics + entry?.diagnostics.orEmpty())
                        .distinctBy { listOf(it.code, it.referenceId, it.message) },
                )
            )
        }
        val root = playbackNode(
            panelId = panelId,
            id = sceneId,
            label = "Scene ${scene.scene.sequence}",
            subtitle = scene.scene.windowContext.activityName ?: scene.scene.windowContext.packageName ?: "Unbekanntes Window",
            kind = SceneInspectorNodeKind.Scene,
            properties = mapOf(
                "sceneId" to sceneId,
                "status" to scene.scene.status.name,
                "openedAtEpochMs" to scene.scene.openedAtEpochMs.toString(),
                "phase" to state.phase.name,
            ),
            children = children,
        )
        return SceneInspectorTree(sceneId, root)
    }

    private fun windowNode(panelId: String, scene: RecordingPlaybackScene) = playbackNode(
        panelId, "window:${scene.scene.sceneId}",
        scene.scene.windowContext.activityName ?: "WindowContext",
        scene.scene.windowContext.packageName,
        SceneInspectorNodeKind.Window,
        properties = buildMap {
            scene.scene.windowContext.packageName?.let { put("package", it) }
            scene.scene.windowContext.activityName?.let { put("activity", it) }
            scene.scene.windowContext.windowId?.let { put("windowId", it) }
            scene.scene.windowContext.title?.let { put("title", it) }
        },
    )

    private fun interactionNode(panelId: String, entry: RecordingPlaybackEntry, phase: RecordingPlaybackPhase): SceneInspectorNode {
        val interaction = entry.interaction.interaction
        val tap = interaction.payload as? com.visualtasker.wss.recording.RecordingInteractionPayload.Tap
        return playbackNode(
        panelId, "interaction:${interaction.interactionId}",
        tap?.let { "Tap ${it.position.xPx}, ${it.position.yPx}" } ?: interaction.type.name,
        "${entry.transitionStatus.name} - ${phase.name}",
        SceneInspectorNodeKind.Interaction,
        properties = buildMap {
            put("interactionId", interaction.interactionId)
            put("sequence", entry.sequence.toString())
            put("transitionStatus", entry.transitionStatus.name)
            tap?.let { put("point", "${it.position.xPx},${it.position.yPx}") }
            tap?.targetReference?.let { put("targetA11yNodeId", it) }
            put("rawEventIds", interaction.rawEventIds.joinToString(","))
        },
    )
    }

    private fun frameNode(panelId: String, scene: RecordingPlaybackScene): SceneInspectorNode {
        val frame = scene.primaryFrame
        return playbackNode(
            panelId, "frame:${scene.scene.primaryFrameId}", "Visual Frame",
            frame?.frame?.let { "${it.widthPx} x ${it.heightPx}" } ?: "nicht aufgezeichnet",
            SceneInspectorNodeKind.Frame,
            properties = buildMap {
                put("frameId", scene.scene.primaryFrameId)
                frame?.frame?.let {
                    put("assetHash", it.assetHash)
                    put("assetReference", it.assetReference)
                    put("resolution", "${it.widthPx}x${it.heightPx}")
                    put("rotation", it.rotation.toString())
                    put("densityDpi", it.densityDpi.toString())
                    put("captureStatus", it.captureStatus.name)
                }
            },
        )
    }

    private fun a11yNode(panelId: String, scene: RecordingPlaybackScene): SceneInspectorNode {
        val snapshot = scene.a11ySnapshot
        return playbackNode(
            panelId, "provider:a11y:${scene.scene.sceneId}", "A11Y",
            if (snapshot == null) "nicht aufgezeichnet" else snapshot.status.name,
            SceneInspectorNodeKind.Elements,
            provider = ObservationProvider.Accessibility,
            properties = mapOf("status" to (snapshot?.status?.name ?: "nicht aufgezeichnet")),
            children = snapshot?.rootNode?.let { listOf(it.toInspectorNode(panelId)) }.orEmpty(),
        )
    }

    private fun A11yNodeSnapshot.toInspectorNode(panelId: String): SceneInspectorNode = playbackNode(
        panelId = panelId,
        id = "a11y:$stableSnapshotNodeId",
        label = inspectorLabel(),
        subtitle = "$className [$left,$top,$right,$bottom]",
        kind = SceneInspectorNodeKind.Element,
        provider = ObservationProvider.Accessibility,
        properties = buildMap {
            put("a11yNodeId", stableSnapshotNodeId)
            put("class", className)
            put("bounds", "$left,$top,$right,$bottom")
            viewIdResourceName?.let { put("viewId", it) }
            text?.let { put("text", it) }
            contentDescription?.let { put("contentDescription", it) }
            put("clickable", clickable.toString())
            put("focusable", actions.any { it.contains("FOCUS", ignoreCase = true) }.toString())
            put("visible", visibleToUser.toString())
            put("enabled", enabled.toString())
        },
        children = children.sortedBy(A11yNodeSnapshot::childOrder).map { it.toInspectorNode(panelId) },
    )

    private fun A11yNodeSnapshot.inspectorLabel(): String =
        sequenceOf(
            text,
            contentDescription,
            viewIdResourceName?.substringAfterLast('/'),
            className.substringAfterLast('.'),
        )
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()
            ?: "Element $stableSnapshotNodeId"

    private fun providerStatusNode(panelId: String, sceneId: String, label: String, provider: ObservationProvider) = playbackNode(
        panelId, "provider:${provider.name.lowercase()}:$sceneId", label, "nicht aufgezeichnet",
        SceneInspectorNodeKind.Elements, provider, mapOf("status" to "nicht aufgezeichnet"),
    )

    private fun diagnosticsNode(
        panelId: String,
        sceneId: String,
        diagnostics: List<RecordingPlaybackDiagnostic>,
    ) = playbackNode(
        panelId, "diagnostics:$sceneId", "Diagnostik", "${diagnostics.size} Eintraege",
        SceneInspectorNodeKind.Evidence,
        properties = mapOf("count" to diagnostics.size.toString()),
        children = diagnostics.mapIndexed { index, diagnostic ->
            playbackNode(
                panelId, "diagnostic:${diagnostic.code}:$index", diagnostic.code,
                diagnostic.message, SceneInspectorNodeKind.Property,
                properties = buildMap {
                    put("severity", diagnostic.severity.name)
                    put("message", diagnostic.message)
                    diagnostic.referenceId?.let { put("referenceId", it) }
                },
            )
        },
    )
}

private fun playbackNode(
    panelId: String,
    id: String,
    label: String,
    subtitle: String?,
    kind: SceneInspectorNodeKind,
    provider: ObservationProvider? = null,
    properties: Map<String, String> = emptyMap(),
    children: List<SceneInspectorNode> = emptyList(),
): SceneInspectorNode {
    val normalizedLabel = label.trim().ifEmpty { kind.name }
    return SceneInspectorNode(
        id = id,
        label = normalizedLabel,
        kind = kind,
        subtitle = subtitle,
        provider = provider,
        properties = properties,
        payload = WssDragPayload(
            id = "record-playback:${id.toWssDragIdSegment("node")}",
            kind = WssDragPayloadKind.InspectorField,
            label = normalizedLabel,
            sourcePanelId = panelId,
            sourcePanelType = PanelType.SceneInspector,
            tags = setOf("recording", "playback", kind.name.lowercase()),
            data = properties + mapOf("playbackNodeId" to id),
        ),
        children = children,
    )
}
