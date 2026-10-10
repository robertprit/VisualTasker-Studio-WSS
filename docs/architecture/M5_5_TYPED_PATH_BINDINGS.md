# M5-5 Typed Path Values, Runtime Bindings And Three-Editor Roundtrip

## Scope

M5-5 connects the M5-4 gesture engine to the existing language type system, variable runtime and
three editor projections. It does not add a second path engine or a second workflow truth:

`EMScript -> CanonicalWorkspaceDocument -> ExecutionPlan -> PointerPath value -> NativeGestureProvider -> Trace`

The existing V1 constructors `point`, `lineSegment`, `quadraticBezierSegment`,
`cubicBezierSegment`, `path` and `pointerPath` are now catalog-backed reporter expressions. The
existing `swipe` action consumes `Path`, `PointerPath` or the legacy numeric point list. No command
or source syntax was invented by this slice.

## Inventory And Ownership

- `LanguageTypes.kt` owns the named domain types `Point`, `PathSegment`, `Path` and `PointerPath`.
- `VisualTaskerCommandCatalog` owns constructor arguments, result types and the typed `swipe` input.
- `WorkspaceValueTypeSystem` owns docking compatibility, including homogeneous `List<T>` values.
- Parser, importer, IR and generator retain named arguments, sparse optional arguments and list
  literals through source apply and persistence.
- `WorkflowExecutionPlanner` lowers reporter graphs to typed runtime expressions. Constructors are
  evaluated by `PointerPathRuntimeConstructors`, not by a provider.
- `PointerPathLowerer` remains the only geometry/timing resolver. `NativeGestureProvider` remains
  the only native gesture provider.
- `WorkflowGraph` and stored path values are immutable during execution.

## Value And Binding Contract

The sealed runtime value surface contains `PointValue`, `PathSegmentValue`, `PathValue` and
`PointerPathValue`. `Path` owns timeless geometry; `PointerPath` combines one `Path` with timing and
single-pointer state. A value can be declared with `LET`, read through a variable reporter and
rebound with `SET`. Rebinding replaces the variable value and cannot mutate an already dispatched
path.

Constructor arguments may contain ordinary typed expressions:

- Point coordinates: `Number`
- Segment endpoints and controls: `Point`
- Path contents: variadic `Point`/`PathSegment`, `List<Point>` or `List<PathSegment>`
- Movement timing: exactly one of `duration`, `segmentDurations` or `speed`
- Additional timing: `startHold`, `endHold`, `delay`, `repeat`
- Pointer identity: `pointerId`; values other than zero remain the explicit multitouch boundary

Static workspace validation rejects incompatible connections such as `Number -> PointerPath`.
Runtime construction rejects unresolved or non-finite numbers, mixed list element types, missing
arguments, duplicate names, invalid whole-number timing and conflicting timing modes. Native
lowering then validates continuity, positive geometry, Android bounds and platform duration limits.

## Three-Editor Contract

Text, Block and Flow are projections of the same constructor/reporter graph:

- Text preserves typed declarations, list literals, named timing arguments and source anchors.
- Block uses catalog-generated reporters and typed ValueInputs. Timing and segment children are
  real connected expressions, not decorative fields.
- Flow receives the same expression nodes and typed data-flow ports. Projection does not create a
  second path definition.

The central conformance harness covers line, quadratic and cubic segments, mixed paths, total
duration, explicit segment durations, speed, holds, delay, repeat, a PointerPath variable,
BlockEditor field mutation, FlowEditor field mutation, undo/redo, persistence, source apply,
unknown metadata and stable semantic identities.

## Trace And Cancellation

Provider start/finish, failure and cancellation events carry a bounded `PointerPathTraceInfo`:

- segment count and segment type names
- planned total duration including repeat
- runtime status
- invalid segment index when a reliable diagnostic identifies one
- cancellation location when the provider boundary can identify it

Entity ID, capability and provider ID remain fields of the enclosing canonical trace event. Full
coordinates are deliberately not copied into Logcat or RailTrace. Android trace projection passes
the structured summary through the existing `OperationResult` model.

M5-4 cancellation boundaries remain unchanged: delays, repeated gestures and 200 ms Android
continuation phases observe cancellation; terminal results cannot be overwritten by late callbacks.

## Performance

`PointerPathContractTest.long PointerPath lowering is deterministic and bounded` constructs a
320-segment path and lowers it 100 times. The final JVM run on 2026-10-11 measured 178.021377 ms in
total (about 1.780 ms per lowering, including equality assertions). The fixture produces:

- 320 resolved movement segments
- one provider dispatch per repeat
- 320 planned Android stroke phases and callbacks because every segment is 10 ms, below the
  service's 200 ms continuation boundary

The test has a conservative two-second ceiling for 100 lowerings. Physical Android dispatch time is
not represented as lowering CPU time and is verified separately by the device gate.

## Physical Device Verification

The debug-only M5-5 gate prepares a canonical workflow with a dynamic `duration` variable, a
`PointerPath` variable named `route` and `swipe(route)`. It then launches the ordinary Workspace
shell; execution still starts through the productive global Play action and the existing
`AndroidRuntimeBridge`.

On the connected SM-S918B (`R3CW5032EGH`) the variable-bound path reached runtime state
`COMPLETED`. The in-app gesture target measured one 324 px horizontal gesture in 969 ms. The
AccessibilityService stayed available, no runtime failure was logged, and the gate restored the
previous canonical workspace after the run.

Repository gates are green: full BlockEditor tests, full FlowEditor tests, all 740 app unit tests,
the extended M4 three-editor harness in three separate runs, `:app:assembleDebug` and both diff
checks.

## Acceptance

| Criterion | Status | Evidence |
| --- | --- | --- |
| AC-01 contracts inventoried | PASS | This document and the runtime/provider matrix. |
| AC-02 typed PointerPath runtime value | PASS | Sealed execution values and constructor evaluator. |
| AC-03 variable-bound execution | PASS | Productive runner/provider test with `LET route` and `swipe(route)`. |
| AC-04 dynamic geometry/timing | PASS | Variable coordinate, duration, holds, delay and repeat fixture. |
| AC-05 static and runtime validation | PASS | Shared type-system tests, planner rejection and constructor/lowerer diagnostics. |
| AC-06 TextEditor roundtrip | PASS | Named/list expression parser, IR/generator and source-apply fixtures. |
| AC-07 BlockEditor projection/editing | PASS | Catalog reporters, typed inputs and mutation/undo test. |
| AC-08 FlowEditor projection/editing | PASS | Typed Flow nodes/ports and Flow property mutation test. |
| AC-09 identity/anchors/metadata | PASS | Three-editor fingerprints, persistence and unknown metadata fixture. |
| AC-10 trace/cancellation | PASS | Structured path trace plus existing M5-4 lifecycle tests. |
| AC-11 performance measured | PASS | 320 segments x 100 deterministic lowerings and phase counts above. |
| AC-12 full gates/device | PASS | Full repository gates plus productive Play-path execution on SM-S918B. |

## Explicit Boundaries

- No multitouch or `MultiPath`.
- No semantic A11y/OCR/OCV/Vision target resolution.
- No Tasker, Shizuku or remote provider.
- No functions, new scopes or TRY/CATCH runtime.
- No new editor panel or dedicated path drawing UI.
- Generic list indexing and the complete V1 collection surface remain outside this slice.
