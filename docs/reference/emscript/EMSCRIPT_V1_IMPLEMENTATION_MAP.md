# EMScript v1 Implementation Map

Stand: 2026-09-26
Phase: M1B-3L Nullable Type Infrastructure
Status: generic T? transport implemented; production nullable queries remain unmigrated

## Boundary

The normative truth remains `EMSCRIPT_V1_SPEC.md` and
`EMSCRIPT_V1_GRAMMAR.ebnf`. The new JVM module
`visualtasker-blockeditor/emscript-language-core` expresses that contract as
serializable Kotlin data. The EMScript syntax highlighter now consumes its
keyword definitions read-only. Parser, catalog, runtime, formatter and other
editor language behavior remain unmigrated.

## Existing Owners

| Existing concept | Current class/file | Production role | M1A treatment |
| --- | --- | --- | --- |
| Parser keyword recognition and lexical tokens | `EmscriptParserSlice.kt` `TokenType` and lexer | Decides accepted text | NOT MIGRATED |
| Keyword highlighting | `SyntaxHighlighter.kt` (formerly hardcoded `controlKeywords`) | Read-only editor coloring | MIGRATED to `LanguageCoreDefinition.keywords` |
| Expression precedence | `EmscriptParserSlice.kt` recursive-descent functions | Decides current binding | Unchanged; currently differs from V1 |
| Parser operators | `EmscriptBinaryOp` | Parser IR and text dry run | IDENTITY MIGRATED through explicit stable IDs |
| Block operators | `CompareOperator`, `ArithmeticOperator`, `OperatorNormalization` | Block fields and workspace runtime | IDENTITY MIGRATED; legacy values retained |
| Generated-IR operators | String fields in `IrExpression.Compare/Operate` | EMScript generation | IDENTITY MIGRATED; strings contain stable IDs |
| Command names and aliases | `VisualTaskerCommandCatalog` | Parser lookup, toolbox, blocks, flow and runtime gates | `event.start`, `action.wait` and `feedback.beep` projected from V1; all others unchanged |
| Command kind | `CommandCatalogKind` | EVENT/STATEMENT/REPORTER/CONTROL/OPERATOR/VARIABLE grouping | Unchanged |
| Parameters and defaults | `CommandArgument` plus parser special cases | Current calls and generated blocks | event/wait/beep contracts typed in V1 then projected; all others unchanged |
| Argument types | `CommandArgumentType` plus block connection strings | Coarse current validation | Unchanged |
| Return types | Nullable strings in `CommandCatalogEntry` | Reporter eligibility and runtime placeholders | Unchanged |
| Side effects | `CommandSideEffect` in registry | Current metadata and gating | Unchanged |
| Capability/provider | `CommandCapability`, runtime binding and `pluginOwner` | Live capability presentation and adapter gates | Unchanged |
| Runtime availability | `CommandRuntimeBinding.liveImplemented` | UI and release preflight | Unchanged; not a language property |
| Core runtime values | `EmscriptValue` in runtime code | Dry/live evaluation | Unchanged |
| Deprecation/versioning | No central current owner | Not enforced | New contract only |
| Projection metadata | `rem.*`, `RemFlowMetadata`, IRGraph facets | Current editor projection | Unchanged; not migrated |

## Duplicate Truth

- Equality and comparison precedence exists in parser code and historical EBNF
  with different grouping.
- Arithmetic/comparison identities exist as parser enums, block-domain enums,
  string-valued generation IR and runtime branches.
- Command names, aliases, defaults and types are split between catalog entries,
  parser special cases, block definitions and runtime field readers.
- `CommandCatalogKind` mixes UI shape with semantic class. In particular,
  reporter/operator/statement do not reliably define return type or effects.
- Provider identity is currently approximated by capability, plugin owner and
  command namespace rather than separate contracts.
- `liveImplemented` describes adapter state and must not become language truth.
- Projection `rem.*` entries are catalog commands even though frozen V1
  requires projection metadata outside Runtime IR.
- Formatter block prefixes in `EmsScriptFormatter.kt`, editor indentation
  patterns in `EmScriptEditorScreen.kt`, and parser `TokenType` keyword mapping
  still duplicate keyword knowledge. They are documented only and remain
  NOT MIGRATED in M1B-0.
- Syntax highlighting still consumes the existing command catalog plus its
  legacy command-name fallback. Command casing and command migration are not
  part of M1B-0.

## M1A Contracts

| Normative concept | New M1A contract | Current legacy owner | Future migration |
| --- | --- | --- | --- |
| Language version | `LanguageVersion` | No central owner | M1B version handshake |
| Keywords/casing | `KeywordDefinition`, `CasingPolicy` | Parser lexer and formatter remain legacy; highlighter migrated | Parser/formatter conformance slice |
| Operator identity | `OperatorId`, `OperatorDefinition` | `EmscriptBinaryOp`, domain operator enums, strings | Operator adapter before parser migration |
| Operator precedence | `LanguageCoreDefinition.operators` | Parser recursive descent | Focused expression-parser migration |
| Statement termination | `StatementTerminationPolicy` | Lexer/parser and generator | Serializer/parser conformance |
| Core/domain type identity | `TypeDefinition`, `LanguageTypeRef` | Catalog enum, strings and runtime values | Shared type resolver |
| `List<T>` | `LanguageTypeRef.ListOf` | None | Later syntax/IR implementation |
| Void boundary | non-storable `CoreTypes.VOID` | Nullable return-type strings | Command/catalog adapter |
| Command identity/name | `CommandDefinition.id/canonicalName` | `CommandCatalogEntry` | Catalog read adapter |
| Alias lifecycle | `CommandAliasDefinition` | `acceptedAliases` | Legacy importer adapter |
| Semantic class | `SemanticClass` | `CommandCatalogKind` | Per-command classification migration |
| EVENT | `SemanticClass.EVENT` | `CommandCatalogKind.EVENT` | Preserve trigger semantics; do not merge into Action |
| Domain/family/variant | stable ID value objects | category/block type conventions | Registry metadata migration |
| Parameters/defaults | `ParameterDefinition` | `CommandArgument`, parser/runtime special cases | CommandDefinition adapter |
| Return type | non-null `LanguageTypeRef` | nullable string | Query/value migration |
| Side effects | multi-valued `CommandSideEffect` | one coarse enum value | Runtime metadata migration |
| Capability/provider | separate IDs | capability plus plugin owner | Adapter registration migration |
| Lifecycle | `DefinitionLifecycle` | No central owner | Registry validation and docs generation |
| Projection | `ProjectionDefinition` | `rem.*` commands and facets | Projection-AST milestone |
| Read-only lookup | `LanguageDefinitionRegistry` | `CommandCatalog` | Initially bridge, later authority |

