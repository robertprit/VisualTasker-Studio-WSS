# M8-1 Native Compose Project Explorer

Stand: 2026-10-12

## Ziel und Ownership

Der Project Explorer macht den in M8-0 eingefuehrten `ProjectDocument`-Vertrag in der produktiven
Workspace Shell bedienbar. Er besitzt keine zweite Projekt- oder Ressourcenwahrheit.

```text
ProjectDocument
  -> ProjectExplorerProjector
  -> ProjectExplorerPanel
  -> ProjectExplorerController
  -> ProjectOperationExecutor
  -> ProjectDocument
```

Semantische Projektmutationen laufen ausschliesslich ueber `ProjectOperation`. Das Compose-Panel
meldet nur Benutzerabsichten. Panelposition, Groesse, Sichtbarkeit und Z-Order bleiben Eigentum der
vorhandenen WindowSession. Auswahl, Suchtext und expandierte Sektionen liegen getrennt im
`ProjectExplorerUiState`.

## Explorer-Projektion

`ProjectExplorerProjector` erzeugt eine deterministisch sortierte, read-only Projektion fuer:

- Workflows und aktiven Root,
- Functions und Subworkflows,
- Local-/Script-, Project-, App-Shared- und External-Variablen,
- Images/Templates, Points/Regions, Paths, Shapes und sonstige bestehende Ressourcentypen,
- ProjectValidator-Diagnosen.

Jedes Item traegt die stabile kanonische ID. Where-used wird aus `ProjectReference` berechnet.
Fehlende Ressourcen und fehlerhafte Referenzen bleiben sichtbar und werden als Status plus Text
ausgegeben; Farbe ist nicht der einzige Informationstraeger.

## Root-Navigation

Create, Rename, Delete und SetActiveRoot verwenden die bestehenden ProjectOperations. Neue IDs
werden nur bei einer expliziten Neuanlage erzeugt. Beim Root-Wechsel gilt:

1. Die Auswahl des bisherigen Roots wird projektionseigen gespeichert.
2. `SetActiveRoot` wird revisionsgesichert ausgefuehrt und persistiert.
3. Text-, Block- und FlowEditor erhalten dasselbe `WorkspaceDocument` des neuen Roots.
4. Der manuelle Textentwurf wird Root-spezifisch gespeichert und beim Wechsel ersetzt; Auswahl,
   Folds sowie lokales Undo/Redo des TextEditors werden fuer das neue Dokument zurueckgesetzt.
5. Der programmatische Textwechsel wird einmalig vom automatischen Source-Apply unterdrueckt und
   kann deshalb keine zusaetzliche Projektmutation ausloesen.
6. Die Root-Auswahl wird gegen das neue Dokument reconciled oder leer gestartet.
7. Andere Roots werden nicht veraendert.

Eine laufende Headless Session behaelt den bei ihrem Start kopierten `projectId`,
`workflowRootId`, `projectRevision` und ExecutionPlan. Ein UI-Root-Wechsel fuehrt weder einen Root
aus noch mutiert eine laufende Session.

## Editor- und Recorder-Mutationen

Editor-Aenderungen werden vor der sichtbaren Zustandsuebernahme als
`ProjectOperation.ReplaceRootWorkspace` auf den aktiven Root angewandt. Bei einer Ablehnung bleibt
der sichtbare kanonische Zustand unveraendert und der Explorer zeigt die Diagnose.

Recorder-Publishing verwendet `RecorderProjectWorkflowPublisher`. Preview und Confirm sind an
Project-ID, Root-ID, Projekt- und Workspace-Revision gebunden. Ein erfolgreicher Publish wird als
akzeptierter Projektuebergang in dieselbe Explorer-History aufgenommen.

## Guards und History

- Referenzierte Roots, Definitionen und Ressourcen werden vom bestehenden ProjectValidator bzw.
  ProjectOperationExecutor abgewiesen.
- Roots aktiver ExecutionSessions werden vor dem Executor blockiert.
- Der aktive Recorder-Publish-Root wird waehrend einer offenen Preview blockiert.
- Jede destruktive UI-Aktion benoetigt eine Bestaetigung; es gibt keine Kaskadenloeschung.
- Undo/Redo restauriert den ProjectDocument-Snapshot einschliesslich aktivem Root und laedt dessen
  Workspace wieder in alle drei Editoren.

## Persistenz und Migration

