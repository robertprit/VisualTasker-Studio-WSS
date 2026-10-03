# EMScript v1 Query Return Convergence Complete

Stand: 2026-10-03

Milestone: M1B-3

Status: COMPLETE

## Final Audit

| Metric | Final value |
| --- | ---: |
| `D_QUERY_RETURN` | 0 |
| Command Catalog | 127 |
| `NATIVE_V1` | 3 |
| `UNMAPPABLE` | 0 |

No V1 catalog query remains statement-shaped or without a canonical static
return type. Historical M1B-3V through M1B-3Y reports remain unchanged as the
decision evidence for their respective migration stages.

## Final Return Categories

The generic EMScript value and adapter path now covers:

- primitive `Bool`, `Number` and `String`
- nullable `T?` and explicit `NullValue`
- `List<T>` through immutable `ListValue`
- `TaskerVariableValue`
- perception values `ImageMatchValue`, `TextMatchValue` and `MarkerValue`
- chart value `ChartSnapshotValue`
- `Success(noValue)`, `Success(value)`, `Success(NullValue)` and
  `Failure(Diagnostic)` as distinct runtime outcomes

Complex results continue to use `RuntimeAdapterResult`, `EmscriptValue`, the
generic command reporter and `IrExpression.CommandCall`. Provider DTOs and
runtime handles do not cross the language boundary.

## Closure Invariants

- `false != Failure`
- `zero != Failure`
- empty string `!= NullValue`
- empty list `!= NullValue`
- `NullValue != noValue`
- `ABSENT != Failure`
- domain value `!=` runtime handle
- provider DTO `!=` EMScript domain value
- complex return `!=` special IR expression
- declaration `LET` and mutation `SET` remain distinct through roundtrip
- every query has a canonical non-`Void` static return type

## Final Chart Closure

`chart.exists(...): Bool` returns true or false only after a successful lookup.
`chart.get(...): ChartSnapshot?` returns an immutable snapshot or `NullValue`.
Repository, storage and decode failures remain diagnostics. Both commands use
one repository lookup contract, retain their stable IDs and legacy aliases,
and project through the existing generic editor and IR paths.

## Scope Boundary

M1B-3 is closed at the language/runtime return boundary. The following remain
outside this milestone: general property access, chart UI or rendering work,
chart DSL expansion, general dataset/table abstractions, runtime handles,
toolbox reorganization, perception workspace work and subsequent roadmap
milestones.
