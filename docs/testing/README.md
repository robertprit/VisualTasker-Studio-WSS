# VisualTasker Studio WSS – Teststrategie

Stand: 2026-09-14

Diese Dokumentation ist die zentrale manuelle und systemweite Testbasis fuer VisualTasker Studio WSS. Sie ergaenzt die vorhandenen Unit-/Regressionstests und die Roadmap. Ziel ist nicht nur zu pruefen, ob einzelne Funktionen existieren, sondern ob die Architektur als zusammenhaengendes System funktioniert.

## Grundsatz

Die wichtigste Systeminvariante lautet:

`eine kanonische Workflow-Wahrheit -> mehrere konsistente Projektionen`

Dazu gehoeren insbesondere TextEditor/EMScript, BlockEditor, FlowEditor und RailTrace. Worldview, Runtime, Recording und Panels duerfen keine konkurrierende fachliche Wahrheit erzeugen.

Die Tests unterscheiden deshalb vier Ebenen:

- **T0 – technische Gesundheit:** Build, Unit Tests, Install, Launch, Restart, Crash-Safety.
- **T1 – Panel-/Featuretests:** ein Panel oder eine Funktion isoliert bedienen.
- **T2 – Integrations-/Synchronisationstests:** mehrere Projektionen und Services arbeiten auf demselben fachlichen Zustand.
- **T3 – End-to-End-Szenarien:** Authoring -> Validate -> DryRun/Run -> Observation/Record -> RailTrace/Replay.

## Statuswerte

Jeder manuelle Test verwendet genau einen Hauptstatus:

- `NOT_TESTED` – noch nicht manuell ausgefuehrt.
- `PASS` – erwartetes Verhalten vollstaendig beobachtet.
- `FAIL` – Verhalten widerspricht der Erwartung oder Regression vorhanden.
- `BLOCKED` – Test kann wegen einer anderen Stoerung/Voraussetzung nicht ausgefuehrt werden.
- `NOT_IMPLEMENTED` – das erwartete Feature ist bewusst noch nicht implementiert.
- `N/A` – auf diesen Build/Testaufbau nicht anwendbar.

`NOT_IMPLEMENTED` ist kein Fehlerstatus. Er verhindert, dass geplante Features versehentlich als Regression behandelt werden.

## Test-ID-Schema

- `SMK-*` Core Smoke
- `WS-*` Workspace Shell
- `EMS-*` EMScript/TextEditor
- `BE-*` BlockEditor
- `FE-*` FlowEditor
- `SYNC-*` Editor-/Projection-Sync
- `RUN-*` DryRun/Runtime
- `RAIL-*` RailTrace/Recording
- `WV-*` Worldview/Datastore/Perception
- `PERSIST-*` Persistenz/Migration
- `INT-*` Integration/Provider/VT2VT
- `E2E-*` End-to-End

## Testfall-Schablone

```text
ID: BE-004
Status: NOT_TESTED
Prioritaet: P0 | P1 | P2 | P3
Ebene: T0 | T1 | T2 | T3
Bereich: BlockEditor

Ziel:
Was soll bewiesen werden?

Vorbedingungen:
- ...

Schritte:
1. ...
2. ...

Erwartet:
- ...

Beobachtet:
- ...

Regression:
[ ] Undo/Redo
[ ] Save/Reload
[ ] Text Projection
[ ] Block Projection
[ ] Flow Projection
[ ] RailTrace/Runtime

Evidenz:
- Build:
- Geraet:
- Screenshot/Video/Log:
- Issue:
```

## Prioritaeten

- **P0:** Kerninvariante, Datenverlust, Crash, falsche Workflow-Wahrheit, Runtime-Sicherheitsgrenze.
- **P1:** zentrale Bedienung oder Synchronisation stark beeintraechtigt.
- **P2:** wichtiger Feature-/Panel-Fehler ohne Gefahr fuer kanonischen Zustand.
- **P3:** Polish, Darstellung, Komfort, Edge Case.

## Reihenfolge fuer einen kompletten manuellen Testlauf

1. `00-CORE-SMOKE.md`
2. `01-WORKSPACE.md`
3. `02-EDITORS.md`
4. `03-EDITOR-SYNC.md`
5. `04-RUNTIME-RAIL-RECORDING.md`
6. `05-WORLDVIEW-PERCEPTION.md`
7. `06-PERSISTENCE-INTEGRATIONS.md`
8. `90-END-TO-END.md`

Die laufende Gesamtuebersicht steht in `MASTER_TEST_CATALOG.md`.

## Vorhandene automatisierte Testbasis

Im Repository existieren bereits echte Unit-/Regressionstests, unter anderem fuer:

- Panel-Restore (`PanelTypeRestoreTest`)
- Studio Logging (`StudioLogStoreTest`)
- WSS Drop Effects und Drag Transport (`WssDropEffectsTest`, `WssDragTransportTest`)
- RailTrace-Modelle (`RailTraceModelsTest`)
- EMScript Apply Guard und Editor Session (`EmscriptApplyGuardTest`, `EmscriptEditorSessionTest`)
- Workspace Resources und Sync Guard (`WorkspaceResourcesTest`, `WorkspaceSyncGuardTest`)
- Worldview Contracts (`WorldviewContractsTest`)
- Flowchart Projection (`IrGraphFlowchartProjectorTest`)
- Recording Event Store (`RecordingEventStoreTest`)
- Visual Policy/Semantics (`VisualPolicyResolverTest`, `VisualSemanticsReporterTest`)
- VT2VT Models/Codec (`Vt2VtModelsTest`)

Manuelle Tests sollen diese nicht kopieren, sondern die sichtbare Bedienung, Integrationspfade und Systeminvarianten abdecken.

## Testlauf-Protokoll

Pro Testsession sollte mindestens festgehalten werden:

```text
Datum:
Commit/Build:
APK/BuildType:
Geraet:
Android-Version:
Display-/Dichte-Einstellungen:
A11y aktiv: ja/nein
Overlay Permission: ja/nein
Besondere Provider: Tasker/OCR/OCV/YOLO/VT2VT/...
Tester:
```

## Abbruchregeln

Bei einem P0-Fehler den betroffenen Pfad nicht weiter als erfolgreich bewerten. Folgefehler werden als `BLOCKED_BY:<Test-ID>` dokumentiert. Besonders bei Save/Load, Apply, Editor-Sync und Runtime darf ein spaeterer sichtbarer Erfolg einen frueheren Zustandsbruch nicht verdecken.

## Definition "Core bereit fuer breiten Test"

Der Core gilt als bereit fuer den grossen manuellen Funktionsdurchlauf, wenn:

- T0 Build/Test/Install/Launch erfolgreich ist,
- der Core-Smoke keine P0-Fehler enthaelt,
- ein kleiner Workflow Text <-> Blocks <-> Flow semantisch konsistent bleibt,
- DryRun mindestens einen nachvollziehbaren Step-Fokus erzeugt,
- Save/Reload denselben Workflow semantisch wiederherstellt.
