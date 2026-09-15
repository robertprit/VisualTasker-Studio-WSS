# Worldview Drift Matrix

Stand: 2026-09-15

Der Architekturvertrag trennt Workflow, Worldview, Runtime und Projektionen. Diese
Matrix markiert, wo aktuelle Modelle bereits in die richtige Richtung zeigen und
wo noch Vermischungen drohen.

## Matrix

| Data Type / Model | Current Meaning | Actual Producers | Consumers | Conflicts With | Should Become | Migration Risk | Prioritaet |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `WorkspaceDocument` | Aktueller Workflow-Truth-Container | TextEditor Apply, BlockEditor, Importer | TextEditor, BlockEditor, FlowEditor, Runtime | Name klingt nach Layout/Workspace statt Workflow | `WorkflowDocument` oder klar aliasierter Workflow-Truth | Hoch | Hoch |
| TextEditor Draft | Nicht angewendeter Script-Entwurf | TextEditor | TextEditor, Apply-Pfad | Sichtbar neben generiertem Code aus Workflow | Expliziter Draft mit Apply/Dirty/Validation Status | Mittel | Hoch |
| FlowView / Flowchart Layout | Raeumliche Projektion des Workflows | FlowEditor, AutoArrange | FlowEditor | Darf keine Workflow-Wahrheit werden | Layout-only Projection State | Mittel | Hoch |
| BlockView Layout | Block-Darstellung und Blockpositionen | BlockEditor | BlockEditor | Darf nicht alleinige Workflow-Quelle bleiben | Layout-only Projection State plus Workflow edits | Mittel | Hoch |
| `WorkspaceResourceBundle` | Erste Resource-Slice fuer Screenshots/Marker/Templates | Legacy-Import, Marker, Canvas | Worldview, Datastore, Marker, Canvas | Kann mit Worldview verwechselt werden | Resource Slice innerhalb Worldview | Mittel | Hoch |
| `WorldviewDocument` | Struktur fuer Scene, Entity, Observation, Relation, Event, Step | Resource projector, future Vision/Recorder | Inspector, Datastore, AI/Junktor | Noch nicht alle Producer schreiben hinein | Kanonischer Reality/Knowledge Read Model | Mittel | Hoch |
| Screenshot Asset | Bildquelle fuer Human Canvas und Detailprojektionen | Screenshot import, Overlay, Recorder | WorkspaceCanvas, CanvasPanel, Marker, Vision | CanvasPanel darf nicht Originalwahrheit werden | Resource mit Provenance, Scene und Timestamp | Mittel | Hoch |
| Marker | Menschliche Markierung fuer Point/Region/Swipe/Spline/Path | MarkerPanel, Canvas | Marker, Canvas, Block/Node Codegen, Datastore | Transient UI vs persistierter Marker | Worldview Entity/Annotation plus Resource ref | Hoch | Hoch |
| Template | Such-/Vergleichsartefakt | Marker/Vision | Vision, Runtime, Datastore | Marker-Template und Vision-Template koennen driften | Resource + Observation + Capability binding | Hoch | Hoch |
| Vision Crop / Compare State | KI/Algorithmus-Arbeitsbereich | VisionPanel | VisionPanel, future Datastore | Darf nicht Human Canvas ersetzen | Eigener Vision Working Set | Mittel | Mittel |
| A11Y Tree | Momentaufnahme der Android UI | Accessibility adapter/Recorder | Inspector, Vision/Worldview, Runtime | Darf nicht gesamte Worldview sein | Observation Graph mit Provenance | Hoch | Hoch |
| OCR/OCV/YOLO Result | Algorithmische Beobachtung | Vision pipeline | Canvas, Datastore, Inspector, AI | Ergebnis darf nicht ohne Confidence als Wahrheit gelten | Observation mit Source, Confidence, Timestamp | Hoch | Hoch |
| Recorder Event | Was real passiert ist | Floating Overlay / Recorder | RailTrace, Canvas, LogConsole, Junktor | Nicht Workflow-Intent | Record Event mit Run/Session Identity | Mittel | Hoch |
| RailTrace Step | Zeitprojektion von Run/Record/WatchDog | Runtime, Recorder, Tasker | RailTrace, Canvas, Text/Flow/Block Fokus | Darf nicht Workflow veraendern | Projection auf Runtime/Record timelines | Mittel | Hoch |
| Tasker Event | Externe Automationsmeldung | Tasker Plugin | RailTrace, LogConsole, Runtime bridge | Weder Workflow noch Recorder per se | Eigene Event/WatchDog Timeline | Mittel | Mittel |
| Runtime Event | Ergebnis eines Dry/Wet Run | Runtime | RailTrace, LogConsole, Debug, Editor Fokus | Darf nicht Layout ersetzen | Execution Event mit Source Mapping | Mittel | Hoch |
| Datastore Model | Zentrale Daten-/Resource-Sicht | Resource/Worldview projectors | Datastore, Inspector, AI | Noch zu viel UI-nahe Mischung | Navigierbares Resource/Worldview Read Model | Mittel | Hoch |
| Inspector Read Model | Kontextsicht auf Auswahl | Editor selection, Worldview, Runtime | Inspector Sheets/Panels | Ueberladung mit Debugdaten | Schlanker Selection Inspector; Debug in Debug/Log | Mittel | Mittel |
| AI / Junktor Proposal | Vorschlag, keine Mutation | AI/Junktor future path | User, Inspector, Workflow apply | Direktmutation verboten | Proposal/Plan/Apply Contract | Hoch | Mittel |
| REM / Region / Bulk | Optische/semantische Flow-Vereinfachung | Text/Block/Flow | FlowEditor, BlockEditor, TextEditor | Kann echte Kontrollstruktur imitieren | Declarative Editor Facet Metadata | Mittel | Mittel |

## Kernaussage

Der Kurs stimmt: Workflow bleibt Intent, Worldview bleibt Reality/Knowledge,
Runtime bleibt Ausfuehrung und RailTrace bleibt Zeitprojektion. Der groesste
Driftpunkt ist Benennung und Sichtbarkeit im UI: Nutzer duerfen nicht den Eindruck
bekommen, TextDraft, FlowLayout, BlockLayout oder RailTrace seien konkurrierende
Workflow-Wahrheiten.

## Naechste Worldview-Schnitte

1. `WorkspaceDocument` im UI konsequent als Workflow/Script-Wahrheit erklaeren
   oder durch eine klarere Alias-Schicht kapseln.
2. Resource/Worldview-Provenance fuer Screenshot, Marker, Template, A11Y, OCR,
   OCV und YOLO vereinheitlichen.
3. CanvasPanel als Detailprojektion und WorkspaceCanvas als Originalarbeitsflaeche
   strikt trennen.
4. RailTrace-Modi `Records`, `Run` und `WatchDog` datenlogisch und visuell
   trennen.
5. Junktor/AI nur ueber Proposal-Objekte anbinden.
