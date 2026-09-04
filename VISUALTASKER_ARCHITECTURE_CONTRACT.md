# VisualTasker Architekturvertrag

## Grundregel

- Workspace verwaltet Panels.
- WorkflowDocument verwaltet Intent-Wahrheit: was passieren soll.
- Worldview verwaltet Reality-/Knowledge-Wahrheit: was beobachtet, bekannt, angenommen oder unsicher ist.
- Runtime koppelt Intent und Reality und erzeugt neue Beobachtungen/Records.
- UI-Ansichten sind Projektionen und besitzen keine eigene dauerhafte Wahrheit.

Kurzform:

`Workflow = Intent`

`Worldview = Reality / Knowledge about Reality`

`Runtime = koppelt Intent und Reality`

## Rollen

### VisualTaskerStudio

- WorkflowDocument als zentrale Datenstruktur
- Recorder/Steps, Blockeditor, Flowchart
- EMScript/IR-Export, Runtime
- Accessibility/OCR/OpenCV und spaetere Computer-Vision Module
- Screenshot/Marker-Kontext als fachliche Daten

### VisualTasker Studio WSS

- Neutrale Floating-Panel-Shell
- Panel-Lifecycle, Drag/Resize, Fokus/Z-Index, Docking
- Snap/Grid, Auto-Arrange
- Tool-Panels als Projektionen auf Workflow, Worldview oder Runtime

## Worldview-Regel

Worldview ist keine zweite UI-Ablage und kein Accessibility-Tree. Accessibility,
OCR, OpenCV, YOLO, DOM, Marker, Screenshots, Templates, Recording und Inspector
liefern Beobachtungen, Ressourcen oder Projektionen desselben Worldview-Modells.

Zentrale Begriffe:

- `WorldEntity`: aufgeloestes Objekt oder Konzept in einer Scene.
- `Observation`: einzelne Provider-Beobachtung, keine Wahrheit.
- `Relation`: explizite Beziehung zwischen Worldview-Objekten.
- `Scene`: zeitlich/semantisch zusammenhaengender Zustand.
- `Record`: persistiertes Ergebnis eines Recording-/Runtime-Ablaufs.
- `Resource`: persistentes Artefakt wie Screenshot, Template, Marker oder Dataset.
- `Ambiguity`: explizite Unsicherheit oder Konflikt.

Providerdaten duerfen WorldEntities nicht still ueberschreiben. Der Pfad lautet:

`Observation -> Entity Resolution / Evidence Fusion -> WorldEntity`

Workflow darf bevorzugt Entity Concepts referenzieren, nicht rohe Koordinaten.

## Integrationsregel

Die Verbindung laeuft ausschliesslich ueber Adapter/Contracts:

`Workspace UI -> PanelAction -> Adapter -> WorkflowDocumentAction -> WorkflowDocument -> Projektionen -> Panels`

Worldview-Integration laeuft analog:

`Provider / Recorder / Runtime -> Observation/Record/Resource Command -> Validator -> Worldview Reducer -> Worldview -> Projektionen -> Panels`

KI-/RAG-Integration:

`Ambiguity/Question -> Retrieval -> ContextBuilder -> Model Provider -> ProposedResolution -> Preview/User Approval -> Validator -> Reducer`

## Verbote

- Keine EMScript-Logik in der Workspace-Shell
- Keine Runtime in der Workspace-Shell
- Keine eigene persistente Step-Wahrheit im Workspace
- Keine eigene persistente Worldview-Wahrheit in Panels
- Kein Missbrauch von Panel-Layout als Workflow-Logik
- Kein Accessibility-Tree als kanonisches Worldview-Modell
- Keine automatische Umwandlung von Recording in Workflow-Wahrheit
- Keine direkte LLM-Mutation von Workflow oder Worldview
- Kein RAG als eigene Datenwelt
- Blockeditor und Flowchart sind nicht die alleinige Wahrheit

## Entscheidungshilfen

- `Observed != Interpreted != Intended`.
- `Resource != Observation != Entity`.
- `DATA` zeigt persistente Artefakte; `CONTEXT` zeigt Retrieval-/KI-Kontext.
- ScreenshotCanvas ist ein gemeinsames Worldview-Werkzeug fuer Marker,
  Template, Vision, Recorder und Inspector.
- Neue Plugins muessen ihre Wahrheit ueber Contracts publizieren; UI-State ist
  nur transient.
