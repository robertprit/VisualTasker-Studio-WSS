# 02 – EMScript, BlockEditor, FlowEditor

Ziel: jeden Editor als eigene Projektion pruefen, bevor die bidirektionale Synchronisation in `03-EDITOR-SYNC.md` getestet wird.

# EMScript / TextEditor

## EMS-001 – Gueltiger Draft
Status: `NOT_TESTED` · P0 · T1

1. TextEditor oeffnen.
2. minimal gueltiges EMScript eingeben.

Erwartet: Draft bleibt lokal bis Apply; keine spontane Mutation des kanonischen WorkflowDocuments.

## EMS-002 – Parserdiagnose
Status: `NOT_TESTED` · P0 · T1

1. absichtlich ungueltige Syntax eingeben.
2. Parse/Preview ausloesen.

Erwartet: praezise Diagnose; fehlerhafte Stelle nachvollziehbar; keine Workflow-Mutation.

## EMS-003 – Apply Preview
Status: `NOT_TESTED` · P1 · T1

1. gueltige semantische Aenderung vorbereiten.
2. Apply Preview oeffnen.

Erwartet: hinzugefuegte/entfernte/geaenderte Struktur wird semantisch beschrieben; keine rohe ID-Diff-Ausgabe als einzige Information.

## EMS-004 – Atomarer Apply
Status: `NOT_TESTED` · P0 · T2

1. gueltigen Draft anwenden.
2. sofort Text/Block/Flow pruefen.

Erwartet: entweder kompletter neuer Workflowzustand oder alter Zustand; kein halb angewendeter Graph.

## EMS-005 – Invalid Draft mutiert nichts
Status: `NOT_TESTED` · P0 · T2

1. kanonischen Workflow merken.
2. invaliden Draft anwenden.

Erwartet: Apply blockiert; kanonischer Workflow und andere Projektionen bleiben unveraendert.

## EMS-006 – Source-/Runtime-Markierung
Status: `NOT_TESTED` · P1 · T2

1. DryRun/Run eines bekannten Steps starten.
2. TextEditor beobachten.

Erwartet: aktuelle Textstelle wird nachvollziehbar markiert, ohne Draft/Quelle zu veraendern.

# BlockEditor

## BE-001 – Block erzeugen
Status: `NOT_TESTED` · P1 · T1

1. Block aus Toolbox erzeugen.
2. frei platzieren.

Erwartet: stabiler Block, korrekte Felder, keine Phantomkopie.

## BE-002 – Sequence Snap
Status: `NOT_TESTED` · P0 · T1

1. zwei Statement-Bloecke erzeugen.
2. NEXT/Sequence verbinden.

Erwartet: Snap Preview und Verbindung stimmen geometrisch und semantisch ueberein.

## BE-003 – Value-/Reporter-Snap
Status: `NOT_TESTED` · P0 · T1

1. Arithmetic/Compare-Block mit ValueInput erzeugen.
2. passenden Literal-/Variable-Reporter verbinden.

Erwartet: Reporter dockt am richtigen Socket; Subtree bewegt sich mit Parent.

## BE-004 – Typinkompatibilitaet
Status: `NOT_TESTED` · P0 · T1

1. Number-Reporter an Boolean-only Input ziehen.

Erwartet: kein gueltiger Snap; bestehende Struktur bleibt unveraendert.

## BE-005 – Parent mit Subtree ziehen
Status: `NOT_TESTED` · P1 · T1

1. Parent mit verbundenem Reporter/Compare/Substack ziehen.

Erwartet: kompletter verbundener Subtree folgt ohne antiparallele oder doppelte Offset-Bewegung.

## BE-006 – Detach/Subtree Peel
Status: `NOT_TESTED` · P1 · T1

1. mittleren Block B aus A -> B -> C loesen.

Erwartet: DetachMode bestimmt reproduzierbar, ob B allein oder B -> C bewegt wird; keine still verlorene Verbindung.

