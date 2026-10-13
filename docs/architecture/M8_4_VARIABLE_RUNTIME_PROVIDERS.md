# M8-4 Variable Runtime, Scope Resolution and Storage Providers

Stand: 2026-10-13

## Ziel und Ownership

M8-4 verbindet die in `ProjectDocument` und `WorkspaceDocument` vorhandenen
Variablendefinitionen mit dem bestehenden `DeterministicExecutionRunner`. Es gibt weiterhin genau
eine Variablenregistry fuer Definitionen und genau eine Runtime-Engine. Mutable Laufzeitwerte sind
bewusst kein Bestandteil des kanonischen WorkflowGraph.

```text
VariableRef
  -> VariableScopeResolver
  -> VariableBinding
  -> VariableRuntimeSession
  -> session-local map oder expliziter VariableProvider
  -> typed ExecutionValue
```

`ProjectVariableBindings` erstellt beim Start einer Ausfuehrung einen unveraenderlichen Snapshot
aus Projekt-ID, Root-ID, stabiler Variablen-ID, Label, Typ, Scope und Provider-Bindung. Ein Wechsel
des aktiven Editors oder Roots aendert eine laufende Session daher nicht.

## Vertraege

| Vertrag | Verantwortung |
| --- | --- |
| `VariableBinding` | Stabile ID, Label, deklarierter Typ, Scope, StoragePolicy und Session-Kontext. |
| `VariableScopeResolver` | ID zuerst; Label nur bei genau einem Treffer; keine Scope-Eskalation. |
| `VariableRuntimeSession` | LET/SET, Typpruefung, Reads/Writes und sitzungsgebundene Werte. |
| `VariableProvider` | Explizites Read/Write mit Revision, Cancellation und typisiertem Fehler. |
| `VariableValueStore` | Atomare persistente Werte getrennt von Definition und WorkflowGraph. |
| `VariableTraceInfo` | ID, Scope, Provider, Operation, Status und Fehlercode ohne Variablenwert. |

`Void`, `Null`, `Bool(false)` und ein fehlender Wert sind verschiedene Zustaende. `Null` ist nur
fuer nullable Typen oder `Any` gueltig. Ein Provider-Read wird erneut gegen den deklarierten Typ
geprueft und kann den Typvertrag nicht umgehen.

## Scope- und Lebensdauerregeln

| Scope | Policy | Lebensdauer | Provider |
| --- | --- | --- | --- |
| Local | VOLATILE | aktuelle ExecutionSession; Funktionsframes folgen erst mit Funktionsruntime | keiner |
| Script | SESSION | aktuelle ExecutionSession des gebundenen Workflow-Roots | keiner |
| Project | PROJECT | persistent und strikt nach Project-ID isoliert | `visualtasker.variable.project` |
| AppShared | APP_SHARED | persistent projektuebergreifend, aber nur ueber explizite Binding-ID | `visualtasker.variable.app-shared` |
| External | EXTERNAL | vom explizit benannten Provider bestimmt | Provider-Bindung erforderlich |

Der historische Scope `Global` wird beim Runtime-Binding eindeutig auf `Project` normalisiert. Es
gibt keine globale Namenssuche. Gleichnamige Variablen bleiben ueber ihre IDs getrennt; ein
mehrdeutiger Label-Fallback bricht mit Diagnose ab.

LET deklariert pro Session genau einmal. Bei einem persistenten, bereits vorhandenen Project- oder
AppShared-Wert erhaelt LET den bestaetigten gespeicherten Wert, statt ihn bei jedem Scriptstart mit
dem Initialausdruck zu ueberschreiben. SET verlangt einen existierenden Wert.

## Persistenz, Concurrency und Recovery

`AtomicFileVariableValueStore` speichert Werte in einem privaten, nach Namespace und Variablen-ID
gehashten Pfad. Das JSON-Schema transportiert Bool, Number, String, List, Point, PathSegment, Path,
PointerPath, MultiPath und Null. Void ist nicht persistierbar.

Writes verwenden Compare-and-Set auf einer monotonen Revision. Pro Prozess serialisiert ein
Schluessellock konkurrierende Updates; ein veralteter erwarteter Stand liefert
`REVISION_CONFLICT`, niemals einen stillen Lost Update. Der neue Inhalt wird geschrieben und per
File-Descriptor synchronisiert, bevor die bestaetigte Datei ueber Backup und Rename ersetzt wird.
Cancellation vor dem Commit laesst den alten Stand unveraendert.

Beim Neustart gilt eine feste Recovery-Reihenfolge: ein vorhandenes Backup ist der letzte
bestaetigte Stand und gewinnt gegen eine gleichzeitig vorhandene Temp-Datei. Nur wenn weder Ziel
noch Backup existieren, wird eine vollstaendige Temp-Datei uebernommen.

