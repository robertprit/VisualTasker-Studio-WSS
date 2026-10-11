# M5-7 Native Accessibility Target Resolution

## Scope And Ownership

M5-7 adds semantic Android target resolution to the existing execution chain:

`WorkflowGraph -> ExecutionPlan -> NativeAccessibilityProvider -> AccessibilityService -> ExecutionTrace`

`AccessibilityTargetResolver` owns pure query matching. `VisualTaskerAccessibilityService` owns
short-lived Android nodes and native `ACTION_CLICK`. `NativeAccessibilityProvider` owns validation,
policy, cancellation, controlled re-resolution and trace projection. The existing
`NativeGestureProvider` remains the only coordinate/path engine and is delegated to for existing
A11Y gesture commands.

`AccessibilityNodeInfo` never enters the workspace, recorder, execution plan or trace. Persistable
references contain only node path, package/class/view ID, pixel bounds, window ID and display ID.

## Typed Contract

- `ElementQuery`: immutable search intent with text, description, view ID, class, package, bounds,
  state predicates and display ID.
- `ObservedAccessibilityElement`: immutable observation from one captured tree.
- `ElementRef`: snapshot-bound reference without an Android object.
- `ElementMatch`: observed element plus the independently selected actionable element.
- `TargetResolution`: status, deterministic candidate count/order and optional unique match.
- `TargetResolutionPolicy`: clickable-parent and coordinate-fallback policy for one display.

Resolution is pre-order deterministic. Zero candidates is `NOT_FOUND`; multiple distinct action
targets is `AMBIGUOUS`; a unique passive target without permitted clickable ancestor is
`NOT_ACTIONABLE`. No branch selects the first ambiguous candidate.

## Action And Stale Policy

For the existing `action.clickText` catalog command the provider:

1. captures the current default-display tree;
2. resolves an exact, case-insensitive text/content-description query;
3. checks cancellation;
4. captures and resolves again;
5. executes native `ACTION_CLICK` on the current target;
6. polls a short, bounded and cancellation-aware post-action observation window and records
   `CHANGED`, `UNCHANGED` or `UNAVAILABLE`.

A disappearing or no longer unique target becomes `STALE`. A unique replacement matching the same
query is accepted by controlled re-resolution. A successful Android `performAction` is recorded as
accepted, but an unchanged observation is not relabelled as confirmed domain success.

Coordinate fallback is disabled in the productive policy. The provider supports it only through an
explicit policy and then delegates to `NativeGestureProvider`; trace records the fallback and its
cause. OCR, OpenCV and YOLO are not fallback sources in this slice.

## Display Contract

All target queries, references, snapshots, policies and traces carry an integer display ID and use
physical screen pixels. Compose dp never enters this contract. The current Android gateway supports
only `Display.DEFAULT_DISPLAY`; other IDs fail as `DISPLAY_UNSUPPORTED` and are never silently
redirected. VirtualDisplay creation remains outside M5-7.

## Recorder And Editor Integration

Recorder `CandidateTarget.A11y` converts through explicit adapters to the shared `ElementRef` and
`ElementQuery` bounds contract. Conversion requires an explicit display ID and does not publish a
recording target directly into a workflow.

The existing literal `click("text")` source maps to `action.clickText` and roundtrips through Text,
Block, Flow, save/reload and source apply with stable identities. The current V1 importer explicitly
rejects `click(variable)` with `click erwartet Text-String`; M5-7 does not expand that language
contract. `clickElement` is likewise absent from the approved catalog and remains a documented GAP,
not hidden syntax.

## Trace And Diagnostics

`TargetResolutionTraceInfo` adds query kind, candidate count, resolution status, action kind,
fallback flag, display ID and observation status to provider events and RailTrace properties. Raw
text and content descriptions are deliberately excluded.

Principal diagnostics include `ACCESSIBILITY_TARGET_NOT_FOUND`,
`ACCESSIBILITY_TARGET_AMBIGUOUS`, `ACCESSIBILITY_TARGET_STALE`,
`ACCESSIBILITY_TARGET_NOT_ACTIONABLE`, `ACCESSIBILITY_PERMISSION_DENIED`,
`ACCESSIBILITY_SERVICE_UNAVAILABLE`, `ACCESSIBILITY_DISPLAY_UNSUPPORTED` and
`ACCESSIBILITY_COORDINATE_FALLBACK_FORBIDDEN`.

## Acceptance

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 existing infrastructure reused | PASS | Existing service, runner, provider registry, gesture provider and RailTrace projection. |
| AC-02 typed target contract | PASS | `targeting/AccessibilityTargetContracts.kt`. |
| AC-03 deterministic resolution | PASS | Pure resolver and repeated-run tests. |
| AC-04 ambiguity diagnosed | PASS | Multiple action targets produce `AMBIGUOUS`; no first-match behavior. |
| AC-05 stale handling | PASS | Double capture, controlled re-resolution and stale provider tests. |
| AC-06 native actions | PASS | `ACTION_CLICK` is preferred and independently observed. |
| AC-07 explicit fallback only | PASS | Production fallback disabled; allowed/forbidden tests. |
| AC-08 display context | PASS | Explicit IDs and fail-closed non-default policy. |
| AC-09 EMScript/runtime | PASS | Existing `action.clickText` runs through planner/runner/provider. |
| AC-10 three-editor roundtrip | PASS | Literal target conformance plus save/reload test. |
| AC-11 trace/cancellation | PASS | Structured target trace and before/during-resolution cancellation tests. |
| AC-12 real device and regression gates | PASS | SM-S918B: unique target, clickable parent and delayed scene change completed through native `ACTION_CLICK`; duplicate targets failed without action. RailTrace showed `FOUND`/`AMBIGUOUS`, candidate count, display 0, action, fallback=false and `CHANGED`. Full app/module gates and APK install/start passed. |

## Explicit Gaps

- `clickElement` has no approved catalog command or source syntax.
- Variable-bound `click` text is rejected by the current V1 importer.
- Productive execution currently supports only the default display.
- Post-action observation is deliberately bounded; changes after the observation window are reported
  as `UNCHANGED`, not inferred later from unrelated events.
- Permission-denied detection is represented by the provider contract, while a disconnected Android
  service is reported as service unavailable; consolidated permission discovery belongs to M5-8.
