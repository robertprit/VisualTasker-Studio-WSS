# M8-0 Canonical Project Workspace and Resource Contract

Status: implemented and verified locally

## Purpose

M8-0 adds a project boundary above the existing workspace model. It does not replace
`WorkspaceDocument`, `CanonicalWorkspaceDocument`, or `WorkflowGraph`.

```text
ProjectDocument
  -> ProjectWorkflowRoot
       -> WorkspaceDocument
            -> CanonicalWorkspaceDocument / WorkflowGraph
```

A project owns identity, membership, references, variables, and resources. Each workflow root
continues to own exactly one normal workspace document and is compiled, projected, mutated, and
executed independently.

## Ownership and identity

The project contract defines stable `ProjectId`, `WorkflowRootId`, `DefinitionId`, `ResourceId`,
`ProjectReferenceId`, and `ProjectRevision` values. Map keys must match contained IDs. Loading a
project never generates replacement IDs.

Legacy single-workspace JSON migrates deterministically:

- project ID: `project:<workspace-id>`
- root ID: `root:<workspace-id>`
- project revision: the non-negative workspace revision
- source anchors and all workspace-owned metadata remain inside the unchanged workspace document

Repeating the migration yields the same IDs and serialized project. Explicit project metadata,
root metadata, definition metadata, resource metadata, reference metadata, and variable metadata
roundtrip without filtering.

## Workflow roots

Every `ProjectWorkflowRoot` wraps one `WorkspaceDocument`. Root switching changes only the active
project selection. It does not merge roots, move entities, or mutate another root's graph.

The three editor projections are created from the selected root by the existing
`WorkspaceWorkflowState`. Text, Block, and Flow therefore continue to observe one canonical
workflow. Selection state is retained per root. Layout metadata and EMScript source anchors remain
owned by the wrapped workspace document.

Canonical relations may not cross root boundaries. Duplicate workspace ownership and invalid
canonical documents are diagnosed by `ProjectValidator`.

## Definitions and references

Definitions represent functions, subworkflows, and custom flow units. A call stores a stable
definition reference; it does not copy the definition graph. M8-0 supports registration,
reference binding, validation, deterministic where-used lookup, and guarded deletion.

`ProjectReference` has an explicit source, target, and kind. Source entities are checked when a
definition or root supplies a workspace. Targets are checked for existence and compatible kind.
Missing, duplicated, mismatched, or dangling references reject the transaction.

Function-call execution is not introduced by M8-0.

## Variable scopes

| Scope | Lifetime | Persistence | Provider required | Runtime in M8-0 |
| --- | --- | --- | --- | --- |
| Local | invocation | none | no | existing behavior |
| Script | workflow run | session | no | existing behavior |
| Global | project | project document | no | contract only |
| Project | project | project document | no | contract only |
| AppShared | application | app store | no | contract only |
| External | provider | provider | yes | contract only |

Variable ID, label, type, scope, lifetime, persistence, and provider binding stay separate.
`LET` and `SET` semantics are unchanged. External variables without a provider binding are invalid.

## Resources

Resources are typed references for image, template, region, point, path, shape, capture,
observation, and provider assets. Scope is explicit: embedded, project-local, app-shared, or
external.

The project stores a URI, relative path, content address, or provider key plus optional content
hash. Bitmap bytes and other large payloads are never stored in `WorkflowGraph` or the project
JSON. References and where-used checks prevent removal of resources that are still in use.

## Mutation and history

`ProjectOperationExecutor` is the only project-level mutation boundary. It provides expected
revision checks, unique operation IDs, atomic staging, final validation, typed diagnostics, and
rollback to the exact input project.

Project operations manage membership and references. `ApplyWorkspaceTransaction` delegates root
graph mutations to the existing `WorkspaceOperationExecutor`; it is not a second graph mutation
engine. `ReplaceRootWorkspace` is restricted to already validated import, shell, and recorder
boundaries.

One accepted project transaction increments the project revision once. `ProjectHistoryController`
records accepted project snapshots for deterministic undo and redo; failed transactions are not
added to history.

## Persistence and migration

