# VisualTasker Studio WSS

**VisualTasker Studio** ist eine native Android-Entwicklungs- und Automationsumgebung für visuelle Workflows, Geräteinteraktion, Wahrnehmung und lokale KI.

Das Projekt verbindet mehrere Arten, dieselbe Automation zu verstehen und zu bearbeiten: **EMScript**, einen nativen **Blockeditor**, einen **Flowchart**, **RailTrace** für Ablauf und Ausführung sowie eine modulare Workspace-Oberfläche mit Werkzeugen für Runtime, Recording, Accessibility, OCR, Computer Vision, Worldview, Daten und KI.

WSS steht für die neue **Workspace-Shell- und Systemarchitektur** des Studios. Aus einem Panel-Prototyp ist dabei schrittweise die gemeinsame Oberfläche geworden, in der die ehemals getrennten Werkzeuge als Projektionen derselben fachlichen Modelle zusammenarbeiten.

> Ich wollte ursprünglich Tasker einfacher machen und habe dabei versehentlich ein Labor dafür gebaut, wie Automationen erstellt, ausgeführt, beobachtet, verstanden und verbessert werden können.

## Motivation

VisualTasker Studio entstand aus der langjährigen Nutzung von **Tasker** und dem Wunsch, dessen enorme Funktionalität nicht nur zu benutzen, sondern zu verstehen, sichtbar zu machen und leichter mit anderen Formen der Automation zu verbinden.

Die erste Idee war vergleichsweise überschaubar: visuelle Erkennung auf dem Smartphone mit Automation kombinieren, lokale Modelle verwenden und dafür eine angenehmere Entwicklungsoberfläche bauen.

Daraus wurden EMScript, Blockeditor und Flowchart. Dann kamen Recorder, Inspector, Accessibility, OCR, OpenCV, Runtime-Debugging und lokale KI hinzu. Spätestens an diesem Punkt war das Problem nicht mehr, noch eine Funktion einzubauen, sondern dafür zu sorgen, dass alle diese Werkzeuge **über dieselbe Automation und dieselbe beobachtete Welt sprechen**.

WSS ist die Antwort auf dieses Problem.

Das Projekt verfolgt weiterhin nicht das Ziel, Tasker einfach zu ersetzen. Tasker ist ein wichtiger Integrations- und Capability-Provider. VisualTasker Studio geht jedoch inzwischen darüber hinaus: Es soll eine Umgebung sein, in der Automationen **erstellt, projiziert, ausgeführt, beobachtet, erklärt, korrigiert und später auch aus Erfahrung verbessert** werden können.

## Die Grundidee

Eine Automation soll nicht an die Darstellungsform gebunden sein, in der sie erstellt wurde.

```text
                     WorkflowDocument
                       Intent-Wahrheit
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
       EMScript          Blockeditor        Flowchart
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                           Runtime
                             │
                ┌────────────┴────────────┐
                │                         │
             RailTrace                Worldview
          Ablauf / Historie      beobachtete Realität
```

EMScript, Blocks und Flow sind keine konkurrierenden Wahrheiten. Sie sind unterschiedliche Projektionen desselben Workflow-Modells.

RailTrace beantwortet eine andere Frage: **Was ist beziehungsweise war im Ablauf tatsächlich relevant?**

Worldview beantwortet: **Was hat das System beobachtet, interpretiert, erkannt oder ist noch unsicher?**

Die Kurzform des Architekturvertrags lautet:

```text
Workflow = Intent
Worldview = Reality / Knowledge about Reality
Runtime = koppelt Intent und Reality
```

Daraus folgen einige zentrale Regeln:

- `Observed != Interpreted != Intended`
- `Resource != Observation != Entity`
- `Prediction != Observation`
- UI-Projektionen besitzen keine eigene dauerhafte fachliche Wahrheit.
- KI darf Ergebnisse und Änderungen vorschlagen, aber Workflow oder Worldview nicht still mutieren.

