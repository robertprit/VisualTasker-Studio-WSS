# Recorder Ecology: WindowContext, Observation Policies And Adaptive Perception

## Status

The WindowContext/WindowTransition slice is implemented and verified on device.
This document maps the resulting recorder/perception boundary. It intentionally
does not promote recordings into workflow truth.

Device verification on 2026-09-17 confirmed a recording baseline, structured
window/activity transitions between VisualTasker WSS and Android Settings, and
strict session termination: subsequent window events did not append evidence
after `RecordingEventStore.stop()`.

Canonical ownership, identity, evidence references, replay bookmarks and
interrupted-session semantics are specified in
[`RECORDER_OWNERSHIP.md`](RECORDER_OWNERSHIP.md).

## Architecture Rules

```text
Activity != Window
Window != Surface
Surface != Scene
Scene != Entity

Signal != Observation
Observation != Entity
Observation != Worldview Truth

Recording != Record
Record != Workflow
Workflow != Runtime

Perception != Inspector
Chart != Data Source
AI Proposal != Canonical Mutation
```

No `RecorderManager`, `PerceptionManager`, `WindowManager`, or other god object
should be introduced. Existing provider, repository, runtime, worldview and
projection boundaries stay authoritative.

## Existing Architecture Map

| Type / File | Responsibility | Module | Current Consumers |
| --- | --- | --- | --- |
| `RecordingEventStore` | JSONL recording sessions, active recording status, raw overlay/external/accessibility events, conversion to `RecorderStepUi` | `workspace.model` | Floating overlay, Accessibility service, RailTrace, Recorder tests |
| `RecordingSessionCoordinator` / `RecorderSessionStore` | Kanonische Room-Session mit Scenes, Frames, A11y-Snapshots, RawEvents und typisierten Canonical Interactions | `recording` | Floating overlay, Playback, Review, Recovery |
| `RecordingPlaybackDocument` | Validierte read-only Record-Projektion einer persistierten kanonischen Session | `recording` | RailTrace, Scene Inspector, StepCandidateAssembler |
| `StepCandidateDocument` / `ReviewedStepDocument` | Von der Historie getrennte Vorschlags- und Review-Ebene | `recording` | Scene Inspector, Junktor-Vorbereitung |
| `RecordingSessionWriter` | Timestamped line writer for active recording sessions | `workspace.model` | `RecordingEventStore` only |
| `RecorderStepUi` | UI-facing step/event projection with timestamp, bounds, point, activity and properties | `workspace.model` | RailTrace, Canvas, Flow recording projection, Junktor, tests |
| `RecordingTraceProjection` | Converts record steps to `ExecutionTrace` and RailTrace-compatible steps | `workspace.model` | RailTrace/record replay path |
| `WatchDogTraceProjection` | Filters provider/system events into WatchDog trace lanes | `workspace.model` | RailTrace live/watchdog path |
| `RecorderObservationProjection` | Converts visual recorder steps into `WorldObservation` evidence | `workspace.model` | Datastore/Worldview/Canvas projections |
| `WorldviewContracts` | World entities, observations, events, steps, scenes, resources, ambiguity contracts | `workspace.model` | Datastore, Canvas, Vision, future RAG/AI |
| `CanvasVisionObservationFactory` | Vision/scan output to `WorldObservation` | `workspace.model` | Vision/Canvas bridge |
| `CanvasObservationProjector` | Provider-styled projection of observations for Canvas | `workspace.model` | Canvas panel / Workspace canvas overlays |
| `ExecutionTrace` / `OperationResult` | Runtime/replay/watchdog operation chain | `emscript.runtime` | RailTrace, Flow runtime mapping, tests |
| `RailTraceModels` | Time-based rail projection modes, tracks and source families | `workspace.model` | RailTrace panel |
| `VisualTaskerAccessibilityService` | Accessibility events and runtime action adapter; screenshot capture adapter | `accessibility` | Runtime, RecordingEventStore, overlay status |
| `StudioOverlayService` | Floating toolbar/panels, recording controls, overlay events and screenshots | `overlay` | RecordingEventStore, user-facing floating controls |
| `RecordingFlowchartProjector` | Recording steps as flowchart projection | `flowchart` | Flow/record visualization |

## Existing Layer Mapping

