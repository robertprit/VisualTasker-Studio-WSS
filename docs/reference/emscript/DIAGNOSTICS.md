# EMScript Diagnostics — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/emscript/v0.1/EMSCRIPT_DIAGNOSTIC_CODES.md`

## Diagnostic Contract

Der alte v0.1-Vertrag sah für jedes Diagnostic mindestens folgende Informationen vor:

- `severity`
- `phase`
- `sourceSpan`
- optional `capabilityId`
- optional `runtimeSessionReference`
- `recoverable`

Recovery bedeutet nicht stillen Erfolg. Ein recoverable Runtime-Fehler darf von einer expliziten Fehlergrenze behandelt werden.

## Erhaltene stabile Codes

| Code | Phase | Bedeutung |
| --- | --- | --- |
| `emscript.parse.unknown_command` | PARSE | unbekannter Line Command |
| `emscript.parse.unknown_capability` | PARSE | Namespace/Member nicht registriert |
| `emscript.parse.unknown_argument` | VALIDATE | unbekanntes Named Argument |
| `emscript.parse.duplicate_argument` | VALIDATE | Argument mehrfach angegeben |
| `emscript.parse.missing_argument` | VALIDATE | Pflichtargument fehlt |
| `emscript.compat.legacy_alias` | NORMALIZE | akzeptierte Legacy-Schreibweise normalisiert |
| `emscript.compat.unsupported_alias` | NORMALIZE | bekannte, aber nicht unterstützte Legacy-Form |
| `emscript.runtime.capability_unavailable` | RUNTIME | kein Provider unterstützt die Capability |
| `emscript.runtime.provider_failure` | RUNTIME | Provider hat ausgeführt und ist fehlgeschlagen |
| `browser.url.invalid` | VALIDATE | ungültige URL |
| `browser.target.unavailable` | RUNTIME | explizites Browser-Ziel nicht verfügbar |
| `browser.session.missing` | RUNTIME | benötigte BrowserSession fehlt |
| `browser.session.mismatch` | RUNTIME | Session gehört nicht zu Run/Source |
| `browser.event.uncorrelated` | RUNTIME | Navigation Event nicht eindeutig korreliert |
| `browser.navigation.timeout` | RUNTIME | erwartetes Navigation Event kam nicht rechtzeitig |
| `browser.navigation.failed` | RUNTIME | korrelierte Navigation fehlgeschlagen |
| `browser.config.invalid` | VALIDATE | ungültige Parameterkombination |
| `tasker.slot.invalid` | RUNTIME | unbekannter Compatibility Slot/Alias |

## WSS-Regel

Diese Codes sind historische stabile Referenzen, aber nicht automatisch der vollständige heutige Diagnostic-Katalog. Neue WSS-Diagnostics sollten:

- stabile IDs besitzen;
- Phase und Severity klar trennen;
- Workflow-, Runtime-, Provider-, Worldview- und Projection-Fehler nicht vermischen;
- keine UI-Texte als maschinenlesbare Identität verwenden;
- als strukturierte Daten in LogConsole, RailTrace, EMScript Editor und AI-Kontext projizierbar sein.

## Aktuelle WSS Runtime-Codes

Runtime- und DryRun-Events transportieren einen optionalen `diagnosticCode`.
Dieser Code wird aus dem Command-Capability-Descriptor übernommen und in
FlowRuntime-Diagnostics sowie `visualtasker.runtime-events` projiziert. Dadurch
können FlowEditor, RailTrace, LogConsole, Debug Panel und Junktor denselben
maschinellen Fehlergrund verwenden, statt lokale Anzeigenamen wie
`CAPABILITY_TERMUX` als Identität zu missbrauchen.

| Code | Phase | Bedeutung |
| --- | --- | --- |
| `CAPABILITY_ADAPTER_REQUIRED` | RUNTIME | Command ist bekannt, benötigt aber einen noch nicht live verfügbaren Adapter |
| `CAPABILITY_CATALOG_MISSING` | RUNTIME | Runtime sieht einen Command, der im zentralen Command-Katalog fehlt |
| `CAPABILITY_BLOCKED` | RUNTIME | Fallback, wenn ein Capability-Warning keinen spezifischen Descriptor-Code hat |

## Drift-Risiko

Wenn Parser, Blockeditor, Flowchart und Runtime für denselben Fehler unterschiedliche IDs oder Bedeutungen verwenden, entsteht semantischer Drift. Der Diagnostic-Katalog sollte deshalb langfristig aus einem gemeinsamen Contract oder Registry-Modell gespeist werden.