## Stable IDs

V1 operator IDs are `add`, `subtract`, `multiply`, `divide`, `modulo`, `less`,
`lessOrEqual`, `greater`, `greaterOrEqual`, `equal`, `notEqual`, `and`, `or`,
`not` and `negate`.

Core type IDs are `core.string`, `core.number`, `core.bool`, `core.any` and
`core.void`. Command, parameter, domain, family, variant, capability, provider
and projection identities use validated serializable value classes. They never
derive from labels, render order, Android objects, process identity or device
identity.

## M1B-1 Operator Convergence

Normative ownership of operator ID, symbol, arity, precedence, associativity
and family belongs exclusively to `EmscriptV1LanguageCore.definition.operators`.
`EmscriptV1OperatorIds` exposes typed stable identities and
`EmscriptV1Operators` resolves definitions by ID or symbol plus arity.

| V1 ID | Symbol | Arity | Rank | Assoc. | Parser | Semantic IR | Block | Runtime | Serializer | Conflict |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- | --- | --- |
| `add` | `+` | binary | 4 | left | `ADD` | `Operate(add)` | `ArithmeticOperator.ADD` | existing `ADD` branch | `+` | none |
| `subtract` | `-` | binary | 4 | left | `SUB` | `Operate(subtract)` | `SUB` | existing `SUB` branch | `-` | unary source minus still lowers to subtraction |
| `multiply` | `*` | binary | 3 | left | `MUL` | `Operate(multiply)` | `MUL` | existing `MUL` branch | `*` | none |
| `divide` | `/` | binary | 3 | left | `DIV` | `Operate(divide)` | `DIV` | existing `DIV` branch | `/` | none |
| `modulo` | `%` | binary | 3 | left | `MOD` | `Operate(modulo)` | `MOD` | existing `MOD` branch | `%` | none |
| `less` | `<` | binary | 5 | left | `LT` | `Compare(less)` | `CompareOperator.LESS` | existing `LT` branch | `<` | parser groups comparison with equality |
| `lessOrEqual` | `<=` | binary | 5 | left | `LTE` | `Compare(lessOrEqual)` | `LESS_OR_EQUAL` | existing `LTE` branch | `<=` | parser groups comparison with equality |
| `greater` | `>` | binary | 5 | left | `GT` | `Compare(greater)` | `GREATER` | existing `GT` branch | `>` | parser groups comparison with equality |
| `greaterOrEqual` | `>=` | binary | 5 | left | `GTE` | `Compare(greaterOrEqual)` | `GREATER_OR_EQUAL` | existing `GTE` branch | `>=` | parser groups comparison with equality |
| `equal` | `==` | binary | 6 | left | `EQ` | `Compare(equal)` | `EQUAL` | existing `EQ` branch | `==` | parser groups equality with comparison |
| `notEqual` | `!=` | binary | 6 | left | `NEQ` | `Compare(notEqual)` | `NOT_EQUAL` | existing `NEQ` branch | `!=` | parser groups equality with comparison |
| `and` | `&&` | binary | 7 | left | `AND` | specialized `And` | specialized block | existing `AND` branch | `&&` | no operator field in specialized IR |
| `or` | `||` | binary | 8 | left | `OR` | specialized `Or` | specialized block | existing `OR` branch | `||` | no operator field in specialized IR |
| `not` | `!` | unary | 2 | right | not implemented | not implemented | not implemented | not implemented | reserved `!` | identity only |
| `negate` | `-` | unary | 2 | right | not implemented | not implemented | not implemented | subtraction fallback | reserved unary `-` | identity only |

### Migration Status

- **IDENTITY MIGRATED:** all implemented parser binary enums, arithmetic and
  comparison block enums, generated IR operator strings, imported operator
  fields and canonical symbol emission map explicitly to stable V1 IDs.
- **IDENTITY MIGRATED:** new blocks store stable IDs. Symbols and legacy values
  including `GREATER_OR_EQUAL`, `GTE`, `MUL`, `>=` and `*` remain readable via
  an explicit compatibility table.
- **PRECEDENCE NOT YET MIGRATED:** `parseComparison` still combines comparison
  and equality. Frozen fixture `v003_precedence` also uses V1 declaration and
  type syntax that the current parser cannot yet run as a focused fixture.
  Activating precedence without that harness would be a partial semantic change.
- **PRECEDENCE NOT YET MIGRATED:** unary `!` is absent and unary minus remains
  lowered to `0 - operand`; M1B-1 does not silently rewrite either behavior.
- Workspace schema/version is unchanged. Legacy operator values round-trip
  losslessly; newly created or imported blocks use stable IDs.

## Projection Boundary

`SemanticClass.PROJECTION` remains expressible in the common semantic
vocabulary, but `CommandDefinition` validation rejects it. Projection metadata
uses `ProjectionDefinition`, which has no provider, capability or runtime
binding and reports `runtimeDispatchable = false` by construction.

## Validation Ownership

`LanguageContractValidator` checks cross-definition invariants without invoking
Android, UI, provider or runtime code. `ImmutableLanguageDefinitionRegistry`
copies input collections, validates them and exposes read-only lookups by stable
ID, canonical name, alias, type and operator symbol/arity.

## Deferred Migration

M1B-1 changes operator identity adapters in `EmscriptParserSlice`,
`EmscriptWorkspaceImporter`, BlockEditor domain/registry, IR generators and
canonical EMScript generation. WorkspaceDocument schema, parser acceptance,
runtime arithmetic, command catalog, Flowchart, formatter and precedence remain
unchanged. The separate M1B-1P slice must add an executable frozen-fixture
harness before changing equality/comparison precedence.

