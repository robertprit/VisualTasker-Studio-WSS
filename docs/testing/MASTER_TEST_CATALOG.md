# VisualTasker Studio WSS – Master Test Catalog

Stand: 2026-09-14

Zweck: zentrale, fortlaufende Checkliste fuer manuelle Systemtests. Die Detailfaelle stehen in den verlinkten Dateien. Dieser Katalog bildet keinen Implementierungsstatus ab, sondern den **Teststatus**.

## Legende

- `[ ]` NOT_TESTED
- `[x]` PASS
- `[!]` FAIL
- `[-]` BLOCKED
- `[~]` NOT_IMPLEMENTED

## P0 – Core Smoke

Datei: `00-CORE-SMOKE.md`

- [ ] SMK-001 Build + Unit Tests
- [ ] SMK-002 Install + Launch
- [ ] SMK-003 Workspace Basisinteraktion
- [ ] SMK-004 Workspace Restart/Restore
- [ ] SMK-005 Minimaler Block-Workflow
- [ ] SMK-006 Block -> EMScript Projektion
- [ ] SMK-007 EMScript Apply -> Workflow
- [ ] SMK-008 Workflow -> Flow Projektion
- [ ] SMK-009 DryRun Step-Fokus
- [ ] SMK-010 gemeinsamer Fokus Text/Block/Flow/Rail

## Workspace Shell

Datei: `01-WORKSPACE.md`

- [ ] WS-001 Panel oeffnen/schliessen
- [ ] WS-002 Panel verschieben
- [ ] WS-003 Panel resize
- [ ] WS-004 minimieren/wiederherstellen
- [ ] WS-005 Fokus/Z-Order
- [ ] WS-006 Grid/Snap
- [ ] WS-007 Docking/Nachbarpanel
- [ ] WS-008 harte Workspace-Grenzen
- [ ] WS-009 Session Restore
- [ ] WS-010 Theme-Wechsel
- [ ] WS-011 Siderail/Panel-Navigation
- [ ] WS-012 mehrfach geoeffnete Panels/Instanzen

## EMScript + BlockEditor + FlowEditor

Datei: `02-EDITORS.md`

- [ ] EMS-001 gueltiger Draft
- [ ] EMS-002 Parserdiagnose bei Fehler
- [ ] EMS-003 Apply Preview
- [ ] EMS-004 atomarer Apply
- [ ] EMS-005 keine Mutation bei invalidem Draft
- [ ] EMS-006 Source-/Runtime-Markierung
- [ ] BE-001 Block erzeugen
- [ ] BE-002 Sequence-Snap
- [ ] BE-003 Value-/Reporter-Snap
- [ ] BE-004 Typinkompatibilitaet ablehnen
- [ ] BE-005 Parent mit Subtree ziehen
- [ ] BE-006 Detach/Subtree Peel
- [ ] BE-007 IF/ELSEIF/ELSE Struktur
- [ ] BE-008 Variablen/Literale/Operatoren
- [ ] BE-009 Undo/Redo
- [ ] BE-010 Save/Reload View State
- [ ] FE-001 Node-Selektion
- [ ] FE-002 Node Drag/Pan/Zoom
- [ ] FE-003 Dock/Undock/Detach
- [ ] FE-004 Ports/Edges
- [ ] FE-005 Auto-Arrange
- [ ] FE-006 Auto-Pan am Viewportrand
- [ ] FE-007 Facets/Collapse
- [ ] FE-008 Minimap/Fokus
- [ ] FE-009 Runtime Layer

## Editor- und Projection-Sync

Datei: `03-EDITOR-SYNC.md`

- [ ] SYNC-001 Block -> Text
- [ ] SYNC-002 Text Apply -> Block
- [ ] SYNC-003 Workflow -> Flow
- [ ] SYNC-004 Block Selection -> Flow
- [ ] SYNC-005 Flow Selection -> Block
- [ ] SYNC-006 Runtime Fokus -> Text
- [ ] SYNC-007 Runtime Fokus -> Block
- [ ] SYNC-008 Runtime Fokus -> Flow
- [ ] SYNC-009 Runtime Fokus -> RailTrace
- [ ] SYNC-010 Save/Reload behaelt Semantik
- [ ] SYNC-011 Invalid Draft veraendert keine anderen Projektionen
- [ ] SYNC-012 Undo/Redo erzeugt konsistente Projektionen

