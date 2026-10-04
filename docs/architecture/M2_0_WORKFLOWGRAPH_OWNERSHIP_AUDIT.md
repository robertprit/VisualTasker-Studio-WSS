# M2-0 WorkflowGraph Ownership & Semantic Truth Audit

Status: **AUDIT ONLY**

Date: 2026-10-03

Scope: active `VisualTaskerStudio-WSS` main line, BlockEditor composite build, FlowEditor composite build

Evidence labels: **VERIFIED BY CODE**, **VERIFIED BY TEST**, **INFERRED**, **UNKNOWN**

## 1. Executive Summary

The repository does not currently contain a canonical, editor-neutral `WorkflowGraph`, `WorkflowNode`, or `WorkflowEdge` model. The concrete workflow truth is `de.visualtasker.blockeditor.domain.WorkspaceDocument`, which is physically owned by the optional BlockEditor composite build. `WorkspaceWorkflowState` wraps that document and derives `IrGraph`, generated EMScript, and `FlowGraphDocument`; it does not replace it as an independent semantic model.

This means the documented intent boundary is conceptually correct but not yet physically enforced:

```text
CURRENT

Text draft --parse/import--> BlockEditor WorkspaceDocument <--mutate-- BlockEditor
                                  |
                                  +--> IrScript --> EMScript
                                  +--> IrGraph --> FlowGraph --> FlowView
                                  +--> Workspace runtime
                                  +--> Workspace JSON / VT2VT snapshot

TARGET

Text <--> Blocks <--> Flow
          |      |
          v      v
       canonical WorkflowGraph in shared core
                    |
                    +--> IR / VM / Runtime
```

The highest-risk ownership leak is not the FlowEditor. It is that loading, saving, importing, executing, validating, and mirroring a workflow all depend on a data model and serializer located in the BlockEditor modules. In addition, that model mixes semantic facts with BlockView presentation (`rootPositions`, `collapsed`, block-shaped slots and paired UI connections).

The audit identified:

- **25 `CORE_SEMANTIC` facts** already represented in usable form.
- **9 `DUPLICATED_TRUTH` cases** that can diverge.
- **16 `MISSING_CONTRACT` cases** required for the intended neutral model.
- **2 `PROJECTION` groups** that are correctly derived but currently depend on BlockEditor-owned input.

Verdict: `WorkflowGraph = canonical Intent Truth` is **NEIN** today. The architecture is **partially converged in orchestration**, but not in model ownership.

## 2. Current Source-of-Truth Model

### 2.1 Effective authority

**VERIFIED BY CODE**

`WorkspaceDocument` contains workflow identity/version, blocks, root list, variables, and root positions. Each `BlockNode` owns fields and all sequence/value/statement connection endpoints. See:

- `visualtasker-blockeditor/blockeditor-domain/.../Models.kt:3-61`
- `visualtasker-blockeditor/blockeditor-domain/.../WorkspaceGraph.kt:186-201`

`WorkspaceWorkflowState.fromDocument()` serializes this document and derives every other workflow representation:

- `WorkspaceSerializer.serialize(document)`
- `IrGraphGenerator().generate(document)`
- `EmscriptGenerator().generate(document)`
- `IrGraphFlowchartProjector.project(irGraph)`

Evidence: `app/.../workspace/model/WorkspaceWorkflowState.kt:23-50`.

Therefore:

| Question | Current answer |
|---|---|
| Canonical persisted workflow | `WorkspaceDocument` JSON |
| Canonical executable input | `WorkspaceDocument` |
| Canonical editor mutation target | `WorkspaceDocument` |
| Canonical text | No; manual text is a draft until Apply |
| Canonical IR | No; generated from `WorkspaceDocument` |
| Canonical FlowGraph | No; generated from `IrGraph` |
| Canonical FlowView | No; presentation only |

### 2.2 Architectural documentation versus implementation

**VERIFIED BY CODE AND DOCUMENTATION**

ADR 0001 correctly names the architectural role `WorkflowDocument`, calls `IRGraph` a projection/read model, and separates `FlowViewDocument`. It also explicitly acknowledges that the current concrete type remains `WorkspaceDocument`. The implementation still locates that type in `blockeditor-domain`, so the role is documented but the module boundary is not enforced.

### 2.3 Competing paths

The current Workspace Shell uses:

```text
WorkspaceDocument -> IrGraphGenerator -> IrGraphFlowchartProjector -> FlowGraphDocument
```

The retained `MainScreen` uses the older direct projector:

```text
WorkspaceDocument -> BlockEditorFlowchartProjector -> FlowGraphDocument
```

**VERIFIED BY CODE** in `WorkspaceWorkflowState.kt`, `IrGraphFlowchartProjector.kt`, `BlockEditorFlowchartProjector.kt`, and `MainScreen.kt:442-456`. This is a projection duplication and a future drift risk.

## 3. Repository and Module Inventory