## M1B-2A CommandDefinition Bridge Audit

`LegacyCommandDefinitionBridge` projects every current
`VisualTaskerCommandCatalog` entry into a proposed V1 `CommandDefinition` or an
explicit conflict. It is an analysis-only consumer: the legacy catalog remains
the production owner for parser lookup, blocks, runtime gates and dispatch.

| Area | Legacy owner | Bridge treatment | Migration status |
| --- | --- | --- | --- |
| Command identity and canonical name | `CommandCatalogEntry` | Proposed stable `CommandId` and canonical spelling, with typed naming issues | ANALYZED, NOT MIGRATED |
| Aliases | `acceptedAliases` plus canonical-name lookup | Case-insensitive collision graph with all candidates and current first-match winner | ANALYZED, NOT MIGRATED |
| Parameters and defaults | `CommandArgument` string defaults | Proposed typed `ParameterDefinition`; ambiguous `ANY` and domain-scalar defaults remain issues | ANALYZED, NOT MIGRATED |
| Semantic class and return type | `CommandCatalogKind` plus nullable `returnType` | Proposed V1 semantic class; query-like Void results remain conflicts | ANALYZED, NOT MIGRATED |
| Capability | `CommandCapability` | Proposed typed `CapabilityId` | ANALYZED, NOT MIGRATED |
| Provider | `pluginOwner`, capability and runtime environment | Explicit provider candidate only where repository evidence exists | ANALYZED, NOT MIGRATED |
| Runtime availability | `liveImplemented` plus runtime branches/adapters | Classified as confirmed, provider-dependent, dry-run-only, catalog-only or no-dispatch | ANALYZED, NOT MIGRATED |
| Projection metadata | `rem.*` catalog statements | Reported as `PROJECTION_MODELED_AS_COMMAND`; no runtime-command migration | ANALYZED, NOT MIGRATED |

The generated normative audit artifacts are:

- `EMSCRIPT_V1_COMMAND_BRIDGE_REPORT.md`
- `EMSCRIPT_V1_COMMAND_BRIDGE.csv`

M1B-2A changed no canonical name, parser rule, runtime dispatch, block
definition, provider adapter or serialized workspace.

## M1B-2B Native `action.wait`

`EmscriptV1Commands.WAIT` is the first productive native command definition.
It owns stable ID `action.wait`, canonical name `wait`, the `ms: Number`
parameter, typed default `ContractValue.NumberValue("500")`, Void return,
TRACE side effect, lifecycle and legacy `WAIT` alias.

`WaitCommandCompatibility` projects that definition into the unchanged legacy
shape required by parser lookup, block metadata and runtime gating:

| Contract field | Native V1 | Legacy compatibility |
| --- | --- | --- |
| ID | `action.wait` | `action.wait` |
| Name | `wait` | `wait` |
| Parameter | `ms: core.number` | `ms: DURATION_MS` |
| Default | typed Number `500` | string `"500"` |
| Return | `core.void` | nullable legacy return |
| Side effect | `TRACE` | `TIMING` |
| Capabilities | `capability.core`, `capability.timing` | `CORE`, `TIMING` |
| Dispatch | existing timing capability | existing `simulate`/TIMING binding |

The bridge performs a dual-read parity check and reports any mismatch as
`NATIVE_DEFINITION_PARITY_MISMATCH`; it cannot silently fall back to a second
definition. The legacy catalog still exposes 127 entries, but its wait entry is
now generated from V1 rather than independently maintained. Parser syntax,
including the requirement for an explicit wait argument, IR lowering, block
shape, scheduling, dry run, live runtime and canonical serializer output remain
unchanged. At the M1B-2B boundary, no other command was marked `NATIVE_V1`;
M1B-2C below adds only `feedback.beep`.

## M1B-2C Native `feedback.beep`

`EmscriptV1Commands.BEEP` owns stable ID `feedback.beep`, canonical name
`beep`, the three typed Number parameters `frequency`, `durationMs` and
`volume`, defaults `1000`, `200` and `100`, Void return, DEVICE side effect,
CORE/FEEDBACK capabilities, lifecycle and legacy `BEEP` alias.

| Inventoried field | Existing behavior retained by M1B-2C |
| --- | --- |
| ID / name / alias | `feedback.beep` / `beep` / `BEEP` |
| Parameters | `frequency: FREQUENCY_HZ`, `durationMs: DURATION_MS`, `volume: PERCENT` |
| Defaults | `1000`, `200`, `100` |
| Return / side effect | Void / feedback-device effect |
| Capability / provider | CORE + FEEDBACK / no provider |
| Source syntax | canonical `beep()` through `beep(frequency, durationMs, volume)`; legacy `BEEP` with zero to three numeric arguments |
| Block / IR | `feedback.beep` block and existing `IrStatement.Beep` |
| Dispatch | existing dry-run simulation and `WorkspaceBasicRuntime` feedback/audio path |
| Range behavior | existing generator/runtime clamping remains unchanged |

`NativeCommandLegacyCompatibility` is now the shared, deliberately small
V1-to-legacy projection used by both `action.wait` and `feedback.beep`. A
per-command projection specification retains only data that has no direct V1
representation in the current catalog consumer: legacy argument subtypes,
block/category bindings and the existing runtime gate. The adapter verifies
identity, aliases, ordered parameter signature, typed-default conversion,
return, side effect, capabilities, provider metadata, lifecycle and
dispatchability.

The independent beep catalog declaration has been removed. The projected entry
still exposes the unchanged `FREQUENCY_HZ`, `DURATION_MS` and `PERCENT`
arguments and existing feedback runtime binding. Canonical `beep(...)`, default
`beep()` and legacy `BEEP ...` import through the existing parser/importer,
lower to the existing block and IR statement, and serialize canonically. This
slice changes no parser acceptance, clamp range, scheduling, audio backend,
runtime dispatch or workspace schema. At the M1B-2C boundary the bridge remained
at 127 historical entries and marked exactly `action.wait` and `feedback.beep`
as `NATIVE_V1`; M1B-2D below adds only `event.start`.

## M1B-2D Controlled Native Batch

