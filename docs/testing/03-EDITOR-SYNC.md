# 03 – Editor- und Projection-Synchronisation

Ziel: beweisen, dass TextEditor, BlockEditor, FlowEditor und RailTrace unterschiedliche Projektionen desselben kanonischen Workflow-Modells bleiben.

## SYNC-001 – Block -> Text
Status: `NOT_TESTED` · P0 · T2

1. BlockEditor: einfache Aktion hinzufuegen.
2. TextEditor oeffnen/refreshen.

Erwartet: EMScript repraesentiert dieselbe semantische Aenderung; keine zusaetzliche oder fehlende Aktion.

## SYNC-002 – Text Apply -> Block
Status: `NOT_TESTED` · P0 · T2

1. gueltigen EMScript-Draft aendern.
2. Apply ausfuehren.
3. BlockEditor pruefen.

Erwartet: Blockprojektion entspricht dem neuen WorkflowDocument; Apply bleibt atomar.

## SYNC-003 – Workflow -> Flow
Status: `NOT_TESTED` · P0 · T2

1. bekannten linearen und danach verzweigten Workflow erzeugen.
2. FlowEditor pruefen.

Erwartet: Nodes/Edges repraesentieren Reihenfolge und Branching korrekt.

## SYNC-004 – Block Selection -> Flow
Status: `NOT_TESTED` · P1 · T2

1. Block selektieren.
2. FlowEditor betrachten.

Erwartet: korrespondierender Node ist selektiert/fokussiert oder eindeutig referenzierbar.

## SYNC-005 – Flow Selection -> Block
Status: `NOT_TESTED` · P1 · T2

1. Flow-Node selektieren.
2. BlockEditor betrachten.

Erwartet: korrespondierender Block ist selektiert/fokussiert.

## SYNC-006 – Runtime Fokus -> Text
Status: `NOT_TESTED` · P1 · T2

Erwartet: aktueller Runtime-Step markiert die korrespondierende Source-/Textstelle ohne Draft-Mutation.

## SYNC-007 – Runtime Fokus -> Block
Status: `NOT_TESTED` · P1 · T2

Erwartet: aktueller Runtime-Step markiert den korrespondierenden Block.

## SYNC-008 – Runtime Fokus -> Flow
Status: `NOT_TESTED` · P1 · T2

Erwartet: aktueller Runtime-Step markiert den korrespondierenden Node/Runtime-Layer.

## SYNC-009 – Runtime Fokus -> RailTrace
Status: `NOT_TESTED` · P0 · T2

Erwartet: RailTrace-Auswahl, Index und Position folgen demselben Ausfuehrungsschritt.

## SYNC-010 – Save/Reload behaelt Semantik
Status: `NOT_TESTED` · P0 · T3

1. Workflow mit Variablen, Operator und Branch erstellen.
2. speichern/App neu starten/laden.
3. Text, Block und Flow vergleichen.

Erwartet: gleiche fachliche Struktur; View-spezifische Positionen duerfen abweichen, Semantik nicht.

## SYNC-011 – Invalid Draft isoliert
Status: `NOT_TESTED` · P0 · T2

1. gueltigen kanonischen Workflow herstellen.
2. im TextEditor invaliden Draft erzeugen.
3. Apply versuchen.
4. Block/Flow/Rail pruefen.

Erwartet: Draft bleibt Draft; keine andere Projektion wird durch den invaliden Text veraendert.

## SYNC-012 – Undo/Redo synchron
Status: `NOT_TESTED` · P0 · T2

1. BlockEditor-Struktur aendern.
2. Undo.
3. Text und Flow pruefen.
4. Redo.
5. erneut pruefen.

Erwartet: alle Projektionen folgen dem kanonischen Zustand; kein Editor besitzt eine eigene konkurrierende History-Wahrheit.

## Empfohlene Referenz-Workflows

### S1 – Linear

`START -> SET x = 1 -> SET x = x + 1`

### S2 – Branch

`START -> IF x > 0 -> THEN action A -> ELSE action B`

### S3 – Verschachtelt

`START -> LET/SET -> IF (a + b) > c -> nested action`

Diese drei kleinen Workflows sollten als wiederkehrende Regression-Fixtures benutzt werden, bevor grosse Demo-Workflows getestet werden.
