# RAG Read-only Index

## Status

Stable-V1 preparation. This is not an AI runtime and has no mutation authority.

## Boundary

The index is a deterministic projection of `WorldviewDocument` and
`WorkspaceResourceBundle`. It may expose retrieval candidates, source references,
freshness, confidence and verification state. It cannot mutate WorkflowDocument,
IRGraph, Worldview, resources, editor layouts or runtime state.

```text
Datastore resources + Worldview
             |
             v
ReadOnlyKnowledgeIndexProjector
             |
             v
ReadOnlyKnowledgeIndex.search(query)
             |
             v
retrieval hits with source refs and reason
```

Any future AI result remains a proposal. Acceptance must pass through Junktor,
validation and the existing domain reducers. Retrieval output never becomes a
fact or workflow mutation merely because it was returned by a search.

## Indexed sources

- scenes;
- entities;
- observations;
- workspace resources;
- recorded steps and records;
- ambiguities.

Every entry retains a stable source ID, source references, a verification state,
confidence and update time. Ambiguities remain conflicting or proposed; they are
not flattened into verified facts.

## Stable-V1 non-goals

- embeddings or vector database;
- network retrieval;
- chat UI;
- automatic workflow generation;
- direct mutation from retrieved context;
- training or fine-tuning pipelines.