Every candidate was traced through legacy identity, aliases, parameters,
return/effects, parser/importer, workspace block, IR, serializer and runtime
before migration. Only one candidate is lossless.

| Candidate | Current contract | Parser / IR / serializer truth | Classification | M1B-2D result |
| --- | --- | --- | --- | --- |
| `event.start` | EVENT, `onStart`, aliases `EVENT.ON_START` and `em_on_start`, no parameters, Void, CONTROL_FLOW, CORE, entrypoint binding, event block/flow kind | Importer creates exactly one start block; IR uses it as root; source serializer intentionally omits it | SAFE | Native V1 definition plus generated legacy projection |
| `variable.get` | Historical REPORTER catalog entry | Source reads create `variable.reporter.<id>` with `variableId`/`variableLabel` and serialize as a variable reference | LEGACY EXPRESSION ALIAS | Read compatibility only; canonical write normalizes to the dynamic reporter without changing identity |
| `logic.and` | OPERATOR, `and(left, right): Boolean`, alias `AND` | M1B-1 owns stable operator ID `and`, symbol `&&`, specialized IR/block and canonical operator serialization | CONFLICT | Retained as visual catalog projection; no competing CommandDefinition |
| `logic.or` | OPERATOR, `or(left, right): Boolean`, alias `OR` | M1B-1 owns stable operator ID `or`, symbol `||`, specialized IR/block and canonical operator serialization | CONFLICT | Retained as visual catalog projection; no competing CommandDefinition |
| `literal.number` | REPORTER-like catalog entry `number(value): Number` | Grammar number token becomes `NumberLiteral` / `IrExpression.LiteralNumber` and serializes as a numeric literal | CONFLICT | Retained as visual catalog projection; no callable `number(...)` command invented |

`EmscriptV1Commands.EVENT_START` now owns stable ID `event.start`, canonical
name `onStart`, lifecycle and aliases. `NativeCommandLegacyCompatibility`
projects it into the unchanged EVENT catalog shape. This is a source-of-truth
migration only: no explicit `onStart` syntax is introduced, no parser branch is
added, and no block, IR, runtime or workspace schema changes.

The bridge still inventories 127 historical entries and now marks exactly
`event.start`, `action.wait` and `feedback.beep` as `NATIVE_V1`. All SAFE
candidates in this bounded batch were migrated; the remaining four are
documented contract decisions, not deferred implementation work.

## M1B-3A Naming Normalization

M1B-3A separates stable command identity, canonical V1 source spelling and
legacy import spelling. `EmscriptV1NamingNormalizations` owns the three bounded
normalization contracts; it is not a second command registry and does not
change parameters, return types, effects, dispatch, block types or flow kinds.

| Stable ID | Legacy spelling | Canonical V1 | Parameters | Return / effect | Runtime | Alias lifecycle | Naming | Overall |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `file.writeText` | `File.writeText` | `file.writeText` | `path: TEXT`, `text: TEXT`; defaults `""`, `""` | Void / VARIABLE_WRITE | confirmed core file write | readable V1.x; remove no earlier than V2 | RESOLVED | CLEAN legacy catalog entry; not native |
| `clipboard.set` | `Clipboard.set` | `clipboard.set` | `text: TEXT`; default `""` | Void / VARIABLE_WRITE | confirmed core clipboard write | readable V1.x; remove no earlier than V2 | RESOLVED | CLEAN legacy catalog entry; not native |
| `cache.clear` | `Cache.clear` | `cache.clear` | none | Void / VARIABLE_WRITE | confirmed core cache clear | readable V1.x; remove no earlier than V2 | RESOLVED | CLEAN legacy catalog entry; not native |

All three retain their stable IDs and generic block types
`emscript.command.<stable-id>`. Parser lookup accepts canonical spelling and
the explicit legacy alias through `CommandCatalog.findByAcceptedName`.
Importer storage, IRGraph `commandId`, and Flowchart block projection retain
the stable identity. `EmscriptGenerator` writes only the canonical spelling.
Canonical and legacy source therefore converge after one roundtrip and remain
stable on the second roundtrip and Workspace serialization.

The report now exposes `naming_status` separately from bridge status and
migration class. A resolved name therefore cannot imply `NATIVE_V1`.
`action.findTemplate` remains an independently confirmed naming proposal
(`templateFind`, legacy alias `findTemplate`) but is not changed here: domain
default typing and its missing query return contract keep it in
`D_QUERY_RETURN`. Resolving those dimensions belongs to a later decision.

## M1B-3B Type-Conflict Decisions

`EmscriptV1TypeConflictDecisions` records the treatment of the eight bounded
cases without creating another command registry. `TypeStatus` is independent
of naming, bridge status and native migration status. All eight contracts are
decided; M1B-3C implements `debug.log` expression transport, M1B-3D adds typed
`variable.set` assignment and M1B-3E enforces the String/String datastore write
contract. M1B-3F resolves VariableReference and Bool-literal projection ownership,
and M1B-3G converges `feedback.vibrate` on the normative variadic signature.

| Stable ID | Normative V1 type/model | Classification | Implemented | Remaining blocker |
| --- | --- | --- | --- | --- |
| `system.datastorePut` | `datastorePut(key: String, value: String)` | TYPE_MAPPING | yes | Both arguments are semantic String ValueInputs. Parser expressions survive Workspace/IR/source roundtrip; shared type compatibility rejects Number, Bool and Any before apply. Native migration remains separate. |
| `debug.log` | `log(value: Any): Void`, rendered deterministically | TYPE_MAPPING | yes | `value` is a semantic Any ValueInput. Existing literal, VariableReference and operator reporters survive Workspace/Block/IR/source roundtrips; legacy `message` remains read-only fallback. Native migration is still separate. |
| `feedback.vibrate` | `vibrate(patternMs: Number...): Void`; one value is a duration, multiple values alternate delay/vibration phases | SIGNATURE_DECISION | yes | Required variadic Number input, no V1 default and no repeat parameter. Legacy scalar field remains read-compatible; dynamic visual mutation is deferred. |
| `variable.set` | assigned expression must be compatible with the variable's declared or inferred type | TYPECHECKER_RULE | yes | `LanguageTypeCompatibility` and `WorkspaceValueTypeSystem` are shared by importer, BlockEditor snapping, Workspace validation and IR generation. Native migration remains separate. |
| `variable.get` | VariableReference expression typed from its declaration | EXPRESSION_MODEL | yes | V1.x legacy alias; read normalizes to `variable.reporter.<variableId>`, canonical source writes the variable reference. |
| `logic.boolean` | grammar/AST/IR Bool literal | EXPRESSION_MODEL | yes | V1.x legacy visual alias; read normalizes to `literal.boolean`. |
| `literal.boolean` | canonical visual projection of the grammar/AST/IR Bool literal | EXPRESSION_MODEL | yes | Projection only; canonical source writes `true` or `false`, never `boolean(...)`. |
| `input.touch` | structural migration into typed touch primitives, PointerPath, MultiPath or GestureSequence | STRUCTURAL_MIGRATION | no | M1B-3H-A classifies the three repository payload forms as PARTIAL and preserves raw text. Timing, pointer identity, coordinate space, multi-pointer structure and complete gesture boundaries remain unproven, so no typed migration is allowed. |

