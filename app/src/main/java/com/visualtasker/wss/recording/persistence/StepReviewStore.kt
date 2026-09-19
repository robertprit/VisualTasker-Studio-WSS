package com.visualtasker.wss.recording.persistence

import androidx.room.withTransaction
import com.visualtasker.wss.recording.CandidateAction
import com.visualtasker.wss.recording.CandidateTarget
import com.visualtasker.wss.recording.StepProposalSnapshot
import com.visualtasker.wss.recording.StepReviewDecision
import com.visualtasker.wss.recording.StepReviewStatus
import org.json.JSONArray
import org.json.JSONObject

interface StepReviewStore {
    suspend fun save(decision: StepReviewDecision)
    suspend fun loadForSession(sessionId: String): List<StepReviewDecision>
}

class RoomStepReviewStore(
    private val database: RecorderDatabase,
) : StepReviewStore {
    override suspend fun save(decision: StepReviewDecision) = database.withTransaction {
        require(database.recorderDao().session(decision.sourceSessionId) != null) {
            "RecordingSession ${decision.sourceSessionId} does not exist."
        }
        database.recorderDao().upsertReviewDecision(decision.toEntity())
    }

    override suspend fun loadForSession(sessionId: String): List<StepReviewDecision> =
        database.recorderDao().reviewDecisions(sessionId).map(StepReviewDecisionEntity::toDomain)
}

private fun StepReviewDecision.toEntity() = StepReviewDecisionEntity(
    decisionId = decisionId,
    candidateId = candidateId,
    sourceSessionId = sourceSessionId,
    sourceRecordVersion = sourceRecordVersion,
    status = status.name,
    originalProposalJson = originalProposal.toJson().toString(),
    correctedProposalJson = correctedProposal?.toJson()?.toString(),
    selectedTargetNodeId = selectedTargetNodeId,
    reasonCode = reasonCode,
    note = note,
    decidedAtEpochMs = decidedAtEpochMs,
)

private fun StepReviewDecisionEntity.toDomain() = StepReviewDecision(
    decisionId = decisionId,
    candidateId = candidateId,
    sourceSessionId = sourceSessionId,
    sourceRecordVersion = sourceRecordVersion,
    status = StepReviewStatus.valueOf(status),
    originalProposal = JSONObject(originalProposalJson).toProposal(),
    correctedProposal = correctedProposalJson?.let(::JSONObject)?.toProposal(),
    selectedTargetNodeId = selectedTargetNodeId,
    reasonCode = reasonCode,
    note = note,
    decidedAtEpochMs = decidedAtEpochMs,
)

private fun StepProposalSnapshot.toJson() = JSONObject()
    .put("action", action.name)
    .put("displayLabel", displayLabel)
    .put("target", target.toJson())

private fun JSONObject.toProposal() = StepProposalSnapshot(
    action = CandidateAction.valueOf(getString("action")),
    target = getJSONObject("target").toTarget(),
    displayLabel = getString("displayLabel"),
)

private fun CandidateTarget.toJson(): JSONObject = when (this) {
    is CandidateTarget.Coordinate -> JSONObject()
        .put("kind", "COORDINATE")
        .put("xPx", xPx)
        .put("yPx", yPx)
    is CandidateTarget.A11y -> JSONObject()
        .put("kind", "A11Y")
        .put("nodeId", nodeId)
        .putNullable("resourceId", resourceId)
        .putNullable("text", text)
        .putNullable("contentDescription", contentDescription)
        .put("className", className)
        .put("left", left)
        .put("top", top)
        .put("right", right)
        .put("bottom", bottom)
        .put("clickable", clickable)
        .put("visible", visible)
        .put("enabled", enabled)
        .putNullable("packageName", packageName)
        .putNullable("windowId", windowId)
        .put("xPx", xPx)
        .put("yPx", yPx)
        .put("alternativeNodeIds", JSONArray(alternativeNodeIds))
}

private fun JSONObject.toTarget(): CandidateTarget = when (getString("kind")) {
    "A11Y" -> CandidateTarget.A11y(
        nodeId = getString("nodeId"),
        resourceId = nullableString("resourceId"),
        text = nullableString("text"),
        contentDescription = nullableString("contentDescription"),
        className = getString("className"),
        left = getInt("left"),
        top = getInt("top"),
        right = getInt("right"),
        bottom = getInt("bottom"),
        clickable = getBoolean("clickable"),
        visible = getBoolean("visible"),
        enabled = getBoolean("enabled"),
        packageName = nullableString("packageName"),
        windowId = nullableString("windowId"),
        xPx = getInt("xPx"),
        yPx = getInt("yPx"),
        alternativeNodeIds = getJSONArray("alternativeNodeIds").let { array ->
            List(array.length()) { index -> array.getString(index) }
        },
    )
    else -> CandidateTarget.Coordinate(getInt("xPx"), getInt("yPx"))
}

private fun JSONObject.putNullable(key: String, value: String?): JSONObject = put(key, value ?: JSONObject.NULL)
private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
