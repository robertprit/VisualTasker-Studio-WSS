# Knowledge / RAG Boundaries — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/05_IKR_RAG_BOUNDARIES.md`

## Zweck

Dieses Dokument trennt Wissensinfrastruktur von Workflow, Runtime, Worldview und UI. Retrieval liefert Kontext; es besitzt keine fachliche Authority.

## Begriffsgrenzen

### Session Memory

Kurzlebiger Gesprächs- oder Task-Kontext. Nicht automatisch Projektwissen und nicht Source of Truth.

### Prompt/Profile Memory

Konfigurierter Kontext, Stil oder statische Hinweise. Nicht automatisch verifiziertes Projektwissen.

### Chat History

Interaktionshistorie. Kann Datenquelle werden, aber nur nach expliziter Auswahl-, Provenance- und Verifikationsregel.

### Project Knowledge

Dauerhafte projektbezogene Information mit Source Identity, Freshness, Confidence und Traceability.

### Runtime Observation

Ausführungsergebnis bzw. Laufzeitbeobachtung. Wird erst durch explizite Persistenz-/Knowledge-Regeln zu dauerhaftem Wissen.

### Dataset / Vision Artifact

Persistentes Trainings- oder Wahrnehmungsartefakt. Nicht automatisch Knowledge Fact.

### Retrieval Context

Für einen Task ausgewählte Fakten und Quellen. Advisory Context, keine Mutations- oder Validierungsautorität.

## WSS-Erweiterung

Die heutige Architektur benötigt zusätzlich eine klare Trennung zu `Worldview`:

- **Worldview** modelliert, was VTS über die aktuelle/gespeicherte Realität beobachtet, kennt, annimmt oder bezweifelt.
- **Project Knowledge** beschreibt dauerhaftes Wissen über das Projekt/System.
- **Memory** hält kontextuelle oder persönliche Kontinuität.
- **Dataset** hält Beispiele/Artefakte für Training, Auswertung oder Kuratierung.
- **RAG** wählt relevante Quellen für einen konkreten AI-Task aus.

Daher gilt:

`Knowledge != Memory != Dataset != Worldview != Retrieval Context`

## Erlaubte AI-Nutzung

RAG/IKR darf:

- Kontext liefern;
- Quellen referenzieren;
- Widersprüche oder Staleness melden;
- Erklärungen unterstützen;
- Drafts oder Vorschläge vorbereiten.

RAG/IKR darf nicht:

- WorkflowDocument direkt mutieren;
- Runtime-Semantik verändern;
- WorldEntities still überschreiben;
- UI-State als fachliche Wahrheit behandeln;
- Retrieval-Treffer automatisch zu Facts erklären.

## Retrieval Result Contract

Ein zukünftiges Result sollte mindestens ausdrücken können:

- ausgewählte Facts/Chunks;
- Source Reference;
- Freshness/Verification State;
- Confidence oder Relevance;
- Reason for Inclusion;
- Warnungen zu Lücken, Konflikten oder veralteten Quellen.

## Priorität der Quellen

Für technische VTS-Fragen sollten deterministische, strukturierte Quellen vor generativer Interpretation stehen. Aktuelle Contracts, ADRs und Code/Test-Fakten haben Vorrang vor alten Migrationsnotizen oder Chat-Summaries.
