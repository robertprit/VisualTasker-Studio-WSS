# VisualTasker Studio WSS Roadmap

Stand: 2026-09-15

Diese Datei ist die laufende Arbeitsliste fuer Ziele, TODOs, Abnahmen und offene Entscheidungen bis zur stabilen Version 1. Sie beschreibt den Projektstand aus Sicht der Workspace Shell App. Details zu Architekturentscheidungen stehen in `docs/adr/`.

## Zielbild

VisualTasker Studio WSS fasst die alte VisualTasker Studio App und die Workspace Shell App in einem modularen Projekt zusammen. BlockEditor, FlowEditor, TextEditor und RailTrace sind unterschiedliche Projektionen desselben Workflow-Modells. Weitere Funktionen werden als WSS-native Panels oder Plugins umgesetzt, nicht durch Rueckfall in die alte monolithische MainScreen-Architektur.

## Leitregeln

- Workspace Shell ist der primaere Anzeigemodus.
- Workflow-Daten liegen in einem gemeinsamen Dokumentmodell mit stabiler IR-Zwischenschicht.
- BlockView, FlowView, TextView und RailView speichern eigene Layout-/Draft-/Runtime-Zustaende, aber nicht konkurrierende Workflow-Wahrheiten.
- Alte Studio-Funktionen werden modular uebernommen: Panel, Plugin, Contract oder Shared Service.
- Nach Codeaenderungen: Build, Tests, Install, Start und Smoke-Test. Commit/Push nur auf ausdruecklichen Auftrag.
- Nach Installation und Smoke-Test werden automatische ADB-Klick-/Screenshot-Pruefungen nur nach ausdruecklicher Freigabe ausgefuehrt.
- Aenderungen an gemeinsam sichtbaren Editor-Panel-Regeln werden fuer BlockEditor und FlowEditor parallel geprueft und, soweit sinnvoll, parallel umgesetzt.
- RAG kommt erst nach stabiler Release-Basis.
- AI/ML/Vision erzeugen spaeter strukturierte Results und Proposals, aber keine direkten Mutationen; siehe `docs/adr/0003-ai-proposals-and-structured-results.md`.
- Der FlowEditor soll kein zweiter zweidimensionaler BlockEditor werden. Er bleibt eine semantische Workflow-/Analyse-Projektion mit optionalen Detail-Layern.

## Aktueller Status

### Stabil / Nutzbar

- Workspace Shell mit verschiebbaren, minimierbaren und resizbaren Panels.
- BlockEditor als WSS-Plugin mit Siderail, Toolbox, Inspector, Minimap, Drag/Drop, Docking, Undo/Redo-Grundlage und Runtime-Fokus.
- FlowEditor als WSS-Plugin mit IR-Projektion, Runtime-Layer, Step-Fokus, Facets, Minimap, Trash, Siderail und View-Persistenz.
- FlowEditor startet in semantischer Sicht; Reporter, Variablen und Operatoren sind als zuschaltbare Detail-Layer vorgesehen.
- Flowchart-Viewport-Recovery verwirft stale Layouts robuster, ignoriert Hintergrund-Facets beim Fit und verhindert initiale Runtime-Fokusspruenge aus alten DryRun-/RailTrace-Zustaenden.
- TextEditor mit EMScript-Draft, Apply-Pfad, Runtime-Zeilenmarkierung und ohne Debug-Footer.
- RailTrace Panel mit Timeline, Replay, Speed-Control, Step-Fokus, Modus-/Scale-Vertrag und gespeicherter letzter Position.
- LogConsole und DebugInfo als Shell-Panels.
- Canvas, Marker, Vision und Datastore als vorbereitete Panels.
- VisualAssets Panel als ShapeMaker-/Asset-Manager-Hub vorbereitet.
- VT2VT Panel als Remote-Sync-/Observer-Hub mit Loopback-Contract vorbereitet.
- Grundlegender EMScript Runtime-/DryRun-Pfad fuer vorhandene Grundbefehle.

### Noch Nicht Final

- Die aktuellen Audit-Matrizen fuer EMScript/Workflow-Roundtrip,
  Capability-Drift und Worldview-Drift liegen in `docs/audit/` und dienen als
  Reparaturkarte fuer die naechsten Architektur- und Editor-Schnitte.
