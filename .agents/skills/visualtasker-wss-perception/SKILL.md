---
name: visualtasker-wss-perception
description: Guides perception-provider architecture in VisualTasker Studio WSS. Use for Accessibility, OCR, OpenCV, YOLO, DOM, screenshot capture, template matching, coordinate spaces, confidence, provider diagnostics, evidence creation, and observation mapping into Worldview.
---

# VisualTasker WSS Perception

## Purpose

Perception answers: how do we observe?

Worldview answers: what do those observations mean?

## Invariants

- Provider output is `Observation` or `Evidence`, not canonical `WorldEntity`.
- Accessibility trees, OCR text, OpenCV matches, YOLO detections, DOM nodes,
  screenshots, and templates are provider-specific views.
- Provider-specific labels must not become canonical entity types without
  resolution through Worldview.
- Coordinate spaces must be explicit: screen, screenshot, crop, node-local,
  window, DOM, or model space.
- Confidence, timestamp, source provider, and provenance must be preserved.
- Unknown, unavailable, denied, stale, conflicting, and low-confidence states
  are first-class diagnostics.

## Boundaries

- Perception does not own Workflow intent.
- Perception does not execute workflow actions.
- Perception does not silently resolve identity; it supplies evidence for
  Worldview.
- Template and marker helpers work through ScreenshotCanvas/resource contracts.

## Related Skills

- Use `visualtasker-wss-worldview` when observations become entities, scenes,
  relations, resources, records, or ambiguities.
- Use `visualtasker-wss-runtime-capabilities` when perception requires
  permissions, providers, fallback, retries, or live execution.
