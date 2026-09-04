# VisualTasker Studio WSS Flowchart Prompt

Build Flowchart as a WSS-native editor and projection, not as a second workflow
truth model.

Rules:

- Derive Flowchart from IR / WorkspaceDocument / Worldview contracts.
- Keep `FlowGraphDocument` semantic and `FlowViewDocument` visual.
- Layout, routing, collapse, regions, and dummy nodes are view facets unless an
  explicit Workspace mutation command is accepted.
- WSS editor actions use `FlowchartWorkspaceMutation` for node add/delete, edge
  connect/disconnect, field changes, branch edits, and view-position sync.
- Runtime traces are overlays and evidence, not graph structure.
- Unsupported commands, missing plugin definitions, and unknown source elements
  remain visible and diagnosable.

Rendering and interaction expectations:

- main stem preferably vertical
- branches stair-step to the side
- loop-back edges route left
- if/elseif/else branch edges must not become left loop-back routes merely
  because they live inside a cycle
- connections are Manhattan / orthogonal
- keep edges short
- avoid node overlap
- avoid routing through nodes
- preserve stable positions and do not reset view spontaneously
- use larger magnetic ports for touch devices

Before changing rendering behavior, add or update deterministic layout/routing
tests for the affected shape.

Milestone split:

- M1: IR structure and semantic validation.
- M2: editor commands through `FlowchartWorkspaceMutation`.
- M3: auto-arrange, auto-routing, stable lanes, crossing reduction, and visual
  polish.
- M4: explicit IR-derived editor facets for regions, collapse groups, variable
  bulk, comment markers, and function regions.
- M5: EMScript command catalog schema diagnostics and registry hardening.
