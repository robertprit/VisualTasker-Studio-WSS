# VisualTasker Studio WSS Feature Status Matrix

Stand: 2026-10-11

Diese Matrix konkretisiert `FEATURE_INVENTORY.md`. `Produktpfad` unterscheidet produktive
Verdrahtung (`ja`), einen produktiven Teilpfad (`teil`), isolierte Contracts/Prototypen (`nein`) und
reine Planung (`-`). Referenzen sind repository-relativ.

Audit-Baseline `09b03dd`: 146 Features, davon 54 `IMPLEMENTED`, 42 `PARTIAL`, 32 `PLANNED`,
12 `DEFERRED`, 5 `CONCEPT` und 1 `BLOCKED`. M5-8 wurde nach dieser Baseline abgeschlossen. Die
42 damaligen `PARTIAL`-Eintraege bleiben deshalb in der Abschlussmatrix unten unveraendert
nachvollziehbar; spaetere Statusverschiebungen werden separat ausgewiesen und nicht rueckwirkend in
die Baseline eingerechnet. Aktueller Matrixstand nach M7-0: 62 `IMPLEMENTED`, 46 `PARTIAL`,
20 `PLANNED`, 12 `DEFERRED`, 5 `CONCEPT` und 1 `BLOCKED`.

## Architektur und Sprache

| ID | Status | Produktpfad | Nachweis | Abhaengigkeit / Release |
| --- | --- | --- | --- | --- |
| A-01 | IMPLEMENTED | ja | `workflow-core`; `M2_1_CANONICAL_WORKFLOW_CONTRACT_EXTRACTION.md` | 1.0 Pflicht, geschlossen |
| A-02 | IMPLEMENTED | ja | Workspace-Serializer und Schema-2-Fixtures; M3/M4 Gates | 1.0 Pflicht |
| A-03 | IMPLEMENTED | ja | `SemanticEntityId`, `SemanticRelationId`; M2-4/M3-3 Tests | 1.0 Pflicht |
| A-04 | IMPLEMENTED | ja | `WorkspaceOperation`, `WorkspaceTransaction`, `WorkspaceOperationExecutor` | 1.0 Pflicht |
| A-05 | IMPLEMENTED | ja | `M3_1_SEMANTIC_PROPERTY_SCHEMA.md` und Property-Tests | 1.0 Pflicht |
| A-06 | IMPLEMENTED | ja | `M3_3_PERSISTENT_SOURCE_ANCHORS.md` | 1.0 Pflicht |
| A-07 | IMPLEMENTED | ja | `EmscriptSemanticSourceApply`; M3-2/M3-3 Rollback-Tests | 1.0 Pflicht |
| A-08 | IMPLEMENTED | ja | `M4_CLOSURE_AUDIT.md`; zentrale Conformance-Harnesses | 1.0 Pflicht |
| A-09 | PARTIAL | ja | `emscript-language-core`, `blockeditor-emscript`; Truth Map/Audit | V1-Untermenge dokumentieren |
| A-10 | IMPLEMENTED | ja | Parser/IR/Workspace-Roundtrip und Typed-Assignment-Milestones | 1.0 Pflicht |
| A-11 | IMPLEMENTED | ja | `PointerPathContract.kt`; M5-4 bis M5-6 | 1.0 Pflicht |
| A-12 | PLANNED | - | `RELEASE_1_0_PLAN.md` M5-9 | Sprach-/Editor-Slice vor Runtime |
| A-13 | DEFERRED | - | M4 Closure markiert M4-4 explizit deferred | nicht 1.0 Core |
| A-14 | PLANNED | - | M8-2/M8-3 | nach Resource-Scope |
| A-15 | PLANNED | - | Feature-Roadmap, kein freigegebener Vertrag | optional/post-1.0 |
| A-16 | PARTIAL | teil | `docs/emscript`, Reference Draft, Grammar und Truth Map | M10-6 |

## Runtime und native Automation

