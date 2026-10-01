# EMScript v1 Spec Decisions

Stand: 2026-09-21
Phase: M0 Spec Reconciliation
Status: Normativ eingefrorener V1-Vertrag, noch nicht implementiert

## Zweck und Quellen

Dieses Dokument gleicht fuenf voneinander abweichende Wahrheiten ab:

1. `docs/emscript/EMScript_v1_API_Referenz_Draft_0_3.docx`
2. `docs/reference/emscript/STABLE_V1_GRAMMAR.ebnf`
3. `docs/reference/emscript/LANGUAGE_CONTRACT.md`
4. `docs/emscript/EMSCRIPT_V1_CONFORMANCE_AUDIT.md`
5. `docs/emscript/EMSCRIPT_LANGUAGE_TRUTH_MAP.md`

`PROPOSED V1` ist eine Spezifikationsentscheidung, keine Aussage ueber den
aktuellen Produktionscode. Parser, Registry, IR, Serializer, Runtime, Editoren
und Tests bleiben durch M0 unveraendert.

## Statusbedeutung

- `KEEP_CURRENT`: bestehendes Verhalten wird V1-Vertrag.
- `ADOPT_DRAFT`: Draft 0.3 wird Zielvertrag.
- `KEEP_LEGACY_ALIAS`: Form bleibt nur fuer Import/Kompatibilitaet lesbar.
- `DEPRECATE`: Form bleibt temporaer lesbar und erhaelt Diagnose.
- `REMOVE_FROM_SPEC`: kein Bestandteil des kanonischen V1-Vertrags.
- `UNRESOLVED`: menschliche Freeze-Entscheidung bleibt erforderlich.

## Sprachentscheidungen

