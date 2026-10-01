# EMScript v1 Repository Conformance Audit

> Post-audit note: Draft 0.3 was supplied after this implementation audit. The
> normative target is now frozen in
> `docs/reference/emscript/EMSCRIPT_V1_SPEC.md`. Findings below remain a
> historical implementation snapshot and are not the normative language spec.

Stand: 2026-09-21

Scope: current WSS main checkout and its embedded BlockEditor/Flowchart modules.
No parser, IR, runtime, registry, editor or test code was changed for this
audit.

## 1. Executive Summary

The repository has a useful EMScript implementation, but not yet one coherent
language implementation. Text is parsed into a local parser IR, imported into
the canonical `WorkspaceDocument`, converted into a second BlockEditor IR for
text generation, converted into `IrGraph` for Flowchart, and interpreted by two
runtime implementations. A 127-entry command catalog covers core and provider
names, but parser special cases and live dispatch can override or contradict
its declarations.

The requested normative document, `EMScript v1 - Sprach- und API-Referenz Draft
0.3`, is absent from this checkout and from the supplied audit material. Exact
SPEC-to-code conformance is therefore `UNRESOLVED`. The checked-in
`STABLE_V1_GRAMMAR.ebnf` and `LANGUAGE_CONTRACT.md` were audited as repository
references, not silently substituted for Draft 0.3.

The implemented core is strongest for `LET`, `SET`, scalar expressions,
`IF/ELSEIF/ELSE`, `repeat`, `while`, basic feedback/actions and
Text -> Workspace -> generated text. Principal P0 risks are duplicated language
truth, non-stable IDs on text re-import, catalog/runtime drift and lossy
roundtrips for unsupported expression/value forms.

## 2. Current Language Architecture

| Layer | Main implementation | Role | Contains language truth |
| --- | --- | --- | --- |
| Reference | `docs/reference/emscript/*` | intended Stable-V1 contract | Yes, but not enforced and partly stale |
| Lexer/parser | `EmscriptParserSlice.kt` | tokens, grammar, parser IR, catalog validation | Yes |
| Text import | `EmscriptWorkspaceImporter.kt` | parser IR -> workspace blocks | Yes |
| Editor facets | `EmscriptEditorFacets.kt` | comment-delimited group metadata | Yes, projection-only |
| Workflow | blockeditor-domain `WorkspaceDocument` | canonical intent document | Yes |
| Block registry | `CommandCatalog.kt`, block registries/definitions | commands, arguments, blocks, capabilities | Yes |
| Text-generation IR | `IrModels.kt`, `IrGenerator.kt` | small statement/expression model | Yes |
| Generator | `EmscriptGenerator.kt` | workspace/IR -> EMScript | Yes |
| Semantic graph | `IrGraphGenerator.kt`, graph models | scopes, branches, data flow, facets, source refs | Yes |
| Persistence | `WorkspaceSerializer.kt` | schema-v1 workflow JSON | No text grammar, but preserves block semantics |
| Flow projection | `IrGraphFlowchartProjector.kt` | IRGraph -> FlowGraph | Mapping truth |
| Dry runtime | `WorkspaceDryRunRuntime`, `EmscriptDryRunRuntime` | two interpreters | Yes |
| Live runtime | `WorkspaceBasicRuntime`, environment/adapters | effects and provider dispatch | Yes |
| Editor tools | formatter and syntax highlighter | formatting/coloring | Duplicated, partly aspirational truth |

`WorkspaceWorkflowState.fromDocument` confirms the intended direction:
`WorkspaceDocument` is serialized, projected to `IrGraph`, generated to text,
and projected to Flowchart. ADR 0001 calls this concrete type the architectural
`WorkflowDocument`.

## 3. Lexer Findings

- Identifiers start with a letter or `_`; following characters are letters,
  digits or `_`. Qualified names are parser-composed with `.`.
- Keywords are case-insensitive: `LET`, `SET`, `IF`, `ELSEIF`, `ELSE`, `LOOP`,
  `REPEAT`, `WHILE`, `END`, `TRUE`, `FALSE`.
