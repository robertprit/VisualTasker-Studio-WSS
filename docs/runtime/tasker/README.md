# VisualTasker Studio WSS Tasker Testpaket

Dieses Paket prueft den ersten echten Tasker-Pfad:

1. WSS ruft einen Tasker-Task mit `Tasker.runTask(...)` auf.
2. Tasker liest `%par1`, `%par2` und optionale lokale Variablen.
3. Tasker sendet per Broadcast eine Rueckmeldung an WSS.
4. WSS legt die Rueckmeldung als RailTrace/Recorder-Event ab und zeigt einen Toast.

## Vorbereitung in Tasker

- Tasker installieren und aktivieren.
- Tasker: `Preferences > Misc > Allow External Access` aktivieren.
- Die Datei `VT_WSS_Tasker_Demo_Echo.tsk.xml` in Tasker importieren.
- Der importierte Task muss exakt `VT WSS Demo Echo` heissen.

## EMScript Beispiele

Minimal:

```emscript
Tasker.runTask("VT WSS Demo Echo", ["hello", "from-wss"]);
```

Mit lokalen Tasker-Variablen:

```emscript
Tasker.runTask("VT WSS Demo Echo", ["alpha", "beta"], {"%vt_source":"wss","%vt_case":"roundtrip"});
```

Kompatibilitaetsalias:

```emscript
Tasker.action("VT WSS Demo Echo", ["alias", "action"]);
```

## Rueckmeldung testen

Nach erfolgreichem Lauf sollte WSS einen Toast anzeigen und in den Recorder-Sessions eine Datei
`external-tasker-feedback.jsonl` aktualisieren. Das RailTrace/Stepper-Panel kann diese Datei als
externe Ereignisspur anzeigen.

Der Task nutzt intern:

```sh
am broadcast -a com.visualtasker.wss.action.TASKER_TEST_FEEDBACK -p com.visualtasker.wss --es task_name "VT WSS Demo Echo" --es message "par1=%par1 par2=%par2" --es par1 "%par1" --es par2 "%par2" --es vt_source "%vt_source" --es vt_case "%vt_case"
```

## WSS als Tasker-Plugin-Action

WSS registriert zusaetzlich eine einfache Locale/Tasker-Setting-Action:

- In Tasker: neue Action anlegen, `Plugin > VisualTasker Studio WSS` auswaehlen.
- Aktion auswaehlen:
  - `Event an RailTrace senden`: schreibt ein externes Event in die Recorder-/RailTrace-Spur.
  - `Workspace oeffnen`: schreibt ein Event und bringt WSS in den Vordergrund.
  - `Script-Draft an WSS senden`: schreibt das Draft-Payload als Event und oeffnet WSS; echte Background-Ausfuehrung folgt spaeter.
- Event-Slot auswaehlen:
  - `WSS Event Slot 1`
  - `WSS Event Slot 2`
  - `WSS Event Slot 3`
- Status auswaehlen:
  - `received`
  - `running`
  - `done`
  - `error`
- `Run-ID` optional setzen, wenn mehrere Tasker-Actions zu einem Lauf gehoeren sollen. Bleibt das Feld leer, erzeugt WSS beim Empfang eine `tasker-...` ID.
- Event-Name und Nachricht werden als strukturierte Attribute gespeichert.

Dieser Pfad prueft die Gegenrichtung: `Tasker -> WSS`. Der Direktaufruf
`Tasker.runTask(...)` prueft weiterhin `WSS -> Tasker`.

## Result-/Ack-Session

Jedes Tasker-Plugin-Event wird doppelt protokolliert:

- RailTrace/Recorder: `emscript-runtime/records/external-tasker-feedback.jsonl`
- Tasker-Session-Historie: `emscript-runtime/tasker/tasker-plugin-sessions.jsonl`

Die Session-Historie ist nach `runId` korrelierbar. Mehrere Tasker-Actions koennen
also denselben Lauf beschreiben:

1. `received`
2. `running`
3. `done` oder `error`

WSS zaehlt pro `runId` ein `ordinal` hoch. Darauf bauen spaeter
`Tasker.waitForResult(...)`, Bedingungen und echte bidirektionale Result-Sessions auf.

Kurzabfragen im EMScript:

```emscript
Tasker.lastResult()
Tasker.lastResult("tasker-run-id")
Tasker.error()
Tasker.error("tasker-run-id")
```

`Tasker.lastResult(...)` liest den letzten bekannten Sessionstatus. `Tasker.error(...)`
ist bewusst kurz und liefert nur den letzten Fehlerstatus oder `kein Fehler`; es ist
kein blockierendes Warten.

## Grenzen dieses Slices

- Das ist noch kein vollstaendiges Tasker/Locale-Plugin-Event/Condition-Paket.
- `Tasker.getVariable`, Profile-Steuerung und bidirektionale Result-Sessions bleiben geblockt, bis der Plugin-Vertrag sauber implementiert ist.
- Das Testpaket validiert die Grundkette: WSS -> Tasker -> WSS.
