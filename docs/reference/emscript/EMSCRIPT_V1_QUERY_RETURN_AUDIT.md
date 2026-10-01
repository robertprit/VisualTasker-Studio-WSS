# EMScript v1 Remaining Query Return Reclassification

Stand: 2026-10-01
Phase: M1B-3U Shizuku availability convergence
Status: shizuku.isAvailable converged as a typed Bool reporter

## Invariants

The bridge derives exactly 12 D_QUERY_RETURN entries. Each has one primary class. The eleven approved query migrations through M1B-3U remain outside this inventory.

## Classification Summary

| Class | Count |
| --- | ---: |
| A_NULLABLE_SCALAR_READY | 0 |
| B_NONNULL_SCALAR_READY | 0 |
| C_STRUCTURED_RESULT_TYPE | 2 |
| D_RUNTIME_RESULT_GAP | 5 |
| E_PROVIDER_CONTRACT_GAP | 0 |
| F_SENTINEL_OR_ERROR_COLLISION | 5 |
| G_LANGUAGE_SEMANTICS_GAP | 0 |
| H_AMBIGUOUS | 0 |

## Decision Matrix

| Stable ID | Proposed | Primary class | Flags | Runtime value | Transport | Consumption | First loss | Risk |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `action.findTemplate` | `ImageMatch?` | C_STRUCTURED_RESULT_TYPE | NULLABLE, STRUCTURED, RUNTIME_VALUE_EXISTS, NEEDS_NEW_TYPE, FAILURE_COLLISION | yes | no | no | WorkspaceBasicRuntime converts RuntimeTemplateMatch? to LiveExecutionOutcome text. | HIGH |
| `vision.findText` | `UNRESOLVED` | D_RUNTIME_RESULT_GAP | PROVIDER_BACKED, BLOCKED | no | no | no | No WorkspaceBasicRuntime environment function or value-returning vision provider dispatch exists. | BLOCKED |
| `vision.markerLoad` | `Region?` | C_STRUCTURED_RESULT_TYPE | NULLABLE, STRUCTURED, RUNTIME_VALUE_EXISTS, NEEDS_NEW_TYPE, FAILURE_COLLISION | yes | no | no | WorkspaceBasicRuntime converts RuntimeAutomationRegion? to text; EmscriptValue has no RegionValue. | HIGH |
| `tasker.isEnabled` | `Bool` | F_SENTINEL_OR_ERROR_COLLISION | PROVIDER_BACKED, RUNTIME_VALUE_EXISTS, FAILURE_COLLISION | yes | yes | yes | WorkspaceScreen maps isEnabled to composite status.available and then to RuntimeAdapterResult. | HIGH |
| `tasker.getVariable` | `UNRESOLVED` | D_RUNTIME_RESULT_GAP | PROVIDER_BACKED, BLOCKED | no | no | no | No value-returning runtime/provider contract exists for this command. | BLOCKED |
| `tasker.getVariables` | `UNRESOLVED` | D_RUNTIME_RESULT_GAP | PROVIDER_BACKED, BLOCKED | no | no | no | No value-returning runtime/provider contract exists for this command. | BLOCKED |
| `shizuku.getUid` | `Number?` | F_SENTINEL_OR_ERROR_COLLISION | NULLABLE, PROVIDER_BACKED, RUNTIME_VALUE_EXISTS, SENTINEL, FAILURE_COLLISION | yes | yes | no | inspect collapses unavailable/permission/binder/getUid failure to uid=null; adapter renders null as -1. | HIGH |
| `termux.get` | `String?` | F_SENTINEL_OR_ERROR_COLLISION | NULLABLE, PROVIDER_BACKED, RUNTIME_VALUE_EXISTS, SENTINEL, FAILURE_COLLISION | yes | yes | no | WorkspaceScreen maps unknown keys to empty String and embeds values in RuntimeAdapterResult.message. | HIGH |
| `scrcpy.isRunning` | `Bool` | F_SENTINEL_OR_ERROR_COLLISION | PROVIDER_BACKED, RUNTIME_VALUE_EXISTS, FAILURE_COLLISION | yes | yes | yes | WorkspaceScreen substitutes bridgeReady for process/session running state. | HIGH |
| `scrcpy.get` | `UNRESOLVED` | F_SENTINEL_OR_ERROR_COLLISION | PROVIDER_BACKED, RUNTIME_VALUE_EXISTS, FAILURE_COLLISION, BLOCKED | yes | no | no | WorkspaceScreen ignores key semantics and always returns bridge status summary text. | BLOCKED |
| `chart.exists` | `UNRESOLVED` | D_RUNTIME_RESULT_GAP | PROVIDER_BACKED, BLOCKED | no | no | no | No value-returning runtime/provider contract exists for this command. | BLOCKED |
| `chart.get` | `UNRESOLVED` | D_RUNTIME_RESULT_GAP | PROVIDER_BACKED, BLOCKED | no | no | no | No value-returning runtime/provider contract exists for this command. | BLOCKED |

## Catalog And Provider Evidence

