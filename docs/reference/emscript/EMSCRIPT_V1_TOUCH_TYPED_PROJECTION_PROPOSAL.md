# M1B-3H-B Typed Touch/Gesture Projection Proposal

Status: PROPOSAL ONLY
Prerequisite: M1B-3H-A structural inventory
Production implementation: none

## Decision

All three repository-backed `input.touch` payload forms remain legacy-preserved
in the first 3H-B implementation slice. None currently qualifies for lossless
canonical migration.

| Legacy form | 3H-B decision | Potential V1 model | Blocking evidence |
| --- | --- | --- | --- |
| `[x, y]` | PRESERVE_LEGACY | Point-backed atomic touch action only after an authoritative legacy contract exists | two numbers prove only a coordinate-like pair; they do not prove tap/down/up, timing or coordinate space |
| `["down", x, y, "up"]` | PRESERVE_LEGACY | typed atomic touch primitives | state-to-coordinate ownership, timing, pointer identity and coordinate space are unspecified |
| `"down(x1,y1);move(x2,y2);up(x3,y3)"` | PRESERVE_LEGACY | PointerPath only if the historic format's timing and pointer semantics become authoritative | ordered atomic states are present, but no timing, pointer ID or coordinate space exists |

No located form may migrate to `MultiPath`: there is no explicit parallel
pointer structure. No located form may migrate to `GestureSequence`: there are
no boundaries between complete gestures. No located form may migrate to
timeless `Path`, because doing so would erase the distinction between geometry
and gesture execution.

## Proposed Implementation Boundary

1. Keep `LegacyTouchStructuralClassifier` as the only raw-payload analyzer.
2. Add a separate migration service only after an authoritative format contract
   exists; do not add touch special cases to the general parser.
3. Require a migration rule to name one exact accepted structural signature,
   one exact V1 target type and every required evidence item.
4. Return `LegacyPreserved` whenever any required evidence is absent or
   ambiguous.
5. Preserve original source alongside any projected typed value until a
   roundtrip proves byte/text-equivalent legacy recovery or a formally accepted
   one-way migration policy exists.
6. Keep classification separate from migration action and runtime dispatch.

## Required Decisions Before Coding

- Establish the authoritative meaning of `[x, y]` from legacy documentation or
  executable reference behavior.
- Establish whether atomic `down/move/up` records permit absent timing and how
  event coordinates bind to states.
- Define a coordinate-space token and whether it is required in V1 values.
- Decide whether pointer identity is mandatory for one-pointer paths or may be
  an explicit `Unspecified` value. It must not silently become observed ID `0`.
- Decide whether timing can be explicitly unknown. It must not silently become
  zero or a runtime default during migration.
- Define whether the semicolon form is one trace or can contain several
  gestures. Current evidence supports only an ordered atomic trace.

## Proposed Acceptance Tests

- Every unsupported or incomplete fixture remains raw and emits a stable
  migration diagnostic.
- A future lossless fixture must include all evidence required by its selected
  target and roundtrip without invented values.
- Reclassification and migration planning are deterministic and idempotent.
- Alias spelling cannot change structural semantics.
- `Path`, `PointerPath`, `MultiPath` and `GestureSequence` remain distinct.
- Runtime dispatch remains a later, independent capability slice.

## Explicit Non-Goals

This proposal does not add source syntax, typed Workspace/IR values,
serializers, block shapes, runtime gesture dispatch, recorder integration,
Accessibility injection, Shizuku injection, IME behavior or provider commands.
