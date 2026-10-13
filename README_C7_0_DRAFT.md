# VisualTasker Studio

> **Status: Pre-Alpha / aktive Entwicklung.** Dieses Repository enthaelt belastbare Kernpfade,
> ist aber noch kein Stable-1.0-Release. Es gibt derzeit keinen Release-Tag.

VisualTasker Studio ist eine Android-native Entwicklungsumgebung fuer visuelle Automationen. Ein
Workflow wird als kanonisches Dokument gespeichert und kann synchron als Text, Bloecke oder
Flowgraph bearbeitet werden. Eine typisierte Runtime fuehrt freigegebene Schritte aus und liefert
Trace-, Recorder- und Evidence-Daten zurueck.

Das Projekt wird in Kotlin mit Jetpack Compose entwickelt. Der Android-Host verwendet aktuell
`minSdk 29`, `compileSdk 36`, `targetSdk 36` und JDK 17.

## Was heute nachgewiesen ist

- Ein kanonisches Workflow-Dokument mit stabilen Entitaets-/Relations-IDs, validierten
  Transaktionen und serialisierbaren Projektionen.
- TextEditor, BlockEditor und FlowEditor arbeiten auf derselben fachlichen Wahrheit; zentrale
  Roundtrip-, Rollback-, History- und Conformance-Pfade sind automatisiert getestet.
- Deterministische Ausfuehrungsplanung mit typisierten Ergebnissen sowie produktive Wait-, Touch-,
  Swipe- und MultiPointer-Kernpfade.
- Ein begrenzter, real auf einem Android-Geraet gepruefter Accessibility-`clickText`-Pfad mit
  deterministischer Zielaufloesung.
- Eine frei anordnbare Workspace-Shell mit Panels, Z-Order, Drag/Resize und Session-Persistenz.
- Recorder-Persistenz, Step Candidates, Review-Modelle, RailTrace, Playback und Recovery als
  produktive Teilkette.
- Plugin-Vertraege und Integrationsvorarbeiten fuer ShapeMaker, ChartGraph, Vision, Tasker,
  Shizuku, Termux, Custom Tabs, VT2VT und ein separates IME.

"Vorhanden" bedeutet nicht automatisch "Stable". Optionale Adapter und Companion-Projekte haben
eigene Reifegrade und koennen kontrolliert fehlen.

## Aktueller Feature-Stand

Die am C7-0-Audit-HEAD `711e13f` committete Feature-Matrix umfasst 146 Eintraege:

| Status | Anzahl |
| --- | ---: |
| `IMPLEMENTED` | 54 |
| `PARTIAL` | 44 |
| `PLANNED` | 30 |
| `DEFERRED` | 12 |
| `CONCEPT` | 5 |
| `BLOCKED` | 1 |

Vor der Uebernahme dieses Entwurfs als Root-README sind die Zahlen gegen den dann freigegebenen
Feature-Matrix-Commit neu zu pruefen; laufendes, uncommittetes M5-8-WIP ist hier nicht eingerechnet.

Details und Nachweise:

- [Feature Status Matrix](docs/FEATURE_STATUS_MATRIX.md)
- [Release 1.0 Scope](docs/architecture/RELEASE_1_0_SCOPE.md)
- [Release 1.0 Plan](docs/architecture/RELEASE_1_0_PLAN.md)
- [Roadmap](docs/ROADMAP.md)

## Was vor Stable 1.0 noch fehlt

Der aktuelle kritische Pfad ist:

`Provider-Policy -> Headless Runtime -> WindowSession -> Capture/Display Context -> Recorder-to-Workflow -> Project Portability -> Release Hardening`

Die wichtigsten offenen Gates sind:

1. einheitliche Provider-, Availability-, Permission- und Fallback-Policy,
2. prozessweiter Headless-/Foreground-Runtime-Host mit Stop, Recovery und UI-Reattachment,
3. gemeinsamer WindowSession-/Display-Vertrag fuer Shell und Overlay,
4. gemeinsamer CaptureFrame-/Display-Kontext fuer Aufnahme, Marker und Evidence,
5. geschlossener Record -> Review -> Proposal -> explizite Workflow-Freigabe-Pfad,
6. portables Projekt-/Ressourcenformat mit Backup/Restore,
7. Security-, Privacy-, Langlauf-, Device-Matrix- und reproduzierbares RC-Gate.

## Architektur in einem Bild

```text
                 TextEditor
                     |
BlockEditor -> WorkspaceDocument <- FlowEditor
                     |
             typed operations
                     |
                 Runtime
              /           \
        Providers       Trace/Records
                            |
                        Worldview
```

