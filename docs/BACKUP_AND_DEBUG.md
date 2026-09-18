# Backup, Recovery und Debug

## Projekt sichern

1. Workflow im Workspace ueber das globale Speichern-Symbol sichern.
2. EMScript-Draft im TextEditor separat speichern, wenn er noch nicht angewendet
   wurde. Ein Draft ist nicht automatisch die Workflow-Wahrheit.
3. Referenzierte Screenshots, Marker-, Template- und `.ema`-Assets zusammen mit
   dem Projekt behalten. Der Release-Preflight meldet fehlende Asset-Dateien.
4. Vor App-Daten-Loeschung Projekt- und Asset-Dateien aus dem Geraet exportieren.

## Automatischer Smoke

Nur Build und Unit-Tests:

```bash
./scripts/wss-smoke.sh
```

Mit genau einem per ADB verbundenen Geraet:

```bash
./scripts/wss-smoke.sh --device
```

Der Geraete-Smoke installiert den Debug-Build, startet `MainActivity` kalt,
prueft die Vordergrund-Activity und sucht nach einem frischen AndroidRuntime-Crash.
Er klickt keine UI-Elemente und erstellt keine Screenshots.

Der RC-Check verwendet absichtlich isolierte Gradle-Prozesse mit SerialGC. Das
verhindert, dass ein alter, bereits stark belasteter IDE-/Gradle-Daemon den
Plugin-, Lint- oder Release-Build-Zustand beeinflusst.

## Manuelle Diagnose

- `LogConsole`: Runtime-, Adapter-, WatchDog- und Workflow-Meldungen filtern.
- `DebugInfo`: Source-Mapping, Selection und Plugin-/Panelzustand pruefen.
- `Datastore > Release Preflight`: strukturelle Fehler, Katalogprobleme und
  fehlende VisualAssets pruefen.
- `Settings > Plugins & Extras`: Adapter als `READY`, `BLOCKED` oder `MISSING`
  unterscheiden.

## Recovery

- Bei inkompatibler FlowView-Anordnung AutoArrange verwenden; das Workflow-
  Dokument bleibt davon unberuehrt.
- Bei ungueltigem Text-Draft nicht erzwingen: Parserdiagnose beheben und erst
  danach anwenden.
- Bei fehlendem `.ema`-Asset Datei wiederherstellen oder die Resource im
  VisualAsset-Katalog bewusst entfernen.
- App-Daten nur als letzte Massnahme loeschen, da dabei lokale Layouts, Drafts,
  RailTrace-State und nicht exportierte Ressourcen verloren gehen.
