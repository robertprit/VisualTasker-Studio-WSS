package com.visualtasker.wss.recording.persistence

import com.visualtasker.wss.recording.A11yNodeSnapshot
import com.visualtasker.wss.recording.A11ySnapshot
import com.visualtasker.wss.recording.CaptureFrame
import com.visualtasker.wss.recording.RawRecordingEvent
import com.visualtasker.wss.recording.RecordingCaptureStatus
import com.visualtasker.wss.recording.RecordingInteractionStatus
import com.visualtasker.wss.recording.RecordingInteraction
import com.visualtasker.wss.recording.RecordingInteractionPayload
import com.visualtasker.wss.recording.RecordingInteractionSource
import com.visualtasker.wss.recording.RecordingInteractionType
import com.visualtasker.wss.recording.RecordingPoint
import com.visualtasker.wss.recording.RecordingResourceKind
import com.visualtasker.wss.recording.RecordingResourceRef
import com.visualtasker.wss.recording.RecordingWindowChange
import com.visualtasker.wss.recording.RecordingPolicySnapshot
import com.visualtasker.wss.recording.RecordingScene
import com.visualtasker.wss.recording.RecordingSceneStatus
import com.visualtasker.wss.recording.RecordingSession
import com.visualtasker.wss.recording.RecordingSessionStatus
import com.visualtasker.wss.recording.RecordingWindowContext
import com.visualtasker.wss.recording.ScreenshotAsset
import org.json.JSONArray
import org.json.JSONObject

internal fun RecordingSession.toEntity() = RecordingSessionEntity(
    sessionId = sessionId,
    schemaVersion = schemaVersion,
    status = status.name,
    startedAtEpochMs = startedAtEpochMs,
    startedAtElapsedRealtimeNanos = startedAtElapsedRealtimeNanos,
    stoppedAtEpochMs = stoppedAtEpochMs,
    createdBy = createdBy,
    recordingPolicyJson = recordingPolicySnapshot.toJson().toString(),
    initialSceneId = initialSceneId,
    latestSceneId = latestSceneId,
    failure = failure,
    resumedFromSessionId = resumedFromSessionId,
)

internal fun RecordingSessionEntity.toDomain() = RecordingSession(
    sessionId = sessionId,
    schemaVersion = schemaVersion,
    status = RecordingSessionStatus.valueOf(status),
    startedAtEpochMs = startedAtEpochMs,
    startedAtElapsedRealtimeNanos = startedAtElapsedRealtimeNanos,
    stoppedAtEpochMs = stoppedAtEpochMs,
    createdBy = createdBy,
    recordingPolicySnapshot = JSONObject(recordingPolicyJson).toPolicy(),
    initialSceneId = initialSceneId,
    latestSceneId = latestSceneId,
    failure = failure,
    resumedFromSessionId = resumedFromSessionId,
)

internal fun RecordingScene.toEntity() = RecordingSceneEntity(
    sceneId, sessionId, sequence, openedAtEpochMs, openedAtElapsedRealtimeNanos, closedAtEpochMs,
    primaryFrameId, windowContext.toJson().toString(), status.name,
)

internal fun RecordingSceneEntity.toDomain() = RecordingScene(
    sceneId, sessionId, sequence, openedAtEpochMs, openedAtElapsedRealtimeNanos, closedAtEpochMs,
    primaryFrameId, JSONObject(windowContextJson).toWindowContext(), RecordingSceneStatus.valueOf(status),
)

internal fun ScreenshotAsset.toEntity() = ScreenshotAssetEntity(assetHash, assetReference, byteCount, createdAtEpochMs)
internal fun ScreenshotAssetEntity.toDomain() = ScreenshotAsset(assetHash, assetReference, byteCount, createdAtEpochMs)

internal fun CaptureFrame.toEntity() = CaptureFrameEntity(
    frameId, sessionId, sceneId, capturedAtEpochMs, capturedAtElapsedRealtimeNanos, assetHash, assetReference,
    widthPx, heightPx, rotation, densityDpi, captureStatus.name, coordinateSpace,
)

internal fun CaptureFrameEntity.toDomain() = CaptureFrame(
    frameId, sessionId, sceneId, capturedAtEpochMs, capturedAtElapsedRealtimeNanos, assetHash, assetReference,
    widthPx, heightPx, rotation, densityDpi, RecordingCaptureStatus.valueOf(captureStatus), coordinateSpace,
)

internal fun A11ySnapshot.toEntity() = A11ySnapshotEntity(
    snapshotId, sessionId, sceneId, frameId, capturedAtEpochMs, rootNode.toJson().toString(),
    windowId, packageName, status.name,
)

