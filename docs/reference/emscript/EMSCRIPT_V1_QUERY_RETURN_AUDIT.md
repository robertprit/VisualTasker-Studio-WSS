# EMScript v1 Remaining Query Return Reclassification

Stand: 2026-10-03
Phase: M1B-3Z Chart Domain Value Convergence
Status: COMPLETE; no D_QUERY_RETURN conflicts remain

## Invariants

The bridge derives exactly 0 D_QUERY_RETURN entries. All twenty-three approved query migrations through M1B-3Z remain outside this inventory.

## Classification Summary

| Class | Count |
| --- | ---: |
| A_NULLABLE_SCALAR_READY | 0 |
| B_NONNULL_SCALAR_READY | 0 |
| C_STRUCTURED_RESULT_TYPE | 0 |
| D_RUNTIME_RESULT_GAP | 0 |
| E_PROVIDER_CONTRACT_GAP | 0 |
| F_SENTINEL_OR_ERROR_COLLISION | 0 |
| G_LANGUAGE_SEMANTICS_GAP | 0 |
| H_AMBIGUOUS | 0 |

## Decision Matrix

| Stable ID | Proposed | Primary class | Flags | Runtime value | Transport | Consumption | First loss | Risk |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

## Catalog And Provider Evidence

| Stable ID | Aliases | Parameters | Provider | Bridge | Tests | Producer / first consumer | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |

## Structured Result Evidence

| Stable ID | Proposed type | Internal type | Repository fields | Planned-only fields | Nullable |
| --- | --- | --- | --- | --- | --- |

## Nullable Language Impact

Scalar nullable transport exists, but source-level inspection or resolution does not. Every nullable candidate here has consumptionReady=false. Repository evidence creates needs for presence testing, conditional branching on presence, extraction of a present value and explicit fallback; this audit chooses no syntax.

## Recommended Next Slice

M1B-3Z converges `chart.exists(...): Bool` and `chart.get(...): ChartSnapshot?`. No D_QUERY_RETURN conflicts remain. See EMSCRIPT_V1_REMAINING_QUERY_RETURN_CLASSIFICATION.md for the historical 3V decision record.
