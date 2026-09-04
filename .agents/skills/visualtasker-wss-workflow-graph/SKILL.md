---
name: visualtasker-wss-workflow-graph
description: Guides Workflow/Workspace intent architecture in VisualTasker Studio WSS. Use for WorkflowDocument or WorkspaceDocument authority, command/reducer mutations, node and edge identity, ports, branches, value connections, validation, source mapping, serialization, roundtrips, and editor projection boundaries.
---

# VisualTasker WSS Workflow Graph

## Purpose

Protect the canonical automation-intent model.

```text
Workflow = Intent
Worldview = Reality / Knowledge
Runtime = bridge between Intent and Reality
```

## Invariants

- Workflow truth is owned by `WorkspaceDocument` / Workflow contracts, not by
  panels, layout, EMScript text, Flowchart view state, or AI proposals.
- Persistent mutations go through typed actions, validators, reducers, and
  shared workflow state.
- Editors may project and request controlled mutations. They must not create
  private persistent workflow truth.
- Ports, value inputs, statement inputs, branches, and connections define
  workflow structure.
- Source mapping must survive projection into EMScript, BlockEditor, Flowchart,
  diagnostics, and runtime traces.
- Unsupported or missing plugin definitions remain visible and diagnosable.

## Current WSS Bridge

Use `WorkspaceWorkflowState` as the shared synchronization point between
BlockEditor, EMScript/TextEditor, Flowchart, resources, and Worldview
projection.

Current projection path:

```text
WorkspaceDocument
  -> IrGraphGenerator
  -> EMScript projection
  -> IrGraphFlowchartProjector
  -> FlowGraphDocument
```

Flowchart edits enter the workspace through `WorkspaceFlowchartMutations` /
`FlowchartWorkspaceMutation`. BlockEditor edits enter through
`WorkspaceAction` / `WorkspaceReducer`.

## Boundaries

- Layout is not semantics.
- Drag is transient; drop may become a mutation.
- Runtime events are evidence/trace, not workflow authoring by themselves.
- Recording creates Records/Observations first, not accepted Workflow truth.
- AI may generate proposals, not silent workflow mutations.

## Validate

For workflow-shape changes, include focused tests for:

- deterministic node/edge/source ids
- branch order
- value/statement connection roundtrips
- serializer compatibility
- EMScript -> WorkspaceDocument -> IR -> Flowchart projection consistency
