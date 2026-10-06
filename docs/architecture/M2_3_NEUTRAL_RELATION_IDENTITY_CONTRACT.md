# M2-3 Neutral Relation and Semantic Identity Contract

Status: **CONTRACT FROZEN; NO MODEL MIGRATION**

Date: 2026-10-06

Scope: repository audit, neutral contract primitives, isolated contract tests. No schema, reducer,
serializer, editor, IR, runtime, layout, routing, operation, or VT2VT migration.

## 1. Executive Result

The current canonical authoring state remains `WorkspaceDocument` schema 1. `BlockId` is stable in
save/load and across Block/IR/Flow projections, but text import creates new IDs and then relies on
`WorkspaceIdentityReconciler` path/type matching. Expressions use reporter `BlockId`s rather than an
explicit neutral expression identity. Branches are not entities: ELSEIF identity is owner plus a
positional slot name/index. A structural relation is persisted twice as reciprocal connector
endpoints and has no single canonical identity.

The smallest neutral algebra that represents current semantics is:

```text
Entity kinds:   WORKFLOW, STATEMENT, EXPRESSION, BRANCH, VARIABLE
Relation kinds: SEQUENCE, EXPRESSION, CONTAINMENT, REFERENCE
Roles:          NEXT, ROOT, VALUE, CONDITION, ARGUMENT(name), OPERAND,
                BRANCH(role), BODY, VARIABLE
Order:          separate non-negative property, never identity
```

`SemanticEntityId`, `SemanticRelationId`, typed refs, kinds and roles are now frozen in
`workflow-core`. They are serializable but are not yet part of `WorkspaceDocument` or schema 1.
Contract fixtures A-G prove expressiveness and invariants without changing production behavior.

Decision: **C**. Stable branch/expression identities and single structural relations must be
introduced together in a bounded migration because current identities are encoded by the same
slot/endpoint structures that relations must replace.

## 2. Current Identity Inventory

| Identity | Type / owner | Creation | Lifetime and persistence | Stability | Runtime visibility |
|---|---|---|---|---|---|
| Workspace/document | `String`, `workflow-core` | caller/importer | schema 1 `id` | save/load yes; import caller-controlled | IR source/workspace |
| Block/node | `BlockId`, `workflow-core` | UUID reducer/importer; fixture IDs | schema 1 map key | save/load yes; projections derive `block:<id>`; text roundtrip heuristic | statement/value trace source |
| Variable | `String`, `VariableDefinition.id` | importer/UI | schema 1 registry key and value | save/load yes; source currently name-driven in places | lookup/evaluation |
| Connection endpoint | `ConnectionId` | factory from owner/slot or custom | schema 1 on each endpoint | save/load yes; changes when owner/positional slot changes | validation/traversal only |
| Value input | `ValueInput.name` + endpoint ID | block definition/importer | inside block | stable only while slot name is stable | type/IR lowering |
| Statement input | `StatementInput.name` + endpoint ID | block definition/importer | inside block | stable only while slot name is stable | traversal/IR lowering |
| Field | map key `String` | definition/catalog | inside block | definition-local; no independent identity | command lowering |
| Root | `BlockId` membership + list index | reducer/importer | `rootBlocks` | entity stable; order can be geometry-influenced | execution entry ordering |
| Expression | reporter `BlockId` indirectly | importer/reducer | reporter block in schema 1 | save/load yes; no neutral expression ID; text roundtrip heuristic | evaluator sees tree, not expression ID |
| Reporter | `BlockId` | importer/reducer | block | same strengths/weaknesses as block | value evaluation source |
| Branch | absent; derived owner/role/index/slot | IR generator | not a schema entity | positional/derived | IR branch refs only |
| ELSEIF | `ELIF[_n]` and `ELIF_CONDITION[_n]` | importer/mutator | slot names + order | **positional**; removal renumbers meaning | derived branch trace ref |
| ELSE | slot `ELSE` | definition/importer | slot name | derived from owner and fixed role | derived branch trace ref |
| Statement region | parent `BlockId` + slot name | definition/importer | endpoint topology | derived; no own ID | scope derivation |
| Loop body | owner + `DO`/`BODY` slot | definition/importer | endpoint topology | derived; no own ID | derived branch/scope |
| Container | parent block + statement slot | definition/importer | endpoint topology | derived; no own ID | traversal |
| Command invocation | statement/reporter `BlockId` + command ID/type | importer/reducer | block | block stable; invocation has no distinct ID | command execution maps to block |
| IR node | `IrGraphNodeId("block:<BlockId>")` | IR generator | derived, not canonical persistence | stable while BlockId survives | runtime/trace/Flow |
| IR edge | source+target+kind+label string | IR generator | derived | deterministic, changes on reconnect/label | trace/projection |
| Flow node/edge | `FlowNodeId`/`FlowEdgeId` | projector | projection document/view | block-backed nodes inherit IR ID; joins/terminators derived | Flow runtime view |
| Selection | block/flow/edge/line strings | UI | history entry/session | transient; reconciliation falls back to line/path | no semantic execution role |

