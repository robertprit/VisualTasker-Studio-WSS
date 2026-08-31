# VisualTasker Architekturvertrag

## Grundregel

- Workspace verwaltet Panels.
- WorkflowDocument verwaltet Wahrheit.
- Runtime fuehrt aus.
- UI-Ansichten sind Projektionen und besitzen keine eigene dauerhafte Wahrheit.

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
- Tool-Panels als Dummy oder Projektion

## Integrationsregel

Die Verbindung laeuft ausschliesslich ueber Adapter/Contracts:

`Workspace UI -> PanelAction -> Adapter -> WorkflowDocumentAction -> WorkflowDocument -> Projektionen -> Panels`

## Verbote

- Keine EMScript-Logik in der Workspace-Shell
- Keine Runtime in der Workspace-Shell
- Keine eigene persistente Step-Wahrheit im Workspace
- Kein Missbrauch von Panel-Layout als Workflow-Logik
- Blockeditor und Flowchart sind nicht die alleinige Wahrheit
