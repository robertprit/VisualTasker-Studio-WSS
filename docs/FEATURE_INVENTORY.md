# VisualTasker Studio WSS Feature Inventory

Stand: 2026-10-11

## Zweck

Dieses Inventar ist die nachpruefbare Produkt- und Architekturuebersicht des aktuellen
Repository-Stands. Es ersetzt keine Milestone-Abnahme. Die Detailnachweise, Abhaengigkeiten und
Release-Zuordnungen stehen in `FEATURE_STATUS_MATRIX.md`; der verbindliche 1.0-Umfang steht in
`architecture/RELEASE_1_0_SCOPE.md`.

## Statusvertrag

- `IMPLEMENTED`: produktiv verdrahtet und durch Code plus Test, Build oder dokumentierte Abnahme belegt.
- `PARTIAL`: nutzbarer Anteil vorhanden, aber Vertrag, Produktverdrahtung oder Abnahme ist unvollstaendig.
- `PLANNED`: in Roadmap oder Architekturvertrag konkret vorgesehen, noch nicht implementiert.
- `CONCEPT`: Zielbild oder Anwendungsfall ohne freigegebenen Implementierungsslice.
- `DEFERRED`: bewusst ausserhalb des aktuellen 1.0-Pfads.
- `BLOCKED`: konkrete Voraussetzung verhindert den Beginn oder Abschluss.
- `UNKNOWN`: Repository liefert keinen ausreichenden belastbaren Nachweis.

Datei- oder Modulbestand allein bedeutet nicht `IMPLEMENTED`. Interfaces, Katalogeintraege und
Demo-Anwendungen werden als `PARTIAL`, `PLANNED` oder `CONCEPT` klassifiziert, solange der produktive
WSS-Pfad nicht belegt ist.

## Repository- und Modulbestand

| Bereich | Produktrolle | Einbindung | Bewertung |
| --- | --- | --- | --- |
| `app` | Android Workspace Shell, Runtime Bridge, Panels, Recorder, Overlay | Root-App | produktiver Host |
| `visualtasker-blockeditor/workflow-core` | kanonischer Workflow- und Workspace-Vertrag | Composite Build | produktive Wahrheit |
| `visualtasker-blockeditor/workflow-semantics` | Command Catalog, Properties, semantische Dienste | Composite Build | produktiv |
| `visualtasker-blockeditor/emscript-language-core` | Lexer, Parser und Sprachmodell | Composite Build | produktiver Kern |
| `visualtasker-blockeditor/blockeditor-*` | Blockmodell, Layout, Interaktion, Validator, IR, EMScript und Compose | Composite Build | produktiver Editor |
| `visualtasker-flowchart/flowchart-*` | Flowmodell, Layout, Interaktion, Validator, Serialisierung und Compose | Composite Build | produktiver Editor |
| `VisualTasker M3ShapeMaker Plugin/m3shapemake:design-editor` | eingebetteter Shape-Designer | Composite Build | produktiv eingebettete Teilflaeche; separates WIP |
| `VisualTasker ChartGraph Plugin` | Chart-Domain und Compose-Darstellung | Composite Build | produktive Panel-Grundlage |
| `VisualTasker Vision AI Plugin/android:vision-contracts` | neutrale Vision-Vertraege | Composite Build | produktiver Vertrag |
| `VisualTasker Vision AI Plugin/android:vision-yolo` | YOLO-Provider/Demo | nicht von `app` abhaengig | isolierter Provider, nicht WSS-Runtime |
| `VisualTasker Integrations Plugin:integration-contracts` | stabile IDs fuer externe Adapter | Composite Build | produktiver Vertrag, Implementierungen teils im Host |
| `VisualTasker IME Keypad` | separate Android-IME | separates Projekt | Companion-WIP, nicht Root-Build |
| `compose-dnd` plus `WssComposeDndAdapter` | UI-lokaler DnD-Transport | Maven + Host-Adapter | produktiv, keine kanonische Datenwahrheit |

