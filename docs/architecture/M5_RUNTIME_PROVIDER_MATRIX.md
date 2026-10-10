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
| MultiPath/multitouch | V1 contract | Not implemented | Out of M5-4 scope; nonzero pointer IDs fail explicitly | None |
| Semantic `clickElement` | Planned A11y target plus optional relative bounds point | Not implemented | Contract only; requires A11y/worldview resolver | None |
| Semantic `clickText` | Existing text command; target resolver not in M5 | Not implemented | Contract only; requires A11y text resolver | None |
| Semantic `clickImage` / `clickTemplate` | Planned image/template target and search region | Not implemented | Contract only; requires Vision provider | None |
| Clipboard/file/cache/system | Core catalog | Fake provider | Existing BasicRun environment later | None |
| Tasker | Tasker catalog/plugin contracts | Not implemented | Optional provider | None |
| Shizuku | Shell capability metadata | Not implemented | Optional provider | None |
| Termux | Termux catalog/plugin registration | Not implemented | Optional provider | None |
| Vision/OCR/OCV/YOLO | Vision catalog/plugin contracts | Not implemented | Vision plugin provider later | None |
| ChartGraph | Chart query adapters | Not implemented | ChartGraph provider later | None |
| Custom Chrome Tab | Browser capability metadata | Not implemented | Integrations provider later | None |

## Resolution and Failure Rules

1. Planner derives required capabilities from catalog-bound commands.
2. Registry resolves providers in explicit registration order.
3. No match produces `CAPABILITY_PROVIDER_MISSING` before that command executes.
4. Provider failures retain their provider-supplied diagnostic code and canonical entity ID.
5. Provider results are typed `ExecutionValue` values; `Void`, `Null` and `false` are distinct.
6. Cancellation is visible through the context token and checked by the runner at controlled boundaries.
7. Provider calls and results are traceable by capability, provider ID and workflow entity.

M5-4 still contains no global provider singleton. The productive `AndroidRuntimeBridge` constructs
one explicit registry containing the native timing and native gesture providers. The gesture
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
slow/fast timing, cancellation and variable-bound PointerPath execution. Multitouch and semantic
target resolution remain documented gaps rather than implicit fallbacks.
