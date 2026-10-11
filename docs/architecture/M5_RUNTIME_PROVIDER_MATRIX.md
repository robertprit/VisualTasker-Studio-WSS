# M5 Runtime Provider Matrix

## Contract

Capability describes required behavior. Provider describes a replaceable implementation.
`ExecutionContext` carries request-local state and cancellation. The runtime core knows none of the
Android service classes behind a provider.

| Capability family | Catalog/source today | M5-0 provider | Product provider status | Core dependency |
| --- | --- | --- | --- | --- |
| Timing `wait(duration)` | `action.wait`, capability `TIMING` | Deterministic fake provider | `AndroidWaitExecutionProvider` live in M5-1 | None |
| Core command invocation | `VisualTaskerCommandCatalog` | Deterministic fake provider | Not registered in M5-1 | None |
| Logging/feedback | Core catalog and existing BasicRun environment | Fake provider | Existing Android behavior, not migrated | None |
| Coordinate click `clickPoint(x,y)` | `input.clickPoint`, capability `A11Y` | Fake provider | `NativeGestureProvider` live since M5-2 | None |
| Swipe/polyline path | `action.swipe`, raw point list plus repeat, capability `A11Y` | Fake provider | `NativeGestureProvider` live in M5-3; absolute pixels, fixed legacy 250 ms duration | None |
| Typed `Path` / timed `PointerPath` | V1 catalog-backed value constructors plus existing `action.swipe` | Typed value construction, variable binding and deterministic lowering | `NativeGestureProvider` live; M5-5 adds LET/SET, dynamic geometry/timing, editor roundtrip and structured trace | None |
| MultiPath/multitouch | V1 `multiPath(...)` value plus `multiSwipe(...)` action | Typed construction, variable binding and deterministic lowering | `NativeGestureProvider` live in M5-6; synchronized line/Bezier pointers, delay/holds, common cancellation and bounded trace | None |
| Semantic `clickElement` | No approved catalog command; typed `ElementRef` contract exists | Not implemented | M5-7 contract only; syntax/runtime binding remains a GAP | None |
| Semantic `clickText` | Existing `action.clickText` / `click("text")` | Deterministic query and trace contract | `NativeAccessibilityProvider` live in M5-7; native `ACTION_CLICK`, stale validation, ambiguity rejection | None |
| Semantic `clickImage` / `clickTemplate` | Planned image/template target and search region | Not implemented | Contract only; requires Vision provider | None |
| Clipboard/file/cache/system | Core catalog | Fake provider | Existing BasicRun environment later | None |
| Tasker | Tasker catalog/plugin contracts | Discovery through existing registration | Optional provider PARTIAL; execution result/callback remains M9 | None |
| Shizuku | Shell capability metadata | Discovery through existing registration | Optional provider PARTIAL; suspend timeout/result adapter remains M9 | None |
| Termux | Termux catalog/plugin registration | Discovery through existing registration | Optional provider PARTIAL; process result/cancellation remains M9 | None |
| Vision/OCR/OCV/YOLO | Vision catalog/plugin contracts | Not implemented | Vision plugin provider later | None |
| Display capture | Workspace/Recorder/Overlay screenshot paths | Separate from command execution | M7-0 `CaptureFrameService` reuses M5-8 selection/availability types and Accessibility screenshot provider | None |
| ChartGraph | Chart query adapters | Not implemented | ChartGraph provider later | None |
| Custom Chrome Tab | Browser capability metadata | Discovery through existing registration | Optional provider PARTIAL; navigation callbacks remain M9 | None |

## Resolution and Failure Rules

1. Planner derives required capabilities from catalog-bound commands.
2. Registry resolves by explicit selection or documented preference, priority and stable provider ID.
3. Registration, installation, connection, permission, capability and execution availability remain distinct.
4. No match produces `CAPABILITY_PROVIDER_MISSING` before that command executes.
5. Provider failures retain diagnostic code, failure class, effect status and canonical entity ID.
6. Provider results are typed `ExecutionValue` values; `Void`, `Null` and `false` are distinct.
7. Fallback is disabled by default and only allowed after a proven `NOT_EXECUTED` failure.
8. Timeout, cancellation and unknown/maybe-executed effects never trigger an automatic retry.
9. Provider calls and results are traceable by capability, provider ID, selection reason and workflow entity.

