---
name: visualtasker-wss-flowchart
description: Guides Flowchart editor/projection work in VisualTasker Studio WSS. Use for IR-derived FlowGraph, FlowView, routing, layout, runtime traces, node editing, and Workflow mutation commands.
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
- Use `FlowRuntimeSnapshot.extensions` for WSS-specific runtime events and variable summaries.
- Treat facets, regions, synthetic joins, collapse groups, comment markers, and variable bulks as visual/editor facets unless promoted by a domain contract.

## Layout Expectations

- Main stem preferably vertical.
- Branches stair-step to the side.
- Connections are Manhattan/orthogonal.
- Keep edges short where possible.
- Avoid node overlap and avoid routing through nodes.
- Preserve stable positions and avoid spontaneous view resets.
- Data-flow and condition edges must not destabilize primary sequence ranking.
- Crossings should be reduced first; unavoidable crossings may use visual edge bridges.

## Related Skills

- Use `visualtasker-wss-workflow-graph` for source mapping, branch semantics, and flowchart mutation authority.
- Use `visualtasker-wss-plugin-host` for panel/session contract changes.
- Use `visualtasker-wss-runtime-capabilities` for run requests and capability-gated execution behavior.