- Variables use bare identifiers. `%variables` and `§variables` are not lexical
  forms; `%` is the modulo token and `§` is invalid.
- Strings are double quoted. Supported escapes are `\\`, `\"`, `\n`, `\r`,
  `\t`. Unknown escapes lose the backslash instead of producing a diagnostic.
- Numbers are decimal integer/fraction tokens. Unary `-` is parsed separately;
  exponent notation and numeric suffixes are absent.
- `//` starts a line comment. `REM` starts a comment unless followed by `.`, so
  `rem.region(...)` remains a qualified command.
- Spaces, tabs and carriage returns are ignored. Newlines and semicolons are
  statement separators.
- Tokens exist for `[]` and `:`, but the expression grammar does not consume
  them as collections, index access or named arguments.
- Braces are lexed and accepted as alternate control-flow delimiters.

Status: `PARTIAL` against the repository Stable-V1 grammar because array tokens
exist without array grammar and the generator chooses braces.

## 4. Parser Findings

Accepted statements:

- `LET name = expression`
- `SET name = expression`
- `IF expression ... ELSEIF/ELSE IF ... ELSE ... END IF`
- brace form `if (expression) { ... } else if (...) { ... }`
- `LOOP` or `REPEAT`, terminated by braces or `END LOOP`/`END REPEAT`
- `WHILE`, terminated by braces or `END WHILE`
- qualified positional calls resolved through the command catalog
- legacy `WAIT expression`, `CLICK "text"`, `OUTPUT expression`, and whitespace
  `BEEP` arguments

Not accepted: named arguments, arrays/lists, index access, `FOR`, `BREAK`,
`CONTINUE`, user functions, `RETURN`, `TRY/CATCH`, `UNTIL`, unary `!`, null or
member/property expressions.

Special parser paths exist for `wait`, `click`, `log`, `beep`, and `vibrate`.
All other statement calls preserve a raw argument string, split top-level
commas, and validate only the catalog's coarse argument types. This allows
nested `()`, `[]`, and `{}` text inside `ANY`, but does not create semantic
collection expressions.

Unknown commands fail parsing. Only catalog `REPORTER` and `OPERATOR` entries
may occur as expression calls.

## 5. Type System

There is no single language type system.

- Parser literals: Number, String, Boolean, variable reference, function call,
  binary expression.
- Catalog argument types: `ANY`, `BOOLEAN`, `NUMBER`, `TEXT`, `DURATION_MS`,
  `FREQUENCY_HZ`, `PERCENT`, `VARIABLE_REF`, `IMAGE_TEMPLATE`, `REGION`,
  `STATEMENT_BODY`.
- Block connections use free-form type strings such as `Bool`, `Boolean`,
  `Number`, `Text`, and `Any`.
- Runtime values add `NullValue`.
- `REGION`, points, paths, images and structured `ANY` are raw call text, not
  typed parser IR values.

Catalog validation for generic calls is lexical/shape based, not semantic type
checking. `VARIABLE_REF` permits an identifier-like or quoted raw argument,
which conflicts with catalog defaults such as `%var` that the lexer cannot
parse as a normal expression.

## 6. Expression System

Implemented precedence, low to high:

1. `||`
2. `&&`
3. `==`, `!=`, `<`, `<=`, `>`, `>=` in one shared tier
4. `+`, `-`
5. `*`, `/`, `%`
6. unary `-`, represented as `0 - operand`
7. literals, variable references, reporter/operator calls, parentheses

This conflicts with the checked-in EBNF, which separates equality from ordered
comparison. Unary `!` is lexed as part of `!=` handling but not implemented as
an expression. Runtime arithmetic is numeric; string concatenation is absent.
Dry-run reporter calls mostly return typed placeholder values rather than
executing queries.

## 7. Control Flow

