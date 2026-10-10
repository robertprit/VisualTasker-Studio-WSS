# M5-4 Typed PointerPath, Stroke And Spline Gesture Engine

## Scope

M5-4 extends the existing M5 runtime and native gesture provider. It does not add a second
runtime, gesture engine or workspace mutation path:

`WorkflowGraph -> WorkflowExecutionPlanner -> PointerPath -> NativeGestureProvider -> VisualTaskerAccessibilityService -> ExecutionTrace`

The existing `action.swipe` command remains the executable catalog entry. Stroke is represented by
an ordered multi-segment path. Spline is represented by quadratic or cubic Bezier segments. No new
EMScript command name was introduced.

## Ownership

- `PointerPathContract.kt` owns immutable runtime geometry, timing and deterministic lowering.
- `WorkflowExecutionPlanner` decodes the losslessly retained `action.swipe` source payload into one
  typed `ExecutionValue.PointerPathValue`.
- `NativeGestureProvider` validates physical bounds, Android duration limits, concurrency,
  cancellation and provider lifecycle.
- `VisualTaskerAccessibilityService` is the only owner of Android `dispatchGesture` and translates
  typed segments into `GestureDescription.StrokeDescription` phases.
- `WorkflowGraph` remains immutable during execution. Runtime values and traces are not written
  back into the canonical workspace.

## Geometry Contract

`Path` is timeless geometry:

- `Line(from, to)`
- `QuadraticBezier(from, control, to)`
- `CubicBezier(from, control1, control2, to)`

Every segment must start at the previous segment endpoint. Coordinates must be finite,
non-negative absolute display pixels. A zero-length or discontinuous segment is rejected. Android
display bounds are checked immediately before dispatch, including Bezier control points.

The source decoder supports the frozen V1 constructors:

- `point(x, y)`
- `path(point(...), point(...), ...)`
- `path([lineSegment(...), quadraticBezierSegment(...), cubicBezierSegment(...)])`
- `pointerPath(path(...), timing...)`

Legacy `swipe([x1, y1, ...], repeat)` remains supported and lowers to linear segments with the
established 250 ms total movement duration.

## Timing Contract

Geometry and timing are separate. A typed PointerPath selects exactly one movement timing source:

1. `duration`: total movement duration in milliseconds. It is distributed deterministically over
   segments in proportion to geometric length. Every segment receives at least 1 ms and the sum is
   exactly the requested duration.
2. `segmentDurations`: one positive millisecond value per segment. Values remain exact and ordered.
3. `speed`: positive pixels per second. Every segment duration is `ceil(length / speed * 1000)`.

Quadratic and cubic lengths use a fixed 32-sample deterministic approximation for timing only.
Android receives slices of the original Bezier path; the curve is not silently replaced by one
straight movement.

Additional timing values are:

- `startHold`: stationary pointer hold before movement.
- `endHold`: stationary pointer hold after movement.
- `delay`: delay before the first pointer phase.
- `repeat`: positive number of sequential executions of the complete PointerPath.
- `pointerId`: currently must be `0`.

Timing names also accept their explicit unit aliases (`durationMs`, `segmentDurationsMs`,
`speedPixelsPerSecond`, `startHoldMs`, `endHoldMs`, `delayMs`). Unknown, duplicated or conflicting
timing arguments fail with typed diagnostics. Speed is never interpreted as duration.

## Android Lowering

Start hold, movement and end hold become ordered Android stroke phases. The first stroke uses
`willContinue`; following phases use `continueStroke`, retaining one pointer-down lifecycle across
the complete path. Line, quadratic and cubic segments originate from Android `Path.lineTo`,
`quadTo` and `cubicTo` respectively. Long movement segments are split with `PathMeasure` into
duration-preserving continuation phases of at most 200 ms. This is a lifecycle boundary for
cancellation, not a second geometry or gesture engine.

Each phase is checked against Android's maximum gesture duration. The provider callback deadline is
the greater of its configured minimum timeout or the complete expected path timeline plus a bounded
grace period. This prevents valid long paths from failing at the former fixed ten-second deadline.

One provider-owned and one service-owned gate prevent overlapping native gesture sequences.
Runtime cancellation closes the waiting provider call and marks the service sequence for cleanup.
At the next continuation boundary the service submits a one-millisecond stationary release instead
of the remaining path. Thus already delivered motion cannot be reversed, while pending physical
motion stops within one bounded phase. Late or duplicate callbacks cannot replace a terminal
result.

## Roundtrip And Identity

The parser/importer continues to preserve the complete raw command argument payload. The generator
emits the same segment constructors, ordering, control points and timing fields. Schema-2 workspace
persistence retains the payload, and semantic source apply preserves source-anchor entity and
relation identities. View-local block IDs from an unrelated fresh import are not identity truth.

Unsupported constructors or timing variants are diagnosed and are never silently flattened into a
linear swipe.

## Diagnostics

Relevant M5-4 diagnostics include:

| Code | Meaning |
| --- | --- |
| `TOUCH_PATH_CONSTRUCTOR_UNSUPPORTED` | Geometry or segment constructor is outside the V1 subset. |
| `TOUCH_PATH_DISCONTINUOUS` | A segment does not begin at the previous endpoint. |
| `TOUCH_PATH_ZERO_LENGTH` | A movement segment has no geometric length. |
| `TOUCH_TIMING_REQUIRED` | No duration, segment duration or speed is present. |
| `TOUCH_TIMING_CONFLICT` | More than one movement timing source is present. |
| `TOUCH_SEGMENT_TIMING_INVALID` | Segment duration count or value is invalid. |
| `TOUCH_SPEED_INVALID` | Speed is not positive and finite. |
| `TOUCH_MULTITOUCH_UNSUPPORTED` | M5-4 received a pointer ID other than zero. |
| `TOUCH_DURATION_ANDROID_LIMIT` | One Android stroke phase exceeds the platform limit. |

M5-2/M5-3 availability, bounds, busy, dispatch, timeout, service-loss and display-change
diagnostics remain unchanged.

## Verification

JVM coverage includes legacy lowering, linear stroke, quadratic and cubic Bezier segments, mixed
segments, proportional total duration, exact per-segment timing, speed conversion, holds, delay,
repeat, invalid controls, timing conflicts, discontinuity, Android limits, cancellation, stable
trace identity, source/workspace/IR/source roundtrip and an unchanged serialized workspace.

The debug-only device gate provides safe center-screen linear, quadratic, cubic, slow, fast and
cancellation fixtures. It measures displacement, traveled distance and touch duration and restores
the previous workspace after the gate. On the connected SM-S918B, linear, quadratic and cubic paths
completed; the 324-pixel speed fixtures measured 2312 ms at 140 px/s and 463 ms at 700 px/s. A
five-second cubic path cancelled after about 0.8 seconds and remained stopped at the same partial displacement
five seconds later. Every fixture preserved the serialized canonical workspace.

## Explicit Boundaries

- No `MultiPath`, multitouch, second pointer or high-level push/pull/pan/rotate execution.
- No OCR, A11y entity or Vision target resolution.
- No typed execution of generic `touch.down`, `touch.move` or `touch.up` in this slice.
- Source geometry must currently be constructor literals. Runtime variable-bound `Path` values need
  a later typed expression/value implementation.
- No dedicated `stroke` or `spline` command was invented; both are representations of `swipe` with
  typed Path geometry.
