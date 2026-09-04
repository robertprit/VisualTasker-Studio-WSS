package com.visualtasker.wss.visual.semantics

enum class ProjectionKind {
    BlockEditor,
    Flowchart,
    Inspector,
    LogConsole,
    DebugInfo,
    SceneCanvas,
    RecordList,
    Timeline,
    DataBrowser,
}

enum class VisualRole {
    WorkflowEvent,
    WorkflowAction,
    WorkflowCondition,
    WorkflowLoop,
    WorkflowValue,
    WorkflowConnector,
    WorldEntity,
    Observation,
    Resource,
    Ambiguity,
    RuntimeEvent,
    RecordStep,
    Group,
    Unknown,
}

enum class VisualAuthority {
    Canonical,
    Derived,
    AiProposed,
    HumanConfirmed,
    Unresolved,
}

enum class VisualCertainty {
    Known,
    Uncertain,
    Ambiguous,
    Conflicting,
}

enum class VisualActivity {
    Idle,
    Queued,
    Running,
    Waiting,
    Succeeded,
    Failed,
    Skipped,
    Cancelled,
}

enum class VisualValidation {
    Valid,
    Warning,
    Invalid,
}

enum class VisualFocus {
    None,
    Focused,
    Selected,
    Editing,
}

enum class VisualAvailability {
    Enabled,
    Disabled,
    Unavailable,
}

enum class VisualFreshness {
    Current,
    Historical,
    Stale,
}

enum class VisualDensity {
    Minimal,
    Compact,
    Detailed,
}

enum class VisualInteractionMode {
    ReadOnly,
    Inspect,
    Edit,
    Connect,
    Drag,
}

enum class VisualRuntimeMode {
    None,
    DryRun,
    LiveRun,
}

data class VisualContext(
    val projection: ProjectionKind,
    val density: VisualDensity = VisualDensity.Compact,
    val interactionMode: VisualInteractionMode = VisualInteractionMode.Inspect,
    val runtimeMode: VisualRuntimeMode = VisualRuntimeMode.None,
    val zoom: Float = 1f,
) {
    init {
        require(zoom.isFinite() && zoom > 0f) {
            "VisualContext zoom must be finite and positive."
        }
    }
}

data class VisualSemanticState(
    val role: VisualRole,
    val authority: VisualAuthority = VisualAuthority.Canonical,
    val certainty: VisualCertainty = VisualCertainty.Known,
    val activity: VisualActivity = VisualActivity.Idle,
    val validation: VisualValidation = VisualValidation.Valid,
    val focus: VisualFocus = VisualFocus.None,
    val availability: VisualAvailability = VisualAvailability.Enabled,
    val freshness: VisualFreshness = VisualFreshness.Current,
) {
    val isInteractive: Boolean
        get() = availability == VisualAvailability.Enabled
}

fun interface VisualSemanticAdapter<T> {
    fun map(value: T, context: VisualContext): VisualSemanticState
}