## EMScript Command Surface

Die folgende Liste bildet `VisualTaskerCommandCatalog` ab. Sie ist die gemeinsame Sicht fuer
Text-, Block- und Flow-Projektion. Ein Eintrag im Katalog belegt Syntax, Metadaten und visuelle
Zuordnung, aber nicht automatisch einen produktiven Live-Provider. Der produktive M5-Providerpfad
ist derzeit auf `wait`, native Pointer/Gesten und MultiTouch begrenzt; Core-/Adapterfunktionen aus
`WorkspaceBasicRuntime` werden in M5-8/M5-10 konsolidiert.

| Familie | Registrierte kanonische Namen | Laufzeitgrenze |
| --- | --- | --- |
| Event/Timing/Feedback | `onStart`, `wait`, `beep`, `vibrate`, `log` | `wait` M5-Provider; uebrige Basic-/Adapterpfade |
| Geometrie/Gestenwerte | `point`, `lineSegment`, `quadraticBezierSegment`, `cubicBezierSegment`, `path`, `pointerPath`, `multiPath` | M5-4 bis M5-6 implementiert |
| Eingabe/Gesten | `click`, `clickPoint`, `swipe`, `multiSwipe`, `touch` | Koordinaten/Pfade implementiert; semantisches `click` geplant |
| Vision/Marker/Scene | `findTemplate`, `screenshot`, `ocr`, `findText`, `highlight`, `markerSave`, `markerLoad`, `markerDelete`, `templateDefine`, `templateCompare`, `sceneSave` | Katalog/Panel teils produktiv; Vision-Provider offen |
| Daten/System | `datastorePut`, `datastoreGet`, `File.readText`, `file.writeText`, `Clipboard.get`, `clipboard.set`, `cache.clear`, `Sys.info`, `Env.get` | Basic-/Adapterpfad; M5-8/M5-10 pruefen |
| Variablen/Kontrollfluss | `set`, `get`, `repeat`, `while`, `if` | unterstuetzter V1-Umfang implementiert; LET ist Sprachdeklaration |
| Logik/Literale | `screenContains`, `boolean`, `and`, `or`, `operate`, `compare`, `number`, `string`, `list` | Projektion implementiert; A11y-Query nicht M5-live |
| REM/Flow-Metadaten | `rem.region`, `rem.variableBulk`, `rem.expressionCapsule`, `rem.flowBreak`, `rem.offPageOut`, `rem.offPageIn`, `rem.group`, `rem.layoutHint` | runtime-inerte Projektionsmetadaten |
| Custom Tabs | `ChromeTab.open`, `ChromeTab.unbind`, `chromeTab.isSupported`, `ChromeTab.bind`, `ChromeTab.create`, `ChromeTab.mayLaunchUrl`, `ChromeTab.requestPostMessageChannel`, `ChromeTab.postMessage`, `ChromeTab.validateRelationship` | Open/Support produktive Hostadapter; Rest geplant |
| Tasker | `Tasker.runTask`, `Tasker.setVariable`, `Tasker.emitEvent`, `tasker.isInstalled`, `tasker.isEnabled`, `Tasker.cancel`, `tasker.getVariable`, `Tasker.clearVariable`, `tasker.getVariables`, `Tasker.lastResult`, `Tasker.error`, `Tasker.action`, `Tasker.pluginAction`, `Tasker.profileEnable`, `Tasker.profileDisable`, `Tasker.profileToggle`, `Tasker.profileState` | Hostbridge teilweise produktiv; M5-Provider offen |
| Shizuku | `Shizuku.exec`, `Shizuku.shell`, `shizuku.isInstalled`, `shizuku.isAvailable`, `shizuku.getUid`, `Shizuku.permissionState`, `Shizuku.requestPermission`, `Shizuku.bindUserService`, `Shizuku.unbindUserService`, `Shizuku.systemService`, `Shizuku.call` | Availability/Shell/UserService teilweise produktiv; M5-Provider offen |
| Termux | `Termux.run`, `Termux.shell`, `Termux.api`, `termux.isInstalled`, `Termux.canRunCommands`, `Termux.writeStdin`, `Termux.cancel`, `termux.get` | RUN_COMMAND teilweise produktiv; Resultat/Cancellation offen |
| scrcpy | `Scrcpy.start`, `Scrcpy.stop`, `Scrcpy.touch`, `Scrcpy.hostAvailable`, `Scrcpy.devices`, `Scrcpy.connect`, `Scrcpy.disconnect`, `scrcpy.isRunning`, `scrcpy.get`, `Scrcpy.key`, `Scrcpy.text`, `Scrcpy.scroll`, `Scrcpy.setClipboard`, `Scrcpy.setScreenPower`, `Scrcpy.rotate` | Sessionmetadaten teilweise; externer Host/Video nicht belegt |
| ChartGraph | `Chart.create`, `Chart.show`, `Chart.export`, `Chart.hide`, `Chart.remove`, `chart.exists`, `Chart.setData`, `Chart.setOptions`, `Chart.add`, `Chart.update`, `Chart.removeData`, `Chart.clear`, `chart.get`, `Chart.capture` | Panel/Domain teilweise; Runtime-Provider offen |

