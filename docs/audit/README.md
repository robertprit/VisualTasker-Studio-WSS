# Architecture Audits

Dieser Ordner ist der **Prüf- und Konsolidierungsbereich** von VisualTasker Studio WSS.

Ein Audit beantwortet nicht automatisch die Frage „Wie soll es sein?“, sondern zuerst:

1. Was behauptet der aktuelle Vertrag?
2. Was macht der aktuelle Code tatsächlich?
3. Wo stimmen beide überein?
4. Wo existiert Drift?
5. Ist die Drift Bug, Legacy-Kompatibilität, bewusste Entwicklung oder eine noch offene Produktentscheidung?

## Audit-Regel

Keine Architektur aus einer einzelnen historischen Datei rekonstruieren. Keine neue Architektur allein aus einem zufällig vorhandenen Codepfad ableiten.

Befunde werden klassifiziert als:

- `DECIDED`
- `COMPATIBLE`
- `PARTIAL`
- `DRIFT`
- `LEGACY`
- `UNKNOWN`
- `OWNER_DECISION_REQUIRED`

Erst danach folgt ein Repair-/Decision-Slice.

## Aktueller Schwerpunkt

Aus dem aktuellen Architektur-Feinschliff-Audit sind drei Arbeitsmatrizen
entstanden:

- [`EMSCRIPT_WORKFLOW_ROUNDTRIP_MATRIX.md`](EMSCRIPT_WORKFLOW_ROUNDTRIP_MATRIX.md)
  prueft Parser, WorkflowDocument/IR, Generator, BlockEditor, FlowEditor,
  Runtime und RailTrace als gemeinsamen Roundtrip-Pfad.
- [`CAPABILITY_DRIFT_MATRIX.md`](CAPABILITY_DRIFT_MATRIX.md) markiert Drift
  zwischen CommandCatalog, UI-Katalogen, Parser, RuntimeGate, Adaptern und
  Diagnose.
- [`WORLDVIEW_DRIFT_MATRIX.md`](WORLDVIEW_DRIFT_MATRIX.md) trennt Workflow,
  Worldview, Runtime, Record, RailTrace, Canvas, Vision und Datastore.

Der nächste große Audit-Komplex ist in `NEXT_ARCHITECTURE_AUDIT_BRIEFING.md` vorbereitet. Er umfasst unter anderem:

- VAL / WSS Visual Semantics;
- integrierten ShapeMaker;
- VT Intelligence Runtime;
- große und kleine lokale Modelle;
- Function Learning;
- klassisches ML;
- YOLO/Vision Training;
- Dataset Lifecycle;
- ökologisches Recording;
- Active Learning / Perugger;
- Prediction vs Reality;
- Model Registry und Evaluation;
- Junktor-Semantik.

`Junktor` ist der kanonische Begriff. Die Integration von ShapeMaker in VisualTasker Studio ist ebenfalls bereits entschieden; offen sind dort nur die technischen Modul-, Asset- und Servicegrenzen.

## Ziel eines Audits

Ein gutes Audit endet nicht mit „müsste man mal refactoren“, sondern mit kleinen überprüfbaren Einheiten:

```text
Problem
→ Fundstelle
→ verletzter/ungeklärter Contract
→ Risiko
→ Ziel
→ Nicht-Ziele
→ kleinster Repair Slice
→ Akzeptanzkriterium
→ Test
```

Große Umbauten ohne diese Zerlegung sind ausdrücklich unerwünscht.

## Autorität

Audit-Dokumente sind keine höhere Autorität als `../../VISUALTASKER_ARCHITECTURE_CONTRACT.md` oder akzeptierte ADRs. Wenn eine neue Entscheidung nötig wird, soll sie nach Klärung als ADR oder normativer Vertrag festgehalten werden.
