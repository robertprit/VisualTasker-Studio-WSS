# EMScript Language Contract — WSS Reference

> **Status:** Adapted Reference
>
> **Origin:** `visualtasker-studio/docs/emscript/v0.1/EMSCRIPT_LANGUAGE_CONTRACT.md`
>
> **Authority:** Aktuelle EMScript-Implementierung, aktuelle ADRs und `VISUALTASKER_ARCHITECTURE_CONTRACT.md` haben Vorrang.

## Vertragsidee

EMScript ist eine Projektion derselben Workflow-Semantik, die auch Blockeditor und Flowchart darstellen. Parser-Syntax, Blocktypen, Flow-Nodes, Runtime-Instruktionen und Provider-Aufrufe dürfen deshalb keine konkurrierenden Wahrheiten bilden.

## Kanonische WSS-Syntax

WSS verwendet EMScript als lesbare Script-Projektion des kanonischen
WorkflowDocuments. Generatoren emittieren nur die folgende kanonische Form:

- Commands sind Funktionsaufrufe: `wait(500)`, `click("OK")`, `beep(1000,200,100)`, `vibrate(80)`.
- Erweiterte Provider-/Plugin-Commands verwenden Namespace und Member:
  `Clipboard.set("value")`, `Tasker.runTask("Name")`, `ChromeTab.open("https://example.org")`.
- Variablen werden mit `LET name = expr` deklariert und mit `SET name = expr` veraendert.
- Reporter/Expressions werden in Slots eingebettet und koennen Literale,
  Variablen, Funktionsaufrufe, Compare- und Operator-Ausdruecke enthalten.
- Kommentare sind `// ...` oder klassische `REM ...`-Zeilen.
- REM-FlowNodes sind keine Kommentare, sondern Namespace-Commands wie
  `rem.region("main","facet","auto")` und `rem.flowBreak("continued","right")`.

Beispiel:

```emscript
LET retry = 0
SET retry = retry + 1
wait(500)
Clipboard.set("visualtasker")
IF retry > 0
    log("running")
ELSE
    vibrate(40)
END IF
```

Capability Calls werden ueber registrierte Descriptoren aufgeloest. Sie sind
weder Reflection noch frei erfundener Object Dispatch.

## Kontrollfluss

Kanonische Control-Blöcke bleiben line-orientiert und verwenden schliessende
Keywords, damit TextEditor, BlockEditor und FlowEditor denselben Scope eindeutig
rekonstruieren koennen:

```emscript
repeat (3) {
    wait(100)
}

while (retry < 3) {
    SET retry = retry + 1
}

IF score > 10
    log("high")
ELSE IF score > 5
    log("medium")
ELSE
    log("low")
END IF
```

Der Parser darf akzeptierte Schreibweisen normalisieren. Der Generator soll
konsequent die kanonische Schreibweise ausgeben.

## Argumente Und Typen

Argumente werden positionsbasiert oder, sobald ein Command dies ausdruecklich
unterstuetzt, benannt uebergeben. Der CommandCatalog ist die Quelle fuer
Argumentnamen, Defaults, Pflichtfelder, Typen und Capability-Bedarf.

Aktuelle Grundtypen:

- `TEXT` / String-Literal: `"Login"`.
- `NUMBER`: `1`, `3.14`.
- `BOOLEAN`: `true`, `false`, Compare-Ausdruck oder Bool-Reporter.
- `DURATION_MS`: numerische Millisekunden.
- `PERCENT`: normalisierte Zahl oder Prozentwert nach Command-Vertrag.
- `REGION`: Region-/Marker-Ausdruck, z.B. `region(10,20,240,160)`.
- `ANY`: strukturierter Platzhalter fuer noch nicht final typisierte Werte.

## Stabile Grundregeln aus v0.1

- `LET` deklariert, `SET` veraendert. Beide sind heute kanonische WSS-Keywords.
- Generatoren sollten nur kanonische Syntax emittieren.
- Parser dürfen dokumentierte Legacy-Formen normalisieren.
- unbekannte, doppelte oder fehlende Argumente müssen deterministisch diagnostiziert werden.
- Provider-Abwesenheit ist ein expliziter Fehler und kein stilles `false`.
- Capability Calls können Side Effects haben; Value Constructors nicht.
- Contract Status und Implementation Status sind getrennte Aussagen.

## WSS-Anpassung

Die alte Aussage, dass die Workflow Domain die alleinige semantische Authority ist, wird präzisiert:

- WorkflowDocument besitzt Intent-Wahrheit.
- Worldview besitzt Reality-/Knowledge-Wahrheit.
- EMScript beschreibt Workflow-Intent und darf Observation/Worldview-Daten referenzieren, besitzt sie aber nicht.
- Runtime führt validierte Workflow-Semantik aus.

## Legacy- und Compatibility-Prinzip

Legacy-Syntax darf akzeptiert und normalisiert werden, ohne dass sie neue kanonische API wird. Kompatibilität ist ein Parser-/Normalizer-Thema; Generatoren sollen keine historischen Schreibweisen neu verbreiten.

## Offene Prüfpunkte vor einer neuen normativen EMScript-Version

Diese Referenz beschreibt den aktuellen WSS-Stand, ist aber noch kein
vollstaendiger Stable-V1-Sprachstandard. Vor einer normativen WSS-Version
muessen insbesondere gegen Code und Tests geprueft werden:

- vollstaendiger Typvertrag (`String`, `Number`, `Bool`, `Any`, `Region`,
  `Image`, `Path`, `Scene`, `DatasetRef` usw.);
- Try/Catch- und Fehlergrenzen;
- strukturierte Werte;
- Await/Event-Semantik;
- Browser-, Tasker-, Shizuku-, Termux- und weitere Capabilities;
- Round-Trip EMScript ↔ WorkflowDocument ↔ Block/Flow.
