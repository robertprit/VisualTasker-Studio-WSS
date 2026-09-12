package com.visualtasker.wss.workspace.model

enum class RailMode {
    Program,
    Step,
    Live,
    Replay,
    Curate
}

enum class RailSurfaceMode(
    val label: String,
    val description: String,
) {
    Records(
        label = "Records",
        description = "Aufnahmen, Live Recording, Screenshots und erkannte Scene-Entities",
    ),
    Run(
        label = "Run",
        description = "Dry/Wet Run aus IR Graph, TextEditor, BlockEditor und FlowEditor",
    ),
    WatchDog(
        label = "WatchDog",
        description = "Tasker, VT2VT, Plugins, Serverabfragen und langfristige Statusspuren",
    )
}

fun RailMode.toSurfaceMode(): RailSurfaceMode =
    when (this) {
        RailMode.Program,
        RailMode.Step -> RailSurfaceMode.Run

        RailMode.Replay,
        RailMode.Curate -> RailSurfaceMode.Records

        RailMode.Live -> RailSurfaceMode.WatchDog
    }

fun RailSurfaceMode.defaultRailMode(): RailMode =
    when (this) {
        RailSurfaceMode.Records -> RailMode.Replay
        RailSurfaceMode.Run -> RailMode.Step
        RailSurfaceMode.WatchDog -> RailMode.Live
    }

enum class RailScaleMode {
    Logical,
    Temporal
}

enum class RailMutationPolicy {
    WorkflowCommands,
    ControlledExecution,
    ReadOnly,
    DatasetCommands
}

enum class RailPrimarySource {
    WorkflowGraph,
    SandboxExecution,
    Runtime,
    Record,
    DatasetDraft
}

enum class RailTrackKind {
    Workflow,
    Runtime,
    Data,
    Events,
    Observation,
    Prediction,
    Worldview,
    Recovery,
    Provider,
    Interpretation
}

enum class RailItemKind {
    WorkflowStep,
    RuntimeEvent,
    RecordingEvent,
    DataLifetime,
    Observation,
    Prediction,
    WorldviewState,
    DatasetCandidate
}

enum class RailItemStatus {
    Pending,
    Running,
    Waiting,
    Success,
    Failed,
    Retrying,
    Cancelled,
    Recorded,
    Edited,
    Invalid
}

enum class RailTimelineMarker(
    val symbol: String,
    val description: String,
) {
    Pending("○", "Pending"),
    Recorded("●", "Recorded"),
    Active("◉", "Active"),
    Data("◆", "Data"),
    Position("📍", "Position"),
    Warning("⚠", "Warning")
}

