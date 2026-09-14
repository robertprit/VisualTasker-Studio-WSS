# Old Studio Reference Migration Index

> **Status:** Migration Audit / Reference Index
>
> **Source repository:** `robertprit/visualtasker-studio`
>
> **Target repository:** `robertprit/VisualTasker-Studio-WSS`

## Bewertungslegende

- **ADAPT** — fachlicher Kern bleibt gültig, Begriffe/Authority an WSS anpassen.
- **LEGACY** — als Kompatibilitätsreferenz behalten, nicht als neuen Vertrag verwenden.
- **EXTRACT** — nur wenige Regeln retten, historische Implementierungsdetails verwerfen.
- **ARCHIVE** — im alten Repository belassen.

## Übernommene Kandidaten

| Alte Datei | Entscheidung | WSS-Ziel | Hauptanpassung |
| --- | --- | --- | --- |
| `docs/02_UBIQUITOUS_LANGUAGE.md` | ADAPT | `reference/language/UBIQUITOUS_LANGUAGE.md` | Workflow nicht mehr alleinige Wahrheit; Worldview/Runtime ergänzen |
| `docs/emscript/v0.1/EMSCRIPT_LANGUAGE_CONTRACT.md` | ADAPT | `reference/emscript/LANGUAGE_CONTRACT.md` | v0.1 nicht als Implementierungsbehauptung behandeln |
| `docs/emscript/v0.1/EMSCRIPT_CAPABILITY_SCHEMA.md` | ADAPT | `reference/emscript/CAPABILITY_SCHEMA.md` | Provider/Worldview-Trennung präzisieren |
| `docs/emscript/v0.1/EMSCRIPT_DIAGNOSTIC_CODES.md` | ADAPT | `reference/emscript/DIAGNOSTICS.md` | Codes als historische stabile Basis, nicht vollständiger heutiger Katalog |
| `docs/emscript/v0.1/EMSCRIPT_BROWSER_CONTRACT.md` | ADAPT | `reference/emscript/BROWSER.md` | Browser in allgemeines Capability-/Provider-Modell integrieren |
| `docs/architecture/runtime/RUNTIME_LIFECYCLE_CONTRACT.md` | ADAPT | `reference/runtime/RUNTIME_LIFECYCLE.md` | harte Kapazitätszahlen und Service-Klassen entnormativieren |
| `docs/03_RUNTIME_VISUAL_SEMANTICS.md` | LEGACY | `reference/runtime/LEGACY_VM_VISUAL_SEMANTICS.md` | `last_*` und alte Failure-Semantik explizit als Legacy markieren |
| `docs/architecture/migration/VISION_PIPELINE_BOUNDARY.md` | ADAPT | `reference/perception/VISION_PIPELINE.md` | Observation -> Resolution/Fusion -> WorldEntity ergänzen |
| `docs/05_IKR_RAG_BOUNDARIES.md` | ADAPT | `reference/ai/KNOWLEDGE_RAG_BOUNDARIES.md` | Worldview klar von Knowledge/Memory/Dataset trennen |
| `docs/architecture/migration/AI_APPLY_TO_EMSCRIPT_BOUNDARY.md` | EXTRACT | `reference/ai/AI_AUTHORITY_AND_APPLY.md` | alte ViewModel-/sync-Details entfernen; Domain Command/Reducer-Grenze erhalten |

## Weitere wertvolle Quellen, noch nicht als eigene Datei migriert

