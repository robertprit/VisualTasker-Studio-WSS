# M3-0 WorkspaceOperation Foundation

## 1. Repository Audit

M2-5 already provides one atomic mutation engine, `CanonicalWorkspaceMutator`, for entity, relation, order, branch, expression, containment, root and reference changes. `CanonicalWorkspaceReducer` translates editor intents into this engine. M3-0 adds a public command facade and the missing property primitive; it does not create another reducer or semantic store.

| Existing path | Neutral operation |
|---|---|
| `CanonicalMutation.CreateEntity` | `WorkspaceOperation.CreateEntity` |
| `CanonicalMutation.DeleteEntity` | `WorkspaceOperation.DeleteEntity` |
| `CanonicalMutation.CreateRelation` | `WorkspaceOperation.CreateRelation` |
| `CanonicalMutation.RemoveRelation` | `WorkspaceOperation.RemoveRelation` |
| `CanonicalMutation.SetRelationOrder` | `WorkspaceOperation.SetOrder` |
| block/variable payload update | `WorkspaceOperation.SetProperty` |
| branch/expression/reference mutation | entity/relation/order algebra above |

## 2. Operation Algebra

`WorkspaceOperation` is a serializable sealed contract containing `CreateEntity`, `DeleteEntity`, `CreateRelation`, `RemoveRelation`, `SetOrder`, and `SetProperty`. Operations use only stable semantic IDs, semantic relation roles and typed property values. They contain no block IDs, connectors, geometry, drag state, flow edges or widgets.

Create operations carry their stable IDs. `WorkspaceOperationId` identifies each command locally and is also the transaction identity for a one-operation transaction. Actor, device, acknowledgement and transport metadata are intentionally absent.

## 3. SetProperty Contract

`SetProperty` addresses a `SemanticEntityId`, a stable `SemanticPropertyId`, and a `WorkspacePropertyValue`. Statement and expression properties are validated against the existing semantic payload shape. Known optional editor-semantic fields are limited to `note` and parameter-source fields; arbitrary string-to-any properties are rejected.

Variable entities support `name`, `type`, `scope`, `defaultValue`/`initialValue`, and an identity-preserving `variableId` check. Structural facts such as branch identity and relation order cannot be represented as properties. Updating a variable reference validates the referenced entity and updates the canonical reference relation in the same atomic mutation.

## 4. Typed Value Model

`WorkspacePropertyValue` contains `Text`, `Number`, `Bool`, and `Null`. Existing fields retain their type. `Null` is accepted only for nullable/optional properties represented by absence, currently `note`, parameter-source fields and variable default values. Type mismatches are rejected with `INVALID_PROPERTY_TYPE`.

## 5. Transaction Contract

`WorkspaceTransaction` contains a stable transaction ID, an ordered operation list and an optional `expectedRevision`. The executor translates the complete list to M2 canonical mutations and invokes `CanonicalWorkspaceMutator` once. Validation, compatibility projection and publication therefore happen once.

If any operation fails, the result contains the original input document. No partial state, history entry or projection is published. Successful transactions advance the canonical/workspace revision exactly once, regardless of operation count.

## 6. Result Envelope

`WorkspaceOperationResult.Success` exposes the resulting document/revision, affected semantic entity IDs, affected semantic relation IDs and warnings. `Failure` exposes the unchanged input document and one typed diagnostic with operation, entity and relation context where available. Expected domain failures are values, not exceptions.

## 7. Diagnostics

The public diagnostic codes are:

- `ENTITY_NOT_FOUND`
- `RELATION_NOT_FOUND`
- `PROPERTY_NOT_FOUND`
- `INVALID_PROPERTY_TYPE`
- `INVALID_RELATION`
- `DANGLING_REFERENCE`
- `INVALID_ORDER`
- `DUPLICATE_ID`
- `VALIDATION_FAILED`
- `REVISION_MISMATCH`

The executor maps existing canonical validation issues into this public vocabulary. Canonical validation remains the source of truth; M3 does not introduce a competing validator.

## 8. Revision Semantics

`expectedRevision` is an optional local optimistic guard. A mismatch rejects the transaction without mutation. A successful non-empty transaction produces one new revision. No conflict resolution, remote revision protocol or acknowledgement mechanism is included.

## 9. Determinism

Given equal canonical/workspace state and equal operations, execution produces equal state and affected-ID sets. IDs required by create operations are supplied by the operation. The contract and transaction are Kotlin-serialization compatible, so the same payload can later be transported without importing editor classes.

## 10. Editor Boundary

BlockEditor property edits still enter through `WorkspaceAction.UpdateField`, but that action now resolves the semantic entity and invokes `WorkspaceOperation.SetProperty`. FlowEditor field edits already use the same action/reducer path and therefore share the proof. Drag, layout, selection and canvas state remain outside the operation layer.

Text Apply remains an atomic parse/import/reconcile/validate/publish boundary. A later migration can express a semantic diff as one `WorkspaceTransaction`; M3-0 deliberately does not replace the proven importer or manufacture an operation log from source text.

## 11. Undo/Redo Implications

Existing history remains document-snapshot based. An editor operation returns one before/after document, so current undo/redo behavior remains valid and a transaction creates at most one history publication. Event sourcing and operation-log persistence are not part of M3-0.

## 12. Persistence and IR Proof

The integration fixture imports `LET x = 1 + 2` plus IF/ELSE, updates click text and wait duration through operations, detaches/restores expression and statement relations, reorders branches, serializes schema 2, reloads it and generates IR. Semantic IDs, payload and relations survive the roundtrip.

## 13. VT2VT Readiness

The operation/transaction input is serializable and deterministic against an identical base revision. This is sufficient for a local replay proof. VT2VT still requires actor/device identity, authorization, ordering, acknowledgement, retries, conflict policy and protocol versioning; none is implemented here.

## 14. Remaining M3 Work

M3-1 should introduce a catalog-backed semantic property schema so optional/default properties no longer rely on the current payload shape, then migrate additional editor commands to explicit operations. It may also define transaction serialization versioning and multi-diagnostic failure envelopes. Remote transport, recorder and AI producers remain later slices.