The earlier `datastorePut(Any)` hypothesis is rejected by repository evidence:
`WorkspaceBasicRuntimeEnvironment`, the in-memory store and persistence all use
`String`. Conversely, `log(Any)` now uses the existing expression hierarchy,
reporter connection and `IrExpression`; deterministic text conversion occurs
only after evaluation.

The bridge remains 127 entries and `NATIVE_V1` remains three. `debug.log`,
`variable.set`, `system.datastorePut`, `variable.get`, `logic.boolean`,
`literal.boolean` and `feedback.vibrate` are resolved for their bounded M1B
slices but intentionally not native-migrated. Only `input.touch` remains in
`C_TYPE_CONFLICT`.

## M1B-3H-A Legacy input.touch Structural Classification

`LegacyTouchStructuralClassifier` is the single read-only analysis boundary for
legacy `input.touch` arguments. The general EMScript parser continues to parse
syntax and transport the raw argument. Workspace field `args`, generic IR
`CommandCall.arguments` and the existing source generator remain authoritative
and unchanged.

The classifier records exact raw payload, historical alias, structural tokens,
coordinate-like pairs, atomic `down`/`move`/`up` evidence, timing evidence,
pointer identity evidence, multi-pointer evidence, sequence-boundary evidence,
unknown tokens, diagnostics and migration readiness. It never manufactures a
target type or missing values.

Repository-backed results:

| Payload | Classification | Proven | Not proven |
| --- | --- | --- | --- |
| `[540, 1100]` | PARTIAL | one coordinate-like pair | tap, down/up, timing, pointer ID, coordinate space |
| `["down", 120, 240, "up"]` | PARTIAL | down, up, one coordinate-like pair | timing, pointer ID, move, coordinate space, complete gesture boundary |
| `"down(10,20);move(20,30);up(20,30)"` | PARTIAL | down, move, up, three coordinate-like pairs | timing, pointer ID, coordinate space, full-gesture sequence boundaries |

No current fixture is `LOSSLESS`; malformed, empty or wholly unknown payloads
are `UNKNOWN` and `PRESERVE_LEGACY`. Recognized partial forms require structural
migration. The bridge therefore remains `C_TYPE_CONFLICT / STRUCTURAL_MIGRATION`,
the catalog remains 127 entries, and `NATIVE_V1` remains three.

Formal closeout is `NORMALIZABLE / C_TYPE_CONFLICT /
STRUCTURAL_MIGRATION / LEGACY_PRESERVE`. Migration readiness is
`BLOCKED_BY_LEGACY_SEMANTICS`; M1B-3H-B is deferred rather than silently
inventing gesture data.

## M1B-3I Query Return Contract Audit

The current bridge derives 23 `D_QUERY_RETURN` entries from live repository
state. `QueryReturnContractAudit` classifies each stable ID exactly once and
validates the matrix against that derived inventory.

| Classification | Count |
| --- | ---: |
| A_RETURN_TYPE_ONLY | 0 |
| B_EXPRESSION_PROJECTION_GAP | 7 |
| C_RUNTIME_RESULT_GAP | 5 |
| D_TYPE_MODEL_GAP | 1 |
| E_PROVIDER_DEPENDENT_QUERY | 10 |
| F_NOT_ACTUALLY_QUERY | 0 |
| G_AMBIGUOUS | 0 |

All 23 are currently statement blocks without output and become statement-only
Workspace/IR/Flow projections. No return type is added by this slice. The
machine-readable decision table is
`EMSCRIPT_V1_QUERY_RETURN_DECISIONS.csv`; the evidence report is
`EMSCRIPT_V1_QUERY_RETURN_AUDIT.md`.

The M1B-3J migration target was `clipboard.get`, `system.info` and `system.env`:
core-owned, non-null String results with no new structured type or provider
contract. The completed migration is recorded below.

## M1B-3J Low-Risk Query Return Convergence

`clipboard.get`, `system.info` and `system.env` now declare non-null `String`
results. A command is expression-capable when its authoritative return type is
not `Void`; parser, generated reporter block, Workspace value connection,
generic `IrExpression.CommandCall`, serializer and runtime evaluation all use
that rule rather than command-name-specific projection code.

The Workspace representation remains an ordinary generated command block with
stable command metadata, typed output and semantic argument inputs. Runtime
evaluation delegates through `WorkspaceCommandExpressionEvaluator`; the basic
runtime maps the three stable IDs to their already-existing environment
handlers and returns `EmscriptValue.StringValue`. Neither
`LiveExecutionOutcome` nor `RuntimeAdapterResult` was widened.

The D_QUERY_RETURN inventory is now 20. The migrated IDs remain explicit in
`QueryReturnContractAudit.MIGRATED_M1B_3J`; the other classifications and the
127-entry bridge inventory remain unchanged. `NATIVE_V1` remains 3 because the
three commands still require canonical naming normalization before native V1
ownership. Nullable, provider-owned and structured-result queries are deferred.

## M1B-3K Nullable / Absent Query Semantics Freeze

