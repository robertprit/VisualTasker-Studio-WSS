# VisualTasker Studio WSS Worldview Architecture

## Purpose

The Worldview model is the shared domain for perception, recording, resources,
inspection, and later retrieval/AI context.

It answers:

```text
What is known, observed, assumed, missing, or conflicting about the current
world?
```

It complements the Workflow model:

```text
Workflow  = Intent: what should happen.
Worldview = Reality/Knowledge: what is known about what happened or exists.
Runtime   = bridge: executes intent against reality and produces observations.
```

## Authority Boundaries

- UI panels are projections and must not own persistent Worldview truth.
- Provider output is evidence, not truth.
- Recorder output is a Record/Observation first, not accepted Workflow.
- AI output is a proposal, not a mutation.
- Retrieval is a selection mechanism over Workflow/Worldview, not a separate
  data model.

## Core Concepts

### WorldEntity

Resolved object, concept, region, screen, window, visual object, UI element, or
future domain entity.

Entities may have:

- an instance identity inside a Scene
- a concept identity across Scenes or Runs
- evidence from multiple Observations
- relations to resources, workflow references, and records

### Observation

A single provider observation. Examples:

- Accessibility sees `Button("Login")`
- OCR reads `Logln` with confidence `0.72`
- OpenCV matches a template with confidence `0.91`
- User touched `x=481 y=912`

Observations are immutable evidence. They may conflict.

### Scene

A semantic and temporal state. It is not just Activity, Screenshot, Tree, or
Event list.

Examples:

- `LoginScene`
- `LoadingScene`
- `HomeScene`
- `ErrorDialogScene`

### Record

Persisted result of recording or runtime execution.

Records group Steps by Scene and keep Events, Observations, EntityRefs, and
Interpretations distinguishable.

### Resource

Persistent artifacts such as screenshots, crops, templates, marker regions,
datasets, generated assets, or saved scans.

The current `WorkspaceResourceBundle` is the first Resource slice. It is not the
complete Worldview model.

### Ambiguity

Explicit uncertainty or conflict. Ambiguity is first-class and should not be
encoded only as low confidence or `null`.

Typical ambiguity kinds:

- identity conflict
- selection ambiguity
- action intent ambiguity
- scene boundary ambiguity
- timing ambiguity
- provider conflict
- expectation divergence
- correction ambiguity

## Panel Responsibilities

Each Worldview panel has one guiding question.

```text
SCENE      What is here?
INSPECTOR  What do we know about it?
RECORD     What happened?
MARKER     What did the user explicitly mark?
VISION     How can we recognize it?
DATA       What persistent artifacts do we have?
CONTEXT    What knowledge was retrieved and why?
AI/PERUGGER What does it mean, and what must human and machine clarify?
```

The same underlying Worldview objects must support multiple projections:

- by Scene
- by Entity type
- by Provider
- by Resource type
- by Workflow usage
- by Runtime trace
- by Retrieval context

## ScreenshotCanvas Role

ScreenshotCanvas is a shared Worldview tool. It should serve:

- Marker
- Template/Vision
- StepRecorder
- Inspector
- Runtime verification
- Code generation helpers

It must not become a separate truth model. Canvas-specific state is transient
unless persisted as Resource, Observation, or Annotation.

## AI And Retrieval

AI may:

- explain
- question
- interpret
- suggest
- compare
- diff
- propose

AI must not directly mutate Workflow or Worldview.

The path is:

```text
Ambiguity / Question
  -> Retrieval
  -> ContextBuilder
  -> Model Provider
  -> ProposedResolution
  -> Preview / Approval
  -> Validator
  -> Reducer
```

Retrieval order should prefer structured sources:

```text
ID -> metadata -> graph relations -> scene/time -> text -> embeddings
   -> visual similarity -> LLM
```

## Vertical Slice

Before broad UI work, prove the model with one vertical slice:

1. Recording creates raw Observations.
2. Observations are assigned to Entities.
3. Entities belong to a Scene.
4. Steps reference Entities, not only coordinates.
5. A Record produces a WorkflowProposal, not Workflow truth.
6. Runtime executes the accepted Workflow.
7. Runtime creates a new Record.
8. Expected and observed state can be compared.
9. Ambiguities can be created.
10. Human clarification can resolve an Ambiguity.

## Current Scope

Implemented foundation:

- `WorkspaceWorkflowState` already separates Workflow projections from panel UI.
- `WorkspaceResourceBundle` stores the initial Resource slice.
- Legacy Studio screenshot/template/marker imports map into Resources.
- Flowchart runtime diagnostics already show Runtime evidence as trace data.
- `WorldviewDocument` defines Scene, Entity, Observation, Relation, Event, Step,
  Record, Ambiguity, and Resource references.
- `WorldviewReducer` provides the first immutable update path.
- `WorkspaceResourcesWorldviewProjector` projects Resources into Worldview
  Entities, Observations, and Relations.
- `WorkspaceWorkflowState` carries the Worldview projection beside Workflow,
  EMScript, IR, Flowchart, and Resources.
- `WorldviewInspectorProjector` and `WorldviewDataProjector` provide the first
  shared read models for Inspector/Data panels.

Still missing:

- ScreenshotCanvas to Resource/Observation bridge
- Context panel for retrieval traces
- WorkflowProposal / ProposedResolution reducers
- runtime Record comparison against accepted Workflow intent
