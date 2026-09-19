package com.visualtasker.wss.recording.persistence

import com.visualtasker.wss.recording.A11yNodeSnapshot
import com.visualtasker.wss.recording.A11ySnapshot
import com.visualtasker.wss.recording.CaptureFrame
import com.visualtasker.wss.recording.RawRecordingEvent
import com.visualtasker.wss.recording.RecordingCaptureStatus
import com.visualtasker.wss.recording.RecordingInteractionStatus
import com.visualtasker.wss.recording.RecordingPolicySnapshot
import com.visualtasker.wss.recording.RecordingScene
import com.visualtasker.wss.recording.RecordingSceneStatus
import com.visualtasker.wss.recording.RecordingSession
import com.visualtasker.wss.recording.RecordingSessionStatus
import com.visualtasker.wss.recording.RecordingWindowContext
import com.visualtasker.wss.recording.ScreenshotAsset
import com.visualtasker.wss.recording.TapInteraction
import org.json.JSONArray
import org.json.JSONObject

internal fun RecordingSession.toEntity() = RecordingSessionEntity(
    sessionId, schemaVersion, status.name, startedAtEpochMs, startedAtElapsedRealtimeNanos,
    stoppedAtEpochMs, createdBy, recordingPolicySnapshot.toJson().toString(), initialSceneId, latestSceneId, failure,
)

internal fun RecordingSessionEntity.toDomain() = RecordingSession(
    sessionId, schemaVersion, RecordingSessionStatus.valueOf(status), startedAtEpochMs,
    startedAtElapsedRealtimeNanos, stoppedAtEpochMs, createdBy,
    JSONObject(recordingPolicyJson).toPolicy(), initialSceneId, latestSceneId, failure,
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

internal fun TapInteraction.toEntity() = TapInteractionEntity(
    interactionId, rawEventId, sessionId, sequence, occurredAtEpochMs, occurredAtElapsedRealtimeNanos,
    xPx, yPx, beforeSceneId, afterSceneId, targetA11yNodeId, status.name,
)

internal fun TapInteractionEntity.toDomain() = TapInteraction(
    interactionId, rawEventId, sessionId, sequence, occurredAtEpochMs, occurredAtElapsedRealtimeNanos,
    xPx, yPx, beforeSceneId, afterSceneId, targetA11yNodeId, RecordingInteractionStatus.valueOf(status),
)

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

private fun JSONObject.putNullable(key: String, value: String?) = put(key, value ?: JSONObject.NULL)
private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
private fun JSONObject.toStringMap(): Map<String, String> = keys().asSequence().associateWith { optString(it) }
private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
