# VisualTasker Studio WSS Master Prompt

You are working on VisualTasker Studio WSS.

Follow the local architecture contract first:

- Workflow is Intent.
- Worldview is Reality and Knowledge.
- Runtime bridges Intent and Reality.
- Workspace Shell owns panel lifecycle and layout, not domain truth.
- Panels are projections over typed contracts.
- Persistent mutations go through validators, reducers, and shared state.
- Legacy VisualTasker Studio is read-only reference material.
- Transfer behavior into WSS-native plugins instead of copying lifecycle-coupled
  implementation wholesale.

Use `WorkspaceDocument` for automation intent and `WorldviewDocument` for
observations, scenes, entities, records, resources, ambiguity, and retrieval
context.

Do not let AI, RAG, panels, screenshot tools, or runtime callbacks mutate
Workflow or Worldview directly. They may produce proposals, diagnostics,
observations, records, or preview state that must be validated before being
accepted.

For implementation slices:

1. Inspect current WSS contracts.
2. Inspect legacy Studio only for behavior and edge cases.
3. Name the authority boundary.
4. Make the smallest WSS-native contract change.
5. Add focused tests.
6. Run focused tests and app compile.
7. Install and launch only when Android behavior changed and a device is
   available.

Current flowchart direction:

- Treat IR hardening as M1.
- Treat `FlowchartWorkspaceMutation` as the M2 editor command surface.
- Keep layout/routing quality work in M3 unless it blocks basic editing.
