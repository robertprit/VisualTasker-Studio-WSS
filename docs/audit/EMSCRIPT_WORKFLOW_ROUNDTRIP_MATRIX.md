# EMScript / Workflow / Editor Roundtrip Matrix

Stand: 2026-09-15

Diese Matrix vergleicht den aktuellen WSS-Stand mit den migrierten
Referenzdokumenten. Die Referenzdokumente sind kein Implementierungsbeweis.
Autoritativ bleiben der Architekturvertrag, ADRs und der aktuelle Code.

## Bewertungslogik

- `READY`: aktuell implementiert und durch Tests oder sichtbare Nutzung gedeckt.
- `PARTIAL`: Grundpfad existiert, aber Semantik, UI, Runtime oder Roundtrip ist noch
  nicht voll belastbar.
- `DRIFT`: Name, Bedeutung oder Besitz widerspricht der aktuellen Ubiquitous
  Language oder existiert mehrfach.
- `MISSING`: vorgesehen, aber im aktuellen WSS-Pfad noch nicht umgesetzt.
- `UNKNOWN`: nicht belastbar genug belegt.

## Matrix

| Bereich | Parser / Import | WorkflowDocument / IR | Generator / Export | BlockEditor | FlowEditor | Runtime / RailTrace | Status | Risiko / Befund |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `wait(...)` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | DryRun/Runtime vorhanden | READY | Basisbefehl ist Roundtrip-tauglich. |
| `click(...)`, `clickText(...)`, `clickPoint(...)` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Adapter-abhaengig | PARTIAL | Ausfuehrung haengt an A11Y/Device-Adaptern; Testscript muss Klicks mittig halten. |
| `touch(...)`, `swipe(...)` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Adapter-abhaengig | PARTIAL | Runtimepfad vorhanden, aber Geraeteadapter und visuelles Feedback bleiben kritisch. |
| `beep()`, `vibrate(...)` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | READY | Kanonische Funktionsschreibweise ist umgesetzt; Soundpfad bleibt geraeteabhaengig. |
| `log(...)` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | LogConsole/RailTrace vorhanden | READY | Grundfunktion ist stabil; Log-Kategorien weiter schaerfen. |
| `let(...)`, `set(...)`, Variablen | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | PARTIAL | Variablen sind funktional, aber FlowEditor-Darstellung/Bulk-Regionen und Scope-Semantik sind noch nicht final. |
| `if / elseif / else` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | PARTIAL | Branch-Roundtrip funktioniert grundsaetzlich; Flow-Layout, REM-Gruppen und Edge-Semantik bleiben harte Zone. |
| `repeat(...)`, `while(...)` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | PARTIAL | Basis laeuft; verschachtelte Loops, Trace-Fokus und Rueckkanten brauchen weiter Tests. |
| Reporter / Operator / Comparator | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Auswertung teilweise vorhanden | PARTIAL | Semantik ist darstellbar, aber FlowEditor-Ports, Slot-Visualisierung und Ausblendmodi sind noch nicht final. |
| Marker: `markerSave(...)`, `markerDelete(...)` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Adapter-/Resource-abhaengig | PARTIAL | Marker sollen beim Speichern Node und Block erzeugen und als Scene-Kategorie erscheinen; Persistenz/Datastore-Bruecke weiter haerten. |
| Template: `templateDefine(...)`, `templateCompare(...)`, `findTemplate(...)` | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vorhanden | Vision-Adapter-abhaengig | PARTIAL | DryRun erkennt Adapterfehler korrekt; Vision/Datastore/Marker-Kopplung noch nicht vollstaendig. |
| Clipboard / File / Cache | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Adapter-abhaengig | PARTIAL | Testscript meldete Adapterstatus; Capability-Kontrakt muss zentral werden. |
| Browser / CustomChromeTab | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Adapter in Arbeit | PARTIAL | Settings und RuntimeAdapter existieren als Zielpfad, aber vollstaendige Feature-Paritaet fehlt. |
| Tasker | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Event/RailTrace sichtbar | PARTIAL | Tasker-Events sind eher Record/WatchDog-Zeitdaten als Workflow-Wahrheit. |
| Shizuku | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Binder/Runtime noch unsicher | PARTIAL | Settings/Runtime zeigten zuvor false trotz Freigabe; UserService-Bruecke bleibt Pruefpunkt. |
| Termux | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Adapter in Arbeit | PARTIAL | RUN_COMMAND braucht externe App-Freigabe; Ergebnis-Session noch haerten. |
| scrcpy / VT2VT | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Verbindung grundsaetzlich sichtbar | PARTIAL | USB/ADB-Autoverbindung und Trace-Mirror muessen belastbar werden. |
| Charts | MISSING | MISSING | MISSING | MISSING | MISSING | MISSING | MISSING | Geplant, aber kein stabiler aktueller Befehlspfad. |
| OCR / OCV / YOLO / A11Y Scan | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Vision-Adapter-abhaengig | PARTIAL | VisionPanel bleibt eigene KI-Sicht; WorkspaceCanvas/CanvasPanel duerfen nur Projektionen sein. |
| REM / Regionen / Bulk / Off-Page Connector | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Projektion vorhanden | PARTIAL | Sehr wichtig fuer Flowchart-Vereinfachung; noch nicht komplett als kanonischer Editorvertrag fixiert. |
| Funktionen / Subroutines | Teilweise vorgesehen | Teilweise vorgesehen | Teilweise vorgesehen | MISSING | PARTIAL | MISSING | PARTIAL | Terminator/Subroutine-Nodes sind geplant; Syntax und Scope-Regeln noch ausarbeiten. |
| Fehlerdiagnose / Source Mapping | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Teilweise vorhanden | Log/RailTrace vorhanden | PARTIAL | Direktlinks und Mapping zu Textzeile, Block, Node, Edge und Step sind noch nicht durchgehend. |

## Rote Zonen

1. TextEditor-Draft, WorkflowDocument und Projektionen duerfen nicht als drei
   konkurrierende Wahrheiten erscheinen. Der Apply/Save-Vertrag muss im UI
   weiter explizit werden.
2. FlowEditor darf nicht aus Layout-Zustand neue Workflow-Semantik erzeugen.
   Layout bleibt FlowViewDocument, Workflow bleibt WorkflowDocument.
3. Runtime/RailTrace/Record sind Zeit- und Ergebnisprojektionen. Sie duerfen
   keine versteckten Workflow-Aenderungen erzeugen.
4. Adapterfehler muessen Capability-Diagnosen bleiben, keine stillen Unknowns.
5. REM/Region/Bulk-Facets sind visuelle und semantische Hilfsobjekte, aber keine
   Ersatzsprache neben EMScript.

## Naechste Reparaturschnitte

1. Eine zentrale `CommandDescriptor`/Capability-Quelle fuer Parser, BlockCatalog,
   FlowCatalog und RuntimeGate als verbindlichen Owner festlegen.
2. Roundtrip-Tests fuer verschachtelte Branches, Loops, Reporterketten und
   Marker/Template-Kommandos erweitern.
3. Source-Mapping von RuntimeEvent zu Textzeile, Block-ID, Node-ID, Edge-ID und
   RailTrace-Step durchziehen.
4. TextEditor-Draft/Apply/Save im UI eindeutig kennzeichnen.
5. REM-FlowNodes in BlockEditor und FlowEditor als ein Vertrag modellieren.