- FlowEditor ist funktional, aber noch nicht finaler Editorstatus: AutoArrange, semantische Lesbarkeit, Branch-Naehe, Facet-Bounds und Detail-Layer brauchen weitere Haertung.
- BlockEditor/FlowEditor Panel-Paritaet ist teilweise umgesetzt; Inspector-Sheets sollen unten direkt am Panelrand andocken und gleiche Grundregeln fuer Fokus, Zoom, Selection und Eingabe verwenden.
- EMScript Parser/Generator/Katalog ist breit, aber noch nicht vollstaendig releasefest.
- Floating Overlays und LiveMarker sind als Basis vorhanden, aber noch nicht final integriert.
- Marker/Vision/Canvas/Datastore brauchen saubere Persistenz, klare Contracts und reale Pipeline-Anbindung.
- MainScreen ist noch nicht bereinigt oder als alte fixe Studio-Darstellung sauber ersetzt.
- Einstellungen sind noch nicht vollstaendig uebergreifend konsolidiert.

## Meilensteine Bis Stable V1

### M1: Editor-Core Finalisieren

- [ ] BlockEditor und FlowEditor Bedienlogik angleichen: Selection, Fokus, Haptik, Sound, Kontextmenues.
- [ ] Inspector-Inhalte in beiden Editoren entschlacken und Debugdetails in DebugInfo/LogConsole verschieben.
- [ ] Inspector-Sheets in BlockEditor und FlowEditor ohne unteren Abstand direkt an den Panelrand andocken und Eingabe-/Drag-Hit-Zonen absichern.
- [ ] Scrollleisten, Minimap-Position, Zoom-auf-Fokus und Centering konsistent machen.
- [ ] Block/Node-Selektion bidirektional stabil synchronisieren.
- [ ] Viewport-Freeze- und Fokuswechsel-Regressionen weiter testen.
- [ ] Panel-Layout-Regeln fuer BlockEditor und FlowEditor parallel pflegen: Iconbar, SideRail, Inspector, Trash, Minimap, Resize- und Hit-Zonen.

### M2: FlowEditor Auf Editorstatus Bringen

- [x] Node-Groessen auf ein quadratisches 96-x-96-Grundraster vereinheitlichen und Legacy-Views zentriert migrieren.
- [x] Reporter/Dataflow/Operator/Compare Nodes visuell und beim Arrange speziell behandeln.
- [x] Ports fuer oben/unten Sequence und seitliche Branch/Dataflow-Verbindungen festlegen.
- [x] Dock, Undock, Detach und magnetische Ports verlaesslich machen.
- [x] Auto-Arrange mit kurzem Routing, wenig Kreuzungen und stabilem Hauptstamm haerten.
- [x] Auto-Pan beim Draggen am Viewportrand wie im BlockEditor einbauen.
- [x] Kanten- und Node-Hervorhebung fuer manuelle Auswahl und Runtime-Fokus finalisieren.
- [x] Facet-Handles mit sichtbarer Bezeichnung, Collapse-Aktion und Kontextmenue ausstatten.
- [x] Facet-Collapse-Verhalten persistent, Undo-faehig und fuer verschachtelte Graphen absichern.
- [x] Eigene Start-/Terminator-Semantik und -Darstellung fuer Workflow, Recording und DryRun festlegen.
- [x] Recording-Graphprojektion auf den gemeinsamen Start-/Terminator-Vertrag anbinden.

M2-Zwischenstand 2026-09-15: Alle Standard-Nodearten besitzen eine explizite quadratische
M3-inspirierte Silhouette. Workflow, Recording und DryRun verwenden getrennte
Start-/End-Geometrien. Records koennen aus RailTrace als read-only FlowGraphDocument
projiziert werden; die Flowchart-Standardansicht bleibt aber Workflow-basiert. Layout-,
Routing-, Interaktions-, Serialisierungs- und Compose-Tests sind gruen. Der Editorstatus
ist technisch nah, visuell aber noch nicht final abgenommen.

### M2.1: FlowEditor Lesbarkeit Und AutoArrange Finalisieren

