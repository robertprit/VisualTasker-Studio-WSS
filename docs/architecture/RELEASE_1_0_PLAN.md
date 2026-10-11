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
| M5 Runtime und Capability Provider | ACTIVE | M5-0 bis M5-6 `DONE`; semantische Ziele, Headless Host und Runtime-Closure fehlen. |
| M6 Editor Productization und Workspace UX | PLANNED | Viele Funktionen vorhanden; ein gemeinsamer Window-Session-Vertrag und M6-Gesamtgate fehlen. |
| M7 Perception, Recorder und Inspection | PLANNED | Recorder/RailTrace/Scene-Inspector-Vorarbeiten vorhanden; Perception und optionale VirtualDisplay-Foundation sind nicht geschlossen. |
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
| M5-8 Provider Resolution | PLANNED | Registry-Policy, Verfuegbarkeit, Berechtigungen, Fehlercodes und explizite Fallbacks. |
| M5-H Headless Execution | PLANNED | Ein prozessweiter Runtime Host, Foreground-/Background-Vertrag, Recovery und UI-Reattachment. |
| M5-9 Runtime Control Flow | PLANNED | Funktionen, Calls, Return und strukturierte Fehler nach Sprach-/Editor-Freigabe. |
| M5-10 Runtime Closure | PLANNED | Vollstaendige Matrix, Recovery, Retention, Langlauf und Closure-Audit. |

M5-Pebbles bleiben `PLANNED`: Cancellation waehrend langer Delays, Pointer-/Callback-Metriken,
vereinheitlichte Provider-Codes, sichtbarer Berechtigungsstatus, Multi-Root-Auswahl,
Runtime-Variablensnapshots, Trace-Retention und Lifecycle-Stop-Verhalten.

M5-Gate: Ein gueltiger kanonischer Workflow kann ueber genau einen produktiven Runner gestartet,
beobachtet, gestoppt, wiederaufgenommen oder diagnostisch beendet werden. Provider-Auswahl,
Fehlersemantik und Trace sind deterministisch dokumentiert.

## M6 Editor Productization Und Workspace UX

Gesamtstatus: `PLANNED`; vorhandene Funktionen gelten als Vorarbeit, nicht als pauschale Abnahme.

| Slice | Status | Ziel |
| --- | --- | --- |
| M6-0 | PLANNED | Bestandsaudit gegen den heutigen Compose-Code und die M4-Vertraege. |
| M6-W | PLANNED | Gemeinsamer Window-/Overlay-Host fuer Sessions, Z-Order, Fokus, Drag/Resize, Snap/Dock und Layoutprofile. |
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

Gesamtstatus: `PLANNED`; Recorder, RailTrace, WatchDog, Marker und Scene Inspector liefern
substantielle Grundlagen, ersetzen aber kein gemeinsames Perception-Closure-Gate.

| Slice | Status | Ziel |
| --- | --- | --- |
| M7-0 | PLANNED | Ist-Audit von Capture, Recording, Evidence, Scene Inspector und Perception. |
| M7-1 | PLANNED | Gemeinsamer CaptureFrameProvider mit Koordinaten-, Rotation- und Freshness-Vertrag. |
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

1. M5-7 baut auf M5-0 bis M5-6 und dem vorhandenen AccessibilityService auf.
2. M5-8 konsolidiert Provider-Aufloesung, bevor weitere externe Adapter releaseverbindlich werden.
3. M5-H uebernimmt den bestehenden Runner nach M5-8 in genau einen UI-unabhaengigen Runtime Host;
   die Compose-Shell wird Beobachter und Steuerflaeche, nicht Runtime-Eigentuemer.
4. Der strukturierte Fehleranteil von M5-9 setzt einen freigegebenen M4-4-Strukturvertrag voraus.
5. M5-10 schliesst erst nach M5-7, M5-8, M5-H, M5-9 und den M5-Pebbles.
6. M6-0 auditiert die vorhandene Editor-Produktreife; M6-W konsolidiert danach vorhandene
   Compose-Panel- und native Overlay-Mechanik ohne Workflow-Mutation.
7. M7-0 trennt vorhandene Recorder-/Inspector-Bausteine von fehlender Perception-Semantik. M7-V
   setzt mindestens den Display-Kontext aus M6-W und den Capture-Vertrag aus M7-1 voraus.
8. M8-0 definiert Ressourcen und Scopes, bevor Explorer, Funktionen und projektweite DnD-Pfade
   erweitert werden.
9. M9 nutzt M5-8 und M8-Vertraege; M9-D benoetigt zusaetzlich M7-V und M9-5. Konkrete
   Integrationen werden nicht in den Runtime-Kern gezogen.
10. M10 beginnt nach festgelegtem 1.0-Scope und Feature-Freeze.

Der aktuelle kleinste sinnvolle Slice ist M5-8: die bereits produktiven Timing-, Gesture- und
Accessibility-Provider erhalten eine gemeinsame Resolver-, Availability-, Berechtigungs- und
Fehlercode-Policy. OCR, Vision-Fusion und externe Provider gehoeren nicht in diesen Slice.

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
| Headless Runtime | PLANNED | M5-H ist 1.0 Core; bestehender Runner wird gehostet, nicht dupliziert. |
| Window/Floating Workspace | PLANNED | M6-W ist 1.0 Core und konsolidiert vorhandene Panel-/Overlay-Bausteine. |
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

## Naechste Zehn Freizugebende Slices

1. `DONE` M5-7 A11y-Vertragsinventur und vertikaler semantischer Zielpfad.
2. `DONE` M5-7 Abschluss mit deterministischer Mehrdeutigkeit, Trace und Geraetegate.
3. `PLANNED` M5-8 Provider-Resolution, Availability und Fehlercode-Vertrag.
4. `PLANNED` M5-H Headless Host, Foreground-Vertrag und UI-Reattachment.
5. `PLANNED` M4-4 Entscheidungs- und Conformance-Slice fuer strukturierte Fehler.
6. `PLANNED` M5-9 Funktionen, Calls, Return und freigegebene Fehlerstrukturen.
7. `PLANNED` M5-10 Runtime-Closure-Audit und Langlaufmatrix.
8. `PLANNED` M6-0 Bestandsaudit, danach M6-W als kleinster Window-Session-Slice.
9. `PLANNED` M7-0 Capture-/Recorder-/Perception-Audit; M7-V bleibt optional.
10. `PLANNED` M8-0 Ressourcen-/Scope-Vertrag und M10-0 Release-Scope-Vorbereitung.

Keiner dieser Slices gilt durch diese Planung als implementiert. Vorhandene Vorarbeiten werden im
jeweiligen Audit inventarisiert und nicht neu gebaut.