| Area | Concrete implementation | Role today | Ownership finding | Evidence |
|---|---|---|---|---|
| WorkflowGraph | No neutral class found | Intended architecture only | Missing | VERIFIED BY CODE |
| WorkflowNode / WorkflowEdge | No neutral classes found | Intended architecture only | Missing | VERIFIED BY CODE |
| Workflow state wrapper | `WorkspaceWorkflowState` | Orchestration/cache of projections | App-owned, not canonical model | VERIFIED BY CODE |
| Workflow document | `WorkspaceDocument` | Actual semantic and persisted truth | BlockEditor-owned | VERIFIED BY CODE |
| Block graph helpers | `WorkspaceGraph` | Traversal/order/connection interpretation | BlockEditor-owned semantic logic | VERIFIED BY CODE |
| Block reducer/actions | `WorkspaceReducer`, `WorkspaceAction` | Mutates document | Mixes semantic and view actions | VERIFIED BY CODE |
| Serializer | `WorkspaceSerializer` | Canonical project payload | Serializes block representation and view facts | VERIFIED BY CODE |
| Validator | `WorkspaceValidator` | Type/graph validation | BlockEditor-owned validation | VERIFIED BY CODE |
| Structured code IR | `IrScript`, `IrStatement`, `IrExpression` | EMScript generation | Derived, smaller than target V1 | VERIFIED BY CODE |
| Semantic graph IR | `IrGraph`, nodes/edges/scopes/branches/facets | Flow/runtime mapping | Derived from blocks | VERIFIED BY CODE |
| Text AST | `EmscriptIrScript/Statement/Expression` | Parser output | No stable semantic IDs | VERIFIED BY CODE |
| Text importer | `EmscriptWorkspaceImporter` | Creates workflow truth from source | Emits BlockEditor document directly | VERIFIED BY CODE |
| Identity reconciliation | `WorkspaceIdentityReconciler` | Attempts ID preservation after Apply | Path/type heuristic | VERIFIED BY CODE |
| Flow semantic model | `FlowGraphDocument` | Read-only graph projection | Generic but not canonical | VERIFIED BY CODE |
| Flow presentation | `FlowViewDocument` | Node/edge geometry, viewport, annotations | Properly separate in Flow module | VERIFIED BY CODE |
| Flow mutations | `FlowchartWorkspaceMutation` | Flow UI edit adapter | Mutates BlockEditor model directly | VERIFIED BY CODE |
| Runtime | `WorkspaceDryRunRuntime`, `WorkspaceBasicRuntime` | Executes workflow | Direct BlockEditor model dependency | VERIFIED BY CODE |
| VT2VT | `Vt2VtPayloads.workspacePayload` | Whole-document mirror | Snapshot transport, not typed ops | VERIFIED BY CODE |

Static Gradle evidence: the app directly depends on seven BlockEditor artifacts and four FlowEditor artifacts (`app/build.gradle.kts:82-93`). The composite substitutions are declared in `settings.gradle.kts:11-33`.

## 4. Semantic Fact Inventory

The matrix in section 6 is normative for this audit. Counts refer to primary classification, not the number of concrete classes.

### 4.1 Present core facts: 25

Workflow identity, entry point, command identity/arguments/return/capability, variable label/type/scope/default/declaration-versus-assignment/reference, literals, expression tree, arithmetic, comparisons, nested expressions, statement order, IF condition, THEN, ELSE, WHILE, REPEAT, nested statement stacks, nullable/type compatibility, and domain/List type descriptors are represented.

Qualification: many are represented through block types, fields, slots, registry entries, and connection topology rather than in a neutral workflow schema. Their meaning exists, but ownership is wrong.

### 4.2 Derived facts: 2 groups

Generated EMScript spelling/format and generated IR/Flow graph structures are projections. They may be persisted as caches or drafts, but must not become competing intent truth.

### 4.3 Duplicated facts: 9

Command identity, variable identity, three connection kinds, root ordering, branch/mutator shape, source mapping, and the two FlowGraph projection paths have multiple representations or implementations.

### 4.4 Missing contracts: 16

The principal gaps are an editor-neutral canonical graph, neutral stable identities, single semantic relations, semantic root ordering, V1 control/function constructs, lossless unsupported constructs, editor-neutral operations, neutral validation ownership, and a complete project/presentation attachment contract.

## 5. Presentation Fact Inventory

Persistence does not make a fact semantic.

| Fact | Current location | Classification | Issue |
|---|---|---|---|
| Block root x/y | `WorkspaceDocument.rootPositions` and legacy block metadata | PRESENTATION | Stored inside canonical workflow payload |
| Block collapsed | `BlockNode.collapsed` | PRESENTATION | Stored inside semantic block node |
| Block visual/mutator metadata | `BlockNode.metadata` | PRESENTATION, sometimes mixed | Metadata also carries source and branch data |
| Block viewport/zoom/pan | BlockEditor session/controller | PRESENTATION | Correct role; persistence path is editor-specific |
| Block selection/hover/drag/snap | controllers / interaction state | EPHEMERAL | Correct unless copied into semantic state |
| Flow node x/y/size/pinned/collapsed | `FlowViewDocument.nodeViews` | PRESENTATION | Correctly separate |
| Flow routes/waypoints | `FlowViewDocument.edgeViews` | PRESENTATION | Correctly separate |
| Flow viewport/zoom/pan | `FlowViewDocument.viewport` | PRESENTATION | Correctly separate |
| Flow selection/focus/drag | `FlowInteractionState` plus workspace selection | EPHEMERAL | Selection is externally synchronized; controller attach can reset local interaction |
| Flow facets/groups | IR facets plus FlowView state | PROJECTION/PRESENTATION | Semantic group intent is not clearly separated from visual grouping |
| Text cursor/selection/scroll | TextEditor UI state | EPHEMERAL | No canonical workflow effect |
| Text draft | preference `workspace_text_editor_draft` | PRESENTATION/AUTHORING DRAFT | Deliberately non-canonical until Apply |
| Text formatting/comments | source draft / generator | PRESENTATION unless language-defined | Generator is not lossless for arbitrary formatting/comments |
| Workspace active panel/focus | Workspace UI | EPHEMERAL | Correct role |
| Panel geometry/layout | panel session/layout persistence | PRESENTATION | Separate from workflow meaning |
| Current projection choice | Workspace UI | EPHEMERAL | Must never select semantic authority |