## 3. Identity Classification

| Identity | Classification | Reason |
|---|---|---|
| Workspace ID | `CANONICAL_STABLE` | persisted authoring identity |
| BlockId for statement/reporter | `CANONICAL_STABLE` with roundtrip caveat | persisted and projected; text import requires heuristic recovery |
| VariableDefinition.id | `CANONICAL_STABLE` with duplicated-name caveat | persisted, but some source/runtime paths resolve names |
| ConnectionId | `DUPLICATED` | identifies one endpoint, not the semantic relation |
| Input/field identity | `CANONICAL_DERIVED` | owner plus stable catalog/slot key; dynamic branch slots are positional |
| Root membership order | `DUPLICATED` | list order competes with presentation coordinates |
| Expression identity | `MISSING` as neutral contract; reporter BlockId is current surrogate |
| Branch/ELSEIF/ELSE identity | `POSITIONAL` / `MISSING` | owner/index/slot derivation |
| Statement/loop region | `CANONICAL_DERIVED` | owner plus slot, no independent addressability |
| IR node IDs | `CANONICAL_DERIVED` | deterministic projection of BlockId |
| IR/Flow edge IDs | `PROJECTION_LOCAL` | derived from projected topology |
| Synthetic join/terminator/facet IDs | `PROJECTION_LOCAL` | no authoring entity |
| Source line/range | `TRANSIENT` | formatting-dependent lookup aid, never identity |
| Pixel position | `TRANSIENT` presentation | must never identify semantic content |

## 4. Expression Identity

For `(a + b) > (c * d)`, the current workspace has five reporter block identities for compare,
plus, multiply and the variable/literal reporters. The structured `IrExpression` tree has no IDs.
The graph IR recreates value nodes as `block:<BlockId>`. Thus reporter `BlockId` is the only stable
surrogate today, but it couples semantics to a Block-shaped representation.

Move and reconnect preserve the reporter block ID while changing relations. Save/load preserves it.
Undo/redo restores it from serialized history. Text import allocates new IDs and path/type
reconciliation preserves an old ID only when the same structural path and type still match. Source
formatting is ignored only after reparsing; structural insertions can shift paths.

Contract: each operation-addressable expression receives `SemanticEntityKind.Expression` identity.
Literal values remain properties. Operand/argument/condition/value placement is expressed by an
`Expression` relation and a role, not by expression identity.

## 5. Branch Identity

Current IF branches are encoded by value/statement slot names and list order. ELSEIF A removal can
make ELSEIF B change from index 2/`ELIF_2` to index 1/`ELIF`, so B is not stable for selection,
trace, undo operations, or distributed operations.

Contract: THEN, every ELSEIF, ELSE and loop body are `Branch` entities with stable IDs. The control
statement contains branch entities through `Containment + Branch(role)` relations. Branch order is
the relation's `order`; changing it does not change branch identity. A condition is an
`Expression + Condition` relation owned by the branch. A body is `Containment + Body` from branch
to its first/member statements. ELSE has no condition relation.

