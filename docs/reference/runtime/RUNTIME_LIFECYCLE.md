# Runtime Lifecycle — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/architecture/runtime/RUNTIME_LIFECYCLE_CONTRACT.md`

## Kernidee

Runtime startet erst mit einem exakt akzeptierten, eingefrorenen Workflow-Ausführungseingang. Sie besitzt die Ausführung, nicht Workflow-Semantik, Editor-Drafts oder Persistence-Authority.

## Historische Lifecycle-Zustände

```text
ADMITTED
  -> STARTING
  -> COMPILING
  -> RUNNING
  -> COMPLETED

RUNNING / STARTING / COMPILING
  -> CANCELLING
  -> CANCELLED

zulässige Phasen
  -> FAILED
```

`COMPLETED`, `FAILED` und `CANCELLED` sind terminal und gegenseitig exklusiv.

## Regeln, die ins WSS übernommen werden sollten

- Ein Run besitzt eine stabile `RunId`.
- Ein Run bindet sich an eine konkrete Workflow-Revision bzw. einen eingefrorenen Ausführungseingang.
- Runtime darf nicht während eines Runs erneut aus dem Editor lesen und damit Semantik wechseln.
- Cancellation adressiert einen konkreten Run.
- Terminalzustände sind unveränderlich.
- Doppelte oder ungültige Zustandsübergänge werden typisiert zurückgewiesen.
- Runtime Events sind keine Workflow-Authority.
- Source/Draft-Inhalte gehören nicht unkontrolliert in Runtime Events oder Diagnostics.
- Process Loss darf nicht still einen Run mit neu gelesenem oder verändertem Input rekonstruieren.

## WSS-Anpassung

Im heutigen Architekturmodell gilt zusätzlich:

`WorkflowDocument -> Execution Input -> Runtime -> Action/Observation -> Record + Worldview Inputs`

Runtime koppelt Intent und Reality. Sie kann Observations und Outcomes erzeugen, besitzt aber weder WorkflowDocument noch Worldview als mutable interne Ablage.

## Nicht automatisch übernommen

Folgende Details aus dem alten Studio sind Implementierungs-Snapshots und bleiben ausdrücklich nicht normativ:

- konkrete Obergrenzen wie `64 concurrent runs`;
- feste Event-/Receipt-/Tombstone-Kapazitäten;
- `ScriptExecutionService` als zwingende Implementierung;
- process-local Registry als einzige mögliche Registry-Architektur;
- Tasker-spezifische Startpfade.

Diese Punkte müssen gegen die aktuelle WSS Runtime und zukünftige VT2VT-/Remote-Execution-Anforderungen neu entschieden werden.

## Anschluss an RailTrace

Runtime Events und Outcomes sollten über eine stabile Run-/Node-/Event-Identität in RailTrace, Flow, Block und LogConsole projizierbar sein. Die Projektionen dürfen dabei keine zusätzliche Runtime-Wahrheit erzeugen.