internal fun A11ySnapshotEntity.toDomain() = A11ySnapshot(
    snapshotId, sessionId, sceneId, frameId, capturedAtEpochMs, JSONObject(rootNodeJson).toA11yNode(),
    windowId, packageName, RecordingCaptureStatus.valueOf(status),
)

internal fun RawRecordingEvent.toEntity() = RawRecordingEventEntity(
    rawEventId, sessionId, sequence, occurredAtEpochMs, occurredAtElapsedRealtimeNanos, kind,
    JSONObject(payload).toString(),
)

internal fun RawRecordingEventEntity.toDomain() = RawRecordingEvent(
    rawEventId, sessionId, sequence, occurredAtEpochMs, occurredAtElapsedRealtimeNanos, kind,
    JSONObject(payloadJson).toStringMap(),
)

internal fun RecordingInteraction.toEntity() = RecordingInteractionEntity(
    interactionId = interactionId,
    sessionId = sessionId,
    sequence = sequence,
    occurredAtEpochMs = occurredAtEpochMs,
    occurredAtElapsedRealtimeNanos = occurredAtElapsedRealtimeNanos,
    type = type.name,
    source = source.name,
    rawEventIdsJson = rawEventIds.toJsonArray().toString(),
    evidenceRefsJson = evidenceRefs.toJsonArray().toString(),
    beforeSceneId = beforeSceneId,
    afterSceneId = afterSceneId,
    status = status.name,
    payloadJson = payload.toJson().toString(),
)

internal fun RecordingInteractionEntity.toDomain() = RecordingInteraction(
    interactionId = interactionId,
    sessionId = sessionId,
    sequence = sequence,
    occurredAtEpochMs = occurredAtEpochMs,
    occurredAtElapsedRealtimeNanos = occurredAtElapsedRealtimeNanos,
    source = RecordingInteractionSource.valueOf(source),
    rawEventIds = JSONArray(rawEventIdsJson).strings(),
    evidenceRefs = JSONArray(evidenceRefsJson).strings(),
    beforeSceneId = beforeSceneId,
    afterSceneId = afterSceneId,
    status = RecordingInteractionStatus.valueOf(status),
    payload = JSONObject(payloadJson).toPayload(RecordingInteractionType.valueOf(type)),
)

private fun RecordingInteractionPayload.toJson(): JSONObject = when (this) {
    is RecordingInteractionPayload.Tap -> JSONObject()
        .put("xPx", position.xPx).put("yPx", position.yPx)
        .putNullable("durationMs", durationMs).putNullable("pointerId", pointerId)
        .putNullable("button", button).putNullable("targetReference", targetReference)
    is RecordingInteractionPayload.Swipe -> JSONObject()
        .put("startXpx", start.xPx).put("startYpx", start.yPx)
        .put("endXpx", end.xPx).put("endYpx", end.yPx)
        .put("durationMs", durationMs).putNullable("pointerId", pointerId)
        .put("path", JSONArray().apply { path.forEach { put(JSONObject().put("xPx", it.xPx).put("yPx", it.yPx)) } })
    is RecordingInteractionPayload.Text -> JSONObject()
        .putNullable("value", value).put("redacted", redacted).putNullable("valueHash", valueHash)
        .putNullable("targetReference", targetReference).putNullable("inputMethod", inputMethod)
    is RecordingInteractionPayload.Window -> JSONObject()
        .put("change", change.name).putNullable("packageName", packageName)
        .putNullable("activityName", activityName).putNullable("windowId", windowId).putNullable("title", title)
    is RecordingInteractionPayload.Screenshot -> JSONObject()
        .put("resourceKind", resource.kind.name).put("resourceId", resource.resourceId)
        .putNullable("widthPx", widthPx).putNullable("heightPx", heightPx)
}