`file.readText` and `system.datastoreGet` are normatively frozen as `String?`
queries, but remain unmigrated statement commands. Empty String is a successful
value; missing regular file or missing datastore key is absence; invalid path,
permission, I/O and store failures belong to the runtime diagnostic/failure
channel. No default parameter, `Any`, `Option` or `Result` model is introduced.

The current host has two documented collisions to repair in a later vertical
slice: invalid file paths currently collapse into null, and datastore load
failure currently collapses into an empty map. Executable audit invariants live in
`NullableQuerySemanticsAudit`; the normative rationale is recorded in
`EMSCRIPT_V1_NULLABLE_QUERY_SEMANTICS.md`.

## M1B-3L Nullable Type Infrastructure

`LanguageTypeRef.Nullable(baseType)` models nullability orthogonally, without
nullable primitive duplicates. Source type suffixes `String?`, `Number?`,
`Bool?`, `Any?` and domain `T?` parse through the shared type resolver. Typed
LET declarations preserve an explicit type in Workspace metadata, variable
definitions, generated IR and canonical source; SET uses the same generic
assignment compatibility.

Assignability is `T -> T?` and `T? -> T?`, but never implicit `T? -> T`.
Nullable values do not flow into non-null `Any`; both nullable and non-null
values flow into `Any?`. WorkspaceReducer, SnapEngine and Validator use the
same `LanguageTypeCompatibility` contract. The serializer stores exact type
strings and `IrExpression.CommandCall.returnType` remains a generic string
transport capable of retaining `T?`.

`EmscriptValue.NullValue` represents ABSENT. It is accepted only against a
nullable expected runtime type and is never coerced to `0`, `false`, `""` or
`Any`. Contract violations remain failures with
`NULLABLE_VALUE_IN_NONNULL_CONTEXT`; static validation additionally exposes
`NULLABLE_TO_NONNULL_ASSIGNMENT` and
`NULLABLE_ARGUMENT_TO_NONNULL_PARAMETER`. Bool? is not a control condition.

`NULL_LITERAL_DECISION_REQUIRED`: V1 still has no source-level `null` literal.
`NULLABLE_GENERIC_COMPOSITION_DEFERRED`: the TypeRef model can distinguish
`Nullable(ListOf(T))` from `ListOf(Nullable(T))`, but generic source parsing and
canonical syntax for those compositions are not activated in this slice.
Nullable function parameter/return contracts are representable by
`ParameterDefinition` and `CommandDefinition`; function runtime remains outside
this slice. Operators, equality, unwrap and absence tests gain no new semantics.

No production query is migrated. `file.readText` and
`system.datastoreGet` remain statement-shaped; D_QUERY_RETURN remains 20, the
bridge remains 127 and NATIVE_V1 remains 3.

## M1B-3E datastorePut String Contract

`system.datastorePut` now follows the existing generic semantic-input path:

`CommandArgument(TEXT + acceptedTypes=Text)` -> parser expression -> matching
Workspace ValueInput -> `WorkspaceValueTypeSystem` ->
`LanguageTypeCompatibility` -> IR expression -> canonical EMScript.

No command-ID-specific typechecker exists. The generic mechanism is available
to commands that explicitly opt into semantic ValueInputs with
`acceptedTypes`; no other command is automatically migrated by this slice.
`datastoreGet` remains unchanged and confirms the same String-backed datastore
model. Runtime and persistence are unchanged.

## M1B-3F Expression Model Cleanup

`VariableReference` and Bool literals belong to Grammar/AST/IR, not to the
language-command namespace. The compatibility catalog marks `variable.get`
and `logic.boolean` as `LEGACY_EXPRESSION_ALIAS`, and `literal.boolean` as
`CANONICAL_EXPRESSION_PROJECTION`.

Workspace normalization is deterministic and idempotent:

- `variable.get` with a stable `variableId` becomes
  `variable.reporter.<variableId>` while retaining block ID, connection IDs,
  parent connection, label and resolved variable type.
- `logic.boolean` becomes `literal.boolean` while retaining block ID, Boolean
  field and output connection.
- Unresolved historical variable blocks are retained instead of guessing an
  identity from a display label.

Canonical source emits variable references and `true`/`false`. The parser does
not treat `get(...)`, `boolean(...)`, `variable.get(...)`, `logic.boolean(...)`
or `literal.boolean(...)` as language commands. The bridge inventory remains
127 heterogeneous compatibility entries, not 127 EMScript V1 commands.
`NATIVE_V1` remains three.

## M1B-3G feedback.vibrate Signature Convergence

The canonical contract is:

`vibrate(patternMs: Number...): Void`

`required=true` plus `variadic=true` uses the existing generic parameter model
to enforce `minCount=1` without a command-specific parser rule. There is no
maximum argument count, no language default, and no repeat argument. Literal,
VariableReference and arithmetic Number expressions use the same lossless
command-expression transport as other typed inputs.

One value is a one-shot duration. Multiple values retain order as alternating
delay/vibration phases and use the existing non-repeating Android waveform
dispatch. Even and odd pattern lengths are valid. Zero and negative values are
preserved through source, Workspace, IR and DryRun; the existing Android adapter
removes non-positive phases before dispatch and performs no vibration when no
positive phase remains.

An explicit legacy scalar, including `80`, remains readable as one explicit
argument. The existing empty-field fallback to `80` is classified strictly as
`LEGACY READ NORMALIZATION`, not as part of the V1 CommandDefinition:
source `vibrate()` is invalid and is never normalized to `vibrate(80)`. The
current block keeps its scalar legacy fallback while imported canonical source
can project dynamic ValueInputs; a dedicated visual variadic mutator is outside
this slice.

## M1B-3C Expression Transport Inventory

