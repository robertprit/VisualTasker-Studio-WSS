# VisualTasker Studio WSS 1.0 Release Scope

Stand: 2026-10-11

## Entscheidung

Version 1.0 liefert eine lokale, kanonische Visual-Automation-IDE mit drei konformen Editoren,
deterministischer Android-Runtime, Recorder/RailTrace, grundlegender semantischer Zielaufloesung,
stabiler Projektpersistenz und belastbarem Recovery. Optionale Provider und experimentelle
Produkte duerfen 1.0 nicht blockieren.

Dieses Dokument konkretisiert `RELEASE_1_0_PLAN.md`. Die Feature-IDs stammen aus
`../FEATURE_INVENTORY.md`.

## Verbindliche Scope-Klassen

- `CORE_1_0`: muss fuer Stable 1.0 geschlossen oder mit einer explizit akzeptierten Grenze
  abgenommen sein.
- `OPTIONAL_1_0`: darf in 1.0 enthalten sein, blockiert Alpha, Beta, RC oder Stable aber nicht.
- `POST_1_0`: bleibt architekturkompatibel, wird jedoch nicht vor Stable 1.0 fertiggestellt.
- `EXPERIMENTAL`: Konzept oder Forschungsflaeche ohne Produkt- oder Kompatibilitaetsversprechen.

Die folgende Zuordnung ist lueckenlos fuer alle 146 inventarisierten IDs. Bereiche mit Ausnahmen
sind absichtlich einzeln aufgeteilt.

| Scope | Feature-IDs |
| --- | --- |
| CORE_1_0 | A-01..A-11, A-16; R-01..R-16, R-18; E-01..E-05, E-09..E-15; W-01..W-09; H-01..H-05; V-01..V-08, V-11..V-12; T-01..T-10; D-01..D-03, D-05, D-07..D-09; I-01..I-02, I-12; Q-01..Q-14 |
| OPTIONAL_1_0 | E-06..E-08; H-06..H-07, H-09..H-11; V-09..V-10; T-11; D-04, D-06; I-03..I-10, I-13 |
| POST_1_0 | A-12..A-15; R-17; W-10; H-12; T-12; D-10..D-12; I-11; X-05, X-08..X-10 |
| EXPERIMENTAL | E-16..E-17; H-08; X-01..X-04, X-06..X-07 |

`WorldviewGraph`, Morf, Patchbay, Virtual App Sessions, RAG-Ausbau und zusaetzliche KI-Agenten
sind damit ausdruecklich keine 1.0-Blocker. Eine spaetere Hochstufung benoetigt eine dokumentierte
Scope-Aenderung und darf nicht implizit durch vorhandene Klassen erfolgen.

## Verpflichtender 1.0-Umfang

### Kanonisches Projekt und EMScript

- A-01 bis A-08 sowie A-10/A-11 bleiben geschlossen und regressionsgesichert.
- A-09 und A-16 muessen die tatsaechlich unterstuetzte V1-Untermenge vollstaendig dokumentieren.
- Unbekannte/ungueltige Sprache bleibt fail-closed; Opaque-Roundtrip ist kein 1.0-Muss.
- Ein Workflow-Root reicht fuer 1.0. Mehrere Roots, Subworkflows und Funktionsbibliotheken sind kein
  Pflichtumfang.

### Runtime

- R-01 bis R-12 bleiben produktiv und regressionsgesichert.
- R-13: mindestens deterministisches `clickText`/Elementziel ueber Accessibility inklusive
  Mehrdeutigkeitsdiagnose und Bounds-Nachweis.
- R-14: eine verbindliche Provider-Auswahl-, Availability-, Permission- und Fehlerpolicy.
- R-15/R-16: genau ein prozessweiter Headless Runtime Host mit sichtbarer Foreground-Ausfuehrung,
  Stop, Cancellation, Recovery und UI-Reattachment.
- Langlauf- und Lifecycle-Gate aus R-18. Ein vollstaendiger Breakpoint-Debugger ist optional.
- R-17 wird aus 1.0 entfernt, solange der M4-4-Sprachvertrag nicht separat freigegeben wird.

