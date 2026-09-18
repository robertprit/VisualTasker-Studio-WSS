# Termux Runtime Adapter

VisualTasker Studio WSS uebergibt `Termux.shell`, `Termux.run` und `Termux.api`
ueber den offiziellen `com.termux.RUN_COMMAND`-Service.

## Voraussetzungen

1. Termux aus einer aktuellen, miteinander kompatiblen Paketquelle installieren.
2. WSS die Permission `com.termux.permission.RUN_COMMAND` gewaehren.
3. In `~/.termux/termux.properties` diese Zeile setzen:

   ```properties
   allow-external-apps=true
   ```

4. Termux nach der Aenderung neu starten.

Android stellt den Inhalt von `termux.properties` anderen Apps nicht bereit. WSS
kann daher Installation und Android-Permission automatisch pruefen, die
`allow-external-apps`-Option aber erst beim echten RUN_COMMAND-Aufruf indirekt
validieren. Ein abgelehnter Aufruf wird als blockierter Adapterzustand gemeldet.

## Release-Smoke

```emscript
Termux.canRunCommands()
Termux.shell("printf visualtasker")
Termux.run("/data/data/com.termux/files/usr/bin/printf", "visualtasker")
Termux.api("battery-status")
```

Ohne Termux oder Freigabe bleibt DryRun gueltig; LiveRun meldet den Adapter als
fehlend beziehungsweise blockiert.
