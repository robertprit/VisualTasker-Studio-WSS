# EMScript Language Contract — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/emscript/v0.1/EMSCRIPT_LANGUAGE_CONTRACT.md`
>
> **Authority:** Aktuelle EMScript-Implementierung, aktuelle ADRs und `VISUALTASKER_ARCHITECTURE_CONTRACT.md` haben Vorrang.

## Vertragsidee

EMScript ist eine Projektion derselben Workflow-Semantik, die auch Blockeditor und Flowchart darstellen. Parser-Syntax, Blocktypen, Flow-Nodes, Runtime-Instruktionen und Provider-Aufrufe dürfen deshalb keine konkurrierenden Wahrheiten bilden.

## Hybrid-Syntax

Der alte Vertrag unterscheidet zwei Ebenen:

1. line-orientierte Kernsyntax für Kontrollfluss, Variablen, Waits, Funktionen und Fehlerbehandlung;
2. typisierte Capability Calls für erweiterbare Funktionen.

Beispiel:

```emscript
Browser.open(url="https://example.org", target=BrowserTarget.SYSTEM)
SET tab = Browser.openCustomTab(url="https://example.org")
```

Capability Calls werden über registrierte Descriptoren aufgelöst. Sie sind weder Reflection noch frei erfundener Object Dispatch.

## Stabile Grundregeln aus v0.1

- `SET` war kanonisch; `LET` wurde als Legacy-Alias akzeptiert.
- Generatoren sollten nur kanonische Syntax emittieren.
- Parser dürfen dokumentierte Legacy-Formen normalisieren.
- unbekannte, doppelte oder fehlende Argumente müssen deterministisch diagnostiziert werden.
- Provider-Abwesenheit ist ein expliziter Fehler und kein stilles `false`.
- Capability Calls können Side Effects haben; Value Constructors nicht.
- Contract Status und Implementation Status sind getrennte Aussagen.

## WSS-Anpassung

Die alte Aussage, dass die Workflow Domain die alleinige semantische Authority ist, wird präzisiert:

- WorkflowDocument besitzt Intent-Wahrheit.
- Worldview besitzt Reality-/Knowledge-Wahrheit.
- EMScript beschreibt Workflow-Intent und darf Observation/Worldview-Daten referenzieren, besitzt sie aber nicht.
- Runtime führt validierte Workflow-Semantik aus.

## Legacy- und Compatibility-Prinzip

Legacy-Syntax darf akzeptiert und normalisiert werden, ohne dass sie neue kanonische API wird. Kompatibilität ist ein Parser-/Normalizer-Thema; Generatoren sollen keine historischen Schreibweisen neu verbreiten.

## Offene Prüfpunkte vor einer neuen normativen EMScript-Version

Diese alte Referenz legt nicht fest, was im heutigen WSS bereits implementiert ist. Vor einer normativen WSS-Version müssen insbesondere gegen Code und Tests geprüft werden:

- aktueller Typvertrag (`String`, `Number`, `Bool`, `Any` usw.);
- `LET`/`SET`-Semantik im aktuellen Parser/Generator;
- Funktionen, Loops und TRY/CATCH;
- strukturierte Werte;
- Await/Event-Semantik;
- Browser-, Tasker-, Shizuku-, Termux- und weitere Capabilities;
- Round-Trip EMScript ↔ WorkflowDocument ↔ Block/Flow.
