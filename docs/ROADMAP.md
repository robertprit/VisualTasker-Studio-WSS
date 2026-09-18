# VisualTasker Studio WSS Roadmap

Stand: 2026-09-18

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
- RailTrace trennt Run, Records und WatchDog als eigene Quellenfamilien; DryRun/BasicRun werden ueber `ExecutionTrace` als stabile Operation-Spur projiziert.
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
- WatchDog-Ereignisse sind als eigene `ExecutionTrace`-kompatible Quelle angebunden und erfassen App-Start, Activity-/App-Wechsel, Button-Klicks sowie Screen-Lock/-Unlock; visuelle Absetzung und LogConsole-Links sind noch nicht final.
- Floating Overlays und LiveMarker sind als Basis vorhanden, aber noch nicht final integriert.
- Marker/Vision/Canvas/Datastore brauchen saubere Persistenz, klare Contracts und reale Pipeline-Anbindung.
- MainScreen ist noch nicht bereinigt oder als alte fixe Studio-Darstellung sauber ersetzt.
- Einstellungen sind noch nicht vollstaendig uebergreifend konsolidiert.

## Meilensteine Bis Stable V1

Aktueller Arbeitsstamm: M10/C6 Release-Haertung. M1, M2.1 und M9.1 sind technisch
abgeschlossen. Settings, Plugin-Readiness, Persistenz, Release-Preflight und die
automatische Smoke-Matrix sind implementiert; offen bleiben vor allem manuelle
visuelle Abnahmen, externe Provider und die MainScreen-Produktentscheidung.

### M1: Editor-Core Finalisieren

- [x] BlockEditor und FlowEditor Bedienlogik angleichen: Selection, Fokus, Haptik, Sound, Kontextmenues.
- [x] Inspector-Inhalte in beiden Editoren entschlacken und Debugdetails in DebugInfo/LogConsole verschieben.
- [x] Inspector-Sheets in BlockEditor und FlowEditor ohne unteren Abstand direkt an den Panelrand andocken und Eingabe-/Drag-Hit-Zonen absichern.
- [x] Scrollleisten, Minimap-Position, Zoom-auf-Fokus und Centering konsistent machen.
- [x] Block/Node-Selektion bidirektional stabil synchronisieren.
- [x] Viewport-Freeze- und Fokuswechsel-Regressionen weiter testen.
- [x] Panel-Layout-Regeln fuer BlockEditor und FlowEditor parallel pflegen: Iconbar, SideRail, Inspector, Trash, Minimap, Resize- und Hit-Zonen.

M1-Abnahme 2026-09-17: Beide Editoren verwenden die gemeinsame Interaktionsrichtlinie
fuer Selection/Haptik/Sound, bidirektionale Workspace-Selektion, Viewport-Scrollbars,
Minimap, Fokus-Zoom und am unteren Panelrand angedockte Inspector-Sheets. Controller-,
Selection-, Visual-Policy- und Panel-Regressionstests decken die gemeinsamen Verträge ab.

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
- [x] Hauptstamm im semantischen Modus kompakter und gleichmaessiger ausrichten.
- [x] Branch-Ziele naeher an Decision-Nodes ziehen und Treppenstruktur verbessern.
- [x] Technische Detail-Nodes als Layer behandeln, ohne AutoArrange-Hauptfluss zu zerlegen.
- [x] Facet-Bounds und Collapsed-Facets duerfen AutoFit und Scroll-Startposition nicht dominieren.
- [x] AutoArrange-Modi klar trennen: Semantik, Analyse, Kompakt, Manuell.
- [x] Optionales Slot-/Dock-Modell fuer Reporter-/Operator-/Dataflow-Nodes entwerfen, ohne FlowEditor zum 2D-BlockEditor umzubauen.
- [x] Port- und Routing-Regeln fuer beidseitige Reporter/Dataflow-Ports weiter haerten.

M2.1-Abnahme 2026-09-17: Der Standardmodus `Semantik` nutzt den kompakten
Hauptfluss mit semantischem Spaltenumbruch; `Analyse`, `Kompakt` und `Manuell`
sind getrennte Profile. Branch-Treppen, Mindestabstaende, kompakte Dataflow-Ketten,
beidseitige Reporter-Ports und deklarierte Sequence-/Branch-Ports sind durch
Layout-/Compose-Tests abgesichert. AutoFit ignoriert Hintergrund-Facets sowie
Mitglieder eingeklappter Facets, damit technische Detailbereiche die Startansicht
nicht mehr verkleinern oder verschieben.

### M3: EMScript Stabilisieren