| ID | Status | Produktpfad | Nachweis | Abhaengigkeit / Release |
| --- | --- | --- | --- | --- |
| R-01 | IMPLEMENTED | ja | `WorkflowExecutionPlanner.kt`; M5-0 Tests | 1.0 Pflicht |
| R-02 | IMPLEMENTED | ja | `DeterministicExecutionRunner.kt` | 1.0 Pflicht |
| R-03 | IMPLEMENTED | ja | `ExecutionContract.kt`, `ExecutionTrace.kt` | 1.0 Pflicht |
| R-04 | IMPLEMENTED | ja | `ExecutionProviderRegistry` | M5-8 erweitert Policy |
| R-05 | IMPLEMENTED | ja | `RuntimeClient`, `RuntimeProcessHost`; M5-1/M5-H | produktiver Shell-Pfad headless |
| R-06 | IMPLEMENTED | ja | `AndroidWaitExecutionProvider.kt` | 1.0 Pflicht |
| R-07 | IMPLEMENTED | ja | `NativeGestureProvider`; M5-2 | A11y Service erforderlich |
| R-08 | IMPLEMENTED | ja | `NativeTouchProvider.kt`; M5-3 | 1.0 Pflicht |
| R-09 | IMPLEMENTED | ja | `PointerPathContract.kt`; M5-4 | 1.0 Pflicht |
| R-10 | IMPLEMENTED | ja | M5-4/M5-5 Timing- und Roundtrip-Tests | 1.0 Pflicht |
| R-11 | IMPLEMENTED | ja | M5-3/M5-4 Lifecycle-Tests | 1.0 Pflicht |
| R-12 | IMPLEMENTED | ja | `M5_6_NATIVE_MULTITOUCH.md`; Geraeteabnahme SM-S918B | 1.0 Pflicht |
| R-13 | PARTIAL | ja | `NativeAccessibilityProvider`, Target Resolver und M5-7 Geraetegate; `clickElement`/variable Query offen | M5-7 Kernpfad DONE, Rest M5-8/M7-2 |
| R-14 | IMPLEMENTED | ja | `ProviderPolicy.kt`, deterministische Registry, sichere Fallback- und Trace-Tests; M5-8 | 1.0 Pflicht |
| R-15 | IMPLEMENTED | ja | `ExecutionSessionManager`, `AndroidRuntimeHost`, `HeadlessRuntimeService`; M5-H | 1.0 Pflicht |
| R-16 | PARTIAL | ja | bounded Checkpoint, `INTERRUPTED`, kein Auto-Replay; M5-H | Langlauf/Retention M5-10 |
| R-17 | BLOCKED | - | M5-9 benoetigt freigegebenen M4-4-Sprachvertrag | Scope-Entscheidung erforderlich |
| R-18 | PLANNED | - | M5-Pebbles/M5-10 | Debugger optional, Langlauf Pflicht |

## Editoren und visuelle Programmierung

| ID | Status | Produktpfad | Nachweis | Abhaengigkeit / Release |
| --- | --- | --- | --- | --- |
| E-01 | IMPLEMENTED | ja | `blockeditor-compose`; `BlockEditorShellPanel.kt` | M6-1 Produktgate |
| E-02 | IMPLEMENTED | ja | `CommandCatalog.kt`, Registry-Module, Toolbox-Tests | M6-1 |
| E-03 | IMPLEMENTED | ja | Block-Domain/Interaction/Validation Tests | M6-1 UX-Gate |
| E-04 | IMPLEMENTED | ja | Dock/Detach/Reparent Conformance und DnD-Tests | M6-1 |
| E-05 | IMPLEMENTED | ja | Container/Branch/Reporter Fixtures | M6-1 |
| E-06 | PARTIAL | teil | BlockDesignFactory und Mutator-Projektionstests | Designer-Produktgate fehlt |
| E-07 | PARTIAL | teil | Material-Shape-Projektionen | visuelle Vollabnahme fehlt |
| E-08 | PARTIAL | teil | ShapeMaker `design-editor`, VisualAsset Panel/Bridge | M6-6/M8-6 |
| E-09 | IMPLEMENTED | ja | `flowchart-compose`; `FlowchartShellPanel.kt` | M6-2 Produktgate |
| E-10 | PARTIAL | ja | Flowchart Layout/Interaction Tests; bekannte Routing-UX-Luecken | M6-2 Blocker fuer Beta |
| E-11 | IMPLEMENTED | ja | Flowchart Domain/Compose und Settings-Layer | M6-2 |
| E-12 | IMPLEMENTED | ja | `WorkspaceEmscriptPanels.kt`, Semantic Source Apply | M6-3 |
| E-13 | IMPLEMENTED | ja | Apply Plan, Diagnostics und Rollback Tests | M6-3 |
| E-14 | IMPLEMENTED | ja | `WorkspaceSelection*`, M4-3 Tests | M6-4 UX-Gate |
| E-15 | PARTIAL | ja | Block/Flow Inspectoren und Scene Inspector | M6-5 |
| E-16 | CONCEPT | nein | Designer-/Rich-Control-Ideen, kein kanonischer Contract | post-1.0 |
| E-17 | CONCEPT | nein | Patchbay/Funktions-Projektion in Roadmap | post-1.0 |

