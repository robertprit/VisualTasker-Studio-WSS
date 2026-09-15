# Capability Drift Matrix

Stand: 2026-09-15

Ziel dieser Matrix ist nicht, den Befehlskatalog neu zu entwerfen, sondern die
Stellen zu markieren, an denen Capability-Definition, UI-Katalog, EMScript,
RuntimeAdapter und Diagnose auseinanderlaufen koennen.

## Matrix

| Capability | Definition Source(s) | Consumers | Provider Path | Duplicate Definition? | Direct Dispatch? | Type Drift? | Authority / Risk Metadata? | Empfohlener Owner | Prioritaet |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Core `wait`, `log`, `beep`, `vibrate` | CommandCatalog, Parser, Runtime | BlockEditor, FlowEditor, TextEditor, RailTrace | Runtime intern / Device | Ja, verteilt | Teilweise | Niedrig | Teilweise | CommandCatalog + CapabilityDescriptor | Hoch |
| A11Y `click`, `clickText`, `touch`, `swipe` | CommandCatalog, Parser, RuntimeGate | BlockEditor, FlowEditor, Runtime, LogConsole | Accessibility/Device Adapter | Ja | Ja | Mittel | Teilweise | CapabilityRegistry | Hoch |
| Variablen `let`, `set`, Read/Write | CommandCatalog, Parser, Runtime | Alle Editoren, Runtime, RailTrace | WorkflowRuntime | Ja | Teilweise | Mittel | Gering | Workflow/CommandCatalog gemeinsam | Hoch |
| Branch/Loop Control | BlockRegistry, Parser, IR, FlowProjector | BlockEditor, FlowEditor, Runtime | WorkflowRuntime | Ja | Nein | Mittel | Gering | Workflow IR | Hoch |
| Reporter/Operator/Comparator | BlockRegistry, Parser, FlowProjector | BlockEditor, FlowEditor, Runtime | ExpressionEvaluator | Ja | Teilweise | Hoch | Gering | Expression/CommandCatalog | Hoch |
| Marker Save/Delete | CommandCatalog, WorkspaceScreen, MarkerPanel | Marker, Canvas, Datastore, FlowEditor, BlockEditor | Resource/Marker bridge | Ja | Teilweise | Hoch | Gering | Resource/Worldview Capability | Hoch |
| Template Define/Compare/Find | CommandCatalog, Vision/Marker Codegen | VisionPanel, MarkerPanel, Runtime | Vision Adapter | Ja | Ja | Hoch | Teilweise | Vision CapabilityDescriptor | Hoch |
| Screenshot / Canvas Capture | WorkspaceCanvas, CanvasPanel, Overlay | Canvas, Marker, Vision, RailTrace | Screen/Overlay Adapter | Ja | Teilweise | Hoch | Gering | Observation Capture Capability | Hoch |
| Clipboard/File/Cache | Parser/Runtime tests, RuntimeGate | TextEditor, Runtime, LogConsole | Android/Storage Adapter | Ja | Ja | Mittel | Teilweise | Runtime CapabilityRegistry | Mittel |
| CustomChromeTab / Browser | Settings, Runtime adapter, command path | Runtime, Settings, Text/Flow/Block | Browser Adapter | Ja | Ja | Mittel | Gering | Browser Plugin Contract | Mittel |
| Tasker | Settings, Runtime adapter, RailTrace event bridge | Runtime, RailTrace, LogConsole | Tasker Plugin API | Ja | Ja | Mittel | Teilweise | Tasker Plugin Contract | Mittel |
| Shizuku | Settings, RuntimeGate, adapter | Runtime, Settings, LogConsole | Shizuku UserService/Shell | Ja | Ja | Hoch | Teilweise | Shizuku Plugin Contract | Hoch |
| Termux | Settings, Runtime adapter | Runtime, Settings, LogConsole | RUN_COMMAND Intent | Ja | Ja | Mittel | Teilweise | Termux Plugin Contract | Mittel |
| scrcpy / VT2VT | VT2VT plugin path, Settings, runtime bridge | Overlay, Runtime, RailTrace | ADB/USB Bridge | Ja | Ja | Hoch | Gering | VT2VT Plugin Contract | Mittel |
| OCR | VisionPanel, Marker codegen, future commands | Vision, Canvas, Datastore, Worldview | Vision Adapter | Ja | Teilweise | Hoch | Gering | Vision CapabilityDescriptor | Mittel |
| OCV | VisionPanel, future commands | Vision, Canvas, Datastore | Vision Adapter | Ja | Teilweise | Hoch | Gering | Vision CapabilityDescriptor | Mittel |
| YOLO / Wisely | Vision concept/reference | Vision, Datastore, future training | Cloud/Local Vision Adapter | Noch unklar | Nein | Hoch | Gering | Vision/Training Plugin Contract | Niedrig |
| Charts | Roadmap/reference | Future Charts panel/runtime | Charts Adapter | Unklar | Nein | Mittel | MISSING | Charts Plugin Contract | Niedrig |
| AI / Junktor | ADR, Worldview, prompts | Inspector, Datastore, future RAG | Proposal pipeline | Teilweise | Nein | Hoch | Teilweise | AI Proposal Contract | Mittel |

## Befund

Der aktuelle Stand hat mehrere funktionierende Pfade. Der erste Repair-Schnitt
hat `CommandCapabilityDescriptor` als ableitbaren Katalog-Descriptor eingefuehrt;
RuntimeGate nutzt diesen Descriptor fuer `requiredAdapter`, `dryRunBehavior` und
`liveImplemented`. Noch offen ist die vollstaendige Nutzung desselben Descriptors
in Settings, Flow-/Block-Toolbox, Diagnostik und Adapter-Registrierung.

## Reparaturregel

Jede Capability nutzt den kanonischen Descriptor:

- `id`
- `canonicalName`
- `category`
- `syntax`
- `inputs`
- `outputs`
- `requiredAdapters`
- `dryRunBehavior`
- `runtimeBehavior`
- `diagnosticCodes`
- `uiHints`
- `sourceMappingPolicy`

Parser, BlockEditor, FlowEditor, TextEditor-Autocomplete, RuntimeGate,
LogConsole und RailTrace duerfen daraus ableiten, aber nicht jeweils eigene
Wahrheiten pflegen.

## Erledigter Repair-Slice

- `CommandCapabilityDescriptor` aus dem bestehenden CommandCatalog ableitbar.
- RuntimeGate bewertet Live-Faehigkeit ueber `requiredAdapter` und
  `liveImplemented` statt ueber eigene Runtime-Namenslisten.
- `touch` bleibt trotz A11Y-Capability blockiert, bis ein echter Live-Pfad
  implementiert ist.
- Katalog- und RuntimeGate-Tests sichern die neue Descriptor-Achse ab.
