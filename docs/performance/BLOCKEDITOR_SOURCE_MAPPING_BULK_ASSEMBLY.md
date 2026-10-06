# BlockEditor Source Mapping and Bulk Assembly

## Scope

This closes the P2C performance slice for the measured LARGE BlockEditor drop
stall. It does not change drag, insertion, snap, ghost, or placement-animation
UX. P2A affected-structure layout remains in place.

## P2B Root Cause

P2B isolated the dominant main-thread cost after a semantic drop in the host's
source-line mapping. The host derived source lines independently for the current
EMScript projection and the visible editor draft. Each derivation parsed the
source and rebuilt a temporary `WorkspaceDocument` by dispatching every import
operation through the interactive `WorkspaceReducer`.

On the 181-block LARGE fixture this caused two sequential assemblies:

| Derived source | Reducer calls | Assembly duration (SM-S918B detail trace) |
| --- | ---: | ---: |
| Projected EMScript | 696 | 2,835.7 ms |
| Visible draft | 706 | 2,581.4 ms |

The complete source-line phase occupied about 5,456 ms in that detailed trace.
The earlier standard P2B runs measured about 3,590-3,690 ms for the same
dominant phase, depending on device and trace overhead.

## Source Mapping Reuse Contract

`WorkspaceSelectionResolver.SourceMappingCache` owns a small, screen-local LRU
of imported source documents. Its semantic key is the exact EMScript source
text. This is deliberate: whitespace and line breaks affect source locations,
so a normalized or hash-only key would be insufficient for source mapping.

A cache hit is allowed when the exact source text is unchanged. Presentation
state is not part of the key. Moving a root, changing selection, panning,
zooming, hovering, dragging, snapping, or animating therefore does not trigger
another import.

A cache miss occurs when the exact source changes or when a new screen-local
cache is created. The bounded LRU also evicts old entries. The cache contains
only the source-derived document; `WorkspaceIdentityReconciler` still runs on
every lookup so source lines are mapped to the stable IDs of the current
`WorkspaceDocument`.

The projected source and visible draft use the same cache. If they contain the
same exact source, the first lookup imports once and the second lookup reuses
it. If they differ, each source has one independent cache entry.

## Bulk Assembly Contract

`EmscriptWorkspaceImporter` now defaults to `WorkspaceAssemblyMode.BULK`. The
legacy sequential reducer path remains available as the semantic test oracle.

The bulk assembler consumes the same ordered import actions but applies the
four actions emitted by this importer directly while constructing one local
document:

- instantiate a block;
- update a field;
- create a variable;
- connect two compatible importer-owned connections.

Connections are written symmetrically and connected children/reporters are
removed from roots exactly as in the legacy reducer result. The finished
document is returned once. Interactive history, callbacks, validation events,
and intermediate publication are intentionally absent because an import is one
transaction, not hundreds of user operations.

Assembly metrics expose mode, reducer calls, constructed objects, action counts,
duration, and publication count. The LARGE device trace after P2C shows zero
reducer calls and one result publication.

## Semantic Equivalence

Tests run the legacy sequential and bulk assemblers over both a focused nested
fixture and the LARGE integration script. After stable-ID reconciliation they
compare the complete `WorkspaceDocument` except its monotonic version. This
covers root ordering and positions, fields, variables, connections, reporter
inputs, nested expressions, IF/ELSEIF/ELSE, loops, source metadata, and facets.

The focused fixture additionally compares generated EMScript and normalized IR.
Existing repository tests continue to cover parsing, generation, roundtrip,
trace/source focus, diagnostics, and undo/redo contracts.

## LARGE Before and After

Same-device detailed linear-detach traces on SM-S918B:

| Metric | P2B before | P2C after |
| --- | ---: | ---: |
| Source-line phase | ~5,456.0 ms | ~17.7 ms |
| Assembler runs during drop | 2 | 1 |
| Reducer calls | 696 + 706 | 0 |
| Measured assembly | 2,835.7 + 2,581.4 ms | 1.38 ms |
| Drop to interactive frame | 6,065.4 ms | 583.1 ms |
| Drop to stable | 6,386.7 ms | 876.2 ms |
| Largest recorded frame gap | 106.1 ms | 102.7 ms |

The P2C drop changed projected EMScript, so it correctly caused one cache miss
and one bulk assembly. The unchanged draft was a cache hit. A presentation-only
change is covered by the cache contract test and performs no re-import.

P2A remained active for the measured detach:

- `measuredBlocks=0`
- `fullBuilds=0`
- `reusedBlocks=181`
- `path=RIGID_CHAIN_RELINK`

The observed drop no longer freezes for several seconds. Geometry, selection,
source mapping, snap, hit testing, and rendering remained operational in the
device smoke test. SM-S938B was not connected for the P2C rerun; the detailed
comparison was therefore performed on SM-S918B.

## Remaining Hotspots

P2C removed the dominant source/import CPU stall. The largest remaining
measured work is now affected layout plus the existing placement-animation and
frame-settling path. The measured detach spent about 136 ms in layout and
settled after about 876 ms total, with frame gaps around 71-103 ms.

Those values are no longer caused by source mapping or sequential assembly.
They belong to the separate BlockEditor Drag/Insertion Interaction Contract.
That follow-up may replace the delayed placement behavior, but it is explicitly
outside P2C.