Critical leak: `SyncViewPositions` copies FlowView root coordinates into `WorkspaceDocument.rootPositions`; root traversal can then be sorted by Y/X. A view edit can therefore influence observable root order.

## 6. Ownership Matrix

Legend: T = TextEditor/parser, B = BlockEditor, F = FlowEditor, R = Runtime/IR, P = persistence. “Owner” means current effective owner, not desired architecture.

| # | Semantic Fact | Current Owner / Representation | T | B | F | R | P | Classification | Risk | Recommended Canonical Owner |
|---:|---|---|---|---|---|---|---|---|---|---|
| 1 | Workflow identity | `WorkspaceDocument.id` | supplies import ID | owns | projects | reads | JSON | CORE_SEMANTIC | HIGH module leak | WorkflowGraph |
| 2 | Entry point | `event.start`, `rootBlocks` | importer creates | owns | entry node | traverses | JSON | CORE_SEMANTIC | MEDIUM | WorkflowGraph |
| 3 | Command arguments | block fields/value slots | parses | owns | projects | reads | JSON | CORE_SEMANTIC | HIGH | WorkflowNode command payload |
| 4 | Command return type | command catalog | validates | registry | node properties | evaluates | indirect | CORE_SEMANTIC | MEDIUM | shared language contract |
| 5 | Command capability/effect | command catalog metadata | resolves | registry | projects | adapter selection | indirect | CORE_SEMANTIC | MEDIUM | shared language contract |
| 6 | Variable label | registry definition/name fields | source name | owns | label | reads | JSON | CORE_SEMANTIC | HIGH | Workflow variable symbol |
| 7 | Variable type | `VariableDefinition.type` | declared type | owns | property | type safety | JSON | CORE_SEMANTIC | HIGH | Workflow type model |
| 8 | Variable scope | `VariableScope` | incomplete lexical syntax | owns enum | projects | reads | JSON | CORE_SEMANTIC | HIGH incomplete | Workflow scope model |
| 9 | Variable default | `VariableDefinition.defaultValue` | LET value | owns | projects | initializes | JSON | CORE_SEMANTIC | MEDIUM | Workflow variable declaration |
| 10 | LET versus SET | assignment field/kind | parses distinction | owns field | projects | executes | JSON | CORE_SEMANTIC | HIGH | Workflow statement kind |
| 11 | Variable reference binding | reporter block + variable ID/name | parses name | owns | data node | resolves | JSON | CORE_SEMANTIC | HIGH | Workflow symbol reference |
| 12 | Literal value/type | literal block fields | AST literal | owns | value node | evaluates | JSON | CORE_SEMANTIC | MEDIUM | Workflow expression |
| 13 | Expression tree | nested value connections | AST tree | owns topology | data edges | evaluates | JSON | CORE_SEMANTIC | CRITICAL ownership | WorkflowExpression |
| 14 | Arithmetic operators | normalized operator field | AST op | owns | projects | evaluates | JSON | CORE_SEMANTIC | MEDIUM | WorkflowExpression |
| 15 | Comparisons | compare operator + operands | AST op | owns | condition edges | evaluates | JSON | CORE_SEMANTIC | MEDIUM | WorkflowExpression |
| 16 | Nested expressions | nested reporter blocks | AST nesting | owns | projected graph | evaluates | JSON | CORE_SEMANTIC | HIGH | WorkflowExpression |
| 17 | Statement ordering | next/previous topology | source order | owns | sequence edge | traverses | JSON | CORE_SEMANTIC | CRITICAL duplicate endpoints | WorkflowRelation |
| 18 | IF condition | value input/condition reporter | AST | owns | condition edge | evaluates | JSON | CORE_SEMANTIC | HIGH | Workflow branch |
| 19 | THEN body | statement input stack | AST list | owns | true edge/scope | traverses | JSON | CORE_SEMANTIC | HIGH | Workflow branch |
| 20 | ELSE body | statement input stack | AST list | owns | false edge/scope | traverses | JSON | CORE_SEMANTIC | HIGH | Workflow branch |
| 21 | WHILE | block + condition/body slots | AST | owns | loop graph | executes | JSON | CORE_SEMANTIC | HIGH | Workflow loop |
| 22 | REPEAT | block + count/body | AST | owns | loop graph | executes | JSON | CORE_SEMANTIC | HIGH | Workflow loop |
| 23 | Nested statement stacks | statement inputs + next chains | nested lists | owns | scopes/edges | traverses | JSON | CORE_SEMANTIC | CRITICAL | WorkflowGraph |
| 24 | Nullable/type compatibility | language/core type systems | validates | validates | diagnostics | checks | indirect | CORE_SEMANTIC | HIGH split owner | shared type system |
| 25 | List/domain type descriptors | language type refs/catalog | parses type ref | registry strings | properties | checks | JSON strings | CORE_SEMANTIC | MEDIUM | shared type model |
| 26 | Generated source spelling | `EmscriptGenerator` | displays draft/projection | source input | none | parser/runtime may consume | draft/files | PROJECTION | LOW | Text projection |
| 27 | IR/Flow scopes, synthetic joins/facets | `IrGraphGenerator` / projector | none | source | displays | trace maps | optional | PROJECTION | MEDIUM | projection services |
| 28 | Command identity | parser name, catalog ID, block type/metadata | name/alias | type + metadata | label/properties | catalog ID | block JSON | DUPLICATED_TRUTH | HIGH | shared command ID |
| 29 | Variable identity | source name, registry key/id, block fields | name | several fields | label/property | name lookup | JSON | DUPLICATED_TRUTH | CRITICAL | stable variable ID |
| 30 | Sequence relation | source order + `next.connectedTo` + `previous.connectedTo` | list order | paired endpoints | edge | traversal | both endpoints | DUPLICATED_TRUTH | CRITICAL | single WorkflowEdge |
| 31 | Value relation | expression nesting + output/input endpoints | AST child | paired endpoints | data edge | traversal | both endpoints | DUPLICATED_TRUTH | CRITICAL | single WorkflowEdge |
| 32 | Statement relation | branch list + input/previous endpoints | AST list | paired endpoints | branch edge | traversal | both endpoints | DUPLICATED_TRUTH | CRITICAL | single WorkflowEdge |
| 33 | Root order | `rootBlocks` plus Y/X/ID sorting | source order | list + geometry | layout | `topLevelRoots` | list + positions | DUPLICATED_TRUTH | CRITICAL | ordered semantic roots |
| 34 | ELSEIF branch shape/order | block type, slots, count metadata, list index | AST list | multiple encodings | branch/scope IDs | traverses slots | JSON | DUPLICATED_TRUTH | CRITICAL | stable branch entities |
| 35 | Source mapping | AST spans, block metadata, resolver reparse | spans | metadata | source refs | trace | block metadata | DUPLICATED_TRUTH | HIGH | canonical source map |
| 36 | Flow projection mapping | direct projector and IR projector | none | two mappings | consumes either | trace mapping | no canonical cache | DUPLICATED_TRUTH | HIGH | one IR-to-Flow projector |
| 37 | Canonical neutral WorkflowGraph | absent | no | no | no | no | no | MISSING_CONTRACT | CRITICAL | shared workflow-core |
| 38 | Stable neutral node identity | BlockId only | no IDs | UUID BlockId | derives `block:` | uses BlockId | JSON | MISSING_CONTRACT | CRITICAL | WorkflowNodeId |
| 39 | Stable expression identity | absent | no IDs | reporter BlockId only | derived node | no expression ID | indirect | MISSING_CONTRACT | HIGH | WorkflowExpressionId |
| 40 | Stable branch identity | derived from owner/index/slot | no IDs | no branch entity | derived string | derived | no entity | MISSING_CONTRACT | HIGH | WorkflowBranchId |
| 41 | Neutral relation/edge identity | connection IDs and derived edge IDs | none | endpoint IDs | derived edge ID | inferred | endpoint IDs | MISSING_CONTRACT | CRITICAL | WorkflowEdgeId |
| 42 | Semantic root ordering independent of geometry | absent | source list | geometry affects order | layout | sorted roots | mixed | MISSING_CONTRACT | CRITICAL | WorkflowGraph roots |
| 43 | FOR | absent from current parser/workspace IR | unsupported | absent | generic kind only | absent | absent | MISSING_CONTRACT | HIGH V1 gap | Workflow loop |
| 44 | BREAK | absent | unsupported | absent | generic kind only | absent | absent | MISSING_CONTRACT | HIGH V1 gap | Workflow control stmt |
| 45 | CONTINUE | absent | unsupported | absent | generic kind only | absent | absent | MISSING_CONTRACT | HIGH V1 gap | Workflow control stmt |
| 46 | Function identity/body | absent from current workflow | unsupported | absent | generic domain kinds | absent | absent | MISSING_CONTRACT | HIGH V1 gap | Workflow function |
| 47 | Function parameters | absent | unsupported | absent | generic display only | absent | absent | MISSING_CONTRACT | HIGH V1 gap | Workflow function signature |
| 48 | RETURN | absent | unsupported | absent | generic kind only | absent | absent | MISSING_CONTRACT | HIGH V1 gap | Workflow return stmt |
| 49 | Lossless unsupported/legacy envelope | no general envelope | rejects/canonicalizes | unknown block diagnostics | unknown node | partial fallback | block JSON only | MISSING_CONTRACT | HIGH | shared extension node |
| 50 | Typed editor-neutral mutation operations | absent | Apply replaces document | block actions | adapter to block actions | none | snapshots | MISSING_CONTRACT | CRITICAL | workflow-core reducer |
| 51 | Canonical validation ownership | split parser/block/type/flow validators | parser validation | block validator | flow validator | runtime checks | decode checks | MISSING_CONTRACT | HIGH | workflow-core validator |
| 52 | Complete project/presentation attachment | fragmented saves/preferences | draft | mixed document/view | view JSON | trace separate | fragmented | MISSING_CONTRACT | HIGH | ProjectSnapshot + view attachments |

