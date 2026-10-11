# VisualTasker Studio WSS Feature Status Matrix

Stand: 2026-10-11

Diese Matrix konkretisiert `FEATURE_INVENTORY.md`. `Produktpfad` unterscheidet produktive
Verdrahtung (`ja`), einen produktiven Teilpfad (`teil`), isolierte Contracts/Prototypen (`nein`) und
reine Planung (`-`). Referenzen sind repository-relativ.

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
| R-05 | IMPLEMENTED | ja | `AndroidRuntimeBridge.kt`; M5-1 | Headless Host fehlt |
| R-06 | IMPLEMENTED | ja | `AndroidWaitExecutionProvider.kt` | 1.0 Pflicht |
| R-07 | IMPLEMENTED | ja | `NativeGestureProvider`; M5-2 | A11y Service erforderlich |
| R-08 | IMPLEMENTED | ja | `NativeTouchProvider.kt`; M5-3 | 1.0 Pflicht |
| R-09 | IMPLEMENTED | ja | `PointerPathContract.kt`; M5-4 | 1.0 Pflicht |
| R-10 | IMPLEMENTED | ja | M5-4/M5-5 Timing- und Roundtrip-Tests | 1.0 Pflicht |
| R-11 | IMPLEMENTED | ja | M5-3/M5-4 Lifecycle-Tests | 1.0 Pflicht |
| R-12 | IMPLEMENTED | ja | `M5_6_NATIVE_MULTITOUCH.md`; Geraeteabnahme SM-S918B | 1.0 Pflicht |
| R-13 | PLANNED | - | Provider Matrix M5-7 | Alpha-Blocker |
| R-14 | PLANNED | - | Provider Matrix M5-8 | Alpha-Blocker |
| R-15 | PLANNED | - | `WINDOW_HEADLESS_DISPLAY_ROADMAP.md` M5-H | Alpha-Blocker |
| R-16 | PLANNED | - | M5-H/M5-10 | Alpha-Blocker |
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
| W-02 | PARTIAL | ja | produktive Gesten und Session Store; offene Resize-/Focus-Haertung | M6-W |
| W-03 | PARTIAL | ja | Snap-/Bounds-Logik und Roadmap-Abnahmen | M6-W |
| W-04 | IMPLEMENTED | ja | `WorkspaceSessionStore.kt` plus Tests | M6-W Recovery erweitern |
| W-05 | PARTIAL | ja | SideRail/Inspector/Toolbar in `WorkspaceScreen.kt` | M6-0 Alias-Audit |
| W-06 | PARTIAL | ja | `StudioOverlayService.kt`, Manifest-Service | FGS-/Lifecycle-Vertrag fehlt |
| W-07 | PARTIAL | ja | `LiveMarkerOverlay.kt`, `OverlayStateStore.kt` | M6-W/M7-7 |
| W-08 | PLANNED | - | `WINDOW_HEADLESS_DISPLAY_ROADMAP.md` M6-W | Alpha-Blocker |
| W-09 | PLANNED | - | M6-W/M10-2 | Alpha-Blocker |
| W-10 | DEFERRED | - | M9-D | post-1.0 |
| H-01 | IMPLEMENTED | ja | Runtime Core ohne Compose-Abhaengigkeit | Basis fuer M5-H |
| H-02 | PLANNED | - | M5-H | Alpha-Blocker |
| H-03 | PLANNED | - | M5-H | Alpha-Blocker |
| H-04 | PLANNED | - | M6-W/M7-V | expliziter Display-Kontext |
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
| V-05 | PLANNED | - | M5-7/M7-2 | Alpha-Blocker |
| V-06 | PLANNED | - | M7-1 | Alpha-Blocker |
| V-07 | PARTIAL | ja | Marker-/Canvas-/Recorder-Crop Projektionen | M7-1/M7-7 |
| V-08 | PLANNED | - | Vision Catalog markiert ML Kit geplant | Alpha-Ziel fuer Kern-OCR |
| V-09 | PLANNED | - | Vision Catalog markiert OpenCV geplant | optional 1.0 |
| V-10 | PARTIAL | teil | Vision Contracts dependency; YOLO/LiteRT Katalog; Provider separat | optional 1.0 |
| V-11 | PARTIAL | teil | `WorldviewEntityUnifier`, Ambiguity-Projektionen | M7-8 |
| V-12 | PARTIAL | teil | Recorder/Worldview Observations und Zeitmetadaten | M7-8 |
| T-01 | IMPLEMENTED | ja | `RecordingSessionCoordinator`, Room v3 | M7-0 Gate |
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
| I-12 | PLANNED | - | M5-8/M9-0 | Alpha/Beta Grenze |
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
| Q-03 | IMPLEMENTED | ja | M5-0 bis M5-6 Tests | laufendes Gate |
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

## Fehlende oder unzureichende Nachweise

- Kein vollstaendiger M6-Geraete- und UX-Abnahmelauf fuer alle drei Editoren und Panelgesten.
- Kein prozessweiter Headless-/Foreground-Service-Test mit Recovery und UI-Reattachment.
- Kein gemeinsamer Display-ID-Vertrag fuer Capture, A11y, Gesture und Remote-Sessions.
- Kein produktiver M5-8-Nachweis fuer externe Adapter in einer einheitlichen Provider-Registry.
- Keine abgeschlossene A11y-/OCR-Zielaufloesung fuer reale Kernworkflows.
- Kein durchgaengiger Record -> Review -> Proposal -> explizite Workflow-Publikation -> WetRun-Test.
- Kein abgeschlossenes Projektformat fuer Explorer, Scopes, Asset-Lifecycle und Portabilitaet.
- Keine vollstaendige Device-/Android-Versionen-, Langlauf-, Permission-, Privacy- und Security-Matrix.
- Kein signierter, reproduzierbarer Release-Candidate-Nachweis.