Gross-/Kleinschreibung in dieser Tabelle folgt dem aktuellen kanonischen Katalog. Akzeptierte
Legacy-Aliase bleiben in `CommandCatalog.kt` und den V1-Normalisierungen dokumentiert.

## Produktive Panels

Der Workspace kennt die Paneltypen `RecorderSteps` (RailTrace), `SceneInspector`, `BlockEditor`,
`Flowchart`, `Screenshot` (Canvas Detail), `Marker`, `Vision`, `Datastore`, `Emscript`, `RuntimeLog`,
`TextEditor`, `LogConsole`, `DebugInfo`, `M3Director`, `ChartGraph` und den historischen Typ `Vt2Vt`.
Der VT2VT-Paneltyp ist nicht mehr Bestandteil der persistierbaren Workspace-Panelmenge; die
Verbindungseinstellungen liegen im Settings-Pfad. `Emscript`/`TextEditor` sowie
`RuntimeLog`/`LogConsole` sind noch ueberlappende historische Typnamen und muessen in M6-0 als
Alias- oder Migrationsfall geschlossen werden.

## Feature-Katalog

### A. Architektur, Workspace und Sprache

| ID | Feature | Status |
| --- | --- | --- |
| A-01 | Kanonischer WorkflowGraph und CanonicalWorkspaceDocument | IMPLEMENTED |
| A-02 | Schema-1-Migration und Schema-2-Persistenz | IMPLEMENTED |
| A-03 | Stabile semantische Entity- und Relation-IDs | IMPLEMENTED |
| A-04 | WorkspaceOperations und atomare WorkspaceTransactions | IMPLEMENTED |
| A-05 | Semantisches Property Schema und Typvalidierung | IMPLEMENTED |
| A-06 | Persistente Source Anchors und Identity Reconciliation | IMPLEMENTED |
| A-07 | Lossless Source Apply, Validierung und Rollback | IMPLEMENTED |
| A-08 | Drei-Editor-Conformance und gemeinsame semantische History | IMPLEMENTED |
| A-09 | EMScript Lexer, Parser, AST/IR und Generator fuer V1-Kern | PARTIAL |
| A-10 | LET/SET, Ausdruecke, Bedingungen und Schleifen | IMPLEMENTED |
| A-11 | Point, Path, PointerPath und MultiPath als typisierte Werte | IMPLEMENTED |
| A-12 | Funktionen, Parameter und Return | PLANNED |
| A-13 | TRY/CATCH/FINALLY/THROW | DEFERRED |
| A-14 | Mehrere Workflow-Roots, Subworkflows und Funktionsbibliotheken | PLANNED |
| A-15 | Benutzerdefinierte Befehle, Makros und erweiterte Collections | PLANNED |
| A-16 | Vollstaendige V1-Referenz und versionsfaehige Migration | PARTIAL |

