# M2-1 Canonical Workflow Contract Extraction

Status: **READY**

Date: 2026-10-04

Scope: ownership extraction only. No workflow redesign, new language semantics, new IR, routing, editor feature, or persistence format.

## 1. Result

Before M2-1, the effective workflow truth was
`de.visualtasker.blockeditor.domain.WorkspaceDocument` in
`blockeditor-domain`. Persistence, importer, IR, runtime, and all editor
projections therefore depended on a model owned by the optional BlockEditor.

The canonical contract now lives in the neutral JVM module:

```text
de.visualtasker.workflow:workflow-core:0.1.0-SNAPSHOT
```

The concrete canonical document remains
`de.visualtasker.workflow.core.WorkspaceDocument`. Keeping the established
type name avoids an unnecessary format and call-site migration. There is no
second workflow model and no Core-to-Block mapper.

The module is independently addressable and contains no BlockEditor,
FlowEditor, Compose, Android graphics, renderer, or gesture dependency. It is
currently stored in the BlockEditor composite repository because that is the
existing composite-build boundary; semantic ownership belongs to the neutral
module and package, not to `blockeditor-domain`. Moving the module to a
separate repository is optional repository packaging work, not a semantic
migration.

## 2. Extracted Contracts

The following existing contracts were moved without semantic redesign:

- `WorkspaceDocument`, `BlockNode`, `BlockId`, and `WorkspacePoint`
- variables, scopes, field values, value inputs, and statement inputs
- connection IDs, kinds, endpoints, accepted/provided types, and slot names
- ordered roots and existing graph traversal helpers
- operator normalization
- current workspace actions and reducer behavior
- canonical schema-v1 serializer, compatibility diagnostics, and normalizer

`blockeditor-domain` now contains BlockEditor-specific `Rect`,
`WorkspaceState`, and `WorkspaceHistory`, and consumes `workflow-core`.
`blockeditor-serialization` retains only a thin compatibility facade that
delegates to `WorkflowSerializer` and adapts registry diagnostics. It stores no
shadow document and performs no model synchronization.

## 3. Presentation Compatibility

M2-1 does not declare all fields in the historical payload to be semantic.
`rootPositions`, `collapsed`, and mixed metadata remain in the neutral data
contract only because existing projects must round-trip losslessly. Their
separation into editor view documents is deliberately deferred. Viewport,
selection, hover, drag, hit testing, rendering, shapes, colors, and Compose
state were not extracted.

## 4. Dependency Direction

```text
emscript-language-core
          |
          v
     workflow-core
       /   |   \
      v    v    v
BlockEditor  WSS importer/persistence/runtime  projections
```

- Root WSS depends directly on `workflow-core`.
- BlockEditor domain, registry, serializer, validation, EMScript, IR, layout,
  interaction, and Compose modules consume the neutral types.
- The IR generators accept the neutral `WorkspaceDocument` directly.
- WSS importers and runtimes accept or produce the neutral document directly.
- Flow projection paths in WSS consume the neutral document/derived IR. The
  generic FlowEditor repository required no source change.
- A source-level architecture test rejects imports from BlockEditor,
  FlowEditor, Compose, and Android graphics in `workflow-core`.

There is no dependency cycle. `workflow-core` depends only on the language
core plus Kotlin serialization.

## 5. Persistence Compatibility

The schema version remains `1`. DTO names, fields, default handling, ordering,
normalization, reciprocal connection representation, IDs, variables,
`rootBlocks`, `rootPositions`, collapsed state, and metadata are unchanged.

`WorkflowSerializer` is now the canonical serializer. The old
`WorkspaceSerializer` delegates to it for source compatibility and adds only
BlockRegistry-backed definition diagnostics. Root persistence and the shell
session use `WorkflowSerializer` directly. A schema-less historical JSON
fixture loads into the neutral model, saves as schema 1, reloads semantically
equal, retains its IDs, and serializes byte-stably after normalization.

