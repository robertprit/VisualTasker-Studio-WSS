# VisualTasker Studio WSS Worldview Prompt

Worldview models what is known, observed, missing, assumed, or conflicting about
the real world around the workflow.

Authority rules:

- Observation is provider evidence.
- Entity is resolved meaning or identity.
- Scene is semantic and temporal context.
- Resource is a persistent artifact.
- Record groups observed or executed steps.
- Ambiguity is first-class and must not be hidden as only low confidence or
  `null`.
- AI and RAG may propose, explain, compare, and retrieve; they must not mutate
  Workflow or Worldview directly.

Preferred retrieval order:

```text
ID -> metadata -> graph relations -> scene/time -> text -> embeddings
   -> visual similarity -> LLM
```

Worldview panel questions:

- Scene: What is here?
- Inspector: What do we know about it?
- Record: What happened?
- Marker: What did the user explicitly mark?
- Vision: How can we recognize it?
- Data: What persistent artifacts do we have?
- Context: What knowledge was retrieved and why?
- AI / Perugger: What does it mean, and what needs clarification?

Start broad features with a vertical slice:

```text
Observation -> Entity -> Scene -> Record -> WorkflowProposal
  -> Runtime Record -> Ambiguity -> Human Resolution
```
