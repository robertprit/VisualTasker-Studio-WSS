# VisualTasker Studio WSS Release 1.0 Plan

Stand: 2026-10-11

## Zweck Und Autoritaet

Dieses Dokument ist die verbindliche Planungsansicht fuer VisualTasker Studio WSS 1.0. Es
konsolidiert die Architektur-Meilensteine, vorhandenen Abschlussberichte und historischen
Feature-Workstreams, ohne deren IDs oder Entscheidungen umzuschreiben.

Statuswerte:

- `DONE`: vertraglich implementiert und mit dem zugehoerigen Gate abgenommen.
- `ACTIVE`: aktueller Master-Meilenstein; einzelne Slices koennen bereits `DONE` sein.
- `PLANNED`: fachlich vorgesehen, aber noch nicht als Implementierung abgenommen.
- `DEFERRED`: bewusst aus dem aktuellen Release-Pfad verschoben.
- `GAP`: bekannte, explizite Luecke innerhalb eines ansonsten gueltigen Vertrags.
- `BLOCKED`: ein erforderlicher Slice kann wegen einer konkreten Voraussetzung nicht beginnen.

Die vorhandenen Feature-Meilensteine in `docs/ROADMAP.md` bleiben historische Workstreams. Bei
Namensgleichheit besitzt die Statusmatrix dieses Dokuments Vorrang fuer die Release-Reihenfolge.

## Belegter Ausgangsstand

| Master-Meilenstein | Status | Nachweis / Grenze |
| --- | --- | --- |
| M1 EMScript- und Strukturgrundlagen | DONE | Kanonische Sprach-, Block-, Node- und Identitaetsgrundlagen. |
| M2 Canonical Workflow Core | DONE | WorkflowGraph/WorkspaceDocument als einzige semantische Wahrheit. |
| M3 Workspace Operations und Source Apply | DONE | `M3_CLOSURE_AUDIT.md`; atomare Operationen, Properties, Source-Anker und Validator. |
| M4 Three-Editor Conformance | DONE | `M4_CLOSURE_AUDIT.md`; M4-0 bis M4-3 ohne Blocker geschlossen. |
| M5 Runtime und Capability Provider | ACTIVE | M5-0 bis M5-8 und M5-H `DONE`; Runtime-Closure fehlt. |
| M6 Editor Productization und Workspace UX | ACTIVE | M6-W Vertrag/Hosts implementiert; Geraete-UX und weitere Editor-Produktgates offen. |
| M7 Perception, Recorder und Inspection | ACTIVE | M7-0 Capture-/Observation-Vertrag implementiert; OCR/OpenCV-Live-Provider und Closure sind offen. |
| M8 Resources, Data Hub und Workspace Explorer | PLANNED | Datastore-/Asset-Vorarbeiten vorhanden; gemeinsame Scope- und Lifecycle-Abnahme fehlt. |
| M9 Integrationen, VT2VT und Erweiterbarkeit | PLANNED | Teilimplementierungen vorhanden; Release-Scope und Sicherheitsgate fehlen. |
| M10 Release Hardening | PLANNED | Noch kein Feature-Freeze und kein vollstaendiges Release-Gate. |

M4 besitzt zwei akzeptierte Produktgrenzen: History ist prozesslokal und beliebige, nicht
unterstuetzte EMScript-Fragmente werden nicht opaque roundgetrippt. Beide sind `GAP`, aber keine
Blocker fuer den aktuell unterstuetzten Vertrag, solange ungueltige Publikation sicher abgewiesen
wird.

## M5 Runtime Und Capability Provider

Gesamtstatus: `ACTIVE`