| Stable ID | Aliases | Parameters | Provider | Bridge | Tests | Producer / first consumer | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `action.findTemplate` | FIND_TEMPLATE | imagePath:IMAGE_TEMPLATE, threshold:PERCENT, timeoutMs:DURATION_MS, retryCount:NUMBER?, searchRegion:REGION? | `workspace vision adapter` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest; WorkspaceDryRunRuntimeTest; EmscriptParserSliceTest | WorkspaceBasicRuntimeEnvironment.findTemplate / WorkspaceScreen vision adapter / WorkspaceBasicRuntime LiveExecutionOutcome | A threshold-qualified match exists; null covers no qualifying match and unavailable comparison evidence, while score and region are discarded into display text. |
| `vision.findText` | Region.findText, FIND_TEXT | text:TEXT, timeoutMs:DURATION_MS? | `visualtasker.vision` | CONFLICT/CONTRACT_DECISION | EmscriptParserSliceTest; RuntimeCapabilityGateTest | none / statement-only catalog/runtime trace | Catalog parameters establish text and timeout only; no authoritative OCR result, match collection or not-found/failure contract exists. |
| `vision.markerLoad` | Marker.load, LOAD_MARKER | name:TEXT | `visualtasker.vision` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest; WorkspaceDryRunRuntimeTest; EmscriptParserSliceTest | WorkspaceScreen saved-marker lookup / WorkspaceBasicRuntime LiveExecutionOutcome | Saved markers yield exact bounds or null; Region exists as parameter syntax but not as a TypeDefinition-backed runtime result. |
| `tasker.isEnabled` | none | none | `visualtasker.tasker` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | TaskerRegistration.inspect / WorkspaceScreen taskerCommand adapter | status.available combines installation, permission, enabled preference, external access and receiver availability; false is not one stable fact. |
| `tasker.getVariable` | none | name:VARIABLE_REF | `visualtasker.tasker` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | none / statement-only adapter/trace | Runner/Receiver contract is explicitly not connected; no variable value is returned. |
| `tasker.getVariables` | none | pattern:TEXT? | `visualtasker.tasker` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | none / statement-only adapter/trace | No collection result exists; command name and pattern do not prove List, Map or opaque payload semantics. |
| `shizuku.getUid` | none | none | `visualtasker.shizuku` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest; ShizukuRegistrationStatusTest; PluginRuntimeReadinessTest | ShizukuRegistration.inspect / Shizuku.getUid / WorkspaceScreen shizukuCommand adapter | uid is read only with live binder and permission; exceptions become null, so not installed, unavailable binder, missing permission and call failure are indistinguishable. |
| `termux.get` | none | key:TEXT | `visualtasker.termux` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | TermuxRegistration.inspect / WorkspaceScreen key selector / WorkspaceScreen termuxCommand adapter | Known keys produce Boolean/status strings; unknown key uses an empty-string sentinel and no typed payload exists. |
| `scrcpy.isRunning` | none | serial:TEXT? | `visualtasker.scrcpy` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | Vt2VtUsbAdbBridge.detect / WorkspaceScreen scrcpyCommand adapter | bridgeReady means USB connected and ADB enabled; it does not establish a running scrcpy process or session. |
| `scrcpy.get` | none | key:TEXT | `visualtasker.scrcpy` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | Vt2VtUsbAdbBridge.detect / WorkspaceScreen scrcpyCommand adapter | A summary exists, but key is ignored and no absence/failure contract exists. |
| `chart.exists` | none | id:TEXT | `visualtasker.charts` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | none / statement-only adapter/trace | Catalog/plugin metadata exists, but WorkspaceBasicRuntime has no chart dispatch or existence result. |
| `chart.get` | none | id:TEXT, key:TEXT? | `visualtasker.charts` | CONFLICT/CONTRACT_DECISION | WorkspaceBasicRuntimeTest provider dispatch; QueryReturnContractAuditTest | none / statement-only adapter/trace | No value-returning chart path exists; id and optional key do not establish scalar, collection or structured semantics. |

## Structured Result Evidence

| Stable ID | Proposed type | Internal type | Repository fields | Planned-only fields | Nullable |
| --- | --- | --- | --- | --- | --- |
| `action.findTemplate` | `ImageMatch` | `RuntimeTemplateMatch` | name:String; region:RuntimeAutomationRegion; score:Float | none | YES |
| `vision.markerLoad` | `Region` | `RuntimeAutomationRegion` | x:Int; y:Int; width:Int; height:Int | none | YES |

## Nullable Language Impact

Scalar nullable transport exists, but source-level inspection or resolution does not. Every nullable candidate here has consumptionReady=false. Repository evidence creates needs for presence testing, conditional branching on presence, extraction of a present value and explicit fallback; this audit chooses no syntax.

## Recommended Next Slice

M1B-3U converges shizuku.isAvailable through provider-local installation, permission and Binder provenance plus the generic RuntimeAdapterResult value payload. The next slice must be selected from the remaining twelve contracts rather than inferred here.
