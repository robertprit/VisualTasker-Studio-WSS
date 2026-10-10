# M4 Three-Editor Conformance Matrix

## Status Legend

- **implemented**: production path exists and direct tests cover the stated roundtrip.
- **partially implemented**: useful production support exists, but the complete three-editor contract
  or a required preservation case is not centrally proven.
- **not supported**: the construct is rejected, presentation-only, or absent from at least one required
  semantic projection.

This matrix describes the M4-3 closure implementation based on main `965e972` / BlockEditor `595a8b7` /
FlowEditor `008b136`. Mention in a
formatter, icon set, palette, or documentation is not implementation evidence.

M4-0 executable evidence is documented in `M4_0_THREE_EDITOR_CONFORMANCE.md`. Its central harness
uses the production import, canonical workspace, Block projection, IR/Flow projection, EMScript
generator, source apply, workspace serializer, and FlowView codec.
M4-1 operation-convergence evidence is documented in `M4_1_WORKSPACE_OPERATION_CONVERGENCE.md`.
M4-2 productive mutation and roundtrip evidence is documented in
`M4_2_EDITOR_MUTATION_ROUNDTRIP.md`.
M4-3 shell-history and closure evidence is documented in `M4_3_SHELL_HISTORY_INTEGRATION.md` and
`M4_CLOSURE_AUDIT.md`.

## Matrix

