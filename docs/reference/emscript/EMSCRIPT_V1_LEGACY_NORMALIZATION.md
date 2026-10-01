# EMScript v1 Legacy Normalization

Status: Normative V1 compatibility policy
Earliest removal of a supported V1.x legacy alias: EMScript 2.0

Normalization is semantic and parser-aware. `SAFE_TEXT_NORMALIZATION` means a
canonical V1 text can be produced without information loss; it does not permit
blind regular-expression replacement.

## Migration Classes

- `SAFE_TEXT_NORMALIZATION`: importer can emit equivalent canonical V1 text.
- `STRUCTURAL_MIGRATION`: importer must reconstruct identity, extent or typed
  payload before canonical output exists.
- `DEPRECATED_READ_ONLY`: accepted by V1.x with warning but never emitted.
- `REMOVED`: not part of V1 input or output.

## Normalization Table

| Legacy syntax | V1 canonical syntax | Migration class | Diagnostic | Information-loss risk | Earliest removal |
| --- | --- | --- | --- | --- | --- |
| `LET x = 1`, `SET x = x + 1` | `LET §x = 1`, `SET §x = §x + 1` | SAFE_TEXT_NORMALIZATION | `LEGACY_SYNTAX` | Low if symbol resolution is scope-aware | 2.0 |
| optional `;` | newline | SAFE_TEXT_NORMALIZATION | `LEGACY_SYNTAX` | None outside open delimiters | 2.0 |
| brace IF/loop forms | `END IF`, `END WHILE`, `END REPEAT` | SAFE_TEXT_NORMALIZATION | `LEGACY_SYNTAX` | Low with CST; high with text replacement | 2.0 |
| `ELSE IF` | `ELSEIF` | SAFE_TEXT_NORMALIZATION | `LEGACY_SYNTAX` | None | 2.0 |
| `REM comment` | `// comment` | SAFE_TEXT_NORMALIZATION | `LEGACY_SYNTAX` | None when `rem.*` is tokenized separately | 2.0 |
| `click("Login")` | `clickText("Login")` | SAFE_TEXT_NORMALIZATION | `LEGACY_ALIAS` | None after argument typing | 2.0 |
| `clickPoint(x, y)` | `click(x, y)` | SAFE_TEXT_NORMALIZATION | `LEGACY_ALIAS` | None | 2.0 |
| capitalized registered provider namespace | canonical registered namespace | SAFE_TEXT_NORMALIZATION | `LEGACY_ALIAS` | None for a registered alias | 2.0 |
| `rem.flowBreak(...)` | `@flow.break(...)` | SAFE_TEXT_NORMALIZATION | `LEGACY_ALIAS` | None for the point form | 2.0 |
| `rem.layoutHint(...)` | `@flow.layout(...)` | SAFE_TEXT_NORMALIZATION | `LEGACY_ALIAS` | None for the point form | 2.0 |
| raw `touch(sequence)` | typed `touch.*`, `PointerPath` or `MultiPath` | STRUCTURAL_MIGRATION | `STRUCTURAL_MIGRATION_REQUIRED` | High: pointer identity and timing may be encoded in payload | 2.0 |
| `rem.region(...)` | paired/identified Projection AST | STRUCTURAL_MIGRATION | `STRUCTURAL_MIGRATION_REQUIRED` | High: extent and membership are implicit | 2.0 |
| `rem.variableBulk(...)` | paired group/bulk Projection AST | STRUCTURAL_MIGRATION | `STRUCTURAL_MIGRATION_REQUIRED` | High: member identity and order may be implicit | 2.0 |
| `rem.expressionCapsule(...)` | paired capsule Projection AST | STRUCTURAL_MIGRATION | `STRUCTURAL_MIGRATION_REQUIRED` | High: expression ownership may be implicit | 2.0 |
| `rem.group(...)` | `@group.start(id)` / `@group.end(id)` | STRUCTURAL_MIGRATION | `STRUCTURAL_MIGRATION_REQUIRED` | High: end and nesting may be unknown | 2.0 |
| `rem.offPageOut/In(...)` | stable-ID paired off-page directives | STRUCTURAL_MIGRATION | `STRUCTURAL_MIGRATION_REQUIRED` | High: endpoint pairing may be ambiguous | 2.0 |
| `WAIT expr`, `CLICK "text"`, whitespace `BEEP` | canonical calls | DEPRECATED_READ_ONLY | `LEGACY_SYNTAX` | Depends on legacy argument parser | 2.0 |
| registered historical command aliases | canonical CommandDefinition name | DEPRECATED_READ_ONLY | `LEGACY_ALIAS` | None only when alias is unique | 2.0 |
| `multiSwipe.build/add/go` Draft convenience | `multiPath(...)` plus `multiSwipe(...)` | REMOVED | `REMOVED_CONSTRUCT` | Not applicable; never frozen as V1 | 1.0 |
| independent `pinch(...)` semantics | `push(...)` or `pull(...)` chosen explicitly | REMOVED | `REMOVED_CONSTRUCT` | High if direction is unknown | 1.0 |
| source-level `null` or `undefined` | no V1 equivalent | REMOVED | `REMOVED_CONSTRUCT` | Not applicable; never valid V1 | 1.0 |

## Structural Migration Rule

`STRUCTURAL_MIGRATION` entries MUST retain their original source and metadata
until a migration service can prove a lossless typed mapping. They MUST be
reported as `CANONICALIZATION_REQUIRES_STRUCTURAL_MIGRATION`; a serializer MUST
NOT invent canonical text.

## Runtime Invariant

Legacy `rem.*` constructs are projection metadata. Importing, ignoring or
removing them MUST NOT add, remove or reorder executable runtime actions.
