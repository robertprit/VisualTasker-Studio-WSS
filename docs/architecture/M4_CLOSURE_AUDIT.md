# M4 Closure Audit

## Decision

The supported three-editor architecture is closed for M4. TextEditor, BlockEditor, and FlowEditor
publish through the same canonical workspace boundary, preserve supported semantics and identity,
and share process-local semantic shell history. There are no M4 blockers.

This decision does not claim support for language structures explicitly marked unsupported. New
structural families such as TRY/CATCH/FINALLY/THROW require a separately approved extension across
parser, canonical model, all projections, persistence, and diagnostics before runtime work begins.

## Milestone Matrix

| Milestone | Status | Binding result | Evidence |
| --- | --- | --- | --- |
| M4-0 Three-Editor Conformance | PASS | Production import/projection/generation/persistence harness and four fingerprints established. | `M4_0_THREE_EDITOR_CONFORMANCE.md`, central harness. |
| M4-1 WorkspaceOperation Convergence | PASS | Text, Block, and Flow semantic publication converges on transactions and the canonical executor. | `M4_1_WORKSPACE_OPERATION_CONVERGENCE.md`. |
| M4-2 Editor-Originated Mutation | PASS | Create/connect/property/reporter/branch/delete/reparent/roundtrip paths preserve supported semantics and identity. | `M4_2_EDITOR_MUTATION_ROUNDTRIP.md`, 23 central tests. |
| M4-3 Shell History Integration | PASS | Productive Compose callbacks use one semantic history controller with selection reconciliation. | `M4_3_SHELL_HISTORY_INTEGRATION.md`, 10 integration tests. |

## Closure Invariants

| Invariant | Status | Evidence / limitation |
| --- | --- | --- |
| WorkflowGraph/canonical workspace is the only semantic truth | PASS | All productive mutation paths publish the same `WorkspaceWorkflowState`. |
| One semantic operation/transaction executor | PASS | M4-1 path inventory and transaction tests. |
| Editor switch is read-only | PASS | Projection and shell integration tests create no revision/history entry. |
| Stable IDs and source anchors | PASS | M4-0/M4-2 roundtrips plus M4-3 Text undo/redo. |
| Projection metadata survives unrelated edits | PASS | Metadata/layout persistence fixtures and exact history snapshots. |
| Unsupported input cannot silently publish | PASS | Deterministic rejection and rollback tests. |
| Undo/redo is projection-independent | PASS | Text, Block, and Flow origin scenarios restore shared canonical snapshots. |
| Selection never falls back by pixel/render order | PASS | Delete, restore, invalid-target, and editor-switch reconciliation tests. |
| Visual actions do not enter semantic history | PASS | Canonical comparison and productive move regression test. |
| Optional editor absence cannot erase canonical data | PASS | History/controller contract depends only on workspace model types. |
| History survives app process restart | GAP | Deliberately unsupported; history is process-local and not persisted. |
| Opaque unsupported EMScript fragments roundtrip | GAP | Deliberately unsupported; publication fails before canonical mutation. |

The two GAPs are explicit product boundaries, not violations of the supported M4 contract.

## Remaining Risks

- Compose callback wiring is compiled and device-smoked, while semantic behavior is covered by JVM
  integration tests over the exact production controllers/adapters. There is no dedicated Compose UI
  automation for pressing every undo/redo icon.
- History stores serialized snapshots in memory. Very large projects may eventually require bounded
  compression or operation-log persistence, but that is not justified by an observed M4 defect.
- New language families must not be inferred from formatter vocabulary or visual node kinds.

## Exit

M4 three-editor conformance is accepted with no blocker. Further work requires an explicit next
milestone; M5 runtime work is not started by this audit.