- [x] Flowchart-Startansicht von alten RailTrace-/DryRun-Fokuszustaenden entkoppeln.
- [x] Stale FlowViewDocument erkennen, wenn nur isolierte stabile Nodes matchen.
- [x] Hintergrund-Facets aus Viewport-Fit-Bounds ausschliessen.
- [x] Semantische Startsicht fuer Flowchart einfuehren: Reporter, Variablen und Operatoren initial ausgeblendet.
- [x] Variable-Bulk-Facets nicht mehr links vor den Hauptstamm legen.
- [ ] Hauptstamm im semantischen Modus kompakter und gleichmaessiger ausrichten.
- [ ] Branch-Ziele naeher an Decision-Nodes ziehen und Treppenstruktur verbessern.
- [ ] Technische Detail-Nodes als Layer behandeln, ohne AutoArrange-Hauptfluss zu zerlegen.
- [ ] Facet-Bounds und Collapsed-Facets duerfen AutoFit und Scroll-Startposition nicht dominieren.
- [ ] AutoArrange-Modi klar trennen: Semantik, Analyse, Kompakt, Manuell.
- [ ] Optionales Slot-/Dock-Modell fuer Reporter-/Operator-/Dataflow-Nodes entwerfen, ohne FlowEditor zum 2D-BlockEditor umzubauen.
- [ ] Port- und Routing-Regeln fuer beidseitige Reporter/Dataflow-Ports weiter haerten.

### M3: EMScript Stabilisieren

- [ ] Kanonische Syntax vollstaendig dokumentieren.
- [ ] Parser aus Demo-Subset herausziehen.
- [ ] Generator fuer alle vorhandenen Grundbefehle vervollstaendigen.
- [x] Ersten `CommandCapabilityDescriptor` als gemeinsame Ableitung aus dem CommandCatalog fuer RuntimeGate, Adapterbedarf und Live-Implementierungsstatus einfuehren.
- [x] Runtime-/DryRun-/FlowRuntime-Events transportieren stabile `diagnosticCode`-Werte bis in Log-/Debug-Projektionen.
- [ ] Command-Catalog fuer alle geplanten Kategorien weiter pflegen und Descriptor in Settings, Toolboxen, Diagnostik und Adapter-Registry durchziehen.
- [ ] Roundtrip-Tests WSS -> BlockEditor -> EMScript -> IR -> FlowEditor erweitern.
- [ ] Fehlerdiagnosen mit Source-Mapping, Node/Block-ID und Textzeile ausgeben.
- [ ] Runtime-Capability-Gates fuer noch fehlende Adapter klar anzeigen.

### M4: Floating Overlays Und LiveMarker

- [ ] Overlay-Berechtigungsflow finalisieren.
- [ ] Floating Toolbar fuer Aufnahme, Screenshot, Marker und Runtime-Aktionen fertigstellen.
- [ ] Floating Inspector fuer LiveMarker und ausgewählte UI-Elemente anbinden.
- [ ] LiveMarker Overlay fuer Region, Point, Swipe, Spline, Path und Multi-Modus umsetzen.
- [x] Recording Start/Stop aus Overlay erreichbar machen.
- [ ] Overlay-Zustaende speichern und wiederherstellen.

### M5: Marker, Canvas Und Vision

- [ ] Marker Panel als reine Steuerkonsole finalisieren.
- [ ] Canvas als Screenshot-Hintergrund/Arbeitsflaeche konsolidieren.
- [ ] Screenshot-Karussell und gespeicherte Assets robust anbinden.
- [ ] Marker-Persistenz fuer Region, Template, Point, Swipe, Spline, Path, Multi und Draw-Elemente fertigstellen.
- [ ] Vision Panel mit Live-Crop und Vergleichsbild finalisieren.
- [ ] Filter, Graustufen, Falschfarben, Maskierung und Thresholds ins Vision Panel verlegen.
- [ ] Template/Marker-Kommandos mit Datastore und Vision verbinden.

### M5.1: Workspace Canvas Als Gemeinsame Visuelle Arbeitsflaeche