```text
Raw signal:
  AccessibilityEvent, overlay button event, external Tasker/plugin event

Recording event:
  JSONL line in RecordingEventStore

Step:
  RecorderStepUi

Trace:
  ExecutionTrace / OperationResult via RecordingTraceProjection or WatchDogTraceProjection

Observation:
  WorldObservation via RecorderObservationProjection or CanvasVisionObservationFactory

Canvas projection:
  CanvasObservationProjection

Record:
  PersistedRecordingSession / RecordingPlaybackDocument sind der kanonische
  Record. Die korrelierte JSONL-Datei bleibt append-only Raw-Evidence fuer
  Activity-, Accessibility-, Overlay- und Provider-Ereignisse.

Workflow proposal:
  JunktorSeed.fromRailTraceStep exists as an early proposal seed.
```

## Gap Analysis

### 1. WindowContext / WindowTransition

Problem: Accessibility window events are recorded as `activity.change` and
`app.foreground`, but there is no explicit evidence contract distinguishing
activity, window, focus, keyboard, dialog or overlay transitions.

Existing reusable component: `RecordingEventStore.recordAccessibilityEvent`,
`RecorderStepUi.properties`, `WorldObservation`, `ExecutionTrace`.

Missing contract: small immutable `WindowSnapshot`, `WindowContext`,
`WindowTransitionEvidence`.

Smallest required change: normalize accessibility window events into structured
recording attributes:

```text
evidence.kind=window.transition
transitionType=ACTIVITY_CHANGED | WINDOW_CHANGED | FOCUS_CHANGED | UNKNOWN
packageName=...
activityName=...
windowClass=...
sceneBoundaryCandidate=true|false
confidence=...
```

No workflow mutation and no worldview truth mutation.

### 2. Observation Policy

Problem: Record, WatchDog, Runtime, Inspect and Probe are behavior modes, but
currently not explicitly represented.

Existing reusable component: Rail surface modes, WatchDog/Recording trace split,
runtime capability gates.

Missing contract: `ObservationPolicy`.

Smallest required change: add a lightweight policy enum and attach policy names
to future perception/recording requests and events. Do not build a planner yet.

Recommended values:

```text
RECORD
WATCHDOG
RUNTIME
INSPECT
PROBE
```

### 3. CaptureFrame

Problem: Accessibility screenshot capture exists, and screenshots/resources are
used by Canvas/Vision, but no shared fresh-frame contract exists.

Existing reusable component: `VisualTaskerAccessibilityService.takeScreenshotTo`,
workspace screenshot assets, Canvas resources.

Missing contract: `CaptureFrameRef` / `CaptureFrameProvider` later.

Smallest required change: do not touch this in the first slice. Capture reuse is
important but not required for WindowTransition evidence.

### 4. GroundingCandidate

Problem: Clicks and gestures are currently mostly raw bounds/points plus labels.
They are not yet grounded against Accessibility/OCR/OCV/YOLO/WorldEntity
candidates.

Existing reusable component: `WorldObservation`, `WorldRelationKind.ResolvesTo`,
`WorldAmbiguity`, Junktor seed.

Missing contract: `GroundingCandidate`.

Smallest required change: defer until after WindowTransition evidence. The first
candidate source should be Accessibility bounds/text, not OCR/YOLO.

### 5. Record Contract

Implemented: `PersistedRecordingSession` stores sessions, scenes, frames,
assets, A11y snapshots, raw events and tap interactions transactionally.
`RecordingPlaybackDocument` validates and projects this immutable history;
`StepCandidateDocument` and `ReviewedStepDocument` keep interpretation and
human review separate from raw history.

Current gap: only taps are first-class canonical interactions. Activity/window,
text, scroll, swipe and other gestures are retained as correlated JSONL evidence
and must be promoted through explicit schema migrations instead of ad-hoc UI
conversion.

## Proposed Data Flow

Recording path:

```text
AccessibilityEvent / OverlayEvent / RuntimeEvent
  -> canonical Room record plus correlated timestamped JSONL evidence
  -> RecordingPlaybackDocument plus evidence projection
  -> RecorderStepUi / RailTrace
  -> RecordingExecutionTrace / RailTrace
  -> optional WorldObservation evidence
  -> future RecordDocument
  -> future WorkflowProposal via Junktor
```

Worldview evidence path:

```text
Evidence event
  -> WorldObservation / WorldEvent candidate
  -> Ambiguity / GroundingCandidate / SceneBoundaryCandidate
  -> human or policy validation
  -> Worldview mutation only after acceptance or stable rule
```

## Observation Policy Design

