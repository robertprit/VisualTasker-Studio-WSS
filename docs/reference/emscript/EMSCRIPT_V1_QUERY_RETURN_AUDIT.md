# EMScript v1 Remaining Query Return Reclassification

Stand: 2026-10-03
Phase: M1B-3Y Perception Domain Value Convergence
Status: Perception domain queries migrated; two chart query contracts remain

## Invariants

The bridge derives exactly 2 D_QUERY_RETURN entries. Each has one primary class. The twenty-one approved query migrations through M1B-3Y remain outside this inventory.

## Classification Summary

| Class | Count |
| --- | ---: |
| A_NULLABLE_SCALAR_READY | 0 |
| B_NONNULL_SCALAR_READY | 0 |
| C_STRUCTURED_RESULT_TYPE | 0 |
| D_RUNTIME_RESULT_GAP | 2 |
| E_PROVIDER_CONTRACT_GAP | 0 |
| F_SENTINEL_OR_ERROR_COLLISION | 0 |
| G_LANGUAGE_SEMANTICS_GAP | 0 |
| H_AMBIGUOUS | 0 |

## Decision Matrix

| Stable ID | Proposed | Primary class | Flags | Runtime value | Transport | Consumption | First loss | Risk |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `chart.exists` | `Bool` | D_RUNTIME_RESULT_GAP | PROVIDER_BACKED, BLOCKED | no | no | no | No provider-independent chart repository adapter is connected to WorkspaceBasicRuntime. | BLOCKED |
| `chart.get` | `ChartSnapshot?` | D_RUNTIME_RESULT_GAP | NULLABLE, PROVIDER_BACKED, STRUCTURED, NEEDS_NEW_TYPE, BLOCKED | no | no | no | No chart repository adapter or EMScript ChartSnapshotValue exists. | BLOCKED |

## Catalog And Provider Evidence

| Stable ID | Aliases | Parameters | Provider | Bridge | Tests | Producer / first consumer | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `chart.exists` | none | id:TEXT | `visualtasker.charts` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | none / statement-only catalog/runtime trace | M1B-3V freezes chart.exists as non-null Bool; a missing persistent chart ID is false, not ABSENT. |
| `chart.get` | none | id:TEXT, key:TEXT? | `visualtasker.charts` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | none / statement-only catalog/runtime trace | M1B-3V freezes missing ID as ABSENT and an empty chart as VALUE; the optional legacy key is not canonical V1. |

## Structured Result Evidence

| Stable ID | Proposed type | Internal type | Repository fields | Planned-only fields | Nullable |
| --- | --- | --- | --- | --- | --- |
| `chart.get` | `ChartSnapshot` | `ChartDocument` | id:String; title:String; kind:String; immutable data/options | none | YES |

## Nullable Language Impact

Scalar nullable transport exists, but source-level inspection or resolution does not. Every nullable candidate here has consumptionReady=false. Repository evidence creates needs for presence testing, conditional branching on presence, extraction of a present value and explicit fallback; this audit chooses no syntax.

## Recommended Next Slice

M1B-3Y converges the three nullable perception/resource queries on `ImageMatch?`, `TextMatch?`, and `Marker?`. M1B-3Z retains only `chart.exists` and `chart.get`. See EMSCRIPT_V1_REMAINING_QUERY_RETURN_CLASSIFICATION.md for the historical 3V decisions and VALUE/ABSENT/NEGATIVE/FAILURE semantics.
