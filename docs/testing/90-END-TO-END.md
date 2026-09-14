# 90 – End-to-End Regression Scenarios

Ziel: nicht einzelne Panels, sondern die fachlichen Gesamtpfade von VTS pruefen. Diese Szenarien sind die wichtigste Abnahme fuer Architektur und Stable-V1-Haertung.

## E2E-001 – Authoring -> DryRun -> RailTrace
Status: `NOT_TESTED` · P0 · T3

**Vorbedingung**
- kleiner gueltiger Workflow mit mindestens zwei Schritten.

**Schritte**
1. Workflow im BlockEditor erstellen.
2. Text- und Flow-Projektion pruefen.
3. DryRun starten.
4. Steps fortschalten.
5. RailTrace verfolgen.

**Erwartet**
- alle Projektionen repraesentieren denselben Workflow.
- DryRun erzeugt nachvollziehbaren Execution-Fokus.
- RailTrace zeigt Verlauf ohne daraus neue Workflow-Wahrheit zu erzeugen.

## E2E-002 – EMScript -> Blocks -> Flow -> Save/Reload
Status: `NOT_TESTED` · P0 · T3

1. gueltiges EMScript mit Variable, Ausdruck und Branch schreiben.
2. Apply Preview pruefen und anwenden.
3. BlockEditor und FlowEditor kontrollieren.
4. speichern und App neu starten.
5. alle drei Projektionen erneut kontrollieren.

Erwartet: semantischer Roundtrip bleibt stabil; View-State und Workflow-Wahrheit bleiben getrennt.

## E2E-003 – Blocks -> EMScript -> Apply -> gleiche Semantik
Status: `NOT_TESTED` · P0 · T3

1. Workflow ausschliesslich ueber Blocks erstellen.
2. generiertes EMScript betrachten.
3. ohne semantische Aenderung erneut anwenden, sofern Apply-Flow dies erlaubt.

Erwartet: kein unnötiger Strukturverlust, keine ID-/Label-Verwechslung, keine Branch-Reorder-Regression.

## E2E-004 – Recording -> Record -> RailTrace Replay
Status: `NOT_TESTED` · P0 · T3

1. Recording starten.
2. mehrere reale Aktionen/Activity-Wechsel ausfuehren.
3. Recording stoppen und speichern.
4. RailTrace laden.
5. Replay ausfuehren.

Erwartet: Replay rekonstruiert Historie statt Aktionen erneut auszufuehren; Rohdaten bleiben von spaeterer Interpretation getrennt.

## E2E-005 – Resource -> Worldview -> Inspector/Canvas
Status: `NOT_TESTED` · P1 · T3

1. aktuell unterstuetzte Resource erzeugen, z. B. Screenshot/Marker/Template.
2. Datastore/Worldview betrachten.
3. Inspector/Canvas auf dieselbe Resource/Observation beziehen.

Erwartet: gemeinsame stabile Referenzen; Resource, Observation und Entity bleiben unterscheidbar.

## E2E-006 – Fehlerhafter Draft -> Diagnose -> keine Mutation
Status: `NOT_TESTED` · P0 · T3

1. bestehenden gueltigen Workflow herstellen.
2. invalides EMScript schreiben.
3. Apply versuchen.
4. Text, Blocks, Flow, Rail und gespeicherten Workflow pruefen.

Erwartet: Diagnose erscheint; kein kanonischer Zustand wird teilweise mutiert; vorheriger Workflow bleibt nutzbar.

## E2E-007 – Runtime Step -> vier Projektionen synchron
Status: `NOT_TESTED` · P0 · T3

1. Workflow mit mehreren eindeutig unterscheidbaren Steps starten.
2. mittleren Step aktivieren.
3. TextEditor, BlockEditor, FlowEditor und RailTrace nacheinander pruefen.

Erwartet: Textzeile, Block, Node und Rail-Item referenzieren denselben fachlichen Execution-Step. Dies ist eine zentrale Stable-V1-Invariante.

## E2E-008 – Neustart im normalen Arbeitszustand
Status: `NOT_TESTED` · P0 · T3

1. Workflow laden.
2. mehrere Panels anordnen.
3. Block/Flow View State veraendern.
4. Rail Session/Position setzen.
5. App normal verlassen und neu starten.

Erwartet: persistierbare Zustaende werden rekonstruiert; transiente Runtime-/Fokuszustaende werden nur dort wiederhergestellt, wo der Contract dies vorsieht; kein erfundener laufender Run.

# Stable-V1-Systemabnahme

Vor Stable V1 sollten mindestens folgende Bedingungen gelten:

- E2E-001, 002, 004, 006, 007 und 008: `PASS`
- kein offener P0-Datenverlust-/Truth-Split-Fehler
- keine direkte Panel- oder LLM-Mutation ausserhalb des Command/Validator/Reducer-Pfads
- Build + Unit Tests + Install + Launch stabil
- bekannte `NOT_IMPLEMENTED`-Bereiche als Known Limitations dokumentiert

# Spaetere Erweiterung

Wenn AI/Perugger/Tasker/YOLO/Provider produktiv werden, werden zusaetzliche E2E-Pfade ergaenzt, ohne die Kernpfade zu ersetzen:

`Observation -> Resolution -> Ambiguity -> Human Correction -> Dataset Candidate`

`Tasker Event -> Observation/Event -> Workflow Trigger -> Runtime -> Outcome`

`AI Task -> Structured Proposal -> Preview -> Validation -> Human/Policy -> Domain Command`