- [ ] Workspace Canvas zeigt nur bei aktiven Canvas-/Marker-/Vision-nahen Panels die Screenshot-Arbeitsflaeche, sonst das neutrale Grid.
- [ ] Canvas Panel bleibt Detailprojektion: fokussierter Crop, Step-Ausschnitt, Marker-Detail oder Screenshot-Ausschnitt.
- [ ] Vision Panel bleibt exklusiver Machine-Vision-Arbeitsbereich fuer Crop, Filter, Template-Vergleich, OCR/OCV/YOLO/A11Y.
- [ ] Marker Panel bleibt Steuerkonsole fuer Marker, nicht Canvas-Duplikat.
- [ ] Marker-Speichern erzeugt immer Ressource plus passenden Block und Node in der Scene-/Marker-Kategorie.
- [ ] Marker-Modi Region, Template, Point, Swipe, Spline, Path, Multi und Draw-Familie konsistent persistieren.
- [ ] RailTrace-Record-Steps projizieren Screenshots, Klicks, Swipes, Pfade und erkannte Entities auf Workspace Canvas und Canvas Panel.

### M6: Datastore Und Worldview

- [ ] Datastore Panel als zentrale Datenansicht fuer Ressourcen ausbauen.
- [ ] Screenshots, Marker, Templates, Runtime-Daten und Sessions gemeinsam anzeigen.
- [x] Visual UI Memory Projektion fuer Worldview, Provider, Facetten und Suggestions anlegen.
- [x] Canvas-/Marker-Ressourcen im Datastore als Worldview-Slice projizieren.
- [ ] VisualAsset Manager fuer ShapeMaker-Assets, Block-Shapes, Node-Shapes, Port-Shapes, Icons und Compose-Overlays ausbauen.
- [ ] `.ema` / `application/vnd.emscript.motion+json` als Austauschformat fuer Visual Assets anbinden.
- [ ] Toolbox-Sets fuer Standard, Custom, Mixed, JavaScript und Plugin-Familien speichern und im Workspace auswahlen.
- [ ] Import-Mapping aus altem Studio fuer Marker/Templates/Screenshots erweitern.
- [ ] Worldview Contracts fuer UI-Elemente, Ressourcen, Overlays und Vision-Ergebnisse haerten.
- [ ] Export/Import und Migrationen fuer Workspace-Ressourcen einbauen.

### M6.1: Visual Abstraction Layer Und VisualAsset Manager

- [ ] VAL als gemeinsame Sprache fuer Block-, Node-, Port-, Facet-, Marker- und Overlay-Darstellung festziehen.
- [ ] M3ShapeMaker als VisualAsset Manager fuer Blockformen, Nodeformen, Portformen, Icons, Handles und Facet-Designs anbinden.
- [ ] BlockDesigner zeigt parallel betroffene FlowchartNode-Vorschau an.
- [ ] Asset-Katalog fuer Standard-, Custom- und Plugin-Assets speichern, bearbeiten, loeschen und versionieren.
- [ ] Austauschbare Toolbox-Sets fuer Standard, Custom, Mixed, JavaScript und Plugin-Familien einrichten.
- [ ] `.ema`-Dateien fuer Shapes, Motion, Draw-Pfade und wiederverwendbare visuelle Assets spezifizieren.
- [ ] Compose-Overlay-Controls nur als bewusstes Rich-Overlay-Konzept behandeln; gezeichnete Block-/Node-Kerne bleiben performant und serialisierbar.
- [ ] DnD-Listen als generische Item-Transport-Schicht fuer Marker, RailTrace, Inspector, Datastore, TextEditor, BlockEditor und FlowEditor ausbauen.

### M7: Recording Und RailTrace

- [x] Recording Sessions speichern und laden.
- [x] RailTrace Session-Liste und aktive Session-Auswahl einfuehren.
- [x] Timeline fuer passive Activity-/Scene-Dauer und aktive Events weiter haerten.
- [x] RailTrace-Modi `Program`, `Step`, `Live`, `Replay` und `Curate` als Vertrag modellieren.
- [x] RailTrace Surface-Modi `Records`, `Run` und `WatchDog` ueber die internen Detailmodi legen.
- [x] Gesamtzeit-Progressbar fuer RailTrace als Timespan-Indikator einbauen.
- [x] RailTrace-Step-Auswahl mit Marker Panel synchronisieren.
- [x] DryRun-/BasicRun-Schritte synchronisieren RailTrace-Auswahl, Position und Index.
- [x] DryRun/LiveRun/Step-Steuerung als globale Runtime-Bedienung in RailTrace konzentrieren.
- [ ] Replay Schritt vor/zurueck und Speed-Control mit echten Recording-Daten testen.
- [ ] RailTrace-Fokus mit TextEditor, BlockEditor und FlowEditor dauerhaft synchron halten.
- [ ] Canvas Panel als fokussierten Scene-Inspector fuer Record-Steps, Screenshots, Activities, Klicks, Gesten und erkannte Entities ausbauen.
- [ ] Marker Panel als jederzeit editierbare Anpassungszentrale fuer RailTrace/Canvas/Datastore etablieren.
- [ ] Junktor vorbereiten: Record/Scan/Trace/User Intent/Datastore zu pruefbaren Block-, Flow- und EMScript-Vorschlaegen konkretisieren.

