# C7-0 Wiki-Informationsarchitektur

Stand: 2026-10-11

Status: Publikationsentwurf. Das GitHub-Wiki ist aktiviert, aber noch nicht initialisiert.

## 1. Grundsatz

Das Wiki ist eine leserfreundliche Projektion versionierter Repository-Dokumente, keine zweite
Architektur- oder Feature-Wahrheit. Aenderungen an Status, Scope oder Vertraegen werden zuerst im
Repository reviewt und danach ins Wiki uebernommen.

Jede Wiki-Seite enthaelt:

- `Stand` oder `Last reviewed`,
- den Reifegrad (`IMPLEMENTED`, `PARTIAL`, `PLANNED`, `DEFERRED`),
- Links auf die kanonischen Quelldokumente,
- sichtbare Voraussetzungen und Grenzen,
- keine lokalen Dateisystempfade.

## 2. Zielgruppen

1. **Interessierte Nutzer:** Was ist VisualTasker Studio, was funktioniert heute, was noch nicht?
2. **Tester:** Wie wird gebaut, installiert, gestartet und reproduzierbar getestet?
3. **Beitragende:** Wo liegen Vertrage, Projektionen, Provider und Qualitaetsgates?
4. **Integratoren:** Welche Plugin-/Companion-Grenzen und Berechtigungen gelten?

Eine Seite soll genau eine primaere Zielgruppe bedienen. Tiefe Audit- und Milestone-Protokolle
bleiben im Repository und werden nur verlinkt.

## 3. Vorgeschlagene Navigation

1. `Home`
2. `Getting-Started`
3. `Project-Status`
4. `Architecture`
5. `Editors`
6. `EMScript`
7. `Runtime-and-Providers`
8. `Recorder-Perception-and-RailTrace`
9. `Projects-Resources-and-Assets`
10. `Plugins-and-Companion-Apps`
11. `Build-Test-and-Debug`
12. `Permissions-Privacy-and-Security`
13. `Known-Limitations`
14. `Roadmap`
15. `Glossary`

Die Sidebar soll auf diese 15 stabilen Einstiege beschraenkt bleiben. Detailseiten werden von dort
aus verlinkt, aber nicht alle in die Hauptnavigation aufgenommen.

## 4. Seitenvertrag

### Home

- Zwei-Satz-Produktbeschreibung.
- Sichtbarer Pre-Alpha-Hinweis.
- Drei bestaetigte Kernfaehigkeiten.
- Drei wichtigste aktuelle Grenzen.
- Links zu Getting Started, Status und Architecture.

Quelle: freigegebene Root-README und Feature-Matrix.

### Getting Started

- JDK 17, Android SDK 36, Android Studio/Gradle.
- Clone mit Submodulen.
- Debug-Build, Tests, Installation und Start als getrennte Schritte.
- Accessibility-/Berechtigungshinweise ohne automatische Freigabe.

Quelle: Builddateien, Testdokumentation und freigegebene Smoke-Skripte.

### Project Status

- 146-Feature-Verteilung.
- Scope-Klassen `CORE_1_0`, `OPTIONAL_1_0`, `POST_1_0`, `EXPERIMENTAL`.
- Kritischer Pfad und sieben Release-Blocker.
- Keine automatische Ersetzung durch eine manuell gepflegte Prozentanzeige.

Quelle: `docs/FEATURE_STATUS_MATRIX.md` und `docs/architecture/RELEASE_1_0_SCOPE.md`.

### Architecture

- Workflow = Intent.
- Worldview = Reality/Knowledge.
- Runtime = Bruecke.
- Editoren sind Projektionen, keine eigenen Wahrheiten.
- AI/RAG darf vorschlagen und erklaeren, aber nicht still mutieren.

Quelle: `VISUALTASKER_ARCHITECTURE_CONTRACT.md` und kanonische Architektur-Audits.

### Editors

- Text-, Block- und FlowEditor auf demselben kanonischen Dokument.
- Source Apply, Transactions, History und Roundtrip.
- Produktive Funktionen von offenen visuellen/UX-Gates trennen.

Quelle: M4-Conformance-Dokumente und Feature-Matrix E-01 bis E-17.

### EMScript

- tatsaechlich unterstuetzte V1-Untermenge.
- Lexer/Parser/AST-IR/Validator/Generator/Runtime korrekt benennen.
- Kein Compiler-/Bytecode-/VM-Claim, solange diese Komponenten fehlen.
- Beispiele nur aus getesteten Fixtures ableiten.

Quelle: Truth Map, Conformance Audit und spaeter freigegebene V1-Referenz.

### Runtime and Providers

- deterministischer Runner und typed Results,
- produktive Wait-/Touch-/Gesture-/Multitouch-Pfade,
- begrenzter A11y-Zielpfad,
- offene Provider-Policy und fehlender Headless Host.

Quelle: M5-0 bis M5-8 und Runtime Provider Matrix.

### Recorder, Perception and RailTrace

- RecordingSession -> Raw Evidence -> StepCandidate -> Review -> ReviewedStep,
- RailTrace, Playback und Recovery,
- offener Review-to-Workflow-Publish-Pfad,
- A11y/Capture/OCR/Vision mit Herkunft und Freshness.

Quelle: Recorder-Architektur, M7-Scope und Feature-Matrix V/T.

### Projects, Resources and Assets

- heutige Session-/Resource-Persistenz,
- offene Projektportabilitaet,
- `.ema`-/VisualAsset-Grenzen,
- Backup/Restore als Release-Gate.

Quelle: Feature-Matrix D und M8-Plan.

### Plugins and Companion Apps

