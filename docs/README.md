# VisualTasker Studio WSS — Dokumentation

Diese Ebene ist die **Landkarte der Projektdokumentation**. Die Root-README erklärt Idee, Motivation und Gesamtbild; hier geht es darum, schnell die richtige Quelle für eine konkrete Frage zu finden.

## Autorität

Nicht jede Markdown-Datei ist ein Architekturvertrag. Bei Widersprüchen gilt grundsätzlich:

1. `../VISUALTASKER_ARCHITECTURE_CONTRACT.md`
2. akzeptierte ADRs unter `adr/`
3. aktuelle normative Fachverträge
4. aktuelle Referenzdokumente
5. Roadmap / Audit / Planungsdokumente
6. Legacy-Referenzen

Ein Audit beschreibt einen Befund oder eine offene Prüfung. Es wird nicht allein dadurch zur neuen Architektur.

## Einstieg nach Frage

| Frage | Einstieg |
|---|---|
| Was ist VTS WSS überhaupt? | `../README.md` |
| Welche Regeln dürfen Module nicht verletzen? | `../VISUALTASKER_ARCHITECTURE_CONTRACT.md` |
| Was ist als Nächstes geplant? | `ROADMAP.md` |
| Wie werden Workflow und beobachtete Realität getrennt? | `WORLDVIEW_ARCHITECTURE.md` |
| Wie wird Fachsemantik visuell dargestellt? | `VISUAL_ABSTRACTION_LAYER.md` |
| Welche konsolidierten Verträge gibt es? | `reference/README.md` |
| Welche Systemtests existieren? | `testing/README.md` |
| Welche Architekturfragen sind als Nächstes zu prüfen? | `audit/NEXT_ARCHITECTURE_AUDIT_BRIEFING.md` |
| Wie sollen Prompts und Skills in WSS eingeordnet werden? | `WSS_PROMPTS_AND_SKILLS.md` |

## Dokumentfamilien

### Architecture / ADR

`adr/` hält bewusst getroffene Architekturentscheidungen fest. Eine ADR soll eine Entscheidung, ihre Gründe und Konsequenzen dokumentieren — nicht den kompletten Projektzustand duplizieren.

### Reference

`reference/` enthält langlebige fachliche Verträge und adaptierte Referenzen. Historische Dokumente sind dort ausdrücklich als Legacy/Reference gekennzeichnet, wenn sie nicht normative Gegenwartsarchitektur sind.

### Runtime

`runtime/` enthält Runtime- und Provider-spezifische Dokumentation. Runtime koppelt Workflow-Intent mit tatsächlicher Ausführung und beobachteter Realität; sie wird nicht zur zweiten Workflow-Wahrheit.

### Testing

`testing/` enthält Teststrategie, Smoke Tests und Systemkataloge. Besonders wichtig sind Roundtrip, Projection Sync, Runtime/Recording und Worldview-Grenzen.

### Audit

`audit/` ist der Arbeitsbereich für Code-vs-Contract-Prüfungen und offene Konsolidierung. Ergebnisse sollen entweder in konkrete Reparatur-Slices, ADRs oder Referenzverträge überführt werden.

## Grundbegriffe

```text
WorkflowDocument = Intent-Wahrheit
Worldview        = Reality-/Knowledge-Modell
Runtime          = Kopplung von Intent und Reality
Projection       = Sicht auf diese Modelle, keine eigene Wahrheit
```

Editoren und Panels dürfen lokale UI-/Draft-Zustände besitzen. Dauerhafte fachliche Bedeutung muss jedoch über explizite Domain-Verträge laufen.

## Dokumentationsregel

Wenn ein neuer Bereich entsteht, nicht automatisch eine zweite Beschreibung derselben Architektur anlegen. Bevorzugt wird:

- Root README für Projekt und Motivation;
- lokales README für Navigation und Modulgrenzen;
- Reference für fachliche Verträge;
- ADR für Entscheidungen;
- Audit für Befunde und offene Fragen;
- Testing für beweisbares Verhalten.

Damit bleibt Dokumentation eine Hilfe und wird nicht zur fünften Projektion mit eigenem Source of Truth.