| Source | Parser AST | Workspace/Block | Semantic IR | Canonical source | Runtime |
| --- | --- | --- | --- | --- | --- |
| `log("hello")` | `StringLiteral` | `literal.string` -> `debug.log:value` | `LiteralString` | `log("hello")` | evaluated String -> display text |
| `log(42)` | `NumberLiteral` | `literal.number` -> `debug.log:value` | `LiteralNumber` | `log(42)` | evaluated Number -> display text |
| `log(3.14)` | `NumberLiteral` | `literal.number` -> `debug.log:value` | `LiteralNumber` | `log(3.14)` | evaluated Number -> display text |
| `log(true)` | `BooleanLiteral` | `literal.boolean` -> `debug.log:value` | `LiteralBoolean` | `log(true)` | evaluated Bool -> display text |
| `log(variable)` | `VariableRef` | `variable.reporter.<id>` -> `debug.log:value`; ID and label remain separate | `GetVariable(id)` | `log(variable)` | referenced value -> display text |
| `log(1 + 2)` | `Binary(ADD)` | `logic.operate(add)` -> `debug.log:value` | `Operate(add, ...)` | `log((1 + 2))` | evaluate operator, then display text |

The generic importer helper resolves expression arguments from the command
parameter contract and matching ValueInput names. It contains no
command-name/ID branch. Fields that are not semantic ValueInputs remain
unchanged.

## M1B-3M Nullable Core Query Convergence

`file.readText` and `system.datastoreGet` are generic `String?` reporters.
They use the existing CommandCall expression, Workspace ValueOutput,
`IrExpression.CommandCall`, nullable assignability and canonical serializer.

The runtime contract is explicit:

- existing content or stored value, including `""`, becomes `StringValue`;
- missing regular file or missing key becomes `NullValue`;
- invalid paths, permission/I/O failures and datastore-load failures use the
  structured runtime failure channel.

The Android file adapter no longer treats an invalid path as absence. The
datastore loader retains load failure instead of replacing it with a successful
empty store. Logs expose only `VALUE(length=n)`, `ABSENT` or a diagnostic and
do not collapse an empty value into absence.

No adjacent query is migrated. `D_QUERY_RETURN` changes from 20 to 18, bridge
count remains 127, `NATIVE_V1` remains 3, and the M1B-3J String trio remains
non-null.

## M1B-3N Remaining Query Return Reclassification

The remaining 18 `D_QUERY_RETURN` entries are classified from current runtime
and provider evidence rather than by command-name heuristics. No entry is yet a
low-risk scalar migration:

- 2 require first-class structured results (`ImageMatch?`, `Region?`);
- 5 have no authoritative runtime result;
- 5 compute provider values but flatten them into status/message adapters;
- 6 collide sentinels, provider states, not-found or failures.

The machine-readable decision matrix records aliases, parameters, current and
proposed types, provider, first loss point, transport readiness, consumption
readiness and repository evidence. Nullable scalar transport exists, but none
of the remaining nullable candidates is consumption-ready because V1 still has
no presence test, extraction or fallback operation. No syntax is selected by
this audit.

The smallest recommended follow-up is a contract-only slice for
`vision.templateCompare`: distinguish VALUE, NOT_FOUND and FAILURE before any
`Number?` migration. The command catalog, runtime, providers, query count,
bridge count and native-command population remain unchanged in M1B-3N.

## M1B-3O Template Compare Result Semantics

Repository evidence establishes `vision.templateCompare` as a concrete
region-to-region comparison, not a search. A completed operation returns the
inclusive `0.0..1.0` normalized mean absolute grayscale similarity score;
higher is better. The command has no match-acceptance threshold. Mask
preprocessing has an internal luminance cutoff, but a low final score remains
a valid value.

The named template and both image regions are required inputs. Missing or
undecodable evidence means the comparison could not execute and therefore uses
the structured runtime failure channel. No state becomes `NullValue`, and no
numeric sentinel is normative. The frozen V1 return contract is non-null
`Number`; the existing Kotlin `Float?` only collapses several current failure
origins.

M1B-3O reclassifies only this audit entry as
`B_NONNULL_SCALAR_READY`. It does not migrate the command: catalog, parser,
Workspace, IR, serializer and runtime remain unchanged, so `D_QUERY_RETURN`
stays 18. M1B-3P is the bounded convergence slice.

## M1B-3P Template Compare Convergence

`vision.templateCompare` is now a non-null `Number` reporter throughout the
catalog, Workspace, generic IR expression and canonical serializer. The parser
accepts `region(x, y, width, height)` as a typed Region expression; its
Workspace projection uses a dedicated Region literal block while IR retains a
generic `core.region` command expression. This introduces no query-specific IR
node and no new runtime value type.

The Android vision adapter returns only finite scores in the inclusive
`0.0..1.0` range. Missing templates, unavailable images, unusable regions and
comparison failures use `TEMPLATE_NOT_FOUND`,
`TEMPLATE_IMAGE_UNAVAILABLE`, `TEMPLATE_REGION_UNAVAILABLE` and
`TEMPLATE_COMPARE_FAILED` respectively. Low scores, including `0.0`, remain
valid values. No threshold, `NullValue` or sentinel is introduced.

Only `vision.templateCompare` leaves `D_QUERY_RETURN` in this slice. The
remaining count is 17; bridge inventory remains 127 and `NATIVE_V1` remains 3.

## M1B-3Q ChromeTab isSupported Result Semantics

Repository evidence defines `chromeTab.isSupported` as Android Custom Tabs
service discovery. `CustomChromeTabRegistration.inspect` queries
`ACTION_CUSTOM_TABS_CONNECTION`; at least one resolvable service produces
`true`, while a completed query with no matching service produces the valid
value `false`. Chrome is preferred only when selecting the reported package.
Provider configuration and active Custom Tabs sessions do not participate.

The proposed V1 type is non-null `Bool`. There is no legitimate absence or
domain-level unknown state. Missing host adapter and technical service
resolution failure require `CHROME_TAB_ADAPTER_UNAVAILABLE` and
`CHROME_TAB_RESOLUTION_FAILED` respectively.

The Bool currently becomes `RuntimeAdapterResult.success` and message text in
`WorkspaceScreen`, then a status event in `WorkspaceBasicRuntime`. This is the
first loss point: the consumer cannot distinguish a valid false capability
answer from adapter failure and receives no typed payload. M1B-3Q documents
that minimal transport gap but changes no production path. `D_QUERY_RETURN`
remains 17, bridge inventory remains 127 and `NATIVE_V1` remains 3.

## M1B-3R Typed Provider Result Transport

