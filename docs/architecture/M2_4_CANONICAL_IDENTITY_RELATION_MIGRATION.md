# M2-4 Canonical Identity and Relation Migration

Status: **READ MODEL INTEGRATED; WRITE MIGRATION DEFERRED BY STOP CONDITION**

Date: 2026-10-06

## 1. Previous Model

Schema 1 persists `WorkspaceDocument.blocks`, `rootBlocks`, variables and presentation-compatible
root positions. Sequence, value and body relations are stored as reciprocal `Connection` endpoints.
Expressions are reporter blocks. Branches are statement slots such as `THEN`, `ELIF_1` and `ELSE`.

The existing mutation surface is too broad for a bounded replacement: the repository contains 544
direct document-block access sites, 143 `WorkspaceDocument` constructions, 28 relevant copy paths
and a connector-addressed reducer used by Block, Flow and EMScript import. Adding mutable canonical
relations beside those fields would create prohibited dual truth. Replacing them in this slice would
require the explicitly excluded BlockEditor interaction rewrite.

## 2. Canonical Model

`CanonicalWorkspaceDocument` is a projection-independent read-only authoring model containing:

- document ID and revision;
- typed semantic entities with origin metadata;
- one directed `SemanticRelation` per structural fact;
- no connectors, geometry, viewport, selection, renderer or editor objects.

`LegacyWorkspaceCanonicalizer` derives it deterministically from schema 1. The result is integrated
into every `WorkspaceWorkflowState` as `canonicalDocument` plus diagnostics. It is recomputed from
the one current mutable authority and cannot be mutated independently.

## 3. Entity Identity

Schema-1 BlockIds map to `block:<BlockId>` and retain their existing stable value. Variables map to
`variable:<VariableDefinition.id>`. Workflow maps to `workflow:<WorkspaceDocument.id>`. Prefixes
provide one collision-free semantic namespace while preserving the legacy source ID.

Identity origin is explicit: `Native`, `LegacyBlock`, `LegacyVariable` or `LegacyDerived`.

## 4. Expression Identity

Every block with an output connection maps to `SemanticEntityKind.Expression`. Nested operators,
comparators, literals, variable reporters and command reporters therefore have stable deterministic
IDs. Primitive values and punctuation remain properties/syntax rather than entities.

The ID survives schema-1 save/load and presentation moves because it derives from persisted BlockId,
not geometry. Native canonical expression relations can be reordered without changing entity or
relation IDs. Text import still relies on the existing path/type reconciler.

## 5. Branch Identity

Every recognized statement slot maps to a Branch entity:

```text
branch:<ownerBlockId>:THEN
branch:<ownerBlockId>:ELIF[_n]
branch:<ownerBlockId>:ELSE
branch:<ownerBlockId>:DO|BODY
```

These IDs are deterministic for schema 1 but retain the legacy limitation that ELSEIF slot
renumbering can change identity. Native canonical branches do not have that limitation: Branch ID and
relation order are independent, proven by removal/reorder tests.

## 6. Relation Identity

Canonical relations carry deterministic `SemanticRelationId`s. Legacy ConnectionIds are only input
evidence and are never reused as relation IDs. Reordering changes `order`, not relation identity.
Presentation moves do not affect relation identity. Detach removes a relation; reconnect should
create a new native relation during the future write migration.

## 7. Sequence

A valid `Next ↔ Previous` endpoint pair becomes one directed relation:

```text
Statement A --Sequence/Next--> Statement B
```

The canonical read model has one truth. The legacy reducer still owns mutable sequence state until
M2-5; therefore schema 1 remains the current write authority.

## 8. Expression Relations

`Output ↔ ValueInput` becomes a consumer-to-expression relation. `CONDITION` and
`ELIF_CONDITION_n` attach to the corresponding Branch. `VALUE` uses role Value. Known positional
operator inputs use Operand plus explicit order. Other named inputs use Argument(name) plus order.

Expression ordering is derived from the stable input list, never canvas position.

## 9. Containment

Containment represents:

- Workflow → root Statement or detached root Expression, role Root;
- control Statement → Branch, role Branch with typed branch role;
- Branch → body-head Statement, role Body.

The body sequence then follows ordinary Sequence relations.

## 10. Reference

Variable declarations, assignments and reporters with resolvable variable identity create one
Reference/Variable relation to the Variable entity. Unknown variable references produce diagnostics
instead of invented entities.

## 11. Ordering

Root, branch, operand and argument order are non-negative relation properties. Root order comes from
`rootBlocks`; `rootPositions` is ignored. Branch role and branch identity are distinct. Relation-list
iteration order has no semantic meaning.

## 12. Legacy Schema-1 Adapter

The adapter:

1. creates deterministic entities;
2. creates Branch entities before reading connections;
3. reads each endpoint pair once;
4. converts valid pairs to one relation;
5. recovers one-sided compatible links with a warning;
6. rejects missing, conflicting, duplicate-ID and incompatible links with errors;
7. validates the resulting canonical document.