Der normative Vertrag liegt in [`VISUALTASKER_ARCHITECTURE_CONTRACT.md`](VISUALTASKER_ARCHITECTURE_CONTRACT.md).

## Vier Sichtweisen auf denselben Workflow

### EMScript

EMScript ist die textuelle Sprache des Studios. Sie dient nicht nur als Exportformat, sondern als bearbeitbare Projektion mit Parser, Import, Validierung, semantischem Diff und kontrolliertem Apply in das Workflow-Modell.

### Blockeditor

Der native Jetpack-Compose-Blockeditor bietet Blockly-artiges visuelles Programmieren ohne WebView. Dazu gehören typisierte Value-/Statement-Verbindungen, Reporter, Variablen, Operatoren, Kontrollstrukturen, Drag/Snap, Undo/Redo, Validierung und Serialisierung.

### Flowchart

Der Flowchart zeigt Struktur, Beziehungen, Verzweigungen und Übergänge. Er entwickelt sich vom früheren reinen Flow-Viewer zu einer echten Workflow-Projektion mit gemeinsamem semantischem Unterbau.

### RailTrace

RailTrace ist die zeitliche und kausale Sicht. Es verbindet Programmstruktur, DryRun, Live-Ausführung, Recording und Replay, ohne ExecutionTrace, Record und Workflow miteinander zu verwechseln.

Kurz gesagt:

```text
Blocks zeigen Struktur.
Flow zeigt Zusammenhang.
RailTrace zeigt Verlauf.
EMScript zeigt die textuelle Form.
```

## Wahrnehmung und Worldview

Accessibility, OCR, OpenCV, YOLO, DOM, Screenshots, Marker und Templates sollen nicht jeweils ihre eigene Vorstellung von der Welt besitzen.

Der gemeinsame Pfad ist:

```text
Provider
   ↓
Processor
   ↓
Observation
   ↓
Evidence Fusion / Entity Resolution
   ↓
WorldEntity
   ↓
Scene
   ↓
Worldview
```

Eine OCR-Erkennung mit dem Text `Login` ist noch kein kanonischer `LoginButton`. Ein Accessibility-Node ist nicht automatisch ein WorldEntity. Eine Prediction ist keine Observation.

Diese Trennung ist entscheidend für robuste visuelle Automation und für spätere lernende Systeme.

Siehe [`docs/WORLDVIEW_ARCHITECTURE.md`](docs/WORLDVIEW_ARCHITECTURE.md) und [`docs/reference/perception/VISION_PIPELINE.md`](docs/reference/perception/VISION_PIPELINE.md).

## Runtime und Capability Provider

VisualTasker Studio soll Automationsfunktionen nicht an einen einzigen Ausführungsweg koppeln.

Das Zielmodell ist:

```text
Workflow / EMScript / AI
          ↓
      Capability
          ↓
    ProviderResolver
          ↓
 ┌────────┼─────────┬──────────┬───────────┐
Native   A11y     Tasker     Browser     weitere Provider
```

Tasker bleibt damit ein wichtiger Bestandteil des Ökosystems, aber nicht die Definition der Runtime selbst. Native Android-Funktionen, Accessibility, Browser, Shizuku, externe Geräte oder später andere VisualTasker-Instanzen können über denselben Capability-Gedanken eingebunden werden.

## KI, ML und Lernen

Lokale KI ist kein nachträglich angeklebtes Chatfenster. Langfristig soll sie innerhalb der fachlichen Werkzeuge arbeiten und ihre Ergebnisse dort sichtbar machen, wo sie entstehen.

Beispiele sind:

- Workflow- oder EMScript-Vorschläge;
- Diagnose von Ausführungsproblemen;
- Entity- und Scene-Interpretation;
- Recovery-Vorschläge;
- strukturierte Ambiguitäten und Rückfragen;
- Function/Capability-Mapping durch kleine Spezialmodelle;
- klassische ML-Modelle für Entity Resolution oder Outcome Prediction;
- YOLO-Modelle für Wahrnehmung;
- lokale größere Modelle für Interpretation, Planung und Generierung.

