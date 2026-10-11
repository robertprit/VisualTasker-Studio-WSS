# M5-6 Native MultiTouch And Synchronized Pointer Execution

## Scope

M5-6 extends the existing M5-3/M5-4 native gesture engine with parallel pointer execution. It does
not add another runtime, another Android dispatcher or new EMScript syntax:

`EMScript -> WorkflowGraph -> ExecutionPlan -> MultiPath -> NativeGestureProvider -> AccessibilityService -> Trace`

The frozen V1 spelling is `multiPath([pointerPath(...), ...])` followed by `multiSwipe(value)`.
`Path` remains timeless geometry, `PointerPath` remains one timed pointer and `MultiPath` is the
parallel aggregate. Pointer IDs are dispatch-local identities and are never workflow entity IDs.

## Contract And Ownership

- `LanguageTypes` owns the `MultiPath` type name.
- `VisualTaskerCommandCatalog` owns the existing `multiPath` value constructor and `multiSwipe`
  action contract.
- `PointerPathRuntimeConstructors` evaluates the constructor and allocates omitted pointer IDs in
  list order, always choosing the smallest unused non-negative integer.
- Explicit duplicate pointer IDs fail with `TOUCH_POINTER_ID_DUPLICATE`.
- `MultiPointerGestureLowerer` resolves every pointer independently and calculates one common
  terminal duration as the maximum pointer end time.
- `NativeGestureProvider` validates display bounds, Android pointer count, Android duration limits,
  cancellation and the shared one-operation concurrency gate.
- `VisualTaskerAccessibilityService` remains the only Android gesture dispatcher. Single touch is
  the one-stroke case of the same phase dispatcher.

Per-pointer `repeat` values other than one are rejected as ambiguous. Repetition belongs to a
complete multi-pointer gesture; V1 currently has no separate complete-gesture repeat argument.

## Synchronized Android Lowering

Every pointer is converted to timed sections:

1. optional start delay;
2. optional stationary start hold;
3. one or more line, quadratic Bezier or cubic Bezier segments;
4. optional stationary end hold.

The dispatcher builds one sorted global boundary set from all sections. Long sections are split at
most every 200 ms so runtime cancellation remains bounded. For every interval it slices all active
pointer paths, adds all corresponding `StrokeDescription`s to one `GestureDescription`, and marks a
stroke as continued only when the same pointer ID is active in the immediately following phase.
This preserves simultaneous contacts, delayed starts, independent end times and segment timing.

An idle interval becomes the next Android stroke start delay. It is never replaced by a dummy
touch. Android stroke-count and duration limits are checked before dispatch. Unsupported or invalid
combinations fail closed and are never reduced to single touch.

## Lifecycle

A `MultiPath` is one provider operation even when it contains several pointers. Click, single
PointerPath and MultiPath share the same provider and AccessibilityService in-flight gates.
Cancellation before dispatch submits nothing. Cancellation after dispatch requests one common
release; all still-continuing pointers receive stationary terminal continuations. Timeout, service
loss, display changes, Android cancellation, dispatch rejection and late or duplicate callbacks
retain the existing typed terminal behavior.

## Editors And Persistence

The central three-editor harness covers a variable-bound `MultiPath` with explicit pointer IDs,
different delays and different durations through:

`Text -> Workspace -> Block -> Flow -> Save/Reload -> Source Apply -> Text`

The canonical graph retains statement/expression IDs, relation IDs, source anchors and unknown
metadata. Pointer count, IDs, path segment types, order and timing remain ordinary typed expression
data; no editor-specific multi-pointer model was introduced.

## Trace

`PointerPathTraceInfo` now distinguishes `SINGLE_TOUCH` and `MULTI_TOUCH` and carries bounded
metadata only:

- pointer count and IDs;
- per-pointer start and end offsets;
- per-pointer and aggregate segment counts;
- planned common duration;
- terminal status and cancellation position.

