# VisualTasker Studio WSS Roadmap

Stand: 2026-09-07

Diese Datei ist die laufende Arbeitsliste fuer Ziele, TODOs, Abnahmen und offene Entscheidungen bis zur stabilen Version 1. Sie beschreibt den Projektstand aus Sicht der Workspace Shell App. Details zu Architekturentscheidungen stehen in `docs/adr/`.

## Zielbild

VisualTasker Studio WSS fasst die alte VisualTasker Studio App und die Workspace Shell App in einem modularen Projekt zusammen. BlockEditor, FlowEditor und TextEditor sind unterschiedliche Projektionen desselben Workflow-Modells. Weitere Funktionen werden als WSS-native Panels oder Plugins umgesetzt, nicht durch Rueckfall in die alte monolithische MainScreen-Architektur.

## Leitregeln

- Workspace Shell ist der primaere Anzeigemodus.
- Workflow-Daten liegen in einem gemeinsamen Dokumentmodell mit stabiler IR-Zwischenschicht.
- BlockView, FlowView und TextView speichern eigene Layout-/Draft-Zustaende, aber nicht konkurrierende Workflow-Wahrheiten.
- Alte Studio-Funktionen werden modular uebernommen: Panel, Plugin, Contract oder Shared Service.
- Nach Codeaenderungen: Build, Tests, Install, Start, Smoke-Test, Commit und Push.
- RAG kommt erst nach stabiler Release-Basis.

## Aktueller Status

### Stabil / Nutzbar

- Workspace Shell mit verschiebbaren, minimierbaren und resizbaren Panels.
- BlockEditor als WSS-Plugin mit Siderail, Toolbox, Inspector, Minimap, Drag/Drop, Docking, Undo/Redo-Grundlage und Runtime-Fokus.
- FlowEditor als WSS-Plugin mit IR-Projektion, Runtime-Layer, Step-Fokus, Facets, Minimap, Trash, Siderail und View-Persistenz.
- TextEditor mit EMScript-Draft, Apply-Pfad, Runtime-Zeilenmarkierung und ohne Debug-Footer.
- Stepper Panel mit Timeline, Replay, Speed-Control, Step-Fokus und gespeicherter letzter Position.
- LogConsole und DebugInfo als Shell-Panels.
- Canvas, Marker, Vision und Datastore als vorbereitete Panels.
- Grundlegender EMScript Runtime-/DryRun-Pfad fuer vorhandene Grundbefehle.

### Noch Nicht Final

- FlowEditor ist funktional, aber noch nicht finaler Editorstatus.
- EMScript Parser/Generator/Katalog ist breit, aber noch nicht vollstaendig releasefest.
- Floating Overlays und LiveMarker sind als Basis vorhanden, aber noch nicht final integriert.
- Marker/Vision/Canvas/Datastore brauchen saubere Persistenz, klare Contracts und reale Pipeline-Anbindung.
- MainScreen ist noch nicht bereinigt oder als alte fixe Studio-Darstellung sauber ersetzt.
- Einstellungen sind noch nicht vollstaendig uebergreifend konsolidiert.

## Meilensteine Bis Stable V1

### M1: Editor-Core Finalisieren

- [ ] BlockEditor und FlowEditor Bedienlogik angleichen: Selection, Fokus, Haptik, Sound, Kontextmenues.
- [ ] Inspector-Inhalte in beiden Editoren entschlacken und Debugdetails in DebugInfo/LogConsole verschieben.
- [ ] Scrollleisten, Minimap-Position, Zoom-auf-Fokus und Centering konsistent machen.
- [ ] Block/Node-Selektion bidirektional stabil synchronisieren.
- [ ] Viewport-Freeze- und Fokuswechsel-Regressionen weiter testen.

### M2: FlowEditor Auf Editorstatus Bringen