## Workspace, Window und Display

| ID | Status | Produktpfad | Nachweis | Abhaengigkeit / Release |
| --- | --- | --- | --- | --- |
| W-01 | IMPLEMENTED | ja | `WorkspaceScreen.kt`, `PanelState` | 1.0 Pflicht |
| W-02 | IMPLEMENTED | ja | `WindowSessionRegistry`, `StudioWindowHost`, produktive DarkPanel-Gesten | Geraete-UX-Gate offen |
| W-03 | PARTIAL | ja | Snap-/Bounds-Logik und Roadmap-Abnahmen | M6-W |
| W-04 | IMPLEMENTED | ja | `WorkspaceSessionStore`, `AndroidWindowSessionStore`, Persistenz-/Reattach-Tests | Geraete-Recovery offen |
| W-05 | PARTIAL | ja | SideRail/Inspector/Toolbar in `WorkspaceScreen.kt` | M6-0 Alias-Audit |
| W-06 | PARTIAL | ja | `OverlayWindowHost`, Permission-/WindowManager-Diagnosen, Cleanup/Reattach | realer Lifecycle-/FGS-Test offen |
| W-07 | PARTIAL | ja | `LiveMarkerOverlay.kt`, `OverlayStateStore.kt` | M6-W/M7-7 |
| W-08 | IMPLEMENTED | ja | `WindowSessionContracts.kt`, zentrale Registry und Hostadapter | Geraetegate vor Alpha |
| W-09 | PARTIAL | ja | `DisplayContext`, explizite Koordinatenraeume und Insets-Clamp | Nicht-Default-Display bleibt M7-V |
| W-10 | DEFERRED | - | M9-D | post-1.0 |
| H-01 | IMPLEMENTED | ja | Runtime Core ohne Compose-Abhaengigkeit | Basis fuer M5-H |
| H-02 | IMPLEMENTED | ja | `HeadlessRuntimeService`, Android-36 `specialUse`, Notification-Stop; M5-H | 1.0 Pflicht |
| H-03 | IMPLEMENTED | ja | `RuntimeClient`, Session-StateFlow, Reattach-Geraetetest; M5-H | visuelle RailTrace-Abnahme offen |
| H-04 | IMPLEMENTED | ja | M6-W `DisplayContext` und persistierte Display-ID | VirtualDisplay bleibt M7-V |
| H-05 | PARTIAL | teil | Recorder Playback Viewport Transform und lokale Mapper | kein globaler Display-Vertrag |
| H-06 | PLANNED | - | M7-V | optional fuer 1.0 |
| H-07 | PLANNED | - | M7-V | optional fuer 1.0 |
| H-08 | CONCEPT | nein | Anwendungsfall, kein produktiver Host | post-1.0 |
| H-09 | PARTIAL | ja | `Vt2VtConnectionManager`, LAN Transport, USB/ADB Bridge | M9-5 |
| H-10 | PARTIAL | ja | `Vt2VtPayloads`, SessionReducer, Mirror-Tests | M9-6 |
| H-11 | PLANNED | - | M9-7 | optional |
| H-12 | DEFERRED | - | M9-D | post-1.0 |

## Perception, Recorder und Inspection