Coordinates and full path payloads are not copied into RailTrace or Logcat. RailTrace projects the
summary into existing step properties and detail text.

## Explicit Gaps

- `touch.single`, `touch.multi`, `touch.down`, `touch.move`, `touch.up`, `touch.dispatch` and
  `touch.reset` remain typed escape-hatch contracts without an executable V1 binding in this slice.
- `push`, `pull`, `pan` and `rotate` remain future high-level actions lowered to `MultiPath` once
  their complete parameter contracts are implemented.
- `pinch` remains a reserved name, not a new V1 command. Pinch-in/out are represented by two
  opposing PointerPaths.
- No semantic A11y/OCR/OCV/YOLO target resolution and no Tasker, Shizuku or remote provider.
- No dedicated editor panel or gesture designer.
- Android cannot reliably introduce a new pointer after an existing pointer has crossed a
  `continueStroke()` timing-section boundary. Such combinations are rejected before dispatch.
  Delayed starts remain supported when the first shared descriptor can contain one complete,
  continuous stroke for every delayed pointer; start/end holds remain supported for simultaneous
  starts. A delayed pointer with its own hold or segment boundary is rejected rather than silently
  losing pointer continuity or timing.

## Verification

JVM coverage includes simultaneous and opposing pointers, pinch-in/out geometry, delayed and
different-duration pointers, three pointers, mixed line/Bezier paths, pointer-ID allocation,
duplicates, Android pointer limits, cancellation, timeout, service loss, callback races,
concurrency, variable binding, trace projection, three-editor roundtrip and immutable workspace
execution.

Android instrumentation inspects the actual Android `Path` phase planner for delayed and
simultaneous contacts, 200 ms continuation boundaries and stable pointer IDs. The debug device gate
prepares canonical PinchIn, PinchOut, Parallel, Opposing, Delayed and Cancellation workflows. Its
existing center-screen test surface measures the actual maximum pointer count, signed span change,
pointer start skew, travel and duration rather than trusting the Android completion callback alone.

The final SM-S918B gate measured two pointers for every physical case: PinchIn `spanDelta=-284`,
PinchOut `spanDelta=+284`, Parallel/Opposing `travel=726`, Delayed `startSkew=250 ms` and common
Cancellation after about `1.24 s`. All non-cancelled cases reached `COMPLETED`; the cancellation
case reached `CANCELLED` with both contacts observed. The planner instrumentation contains three
passing tests, including explicit pre-dispatch rejection of an unsupported delayed timing boundary.
BlockEditor and FlowEditor full suites, 751 app unit tests, `assembleDebug`, the three-run M4
conformance gate and all repository diff checks are green.

## Acceptance

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 existing engine reused | PASS | One provider, one AccessibilityService phase dispatcher. |
| AC-02 typed multi-pointer contract | PASS | `MultiPathValue`, `MultiPointerGesture` and deterministic ID rules. |
| AC-03 simultaneous pointers | PASS | Provider/JVM tests and Android synchronized phase instrumentation. |
| AC-04 independent start/end timing | PASS | Global timeline planner and delayed-pointer tests. |
| AC-05 continuation preserves contact | PASS | Pointer-ID keyed `continueStroke()` phases. |
| AC-06 cancellation and concurrency | PASS | Shared operation gate, common release and lifecycle tests. |
| AC-07 Android limits diagnosed | PASS | Pointer-count, bounds and duration gates. |
| AC-08 EMScript/runtime binding | PASS | Catalog constructor/action through productive planner/provider. |
| AC-09 three-editor stability | PASS | Central conformance fixture with persistence and source apply. |
| AC-10 trace completeness | PASS | Structured execution trace and RailTrace regression. |
| AC-11 physical Android execution | PASS | SM-S918B measured two real pointers for PinchIn/Out, Parallel, Opposing, Delayed and Cancellation. |
| AC-12 repository gates | PASS | Full BlockEditor/FlowEditor, 751 app tests, instrumentation, three-run M4 harness, assembleDebug and diff checks. |
