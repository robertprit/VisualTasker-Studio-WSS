# EMScript Language Truth Map

> Post-audit note: The V1 target contract is now frozen in
> `docs/reference/emscript/EMSCRIPT_V1_SPEC.md` and
> `docs/reference/emscript/EMSCRIPT_V1_GRAMMAR.ebnf`. This file continues to
> describe current implementation ownership and drift, not normative syntax.

Stand: 2026-09-21

This document records where EMScript language knowledge exists in the current
repository. It is an audit of the implementation, not a new language contract.
The document named `EMScript v1 - Sprach- und API-Referenz Draft 0.3` in the
audit request is not present in this checkout. Therefore exact Draft-0.3
conformance remains unresolved; the checked-in Stable-V1 reference is listed as
a separate, non-executable source.

## Truth Owners

| Concept | Current owner(s) | Duplicate or conflict | Risk | Recommended future owner |
| --- | --- | --- | --- | --- |
| Lexical tokens and comments | `app/.../emscript/parser/EmscriptParserSlice.kt` | `SyntaxHighlighter.kt`, formatter, EBNF | High: editor recognizes words the parser rejects | Versioned grammar/lexer package |
| Statement grammar | `EmscriptParserSlice.kt` | EBNF, language contract, formatter | High: braces are generated although EBNF calls line form canonical | Versioned grammar + parser |
| Expression grammar and precedence | `EmscriptParserSlice.kt` | `OperatorNormalization.kt`, workspace runtimes, EBNF | High: equality/comparison tiers differ from EBNF | Expression definition registry |
| Command names and aliases | `VisualTaskerCommandCatalog` | parser special cases, block types, runtime dispatch | High: special commands bypass normal catalog parsing | `CommandDefinition` registry |
| Command parameters/defaults | `VisualTaskerCommandCatalog` | parser special cases and runtime field readers | High: `vibrate` and `log` disagree with catalog | `CommandDefinition` registry |
| Command semantic class | `CommandCatalogKind` | runtime dispatch and category inference | High: many provider queries are `STATEMENT -> Void` | `CommandDefinition.semanticClass` |
| Command domain/category | `BlockCategories`, catalog entries | plugin ownership and capability enums | Medium: category is UI-oriented, not language-domain complete | `CommandDefinition.domain` |
| Command capability/provider | catalog runtime binding | `RuntimeCapabilityGate`, environment dispatch | High: metadata may claim live support without a dispatch branch | Registry plus adapter capability descriptor |
| Primitive argument types | `CommandArgumentType` | block connection type strings and runtime `EmscriptValue` | High: nominal names are not one type system | Versioned language type registry |
| Variables | parser IR, importer, workspace variable registry | Tasker `%var` defaults contradict parser identifiers | High | Core language model |
| Operators | parser tokens/precedence, `OperatorNormalization` | block field labels and runtime evaluators | Medium | Core expression registry |
| Parser AST/import IR | `EmscriptIrStatement`, `EmscriptIrExpression` | blockeditor `IrScript` is a second, smaller IR | High | One versioned semantic AST/IR boundary |
| Workflow intent | `WorkspaceDocument` (architectural `WorkflowDocument`) | manual EMScript draft is separate by design | Low if ADR 0001 is followed | Workflow domain |
| Text-to-workflow import | `EmscriptWorkspaceImporter` | source-line cursor heuristics | High: regenerated random block IDs and approximate spans | Import service using stable semantic IDs |
| Block-to-text generation | `IrGenerator` + `EmscriptGenerator` | parser accepts more than generator can represent | High | Semantic IR serializer |
| Workspace serialization | `WorkspaceSerializer` | not an EMScript serializer | Low, but often confused with text roundtrip | Workflow persistence layer |
| Validation | blockeditor `Validator`, parser validation, apply guard | checks different layers independently | Medium | Layered validators backed by registry |
| Block definitions | `DefaultBlockRegistry`, `StaticBlockRegistry`, catalog bindings | command blocks and dedicated blocks coexist | Medium | Registry-generated block projection |
| Flow node definitions | flowchart domain + `IrGraphFlowchartProjector` | category strings and node kind mappings | Medium | Registry-generated flow projection |
| Semantic graph | `IrGraphGenerator` and `IrModels.kt` | smaller `IrScript` pipeline also exists | High | Single workflow semantic IR |
| Flow projection | `IrGraphFlowchartProjector` | synthetic joins, terminator and facets add view semantics | Medium | Flow projection service |
| Projection directives | `rem.*` catalog entries, `RemFlowMetadata`, `IrGraphGenerator` | represented as statement commands with `CONTROL_FLOW` side effect | High: visually inert intent is typed as executable control flow | Separate projection-metadata schema |
| Dry-run semantics | `WorkspaceDryRunRuntime`, `EmscriptDryRunRuntime` | two interpreters; reporter functions return placeholders | High | One VM over semantic IR |
| Live semantics | `WorkspaceBasicRuntime` + environment adapters | catalog `liveImplemented` is broader than actual dispatch | Critical | Runtime binding registered by adapter |
| Capability gating | `RuntimeCapabilityGate` | catalog flags can authorize commands that execute no effect | Critical | Adapter registry verified against dispatch |
| Syntax highlighting | `SyntaxHighlighter` | large legacy hardcoded keyword set | Medium | Lexer token stream + registry |
| Formatting | `EmsScriptFormatter` | supports `FOR`, `FUNC`, `TRY`, `UNTIL` that parser rejects | High | Concrete-syntax-tree formatter |
| Documentation grammar | `docs/reference/emscript/STABLE_V1_GRAMMAR.ebnf` | arrays and canonical line control do not match implementation/generator | High | Versioned spec generated/verified by tests |
| Command documentation | blockeditor wiki documents | may lag the 127-entry catalog | Medium | Generated catalog reference |
| Roundtrip claims | `docs/audit/EMSCRIPT_WORKFLOW_ROUNDTRIP_MATRIX.md` | says Charts missing although 14 catalog entries exist | High | Generated conformance report |
| Tests/fixtures | parser/runtime/generator/IR/serializer tests | broad happy-path coverage but no authoritative spec suite | Medium | Versioned conformance suite |