| Slice | Status | Ergebnis / Ziel |
| --- | --- | --- |
| M5-0 Execution Contract | DONE | Planner, Runner, Lifecycle, Cancellation und Trace. |
| M5-1 Android Runtime Bridge | DONE | Produktiver Start/Stop und nativer `wait`-Provider. |
| M5-2 Native Click | DONE | `clickPoint` mit realem Geraetenachweis. |
| M5-3 Native Gesture | DONE | Swipe, lineare Pfade und deterministische Konkurrenzbehandlung. |
| M5-4 Typed PointerPath | DONE | Line, quadratische/kubische Bezier-Segmente und Timing. |
| M5-5 Typed Path Bindings | DONE | Variablengebundene Pfadwerte und Drei-Editor-Roundtrip. |
| M5-6 Native MultiTouch | DONE | Synchronisierte Pointer, Pinch-Nachweis und gemeinsame Cancellation. |
| M5-7 Semantic Accessibility | DONE | `clickText`, typisierte Targets, deterministische Aufloesung, native Aktion und Target-Trace; dokumentierte Sprach-/Display-GAPs. |
| M5-8 Provider Resolution | DONE | Registry-Policy, Verfuegbarkeit, Berechtigungen, Fehlercodes und explizite Fallbacks. |
| M5-H Headless Execution | DONE | Ein prozessweiter Runtime Host, Foreground-/Background-Vertrag, Recovery und UI-Reattachment. |
| M5-9 Runtime Control Flow | PLANNED | Funktionen, Calls, Return und strukturierte Fehler nach Sprach-/Editor-Freigabe. |
| M5-10 Runtime Closure | PLANNED | Vollstaendige Matrix, Recovery, Retention, Langlauf und Closure-Audit. |

M5-Pebbles bleiben `PLANNED`: Cancellation waehrend langer Delays, Pointer-/Callback-Metriken,
vereinheitlichte Provider-Codes, sichtbarer Berechtigungsstatus, Multi-Root-Auswahl,
Runtime-Variablensnapshots, Trace-Retention und Lifecycle-Stop-Verhalten.

M5-Gate: Ein gueltiger kanonischer Workflow kann ueber genau einen produktiven Runner gestartet,
beobachtet, gestoppt, wiederaufgenommen oder diagnostisch beendet werden. Provider-Auswahl,
Fehlersemantik und Trace sind deterministisch dokumentiert.

### Testbare Integrationsgates M5-7 bis M7

| Gate | Abnahmekriterien | Verbindlicher Nachweis | Status / Blocker |
| --- | --- | --- | --- |
| M5-7 Accessibility Target Resolution | Eine typisierte Query adressiert genau eine stabile Workflow-Entity; Preorder-Aufloesung ist deterministisch; 0/1/n Treffer ergeben NOT_FOUND/FOUND/AMBIGUOUS; stale Nodes werden vor Aktion abgewiesen; Cancellation beendet Polling/Aktion; `ACTION_CLICK` trifft Element oder klickbaren Parent; keine implizite Koordinatenfallback- oder Workspace-Mutation | JVM-Resolver-/Provider-Tests, Text-Block-Flow-Roundtrip, reale Tests fuer unique, parent, changed observation und ambiguity, RailTrace mit Querytyp, Kandidatenzahl, Status, Aktion, Display und Observation | DONE fuer `clickText`; `clickElement`, variable Query und Nicht-Default-Display bleiben GAP |
| M5-8 Provider Policy | Bestehende Timing-, Gesture-, Accessibility-, Tasker-, Shizuku-, Termux- und CustomTabs-Hosts sind inventarisiert; jede Capability liefert Discovery, Availability, Permission und typed Result; Auswahl ist priorisiert und deterministisch; Fallback ist pro Capability explizit und im Trace sichtbar; Abwesenheit, Permission denial, Timeout, Cancellation und Providerverlust teilen stabile Fehlercodes | Registry-Unit-Tests fuer Prioritaet und Konflikt, Host-Adapter-Fixtures, kontrollierte Abwesenheits-/Permission-Tests, keine direkte zweite Dispatch-Engine | DONE; externe asynchrone Adapter bleiben M9 |
| M5-H Headless Runtime | Genau eine prozessweite `ExecutionSession` besitzt Runner/Job/Trace; Compose kann attach/detach ohne Laufabbruch; Doppelstart, Stop, Cancellation, Prozess-/Service-Recovery und UI-Reattachment sind deterministisch; Foreground-Service und Notification-Stop entsprechen Android 36; Session-ID, Workflow-ID, Status und Trace-Retention bleiben stabil | JVM State-Machine-Tests, Service-Lifecycle-/Process-recreation-Test, Hintergrundlauf, Reattach und Stop auf SM-S918B; visuelle RailTrace-Abnahme separat | DONE; kein Auto-Replay nach Prozessverlust |
| M6-W WindowSession | Vorhandene Panel-Persistenz, Z-Order, Drag, Resize und Snap werden wiederverwendet; Shell und `StudioOverlayService` teilen Session-, Display-, Fokus- und Touch-Routing-Vertrag; harte Bounds, Insets, Min/Max, Crash-Restore und Multi-Panel-Z-Order sind deterministisch; Window-Aktionen mutieren keinen Workflow | Pure-State-Tests, Persistenz-/Migrationstest, Rotation/Insets, Overlay attach/detach, reproduzierbarer Geraete-Gestenlauf | PARTIAL: Vertrag, Hosts und automatisierte Gates implementiert; reale Gesten-/Prozess-Recovery-Abnahme offen |
| M7 Perception/Recorder | Capture, Marker, Recorder, Scene Inspector und Runtime tragen denselben Display-/Frame-/Zeitkontext; bestehende Recording-Persistenz, Integrity, Review, Replay und Recovery bleiben erhalten; Record -> Candidate -> Review -> Junktor Proposal -> explizite WorkspaceTransaction -> Runtime ist nachvollziehbar; Ablehnung mutiert nichts; es entsteht keine zweite Recorder-Pipeline | Capture-/Koordinatenfixtures, persistenter Record-Neustart, Evidence-Provenienz, Publish/Reject-Integrationstest, zwei reale App-Workflows bis Trace | ACTIVE: M7-0 Capture-Kontext implementiert; OCR/OpenCV, Publishing und reale Capture-Abnahme offen |