| Alte Quelle | Empfehlung |
| --- | --- |
| `docs/emscript/EMSCRIPT_LEGACY_COMPATIBILITY.md` | beim nächsten EMScript-Compatibility-Audit mit aktuellem Parser abgleichen und ggf. in `LANGUAGE_CONTRACT.md` integrieren |
| `docs/emscript/EMSCRIPT_CURRENT_GRAMMAR.ebnf` | gegen aktuelle Grammar/Parser-Tests diffen; nicht blind kopieren |
| `docs/emscript/v0.1/EMSCRIPT_CANONICAL_GRAMMAR.ebnf` | als historische Normquelle beim Grammar-Audit verwenden |
| `docs/emscript/EMSCRIPT_FEATURE_MATRIX.yaml` | als Audit-Datenquelle nutzen, aber Status gegen WSS-Code verifizieren |
| `docs/emscript/EMSCRIPT_FLOWGRAPH_CONTRACT_PROPOSAL.md` | gegen heutigen WorkflowDocument-/Flow-Contract prüfen; wahrscheinlich nur Teilkonzepte retten |
| `docs/architecture/runtime/RUNTIME_CONCURRENCY_POLICY.md` | mit heutiger Runtime, Parallel-Fiber- und VT2VT-Richtung neu bewerten |
| `DECISIONS.md` | einzelne Entscheidungen als neue WSS-ADRs neu formulieren, nicht als Ganzes kopieren |
| `ARCHITECTURE.md` | ausschließlich historische Modul-/Ownership-Referenz |

## Bewusst nicht migriert

Die folgenden Kategorien bleiben im alten Repository:

- `FLOWCHART_M216_*`, `M217_*`, `M218_*`;
- Extraction-/Cutover-/Deletion-/Readiness-Reports;
- alte Migrations-Meilensteinprotokolle;
- `ACTIVE_SOURCE_AUTHORITY.md` als große historische Migrationsakte;
- konkrete alte Modulbaum-/ViewModel-/Service-Snapshots;
- `AGENTS.md` und `CLAUDE.md` als repo-/agentenspezifische Arbeitsanweisungen;
- ShapeMaker-Skill als eigenständige Produkt-/Integrationsdomäne.

## Drift-Kategorien

### D1 — Terminology Drift

Alt: Workflow als einzige Wahrheit, Timeline, Agent Projection, alte Source-Authority-Terminologie.

Neu: WorkflowDocument + Worldview + Runtime, RailTrace, strukturierte AI Projection.

### D2 — Authority Drift

Alt: einige Texte setzen Workflow Domain absolut zentral.

Neu: Intent-Authority und Reality-/Knowledge-Authority sind getrennt. AI/RAG bleibt advisory.

### D3 — Runtime Drift

Alt: konkrete `ScriptExecutionService`, process-local Registry, feste Limits und VM-Globals.

Neu: Contract soll provider-/execution-topology-neutral bleiben und Remote/VT2VT nicht blockieren.

### D4 — Perception Drift

Alt: Detection/ElementNode/Canvas/Persistence teilweise vermischt.

Neu: Provider -> Processor -> Observation -> Resolution/Fusion -> WorldEntity/Scene.

### D5 — Editor/Projection Drift

Alt: Flowchart zeitweise read-only Viewer und Timeline als Projektion.

Neu: Flow ist Workflow-Projektion mit Authoring-Potenzial; RailTrace übernimmt Zeit/Verlauf/Runtime-Projektion.

### D6 — AI Drift

Alt: AI-Chat und Script-Transfer im Vordergrund.

Neu: strukturierte AiTask/AiResult/AiProposal-Pipeline; Chat ist optionale Projektion, Mutation nur über Domain Commands.

### D7 — EMScript Drift

Alt: v0.1-Vertrag enthält bewusst begrenzte Typ-/Capability-Semantik und zahlreiche Partial/Legacy-Konstrukte.

Neu: aktueller EMScript-Stand muss gegen Parser, IR/WorkflowDocument, Blockeditor, Flow und Runtime verifiziert werden.

## Reparaturpriorität

1. **Hoch:** EMScript Grammar/Feature Matrix gegen aktuellen Code und Round-Trip prüfen.
2. **Hoch:** gemeinsame Capability-/Diagnostic-Registry festziehen, bevor weitere Provider und AI-Tools wachsen.
3. **Hoch:** Runtime Execution Identity/Outcome-Vertrag an RailTrace/Flow/Block-Projektionen koppeln.
4. **Mittel:** Ubiquitous Language mit maschinenlesbarem Studio Vocabulary synchronisieren.
5. **Mittel:** Vision/Worldview-Modelle auf Observation/Entity/Resource-Trennung auditieren.
6. **Mittel:** alte ADRs einzeln prüfen und gültige Entscheidungen als WSS-ADRs neu schreiben.
7. **Niedrig:** historische Migration Reports nur indexieren; nicht portieren.
