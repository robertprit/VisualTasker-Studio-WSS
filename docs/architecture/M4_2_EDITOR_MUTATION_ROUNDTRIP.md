# M4-2 Editor-Originated Mutation and Roundtrip Conformance

## Scope

M4-2 verifies the existing production mutation paths of TextEditor, BlockEditor, and FlowEditor. It
does not add syntax, runtime behavior, UI features, or a second mutation model. Workflow semantics
remain owned by `CanonicalWorkspaceDocument`; editor state remains projection-owned.

Baseline: main `c475236`, BlockEditor `595a8b7`, FlowEditor `008b136`.

## Production Mutation Inventory

| Origin | Productive entry point | Semantic actions | Publication boundary | Visual-only actions |
| --- | --- | --- | --- | --- |
| TextEditor | `EmscriptApplyGuard` -> `EmscriptSemanticSourceApply` | parse, reconcile, create/delete, relation/property/order changes | one planned `WorkspaceTransaction` executed by the shared executor | draft, caret, selection, formatting before Apply |
| BlockEditor | `BlockEditorController.onAction` -> workspace action planner/reducer | create, connect, disconnect, detach, delete, field/property/type and branch changes | one transaction per accepted controller action | selection, focus, pan, zoom and drag preview |
| FlowEditor | `applyFlowchartWorkspaceMutation` | add/delete node, connect/disconnect ports, property/type and IF-branch changes | one transaction per accepted Flow mutation | FlowView position, routing, facets, collapse, viewport and focus |
| Shell history | serialized `WorkspaceHistoryEntry` plus semantic selection | restore accepted canonical before/after snapshot | restored document is reconciled before publication | selection is retained by semantic target, not render order |

Collapse is projection state unless a separately declared semantic property owns it. Opening an
editor, projection, save/reload, focus, selection, pan, zoom, and FlowView synchronization must not
advance canonical revision.

## Conformance Scenarios

The central `ThreeEditorConformanceTest` contains 23 tests. M4-2 added productive evidence for:

- Block controller statement creation and connection with two accepted transactions and two revisions.
- Disconnect/reconnect of a nested reporter while retaining all statement/expression entity IDs.
- Text Source Apply property mutation with stable identity, source directives, and unknown metadata.
- Statement detach and delete without loss of unaffected entities.
- Explicit statement reparenting through detach/connect/connect and matching three-step undo/redo.
- Complete four-dimension fingerprint restoration through Block controller undo/redo.
- Save, reload, all three projections, and zero-operation anchored reapply.

Existing M4-0/M4-1 scenarios continue to cover Flow node creation/connection, Flow property mutation,
IF/ELSEIF/ELSE identity and ordering, invalid-operation rollback, projection-only zero revisions, and
three deterministic fingerprint runs.

## Identity and Mutation Rules Confirmed

1. Unchanged entities retain `SemanticEntityId`, legacy source mapping, and source entity directives.
2. Unchanged relations retain identity. A relation deliberately removed by disconnect/delete no
   longer exists; a later connect creates a new relation identity with the required kind, role, and order.
3. Occupied statement ports reject implicit rewiring. Productive reorder/reparent is explicit
   detach followed by connect operations, each represented in history.
4. Detaching a statement may normalize it into a root containment relation. This is not an orphan or
   a hidden sequence relation.
5. Source anchors are transport directives for reconciliation. They are verified separately and are
   not a fifth canonical fingerprint dimension.
6. Every accepted transaction advances revision exactly once. Rejected transactions and projection
   operations advance it zero times.
7. Unknown metadata survives unrelated editor mutations, projection, and persistence.
8. The four authoritative fingerprints remain semantic, identity, metadata, and persistence.

## Repairs and Gaps

No productive defect was demonstrated, so M4-2 changes no production code. Test development exposed
three invalid assumptions and corrected the tests rather than weakening production contracts:

- reconnecting a removed relation cannot preserve the identity of a relation that no longer exists;
- detach correctly keeps root containment while removing sequence/data ownership;
- occupied ports require explicit detach instead of implicit destructive rewiring.

The shell-level Flow/Text undo buttons still use the shared serialized workspace history rather than
editor-local controllers. Existing `WorkspaceEditorSyncContractTest` proves semantic selection
reconciliation for restored history entries. A direct Compose interaction test of those local shell
closures remains a UI-test gap; it is not evidence of a semantic or persistence failure.

Unsupported language structures and opaque fragments remain explicitly unsupported and fail before
publication. Automatic occupied-port reparenting is not implemented and is not required by M4-2.

## Acceptance Criteria

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 productive mutation paths inventoried | PASS | Inventory above and M4-1 operation-path documentation. |
| AC-02 BlockEditor mutations covered | PASS | Productive controller create/connect, reporter reconnect, detach/delete, reparent, undo/redo tests. |
| AC-03 FlowEditor mutations covered | PASS | Central Flow add/connect/property/rollback tests from M4-1 remain green. |
| AC-04 Text Source Apply conformant | PASS | Property apply and anchored zero-operation reapply tests. |
| AC-05 nested reporters and branches stable | PASS | Nested reconnect plus IF/ELSEIF/ELSE roundtrip tests. |
| AC-06 IDs, anchors, metadata preserved | PASS | Reporter, text apply, metadata, save/reload tests. |
| AC-07 delete/reparent/detach constrained | PASS | Dedicated productive controller tests preserve unaffected entities. |
| AC-08 undo/redo checked | GAP | Block controller and semantic selection restoration pass; direct shell Compose closure interaction is not centrally exercised. |
| AC-09 persistence and rollback correct | PASS | Save/reload/reapply plus invalid transaction and invalid Flow property rollback tests. |
| AC-10 four fingerprints deterministic | PASS | Complete undo/redo snapshots and three consecutive deterministic runs. |
| AC-11 M4-0/M4-1 regressions green | PASS | All 23 central tests pass together. |
| AC-12 build and relevant regressions green | PASS | Final command evidence recorded in the M4-2 completion report/commit. |

## Device Gate

M4-2 changes tests and architecture documentation only. No productive Android UI, gesture, or
rendering behavior changed, so the requested device gate does not apply.

## Validation

- BlockEditor `./gradlew test`: PASS.
- FlowEditor `./gradlew test`: PASS.
- App `./gradlew :app:testDebugUnitTest :app:assembleDebug`: PASS.
- Central `ThreeEditorConformanceTest`: 23/23 PASS in three consecutive forced runs.
- Root, BlockEditor, and FlowEditor `git diff --check`: PASS.
- Productive Android files changed: none; device installation not required by this slice.