| Concept | CURRENT IMPLEMENTATION | OLD STABLE V1 | DRAFT 0.3 | PROPOSED V1 | Status |
| --- | --- | --- | --- | --- | --- |
| Variablen | bare Identifier | bare Identifier | `§name` canonical | `§name` canonical; bare Namen nur Legacy-Import | ADOPT_DRAFT |
| Tasker `%variable` | Katalogdefaults enthalten `%var`, Parser kann es nicht als Variable lesen | nicht definiert | `%` fuer Tasker/Provider-Kontext reserviert | `%name` nie lokale EMScript-Variable; nur typisierter Provider-Wert/Import | ADOPT_DRAFT |
| Gross-/Kleinschreibung | Keywords und Command-Lookup case-insensitive; Variablennamen werden original erhalten | nicht eindeutig normiert | Keywords uppercase, Commands lowerCamel, Typen PascalCase | Keywords case-insensitive lesbar und uppercase serialisiert; Commands canonical case-sensitive plus registrierte case-insensitive Legacy-Aliase; Variablen case-sensitive | KEEP_CURRENT |
| Newline/Semikolon | beide trennen Statements | Newline; Semikolon optional | Newline canonical; `;` kompatibel, Serializer entfernt es | Draft-Regel | ADOPT_DRAFT |
| Brace-Control | Parser akzeptiert `{}`; Generator emittiert `{}` | `END IF/LOOP/WHILE` canonical, Braces Legacy | `{}` reserviert; `END IF/WHILE/REPEAT` canonical | `END ...` canonical; Braces nur Legacy-Import und deprecate | ADOPT_DRAFT |
| `ELSEIF`/`ELSE IF` | beide akzeptiert; Generator nutzt `else if` in Braces | beide | `ELSEIF` canonical | `ELSEIF` canonical; `ELSE IF` Legacy-Alias | KEEP_LEGACY_ALIAS |
| Kommentare | `//` und einzeiliges `REM`; `rem.*` ist Command | `//` und `REM` | `//` und `/* ... */`; Directives separat | `//` und `/* ... */` canonical; `REM` nur Legacy-Kommentar; `rem.*` separat migrieren | ADOPT_DRAFT |
| String-Escapes | `\\`, `\"`, `\n`, `\r`, `\t` | identisch | `\\`, `\"`, `\n`, `\t`; `\uXXXX` optional | `\\`, `\"`, `\n`, `\r`, `\t`, `\uXXXX`; ungueltige Escapes liefern `INVALID_ESCAPE` und behalten den Source-Text | ADOPT_DRAFT |
| Unbekannte Escapes | Backslash wird still entfernt | nicht definiert | nicht als gueltiges Escape gelistet | strukturierte `INVALID_ESCAPE`-Diagnose; niemals still veraendern | ADOPT_DRAFT |
| Equality-/Comparison-Praezedenz | `== != < <= > >=` gleiche Stufe | Equality bindet schwaecher als Comparison | Equality bindet schwaecher als Comparison | Draft-Praezedenz | ADOPT_DRAFT |
| Unary `!` | fehlt | fehlt | canonical | unary NOT canonical, stabile Operator-ID | ADOPT_DRAFT |
| `LET`/`SET` | vorhanden; Importer legt bei `SET` unbekannte Variable faktisch an | vorhanden | `LET` deklariert, `SET` mutiert; Diagnosen fuer Doppel-/Unknown | Draft-Semantik; keine implizite Deklaration durch `SET` | ADOPT_DRAFT |
| Named Arguments | `:` wird lexed, aber nicht geparst | Contract erwaehnt spaetere Unterstuetzung | `name = expression`, nach positional args | Draft-Regel; doppelte/unbekannte Namen diagnostizieren | ADOPT_DRAFT |
| Default Arguments | Katalogdefaults, Sonderparser und Runtime koennen abweichen | CommandCatalog ist Owner | CommandDefinition ist Owner | nur zentrale CommandDefinition setzt Defaults; Serializer darf explizite Defaults kanonisch weglassen | ADOPT_DRAFT |
| Listen/Index | Tokens vorhanden, keine Grammatik | EBNF nennt Arrays, Implementierung fehlt | `List<T>`, `[...]`, `value[index]` | Draft mit `List<T>`; keine Map/Set/Tuple in V1 | ADOPT_DRAFT |
| Listenmutation/Indexgrenzen | fehlt | fehlt | offener Freeze-Punkt | `List<T>` ist veraenderbar und unterstuetzt mindestens add/remove/set; ungueltige Indizes liefern strukturierten Fehler, niemals Clamp/null/Fallback | ADOPT_DRAFT |
| `FOR`/`BREAK`/`CONTINUE` | nur Formatter/Highlighter, Parser lehnt ab | fehlt | canonical | Draft uebernehmen; `BREAK`/`CONTINUE` nur im Loop-Scope | ADOPT_DRAFT |
| `FUNCTION`/`RETURN` | fehlt | ausserhalb Stable-Kern | PREVIEW; Scope/Recursion/Closures offen | V1-Syntax mit lexikalischem Scope, typisierten Parametern, explizitem ReturnType und `Void`; keine Closures, Funktionswerte oder Rekursion | ADOPT_DRAFT |
| `null` | kein Literal; Runtime hat internes `NullValue` | fehlt | Freeze-Punkt | kein `null`/`undefined` in V1-Quellsprache; internes Missing bleibt Runtime-Detail | REMOVE_FROM_SPEC |
| Typmodell | Parser: Number/String/Bool; Katalogtypen und Blocktypen weichen ab | kleiner Basistypkern | Core plus strukturierte Domain-Typen | `String`, `Number`, `Bool`, `Any`, `List<T>` plus registrierte Domain-Typen; keine JVM-Zahltypen | ADOPT_DRAFT |
| Point/Bounds/Region/Line/Path | Region und Swipe-Geometrie nur raw; `clickPoint` Zahlen | Region als Katalogtyp | immutable Value API | Draft-Value-Modell; `Region` ist Such-/Viewportbereich, `Path` geordnete Bahn eines Pointers | ADOPT_DRAFT |
| Shape-Modell | fehlt | fehlt | Circle/Rectangle/Triangle/Polygon/Shape, genaue Obertypfrage offen | `Shape` als gemeinsamer Wert-Obertyp; konkrete immutable Shapes; Region bleibt kein Shape | ADOPT_DRAFT |
| Shape/Overlay-Lifecycle | fehlt | fehlt | Shape- und Runtime-Identitaet nicht getrennt | `Shape` ist immutable Value; `OverlayHandle` ist stabile Runtime-Identitaet, entsteht durch `draw`, endet durch `remove`, `clear` oder Kontextende | ADOPT_DRAFT |
| Geometry-Overloads/Bezier | raw oder fehlt | fehlt | genaue Region-/Path-/Bezier-Parameter offen | `Path` besteht aus zeitlosen Line-/Bezier-Segmenten; konkrete Constructor-Overloads liegen in CommandDefinition, enthalten aber nie Gesture-Timing | ADOPT_DRAFT |
| Geometry/Gesture-Timing | raw vermischt | fehlt | Path darf Timing tragen | harte Typgrenze: `Path` ist zeitlos; `PointerPath` verbindet Path mit Timing; `MultiPath` ist parallel, `GestureSequence` sequenziell | ADOPT_DRAFT |
| Click-Namen | `click("text")`, `clickPoint(x,y)`; Block `clickText` | `click(...)` im Contract | `click(Point|x,y)`, `clickText`, `clickElement`, `clickImage`, `clickTemplate`, `mClick` | Draft-Namen; `click("text") -> clickText` und `clickPoint -> click` als Legacy-Aliase | ADOPT_DRAFT |
| Swipe/Path | `swipe(ANY, repeat?)`; live extrahiert Zahlen | keine typisierte Path-Semantik | `Path` Value; `swipe(from,to,...)` oder `swipe(path)` | Draft-Regel; ein Path entspricht genau einem Pointer | ADOPT_DRAFT |
| Push/Pull/Pan/Rotate | fehlen | fehlen | definierte High-Level-MultiTouch-Familie | als canonical MultiTouch-Actions uebernehmen | ADOPT_DRAFT |
| Pinch | fehlt | fehlt | Semantik gegen Push/Pull offen | kein eigener V1-Befehl; Name reserviert und spaeter hoechstens Alias fuer `push`/`pull` ohne neue Semantik | REMOVE_FROM_SPEC |
| Rotate-Richtung | fehlt | fehlt | Vorzeichenkonvention offen | Android-Screen-Koordinaten: positiver Winkel clockwise, negativer Winkel counter-clockwise; Richtungs-Enum darf ergaenzen | ADOPT_DRAFT |
| MultiPath/MultiSwipe/Sequence | nur untypisiertes `touch(ANY)` | fehlt | MultiPath fuer parallele Pointer; Sequence fuer aufeinanderfolgende Gesten | Draft-Typtrennung uebernehmen | ADOPT_DRAFT |
| `build/add/go` Builder | fehlt | fehlt | deklarative Convenience-Schicht | nicht Teil des V1-Kerns; canonical sind `multiPath(...)` Value und `multiSwipe(value)` Action | REMOVE_FROM_SPEC |
| Low-Level `touch.*` | ein generisches `touch(sequence)`; nicht live implementiert | fehlt | `single/multi/down/move/up/dispatch/reset/swipe/getBetween` | Draft als Escape-Hatch, aber typisiert ueber Pointer/TouchSequence | ADOPT_DRAFT |
| Draw | fehlt | fehlt | Geometry Value plus Draw Action; canonical Form offen | `draw(value, ...)` canonical; `draw.circle` usw. nur Convenience-/Legacy-Aliase | ADOPT_DRAFT |
| Transform | fehlt | fehlt | move/translate/rotate/scale/alpha/resize | namespaced `transform.*` canonical, auf Shape/Overlay-Handles typisiert | ADOPT_DRAFT |
| Animate | fehlt | fehlt | move/rotate/scale/alpha/path/sequence | namespaced `animate.*` canonical; Dauer/Easing typisierte Parameter | ADOPT_DRAFT |
| Query-Rueckgabetypen | viele `get/is/find/compare` Commands sind `STATEMENT -> Void` | Typvertrag unvollstaendig | Queries liefern typisierte Werte | jede Query braucht ReturnType und darf in Expressions stehen; Actions bleiben Void | ADOPT_DRAFT |
| Provider-Namespaces | `Tasker.*`, `Shizuku.*`, `Termux.*`, `ChromeTab.*`, `Scrcpy.*`, `Chart.*`; Lookup case-insensitive | qualifizierte Calls | lowercase Namespaces, Capability-basiert | `tasker.*`, `shizuku.*`, `termux.*`, `customTab.*`, `scrcpy.*`, `chart.*`, `vt2vt.*`; bestehende Schreibweisen Legacy-Aliase | ADOPT_DRAFT |
| Unbekannte Provider/Commands | Parserfehler; Adapterstatus spaeter | Diagnose gefordert | strukturierte Unknown/Unavailable-Diagnosen | unbekannt = `UNKNOWN_COMMAND`; bekannt ohne Provider = `PROVIDER_UNAVAILABLE`; nie stiller Fallback | KEEP_CURRENT |
| `@`-Projection Directives | fehlen | fehlen | Projection AST, persistent, runtime-inert | Draft-Modell uebernehmen; eigener Projection-Knoten ausserhalb Runtime-IR | ADOPT_DRAFT |
| `rem.*` FlowNodes | acht Commands, als Statements/CONTROL_FLOW katalogisiert, praktisch runtime-inert | REM-Kommentar kollidiert begrifflich | durch `@`-Directives zu ersetzen | lesbare Legacy-Aliase; Serializer emittiert passende `@`-Directives; niemals Runtime-Commands | KEEP_LEGACY_ALIAS |
| Directive-Paarung | vorhandene REM-Facets teils punktuell/implizit | fehlt | `@group.start/end`, Layer/Page/Break; Paarung offen | Bereichs-Directives werden ueber stabile IDs explizit gepaart und auf missing/duplicate/mismatch/nesting validiert; punktuelle Directives bleiben ungepaart | ADOPT_DRAFT |
| Runtime-Marker | Log/Trace getrennt, keine klare Sprachform | fehlt | `trace.mark(...)` getrennt von `@marker` | `trace.mark` ist Action; `@marker` bleibt reine Projektion | ADOPT_DRAFT |
| Canonical Serializer | Generator emittiert Braces und Semikolons; Aliase werden teilweise normalisiert | line-oriented Ziel | deterministisch, Newline, END-Form, canonical Namen | Draft-Ausgabe, ohne optionale Semikolons; Legacy nur lesen | ADOPT_DRAFT |
| Legacy-Lebensdauer | uneinheitlich | keine Frist | Kompatibilitaetsperiode offen | dokumentierte Legacy-Formen bleiben in V1.x lesbar, werden nie serialisiert und koennen fruehestens in 2.0 entfernt werden | KEEP_LEGACY_ALIAS |

