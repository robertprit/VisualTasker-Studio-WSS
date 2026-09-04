---
name: visualtasker-wss-blockeditor
description: Guides native BlockEditor work in VisualTasker Studio WSS. Use for blocks, reporters, slots, docking, snapping, undo/redo, palette, layout, rendering, and EMScript/IR export.
disable-model-invocation: true
---

# VisualTasker WSS BlockEditor

## Rules

- `WorkspaceDocument` owns block truth.
- UI Composables never mutate the document directly.
- Persistent changes go through actions/reducers/history.
- DragMove is transient and must not rebuild semantic state at 60fps.
- Drop creates one history entry; pointer movement does not.
- Hit testing uses layout indexes/spatial indexes, not recursive tree search.
- EMScript is generated from IR/generators, not UI string concatenation.
- Unsupported blocks remain visible and diagnosable.

## UI Expectations

- Reporter slots and docking candidates must be visible enough on small screens.
- Container branches must size from measured children.
- Collapsed blocks must reduce visual footprint where meaningful.
- Siderail/palette should maximize canvas visibility.