## 7. Source-of-Truth Traces

### 7.1 Simple command sequence

```text
EMScript command lines
  -> EmscriptParserSlice statements (no stable IDs)
  -> EmscriptWorkspaceImporter instantiates UUID BlockIds
  -> previous/next connection pairs encode sequence
  -> WorkspaceDocument JSON stores blocks and both endpoints
  -> IrGenerator/IrGraphGenerator traverse WorkspaceGraph
  -> generator emits canonical EMScript; Flow projector emits sequence edges
```

First durable identity: BlockId during import. Ordering is initially source order, then encoded as block connections. Reload reconstructs exactly those block endpoints. Without BlockEditor modules, import, storage, projection, and runtime cannot compile.

### 7.2 LET / SET

Parser distinguishes `LET` and `SET`. Importer creates/updates `VariableDefinition` and emits a variable-set block carrying assignment kind and declared type. The structured IR stores both through `IrStatement.SetVariable.assignmentKind`. Persistence stores variable registry plus block fields/connections. Existing roundtrip tests verify declaration/assignment distinction on covered fixtures.

Risk: variable name is still used as a practical lookup identity in several paths; stable variable identity is not uniformly separate from label.

### 7.3 Nested expression

Parser creates nested `EmscriptIrExpression`; importer creates reporter/operator blocks and connects output/input endpoints. The expression first receives durable IDs only as BlockIds. IR reconstructs nesting from connections. Expression nodes have no independent `ExpressionId`, so text re-import relies on path/type reconciliation.