## Migrationskosten und betroffene Komponenten

### Hohe Kosten

- **Variablenpraefix und typisierte Variablen:** Lexer/Parser, parser IR,
  WorkspaceImporter, Generator, Formatter, Highlighter, Source-Mapping,
  Blockfelder, Runtime-Auswertung und Roundtrip-Tests.
- **Praezedenz und neue Expressions:** Parser, beide IR-Modelle, Importer,
  Block-Operatoren, Generator, beide Dry-Run-Runtimes und Conformance-Tests.
- **Listen, Funktionen und strukturierte Typen:** Parser/AST/IR, Symboltabellen,
  Rekursionsdiagnose, Workspace-Modell,
  Block-/Flow-Projektionen, Serializer, Validator und VM.
- **Geometry/Gesture/Draw:** zeitlose Geometry-Values, PointerPath-/MultiPath-
  Timing, OverlayHandle-Lifecycle, zentrale CommandDefinition, Domain-Values,
  Provider-Adapter, Blockfamilien, FlowNodes, Runtime und ShapeMaker-Grenze.
- **Projection Directives:** Lexer/Parser, Projection-AST, Persistenz,
  Block-/Flow-Facets, Import der acht `rem.*`-Formen und Invariantentests.

### Mittlere Kosten

- **END-Normalisierung:** Parser bleibt kompatibel; Generator, Formatter,
  Fixtures und Snapshot-/Roundtrip-Erwartungen aendern sich.
