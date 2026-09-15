# VisualTasker Studio WSS Ubiquitous Language

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/02_UBIQUITOUS_LANGUAGE.md`
>
> **Authority:** `VISUALTASKER_ARCHITECTURE_CONTRACT.md` und aktuelle ADRs haben Vorrang.

## Zweck

Dieses Dokument bewahrt die zentrale Terminologie des alten Studios und passt sie an das heutige WSS-Modell an. Ein Begriff soll in Code, Dokumentation, Panels, AI-Kontext und Reviews dieselbe fachliche Bedeutung besitzen.

## Zentrale Wahrheiten

Die alte Formulierung **"The Workflow is the truth"** ist im WSS zu grob. Heute gilt:

- **WorkflowDocument** = Intent-Wahrheit: was passieren soll.
- **Worldview** = Reality-/Knowledge-Wahrheit: was beobachtet, bekannt, angenommen oder unsicher ist.
- **Runtime** = Kopplung von Intent und Reality; sie führt aus und erzeugt neue Beobachtungen und Records.
- **Projection** = abgeleitete Darstellung; keine eigene dauerhafte Wahrheit.

Kurzform:

`Observed != Interpreted != Intended`

## Workflow-Begriffe

### Workflow

Benutzerdefinierte Automation. Sie beschreibt Ziel, Reihenfolge, Bedingungen, Inputs und Kontrollfluss.

### WorkflowDocument

Kanonische bearbeitbare Domänenrepräsentation eines Workflows im WSS. Panels und Editoren lesen daraus oder senden typisierte Änderungsbefehle dorthin.

### Workflow Node

Eine semantische Operation, Entscheidung, Wartebedingung, Beobachtung, Aktion oder Kontrollflussstruktur innerhalb des Workflows.

### Workflow Metadata

Beschreibende Daten wie Name, Tags, Beschreibung oder Projektzuordnung. Metadaten definieren nicht automatisch Ausführungssemantik.

## Worldview-Begriffe

### Observation

Ein einzelnes Ergebnis eines Providers oder einer Wahrnehmungsquelle. Eine Observation ist Evidenz, nicht automatisch Wahrheit.

### WorldEntity

Aufgelöstes Objekt oder Konzept in einer Scene. Providerdaten dürfen WorldEntities nicht still überschreiben.

### Scene

Zeitlich und semantisch zusammenhängender Kontext, in dem Observations, Entities und Relations interpretiert werden.

### Relation

Explizite Beziehung zwischen Worldview-Objekten.

### Ambiguity

Explizite Unsicherheit, Mehrdeutigkeit oder Konflikt. Ambiguity ist ein gültiger Zustand und darf nicht durch stilles Raten beseitigt werden.

### Resource

Persistentes Artefakt wie Screenshot, Template, Marker oder Dataset-Asset.

### Record

Persistiertes Ergebnis eines Recording- oder Runtime-Ablaufs.

## Projection-Begriffe

### Projection

Abgeleitete textuelle, visuelle, zeitliche, diagnostische oder AI-orientierte Darstellung von Workflow, Worldview oder Runtime.

### Projection State

Transienter UI-Zustand wie Auswahl, Zoom, Pan, Drag-Zustand, Layer-Sichtbarkeit oder Layoutmodus. Er ist keine Domänenwahrheit.

### EMScript Projection

Textuelle Projektion des Workflows.

### Block Projection

Hierarchische, verbindungsorientierte Projektion des Workflows als Blöcke, Slots, Reporter und Statements.

### Flow Projection

Graphische Projektion von Kontrollfluss, Übergängen, Verzweigungen und Beziehungen.

### RailTrace Projection

Zeitliche/kausale Projektion von geplantem, simuliertem, ausgeführtem oder aufgezeichnetem Verlauf.

### Runtime Projection

Darstellung von Execution State, Events, Outcomes, Fehlern und Laufzeitdiagnostik.

### Agent / AI Projection

Darstellung strukturierter Interpretation, Kandidaten, Evidenz, Ambiguität, Vorschlägen und Ergebnissen. Sie besitzt keine direkte Mutationsautorität.

## Actor-Begriffe

### User

Menschlicher Autor oder Operator. Er besitzt finale Intent-Autorität über Authoring-Entscheidungen.

### AI Actor

AI-System, das analysieren, erklären, generieren oder Änderungen vorschlagen kann. Ein AI Actor ist kein kanonischer Workflow- oder Worldview-Eigentümer.

### Runtime Actor

Ausführungssystem, das validierte Workflow-Semantik ausführt, Outcomes produziert und Beobachtungen meldet.

### Provider

Austauschbare Implementierung einer Capability oder Wahrnehmungsquelle. Provider besitzen weder Workflow-Wahrheit noch UI-Rendering-Autorität.

## Naming-Regeln

- Nicht zwischen `View`, `Panel`, `Projection`, `Provider`, `Adapter`, `Plugin` und `Tool` wechseln, wenn fachlich unterschiedliche Verantwortungen gemeint sind.
- Überladene Begriffe wie `State`, `Context`, `Node`, `Memory`, `AI` und `Plugin` qualifizieren.
- `Observation`, `Entity`, `Resource`, `Record`, `Workflow`, `Runtime` und `Worldview` niemals synonym verwenden.