`IF`, multiple `ELSEIF`, `ELSE`, `REPEAT/LOOP`, and `WHILE` are represented in
parser IR, workspace blocks, generated text, `IrGraph`, Flowchart and dry run.
Branch ordering is preserved by numbered `ELIF_CONDITION_n`/`ELIF_n` slots and
sorted reconstruction. Import currently caps dynamic else-if branches at six.

`FOR`, `BREAK`, `CONTINUE`, `FUNCTION`, and `RETURN` are `MISSING`. Formatter and
highlighter recognition of several of these is editor-only and does not make
them valid language constructs.

## 8. Command API

The catalog contains 127 entries and 124 unique canonical names:

- kinds: 1 EVENT, 110 STATEMENT, 5 CONTROL, 4 OPERATOR, 5 REPORTER, 2 VARIABLE;
- owners: core 40, custom tabs 9, flowchart 8, scrcpy 15, shizuku 11, tasker 17,
  termux 8, vision 5, charts 14;
- every entry declares block and Flowchart bindings plus runtime metadata.

Compact inventory below uses `?` for optional arguments. Defaults follow `=`.
Unless noted, provider names have no alias. All generic commands have positional
parser support and serialize through a generic EMScript command block.

### Core, input, vision and system

| Commands/signatures | Class/domain | Implementation status |
| --- | --- | --- |
| `onStart()` aliases `EVENT.ON_START`, `em_on_start` | CONTROL/CORE | Entry block; not a text statement handled by parser |
| `wait(ms:DURATION_MS=500)` | ACTION/TIMING | Parser special case, block, flow, dry/live |
| `click(text:TEXT=OK)` | ACTION/A11Y | Parser special case, block, flow, dry/live adapter |
| `clickPoint(x:NUMBER=0,y:NUMBER=0,repeat?:NUMBER=1)` | ACTION/INPUT | Generic parse; live dispatch |
| `swipe(points:ANY,repeat?:ANY=1)` | ACTION/INPUT | Raw geometry; live dispatch |
| `touch(sequence:ANY)` | ACTION/MULTI_TOUCH | Catalogued; `liveImplemented=false` |
| `beep(frequency=1000,durationMs=200,volume=100)` | ACTION/FEEDBACK | Parser special case, dry/live |
| `vibrate(pattern:DURATION_MS=80)` | ACTION/FEEDBACK | CONFLICT: parser accepts 1..16 integers |
| `log(message:TEXT=debug)` | ACTION/DEBUG | CONFLICT: parser accepts expression |
| `screenshot(path?:TEXT)` | ACTION/PERCEPTION | Basic live dispatch with screen capability |
| `findTemplate(imagePath,threshold=.82,timeoutMs=3000,retryCount?=1,searchRegion?)` | QUERY/PERCEPTION | Live dispatch ignores retry count |
| `ocr(region?,timeoutMs?=3000)`, `findText(text,timeoutMs?=3000)`, `highlight(region)` | QUERY/ACTION/PERCEPTION | Catalog says live; no direct BasicRuntime dispatch |
| `markerSave(name,region,mode?=region,threshold?=.85)`, `markerLoad(name)`, `markerDelete(name)` | DEFINITION/QUERY/PERCEPTION | Live dispatch present |
| `templateDefine(name,region,processing?=grayscale)`, `templateCompare(name,region,processing?=grayscale)` | DEFINITION/QUERY/PERCEPTION | Live dispatch present, but both return Void in catalog |
| `sceneSave(name,markerMode?,region?,asset?)` | DEFINITION/OTHER | Catalogued as core; no direct BasicRuntime dispatch |
| `datastorePut(key,value)`, `datastoreGet(key)` | DEFINITION/QUERY/SYSTEM | Live dispatch; `get` returns Void in catalog |
| `File.readText(path)`, `File.writeText(path,text)` | QUERY/ACTION/SYSTEM | Live dispatch; read returns Void in catalog |
| `Clipboard.get()`, `Clipboard.set(text)` | QUERY/ACTION/SYSTEM | Live dispatch; get returns Void in catalog |
| `Cache.clear()`, `Sys.info()`, `Env.get(name)` | ACTION/QUERY/SYSTEM | Live dispatch; queries return Void in catalog |

