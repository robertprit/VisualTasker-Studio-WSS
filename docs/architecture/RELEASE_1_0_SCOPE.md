# VisualTasker Studio WSS 1.0 Release Scope

Stand: 2026-10-11

## Entscheidung

Version 1.0 liefert eine lokale, kanonische Visual-Automation-IDE mit drei konformen Editoren,
deterministischer Android-Runtime, Recorder/RailTrace, grundlegender semantischer Zielaufloesung,
stabiler Projektpersistenz und belastbarem Recovery. Optionale Provider und experimentelle
Produkte duerfen 1.0 nicht blockieren.

Dieses Dokument konkretisiert `RELEASE_1_0_PLAN.md`. Die Feature-IDs stammen aus
`../FEATURE_INVENTORY.md`.

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

1. **M5-7:** A11y-Zielvertrag und ein realer semantischer Klickpfad.
2. **M5-8:** Provider-Resolution, Berechtigungen und deterministische Fehler.
3. **M5-H:** Headless Host, Foreground Lifecycle, Recovery und UI-Reattachment.
4. **M5-10:** Runtime Closure, Retention und Langlaufmatrix. M5-9 wird nur bei separater Freigabe
   aufgenommen.
5. **M6-0/M6-W:** produktiver Editor-/Panel-Audit und gemeinsamer WindowSession-Vertrag.
6. **M6-1 bis M6-5/M6-7:** Block, Flow, Text, Selection, Inspector und Performance/Geraetegate.
7. **M7-0/M7-1/M7-2:** Capture, A11y Scene/Evidence und Freshness.
8. **M7-3/M7-6 bis M7-9:** OCR nach Bedarf sowie Record -> Review -> Proposal -> Workflow.
9. **M8-0/M8-1/M8-6/M8-9:** minimales Projekt-/Resource-Modell, Explorer und Portabilitaet.
10. **M10:** Feature Freeze, Security, Migration, Langlauf, Dokumentation und RC.

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

1. M5-7 semantische Accessibility-Zielaufloesung fehlt.
2. M5-8 einheitliche Provider- und Permission-Policy fehlt.
3. M5-H Headless/Foreground Host und Recovery fehlen.
4. M6-W gemeinsamer WindowSession-Vertrag und vollstaendige Panel-Geraeteabnahme fehlen.
5. Gemeinsamer CaptureFrame-/Display-Kontext fehlt.
6. Record -> Review -> Proposal -> Workflow ist nicht vollstaendig geschlossen.
7. Projekt-/Resource-Portabilitaet und Backup/Restore sind nicht abgenommen.
8. Security-, Privacy-, Langlauf-, Device-Matrix- und reproduzierbarer Release-Build fehlen.

R-17 ist kein Blocker, solange Funktionen und strukturierte Fehler explizit ausserhalb des
1.0-Sprachumfangs bleiben.
