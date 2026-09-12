package com.visualtasker.wss.workspace.model

data class RecorderStepUi(
    val id: String,
    val label: String,
    val actionType: String,
    val status: StepStatus,
    val timestampMs: Long? = null,
    val durationMs: Long? = null,
    val activityName: String? = null,
    val detail: String? = null,
    val bounds: WorldviewRect? = null,
    val point: WorldviewPoint? = null,
    val properties: Map<String, String> = emptyMap(),
)

enum class StepStatus {
    Recorded,
    Edited,
    Invalid,
    Executed
}
