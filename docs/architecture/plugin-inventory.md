# Plugin Integration Inventory

## M3ShapeMaker

- Canonical checkout: `VisualTasker M3ShapeMaker Plugin/m3shapemake`
- Repository: `https://github.com/robertprit/M3ShapeMake.git`
- Embedded surface: `:design-editor`
- Standalone surface: `:app`
- Portable format: `application/vnd.emscript.motion+json` (`.ema`)
- The archived `ComposeCanvas` and `M3ShapeMake-sync` copies were removed after
  the canonical checkout passed standalone and host builds.

## ChartGraph

- Composite build: `VisualTasker ChartGraph Plugin`
- Embedded modules: `:chartgraph-domain`, `:chartgraph-compose`
- Standalone surface: `:app`
- WSS surface: `ChartGraph` panel
- Initial formats: line, bar, pie/donut and candlestick specifications
- Demo package: `com.visualtasker.chartgraph.demo`

## IME Keypad

- Deployment: separately installed Android input method
- Package: `com.visualtasker.ime`
- WSS built-in input-method service has been removed
- Build baseline: AGP 8.13.2, Kotlin 2.3.21, API 35
- VisualTasker communication must use explicit, permission-protected contracts

## Vision AI

- Canonical project: `VisualTasker Vision AI Plugin`
- YOLO camera activity is a demo, not the WSS integration surface
- Extracted modules: `:vision-contracts`, `:vision-yolo`
- Runtime owns model loading, preprocessing, inference and NMS
- WSS Vision supplies bitmaps/crops and consumes neutral observations
- WSS depends on `:vision-contracts` only; YOLO code and weights remain in the
  separately installed provider/demo application
- Model files require model cards and independent license records

## Compose DND

- Maven dependency: `com.mohamedrejeb.dnd:compose-dnd:0.5.0`
- Adapter: `WssComposeDndAdapter`
- Third-party types remain UI-local and are never serialized
- Existing WSS payload, drop-policy and undo contracts remain canonical

## Integrations

- Canonical project: `VisualTasker Integrations Plugin`
- Extracted module: `:integration-contracts`
- Stable IDs cover Tasker, Termux, Join, Custom Tabs, scrcpy, Ktor, VT2VT and Sherpa
- Existing host runtimes remain operational while implementations move behind
  these contracts in small, independently tested slices

## Repository transition

ChartGraph, IME and Vision AI stay local project folders until their module
boundaries and package identities compile independently. They become Git
repositories/submodules only after that point, avoiding repositories whose first
history consists mostly of structural churn.
