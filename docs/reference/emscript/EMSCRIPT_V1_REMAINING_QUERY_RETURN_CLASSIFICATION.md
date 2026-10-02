# EMScript v1 M1B-3V Remaining Query Return Classification

Stand: 2026-10-01
Status: historical 3V audit; scalar and Tasker collection queries migrated through M1B-3X

## Repository Truth

M1B-3V derived exactly 12 D_QUERY_RETURN conflicts. Post-3X, seven provider queries are typed reporters and five conflicts remain. Command Catalog remains 127 and NATIVE_V1 remains 3.

## Decision Summary

| Command | Proposed V1 | Nullable | Infrastructure | Group |
| --- | --- | --- | --- | --- |
| `action.findTemplate` | `ImageMatch?` | yes | NEW_STRUCTURED_VALUE | M1B-3Y |
| `vision.findText` | `TextMatch?` | yes | NEW_STRUCTURED_VALUE | M1B-3Y |
| `vision.markerLoad` | `Marker?` | yes | NEW_STRUCTURED_VALUE | M1B-3Y |
| `tasker.isEnabled` | `Bool` | no | EXISTING_SCALAR | M1B-3W |
| `tasker.getVariable` | `String?` | yes | EXISTING_SCALAR | M1B-3W |
| `tasker.getVariables` | `List<TaskerVariable>` | no | NEW_COLLECTION_AND_STRUCTURED_VALUE | M1B-3X |
| `shizuku.getUid` | `Number?` | yes | EXISTING_SCALAR | M1B-3W |
| `termux.get` | `String?` | yes | EXISTING_SCALAR | M1B-3W |
| `scrcpy.isRunning` | `Bool` | no | EXISTING_SCALAR | M1B-3W |
| `scrcpy.get` | `String?` | yes | EXISTING_SCALAR | M1B-3W |
| `chart.exists` | `Bool` | no | EXISTING_SCALAR | M1B-3Z |
| `chart.get` | `ChartSnapshot?` | yes | NEW_STRUCTURED_VALUE | M1B-3Z |

## Per-command Classification

### `action.findTemplate`

- Meaning: Find the best threshold-qualified occurrence of a stored image template in the current visual evidence.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: RuntimeTemplateMatch? is converted to LiveExecutionOutcome text; null is shown as not found.
- Proposed V1 return: `ImageMatch?`; nullable: yes
- ABSENT: The template search completed successfully but no candidate in the requested region reached the threshold before timeout.
- Negative/empty/zero: No separate negative scalar exists; a score, including 0 when threshold permits it, is data inside ImageMatch.
- Failure: Vision adapter unavailable, template resource missing or unreadable, capture unavailable, or comparison failed.
- First loss point: compareScreenshotRegions returns nullable score, so missing evidence, comparison failure and no match collapse before RuntimeTemplateMatch; LiveExecutionOutcome then discards region and score.
- Current transport: WorkspaceScreen vision callback -> RuntimeTemplateMatch? -> WorkspaceBasicRuntime statement outcome text.
- Infrastructure: NEW_STRUCTURED_VALUE; existing `NullValue only`; required `ImageMatchValue(templateId, label, region, score)`
- Diagnostics: `VISION_ADAPTER_UNAVAILABLE`, `TEMPLATE_RESOURCE_UNAVAILABLE`, `VISION_CAPTURE_FAILED`, `TEMPLATE_MATCH_FAILED`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Keep stable ID action.findTemplate and import aliases findTemplate/FIND_TEMPLATE; canonical templateFind normalization must preserve all five arguments.
- Migration: M1B-3Y

### `vision.findText`

- Meaning: Return the best textual observation matching the requested text from one completed perception pass.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: No value-returning provider or runtime handler exists; the catalog currently produces only a statement trace.
- Proposed V1 return: `TextMatch?`; nullable: yes
- ABSENT: OCR/perception completed successfully and produced no text observation matching the query.
- Negative/empty/zero: No negative scalar; confidence 0 is a legitimate TextMatch field only when a provider emits such a match.
- Failure: Vision adapter unavailable, screen capture failed, OCR engine failed, or provider response was malformed.
- First loss point: There is no provider result contract or WorkspaceBasicRuntime environment function.
- Current transport: Catalog statement -> generic trace only; no producer-to-runtime value path.
- Infrastructure: NEW_STRUCTURED_VALUE; existing `NullValue only`; required `TextMatchValue(text, region, confidence, source)`
- Diagnostics: `VISION_ADAPTER_UNAVAILABLE`, `VISION_CAPTURE_FAILED`, `OCR_FAILED`, `VISION_RESULT_INVALID`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Keep stable ID vision.findText and aliases Region.findText/FIND_TEXT. Singular findText returns the best match; a future multi-match command must use a distinct ID.
- Migration: M1B-3Y

