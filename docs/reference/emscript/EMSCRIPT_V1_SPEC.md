# EMScript v1 Language Specification

Freeze date: 2026-09-21
Language version: 1.0
Status: Normative target contract

This specification defines the EMScript v1 language contract. It does not
claim that the current parser, editors, registry or runtime implement every
rule. Implementation status is tracked separately in the conformance audit.

## 1. Scope And Version

EMScript is the textual projection of the same workflow semantics represented
by the BlockEditor and FlowEditor. Version 1 defines core syntax, values,
control flow, functions, geometry, gestures, projection metadata and the
boundary to provider commands.

The language version and CommandDefinition registry version are distinct.
Changing provider availability does not change the language grammar.

## 2. Normative Terminology

The words MUST, MUST NOT, SHOULD, SHOULD NOT and MAY are normative.

- **Canonical** means the only form emitted by a V1 serializer.
- **Legacy** means readable by the V1 importer but never newly emitted.
- **Value** means data without a runtime side effect.
- **Action** means an operation that can change runtime or environment state.
- **Projection** means persistent editor metadata with no runtime effect.

## 3. Lexical Grammar

The normative syntax is defined by
[`EMSCRIPT_V1_GRAMMAR.ebnf`](EMSCRIPT_V1_GRAMMAR.ebnf). Keywords are read
case-insensitively and serialized uppercase. Canonical command names use
lowerCamelCase, type names PascalCase and provider namespaces their registered
canonical spelling.

Whitespace separates tokens. Newlines have statement meaning except inside
open parentheses or brackets. Braces are reserved and are not canonical V1
control delimiters.

## 4. Identifiers

An identifier starts with an ASCII letter or underscore and continues with
ASCII letters, digits or underscores. Keywords cannot be unquoted identifiers.
Local variable identity is case-sensitive.

Qualified names use dots, for example `tasker.run` or `animate.path`. Command
resolution uses the canonical name or an explicitly registered legacy alias;
arbitrary case folding does not create a new command.

## 5. Variable Syntax

Local variables MUST use the section-sign prefix:

```emscript
LET §count = 0
SET §count = §count + 1
```

`%name` is reserved for Tasker/provider values and MUST NOT enter the local
EMScript symbol table. Bare local variable names are legacy input only.

## 6. Literals

V1 literals are:

- double-quoted `String` values;
- decimal `Number` values;
- `true` and `false` Bool values;
- list literals such as `[1, 2, 3]`.

V1 has no source-level `null` or `undefined` literal. A nullable query result may
carry the distinct runtime state **absent**, but absence is never represented by
an empty value, `false`, `0`, `Any` or an error string. Internal error sentinels
remain runtime implementation details and cannot be stored by scripts.

## 7. Escapes

Strings support exactly these escapes:

| Escape | Meaning |
| --- | --- |
| `\\` | Backslash |
| `\"` | Double quote |
| `\n` | Newline |
| `\r` | Carriage return |
| `\t` | Tab |
| `\uXXXX` | Unicode code unit using four hexadecimal digits |

An unknown or malformed escape MUST produce `INVALID_ESCAPE`. The original
source spelling and range MUST remain available to diagnostics; implementations
MUST NOT silently discard or rewrite the backslash.

## 8. Comments

Canonical comments are `//` line comments and `/* ... */` block comments.
Block comments do not nest. `REM` line comments are legacy input. A `rem.*`
qualified call is a legacy projection construct, not a comment.

Comments do not enter runtime IR. A concrete-syntax implementation MAY preserve
them for roundtrip editing.

## 9. Statement Termination

Newline is the canonical statement terminator. A semicolon is accepted only as
a compatibility terminator. Canonical serialization removes optional
semicolons. Newline inside open `()` or `[]` does not terminate a statement.

## 10. Type System

Core types are `String`, `Number`, `Bool`, `Any` and `List<T>`. `T?` denotes a
nullable value of exactly `T` and is not shorthand for `Any`, `Option<T>` or
`Result<T>`. `Void` is only a function/action return type and is not a storable
value.