M7-V VirtualDisplay ist kein Bestandteil dieser Gates. M9-D Remote Display bleibt bis zu einer
separaten Scope-Freigabe deferred.

## M6 Editor Productization Und Workspace UX

Gesamtstatus: `PLANNED`; vorhandene Funktionen gelten als Vorarbeit, nicht als pauschale Abnahme.

| Slice | Status | Ziel |
| --- | --- | --- |
| M6-0 | PLANNED | Bestandsaudit gegen den heutigen Compose-Code und die M4-Vertraege. |
| M6-W | PARTIAL | Gemeinsamer Window-/Overlay-Vertrag produktiv; reale Geraete-UX- und Recovery-Abnahme offen. |
| M6-1 | PLANNED | BlockEditor Docking, Reporter, Container, Drag/Snap und Typvalidierung abnehmen. |
| M6-2 | PLANNED | FlowEditor AutoArrange, Routing, Branches und Kantenfreiheit abnehmen. |
| M6-3 | PLANNED | TextEditor Diagnostik, Source Mapping, Navigation und Apply-Vorschau abnehmen. |
| M6-4 | PLANNED | Fokus, Selektion, Navigation und History ueber alle Editoren abnehmen. |
| M6-5 | PLANNED | Typisierter Inspector fuer Parameter und Runtime-Bindings. |
| M6-6 | PLANNED | Material-3-Geometrie und Designer-Vertraege konsolidieren. |
| M6-7 | PLANNED | Performance-, Accessibility- und reale Geraeteabnahme. |

M6 darf M4 nicht neu erfinden. Semantische Aenderungen bleiben WorkspaceOperations; Zoom, Pan,
Layout, Selection und Designer-Vorschau duerfen keine unnoetige kanonische Revision erzeugen.

## M7 Perception, Recorder Und Inspection

Gesamtstatus: `ACTIVE`; Recorder, RailTrace, WatchDog, Marker und Scene Inspector liefern
substantielle Grundlagen, ersetzen aber kein gemeinsames Perception-Closure-Gate.

| Slice | Status | Ziel |
| --- | --- | --- |
| M7-0 | PARTIAL | Gemeinsamer CaptureFrameProvider, Lifecycle, Display-/Koordinatenvertrag und Recorder-/Inspector-Provenienz implementiert; reale Capture-Abnahme und Live-OCR/OCV offen. |
| M7-1 | DONE | CaptureFrameProvider mit Koordinaten-, Rotation-, Crop-, Sharing- und Failure-Vertrag in M7-0 geschlossen. |
| M7-V | PLANNED | Optionale VirtualDisplay-Machbarkeit mit eigener Testoberflaeche, Display-Kontext und sicherem Cleanup. |
| M7-2 | PLANNED | Accessibility Tree, stabile Element-Resolution und Evidence-Herkunft. |
| M7-3 | PLANNED | ML Kit OCR, ROI und Text-Matching. |
| M7-4 | PLANNED | OpenCV Template Matching und Bildsuche. |
| M7-5 | PLANNED | YOLO/Object Detection als optionaler Provider; kein Release-Gate ohne Scope-Entscheidung. |
| M7-6 | PLANNED | Raw Record zu Candidates, Review und kanonischem Workflow. |
| M7-7 | PLANNED | Scene Inspector, Floating Marker und visuelle Zielkorrektur. |
| M7-8 | PLANNED | Korrelation von Observation, Action, Result und frischer Observation. |
| M7-9 | PLANNED | Perception-Closure mit realen, reproduzierbaren App-Workflows. |