## Runtime, RailTrace, Recording

Datei: `04-RUNTIME-RAIL-RECORDING.md`

- [ ] RUN-001 DryRun Start/Stop
- [ ] RUN-002 Step vor/zurueck
- [ ] RUN-003 BasicRun vorhandener Grundbefehl
- [ ] RUN-004 Capability Gate bei fehlendem Adapter
- [ ] RUN-005 Runtime-Fehler sichtbar und nicht verschluckt
- [ ] RAIL-001 Program Mode
- [ ] RAIL-002 Step Mode
- [ ] RAIL-003 Live Mode
- [ ] RAIL-004 Replay Mode
- [ ] RAIL-005 Curate Mode
- [ ] RAIL-006 Surface Records/Run/WatchDog
- [ ] RAIL-007 Session-Auswahl
- [ ] RAIL-008 Gesamtzeit/Progress
- [ ] RAIL-009 Replay Speed
- [ ] RAIL-010 Auswahl-Sync mit Marker/Editoren
- [ ] RAIL-011 Recording Events an Activity/Scene gebunden
- [ ] RAIL-012 persistierte Session wieder laden

## Worldview, Ressourcen, Perception

Datei: `05-WORLDVIEW-PERCEPTION.md`

- [ ] WV-001 Observation bleibt von Entity getrennt
- [ ] WV-002 Resource bleibt von Observation getrennt
- [ ] WV-003 WorldEntity/Scene Projektion
- [ ] WV-004 Ambiguity sichtbar
- [ ] WV-005 Marker Resource erfassen
- [ ] WV-006 Screenshot Resource erfassen
- [ ] WV-007 Template Resource erfassen
- [ ] WV-008 Canvas/Datastore Projektion
- [ ] WV-009 Vision Input/Live-Crop
- [ ] WV-010 Provider-Provenance nachvollziehbar
- [ ] WV-011 keine stille Provider-Mutation einer Entity
- [ ] WV-012 Unknown/fehlende Evidenz bleibt explizit

## Persistenz + Integrationen

Datei: `06-PERSISTENCE-INTEGRATIONS.md`

- [ ] PERSIST-001 Workspace Session
- [ ] PERSIST-002 Block View
- [ ] PERSIST-003 Flow View
- [ ] PERSIST-004 RailTrace letzte Position
- [ ] PERSIST-005 Recording Session
- [ ] PERSIST-006 Ressourcen Save/Reload
- [ ] PERSIST-007 inkompatible/alte Daten sicher behandeln
- [ ] INT-001 VT2VT Loopback
- [ ] INT-002 VT2VT Codec Roundtrip sichtbar
- [ ] INT-003 VT2VT LAN Observer
- [ ] INT-004 Drag/Drop TextEditor Effect
- [ ] INT-005 Drag/Drop Editor Representation
- [ ] INT-006 Provider-/Plugin-Gate sichtbar

## End-to-End

Datei: `90-END-TO-END.md`

- [ ] E2E-001 Authoring -> DryRun -> RailTrace
- [ ] E2E-002 EMScript -> Blocks -> Flow -> Save/Reload
- [ ] E2E-003 Blocks -> EMScript -> Apply -> gleiche Semantik
- [ ] E2E-004 Recording -> Record -> RailTrace Replay
- [ ] E2E-005 Resource -> Worldview -> Inspector/Canvas
- [ ] E2E-006 Fehlerhafter Draft -> Diagnose -> keine Mutation
- [ ] E2E-007 Runtime Step -> vier Projektionen synchron
- [ ] E2E-008 App-Neustart mitten im normalen Arbeitszustand

## Testlauf-Zusammenfassung

```text
Datum:
Commit:
Build:
Geraet:

PASS:
FAIL:
BLOCKED:
NOT_IMPLEMENTED:
NOT_TESTED:

P0 Fehler:
P1 Fehler:

Freigabeempfehlung:
[ ] weiter testen
[ ] Core stabil genug fuer breiteren Funktionslauf
[ ] Regression beheben und Smoke erneut starten
```
