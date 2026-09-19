# VisualTasker Plugin Ecosystem

## Objective

VisualTasker Studio WSS is the host shell. Editors, visual tooling, integrations
and perception providers remain independently maintainable components with
versioned capabilities. A plugin must not make its local UI state a second
workflow, record or worldview source of truth.

## Naming

`Plugin` is reserved for extensions developed specifically for VisualTasker.
Standalone applications with a VisualTasker bridge do not use the suffix.

| Product | Deployment | Canonical role |
| --- | --- | --- |
| VisualTasker M3ShapeMaker Plugin | library plus standalone app | Shape design and portable `.ema` assets |
| VisualTasker ChartGraph Plugin | embedded library plus demo app | charts and visual data graphs |
| VisualTasker IME Keypad | companion APK | system IME with VisualTasker bridge |
| VisualTasker Vision AI Plugin | provider libraries plus optional demo app | YOLO, ML Kit, OpenCV, Gemma, RAG and cloud AI |
| VisualTasker Integrations | multi-module repository | Tasker, Termux, Join, Custom Tabs, scrcpy, Ktor, VT2VT and Sherpa adapters |

The TextEditor remains an editor plugin. Accessibility remains a host/system
adapter and publishes neutral observations; it is not owned by the AI plugin.

## Plugin boundary

```text
portable contracts
       ^
plugin implementation
       v
WSS host adapter -> Workflow / Record / Worldview
```

Plugins expose descriptors, capabilities, data formats and runtime readiness.
The host owns panel framing, project persistence, global workflow commands and
cross-plugin transport.

## Canonical module layout

Every embedded plugin should converge on this structure:

```text
plugin-contracts       pure Kotlin contracts and serialization
plugin-domain          deterministic domain behavior
plugin-compose         reusable panel/UI surface
plugin-runtime         Android or external-service adapter
plugin-test-support    fixtures and host contract tests
demo-app               optional standalone development harness
```

Companion APK projects such as the IME expose a small contract module, but their
Android service remains out-of-process and independently installable.

## Development and release

- Local development uses Gradle composite builds and dependency substitution.
- Git projects are linked as submodules once their repository URLs are stable.
- Releases use versioned Maven artifacts for libraries and signed APKs for
  companion applications.
- The host accepts a plugin only when API version, capability version and data
  format versions are compatible.
- Unknown or unavailable plugins remain visible as a diagnosable missing
  capability rather than crashing the workspace.

## DnD

Compose DND is an Apache-2.0 third-party implementation detail. It owns pointer
gestures, reorder animation, edge auto-scroll and drop presentation. The WSS
contracts own payload identity, source/target policy, transfer mode, mutation,
undo and cross-panel effects. No third-party DnD object is serialized into a
VisualTasker project.

## Visual assets

`VisualAsset` is the neutral source of truth. SVG, Compose Path and Android
Shape are import/export or rendering formats. The embedded ShapeMaker panel
contains the DESIGN surface only; animation and sequencing remain in the
standalone ShapeMaker application. `.ema` stays the portable interchange format.

## Vision and AI

The Vision AI plugin publishes versioned observations and derived artifacts.
Providers remain separable:

```text
vision-contracts
vision-pipeline
vision-yolo
vision-mlkit
vision-opencv
vision-gemma
vision-rag
vision-cloud
vision-compose
```

Accessibility, OCR, OpenCV, YOLO, DOM and model output can meet in an
`ObservationDocument`, but provider-specific runtime, permissions, model cards
and diagnostics remain explicit.