### `vision.markerLoad`

- Meaning: Load an immutable snapshot of a persisted marker resource by stable marker ID or legacy label.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: RuntimeAutomationRegion? is returned by the callback after mutating canvas selection; marker identity and metadata are discarded.
- Proposed V1 return: `Marker?`; nullable: yes
- ABSENT: The marker repository was read successfully and contains no marker with the requested ID or label.
- Negative/empty/zero: No negative scalar and no empty-marker sentinel.
- Failure: Marker repository unavailable, persisted marker malformed, or geometry/metadata decoding failed.
- First loss point: WorkspaceScreen markerLoad projects ScreenshotCanvasSavedMarker to region only and uses null for both missing and failed storage paths.
- Current transport: Persisted marker list -> ScreenshotCanvasSavedMarker -> RuntimeAutomationRegion? -> statement outcome text.
- Infrastructure: NEW_STRUCTURED_VALUE; existing `NullValue only`; required `MarkerValue(markerId, label, region, path, mode, assetId, threshold)`
- Diagnostics: `VISION_ADAPTER_UNAVAILABLE`, `MARKER_REPOSITORY_UNAVAILABLE`, `MARKER_DECODE_FAILED`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Keep markerLoad/Marker.load/LOAD_MARKER. Lookup may accept a legacy label, but the returned value must retain the stable marker ID and must not be a live UI handle.
- Migration: M1B-3Y

### `tasker.isEnabled`

- Meaning: Report Tasker's own enabled preference, not complete WSS-to-Tasker execution readiness.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: TaskerRegistrationStatus.available combines installation, RUN_TASKS permission, enabled preference, external access and receiver availability.
- Proposed V1 return: `Bool`; nullable: no
- ABSENT: No ABSENT state.
- Negative/empty/zero: false means Tasker is not installed or its own enabled preference is explicitly false.
- Failure: Adapter unavailable, installation inspection failed, or the installed Tasker preference could not be read reliably.
- First loss point: taskerPrefSet converts provider exceptions and missing rows to null; WorkspaceScreen then substitutes composite status.available.
- Current transport: Tasker content-provider/package inspection -> TaskerRegistrationStatus -> RuntimeAdapterResult success/message without typed value.
- Infrastructure: EXISTING_SCALAR; existing `BooleanValue`; required `none`
- Diagnostics: `TASKER_ADAPTER_UNAVAILABLE`, `TASKER_INSTALLATION_CHECK_FAILED`, `TASKER_ENABLED_CHECK_FAILED`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Tasker.isEnabled to tasker.isEnabled while retaining the former spelling as an import alias.
- Migration: M1B-3W

### `tasker.getVariable`

- Meaning: Read the current String value of one named Tasker variable.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: No Runner/Receiver result contract is connected and no value is produced.
- Proposed V1 return: `String?`; nullable: yes
- ABSENT: The Tasker query succeeded but the named variable does not exist.
- Negative/empty/zero: An empty String is a legitimate variable value and must not become ABSENT.
- Failure: Tasker or adapter unavailable, permission/access denied, IPC failed, or response was malformed.
- First loss point: The Tasker adapter has no value-returning receiver/session path for getVariable.
- Current transport: Catalog statement -> unconnected Tasker adapter warning; no return payload.
- Infrastructure: EXISTING_SCALAR; existing `StringValue and NullValue`; required `none`
- Diagnostics: `TASKER_ADAPTER_UNAVAILABLE`, `TASKER_NOT_INSTALLED`, `TASKER_ACCESS_DENIED`, `TASKER_VARIABLE_QUERY_FAILED`, `TASKER_RESULT_INVALID`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Tasker.getVariable to tasker.getVariable and retain the legacy alias and variable-reference argument spelling.
- Migration: M1B-3W

### `tasker.getVariables`

