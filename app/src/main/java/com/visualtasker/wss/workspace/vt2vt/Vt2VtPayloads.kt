package com.visualtasker.wss.workspace.vt2vt

import com.visualtasker.wss.logging.StudioLogEntry
import com.visualtasker.wss.workspace.model.WorkspaceWorkflowState
import de.visualtasker.flowchart.domain.FlowEdgeId
import de.visualtasker.flowchart.domain.FlowNodeId
import de.visualtasker.flowchart.domain.FlowRuntimeNodeState
import de.visualtasker.flowchart.domain.FlowRuntimeSnapshot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.json.JSONArray
import org.json.JSONObject

private const val VT2VT_PAYLOAD_SCHEMA_VERSION = 1
private const val VT2VT_WORKSPACE_ENCODING = "gzip+base64"
private const val VT2VT_MAX_DECOMPRESSED_WORKSPACE_BYTES = 4 * 1024 * 1024

data class Vt2VtWorkspaceMirror(
    val revision: Int,
    val mutationSource: String,
    val workspaceJson: String,
    val emscript: String?,
    val checksum: String,
)

data class Vt2VtRuntimeMirror(
    val runId: String?,
    val sourceSessionId: String?,
    val documentId: String?,
    val documentRevision: String?,
    val sequence: Long?,
    val capturedAtEpochMs: Long?,
    val activeStep: Int?,
    val activeNodeId: String?,
    val nodeStates: Map<String, FlowRuntimeNodeState>,
    val traversedEdgeIds: List<String>,
    val diagnostics: List<String>,
)

fun workspacePayload(workflowState: WorkspaceWorkflowState): Map<String, String> =
    mapOf(
        "payloadSchemaVersion" to VT2VT_PAYLOAD_SCHEMA_VERSION.toString(),
        "revision" to workflowState.revision.toString(),
        "mutationSource" to workflowState.mutationSource,
        "blocks" to workflowState.document.blocks.size.toString(),
        "flowNodes" to workflowState.flowchartProjection.graph.nodes.size.toString(),
        "flowEdges" to workflowState.flowchartProjection.graph.edges.size.toString(),
        "workspaceEncoding" to VT2VT_WORKSPACE_ENCODING,
        "workspaceChecksum" to workflowState.serializedJson.sha256(),
        "workspaceData" to workflowState.serializedJson.gzipBase64(),
        "emscript" to workflowState.emscriptProjection.getOrNull().orEmpty(),
    )

fun decodeWorkspacePayload(payload: Map<String, String>): Vt2VtWorkspaceMirror {
    require(payload["payloadSchemaVersion"]?.toIntOrNull() == VT2VT_PAYLOAD_SCHEMA_VERSION) {
        "Unsupported VT2VT workspace payload schema: ${payload["payloadSchemaVersion"]}"
    }
    require(payload["workspaceEncoding"] == VT2VT_WORKSPACE_ENCODING) {
        "Unsupported VT2VT workspace encoding: ${payload["workspaceEncoding"]}"
    }
    val workspaceJson = payload.getValue("workspaceData").gunzipBase64()
    val expectedChecksum = payload.getValue("workspaceChecksum")
    require(workspaceJson.sha256() == expectedChecksum) { "VT2VT workspace checksum mismatch." }
    return Vt2VtWorkspaceMirror(
        revision = payload.getValue("revision").toInt(),
        mutationSource = payload.getValue("mutationSource"),
        workspaceJson = workspaceJson,
        emscript = payload["emscript"]?.takeIf { it.isNotBlank() },
        checksum = expectedChecksum,
    )
}

fun selectionPayload(
    focusedNodeId: FlowNodeId?,
    focusedEdgeId: FlowEdgeId?
): Map<String, String> =
    mapOf(
        "node" to (focusedNodeId?.value ?: "-"),
        "edge" to (focusedEdgeId?.value ?: "-")
    )

