# ADR 0003: AI Proposals And Structured Results

Status: Accepted

## Context

VisualTasker Studio WSS will later use AI, ML, Vision, OCR, OCV, YOLO and
retrieval systems. These systems must not become a second application model
beside Workflow, Runtime, Record and Worldview.

The current release work still prioritizes editor stability, EMScript,
RailTrace, Recorder, Runtime, Worldview and persistence. AI integration is
therefore prepared only through clear boundaries and extension points.

## Decision

AI output is never a direct mutation of canonical state.

AI may create:

- observations
- predictions
- interpretations
- ambiguities
- diagnostics
- proposals
- explanations derived from structured results

AI must not directly change:

- `WorkspaceDocument`
- `WorkflowGraph`
- `WorldviewDocument`
- runtime state
- persisted recorder evidence
- accepted user-authored resources

The allowed path is:

```text
Input / Observation / Workflow Context
  -> AiTask
  -> Structured Result
  -> Proposal / Ambiguity / Diagnostic
  -> Preview
  -> Policy / Validator
  -> Human confirmation when required
  -> Domain Command
  -> Reducer
  -> Mutation
```

Chat is only one possible projection of structured AI results.

## Terminology

- `Observation`: raw or normalized evidence from a provider.
- `Prediction`: model output with confidence and provenance.
- `Interpretation`: meaning derived from evidence.
- `Proposal`: suggested change or action.
- `Mutation`: accepted domain command applied by a reducer.
- `Worldview`: what WSS currently believes about reality, not truth itself.

Therefore:

```text
Observation != Entity
Prediction != Observation
Prediction != Truth
Proposal != Mutation
Workflow != Runtime != Worldview
```

## Consequences

- AI results need stable references to source evidence, scenes, entities,
  runtime events, workflow nodes, blocks, text ranges and rail steps.
- Future AI UI should display structured work events, not hidden model
  reasoning.
- DnD involving AI results must still resolve to semantic domain commands.
- Dataset and training features must preserve provenance and human correction
  history.
- RAG remains read-only until Worldview, resources, records and inspector
  projections are stable.

## Do Not Build Yet

- no AI runtime subsystem
- no fine-tuning pipeline
- no YOLO training pipeline
- no chat-first AI panel
- no AI-specific parallel datastore
- no direct AI mutation path

## Near-Term Preparation

During stable-V1 work, only small neutral preparations are allowed:

- preserve stable IDs and provenance fields
- keep Workflow, Runtime, Record and Worldview separated
- route suggestions through existing validation and reducer paths
- document ambiguous naming and state boundaries
- extend tests where future AI use would expose current coupling bugs
