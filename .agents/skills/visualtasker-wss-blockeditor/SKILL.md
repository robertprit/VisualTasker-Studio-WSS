---
name: visualtasker-wss-blockeditor
description: Guides native BlockEditor work in VisualTasker Studio WSS. Use for blocks, reporters, slots, docking, snapping, undo/redo, palette, layout, rendering, and EMScript/IR export.
---

# VisualTasker WSS BlockEditor

## Rules

- `WorkspaceDocument` owns block truth.
- Shared workflow state synchronizes BlockEditor, EMScript/TextEditor, and Flowchart.
- UI Composables never mutate the document directly.
- Persistent changes go through actions/reducers/history.
- DragMove is transient and must not rebuild semantic state at 60fps.
- Drop creates one history entry; pointer movement does not.
- Hit testing uses layout indexes/spatial indexes, not recursive tree search.
- EMScript is generated from Workspace/IR/generators, not UI string concatenation.
- Flowchart is derived from IR where possible; do not add a private BlockEditor-only flowchart truth.
- Unsupported blocks remain visible and diagnosable.
- Use `VisualTaskerCommandCatalog` for command metadata, canonical names, aliases, capabilities, block bindings, flowchart bindings, and runtime bindings.

## UI Expectations

- Reporter slots and docking candidates must be visible enough on small screens.
- Container branches must size from measured children.
- Collapsed blocks must reduce visual footprint where meaningful.
- Siderail/palette should maximize canvas visibility and use panel-owned rail behavior.
- Haptic/sound feedback remains host-configurable.

## Related Skills

- Use `visualtasker-wss-workflow-graph` for mutation authority, ports, branches, source mapping, and serialization invariants.
- Use `visualtasker-wss-emscript` for grammar, command syntax, importer/exporter, and dry-run behavior.
- Use `visualtasker-wss-plugin-host` for shell session/save/dirty/toolbar contracts.
