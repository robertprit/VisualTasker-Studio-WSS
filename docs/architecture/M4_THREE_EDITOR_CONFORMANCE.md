# M4 Three-Editor Conformance Contract

## Purpose

M4 proves that TextEditor, BlockEditor, and FlowEditor are reversible projections of one canonical
workflow. It does not turn visual layout into runtime semantics and does not implement runtime error
handling. Runtime execution of newly modeled constructs belongs to M5.

## Authority and Projection Chain

```text
TextEditor source --parse/import/diff--> WorkspaceTransaction --+
BlockEditor intent --------------------> WorkspaceOperation -----+--> CanonicalWorkspaceDocument
FlowEditor intent ---------------------> WorkspaceOperation -----+
                                                                  |
                              +-----------------------------------+
                              v
                compatibility blocks/connectors + semantic IR
                              |
                  +-----------+-----------+
                  v           v           v
                Text        Block        Flow
              projection  projection   projection
```

`CanonicalWorkspaceDocument` is the semantic truth. Compatibility block data remains necessary for
current rendering and payload projection, but it may not contradict canonical entities or relations.

## Binding Invariants

1. **Canonical authority.** WorkflowGraph/`CanonicalWorkspaceDocument` is the only writable semantic
   authority. Editor documents are projections or projection-owned state.
2. **No competing editor models.** Text, Block, and Flow may cache presentation data, but may not own
   an independent semantic workflow.
3. **Operation boundary.** Every semantic editor change is represented by one or more
   `WorkspaceOperation`s inside one atomic `WorkspaceTransaction`.
4. **Read-only switching.** Opening, focusing, switching, regenerating, or arranging an editor must
   not mutate canonical entities, relations, properties, IDs, order, or revision.
5. **Stable identity.** Semantically unchanged statements, expressions, branches, variables, and
   relations retain their IDs through projection, save/reload, and repeated roundtrips.
6. **Explicit structure.** Sequence, expression ownership, branch containment, references, and order
   remain explicit canonical relations. Pixel position and render order never infer identity.
7. **Projection metadata preservation.** Block layout, FlowView layout, collapse/facet state, source
   anchors, labels, and other projection-owned metadata survive unrelated semantic edits.
8. **No silent loss.** Unsupported constructs, opaque source, missing plugin definitions, invalid
   anchors, and incompatible relations produce deterministic diagnostics. They are never silently
   deleted or normalized into a different construct.
9. **Atomic failure.** Parse, reconciliation, validation, property, relation, or projection failure
   leaves the published canonical document, revision, history, and selection unchanged.
10. **Runtime independence.** Runtime semantics depend on canonical semantic data and registered
    command contracts, never on node geometry, colors, facets, canvas positions, or editor focus.
11. **Deterministic projection.** Equal canonical input plus equal projection state yields equal
    serialized/projected output and diagnostic order.
12. **Selection is referential.** Cross-editor focus resolves through stable semantic IDs. Missing
    targets are cleared diagnostically, never remapped by line, pixel, or render position.

## Editor Responsibilities

### TextEditor

- Owns draft text, caret, selection, formatting, and source diagnostics.
- Applies semantic edits only through parse, anchor reconciliation, semantic diff, and one transaction.
- May preserve runtime-inert projection directives.
- Must not publish a partially imported or ambiguously reconciled document.

### BlockEditor

- Owns BlockView layout and interaction state.
- Projects canonical statements, expressions, branches, variables, and relations into blocks/slots.
- Emits semantic operations for structural/property edits; drag-only layout changes remain projection data.
- Must preserve IDs when replacing presentation or reconnecting existing semantic entities.

### FlowEditor

- Owns FlowView layout, routing, facets, visibility layers, and analysis presentation.
- Projects semantic IR/canonical identity into nodes and edges.
- Synthetic joins, routes, facets, and off-page helpers are projection artifacts unless an explicit
  canonical construct exists.
- Semantic connect, detach, reorder, branch, property, and delete actions use the same operation boundary.