private fun JSONObject.toPayload(type: RecordingInteractionType): RecordingInteractionPayload = when (type) {
    RecordingInteractionType.TAP -> RecordingInteractionPayload.Tap(
        position = RecordingPoint(getInt("xPx"), getInt("yPx")),
        durationMs = nullableLong("durationMs"),
        pointerId = nullableInt("pointerId"),
        button = nullableString("button"),
        targetReference = nullableString("targetReference"),
    )
    RecordingInteractionType.SWIPE -> RecordingInteractionPayload.Swipe(
        start = RecordingPoint(getInt("startXpx"), getInt("startYpx")),
        end = RecordingPoint(getInt("endXpx"), getInt("endYpx")),
        durationMs = getLong("durationMs"),
        path = optJSONArray("path")?.objects()?.map { RecordingPoint(it.getInt("xPx"), it.getInt("yPx")) }.orEmpty(),
        pointerId = nullableInt("pointerId"),
    )
    RecordingInteractionType.TEXT -> RecordingInteractionPayload.Text(
        value = nullableString("value"),
        redacted = optBoolean("redacted", false),
        valueHash = nullableString("valueHash"),
        targetReference = nullableString("targetReference"),
        inputMethod = nullableString("inputMethod"),
    )
    RecordingInteractionType.WINDOW -> RecordingInteractionPayload.Window(
        change = RecordingWindowChange.valueOf(getString("change")),
        packageName = nullableString("packageName"), activityName = nullableString("activityName"),
        windowId = nullableString("windowId"), title = nullableString("title"),
    )
    RecordingInteractionType.SCREENSHOT -> RecordingInteractionPayload.Screenshot(
        resource = RecordingResourceRef(RecordingResourceKind.valueOf(getString("resourceKind")), getString("resourceId")),
        widthPx = nullableInt("widthPx"), heightPx = nullableInt("heightPx"),
    )
}

private fun RecordingPolicySnapshot.toJson() = JSONObject()
    .put("captureScreenshot", captureScreenshot)
    .put("captureAccessibility", captureAccessibility)
    .put("captureText", captureText)

private fun JSONObject.toPolicy() = RecordingPolicySnapshot(
    captureScreenshot = optBoolean("captureScreenshot", true),
    captureAccessibility = optBoolean("captureAccessibility", true),
    captureText = optBoolean("captureText", false),
)

private fun RecordingWindowContext.toJson() = JSONObject()
    .putNullable("packageName", packageName)
    .putNullable("activityName", activityName)
    .putNullable("windowId", windowId)
    .putNullable("title", title)

private fun JSONObject.toWindowContext() = RecordingWindowContext(
    packageName = nullableString("packageName"),
    activityName = nullableString("activityName"),
    windowId = nullableString("windowId"),
    title = nullableString("title"),
)

private fun A11yNodeSnapshot.toJson(): JSONObject = JSONObject()
    .put("stableSnapshotNodeId", stableSnapshotNodeId)
    .putNullable("parentNodeId", parentNodeId)
    .put("childOrder", childOrder)
    .put("className", className)
    .putNullable("viewIdResourceName", viewIdResourceName)
    .putNullable("text", text)
    .putNullable("contentDescription", contentDescription)
    .put("left", left).put("top", top).put("right", right).put("bottom", bottom)
    .put("clickable", clickable).put("longClickable", longClickable).put("scrollable", scrollable)
    .put("editable", editable).put("enabled", enabled).put("selected", selected).put("checked", checked)
    .put("visibleToUser", visibleToUser)
    .put("actions", JSONArray().apply { actions.forEach { put(it) } })
    .put("children", JSONArray().apply { children.forEach { put(it.toJson()) } })

private fun JSONObject.toA11yNode(): A11yNodeSnapshot = A11yNodeSnapshot(
    stableSnapshotNodeId = getString("stableSnapshotNodeId"),
    parentNodeId = nullableString("parentNodeId"),
    childOrder = getInt("childOrder"),
    className = getString("className"),
    viewIdResourceName = nullableString("viewIdResourceName"),
    text = nullableString("text"),
    contentDescription = nullableString("contentDescription"),
    left = getInt("left"), top = getInt("top"), right = getInt("right"), bottom = getInt("bottom"),
    clickable = getBoolean("clickable"), longClickable = getBoolean("longClickable"),
    scrollable = getBoolean("scrollable"), editable = getBoolean("editable"), enabled = getBoolean("enabled"),
    selected = getBoolean("selected"), checked = getBoolean("checked"), visibleToUser = getBoolean("visibleToUser"),
    actions = getJSONArray("actions").strings(),
    children = getJSONArray("children").objects().map(JSONObject::toA11yNode),
)

private fun JSONObject.putNullable(key: String, value: Any?) = put(key, value ?: JSONObject.NULL)
private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
private fun JSONObject.nullableInt(key: String): Int? = if (!has(key) || isNull(key)) null else getInt(key)
private fun JSONObject.nullableLong(key: String): Long? = if (!has(key) || isNull(key)) null else getLong(key)
private fun JSONObject.toStringMap(): Map<String, String> = keys().asSequence().associateWith { optString(it) }
private fun List<String>.toJsonArray() = JSONArray().apply { forEach(::put) }
private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