- **Named/default Arguments:** Parser, CommandDefinition-Aufloesung,
  Diagnostics, Generator und Completion/Parameterhilfe.
- **Query-ReturnTypes:** CommandCatalog, Parser-Ausdrucksaufloesung, IR,
  Blockslots, Runtime-Adapter und Provider-Tests.
- **Provider-Namespace-Normalisierung:** Registrydaten, Serializer,
  Dokumentation, Testskripte und Legacy-Alias-Tabelle.

### Niedrige Kosten

- Newline/Semikolon-Normalisierung, `ELSE IF`-Alias, Kommentar-Aliase und
  canonical case koennen im Importer/Serializer ohne sofortigen Semantikbruch
  eingefuehrt werden.

## Legacy-Normalisierung

| Legacy input | Canonical V1 output | Behandlung |
| --- | --- | --- |
| `LET x = 1` | `LET §x = 1` | Import-Alias mit Migrationshinweis |
| `if (...) { ... }` | `IF ... END IF` | deprecate; CST-basiert normalisieren |
| `ELSE IF` | `ELSEIF` | Import-Alias |
| `REM text` | `// text` | Kommentar-Alias |
| `click("Login")` | `clickText("Login")` | typgesteuerter Legacy-Alias |
| `clickPoint(10, 20)` | `click(10, 20)` | Command-Alias |
| `Tasker.runTask(...)` | `tasker.runTask(...)` | Namespace-Alias |
| `rem.flowBreak(...)` | `@flow.break(...)` | Projection-Alias |
| `rem.layoutHint(...)` | `@flow.layout(...)` | Projection-Alias |