### B. Runtime und native Automation

| ID | Feature | Status |
| --- | --- | --- |
| R-01 | WorkflowGraph zu ExecutionPlan | IMPLEMENTED |
| R-02 | Deterministischer ExecutionRunner | IMPLEMENTED |
| R-03 | Lifecycle, Diagnostics, Trace und Cancellation | IMPLEMENTED |
| R-04 | Capability-/Provider-Trennung und Registry | IMPLEMENTED |
| R-05 | Android Runtime Bridge mit Doppelstartschutz | IMPLEMENTED |
| R-06 | Nativer abbrechbarer `wait`-Provider | IMPLEMENTED |
| R-07 | Nativer `clickPoint`-Provider | IMPLEMENTED |
| R-08 | Swipe und lineare Mehrpunktpfade | IMPLEMENTED |
| R-09 | Quadratische und kubische Bezier-Gesten | IMPLEMENTED |
| R-10 | Hold, Delay, Repeat, Duration und Speed | IMPLEMENTED |
| R-11 | Gesture Concurrency, Timeout und Serviceverlust | IMPLEMENTED |
| R-12 | Native MultiTouch-, Pinch- und parallele Pointer-Pfade | IMPLEMENTED |
| R-13 | Semantische A11y-Ziele (`clickElement`, `clickText`) | PLANNED |
| R-14 | Provider-Prioritaeten, Availability und Fallback-Policy | PLANNED |
| R-15 | Prozessweiter Headless Runtime Host | PLANNED |
| R-16 | Runtime-Recovery und persistente Execution Sessions | PLANNED |
| R-17 | Runtime-Kontrollfluss fuer Funktionen und strukturierte Fehler | BLOCKED |
| R-18 | Scheduler, Breakpoints und Langlauf-Gate | PLANNED |

### C. Editor-System und visuelle Programmierung

| ID | Feature | Status |
| --- | --- | --- |
| E-01 | Nativer Compose BlockEditor | IMPLEMENTED |
| E-02 | Toolbox, TypeRegistry und Command-Katalog-Projektion | IMPLEMENTED |
| E-03 | Statement-, Reporter- und Value-Verbindungen | IMPLEMENTED |
| E-04 | Drag, Snap, Detach, Reparenting und Typed Docking | IMPLEMENTED |
| E-05 | IF/ELSEIF/ELSE-Container und typisierte Reporter | IMPLEMENTED |
| E-06 | Block-Mutator und dynamische Definitionen | PARTIAL |
| E-07 | Material-3-Expressive Block-/Reporter-Shapes | PARTIAL |
| E-08 | Eingebetteter Block-/Node-/Shape-Designer | PARTIAL |
| E-09 | Nativer Compose FlowEditor | IMPLEMENTED |
| E-10 | AutoArrange, Branch-Layout und Manhattan-Routing | PARTIAL |
| E-11 | Flow-Nodes, Ports, Verbindungen, Facets und Detail-Layer | IMPLEMENTED |
| E-12 | EMScript TextEditor mit Source Apply | IMPLEMENTED |
| E-13 | Apply-Diff, Diagnosen und atomarer Rollback | IMPLEMENTED |
| E-14 | Fokus, Selection und Navigation ueber drei Editoren | IMPLEMENTED |
| E-15 | Typisierter Inspector und direkte Parameterbearbeitung | PARTIAL |
| E-16 | Benutzerdefinierte Rich-Compose-Block-/Node-Komponenten | CONCEPT |
| E-17 | Funktionen/Patches und Synthesizer-/Patchbay-Projektion | CONCEPT |

### D. Workspace, Panels, Window und Overlay