- [x] Kanonische Syntax vollstaendig dokumentieren.
- [x] Parser aus Demo-Subset herausziehen.
- [x] Generator fuer alle vorhandenen Grundbefehle vervollstaendigen.
- [x] Ersten `CommandCapabilityDescriptor` als gemeinsame Ableitung aus dem CommandCatalog fuer RuntimeGate, Adapterbedarf und Live-Implementierungsstatus einfuehren.
- [x] Runtime-/DryRun-/FlowRuntime-Events transportieren stabile `diagnosticCode`-Werte bis in Log-/Debug-Projektionen.
- [x] Command-Catalog fuer alle geplanten Kategorien weiter pflegen und Descriptor in Settings, Toolboxen, Diagnostik und Adapter-Registry durchziehen.
- [x] Roundtrip-Tests WSS -> BlockEditor -> EMScript -> IR -> FlowEditor erweitern.
- [x] Fehlerdiagnosen mit Source-Mapping, Node/Block-ID und Textzeile ausgeben.
- [x] EMScript-Import schreibt Source-Zeilenmetadaten auf erzeugte Workspace-Bloecke; IR und Flowchart-SourceReference uebernehmen sie.
- [x] Runtime-Capability-Gates fuer noch fehlende Adapter klar anzeigen.

### M4: Floating Overlays Und LiveMarker

- [x] Overlay-Berechtigungsflow finalisieren.
- [x] Floating Toolbar fuer Aufnahme, Screenshot, Marker und Runtime-Aktionen fertigstellen.
- [x] Floating Inspector fuer LiveMarker und ausgewaehlte UI-Elemente anbinden.
- [x] LiveMarker Overlay fuer Region, Point, Swipe, Spline, Path und Multi-Modus umsetzen.
- [x] Recording Start/Stop aus Overlay erreichbar machen.
- [x] Overlay-Zustaende speichern und wiederherstellen.

M4-Abnahme 2026-09-18: Der Berechtigungsflow setzt die angeforderte Overlay-Aktion
nach Rueckkehr aus den Systemeinstellungen fort. Floating Toolbar und Inspector
verwenden persistente Positionen. LiveMarker speichert Point, Region, Swipe, Spline
und Path inklusive Multi-Gruppe; der Inspector zeigt Markerstatus und den aktuellen
Accessibility-Kontext mit App, Element, Bounds und Interaktionsstatus.

### M5: Marker, Canvas Und Vision

- [x] Marker Panel als reine Steuerkonsole finalisieren.
- [x] Canvas als Screenshot-Hintergrund/Arbeitsflaeche konsolidieren.
- [x] Screenshot-Karussell und gespeicherte Assets robust anbinden.
- [x] Marker-Persistenz fuer Region, Template, Point, Swipe, Spline, Path, Multi und Draw-Elemente fertigstellen.
- [x] Vision Panel mit Live-Crop und Vergleichsbild finalisieren.
- [x] Filter, Graustufen, Falschfarben, Maskierung und Thresholds ins Vision Panel verlegen.
- [x] Template/Marker-Kommandos mit Datastore und Vision verbinden.

M5-Abnahme 2026-09-17: Marker bleibt eine Steuerkonsole; Workspace Canvas ist die
originale visuelle Arbeitsflaeche und Canvas Panel ihre Detailprojektion. Screenshot-
Assets und Marker werden ueber die gemeinsame SideRail ausgewaehlt. Marker-Schema v2
persistiert Draw-Typ und Multi-Gruppe atomar. Vision besitzt getrennte Live-/Referenz-
Crops mit Original, Graustufen, Falschfarben, Kontrast, threshold-gesteuerter Maske,
Kanten und Invers. UI- und EMScript-Speichern erzeugen Worldview-Ressource sowie
Scene-Block/Flow-Node ueber den gemeinsamen Workspace-Mutationspfad.

### M5.1: Workspace Canvas Als Gemeinsame Visuelle Arbeitsflaeche

- [x] Workspace Canvas zeigt bei Canvas/Marker sowie visuellen RailTrace-Steps die Screenshot-Arbeitsflaeche, sonst das neutrale Grid; Vision bleibt exklusiv.
- [x] Canvas Panel bleibt Detailprojektion: fokussierter Crop, Step-Ausschnitt, Marker-Detail oder Screenshot-Ausschnitt.
- [x] Vision Panel bleibt exklusiver Machine-Vision-Arbeitsbereich fuer Crop, Filter, Template-Vergleich, OCR/OCV/YOLO/A11Y.
- [x] Marker Panel bleibt Steuerkonsole fuer Marker, nicht Canvas-Duplikat.
- [x] Marker-Speichern erzeugt immer Ressource plus passenden Block und Node in der Scene-/Marker-Kategorie.
- [x] Marker-Modi Region, Template, Point, Swipe, Spline, Path, Multi und Draw-Familie konsistent persistieren.
- [x] RailTrace-Record-Steps projizieren Screenshots, Klicks, Swipes, Pfade und erkannte Entities auf Workspace Canvas und Canvas Panel.

### M6: Datastore Und Worldview

