# M5-3 Native Gesture Provider

## Scope And Existing Contract

M5-3 extends the M5-2 touch path without adding another runtime or Android gesture engine:

`WorkflowGraph -> WorkflowExecutionPlanner -> DeterministicExecutionRunner -> ExecutionProviderRegistry -> NativeGestureProvider -> VisualTaskerAccessibilityService -> ExecutionTrace`

The productive catalog currently exposes `swipe(points: ANY, repeat?: ANY = 1)`. Existing scripts and
fixtures use `swipe([x1, y1, x2, y2, ...], repeat)`. The legacy live runtime executes the complete
ordered point list as one Android path with a fixed duration of 250 ms. The current catalog has no
duration, speed, delay, hold or segment-timing parameter.

The language specification already distinguishes timeless geometric `Path` values from timed
`PointerPath` values. That distinction remains intact. M5-3 does not pretend that the raw catalog
payload is a typed `Path` or `PointerPath` and introduces no new source syntax.

## Supported Native Variants

- `clickPoint(x, y)` remains one absolute-pixel tap.
- `swipe([x1, y1, x2, y2], repeat)` dispatches one straight path per repetition.
- `swipe([x1, y1, x2, y2, ...], repeat)` dispatches one continuous polyline through every waypoint.
- Repetitions execute sequentially inside one provider-owned gesture sequence.
- Every coordinate is finite and inside the current half-open physical display bounds.
- The point list must contain at least two complete coordinate pairs.

`WorkflowExecutionPlanner` preserves the two existing raw catalog arguments as runtime values.
`NativeGestureProvider` also accepts an internal numeric `ExecutionValue.ListValue`, so future typed
planning can converge without changing the Android gateway.

## Deliberate Gaps

- The fixed 250 ms swipe duration is inherited from the existing live implementation. Explicit
  duration or speed cannot be implemented without changing the catalog/language contract.
- Hold, delay and per-segment timing are absent from the current executable catalog.
- Typed `Path`, timed `PointerPath`, quadratic/cubic Bezier segments and multi-pointer `MultiPath`
  are not lowered by the M5 planner yet. Supplying textual `path(...)` data to the raw swipe slot is
  rejected with `TOUCH_PATH_INVALID` instead of being flattened.
- `input.touch` remains structural legacy data and is not executed by this provider.

## Android Dispatch Ownership

`VisualTaskerAccessibilityService` remains the sole owner of Android `dispatchGesture`. It accepts
an immutable list of pixel points, creates one Android `Path`, and returns `Accepted`, `Busy` or
`Rejected`. A service-level token protects the dispatch context from concurrent calls, including
calls outside M5. A bounded callback grace period prevents a missing Android callback from holding
that gate forever.

`NativeGestureProvider` owns the runtime sequence gate shared by click and swipe. M5-3 chooses
deterministic rejection, not hidden queuing: a second request receives `TOUCH_GESTURE_BUSY`.

## Cancellation And Callback Contract

- Cancellation before dispatch submits no physical gesture.
- Cancellation after submission returns runtime `CANCELLED`; it does not claim to reverse already
  delivered touch input.
- Android `onCancelled` is also represented as typed provider cancellation. A bounded correlation
  window preserves an almost-simultaneous runtime cancellation message without turning a genuine
  Android cancellation into an unrelated dispatch failure.
- The first Android callback wins. Duplicate and late callbacks cannot change a terminal result.
- A missing callback produces `TOUCH_GESTURE_TIMEOUT`.
- Service loss produces `ACCESSIBILITY_SERVICE_LOST`.
- Changed display bounds produce `TOUCH_DISPLAY_CHANGED`.
- Dispatch exceptions and synchronous rejection remain distinct provider failures.
- Provider gates are released after success, failure, timeout or runtime cancellation.
- Repeated swipes use a bounded 100 ms inter-dispatch interval. This keeps the sequence serialized,
  avoids flooding Android's gesture dispatcher and provides a deterministic cancellation point.

The existing runner emits provider start/finish, capability, provider ID and canonical entity ID.
Gesture execution never writes to `WorkspaceDocument`.

## Provider Diagnostics