Noteworthy aliases also include `CLICK_TEXT`, `tap`, `VISION.SCREENSHOT`,
`ScreenCapture.capture`, `Region.readText`, `Region.findText`,
`Region.highlight`, `Marker.*`, `Template.*`, and uppercase legacy forms.

### Variables, control and expressions

| Commands/signatures | Class/domain | Status |
| --- | --- | --- |
| `set(variable:VARIABLE_REF,value:ANY)` | DEFINITION/CORE | Catalog projection of `SET`; parser uses keyword path |
| `get(variable:VARIABLE_REF)->Any` aliases `GET`, `LET` | VALUE/CORE | Catalog conflicts conceptually with `LET` declaration |
| `repeat(times=3,DO)`, `while(condition,BODY)` | CONTROL/CORE | Parser keywords and blocks |
| three `if(...)` entries for IF, IF/ELSE, IF/ELSEIF/ELSE | CONTROL/CORE | Duplicate canonical name; parser keyword path |
| `screenContains(text=OK)->Boolean` | QUERY/A11Y | Reporter; dry-run placeholder false |
| `boolean(value=true)` and `boolean(value=false)` | VALUE/CORE | Duplicate canonical name for two block types |
| `number(value=0)->Number`, `string(value)->Text` | VALUE/CORE | Literal reporters |
| `and(A,B)`, `or(A,B)`, `operate(Input1,Input2,operator=add)`, `compare(LEFT,RIGHT,operator=GREATER_OR_EQUAL)` | EXPRESSION/CORE | Parser functions plus infix aliases/operators |

### Projection commands

`rem.region(name,mode,color?)`, `rem.variableBulk(name,layout?,variables?)`,
`rem.expressionCapsule(name,strategy?,nodes?)`, `rem.flowBreak(label,direction?)`,
`rem.offPageOut(connector)`, `rem.offPageIn(connector)`,
`rem.group(name,active?)`, `rem.layoutHint(mode,scope?)`.

All are `PROJECTION` class, owned by `visualtasker.flowchart`, mapped to generic
blocks and `rem-flow-node`, and tagged `visual-metadata`. They are currently
declared as `STATEMENT` with `CONTROL_FLOW` side effect, which is a model
conflict even though BasicRuntime has no effect branch for them.

### Provider families

| Provider | Catalogued commands | Declared status | Observed BasicRuntime path |
| --- | --- | --- | --- |
| Custom Tabs | `open`, `unbind`, `isSupported`, `bind`, `create`, `mayLaunchUrl`, `requestPostMessageChannel`, `postMessage`, `validateRelationship` | 9 adapter-gated statements | Family dispatch exists |
| Tasker | `runTask`, `setVariable`, `emitEvent`, `isInstalled`, `isEnabled`, `cancel`, `getVariable`, `clearVariable`, `getVariables`, `lastResult`, `error`, `action`, `pluginAction`, `profileEnable`, `profileDisable`, `profileToggle`, `profileState` | 17 adapter-gated statements | Family dispatch exists |
| Shizuku | `exec`, `shell`, `isInstalled`, `isAvailable`, `getUid`, `permissionState`, `requestPermission`, `bindUserService`, `unbindUserService`, `systemService`, `call` | 11 adapter-gated statements | Family dispatch exists |
| Termux | `run`, `shell`, `api`, `isInstalled`, `canRunCommands`, `writeStdin`, `cancel`, `get` | 8 adapter-gated statements | Family dispatch exists |
| Scrcpy | `start`, `stop`, `touch`, `hostAvailable`, `devices`, `connect`, `disconnect`, `isRunning`, `get`, `key`, `text`, `scroll`, `setClipboard`, `setScreenPower`, `rotate` | 15 adapter-gated statements | Family dispatch exists |
| Charts | `create`, `show`, `export`, `hide`, `remove`, `exists`, `setData`, `setOptions`, `add`, `update`, `removeData`, `clear`, `get`, `capture` | 14 adapter-gated statements | No Chart family dispatch in BasicRuntime |