## 6. Connection Representation

Schema 1 stores a relation as two `Connection` records. Example sequence A→B:

```text
A.next.connectedTo     = B.previous.id
B.previous.connectedTo = A.next.id
```

The same pattern applies to output/value-input and statement-input/previous. The reducer writes and
disconnects both endpoints. The serializer persists both without choosing an authoritative side.
The validator checks partner existence and selected kind compatibility, but does not require the
partner to point back. Therefore contradictory raw state is representable: **YES**.

Current truth in healthy documents is the reciprocal pair maintained by `WorkspaceReducer`; there
is no canonical endpoint if the pair disagrees. A future importer must reject or diagnose ambiguity,
not silently trust whichever endpoint was traversed first.

## 7. Relation Inventory

| Current semantic relation | Source → target | Cardinality/order | Constraint | Persistence | Projection |
|---|---|---|---|---|---|
| statement sequence | statement → next statement | 0..1 outgoing, ordered topology | Next ↔ Previous | paired endpoints | sequence edge |
| root membership | workflow → root statement | many, ordered | target exists, not nested/value child | list plus positions | entry/root layout |
| value/expression | consumer → expression | role-dependent 0..1/many, ordered args | accepted/provided type | ValueInput ↔ Output | data/condition edge |
| command argument | invocation → expression | named and/or ordered | catalog type/cardinality | value slot | data edge/property |
| condition | branch/control → expression | required except ELSE | Boolean | value slot | condition edge |
| branch ownership | control → branch | ordered set | legal branch roles/cardinality | implicit slots/index | branch/scope records |
| statement body | branch/container → statements | ordered sequence | statement-only | StatementInput ↔ Previous plus Next chain | branch edge/scope |
| loop body | loop → branch/body | one | loop-specific role | statement slot | loop-body/loop-back |
| variable reference | expression/assignment → variable | one where required | variable exists/type compatible | field/name/id | value node/property |
| merge/join | branch exits → continuation | derived | control-flow lowering | not authored | synthetic IR/Flow edges/nodes |
| container membership | semantic owner → member | ordered where applicable | owner/member kinds | implicit topology | scopes/facets |

Fundamental relation kinds are `Sequence`, `Expression`, `Containment`, and `Reference`. Condition,
argument, operand, branch, root, body and variable are roles. Merge/join is derived execution/Flow
structure and is deliberately excluded from authoring truth.

## 8. Ordering

Identity and order are independent. The contract stores `order: Int?` on a relation. It orders roots,
arguments/operands, branches and contained members where topology alone is insufficient. Sequence
uses explicit relations; relation list order has no meaning. Reorder preserves entity and relation
IDs when the same membership/attachment remains. Reconnect to a different semantic target deletes
the old relation and creates a new relation identity. Detach plus later reconnect likewise creates a
new relation. Presentation coordinates never determine semantic order.

## 9. Ownership

`workflow-core` owns canonical entity/relation primitives. `workflow-semantics` owns catalog, type,
validation and lowering rules over them. Text, Block and Flow own projection-specific adapters and
view state. Runtime consumes lowered semantic/IR identities. Persistence will eventually own a
versioned mapping, but schema 1 remains authoritative until a dedicated migration.

## 10. Text Projection

Text parsing must assign or reconcile semantic IDs independently of source range. For `LET x = a+b`,
neutral entities are the declaration statement, variable, plus expression, two reference expressions
and workflow. Relations are root containment, declaration→variable reference, declaration→plus
value, plus→operand A/B, and each reference→variable. Whitespace, comments and line movement do not
define identity. A future lossless source map attaches spans to semantic IDs as projection data.

## 11. Block Projection

| Neutral fact | Block projection |
|---|---|
| Statement entity | command/control block with same semantic ID |
| Expression entity | reporter block with same semantic ID |
| Sequence + Next | next/previous connector pair |
| Expression + named role | value slot/output connector pair |
| Branch entity | C-shape branch region; branch ID retained outside slot index |
| Containment + Body | statement input plus stack |
| Reference + Variable | variable field/reporter |
| order | deterministic slot/row/stack order |