- Meaning: Return all Tasker variable name/value pairs matching the optional pattern.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: No collection result producer or receiver contract exists.
- Proposed V1 return: `List<TaskerVariable>`; nullable: no
- ABSENT: No ABSENT state; a successful query always returns a list.
- Negative/empty/zero: An empty list means the query succeeded and no variables matched. Empty String remains a legitimate TaskerVariable value.
- Failure: Tasker or adapter unavailable, permission/access denied, IPC failed, or collection response was malformed.
- First loss point: The Tasker bridge has no collection-returning receiver/session path or runtime list value.
- Current transport: Catalog statement -> unconnected Tasker adapter warning; no collection payload.
- Infrastructure: NEW_COLLECTION_AND_STRUCTURED_VALUE; existing `ContractValue.ListValue exists only in language-core; runtime EmscriptValue has no list or TaskerVariable value.`; required `ListValue<TaskerVariableValue(name, value)>`
- Diagnostics: `TASKER_ADAPTER_UNAVAILABLE`, `TASKER_NOT_INSTALLED`, `TASKER_ACCESS_DENIED`, `TASKER_VARIABLE_QUERY_FAILED`, `TASKER_RESULT_INVALID`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Tasker.getVariables to tasker.getVariables and retain the legacy spelling; result ordering must be deterministic by variable name.
- Migration: M1B-3X

### `shizuku.getUid`

- Meaning: Return the UID of the currently available Shizuku service.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: ShizukuRegistrationStatus.uid is rendered as -1 when unavailable; inspectAvailability also suppresses getUid exceptions into null.
- Proposed V1 return: `Number?`; nullable: yes
- ABSENT: No Shizuku service UID is currently available because Shizuku is not installed, permission is not granted, or the binder is not alive.
- Negative/empty/zero: 0 is a legitimate UID and must remain NumberValue(0); negative sentinel values are forbidden.
- Failure: Adapter unavailable, package/permission/binder inspection failed technically, getUid threw, or returned an invalid negative UID.
- First loss point: inspectShizukuAvailability uses runCatching(uidProbe).getOrNull and WorkspaceScreen converts null to -1 text.
- Current transport: Shizuku inspection -> nullable Int -> RuntimeAdapterResult message text with -1 sentinel.
- Infrastructure: EXISTING_SCALAR; existing `NumberValue and NullValue`; required `none`
- Diagnostics: `SHIZUKU_ADAPTER_UNAVAILABLE`, `SHIZUKU_INSTALLATION_CHECK_FAILED`, `SHIZUKU_PERMISSION_CHECK_FAILED`, `SHIZUKU_BINDER_CHECK_FAILED`, `SHIZUKU_UID_QUERY_FAILED`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Shizuku.getUid to shizuku.getUid and retain the legacy spelling; remove only the -1 sentinel, not valid UID 0.
- Migration: M1B-3W

### `termux.get`

- Meaning: Read one supported Termux registration-status property as canonical text.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: Known keys produce String text; an unknown key becomes empty String and all inspection faults are flattened into status fields.
- Proposed V1 return: `String?`; nullable: yes
- ABSENT: The requested key is not in the frozen key set: installed, apiInstalled, canRunCommands, permission, summary.
- Negative/empty/zero: Supported Boolean/status keys return explicit text such as false or missing; these are values, not ABSENT. Empty String is not a missing-key sentinel in V1.
- Failure: Adapter unavailable or package/permission/status inspection failed technically.
- First loss point: TermuxRegistration.inspect uses Boolean fallbacks, then WorkspaceScreen maps unknown keys to empty String and stores the value only in message text.
- Current transport: Termux registration inspection -> key switch -> RuntimeAdapterResult message text.
- Infrastructure: EXISTING_SCALAR; existing `StringValue and NullValue`; required `none`
- Diagnostics: `TERMUX_ADAPTER_UNAVAILABLE`, `TERMUX_STATUS_CHECK_FAILED`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Termux.get to termux.get and retain the legacy spelling. Empty key remains a legacy alias for summary; unknown nonempty keys become NullValue.
- Migration: M1B-3W

### `scrcpy.isRunning`