| ID | Status | Produktpfad | Nachweis | Abhaengigkeit / Release |
| --- | --- | --- | --- | --- |
| V-01 | IMPLEMENTED | ja | `VisualTaskerAccessibilityService.kt`, Manifest | M5-7/M7-2 haerten |
| V-02 | IMPLEMENTED | ja | `NativeGestureProvider` | Runtime Core |
| V-03 | PARTIAL | ja | `SceneInspectorPanel.kt`, Scene Tree Tests | M7-2 |
| V-04 | PARTIAL | ja | A11y Bounds, Marker/Canvas Projektionen | einheitlicher Resolver fehlt |
| V-05 | PARTIAL | ja | M5-7 `clickText` mit deterministischer Suche; allgemeines Elementziel offen | M7-2 |
| V-06 | IMPLEMENTED | ja | `CaptureFrameService`, Accessibility-Provider, Frame-Leases und M7-0 Tests | reale Capture-Abnahme offen |
| V-07 | PARTIAL | ja | gemeinsamer Capture-Koordinatenmapper plus Marker-/Canvas-/Recorder-Crop-Projektionen | visuelle Marker-Synchronisation M7-7 |
| V-08 | PLANNED | - | Vision Catalog markiert ML Kit geplant | Alpha-Ziel fuer Kern-OCR |
| V-09 | PLANNED | - | Vision Catalog markiert OpenCV geplant | optional 1.0 |
| V-10 | PARTIAL | teil | Vision Contracts dependency; YOLO/LiteRT Katalog; Provider separat | optional 1.0 |
| V-11 | PARTIAL | teil | `WorldviewEntityUnifier`, Ambiguity-Projektionen | M7-8 |
| V-12 | PARTIAL | teil | Recorder/Worldview Observations und Zeitmetadaten | M7-8 |
| T-01 | IMPLEMENTED | ja | `RecordingSessionCoordinator`, Room v5 mit Capture-Provenienz | M7-0 Gate |
| T-02 | IMPLEMENTED | ja | Recorder Persistence, `ScreenshotAssetStore` | M7 Pflicht |
| T-03 | IMPLEMENTED | ja | `StepCandidateAssembler`, Candidate Tests | M7-6 |
| T-04 | IMPLEMENTED | ja | `StepReviewStore`, ReviewedStepProjector | M7-6 |
| T-05 | IMPLEMENTED | ja | `RecordingIntegrityValidatorTest`, RailTrace Evidence | M7 Pflicht |
| T-06 | IMPLEMENTED | ja | RailTrace Models/Projections, source-getrennte Modi | M7 Pflicht |
| T-07 | IMPLEMENTED | ja | Playback Controller/Projection und UI | M7 Pflicht |
| T-08 | IMPLEMENTED | ja | Replay Bookmark/Recovery Tests | M7 Pflicht |
| T-09 | PARTIAL | ja | Scene Inspector Tree/Evidence/Resources | Sensor-Fusion/Historie offen |
| T-10 | PARTIAL | teil | `JunktorContracts.kt`, Vorschlagsprojektionen | Freigabe-/Publish-Pfad offen |
| T-11 | PLANNED | - | M5/M7 Roadmap | optional nach Runtime Closure |
| T-12 | DEFERRED | - | Release Plan Erweiterungen | post-1.0 |

## Ressourcen, Plugins und Erweiterungen

| ID | Status | Produktpfad | Nachweis | Abhaengigkeit / Release |
| --- | --- | --- | --- | --- |
| D-01 | IMPLEMENTED | ja | `WorkspaceSessionStore.kt` | Projektformat M8-0 erweitern |
| D-02 | PARTIAL | ja | Datastore Panel und Domain-/Resource-Projektoren | M8-0 |
| D-03 | PARTIAL | ja | WorkspaceResources, Marker/Vision/Recorder Projektionen | M8-6 |
| D-04 | PARTIAL | ja | VisualAsset Catalog/Package/Inbox/Runtime | M8-6 |
| D-05 | IMPLEMENTED | ja | `LegacyStudioResourceImporter/Mapper` plus Tests | Migration-Gate |
| D-06 | PARTIAL | ja | `ToolboxSetMode` und VisualAsset-UI | M8-6 |
| D-07 | IMPLEMENTED | ja | WSS Drag contracts, effects, Compose-DnD adapter | M8-7 erweitern |
| D-08 | PLANNED | - | M8-1/M8-4/M8-8 | Beta-Blocker |
| D-09 | PARTIAL | teil | Resource Codec, Session Store, Backup docs | M8-9/M10-3 |
| D-10 | PARTIAL | teil | Worldview Contracts/Unifier/Panel Projections | kein dynamisches World Model |
| D-11 | PARTIAL | teil | `ReadOnlyKnowledgeIndex.kt`, RAG Architekturdoc | read-only, post-1.0 Ausbau |
| D-12 | DEFERRED | - | Release Plan Erweiterungen | post-1.0 |
| I-01 | IMPLEMENTED | ja | Shell Plugin Contracts/Catalog/Coordinator Tests | M9 Basis |
| I-02 | IMPLEMENTED | ja | BlockEditor/Flowchart Shell Plugins und Tests | M6 |
| I-03 | PARTIAL | ja | `design-editor` dependency, VisualAsset Panel/Bridge | separates WIP nicht auditieren |
| I-04 | PARTIAL | ja | `chartgraph-compose`, `ChartGraphPanel` | Chartfamilien/Produktgate offen |
| I-05 | PARTIAL | teil | `vision-contracts`, Vision Plugin Catalog | Runtime Provider fehlt |
| I-06 | PARTIAL | ja | Tasker Config/Receiver/Session/Feedback/Registration | nicht M5 Provider Registry |
| I-07 | PARTIAL | ja | Shizuku API/Provider/UserService/Inspection Tests | nicht M5 Provider Registry |
| I-08 | PARTIAL | ja | `TermuxRegistration.runCommand` | Resultat-/Cancellation-Vertrag offen |
| I-09 | PARTIAL | ja | `CustomChromeTabRegistration.open` und Settings | Callback-/Navigation-Vertrag offen |
| I-10 | PARTIAL | teil | Scrcpy Session Store plus VT2VT Transports | kein echter scrcpy Video-Host |
| I-11 | PLANNED | - | Integration Catalog Contracts | keine Implementierung im Host |
| I-12 | PARTIAL | teil | `AndroidOptionalProviderInventory`, bestehende Host-Statusvertraege; M5-8 | Ausfuehrungsadapter bleiben M9 |
| I-13 | PARTIAL | nein | separates IME-Projekt und Shell Descriptor | eigenes Produkt-WIP |

