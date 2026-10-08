# M3-2 Lossless Semantic Source Apply

## Status

M3-2 replaces whole-document EMScript publication with a semantic source-apply plan. The parser and language contract are unchanged. A successful draft now follows one bounded path:

`Draft -> Parse -> Import -> Identity Reconcile -> Validate -> Semantic Diff -> WorkspaceTransaction -> Validate -> Publish`

## Owner

`EmscriptSemanticSourceApply` owns the pure source diff and plan. It depends on the existing canonical workspace, `WorkspaceOperation`, `WorkspaceTransaction`, and `VisualTaskerSemanticPropertySchema`. It owns no editor state, rendering state, history stack, parser grammar, or second mutable graph.

`EmscriptApplyGuard` orchestrates parse, reconciliation, pre-validation, plan execution, final validation, roundtrip generation, and serialization. Workspace publication and history remain owned by the Workspace host.

## Source Representation Mask

The EMScript importer records the block fields actually populated from source in `emscript.source.properties`. Factory defaults that were merely materialized are not source assertions. This mask is import provenance, not a property registry and not runtime semantics.

For a retained entity:

- represented fields may produce `SetProperty`;
- represented fields with equal values are `Unchanged`;
- fields absent from the mask are `NotRepresentedPreserved`;
- new entities retain their importer defaults as `NewEntityDefault`.

## Missing Is Not Delete

The central M3-2 rule is:

> A property missing from EMScript is not a request to clear or reset that property.

Display labels, notes, parameter-source selectors, custom metadata, collapse state, and other projection-only payload survive a source apply unless the source contract explicitly represents a replacement value.

An explicit structural removal is different. Removing a statement, expression, branch, relation, or variable construct from source produces `RemoveRelation` and/or `DeleteEntity`. It does not infer scalar-property deletion. A future source syntax that explicitly represents nullable-property deletion can emit `SetProperty(..., Null)` through the same plan without changing this rule.

## Identity Reconciliation

Reconciliation uses projection-independent structure and source payload, never pixels, render order, widgets, or temporary view objects.

The matching order is:

1. unique semantic signatures;
2. stable structural path plus normalized semantic type;
3. a single remaining type-compatible identity candidate.

IF, IF/ELSE, and IF/ELSEIF/ELSE share one identity family so branch-shape changes retain the owning statement identity. Existing relation IDs are retained when endpoints, relation kind, and role are unchanged; order changes use `SetOrder`.

Repeated equal elements whose cardinality changes cannot be mapped losslessly. Reconciliation returns `IDENTITY_RECONCILIATION_AMBIGUOUS`, and no transaction is published.

## Semantic Diff

The plan classifies:

- `CreateEntity`
- `DeleteEntity`
- `CreateRelation`
- `RemoveRelation`
- `SetOrder`
- `SetProperty`
- unchanged entity and relation IDs
- represented unchanged properties
- nonrepresented preserved properties
- defaults on new entities

Operation IDs and ordering are deterministic for equal baseline and target semantics. Relation removals precede entity deletion; entity creation precedes relation creation; scalar property operations follow structural preparation.

## Prepared Payload

Structural source edits can change a retained block shape, for example IF becoming IF/ELSE. The current canonical baseline must still validate against the old shape before the transaction starts. `WorkspaceOperationExecutor.executeWithStagedPayload` therefore:

1. validates and migrates the unchanged baseline;
2. overlays locally prepared block and variable payload;
3. validates all property operations;
4. invokes the existing canonical mutator once;
5. publishes only the fully projected result.

The staged payload is not a second mutation engine. It cannot publish independently and is discarded on any failure.

## Atomicity, Revision, and History

A non-empty accepted source transaction advances the workspace revision exactly once. Failure returns the original input document. No staged block payload, variable value, relation, metadata, revision, selection, projection, or history entry leaks from a rejected plan.

The Workspace host receives one serialized result and therefore creates at most one existing snapshot-history entry. Formatting-only changes with no semantic operations keep the revision and semantic IDs stable.

## Automatic Draft Import

The first observed manual-draft snapshot after Workspace startup is treated as persisted editor state, not a fresh edit. `EmscriptAutomaticDraftApplyGate` consumes that observation without applying it. Later nonblank changes continue through the guarded source-apply path.

This prevents a stale persisted draft from overwriting a valid canonical project during restart. Explicit file load and explicit text edits continue to apply normally.

## Persistence and Projections

The final document remains schema-2 and uses the existing `WorkflowSerializer`. Source apply preserves canonical entity and relation IDs across save/reload. BlockEditor and FlowEditor continue to consume compatibility projections of the same canonical document; the TextEditor consumes the EMScript projection of that document.

## Diagnostics

The guard distinguishes parse/import, identity reconciliation, pre-validation, transaction application, and roundtrip failures. Ambiguous identity and operation failures are returned as values. No fallback whole-document replacement occurs after a diagnostic.

## Scope Boundary

M3-2 adds no syntax, parser rewrite, property registry, editor renderer, VT2VT behavior, Component Designer, SmartPad UI, or generic inspector. Remote operation transport and broader importer convergence remain later M3 work.