- [x] Datastore Panel als zentrale Datenansicht fuer Ressourcen ausbauen.
- [x] Screenshots, Marker, Templates, Runtime-Daten und Sessions gemeinsam anzeigen.
- [x] Visual UI Memory Projektion fuer Worldview, Provider, Facetten und Suggestions anlegen.
- [x] Canvas-/Marker-Ressourcen im Datastore als Worldview-Slice projizieren.
- [ ] VisualAsset Manager fuer ShapeMaker-Assets, Block-Shapes, Node-Shapes, Port-Shapes, Icons und Compose-Overlays ausbauen.
- [x] `.ema` / `application/vnd.emscript.motion+json` als Austauschformat fuer Visual Assets anbinden.
- [x] Toolbox-Sets fuer Standard, Custom, Mixed, JavaScript und Plugin-Familien speichern und im Workspace auswahlen.
- [x] Import-Mapping aus altem Studio fuer Marker/Templates/Screenshots erweitern.
- [x] Worldview Contracts fuer UI-Elemente, Ressourcen, Overlays und Vision-Ergebnisse haerten.
- [x] Export/Import und Migrationen fuer Workspace-Ressourcen einbauen.

M6-Datastore-Abnahme 2026-09-17: Screenshot-, Marker-, Template-, Vision- und
Workflow-Ressourcen werden gemeinsam mit Recording-Sessions und Runtime-Key/Values
in den `WorkspaceResourceBundle` projiziert. Sessions und Runtime-Werte besitzen
stabile Dataset-IDs, Owner, MIME-Typen und Metadaten und stehen dadurch Worldview,
Inspector und paneluebergreifendem DnD als adressierbare Ressourcen zur Verfuegung.
Ressourcen werden zusaetzlich in einem versionierten JSON-Bundle atomar gespeichert,
beim Start geladen und aus unversionierten Legacy-Bundles migriert. Human-Marker,
Accessibility, OCR, OCV, YOLO, DOM und Runtime erhalten getrennte Worldview-Provider.

### M6.1: Visual Abstraction Layer Und VisualAsset Manager

- [ ] VAL als gemeinsame Sprache fuer Block-, Node-, Port-, Facet-, Marker- und Overlay-Darstellung festziehen.
- [ ] M3ShapeMaker als VisualAsset Manager fuer Blockformen, Nodeformen, Portformen, Icons, Handles und Facet-Designs anbinden.
- [ ] BlockDesigner zeigt parallel betroffene FlowchartNode-Vorschau an.
- [ ] Asset-Katalog fuer Standard-, Custom- und Plugin-Assets speichern, bearbeiten, loeschen und versionieren.
- [x] Austauschbare Toolbox-Sets fuer Standard, Custom, Mixed, JavaScript und Plugin-Familien einrichten.
- [x] `.ema`-Dateien fuer Shapes, Motion, Draw-Pfade und wiederverwendbare visuelle Assets spezifizieren.
- [ ] Compose-Overlay-Controls nur als bewusstes Rich-Overlay-Konzept behandeln; gezeichnete Block-/Node-Kerne bleiben performant und serialisierbar.
- [ ] DnD-Listen als generische Item-Transport-Schicht fuer Marker, RailTrace, Inspector, Datastore, TextEditor, BlockEditor und FlowEditor ausbauen.

### M7: Recording Und RailTrace

- [x] Recording Sessions speichern und laden.
- [x] RailTrace Session-Liste und aktive Session-Auswahl einfuehren.
- [x] Timeline fuer passive Activity-/Scene-Dauer und aktive Events weiter haerten.
- [x] RailTrace-Modi `Program`, `Step`, `Live`, `Replay` und `Curate` als Vertrag modellieren.
- [x] RailTrace Surface-Modi `Records`, `Run` und `WatchDog` ueber die internen Detailmodi legen.
- [x] RailTrace Source-Kinds `WorkflowRun`, `Recording` und `WatchDog` als explizite Datenquellen einfuehren.
- [x] DryRun/BasicRun ueber `ExecutionTrace` in RailTrace-Steps projizieren.
- [x] Gesamtzeit-Progressbar fuer RailTrace als Timespan-Indikator einbauen.
- [x] RailTrace-Step-Auswahl mit Marker Panel synchronisieren.
- [x] DryRun-/BasicRun-Schritte synchronisieren RailTrace-Auswahl, Position und Index.
- [x] DryRun/LiveRun/Step-Steuerung als globale Runtime-Bedienung in RailTrace konzentrieren.
- [x] Redundante Dry/Wet-Run Buttons aus Editor-Panels entfernt; WetRun bleibt global in AppTopBar/Floating Iconbar, DryRun-Step-Navigation bleibt in RailTrace.
- [x] Run- und WatchDog-Spuren zeigen keine Recorder-Demo-Daten mehr als stillen Fallback.
- [x] RailTrace-Leerzustand beschreibt source-spezifisch, ob Run, Recording oder WatchDog aktiv ist.
- [x] Recorder-Events in denselben Trace-Vertrag wie Runtime-Operationen ueberfuehren.
- [x] WatchDog-Events in denselben Trace-Vertrag wie Runtime-Operationen ueberfuehren.
- [x] Replay Schritt vor/zurueck und Speed-Control mit echten Recording-Daten testen.
- [x] RailTrace-Fokus mit TextEditor, BlockEditor und FlowEditor dauerhaft synchron halten.
- [ ] Canvas Panel als fokussierten Scene-Inspector fuer Record-Steps, Screenshots, Activities, Klicks, Gesten und erkannte Entities ausbauen.
- [ ] Marker Panel als jederzeit editierbare Anpassungszentrale fuer RailTrace/Canvas/Datastore etablieren.
- [ ] Junktor vorbereiten: Record/Scan/Trace/User Intent/Datastore zu pruefbaren Block-, Flow- und EMScript-Vorschlaegen konkretisieren.