Provider parameters and defaults are declared in `CommandCatalog.kt`. Query-like
provider commands are still `STATEMENT -> Void`, so value use is not available
despite names such as `get`, `exists`, `isInstalled`, `lastResult`, or `error`.

## 9. Geometry

Only `REGION` has a catalog argument type, accepted as quoted text or raw
`region(...)`. It has no typed parser AST/IR node. `clickPoint` stores numeric
coordinates, and `swipe` accepts untyped point text. There are no language value
constructors for Point, Points, Line, Path, Bounds, Circle, Rectangle, Triangle,
Polygon or general Shape. Canvas/marker geometry classes elsewhere in the app
are UI/domain facilities, not EMScript syntax.

Status: `MISSING` for a typed geometry API; `PARTIAL` for raw region/point use.

## 10. Gesture / MultiTouch

- `click`, `clickPoint`, and `swipe` are represented and partly live-executable.
- `touch(sequence:ANY)` is catalogued but explicitly not live implemented.
- No `mClick`, double/triple click, `multiSwipe`, `multiPath`, pinch, push, pull,
  pan, rotate, or `touch.single/multi/down/move/up/reset/swipe/getBetween` API is
  present in the EMScript catalog.
- No builder API (`build/add/go`) exists.
- Timing, hold, speed, control points, Bezier behavior, parallel pointers and
  staggered/sequential multi-touch are not represented semantically.

Status: `PARTIAL` for single-pointer basics, otherwise `MISSING`.

## 11. Perception

Screenshot, OCR, text/template search, highlight, marker and template commands
are catalogued. The catalog has no OCV or YOLO command family. Vision behavior
is split between core, `visualtasker.vision`, UI resources and runtime
environment functions. Several query operations return Void and therefore
cannot participate in expressions.

## 12. Draw / ShapeMaker Boundary

No Draw, Transform, Animation or ShapeMaker command family exists in the
EMScript catalog. ShapeMaker/VisualAsset code is an external visual-asset
boundary and must not be inferred as language support. `.ema`, drawing paths,
shape constructors and animation values are `MISSING` from EMScript.

## 13. Provider Commands

Provider names are parser-visible because the catalog supplies qualified names,
raw positional signatures, plugin owners and capability gates. This is stronger
than runtime conformance: all are statements, many query names lack return
types, Chart has no BasicRuntime family dispatch, and catalog
`liveImplemented=true` does not prove an environment adapter performs the
operation. Provider absence is generally surfaced by capability warnings/gates.

## 14. Projection Metadata

Two projection mechanisms exist:

1. `REM`/`//` group facets scanned from source comments and attached as start
   block metadata.
2. executable-looking `rem.*` namespace calls parsed into generic command
   blocks, decoded by `RemFlowMetadata`, and projected to typed IR facets or
   off-page Flow edges.

IR facets cover branch regions, collapse groups, comment markers, variable
bulks and function regions. Off-page in/out pairs become synthetic `GOTO`
edges; branch joins and the workflow terminator are also synthetic Flow nodes.

No live side effect is dispatched for `rem.*`, so the runtime-inert invariant
holds operationally. It is not structurally guaranteed because the catalog
still labels them `STATEMENT`, `CONTROL_FLOW`, and `live=true`.

## 15. BlockEditor Mapping

Dedicated blocks exist for core actions, feedback, variables, literals,
operators and controls. Generic catalog commands use
`emscript.command.<catalog-id>` with fields `command` and raw `args`. Dynamic
ELSEIF slots are added during import. Unknown custom blocks degrade to generated
`log(...)`; unsupported standard blocks also generate a log message rather than
a hard serialization error, which can hide semantic loss.

## 16. Flowchart Mapping