M5-7 und M7-2 sind getrennt: M5-7 konsumiert eine deterministische semantische Zielaufloesung fuer
die Runtime. M7-2 besitzt den breiteren Baum-, Scene-, Evidence-, Freshness- und Identitaetsvertrag.
A11y-, OCR-, OpenCV- und YOLO-Beobachtungen bleiben unterscheidbare Quellen; Fusion darf Herkunft
und Confidence nicht vernichten.

## M8 Resources, Data Hub Und Workspace Explorer

Gesamtstatus: `PLANNED`

| Slice | Status | Ziel |
| --- | --- | --- |
| M8-0 | PLANNED | Ressourcen-, Scope-, Eigentums- und Persistenzvertraege. |
| M8-1 | PLANNED | Workspace Explorer fuer Workflows, Scripts, Funktionen und Ressourcen. |
| M8-2 | PLANNED | Mehrere Workflow-Roots und referenzierte Subworkflows. |
| M8-3 | PLANNED | Funktionsbibliothek mit stabilen IDs und Call-Referenzen. |
| M8-4 | PLANNED | Local-, Script-, Project-, App-Shared- und External-Scopes. |
| M8-5 | PLANNED | Persistente Daten ueber klar getrennte lokale und externe Provider. |
| M8-6 | PLANNED | Asset Registry fuer Images, Templates, Regions und Shapes. |
| M8-7 | PLANNED | Typisiertes Drag-and-Drop zwischen Explorer und Editoren. |
| M8-8 | PLANNED | Where-Used, Rename, Delete Guards und Resource Lifecycle. |
| M8-9 | PLANNED | Import/Export, Migration und Projektportabilitaet. |

Vorhandene Datastore-, VisualAsset-, DnD- und Worldview-Vorarbeiten werden in M8-0 inventarisiert
und wiederverwendet. Es wird kein paralleler Resource Manager oder zweites Projektmodell gebaut.

## M9 Integrationen, VT2VT Und Erweiterbarkeit

Gesamtstatus: `PLANNED`

| Slice | Status | Ziel |
| --- | --- | --- |
| M9-0 | PLANNED | Discovery auf Basis der M5-8-Registry, nicht als zweite Registry. |
| M9-1 | PLANNED | Tasker Profiles, Actions, Events, Variablen und Import/Export. |
| M9-2 | PLANNED | Optionale Shizuku-Capabilities mit expliziter Berechtigung. |
| M9-3 | PLANNED | Termux/Shell und externe Prozesse mit begrenztem Vertrag. |
| M9-4 | PLANNED | Browser Bridge, Custom Tabs und Navigation Events. |
| M9-5 | PLANNED | VT2VT Pairing, Transport und stabile Geraeteidentitaeten. |
| M9-D | DEFERRED | Display-Session-Registry fuer lokale, virtuelle und entfernte Flaechen nach M7-V und VT2VT-Scope. |
| M9-6 | PLANNED | Versionierte WorkspaceOperation-Synchronisierung und Konflikte. |
| M9-7 | PLANNED | Remote Capability Dispatch und typisierte Resultate. |
| M9-8 | PLANNED | Erweiterungsvertraege fuer eigene Compose-/Provider-Module. |
| M9-9 | PLANNED | Integrations-, Sicherheits- und Konfliktabnahme. |

M5-8 besitzt die allgemeine Auswahl- und Fehlerpolicy. M9 implementiert und prueft konkrete externe
Provider. Workspace-Synchronisierung, Display-Mirroring und Remote-Ausfuehrung bleiben drei
verschiedene Vertraege.

## M10 Release Hardening

Gesamtstatus: `PLANNED`

