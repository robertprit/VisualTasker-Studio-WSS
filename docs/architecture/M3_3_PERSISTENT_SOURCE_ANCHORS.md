# M3-3 Persistent Source Anchors and Import Convergence

## Status

M3-3 closes the identity gap left by structurally identical EMScript elements. Generated EMScript can now carry runtime-inert projection directives that preserve canonical statement, expression, branch, and relation identities across text edits, save/reload, and source apply.

The canonical workspace remains the only semantic source of truth. Source anchors transport identity; they do not create a second graph, runtime instruction, or mutation engine.

## Ownership

- `SemanticEntityId` and `SemanticRelationId` in the canonical workspace own identity.
- `EmscriptSourceAnchorContract` owns directive names, locator syntax, and lexical validation.
- `WorkspaceSourceAnchorProjector` reads the canonical workspace and projects anchors into EMScript.
- `EmscriptParserSlice` parses directives into a projection AST separate from runtime IR.
- `EmscriptWorkspaceImporter` attaches parsed anchor provenance to imported blocks.
- `WorkspaceIdentityReconciler` validates and applies anchors before legacy structural reconciliation.
- `EmscriptSemanticSourceApply` remains the sole source-to-transaction planner.

No anchor is derived from a line number, text offset, formatting, list position, pixel coordinate, render order, or temporary view object.

## Transport Format

Entity anchors are attached to the next semantic statement:

```text
@source.entity("block", "block:stable-id", "Statement")
@source.entity("input:CONDITION/LEFT", "block:stable-expression-id", "Expression")
@source.entity("branch:THEN", "branch:stable-id:THEN", "Branch")
if (...) {
    ...
}
```

Supported locators are:

- `block` for the statement represented by the following source statement;
- `input:<slot path>` for an expression reached through stable catalog slot names;
- `branch:<slot>` for a branch owned by that statement.

Canonical relation anchors are emitted as a deterministic prelude:

```text
@source.relation(
    "relation-id",
    "RelationKind",
    "source-entity-id",
    "target-entity-id",
    "RoleKind",
    "role-name-or-empty",
    "branch-role-or-empty",
    "order-or-empty"
)
```

The serializer currently emits each directive on one line. The parser treats all `@` projection directives as runtime-inert. They never enter `EmscriptIrScript`, command execution, providers, or dry/wet-run semantics.

## Identity Resolution

Reconciliation proceeds in this order:

1. validate every explicit entity and relation anchor;
2. bind anchored statements and expressions to their previous canonical IDs;
3. rebuild compatibility block identities from the anchored canonical entities;
4. reconcile unanchored legacy elements by unique signature, structural path, and type-compatible fallback;
5. restore anchored canonical entity IDs;
6. restore anchored relation IDs by relation kind, endpoints, and role;
7. retain previous relation IDs for structurally equal unanchored relations;
8. plan one `WorkspaceTransaction` through `EmscriptSemanticSourceApply`.

New unanchored source elements receive new identities. Unchanged anchored elements retain their identities even when equal neighbors are inserted, deleted, or reordered.

## Legacy Fallback

Older scripts without `@source.*` directives remain valid. They use the M3-2 reconciliation fallback unchanged. An ambiguous cardinality change among repeated equal elements still returns `IDENTITY_RECONCILIATION_AMBIGUOUS` because identity cannot be proven.

The first application of an anchored script to a start-only/default workspace may establish its transported identities. Applying anchors to a populated previous workspace is strict.

## Conflict Handling

The following conditions abort before publication:

- duplicate entity IDs;
- duplicate relation IDs;
- duplicate locators on one statement;
- orphaned entity directives;
- invalid locator, ID, enum, role, branch role, or order;
- an anchor referencing an entity or relation absent from the previous populated workspace;
- entity-kind mismatch;
- relation structure conflicting with the previous relation;
- multiple structural relation matches;
- collisions with already generated canonical IDs.

When a valid previous relation anchor no longer has a structural match after an intentional source edit, the old relation is treated as deleted. Its stale directive does not get reassigned to a different relation; the next generated source drops it. Unknown relation IDs and descriptors that contradict the previous relation still abort.

Diagnostics use `SOURCE_ANCHOR_INVALID`, `SOURCE_ANCHOR_ORPHANED`, `SOURCE_ANCHOR_DUPLICATE`, `SOURCE_ANCHOR_TARGET_MISSING`, `SOURCE_ANCHOR_KIND_MISMATCH`, `SOURCE_ANCHOR_CONFLICT`, or `SOURCE_ANCHOR_AMBIGUOUS`. No conflict falls back to a guessed identity and no transaction is published.

## Import and Apply Paths

All productive EMScript mutations converge through:

`EmscriptApplyGuard -> WorkspaceIdentityReconciler -> EmscriptSemanticSourceApply.plan -> WorkspaceTransaction`

This applies to:

- Workspace EMScript panels;
- explicit and automatic draft apply in `WorkspaceScreen`;
- EMScript file load in `WorkspaceScreen`;
- the legacy `MainScreen` draft apply path.

Direct `EmscriptWorkspaceImporter` use remains only where no workspace mutation occurs:

- parser/preview diagnostics;
- source-selection lookup in `WorkspaceSelectionResolver`;
- test-script and read-only projection helpers.

Explicit non-EMScript project replacement and legacy-format import keep their own declared replace semantics. M3-3 does not silently turn those operations into source merge.

## Persistence and Roundtrip

Anchors are regenerated from canonical schema-2 identity whenever a workspace is serialized to EMScript. The workspace JSON continues to persist canonical IDs through `WorkflowSerializer`; imported directive provenance is retained in compatibility block metadata only long enough to reconcile back to those canonical IDs.

The supported cycle is:

`Workspace -> anchored EMScript -> Parse projection AST -> Import -> Reconcile -> Transaction -> Workspace`

Formatting and comments may change without changing semantic identity. Directive order is deterministic. The runtime script obtained after removing projection directives is unchanged.

## Performance

The bounded M3-3 close-out profile is documented in
[`M3_3_PERFORMANCE_DIAGNOSIS.md`](M3_3_PERFORMANCE_DIAGNOSIS.md). Raw measurements are retained in
[`M3_3_PERFORMANCE_PROFILE.csv`](M3_3_PERFORMANCE_PROFILE.csv).

The profile covers 40, 80, 160, and 320 statements for legacy and anchored input, with both
format-only and structural edits. It identifies the existing workspace validator traversal as the
dominant nonlinear cost. Anchor resolution, identity reconciliation, planning, transaction, and
publication remain small by comparison. No performance optimization or semantic change is part of
M3-3.

## Scope Boundary

M3-3 changes no runtime command behavior, provider contract, visual editor renderer, VT2VT protocol, property schema, or source-apply atomicity. It does not begin M3-4 or M4.