### M7.1: RailTrace Als Zentrale Runtime-Sicht

- [x] RailTrace zeigt strikt getrennte Modi: Last Records/Live Recording, Dry/Wet Run und WatchDog.
- [x] DryRun/WetRun globale Steuerung aus RailTrace/AppTopBar heraus finalisieren und redundante Run-Buttons aus Editor-Panels entfernen.
- [x] Step-Liste in SideRail fuehren; Panel-Inhalt zeigt Timeline, Progress, Lanes und Step-Inspector.
- [x] Aktiver Runtime-Step synchronisiert TextEditor-Zeile, BlockEditor-Block, FlowEditor-Node/Kante und Step-Liste ueber denselben Selection-State.
- [ ] Aktiver Runtime-Step zentriert alle betroffenen Viewports sichtbar ohne Drag-/Connect-Interaktion zu stoeren.
- [x] Activity-/Scene-Band als passive Dauer oberhalb aktiver Event-Lanes darstellen.
- [x] Fehler, Invalids und Resultate direkt auf Timeline projizieren.
- [x] Variables-/Observations-Lane ergaenzen.
- [x] Recording-Replay-Hakeln analysieren und Render-/Tick-Strategie optimieren.

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

- [x] Tasker-Settings-Tab, Beispielprofil, Testevent und Action mit Rueckmeldung final pruefbar machen.
- [x] Tasker Result-/Error-Namen kurz und konfliktfrei halten, z.B. `Tasker.lastResult` und `Tasker.error`.
- [x] Shizuku UserService statt privater/reflection APIs fuer echte Shell-Ausfuehrung finalisieren.
- [x] Termux RUN_COMMAND mit dokumentierter `allow-external-apps=true`-Pruefung finalisieren.
- [x] CustomChromeTab Settings fuer Farben, Close/Share/Download/Favoriten/Menu/ActionIcon und BottomBar vervollstaendigen.
- [x] scrcpy/VT2VT USB-/ADB-nahe Verbindung als schneller Transportpfad vorbereiten.

### M10: Release-Haertung

- [x] Smoke-Test-Checkliste fuer Geraet, DryRun, LiveRun, Overlay und Editor-Sync erstellen.
- [x] Persistenz-/Migrationstests fuer Dokumente, Views und Ressourcen ergaenzen.
- [x] Crash-Safety und inkompatible Plugin-/Command-Versionen pruefen.
- [ ] Settings vollstaendig konsolidieren.
- [x] MainScreen als produktiven Pfad stilllegen; fixe Studio-Darstellung als Workspace-Layout-Preset festlegen.
- [x] Release Notes und Known Limitations erstellen.
- [x] Reproduzierbares Stable-V1-Release-Candidate-Gate und Tag-Ablauf vorbereiten.

## C-Meilensteine: Cross-Cut Architektur

### C1: Human Canvas, Vision Pipeline Und RailTrace-Projektionen Trennen

- [x] Workspace Canvas als menschliche Originaldarstellung und Arbeitsflaeche von der Vision-Pipeline trennen.
- [x] Vision Panel als exklusiven Bereich fuer Crop, Template-Vergleich, Filter, Processing und Machine-Vision-Auswertung behandeln.
- [x] Canvas Panel als Detailprojektion der menschlichen Arbeitsflaeche markieren, nicht als Vision-Arbeitsbereich.
- [x] Canvas Panel zeigt bei aktivem gespeichertem Marker nur dessen Ausschnitt; ohne aktiven Marker bleibt das ganze Bild als Fallback sichtbar.
- [x] Vision-Siderail von Marker-/Canvas-Modusbuttons entkoppeln.
- [x] Canvas Panel als echte Detailprojektion eines ausgewaehlten Workspace-/RailTrace-Elements schaerfen.
- [x] RailTrace-Step auf Workspace Canvas und Canvas Detail synchron fokussieren.
- [x] Marker-Auswahl auf Workspace Canvas und Marker Panel bidirektional synchronisieren.
- [x] Vision Panel mit eigenem Screenshot-/Referenz-Auswahlmodell absichern.
- [x] Recorder-Step-Screenshots sauber mit RailTrace und Canvas Detail verbinden.
- [x] Workflow/Record/WatchDog-Rail-Modi im RailTrace weiter trennen.
- [x] `ExecutionTrace` als gemeinsame Run-Operation-Spur fuer Runtime, Flowchart und RailTrace eingefuehrt.
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

