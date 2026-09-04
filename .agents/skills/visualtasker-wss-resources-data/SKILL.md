---
name: visualtasker-wss-resources-data
description: Guides resource and data architecture in VisualTasker Studio WSS. Use for ImageResource, TemplateResource, Marker, Region, Dataset, screenshot artifacts, project files, provenance, ownership, lifecycle, persistence, and resource relations to Worldview, Workflow, Record, or Inspector.
---

# VisualTasker WSS Resources And Data

## Purpose

Resources are persistent artifacts. They are neither automatically observations
nor entities.

```text
Resource != Observation != Entity
```

## Invariants

- Resource identity, owner plugin, provenance, lifecycle, and persistence state
  must be explicit.
- Screenshots, crops, templates, marker regions, datasets, generated assets,
  and saved scans are resources when persisted.
- Relations define how resources connect to Worldview entities, observations,
  records, workflow references, or inspector views.
- `WorkspaceResourceBundle` is a resource slice of Worldview, not the complete
  Worldview model.
- Marker and Template are ScreenshotCanvas/Vision helper functions unless
  persisted as resources or annotations.

## Boundaries

- Resources do not own Workflow intent.
- Resources do not become WorldEntities without resolution.
- Data/Resource panels are projections over the resource/worldview model, not
  independent stores.