- BlockEditor und Flowchart als Kernprojektionen,
- ShapeMaker, ChartGraph, Vision, IME und externe Adapter mit ihrem echten Reifegrad,
- kontrollierte Abwesenheit optionaler Provider,
- keine implizite Produktgarantie durch vorhandenen Quellcode.

Quelle: Plugin-Inventar, Feature-Matrix I und jeweilige Readiness-Dokumente.

### Build, Test and Debug

- automatisierte Gates und manuelle Abnahmematrix,
- Build/Install/Foreground getrennt von UI-/Runtime-Akzeptanz,
- bekannte Geraete- und Langlauf-Luecken.

Quelle: `docs/testing/`, `docs/SMOKE_TEST_MATRIX.md`, Smoke-Skripte.

### Permissions, Privacy and Security

- Accessibility, Foreground Service, Screenshots/Recording und externe Adapter,
- lokale Daten, Logs, Screenshots und Modellartefakte,
- Human-in-the-loop- und Mutationsgrenzen,
- verantwortliche Meldung von Sicherheitsproblemen erst nach Supportfreigabe.

Quelle: Manifest/Provider-Dokumentation, Release-Scope und spaetere `SECURITY.md`.

### Known Limitations und Roadmap

- direkt aus aktuellem Scope ableiten,
- Datum und Matrixrevision nennen,
- keine erledigten Meilensteine als offene Produktversprechen wiederholen.

### Glossary

Mindestens: WorkspaceDocument, Workflow, Worldview, Projection, Provider, Observation, Evidence,
Record, RailTrace, Junktor, VisualAsset, WetRun, DryRun und WSS.

## 5. Quellen-Mapping

| Wiki-Bereich | Primaere Quelle | Sekundaere Quelle |
| --- | --- | --- |
| Status | `docs/FEATURE_STATUS_MATRIX.md` | `docs/FEATURE_INVENTORY.md` |
| Scope/Roadmap | `docs/architecture/RELEASE_1_0_SCOPE.md` | `RELEASE_1_0_PLAN.md`, `docs/ROADMAP.md` |
| Architektur | `VISUALTASKER_ARCHITECTURE_CONTRACT.md` | M2/M3/M4-Audits |
| EMScript | `docs/emscript/EMSCRIPT_LANGUAGE_TRUTH_MAP.md` | Conformance Audit, Reference |
| Runtime | M5-Dokumente | Runtime Provider Matrix |
| Recorder | Recorder Ownership/Ecology | Feature-Matrix T |
| Build/Test | `docs/testing/README.md` | Smoke-Matrix und Skripte |
| Grenzen | aktualisierte `docs/KNOWN_LIMITATIONS.md` | Release-Scope |

## 6. Uebernahme der BlockEditor-Wiki-Quellen

Die 19 Seiten im BlockEditor-Submodule sind wertvolle Quelltexte, aber keine automatisch
freigegebene WSS-Wiki-Kopie.

| Seitengruppe | Behandlung |
| --- | --- |
| BlockEditor Overview/Layout/Docking | nach WSS-Vertragsabgleich uebernehmen |
| IR Synchronization/Flowchart Layout | mit M4-Conformance und Flowchart-Quellen zusammenfuehren |
| EMScript Overview/Commands/Core Types/Find API | nur aus aktueller Truth Map neu ableiten |
| Legacy Blockly XML Import | als Migrations-/Historienseite, nicht als Kernfeature |
| Roadmap/Known Limitations | nicht kopieren; aus WSS-Scope neu erzeugen |
| Home | durch WSS-Home ersetzen |

Jede uebernommene Aussage benoetigt eine aktuelle Source-Referenz. Alte Statusmarker werden nicht
stillschweigend entfernt, sondern gegen die Feature-Matrix entschieden.

## 7. Publikationsworkflow

1. Kanonische Quellen auf denselben Auditstand bringen.
2. Wiki-Seiten unter einer versionierten Source-Struktur erstellen.
3. Link-, Pfad-, Claim- und Sprachreview ausfuehren.
4. Home und Sidebar zuerst freigeben.
5. GitHub-Wiki initialisieren.
6. Freigegebene Seiten in einem nachvollziehbaren Batch publizieren.
7. Source-Commit und Wiki-Commit gegenseitig referenzieren.
8. Bei jeder Statusaenderung zuerst die Feature-Matrix aktualisieren.

Ein spaeteres Publish-Script darf nur freigegebene Markdown-Dateien spiegeln. Es darf keine
Feature- oder Reifegradtexte generieren, die nicht in den kanonischen Quellen stehen.

## 8. Qualitaetsgate je Seite

- Titel und Zielgruppe klar.
- Stand/Revision vorhanden.
- Alle Funktionsclaims einem Status und Nachweis zugeordnet.
- Keine lokalen Pfade oder private Daten.
- Keine toten relativen Links.
- Keine Stable-/RC-Sprache ohne Release-Gate.
- Deutsche Begriffe konsistent; insbesondere `Junktor`.
- Optionales/Experimentelles sichtbar vom Core getrennt.
- Berechtigungs-, Datenschutz- und Datenverlustgrenzen nicht versteckt.

## 9. Offene Wiki-Entscheidungen

- Deutsch, Englisch oder zweisprachig?
- GitHub-Wiki als Ziel oder GitHub Pages/Dokumentationssite?
- Welche 15 Einstiegsseiten werden fuer die erste Publikation gekuerzt?
- Werden BlockEditor-Detailseiten im WSS-Wiki gespiegelt oder im Submodule belassen?
- Wer darf Status-/Roadmap-Seiten publizieren?
- Soll ein kontrolliertes Mirror-Script spaeter Teil des Repositories werden?
