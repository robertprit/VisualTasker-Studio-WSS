# M7-1 Recorder-to-Workflow Publishing

## Status

M7-1 implements the controlled path from reviewed recorder evidence into the canonical workflow:

`RawRecord -> StepCandidateDocument -> Review -> ReviewedStepDocument -> RecorderPublishPlan -> WorkspaceTransaction -> WorkflowGraph`

Recorder documents remain immutable evidence. Publishing only mutates the active `WorkspaceDocument`, through the existing semantic source-apply planner and operation executor.

## Ownership

- Recorder persistence owns raw interactions, captures, evidence and review decisions.
- `ReviewedStepDocument` owns the accepted or corrected recorder interpretation.
- `RecorderWorkflowPublisher` owns mapping, preview, confirmation and idempotence checks.
- `EmscriptSemanticSourceApply` owns the source-to-operation plan.
- `WorkspaceOperationExecutor` owns atomic mutation and validation.
- `WorkspaceShellHistoryController` owns shell undo and redo.
- `WorkflowGraph` remains the only canonical workflow truth.

No recorder-specific block model, mutation engine or editor document was added.

## Typed Contract

`RecorderPublishRequest` identifies recorder document and revision, target workspace and revision, target workflow/root, partial-publish policy and optional explicit confirmation.

`RecorderPublishPreview` contains the immutable preview token, diagnostics and an optional `RecorderPublishPlan`. The plan exposes mappings, capabilities, fallback count, omitted steps, operation counts, target document and runtime capability report.

`RecorderPublishResult` is `Success`, `AlreadyPublished` or `Failure`. Failure never publishes a partial document.

## Mapping

| Reviewed action | Existing EMScript contract | Rule |
| --- | --- | --- |
| semantic tap | `click(text)` | Requires a unique A11y label or an explicit correction. |
| coordinate tap | `clickPoint(x,y,1)` | Only after the coordinate target was explicitly reviewed; emits a warning. |
| swipe | `swipe(pointerPath(...))` | Preserves all recorded points, duration and pointer ID. |
| wait | `wait(duration)` | Duration must be non-negative. |
| screenshot | `screenshot(asset)` | Requires a durable asset reference; a missing file is a runtime warning. |

Rejected and deferred steps are omitted. Unresolved steps block by default. Explicit `allowPartial` permits omission and is visible as a warning. Unsupported, ambiguous and incomplete mappings block publication.

## Identity And Anchors

New recorder statement IDs are deterministic from recorder-document and candidate identity. Relations touching a published statement receive deterministic structural IDs. IDs do not depend on source lines, pixel positions, render order or temporary view objects.

M3 source anchors are strict in populated workspaces and may only refer to existing entities. Therefore the publisher first validates unanchored additions, replaces only the new statement/relation IDs in the in-memory target, and asks the existing source-apply planner to build the final transaction. After publication the normal EMScript generator emits the canonical `@source.*` anchors. Repeated preview is stable; repeated publication is detected through persisted provenance.

## Preview And Confirmation

The existing Scene Inspector review rail contains the publication controls. `Vorschau erstellen` performs mapping, parse/import, reconciliation, validation, transaction planning and runtime capability inspection without changing the workspace. `Veroeffentlichung bestaetigen` is enabled only for a publishable preview.

Confirmation contains the exact preview token. A changed request, recorder revision, workspace revision or canonical baseline rejects apply. Ending a recording never publishes automatically.

## Atomic Apply And History

The final plan is executed once through `EmscriptSemanticSourceApply.execute`, which delegates to the existing staged-payload `WorkspaceOperationExecutor`. The shell then accepts the serialized result through its normal workspace-change boundary, creating the existing undo/redo history entry and refreshing all projections.

The preview rejects destructive plans, multi-root ambiguity, missing roots and partial identity collisions. Apply rechecks the workspace baseline. No half-published state is exposed.

## Provenance

Published blocks retain:

- recorder document ID and source revision;
- candidate and reviewed-step ID;
- review status and decision ID;
- mapping version;
- durable frame and evidence references when available.

Image bytes are never copied into the workflow graph. Provenance survives serializer save/reload and does not make runtime execution depend on the recorder database.

## Three Editors And Runtime

The accepted canonical document is projected by the existing shell state into EMScript, BlockEditor and FlowEditor. No recorder-only projection path exists. Generated source anchors preserve the published statement identity across reapply.

Preview separately reports structural validity and current provider readiness through `RuntimeCapabilityGate`. Publication can succeed while live execution is unavailable; this is reported rather than hidden.

## Limits

- M7-1 publishes into the existing single workflow/root only.
- There is no project explorer or multi-root selection.
- Partial publication requires an explicit request; the current UI defaults to all reviewed steps.
- OCR, OpenCV, YOLO, VirtualDisplay, VT2VT and Worldview fusion are unchanged.
- Full interactive device acceptance requires explicit permission for taps and screenshots.

## Verification

`RecorderWorkflowPublisherTest` covers mappings, preview, confirmation, conflicts, atomicity, idempotence, provenance, save/reload, shell undo/redo, three-editor projection, productive execution-plan generation and runtime capability reporting. Existing candidate, review, Room migration, workspace, runtime and conformance suites remain the release gates.

## Acceptance Status

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 Existing recorder pipeline reused | PASS | Publish starts from `StepCandidateDocument` and `ReviewedStepDocument`; raw recording is not mutated. |
| AC-02 Typed publish contract | PASS | Request, plan, mapping, diagnostic, preview and result contracts are explicit. |
| AC-03 Deterministic step mapping | PASS | Existing click, clickPoint, swipe/pointerPath, wait and screenshot commands are mapped and tested. |
| AC-04 Preview before mutation | PASS | Preview executes planning and validation in memory; mutation tests assert an unchanged baseline. |
| AC-05 Explicit confirmation | PASS | Apply requires the exact preview token and unchanged request. |
| AC-06 Atomic workspace apply | PASS | Existing `EmscriptSemanticSourceApply` and `WorkspaceTransaction` execute the complete plan once. |
| AC-07 Undo/redo | PASS | The published document is recorded and restored through `WorkspaceShellHistoryController`. |
| AC-08 Three-editor roundtrip | PASS | Text, block and flow projections plus source-anchor reapply are covered. |
| AC-09 Save/reload and provenance | PASS | Serializer and Android Room reopen tests preserve identity, target data and provenance. |
| AC-10 Runtime compatibility | PASS | Productive `WorkflowExecutionPlanner` accepts the published workflow; provider readiness remains separately reported. |
| AC-11 Idempotence and errors | PASS | Duplicate, stale revision, ambiguity, missing parameter/asset and atomic rejection cases are covered. |
| AC-12 Regression and device acceptance | GAP | Automated unit, module, build and Android persistence gates pass. The instructed interactive recording/publish/run inspection was not performed without ADB tap/screenshot permission. |