### 7.4 IF / ELSE

Parser creates an IF statement with lists. Importer chooses a BlockEditor IF type, creates condition/value and statement inputs, then connects branch heads. `IrGraphGenerator` derives scopes, branch references, and true/false edges. Persistence stores slots and paired connection endpoints, not a neutral branch entity.

### 7.5 IF / ELSEIF / ELSE

The importer and Flow mutation adapter create dynamic slots and branch-count/type metadata. Branch ordering is the list/slot order. `IrGraphGenerator` derives branch IDs from owner BlockId, role, current index, and slot name (`IrGraphGenerator.kt:176-205`). Reordering/inserting branches can therefore change identity even when logical branch content survives.

### 7.6 WHILE

Parser creates condition and body. Importer emits a control block with value and statement slots. IR derives LOOP_BODY and LOOP_EXIT relations. Runtime interprets the BlockEditor topology directly. No neutral loop entity exists.

### 7.7 Value reporter / value connection

The producer output and consumer value input each store `connectedTo`. Flow derives one DATA_FLOW/CONDITION edge. Validation checks partner existence and connection kind, but not full reciprocal equality for every connection kind. The semantic relation therefore has two persisted endpoint truths before it becomes one derived edge.

## 8. Roundtrip Analysis

### 8.1 Text -> Workspace -> IR -> Block -> IR -> Text

**VERIFIED BY TEST for covered constructs.** Tests cover LET/SET, nested expressions, IF/ELSEIF, command identities, serialization, generated source, and stress fixtures.

Preserved on covered paths:

- statement/expression structure;
- variable declaration versus assignment;
- command IDs for tested catalog entries;
- branch count/order for tested IF fixtures;
- workspace serialization of block and connection IDs;
- type strings and nullable decisions covered by M1B tests.

Not generally guaranteed:

- original source formatting/comments;
- stable IDs across arbitrary insertion/reorder/duplicate same-type siblings;
- stable expression and branch IDs;
- unsupported future constructs;
- BlockView positions/collapse after text Apply;
- full V1 functions/FOR/BREAK/CONTINUE/RETURN.

### 8.2 Block -> IR -> Flow -> IR -> Block

There is no general inverse `FlowGraph -> IrGraph -> WorkspaceDocument`. Flow content edits are translated by `WorkspaceFlowchartMutations` directly into block actions. Therefore this is not a true graph roundtrip; it is a projection plus an editor-specific mutation adapter.

FlowView geometry is separately modeled, but `SyncViewPositions` writes connected root node positions back into `WorkspaceDocument`. This weakens the separation.

### 8.3 Text -> Flow -> Text

Implemented as a composite path:

```text
Text -> WorkspaceDocument -> IrGraph -> FlowGraph
Text <- EmscriptGenerator <- WorkspaceDocument <- Flow mutation adapter
```

It is not a direct Flow roundtrip. Its semantic fidelity depends on BlockEditor types and adapters.

## 9. Identity Audit

| ID | Creation | Survival | Finding |
|---|---|---|---|
| Workflow ID | Import argument/bootstrap | save/reload yes | Semantic, but named “workspace” and owned by block model |
| Block ID | UUID via `newBlockId()` | save/reload yes | De facto workflow node identity |
| Neutral node ID | absent | n/a | Missing contract |
| Flow node ID | `block:<BlockId>` or synthetic | projection only | Stable while BlockId and projection rule stay stable |
| Connection ID | block factory/importer | save/reload yes | UI endpoint identity, not a neutral relation identity |
| Variable ID | registry/importer | save/reload yes | Source name and ID remain insufficiently separated in all paths |
| Branch ID | derived owner/role/index/slot | regenerated | Unstable under branch structural changes |
| Function ID | absent | n/a | Missing contract |
| Expression ID | absent; reporter BlockId substitutes | heuristic through re-import | Critical gap for nested expression identity |
| IR edge ID | derived source/target/kind/label | regenerated | Deterministic projection, not semantic identity |
| Flow edge ID | derived projection key | regenerated | Stable only if source mapping remains identical |

`WorkspaceIdentityReconciler` matches imported blocks by semantic path plus block type, queues duplicate matches sorted by old ID, and remaps connection IDs by string replacement (`WorkspaceIdentityReconciler.kt:10-96`). This works for unchanged or simple field changes, but insertion, reorder, and duplicate same-type siblings can reassign semantic identity.

Selection synchronization assumes `FlowNodeId("block:<BlockId>")` (`WorkspaceSelectionState.kt:76-80`). This is deterministic but exposes the BlockEditor identity scheme as a cross-editor contract.

## 10. Connection Audit

### 10.1 Same semantic relationship, multiple representations

| Semantic relation | Block representation | IR representation | Flow representation |
|---|---|---|---|
| Sequence | next + previous endpoints | `SEQUENCE` edge | `SEQUENCE` edge |
| Value | output + valueInput endpoints | `DATA_FLOW` / `CONDITION` edge | matching edge |
| Branch body | statementInput + child previous | branch/scope + branch edge | branch edge and synthetic joins |
| Loop body/exit | statement slot + next chain | loop edges | loop edges |

The Block reducer normally updates both endpoints, which is the current synchronization protection. The validator checks that the referenced partner exists and checks some complementary kinds (`Validator.kt:111-132`), but no audited invariant proves `A.connectedTo == B.id` and `B.connectedTo == A.id` for every endpoint pair. A malformed or migrated document can therefore produce traversal disagreement.

