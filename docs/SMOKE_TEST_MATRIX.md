# WSS Smoke-Test-Matrix

Stand: 2026-09-18

## Automatisierter Basissmoke

Ausfuehren ohne Geraet:

```bash
./scripts/wss-smoke.sh
```

Mit genau einem per ADB verbundenen Geraet:

```bash
./scripts/wss-smoke.sh --device
```

Der Geraetelauf prueft Build, Unit-Tests, Installation, kalten Start,
Foreground-Activity und unmittelbare AndroidRuntime-Abstuerze. Er fuehrt keine
automatischen UI-Klicks aus und erstellt keine Screenshots.

Der vollstaendige, nicht veroeffentlichende Release-Candidate-Check prueft
zusaetzlich beide Editor-Plugins, Android Lint, Release-Build, Diff-Whitespace
und den SHA-256-Hash des unsignierten APK-Artefakts:

```bash
./scripts/wss-release-candidate.sh
```

Signierung, Commit, Tag, Push und die manuelle Abnahmematrix bleiben getrennte
und ausdruecklich freizugebende Schritte.

## Manuelle Stable-V1-Abnahme

| Bereich | Pruefung | Erwartung |
| --- | --- | --- |
| Editor-Sync | EMScript laden/anwenden, Block und Node selektieren | Text, BlockView und FlowView zeigen denselben Workflow und Fokus |
| Persistenz | Projekt und Layout speichern, App neu starten | WorkflowDocument und getrennte View-Zustaende werden wiederhergestellt |
| BlockEditor | Add, Move, Dock, Undock, Delete, Undo, Redo, Zoom | Interaktion bleibt nach Panel-/Fokuswechsel aktiv |
| FlowEditor | Connect, Detach, Single-/Group-Drag, AutoArrange | Ports, Routing und View bleiben stabil; keine View-Resets |
| RailTrace | DryRun sowie Recording-Replay schrittweise abspielen | Aktiver Step wird in Timeline und Editoren synchron fokussiert |
| Overlay | Toolbar, Recording und LiveMarker starten/stoppen | Overlay-State wird wiederhergestellt; Aufnahme erzeugt Events |
| Canvas/Marker | Recorder-Step und Marker auswaehlen | Workspace zeigt Original; Canvas zeigt fokussiertes Detail |
| Vision | Crop und Referenzbild vergleichen | Vision bleibt eigene Pipeline ohne Human-Canvas-Mutation |
| Datastore | Ressourcen, Sessions und Visual Assets oeffnen | Fehlende `.ema`-Dateien erscheinen als Diagnose |
| Plugins | Tasker, Termux, Shizuku, VT2VT und CustomTabs pruefen | Capability-Gates melden Ready, Blocked oder Missing eindeutig |

Automatische ADB-Klick- und Screenshot-Pruefungen erfolgen nur nach expliziter
Freigabe. LiveRun, Accessibility-Aktionen und externe Plugin-Kommandos benoetigen
zusaetzlich die passende Geraete- und Adapterkonfiguration.