`RuntimeAdapterResult` is extended additively with an optional `EmscriptValue`
payload and an optional structured diagnostic code. Existing `success`,
`message`, `warning` construction and Void callers remain source-compatible.
Payload absence means a successful Void operation when `success=true`; an
explicit `EmscriptValue.NullValue` remains a present nullable query result.
Failed results cannot carry a value.

Only `chromeTab.isSupported` uses the new typed provider path. The Android host
maps a completed `CustomChromeTabRegistration.inspect` to successful
`BooleanValue(true)` or successful `BooleanValue(false)`. A missing host adapter
uses `CHROME_TAB_ADAPTER_UNAVAILABLE`; a PackageManager/service-resolution
exception uses `CHROME_TAB_RESOLUTION_FAILED`. `message` remains trace text and
is never parsed for the result.

The command catalog now exposes canonical `chromeTab.isSupported(): Bool` with
legacy alias `ChromeTab.isSupported`. Generic Workspace reporter projection,
`IrExpression.CommandCall`, LET/SET/IF evaluation and canonical roundtrip are
used without a ChromeTab-specific value or expression class. Other provider
queries are unchanged. `D_QUERY_RETURN` is 16, bridge inventory is 127 and
`NATIVE_V1` is 3.

## M1B-3S Provider Bool Semantics Freeze

M1B-3S freezes, but does not migrate, `tasker.isInstalled`,
`shizuku.isInstalled`, `shizuku.isAvailable` and `termux.isInstalled`. All four
have non-null `Bool` domain semantics and no legitimate absence. A technically
failed inspection is a structured failure rather than `false` or `NullValue`.

The three `isInstalled` commands ask only whether their explicitly declared
package IDs are visible to PackageManager. Permission, launchability, service
readiness and command execution are separate facts. `shizuku.isAvailable` is
the distinct composite `installed && permissionGranted && binderAlive`; it is
not proof that a shell command executed successfully.

All four value sources exist, and the 3R typed transport plus generic
Workspace/IR expression infrastructure can carry their values. They remain
blocked from migration because current provider helpers collapse technical
PackageManager, Binder or permission-inspection exceptions to `false`.
`D_QUERY_RETURN` remains 16, bridge inventory remains 127 and `NATIVE_V1`
remains 3.

## M1B-3T Installed Provider Bool Convergence

M1B-3T migrates exactly `tasker.isInstalled`, `shizuku.isInstalled` and
`termux.isInstalled` to non-null generic Bool reporters. Their old uppercase
namespace spellings remain V1 import aliases. PackageManager
`NameNotFoundException` is a successful `false`; every other technical package
inspection failure is preserved as a structured provider-specific diagnostic.

All three commands reuse `RuntimeAdapterResult` with
`EmscriptValue.BooleanValue`, generic Workspace reporters and
`IrExpression.CommandCall`. LET, SET, IF, nullable widening and canonical
roundtrip are covered without query-specific models. `shizuku.isAvailable`
remains unchanged for a separate composite Binder/permission slice.

`D_QUERY_RETURN` is 13, bridge inventory remains 127 and `NATIVE_V1` remains
3. The detailed contract is
`EMSCRIPT_V1_INSTALLED_PROVIDER_BOOL_CONVERGENCE.md`.

## M1B-3U Shizuku Availability Convergence

M1B-3U migrates `shizuku.isAvailable(): Bool` through the existing generic
reporter, `IrExpression.CommandCall`, `RuntimeAdapterResult` and
`EmscriptValue.BooleanValue` path. Its provider-local inspection preserves
`NOT_INSTALLED`, `PERMISSION_NOT_GRANTED`, `BINDER_NOT_ALIVE` and `AVAILABLE`
before projecting successful results to Bool.

Package, permission and Binder inspection failures remain distinct structured
diagnostics and cannot become `false`. The uppercase namespace spelling remains
an import alias. `D_QUERY_RETURN` is 12, bridge inventory remains 127 and
`NATIVE_V1` remains 3. The detailed contract is
`EMSCRIPT_V1_SHIZUKU_AVAILABILITY_CONVERGENCE.md`.

## M1B-3W Scalar Provider Query Convergence

M1B-3W migrates `tasker.isEnabled(): Bool`,
`tasker.getVariable(...): String?`, `shizuku.getUid(): Number?`,
`termux.get(...): String?`, `scrcpy.isRunning(): Bool` and
`scrcpy.get(...): String?`. All six use the generic reporter,
`IrExpression.CommandCall`, `RuntimeAdapterResult` and existing scalar
`EmscriptValue` variants; no provider-specific expression type is introduced.

Provider-local inspections preserve VALUE, ABSENT, legitimate false, empty
String and zero results, and technical FAILURE as separate states. Tasker
enabled state is read independently from installation, Shizuku UID no longer
uses a negative sentinel, Termux keys are frozen, and scrcpy session state is
separate from USB/ADB readiness. Legacy namespace spellings remain read
aliases while serialization writes canonical lower-case provider names.

`D_QUERY_RETURN` is 6, bridge inventory remains 127 and `NATIVE_V1` remains 3.
The detailed contract is
`EMSCRIPT_V1_SCALAR_PROVIDER_QUERY_CONVERGENCE.md`.

## M1B-3X Tasker Collection Query Convergence

M1B-3X migrates
`tasker.getVariables(pattern?): List<TaskerVariable>` to the generic reporter
and `IrExpression.CommandCall` path. The command reuses the Tasker variable
snapshot established by M1B-3W and maps each provider entry to the immutable
EMScript domain value `TaskerVariableValue(name, value)`.

The generic runtime `ListValue` retains its element type and ordered values.
A successful query always returns a list, including an empty list when no
variable matches. Adapter, installation, snapshot and invalid-result failures
remain structured diagnostics and never become `NullValue`, `noValue` or an
empty-list sentinel.

The static return type is exactly `List<TaskerVariable>` and is incompatible
with `String`, `Number` and `Bool` without an explicit conversion. The former
`Tasker.getVariables` spelling remains an import alias; serialization writes
the canonical lower-case command name.

`D_QUERY_RETURN` is 5, bridge inventory remains 127 and `NATIVE_V1` remains 3.
The detailed contract is
`EMSCRIPT_V1_TASKER_COLLECTION_QUERY_CONVERGENCE.md`.
