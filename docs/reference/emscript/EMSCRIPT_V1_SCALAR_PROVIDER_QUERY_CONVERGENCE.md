# EMScript v1 Scalar Provider Query Convergence

Stand: 2026-10-02

Phase: M1B-3W

## Scope

| Command | Return | VALUE | NEGATIVE / ABSENT | FAILURE |
| --- | --- | --- | --- | --- |
| `tasker.isEnabled()` | `Bool` | `BooleanValue(true)` | `BooleanValue(false)` for a reliably disabled or not-installed Tasker | adapter, installation or enabled-preference diagnostic |
| `tasker.getVariable(name)` | `String?` | `StringValue(value)`, including `""` | `NullValue` when the snapshot contains no variable | adapter or variable-query diagnostic |
| `shizuku.getUid()` | `Number?` | `NumberValue(uid)`, including `0` | `NullValue` when no service UID is available | package, permission, Binder or UID diagnostic |
| `termux.get(key)` | `String?` | `StringValue(value)`, including status text | `NullValue` for an unknown frozen key | adapter or status-inspection diagnostic |
| `scrcpy.isRunning(serial?)` | `Bool` | `BooleanValue(true)` | `BooleanValue(false)` when no matching active session exists | adapter or session-inspection diagnostic |
| `scrcpy.get(key)` | `String?` | `StringValue(value)` | `NullValue` when the session or optional value is absent | adapter, session-inspection or unknown-key diagnostic |

## Provider Boundaries

The providers remain independent. Their only shared transport is
`RuntimeAdapterResult` plus `EmscriptValue`. Messages are trace text and never
carry hidden return values.

Tasker variable values come from the latest variable snapshot explicitly
delivered through the WSS Tasker plugin receiver. scrcpy status comes from the
minimal WSS scrcpy session snapshot and is not inferred from USB/ADB bridge
readiness.

## Projection And Compatibility

All six commands remain generic `IrExpression.CommandCall` reporters. Stable
command IDs remain unchanged. Former upper-case provider spellings remain
import aliases; generated EMScript uses canonical lower-case provider names.

Post-M1B-3W inventory:

- `D_QUERY_RETURN = 6`
- Command Catalog `= 127`
- `NATIVE_V1 = 3`

Remaining conflicts are `tasker.getVariables`, `action.findTemplate`,
`vision.findText`, `vision.markerLoad`, `chart.exists` and `chart.get`.