| ID | Feature | Status |
| --- | --- | --- |
| W-01 | Compose Workspace Shell und Floating Panel Host | IMPLEMENTED |
| W-02 | Panel Drag, Resize, Z-Order, Minimize und Maximize | PARTIAL |
| W-03 | Panel Snap, Docking und harte Workspace-Grenzen | PARTIAL |
| W-04 | Persistente Panel-Geometrie, Akzent und Layoutzustand | IMPLEMENTED |
| W-05 | SideRails, Toolbars, Bottom Sheets und Panel-Wechsel | PARTIAL |
| W-06 | Systemweites StudioOverlayService | PARTIAL |
| W-07 | Floating Toolbar, Marker und Screenshot-Hide/Restore | PARTIAL |
| W-08 | Einheitlicher WindowSession-/Focus-/Input-Vertrag | PLANNED |
| W-09 | Layoutprofile und robuste Crash-Wiederherstellung | PLANNED |
| W-10 | MultiDisplay Window Sessions | DEFERRED |

### E. Headless, Display und MultiDevice

| ID | Feature | Status |
| --- | --- | --- |
| H-01 | Runtime unabhaengig von Editorprojektionen | IMPLEMENTED |
| H-02 | Ausfuehrung ohne sichtbare Studio-Activity | PLANNED |
| H-03 | Foreground-Service-Lifecycle und UI-Reattachment | PLANNED |
| H-04 | DisplaySession, Display Registry und explizite Display-ID | PLANNED |
| H-05 | DisplayCoordinateMapper | PARTIAL |
| H-06 | VirtualDisplay und MediaProjection Surface | PLANNED |
| H-07 | Display-spezifische Capture- und Gesture-Capabilities | PLANNED |
| H-08 | Separate App-Sessions auf VirtualDisplays | CONCEPT |
| H-09 | VT2VT Pairing, LAN/WebSocket und USB/ADB-Transport | PARTIAL |
| H-10 | Versionierte Workspace-/RuntimeTrace-Mirror | PARTIAL |
| H-11 | Remote Capability Execution | PLANNED |
| H-12 | Remote Display-/Session-Integration | DEFERRED |

### F. Accessibility, Capture und Vision

| ID | Feature | Status |
| --- | --- | --- |
| V-01 | AccessibilityService-Anbindung | IMPLEMENTED |
| V-02 | Native GestureDescription-Ausfuehrung | IMPLEMENTED |
| V-03 | Accessibility Tree im Scene Inspector | PARTIAL |
| V-04 | Bounds- und Element-Hit-Testing | PARTIAL |
| V-05 | Semantische Element-/Text-Suche | PLANNED |
| V-06 | Gemeinsamer CaptureFrameProvider | PLANNED |
| V-07 | Marker ROI, Crop und Canvas Detail | PARTIAL |
| V-08 | ML Kit OCR | PLANNED |
| V-09 | OpenCV Template Matching | PLANNED |
| V-10 | YOLO/LiteRT Vision-Vertrag und Provider-Katalog | PARTIAL |
| V-11 | Confidence, Mehrdeutigkeit und Sensor-Fusion | PARTIAL |
| V-12 | Vorher-/Nachher-Observation und Freshness | PARTIAL |

### G. Recorder, RailTrace, Inspector und Debugging

| ID | Feature | Status |
| --- | --- | --- |
| T-01 | RecordingSession, Raw Events und kanonische Interactions | IMPLEMENTED |
| T-02 | Room-Schema, Screenshot Assets und stabile Provenance | IMPLEMENTED |
| T-03 | StepCandidateDocument und stabile Candidate IDs | IMPLEMENTED |
| T-04 | Review, Korrektur und persistente Entscheidungen | IMPLEMENTED |
| T-05 | Recording Integrity und Evidence Inspector | IMPLEMENTED |
| T-06 | RailTrace fuer Record, Dry/Wet Run und WatchDog | IMPLEMENTED |
| T-07 | Timeline, Lanes, Activity-Band, Seek, Speed und Bookmark | IMPLEMENTED |
| T-08 | Replay und INTERRUPTED-Session-Recovery | IMPLEMENTED |
| T-09 | Scene Inspector mit Tree, Evidence und Resources | PARTIAL |
| T-10 | Recorder zu Workflow-Proposal ueber Junktor | PARTIAL |
| T-11 | Runtime Variable Inspector und Breakpoint-Debugger | PLANNED |
| T-12 | Perugger Ambiguitaets-/Interpretations-Workbench | DEFERRED |

