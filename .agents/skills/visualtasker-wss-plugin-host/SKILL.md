---
name: visualtasker-wss-plugin-host
description: Guides plugin host and shell contract architecture in VisualTasker Studio WSS. Use for panel registration, plugin lifecycle, editor sessions, save/dirty/validation/toolbar contracts, capability exposure, permissions, versioning, compatibility, and host authority boundaries.
---

# VisualTasker WSS Plugin Host

## Purpose

Keep WSS a shell and contract host, not a hidden domain owner.

## Invariants

- Workspace Shell owns panel lifecycle, focus, z-order, drag/resize, docking,
  shell layout, and plugin session framing.
- Plugins publish through typed host contracts.
- Plugins must not create a second canonical Workflow or Worldview truth.
- Editor sessions own editor-local draft state and report dirty/save/validation
  status through shell contracts.
- Save acknowledgement must protect against stale writes.
- Toolbar actions must be explicit and scoped to the owning session/panel.
- Capability exposure must be explicit, versioned, and permission-aware.

## Current Contracts

Use `WorkspaceShellPluginContracts.kt` for shell-side session/save/dirty/
validation/toolbar/runtime state contracts.

Use editor-specific bridge classes for concrete integrations:

- BlockEditor: `BlockEditorShellPlugin.kt`
- Flowchart: `FlowchartShellPlugin.kt`

## Boundaries

- No runtime execution in the shell.
- No EMScript parser/compiler logic in panel chrome.
- No lifecycle-coupled legacy Studio code copied wholesale.
- No direct AI mutation through plugin UI.
