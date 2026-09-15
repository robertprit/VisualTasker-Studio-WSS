# AI Authority and Apply Boundary — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** vor allem `visualtasker-studio/docs/architecture/migration/AI_APPLY_TO_EMSCRIPT_BOUNDARY.md`

## Kernregel

AI-Ausgabe ist Vorschlag oder Draft. Sie ist weder Workflow-Wahrheit noch direkte Mutationsautorität.

Historische Kurzform:

```text
AI may propose.
System gates.
User or policy applies.
```

Im heutigen WSS wird das präzisiert zu:

```text
AI Task
  -> structured AiResult / AiProposal
  -> Preview / Diff / Clarification
  -> Validator / Policy
  -> explicit Apply / Domain Command
  -> Reducer
  -> WorkflowDocument or Worldview mutation
```

## Source Transfer != Source Mutation

Das alte Studio unterschied sinnvoll:

- AI erzeugt einen Draft;
- Draft wird explizit in eine Editor-/Source-Projektion übertragen;
- daraus entsteht noch nicht automatisch eine akzeptierte Workflow-Mutation.

Diese Trennung bleibt erhalten.

## WSS-Regeln

- AI darf interpretieren, erklären, diagnostizieren und Vorschläge erzeugen.
- AI darf strukturierte Kandidaten, Confidence, Evidenz und Ambiguität projizieren.
- AI darf keine direkte Mutation an WorkflowDocument oder Worldview durchführen.
- EMScript-, Block- und Flow-Proposals müssen denselben Domain-Command-/Validator-Pfad benutzen wie menschliche Änderungen.
- Generierter EMScript-Code ist zunächst Draft/Proposal und nicht ausführbare Authority.
- Human Correction oder Policy-Entscheidung kann aus einem Vorschlag einen akzeptierten Command machen.
- Runtime führt nur validierten, akzeptierten Workflow-Input aus.

## Sichtbare AI-Projektion

WSS sollte keine rohe private Modellreasoning-Kette anzeigen. Sinnvoll sichtbar sind stattdessen strukturierte Resultate wie:

- geladener Kontext;
- gefundene Kandidaten;
- verknüpfte Evidenz;
- Confidence;
- Ambiguity;
- Proposal;
- Diagnostic;
- Final Result.

## Training / Learning

`AiTask + AiContext + ModelResult + HumanCorrection + Outcome` kann später als Trainingskandidat dienen. Die Korrektur verändert jedoch nicht rückwirkend die ursprüngliche Observation oder das ursprüngliche Modellresultat.

## Hauptrisiko

Der gefährlichste Drift wäre ein separater "AI Apply"-Weg, der Parser, Validator, WorkflowCommand, Reducer oder Authority-Regeln umgeht. Jede neue AI-Funktion muss deshalb gegen dieselben Mutationsgrenzen geprüft werden wie menschliche UI-Interaktionen.