FlowEditor cannot persist an independent semantic edge as workflow truth; its connect/disconnect operations are translated back to block connections. This avoids one class of dual truth, but makes Flow semantics dependent on BlockEditor adapters.

## 11. Control-Flow Audit

| Construct | State | Finding |
|---|---|---|
| `next` | Present, editor-dependent | Paired block connections; no neutral edge |
| IF true/false | Present, editor-dependent | Statement slots become derived branch edges |
| ELSEIF order | Present, implicit | Slot/list index; branch ID derived from index |
| ELSE | Present, editor-dependent | Statement slot |
| Loop body | Present, editor-dependent | Statement slot and derived edge |
| Loop exit | Present, implicit | Block next relation and derived edge |
| Function body | Missing | Flow domain has generic kinds but workflow/parser/runtime contract absent |
| Return | Missing | No current workflow/parser/runtime contract |

There is no neutral canonical `next`; the effective canonical control relation is BlockEditor connection topology.

## 12. Expression Audit

Literals, variable references, command reporters, arithmetic, comparisons, Boolean AND/OR, nullable domain values, and `List<T>` type descriptors are available across meaningful slices. Nested expression semantics are reconstructed from block value connections.

However:

- `IrExpression` is structurally editor-neutral but has no stable IDs.
- `IrGraphNode` identity is derived from reporter BlockId.
- Flow can display expressions through projected nodes/edges, but creation and mutation require BlockEditor definitions and slots.
- Runtime evaluates from `WorkspaceDocument` and `WorkspaceGraph`, not from a neutral expression graph.
- The current parser explicitly rejects `null` as a V1 literal pending a language decision.
- Unary `!` and complete list literal/index behavior are not established by the audited parser path.

Verdict: expression **meaning exists**, but expression **ownership and identity are not editor-independent**.

## 13. Mutator Audit

IF shape changes are semantic when they add/remove branches. Today branch mutation exists in at least these forms:

- BlockEditor controller/registry mutator handling;
- text import choosing IF block type and dynamic branch slots;
- app-level `FlowchartWorkspaceMutation.AddIfBranch/RemoveIfBranch` rebuilding BlockEditor shapes;
- branch count/type/slot metadata interpreted by IR generation.

The visual mutator UI is not the only route, which is positive. But all routes ultimately encode the change as BlockEditor shape/slots. Branch IDs are not stable entities, and no editor-neutral `AddBranch(branchId, position, condition, body)` operation exists. VT2VT cannot yet express this safely as a semantic operation.

## 14. Persistence Audit

### 14.1 Semantic persisted state

`WorkspaceSerializer` schema 1 persists:

- document ID/version;
- all block types/fields;
- previous/next/output/value/statement endpoints;
- root block list;
- variables.

### 14.2 Presentation persisted inside semantic payload

The same serializer persists:

- `rootPositions`;
- `collapsed`;
- arbitrary block metadata, including legacy root position and source/facet annotations.

Evidence: `WorkspaceSerializer.kt:29-121`.

### 14.3 Separate presentation/draft persistence

- Text manual draft uses `workspace_text_editor_draft`.
- Flow panel save writes `flowchart_view_json`.
- FlowView has a proper revision attachment contract (`documentId`, `compatibleDocumentRevision`).

No read path for `flowchart_view_json` was found in the audited source search. The preference currently appears save-only. This is **INFERRED** from repository-wide reference search and should be confirmed in M2 implementation work before deletion or migration.

### 14.4 History

BlockEditor undo/redo snapshots the whole `WorkspaceDocument`, so semantic and presentation changes share history. Workspace-level history stores serialized JSON plus selection. A future split must preserve user-visible undo semantics while separating canonical graph changes from view-only changes.

## 15. Editor Dependency Audit

### 15.1 FlowEditor dependencies

| Consumer | Dependency | Classification |
|---|---|---|
| Workspace state | Flow projection result/types | SUSPICIOUS; orchestration coupling |
| Workspace selection | `FlowNodeId`, `FlowEdgeId` | SUSPICIOUS shared selection coupling |
| Workspace mutation adapter | FlowGraph/View types | HARD_DEPENDENCY for Flow editing |
| VT2VT runtime/selection | Flow runtime/ID types | SUSPICIOUS |
| Persistence/runtime core | Does not require FlowView for workflow execution | SAFE semantically |

The module cannot currently be removed from Gradle without compilation work, but the persisted workflow meaning does not fundamentally reside in it.

### 15.2 BlockEditor dependencies

| Consumer | Dependency | Classification |
|---|---|---|
| Canonical workflow state | `WorkspaceDocument` | HARD_DEPENDENCY |
| Persistence | `WorkspaceSerializer` | HARD_DEPENDENCY |
| Text import | Block types, reducer, registry | HARD_DEPENDENCY |
| IR generation | Block document/graph/registry | HARD_DEPENDENCY |
| Runtime | Block document/graph/catalog | HARD_DEPENDENCY |
| Flow mutations | Block action/reducer/registry | HARD_DEPENDENCY |
| VT2VT snapshot | serialized block workspace | HARD_DEPENDENCY |

BlockEditor is optional only as UI in theory; its domain, registry, serialization, validation, IR, and EMScript modules are currently foundational core dependencies.

## 16. Runtime Independence

Verdict: **TEILWEISE**.

- No TextEditor UI instance is required.
- No BlockEditor Compose UI instance is required.
- No FlowEditor UI instance is required.
- Runtime does require BlockEditor domain and registry types at compile time and as its input data model.
- `WorkspaceDryRunRuntime.run(document: WorkspaceDocument)` traverses `WorkspaceGraph` directly (`WorkspaceDryRunRuntime.kt:24-65`). `WorkspaceBasicRuntime` follows the same ownership direction.