`T` is assignable to `T?`; `T?` is not assignable to `T` without an explicit
absence-handling operation. LET inference and SET compatibility retain the
nullable qualifier. Equality may compare compatible nullable values; other
operators and command parameters retain their declared non-null requirements.
Non-null values are assignable to `Any`; nullable values are not assignable to
non-null `Any`. Both are assignable to `Any?`, so the `Any` base type never
erases the nullable qualifier.
The concrete absence-test/unwrap surface must be frozen before the first
nullable query is migrated. This specification rule does not itself make an
existing command expression-capable.

Registered domain types include:

- geometry: `Point`, `Size`, `Bounds`, `Region`, `Line`, `PathSegment`, `Path`;
- shape: `Circle`, `Rectangle`, `Triangle`, `Polygon`, `Shape`;
- gesture: `PointerPath`, `MultiPath`, `Gesture`, `GestureSequence`;
- runtime: `OverlayHandle` and provider-specific typed handles;
- perception: typed `Element`, `Match`, `SearchResult`, `Image` and related
  registry-owned values.

There is no implicit String/Number/Bool coercion and no JavaScript-style
truthiness. IF and WHILE conditions MUST be Bool.

The type model can structurally distinguish a nullable list from a list of
nullable elements. Source-level generic nullable composition remains deferred
(`NULLABLE_GENERIC_COMPOSITION_DEFERRED`) until its grammar and operator
semantics are frozen. V1 has no source `null` literal
(`NULL_LITERAL_DECISION_REQUIRED`).

## 11. List<T>

`List<T>` is the only core V1 collection. Map, Set, Tuple and Object are not
core V1 types. Lists are mutable and support at least:

```emscript
list.add(§items, §value)
list.remove(§items, §index)
list.set(§items, §index, §value)
```

Indexing uses `value[index]`. An invalid index MUST produce a structured
validation or runtime error. Implementations MUST NOT clamp it, return null or
silently choose a fallback. Concrete signatures and return types are owned by
CommandDefinition; mutation cannot violate the list element type.

## 12. Expressions

Expressions include literals, local variable reads, list literals, index
access, calls, grouping and unary/binary operators. Value constructors and
queries may appear in expressions. Actions and Void-returning calls may not.

Unary `!` requires Bool. Unary `-` requires Number. Arithmetic requires Number
unless a future version explicitly defines another typed overload.

## 13. Operator Precedence

From strongest to weakest:

1. grouping and postfix index access;
2. unary `!` and unary `-`;
3. `*`, `/`, `%`;
4. `+`, `-`;
5. `<`, `<=`, `>`, `>=`;
6. `==`, `!=`;
7. `&&`;
8. `||`.

Equality and ordered comparison are distinct precedence levels.

## 14. LET And SET

`LET` declares a variable in the current lexical scope. A second declaration
of the same name in that scope produces `VARIABLE_ALREADY_DECLARED`. `SET`
mutates an existing visible variable. Mutation of an unknown name produces
`UNKNOWN_VARIABLE`; it MUST NOT create the variable implicitly.

An optional type annotation follows the variable:

```emscript
LET §timeout: Number = 1500
```

## 15. Calls

Calls use a generic grammar:

```emscript
qualifiedName(argument, name = expression)
```

Semantic resolution decides whether the target is a value constructor, user
function, query, action or provider command. CommandDefinition owns canonical
name, aliases, parameters, defaults, return type, side effects, capability and
provider requirements.

## 16. Positional Arguments

Positional arguments are matched left-to-right against the declared parameter
list and MUST precede named arguments. Too many, missing or incompatible
arguments produce `INVALID_ARGUMENT` or `TYPE_MISMATCH`.

## 17. Named Arguments

Named arguments use `name = expression`. A parameter may not be supplied more
than once, positionally and/or by name. Unknown and duplicate names are
diagnostics. Named argument order does not change semantics.

## 18. Default Arguments

Only CommandDefinition or a user-function declaration may define defaults.
Parser special cases and runtime adapters MUST NOT invent competing defaults.
Canonical serialization MAY omit an argument whose value equals its declared
default when omission is semantically identical.

