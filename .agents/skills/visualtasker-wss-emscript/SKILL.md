---
name: visualtasker-wss-emscript
description: Guides EMScript parser, command catalog, editor, dry-run runtime, capability gates, and Workflow projection work in VisualTasker Studio WSS.
---

# VisualTasker WSS EMScript

## Rules

- Keep canonical command syntax explicit and tested.
- Prefer canonical function-style EMScript commands such as `wait(...)`, `click(...)`, `beep(...)`, and `vibrate(...)`; keep legacy aliases only as importer compatibility where supported.
- `VisualTaskerCommandCatalog` is the registry backbone for command ids, canonical names, aliases, arguments, side effects, capability ownership, block bindings, flowchart bindings, and runtime bindings.
- Parser/importer changes must roundtrip with WorkspaceDocument where possible.
- Dry-run must distinguish unsupported capability from parser/runtime failure.
- Adapter-gated commands should report diagnostics without falsely marking core dry-run as broken.
- EMScript editor applies changes through guarded WorkspaceDocument replacement.
- Do not add UI-only command semantics.

## Boundaries

- EMScript text is not the only workflow truth.
- Command support requires parser/importer, catalog metadata, Workspace mapping, EMScript generation, diagnostics, and runtime/capability behavior to stay aligned.
- Plugin-backed commands may be known in the catalog before live runtime providers exist.

## Validate

Use focused parser/runtime tests and compile:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.visualtasker.wss.emscript.parser.*' \
  --tests 'com.visualtasker.wss.emscript.runtime.*' \
  :app:compileDebugKotlin \
  --console=plain
```