Thus runtime is editor-UI-independent but not editor-module/data-model-independent. This is a structural dependency, not merely a convenience import.

## 17. Duplicated Truth Register

| Priority | Fact | Owner A | Owner B | Synchronization | Divergence | Protection | Severity |
|---:|---|---|---|---|---|---|---|
| 1 | Sequence relation | next endpoint | previous endpoint | reducer writes both | one-sided relation changes traversal | partial validator/tests | CRITICAL |
| 2 | Value relation | reporter output | consumer input | reducer writes both | expression tree differs by traversal direction | type/connection tests | CRITICAL |
| 3 | Statement relation | statement input | child previous | reducer writes both | branch content can detach or move | branch tests | CRITICAL |
| 4 | Root ordering | `rootBlocks` order | Y/X/ID sort from presentation | helper sorting | moving a root can alter effective order | root sort test | CRITICAL |
| 5 | ELSEIF structure | type/slot list | branch count/metadata/index | importer/controller/adapters | branch count/order/identity drift | parser and mutation tests | CRITICAL |
| 6 | Variable identity | registry ID/key | labels/source names/block fields | importer and conventions | rename/reimport can rebind | focused roundtrip tests | CRITICAL |
| 7 | Command identity | command ID/catalog | source aliases/block type/metadata | normalization | alias/type lookup can disagree | M1B catalog tests | HIGH |
| 8 | Source mapping | AST spans | block metadata/reparse heuristic | selection resolver | focus can target wrong sibling/expression | sync tests | HIGH |
| 9 | Flow projection | direct Block projector | IRGraph projector | no common mapper | MainScreen and Shell can show different graph semantics | separate tests | HIGH |

## 18. Missing Contract Register

| Priority | Missing contract | Why required | Current substitute | Severity |
|---:|---|---|---|---|
| 1 | Editor-neutral canonical WorkflowGraph | Optional modules must not own project truth | `WorkspaceDocument` | CRITICAL |
| 2 | Stable neutral WorkflowNodeId | Cross-editor identity must not be BlockId-specific | `BlockId` / `block:` prefix | CRITICAL |
| 3 | Single semantic relation/edge identity | Remove paired endpoint truth | `ConnectionId` pairs / derived edges | CRITICAL |
| 4 | Typed editor-neutral mutation operations | Text/Block/Flow/VT2VT need one reducer | block actions and adapters | CRITICAL |
| 5 | Semantic root ordering independent of geometry | Layout must not change meaning | root list plus Y/X sort | CRITICAL |
| 6 | Stable WorkflowExpressionId | Nested expression focus/edit/merge | reporter BlockId/path | HIGH |
| 7 | Stable WorkflowBranchId | Branch insertion/reorder/merge | owner/role/index/slot string | HIGH |
| 8 | Canonical validation owner | One workflow invariant set | parser/block/flow/runtime validators | HIGH |
| 9 | FOR semantic node | Required V1 construct | none | HIGH |
| 10 | BREAK semantic node | Required V1 construct | none | HIGH |
| 11 | CONTINUE semantic node | Required V1 construct | none | HIGH |
| 12 | Function identity/body | Required V1 construct | generic Flow kinds only | HIGH |
| 13 | Function parameters | Required function semantics | none | HIGH |
| 14 | RETURN semantic node | Required function semantics | none | HIGH |
| 15 | Lossless unsupported/legacy envelope | Forward compatibility and migrations | diagnostics/fallback log/unknown node | HIGH |
| 16 | Project snapshot with presentation attachments | Preserve optional editor views without mixing them into intent | fragmented JSON/preferences | HIGH |

## 19. Editor Removal Risk

| Scenario | Load | Save | Execute | Semantic completeness | Missing editor presentation | Broken features | Verdict |
|---|---|---|---|---|---|---|---|
| FlowEditor absent | Conceptually yes from workspace JSON | Conceptually yes | yes | Core meaning can survive | FlowView unavailable; current saved view handling already incomplete | App currently fails compilation due hard Flow imports; no Flow editing/selection/runtime projection | TEILWEISE |
| BlockEditor absent | No with current modules removed | No | No | Canonical model, serializer, importer, IR and runtime disappear | BlockView unavailable | Almost all workflow paths | NEIN |
| TextEditor absent | yes | yes | yes | Existing workflow meaning survives | Manual drafts/files unavailable | No source authoring/import | JA semantically, TEILWEISE product feature |

For the required optional FlowEditor acceptance test, the blocker is mainly static/app coupling, not ownership of canonical intent. For BlockEditor, the blocker is canonical ownership itself.

## 20. VT2VT Readiness

Verdict for typed workspace operations: **BLOCKED**.

Current VT2VT transport is a robust whole-snapshot mirror: it sends compressed `WorkspaceSerializer` JSON plus checksum, revision, mutation source, and generated EMScript (`Vt2VtPayloads.kt:54-84`). It does not provide editor-neutral operations.

`WorkspaceAction` is BlockEditor-specific and mixes semantic (`Connect`, `UpdateField`, variable actions) with presentation (`MoveRoot`, `Collapse`, `Expand`). `WorkflowDocumentAction` contains only selection, reorder, and remove variants and is not a complete workflow reducer contract.

Blockers:

1. no neutral WorkflowNode/Edge/Expression/Branch IDs;
2. no single-edge relation model;
3. no editor-neutral field/argument addressing;
4. no branch/function operations;
5. no canonical conflict/revision semantics independent of serialized presentation;
6. no lossless unknown operation/node envelope.

Snapshot mirroring is **PARTIAL transport capability**, but typed semantic operation readiness is **BLOCKED**.

## 21. Recommended M2 Slices

### M2-1: Extract semantic ownership from the optional BlockEditor module