| Policy | Goal | Preferred Inputs | Expensive Analysis |
| --- | --- | --- | --- |
| `RECORD` | Max reconstruction with acceptable cost | Input, window/activity, accessibility, screenshot references, targeted scans | Allowed only when evidence requires it |
| `WATCHDOG` | Cheap situational awareness | Activity/window changes, provider events, known expectations | Escalate only on deviation |
| `RUNTIME` | Observe what active workflow needs | Active node/condition/capability context | Only for required conditions |
| `INSPECT` | Rich developer/user view | All requested provider layers | Explicitly allowed |
| `PROBE` | Answer one concrete question | Smallest provider that can answer | Never triggers full stack automatically |

The policy should configure provider requests; it must not create a second
perception pipeline.

## WindowContext Integration

Minimal contracts for a later code slice:

```kotlin
enum class ObservationPolicy {
    RECORD,
    WATCHDOG,
    RUNTIME,
    INSPECT,
    PROBE,
}

data class WindowSnapshot(
    val id: String,
    val packageName: String?,
    val activityName: String?,
    val title: String?,
    val bounds: WorldviewRect?,
    val focused: Boolean,
    val active: Boolean,
    val displayId: Int?,
    val timestampMs: Long,
    val properties: Map<String, String> = emptyMap(),
)

data class WindowContext(
    val packageName: String?,
    val activityName: String?,
    val windows: List<WindowSnapshot>,
    val focusedWindowId: String?,
    val activeWindowId: String?,
    val displayId: Int?,
    val timestampMs: Long,
)

enum class WindowTransitionKind {
    ACTIVITY_CHANGED,
    WINDOW_APPEARED,
    WINDOW_DISAPPEARED,
    FOCUS_CHANGED,
    BOUNDS_CHANGED,
    KEYBOARD_APPEARED,
    SYSTEM_OVERLAY_APPEARED,
    UNKNOWN,
}

data class WindowTransitionEvidence(
    val kind: WindowTransitionKind,
    val previous: WindowSnapshot?,
    val current: WindowSnapshot?,
    val reason: String,
    val timestampMs: Long,
    val confidence: Float,
    val sceneBoundaryCandidate: Boolean,
    val properties: Map<String, String> = emptyMap(),
)
```

These types should live in `workspace.model` or a small perception model package.
They should be pure data contracts, not services.

## Minimal Implementation Slice

### Scope

Window/activity transition evidence through the existing recorder path.

### Files

Expected code slice:

- `app/src/main/java/com/visualtasker/wss/workspace/model/RecordingPerceptionModels.kt`
- `app/src/main/java/com/visualtasker/wss/workspace/model/RecordingEventStore.kt`
- `app/src/test/java/com/visualtasker/wss/workspace/model/RecordingEventStoreTest.kt`
- `docs/ROADMAP.md`

### Data Flow

```text
AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
  -> WindowTransitionEvidence.ACTIVITY_CHANGED
  -> RecordingEventStore JSONL attributes
  -> RecorderStepUi(actionType="window.transition" or existing "activity.change")
  -> RailTrace Records/WatchDog lanes
  -> no WorkflowDocument mutation
  -> no automatic Worldview truth mutation
```

### Acceptance Criteria

1. Recording can start normally.
2. An accessibility window/activity event is captured.
3. The event line contains timestamp, package/activity/window evidence.
4. The event line contains a transition kind and confidence.
5. The event maps to a `RecorderStepUi`.
6. The step stays consumable by RailTrace and WatchDog.
7. No workflow graph mutation happens.
8. No worldview truth mutation happens.
9. OCR/OCV/YOLO are not required.
10. Tests verify the transition metadata survives JSONL -> Step conversion.

### Implemented Slice Notes

- The active `RecordingSessionWriter` keeps the previous normalized
  `WindowContext` for the current recording session.
- When the overlay recorder starts and the AccessibilityService is connected,
  the initial baseline is captured from the current interactive window list.
- If no service snapshot is available at start, the first observed
  window/activity event in an active recording is stored as
  `recording.evidence=window.baseline`; it does not emit a transition.
- Repeated identical window states are ignored by the structural transition path.
- Later diffs are serialized into the existing recorder JSONL as
  `recording.evidence=window.transition` plus `window.transitionKinds`,
  previous/current package/activity and confidence.
- The AccessibilityService uses `flagRetrieveInteractiveWindows` and normalizes
  `service.windows` into `WindowSnapshot` entries, so dialog/window
  appear/disappear can be detected from state diffs.

## Deferred

- CaptureFrame reuse.
- SurfaceFlinger/layer provider.
- OCR/OCV/YOLO escalation.
- First-class `RecordDocument`.
- GroundingCandidate and robust target generalization.
- Scene segmentation policy beyond a transition candidate flag.
- Railchart/ChartGraph.
- AI/Perugger in any hot path.