data class RailModeContract(
    val mode: RailMode,
    val primarySources: Set<RailPrimarySource>,
    val mutationPolicy: RailMutationPolicy,
    val visibleTracks: Set<RailTrackKind>,
    val canMutateWorkflow: Boolean,
    val canControlExecution: Boolean,
    val canMutateDataset: Boolean,
) {
    companion object {
        fun forMode(mode: RailMode): RailModeContract =
            when (mode) {
                RailMode.Program -> RailModeContract(
                    mode = mode,
                    primarySources = setOf(RailPrimarySource.WorkflowGraph),
                    mutationPolicy = RailMutationPolicy.WorkflowCommands,
                    visibleTracks = setOf(RailTrackKind.Workflow, RailTrackKind.Data, RailTrackKind.Recovery),
                    canMutateWorkflow = true,
                    canControlExecution = false,
                    canMutateDataset = false,
                )

                RailMode.Step -> RailModeContract(
                    mode = mode,
                    primarySources = setOf(RailPrimarySource.WorkflowGraph, RailPrimarySource.SandboxExecution),
                    mutationPolicy = RailMutationPolicy.ControlledExecution,
                    visibleTracks = setOf(RailTrackKind.Workflow, RailTrackKind.Runtime, RailTrackKind.Data, RailTrackKind.Events),
                    canMutateWorkflow = false,
                    canControlExecution = true,
                    canMutateDataset = false,
                )

                RailMode.Live -> RailModeContract(
                    mode = mode,
                    primarySources = setOf(RailPrimarySource.Runtime),
                    mutationPolicy = RailMutationPolicy.ReadOnly,
                    visibleTracks = setOf(
                        RailTrackKind.Runtime,
                        RailTrackKind.Events,
                        RailTrackKind.Observation,
                        RailTrackKind.Worldview,
                        RailTrackKind.Provider,
                    ),
                    canMutateWorkflow = false,
                    canControlExecution = false,
                    canMutateDataset = false,
                )

                RailMode.Replay -> RailModeContract(
                    mode = mode,
                    primarySources = setOf(RailPrimarySource.Record),
                    mutationPolicy = RailMutationPolicy.ReadOnly,
                    visibleTracks = setOf(
                        RailTrackKind.Workflow,
                        RailTrackKind.Runtime,
                        RailTrackKind.Events,
                        RailTrackKind.Observation,
                        RailTrackKind.Worldview,
                    ),
                    canMutateWorkflow = false,
                    canControlExecution = false,
                    canMutateDataset = false,
                )

                RailMode.Curate -> RailModeContract(
                    mode = mode,
                    primarySources = setOf(RailPrimarySource.Record, RailPrimarySource.DatasetDraft),
                    mutationPolicy = RailMutationPolicy.DatasetCommands,
                    visibleTracks = setOf(
                        RailTrackKind.Events,
                        RailTrackKind.Observation,
                        RailTrackKind.Prediction,
                        RailTrackKind.Worldview,
                        RailTrackKind.Interpretation,
                    ),
                    canMutateWorkflow = false,
                    canControlExecution = false,
                    canMutateDataset = true,
                )
            }
    }
}

data class RailViewDocument(
    val scaleMode: RailScaleMode = RailScaleMode.Logical,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val visibleTracks: Set<RailTrackKind> = RailModeContract.forMode(RailMode.Step).visibleTracks,
    val trackOrder: List<RailTrackKind> = visibleTracks.toList(),
    val collapsedTracks: Set<RailTrackKind> = emptySet(),
    val selectedItemId: String? = null,
    val selectedEpisodeSliceId: String? = null,
    val followRuntime: Boolean = true,
    val semanticZoom: Int = 1,
    val filterText: String = "",
)

data class RailTrack(
    val id: String,
    val kind: RailTrackKind,
    val label: String,
    val items: List<RailItem>,
)

data class RailItem(
    val id: String,
    val sourceId: String,
    val kind: RailItemKind,
    val status: RailItemStatus,
    val label: String,
    val detail: String? = null,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val activityName: String? = null,
)

data class RailProjection(
    val mode: RailMode,
    val surfaceMode: RailSurfaceMode = mode.toSurfaceMode(),
    val contract: RailModeContract = RailModeContract.forMode(mode),
    val scaleMode: RailScaleMode,
    val tracks: List<RailTrack>,
    val title: String = surfaceMode.label,
    val description: String = surfaceMode.description,
)

