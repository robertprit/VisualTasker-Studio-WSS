---
name: visualtasker-wss-emscript
description: Guides EMScript parser, command catalog, editor, dry-run runtime, capability gates, and Workflow projection work in VisualTasker Studio WSS.
disable-model-invocation: true
---

# VisualTasker WSS EMScript

## Rules

- Keep canonical command syntax explicit and tested.
- Parser/importer changes must roundtrip with WorkspaceDocument where possible.
- Dry-run must distinguish unsupported capability from parser/runtime failure.
- Adapter-gated commands should report diagnostics without falsely marking core dry-run as broken.
- EMScript editor applies changes through guarded WorkspaceDocument replacement.
- Do not add UI-only command semantics.

## Validate

Use focused parser/runtime tests and compile:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.visualtasker.wss.emscript.parser.*' \
  --tests 'com.visualtasker.wss.emscript.runtime.*' \
  :app:compileDebugKotlin \
  --console=plain
```