`ProjectWorkspaceStore` persistiert Projekt und Explorer-UI getrennt. Ein bestehender
Single-Workspace wird ueber `ProjectDocument.fromLegacyWorkspace` deterministisch migriert. Save
und Reload erzeugen keine neuen Projekt-, Root- oder Workspace-IDs. Die bestehende
`WorkspaceSessionStore`-Persistenz verwaltet das neue Panel wie alle anderen Workspace-Panels.

## UI-Vertrag

Der Explorer ist ein natives Material-3-Panel mit Suche, expandierbaren Sektionen, mindestens
48-dp grossen Item-Zeilen, Auswahl-Inspector, Root-CRUD, Undo/Redo und direktem Oeffnen in Text-,
Block- oder FlowEditor. TalkBack erhaelt Label, Status und Details. Nicht implementierte
Operationen werden nicht als funktionsfaehige Controls dargestellt.

## Abnahme

| AC | Status | Nachweis |
| --- | --- | --- |
| AC-01 ProjectDocument einzige Datenquelle | PASS | ProjectExplorerProjector und ProjectWorkspaceStore |
| AC-02 Native ExplorerProjection | PASS | ProjectExplorer.kt und ProjectExplorerPanel.kt |
| AC-03 Root-CRUD | PASS | ProjectOperations plus Bestaetigungsdialoge |
| AC-04 Drei-Editor-Root-Navigation | PASS | aktiver Root speist den gemeinsamen WorkspaceWorkflowState |
| AC-05 Definitionen und Where-used | PASS | stabile IDs, Gruppierung, Referenzzaehler und Delete-Guard |
| AC-06 Variable Scope Navigation | PASS | Scope-gruppierte, read-only Projektion mit Provider-Bindung |
| AC-07 Resource Navigation | PASS | Typ-/Scope-/Location-/Missing-/Where-used-Projektion |
| AC-08 ProjectOperations und Guards | PASS | ProjectExplorerController und ProjectOperationExecutor |
| AC-09 Undo/Redo | PASS | ProjectHistoryController und Root-Reattach |
| AC-10 Persistenz und WindowSession | PASS | getrennte Stores, Instrumentation und PanelType-Persistenz |
| AC-11 Runtime-/Recorder-Isolation | PASS | Session-Root-Snapshot und RecorderProjectWorkflowPublisher |
| AC-12 Regressionen und Geraetetest | PASS | Vollgates, Instrumentation und interaktiver Test auf SM-S918B |

## Geraetepruefung

Auf einem SM-S918B mit Android 16 wurden Project Explorer, Root-Neuanlage, Auswahl, Umbenennung,
Loeschbestaetigung, Suche, Panel-Reopen und ein kalter Activity-Neustart geprueft. Root-ID,
Projektzustand und Explorer-UI blieben stabil. Das Oeffnen desselben Roots in Text-, Block- und
FlowEditor funktionierte.

Der Root-Wechsel wurde zusaetzlich ueber die Projektrevision kontrolliert: Der Wechsel auf einen
leeren Root und der Rueckwechsel auf einen befuellten Root erhoehten die Revision jeweils exakt um
eins. Auch nach Wartezeit entstand keine zweite Source-Apply-Transaktion; der leere Root blieb bei
null Bloecken und der befuellte Root unveraendert. Die Store- und TalkBack-Instrumentationstests
liefen auf demselben Geraet erfolgreich.

## Bewusste Grenzen

- Definitionen ohne eigenes bestehendes Workspace-Dokument werden angezeigt, aber nicht in einem
  erfundenen Editor geoeffnet.
- Variablen sind in M8-1 navigierbar; neue Scope-Provider und Runtime-Persistenz folgen nicht.
- Ressourcen koennen inspiziert und referenzsicher entfernt werden; Import/Export und Asset-Pakete
  bleiben M8-6/M8-9.
- Typisiertes Drag-and-Drop vom Explorer in Editoren bleibt M8-7.
- Eine vorhandene M5-Gesten-Testflaeche liegt derzeit in der Workspace-Z-Order ueber Panels und
  kann Explorer-Zeilen optisch verdecken. Das ist kein Project-Explorer-Datenfehler, bleibt aber
  eine separate Shell-Darstellungsluecke.
- Kein VT2VT-, Cloud-, VirtualDisplay-, WorldviewGraph- oder KI-Scope wurde vorgezogen.
