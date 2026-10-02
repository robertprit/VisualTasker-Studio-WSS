# EMScript Language Contract — WSS Reference

> **Status:** Historical adapted reference; superseded for V1 syntax
>
> **Origin:** `visualtasker-studio/docs/emscript/v0.1/EMSCRIPT_LANGUAGE_CONTRACT.md`
>
> **Authority:** Der normative Zielvertrag liegt in
> [`EMSCRIPT_V1_SPEC.md`](EMSCRIPT_V1_SPEC.md) und
> [`EMSCRIPT_V1_GRAMMAR.ebnf`](EMSCRIPT_V1_GRAMMAR.ebnf). Dieses Dokument
> bleibt als Pre-Freeze-Migrationsreferenz erhalten. Der aktuelle
> Implementierungsstand wird separat auditiert.

## Vertragsidee

EMScript ist eine Projektion derselben Workflow-Semantik, die auch Blockeditor und Flowchart darstellen. Parser-Syntax, Blocktypen, Flow-Nodes, Runtime-Instruktionen und Provider-Aufrufe dürfen deshalb keine konkurrierenden Wahrheiten bilden.

## Kanonische WSS-Syntax

Die historische Pre-Freeze-Grammatik liegt in
[`STABLE_V1_GRAMMAR.ebnf`](STABLE_V1_GRAMMAR.ebnf). Die normative V1-Grammatik
liegt in [`EMSCRIPT_V1_GRAMMAR.ebnf`](EMSCRIPT_V1_GRAMMAR.ebnf). Der spaetere
zentrale `CommandDefinition`-Vertrag
ergaenzt diese Syntax um die gueltigen Command-Namen, Argumenttypen, Defaults
und Capability-Vertraege. Parser-Akzeptanz und Live-Verfuegbarkeit sind getrennt:
ein korrektes Plugin-Kommando kann syntaktisch gueltig und dennoch durch einen
fehlenden Adapter blockiert sein.

WSS verwendet EMScript als lesbare Script-Projektion des kanonischen
WorkflowDocuments. Generatoren emittieren nur die folgende kanonische Form:

- Commands sind Funktionsaufrufe: `wait(500)`, `click("OK")`, `beep(1000,200,100)`, `vibrate(100,200,100,400)`.
- `vibrate(patternMs: Number...)` verlangt mindestens einen Wert, besitzt keinen
  Sprachdefault und keine Repeat-Semantik. Ein Wert ist eine Dauer; mehrere
  Werte sind alternierende Delay-/Vibrationsphasen.
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

Nullability ist ein orthogonaler Suffixvertrag: `T?`. `T` darf nach `T?`
fließen, `T?` aber nicht ohne explizite, noch festzulegende Behandlung nach
`T`. `Any` verschluckt keine Abwesenheit; nullable Werte verlangen `Any?`.
`Bool?` ist keine gueltige IF-/WHILE-Bedingung. Ein Source-`null`-Literal,
implizites Unwrap, Truthiness und Sentinelwerte sind nicht Teil des Vertrags.

## Stabile Grundregeln aus v0.1

- `LET` deklariert, `SET` veraendert. Beide sind heute kanonische WSS-Keywords.
- Generatoren sollten nur kanonische Syntax emittieren.
- Parser dürfen dokumentierte Legacy-Formen normalisieren.
- unbekannte, doppelte oder fehlende Argumente müssen deterministisch diagnostiziert werden.
- Provider-Abwesenheit ist ein expliziter Fehler und kein stilles `false`.
- Provider-Ausfuehrung und Rueckgabewert sind getrennte Achsen: ein erfolgreicher
  Bool-Reporter darf `false` liefern, ohne dadurch fehlzuschlagen. Erfolgreiches
  Void ohne Value und erfolgreiches nullable `NullValue` bleiben unterscheidbar.
- Provider-Statusabfragen duerfen technische PackageManager-, Binder- oder
  Permission-Prueffehler nicht als fachliches `false` oder `NullValue`
  darstellen. `installed` bezeichnet nur Installation; Shizuku `available`
  bezeichnet den expliziten Vertrag aus Installation, live Binder und erteilter
  Berechtigung.
- `tasker.isInstalled()`, `shizuku.isInstalled()` und
  `termux.isInstalled()` sind nicht-nullbare Bool-Reporter. Nur eine
  abgeschlossene Paketpruefung ohne Treffer ergibt `false`; technische
  Paketprueffehler liefern strukturierte Runtime-Diagnosen.
- `shizuku.isAvailable()` ist ein nicht-nullbarer Bool-Reporter fuer exakt
  `installed && permissionGranted && binderAlive`. Erfolgreich festgestellte
  fehlende Voraussetzungen ergeben `false`; technische Paket-, Permission-
  oder Binder-Prueffehler ergeben unterschiedliche Runtime-Diagnosen.
- `tasker.isEnabled()` und `scrcpy.isRunning()` sind nicht-nullbare
  Bool-Reporter. Tasker-Installation ist nicht Tasker-Enabled; USB-/ADB-
  Bereitschaft ist nicht scrcpy-Sessionstatus.
- `tasker.getVariable(...)`, `termux.get(...)` und `scrcpy.get(...)` liefern
  `String?`; `shizuku.getUid()` liefert `Number?`. Ein erfolgreiches
  `NullValue` bezeichnet fachliche Abwesenheit, waehrend leere Strings und UID
  `0` echte Werte bleiben. Technische Prueffehler liefern Diagnostics.
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

## Erweiterungen Nach Stable V1

Folgende Konstrukte gehoeren ausdruecklich nicht zum Stable-V1-Kern und werden
erst ueber einen neuen Sprachvertrag aufgenommen:

- vollstaendiger Typvertrag (`String`, `Number`, `Bool`, `Any`, `Region`,
  `Image`, `Path`, `Scene`, `DatasetRef` usw.);
- Try/Catch- und Fehlergrenzen;
- strukturierte Werte;
- Await/Event-Semantik;
- Browser-, Tasker-, Shizuku-, Termux- und weitere Capabilities;
- benutzerdefinierte Funktionen und Subroutines.