## 19. IF / ELSEIF / ELSE

Canonical conditional syntax is:

```emscript
IF §score >= 100
    clickText("Winner")
ELSEIF §score >= 50
    clickText("Continue")
ELSE
    clickText("Retry")
END IF
```

There is one IF, zero or more ELSEIF branches, at most one ELSE and exactly one
`END IF`. `ELSE IF` is a V1.x legacy alias. Empty branches are syntactically
valid but MAY receive `EMPTY_BRANCH` warnings.

## 20. REPEAT

`REPEAT expression ... END REPEAT` evaluates a numeric repeat count once on
entry. Negative or non-integral behavior MUST be rejected by validation unless
the CommandDefinition later introduces an explicit conversion contract.

## 21. WHILE

`WHILE expression ... END WHILE` re-evaluates a Bool condition before every
iteration. Runtime execution limits are host policy, not language semantics.

## 22. FOR

`FOR §item IN expression ... END FOR` iterates a `List<T>` in list order. The
loop variable is local to the loop scope. Mutating the iterated list in a way
that changes traversal is invalid unless a future version defines snapshot
semantics.

## 23. BREAK And CONTINUE

`BREAK` exits the nearest enclosing loop. `CONTINUE` starts its next iteration.
Both are invalid outside REPEAT, WHILE or FOR and are unrelated to
`@flow.break`.

## 24. FUNCTION And RETURN

Functions are V1 syntax:

```emscript
FUNCTION add(§a: Number, §b: Number): Number
    RETURN §a + §b
END FUNCTION

FUNCTION notify(§message: String): Void
    log(§message)
    RETURN
END FUNCTION
```

Functions have lexical function scope, typed parameters, local variables and an
explicit return type. A non-Void function MUST return a compatible value on all
reachable paths. Void functions use `RETURN` without a value.

V1 excludes closures, first-class functions, function values and recursion.
Direct or indirect recursive calls MUST be rejected with
`RECURSION_NOT_SUPPORTED`. `Void` cannot be assigned or passed as a value.

## 25. Geometry Values

Geometry is immutable and side-effect free. Constructors include `point`,
`size`, `bounds`, `region`, `line`, shape constructors and Path constructors.
`Region` is a search/viewport area and is not a Shape.

**Normative boundary: geometry is timeless.** Geometry values MUST NOT contain
hold, delay, duration, speed, pointer offset or animation timing.

## 26. Path Model

Path is timeless geometry and an ordered geometric route. Its segment model is:

