package com.visualtasker.wss.recording

import java.security.MessageDigest

object RecordingInteractionCanonicalizer {
    fun canonicalize(events: List<RawRecordingEvent>): List<RecordingInteraction> =
        events.sortedWith(RecordingCanonicalOrder.events).mapNotNull(::canonicalize)

    fun canonicalize(event: RawRecordingEvent): RecordingInteraction? {
        val payload = event.toPayload() ?: return null
        return RecordingInteraction(
            interactionId = "interaction:${event.rawEventId}",
            sessionId = event.sessionId,
            sequence = event.sequence,
            occurredAtEpochMs = event.occurredAtEpochMs,
            occurredAtElapsedRealtimeNanos = event.occurredAtElapsedRealtimeNanos,
            source = event.source(),
            rawEventIds = listOf(event.rawEventId),
            evidenceRefs = listOf("evidence:interaction:${event.rawEventId}:input"),
            beforeSceneId = event.payload["beforeSceneId"],
            afterSceneId = event.payload["afterSceneId"],
            status = RecordingInteractionStatus.UNCHANGED,
            payload = payload,
        )
    }

    private fun RawRecordingEvent.toPayload(): RecordingInteractionPayload? = when (kind.lowercase()) {
        "tap", "click", "button.click", "longclick" -> point("x", "y")?.let { position ->
            RecordingInteractionPayload.Tap(
                position = position,
                durationMs = payload.long("durationMs"),
                pointerId = payload.int("pointerId"),
                button = payload["button"],
                targetReference = payload["targetA11yNodeId"] ?: payload["viewId"] ?: payload["resourceId"],
            )
        }
        "swipe", "gesture.swipe" -> {
            val start = point("startX", "startY") ?: point("x1", "y1")
            val end = point("endX", "endY") ?: point("x2", "y2")
            if (start == null || end == null) null else RecordingInteractionPayload.Swipe(
                start = start,
                end = end,
                durationMs = payload.long("durationMs") ?: 0L,
                path = parsePath(payload["path"]),
                pointerId = payload.int("pointerId"),
            )
        }
        "text", "text.change", "input.text" -> {
            val value = payload["text"] ?: payload["value"]
            val redacted = payload["redacted"].toBoolean() || payload["password"].toBoolean()
            RecordingInteractionPayload.Text(
                value = value.takeUnless { redacted },
                redacted = redacted,
                valueHash = payload["textHash"] ?: value?.takeIf { redacted }?.sha256(),
                targetReference = payload["targetA11yNodeId"] ?: payload["viewId"] ?: payload["resourceId"],
                inputMethod = payload["inputMethod"] ?: payload["ime"],
            )
        }
        "window.appeared" -> window(RecordingWindowChange.APPEARED)
        "window.disappeared" -> window(RecordingWindowChange.DISAPPEARED)
        "activity.change" -> window(RecordingWindowChange.ACTIVITY_CHANGED)
        "focus", "window.focus" -> window(RecordingWindowChange.FOCUS_CHANGED)
        "app.foreground", "window.foreground" -> window(RecordingWindowChange.FOREGROUND_CHANGED)
        "screenshot", "screenshot.captured" -> {
            val resourceId = payload["assetHash"] ?: payload["resourceId"] ?: payload["assetReference"]
            resourceId?.let {
                RecordingInteractionPayload.Screenshot(
                    resource = RecordingResourceRef(RecordingResourceKind.SCREENSHOT_ASSET, it),
                    widthPx = payload.int("widthPx"),
                    heightPx = payload.int("heightPx"),
                )
            }
        }
        else -> null
    }

    private fun RawRecordingEvent.window(change: RecordingWindowChange) = RecordingInteractionPayload.Window(
        change = change,
        packageName = payload["package"] ?: payload["packageName"],
        activityName = payload["activity"] ?: payload["activityName"],
        windowId = payload["windowId"],
        title = payload["title"] ?: payload["text"],
    )

    private fun RawRecordingEvent.point(xKey: String, yKey: String): RecordingPoint? {
        val x = payload.int(xKey) ?: payload.int("${xKey}Px")
        val y = payload.int(yKey) ?: payload.int("${yKey}Px")
        return if (x == null || y == null) null else RecordingPoint(x, y)
    }

    private fun RawRecordingEvent.source(): RecordingInteractionSource = when (payload["source"]?.lowercase()) {
        "accessibility", "a11y" -> RecordingInteractionSource.ACCESSIBILITY
        "overlay", "floatingoverlay" -> RecordingInteractionSource.OVERLAY
        "system" -> RecordingInteractionSource.SYSTEM
        "recorder" -> RecordingInteractionSource.RECORDER
        else -> RecordingInteractionSource.IMPORT
    }

    private fun parsePath(raw: String?): List<RecordingPoint> = raw.orEmpty()
        .split(';')
        .mapNotNull { pair ->
            val values = pair.split(',').map(String::trim)
            if (values.size != 2) null else {
                val x = values[0].toIntOrNull()
                val y = values[1].toIntOrNull()
                if (x == null || y == null) null else RecordingPoint(x, y)
            }
        }

    private fun Map<String, String>.int(key: String): Int? = get(key)?.toIntOrNull()
    private fun Map<String, String>.long(key: String): Long? = get(key)?.toLongOrNull()
    private fun String?.toBoolean(): Boolean = this?.equals("true", ignoreCase = true) == true
    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
}
