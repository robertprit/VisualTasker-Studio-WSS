# VisualTasker Studio WSS - Stable V1 Release Notes (Draft)

Stand: 2026-09-18

Dieses Dokument beschreibt den derzeitigen Release-Kandidaten. Es ist noch kein
Stable-V1-Tag und wird erst nach der manuellen Abnahmematrix finalisiert.

## Kern

- Workspace Shell ist der primaere Arbeitsmodus mit frei positionierbaren,
  skalierbaren und minimierbaren Panels.
- Der alte monolithische MainScreen ist kein startbarer Produktpfad mehr. Eine
  fixe Studio-Darstellung wird spaeter als Workspace-Layout-Preset umgesetzt.
- BlockEditor, FlowEditor und TextEditor arbeiten als Projektionen desselben
  Workflow-Dokuments; RailTrace bildet Runs, Records und WatchDog-Ereignisse ab.
- EMScript besitzt Parser, Generator, Command-Katalog, DryRun und geraeteabhaengige
  Live-Adapter.
- Workspace-, FlowView-, Resource- und RailTrace-Zustaende werden getrennt
  persistiert und ueber Release-Preflight geprueft.

## Editoren

- BlockEditor mit Toolbox-Sets, Siderail, Inspector, Minimap, Drag/Drop,
  Docking, Undo/Redo und Runtime-Fokus.
- FlowEditor mit IR-Projektion, semantischen Detail-Layern, Facets,
  Port-/Routing-Regeln, AutoArrange, Runtime-Trace und persistiertem FlowView.
- TextEditor mit Draft/Apply-Pfad, Source-Mapping und Runtime-Zeilenfokus.
- RailTrace mit getrennten Records-, Run- und WatchDog-Rails, Replay,
  Step-Navigation, logischer/zeitlicher Skala und persistentem Zoom.

## Runtime und Plugins

- Accessibility, Custom Tabs, Tasker, Termux, Shizuku und VT2VT werden ueber
  explizite Capability-Gates ausgewertet.
- Tasker-Testpaket und Rueckmeldepfad sind unter `docs/runtime/tasker/` enthalten.
- Termux-Voraussetzungen und Smoke-Script stehen unter `docs/runtime/termux/`.
- Shizuku verwendet eine UserService-Bruecke statt privater oder reflektierter APIs.
- VT2VT spiegelt validierte Workspace- und RuntimeTrace-Zustaende read-only ueber
  LAN/TCP oder die USB/ADB-Bridge; Pairing, Deduplizierung und Sequenzschutz sind aktiv.

## Ressourcen und Worldview

- Marker, Canvas, Vision und Datastore besitzen getrennte Verantwortlichkeiten.
- `.ema`-VisualAssets werden katalogisiert, auf Integritaet geprueft und als
  Workspace-Ressourcen referenziert.
- Datastore zeigt Release-Preflight und Asset-Diagnosen.

## Qualitaet

- `scripts/wss-smoke.sh` prueft Unit-Tests und Debug-Build; `--device` ergaenzt
  Installation, Kaltstart, Vordergrund- und Crash-Pruefung.
- `scripts/wss-release-candidate.sh` prueft beide Editor-Plugins, App-Tests,
  Lint und das unsignierte Release-Artefakt ohne Commit oder Veroeffentlichung.
- Die manuelle Abnahmematrix steht in `docs/SMOKE_TEST_MATRIX.md`.
- Bekannte Grenzen stehen in `docs/KNOWN_LIMITATIONS.md`.