Die wichtige Grenze bleibt:

```text
AI Result != Domain Command
Prediction != Observation
Training completion != Deployment approval
```

Normale Nutzung kann später über **ökologisches Recording** Lernkandidaten erzeugen. Diese werden jedoch kuratiert, evaluiert und versioniert, bevor daraus Training oder ein neues aktives Modell entsteht.

Die nächste Konsolidierungsrunde für Intelligence, Training, Dataset Lifecycle, Model Registry und Evaluation ist in [`docs/audit/NEXT_ARCHITECTURE_AUDIT_BRIEFING.md`](docs/audit/NEXT_ARCHITECTURE_AUDIT_BRIEFING.md) vorbereitet.

## VAL und ShapeMaker

Die **Visual Abstraction Layer (VAL)** trennt fachliche Bedeutung von ihrer konkreten Darstellung.

```text
Domain
  ↓
Projection
  ↓
Visual Semantics
  ↓
Visual Policy
  ↓
Material / Compose
```

Damit können Zustand, Autorität, Unsicherheit, Aktivität oder Beziehungen konsistent in Blockeditor, Flowchart, RailTrace, Inspector und weiteren Panels dargestellt werden.

**ShapeMaker gehört als integriertes Zeichen-, Shape- und Animationswerkzeug zum Studio.** Es soll wiederverwendbare Visual Assets für Block-/Node-Formen, Overlays, Indikatoren und programmierbare visuelle Erscheinungsformen erzeugen. ShapeMaker erstellt die visuellen Ressourcen; VAL entscheidet über deren semantische Verwendung.

Siehe [`docs/VISUAL_ABSTRACTION_LAYER.md`](docs/VISUAL_ABSTRACTION_LAYER.md).

## Junktor

`Junktor` ist der kanonische Begriff für explizite semantische beziehungsweise Evidence-Relationen im Interpretationsmodell. Der Junktor ist kein EntityResolver und besitzt keine eigene Mutationsautorität; er beschreibt Beziehungen, mit denen Evidenz, Alternativen oder gelernte Zusammenhänge nachvollziehbar verbunden werden können.

Die konkrete Modellierung wird im nächsten Architektur-Audit weiter konsolidiert.

## Workspace

WSS stellt die Werkzeuge als frei organisierbare native Compose-Panels bereit. Der Workspace verwaltet dabei **Darstellung und Interaktion**, nicht die fachliche Wahrheit.

Bereits vorhanden beziehungsweise in aktiver Entwicklung sind unter anderem:

- Multi-Panel-Workspace mit Drag, Resize, Minimize und Close;
- Docking, Raster und Auto-Arrange-Grundlagen;
- Blockeditor;
- Flowchart;
- EMScript-Editor;
- RailTrace;
- Recorder / Record;
- LogConsole und Debug-Informationen;
- Inspector, Marker, Vision und Canvas;
- Dataset-/Resource-Werkzeuge;
- Runtime- und Browser-Integration;
- AI-/Human-Machine-Werkzeuge;
- Settings und Plugin-/Provider-Infrastruktur.

## Projektstatus

VisualTasker Studio WSS befindet sich in **aktiver Entwicklung und Architektur-Konsolidierung**.

Mehrere Kernsysteme sind bereits als funktionierende Prototypen oder produktive Slices vorhanden. Andere Bereiche — insbesondere vollständiger Editor-Roundtrip, Worldview/Perception-Konsolidierung, Capability/Provider-Verträge, Intelligence/Learning und einige Runtime-/Overlay-Funktionen — werden derzeit systematisch gegen die neue Architektur geprüft.

Der Anspruch der Dokumentation ist deshalb ausdrücklich, zwischen **implementiert**, **partiell**, **geplant** und **historisch/legacy** zu unterscheiden, statt geplante Architektur als bereits fertige Funktion darzustellen.

