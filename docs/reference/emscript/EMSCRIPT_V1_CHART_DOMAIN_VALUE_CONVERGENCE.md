# EMScript v1 Chart Domain Value Convergence

Stand: 2026-10-03

Phase: M1B-3Z

Status: COMPLETE

## Signatures

| Stable command ID | Canonical source name | Return type | Legacy aliases |
| --- | --- | --- | --- |
| `chart.exists` | `chart.exists` | `Bool` | `Chart.exists` |
| `chart.get` | `chart.get` | `ChartSnapshot?` | `Chart.get` |

Both commands are generic value reporters and remain
`IrExpression.CommandCall`. Canonical serialization writes the lower-case
namespace while imports retain the existing upper-case aliases. Existing
parameters are unchanged. The optional `key` parameter of `chart.get` remains
accepted for legacy import, but a non-empty value is rejected at runtime
because EMScript v1 has no frozen property-access syntax.

## ChartSnapshotValue

`ChartSnapshotValue` is an immutable EMScript domain value copied from the
existing ChartGraph `ChartDocument`. It contains:

- stable chart `id`, `title` and existing `ChartKind` name
- series with stable IDs, labels, colors and points
- candles, box plots and outliers, heatmap cells and histogram bin count
- bubbles, Venn sets and overlaps, and mosaic cells
- gauge value and range
- Gantt tasks
- radar series and axes
- diagram nodes and edges
- domain options: x-axis label, y-axis label and legend visibility

The snapshot does not contain a renderer, Canvas, Compose state, View,
repository entity, cursor, runtime session or mutable runtime handle. Chart and
dataset structures are not converted to strings, JSON or `List<Any>`.

## Identity And Immutability

The persisted `ChartDocument.id` is retained. Repository results whose ID does
not match the requested ID are invalid; no identity is generated during
lookup. Series, task, radar, Venn and diagram identities remain separate from
the chart identity.

All top-level and nested collections are defensively copied and exposed as
unmodifiable collections. Mutating repository-owned lists, point collections,
outlier lists or overlap sets after lookup cannot modify an already returned
snapshot.

## VALUE, ABSENT And FAILURE

| Repository state | `chart.exists(id)` | `chart.get(id)` |
| --- | --- | --- |
| present and valid | `BooleanValue(true)` | `ChartSnapshotValue` |
| missing | `BooleanValue(false)` | `NullValue` |
| adapter/repository unavailable | failure diagnostic | failure diagnostic |
| query failure | failure diagnostic | failure diagnostic |
| storage/load failure | failure diagnostic | failure diagnostic |
| invalid/decode result | failure diagnostic | failure diagnostic |

Both commands use the same injected chart lookup boundary, so presence and
retrieval cannot silently consult different sources. Missing is a successful
domain result. It is never represented as a failure, an empty snapshot or a
sentinel ID.

The runtime uses these diagnostics where applicable:

- `CHART_REPOSITORY_UNAVAILABLE`
- `CHART_QUERY_FAILED`
- `CHART_LOAD_FAILED`
- `CHART_DECODE_FAILED`
- `CHART_RESULT_INVALID`

## Runtime, Types And Editors

The repository result is mapped once to `RuntimeAdapterResult` containing an
`EmscriptValue.ChartSnapshotValue` or `NullValue`. `chart.exists` derives its
Boolean from that same value/absence result. Technical failures remain
`RuntimeAdapterResult.failure` and preserve their diagnostic.

`chart.exists` has static type `Bool`; it is compatible with `Bool` and
`Bool?`, not with `String`, `Number` or `ChartSnapshot`. `chart.get` has static
type `ChartSnapshot?`; it is not weakened to `Any`, `String` or a non-null
snapshot.

The BlockEditor uses the generic command-reporter projection for both values.
No chart-specific reporter or IR expression was introduced. Source, parser,
workspace, IR and serializer preserve command identity, arguments, nullable
return type and expression structure across LET, SET, IF and roundtrip paths.

## Property Access

Transport, assignment, serialization and reporter projection are available.
No new `snapshot.id`, `snapshot.series` or equivalent syntax is introduced by
M1B-3Z. Property access remains a separate language decision.

## Post-Migration Audit

- `D_QUERY_RETURN = 0`
- Command Catalog `= 127`
- `NATIVE_V1 = 3`
- `UNMAPPABLE = 0`
- Remaining `D_QUERY_RETURN` commands: none

M1B-3Z closes the two final query-return conflicts without expanding the chart
engine, UI, rendering architecture, dataset model or DSL.