- [x] Kanonische EMScript-Grammatik fuer alle Stable-V1-Kommandos festschreiben.
- [x] Parser, Generator und Formatter fuer Grundbefehle, Control, Variablen und Reporter vervollstaendigen.
- [x] Runtime-Ausfuehrung fuer vorhandene Grundbefehle mit DryRun, BasicRun und LiveRun absichern.
- [x] Capability-Gates fuer A11Y, Vision, Tasker, Termux, Shizuku, scrcpy und CustomTabs einheitlich melden.
- [x] Command-Katalog mit Kategorien, Argumenttypen, Defaults, Hilfetexten und Block-/Node-Mapping erweitern.
- [x] Testscript-Suite in Basic, Vision, Runtime, Plugin und Stress-Scripte aufteilen.
- [x] Roundtrip-Tests Text -> IR -> Block -> Flow -> EMScript -> Runtime erweitern.
- [x] Source-Mapping fuer Fehler von Runtime-Event zu Textzeile, Block-ID, Node-ID und RailTrace-Step herstellen.
- [x] Snackbar-/LogConsole-Fehler mit Direktlinks zu betroffenen Panels und Einstellungen ausstatten.
- [x] Release-Check fuer noch nicht implementierte Kommandos und inkompatible Plugin-Adapter einfuehren.

### C4: Recorder, RailTrace Und Floating Overlays Fertigstellen

- [ ] Floating Toolbar mit Screenshot, Scan-Varianten, Record/WatchDog, Workspace, Replay und Marker-Modus finalisieren.
- [ ] Floating Panels stabil beweglich, minimierbar und vor Screenshots automatisch ausblendbar machen.
- [x] Recorder-Events fuer Activity-Wechsel, Klicks, Swipes, Text, Gesten, Screenshots und Fehler persistieren.
- [x] Recorder-Oekologie analysiert: Recording, Record, Observation, WindowTransition, Policy und Perception-Grenzen dokumentiert.
- [x] Window-/Activity-Transitionen als strukturierte Recording-Evidence erfassen.
- [x] ObservationPolicy fuer RECORD, WATCHDOG, RUNTIME, INSPECT und PROBE als kleinen Contract einfuehren.
- [x] Recorder-Window-Evidence auf dem Geraet validiert: echte Activity-Namen statt Widget-Klassen, deterministische Transitionen und keine neuen Events nach Recording-Stop.
- [x] RailTrace-Modi Records, Run und WatchDog datenlogisch strikt trennen.
- [x] RailTrace-Modi Records, Run und WatchDog visuell klarer voneinander absetzen.
- [x] RailTrace-Timeline mit Lanes, Activity-Band, Laufzeitmarker, Progressbar und Step-Inspector finalisieren.
- [x] Replay Schritt vor/zurueck, Geschwindigkeit, Pause und Resume mit echten Recording Sessions testen.
- [x] Recorder-Steps als Marker-/Block-/Node-Vorschlaege ueber Junktor bereitstellen.
- [x] Workspace Canvas zeigt Recorder-Events als menschliche Step-Projektion.
- [x] Canvas Panel zeigt fokussierte Recorder-Step-Details, Screenshots, Klicks, Swipes und Pfade.
- [x] WatchDog-Events in RailTrace ohne Vermischung mit Workflow-Runs darstellen.
- [x] WatchDog-Events in LogConsole ohne Vermischung mit Workflow-Runs und mit Source-Links darstellen.
- [x] WatchDog-Events in RailTrace in System-, Provider- und Runtime-Spuren aufteilen.
- [x] RailTrace-Step-Auswahl ueber Timeline, SideRail und Panel-Content auf denselben Workspace-Selection-State fuehren.
- [x] WatchDog registriert App-Start, Screen-Lock/-Unlock, Activity-/App-Wechsel und Button-Klicks als Provider-Trace.

### C5: Visual UI Memory, Datastore Und Asset-System Stabilisieren