## KI, Benchmarks und Release

| ID | Status | Produktpfad | Nachweis | Abhaengigkeit / Release |
| --- | --- | --- | --- | --- |
| X-01 | DEFERRED | - | Release Plan | post-1.0 |
| X-02 | DEFERRED | - | ADR-0003 definiert nur Proposal-Grenze | post-1.0 |
| X-03 | DEFERRED | - | Konzept/RAG Index | post-1.0 |
| X-04 | DEFERRED | - | Human-in-the-loop-Regel | post-1.0 |
| X-05 | PARTIAL | teil | ChartGraph Plugin besitzt weitere Darstellungen, Shell Descriptor nennt Kernset | Plugin-Scope |
| X-06 | CONCEPT | nein | Overlay-/ShapeMaker-Zielbild | post-1.0 |
| X-07 | CONCEPT | nein | Anwendungsfaelle ohne Slice | post-1.0 |
| X-08 | DEFERRED | - | Benchmark-Idee | eigenes Projekt |
| X-09 | DEFERRED | - | Benchmark-Idee | eigenes Projekt |
| X-10 | DEFERRED | - | separate Projekte/Konzepte | eigenes Projekt |
| Q-01 | IMPLEMENTED | ja | 139 App-, 76 BlockEditor-, 9 Flowchart-Testdateien | laufendes Gate |
| Q-02 | IMPLEMENTED | ja | M4 Harness und Closure Audit | laufendes Gate |
| Q-03 | IMPLEMENTED | ja | M5-0 bis M5-7 Tests und reale A11y-Geraeteabnahme | laufendes Gate |
| Q-04 | PARTIAL | ja | versionierte Geraeteabnahmen, keine vollstaendige Matrix | M10-5 |
| Q-05 | IMPLEMENTED | ja | M3-3/M3-4 Performanceprofile | laufendes Gate |
| Q-06 | PLANNED | - | M10-4 | RC-Blocker |
| Q-07 | PLANNED | - | M5-10/M10-1/M10-2 | Alpha/RC-Blocker |
| Q-08 | PLANNED | - | M8-9/M10-3 | Beta/RC-Blocker |
| Q-09 | PLANNED | - | M10-8/M10-9 | Stable-Blocker |
| Q-10 | PARTIAL | teil | umfangreiche Architektur-/Testdocs, V1-Referenz nicht geschlossen | M10-6 |
| Q-11 | PLANNED | - | Release Scope Gate | nach Core-Pflichtumfang |
| Q-12 | PLANNED | - | Release Scope Gate | nach Integrationsscope |
| Q-13 | PLANNED | - | M10-9 | Release Freeze |
| Q-14 | PLANNED | - | M10-9 | finale Freigabe |

## Abschlussmatrix der 42 PARTIAL-Features aus Audit-Baseline 09b03dd

