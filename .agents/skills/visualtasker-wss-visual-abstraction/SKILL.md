---
name: visualtasker-wss-visual-abstraction
description: Guides Visual Abstraction Layer architecture in VisualTasker Studio WSS. Use for domain-to-visual semantic mapping, Material 3 Expressive policy, visual roles, badges, icons, shapes, outlines, motion, opacity, connector affordances, semantic color use, and styling boundaries.
---

# VisualTasker WSS Visual Abstraction

## Purpose

WSS translates domain semantics into consistent visual and interaction semantics.

```text
Domain / Projection
  -> Visual Semantics
  -> Visual Policy
  -> Material / Compose
```

## Invariants

- Visual semantics are not a theme.
- Expressive styling may support meaning, but must not be the only carrier of
  meaning.
- Color is never the only information channel.
- Gradient is not a semantic sensor value.
- Projection may change visual form, but not domain meaning.
- Invalid/error state wins over warning, runtime, and selection for outlines.
- AI proposal, ambiguity, warning, blocked, unavailable, and stale state require
  discrete readable signals.

## Current Source

Use `docs/VISUAL_ABSTRACTION_LAYER.md` as the current architecture reference.

## Related Skills

- Use editor-specific skills for concrete BlockEditor or Flowchart rendering.
- Use `visualtasker-wss-worldview` when visual state represents reality,
  resources, scenes, observations, or ambiguities.