## BE-007 – IF/ELSEIF/ELSE
Status: `NOT_TESTED` · P0 · T1

1. IF mit Condition erstellen.
2. ELSEIF/ELSE hinzufuegen, sofern UI unterstuetzt.
3. unterschiedliche Statement-Stacks in Branches setzen.

Erwartet: Containerhoehen/-breiten passen; Branch-Reihenfolge bleibt stabil; keine doppelten Placeholder.

## BE-008 – Variablen/Literale/Operatoren
Status: `NOT_TESTED` · P0 · T1

1. Variable deklarieren/setzen/lesen.
2. Number/String/Bool-Literal verwenden.
3. Arithmetic + Compare verschachteln.

Erwartet: IDs und sichtbare Labels bleiben getrennt; Operatoren bleiben semantisch stabil.

## BE-009 – Undo/Redo
Status: `NOT_TESTED` · P0 · T1

1. create -> connect -> field edit -> detach.
2. schrittweise Undo, danach Redo.

Erwartet: Struktur und Felder kehren deterministisch zurueck; keine Verbindung ohne Gegenstueck.

## BE-010 – Save/Reload View State
Status: `NOT_TESTED` · P1 · T2

1. Blockpositionen/Zoom/Fokus setzen.
2. speichern/neustarten/laden.

Erwartet: View-Zustand wird rekonstruiert, ohne Workflow-Semantik zu veraendern.

# FlowEditor

## FE-001 – Node-Selektion
Status: `NOT_TESTED` · P1 · T1

1. verschiedene Node-Typen selektieren/abwahlen.

Erwartet: eindeutige Selection; Inspector/Fokus folgen korrekt.

## FE-002 – Node Drag/Pan/Zoom
Status: `NOT_TESTED` · P1 · T1

1. Node ziehen.
2. Canvas pannen und zoomen.

Erwartet: keine Koordinatenspruenge; Zoom/Pan zerstoert keine Verbindung.

## FE-003 – Dock/Undock/Detach
Status: `NOT_TESTED` · P0 · T1

1. Node an gueltigen Port ziehen.
2. wieder loesen.

Erwartet: magnetische Ports und resultierende Domain-Operation stimmen ueberein; keine zweite Graph-Wahrheit im ViewDocument.

## FE-004 – Ports/Edges
Status: `NOT_TESTED` · P0 · T1

1. Sequence-, Branch- und Dataflow-artige Verbindungen pruefen, soweit implementiert.

Erwartet: Portsemantik ist eindeutig; Edge verbindet die erwarteten Nodes.

## FE-005 – Auto-Arrange
Status: `NOT_TESTED` · P1 · T1

1. kleinen linearen und verzweigten Graph auto-arrangieren.

Erwartet: stabiler Hauptstamm, kurze nachvollziehbare Kanten, keine semantische Graphaenderung.

## FE-006 – Auto-Pan am Viewportrand
Status: `NOT_TESTED` · P1 · T1

1. Node/Facet waehrend Drag an jede Viewportkante fuehren.

Erwartet: kontrolliertes Auto-Pan; Dragziel bleibt unter Pointer logisch erhalten.

## FE-007 – Facets/Collapse
Status: `NOT_TESTED` · P2 · T1

1. Facet-Handle benutzen.
2. Node/Gruppe collapse/expand, soweit vorhanden.

Erwartet: Darstellung aendert sich, Semantik nicht.

## FE-008 – Minimap/Fokus
Status: `NOT_TESTED` · P2 · T1

1. Node auswaehlen.
2. Minimap/Centering/Fokus verwenden.

Erwartet: Viewport landet reproduzierbar am Ziel.

## FE-009 – Runtime Layer
Status: `NOT_TESTED` · P1 · T2

1. DryRun/Run starten.
2. FlowEditor beobachten.

Erwartet: Runtime-Zustand ist Overlay/Projection; Node-Struktur selbst wird dadurch nicht mutiert.