Equal schema-1 content produces equal canonical IDs and relations across repeated loads.

## 13. Schema Strategy

Current schema remains **1**. Schema 2 was not introduced because native canonical write ownership
does not yet exist. Persisting relations now would either create an independently mutable second
store or require the excluded reducer/editor rewrite. A future schema 2 must be introduced together
with canonical mutation ownership, not before it.

Schema-1 read, save, reload and re-canonicalization are deterministic and semantically equivalent.

## 14. Compatibility Views

The current compatibility direction is:

```text
schema-1 WorkspaceDocument (single mutable authority)
                 ↓ read-only canonicalization
CanonicalWorkspaceDocument
                 ↓ source references
IR / Flow / Trace preparation
```

There is no reverse synchronization and no dual-write. The future target reverses the first arrow:
canonical mutable model → derived Block compatibility view. That belongs to M2-5.

## 15. Validator Invariants

`CanonicalWorkspaceValidator` checks:

- unique entity IDs;
- unique relation IDs;
- existing endpoints;
- relation-kind/role/entity compatibility;
- one outgoing Sequence relation per statement;
- duplicate semantic relation facts.

Negative order is rejected by the relation constructor. Root and branch containment require explicit
order. Legacy asymmetry/conflict is diagnosed before canonical validation.

## 16. IR/Trace Mapping

IR nodes already use `block:<BlockId>`, matching canonical statement/expression IDs. IR branch IDs
now use `branch:<ownerBlockId>:<slotName>`, matching the legacy canonicalizer. IR edges, scopes,
joins, loop-back edges and terminators remain derived. Existing source refs retain block, slot and
branch references, enabling statement/expression/branch trace addressing without a trace redesign.

## 17. Projection Compatibility

BlockEditor continues to consume schema-1 compatibility structures, so rendering, nested reporters,
IF branches, loops, snap and drag behavior are unchanged. FlowEditor still consumes projected IR;
synthetic joins remain projection-local. EMScript generation remains unchanged. The only visible
semantic change is a more stable internal branch source ID.

## 18. Migration Fixtures

Automated fixtures cover:

- A: A→B→C sequence;
- B: LET value expression;
- C: `(a+b)>(c*d)`;
- D: IF/THEN/ELSEIF/ELSE;
- E: multiple ELSEIF;
- F: native removal/reorder preserving remaining BranchId and RelationId;
- G: WHILE body;
- H: command reporter;
- I: variable reference;
- J: multiple roots independent of geometry;
- K: one-sided legacy endpoint with deterministic recovery and warning;
- L: conflicting endpoint with error and no silent winner.

Additional tests cover schema-1 save/reload, native expression reorder, negative order, duplicate
entities/relations, dangling endpoints, incompatible relations and duplicate sequence.
An app-level reconciliation test also proves that added blank lines preserve BlockIds, canonical
entity identities and canonical relations for otherwise unchanged EMScript.

## 19. DUPLICATED_TRUTH

Count remains **9 → 9**. The canonical read model is derived and immutable, so it does not add a
duplicate mutable truth. Reciprocal endpoints remain the schema-1 write truth until mutation
migration. Command/variable identity, source mapping, root representation and dual Flow projector
issues are not artificially marked solved.

## 20. MISSING_CONTRACT

Count remains **9 → 9 productively**. M2-3's identity/relation contracts now have executable model,
adapter, validation and workspace-state integration, but canonical mutation and persistence are not
complete. Paper/read integration is not counted as eliminating the production gap.

## 21. WorkspaceOperation Readiness

The canonical model can address, without editor objects:

- move/reorder entity X;
- insert X after Y through Sequence;
- attach expression X to role R of Y;
- insert statement X into Branch Z at order N;
- remove Relation R;
- reorder Branch Z;
- reference Variable V.

Read-model readiness is **YES**. Executable operation/reducer readiness is **NO** until M2-5.

## 22. Remaining Gaps

- Canonical model is not the mutable `WorkspaceDocument` authority.
- Schema 2/native IDs are not persisted.
- Legacy ELSEIF-derived IDs remain positional until first native migration.
- Existing reducer accepts ConnectionIds and mutates reciprocal endpoints.
- Text formatting identity is only as strong as current path/type reconciliation.
- Validation is available but not yet a mutation precondition.
- Runtime still lowers from schema-1 WorkspaceDocument rather than canonical relations.

## 23. Recommended M2-5

**M2-5 Canonical Mutation and Compatibility Projection** should:

1. define a bounded canonical reducer over entity/relation IDs;
2. migrate Connect/Disconnect/Insert/Reorder/Branch mutations first;
3. derive the legacy Block connector view one-way from canonical relations;
4. preserve BlockIds as statement/expression mappings;
5. allocate native stable BranchIds and RelationIds;
6. introduce schema 2 only when that reducer is authoritative;
7. keep a schema-1 migration reader and prove editor equivalence;
8. avoid WorkspaceOperation/VT2VT protocol work until the reducer is stable.
