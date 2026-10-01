# EMScript v1 Freeze

EMScript v1 language contract frozen.

- Freeze date: 2026-09-21
- Language version: 1.0
- Normative specification: `EMSCRIPT_V1_SPEC.md`
- Normative grammar: `EMSCRIPT_V1_GRAMMAR.ebnf`
- Decision record: `EMSCRIPT_V1_SPEC_DECISIONS.md`
- Machine-readable decisions: `EMSCRIPT_V1_DECISION_MATRIX.csv`
- Legacy policy: `EMSCRIPT_V1_LEGACY_NORMALIZATION.md`
- Conformance fixtures: `conformance/v1/`
- Fixture manifest: `conformance/v1/manifest.csv`

The canonical serializer writes V1 syntax only. Documented legacy forms remain
readable throughout V1.x and may be removed no earlier than EMScript 2.0.
Structural legacy constructs are never rewritten when identity, extent or
payload would be lost.

Implementation conformance is NOT yet complete. This freeze defines the target
contract and fixtures; it does not certify the current parser, registry,
editors, IR or runtime.

Next phase: **M1 - Language Core & CommandDefinition Conformance**.