`IrGraphFlowchartProjector` maps IR node/edge kinds, source references, scopes,
branches and facets to FlowGraph. It adds synthetic JOIN and workflow END nodes.
Flowchart is a projection from IRGraph; there is no general
Text -> Flow -> IR -> Text reverse serializer. Flow content mutations route back
through typed workflow mutations, while FlowView positions/routes remain view
state.

## 17. Runtime Mapping

`WorkspaceDryRunRuntime` interprets workspace blocks and emits block/edge trace
events. `EmscriptDryRunRuntime` separately interprets parser IR. This duplication
can drift. Reporter functions commonly return default placeholder values.

`WorkspaceBasicRuntime` first dry-runs, then replays selected event kinds into
live environment methods. It directly supports wait/log/click/beep/vibrate,
clickPoint/swipe, clipboard/cache/system/file, screenshot, template/marker and
datastore operations, plus family dispatch for Custom Tabs, Tasker, Shizuku,
Termux and Scrcpy. Commands lacking a branch can pass catalog gating yet produce
no live outcome. Chart and several Vision/Scene commands expose this risk.

## 18. Roundtrip

| Path | Current state | Loss/risk |
| --- | --- | --- |
| Text -> parser IR | Implemented/tested | first parse issue only; spans are shallow |
| Parser IR -> workspace | Implemented/tested | random block IDs; source lines assigned heuristically |
| Workspace -> JSON -> workspace | Implemented/tested, schema 1 | preserves block IDs/fields/connections |
| Workspace -> small IR -> text | Implemented/tested | unsupported blocks become logs; generator emits braces |
| Text -> workspace -> text | Broad tests | aliases canonicalized; formatting/literal representation may change |
| Text -> workspace -> IRGraph -> Flow | Implemented/tested | projection adds synthetic nodes/edges |
| Text -> Flow -> IR -> text | No general reverse path | `MISSING` |
| Block -> Flow -> Block | Shared workflow document, not serialization roundtrip | view edits and semantic mutations are separate |

Block IDs are UUIDs on every text import. Therefore semantic identity is stable
inside a persisted workspace revision, but not across a fresh Text -> Workspace
roundtrip. Operator names normalize to canonical field values. Variable ID and
label are often the same string on import. Branch order is preserved; original
aliases and exact number formatting are not guaranteed. Unknown commands fail.

## 19. Language Truth Map

See `EMSCRIPT_LANGUAGE_TRUTH_MAP.md`. The highest-risk duplicate owners are the
parser special cases, catalog, two IRs, two runtimes, generator, hardcoded
formatter/highlighter lists and stale documentation.

## 20. Conformance Matrix

`Spec 0.3` is `UNAVAILABLE` below because the normative file was not supplied.
Statuses evaluate the current implementation against constructs explicitly
named in the audit request and checked-in reference where applicable.