`Nachweis offen` bezeichnet den kleinsten noch fehlenden Produkt- oder Testnachweis. Ein Pebble
darf den Status erst nach produktiver Verdrahtung und diesem Nachweis anheben. M5-7 verschiebt
danach zusaetzlich R-13 und V-05 von `PLANNED` nach `PARTIAL`; diese beiden stehen bewusst nicht in
der historischen 42er-Baseline.

Primaere Verteilung der 42 Baseline-Eintraege: M6 Editor 5, M6-W WindowSession 6, M7
Perception/Recorder 8, M8 Resources 5, M9 Integrationen 11, M10 Sprache/Qualitaet 4 und Post-1.0 3.
Ueberlappende Abhaengigkeiten stehen weiterhin in den Einzelzeilen.

| ID | Vorhanden | Produktverbindung fehlt | Nachweis offen | Milestone | Release | Kleinster abschliessbarer Pebble |
| --- | --- | --- | --- | --- | --- | --- |
| A-09 | Lexer, Parser, AST/IR, Generator | Nicht der gesamte dokumentierte V1-Umfang ist produktiv | Korpus gegen freigegebene Untermenge | M10-6 | CORE_1_0 | Untermenge einfrieren und Conformance-Korpus gruen machen |
| A-16 | Grammar, Truth Map, API-Draft | Eine normative, versionierte Referenz | Docs-/Fixture-Abgleich | M10-6 | CORE_1_0 | V1-Referenz aus akzeptierter Untermenge publizierbar machen |
| E-06 | Factory, Mutator-Projektion | Designer-Aenderung bis kanonische Projektionen | Produktiver Add/Reorder/Delete-Roundtrip | M6-6 | OPTIONAL_1_0 | Einen dynamischen Block end-to-end abnehmen |
| E-07 | Material-Shape-Projektionen | Vollstaendige produktive Shape-Zuordnung | Visuelle Block-/Node-Matrix | M6-6 | OPTIONAL_1_0 | Kernformen auf Zielgeraet visuell abnehmen |
| E-08 | ShapeMaker Design-Editor und Bridge | Stabiler Asset-Lifecycle im Projekt | Save/Edit/Reload/Port-Test | M6-6/M8-6 | OPTIONAL_1_0 | Ein `.ema`-Asset verlustfrei roundtrippen |
| E-10 | AutoArrange, Routing, Interaktion | Robustes Standardlayout fuer grosse/verschachtelte Graphen | Determinismus- und Geraete-UX-Gate | M6-2 | CORE_1_0 | Standardfixtures zweimal identisch arrangieren und bedienen |
| E-15 | Block-/Flow-/Scene-Inspectoren | Einheitlicher typisierter Edit-Pfad | Eingabe-, Fokus- und Undo-Geraetetest | M6-5 | CORE_1_0 | Einen Parameter in Block und Flow identisch aendern |
| W-02 | Drag, Resize, Z-Order, Min/Max | Gemeinsamer WindowSession-Vertrag | Multi-Panel-Gesten und Recovery | M6-W | CORE_1_0 | Eine Session mit Drag/Resize/Z-Order wiederherstellen |
| W-03 | Snap und Bounds | Einheitliche Kanten-/Panel-Snap-Policy | Alle Displaykanten und Panelnachbarn | M6-W | CORE_1_0 | Deterministischen Snap-Fixture plus Geraetetest abschliessen |
| W-05 | SideRail, Toolbars, Sheets | Alias- und Zustandsvertrag je Panel | Panelwechsel/Fokus/Restore | M6-0/M6-W | CORE_1_0 | Historische Panel-Aliase migrieren und testen |
| W-06 | StudioOverlayService | WindowSession, FGS und Recovery | Android-36-Lifecycle-Test | M6-W | CORE_1_0 | Overlay starten, detach/attach, stoppen und recovern |
| W-07 | LiveMarker und OverlayStateStore | Capture-/Record-Session-Anbindung | Hide/restore, Prozessneustart | M6-W/M7-7 | CORE_1_0 | Screenshot-Hide/Restore ohne Stateverlust nachweisen |
| H-05 | Playback-Transforms und Mapper | Globaler Display-/Koordinatenvertrag | Rotation/Insets/Display-ID-Matrix | M6-W/M7-1 | CORE_1_0 | Einen gemeinsamen Mapper fuer Capture und Aktion abnehmen |
| H-09 | VT2VT LAN, WebSocket, USB/ADB | Releasefaehige Session-/Security-Policy | Reconnect- und Transportmatrix | M9-5 | OPTIONAL_1_0 | Zwei Geraete nach Abbruch deterministisch reconnecten |
| H-10 | Payloads, Reducer, Mirror-Tests | Konflikt- und Versionsvertrag | Doppelte/stale Revisionen | M9-6 | OPTIONAL_1_0 | Versionierten Mirror-Roundtrip mit Konfliktdiagnose testen |
| V-03 | A11y-Tree im Scene Inspector | Gemeinsamer Scene-/Evidence-Kontext | Reale Tree-, Selection- und Freshness-Abnahme | M7-2 | CORE_1_0 | Eine reale Scene inklusive Herkunft und Wechsel abnehmen |
| V-04 | Bounds, Marker-/Canvas-Hit-Testing | Einheitlicher Element-/Display-Resolver | Koordinaten- und Stale-Gate | M7-2 | CORE_1_0 | Bounds-Ziel nach Scene-Aenderung sicher neu aufloesen |
| V-07 | ROI, Crop, Canvas Detail | Gemeinsamer CaptureFrame-Kontext | Rotation/Freshness/Marker-Sync | M7-1/M7-7 | CORE_1_0 | Marker-ROI aus exakt demselben Frame roundtrippen |
| V-10 | Vision Contracts, YOLO/LiteRT-Katalog | Produktiver WSS-Vision-Provider | Modell-/Permission-/Failure-Gate | M7-5 | OPTIONAL_1_0 | Einen optionalen Provider kontrolliert discovern/ablehnen |
| V-11 | Unifier und Ambiguity-Projektion | Freigegebene Fusion-Policy | Mehrquellen-Fixtures mit Provenance | M7-8 | CORE_1_0 | A11y/OCR-Treffer ohne Herkunftsverlust korrelieren |
| V-12 | Observations und Zeitmetadaten | Einheitlicher Freshness-Vertrag | Before/after und verschwundene Elemente | M7-8 | CORE_1_0 | Action mit frischer Folgeobservation belegen |
| T-09 | Tree, Evidence, Resources | Fusion, Historie und stabile Entity-Sicht | Scene-Wechsel- und Stale-Test | M7-2/M7-7 | CORE_1_0 | Inspector-Entity ueber zwei Beobachtungen verfolgen |
| T-10 | Junktor-Contracts und Vorschlaege | Expliziter Review-/Publish-Pfad | Record bis Workflow plus Ablehnung | M7-6 | CORE_1_0 | Einen Candidate bestaetigt als WorkspaceTransaction publizieren |
| D-02 | Datastore Panel und Projektoren | Gemeinsamer Scope-/Ownership-Vertrag | Save/Load und Delete-Guard | M8-0 | CORE_1_0 | Eine Ressource mit Owner und Referenz persistieren |
| D-03 | Marker-/Vision-/Recorder-Ressourcen | Einheitliche Resource IDs/Lifecycle | Import/Export und fehlende Referenz | M8-6 | CORE_1_0 | Marker samt Screenshotreferenz portabel speichern |
| D-04 | VisualAsset Catalog, Package, Inbox | Projektweiter Asset-Lifecycle | Edit/replace/delete/where-used | M8-6 | OPTIONAL_1_0 | Ein Asset sicher ersetzen und Referenzen erhalten |
| D-06 | ToolboxSetMode und UI | Persistente projektbezogene Toolbox-Auswahl | Reload und fehlendes Plugin | M8-6 | OPTIONAL_1_0 | Custom/Mixed-Toolbox speichern und wiederherstellen |
| D-09 | Resource Codec, Session Store, Backupdocs | Vollstaendiges Projektpaket | Backup/Restore/Migration auf Neuinstallation | M8-9/M10-3 | CORE_1_0 | Minimalprojekt exportieren, loeschen und identisch importieren |
| D-10 | Worldview Contracts, Unifier, Projektionen | Kein releaseverbindliches dynamisches Modell | Nur Kompatibilitaets-/Nichtblocker-Test | Post-1.0 | POST_1_0 | 1.0-Grenze und stabile Extension Points dokumentieren |
| D-11 | ReadOnlyKnowledgeIndex und RAG-Doku | Produktiver Index/Ingestion-Pfad | Kein 1.0-Nachweis erforderlich | Post-1.0 | POST_1_0 | Read-only Vertrag isoliert als Extension erhalten |
| I-03 | Eingebetteter ShapeMaker-Teil | Versionierter Plugin-/Asset-Vertrag | Abwesenheit und Upgrade | M8-6/M9-8 | OPTIONAL_1_0 | Designer optional laden, fehlen lassen und diagnostizieren |
| I-04 | ChartGraph Domain und Panel | Abgenommener Kernchart-Scope | Daten-, Lifecycle- und Exporttest | M9-8 | OPTIONAL_1_0 | Einen Kernchart speichern, reloaden und exportieren |
| I-05 | Vision Contracts und YOLO-Beschreibung | Runtime-Provider/Discovery | Availability und typed result | M7-5/M9-0 | OPTIONAL_1_0 | Vision-Provider ueber M5-8-Policy sichtbar machen |
| I-06 | Tasker Config, Receiver, Result, Settings | Gemeinsame Provider-Registry | Permission/timeout/cancel/result | M5-8/M9-1 | OPTIONAL_1_0 | Host inventarisieren und kontrollierte Abwesenheit testen |
| I-07 | Shizuku API, Permission, UserService | Gemeinsame Provider-Registry | Binderverlust/Permission-Recovery | M5-8/M9-2 | OPTIONAL_1_0 | Availability-Snapshot plus Permission-Diagnose vereinheitlichen |
| I-08 | Termux RUN_COMMAND | Typed Result/Cancellation in Registry | Timeout, Prozessende, fehlende App | M5-8/M9-3 | OPTIONAL_1_0 | Einen Command mit Result und Cancellation abnehmen |
| I-09 | Custom Tabs Open und Settings | Callback-/Navigation-Vertrag | Browserabwesenheit und Rueckkehr | M5-8/M9-4 | OPTIONAL_1_0 | Open/close/result ueber Provider-Policy testen |
| I-10 | Scrcpy-Metadaten und VT2VT-Transporte | Kein echter Video-/Input-Host | Hostabwesenheit, reconnect, cleanup | M9-5 | OPTIONAL_1_0 | Scope auf Steuerung begrenzen oder Host nachweisen |
| I-13 | Separates IME und Descriptor | Kein WSS-Releasevertrag | Companion-Version/Bridge-Kompatibilitaet | M9-8 | OPTIONAL_1_0 | Versionierten Bridge-Handshake dokumentieren und testen |
| X-05 | Erweiterte ChartGraph-Darstellungen | Kein freigegebener 1.0-Chartumfang | Plugin-eigene visuelle Matrix | Post-1.0 | POST_1_0 | Kerncharts von experimentellen Familien trennen |
| Q-04 | Einzelne Geraeteabnahmen | Vollstaendige Zielmatrix | Android/API/Display/Lifecycle-Matrix | M10-5 | CORE_1_0 | Verbindliche Minimalmatrix definieren und ersten Lauf erfassen |
| Q-10 | Architektur-/Testdoku | Geschlossene V1-Produktreferenz | Link-, Beispiel- und Versionsaudit | M10-6 | CORE_1_0 | Freigegebene EMScript-Untermenge mit Beispielen publizieren |