| Slice | Status | Ziel |
| --- | --- | --- |
| M10-0 | PLANNED | Vollstaendiges Feature-, Architektur- und Scope-Audit. |
| M10-1 | PLANNED | Performance- und Speicherprofiling. |
| M10-2 | PLANNED | Crash Recovery, Lifecycle und Langlaufstabilitaet. |
| M10-3 | PLANNED | Datenmigration, Backup und Restore. |
| M10-4 | PLANNED | Permission-, Privacy- und Security-Audit. |
| M10-5 | PLANNED | Android-, Geraete- und Display-Kompatibilitaet. |
| M10-6 | PLANNED | Produktdokumentation, EMScript-Referenz und Beispiele. |
| M10-7 | PLANNED | Bedienbarkeit, Onboarding und Fehlermeldungen. |
| M10-8 | PLANNED | Regression Suite und Release-Automation. |
| M10-9 | PLANNED | Release-Candidate-Qualifikation und Stable-Freigabe. |

## Abhaengigkeiten Und Kritischer Pfad

1. M5-7 baut auf M5-0 bis M5-6 und dem vorhandenen AccessibilityService auf und ist abgeschlossen.
2. M5-8 konsolidiert Provider-Aufloesung, bevor weitere externe Adapter releaseverbindlich werden.
3. M5-H uebernimmt den bestehenden Runner nach M5-8 in genau einen UI-unabhaengigen Runtime Host;
   die Compose-Shell wird Beobachter und Steuerflaeche, nicht Runtime-Eigentuemer.
4. M6-W konsolidiert nach M5-H die vorhandene Compose-Panel- und native Overlay-Mechanik in einen
   gemeinsamen WindowSession-/Display-Vertrag.
5. M7-1/M7-2 verwenden diesen Display-Vertrag fuer Capture, Scene und Evidence; M7-6 schliesst
   danach den Recorder-to-Workflow-Publishing-Pfad.
6. M8-0 definiert Ressourcen und Scopes, bevor Explorer und Portabilitaet geschlossen werden.
7. M10 hardenet erst den festgelegten 1.0-Scope und fuehrt zu Alpha, Beta, RC und Stable.
8. Der strukturierte Fehleranteil von M5-9 setzt einen freigegebenen M4-4-Strukturvertrag voraus,
   ist aber kein 1.0-Core-Blocker.
9. M5-10 schliesst nach M5-8, M5-H und den releaseverbindlichen M5-Pebbles; M5-9 ist nur nach
   separater Scope-Freigabe Voraussetzung.
10. M6-0 auditiert parallel die vorhandene Editor-Produktreife; M6-W konsolidiert danach vorhandene
   Compose-Panel- und native Overlay-Mechanik ohne Workflow-Mutation.
11. M7-0 trennt vorhandene Recorder-/Inspector-Bausteine von fehlender Perception-Semantik. M7-V
   setzt mindestens den Display-Kontext aus M6-W und den Capture-Vertrag aus M7-1 voraus.
12. M9 nutzt M5-8 und M8-Vertraege; M9-D benoetigt zusaetzlich M7-V und M9-5. Konkrete
   Integrationen werden nicht in den Runtime-Kern gezogen.

Verbindlicher kritischer Pfad:

`M5-H DONE -> M6-W -> M7 Capture/Display -> M7 Recorder-to-Workflow -> M8 Portability -> M10 Hardening -> Alpha -> Beta -> RC -> Stable`

Parallel erlaubt sind M6-Editor-Haertung, Dokumentationsabschluss und Regressionserweiterung. Sie
duerfen keine noch offenen Runtime-, WindowSession- oder Capture-Vertraege vorwegnehmen.

Der aktuelle kleinste sinnvolle Slice ist die reale M6-W-Geraeteabnahme fuer Panel-, Overlay- und
Recovery-Gesten. Danach folgt M6-0 als Produktreife-Audit der vorhandenen Editoren. Vertrag,
Registry und Hostadapter aus M6-W duplizieren weder Runtime noch Workflow-Semantik.

## Release-Gates

### Alpha