Connectors, docking, snap, rows, shapes and geometry remain Block projection state.

## 12. Flow Projection

Statements and expressions project to Flow nodes carrying semantic source IDs. Sequence,
Expression, Containment and Reference become suitable Flow edges or node properties. Branch entities
project to branch ports/scopes. Synthetic merges, loop-back edges, off-page connectors, terminators,
facets and routed edge IDs are Flow/IR projection identities unless explicitly authored later.
Flow node position, route and selection never mutate semantic identity.

## 13. IR / Runtime / Trace

The current structured IR is execution-oriented and discards statement/expression/branch IDs. The
graph IR preserves BlockId-derived node IDs but derives branch IDs from position and edge IDs from
topology. Therefore IR is not the authoring graph.

Lowering must retain source references for statement, evaluated expression and entered branch.
Execution-only joins/scopes may be created and discarded. Trace needs stable IDs to report
`executing statement X`, `evaluating expression Y`, and `entered branch Z`; expression and branch
trace are currently incomplete contract gaps.

## 14. WorkspaceOperation Requirements

Future operations address `SemanticEntityId`, `SemanticRelationId` and stable roles, never pixels,
list index alone or connector objects. Required operations include move/reorder entity, connect,
disconnect, insert into branch, replace expression and remove branch. Preconditions should carry a
workflow revision and expected relation endpoints. WorkspaceOperation itself is not introduced here.

## 15. Proposed Neutral Contract

Implemented contract primitives:

- `SemanticEntityId`, `SemanticRelationId`
- `SemanticEntityKind`: Workflow, Statement, Expression, Branch, Variable
- `SemanticEntityRef`
- `SemanticRelationKind`: Sequence, Expression, Containment, Reference
- `SemanticRelationRoleKind`: Next, Root, Value, Condition, Argument, Operand, Branch, Body, Variable
- `SemanticBranchRole`: Then, ElseIf, Else, LoopBody
- `SemanticRelationRole` with stable optional slot name and typed branch role
- `SemanticRelation` with owner→member direction and separate non-negative order

Every persisted authoring relation in the future contract has an ID because connect/disconnect,
selection, undo and VT2VT must address it. Purely derived IR/Flow edges do not receive semantic
relation IDs. This avoids assigning IDs to labels, visual rows, connectors, commas or geometry.

## 16. Persistence Impact

No current impact: schema remains 1 and the new types are not serialized by `WorkflowSerializer`.
A future schema migration is required to persist branch identities, expression identities that are
not safely reusable BlockIds, single relations, relation IDs and order independent of geometry.
Migration must deterministically pair valid reciprocal endpoints, diagnose contradictions, create
stable branch IDs before renumbering, preserve existing BlockIds as initial statement/expression IDs,
and retain an exact schema-1 compatibility reader.

## 17. Compatibility Strategy

1. Keep schema 1 reader/writer unchanged in M2-3.
2. Introduce a read-only schema-1→contract adapter in M2-4 with diagnostics for asymmetric pairs.
3. Reuse `BlockId.value` for initial statement/expression semantic IDs where one block represents one
   semantic entity.
4. Reuse variable IDs; add a typed wrapper at the boundary.
5. Generate and persist branch/relation IDs only in a versioned migration.
6. Keep connector IDs as Block projection IDs; do not pretend either endpoint is a relation ID.
7. Keep old projection IDs readable while all new projections source-map to semantic IDs.

## 18. DUPLICATED_TRUTH Matrix

