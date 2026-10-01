# EMScript v1 `vision.templateCompare` Result Semantics

Stand: 2026-09-28
Phase: M1B-3P
Status: READY; contract implemented end to end

## Decision

```emscript
templateCompare(name: String, region: Region, processing: String = "grayscale"): Number
```

`templateCompare` compares two concrete image regions. It is not a scene
search and it does not decide whether a match is accepted. A completed
comparison always returns a score. Missing required evidence and technical
errors are runtime failures, not nullable absence.

## Repository Path

1. `CommandCatalog` defines stable ID `vision.templateCompare`, canonical name
   `templateCompare`, aliases `Template.compare` and `COMPARE_TEMPLATE`, and
   parameters `name`, `region`, `processing="grayscale"`.
2. The parser projects `region(x, y, width, height)` to a typed Region literal
   block. Workspace-to-IR lowers it to generic `core.region`, and
   `vision.templateCompare` is a generic `IrExpression.CommandCall` returning
   `Number`.
3. `WorkspaceBasicRuntime` resolves all three arguments and calls
   `WorkspaceBasicRuntimeEnvironment.templateCompare`, whose transport type is
   non-null `Float`.
4. `WorkspaceScreen` resolves a saved Template marker by label or ID, resolves
   the live and reference assets, decodes both bitmaps and calls
   `compareScreenshotRegions`.
5. `compareScreenshotRegions` samples at most 32 by 32 corresponding points,
   computes processed grayscale values, accumulates absolute differences and
   returns the normalized similarity score.
6. `WorkspaceBasicRuntime` validates finite `0.0..1.0` output and transports it
   as `NumberValue`. Former nullable paths now raise specific structured
   diagnostics.

## Numeric Contract

| Property | Contract |
| --- | --- |
| Semantic name | normalized mean absolute grayscale similarity |
| Formula | `clamp(1 - mean(abs(process(live) - process(reference))) / 255, 0, 1)` |
| Range | inclusive `0.0..1.0` |
| Direction | higher is more similar |
| Internal representation | Kotlin `Float` |
| EMScript representation | `Number` |
| Unit | unitless ratio; UI may display `score * 100` percent |
| NaN / Infinity | forbidden |
| Negative values | forbidden |
| Rounding | no semantic rounding; current UI formats one decimal percent |
| Sentinel values | none |

The preprocessing modes reduce each sampled pixel to an integer `0..255`.
`Original`, `Grayscale` and `FalseColor` currently compare the same grayscale
value. `HighContrast`, `Mask`, `Inverse` and `Edge` transform that value before
comparison. Regions of different dimensions are normalized onto the same
sample grid; this is not OpenCV correlation and not a geometric template
search.

## Threshold Contract

There is no acceptance-threshold parameter. The `processing` parameter selects
a preprocessing mode. Mask mode uses an internal default luminance cutoff of
`0.5`; this changes each processed pixel to black or white and does not filter
the resulting score. Consequently, a low score remains a valid `VALUE`.

The threshold stored on a marker belongs to marker/find behavior and is not
read by `templateCompare`. `action.findTemplate` does accept a threshold and
returns a match only when `score >= threshold`; that behavior must not be
copied onto `templateCompare`.

## Result-State Decision

| Runtime state | Current behavior | V1 VALUE | V1 ABSENT | V1 FAILURE |
| --- | --- | --- | --- | --- |
| Valid comparison | `Float`, rendered as percent | normalized score | no | no |
| Score below a caller threshold | no such state | not applicable | no | no |
| No match | no search/acceptance step exists | not applicable | no | no |
| Missing template | structured failure | no | no | `TEMPLATE_NOT_FOUND` |
| Missing/undecodable image or frame | structured failure | no | no | `TEMPLATE_IMAGE_UNAVAILABLE` |
| Unusable comparison region | structured failure | no | no | `TEMPLATE_REGION_UNAVAILABLE` |
| Invalid input | malformed/nonpositive regions fail before comparison | no | no | `TEMPLATE_REGION_UNAVAILABLE` |
| Backend/comparison failure | structured failure | no | no | `TEMPLATE_COMPARE_FAILED` |
| Unsupported backend | no distinct current state | no | no | provider/capability failure if introduced |

There is no normative `NOT_FOUND`, `NO_MATCH`, `NO_EVIDENCE` or `ABSENT`
result. The evidence is required input to a comparison. If it is missing, the
operation was not successfully performed.

## Null-Origin Matrix

| Source location | Trigger | Intended meaning | Current downstream interpretation |
| --- | --- | --- | --- |
| `WorkspaceScreen` template lookup | no Template marker matches name/ID | required template missing | `TEMPLATE_NOT_FOUND` |
| `decodeScreenshotBitmap` / live `safeBitmapRegion` | live asset/file/decode/region unavailable | required live evidence missing | `TEMPLATE_IMAGE_UNAVAILABLE` or `TEMPLATE_REGION_UNAVAILABLE` |
| `decodeScreenshotBitmap` / reference `safeBitmapRegion` | reference asset/file/decode/region unavailable | required reference evidence missing | `TEMPLATE_IMAGE_UNAVAILABLE` or `TEMPLATE_REGION_UNAVAILABLE` |
| `compareScreenshotRegions` sample guard | zero samples; defensive and unreachable under current bounds | comparison impossible | `TEMPLATE_COMPARE_FAILED` |
| default runtime environment adapter | no production vision adapter supplied | runtime capability unavailable | `TEMPLATE_COMPARE_FAILED` |

These Kotlin nulls are transport artifacts. None becomes V1 `NullValue`.

## Input Semantics And Current Weaknesses

- `name` is currently an opaque String template label or ID.
- `region` is the live image region; dimensions are coerced to at least one and
  coordinates are clamped to bitmap bounds.
- `processing` is a weak String selector. Known aliases map to grayscale,
  false-color, high-contrast, mask, edge or inverse; unknown values silently
  select Original.
- The stored template marker supplies the reference asset, reference region
  and reference processing mode.
- If the marker's asset cannot be resolved, the adapter raises
  `TEMPLATE_IMAGE_UNAVAILABLE`; it does not compare the live image with itself.

M1B-3P adds the Region expression projection needed by the existing signature;
it does not introduce a new structured runtime value.

## Relation To `action.findTemplate`

`action.findTemplate` searches a live region for a threshold-qualified result
and returns position/bounds plus score through `RuntimeTemplateMatch?`.
`vision.templateCompare` compares one live region with one saved reference
region and returns only the score. Both reuse `compareScreenshotRegions`, but
only Find applies `score >= threshold`. Find can legitimately produce no
match; Compare cannot. Compare therefore needs no `ImageMatch` type.

## Migration Boundary

The contract needs neither a new structured result nor nullable-consumption
syntax. Existing generic Number transport is sufficient. M1B-3P migrates only
`vision.templateCompare` to a non-null Number command expression and routes all
former null origins through structured runtime failures.

Adjacent queries and `action.findTemplate` remain unchanged.
`D_QUERY_RETURN` is 17, bridge count remains 127 and `NATIVE_V1` remains 3.