## Current Authority Chain

The practical chain is currently:

1. `EmscriptParserSlice` decides whether text is accepted.
2. `VisualTaskerCommandCatalog` validates generic call names and raw argument
   shapes.
3. `EmscriptWorkspaceImporter` converts the parser IR into a
   `WorkspaceDocument`.
4. `WorkspaceDocument` is the workflow-intent authority.
5. `IrGraphGenerator` derives the editor-neutral graph used by Flowchart.
6. `IrGenerator` plus `EmscriptGenerator` derive generated EMScript.
7. `WorkspaceDryRunRuntime` and `WorkspaceBasicRuntime` interpret the workspace
   independently of the text parser IR.

No single component currently owns the entire language contract.

## Confirmed Duplicated Truth

- `wait`, `click`, `log`, `beep`, and `vibrate` have parser-specific branches in
  addition to catalog definitions.
- `vibrate` accepts 1..16 integer arguments in the parser, while the catalog
  declares one `DURATION_MS` argument.
- `log` accepts any parser expression, while the catalog declares one `TEXT`
  argument.
- `if` appears as three catalog entries with the same canonical name.
- `boolean` appears as two reporter entries with the same canonical name.
- Operators are represented as parser tokens, catalog aliases, normalized block
  field values, IR enums, and runtime branches.
- Provider query-like commands (`Tasker.lastResult`, `Chart.get`,
  `Shizuku.isAvailable`, etc.) are catalogued as `STATEMENT -> Void`, despite
  query naming and side effects.
- The formatter/highlighter advertise unsupported constructs (`FOR`, `FUNC`,
  `TRY`, `UNTIL`, `BREAK`, `CONTINUE`).

## Recommended Ownership, Not Yet Implemented

A future versioned `CommandDefinition` should own canonical name, aliases,
semantic class, domain, parameters, types, defaults, return type, side effects,
capability, provider requirement and lifecycle version. Parser, generators,
BlockEditor, Flowchart, completion, documentation and runtime adapters should
consume it. Core grammar and expression semantics must remain a separate,
versioned language definition. Projection directives should be a third,
runtime-inert metadata schema rather than ordinary action commands.
