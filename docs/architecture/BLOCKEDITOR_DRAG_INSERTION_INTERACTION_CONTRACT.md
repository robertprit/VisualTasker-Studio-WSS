# BlockEditor Drag and Insertion Interaction Contract

## Scope

This contract defines BlockEditor drag presentation after P2C. It does not
change `WorkspaceDocument`, `WorkflowGraph`, connection compatibility, stable
IDs, history boundaries, EMScript generation, source mapping, or bulk assembly.

## Previous interaction model

The lifted root remained in the static canvas as a translucent ghost while a
second copy was rendered in the drag layer. The drag copy used the snap
candidate offset once snapping became active. On successful drop, the normal
placement animation still owned the dragged block, so it could visibly travel
from its remembered pre-drop position to the canonical target.

This represented an expected future mutation rather than direct manipulation.

## Interaction invariant

During an active drag, the complete dragged structure follows the pointer with
the grab offset captured at drag start. Snap detection must not replace this
pointer offset. The persistent rendering of every member of the dragged
structure is excluded from the static canvas, so no second visible copy remains
at the source.

`DraggedStructure` is the existing drag session's `includedBlocks` set. It may
contain one statement, a statement stack, a control subtree, a reporter, or a
nested expression. Relative child geometry is supplied by the existing drag
layout and remains unchanged while the structure moves.

## Drag visual

The drag layer renders the real block definitions and the existing drag layout.
It is above the static workspace, uses `DragSession.dragOffset` directly, and is
not driven by placement animation. Pure presentation effects may be added only
if they do not delay or alter pointer tracking.

## Lift and detach

Drag start builds a detached presentation document using the existing lift
operation. The static layout is derived from that detached document for all
pull modes. This closes a vacated statement gap while the lifted structure
continues to follow the pointer. The canonical document is unchanged until
drop.

## Insertion preview

When the snap engine reports a new valid candidate, the BlockEditor applies the
candidate connection to the detached preview document only and lays out that
hypothetical result. The resulting layout becomes the static presentation
layout while the dragged structure remains in the drag layer.

The preview document is not canonical truth. It is never published, persisted,
projected to EMScript, or written to history. Candidate changes recompute the
hypothetical relation only when source or target connection identity changes,
not on every pointer move.

`previewAffectedBlockIds` contains non-dragged blocks whose bounds differ from
the candidate-independent post-lift layout. It is diagnostic presentation
state and has no semantic role.

## Animation ownership

- Dragged blocks use direct pointer movement and never placement animation.
- Blocks whose preview positions change animate toward the newest layout.
- Unchanged blocks keep their positions.
- Candidate loss restores the post-lift base layout, allowing displaced blocks
  to animate back.
- Retargeting always updates existing per-block animatables toward the newest
  target; it does not queue complete candidate animations.

After successful drop, IDs remembered from the just-finished drag are snapped
to their canonical target positions before normal placement animation can own
them. Therefore the committed block does not fly from its source to its target.

## Statements and reordering

Statement insertion uses the existing `Connect` reducer behavior on the preview
copy. If a target connection already owns a following statement, that structure
is reattached after the inserted structure exactly as the real commit will do.
The visible gap therefore reflects the actual insertion result. Reordering
inside the same stack follows the same rule after lift; no separate reorder
model exists.

## Subtrees and containers

All blocks in `includedBlocks` are connected in the hypothetical preview.
Consequently layout reserves the complete measured envelope of a stack or
control subtree, including statement-input children. IF/ELSE and loop geometry
continues to be calculated by the normal layout engine. Complex controls use
that full-layout correctness path rather than an approximate local geometry.

## Reporters

Reporter and nested expression drags use their output connection as the source.
A compatible value-input candidate is connected in the preview copy, allowing
the normal expression layout to resize the owner and its surrounding container.
Existing occupied-input and type-compatibility semantics are unchanged. The
dragged reporter remains pointer-owned and receives no snap tween or post-drop
placement flight.

## Candidate switching, loss, and cancel

Snap hysteresis may retain the current candidate only while that candidate is
still valid. Switching source/target identity replaces the insertion preview;
offsets never accumulate. Losing the candidate restores the base post-lift
layout and clears preview metadata. Cancel discards all presentation state and
reveals the unchanged canonical document. None of these paths creates history.

## Commit and undo boundary

A successful pointer-up performs the existing single semantic connection or
free-position commit. The active preview is evidence of the expected result,
not an earlier mutation. Hover, candidate switching, candidate loss, and cancel
produce zero canonical mutations and zero history entries. Successful drop
keeps the existing one-action history boundary, and undo restores the original
structure.

## Performance invariants

- No EMScript generation, source import, persistence, or history write occurs
  while hovering.
- Pointer-only movement does not rebuild preview layout while the candidate
  relation remains the same.
- Candidate changes may use the normal full layout as the correctness fallback,
  especially for control containers.
- P2A affected-structure rearrange and P2C source-mapping/bulk-assembly contracts
  remain unchanged.
- Pointer tracking has priority over all decorative animation.