### M7.1: RailTrace Als Zentrale Runtime-Sicht

- [ ] RailTrace zeigt strikt getrennte Modi: Last Records/Live Recording, Dry/Wet Run und WatchDog.
- [ ] DryRun/WetRun globale Steuerung aus RailTrace heraus finalisieren und redundante Run-Buttons aus anderen Panels entfernen.
- [ ] Step-Liste in SideRail fuehren; Panel-Inhalt zeigt Timeline, Progress, Lanes und Step-Inspector.
- [ ] Aktiver Runtime-Step zentriert TextEditor-Zeile, BlockEditor-Block, FlowEditor-Node/Kante und Step-Liste.
- [ ] Activity-/Scene-Band als passive Dauer oberhalb aktiver Event-Lanes darstellen.
- [ ] Fehler, Invalids und Resultate direkt auf Timeline projizieren.
- [ ] Variables-/Observations-Lane ergaenzen.
- [ ] Recording-Replay-Hakeln analysieren und Render-/Tick-Strategie optimieren.

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
- [x] VT2VT Remote-Sync-Contract, Rollenmodell und Loopback-Panel vorbereiten.
- [x] VT2VT LAN-TCP Transport mit Pairing-Haken und Read-only Observer-Grundlage implementieren.
- [ ] VT2VT WebSocket/Discovery-Schicht fuer komfortables Pairing zwischen zwei Geraeten ergaenzen.
- [ ] VT2VT RuntimeTrace/Log/RailTrace Live-Mirror zwischen zwei Geraeten testen.
- [ ] Vision-Scan-Pipeline fuer OCR, OCV, YOLO und A11Y entwerfen.
- [ ] YOLO/Wisely Cloud-Training nur vorbereiten, nicht vor Stable-V1 erzwingen.

### M9.1: Plugin-Adapter Und Beispielprofile

- [ ] Tasker-Settings-Tab, Beispielprofil, Testevent und Action mit Rueckmeldung final pruefbar machen.
- [ ] Tasker Result-/Error-Namen kurz und konfliktfrei halten, z.B. `Tasker.lastResult` und `Tasker.error`.
- [ ] Shizuku UserService statt privater/reflection APIs fuer echte Shell-Ausfuehrung finalisieren.
- [ ] Termux RUN_COMMAND mit dokumentierter `allow-external-apps=true`-Pruefung finalisieren.
- [ ] CustomChromeTab Settings fuer Farben, Close/Share/Download/Favoriten/Menu/ActionIcon und BottomBar vervollstaendigen.
- [ ] scrcpy/VT2VT USB-/ADB-nahe Verbindung als schneller Transportpfad vorbereiten.

### M10: Release-Haertung

- [ ] Smoke-Test-Checkliste fuer Geraet, DryRun, LiveRun, Overlay und Editor-Sync erstellen.
- [ ] Persistenz-/Migrationstests fuer Dokumente, Views und Ressourcen ergaenzen.
- [ ] Crash-Safety und inkompatible Plugin-/Command-Versionen pruefen.
- [ ] Settings vollstaendig konsolidieren.
- [ ] MainScreen bereinigen oder durch alte fixe Studio-Paneldarstellung ersetzen.
- [ ] Release Notes und Known Limitations erstellen.
- [ ] Stable V1 Tag vorbereiten.

## C-Meilensteine: Cross-Cut Architektur

### C1: Human Canvas, Vision Pipeline Und RailTrace-Projektionen Trennen

