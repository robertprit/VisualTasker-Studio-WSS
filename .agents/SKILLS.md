# VisualTasker Studio WSS Skill Router

Use this file to choose the smallest relevant project skill. Skills are
architecture guardrails, not implementation prompts. Masterprompts describe
target outcomes; skills describe durable invariants and boundaries.

## Primary Router

- Cross-domain architecture, migration slices, milestone planning, or authority
  questions: `visualtasker-wss-orchestrator`
- Workflow intent, command/reducer semantics, ports, branches, validation,
  source mapping, or editor projection authority: `visualtasker-wss-workflow-graph`
- EMScript grammar, parser/importer, command catalog, code generation, dry-run
  runtime, or canonical command syntax: `visualtasker-wss-emscript`
- Block rendering, docking, snapping, reporters, slots, palette, undo/redo, or
  block workspace layout: `visualtasker-wss-blockeditor`
- FlowGraph, IR projection, layout, Manhattan routing, runtime trace overlays,
  ports, facets, or Flowchart editor interactions: `visualtasker-wss-flowchart`
- Scene, Entity, Observation, Relation, Record, Resource, Ambiguity, Inspector,
  Context, or RAG boundaries: `visualtasker-wss-worldview`
- Screenshot, Accessibility, OCR, OpenCV, YOLO, DOM, template matching,
  coordinate spaces, confidence, or provider observations: `visualtasker-wss-perception`
- Runtime execution, capability gates, provider selection, permissions,
  fallback, timeout, cancellation, recovery, or operation results:
  `visualtasker-wss-runtime-capabilities`
- StepRecorder, recording modes, replay, timeline, event interpretation, or
  persisted record shape: `visualtasker-wss-recording-record`
- Human-machine perception debugging, ambiguity questions, retrieval context,
  prompt/context builders, model routing, or proposed resolutions:
  `visualtasker-wss-perugger-context`
- Visual semantic mapping, Material 3 Expressive policy, semantic colors,
  shapes, badges, motion, opacity, or styling boundaries:
  `visualtasker-wss-visual-abstraction`
- Resource identity, screenshots, templates, markers, datasets, project files,
  provenance, ownership, or lifecycle: `visualtasker-wss-resources-data`
- Plugin lifecycle, panel registration, session/save/dirty/validation/toolbar
  contracts, capability exposure, versioning, or compatibility:
  `visualtasker-wss-plugin-host`
- Build/test/install/smoke gates, dirty repo handling, submodules, migration
  safety, serialization roundtrips, or release hygiene:
  `visualtasker-wss-quality-ops`

## Cross-Domain Loading

For cross-domain work, start with `visualtasker-wss-orchestrator`, then load
only the directly affected domain skills. Do not load every skill by default.

Common pairs:

- EMScript and BlockEditor sync: `visualtasker-wss-workflow-graph`,
  `visualtasker-wss-emscript`, `visualtasker-wss-blockeditor`
- Flowchart as editor: `visualtasker-wss-workflow-graph`,
  `visualtasker-wss-flowchart`, `visualtasker-wss-plugin-host`
- Runtime execution from visual editors: `visualtasker-wss-workflow-graph`,
  `visualtasker-wss-runtime-capabilities`, `visualtasker-wss-quality-ops`
- Marker/template/vision work: `visualtasker-wss-worldview`,
  `visualtasker-wss-perception`, `visualtasker-wss-resources-data`
- RAG/context work: `visualtasker-wss-worldview`,
  `visualtasker-wss-perugger-context`, `visualtasker-wss-resources-data`

## Canonical Documents

- Architecture contract: `VISUALTASKER_ARCHITECTURE_CONTRACT.md`
- Prompt and skill state: `docs/WSS_PROMPTS_AND_SKILLS.md`
- Worldview: `docs/WORLDVIEW_ARCHITECTURE.md`
- Visual abstraction: `docs/VISUAL_ABSTRACTION_LAYER.md`
- Flowchart module architecture: `visualtasker-flowchart/docs/ARCHITECTURE.md`

## Hard Boundaries

- Workflow is Intent.
- Worldview is Reality / Knowledge.
- Runtime bridges Intent and Reality.
- Editors are projections and controlled mutation surfaces, not independent
  canonical truth.
- AI and RAG may propose, explain, retrieve, and diff; they must not silently
  mutate Workflow or Worldview.
- Layout, panel state, styling, and transient interaction state are not
  workflow semantics.