- Meaning: Report whether a scrcpy session for the optional serial is actively running.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: Vt2VtUsbAdbBridgeStatus.bridgeReady is substituted for scrcpy process/session state.
- Proposed V1 return: `Bool`; nullable: no
- ABSENT: No ABSENT state.
- Negative/empty/zero: false means the session registry was inspected successfully and no matching scrcpy session is running, including when no host/device is available.
- Failure: scrcpy adapter unavailable or session/process state could not be inspected reliably.
- First loss point: WorkspaceScreen uses USB/ADB bridge readiness instead of a scrcpy session registry; Vt2VtUsbAdbBridge.detect also converts technical probe errors to false.
- Current transport: VT2VT USB/ADB status -> bridgeReady Boolean -> RuntimeAdapterResult success/message without typed value.
- Infrastructure: EXISTING_SCALAR; existing `BooleanValue`; required `none`
- Diagnostics: `SCRCPY_ADAPTER_UNAVAILABLE`, `SCRCPY_SESSION_CHECK_FAILED`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Scrcpy.isRunning to scrcpy.isRunning and retain the legacy spelling. Optional serial selects one session without changing return type.
- Migration: M1B-3W

### `scrcpy.get`

- Meaning: Read one textual property of the active scrcpy session selected by the provider and key.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: The requested key is ignored and the VT2VT USB/ADB bridge summary is always embedded in message text.
- Proposed V1 return: `String?`; nullable: yes
- ABSENT: A known session key has no value because no matching active session exists. Frozen keys are sessionId, serial, state, transport, host and port.
- Negative/empty/zero: No negative scalar; textual state values such as stopped are legitimate values when supplied by an existing session snapshot.
- Failure: Adapter unavailable, session inspection failed, response malformed, or a nonempty key is outside the frozen key set.
- First loss point: WorkspaceScreen ignores key semantics and substitutes Vt2VtUsbAdbBridgeStatus.summary for a scrcpy session value.
- Current transport: VT2VT USB/ADB status -> summary String -> RuntimeAdapterResult message text.
- Infrastructure: EXISTING_SCALAR; existing `StringValue and NullValue`; required `none`
- Diagnostics: `SCRCPY_ADAPTER_UNAVAILABLE`, `SCRCPY_SESSION_CHECK_FAILED`, `SCRCPY_UNKNOWN_KEY`, `SCRCPY_RESULT_INVALID`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Scrcpy.get to scrcpy.get and retain the legacy spelling; key names are case-insensitive on import and canonical lowerCamelCase on serialization.
- Migration: M1B-3W

### `chart.exists`

- Meaning: Test whether the chart repository contains a chart snapshot with the requested stable ID.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: No chart dispatch, repository lookup or result exists in WorkspaceBasicRuntime.
- Proposed V1 return: `Bool`; nullable: no
- ABSENT: No ABSENT state.
- Negative/empty/zero: false means the chart repository was read successfully and contains no chart with that ID.
- Failure: Chart adapter/repository unavailable or repository lookup failed.
- First loss point: No provider-independent chart repository adapter is connected to WorkspaceBasicRuntime.
- Current transport: Catalog statement only; ChartGraph domain models are not connected to EMScript runtime.
- Infrastructure: EXISTING_SCALAR; existing `BooleanValue`; required `none`
- Diagnostics: `CHART_ADAPTER_UNAVAILABLE`, `CHART_REPOSITORY_QUERY_FAILED`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Chart.exists to chart.exists and retain the legacy spelling; chart ID is a persistent resource ID, not a runtime handle.
- Migration: M1B-3Z

### `chart.get`

- Meaning: Load an immutable chart value snapshot by persistent chart ID.
- Current declaration: `Void/unspecified (statement-shaped)`
- Current runtime value: No chart dispatch or value exists; the optional legacy key has no repository-backed semantics.
- Proposed V1 return: `ChartSnapshot?`; nullable: yes
- ABSENT: The chart repository was read successfully and contains no chart with the requested ID.
- Negative/empty/zero: An existing chart with empty series/data is a legitimate ChartSnapshot value, not ABSENT.
- Failure: Chart adapter/repository unavailable, persisted chart malformed, or snapshot decoding failed.
- First loss point: No chart repository adapter or EMScript chart value exists; catalog metadata is the only current path.
- Current transport: Catalog statement only; ChartDocument exists in the plugin but is not transported into EMScript.
- Infrastructure: NEW_STRUCTURED_VALUE; existing `NullValue only; ChartDocument is a plugin domain model, not an EMScript runtime value.`; required `ChartSnapshotValue(id, title, kind, immutable data and options)`
- Diagnostics: `CHART_ADAPTER_UNAVAILABLE`, `CHART_REPOSITORY_QUERY_FAILED`, `CHART_DECODE_FAILED`, `CHART_LEGACY_KEY_UNSUPPORTED`
- IR/projection: generic `IrExpression.CommandCall` and generic reporter are sufficient.
- Roundtrip/legacy: Normalize Chart.get to chart.get and retain the legacy spelling. Canonical V1 uses chart.get(id); the optional legacy key is import-only and must fail explicitly until mapped to a dedicated property command.
- Migration: M1B-3Z

