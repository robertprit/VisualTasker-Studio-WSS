# Recorder Ownership And Identity

## Status

Implemented contract baseline. This document describes ownership; it does not
promote recorder history into workflow intent.

## Ownership Matrix

| Concept | Producer | Owner | Mutable By | Persistent | Stable ID | Time | References |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Recording process | `RecordingSessionCoordinator` | recorder runtime | coordinator only | no | active `sessionId` | monotonic + epoch | canonical session |
| RecordingSession | `RecordingSessionCoordinator` | `RecorderSessionStore` | transactional recorder store | Room | `sessionId` | start/stop | scenes by ID |
| RawRecordingEvent | capture/provider adapters | `RecorderSessionStore`; JSONL is correlated raw evidence | append-only recorder path | Room + correlated JSONL | `rawEventId` | epoch + monotonic + sequence | session by ID |
| Canonical Interaction | `RecordingInteractionCanonicalizer` or live coordinator | `RecorderSessionStore` | transactional recorder store | Room schema v3 | `interactionId` | source sequence + epoch + monotonic | one or more RawEvent IDs, Evidence IDs, typed payload |
| Record | `RecorderSessionStore` | `PersistedRecordingSession` | never through playback | Room/resources | `sessionId` | inherited | all members by ID |
| Step | `RecordingPlaybackProjector` | read-only `RecordingPlaybackDocument` | projection rebuild only | derived | `entry:<interactionId>` | event time + sequence | Evidence by ID |
| Scene | coordinator | canonical Record | coordinator/store | Room | `sceneId` | open/close + sequence | frame/window by ID |
| Evidence | playback projector | read-only Record projection | projection rebuild only | derived | `evidence:<interactionId>:<kind>` | source event time + sequence | resources by typed ID |
| Integrity report | `RecordingIntegrityValidator` | recorder domain | never; rebuilt read-only | derived | Record ID + stable references | none | traverses Step, Evidence, Interaction, RawEvent and Resource IDs |
| Screenshot | `ScreenshotAssetStore` | resource store | content-addressed insert only | file + Room metadata | SHA-256 `assetHash` | creation time | frame references hash |
| Replay | `RecordingPlaybackController` | replay runtime | replay controls only | bookmark only | Record ID + entry ID | phase position | read-only Record |
| Watchdog | watchdog providers | WatchDog trace | watchdog runtime | trace policy dependent | trace operation ID | provider time | evidence refs |
| DryRun | workflow runtime | execution trace | runtime only | trace policy dependent | run/operation IDs | runtime time | workflow source refs |
| WetRun | workflow runtime | execution trace | runtime only | trace policy dependent | run/operation IDs | runtime time | workflow source refs |

## Canonical Source

`PersistedRecordingSession` is the canonical persisted Record. The active
`RecordingSession` is its lifecycle root. JSONL remains append-only correlated
raw evidence and is not a second canonical Record.

```text
Recording process
  -> RecordingSession + canonical Room members
  -> PersistedRecordingSession (Record)
  -> RecordingPlaybackDocument (read-only Step projection)
  -> StepCandidateDocument (interpretation)
  -> StepReviewDecision (human correction, original preserved)
  -> ReviewedStepDocument
  -> future Workflow Proposal
  -> explicit approval
  -> WorkflowDocument
```

## Identity And Ordering

- IDs are generated once at capture time and never derived from pixels,
  Compose objects or render order.
- Canonical order uses sequence first, monotonic time second and stable ID as
  deterministic final tie-breaker.
- Epoch time is display/correlation time. Monotonic time is duration/order time.
- Repeated loads must yield byte-for-byte equivalent member ordering.
- `RecordingInteraction` has one common header and exactly one typed payload:
  `Tap`, `Swipe`, `Text`, `Window` or `Screenshot`.
- Interaction provenance is an ordered list of `rawEventIds`; import never
  invents meaning for unsupported or incomplete raw events.
- Sensitive text is represented as redacted text plus an optional hash. Clear
  text must not survive in a redacted canonical payload.

## Persistence And Migration

Room schema v3 replaces the tap-only table with `recording_interactions`.
Migration 2 -> 3 retains interaction ID, session, sequence, timestamps, scene
references, status, target reference and RawEvent provenance, then removes the
old table. JSONL import derives stable RawEvent IDs from file identity and line
index. The importer returns raw events and canonical interactions together so
they can be persisted atomically and audited in both directions.

## Evidence Ownership

A Step does not own screenshot bytes. It references Evidence; Evidence contains
typed references to raw events, scenes, A11y snapshots, capture frames and
content-addressed screenshot assets. Resources may therefore be reused by
markers, observations and datasets without copying.

## Integrity And Provenance

`RecordingIntegrityValidator` consumes the immutable Record and a derived
`RecordingIntegritySnapshot`. It reports missing RawEvents, Evidence and
Resources, broken Step-to-Evidence or Step-to-Interaction references, and orphan Evidence or
Interactions. It never repairs, deletes, persists or mutates data.

Provenance is rebuilt by traversing stable references:

```text
Step -> Evidence -> Interaction -> RawEvent
                 -> typed Resource
```

RailTrace receives `RecordingIntegrityStepReport` as a read-only projection for
its Evidence Inspector. Validation remains owned by the recorder domain.

## Lifecycle Boundaries

- `RECORDING`: active capture process.
- `RECORD`: immutable persisted history.
- `REPLAY`: timed read-only view of a Record.
- `DRY_RUN`: workflow execution under dry-run capability policy.
- `WET_RUN`: real workflow execution.
- `WATCHDOG`: ongoing observation, not recording and not workflow runtime.

Interrupted process recovery writes `INTERRUPTED`, not `COMPLETED` or generic
`PARTIAL`. Replay bookmarks restore selection, phase, speed and position, but
never resume playback automatically after process restart.

## Projection Boundary

Raw events such as `TOUCH_DOWN`, `TOUCH_UP` and `WINDOW_APPEARED` remain
evidence. Read-only timeline, text, block or flow views may display them. Only
the Interpretation -> Workflow Proposal -> Review path may create workflow
intent. No projection is allowed to mutate the Record or WorkflowDocument.