- [x] Datastore in Bereiche fuer Human, Machine Vision, Runtime, Resources, Plugins und Junktor aufteilen.
- [x] Marker, Screenshots, Templates, Recorder-Steps und Vision-Ergebnisse als versionierte Ressourcen persistieren.
- [x] Worldview-Entities fuer A11Y, OCR, OCV, YOLO, DOM, Marker und Runtime vereinheitlichen.
- [x] Visual UI Memory als gemeinsame Sicht fuer Inspector, Datastore, Marker, Vision und Junktor haerten.
- [x] M3ShapeMaker als VisualAsset Manager fuer Block-, Node-, Port-, Icon- und Overlay-Shapes anbinden.
- [x] `.ema`-Export/Import fuer Shapes, Pfade, Motion und Draw-Kommandos spezifizieren.
- [x] Toolbox-Sets fuer Standard, Custom, Mixed, JavaScript und Plugin-Familien speichern und umschalten.
- [x] Asset-Abhaengigkeiten in Workspace-Projekten speichern, migrieren und bei fehlenden Ressourcen melden.
- [x] Import-Mapping aus altem Studio fuer Marker, Templates, Screenshots und Resources fertigstellen.
- [x] RAG-Vorbereitung als read-only Index ueber stabile Datastore-/Worldview-Ressourcen planen.

### C6: Plugin-Integration, Settings Und Stable-V1-Abnahme

- [x] FlowEditor-Layer fuer Dataflow, Runtime-Trace und Diagnosen persistent und zentral in den Settings schaltbar machen.
- [x] RailTrace-Quelle, Detailmodus, Zeitskala und Timeline-Zoom zentral auf denselben persistenten Panel-State abbilden.
- [x] RailTrace-Settings-Persistenz mit Roundtrip-, Fallback- und Grenzwerttests absichern.
- [x] Plugin-Einstellungen fuer Tasker, Termux, Shizuku, scrcpy/VT2VT, CustomTabs und Vision zentral zusammenfassen.
- [x] Tasker Profile, Testevent, Action, Result und Error-Pfade als Beispielpaket pruefbar machen.
- [x] Termux RUN_COMMAND, Shizuku UserService und CustomChromeTab Runtime Adapter releasefest anbinden.
- [x] VT2VT Verbindung fuer Observer, Workspace-Mirror und RuntimeTrace-Mirror stabilisieren.
- [x] Settings fuer Layout, Farben, UI-Skalierung, Schriftgrad, Minimap, Rail-Zustaende und Plugin-Gates konsolidieren.
- [x] MainScreen als produktiven Pfad stilllegen; alte fixe Studio-Darstellung bleibt ein Workspace-Layout-Preset.
- [x] Smoke-Test-Matrix fuer Editor-Sync, Runtime, Overlay, Recorder, Vision, Datastore und Plugins erstellen.
- [x] Persistenz-, Migrations- und Crash-Recovery-Tests fuer Workspace-Projekte und Ressourcen ergaenzen.
- [x] Release Notes, Known Limitations, Backup-Hinweise und Debug-Anleitung schreiben.
- [x] Stable-V1-Build und automatisierten Release-Candidate-Check vorbereiten.
- [ ] Manuelle Geraete-Abnahmematrix vollstaendig durchlaufen.
- [ ] Finalen Repo-Stand nach Freigabe committen, signieren, taggen und pushen.

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

