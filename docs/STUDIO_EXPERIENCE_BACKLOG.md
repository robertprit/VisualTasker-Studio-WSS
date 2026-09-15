# Studio Experience Backlog

> Status: product/UX direction and integration backlog. This document is **not** a normative architecture contract.
>
> Purpose: preserve cross-cutting Studio-experience ideas without prematurely turning every idea into a new panel, domain model, or subsystem. Prefer integration into existing/planned panels, shared projections, or Settings where appropriate.

## Guiding principle

VisualTasker Studio should feel like one coherent development environment rather than a collection of panels.

The target experience is a traceable path through:

`Intent -> Execution -> Reality -> Evidence -> Diagnosis -> Correction -> Learning`

Existing authority boundaries remain unchanged:

- `WorkflowDocument = Intent truth`
- `Worldview = Reality / Knowledge about Reality`
- `Runtime = coupling between Intent and Reality`
- `Observation != Entity`
- `Prediction != Observation`
- `Proposal != Domain Command`
- UI projections do not own independent durable domain truth.

The items below should therefore normally be implemented as **projections, navigation, commands, inspectors, settings, or extensions of existing panels**, not as new sources of truth.

## Placement matrix

| Experience capability | Preferred home | Notes |
|---|---|---|
| Command Palette / Universal Search | Workspace Shell | Global typed search and command surface for workflows, capabilities, panels, resources, entities, records, settings and actions. Reuse the same command catalog for keyboard, touch and later natural-language mapping. |
| Project Explorer | Workspace / Project navigation | Navigation projection over workflows, resources, Worldview concepts/scenes, datasets, records, models, providers and tests. Must not become another persistence authority. |
| Problems / Diagnostics Center | Log Console + Workspace diagnostics projection | Aggregate parser, validator, runtime, provider, perception, model and dataset diagnostics. Selecting a problem should navigate to the responsible projection/source. |
| Semantic Inspector | Inspector / shared selection system | Universal “What is this?” view for selected workflow nodes, entities, observations, resources, records, runtime events and learning artifacts. Cross-links Workflow, Worldview, Runtime, Record and Learning without merging their authority. |
| Execution Control Center | Runtime / RailTrace controls | Shared Run, Pause, Step, Stop, Record, Follow, DryRun, Live, Replay controls; breakpoints and execution focus. RailTrace remains the detailed temporal projection. |
| History / Checkpoints | Workspace + persistence/versioning | Distinguish local Undo/Redo, Checkpoint, Revision, Run, Record and Model Version. Support navigation to previous successful execution context and comparisons. |
| Semantic Diff | Shared Workflow/Apply infrastructure | Generalize the existing EMScript Apply diff into semantic workflow/resource/provider-impact diffs. Reuse for AI proposals, imports, editor synchronization and revision comparison. |
| Dependency / Impact Explorer | Inspector / Project Explorer | “Find References” and “What breaks if this changes?” across workflows, entity concepts, resources, datasets, models and providers. Graph view is optional projection, not a new graph truth. |
| Workspace Presets | Workspace Settings | Named layouts such as Author, Debug, Vision, Learning, Runtime and Design. Persist panel arrangement and relevant view preferences, not domain state. |
| Context Actions | Workspace shared selection/action layer | Type-dependent Inspect, Find References, Open Source, Show in Flow, Show in RailTrace, Show Evidence, Show History, Add Breakpoint, Create Test, Ask AI. |
| Workflow Tests as Studio objects | Testing + Workflow tooling | User-level automation scenarios such as Given Scene/Entities -> Run Workflow -> Expect Action/Transition/Outcome. Potential `Record -> Create Test` workflow. Keep separate from VTS implementation/unit tests. |
| Asset / Resource Browser | Dataset Manager / Resource tooling + ShapeMaker integration | Browse templates, images, regions, shapes, animations and other reusable assets. ShapeMaker authors visual assets; VAL assigns semantic visual meaning. |
| Navigation history | Workspace Shell | Back/Forward, breadcrumbs, recent workflows/projects, bookmarks, favorites and selection history. |
| Go to Definition / Find References | Shared navigation + Inspector | Typed semantic navigation across editors and domain references. |
| Quick Documentation | Wiki/reference integration + Inspector | Contextual documentation for selected capabilities, EMScript constructs, block types, providers and concepts. |
| Graph overview / minimap | Blockeditor / Flowchart / RailTrace as applicable | Local projection feature for large documents; avoid a global subsystem. |
| Scratch Workflow / Playground | Workflow authoring | Temporary experimentation surface using normal parse/validate/dry-run safety gates. |
| Templates / Examples | Project/Workflow creation | Guided starting points and example projects; documentation and onboarding should link directly to them. |
| Keyboard / S-Pen shortcuts | Settings -> Input / Workspace | Configurable interaction mappings over the shared command catalog. |
| Command History | Workspace Shell | Recent commands/actions for repeatability and discoverability; not Runtime Record. |

## Suggested Workspace presets

Presets are compositions of existing tools, not new architecture layers.

