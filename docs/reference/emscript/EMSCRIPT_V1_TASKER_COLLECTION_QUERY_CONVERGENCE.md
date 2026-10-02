# EMScript v1 Tasker Collection Query Convergence

Stand: 2026-10-02

Phase: M1B-3X

## Contract

| Command | Return | VALUE | EMPTY | FAILURE |
| --- | --- | --- | --- | --- |
| `tasker.getVariables(pattern?)` | `List<TaskerVariable>` | ordered `ListValue` of `TaskerVariableValue(name, value)` | successful typed empty `ListValue` | adapter, installation, snapshot-query or invalid-result diagnostic |

`ListValue` is generic EMScript runtime infrastructure. It retains an explicit
element type, preserves order and copies its input into an externally
unmodifiable list. It is distinct from `NullValue` and from an adapter result
without a value.

`TaskerVariableValue` is an EMScript domain value rather than a Tasker, Android
or plugin DTO. M1B-3X includes only the stable variable name and String value
available in the existing receiver snapshot.

## Provider Boundary

`tasker.getVariable` and `tasker.getVariables` read the same latest snapshot
delivered through the WSS Tasker plugin receiver. The collection query adds no
second receiver, provider or source of truth. Optional filtering keeps the
existing single `pattern` argument; no matches produce a successful empty
list. Results are ordered deterministically by normalized variable name.

Messages remain trace text and never transport hidden values. Technical
failures use the existing Tasker diagnostics:

- `TASKER_ADAPTER_UNAVAILABLE`
- `TASKER_NOT_INSTALLED`
- `TASKER_VARIABLE_QUERY_FAILED`
- `TASKER_RESULT_INVALID`

## Projection And Compatibility

The command remains a generic value reporter and
`IrExpression.CommandCall`. Its exact static type is
`List<TaskerVariable>`; it is not assignable to `String`, `Number` or `Bool`.
The legacy spelling `Tasker.getVariables` is accepted on import, while
canonical serialization writes `tasker.getVariables`.

Post-M1B-3X inventory:

- `D_QUERY_RETURN = 5`
- Command Catalog `= 127`
- `NATIVE_V1 = 3`

Remaining conflicts are `action.findTemplate`, `vision.findText`,
`vision.markerLoad`, `chart.exists` and `chart.get`.