M5-H owns one process-wide `AndroidRuntimeHost`. It constructs one explicit registry containing
the native timing and native gesture providers and exposes it through `RuntimeClient`. The gesture
gateway reuses the already connected `VisualTaskerAccessibilityService`; it does not activate that
service and fails closed when Android has not connected it. All other capabilities continue to fail
closed until a later, separately approved provider slice.

Click, swipe, stroke and typed PointerPath share one active-sequence gate. Concurrent requests are
rejected with `TOUCH_GESTURE_BUSY`, never executed in hidden parallel. Swipe repeats are dispatched
sequentially with a bounded inter-repeat cancellation point. Android completion/cancellation,
runtime cancellation, timeout, service loss, display changes and synchronous dispatch rejection
remain typed terminal outcomes.

Typed movement segments longer than 200 ms are dispatched as duration-preserving continued
subphases. Cancellation replaces the next continuation with a stationary release, bounding pending
physical delivery without changing PointerPath geometry or timing ownership.

The catalog's broad `swipe(points: ANY, repeat?: ANY)` payload now carries either the established
legacy numeric list or frozen V1 Path/PointerPath constructors. Legacy input retains the fixed
250 ms behavior. Typed input selects exactly one of total duration, per-segment durations or speed;
hold and delay remain PointerPath timing and never become Path geometry.

The supported subset has a debug-only physical gate for straight, quadratic and cubic paths,
slow/fast timing, cancellation, variable-bound PointerPath execution and M5-6 PinchIn, PinchOut,
parallel, opposing, delayed and cancellable MultiPath execution. The test surface records actual
pointer count, span delta and start skew; callback completion alone is not device acceptance.
Semantic text target resolution is live in M5-7. `clickElement`, variable-bound click text and
non-default displays remain explicit gaps rather than implicit fallback behavior.

## Runtime Host And Display Context

| Contract | Status | Current boundary |
| --- | --- | --- |
| UI-bound Android bridge | DONE | Compatibility bridge remains tested; productive Shell execution now uses `RuntimeClient`. |
| Process-wide headless host | DONE | `RuntimeProcessHost` reuses the runner, owns lifecycle outside Compose and supports status reattachment. |
| Persistent runtime status/trace | PARTIAL | Bounded non-sensitive recovery checkpoint and live session trace exist; full retained trace history remains M5-10. |
| Explicit target display | PARTIAL | M5-7 semantic target contracts and trace carry display ID and reject non-default displays; coordinate gestures still expose bounds only. |
| Virtual-display provider | PLANNED | M7-V is an optional feasibility slice, not an implicit capability of the gesture provider. |
| Remote display session | DEFERRED | M9-D follows only after M7-V and the VT2VT transport/session scope are accepted. |

Every future pointer, capture and semantic-target invocation must carry one explicit display
context. Absence or loss of that context is a typed failure; it must never silently fall back to the
default display. Headless execution changes the Android host only and must not create a second
`ExecutionRunner`, provider registry or trace model.

M5-6 maps every MultiPath to one provider operation and one globally synchronized Android phase
timeline. The service keys continuations by pointer ID and releases all still-continuing strokes
together on cancellation. The generic `touch.*` escape-hatch family and high-level push/pull/pan/
rotate actions are not silently mapped to this contract.

Physical acceptance on SM-S918B covers two-pointer PinchIn/Out, parallel and opposing movement,
a measured 250 ms delayed start and cancellation with both contacts observed. Android combinations
that would require a delayed pointer itself to cross a continuation boundary are rejected before
dispatch instead of being degraded to disconnected contacts or SingleTouch.

M5-8 adds request-local provider policy, deterministic discovery and safe fallback to this same
registry. `AndroidOptionalProviderInventory` reuses Tasker, Shizuku, Termux and Custom Tabs status
contracts without registering incomplete asynchronous hosts as successful execution providers.
Their exact completion/cancellation adapters remain explicit M9 gaps.

M7-0 applies the same `ProviderSelectionPolicy`, `ProviderAvailability` and stable `ProviderId`
contract to display capture without registering capture as an EMScript execution side effect.
Capture has its own typed result and lifecycle because a frame is shared evidence, not a scalar
runtime value. See `M7_CAPTURE_PROVIDER_MATRIX.md`.