1. Aktiven Trace-Step in TextEditor, BlockEditor und FlowEditor zentrieren und markiert halten.
2. [x] Recording-Replay-Hakeln stabilisieren: Step-Suche logarithmisch, Timeline-Autoscroll nur bei Stepwechsel statt bei jedem 50-ms-Tick.
3. [x] DebugInfo/LogConsole um filterbare `Trace`-Kategorie mit strukturierten Source-Links erweitern.
4. FlowEditor AutoArrange/Autorouting weiter haerten: Branch-Naehe, Hauptstamm, beidseitige Reporter-/Dataflow-Ports.
5. Canvas Detail zum fokussierten Scene-Inspector fuer Activity, Evidence und erkannte Entities ausbauen.
6. EMScript Command-Katalog mit Runtime-/Adapter-Descriptoren weiter vervollstaendigen.
7. Marker-Persistenz mit Datastore/Worldview verbinden und Scene-Block/Node-Erzeugung absichern.
8. RailTrace WatchDog-Visualisierung fuer Tasker, Plugin, VT2VT und Provider-Ereignisse weiter farblich/semantisch trennen.
9. [x] VisualAssets/ShapeMaker-Hub oeffnen, `.ema`-Descriptor pruefen und Toolbox-Set-Konzept im Workspace verankern.
10. Roundtrip- und Persistenztests fuer Workflow, RecordTrace, Marker und Canvas-Evidence erweitern.

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
- [x] `ExecutionTrace` mit stabilen Run-IDs, Operation-IDs, Status, Source-Refs und Evidence-Refs angelegt.
- [x] Runtime-Trace-Summary, FlowRuntimeSnapshot und RailTrace-Steps nutzen die gemeinsame Trace-Projektion.
- [x] RailTrace unterscheidet WorkflowRun, Recording und WatchDog als Datenquellenfamilien.
- [x] Run/WatchDog-Fallbacks wurden von Recording-Demo-Daten getrennt.
- [x] Validiert: `RailTraceModelsTest`, `WorkspaceDryRunRuntimeTest`, `EmscriptDryRunFlowRuntimeMapperTest`, `assembleDebug`, Installation und Foreground-Start.
- [x] Recording-Steps werden als `ExecutionTrace`-kompatible RecordTrace-Operationen mit stabiler Record-Source, EvidenceRefs und erhaltenen visuellen Feldern projiziert.
- [x] Validiert: `RecordingEventStoreTest`, kombinierte RailTrace-/Runtime-/Flowchart-Mapping-Tests, `assembleDebug`, Installation und Foreground-Start.
- [x] Roadmap-Struktur bereinigt: aktueller Arbeitsstamm markiert, erledigte RecordTrace-Aufgaben aus den naechsten Schritten entfernt und C4/M7 an den Trace-Stand angepasst.
- [x] WatchDog-/Tasker-/Plugin-/VT2VT-nahe Recorder-Events werden als `ExecutionTrace`-kompatible WatchDogTrace-Operationen ohne Recorder-Demo-Fallback in RailTrace projiziert.
- [x] Validiert: `RecordingEventStoreTest`, `RailTraceModelsTest`, `assembleDebug`; Installation und Foreground-Start folgen im aktuellen Slice.
- [x] WatchDog-Erfassung erweitert: App-Start, Screen-Lock/-Unlock, Activity-/App-Wechsel und Button-Klicks werden als WatchDog-relevante Events erkannt.
- [x] Canvas Panel zeigt bei aktivem gespeichertem Marker einen echten Crop-Detailausschnitt; ohne aktiven Marker bleibt die ganze Screenshot-Ansicht als Fallback erhalten.
- [x] RailTrace Live/WatchDog trennt System-, Provider- und Runtime-Spuren; Tasker/Plugin/VT2VT-Events landen nicht mehr in einer neutralen Runtime-Lane.
- [x] RailTrace-Auswahl aus Timeline, SideRail und Panel-Content aktualisiert denselben Workspace-Selection-State inklusive Replay-Index und Position.
- [x] Validiert: `RailTraceModelsTest`, `RecordingEventStoreTest`, `assembleDebug`, Installation und Foreground-Start.
- [x] RailTrace-Step-Fokus mappt `ExecutionTrace`-Quellen auf BlockEditor-Block, FlowEditor-Node oder FlowEditor-Kante und ist mit Model-Tests abgesichert.
- [x] Validiert: `WorkspaceSelectionStateTest`, `RailTraceModelsTest`, `RecordingEventStoreTest`, `assembleDebug`, Installation und Foreground-Start.
- [x] Editor-Panels enthalten keine eigenen Dry-/Wet-Run Buttons mehr; Run-Steuerung bleibt global in TopAppBar/FloatingBar und zentral in RailTrace.
- [x] EMScript DryRun-Events tragen Source-Line-Metadaten bis in RailTrace-Steps, Inspector und WorkspaceSelectionState.
- [x] RailTrace Replay-, Seek-, Zoom- und Tempo-Aktionen publizieren denselben persistenten Stepper-State.
- [x] TextEditor-Zeilenfokus verwendet echte TextLayout-Zeilenhoehe fuer Scroll-/Drop-Metrik statt DP-Schaetzung.

### 2026-09-17