fun runtimePayload(
    workflowState: WorkspaceWorkflowState,
    snapshot: FlowRuntimeSnapshot?,
    activeRuntimeStepIndex: Int?,
    focusedNodeId: FlowNodeId?,
    focusedEdgeId: FlowEdgeId?
): Map<String, String> =
    selectionPayload(focusedNodeId, focusedEdgeId) + mapOf(
        "payloadSchemaVersion" to VT2VT_PAYLOAD_SCHEMA_VERSION.toString(),
        "revision" to workflowState.revision.toString(),
        "mutationSource" to workflowState.mutationSource,
        "runId" to (snapshot?.runId?.value ?: "-"),
        "sourceSessionId" to (snapshot?.sourceSessionId?.value ?: "-"),
        "documentId" to (snapshot?.documentId?.value ?: "-"),
        "documentRevision" to (snapshot?.documentRevision?.value?.toString() ?: "-"),
        "runtimeSequence" to (snapshot?.sequence?.toString() ?: "-"),
        "capturedAtEpochMs" to (snapshot?.capturedAtEpochMs?.toString() ?: "-"),
        "activeStep" to (activeRuntimeStepIndex?.toString() ?: "-"),
        "activeNode" to (snapshot?.activeNodeId?.value ?: focusedNodeId?.value ?: "-"),
        "traversedEdges" to (snapshot?.traversedEdgeIds?.size?.toString() ?: "0"),
        "diagnostics" to (snapshot?.diagnostics?.size?.toString() ?: "0"),
        "succeeded" to snapshot.countState(FlowRuntimeNodeState.SUCCEEDED).toString(),
        "failed" to snapshot.countState(FlowRuntimeNodeState.FAILED).toString(),
        "skipped" to snapshot.countState(FlowRuntimeNodeState.SKIPPED).toString(),
        "nodeStatesJson" to JSONObject().apply {
            snapshot?.nodeStates
                ?.entries
                ?.sortedBy { it.key.value }
                ?.forEach { (nodeId, state) -> put(nodeId.value, state.name) }
        }.toString(),
        "traversedEdgeIdsJson" to JSONArray().apply {
            snapshot?.traversedEdgeIds?.forEach { put(it.value) }
        }.toString(),
        "diagnosticsJson" to JSONArray().apply {
            snapshot?.diagnostics?.forEach { diagnostic ->
                put(
                    JSONObject()
                        .put("code", diagnostic.code)
                        .put("message", diagnostic.message)
                        .put("severity", diagnostic.severity.name)
                        .put("nodeId", diagnostic.nodeId?.value.orEmpty())
                        .put("edgeId", diagnostic.edgeId?.value.orEmpty()),
                )
            }
        }.toString(),
    )

fun decodeRuntimePayload(payload: Map<String, String>): Vt2VtRuntimeMirror {
    require(payload["payloadSchemaVersion"]?.toIntOrNull() == VT2VT_PAYLOAD_SCHEMA_VERSION) {
        "Unsupported VT2VT runtime payload schema: ${payload["payloadSchemaVersion"]}"
    }
    val nodeStatesObject = JSONObject(payload["nodeStatesJson"] ?: "{}")
    val nodeStates = buildMap {
        nodeStatesObject.keys().forEach { nodeId ->
            put(nodeId, enumValueOf<FlowRuntimeNodeState>(nodeStatesObject.getString(nodeId)))
        }
    }
    val traversedEdges = JSONArray(payload["traversedEdgeIdsJson"] ?: "[]").toStringList()
    val diagnosticArray = JSONArray(payload["diagnosticsJson"] ?: "[]")
    val diagnostics = buildList {
        for (index in 0 until diagnosticArray.length()) {
            val item = diagnosticArray.getJSONObject(index)
            add("${item.optString("severity")}:${item.optString("code")}:${item.optString("message")}")
        }
    }
    return Vt2VtRuntimeMirror(
        runId = payload.optionalValue("runId"),
        sourceSessionId = payload.optionalValue("sourceSessionId"),
        documentId = payload.optionalValue("documentId"),
        documentRevision = payload.optionalValue("documentRevision"),
        sequence = payload.optionalLong("runtimeSequence"),
        capturedAtEpochMs = payload.optionalLong("capturedAtEpochMs"),
        activeStep = payload.optionalInt("activeStep"),
        activeNodeId = payload.optionalValue("activeNode"),
        nodeStates = nodeStates,
        traversedEdgeIds = traversedEdges,
        diagnostics = diagnostics,
    )
}

fun logPayload(entry: StudioLogEntry?): Map<String, String> =
    if (entry == null) {
        mapOf("status" to "no-log-entry")
    } else {
        mapOf(
            "id" to entry.id,
            "timestamp" to entry.timestamp.toString(),
            "level" to entry.level.name,
            "source" to entry.source,
            "message" to entry.message,
            "details" to entry.details.orEmpty(),
            "documentRevision" to (entry.documentRevision?.toString() ?: "-"),
            "repeatCount" to entry.repeatCount.toString()
        )
    }

private fun FlowRuntimeSnapshot?.countState(state: FlowRuntimeNodeState): Int =
    this?.nodeStates?.values?.count { it == state } ?: 0

private fun String.gzipBase64(): String {
    val output = ByteArrayOutputStream()
    GZIPOutputStream(output).bufferedWriter(Charsets.UTF_8).use { it.write(this) }
    return Base64.getEncoder().encodeToString(output.toByteArray())
}

private fun String.gunzipBase64(): String {
    val compressed = Base64.getDecoder().decode(this)
    val output = ByteArrayOutputStream()
    GZIPInputStream(ByteArrayInputStream(compressed)).use { input ->
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= VT2VT_MAX_DECOMPRESSED_WORKSPACE_BYTES) {
                "VT2VT workspace payload exceeds decompressed size limit."
            }
            output.write(buffer, 0, count)
        }
    }
    return output.toString(Charsets.UTF_8.name())
}

private fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

private fun JSONArray.toStringList(): List<String> =
    buildList { for (index in 0 until length()) add(getString(index)) }

private fun Map<String, String>.optionalValue(key: String): String? =
    get(key)?.takeUnless { it == "-" || it.isBlank() }

private fun Map<String, String>.optionalLong(key: String): Long? =
    optionalValue(key)?.toLongOrNull()

private fun Map<String, String>.optionalInt(key: String): Int? =
    optionalValue(key)?.toIntOrNull()
