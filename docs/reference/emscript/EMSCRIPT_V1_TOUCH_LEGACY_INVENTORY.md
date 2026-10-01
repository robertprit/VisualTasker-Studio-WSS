# EMScript v1 Legacy input.touch Inventory

Status: M1B-3H-A CLOSED / LEGACY_PRESERVE
Scope: structural classification only; no migration, rewrite or dispatch

Formal closeout:

- bridge status: `NORMALIZABLE`
- remaining issue group: `C_TYPE_CONFLICT`
- migration class: `STRUCTURAL_MIGRATION`
- compatibility decision: `LEGACY_PRESERVE`
- migration readiness: `BLOCKED_BY_LEGACY_SEMANTICS`

The slice is complete as an audit. It is intentionally unresolved as a typed
migration because no repository evidence establishes timing, pointer identity,
coordinate space, multi-pointer structure or complete gesture boundaries.

## Contract Boundary

`Path` is timeless geometry. `PointerPath` adds one pointer's timing,
`MultiPath` represents parallel pointer paths and `GestureSequence` represents
sequential complete gestures. Legacy data may enter one of those models only
when its own payload proves the required information. Names and numeric shapes
alone are not proof.

The canonical analysis path is:

```text
EMScript parser
  -> unchanged raw legacy argument
  -> LegacyTouchStructuralClassifier
  -> evidence + diagnostics + migration readiness
```

Workspace and IR continue to carry the unchanged raw argument. The classifier
does not write canonical source.

## Aliases

| Alias | Repository evidence | Semantic distinction |
| --- | --- | --- |
| `touch` | canonical catalog name and all located source fixtures | none |
| `Touch.dispatch` | declared historical catalog alias | none found |
| `TOUCH` | declared historical catalog alias | none found |

`Scrcpy.touch` is a separate provider command and is not an `input.touch`
alias.

## Payload Inventory

| Payload form | Fundstelle | Alias | Structure | Present information | Missing information | Classification | Previous interpretation |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `[540, 1100]` | `app/src/main/java/com/visualtasker/wss/emscript/editor/EditorDefaults.kt` | `touch` | flat numeric array | one coordinate-like pair | state, timing, pointer ID, movement, multi-pointer structure, gesture boundaries, coordinate space | PARTIAL / REQUIRES_STRUCTURAL_MIGRATION | raw `sequence: ANY` |
| `["down", 120, 240, "up"]` | `WorkspaceDryRunRuntimeTest.kt`, `RuntimeCapabilityGateTest.kt`, `WssDropEffects.kt`, `WssDropEffectsTest.kt` | `touch` | mixed primitive array | `down`, `up`, one coordinate-like pair | timing, pointer ID, movement, multi-pointer structure, gesture boundaries, coordinate space | PARTIAL / REQUIRES_STRUCTURAL_MIGRATION | raw `sequence: ANY`; live disabled |
| `"down(10,20);move(20,30);up(20,30)"` | `docs/reference/emscript/conformance/v1/legacy/l008_touch_sequence.ems` | `touch` | string containing three atomic calls | `down`, `move`, `up`, three coordinate-like pairs | timing, pointer ID, multi-pointer structure, complete-gesture boundaries, coordinate space | PARTIAL / REQUIRES_STRUCTURAL_MIGRATION | legacy conformance input |

Repository searches also covered block definitions, catalog, parser, Workspace
importer/serializer, IR and IR generator, EMScript generator, runtime gates,
legacy normalization, Macrorify references, tests, examples and documentation.
No additional `input.touch` payload schema was found.

## Evidence Summary

| Evidence | Repository-backed result |
| --- | --- |
| coordinates | present in all three fixtures |
| down | present in two fixtures |
| move | present only in the atomic-call string fixture |
| up | present in two fixtures |
| timing | absent |
| pointer ID | absent |
| explicit multi-pointer structure | absent |
| complete gesture-sequence boundaries | absent |
| authoritative coordinate space | absent |

Semicolons in the atomic-call string separate atomic calls; they do not prove
boundaries between complete gestures. Nested arrays are not interpreted as
multi-pointer input without an explicit legacy contract.

## Classification Rules

- `LOSSLESS`: a concrete known legacy semantic model is complete and
  unambiguous. No current fixture qualifies.
- `PARTIAL`: coordinates and/or atomic states are recognizable, but a complete
  typed V1 gesture cannot be reconstructed.
- `UNKNOWN`: empty, malformed or wholly unknown payload. Raw text remains
  authoritative and migration readiness is `PRESERVE_LEGACY`.

The classifier reports absent evidence separately from migration diagnostics.
It does not synthesize `duration=0`, `pointerId=0`, screen-pixel coordinates,
a tap, a swipe, a `PointerPath`, a `MultiPath` or a `GestureSequence`.

## Roundtrip And Runtime

Classification preserves `rawPayload` exactly and is deterministic and
idempotent. Existing parse -> Workspace `args` -> IR `arguments` -> source
generation remains unchanged. Live dispatch stays disabled and no runtime,
recorder, Accessibility, Shizuku, IME or provider behavior is added.

## Future Projection Candidates

These are investigation candidates, not 3H-A decisions:

- `[540, 1100]`: preserve legacy unless an authoritative contract proves a
  Point-based atomic touch action. It cannot currently become a complete
  gesture.
- `down/up` array: may support atomic touch primitives after its ordering,
  coordinate ownership and omitted timing semantics are specified. It cannot
  currently become `PointerPath` or `GestureSequence` losslessly.
- atomic-call string: may support one pointer's ordered atomic trace if timing,
  pointer identity and legacy ordering semantics are resolved. Its semicolons
  do not justify `GestureSequence`.

No current fixture justifies `MultiPath`. `Path` must not be used as a shortcut
because it would erase the geometry/gesture-timing boundary.

M1B-3H-B remains deferred until an authoritative legacy touch contract exists.