- [x] Workspace Canvas als menschliche Originaldarstellung und Arbeitsflaeche von der Vision-Pipeline trennen.
- [x] Vision Panel als exklusiven Bereich fuer Crop, Template-Vergleich, Filter, Processing und Machine-Vision-Auswertung behandeln.
- [x] Canvas Panel als Detailprojektion der menschlichen Arbeitsflaeche markieren, nicht als Vision-Arbeitsbereich.
- [x] Vision-Siderail von Marker-/Canvas-Modusbuttons entkoppeln.
- [x] Canvas Panel als echte Detailprojektion eines ausgewaehlten Workspace-/RailTrace-Elements schaerfen.
- [x] RailTrace-Step auf Workspace Canvas und Canvas Detail synchron fokussieren.
- [x] Marker-Auswahl auf Workspace Canvas und Marker Panel bidirektional synchronisieren.
- [x] Vision Panel mit eigenem Screenshot-/Referenz-Auswahlmodell absichern.
- [x] Recorder-Step-Screenshots sauber mit RailTrace und Canvas Detail verbinden.
- [x] Workflow/Record/WatchDog-Rail-Modi im RailTrace weiter trennen.
- [ ] FlowEditor Layout-/Port-/Routing-Finalisierung fortsetzen.
- [ ] Block/Flow/Text-Synchronisation fuer aktive Auswahl und Undo-Zustand weiter haerten.
- [x] Datastore als zentrale Worldview-Ansicht strukturieren: Human, Machine Vision, Runtime, Resources, Junktor.
- [x] AI-Verarbeitungsschicht als Proposal-/Structured-Result-Grenze per ADR festhalten.

### C2: Editor-Paritaet Und Gemeinsame Bedienlogik Finalisieren

- [ ] BlockEditor, FlowEditor und TextEditor nutzen dieselben Selection-, Focus- und Highlight-Contracts.
- [ ] Undo/Redo fuer Block, Flow und Text ueber gemeinsame Workflow-Commands absichern.
- [ ] Zoom, Pan, Center-on-Focus und Minimap-Verhalten in BlockEditor und FlowEditor vereinheitlichen.
- [ ] Drag/Drop-Feedback fuer Dock, Undock, Drop, Delete und Reject visuell, haptisch und akustisch angleichen.
- [ ] Kontextmenues fuer Block, Node, Edge, Textzeile und RailTrace-Step konsistent modellieren.
- [ ] Inspector-Sheets in BlockEditor und FlowEditor auf editierbare Kerndaten reduzieren.
- [ ] Debug-/Info-Daten aus Inspectoren in DebugInfo und LogConsole verschieben.
- [ ] BlockDesigner und Node-/Shape-Designer ueber VisualAsset Contracts verbinden.
- [ ] Reporter-, Operator- und Dataflow-Darstellung in BlockEditor und FlowEditor semantisch angleichen.
- [ ] Regressionstests fuer Fokuswechsel, Panelwechsel, Drag, Zoom und Auswahl-Sync ergaenzen.

### C3: Runtime, EMScript Und Command-Katalog Releasefest Machen

- [ ] Kanonische EMScript-Grammatik fuer alle Stable-V1-Kommandos festschreiben.
- [ ] Parser, Generator und Formatter fuer Grundbefehle, Control, Variablen und Reporter vervollstaendigen.
- [ ] Runtime-Ausfuehrung fuer vorhandene Grundbefehle mit DryRun, BasicRun und LiveRun absichern.
- [ ] Capability-Gates fuer A11Y, Vision, Tasker, Termux, Shizuku, scrcpy und CustomTabs einheitlich melden.
- [ ] Command-Katalog mit Kategorien, Argumenttypen, Defaults, Hilfetexten und Block-/Node-Mapping erweitern.
- [ ] Testscript-Suite in Basic, Vision, Runtime, Plugin und Stress-Scripte aufteilen.
- [ ] Roundtrip-Tests Text -> IR -> Block -> Flow -> EMScript -> Runtime erweitern.
- [ ] Source-Mapping fuer Fehler von Runtime-Event zu Textzeile, Block-ID, Node-ID und RailTrace-Step herstellen.
- [ ] Snackbar-/LogConsole-Fehler mit Direktlinks zu betroffenen Panels und Einstellungen ausstatten.
- [ ] Release-Check fuer noch nicht implementierte Kommandos und inkompatible Plugin-Adapter einfuehren.

### C4: Recorder, RailTrace Und Floating Overlays Fertigstellen