- [ ] Node-Groessen vereinheitlichen.
- [ ] Reporter/Dataflow/Operator/Compare Nodes visuell und beim Arrange speziell behandeln.
- [ ] Ports fuer oben/unten Sequence und seitliche Branch/Dataflow-Verbindungen festlegen.
- [ ] Dock, Undock, Detach und magnetische Ports verlaesslich machen.
- [ ] Auto-Arrange mit kurzem Routing, wenig Kreuzungen und stabilem Hauptstamm haerten.
- [ ] Auto-Pan beim Draggen am Viewportrand wie im BlockEditor einbauen.
- [ ] Kanten-Hervorhebung fuer selektierte Nodes und einzeln selektierte Edges finalisieren.
- [ ] Facet-Handles weiter polieren, Collapse-Verhalten releasefest machen.

### M3: EMScript Stabilisieren

- [ ] Kanonische Syntax vollstaendig dokumentieren.
- [ ] Parser aus Demo-Subset herausziehen.
- [ ] Generator fuer alle vorhandenen Grundbefehle vervollstaendigen.
- [ ] Command-Catalog fuer alle geplanten Kategorien pflegen.
- [ ] Roundtrip-Tests WSS -> BlockEditor -> EMScript -> IR -> FlowEditor erweitern.
- [ ] Fehlerdiagnosen mit Source-Mapping, Node/Block-ID und Textzeile ausgeben.
- [ ] Runtime-Capability-Gates fuer noch fehlende Adapter klar anzeigen.

### M4: Floating Overlays Und LiveMarker

- [ ] Overlay-Berechtigungsflow finalisieren.
- [ ] Floating Toolbar fuer Aufnahme, Screenshot, Marker und Runtime-Aktionen fertigstellen.
- [ ] Floating Inspector fuer LiveMarker und ausgewählte UI-Elemente anbinden.
- [ ] LiveMarker Overlay fuer Region, Point, Swipe, Spline, Path und Multi-Modus umsetzen.
- [ ] Recording Start/Stop aus Overlay erreichbar machen.
- [ ] Overlay-Zustaende speichern und wiederherstellen.

### M5: Marker, Canvas Und Vision

- [ ] Marker Panel als reine Steuerkonsole finalisieren.
- [ ] Canvas als Screenshot-Hintergrund/Arbeitsflaeche konsolidieren.
- [ ] Screenshot-Karussell und gespeicherte Assets robust anbinden.
- [ ] Marker-Persistenz fuer Region, Template, Point, Swipe, Spline, Path, Multi und Draw-Elemente fertigstellen.
- [ ] Vision Panel mit Live-Crop und Vergleichsbild finalisieren.
- [ ] Filter, Graustufen, Falschfarben, Maskierung und Thresholds ins Vision Panel verlegen.
- [ ] Template/Marker-Kommandos mit Datastore und Vision verbinden.

### M6: Datastore Und Worldview

- [ ] Datastore Panel als zentrale Datenansicht fuer Ressourcen ausbauen.
- [ ] Screenshots, Marker, Templates, Runtime-Daten und Sessions gemeinsam anzeigen.
- [ ] Import-Mapping aus altem Studio fuer Marker/Templates/Screenshots erweitern.
- [ ] Worldview Contracts fuer UI-Elemente, Ressourcen, Overlays und Vision-Ergebnisse haerten.
- [ ] Export/Import und Migrationen fuer Workspace-Ressourcen einbauen.

### M7: Recording Und Stepper

- [ ] Recording Sessions speichern und laden.
- [ ] Stepper Session-Liste und aktive Session-Auswahl einfuehren.
- [ ] Timeline fuer passive Activity-/Scene-Dauer und aktive Events weiter haerten.
- [ ] Replay Schritt vor/zurueck und Speed-Control mit echten Recording-Daten testen.
- [ ] Stepper-Fokus mit TextEditor, BlockEditor und FlowEditor dauerhaft synchron halten.

### M8: Alte Studio-Funktionen Modular Uebernehmen