Aktuelle Planung: [`docs/ROADMAP.md`](docs/ROADMAP.md)

## Dokumentation

Wichtige Einstiegspunkte:

- [`VISUALTASKER_ARCHITECTURE_CONTRACT.md`](VISUALTASKER_ARCHITECTURE_CONTRACT.md) — oberste Architekturregeln
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — Entwicklungsstand und Meilensteine
- [`docs/WORLDVIEW_ARCHITECTURE.md`](docs/WORLDVIEW_ARCHITECTURE.md) — Worldview-Modell
- [`docs/VISUAL_ABSTRACTION_LAYER.md`](docs/VISUAL_ABSTRACTION_LAYER.md) — visuelle Abstraktion
- [`docs/reference/README.md`](docs/reference/README.md) — konsolidierte Referenzverträge
- [`docs/testing/README.md`](docs/testing/README.md) — Teststrategie
- [`docs/testing/MASTER_TEST_CATALOG.md`](docs/testing/MASTER_TEST_CATALOG.md) — manueller Systemtest-Katalog
- [`docs/audit/NEXT_ARCHITECTURE_AUDIT_BRIEFING.md`](docs/audit/NEXT_ARCHITECTURE_AUDIT_BRIEFING.md) — nächste Architektur-/Learning-Konsolidierung

## Entwicklung

### Tech-Stack

- Kotlin
- Jetpack Compose
- Android Gradle Plugin / Kotlin DSL
- Java/Kotlin 17
- minSdk 29
- targetSdk 35
- compileSdk 35

### Build

```bash
./gradlew assembleDebug
```

Installation:

```bash
./gradlew installDebug
```

Tests:

```bash
./gradlew test
```

Für Änderungen an Workflow, Editor-Synchronisation, Runtime, Recording oder Worldview sollten zusätzlich die manuellen Systemtests unter `docs/testing/` berücksichtigt werden.

## Architektur statt Monolith

VisualTasker Studio besteht aus vielen Werkzeugen, soll aber nicht wieder zu einem einzigen untrennbaren Studio-Monolithen werden.

Die Module dürfen spezialisierte Zustände besitzen — Layout, Selection, Drafts, Zoom, temporäre UI-Zustände — aber fachliche Wahrheit wird über explizite Contracts ausgetauscht.

Das gilt auch für integrierte Werkzeuge wie ShapeMaker: **integriert bedeutet nicht untrennbar gekoppelt**.

So soll das Studio gleichzeitig experimentierbar bleiben und trotzdem verhindern, dass jeder neue Editor, jedes Modell oder jeder Provider heimlich seine eigene Realität erfindet.

## Woher das Projekt kommt

VisualTasker Studio ist kein Projekt, das von Anfang an nach einem fertigen Architekturdiagramm gebaut wurde.

Es entstand durch Experimentieren: Tasker, Accessibility, visuelle Erkennung, eigene Skriptsprache, Blockprogrammierung, Flowcharts, lokale LLMs und immer wieder die Frage, ob sich zwei Dinge miteinander verbinden lassen, die ursprünglich nicht füreinander gedacht waren.

Das hat zwischenzeitlich auch genau das erzeugt, was man erwarten würde: funktionierende Ideen, die sich gegenseitig kaum noch erklären konnten.

WSS ist deshalb nicht einfach ein UI-Redesign. Es ist der Versuch, diese Experimente in ein System mit klaren Begriffen, Grenzen und gemeinsamen Wahrheiten zu überführen, **ohne den experimentellen Charakter zu verlieren, aus dem das Projekt überhaupt entstanden ist**.

Oder kürzer:

> Erst wollte ich eine angenehmere Automation bauen. Dann wollte ich verstehen, warum sie funktioniert. Inzwischen baue ich das Werkzeug, mit dem ich beides gleichzeitig tun kann.

## Lizenz

Siehe `LICENSE`, sofern im jeweiligen Modul/Repository vorhanden.