## 6. Removal Probe

| Consumer | BlockEditor-owned workflow-model dependency | Remaining dependency |
|---|---|---|
| Persistence | **REMOVED** | compatibility facade is `COMPATIBILITY_ONLY` |
| Runtime | **REMOVED** | BlockEditor registry/IR implementation remains a `REAL_DEPENDENCY` |
| EMScript importer | **REMOVED** | command catalog, block definitions, and type services remain a `REAL_DEPENDENCY` |
| IR generator | **REMOVED** | generator implementation and registry/catalog remain in BlockEditor composite |
| BlockEditor | consumer of `workflow-core` | UI, history, layout, interaction, registry, and projection remain its responsibility |
| FlowEditor | no canonical-model ownership | generic view/editor modules unchanged; WSS adapter consumes neutral model/IR |

Runtime is therefore **partly editor-independent**: execution paths no longer
depend on a BlockEditor-owned workflow model, but still use registry and IR
implementations physically supplied by the BlockEditor composite.

## 7. Semantic and Compatibility Evidence

The neutral core fixture covers the currently supported forms requested by
M2-1: linear commands, LET and SET data, nested arithmetic, comparison,
IF/ELSEIF/ELSE (including IF and IF/ELSE shapes), WHILE, REPEAT, command
reporters, nullable domain values, `List<T>`, variable identity, root order,
and reciprocal value connections.

Existing test suites additionally retain the real parser/importer/generator,
three-editor projection, runtime, validation, and EMScript roundtrip coverage.
No IDs are regenerated by this extraction, and no connection representation,
root order, branch slot, command spelling, type, or runtime behavior changed.

Verification on 2026-10-04:

- `workflow-core:test`: 3/3 green
- focused serializer, registry, IR, EMScript, and validation tests: green
- complete BlockEditor `test`: green
- complete FlowEditor `test`: green
- WSS `:app:testDebugUnitTest`: 596/596 green
- WSS `:app:compileDebugKotlin`: green
- WSS `:app:assembleDebug`: green
- Root, BlockEditor, and FlowEditor diff checks: green

## 8. Device Evidence

Clean-state gate ran on Samsung SM-S918B (`R3CW5032EGH`):

1. debug APK installed;
2. `com.visualtasker.wss` app data cleared;
3. fresh start reached `MainActivity` and Workspace;
4. RailTrace, BlockEditor, and Flowchart were present on the fresh workspace;
5. TextEditor opened and displayed the generated EMScript fixture;
6. editor panel focus and return to Workspace completed;
7. no crash, ANR, process death, or fresh fatal AndroidRuntime exception was
   observed.

The cleared-data cold start became visible after about 15.3 seconds while the
start command timed out at 10.5 seconds. This is a startup-performance
observation, not an ANR or M2-1 functional failure.

## 9. Post-M2-1 Audit

| Metric | Before | After | Explanation |
|---|---:|---:|---|
| `DUPLICATED_TRUTH` | 9 | 9 | Ownership extraction intentionally did not redesign identities, relations, roots, branches, source maps, or projections. |
| `MISSING_CONTRACT` | 16 | 13 | Neutral canonical document, neutral stable block/node ID contract, and neutral reducer/action contract now exist. Expression IDs, branch IDs, neutral edges, full V1 constructs, extension envelopes, validation ownership, and project/view attachments remain open. |

The most important remaining ownership leak is the shared command catalog,
block/type compatibility logic, validation, and IR implementation still being
physically owned by BlockEditor modules. The neutral document also still
retains presentation-compatible fields that should eventually move to view
attachments.

## 10. Recommended M2-2 Slice

Extract shared command/type/validation and IR-facing contracts from the
BlockEditor composite while preserving behavior. Do not yet redesign
connections or add expression/branch identities. That is the smallest next
step that makes importer and runtime implementation dependencies genuinely
editor-independent without reopening the schema.
