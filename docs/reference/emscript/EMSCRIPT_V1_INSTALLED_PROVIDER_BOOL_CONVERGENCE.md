# EMScript V1 Installed Provider Bool Convergence

Status: M1B-3T READY.

## Scope

This slice migrates exactly these package-presence queries:

- `tasker.isInstalled(): Bool`
- `shizuku.isInstalled(): Bool`
- `termux.isInstalled(): Bool`

Their former namespace spellings remain import-only V1 aliases:
`Tasker.isInstalled`, `Shizuku.isInstalled` and `Termux.isInstalled`.
`shizuku.isAvailable` is explicitly excluded because it combines package,
Binder and permission state.

## Result contract

`true` means PackageManager successfully resolved one of the command's
supported package IDs. `false` means package inspection completed and none of
those IDs was found. `PackageManager.NameNotFoundException` is therefore a
valid negative answer. Any other package-inspection exception is a structured
failure and produces no Bool value.

The diagnostics are:

| Command | Missing adapter | Inspection failure |
|---|---|---|
| `tasker.isInstalled` | `TASKER_ADAPTER_UNAVAILABLE` | `TASKER_INSTALLATION_CHECK_FAILED` |
| `shizuku.isInstalled` | `SHIZUKU_ADAPTER_UNAVAILABLE` | `SHIZUKU_INSTALLATION_CHECK_FAILED` |
| `termux.isInstalled` | `TERMUX_ADAPTER_UNAVAILABLE` | `TERMUX_INSTALLATION_CHECK_FAILED` |

Tasker checks both supported package IDs. A technical failure for one
candidate does not hide a later successful match. If no candidate matches,
the first technical failure is retained rather than collapsed to `false`.

## Shared pipeline

The commands use the existing generic reporter block, generic
`IrExpression.CommandCall`, `RuntimeAdapterResult` value payload and
`EmscriptValue.BooleanValue`. No provider-specific expression, runtime value,
result hierarchy or IR node is introduced. The typed values are valid in
`LET`, `SET` and `IF`; `Bool` flows to `Bool?`, while `String` and `Number`
targets are rejected.

Canonical serialization emits lower-camel provider namespaces. Parsing and
roundtrip preserve the stable command IDs and the distinction between a
successful `false` and technical failure.

## Unchanged contracts

`shizuku.isAvailable`, `tasker.isEnabled`, `shizuku.getUid`, `termux.get`,
ChromeTab, Template Compare, Touch, Recorder, Designer and all productive
provider actions are unchanged.

After this slice, `D_QUERY_RETURN` is 13, bridge inventory is 127 and
`NATIVE_V1` is 3.
