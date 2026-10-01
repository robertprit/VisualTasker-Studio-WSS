# EMScript v1 Nullable Core Query Convergence

Date: 2026-09-26
Phase: M1B-3M
Status: IMPLEMENTED

## Scope

Only these stable commands are migrated:

```text
file.readText(path: String): String?
system.datastoreGet(key: String): String?
```

Both use the generic CommandCall expression pipeline. No file- or datastore-
specific AST, Workspace, IR or connector type was introduced.

## Runtime States

| Query | VALUE | EMPTY | ABSENT | FAILURE |
| --- | --- | --- | --- | --- |
| `file.readText` | file contents | `StringValue("")` | missing or non-regular file | `FILE_INVALID_PATH`, `FILE_READ_PERMISSION_DENIED`, `FILE_READ_FAILED` |
| `system.datastoreGet` | stored String | `StringValue("")` | missing key in a loaded store | `DATASTORE_LOAD_FAILED` |

`NullValue` represents only legitimate absence. Runtime failure remains in the
diagnostic/failure channel. Neither state is encoded as an empty String.

## Type Safety

`String` is assignable to `String?`; `String?` is assignable to `String?`; and
`String?` is not assignable to `String` or non-null `Any`. LET, SET, nested
arguments and connectors use the existing central compatibility rules.

The source `null` literal, nullable operators, truthiness and implicit unwrap
remain unavailable. Consumers can transport and persist nullable results, but
source-level absence branching remains a later language decision.

## Inventory

- D_QUERY_RETURN: 20 -> 18
- Bridge entries: 127 -> 127
- NATIVE_V1: 3 -> 3
- Other production queries migrated: none