### Editoren und Workspace

- E-01 bis E-05 und E-09 bis E-14 erhalten ein gemeinsames M6-Produktgate.
- AutoArrange/Routing muss fuer die dokumentierten Standardstrukturen deterministisch und ohne
  Datenverlust sein; perfekte Darstellung beliebiger Grossgraphen ist kein Stable-Kriterium.
- W-01 bis W-05 werden ueber M6-W konsolidiert: Fokus, Z-Order, Drag/Resize, Snap/Dock,
  harte Grenzen, Persistenz und Recovery.
- W-06/W-07 muessen fuer die freigegebenen Screenshot-/Record-Workflows lifecycle-sicher sein.
- Historische Panel-Aliase werden migriert oder explizit als Alias behandelt.

### Perception, Recorder und Inspection

- V-01 bis V-07 im fuer A11y und Capture erforderlichen Umfang.
- V-08 OCR ist Bestandteil des Alpha-Ziels, sofern die semantischen Kernworkflows sonst nicht
  robust identifizierbar sind.
- T-01 bis T-09 bleiben geschlossen bzw. werden mit echten Records abgenommen.
- T-10 wird bis zur expliziten Workflow-Proposal-Vorschau und menschlichen Freigabe geschlossen.
- Record, Dry/Wet Run und WatchDog bleiben getrennte Zeit- und Eigentumsmodelle.

### Projekt, Ressourcen und Release

- D-01/D-02/D-03/D-07/D-09: ein dokumentiertes Projektformat mit Save/Load, Ressourcenreferenzen,
  Migration, Backup/Restore und Portabilitaet.
- D-08: ein minimaler Workspace Explorer fuer Workflows und verwendete Ressourcen. Vollstaendige
  Where-Used- und Multi-Scope-Funktionen koennen nach 1.0 folgen.
- Q-01 bis Q-05 bleiben dauerhafte Gates.
- Q-06 bis Q-10 werden vor RC geschlossen.
- Keine offenen P0-/P1-Fehler und keine undokumentierten Datenverlustrisiken.

## Optional fuer 1.0

Diese Funktionen duerfen aufgenommen werden, wenn sie den kritischen Pfad nicht verlaengern und
ein eigenes Permission-/Failure-Gate besitzen:

- V-09 OpenCV Template Matching.
- V-10 YOLO/LiteRT als separater Vision-Provider.
- H-06/H-07 VirtualDisplay Foundation ohne Remote-Display-Versprechen.
- I-03 ShapeMaker Embedded Designer und `.ema`-Assets.
- I-04 ChartGraph Kerncharts.
- I-06 bis I-10 Tasker, Shizuku, Termux, Custom Tabs, scrcpy/VT2VT.
- I-13 IME Keypad als separat versioniertes Companion-Produkt.

Optionale Funktionen werden in Release Notes als optionaler Adapter oder Companion ausgewiesen.
Fehlt ein Adapter, muss WSS kontrolliert diagnostizieren und weiter nutzbar bleiben.

## Ausdruecklich nicht im 1.0-Pflichtumfang

- A-12 bis A-15: Funktionen, strukturierte Fehler, Multi-Root, Makros und erweiterte Collections.
- E-16/E-17: Rich-Compose-Blockkomponenten, Patchbay und Synthesizer-Projektion.
- W-10/H-08/H-11/H-12: MultiDisplay, Virtual-App-Sessions, Remote Capability/Display Sessions.
- D-11-Ausbau und D-12: produktives RAG, lernendes World Model und ExperienceRecords.
- T-11/T-12: vollstaendiger Breakpoint-Debugger und Perugger.
- I-11: Join, Ktor und Sherpa.
- X-01 bis X-10: lokale KI/Agenten, autonome Skripte, Spezialprodukte und Benchmarks.

## Kritischer Pfad

