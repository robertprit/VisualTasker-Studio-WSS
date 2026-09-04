---
name: visualtasker-wss-worldview
description: Guides Worldview architecture work for VisualTasker Studio WSS. Use for Scene, Entity, Observation, Relation, Record, Resource, Ambiguity, Inspector, Data, Context, RAG boundaries, ScreenshotCanvas, Marker, Template, Vision, and Recorder features.
disable-model-invocation: true
---

# VisualTasker WSS Worldview

## Authority

- Worldview models reality/knowledge, not intended automation.
- Provider output is `Observation`, not `WorldEntity`.
- `Resource` is a persistent artifact, not an Observation or Entity.
- Recorder produces `Record` and Observations first, not accepted Workflow.
- AI produces `ProposedResolution`; it never mutates Workflow or Worldview directly.
- RAG/Context retrieves from structured Workflow/Worldview data; it is not a separate data world.

## Required Concepts

Start with:

- `WorldEntity`
- `Observation`
- `Relation`
- `Scene`
- `Event`
- `Step`
- `Record`
- `Resource`
- `Ambiguity`

Add optional concepts only when a vertical slice proves they are needed:

- `EntityConcept`
- `EntityInstance`
- `CoordinateSpace`
- `Interpretation`
- `ProposedIntent`
- `PerceptionQuestion`
- `ProposedResolution`

## Panel Questions

- Scene: What is here?
- Inspector: What do we know about it?
- Record: What happened?
- Marker: What did the user explicitly mark?
- Vision: How can we recognize it?
- Data: What persistent artifacts do we have?
- Context: What knowledge was retrieved and why?
- AI/Perugger: What does it mean and what must human and machine clarify?

## Implementation Rule

Build domain contracts and tests before broad UI. Prove changes with a vertical
slice: Observation -> Entity -> Scene -> Record -> WorkflowProposal -> Runtime
Record -> Ambiguity -> Human Resolution.
