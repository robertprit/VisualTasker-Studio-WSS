# M4-0 Three-Editor Conformance Harness

## Scope

M4-0 adds one deterministic test harness for the currently supported EMScript subset. It introduces
no workflow model, parser feature, runtime behavior, provider behavior, or editor UI. The harness is
located in:

- `app/src/test/java/com/visualtasker/wss/workspace/conformance/ThreeEditorConformanceHarness.kt`;
- `app/src/test/java/com/visualtasker/wss/workspace/conformance/ThreeEditorConformanceTest.kt`.

## Production Paths Under Test

| Projection or mutation | Production path | M4-0 classification |
| --- | --- | --- |
| Text to canonical workspace | `EmscriptApplyGuard` -> `EmscriptWorkspaceImporter` -> `WorkspaceIdentityReconciler` -> `EmscriptSemanticSourceApply` -> `WorkspaceOperationExecutor` | Semantic mutation |
| Canonical workspace to BlockEditor | `WorkspaceDocument.blocks` compatibility projection rebuilt by the canonical mutation path | Read-only projection |
| Canonical workspace to FlowEditor | `WorkspaceWorkflowState` -> `IrGraphGenerator` -> `IrGraphFlowchartProjector` | Read-only projection |
| Canonical workspace to text | production `EmscriptGenerator` with persistent source anchors | Read-only projection |
| BlockEditor field edit | `WorkspaceReducer` / `WorkspaceAction.UpdateField` -> `CanonicalWorkspaceReducer` | Semantic mutation through compatibility facade |
| FlowEditor field edit | `applyFlowchartWorkspaceMutation` -> `WorkspaceReducer` / `WorkspaceAction.UpdateField` | Semantic mutation through compatibility facade |
| Workspace persistence | production `WorkflowSerializer` | Save/reload |
| FlowView persistence | production `FlowViewJsonCodec` | Save/reload of projection-owned state |

The standalone FlowEditor host remains semantically read-only. WSS FlowEditor commands use the app
facade listed above. Moving BlockEditor and WSS FlowEditor commands from `WorkspaceAction` to public
`WorkspaceOperation` transactions remains M4-1 work.

## Fingerprint Contract

The harness calculates four SHA-256 fingerprints over sorted, explicit structural records:

1. **Semantics:** canonical entities, relation kind/role/order, block type/properties, and variables.
2. **Identity:** entity IDs, relation IDs, relation endpoints, and explicit branch IDs.
3. **Metadata:** unknown block metadata, labels, notes, collapse state, BlockView root positions, and
   FlowView viewport/node/annotation data.
4. **Persistence:** the normalized union of semantics, identities, metadata, document/revision data,
   and persisted FlowView compatibility data.

Map iteration order, object identity, memory addresses, and JSON field order are excluded. Derived
`emscript.source.*` parser coordinates and anchor carrier metadata are excluded from the metadata
fingerprint: their semantic identity is covered by the identity fingerprint, while source line and
column are allowed to change under format-only edits. Fingerprint assertions are paired with direct
list/data-class equality assertions.

## Fixtures

| Fixture | Coverage |
| --- | --- |
| `linear` | Three sequenced statements |
| `nested-expressions` | Nested arithmetic expressions and reporter connection |
| `branches` | IF/ELSEIF/ELSE with stable branch IDs and order |
| `typed-variables` | LET/SET, Number/Bool types, variable reference |
| `metadata-layout` | Unknown metadata, label, note, collapse, BlockView and FlowView layout |
| `unsupported` | FOR construct rejected with deterministic parse/import diagnostic |

Format-only, valid transaction, rollback, save/reload, BlockEditor mutation, FlowEditor mutation, and
three-run determinism reuse these fixtures or dedicated minimal fixtures.

## Acceptance