| Construct | Spec 0.3 | Lexer | Parser | IR | Serializer | Block | Flow | Runtime | Registry | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| LET/SET | unavailable | YES | YES | YES | YES | YES | YES | YES | YES | PARTIAL | text re-import regenerates IDs |
| String/Number/Bool | unavailable | YES | YES | YES | YES | YES | YES | YES | YES | PARTIAL | exact literal form not preserved |
| Any | unavailable | raw | raw calls | raw | raw | YES | YES | partial | YES | PARTIAL | no structured semantic value |
| `%`/`§` variables | unavailable | NO | NO | NO | NO | NO | NO | NO | catalog default only | CONFLICT | `%` is modulo |
| Qualified call | unavailable | YES | YES | raw command IR | YES | YES | YES | partial | YES | PARTIAL | runtime dispatch may be absent |
| Positional/default args | unavailable | YES | YES | partial | YES | YES | YES | partial | YES | PARTIAL | special parser paths differ |
| Named args | unavailable | token `:` only | NO | NO | NO | NO | NO | NO | names metadata only | MISSING | |
| Arithmetic/comparison | unavailable | YES | YES | YES | YES | YES | YES | YES | YES | PARTIAL | precedence conflict with EBNF |
| `&&`/`||` | unavailable | YES | YES | YES | YES | YES | YES | YES | YES | CONFORMANT | for implemented scalar model |
| unary `!` | unavailable | NO | NO | NO | NO | NO | NO | NO | alias-like metadata only | MISSING | |
| IF/ELSEIF/ELSE | unavailable | YES | YES | YES | YES | YES | YES | YES | YES | PARTIAL | max six ELSEIF on import |
| REPEAT/WHILE | unavailable | YES | YES | YES | YES | YES | YES | YES | YES | CONFORMANT | bounded by dry-run limits |
| FOR/BREAK/CONTINUE | unavailable | identifiers/keywords absent | NO | NO | NO | NO | NO | NO | NO | MISSING | formatter/highlighter only |
| List/index | unavailable | brackets YES | NO | NO | NO | NO | NO | NO | NO | MISSING | raw ANY may contain text |
| FUNCTION/RETURN | unavailable | identifiers | NO | NO | NO | NO | partial visual facet | NO | NO | MISSING | |
| Point/Region | unavailable | call text | raw only | raw | raw | generic | generic | partial | partial | PARTIAL | no typed values |
| Path/Bounds/Shapes | unavailable | NO | NO | NO | NO | NO | NO | NO | NO | MISSING | |
| Click/Swipe | unavailable | YES | YES | raw/typed split | YES | YES | YES | partial live | YES | PARTIAL | swipe points are ANY |
| MultiTouch | unavailable | raw `touch` | generic | raw | YES | generic | generic | blocked | YES | PARTIAL | no semantic model |
| Perception | unavailable | calls | YES | raw commands | YES | generic | generic | mixed | YES | PARTIAL | queries return Void |
| Draw/Transform/Animation | unavailable | NO | NO | NO | NO | NO | NO | NO | NO | MISSING | |
| Provider commands | unavailable | YES | YES | raw | YES | generic | generic | mixed | YES | PARTIAL | catalog metadata exceeds runtime |
| Comments | unavailable | YES | YES | facets partial | n/a | metadata | facets | inert | n/a | PARTIAL | REM namespace exception |
| Semicolons | unavailable | YES | YES | n/a | generator YES | n/a | n/a | n/a | n/a | CONFORMANT | optional separators |
| Escapes | unavailable | YES | YES | YES | YES | fields | labels | YES | n/a | PARTIAL | unknown escape silently changes text |
| Projection directives | unavailable | calls/comments | YES | YES | YES | YES | YES | operationally inert | YES | CONFLICT | typed as control-flow statements |

## 21. Conflicts

1. The named Draft 0.3 is unavailable, while repository docs claim a different
   Stable-V1 authority.
2. EBNF includes arrays; parser does not.
3. EBNF calls line-oriented control canonical; generator emits braces.
4. EBNF separates equality/comparison precedence; parser combines them.
5. `vibrate` catalog is one argument; parser accepts 1..16.
6. `log` catalog requires TEXT; parser accepts expressions.
7. `%var` provider defaults are not parser variable syntax.
8. Catalog says many query commands return Void.
9. Catalog `liveImplemented=true` is not equivalent to actual BasicRuntime
   dispatch.
10. Projection commands are runtime-inert in practice but modelled as
    side-effecting control-flow statements.
11. Formatter/highlighter advertise constructs rejected by parser.
12. Existing roundtrip documentation says Charts are missing, while 14 Chart
    commands are currently catalogued.

## 22. Missing v1 Features

Exact Draft-0.3 gaps cannot be certified without the draft. Explicitly requested
but absent capabilities are named arguments, unary `!`, list/index semantics,
`FOR`, `BREAK`, `CONTINUE`, functions/return, typed geometry and shapes, complete
multi-touch/gesture builders, Draw/Transform/Animation, value-returning provider
queries, and a single semantic runtime representation.

## 23. Legacy Constructs

- uppercase aliases and old names (`CLICK_TEXT`, `em_click_text`,
  `EVENT.ON_START`, etc.);