## Metadata Classes

| Class | Examples | Semantic mutation? | Persistence owner |
| --- | --- | --- | --- |
| Canonical semantic | entity kind, relation kind/role/order, typed property, variable reference | Yes | Canonical workspace |
| Shared projection identity | source anchors, legacy source ID, semantic selection target | No independent mutation | Canonical/source metadata contracts |
| BlockView | root position, collapse presentation, toolbox/view state | No, unless an explicit semantic property exists | Block/workspace view persistence |
| FlowView | node position, route, facet bounds/collapse, layer visibility | No | `FlowViewDocument` |
| TextView | draft, caret, folds, formatting | No until Apply succeeds | Text editor persistence |

Unknown metadata is retained when its owner survives. A projection may ignore data it cannot render,
but it may not erase that data during an unrelated semantic transaction.

## Planned TRY/CATCH/FINALLY/THROW Contract

This is an M4 structural target, not current support:

- `TRY` is one stable statement entity.
- The protected body, zero or more ordered `CATCH` clauses, and optional `FINALLY` are stable branch
  entities owned by the `TRY` statement through containment relations.
- Each catch clause has stable identity independent of order. Reordering changes relation order, not ID.
- Catch binding/type/filter data is typed semantic payload, not a label or FlowEditor annotation.
- `THROW` is a statement entity with an optional/required expression relation as fixed by the later
  language contract; the expression retains its own identity.
- Text source anchors address the try statement, each branch, throw statement, expression, and their
  canonical relations.
- BlockEditor may render a mutator/container; FlowEditor may render try/catch/finally regions and
  exceptional edges. These are projections of the same canonical structure.
- Unsupported TRY-family input must fail before publication until all three projections can preserve
  it. No placeholder block may silently discard catch/finally structure.
- Execution, propagation, stack unwinding, catch matching, finally guarantees, and runtime diagnostics
  are M5 responsibilities.

The current relation algebra has `Then`, `ElseIf`, `Else`, and `LoopBody` branch roles only. Adding
TRY-family roles requires a separately approved M4 slice with schema, migration, parser, generator,
BlockEditor, FlowEditor, and conformance tests together.

## M4 Exit Gate

M4 is complete only when every `implemented` row in the conformance matrix has a central cross-editor
test, every `partially implemented` row is either completed or explicitly deferred, and every
unsupported construct fails without mutation or information loss. Passing isolated editor tests is
necessary but not sufficient.

## Smallest M4-0 Slice

**Name:** Supported-subset three-editor conformance harness.

**Goal:** Establish one executable, canonical fingerprint-based test harness before migrating or
adding editor behavior.

**Affected modules:**

- `app/src/test/.../workspace/conformance` for the shared end-to-end harness;
- existing EMScript importer/apply and `WorkspaceWorkflowState` as consumers only;
- BlockEditor `workflow-core`, generator, and serializer test fixtures as existing dependencies;
- FlowEditor projection and `FlowViewDocument` codecs as existing dependencies.

**Fixture:** LET/SET variable, nested reporter expression, IF/ELSEIF/ELSE, REPEAT or WHILE, statement
sequence, custom unknown metadata, BlockView root position, and FlowView node position.

**Acceptance:**

1. Text import creates one canonical fingerprint of entity IDs, relation IDs/roles/order, variables,
   and represented properties.
2. Creating Block and Flow projections changes neither fingerprint nor revision.
3. Generated anchored text reapplies with zero semantic operations and stable IDs.
4. Save/reload preserves canonical fingerprint and projection-owned metadata.
5. One invalid anchor and one incompatible relation return deterministic diagnostics and leave the
   prior document byte-identical.
6. The harness explicitly labels unsupported FOR, function, and TRY-family fixtures as rejected,
   not skipped or normalized.

**Minimal regression test:** `supportedSubsetSurvivesTextBlockFlowTextWithoutMutationOrIdentityLoss`.

This slice adds conformance evidence first. It does not add syntax, runtime behavior, UI, or a second
mutation layer.