- **Author** — Blockeditor, Flowchart, EMScript.
- **Debug** — Flowchart, RailTrace, Problems, Inspector.
- **Vision** — Scene/Screen, Inspector, Worldview, Resources.
- **Learning** — Dataset Manager, Perugger, Evaluation, Model Registry.
- **Runtime** — RailTrace, Log Console, Worldview, Capabilities.
- **Design** — ShapeMaker, Preview, Assets, VAL / Visual Semantics Inspector.

## Cross-cutting interaction contract candidate

A shared typed selection/reference mechanism would unlock much of the Studio experience without introducing new domain truth.

Candidate concepts:

- `StudioReference` — stable typed reference to a navigable Studio object.
- `StudioSelection` — current UI selection(s), session/projection state only.
- `StudioCommand` — discoverable executable UI/domain action with explicit authority/risk requirements.
- `NavigationTarget` — projection/location capable of revealing a referenced object.
- `ReferenceRelation` — definition/reference/dependency/evidence/history links used for navigation and impact analysis.

These are candidates for later audit/contract work; names and schemas are not frozen by this backlog.

## Problems and diagnostics experience

Diagnostics should be aggregated rather than trapped inside individual panels. Sources may include:

- EMScript parser/import/apply diagnostics,
- Workflow validator diagnostics,
- Blockeditor/Flowchart projection diagnostics,
- Runtime outcomes and failures,
- Capability/provider availability and failures,
- perception conflicts and ambiguity,
- Worldview/entity-resolution diagnostics,
- Dataset/model/evaluation diagnostics.

A diagnostic should carry enough typed context to navigate back to its source or relevant projection.

## Semantic Inspector experience

The Inspector should evolve toward a universal cross-domain explanation surface. Depending on selection type it may show:

- identity/type and stable reference,
- definitions and references,
- current runtime involvement,
- observations/evidence/confidence/ambiguity,
- history and last observation/run,
- related resources,
- dataset/model involvement,
- available context actions.

The Inspector explains and navigates; it does not become a competing Worldview or Workflow store.

## History and semantic comparison

The Studio should explicitly distinguish:

- **Undo/Redo** — local editing history,
- **Checkpoint** — named useful working state,
- **Revision** — persisted document/project change,
- **Run** — one execution identity,
- **Record** — persisted structured history/evidence,
- **Model Version** — versioned learning artifact.

Long-term high-value debugging question:

> What changed since this workflow last succeeded?

The answer may correlate Workflow revision, execution/run context, relevant Worldview/Record evidence and provider/model versions while preserving their separate authority.

## User workflow tests

In addition to tests of VisualTasker Studio itself, the Studio should eventually support tests of user automations. Example conceptual scenario:

```text
Given:
  Scene = LoginScreen
  LoginButton exists

When:
  Run LoginWorkflow

Expect:
  click(LoginButton)
  Scene -> HomeScreen
```

Potential authoring paths include manual creation, conversion from a successful Record, and AI-assisted proposal followed by explicit validation/acceptance.

## Settings integration

Prefer Settings categories for policy and personalization rather than additional permanent panels. Candidate settings:

- Workspace presets and layout behavior,
- keyboard/S-Pen/input mappings,
- navigation/history retention,
- diagnostic filtering and severity display,
- Inspector detail/density,
- semantic diff presentation,
- Runtime follow/step defaults,
- visual semantics/accessibility preferences,
- command palette ranking/history,
- AI assistance/proposal display policies where user-configurable.

Settings configure behavior; they must not redefine domain semantics or authority contracts.

## Small Studio-quality features

These features have low architectural cost but high perceived coherence:

- Recent Projects / Recent Workflows,
- Favorites,
- Breadcrumbs,
- Back/Forward navigation,
- Go to Definition,
- Find References,
- Quick Documentation,
- Bookmarks,
- named layouts,
- persistent panel sessions,
- selection history,
- minimap/overview where useful,
- Scratch Workflow / Playground,
- templates and example projects.

## Non-goals

Do **not** create another universal domain core merely to support Studio UX. In particular:

- no competing Workflow truth,
- no competing Worldview truth,
- no generic “everything graph” as a new authority,
- no direct editor-to-editor synchronization authority,
- no diagnostic store that silently mutates domain state,
- no Inspector that owns copied canonical state.

The next quality step is primarily **integration and navigation across existing contracts**, not additional foundational abstractions.

## Experience acceptance vision

A mature Studio should allow a user to start from something interesting or wrong and follow the cause without manually reconstructing context:

`RailTrace failure -> Workflow node -> expected EntityConcept -> Worldview candidates -> evidence conflict -> history -> semantic diff -> correction/Perugger -> Learning Candidate or workflow/resolver fix`

That traceability is the target Studio feeling.

## Mandatory reference fixture

`Resources / Test Fixtures / Opaque Lobster`

The Opaque Lobster is permitted to exist as a resource while remaining semantically unknown. It must not be promoted automatically to an Observation or WorldEntity merely because the Rationalisateur is uncomfortable with uncertainty. This is intentionally humorous, but it also serves as a useful reminder that **UNKNOWN is a valid state**. 🦞