### H. Ressourcen, Daten, Assets und Worldview

| ID | Feature | Status |
| --- | --- | --- |
| D-01 | WorkspaceSessionStore und Workspace-Persistenz | IMPLEMENTED |
| D-02 | Datastore Panel und Domain-/Resource-Projektionen | PARTIAL |
| D-03 | Marker, Templates, Screenshots und Vision-Ressourcen | PARTIAL |
| D-04 | VisualAsset Catalog, `.ema` Bridge und Inbox | PARTIAL |
| D-05 | Legacy Studio Resource Import | IMPLEMENTED |
| D-06 | Toolbox Sets fuer Standard/Custom/Mixed/Plugin | PARTIAL |
| D-07 | Typisierter paneluebergreifender DnD-Transport | IMPLEMENTED |
| D-08 | Workspace Explorer, Scopes, Where-Used und Delete Guards | PLANNED |
| D-09 | Projekt-Backup, Export, Import und Portabilitaet | PARTIAL |
| D-10 | Worldview-Entities, Relations und Panel-Projektionen | PARTIAL |
| D-11 | Read-only Knowledge/RAG Index | PARTIAL |
| D-12 | Lernendes World Model und ExperienceRecords | DEFERRED |

### I. Plugins, externe Provider und Companion Apps

| ID | Feature | Status |
| --- | --- | --- |
| I-01 | Shell Plugin Contracts, Descriptor Catalog und Host Coordinator | IMPLEMENTED |
| I-02 | BlockEditor- und FlowEditor-Shell-Plugins | IMPLEMENTED |
| I-03 | ShapeMaker Design-Editor im VisualAsset Panel | PARTIAL |
| I-04 | ChartGraph Panel und Chart-Domain | PARTIAL |
| I-05 | Vision-AI-Vertraege und YOLO-Providerbeschreibung | PARTIAL |
| I-06 | Tasker Action/Event/Result-Bridge und Settings | PARTIAL |
| I-07 | Shizuku Availability, Permission und UserService/Shell | PARTIAL |
| I-08 | Termux RUN_COMMAND Adapter | PARTIAL |
| I-09 | Custom Chrome Tabs Open/Settings | PARTIAL |
| I-10 | scrcpy Session-Metadaten und VT2VT-nahe Steuerung | PARTIAL |
| I-11 | Join, Ktor Server und Sherpa | PLANNED |
| I-12 | Externe Provider Discovery ueber M5-8 Policy | PLANNED |
| I-13 | VisualTasker IME Keypad Companion | PARTIAL |

### J. Lokale KI, Spezialfunktionen und Benchmarks

| ID | Feature | Status |
| --- | --- | --- |
| X-01 | Lokales KI-Panel und SLM/LLM-Anbindung | DEFERRED |
| X-02 | KI-generierte WorkspaceOperations mit Review | DEFERRED |
| X-03 | Natuerlichsprachliche Suche und Diagnose | DEFERRED |
| X-04 | Agenten-Orchestrierung und autonome Skripte | DEFERRED |
| X-05 | Chartfamilien ueber Line/Bar/Pie/Candlestick hinaus | PARTIAL |
| X-06 | Programmierbare Overlay-Indikatoren | CONCEPT |
| X-07 | Home Automation und Tabellenautomation | CONCEPT |
| X-08 | Paper Trading Benchmark | DEFERRED |
| X-09 | Tank Fortress/PhysicsBox/VT2VT Benchmark | DEFERRED |
| X-10 | Morf/Kubo/Glyph/Nox Visual Agents | DEFERRED |

### K. Qualitaet, Sicherheit und Release

