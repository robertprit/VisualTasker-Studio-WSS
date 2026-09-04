---
name: visualtasker-wss-orchestrator
description: Coordinates VisualTasker Studio WSS architecture and cross-module work. Use for migration slices spanning Workspace Shell, plugins, Workflow, Worldview, EMScript, Flowchart, BlockEditor, Vision, Runtime, settings, and legacy Studio transfer.
disable-model-invocation: true
---

# VisualTasker WSS Orchestrator

## Rules

- Read `VISUALTASKER_ARCHITECTURE_CONTRACT.md` before architecture-changing work.
- Treat `/home/main/Schreibtisch/VisualTaskerStudio(Alt)` as read-only legacy reference.
- Transfer behavior and contracts into WSS-native plugins; do not copy lifecycle-coupled legacy code wholesale.
- Keep Workspace Shell responsible for panel lifecycle, not Workflow, Worldview, or Runtime truth.
- Route mutations through typed contracts, validators, reducers, and projections.
- Preserve dirty worktrees. Stage/commit only explicitly requested paths.

## Domain Split

```text
Workflow = Intent
Worldview = Reality / Knowledge
Runtime = bridge between Intent and Reality
```

## Preferred Sequence

1. Inspect current WSS contracts and tests.
2. Inspect legacy Studio only for behavior/reference.
3. Name the authority boundary.
4. Make the smallest WSS-native contract change.
5. Add focused tests.
6. Run focused tests, then app compile.
7. Install and launch when Android behavior changed and a device is connected.
