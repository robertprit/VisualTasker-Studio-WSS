# M2-5 Canonical Mutation Migration

## 1. Legacy Mutation Architecture

Before M2-5, `WorkspaceReducer`, BlockEditor controller deletion helpers, FlowEditor adapters, and the EMScript bulk assembler could change reciprocal connector endpoints and root lists directly. The productive reducer and the dead legacy reducer therefore encoded competing semantic write paths. M2-5 removes the dead reducer/controller implementations and routes editor commits through canonical mutations. Direct connector construction remains only inside bounded schema-1/XML/import assembly and the compatibility projector.

Mutation inventory:

| Mutation | Productive entry | M2-5 classification |
|---|---|---|
| create block/entity | `WorkspaceAction.InstantiateBlock/CreateBlock` | CANONICAL_READY |
| delete/group delete | `DeleteBlock/DeleteBlockPreservingChain/DeleteBlocks` | CANONICAL_READY |
| connect/reconnect | `Connect` | CANONICAL_READY |
| disconnect/detach/lift | `Disconnect/DetachBlock`, lift facade | CANONICAL_READY |
| statement insert/reorder | drag commit through reducer | CANONICAL_READY |
| reporter attach/replace/detach | expression relations | CANONICAL_READY |
| root add/remove/reorder | normalized root containment relations | CANONICAL_READY |
| IF/ELSEIF/ELSE/loop branches | `ReplaceBlockShape`, branch/body relations | CANONICAL_READY |
| variable reference | field/create mutation to reference relation | CANONICAL_READY |
| undo/redo | canonical before/after document history | CANONICAL_READY |
| EMScript import | local legacy assembly, one canonical publish | NEEDS_ADAPTER, bounded |
| text apply | import, reconcile, validate, atomic publish | CANONICAL_READY |
| Blockly XML import | schema-1 local DTO construction | NEEDS_ADAPTER, bounded |
| layout/preview copies | stripped compatibility data only | PROJECTION_ONLY |

## 2. Canonical State Owner

`WorkspaceDocument.canonical: CanonicalWorkspaceDocument` is the sole productive owner of semantic entities, relations, and ordering. `CanonicalWorkspaceMutator` performs atomic local transactions. `CanonicalWorkspaceReducer` translates editor actions into those transactions. Connector fields and `rootBlocks` are compatibility output and have no independent semantic authority when canonical state is present.

## 3. Schema Strategy

Option A was selected: `WorkspaceDocument` carries canonical state directly. This minimizes migration chains and keeps payload, presentation state, and canonical authority in one versioned document without introducing a third semantic model. Current persisted schema is 2.

## 4. Schema-1 Migration

Schema 1 and unversioned legacy JSON are read into the legacy DTO, deterministically canonicalized, validated, and immediately projected as current state. Entity, branch, relation, reference, and root identities are established once at this boundary. The legacy input is not retained as mutable truth.

## 5. Canonical Mutation Contract

The internal contract supports `CreateEntity`, `DeleteEntity`, `CreateRelation`, `RemoveRelation`, and `SetRelationOrder`. Actions may stage block payload changes, but semantic publication succeeds only when the complete canonical transaction validates and the compatibility projection can be reconstructed.

## 6. Transaction Boundary

Each reducer action publishes one before/after document. Multi-relation operations such as insertion, replacement, preserved-chain deletion, branch shape changes, and root normalization are applied to local maps, validated together, and published once. Rejection returns the original document object.

## 7. Sequence Mutation

Sequence connect, disconnect, insertion, reconnection, and preserved-chain deletion change `Sequence` relations first. Reciprocal `previous/next` endpoints are rebuilt by `CanonicalCompatibilityProjection`. Duplicate incoming or outgoing sequence relations are rejected.

## 8. Expression Mutation

Reporter attach, replacement, and detach use `Expression` relations with stable owner/expression entities, semantic roles, and optional operand order. Dynamic variable input types are resolved before accepting a connection. Replacing one operand does not recreate unaffected entities.

## 9. Branch Mutation

Control blocks own stable branch entities through branch containment relations. `ReplaceBlockShape` creates missing branches, deletes removed branches, and changes only relation order for survivors. THEN, ELSEIF, ELSE, and loop-body roles are semantic roles, not identities.

## 10. Containment

Containment represents workflow roots, control-to-branch ownership, and branch-to-body ownership. Body insertion/removal is canonical. Branch containment order is validated for uniqueness per owner and role.

## 11. Root Semantics

Statement and expression entities without structural ownership are canonical roots. Root membership and order are containment relations. `rootPositions` remains presentation-only and cannot create semantic roots. Root relations are normalized after every canonical transaction while preserving retained relation identities and order.