## Migration Groups

### M1B-3W - Provider scalar queries

- Commands: `tasker.isEnabled`, `tasker.getVariable`, `shizuku.getUid`, `termux.get`, `scrcpy.isRunning`, `scrcpy.get`
- Contract: Provider-backed Bool, Number? and String? values use Success(value/NullValue) while technical inability remains Failure(Diagnostic).
- Infrastructure: Existing RuntimeAdapterResult, scalar EmscriptValue variants, NullValue and provider-local provenance; no new public value type.
- Diagnostics: Provider-specific adapter, inspection, query and invalid-result diagnostics.
- Expected D_QUERY_RETURN reduction: 6
- Risk: HIGH: each provider needs its own provenance even though transport is shared.
- Dependencies: M1B-3R typed transport, M1B-3L nullable infrastructure, M1B-3T/U provider inspection patterns.

### M1B-3X - Tasker variable collection

- Commands: `tasker.getVariables`
- Contract: A successful collection query always returns List<TaskerVariable>; no matches is an empty list, never NullValue.
- Infrastructure: Runtime ListValue plus TaskerVariableValue and a value-returning Tasker receiver/session contract.
- Diagnostics: TASKER_ADAPTER_UNAVAILABLE, TASKER_NOT_INSTALLED, TASKER_ACCESS_DENIED, TASKER_VARIABLE_QUERY_FAILED, TASKER_RESULT_INVALID.
- Expected D_QUERY_RETURN reduction: 1
- Risk: HIGH: runtime list transport is missing even though language-core already models lists.
- Dependencies: M1B-3W Tasker scalar/provider provenance should be reused.

### M1B-3Y - Perception and marker values

- Commands: `action.findTemplate`, `vision.findText`, `vision.markerLoad`
- Contract: Successful lookup returns an immutable structured value or NullValue for a completed no-match/no-resource result; capture, engine and storage faults are failures.
- Infrastructure: ImageMatchValue, TextMatchValue and MarkerValue plus provider result contracts; generic CommandCall and reporter remain sufficient.
- Diagnostics: Vision adapter/capture/engine/result diagnostics and marker repository/decode diagnostics.
- Expected D_QUERY_RETURN reduction: 3
- Risk: HIGH: existing callbacks collapse absent and failure and markerLoad currently discards identity.
- Dependencies: M1B-3L nullable infrastructure and M1B-3P perception failure separation.

### M1B-3Z - Chart repository queries

- Commands: `chart.exists`, `chart.get`
- Contract: Named persistent chart resources expose Bool existence and an immutable ChartSnapshot? value; runtime UI handles never cross into EMScript.
- Infrastructure: Provider-independent chart repository adapter and ChartSnapshotValue; existing Bool and NullValue transport for existence/absence.
- Diagnostics: CHART_ADAPTER_UNAVAILABLE, CHART_REPOSITORY_QUERY_FAILED, CHART_DECODE_FAILED, CHART_LEGACY_KEY_UNSUPPORTED.
- Expected D_QUERY_RETURN reduction: 2
- Risk: HIGH: no runtime dispatch exists and legacy chart.get key semantics are undefined.
- Dependencies: ChartGraph ChartDocument can inform the immutable value schema but must not be exposed as a live handle.

## Recommended Order

1. M1B-3W reuses only existing scalar values and establishes provider-local provenance.
2. M1B-3X adds the missing runtime collection/value pair on top of the Tasker provider path.
3. M1B-3Y adds immutable perception and marker values with strict no-match versus failure handling.
4. M1B-3Z connects the chart repository and removes the final two conflicts without exposing runtime handles.

Expected reductions are 6 + 1 + 3 + 2 = 12; successful completion leaves D_QUERY_RETURN at 0.

## Infrastructure Gap Summary

RuntimeAdapterResult, scalar EmscriptValue variants, NullValue, ListValue, TaskerVariableValue, nullable/list type compatibility, generic CommandCall and generic reporter projection are sufficient through M1B-3X. The remaining minimal additions are ImageMatchValue, TextMatchValue, MarkerValue and ChartSnapshotValue plus provider result contracts. No special expression class is required.