- [ ] Floating Toolbar mit Screenshot, Scan-Varianten, Record/WatchDog, Workspace, Replay und Marker-Modus finalisieren.
- [ ] Floating Panels stabil beweglich, minimierbar und vor Screenshots automatisch ausblendbar machen.
- [ ] Recorder-Events fuer Activity-Wechsel, Klicks, Swipes, Text, Gesten, Screenshots und Fehler persistieren.
- [ ] RailTrace-Modi Records, Run und WatchDog visuell und datenlogisch strikt trennen.
- [ ] RailTrace-Timeline mit Lanes, Activity-Band, Laufzeitmarker, Progressbar und Step-Inspector finalisieren.
- [ ] Replay Schritt vor/zurueck, Geschwindigkeit, Pause und Resume mit echten Recording Sessions testen.
- [ ] Recorder-Steps als Marker-/Block-/Node-Vorschlaege ueber Junktor bereitstellen.
- [ ] Workspace Canvas zeigt Recorder-Events als menschliche Step-Projektion.
- [ ] Canvas Panel zeigt fokussierte Recorder-Step-Details, Screenshots, Klicks, Swipes und Pfade.
- [ ] WatchDog-Events in RailTrace und LogConsole ohne Vermischung mit Workflow-Runs darstellen.

### C5: Visual UI Memory, Datastore Und Asset-System Stabilisieren

- [ ] Datastore in Bereiche fuer Human, Machine Vision, Runtime, Resources, Plugins und Junktor aufteilen.
- [ ] Marker, Screenshots, Templates, Recorder-Steps und Vision-Ergebnisse als versionierte Ressourcen persistieren.
- [ ] Worldview-Entities fuer A11Y, OCR, OCV, YOLO, DOM, Marker und Runtime vereinheitlichen.
- [ ] Visual UI Memory als gemeinsame Sicht fuer Inspector, Datastore, Marker, Vision und Junktor haerten.
- [ ] M3ShapeMaker als VisualAsset Manager fuer Block-, Node-, Port-, Icon- und Overlay-Shapes anbinden.
- [ ] `.ema`-Export/Import fuer Shapes, Pfade, Motion und Draw-Kommandos spezifizieren.
- [ ] Toolbox-Sets fuer Standard, Custom, Mixed, JavaScript und Plugin-Familien speichern und umschalten.
- [ ] Asset-Abhaengigkeiten in Workspace-Projekten speichern, migrieren und bei fehlenden Ressourcen melden.
- [ ] Import-Mapping aus altem Studio fuer Marker, Templates, Screenshots und Resources fertigstellen.
- [ ] RAG-Vorbereitung als read-only Index ueber stabile Datastore-/Worldview-Ressourcen planen.

### C6: Plugin-Integration, Settings Und Stable-V1-Abnahme

- [ ] Plugin-Einstellungen fuer Tasker, Termux, Shizuku, scrcpy/VT2VT, CustomTabs und Vision zentral zusammenfassen.
- [ ] Tasker Profile, Testevent, Action, Result und Error-Pfade als Beispielpaket pruefbar machen.
- [ ] Termux RUN_COMMAND, Shizuku UserService und CustomChromeTab Runtime Adapter releasefest anbinden.
- [ ] VT2VT Verbindung fuer Observer, Workspace-Mirror und RuntimeTrace-Mirror stabilisieren.
- [ ] Settings fuer Layout, Farben, UI-Skalierung, Schriftgrad, Minimap, Rail-Zustaende und Plugin-Gates konsolidieren.
- [ ] MainScreen entweder bereinigen oder als alte fixe Studio-Paneldarstellung sauber ersetzen.
- [ ] Smoke-Test-Matrix fuer Editor-Sync, Runtime, Overlay, Recorder, Vision, Datastore und Plugins erstellen.
- [ ] Persistenz-, Migrations- und Crash-Recovery-Tests fuer Workspace-Projekte und Ressourcen ergaenzen.
- [ ] Release Notes, Known Limitations, Backup-Hinweise und Debug-Anleitung schreiben.
- [ ] Stable V1 Build, Tag, Abnahmecheck und finalen Repo-Stand vorbereiten.

## Offene Entscheidungen

