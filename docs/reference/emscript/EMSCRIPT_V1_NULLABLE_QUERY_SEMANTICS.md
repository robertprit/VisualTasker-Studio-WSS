# EMScript v1 Nullable Query Semantics Decision

Date: 2026-09-25
Phase: M1B-3K
Status: IMPLEMENTED BY M1B-3M

## Decision

The smallest V1 model supported by repository evidence is nullable `String`:

```text
File.readText(path: String): String?
datastoreGet(key: String): String?
```

Absence is a normal value state for these two queries. Empty String is a
successful value. Invalid input, permission denial, provider/store
unavailability and operational failure use the runtime failure and diagnostic
channel. M1B-3M implements these two contracts without migrating adjacent queries.

## Current Repository Semantics

| Layer | `file.readText` | `system.datastoreGet` |
| --- | --- | --- |
| Stable ID / canonical | `file.readText` / `File.readText` | `system.datastoreGet` / `datastoreGet` |
| Legacy aliases | none | `Datastore.get`, `DATASTORE_GET` |
| Catalog argument | `path: TEXT`, legacy default `""` | `key: TEXT`, legacy default `"key"` |
| Catalog return | `String?`; reporter block | `String?`; reporter block |
| Parser / Workspace / IR | generic CommandCall expression with `String?` output | generic CommandCall expression with `String?` output |
| Kotlin environment | `(String) -> String?` | `(String) -> String?` |
| Host success | regular file contents, including `""` | map value, including `""` |
| Host null | missing path or non-file | key absent from a successfully loaded store |
| Failure separation | invalid path, permission and I/O errors carry structured runtime diagnostics | datastore-load failure carries `DATASTORE_LOAD_FAILED` |
| Current outcome | `StringValue`, `NullValue`, or Failure | `StringValue`, `NullValue`, or Failure |

Serialization retains the reporter command, arguments and nullable return type
through Workspace and IR. Runtime tests cover non-empty and empty values,
absence, invalid input, permission/I/O failure and datastore-load failure.

## State Decision Table

| State | Current `file.readText` | Current `datastoreGet` | Normative V1 value | Runtime failure |
| --- | --- | --- | --- | --- |
| VALUE | returns file text | returns stored text | non-null `String` | no |
| EMPTY_VALUE | returns `""`; success, but length log is `0` | returns `""`; success, but log text equals absent text | `""` | no |
| ABSENT | missing/non-file path returns null | missing key returns null | absent member of `String?` | no |
| PERMISSION_DENIED | read may throw; not normalized | not evidenced at get boundary | none | yes |
| INVALID_ARGUMENT | blank/traversal-invalid path currently returns null | no distinct state evidenced | none | yes for file path |
| FAILURE | read exception may escape | load failure becomes empty map and collides with absent | none | yes |
| UNSUPPORTED | not evidenced; core-owned | not evidenced; core-owned | n/a | n/a |

The two current diagnostic renderings are lossy, but `""` is not itself used
as the runtime sentinel: Kotlin null is. The normative contract preserves
empty and absent as distinct states and removes the two documented collisions
during a later implementation slice.

## Model Assessment

| Model | Decision | Reason |
| --- | --- | --- |
| A: `String` + runtime failure | rejected | Missing file and missing key are legitimate absence, not successful Strings or failures. |
| B: `String?` | selected | Smallest model that preserves String, empty and absent while keeping failure out of the value channel. |
| C: `Option<String>` | rejected for V1 | Adds constructors and matching APIs without evidence that a container abstraction is required. |
| D: caller default | rejected | Neither command currently has that API; it would erase observable absence and invent semantics. |
| E: `Result<String>` | rejected for V1 | Mixes operational failure into the value model despite an existing execution failure/diagnostic channel. |

## Language-Wide Impact

The first implementation of `T?` must be one coherent vertical slice. It
affects `LanguageTypeRef`, assignability, LET inference, SET compatibility,
equality, expression and command typechecking, function parameters/returns,
`List<T>`, Workspace value outputs/connectors, Flow ports, serializer schema,
IR return types and runtime values. `Any` remains a value type and cannot hide
absence or failure.

M1B-3L adds the orthogonal `LanguageTypeRef.Nullable(baseType)` wrapper,
generic assignability, typed LET transport, Workspace/connector/IR/source
roundtrip and runtime contract checks. `EmscriptValue.NullValue` is the absent
value and remains distinct from both `StringValue("")` and runtime failure.
M1B-3M applies that infrastructure to exactly these two production queries.

The rule `T -> T?` is allowed and `T? -> T` is rejected without explicit
handling. The source-level absence-test/unwrap operation remains unresolved and
must be frozen before a production nullable query is migrated. No
query-specific branch may bypass the generic CommandCall expression pipeline
introduced by M1B-3J.

## Adjacent Query Impact

- `vision.markerLoad` may later need `Region?` for not-found, but its evidence
  and provider failure states remain separate work.
- `vision.templateCompare` may later need `Number?`; unavailable comparison is
  not resolved by this String decision.
- `action.findTemplate` still needs a structured `ImageMatch` model and must
  distinguish no match from unavailable evidence.
- `shizuku.getUid` must remove the `-1` sentinel and separate provider/
  permission failure before choosing nullable `Number`.
- `tasker.getVariable` and `chart.get` have no authoritative typed runtime
  result yet and remain blocked.

No adjacent query is migrated or normatively resolved by M1B-3K.

## Compatibility And Follow-Up

The catalog still contains 127 entries, `D_QUERY_RETURN` is now 18 and
`NATIVE_V1` remains 3. The M1B-3J queries remain non-null `String`.
`input.touch`, providers and Designer are outside this slice.

The next query slice must reclassify remaining nullable scalar candidates
separately from structured-result, runtime-gap and provider-dependent queries.