- [x] Recorder Window-/Activity-Evidence inklusive Baseline, Transitionen, Activity-Filter und Session-Stop auf dem Geraet validiert.
- [x] RailTrace Records, Run und WatchDog mit eigenen Akzentfarben, Modus-Badges und source-spezifischen Controls visuell getrennt.
- [x] Recording-Session-Auswahl erscheint ausschliesslich im Records-Modus.
- [x] RailTrace-Header fuer kompakte und ausgefahrene SideRail responsiv abgesichert.
- [x] Echtes Recording-Replay mit Anfang, Schritt vor/zurueck, Pause/Resume und diskreten Speed-Stufen getestet.
- [x] Vollstaendige App-Unit-Test-Suite und Debug-Build erfolgreich; Installation und Foreground-Start erfolgreich.
- [x] R1 abgeschlossen: Root-App, BlockEditor und FlowEditor seriell und ohne konkurrierende Gradle-Builds vollstaendig getestet.
- [x] FlowEditor-Port-Hitbox-Test an die gemeinsam von Rendering und Hit-Test verwendete Port-Geometrie angeglichen; Debug- und Release-Suiten sind gruen.
- [x] Aktuellen Debug-Build auf dem SM-S918B installiert, `MainActivity` im Vordergrund verifiziert und RailTrace-Startansicht visuell geprueft.
- [x] Semantisches FlowEditor-AutoArrange arbeitet auf dem sichtbaren Projektionsgraphen; ausgeblendete Reporter-, Variablen- und Operator-Nodes beeinflussen den Hauptfluss nicht mehr und behalten ihre gespeicherte Geometrie.
- [x] Workspace-Canvas-Sichtbarkeit fuer Canvas/Marker und visuelle RailTrace-Steps als eigener, getesteter Vertrag festgelegt; Vision und Workflow-Editoren behalten ihre dedizierten Flaechen.
- [x] M2.1/M5.1-Build auf dem SM-S938B installiert und visuell abgenommen: FlowEditor-AutoArrange bleibt ohne ausgeblendete Detail-Nodes kompakt; RailTrace stellt den beim ersten Rendern effektiven Step samt Screenshot auf dem Workspace Canvas dar.
- [x] Record-Step-Canvas-Projektion vereinheitlicht: Bounds, Click/Point, Swipe, Path und Spline werden aus Recorder-Evidence modelliert und in Workspace- sowie Detailcanvas identisch gerendert.
- [x] Floating-Screenshots speichern fuer neue Recording-Sessions einen maschinenlesbaren `screenshotPath`; alte Sessions bleiben ueber den Screenshot-Fallback darstellbar.
- [x] Auf dem SM-S938B visuell abgenommen: RailTrace `Click STOP` fokussiert den echten 145x138-Bereich im Canvas Detail samt Bounds und Click-Marker; eine alte Marker-Auswahl ueberstimmt den Record-Step nicht mehr.
- [x] FlowEditor-Grundlayout auf ein 96-x-96-Node-Raster mit jeweils einer Nodebreite/-hoehe Abstand umgestellt.
- [x] Vertikaler Hauptstamm, abgesetzte True-/ElseIf-Branches, gerade False-Fortsetzung und terminale Stop-Position als Layoutregeln gehaertet.
- [x] Branch-Ausgaenge liegen unten: False links, True-/ElseIf-Ausgaenge stabil von rechts nach links; parallele Kanten erhalten getrennte Routing-Lanes.
- [x] Aggressives Zentrieren bei externer Node-Selektion entfernt; Runtime-Fokus und explizites Zentrieren bleiben erhalten.
- [x] Node-Autopan folgt dem Finger ohne doppelte Viewport-Kompensation.
- [x] FlowEditor-SideRail schaltet explizit zwischen Einzel- und Next-follow-first-Drag sowie sichtbaren/verdeckten Facet-Handlern um.
- [x] Zweizonen-Drag entfernt; die gesamte Node ist unabhaengig vom Zoomfaktor eine einheitliche Drag-Zone.
- [x] Node-Inhalte verwenden kontrastreiche Material-Symbole statt schlecht lesbarer Textlabels; Semantik/Inspector behalten vollstaendige Bezeichnungen.
- [x] Unverbundene Flow-Nodes schreiben keine Positionsdaten mehr in unverbundene BlockEditor-Roots zurueck.
- [x] Layout-, Interaction-, Compose- und App-Tests sowie Debug-Build und Installation erfolgreich; visuelle Abnahme wegen gesperrtem Testgeraet ausstehend.
- [x] Oberste AutoArrange-Richtlinie festgelegt: linearer Hauptstamm vor Kompaktheit; Conditions/Reporter quer dazu; jeder Branch bleibt als eigene zusammenhaengende Lane; Join erst unter dem tiefsten Branch.
- [x] Branch-Staffelung verschiebt nicht mehr nur den Einstieg, sondern die komplette Sequenz bis zum synthetischen Join; Regressionstest fuer Lane-Kohärenz und Join-Position ergaenzt.
- [x] RailTrace-Replay startet keine konkurrierende Scroll-Animation mehr bei jedem Zeit-Tick; die Timeline zentriert nur noch beim Stepwechsel.
- [x] RailTrace-Step-Suche verwendet gecachte Startzeiten und binaere Suche; Regressionstest mit 1.000 Steps ergaenzt.
- [x] LogConsole-Trace-Eintraege tragen strukturierte Source-Targets und koennen RailTrace-Step, Textzeile, Block, Node oder Kante im gemeinsamen Selection-State fokussieren.
- [x] DebugInfo projiziert die letzten Trace-Eintraege aus demselben LogStore und verwendet denselben Source-Link-Vertrag.
- [x] RailTrace-Canvas-Detail besitzt einen strukturierten Scene Inspector fuer Activity, Aktion, Status, Zeit, Geometrie, Gesten, Evidence und passende Recorder-Observations; Vision-Pipeline-Daten bleiben getrennt.
- [x] M1 technisch abgeschlossen: gemeinsame Editor-Interaktion, Selection-Synchronisation, Bottom-Inspectoren und Viewport-Regeln sind implementiert und getestet.
- [x] M2.1 technisch abgeschlossen: vier eindeutige Arrange-Modi, semantischer Hauptfluss, Branch-Treppen, Detail-Layer, Facet-sicheres AutoFit sowie beidseitige Dataflow-Ports sind implementiert und getestet.
- [x] Vollstaendige App-, Flowchart-Layout-/Compose-/Interaction- und BlockEditor-Compose-Tests gruen; Debug-Build auf dem SM-S918B installiert und `MainActivity` per Kaltstart als Vordergrundaktivitaet verifiziert.