- [ ] Namen finalisieren: WorkspaceDocument, BlockViewDocument, FlowViewDocument, FlowchartProjection, IRGraph.
- [ ] Soll FlowEditor Nodes mit Slots fuer Reporter/Operatoren bekommen oder nur spezielle kompakte Dataflow-Darstellung?
- [ ] Welche VisualAsset-Typen sind fuer Block, Node, Port, Icon, Overlay und Component verbindlich?
- [ ] Wie werden Compose-Overlays an selektierte Canvas-Elemente fixiert, ohne eine zweite Dokumentwahrheit zu erzeugen?
- [ ] Welche Toolbox-Sets gehoeren in Stable V1 und welche bleiben projekt-/plugin-spezifisch?
- [ ] Wird VT2VT zuerst nur LAN/WebSocket oder zusaetzlich USB/ADB-Bridge fuer scrcpy-nahe Szenarien?
- [ ] Welche EMScript-Kommandos gehoeren vor Stable V1 zwingend in den Runtime-Pfad?
- [ ] Welche alten Studio-Funktionen werden Panels, welche Plugins, welche Shared Services?
- [ ] Wie wird RAG spaeter an Datastore/Worldview angebunden?

## Naechste 10 Arbeitsschritte

1. FlowEditor Node-Groessen und Port-Regeln vereinheitlichen.
2. FlowEditor Dock/Undock/Detach und magnetische Ports haerten.
3. FlowEditor Auto-Pan am Viewportrand einbauen.
4. FlowEditor Auto-Arrange/Autorouting weiter stabilisieren.
5. Inspector-Inhalte in BlockEditor und FlowEditor entschlacken.
6. RailTrace Recording Sessions speichern/laden.
7. Floating Overlay Toolbar fuer Aufnahme und Screenshot anbinden.
8. LiveMarker Overlay Grundfunktionen integrieren.
9. Marker-Persistenz mit Datastore verbinden.
10. VisualAssets/ShapeMaker-Hub oeffnen, `.ema`-Descriptor pruefen und Toolbox-Set-Konzept im Workspace verankern.

## Abnahmeprotokoll

### 2026-09-07

- [x] RailTrace Save Button eingebaut.
- [x] Letzter RailTrace-Stand wird gespeichert und wiederhergestellt.
- [x] BlockEditor- und FlowEditor-Inspector breiter und naeher am linken Rand dargestellt.
- [x] Build, Unit-Tests, Install und Launch waren erfolgreich.
- [x] Panel-Akzentfarben typisiert und Legacy-Defaults beim Laden normalisiert.
- [x] Panel-Resize-Griff vergroessert und sichtbarer gemacht.
- [x] Harte Workspace-Grenzen rechts und unten mit Dock-Reserve umgesetzt.
- [x] Magnetisches Andocken an Workspace- und Nachbarpanel-Kanten vorbereitet.
- [x] FlowEditor Auto-Pan beim Node-/Facet-Drag am Viewportrand korrigiert und per Regressionstest abgesichert.
- [x] Einfache Recording-JSONL-Pipeline fuer Overlay und Accessibility-Events angelegt und RailTrace-Projektion angebunden.
- [x] RailTrace kann gespeicherte Recording-Sessions auflisten und eine aktive Session als Step-Projektion anzeigen.
- [x] Recording-Events werden in RailTrace der letzten Activity zugeordnet, damit passive Activity-Segmente und aktive Aktionen zusammenhaengen.

### 2026-09-15

- [x] Flowchart-Nodes verwenden pluginweit quadratische 96-x-96-Viewports.
- [x] Alte rechteckige Node-Views werden unter Erhalt ihrer Mittelpunkte migriert.
- [x] Layout, Rendering, Hit-Testing, Routing, Minimap und Viewport-Fit verwenden dieselbe Standardgroesse.
- [x] Zoom-Buttons publizieren Viewport-State sofort und benoetigen keinen Canvas-Tap mehr.
- [x] Manuell selektierte und aktive Runtime-Nodes erhalten eine deutlich sichtbare Doppelkontur mit Halo.
- [x] Facet-Collapse ist persistent, verschachtelungsfest und Undo-/Redo-faehig.
- [x] Workflow-/Recording-/DryRun-Lifecycle-Semantik sowie Workflow-End-Terminator sind modelliert.
- [x] Plugin-, Host-, Installations-, Kaltstart-, Zoom-, Auswahl- und DryRun-Smoke-Tests erfolgreich.
