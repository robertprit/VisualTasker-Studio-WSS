# EMScript v1 Nullable Type Infrastructure

Date: 2026-09-26
Phase: M1B-3L
Status: IMPLEMENTED; NO PRODUCTION QUERY MIGRATED

## Representation

Nullability is an orthogonal serializable wrapper:

```text
LanguageTypeRef.Nullable(baseType = T)
```

There are no `STRING_NULLABLE`, `NUMBER_NULLABLE` or `BOOL_NULLABLE`
primitives. Nested nullable wrappers are invalid. Source names use the sole
suffix syntax `T?`.

## Assignability

| Source | Target | Result |
| --- | --- | --- |
| `T` | `T` | accept |
| `T` | `T?` | accept |
| `T?` | `T?` | accept |
| `T?` | `T` | reject |
| `String?` | `Number?` | reject |
| `String` | `Any` | accept |
| `String?` | `Any` | reject |
| `String` | `Any?` | accept |
| `String?` | `Any?` | accept |

There is no implicit unwrap, truthiness or sentinel conversion. `Bool?` is not
assignable to the non-null `Bool` required by IF and WHILE.

## Transport

- Parser: typed LET accepts `String?`, `Number?`, `Bool?`, `Any?` and domain
  `T?`.
- Workspace: `VariableDefinition.type`, connection `provides`/`accepts` and
  reporter `outputType` preserve the exact nullable type.
- IR: explicit LET type metadata and `CommandCall.returnType` preserve `T?`.
- Serializer: Workspace JSON and canonical EMScript retain the suffix.
- Commands: `ParameterDefinition.type` and `CommandDefinition.returnType`
  carry the same generic TypeRef; non-Void nullable returns are expressions.

## Runtime States

`StringValue("")` is a successful empty value. `NullValue` is ABSENT.
`EmscriptDryRunResult.Failure`, diagnostics and trace severity carry failures.
These states are never substituted for one another. A `NullValue` satisfies a
nullable runtime contract only; a non-null contract produces a structured
runtime contract failure.

## Diagnostics

- `NULLABLE_TO_NONNULL_ASSIGNMENT`
- `NULLABLE_ARGUMENT_TO_NONNULL_PARAMETER`
- `NULLABLE_VALUE_IN_NONNULL_CONTEXT`

## Deferred Decisions

- `NULL_LITERAL_DECISION_REQUIRED`: V1 deliberately has no source `null`
  literal.
- `NULLABLE_GENERIC_COMPOSITION_DEFERRED`: the model distinguishes
  `Nullable(ListOf(T))` and `ListOf(Nullable(T))`; source generic composition
  is not activated here.
- Equality, nullable arithmetic/comparison, absence testing and explicit
  unwrap syntax remain unimplemented until separately frozen.
- Function contracts can represent nullable parameters/returns, but this
  slice adds no function runtime.

## Migration Boundary

All nullable commands used by conformance tests are synthetic. The production
catalog is unchanged: `file.readText` and `system.datastoreGet` remain
unmigrated, `clipboard.get`, `system.info` and `system.env` remain non-null
`String`, D_QUERY_RETURN remains 20, bridge count remains 127 and NATIVE_V1
remains 3.