## Fehlende oder unzureichende Nachweise

- Kein vollstaendiger M6-Geraete- und UX-Abnahmelauf fuer alle drei Editoren und Panelgesten.
- Visuelle RailTrace-Abnahme der laufenden Headless-Session steht noch aus; Prozesshost,
  Foreground-Service, Recovery, Reattach und Stop sind automatisiert und auf SM-S918B getestet.
- Kein gemeinsamer Display-ID-Vertrag fuer Capture, A11y, Gesture und Remote-Sessions.
- M5-8 erfasst externe Hosts einheitlich; vollstaendige asynchrone ExecutionProvider-Resultate fuer Tasker, Shizuku, Termux und Custom Tabs bleiben offen.
- M5-7 belegt semantisches `clickText`; allgemeines `clickElement` und OCR-Zielaufloesung fuer reale
  Kernworkflows bleiben offen.
- Kein durchgaengiger Record -> Review -> Proposal -> explizite Workflow-Publikation -> WetRun-Test.
- Kein abgeschlossenes Projektformat fuer Explorer, Scopes, Asset-Lifecycle und Portabilitaet.
- Keine vollstaendige Device-/Android-Versionen-, Langlauf-, Permission-, Privacy- und Security-Matrix.
- Kein signierter, reproduzierbarer Release-Candidate-Nachweis.
