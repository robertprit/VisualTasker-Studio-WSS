# EMScript v1 `chromeTab.isSupported` Result Semantics

Stand: 2026-09-28
Phase: M1B-3R
Status: READY; typed provider-result convergence

## Decision

Implemented V1 contract:

```emscript
chromeTab.isSupported(): Bool
```

The query asks one precise capability question in the current Android
package-manager context: can Android resolve at least one service for
`ACTION_CUSTOM_TABS_CONNECTION`? It does not ask whether a browser package is
merely installed, whether the user selected a provider, whether a Custom Tabs
session is connected, or whether opening a URL will succeed.

## Repository Path

1. The catalog entry has stable ID and canonical spelling
   `chromeTab.isSupported`, legacy import alias `ChromeTab.isSupported`, no
   parameters and return type `Bool`. It is reporter-shaped.
2. `CustomChromeTabRegistration.inspect` asks `PackageManager` for services
   matching `CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION`.
3. It prefers `com.android.chrome` when present, otherwise the first matching
   service. This preference does not alter the capability answer.
4. `CustomChromeTabStatus.supported` is `true` exactly when a matching service
   was resolved. `preferredPackage` and `packageCount` are supporting evidence.
5. `WorkspaceScreen.chromeTabCommand` returns execution success with an
   `EmscriptValue.BooleanValue` payload. The payload may be true or false.
6. `WorkspaceBasicRuntime` consumes that payload through the generic command
   expression evaluator and validates it against `Bool`.

## True / False / Failure

- `true`: the service-resolution query completed and returned at least one
  visible matching Custom Tabs service.
- `false`: the query completed successfully and returned no matching service.
- failure: the host adapter is unavailable or Android service resolution could
  not be completed reliably.

`false` is therefore a valid successful query value. It must not be reused as
an execution status, warning sentinel or substitute for an exception.

There is no legitimate absent result and no domain-level unknown state. An
unknown answer caused by execution failure belongs to the failure channel.

## State Matrix

| State | Current result | V1 true | V1 false | V1 failure |
| --- | --- | ---: | ---: | ---: |
| At least one matching Custom Tabs service resolves | success + `BooleanValue(true)` | yes | no | no |
| Service query completes with no match | success + `BooleanValue(false)` | no | yes | no |
| Browser installed but exposes no matching service | no matching service | no | yes | no |
| Host Custom Tabs adapter missing | failure without value | no | no | `CHROME_TAB_ADAPTER_UNAVAILABLE` |
| Provider not configured | not a state used by `inspect` | no | no | no |
| PackageManager/service resolution throws | failure without value | no | no | `CHROME_TAB_RESOLUTION_FAILED` |
| Runtime permission missing | no runtime permission is required by the current query | no | no | no |
| No active Custom Tabs session or binding | irrelevant to `inspect` | no | no | no |

The manifest declares package visibility for the Custom Tabs service action.
If service resolution still fails technically, that is resolution failure, not
proof of unsupported capability.

## Installed, Configured And Connected

An installed browser is not automatically supported: it must expose a service
matching the required Custom Tabs action. Conversely, `inspect` does not use a
selected-provider setting. It queries all visible matching services and only
uses Chrome-first ordering to choose `preferredPackage`.

No Custom Tabs session, binder connection, service binding, provider
configuration or browser permission participates in the query. Those states
belong to later operational commands such as bind, create or open.

## Typed Provider Transport

The existing `RuntimeAdapterResult` is extended additively. `success` remains
the execution outcome, `value: EmscriptValue?` is the optional typed return
payload, `diagnosticCode` identifies failures, and `message` remains human
readable trace text. No parallel runtime-value system is introduced.

The states are distinct:

- success plus `BooleanValue(true)`;
- success plus `BooleanValue(false)`;
- success with no value for Void;
- success plus `NullValue` for a nullable query;
- failure with a diagnostic and no value.

## Diagnostics

The minimum evidenced categories are:

- `CHROME_TAB_ADAPTER_UNAVAILABLE`: the host did not supply a Custom Tabs
  adapter, so the capability query was not executed.
- `CHROME_TAB_RESOLUTION_FAILED`: PackageManager/service resolution failed
  technically and produced no reliable capability answer.

No `PROVIDER_NOT_CONFIGURED` diagnostic is needed because provider selection is
not part of this query. No `PERMISSION_DENIED` diagnostic is frozen because the
current implementation requires no runtime permission.

## Expression Impact

The existing Bool, assignment and IF infrastructure consumes the result without
nullable-language additions:

```emscript
LET supported:Bool = chromeTab.isSupported()
IF chromeTab.isSupported()
    log("Custom Tabs available")
END IF
```

Both forms project through the generic reporter and
`IrExpression.CommandCall`. `Bool` is assignable to `Bool?`; String and Number
targets are rejected. A false IF condition selects its normal false branch,
whereas adapter or resolution failure stops through the runtime failure path.

## Related Provider Queries

`tasker.isInstalled`, `shizuku.isInstalled`, `shizuku.isAvailable` and
`termux.isInstalled` show the same broad transport shape: a concrete provider
Boolean is flattened into `RuntimeAdapterResult.success/message`. Their domain
meanings and failure boundaries are not identical, so M1B-3R does not migrate
them.

## Migration Boundary

M1B-3R migrates only `chromeTab.isSupported`. `D_QUERY_RETURN` changes from 17
to 16, bridge count remains 127 and `NATIVE_V1` remains 3. Tasker, Shizuku,
Termux, scrcpy, other Custom Tabs commands and all existing core/nullable
queries remain unchanged.
