# M4 Three-Editor Conformance Matrix

## Status Legend

- **implemented**: production path exists and direct tests cover the stated roundtrip.
- **partially implemented**: useful production support exists, but the complete three-editor contract
  or a required preservation case is not centrally proven.
- **not supported**: the construct is rejected, presentation-only, or absent from at least one required
  semantic projection.

This matrix describes the current repository at `855e6bc` / BlockEditor `a5790ad`. Mention in a
formatter, icon set, palette, or documentation is not implementation evidence.

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
| Format-only text change | No semantic operations | No block identity change | Reprojection only | Revision and IDs unchanged | implemented | `formattingOnlyApplyKeepsIdentityRevisionAndProducesNoOperations`. |
| Insert/Delete | Semantic diff emits create/delete | Reducer supports add/delete | Flow mutation facade supports add/delete | Entity/relation operations | implemented | Source insertion/deletion and Flow mutation tests. |
| Reorder | Anchors disambiguate equal elements | Structural reconnect/order support | Edge/branch operations exist | `SetOrder` | partially implemented | Canonical/source cases are covered; one central Text/Block/Flow reorder matrix is missing. |
| Detach/Reconnect | Represented through source structure | Supported by reducer | Sequence/branch/data edges supported | Remove/CreateRelation | implemented | Workspace operation integration and Flow mutation tests. |
| Undo/Redo | Host snapshot after accepted apply | Snapshot transaction history | Uses host workflow history | Atomic before/after document | partially implemented | Block history and selection restoration are covered; one cross-editor transaction/selection history suite is missing. |
| Atomic transaction and rollback | One guarded source transaction | Canonical reducer transaction boundary | Mutations reach canonical reducer | Original document returned on failure | implemented | Transaction rollback and invalid source apply tests. |
| Unknown block metadata | Missing source fields preserved | Metadata map roundtrips | May ignore but must retain via workspace | Compatibility payload retained | implemented | `custom.owner`, collapse state, missing plugin definition, and serializer tests. |
| Opaque EMScript fragments | No opaque AST/source-fragment model | No lossless placeholder semantics | No lossless placeholder semantics | No opaque entity kind | not supported | Unknown text fails; this is acceptable only because publication is blocked. |
| BlockView layout | Not represented | Root positions/collapse presentation | Read-only input to projection | Outside canonical relations | partially implemented | Root positions persist; a dedicated BlockView document is less explicit than FlowView. |
| FlowView layout/facets/routes | Not represented | Not owner | Separate `FlowViewDocument` | Outside canonical relations | implemented | Persistence compatibility proves separate workflow and FlowView documents. |
| TextView draft/caret/formatting | Owned by TextEditor | Not owner | Not owner | Outside canonical document until Apply | partially implemented | Restart draft gate exists; full lossless text-view persistence is not a semantic M4 requirement. |
| Workspace JSON save/reload | Can regenerate source | Full workspace serialization | Reprojects from restored canonical data | Schema-2 canonical payload | implemented | Serializer and persistence compatibility suites. |
| EMScript repeated roundtrip | Anchored generation/reapply | Rebuilt from same workspace | Rebuilt from IR | IDs and relation IDs retained | implemented | Anchor save/reload and three-editor projection tests. |
| Editor switch without mutation | Draft remains separate | Projection consumer | Projection consumer | Revision must stay fixed | partially implemented | Shared state construction is read-only and tested; no central repeated-switch conformance loop yet. |
| Cross-editor selection | Source line resolves semantic ID | Block selection uses semantic ID | Node selection uses semantic ID | Selection references entity identity | implemented | `WorkspaceEditorSyncContractTest` covers all directions and removed targets. |
| Invalid anchors/conflicts | Typed failure stage/code | Prior projection retained | Prior projection retained | No publication | implemented | Duplicate/orphan/stale anchors and ambiguity tests. |
| Invalid relation/type/graph | Parse/apply diagnostic where applicable | Validator/schema diagnostic | Invalid mutations return unchanged document | Deterministic canonical/validator diagnostics | implemented | Core validator, property, relation, and Flow mutation fixtures. |
| Projection metadata affects runtime | Not allowed | Geometry ignored | Geometry/facets ignored | Runtime reads semantics | implemented | Layout-only position test leaves EMScript and canonical graph unchanged. |
| Direct WorkspaceOperation use by every editor | Source Apply uses operations | Several actions still use `WorkspaceAction` facade | Several actions still use `WorkspaceAction` facade | One canonical mutator underneath | partially implemented | Primary M4 operation-convergence gap; no competing semantic store exists. |

## M4 Priority

1. Build the M4-0 supported-subset conformance harness.
2. Use its fingerprint and preservation assertions while moving remaining Block/Flow semantic commands
   from compatibility `WorkspaceAction` facades to explicit transactions.
3. Add one structural language family at a time only when Text, Block, Flow, persistence, diagnostics,
   and stable identity can land together.
4. Introduce TRY-family structure only in an explicitly approved later M4 slice; defer execution to M5.
