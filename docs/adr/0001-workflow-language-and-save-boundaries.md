# ADR 0001: Workflow Language And Save Boundaries

## Status

Accepted

## Context

VisualTasker Studio WSS has multiple synchronized editors for the same automation
intent:

- BlockEditor
- Flowchart
- EMScript/TextEditor
- Stepper/Runtime views
- future Vision, Marker, Datastore, AI, and plugin panels

The implementation already separates intent, visual projections, runtime traces,
and Worldview evidence. The naming is still confusing in places:

- `WorkspaceDocument` currently acts as the workflow intent document.
- "Workspace" can also mean the Shell project surface and panel container.
- TextEditor shows both generated EMScript and a manual EMScript draft.
- Flowchart stores visual layout separately as `FlowViewDocument`.
- BlockEditor visually owns block positions, but must not be mistaken for the
  source of truth just because it was the first complete editor.

This causes confusing restart and save/load moments. A user may see a generated
workflow projection and an older manual text draft at the same time and assume
both are the same persisted state.

## Decision

WSS uses the following ubiquitous language.

### Project / Workspace Shell

The app-level project container. It owns panel sessions, editor instances,
project metadata, shell layout, user settings, marker resources, datastore
resources, and future RAG/AI context.

It is not the workflow intent document.

### WorkflowDocument

The canonical source of truth for automation intent: commands, control flow,
variables, scopes, branches, and block-level workflow structure.

In current code this role is still implemented by the type named
`WorkspaceDocument`. The architectural name is `WorkflowDocument`.

### BlockViewDocument

The visual BlockEditor representation of a WorkflowDocument. It may include
block positions, collapsed state, slots, visual grouping, palette/flyout state,
and BlockEditor-specific UI metadata.

The BlockEditor may mutate the WorkflowDocument through validated editor actions.
Its view data is not the whole workflow truth.

### FlowViewDocument

The visual Flowchart representation of a projected flow graph. It owns
positions, routes, bend points, viewport, pinned/manual layout state, and
Flowchart-specific UI metadata.

Saving the Flowchart view saves layout, not the workflow intent.

### IRGraph

The editor-neutral semantic graph derived from WorkflowDocument. It describes
nodes, edges, scopes, branches, data-flow, facets, source mapping, and runtime
trace anchors.

IRGraph is a projection/read model, not a second source of truth.

### FlowGraphDocument

The Flowchart-capable graph projection derived from IRGraph. It exists so the
Flowchart editor can render and edit graph concepts through typed mutations.

### EMScriptProjection

Generated EMScript from WorkflowDocument/IRGraph. This is the readable and
executable script projection of the canonical workflow.

### EMScriptDraft

The manual TextEditor content. It may differ from the generated projection.
It becomes workflow truth only after an explicit Apply/Import action succeeds
and replaces or updates the WorkflowDocument.

### WorldviewDocument

The source of truth for observed reality and knowledge: scenes, entities,
observations, records, resources, ambiguities, and relations.

Workflow describes what should happen. Worldview describes what is known or
observed. Runtime bridges both.

## Save Semantics

WSS distinguishes four save operations.

### Save Workflow

Persists WorkflowDocument, currently serialized from `WorkspaceDocument`.
This saves the automation intent.

### Save View

Persists visual editor state only:

- `BlockViewDocument` for BlockEditor-specific visual state
- `FlowViewDocument` for Flowchart layout, routes, viewport, and manual/pinned
  view state

View save must not silently rewrite workflow intent.

### Save Draft

Persists manual EMScript draft text and named EMScript files/samples.
Draft save must not silently replace WorkflowDocument.

### Save Project

Persists a complete WSS project snapshot:

- WorkflowDocument
- editor view documents
- EMScript drafts/named scripts
- panel sessions
- resources
- marker/template/datastore state
- Worldview state
- runtime/record metadata where appropriate

This is the desired user-facing project save model. It is broader than any
single panel save button.

## Editor Synchronization Rules

- BlockEditor changes must enter WorkflowDocument through validated document
  mutations.
- Flowchart content edits must enter WorkflowDocument through typed
  `FlowchartWorkspaceMutation` commands.
- Flowchart view edits mutate only `FlowViewDocument`.
- TextEditor generated code is a projection and should be labelled as such.
- TextEditor draft code is editable local text and should be labelled as draft.
- Applying EMScript parses/imports text and replaces or updates WorkflowDocument
  only after validation succeeds.
- Runtime traces are observations over a WorkflowDocument revision, not editor
  ownership.
- Worldview observations/resources may inform workflow proposals, but must not
  mutate WorkflowDocument without explicit reducer/approval path.

## Naming Consequences

Preferred architectural names:

- `WorkflowDocument`
- `WorkflowState`
- `BlockViewDocument`
- `FlowViewDocument`
- `IRGraph`
- `FlowGraphDocument`
- `EMScriptProjection`
- `EMScriptDraft`
- `WorldviewDocument`
- `ProjectSnapshot`

Current implementation compatibility:

- Existing `WorkspaceDocument` remains a concrete type for now.
- Documentation and new APIs should describe its role as WorkflowDocument.
- New code should avoid using "workspace" when it actually means workflow
  intent.
- Large renames should be incremental and facade-based, not a destabilizing
  project-wide refactor.

## Migration Path

1. Add documentation and UI labels that distinguish Workflow, View, Draft, and
   Project.
2. Introduce aliases/facades where useful, e.g. `WorkflowDocument` around the
   current `WorkspaceDocument`.
3. Rename save actions in UI where needed:
   - Save Workflow
   - Save Flow View
   - Save Draft
   - Save Project
4. Add a ProjectSnapshot model after Workflow, Flowchart, BlockEditor, Marker,
   Vision, Datastore, and Worldview persistence have stable contracts.
5. Only then perform deeper type/package renames if they reduce confusion
   without breaking plugin contracts.

## Consequences

Positive:

- Users can understand why generated EMScript and draft EMScript can differ.
- Auto-arrange can remain a deliberate FlowView reset without overwriting the
  WorkflowDocument.
- BlockEditor no longer appears to own the workflow simply because it renders
  the block structure.
- Flowchart can become a real editor while still routing semantic changes
  through workflow mutations.
- Project save/load can be designed cleanly instead of overloading panel save
  buttons.

Tradeoffs:

- Some existing class names remain imperfect until migration.
- UI may need temporary labels that expose implementation distinctions.
- Tests must be explicit about whether they validate workflow intent, view
  state, generated code, draft code, or project snapshots.

## Non-Goals

- No immediate monolithic rename of `WorkspaceDocument`.
- No change to runtime semantics.
- No automatic draft-to-workflow overwrite.
- No automatic FlowView auto-arrange on every first render.
- No promotion of IRGraph to source of truth.