| AC-ID | Status | Test name | Fixture | Production path | Evidence |
| --- | --- | --- | --- | --- | --- |
| AC-01 | PASS | all `ThreeEditorConformanceTest` tests | all | Real guard/importer, workflow state, projectors, generators and codecs | Harness has no production mocks or alternate workflow model. |
| AC-02 | PASS | `linearWorkflowSurvivesTextBlockFlowTextRoundtrip`, `nestedExpressionsAndReporterConnectionsSurviveRoundtrip`, `ifElseIfElseKeepsBranchIdentityAndOrder`, `typedVariablesAndPropertiesSurviveRoundtrip` | linear, nested, branches, typed variables | Text -> canonical -> Block/Flow -> generated anchored text -> source apply | Semantic fingerprints and normalized semantic structures are equal. |
| AC-03 | PASS | same four roundtrip tests | supported fixtures | Source anchors plus identity reconciliation | Entity, relation, and branch identity structures are equal. |
| AC-04 | PASS | `metadataAndProjectionLayoutsSurviveSourceAndPersistenceRoundtrip` | metadata-layout | Source apply, `WorkflowSerializer`, `FlowViewJsonCodec` | Unknown metadata, labels, notes, collapse and layouts compare directly and by fingerprint. |
| AC-05 | PASS | `formatOnlyChangeDoesNotMutateSemanticsIdentityOrRevision`, `readOnlyEditorProjectionsAndSaveReloadDoNotAdvanceRevision` | format-only, read-only | Source apply, editor projection, serializer | Zero format operations; revision remains unchanged. |
| AC-06 | PASS | `validTransactionIsAtomicAndAdvancesRevisionOnce` | valid transaction | `WorkspaceOperationExecutor` with two operations | Both properties publish together and revision advances exactly once. |
| AC-07 | PASS | `invalidTransactionRollsBackDocumentIdentityMetadataAndRevision` | rollback | `WorkspaceOperationExecutor` | Missing entity is diagnosed; complete document and all fingerprints remain unchanged. |
| AC-08 | PASS | `unsupportedLanguageConstructIsRejectedDeterministically` | unsupported FOR | Production parser/import guard | Three attempts return one deterministic failure; no document is published. |
| AC-09 | PASS | `blockEditorWorkspaceActionMutatesCanonicalWorkspaceThroughProductionFacade`, `flowEditorWorkspaceActionFacadeMutatesAndReprojectsCanonicalWorkspace` | editor mutations | Existing Block and WSS Flow facades | Canonical revision/property changes and Flow reprojection are asserted. |
| AC-10 | PASS | `metadataAndProjectionLayoutsSurviveSourceAndPersistenceRoundtrip`, `readOnlyEditorProjectionsAndSaveReloadDoNotAdvanceRevision` | metadata-layout | Production workspace and FlowView codecs | Direct document/view equality and persistence fingerprint equality. |
| AC-11 | PASS | `threeConsecutiveRunsProduceIdenticalFingerprintsAndDiagnostics` | deterministic anchored fixture | Three independent applies against the same canonical baseline | Fingerprints, transactions, decisions, and zero-operation result are identical. |
| AC-12 | PASS | complete module and app gates | repository | Gradle test/build plus diff checks | BlockEditor, FlowEditor, app unit tests, `assembleDebug`, and diff checks pass. |

## Result

M4-0 is **PASS**: 13 conformance tests executed, 13 passed, 0 failed. The test class is also executed
three times as an external determinism gate in addition to its internal three-run assertion.

Known GAPs carried forward:

1. **M4-1:** BlockEditor and WSS FlowEditor semantic commands still enter through `WorkspaceAction`
   compatibility facades, although both reach the single canonical reducer.
2. **M4-1/M4-2:** a central cross-editor Insert/Delete/Reorder/Detach/Reconnect and Undo/Redo matrix is
   still missing; existing module tests cover these paths separately.
3. **Later M4:** opaque arbitrary EMScript fragments, FOR/functions, and TRY/CATCH/FINALLY/THROW are
   not supported. Unsupported input is rejected without publication.

Baseline commits: main `f5e6ce3`, BlockEditor `a5790ad`, FlowEditor `008b136`.