`ProjectSerializer` uses project schema version 1 and embeds each root/definition workspace using
the existing `WorkflowSerializer`. Collections are sorted before serialization. Decode performs
strict schema and reference validation.

Input without `projectSchemaVersion` is treated as a legacy workspace and migrated through the
deterministic single-root rule. No Room schema change is needed for this contract slice because
the project codec is an explicit file/import boundary and existing workspace/session persistence
remains unchanged.

## Runtime and recorder

`ProjectExecutionPlanner` requires an explicit `ProjectDocument + WorkflowRootId`. It selects one
root and delegates to the existing `WorkflowExecutionPlanner`. `ExecutionRequest`,
`ExecutionPlan`, headless session snapshots, and checkpoints retain project ID, root ID, and
project revision. Existing single-root callers remain valid because these fields are optional.

Recorder preview and apply carry the target project ID, project revision, root ID, and workspace
revision. Publish validates all four, applies the existing recorder workspace transaction to the
selected root, and commits that result through one project transaction. Other roots remain byte-
for-byte unchanged and the existing duplicate-publish guard remains authoritative.

## Diagnostics and failure behavior

- Missing roots, definitions, resources, variables, references, or source entities are typed.
- Revision conflicts reject before mutation.
- Invalid reference kinds and external provider bindings are rejected.
- Cross-root relations and duplicate workspace ownership are rejected.
- Any failed operation restores the complete original project.
- Runtime root lookup returns `PROJECT_ROOT_NOT_FOUND`; it never falls back to another root.

## Acceptance matrix

| AC | Status | Evidence |
| --- | --- | --- |
| AC-01 Existing workspace architecture reused | PASS | Roots wrap `WorkspaceDocument`; graph mutation delegates to `WorkspaceOperationExecutor`. |
| AC-02 Canonical project contract | PASS | Typed project IDs, manifest, revision, validator, and deterministic codec. |
| AC-03 Multiple independent roots | PASS | Root operations, editor root session, isolation and cross-root tests. |
| AC-04 Stable definition references | PASS | Definition IDs, typed references, where-used and delete guard. |
| AC-05 Variable scope contract | PASS | Scope/lifetime/persistence/provider matrix; unimplemented runtime scopes are explicit. |
| AC-06 Resource contract | PASS | Typed IDs/types/scopes, location/hash storage, references and delete guard. |
| AC-07 Atomic project operations | PASS | Expected revision, rollback, typed diagnostics, undo/redo. |
| AC-08 Backward-compatible persistence | PASS | Legacy single-root migration, deterministic save/reload, metadata preservation. |
| AC-09 Three-editor integration | PASS | Active-root session reuses `WorkspaceWorkflowState`; per-root selection tests. |
| AC-10 Runtime root selection | PASS | Explicit project/root planner and headless identity propagation. |
| AC-11 Recorder target integration | PASS | Root-aware preview/apply with project/workspace revision checks and isolation. |
| AC-12 Regression and migration tests | PASS | Focused contracts plus complete repository gates documented below. |

## Deliberate gaps after M8-0

- No project explorer or new panel.
- No function/subworkflow call runtime.
- Project/AppShared/External scope execution remains future provider work.
- No binary asset package, cloud storage, VT2VT project sync, or Room project table.
- No new EMScript syntax.

These are later M8/M9 slices and are not hidden behind fallback behavior.

## Verification

The M8-0 gate covers new project creation, deterministic legacy migration, root lifecycle and
isolation, references, scopes, resources, atomic rollback, history, save/reload, editor switching,
runtime root selection, headless identity, recorder root targeting, metadata preservation, and
existing single-root behavior. Complete command results and commit hashes are recorded in the
implementation closeout.

Verified on 2026-10-12:

| Gate | Result |
| --- | --- |
| Project/workspace focused contracts | PASS |
| BlockEditor complete tests | PASS |
| FlowEditor complete tests | PASS |
| App unit tests | PASS |
| M4 three-editor conformance, three independent runs | PASS |
| Debug assembly | PASS |
| Root and BlockEditor diff checks | PASS |
| Device smoke | NOT REQUIRED; no productive UI interaction changed |
