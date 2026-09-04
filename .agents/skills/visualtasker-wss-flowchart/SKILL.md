---
name: visualtasker-wss-flowchart
description: Guides Flowchart editor/projection work in VisualTasker Studio WSS. Use for IR-derived FlowGraph, FlowView, routing, layout, runtime traces, node editing, and Workflow mutation commands.
disable-model-invocation: true
---

# VisualTasker WSS Flowchart

## Rules

- Derive Flowchart from IR/Workflow contracts, not local UI shadows.
- Keep `FlowGraphDocument` semantic and `FlowViewDocument` visual.
- Layout/routing may not alter Workflow semantics.
- Flowchart editing must use explicit Workspace mutation commands.
- WSS Flowchart editor actions must use `FlowchartWorkspaceMutation`.
- Unknown or unsupported nodes remain visible and diagnosable.
- Runtime trace data is overlay state, not graph structure.

## Layout Expectations

- Main stem preferably vertical.
- Branches stair-step to the side.
- Connections are Manhattan/orthogonal.
- Keep edges short where possible.
- Avoid node overlap and avoid routing through nodes.
- Preserve stable positions and avoid spontaneous view resets.