| ID | Feature | Status |
| --- | --- | --- |
| Q-01 | JVM-Modultests und Android-App-Unit-Tests | IMPLEMENTED |
| Q-02 | Three-Editor-Conformance-Harness | IMPLEMENTED |
| Q-03 | Runtime-/Provider-Regressionstests | IMPLEMENTED |
| Q-04 | Physische Android-Geraete-Smoke-Gates | PARTIAL |
| Q-05 | Performance-Gates fuer Validator und Source Apply | IMPLEMENTED |
| Q-06 | Vollstaendiges Permission-, Privacy- und Security-Audit | PLANNED |
| Q-07 | Langlauf-, Stress- und Background-Kompatibilitaet | PLANNED |
| Q-08 | Backup-/Restore- und Migrationsabnahme | PLANNED |
| Q-09 | Reproduzierbarer signierter Release-Build | PLANNED |
| Q-10 | Vollstaendige Produkt- und EMScript-Dokumentation | PARTIAL |
| Q-11 | Alpha | PLANNED |
| Q-12 | Beta | PLANNED |
| Q-13 | Release Candidate | PLANNED |
| Q-14 | Stable 1.0 | PLANNED |

## Neu oder gegen die Ausgangsliste korrigiert

1. M5-6 MultiTouch ist `IMPLEMENTED`, nicht geplant. Architekturbericht, Tests und physischer
   SM-S918B-Nachweis sind vorhanden.
2. ChartGraph, ShapeMaker Design-Editor, Vision Contracts und Integrations Contracts sind echte
   Composite-Build-Abhaengigkeiten. Das beweist die Modulgrenze, aber nicht automatisch eine
   abgeschlossene Produktintegration.
3. Tasker, Shizuku, Termux und Custom Tabs besitzen produktiven Android-Code und Settings-
   Verdrahtung. Sie bleiben `PARTIAL`, weil sie noch nicht ueber den M5-8 Providervertrag laufen.
4. Recorder-Persistenz, Integrity, Review und Recovery sind wesentlich weiter als eine reine
   Vorarbeit. Der fehlende Schritt ist die explizit freigegebene Workflow-Proposal-Publikation.
5. Das Vision-Plugin markiert YOLO/LiteRT katalogseitig als verfuegbar; WSS bindet aber nur die
   neutralen Contracts ein. Daher bleibt die WSS-Runtime-Integration `PARTIAL`.
6. Der produktive Workspace besitzt historische Panel-Aliase und einen nicht mehr persistierbaren
   VT2VT-Paneltyp. Das ist ein M6-0-Migrationspunkt, kein neues Feature.

## Bekannte Doppelungen und Architektur-GAPs

- `WorkspaceBasicRuntime`/DryRun und der neue ExecutionRunner existieren noch nebeneinander. Der
  produktive Live-Pfad fuer M5-Provider ist eindeutig; Altpfade muessen in M5-10 klassifiziert werden.
- `Emscript` und `TextEditor` sowie `RuntimeLog` und `LogConsole` sind ueberlappende Panelnamen.
- Native Overlay-Fenster und Compose Workspace-Panels besitzen noch keinen gemeinsamen
  WindowSession-Vertrag.
- Capture und Gesten verwenden noch keinen durchgaengig expliziten Display-Kontext.
- Plugin-Registrierung/Readiness und M5 ExecutionProvider-Aufloesung sind noch getrennte Schichten.
- Worldview-Projektionen sind vorhanden, aber noch kein scene-uebergreifendes lernendes World Model.
- History ist prozesslokal; unbekanntes EMScript wird fail-closed statt opaque roundgetrippt.

## Nachweisgrenze

Dieses Audit hat Quellcode, Tests, Modulverdrahtung und vorhandene Abschlussdokumente ausgewertet.
Es hat keinen neuen Build und keinen Geraetetest ausgefuehrt, weil der Auftrag ausschliesslich
Dokumentation verlangt. Geraeteabnahmen werden nur dort als Nachweis verwendet, wo sie bereits in
den versionierten Milestone-Dokumenten festgehalten sind.