fun List<RecorderStepUi>.toRailProjection(
    mode: RailMode,
    scaleMode: RailScaleMode = RailScaleMode.Temporal,
): RailProjection {
    val contract = RailModeContract.forMode(mode)
    val stepsWithItems = map { step -> step to step.toRailItem(mode = mode) }
    val tracks = when (mode) {
        RailMode.Program -> buildList {
            add(
                RailTrack(
                    id = "workflow",
                    kind = RailTrackKind.Workflow,
                    label = "Workflow",
                    items = stepsWithItems.map { it.second.copy(kind = RailItemKind.WorkflowStep) },
                )
            )
            stepsWithItems
                .filter { (step, _) -> step.isDataStep() }
                .map { it.second.copy(kind = RailItemKind.DataLifetime) }
                .takeIf { it.isNotEmpty() }
                ?.let { items -> add(RailTrack("variables", RailTrackKind.Data, "Variables", items)) }
        }

        RailMode.Step -> buildList {
            add(
                RailTrack(
                    id = "workflow",
                    kind = RailTrackKind.Workflow,
                    label = "Workflow",
                    items = stepsWithItems.map { it.second.copy(kind = RailItemKind.WorkflowStep) },
                )
            )
            add(
                RailTrack(
                    id = "runtime",
                    kind = RailTrackKind.Runtime,
                    label = "Runtime",
                    items = stepsWithItems
                        .filterNot { (step, _) -> step.isRecordingOnlyEvent() }
                        .map { it.second.copy(kind = RailItemKind.RuntimeEvent) },
                )
            )
            stepsWithItems
                .filter { (step, _) -> step.isRecordingOnlyEvent() }
                .map { it.second.copy(kind = RailItemKind.RecordingEvent) }
                .takeIf { it.isNotEmpty() }
                ?.let { items -> add(RailTrack("events", RailTrackKind.Events, "Events", items)) }
            stepsWithItems
                .filter { (step, _) -> step.isDataStep() }
                .map { it.second.copy(kind = RailItemKind.DataLifetime) }
                .takeIf { it.isNotEmpty() }
                ?.let { items -> add(RailTrack("variables", RailTrackKind.Data, "Variables", items)) }
        }

        RailMode.Live -> listOf(
            RailTrack(
                id = "live-runtime",
                kind = RailTrackKind.Runtime,
                label = "Runtime",
                items = stepsWithItems.map { it.second.copy(kind = RailItemKind.RuntimeEvent) },
            )
        )

        RailMode.Replay -> buildList {
            add(
                RailTrack(
                    id = "record",
                    kind = RailTrackKind.Events,
                    label = "Record",
                    items = stepsWithItems.map { it.second.copy(kind = RailItemKind.RecordingEvent) },
                )
            )
            stepsWithItems
                .filter { (step, _) -> step.isDataStep() }
                .map { it.second.copy(kind = RailItemKind.DataLifetime) }
                .takeIf { it.isNotEmpty() }
                ?.let { items -> add(RailTrack("variables", RailTrackKind.Data, "Variables", items)) }
        }

        RailMode.Curate -> listOf(
            RailTrack(
                id = "dataset-candidates",
                kind = RailTrackKind.Interpretation,
                label = "Curation",
                items = stepsWithItems.map { it.second.copy(kind = RailItemKind.DatasetCandidate) },
            )
        )
    }.filter { it.kind in contract.visibleTracks || mode == RailMode.Program }
    return RailProjection(
        mode = mode,
        surfaceMode = mode.toSurfaceMode(),
        contract = contract,
        scaleMode = scaleMode,
        tracks = tracks,
    )
}

private fun RecorderStepUi.toRailItem(mode: RailMode): RailItem {
    val start = timestampMs
    return RailItem(
        id = "rail-$id",
        sourceId = id,
        kind = when (mode) {
            RailMode.Program -> RailItemKind.WorkflowStep
            RailMode.Step -> RailItemKind.RuntimeEvent
            RailMode.Live -> RailItemKind.RuntimeEvent
            RailMode.Replay -> RailItemKind.RecordingEvent
            RailMode.Curate -> RailItemKind.DatasetCandidate
        },
        status = status.toRailItemStatus(),
        label = label,
        detail = detail,
        startMs = start,
        endMs = start?.let { it + (durationMs ?: 0L).coerceAtLeast(80L) },
        activityName = activityName,
    )
}

private fun RecorderStepUi.isRecordingOnlyEvent(): Boolean =
    status == StepStatus.Recorded ||
        actionType in setOf(
            "activity.change",
            "click",
            "longClick",
            "text.change",
            "scroll",
            "focus",
            "gesture.start",
            "gesture.end",
        )

private fun RecorderStepUi.isDataStep(): Boolean {
    val haystack = "$actionType $label $detail".lowercase()
    return listOf("let", "set", "variable", "data", "result", "clipboard", "file").any { it in haystack }
}

private fun StepStatus.toRailItemStatus(): RailItemStatus =
    when (this) {
        StepStatus.Recorded -> RailItemStatus.Recorded
        StepStatus.Edited -> RailItemStatus.Edited
        StepStatus.Invalid -> RailItemStatus.Invalid
        StepStatus.Executed -> RailItemStatus.Success
    }