- M5 Runtime-Closure erreicht.
- M5-H Headless-Ausfuehrung mit sichtbarer Benachrichtigung, Stop, Recovery und UI-Reattachment.
- M6-Editoren fuer den unterstuetzten Umfang produktiv und semantisch deckungsgleich.
- M6-W Panel-/Window-Sessions wiederherstellbar, ohne Workflow- oder Runtime-Mutation.
- A11y- und OCR-Zielaufloesung fuer reale Kernworkflows.
- Recorder zu Review zu Workflow zu Runtime zu Trace nachweisbar.
- Projekt speichern, laden und nach Crash/Neustart ohne kritischen Datenverlust wiederherstellen.

### Beta

- M7-Perception-Closure fuer den freigegebenen Umfang.
- M8-Projektorganisation, Ressourcen und Portabilitaet.
- Fuer 1.0 ausgewaehlte M9-Integrationen mit Berechtigungs- und Fehlervertraegen.
- M7-V nur dann, wenn die optionale VirtualDisplay-Funktion ausdruecklich in den 1.0-Scope
  aufgenommen wurde.
- Mehrgeraete-, Reconnect-, Langlauf- und grosse-Workflow-Tests.

### Release Candidate

- Feature-Freeze und M10-0 bis M10-8 abgenommen.
- Keine offenen P0-/P1-Fehler oder undokumentierten Datenmigrationsrisiken.
- Reproduzierbarer Release-Build, vollstaendige Regression, Backup/Restore und Dokumentation.

### Stable 1.0

- M10-9 bestanden.
- Kanonische Datenintegritaet, Drei-Editor-Roundtrip, Runtime-Cancellation und Provider-Fehlersemantik
  auf den dokumentierten Zielgeraeten nachgewiesen.
- Signierter Build, Changelog, Migrationshinweise und bekannte Grenzen veroeffentlichungsbereit.

## Konflikte Und Produktentscheidungen

| Thema | Status | Entscheidung / offene Grenze |
| --- | --- | --- |
| M5-6 in `roadmap-1.md` als offen | DONE | Durch `M5_6_NATIVE_MULTITOUCH.md`, Tests und physischen SM-S918B-Nachweis ueberholt. |
| Historische und neue M1-M10-IDs | DONE | Historische IDs bleiben; dieses Dokument ist der Master-Namensraum fuer die Release-Reihenfolge. |
| M4-4 gegen M5-9 | DEFERRED | Strukturvertrag separat freigeben; Runtime darf ihn nicht vorwegnehmen. |
| Headless Runtime | DONE | M5-H hostet den bestehenden Runner prozessweit, ohne ihn zu duplizieren. |
| Window/Floating Workspace | PARTIAL | M6-W-Vertrag und Hostadapter sind implementiert; reale Geraete- und Recovery-Abnahme bleibt offen. |
| VirtualDisplay Foundation | PLANNED | M7-V ist optional fuer 1.0 und erfordert einen eigenen Machbarkeits-/Permission-Gate. |
| MultiDisplay/Remote Surface | DEFERRED | M9-D folgt erst nach M7-V und freigegebenem VT2VT-Scope. |
| Prozessuebergreifende Undo-History | GAP | Fuer M4 nicht erforderlich; vor Stable explizit als Produktanforderung annehmen oder ablehnen. |
| Opaque unbekanntes EMScript | GAP | Weiter fail-closed; lossless Opaque-Support nur nach eigener Freigabe. |
| YOLO in 1.0 | PLANNED | Optionaler Provider; Aufnahme in das 1.0-Gate bleibt eine offene Scope-Entscheidung. |
| Konkreter M9-Umfang | PLANNED | Tasker, Shizuku, Termux, Browser und VT2VT einzeln fuer Beta/1.0 priorisieren. |
| WorldviewGraph, RAG, lokale KI/SLM | DEFERRED | Architekturkompatibel halten, aber nach Stable 1.0 priorisieren. |
| ShapeMaker, Morf, ChartGraph, Tank Fortress, Paper Trading | DEFERRED | Eigene Produkt-/Plugin-Roadmaps; keine automatischen 1.0-Blocker. |
| Autonomous Scripting | DEFERRED | Nur mit expliziter Human-in-the-loop-, Sicherheits- und Mutationspolicy. |

## Erweiterungen Nach 1.0

Die folgenden Konzepte bleiben architektonisch beruecksichtigt, sind aber ohne neue Freigabe keine
Stable-1.0-Blocker:

