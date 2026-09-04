---
name: visualtasker-wss-perugger-context
description: Guides Human-Machine Perception Debugger context architecture in VisualTasker Studio WSS. Use for ambiguity handling, interactive grounding, retrieval, prompt/context builders, model routing, AI explanations, proposed resolutions, RAG boundaries, and human approval paths.
---

# VisualTasker WSS Perugger Context

## Purpose

Perugger is the Human-Machine Perception Debugger function. It coordinates
ambiguity, retrieval, explanation, clarification, and proposal creation.

LLM is a provider. Perugger is the product function and context pipeline.

## Flow

```text
Worldview
+ Workflow
+ Record
+ Runtime
+ Ambiguity
  -> Retrieval
  -> ContextBuilder
  -> PromptBuilder
  -> Model Provider
  -> Explanation / Clarification / ProposedResolution
  -> User / Validator / Command
```

## Invariants

- AI may explain, interpret, compare, ask, suggest, generate proposals, and
  produce diffs.
- AI must not silently mutate Workflow or Worldview.
- Retrieval is not a separate truth model.
- Every proposed resolution should carry enough source context for inspection.
- Ambiguity is first-class; do not hide it as low confidence only.

## Defer RAG

RAG belongs after stable Workflow, Worldview, Resources, Records, Inspector, and
runtime traces. Until then, keep context explicit and inspectable.
