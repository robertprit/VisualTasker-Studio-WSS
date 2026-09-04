---
name: visualtasker-wss-orchestrator
description: Coordinates VisualTasker Studio WSS architecture and cross-module work. Use for migration slices spanning Workspace Shell, plugins, Workflow, Worldview, EMScript, Flowchart, BlockEditor, Vision, Runtime, settings, and legacy Studio transfer.
---

# VisualTasker WSS Orchestrator

## Rules

- Read `VISUALTASKER_ARCHITECTURE_CONTRACT.md` before architecture-changing work.
- Use `.agents/SKILLS.md` to choose the smallest relevant domain skill set.
- Treat `/home/main/Schreibtisch/VisualTaskerStudio(Alt)` as read-only legacy reference.
- Transfer behavior and contracts into WSS-native plugins; do not copy lifecycle-coupled legacy code wholesale.
- Keep Workspace Shell responsible for panel lifecycle, not Workflow, Worldview, or Runtime truth.
- Route mutations through typed contracts, validators, reducers, and projections.
- Keep WorkflowGraph/WorkspaceDocument as Intent and Worldview as Reality/Knowledge.
- Keep Runtime as the bridge that executes accepted intent and produces trace/evidence/records.
- Keep AI/RAG as proposal and context providers, not mutation authority.
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

## Related Skills

- Workflow authority: `visualtasker-wss-workflow-graph`
- Plugin/session contracts: `visualtasker-wss-plugin-host`
- EMScript grammar/runtime bridge: `visualtasker-wss-emscript`
- Editor UI/projections: `visualtasker-wss-blockeditor`, `visualtasker-wss-flowchart`
- Reality/resource/context: `visualtasker-wss-worldview`, `visualtasker-wss-resources-data`, `visualtasker-wss-perugger-context`
- Providers/runtime: `visualtasker-wss-perception`, `visualtasker-wss-runtime-capabilities`
- Validation and git hygiene: `visualtasker-wss-quality-ops`