| Dimension | TextEditor | BlockEditor | FlowEditor | Canonical workspace | Current status | Evidence / gap |
| --- | --- | --- | --- | --- | --- | --- |
| Statement identity | Persistent block/entity anchors | Stable block-to-entity mapping | Nodes carry semantic source identity | `SemanticEntityKind.Statement` | implemented | Source-anchor and editor-sync tests retain IDs. |
| Expression identity | Input-path entity anchors | Reporter/output blocks and typed slots | Data-flow/reporter nodes | `SemanticEntityKind.Expression` plus expression relations | implemented | Repeated-expression reorder and expression replacement tests retain IDs. |
| Branch identity | `branch:<slot>` anchors | Statement slots and IF mutator shapes | Branch edges/scopes | Branch entities plus containment | implemented | IF shape changes preserve owner and surviving branch identities. |
| Relation identity/order | Relation directive prelude | Connector projection | Edge projection | Stable `SemanticRelationId`; order separate from ID | implemented | Reorder and serialization fixtures cover relation stability. |
| Basic statement sequence | Parsed/generated | Previous/next stack | Sequence edges | `Sequence/Next` | implemented | Text-to-workspace and Flow mutation tests. |
| IF/ELSEIF/ELSE | Parsed, generated, anchored | Dedicated control blocks and dynamic slots | Branch nodes/edges | Ordered branch entities | implemented | Multiple ELSEIF import, generation, mutation, and roundtrip tests. |
| REPEAT and WHILE | Parsed/imported/generated for supported forms | Control blocks with body slots | Loop nodes/body/exit edges | Loop statement plus branch/body relations | implemented | Integration and nested-flow fixtures. |
| FOR/BREAK/CONTINUE | Parser rejects | No complete semantic family | No canonical projection | No complete contract | not supported | Keywords exist in language metadata/formatter only; must not be treated as conformance. |
| Functions/RETURN | Parser rejects | No function declaration/call contract | Presentation has function-region vocabulary only | No function/scope entities | not supported | Requires separate structural M4 contract. |
| TRY/CATCH/FINALLY/THROW | Parser rejects | No canonical blocks/mutator | Some visual node kinds only | Branch roles absent | not supported | Planned structural contract in `M4_THREE_EDITOR_CONFORMANCE.md`; runtime deferred to M5. |
| Variable declaration and assignment | LET/SET supported in current subset | Variable registry, setter/getter blocks | Variable/data-flow projection | Stable variable entities/references | implemented | LET/SET roundtrip and typed assignment suites. |
| Full V1 type surface | Scalar and selected domain values | Shared compatibility layer | Displays projected types | `LanguageTypeRef` property schema | partially implemented | Lists/indexing, several domain value constructors, and some named/default argument rules remain outside parser semantics. |
| Reporter connections | Reporter expressions and call expressions | Output/value slots | Data-flow/condition edges | Expression relations with roles/order | implemented | Nested reporter, condition, and compatibility tests. |
| Typed Path/PointerPath values | V1 constructors, named timing and list literals roundtrip | Catalog reporters with typed ValueInputs | Typed expression nodes and data-flow ports | Stable expression entities and relations | implemented | M5-5 fixtures cover all segment kinds, duration/segment duration/speed, variable binding, Block/Flow edits, undo/redo and persistence. |
| Format-only text change | No semantic operations | No block identity change | Reprojection only | Revision and IDs unchanged | implemented | `formattingOnlyApplyKeepsIdentityRevisionAndProducesNoOperations`. |
| Insert/Delete | Semantic diff emits create/delete | Action adapter emits create/delete transaction | Flow add/delete facade emits one transaction | Entity/relation operations | implemented | M4-1 core and central Flow compound-add tests. |
| Reorder | Anchors disambiguate equal elements | Explicit detach/connect sequence | Edge/branch operations exist | `SetOrder` or explicit relation operations | implemented | M4-2 productive controller reparent test preserves entities, order, and complete undo/redo state. |
| Detach/Reconnect | Represented through source structure | Adapter emits remove/create relation operations | Sequence/branch/data edges use the same adapter | Remove/CreateRelation | implemented | M4-2 detach/delete and nested reporter reconnect tests plus Flow mutation tests. |
| Undo/Redo | Local draft history plus Source Apply; shared shell snapshots | Controller mutation publishes into shared shell history | Flow mutation publishes into shared shell history | Atomic serialized before/after document plus semantic selection | implemented | M4-3 covers all three origins, editor switch, delete/restore, strict ordering, rejection, and exact fingerprints. |
| Atomic transaction and rollback | One guarded source transaction | Compound actions use `reduceAll` | Add-with-fields/connect and occupied-port replacement use `reduceAll` | Executor owns publication and rollback | implemented | M4-1 verifies one revision and complete document/fingerprint rollback. |
| Unknown block metadata | Missing source fields preserved | Metadata map roundtrips | May ignore but must retain via workspace | Compatibility payload retained | implemented | `custom.owner`, collapse state, missing plugin definition, and serializer tests. |
| Opaque EMScript fragments | No opaque AST/source-fragment model | No lossless placeholder semantics | No lossless placeholder semantics | No opaque entity kind | not supported | Unknown text fails; this is acceptable only because publication is blocked. |
| BlockView layout | Not represented | Root positions/collapse presentation | Read-only input to projection | Outside canonical relations | partially implemented | Root positions persist; a dedicated BlockView document is less explicit than FlowView. |
| FlowView layout/facets/routes | Not represented | Not owner | Separate `FlowViewDocument` | Outside canonical relations | implemented | Persistence compatibility proves separate workflow and FlowView documents. |
| TextView draft/caret/formatting | Owned by TextEditor | Not owner | Not owner | Outside canonical document until Apply | partially implemented | Restart draft gate exists; full lossless text-view persistence is not a semantic M4 requirement. |
| Workspace JSON save/reload | Can regenerate source | Full workspace serialization | Reprojects from restored canonical data | Schema-2 canonical payload | implemented | M4-0 directly compares workspace and FlowView structures plus persistence fingerprints after save/reload. |
| EMScript repeated roundtrip | Anchored generation/reapply | Rebuilt from same workspace | Rebuilt from IR | IDs and relation IDs retained | implemented | M4-0 central supported-subset fixtures compare semantic and identity fingerprints plus structures. |
| Editor switch without mutation | Draft remains separate | Projection consumer | Projection consumer | Revision must stay fixed | implemented | M4-0 repeats production Block/Flow/text projection and save/reload without canonical revision change. |
| Cross-editor selection | Source line resolves semantic ID | Block selection uses semantic ID | Node selection uses semantic ID | Selection references entity identity | implemented | `WorkspaceEditorSyncContractTest` covers all directions and removed targets. |
| Invalid anchors/conflicts | Typed failure stage/code | Prior projection retained | Prior projection retained | No publication | implemented | Duplicate/orphan/stale anchors and ambiguity tests. |
| Invalid relation/type/graph | Parse/apply diagnostic where applicable | Validator/schema diagnostic | Invalid mutations return unchanged document | Deterministic canonical/validator diagnostics | implemented | Core validator, property, relation, and Flow mutation fixtures. |
| Projection metadata affects runtime | Not allowed | Geometry ignored | Geometry/facets ignored | Runtime reads semantics | implemented | Layout-only position test leaves EMScript and canonical graph unchanged. |
| Direct WorkspaceOperation use by every editor | Source Apply emits operations | `WorkspaceAction` is a delegating operation-plan adapter | Flow facade delegates to the same adapter/transactions | One executor publishes semantic changes | implemented | M4-1 core and 23 central conformance tests; compatibility API remains intentionally. |

## M4 Priority

1. M4 three-editor architecture is closed; do not start M5 without explicit approval.
2. Keep process-restart history persistence and opaque unsupported source as documented product gaps.
3. Add one structural language family at a time only when Text, Block, Flow, persistence, diagnostics,
   and stable identity can land together.
4. Introduce TRY-family structure only in an explicitly approved later M4 slice; defer execution to M5.
