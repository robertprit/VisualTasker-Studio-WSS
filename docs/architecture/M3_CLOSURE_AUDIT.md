# M3 Closure Audit

## Scope and Baseline

This audit closes architecture milestone M3 against the local repository state:

- main repository: `855e6bc`;
- BlockEditor repository: `a5790ad`;
- FlowEditor repository: `008b136`.

The audit is evidence-based. A possible future improvement is not a defect unless it violates an
accepted M3 contract. Runtime, providers, UI, and unrelated IME/ShapeMaker work are outside scope.

Status meanings:

- `PASS`: the M3 contract is implemented and covered by direct tests;
- `GAP`: the M3 foundation is valid, but an explicit M4 convergence requirement is incomplete;
- `BLOCKER`: M3 cannot be closed without correction.

## Results

| Area | Status | Implementation evidence | Test evidence | Finding |
| --- | --- | --- | --- | --- |
| WorkspaceOperations and atomic WorkspaceTransactions | PASS | `visualtasker-blockeditor/workflow-core/src/main/kotlin/de/visualtasker/workflow/core/WorkspaceOperation.kt`, `CanonicalWorkspaceMutation.kt` in the same package | `WorkspaceOperationExecutorTest.threeOperationTransactionPublishesOneRevision`, `failedSecondOperationRollsBackWholeTransaction`, `stagedSourcePayloadPublishesOnlyWithSuccessfulTransaction`, `WorkspaceOperationIntegrationTest` | Operations use stable semantic IDs. A transaction invokes the canonical mutator once; rejection returns the original document and publishes no partial state. |
| Semantic Property Schema and type validation | PASS | `visualtasker-blockeditor/workflow-core/src/main/kotlin/de/visualtasker/workflow/core/SemanticPropertySchema.kt`, `visualtasker-blockeditor/workflow-semantics/src/main/kotlin/de/visualtasker/workflow/semantics/SemanticPropertyCatalog.kt` | `WorkspaceOperationExecutorTest.schemaRejectsWrongOwnerTypeAndNullWithoutChangingRevision`, `schemaRejectsReadOnlyStructuralAndConstraintProperties`, `SharedSemanticServicesTest.catalogSchemaValidatesAndAppliesCommandProperty` | Property discovery is deterministic and Compose-free. Scalar types reuse `LanguageTypeRef`; structural properties are rejected rather than coerced. |
| Lossless Semantic Source Apply | PASS | `app/src/main/java/com/visualtasker/wss/emscript/apply/EmscriptSemanticSourceApply.kt`, `EmscriptApplyGuard.kt` in the same package | `EmscriptSemanticSourceApplyTest.formattingOnlyApplyKeepsIdentityRevisionAndProducesNoOperations`, `explicitCommandChangeUsesSetPropertyAndPreservesUnrepresentedPayload`, `invalidApplyLeavesOriginalDocumentUntouched` | Source changes produce one semantic diff and one staged transaction. Missing source properties are preserved, and failure does not replace the canonical workflow. |
| Persistent Source Anchors and stable identities | PASS | `EmscriptSourceAnchorContract.kt`, `WorkspaceSourceAnchorProjection.kt`, `EmscriptSourceAnchorMetadata.kt`, `WorkspaceIdentityReconciler.kt` | `persistentAnchorsDisambiguateDeletionAndInsertionOfIdenticalStatements`, `stalePersistentAnchorFailsInsteadOfGuessing`, `persistentAnchorsSurviveFormattingSaveReloadAndBranchRoundtrip`, `repeatedExpressionsKeepEntityIdentityWhenStatementsAreReordered` | Statement, expression, branch, and relation identities survive supported text edits. Duplicate, stale, orphaned, or contradictory anchors fail diagnostically instead of being guessed. |
| Validator correctness, graph traversal, and rollback | PASS | `visualtasker-blockeditor/workflow-semantics/src/main/kotlin/de/visualtasker/workflow/semantics/validation/Validator.kt`, operation executor in `WorkspaceOperation.kt` | `ValidatorGraphTraversalTest`, complete validator fixtures, transaction rollback tests | Validation uses a per-run connection/adjacency index and linear shared traversals. Existing diagnostic payload/order is retained. At 320 statements median validation is 1.269-1.403 ms. Invalid transactions preserve the input document. |
| Unknown metadata and projection-owned state | PASS | source property mask in `EmscriptWorkspaceImporter`, preservation plan in `EmscriptSemanticSourceApply`, separate `FlowViewDocument` persistence | `explicitCommandChangeUsesSetPropertyAndPreservesUnrepresentedPayload`, `variableSourceChangePreservesUnrepresentedScope`, `WorkspacePersistenceCompatibilityTest`, `WorkspaceWorkflowStateTest.layoutPositionChangeKeepsExecutableProjectionStable`, `WorkspaceSerializerTest.decode_missingPluginDefinitionKeepsDocumentAndReportsDiagnostic` | Retained blocks keep unknown metadata, collapse state, unrepresented scalar fields, and variable scope. Flow layout is persisted separately and does not alter executable semantics. This PASS does not claim lossless arbitrary EMScript source fragments. |
| One canonical mutation path | GAP | `WorkspaceReducer` delegates to `CanonicalWorkspaceReducer`; source apply uses `WorkspaceOperationExecutor`; Flow mutations use `WorkspaceReducer` | `CanonicalWorkspaceMutationTest`, `WorkspaceFlowchartMutationsTest`, `WorkspaceEditorSyncContractTest` | There is one canonical mutation engine and no competing writable graph. However, BlockEditor and FlowEditor still express several semantic edits as `WorkspaceAction` rather than public `WorkspaceOperation`. Direct operation conformance for every editor is an M4 carry-over, not an M3 data-integrity blocker. |

## Canonical Ownership Finding

`WorkspaceDocument.canonical` / `CanonicalWorkspaceDocument` is the semantic authority. Compatibility
blocks and connectors are rebuilt projections used by existing editor code. `WorkspaceWorkflowState`
normalizes one document and derives serialized workspace JSON, IR graph, EMScript, and FlowEditor graph
from it. Neither TextEditor, BlockEditor, nor FlowEditor owns a second semantic workflow.

The phrase "one canonical mutation path" therefore has two levels:

1. **Engine ownership: PASS.** All supported semantic mutations ultimately pass through the canonical
   reducer/mutator and regenerate compatibility state.
2. **Public command convergence: GAP.** Text Apply already plans `WorkspaceOperation` transactions;
   BlockEditor/FlowEditor still expose compatibility `WorkspaceAction` facades for part of their edits.

M4 must close level 2 without introducing another manager or graph.

## Explicit Non-Claims

M3 does not prove support for arbitrary or unknown EMScript syntax. `FOR`, `BREAK`, `CONTINUE`, user
functions, `RETURN`, `TRY/CATCH/FINALLY/THROW`, lists/indexing, and several V1 expression features are
not implemented end to end. Existing formatters, icons, or documentation mentioning a construct are
not conformance evidence.

Likewise, unknown plugin blocks can survive workspace JSON loading with diagnostics, but unknown text
fragments do not yet have a canonical opaque-source representation. M4 must reject unsupported text
without silent loss until such a representation exists.

## Closure Decision

**M3 is CLOSED with one documented M4 carry-over and no blockers.**

The carry-over is direct editor-operation convergence plus a central three-editor conformance suite.
No M3 production repair is justified by this audit.
