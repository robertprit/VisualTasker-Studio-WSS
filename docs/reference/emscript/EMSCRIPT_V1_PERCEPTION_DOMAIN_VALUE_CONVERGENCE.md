# EMScript v1 Perception Domain Value Convergence

Stand: 2026-10-03

Phase: M1B-3Y

## Signatures

| Stable command ID | Canonical source name | Return type | Legacy aliases |
| --- | --- | --- | --- |
| `action.findTemplate` | `findTemplate` | `ImageMatch?` | `FIND_TEMPLATE` |
| `vision.findText` | `findText` | `TextMatch?` | `Region.findText`, `FIND_TEXT` |
| `vision.markerLoad` | `markerLoad` | `Marker?` | `Marker.load`, `LOAD_MARKER` |

All three commands are generic value reporters and remain
`IrExpression.CommandCall`. Canonical serialization writes the canonical source
name while imports retain the aliases.

## Domain Values

`ImageMatchValue` contains:

- stable template/resource ID
- display label
- matched region
- similarity score

`TextMatchValue` contains:

- matched text
- observed region
- confidence
- stable source label such as `ocr` or `a11y`

`MarkerValue` contains:

- stable marker ID
- label
- marker region
- optional three-point Bezier path
- mode
- optional visual-asset ID
- threshold

No value carries a `Bitmap`, Android `Rect`, OpenCV/ML Kit object, database
entity, repository DTO, accessibility node or runtime handle. A loaded marker
keeps its persisted marker ID; lookup by a legacy label does not replace that
identity.

## Geometry

The runtime previously had no canonical EMScript geometry value suitable for
these results. M1B-3Y therefore adds only the required immutable value
representations: point, positive non-empty region and three-point Bezier path.
Every geometry declares either `PIXEL` or `NORMALIZED` coordinates. Normalized
points and regions must stay in `0..1`; one path cannot mix coordinate spaces.
This is not a general geometry rewrite.

## Score And Confidence

Image similarity, text confidence and marker threshold retain the existing
native `0..1` scale. They are not converted to percentages. Zero is a valid
field value when a provider emits a successful result with zero; values outside
`0..1`, NaN and infinity are invalid provider results.

## VALUE, ABSENT And FAILURE

| Command | VALUE | ABSENT | Representative failures |
| --- | --- | --- | --- |
| `action.findTemplate` | `ImageMatchValue` | completed search without threshold-qualified match | vision adapter unavailable, capture failed, template unavailable, comparison failed, invalid result |
| `vision.findText` | best `TextMatchValue` | completed observation query without matching text | vision adapter unavailable, capture failed, OCR failed, invalid result |
| `vision.markerLoad` | `MarkerValue` | repository read succeeded but marker is missing | repository unavailable, load/storage failure, malformed marker |

ABSENT is always `RuntimeAdapterResult.success(NullValue)`. A successful adapter
result without a value is invalid and is not ABSENT. Technical faults are
`RuntimeAdapterResult.failure` with a diagnostic and never become `NullValue`,
empty text, dummy geometry, negative confidence or another sentinel.

The runtime uses these diagnostics where applicable:

- `VISION_ADAPTER_UNAVAILABLE`
- `TEMPLATE_RESOURCE_UNAVAILABLE`
- `VISION_CAPTURE_FAILED`
- `TEMPLATE_MATCH_FAILED`
- `OCR_FAILED`
- `VISION_RESULT_INVALID`
- `MARKER_REPOSITORY_UNAVAILABLE`
- `MARKER_DECODE_FAILED`

## Provider Mapping

Template matching continues to use the existing screenshot/template comparison
path. A threshold-qualified result maps to `ImageMatchValue`; a valid score
below the threshold maps to ABSENT. Missing capture or template evidence and
comparison failures remain diagnostics.

Text lookup reads the existing visual observations without introducing a new
OCR provider. It filters text/OCR observations and selects deterministically:
exact text before partial text, then higher confidence, newer observation and
finally stable observation ID. No match is ABSENT.

Marker lookup uses the existing persisted marker collection. Region, optional
path, mode, asset reference, threshold and persisted ID are copied into an
immutable value. Loading does not promote a marker to a Worldview entity or
create a new repository.

## Type System And Editors

`ImageMatch`, `TextMatch` and `Marker` are distinct domain types. Their nullable
forms reuse the existing EMScript nullable rules. They are not implicitly
assignable to each other or to `String`, `Number` or `Bool`.

The BlockEditor projects all three through the generic command-reporter path.
The former special statement-shaped `action.findTemplate` definition remains
hidden only for legacy workspace resolution. New imports use
`emscript.command.action.findTemplate`. Reporter slot compatibility follows the
declared nullable domain type.

Source, parser, workspace, IR, serialization and regenerated source preserve
the stable command ID, arguments, nullable return type and expression shape.
LET and SET remain distinguishable. Alias input normalizes only the source
spelling.

## Property Access

Domain-value transport, assignment, serialization and reporter projection are
available. EMScript v1 currently has no frozen general property-access contract,
so M1B-3Y does not invent `match.score`, `text.bounds` or `marker.id` syntax.
Property access remains pending a separate language decision.

## Post-Migration Audit

- `D_QUERY_RETURN = 2`
- Command Catalog `= 127`
- `NATIVE_V1 = 3`
- Unmappable `= 0`

The only remaining query-return conflicts are `chart.exists` and `chart.get`.
