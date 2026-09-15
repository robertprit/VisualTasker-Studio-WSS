# WSS App

Das `app`-Modul ist der **native Android-Host von VisualTasker Studio WSS**. Hier werden Workspace, Panels und die Adapter zu den fachlichen Systemen zu einer ausführbaren Compose-Anwendung zusammengesetzt.

Es ist ausdrücklich **nicht** der Ort, an dem jeder Fachbereich seine eigene Wahrheit ablegt.

## Verantwortung

Das App-Modul darf unter anderem besitzen:

- Workspace- und Panel-Lifecycle;
- Compose-Navigation und Shell-Interaktion;
- Drag, Resize, Docking, Minimize und lokale Layoutzustände;
- Panel-Adapter und Projektionen;
- App-weite Composition/Dependency-Wiring;
- Android-spezifische Host-Integration;
- Session-/UI-Persistenz, soweit sie keine Domain-Wahrheit ersetzt.

## Nicht seine Verantwortung

Nicht als dauerhafte Wahrheit in Workspace-/Panel-State verschieben:

- Workflow-Semantik;
- EMScript-Semantik;
- Worldview-Entities oder Observations;
- Runtime-Ausführungswahrheit;
- Capability-Definitionen;
- AI-/ML-Ergebnisse als ungeprüfte Domain-Mutation.

Der zentrale Pfad bleibt sinngemäß:

```text
Workspace UI
    ↓
Panel Action
    ↓
Adapter / Domain Command
    ↓
Domain Model
    ↓
Projection
    ↓
Panel
```

## Editor-Integration

Blockeditor, Flowchart und EMScript sind unterschiedliche Bearbeitungs-/Darstellungsformen desselben Workflow-Intent. Der Host soll Synchronisation über gemeinsame Domain-Verträge ermöglichen, nicht durch direkte Editor-zu-Editor-Kopplung.

RailTrace ist dagegen eine Ablauf-/Execution-Projektion und keine vierte Workflow-Autorität.

## Worldview und Perception

Inspector-, Marker-, OCR-, Accessibility- und Vision-Oberflächen dürfen Rohdaten darstellen, aber UI-Modelle dürfen nicht still zu Worldview-Entities werden.

```text
Provider → Observation → Resolution/Fusion → WorldEntity → Scene → Worldview
```

## Integrierte Studio-Werkzeuge

ShapeMaker gehört als Zeichen-/Shape-/Animationswerkzeug zum Studio. Seine interne Modulgrenze darf unabhängig bleiben; Integration bedeutet nicht, dass seine Asset- oder Canvas-Zustände Workflow-/Worldview-Autorität erhalten.

Dasselbe Prinzip gilt für Dataset-, AI-, Perugger-, Recorder- und Debug-Werkzeuge.

## Entwicklung

Build vom Repository-Root:

```bash
./gradlew assembleDebug
```

Unit Tests:

```bash
./gradlew test
```

Für systemische Änderungen zusätzlich `../docs/testing/README.md` und den Master-Testkatalog verwenden.

## Architekturquellen

- `../VISUALTASKER_ARCHITECTURE_CONTRACT.md`
- `../docs/WORLDVIEW_ARCHITECTURE.md`
- `../docs/VISUAL_ABSTRACTION_LAYER.md`
- `../docs/reference/README.md`
- `../docs/testing/README.md`

Wenn Implementierung und Dokumentation widersprechen, den Widerspruch sichtbar machen. Nicht durch einen weiteren lokalen Sondervertrag kaschieren.
