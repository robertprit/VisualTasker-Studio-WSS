---
name: visualtasker-wss-runtime-capabilities
description: Guides runtime and capability architecture in VisualTasker Studio WSS. Use for dry-run/live-run execution, capability gates, provider selection, permissions, fallback, retries, timeout, cancellation, recovery, side effects, OperationResult categories, and runtime observability.
---

# VisualTasker WSS Runtime Capabilities

## Purpose

Runtime bridges accepted Workflow intent with real device/application state and
produces trace evidence, observations, and records.

```text
Workflow / VM
  -> Capability
  -> Provider Resolution
  -> Provider
  -> OperationResult
  -> Observation / Runtime Event / Record
```

## Invariants

- Editors do not execute device actions.
- Runtime execution is capability-gated.
- Dry-run must distinguish parser failure, validation failure, unsupported
  command, missing provider, denied permission, and real execution failure.
- Unsupported plugin-backed commands should produce explicit diagnostics without
  falsely marking unrelated core commands broken.
- Side effects must be explicit in command metadata and runtime binding.
- Runtime traces are overlays/projections until accepted as Records or
  Worldview Observations through reducers.

## Result Categories

Use stable categories where available:

- `SUCCESS`
- `NOT_FOUND`
- `TIMEOUT`
- `DENIED`
- `UNAVAILABLE`
- `FAILED`
- `CANCELLED`
- `UNCERTAIN`

## Related Capabilities

Tasker, Shizuku, Termux, Custom Tabs, scrcpy, charts, A11Y, screen capture,
vision, feedback, timing, logging, and debug should be represented as
capabilities/providers, not hardcoded editor behavior.
