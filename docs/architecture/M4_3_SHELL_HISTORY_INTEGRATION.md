# M4-3 Shell History Integration

## Scope

M4-3 closes the integration boundary between productive editor mutations, the Compose workspace
shell, semantic undo/redo, and cross-editor selection reconciliation. It adds no language, runtime,
provider, gesture, or editor feature.

Baseline: main `965e972`, BlockEditor `595a8b7`, FlowEditor `008b136`.

## Productive History Paths

| Origin | Mutation path | Local UI history | Canonical shell history | Projection after restore |
| --- | --- | --- | --- | --- |
| TextEditor | draft -> `EmscriptApplyGuard` -> Source Apply transaction -> shell publication | text snapshots per tab | accepted canonical before-state | generated text, blocks, and Flow are rebuilt from restored workspace |
| BlockEditor | `BlockEditorController` -> `WorkspaceAction` plan -> transaction -> serialized callback | controller history exists inside the editor | shell records accepted canonical transition | Flow and text consume the restored canonical document |
| FlowEditor | `FlowchartWorkspaceMutation` -> shared action/transaction path -> shell publication | FlowView owns layout/routing | shell records accepted canonical transition | blocks and text consume the restored canonical document |

The shell owns one current `WorkspaceWorkflowState`. `WorkspaceShellHistoryController` stores only
serialized before/after entries plus semantic selection. It neither validates nor mutates a graph and
therefore is not a second workspace state or mutation engine.

## Integration Repair

The former undo/redo stacks and source-coalescing state were local closures in `WorkspaceScreen`.
They worked productively but could only be tested indirectly. They are now encapsulated by
`WorkspaceShellHistoryController` and used directly by the same Compose callbacks.

History admission now compares the canonical before/after documents:

- an accepted semantic transaction creates one history boundary, subject to the existing source
  coalescing policy;
- rejected/no-op transitions create no entry;
- selection, focus, editor switch, pan, zoom, collapse, and root-position changes do not create a
  semantic history entry;
- a visual transition does not clear semantic redo history;
- undo/redo restores serialized workspace state and reconciles selection by semantic identity.

This fixes the demonstrated case where a Flow/Block layout move could affect the global semantic
undo stack even though canonical revision and semantics had not changed.

## Integration Evidence

`WorkspaceShellHistoryIntegrationTest` exercises ten productive scenarios:

1. Text draft mutation, Source Apply, local text undo, and redo.
2. Flow property mutation, shell undo, and redo.
3. Block mutation, Flow projection/editor switch, and shell undo.
4. Flow mutation, Text projection/editor switch, and shell redo.
5. Delete of a selected entity and controlled selection clearing.
6. Undo of delete with exact identity and focus target restoration.
7. Invalid selection after editor switch without inferred replacement identity.
8. Three transactions with strict reverse undo and forward redo order.
9. Rejected mutation without history entry.
10. Visual move without canonical revision or semantic history entry.

The save/reload scenario additionally proves that serialization/reload inside the running shell does
not destroy its in-memory history. History is intentionally not persisted across process restart; a
new shell process starts with empty undo/redo stacks. Persistent history remains outside M4 scope.

## Invariants Confirmed

- One current canonical workspace and one semantic mutation executor remain authoritative.
- Successful semantic transactions advance canonical revision exactly once before shell publication.
- Failed and visual transitions add no history entry.
- Undo/redo restores exact serialized semantic, identity, metadata, and persistence fingerprints.
- Text Source Apply undo/redo retains entity IDs, source directives, and unknown metadata.
- Deleted selection targets are cleared; undo restores the original semantic selection target.
- Editor projection/switch does not trigger Source Apply or semantic mutation.
- The history controller has no dependency on an optional editor module.

## Acceptance Criteria

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 shell history paths inventoried | PASS | Productive path table and direct `WorkspaceScreen` integration. |
| AC-02 TextEditor undo/redo integrated | PASS | Text draft/Source Apply undo/redo integration test. |
| AC-03 FlowEditor undo/redo integrated | PASS | Flow property mutation exact snapshot undo/redo test. |
| AC-04 BlockEditor undo/redo/editor switch consistent | PASS | Productive Block controller mutation, Flow projection, shell undo test. |
| AC-05 selection/focus reconciliation | PASS | Delete clears target; undo restores original block/node identity. |
| AC-06 editor switch has no semantic mutation | PASS | Flow/Text projection switch tests retain revision and history depth. |
| AC-07 order, revision, rollback | PASS | Three-step ordering, rejected mutation, and existing transaction tests. |
| AC-08 IDs, anchors, metadata stable | PASS | Text integration and delete/undo fingerprint evidence. |
| AC-09 four fingerprints deterministic | PASS | Exact snapshots over Flow and multi-step shell undo/redo. |
| AC-10 M4-0/1/2 tests retained | PASS | Central 23-test harness and complete module suites. |
| AC-11 closure matrix created | PASS | `M4_CLOSURE_AUDIT.md`. |
| AC-12 tests/build successful | PASS | Validation section below. |

## Validation

- Shell/history integration: 10/10 PASS.
- Central M4 conformance harness: 23/23 PASS in three forced runs.
- Complete BlockEditor and FlowEditor suites: PASS.
- Complete app unit suite and `assembleDebug`: PASS.
- Root, BlockEditor, and FlowEditor `git diff --check`: PASS.
- Productive Compose shell changed; install/start smoke on the connected device: PASS.
- No automated panel clicks or screenshots were performed.