**Goal:** remove the most critical ownership leak without rewriting every editor.

**Truth affected:** workflow document identity, nodes, variables, ordered roots, semantic graph access, serializer/validator entry points.

**Minimal change:** introduce a shared `workflow-core` boundary and move/alias the current semantic document contracts behind it. App persistence, runtime, and `WorkspaceWorkflowState` must import shared core, not `blockeditor-domain`. BlockEditor becomes an adapter/client. Preserve behavior and file compatibility in this slice; do not yet redesign every relation.

**Acceptance criterion:** project load/save/runtime and text Apply compile and pass while the BlockEditor UI/Compose modules are absent; app core has no dependency on BlockEditor UI. A dependency test prevents canonical contracts from moving back.

### M2-2: Canonical single relations and stable neutral IDs

**Goal:** eliminate the three critical paired-connection truths.

**Minimal change:** add neutral node/relation IDs and one semantic relation record; Block connection endpoints and Flow edges become projections/adapters.

**Acceptance criterion:** one relation mutation deterministically projects to Block and Flow; reciprocal endpoint corruption is no longer representable in canonical state.

### M2-3: Stable expression, branch, and source identity

**Goal:** make text Apply, selection, undo, and branch insertion identity-safe.

**Minimal change:** canonical `ExpressionId`, `BranchId`, and source-map attachment; import reconciliation uses explicit identity where available and a documented migration fallback otherwise.

**Acceptance criterion:** inserting/reordering duplicate sibling commands or ELSEIF branches preserves unaffected IDs and synchronized selection.

### M2-4: Consolidate semantic reducers and validation

**Goal:** remove duplicated Block/Flow mutator semantics.

**Minimal change:** define typed workflow operations and one workflow validator/reducer; adapt Block and Flow actions to those operations.

**Acceptance criterion:** Add/Remove/Connect/Disconnect/SetArgument/AddBranch/RemoveBranch/MoveStatement have editor-independent tests and produce equivalent projections.

### M2-5: Complete V1 control/function model and extension envelope

**Goal:** represent FOR/BREAK/CONTINUE/FUNCTION/parameters/RETURN and unknown future constructs losslessly.

**Minimal change:** extend shared workflow core after IDs/operations are stable; keep parser/runtime adapters explicit.

**Acceptance criterion:** parse, save, reload, project, generate, and execute supported V1 fixtures without degradation; unsupported extension nodes roundtrip without semantic loss.

### M2-6: Separate all presentation attachments and project snapshot

**Goal:** prevent layout from influencing semantics while preserving every editor view.

**Minimal change:** move Block root positions/collapse and Flow view into versioned view documents attached by workflow ID/revision; define one project snapshot manifest.

**Acceptance criterion:** moving/collapsing/auto-arranging never changes workflow revision or execution order; removing an editor preserves its opaque view attachment.

### M2-7: Typed VT2VT operations

**Goal:** replace whole-snapshot-only collaboration with semantic operations while retaining snapshot recovery.

**Minimal change:** serialize the M2-4 operation contract with revision/precondition and unknown-operation handling.

**Acceptance criterion:** the required operation set can be replayed independently of Text/Block/Flow UI and converges to the same canonical graph.

## Audit Validation and Final Verdict

Evidence was obtained by static source/dependency tracing and existing focused tests. No production source, test, Gradle file, parser, serializer, IR, editor, runtime, or data was modified. No build was run because M2-0 permits documentation-only validation.

### Evidence confidence

- Current authority chain: **VERIFIED BY CODE**.
- Covered parser/workspace/IR/Flow roundtrips: **VERIFIED BY TEST** from existing tests; tests were inspected, not rerun for this documentation-only audit.
- FlowView preference appears save-only: **INFERRED** from repository-wide reference search.
- Behavior of unknown future constructs outside current registries: **UNKNOWN** beyond existing diagnostics/fallback paths.

### Direct answers

1. **Workflow truth today:** BlockEditor-owned `WorkspaceDocument` and its serialized JSON.
2. **WorkflowGraph canonical truth:** **NEIN**.
3. **CORE_SEMANTIC facts:** **25**.
4. **DUPLICATED_TRUTH cases:** **9**.
5. **MISSING_CONTRACT cases:** **16**.
6. **Most critical ownership leak:** persistence, runtime, importer, IR, and orchestration depend on the optional BlockEditor domain/registry/serializer.
7. **Most critical identity issue:** text AST/expressions/branches lack stable neutral IDs; path/type reconciliation can reassign identity after structural edits.
8. **Most critical connection issue:** one semantic connection is persisted twice as reciprocal Block endpoints, but complete reciprocity is not a canonical invariant.
9. **FlowEditor removable:** **TEILWEISE**; semantics survive in principle, current build does not.
10. **BlockEditor removable:** **NEIN**.
11. **Runtime editor-independent:** **TEILWEISE**; UI-independent, not BlockEditor model/module-independent.
12. **VT2VT readiness:** **BLOCKED** for typed operations; whole-snapshot mirroring exists.
13. **Recommended M2-1:** extract canonical semantic ownership behind a shared `workflow-core` boundary while preserving current behavior and file compatibility.
14. **Documentation:** this file.
15. **Root Git status:** `main` is four commits ahead of `origin/main`; pre-existing ShapeMaker submodule WIP and untracked `tmp_device_live.png` / `tmp_factory_check.png` remain untouched; this audit adds only this untracked report.
16. **Submodule status:** BlockEditor is clean and four commits ahead on `codex/blockeditor-compact-badge-bounds`; FlowEditor is clean on `codex/flowchart-visual-semantics`; ShapeMaker contains the pre-existing uncommitted WIP listed by its own status.
