# EMScript V1 Shizuku Availability Convergence

Status: M1B-3U READY.

## Public contract

`shizuku.isAvailable(): Bool` has no parameters and no legitimate absent or
unknown value. It means exactly:

```text
installed && permissionGranted && binderAlive
```

The historical `Shizuku.isAvailable` spelling remains an import-only V1 alias.
Canonical serialization emits `shizuku.isAvailable`.

## Provider-local provenance

The inspection layer retains one of these internal states before projecting to
the public Bool:

| Internal state | Public result |
|---|---|
| `NOT_INSTALLED` | successful `false` |
| `PERMISSION_NOT_GRANTED` | successful `false` |
| `BINDER_NOT_ALIVE` | successful `false` |
| `AVAILABLE` | successful `true` |

Technical inspection failures carry an independent failure stage and no Bool:

| Failure stage | Diagnostic |
|---|---|
| host adapter absent | `SHIZUKU_ADAPTER_UNAVAILABLE` |
| package inspection | `SHIZUKU_INSTALLATION_CHECK_FAILED` |
| permission inspection | `SHIZUKU_PERMISSION_CHECK_FAILED` |
| Binder inspection | `SHIZUKU_BINDER_CHECK_FAILED` |

The provider state is not exposed as a new EMScript type. It exists only long
enough to prevent false-as-failure collapse and to retain useful trace text.

## Shared pipeline

The command uses the generic command reporter, generic
`IrExpression.CommandCall`, existing `RuntimeAdapterResult` and
`EmscriptValue.BooleanValue`. It is valid in LET, SET and IF. `Bool` widens to
`Bool?`; String and Number targets are rejected. Successful `false` selects the
normal ELSE branch, while inspection failure follows the structured runtime
failure path.

Source, Workspace, IR and canonical serialization preserve the stable command
ID. No Shizuku-specific expression, reporter or runtime-result hierarchy is
introduced.

## Inventory

After M1B-3U, `D_QUERY_RETURN` is 12, command catalog count is 127 and
`NATIVE_V1` is 3. Installation reporters and unrelated provider queries remain
unchanged.