- `DEFERRED` WorldviewGraph als dynamisches Modell beobachteter Realitaet.
- `DEFERRED` Perugger als Ambiguitaets-, Interpretations- und Debug-Workbench.
- `DEFERRED` lokale KI/SLM fuer vorgeschlagene, explizit freizugebende Workflow-Aenderungen.
- `DEFERRED` RAG und ExperienceRecords als durchsuchbare Ausfuehrungs- und Lernerfahrung.
- `DEFERRED` Patchbay/Patches fuer typisierte Zustaende, Datenquellen und Signalverbindungen.
- `DEFERRED` ShapeMaker als erweiterte VisualAsset-, Overlay- und Indikator-Werkzeugkette.
- `DEFERRED` Morf, ChartGraph, Tank Fortress und Paper Trading als eigenstaendige Produkte/Plugins.
- `DEFERRED` Autonomous Scripting nur mit Human-in-the-loop-, Berechtigungs- und Mutationsgrenzen.

## Historische Workstream-Zuordnung

| Historischer Workstream in `docs/ROADMAP.md` | Master-Zuordnung |
| --- | --- |
| Feature M1, M2, M2.1 | M6 Editor Productization |
| Feature M3 | M1 bis M5 Sprach-, Conformance- und Runtime-Pfad |
| Feature M4 | M7 Recorder/Inspection und M9 optionale Overlay-Integration |
| Feature M5, M5.1 | M7 Capture/Marker/Perception |
| Feature M6, M6.1 | M8 Resources/Assets; Worldview/RAG teilweise nach 1.0 `DEFERRED` |
| Feature M7, M7.1 | M7 Recorder/RailTrace |
| Feature M8 | M8/M9/M10 je nach Funktion |
| Feature M9, M9.1 | M9 Integrationen |
| Feature M10 | M10 Release Hardening |
| C1 bis C7 | Querschnittsbacklog; vor Umsetzung einem Master-Slice zuordnen |

## Naechste Zehn Ausfuehrbare Pebbles

| Nr. | Pebble | Abhaengigkeit | Abnahme |
| --- | --- | --- | --- |
| 1 | M5-8 Host-Inventar fuer Registry, Accessibility, Tasker, Shizuku, Termux und Custom Tabs | M5-7 DONE | Jede Capability hat genau einen dokumentierten produktiven Einstieg oder eine GAP-Markierung |
| 2 | M5-8 gemeinsamer Availability-/Permission-Snapshot | 1 | Deterministische typed States fuer available, unavailable, denied und stale |
| 3 | M5-8 Provider-Prioritaet und Konfliktentscheidung | 2 | Reihenfolge unabhaengig von Registrierung; Konflikt wird diagnostiziert |
| 4 | M5-8 stabile Result-/Fehlercodes und explizite Fallback-Metadaten | 3 | Abwesenheit, denial, timeout, cancellation und provider loss im Trace testbar |
| 5 | M5-8 vertikales Gate mit vorhandenen nativen Providern; externe Hosts nur inventarisieren | 4 | Kein Direktdispatch neben der Registry, bestehende M5-7-Tests bleiben gruen |
| 6 | M5-H `ExecutionSession`-State-Machine aus bestehender Bridge extrahieren | 5 | Genau eine Session, Doppelstart/Stop/Cancellation als JVM-Test |
| 7 | M5-H UI Attach/Detach und Status-/Trace-Reattachment | 6 | Lauf ueberlebt Shell-Detach, UI zeigt denselben Sessionzustand |
| 8 | M5-H Android-36 Foreground-Service-, Notification-Stop- und Recovery-Gate | 7 | Hintergrundlauf, Prozessrecovery und Stop auf Zielgeraet |
| 9 | M6-W vorhandene Panel-/Overlay-Zustaende auf einen WindowSession-Vertrag abbilden | 8 | Keine zweite Persistenz; Shell/Overlay teilen ID, Display, Bounds und Z-Order |
| 10 | M7-0/M7-1 Capture-/Recorder-Audit und kleinster gemeinsamer Frame-/Display-Kontext | 9 | Eine Aufnahme, Marker-ROI und Scene Evidence referenzieren denselben Frame |

Die Pebbles 1 bis 5 bilden M5-8, 6 bis 8 M5-H, 9 M6-W und 10 den Einstieg in M7. Vorhandene
Vorarbeiten werden inventarisiert und nicht neu gebaut.
