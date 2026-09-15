# VisualTasker Studio WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** ausgewählte Architektur- und Vertragsdokumente aus `robertprit/visualtasker-studio`.
>
> **Authority:** Bei Widersprüchen gelten `VISUALTASKER_ARCHITECTURE_CONTRACT.md`, aktuelle ADRs und die aktuelle Implementierung im WSS-Repository.
>
> **Zweck:** Bewährte Verträge und Semantik aus dem alten Studio erhalten, ohne historische Implementierungsdetails zur neuen Wahrheit zu machen.

## Grundregel

Die migrierten Dokumente sind Referenzwissen, keine automatische Implementierungsbehauptung.

Insbesondere gilt im WSS:

- `WorkflowDocument` besitzt Intent-Wahrheit.
- `Worldview` besitzt Reality-/Knowledge-Wahrheit.
- Runtime koppelt Intent und Reality.
- Panels und Editoren sind Projektionen.
- AI/RAG darf vorschlagen, aber Workflow oder Worldview nicht direkt mutieren.

## Übernommene Referenzen

- `language/UBIQUITOUS_LANGUAGE.md`
- `emscript/LANGUAGE_CONTRACT.md`
- `emscript/CAPABILITY_SCHEMA.md`
- `emscript/DIAGNOSTICS.md`
- `emscript/BROWSER.md`
- `runtime/RUNTIME_LIFECYCLE.md`
- `runtime/LEGACY_VM_VISUAL_SEMANTICS.md`
- `perception/VISION_PIPELINE.md`
- `ai/KNOWLEDGE_RAG_BOUNDARIES.md`
- `ai/AI_AUTHORITY_AND_APPLY.md`
- `legacy/OLD_STUDIO_REFERENCE_INDEX.md`

## Nicht übernommen

Migrationsprotokolle, Cutover-Reports, Milestone-Handoffs, Extraktions-Audits und ähnliche historische Dateien bleiben im alten Repository. Sie dokumentieren Projektgeschichte, sind aber keine geeigneten WSS-Verträge.