## 12. Reference Mutation

Variable entities use stable variable IDs. Blocks carrying `variableId` create a canonical `Reference` relation. Changing or clearing that field replaces or removes the relation atomically. A referenced variable cannot be deleted.

## 13. Compatibility Projection

`CanonicalCompatibilityProjection` is deterministic, disposable, and reconstructable. It clears all semantic connector links, projects canonical relations into reciprocal endpoints, and derives ordered roots. It is the only productive component allowed to write connector-shaped semantic links after canonical state exists.

## 14. History

BlockEditor history records canonical `WorkspaceDocument` before/after states. Undo and redo restore canonical entities, relations, ordering, payload, and expected selection together. Invalid or presentation-only operations do not create semantic history entries.

## 15. Importer/Text Apply

The EMScript bulk assembler may construct a local schema-1-shaped graph for performance, but it canonicalizes exactly once before returning. Text apply performs parse, import, identity reconciliation, canonical validation, roundtrip validation, and atomic publish. For an invalid typed connection, the importer may retain a local schema-1-shaped candidate with `canonical = null` so the apply guard can report the precise type diagnostic. That candidate is never published or persisted as canonical truth and must be canonicalized again before crossing the import boundary.

## 16. Block Projection

BlockEditor reads the compatibility projection but submits semantic intents to `WorkspaceReducer`. Create, delete, drag commit, detach, insert, reconnect, reporter operations, branch shape changes, variable references, undo, and redo all end at the canonical reducer. Hover and drag preview remain presentation-only.

## 17. Flow Projection

FlowEditor remains a projection. Root-app Flow mutations now use the same `WorkspaceAction`/`WorkspaceReducer` path for instantiate, delete, connect, disconnect, field edits, reporter replacement, and branch shape changes. It has no separate edge truth. Flow layout and positions remain projection/presentation state.

## 18. Persistence

Schema-2 JSON persists canonical entities, relations, relation IDs, branch/expression identities, and semantic ordering. Connector links and `rootBlocks` are not persisted as schema-2 truth; they are reconstructed on load. Schema-1 read and migration remain supported. Save/reload retains canonical IDs exactly.

## 19. Validator

Pre-publication validation covers duplicate entity/relation IDs, missing endpoints, endpoint kind mismatches, relation compatibility, duplicate sequence inputs/outputs, duplicate semantic facts, and duplicate containment order. Projection shape errors also reject publication.

## 20. Performance

Canonicalization occurs at import/load or semantic commit boundaries, not during pointer movement, selection, viewport changes, hover, or animation. The bulk importer retains one publication and zero interactive reducer calls. Layout invalidation remains keyed by semantic projection changes.

## 21. Dual-Truth Elimination

The dead legacy reducer and dead controller endpoint-mutating deletion helpers were removed. A canonical document ignores stale manual connector/root modifications because migration reprojects compatibility state from canonical truth. Remaining direct connector writes are limited to compatibility projection, bounded legacy/XML/import construction, test/demo fixtures, or connection stripping for presentation analysis.

## 22. DUPLICATED_TRUTH

Before: 9 identified mutable duplication points. After: 0 productive semantic dual-write owners. Two bounded construction adapters remain (EMScript bulk assembly and Blockly XML/schema-1 import), but neither publishes or persists its local legacy representation as authority. Derived compatibility fields do not count as duplicated truth.

## 23. MISSING_CONTRACT

Before: 9. After: 2 material follow-ups remain: a neutral canonical property mutation primitive and an externally consumable transaction/diagnostic result contract. Current field payload updates are canonical-publication guarded, but are not yet the future M3 `SetProperty` operation.

## 24. WorkspaceOperation Readiness

READY for local neutral IDs and the operation shapes `CreateEntity`, `DeleteEntity`, `CreateRelation`, `RemoveRelation`, and `SetOrder`. NOT YET IMPLEMENTED: actor/device metadata, transport, acknowledgement, revision synchronization, or generic `SetProperty`. These belong to M3 and were intentionally excluded.

## 25. Remaining M2 Gaps

No editor-owned productive semantic truth remains in BlockEditor, FlowEditor, or Text apply. Remaining adapters should eventually build canonical state directly, but are bounded and validated. The largest residual gap is formalizing payload/property mutation and public diagnostics without turning M2-5 into the distributed WorkspaceOperation protocol.

## 26. M2 Closure Recommendation

M2 can close after all repository, performance, persistence, and device gates remain green. Workflow semantics can now be loaded, migrated, mutated, validated, serialized, compiled, and projected without editor-owned semantic truth. The recommended next slice is M3-0: specify neutral `SetProperty` plus transaction result envelopes on top of this canonical owner, without changing editor UX.
