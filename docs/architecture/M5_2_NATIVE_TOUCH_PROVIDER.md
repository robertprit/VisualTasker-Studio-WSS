# M5-2 Native Touch Provider

## Decision

M5-2 adds the first physical Android action to the existing M5 runtime. The productive path remains:

`WorkspaceDocument -> WorkflowExecutionPlanner -> DeterministicExecutionRunner -> ExecutionProviderRegistry -> NativeTouchProvider -> VisualTaskerAccessibilityService -> ExecutionTrace -> RailTrace`

The language/catalog already defines coordinate clicks as `clickPoint(x, y[, repeat])` with command
ID `input.clickPoint` and capability `A11Y`. M5-2 therefore uses that canonical spelling instead of
introducing a conflicting coordinate overload for the legacy text command `click("text")`.

## Reused Android Infrastructure

`VisualTaskerAccessibilityService` remains the single owner of Android `dispatchGesture`. Its
existing path dispatcher now has a callback form shared by the established suspend click/swipe
entry points and the runtime gateway. No second touch engine, Tasker fallback, Shizuku fallback or
permission bypass is introduced.

The service exposes only the information required by the provider boundary:

- physical display bounds from `DisplayMetrics` in pixels;
- `CAPABILITY_CAN_PERFORM_GESTURES` availability;
- callback-based single-point gesture dispatch.

Coordinates are passed to `Path.moveTo` unchanged. There is no dp conversion, scaling or display
normalization.

## Provider Contract

`NativeTouchProvider` advertises `A11Y` and accepts `input.clickPoint` plus its source alias
`clickPoint`. M5-2 supports one click per invocation. It validates:

1. two finite numeric coordinates and optional repeat value;
2. connected AccessibilityService;
3. gesture capability;
4. valid physical display bounds;
5. coordinates inside the half-open display rectangle;
6. no other native gesture currently in flight.

Failures remain typed `ProviderResult.Failure` diagnostics. Cancellation remains
`ProviderResult.Cancelled`. The provider never mutates the `WorkspaceDocument`.

## Callback And Cancellation Rules

Gesture dispatch runs from the existing `AndroidRuntimeBridge` worker dispatcher. The provider
waits in short bounded intervals for the Android callback; it never blocks the Compose/main thread.

- First completed callback publishes provider success.
- Android cancellation produces `TOUCH_GESTURE_CANCELLED_BY_ANDROID`.
- Dispatch rejection and exceptions produce structured diagnostics.
- Runtime cancellation closes the callback gate and publishes `Cancelled`, even when physical
  delivery may already have occurred.
- Late and duplicate callbacks cannot overwrite the first terminal result.
- A ten-second callback deadline prevents an abandoned Android callback from hanging a run.
- Worker interruption restores the interrupt flag and produces cancellation.
- An atomic in-flight guard prevents uncontrolled parallel gestures.

## Productive Shell Device Gate

The debug build contains a preference-gated test target centered inside the Workspace. It is absent
from ordinary sessions and non-debuggable builds. A debug-only broadcast gate:

1. reveals that safe target;
2. derives its physical center;
3. imports `wait(500) -> clickPoint(x, y) -> wait(500)` into the canonical workspace;
4. starts it through the productive top-bar Play action;
5. requires the target hit count and shell state `COMPLETED`;
6. requires the persisted RailTrace focus to reference a `live-run` step;
7. verifies that serialized workspace JSON did not change;
8. restores all replaced preferences after the test.

The gate runs against the already running app instead of Android instrumentation. Instrumentation
force-stops its target package and therefore disconnects an AccessibilityService owned by that same
package before the test can dispatch a gesture. The debug gate preserves the prior workspace,
prepares the canonical test document without stopping the process and restores the prior state after
verification. A simulated click is not accepted as physical evidence.

## Diagnostics

| Code | Meaning |
| --- | --- |
| `TOUCH_COMMAND_UNSUPPORTED` | Provider received a non-touch command. |
| `TOUCH_COORDINATES_REQUIRED` | Coordinates are absent, non-numeric or non-finite. |
| `TOUCH_REPEAT_UNSUPPORTED` | M5-2 received a repeat count other than one. |
| `ACCESSIBILITY_SERVICE_UNAVAILABLE` | WSS AccessibilityService is not connected. |
| `ACCESSIBILITY_GESTURES_UNAVAILABLE` | Connected service cannot dispatch gestures. |
| `TOUCH_DISPLAY_UNAVAILABLE` | Physical display bounds are unavailable or invalid. |
| `TOUCH_COORDINATES_OUT_OF_BOUNDS` | Target lies outside the physical display. |
| `TOUCH_GESTURE_BUSY` | Another native gesture is still active. |
| `TOUCH_DISPATCH_REJECTED` | Android rejected dispatch synchronously. |
| `TOUCH_DISPATCH_FAILED` | Gateway raised an exception. |
| `TOUCH_GESTURE_CANCELLED_BY_ANDROID` | Android cancelled the accepted gesture. |
| `TOUCH_GESTURE_TIMEOUT` | No terminal callback arrived before the deadline. |

## Acceptance Status

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 Existing infrastructure inventoried | PASS | Service, command catalog, provider registry, metrics and trace path documented above. |
| AC-02 Coordinate click provider | PASS | `NativeTouchProvider` resolves `input.clickPoint`. |
| AC-03 Physical pixel coordinates | PASS | Bounds validation and unchanged `Path.moveTo` coordinates; JVM tests. |
| AC-04 Deterministic callbacks | PASS | First-result gate, timeout and callback tests. |
| AC-05 Cancellation/late callbacks | PASS | Cancellation-before/during/late-callback tests. |
| AC-06 Availability/diagnostics | PASS | Disconnected, missing-capability, invalid-bounds and rejected-dispatch paths. |
| AC-07 Productive Play reaches provider | PASS | Top-bar Play executed the prepared canonical workflow on SM-S918B. |
| AC-08 Valid workflow completes | PASS | Physical target changed from `Touch 0` to `Touch 1`; shell state reached `COMPLETED`. |
| AC-09 RailTrace entity references | PASS | Stable canonical IDs tested; physical run persisted `selectedStepId=live-run-18`. |
| AC-10 No parallel runtime/touch engine | PASS | One existing bridge, explicit registry and shared service dispatcher. |
| AC-11 Regression suites | PASS | Provider suite: 20/20; complete app, BlockEditor, FlowEditor and repeated M4 gates pass. |
| AC-12 Android build/device | PASS | `assembleDebug`, install and cold/warm launch pass; WSS service bound with gesture capability on SM-S918B. |

The physical run also retained the exact serialized workspace hash before and after execution.

## Bounded Limits

- M5-2 implements only one coordinate click. Swipe, paths, multitouch and semantic/vision targeting
  remain outside this slice.
- Android does not guarantee that an already submitted gesture can be physically retracted when a
  runtime cancellation arrives.
- Permission activation stays a human Android Settings action.
- Runtime trace remains process-local under the M5-1 contract.