- statement forms `WAIT expr`, `CLICK "text"`, `OUTPUT expr`, `BEEP n n n`;
- both `ELSEIF` and `ELSE IF`;
- `LOOP` and `REPEAT` aliases;
- brace and `END ...` control-flow forms;
- comments used as editor group facets;
- generic command blocks retaining raw argument text;
- legacy Blockly XML importer outside the EMScript parser.

## 24. Unsafe Ambiguities

- `ANY` can preserve syntactically nested text without defining its value
  semantics.
- default values make some catalog arguments appear required and optional at
  the same time.
- duplicate canonical `if` and `boolean` entries make name lookup order
  significant.
- query names returning Void prevent reliable expression use.
- placeholder dry-run values can choose a branch different from live reality.
- BasicRuntime can authorize but silently omit an undispatched command.
- equality compares rendered runtime values, not a formal coercion contract.
- division/modulo behavior has no explicit zero/error language contract.
- parser source spans cover only the starting token, and workspace source-line
  mapping uses a statement-line cursor rather than a full syntax tree.
- random IDs on import break identity across fresh text roundtrips.

## 25. Recommended Migration Order

| Milestone | Priority | Audit-driven outcome |
| --- | --- | --- |
| M1 - Stabilize language core | P0 | Freeze actual lexical/grammar behavior and resolve brace/line, precedence, escape and variable conflicts |
| M2 - Central registry | P0 | Make names/signatures/return types/runtime binding machine-verifiable; remove duplicate canonical ambiguity |
| M3 - Parser/serializer conformance | P0 | One canonical syntax, CST-aware diagnostics, defaults/named-argument decision, deterministic generation |
| M4 - IR conformance | P0 | Unify parser IR and generation/runtime semantic IR; stable semantic IDs |
| M5 - Block/Flow projections | P1 | Generate mappings from definitions and prove projection reversibility where editing is allowed |
| M6 - Gesture/Geometry API | P1 | Typed Point/Region/Path/gesture timing and multi-pointer model |
| M7 - Projection directives | P1 | Runtime-inert directive schema separate from action statements |
| M8 - Provider commands | P1 | Correct VALUE/QUERY/ACTION classes, return types and adapter-owned dispatch |
| M9 - Conformance suite | P0 | Executable lexer/parser/IR/generator/runtime/catalog matrix from the normative spec |
| M10 - EMScript v1 freeze | P1 | Versioned spec, migration policy, generated reference and compatibility fixtures |

P2 after freeze: editor highlighting/format UX and richer projection sugar.
P3: optional domain expansion not required by the v1 contract.

## Audited Files

Primary files read include:

- `app/.../emscript/parser/EmscriptParserSlice.kt`
- `app/.../emscript/parser/EmscriptWorkspaceImporter.kt`
- `app/.../emscript/parser/EmscriptEditorFacets.kt`
- `app/.../emscript/runtime/{EmscriptDryRunRuntime,WorkspaceDryRunRuntime,WorkspaceBasicRuntime,RuntimeCapabilityGate}.kt`
- `app/.../emscript/editor/{EmsScriptFormatter,SyntaxHighlighter,EmscriptEditorSession}.kt`
- `app/.../workspace/model/WorkspaceWorkflowState.kt`
- `app/.../flowchart/IrGraphFlowchartProjector.kt`
- blockeditor domain, registry, validation, IR, EMScript and serialization
  modules, especially `CommandCatalog.kt`, `IrGenerator.kt`,
  `IrGraphGenerator.kt`, `IrModels.kt`, `RemFlowMetadata.kt`,
  `EmscriptGenerator.kt`, `WorkspaceSerializer.kt`, and `Validator.kt`
- flowchart domain/layout/projection models relevant to node and edge mappings
- EMScript reference, ADR, wiki, fixtures and all corresponding existing tests.

## Verification Boundary

The audit deliberately does not claim device behavior or provider availability.
Existing tests and `assembleDebug` are the required verification; their actual
results are reported in the completing task response rather than predeclared in
this document.
