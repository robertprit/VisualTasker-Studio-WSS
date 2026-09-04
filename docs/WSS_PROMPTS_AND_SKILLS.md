# WSS Prompts And Skills

## Decision

The old VisualTasker Studio prompts and skills are reference material, not a
drop-in authority for WSS.

WSS keeps the domain rules and quality gates, but replaces old package/module
assumptions with the WSS plugin/shell architecture.

## Reused Principles

- Modular panels and plugins.
- Material 3 Expressive UI.
- Workflow truth is not owned by UI.
- Editors publish drafts or validated mutations through contracts.
- Runtime execution is capability-gated.
- Unsupported and unknown elements stay visible and diagnosable.
- Build, test, install, and smoke after implementation slices.

## Updated Principles

- `WorkflowDocument` is the Intent source of truth.
- `Worldview` is the Reality/Knowledge source of truth.
- `Runtime` bridges Workflow and Worldview.
- `IrGraph` and Flowchart are projections/read models unless an explicit
  mutation command is routed through Workspace reducers.
- Flowchart can become an editor only through typed Workspace mutation commands,
  not through direct EMScript or graph rewrites.
- IR hardening must validate structure and semantics: source mapping, scopes,
  branches, data-flow, ports, facets, and deterministic branch order.
- Flowchart edit actions must enter the Workspace through
  `FlowchartWorkspaceMutation`.
- `WorkspaceResourceBundle` is a Resource slice of Worldview, not the full
  Worldview.
- Marker and Template are ScreenshotCanvas/Vision helper functions, not isolated
  panel truth models.
- RAG is deferred until Worldview/Resource/Record/Inspector are stable.

## Rejected Legacy Assumptions

- Do not use the old package name `com.phuntasker.visualtaskerstudio`.
- Do not recreate old monolithic MainScreen behavior as the default WSS mode.
- Do not make Shizuku or Root the default execution path.
- Do not port V10 accessibility or legacy Studio lifecycle coupling.
- Do not expose deprecated `MOBILE_SAM` as a new active UX mode.

## Project Skills To Keep As WSS Variants

- `visualtasker-wss-orchestrator`
- `visualtasker-wss-quality-ops`
- `visualtasker-wss-worldview`
- `visualtasker-wss-emscript`
- `visualtasker-wss-flowchart`
- `visualtasker-wss-blockeditor`

Future skills once implementation starts:

- `visualtasker-wss-vision`
- `visualtasker-wss-tasker-bridge`
- `visualtasker-wss-custom-tabs`
- `visualtasker-wss-rag-context`

## Project Prompts

Legacy prompts inspected as reference:

- `visualtasker-studio-master.prompt.md`
- `flowchart-masterprompt-agent.md`
- `blockeditor-masterprompt-agent.md`

WSS-native prompts added:

- `.agents/prompts/visualtasker-wss-master.prompt.md`
- `.agents/prompts/flowchart-wss-master.prompt.md`
- `.agents/prompts/worldview-wss-master.prompt.md`

The WSS prompts intentionally keep the old quality expectations while replacing
old Studio package, lifecycle, and monolithic MainScreen assumptions with the
Workspace Shell plugin architecture.

## Skill Trigger Guidance

Use `visualtasker-wss-orchestrator` for cross-module migration and architecture
work.

Use `visualtasker-wss-worldview` for Scene, Entity, Observation, Record,
Resource, Ambiguity, Inspector, Data, Context, RAG boundaries, ScreenshotCanvas,
Marker, Template, Vision, and Recorder work.

Use editor-specific skills when work is limited to one editor plugin.

Use `visualtasker-wss-quality-ops` before finalizing implementation work.

## Current Milestone State

- M1 IR Graph hardening: structure and semantic validation are active for
  source mapping, scopes, branch body/condition edges, ports, facets, and
  deterministic branch ordering.
- M2 Flowchart editor foundation: Flowchart node add/delete, edge
  connect/disconnect, port connect, field update, branch edit, and view-position
  sync are routed through `FlowchartWorkspaceMutation`.
- M3 Flowchart layout/routing: branch edges stay branch-routed even when cycle
  detection marks them as back edges, loop-back routing remains left-sided,
  value/condition nodes reserve a real side lane, and automatic Manhattan
  routes prefer collision-free candidates around node rectangles.
- M4 editor facets: IR branch regions, collapse groups, variable bulk regions,
  comment markers, and function regions are projected as explicit Flowchart
  facet nodes with stable `flowFacetRole` metadata.
- M5 command catalog: the EMScript command registry exposes schema diagnostics
  for duplicate ids, duplicate block bindings, missing categories, argument
  clashes, runtime capability mismatches, and missing return types.