| Code | Meaning |
| --- | --- |
| `TOUCH_COMMAND_UNSUPPORTED` | Command is not owned by the native gesture provider. |
| `TOUCH_COORDINATES_REQUIRED` | Coordinate click has no valid finite pair. |
| `TOUCH_PATH_INVALID` | Swipe has fewer than two complete finite point pairs or unsupported path syntax. |
| `TOUCH_REPEAT_INVALID` | Swipe repeat is not a positive whole number. |
| `TOUCH_REPEAT_UNSUPPORTED` | Coordinate click requests more than one click in the M5-2 contract. |
| `TOUCH_COORDINATES_OUT_OF_BOUNDS` | At least one point lies outside physical display bounds. |
| `ACCESSIBILITY_SERVICE_UNAVAILABLE` | WSS AccessibilityService is not connected. |
| `ACCESSIBILITY_GESTURES_UNAVAILABLE` | Connected service cannot perform gestures. |
| `ACCESSIBILITY_SERVICE_LOST` | Service disconnected after gesture submission. |
| `TOUCH_DISPLAY_UNAVAILABLE` | No valid physical display bounds are available. |
| `TOUCH_DISPLAY_CHANGED` | Bounds changed while a gesture was executing. |
| `TOUCH_GESTURE_BUSY` | Provider sequence or service dispatch context is occupied. |
| `TOUCH_DISPATCH_REJECTED` | Android rejected the gesture synchronously. |
| `TOUCH_DISPATCH_FAILED` | Gateway dispatch raised an exception. |
| `TOUCH_GESTURE_TIMEOUT` | No callback arrived before the provider deadline. |
| `TOUCH_AVAILABILITY_FAILED` | Availability could not be read safely. |

## Future Semantic Targets

| Target | Required input | Resolver dependency | Native dispatch result |
| --- | --- | --- | --- |
| `clickElement` | stable element/entity reference plus optional relative target | A11y/worldview resolver | absolute point or actionable diagnostic |
| `clickText` | text selector, match policy, optional scope | A11y text resolver; OCR only in a later provider | element bounds plus relative point |
| `clickImage` | image resource/template and search region | Vision provider | match bounds plus relative point |
| `clickTemplate` | template ID, threshold and search region | Vision/template provider | match bounds plus relative point |
| Relative bounds target | bounds and normalized x/y within bounds | typed geometry contract | validated absolute pixel point |

These are matrix contracts only. M5-3 implements no semantic target search, OCR, OCV or YOLO.

## Verification

JVM coverage includes click, two-point swipe, multi-waypoint polyline, repeat ordering, invalid and
out-of-bounds inputs, provider/service concurrency, cancellation before and during dispatch, late
and duplicate callbacks, Android cancellation, timeout, service loss, display change, retry after
failure, stable trace IDs and unchanged workspace serialization.

The debug-only device gate prepares safe center-screen swipe, polyline and cancellation workflows,
shows an in-app gesture target, and preserves/restores the previous workspace. Android
instrumentation still covers the Android runtime bridge; physical same-package Accessibility
dispatch uses the debug gate because instrumentation force-stops the target package and disconnects
its own AccessibilityService.

The physical gate was executed on an SM-S918B (`R3CW5032EGH`):

- Straight swipe completed with a measured target delta of 324 px.
- The V-shaped three-point path completed with a 324 px net delta and approximately 500 px of
  measured travel, proving that the middle waypoint was traversed rather than flattened.
- Cancellation stopped a 20-repeat sequence after the second accepted gesture and ended in runtime
  state `CANCELLED`.
- The serialized canonical workspace was byte-for-byte unchanged across the completed path and
  cancellation runs (SHA-256 checks before and after each run matched).
- The debug fixture restored the prior workspace after the gate.

Verification gates are green: native provider JVM tests, the full app unit-test suite, BlockEditor
tests, FlowEditor tests, Android bridge instrumentation (2/2), the M4 three-editor conformance
harness in three clean runs, and `:app:assembleDebug`.

## Acceptance

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 existing infrastructure reused | PASS | One M5 runner, registry and AccessibilityService dispatcher. |
| AC-02 native swipe | PASS | Provider/planner tests and physical 324 px center-screen swipe. |
| AC-03 supported path | PASS | Continuous multi-waypoint polyline test plus approximately 500 px measured V-path travel. |
| AC-04 coordinate/timing contract | PASS | Absolute pixels and existing fixed 250 ms catalog behavior; unsupported timing remains explicit below. |
| AC-05 deterministic concurrency | PASS | Shared provider and service gates; busy tests. |
| AC-06 cancellation/callback races | PASS | Before/during/late/duplicate/timeout tests and physical cancellation after 2/20 repeats. |
| AC-07 service loss and timeout | PASS | Dedicated diagnostics and retry tests. |
| AC-08 trace and identity | PASS | Runner E2E test with canonical entity IDs and provider metadata. |
| AC-09 semantic A11y matrix | PASS | Future target table above. |
| AC-10 physical Android swipe | PASS | SM-S918B target reported 324 px horizontal displacement and runtime `COMPLETED`. |
| AC-11 physical path or explicit gap | PASS | SM-S918B traversed the supported three-point polyline; typed/timed paths remain the explicit GAP below. |
| AC-12 regression/build | PASS | App, BlockEditor, FlowEditor, Android bridge instrumentation, M4 conformance x3 and assembleDebug are green. |

The missing typed timing/PointerPath/Bezier lowering is a documented product GAP, not an implicit
success claim for the supported raw polyline capability.
