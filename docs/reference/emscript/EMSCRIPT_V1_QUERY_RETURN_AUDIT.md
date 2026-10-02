# EMScript v1 Remaining Query Return Reclassification

Stand: 2026-10-02
Phase: M1B-3X Tasker Collection Query Convergence
Status: Tasker collection query migrated; five query contracts remain

## Invariants

The bridge derives exactly 5 D_QUERY_RETURN entries. Each has one primary class. The eighteen approved query migrations through M1B-3X remain outside this inventory.

## Classification Summary

| Class | Count |
| --- | ---: |
| A_NULLABLE_SCALAR_READY | 0 |
| B_NONNULL_SCALAR_READY | 0 |
| C_STRUCTURED_RESULT_TYPE | 2 |
| D_RUNTIME_RESULT_GAP | 3 |
| E_PROVIDER_CONTRACT_GAP | 0 |
| F_SENTINEL_OR_ERROR_COLLISION | 0 |
| G_LANGUAGE_SEMANTICS_GAP | 0 |
| H_AMBIGUOUS | 0 |

## Decision Matrix

| Stable ID | Proposed | Primary class | Flags | Runtime value | Transport | Consumption | First loss | Risk |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `action.findTemplate` | `ImageMatch?` | C_STRUCTURED_RESULT_TYPE | NULLABLE, STRUCTURED, RUNTIME_VALUE_EXISTS, NEEDS_NEW_TYPE, FAILURE_COLLISION | yes | no | no | WorkspaceBasicRuntime converts RuntimeTemplateMatch? to LiveExecutionOutcome text. | HIGH |
| `vision.findText` | `TextMatch?` | D_RUNTIME_RESULT_GAP | NULLABLE, PROVIDER_BACKED, STRUCTURED, NEEDS_NEW_TYPE, BLOCKED | no | no | no | No WorkspaceBasicRuntime environment function or value-returning vision provider dispatch exists. | BLOCKED |
| `vision.markerLoad` | `Marker?` | C_STRUCTURED_RESULT_TYPE | NULLABLE, STRUCTURED, RUNTIME_VALUE_EXISTS, NEEDS_NEW_TYPE, FAILURE_COLLISION | yes | no | no | WorkspaceBasicRuntime converts RuntimeAutomationRegion? to text; EmscriptValue has no RegionValue. | HIGH |
| `chart.exists` | `Bool` | D_RUNTIME_RESULT_GAP | PROVIDER_BACKED, BLOCKED | no | no | no | No provider-independent chart repository adapter is connected to WorkspaceBasicRuntime. | BLOCKED |
| `chart.get` | `ChartSnapshot?` | D_RUNTIME_RESULT_GAP | NULLABLE, PROVIDER_BACKED, STRUCTURED, NEEDS_NEW_TYPE, BLOCKED | no | no | no | No chart repository adapter or EMScript ChartSnapshotValue exists. | BLOCKED |

## Catalog And Provider Evidence

| Stable ID | Aliases | Parameters | Provider | Bridge | Tests | Producer / first consumer | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `action.findTemplate` | FIND_TEMPLATE | imagePath:IMAGE_TEMPLATE, threshold:PERCENT, timeoutMs:DURATION_MS, retryCount:NUMBER?, searchRegion:REGION? | `workspace vision adapter` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest; WorkspaceDryRunRuntimeTest; EmscriptParserSliceTest | WorkspaceBasicRuntimeEnvironment.findTemplate / WorkspaceScreen vision adapter / WorkspaceBasicRuntime LiveExecutionOutcome | A threshold-qualified match exists; null covers no qualifying match and unavailable comparison evidence, while score and region are discarded into display text. |
| `vision.findText` | Region.findText, FIND_TEXT | text:TEXT, timeoutMs:DURATION_MS? | `visualtasker.vision` | CONFLICT/CONTRACT_DECISION | EmscriptParserSliceTest; RuntimeCapabilityGateTest | none / statement-only catalog/runtime trace | M1B-3V freezes singular findText as the best TextMatch?; no authoritative OCR producer is connected yet. |
| `vision.markerLoad` | Marker.load, LOAD_MARKER | name:TEXT | `visualtasker.vision` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest; WorkspaceDryRunRuntimeTest; EmscriptParserSliceTest | WorkspaceScreen saved-marker lookup / WorkspaceBasicRuntime LiveExecutionOutcome | Saved markers currently yield only bounds or null; M1B-3V requires stable marker ID, geometry and metadata to survive. |
| `chart.exists` | none | id:TEXT | `visualtasker.charts` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | none / statement-only catalog/runtime trace | M1B-3V freezes chart.exists as non-null Bool; a missing persistent chart ID is false, not ABSENT. |
| `chart.get` | none | id:TEXT, key:TEXT? | `visualtasker.charts` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | none / statement-only catalog/runtime trace | M1B-3V freezes missing ID as ABSENT and an empty chart as VALUE; the optional legacy key is not canonical V1. |

## Structured Result Evidence

| Stable ID | Proposed type | Internal type | Repository fields | Planned-only fields | Nullable |
| --- | --- | --- | --- | --- | --- |
| `action.findTemplate` | `ImageMatch` | `RuntimeTemplateMatch` | name:String; region:RuntimeAutomationRegion; score:Float | none | YES |
| `vision.findText` | `TextMatch` | `none` | text:String; region:Region; confidence:Number; source:String | none | YES |
| `vision.markerLoad` | `Marker` | `ScreenshotCanvasSavedMarker` | markerId:String; label:String; region:Region; path:Path?; mode:String; assetId:String?; threshold:Number | none | YES |
| `chart.get` | `ChartSnapshot` | `ChartDocument` | id:String; title:String; kind:String; immutable data/options | none | YES |

## Nullable Language Impact

Scalar nullable transport exists, but source-level inspection or resolution does not. Every nullable candidate here has consumptionReady=false. Repository evidence creates needs for presence testing, conditional branching on presence, extraction of a present value and explicit fallback; this audit chooses no syntax.

## Recommended Next Slice

M1B-3X converges `tasker.getVariables` on a non-null `List<TaskerVariable>`. M1B-3Y and M1B-3Z retain the five perception and chart contracts. See EMSCRIPT_V1_REMAINING_QUERY_RETURN_CLASSIFICATION.md for the historical 3V decisions and VALUE/ABSENT/NEGATIVE/FAILURE semantics.
