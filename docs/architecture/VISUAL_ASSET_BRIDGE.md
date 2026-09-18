# Visual Asset Bridge

## Authority

M3ShapeMaker owns visual authoring and the canonical vector design. VisualTasker Studio WSS owns
catalog assignment, workflow dependencies and the decision where an asset is rendered. Neither app
duplicates the other app's document model.

## Exchange Format

- File extension: `.ema`
- MIME type: `application/vnd.emscript.motion+json`
- Format id: `emscript_motion_asset`
- Schema version: `1`
- Transfer: `content://` URI with a temporary read grant

The schema carries design, visual states, transitions, animation sequences and metadata. WSS reads a
descriptor and preserves the complete source file losslessly. It also projects semantic counts for
`ports`, `anchors` and `contentAreas` from the canonical design.

Supported asset types are `SHAPE`, `ANIMATED_SHAPE`, `BLOCK_SHAPE`, `NODE_SHAPE`, `PORT_SHAPE`,
`ICON`, `VISUAL_COMPONENT`, `OVERLAY_SHAPE` and `ACTOR`.

## Android Contract

- Action: `com.m3shapes.editor.action.CREATE_VISUAL_ASSET`
- Request extra: `com.m3shapes.editor.extra.REQUESTED_ASSET_TYPE`
- Result extra: `com.m3shapes.editor.extra.RESULT_ASSET_ID`
- Result data: readable `.ema` content URI

WSS imports the returned file atomically into its managed asset directory and registers a versioned
`WorkspaceResourceKind.VisualAsset`. Manual `.ema` import remains available as a fallback.

## Binding Projection

- `BLOCK_SHAPE` -> Block
- `NODE_SHAPE` -> FlowNode
- `PORT_SHAPE` -> Port
- `ICON` -> Icon
- `VISUAL_COMPONENT`, `OVERLAY_SHAPE`, `ACTOR` -> Overlay
- generic shape types -> Block, FlowNode and Overlay candidates

Bindings are catalog metadata, not runtime authority. Editor renderers may consume them only through
their plugin-facing visual contracts. Compose overlays remain selected-element affordances and never
replace serializable block or node geometry.