- [ ] Screenshot Panel Funktionen in Canvas/Vision/Marker sauber aufteilen.
- [ ] Inspector-Funktionen in Inspector/Debug/Datastore trennen.
- [ ] Template-Hilfsfunktionen als Codegenerator-/Vision-Komponenten uebernehmen.
- [ ] Browser/CustomChromeTab Panel vorbereiten.
- [ ] Keypad/Floating Keyboard final integrieren.
- [ ] Resource/Data Manager in Datastore/Worldview ueberfuehren.
- [ ] LogConsole/DebugInfo vollstaendig auf alten Funktionsumfang bringen.

### M9: Adapter Und Plugin-Vorbereitung

- [ ] Plugin-Vertrag fuer Session, Save, Dirty, Validation, Toolbar und Runtime finalisieren.
- [ ] Adapter-Gates fuer A11Y, Vision, OCR, OCV, YOLO, Tasker, Termux, scrcpy/Shizuku definieren.
- [ ] CustomChromeTab, Tasker, Charts, Shizuku, Termux und scrcpy als Plugin-Kandidaten vorbereiten.
- [ ] Vision-Scan-Pipeline fuer OCR, OCV, YOLO und A11Y entwerfen.
- [ ] YOLO/Wisely Cloud-Training nur vorbereiten, nicht vor Stable-V1 erzwingen.

### M10: Release-Haertung

- [ ] Smoke-Test-Checkliste fuer Geraet, DryRun, LiveRun, Overlay und Editor-Sync erstellen.
- [ ] Persistenz-/Migrationstests fuer Dokumente, Views und Ressourcen ergaenzen.
- [ ] Crash-Safety und inkompatible Plugin-/Command-Versionen pruefen.
- [ ] Settings vollstaendig konsolidieren.
- [ ] MainScreen bereinigen oder durch alte fixe Studio-Paneldarstellung ersetzen.
- [ ] Release Notes und Known Limitations erstellen.
- [ ] Stable V1 Tag vorbereiten.

## Offene Entscheidungen

- [ ] Namen finalisieren: WorkspaceDocument, BlockViewDocument, FlowViewDocument, FlowchartProjection, IRGraph.
- [ ] Soll FlowEditor Nodes mit Slots fuer Reporter/Operatoren bekommen oder nur spezielle kompakte Dataflow-Darstellung?
- [ ] Welche EMScript-Kommandos gehoeren vor Stable V1 zwingend in den Runtime-Pfad?
- [ ] Welche alten Studio-Funktionen werden Panels, welche Plugins, welche Shared Services?
- [ ] Wie wird RAG spaeter an Datastore/Worldview angebunden?

## Naechste 10 Arbeitsschritte

1. FlowEditor Node-Groessen und Port-Regeln vereinheitlichen.
2. FlowEditor Dock/Undock/Detach und magnetische Ports haerten.
3. FlowEditor Auto-Pan am Viewportrand einbauen.
4. FlowEditor Auto-Arrange/Autorouting weiter stabilisieren.
5. Inspector-Inhalte in BlockEditor und FlowEditor entschlacken.
6. Stepper Recording Sessions speichern/laden.
7. Floating Overlay Toolbar fuer Aufnahme und Screenshot anbinden.
8. LiveMarker Overlay Grundfunktionen integrieren.
9. Marker-Persistenz mit Datastore verbinden.
10. EMScript Roundtrip-Tests fuer aktuelle Grundbefehle erweitern.

## Abnahmeprotokoll

### 2026-09-07

- [x] Stepper Save Button eingebaut.
- [x] Letzter Stepper-Stand wird gespeichert und wiederhergestellt.
- [x] BlockEditor- und FlowEditor-Inspector breiter und naeher am linken Rand dargestellt.
- [x] Build, Unit-Tests, Install und Launch waren erfolgreich.
- [x] Panel-Akzentfarben typisiert und Legacy-Defaults beim Laden normalisiert.
- [x] Panel-Resize-Griff vergroessert und sichtbarer gemacht.
- [x] Harte Workspace-Grenzen rechts und unten mit Dock-Reserve umgesetzt.
- [x] Magnetisches Andocken an Workspace- und Nachbarpanel-Kanten vorbereitet.