- **Workflow** ist Intent.
- **Worldview** ist beobachtete Realitaet und Wissen.
- **Runtime** fuehrt akzeptierten Intent aus und erzeugt Ergebnisse/Evidence.
- **Editoren** sind Projektionen und kontrollierte Mutationsoberflaechen.
- **AI/RAG** darf Kontext liefern und Aenderungen vorschlagen, aber nicht still Workflow oder
  Worldview mutieren.

Der verbindliche Vertrag steht in
[VISUALTASKER_ARCHITECTURE_CONTRACT.md](VISUALTASKER_ARCHITECTURE_CONTRACT.md).

## Repository-Struktur

| Pfad | Rolle |
| --- | --- |
| `app/` | Android Workspace-Shell und Runtime-Host |
| `visualtasker-blockeditor/` | BlockEditor und gemeinsame Workflow-/EMScript-Bausteine |
| `visualtasker-flowchart/` | FlowEditor, Layout, Routing und Validierung |
| `VisualTasker M3ShapeMaker Plugin/` | VisualAsset-/ShapeMaker-Integration |
| `VisualTasker ChartGraph Plugin/` | optionale ChartGraph-Komponenten |
| `VisualTasker Vision AI Plugin/` | Vision-Vertraege und optionale Modellprovider |
| `VisualTasker Integrations Plugin/` | Integrationsvertraege |
| `VisualTasker IME Keypad/` | separates IME-/Companion-Projekt |
| `docs/` | Architektur, Feature-Status, Tests, Roadmap und Referenzen |

Hinweis fuer Maintainer: Vor einer Veroeffentlichung dieses Entwurfs muessen alle im
Superprojekt referenzierten Submodule-Commits auf GitHub erreichbar sein.

## Voraussetzungen

- JDK 17
- Android Studio mit Android SDK 36
- Git mit Submodule-Unterstuetzung
- Android-Geraet oder Emulator fuer Android-spezifische Tests

## Klonen und bauen

```bash
git clone --recurse-submodules https://github.com/robertprit/VisualTasker-Studio-WSS.git
cd VisualTasker-Studio-WSS
./gradlew :app:assembleDebug
```

App- und Kernregressionen:

```bash
./gradlew \
  :visualtasker-blockeditor:blockeditor-registry:test \
  :visualtasker-blockeditor:blockeditor-compose:compileDebugKotlin \
  :app:testDebugUnitTest \
  :app:compileDebugKotlin \
  --console=plain
```

Diese Befehle pruefen Build und automatisierte Tests. Installation, Vordergrundstart, konkrete UI-
Zustaende und reale Runtime-Aktionen sind getrennte Abnahmegates.

## Manuelle Tests

- [Teststrategie](docs/testing/README.md)
- [Master Test Catalog](docs/testing/MASTER_TEST_CATALOG.md)
- [Core Smoke Test](docs/testing/00-CORE-SMOKE.md)
- [Smoke Test Matrix](docs/SMOKE_TEST_MATRIX.md)

## Berechtigungen und Sicherheit

Einige Automationspfade benoetigen Android Accessibility, Foreground Services, Screenshots oder
externe Apps. VisualTasker Studio soll fehlende Berechtigungen und Provider explizit
diagnostizieren. Es darf nicht still auf einen anderen Provider oder ein anderes Display
ausweichen.

Recorder-, Screenshot- und Evidence-Daten koennen sensible Inhalte enthalten. Vor einem
oeffentlichen Release fehlen noch die vollstaendige Privacy-/Log-Redaktionspruefung und die
verbindliche Geraete-/Langlaufmatrix.

## Dokumentation

- [Architecture Contract](VISUALTASKER_ARCHITECTURE_CONTRACT.md)
- [Worldview Architecture](docs/WORLDVIEW_ARCHITECTURE.md)
- [EMScript Truth Map](docs/emscript/EMSCRIPT_LANGUAGE_TRUTH_MAP.md)
- [Known Limitations](docs/KNOWN_LIMITATIONS.md)
- [Backup and Debug](docs/BACKUP_AND_DEBUG.md)

## Lizenz

Der WSS-Host steht unter der [Apache License 2.0](LICENSE). Abhaengigkeiten, optionale Provider,
Modelldateien und eingebettete Projekte koennen zusaetzlichen oder abweichenden Bedingungen
unterliegen; siehe [Third-Party Notices](THIRD_PARTY_NOTICES.md) und die Lizenzdateien der
jeweiligen Komponenten.

Dieser README-Entwurf wird erst nach Namens-, Lizenz-, Submodule- und Reifegradfreigabe zur
oeffentlichen Root-README.