| # | Case | Owner A | Owner B | Canonical truth | Identity/relation? | M2-3 resolution | Later slice |
|---:|---|---|---|---|---|---|---|
| 1 | command identity | parser/catalog name | block type/metadata | command catalog ID | identity-adjacent | unchanged; not relation core | catalog cleanup |
| 2 | variable identity | source/name lookup | registry ID/fields | stable variable semantic ID | identity | contract says Variable entity | M2-4 migration |
| 3 | sequence relation | source/list order | reciprocal Next/Previous | Sequence relation | relation | algebra frozen | M2-4 migration |
| 4 | value relation | AST nesting | reciprocal Output/ValueInput | Expression relation + role | relation | algebra frozen | M2-4 migration |
| 5 | statement relation | branch list | StatementInput/Previous + chain | Containment/Sequence | relation | algebra frozen | M2-4 migration |
| 6 | root order | root list | Y/X/ID sorting | containment order | relation/order | contract forbids geometry truth | M2-4/M2-6 |
| 7 | ELSEIF shape/order | slots/count/index | IR branch/index | stable Branch entity + order | identity/relation | contract frozen | M2-4 migration |
| 8 | source mapping | AST spans/line | block metadata/reparse | projection attachment to semantic ID | identity mapping | requirement frozen | source-map slice |
| 9 | Flow projection mapping | direct projector | IR projector | one IR→Flow path | projection | obsolete path identified | projection cleanup |

Count remains **9 → 9** because no production representation was migrated.

## 19. MISSING_CONTRACT Matrix

| # | Missing after M2-2 | Impact | Priority | M2-3 relevance/result | Proposed owner |
|---:|---|---|---|---|---|
| 1 | stable expression identity | weak roundtrip/trace/operation address | critical | contract added, persistence absent | workflow-core |
| 2 | stable branch identity | ELSEIF reorder changes meaning | critical | contract added, persistence absent | workflow-core |
| 3 | neutral relation identity/model | paired contradictory endpoints | critical | contract added, migration absent | workflow-core |
| 4 | semantic root ordering | geometry can affect execution order | critical | ordering contract added, migration absent | workflow-core |
| 5 | FOR | V1 gap | high | later | workflow-core/language |
| 6 | BREAK | V1 gap | high | later | workflow-core/language |
| 7 | CONTINUE | V1 gap | high | later | workflow-core/language |
| 8 | function identity/body | V1 gap | high | later | workflow-core/language |
| 9 | function parameters | V1 gap | high | later | workflow-core/language |
| 10 | RETURN | V1 gap | high | later | workflow-core/language |
| 11 | lossless extension envelope | unknown constructs degrade | high | later | workflow-core |
| 12 | project/presentation attachment contract | view facts mixed with intent | high | explicitly later | project core |

The contract-level count is **12 → 9**: expression identity, branch identity and neutral structural
relations now have frozen contracts, but remain implementation gaps until migration. Root ordering is
specified as part of relations but remains counted until production no longer consults geometry.

## 20. Risks

- Treating reporter BlockId as universally equivalent to ExpressionId fails for future non-block text
  expressions or one visual block representing multiple semantic entities.
- Renumbering ELSEIF slots before assigning IDs loses selection, trace and remote-operation targets.
- Choosing one side of an asymmetric schema-1 connection can silently change meaning.
- Reusing projection edge IDs as relation IDs couples authoring to lowering labels and topology.
- Adding relation objects beside reciprocal endpoints without one-way authority creates a third truth.
- Migrating root order while geometry still sorts roots can change execution behavior.
- A generic entity graph without kind/role constraints would accept invalid workflows.

## 21. Recommended M2-4

Proceed with **M2-4 Bounded Semantic Identity + Relation Migration**:

1. Implement a read-only adapter from schema-1 `WorkspaceDocument` to the frozen contract.
2. Emit explicit diagnostics for missing, asymmetric, duplicate and incompatible endpoint pairs.
3. Reuse BlockIds as initial statement/expression IDs and variable IDs as Variable identities.
4. Allocate stable BranchIds before any branch reorder/remove operation.
5. Convert sequence, expression, branch/body and root facts into one-way relations.
6. Prove fixtures A-G, save/load compatibility, projection identity and undo selection.
7. Do not yet replace schema 1; first compare adapter output against current traversal/IR behavior.

Only after equivalence is proven should a versioned persistence migration be proposed.