`rem.region`, `rem.variableBulk`, `rem.expressionCapsule`, `rem.group` und
Off-Page-Paare benoetigen vor der Normalisierung einen eindeutigen Bereichs-
und Paarungsvertrag. Eine rein textuelle Eins-zu-eins-Ersetzung waere
verlustbehaftet.

## A Entscheidungen ohne Breaking Change

- Newline als canonical Statement-Grenze und optionales Semikolon beim Import.
- `ELSEIF` canonical bei weiter akzeptiertem `ELSE IF`.
- Case-insensitive Keyword-Import bei deterministischer Schreibweise.
- bestehende Escapes einschliesslich `\r` beibehalten und `\uXXXX` additiv
  aufnehmen.
- Defaultwerte zentral aus CommandDefinition beziehen.
- unbekannte Commands und fehlende Provider getrennt diagnostizieren.
- `null` nicht in die V1-Quellsprache aufnehmen.
- Query-Rueckgabetypen, neue Domain-Typen und Low-Level-Touch additiv einfuehren.

## B Entscheidungen mit Serializer-Normalisierung

- bare Variable zu `§variable`.
- Brace-Control zu `END IF`, `END WHILE`, `END REPEAT`.
- `ELSE IF` zu `ELSEIF`.
- optionale Semikolons entfernen.
- Command- und Provider-Aliase auf canonical lowerCamel-Namen schreiben.
- `click("text")` zu `clickText("text")` und `clickPoint` zu `click`.
- eindeutig abbildbare `rem.*`-Formen als `@`-Directives schreiben.

## C Entscheidungen mit Import oder Legacy Alias

- bare lokale Variablen, Braces, `ELSE IF`, `REM`-Kommentare.
- bisherige uppercase/alte Command-Namen und Provider-Namespaces.
- `click("text")`, `clickPoint`, vorhandene `touch(sequence)`-Form.
- die acht `rem.*`-Commands, bis alle Projektionsmetadaten verlustfrei migriert
  werden koennen.
- bestehende `draw.circle`-artige Convenience-Namen, falls sie spaeter bereits
  in Skripten auftauchen; canonical bleibt `draw(value)`.

## D Echte Breaking Changes

- Equality und Comparison erhalten getrennte Praezedenzstufen; unklammerte
  Mischungen koennen danach anders gebunden werden.
- unbekannte String-Escapes werden Fehler statt still veraenderter Text.
- `SET` auf unbekannte Variablen und doppeltes `LET` werden Diagnostics statt
  impliziter Reparatur.
