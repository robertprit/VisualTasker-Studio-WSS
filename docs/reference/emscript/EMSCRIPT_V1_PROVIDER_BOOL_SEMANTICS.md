# EMScript V1 Provider Bool Semantics Freeze

Status: M1B-3S is the historical freeze; M1B-3T migrated the three
`isInstalled` queries and M1B-3U migrated `shizuku.isAvailable`.

## Shared invariant

For all four audited commands, `false` is a successful negative answer only
when the underlying technical inspection completed. Adapter absence,
PackageManager failure, Binder/API failure and permission-inspection failure
are failures, not `false` and not `NullValue`. None of the four contracts has a
legitimate absent value; the proposed type is non-null `Bool`.

The generic M1B-3R `RuntimeAdapterResult` payload is reusable. Workspace and IR
already have generic reporter and `IrExpression.CommandCall` infrastructure.
The commands are nevertheless not migration-ready because their current
provider inspections erase technical failures before the typed transport sees
them.

## `tasker.isInstalled`

- Stable ID: `tasker.isInstalled`
- Current canonical spelling: `Tasker.isInstalled`; aliases: none
- Parameters: none; current return declaration: Void/unspecified
- Source: `TaskerRegistrationStatus.installed`
- `true`: either `net.dinglisch.android.tasker` or
  `net.dinglisch.android.taskerm` was resolved by PackageManager.
- `false`: neither supported package was definitively found.
- Failure: host adapter unavailable or package inspection failed technically.
- Proposed diagnostics: `TASKER_ADAPTER_UNAVAILABLE`,
  `TASKER_INSTALLATION_CHECK_FAILED`.

Installation does not include launchability, `RUN_TASKS` permission, Tasker
enabled preference, external access or broadcast receiver reachability.
`tasker.isEnabled` remains a separate unresolved composite command.

Current collision: every `getPackageInfo` exception is converted to an
unsuccessful candidate lookup. The earliest semantic loss is therefore in
`TaskerRegistration.inspect`; the surviving Boolean is later flattened to
adapter message text in `WorkspaceScreen`.

## `shizuku.isInstalled`

- Stable ID: `shizuku.isInstalled`
- Current canonical spelling: `Shizuku.isInstalled`; aliases: none
- Parameters: none; current return declaration: Void/unspecified
- Source: `ShizukuRegistrationStatus.installed`
- `true`: PackageManager resolved `moe.shizuku.privileged.api`.
- `false`: that package was definitively not found.
- Failure: host adapter unavailable or package inspection failed technically.
- Proposed diagnostics: `SHIZUKU_ADAPTER_UNAVAILABLE`,
  `SHIZUKU_INSTALLATION_CHECK_FAILED`.

Binder state, permission and UID do not participate in installation. Current
`runCatching(...).isSuccess` collapses every PackageManager exception to
`false`, so provider repair is required before migration.

## `shizuku.isAvailable`

Repository meaning is exact:

```text
installed && permissionGranted && binderAlive
```

It does not mean installation alone, permission alone, configuration, or proof
that a productive shell operation succeeded. A live Binder probe establishes
API reachability, while permission establishes authorization for this app.

- `true`: package installed, Binder responds, permission granted.
- `false`: at least one successfully inspected prerequisite is negative.
- Failure: adapter unavailable or package/Binder/permission inspection failed
  technically.
- Proposed diagnostics: `SHIZUKU_ADAPTER_UNAVAILABLE`,
  `SHIZUKU_AVAILABILITY_CHECK_FAILED`.
- Proposed type: `Bool`; no legitimate absence.

Current source collisions:

- PackageManager exceptions become `installed=false`.
- `Shizuku.pingBinder()` exceptions become `binderAlive=false`.
- live-Binder permission-check exceptions become `permissionGranted=false`.
- `WorkspaceScreen` additionally uses the composite Boolean as adapter success,
  making a valid negative answer indistinguishable from execution failure.

### Shizuku state matrix

| State | `isInstalled` | `isAvailable` | Failure |
|---|---:|---:|---|
| Package definitively absent | false | false | no |
| Installed, Binder inactive | true | false | no |
| Binder active, permission missing/denied | true | false | no |
| Installed, Binder active, permission granted | true | true | no |
| Package inspection fails | no value | no value | installation/availability check failed |
| Binder probe fails technically | installation result remains valid | no value | availability check failed |
| Permission inspection fails technically | installation result remains valid | no value | availability check failed |

An inconsistent synthetic state such as a live Binder while the package is not
visible still yields `isInstalled=false` and `isAvailable=false` under the
current explicit formula; it must not be reinterpreted as installation.

## `termux.isInstalled`

- Stable ID: `termux.isInstalled`
- Current canonical spelling: `Termux.isInstalled`; aliases: none
- Parameters: none; current return declaration: Void/unspecified
- Source: `TermuxRegistrationStatus.installed`
- `true`: PackageManager resolved `com.termux`.
- `false`: `com.termux` was definitively not found.
- Failure: host adapter unavailable or package inspection failed technically.
- Proposed diagnostics: `TERMUX_ADAPTER_UNAVAILABLE`,
  `TERMUX_INSTALLATION_CHECK_FAILED`.

`com.termux.api`, `RUN_COMMAND` permission, launchability, configuration and
command execution are separate facts. The helper currently converts every
`getPackageInfo` exception to `false`, so failure semantics are not yet ready.

## Readiness

| Command | Value source | Failure semantics | Typed transport | Workspace | IR | Migration |
|---|---|---|---|---|---|---|
| `tasker.isInstalled` | ready | ready | ready | ready | ready | migrated in M1B-3T |
| `shizuku.isInstalled` | ready | ready | ready | ready | ready | migrated in M1B-3T |
| `shizuku.isAvailable` | ready | ready | ready | ready | ready | migrated in M1B-3U |
| `termux.isInstalled` | ready | ready | ready | ready | ready | migrated in M1B-3T |

The installation queries retain independent provider boundaries. The separate
M1B-3U slice now preserves Shizuku package, permission and Binder provenance
before projecting the composite result to Bool.

## Explicit exclusions

`tasker.isEnabled`, `shizuku.getUid`, `termux.get`, all other provider queries,
`input.touch`, Recorder, Designer, VT2VT, IME, RAG, LLM and ML are unchanged.
M1B-3T repaired the package-inspection boundary and M1B-3U repaired the
composite availability boundary. `D_QUERY_RETURN` is now 12; bridge inventory
remains 127 and `NATIVE_V1` remains 3.