## External und Tasker

External-Variablen laufen durch dieselbe `VariableProviderRegistry`; eine fehlende oder unbekannte
Bindung wird als `PROVIDER_UNAVAILABLE` diagnostiziert.

Der vorhandene Tasker-Stand ist bewusst `PARTIAL`:

- Discovery und Availability verwenden `TaskerRegistration`.
- Read verwendet ausschliesslich den bereits empfangenen `TaskerVariableSnapshotStore`.
- String, Number, Bool und nullable Werte werden explizit konvertiert; unklare Typen scheitern.
- Cancellation wird vor dem lokalen Read geprueft.
- Ein bestaetigter Write-/Callback-/Timeout-Vertrag existiert nicht; Writes liefern daher
  `WRITE_UNAVAILABLE` und werden nicht vorgetaeuscht.

Ein echter bidirektionaler Tasker-Provider mit Timeout und bestaetigtem Resultat bleibt M9-1.

## Runtime und Trace

Der produktive `RuntimeProcessHost` oeffnet pro `ExecutionRequest` eine
`VariableRuntimeSession`. `ProjectExecutionPlanner`, Workspace-Live-Run und Headless
`ExecutionSessionManager` transportieren den Binding-Snapshot bis zum bestehenden Runner. Reads
in Expressions sowie LET/SET verwenden denselben Pfad; IF/WHILE profitieren ohne Sonderruntime.

RailTrace erhaelt `VARIABLE_READ` und `VARIABLE_WRITTEN` mit strukturierter Provenienz. Weder
Trace-Nachricht noch Diagnose enthalten den Variablenwert. Runtime-Writes mutieren weder
WorkspaceDocument noch ProjectDocument.

## Testmatrix

Die M8-4-Suite prueft Local/Script LET/SET, Null/false/Void/missing, gleichnamige IDs, Project- und
AppShared-Isolation, Restart, CAS-Konflikte, Cancellation, Backup-/Temp-Recovery, alle aktuell
persistierbaren Wertfamilien, Providerverlust, Provider-Typverletzung, feste Root-/Projektbindung,
wertfreie Trace-Provenienz und einen vollstaendigen Runner-Durchlauf. Bestehende Projekt-, Paket-,
M4-, BlockEditor- und FlowEditor-Gates sichern Save/Reload, Single-Root-Migration,
Drei-Editor-Roundtrip und unveraenderten WorkflowGraph ab.

| AC | Status | Nachweis / Grenze |
| --- | --- | --- |
| AC-01 bestehende Architektur | PASS | Project-/Workspace-Definitionen und bestehender Runner werden wiederverwendet. |
| AC-02 typisierter Vertrag | PASS | Binding, Resolution, Provider, Failure, StoragePolicy und ExecutionValue. |
| AC-03 deterministische Scopes | PASS | ID-first, eindeutiger Label-Fallback, keine Eskalation. |
| AC-04 Project Storage | PASS | Projekt-Namespace, persistentes CAS und Recovery. |
| AC-05 AppShared Storage | PASS | Explizites Binding, gemeinsamer Namespace, keine projektlokale Namenssuche. |
| AC-06 External Provider | PASS | Registry und ehrlicher Tasker-PARTIAL-Vertrag; produktiver Write bleibt GAP. |
| AC-07 ExecutionRunner | PASS | Ein bestehender Runner mit optionaler VariableRuntimeSession. |
| AC-08 Concurrency/Atomicity | PASS | Schluessellock, Revision/CAS und Konfliktdiagnose. |
| AC-09 Recovery/Persistenz | PASS | Restart sowie deterministische Backup-/Temp-Recovery. |
| AC-10 Trace/Diagnostics | PASS | Strukturierte, wertfreie VariableTraceInfo. |
| AC-11 Editor-/Projektintegritaet | PASS | Binding-Snapshot, keine Graphmutation, bestehende M4-/Projektgates. |
| AC-12 Regressionen | PASS | App-, Runtime-, Package-, Block-, Flow-, M4- und Build-Gates. |

## Bewusste Grenzen

- Local-Funktionsframes folgen erst mit der noch nicht freigegebenen Funktionsruntime.
- Tasker-Write und live angefragte Tasker-Reads bleiben M9-1; der aktuelle Read ist Snapshot-basiert.
- Der Dateistore bietet Prozess-Atomicity und Crash-Recovery, aber keine Mehrprozess-Transaktion.
- Persistente Runtime-Werte sind absichtlich nicht Bestandteil von `.vtpkg`; Definitionen und
  Provider-Bindungen bleiben portabel, geraetelokale Werte werden nicht unbemerkt exportiert.
- Ein isolierter Android-Gerätetest benoetigt ein ausdruecklich freigegebenes Testprojekt.