1. **M5-7 DONE:** A11y-Zielvertrag und realer semantischer `clickText`-Pfad.
2. **M5-8:** Provider-Resolution, Berechtigungen und deterministische Fehler.
3. **M5-H:** Headless Host, Foreground Lifecycle, Recovery und UI-Reattachment.
4. **M6-W:** gemeinsamer WindowSession-/Display-Vertrag fuer Shell und Overlay.
5. **M7-1/M7-2:** gemeinsamer Capture-/Display-Kontext, A11y Scene/Evidence und Freshness.
6. **M7-6:** Record -> Review -> Proposal -> explizite Workflow-Publikation.
7. **M8-0/M8-1/M8-6/M8-9:** minimales Projekt-/Resource-Modell, Explorer und Portabilitaet.
8. **M10:** Feature Freeze, Security, Migration, Langlauf, Dokumentation und RC.
9. **Alpha -> Beta -> RC -> Stable:** jede Stufe setzt alle vorherigen Core-Gates voraus.

M6-Editor-Haertung, Dokumentation und automatisierte Regression duerfen parallel laufen, solange
sie keine neuen Runtime-, WindowSession- oder Capture-Vertraege vorwegnehmen. M7-V bleibt optional;
M9-D bleibt deferred.

## Release-Stufen

### Alpha

- M5 Runtime Closure inklusive Headless Host.
- M6-W und Kerneditoren fuer den dokumentierten EMScript-Umfang stabil.
- A11y-Zielaufloesung und erforderliches OCR fuer mindestens zwei reale App-Workflows.
- Recorder -> Review -> Proposal -> explizite Workflow-Freigabe -> Runtime -> Trace.
- Projekt Save/Load und Recovery ohne kritischen Datenverlust.

### Beta

- M7 Closure fuer den freigegebenen Perception-Umfang.
- Minimales M8 Projekt-/Resource-/Explorer-/Portabilitaetsgate.
- Ausgewaehlte optionale Integrationen mit kontrollierter Abwesenheit und Berechtigungsfehlern.
- Mehrgeraete-, Reconnect-, grosse Workflow- und mehrstuendige Langlauftests.

### Release Candidate

- Feature Freeze.
- Q-06 bis Q-10 geschlossen.
- Vollstaendige Regression, Backup/Restore, Migration und Datenschutz-/Log-Redaktionsaudit.
- Reproduzierbarer signierter Build und vollstaendige Known-Limitations-Liste.

### Stable 1.0

- RC-Gate ohne offene P0/P1-Probleme.
- Kanonische Datenintegritaet, Three-Editor-Roundtrip, Runtime-Cancellation, Recovery und
  Provider-Fehlersemantik auf der Zielgeraetematrix nachgewiesen.
- Changelog, EMScript-Untermenge, Berechtigungen, Backup und Migrationshinweise veroeffentlichbar.

## Aktuelle Release-Blocker

1. M5-8 einheitliche Provider-, Availability-, Permission- und Fallback-Policy fehlt.
2. M5-H Headless/Foreground Host, Recovery und UI-Reattachment fehlen.
3. M6-W gemeinsamer WindowSession-Vertrag und vollstaendige Panel-Geraeteabnahme fehlen.
4. Gemeinsamer CaptureFrame-/Display-Kontext fehlt.
5. Record -> Review -> Proposal -> Workflow ist nicht vollstaendig geschlossen.
6. Projekt-/Resource-Portabilitaet und Backup/Restore sind nicht abgenommen.
7. Security-, Privacy-, Langlauf-, Device-Matrix- und reproduzierbarer Release-Build fehlen.

M5-7 ist kein offener Blocker mehr. Allgemeines `clickElement`, variable Textqueries und
Nicht-Default-Displays bleiben sichtbare GAPs fuer M5-8/M7-2, ohne den nachgewiesenen
`clickText`-Vertikalschnitt rueckwirkend zu entwerten.

R-17 ist kein Blocker, solange Funktionen und strukturierte Fehler explizit ausserhalb des
1.0-Sprachumfangs bleiben.