- Command-Namen werden nach der Kompatibilitaetsperiode canonical
  case-sensitive; nur registrierte Aliase bleiben lesbar.
- Projection-Metadaten duerfen nicht mehr als Runtime-/CONTROL_FLOW-Statements
  behandelt werden.

## E Noch menschlich zu entscheidende Punkte

Keine offenen V1-Freeze-Punkte. Konkrete Command-Signaturen, Provider-
Verfuegbarkeit und UI-Projektionen sind Implementierungsarbeit fuer M1 und
spaetere Meilensteine, aber keine offenen Sprachvertragsentscheidungen.

## M0 Ergebnis

Die V1-Linie ist eingefroren: Draft 0.3 wird fuer Syntax und Typmodell
uebernommen, durch die Entscheidungen dieses Dokuments praezisiert und durch
versionierte Conformance-Fixtures abgesichert. Funktionierende heutige Formen
bleiben explizite Legacy-Importe der gesamten V1.x-Linie. Implementierungs-
konformitaet ist noch nicht hergestellt und beginnt erst mit M1.

## V1 Clarification M1B-3B: Type-Conflict Treatment

Diese Klarstellung aendert weder die eingefrorene Grammatik noch bestehendes
Runtime-Verhalten. Sie legt fuer sieben historische Katalogformen fest, welches
V1-Konstrukt semantisch zustaendig ist. Eine Klarstellung gilt nicht als
abgeschlossene Migration.

| Stable ID | V1-Entscheidung | Primaerer Pfad |
| --- | --- | --- |
| `system.datastorePut` | Der bestehende V1-Runtime-Vertrag ist `datastorePut(key: String, value: String)`. Ein String-Default macht keinen Any-Parameter; vielmehr belegen Environment, Gegenstueck `datastoreGet` und Persistenz einen String-Store. | TYPE_MAPPING |
| `debug.log` | `log(value: Any): Void` akzeptiert einen Ausdruck. String, Number und Bool werden fuer das Log deterministisch textuell dargestellt; internes Missing/Null ist keine neue V1-Quellkonstante. | TYPE_MAPPING |
| `feedback.vibrate` | `vibrate(patternMs: Number...)` ist eine variadische Millisekundenfolge. Ein Wert ist eine Dauer; mehrere Werte bilden alternierende Delay-/Vibrationsphasen. Es gibt in V1 keine Repeat-Semantik und keinen impliziten Sprachdefault. | SIGNATURE_DECISION |
| `variable.set` | Der Wert von `SET` muss dem deklarierten oder aus `LET` abgeleiteten Variablentyp zuweisbar sein. Nur eine explizit als `Any` typisierte Variable akzeptiert beliebige V1-Werte. | TYPECHECKER_RULE |
| `variable.get` | Variablenlesen ist eine `VariableReference`-Expression und kein Source-Level-Command. Ihr Ergebnistyp ist der Typ der referenzierten Deklaration. | EXPRESSION_MODEL |
| `logic.boolean` | `true` und `false` gehoeren Grammar, Literal-AST und Literal-IR. `logic.boolean` und `literal.boolean` sind historische visuelle Projektionen, keine konkurrierenden Commands. | EXPRESSION_MODEL |
| `input.touch` | Raw `touch(sequence)` bleibt ein struktureller Legacy-Import. Je nach rekonstruierbarer Bedeutung wird er in typed Touch-Primitives, `PointerPath`, `MultiPath` oder `GestureSequence` ueberfuehrt; `Path` bleibt zeitlose Geometrie. | STRUCTURAL_MIGRATION |

Noch nicht umgesetzt sind der gemeinsame Variablen-Typechecker, lossless
Expression-Slots fuer Log im Workspace, die variadische CommandDefinition fuer
Vibrate, die Bereinigung historischer Bool-/Variablen-Blocktypen und der
payload-sensitive Touch-Migrator. Der Datastore-Katalog wird erst verengt,
wenn der Importer nicht-String Legacy-Aufrufe eindeutig diagnostizieren oder
verlustfrei migrieren kann.