```text
PathSegment
|- LineSegment(from, to)
|- QuadraticBezierSegment(from, control, to)
`- CubicBezierSegment(from, control1, control2, to)
```

Canonical constructors are `lineSegment`, `quadraticBezierSegment`,
`cubicBezierSegment` and `path(segments)`. The overload `path(points...)` is a
canonical polyline convenience and is equivalent to consecutive LineSegments.
Neither representation contains gesture or animation timing.

## 27. Gesture Model

A Gesture is an executable input value or action description. Gesture timing
belongs to Gesture/PointerPath parameters, never to Path. High-level gestures
MAY compile to the same typed low-level touch representation.

## 28. PointerPath, MultiPath And GestureSequence

`PointerPath` combines exactly one Path with timing such as duration, delay,
start/end hold or speed. Duration and speed cannot conflict; CommandDefinition
defines which one wins or rejects the call.

`MultiPath` contains multiple PointerPaths whose time ranges may overlap and
therefore represents one multi-pointer gesture. `GestureSequence` contains
complete Gestures executed sequentially. Parallel and sequential composition
are never inferred from list shape alone.

## 29. Click Family

Canonical names are:

- `click(x, y)` and `click(point)` for one coordinate tap;
- `clickText(text)`, `clickElement(element)`, `clickImage(...)` and
  `clickTemplate(...)` for semantic targets;
- `mClick(..., count, delay)` for repeated taps.

Legacy `click("text")` normalizes to `clickText("text")`. Legacy
`clickPoint(...)` normalizes to `click(...)`.

## 30. Swipe Family

`swipe(from, to, ...)` and `swipe(path, ...)` execute one pointer. Timing is a
named Gesture parameter or a PointerPath, not part of Path. A MultiPath is
executed by `multiSwipe(value)`. A GestureSequence executes complete gestures
in order.

## 31. Push, Pull, Pan And Rotate

- `push` moves multiple pointers apart.
- `pull` moves multiple pointers together.
- `pan` moves multiple pointers approximately in parallel.
- `rotate` moves pointers around a center.

In Android screen coordinates, positive rotate angles are clockwise and
negative angles counter-clockwise because positive Y points downward. An
explicit direction enum MAY be offered as equivalent syntax.

`pinch` is reserved and is not an independent V1 primitive. A later alias MUST
map to push or pull and MUST NOT introduce a third direction model.

## 32. Low-Level Touch

Typed escape-hatch operations are `touch.single`, `touch.multi`, `touch.down`,
`touch.move`, `touch.up`, `touch.dispatch`, `touch.reset`, `touch.swipe` and
`touch.getBetween`. High-level gestures SHOULD be preferred. Legacy
`touch(sequence)` requires structural migration when its raw payload cannot be
typed without information loss.

## 33. Perception And Query Semantics

Queries observe state and MUST declare a non-Void return type. They may appear
in expressions. Provider absence produces `PROVIDER_UNAVAILABLE`; an unknown
name produces `UNKNOWN_COMMAND`. Neither condition may silently return false,
an empty value or a fabricated match.

### 33.1 Nullable Core String Queries

The normative V1 contracts for the two core queries audited in M1B-3K are:

```emscript
File.readText(path: String): String?
datastoreGet(key: String): String?
```

`File.readText` returns the complete String contents of an existing readable
regular file. A zero-length file returns `""`. A path that resolves to no
existing regular file returns **absent**. An invalid path, permission denial or
I/O/read failure is a structured runtime failure and MUST NOT return absent or
an empty String.

`datastoreGet` returns the String stored for a present key. A present key whose
value is empty returns `""`. A missing key in an available datastore returns
**absent**. Store unavailability or a read failure is a structured runtime
failure and MUST NOT be reported as a missing key.

Neither command has a V1 default-value parameter. Callers that want a fallback
must handle absence explicitly. The runtime value channel carries String or
absent; the existing execution failure/diagnostic/trace channel carries
failures. `Option<String>`, `Result<String>`, `Any` and sentinel Strings are not
part of these contracts.

M1B-3M implements these contracts as generic `String?` CommandCall
expressions. Both commands are reporter-shaped across Workspace and IR. Legacy
accepted names remain readable; no query-specific expression type exists.

### 33.2 Template Comparison Score

The normative result contract frozen by M1B-3O is:

```emscript
templateCompare(name: String, region: Region, processing: String = "grayscale"): Number
```

The command compares the requested live region with the region stored by the
named template. It is a comparison, not a search. A completed comparison MUST
return the normalized mean absolute processed-pixel similarity:

```text
clamp(1 - mean(abs(process(live) - process(reference))) / 255, 0, 1)
```

The inclusive range is `0.0..1.0`; higher values mean greater similarity.
`0.0` and `1.0` are valid values. Negative values, NaN and infinities are not
valid results. Runtime implementations may calculate with `Float`; the
language value is `Number` and canonical serialization does not prescribe
display rounding.

`templateCompare` has no match-acceptance threshold and no `NOT_FOUND` result.
The internal `0.5` cutoff used by Mask preprocessing changes processed pixels;
it does not accept or reject the resulting score. Every successfully completed
comparison returns its score, including a low score.

The named template, live image/frame, reference image and usable regions are
required inputs. If any is unavailable, the comparison did not occur and the
runtime MUST fail through the structured diagnostic channel. Such failures
MUST NOT return absent, `0`, `-1`, NaN or another numeric sentinel. The
recognized diagnostic categories are `TEMPLATE_NOT_FOUND`,
`TEMPLATE_IMAGE_UNAVAILABLE`, `TEMPLATE_REGION_UNAVAILABLE` and
`TEMPLATE_COMPARE_FAILED`.

M1B-3P implements this contract as a generic `IrExpression.CommandCall` with
`Number` return type. The Workspace uses a typed Region literal projection,
canonical serialization preserves the call expression, and the runtime maps
all unavailable evidence to the diagnostics above. No nullable value or
numeric sentinel is part of the language contract.

### 33.3 Custom Tabs Support Capability

M1B-3R implements the result contract frozen by M1B-3Q:

```emscript
chromeTab.isSupported(): Bool
```

The query means that Android successfully completed service discovery and can
resolve at least one service for `ACTION_CUSTOM_TABS_CONNECTION` in the current
package-manager context. `true` means at least one matching service resolved;
`false` means discovery completed with no matching service. Browser
installation alone, provider configuration and active session/connection state
are not equivalent to this capability.

Failure to execute the host adapter or complete service resolution MUST use the
structured failure channel and MUST NOT produce `false` or absent. The frozen
diagnostic categories are `CHROME_TAB_ADAPTER_UNAVAILABLE` and
`CHROME_TAB_RESOLUTION_FAILED`. No legitimate absent or domain-level unknown
state exists, so the proposed type is non-null `Bool`.

The command is a generic `Bool` reporter and projects through
`IrExpression.CommandCall` without a query-specific expression type. The legacy
spelling `ChromeTab.isSupported` remains an import alias; canonical serialization
emits `chromeTab.isSupported`.

The runtime result transport keeps execution outcome and return value separate.
A successful result may contain `BooleanValue(true)`, `BooleanValue(false)`, no
value for a Void action, or `NullValue` for a successful nullable query. No value
and `NullValue` are distinct. Failure carries a diagnostic and no value. Human
readable `message` text is trace metadata, never the return-value channel.

Only `chromeTab.isSupported` is migrated in M1B-3R. It leaves
`D_QUERY_RETURN`, reducing that inventory from 17 to 16 while the bridge remains
127 entries and `NATIVE_V1` remains 3.

## 34. Draw

`draw(value, ...)` is the canonical Draw action. It accepts supported Geometry,
Shape, text or image values as declared by CommandDefinition and returns an
`OverlayHandle`. Names such as `draw.circle` are convenience or legacy aliases,
not competing canonical semantics.

## 35. Shape

Shape is an immutable value supertype for concrete visual forms such as Circle,
Rectangle, Triangle and Polygon. Transforming or animating a rendered object
MUST NOT mutate the Shape value or a persisted VisualAsset definition.

## 36. OverlayHandle

OverlayHandle is the stable runtime identity of a rendered object:

```emscript
LET §shape = circle(point(100, 100), 40)
LET §handle: OverlayHandle = draw(§shape)
```

It is valid in its runtime/overlay context until `remove(§handle)`, `clear()` or
context termination. A persisted Shape/VisualAsset identity and a runtime
OverlayHandle are never interchangeable.

## 37. Transform

`transform.move`, `transform.translate`, `transform.rotate`,
`transform.scale`, `transform.alpha` and `transform.resize` synchronously change
the state of an OverlayHandle. They do not mutate its source Shape.

## 38. Animation

`animate.move`, `animate.rotate`, `animate.scale`, `animate.alpha`,
`animate.path` and `animate.sequence` describe temporal transitions of an
OverlayHandle. Duration and easing are animation parameters. `animate.path`
consumes the same timeless Path type as Draw and Swipe.

## 39. Provider Namespaces

Provider calls use registered qualified namespaces such as `tasker.*`,
`shizuku.*`, `termux.*`, `customTab.*`, `scrcpy.*`, `chart.*` and `vt2vt.*`.
Provider commands use the generic call grammar. Their availability, capability
and return types are registry data, not parser branches. Historical capitalized
spellings remain V1.x legacy aliases.

## 40. Projection Directives

Directives begin with `@`, persist in a Projection AST and MUST NOT enter
Runtime IR. Removing every directive MUST leave runtime behavior unchanged.

Range directives use explicit stable-ID pairs:

```emscript
@group.start("setup")
LET §x = 0
@group.end("setup")
```

Layer and other true ranges follow the same rule. Validation distinguishes
missing start/end, duplicate ID, mismatched pair and invalid nesting. Point
directives such as `@flow.break(...)`, `@marker(...)` and
`@flow.layout(...)` are not paired.

## 41. Runtime Trace Marker

`trace.mark(label)` is a normal runtime Action that emits trace evidence.
`@marker(label)` is runtime-inert projection metadata. Equal labels do not make
the constructs semantically interchangeable.

## 42. Diagnostics

Diagnostics are structured records containing at least code, severity,
message, line, column and source range, with optional suggestion/fix.
Normative codes include:

- `INVALID_ESCAPE`, `UNKNOWN_VARIABLE`, `VARIABLE_ALREADY_DECLARED`;
- `TYPE_MISMATCH`, `EXPECTED_BOOL`, `INVALID_ARGUMENT`;
- `INDEX_OUT_OF_BOUNDS`, `INVALID_LOOP_CONTROL`;
- `RECURSION_NOT_SUPPORTED`, `UNKNOWN_COMMAND`, `PROVIDER_UNAVAILABLE`;
- `DIRECTIVE_MISSING_START`, `DIRECTIVE_MISSING_END`,
  `DIRECTIVE_DUPLICATE_ID`, `DIRECTIVE_MISMATCHED_PAIR`,
  `DIRECTIVE_INVALID_NESTING`.

Validation and runtime failures remain distinguishable. Diagnostics MUST retain
the original source range.

## 43. Canonical Serialization

The canonical serializer MUST be deterministic and emit:

- `§` local variables;
- uppercase keywords and lowerCamel canonical command names;
- Newline termination without optional semicolons;
- `ELSEIF` and `END ...` control forms;
- canonical provider namespaces and argument names;
- `@` directives rather than safely mapped `rem.*` aliases.

It MUST NOT emit legacy syntax. Formatting may change whitespace but not
semantics, stable references or projection metadata.

## 44. Legacy Compatibility

The V1.x reader/importer MUST continue to read documented legacy forms,
including bare variables, braces, `ELSE IF`, `REM` comments, registered command
aliases, old provider casing, `click("text")`, `clickPoint`, raw
`touch(sequence)` and `rem.*` projection legacy.

Safe forms normalize to V1. Constructs whose extent, identity or payload cannot
be recovered without loss require structural migration and MUST NOT receive
invented canonical text. The serializer never emits legacy forms. Earliest
removal of a supported V1 legacy alias is EMScript 2.0. A supported V1 legacy
alias MUST NOT be removed earlier than EMScript 2.0.

## 45. Versioning Rules

Patch releases may clarify wording and add diagnostics without changing valid
program meaning. Minor V1.x releases may add registered commands, types and
backward-compatible aliases, but MUST preserve this grammar and legacy-reading
policy. Any removal of V1 syntax, changed operator binding or changed runtime
meaning requires a major language version.

The normative fixtures under `conformance/v1/` are part of the frozen V1
contract. Implementation conformance is a separate, testable claim.

## 46. Installed Provider Queries

`tasker.isInstalled()`, `shizuku.isInstalled()` and
`termux.isInstalled()` return non-null `Bool`. A completed PackageManager
lookup with no supported package is `false`; adapter absence or technical
package inspection failure is a structured runtime failure and MUST NOT be
serialized as `false` or `null`. Their historical uppercase namespace
spellings remain V1 import aliases. `shizuku.isAvailable()` is a separate
composite capability query and is not covered by this rule.

## 47. Shizuku Availability Query

`shizuku.isAvailable()` returns non-null `Bool` and means exactly that the
Shizuku package is installed, this app has permission and the Binder is alive.
A successfully inspected missing package, permission or Binder produces
`false`. Adapter absence or technical package, permission or Binder inspection
failure produces a structured runtime failure and MUST NOT become `false` or
`null`. `Shizuku.isAvailable` remains a V1 import alias.
